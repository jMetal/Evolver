#!/usr/bin/env python3
"""
Create compact LaTeX tables in the style of the AMOPSO paper example.
This version fixes the formatting issues and creates paper-ready tables.
"""

import yaml
import argparse
import os
from pathlib import Path
from typing import Dict, Any, List, Tuple
import re

class CompactLaTeXGenerator:
    def __init__(self):
        self.parameter_count = 0
        self.conditional_count = 0
        
    def escape_latex(self, text: str) -> str:
        """Escape special LaTeX characters in text."""
        text = text.replace('_', r'\_')
        text = text.replace('&', r'\&')
        text = text.replace('%', r'\%')
        text = text.replace('$', r'\$')
        text = text.replace('#', r'\#')
        text = text.replace('^', r'\textasciicircum{}')
        text = text.replace('~', r'\textasciitilde{}')
        return text
    
    def generate_compact_table(self, yaml_file: str, output_file: str = None) -> str:
        """Generate compact LaTeX table from YAML parameter space file."""
        
        # Reset counters
        self.parameter_count = 0
        self.conditional_count = 0
        
        # Load YAML file
        try:
            with open(yaml_file, 'r') as f:
                data = yaml.safe_load(f)
        except Exception as e:
            raise ValueError(f"Error loading YAML file {yaml_file}: {e}")
        
        # Extract parameters
        parameters = self._extract_parameters_recursive(data)
        
        if not parameters:
            raise ValueError(f"No parameters found in {yaml_file}")
        
        # Generate LaTeX table
        algorithm_name = Path(yaml_file).stem
        
        latex_content = self._generate_compact_latex(algorithm_name, parameters)
        
        # Write to file if specified
        if output_file:
            with open(output_file, 'w') as f:
                f.write(latex_content)
            print(f"Compact LaTeX table saved to {output_file}")
        
        return latex_content
    
    def _extract_parameters_recursive(self, data: Dict[str, Any], prefix: str = "", level: int = 0, condition_path: str = "") -> List[Tuple[str, str, str, str, str, str]]:
        """Extract parameters with full condition path for accurate inference."""
        parameters = []
        
        for key, value in data.items():
            if not isinstance(value, dict):
                continue
                
            current_name = f"{prefix}.{key}" if prefix else key
            
            # Check if this is a parameter definition
            if 'type' in value:
                self.parameter_count += 1
                param_type = value.get('type', 'unknown')
                param_range = self._format_range_or_values(value)
                
                is_conditional = level > 0
                if is_conditional:
                    self.conditional_count += 1
                
                parameters.append((key, param_type, param_range, prefix, str(level), condition_path))
                
                # Process conditional parameters
                if 'conditionalParameters' in value:
                    conditional_params = self._extract_parameters_recursive(
                        value['conditionalParameters'], current_name, level + 1, condition_path
                    )
                    parameters.extend(conditional_params)
                
                # Process global sub-parameters
                if 'globalSubParameters' in value:
                    global_params = self._extract_parameters_recursive(
                        value['globalSubParameters'], current_name, level + 1, condition_path
                    )
                    parameters.extend(global_params)
                
                # Process categorical values that might have conditional parameters
                if 'values' in value and isinstance(value['values'], dict):
                    for option_key, option_value in value['values'].items():
                        if isinstance(option_value, dict) and 'conditionalParameters' in option_value:
                            # Build condition path for this option
                            new_condition_path = f"{key}$=${option_key}"
                            option_params = self._extract_parameters_recursive(
                                option_value['conditionalParameters'], 
                                f"{current_name}.{option_key}", 
                                level + 1,
                                new_condition_path
                            )
                            parameters.extend(option_params)
            
            # Process categorical values that might have conditional parameters (standalone)
            elif 'values' in value and isinstance(value['values'], dict):
                self.parameter_count += 1
                param_type = 'categorical'
                param_range = self._format_range_or_values(value)
                
                is_conditional = level > 0
                if is_conditional:
                    self.conditional_count += 1
                
                parameters.append((key, param_type, param_range, prefix, str(level), condition_path))
                
                # Process each categorical option's conditional parameters
                for option_key, option_value in value['values'].items():
                    if isinstance(option_value, dict) and 'conditionalParameters' in option_value:
                        # Build condition path for this option
                        new_condition_path = f"{key}$=${option_key}"
                        option_params = self._extract_parameters_recursive(
                            option_value['conditionalParameters'], 
                            f"{current_name}.{option_key}", 
                            level + 1,
                            new_condition_path
                        )
                        parameters.extend(option_params)
                
                # Process global sub-parameters for categorical
                if 'globalSubParameters' in value:
                    global_params = self._extract_parameters_recursive(
                        value['globalSubParameters'], current_name, level + 1, condition_path
                    )
                    parameters.extend(global_params)
            
            # Recursively process nested structures that might contain parameters
            else:
                nested_params = self._extract_parameters_recursive(value, current_name, level, condition_path)
                parameters.extend(nested_params)
        
        return parameters
    
    def _format_range_or_values(self, param_info: Dict[str, Any]) -> str:
        """Format parameter range or categorical values."""
        if 'range' in param_info:
            range_val = param_info['range']
            if isinstance(range_val, list) and len(range_val) == 2:
                return f"[{range_val[0]}, {range_val[1]}]"
            else:
                return str(range_val)
        elif 'values' in param_info:
            values = param_info['values']
            if isinstance(values, list):
                # Include ALL values, no truncation
                return "{" + ", ".join(map(str, values)) + "}"
            elif isinstance(values, dict):
                keys = list(values.keys())
                # Include ALL keys, no truncation
                return "{" + ", ".join(keys) + "}"
        return "N/A"
    
    def _generate_compact_latex(self, algorithm_name: str, parameters: List[Tuple[str, str, str, str, str, str]]) -> str:
        """Generate compact LaTeX document."""
        
        # Process parameters into compact format with alignment
        table_rows = []
        max_domain_length = 0
        
        # Process rows with better alignment
        for param_name, param_type, param_range, parent, level, condition_path in parameters:
            level_int = int(level)
            
            # Format domain
            domain = self._format_compact_domain(param_type, param_range)
            
            # Determine condition using the full condition path
            condition = ""
            if level_int > 0 and condition_path:
                condition = condition_path
            elif level_int > 0:
                # Fallback to simple inference if condition_path is empty
                condition = self._infer_condition_simple(param_name, parent)
            
            # Create table row with consistent alignment for conditions
            if condition:
                # Use fixed spacing for alignment
                row = f"{self.escape_latex(param_name)} & {domain} \\hspace{{6em}}\\textbf{{if}} {condition}"
            else:
                row = f"{self.escape_latex(param_name)} & {domain}"
            
            table_rows.append(row)
        
        # Generate complete document
        latex = f"""\\documentclass[11pt,a4paper]{{article}}
\\usepackage[utf8]{{inputenc}}
\\usepackage[margin=2cm]{{geometry}}
\\usepackage{{booktabs}}
\\usepackage{{array}}
\\usepackage{{amsmath}}
\\usepackage{{amssymb}}
\\usepackage{{graphicx}}

% Define compact domain notation commands
\\newcommand{{\\Realdomain}}[1]{{\\ensuremath{{[#1]\\subset\\mathbb{{R}}}}}}
\\newcommand{{\\Intdomain}}[1]{{\\ensuremath{{[#1]\\subset\\mathbb{{N}}}}}}
\\newcommand{{\\Catdomain}}[1]{{\\ensuremath{{\\{{\\;}}#1\\ensuremath{{\\;\\}}}}}}

\\title{{Parameter Space for {self.escape_latex(algorithm_name)} Algorithm}}
\\author{{jMetal Evolver Framework}}
\\date{{\\today}}

\\begin{{document}}

\\maketitle

\\section{{Parameter Space}}

\\begin{{table}}[!tb]
\\caption{{Parameter space of {self.escape_latex(algorithm_name)}.}}
\\label{{tab:params_{algorithm_name.lower()}}}
\\centering
\\resizebox{{\\textwidth}}{{!}}{{%
\\small
\\begin{{tabular}}{{r@{{\\hskip 1em}}p{{25em}}}}
\\toprule
\\bf Parameter & \\bf Domain \\\\
\\midrule
"""
        
        # Add table rows
        for row in table_rows:
            latex += f"{row} \\\\\n"
        
        # Complete the document
        latex += f"""\\bottomrule
\\end{{tabular}}}}
\\end{{table}}

\\section{{Summary}}

This parameter space contains {self.parameter_count} parameters total, of which {self.conditional_count} are conditional parameters that are only active under specific conditions.

\\subsection{{Domain Notation}}

\\begin{{itemize}}
\\item \\texttt{{\\textbackslash Realdomain{{a, b}}}}: Real-valued parameters in range [a, b] $\\subset \\mathbb{{R}}$
\\item \\texttt{{\\textbackslash Intdomain{{a, b}}}}: Integer-valued parameters in range [a, b] $\\subset \\mathbb{{N}}$
\\item \\texttt{{\\textbackslash Catdomain{{opt1, opt2, ...}}}}: Categorical parameters with discrete options
\\end{{itemize}}

\\end{{document}}
"""
        return latex
    
    def _format_compact_domain(self, param_type: str, param_range: str) -> str:
        """Format parameter domain in compact notation."""
        if param_type == 'integer':
            if param_range.startswith('[') and param_range.endswith(']'):
                range_content = param_range[1:-1]
                return f"\\Intdomain{{{range_content}}}"
        elif param_type == 'double':
            if param_range.startswith('[') and param_range.endswith(']'):
                range_content = param_range[1:-1]
                return f"\\Realdomain{{{range_content}}}"
        elif param_type == 'categorical':
            if param_range.startswith('{') and param_range.endswith('}'):
                options_content = param_range[1:-1]
                return f"\\Catdomain{{{options_content}}}"
        
        # Handle cases where param_range is already formatted but param_type is not detected properly
        if param_range.startswith('[') and param_range.endswith(']'):
            range_content = param_range[1:-1]
            # Try to determine if it's integer or real based on content
            try:
                values = [float(x.strip()) for x in range_content.split(',')]
                if len(values) == 2:
                    if all(v.is_integer() for v in values):
                        return f"\\Intdomain{{{range_content}}}"
                    else:
                        return f"\\Realdomain{{{range_content}}}"
            except:
                pass
        
        return param_range
    
    def _infer_condition_simple(self, param_name: str, parent: str) -> str:
        """Simple condition inference based on parameter and parent names."""
        param_lower = param_name.lower()
        parent_lower = parent.lower() if parent else ""
        
        # Archive conditions
        if ('archive' in param_lower and 'external' not in param_lower) or 'swarmsize' in param_lower:
            return "algorithmResult$=$externalArchive"
        
        # Mutation conditions
        if 'uniform' in param_lower and 'nonuniform' not in param_lower and ('mutation' in parent_lower or 'perturbation' in param_lower):
            return "mutation$=$uniform"
        elif 'polynomial' in param_lower and 'linked' not in param_lower and 'mutation' in parent_lower:
            return "mutation$=$polynomial"
        elif 'linkedpolynomial' in param_lower:
            return "mutation$=$linkedPolynomial"
        elif 'nonuniform' in param_lower and 'mutation' in parent_lower:
            return "mutation$=$nonUniform"
        
        # Crossover conditions
        if 'sbx' in param_lower:
            return "crossover$=$SBX"
        elif 'blxalpha' in param_lower:
            return "crossover$=$blxAlpha"
        
        # Selection conditions
        if 'tournament' in param_lower:
            return "selection$=$tournament"
        
        # Inertia weight conditions
        if 'random' in param_lower and 'inertiaweight' in param_lower.replace('_', ''):
            return "inertiaWeightComputingStrategy$=$randomSelectedValue"
        elif 'linearincreasing' in param_lower.replace('_', ''):
            return "inertiaWeightComputingStrategy$=$linearIncreasingValue"
        elif 'lineardecreasing' in param_lower.replace('_', ''):
            return "inertiaWeightComputingStrategy$=$linearDecreasingValue"
        
        return ""

