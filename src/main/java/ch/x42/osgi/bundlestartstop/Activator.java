package ch.x42.osgi.bundlestartstop;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private StartStopThread startStopThread;
    
    @Override
    public void start(BundleContext context) throws Exception {
        startStopThread = new StartStopThread(context);
        log.info("{} created", startStopThread);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.info("stopping {}", startStopThread);
        startStopThread.stopAndJoin();
        startStopThread = null;
    }
}
