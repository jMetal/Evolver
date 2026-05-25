# JAVA_CODING_GUIDELINES.md

## Purpose
This document defines Java coding standards for projects using Java 21+ and Maven. These guidelines work in conjunction with `AGENTS.md` and should be followed by all contributors and AI assistants.

---

## 1. Records for DTOs and Value Objects

### ✅ DO
- Use `record` for immutable data classes
- Add validations in compact constructor when necessary
- Leverage automatic generation of constructor, getters, `equals()`, `hashCode()`, and `toString()`

```java
public record UserDTO(String name, String email) {
    public UserDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }
    }
}
```

### ❌ DON'T
- Create verbose classes with manual getters/setters/equals/hashCode
- Use mutable classes for simple data transfer objects

---

## 2. Sealed Classes for Controlled Hierarchies

### ✅ DO
- Use `sealed interface/class` for known and limited type hierarchies
- Combine with pattern matching in switch expressions
- Explicitly permit all subtypes

```java
public sealed interface PaymentMethod permits CreditCard, PayPal, BankTransfer {}

public record CreditCard(String number) implements PaymentMethod {}
public record PayPal(String email) implements PaymentMethod {}
public record BankTransfer(String iban) implements PaymentMethod {}
```

### ❌ DON'T
- Use open hierarchies with cascading `instanceof` checks
- Allow unlimited subclassing when types are known

---

## 3. Pattern Matching and Switch Expressions

### ✅ DO
- Use pattern matching with `switch` and `instanceof`
- Use switch expressions that return values
- Leverage exhaustiveness checking with sealed types

```java
String processPayment(PaymentMethod method) {
    return switch (method) {
        case CreditCard(var number) -> "Processing credit card: " + number;
        case PayPal(var email) -> "Processing PayPal: " + email;
        case BankTransfer(var iban) -> "Processing transfer: " + iban;
    };
}
```

### ❌ DON'T
- Use if-else chains with `instanceof` and manual casting
- Use traditional switch statements when expressions are cleaner

---

## 4. Optional Instead of null

### ✅ DO
- Return `Optional<T>` when a value may be absent
- Use fluent API: `.map()`, `.filter()`, `.flatMap()`, `.orElse()`, `.orElseThrow()`
- Make nullability explicit in method signatures

```java
Optional<User> findUserById(String id) {
    return userRepository.findById(id);
}

// Usage
String userName = findUserById("123")
    .map(User::name)
    .orElse("Unknown");
```

### ❌ DON'T
- Return `null` from methods
- Use manual null checks with `if (x == null)`
- Use `Optional` for fields or method parameters

---

## 5. Streams API

### ✅ DO
- Use streams for collection operations
- Prefer declarative over imperative style
- Use method references when possible
- Keep stream pipelines readable (max 3-4 operations)

```java
List<String> activeUserNames = users.stream()
    .filter(User::isActive)
    .map(User::name)
    .toList();
```

### ❌ DON'T
- Use for/while loops for simple transformations
- Create overly complex stream chains that harm readability
- Use streams for simple iterations (prefer enhanced for-loop)

---

## 6. Try-with-Resources

### ✅ DO
- Use try-with-resources for ALL closeable resources
- Use `var` to reduce verbosity when type is obvious
- Stack multiple resources in one try statement

```java
try (var connection = dataSource.getConnection();
     var statement = connection.prepareStatement(sql);
     var resultSet = statement.executeQuery()) {
    // Process results
}
```

### ❌ DON'T
- Close resources manually with `finally` blocks
- Forget to close any `AutoCloseable` resource

---

## 7. Single Return Point + Guard Clauses

### ✅ DO
- Each method has ONE return statement (at the end)
- Use flat if-else for validations (no nesting)
- Declare result variable at the beginning
- Use early validation with guard clauses when needed

```java
public String processOrder(Order order) {
    String result;
    
    if (order == null) {
        result = "Invalid order";
    } else if (!order.isValid()) {
        result = "Order validation failed";
    } else if (order.isEmpty()) {
        result = "Empty order";
    } else {
        result = fulfillOrder(order);
    }
    
    return result;
}
```

### ❌ DON'T
- Use multiple `return` statements scattered throughout the method
- Create nested if-else pyramids (pyramid of doom)
- Mix validation logic with business logic

---

## 8. Single Responsibility Principle

### ✅ DO
- Each method does ONE thing
- Keep cognitive complexity low (max 10)
- Use descriptive names that explain the purpose
- Extract complex logic into separate methods
- Aim for methods under 20 lines

