/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2026, Algorithmx Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rulii.spring.annotation;

import org.rulii.spring.config.RuleRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Annotation used to enable scanning for rule classes in specified base packages.
 * Rules found during scanning will be registered in the Spring application context.
 *
 * <p>The scan runs in one of three modes, depending on which attributes are set:
 * <ul>
 *   <li><b>Neither attribute</b> — the package of the annotated class is scanned for
 *       {@code @Rule} classes (mirroring {@code @ComponentScan} semantics)</li>
 *   <li><b>{@link #scanBasePackages()}</b> — the declared packages are scanned;
 *       any {@link #xmlLocations()} are loaded afterwards</li>
 *   <li><b>{@link #xmlLocations()} only</b> — XML-only: the declared locations are
 *       loaded and <em>no</em> class scanning is performed</li>
 * </ul>
 *
 * @author Max Arulananthan
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(RuleRegistrar.class)
public @interface RuleScan {

    /**
     * Retrieve the base packages to be scanned for rule classes.
     *
     * @return an array of strings representing the base packages for scanning
     */
    String[] scanBasePackages() default {};

    /**
     * Resource locations of Spring XML context files that declare rulii rules
     * and rulesets. Each entry may take one of three forms:
     *
     * <ul>
     *   <li><b>Folder</b> (e.g. {@code "classpath:rules/pricing/"}) — all {@code *.xml}
     *       files directly inside it are loaded; a {@code classpath:} prefix is upgraded
     *       to {@code classpath*:} so all classpath roots (e.g. rule sets shipped in
     *       separate jars) are searched</li>
     *   <li><b>File</b> (e.g. {@code "classpath:rules/pricing/pricing-rules.xml"}) —
     *       that file is loaded; startup fails if it does not exist</li>
     *   <li><b>Pattern</b> (e.g. {@code "classpath*:rules/**&#47;*-rules.xml"}) —
     *       used as-is for resource resolution</li>
     * </ul>
     *
     * <p>All matched files are loaded via
     * {@link org.springframework.beans.factory.xml.XmlBeanDefinitionReader}
     * and their bean definitions are registered into the same application context.
     *
     * <p>Class-based rules from {@link #scanBasePackages()} are always registered
     * first; XML locations are loaded afterwards. Declaring only {@code xmlLocations}
     * (with no {@code scanBasePackages}) is XML-only: no class scanning is performed.
     *
     * @return an array of locations to load XML rule context files from
     */
    String[] xmlLocations() default {};
}


