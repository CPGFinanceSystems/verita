package de.cpg.shared.event_sourcing.service;

import com.google.protobuf.MessageLite;
import de.cpg.shared.event_sourcing.domain.AggregateRoot;
import de.cpg.shared.event_sourcing.event.EventHandler;

import java.io.Closeable;
import java.util.Optional;
import java.util.UUID;

public interface EventBus {

    Optional<UUID> publish(MessageLite event, AggregateRoot aggregateRoot);

    <T extends MessageLite> Closeable subscribeTo(Class<T> eventClass, EventHandler<T> handler);

    <T extends MessageLite> Closeable subscribeToStartingFrom(Class<T> eventClass, EventHandler<T> handler, int sequenceNumber);
}
