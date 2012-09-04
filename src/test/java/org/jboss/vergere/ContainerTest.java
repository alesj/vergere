package org.jboss.vergere;

import org.jboss.vergere.client.Container;
import org.jboss.vergere.client.container.IOC;
import org.jboss.vergere.res.Foo;
import org.junit.Test;

/**
 * @author Mike Brock
 */
public class ContainerTest {
  @Test
  public void testContainer() {
    try {
      new Container().bootstrapContainer();

      Foo foo = IOC.getBeanManager().lookupBean(Foo.class).getInstance();

      System.out.println("foo:" + foo);
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
