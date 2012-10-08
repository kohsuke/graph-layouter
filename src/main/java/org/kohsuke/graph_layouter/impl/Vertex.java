package org.kohsuke.graph_layouter.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Comparator;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * @author Kohsuke Kawaguchi
 */
public final class Vertex<T> {
    public final Set<Vertex<T>> forward = new HashSet<Vertex<T>>();
    public final Set<Vertex<T>> backward = new HashSet<Vertex<T>>();

    /**
     * Used by {@link Dfs} to mark vertices that are already visited.
     */
    Object dfsMarker;

    /**
     * Level of this vertex.
     * <p>
     * In the Sugiyama algorithm, this is an important factor in the layout of vertices.
     * All vertices of the same level are drawn on a single line.
     */
    int level;

    /**
     * Order among verticies that belong to the same level.
     * Updated by {@link Level#assignOrder()}.
     */
    int order;

    /**
     * Cluster that this vertex is in.
     */
    Cluster cluster;

    /**
     * User object that represents the vertex.
     */
    public final T tag;

    /**
     * Non-null if this vertex is a dummy vertex inserted
     * to make the graph proper.
     *
     * If non-null, this represents the ultimate source and sink
     * of the edge.
     */
    public final Vertex<T> source,sink;

    /**
     * Size of this vertex in the drawing.
     * This value is used to determine (x,y) coordinate.
     */
    public final Dimension size;

    /**
     * Position of the cetner of this vertex in the drawing.
     * This is the ultimate objective of the whole computation.
     */
    public final Point pos = new Point();

    public Vertex(T tag, Dimension size) {
        this.tag = tag;
        this.size = size;
        this.source = this.sink = null;
    }

    /*package*/ Vertex(Vertex<T> source, Vertex<T> sink) {
        this.tag = null;
        this.size = new Dimension(0,0);
        this.source = source;
        this.sink = sink;
    }

    /**
     * Adds an edge "this->that" to this graph.
     */
    public void addEdge(Vertex<T> that) {
        this.forward.add(that);
        that.backward.add(this);
    }

    /**
     * Adds multiple edges.
     */
    public void addEdges(Vertex<T>... that) {
        for (Vertex<T> v : that)
            addEdge(v);
    }

    public void removeEdge(Vertex<T> that) {
        this.forward.remove(that);
        that.backward.remove(this);
    }

    public String toString() {
        return tag.toString();
    }

    /**
     * Returns the top-left corner.
     */
    public Point topLeft() {
        return new Point(pos.x-size.width/2, pos.y-size.height/2);
    }

    /**
     * Returns the bounding box of this vertex.
     */
    public Rectangle boundBox() {
        return new Rectangle(topLeft(),size);
    }

    public boolean isDummy() {
        return source!=null; // sink!=null would have done, too.
    }

    /**
     * Sorts the array by {@link Vertex#order}.
     *
     * <p>
     * This method sorts the array in place and returns the same array.
     */
    public static <T> Vertex<T>[] sortByOrder(Vertex<T>[] array) {
        Arrays.sort(array,BY_ORDER);
        return array;
    }

    private static final Comparator<Vertex> BY_ORDER = new Comparator<Vertex>() {
        public int compare(Vertex lhs, Vertex rhs) {
            if(lhs.order<rhs.order) return -1;
            if(lhs.order>rhs.order) return +1;
            return 0;
        }
    };
}
