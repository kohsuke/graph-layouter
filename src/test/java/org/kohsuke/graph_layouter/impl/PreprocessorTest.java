package org.kohsuke.graph_layouter.impl;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class PreprocessorTest extends TestCase {
    /**
     *
     * <pre>
     *    +-> x
     *    |
     * a -+-> b -+-> c
     *           |
     *        y -+
     * </pre>
     */
    public void test1() throws Exception {
        Graph<String> g = new Graph<String>();
        Vertex a = g.makeVertex("a");
        Vertex b = g.makeVertex("b");
        Vertex c = g.makeVertex("c");
        Vertex x = g.makeVertex("x");
        Vertex y = g.makeVertex("y");

        a.addEdges(x,b);
        b.addEdge(c);
        y.addEdge(c);

        HierarchyBuilder proc = new HierarchyBuilder();


        for (EdgeDirection dir : EdgeDirection.values()) {
            proc.assignLevels(g,dir);

            int lv = a.level;
            assertEquals(lv+1,x.level);
            assertEquals(lv+1,b.level);
            assertEquals(lv+1,y.level);
            assertEquals(lv+2,c.level);
        }

        proc.topologicalSort(g, EdgeDirection.FORWARD);
        int lv = a.level;
        assertEquals(lv+1,x.level);
        assertEquals(lv+1,b.level);
        assertEquals(lv  ,y.level); // y is a source so it should be on the same level as a.
        assertEquals(lv+2,c.level);

        proc.topologicalSort(g, EdgeDirection.BACKWARD);
        lv = a.level;
        assertEquals(lv+2,x.level); // x should be pushed down to the floor as it's a sink
        assertEquals(lv+1,b.level);
        assertEquals(lv+1,y.level);
        assertEquals(lv+2,c.level);
    }

}
