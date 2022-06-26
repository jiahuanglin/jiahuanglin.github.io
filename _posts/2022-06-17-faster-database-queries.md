---
title: Faster database queries
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-06-17 10:23:00 -0500
categories: [System]
tags: [database]
---

## Cache
Most production systems use the classic combination of MySQL and Redis. Redis acts as a front-end cache for MySQL, blocking most of the query requests for MySQL and essentially relieving the pressure on MySQL's concurrent requests. 

Redis is a high-performance KV database that uses memory to store data, and its high performance comes from its simple data structure and use of memory to store data. By design, Redis sacrifices much of its functionality and data reliability for high performance. But it is these same features that make Redis particularly suitable as a front-end cache for MySQL. Although Redis supports persisting data to disk and master-slave replication, you need to know that Redis is still an unreliable store and is not designed to be reliable. We generally use Redis as a cache.

Even if Redis is only used as a cache, we must design the Redis cache with its `data unreliability` characteristic in mind, or in other words, our application must be compatible with Redis data loss when using Redis, so that even if Redis data loss occurs, it does not affect the data accuracy of the system.

When caching a MySQL table, the primary key is usually used in Redis. For example, if you are caching an order table, you would use the order number as the primary key in Redis. Value is used to store the entire order record after serialization. Then we come to the question of how to update the data in the cache. 

> `Read/Write Through`: When querying order data, first go to the cache query. If the cache is hit, then directly return the order data. If it does not hit, then go to the database query, get the query results after the order data into the cache, and then return. When updating the order data, we first update the order table in the database and then update the data in the cache if the update is successful. 

Is there any problem with using the cache in the above way? Probably not in most cases. However, in the case of concurrency, there is a certain probability that `dirty data` will occur, and the data in the cache may be incorrectly updated with old data. For example, for the same order record, a read request and a write request are generated at the same time, and these two requests are assigned to two different threads to execute in parallel, the read thread tries to read the cache and misses, and goes to the database to read the order data, then maybe another read thread updates the cache first, and in the thread that handles the write request, it updates the data and the cache, then the first read thread with the old order data updates the cache to the old data.

So how to solve the `dirty data` problem? The `Cache Aside` pattern is very similar to the `Read/Write Through` pattern above, but with only a small difference:

> The `Cache Aside` pattern does not try to update the cache when updating data, but to delete the cache. After the order service receives a request to update data, it first updates the database, and if the update is successful, then tries to delete the order in the cache. This updated order data will be loaded into the cache the next time it is accessed. Using Cache Aside mode to update the cache is very effective in avoiding dirty data problems caused by concurrent reads and writes.

## Read-write separation
However, caching is not so effective for user-related systems, such as order systems, account systems, shopping cart systems, and so on. In these systems, each user needs to query information related to the user. Even if it is the same function interface, the data that each person sees is different. For example, in the "My Orders" function, where users see their order data, I open my order cache data, but can not open your order for you to use because our two orders are different. In this case, the cache's hit rate is not that high, and there are still a considerable number of query requests that hit MySQL because they do not hit the cache. Then, as the number of system users grows, more and more read and write requests hit MySQL. What do we do when a single MySQL cannot support many concurrent requests? 

When a single MySQL cannot meet the requirements, multiple MySQL instances can only be used to take up the large number of read and write requests. MySQL, like most commonly used relational databases, is typically a standalone database and does not support distributed deployment. It is very difficult to use multiple instances of a single database to form a cluster and provide distributed database services. It requires a lot of extra work when deploying a cluster, and it is hard to be transparent to the application then your application has to make a big architectural adjustment for that too. So, unless the system is really large enough that this is the only way to go, it is not recommended that you shard your data and build MySQL clusters on your own, which is very costly. A simple and very effective solution is not to slice the data, but to use multiple MySQL instances with the same data to share many query requests, often called `read-write separation`. 

The reason read-write separation can solve the problem is that it is based on an objective situation favourable to us. Many systems, especially systems for public users, have a severe imbalance in the ratio of read-to-write data. The read-to-write ratio is generally around a few dozen, with an average of only one update request for every few dozen query requests. In other words, the vast majority of requests the database needs to respond to are read-only query requests. A distributed storage system is complicated to do distributed writes because it is challenging to solve the data consistency problem. As long as I can synchronize the data to these read-only instances in real-time and ensure that the data on these read-only instances are the same, these read-only instances can share many query requests. Another benefit of read-write separation is that it is relatively easy to implement. Upgrading a system using standalone MySQL to a read-write separated multi-instance architecture is very easy and generally requires no changes to the business logic of the system, just simple changes to the DAO code to separate the read and write requests to the database.


## Query optimization

## Sharding