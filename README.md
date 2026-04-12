[rulii-spring Maven Central]:http://search.maven.org/#artifactdetails|org.rulii|rulii-spring|1.2.0|
[Apache 2.0 License]:https://opensource.org/licenses/Apache-2.0

# _rulii-spring_
**Spring Boot integration for the [rulii](https://github.com/algox/rulii) rule engine** <br/>
<sub> _Auto-configuration_ &middot; _Rule discovery via @RuleScan_ &middot; _Spring bean injection in Rules_ &middot; _Externalized messages_ &middot; _Spring type conversion_ </sub>

---

[![License](https://img.shields.io/badge/license-Apache%202.0-orange.svg)][Apache 2.0 License]
[![Maven Central Version](https://img.shields.io/maven-central/v/org.rulii/rulii-spring)][rulii-spring Maven Central]
[![Javadoc](https://javadoc.io/badge2/org.rulii/rulii-spring/1.2.0/javadoc.svg)](https://javadoc.io/doc/org.rulii/rulii-spring/1.2.0)
![Build](https://github.com/algox/rulii-spring/actions/workflows/maven.yml/badge.svg)

---

## Table of Contents

- [What is it?](#what-is-it)
- [Features](#features)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Rule Discovery](#rule-discovery)
- [Spring Beans in Rules](#spring-beans-in-rules)
- [Building RuleSets](#building-rulesets)
- [Using RuleSets in Services](#using-rulesets-in-services)
- [Documentation](#documentation)
- [Contributing](#contributing)

---

## What is it?

_rulii-spring_ brings the [rulii](https://github.com/algox/rulii) rule engine into the Spring ecosystem.
It provides auto-configuration, classpath scanning for rules, and seamless injection of Spring-managed beans
directly into rule classes — so your rules can participate fully in the Spring application context.

If you are new to rulii, start with the [rulii documentation](https://rulii.org/introduction.html) before
reading this guide.

---

## Features

- Auto-configuration of rulii options in Spring Boot applications
- Automatic rule discovery via `@RuleScan` — no manual registration needed
- Spring-managed beans injected directly into rule classes
- Externalize rule messages via `application.yaml` / `application.properties`
- Default parameter values using Spring's type conversion system

---

## Getting Started

**Maven**
```xml
<dependency>
    <groupId>org.rulii</groupId>
    <artifactId>rulii-spring</artifactId>
    <version>1.2.0</version>
</dependency>
```

**Gradle**
```groovy
implementation 'org.rulii:rulii-spring:1.2.0'
```

> rulii-spring transitively pulls in `org.rulii:rulii`. You do not need to declare it separately.

---

## Configuration

Add `@RuleScan` to your Spring configuration class to enable rule auto-discovery:

```java
@Configuration
@RuleScan(scanBasePackages = "com.example.rules")
public class RuleConfig {
}
```

> If `@RuleScan` is omitted, rulii-spring defaults to scanning the package of your `@SpringBootApplication` class.

---

## Rule Discovery

Any class annotated with `@Rule` within the scanned package is automatically registered in the `RuleRegistry` bean.
Rules can then be retrieved by class type or by name:

```java
ruleRegistry.getRule(ConsistentDateRule.class)   // by type
ruleRegistry.getRule("consistentDateRule")        // by name
```

---

## Spring Beans in Rules

Rules discovered via `@RuleScan` are Spring-managed and support full dependency injection.
Use `@Value`, `@Autowired`, or constructor injection just as you would in any Spring component:

```java
import org.rulii.annotation.*;
import org.rulii.validation.RuleViolation;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;

@Rule
@Description("Validates that fromDate is before toDate.")
public class ConsistentDateRule {

    // Externalize the message via application.yaml / application.properties
    @Value("${errorCode.100}")
    private String message;

    @PreCondition
    public boolean check(LocalDate fromDate, LocalDate toDate) {
        return fromDate != null && toDate != null;
    }

    @Given
    public boolean isValid(LocalDate fromDate, LocalDate toDate) {
        return fromDate.isBefore(toDate);
    }

    @Otherwise
    public void otherwise(LocalDate fromDate, LocalDate toDate, RuleViolations violations) {
        violations.add(RuleViolation.builder().build("consistentDateRule", "errorCode.100", message));
    }
}
```

---

## Building RuleSets

Compose rules discovered by `@RuleScan` — or manually created rules — into a `RuleSet` bean:

```java
@Configuration
@RuleScan(scanBasePackages = "com.example.rules")
public class RuleConfig {

    @Bean
    public RuleSet<?> validationRules(RuleRegistry ruleRegistry) {
        return RuleSet.builder()
                .with("validationRules")
                // Rule auto-discovered via @RuleScan
                .rule(ruleRegistry.getRule(ConsistentDateRule.class))
                // Manually created functional rule
                .rule(Rule.builder()
                        .name("alwaysPass")
                        .given(Conditions.TRUE())
                        .then(Actions.EMPTY_ACTION())
                        .build())
                .build();
    }
}
```

For rules that are not in the scanned package, build them manually using `ObjectFactory` to retain
Spring support:

```java
@Bean("myRule")
public Rule myRule(ObjectFactory objectFactory) {
    return Rule.builder()
            .with(MyRule.class, objectFactory)
            .build();
}
```

---

## Using RuleSets in Services

Inject a `RuleSet` bean into any Spring service and run it against named `Bindings`:

```java
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OrderService {

    @Autowired
    @Qualifier("validationRules")
    private RuleSet<?> validationRules;

    public void validate(String username, String email, LocalDate fromDate, LocalDate toDate) {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("username", username);
        bindings.bind("email", email);
        bindings.bind("fromDate", fromDate);
        bindings.bind("toDate", toDate);
        bindings.bind("violations", new RuleViolations());

        RuleViolations violations = (RuleViolations) validationRules.run(bindings);

        if (violations.hasErrors()) {
            throw new ValidationException(violations);
        }
    }
}
```

---

## Claude Code Skills

This project ships with [Claude Code](https://claude.ai/code) skills that assist with common development tasks.
If you have Claude Code installed, invoke any skill with its slash command from within the project directory.

| Skill | Command | What it does |
|---|---|---|
| New Rule | `/new-rule` | Creates a rulii `@Rule` class with Spring injection — covers declarative and lambda styles, `@Value`, `@Autowired`, `ObjectFactory`, and preConditions |
| New RuleSet | `/new-ruleset` | Full RuleSet builder API — lifecycle hooks, input params, stop conditions, `.validating()` mode, and Spring `@Bean` wiring with `@RuleScan` and `@Qualifier` |
| New Validation Rule | `/new-validation-rule` | Creates a custom `ValueValidationRule` with its companion builder — covers supported types, `isValid()` logic, violation customisation, and the full checklist |
| New XML RuleSet | `/new-xml-ruleset` | Declares rules, rulesets, and predefined validators in Spring XML using the rulii namespace — covers SpEL expressions, lifecycle hooks, property placeholders, and `@RuleScan` loading |
| Write Test | `/write-test` | JUnit 5 + Spring Boot test patterns — `@SpringBootTest` setup, `@RuleScan` wiring, PASS/FAIL/SKIP scenarios, XML rule testing, and Spring message resolution |
| Debug Rule | `/debug-rule` | Diagnostic guide for rules that produce the wrong result — covers SKIP (type mismatch), missing bindings, Spring discovery failures, `@Value` injection issues, and tracing |

### Using the skills in your own project

Since you'll typically reference rulii-spring as a dependency rather than working in this repo directly,
copy the skills into your own project's `.claude/skills/` directory:

```bash
cp -r <path-to-rulii-spring>/.claude/skills/new-rule             .claude/skills/
cp -r <path-to-rulii-spring>/.claude/skills/new-ruleset           .claude/skills/
cp -r <path-to-rulii-spring>/.claude/skills/new-validation-rule   .claude/skills/
cp -r <path-to-rulii-spring>/.claude/skills/new-xml-ruleset       .claude/skills/
cp -r <path-to-rulii-spring>/.claude/skills/write-test            .claude/skills/
cp -r <path-to-rulii-spring>/.claude/skills/debug-rule            .claude/skills/
```

Once copied, run `claude` from your project root — skills in `.claude/skills/` are discovered automatically.

### Prerequisites

Install Claude Code:
```bash
npm install -g @anthropic-ai/claude-code
```

---

## Documentation

**[Full documentation at rulii.org](https://rulii.org)**

- [Spring integration docs](https://rulii.org/spring/introduction.html)
- [rulii core documentation](https://rulii.org/introduction.html)
- [Javadoc (1.2.0)](https://javadoc.io/doc/org.rulii/rulii-spring/1.2.0)
- [Spring Boot sample project](https://github.com/algox/rulii-samples/tree/develop/spring-boot-sample)
- [All sample projects](https://github.com/algox/rulii-samples)
- [rulii core library](https://github.com/algox/rulii)

---

## Contributing

Contributions are welcome! Please open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes
4. Push and open a pull request against `develop`

All PRs must pass the full test suite (`mvn test`) before review.

---

_Licensed under the [Apache 2.0 License]._
