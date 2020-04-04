(ns datains.gitter.core
  (:require [clj-jgit.porcelain :as c]
            [clj-jgit.querying :as q]
            [clj-jgit.internal :as i]
            [datains.adapters.file-manager.local :as local]
            [clojure.string :as string]))

(defn search-commit
  [path commit-ish]
  (c/with-repo path (q/find-rev-commit repo rev-walk commit-ish)))

(defn list-files
  ([path commit-ish recursive?]
   (c/with-repo path
     (let [tree-walk (i/new-tree-walk
                      repo
                      (q/find-rev-commit repo rev-walk commit-ish))]
       (.setRecursive tree-walk recursive?)
       (loop [results   []]
         (if (.next tree-walk)
           (recur (conj results (.getPathString tree-walk)))
           results)))))
  ([path commit-ish] (list-files path commit-ish true)))

(defn list-files-details
  [path commit-ish recursive?]
  (->> (list-files path commit-ish recursive?)
       (map #(local/file-details (str path "/" %) path))))
