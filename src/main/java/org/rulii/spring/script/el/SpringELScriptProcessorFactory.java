package org.rulii.spring.script.el;

import org.rulii.lib.spring.util.Assert;
import org.rulii.script.ScriptOptions;
import org.rulii.script.ScriptProcessor;
import org.rulii.script.ScriptProcessorFactory;

public class SpringELScriptProcessorFactory implements ScriptProcessorFactory {

    private final String languageName;
    private final String bindingName;

    public SpringELScriptProcessorFactory() {
        this("el", ScriptOptions.DEFAULT.bindingsName());
    }

    public SpringELScriptProcessorFactory(String languageName, String bindingName) {
        super();
        Assert.hasText(languageName, "languageName cannot be empty.");
        Assert.hasText(bindingName, "bindingName cannot be empty.");
        this.languageName = languageName;
        this.bindingName = bindingName;
    }

    @Override
    public String getLanguageName() {
        return languageName;
    }

    @Override
    public String getBindingName() {
        return bindingName;
    }

    @Override
    public ScriptProcessor create() {
        return new SpringELScriptProcessor(languageName, bindingName);
    }
}
