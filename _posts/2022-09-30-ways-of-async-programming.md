---
title: Async programming
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-09-30 10:23:00 -0500
categories: [System]
tags: [programming]
---

## Why is asynchronous programming necessary? 

Operating system can be seen as a virtual machine (VM) in which processes exist. Processes do not need to know the exact number of cores or how much memory is available. As long as a process does not require excessive resources, the operating system behaves as though it has unlimited resources. Similarly, the number of threads is not limited by hardware. Your program can have only one thread or hundreds or thousands of them. The operating system silently schedules these threads, allowing them to share limited CPU time slices. The scheduling process is completely transparent to the threads.

But how does the operating system schedule threads without them being aware of it? The answer is context switching. In simple terms, the operating system uses a software interrupt mechanism to interrupt the program from any position and saves all current registers, including the program counter (PC) and stack pointer (SP), as well as some thread control information (TCB). This entire process incurs a few microseconds of overhead.

However, threads are resource expensive:

- The context switch of threads consumes quite some CPU time. 
- Each thread occupies a certain amount of memory. 

These reasons prompt us to reduce the number of threads without compromising on performance, thereby prompting us to consider asynchronous programming.


## Continuation

[Continuation passing style (CPS)](https://en.wikipedia.org/wiki/Continuation-passing_style) is a style of programming in which control is passed explicitly in the form of a continuation. A continuation is a function that represents the rest of the computation. In other words, a continuation is a function that takes the result of a computation as an argument and performs the rest of the computation:

1. Package the remaining code of the calling function f() into a function object called cont, and pass it as an argument to the called function g().
2. Execute the body of g() normally.
3. After g() completes, call the continuation cont together with its result, allowing the remaining code in f() to be executed.

As you may have noticed, this is essentially a callback function by a different name.

## A naive approach: Callback

Why should one concern themselves with utilizing Continuations in programming? It is unnecessary, except in situations where there is no involvement of I/O operations. I/O operations generally are several magnitutes slower than CPU. In a synchronous blocking I/O model, once the I/O operation is initiated, the thread is suspended and awaits its completion before being revived by the operating system.

However, in an asynchronous non-blocking I/O model, a process not only initiates an I/O operation but also simultaneously provides a callback (Continuation). This approach enhances efficiency as the thread is not required to wait idly, but can resume other tasks. Following successful completion of the I/O, the AIO Event Loop promptly invokes the callback function that was previously configured, thereby concluding the remaining work. This particular methodology is sometimes elegantly referred to as the "Fire and Forget" pattern.

[This article explains different I/O model](https://alibaba-cloud.medium.com/essential-technologies-for-java-developers-i-o-and-netty-ec765676fd21) 


## Pass callback to the future: Promise
One issue with callbacks is the readability. By avoiding the operating system's Continuation mechanism, every function call requires passing a lambda expression/function argument.

Another issue is the cumbersome handling of various details. For example, consider exception handling. It seems that passing just a Continuation is not enough; it's better to pass a callback for exception handling as well.

Promise encapsulates the result of asynchronous calls. In Java, it is known as CompletableFuture (JDK8). Promise has two layers of meaning:

1. "I am not the actual result right now, but I promise to deliver it in the future". This is easily understood as asynchronous tasks will eventually complete. If the caller is stubborn, they can forcefully obtain the result using Promise.get() and also block the current thread, converting the asynchronous into synchronous.

2. "If you (the caller) have any instructions, just let me know". In other words, the callback function is no longer passed to g(), but rather returned as Promise from g().
```
var promise = g();
promise.then((x) => {
    f(x); // do this when g() completes/resolves
}).catch((e) => {
    // handle exception
});
```

## A more elegant approach: Async/Await
Regardless of whether we are talking about callbacks or promises, it ultimately still requires manual construction of continuations. Developers have to write their business logic as callback functions. While this approach can handle linear logic smoothly, what happens when the logic becomes more complex? (For example, consider a situation involving loops.)

Languages like JavaScript provide the async/await keywords: asynchronous programming finally breaks free from callback functions! All you need to do is add await to your asynchronous function calls, and the compiler will automatically convert them into coroutines rather than expensive threads.

Behind this magic lies the Continuation-Passing Style (CPS) transformation, which converts regular functions into CPS functions, meaning that the continuation can also act as a call argument. Functions can not only start from the beginning but also continue running from a specific point based on the continuation's instructions (such as calling an IO operation).

As you can see, a function is no longer just a function but a state machine. Each time it is called or calls other asynchronous functions, the state machine performs certain calculations and state transitions. So where is the promised continuation? It is the object itself (this).

The implementation of CPS transformation is quite complex, especially when considering try-catch blocks. However, users don't need to worry since all the complexity lies within the compiler. They just need to learn two keywords.


## The answer: User-level Thread (Coroutine)
With the introduction of `async/await`, the code has become much more concise, almost indistinguishable from synchronous code. Is it possible to make asynchronous code identical to synchronous code? Yes!

User-level threads completely discard the thread mechanism provided by the operating system, thereby avoiding the virtualization mechanism of the VM. For instance, in a hardware with 8 cores, 8 system threads are generated and N user threads are allocated to run on these 8 system threads. The scheduling of N user threads is executed within the user process, resulting in a significantly lower context switch cost compared to the operating system.

At the same time, any operations that could potentially block system-level threads, like `sleep()` or `recv()`, must not be accessed by user-level threads. Should these operations be blocked, one of the 8 system threads will also be blocked. The Go Runtime handles all such system calls by employing a unified event loop for polling and dispatching.

In addition, since user-level threads are lightweight, there is no need for a thread pool. If a thread is needed, it can be created directly. For example, in Java, a WebServer almost certainly has a thread pool, whereas Go can allocate a goroutine to handle each request.

