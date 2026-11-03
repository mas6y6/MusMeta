package com.mas6y6.musmeta.event.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {

    EventPriority priority() default EventPriority.NORMAL;

    boolean ignoreCancelled() default false;
}