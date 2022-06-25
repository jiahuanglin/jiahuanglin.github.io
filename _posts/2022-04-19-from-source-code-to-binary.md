---
title: From source code to binary
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-04-19 22:14:00 -0500
categories: [System]
tags: [compiler]
---

What happens behind the scene when we compile the following code:

```c++
#include <iostream>
#include "demo.h"

int main() {
    std::cout << "Demo" << std::endl;
    return GREETING;
}

// demo.h
#define GREETING 0
```

## Preprocessing
Before compilation begins, the preprocessor processes macro definitions in the source file to expand or replace the source code. For example, for `#include <iostream>`, the preprocessor copies the iostream file directly into the source file. However, there is no iostream file in our source code directory, so how does the preprocessor know where to look for it?

The preprocessor doesn't know where all our header files are, and we need to provide the `Include Path` manually. In the case of g++, this is the -I option. The preprocessor will search through the paths we provide until it finds the header file. If we want to specify multiple lookup directories, we need to provide multiple -I options, and the preprocessor will look them up in order. For example, g++ -Iinclude1 -Iinclude2. For common system headers like iostream, it would be very tedious to specify the search directory manually, so g++ already has many common include paths built-in. By default, the include path will include the source files' directory.

The output of this step is the source code file after macro expansion. Not all languages have a preprocessing step like C/C++, like text replacement.

## Compiling
Once the source file has been preprocessed, the compiler can be called to compile the source file. In simple terms, each .cpp file can be referred to as a `compilation unit`. Each compilation unit can be compiled independently. The compiler's job is to parse the source file, go through a series of complex operations such as lexical analysis, syntax analysis, intermediate code generation, code optimization, etc., and finally generate an assembly file for the target platform.

The output is an assembly file, and the assembly language used varies from platform to platform. The most common one is the x86-64 platform.

## Assembling
Once we have the assembly file, we can use the assembler to assemble the assembly file into an object file (Object File). The assembler's job is much simpler. It only needs to translate the instructions in the assembly file, output the corresponding binary machine instructions, and assemble them into the operating system's object file format.

The output of this step is the binary object file (`ELF` file) for the corresponding platform, which contains the function symbols and the corresponding binary machine code.

> We can use the `readelf` command to view the file information of an `ELF` file.
```
$ readelf -a demo.o
ELF Header:
    Magic:   7f 45 4c 46 02 01 01 00 00 00 00 00 00 00 00 00
    Class:                             ELF64
    Data:                              2's complement, little endian
    Version:                           1 (current)
    OS/ABI:                            UNIX - System V
    ABI Version:                       0
    Type:                              REL (Relocatable file)
    Machine:                           Advanced Micro Devices X86-64
    Version:                           0x1
    Entry point address:               0x0
    ...

Section Headers:
    [Nr] Name              Type             Address           Offset
        Size              EntSize          Flags  Link  Info  Align
    [ 0]                   NULL             0000000000000000  00000000
        0000000000000000  0000000000000000           0     0     0
    [ 1] .text             PROGBITS         0000000000000000  00000040
        0000000000000091  0000000000000000  AX       0     0     1
    [ 2] .rela.text        RELA             0000000000000000  00000548
        0000000000000108  0000000000000018   I      12     1     8
    [ 3] .data             PROGBITS         0000000000000000  000000d1
    ...
```

## Linking
After getting the target file, we cannot execute the file yet. This is because target files often rely on external function implementations that are not themselves available. If we use the nm command to look at our target file, we will find many U's, meaning that these symbols cannot be defined in the current target file. Also, functions need to interact with the OS to execute the program, such as allocating memory, inputting and outputting, and so on. Our target file does not have all these. here is one last step to get the program running in the target file: linking.

Linking merges many target files and outputs an executable file.
 
```Bash
$ ld -static demo.o \
    /usr/lib/x86_64-linux-gnu/crt1.o \
    /usr/lib/x86_64-linux-gnu/crti.o \
    /usr/lib/gcc/x86_64-linux-gnu/9/crtbeginT.o \
    -L/usr/lib/gcc/x86_64-linux-gnu/9 \
    -L/usr/lib/x86_64-linux-gnu \
    -L/lib/x86_64-linux-gnu \
    -L/lib \
    -L/usr/lib \
    -lstdc++ \
    -lm \
    --start-group \
    -lgcc \
    -lgcc_eh \
    -lc \
    --end-group \
    /usr/lib/gcc/x86_64-linux-gnu/9/crtend.o \
    /usr/lib/x86_64-linux-gnu/crtn.o
$ ls
a.out  demo.cpp  demo.o
$ ./a.out
Demo
```

To call the `ld` command, pass in the target files we wish to link. Some system libraries are not stored as `.o` target files, but as `.a` files. An `.a` file is really just a bunch of `.o` files put together. The `.a` files are often named `lib***.a`. 

The linker, like the preprocessor, doesn't know where all the `.a` files are, so we need to specify the Library Path with the `-L` option. Together with the `-l` above, the linker will find the `.a` file and link it.

A question is how does the linker know where the program should start executing from? Actually the `main` function is not the real entry point of the program. By default, `ld` looks for the `_start` symbol, which is the program's starting point. This symbol is included in the system runtime library. The `_start` function in the library will eventually call our `main` function after initializing the runtime environment.

## Runtime
Loading a program is similar on different operating systems, which generally requires the following steps:

> 1. Calling the system function that creates the process
> 2. System validation parameters to open the specified program file
> 3. Parsing the program binary format
> 4. Performing the necessary initialization based on the information provided by the program
> 5. Create a structure in the kernel that holds the process information
> 6. Create the virtual memory map, run stack, etc. needed to run the program
> 7. Jump to the entry point specified in the program file and start running the program

The fourth step is more complex and may vary from program to program depending on the operating system. For example, if a program is dynamically linked, then the operating system is responsible for resolving the dynamic linking issue. For example, if we are running a program on Windows, Windows will handle it differently.

On Linux, the system call that loads a program into memory is `execve`, which reads our executable file, loads the code and data into memory, and then starts running our program from the entry address specified in the file. If all our programs were statically linked, the process would be as simple. However, if our program needs to use dynamic libraries, things are not so simple. When Linux detects that our program needs dynamic libraries, it will call the `ld.so` loader to load our program.