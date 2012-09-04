package org.jboss.vergere.injector.api;

/**
 * @author Mike Brock
 */
public interface RenderingHook {
  public void onRender(InjectableInstance instance);
}
