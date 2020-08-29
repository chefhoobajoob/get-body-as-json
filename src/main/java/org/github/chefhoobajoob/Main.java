package org.github.chefhoobajoob;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;

public class Main extends AbstractVerticle {
    private String _serviceId;
    private String _clientId;
    @Override
    public void start( Promise<Void> start ) throws Exception {
        DeploymentOptions options = new DeploymentOptions().setConfig( config() );
        vertx.deployVerticle( Service.class.getCanonicalName(), options, deployService -> {
            if (deployService.failed()) {
                start.fail( deployService.cause() );
                return;
            }
            _serviceId = deployService.result();
            vertx.deployVerticle( Client.class.getCanonicalName(), options, deployClient -> {
                if (deployClient.failed()) {
                    start.fail( deployClient.cause() );
                    return;
                }
                _clientId = deployClient.result();
                start.complete();
            });
        });
    }

    @Override
    public void stop() throws Exception {
    }
}
