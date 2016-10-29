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
package com.samsung.sjs.types;

/**
 * This is the type assigned to unconstrained elements during
 * type inference. It is, for all intents and purposes, exactly
 * the same as {@link IntegerType}, but it has a different
 * implementation of {@link Object#toString()} and it can be
 * differentiated from <code>IntegerType</code> using
 * "<code>x instanceof DefaultType</code>". This is useful for
 * explaining type errors in a more sane manner after type
 * inference completes.
 *
 * <p>Note that a <code>DefaultType</code> is always
 * {@link Object#equals(Object)} to an <code>IntegerType</code>
 * and vice-versa, and they have the same hash code.
 */
public class DefaultType extends IntegerType {

    public static final DefaultType SINGLETON = new DefaultType();

    @Override
    public String toString() {
        return "___";
    }

}
