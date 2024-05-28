package com.github.olivergondza.saxeed;

import com.github.olivergondza.saxeed.ex.FailedTransforming;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamespaceTest {

    static final class CollectNamespaces implements UpdatingVisitor {
        private HashMap<String, String> visited;

        @Override
        public void startDocument() throws FailedTransforming {
            visited = new LinkedHashMap<>();
        }

        @Override
        public void startTag(Tag.Start tag) throws FailedTransforming {
            TagName name = tag.getName();
            String val = name.getQualifiedName() + "(" + name.getNsUri() + ")";
            visited.put(tag.toString(), val);
        }

        public HashMap<String, String> getVisited() {
            return visited;
        }
    }

    @Test
    void defaultNamespace() {
        CollectNamespaces cn = new CollectNamespaces();
        String input = "<r><d></d><n xmlns=\"namespace\"><nn></nn><dn xmlns=\"deep\"><ddn></ddn></dn></n><o xmlns=\"other\"></o></r>";
        String actual = Util.transform(input, cn);

        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("r>", "r()");
        expected.put("r>d>", "d()");
        expected.put("r>n>", "n(namespace)");
        expected.put("r>n>nn>", "nn(namespace)");
        expected.put("r>n>dn>", "dn(deep)");
        expected.put("r>n>dn>ddn>", "ddn(deep)");
        expected.put("r>o>", "o(other)");

        assertEquals(expected, cn.getVisited());
        assertEquals(input, actual);
    }

    @Test
    void namedNamespace() {

        CollectNamespaces cn = new CollectNamespaces();
        String input = "<r><d></d><N:n xmlns:N=\"namespace\"><N:nn></N:nn><D:dn xmlns:D=\"deep\"><D:ddn></D:ddn></D:dn></N:n><O:o xmlns:O=\"other\"></O:o></r>";
        String actual = Util.transform(input, cn);

        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("r>", "r()");
        expected.put("r>d>", "d()");
        expected.put("r>N:n>", "N:n(namespace)");
        expected.put("r>N:n>N:nn>", "N:nn(namespace)");
        expected.put("r>N:n>D:dn>", "D:dn(deep)");
        expected.put("r>N:n>D:dn>D:ddn>", "D:ddn(deep)");
        expected.put("r>O:o>", "O:o(other)");

        assertEquals(expected, cn.getVisited());
        assertEquals(input, actual);
    }

    @Test
    void normalizedNamespace() {
        CollectNamespaces cn = new CollectNamespaces();
        String input = "<r xmlns:N=\"namespace\" xmlns:D=\"deep\" xmlns:O=\"other\"><d></d><N:n><N:nn></N:nn><D:dn><D:ddn></D:ddn></D:dn></N:n><O:o></O:o></r>";
        String actual = Util.transform(input, cn);

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("r>", "r()");
        expected.put("r>d>", "d()");
        expected.put("r>N:n>", "N:n(namespace)");
        expected.put("r>N:n>N:nn>", "N:nn(namespace)");
        expected.put("r>N:n>D:dn>", "D:dn(deep)");
        expected.put("r>N:n>D:dn>D:ddn>", "D:ddn(deep)");
        expected.put("r>O:o>", "O:o(other)");

        assertEquals(expected, cn.getVisited());
        assertEquals(input, actual);
    }

    @Test
    void lookup() {
        Consumer<Tag.Start> verify = (Tag.Start test) -> {
            Tag n = test.getParent();
            Tag def = n.getParent();
            Tag no = def.getParent();

            assertTrue(test.isNamed("test"));
            assertNull(test.getAncestor("none"));
            assertEquals(no, test.getAncestor("no"));
            assertEquals(def, test.getAncestor("def"));
            assertEquals(n, test.getAncestor("named"));

            assertTrue(n.isNamed("named"));
            assertFalse(n.isNamed(TagName.noNs("named")));
            assertTrue(n.isNamed(TagName.withNs("n","named")));
            assertTrue(n.isNamed(TagName.withNs("n", "N", "named")));
            assertNull(n.getAncestor("none"));
            assertEquals(no, n.getAncestor("no"));
            assertEquals(def, n.getAncestor("def"));
            assertEquals(def, n.getAncestor(TagName.withNs("d", "def")));

            assertTrue(def.isNamed("def"));
            assertFalse(def.isNamed(TagName.noNs("def")));
            assertTrue(def.isNamed(TagName.withNs("d","def")));
            assertTrue(def.isNamed(TagName.withNs("d", "IGNORED", "def")));
            assertNull(def.getParent("none"));
            assertNull(def.getParent(TagName.noNs("none")));
            assertEquals(no, def.getParent("no"));
            assertEquals(no, def.getParent(TagName.noNs("no")));
            assertNull(def.getAncestor("none"));
            assertNull(def.getAncestor(TagName.noNs("none")));
            assertEquals(no, def.getAncestor("no"));
            assertEquals(no, def.getAncestor(TagName.noNs("no")));
            assertNull(def.getAncestor(TagName.withNs("foo", "no")));

            assertTrue(no.isNamed("no"));
            assertTrue(no.isNamed(TagName.noNs("no")));
            assertFalse(no.isNamed(TagName.withNs("nsuri","no")));
            assertFalse(no.isNamed(TagName.withNs("nsuri", "ns", "no")));

            assertNull(no.getParent());
            assertNull(no.getParent("x"));
            assertNull(no.getParent(TagName.withNs("uri", "prefix", "local")));
        };
        Util.transform("<no><def xmlns=\"d\"><N:named xmlns:N=\"n\"><test/></N:named></def></no>", verify, "test");
    }

    @Test
    void addChildToPlain() {
        String input = "<r/>";

        Consumer<Tag.Start> addNamedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "prefix", "local"))
                .declareNamespace("uri", "prefix")
        ;
        String actual = Util.transform(input, addNamedNs, "r");
        assertEquals("<r><prefix:local xmlns:prefix=\"uri\"></prefix:local></r>", actual);

        Consumer<Tag.Start> addDefaultNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"))
                .declareNamespace("uri", "");
        ;
        actual = Util.transform(input, addDefaultNs, "r");
        assertEquals("<r><local xmlns=\"uri\"></local></r>", actual);

        Consumer<Tag.Start> addNsToParent = (Tag.Start tag) -> {
            tag.declareNamespace("uri", "prefix");
            tag.addChild(new TagName("uri", "prefix", "local"));
        };
        actual = Util.transform(input, addNsToParent, "r");
        assertEquals("<r xmlns:prefix=\"uri\"><prefix:local></prefix:local></r>", actual);

        Consumer<Tag.Start> addUndefinedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"));
        FailedTransforming ft = assertThrows(FailedTransforming.class, () -> Util.transform(input, addUndefinedNs, "r"));
        assertEquals("Unable to write tag (TagName{local='local', uri='uri', ns=''}), no such namespace URI declared. Have: {}", ft.getMessage());
    }

    @Test
    void addChildToDefaultNs() {
        String input = "<r xmlns=\"def\"/>";

        Consumer<Tag.Start> addNoNs = (Tag.Start tag) -> tag.addChild("plain");
        String actual = Util.transform(input, addNoNs, "r");
        assertEquals("<r xmlns=\"def\"><plain></plain></r>", actual);

        Consumer<Tag.Start> addNamedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "prefix", "local"))
                .declareNamespace("uri", "prefix");
        ;
        actual = Util.transform(input, addNamedNs, "r");
        assertEquals("<r xmlns=\"def\"><prefix:local xmlns:prefix=\"uri\"></prefix:local></r>", actual);

        Consumer<Tag.Start> addDefaultNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"))
                .declareNamespace("uri", "");
        ;
        actual = Util.transform(input, addDefaultNs, "r");
        assertEquals("<r xmlns=\"def\"><local xmlns=\"uri\"></local></r>", actual);

        Consumer<Tag.Start> addNsToParent = (Tag.Start tag) -> {
            tag.declareNamespace("uri", "prefix");
            tag.addChild(new TagName("uri", "prefix", "local"));
        };
        actual = Util.transform(input, addNsToParent, "r");
        assertEquals("<r xmlns=\"def\" xmlns:prefix=\"uri\"><prefix:local></prefix:local></r>", actual);

        Consumer<Tag.Start> addUndefinedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"));
        FailedTransforming ft = assertThrows(FailedTransforming.class, () -> Util.transform(input, addUndefinedNs, "r"));
        assertEquals("Unable to write tag (TagName{local='local', uri='uri', ns=''}), no such namespace URI declared. Have: {def=}", ft.getMessage());
    }

    @Test
    void addChildToNamedNs() {
        String input = "<d:r xmlns:d=\"def\"/>";

        Consumer<Tag.Start> addInheritNs = (Tag.Start tag) -> tag.addChild("plain");
        String actual = Util.transform(input, addInheritNs, "r");
        assertEquals("<d:r xmlns:d=\"def\"><d:plain></d:plain></d:r>", actual);

        Consumer<Tag.Start> addNoNs = (Tag.Start tag) -> tag.addChild(new TagName("", "", "plain"));
        actual = Util.transform(input, addNoNs, "r");
        assertEquals("<d:r xmlns:d=\"def\"><plain></plain></d:r>", actual);

        Consumer<Tag.Start> addNamedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "prefix", "local"))
                .declareNamespace("uri", "prefix");
        ;
        actual = Util.transform(input, addNamedNs, "r");
        assertEquals("<d:r xmlns:d=\"def\"><prefix:local xmlns:prefix=\"uri\"></prefix:local></d:r>", actual);

        Consumer<Tag.Start> addDefaultNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"))
                .declareNamespace("uri", "");
        ;
        actual = Util.transform(input, addDefaultNs, "r");
        assertEquals("<d:r xmlns:d=\"def\"><local xmlns=\"uri\"></local></d:r>", actual);

        Consumer<Tag.Start> addNsToParent = (Tag.Start tag) -> {
            tag.declareNamespace("uri", "prefix");
            tag.addChild(new TagName("uri", "prefix", "local"));
        };
        actual = Util.transform(input, addNsToParent, "r");
        assertEquals("<d:r xmlns:d=\"def\" xmlns:prefix=\"uri\"><prefix:local></prefix:local></d:r>", actual);

        Consumer<Tag.Start> addUndefinedNs = (Tag.Start tag) -> tag.addChild(new TagName("uri", "", "local"));
        FailedTransforming ft = assertThrows(FailedTransforming.class, () -> Util.transform(input, addUndefinedNs, "r"));
        assertEquals("Unable to write tag (TagName{local='local', uri='uri', ns=''}), no such namespace URI declared. Have: {def=d}", ft.getMessage());
    }

    @Test
    void wrap() {
        String output = Util.transform(
                "<o><ch/></o>",
                tag -> tag.wrapWith(TagName.withNs("uri", "prefix", "r")).declareNamespace("uri", "prefix"),
                "ch"
        );
        assertEquals("<o><prefix:r xmlns:prefix=\"uri\"><ch></ch></prefix:r></o>", output);
    }

    @Test
    void wrapRoot() {
        String output = Util.transform(
                "<r xmlns:n=\"named\"/>",
                tag -> tag.wrapWith("o"),
                "r"
        );
        assertEquals("<o xmlns:n=\"named\"><r></r></o>", output);

        output = Util.transform(
                "<r xmlns:n=\"named\"/>",
                tag -> tag.wrapWith("o").declareNamespace("new-named", "nn"),
                "r"
        );
        assertEquals("<o xmlns:nn=\"new-named\" xmlns:n=\"named\"><r></r></o>", output);

        output = Util.transform(
                "<n:r xmlns:n=\"named\"/>",
                tag -> tag.wrapWith("o"),
                "r"
        );
        assertEquals("<o xmlns:n=\"named\"><n:r></n:r></o>", output);

        output = Util.transform(
                "<n:r xmlns:n=\"named\"/>",
                tag -> tag.wrapWith(TagName.withNs("named", "n", "o")),
                "r"
        );
        assertEquals("<n:o xmlns:n=\"named\"><n:r></n:r></n:o>", output);

        output = Util.transform(
                "<n:r xmlns:n=\"named\"/>",
                tag -> tag.wrapWith(TagName.withNs("other", "O", "o")).declareNamespace("other", "O"),
                "r"
        );
        assertEquals("<O:o xmlns:O=\"other\" xmlns:n=\"named\"><n:r></n:r></O:o>", output);
    }

    @Test
    void nsSubscribe() {
        CollectNamespaces no = new CollectNamespaces();
        CollectNamespaces def = new CollectNamespaces();
        CollectNamespaces namespaced = new CollectNamespaces();

        TransformationBuilder transformation = new TransformationBuilder()
                .add(Subscribed.to().noNamespace().build(), no)
                .add(Subscribed.to().defaultNamespace().build(), def)
                .add(Subscribed.to().namespaceUris("DEFAULT").build(), namespaced)
        ;
        new Saxeed()
                .setInputString("<plain><default xmlns=\"DEFAULT\"><A:a xmlns:A=\"AAA\"><d></d></A:a></default></plain>")
                .addTransformation(transformation)
                .transform()
        ;

        assertEquals(Map.of("plain>", "plain()"), no.visited);
        assertEquals(Map.of(
                "plain>", "plain()",
                "plain>default>", "default(DEFAULT)",
                "plain>default>A:a>d>", "d(DEFAULT)"
        ), def.visited);
        assertEquals(Map.of(
                "plain>default>", "default(DEFAULT)",
                "plain>default>A:a>d>", "d(DEFAULT)"
        ), namespaced.visited);
    }

    @Test
    void processRealistic() {
        Subscribed subs = Subscribed.to().namespaceUris("http://www.w3.org/2001/XMLSchema").build();

        class Visitor implements UpdatingVisitor {
            public final Set<String> qualifiedNames = new HashSet<>();

            @Override
            public void startTag(Tag.Start tag) throws FailedTransforming {
                qualifiedNames.add(tag.getName().getQualifiedName());
            }
        }
        Visitor visitor = new Visitor();
        TransformationBuilder tb = new TransformationBuilder().add(subs, visitor);
        new Saxeed()
                .setInput(Path.of("src/test/resources/schema.xsd"))
                .addTransformation(tb)
                .transform()
        ;

        assertEquals(Set.of(
                "xs:schema", "xs:element", "xs:complexType", "xs:sequence", "xs:attribute"
        ), visitor.qualifiedNames);
    }
}
