Database Architecture Course
----------------------------

This is a course for Enterprise/Web Software Developers. It goes into details of Relational Databases (RDBMS):

* What they are good for
* How to design DB schemas
* How to properly access DB from our application code

# Programme

## Types of databases

* Log-structured Merge Trees (LSM)
* [Heap-organized vs Index-organized tables](docs/table-organization.adoc): the internal structure of the tables (heap vs. clustered index)
* Document-oriented
* Relational
* Graph-oriented
* OLTP vs OLAP
* Replication

## Database Index

* B+ Tree, Hash Tables, Bitmaps
* Primary & Secondary index
* Composite Index
* Performance of Index vs Sequential Table Scan
* Analyzing Query Plan
* Foreign Keys, Unique Constraints
* [Clustered Index, Covering Index](https://github.com/ctapobep/blog/issues/9)

## Joins

* Nested Loops
* Merge Join
* Hash Join
* Analyzing Query Plan

## Concurrency and Transactions

* [Concurrency intro: MVCC vs Locking](./docs/concurrency-mvcc-vs-locks.adoc)
* [Postgres MVCC](./docs/mvcc-pg.adoc), isolation: Read Committed, Snapshot, Snapshot Serializable (SSI)
* [Mysql vs Postgres MVCC](https://github.com/ctapobep/blog/issues/24)
* Pessimistic Concurrency Control (via locking)
* `skip locked` and background jobs
* Optimistic Concurrency Control (via verification)
* Materializing locks
* Advisory locks
* Durability & Write-ahead logging
* Data Anomalies: Dirty Write, Dirty Read, Non-repeatable Read, Phantoms, Lost Update, Write Skew
* Concurrency with locking. Locking, 2 Phase Locking (2PL); isolation: Read Committed, Repeatable Read, Serializable
* Hot backups
* Redo and Undo logs
* Dead Locks
* Index, MVCC, Visibility Maps
* Index vs Isolation vs Unique Constraints

## Application code

* Evolving schema with migrations: (blocking) pre-deployment migration vs. background jobs
* [DB Connection Pools](https://github.com/qala-io/java-course/blob/master/docs/programme/db-pools.md) and [Thread Pools](https://github.com/qala-io/java-course/blob/master/docs/programme/web-apps.md#step-2---thread-pools)
* Bloom filter
* [Hibernate](https://github.com/qala-io/java-course/blob/master/docs/programme/hibernate.md) (optional)
