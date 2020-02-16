--------- Tag Table -------------
---------------------------------
-- :name create-tag! :! :n
-- :doc Create a new tag record
INSERT INTO tag
(name, category)
VALUES (:name, :category)

-- :name get-tag-count :? :1
-- :doc Get count
SELECT COUNT(id) FROM tag

-- :name search-tags :?
-- :doc Get tags by query string
SELECT * FROM tag
--~ (when (:query-str params) (str "WHERE " (:query-str params)))
ORDER BY id
--~ (when (and (:per-page params) (:offset params)) "LIMIT :per-page OFFSET :offset")

-- :name delete-tag! :! :n
-- :doc Delete a tag record given the id
DELETE FROM tag
WHERE id = :id
