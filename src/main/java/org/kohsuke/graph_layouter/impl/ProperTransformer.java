package org.kohsuke.graph_layouter.impl;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author Kohsuke Kawaguchi
 */
public class ProperTransformer {
    /**
     * Makes the graph "proper" by inserting dummy nodes.
     *
     * In a proper graph, edges only run between adjacent levels.
     *
     * This alters the given {@link Collection}.
     */
    public <T> void makeProper(Collection<Vertex<T>> graph) {
        for (Vertex<T> v : new ArrayList<Vertex<T>>(graph)) {// copy first since we'll modify graph
            for (Vertex<T> w : new ArrayList<Vertex<T>>(v.forward)) {// copy first since we'll modify graph
                assert v.level<w.level;
                if(w.level-v.level>1) {
                    // this is a long edge that cross multiple levels. split them
                    // to smaller edges by inserting dummy nodes

                    v.removeEdge(w);
                    Vertex<T> src = v;
                    for(int i=v.level; i<w.level-1; i++) {
                        Vertex<T> u = new Vertex<T>(v,w);
                        graph.add(u);
                        u.level=i+1;
                        src.addEdge(u);
                        src = u;
                    }
                    src.addEdge(w);
                }
            }
        }
    }
}