```java
// Good: Each method has a single, clear responsibility
public void registerUser(UserDTO dto) {
    validateUserData(dto);
    User user = createUser(dto);
    saveUser(user);
    sendWelcomeEmail(user);
}
```

### ❌ DON'T
- Create methods that do validation + logic + email + logging all in one
- Write "god methods" that handle multiple responsibilities

---

## 9. Complete Javadoc

### ✅ DO
- Document all public classes, interfaces, and methods
- Include description, parameters, return values, and exceptions
- Use `@param`, `@return`, `@throws` tags appropriately
- Write clear, concise descriptions

```java
/**
 * Retrieves a user by their unique identifier.
 *
 * @param userId the unique identifier of the user
 * @return an Optional containing the user if found, empty otherwise
 * @throws IllegalArgumentException if userId is null or blank
 */
public Optional<User> findUserById(String userId) {
    if (userId == null || userId.isBlank()) {
        throw new IllegalArgumentException("User ID cannot be null or blank");
    }
    return userRepository.findById(userId);
}
```

### ❌ DON'T
- Leave public APIs undocumented
- Write vague or redundant documentation
- Forget to update Javadoc when changing method signatures

---

## 10. Specific Exceptions

### ✅ DO
- Create custom exceptions for different error types
- Provide descriptive messages with context
- Include relevant data in exception messages
- Use checked exceptions for recoverable errors
- Use unchecked exceptions for programming errors

```java
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userId) {
        super("User not found with ID: " + userId);
    }
}

public class InvalidEmailException extends IllegalArgumentException {
    public InvalidEmailException(String email) {
        super("Invalid email format: " + email);
    }
}
```

### ❌ DON'T
- Throw generic `Exception` or `RuntimeException`
- Use exceptions for control flow
- Swallow exceptions without logging or handling

---

## 11. Immutability by Default

### ✅ DO
- Make fields `final` whenever possible
- Use immutable collections: `List.of()`, `Set.of()`, `Map.of()`
- Return defensive copies of mutable collections if necessary

```java
public class OrderService {
    private final OrderRepository repository;
    private final List<String> allowedStatuses = List.of("PENDING", "APPROVED", "REJECTED");
    
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
    
    public List<String> getAllowedStatuses() {
        return allowedStatuses; // Already immutable, safe to return
    }
}
```

### ❌ DON'T
- Expose mutable collections from methods
- Use mutable fields when immutable would suffice

---

## 12. Variable Naming with `var`

### ✅ DO
- Use `var` when type is obvious from context
- Keep variable names descriptive when using `var`
- Use `var` for complex generic types to improve readability

```java
var users = userRepository.findAll(); // Type is clear from method name
var connection = dataSource.getConnection(); // Type is obvious
var result = new HashMap<String, List<UserDTO>>(); // Reduces verbosity
```

### ❌ DON'T
- Use `var` for primitives or when type is ambiguous
- Use `var` with unclear initializers like `var x = getResult();`

---

## 13. Maven Project Structure

### ✅ DO
- Follow standard Maven directory layout
- Keep `pom.xml` organized with properties for versions
- Use dependency management for version control
- Group dependencies logically with comments

```xml
<properties>
    <java.version>21</java.version>
    <junit.version>5.10.1</junit.version>
    <spring.version>6.1.0</spring.version>
</properties>

<dependencies>
    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>${junit.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### ❌ DON'T
- Mix build logic with source code
- Hardcode dependency versions throughout the POM
- Leave dependencies without scope definition

---

## 14. Testing

### ✅ DO
- Write tests using JUnit 6 (Jupiter)
- Use Given-When-Then naming convention for test methods and `@DisplayName`
- Use `@Nested` classes to group related tests
- Follow AAA pattern (Arrange, Act, Assert) for internal test structure
- Declare the class under test as an uninitialized field; create its instance in `@BeforeEach void setUp()`
- Use field initializers only for immutable test constants (`private final List<X> VALUES = List.of(...)`)
- Aim for high code coverage (>80%)
- Test both happy paths and edge cases

```java
@DisplayName("Unit tests for class UserService")
class UserServiceTest {
    
