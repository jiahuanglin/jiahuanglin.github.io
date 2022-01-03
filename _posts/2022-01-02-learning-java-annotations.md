---
title: Learning Java annotations
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-01-02 22:34:00 -0500
categories: [Language]
tags: [Java, annotations]
---

## Overview

Java developers must have used @`Override` annotation which declares that an instance method overrides a method of the same name and the same parameter type of the parent class. The definition of @`Override` annotation looks like the following:
```java
package java.lang;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Override {
}
```

Note that the @`Override` annotation itself is annotated by two other meta-annotations (i.e., annotations that act on the annotation). One of them, @`Target`, is used to limit the type that can be annotated by the target annotation, in this case @`Override` can only be used to annotate methods.

@`Retention` is used to define the current annotation lifecycle. There are three different lifecycles for annotations:
1. `SOURCE`: the annotation appears only in the source code
2. `CLASS`: the annotation appears only in the source code and bytecode
3. `RUNTIME`: the annotation appears only in the source code, bytecode and runtime. 

Annotaions' lifecycle determines when they will be applied/used:

 - `SOURCE` annotations like @`Override` are used by the compiler and are not compiled into the .class file. They are discarded after compilation.
 - `CLASS` annotations are used by tools that handle .class files. These annotations are compiled into the .class file, but do not exist in memory after loading. Some underlying libraries use these annotations, and we generally do not have to deal with them ourselves.
 - `RUNTIME` annotations are the most commonly used annotations. They can be read during the runtime of the program and are always present in the JVM after loading.

## Self defined annotation

As we know, Java's annotation mechanism allows developers to customize annotations. Annotations do not affect the code logic. How annotations are used is entirely up to the tool. The compiler uses annotations of type `SOURCE`, so we generally only use them and do not implement them. Annotations of type `CLASS` are primarily used by the underlying tool library and involve class loading, which we rarely use. In practice, we almost only will implement self-defined `RUNTIME` type annotations.

Note that all annotations inherit from java.lang.annotation. The methods provided by Java for reading Annotation using the reflection API include:

- Determining whether an annotation exists for a `Class`, `Field`, `Method` or `Constructor`:
   ```java
    Class.isAnnotationPresent(Class)
    Field.isAnnotationPresent(Class)
    Method.isAnnotationPresent(Class)
    Constructor.isAnnotationPresent(Class)
    ```


- Get annotation:
    ```java
    Class.getAnnotation(Class)
    Field.getAnnotation(Class)
    Method.getAnnotation(Class)
    Constructor.getAnnotation(Class)
    ```


## Use case
Suppose we want to parse tables from the relational database. The table might contain various data formats like `date`, `text meg`, `integer array`, `floating-point numbers array` etc. Tables are user input. Only user knows what formats are there in the tables and we don't know what format is there beforehand. This means that we need to let the user bind the correct format parsers to the columns of the data table, and call the parser at runtime based on the column.

We can expose a parser registry class that holds a map from column name to parser class, or we can utilize annotations which is a neat solution for this scenerio:

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Processor {
    Class<? extends Parser> value();
}


public interface Parser {
  void setColumn(String column);
  void parse(String[][] rows);
}

public class DateParser implements Parser {

  private String columnName;

  public DateParser(String columnName) {
    this.columnName = columnName
  }

  // ...
}

public class IntArrayParser implements Parser {

  private String columnName;

  public IntArrayParser(String columnName) {
    this.columnName = columnName
  }

  // ...
}

public abstract class Table {
  // ...
}

public class TableLoader {

  public TableLoader() {
    // ...
  }
  
  public void load(Class tableClass, String fileName) {
    for (Field field : tableClass.getDeclaredFields()) {
      String columnName = field.getName();
      if (field.isAnnotationPresent(Parser.class)) {
          Class<? extends Parser> parserClass = field.getDeclaredAnnotation(Parser.class).value();
          Parser parser = parserClass.getConstructor(String.class).newInstance(columnName);
          // ... parse and other work
      }
    }

    if (field.isAnnotationPresent(Bind.class)) {
        Class<? extends Parser> parserClass = field.getDeclaredAnnotation(Bind.class).value();
    }
}


// user code
public class UserTable extends Table {
  @Parser(DateParser.class) Long birthday;
  @Parser(IntArrayParser.class) Integer[] account;
}
```




