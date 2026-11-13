package com.mas6y6.musmeta.event.application;

import com.mas6y6.musmeta.event.core.Event;
import com.mas6y6.musmeta.utils.VersionSource;


public abstract class ApplicationStartedEvent extends Event {
    protected abstract VersionSource.Version version();
}
