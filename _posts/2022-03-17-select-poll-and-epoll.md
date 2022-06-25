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
void FD_ZERO(fd_set *fdset);         // set all elements of fdset to 0
void FD_SET(int fd, fd_set *fdset);  // set the element corresponding to the socket
void FD_CLR(int fd, fd_set *fdset);  // clear the element corresponding to the socket
int FD_ISSET(int fd, fd_set *fdset); // determines whether the element corresponding to the socket is 0 or 1
```
Where 0 means no processing is required and 1 means the opposite.


However, the select function has two design flaws: 

1. the select function limits the number of file descriptors a single process can listen to, which is determined by __FD_SETSIZE, with a default value of 1024. 
2. when the select function returns, we need to iterate through the descriptor to find out which descriptors are ready. This traversal process generates some overhead, which can slow down the program's performance. 
  
 
As a result, the poll function has been proposed to solve the shortage of 1024 file descriptors to which the select function is limited.

## Poll
Let's first take a look at the poll function definition: 
```c
int poll (struct pollfd *__fds, nfds_t __nfds, int __timeout); 
```
Where the argument `*__fds` is the `pollfd` structure array, the argument `__nfds` represents the number of elements of the `*__fds` array, and `__timeout` represents the timeout for the poll function to block. 

`pollfd` contains three member variables, `fd`, `events`, and `revents`, indicating the file descriptor to listen on, the type of event to listen on, and the type of event that actually occurred.
```c
struct pollfd { 
  int fd; // the file descriptor to listen on short int events
  short int events; //the type of event to listen to short int revents
  short int revents; //the type of event that actually occurs
};
```

The poll process can be divided into three steps: 

1. to create a pollfd array and a listening socket and bind them; 
2. to add the listening socket to the pollfd array and set it to listen for read events, that is, connection requests from the client; 
3. to call the poll function in a loop to detect if there are ready file descriptors in the pollfd array. 

The main improvement of the poll function over the select function is that it allows more than 1024 file descriptors to be listened to at once. However, after calling the poll function, we still need to iterate through each file descriptor, check if it is ready, and then process it.

## Epoll
The epoll mechanism avoids traversing every descriptor. It uses the `epoll_event` structure to record the file descriptor to be listened to and the type of event it is listening for, similar to the `pollfd` structure used in the poll mechanism. The `epoll_event` structure contains the `epoll_data_t` union variable and the events variable, which is an integer. `epoll_data_t` has the member variable `fd` that records the file descriptor. The events variable takes a different macro value to represent read, write & error events.

```c
typedef union epoll_data{ 
  ... int fd; //record file descriptor 
  ...
} epoll_data_t;

struct epoll_event{ 
  uint32_t events; //events that epoll listens to
  epoll_data_t data; //application data
};
```
When using select or poll functions, after creating a collection of file descriptors or `pollfd` array, we can add the file descriptors we need to listen to to the array. But for the epoll mechanism, we need to call the `epoll_create` function first for an epoll instance. This epoll instance maintains two structures, one for the file descriptors to listen to and one for the file descriptors ready to be returned to the user program for processing. So, when we use the epoll mechanism, we don't have to iterate through which file descriptors are ready, as we do with select and poll. This makes epoll much more efficient than select and poll.

### kqueue
Long before Linux implemented `epoll`, Windows introduced IOCP, an asynchronous I/O model to support highly concurrent network I/O, in 1994, and the famous FreeBSD introduced `kqueue`, an I/O event distribution framework, in 2000. Linux introduced `epoll` in 2002, although related work was discussed and designed as early as 2000. 

Why didn't Linux port FreeBSD's `kqueue` directly over to `epoll` instead? Let's look at the usage of `kqueue`. `kqueue` also needs to create an object called `kqueue` first, then, through this object, call the `kevent` function to add the event of interest, and through this `kevent` function, wait for the event to happen.

```c
int kqueue(void);
int kevent(int kq, const struct kevent *changelist, int nchanges, 
          struct kevent *eventlist, int nevents, const struct timespec *timeout);

void EV_SET(struct kevent *kev, uintptr_t ident, short filter, 
            u_short flags, u_int fflags, intptr_t data, void *udata);

struct kevent {
  uintptr_t ident; /* identifier (e.g., file descriptor) */
  short filter;    /* filter type (e.g., EVFILT_READ) */
  u_short flags;   /* action flags (e.g., EV_ADD) */
  u_int fflags;    /* filter-specific flags */
  intptr_t data;   /* filter-specific data */
  void *udata;     /* opaque user data */
};
```

In his original vision, Linus stated that he thought that arrays like select or poll were OK, while queues were bad. Quoted below:

> So sticky arrays of events are good, while queues are bad. Let’s take that as one of the fundamentals.