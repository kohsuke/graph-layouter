package org.kohsuke.graph_layouter.impl;

import junit.framework.TestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class GraphTestBase extends TestCase {
    public static void assertOnLevel(int lv, Vertex... vertices ) {
        for (Vertex v : vertices)
            assertEquals(lv,v.level);
    }
}
