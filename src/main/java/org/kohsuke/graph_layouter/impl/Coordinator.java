package org.kohsuke.graph_layouter.impl;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.AbstractList;
import java.util.Set;
import java.util.logging.*;

/**
 * Assigns (X,Y) coordinates based on STT81.
 *
 * <p>
 * This algorithm is rather dumb, so may not result in the optimal layout.
 * For example, given the following graph, and when we are going down,
 * it will end up placing either K or J directly under H, and displace the other on the
 * side. So it can never put H between K and J.
 * <pre>
 * G I H
 * |  /|
 * L K J
 * </pre>
 * 
 * @author Kohsuke Kawaguchi
 */
public class Coordinator {
    /**
     * Minimum space required between {@link Vertex}s.
     */
    private final int xGap = 10;

    private final int yGap = 10;

    public <T> void layout( LevelMap<T> lm ) {
        initial(lm);

        if(LOGGER.isLoggable(FINE)) {
            LOGGER.fine("Initial cost="+objective(lm));
            LOGGER.fine("Graph=\n"+lm);
        }

        // at least run a cerain number of times
        LevelDirection dir = LevelDirection.DOWN;
        for( int i=0; i<MAX_ITERATION; i++,dir=dir.opposite() ) {
            move(dir, lm);

            if(LOGGER.isLoggable(FINE)) {
                LOGGER.fine("After "+i+"th iteration, cost="+objective(lm));
                LOGGER.fine("Graph=\n"+lm);
            }
        }

        // repeat while we are still improving
        while(true) {
            long before = objective(lm);
            move(LevelDirection.DOWN,lm);
            move(LevelDirection.UP,lm);
            long after = objective(lm);

            if(LOGGER.isLoggable(FINE)) {
                LOGGER.fine("cost: "+ before +"=>"+ after);
                LOGGER.fine("Graph=\n"+lm);
            }                

            if(after>=before) {
                return;
            }
        }
    }

    /**
     * Scans the whole levels.
     */
    private <T> void move(LevelDirection dir, LevelMap<T> lm) {
        for(Level<T> lv=dir.first(lm); dir.next(lv)!=null; lv=dir.next(lv)) {
            move(lv, dir.next(lv),dir);
        }
    }

    /**
     * Moves the vertices in the 'next' level by considering the verticies in the 'fixed' level.
     */
    private <T> void move(Level<T> fixed, Level<T> next, LevelDirection dir) {
        StringBuilder buf = new StringBuilder();    // log message
        if(LOGGER.isLoggable(FINER))
            buf.append("fixed=").append(fixed).append(',').append("next=").append(next);


        // use the barycenter
        List<WeightedVertex<T>> plist = new ArrayList<WeightedVertex<T>>();
        for (Vertex<T> v : next.vertices) {
            plist.add(new WeightedVertex<T>(getPriority(v, dir, fixed),v));
        }

        // sort in the priority order from high to low
        Collections.sort(plist, Collections.reverseOrder());

        // consider moving vertex to its preferred position
        for (WeightedVertex<T> v : plist) {
            int bc = getBarycenter(v.v,dir);
            int xpos = v.v.pos.x;

            if(bc==NO_BARYCENTER)
                continue;   // no guidance. leave it as is
            if(bc==xpos)
                continue;   // ideal

            if(bc>xpos) {
                // shift to right
                int amount = Math.min( bc-xpos, findSlack(v.weight,
                    reverse(next.vertices.subList(v.v.order,plist.size())),dir,fixed) );
                shiftRight(next.vertices.subList(v.v.order, plist.size()),amount);
            } else {
                // shift to left
                int amount = Math.min( xpos-bc, findSlack(v.weight,
                    next.vertices.subList(0,v.v.order+1),dir,fixed));
                shiftLeft(next.vertices.subList(0,v.v.order+1),amount);
            }
        }

        if(LOGGER.isLoggable(FINER)) {
            buf.append("=>").append(next);
            LOGGER.finer(buf.toString());
        }
    }

    /**
     * Shifts verticies to right by the given width.
     *
     * <p>
     * The way to visualize this is that you have:
     * <pre>
     * o o o o o o o o o o o
     * </pre>
     * ... and we are moving 'o's to the right.
     * <pre>
     *     ooooo o o o o o o
     * </pre>
     */
    private <T> void shiftRight(List<Vertex<T>> vertices, int amount) {
        if(vertices.isEmpty())  return; // nothing to do

        int sz = vertices.size();
        int x = vertices.get(0).pos.x+amount;
        for (int i=0; i< sz; i++) {
            Vertex<T> v = vertices.get(i);

            if(x<=v.pos.x) return;     // no need to shift any further

            v.pos.x = x;  // shift but just enough

            if(i+1<sz) {
                Vertex<T> w =vertices.get(i+1);
                x += v.size.width/2+xGap+w.size.width/2;
            }
        }
    }

    private <T> void shiftLeft(List<Vertex<T>> vertices, int amount) {
        if(vertices.isEmpty())  return; // nothing to do

        int sz = vertices.size();
        int x = vertices.get(sz-1).pos.x-amount;
        for (int i=sz-1; i>=0; i--) {
            Vertex<T> v = vertices.get(i);

            if(x>=v.pos.x) return;     // no need to shift any further

            v.pos.x = x;  // shift but just enough

            if(i>0) {
                Vertex<T> w =vertices.get(i-1);
                x -= v.size.width/2+xGap+w.size.width/2;
            }
        }
    }

