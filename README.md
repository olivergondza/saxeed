# Sufficiently Advanced XML Eventing Editor

Your user-friendly SAX wrapper to transform XML files easily, with memory consumption in mind.

## Spirit

Saxeed, a SAX wrapper, stream process XML input performing modifications to its outputs based on predefined transformation(s).

It accepts the constraints of "streaming" (or "eventing") approach — elements are visited one-by-one with no option to look ahead in the stream.
This is a tradeoff we accept in return for predictable memory footprint. 

## Usage

### Dependency

To consume the library in maven:

```xml
<dependency>
    <groupId>com.github.olivergondza</groupId>
    <artifactId>saxeed</artifactId>
    <version>...</version>
</dependency>
```

## Maintenance

### Contributing

Saxeed is an Open Source library, and we welcome contribution. File your Issue or an MR now!

### Releasing

The library is released to maven central.

To produce a new release, update version in pom.xml and run `mvn deploy`.
Note this is used for both releases and snapshots.
