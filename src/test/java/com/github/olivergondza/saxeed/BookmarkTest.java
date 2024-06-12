package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import com.github.olivergondza.saxeed.internal.CharChunk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookmarkTest {

    @Test
    void annotateChars() {
        final List<Bookmark> bookmarks = new ArrayList<>();
        class Screener implements UpdatingVisitor {

            @Override
            public void chars(Tag.Chars tag, CharChunk chars) {
                bookmarks.add(tag.bookmark());
            }
        }
        Screener screener = new Screener();

        class Updater implements  UpdatingVisitor {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                if (tag.isBookmarked(bookmarks)) {
                    tag.getAttributes().put("chars", "true");
                }
            }
        }
        Updater updater = new Updater();

        String input = "<r>!<a><aa></aa>!<aa></aa></a><b><bb></bb><bb>!</bb></b><c/></r>";
        Util.transform(input, screener);
        String actual = Util.transform(input, updater);

        String expected = "<r chars=\"true\">!<a chars=\"true\"><aa></aa>!<aa></aa></a><b><bb></bb><bb chars=\"true\">!</bb></b><c></c></r>";

        assertEquals(expected, actual);
    }

    @Test
    void generate() {
        final List<Bookmark> bookmarks = new ArrayList<>();
        class Generator implements UpdatingVisitor {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                if (tag.isNamed("e")) {
                    bookmarks.add(tag.wrapWith("w").bookmark());
                    bookmarks.add(tag.addChild("ch").bookmark());
                }
            }
        }
        Generator generator = new Generator();

        class Reverter implements  UpdatingVisitor {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                if (tag.isBookmarked(bookmarks)) {
                    tag.unwrap();
                }
            }
        }
        Reverter reverter = new Reverter();

        String input = "<e><e><e></e></e></e>";
        String actual = Util.transform(input, generator);

        assertEquals("<w><e><ch></ch><w><e><ch></ch><w><e><ch></ch></e></w></e></w></e></w>", actual);
        assertEquals(6, bookmarks.size());

        actual = Util.transform(actual, reverter);

        assertEquals(input, actual);
    }

    @Test
    void invalidateRemoved() {
        Map<TagName, Bookmark> t2b = new HashMap<>();
        class Screener implements UpdatingVisitor {

            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                t2b.put(tag.getName(), tag.bookmark());
                if (tag.isNamed("del")) {
                    tag.skip();
                }
            }

            @Override
            public void endDocument() throws FailedTransforming {
                verifyBookmarksInvalidated(t2b);
            }
        }
        Screener screener = new Screener();

        String first = "<r><del></del><keep></keep></r>";
        String second = Util.transform(first, screener);
        assertEquals("<r><keep></keep></r>", second);

        verifyBookmarksInvalidated(t2b);

        class User implements  UpdatingVisitor {
            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                Bookmark bookmarkForSameName = t2b.remove(tag.getName());
                assertFalse(bookmarkForSameName.isOmitted());
                assertTrue(tag.isBookmarked(bookmarkForSameName), bookmarkForSameName.toString());
            }
        }
        User user = new User();
        Util.transform(second, user);

        assertEquals(1, t2b.size());
        assertTrue(t2b.values().iterator().next().isOmitted());
    }

    private static void verifyBookmarksInvalidated(Map<TagName, Bookmark> t2b) {
        assertEquals(3, t2b.size());
        Map<String, Boolean> bookmarkValidity = t2b.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getLocal(),
                        e -> e.getValue().isOmitted()
                )
        );
        assertEquals(Map.of("r", false, "del", true, "keep", false), bookmarkValidity, t2b.toString());
    }
}
