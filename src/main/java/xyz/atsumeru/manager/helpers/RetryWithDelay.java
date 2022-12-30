package xyz.atsumeru.manager.helpers;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;

import java.util.concurrent.TimeUnit;

public class RetryWithDelay implements Function<Flowable<? extends Throwable>, Publisher<?>> {
    private final int maxRetries;
    private final int retryDelay;
    private final TimeUnit timeUnit;
    private int retryCount;

    public RetryWithDelay(final int maxRetries, final int retryDelay, final TimeUnit timeUnit) {
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.timeUnit = timeUnit;
        this.retryCount = 0;
    }

    @Override
    public Publisher<?> apply(final Flowable<? extends Throwable> attempts) {
        return attempts.flatMap((Function<Throwable, Flowable<?>>) throwable -> {
            if (++retryCount < maxRetries) {
                // When this Observable calls onNext, the original
                // Observable will be retried (i.e. re-subscribed).
                return Flowable.timer(retryDelay, timeUnit);
            }

            // Max retries hit. Just pass the error along.
            return Flowable.error(throwable);
        });
    }
}