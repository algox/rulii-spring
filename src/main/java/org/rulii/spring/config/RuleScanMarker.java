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
package org.rulii.spring.config;

/**
 * Internal marker bean registered by {@link RuleRegistrar} once {@code @RuleScan} has been
 * processed. Its presence disables {@link RuleConfig}'s auto-scan fallback.
 *
 * <p>Deliberately package-private so applications cannot define one accidentally - the
 * condition it drives means exactly "{@code @RuleScan} was processed" and nothing else
 * (a user-defined {@link RuleRegistrarMetaInfo} bean, in particular, has no effect).
 *
 * @author Max Arulananthan
 * @since 2.0
 */
final class RuleScanMarker {

    RuleScanMarker() {
        super();
    }
}
