# Description
This reproducer illustrates how sending a large POST body (3MB worth of 9's) to a vert.x http server using vertx-web's BodyHandler and routes will cause the server to hang for a very long time (counted in minutes) during a call to `RoutingContext.getBodyAsJson()` in its POST handler.

This reproducer consists of 3 verticles: `Main`, `Client`, and `Service`.

The `Service` verticle contains the vertx-web route and http server that will attempt to retrieve the POST body using either `RoutingContext.getBodyAsJson()` or `RoutingContext.getBodyAsString()` to retrieve the POST body, depending on configuration. It fails to deploy only if it can't listen on the configured port.

The `Client` verticle creates a web client instant and issues the large POST body to the service, and fails to deploy if it fails to receive a response within 5 seconds, or if the response is not the expected `400`. Otherwise, it deploys successfully.

The `Main` verticle attempts to deploy the `Service` verticle, then the `Client` verticle, and fails to deploy only if either the `Client` or `Service` verticles fail to deploy.

The POST body used by the `Client` verticle is loaded from the file: `./src/main/resources/all-nines.txt`

Verticle port and `getBody*` policy configuration is loaded from the files:
`./src/main/resources/get-body-as-json.json`
`./src/main/resources/get-body-as-string.json`

The command line used to launch the application can be found in the `commandLine` property of the two `gradle Exec` tasks in `build.gradle`: `getBodyAsJson` and `getBodyAsString`.

# Running the Example
## GetBodyAsString
To run the example using `RoutingContext.getBodyAsString()`, use the following gradle task from the command line: `./gradlew getBodyAsString`. This will build the project if it hasn't been built already, then launch the `Service` verticle configured to retrieve the body using `getBodyAsString()`, resulting in the client receiving the expected `400` error response immediately.

Use `CTRL-C` to exit after seeing output similar to the following:
```
>>> service: starting verticle
>>> service: now listening on port 8081
>>> client: starting verticle
>>> client: loading all-nines.txt
>>> client: issuing POST to: http://localhost:8081/resources
>>> service: getting body as string
>>> service: failed getting body: io.vertx.core.json.DecodeException: Failed to decode:Cannot deserialize instance of `java.util.LinkedHashMap<java.lang.Object,java.lang.Object>` out of VALUE_NUMBER_INT token at [Source: (StringReader); line: 1, column: 1]
>>> client: received expected 400 response
>>> client: done issuing http requests
>>> service: done listening for http requests
INFO: Succeeded in deploying verticle
```

## GetBodyAsJson
To run the example using `RoutingContext.getBodyAsJson()`, use the following gradle task from the command line: `./gradlew getBodyAsJson`. This will build the project if it hasn't been built already, then launch the `Service` verticle configured to retrieve the body using `getBodyAsJson()`, resulting in the client complaining that it couldn't complete the request due to a response timeout, followed by a very long sequence of blocked thread warnings and exceptions from vertx core. During this time, no other incoming requests can be served.

Use `CTRL-C` to exit after seeing output similar to the following:
```
>>> service: starting verticle
>>> service: now listening on port 8081
>>> client: starting verticle
>>> client: loading all-nines.txt
>>> client: issuing POST to: http://localhost:8081/resources
>>> service: getting body as json
Aug 28, 2020 7:17:50 PM io.vertx.core.impl.BlockedThreadChecker
WARNING: Thread Thread[vert.x-eventloop-thread-1,5,main]=Thread[vert.x-eventloop-thread-1,5,main] has been blocked for 2
810 ms, time limit is 2000 ms
Aug 28, 2020 7:17:51 PM io.vertx.core.impl.BlockedThreadChecker
WARNING: Thread Thread[vert.x-eventloop-thread-1,5,main]=Thread[vert.x-eventloop-thread-1,5,main] has been blocked for 3
810 ms, time limit is 2000 ms
Aug 28, 2020 7:17:52 PM io.vertx.core.impl.BlockedThreadChecker
WARNING: Thread Thread[vert.x-eventloop-thread-1,5,main]=Thread[vert.x-eventloop-thread-1,5,main] has been blocked for 4
811 ms, time limit is 2000 ms
>>> client: couldn't issue POST request: io.vertx.core.http.impl.NoStackTraceTimeoutException: The timeout period of 500
0ms has been exceeded while executing POST /resources for server localhost:8081
Aug 28, 2020 7:17:53 PM io.vertx.core.impl.BlockedThreadChecker
WARNING: Thread Thread[vert.x-eventloop-thread-1,5,main]=Thread[vert.x-eventloop-thread-1,5,main] has been blocked for 5
812 ms, time limit is 2000 ms
io.vertx.core.VertxException: Thread blocked
        at java.math.BigInteger.<init>(BigInteger.java:473)
        at java.math.BigInteger.<init>(BigInteger.java:597)
        at com.fasterxml.jackson.core.base.ParserBase._parseSlowInt(ParserBase.java:856)
        at com.fasterxml.jackson.core.base.ParserBase._parseNumericValue(ParserBase.java:775)
        at com.fasterxml.jackson.core.base.ParserBase.getNumberValue(ParserBase.java:584)
        at com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer$Vanilla.deserialize(UntypedObjectDeseriali
zer.java:678)
        at com.fasterxml.jackson.databind.ObjectMapper._readValue(ObjectMapper.java:4173)
        at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:2467)
        at io.vertx.core.json.jackson.JacksonCodec.fromParser(JacksonCodec.java:97)
        at io.vertx.core.json.jackson.JacksonCodec.fromBuffer(JacksonCodec.java:67)
        at io.vertx.ext.web.codec.impl.BodyCodecImpl.lambda$static$1(BodyCodecImpl.java:41)
        at io.vertx.ext.web.codec.impl.BodyCodecImpl$$Lambda$48/1665919463.apply(Unknown Source)
        at io.vertx.ext.web.impl.RoutingContextImpl.getBodyAsJson(RoutingContextImpl.java:280)
        ...
```