package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FindClassInPackage {
  public static List<String> findClasses(String packageName) {
    String path = packageName.replace(".", "/");
    File directory =
        new File(FindClassInPackage.class.getClassLoader().getResource(path).getFile());
    List<String> classes = new ArrayList<>();

    if (directory.exists()) {
      for (File file : directory.listFiles()) {
        if (file.getName().endsWith(".class")) {
          String className = file.getName().substring(0, file.getName().length() - 6);
          classes.add(packageName + "." + className);
        }
      }
    }
    return classes;
  }

  public static void main(String[] args){
      List<String> classes = FindClassInPackage.findClasses("org.uma.evolver.parameter.catalogue.crossoverparameter.impl") ;
      classes.forEach(className -> System.out.println(
              className.substring(className.lastIndexOf(".")+1)
      ));
  }
}
