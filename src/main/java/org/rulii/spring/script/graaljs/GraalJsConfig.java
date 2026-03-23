package org.rulii.spring.script.graaljs;

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.rulii.script.graaljs.GraalJsScriptProcessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(GraalJSScriptEngine.class)
public class GraalJsConfig {

    public GraalJsConfig() {
        super();
    }

    @Bean
    @ConditionalOnMissingBean(GraalJsScriptProcessorFactory.class)
    public GraalJsScriptProcessorFactory graalJsScriptProcessor(@Value("${graal.js.languageName:js}") String languageName,
                                                                @Value("${graal.js.bindingName:ctx}") String bindingName) {
        return new GraalJsScriptProcessorFactory(languageName, bindingName);
    }
}
