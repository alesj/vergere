package org.jboss.vergere.client.container;

import org.jboss.vergere.client.api.ContextualTypeProvider;
import org.jboss.vergere.client.api.IOCProvider;

import javax.enterprise.inject.Instance;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;

@IOCProvider
@Singleton
public class InstanceProvider implements ContextualTypeProvider<Instance> {

  @Override
  public Instance provide(final Class[] typeargs, final Annotation[] qualifiers) {

    /*
    * If you see a compile error here, ensure that you are using Errai's custom
    * version of javax.enterprise.event.Event, which comes from the
    * errai-javax-enterprise project. The errai-cdi-client POM is set up this
    * way.
    *
    * Eclipse users: seeing an error here probably indicates that M2E has
    * clobbered your errai-javax-enterprise source folder settings. To fix your
    * setup, see the README in the root of errai-javax-enterprise.
    */

    return new InstanceImpl(typeargs[0], qualifiers);
  }

  static class InstanceImpl implements Instance<Object> {
    private final Class type;
    private final Annotation[] qualifiers;

    InstanceImpl(Class type, Annotation[] qualifiers) {
      this.type = type;
      this.qualifiers = qualifiers;
    }

    @Override
    public Instance<Object> select(Annotation... qualifiers) {
      return new InstanceImpl(type, qualifiers);
    }

    @Override
    public Instance<Object> select(Class subtype, Annotation... qualifiers) {
      return new InstanceImpl(type, qualifiers);
    }

    @Override
    public boolean isUnsatisfied() {
      return false;
    }

    @Override
    public boolean isAmbiguous() {
      return false;
    }

    @Override
    public Iterator<Object> iterator() {
      return Collections.emptyList().iterator();
    }

    @Override
    public Object get() {
      IOCBeanDef bean = IOC.getBeanManager().lookupBean(type, qualifiers);
      if (bean == null) {
        return null;
      }
      else {
        return bean.getInstance();
      }
    }

    @Override
    public Instance<Object>  select(TypeLiteral subtype, Annotation... qualifiers) {
      return null;
    }
  }
}