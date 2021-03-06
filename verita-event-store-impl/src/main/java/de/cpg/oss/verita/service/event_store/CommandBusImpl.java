package de.cpg.oss.verita.service.event_store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.uuid.StringArgGenerator;
import de.cpg.oss.verita.command.Command;
import de.cpg.oss.verita.command.CommandHandler;
import de.cpg.oss.verita.service.CommandBus;
import eventstore.Event;
import eventstore.EventData;
import eventstore.SubscriptionObserver;
import eventstore.WriteResult;
import eventstore.j.EsConnection;
import eventstore.j.EventDataBuilder;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.Closeable;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class CommandBusImpl implements CommandBus {

    private final EventStoreObjectMapper objectMapper;
    private final StringArgGenerator uuidGenerator;
    private final EsConnection esConnection;

    public CommandBusImpl(final EventStoreObjectMapper objectMapper, final StringArgGenerator uuidGenerator, final EsConnection esConnection) {
        this.objectMapper = objectMapper;
        this.uuidGenerator = uuidGenerator;
        this.esConnection = esConnection;
    }

    @Override
    public Optional<UUID> publish(final Command command) {
        final String commandName = command.getClass().getSimpleName();
        final String jsonCommand;
        try {
            jsonCommand = objectMapper.writeValueAsString(command);
        } catch (final JsonProcessingException e) {
            log.error("Could not convert " + commandName + " to JSON", e);
            return Optional.empty();
        }

        final UUID commandId = command.uniqueKey()
                .map((uniqueKey) -> uuidGenerator.generate(commandName.concat("-").concat(uniqueKey)))
                .orElse(UUID.randomUUID());
        log.debug("Generated UUID {} for {}", commandId, commandName);
        final EventData eventData = new EventDataBuilder(commandName)
                .jsonData(jsonCommand)
                .eventId(commandId).build();

        final Future<WriteResult> future = esConnection.writeEvents(queueNameFor(command.getClass()), null,
                Collections.singleton(eventData), null);
        try {
            final WriteResult result = Await.result(future, Duration.Inf());
            return null != result ? Optional.of(commandId) : Optional.empty();
        } catch (final Exception e) {
            log.error("Unhandled exception on waiting for result", e);
            return Optional.empty();
        }
    }

    @Override
    public Closeable subscribeTo(final CommandHandler<? extends Command> handler) {
        return esConnection.subscribeToStream(
                queueNameFor(handler.commandClass()),
                asObserver(handler),
                false,
                null);
    }

    @Override
    public boolean deleteQueueFor(final Class<? extends Command> commandClass) {
        try {
            Await.result(esConnection.deleteStream(queueNameFor(commandClass), null, false, null), Duration.Inf());
            return true;
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private SubscriptionObserver<Event> asObserver(final CommandHandler handler) {
        return new SubscriptionObserver<Event>() {
            @Override
            public void onLiveProcessingStart(final Closeable closeable) {
            }

            @Override
            public void onEvent(final Event event, final Closeable closeable) {
                try {
                    final Command command = (Command) objectMapper.readValue(
                            event.data().data().value().utf8String(),
                            handler.commandClass());
                    handler.handle(command, event.data().eventId(), event.number().value());
                } catch (final Exception e) {
                    onError(e);
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                handler.onError(throwable);
            }

            @Override
            public void onClose() {
            }
        };
    }

    private static String queueNameFor(final Class<? extends Command> commandClass) {
        return commandClass.getSimpleName().concat("Queue");
    }
}
