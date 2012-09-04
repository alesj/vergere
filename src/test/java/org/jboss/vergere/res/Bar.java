package org.jboss.vergere.res;

import javax.enterprise.context.Dependent;

/**
 * @author Mike Brock
 */
@Dependent
public class Bar {
  private String name = "BARZ!";

  public String getName() {
    return name;
  }
}
