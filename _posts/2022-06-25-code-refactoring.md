---
title: Code refactoring
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-06-25 10:23:00 -0500
categories: [Software]
tags: [refactoring]
---
> Refactoring is the improvement of the internal structure of software without changing its observable behavior.


## When to refactor

### When adding new features
The most common time to refactor is when I want to add a new feature to the software. 

At this point, the immediate reason for refactoring is often to help me understand the code that needs to be changed - code that may have been written by someone else or by me. Part of the reason for doing this is to make the code easier to understand the next time I look at it, but the main reason is this: I can understand more from it if I get the structure of the code straightened out as I move forward.

Another motivation for refactoring here is the code's design doesn't help me easily add the features I need. Part of the reason for doing this is to make it easier to add new features in the future, but the main reason is the same: I've found it to be the quickest route.

Refactoring is a fast and fluid process, and once it's done, new features are added more quickly and smoothly.

### When fixing bugs
Most of the time, refactoring is used during debugging to make the code more readable.

As I look at the code and try to understand it, I use refactoring to help deepen my understanding.

I find that working with the code in this way often helps me to identify bugs.
Think of it this way: if you get a bug report, that's a sign that you need to refactor because the code isn't clear enough - not clear enough to see the bug at a glance.

### When reviewing code
Code reviews are crucial for writing clear code. Refactoring helps us review other people's codes as well as the code review process to get more concrete results.

My code may be apparent to me but not to others. This is unavoidable because it is difficult to get developers to put themselves in the shoes of people unfamiliar with what they are doing.

Code review also gives more people the opportunity to make useful suggestions; after all, there are only so many good ideas one can come up with in a week.


## Encapsulation

## Simplify conditional logic

### Replace Conditional with Polymorphism

## Reorganize data
