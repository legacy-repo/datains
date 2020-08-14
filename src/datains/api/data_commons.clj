(ns datains.api.data-commons
  (:require [clojure.tools.logging :as log]
            [ring.util.http-response :refer [ok]]
            [datains.api.data-commons-spec :as data-commons-spec]
            [datains.adapters.data-commons.core :as dc]))

(def data-commons
  [""
   {:swagger {:tags ["Data Commons"]}}

   ["/collections"
    {:get  {:summary    "Get collections"
            :parameters {:query any?}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{:keys [query]} :parameters}]
                          (let [page (if (:page query) (Integer/parseInt (:page query)) 1)
                                per-page (if (:per_page query) (Integer/parseInt (:per_page query)) 10)
                                query-map (dissoc query :page :per_page)]
                            (ok {:total (dc/count-coll query-map)
                                 :page page
                                 :per_page per-page
                                 :data (dc/query [(dc/find-coll query-map)
                                                  (dc/paginate {:page page :per-page per-page})])})))}}]
   ["/count-collections"
    {:get  {:summary    "Get counts by group"
            :parameters {:query any?}
            :responses  {200 {:body any?}}
            :handler    (fn [{{:keys [query]} :parameters}]
                          (let [group (:group query)
                                query-map (dissoc query :group)]
                            (log/info query-map group query)
                            (ok (dc/count-group-by query-map group))))}}]])