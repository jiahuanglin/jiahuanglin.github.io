---
title: Proxy design pattern and RPC framework
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-28 00:01:00 -0500
categories: [Software]
tags: [design pattern, server, Java, Go]
---

> The proxy pattern is a structural design pattern that allows you to provide a substitute for an object or its placeholder. Proxies control access to the original object and allow some processing before and after the request is submitted to the object.

## Static proxy
```java
public class ServerLoadBalancer {

    public Server find(Context context) {
        // return the least traffic server
    }

}

public class Server {

    public Response serve(Request request) {
        // ...
    }
}

public class ServerProxy extends Server {

    private ServerLoadBalancer loadBalancer;

    // ...
    
    public Response serve(Request request) {

        /* Do something before routing request to actual server
           The 'something' can be like network I/O, load balancing, service discovery etc.
        */
        Context context = ...
        Server server = loadBalancer.find(context);

        server.serve(request)
    }
}
```
We can tell the drawbacks of static proxy (code above): we need to reimplement all the methods in the original class in the proxy class. Say there are over 100 classes to be proxied, then we need to make over 100 corresponding proxy classes. In addition, the code in each proxy class is a bit like boilerplate code, which adds unnecessary code maintainance costs.

Apparently, a RPC framework cannot know the original class beforehand as the framework assumes no knowledge of the client(customer) class. So how can we solve this problem?

## Dynamic proxy
We can use dynamic proxies to solve this problem. Instead of writing a proxy class for each original class, we dynamically create a proxy class for the original class at runtime and then replace the original class with the proxy class in the framework.

Note that dynamic proxy relies on Java's reflection feature, namely the two core classes in the java.lang.reflect package: the `InvocationHandler interface` and the `Proxy class`.

```java
public interface InvocationHandler {

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable;

}

public class Proxy {
    // ...
    public static Object newProxyInstance(ClassLoader loader, Class<?>[] interfaces, InvocationHandler h) {

        Objects.requireNonNull(h);

        Class<?> caller = System.getSecurityManager() == null ? null : Reflection.getCallerClass();

        Constructor<?> cons = getProxyConstructor(caller, loader, interfaces);

        return newProxyInstance(caller, cons, h);
    }
}
```

Each dynamic proxy object must provide an implementation class of the `InvocationHandler interface`, with only one `invoke()` method. When using a proxy object to invoke a method, the proxy object will eventually forward the method call to the `invoke()` method to execute the specific logic.

The `Proxy` class is actually a factory class for dynamically creating proxy classes. It provides static method `newProxyInstance()` for dynamically generating the proxy classes.

A working example utilizing dynamic proxy will be like the following:
```java
public interface LoggingService {

    void emit(Event event);

}

public class LoggingServiceImpl implements LoggingService {

    @Override
    public void emit(Event event) {
        Timestamp timestamp = ...;
        System.out.println(timestamp.toString() + event.toString());
    }
}

public class LoggingServiceInvocationHandler implements InvocationHandler {

    private Object proxiedObject; 
    
    public LoggingServiceInvocationHandler(Object proxiedObject) { 
        this.proxiedObject = proxiedObject; 
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("start logging");
        Object result = method.invoke(proxiedObject, args); 
        System.out.println("emitted logging");
        return result;
    }
}

public class LoggingProxyFactory {

    private Object target;

    public LoggingProxyFactory(Object target) {
        this.target = target;
    }

    public Object getProxyInstance(Object proxiedObject) {
        LoggingServiceInvocationHandler handler = new LoggingServiceInvocationHandler(proxiedObject);
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                handler);
    }
}
```

## RPC Framework
```java
public class RpcProtocol<T> implements Serializable {
    private RpcHeader header;
    private T body;
}

public class RpcRequest implements Serializable {
    private String serviceVersion;
    private String className;
    private String methodName;
    private Object[] params;
    private Class<?>[] parameterTypes;
}

public class RpcResponse implements Serializable {
    private Object data;
    private String msg;
}

public class RpcFuture<T> {
    private Promise<T> promise;
    private long timeout;
}

public class RequestQueue {
    private Queue<RpcRequest> queue;

    public RequestQueue() {
        queue = new ConcurrentLinkedQueue<>();
    }

    public void hasRequest() {
        return queue.size() > 0;
    }

    public void addRequest() {
        // ...
    }

    public void popRequest() {
        // ...
    }
}

public class RpcExecutor implements Runnable {
    //...

    public void start(RequestQueue queue) {
        while (queue.hasRequest()) {
            // ...
        }

    }
}


public class RpcInvocationHandler implements InvocationHandler {

    private RequestQueue queue;
    private RpcExecutor executor;
    private String serviceVersion;
    private long timeout;
    
    public RpcInvocationHandler(String serviceVersion, long timeout) { 
        executor = new RpcExecutor();
        queue = new RequestQueue();
        this.serviceVersion = serviceVersion;
        this.timeout = timeout;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcProtocol<RpcRequest> protocol = new RpcProtocol<>();
        MsgHeader header = new MsgHeader();
        // header.setMagic(...);
        // ...
        protocol.setHeader(header);

        RpcRequest request = new RpcRequest();
        request.setServiceVersion(this.serviceVersion);
        request.setClassName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParams(args);
        protocol.setBody(request);

        RpcFuture<RpcResponse> future = new RpcFuture<>(new DefaultPromise<>(new DefaultEventLoop()), timeout);
        queue.addRequest(protocol);

        return future.getPromise().get(future.getTimeout(), TimeUnit.MILLISECONDS).getData();
    }
}

public class LoggingProxyFactory {

    private Object target;

    public LoggingProxyFactory(Object target) {
        this.target = target;
    }

    public Object getProxyInstance(Object proxiedObject) {
        LoggingServiceInvocationHandler handler = new LoggingServiceInvocationHandler(proxiedObject);
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                handler);
    }
}
```


```go
// server.go
type server interface {
    handleRequest(string, string) (int, string)
}

// proxy.go
type proxy struct {
    application       *application
    maxAllowedRequest int
    rateLimiter       map[string]int
}

func newProxyServer() *proxy {
    return &proxy{
        application:       &application{},
        maxAllowedRequest: 2,
        rateLimiter:       make(map[string]int),
    }
}

func (p *proxy) handleRequest(url, method string) (int, string) {
    allowed := n.checkRateLimiting(url)
    if !allowed {
        return 403, "Not Allowed"
    }
    return p.application.handleRequest(url, method)
}

func (p *proxy) checkRateLimiting(url string) bool {
    if p.rateLimiter[url] == 0 {
        p.rateLimiter[url] = 1
    }
    if p.rateLimiter[url] > p.maxAllowedRequest {
        return false
    }
    p.rateLimiter[url] = p.rateLimiter[url] + 1
    return true
}


// application.go
type application struct {
}

func (d *application) handleRequest(url, method string) (int, string) {
    if url == "/app/status" && method == "GET" {
        return 200, "Ok"
    }

    if url == "/create/user" && method == "POST" {
        return 201, "User Created"
    }
    return 404, "Not Found"
```