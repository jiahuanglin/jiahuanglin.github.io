---
title: Pub-Sub design pattern
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-15 21:48:00 -0500
categories: [Tech Blog, Design Pattern]
tags: [summary, methodology]
---

Pub-Sub design pattern is usually known as Observer pattern. I personally like to refer this pattern as `Pub-Sub` which I think better captures the essence of this pattern.

In GoF's book `Design Patterns`, it is defined as follows: 
> Define a one-to-many dependency between objects so that when one object changes state, all its dependents are notified and updated automatically


Let's look at the classical implementation of this design pattern.

```Java
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
    System.out.println("ConcreteObserverOne is notified."); 
  }
```

Let's take a look at a concrete example and see how this pattern can help. Suppose we are developing a workflow management system. Whenever a workflow is created, we need to register the configuration of the workflow into elastic storage and push workflow into execution queue. We can implement this like the following:

```Java

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

```Java
public interface RegObserver {
  void handleRegSuccess(Workflow workflow);
}

public class RegStorageObserver implements RegObserver {
  private StorageService storageService;

  @Override
  public void handleRegSuccess(Workflow workflow) {
    WorkflowConfig config = workflow.getConfiguration();
    storageService.save(config);
  }
}

public class RegNotificationObserver implements RegObserver {
  private NotificationService notificationService;

  @Override
  public void handleRegSuccess(Workflow workflow) {
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
      observer.handleRegSuccess(workflow);
    }
  }
}
```




