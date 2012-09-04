package org.jboss.vergere.util;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import javassist.bytecode.ClassFile;
import org.jboss.errai.common.client.framework.ErraiAppAttribs;
import org.jboss.errai.common.metadata.DeploymentContext;
import org.jboss.errai.common.metadata.PropertyScanner;
import org.jboss.errai.common.metadata.RebindUtils;
import org.jboss.errai.common.metadata.VfsUrlType;
import org.jboss.errai.common.metadata.WarUrlType;
import org.jboss.errai.reflections.Configuration;
import org.jboss.errai.reflections.Reflections;
import org.jboss.errai.reflections.adapters.MetadataAdapter;
import org.jboss.errai.reflections.scanners.FieldAnnotationsScanner;
import org.jboss.errai.reflections.scanners.MethodAnnotationsScanner;
import org.jboss.errai.reflections.scanners.TypeAnnotationsScanner;
import org.jboss.errai.reflections.util.ConfigurationBuilder;
import org.jboss.errai.reflections.vfs.Vfs;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Scans component meta data. The scanner creates a {@link DeploymentContext} that identifies nested subdeployments
 * (i.e. WAR inside EAR) and processes the resulting archive Url's using the <a
 * href="http://code.google.com/p/reflections/">Reflections</a> library.
 * <p/>
 * <p/>
 * The initial set of config Url's (entry points) is discovered through ErraiApp.properties.
 *
 * @author Heiko Braun <hbraun@redhat.com>
 * @author Mike Brock <cbrock@redhat.com>
 * @author Christian Sadilek <csadilek@redhat.com>
 */
public class MetaDataScanner extends Reflections {
  public static final String CLASSPATH_ANCHOR_FILE = "vergere.properties";


  private static final PropertyScanner propScanner = new PropertyScanner(
      new Predicate<String>() {
        public boolean apply(String file) {
          return file.endsWith(".properties");
        }
      }
  );

  MetaDataScanner(List<URL> urls) {
    super(getConfiguration(urls));
    scan();
  }

  private static final Map<String, Set<SortableClassFileWrapper>> annotationsToClassFile =
      new TreeMap<String, Set<SortableClassFileWrapper>>();

  private static class SortableClassFileWrapper implements Comparable<SortableClassFileWrapper> {
    private String name;
    private ClassFile classFile;

    private SortableClassFileWrapper(String name, ClassFile classFile) {
      this.name = name;
      this.classFile = classFile;
    }

    public ClassFile getClassFile() {
      return classFile;
    }

    @Override
    public int compareTo(SortableClassFileWrapper o) {
      return name.compareTo(o.name);
    }
  }

  private static Configuration getConfiguration(List<URL> urls) {

    return new ConfigurationBuilder()
        .setUrls(urls)
        .setScanners(
            new FieldAnnotationsScanner(),
            new MethodAnnotationsScanner(),
            new TypeAnnotationsScanner() {
              @Override
              public void scan(Object cls) {
                @SuppressWarnings("unchecked")
                MetadataAdapter adapter = getMetadataAdapter();

                final String className = adapter.getClassName(cls);

                // noinspection unchecked
                for (String annotationType : (List<String>) adapter.getClassAnnotationNames(cls)) {
                  if (acceptResult(annotationType) ||
                      annotationType.equals(Inherited.class.getName())) { // as an exception, accept
                    // Inherited as well
                    getStore().put(annotationType, className);

                    if (cls instanceof ClassFile) {
                      Set<SortableClassFileWrapper> classes = annotationsToClassFile.get(annotationType);
                      if (classes == null) {
                        annotationsToClassFile.put(annotationType, classes =
                            new TreeSet<SortableClassFileWrapper>());
                      }
                      classes.add(new SortableClassFileWrapper(className, (ClassFile) cls));
                    }
                  }
                }

              }
            },
            propScanner
        );

  }

  public static MetaDataScanner createInstance() {
    return createInstance(getConfigUrls());
  }

  public static MetaDataScanner createInstance(List<URL> urls) {
    registerUrlTypeHandlers();

    final DeploymentContext ctx = new DeploymentContext(urls);
    final List<URL> actualUrls = ctx.process();
    final MetaDataScanner scanner = new MetaDataScanner(actualUrls);
    ctx.close(); // needs to closed after the scanner was created

    return scanner;
  }

  private static void registerUrlTypeHandlers() {
    List<Vfs.UrlType> urlTypes = Vfs.getDefaultUrlTypes();
    urlTypes.add(new VfsUrlType());
    urlTypes.add(new WarUrlType());

    // thread safe?
    Vfs.setDefaultURLTypes(urlTypes);
  }

