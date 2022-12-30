package xyz.atsumeru.manager.features;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

@AutomaticFeature
public class AwtFeature implements Feature {

    @SuppressWarnings("unchecked")
    public void afterRegistration(Feature.AfterRegistrationAccess access) {
        RuntimeClassInitializationSupport runtimeInit = (RuntimeClassInitializationSupport) ImageSingletons.lookup((Class) RuntimeClassInitializationSupport.class);
        String reason = "Run time init for AWT";
        runtimeInit.initializeAtRunTime("com.sun.imageio", reason);
        runtimeInit.initializeAtRunTime("java.awt", reason);
        runtimeInit.initializeAtRunTime("javax.imageio", reason);
        runtimeInit.initializeAtRunTime("sun.awt", reason);
        runtimeInit.initializeAtRunTime("sun.font", reason);
        runtimeInit.initializeAtRunTime("sun.java2d", reason);
    }
}