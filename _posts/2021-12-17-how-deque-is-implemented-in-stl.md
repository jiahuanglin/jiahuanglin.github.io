---
title: How deque is implemented in STL
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-17 17:41:00 -0500
categories: [Algorithm & Data Structure]
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

Basically the iterator really has to pay attention to the buffer boudary, which brings us `SetNode()` method. Every time when iterator increments/decrements at the boudary, we `SetNode()` to next buffer (which also sets up `_first` and `_last`).

```cpp
template<class T, size_t BuffSize = 8>
class Deque
{
	typedef T** MapPointer;
public:
	typedef __DequeIterator<T, T&, T*, BuffSize>				Iterator;
	typedef __DequeIterator<T, const T&, const T*, BuffSize>	ConstIterator;
	typedef T& Ref;
	typedef const T& ConstRef;
public:
	//construction
	Deque()
		:_map(NULL)
		,_mapSize(0)
		,_size(0)
	{
		assert(BuffSize > 2);
	}

	void PushBack(const T& value)
	{
		if (NULL == _map || _finish._cur == _finish._last - 1)	
			_PushBackAux(value);								
		else{
			*(_finish._cur) = value;
			++_finish._cur;
			++_size;
		}
	}

	void PushFront(const T& value)
	{
		if (NULL == _map || _start._cur == _start._first)
			_PushFrontAux(value);
		else{
			*(_start._cur-1) = value;
			--_start._cur;
			++_size;
		}
	}

	void PopBack()
	{
		--_finish;
		if (_finish._cur == _finish._last){
			delete[] * (_finish._node - 1);
			*(_finish._node - 1) = NULL;
		}

		if (_finish == _start){
			delete[] _map;
			_map = NULL;
		}
		--_size;
	}

	void PopFront()
	{
		++_start;
		if (_start._cur == _start._first){
			delete[] * (_start._node - 1);
			*(_start._node - 1) = NULL;
		}

		if (_finish == _start){
			delete _map;
			_map = NULL;
		}
		--_size;
	}

	//Iteration
	Iterator Begin()
	{
		return _start;
	}

	ConstIterator Begin()const
	{
		return _start;
	}

	Iterator End()
	{
		return _finish;
	}

	ConstIterator End()const
	{
		return _finish;
	}

	//capacity
	size_t Size()
	{
		return _size;
	}

	bool Empty()
	{
		return _start == _finish;
	}

	T& Back()
	{
		assert(0 != _size);
		if (_finish._cur != _finish._first)
			return *(_finish._cur - 1);
		else{
			Iterator it(_finish);
			--it;
			return *(it._cur);
		}
	}

	T& Front()
	{
		assert(0 != _size);
		return *(_start._cur);
	}
	~Deque() // destructor to release memory
	{
		if (_map){
			T** cur = _start._node;
			for (; cur != _finish._node; ++cur){
				if (*cur){
					delete[] * cur;
					*cur = NULL;
				}
			}

			if (*cur){
				delete[] * cur;
				*cur = NULL;
			}
			delete[] _map;
			_map = NULL;
		}
	}

protected:
	void _PushBackAux(const T& value)
	{
		if (NULL == _map || _map + _mapSize - 1 == _finish._node){
			size_t newSize = _mapSize == 0 ? 2 : _mapSize * 2;
			MapPointer temp = new T*[newSize];  // allocate new room
			for (size_t i = 0; i < newSize; ++i)
				*(temp + i) = NULL;
			
			size_t addToNode = _mapSize / 2;	//0
			for (size_t i = 0; i < _mapSize; ++i)
				temp[addToNode + i] = _map[i];

			size_t oldStartNode = _start._node - _map;//0
			size_t oldFinishNode = _finish._node - _map;//0

			if (_map)
				delete[] _map;

			_map = temp;

			if (NULL != _finish._cur){
				_start.SetNode(temp + addToNode + oldStartNode);
				_finish.SetNode(temp + addToNode + oldFinishNode);
			}
			else{
				*(_map) = new T[BuffSize];

				_finish.SetNode(temp);
				_start.SetNode(temp);
				_finish._cur = *(_map) + BuffSize / 2;
				_start._cur = *(_map) + BuffSize / 2;
				*(_finish._cur++) = value;
				++_size;
				_mapSize = newSize;
				return;
			}
			_mapSize = newSize;
		}

		*(_finish._cur) = value;
		*(_finish._node + 1) = new T[BuffSize];
		_finish.SetNode(_finish._node + 1);
		_finish._cur = _finish._first;
		++_size;
	}
	void _PushFrontAux(const T& value)
	{
		if (NULL == _map || _map == _start._node){
			size_t newSize = _mapSize == 0 ? 2 : _mapSize * 2;
			MapPointer temp = new T*[newSize];

			size_t addToNode = _mapSize / 2;
			for (size_t i = 0; i < _mapSize; ++i)
				temp[addToNode + i] = _map[i];

			size_t oldStartNode = _start._node - _map;
			size_t oldFinishNode = _finish._node - _map;

			if (_map)
				delete[] _map;

			_map = temp;

			if (NULL != _start._cur){
				_start.SetNode(temp + addToNode + oldStartNode);
				_finish.SetNode(temp + addToNode + oldFinishNode);
			}
			else{
				*(_map) = new T[BuffSize];

				_start.SetNode(_map);
				_finish.SetNode(_map);

				_start._cur = *(_map)+BuffSize / 2;
				_finish._cur = *(_map)+BuffSize / 2;

				*(_start._cur - 1) = value;
				--_start._cur;
				++_size;
				_mapSize = newSize;
				return;
			}
			_mapSize = newSize;
		}

		*(_start._node - 1) = new T[BuffSize];
		_start.SetNode(_start._node - 1);
		_start._cur = _start._last - 1;
		*(_start._cur) = value;
		++_size;
	}
protected:
	MapPointer _map;
	Iterator _start;
	Iterator _finish;
	size_t _mapSize;
	size_t _size;
};
```



