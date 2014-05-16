/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.fusepool.sample.extractor;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.clerezza.rdf.core.BNode;
import org.apache.clerezza.rdf.core.TripleCollection;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.impl.SimpleMGraph;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.ontologies.SIOC;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.commons.io.IOUtils;


public class SimpleExtractor extends RdfGeneratingExtractor {

    @Override
    protected Set<MimeType> getSupportedInputFormats() {
        try {
            MimeType mimeType = new MimeType("text/plain;charset=UTF-8");
            return Collections.singleton(mimeType);
        } catch (MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected TripleCollection generateRdf(HttpServletRequest request) throws IOException, ServletException {
        final Reader in = request.getReader();
        final String text = IOUtils.toString(in);
        final TripleCollection result = new SimpleMGraph();
        final GraphNode node = new GraphNode(new BNode(), result);
        node.addProperty(RDF.type, new UriRef("http://example.org/ontology#TextDescription"));
        node.addPropertyValue(SIOC.content, text);
        node.addPropertyValue(new UriRef("http://example.org/ontology#textLength"), text.length());
        return result;
    }
    
}
