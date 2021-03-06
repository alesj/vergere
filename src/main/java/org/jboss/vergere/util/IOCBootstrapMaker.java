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

package org.jboss.vergere.util;


import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.vergere.bootstrapper.IOCBootstrapGenerator;
import org.jboss.vergere.bootstrapper.QualifierEqualityFactoryGenerator;
import org.jboss.vergere.client.Bootstrapper;
import org.jboss.vergere.client.QualifierEqualityFactory;
import org.jboss.vergere.client.QualifierEqualityFactoryProvider;
import org.jboss.vergere.client.QualifierUtil;

import java.io.File;
import java.io.FileOutputStream;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class IOCBootstrapMaker {
  public IOCBootstrapMaker() {
  }

  public Class<? extends Bootstrapper> generate() {
    final String qualifierFactoryPackageName = QualifierEqualityFactory.class.getPackage().getName();
    final String qualifierFactoryClassName = "QualifierEqualityFactoryImpl";

    final String bootstrapperPackageName = Bootstrapper.class.getPackage().getName();
    final String bootstrapperClassName = "BootstrapperImpl";

    final IOCBootstrapGenerator bootstrapGenerator = new IOCBootstrapGenerator();

    final String qualifierFactoryClassString = new QualifierEqualityFactoryGenerator().generate();

    final File fileCacheDir = VergereUtils.getApplicationCacheDirectory();
    final File cacheFile = new File(fileCacheDir.getAbsolutePath() + "/" + bootstrapperClassName + ".java");

    try {
      final File directory =
          new File(VergereUtils.getApplicationCacheDirectory() + "/generated/" + bootstrapperPackageName.replaceAll("\\.", "/"));

      final File outputDirectory = new File(VergereUtils.getApplicationCacheDirectory(), "generated");

      final File qualifierFactorySourceFile = new File(directory.getAbsolutePath(), qualifierFactoryClassName + ".java")
          .getAbsoluteFile();
      final File qualifierFactoryOutFile = new File(directory.getAbsolutePath(), qualifierFactoryClassName + ".class")
          .getAbsoluteFile();

      final File bootstrapperSourceFile = new File(directory.getAbsolutePath(), bootstrapperClassName + ".java")
          .getAbsoluteFile();
      final File bootstrapperOutFile = new File(directory.getAbsolutePath(), bootstrapperClassName + ".class")
          .getAbsoluteFile();

      if (!VergereUtils.hasClasspathChanged() && qualifierFactoryOutFile.exists() && bootstrapperOutFile.exists()) {
        final Class aClass = CompileUtil.loadClassDefinition(qualifierFactoryOutFile.getAbsolutePath(), qualifierFactoryPackageName, qualifierFactoryClassName);

        QualifierUtil.initFromFactoryProvider(new QualifierEqualityFactoryProvider() {
          @Override
          public QualifierEqualityFactory provide() {
            try {
              return (QualifierEqualityFactory) aClass.newInstance();
            }
            catch (Throwable e) {
              throw new RuntimeException("failed to load qualifer equality factory", e);
            }
          }
        });

        final Class aClass1 = CompileUtil.loadClassDefinition(bootstrapperOutFile.getAbsolutePath(), bootstrapperPackageName, bootstrapperClassName);
        return aClass1;
      }
      else {
        if (qualifierFactorySourceFile.exists()) {
          qualifierFactorySourceFile.delete();
          qualifierFactoryOutFile.delete();
        }

        if (bootstrapperSourceFile.exists()) {
          bootstrapperSourceFile.delete();
          bootstrapperOutFile.delete();
        }

        final String bootstrapperClassString = bootstrapGenerator.generate(bootstrapperPackageName, bootstrapperClassName);
        RebindUtils.writeStringToFile(cacheFile, bootstrapperClassString);

        directory.mkdirs();

        FileOutputStream outputStream = new FileOutputStream(qualifierFactorySourceFile);

        outputStream.write(qualifierFactoryClassString.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        outputStream = new FileOutputStream(bootstrapperSourceFile);

        outputStream.write(bootstrapperClassString.getBytes("UTF-8"));
        outputStream.flush();
        outputStream.close();

        final String factoryPath = qualifierFactorySourceFile.getParentFile().getAbsolutePath();
        final Class qualifierEqualityImplClass
            = CompileUtil.compileAndLoad(factoryPath, qualifierFactoryPackageName, qualifierFactoryClassName,
            outputDirectory.getAbsolutePath());

        QualifierUtil.initFromFactoryProvider(new QualifierEqualityFactoryProvider() {
          @Override
          public QualifierEqualityFactory provide() {
            try {
              return (QualifierEqualityFactory) qualifierEqualityImplClass.newInstance();
            }
            catch (Throwable e) {
              throw new RuntimeException("failed to load qualifer equality factory", e);
            }
          }
        });

        final String bootstrapperPath = bootstrapperSourceFile.getParentFile().getAbsolutePath();
        return CompileUtil.compileAndLoad(bootstrapperPath, bootstrapperPackageName, bootstrapperClassName,
            outputDirectory.getAbsolutePath());
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static class BootstrapClassloader extends ClassLoader {
    private BootstrapClassloader(final ClassLoader classLoader) {
      super(classLoader);
    }

    public Class<?> defineClassX(final String className, final byte[] b, final int off, final int len) {
      return super.defineClass(className, b, off, len);
    }
  }
}
