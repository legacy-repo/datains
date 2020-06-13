.PHONY: install clean

mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_dir := $(notdir $(patsubst %/,%,$(dir $(mkfile_path))))

install:
	@echo "Install dependencies..."
	@virtualenv $(current_dir)/.env -p python3
	@source $(current_dir)/.env/bin/activate && pip install -r $(current_dir)/requirements.txt
	@printf "\n\033[0;32m-------------------------- Notice --------------------------\n\n"
	@printf "Please add \`$(current_dir)/.env/bin\` into your PATH variable.\033[0m\n\n"

clean:
	rm -rf .env
