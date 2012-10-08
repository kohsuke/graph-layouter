package org.kohsuke.graph_layouter.impl;

import java.util.Iterator;
import java.util.Set;

/**
 * Two directions to scan {@link LevelMap}.
 *
 * @author Kohsuke Kawaguchi
 */
public enum LevelDirection {
    DOWN {
        public <T> Level<T> first(LevelMap<T> lm) {
            return lm.first();
        }
        public <T> Level<T> next(Level<T> l) {
            return l.next();
        }
        public <T> Set<Vertex<T>> forwardEdges(Vertex<T> v) {
            return v.forward;
        }
        public <T> Set<Vertex<T>> backwardEdges(Vertex<T> v) {
            return v.backward;
        }
        public LevelDirection opposite() {
            return UP;
        }
    },
    UP {
        public <T> Level<T> first(LevelMap<T> lm) {
            return lm.last();
        }
        public <T> Level<T> next(Level<T> l) {
            return l.prev();
        }
        public <T> Set<Vertex<T>> forwardEdges(Vertex<T> v) {
            return v.backward;
        }
        public <T> Set<Vertex<T>> backwardEdges(Vertex<T> v) {
            return v.forward;
        }
        public LevelDirection opposite() {
            return DOWN;
        }
    };

    /**
     * Gets the first level in the given map to be traversed.
     */
    public abstract <T> Level<T> first(LevelMap<T> lm);

    /**
     * Gets the next level to be traversed.
     */
    public abstract <T> Level<T> next(Level<T> l);

    /**
     * Gets the opposite direction.
     */
    public abstract LevelDirection opposite();

    /**
     * Gets the edges of the given vertex that goes to the 'next' level
     * (by the definition of the current traversal order.)
     */
    public abstract <T> Set<Vertex<T>> forwardEdges(Vertex<T> v);

    public abstract <T> Set<Vertex<T>> backwardEdges(Vertex<T> v);

    public <T> Iterable<Level<T>> scan(final LevelMap<T> lm) {
        return new Iterable<Level<T>>() {
            public Iterator<Level<T>> iterator() {
                return new Iterator<Level<T>>() {
                    Level<T> last = first(lm);
                    public boolean hasNext() {
                        return LevelDirection.this.next(last)!=null;
                    }

                    public Level<T> next() {
                        return last=LevelDirection.this.next(last);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
