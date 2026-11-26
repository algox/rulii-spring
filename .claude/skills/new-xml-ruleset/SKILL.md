---
name: new-xml-ruleset
description: Guide for declaring rulii Rules, RuleSets, and predefined validators in Spring XML using the rulii namespace — covers namespace setup, SpEL expressions, lifecycle hooks, validation rules, and loading via @RuleScan
user-invocable: true
---

# Declaring Rules and RuleSets in Spring XML

Use this guide when the user wants to define rulii rules or rulesets in a Spring XML context file using the `http://www.rulii.org/schema/rulii` namespace.

---

## 1. Namespace declaration (required boilerplate)

Every rulii XML file must include the namespace and XSD location:

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

    <!-- Set SpEL as the default scripting language for all expressions -->
    <r:scripting defaultLanguage="el"/>

    <!-- ... rules and rulesets go here ... -->

</beans>
```

The `<r:scripting defaultLanguage="el"/>` declaration applies to the whole file. Always include it at the top unless you need per-element language overrides.

---

## 2. Loading XML files via `@RuleScan`

Point `@RuleScan` at a **classpath folder** — all `*.xml` files directly inside it are loaded:

```java
@Configuration
@RuleScan(
    scanBasePackages = "com.example.rules",      // @Rule classes
    xmlLocations    = "classpath:rules/pricing/" // all *.xml in this folder
)
public class RuleConfig { }
```

Place XML files at:
```
src/main/resources/rules/pricing/pricing-rules.xml
src/main/resources/rules/pricing/discount-rules.xml
```

Class-based rules from `scanBasePackages` are registered first; XML locations are loaded afterwards.

---

## 3. SpEL expression syntax

Rulii exposes the active `Bindings` as properties on `#ctx` using `BindingAccessor`. Reference any binding by name:

```
#ctx.age          → value of the "age" binding
#ctx.email        → value of the "email" binding
#ctx.fromDate     → value of the "fromDate" binding (any type)
```

Use standard SpEL operators:
```
#ctx.age >= 18
#ctx.name != null && #ctx.name.length() > 0
#ctx.fromDate.isBefore(#ctx.toDate)
#ctx.status == 'ACTIVE'
```

> **XML escaping**: `<` must be written as `&lt;` inside XML attributes. Use the inline text form (child element content) to avoid escaping:
> ```xml
> <!-- attribute form — must escape < -->
> <r:given expr="#ctx.age &lt;= 150"/>
>
> <!-- inline form — no escaping needed -->
> <r:given>#ctx.age <= 150</r:given>
> ```

---

## 4. Declaring a Rule (`<r:rule>`)

```xml
<r:rule name="AgeCheckRule" description="Checks if a person is an adult">
    <r:pre-condition>#ctx.age != null</r:pre-condition>
    <r:given>#ctx.age >= 18</r:given>
    <r:then>'adult'</r:then>
    <r:otherwise>'minor'</r:otherwise>
</r:rule>
```

### Child elements

| Element | Required | Description |
|---|---|---|
| `<r:pre-condition>` | No | Guard — rule is SKIP if false |
| `<r:given>` | No* | Main condition. Omit for always-true |
| `<r:then>` | No | Action when condition passes. Multiple allowed (run in order) |
| `<r:otherwise>` | No | Action when condition fails |

*A rule with no `<r:given>` defaults to always-true (logs a warning).

### Expression can be inline text or `expr` attribute

```xml
<!-- inline text -->
<r:given>#ctx.value > 0</r:given>

<!-- expr attribute (useful for short expressions) -->
<r:given expr="#ctx.value > 0"/>
```

### Per-element language override

```xml
<r:given language="el">#ctx.value == 42</r:given>
```

---

## 5. Declaring a Validation Rule (`<r:validationRule>`)

Use `<r:validationRule>` when you want the rule to write a `RuleViolation` on failure instead of running an otherwise action:

```xml
<r:validationRule name="MinAgeRule"
                  description="Age must be zero or greater"
                  errorCode="INVALID_AGE"
                  severity="ERROR"
                  errorMessage="Age must be >= 0"
                  defaultMessage="Invalid age value">
    <r:given>#ctx.age >= 0</r:given>
</r:validationRule>
```

### Attributes

| Attribute | Required | Description |
|---|---|---|
| `name` | Yes | Bean name and rule name |
| `description` | No | Human-readable description |
| `errorCode` | Yes | Code written to `RuleViolation` on failure |
| `severity` | No | `ERROR` (default), `WARNING`, `FATAL`, `INFO` |
| `errorMessage` | No | Overrides the default message |
| `defaultMessage` | No | Fallback if `errorMessage` is not resolved |

---

## 6. Declaring a RuleSet (`<r:ruleset>`)

