-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2021.04.21
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: datains_message
-- Description: Managing messages for choppy_app, project, report etc.
-- Functions: create-message!, get-message-count, search-messages, delete-message!
---------------------------------------------------------------------------------------------

-- :name create-message!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | serial id
    | :title             | true      | The title of message.
    | :description       | false     | A description of message.
    | :payload           | true      | More details with message.
    | :message_type      | true      | Which type the message is. request-materials, request-data
    | :created_time      | true      | BigInt
    | :status            | true      | Succeeded, Submitted, Running
  Description:
    Create a new message record and then return the number of affected rows.
  Examples: 
    Clojure: (create-message! {:title "title" :message "app-store" :paylad {} :created_time "" :status "Submitted"})
  Conditions:
    1. `ON CONFLICT` expression is only support by PostgreSQL
    2. `title` must have an unique or exclusion constrait
*/
INSERT INTO datains_message (title, description, payload, message_type, created_time, status)
VALUES (:title, :description, :payload, :message_type, :created_time, :status)
RETURNING id


-- :name get-message-count
-- :command :query
-- :result :one
/* :doc
  Description:
    Get count.
  Examples:
    Clojure: (get-message-count)
    SQL: SELECT COUNT(id) FROM datains_message

    Clojure: (get-message-count {:query-map {:title "XXX"}})
    HugSQL: SELECT COUNT(id) FROM datains_message WHERE title = :v:query-map.title
    SQL: SELECT COUNT(id) FROM datains_message WHERE title = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM datains_message
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-messages
-- :command :query
-- :result :many
/* :doc
  Description:
    Get messages by using query map
  Examples: 
    Clojure: (search-messages {:query-map {:title "XXX"}})
    HugSQL: SELECT * FROM datains_message WHERE title = :v:query-map.title
    SQL: SELECT * FROM datains_message WHERE title = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM datains_message
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

-- :name update-message!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:status "status" :message_type ""} :id "3"}
  Description: 
    Update an existing message record.
  Examples:
    Clojure: (update-message! {:updates {:message_type "message-type" :status "status"} :id "3"})
    HugSQL: UPDATE datains_message SET message-type = :v:query-map.message-type,status = :v:query-map.status WHERE id = :id
    SQL: UPDATE datains_message SET message-type = "message-type", status = "status" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE datains_message
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id

-- :name delete-message!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete a message record given the title
  Examples:
    Clojure: (delete-message! {:id 1})
    SQL: DELETE FROM datains_message WHERE id = 1
*/
DELETE
FROM datains_message
WHERE id = :id
