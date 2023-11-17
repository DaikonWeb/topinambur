# Topinambur

![Topinambur](./logo.svg)

Topinambur is a simple and minimal library to make HTTP calls in Kotlin with minimal effort. The main goals are:
* Lightweight
* Easy to use
* No boilerplate code through named parameters and defaults
* Easily extended


## How to add Topinambur to your project
[![](https://jitpack.io/v/daikonweb/topinambur.svg)](https://jitpack.io/#daikonweb/topinambur)

### Gradle
- Add JitPack in your root build.gradle at the end of repositories:
```
repositories {
    ...
    maven { url 'https://jitpack.io' }
}
```

- Add the dependency
```
implementation 'com.github.DaikonWeb:topinambur:1.14.1'
```

### Maven
- Add the JitPack repository to your build file
```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
- Add the dependency
```
<dependency>
    <groupId>com.github.DaikonWeb</groupId>
    <artifactId>topinambur</artifactId>
    <version>1.14.1</version>
</dependency>
```

## Getting Started
```kotlin
import topinambur.Http.Companion.HTTP

val response = HTTP.get("https://github.com/DaikonWeb")

println(response.statusCode)
println(response.body)
```

## Client Instance with a baseUrl
```kotlin
import topinambur.Http

val http = Http("https://github.com")

val response = http.get("/DaikonWeb")

println(response.statusCode)
println(response.body)
```

## Methods available
You can also pass the parameters as named parameters to enhance readability.

```kotlin
val http = Http(baseUrl = "http://localhost:8080")

http.head(url = "/")
http.get(url = "/")
http.options(url = "/")

http.post(url = "/")
http.post(url = "/", body = Multipart())
http.post(url = "/", body = mapOf())
http.post(url = "/", body = "")
http.post(url = "/", body = "".toByteArray())

http.put(url = "/")
http.put(url = "/", body = mapOf())
http.put(url = "/", body = "")
http.put(url = "/", body = "".toByteArray())

http.delete(url = "/")
http.delete(url = "/", body = mapOf())
http.delete(url = "/", body = "")
http.delete(url = "/", body = "".toByteArray())

http.call(url = "/", method = "GET")
```

All these methods have some other optional parameters:
```kotlin
HTTP.get(
    url = "/",
    params = mapOf("query" to "string"),
    headers = mapOf("Content-Type" to "application/json"),
    auth = Bearer("token"),
    followRedirects = true,
    timeoutMillis = 30000
)
```

Also the `Http` constructor have some other optional parameters
that are intended as defaults for every request you will make with that instance:
```kotlin
Http(
    baseUrl = "http://localhost:8080",
    headers = mapOf("Content-Type" to "application/json"),
    auth = Bearer("token"),
    followRedirects = true,
    timeoutMillis = 30000,
    printer = null
)
```

## Enable request logging
To enable it you only need to pass a printer to the `Http` instance.
The only printer available is `CurlPrinter`, but you can implement your own `Printer`.

```kotlin
import topinambur.Http

val curlPrinter = CurlPrinter(System.out)
Http(printer = curlPrinter).get("http://localhost:8080")
```

## Multipart data post request
You can also make a post request to send a multipart data file along with some fields.

```kotlin
import topinambur.Http.Companion.HTTP

HTTP.post(
    url = "http://localhost:8080/",
    body = Multipart(
        FilePart(field = "file", name = "a.txt", type = "plain/text", content = byteArrayOf(112, 124)),
        FieldPart(field = "field", value = "value")
    )
)
```


## Autorize a request
You can also make an authorized request with base and bearer authentication.

You can also implement your own auth method implementing the `AuthorizationStrategy` interface.

**No auth** (it is the default behaviour)
```kotlin
import topinambur.Http.Companion.HTTP

HTTP.get(
    url = "http://localhost:8080/",
    auth = None()
)
```

**Basic auth**
```kotlin
import topinambur.Http.Companion.HTTP

        HTTP.get(
            url = "http://localhost:8080/",
            auth = Basic("usr", "pwd")
        )
```

**Bearer auth**
```kotlin
import topinambur.Http.Companion.HTTP

HTTP.get(
    url = "http://localhost:8080/",
    auth = Bearer("token"),
)
```