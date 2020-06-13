(ns datains.gitter.core
  (:require [clj-jgit.porcelain :as c]
            [clj-jgit.querying :as q]
            [clj-jgit.internal :as i]
            [datains.adapters.file-manager.local :as local]
            [datains.adapters.file-manager.fs :as fs]
            [clojure.string :as string])
  (:import (java.io File FileNotFoundException IOException)
           [org.eclipse.jgit.treewalk TreeWalk]
           [org.eclipse.jgit.revwalk RevWalk]
           [org.eclipse.jgit.lib ObjectId]
           [org.eclipse.jgit.api Git]
           [org.eclipse.jgit.lib RepositoryBuilder]))

(defn exist-commit?
  [path commit-ish]
  (c/with-repo path (q/find-rev-commit repo rev-walk commit-ish)))

(defn build-repo
  [path]
  (if-let [git-dir (c/discover-repo path)]
    (-> (RepositoryBuilder.)
        (.setGitDir git-dir)
        (.readEnvironment)
        (.findGitDir)
        (.build))
    (throw
     (FileNotFoundException. (str "The Git repository at '" path "' could not be located.")))))

(defn build-rev-commit
  [repo commit-ish]
  (.parseCommit (RevWalk. repo) (ObjectId/fromString commit-ish)))

(defn build-tree-walk
  [repo path tree]
  (TreeWalk/forPath repo path tree))

(defn new-tree-walk
  [repo path rev-commit]
  (if path
    (let [dir-walk  (TreeWalk. repo)
          tree-walk (build-tree-walk repo path (.getTree rev-commit))]
      (.addTree dir-walk (.getObjectId tree-walk 0))
      (.setRecursive tree-walk false)
      dir-walk)
    (let [tree-walk (i/new-tree-walk (Git. repo) rev-commit)]
      (.setRecursive tree-walk false)
      tree-walk)))

(defn query-by-treewalk
  [path commit-ish f & args]
  (let [repo       (build-repo path)
        rev-commit (build-rev-commit repo commit-ish)
        args       (concat [repo rev-commit] args)]
    (apply f args)))

(defn list-files
  [repo rev-commit subpath]
  (let [tree-walk (new-tree-walk repo subpath rev-commit)]
    (loop [results []]
      (if (not (.next tree-walk))
        results
        (recur (conj results (.getPathString tree-walk)))))))

(defn list-files-details
  [path commit-ish subpath]
  (let [base-dir (if subpath (fs/join-paths path subpath) path)]
  (->> (query-by-treewalk path commit-ish list-files subpath)
       (map #(local/file-details (fs/join-paths base-dir %) base-dir)))))
