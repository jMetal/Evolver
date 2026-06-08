# Analysis A: HV Evolution Analysis

## Overview
This analysis generates hypervolume evolution figures for NSGA-II meta-optimization experiments, showing convergence patterns with min-max ranges for each problem and reference front type.

## Files
- `analysis_A_hv_evolution.py` - Main analysis script
- `README.md` - This documentation
- `requirements.txt` - Specific dependencies for this analysis
- Generated figure (after running script):
  - `hv_evolution_grid_2x2.png`

python analysis_A_hv_evolution.py
```

## Description
Generates a **single 2×2 grid figure** showing all configurations with enhanced convergence analysis:

### Figure Title
- **Main title**: "HV Evolution: RE3D vs RWA3D Problems"
# Analysis A: HV Evolution — instrucciones para regenerar la figura

Este documento explica exactamente cómo regenerar la figura actual `hv_evolution_grid_2x2.png` usada en el análisis A (HV evolution). Incluye dependencias, pasos, estructura de datos requerida y parámetros que se pueden ajustar.

## Requisitos previos
- Python 3.8+ (se probó con Python 3.11)
- Entorno con las dependencias del proyecto (recomendado: conda environment `evolver`)

Instalar dependencias (desde el directorio del script):

```bash
cd scripts/analysis_A_hv_evolution
pip install -r requirements.txt
```

O usando conda (si existe el ambiente):

```bash
conda activate evolver
pip install -r requirements.txt
```

## Estructura de datos esperada
El script lee resultados en:

- `../../experimentation/training/referenceFronts/`
- `../../experimentation/training/extremePoints/`

Cada experimento debe estar organizado como:

```
{PROBLEM}.{dataset_name}.{budget}/run{run_number}/VAR_CONF.txt
```

Ejemplo válido:

```
RE3D.referenceFronts.1000/run1/VAR_CONF.txt
```

El parser extrae bloques marcados por `# Evaluation: <num>` y líneas con `HVMinus=` dentro de cada bloque.

## Cómo regenerar la figura (comando)

Desde el directorio `scripts/analysis_A_hv_evolution` ejecutar:

```bash
python3 analysis_A_hv_evolution.py
```

Salida esperada:

- `hv_evolution_grid_2x2.png` (guardado en el mismo directorio del script)

Para abrir la imagen en macOS:

```bash
open hv_evolution_grid_2x2.png
```

## Comportamiento actual del script (detalles reproducibles)
- Rutas de datos: definidas en las constantes `DATASETS` al comienzo del script.
- Problemas procesados: `PROBLEMS = ["RE3D", "RWA3D"]`.
- Presupuestos analizados: `BUDGETS = [1000, 3000, 5000, 7000]`.
- Número de ejecuciones esperadas por configuración: `N_RUNS = 30`.
- Checkpoints: para cada ejecución el script reconstruye los puntos de evaluación desde `100` hasta `max_eval` de esa ejecución y genera `N_CHECKPOINTS = 30` evaluaciones igualmente espaciadas; cada serie se interpola por forward-fill a esos checkpoints.
- Estadística: se calcula la mediana, mínimo y máximo across runs en cada checkpoint.
- Convergencia 95%: se calcula como el primer checkpoint donde `HV >= HV_min + 0.95*(HV_max - HV_min)` sobre la curva mediana.
- Leyenda: el script fuerza la presencia de las 4 etiquetas de budget en cada subplot para consistencia visual.

## Parámetros que puedes cambiar
- `PROBLEMS`, `BUDGETS`, `N_RUNS`, `N_CHECKPOINTS` en la cabecera de `analysis_A_hv_evolution.py`.
- Rutas de entrada en `DATASETS` si tus datos están en otra carpeta.

## Errores comunes y soluciones
- Si faltan directorios o archivos `VAR_CONF.txt` el script imprime advertencias (`Warning: File not found`) y sigue; la figura se genera con los datos disponibles.
- Si los valores de HV aparecen negativos en tu origen, el parser espera `HVMinus=` y convierte a positivo (el formato actual de los `VAR_CONF.txt` usa `HVMinus=`). No cambies esto salvo que tus archivos usen otra notación.

## Notas finales
- El script produce una figura 2×2 con escalado Y independiente por problema y con texto de convergencia en cada línea. Si necesitas que la figura sea idéntica a una figura histórica llamada `hv_comparison_convergence.png` (2×4 grid u otro layout), indícalo y ajustaré el layout y las configuraciones necesarias.

Si tienes dudas sobre un caso concreto de datos, dime una ruta de ejemplo y lo verifico.
