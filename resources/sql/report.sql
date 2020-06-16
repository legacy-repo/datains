-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: datains_report
-- Description: Managing reports
-- Functions: create-report!, update-report!, get-report-count, search-reports, delete-report!
---------------------------------------------------------------------------------------------

-- :name create-report!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | UUID string
    | :report_name       | true      | The report name, required, [a-zA-Z0-9]+
    | :project_id        | false     | The id  of the related project
    | :script            | false     | Auto generated script for making a report
    | :description       | false     | A description of the report
    | :started_time      | true      | Bigint
    | :finished_time     | false     | Bigint
    | :checked_time      | false     | Bigint
    | :archived_time     | false     | Bigint
    | :report_path       | false     | A relative path of a report based on the report directory
    | :report_type       | true      | multiqc
    | :status            | true      | Started, Finished, Checked, Archived
  Description:
    Create a new report record and then return the number of affected rows.
  Examples: 
    Clojure: (create-report! {})
*/
INSERT INTO datains_report (id, report_name, project_id, script, started_time, finished_time, checked_time, archived_time, report_path, log, status)
VALUES (:id, :report_name, :project_id, :script, :started_time, :finished_time, :checked_time, :archived_time, :report_path, :log, :status)
RETURNING id


-- :name update-report!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished_time ""} :id "3"}
  Description: 
    Update an existing report record.
  Examples:
    Clojure: (update-report! {:updates {:finished_time "finished-time" :status "status"} :id "3"})
    HugSQL: UPDATE datains_report SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE datains_report SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE datains_report
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id


-- :name get-report-count
-- :command :query
-- :result :one
/* :doc
  Args:
    {:query-map {:status "XXX"}}
  Description:
    Get count.
  Examples:
    Clojure: (get-report-count)
    SQL: SELECT COUNT(id) FROM datains_report

    Clojure: (get-report-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM datains_report WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM datains_report WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM datains_report
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-reports
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get reports by using query map
  Examples: 
    Clojure: (search-reports {:query-map {:status "XXX"}})
    HugSQL: SELECT * FROM datains_report WHERE status = :v:query-map.status
    SQL: SELECT * FROM datains_report WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM datains_report
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


-- :name search-reports-with-tags
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:status "XXX"} :limit 1 :offset 0}
  Description:
    Get reports with tags by using query map
  Examples: 
    Clojure: (search-reports-with-tags {:query-map {:status "XXX"}})
    HugSQL:
      SELECT  datains_report.id,
              datains_report.report_name,
              datains_report.project_id,
              datains_report.script,
              datains_report.started_time,
              datains_report.finished_time,
              datains_report.checked_time,
              datains_report.archived_time,
              datains_report.report_path,
              datains_report.log,
              datains_report.status
              array_agg( datains_tag.id ) as tag_ids,
              array_agg( datains_tag.title ) as tags
      FROM datains_entity_tag
      JOIN datains_report ON datains_entity_tag.entity_id = datains_report.id
      JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
      WHERE datains_report.status = :v:query-map.status
      GROUP BY datains_report.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add datains_entity_tag.entity_type = "report" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  datains_report.id,
        datains_report.report_name,
        datains_report.project_id,
        datains_report.script,
        datains_report.started_time,
        datains_report.finished_time,
        datains_report.checked_time,
        datains_report.archived_time,
        datains_report.report_path,
        datains_report.log,
        datains_report.status
        array_agg( datains_tag.id ) as tag_ids,
        array_agg( datains_tag.title ) as tags
FROM datains_entity_tag
JOIN datains_report ON datains_entity_tag.entity_id = datains_report.id
JOIN datains_tag ON datains_entity_tag.tag_id = datains_tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "datains_report."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY datains_report.id
ORDER BY datains_report.id
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name delete-report!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:id "XXX"}
  Description:
    Delete a report record given the id
  Examples:
    Clojure: (delete-report! {:id "XXX"})
    SQL: DELETE FROM datains_report WHERE id = "XXX"
*/
DELETE
FROM datains_report
WHERE id = :id


-- :name delete-all-reports!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all report records.
  Examples:
    Clojure: (delete-all-reports!)
    SQL: TRUNCATE datains_report;
*/
TRUNCATE datains_report;
