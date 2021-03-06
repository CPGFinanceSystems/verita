package de.cpg.oss.verita.test;

import de.cpg.oss.verita.event.Event;
import lombok.Value;

@Value
public class ToDoItemDescriptionChanged extends Event {
    private static final long serialVersionUID = 1L;

    private final String description;
}
