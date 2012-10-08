package org.kohsuke.graph_layouter.impl;

import java.util.List;
import java.util.AbstractList;

/**
 * {@link Vertex} with assigned weight, which is used as the sort key.
 *
 * @see OrderingHeuristic
 */
class WeightedVertex<T> implements Comparable<WeightedVertex<T>> {
    public final float weight;
    public final Vertex<T> v;

    public WeightedVertex(float w, Vertex<T> v) {
        this.weight = w;
        this.v = v;
    }

    public int compareTo(WeightedVertex<T> that) {
        return Float.compare(this.weight,that.weight);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(v).append('=').append(weight);
        return buf.toString();
    }

    /**
     * Takes a {@link List} of {@link WeightedVertex}s and make it look like a list of {@link Vertex}.
     */
    public static final class Adapter<T> extends AbstractList<Vertex<T>> {
        private final List<WeightedVertex<T>> core;

        public Adapter(List<WeightedVertex<T>> core) {
            this.core = core;
        }

        public Vertex<T> get(int index) {
            return core.get(index).v;
        }

        public int size() {
            return core.size();
        }
    }
}
