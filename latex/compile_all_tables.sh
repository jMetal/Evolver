#!/bin/bash

# Script to compile all individual table LaTeX documents
# This will generate a PDF for each algorithm's parameter space

echo "Compiling all parameter space table documents..."

# Check if pdflatex is available
if ! command -v pdflatex &> /dev/null; then
    echo "Error: pdflatex not found. Please install a LaTeX distribution (e.g., TeX Live, MiKTeX)"
    exit 1
fi

# Find all table_*.tex files
TABLE_FILES=(table_*.tex)

if [ ${#TABLE_FILES[@]} -eq 0 ]; then
    echo "No table_*.tex files found"
    exit 1
fi

# Compile each table file
SUCCESS_COUNT=0
TOTAL_COUNT=0

for tex_file in "${TABLE_FILES[@]}"; do
    if [ -f "$tex_file" ]; then
        TOTAL_COUNT=$((TOTAL_COUNT + 1))
        echo "Compiling $tex_file..."
        
        # Compile twice for proper references
        pdflatex -interaction=nonstopmode "$tex_file" > /dev/null 2>&1
        pdflatex -interaction=nonstopmode "$tex_file" > /dev/null 2>&1
        
        # Check if PDF was created
        pdf_file="${tex_file%.tex}.pdf"
        if [ -f "$pdf_file" ]; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            SIZE=$(ls -lh "$pdf_file" | awk '{print $5}')
            echo "  ✓ Successfully created $pdf_file (${SIZE})"
        else
            echo "  ✗ Failed to create $pdf_file"
        fi
    fi
done

# Clean up auxiliary files
echo "Cleaning up auxiliary files..."
rm -f *.aux *.log *.out *.toc *.lof *.lot

echo ""
echo "Compilation complete!"
echo "Successfully compiled: $SUCCESS_COUNT/$TOTAL_COUNT documents"

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    echo "All documents compiled successfully!"
    
    # List all generated PDFs
    echo ""
    echo "Generated PDF files:"
    for pdf in table_*.pdf; do
        if [ -f "$pdf" ]; then
            SIZE=$(ls -lh "$pdf" | awk '{print $5}')
            echo "  - $pdf (${SIZE})"
        fi
    done
else
    echo "Some documents failed to compile. Check the .log files for errors."
fi