package org.kohsuke.graph_layouter.impl;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Looks for long line and tries to straighten them.
 *
 * This is a heuristic implementation that locally looks for a long edge that crosses
 * multiple levels and try to straighten them, provided that doing so will not reorder
 * nodes within the same level. This tends to create more aesthetically pleasing result.
 *
 * @author Kohsuke Kawaguchi
 */
public class StraightenLongEdge<T> {
    /**
     * Minimum space required between {@link Vertex}s.
     *
     * TODO: consolidate with {@link Coordinator#xGap}
     */
    private final int xGap = 10;

    private final int yGap = 10;

    public void layout(LevelMap<T> lm) {
        boolean improving;

        /*
            TODO: if there are enough space, single line can be both top-aligned and bottom-aligned:

              x         x
              |          \
              |           |
               \          |
                y         y

            so this algorithm will never terminate. for now, set a cap of iteration, but a better
            way to detect this is preferred.

            TODO: performance improvement: there gotta be a way to order edges that minimizes
            the # of iterations. left-to-right or right-to-left scan.
         */
        int count=0;
        do {
            improving = false;
            for (Level<T> lv : lm.levels()) {
                for (Vertex<T> v : lv.vertices) {
                    if (v.isDummy())    continue;
                    for (Vertex<T> w : v.forward) {
                        improving |= straighten(lv,v,w,EdgeDirection.FORWARD);
                    }
                    for (Vertex<T> w : v.backward) {
                        improving |= straighten(lv,v,w,EdgeDirection.BACKWARD);
                    }
                }
            }
        } while (improving && count++ < 10);
        LOGGER.fine(String.format("Looped %d times",count));
    }

    /**
     * @return
     *      true if the edge was straightened, false if nothing was touched.
     */
    private boolean straighten(final Level<T> lv, final Vertex<T> v, final Vertex<T> w, final EdgeDirection dir) {
        assert lv.contains(v);

        for (Vertex<T> u=w; true; ) {
            if (u.pos.x!=w.pos.x) {
                final int ideal = u.pos.x;

                // to see if we can move virtual nodes a=[w,...u) to u.pos.x,
                // verify that doing so will not violate the relative vertices positions within the same rank
                Level<T> av = lv.forward(dir);
                for (Vertex<T> a=w; a!=u; a=singleton(dir.getEdges(a)), av=av.forward(dir)) {
                    int pos = av.indexOf(a);
                    assert pos>=0 : "a must belongs to this level";

                    int before = pos==0 ? Integer.MIN_VALUE /* no left constraint */ : av.vertices.get(pos - 1).bottomRight().x+xGap;
                    int after  = pos==av.vertices.size()-1 ? Integer.MAX_VALUE /* no right constraint */ : av.vertices.get(pos+1).topLeft().x -xGap;

                    if (!lte_lte(before, ideal, after))
                        return false;   // can't move without swapping the node
                }

                // looks good, move them
                for (Vertex<T> a=w; a!=u; a=singleton(dir.getEdges(a))) {
                    a.pos.x = ideal;
                }
                return true;
            }

            if (u.isDummy())
                u=singleton(dir.getEdges(u));
            else
                return false;
        }
    }

    private Vertex<T> singleton(Set<Vertex<T>> edges) {
        assert edges.size()==1;
        return edges.iterator().next();
    }

    private boolean lte_lte(int a, int b, int c) {
        return a<=b && b<=c;
    }

    private static final Logger LOGGER = Logger.getLogger(StraightenLongEdge.class.getName());
}