```xml
<r:ruleset name="UserValidationRuleSet"
           description="Validates a user record"
           validating="true">

    <!-- Declare required input bindings -->
    <r:param name="username" type="java.lang.String"/>
    <r:param name="age"      type="java.lang.Integer"/>
    <r:param name="status"   type="java.lang.String" required="false"/>

    <!-- Lifecycle hooks -->
    <r:pre-condition>#ctx.username != null</r:pre-condition>
    <r:initializer>'starting validation'</r:initializer>
    <r:stop-condition>#ctx.age &lt; 0</r:stop-condition>
    <r:finalizer>'done'</r:finalizer>

    <r:rules>
        <!-- Inline rule -->
        <r:rule name="UsernameLengthRule">
            <r:given>#ctx.username.length() >= 3</r:given>
        </r:rule>

        <!-- Reference a previously declared rule bean -->
        <r:bean-ref name="AgeCheckRule"/>

        <!-- Inline predefined validator -->
        <r:notBlank name="UsernameNotBlank" binding="username"/>
    </r:rules>

</r:ruleset>
```

### `validating="true"`

Sets the ruleset into validation mode — equivalent to `.validating()` in the Java builder:
- Adds a `ruleViolations` param with a `RuleViolations` default
- Installs a finalizer that throws `ValidationException` if any severe violations exist

### Lifecycle hook elements

| Element | Description |
|---|---|
| `<r:pre-condition>` | Skip entire ruleset if false |
| `<r:initializer>` | Runs before any rule |
| `<r:stop-condition>` | Halts rule loop when true |
| `<r:finalizer>` | Runs after all rules (even if stopped) |

### `<r:bean-ref name="..."/>`

References a rule bean already declared in the same (or imported) Spring context:

```xml
<r:rules>
    <r:bean-ref name="AgeCheckRule"/>
    <r:bean-ref name="ConsistentDateRule"/>
</r:rules>
```

---

## 7. Predefined validators as XML elements

All 48 built-in validators are available directly as XML elements (the authoritative list is `PredefinedValidationRuleFactoryBean.TYPES`). Since rulii-spring 2.3.0; up to 2.2.0 the eleven validators added in rulii 2.2.0 (`uniqueElements`, `normalized`, `codePointLength`, `uuid`, `durationMin`, `durationMax`, `luhnCheck`, `mod10Check`, `mod11Check`, `ean`, `isbn`) had no element. Reference a binding by name (`binding`) or by SpEL expression (`expr`):

```xml
<!-- By binding name -->
<r:notNull   name="IdNotNull"   binding="id"/>
<r:notBlank  name="NameNotBlank" binding="name"/>
<r:email     name="EmailValid"  binding="email"/>
<r:min       name="AgeMin"      binding="age"   min="18"/>
<r:max       name="AgeMax"      binding="age"   max="120"/>
<r:size      name="NameSize"    binding="name"  min="2" max="50"/>
<r:pattern   name="ZipCode"     binding="zip"   pattern="\d{5}"/>

<!-- By SpEL expression -->
<r:min name="AgeMinByExpr" expr="#ctx.age" min="18"/>
```

### Validator settings (since 2.3.0)

Every setting a validator's builder has in Java is an optional attribute of its element, named after the
builder method. A setting left out keeps the Java default.

```xml
<r:isbn        name="IsbnValid"   binding="isbn"    type="ANY"/>
<r:uuid        name="IdIsUuid"    binding="id"      versions="4" letterCase="INSENSITIVE"/>
<r:durationMax name="SessionMax"  binding="session" max="PT8H" inclusive="false"/>
<r:alpha       name="NameAlpha"   binding="name"    allowSpace="true"/>
<r:url         name="SiteValid"   binding="site"    protocol="https" host="${site.host}"/>
<r:pattern     name="NotesFormat" binding="notes"   pattern="a.b" flags="DOTALL,CASE_INSENSITIVE"/>
<r:size        name="TagsSize"    binding="tags"    min="1" max="3" failOnNull="false"/>
```

| Element | Settings |
|---|---|
| every element | `failOnNull` (false skips the rule for a null value; `notNull`, `notBlank`, `notEmpty` accept true only), `valueName` |
| `alpha`, `alphaNumeric`, `decimal`, `numeric` | `allowSpace` |
| `decimalMin`, `decimalMax`, `durationMin`, `durationMax` | `inclusive` |
| `email` | `allowLocal`, `allowTopLevelDomain`, `anyDomain`, `pattern`, `flags` |
| `url` | `protocol`, `host`, `port`, `regexp`, `flags` |
| `pattern` | `caseSensitive`, `flags` |
| `normalized` | `form` (`NFC`, `NFD`, `NFKC`, `NFKD`) |
| `codePointLength` (`min`, `max` required) | `normalization` |
| `uuid` | `allowEmpty`, `allowNil`, `versions`, `variants`, `letterCase` (`LOWER_CASE`, `UPPER_CASE`, `INSENSITIVE`) |
| `luhnCheck` | `startIndex`, `endIndex`, `checkDigitIndex`, `ignoreNonDigitCharacters` |
| `mod10Check` | the four of `luhnCheck`, `multiplier`, `weight` |
| `mod11Check` | the four of `luhnCheck`, `threshold`, `treatCheck10As`, `treatCheck11As`, `processingDirection` (`RIGHT_TO_LEFT`, `LEFT_TO_RIGHT`) |
| `ean` | `type` (`EAN13`, `EAN8`) |
| `isbn` | `type` (`ISBN_10`, `ISBN_13`, `ANY`) |

