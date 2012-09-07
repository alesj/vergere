package org.jboss.vergere;

import org.jboss.vergere.client.Container;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Mike Brock
 */
public class Main {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("must specify a module.");
      return;
    }

    URL url = new File(args[0]).getAbsoluteFile().toURI().toURL();

    final URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url});
    Thread.currentThread().setContextClassLoader(urlClassLoader);

    new Container().bootstrapContainer();
  }
}
