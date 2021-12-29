---
title: What is a database index
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-29 22:49:00 -0500
categories: [Data Structure]
tags: [database]
---

> Index is a data structure to improve the speed of queries.

Every book has its table of contents. Similarly, an index is the "table of contents" for a database table. The database index is there to improve data query efficiency.

## B+ tree
B+ tree index is one of the most common index data structures in database systems. But why?

Let's consider implementing an index with common data structures that supports fast look-ups like `hashmap`, `array` and `binary search tree`.

