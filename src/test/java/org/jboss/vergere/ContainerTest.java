package org.jboss.vergere;

import org.jboss.vergere.client.Container;
import org.jboss.vergere.client.container.IOC;
import org.jboss.vergere.res.Foo;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Mike Brock
 */
public class ContainerTest {
  @Before
  public void setupContainer() {
    new Container().bootstrapContainer();
  }

  @Test
  public void testSimpleBean() {
    Foo foo = IOC.getBeanManager().lookupBean(Foo.class).getInstance();
    System.out.println("foo:" + foo);
  }

  @Test
  public void testSimpleBeanDestroy() {
    Foo foo = IOC.getBeanManager().lookupBean(Foo.class).getInstance();
    IOC.getBeanManager().destroyBean(foo);
  }
}
