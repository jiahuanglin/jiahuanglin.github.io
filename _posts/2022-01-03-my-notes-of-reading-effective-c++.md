---
title: My notes of reading effective C++
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-01-03 02:04:00 -0500
categories: [Software]
tags: [C++]
---

> Following is my note of reading effective c++ (this is an on-going post)

# Copy all parts of an object
In a mature object-oriented C++ system, there are only two ways of copying objects: `copy constructors` and `assignment operators` referred to as `copy` functions. Copy functions are compiler-generated functions by default, and the default copy function does copy the object in its entirety. Still, sometimes we choose to `overload` the copy function. And that's where the problem arises!

A correct implementation of the `copy` function would look like this:
```c++
class Customer{
  string name;
public:
  Customer(const Customer& rhs): name(rhs.name){}
  Customer& operator=(const Customer& rhs){
    name = rhs.name; // copy rhs's data
    return *this;
  }  
};
```

Perfect, right? But then, one day, we add a new data member and forget to update the copy function:

```c++
class Customer{
  string name;
  Date lastTransaction;
public:
  Customer(const Customer& rhs): name(rhs.name){}
  Customer& operator=(const Customer& rhs){
    name = rhs.name; // copy rhs's data
    return *this;
  }  
};
```

The lastTransaction is ignored and the compiler does not give any warnings (even at the highest warning level). Another common scenario is when we inherit from a parent class:

```c++
class PriorityCustomer: public Customer {
int priority;
public:
  PriorityCustomer(const PriorityCustomer& rhs)
  : priority(rhs.priority){}
  
  PriorityCustomer& 
  operator=(const PriorityCustomer& rhs){
    priority = rhs.priority;
  }  
};
```

The above code looks fine, but we forgot to copy the part of the parent class:

```c++
class PriorityCustomer: public Customer {
int priority;
public:
  PriorityCustomer(const PriorityCustomer& rhs)
  : Customer(rhs), priority(rhs.priority){}
  
  PriorityCustomer& 
  operator=(const PriorityCustomer& rhs){
    Customer::operator=(rhs);
    priority = rhs.priority;
  }  
};
```

In short, when we implement the copy function:
  1. **make a complete copy of the current object's data (local data).**
  2. **call all the corresponding copy functions in the parent class.**

You may notice the repetition of the code, but don't let the `copy` constructor and the `assignment` operator call each other. They have completely different semantics! C++ doesn't even provide a syntax for the `assignment` operator to call the copy constructor. Conversely, it would compile to have the `copy` constructor call the `assignment` operator. But since the precondition for the `copy` constructor is an `uninitialized` object, the precondition for the `assignment` operator is an `initialized` object. Such a call is not a good design and may cause logical confusion.



# Use object to manage resources
Those who are familiar with smart pointers will certainly not find this strange:

```c++
{
  // ...
  Binder *pBin = binder_factory.createBinder();
  // ...
  delete pBin;
}
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




