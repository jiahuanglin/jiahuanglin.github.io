---
title: Pub-Sub design pattern
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-15 21:48:00 -0500
categories: [Software]
tags: [design pattern]
render_with_liquid: false
---

Pub-Sub design pattern is usually known as Observer pattern. I personally like to refer this pattern as `Pub-Sub` which I think better captures the essence of this pattern.

In GoF's book `Design Patterns`, it is defined as follows: 
> Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically


Let's look at the classical implementation of this design pattern.

Java version:

```java
public interface Subject { 
  void registerObserver(Observer observer); 
  void removeObserver(Observer observer); 
  void notifyObservers(Message message);
}

public interface Observer { 
  void update(Message message);
}

public class ConcreteSubject implements Subject { 
  private List observers = new ArrayList(); 
  
  @Override 
  public void registerObserver(Observer observer) { 
    observers.add(observer); 
  } 
  
  @Override 
  public void removeObserver(Observer observer) {
     observers.remove(observer); 
  } 
  
  @Override 
  public void notifyObservers(Message message) {
    for (Observer observer : observers) { 
      observer.update(message); 
    } 
  }
}

public class ConcreteObserverOne implements Observer { 
  @Override 
  public void update(Message message) {
    //TODO obtain notifications and process 
    System.out.println("ConcreteObserverOne is notified."); 
  }
}

public class ConcreteObserverTwo implements Observer { 
  @Override 
  public void update(Message message) { 
    //TODO obtain notifications and process 
    System.out.println("ConcreteObserverTwo is notified."); 
  }
```

Let's take a look at a concrete example and see how this pattern can help. Suppose we are developing a workflow management system. Whenever a workflow is created, we need to register the configuration of the workflow into elastic storage and push workflow into execution queue. We can implement this like the following:

```java
public class WorkflowManager {
  private StorageService storageService; // dependency injection
  private ExecutionQueue executionQueue; // dependency injection

  public void register(Workflow workflow) {
    // ...
    WorkflowConfig config = workflow.getConfiguration();
    storageService.save(config);
    executionQueue.push(workflow);
    // ...
  }
}
```

Now suppose, say after a workflow is registered, we put it on hold and notify the creator that the workflow has been validated and created and is ready to run instead of putting it into execution. In this case, we need to frequently modify the register() function code, which violates the open-close principle. Moreover, suppose a successful registration will require more and more follow-up operations. In that case, the logic of the register() function will become more and more complex, which will affect the readability and maintainability of the code. This is where the observer pattern comes in handy. Using the observer pattern, we can refactor the above code like the following:

```java
public interface RegObserver {
  void onRegSuccess(Workflow workflow);
}

public class RegStorageObserver implements RegObserver {
  private StorageService storageService;

  @Override
  public void onRegSuccess(Workflow workflow) {
    WorkflowConfig config = workflow.getConfiguration();
    storageService.save(config);
  }
}

public class RegNotificationObserver implements RegObserver {
  private NotificationService notificationService;

  @Override
  public void onRegSuccess(Workflow workflow) {
    Contact owner = workflow.getOwner();
    notificationService.sendMail(owner, "Workflow is ready...");
  }
}

public class WorkflowManager {
  private WorkflowValidator validator;
  private List<RegObserver> regObservers = new ArrayList<>();

  public void setRegObservers(List<RegObserver> observers) {
    regObservers.addAll(observers);
  }

  public void register(Workflow workflow) {
    if (!validator.validate(workflow)) {
      // ...
    }

    for (RegObserver observer : regObservers) {
      observer.onRegSuccess(workflow);
    }
  }
}
```

## Appendix
Golang implementation of Pub-Sub pattern:
```go
// subject.go
type subject interface {
    registerObserver(Observer observer)
    removeObserver(Observer observer)
    notifyObservers(string)
}

// event.go implements subject
type event struct {
    observers    []observer
    name         string
}

func newEvent(name string) *item {
    return &item{
        name: name,
    }
}

func (i *item) registerObserver(o observer) {
    i.observers = append(i.observers, o)
}

func (i *item) removeObserver(o observer) {
    i.observerList = removeFromslice(i.observers, o)
}

func (i *item) notifyObservers() {
    for _, observer := range i.observers {
        observer.update(i.name)
    }
}

// observer.go
type observer interface {
    update(string)
}

// listener.go implements observer
type listener struct {
    id string
}

func (l *listener) update(eventMsg string) {
    fmt.Printf("Listener %s received event %s\n", l.id, eventMsg)
}
```


