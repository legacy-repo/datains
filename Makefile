# WARNING
# The Makefile file is used for local postgres database.
# This file is listed in .gitignore and will be excluded from version control by Git.

db:
	@echo "Launch postgres database..."
	@docker run --name datains -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 5432:5432 -d postgres:10.0
	@echo "Create database: datains_dev"
	@bash create-db.sh datains_dev


test-db:
	@echo "Launch postgres database..."
	@docker run --name datains-test -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 5432:5432 -d postgres:10.0
	@echo "Create database: datains_test"
	@bash create-db.sh datains_test


clean-test-db:
	@printf "Stop "
	@docker stop datains-test
	@printf "Clean "
	@docker rm datains-test


clean-db:
	@printf "Stop "
	@docker stop datains
	@printf "Clean "
	@docker rm datains