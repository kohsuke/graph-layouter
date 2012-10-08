package org.kohsuke.graph_layouter.impl;

import java.util.Set;

/**
 * Abstraction of vertex reordering algorithm.
 *
 * @author Kohsuke Kawaguchi
 */
public interface OrderingHeuristic {
    /**
     * Computes the 'weight' of the given vertex, by using the current level as fixed.
     *
     * @param dir
     *      Direction of the traversal. 'v' always belongs to the "next" level to the current level
     *      in the given traversal direction.
     * @return
     *      float representing the weight. Vertices with smaller weight comes to left.
     *      A special constant {@link #DONT_MOVE} can be used to indicate that the vertex
     *      should be left in its current position.
     */
    <T> float weight(Vertex<T> v, Level<T> current, LevelDirection dir);

    public static final float DONT_MOVE = -1;

    /**
     * Weighted median algorithm.
     */
    public class WeightedMedian implements OrderingHeuristic {
        public <T> float weight(Vertex<T> v, Level<T> current, LevelDirection dir) {
            assert dir.next(current).contains(v);

            Set<Vertex<T>> fe = dir.backwardEdges(v);
            Vertex[] edges = fe.toArray(new Vertex[fe.size()]);
            Vertex.sortByOrder(edges);

            if(edges.length==0) return DONT_MOVE;

            if(edges.length%2==1) {
                // odd size
                return edges[edges.length/2].order;
            }

            // even size, so there's left median and right median
            Vertex medL = edges[edges.length/2-1];
            Vertex medR = edges[edges.length/2];

            if(edges.length==2) {
                // in the middle
                return ((float)medL.order+medR.order)/2;
            }

            // weighted median to favor tightly packed vertices
            float left = medL.order - edges[0].order;
            float right= edges[edges.length-1].order - medR.order;
            return (medL.order*right + medR.order*left)/(left+right);
        }
    }

    /**
     * Barycenter algorithm.
     */
    public class BaryCenter implements OrderingHeuristic {
        public <T> float weight(Vertex<T> v, Level<T> current, LevelDirection dir) {
            assert dir.next(current).contains(v);

            Set<Vertex<T>> fe = dir.backwardEdges(v);

            if(fe.isEmpty())    return DONT_MOVE;

            int r=0;
            for (Vertex<T> w : fe)
                r+=w.order;

            return ((float)r)/fe.size();
        }
    }
}
