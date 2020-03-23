-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.02.16
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: tag
-- Description: Managing tags for choppy_app, project, report etc.
-- Functions: create-tag!, get-tag-count, search-tags, delete-tag!
---------------------------------------------------------------------------------------------

-- :name create-tag!
-- :command :insert
-- :result :affected
/* :doc
  Description:
    Create a new tag record and then return the number of affected rows.
  Examples: 
    Clojure: (create-tag! {:title "title"})
  Conditions:
    1. `ON CONFLICT` expression is only support by PostgreSQL
    2. `title` must have an unique or exclusion constrait
*/
INSERT INTO tag (title)
VALUES (:title)
ON CONFLICT (title) DO UPDATE SET title = :title;


-- :name get-tag-count
-- :command :query
-- :result :one
/* :doc
  Description:
    Get count.
  Examples:
    Clojure: (get-tag-count)
    SQL: SELECT COUNT(id) FROM tag

    Clojure: (get-tag-count {:query-map {:title "XXX"}})
    HugSQL: SELECT COUNT(id) FROM tag WHERE title = :v:query-map.title
    SQL: SELECT COUNT(id) FROM tag WHERE title = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM tag
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-tags
-- :command :query
-- :result :many
/* :doc
  Description:
    Get tags by using query map
  Examples: 
    Clojure: (search-tags {:query-map {:title "XXX"}})
    HugSQL: SELECT * FROM tag WHERE title = :v:query-map.title
    SQL: SELECT * FROM tag WHERE title = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM tag
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


-- :name delete-tag!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete a tag record given the title
  Examples:
    Clojure: (delete-tag! {:title "XXX"})
    SQL: DELETE FROM tag WHERE title = "XXX"
*/
DELETE
FROM tag
WHERE title = :title


-- :name connect-entity-tag!
-- :command :insert
-- :result :affected
/* :doc
  Args: 
    {:tag-id 1 :entity-id "XXX" :entity-type "choppy-app"}
    {:entity-id "XXX" :entity-type "choppy-app" :tag-title "XXX"}
  Description:
    Connect an app record with several tag records and then return the number of affected rows.
  Examples: 
    Clojure: (connect-entity-tag! {:tag-id 1 :entity-id "test" :entity-type "choppy-app"})
*/
INSERT INTO entity_tag (tag_id, entity_id, entity_type)
/*~
(if (and (:tag-id params) (and (:entity-id params) (:entity-type params)))
  "VALUES (:tag-id, :entity-id, :entity-type)"
  "SELECT id, :entity-id, :entity-type FROM tag WHERE title = :tag-title;")
~*/