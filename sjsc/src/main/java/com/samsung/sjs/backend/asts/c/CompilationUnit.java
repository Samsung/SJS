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
 * Representation of a C compilation unit
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;
import java.util.*;
import java.io.*;
public class CompilationUnit implements Iterable<Statement> {
    private List<Statement> toplevels;
    private List<Statement> header;
    public CompilationUnit() {
        toplevels = new LinkedList<Statement>();
        header = new LinkedList<Statement>();
    }
    public void addStatement(Statement s) {
        toplevels.add(s);
    }
    public void addExpressionStatement(Expression e) {
        toplevels.add(new ExpressionStatement(e));
    }
    public Iterator<Statement> iterator() {
        return toplevels.iterator();
    }
    public void writeToDisk(String filename) throws IOException {
        if (!filename.endsWith(".c")) {
            throw new IllegalArgumentException("Filename does not end in .c: "+filename);
        }

        // Write main .c file
        FileWriter fw = new FileWriter(filename);
        for (Statement s : toplevels) {
            fw.write(s.toSource(0));
        }
        fw.flush();
        fw.close();

        // Write export header
        String headerf = filename.substring(0, filename.lastIndexOf("."))+".h";
        fw = new FileWriter(headerf);
        for (Statement s : header) {
            fw.write(s.toSource(0));
        }
        fw.flush();
        fw.close();
    }


    /* For #define'ing property names for linking against C code */
    public void declarePropertyConstant(String propname, int offset) {
        header.add(new DefineDirective("___js_"+propname+"\t"+offset));
        toplevels.add(new DefineDirective("___js_"+propname+"\t"+offset));
    }
    public void exportString(String s) {
        header.add(new ExpressionStatement(new InlineCCode(s)));
    }
    public void exportIndirectionMap(String name) {
        header.add(new ExpressionStatement(new InlineCCode("extern int "+name+"[]")));
    }

}
