--------- Schema Table ----------
---------------------------------
-- :name create-schema! :! :n
-- :doc Create a new schema record
INSERT INTO schema
(name, schema)
VALUES (:name, :schema)

-- :name update-schema! :! :n
-- :doc Update an existing schema record
UPDATE schema
SET name = :name, schema = :schema
WHERE id = :id

-- :name get-schema-count :? :1
-- :doc Get count
SELECT COUNT(id) FROM schema

-- :name search-schemas :?
-- :doc Get schemas by query string
SELECT * FROM schema
--~ (when (:query-str params) (str "WHERE " (:query-str params)))
ORDER BY id
--~ (when (and (:per-page params) (:offset params)) "LIMIT :per-page OFFSET :offset")

-- :name delete-schema! :! :n
-- :doc Delete a schema record given the name
DELETE FROM schema
WHERE name = :name