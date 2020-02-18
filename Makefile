# WARNING
# The Makefile file is used for local postgres database.
# This file is listed in .gitignore and will be excluded from version control by Git.

test: clean-test-db test-db
	@printf "\nRunning unittest...\n"
	lein test :all

db:
	@printf "\nLaunch postgres database...(default password: password)\n"
	@docker run --name datains -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 5432:5432 -d postgres:10.0
	@echo "Create database: datains_dev"
	@bash create-db.sh datains_dev


test-db:
	@printf "\nLaunch postgres database...(default password: password)\n"
	@docker run --name datains-test -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 5432:5432 -d postgres:10.0
	@sleep 3
	@echo "Create database: datains_test"
	@bash create-db.sh datains_test


clean-test-db:
	@printf "Stop "
	@-docker stop datains-test
	@printf "Clean "
	@-docker rm datains-test


clean-db:
	@printf "Stop "
	@-docker stop datains
	@printf "Clean "
	@-docker rm datains