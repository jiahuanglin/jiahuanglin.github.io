---
title: Decorator design pattern and Java I/O package
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-18 11:12:00 -0500
categories: [Software]
tags: [design pattern, Java, Go]
---

> The decorator pattern is a way to dynamically add functionality to an instance of an object at runtime.

If we want to read data from file with Java, our code will look something like the following:
```java
InputStream in = new FileInputStream("demo.txt");
InputStream bin = new BufferedInputStream(in);
byte[] data = new byte[128];
while (bin.read(data) != -1) {
  //...
}
```
We almost always need to do the following:
> Create a `FileInputStream` object first, and then pass it to `BufferedInputStream` object to use. 

Why not just have a `BufferedFileInputStream` class that inherits from `FileInputStream` and supports caching? This way we can create a BufferedFileInputStream object like the code below and open the file to read the data, wouldn't it be easier to use?

The truth is this approach is not scalable. Let's say in addition to adding buffering function, we also want encryption and decryption support when transmitting data, as well as I/O from different data source like `socket` and `pipe`. 3 different final data sources with 2 functions will need 6 subclasses in total. If we continue to add functionalities & data source support, the subclasses will explode.

Instead of utilizing inheritance, utilizing composition will keep the class structure relatively simple. The following code shows how Java `I/O` accomplished this with decorator pattern:

```java
public abstract class InputStream {
  //...
  public int read(byte b[]) throws IOException {
    return read(b, 0, b.length);
  }
  
  public int read(byte b[], int off, int len) throws IOException {
    //...
  }
  
  public long skip(long n) throws IOException {
    //...
  }
  
  public void close() throws IOException {}

  //...

}

public class BufferedInputStream extends InputStream {
  protected volatile InputStream in;

  protected BufferedInputStream(InputStream in) {
    this.in = in;
  }
  
  //...
}

public class DataInputStream extends InputStream {
  protected volatile InputStream in;

  protected DataInputStream(InputStream in) {
    this.in = in;
  }
  
  //...
}
```

The purpose of the `decorator pattern` is to add additional functionalities to the original layer in a layering manner so that we can get the ultimate function we want by combining them.



### Appendix

Following is a Golang example of the `decorator pattern`:
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