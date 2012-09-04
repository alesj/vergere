package org.jboss.vergere.client.api.builtin;

import org.jboss.vergere.client.api.ContextualTypeProvider;
import org.jboss.vergere.client.api.Disposer;
import org.jboss.vergere.client.api.IOCProvider;
import org.jboss.vergere.client.container.IOCBeanManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;

/**
 * @author Mike Brock
 */
@IOCProvider
@Singleton
public class DisposerProvider implements ContextualTypeProvider<Disposer> {
  @Inject
  IOCBeanManager beanManager;

  @Override
  public Disposer provide(final Class<?>[] typeargs, final Annotation[] qualifiers) {
    return new Disposer() {
      @Override
      public void dispose(Object beanInstance) {
        beanManager.destroyBean(beanInstance);
      }
    };
  }
}
