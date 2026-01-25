package net.janrupf.gradle.hytale.dev.actions;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

public interface WriteJsonWorkActionParams extends WorkParameters {
    Property<Boolean> getPrettyPrint();
    Property<Object> getValue();
    RegularFileProperty getTarget();
}
