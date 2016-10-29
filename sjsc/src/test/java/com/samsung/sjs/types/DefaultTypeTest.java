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

import org.junit.Assert;
import org.junit.Test;

public class DefaultTypeTest {

    /**
     * {@link DefaultType} should be (mostly) indistinguishable
     * from {@link IntegerType}.
     */
    @Test
    public void testBehavior() {
        Type i = IntegerType.make();
        Type j = DefaultType.SINGLETON;

        Assert.assertTrue(i.hashCode() == j.hashCode());
        Assert.assertTrue(i.equals(j));
        Assert.assertTrue(j.equals(i));

        Assert.assertTrue(i instanceof IntegerType);
        Assert.assertTrue(j instanceof IntegerType);
        Assert.assertFalse(i instanceof DefaultType);
        Assert.assertTrue(j instanceof DefaultType);

        Assert.assertTrue(Types.isEqual(i, j));
        Assert.assertTrue(Types.isEqual(j, i));
    }

    /**
     * {@link DefaultType} should be indistinguishable from
     * {@link IntegerType} when it comes to subtyping.
     */
    @Test
    public void testSubtyping() {
        Type i = IntegerType.make();
        Type j = DefaultType.SINGLETON;
        Type f = FloatType.make();

        Assert.assertTrue(Types.isSubtype(j, f));
        Assert.assertFalse(Types.isSubtype(f, j));

        Assert.assertTrue(Types.isSubtype(i, j));
        Assert.assertTrue(Types.isSubtype(j, i));
    }

}