  public Set<Class<?>> getTypesAnnotatedWithExcluding(
      Class<? extends Annotation> annotation, String excludeRegex) {
    Pattern p = Pattern.compile(excludeRegex);

    final Set<String> result = new java.util.HashSet<String>();
    final Set<String> types = getStore().getTypesAnnotatedWith(annotation.getName());
    for (String className : types) {
      if (!p.matcher(className).matches())
        result.add(className);
    }

    return ImmutableSet.copyOf(forNames(result));
  }

  public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation, Collection<String> packages) {
    final Set<Class<?>> results = new java.util.HashSet<Class<?>>();
    for (Class<?> cls : getTypesAnnotatedWith(annotation)) {
      if (packages.contains(cls.getPackage().getName())) {
        results.add(cls);
      }
    }
    return results;
  }

  public Set<Method> getMethodsAnnotatedWith(Class<? extends Annotation> annotation, Collection<String> packages) {
    final Set<Method> results = new java.util.HashSet<Method>();
    for (Method method : getMethodsAnnotatedWith(annotation)) {
      if (packages.contains(method.getDeclaringClass().getPackage().getName())) {
        results.add(method);
      }
    }
    return results;
  }

  public Set<Field> getFieldsAnnotatedWith(Class<? extends Annotation> annotation, Collection<String> packages) {
    final Set<Field> results = new java.util.HashSet<Field>();
    for (Field field : getFieldsAnnotatedWith(annotation)) {
      if (packages.contains(field.getDeclaringClass().getPackage().getName())) {
        results.add(field);
      }
    }
    return results;
  }

  private Map<Class<? extends Annotation>, Set<Class<?>>> _annotationCache =
      new HashMap<Class<? extends Annotation>, Set<Class<?>>>();

  @Override
  public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
    Set<Class<?>> types = _annotationCache.get(annotation);
    if (types == null) {
      types = new java.util.HashSet<Class<?>>(super.getTypesAnnotatedWith(annotation));

      if (annotation.isAnnotationPresent(Inherited.class)) {
        for (Class<?> cls : new ArrayList<Class<?>>(types)) {
          types.addAll(getSubTypesOf(cls));
        }
      }

      _annotationCache.put(annotation, types);
    }

    return types;
  }

  public String getHashForTypesAnnotatedWith(String seed, Class<? extends Annotation> annotation) {
    if (!annotationsToClassFile.containsKey(annotation.getName())) {
      return "0";
    }
    else {
      try {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");

        if (seed != null) {
          md.update(seed.getBytes());
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (SortableClassFileWrapper classFileWrapper : annotationsToClassFile.get(annotation.getName())) {
          byteArrayOutputStream.reset();
          DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
          classFileWrapper.getClassFile().write(dataOutputStream);
          dataOutputStream.flush();
          md.update(byteArrayOutputStream.toByteArray());
        }

        return RebindUtils.hashToHexString(md.digest());

      }
      catch (Exception e) {
        throw new RuntimeException("could not generate hash", e);
      }
    }
  }

  public static List<URL> getConfigUrls(ClassLoader loader) {
    try {
      final String[] targetRoots = {"", "META-INF/"};
      final List<URL> urls = new ArrayList<URL>();

      for (int i = 0; i < targetRoots.length; i++) {
        String scanTarget = targetRoots[i] + CLASSPATH_ANCHOR_FILE;
        final Enumeration<URL> configTargets = loader.getResources(scanTarget);

        while (configTargets.hasMoreElements()) {
          URL url = configTargets.nextElement();

          try {
            Properties properties = new Properties();
            InputStream stream = url.openStream();
            try {
              properties.load(stream);

              if (properties.contains(ErraiAppAttribs.JUNIT_PACKAGE_ONLY)) {
                if ("true".equalsIgnoreCase(String.valueOf(properties.get(ErraiAppAttribs.JUNIT_PACKAGE_ONLY)))) {
                  continue;
                }
              }
            }
            finally {
              stream.close();
            }
          }
          catch (IOException e) {
            System.err.println("could not read properties file");
            e.printStackTrace();
          }


          String urlString = url.toExternalForm();

          urlString = urlString.substring(0, urlString.indexOf(scanTarget));
          // URLs returned by the classloader are UTF-8 encoded. The URLDecoder assumes
          // a HTML form encoded String, which is why we escape the plus symbols here.
          // Otherwise, they would be decoded into space characters.
          // The pound character still must not appear anywhere in the path!
          urls.add(new URL(URLDecoder.decode(urlString.replaceAll("\\+", "%2b"), "UTF-8")));
        }

      }
      return urls;
    }
    catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to scan configuration Url's", e);
    }
  }

  public static List<URL> getConfigUrls() {
    return getConfigUrls(MetaDataScanner.class.getClassLoader());
  }

  public Properties getProperties(String name) {
    return propScanner.getProperties().get(name);
  }
}
