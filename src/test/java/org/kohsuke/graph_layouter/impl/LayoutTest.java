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
        g.html(layout(g),new File("unixFamilyTree.html"));
    }

    public void testGraphVizPluginForConfluence() throws Exception {
        Graph<String> g = parseDot("graphVizPluginForConfluence.dot");
        g.html(layout(g),new File("graphVizPluginForConfluence.html"));
    }

    public void testTraceRoute() throws Exception {
        Graph<String> g = parseDot("traceroute.dot");
        g.html(layout(g),new File("traceroute.html"));
    }

    // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testAllaFieraDellEst() throws Exception {
        Graph<String> g = parseDot("alla_fiera_dell_est.dot");
        g.html(layout(g),new File("alla_fiera_dell_est.html"));
    }

    // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testAqueousTransmission() throws Exception {
        Graph<String> g = parseDot("aqueousTransmission.dot");
        g.html(layout(g),new File("aqueousTransmission.html"));
    }

        // from http://www.fantascienza.net/leonardo/so/lyrics_graph/lyrics_graph.html
    public void testBalloInFaDiesisMinore() throws Exception {
        Graph<String> g = parseDot("Ballo in Fa diesis minore.dot");
        g.html(layout(g),new File("Ballo in Fa diesis minore.html"));
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
