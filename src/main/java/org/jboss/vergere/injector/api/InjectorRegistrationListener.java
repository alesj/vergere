package org.jboss.vergere.injector.api;

import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.vergere.injector.Injector;

/**
 * @author Mike Brock
 */
public interface InjectorRegistrationListener {
  public void onRegister(MetaClass type, Injector injector);
}
