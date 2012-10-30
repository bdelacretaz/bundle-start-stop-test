package ch.x42.osgi.bundlestartstop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartStopThread extends Thread {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final BundleContext bundleContext;
    private boolean running = true;
    private int counter;
    
    /** The closest this is to 1, the more bundles we stop at every cycle */
    public static final double STOP_BUNDLE_FACTOR = 0.3;
    
    private final static long JOIN_TIMEOUT_MSEC = 5000;
    private final static long FIXED_WAIT_BETWEEN_CYCLES_MSEC = 5000L;
    private final static long RANDOM_WAIT_BETWEEN_CYCLES_MSEC = 5000L;
    private final static long RANDOM_SEED = 42L;
    private final static long STOP_WAIT_MSEC = 5000L;
    private final static long START_WAIT_MSEC = 5000L;
    private final Set<String> ignoredBundlePatterns = new HashSet<String>();
    private final Random random;
    private final String mySymbolicName;
    
    StartStopThread(BundleContext bundleContext) {
        super(StartStopThread.class.getSimpleName());
        this.bundleContext = bundleContext;
        mySymbolicName = bundleContext.getBundle().getSymbolicName();
        
        log.info("Using random seed {}", RANDOM_SEED);
        random = new Random(RANDOM_SEED);
        
        ignoredBundlePatterns.add("org.osgi");
        ignoredBundlePatterns.add("org.apache.felix");
        ignoredBundlePatterns.add("commons");
        ignoredBundlePatterns.add("ch.x42");
        ignoredBundlePatterns.add("slf4j");
        ignoredBundlePatterns.add("log");
        log.info(
                "Won't touch bundles having one of the following patterns in their symbolic name: {}", 
                ignoredBundlePatterns);
        
        start();
    }
    
    public void run() {
        while(running) {
            try {
                runOneCycle();
                log.info("Waiting up to {} msec to start next cycle...", 
                        FIXED_WAIT_BETWEEN_CYCLES_MSEC + RANDOM_WAIT_BETWEEN_CYCLES_MSEC);
                waitMsec(FIXED_WAIT_BETWEEN_CYCLES_MSEC);
                waitMsec(-RANDOM_WAIT_BETWEEN_CYCLES_MSEC);
            } catch(Exception e) {
                log.error("Exception in runOneCycle", e);
                waitMsec(500);
            }
        }
    }
    
    private void runOneCycle() {
        log.info("Running cycle {}", ++counter);
        
        // Randomly select a bunch of bundles to stop
        final List<Bundle> toStop = new ArrayList<Bundle>();
        for(Bundle b : bundleContext.getBundles()) {
            if(mySymbolicName.equals(b.getSymbolicName())) {
                continue;
            }
            if(ignoreBundle(b)) {
                continue;
            }
            if(b.getState() != Bundle.ACTIVE) {
                continue;
            }
            if(random.nextDouble() > STOP_BUNDLE_FACTOR) {
                continue;
            }
            toStop.add(b);
        }
        
        log.info("Stopping {} bundles (if they are active)", toStop.size());
        
        for(Bundle b : toStop) {
            try {
                if(b.getState() == Bundle.ACTIVE) {
                    b.stop();
                }
            } catch(Exception e) {
                log.error("Could not stop " + b, e);
            }
        }
        
        for(Bundle b : toStop) {
            if(waitForState(b, Bundle.ACTIVE, false, STOP_WAIT_MSEC)) {
                log.info("{} stopped", b);
            } else {
                log.warn("State is still {} for {}??", Bundle.ACTIVE, b);
            }
        }
        
        log.info("Restarting all {} stopped bundles", toStop.size());
        for(Bundle b : toStop) {
            try {
                b.start();
            } catch(Exception e) {
                log.error("Could not start " + b, e);
            }
        }
        
        for(Bundle b : toStop) {
            if(waitForState(b, Bundle.ACTIVE, true, START_WAIT_MSEC)) {
                log.info("{} started", b);
            } else {
                log.warn("State is not {} for {}??", Bundle.ACTIVE, b);
            }
        }
        
        log.info("Cycle {} ends, successfully stopped/started {} bundles", counter, toStop.size());
        
    }
    
    private boolean waitForState(Bundle b, int expectedState, boolean expectEqual, long timeoutMsec) {
        final long end = System.currentTimeMillis() + timeoutMsec;
        while(System.currentTimeMillis() < end) {
            final boolean isEqual = b.getState() == expectedState; 
            if(expectEqual && isEqual) {
                return true;
            } else if(!expectEqual && !isEqual) {
                return true;
            }
        }
        return false;
    }
    
    private boolean ignoreBundle(Bundle b) {
        for(String pattern : ignoredBundlePatterns) {
            if(b.getSymbolicName().contains(pattern)) {
                return true;
            }
        }
        return false;
    }
    
    /** Wait a few msec. If msec is negative, random wait 
     *  up to its positive value.
     */
    private void waitMsec(long msec) {
        if(msec < 0) {
            msec = (long)(random.nextFloat() * -msec);
        }
        
        try {
            Thread.sleep(msec);
        } catch(InterruptedException ignore) {
        }
    }

    void stopAndJoin() throws InterruptedException {
        running=false;
        log.info("Waiting up to {} msec for {} to end", JOIN_TIMEOUT_MSEC, this);
        join(JOIN_TIMEOUT_MSEC);
        log.info("{} ended", this);
    }
}
