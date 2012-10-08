package org.kohsuke.graph_layouter.impl;

import java.util.Collection;

/**
 * Used in {@link HierarchyBuilder#fall(Collection, EdgeDirection)} to move
 * vertices closer to each other to avoid unnecessary long edges.
 *
 * @author Kohsuke Kawaguchi
 */
class Cluster {
    /**
     * This field holds the amount of level adjustments for vertices in thsi cluster.
     */
    int dropHeight = Integer.MAX_VALUE;

    /**
     * Adds the given vertex and recursively finds vertex cluster from there.
     * In the end {@link Vertex#cluster} of all verticies in this cluster will be
     * updated to point to this cluster.
     */
    public void add(Vertex<?> v) {
        assert v.cluster==null;
        v.cluster = this;

        for (Vertex w : v.forward) {
            if(w.level==v.level+1 && w.cluster==null)
                add(w);
        }

        for (Vertex w : v.backward) {
            if(w.level+1==v.level && w.cluster==null)
                add(w);
        }

    }
}
