package org.westford.compositor.core.events;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Start {
    public static Start create() {
        return new AutoValue_Start();
    }
}
