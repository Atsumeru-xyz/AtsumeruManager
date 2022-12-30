package xyz.atsumeru.manager.helpers;

import io.reactivex.disposables.Disposable;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class RXUtils {

    public static void safeDispose(@Nullable Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    public static void safeDispose(@Nullable AtomicReference<Disposable> disposableReference) {
        if (disposableReference != null) {
            safeDispose(disposableReference.get());
        }
    }
}
