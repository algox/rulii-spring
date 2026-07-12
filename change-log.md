# Changelog

## [2.0.0]

### rulii 2.0 Upgrade

- Built against rulii `2.0.0` — registry null-return contract, executor service support, and tracer listener wiring adopted throughout
- New `rulii.executorService` bean — bounded thread pool (queue of 1000, caller-runs policy) used for async rule/flow execution; tied to the context lifecycle. **Overridden by NAME**: define a bean named `rulii.executorService` (unrelated `ExecutorService` beans are deliberately ignored)
- `Tracer` auto-configuration registers `RuliiListener`, `RuleListener`, `RuleSetListener`, and `RuleFlowListener` beans found in the context; each listener bean is registered exactly once even when it implements multiple listener interfaces

---

### New Feature: RuleFlow Integration (`org.rulii.spring.xml`, `org.rulii.spring.registry`)

RuleFlows are first-class citizens of the Spring integration:

- `SpringRuleRegistry.getRuleFlow(name)` / `getRuleFlows()` — flows declared as beans are discoverable like rules and rulesets
- Registry lookups inside flows (`run(nameInRegistry)` / `run(Class)`) resolve against the Spring registry at execution time; `asyncRun` uses the Spring-configured executor
- **Full `<r:ruleflow>` XML namespace** mirroring the `RuleFlow.builder()` DSL — script expressions substitute the builder's lambdas:
  - Commands: `bind` (literal `value`, Spring `ref`, or script expression), `run` / `async-run`, `apply`, `execute`, `when`/`then`/`otherwise`, `for-each` (with `source` and optional `stop-condition`), `scope`, `exit`, `await` / `await-all` / `await-any` (`names="a,b"`, `timeout` + `unit`), `then-run` continuations, `on-exception` handlers (step-level and flow-level; the caught exception is bound as `ex`), `finalizer`, `returning`
  - Run targets — exactly one of: `bean-ref` (Spring bean, resolved at wiring time), `name` (RuleRegistry lookup at execution time), `class` (registry lookup by rule class at execution time)
  - `async-run` supports `context-mode` (`shared` | `immutable`)
  - Escape hatches: `<r:context ref="..."/>` (a `Consumer<RuleContextBuilder>` bean) and `<r:command ref="..."/>` (a custom `RuleFlowCommand` bean; with a nested command body the bean must be a `ContainerCommand` — retry, parallel, etc. — and receives the parsed body; reusing one container instance for several bodies fails fast, use `@Scope("prototype")`)
  - Java-only (no XML form): inline `context(...)` lambdas and `asyncRun(...).withContext(RuleContext)`
- New types: `RuleFlowFactoryBean`, `RuleFlowBeanDefinitionParser`, `RuleFlowCommandDefinition`
- Note: flow bindings live in the flow's own scope, which is removed after execution — export observable state via `#ctx.x = ...` writes to pre-bound bindings or via `returning`

---

### New Feature: Environment Placeholders in Script Text

- Script text — any language, XML-declared or programmatic `Script.builder()` — may contain `${property:default}` placeholders, resolved once at compile time against the Spring environment with `@Value` semantics: `:default` fallbacks, fail-fast on a missing key (naming the placeholder), `\${...}` escaping
- Wired by `ScriptTextResolverConfigurer` (a `BeanFactoryPostProcessor`, active before any singleton instantiates); resolution delegates to the bean factory's embedded value resolver with an `Environment` fallback
- Overridable: subclass `ScriptTextResolverConfigurer` (override `createResolver`, register as a static `@Bean`); disable entirely with `rulii.scripts.resolvePlaceholders=false`
- Caveat: languages where `${}` is native syntax (Groovy GStrings) need escaping

---

### New Feature: Terse Expression Attributes (XML)

Condition, single-action, and function child elements now have equivalent attributes — a simple rule collapses to a one-liner:

```xml
<r:rule name="ApproveRule" given="#ctx.total >= 100"
        then="#ctx.approved = true" otherwise="#ctx.approved = false"/>
```

- `given` / `pre-condition` / `then` / `otherwise` on `<r:rule>`; `given` on `<r:validationRule>`; `pre-condition` / `stop-condition` / `initializer` / `finalizer` on `<r:ruleset>`; `condition` on ruleflow `<r:when>`; `source` / `stop-condition` on `<r:for-each>`; `finalizer` / `returning` on `<r:ruleflow>`
- The attribute form uses the file's default scripting language (`language` overrides need the element form); declaring both forms is a parse error; the `then` attribute covers the single-action case only

