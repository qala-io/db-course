MySQL MVCC
---

Here we go into the implementation details of Postgres and MySQL InnoDB and see the differences in their approaches. While they both solve problems in similar domains (they are transactional, SQL, OLTP databases), their implementations couldn't be more different. These differences lead to different problems they have to face. Which by itself is very interesting, but they also end up solving some problems more efficiently, and other problems - less efficiently.

Vocabulary:

- tx - transaction
- Record, tuple, row are used interchangeably

# Storage difference between MySQL and Postgres

Conventional databases can store the rows (aka records, aka tuples) in 2 possible forms: either a Heap or an Index-Organized Table. Each having their own pros and cons.

## Table as a Heap

A Heap is just a file split into logical pages (let's say 1MB per page), and records are added to those pages as we insert into the database. Since multiple records can reside on the same page, the full address of any record becomes `(page_no, record_no)`, where `record_no` is the record number _within the page_. 

There's no particular sorting to those records. While originally we fill the table sequentially and the order coincides with the order of insertion, at some point the old (deleted) records can be removed. This leaves the pages half-full, which makes them ready to accept new records.

But we don't have just tables - every real-world database needs indexes too. These are separate data structures, residing in separate (logical or physical) files. Indexes are _sorted_ by some key (a column in a simple case), and finding something in a sorted file is fast. Very fast. So long as we're searching by that key.

Of course, we often need to search by different keys - e.g. our app may need to search users by name in one use case and by their ID in a different use case. So we have to have 2 separated indexes. Each of them is going to be sorted by its own key (column).

If the actual records are located in a Heap, then these indexes reference their physical location `(page_no, record_no)`. So when searching a user by name, the database will first look it up in the respective index, find its physical location - and only then it'll be able to return the full information about the user.  

## Index-organized table aka Clustered Index

The alternative is to keep the records sorted to begin with. Basically, our table file becomes and an index, but that index also contains the actual values. We can have only one such index per each table, and these indexes are special. We call them Clustered Indexes, but probably a better name would be Clustering Indexes - because they sort the data and basically keep the rows with similar keys together (the rows are clustered together).

Which of the columns should become the key in such an index? Usually it defaults to the Primary Key of our table. Of course, the column marked as the Primary Key is special - it's basically a unique index, and the values can never be null.

What if we create other indexes? Those are called Secondary Indexes - and now instead of referencing a physical location, they can reference the Primary Key. So if we're looking up a user by name, the database would have to first goes to the Secondary Index, finds the Primary Keys of the matching records, and now it has to make another lookup - now in the Clustered Index.

BTW, in case when we were using _Heaps_ there was no distinction between types of indexes. All of them were in fact Secondary Indexes. Such storage doesn't give any special meaning to the Primary Key - it's just yet another Secondary Index, although it's unique and non-null without saying so explicitly.

## Multiversioning (MVCC)

Databases need to handle multiple connections and transactions simultaneously. So what happens if one tx modifies some row, while another one reads it? There are 2 choices:

1. Writing tx (Writer) locks the record, and reading tx (Reader) must wait. Once the Writer ends, the Reader can proceed reading the new value. While this approach is supported by all the conventional SQL databases with `select ... for share` (we'll talk about it later), it's not the default because it causes a lot of locks and thus reduces concurrency.
2. Instead of just overwriting the record, the Writer can create a new version of it. So Readers can read the old versions until the Writer commits. Since this allows multiple versions of the record to be present, this is called Multiversioning or Multi-version concurrency control (MVCC). And this is the default in most databases.

The fact that most SQL databases implement MVCC, it doesn't mean they do it the same or even similarly. Au contraire! There are 2 approaches:

1. _Copy-on-write_ (COW) - we create a full copy of the row with the new values. The readers read the old version. This is a Postgres way.
2. _Undo storage_ - we write the new value into the table, but we copy the old values into a special temporary storage. The parallel Readers can reconstruct the version that they want to see from the current value in the table plus the data from the Undo storage. Most (if not all) other databases use this approach.

But how do Readers know which version of the tuple to use? Simple: each version must be marked with the tx ID. While the algorithm and the exact data may differ, the general approach is:

1. Assign each tx a monotonically increasing ID. Could be an integer that increments with each next tx.
2. When a Writer creates a tuple, it writes its TX ID in the header of that tuple. If Writer _updates_ an existing tuple, then the old version either stays in the heap (_COW_) or the old attributes are written into the _Undo_ storage with their previous TX ID.
3. When a tx is created, the database captures all other transactions that are currently in-progress/aborted/committed. This info is stored within every tx (and it's different for every tx). This state of transactions is called a Snapshot (PG) or a Read View (MySQL).
4. When running into the tuple, the Reader knows that this or that version is required because it can't see the results of the in-progress or aborted transactions. With the _COW_ we have to iterate over all the old/new versions and find only the one we're interested in. With the _Undo_ storage we first find the latest version in the table, and then go through the Undo to find which attributes changed, and reconstruct the version we need.

This allows Writers not block Readers, which is good! But there's a lot of problems and complexity hidden in this generic approach - this is going to differ between implementations (keep reading).

# Postgres vs MySQL

Finally, the core of our discussion! We have 2 popular databases with completely different approaches:

* Postgres uses Heaps and Copy-on-write MVCC
* MySQL uses Clustered Indexes and Undo storage MVCC

Let's see how these approaches compare and what complexity they bring with themselves.

## Postgres difficulties

First, the MVCC that we discussed isn't enough. There can be more than one old version of tuples, and each Reader needs to distinguish the old version that it can see from the versions that it can't. If interested, [read about PG MVCC in more details](https://github.com/qala-io/db-course/blob/master/docs/mvcc.md) after you finish this page.

## Deleting old versions 

We need to track transactions that end, after that can discard old versions of tuples. After all, those old versions aren't reachable by any tx anymore. 

With the Undo storage it's quite efficient - all the obsolete tuples are in a single place. We have to figure out which versions are in fact unreachable, but at least all the candidates are grouped together. 

But with the Copy-on-write, each version is a first-class citizen. So we have to traverse the whole table to find the obsolete records! To solve this problem PG has a special garbage collecting mechanism - VACUUM. In the systems with heavy load this process can cause problems: it can lag behind the UPDATE/DELETE operations, and it can take considerable CPU power.

Both in MySQL and PG this clean up happens as a separate thread/process.

## Reading old versions 

Suppose there's some old Reader that needs an old version of the tuple, what complexity goes into actually reading it?

In case of Postgres and its COW it's trivial - old versions don't differ in any way from the latest version of the tuple. They are all in Heap, they all are equal.

In MySQL and its Undo storage it's more complicated:

1. First we find the record in the table, and we see that this latest version shouldn't be visible. This latest version (v5) points to the location of the previous version (v4) in the Undo storage.
2. So we go there, we see which attributes were changed in v4 (let's say user `name` changed, but the `age` didn't), and we merge the latest v5 with v4 to figure out the state at v4. But v4 also may be not-sufficiently-old version. So this record points to an even older (v3) version in the Undo storage.
3. Another jump, and we finally found the right version (v3). Now we can merge our previous state with the changes in v3. Phew!

As you can see, in MySQL this process is more complicated and slower. 

## Resurrecting old versions in case of abort

Suppose we issued a lot of UPDATE/DELETE statements, but in the end the tx aborted. How do we return back the old versions?

In Postgres it's trivial - you actually don't do anything with the records. Just mark the tx aborted. Other transactions will see the status, and they will know not to use those versions of the tuples. VACUUM then will have to clean up those aborted versions.

But with the Undo storage of MySQL this operation is much more complicated, as it needs to go over the Undo storage and write the old versions back into the table. If we updated a million of rows, we have to restore a million of rows!

## Updating indexes

Okay, we change an attribute in the table. What happens to the indexes if the attribute is not indexed? What's happens if it _is_ indexed? This one is tough in both databases.

### Updating indexes in Postgres

In Postgres, as you remember, indexes reference physical location. Updating a row will create a new record with a new physical location. So if we have 5 indexes for this table, we need to create index entries for the new versions in addition to keeping the entries of the old versions. Even if the indexed keys didn't change!

Okay, okay, at some point PG introduced an optimization called Heap-only Tuples (HOT). If the new version stayed within the page as the old version of a tuple, then we could reference the old version (and thus no changes in the index). When we find the old version, it can point to the new version within the same page - and this allows us to quickly find the right version. For this to work the pages need to have spare room for extra versions - and PG has a `fillfactor` that defines how much we fill the pages initially.

But suppose the attribute, that participates in the index, changed. We add new index entries. Then there's a Reader that found all these entries by some key - how do we know which version to go for? Even if it's just one version - how do we know that tuple is accessible to the current tx? Unfortunately, we don't. We have to actually go through the tuples because the information about which tx created/deleted them is stored only in the Heap.

This is very frustrating, because if our index contains all the attributes needed to answer the query without jumping into the Heap (we say the index _covers_ the query), we still need to access the Heap because of the MVCC! It's a huge performance hit! And to alleviate the pain (at least partially), Postgres has another optimization. If all the tuples on a page were created by old transactions and are visible to every in-progress tx, then we can mark the whole page as "visible for all". PG keeps this information in a special bitmap called Visibility Map. So after finding an entry in the index, we can quickly learn if it's visible-for-all, and only if not - we jump to the Heap and check the tuple itself. 

And the last thing - VACUUMing is actually more complicated than we thought. Before deleting obsolete tuples we actually need to first delete obsolete index entries. And it's actually VACUUM who decides which pages go into the Visibility Map.  

### Updating indexes in MySQL

MySQL faces similar problems. But instead of keeping a separate Visibility Map (which gets updated not as frequently as one might hope), MySQL keeps the transaction-that-last-updated right in the page. So if there are 10 records on an index page (yes, indexes also go in pages), and we just updated one of them - we have to update it with the tx ID.

When searching in the index we now can check if that tx was committed. If yes, and if we know that all transactions before this one are guaranteed to be committed (both MySQL and PG track this), then we know for sure that this entry is accessible. If not, then we jump to the Clustered Index and check the version of that record itself. And if we are looking for a different one, jump through the Undo and do the whole reconstruction business.

We won't go into the details here in Part I, but if 2 transactions are updating the same row, they have to wait on one another. And to do that the database needs to create Locks. In case of PG it was easy - we just lock the heap tuple itself, it's a single source of truth. But MySQL locks index records (both Clustered and Secondary). So when issuing `update ... where name = 'a'`, and then in another tx we run `update ... where name='b'` MySQL somehow needs to know that both transactions arrived at the same record. For that reason MySQL either needs to put locks in all the indexes or have a way to dynamically check the locks in unrelated indexes. In fact MySQL does kind of both, depending on which approach it deems more optimal in any particular situation.

**Locking records**. We mostly talked about MVCC, but this is a mechanism that works exclusively for non-blocking Readers. What if 2 transactions modify the same row? This is where a lot of fun happens - the locking, the isolation levels and leakages in those levels. This is a complicated topic, so have some rest and return tomorrow before going further. 

\*Next day\*. Hello tomorrow you! In both databases tx that comes 2nd is locked and waits for the 1st one to end. But if the 1st one commits, what should the 2nd do? It's a philosophical problem, there are no perfect answers:

1. First, we should decide whether the 2nd tx sees the changes of the 1st one. If our query says `update ... where name='A'` and the name just changed to `B`, should we update the record? After all, if we want to say that tx2 was running _after_ tx1, then we must've seen `B`.
2. 

 If both transactions just write their content, then we have introduced modifications based on tuple v5, then both of them end up v6. We can't have that! Our UPDATE/DELETE/etc statements have `where` condition, so if 

## Heaps vs Clustered Indexes - pros & cons

Postgres keeps data in Heaps, while MySQL uses Clustered Indexes. This has huge effect on the problems that they face. Here are some:

### When updating a record, what happens to the indexes?

What are the challenges that 

This makes it easier to UPDATE tuples. If some value changes - the record still keeps its ID, so no secondary indexes need to be updated (well, unless the indexed column changed). This is very different from PG where we have to update all the indices to point to the new physical location. However, Index-organized tables make it harder to do the lookup and keep track of locks. And the code also gets convoluted because now we have 2 types of indexes - Clustered and Secondary, which have different roles.


## Locking the tuples

Both databases have 2 ways of handling the concurrency:

1. Multiversioning (MVCC) allows reading tx to see the previous versions of tuples - and therefore they don't have to be blocked when there's a writing tx that updates those same tuples.
2. But MVCC doesn't work with 2 writing tx. When both are updating the same row - we have to fall back to the good old locking. So in case of parallel writes - only one writer can update a record. And our 2nd writer has to wait until the first tx ends. 

### MVCC: Undo storage vs Copy-on-write

Another big difference is that MySQL doesn't store previous versions in the table - it has a separate Undo storage. So if a record is updated, MySQL writes old column values into the Undo storage. It's possible that some long-running reading transactions are still in progress, and so MySQL has to keep all the previous versions of the record for reconstruction. So if a parallel transaction needs to read the previous version - MySQL can reconstruct the object by knowing which version had to be visible for that tx, and going through the Undo and getting the previous values of the columns.

Undo storage makes it complicated to get the previous versions of tuples, but at the same time it simplifies the cleanup. Postgres has to search for tuples across the whole Heap, while MySQL knows that it's in the Undo storage, and is able to wipe them up easily.

### Locking mechanisms

Let's talk about locking in more details. Here's an UPDATE in tx1: `update t set a='A' where b='B'`. Then a new tx2 is updating records that were found by this tx1, what do we do? How do we prohibit tx2 from updating this row?

There are several questions that we need to answer:

1. What to lock (tuple, index, predicate)
2. How to lock it (implicit, explicit)

Let's start with what-to-lock. There are multiple choices:

* Lock Tuples - no matter how we found it (index, table scan), we have a single source of truth to check. But no matter where we found it (some secondary index), we have to look up the actual tuple info.
* Lock Index keys - if we found a record in IndexA and locked it, we need to spread the lock to other indexes too. We can:
   * Put an actual lock in all related indexes
   * Or at least put it on a Clustered Index too. This way if we found a record in some Secondary Index, we know where to go for the locking info.
* Lock predicates - if another tx updates a row where `b='B'` (or inserts a new one with such value), we know that this _value_ has been locked

### Implicit vs explicit locking

What's a lock? In database terms it's something that contains the info:

1. What's locked (a table, a tuple, an index or something else)
2. Which tx locked it
3. What type of lock this is (e.g. some locks aren't mutually exclusive)

It's possible to keep this information in-memory or calculate it on the fly:

* An _Explicit_ Lock is an object/structure that contains these fields. Such locks can be monitored and interrogated by a DBA.
* An _Implicit_ Lock doesn't exist as an object. There could be ways to _determine_ it on the fly. E.g. tuples themselves usually contain an ID of the tx that created/updated them (these TX IDs increase monotonically). So if we run into a tuple, we can check the status of that tx - if it's committed, then the record is visible and there are no locks. If the tx is in progress, then we know there's a lock - we can't update such tuple.

We don't want a lot of explicit locks in the database. If our tx updates a million of records, we don't want to create a million of lock objects - it's too much effort. So if possible, databases try to employ implicit locking.

But for implicit locking we need a simple way of checking the status of other transactions. And both PG and MySQL, when they begin transactions, create a so-called Snapshot - a list of active, aborted, committed transactions. 

Of course, we don't want to track the whole history of all the transactions ever executed. But we don't need to. We have a mechanism to delete old tuples that aren't reachable anymore e.g. because they were deleted. When starting this mechanism, we can look at the currently executing transactions and remember the ID of the oldest (MIN_TX). After the cleanup we know that either the tuples are available because they were created by very old transactions, or they were created by the MIN_TX or after it. So when creating a Snapshot, we just need to remember this OLD_ID - if the tuple was created by something prior to it, then it _must_ be committed and reachable.

But implicit locking doesn't allow us to keep track of locks. So if tx2 runs into a record that it waits for, _then_ potentially we want to create an Explicit Lock that we can track and report.

### Waiting queue and Scheduling

So we run into a locked object and we need to wait. At this point if Explicit Lock didn't exist - it's time to create one. An Explicit Lock needs a little more information than we mentioned before. In particular, it needs a list of transactions/threads that are waiting to acquire it. It's this queue that the database will inspect to determine which tx to unblock next. For reference, here's an [Explicit (lightweight) lock structure in PG](https://github.com/postgres/postgres/blob/0cecc908e9749101b5e93ba58d76a62c9f226f9e/src/include/storage/lock.h#L308) and the [waiting code](https://github.com/postgres/postgres/blob/f7ab71ba0c7bcf237403d40269aeea0a0f2551df/src/backend/storage/lmgr/proc.c#L1355).

So what if the queue isn't empty by now (suppose tx2 and tx3 already wait for this tuple), whose turn is it once the lock is released? In other words - where do we insert ourselves in the queue? The simplest approach is a FIFO - we just put ourselves at the end. Seems fair, after all - we came last. But:

1. What if our tx blocks 10 other transactions on some other locks, while tx2 and tx3 don't block anyone? In such cases it may be more efficient to put our tx in front. 
2. What if there are 10 transactions in the queue and all of them require a Shared (non-conflicting) lock and there's one tx in the head of the queue that requires Exclusive (blocking) access? Do we want to push it to the tail and unblock 10 transactions instead? But what if this keeps happening, and we run into a Live Lock?  

There are tradeoffs in the approaches. FIFO is super simple, but may not be the most efficient, other approaches are super complicated but may (potentially) result in a more effective execution order. At the moment [PG implements FIFO](https://github.com/postgres/postgres/blob/f7ab71ba0c7bcf237403d40269aeea0a0f2551df/src/backend/storage/lmgr/proc.c#L1109), while MySQL eventually implemented a so-called CATS which is quite sophisticated. More details on CATS:

* [Contention-Aware Transaction Scheduling](https://dev.mysql.com/blog-archive/contention-aware-transaction-scheduling-arriving-in-innodb-to-boost-performance/)
* [InnoDB Data Locking - Part 4 "Scheduling"](https://dev.mysql.com/blog-archive/innodb-data-locking-part-4-scheduling/)

Note, that MySQL locks the shit out of your records (and the gaps between them), so for them effective transaction scheduling is more important. 