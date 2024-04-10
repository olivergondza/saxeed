# Creating custom visitors

Visitors respond to all events related to the tags they have been subscribed to.

## Data access

Each visitor method receives information about the input XML document position through its arguments.
Namely, `Tag` implementations.

It provides access to the tag name, but more importantly tag attributes.

Also, the chain of parents is also available.
They are too implementations of `Tag` interface, so they can be looked up, and decisions can be made based on their state.

Note the tag ancestors (parent and its ancestors) can only access data, but not modify it, because they have been written already.

## Modifications

Currently visited tag can perform modification as described in the [Capabilities](../README.md#capabilities) section.

What modifications are possible differs between `startTag()` and `endTag()`, simply because by the time the closing tag is encountered, the opening tag have already been written.
So attribute modifications and tag deletion are reserved to tag start event.

Children addition, however, can be done it both `startTag()` and `endTag()`.
In the latter case, they will be added before the closing tag.

## Tag deletion

Tag can be deleted.
Or put differently, Saxeed let visitors decide that some tags will not be writen to the target.

By default, all tags are written as they are.
In `startTag()` (only), visitor can choose to delete the tag.

When the tag is deleted, its opening and closing tag will not be writen, and neither will be its text content.
Handling children deletion is configured as follows:

|                 | `skip()` | `unwrap()` | `empty()` | keep (the default) |
|-----------------|----------|------------|-----------|--------------------|
| Delete this tag | ☑        | ☑          | ☐         | ☐                  |
| Delete children | ☑        | ☐          | ☑         | ☐                  |


---

When the currently visited tag is deleted, all remaining visitors have their `startTag()` method called.
The `Tag#isOmitted()` will return `true` for them signaling the tag will not be part of the output.
The `endTag()` methods will not be called for a deleted tags at all.

When tag is deleted as a result of an ancestor calling `skip()` or `empty()`, no listener methods are called.

## `Tag` interfaces

In saxeed, current tag is represented by `Tag` interface.
It is the most restricted form, that only permit data access.

`Tag.End` specialization passed to `endTag()`, and `Tag.Start` passed to `startTag()` adds respective methods for extended capabilities present in given time of the input document traversal.
This is to provide compile-type guarantee, that operations used are permitted in any given time.
