---
title: Chain of responsibility design pattern
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-20 00:01:00 -0500
categories: [Software]
tags: [design pattern, Java, Go, C++]
---

> The chain of responsibility design pattern

In compiler backend development, we usually will have to run multiple passes on the intermediate representation. Those passes including optimization passes, lowering passes, scheduling passes and code gen passes. In addition, passes will follow some particular order, namely we always run optimization passes before scheduling and code gen passes. Inputs to the downstream passes usually will depend on the outputs of upstream passes with possibly some twisks.

```java

```


### Appendix
Following is a Go implementation of the `chain of responsibility pattern`.
```go

```


Following is a C++ example of the `chain of responsibility pattern`:
```c++

```