---
title: Proxy design pattern and RPC framework
author:
  name: Jacob Lin
  link: https://github.com/jiahuanglin
date: 2021-12-19 00:01:00 -0500
categories: [Software]
tags: [design pattern, Java, Go]
---

> The proxy pattern is a structural design pattern that allows you to provide a substitute for an object or its placeholder. Proxies control access to the original object and allow some processing before and after the request is submitted to the object.

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