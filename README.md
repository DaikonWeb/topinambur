# Topinambur

![Topinambur](./logo.svg)

Topinambur is a simple and minimal library to make HTTP calls in Kotlin with minimal effort. The main goals are:
* Use a really lightweight http library
* Use an object-oriented http library


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
implementation 'com.github.DaikonWeb:topinambur:1.6.0'
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
    <version>1.6.0</version>
</dependency>
```

## Getting Started
```
val response = "https://github.com/DaikonWeb".http.get()

println(response.statusCode)
println(response.body)
```

## Client Instance with a baseUrl
```
val http = Http("https://github.com")

val response = http.get("/DaikonWeb")

println(response.statusCode)
println(response.body)
```

## Enable request logging as Curl
```
HttpClient("https://github.com/DaikonWeb", System.out).get().body
```
