package org.jboss.vergere.res;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@Dependent
public class Foo {
  @Inject Bar bar;

  @PostConstruct
  private void testPostConstruct() {
    System.out.println("Hello, World! -- I have an injected Bar! " + bar.getName()
        + "; which cycles on itself (" + bar.getBar().getName() + ")");
  }

  @PreDestroy
  private void testPreDestroy() {
    System.out.println("Goodbye, World!");
  }
}
