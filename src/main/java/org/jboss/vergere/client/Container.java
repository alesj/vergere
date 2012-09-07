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

import org.jboss.vergere.client.container.IOCBeanManagerLifecycle;
import org.jboss.vergere.util.IOCBootstrapMaker;
import org.jboss.vergere.util.ThreadUtil;

import java.lang.annotation.Annotation;

public class Container {

  public void bootstrapContainer() {
    try {
      new IOCBeanManagerLifecycle().resetBeanManager();

      System.out.println("Vergere bootstrapper successfully initialized.");

      long tm = System.currentTimeMillis();
      final Class<? extends Bootstrapper> bootstrapperClass = new IOCBootstrapMaker().generate();

      final Bootstrapper bootstrapper = bootstrapperClass.newInstance();
      BootstrapperInjectionContext injectionContext = bootstrapper.bootstrapContainer();
      injectionContext.getRootContext().finish();

      ThreadUtil.stopExecutor();

      System.out.println("Vergere container bootstrapped. (time: " + (System.currentTimeMillis() - tm) +  "ms)");
    }
    catch (Throwable t) {
      t.printStackTrace();
      throw new RuntimeException("critical error in IOC container bootstrap", t);
    }
  }
}
