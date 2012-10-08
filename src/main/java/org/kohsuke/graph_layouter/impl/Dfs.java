package org.kohsuke.graph_layouter.impl;

import java.util.Collection;
import java.util.Stack;

/**
 * Base class to perform DFS search.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class Dfs<T> {
    protected final EdgeDirection dir;

    /**
     * @param dir
     *      Traversal direction. If {@link EdgeDirection#FORWARD}, the search will
     *      be performed by following edges. If {@link EdgeDirection#BACKWARD} then
     *      the search goes in the reverse order by following edges backward.
     */
    protected Dfs(EdgeDirection dir) {
        this.dir = dir;
    }

    /**
     * Runs a DFS search by starting from given set of vertices.
     */
    public final void run(Collection<Vertex<T>> vertices) {
        abstract class Task {
            abstract void perform();
        }

        final Stack<Task> s = new Stack<Task>();
        final Object marker = new Object();

        class In extends Task {
            final Vertex<T> v;

            public In(Vertex<T> v) {
                this.v = v;
            }

            void perform() {
                if(v.dfsMarker==marker)
                    return; // already visited
                v.dfsMarker=marker;

                in(v);

                // schedule 'out'
                s.push(new Task() {
                    void perform() {
                        out(v);
                    }
                });

                // schedule visits
                for (Vertex<T> w : dir.getEdges(v)) {
                    if(w.dfsMarker!=marker)
                        s.push(new In(w));
                }
            }
        }

        for (Vertex<T> v : vertices) {// visit vertices in the order given
            s.push(new In(v));

            while(!s.isEmpty())
                s.pop().perform();
        }
    }

    /**
     * Called in-order when the vertex is visited.
     */
    protected void in(Vertex<T> v) {}
    
    /**
     * Called out-order when the vertex is visited.
     */
    protected void out(Vertex<T> v) {}
}
