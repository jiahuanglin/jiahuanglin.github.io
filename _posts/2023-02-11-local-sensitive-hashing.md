---
title: Locality sensitive hashing
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2023-02-11 20:11:00 -0500
categories: [Algorithm]
tags: [similarity search]
---

## Similarity Search
How to find the most similar vector to a certain vector in a list of vectors? A brute force algorithm is compare each vector in the list once with similarity measurements like Euclidean distance. But such a calculation is very expensive if the amount of data is huge. Is there a better way to do this?

There are many efficient search algorithms, and their main idea is to improve search efficiency in two ways:

1. Reduce vector size - by reducing dimensions or reducing the length of the vector representation.
2. Narrow the search range - this can be achieved by clustering or organizing vectors into tree-based or graphical structures, and restricting the search to only the closest clusters, or filtering through the most similar branches.

Let's first introduce the core concept common to most algorithms, which is clustering.

## K-Means
After saving vector data, we can first cluster the vector data. For example, in the two-dimensional coordinate system in the figure below, four clustering centers are defined, and then each vector is assigned to the nearest clustering center. After adjusting the position of the clustering center through the clustering algorithm, the vector data can be divided into four clusters. Each time you search, you only need to determine which cluster the search vector belongs to, and then search in this cluster. This reduces the search range from 4 clusters to 1 cluster, greatly reducing the search range.

K-means is a common clustering algorithm that can divide data into k categories, where k is specified in advance. Here are the basic steps of the k-means algorithm:

1. Select k initial clustering centers.
2. Assign each data point to the nearest clustering center.
3. Calculate the new center of each cluster.
4. Repeat steps 2 and 3 until the clustering centers no longer change or reach the maximum number of iterations.

However, this search method also has some disadvantages. For example, during the search, if the content of the search is right in the middle of two classification areas, it is very likely to miss the most similar vector.

To solve the possible omission problem during the search, the search range can be dynamically adjusted. For example, when `nprobe` = 1, only the nearest clustering center is searched, and when `nprobe` = 2, the nearest two clustering centers are searched, and the value of nprobe is adjusted according to the actual business needs.

In fact, except for brute force search that can perfectly search for the nearest neighbor, all search algorithms can only make a trade-off between speed, quality, and memory. These algorithms are also called `Approximate Nearest Neighbor`.


## Locality Sensitive Hashing (LSH)
Locality Sensitive Hashing (LSH) is an indexing technique that employs approximate nearest neighbor search. It is characterized by its speed and the provision of an approximate, non-exhaustive result. LSH uses a set of hash functions to map similar vectors into "buckets", thereby giving similar vectors the same hash value. Consequently, the similarity between vectors can be determined by comparing their hash values.

Typically, the hash algorithms we design aim to minimize the number of hash collisions. This is because the search time complexity of a hash function is O(1). However, if a hash collision occurs, meaning that two different keys are mapped to the same bucket, we need to use data structures like linked lists to resolve the conflict. In such cases, the time complexity of the search is usually O(n), where n is the length of the linked list. Therefore, to enhance the efficiency of hash function searches, we usually aim to make the probability of hash collisions as small as possible.

However, in vector search, our goal is to find similar vectors. Therefore, we can design a special hash function to make the probability of hash collisions as high as possible, and vectors that are closer or more similar are more likely to collide. This way, similar vectors will be mapped to the same bucket.


When searching for a specific vector, in order to find the nearest neighbor of a given query vector, similar vectors are "bucketed" into a hash table using the same hash function. The query vector is hashed into a specific table, and then compared with other vectors in that table to find the closest match. This method is much faster than searching the entire dataset because the number of vectors in each hash table bucket is far less than the number of vectors in the entire space.

So, how should this hash function be designed? For better understanding, let's first explain it in a two-dimensional coordinate system. As shown in the figure below, a two-dimensional coordinate system can be divided into two regions by randomly generating a straight line P (which is the normal vector to the hyperplane). In this way, the similarity of vectors can be judged by whether they are on the same side of the line. For instance, on the right hand side a and b indicate that the vectors are not similar as they lie in different regions. While on the left hand side, A and B are similar as they lie in the same region.

![](https://www.researchgate.net/publication/334059888/figure/fig2/AS:963424548818944@1606709720016/a-b-Rationale-of-locality-sensitive-hashing.png)

The principle is simple. If the distance between two vectors is close, the probability that they are on the same side of the line will be high. 

When searching for a vector, the vector is recalculated with the hash function, and the vectors in the same bucket are obtained. Then, the closest vector is found through brute force search. 


## Random Projection for LSH (Locality Sensitive Hashing)
If we can distinguish similarity in a two-dimensional coordinate system through randomly generated lines, then similarly, in a three-dimensional coordinate system, we can divide the three-dimensional coordinate system into two regions by randomly generating a plane. In a multi-dimensional coordinate system, we can also divide the multi-dimensional coordinate system into two regions by randomly generating a hyperplane, thereby distinguishing similarity.

However, in high-dimensional space, the distances between data points are often very sparse, and the distances between data points will increase exponentially with the increase in dimensions. This leads to a large number of calculated buckets, and in the most extreme cases, there is only one vector in each bucket, and the calculation speed is very slow. Therefore, in actual implementation of the LSH algorithm, we consider using random projection to project high-dimensional data points into low-dimensional space, thereby reducing calculation time and improving query quality.

The basic idea behind random projection is to use a random projection matrix to project high-dimensional vectors into low-dimensional space. Create a matrix composed of random numbers, the size of which will be the desired target low-dimensional value. Then, calculate the dot product between the input vector and the matrix to get a projected matrix, which has fewer dimensions than the original vector but still retains their similarity.

When we query, we use the same projection matrix to project the query vector into low-dimensional space. Then, compare the projected query vector with the projected vectors in the database to find the nearest neighbors. Since the dimensionality of the data has been reduced, the search process is much faster than searching in the entire high-dimensional space.

The basic steps are:

1. Randomly select a hyperplane from the high-dimensional space and project the data points onto this hyperplane.
2. Repeat step 1, select multiple hyperplanes, and project the data points onto multiple hyperplanes.
3. Combine the projection results of multiple hyperplanes into a vector as a representation in low-dimensional space.
4. Use a hash function to map the vectors in low-dimensional space to hash buckets.

Similarly, random projection is also an approximate method, and the quality of the projection depends on the projection matrix. Generally, the more random the projection matrix, the better its mapping quality. However, generating a truly random projection matrix may be computationally expensive, especially for large datasets.