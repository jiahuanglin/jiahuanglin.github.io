---
title: How does browser render a page
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-05-12 10:23:00 -0500
categories: [System]
tags: [browser, frontend]
---

## HTML, CSS and JavaScript
**HTML is made up of tags and text. Each tag has its semantic meaning, and the browser will display the HTML content correctly according to the semantic meaning of the tag.**

```HTML
<p> Hello World</p>
```

For example, the above tag is telling the browser that the content here needs to create a new paragraph, and the text in the middle is what needs to be displayed in the section. Suppose you need to change the font colour, size, and other information in the HTML. **In that case, you need to use CSS, which is also known as a cascading style sheet and consists of selectors and properties, such as the p selector in the code, which selects the contents of the HTML tag, and then applies the selector's property values to the content of the tag.** There is a colour attribute inside the selector, and its value is red, which tells the rendering engine to display the tag's contents as red. 

**As for `JavaScript`, you can use it to make the content of a web page "move" or "dynamically changing".** For example, you can use JavaScript to modify the CSS style value to change the text color.

## Rendering pipeline
Because of the complexity of the rendering mechanism, the rendering module is divided into many sub-stages during its execution, and the input HTML passes through these sub-stages before the final output pixels. We call such a processing flow a `rendering pipeline`.

The pipeline can be divided into sub-stages according to the rendering chronology: 
> 1. building DOM tree 
> 2. style calculation 
> 3. layout stage
> 4. layering
> 5. drawing 
> 6. chunking 
> 7. rasterization and compositing


### Building a DOM tree 
Why do we build a DOM tree? This is because browsers cannot understand and use HTML directly, so they need to convert HTML into a structure that browsers can understand - a DOM tree.

Generating the DOM tree is just a first step, up to now we still don't know the style of the DOM nodes. Making the DOM nodes have the correct style requires style calculation. 

### Styling && layout
The purpose of recalculate style is to calculate the specific style of each element in the DOM node. This stage can be divided into three steps to complete. 
  1. Convert CSS into a structure that the browser can understand. 
      - Like HTML files, browsers cannot understand these plain text CSS styles directly, so when the rendering engine receives the CSS text, it performs a conversion operation that converts the CSS text into a structure that the browser can understand - styleSheets. The rendering engine will get all the CSS text into the data in the styleSheets structure, and the structure is easy to query and modify, which will provide the basis for the later style operations.
  2. Converting and normalizing property values in a stylesheet 
  3. Calculate the specific style of each node in the DOM tree
    - Now that the style properties have been standardized, the next step is calculating each node's style properties in the DOM tree, which involves CSS inheritance rules and cascading rules. The first is CSS inheritance, which means that each DOM node contains the style of its parent node. This may be a bit abstract, so let's combine it with a concrete example of how a style sheet like the one below is applied to a DOM node.
  4. Calculating the geometric positions of the visible elements in the DOM tree

### Layering && drawing
Now that we have the layout tree and the specific location information for each element is calculated, is the next step to start drawing the page? The answer is still no. Because there are many complex effects on the page, such as some complex 3D transformations, page scrolling, or z-indexing to do z-axis sorting, etc. To better to achieve these effects, the rendering engine also needs to generate special layers for specific nodes and a corresponding LayerTree. 

The rendering engine gives the page several layers stacked together in a specific order to form the final page. After the layer tree is built, the rendering engine draws each layer in the layer tree.


### Rasterization && compositing
When the drawing list of layers is ready, the main thread will submit the drawing list to the compositing thread. Then we have to see what the viewport is. 

> Ssually a page can be large, but the user can only see a part of it. A viewpoint is the part that user can see.

In some cases, some layers can be very large. For example, some pages take a long time to scroll to the bottom using the scrollbar, but through the viewport, the user can only see a small part of the page, so in this case, it would be too much overhead to draw all the layers, and it is not necessary. For this reason, the composition thread divides the layers into blocks (tiles), and the composition thread prioritizes the generation of bitmaps according to the blocks near the viewport, and the actual generation of bitmaps is performed by rasterization. 

> Rasterization is the conversion of blocks to bitmaps. 

The block is the smallest unit of rasterization execution. The rendering process maintains a rasterization thread pool. All block rasterization is performed within the thread pool. If the rasterization operation uses the GPU, the final bitmap generation is done in the GPU, which involves cross-process operations. The rendering process sends the instructions for generating the block to the GPU, and then the bitmap of the generated block is executed in the GPU and saved in the GPU's memory.

## Chrome
Before Chrome came out, all browsers on the market were single-process. (I think)

### Single process browser
A single-process browser is one in which all the functional modules of the browser run in the same process, including the network, plug-ins, JavaScript runtime environment, rendering engine, pages, and so on. Such architecture design has a lot of drawbacks.

