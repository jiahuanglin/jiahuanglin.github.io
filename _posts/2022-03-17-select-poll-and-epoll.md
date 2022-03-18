---
title: Select, poll and epoll
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-03-17 10:23:00 -0500
categories: [System]
tags: [network, server]
---

## What is I/O multiplexing
Consider a program that reads from `stdin` and sends the read in content to a socket channel and vice versa (reads from socket channel and writes to `stdout`). We could use the `fgets` method to wait for the `stdin`. But once we do that, there is no way to read the data when the socket has data. We could also use the `read` method to wait for the socket to return with data. but once we do that, there is also no way to read in the data and send it to the other side when the `stdin` has data.

Developers propose I/O multiplexing to solve such scenarios. We can consider `stdin` as one way of I/O and `sockets` as another way of I/O. Multiplexing means that if there is an "event" in any way of I/O, the application is notified to handle the corresponding I/O event, so that our program becomes a multi-tasker as if it can handle multiple I/O events at the same time.

The `select` and `poll` models are two such I/O multiplexing implementations.

## Select
Here is the `select` function signature:
```c
int select(int nfds, 
           fd_set *readfds,
           fd_set *writefds,
           fd_set *exceptfds,
           struct timeval *timeout);
```

The select function is used to detect an "event" on one or more of the sockets in a group, where "events" are generally divided into three categories.

1. Readable events, which generally mean that the recv or read function can be called to read data from the socket
2. Writable event, which generally means that the send or write function can be called to "send out" the data.
3. Exception events, where a socket has an exception.

The parameters are:
- `nfds`, also known as fd for Linux sockets, is set to the maximum fd value of all fd's that need to be listened to using the select function plus 1 (0-based index).

- `readfds`, the set of fd's that need to listen for readable events.

- `writefds`, the set of fd's to listen to for writable events.

- `exceptfds`, the set of fd's to listen to for exception events.

We can utilize the following macros:
```c
void FD_ZERO(fd_set *fdset);　　　　　　// set all elements of fdset to 0
void FD_SET(int fd, fd_set *fdset);　// set the element corresponding to the socket
void FD_CLR(int fd, fd_set *fdset);　// clear the element corresponding to the socket
int FD_ISSET(int fd, fd_set *fdset); // determines whether the element corresponding to the socket is 0 or 1
```
Where 0 means no processing is required and 1 means the opposite.


## Poll


## Epoll