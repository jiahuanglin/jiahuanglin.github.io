---
title: Effective C++ -- use object to manage resources
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-01-03 02:04:00 -0500
categories: [Language]
tags: [C++]
---

> Following is my note of reading effective c++

Those who are familiar with smart pointers will certainly not find this strange:

```c++
Binder *pBin = binder_factory.createBinder();
...
delete pBin;
```
The C++ compiler does not provide an automated garbage collection mechanism, so it's the programmer's responsibility to release resources. We are always asked to use new and delete in pairs. The above code does work properly without leaking memory. However, the problem is that the `createBinder()` function shifts the responsibility of releasing the resource over to the caller, but does not explicitly declare this. Hence, the caller is sometimes unaware of it. Even if the caller knows that the resource needs to be destroyed, it may not be released in time due to flow control statements or exceptions.

Fortunately, we can wrap the resource in an object and release it in a destructor. This eliminates the need for clients to maintain the resource's memory. One such object is `std::unique_ptr`, called a smart pointer. A typical usage scenario where resources are stored in heap space but only used locally is:

```c++
void f(){
  std::unique_ptr<Binder> pBin(binder_factory.createBinder());
}
```

At the end of the `f()` call `pBin` exits the scope and the destructor is called, eventually causing the resource to be freed. It would be better to have createInvestment return a smart pointer directly. As you can see, the key to using objects to manage resources is to put them into a resource management object as soon as they are created, and to use the resource management object's destructor to ensure that they are released.

The framework for implementing the resource management object is exactly the `RAII` principle: `acquisition is initialization`, using a resource to initialize a smart pointer. The resource is released in the pointer's destructor.

With `RAII`, instead of writing

```c++
std::mutex mtx;

void f(){ 
  mtx.lock(); 
  //... 
  mtx.unlock();
}
```
we should be writing

```c++
std::mutex mtx;

void f(){ 
  std::lock_guard<std::mutex> guard(mtx); 
  //...
}
```


### Reference
[Effective C++](https://www.amazon.ca/Effective-Specific-Improve-Programs-Designs/dp/0201563649)




