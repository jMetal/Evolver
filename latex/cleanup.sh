#!/bin/bash

# Script to clean up LaTeX temporary files
# Usage: ./cleanup.sh

echo "Cleaning up LaTeX temporary files..."

# Remove common LaTeX auxiliary files
find . -name "*.aux" -delete
find . -name "*.log" -delete
find . -name "*.out" -delete
find . -name "*.toc" -delete
find . -name "*.lof" -delete
find . -name "*.lot" -delete
find . -name "*.fls" -delete
find . -name "*.fdb_latexmk" -delete
find . -name "*.synctex.gz" -delete
find . -name "*.bbl" -delete
find . -name "*.blg" -delete
find . -name "*.idx" -delete
find . -name "*.ind" -delete
find . -name "*.ilg" -delete
find . -name "*.nav" -delete
find . -name "*.snm" -delete
find . -name "*.vrb" -delete

echo "Cleanup complete!"

# Show remaining files
echo ""
echo "Remaining files in latex directory:"
ls -la