    private UserService userService;
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }
    
    @Nested
    @DisplayName("When finding user by ID")
    class FindUserById {
        
        @Test
        @DisplayName("given valid ID, when user exists, then return user")
        void givenValidId_whenUserExists_thenReturnUser() {
            // Arrange
            String userId = "123";
            User expectedUser = new User(userId, "John Doe");
            when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));
            
            // Act
            Optional<User> result = userService.findUserById(userId);
            
            // Assert
            assertTrue(result.isPresent());
            assertEquals(expectedUser, result.get());
        }
        
        @Test
        @DisplayName("given valid ID, when user does not exist, then return empty")
        void givenValidId_whenUserDoesNotExist_thenReturnEmpty() {
            // Arrange
            String userId = "999";
            when(userRepository.findById(userId)).thenReturn(Optional.empty());
            
            // Act
            Optional<User> result = userService.findUserById(userId);
            
            // Assert
            assertTrue(result.isEmpty());
        }
        
        @Test
        @DisplayName("given null ID, when finding user, then throw IllegalArgumentException")
        void givenNullId_whenFindingUser_thenIllegalArgumentExceptionIsThrown() {
            // Arrange
            Executable executable = () -> userService.findUserById(null);
            
            // Act & Assert
            assertThrows(IllegalArgumentException.class, executable);
        }
    }
    
    @Nested
    @DisplayName("When creating a new user")
    class CreateUser {
        
        @Test
        @DisplayName("given valid data, when creating user, then save and return user")
        void givenValidData_whenCreatingUser_thenUserIsSavedAndReturned() {
            // Arrange
            UserDTO dto = new UserDTO("Jane Doe", "jane@example.com");
            User expectedUser = new User("456", "Jane Doe");
            when(userRepository.save(any(User.class))).thenReturn(expectedUser);
            
            // Act
            User result = userService.createUser(dto);
            
            // Assert
            assertNotNull(result);
            assertEquals(expectedUser.name(), result.name());
            verify(userRepository, times(1)).save(any(User.class));
        }
        
        @Test
        @DisplayName("given invalid email, when creating user, then throw InvalidEmailException")
        void givenInvalidEmail_whenCreatingUser_thenInvalidEmailExceptionIsThrown() {
            // Arrange
            Executable executable = () -> userService.createUser(new UserDTO("Jane Doe", "invalid-email"));
            
            // Act & Assert
            assertThrows(InvalidEmailException.class, executable);
        }
    }
}
```

### Test Naming Convention

Use **Given-When-Then** for method names and `@DisplayName`:
- **given**: Initial context/preconditions
- **when**: The action being tested
- **then**: Expected outcome

Method format: `given[Context]_when[Action]_then[Outcome]`  
`@DisplayName` format: `given [context], when [action], then [outcome]` (lowercase, sentence style)

### Internal structure: AAA

Inside each test method use `// Arrange / Act / Assert` comments to delimit the three phases.  
For tests where act and assert cannot be separated (e.g. `assertThrows`), use `// Act & Assert`.

### Using @Nested and @DisplayName

- Use `@Nested` to group tests by scenario or method under test
- `@DisplayName` on the class: `"Unit tests for class ClassName"`
- `@DisplayName` on `@Nested`: `"When <scenario>"` (sentence describing the context)
- `@DisplayName` on `@Test`: `"given ..., when ..., then ..."` (lowercase GWT summary)

### ❌ DON'T
- Use "JUnit 4" style (`@RunWith`, `Assert.*` static imports from `org.junit`)
- Use vague test method names like `test1()`, `testUser()`, or `shouldDoSomething()`
- Mix GWT comments (`// Given/When/Then`) with AAA method names, or vice versa
- Initialize the class under test directly as a field initializer (`private X subject = new X(...)`) — always use `@BeforeEach` instead
- Skip edge cases and error scenarios
- Write tests that depend on execution order
- Test multiple unrelated things in a single test method

---

## Review Checklist

Before submitting code, verify:
- [ ] Records used for DTOs and value objects
- [ ] Sealed classes used for controlled hierarchies
- [ ] Pattern matching used instead of instanceof chains
- [ ] Optional returned instead of null
- [ ] Streams used for collection operations
- [ ] Try-with-resources used for all closeable resources
- [ ] Single return point per method
- [ ] Each method has single responsibility
- [ ] Complete Javadoc for public APIs
- [ ] Specific custom exceptions used
- [ ] Fields are final where possible
- [ ] Tests follow Given-When-Then naming
- [ ] Tests use @DisplayName and @Nested appropriately
- [ ] Class under test initialized in @BeforeEach, not as a field initializer
- [ ] Test coverage is above 80%

---

## Integration with AGENTS.md

When AI assistants work on this codebase:
1. Follow ALL guidelines in this document
2. Reference AGENTS.md for project-specific context and conventions
3. Prioritize code quality and readability over brevity
4. Ask for clarification when guidelines conflict with specific requirements
5. When writing tests, always use Given-When-Then naming and @DisplayName
6. Organize related tests using @Nested classes

---

*This document should be reviewed and updated as Java evolves and project needs change.*