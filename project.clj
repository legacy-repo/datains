(defproject datains "0.2.5"

  :description "Datains is an web tool for `Reproducible Omics Pipeline `."
  :url "http://datains.3steps.cn/"

  ;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  ;; !!                                   PLEASE KEEP THESE ORGANIZED ALPHABETICALLY                                  !!
  ;; !!                                   AND ADD A COMMENT EXPLAINING THEIR PURPOSE                                  !!
  ;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.namespace "1.0.0"]
                 [org.clojure/tools.cli "0.4.2"
                  :exclusions [org.clojure/clojure]]
                 [org.clojure/tools.logging "0.5.0"
                  :exclusions [org.clojure/clojure]]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [cheshire "5.9.0" :exclusions [org.clojure/clojure]]
                 [clj-http "3.9.1"]                                                 ; HTTP client
                 [clojure.java-time "0.3.2"]
                 [clojurewerkz/quartzite "2.1.0"
                  :exclusions [org.clojure/clojure]]                                ; scheduling library
                 [colorize "0.1.1" :exclusions [org.clojure/clojure]]               ; string output with ANSI color codes (for logging)
                 [conman "0.8.4"
                  :exclusions [org.clojure/java.jdbc
                               org.clojure/clojure]]
                 [cprop "0.1.14" :exclusions [org.clojure/clojure]]
                 [expound "0.7.2"]
                 [funcool/struct "1.4.0"
                  :exclusions [org.clojure/clojure
                               com.google.code.findbugs/jsr305
                               com.google.errorprone/error_prone_annotations]]
                 [luminus-jetty "0.1.7"
                  :exclusions [clj-time joda-time org.clojure/clojure]]
                 [luminus-migrations "0.6.6" :exclusions [org.clojure/clojure]]
                 [luminus-transit "0.1.2" :exclusions [org.clojure/clojure]]
                 [luminus/ring-ttl-session "0.3.3"
                  :exclusions [org.clojure/clojure]]
                 [markdown-clj "1.10.0" :exclusions [org.clojure/clojure]]
                 [metosin/muuntaja "0.6.6"
                  :exclusions [com.fasterxml.jackson.core/jackson-core
                               com.fasterxml.jackson.core/jackson-databind
                               com.fasterxml.jackson.core/jackson-annotations]]
                 [metosin/reitit "0.3.10"
                  :exclusions [clj-time
                               joda-time
                               org.clojure/clojure
                               com.fasterxml.jackson.core/jackson-databind
                               com.fasterxml.jackson.core/jackson-core
                               com.fasterxml.jackson.core/jackson-annotations]]
                 [metosin/ring-http-response "0.9.1"
                  :exclusions [clj-time
                               joda-time
                               org.clojure/clojure]]
                 [mount "0.1.16"]
                 [nrepl "0.6.0"]
                 [org.postgresql/postgresql "42.2.8"]
                 [org.tcrawley/dynapath "1.0.0"]                                    ; Dynamically add Jars (e.g. Oracle or Vertica) to classpath
                 [org.webjars.npm/bulma "0.8.0"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.38"
                  :exclusions [org.slf4j/slf4j-api
                               com.fasterxml.jackson.core/jackson-core]]
                 [prismatic/schema "1.1.11"]                                        ; Data schema declaration and validation library
                 [ring-webjars "0.2.0" :exclusions [org.clojure/clojure]]
                 [ring/ring-core "1.8.0" :exclusions [org.clojure/clojure]]
                 [ring/ring-defaults "0.3.2" :exclusions [org.clojure/clojure]]
                 [ring/ring-servlet "1.7.1"
                  :exclusions [joda-time
                               clj-time
                               org.clojure/clojure]]
                 [selmer "1.12.17" :exclusions [org.clojure/clojure]]
                 [honeysql "1.0.444"]
                 [camel-snake-kebab "0.4.1"]
                 [clj-jgit "1.0.0-beta3"]
                 [ring-cors "0.1.13"]
                 [ring/ring-headers "0.3.0"]
                 [me.raynes/fs "1.4.6"]
                 [danlentz/clj-uuid "0.1.9"]
                 [clj-time "0.15.2"]
                 [lambdaisland/uri "1.2.1"]                                         ; https://github.com/dakrone/clj-http#optional-dependencies
                 [org.clojure/tools.reader "1.3.2"]                                 ; for :as :clojure
                 [digest "1.4.9"]                                                   ; Digest algorithms (md5, sha1 ...) for Clojure
                 [clj-filesystem "0.2.7"]
                 [com.novemberain/monger "3.1.0"]]

  :repositories [["central" "https://maven.aliyun.com/repository/central"]
                 ["jcenter" "https://maven.aliyun.com/repository/jcenter"]
                 ["clojars" "https://mirrors.tuna.tsinghua.edu.cn/clojars/"]
                 ["clojars-official" "https://clojars.org/repo/"]]

  :plugin-repositories [["central" "https://maven.aliyun.com/repository/central"]
                        ["jcenter" "https://maven.aliyun.com/repository/jcenter"]
                        ["clojars" "https://mirrors.tuna.tsinghua.edu.cn/clojars/"]]

  :min-lein-version "2.0.0"

  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot datains.core

  :profiles
  {:uberjar       {:omit-source    false  ; You can't set to true, if you want to make the findnamespace valid (for tasks/events).
                   :aot            :all
                   :uberjar-name   "datains.jar"
                   :source-paths   ["env/prod"]
                   :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev   {:env             {:datains-run-mode "dev"}
                   :jvm-opts        ["-Dconf=dev-config.edn"]
                   :dependencies    [[directory-naming/naming-java "0.8"]
                                     [pjstadig/humane-test-output "0.10.0"]
                                     [prone "2019-07-08"]
                                     [ring/ring-devel "1.8.0" :exclusions [org.clojure/clojure]]
                                     [ring/ring-mock "0.4.0" :exclusions [org.clojure/clojure]]]
                   :plugins         [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                     [jonase/eastwood "0.3.6"]
                                     [cider/cider-nrepl "0.25.3"]
                                     [nubank/lein-jupyter "0.1.18"]]

                   :jupyter-options {:jupyter-path "jupyter"}

                   :source-paths    ["env/dev"]
                   :resource-paths  ["env/dev/resources"]
                   :repl            {:plugins      [[cider/cider-nrepl "0.25.3"]]
                                     :dependencies [[nrepl "0.6.0"]
                                                    [cider/piggieback "0.4.0"]
                                                    [figwheel-sidecar "0.5.18"]]
                                     :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
                   :injections      [(require 'pjstadig.humane-test-output)
                                     (pjstadig.humane-test-output/activate!)]}
   :project/test  {:env            {:datains-run-mode "test"}
                   :jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}
   :profiles/dev  {}
   :profiles/test {}})
