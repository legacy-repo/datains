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

## License

Copyright © 2019 FIXME
