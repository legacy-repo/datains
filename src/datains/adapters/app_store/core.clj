(ns datains.adapters.app-store.core
  (:require [datains.config :refer [env]]
            [clj-http.client :as client]
            [lambdaisland.uri :as uri-lib]
            [clojure.tools.logging :as log]
            [digest :as digest]
            [clj-jgit.porcelain :as git]
            [clojure.string :as clj-str]))

; Initialize the configuration of choppy store
(def ^:private config
  (atom {:access-token  nil
         :ping          "/api/v1/version"
         :api-prefix    "/api/v1"
         :host          nil
         :port          nil
         :scheme        nil
         :default-cover ""}))

(defn get-app-workdir
  "Fix bugs: env is null when mount is not evaluated, so need to use function instead of variable."
  []
  (str (clj-str/replace (get-in env [:datains-workdir]) #"/$" "") "/apps"))

(defn get-local-path
  [relative-path]
  (str (get-app-workdir) "/" relative-path))

(defn show-config
  []
  @config)

(defn setup-cs-from-env!
  "Setup the configuration of choppy store from environment variables."
  []
  (reset! config
          (merge @config
                 {:access-token  (:app-store-access-token env)
                  :host          (:app-store-host env "choppy.3steps.cn")
                  :port          (:app-store-port env 80)
                  :scheme        (:app-store-scheme env "http")
                  :default-cover (:app-default-cover env "")})))

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
     :repo-url    (:clone_url resp)
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

;;; ------------------------------------------------- Git API ---------------------------------------------------
(defn credentials
  []
  {:user (:app-store-username env)
   :pw   (:app-store-password env)})

(defn get-repo-path
  [repo]
  (.getParent (.getDirectory (.getRepository repo))))

(defn clone!
  "Clone a specified repo into a local path."
  [repo local-dir]
  ;; Use user/pw auth instead of key based auth
  (git/with-credentials (credentials)
    (git/git-clone repo :dir local-dir)))
