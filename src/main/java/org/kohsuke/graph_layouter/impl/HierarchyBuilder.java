package org.kohsuke.graph_layouter.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Decomposes DAG to hierarchical graph, where vertices are assigned levels,
 * such that edges only flow from lower levels to higher levels.
 *
 * Levels assigned will be set to {@link Vertex#level}.
 *
 * <h3>Example</h3>
 * <pre>
 * B C A   <-- level 0
 * |/ /
 * D E F   <-- level 1
 * |/|\|
 * G I H   <-- level 2
 * |  /|
 * L K J   <-- level 3
 * </pre>
 *
 * @author Kohsuke Kawaguchi
 */
public class HierarchyBuilder {
    /**
     * Decomposes DAG to levels.
     *
     * @param dir
     *      If {@link EdgeDirection#FORWARD}, then the nodes tend to fall down
     *      on the "floor". And otherwise nodes tend to be pulled up on the ceiling.
     */
    public <T> void assignLevels(Collection<Vertex<T>> graph, EdgeDirection dir) {
        topologicalSort(graph,dir);
        fall(graph,dir);
    }

    /**
     * Do a topological sort and assigns {@link Vertex#level}.
     *
     * @param dir
     *      If {@link EdgeDirection#FORWARD}, then the algorithm will "pulls up"
     *      all source nodes to the level 0. If {@link EdgeDirection#BACKWARD},
     *      then it "pushes down" all sink nodes to level N.
     *
     * @return
     *      Topological order of {@link Vertex}s.
     *      If dir==FORWARD,  topoOrder is sorted from sink to source.
     *      If dir==BACKWARD, topoOrder is sorted from source to sink.
     */
    public <T> List<Vertex<T>> topologicalSort(Collection<Vertex<T>> graph, EdgeDirection dir) {
        // Receives vertices in the reverse topological order,
        // where (v,w) \in E =>  indexOf(v) > indexOf(w)
        final List<Vertex<T>> topoOrder = new ArrayList<Vertex<T>>(graph.size());
        new Dfs<T>(dir) {
            protected void out(Vertex<T> v) {
                topoOrder.add(v);
            }
        }.run(graph);

        // assigns levels
        EdgeDirection rdir = dir.opposite();
        for( int i=graph.size()-1; i>=0; i-- ) {
            Vertex<T> v = topoOrder.get(i);
            int level = 0;
            for (Vertex<T> w : rdir.getEdges(v)) {
                switch(dir) {
                case FORWARD:
                    level = Math.max(level,w.level+1);
                    break;
                case BACKWARD:
                    level = Math.min(level,w.level-1);
                    break;
                }
            }
            v.level = level;
        }

        // break cycles by reversing edges
        for (Vertex<T> v : topoOrder) {
            for (Vertex<T> w : new ArrayList<Vertex<T>>(dir.getEdges(v))) {// make a copy because we might change this
                switch (dir) {
                case FORWARD:
                    if (v.level>w.level) {
                        v.forward.remove(w);
                        w.backward.remove(v);
                        v.backward.add(w);
                        w.forward.add(v);
                    }
                    break;
                case BACKWARD:
                    if (v.level<w.level) {
                        w.forward.remove(v);
                        v.backward.remove(w);
                        w.backward.add(v);
                        v.forward.add(w);
                    }
                    break;
                }
            }
        }


        return topoOrder;
    }

    /**
     * Performs clustering and move vertices to closer levels.
     *
     * @param dir
     *      If {@link EdgeDirection#FORWARD}, then the algorithm will "push down"
     *      nodes to higher levels (works with {@link #topologicalSort(Collection,EdgeDirection)}
     *      with {@link EdgeDirection#FORWARD}.)
     *
     *      Otherwise the algorithm will pull up.
     */
    public <T> void fall(Collection<Vertex<T>> graph, EdgeDirection dir) {
        boolean hasDrop;
        do {// drops might happen in multiple levels. so repeat until everything settles.
            assert nothingInCluster(graph);

            // form a cluster set
            for (Vertex v : graph) {
                if(v.cluster!=null)
                    continue;   // already belongs to a cluster
                new Cluster().add(v);
            }

            assert allBelongsToCluster(graph);

            // compute drop height.
            // if dir==FORWARD,  then this is the number of levels the nodes in a cluster
            // will be increased (push down effect.)
            // if dir==BACKWARD, then this is the number of levels the nodes in a cluster
            // will be decreased (pull up effect.)

            for (Vertex<T> v : graph) {// for all edges
                for (Vertex<T> w : dir.getEdges(v)) {
                    if(v.cluster!=w.cluster) {
                        assert Math.abs(w.level-v.level)>1;
                        // if |Lv(w)-Lv(v)|=0 then there shouldn't be an edge
                        // if |Lv(w)-Lv(v)|=1 then they should be in the same cluster
                        v.cluster.dropHeight = Math.min( v.cluster.dropHeight, Math.abs(w.level-v.level)-1 );
                    }
                }
            }

            hasDrop = false;
            for (Vertex v : graph) {// drop nodes
                int h = v.cluster.dropHeight;
                if(h!=Integer.MAX_VALUE) {
                    assert h>0; // if h==0 then v must have been merged with another cluster
                    hasDrop = true;
                    v.level += dir.sign()*h;
                }
                v.cluster=null; // erase the cluster mark for the next iteration
            }
        } while(hasDrop);
    }

    /**
     * Makes sure that no vertices belong to a cluster.
     */
    private <T> boolean nothingInCluster(Collection<Vertex<T>> graph) {
        for (Vertex v : graph)
            assert v.cluster==null;
        return true;
    }

    /**
     * Makes sure that all vertices belong to some cluster.
     */
    private <T> boolean allBelongsToCluster(Collection<Vertex<T>> graph) {
        for (Vertex v : graph)
            assert v.cluster!=null;
        return true;
    }
}
