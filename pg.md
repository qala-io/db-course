# Debugging PostgreSQL

Need to follow this guide to build the package locally: 
[Chapter 17. Installation from Source Code/Short Version](https://www.postgresql.org/docs/current/install-short.html), 
but:

1. During `./configure` we need to set debug options so that Symbol Table is built which will allow IDE to 
associate binary execution with lines in the source code (see [Developer_FAQ#gdb](https://wiki.postgresql.org/wiki/Developer_FAQ#gdb)):
```
./configure --enable-cassert --enable-debug CFLAGS="-ggdb -Og -g3 -fno-omit-frame-pointer"
```
2. To start Postgre, select built binary in Clion: `/usr/local/pgsql/bin/postgres`
3. Usually Postgre starts with in multi-user mode which means that the process that you start isn't the process that
will handle actual code execution. Need to run in a single-user mode:
```
--single -D /usr/local/pgsql/data postgres
```