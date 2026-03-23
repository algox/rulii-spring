package org.rulii.spring.test.script.el;

import org.junit.jupiter.api.Test;
import org.rulii.bind.Bindings;
import org.rulii.context.RuleContext;
import org.rulii.script.Script;
import org.rulii.spring.script.el.SpringELScriptProcessorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScriptTests {

    public ScriptTests() {
        super();
    }

    @Test
    public void test1() {
        Script<Integer> script = Script.builder().build("el", "#ctx.c = #ctx.a + #ctx.b");

        RuleContext ctx = RuleContext.builder().standard()
                .bindings(Bindings.builder().standard(a -> 10, b -> 20))
                .scriptUsing(new SpringELScriptProcessorFactory())
                .build();

        int result = script.run(ctx);
        assertEquals(30, result);
        assertEquals(30, (Integer) ctx.getBindings().getValue("c"));
    }
}
