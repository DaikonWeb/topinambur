# Topinambur

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
implementation 'com.github.DaikonWeb:topinambur:0.0.2'
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
    <version>0.0.2</version>
</dependency>
```

## Getting Started
```
"https://github.com/DaikonWeb".get()
```
