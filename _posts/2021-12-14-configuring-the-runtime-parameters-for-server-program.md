---
title: Configuring the runtime parameters for server program
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-14 20:55:00 -0500
categories: [Software]
tags: [server]
---

Although we can pass argv through the main function, it is still tedious when dealing with server applications with lots of parameters that need to be specified flexibly. As a result, server programs typically support the following approaches for loading configurations in order to start:

  1. **Passing through cmd line**
  2. **Configuration of environment variables**
  3. **Configuration of files**
  4. **Pull from remote configuration center**
  5. **Hard-coded default values in the program**

Notice that different configuration approaches will have different priorities to allow flexible overrides. Usually, the command line parsing will have top priority, and environment variable configuration will come second over other approaches. The higher priority configuration approach overrides the lower priority configuration approach parameters with the same name.

**Command-line parsing** is commonly used during debugging to override particular settings without altering the configuration file quickly.

**Environment variables** are also used to override specific parameters but avoid lengthy command-line parameters. In addition, configuring the program through environment variables is a standard configuration method when deploying containerized services.

**File configuration** is usually used for stable program configuration after officially launching the program. One of the benefits of using files is that it is easy to view and modify. Also, the program can detect changes to the configuration file while running. The new configuration can take effect without restarting the program after modifying the configuration file, making the configuration changes have minimal impact on the online service.

In contrast to the file configuration, **pulling from a remote configuration centre** is a unified service responsible for issuing program settings in a distributed system and guaranteeing that every server program uses the same configuration. As with file configuration, the program can listen for changes to the configuration center or have the configuration center proactively issue new configurations, enabling configuration reloading.