---
title: Visitor design pattern and k8s kubectl implementation
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-01-01 21:11:00 -0500
categories: [Software]
tags: [design pattern, k8s, Java, Go]
---

## Visitor pattern
A machine learning framework will typically implement its dataset I/O functionality. Data source are usually of different types and format. Say we have 3 kinds of format to parse, namely CSV, text and images. We could have our code implemented like the following:

```java
public abstract class Parser {

  protected String filepath;

  public Parser(String filepath) {
    this.filepath = filepath;
  }

  public abstract Dataset parse();
}

public class CsvParser extends Parser {
  @Override
  public Dataset parse() {
    //...
  }
}

public class TextParser extends Parser {
  @Override
  public Dataset parse() {
    //...
  }
}

public class ImageParser extends Parser {
  @Override
  public Dataset parse() {
    //...
  }
}
```

The disadvantage of this approach is that data sources can be in many different formats, and the framework needs to provide additional I/O support for each data source. We already have 3 classes with 3 different formats and one parsing functionality. If the framework also needs to provide serialization, compression, and transformation for each output data format, then even with only three data formats, we would have 3 * 4 = 12 classes. Given that the number of data source format can be big, the above implementation doesn't look like a scalable solution.

If we could decouple the data format from the I/O function, then the total number of classes would be an addition result rather than a multiplication result. The implementation looks like the following:

```java
public abstract class DataFile {

  protected String filepath;

  public DataFile(String filepath) {
    this.filepath = filepath;
  }
}

public class CsvFile extends DataFile {
  public CsvFile(String filepath) {
    super(filepath);
  }
}

public class TextFile extends DataFile {
  public TextFile(String filepath) {
    super(filepath);
  }
}

public class ImageFile extends DataFile {
  public ImageFile(String filepath) {
    super(filepath);
  }
}

public class DataParser {
  // return a parsed dataset object
  public Dataset parse(CsvFile file) {
    // ...
  }

  public Dataset parse(TextFile file) {
    // ...
  }

  public Dataset parse(ImageFile file) {
    // ...
  }
}

public class DataTransformer {
  public Dataset transform(CsvFile file) {
    // ...
  }

  public Dataset transform(TextFile file) {
    // ...
  }

  public Dataset transform(ImageFile file) {
    // ...
  }
}
```

However, the above code doesn't compile in Java. The implementation makes an inheritance relationship with the `DataFile` class and then uses overloaded functions in the `Parser`/`Serializer` class to handle different file types. But because the binding time of Java polymorphism and function overloading is different, the compiler doesn't know what the parameters passed in the end during compile time, so it reports an error.

We can apply visitor pattern like the following:
```java
public abstract class DataFile {

  protected String filepath;

  public DataFile(String filepath) {
    this.filepath = filepath;
  }

  public abstract void accept(Visitor visitor);
}

public class CsvFile extends DataFile {
  public CsvFile(String filepath) {
    super(filepath);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}

public class TextFile extends DataFile {
  public TextFile(String filepath) {
    super(filepath);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  // ...
}

public class ImageFile extends DataFile {
  public ImageFile(String filepath) {
    super(filepath);
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  // ...
}

public interface Visitor {
  public Dataset visit(CsvFile file);
  public Dataset visit(TextFile file);
  public Dataset visit(ImageFile file);
}

public class DataParser implements Visitor {
  // return a parsed dataset object
  public Dataset visit(CsvFile file) {
    // ... parsing
  }

  public Dataset visit(TextFile file) {
    // ... parsing
  }

  public Dataset visit(ImageFile file) {
    // ... parsing
  }
}

public class DataTransformer {
  // return a parsed dataset object
  public Dataset visit(CsvFile file) {
    // ... transform
  }

  public Dataset visit(TextFile file) {
    // ... transform
  }

  public Dataset visit(ImageFile file) {
    // ... transform
  }
}
```

## Visitor pattern in Kubectl
```go
type VisitorFunc func(*Info, error) error

type Visitor interface {
    Visit(VisitorFunc) error
}

type Info struct {
    Namespace   string
    Name        string
    OtherThings string
}

func (info *Info) Visit(fn VisitorFunc) error {
  return fn(info, nil)
}
```

Note that the `Visit()` method of the Visitor interface is implemented `for Info`, and instead of passing the object to `Visit()`, the implementation directly passes the funtion pointer (another form of runtime `overloading`).

```go
// visitors for each field of Info struct

// Name visitor
type NameVisitor struct { 
  visitor Visitor
}

func (v NameVisitor) Visit(fn VisitorFunc) error { 
  return v.visitor.Visit(
    func(info *Info, err error) error {
      fmt.Println("NameVisitor() before call function") 
      err = fn(info, err) 
      if err == nil { 
        fmt.Printf("==> Name=%s, NameSpace=%s\n", info.Name, info.Namespace) 
      }
      fmt.Println("NameVisitor() after call function") 
      return err 
    }
  )
}


// Other visitor
type OtherThingsVisitor struct { 
  visitor Visitor
}

func (v OtherThingsVisitor) Visit(fn VisitorFunc) error {
  return v.visitor.Visit(
    func(info *Info, err error) error {
      fmt.Println("OtherThingsVisitor() before call function") 
      err = fn(info, err) 
      if err == nil { 
        fmt.Printf("==> OtherThings=%s\n", info.OtherThings) 
      } 
      fmt.Println("OtherThingsVisitor() after call function") 
      return err   
    }
  )
}

// Log visitor
type LogVisitor struct { 
  visitor Visitor
}

func (v LogVisitor) Visit(fn VisitorFunc) error {
  return v.visitor.Visit(
    func(info *Info, err error) error {
      fmt.Println("LogVisitor() before call function") 
      err = fn(info, err)
      fmt.Println("LogVisitor() after call function")
      return err 
    }
  )
}
```



### Reference
[Kubectl source code visitor.go](https://github.com/kubernetes/kubernetes/blob/cea1d4e20b4a7886d8ff65f34c6d4f95efcb4742/staging/src/k8s.io/cli-runtime/pkg/resource/visitor.go)