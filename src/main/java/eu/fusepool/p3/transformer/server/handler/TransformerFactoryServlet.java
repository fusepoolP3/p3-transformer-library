/*
 * Copyright 2014 reto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fusepool.p3.transformer.server.handler;

import eu.fusepool.p3.transformer.AsyncTransformer;
import eu.fusepool.p3.transformer.LongRunningTransformerWrapper;
import eu.fusepool.p3.transformer.SyncTransformer;
import eu.fusepool.p3.transformer.Transformer;
import eu.fusepool.p3.transformer.TransformerException;
import eu.fusepool.p3.transformer.TransformerFactory;
import static eu.fusepool.p3.transformer.server.handler.TransformerServlet.writeResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author reto
 */
public class TransformerFactoryServlet extends HttpServlet {

    private final TransformerFactory factory;
    private ASyncResponsesManager aSyncResponsesManager = new ASyncResponsesManager();
    private final Map<String, AsyncTransformer> requestId2Transformer = new HashMap<>();
    private final Set<AsyncTransformer> aSyncTransformerSet = Collections.newSetFromMap(
            new WeakHashMap<AsyncTransformer, Boolean>());

    public TransformerFactoryServlet(TransformerFactory factory) {
        this.factory = factory;
    }

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

    public void handlePost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        final Transformer transformer;
        try {
            transformer = wrapLongRunning(getTransformer(request, response));
        } catch (TransformerException e) {
            response.setStatus(e.getStatusCode());
            writeResponse(e.getResponseEntity(), response);
            return;
        }
        if (transformer == null) {
            response.sendError(404);
        } else {
            if (transformer instanceof SyncTransformer) {
                new SyncTransformerServlet((SyncTransformer) transformer).handlePost(request, response);
            } else {
                final AsyncTransformer aSyncTransformer = (AsyncTransformer) transformer;
                synchronized (aSyncTransformerSet) {
                    if (!aSyncTransformerSet.contains(aSyncTransformer)) {
                        aSyncTransformerSet.add(aSyncTransformer);
                        aSyncTransformer.activate(aSyncResponsesManager);
                    }
                }
                final String requestId = aSyncResponsesManager.handlePost(request, response, aSyncTransformer);
                requestId2Transformer.put(requestId, aSyncTransformer);
            }
        }
    }

    private void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final String requestUri = request.getRequestURI();
        if (requestUri.startsWith(ASyncResponsesManager.JOB_URI_PREFIX)) {
            final AsyncTransformer transformer = requestId2Transformer.get(requestUri);
            if (transformer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                if (!aSyncResponsesManager.handleJobRequest(request, response, transformer)) {
                    requestId2Transformer.remove(requestUri);
                }
            }
        } else {
            final Transformer transformer;
            try {
                transformer = getTransformer(request, response);
            } catch (TransformerException e) {
                response.setStatus(e.getStatusCode());
                writeResponse(e.getResponseEntity(), response);
                return;
            }
            if (transformer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                AbstractTransformingServlet handler = TransformerHandlerFactory.getTransformerHandler(transformer);
                handler.handleGet(request, response);
            }
        }
    }

    private Transformer getTransformer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return factory.getTransformer(request);
    }

    private Transformer wrapLongRunning(Transformer transformer) {
        if (transformer instanceof SyncTransformer) {
            SyncTransformer syncTransformer = (SyncTransformer) transformer;
            if (syncTransformer.isLongRunning()) {
                return new LongRunningTransformerWrapper(syncTransformer);
            }
        }
        return transformer;
    }

}
