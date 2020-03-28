-- Author: Jingcheng Yang <yjcyxky@163.com>
-- Date: 2020.02.16
-- License: See the details in license.md

---------------------------------------------------------------------------------------------
-- Table Name: choppy_app
-- Description: Managing choppy apps
-- Functions: create-app!, update-app!, get-app-count, search-apps, delete-app!
---------------------------------------------------------------------------------------------

-- :name create-app!
-- :command :returning-execute
-- :result :affected
/* :doc
  Args:
    {:id "id" :icon "icon" :cover "cover" :title "title" :description "description" :repo-url "repo_url" :author "author" :rate "rate" :valid true}
  Description:
    Create a new app record and then return the number of affected rows.
  Examples: 
    Clojure: (create-app! {:id "id" :icon "icon" :cover "cover" :title "title" :description "description" :repo-url "repo_url" :author "author" :rate "rate" :valid true})
*/
INSERT INTO choppy_app (id, icon, cover, title, description, repo_url, author, rate, valid)
VALUES (:id, :icon, :cover, :title, :description, :repo-url, :author, :rate, :valid)
RETURNING id


-- :name update-app!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:updates {:title "name" :icon "icon"} :id "3"}
  Description: 
    Update an existing app record.
  Examples:
    Clojure: (update-app! {:updates {:title "name" :icon "icon"} :id "3"})
    HugSQL: UPDATE choppy_app SET title = :v:query-map.title,icon = :v:query-map.icon WHERE id = :id
    SQL: UPDATE choppy_app SET title = "name", icon = "icon" WHERE id = "3"
  TODO:
    It will be raise exception when (:updates params) is nil.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
UPDATE choppy_app
SET
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
WHERE id = :id


-- :name get-app-count
-- :command :query
-- :result :one
/* :doc
  Args:
    {:query-map {:title "XXX" :icon "XXX"}}
  Description:
    Get count.
  Examples:
    Clojure: (get-app-count)
    SQL: SELECT COUNT(id) FROM choppy_app

    Clojure: (get-app-count {:query-map {:title "XXX" :icon "XXX"}})
    HugSQL: SELECT COUNT(id) FROM choppy_app WHERE title = :v:query-map.title AND icon = :v:query-map.icon
    SQL: SELECT COUNT(id) FROM choppy_app WHERE title = "XXX" AND icon = "XXX"
  TODO: 
    Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
  FAQs:
    1. why we need to use :one as the :result
      Because the result will be ({:count 0}), when we use :raw to replace :one.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT COUNT(id)
FROM choppy_app
/*~
; TODO: May be raise error, when the value of :query-map is unqualified.
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str (identifier-param-quote (name field) options)
        " = :v:query-map." (name field))))))
~*/


-- :name search-apps
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:title "XXX" :icon "XXX"} :limit 1 :offset 0}
  Description:
    Get apps by using query map
  Examples: 
    Clojure: (search-apps {:query-map {:title "XXX" :icon "XXX"}})
    HugSQL: SELECT * FROM choppy_app WHERE title = :v:query-map.title AND icon = :v:query-map.icon
    SQL: SELECT * FROM choppy_app WHERE title = "XXX" AND icon = "XXX"
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT * 
FROM choppy_app
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


-- :name search-apps-with-tags
-- :command :query
-- :result :many
/* :doc
  Args:
    {:query-map {:title "XXX" :icon "XXX"} :limit 1 :offset 0}
  Description:
    Get apps with tags by using query map
  Examples: 
    Clojure: (search-apps-with-tags {:query-map {:title "XXX" :icon "XXX"}})
    HugSQL:
      SELECT  choppy_app.id,
              choppy_app.title,
              choppy_app.icon,
              choppy_app.cover,
              choppy_app.description,
              choppy_app.repo_url,
              choppy_app.author,
              choppy_app.rate,
              array_agg( tag.id ) as tag_ids,
              array_agg( tag.title ) as tags
      FROM entity_tag
      JOIN choppy_app ON entity_tag.entity_id = choppy_app.id
      JOIN tag ON entity_tag.tag_id = tag.id
      WHERE choppy_app.title = :v:query-map.title AND choppy_app.icon = :v:query-map.icon
      GROUP BY choppy_app.id
  TODO:
    1. Maybe we need to support OR/LIKE/IS NOT/etc. expressions in WHERE clause.
    2. Maybe we need to use exact field name to replace *.
    3. Maybe we need to add entity_tag.entity_type = "choppy-app" condition.
*/
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
SELECT  choppy_app.id,
        choppy_app.title,
        choppy_app.icon,
        choppy_app.cover,
        choppy_app.description,
        choppy_app.repo_url,
        choppy_app.author,
        choppy_app.rate,
        choppy_app.valid,
        array_agg( tag.id ) as tag_ids,
        array_agg( tag.title ) as tags
FROM entity_tag
JOIN choppy_app ON entity_tag.entity_id = choppy_app.id
JOIN tag ON entity_tag.tag_id = tag.id
/*~
(when (:query-map params) 
 (str "WHERE "
  (string/join " AND "
    (for [[field _] (:query-map params)]
      (str "choppy_app."
        (identifier-param-quote (name field) options)
          " = :v:query-map." (name field))))))
~*/
GROUP BY choppy_app.id
ORDER BY choppy_app.id
--~ (when (and (:limit params) (:offset params)) "LIMIT :limit OFFSET :offset")


-- :name delete-app!
-- :command :execute
-- :result :affected
/* :doc
  Args:
    {:id "XXX"}
  Description:
    Delete a app record given the id
  Examples:
    Clojure: (delete-app! {:id "XXX"})
    SQL: DELETE FROM choppy_app WHERE id = "XXX"
*/
DELETE
FROM choppy_app
WHERE id = :id


-- :name delete-all-apps!
-- :command :execute
-- :result :affected
/* :doc
  Description:
    Delete all app records.
  Examples:
    Clojure: (delete-all-apps!)
    SQL: TRUNCATE choppy_app;
*/
TRUNCATE choppy_app;
