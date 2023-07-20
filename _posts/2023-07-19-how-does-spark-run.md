---
title: How does Spark run?
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2023-07-19 23:57:00 -0500
categories: [System]
tags: [distributed system, Spark]
---

## Thrity thousand feet view

Running a Spark application essentially requires only two roles: a `Driver` and `Executors`:
-  The `Driver` is responsible for dividing the user's application into multiple jobs, further breaking them down into tasks, and submitting these tasks to the Executors for execution. 
-  The `Executors` are responsible for running these tasks and returning the results to the Driver program. 

Both the `Driver` and `Executors` do not concern themselves with where they are running; as long as they can start a Java process, get the `Driver` program and `Executors` running, and facilitate communication between the `Driver` and `Executors`, they can function (Note: The `Driver` and `Executors` do not each need to be an independent process). Therefore, various deployment modes are defined based on the different locations where the `Driver` and `Executors` can run:

- `Local execution` is typically used during development and testing. It involves running the driver and one executor process simultaneously in a local JVM process, enabling the local execution of Spark tasks.

- `Cluster execution` allows Spark to run within various types of clusters: Spark Standalone, YARN, Mesos, or Kubernetes. The essence of these implementations is to consider how to schedule the Spark Driver process and Executor processes within the cluster and facilitate communication between the Driver and Executors. Each Spark application will have one Driver and one or more Executors. When running in a cluster, multiple Executors will definitely run within the cluster. The Driver program, however, can run either within the cluster or outside of it, i.e., on the machine submitting the Spark tasks. When the Driver program runs within the cluster, it is referred to as cluster mode. When the Driver program runs outside the cluster, it is known as client mode.

