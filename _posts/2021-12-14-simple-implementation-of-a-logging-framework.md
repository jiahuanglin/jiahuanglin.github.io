---
title: Simple implementation of a logging framework
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-14 00:16:00 -0500
categories: [Software]
tags: [logging, framework, server, Java]
---

Details of an Apache Log4j vulnerability were recently made public, which attackers could exploit to execute code remotely. The matter has caused an extremely heated discussion online and has led to countless programmers working overtime to change the code. Numerous software is implemented in java, each equipped with a logging system that depends on log4j. The silly-sounding question is, why do we need a logging module?

## Why do we need logging
When we talk about software development, we refer to a pipeline: development, testing, and then release to production. No one can write bug-free code or tests that cover every single corner case all the time. Usually, programmers use debuggers or simply just `printf` to debug programs. However, when we launched the server in the production environment, we could no longer troubleshoot problems through debuggers, mainly because we couldn't just hang the execution and leave the user's request stuck there. In addition, the production environment is typically distributed/multi-processed, which in general will make debugger useless.

Since we can't debug through a debugger, we need a logging system to track and recall the program's behaviour and locate the problem.

## Functionalities of a logging framework
Generally speaking, a logging system will support the following functionalities over a simple `printf`:

1. Supports hierarchy of messages — namely, DEBUG, INFO, WARNING, ERROR, etc. So developers don't need to comment out the output statements after development but only adjust the level.
2. Support for multiple output destinations. For example, we can have a log printed on the screen and save it to a file in the meantime for persistent storage and easy analysis.
3. Support for log scrolling. When there are too many log files, the log library can delete some obsolete and irrelevant logs.
4. Better performance. Compared with `printf`, log libraries are usually highly optimized for formatting and output performance, not affecting the program's execution efficiency.
5. Multi-threaded processing. With `printf`, when multiple threads are outputting simultaneously, a single line of logs may mix up the output of numerous threads, and the log library can synchronize the work correctly and efficiently to avoid output confusion.

## How to implement a logging module

```java
public enum LoggingLevel {
  DEBUG(1, "DEBUG"), INFO(2, "INFO"), WARNING(3, "WARNING"), ERROR(4, "ERROR");

  private int level;
  private String name;

  public LoggingLevel(int level, String name) {
    this.level = level;
    this.name = name;
  }

  public String toString() {
        return name;
  }
}

public class LoggingEvent {
    public long timestamp;
    private LoggingLevel level;
    private Object message;
    private String threadName;
    private long threadId;
    private String loggerName;
    
    public LoggingEvent withLevel(LoggingLevel level) {
      this.level = level;
      return this;
    }

    public LoggingEvent withMessage(Object message) {
      this.message = message;
      return this;
    }

    public LoggingEvent withLoggerName(String loggerName) {
      this.loggerName = loggerName;
      return this;
    }
    // ...

    public String toString() {
      return String.format(
          "[%s-%d]-[%s] %s, thread %s, threadID %d", 
          loggerName, 
          timestamp, 
          level,
          message,
          threadName,
          threadId
        )
    }
}

public interface Writer {

  void emit(LoggingEvent event);

}

public class ConsoleWriter implements Writer {

  private OutputStream out = System.out;

  @Override
  public void emit(LoggingEvent event) {
      try {
          out.write(event.toString().getBytes(encoding));
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
}

public interface Logger {

  void info(String msg);

  void debug(String msg);

  void warning(String msg);

  void error(String msg);

  String getName(); 
}


public class DefaultLogger implements Logger{
    private String name;
    private Writer writer;
    private LoggingLevel level = Level.INFO;
    private int outputLevel;

    @Override
    public void info(String msg) {
        filterAndLog(Level.INFO, msg);
    }

    @Override
    public void debug(String msg) {
        filterAndLog(Level.DEBUG, msg);
    }

    @Override
    public void warning(String msg) {
        filterAndLog(Level.WARN, msg);
    }

    @Override
    public void error(String msg) {
        filterAndLog(Level.ERROR, msg);
    }
    
    private void filterAndLog(LoggingLevel level, String msg){
      LoggingEvent e = new LoggingEvent()
                          .withLevel(level)
                          .withMessage(meg)
                          .withLoggerName(getName());
      if(level.toInt() >= outputLevel){
          writer.emit(e);
      }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    //...
}
```

