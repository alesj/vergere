package org.jboss.vergere.util;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.util.QuickDeps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Brock
 */
public class EnvUtil {


  private static final Logger log = LoggerFactory.getLogger("Env");


  public static ReachableTypes getAllReachableClasses() {

//
//    final EnviromentConfig config = getEnvironmentConfig();
//
//    if (System.getProperty(SYSPROP_USE_REACHABILITY_ANALYSIS) != null
//        && !Boolean.getBoolean(SYSPROP_USE_REACHABILITY_ANALYSIS)) {
//
//      log.warn("reachability analysis disabled. errai may generate unnecessary code.");
//      log.warn("enable reachability analysis with -D" + SYSPROP_USE_REACHABILITY_ANALYSIS + "=true");
//      return ReachableTypes.EVERYTHING_REACHABLE_INSTANCE;
//    }

    long time = System.currentTimeMillis();


    final Set<String> allDeps = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(100));
    final Collection<MetaClass> allCachedClasses = MetaClassFactory.getAllCachedClasses();
    final ClassLoader classLoader = EnvUtil.class.getClassLoader();

    final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    try {
      for (final MetaClass mc : allCachedClasses) {
        final URL resource = classLoader.getResource(mc.getFullyQualifiedName().replaceAll("\\.", "/") + ".java");

        if (resource != null) {
          InputStream stream = null;
          try {
            stream = new BufferedInputStream(resource.openStream());
            final byte[] readBuffer = new byte[stream.available()];
            stream.read(readBuffer);

            executor.execute(new ReachabilityRunnable(readBuffer, allDeps));
          }
          catch (IOException e) {
            log.warn("could not open resource: " + resource.getFile());
          }
          finally {
            if (stream != null) {
              stream.close();
            }
          }
        }
      }
    }
    catch (Throwable e) {
      e.printStackTrace();
    }

    try {
      executor.shutdown();
      executor.awaitTermination(60, TimeUnit.MINUTES);
    }
    catch (InterruptedException e) {
      e.printStackTrace();
    }

    if (log.isDebugEnabled()) {
      log.debug("*** REACHABILITY ANALYSIS ***");
      for (final String s : allDeps) {
        log.debug(" -> " + s);
      }

      time = System.currentTimeMillis() - time;

      log.debug("*** END OF REACHABILITY ANALYSIS (" + time + "ms) *** ");
    }

    return new ReachableTypes(allDeps, true);
  }

  private static class ReachabilityRunnable implements Runnable {
    private final byte[] sourceBuffer;
    private final Set<String> results;

    private ReachabilityRunnable(final byte[] sourceBuffer, final Set<String> results) {
      this.sourceBuffer = sourceBuffer;
      this.results = results;
    }

    @Override
    public void run() {
      results.addAll(QuickDeps.getQuickTypeDependencyList(new String(sourceBuffer), null));
    }
  }
}
