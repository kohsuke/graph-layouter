package org.kohsuke.graph_layouter.impl;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;

import org.kohsuke.graph_layouter.impl.OrderingHeuristic.WeightedMedian;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Comparator;
import java.util.logging.*;

/**
 * Assigns order among vertices in the same level.
 * 
 * @author Kohsuke Kawaguchi
 */
public class OrderAssigner {
    private final OrderingHeuristic orderingHeuristic;
    private static final Logger LOGGER = Logger.getLogger(OrderAssigner.class.getName());

    public OrderAssigner(OrderingHeuristic orderingHeuristic) {
        this.orderingHeuristic = orderingHeuristic;
    }

    public OrderAssigner() {
        this(new WeightedMedian());
    }

    public <T> LevelMap<T> layout(Collection<Vertex<T>> graph) {
        LevelMap<T> lm = new LevelMap<T>(graph);
        layout(lm);
        return lm;
    }

    /*package*/ <T> void layout(LevelMap<T> lm) {
        for(int i=0; i<MAX_ITERATION; i++) {
            int xing = lm.countCrossing();
            if(LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Starting "+i+"th iteration: xing="+lm.countCrossing());
                LOGGER.fine("Graph=\n"+lm);
            }
            reorderAndTranspose(lm);

            if(LOGGER.isLoggable(FINE)) {
                LOGGER.fine("Finishing "+i+"th iteration: xing="+lm.countCrossing());
                LOGGER.fine("Graph=\n"+lm);
            }

            if(xing==lm.countCrossing()) {
                LOGGER.fine("Terminating as we are not making progress");
                return; // not making any progress, so terminate early
            }
        }

        // if we are still making progress, run it until we reach optimum.
        // but unlike above, if we see things worsen, cut it right there
        while(true) {
            int xing = lm.countCrossing();
            LevelMap<T>.Memento memento = lm.new Memento(); // remember the current ordering in case it's worsen in the next run

            reorderAndTranspose(lm);

            int newXing = lm.countCrossing();
            if(newXing>xing) {
                // worsen. abort.
                memento.restore();
                return;
            }
            if(newXing==xing)
                // no improvements
                return;
        }
    }

    private <T> void reorderAndTranspose(LevelMap<T> lm) {
        LevelDirection dir = LevelDirection.DOWN;
        for( int j=0; j<4; j++,dir=dir.opposite() ) {
            // flip verticies with the same weight/crossing in every other visit
            boolean flipEqual = j<2;
            reorder(dir,lm,flipEqual);

            if(LOGGER.isLoggable(FINER)) {
                LOGGER.finer("reordered: dir="+dir+",xing="+lm.countCrossing());
                LOGGER.finer("Graph=\n"+lm);
            }

            transpose(lm,flipEqual);

            if(LOGGER.isLoggable(FINER)) {
                LOGGER.finer("transposed: dir="+dir+",xing="+lm.countCrossing());
                LOGGER.finer("Graph=\n"+lm);
            }
        }
    }

    /**
     * Reorders verticies of levels by using {@link #orderingHeuristic}.
     */
    private <T> void reorder(LevelDirection dir, LevelMap<T> lm, boolean flipEqual) {
        for(org.kohsuke.graph_layouter.impl.Level<T> lv=dir.first(lm); dir.next(lv)!=null; lv=dir.next(lv)) {
            org.kohsuke.graph_layouter.impl.Level<T> nextLevel=dir.next(lv);

            // reorder next level by using the current level as fixed
            List<WeightedVertex<T>> orders = new ArrayList<WeightedVertex<T>>(nextLevel.vertices.size());
            for (Vertex<T> v : nextLevel.vertices) {
                assert v.order==orders.size();
                orders.add(new WeightedVertex<T>(orderingHeuristic.weight(v,lv,dir),v));
            }

            BitSet split = new BitSet(orders.size());
            List<WeightedVertex<T>> dontMoves = new ArrayList<WeightedVertex<T>>();
            {// keep 'DONT_MOVE' stuff on the side
                int j=0;
                for (Iterator<WeightedVertex<T>> itr = orders.iterator(); itr.hasNext();) {
                    WeightedVertex<T> o = itr.next();
                    split.set(j++,isDontMove(o));
                    if(isDontMove(o)) {
                        itr.remove();
                        dontMoves.add(o);
                    }
                }
                assert j==nextLevel.vertices.size();
            }

            // sort the other stuff.
            if(flipEqual)
                Collections.sort(orders,FLIP_EQUALS);
            else
                Collections.sort(orders);   // stable sort


            {// then merge it back to a single list
                List<WeightedVertex<T>> r = new ArrayList<WeightedVertex<T>>(nextLevel.vertices.size());
                Iterator<WeightedVertex<T>> itr = dontMoves.iterator();
                Iterator<WeightedVertex<T>> jtr = orders.iterator();
                for( int j=0; j<nextLevel.vertices.size(); j++ ) {
                    if(split.get(j)) {
                        r.add(itr.next());
                    } else {
                        r.add(jtr.next());
                    }
                }
                assert !itr.hasNext(); // should have stiched all of them
                assert !jtr.hasNext();
                assert r.size()==nextLevel.vertices.size();

                orders = r;
            }


            nextLevel.reorder(new WeightedVertex.Adapter<T>(orders));
        }
    }

    private static boolean isDontMove(WeightedVertex v) {
        return v.weight==OrderingHeuristic.DONT_MOVE;
    }

    /**
     * Try to swap adjacent nodes and see if that makes any improvement.
     */
    private <T> void transpose(LevelMap<T> lm, boolean flipEqual) {
        boolean improved;

        do {
            improved = false;

            for (org.kohsuke.graph_layouter.impl.Level<T> lv : lm.levels()) {
                int xing = lv.getAdjacentCrossings();

                for( int i=0; i<lv.vertices.size()-1; i++ ) {
                    Vertex<T> v = lv.vertices.get(i);
                    Vertex<T> w = lv.vertices.get(i+1);

                    int swapXing = lv.getAdjacentSwapCrossing(v, w);
                    if(swapXing<xing || (flipEqual && swapXing==xing)) {
                        // swapping two would achieve a better result
                        lv.swap(v,w);
                        improved |= swapXing<xing;
                        xing = swapXing;
                        assert xing==lv.getAdjacentCrossings();
                    }
                }
            }
        } while(improved);
    }

    private static final int MAX_ITERATION = 8;

    /**
     * Sort order that follows {@link WeightedVertex#weight} but reverses orders of the same weight.
     */
    private static final Comparator<WeightedVertex<?>> FLIP_EQUALS = new Comparator<WeightedVertex<?>>() {
        public int compare(WeightedVertex lhs, WeightedVertex rhs) {
            int r = lhs.compareTo(rhs);
            if(r!=0)    return r;

            if(lhs.v.order<rhs.v.order) return 1;
            if(lhs.v.order>rhs.v.order) return -1;
            return 0;
        }
    };
}
