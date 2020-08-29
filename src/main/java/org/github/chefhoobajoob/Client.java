package org.github.chefhoobajoob;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;

public class Client extends AbstractVerticle {
    private WebClient _client;

    @Override
    public void start( Promise<Void> start ) throws Exception {
        System.out.println(">>> client: starting verticle");
        String url = "http://localhost:" + config().getInteger( "port", 8081 ) + "/resources";
        _client = WebClient.create( vertx );

        System.out.println(">>> client: loading all-nines.txt");
        vertx.fileSystem()
        .readFile( "./src/main/resources/all-nines.txt", getFile -> {
            if (getFile.failed()) {
                start.fail( getFile.cause() );
                return;
            }

            System.out.println(">>> client: issuing POST to: " + url );
            _client.postAbs(url)
            .putHeader( "Accept-Encoding", "gzip, deflate")
            .putHeader( "Connection", "close" )
            .putHeader( "Cache-Control", "no-cache" )
            .putHeader( "Content-Type", "application/json" )
            .putHeader( "Accept", "application/json" )
            .timeout( 5*1000L )
            .sendBuffer( getFile.result(), postBuffer -> {
                if (postBuffer.failed()) {
                    System.out.println(">>> client: couldn't issue POST request: " + postBuffer.cause().getClass().getCanonicalName() + ": " + postBuffer.cause().getMessage());
                    start.fail( postBuffer.cause() );
                    return;
                }
                if (postBuffer.result().statusCode() != 400) {
                    System.out.println(">>> client: expected POST to fail with a status code of 400, but received: " + postBuffer.result().statusCode());
                    start.fail(new Exception("Unexpected service response"));
                }
                System.out.println(">>> client: received expected 400 response");
                start.complete();
            });
        });
    }

    @Override
    public void stop( Promise<Void> stop ) throws Exception {
        _client.close();
        System.out.println(">>> client: done issuing http requests" );
    }
}
