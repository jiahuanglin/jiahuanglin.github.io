---
title: Elasticsearch 101
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-03-02 09:01:00 -0500
categories: [Software]
tags: [database, elasticsearch]
---

## Basic concepts

- `Cluster`: A cluster is identified by a unique name, defaulting "elasticsearch". The cluster name is critical, as nodes with the same cluster name will form a cluster. The cluster name can be specified in the configuration file.
- `Node`: Node stores the cluster's data and participate in the cluster's indexing and search functions. Nodes also have their names. By default, the first seven characters of a random UUID are used as the name of a node at startup, and you can specify any name for it. Cluster names are used to discover peers in the network to form clusters. A node can also be a cluster.
- `Index`: An index is a collection of documents (equivalent to a collection in solr). Each index has a unique name by which it can be manipulated. There can be any number of indexes in a cluster.
- `Type`: Deprecated since version 6.0.0, only one type of data is stored in an index.
- `Document`: A piece of data to be indexed, the basic information unit of the index, represented in JSON format.
- `Shard`: When creating an index, you can specify the number of shards to be stored. Each shard itself is a fully functional and independent "index" that can be placed on any node of the cluster.
- `Replication backups`: A shard can have multiple backups (replicas)

![rdbms vs elasticsearch](/assets/img/posts/elasticsearch/rdbms-vs-elasticsearch.png)