![](https://spark.apache.org/docs/latest/img/cluster-overview.png)

Running Executors in different environments is achieved through different implementations of the `SchedulerBackend` interface. `SchedulerBackend` interacts with different cluster managers to implement resource scheduling in different clusters.


## Submit job to Spark
The command to submit tasks is called `spark-submit`:

```
bin/spark-submit 
  --deploy-mode cluster \
  --executor-memory 20G \
  --total-executor-cores 100 \
  --driver-memory 1G \
  --master yarn \
  --jars jar_name \
  --class com.waitingforcode.Main 
```

After executing the spark-submit script, the initialization of SparkContext occurs. This involves sending a registration message to the Master node and instructing the Worker to launch Executors. After launching, the Executors register back with the Driver process. The Driver then knows about all available Executors and can submit tasks to them during task execution.

The client performs the following tasks:

1. It initializes the yarnClient and starts the yarnClient.
2. It creates a client application and obtains the Application ID. 
3. It checks if the cluster resources meet the requested resources for the executor and ApplicationMaster. If the resources are insufficient, it throws an IllegalArgumentException.
4. It sets the resources and environment variables. 
   - This includes setting the Application's Staging directory, preparing local resources (like jar files and log files), setting environment variables within the Application, and creating the Context for launching the Container.
5. It sets the Context for submitting the Application
   - This includes specifying the application's name, queue, requested Container by the AM, and marking the job type as Spark.
6. It requests Memory and finally submits the Application to the ResourceManager through yarnClient.submitApplication.


The following code demonstrates the high-level flow of how the client submits the Application to the ResourceManager:

```Scala
// Create SparkSubmitArguments object with input arguments
appArgs = new SparkSubmitArguments(args)

// Submit the Spark application with the created arguments
submit(appArgs)

// Prepare the environment for submission
childArgs, childClasspath, sysProps, childMainClass = prepareSubmitEnvironment(args)

// Run the main function with the prepared environment
runMain(childArgs, childClasspath, sysProps, childMainClass, args.verbose)

// Set the class loader
loader = some_value

// Add jar file to classpath
addJarToClasspath()

// Set system property
System.setProperty(key, value)

// Get the main class
mainClass = Utils.classForName(childMainClass)

// Invoke the main method
mainMethod.invoke(null, childArgs.toArray)  // Essentially client.main

// Create ClientArguments object
args = new ClientArguments(argStrings)

// Create a new client and run it
new Client(args, sparkConf).run()

// Submit the application and get the application ID
this.appId = client.submitApplication()

// Set the container context
containerContext = some_value

// Set the application context
appContext = some_value

// Submit the application to the YARN client
yarnClient.submitApplication(appContext)
```

## What does YARN do
YARN does the following:
1. Run the "run" method of the ApplicationMaster.
2. Set up the necessary environment variables.
3. Create and start the ApplicationMasterClient.
4. Start a thread named "Driver" in the "startUserClass" function to launch the user-submitted Application, specifically the Driver. The `Driver` will initialize the `SparkContext`.
5. Wait for the SparkContext to initialize. If the wait count exceeds the configuration, the program will exit. Otherwise, initialize the yarnAllocator with SparkContext.
6. Once the SparkContext and Driver have initialized, register the ApplicationMaster with the ResourceManager via ApplicationMasterClient.
7. Allocate and start Executors. Before starting Executors, obtain "numExecutors" Containers from yarnAllocator, and then launch the Executors within the Containers.
8. Finally, the tasks will run within the `CoarseGrainedExecutorBackend`


```Scala
// Define ApplicationMaster object
object ApplicationMaster extends Logging {
  private var master: ApplicationMaster = _

  def main(args: Array[String]): Unit {
    SignalUtils.registerLogger(log)
    amArgs = new ApplicationMasterArguments(args)

    // Run as Spark user
    SparkHadoopUtil.get.runAsSparkUser { () =>
      master = new ApplicationMaster(amArgs, new YarnRMClient)
      System.exit(master.run())
    }
  }
}

// ApplicationMaster class
private[spark] class ApplicationMaster(args: ApplicationMasterArguments, client: YarnRMClient) extends Logging {
  private val sparkConf = new SparkConf()
  private val yarnConf: YarnConfiguration = SparkHadoopUtil.get.newConfiguration(sparkConf)
  private val isClusterMode = args.userClass != null

  final def run(): Int {
    appAttemptId = client.getAttemptId()
    var attemptID: Option[String] = None

    // Set system properties
    System.setProperty("spark.ui.port", "0")
    System.setProperty("spark.master", "yarn")
    System.setProperty("spark.submit.deployMode", "cluster")
    System.setProperty("spark.yarn.app.id", appAttemptId.getApplicationId().toString())
    attemptID = Option(appAttemptId.getAttemptId.toString)

    new CallerContext("APPMASTER", Option(appAttemptId.getApplicationId.toString), attemptID).setCurrentContext()
    logInfo("ApplicationAttemptId: " + appAttemptId)
    fs = FileSystem.get(yarnConf)  // Create HDFS file system
    runDriver(securityMgr)         // Execute Driver
    exitCode
  }

  // runDriver function
  private def runDriver(securityMgr: SecurityManager): Unit {
    addAmIpFilter()
    userClassThread = startUserApplication()  // Start user class thread

    logInfo("Waiting for spark context initialization...")
    sc = ThreadUtils.awaitResult(sparkContextPromise.future, Duration(totalWaitTime, TimeUnit.MILLISECONDS))
    rpcEnv = sc.env.rpcEnv
    driverRef = runAMEndpoint(sc.getConf.get("spark.driver.host"), sc.getConf.get("spark.driver.port"), isClusterMode = true)
    registerAM(sc.getConf, rpcEnv, driverRef, sc.ui.map(_.appUIAddress).getOrElse(""), securityMgr) // Register AM with RM, request resources from RM
    userClassThread.join()
  }

  private def startUserApplication(): Thread {
    logInfo("Starting the user application in a separate Thread")

    classpath = Client.getUserClasspath(sparkConf)
    urls = classpath.map { entry =>
      new URL("file:" + new File(entry.getPath()).getAbsolutePath())
    }
    userClassLoader = some_value
    userArgs = args.userArgs
    mainMethod = userClassLoader.loadClass(args.userClass).getMethod("main", classOf[Array[String]])

    // Define userThread
    userThread = new Thread {
      override def run() {
        mainMethod.invoke(null, userArgs.toArray) // Load user class main method with class loader, set thread name to "Driver"
        finish(FinalApplicationStatus.SUCCEEDED, ApplicationMaster.EXIT_SUCCESS)
      }
    }
    userThread.setContextClassLoader(userClassLoader)
    userThread.setName("Driver")
    userThread.start()
    return userThread
  }

  // registerApplicationMaster function
  private def registerApplicationMaster(_sparkConf: SparkConf, _rpcEnv: RpcEnv, driverRef: RpcEndpointRef, uiAddress: String, securityMgr: SecurityManager) {
    appId = client.getAttemptId().getApplicationId().toString()
    attemptId = client.getAttemptId().getAttemptId().toString()

    driverUrl = RpcEndpointAddress(_sparkConf.get("spark.driver.host"), _sparkConf.get("spark.driver.port").toInt, CoarseGrainedSchedulerBackend.ENDPOINT_NAME).toString

    allocator = client.register(driverUrl, driverRef, yarnConf, _sparkConf, uiAddress, historyAddress, securityMgr, localResources)
    allocator.allocateResources()   // Request RM to allocate resources for AM
    reporterThread = launchReporterThread()
  }
}
```
One can see that from `Driver` to `Executor` is a RPC call.

## DAGScheduler and RDDs

### This helps with the following reading: 
> `RDD` = `stage` = `task`, where `RDD` ultimately becomes `tasks`


We know that in Spark, operations are lazy and are only executed when an action operation is encountered (e.g., collect()). Let's take the following operation as an example:

```
dataRDD.flatMap(_.split(" ")).map((_,1)).reduceByKey(_ + _).collect()
```

Each action operation triggers the `runJob` function of SparkContext, which initiates a session of distributed scheduling. The main purpose of SparkContext.runJob is to call the runJob function of `DAGScheduler`.

When a job is submitted, it triggers the creation of stages by the `DAGScheduler`. After the stages are created, the DAGScheduler submits the ResultStage by calling submitStage. It is important to note that in submitStage, the DAGScheduler first checks if the parent stages on which the pending stage depends have completed execution. If not, it recursively requests the execution of all pending parent stages. For the current stage to be executed, submitMissingTasks is called to request task scheduling. submitMissingTasks performs the following four operations:

1. Calculates the location preference for each missing task.
2. Creates ShuffleMapTask and ResultTask based on the type of stage.
3. Creates a TaskSet (created by DAGScheduler) and a TaskSetManager (created by TaskScheduler).
4. Submits the newly created TaskSet by calling the submitTasks method of the TaskScheduler.

For each divided stage, DAGScheduler creates a corresponding collection of tasks. DAGScheduler submits task scheduling requests to the TaskScheduler at the granularity of TaskSet.

Overall it's the following process: 
1. Actions operator triggers SparkContext.runJob, which leads to DAGScheduler.runJob
2. DAGScheduler.submitJob 
3. EventProcessLoop/JobSubmitted event
4. DAGScheduler.handleJobSubmitted creates all stages 
5. DAGScheduler.submitStage 
6. DAGScheduler.submitMissingTasks creates TaskSet 
7. TaskScheduler.submitTasks
8. TaskSetManager is created for TaskSet and added to the task queue
9. SchedulerBackend requests resources, obtains Worker Offers
10. TaskDescriptions are computed and sent to the SchedulerBackend, which distributes the tasks to Executors.

```Scala
// RDD class
abstract class RDD[T: ClassTag](_sc: SparkContext, deps: Seq[Dependency[_]]) extends Serializable with Logging {
  def collect(): Array[T] = withScope {
    results = runJob(this, convert iterator to array)
    return concatenate(results)
  }
}

// SparkContext class
class SparkContext(config: SparkConf) extends Logging {
  def runJob(rdd, func, partitions, resultHandler) {
    callSite = getCallSite
    cleanedFunc = clean(func)
    logInfo("Starting job: " + callSite)
    runJob(rdd, cleanedFunc, partitions, callSite, resultHandler, getLocalProperties)
    finishAll(progressBar)
    checkpoint(rdd)
  }
}

// DAGScheduler class
private[spark] class DAGScheduler(sc: SparkContext, taskScheduler: TaskScheduler, env: SparkEnv, ...) extends Logging {
  // Define runJob function
  def runJob(rdd, func, partitions, callSite, resultHandler, properties) {
    start = getCurrentTime
    waiter = submitJob(rdd, func, partitions, callSite, resultHandler, properties)
    awaitPermission = null
    waitFor(waiter.completionFuture, infiniteDuration)
    switch(waiter.completionFuture.value) {
      case Success:
        logInfo("Job finished: " + callSite + ", took " + (getCurrentTime - start))
      case Failure:
        logInfo("Job failed: " + callSite + ", took " + (getCurrentTime - start))
        ...
    }
  }

  // submitJob function
  def submitJob(rdd, func, partitions, callSite, resultHandler, properties) {
    jobId = incrementJobId
    func2 = cast func to new type
    waiter = new JobWaiter(this, jobId, size(partitions), resultHandler)
    postEvent(JobSubmitted(jobId, rdd, func2, toArray(partitions), callSite, waiter, clone(properties))) // Send a job submission event
    return waiter
  }

  // handleJobSubmitted function
  private[scheduler] def handleJobSubmitted(jobId, finalRDD, func, partitions, callSite, listener, properties) {  
    finalStage = createResultStage(finalRDD, func, partitions, jobId, callSite) // Create final stage first
    job = new ActiveJob(jobId, finalStage, callSite, listener, properties)
    clearCacheLocs()

    jobSubmissionTime = getCurrentTime
    jobIdToActiveJob(jobId) = job
    addJob(activeJobs, job)
    setActiveJob(finalStage, job)
    stageIds = toArray(jobIdToStageIds(jobId))
    stageInfos = map(stageIds, id => getLatestInfo(stageIdToStage(id)))
    postEvent(SparkListenerJobStart(jobId, jobSubmissionTime, stageInfos, properties))
    submitStage(finalStage)
  }

  // submitStage function
  private def submitStage(stage) {
    jobId = getActiveJobForStage(stage)
    if (!isWaiting(stage) && !isRunning(stage) && !isFailed(stage)) {
      missing = sort(getMissingParentStages(stage))
      if (isEmpty(missing)) {
        submitMissingTasks(stage, jobId)
      } else {
        for each parent in missing {
          submitStage(parent)
        }
        addStage(waitingStages, stage)
      }
    }
  }
}
```
