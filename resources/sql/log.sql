-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.02.16
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: datains_log
-- Description: Managing logs for choppy_app, project, report etc.
-- Functions: create-log!, get-log-count, search-logs, delete-log!
---------------------------------------------------------------------------------------------

-- :name create-log!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | serial id
    | :title             | true      | The title of log.
    | :content           | true      | A content of log.
    | :created_time      | true      | BigInt
    | :entity_type       | true      | entity type
    | :entity_id         | true      | entity id
    | :log_type          | true      | Which type the log is.
  Description:
    Create a new log record and then return the number of affected rows.
  Examples: 
    Clojure: (create-log! {:title "title" :content "app-store" :created-time "" :log-type "Unread"})
  Conditions:
    1. `ON CONFLICT` expression is only support by PostgreSQL
    2. `title` must have an unique or exclusion constrait
*/
INSERT INTO datains_log (title, content, created_time, entity_type, entity_id, log_type)
VALUES (:title, :content, :created-time, :entity-type, :entity-id, :log-type)
RETURNING id


-- :name get-log-count
-- :command :query
-- :result :one
/* :doc
  Description:
    Get count.
  Examples:
    Clojure: (get-log-count)
    SQL: SELECT COUNT(id) FROM datains_log

    Clojure: (get-log-count {:query-map {:title "XXX"}})
    HugSQL: SELECT COUNT(id) FROM datains_log WHERE title = :v:query-map.title
    SQL: SELECT COUNT(id) FROM datains_log WHERE title = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM datains_log
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-logs
-- :command :query
-- :result :many
/* :doc
  Description:
    Get logs by using query map
  Examples: 
    Clojure: (search-logs {:query-map {:title "XXX"}})
    HugSQL: SELECT * FROM datains_log WHERE title = :v:query-map.title
    SQL: SELECT * FROM datains_log WHERE title = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM datains_log
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

-- :name update-log!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:log-type ""} :id "3"}
  Description: 
    Update an existing log record.
  Examples:
    Clojure: (update-log! {:updates {:log-type "log-type"} :id "3"})
    HugSQL: UPDATE datains_log SET log-type = :v:query-map.log-type WHERE id = :id
    SQL: UPDATE datains_log SET log-type = "log-type" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE datains_log
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id

-- :name delete-log!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete a log record given the title
  Examples:
    Clojure: (delete-log! {:id 1})
    SQL: DELETE FROM datains_log WHERE id = 1
*/
DELETE
FROM datains_log
WHERE id = :id
