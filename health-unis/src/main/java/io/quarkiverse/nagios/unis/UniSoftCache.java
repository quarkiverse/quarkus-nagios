package io.quarkiverse.nagios.unis;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import java.time.Duration;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class UniSoftCache {

    public static <T> Uni<T> cached(Uni<T> uni, Duration refresh, Duration wait, T initial) {
        return cached(uni, toMillis(refresh), wait, initial);
    }

    public static <T> Uni<T> cached(Uni<T> uni, ToLongFunction<? super T> refresh, Duration wait, T initial) {
        var cache = new AtomicReference<>(initial);
        var until = new AtomicLong();
        return uni.invoke(cache::set)
                .onItem().invoke(t -> until.set(System.currentTimeMillis() + refresh.applyAsLong(t)))
                .memoize().until(() -> System.currentTimeMillis() > until.get())
                .ifNoItem().after(wait).recoverWithItem(cache::get);
    }

    public static Uni<NagiosCheckResult> cached(Uni<NagiosCheckResult> uni, Duration refresh, Duration wait) {
        var timeout = NagiosCheck.named("timeout").result(NagiosStatus.UNKNOWN);
        return cached(uni, refresh, wait, timeout);
    }

    public static Builder<Void> build() {
        return Builder.DEFAULT;
    }

    private static <T> ToLongFunction<? super T> toMillis(Duration duration) {
        var millis = duration.toMillis();
        var twoYears = 1000L * 60 * 60 * 24 * 365 * 2;
        if (millis > twoYears)
            return t -> twoYears;
        return t -> millis;
    }

    public record Builder<T>(ToLongFunction<? super T> refresh, Duration waiting,
            T initial) implements Function<Uni<T>, Uni<T>> {

        private static final Builder<Void> DEFAULT = new Builder<>(toMillis(Duration.ofSeconds(60)), Duration.ofSeconds(5),
                null);

        public Builder<T> cacheFor(Duration refresh) {
            return cacheForMillis(toMillis(refresh));
        }

        public Builder<T> cacheFor(int seconds) {
            return cacheFor(Duration.ofSeconds(seconds));
        }

        public Builder<T> cacheForMillis(ToLongFunction<? super T> refresh) {
            return new Builder<>(refresh, waiting, initial);
        }

        public Builder<T> waitFor(Duration waiting) {
            return new Builder<>(refresh, waiting, initial);
        }

        public Builder<T> waitFor(int seconds) {
            return waitFor(Duration.ofSeconds(seconds));
        }

        @SuppressWarnings("unchecked")
        public <U> Builder<U> initially(U initial) {
            return new Builder<>((ToLongFunction<U>) refresh, waiting, initial);
        }

        public Builder<NagiosCheckResult> initiallyTimeout(String label) {
            var timeout = NagiosCheck.named(label).result("timeout", NagiosStatus.UNKNOWN);
            return initially(timeout);
        }

        @Override
        public Uni<T> apply(Uni<T> uni) {
            return cached(uni, refresh, waiting, initial);
        }

        public Uni<T> deferred(Supplier<? extends Uni<? extends T>> supplier) {
            return Uni.createFrom().<T>deferred(supplier::get).plug(this);
        }

        public <U extends T> Uni<T> deferredWork(Function<? super Uni<Void>, ? extends Uni<U>> function) {
            return deferred(() -> Uni.createFrom().voidItem()
                    .emitOn(Infrastructure.getDefaultWorkerPool())
                    .plug(function::apply));
        }

        public <U> Uni<U> deferredValue(U initial, Supplier<? extends U> supplier) {
            return initially(initial).deferredValue(supplier);
        }

        public Uni<T> deferredValue(Supplier<? extends T> supplier) {
            return deferredWork(uni -> uni.map(nil -> supplier.get()));
        }
    }
}
