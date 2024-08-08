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

# Debugging MySQL

Need to install first:
- `cmake` - a build tool used by C/C++ projects. If using CLion/Clion Nova, you can use their bundled version: `/Users/[your user]/Applications/CLion Nova.app/Contents/bin/cmake/mac/aarch64/bin/cmake`.
- `bison`
- `m4` - used for C/C++ macro evaluation. Should come with Xcode Command Line Tools, but they had a bug and didn't actually ship it no matter how many times you ask to install m4. So had to install it using Brew, and then put their m4 into the PATH before `/usr/bin/m4`.

```
# Didn't have to pass -DWITH_DEBUG=1 -DWITH_INNODB_EXTRA_DEBUG=1, so I guess both Release and Debug builds are created here?
cmake --build /Users/stas/projects/mysql-server/cmake-build-debug --target mysqld -j 2
```

Then start `mysqld` target from CLion, it will fail because the DB wasn't initialized. Initialize it with the binary that you just built, point it to the director from the error, I don't remember what was the parameter name (either `basedir` or `datadir`), something like this:

```
./mysql-server/cmake-build-debug/runtime_output_directory/mysqld --initialize --basedir=[path from the error]
```

Now when starting `mysqld` target from CLion, MySQL should start successfully.

## How to read MySQL source code

A good entry point to all the commands start is `sql_parse.cc`, function `mysql_execute_command()`.

Structures and terminology that you'll run into when debugging:

- trx - transaction
- ha - handler
- THD - thread, associated with the current session(connection) and current transaction
- rnd - random, as in random access file, random read/scan. Basically, it's all about reading a record from a table.
- G/GE/L/LE - greater, greater or equal, less, less or equal
- External fields - in case of data types with large lengths (like BLOB/TEXT) it's possible that the data is stored outside the clustered index. The index may still have the first part of the field (called prefix).
- `handler.h`, interface `handler`, and respective implementations of the methods in `handler.cc`, `ha_innodb.cc` represent an actual table or index. It gives access to particular records. You should read the documentation to this class, but overall there are methods for:
   - read/write/delete rows 
   - lock management
   - accessing indexes

**Call stack for reading a tuple by primary index**:

- `handler.cc handler::read_range_first()` - finds the record in the index, engine-independent logic
  - `ha_innodb.cc index_read()` - diving into InnoDB search in the index
    - `row0sel.cc row_search_mvcc()` - This function is huge. It finds and constructs the record:
      1. Tries to find the record in the Record Buffer (in-memory cache, not sure when it's filled) (see `row_sel_dequeue_cached_row_for_mysql()`)
      2. If not found, looks it up in the Adaptive Hash Index (fancy name for a hashtable with frequently-queried keys, seems useless and probably gives [little to no boost compared to the btree lookup](https://planetscale.com/blog/the-mysql-adaptive-hash-index))
      3. Traverses the index file to find the record, finds if locks exist and waits for them. Reads the records into the buffer (`byte *buf`). Reconstructs them from Undo storage if needed.
      - `row0sel.cc sel_set_rec_lock()` - sets a lock on the found record
        - `lock0lock.cc lock_clust_rec_read_check_and_lock()` - figures out if it's a GAP/Record/NextKey lock, puts the lock
          - `lock0lock.cc lock_rec_has_expl()` - traverses the existing locks in the hashtable `lock_sys->rec_hash`, see `lock0priv.h for_each()`
      - `os0event.cc` (`ret = pthread_cond_wait(&cond_var, mutex);`) - the invocation of pthreads to lock anything, and lock records in particular
- `sql_base.cc fill_record()` writes a new value into the record
- `row0mysql.cc`