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

package org.jboss.vergere.injector;

import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.util.Stmt;
import org.jboss.vergere.injector.api.InjectableInstance;
import org.jboss.vergere.injector.api.InjectionContext;
import org.jboss.vergere.injector.api.WiringElementType;


public class ProviderInjector extends TypeInjector {
  private final AbstractInjector providerInjector;
  private boolean provided = false;

  public ProviderInjector(MetaClass type, MetaClass providerType, InjectionContext context) {
    super(type, context);

    setEnabled(context.isReachable(type) || context.isReachable(providerType));

    this.providerInjector = new TypeInjector(providerType, context);
    context.registerInjector(providerInjector);
    providerInjector.setEnabled(isEnabled());

    this.testmock = context.isElementType(WiringElementType.TestMockBean, providerType);
    this.singleton = context.isElementType(WiringElementType.SingletonBean, providerType);
    this.alternative = context.isElementType(WiringElementType.AlternativeBean, type);
    setRendered(true);
  }

  @Override
  public Statement getBeanInstance(InjectableInstance injectableInstance) {
    if (isSingleton() && provided) {
      return Stmt.loadVariable(providerInjector.getInstanceVarName()).invoke("get");
    }

    provided = true;

    return Stmt.nestedCall(providerInjector.getBeanInstance(injectableInstance)).invoke("get");
  }
}