---

### Correctness & Hardening Pass (all packages)

#### `@RuleScan` / Auto-Configuration
- Classpath scanning honors the running application's `Environment` and `ResourceLoader` — `@Profile` / `@Conditional` on rule classes are respected (class scan and XML `<beans profile="...">` sections alike)
- `xmlLocations` accepts three forms — folder (`classpath:rules/pricing/`, upgraded to `classpath*:`), explicit file (fails fast when missing), and pattern (used as-is)
- `xmlLocations`-only `@RuleScan` is truly XML-only (no silent scan of the config class's package); a bare `@RuleScan` — or direct `@Import(RuleRegistrar.class)` — falls back to the importing class's package
- Rule-name collisions fail fast with both class names and an `@Rule(name=...)` remedy; the same class rediscovered via overlapping packages is skipped
- Multiple `@RuleScan` configurations merge into a single injectable `RuleRegistrarMetaInfo` (now `List<String>`-based with value semantics)
- An application-supplied `ObjectFactory` bean is honored by scanned rules regardless of its bean name

#### XML Namespace
- `spring.schemas` maps the `https://` schema URL — validation resolves the bundled XSD offline (no network fetch at startup)
- Duplicate inline rule names inside rulesets fail at parse time with the XML location and a `<bean-ref>` remedy (previously the wrong rule silently ran)
- `<r:scripting defaultLanguage="...">` is scoped to its file and position-independent (previously leaked across files in parse order)
- Empty expressions and missing validator value sources are reported at parse time with the file/element location
- Predefined validators accept the documented text-body form (`<r:email>#ctx.email</r:email>`); child-element text (e.g. `<r:item>`) is never mistaken for an expression
- `xs:boolean` attributes honor the full lexical space (`caseSensitive="1"`, `required="1"`)
- `<r:class-ref>` classes load lazily via the container's bean class loader
- Inline and top-level `<r:rule>`/`<r:validationRule>` share one parse routine and cannot drift; the 37 predefined validator elements are driven by a single list (`PredefinedValidationRuleFactoryBean.TYPES`)

#### SpEL Scripting
- Property navigation into binding values works (`#ctx.person.name`, not just `getName()`), and bare-name expressions resolve against the bindings root (`order.total >= 100`)
- Absent bindings read as `null` consistently — guard expressions like `#ctx.x != null` are reliable
- Reads carry the binding's declared type (generics-aware conversion); the declared script return type is applied via SpEL conversion
- Custom-named `SpelScriptProcessorFactory` instances compile scripts under their own language name; `isAvailable()` reports `false` when `spring-expression` is absent
- A wrong-language script produces a diagnostic `EvaluationException` instead of a raw `ClassCastException`
- Trust model documented: expressions run with full `StandardEvaluationContext` power — treat rule expressions as trusted code

#### Service Adapters / `RuleConfig`
- **Converter precedence**: custom rulii `Converter` beans → the Spring `ConversionService` bridge → rulii's built-in defaults — application-registered Spring converters are finally honored for types the built-ins also cover; toggle key fixed to `rulii.converters.registerDefaults`
- `SpringRuleRegistry`: cached per-type bean-name lookups (no more full context scans per lookup), parent-context-aware enumeration, metadata-based type checks, `getRuleFlows()` support
- `SpringObjectFactory`: honors `isUseCache` (stateless strategies created once, not per execution) and wraps Spring failures in `UnrulyException` per the contract
- `SpringEnvironmentMessageResolver`: `MessageSource`-first (locale-aware, standard i18n bundles) with `Environment` fallback; null-code and unresolvable-placeholder safe
- `SpringConverterAdapter`: Spring conversion failures wrapped in rulii's `ConversionException`; `TypeDescriptor`s cached
- `SpringContextBindingLoader`: works against real contexts — resolves types from metadata (never instantiates; `@Lazy` safe) and skips non-bindable bean names
- Optional `Clock` and `Locale` beans are honored by `ruleContextOptions` (deterministic time in tests)

### Breaking Changes
- `RuleRegistrarMetaInfo` components changed from `String[]` to `List<String>` (value-based equality)
- `xmlLocations`-only `@RuleScan` no longer scans the config class's package
- Converter lookup order changed (Spring `ConversionService` now wins over rulii built-ins for pairs both handle)
- `SpringConverterAdapter` throws rulii `ConversionException` instead of Spring's conversion exceptions
- `ResolvableTypeDescriptor` removed (Spring's `TypeDescriptor(ResolvableType, ...)` constructor is public)
- Registry enumeration (`getRules()` etc.) now includes ancestor bean factories, matching by-name lookups

---

## [1.2.0]

### Spring Expression Language (SpEL) Scripting Support (`org.rulii.spring.script.el`)

#### Core SpEL Components
- `SpelScript<T>` — `Script<T>` implementation backed by a SpEL expression string; stores language name (`"el"`), expression text, and parameters
- `SpelScriptCompiler` — compiles a `SpelScript` into a reusable `Expression` via Spring's `ExpressionParser`; caches compiled expressions for reuse
- `SpelScriptProcessor` — `ScriptProcessor` implementation that evaluates a compiled SpEL `Expression` against a `RuleContext`; exposes the bindings map into the evaluation context via `BindingAccessor`
- `SpelScriptProcessorFactory` — `ScriptProcessorFactory` SPI implementation; registers `SpelScriptProcessor` under the language name `"el"` and is auto-discovered at startup via `META-INF/services`
- `BindingAccessor` — Spring `PropertyAccessor` implementation that bridges rulii `Bindings` into SpEL's `EvaluationContext`; allows expressions to reference bindings by name (e.g. `#ctx.age`)

#### RuleConfig / BeanNames Updates
- `RuleConfig` now creates and registers a `SpelScriptProcessorFactory` bean and wires a `ScriptProcessorRegistry` into `SpringEnabledRuleContextOptions`
- `BeanNames` updated with constants for scripting-related beans

---

### XML Namespace — Declarative Rule Definition (`org.rulii.spring.xml`)

#### Namespace Handler
- `RuliiNamespaceHandler` — Spring `NamespaceHandlerSupport` that registers all element parsers for the `http://www.rulii.org/schema/rulii` namespace (prefix `r:`); default expression language is `"el"` and is configurable via `<r:scripting defaultLanguage="..."/>`

#### Element Parsers
- `ScriptingBeanDefinitionParser` — parses `<r:scripting>` to set the default expression language on the namespace handler
- `RuleBeanDefinitionParser` — parses `<r:rule>` elements; supports `pre-condition`, `given`, `then` (repeatable), and `otherwise` child elements containing script expressions
- `RuleSetBeanDefinitionParser` — parses `<r:ruleset>` elements; supports typed `<r:param>` declarations with optional `<r:default-value>`, lifecycle hooks (`pre-condition`, `initializer`, `stop-condition`, `finalizer`), `<r:rules>` child collection, `result-extractor`, and `error-handler`
- `ValidationRuleBeanDefinitionParser` — parses `<r:validationRule>` elements for inline script-based validation rules
- `PredefinedValidationRuleBeanDefinitionParser` — parses all 37 built-in validation rule elements (see below); shared parser instance registered for every predefined rule tag

#### Factory Beans
- `RuleFactoryBean` — `FactoryBean<Rule>` that constructs a `Rule` from parsed XML attributes and script expressions
- `RuleSetFactoryBean` — `FactoryBean<RuleSet>` that constructs a `RuleSet` from parsed XML attributes, typed parameters, lifecycle hooks, and a rule collection
- `ValidationRuleFactoryBean` — `FactoryBean<Rule>` that constructs a script-based validation rule from XML
- `PredefinedValidationRuleFactoryBean` — `FactoryBean<Rule>` that instantiates one of the 37 built-in `ValueValidationRule` classes from XML attributes (`binding`, `errorCode`, `severity`, `errorMessage`, and rule-specific parameters)
- `RuleFromInstanceFactoryBean` — `FactoryBean<Rule>` that wraps an existing Spring bean (referenced by `<r:bean-ref name="..."/>`) or a class (referenced by `<r:class-ref class="..."/>`) as a `Rule`

#### Supporting Types
- `ScriptExpression` — value object holding a `(language, expressionText)` pair parsed from an XML element; resolved to a `Script` at factory bean initialization time
- `ValueSource` — value object capturing the binding name, error code, severity, and message attributes shared across all predefined validation rule elements

#### XML Schema
- `rulii-spring.xsd` (renamed from `spring-rulii.xsd`) — full XSD schema for the `http://www.rulii.org/schema/rulii` namespace; defines types for all rule, ruleset, scripting, and predefined validation rule elements
- `META-INF/spring.handlers` and `META-INF/spring.schemas` updated to register `RuliiNamespaceHandler` and point to the new schema location

#### Predefined Validation Rules — XML Elements
37 built-in validation rules are now available as first-class XML elements inside `<r:ruleset>` or standalone:

| Element | Description |
|---|---|
| `<r:notNull>` | Value must not be null |
| `<r:notBlank>` | Value must not be blank |
| `<r:notEmpty>` | Collection / String / Array must not be empty |
| `<r:isNull>` | Value must be null |
| `<r:blank>` | Value must be blank |
| `<r:alpha>` | Value must contain only alphabetic characters |
| `<r:alphaNumeric>` | Value must contain only alphanumeric characters |
| `<r:ascii>` | Value must contain only ASCII characters |
| `<r:decimal>` | Value must be a valid decimal number |
| `<r:numeric>` | Value must be a numeric string |
| `<r:email>` | Value must be a valid e-mail address |
| `<r:url>` | Value must be a valid URL |
| `<r:lowerCase>` | Value must be all lower-case |
| `<r:upperCase>` | Value must be all upper-case |
| `<r:assertFalse>` | Value must be `false` |
| `<r:assertTrue>` | Value must be `true` |
| `<r:positive>` | Numeric value must be strictly positive |
| `<r:positiveOrZero>` | Numeric value must be positive or zero |
| `<r:negative>` | Numeric value must be strictly negative |
| `<r:negativeOrZero>` | Numeric value must be negative or zero |
| `<r:future>` | Date/time value must be in the future |
| `<r:futureOrPresent>` | Date/time value must be in the future or present |
| `<r:past>` | Date/time value must be in the past |
| `<r:pastOrPresent>` | Date/time value must be in the past or present |
| `<r:fileExists>` | Value must be a path to an existing file |
| `<r:min min="N">` | Numeric value must be ≥ `N` |
| `<r:max max="N">` | Numeric value must be ≤ `N` |
| `<r:decimalMin min="N">` | Decimal value must be ≥ `N` |
| `<r:decimalMax max="N">` | Decimal value must be ≤ `N` |
| `<r:size min="N" max="M">` | Size of collection / String / Array must be within `[N, M]` |
| `<r:digits maxInteger="N" maxFraction="M">` | Value must have at most `N` integer digits and `M` fractional digits |
| `<r:pattern regexp="...">` | Value must match the given regular expression |
| `<r:assertEquals value="...">` | Value must equal the given string |
| `<r:assertNotEquals value="...">` | Value must not equal the given string |
| `<r:startsWith values="...">` | Value must start with one of the given prefixes |
| `<r:endsWith values="...">` | Value must end with one of the given suffixes |
| `<r:in values="...">` | Value must be contained in the given comma-separated list |

All predefined elements share common attributes: `binding` (required), `errorCode`, `severity`, and `errorMessage`. Error messages support Spring property placeholder substitution (e.g. `${age.error.message}`).

---

### `@RuleScan` — XML Location Scanning

- New `xmlLocations` attribute on `@RuleScan` — accepts an array of classpath folder locations (e.g. `"classpath:rules/pricing/"`); all `*.xml` files found inside each folder are loaded via `XmlBeanDefinitionReader` and their bean definitions are merged into the same application context
- Class-based rules from `scanBasePackages` are always registered first; XML locations are loaded afterwards, preserving a predictable registration order
- `RuleRegistrar` updated to handle both `scanBasePackages` and `xmlLocations` in a single pass
- `RuleRegistrarMetaInfo` updated to carry both sets of metadata

---

### Test Coverage Added in 1.1.0

- `SpelScriptTest`, `SpelScriptCompilerTest`, `SpelScriptProcessorTest`, `SpelScriptProcessorFactoryTest`, `SpelScriptIntegrationTest` — full SpEL scripting pipeline
- `BindingAccessorTest` — verifies SpEL property access against live `Bindings`
- `RuleXmlTest` — XML-declared `<r:rule>` elements with conditions and actions
- `RuleSetXmlTest` — XML-declared `<r:ruleset>` elements with parameters and lifecycle hooks
- `ValidationRuleXmlTest` — inline script-based validation rules via XML
- `PredefinedValidationRuleXmlTest` — all 37 predefined validation rule elements
- `RuleRefXmlTest` — `<r:bean-ref>` and `<r:class-ref>` rule references inside rulesets
- `ScriptExpressionTest` — `ScriptExpression` parsing and resolution
- `RuleScanXmlLocationsTest` — pure XML-based rule discovery via `xmlLocations`
- `RuleScanCombinedTest` — combined class-scan + XML-location rule discovery and registration ordering
- `SpringRuleRegistryTest` — registry lookup by name and by class
- `SpringConverterAdapterTest` — Spring `ConversionService` adapter
- `SpringEnabledRuleContextOptionsTest` — rule context options wiring
- `SpringContextBindingLoaderTest` — Spring context binding loader
- `SpringEnvironmentMessageResolverTest` — property-based message resolution

---

## [1.0.0]

### Spring Boot Auto-Configuration (`org.rulii.spring.config`)

- `RuleConfig` — `@AutoConfiguration` class that provides all rulii Spring beans; every bean is `@ConditionalOnMissingBean` so applications can override individual components without disabling auto-configuration
- `BeanNames` — constants for all auto-configured bean names
- `RuleBeanBuilder` — helper that constructs a rulii `Rule` from an annotated class registered as a Spring bean

---

### Rule Discovery — `@RuleScan`

- `@RuleScan` — annotation placed on a `@Configuration` class to activate classpath scanning for `@Rule`-annotated classes; `scanBasePackages` attribute specifies which packages to scan
- `RuleRegistrar` — `ImportBeanDefinitionRegistrar` invoked by `@RuleScan`; drives classpath scanning and registers discovered rule classes as Spring bean definitions
- `RuleBeanDefinitionScanner` — extends Spring's `ClassPathBeanDefinitionScanner`; filters for classes annotated with `@Rule` and registers them as singleton bean definitions
- `RuleBeanDefinitionRegistryPostProcessor` — `BeanDefinitionRegistryPostProcessor` that finalises rule bean definitions after the main context refresh
- `RuleRegistrarMetaInfo` — value object carrying the scan configuration (base packages, annotation type) captured by `RuleRegistrar`

---

### Spring-Enabled Rule Context (`org.rulii.spring.context`)

- `SpringEnabledRuleContextOptions` — `RuleContextOptions` implementation that wires Spring-provided components (converters, message resolver, object factory) into the `RuleContext` at construction time; serves as the default options bean in auto-configuration

---

### Type Conversion (`org.rulii.spring.convert`)

- `SpringConverterAdapter` — adapts Spring's `ConversionService` to the rulii `Converter<S, T>` interface; automatically registered into the rulii `ConverterRegistry`
- `ResolvableTypeDescriptor` — bridges Spring's `ResolvableType` and rulii's type descriptor model

---

### Object Factory (`org.rulii.spring.factory`)

- `SpringObjectFactory` — `ObjectFactory` implementation that delegates instance creation to Spring's `AutowireCapableBeanFactory`; enables full dependency injection (constructor, field, setter) inside rule instances; listens to `ContextClosedEvent` for graceful shutdown

---

### Rule Registry (`org.rulii.spring.registry`)

- `SpringRuleRegistry` — `RuleRegistry` implementation backed by the Spring `ApplicationContext`; looks up `Rule` and `RuleSet` beans by name or by class; handles context shutdown gracefully

---

### Message Resolution (`org.rulii.spring.text`)

- `SpringEnvironmentMessageResolver` — `MessageResolver` implementation that reads messages from the Spring `Environment` (i.e. `application.properties` / `application.yaml`); supports property placeholder substitution in error messages

---

### Spring Context Binding Loader (`org.rulii.spring.bind.load`)

- `SpringContextBindingLoader` — `BindingLoader` that populates a `Bindings` instance from Spring `ApplicationContext` beans; useful for injecting context-level values into rule execution

---

### GitHub CI

- `maven.yml` — GitHub Actions workflow that builds and tests the project on every push and pull request