---
name: new-xml-ruleflow
description: Guide for declaring rulii RuleFlows in Spring XML using the rulii namespace — covers the command grammar (run/bind/when/for-each/scope), async execution, exception handling, custom commands, terse attribute forms, and running flows
user-invocable: true
---

# Declaring RuleFlows in Spring XML

Use this guide when the user wants to define a rulii `RuleFlow` — a pipeline of commands composing Rules, RuleSets, and other RuleFlows — in a Spring XML context file using the `http://www.rulii.org/schema/rulii` namespace. The XML grammar mirrors the `RuleFlow.builder()` DSL, with script expressions substituting the builder's lambdas.

---

## 1. Namespace declaration (required boilerplate)

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

    <!-- ... flows, rules, rulesets go here ... -->

</beans>
```

Load the file via `@RuleScan(xmlLocations = "classpath:rules/flows/")` like any other rulii XML. Flows are ordinary Spring beans: retrievable with `ruleRegistry.getRuleFlow("name")` and enumerable with `getRuleFlows()`.

---

## 2. Flow skeleton — element order matters

```xml
<r:ruleflow name="CheckoutFlow" description="...">
    <r:context ref="..."/>          <!-- optional, must be FIRST -->
    <r:param .../>                  <!-- optional, repeatable -->

    <!-- commands, in execution order -->

    <r:on-exception .../>           <!-- optional: flow-level (global) handler -->
    <r:finalizer .../>              <!-- optional: always runs, even on exit/error -->
    <r:returning .../>              <!-- optional: extracts the flow's return value -->
</r:ruleflow>
```

`<r:param>` uses the same grammar as rulesets: `<r:param name="order" type="com.example.Order" required="false"/>` with an optional `<r:default-value>` child.

---

## 3. The commands

| Element | Purpose |
|---|---|
| `<r:bind>` | Bind a value: literal `value=`, Spring bean `ref=`, or a script expression (evaluated per execution) |
| `<r:run>` | Run a Rule / RuleSet / RuleFlow |
| `<r:async-run>` | Run one asynchronously; result bound as a `CompletableFuture` |
| `<r:apply>` | Evaluate a function expression, bind its result via `as=` |
| `<r:execute>` | Execute an action expression (side effect) |
| `<r:when>` | Conditional with `<r:then>` body and optional `<r:otherwise>` body |
| `<r:for-each>` | Iterate a source collection, binding each element |
| `<r:scope>` | Run a body inside a named binding scope |
| `<r:await>` / `<r:await-all>` / `<r:await-any>` | Block on async results |
| `<r:exit/>` | Stop the flow early (finalizer still runs) |
| `<r:command>` | A user-supplied `RuleFlowCommand` bean (see §8) |

Bodies (`then`, `otherwise`, `for-each`, `scope`, `then-run`, `on-exception`) contain the same command grammar — nesting is unlimited.

---

## 4. Run targets — exactly one of three forms

```xml
<r:run bean-ref="pricingRuleSet"/>    <!-- Spring bean, resolved at WIRING time (startup) -->
<r:run name="AgeCheckRule"/>          <!-- RuleRegistry lookup by name at EXECUTION time -->
<r:run class="com.example.TaxRule"/>  <!-- RuleRegistry lookup by rule class at EXECUTION time -->
```

- `bean-ref` fails at startup if the bean is missing — safest for fixed wiring
- `name`/`class` defer to the registry each execution — use when the rule set is dynamic
- Bind the result with `as="result"` (optionally `scope="scopeName"`); supply step-scoped **literal** parameters with `<r:with name="x" value="10"/>` children (with-param values are constants — a core `BindingDeclaration` semantic; for computed values `bind` before the run)

---

## 5. Conditionals, loops, scopes

```xml
<r:when condition="#ctx.total >= ${order.minTotal:100}">
    <r:then>
        <r:run name="ApplyDiscountRule"/>
    </r:then>
    <r:otherwise>
        <r:exit/>
    </r:otherwise>
