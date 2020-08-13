(ns datains.api.data-commons
  (:require
   [ring.util.http-response :refer [ok]]
   [datains.api.data-commons-spec :as data-commons-spec]
   [datains.adapters.data-commons.core :as dc]))

(def data-commons
  [""
   {:swagger {:tags ["Data Commons"]}}

   ["/collections"
    {:get  {:summary    "Get collections"
            :parameters {:query data-commons-spec/collection-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{:keys [query]} :parameters}]
                          (let [page (if (:page query) (:page query) 1)
                                per-page (if (:per_page query) (:per_page query) 10)
                                query-map (dissoc query :page :per_page)]
                            (ok {:total (dc/count-coll query-map)
                                 :page page
                                 :per_page per-page
                                 :data (dc/query [(dc/find-coll query-map)
                                                  (dc/paginate {:page page :per-page per-page})])})))}}]
   ["/count-collections"
    {:get  {:summary    "Get counts by group"
            :parameters {:query {:group string?}}
            :responses  {200 {:body any?}}
            :handler    (fn [{{:keys [query]} :parameters}]
                          (let [group (:group query)]
                            (ok (dc/count-group-by group))))}}]])