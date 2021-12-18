---
title: How to design a distributed key-value storage system?
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-18 01:09:00 -0500
categories: [Sytem]
tags: [system design, key-value store]
---

> The following are my notes from reading G.K's system design book

Let's first condier a easier question: how to design a simple key-value storage system on a single machine?

## Single node key-value store
The most straightforward way is to use a hash table to store key-value pairs, which is how most such systems work today. Hash tables allow us to read/write a key-value pair in constant time and are very easy to implement as most languages have built-in support for it.

However, the drawbacks are also evident. Using hash tables usually means storing everything in memory, which may not be possible when the data set is large. There are two standard solutions.

1. `Compress our data.` This should be the first thing to consider, and there are often many things we can compress. For example, we can store references instead of the actual data. We can also use float32 instead of float64. Also, it is valid to use a different data representation like bit arrays (integers) or vectors.
2. `Storage on disk.` If it is impossible to put everything in memory, you can store some data on a disk, which means we can think of this system as a caching system to optimize it further: we keep the frequently accessed data in memory and the rest on disk.

## Distributed key-value store
The exciting part is the scaling of key-value stores to multiple machines. 
Since one machine does not have enough storage space for all the data, the general idea is to split the data across multiple machines by some rules. The coordinator can direct the clients to the machine with the requested resources. The question is how to split the data across multiple machines and, more importantly, what is the strategy for distributing the data?

### Splitting
Suppose all the keys are URLs like and we have 26 machines. One way to do this is to assign all keys (URLs) to these 26 machines based on the first character of the URL (after www). For example, https://google.ca would be stored on machine G, while https://jiahuanglin.xyz will be stored on machine J. So what is the downside of this design?

Let's ignore the case where the URL contains ASCII characters. A good sharding algorithm should balance the traffic evenly across all machines. In other words, ideally, each machine would receive the same number of requests. The above design is not good. First of all, the storage is not evenly distributed. There are probably more URLs starting with a than z. Second, some URLs are more popular, such as sites like Facebook and Google.

It is better to make sure that the keys are randomly distributed to balance the traffic. Another solution is to use a hash of URLs, which usually performs better.

### System availability
System availability is an important metric to evaluate a distributed system. For example, suppose one of our computers crashes for some reason (perhaps a hardware problem or a program error). How does this affect our key-value storage system?

If someone requests resources from this machine, we will not be able to return the correct response. Crashes will often happen if we use a large number of servers to serve millions of users, and you won't be able to restart the server every time manually. That's why availability is essential in every distributed system today. So how can we solve this problem?

Of course, we can write more robust code with test cases. But there will always be bugs in our programs. In addition, hardware problems are more challenging to protect. The most common solution is redundancy. By creating machines with duplicate resources, we can significantly reduce system downtime. If one machine has a 10% chance of crashing every month, then using a backup machine reduces the probability of both machines being down to 1%.

### `Redundancy` versus `sharding`
At first glance, redundancy and sharding look very similar. So how do the two relate? How do we choose between redundancy and sharding when designing a distributed key-value store?

Note that Sharding is basically used to split data across multiple machines because one machine cannot store too much data. Redundancy is a way to protect the system from downtime. With that in mind, redundancy is useless if one machine can't hold all the data.

By introducing redundancy, we can make the system more robust. However, consistency is an issue. For example, if machine M1 exists and has redundancy M2, how do you ensure that M1 and M2 have the same data? For example, we need to update both machines when inserting a new entry. But one of the write operations may fail. So over time, M1 and M2 may have a lot of inconsistent data, which is a big problem.

Here are a couple of solutions. The first method is to keep the local copy in the coordination machine. Whenever a resource is updated, the coordinator keeps a copy of the updated version. Therefore, if the update fails, the coordinator can operate to update.

The other method is to commit the log. Each node machine keeps a commit log of each operation, just like the history of all updates. So when we want to update an entry in machine M, it will first store that request in the commit log. Then we can have a separate program will process all the commit logs in order (in a queue). Whenever an operation fails, we can easily recover because we can look up the commit log.

The last method is to resolve conflicts in reads. Suppose the coordinator can request all three machines when the requested resources are located in M1, M2 and M3. If the coordinator sees the data is different, coordinator can resolve the conflict instantly.

### Read throughput
In this article, I also want to mention read throughput briefly. Typically, key-value storage systems should support a large number of reading requests. So, what methods can we apply to increase read throughput?

Utilizing memory is a common approach to improve read throughput. If the data is stored on a disk in each node machine, we can move some of it to memory. The more common idea is to use caching.