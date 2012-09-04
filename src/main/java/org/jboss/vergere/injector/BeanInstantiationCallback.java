package org.jboss.vergere.injector;

import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.vergere.injector.api.InjectionContext;

/**
 * @author Mike Brock
 */
public interface BeanInstantiationCallback {
  public void instantiateBean(InjectionContext injectContext, BlockBuilder creationCallbackMethod);
}
