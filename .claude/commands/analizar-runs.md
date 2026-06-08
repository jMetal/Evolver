Analiza los resultados de una ejecución de meta-optimización en el directorio indicado.

Directorio a analizar: $ARGUMENTS

Si no se especifica directorio, usa `experimentation/`.

## Pasos

### 1. Explorar la estructura

Lista el contenido del directorio para entender qué experimentos hay:
- Subdirectorios por algoritmo/problema/configuración
- Ficheros de resultados (`.csv`, `.txt`, `.out`)
- Ficheros de indicadores de calidad

### 2. Identificar el tipo de resultados

Determina qué hay en los ficheros:
- **Configuraciones encontradas**: parámetros del algoritmo base (formato `--param value`)
- **Indicadores de calidad**: HV, EP, IGD por ejecución
- **Frentes de Pareto**: ficheros FUN/VAR

### 3. Calcular estadísticas por experimento

Para cada conjunto de resultados:
- Mediana y desviación estándar de cada indicador
- Mejor y peor valor
- Número de ejecuciones completadas vs esperadas

Muestra los resultados en una tabla Markdown:

| Algoritmo | Problema | Métrica | Mediana | Std | Min | Max | N |
|-----------|----------|---------|---------|-----|-----|-----|---|

### 4. Identificar la mejor configuración

Si hay ficheros de configuración:
- Localiza la configuración asociada al mejor valor del indicador principal
- Muestra los parámetros más relevantes (operadores, tamaños de población, probabilidades)

### 5. Detectar anomalías

Señala si hay:
- Ejecuciones sin terminar (ficheros vacíos o incompletos)
- Outliers significativos (valores > 3σ de la mediana)
- Directorios vacíos o con estructura inesperada

### 6. Resumen ejecutivo

Termina con 3-5 líneas indicando:
- Qué algoritmo/configuración obtuvo mejores resultados
- Si hay patrones claros en los parámetros de las mejores configuraciones
- Qué habría que investigar más