</r:when>

<r:for-each item="n" source="{1, 2, 3}" stop-condition="#ctx.done == true">
    <r:execute>#ctx.sum = #ctx.sum + #ctx.n</r:execute>
</r:for-each>

<r:scope name="tmp">
    <r:bind name="scratch">#ctx.total * 0.1</r:bind>
</r:scope>
```

`condition`, `source`, and `stop-condition` may also be child elements (`<r:condition>`, `<r:source>`, `<r:stop-condition>`) — required when you need a per-expression `language` override. Declaring both forms is a parse error.

---

## 6. Async execution

```xml
<r:async-run bean-ref="ScoreFlow" as="f1"/>
<r:async-run bean-ref="RiskFlow" as="f2" context-mode="immutable">
    <r:then-run result="risk">
        <r:execute>#ctx.riskLevel = #ctx.risk</r:execute>   <!-- continuation, non-blocking -->
    </r:then-run>
</r:async-run>

<r:await name="f1" timeout="10" unit="seconds"/>
<r:await-all names="f1, f2" timeout="30" unit="seconds"/>
```

- Runs on the Spring-configured `rulii.executorService` pool
- `context-mode`: `shared` (default) or `immutable` (async task gets an immutable snapshot)
- `await` **blocks but leaves the future bound** — read the value afterwards with `#ctx.f1.get()`
- Avoid launching multiple `shared`-mode children concurrently that write bindings — the scope stack is shared; serialize with `await` between launches, or use `immutable`

---

## 7. Exception handling

```xml
<!-- Step-level: attached to run / apply / execute / async-run -->
<r:execute>
    #ctx.risky.call()
    <r:on-exception exception="org.rulii.model.UnrulyException">
        <r:execute>#ctx.recovered = true</r:execute>
    </r:on-exception>
</r:execute>

<!-- Flow-level (global), declared after the commands -->
<r:on-exception exception="java.lang.Exception">
    <r:execute>#ctx.failed = true</r:execute>
</r:on-exception>
```

The caught exception is bound as `ex` inside the handler body (`#ctx.ex.message`). Step handler is checked first, then the global handler; a matched handler swallows the exception and execution continues. `exception` defaults to `java.lang.Exception`.

---

## 8. Custom commands (`<r:command>`)

**Leaf command** — a Spring bean implementing `RuleFlowCommand`:

```xml
<r:command ref="publishOrderCommand"/>
```

**Container command** — a bean extending `ContainerCommand` (retry, parallel, …) receives the nested body:

```xml
<r:command ref="retryCommand">
    <r:run name="ChargeCardRule"/>
    <r:execute>#ctx.attempts = #ctx.attempts + 1</r:execute>
</r:command>
```

> **Container beans must be `@Scope("prototype")` when referenced from more than one `<r:command>`** — `setBody()` mutates the bean, and reuse fails fast at startup with exactly this remedy. Keep per-execution state in the bindings, never in command fields (the instance is shared across executions and threads).

There is also `<r:context ref="..."/>` (first element only) — a `Consumer<RuleContextBuilder>` bean for programmatic context configuration.

---

## 9. Results and observability — the flow-scope rule

**Flow-internal bindings live in the flow's own scope, which is removed after execution.** `bind` results and `as=` bindings are invisible once `run()` returns. Two ways to get data out:

1. **`returning`** — extracts the flow's return value (runs inside the scope, so it sees everything):
   ```xml
   <r:returning>#ctx.total</r:returning>          <!-- or the attribute form: returning="#ctx.total" -->
   ```
   An empty `<r:returning/>` returns the `RuleContext` itself.

2. **Write-through to pre-bound bindings** — `#ctx.x = ...` sets an *existing* caller binding in place:
   ```java
   RuleContext ctx = RuleContext.builder().with(ruleContextOptions).build();
   ctx.getBindings().bind("approved", false);     // pre-bind the observation slot
   flow.run(ctx);
   ctx.getBindings().getValue("approved");        // set by the flow via #ctx.approved = true
   ```

