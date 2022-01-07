Database Architecture Course
----------------------------

This is a course for Enterprise/Web Software Developers. It goes into details of Relational Databases (RDBMS):

* What they are good for
* How to design DB schemas
* How to properly access DB from our application code

# Programme

## Types of databases

* Log-structured Merge Trees (LSM)
* Heap files and Indexes
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

## Concurrency

* Pessimistic Locking
* `skip locked` and background jobs
* Optimistic Locking
* Materializing locks
* Advisory locks

## Transactions

* Durability & Write-ahead logging
* Data Anomalies: Dirty Write, Dirty Read, Non-repeatable Read, Phantoms, Write Skew
* Concurrency: MVCC, Locking, 2 Phase Locking (2PL)
* Redo and Undo logs
* Isolation: Read Committed, Snapshot, Repeatable Read, Serializable
* Dead Locks
* Hot backups
* Index, MVCC, Visibility Maps
* Index vs Isolation vs Unique Constraints

## Application code

* Evolving schema with migrations: (blocking) pre-deployment migration vs. background jobs
* [DB Connection Pools](https://github.com/qala-io/java-course/blob/master/docs/programme/db-pools.md) and [Thread Pools](https://github.com/qala-io/java-course/blob/master/docs/programme/web-apps.md#step-2---thread-pools)
* Bloom filter
* [Hibernate](https://github.com/qala-io/java-course/blob/master/docs/programme/hibernate.md) (optional)