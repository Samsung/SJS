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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypesTest {

    private static <T> ArrayList<T> oneElementArrayList(T o) {
        ArrayList<T> a = new ArrayList<>(1);
        a.add(o);
        return a;
    }

    @Test
    public void testBasics() {
        Type i = IntegerType.make();
        Type f = FloatType.make();

        Assert.assertTrue(Types.isEqual(i, i));
        Assert.assertTrue(Types.isEqual(f, f));
        Assert.assertFalse(Types.isEqual(f, i));

        Assert.assertTrue(Types.isSubtype(i, f));
        Assert.assertFalse(Types.isSubtype(f, i));
    }

    @Test
    public void testRecursiveEquality() {
        ObjectType o1 = new ObjectType();
        o1.setProperty("f", o1);
        ObjectType o2 = new ObjectType();
        o2.setProperty("f", o2);
        Assert.assertTrue(Types.isEqual(o1, o2));

        FunctionType f1 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f1.setParamType(f1, 0);
        FunctionType f2 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f2.setParamType(f2, 0);
        Assert.assertTrue(Types.isEqual(f1, f2));
    }

    @Test
    public void testMutualRecursiveEquality() {
        ObjectType o1 = new ObjectType();
        ObjectType o2 = new ObjectType();
        o1.setProperty("f", o2);
        o2.setProperty("f", o1);
        Assert.assertTrue(Types.isEqual(o1, o2));

        FunctionType f1 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        FunctionType f2 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f1.setParamType(f2, 0);
        f2.setParamType(f1, 0);
        Assert.assertTrue(Types.isEqual(f1, f2));
    }

    @Test
    public void testRecursiveSubtype() {
        ObjectType o1 = new ObjectType();
        o1.setProperty("f", o1);
        ObjectType o2 = new ObjectType();
        o2.setProperty("f", o2);
        Assert.assertTrue(Types.isSubtype(o1, o2));

        FunctionType f1 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f1.setParamType(f1, 0);
        FunctionType f2 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f2.setParamType(f2, 0);
        Assert.assertTrue(Types.isSubtype(f1, f2));
    }

    @Test
    public void testMutualRecursiveSubtype() {
        ObjectType o1 = new ObjectType();
        ObjectType o2 = new ObjectType();
        o1.setProperty("f", o2);
        o2.setProperty("f", o1);
        Assert.assertTrue(Types.isSubtype(o1, o2));

        FunctionType f1 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        FunctionType f2 = new FunctionType(oneElementArrayList(null), oneElementArrayList("1"), new VoidType());
        f1.setParamType(f2, 0);
        f2.setParamType(f1, 0);
        Assert.assertTrue(Types.isSubtype(f1, f2));
    }

}
