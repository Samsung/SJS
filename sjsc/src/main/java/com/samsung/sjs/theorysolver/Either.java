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
package com.samsung.sjs.theorysolver;

/**
 * A type which holds either a left value or a right value. Build one
 * using {@link #left(Object)} or {@link #right(Object)}.
 * @param <L> the type of the left value
 * @param <R> the type of the right value
 */
public class Either<L,R> {
    public static <L,R> Either<L,R> left(L left) { return new Either<>(left, null); }
    public static <L,R> Either<L,R> right(R right) { return new Either<>(null, right); }
    public final L left;
    public final R right;
    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }
}
