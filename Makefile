.PHONY: test dev-db test-db clean-test-db clean-dev-db deploy
# WARNING
# The Makefile file is used for local postgres database.
# This file is listed in .gitignore and will be excluded from version control by Git.

test: clean-test-db test-db
	@printf "\nRunning unittest...\n"
	lein test :all

dev-db: clean-dev-db
	@printf "\nLaunch postgres database...(default password: password)\n"
	@docker run --name datains -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 5432:5432 -d postgres:10.0
	@sleep 3
	@echo "Create database: datains_dev"
	@bash create-db.sh datains_dev 5432
	@echo "Migrate database..."
	@bash lein run migrate


test-db:
	@printf "\nLaunch postgres database...(default password: password)\n"
	@docker run --name datains-test -e POSTGRES_PASSWORD=password -e POSTGRES_USER=postgres -p 54320:5432 -d postgres:10.0
	@sleep 3
	@echo "Create database: datains_test"
	@bash create-db.sh datains_test 54320


clean-test-db:
	@printf "Stop "
	@-docker stop datains-test
	@printf "Clean "
	@-docker rm datains-test


clean-dev-db:
	@printf "Stop "
	@-docker stop datains
	@printf "Clean "
	@-docker rm datains

deploy:
	@printf "Clean old files...\n"
	@rm -rf dist
	@mkdir -p dist/bin dist/lib
	@printf "Make datains.jar package...\n"
	@lein uberjar
	@printf "Copy Makefile, requirements.txt to dist...\n"
	@cp external/app-utility dist/bin/app-utility
	@cp external/Makefile dist/Makefile
	@cp external/requirements.txt dist/requirements.txt
	@cp target/uberjar/datains.jar dist/lib/
	@tar -czvf target/datains.tar.gz dist/ 2> /dev/null
	@printf "Your package is prepared into target directory..."
	@rm -rf dist