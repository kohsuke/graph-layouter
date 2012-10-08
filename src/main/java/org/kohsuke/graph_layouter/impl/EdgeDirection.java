package org.kohsuke.graph_layouter.impl;

import java.util.Set;

/**
 * @author Kohsuke Kawaguchi
 */
public enum EdgeDirection {
    FORWARD {
        <T> Set<Vertex<T>> getEdges(Vertex<T> v) {
            return v.forward;
        }
        EdgeDirection opposite() {
            return BACKWARD;
        }
        int sign() {
            return 1;
        }
    },
    BACKWARD {
        <T> Set<Vertex<T>> getEdges(Vertex<T> v) {
            return v.backward;
        }
        EdgeDirection opposite() {
            return FORWARD;
        }
        int sign() {
            return -1;
        }
    };

    abstract <T> Set<Vertex<T>> getEdges(Vertex<T> v);
    abstract EdgeDirection opposite();
    /**
     * +1 if {@link #FORWARD} otherwise -1.
     */
    abstract int sign();
}
