package com.mehmetnuri.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.consul.*;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.consul.ConsulClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class ApplicationConsulBean {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(ApplicationConsulBean.class);
    private String instanceId;
    private ConsulClient consulClient;

    @ConfigProperty(name = "quarkus.application.name")
    String appName;

    @ConfigProperty(name = "consul-host")
    String consulHost;

    @ConfigProperty(name = "consul-port")
    int consulPort;

    void onStart(@Observes StartupEvent ev, Vertx vertx) {

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        consulClient = ConsulClient.create(vertx, new ConsulClientOptions().setHost(consulHost).setPort(consulPort));

        executorService.schedule(() -> {

            ServiceEntryList serviceEntryList = consulClient.healthServiceNodesAndAwait(appName, false);

            instanceId = appName + "-" + serviceEntryList.getList().size();

            consulClient.registerServiceAndAwait(
                    new ServiceOptions()
                            .setPort(Integer.parseInt(System.getProperty("quarkus.http.port")))
                            .setAddress("localhost")
                            .setName(appName)
                            .setId(instanceId));

            LOGGER.info("Instance registered: id={}", instanceId);

        }, 5000, TimeUnit.MILLISECONDS);

    }

    void onStop(@Observes ShutdownEvent ev) {
        consulClient.deregisterServiceAndAwait(instanceId);
        LOGGER.info("Instance degistered: id={}", instanceId);

    }

}
