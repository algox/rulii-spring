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
package org.rulii.spring.testrules.directimport;

import org.rulii.spring.config.RuleRegistrar;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

// Imports RuleRegistrar directly - deliberately NO @RuleScan - to exercise the
// bare-annotation fallback (scan this package, no XML). Plain @Configuration
// (not @SpringBootConfiguration) so Boot's default configuration resolution
// never discovers it.
@Configuration
@EnableAutoConfiguration
@Import(RuleRegistrar.class)
public class DirectImportConfig {

    public DirectImportConfig() {
        super();
    }
}
