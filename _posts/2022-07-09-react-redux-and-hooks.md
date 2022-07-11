---
title: React, redux and hooks
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-07-09 14:58:00 -0500
categories: [Software]
tags: [framework, frontend]
---

## React & React hooks

### Class Component vs Functional Component
The following is an example of writing a class component in React:
```javascript
class Welcome extends React.Component {
  render() {
    return <h1>Hello, {this.props.name}</h1>;
  }
}
```

The React team hopes that components don't become complex containers but preferably just pipelines for data flow. The best way to write components should be functions, not classes, like the following:

```javascript
function Welcome(props) {
  return <h1>{props.name}</h1>;
}
```

However, there are significant limitations to this writing style; it must be a pure function, cannot contain state, and does not support lifecycle methods, so it cannot replace classes.

React Hooks are designed to be an enhanced version of functional components, writing a fully functional component without using "classes" at all. In another words, components should be written as purely functional as possible, and if external functionality and side effects are needed, external code is "hooked" in with hooks. React Hooks are those hooks.

React provides some common hooks by default, but you can also wrap your own hooks.

All hooks introduce external functionality to the function, so React makes it a convention to name all hooks with the use prefix to make them easy to identify. If you want to use the xxx function, the hook will be named usexxx.

The following are some of the most common hooks that React provides by default.

```javascript
useState()
useContext()
useReducer()
useEffect()
```

### useEffect()
`useEffect()` itself is a function, provided by the React framework, that can be called from within the function component.

For example, we want the page title (document.title) to change when the component is loaded. Then, the action of changing the page title is a side effect of the component, and must be implemented with `useEffect()`.

```javascript
import React, { useEffect } from 'react';

function Welcome(props) {
  useEffect(() => {
    document.title = 'Loading complete';
  });
  return <h1>{props.name}</h1>;
}
```

In the above example, the argument to `useEffect()` is a function that is the side effect (changing the page title) to be completed. Once the component is loaded, React executes this function.

What `useEffect()` does is to specify a side effect function that is automatically executed every time the component is rendered. The side effect function is also executed after the component is loaded in the page DOM for the first time.

Sometimes we don't want useEffect() to be executed every time it renders, so we can use its `2nd argument` to specify the dependencies of the side effect function using an array, and only if the dependencies change will it be re-rendered.

```javascript
function Welcome(props) {
  useEffect(() => {
    document.title = `Hello, ${props.name}`;
  }, [props.name]);
  return <h1>{props.name}</h1>;
}
```

In the above example, the second argument to `useEffect()` is an array specifying the dependency (props.name) of the first argument (the side-effect function). The side effect function will be executed only if this variable changes.

If the second argument is an empty array, the side effect parameter has no dependencies. Therefore, the side effect function will only be executed once the component is loaded into the DOM at this point, but never not again whenever the component re-renders. This is because the side effect does not depend on any variables, so no matter how those variables change, the result of the side effect function will not change, so running it once is enough.

### Hook Dependencies
Hooks allows you to listen for a change in data. This change may trigger a refresh of a component, create a side effect, or refresh a cache. Defining which data changes to listen for is a matter of specifying the Hooks' dependencies. However, it is important to note that dependencies are not a special mechanism of built-in Hooks, but can be considered as a design pattern. Hooks that have similar requirements can be implemented using this pattern. When defining a dependency, we need to pay attention to the following three points: 
1. The variable defined in the dependency must be used in the callback function. Otherwise, it is meaningless to declare dependency. 
2. A dependency is generally an array of constants, not a variable. 
3. React uses shallow comparisons to compare whether dependencies have changed, so pay special attention to arrays or object types. If you are creating a new object each time, even if it is equivalent to the previous value, it will assume that the dependencies have changed. This area can easily lead to bugs when you first start using Hooks. For example, the following code: 

```javascript
function Sample() { 
    // Here a new array is created each time the component is executed 
    const todos = [text: 'Learn hooks.'}]; 
    useEffect(() => {
          console.log('Todos changed.'); 
      }, [todos]
    );
}
```

The code's original intent was to generate side effects when todos change. Still, here the todos variable is created within the function, effectively generating a new array each time. So the comparison of references made while acting as dependencies is considered to have changed.

## Redux

Redux is a useful architecture, but it's not a must-have. In fact, for the most part, you can get away without it and use React.

> "If you don't know if you need Redux, you don't need it."
>
> "You only need Redux if you encouter a problem that React just can't solve."
> 
> Dan Abramov, the creator of Redux


Simply Put, if your UI layer is straightforward and doesn't have a lot of interaction, Redux is unnecessary and adds complexity.

- Complex user usage
- Users with different identities have different usage styles (e.g., regular users and administrators)
- Multiple users can collaborate
- There is a lot of interaction with the server, or WebSockets are used
- View has to get data from multiple sources

These are the situations where Redux is appropriate: `multiple interactions, multiple data sources`.

From a component perspective, consider using Redux if your application has the following scenarios

- A component has state that needs to be shared
- A piece of state needs to be available anywhere
- A component needs to change global state
- A component needs to change the state of another component

In the above scenario, the code will quickly become a mess if you don't use Redux or other state management tools and don't handle reading and writing state according to certain rules. You need a mechanism to query state, change state, and propagate state changes simultaneously.

### Store
Store is the place where data is stored, you can think of it as a container. There can be only one Store for the whole application.

Redux provides the createStore function, which is used to generate a Store.

```javascript
import { createStore } from 'redux';
const store = createStore(fn);
```

In the above code, the `createStore` function accepts another function as an argument and returns the newly generated Store object.

### State
The Store object contains all the data. If you want to get the data at a certain point in time, you have to generate a snapshot of the Store. This collection of data at a point in time is called a State.

The State of the current moment can be obtained by store.getState().

```javascript
import { createStore } from 'redux';
const store = createStore(fn);

const state = store.getState();
```

Redux specifies that a State corresponds to a View, and as long as the State is the same, the View is the same. If you know the State, you know what the View is, and vice versa.

### Action
Changes to the State will result in changes to the View. Action is a notification from the View that the State is supposed to change.

Action is an object. The type attribute is required and indicates the name of the Action. The other properties can be set freely, and the community has a specification to follow.

```javascript
const action = {
  type: 'ADD_TODO',
  payload: 'Learn Redux'
};
```

In the above code, the name of the Action is `ADD_TODO` and the information it carries is the string Learn Redux.

As you can understand, the Action describes what is currently happening. The only way to change the state is to use the Action, which transports data to the Store.

