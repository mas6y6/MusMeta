package com.mas6y6.musmeta.event.core;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}