1. **Unstable**
   - Early browsers needed plug-ins to implement various powerful features such as Web video, Web games, etc. However, plug-ins were the most problematic modules and also ran in the browser process, so an unexpected plug-in crash could cause the entire browser to crash. In addition to plugins, the rendering engine module is also unstable, and often some complex JavaScript code may cause the rendering engine module to crash. As with plugins, a rendering engine crash can also cause the entire browser to crash. 

2. **Not smooth**
   - By single-process design, the rendering module, JavaScript execution environment, and plug-ins of all pages run in the same thread, which means that only one module can be executed at the same time. Consider an infinite loop script running on that single thread, because this script is infinite loop, when it executes, it monopolizes the entire thread, which causes other modules running in that thread to not have a chance to be executed. Since all the pages in the browser are running in that thread, none of those pages have a chance to execute the task, which causes the whole browser to become unresponsive and laggy. In addition to the above script or plug-in will make the single-process browser lagging, the page memory leak is also a major cause of the single-process slowdown. Usually the kernel of the browser is very complex, running a more complex page and then close the page, there will be memory can not be fully recycled, which leads to the problem that the longer you use, the higher the memory occupation, the slower the browser will become. 
3. **Insecurity** 
   - When you run a plug-in on a page, it means that the plug-in can fully operate your computer. If it is a malicious plugin, then it can release viruses, steal your account passwords and cause security problems. As for the page script, it can obtain system privileges through the browser's vulnerability. These scripts can also do something malicious to your computer after obtaining system privileges, which can also cause security problems. 

### Multi-process browser

The following diagram shows the process architecture of Chrome when it was released in 2008.

![Chrome V8 Architech](/assets/img/posts/how-does-browser-render-a-page/Chrome-architecture.png)

As you can see from the diagram, Chrome pages run in a separate rendering process. At the same time, the plug-ins in the pages also run in a separate plug-in process, and the processes communicate with each other through the IPC mechanism (as shown in the dashed part of the diagram). So how does Chrome solves the above problems? 
1. Since the processes are isolated from each other, a page or plug-in crashes only affects the current page or plug-in process. It does not affect the browser and other pages, which perfectly solves the problem that a page or plug-in crash can cause the whole browser to crash, that is, the problem of instability.
2. Now again, JavaScript runs in the rendering process, so even if JavaScript blocks the rendering process, it only affects the rendered page, not the browser or other pages, because the scripts on other pages run in their rendering process. So when we run the above dead-loop script in Chrome again, only the current page doesn't respond. The solution to the memory leak is even simpler, because when a page is closed, the entire rendering process is also closed, and the memory occupied by that process is then reclaimed by the system, which easily solves the memory leak problem for browser pages. 
3. Lastly, the benefit of using a multi-process architecture is the ability to use a security sandbox, which you can think of as the operating system putting a lock on the process, so that programs inside the sandbox can run but cannot write any data to your hard drive or read any data in sensitive locations, such as your documents and desktop. Chrome locks the plug-in and rendering processes in a sandbox, so that even if a malicious program is executed in the rendering or plug-in process, the program cannot break out of the sandbox and gain access to the system.

In fact, the latest Chrome browser includes:
> 1. The main Browser process.
> 2. A GPU process.
> 3. A NetWork process.
> 4. Multiple rendering processes.
> 5. Multiple plug-in processes.
   
Let's break down the functions of each of these processes. 
- **Browser process**: Mainly responsible for interface display, user interaction, sub-process management, and also provides storage and other functions.
- **Rendering process**: The core task is to convert HTML, CSS, and JavaScript into web pages that users can interact with, and both the layout engine Blink and the JavaScript engine V8 run in this process. For security reasons, the rendering processes are run in sandbox mode. When Chrome was first released, there were no GPU processes. The GPU was originally intended for 3D CSS effects, but web pages and Chrome's UI interface chose to draw with the GPU, making it a common browser requirement. Finally, Chrome also introduced GPU processes to its multi-process architecture. 

- **The web process**: It was previously run as a module within the browser process, but only recently has it been separated into a separate process.

- **Plug-in process**: It is mainly responsible for the running of plug-ins. Since plug-ins are prone to crash, they need to be isolated by the plug-in process to ensure that the crash will not affect the browser and the page. 

To sum up, opening a page requires at least one network process, one browser process, one GPU process and one rendering process, 4 in total. 

Although the multi-process model improves browser stability, fluency and security, it also inevitably brings some problems: higher resource usage. Because each process contains a copy of the shared infrastructure (such as the JavaScript runtime environment), the browser consumes more memory resources. More complex architecture. Problems such as high coupling between browser modules and poor scalability can cause the current architecture to be challenging to adapt to new needs.
