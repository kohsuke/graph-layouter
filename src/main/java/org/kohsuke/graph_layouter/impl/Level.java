package org.kohsuke.graph_layouter.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a set of {@link Vertex} that form a single level.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class Level<T> {
    private Level<T> next,prev;
    /**
     * Integer that represents this level.
     * This is consistent with the order relationship among levels,
     * but not necessarily 0-origin. 
     */
    public final int n;

    public final ArrayList<Vertex<T>> vertices = new ArrayList<Vertex<T>>();

    public Level(int n) {
        this.n = n;
    }

    public Level<T> prev() {
        return prev;
    }

    public Level<T> next() {
        return next;
    }

    public Level<T> makeNext() {
        if(next==null) {
            next = new Level<T>(n+1);
            next.prev = this;
        }
        return next;
    }

    public Level<T> makePrev() {
        if(prev==null) {
            prev = new Level<T>(n-1);
            prev.next = this;
        }
        return prev;
    }

    public Level<T> first() {
        Level<T> l = this;
        while(l.prev!=null)
            l = l.prev;
        return l;
    }

    public Level<T> last() {
        Level<T> l = this;
        while(l.next!=null)
            l = l.next;
        return l;
    }

    /**
     * Assigns {@link Vertex#order}.
     */
    /*package*/ void assignOrder() {
        int i=0;
        for (Vertex<T> v : vertices)
            v.order = i++;
    }

    /**
     * Reorders the vertices in this level by using the new given order.
     */
    public void reorder(List<Vertex<T>> newList) {
        assert vertices.size()==newList.size();
        for( int i=0; i<newList.size(); i++ ) {
            Vertex<T> v = newList.get(i);
            vertices.set(i, v);
            v.order = i;
        }
    }

    /**
     * Reorders the vertices in this level by swapping two vertices.
     */
    public void swap(Vertex<T> v, Vertex<T> w) {
        assert vertices.get(v.order)==v;
        assert vertices.get(w.order)==w;

        vertices.set(v.order,w);
        vertices.set(w.order,v);

        int t = v.order;
        v.order = w.order;
        w.order = t;
    }

    /**
     * Gets the number of crossings between this level and its two adjacent levels.
     */
    public int getAdjacentCrossings() {
        int r = countCrossings();
        if(prev!=null)  r+=prev.countCrossings();
        return r;
    }

    /**
     * Gets the number of crossings between this level and its two adjacent levels,
     * when two vertices v and w on this level are swapped.
     */
    public int getAdjacentSwapCrossing(Vertex<T> v, Vertex<T> w) {
        return Level.countSwapCrossing(prev, this, v,w, EdgeDirection.FORWARD)
              +Level.countSwapCrossing(next, this, v,w, EdgeDirection.BACKWARD);
    }

    /**
     * Counts the number of edge crossings between this level and the {@link #next} level.
     */
    public int countCrossings() {
        if(next==null)  return 0;   // no next level

        // try to find a pair (a,c) and (b,d) like this
        //
        // a  b
        //  \/
        //  /\
        // d  c

        int crossing = 0;

        for( int ai=0; ai<vertices.size()-1; ai++ ) {
            Vertex<T> a = vertices.get(ai);
            for( int bi=ai+1; bi<vertices.size(); bi++ ) {
                Vertex<T> b = vertices.get(bi);

                for(Vertex<T> c : a.forward) {
                    for(Vertex<T> d : b.forward) {
                        if(c.order>d.order)
                            crossing++;
                    }
                }
            }
        }

        return crossing;
    }

    /**
     * Counts the number of edge crossings between two levels, by assuming v and w's positions are swapped.
     *
     * @param v
     *      must be on the 'to' level.
     * @param w
     *      must be on the 'to' level.
     * @param dir
     *      The direction from 'from' to 'to'.
     */
    public static <T> int countSwapCrossing(Level<T> from, Level<T> to, Vertex<T> v, Vertex<T> w, EdgeDirection dir) {
        if(from==null)    return 0; // not valid levels
        assert to!=null;

        // v and w must be on the 'to' level
        assert to.n==v.level;
        assert to.n==w.level;
        // from and to must be adjacent, and its ordering must be consistent with the direction
        assert from.n+dir.sign()==to.n;

        // try to find a pair (a,c) and (b,d) like this
        //
        // a  b
        //  \/
        //  /\
        // d  c

        int crossing = 0;

        for( int ai=0; ai<from.vertices.size()-1; ai++ ) {
            Vertex<T> a = from.vertices.get(ai);
            for( int bi=ai+1; bi<from.vertices.size(); bi++ ) {
                Vertex<T> b = from.vertices.get(bi);

                for(Vertex<T> c : dir.getEdges(a)) {
                    // swap
                    if(c==v)    c=w;
                    else
                    if(c==w)    c=v;

                    for(Vertex<T> d : dir.getEdges(b)) {
                        // swap
                        if(d==v)    d=w;
                        else
                        if(d==w)    d=v;

                        if(c.order>d.order)
                            crossing++;
                    }
                }
            }
        }

        return crossing;
    }

    public boolean contains(Vertex<T> v) {
        // v.level==n => verticies.get(v.order)==v
        assert v.level!=n || vertices.get(v.order)==v;
        return v.level==n;
    }

    /**
     * Represents the backed up ordering information.
     */
    /*package*/ final class Memento {
        private final List<Vertex<T>> order = new ArrayList<Vertex<T>>(vertices);
        void restore() {
            reorder(order);
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        boolean first = true;
        for (Vertex<T> v : vertices) {
            if(!first)
                buf.append(',');
            first = false;
            buf.append(v).append('=').append(v.pos.x);
        }
        buf.append(']');
        return buf.toString();
    }
}
