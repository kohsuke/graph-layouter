package org.kohsuke.graph_layouter.impl;

import org.kohsuke.graph_layouter.impl.OrderingHeuristic.BaryCenter;
import org.kohsuke.graph_layouter.impl.OrderingHeuristic.WeightedMedian;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
@SuppressWarnings({"unchecked"})
public class OrderAssignerTest extends GraphTestBase {


    static {
        // if you need to log
        //Logger logger = Logger.getLogger(Coordinator.class.getName());
        //logger.setLevel(java.util.logging.Level.FINER);
        //ConsoleHandler handler = new ConsoleHandler();
        //handler.setLevel(java.util.logging.Level.FINER);
        //logger.addHandler(handler);
    }

    public void test1() throws IOException {
        test(new WeightedMedian());
    }

    public void test2() throws IOException {
        for(int i=0; i<16; i++)
            test1();
    }

    public void test3() throws IOException {
        test(new BaryCenter());
    }

    public void test4() throws IOException {
        for(int i=0; i<16; i++)
            test3();
    }

    /**
     * <pre>
     * B C A
     * |/ /
     * D E F
     * |/|\|
     * G I H
     * |  /|
     * L K J
     * </pre>
     * @param orderingHeuristic
     */
    public void test(OrderingHeuristic orderingHeuristic) throws IOException {
        Graph<String> graph = new Graph<String>();
        Vertex a = graph.makeVertex("a");
        Vertex b = graph.makeVertex("b");
        Vertex c = graph.makeVertex("c");
        Vertex d = graph.makeVertex("d");
        Vertex e = graph.makeVertex("e");
        Vertex f = graph.makeVertex("f");
        Vertex g = graph.makeVertex("g");
        Vertex h = graph.makeVertex("h");
        Vertex i = graph.makeVertex("i");
        Vertex j = graph.makeVertex("j");
        Vertex k = graph.makeVertex("k");
        Vertex l = graph.makeVertex("l");

        a.addEdge(e);
        b.addEdge(d);
        c.addEdge(d);

        d.addEdge(g);
        e.addEdges(g,h,i);
        f.addEdge(h);

        g.addEdge(l);
        h.addEdges(k,j);


        new HierarchyBuilder().assignLevels(graph, EdgeDirection.FORWARD);

        LevelMap<String> lm = new LevelMap<String>(graph);
        // if you need to debug the problem by forcing a certain initial order
        //lm.get(0).reorder($(a,b,c));
        //lm.get(1).reorder($(e,d,f));
        //lm.get(2).reorder($(i,g,h));
        //lm.get(3).reorder($(l,k,j));

        new OrderAssigner(orderingHeuristic).layout(lm);

        System.out.println(lm);

        assertOnLevel(0,a,b,c);
        assertOnLevel(1,d,e,f);
        assertOnLevel(2,g,h,i);
        assertOnLevel(3,j,k,l);
        // optimal layout involves no crossing
        assertEquals(0,lm.countCrossing());

        assertAllOrNothing(
            assertOrder(lm.get(0),$(b,c,a),$(c,b,a)),
            assertOrder(lm.get(1),$(d,e,f)),
            assertOrder(lm.get(2),$(g,i,h)),
            assertOrder(lm.get(3),$(l,k,j),$(l,j,k))
        );

        new Coordinator().layout(lm);
        graph.draw(new File("test.png"));
        System.out.println(lm);
    }

    /**
     * Convenient form to create an array.
     */
    public static <T> List<Vertex<T>> $(Vertex<T>... vertices) {
        return Arrays.asList(vertices);
    }

    /**
     * Requires that all the parameters are true or all are false.
     */
    public static void assertAllOrNothing(boolean... bs) {
        boolean foundFalse=false,foundTrue=false;

        for (boolean b : bs) {
            if(b)   foundTrue=true;
            else    foundFalse=true;
        }

        assertTrue(!(foundFalse && foundTrue));
    }

    /**
     * Checks if the ordering of the level is one of the specified pattern
     * or its mirror pattern.
     *
     * Return true if the ordering matched in its given form, and false
     * if the ordering matched in the mirror form. Asserts if the ordering
     * doesn't match.
     */
    public static <T> boolean assertOrder(Level<T> level, List<Vertex<T>>... orders) {
        Vertex[] actual = level.vertices.toArray(new Vertex[level.vertices.size()]);

        for (List<Vertex<T>> order : orders) {
            if(Arrays.equals(actual,order.toArray()))
                return true; // OK.
        }

        // reverse
        for( int i=0; i<actual.length/2; i++ ) {
            int j = actual.length - i - 1;
            Vertex t = actual[i];
            actual[i] = actual[j];
            actual[j] = t;
        }

        for (List<Vertex<T>> order : orders) {
            if(Arrays.equals(actual,order.toArray()))
                return false; // OK.
        }

        fail("inconsistent order: "+actual);
        return false;   // never run
    }
}
