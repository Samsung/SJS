/* 
 * Copyright 2014-2016 Samsung Research America, Inc.
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
/**
 * Representation of the C type that the C backend uses to
 * represent closure environments.  (An array of value pointers.)
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c.types;
import com.samsung.sjs.backend.EnvironmentLayout;
// This type hard-codes a type name defined in a C header we always include
// in generated code.  It corresponds to the environment argument for a converted closure's body
public final class EnvironmentPseudoType extends CType {
    private EnvironmentLayout js_layout;
    public String toSource() { return "env_t"; }
    public EnvironmentPseudoType() { super(0); }
    public EnvironmentPseudoType(EnvironmentLayout layout) {
        super(0);
        js_layout = layout;
    }

    public EnvironmentLayout getLayout() { return js_layout; }
}
