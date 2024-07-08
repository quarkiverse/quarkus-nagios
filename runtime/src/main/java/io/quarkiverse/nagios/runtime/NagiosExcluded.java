package io.quarkiverse.nagios.runtime;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.*;

import jakarta.inject.Qualifier;

@Target(TYPE)
@Retention(RUNTIME)
@Qualifier
@Documented
public @interface NagiosExcluded {
}
