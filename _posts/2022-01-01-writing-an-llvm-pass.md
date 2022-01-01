---
title: Writing an LLVM pass
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-01-01 04:01:00 -0500
categories: [Sytem]
tags: [compiler, LLVM]
---

## Basic Pass

```c++
#include "llvm/ADT/Statistic.h"
#include "llvm/Pass.h"
#include "llvm/IR/Function.h"
#include "llvm/Support/raw_ostream.h"
using namespace llvm;

STATISTIC(MyCounter, "Counts number of functions greeted");

namespace {
  struct MyPass : public FunctionPass {
    static char ID;
    MyPass() : FunctionPass(ID) {}

    virtual bool runOnFunction(Function &F) {
      ++MyCounter;
      errs() << "A function called " << F.getName() << "!\n";
      return false;
    }
  };
}

char MyPass::ID = 0;

// Register the pass so `opt -skeleton` runs it.
static RegisterPass<MyPass> X("mypass", "a useless pass");
```

## Vectorization Pass
```c++
#include "llvm/Analysis/LoopInfo.h"
#include "llvm/Analysis/LoopPass.h"
#include "llvm/IR/Function.h"
#include "llvm/IR/IRBuilder.h"
#include "llvm/Pass.h"
#include "llvm/Support/raw_ostream.h"
#include "llvm/Transforms/IPO/PassManagerBuilder.h"

using namespace llvm;

namespace {
  struct VectorizationPass : public LoopPass {
    static char ID;
    VectorizationPass() : LoopPass(ID) {}

    virtual bool runOnLoop(Loop* L, LPPassManager &LPM) {
      
    }
  }
}
```





### Reference
[Writing An LLVM Pass](https://llvm.org/docs/WritingAnLLVMPass.html)\
[LLVM Hello Word Pass](https://github.com/llvm/llvm-project/blob/main/llvm/lib/Transforms/Hello/Hello.cpp)