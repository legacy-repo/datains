-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: project
-- Description: Managing projects
-- Functions: create-project!, update-project!, get-project-count, search-projects, delete-project!
---------------------------------------------------------------------------------------------

-- :name create-project!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | UUID string
    | :project-name      | true      | The project name, required, [a-zA-Z0-9]+
    | :description       | false     | A description of the project
    | :app-id            | true      | Id of the related app
    | :app-name          | true      | Name of the related app
    | :author            | true      | The author of creating project
    | :group-name        | false     | The team name
    | :started-time      | true      | Bigint
    | :finished-time     | false     | Bigint
    | :status            | true      | Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold
  Description:
    Create a new project record and then return the number of affected rows.
  Examples: 
    Clojure: (create-project! {:id "id" :project-name "project-name" :description "description" :app-id "app-id" :app-name "app-name" :author "author" :group-name "group" :started-time "started-time" :status "status"})
*/
INSERT INTO project (id, project_name, description, app_id, app_name, author, group_name, started_time, finished_time, status)
VALUES (:id, :project-name, :description, :app-id, :app-name, :author, :group-name, :started-time, :finished-time, :status)
RETURNING id


-- :name update-project!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished-time ""} :id "3"}
  Description: 
    Update an existing project record.
  Examples:
    Clojure: (update-project! {:updates {:finished-time "finished-time" :status "status"} :id "3"})
    HugSQL: UPDATE project SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE project SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE project
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id


-- :name get-project-count
-- :command :query
-- :result :one
/* :doc
  Args:
    {:query-map {:status "XXX"}}
  Description:
    Get count.
  Examples:
    Clojure: (get-project-count)
    SQL: SELECT COUNT(id) FROM project

    Clojure: (get-project-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM project WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM project WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM project
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-projects
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get projects by using query map
  Examples: 
    Clojure: (search-projects {:query-map {:status "XXX"}})
    HugSQL: SELECT * FROM project WHERE status = :v:query-map.status
    SQL: SELECT * FROM project WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM project
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/
ORDER BY id
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name search-projects-with-tags
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get projects with tags by using query map
  Examples: 
    Clojure: (search-projects-with-tags {:query-map {:status "XXX"}})
    HugSQL:
      SELECT  project.id,
              project.project_name,
              project.description,
              project.app_id,
              project.app_name,
              project.author,
              project.group_name,
              project.started_time,
              project.finished_time,
              project.status
              array_agg( tag.id ) as tag_ids,
              array_agg( tag.title ) as tags
      FROM entity_tag
      JOIN project ON entity_tag.entity_id = project.id
      JOIN tag ON entity_tag.tag_id = tag.id
      WHERE project.project_name = :v:query-map.project_name
      GROUP BY project.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add entity_tag.entity_type = "project" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  project.id,
        project.project_name,
        project.description,
        project.app_id,
        project.app_name,
        project.author,
        project.group_name,
        project.started_time,
        project.finished_time,
        project.status
        array_agg( tag.id ) as tag_ids,
        array_agg( tag.title ) as tags
FROM entity_tag
JOIN project ON entity_tag.entity_id = project.id
JOIN tag ON entity_tag.tag_id = tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "project."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY project.id
ORDER BY project.id
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name delete-project!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:id "XXX"}
  Description:
    Delete a project record given the id
  Examples:
    Clojure: (delete-project! {:id "XXX"})
    SQL: DELETE FROM project WHERE id = "XXX"
*/
DELETE
FROM project
WHERE id = :id


-- :name delete-all-projects!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all project records.
  Examples:
    Clojure: (delete-all-projects!)
    SQL: TRUNCATE project;
*/
TRUNCATE project;
