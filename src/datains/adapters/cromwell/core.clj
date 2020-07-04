(ns datains.adapters.cromwell.core
  "Launch a Cromwell workflow and wait for it to complete."
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [me.raynes.fs :as fs]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [datains.adapters.cromwell.debug :as debug]
            [datains.adapters.cromwell.util :as util]
            ; Need to import env environment
            [datains.config :refer [env]]))

(defn get-workdir
  "Fix bugs: env is null when mount is not evaluated, so need to use function instead of variable."
  []
  (let [work-dir (fs/expand-home (get-in env [:datains-workdir]))]
    (str/replace work-dir #"/$" "")))

(defn get-cromwell-workdir
  []
  (str (get-workdir) "/cromwell"))

(def statuses
  "The statuses a Cromwell workflow can have."
  ["Aborted"
   "Aborting"
   "Failed"
   "On Hold"
   "Running"
   "Submitted"
   "Succeeded"])

(defn get-url
  "URL for GotC Cromwell."
  []
  (get-in env [:cromwell :url]))

(defn engine
  "Engine URL for Connecting Cromwell Engine."
  []
  (str (get-url) "/engine/v1"))

(defn get-local-auth-header
  "Local auth header."
  []
  {"Authorization" (get-in env [:cromwell :token])})

(defn api
  "API URL for GotC Cromwell API."
  []
  (str (get-url) "/api/workflows/v1"))

(defn request-json
  "Response to REQUEST with :body parsed as JSON."
  [request]
  (let [{:keys [body]
         :as   response} (http/request request)]
    (assoc response :body (json/read-str body :key-fn keyword))))

(def bogus-key-character-map
  "Map bogus characters in metadata keys to replacements."
  (let [tag   (str "%" util/the-name "%")
        bogus {" " "SPACE"
               "(" "OPEN"
               ")" "CLOSE"}]
    (letfn [(wrap [v] (str tag v tag))]
      (zipmap (keys bogus) (map wrap (vals bogus))))))

(def bogus-key-characters
  "Set of the bogus characters found in metadata keys"
  (set (str/join (keys bogus-key-character-map))))

(defn name-bogus-characters
  "Replace bogus characters in S with their names."
  [s]
  (reduce (fn [s [k v]] (str/replace s k v))
          s bogus-key-character-map))

(defn ok?
  "Check whether the cromwell service is okay."
  []
  (-> {:method       :get
       :content-type :application/json
       :headers      (get-local-auth-header)
       :url          (str (engine) "/version")}
      (request-json)
      :body))

(defn some-thing
  "GET or POST THING to ENVIRONMENT Cromwell for workflow with ID, where
  QUERY-PARAMS is a map of extra query parameters to pass on the URL.
  HACK: Frob any BOGUS-KEY-CHARACTERS so maps can be keywordized."
  ([method thing id query-params]
   (letfn [(maybe [m k v] (if (seq v) (assoc m k v) m))]
     (let [edn (-> {:method  method ;; :debug true :debug-body true
                    :url     (str (api) "/" id "/" thing)
                    :headers (get-local-auth-header)}
                   (maybe :query-params query-params)
                   http/request :body json/read-str)
           bad (filter (partial some bogus-key-characters) (util/keys-in edn))
           fix (into {} (for [k bad] [k (name-bogus-characters k)]))]
       (->> edn
            (walk/postwalk-replace fix)
            walk/keywordize-keys))))
  ([method thing id]
   (some-thing method thing id {})))

(defn get-thing
  "GET the ENVIRONMENT Cromwell THING for the workflow with ID."
  ([thing id query-params]
   (try
     (some-thing :get thing id query-params)
     (catch Exception e
       (log/warn "<" (.getMessage e) ">" "Query Cromwell Instance: " thing id query-params)
       {})))
  ([thing id]
   (get-thing thing id {})))

(defn post-thing
  "POST the ENVIRONMENT Cromwell THING for the workflow with ID."
  [thing id]
  (some-thing :post thing id))

(defn status
  "Status of the workflow with ID on Cromwell."
  [id]
  (:status (get-thing "status" id)))

(defn release-hold
  "Let 'On Hold' workflow with ID run on Cromwell."
  [id]
  (post-thing "releaseHold" id))

(defn release-a-workflow-every-10-seconds
  "Every 10 seconds release one workflow from WORKFLOW-IDS."
  [workflow-ids]
  (when (seq workflow-ids)
    (util/sleep-seconds 10)
    (release-hold (first workflow-ids))
    (recur (rest workflow-ids))))

(defn release-workflows-using-agent
  "Return an agent running release-a-workflow-every-10-seconds on all
  the workflow IDs returned by FIND-ENVIRONMENT-AND-WORKFLOW-IDS."
  [find-environment-and-workflow-ids]
  (let [[& workflow-ids] (find-environment-and-workflow-ids)]
    (send-off (agent workflow-ids)
              release-a-workflow-every-10-seconds)))

(defn metadata
  "GET the metadata for workflow ID."
  ([id query-params]
   (get-thing "metadata" id query-params))
  ([id]
   (metadata id {})))

(defn all-metadata
  "Fetch all metadata from ENVIRONMENT Cromwell for workflow ID."
  [id]
  (metadata id {:expandSubWorkflows true}))

(defn outputs
  "GET the metadata for workflow ID."
  ([id query-params]
   (get-thing "outputs" id query-params))
  ([id]
   (outputs id {})))

(defn cromwellify-json-form
  "Translate FORM-PARAMS into the list of single-entry maps that
  Cromwell expects in its query POST request."
  [form-params]
  (letfn [(expand [[k v]] (if (vector? v)
                            (for [x v] {k (str x)})
                            [{k (str v)}]))]
    (mapcat expand form-params)))

(defn query
  "Lazy results of querying Cromwell with PARAMS map."
  [params]
  (let [form-params (merge {:pagesize 999} params)
        request     {:method       :post ;; :debug true :debug-body true
                     :url          (str (api) "/query")
                     :form-params  (cromwellify-json-form form-params)
                     :content-type :application/json}]
    (letfn [(each [page sofar]
              (let [response                            (-> request
                                                            (update :form-params conj {:page (str page)})
                                                            (assoc :headers (get-local-auth-header))
                                                            request-json :body)
                    {:keys [results totalResultsCount]} response
                    total                               (+ sofar (count results))]
                (lazy-cat results (when (< total totalResultsCount)
                                    (each (inc page) total)))))]
      (util/lazy-unchunk (each 1 0)))))

;; HACK: (into (array-map) ...) is egregious.
;;
(defn status-counts
  "Map status to workflow counts on Cromwell with PARAMS
  map and AUTH-HEADER."
  ([auth-header params]
   (letfn [(each [status]
             (let [form-params (-> {:pagesize 1
                                    :status   status}
                                   (merge params)
                                   cromwellify-json-form)]
               [status (-> {:method       :post ;; :debug true :debug-body true
                            :url          (str (api) "/query")
                            :form-params  form-params
                            :content-type :application/json
                            :headers      auth-header}
                           request-json :body :totalResultsCount)]))]
     (let [counts (into (array-map) (map each statuses))
           total  (apply + (map counts statuses))]
       (into counts [[:total total]]))))
  ([params]
   (status-counts (get-local-auth-header) params)))

(defn make-workflow-labels
  "Return the workflow labels from ENVIRONMENT, WDL, and INPUTS."
  [wdl inputs]
  (letfn [(unprefix [[k v]] [(keyword (last (str/split (name k) #"\."))) v])
          (key-for [suffix] (keyword (str util/the-name "-" (name suffix))))]
    (let [the-version   (util/get-the-version)
          wdl-value     (last (str/split wdl #"/"))
          version-value (-> the-version
                            (select-keys [:commit :version])
                            (json/write-str :escape-slash false))]
      (merge
       {(key-for :version)     version-value
        (key-for :wdl)         wdl-value
        (key-for :wdl-version) (or (the-version wdl-value) "Unknown")}
       (select-keys (into {} (map unprefix inputs))
                    (get-in env [:cromwell :labels]))))))

(defn post-workflow
  "Assemble PARTS into a multipart HTML body and post it to the Cromwell
  server, and return the workflow ID."
  [parts]
  (letfn [(multipartify [[k v]] {:name    (name k)
                                 :content v})]
    (-> {:method    :post ;; :debug true :debug-body true
         :url       (api)
         :headers   (get-local-auth-header)
         :multipart (map multipartify parts)}
        request-json #_debug/dump :body :id)))

(defn partify-workflow
  "Return a map describing a workflow named WF to run
   with DEPENDENCIES, INPUTS, OPTIONS, and LABELS."
  [wf dependencies inputs options labels]
  (letfn [(jsonify [edn] (when edn (json/write-str edn :escape-slash false)))
          (maybe [m k v] (if v (assoc m k v) m))]
    (-> {:workflowSource wf
         :workflowType   "WDL"
         :labels         (jsonify labels)}
        (maybe :workflowDependencies dependencies)
        (maybe :workflowInputs       (jsonify inputs))
        (maybe :workflowOptions      (jsonify options)))))

(defn hold-workflow
  "Submit a workflow 'On Hold' to run WDL with IMPORTS-ZIP, INPUTS,
  OPTIONS, and LABELS on the Cromwell and return its
  ID.  IMPORTS-ZIP, INPUTS, OPTIONS, and LABELS can be nil.  WDL is
  the top-level wf.wdl file specifying the workflow.  IMPORTS-ZIP is a
  zip archive of WDL's dependencies.  INPUTS and OPTIONS are the
  standard JSON files for Cromwell.  LABELS is a {:key value} map."
  [wdl imports-zip inputs options labels]
  (post-workflow (assoc (partify-workflow
                         wdl
                         imports-zip
                         inputs
                         options
                         labels)
                        :workflowOnHold "true")))

(defn submit-workflow
  "Submit a workflow to run WDL with IMPORTS-ZIP, INPUTS,
  OPTIONS, and LABELS on the Cromwell and return its
  ID.  IMPORTS-ZIP, INPUTS, OPTIONS, and LABELS can be nil.  WDL is
  the top-level wf.wdl file specifying the workflow.  IMPORTS-ZIP is a
  zip archive of WDL's dependencies.  INPUTS and OPTIONS are the
  standard JSON files for Cromwell.  LABELS is a {:key value} map."
  [wdl imports-zip inputs options labels]
  (post-workflow (partify-workflow
                  wdl
                  imports-zip
                  inputs
                  options
                  labels)))

(defn work-around-cromwell-fail-bug
  "Wait 2 seconds and ignore up to N times a bogus failure response from
  Cromwell for workflow ID.  Work around the 'sore spot'
  reported in https://github.com/broadinstitute/cromwell/issues/2671"
  [n id]
  (util/sleep-seconds 2)
  (let [fail                    {"status"  "fail"
                                 "message" (str "Unrecognized workflow ID: " id)}
        {:keys [body]
         :as   bug} (try (get-thing "status"  id)
                         (catch Exception e (ex-data e)))]
    (debug/trace [bug n])
    (when (and (pos? n) bug
               (= 404 (:status bug))
               (= fail (json/read-str body)))
      (recur (dec n)  id))))

(defn wait-for-workflow-complete
  "Return metadata of workflow named by ID when it completes."
  [id]
  (work-around-cromwell-fail-bug 9  id)
  (loop [id          id]
    (let [now (status id)]
      (if (and now (#{"Submitted" "Running"} now))
        (do (util/sleep-seconds 15) (recur  id))
        (metadata  id)))))

(defn abort
  "Abort the workflow with ID run on Cromwell."
  [id]
  (post-thing "abort" id))

(defn count-task
  "How many tasks in a workflow?"
  [all-metadata]
  (count (:imports (:submittedFiles all-metadata))))

(defn count-finished-task
  "How many finished tasks in a workflow?"
  [all-metadata]
  (count
   (filter
    (fn [[_ value]]
      (not-empty (filter #(and (:end %)
                               (= 0 (:returnCode %)))
                         value)))
    (:calls all-metadata))))

(defn workflow-metadata
  "Return the finished percentage of workflow named by ID."
  [id]
  (let [metadata (all-metadata id)]
    {:status         (:status metadata)
     :started_time   (:start metadata)
     :finished_time  (:end metadata)
     :submitted_time (:submission metadata)
     :percentage     (* 100 (/ (count-finished-task metadata) (* 1.0 (count-task metadata))))}))

(defn list-task-logs
  [id]
  (let [metadata (all-metadata id)
        calls    (:calls metadata)
        root     (re-pattern (get-cromwell-workdir))]
    (apply merge
           (map (fn [[key value]]
                  {key {:stdout (str/replace (:stdout (first value)) root "")
                        :stderr (str/replace (:stderr (first value)) root "")}}) calls))))