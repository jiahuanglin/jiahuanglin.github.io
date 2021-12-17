---
title: How deque is implemented in STL
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-16 17:41:00 -0500
categories: [Data Structure]
tags: [C++, STL]
---

A double-ended queue is open at both ends compared to the regular queue. Both `push` and `pop` operations can be performed at the head and tail of the queue. Essentially, a double-ended queue is just a queue that supports FIFO insertion and deletion at both ends and semantically does not support random access, insertion and deletion based on subscripts like an array.

### How to implement deque
If we need to implement a fixed-length deque, we just need to allocate an array of size `N`. Two indices represent the `head` and `tail`. When there is an `insertFront()` operation, we do `array[(--front+N)%N] = value`. This implementation essentially constitutes a circular deque. And the `add()/delete()` operations are `O(1)` operations since all we need is to move the `head/tail indices`. We can even extend this solution to support random access by the element order.

But the problem with this implementation is that if the deque is of dynamic length, each expansion will require linear time to copy the array.

Suppose we implement deque in a linked list way. In that case, the `add()/delete()` operations are constant time, and we don't need to consider the expansion of deque because each time we add an element, we only need to allocate one more node in memory. Such implementation looks lovely on the surface, but the problem with linked list is that each node in the chain may be discontinuous in memory. A cache line load may not read exactly the next node, and one main memory lookup is very expensive. Such implementation can lead to a degradation of deque performance.

Is there a way to implement deque that supports expansion in `O(1)`, and also makes use of locality pattern of memory access?

### How STL implements deque
In STL, the memory layout of deque composes segments of contiguous space, and the address information of these spaces is stitched together with another array structure. The time complexity of `insertion()` and `deletion()` at the front and tail is `O(1)`. For example, each time we use up an adjacent space, deque will request a new space and link it to the end of its segment space. So deque does not need to pay the high cost of replication and copying every time since we expand it like vector, nor does it need to request memory every time a new node is inserted like a linked list. Deque needs to keep a map to maintain a contiguous segment of memory space, which is called a buffer.

```cpp
template<class T, class Ref, class Ptr, size_t BuffSize = 8>
struct __DequeIterator
{
	typedef T**										MapPointer;
	typedef __DequeIterator<T, T&, T*, BuffSize>	Iterator;
	typedef __DequeIterator<T, Ref, Ptr, BuffSize>	Self;

	//typedef __DequeIterator Self;

	// constructor
	__DequeIterator()
	:_cur(NULL)
	,_first(NULL)
	,_last(NULL)
	,_node(NULL)
	{}

	__DequeIterator(T* cur, MapPointer node)
		:_cur(cur)
		,_first(*node)
		,_last(_first + BuffSize)
		,_node(node)
	{}
	
	__DequeIterator(const Iterator& s)
		:_cur(s._cur)
		,_first(s._first)
		,_last(s._last)
		,_node(s._node)
	{}

	// self increment
	Self& operator++()
	{
		++_cur;
		if (_cur == _last){
			SetNode(_node + 1);
			_cur = _first;
		}
		return *this;
	}

	Self operator++(int)
	{
		Self temp(*this);
		++(*this);
		return temp;
	}

  // self decrement
	Self& operator--()
	{
		if (_cur == _first){
			SetNode(_node - 1);
			_cur = _last;
		}
		--_cur;
		
		return *this;
	}

	Self operator--(int)
	{
		Self temp(*this);
		--(*this);
		return temp;
	}

	// comparison
	bool operator!=(const Self& s)const
	{
		return _cur != s._cur;
	}

	bool operator==(const Self& s)const
	{
		return !(operator!=(s));
	}

	// de-reference
	Ref operator *()const
	{
		return *_cur;
	}

	Ptr operator->()const
	{
		return &(operator*());
	}


	// reset iterator position
	void SetNode(MapPointer newNode)
	{
		_node = newNode;
		_first = *newNode;
		_last = *newNode + BuffSize;
	}

	T* _cur;
	T* _first;
	T* _last;
	MapPointer _node;
}
```


