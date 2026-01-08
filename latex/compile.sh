#!/bin/bash

# Script to compile LaTeX documents
# Usage: ./compile.sh [document_name]
# Available documents: any table_*.tex file (without .tex extension)
# If no document name is provided, compiles table_mopso.tex

DOCUMENT=${1:-table_mopso}

echo "Compiling ${DOCUMENT}.tex..."

# Check if pdflatex is available
if ! command -v pdflatex &> /dev/null; then
    echo "Error: pdflatex not found. Please install a LaTeX distribution (e.g., TeX Live, MiKTeX)"
    exit 1
fi

# Check if document exists
if [ ! -f "${DOCUMENT}.tex" ]; then
    echo "Error: ${DOCUMENT}.tex not found"
    echo "Available documents:"
    echo "  - table_mopso (default)"
    echo "  - table_nsgaiidouble"
    echo "  - table_nsgaiidoublefull"
    echo "  - table_moeaddouble"
    echo "  - Any other table_*.tex file"
    exit 1
fi

# Compile the document (run twice for proper references and TOC)
echo "First pass..."
pdflatex -interaction=nonstopmode "${DOCUMENT}.tex" > /dev/null

echo "Second pass..."
pdflatex -interaction=nonstopmode "${DOCUMENT}.tex" > /dev/null

# Clean up auxiliary files
rm -f *.aux *.log *.out *.toc *.lof *.lot

if [ -f "${DOCUMENT}.pdf" ]; then
    echo "Successfully compiled ${DOCUMENT}.pdf"
    
    # Show file size
    SIZE=$(ls -lh "${DOCUMENT}.pdf" | awk '{print $5}')
    echo "File size: ${SIZE}"
    
    # Open the PDF if on macOS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "Opening PDF..."
        open "${DOCUMENT}.pdf"
    fi
else
    echo "Error: Compilation failed"
    echo "Check for LaTeX errors in the .log file"
    exit 1
fi