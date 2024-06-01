# Saxeed essential concepts

Saxeed passes through an input xml — in form of stream, or a file — and passes is through one or more *transformations*.
Each transformation prescribes what changes to perform to an XML document through a series of *visitors* that are *subscribed* to certain tag sets.
Also, it specifies a *target* location where the resulting XML document is written. 

```java
tb = new TransformationBuilder().add(Subscribed.to("entry"), entryVisitor);
new Saxeed()
    .setInput(srcFile)
    .addTransformation(tb, targetPath)
    .transform();
```

In this example, we stream through `srcFile` and have its content processed by a single transformation.
That transformation responds to all `<entry>` tags in that document invoking `entryVisitor` we provided.
The resulting XML document is then written to `targetPath`.

## Targets

Targets specifies where the resulting XML stream should be written.
It can be a `File` or `Path` instance to write it to a file system, or an `OutputStream` or `XMLStreamWriter` if more control is needed.

Saxeed always closes the targets that it had opened (files), and never closes targets opened by the client (streams or writers).
Likewise, it applies output buffering for target it controls, while for the client provided ones, this is a responsibility of the customer.

## Visitors

Visitor is a client-provided implementation of `UpdatingVisitor` that handles XML tag events.
The visitor methods are invoked when corresponding even is encountered in the input XML document, like `startTag(Tag)` or `endDocument()`.

Depending on a particular method invoked, the visitor can perform modifications on visited tags — the modified version will be sent to target.

See [Implementing Visitors](./VISITORS.md) for more details.

## Transformations

Transformation is a composition of visitors *subscribed* to certain tag sets.
A transformation with no visitors simply writes the input XML document to its target.
It will also be unchanged if the visitors perform no modifications, additions or removals to subscribed tags. 

Client can register any number of transformations, provided they output to a different targets.
Parallel transformations are executed independently on one another, but still during a single pass through the input XML document.

Each transformation can contain one or more visitors.
Each visitor can either be subscribed to all the tags in the document (`Subscribed.toAll()`), or just a set of selected ones.

## Subscriptions

Same as single visitor can be subscribed to multiple tag names, multiple visitors can be subscribed the same tag name.
Then, they are executed in the order of their addition for "opening events" and in reversed addition order for "closing events".
So for example on `</entry>` all the visitors subscribed to "entry" have their `endTag(Tag)` method called.

The subscription is declared by an instance of `Subscribed` functional interface.
There is a convenient builder to declare the tags of interest, based on namespace, local tag name, etc.
Customers can also provide a custom implementation if needed.
