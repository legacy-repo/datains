# Datains

Datains is an web tool for `Reproducible Omics Pipeline`.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Install Dependencies

```bash
lein deps
```

## Initial Database

- Create Database on PostgreSQL
- Init dev-config.edn file
- migrate database by running `lein run migrate`

## Running

To start a web server for the application, run:

```bash
lein run 
```

## Launch a Jupyter Server for Datains

```bash
lein jupyter notebook
```

## How to reload application without the need to restart the REPL itself

```
(require '[user :as u])
(u/restart)
```

## Environment Variables for Datains

```bash
# Required
## General
PORT=
### Only support postgresql
DATABASE_URL='postgresql://localhost:5432/datains_dev?user=postgres&password=password'
### For reports/apps/projects
WORKDIR=

## App Store
APP_STORE_ACCESS_TOKEN=
APP_STORE_HOST=
APP_STORE_PORT=
APP_STORE_USER_NAME=
APP_STORE_PASSWORD=
### app_utility is a script for rendering choppy-app. where is the virtualenv for the app_utility?
APP_UTILITY_BIN=/app/external

## Cromwell
### Where is the cromwell service? More details: https://cromwell.readthedocs.io/en/stable/
CROMWELL__URL=http://localhost:8000
### Maybe you can use basic auth by nginx or httpd, e.g. 'Basic cGd4LWNyb213ZWxsOmxyb2Nr'
CROMWELL__TOKEN=

## Tasks
### More details: http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html
### Cron expression for submit-jobs task
TASKS__SUBMIT_JOBS__CRON=0 */1 * * * ?
### Cron expression for sync-apps task
TASKS__SYNC_APPS__CRON=0 */3 * * * ?

# Optional
DINGTALK_ACCESS_TOKEN=
DINGTALK_SECRET=
```

## Change Log
1. Database (Model)
2. Database Handler (Controller)
3. Route (View)

## TODO
### Drivers
#### [Gitea Driver](https://github.com/zeripath/java-gitea-api)

#### [Cromwell Driver]()

#### [SmartCDP Driver]()

#### [Data Portal Driver]()

#### [Report Driver]()

### [Scheduler](http://clojurequartz.info/)
Scheduler will help us to do a series of things, such as:
1. Sync choppy apps from gitea.
2. Sync the status of all jobs from Cromwell.
3. Submit the jobs to Cromwell periodically.

So we need a plugable implement.

### [Async Task](core.async)

## License

Copyright © 2019 FIXME
