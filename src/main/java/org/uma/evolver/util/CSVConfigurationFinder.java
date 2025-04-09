package org.uma.evolver.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class CSVConfigurationFinder {
  /**
   * Finds the line with the extreme (lowest or highest) value in a specified column.
   *
   * @param filePath Path to the headerless CSV file
   * @param columnIndex Zero-based index of the column to analyze
   * @param findLowest If true, finds the lowest value; if false, finds the highest
   * @return The entire line containing the extreme value
   * @throws IOException If there are issues reading the file
   * @throws IllegalArgumentException If column index is invalid or no valid values found
   */
  public int findExtremeValueLine(String filePath, int columnIndex, boolean findLowest)
      throws IOException {
    Objects.requireNonNull(filePath, "File path cannot be null");

    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
      throw new FileNotFoundException("CSV file not found: " + filePath);
    }

    int extremeLine = 0;
    int lineCounter = 0 ;
    float extremeValue = findLowest ? Float.MAX_VALUE : Float.MIN_VALUE;

    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;

      while ((line = reader.readLine()) != null) {
        String[] values = line.split(",");

        float value = Float.parseFloat(values[columnIndex].trim());

        // Update extreme value logic
        if ((findLowest && value < extremeValue) || (!findLowest && value > extremeValue)) {
          extremeValue = value;
          extremeLine = lineCounter;
        }
        lineCounter++ ;
      }
    }

    return extremeLine;
  }

  /**
   * Retrieves the contents of a specific line from a file
   * @param filePath Path to the file
   * @param lineNumber Line number to retrieve (1-based indexing)
   * @return The contents of the specified line
   * @throws IOException If there's an error reading the file
   * @throws IllegalArgumentException If line number is invalid
   */
  public String getLineFromFile(String filePath, int lineNumber) throws IOException {
    if (lineNumber < 0) {
      throw new IllegalArgumentException("Line number must not be negative");
    }

    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      int currentLineNumber = 0;

      while ((line = br.readLine()) != null) {
        if (currentLineNumber == lineNumber) {
          return line;
        }
        currentLineNumber++;
      }

      // If we've reached here, the line number is beyond the file's length
      throw new IllegalArgumentException("Line number exceeds file length");
    }
  }

  public static void main(String[] args) throws IOException {
    CSVConfigurationFinder csvValueFinder = new CSVConfigurationFinder() ;
    String funFileName = "FUN.NSGA-II.ZCAT1.EP.NHV.2000.csv" ;
    String varFileName = "VAR.NSGA-II.ZCAT1.EP.NHV.Conf.2000.csv" ;
    String dataPath ="RESULTS/AsyncNSGAII/ZCAT1/bias" ;
    String funPath = dataPath+"/"+funFileName ;
    int extremeLine = csvValueFinder.findExtremeValueLine(funPath, 1, true) ;
    System.out.println(extremeLine) ;

    String varPath = dataPath+"/"+varFileName ;
    String foundConfiguration = csvValueFinder.getLineFromFile(varPath, extremeLine) ;
    System.out.println(foundConfiguration) ;
  }
}
