(ns datains.adapters.app-store.core
  (:require [clj-http.client :as client]
            [lambdaisland.uri :as uri-lib]
            [clojure.tools.logging :as log]
            [digest :as digest]
            [me.raynes.fs :as fs]
            [clj-jgit.porcelain :as git]
            [clojure.string :as clj-str]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.java.shell :as shell :refer [sh]]))

; Initialize the configuration of choppy store
(def ^:private config
  (atom {:access-token       nil
         :ping               "/api/v1/version"
         :api-prefix         "/api/v1"
         :host               nil
         :port               nil
         :scheme             nil
         :default-cover      ""
         :datains-workdir    "~/.datains/"
         :app-utility-bin    nil
         :app-store-password nil
         :app-store-username nil}))

(defn get-workdir
  "Fix bugs: env is null when mount is not evaluated, so need to use function instead of variable."
  []
  (let [work-dir (fs/expand-home (:datains-workdir @config))]
    (clj-str/replace work-dir #"/$" "")))

(defn get-app-workdir
  []
  (str (get-workdir) "/apps"))

(defn get-project-workdir
  []
  (str (get-workdir) "/projects"))

(defn get-local-path
  [relative-path]
  (str (get-app-workdir) "/" relative-path))

(defn show-config
  []
  @config)

(defn setup-config
  [new-config]
  (reset! config
          (merge @config new-config)))

(defn join-url
  [scheme host port path query]
  (str (assoc (uri-lib/uri "//example.com/foo/bar")
              :scheme scheme
              :host host
              :port port
              :path path
              :query query)))

(defn make-url
  [path query]
  (if (re-find #"http[s]?://" path)
    (str (assoc (uri-lib/uri path)
                :query query))
    (join-url (:scheme @config) (:host @config) (:port @config) path query)))

(def ^:private ping-path
  (:ping @config))

(defn app-utility-bin
  []
  (:app-utility-bin @config))

(defn- make-path
  "Concat the api-prefix with a path."
  [path]
  (str (:api-prefix @config) path))

(defn http-get
  "Access choppy store with GET method."
  ([path] (http-get path ""))
  ([path query]
   (client/get (make-url path query)
               {:as                 :json ;; TODO: It will throw "Invalid token" exception when I use :clojure. why?
                :content-type       :json
                :accept             :json
                :socket-timeout     1000  ;; in milliseconds
                :connection-timeout 1000  ;; in milliseconds
                :query-params       {"access_token" (:access-token @config)}})))

(defn http-head
  ([path] (http-head path ""))
  ([path query]
   (client/head (make-url path query)
                {:socket-timeout     1000
                 :connection-timeout 1000
                 :query-params       {"access_token" (:access-token @config)}})))

(defn exist-file?
  [path]
  (try
    (http-head path)
    path
    (catch Throwable _
      false)))

(defn gen-repo-path
  "Version can be nil or '', then return the master branch."
  [app-name version]
  (if (and version
           (> 0 (count version)))
    (make-url (str app-name "/src/tag/" version "spec.json") "")
    (make-url (str app-name "/src/branch/master" "spec.json") "")))

;;; ------------------------------------------------- App Store's API ---------------------------------------------------
(defn service-is-ok?
  []
  (try
    (http-get ping-path)
    true
    (catch Throwable _
      false)))

(defn count-by-topic
  "Get count of all apps which are marked by the specified topic.
   More details: http://choppy.3steps.cn/api/swagger#/repository/topicSearch
  "
  [topic]
  (if (= 0 (count topic))
    (throw (Exception. "Topic can't be empty.")))
  (try
    (-> (http-get (make-path "/topics/search")
                  (str "q=" topic))
        (:body)
        (:topics)
        (first)
        (:repo_count))
    (catch Throwable e
      (log/error e "Error access /api/v1/topics/search")
      nil)))

(defn app-is-valid?
  "it is a valid app, if the app which is marked with `choppy-app` 
   contains a spec.json file in the root directory."
  [app-name version]
  (if-let [spec (exist-file? (gen-repo-path app-name version))]
    spec
    false))

(defn get-cover-or-default
  [resp]
  (if-let [cover (exist-file?
                  (str (:html_url resp)
                       "/src/branch/master/cover.png"))]
    cover
    (:default-cover @config)))

(defn resp->record
  "Transform reponse data to the app record. 

    The response is request from app store and the app record is matched with database schema.
    More details: http://choppy.3steps.cn/api/v1/repos/search?q=choppy-app&topic=true&includeDesc=false&limit=1
  "
  [resp]
  (let [full_name (:full_name resp)]
    {:id          (digest/md5 full_name)            ; Computing md5 value as the app id
     :title       full_name
     :icon        (:avatar_url resp)
     :cover       (get-cover-or-default resp)
     :description (:description resp)
     :repo_url    (:clone_url resp)
     :author      (:username (:owner resp))
     :rate        (:stars_count resp)
     :valid       (app-is-valid? full_name nil)}))  ; Don't need to check whether the specified version is valid when we fetch all apps

(defn make-app-data
  "Map resp data to a set of app record."
  [data]
  (->> data
       (:data)
       (map resp->record)))

(defn get-apps-by-page
  "Get app records from app store.
   More details: http://choppy.3steps.cn/api/swagger#/repository/repoSearch
   TODO: how to handle the exceptions?
  "
  [page per-page topic]
  (try
    (-> (http-get (make-path "/repos/search")
                  (format "topic=true&includeDesc=false&limit=%d&q=%s&page=%d"
                          per-page topic page))
        (:body)
        (make-app-data))))

(defn get-all-apps
  "Get all apps and transform to a set of app record."
  [topic]
  (flatten
   (pmap
    #(get-apps-by-page % 50 topic)
    (range 1 (+ 1
                (/ (count-by-topic topic)
                   50))))))  ; maximum page size is 50

(defn get-installed-apps
  "Get the list of installed apps.
   e.g. ('choppy/bedtools' 'choppy/samtools')
  "
  [app-root-dir]
  (let [app-root-dir (fs/absolute (fs/expand-home app-root-dir))]
    (map #(clj-str/replace (.getPath %)
                           (re-pattern (str (.getPath app-root-dir) "/"))
                           "")
         (filter #(and (some? %) (fs/directory? %))
                 (flatten
                  (map #(fs/list-dir %)
                       (fs/list-dir app-root-dir)))))))

(defn is-installed?
  "Returns true if an app is installed, false otherwise."
  ([app-name] (is-installed? app-name (get-app-workdir)))
  ([app-name app-root-dir] (> (count
                               (filter #(re-find (re-pattern app-name) %)
                                       (get-installed-apps app-root-dir)))
                              0)))

(defn exist-bin?
  [name]
  (= 0 (:exit (sh "which" name))))

(defn make-sample-file!
  "Save job-params as a sample file."
  [project-name sample-id job-params]
  (let [dest-file (format "%s/%s/%s/%s" (get-project-workdir) project-name sample-id "sample.json")]
    (io/make-parents dest-file)
    (spit dest-file (json/write-str job-params))
    dest-file))

(defn render-app!
  "Render as a pipeline based on the specified app template.
   project_name: ^[a-zA-Z_][a-zA-Z0-9_]+$
   app_name: huangyechao/annovar
   samples: file path
  "
  ([project-name app-name samples] (render-app! project-name app-name samples
                                                (get-app-workdir)
                                                (get-project-workdir)))
  ([project-name app-name samples base-dir work-dir]
   (if (re-find #"^[a-zA-Z_][a-zA-Z0-9_]+$" project-name)
     (shell/with-sh-env {:PATH   (app-utility-bin)
                         :LC_ALL "en_US.utf-8"
                         :LANG   "en_US.utf-8"}
       (if (exist-bin? "app-utility")
         (let [command ["bash" "-c"
                        (format "app-utility render %s %s --base-dir %s --work-dir %s --project-name %s --force"
                                app-name samples base-dir work-dir project-name)]
               result  (apply sh command)]
           result)
         {:exit 1
          :out   ""
          :err   "Command not found: app-utility."}))
     {:exit 2
      :out  ""
      :err "Not valid project-name: ^[a-zA-Z_][a-zA-Z0-9_]+$"})))

;;; ------------------------------------------------- Git API ---------------------------------------------------
(defn credentials
  []
  {:user (:app-store-username @config)
   :pw   (:app-store-password @config)})

(defn get-repo-path
  [repo]
  (.getParent (.getDirectory (.getRepository repo))))

(defn clone!
  "Clone a specified repo into a local path."
  [repo local-dir]
  ;; Use user/pw auth instead of key based auth
  (git/with-credentials (credentials)
    (git/git-clone repo :dir local-dir)))
