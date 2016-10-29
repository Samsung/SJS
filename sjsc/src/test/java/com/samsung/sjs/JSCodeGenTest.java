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
 * Unit tests for C Code generation backend
 *
 * @author colin.gordon
 */
package com.samsung.sjs;

import com.samsung.sjs.backend.asts.c.*;
import com.samsung.sjs.backend.asts.c.types.*;

import java.io.*;
import java.net.*;
import java.nio.file.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Ignore;

@Ignore("these may have bitrotted, not testing emscripten backend for now")
public class JSCodeGenTest extends ABackendTest
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JSCodeGenTest( String testName ) throws IOException
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( JSCodeGenTest.class );
    }

    public void test_assert() {
        asmTest();
    }
    public void test_store1() {
        asmTest();
    }
    public void test_store2() {
        asmTest();
    }
    public void test_bool() {
        asmTest();
    }
    public void test_update1() {
        asmTest();
    }
    public void test_lambda1() {
        asmTest();
    }
    public void test_lambda2() {
        asmTest();
    }
    public void test_lambda3() {
        asmTest();
    }
    public void test_lambda4() {
        asmTest();
    }
    public void test_lambda5() {
        asmTest();
    }
    public void test_printstub() {
        asmTest();
    }
    public void test_console1() {
        asmTest();
    }
    public void test_console2() {
        asmTest();
    }
    public void test_objlit1() {
        asmTest();
    }
    public void test_objlit2() {
        asmTest();
    }
    public void test_objlit3() {
        asmTest();
    }
    public void test_objlit4() {
        asmTest();
    }
    public void test_objlit5() {
        asmTest();
    }
    public void test_fib() {
        asmTest();
    }
    public void test_directcalls() {
        asmTest();
    }
    public void test_consoleassert1() {
        asmTest();
    }
    public void test_consolewarn() {
        asmTest();
    }
    public void test_consoleerror() {
        asmTest();
    }
    public void test_arraylit1() {
        asmTest();
    }
    public void test_arraylit2() {
        asmTest();
    }
    public void test_arraylit3() {
        asmTest();
    }
    public void test_forloop1() {
        asmTest();
    }
    public void test_forloop2() {
        asmTest();
    }
    public void test_arraylit4() {
        asmTest();
    }
    public void test_environments1() {
        asmTest();
    }
    public void test_mathtest() {
        asmTest();
    }
    public void test_constructors1() {
        asmTest();
    }
    public void test_nsieve() {
        asmTest();
    }
    public void test_2darray() {
        asmTest();
    }
    public void test_rectype_ctor1() {
        asmTest();
    }
    public void test_nbody() {
        asmTest();
    }
}
