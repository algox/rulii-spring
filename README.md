[Rulii Maven Central]:http://search.maven.org/#artifactdetails|org.rulii|rulii-spring|1.0.0|
[Apache 2.0 License]:https://opensource.org/licenses/Apache-2.0

# _Rulii Spring_
**Spring support for Rulii**

---

[![License](https://img.shields.io/badge/license-Apache%202.0-orange.svg)][Apache 2.0 License]
[![Maven Central Version](https://img.shields.io/maven-central/v/org.rulii/rulii-spring)][Rulii Maven Central]
[![Javadoc](https://javadoc.io/badge2/org.rulii/rulii-spring/1.0.0/javadoc.svg)](https://javadoc.io/doc/org.rulii/rulii-spring/1.0.0)
![Build](https://github.com/algox/rulii-spring/actions/workflows/maven.yml/badge.svg)


_Rulii Spring_ brings Rulii into the Spring ecosystem, allowing developers to define Rules and RuleSets using familiar Spring conventions.


## Features

* Enables auto-configuration of Rulii options in Spring applications.
* Enables automatic discovery of Rules within the application context.
* Enables the use of Spring-managed Beans directly in Rules.
* Supports externalizing Rule messages to application.yaml or application.properties
* Supports default parameter values using Spring’s conversion system.

**Configuration**
```java
@RuleScan(scanBasePackages = "base package of your rules")
```
** If @RuleScan is not specified, the package of the SpringBoot application will be used.

**Example: Spring Configuration**

**[Example Found here](https://github.com/algox/rulii-samples/tree/develop/spring-boot-sample)**

```java

@Configuration
@RuleScan(scanBasePackages = "base package of your rules")
public class RuleConfig {

    @Bean("testRule1")
    // Here we are manually creating TestRule1 (ie: it was not scanned in the base package.
    // ObjectFactory is autoconfigured and allows Rules to leverage Spring.
    public Rule testRule1(ObjectFactory objectFactory) {
        return Rule.builder()
                .with(TestRule1.class, objectFactory)
                .build();
    }

    @Bean("testFunctionalRule")
    public Rule testFunctionalRule() {
        return Rule.builder()
                .name("testFunctionalRule")
                .given(Conditions.TRUE())
                .then(Actions.EMPTY_ACTION())
                .build();
    }

    @Bean
    public RuleSet<?> testRuleSet(RuleRegistry ruleRegistry) {
        return RuleSet.builder()
                .with("TestRuleSet1")
                // Rule that is defined above
                .rule(ruleRegistry.getRule(TestRule1.class))
                // Rule that is defined above
                .rule(ruleRegistry.getRule("testFunctionalRule"))
                // Find a registered rule (ie: via @RuleScan)
                .rule(ruleRegistry.getRule(ConsistentDateRule.class))
                .build();
    }
}
```

**Example: Service injecting RuleSet (that is defined above)**

```java
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TestService {

    @Autowired
    private RuleSet<?> testRuleSet;
    
    public void validate(Object param1, Object param2, LocalDate fromDate, LocalDate toDate) {
        Bindings bindings = Bindings.builder().standard();
        bindings.bind("param1", param1);
        bindings.bind("param2", param2);
        bindings.bind("fromDate", fromDate);
        bindings.bind("toDate", toDate);
        bindings.bind("violations", new RuleViolations());

        //Run the RuleSet
        RuleViolations violations = ruleSet.run(bindings);

        // Found errors
        if (violations.hasErrors()) {
            throw new ValidationException(violations);
        }
    }
}

```

**Example: Rule leveraging Spring**
```java

import org.rulii.annotation.*;
import org.rulii.validation.RuleViolation;
import org.rulii.validation.RuleViolations;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;

@Rule
@Description("This Rule will validate that the from date is before the to date.")
public class ConsistentDateRule {

    // We are leveraging Spring to externalize the message.
    @Value("${errorCode.100}")
    private String message;

    public ConsistentDateRule() {
        super();
    }

    @PreCondition // Don't run the rule if there are null values
    public boolean check(LocalDate fromDate, LocalDate toDate) {
        return fromDate != null && toDate != null;
    }

    @Given // Condition
    public boolean isValid(LocalDate fromDate, LocalDate toDate) {
        return fromDate.isBefore(toDate);
    }

    @Otherwise() // Else Action
    public void otherwise(LocalDate fromDate, LocalDate toDate, RuleViolations violations) {
        violations.add(RuleViolation.builder().build("consistentDateRule", "errorCode.100", message));
    }
}

```

**Now you are ready to leverage Spring in your Rule(s).**