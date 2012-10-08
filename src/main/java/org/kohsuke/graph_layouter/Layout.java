package org.kohsuke.graph_layouter;

import org.kohsuke.graph_layouter.impl.Coordinator;
import org.kohsuke.graph_layouter.impl.EdgeDirection;
import org.kohsuke.graph_layouter.impl.HierarchyBuilder;
import org.kohsuke.graph_layouter.impl.LevelMap;
import org.kohsuke.graph_layouter.impl.OrderAssigner;
import org.kohsuke.graph_layouter.impl.ProperTransformer;
import org.kohsuke.graph_layouter.impl.Vertex;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point to the layout algorithm.
 *
 * @author Kohsuke Kawaguchi
 */
public class Layout<T> {
    private final Map<T, Vertex<T>> graph = new LinkedHashMap<T,Vertex<T>>();
    private final Direction dir;

    /**
     * Computes the layout and determines the positions.
     *
     * <p>
     * The computed positions can be queried by calling the getters
     * after this object is created.
     */
    public Layout(Navigator<T> nav, Direction dir) {
        this.dir = dir;

        Collection<T> vertices = nav.vertices();

        for (T t : vertices)
            graph.put(t,new Vertex<T>(t,rotate(nav.getSize(t))));

        for (T t : vertices) {
            Vertex<T> v = graph.get(t);
            for (T u : nav.edge(t))
                v.addEdge(graph.get(u));
        }

        // four steps algorithm
        List<Vertex<T>> g = new ArrayList<Vertex<T>>(graph.values()); // ProperTransformer alters the list, so make a copy
        new HierarchyBuilder().assignLevels(g, EdgeDirection.FORWARD);
        new ProperTransformer().makeProper(g);
        LevelMap<T> lm = new OrderAssigner().layout(g);
        new Coordinator().layout(lm);
    }

    /**
     * Returns all the vertices.
     */
    public Collection<T> vertices() {
        return graph.keySet();
    }

    /**
     * Returns all the edges from the given vertex.
     */
    public Collection<T> edges(T v) {
        List<T> r = new ArrayList<T>();
        for (Vertex<T> w : graph.get(v).forward) {
            if(w.isDummy()) r.add(w.sink.tag);
            else            r.add(w.tag);
        }
        return r;
    }

    /**
     * Gets the bounding box of the vertex.
     *
     * @return
     *      null if t was not reported by {@link Navigator}.
     */
    public Rectangle vertex(T t) {
        Vertex<T> v = graph.get(t);
        if(v==null) return null;
        return rotate(v.boundBox());
    }

    /**
     * Gets the way points of the edge (v,w)
     *
     * @return
     *      null if there's no such edge.
     *      Otherwise the list will contain any intermediate waypoints from v to w.
     *      If the edge (v,w) should be drawn as a straight-line, then
     *      this method returns empty list.
     */
    public List<Point> edge(T v, T w) {
        Vertex<T> a = graph.get(v);
        if(a==null) return null;

        Vertex<T> b = graph.get(w);
        if(b==null) return null;

        if(a.forward.contains(b)) {
            // direct edge
            return Collections.emptyList();
        } else {
            for (Vertex<T> c : a.forward) {
                assert c.source==a;
                if(c.sink==b) {
                    // this is the path. follow it.
                    List<Point> points = new ArrayList<Point>();
                    do {
                        points.add(rotate(c.pos));
                        c = c.forward.iterator().next();
                    } while(c.isDummy());
                    return points;
                }
            }
            // no such edge
            return null;
        }
    }

    /**
     * Computes the size needed to hold all figures.
     */
    public Rectangle calcDrawingArea() {
        Rectangle area = null;
        for (Vertex<T> v : graph.values()) {
            Rectangle vr = v.boundBox();
            if(area==null)  area=vr;
            else            area.add(vr);
        }

        if(area==null)  area = new Rectangle();
        return rotate(area);
    }

    @SuppressWarnings({"SuspiciousNameCombination"})
    private Dimension rotate(Dimension sz) {
        switch (dir) {
        case LEFTRIGHT:
            return new Dimension(sz.height,sz.width);
        case TOPDOWN:
            return sz;
        }
        throw new AssertionError();
    }

    @SuppressWarnings({"SuspiciousNameCombination"})
    private Point rotate(Point p) {
        switch (dir) {
        case LEFTRIGHT:
            return new Point(p.y,p.x);
        case TOPDOWN:
            return p;
        }
        throw new AssertionError();
    }

    @SuppressWarnings({"SuspiciousNameCombination"})
    private Rectangle rotate(Rectangle r) {
        switch (dir) {
        case LEFTRIGHT:
            return new Rectangle(r.y, r.x, r.height, r.width);
        case TOPDOWN:
            return r;
        }
        throw new AssertionError();
    }
}
