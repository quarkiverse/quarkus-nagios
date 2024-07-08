package io.quarkiverse.nagios.runtime;

import jakarta.inject.Qualifier;
import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
@Qualifier
@Documented
public @interface NagiosExcluded {
}
