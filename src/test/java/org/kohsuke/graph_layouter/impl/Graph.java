package org.kohsuke.graph_layouter.impl;

import org.kohsuke.graph_layouter.Layout;
import org.kohsuke.graph_layouter.Navigator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class Graph<T> extends HashSet<Vertex<T>> {
    private final Map<T,Vertex<T>> vertices = new HashMap<T, Vertex<T>>();

    Vertex makeVertex(T tag) {
        Vertex<T> v = vertices.get(tag);
        if (v==null) {
            v = new Vertex<T>(tag,new Dimension(10,10));
            vertices.put(tag,v);
            add(v);
        }
        return v;
    }

    /**
     * Computes the size needed to hold all figures.
     */
    public Rectangle calcDrawingArea() {
        Rectangle area = null;
        for (Vertex<T> v : this) {
            Rectangle vr = v.boundBox();
            if(area==null)  area=vr;
            else            area.add(vr);
        }

        if(area==null)  area = new Rectangle();
        return area;
    }

    public void draw(/*optional*/ Layout<Vertex<T>> layout, File file) throws IOException {
        Rectangle area = calcDrawingArea();
        BufferedImage image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB );
        Graphics2D g2 = image.createGraphics();
        g2.setTransform(AffineTransform.getTranslateInstance(-area.x,-area.y));
        g2.setPaint(Color.WHITE);
        g2.fill(area);

        g2.setPaint(Color.BLACK);
        for (Vertex<T> v : this) {
            g2.fill(v.boundBox());
            for (Vertex<T> w : v.forward) {
                Point p = v.pos;
                if (layout!=null) {
                    for (Point waypoint : layout.edge(v, w)) {
                        g2.drawLine(p.x,p.y, waypoint.x, waypoint.y);
                        p = waypoint;
                    }
                }

                g2.drawLine(p.x,p.y, w.pos.x, w.pos.y);
            }
        }

        g2.dispose();

        FileOutputStream fos = new FileOutputStream(file);
        try {
            ImageIO.write(image, "PNG", fos);
        } finally {
            fos.close();
        }
    }

    public Navigator<Vertex<T>> makeNavigator() {
        return new Navigator<Vertex<T>>() {
            public Collection<Vertex<T>> vertices() {
                return Graph.this;
            }

            public Collection<Vertex<T>> edge(Vertex<T> t) {
                return t.forward;
            }

            public Dimension getSize(Vertex<T> t) {
                return t.size;
            }
        };
    }
}
