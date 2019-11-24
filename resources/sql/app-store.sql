--------- App Table-------------
--------------------------------
-- :name create-app! :! :n
-- :doc Create a new app record
INSERT INTO apps
(id, icon, cover, title, description, repo_url, author, rate)
VALUES (:id, :icon, :cover, :title, :description, :repo-url, :author, :rate)

-- :name update-app! :! :n
-- :doc Update an existing app record
UPDATE apps
SET icon = :icon, cover = :cover, title = :title, description = :description, repo_url = :repo-url, author = :author, rate = :rate
WHERE id = :id

-- :name get-app :? :1
-- :doc Retrieve a app record given the id
SELECT * FROM apps
WHERE id = :id

-- :name get-app-count :? :1
-- :doc Get count
SELECT COUNT(id) FROM apps

-- :name get-apps :? :*
-- :doc Retrieve apps record
SELECT * FROM apps
ORDER BY id
LIMIT :per-page OFFSET :offset

-- :name search-apps :?
-- :doc Get apps by title like, :title-like should include % wildcards
SELECT * FROM apps
WHERE title LIKE :title-like
ORDER BY id
LIMIT :per-page OFFSET :offset

-- :name delete-app! :! :n
-- :doc Delete a app record given the id
DELETE FROM apps
WHERE id = :id


--------- Tag Table-------------
--------------------------------
-- :name create-tag! :! :n
-- :doc Create a new tag record
INSERT INTO tags
(name, category)
VALUES (:name, :category)

-- :name get-tag :? :1
-- :doc Retrieve an app record given the id
SELECT * FROM tags
WHERE id = :id

-- :name get-tag-count :? :1
-- :doc Get count
SELECT COUNT(id) FROM tags

-- :name get-tags :? :*
-- :doc Retrieve tags record
SELECT * FROM tags
ORDER BY id
LIMIT :per-page OFFSET :offset

-- :name search-tags :?
-- :doc Get tags by name like, :name-like should include % wildcards
SELECT * FROM tags
WHERE name LIKE :name-like
ORDER BY id
LIMIT :per-page OFFSET :offset

-- :name delete-tag! :! :n
-- :doc Delete a tag record given the id
DELETE FROM tags
WHERE id = :id

--------- Schema Table----------
--------------------------------
-- :name create-schema! :! :n
-- :doc Create a new schema record
INSERT INTO schemas
(name, schema)
VALUES (:name, :schema)

-- :name get-schema :? :1
-- :doc Retrieve a schema record given the name
SELECT * FROM apps
WHERE name = :name

-- :name delete-schema! :! :n
-- :doc Delete a schema record given the name
DELETE FROM schemas
WHERE name = :name