- Switches (`allowSpace`, `allowLocal`, `anyDomain`, ...) are booleans: `true` turns the setting on.
- `versions` / `variants` are comma-separated numbers; `flags` is comma-separated `java.util.regex.Pattern` flag names.
- `durationMin` / `durationMax` take an ISO-8601 duration in `min` / `max`: `PT30M`, `PT8H`, `P2D`.
- Text and number settings accept `${...}` properties; booleans and enum settings do not.
- Extra violation parameters (`param(name, value)` in Java) have no XML form: build such a rule in Java and use `<r:bean-ref>`.

### Overriding error metadata

```xml
<r:notNull name="IdRequired"
           binding="id"
           severity="FATAL"
           errorCode="REQUIRED_FIELD"
           errorMessage="This field is required"/>
```

### Multi-value validators

```xml
<r:in name="StatusValid" binding="status">
    <r:item>ACTIVE</r:item>
    <r:item>INACTIVE</r:item>
    <r:item>PENDING</r:item>
</r:in>

<r:startsWith name="TitlePrefix" binding="title">
    <r:prefix>Mr.</r:prefix>
    <r:prefix>Dr.</r:prefix>
</r:startsWith>

<r:endsWith name="DomainSuffix" binding="email">
    <r:suffix>.com</r:suffix>
    <r:suffix>.org</r:suffix>
</r:endsWith>
```

### Predefined validators inside a ruleset

```xml
<r:ruleset name="PersonValidation" validating="true">
    <r:param name="name"   type="java.lang.String"/>
    <r:param name="age"    type="java.lang.Integer"/>
    <r:param name="status" type="java.lang.String"/>
    <r:rules>
        <r:notNull  name="NameNotNull"  binding="name"/>
        <r:notBlank name="NameNotBlank" binding="name"/>
        <r:size     name="NameSize"     binding="name" min="2" max="50"/>
        <r:min      name="AgeMin"       binding="age"  min="18"/>
        <r:in       name="StatusValid"  binding="status">
            <r:item>ACTIVE</r:item>
            <r:item>INACTIVE</r:item>
        </r:in>
    </r:rules>
</r:ruleset>
```

---

## 8. Externalising values via Spring properties

Validator attributes support Spring `${...}` property placeholders. Add a `PropertySourcesPlaceholderConfigurer` bean if your XML file needs properties from `application.properties`:

```xml
<bean class="org.springframework.context.support.PropertySourcesPlaceholderConfigurer">
    <property name="location" value="classpath:rules/my-rules.properties"/>
</bean>

<r:min name="AgeMin" binding="age" min="${age.min}"/>
<r:max name="AgeMax" binding="age" max="${age.max}"/>
<r:size name="NameSize" binding="name" min="${name.size.min}" max="${name.size.max}"/>
```

In `application.properties` / `my-rules.properties`:
```properties
age.min=18
age.max=120
name.size.min=2
name.size.max=50
```

---

## 9. Full example — pricing rules file

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

    <r:scripting defaultLanguage="el"/>

    <r:rule name="PositiveQuantityRule" description="Quantity must be positive">
        <r:given>#ctx.quantity > 0</r:given>
    </r:rule>

    <r:rule name="DiscountEligibleRule" description="Qualifies for discount when quantity > 10">
        <r:pre-condition>#ctx.quantity != null</r:pre-condition>
        <r:given>#ctx.quantity > 10</r:given>
        <r:then>'discount-eligible'</r:then>
        <r:otherwise>'standard-price'</r:otherwise>
    </r:rule>

    <r:ruleset name="orderValidationRules"
               description="Validates an order before processing"
               validating="true">
        <r:param name="quantity" type="java.lang.Integer"/>
        <r:param name="sku"      type="java.lang.String"/>
        <r:rules>
            <r:notNull  name="SkuNotNull"       binding="sku"/>
            <r:notBlank name="SkuNotBlank"      binding="sku"/>
            <r:positive name="QuantityPositive" binding="quantity"/>
            <r:bean-ref name="DiscountEligibleRule"/>
        </r:rules>
    </r:ruleset>

</beans>
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Forgetting `<r:scripting defaultLanguage="el"/>` | Without it, there is no default language and expression elements will fail |
| Using `<` directly in `expr` attribute | Escape as `&lt;`, or use inline text form instead |
| Referencing a binding not in the ruleset `<r:param>` list | At runtime this throws `UnrulyException`. Declare all expected bindings with `<r:param>` |
| `xmlLocations` pointing to a file path instead of a folder | The attribute expects a **folder** — all `*.xml` files in it are loaded |
| Using `validating="true"` on a ruleset without a `ruleViolations` binding at runtime | `validating` auto-adds a `ruleViolations` param with a default — no manual binding needed |
| `<r:bean-ref>` name does not match the declared rule bean name | The name is case-sensitive and must exactly match the `name` attribute of the referenced `<r:rule>` or scanned `@Rule` class bean |
