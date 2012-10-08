package org.kohsuke.graph_layouter;

import java.util.Collection;
import java.awt.Dimension;

/**
 * Implemented by the application to allow the graph layouter to construct a graph. 
 *
 * @author Kohsuke Kawaguchi
 */
public interface Navigator<T> {
    /**
     * List up all vertices.
     */
    Collection<T> vertices();

    /**
     * List up all edges from the given vertex t.
     */
    Collection<T> edge(T t);

    /**
     * Bounding box of the given vertex on the graph.
     */
    Dimension getSize(T t);
}
