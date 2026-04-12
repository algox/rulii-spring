---
name: write-test
description: Guide for writing JUnit 5 tests in the rulii-spring project — Spring Boot test setup, @RuleScan wiring, PASS/FAIL/SKIP patterns, ValidationRule patterns, and missing-binding patterns
user-invocable: true
---

# Writing Tests in rulii-spring

Use this guide when writing JUnit 5 tests for Spring-integrated rules, RuleSets, registry, converters, or other rulii-spring components.

Tests live in `src/test/java/org/rulii/spring/test/`.

---

## 1. Spring Boot test class structure

Most tests in this project use `@SpringBootTest` with a `TestApplication` entry point:

```java
package org.rulii.spring.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rulii.bind.Bindings;
import org.rulii.context.RuleContext;
import org.rulii.registry.RuleRegistry;
import org.rulii.rule.Rule;
import org.rulii.ruleset.RuleSet;
import org.rulii.validation.RuleViolations;
import org.rulii.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MyRuleSetTest {

    @Autowired
    @Qualifier("myRuleSet")          // always qualify when multiple RuleSets exist
    private RuleSet<?> myRuleSet;

    @Autowired
    private RuleRegistry ruleRegistry;

    @Test
    public void testValid() {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("username", String.class, "alice");
        bindings.bind("age", Integer.class, 25);
        bindings.bind("violations", RuleViolations.class, new RuleViolations());

        RuleContext ctx = RuleContext.builder().build(bindings);
        myRuleSet.run(ctx);

        RuleViolations violations = bindings.getValue("violations", RuleViolations.class);
        Assertions.assertTrue(violations.isEmpty());
    }
}
```

---

## 2. TestApplication

`TestApplication` is the `@SpringBootApplication` bootstrap used in all tests. It must be present for `@SpringBootTest` to find a configuration:

```java
package org.rulii.spring.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestApplication {
}
```

---

## 3. TestConfig — wiring rules and rulesets for tests

Use a `@TestConfiguration` (or a regular `@Configuration` under the test package) to define the `@RuleScan` and any test `RuleSet` beans:

```java
package org.rulii.spring.test.config;

import org.rulii.registry.RuleRegistry;
import org.rulii.ruleset.RuleSet;
import org.rulii.spring.annotation.RuleScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RuleScan(scanBasePackages = "org.rulii.spring.test.rules.seta")
public class TestConfig {

    @Bean
    public RuleSet<?> testRuleSet(RuleRegistry ruleRegistry) {
        return RuleSet.builder()
                .with("testRuleSet")
                .rule(ruleRegistry.getRule(ConsistentDateRule.class))
                .validating()
                .build();
    }
}
```

---

## 4. JUnit 5.12.1 — Known Ambiguity Issues

These cause **compile errors**, not runtime errors. Fix them as shown.

### 4a. `assertEquals` with int literal + Object + message

```java
// AMBIGUOUS — new overloads assertEquals(float, Float, String) / (double, Double, String)
assertEquals(5, someObject, "message");

// FIX — cast to Number and extract int
Assertions.assertEquals(5, ((Number) someObject).intValue(), "message");
```

### 4b. `assertDoesNotThrow` with method reference returning `Void`

```java
// AMBIGUOUS — Void return creates overload conflict
assertDoesNotThrow(action::run);

// FIX — use block lambda
assertDoesNotThrow(() -> { action.run(); });
```

---

## 5. Testing PASS / FAIL / SKIP

### PASS

```java
Bindings bindings = Bindings.builder().standard();
bindings.bind("value", String.class, "hello");
bindings.bind("ruleViolations", RuleViolations.class, new RuleViolations());
RuleContext ctx = RuleContext.builder().build(bindings);

Rule rule = ruleRegistry.getRule(MyRule.class);
var result = rule.run(ctx);

Assertions.assertEquals(RuleExecutionStatus.PASS, result.getStatus());
Assertions.assertTrue(bindings.getValue("ruleViolations", RuleViolations.class).isEmpty());
```

