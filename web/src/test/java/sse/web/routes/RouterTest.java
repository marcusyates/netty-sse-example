package sse.web.routes;

import io.netty.handler.codec.http.HttpMethod;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static sse.web.routes.Router.NOT_FOUND_REQUEST_HANDLER;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class RouterTest {
    @Test
    public void shouldHandleUnknown() {
        final Router router = Router.builder().build();
        final RequestHandler route = router.route(HttpMethod.GET, "/");
        assertThat(route, is(NOT_FOUND_REQUEST_HANDLER));
    }

    @Test
    public void shouldHandleKnownGetRootUrl() {
        final RequestHandler doNothingRequestHandler = (ctx, request) -> request.toString();

        final Router router = Router
                .builder()
                .get("/", doNothingRequestHandler)
                .build();

        final RequestHandler route = router.route(HttpMethod.GET, "/");

        assertThat(route, is(doNothingRequestHandler));
    }

    @Test
    public void shouldHandleKnownGetUrl() {
        final RequestHandler handler1 = (ctx, request) -> request.toString();
        final RequestHandler handler2 = (ctx, request) -> request.toString();

        final Router router = Router
                .builder()
                .get("/", handler1)
                .get("/sausages", handler2)
                .build();

        final RequestHandler route = router.route(HttpMethod.GET, "/sausages/");

        assertThat(route, is(handler2));
    }
}