package me.eggdev.eggbot;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MemoryCache<T> {

    // The life for this cache
    private int life = -1;

    // The unit for the life for this cache
    private TimeUnit lifeUnit = TimeUnit.MILLISECONDS;

    // last use
    private Date last;
    private ScheduledFuture<?> scheduledFuture; // future to delete object

    // The cached object
    private T cache;
    private Function<T, T> resetPoint = t -> null; // The reset point. The cache is set to this value
                                                    // as a reset point

    {
        newLife();
    }

    private void newLife() {
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
        scheduledFuture = EggBotKt.getExecutorService()
                .schedule(() -> {
                    cache = resetPoint.apply(cache);
                    newLife();
                }, life, lifeUnit);
    }

    /**
     * Retrieves the cache from memory. If the cache is not
     * retrieved or if the cache is beyond its life duration
     * duration, a new cache will be fetched.
     *
     * @return The object
     */
    public T get() {
        return cache;
    }

}