### FAIL (violation recorded)

```java
Bindings bindings = Bindings.builder().standard();
bindings.bind("value", String.class, null);                          // null → FAIL
bindings.bind("ruleViolations", RuleViolations.class, new RuleViolations());
RuleContext ctx = RuleContext.builder().build(bindings);

Rule rule = ruleRegistry.getRule(MyRule.class);
rule.run(ctx);

RuleViolations violations = bindings.getValue("ruleViolations", RuleViolations.class);
Assertions.assertFalse(violations.isEmpty());
Assertions.assertEquals("myRule.errorCode", violations.getViolation(0).getErrorCode());
```

### SKIP (type mismatch — no violation, no otherwise)

```java
// Pass a Boolean where the rule expects CharSequence → checkType() returns false → SKIP
Bindings bindings = Bindings.builder().standard();
bindings.bind("value", Boolean.class, true);
bindings.bind("ruleViolations", RuleViolations.class, new RuleViolations());
RuleContext ctx = RuleContext.builder().build(bindings);

Rule rule = ruleRegistry.getRule(MyRule.class);
var result = rule.run(ctx);

Assertions.assertEquals(RuleExecutionStatus.SKIP, result.getStatus());
Assertions.assertTrue(bindings.getValue("ruleViolations", RuleViolations.class).isEmpty());
```

> **Prefer type-mismatch SKIP over missing-binding** when testing SKIP inside a RuleSet.
> Missing bindings throw `UnrulyException`, which is harder to isolate.

---

## 6. Testing missing bindings

A missing binding propagates `NoSuchBindingException` → wrapped in `UnrulyException`:

```java
Bindings bindings = Bindings.builder().standard();
// "value" binding intentionally absent
RuleContext ctx = RuleContext.builder().build(bindings);

Rule rule = ruleRegistry.getRule(MyRule.class);

// Assert UnrulyException — NOT NoSuchBindingException
Assertions.assertThrows(UnrulyException.class, () -> rule.run(ctx));
```

---

## 7. Testing a ValidationException from a validating RuleSet

```java
ValidationException ex = Assertions.assertThrows(
        ValidationException.class,
        () -> myRuleSet.run(email -> "not-an-email")
);

Assertions.assertEquals(1, ex.getViolations().size());
Assertions.assertEquals("email.errorCode", ex.getViolations().getViolation(0).getErrorCode());
```

---

## 8. Testing Spring message resolution (`@Value`)

Place the property in `src/test/resources/application.properties`:

```properties
errorCode.100=Date range is invalid.
```

Then assert the resolved message appears in the violation:

```java
RuleViolations violations = bindings.getValue("ruleViolations", RuleViolations.class);
Assertions.assertEquals("Date range is invalid.", violations.getViolation(0).getMessage());
```

---

## 9. Testing XML-declared rules

Load an XML context file directly in the test and retrieve the rule bean:

```java
@SpringBootTest
@ImportResource("classpath:rules/my-rules.xml")
public class MyXmlRuleTest {

    @Autowired
    @Qualifier("AgeCheckRule")
    private Rule ageCheckRule;

    @Test
    public void testAdult() {
        var result = ageCheckRule.run(age -> 21);
        Assertions.assertEquals(RuleExecutionStatus.PASS, result.getStatus());
    }
}
```

---

## 10. Quick reference — common imports

```java
// JUnit
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// Spring test
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

// Core builders
import org.rulii.bind.Bindings;
import org.rulii.context.RuleContext;
import org.rulii.rule.Rule;
import org.rulii.rule.RuleExecutionStatus;
import org.rulii.ruleset.RuleSet;
import org.rulii.registry.RuleRegistry;

// Validation
import org.rulii.validation.RuleViolations;
import org.rulii.validation.ValidationException;
import org.rulii.model.UnrulyException;

// Static helpers
import static org.rulii.model.condition.Conditions.condition;
import static org.rulii.model.action.Actions.action;
import static org.rulii.validation.rules.Validators.binding;
import static org.rulii.validation.rules.Validators.value;
```
