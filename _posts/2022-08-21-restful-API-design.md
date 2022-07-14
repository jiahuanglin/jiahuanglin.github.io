---
title: RESTful API design
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2022-08-21 22:38:00 -0500
categories: [Software]
tags: [framework, frontend]
---

## RESTful API
The REST specification treats all content as a resource, meaning that everything on the network is a resource, and the REST architecture operates on resources by fetching, creating, modifying, and deleting, which correspond to the GET, POST, PUT, and DELETE methods provided by the HTTP protocol. 

While the REST style applies to many transport protocols, the HTTP protocol has become the standard for implementing RESTful APIs, as REST is inherently complementary to the HTTP protocol. Therefore, REST has the following core characteristics: 
> - Resource-centric, everything is abstracted as a resource, and all actions should be CRUD operations on resources. Resources correspond to objects in the object-oriented paradigm, and the object-oriented paradigm is object-centric. Resources are identified using URIs, and each resource instance has a unique URI identifier. For example, if we have a username admin, its URI identifier could be /users/admin. 
> - Resources are stateful. Use JSON/XML etc. to characterize the resource's state in the HTTP Body. 
> - The client operates on the server-side resource through four HTTP verbs to achieve "presentation-level state transformation." 
> - Stateless. Stateless means that each RESTful API request contains all the information needed to complete the operation, and the server does not need to maintain a session. Stateless is essential for resilient server-side scaling. 

### URI design
The following are some specifications that should be followed when designing URIs: 
- Use nouns instead of verbs for resource names, and use the noun plural. There are two types of resources: Collection and Member.
  - Collection: a collection of resources. For example, if we have many users in the system, the Collection of these users is the Collection. The URI of the Collection should be the domain/resource name plural, e.g. https://jiahuanglin.xyz/posts. 
  - Member: a single specific resource. For example, a user with a specific name in the system is a Member in the Collection. 
- The URI identifier for Member should be domain/resource name plural/resource name, e.g. https://jiahuanglin.xyz/posts/api-design.
- URIs should not end with /. URIs should not have an underscore _ in them. You must replace it with a middle bar -. 
- Avoid URIs that are too deep in the hierarchy; nesting resources with more than 2 layers can be messy, so it is recommended to convert other resources into ? parameters, for example: 
  ```
  /search/jiahuanglin/chrome/UTF-8 # Not recommended 

  /search?q=jiahuanglin&sourceid=chrome&ie=UTF-8 # Recommended
  ``` 

Here is a place to note: In the actual API development, you may find some operations can not be mapped well to a REST resource. We can do the following: 

- Treat the action as an attribute to a resource. For example, if you want to temporarily disable a user on the system, you can design a URI like this: 
  ```
  /users/jiahuanglin?active=false
  ```
- Treat the action as a nested resource, like the GitHub star action: 
  ```
  PUT /gists/:id/star    # github star action 
  DELETE /gists/:id/star # github unstar action
  ``` 
- If none of the above solves the problem, it is sometimes possible to break this type of specification. For example, if the login action is not part of any resource, the URI can be designed as: /login.

### Uniform paging / filtering / sorting / search function 
The REST resource query interface usually need to implement paging, filtering, sorting and search function, because every REST resource uses these functions, so can be implemented as a public API component. These functions are described below. 
- Paging: Paging should be provided when listing all Members in a Collection, e.g. /users?offset=0&limit=20 (limit, specifies the number of returned records; offset, specifies the starting position of returned records). Introducing paging can reduce the latency of API responses and avoid returning too many entries, leading to particularly slow server/client responses or even server/client crashes. 
- Filtering: If the user does not need all the state attributes of a resource, you can specify in the URI parameter which attributes to return, e.g. /users?fields=email,username,address. 
- Sorting: Users often list the top 100 Members in a collection based on creation time or other factors. 
- Search: When there are too many Members in a resource, users may search to find the required Member quickly, or to search if there is a certain type of resource with the name xxx, then you need to provide a search function. It is recommended to search by fuzzy match.

### Other than RESTful API
RPC (Remote Procedure Call) is a computer communication protocol. The protocol allows a program on one computer to call a subroutine on another without the programmer having to program this interaction. RPC shields the underlying network communication details, allowing developers to focus less on the details of network programming and more on implementing the business logic itself, thus improving development efficiency. 

The process of RPC invocation is as follows: 
1. Client calls Client Stub through a local call, 
2. Client Stub packages (also called Marshalling) the parameters into a message and sends the message. 
3. Server Stub unpacks the message (also called Unmarshalling) to get the parameters
4. Server Stub calls a subroutine (function) on the server side, processes it, and returns the final result to the Client in the reverse order. 

gRPC supports the definition of 4 types of service methods, namely Simple Mode, Server Side Data Flow Mode, Client Side Data Flow Mode, and Bidirectional Data Flow Mode. 
- Simple RPC: is the simplest gRPC mode. The client initiates a request and the server responds with a data. The defined format is rpc SayHello (HelloRequest) returns (HelloReply) {}. 
- Server-side streaming RPC mode: The client sends a request, the server returns a streaming response, and the client reads data from the stream until it is empty. The defined format is rpc SayHello (HelloRequest) returns (stream HelloReply) {}. 
- Client-side streaming RPC: The client sends a message to the server as a stream, and the server returns a response after all processing is complete. The defined format is rpc SayHello (stream HelloRequest) returns (HelloReply) {}. 
- Bidirectional streaming RPC mode (Bidirectional streaming RPC): both the client and the server can send data streams to each other, and this time the data from both sides can be sent to each other at the same time, which means that the principle of real-time interactive RPC framework can be realized. The definition format is rpc SayHello (stream HelloRequest) returns (stream HelloReply) {}.