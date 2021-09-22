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
    | :project_id        | true      | The project id, required, uuid
    | :workflow_id       | false     | The workflow id from cromwell instance.
    | :sample_id         | true      | A unique index in the specified project.
    | :submitted_time    | true      | Bigint
    | :started_time      | true      | Bigint
    | :finished_time     | false     | Bigint
    | :job_params        | true      | JSON string, the parameters be used to render inputs file.
    | :labels            | true      | JSON string, the labels be used to label workflow. default: { sample-id: "", project-name: ""}
    | :status            | true      | Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold
  Description:
    Create a new workflow record and then return the number of affected rows.
  Examples: 
    Clojure: (create-workflow! {:id "id" :project_id "project-id" :sample_id "" :job_params "" :labels "" :status "status"})
*/
INSERT INTO datains_workflow (id, project_id, workflow_id, sample_id, submitted_time, started_time, finished_time, job_params, labels, status, percentage)
VALUES (:id, :project_id, :workflow_id, :sample_id, :submitted_time, :started_time, :finished_time, :job_params, :labels, :status, :percentage)
RETURNING id


-- :name update-workflow!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished_time ""} :id "3"}
  Description: 
    Update an existing workflow record.
  Examples:
    Clojure: (update-workflow! {:updates {:finished_time "finished-time" :status "status"} :id "3"})
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
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT COUNT(id)
FROM datains_workflow
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options)
  (:where-clause params) ":snip:where-clause")
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
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT * 
FROM datains_workflow
/*~
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options)
  (:where-clause params) ":snip:where-clause")
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
              datains_workflow.project_id,
              datains_workflow.workflow_id,
              datains_workflow.sample_id,
              datains_workflow.submitted_time,
              datains_workflow.started_time,
              datains_workflow.finished_time,
              datains_workflow.job_params,
              datains_workflow.labels,
              datains_workflow.status,
              datains_workflow.percentage
              array_agg( datains_tag.id ) as tag_ids,
              array_agg( datains_tag.title ) as tags
      FROM datains_entity_tag
      JOIN datains_workflow ON datains_entity_tag.entity_id = datains_workflow.id
      JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
      WHERE datains_workflow.project_id = :v:query-map.project_id
      GROUP BY datains_workflow.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add datains_entity_tag.entity_type = "workflow" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  datains_workflow.id,
        datains_workflow.project_id,
        datains_workflow.workflow_id,
        datains_workflow.sample_id,
        datains_workflow.submitted_time,
        datains_workflow.started_time,
        datains_workflow.finished_time,
        datains_workflow.job_params,
        datains_workflow.labels,
        datains_workflow.status,
        datains_workflow.percentage
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


-- :name search-workflows-with-projects
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get workflows with projects by using query map
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT  datains_workflow.id,
        datains_workflow.project_id,
        datains_workflow.workflow_id,
        datains_workflow.sample_id,
        datains_workflow.submitted_time,
        datains_workflow.started_time,
        datains_workflow.finished_time,
        datains_workflow.job_params,
        datains_workflow.labels,
        datains_workflow.status,
        datains_workflow.percentage,
				datains_project.app_id,
				datains_project.app_name,
				datains_project.author,
				datains_project.group_name,
				datains_project.project_name,
        datains_project.description
FROM datains_workflow
INNER JOIN datains_project ON datains_workflow.project_id = datains_project.id
/*~
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options "datains_workflow")
  (:where-clause params) ":snip:where-clause")
~*/
ORDER BY datains_workflow.submitted_time DESC
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name count-workflow-with-status
-- :command :query
-- :result :many
/* :doc
  Args: {:query-map {:project_id "XXX"}}
*/
/* :require [datains.db.sql-helper :as sql-helper] */
SELECT count(*), status
FROM datains_workflow
/*~
(cond
  (:query-map params) (sql-helper/where-clause (:query-map params) options "datains_workflow")
  (:where-clause params) ":snip:where-clause")
~*/
GROUP BY status


-- :name get-finished-project
-- :command :query
-- :result :many
/* :doc
  Args: nil
*/
WITH not_finished AS (
	SELECT project_id
	FROM datains_workflow
	WHERE status IN ('Submitted', 'Aborting', 'On Hold', 'Running', 'Failed')
)
SELECT id
FROM not_finished
RIGHT JOIN datains_project
ON not_finished.project_id = datains_project.id
WHERE not_finished.project_id IS NULL AND datains_project.finished_time IS NULL
