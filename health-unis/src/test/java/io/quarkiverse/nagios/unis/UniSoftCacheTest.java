package io.quarkiverse.nagios.unis;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UniSoftCacheTest {

    @Test
    void deferredValue() {
        var latch = new CountDownLatch(1);
        var value = new AtomicReference<>("actual");
        var uni = UniSoftCache.build().waitFor(1).cacheFor(1).deferredValue("initial", () -> {
            await(latch);
            return value.get();
        });

        assertEquals("initial", value(uni));
        latch.countDown();
        assertEquals("actual", value(uni));
        value.set("updated");
        assertEquals("actual", value(uni));
        sleep(2);
        assertEquals("updated", value(uni));
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
