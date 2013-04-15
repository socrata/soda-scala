# Soda-Scala

Scala bindings for the SODA2 API.

## Getting it

Soda-Scala is published to maven central.  There are two artifacts,
`soda-publisher-scala` which contains everything, and
`soda-consumer-scala` which contains only those features required for
read-access.  SBT configuration can be done by adding

```scala
libraryDependencies += "com.socrata" %% "soda-publisher-scala" % "1.0.0"
```

to your `.sbt` file, while for Maven, the pom snippet is:

```xml
<dependencies>
  <dependency>
    <groupId>com.socrata</groupId>
    <artifactId>soda-publisher-scala_${scala.version}</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

Soda-scala is published for Scala versions 2.8.1, 2.8.2, 2.9.0,
2.9.0-1, 2.9.1, 2.9.1-1, and 2.9.2.

## Sample code

The soda-scala-sample subproject contains sample code for reading and
writing.

## Interop with databinder-dispatch

Version 0.9 and above of
[databinder-dispatch](https://github.com/dispatch/reboot/) uses
[async-http-client](https://github.com/sonatype/async-http-client) as
its underlying engine.  The default client can be retrieved using
`Http.client` and passed to the soda-scala `HttpConsumer` or
`HttpProducer`.

## Future work

* A query-building system richer than raw SoQL strings.
* More ways to upload data (e.g., from a CSV file).
