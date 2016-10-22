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
 * represent arbitrary objects
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c.types;
// This type hard-codes a type name defined in a C header we always include
// in generated code.
public class ObjectPseudoType extends CType {
    public String toSource() { return "object_t*"; }
    public ObjectPseudoType() { super(1); }
}
