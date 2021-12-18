---
title: How to design a distributed cache system?
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-18 01:45:00 -0500
categories: [Sytem]
tags: [system design, cache system]
---

> The following are my notes from reading G.K's system design book

How to design a caching system?

The caching system is a widely used technology in almost all applications today. In addition, it applies to every layer of the technology stack. For example, DNS lookups heavily utilize caching.

In short, a caching system (possibly in memory) stores commonly used resources so that the next request of the same resource can return immediately. It improves system efficiency by consuming more storage space.

### LRU
One of the most commonly used caching systems is the LRU (longest unused). The way an LRU cache works is very simple. When a client requests resource A, the following happens:

- If A exists in the cache, we return it immediately.
- If not, and the cache has additional storage, we fetch resource A and return it to the client. Alternatively, A is inserted into the cache.
- If the cache is full, we eliminate the longest unused resource and replace it with resource A

The strategy here is to maximize the requested resource's chances in the cache. So how can we implement a simple LRU?

### LRU Design
The LRU cache should support these operations: lookup, insertion and deletion. To achieve fast lookups, we need to use hashing. By the same token, if we want to insert/delete quickly, a list of links comes to mind. Since we need to find the longest unused items efficiently, we need to order the queues, stacks, or ordered arrays.

We can use a queue implemented by a two-way linked list to store all the resources to combine all these analyses. In addition, a hash table is needed, where the resource identifier is the key and the address of the corresponding queue node is the value.

The working principle is, when requesting resource A, we check the hash table to see if A exists in the cache. if so, we can immediately find the corresponding queue node, return the resource (and move it to the end of the queue). If not, we add A to the cache. If there is enough space, we simply add A to the end of the queue. Otherwise, we need to delete the longest unused entry. We can easily delete the head of the queue and the corresponding entry in the hash table to do this.

> The resource IDs also need to be stored in the queue, so that after removing resources from the queue, they can be removed from the hash table as well.

### Page change policy
When the cache is full, we need to delete the existing items to store the new resources. In fact, deleting the oldest unused items is only one of the most common methods. So are there other ways to do it?

1. Random Replacement (RR) - As the term suggests, we can remove an entry at random.
2. Least Frequently Used (LFU) - We maintain the frequency of requests for each item and remove the least frequently used items.
3. W-TinyLFU - This is a modern page-changing strategy. In a nutshell, the problem with LFU is that sometimes an item is only used frequently in the past, and LFU still keeps the item for a long time. W-TinyLFU solves this problem by calculating the frequency within a time window. It also has various storage optimizations.

## Concurrency
Caches will have concurrency problems as it forms a classic read-write problem. When multiple clients try to update the cache simultaneously, there may be conflicts. For example, two clients may compete for the same cache slot, and the last client to update the cache will win.

Of course, the standard solution is to use locks. The disadvantage is obvious - it can seriously affect performance. How can we optimize it?

One way is to split the cache into multiple slices and assign a lock to each slice so that if clients update the cache in different slices, they don't wait for each other. However, since popular entries are more likely to be accessed, some shards will lock more frequently than others.

Another approach is to use commit logs. We can store all changes in the log to update the cache instead of updating them immediately. Then some background process will execute all the logs asynchronously. This strategy is usually used in database design.

## Distributed caching
When the system reaches a certain size, we need to distribute the cache to multiple machines.

The general strategy is to keep a hash table that maps each resource to the corresponding machine. Thus, when a resource A is requested, from this hash table, we know that machine M is responsible for caching A and directing the request to M. In machine M, it works similarly to the local cache discussed above. If A does not exist in memory, machine M may need to fetch and update A's cache. After that, it returns the cache to the original. After that, it returns the cache to the original server.