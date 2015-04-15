package de.cpg.shared.event_sourcing.configuration;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.uuid.Generators;
import de.cpg.shared.event_sourcing.service.CommandBus;
import de.cpg.shared.event_sourcing.service.CommandBusImpl;
import de.cpg.shared.event_sourcing.service.EventBus;
import de.cpg.shared.event_sourcing.service.EventBusImpl;
import eventstore.Settings;
import eventstore.UserCredentials;
import eventstore.j.EsConnection;
import eventstore.j.EsConnectionFactory;
import eventstore.j.SettingsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;

@Configuration
public class EventStore {

    @Value("${eventstore.username:admin}")
    private String username;

    @Value("${eventstore.password:changeit}")
    private String password;

    @Value("${eventstore.hostname:localhost}")
    private String hostname;

    @Value("${eventstore.port:1113}")
    private int port;

    @Bean
    public ActorSystem _actorSystem() {
        return ActorSystem.create();
    }

    @Bean
    public EsConnection _esConnection(final ActorSystem system) {
        final Settings settings = new SettingsBuilder()
                .address(new InetSocketAddress(hostname, port))
                .defaultCredentials(new UserCredentials(username, password))
                .build();

        return EsConnectionFactory.create(system, settings);
    }

    @Bean
    public CommandBus commandBus(final EsConnection esConnection) {
        return new CommandBusImpl(new ObjectMapper(), Generators.nameBasedGenerator(), esConnection);
    }

    @Bean
    public EventBus eventBus(final EsConnection esConnection, final ActorSystem actorSystem) {
        return new EventBusImpl(esConnection, actorSystem);
    }
}
