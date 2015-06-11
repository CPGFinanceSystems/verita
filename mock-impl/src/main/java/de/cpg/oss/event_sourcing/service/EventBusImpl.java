package de.cpg.oss.event_sourcing.service;

import de.cpg.oss.event_sourcing.domain.AggregateRoot;
import de.cpg.oss.event_sourcing.event.Event;
import de.cpg.oss.event_sourcing.event.EventHandler;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventBusImpl implements EventBus {

    private final Map<String, EventHandler> subscriptions;
    private final Map<String, List<Event>> eventStreams;
    private final Map<String, List<Event>> domainStreams;
    private final Map<String, Integer> offsets;

    public EventBusImpl() {
        subscriptions = new ConcurrentHashMap<>();
        eventStreams = new ConcurrentHashMap<>();
        domainStreams = new ConcurrentHashMap<>();
        offsets = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<UUID> publish(Event event, AggregateRoot aggregateRoot) {
        final UUID eventId = UUID.randomUUID();
        final String key = event.getClass().getSimpleName();

        eventStreams.putIfAbsent(key, new ArrayList<>());
        offsets.putIfAbsent(key, -1);

        final List<Event> eventStream = eventStreams.get(key);
        final List<Event> domainStream = domainStreamOf(aggregateRoot.getClass());
        int sequenceNumber = eventStream.size();

        eventStream.add(event);
        domainStream.add(event);

        if (offsets.get(key) < eventStream.size()) {
            subscriptions.entrySet().stream()
                    .filter((set) -> key.equals(set.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .ifPresent((eventHandler) -> {
                        try {
                            eventHandler.handle(event, eventId, sequenceNumber);
                        } catch (Exception e) {
                            eventHandler.onError(e);
                        }
                    });
        }
        return Optional.of(eventId);
    }

    @Override
    public <T extends Event> Closeable subscribeTo(final Class<T> eventClass, final EventHandler<T> handler) {
        return subscribeToStartingFrom(eventClass, handler, 0);
    }

    @Override
    public <T extends Event> Closeable subscribeToStartingFrom(final Class<T> eventClass, final EventHandler<T> handler, int sequenceNumber) {
        final String key = eventClass.getSimpleName();

        subscriptions.put(key, handler);
        offsets.put(key, sequenceNumber);

        Optional.ofNullable(eventStreams.get(key)).ifPresent((stream) -> {
            int counter = 0;
            for (final Event event : stream) {
                if (counter > sequenceNumber) {
                    try {
                        handler.handle((T) event, UUID.randomUUID(), counter);
                    } catch (Exception e) {
                        handler.onError(e);
                    }
                }
                counter++;
            }
        });
        return () -> subscriptions.remove(eventClass.getSimpleName());
    }

    public <T extends AggregateRoot> List<Event> domainStreamOf(final Class<T> aggregateRootClass) {
        final String key = aggregateRootClass.getSimpleName();
        domainStreams.putIfAbsent(key, new ArrayList<>());
        return domainStreams.get(key);
    }
}