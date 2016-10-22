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
 * Enumeration for distinguishing between type _representations_ in the backend
 *
 * @author colin.gordon
 */

package com.samsung.sjs.types;

public enum RepresentationSort {

    INT,
    BOOL,
    STRING,
    FLOAT,
    OBJECT, // includes arrays, maps
    CODE,
    TOPREF,

    NEVERBOXED, // map iterator, environment... types that are physically manifest, but never treated as general values
    UNREPRESENTABLE // void, top; types that shouldn't describe runtime values

}