The finalizer always runs — including on `<r:exit/>` and on errors.

---

## 10. Terse attribute forms

Single-expression slots can be attributes instead of child elements (file default language only; both forms together = parse error):

```xml
<r:ruleflow name="SquareFlow" returning="#ctx.sq" finalizer="#ctx.done = true">
    <r:param name="v" type="java.lang.Integer"/>
    <r:apply as="sq">#ctx.v * #ctx.v</r:apply>
</r:ruleflow>
```

Available on `<r:ruleflow>`: `finalizer`, `returning`. On `<r:when>`: `condition`. On `<r:for-each>`: `source`, `stop-condition`.

---

## 11. Environment placeholders

Any script expression may contain `${property:default}` placeholders, resolved once at startup with `@Value` semantics (missing key without a default fails startup; `\${...}` escapes):

```xml
<r:when condition="#ctx.total >= ${order.minTotal:100}">
```

---

## 12. Running a flow from Java

```java
@Autowired RuleRegistry ruleRegistry;
@Autowired RuleContextOptions options;      // the auto-configured Spring options

RuleFlow<?> flow = ruleRegistry.getRuleFlow("CheckoutFlow");
RuleContext ctx = RuleContext.builder().with(options).build();
ctx.getBindings().bind("total", 150);
Object result = flow.run(ctx);
```

Building the context `.with(options)` is what wires the Spring registry (for `run name=`/`class=` lookups), the Spring executor (for `async-run`), converters, and tracing into the flow.

---

## 13. Full example

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

    <r:rule name="ValidateOrderRule" given="#ctx.order != null" then="#ctx.valid = true"/>

    <r:ruleflow name="CheckoutFlow" description="Validates, prices, and finalizes an order"
                finalizer="#ctx.audited = true" returning="#ctx.status">
        <r:param name="order" type="com.example.Order"/>

        <r:bind name="status" value="pending"/>
        <r:run name="ValidateOrderRule"/>

        <r:when condition="#ctx.valid == true">
            <r:then>
                <r:async-run bean-ref="pricingFlow" as="priceFuture"/>
                <r:await name="priceFuture" timeout="10" unit="seconds"/>
                <r:execute>#ctx.status = 'priced'</r:execute>
            </r:then>
            <r:otherwise>
                <r:execute>#ctx.status = 'rejected'</r:execute>
                <r:exit/>
            </r:otherwise>
        </r:when>

        <r:command ref="publishOrderCommand"/>

        <r:on-exception exception="java.lang.Exception">
            <r:execute>#ctx.status = 'failed'</r:execute>
        </r:on-exception>
    </r:ruleflow>

</beans>
```

---

## Common Mistakes

| Mistake | Fix |
|---------|-----|
| Asserting on `bind`/`as=` results after `flow.run()` | Flow scope is removed after execution — use `returning` or write through to pre-bound bindings with `#ctx.x = ...` |
| Two run targets on one element (`bean-ref` + `name`) | Exactly one of `bean-ref` / `name` / `class` — parse error otherwise |
| Reading an awaited result with `#ctx.f1` | `await` leaves the `CompletableFuture` bound — use `#ctx.f1.get()` |
| `<r:with>` with a computed expression | With-param values are literals (core semantic) — `bind` the computed value before the run instead |
| A singleton `ContainerCommand` bean in two `<r:command>` bodies | `setBody()` mutates the bean — declare it `@Scope("prototype")` |
| `<r:context>` not first | The schema requires it before params/commands |
| Running with `RuleContext.builder().standard()` | `run name=`/`class=` lookups and `async-run` need the Spring wiring — build with `.with(ruleContextOptions)` |
| Concurrent `shared`-mode `async-run`s writing bindings | The scope stack is shared — serialize with `await`, or use `context-mode="immutable"` |
