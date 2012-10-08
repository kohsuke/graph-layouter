package org.kohsuke.graph_layouter.impl;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;

/**
 * @author Kohsuke Kawaguchi
 */
public class Graph<T> extends HashSet<Vertex<T>> {
    Vertex makeVertex(T tag) {
        Vertex<T> v = new Vertex<T>(tag,new Dimension(10,10));
        add(v);
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

    public void draw(File file) throws IOException {
        Rectangle area = calcDrawingArea();
        BufferedImage image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB );
        Graphics2D g2 = image.createGraphics();
        g2.setTransform(AffineTransform.getTranslateInstance(-area.x,-area.y));
        g2.setPaint(Color.WHITE);
        g2.fill(area);

        g2.setPaint(Color.BLACK);
        for (Vertex<T> v : this) {
            g2.fill(v.boundBox());
            for (Vertex<T> w : v.forward)
                g2.drawLine(v.pos. x,v.pos.y, w.pos.x, w.pos.y);
        }

        g2.dispose();

        FileOutputStream fos = new FileOutputStream(file);
        try {
            ImageIO.write(image, "PNG", fos);
        } finally {
            fos.close();
        }
    }
}
