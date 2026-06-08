Crea el esqueleto de una clase de test para la clase indicada, siguiendo exactamente §13 de JAVA_CODING_GUIDELINES.md.

La clase a testear es: $ARGUMENTS

## Pasos

### 1. Leer la clase a testear

Lee la implementación completa de la clase. Identifica:
- Sus dependencias (para saber qué mockear)
- Sus métodos públicos (para determinar los `@Nested` a crear)
- Sus precondiciones y casos de error

### 2. Determinar el paquete y la ruta

El test va en `src/test/java/` replicando el mismo paquete que la clase bajo test.
El nombre del fichero es `<NombreClase>Test.java`.

### 3. Crear el fichero de test

Estructura obligatoria:

```java
@DisplayName("Unit tests for class <NombreClase>")
class <NombreClase>Test {

    private <NombreClase> <instancia>;
    // mocks de las dependencias

    @BeforeEach
    void setUp() {
        // crear mocks manualmente con mock()
        // instanciar la clase bajo test
    }

    @Nested
    @DisplayName("When <escenario>")
    class <Escenario> {

        @Test
        @DisplayName("given <contexto>, when <acción>, then <resultado>")
        void given<Contexto>_when<Acción>_then<Resultado>() {
            // Arrange

            // Act

            // Assert
        }
    }
}
```

Reglas de §13 que debes respetar:
- Mocks **siempre** con `mock()` en `@BeforeEach`, nunca con anotaciones `@Mock`
- Nombre de método: `given[Contexto]_when[Acción]_then[Resultado]`
- `@DisplayName` en minúsculas: `"given ..., when ..., then ..."`
- Un `@Nested` por método o escenario relevante
- Comentarios `// Arrange / Act / Assert` dentro de cada test
- Para `assertThrows`: usar `// Act & Assert`
- Si hay múltiples inputs similares, usar `@ParameterizedTest` con `@ValueSource` o `@CsvSource`

### 4. Cubrir los casos mínimos

Para cada método público:
- Happy path (entrada válida, resultado esperado)
- Casos límite relevantes
- Casos de error (null, vacío, fuera de rango)

### 5. Verificar que compila

Ejecuta `mvn test -Dtest=<NombreClase>Test` y corrige cualquier error de compilación.
