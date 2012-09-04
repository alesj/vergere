package org.jboss.vergere.res;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

/**
 * @author Mike Brock
 */
@Dependent
public class Foo {

  @PostConstruct
  private void testPostConstruct() {
    System.out.println("Hello, World!");
  }

  @PreDestroy
  private void testPreDestroy() {
    System.out.println("Goodbye, World!");
  }
}
