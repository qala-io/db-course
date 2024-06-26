# Debugging PostgreSQL

Need to follow this guide to build the package locally: 
[17.3. Building and Installation with Autoconf and Make](https://www.postgresql.org/docs/current/install-make.html), 
but:

1. You probably don't want to change user to `postgres` when installing its binaries. You'll start it as your own user
in IDE, so don't do any `su`.
2. During `./configure` we need to set debug options so that Symbol Table is built which will allow IDE to 
associate binary execution with lines in the source code (see [Developer_FAQ#gdb](https://wiki.postgresql.org/wiki/Developer_FAQ#gdb)).
In fact, we want to keep as much of debug info as possible, so we'll remove ever more optimizations (`-O0` instead of `-Og`).
Otherwise, there'll be a lot of information (variable values) unavailable during debugging. Additionally, let's exclude ICU (`--without-icu`) to keep our build as simple as possible:
```
./configure --enable-cassert --enable-debug CFLAGS="-ggdb -O0 -g3 -fno-omit-frame-pointer" --without-icu
```
3. Since we don't use `su`, create the folder to build to and grant yourself permissions:
```
sudo mkdir /usr/local/pgsql
sudo chown [you user] /usr/local/pgsql
```
4. Before running PG initialize database (you can choose a different folder to):
```
/usr/local/pgsql/bin/initdb -D /usr/local/pgsql/data
```

5. To start Postgres run:
```
/usr/local/pgsql/bin/postgres -D /usr/local/pgsql/data
```

6. Connect to the database, find the backend process which will actually handle queries using `select pg_backend_pid();`. Use it in CLion to attach: _Run -> Attach to Process_.