MVCC, anomalies and Transaction Isolation levels
---

The most important thing to understand about the transaction (tx) isolation levels aren't the levels themselves. Even though they are described in SQL Standard, they actually mean different things in different databases. For details: [A Critique of ANSI SQL Isolation Levels (Microsoft, 1995)](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/tr-95-51.pdf).

What's actually important is to understand **the anomalies** - what can go wrong.  The next important thing is how MVCC or some other mechanism can fix them. 

And then if you know which concurrency mechanism is used in your DB, it'll be trivial to realize what this or that Isolation Level means. It'll be very natural, and you won't have to memorize anything.  

# Lost update

2 transactions update the same row simultaneously. One of them overwrites the value, the other one looses its update.


<details><summary>When this is a problem</summary>

</details>