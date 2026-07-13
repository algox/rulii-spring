[rulii-spring Maven Central]:http://search.maven.org/#artifactdetails|org.rulii|rulii-spring|1.2.0|
[Apache 2.0 License]:https://opensource.org/licenses/Apache-2.0

# _rulii-spring_
**Spring Boot integration for the [rulii](https://github.com/algox/rulii) rule engine** <br/>
<sub> _Auto-configuration_ &middot; _Rule discovery via @RuleScan_ &middot; _Spring bean injection in Rules_ &middot; _SpEL scripting_ &middot; _Rules, RuleSets & RuleFlows in XML_ &middot; _Externalized messages_ &middot; _Spring type conversion_ </sub>

---

[![License](https://img.shields.io/badge/license-Apache%202.0-orange.svg)][Apache 2.0 License]
[![Maven Central Version](https://img.shields.io/maven-central/v/org.rulii/rulii-spring)][rulii-spring Maven Central]
[![Javadoc](https://javadoc.io/badge2/org.rulii/rulii-spring/1.2.0/javadoc.svg)](https://javadoc.io/doc/org.rulii/rulii-spring/1.2.0)
![Build](https://github.com/algox/rulii-spring/actions/workflows/maven.yml/badge.svg)
[![codecov](https://codecov.io/gh/algox/rulii-spring/branch/develop/graph/badge.svg)](https://codecov.io/gh/algox/rulii-spring)

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
- [SpEL Scripting](#spel-scripting)
- [Rules, RuleSets & RuleFlows in XML](#rules-rulesets--ruleflows-in-xml)
- [Documentation](#documentation)
- [Contributing](#contributing)

---

## What is it?

_rulii-spring_ brings the [rulii](https://github.com/algox/rulii) rule engine into the Spring ecosystem.
It provides auto-configuration, classpath scanning for rules, and seamless injection of Spring-managed beans
directly into rule classes — so your rules can participate fully in the Spring application context.

If you are new to rulii, start with the [rulii documentation](https://rulii.com/introduction.html) before
reading this guide.

---

## Features

- Auto-configuration of rulii options in Spring Boot applications
- Automatic rule discovery via `@RuleScan` — no manual registration needed
- Spring-managed beans injected directly into rule classes
- Externalize rule messages via `application.yaml` / `application.properties`
- Default parameter values using Spring's type conversion system
- Spring Expression Language (SpEL) as a rule scripting language
- Declare Rules, RuleSets, and RuleFlows in XML using the `rulii` Spring namespace
- `${property:default}` placeholders in script text, resolved from the Spring `Environment`

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

## SpEL Scripting

rulii-spring registers the **Spring Expression Language (SpEL)** as a rulii scripting language under the
name `el`. Conditions, actions, and functions can then be written as expressions instead of Java methods.
Bindings are exposed through the `#ctx` variable:

```java
#ctx.age >= 18                 // condition — read a binding
#ctx.approved = true           // action — write to a binding (binds it if absent)
#ctx.total * (1 - #ctx.rate)   // function — compute a value
```

A few things worth knowing:

- **Absent bindings read as `null`** — guards like `#ctx.middleName != null` work without pre-binding.
- **Property placeholders**: script text may contain `${property:default}` placeholders, resolved once at
  startup against the Spring `Environment` with the same semantics as `@Value` — a missing key without a
  default fails startup naming the placeholder, and `\${...}` escapes to a literal. Disable with
  `rulii.scripts.resolvePlaceholders=false`.
- **Trust model**: SpEL expressions run with the full power of the evaluation context. Treat rule
  expressions as trusted code (the same trust level as Spring bean XML) — never assemble them from user input.

```xml
<r:rule name="MinTotalRule" given="#ctx.total >= ${order.minTotal:100}"/>
```

SpEL is the default choice in XML rule files, but any JSR-223 language registered with rulii's
`ScriptProcessorManager` (e.g. JavaScript, Groovy) can be used as well.

---

## Rules, RuleSets & RuleFlows in XML

Rules do not have to be Java classes. The `rulii` XML namespace lets you externalize entire rule files —
rules, validation rules, rulesets, and ruleflows — with SpEL expressions for the logic:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:r="http://www.rulii.org/schema/rulii"
       xsi:schemaLocation="
           http://www.springframework.org/schema/beans
           https://www.springframework.org/schema/beans/spring-beans.xsd
           http://www.rulii.org/schema/rulii
           https://www.rulii.org/spring/rulii-spring.xsd">

    <!-- Default scripting language for every expression in this file -->
    <r:scripting defaultLanguage="el"/>

</beans>
```

### Rules

```xml
<r:rule name="AgeCheckRule" description="Applicant must be an adult">
    <r:pre-condition>#ctx.age != null</r:pre-condition>
    <r:given>#ctx.age >= 18</r:given>
    <r:then>#ctx.eligible = true</r:then>
    <r:otherwise>#ctx.eligible = false</r:otherwise>
</r:rule>
```

Simple rules collapse to a one-liner using the equivalent attribute forms:

```xml
<r:rule name="ApproveRule" given="#ctx.total >= 100" then="#ctx.approved = true" otherwise="#ctx.approved = false"/>
```

### Validation rules

All **37 predefined validators** (`notNull`, `email`, `min`, `max`, `pattern`, …) are available directly
as XML elements, and `<r:validationRule>` wraps custom checks:

```xml
<r:notNull name="NameRequiredRule" binding="name"/>
<r:min name="MinAgeRule" binding="age" min="${age.rule.min:18}"/>
<r:email name="EmailRule" binding="email"/>

<r:validationRule name="BalancedRule" errorCode="account.balanced" given="#ctx.debits == #ctx.credits"/>
```

### RuleSets

Group rules — declared in the same file or referenced by bean name — with optional lifecycle hooks:

```xml
<r:ruleset name="ApplicantChecks" pre-condition="#ctx.applicant != null">
    <r:rules>
        <r:bean-ref name="AgeCheckRule"/>
        <r:bean-ref name="NameRequiredRule"/>
        <r:bean-ref name="EmailRule"/>
    </r:rules>
</r:ruleset>
```

### RuleFlows

The full `RuleFlow.builder()` DSL is available in XML — sequential and async rule execution,
conditionals, loops, scopes, exception handlers, and a returning value:

```xml
<r:ruleflow name="OrderFlow">
    <r:param name="total" type="java.lang.Integer"/>

    <r:bind name="doubled">#ctx.total * 2</r:bind>

    <r:when condition="#ctx.total >= ${order.flowMin:100}">
        <r:then>
            <r:bind name="status" value="approved"/>
            <r:run name="DiscountRule"/>          <!-- RuleRegistry lookup at execution time -->
        </r:then>
        <r:otherwise>
            <r:bind name="status" value="rejected"/>
            <r:exit/>
        </r:otherwise>
    </r:when>

    <r:for-each item="n" source="{1, 2, 3}">
        <r:execute>#ctx.sum = #ctx.sum + #ctx.n</r:execute>
    </r:for-each>

    <r:on-exception exception="java.lang.Exception">
        <r:execute>#ctx.failed = true</r:execute>
    </r:on-exception>

    <r:finalizer>#ctx.done = true</r:finalizer>
    <r:returning>#ctx.status</r:returning>
</r:ruleflow>
```

Async execution is built in: `<r:async-run>` fans work out, `<r:then-run>` chains continuations, and
`<r:await>` / `<r:await-all>` / `<r:await-any>` (with `timeout` + `unit`) join the results. Custom
`RuleFlowCommand` beans plug in via `<r:command ref="..."/>`.

### Loading XML rule files

Point `@RuleScan` at the folder — every `*.xml` file in it is loaded and its beans registered:

```java
@Configuration
@RuleScan(xmlLocations = "classpath:rules/pricing/")
public class RuleConfig {
}
```

XML-declared rules are regular Spring beans: inject them directly, or look them up through the
`RuleRegistry` just like scanned `@Rule` classes:

```java
Rule rule          = ruleRegistry.getRule("AgeCheckRule");
RuleSet<?> ruleSet = ruleRegistry.getRuleSet("ApplicantChecks");
RuleFlow<?> flow   = ruleRegistry.getRuleFlow("OrderFlow");
```

> The `/new-xml-ruleset` and `/new-xml-ruleflow` Claude Code skills (below) cover the complete
> element grammar, including the parts not shown here.

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
| New XML RuleFlow | `/new-xml-ruleflow` | Declares RuleFlows in Spring XML — covers the command grammar (`run`/`bind`/`when`/`for-each`/`scope`), async execution, exception handling, custom commands, and terse attribute forms |
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
cp -r <path-to-rulii-spring>/.claude/skills/new-xml-ruleflow      .claude/skills/
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

**[Full documentation at rulii.com](https://rulii.com)**

- [Spring integration docs](https://rulii.com/spring/introduction.html)
- [rulii core documentation](https://rulii.com/introduction.html)
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
