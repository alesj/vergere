package org.jboss.vergere.res;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Mike Brock
 */
@ApplicationScoped
public class Bar {
  private String name = "BARZ!";

  @Inject Bar bar;

  public String getName() {
    return name;
  }

  public Bar getBar() {
    return bar;
  }
}
