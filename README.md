# Sufficiently Advanced XML Eventing EDitor

Your user-friendly, namespace aware tool to transform XML files easily, with memory consumption in mind.

[![Saxeed CI](https://github.com/olivergondza/saxeed/actions/workflows/ci.yaml/badge.svg)](https://github.com/olivergondza/saxeed/actions/workflows/ci.yaml)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.github.olivergondza/saxeed)](https://central.sonatype.com/artifact/com.github.olivergondza/saxeed)
[![javadoc](https://javadoc.io/badge2/com.github.olivergondza/saxeed/javadoc.svg)](https://javadoc.io/doc/com.github.olivergondza/saxeed)

## Spirit

Saxeed, a SAX wrapper, traverses XML input document performing predefined transformations, and writes the resulting XML document to declared output.

It accepts the constraints of "streaming" (or "eventing") approach — elements are visited one-by-one with no option to move around the stream.
This is a tradeoff we accept in return for predictable memory footprint.

The very nature of stream-based processing restricts the data that are available in every moment, and modifications that are permitted.
To accommodate that, developer needs to accept a paradigm shift compared to, say, dom4j.

Saxeed strives to add as much convenience on top of plain old SAX, while adding as little of an overhead.

### Capabilities

Each tag visitor can do the following: 

| operation / event                       | Tag Start              | Tag End                | Text Content |
|-----------------------------------------|------------------------|------------------------|--------------|
| Access Tag attributes                   | ☑                      | ☑                      | ☑            |
| Access Parent(s) Tag attributes         | ☑                      | ☑                      | ☑            |
| Add Child Tags/Text                     | ☑                      | ☑ (before closing tag) | ☑            |
| Add Sibling Tags/text (NOT IMPLEMENTED) | ☑ (before and after)   | ☑ (only after)         | ☑            |
| Add Parent Tag (`wrapWith()`)           | ☑                      | ☐                      | ☑            |
| Change Attributes                       | ☑                      | ☐                      | ☐            |
| Delete Tag (`unwrap()`)                 | ☑                      | ☐                      | n/a          |
| Delete Tag Recursively (`skip()`)       | ☑                      | ☐                      | n/a          |         
| Delete Child Tags (`empty()`)           | ☑                      | ☐                      | n/a          |

More complex changes can be implemented by subscribing visitors to multiple tags, and retaining information between their visits.  

## Usage

- [Basic Concepts](./docs/BASICS.md)
- [Implementing Visitors](./docs/VISITORS.md)

## Maintenance

### Contributing

Saxeed is an Open Source library, and we welcome contribution. File your Issue or an MR now!

### Releasing

The library is released to maven central.

To produce a new release, run `git tag X.Y.Z` and then `mvn deploy`.

To deploy `-SNAPSHOT`, run `mvn deploy` on a commit without tag.
