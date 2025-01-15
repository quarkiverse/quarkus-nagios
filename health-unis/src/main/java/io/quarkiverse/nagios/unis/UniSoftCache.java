package io.quarkiverse.nagios.unis;

import java.time.Duration;
import java.util.function.*;

import io.quarkiverse.nagios.health.*;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

public class UniSoftCache<T> {

    public static <T> Uni<T> cached(Uni<T> uni, Duration refresh, Duration wait, T initial) {
        return cached(uni, toMillis(refresh), wait, initial);
    }

    public static <T> Uni<T> cached(Uni<T> uni, ToLongFunction<? super T> refresh, Duration wait, T initial) {
        return new UniSoftCache<>(uni, refresh, wait, initial).asUni();
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

    private final Uni<T> actual;
    private final Uni<T> deferred;
    private final ToLongFunction<? super T> refresh;
    private Uni<T> cache;
    private Uni<T> next = null;
    private long until = 0;

    public UniSoftCache(Uni<T> actual, ToLongFunction<? super T> refresh, Duration wait, T initial) {
        this.refresh = refresh;
        this.cache = Uni.createFrom().item(initial);
        this.actual = actual.invoke(this::onItem)
                .onFailure().invoke(this::onFailure);
        this.deferred = Uni.createFrom().deferred(this::get)
                .ifNoItem().after(wait).recoverWithUni(() -> cache);
    }

    public Uni<T> asUni() {
        return deferred;
    }

    private Uni<T> get() {
        var n = next;
        if (n != null) {
            return n;
        }
        if (useCache()) {
            return cache;
        }
        return refresh();
    }

    private boolean useCache() {
        return System.currentTimeMillis() < until;
    }

    private synchronized Uni<T> refresh() {
        if (next == null) {
            var future = actual.subscribeAsCompletionStage();
            next = Uni.createFrom().emitter(emitter ->
                future.whenComplete((item, error) -> {
                    if (error != null) {
                        emitter.fail(error);
                    } else {
                        emitter.complete(item);
                    }
                })
            );
        }
        return next;
    }

    private void onItem(T item) {
        cache = Uni.createFrom().item(item);
        until = System.currentTimeMillis() + refresh.applyAsLong(item);
        synchronized (this) {
            next = null;
        }
    }

    private synchronized void onFailure() {
        next = null;
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
            return initially(NagiosCheck.named(label).result("timeout", NagiosStatus.UNKNOWN));
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
