; (ns datains.test.handler
;   (:require
;     [clojure.test :refer :all]
;     [ring.mock.request :refer :all]
;     [datains.handler :refer :all]
;     [datains.middleware.formats :as formats]
;     [muuntaja.core :as m]
;     [mount.core :as mount]))

; (defn parse-json [body]
;   (m/decode formats/instance "application/json" body))

; (use-fixtures
;   :once
;   (fn [f]
;     (mount/start #'datains.config/env
;                  #'datains.handler/app-routes)
;     (f)))

; (deftest test-app
;   (testing "main route"
;     (let [response ((app) (request :get "/api/apps"))]
;       (is (= 200 (:status response)))))

;   (testing "not-found route"
;     (let [response ((app) (request :get "/invalid"))]
;       (is (= 404 (:status response)))))
  
;   (testing "services"
;     (testing "success"
;       (let [response ((app) (-> (request :post "/api/apps")
;                                 (json-body {:id          "1"
;                                             :icon        "Sam"
;                                             :cover       "Smith"
;                                             :title       "exceRptSmallRNA"
;                                             :description "exceRptSmallRNA"
;                                             :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
;                                             :author      "chenziyin"
;                                             :rate        "5"})))]
;         (is (= 201 (:status response)))
;         (is (= {:message 1} (m/decode-response-body response)))))

;     (testing "parameter coercion error"
;       (let [response ((app) (-> (request :post "/api/apps")
;                                 (json-body {:id          2
;                                             :icon        "Sam"
;                                             :cover       "Smith"
;                                             :title       "exceRptSmallRNA"
;                                             :description "exceRptSmallRNA"
;                                             :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
;                                             :author      "chenziyin"
;                                             :rate        5})))]
;         (is (= 400 (:status response)))))

;     (testing "content negotiation"
;       (let [response ((app) (-> (request :post "/api/apps")
;                                 (body (pr-str {:id          "3"
;                                                :icon        "Sam"
;                                                :cover       "Smith"
;                                                :title       "exceRptSmallRNA"
;                                                :description "exceRptSmallRNA"
;                                                :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
;                                                :author      "chenziyin"
;                                                :rate        "5"}))
;                                 (content-type "application/edn")
;                                 (header "accept" "application/transit+json")))]
;         (is (= 201 (:status response)))
;         (is (= {:message 1} (m/decode-response-body response)))))))
