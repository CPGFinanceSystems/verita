package de.cpg.oss.verita.event;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a persistent immutable event within the system - A business fact which happened in the past.
 * The events are processed by the {@link de.cpg.oss.verita.service.EventBus} and handled by their respective
 * {@link EventHandler}s.
 * An event is something which the system must be able to process from a business point of view since it represents
 * something which happened already in the past. Events can only be 'deleted' or 'undone' if another event with the
 * logical reverse operation is applied (for example this could be a <code>UserDeletedEvent</code>).
 */
public abstract class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Supply a unique logical identifier - for example in a <code>CreatedUserEvent</code> this could be the user's
     * email address.
     *
     * @return A unique logical identifier for this event
     */
    public Optional<String> uniqueKey() {
        return Optional.empty();
    }
}
