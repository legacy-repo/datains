-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: datains_project
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
    | :project_name      | true      | The project name, required, [a-zA-Z0-9]+
    | :description       | false     | A description of the project
    | :app_id            | true      | Id of the related app
    | :app_name          | true      | Name of the related app
    | :author            | true      | The author of creating project
    | :group_name        | false     | The team name
    | :started_time      | true      | Bigint
    | :finished_time     | false     | Bigint
    | :samples           | true      | JSON string
    | :status            | true      | Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold
  Description:
    Create a new project record and then return the number of affected rows.
  Examples: 
    Clojure: (create-project! {:id "id" :project_name "project-name" :description "description" :app_id "app-id" :app_name "app-name" :author "author" :group_name "group" :started_time "started-time" :status "status"})
*/
INSERT INTO datains_project (id, project_name, description, app_id, app_name, author, group_name, started_time, finished_time, samples, status, percentage)
VALUES (:id, :project_name, :description, :app_id, :app_name, :author, :group_name, :started_time, :finished_time, :samples, :status, :percentage)
RETURNING id


-- :name update-project!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished_time ""} :id "3"}
  Description: 
    Update an existing project record.
  Examples:
    Clojure: (update-project! {:updates {:finished_time "finished-time" :status "status"} :id "3"})
    HugSQL: UPDATE datains_project SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE datains_project SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE datains_project
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
    SQL: SELECT COUNT(id) FROM datains_project

    Clojure: (get-project-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM datains_project WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM datains_project WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT COUNT(id)
FROM datains_project
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options)
  (:where-clause params) ":snip:where-clause")
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
    HugSQL: SELECT * FROM datains_project WHERE status = :v:query-map.status
    SQL: SELECT * FROM datains_project WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT * 
FROM datains_project
/*~
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options)
  (:where-clause params) ":snip:where-clause")
~*/
ORDER BY started_time DESC
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
      SELECT  datains_project.id,
              datains_project.project_name,
              datains_project.description,
              datains_project.app_id,
              datains_project.app_name,
              datains_project.author,
              datains_project.group_name,
              datains_project.started_time,
              datains_project.finished_time,
              datains_project.samples,
              datains_project.status,
              datains_project.percentage
              array_agg( datains_tag.id ) as tag_ids,
              array_agg( datains_tag.title ) as tags
      FROM datains_entity_tag
      JOIN datains_project ON datains_entity_tag.entity_id = datains_project.id
      JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
      WHERE datains_project.project_name = :v:query-map.project_name
      GROUP BY datains_project.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add datains_entity_tag.entity_type = "project" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  datains_project.id,
        datains_project.project_name,
        datains_project.description,
        datains_project.app_id,
        datains_project.app_name,
        datains_project.author,
        datains_project.group_name,
        datains_project.started_time,
        datains_project.finished_time,
        datains_project.samples,
        datains_project.status,
        datains_project.percentage
        array_agg( datains_tag.id ) as tag_ids,
        array_agg( datains_tag.title ) as tags
FROM datains_entity_tag
JOIN datains_project ON datains_entity_tag.entity_id = datains_project.id
JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "datains_project."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY datains_project.id
ORDER BY datains_project.id DESC
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
    SQL: DELETE FROM datains_project WHERE id = "XXX"
*/
DELETE
FROM datains_project
WHERE id = :id


-- :name delete-all-projects!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all project records.
  Examples:
    Clojure: (delete-all-projects!)
    SQL: TRUNCATE datains_project;
*/
TRUNCATE datains_project;
