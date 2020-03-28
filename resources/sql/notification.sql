-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.02.16
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: notification
-- Description: Managing notifications for choppy_app, project, report etc.
-- Functions: create-notification!, get-notification-count, search-notifications, delete-notification!
---------------------------------------------------------------------------------------------

-- :name create-notification!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    | key                | required  | description |
    | -------------------|-----------|-------------|
    | :id                | true/uniq | serial id
    | :title             | true      | The title of notification.
    | :description       | false     | A description of notification.
    | :notification_type | true      | Which type the notification is.
    | :created_time      | true      | BigInt
    | :status            | true      | Read, Unread
  Description:
    Create a new notification record and then return the number of affected rows.
  Examples: 
    Clojure: (create-notification! {:title "title" :notification "app-store" :created-time "" :status "Unread"})
  Conditions:
    1. `ON CONFLICT` expression is only support by PostgreSQL
    2. `title` must have an unique or exclusion constrait
*/
INSERT INTO notification (title, description, notification_type, created_time, status)
VALUES (:title, :description, :notification-type, :created-time, :status)
RETURNING id


-- :name get-notification-count
-- :command :query
-- :result :one
/* :doc
  Description:
    Get count.
  Examples:
    Clojure: (get-notification-count)
    SQL: SELECT COUNT(id) FROM notification

    Clojure: (get-notification-count {:query-map {:title "XXX"}})
    HugSQL: SELECT COUNT(id) FROM notification WHERE title = :v:query-map.title
    SQL: SELECT COUNT(id) FROM notification WHERE title = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM notification
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-notifications
-- :command :query
-- :result :many
/* :doc
  Description:
    Get notifications by using query map
  Examples: 
    Clojure: (search-notifications {:query-map {:title "XXX"}})
    HugSQL: SELECT * FROM notification WHERE title = :v:query-map.title
    SQL: SELECT * FROM notification WHERE title = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM notification
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


-- :name delete-notification!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete a notification record given the title
  Examples:
    Clojure: (delete-notification! {:id 1})
    SQL: DELETE FROM notification WHERE id = 1
*/
DELETE
FROM notification
WHERE id = :id
