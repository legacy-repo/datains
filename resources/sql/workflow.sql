-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: datains_workflow
-- Description: Managing workflows
-- Functions: create-workflow!, update-workflow!, get-workflow-count, search-workflows, delete-workflow!
---------------------------------------------------------------------------------------------

-- :name create-workflow!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | UUID string
    | :project-name      | true      | The project name, required, [a-zA-Z0-9]+
    | :sample-id         | true      | A unique index in the specified project.
    | :submitted-time    | true      | Bigint
    | :started-time      | true      | Bigint
    | :finished-time     | false     | Bigint
    | :job-params        | false     | JSON string, the parameters be used to render inputs file.
    | :labels            | false     | JSON string, the labels be used to label workflow.
    | :status            | true      | Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold
  Description:
    Create a new workflow record and then return the number of affected rows.
  Examples: 
    Clojure: (create-workflow! {:id "id" :project-name "project-name" :sample-id "" :job-params "" :labels "" :status "status"})
*/
INSERT INTO datains_workflow (id, project_name, sample_id, submitted_time, started_time, finished_time, job_params, labels, status)
VALUES (:id, :project-name, :sample-id, :submitted-time, :started-time, :finished-time, :job-params, :labels, :status)
RETURNING id


-- :name update-workflow!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished-time ""} :id "3"}
  Description: 
    Update an existing workflow record.
  Examples:
    Clojure: (update-workflow! {:updates {:finished-time "finished-time" :status "status"} :id "3"})
    HugSQL: UPDATE datains_workflow SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE datains_workflow SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE datains_workflow
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id


-- :name get-workflow-count
-- :command :query
-- :result :one
/* :doc
  Args:
    {:query-map {:status "XXX"}}
  Description:
    Get count.
  Examples:
    Clojure: (get-workflow-count)
    SQL: SELECT COUNT(id) FROM datains_workflow

    Clojure: (get-workflow-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM datains_workflow WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM datains_workflow WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM datains_workflow
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-workflows
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get workflows by using query map
  Examples: 
    Clojure: (search-workflows {:query-map {:status "XXX"}})
    HugSQL: SELECT * FROM datains_workflow WHERE status = :v:query-map.status
    SQL: SELECT * FROM datains_workflow WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM datains_workflow
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


-- :name search-workflows-with-tags
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get workflows with tags by using query map
  Examples: 
    Clojure: (search-workflows-with-tags {:query-map {:status "XXX"}})
    HugSQL:
      SELECT  datains_workflow.id,
              datains_workflow.project_name,
              datains_workflow.sample_id,
              datains_workflow.submitted_time,
              datains_workflow.started_time,
              datains_workflow.finished_time,
              datains_workflow.job_params,
              datains_workflow.labels,
              datains_workflow.status
              array_agg( datains_tag.id ) as tag_ids,
              array_agg( datains_tag.title ) as tags
      FROM datains_entity_tag
      JOIN datains_workflow ON datains_entity_tag.entity_id = datains_workflow.id
      JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
      WHERE datains_workflow.project_name = :v:query-map.project_name
      GROUP BY datains_workflow.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add datains_entity_tag.entity_type = "workflow" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  datains_workflow.id,
        datains_workflow.project_name,
        datains_workflow.sample_id,
        datains_workflow.submitted_time,
        datains_workflow.started_time,
        datains_workflow.finished_time,
        datains_workflow.job_params,
        datains_workflow.labels,
        datains_workflow.status
        array_agg( datains_tag.id ) as tag_ids,
        array_agg( datains_tag.title ) as tags
FROM datains_entity_tag
JOIN datains_workflow ON datains_entity_tag.entity_id = datains_workflow.id
JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "datains_workflow."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY datains_workflow.id
ORDER BY datains_workflow.id
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name delete-workflow!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:id "XXX"}
  Description:
    Delete a workflow record given the id
  Examples:
    Clojure: (delete-workflow! {:id "XXX"})
    SQL: DELETE FROM datains_workflow WHERE id = "XXX"
*/
DELETE
FROM datains_workflow
WHERE id = :id


-- :name delete-all-workflows!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all workflow records.
  Examples:
    Clojure: (delete-all-workflows!)
    SQL: TRUNCATE datains_workflow;
*/
TRUNCATE datains_workflow;
