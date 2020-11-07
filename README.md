# vmstat-sse

This is a toy Netty project demonstrating how to push Server Sent Events from an SSL enabled HTTP/2 webserver. 

The project uses the Application Layer Protocol Negotiation (ALPN) TLS extension so if you are using JDK 8, you'll need to follow these [ALPN-OpenJDK8 instructions](https://www.eclipse.org/jetty/documentation/current/alpn-chapter.html#alpn-openjdk8) run it.

### Building

Build the project with Gradle:

```bash
$ ./gradlew build
```

and run it locally:

```bash
$ ./gradlew run
```

### Structure

The three separate concerns have been split into modules

* **web** 
    * a general use secure HTTP web server
    * a very simple router to map handlers to paths 
* **stats**
    * all things `vmstat`
* **app**
    * bootstrapping the main application
    * index and sse request handlers
