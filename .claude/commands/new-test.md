Create a test class skeleton for the given class, following §13 of JAVA_CODING_GUIDELINES.md exactly.

The class to test is: $ARGUMENTS

## Steps

### 1. Read the class under test

Read the full implementation. Identify:
- Its dependencies (to decide what to mock)
- Its public methods (to determine the `@Nested` groups to create)
- Its preconditions and error cases

### 2. Determine the package and path

The test goes under `src/test/java/` mirroring the same package as the class under test.
The file is named `<ClassName>Test.java`.

### 3. Create the test file

Required structure:

```java
@DisplayName("Unit tests for class <ClassName>")
class <ClassName>Test {

    private <ClassName> <instance>;
    // mocks for dependencies

    @BeforeEach
    void setUp() {
        // create mocks manually with mock()
        // instantiate the class under test
    }

    @Nested
    @DisplayName("When <scenario>")
    class <Scenario> {

        @Test
        @DisplayName("given <context>, when <action>, then <result>")
        void given<Context>_when<Action>_then<Result>() {
            // Arrange

            // Act

            // Assert
        }
    }
}
```

Rules from §13 that must be followed:
- Mocks **always** with `mock()` in `@BeforeEach`, never with `@Mock` annotations
- Method name pattern: `given[Context]_when[Action]_then[Result]`
- `@DisplayName` in lowercase: `"given ..., when ..., then ..."`
- One `@Nested` per method or relevant scenario
- `// Arrange / Act / Assert` comments inside each test
- For `assertThrows`: use `// Act & Assert`
- For multiple similar inputs, use `@ParameterizedTest` with `@ValueSource` or `@CsvSource`

### 4. Cover the minimum cases

For each public method:
- Happy path (valid input, expected result)
- Relevant boundary cases
- Error cases (null, empty, out of range)

### 5. Verify it compiles

Run `mvn test -Dtest=<ClassName>Test` and fix any compilation errors.
