package org.kohsuke.graph_layouter.impl;

import org.kohsuke.graph_layouter.Direction;
import org.kohsuke.graph_layouter.Layout;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author Kohsuke Kawaguchi
 */
public class LayoutTest extends GraphTestBase {
    public void testUnixFamilyTree() throws Exception {
        Graph<String> g = parseDot("unixFamilyTree.dot");
        g.draw(layout(g),new File("unixFamilyTree.png"));
    }

    public void testGraphVizPluginForConfluence() throws Exception {
        Graph<String> g = parseDot("graphVizPluginForConfluence.dot");
        g.draw(layout(g),new File("graphVizPluginForConfluence.png"));
    }

    public void testTraceRoute() throws Exception {
        Graph<String> g = parseDot("traceroute.dot");
        g.draw(layout(g),new File("traceroute.png"));
    }

    // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testAllaFieraDellEst() throws Exception {
        Graph<String> g = parseDot("alla_fiera_dell_est.dot");
        g.draw(layout(g),new File("alla_fiera_dell_est.png"));
    }

    // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testAqueousTransmission() throws Exception {
        Graph<String> g = parseDot("aqueousTransmission.dot");
        g.draw(layout(g),new File("aqueousTransmission.png"));
    }

        // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testBalloInFaDiesisMinore() throws Exception {
        Graph<String> g = parseDot("Ballo in Fa diesis minore.dot");
        g.draw(layout(g),new File("Ballo in Fa diesis minore.png"));
    }

    private Layout<Vertex<String>> layout(Graph<String> g) {
        Layout<Vertex<String>> layout = new Layout<Vertex<String>>(g.makeNavigator(), Direction.TOPDOWN);
        for (Vertex<String> v : g) {
            v.pos.setLocation(
                    layout.vertex(v).getCenterX(),
                    layout.vertex(v).getCenterY());
        }
        return layout;
    }

    /**
     * Fake simplified *.dot parser.
     */
    private Graph<String> parseDot(String resName) throws Exception {
        Graph<String> g = new Graph<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resName)));
        String line;
        while ((line=r.readLine())!=null) {
            if (!line.contains("->"))   continue;

            Vertex<String> previous = null;
            for (String token : line.split("->")) {
                token = token.trim();
                if (token.endsWith(";"))    token=token.substring(0,token.length()-1);
                token = token.trim();

                Vertex v = g.makeVertex(unquote(token));
                if (previous!=null)
                    previous.addEdge(v);
                previous = v;
            }
        }
        return g;
    }

    private String unquote(String s) {
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1,s.length()-1);
        return s;
    }
}
