-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: workflow
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
INSERT INTO workflow (id, project_name, sample_id, submitted_time, started_time, finished_time, job_params, labels, status)
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
    HugSQL: UPDATE workflow SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE workflow SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE workflow
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
    SQL: SELECT COUNT(id) FROM workflow

    Clojure: (get-workflow-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM workflow WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM workflow WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM workflow
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
    HugSQL: SELECT * FROM workflow WHERE status = :v:query-map.status
    SQL: SELECT * FROM workflow WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM workflow
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
      SELECT  workflow.id,
              workflow.project_name,
              workflow.sample_id,
              workflow.submitted_time,
              workflow.started_time,
              workflow.finished_time,
              workflow.job_params,
              workflow.labels,
              workflow.status
              array_agg( tag.id ) as tag_ids,
              array_agg( tag.title ) as tags
      FROM entity_tag
      JOIN workflow ON entity_tag.entity_id = workflow.id
      JOIN tag ON entity_tag.tag_id = tag.id
      WHERE workflow.project_name = :v:query-map.project_name
      GROUP BY workflow.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add entity_tag.entity_type = "workflow" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  workflow.id,
        workflow.project_name,
        workflow.sample_id,
        workflow.submitted_time,
        workflow.started_time,
        workflow.finished_time,
        workflow.job_params,
        workflow.labels,
        workflow.status
        array_agg( tag.id ) as tag_ids,
        array_agg( tag.title ) as tags
FROM entity_tag
JOIN workflow ON entity_tag.entity_id = workflow.id
JOIN tag ON entity_tag.tag_id = tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "workflow."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY workflow.id
ORDER BY workflow.id
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
    SQL: DELETE FROM workflow WHERE id = "XXX"
*/
DELETE
FROM workflow
WHERE id = :id


-- :name delete-all-workflows!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all workflow records.
  Examples:
    Clojure: (delete-all-workflows!)
    SQL: TRUNCATE workflow;
*/
TRUNCATE workflow;
