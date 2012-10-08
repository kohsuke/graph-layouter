package org.kohsuke.graph_layouter.impl;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProperTransformerTest extends TestCase {
    /**
     * <pre>
     * a -+-> b ---> c -+-> d
     *    |             |
     *    +-------------+
     * </pre>
     */
    public void test1() throws Exception {
        Graph<String> g = new Graph<String>();
        Vertex a = g.makeVertex("a");
        Vertex b = g.makeVertex("b");
        Vertex c = g.makeVertex("c");
        Vertex d = g.makeVertex("d");
        a.addEdges(b,d);
        b.addEdge(c);
        c.addEdge(d);

        new HierarchyBuilder().assignLevels(g, EdgeDirection.FORWARD);
        new ProperTransformer().makeProper(g);

        assertEquals(6,g.size());

        // make sure that we are proper
        for (Vertex<String> v : g) {
            for (Vertex w : v.forward)
                assertEquals(v.level+1,w.level);
            for (Vertex w : v.backward)
                assertEquals(w.level+1,v.level);
        }
    }
}
