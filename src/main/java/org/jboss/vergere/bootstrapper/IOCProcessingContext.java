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

package org.jboss.vergere.bootstrapper;

import org.jboss.errai.codegen.Context;
import org.jboss.errai.codegen.Statement;
import org.jboss.errai.codegen.Variable;
import org.jboss.errai.codegen.VariableReference;
import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.common.client.framework.Assert;
import org.jboss.vergere.client.BootstrapperInjectionContext;
import org.jboss.vergere.injector.api.InjectionPoint;
import org.jboss.vergere.injector.api.TypeDiscoveryListener;
import org.jboss.vergere.metadata.JSR330QualifyingMetadataFactory;
import org.jboss.vergere.metadata.QualifyingMetadataFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class IOCProcessingContext {
   protected final Context context;
  protected final BuildMetaClass bootstrapClass;
  protected final ClassStructureBuilder bootstrapBuilder;

  protected final Stack<BlockBuilder<?>> blockBuilder;

  protected final List<Statement> appendToEnd;
  protected final List<TypeDiscoveryListener> typeDiscoveryListeners;
  protected final Set<MetaClass> discovered = new HashSet<MetaClass>();

  protected final Variable contextVariable = Variable.create("injContext", BootstrapperInjectionContext.class);

  protected final QualifyingMetadataFactory qualifyingMetadataFactory;

  private IOCProcessingContext(final Builder builder) {
    this.context = builder.context;
    this.bootstrapClass = builder.bootstrapClassInstance;
    this.bootstrapBuilder = builder.bootstrapBuilder;

    this.blockBuilder = new Stack<BlockBuilder<?>>();
    this.blockBuilder.push(builder.blockBuilder);

    this.appendToEnd = new ArrayList<Statement>();
    this.typeDiscoveryListeners = new ArrayList<TypeDiscoveryListener>();
    this.qualifyingMetadataFactory = builder.qualifyingMetadataFactory;
  }

  public static class Builder {
    private Context context;
    private BuildMetaClass bootstrapClassInstance;
    private ClassStructureBuilder bootstrapBuilder;
    private BlockBuilder<?> blockBuilder;
    private QualifyingMetadataFactory qualifyingMetadataFactory;

    public static Builder create() {
      return new Builder();
    }

    public Builder context(Context context) {
      this.context = context;
      return this;
    }

    public Builder bootstrapClassInstance(BuildMetaClass bootstrapClassInstance) {
      this.bootstrapClassInstance = bootstrapClassInstance;
      return this;
    }

    public Builder bootstrapBuilder(ClassStructureBuilder classStructureBuilder) {
      this.bootstrapBuilder = classStructureBuilder;
      return this;
    }

    public Builder blockBuilder(BlockBuilder<?> blockBuilder) {
      this.blockBuilder = blockBuilder;
      return this;
    }

    public Builder qualifyingMetadata(QualifyingMetadataFactory qualifyingMetadataFactory) {
      this.qualifyingMetadataFactory = qualifyingMetadataFactory;
      return this;
    }


    public IOCProcessingContext build() {
      Assert.notNull("context cannot be null", context);
      Assert.notNull("bootstrapClassInstance cannot be null", bootstrapClassInstance);
      Assert.notNull("bootstrapBuilder cannot be null", bootstrapBuilder);
      Assert.notNull("blockBuilder cannot be null", blockBuilder);

      if (qualifyingMetadataFactory == null) {
        qualifyingMetadataFactory = new JSR330QualifyingMetadataFactory();
      }

      return new IOCProcessingContext(this);
    }
  }

  public BlockBuilder<?> getBlockBuilder() {
    return blockBuilder.peek();
  }

  public BlockBuilder<?> append(Statement statement) {
    return getBlockBuilder().append(statement);
  }

  public void globalInsertBefore(Statement statement) {
    blockBuilder.get(0).insertBefore(statement);
  }

  public BlockBuilder<?> globalAppend(Statement statement) {
    return blockBuilder.get(0).append(statement);
  }

  public void pushBlockBuilder(BlockBuilder<?> blockBuilder) {
    this.blockBuilder.push(blockBuilder);
  }

  public void popBlockBuilder() {
    this.blockBuilder.pop();

    if (this.blockBuilder.size() == 0) {
      throw new AssertionError("block builder was over popped! something is wrong.");
    }
  }

  public void appendToEnd(Statement statement) {
    appendToEnd.add(statement);
  }

  public List<Statement> getAppendToEnd() {
    return appendToEnd;
  }

  public BuildMetaClass getBootstrapClass() {
    return bootstrapClass;
  }

  public ClassStructureBuilder getBootstrapBuilder() {
    return bootstrapBuilder;
  }

  public Context getContext() {
    return context;
  }

  public VariableReference getContextVariableReference() {
    return contextVariable.getReference();
  }

  public QualifyingMetadataFactory getQualifyingMetadataFactory() {
    return qualifyingMetadataFactory;
  }

  public void registerTypeDiscoveryListener(TypeDiscoveryListener discoveryListener) {
    this.typeDiscoveryListeners.add(discoveryListener);
  }

  public void handleDiscoveryOfType(InjectionPoint injectionPoint) {
    if (discovered.contains(injectionPoint.getType())) return;
    for (TypeDiscoveryListener listener : typeDiscoveryListeners) {
      listener.onDiscovery(this, injectionPoint);
    }
    discovered.add(injectionPoint.getType());
  }
}
