---
title: Logging System
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-16 00:16:00 -0500
categories: [Software]
tags: [logging, server]
---

Details of an Apache Log4j vulnerability were recently made public, which attackers could exploit to execute code remotely. The matter has caused an extremely heated discussion online and has led to countless programmers working overtime to change the code. Numerous software is implemented in java, each equipped with a logging system that depends on log4j. The silly-sounding question is, why do we need a logging module?

## Why do we need logging
When we talk about software development, we refer to a pipeline: development, testing, and then release to production. No one can write bug-free code or tests that cover every single corner case all the time. Usually, programmers use debuggers or simply just `printf` to debug programs. However, when we launched the server in the production environment, we could no longer troubleshoot problems through debuggers, mainly because we couldn't just hang the execution and leave the user's request stuck there. In addition, the production environment is typically distributed/multi-processed, which in general will make debugger useless.

Since we can't debug through a debugger, we need a logging system to track and recall the program's behaviour and locate the problem.

## Functionalities of a logging system
Generally speaking, a logging system will support the following functionalities over a simple `printf`:

1. Supports hierarchy of messages — namely, DEBUG, INFO, WARNING, ERROR, etc. So developers don't need to comment out the output statements after development but only adjust the level.
2. Support for multiple output destinations. For example, we can have a log printed on the screen and save it to a file in the meantime for persistent storage and easy analysis.
3. Support for log scrolling. When there are too many log files, the log library can delete some obsolete and irrelevant logs.
4. Better performance. Compared with `printf`, log libraries are usually highly optimized for formatting and output performance, not affecting the program's execution efficiency.
5. Multi-threaded processing. With `printf`, when multiple threads are outputting simultaneously, a single line of logs may mix up the output of numerous threads, and the log library can synchronize the work correctly and efficiently to avoid output confusion.

## How to implement a logging system



