/*
 * Copyright 2011 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.vergere.client;

import org.jboss.vergere.client.container.CreationalCallback;
import org.jboss.vergere.client.container.CreationalContext;
import org.jboss.vergere.client.container.IOC;
import org.jboss.vergere.client.container.IOCBeanManager;

import java.lang.annotation.Annotation;

public class BootstrapperInjectionContext {
  private final IOCBeanManager manager;
  private final CreationalContext rootContext;

  public BootstrapperInjectionContext() {
    manager = IOC.getBeanManager();
    rootContext = new CreationalContext(true, manager, "javax.enterprise.context.ApplicationScoped");
  }

  @SuppressWarnings("unchecked")
  public void addBean(final Class type,
                      final Class beanType,
                      final CreationalCallback callback,
                      final Object instance,
                      final Annotation[] qualifiers) {

    manager.addBean(type, beanType, callback, instance, qualifiers);
  }


  @SuppressWarnings("unchecked")
  public void addBean(final Class type,
                      final Class beanType,
                      final CreationalCallback callback,
                      final Object instance,
                      final Annotation[] qualifiers,
                      final String name) {

    manager.addBean(type, beanType, callback, instance, qualifiers, name);
  }

  @SuppressWarnings("unchecked")
  public void addBean(final Class type,
                      final Class beanType,
                      final CreationalCallback callback,
                      final Object instance,
                      final Annotation[] qualifiers,
                      final String name,
                      final boolean concrete) {

    manager.addBean(type, beanType, callback, instance, qualifiers, name, concrete);
  }

  public CreationalContext getRootContext() {
    return rootContext;
  }
}
