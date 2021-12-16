---
title: JVM execution engine - interpreter and JIT
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-16 14:33:00 -0500
categories: [Sytem]
tags: [JIT, JVM]
---

Common compiled languages such as C++ usually compile the code directly into machine code that the CPU understands to run. On the other hand, to achieve the "compile once, run everywhere" feature, Java divides the compilation process into two parts to execute.

### How JVM executes JAVA code
Usually, the JVM contains two core modules: the executor and the memory manager, where the executor is specifically used to execute the bytecode. The most widely used virtual machine is Hotspot, whose executor includes an interpreter and a JIT compiler. 

Before interpreter can start executing Java code, the first step is to compile the source code into byte code through `javac`. This process includes lexical analysis, syntax analysis, semantic analysis. Next, the interpreter directly interpretes bytecode and executes line by line without compilation. In the meantime, the virtual machine collects meta-data regarding the program's execution. The compiler (JIT) can gradually come into play based on this data. It will perform backstage compilation - compiling the bytecode into machine code. But JIT will only compile code identified as a hotspot by the JVM.

Let's look at an example.

```java
import java.lang.Math;

public class ByteCodeDemo {

    public static int absDifference(int a, int b) {
        int difference = a - b;
        return Math.abs(difference);
    }

    public static void main(String[] args) {
        System.out.println(absDifference(2, 1));
    }
}
```
One can use the javap command to see its bytecode:

```bash
>>> javac ByteCodeDemo.java
>>> javap -c ByteCodeDemo.class
Compiled from "ByteCodeDemo.java"
public class ByteCodeDemo {
  public ByteCodeDemo();
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: return

  public static int absDifference(int, int);
    Code:
       0: iload_0
       1: iload_1
       2: isub
       3: istore_2
       4: iload_2
       5: invokestatic  #2                  // Method java/lang/Math.abs:(I)I
       8: ireturn

  public static void main(java.lang.String[]);
    Code:
       0: getstatic     #3                  // Field java/lang/System.out:Ljava/io/PrintStream;
       3: iconst_2
       4: iconst_1
       5: invokestatic  #4                  // Method absDifference:(II)I
       8: invokevirtual #5                  // Method java/io/PrintStream.println:(I)V
      11: return
}
```

Each bytecode has its `op_code` with either 0 or 1 argument. Each `op_code` is an unsigned byte type integer in the class file, occupying precisely one byte, which is how JVM instructions are called bytecodes.

> Note that the javap command translates the op_code into literal helper characters for human readability.

We can take a look at the bytecode of the `absDifference()` method. The number 0 preceding iload_0 represents the offset of this bytecode. The number 1 at the next line also illustrates the offset value of this bytecode. If we look at bytecode `invokestatic`, we can notice that this bytecode is different from the previous one as it has a parameter #2 and a length of 3 bytes.

So how does the interpreter work? The interpreter is in fact a [Stack Machine](https://en.wikipedia.org/wiki/Stack_machine) that executes bytecode in stack order according to the semantics of the bytecode. Take the above substraction for example. When interpreter executes a substraction, it will first push two operands into stack, in our case is 
`iload_0` and `iload_1`. Then it exeutes `isub` which will pop operand 0 and operand 1 out of stack and perform the substraction. Then it executes `istore_2` which pushes the substraction result into the stack.

### JIT


