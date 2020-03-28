-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.03.27
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: report
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
    | :report-name       | true      | The report name, required, [a-zA-Z0-9]+
    | :project-id        | false     | The id  of the related project
    | :script            | false     | Auto generated script for making a report
    | :description       | false     | A description of the report
    | :started-time      | true      | Bigint
    | :finished-time     | false     | Bigint
    | :checked-time      | false     | Bigint
    | :archived-time     | false     | Bigint
    | :report_path       | false     | A relative path of a report based on the report directory
    | :report_type       | true      | multiqc
    | :status            | true      | Started, Finished, Checked, Archived
  Description:
    Create a new report record and then return the number of affected rows.
  Examples: 
    Clojure: (create-report! {})
*/
INSERT INTO report (id, report_name, project_id, script, started_time, finished_time, checked_time, archived_time, report_path, log, status)
VALUES (:id, :report-name, :project-id, :script, :started-time, :finished-time, :checked-time, :archived-time, :report-path, :log, :status)
RETURNING id


-- :name update-report!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :finished-time ""} :id "3"}
  Description: 
    Update an existing report record.
  Examples:
    Clojure: (update-report! {:updates {:finished-time "finished-time" :status "status"} :id "3"})
    HugSQL: UPDATE report SET finished_time = :v:query-map.finished-time,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE report SET finished_time = "finished_time", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE report
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
    SQL: SELECT COUNT(id) FROM report

    Clojure: (get-report-count {:query-map {:status "XXX"}})
    HugSQL: SELECT COUNT(id) FROM report WHERE status = :v:query-map.status
    SQL: SELECT COUNT(id) FROM report WHERE status = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM report
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
    HugSQL: SELECT * FROM report WHERE status = :v:query-map.status
    SQL: SELECT * FROM report WHERE status = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM report
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
      SELECT  report.id,
              report.report_name,
              report.project_id,
              report.script,
              report.started_time,
              report.finished_time,
              report.checked_time,
              report.archived_time,
              report.report_path,
              report.log,
              report.status
              array_agg( tag.id ) as tag_ids,
              array_agg( tag.title ) as tags
      FROM entity_tag
      JOIN report ON entity_tag.entity_id = report.id
      JOIN tag ON entity_tag.tag_id = tag.id
      WHERE report.status = :v:query-map.status
      GROUP BY report.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add entity_tag.entity_type = "report" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  report.id,
        report.report_name,
        report.project_id,
        report.script,
        report.started_time,
        report.finished_time,
        report.checked_time,
        report.archived_time,
        report.report_path,
        report.log,
        report.status
        array_agg( tag.id ) as tag_ids,
        array_agg( tag.title ) as tags
FROM entity_tag
JOIN report ON entity_tag.entity_id = report.id
JOIN tag ON entity_tag.tag_id = tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "report."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY report.id
ORDER BY report.id
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
    SQL: DELETE FROM report WHERE id = "XXX"
*/
DELETE
FROM report
WHERE id = :id


-- :name delete-all-reports!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all report records.
  Examples:
    Clojure: (delete-all-reports!)
    SQL: TRUNCATE report;
*/
TRUNCATE report;
