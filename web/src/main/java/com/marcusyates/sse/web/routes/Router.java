package com.marcusyates.sse.web.routes;

import io.netty.handler.codec.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.POST;

public class Router {
    public static final RequestHandler NOT_FOUND_REQUEST_HANDLER = new NotFoundRequestHandler();
    private final Routes routes;

    public static RouterBuilder builder() {
        return new RouterBuilder();
    }

    private Router(Routes routes) {
        this.routes = routes;
    }

    public RequestHandler route(HttpMethod method, String uri) {
        // see also https://tools.ietf.org/html/rfc6570
        final RequestHandler handler = routes.lookup(method, uri);
        return handler == null ? NOT_FOUND_REQUEST_HANDLER : handler;
    }

    public static class RouterBuilder {
        private final Routes routes = new Routes();

        public RouterBuilder get(String path, RequestHandler handler) {
            return add(path, handler, GET);
        }

        public RouterBuilder post(String path, RequestHandler handler) {
            return add(path, handler, POST);
        }

        public Router build() {
            return new Router(routes);
        }

        private RouterBuilder add(String path, RequestHandler handler, HttpMethod method) {
            routes.add(path, handler, method);
            return this;
        }
    }

    private final static class Routes {
        private final Map<HttpMethod, Map<String, RequestHandler>> routeMap = new HashMap<>();

        public RequestHandler lookup(final HttpMethod method, final String uri) {
            final Map<String, RequestHandler> uriToHandler = routeMap.get(method);
            return uriToHandler == null ? null : uriToHandler.get(sanitise(uri));
        }

        public void add(final String path, final RequestHandler handler, final HttpMethod method) {
            routeMap.computeIfAbsent(method, k -> new HashMap<>()).put(sanitise(path), handler);
        }

        private String sanitise(final String path) {
            final String s = path.trim();
            return (s.length() > 1 && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
        }
    }
}
