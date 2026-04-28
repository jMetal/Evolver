write("", "HV.Wilcoxon.tex",append=FALSE)
resultDirectory<-"../data"
latexHeader <- function() {
  write("\\documentclass{article}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\title{StandardStudy}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\usepackage{amssymb}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\author{A.J.Nebro}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\begin{document}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\maketitle", "HV.Wilcoxon.tex", append=TRUE)
  write("\\section{Tables}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\", "HV.Wilcoxon.tex", append=TRUE)
}

latexTableHeader <- function(problem, tabularString, latexTableFirstLine) {
  write("\\begin{table}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\caption{", "HV.Wilcoxon.tex", append=TRUE)
  write(problem, "HV.Wilcoxon.tex", append=TRUE)
  write(".HV.}", "HV.Wilcoxon.tex", append=TRUE)

  write("\\label{Table:", "HV.Wilcoxon.tex", append=TRUE)
  write(problem, "HV.Wilcoxon.tex", append=TRUE)
  write(".HV.}", "HV.Wilcoxon.tex", append=TRUE)

  write("\\centering", "HV.Wilcoxon.tex", append=TRUE)
  write("\\setlength\\tabcolsep{1pt}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\begin{scriptsize}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\begin{tabular}{", "HV.Wilcoxon.tex", append=TRUE)
  write(tabularString, "HV.Wilcoxon.tex", append=TRUE)
  write("}", "HV.Wilcoxon.tex", append=TRUE)
  write(latexTableFirstLine, "HV.Wilcoxon.tex", append=TRUE)
  write("\\hline ", "HV.Wilcoxon.tex", append=TRUE)
}

printTableLine <- function(indicator, algorithm1, algorithm2, i, j, problem) { 
  file1<-paste(resultDirectory, algorithm1, sep="/")
  file1<-paste(file1, problem, sep="/")
  file1<-paste(file1, indicator, sep="/")
  data1<-scan(file1)
  file2<-paste(resultDirectory, algorithm2, sep="/")
  file2<-paste(file2, problem, sep="/")
  file2<-paste(file2, indicator, sep="/")
  data2<-scan(file2)
  if (i == j) {
    write("--", "HV.Wilcoxon.tex", append=TRUE)
  }
  else if (i < j) {
    if (is.finite(wilcox.test(data1, data2)$p.value) & wilcox.test(data1, data2)$p.value <= 0.05) {
      if (median(data1) >= median(data2)) {
        write("$\\blacktriangle$", "HV.Wilcoxon.tex", append=TRUE)
}
      else {
        write("$\\triangledown$", "HV.Wilcoxon.tex", append=TRUE)
}
}
    else {
      write("$-$", "HV.Wilcoxon.tex", append=TRUE)
}
  }
  else {
    write(" ", "HV.Wilcoxon.tex", append=TRUE)
  }
}

latexTableTail <- function() { 
  write("\\hline", "HV.Wilcoxon.tex", append=TRUE)
  write("\\end{tabular}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\end{scriptsize}", "HV.Wilcoxon.tex", append=TRUE)
  write("\\end{table}", "HV.Wilcoxon.tex", append=TRUE)
}

latexTail <- function() { 
  write("\\end{document}", "HV.Wilcoxon.tex", append=TRUE)
}

### START OF SCRIPT 
# Constants
problemList <-c("RWA1", "RWA2", "RWA3", "RWA4", "RWA5", "RWA6", "RWA7", "RWA8", "RWA9", "RWA10") 
algorithmList <-c("NSGAII-Standard", "Extreme-RWA3D", "Extreme-RWA3D-ArchiveReset", "Extreme-RWA3D-CrossoverReset", "Extreme-RWA3D-MutationReset") 
tabularString <-c("lcccc") 
latexTableFirstLine <-c("\\hline  & Extreme-RWA3D & Extreme-RWA3D-ArchiveReset & Extreme-RWA3D-CrossoverReset & Extreme-RWA3D-MutationReset\\\\ ") 
indicator<-"HV"

 # Step 1.  Writes the latex header
latexHeader()
tabularString <-c("| l | cccccccccc | cccccccccc | cccccccccc | cccccccccc | ") 

latexTableFirstLine <-c("\\hline \\multicolumn{1}{|c|}{} & \\multicolumn{10}{c|}{Extreme-RWA3D} & \\multicolumn{10}{c|}{Extreme-RWA3D-ArchiveReset} & \\multicolumn{10}{c|}{Extreme-RWA3D-CrossoverReset} & \\multicolumn{10}{c|}{Extreme-RWA3D-MutationReset} \\\\") 

# Step 3. Problem loop 
latexTableHeader("RWA1 RWA2 RWA3 RWA4 RWA5 RWA6 RWA7 RWA8 RWA9 RWA10 ", tabularString, latexTableFirstLine)

indx = 0
for (i in algorithmList) {
  if (i != "Extreme-RWA3D-MutationReset") {
    write(i , "HV.Wilcoxon.tex", append=TRUE)
    write(" & ", "HV.Wilcoxon.tex", append=TRUE)

    jndx = 0
    for (j in algorithmList) {
      for (problem in problemList) {
        if (jndx != 0) {
          if (i != j) {
            printTableLine(indicator, i, j, indx, jndx, problem)
          }
          else {
            write("  ", "HV.Wilcoxon.tex", append=TRUE)
          } 
          if (problem == "RWA10") {
            if (j == "Extreme-RWA3D-MutationReset") {
              write(" \\\\ ", "HV.Wilcoxon.tex", append=TRUE)
            } 
            else {
              write(" & ", "HV.Wilcoxon.tex", append=TRUE)
            }
          }
     else {
    write("&", "HV.Wilcoxon.tex", append=TRUE)
     }
        }
      }
      jndx = jndx + 1
}
    indx = indx + 1
  }
} # for algorithm

  latexTableTail()

#Step 3. Writes the end of latex file 
latexTail()

