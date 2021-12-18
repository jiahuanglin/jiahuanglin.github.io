---
title: Decorator design pattern
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-18 11:12:00 -0500
categories: [Software]
tags: [design pattern, Java, Go]
---

> The decorator pattern is a way to dynamically add functionality to an instance of an object at runtime.



```go
// notifier interface
type notifier interface {
    notify() bool
}

// default_notifier.go
type defaultNotifier struct {
}

func (d *defaultNotifier) notify() bool {
    // phone bannr push
    return success
}

// sms_notifier.go
type smsNotifier struct {
    notifier notifier
}

func (s *smsNotifier) notify() bool {
    success := s.notifier.notify()
    // ...
    // sms push
    return success
}


// email_notifier.go
type emailNotifier struct {
    notifier notifier
}

func (e *emailNotifier) notify() bool {
    success := e.notifier.notify()
    // ...
    // email push
    return success
}


// main.go
func main() {

    notifier := &defaultNotifier{}

    // add sms functionality
    notifierSms := &smsNotifier{
        notifier: notifier,
    }

    // add email functionality
    notifierSmsEmail := &emailNotifier{
        notifier: notifierSms,
    }

    notifierSmsEmail.notify()
}
```