def main():
    parser = argparse.ArgumentParser(description='Generate compact LaTeX parameter tables')
    parser.add_argument('yaml_file', nargs='?', help='YAML parameter space file to convert')
    parser.add_argument('--all', action='store_true', help='Convert all YAML files')
    parser.add_argument('--yaml-dir', default='../src/main/resources/parameterSpaces')
    parser.add_argument('--output-dir', default='.')
    
    args = parser.parse_args()
    
    generator = CompactLaTeXGenerator()
    
    if args.all:
        yaml_path = Path(args.yaml_dir)
        output_path = Path(args.output_dir)
        output_path.mkdir(exist_ok=True)
        
        yaml_files = list(yaml_path.glob("*.yaml"))
        
        for yaml_file in yaml_files:
            try:
                algorithm_name = yaml_file.stem
                output_file = output_path / f"table_{algorithm_name.lower()}.tex"
                generator.generate_compact_table(str(yaml_file), str(output_file))
            except Exception as e:
                print(f"Error converting {yaml_file.name}: {e}")
    
    elif args.yaml_file:
        output_file = f"table_{Path(args.yaml_file).stem.lower()}.tex"
        generator.generate_compact_table(args.yaml_file, output_file)
    
    else:
        parser.print_help()

if __name__ == "__main__":
    main()