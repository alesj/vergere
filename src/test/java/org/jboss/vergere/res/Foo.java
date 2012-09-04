package org.jboss.vergere.res;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 * @author Mike Brock
 */
@ApplicationScoped
public class Foo {

  @PostConstruct
  private void testPostConstruct() {
    System.out.println("Hello, World!");
  }
}
