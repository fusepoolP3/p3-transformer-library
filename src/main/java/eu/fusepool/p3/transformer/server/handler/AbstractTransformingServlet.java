/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.fusepool.p3.transformer.server.handler;

import eu.fusepool.p3.vocab.TRANSFORMER;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;
import javax.activation.MimeType;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.clerezza.commons.rdf.Graph;
import org.apache.clerezza.commons.rdf.IRI;
import org.apache.clerezza.commons.rdf.impl.utils.simple.SimpleGraph;
import org.apache.clerezza.rdf.core.serializedform.Serializer;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.apache.clerezza.rdf.core.serializedform.UnsupportedFormatException;
import org.apache.clerezza.rdf.utils.GraphNode;


/**
 * This class provides the default service description page. Concrete 
 * implemetations provide the actual POST functionality. Normally there's no
 * need to subclass this class, rather typically an Transformer is written and
 * a Handler is created for it using the TransformerHandlerFactory.
 * 
 * @author reto
 */
public abstract class AbstractTransformingServlet extends HttpServlet {

    protected abstract Set<MimeType> getSupportedInputFormats();
    protected abstract Set<MimeType> getSupportedOutputFormats();
    
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        if (request.getMethod().equals("GET")) {
            handleGet(request, response);
            return;
        }
        if (request.getMethod().equals("POST")) {
            handlePost(request, response);
            return;
        }
        //TODO support at least HEAD
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    static String getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        if (request.getQueryString() != null) {
            requestURL.append("?").append(request.getQueryString());
        }
        return requestURL.toString();
    }

    protected void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final GraphNode node = getServiceNode(request);
        for (MimeType mimeType : getSupportedInputFormats()) {
            node.addPropertyValue(TRANSFORMER.supportedInputFormat, mimeType.toString());
        }
        for (MimeType mimeType : getSupportedOutputFormats()) {
            node.addPropertyValue(TRANSFORMER.supportedOutputFormat, mimeType.toString());
        }
        response.setStatus(HttpServletResponse.SC_OK);
        respondFromNode(response, node);
    }

    static void respondFromNode(HttpServletResponse response, final GraphNode node) throws IOException, UnsupportedFormatException {
        //TODO content negotiation
        final String responseFormat = SupportedFormat.TURTLE;
        response.setContentType(responseFormat);
        final ServletOutputStream outputStream = response.getOutputStream();
        try {
            Serializer.getInstance().serialize(outputStream, node.getGraph(), responseFormat);
        } catch (RuntimeException ex) {
            final PrintWriter printWriter = new PrintWriter(outputStream);
            ex.printStackTrace(printWriter);
            printWriter.flush();
        }
    }
    
    /**
     * Returns a GraphNode representing the requested resources in an empty Graph
     * @param request
     * @return a GraphNode representing the resource
     */
    static GraphNode getServiceNode(HttpServletRequest request) {
        final IRI serviceUri = new IRI(getFullRequestUrl(request));
        final Graph resultGraph = new SimpleGraph();
        return new GraphNode(serviceUri, resultGraph);
    }

    protected abstract void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;
}
