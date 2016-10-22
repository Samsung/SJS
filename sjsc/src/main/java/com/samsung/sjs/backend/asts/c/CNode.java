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
 * Parent class for representing C code (for serialization)
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend.asts.c;

// The subclasses we need are:
// toplevel:
// +IncludeDirective (really just for including the object layout stuff)
// +FunctionDeclaration
// +VariableDeclaration // top level and local?
// +For/While/Do loops
// +IfStatement
// +BlockStatement
// +ReturnStatement
// +BreakStatement
// +ContinueStatement
// +ExpressionStatement
// ...
// Expressions:
//     Variable
//     UnaryExpression
//     BinaryExpression
//     FunctionCall (probably suffices for macro invocation)
//     ArrayRead
//     ArrayWrite
//     IntLiteral
//     StringLiteral
//     VariableAssignment
//     Dereference
// Type:
//      +Void
//      +Char
//      +Double
//      FnPtr
//      integer type?

public abstract class CNode {
    public abstract String toSource(int indentLevel);
    protected final void indent(int n, StringBuilder b) {
        for(int i = 0; i < n; i++) {
            b.append("  ");
        }
    }
    public boolean isExpression() { return false; }
    public Expression asExpression() { throw new UnsupportedOperationException(); }
    public boolean isStatement() { return false; }
    public Statement asStatement() { throw new UnsupportedOperationException(); }
}
