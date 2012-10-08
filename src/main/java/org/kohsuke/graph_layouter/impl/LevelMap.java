package org.kohsuke.graph_layouter.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Set of {@link Level}s.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class LevelMap<T> {
    /**
     * Levels that are already created. This must be "dense".
     * That is, if level i and level j are in this map, all level k (j>k>i) must be
     * in this map, too.
     */
    private final SortedMap<Integer,Level<T>> core = new TreeMap<Integer,Level<T>>();

    /**
     * Creates an empty level map.
     */
    public LevelMap() {
    }

    /**
     * Creates a filled level map.
     *
     * <p>
     * Uses DFS to scan all vertices so that initial order inside a level
     * do not have crossing if the graph was a tree [TSE93]. I guess the idea
     * is that this tends to create a fewer initial crossings than a "random" placement
     * by scanning the graph linearly.
     */
    public LevelMap(Collection<Vertex<T>> graph) {
        new Dfs<T>(EdgeDirection.FORWARD) {
            protected void in(Vertex<T> v) {
                make(v.level).vertices.add(v);
            }
        }.run(graph);

        // assign the initial order
        for (Level<T> l : levels())
            l.assignOrder();
    }

    /**
     * All levels in ascending order.
     */
    public Collection<Level<T>> levels() {
        return core.values();
    }

    public Level<T> get(int n) {
        return core.get(n);
    }

    public Level<T> make(final int n) {
        Level<T> l = core.get(n);
        if(l!=null) return l;

        if(core.isEmpty()) {
            // first level
            l = new Level<T>(n);
            core.put(l.n,l);
            return l;
        }

        int k = core.firstKey();
        if(n<k) {
            l = core.get(k);
            while(l.n!=n) {
                l = l.makePrev();
                core.put(l.n,l);
            }
            return l;
        }

        k = core.lastKey();
        l = core.get(k);
        assert k<n;
        while(l.n!=n) {
            l = l.makeNext();
            core.put(l.n,l);
        }
        return l;
    }

    public Level<T> first() {
        return core.get(core.firstKey());
    }

    public Level<T> last() {
        return core.get(core.lastKey());
    }

    /**
     * Counts the number of crossing in the current ordering.
     */
    public int countCrossing() {
        int crossing = 0;
        for (Level<T> lv : levels())
            crossing += lv.countCrossings();
        return crossing;
    }

    public final class Memento {
        private final List<Level.Memento> mementos = new ArrayList<Level.Memento>();
        public Memento() {
            for (Level<T> lv : levels())
                mementos.add(lv.new Memento());
        }

        /**
         * Restores the memento.
         */
        public void restore() {
            for (Level.Memento m : mementos)
                m.restore();
        }
    }


    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (Level<T> lv : levels()) {
            buf.append(lv.toString());
            buf.append('\n');
        }
        return buf.toString();
    }
}
