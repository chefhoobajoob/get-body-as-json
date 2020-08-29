package org.github.chefhoobajoob;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class Service extends AbstractVerticle {
    HttpServer _server;

    @Override
    public void start( Promise<Void> start ) throws Exception {
        System.out.println(">>> service: starting verticle");
        _server = vertx.createHttpServer().requestHandler( router() );
        _server.listen( config().getInteger("port", 8081), request -> {
            if (request.failed()) {
                start.fail(request.cause());
                return;
            }
            System.out.println(">>> service: now listening on port " + request.result().actualPort() );
            start.complete();
        } );
    }

    @Override
    public void stop( Promise<Void> stop ) throws Exception {
        _server.close(result -> {
            stop.complete();
            System.out.println(">>> service: done listening for http requests" );
        });
    }

    private Router router() {
        Router router =  Router.router( vertx );
        router .route().handler( BodyHandler.create() );
        router.route( HttpMethod.POST, "/resources" )
        .consumes( "application/json" )
        .produces( "application/json" )
        .handler( context -> {
            System.out.println(">>> service: getting body" + (config().getBoolean("getBodyAsJson", true) ? " as json" : " as string"));
            try {
                JsonObject body = config().getBoolean( "getBodyAsJson", true )
                        ? context.getBodyAsJson()
                        : new JsonObject( context.getBodyAsString() );
                System.out.println(">>> service: done getting body");
                context.response()
                       .setStatusCode( HttpResponseStatus.OK.code() )
                       .setStatusMessage( HttpResponseStatus.OK.reasonPhrase() )
                       .end( body.encode() );
            } catch ( Throwable t ) {
                System.out.println(">>> service: failed getting body: " + t.getClass().getName() + ": " + t.getMessage() );
                context.response()
                       .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                       .setStatusMessage( HttpResponseStatus.BAD_REQUEST.reasonPhrase())
                       .end();
            }
        });
        return router;
    }
}
