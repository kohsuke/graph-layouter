package org.kohsuke.graph_layouter.impl;

import org.apache.commons.io.IOUtils;
import org.kohsuke.graph_layouter.Layout;
import org.kohsuke.graph_layouter.Navigator;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class Graph<T> extends LinkedHashSet<Vertex<T>> {
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
        area.grow(MARGIN,MARGIN);
        return area;
    }

    public void html(/*optional*/ Layout<Vertex<T>> layout, File file) throws IOException {
        PrintWriter html = new PrintWriter(file);
        html.printf("<html><head><style>");
        IOUtils.copy(new InputStreamReader(getClass().getResourceAsStream("style.css")),html);
        html.printf("</style><script>");
        IOUtils.copy(new InputStreamReader(getClass().getResourceAsStream("style.js")),html);
        html.printf("</script></head><body>");
        draw(layout,new File(file.getPath()+".png"));

        Rectangle bound = calcDrawingArea();

        html.printf("<div position='relative'>");
        html.printf("<img src='%s.png'/>", file.getName());
        for (Vertex<T> v : this) {
            Rectangle box = v.boundBox();
            html.printf("<div class='node' style='left:%d; top:%d; width:%d; height:%d' tag='%s'></div>",
                    box.x - bound.x, box.y - bound.y, box.width, box.height, v.tag);
        }
        html.print("</div><div id=name></div></body></html>");
        html.close();
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

    private static final int MARGIN = 25;
}
