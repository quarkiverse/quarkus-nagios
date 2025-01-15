package io.quarkiverse.nagios.unis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Uni;

class UniSoftCacheTest {

    @Test
    void deferredValue() {
        var latch = new CountDownLatch(1);
        var value = new AtomicReference<>("actual");
        var uni = UniSoftCache.build().waitFor(1).cacheFor(1).deferredValue("initial", () -> {
            await(latch);
            return value.get();
        });

        var f1 = uni.subscribeAsCompletionStage();
        assertEquals("initial", value(uni));
        latch.countDown();

        var f2 = uni.subscribeAsCompletionStage();
        assertEquals("actual", value(uni));
        value.set("updated");
        assertEquals("actual", value(uni));

        sleep(2);
        var f3 = uni.subscribeAsCompletionStage();
        assertEquals("updated", value(uni));

        assertEquals("initial", f1.join());
        assertEquals("actual", f2.join());
        assertEquals("updated", f3.join());
    }

    @Test
    void dontRetryPending() {
        var counter = new AtomicInteger();
        var uni = UniSoftCache.build()
                .waitFor(Duration.ofMillis(400)).cacheFor(10)
                .initially("initial")
                .deferred(() -> Uni.createFrom().item(() -> "actual " + counter.incrementAndGet())
                        .onItem().delayIt().by(Duration.ofSeconds(1)));

        var f1 = uni.subscribeAsCompletionStage();
        assertEquals("initial", value(uni));
        assertEquals("initial", value(uni));

        assertEquals(1, counter.get());
        var f2 = uni.subscribeAsCompletionStage();
        assertEquals("actual 1", value(uni));
        assertEquals("actual 1", value(uni));

        assertEquals(1, counter.get());
        assertEquals("initial", f1.join());
        assertEquals("actual 1", f2.join());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings({ "java:S2925", "SameParameterValue" })
    private static void sleep(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static <T> T value(Uni<T> uni) {
        return uni.await().atMost(Duration.ofSeconds(2));
    }
}
