package org.jboss.vergere.util;

import com.google.common.io.Files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Brock
 */
public class VergereUtils {

  private static String hashSeed = "vergere-zz1";


  private static volatile String _tempDirectory;

  public static String getTempDirectory() {
    if (_tempDirectory != null) {
      return _tempDirectory;
    }

    final File file = new File(System.getProperty("java.io.tmpdir") + "/vergere/" + getClasspathHash() + "/");

    if (!file.exists()) {
      file.mkdirs();
    }

    return _tempDirectory = file.getAbsolutePath();
  }

  private static volatile String _classpathHashCache;

  public static String getClasspathHash() {
    if (_hasClasspathChanged != null) {
      return _classpathHashCache;
    }

    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final String classPath = System.getProperty("java.class.path");

      md.update(hashSeed.getBytes());

      for (final String p : classPath.split(System.getProperty("path.separator"))) {
        _recurseDir(new File(p), new FileVisitor() {
          @Override
          public void visit(final File f) {
            md.update(f.getName().getBytes());
            md.update((byte) f.lastModified());
            md.update((byte) f.length());
          }
        });
      }

      return _classpathHashCache = hashToHexString(md.digest());
    }
    catch (Exception e) {
      throw new RuntimeException("failed to generate hash for classpath fingerprint", e);
    }
  }

  public static String hashToHexString(final byte[] hash) {
    final StringBuilder hexString = new StringBuilder();
    for (final byte mdbyte : hash) {
      hexString.append(Integer.toHexString(0xFF & mdbyte));
    }
    return hexString.toString();
  }

  public static File getApplicationCacheDirectory() {
    String cacheDir = System.getProperty("vergere.debugCacheDir");
    if (cacheDir == null) cacheDir = new File(".vergere/").getAbsolutePath();
    final File fileCacheDir = new File(cacheDir);
    fileCacheDir.mkdirs();
    return fileCacheDir;
  }

  private static boolean nocache = Boolean.getBoolean("vergere.nocache");
  private static Boolean _hasClasspathChanged;

  public static boolean hasClasspathChanged() {
    if (nocache) return true;
    if (_hasClasspathChanged != null) return _hasClasspathChanged;
    final File hashFile = new File(getApplicationCacheDirectory().getAbsolutePath() + "/classpath.sha");
    final String hashValue = VergereUtils.getClasspathHash();

    if (!hashFile.exists()) {
      writeStringToFile(hashFile, hashValue);
    }
    else {
      final String fileHashValue = readFileToString(hashFile);
      if (fileHashValue.equals(hashValue)) {
        return _hasClasspathChanged = true;
      }
      else {
        writeStringToFile(hashFile, hashValue);
      }
    }

    return _hasClasspathChanged = false;
  }

  private static Map<Class<? extends Annotation>, Boolean> _changeMapForAnnotationScope
      = new HashMap<Class<? extends Annotation>, Boolean>();

  public static boolean hasClasspathChangedForAnnotatedWith(final Set<Class<? extends Annotation>> annotations) {
    if (Boolean.getBoolean("vergere.forcecache")) return true;

    boolean result = false;
    for (final Class<? extends Annotation> a : annotations) {
      /**
       * We don't terminate prematurely, because we want to cache the hashes for the next run.
       */
      if (hasClasspathChangedForAnnotatedWith(a)) result = true;
    }


    return result;
  }

  public static boolean hasClasspathChangedForAnnotatedWith(final Class<? extends Annotation> annoClass) {
    if (nocache) return true;
    Boolean changed = _changeMapForAnnotationScope.get(annoClass);
    if (changed == null) {
      final File hashFile = new File(getApplicationCacheDirectory().getAbsolutePath() + "/"
          + annoClass.getName().replaceAll("\\.", "_") + ".sha");

      final MetaDataScanner scanner = ClassScanner.getScanner();
      final String hash = scanner.getHashForTypesAnnotatedWith(hashSeed, annoClass);

      if (!hashFile.exists()) {
        writeStringToFile(hashFile, hash);
        changed = Boolean.TRUE;
      }
      else {
        final String fileHashValue = readFileToString(hashFile);
        if (fileHashValue.equals(hash)) {
          _changeMapForAnnotationScope.put(annoClass, changed = Boolean.FALSE);
        }
        else {
          writeStringToFile(hashFile, hash);
          _changeMapForAnnotationScope.put(annoClass, changed = Boolean.TRUE);
        }
      }

    }
    return changed;
  }

  public static void writeStringToFile(final File file, final String data) {
    try {
      final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file, false));
      outputStream.write(data.getBytes("UTF-8"));
      outputStream.close();
    }
    catch (IOException e) {
      throw new RuntimeException("could not write file for debug cache", e);
    }
  }

  public static String readFileToString(final File file) {
    try {
      return Files.toString(file, Charset.forName("UTF-8"));
    }
    catch (IOException e) {
      throw new RuntimeException("could not read file for debug cache", e);
    }
  }

  public static String packageNameToDirName(final String pkg) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < pkg.length(); i++) {
      if (pkg.charAt(i) == '.') {
        sb.append(File.separator);
      }
      else {
        sb.append(pkg.charAt(i));
      }
    }
    return sb.toString();
  }

  private interface FileVisitor {
    public void visit(File f);
  }

  private static void _recurseDir(final File f, final FileVisitor visitor) {
    if (f.isDirectory()) {
      for (final File file : f.listFiles()) {
        _recurseDir(file, visitor);
      }
    }
    else {
      visitor.visit(f);
    }
  }
}