    /**
     * Computes the amount of slack we have in the given vertices from left to right.
     *
     * <p>
     * The way to visualize this is that you have:
     * <pre>
     * |&lt;-- vertices -->|
     * o o o o o o o o o o o
     *
     * becomes
     *
     * o o o o o o ooooo     &lt;-- shifted
     * </pre>
     * ... and we are trying to determine how much those 'o's can be shited to the left.
     * This is subject to the constraint that only 'o's with lesser priority can be
     * moved. 
     */
    private <T> int findSlack(float weight, List<Vertex<T>> vertices, LevelDirection dir, Level<T> fixed) {
        assert !vertices.isEmpty();
        
        final Vertex<T> tail = vertices.get(vertices.size()-1);

        int width = Integer.MAX_VALUE;
        Vertex<T> last = null;
        for (Vertex<T> w : vertices) {
            if(getPriority(w,dir,fixed)>=weight && w!=tail) {
                width = 0; // can't move this node
            } else {
                if(last!=null) {
                    width = Math.max(width,width+findSlack(last,w));
                }
            }

            last = w;
        }

        return width;
    }

    /**
     * Finds the slack between two adjacent nodes (v,w)
     */
    private <T> int findSlack(Vertex<T> w, Vertex<T> v) {
        assert Math.abs(v.order-w.order)==1; // must be adjacent

        int dist = Math.abs(w.pos.x-v.pos.x);  // current distance
        // subtract the required space, and we get the slack between (last,w)
        dist -= v.size.width/2 + w.size.width/2 + xGap;
        assert dist>=0;
        return dist;
    }

    /**
     * Computes the barycenter X position of the given vertex.
     *
     * @param dir
     *      if {@link LevelDirection#DOWN} then it computes the up barycenter,
     *      that is the barycenter position of 'v' against vertices above it.
     * @return
     *      {@link #NO_BARYCENTER} if there's no barycenter position (no edges.)
     */
    private <T> int getBarycenter(Vertex<T> v, LevelDirection dir) {
        int sumx = 0;
        Set<Vertex<T>> be = dir.backwardEdges(v);

        if(be.isEmpty())    return NO_BARYCENTER;

        for (Vertex<T> w : be)
            sumx += w.pos.x;

        return sumx / be.size();
    }

    /**
     * Computes the 'priority' of the vertex, which is the number of connections
     * for normal vertices, and a very high value for a dummy node &lt;-> dummy node
     * connection. 
     */
    private <T> int getPriority(Vertex<T> v, LevelDirection dir, Level<T> fixed) {
        if(v.isDummy() && dir.backwardEdges(v).iterator().next().isDummy()) {
            // this is a span of a longer edge. Give it the higest priority.
            return fixed.vertices.size()+1;
        } else {
            return dir.backwardEdges(v).size();
        }
    }

    /**
     * Objective function that we are optimizing against.
     */
    private <T> long objective(LevelMap<T> lm) {
        long closeness = 0; // how far apart edges are?
        long balanced = 0; // how close nodes are to their barycenter positions?

        for(Level<T> lv=lm.first(); lv.next()!=null; lv=lv.next()) {
            for (Vertex<T> v : lv.vertices) {
                for (Vertex<T> w : v.forward) {
                    closeness += pow(v.pos.x-w.pos.x);
                }

                if(v.backward.size()>1) {
                    int bc = getBarycenter(v,LevelDirection.DOWN);
                    assert bc!=NO_BARYCENTER;
                    balanced += pow(bc-v.pos.x);
                }
                if(v.forward.size()>1) {
                    int bc = getBarycenter(v,LevelDirection.UP);
                    assert bc!=NO_BARYCENTER;
                    balanced += pow(bc-v.pos.x);
                }
            }
        }

        return closeness+balanced;  // 1:1 mix
    }

    private int pow(int x) {
        return x*x;
    }

    /**
     * Assigns initial coordinates.
     */
    private <T> void initial(LevelMap<T> lm) {
        int y = 0;
        for (Level<T> lv : lm.levels()) {
            int x = 0;
            int ysz=0;      // compute the height
            for (Vertex<T> v : lv.vertices) {
                ysz = Math.max(ysz,v.size.height);

                v.pos.y = y;
                v.pos.x = x + v.size.width/2;

                x+= v.size.width+xGap;
            }

            y += ysz+yGap;
        }
    }

    /**
     * Creates a view of the list that returns items in the reverse order.
     */
    private <T> List<T> reverse(final List<T> l) {
        return new AbstractList<T>() {
            final int sz = l.size();

            public T get(int index) {
                return l.get(sz-index-1);
            }

            public int size() {
                return sz;
            }
        };
    }

    private static final int MAX_ITERATION = 8;
    private static final int NO_BARYCENTER = Integer.MIN_VALUE;

    private static final Logger LOGGER = Logger.getLogger(Coordinator.class.getName());
}
