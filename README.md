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
