package org.uma.evolver.cli.training;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Lists the file names available under a {@code src/main/resources/<directory>} resource
 * directory (e.g. {@code baseLevelConfigurations}), the same directories {@link
 * BaseLevelConfigurationReader}/{@link MetaOptimizerConfigurationReader}/{@link
 * BaseAlgorithmRegistry}'s {@code yamlParameterSpaceFile} resolve names against — so {@link
 * DescribeMain} can tell a caller which names are valid without it having to guess a naming
 * convention or ship its own copy of the list.
 *
 * <p>Two resolution tiers, mirroring the single-file lookup those readers already do: the source
 * tree (when running from a repo checkout, e.g. under {@code mvn test}) and, when that is not
 * available, the packaged fat jar {@link DescribeMain} itself is running from.
 */
final class ResourceDirectoryLister {

  private ResourceDirectoryLister() {}

  static List<String> list(String directory) {
    File sourceDirectory = new File("src/main/resources/" + directory);
    if (sourceDirectory.isDirectory()) {
      return listSourceDirectory(sourceDirectory);
    }
    return listFromJar(directory);
  }

  private static List<String> listSourceDirectory(File directory) {
    File[] files = directory.listFiles(File::isFile);
    if (files == null) {
      return List.of();
    }
    return java.util.Arrays.stream(files).map(File::getName).sorted().toList();
  }

  private static List<String> listFromJar(String directory) {
    URL location =
        ResourceDirectoryLister.class.getProtectionDomain().getCodeSource().getLocation();
    File jarFile;
    try {
      jarFile = new File(location.toURI());
    } catch (URISyntaxException e) {
      throw new JMetalException("Cannot resolve running jar location: " + e.getMessage());
    }
    if (!jarFile.isFile()) {
      return List.of();
    }

    String prefix = directory.endsWith("/") ? directory : directory + "/";
    List<String> names = new ArrayList<>();
    try (JarFile jar = new JarFile(jarFile)) {
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (!entry.isDirectory() && name.startsWith(prefix) && !name.substring(prefix.length()).contains("/")) {
          names.add(name.substring(prefix.length()));
        }
      }
    } catch (IOException e) {
      throw new JMetalException("Error reading jar " + jarFile + ": " + e.getMessage());
    }
    names.sort(String::compareTo);
    return names;
  }
}
