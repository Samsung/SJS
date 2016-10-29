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
 * Conversion pass taking a decorated Rhino AST to our IR
 *
 * @author colin.gordon
 */
package com.samsung.sjs.backend;

import org.mozilla.javascript.ast.*;
import org.mozilla.javascript.*;

import com.samsung.sjs.ExternalRhinoVisitor;
import com.samsung.sjs.CompilerOptions;
import com.samsung.sjs.FFILinkage;
import com.samsung.sjs.JSEnvironment;
import com.samsung.sjs.backend.asts.ir.*;
import com.samsung.sjs.types.*;

import java.util.*;

// NOTE: Static import
import static com.samsung.sjs.backend.asts.ir.IRManipulator.*;

public class RhinoToIR extends ExternalRhinoVisitor {

    private boolean debug;
    private AstRoot source;
    private Map<AstNode,Type> types;
    private com.samsung.sjs.backend.asts.ir.Scope currentScope;
    private com.samsung.sjs.backend.asts.ir.Function currentFunction;
    private CompilerOptions options;
    private com.samsung.sjs.backend.asts.ir.Block currentBlockScope = null;
    private com.samsung.sjs.backend.asts.ir.Block current_closure_hoist = null;

    private IRNode lastChildResult;

    public RhinoToIR(CompilerOptions opts,
                     AstRoot source,
                     Map<AstNode,Type> types) {
        options = opts;
        this.debug = opts.debug();
        this.source = source;
        this.types = types;
    }

    private void populateGlobals(com.samsung.sjs.backend.asts.ir.Scope s) {
        JSEnvironment env = options.getRuntimeEnvironment();
        for (Map.Entry<String,Type> e : env.entrySet()) {
            if (debug) {
                System.err.println("|- "+e.getKey()+" : "+e.getValue().toString());
            }
            s.declareVariable(mkVar(e.getKey()), e.getValue());
        }
    }

    /** Translate the Rhino {@code AstRoot} object passed to the constructor
     * into an IR {@code Script}.
    */
    public com.samsung.sjs.backend.asts.ir.Script convert() {
        com.samsung.sjs.backend.asts.ir.Block b = mkBlock();
        currentBlockScope = b;
        current_closure_hoist = mkBlock();
        b.addStatement(current_closure_hoist);
        com.samsung.sjs.backend.asts.ir.Scope globals =
             new com.samsung.sjs.backend.asts.ir.Scope(null); // global scope
        populateGlobals(globals);
        // currentScope will become the scope for main()
        currentScope = new com.samsung.sjs.backend.asts.ir.Scope(globals);
        // TODO: Build an FFI framework, and hook into that to populate the global scope
        for (Node n : source) {
            if (!(n instanceof EmptyExpression) && !(n instanceof EmptyStatement)) {
                if (debug) {
                    System.err.println("Translating node type ["+n.getType()+"]: "+((AstNode)n).toSource(0));
                }
                visit((AstNode)n);
                assert (currentScope.getParentScope() == globals);
                IRNode ir = lastChildResult;
                if (debug) {
                    System.err.println("Translation result: "+ir.toSource(0));
                }
                //if (ir.declaresVariables()) {
                //    if (debug) {
                //        System.err.println("Declaring variable: "+ir.toSource(0));
                //    }
                //    ir.asDeclaration().declareInScope(currentScope);
                //}
                if (ir.isStatement()) {
                    b.addStatement(ir.asStatement());
                } else {
                    b.addStatement(mkExpressionStatement(ir.asExpression()));
                }
            }
        }
        return new com.samsung.sjs.backend.asts.ir.Script(b, currentScope);
    }

    @Override
    protected void visitEmpty(Node node) {
        // TODO: add an empty statement node type to the SJS AST?
        lastChildResult = mkBlock();
        lastChildResult.setType(new VoidType());
    }

    @Override
    protected void visitName(Name node) {
        if (node.getIdentifier().equals("undefined")) {
            lastChildResult = mkUndefined();
        } else {
            lastChildResult = new Var(node.getIdentifier());
        }
        Type t = getType(node);
        assert (t != null);
        lastChildResult.setType(t);
    }

    @Override
    protected void visitElementGet(ElementGet node) {
        visit(node.getTarget());
        Expression arr = lastChildResult.asExpression();
        visit(node.getElement());
        Expression offset = lastChildResult.asExpression();
        lastChildResult = mkArrayIndex(arr, offset);
        assert (getType(node) != null);
        lastChildResult.setType(getType(node));
    }

    @Override
    protected void visitNumber(NumberLiteral node) {
        assert (getType(node).isPrimitive());
        if (getType(node) instanceof IntegerType) {
            lastChildResult = new IntLiteral((long)node.getNumber());
            lastChildResult.setType(Types.mkInt());
        } else {
            assert (getType(node) instanceof FloatType);
            lastChildResult = new FloatLiteral(node.getNumber());
            lastChildResult.setType(Types.mkFloat());
        }
    }

    // TODO: Remove once the new frontend comes online with proper method types
    // Until then, we use this hack, assuming there are no nested constructors.  This lets us infer
    // the type of 'this' expressions inside methods attached inside the constructor, which are
    // currently unannotated by the old frontend
    Type type_under_construction = null;

    @Override
    protected void visitLiteral(AstNode node) {
        // This only treats true, false, this, null, and debugger
        switch (node.getType()) {
            case Token.TRUE:
                lastChildResult = new BoolLiteral(true);
                lastChildResult.setType(Types.mkBool());
                break;
            case Token.FALSE:
                lastChildResult = new BoolLiteral(false);
                lastChildResult.setType(Types.mkBool());
                break;
            case Token.NULL:
                lastChildResult = new NullLiteral();
                lastChildResult.setType(getType(node));
                break;
            case Token.THIS:
                lastChildResult = new ThisLiteral();
                assert (currentFunction != null);
                assert (currentFunction.getType().isAttachedMethod() ||
                            currentFunction.getType().isUnattachedMethod() ||
                            currentFunction.getType().isConstructor());
                if (currentFunction.getType().isConstructor()) {
                    lastChildResult.setType(((ConstructorType)currentFunction.getType()).returnType());
                } else {
                    currentFunction.markMethod();
                    lastChildResult.setType(((UnattachedMethodType)currentFunction.getType()).receiverType());
                }
                if (getType(node) != null) {
                    lastChildResult.setType(getType(node));
                }
                break;
            case Token.DEBUGGER: // TODO: implement debugger stub interface, which will need the field indirection map to be computed along with other maps
            default:
                throw new UnsupportedOperationException();
        }
    }

    protected Type getType(AstNode n) {
        return types.get(n);
    }
    protected void hack_type(AstNode n, Type t) {
        types.put(n, t);
    }

    @Override
    protected void visitVariableInitializers(VariableDeclaration node) {
        List<VariableInitializer> vars = node.getVariables();
        CompoundStatement cs = mkCompoundStatement();
        for (VariableInitializer vi : vars) {
            Type t = getType(vi.getTarget());
            assert (t != null);
            visit(vi.getTarget());
            Var v = (Var)lastChildResult.asExpression();
            v.setType(t);
            assert (v.getType() != null); // should be redundant
            Expression init = null;
            if (vi.getInitializer() != null) {
                visit(vi.getInitializer());
                init = lastChildResult.asExpression();
            } else {
                // TODO: Pick a proper default initializer based on type...
                init = mkIntLiteral(0);
                init.setType(t);
            }
            //assert(false); // Apparently the next line makes everything non-captured.  It shouldn't.  Am I mis-managing currentScope?  Am I mismanaging the current scope / function stack in the env layout pass?
            assert (currentFunction == null || currentScope == currentFunction.getScope());
            if (debug) {
                if (currentFunction != null) {
                    System.err.println("Declaring variable ["+v.getIdentifier()+"] in function ["+currentFunction.getName()+"]");
                } else {
                    System.err.println("Declaring variable ["+v.getIdentifier()+"] in the global scope");
                }
            }
            boolean asgn_only = false;
            asgn_only = currentScope.declareVariable(v, t);
            if (asgn_only) {
                VarAssignment asgn = mkVarAssignment(v, "=", init);
                asgn.setType(t);
                cs.addStatement(mkExpressionStatement(asgn));
            } else {
                //currentBlockScope.prefixStatement(mkVarDecl(v, t, mkIntLiteral(0)));
                cs.addStatement(mkVarDecl(v, t, init));
            }
        }
        lastChildResult = cs;
    }

    @Override
    protected void visitExprStmt(org.mozilla.javascript.ast.ExpressionStatement node) {
        visit(node.getExpression());
        // Unfortunately, C and JS disagree on what is an expression
        if (lastChildResult.isExpression()) {
            lastChildResult = mkExpressionStatement(lastChildResult.asExpression());
        }
    }

    // This is essentially the reverse of RhinoParser.mapOperator
    public static String decodeRhinoOperator(int code) {
        switch (code) {
	    // Assignment operators
	    case Token.ASSIGN: return "=";
	    case Token.ASSIGN_ADD: return "+=";
	    case Token.ASSIGN_SUB: return "-=";
	    case Token.ASSIGN_MUL: return "*=";
	    case Token.ASSIGN_DIV: return "/=";
	    case Token.ASSIGN_MOD: return "%=";
	    case Token.ASSIGN_LSH: return "<<=";
	    case Token.ASSIGN_RSH: return ">>=";
	    case Token.ASSIGN_URSH: return ">>>=";
	    case Token.ASSIGN_BITOR: return "|=";
	    case Token.ASSIGN_BITXOR: return "^=";
	    case Token.ASSIGN_BITAND: return "&=";
	    // Binary operators
	    case Token.EQ: return "==";
	    case Token.NE: return "!=";
	    case Token.SHEQ: return "===";
	    case Token.SHNE: return "!==";
	    case Token.LT: return "<";
	    case Token.LE: return "<=";
	    case Token.GT: return ">";
	    case Token.GE: return ">=";
	    case Token.LSH: return "<<";
	    case Token.RSH: return ">>";
	    case Token.URSH: return ">>>";
	    case Token.ADD: return "+";
	    case Token.SUB: return "-";
	    case Token.MUL: return "*";
	    case Token.DIV: return "/";
	    case Token.MOD: return "%";
	    case Token.BITOR: return "|";
	    case Token.BITXOR: return "^";
	    case Token.BITAND: return "&";
	    case Token.IN: return "in";
	    case Token.INSTANCEOF: return "instanceof";
	    // Update operators
	    case Token.INC: return "++";
	    case Token.DEC: return "--";
	    // Logical operators
 	    case Token.OR: return "||";
	    case Token.AND: return "&&";
	    // Unary operators (some covered earlier...)
	    case Token.NEG: return "-";
	    case Token.NOT: return "!";
	    case Token.BITNOT: return "~";
	    case Token.TYPEOF: return "typeof";
	    case Token.VOID: return "void";
	    case Token.DELPROP: return "delete"; // TODO: not sure
	    default:
		throw new UnsupportedOperationException("Unknown operator to decode: "+code);
  	}
    }

    @Override
    protected void visitInfix(InfixExpression node) {
        visit(node.getLeft());
        Expression left = lastChildResult.asExpression();
        visit(node.getRight());
        Expression right = lastChildResult.asExpression();
        String op = decodeRhinoOperator(node.getOperator());
        lastChildResult = mkBinaryOp(left, op, right);
        assert (!"+=".equals(op));
        if (getType(node) == null) {
            System.err.println("Missing type on: "+node.toSource(0));
        }
        assert(getType(node) != null);
        lastChildResult.setType(getType(node));
    }

    @Override
    protected void visitAssignment(org.mozilla.javascript.ast.Assignment node) {
        Type t = getType(node);
        assert(node.getLeft() instanceof Name
               || node.getLeft() instanceof ElementGet
               || node.getLeft() instanceof PropertyGet);
        visit(node.getLeft());
        Expression target = lastChildResult.asExpression();
        visit(node.getRight());
        Expression val = lastChildResult.asExpression();
        if (target instanceof Var) {
            Var v = (Var)target;
            lastChildResult = mkVarAssignment(v, decodeRhinoOperator(node.getOperator()), val);
        } else if (target instanceof FieldRead) {
            FieldRead fr = (FieldRead)target;
            lastChildResult = mkFieldAssignment(fr.getObject(), fr.getField(), decodeRhinoOperator(node.getOperator()), val);
        } else {
            assert (target instanceof ArrayIndex);
            // TODO: remove the distinction between these different types of assignments
            // TODO: Need a unified way to determine whether an lval is boxed or not
            lastChildResult = mkVarAssignment(target, decodeRhinoOperator(node.getOperator()), val);
        }
        if (t == null) {
            System.err.println("Reconstructing type of assignment: "+node.toSource(0));
            t = target.getType();
            assert (t != null);
        }
        lastChildResult.setType(t);
    }

    private FunctionNode currentRhinoFunction = null;
    protected com.samsung.sjs.backend.asts.ir.Function buildFunction(FunctionNode node) {
        CodeType ftype = (CodeType)types.get(node);
        assert (ftype != null);
        Type retty = ftype.returnType();
        int i = 0;
        com.samsung.sjs.backend.asts.ir.Function oldcurr = currentFunction;
        com.samsung.sjs.backend.asts.ir.Function f = mkFunction(currentScope, node.getName(), retty);
        FunctionNode oldrhino = currentRhinoFunction;
        currentRhinoFunction = node;
        currentFunction = f;
        com.samsung.sjs.backend.asts.ir.Block oldcurrentblockscope = currentBlockScope;
        currentBlockScope = f.getBody();
        com.samsung.sjs.backend.asts.ir.Block old_closure_hoist = current_closure_hoist;
        current_closure_hoist = new com.samsung.sjs.backend.asts.ir.Block();
        currentBlockScope.addStatement(current_closure_hoist);
        f.setType(ftype);
        if (ftype.isConstructor()) {
            f.markConstructor();
        }
        currentScope = f.getScope();
        for (AstNode arg : node.getParams()) {
            Name n = (Name)arg;
            Type paramTy = ftype.paramTypes().get(i);
            f.addParameter(mkVar(n.getIdentifier()), paramTy);
            i++;
        }
        org.mozilla.javascript.ast.Block body = (org.mozilla.javascript.ast.Block)node.getBody();
        for (Node n : body) {
            visit((AstNode)n);
            if (lastChildResult.isExpression()) {
                lastChildResult = mkExpressionStatement(lastChildResult.asExpression());
            }
            f.addBodyStatement(lastChildResult.asStatement());
        }
        currentScope = currentScope.getParentScope();
        currentFunction = oldcurr;
        currentRhinoFunction = oldrhino;
        current_closure_hoist = old_closure_hoist;
        assert (oldcurr == null || currentFunction.getScope() == currentScope);
        currentBlockScope = oldcurrentblockscope;
        return f;
    }

    @Override
    protected void visitFunction(FunctionNode node) {
        /* This method has to distinguish between two cases:
         * 1. Function declaration introducing a local binding
         * 2. Function expression (w/ or w/o name)
         * Either case can be used to define an object method.
         *
         * The FunctionNode carries this in its functionType field
         */
        lastChildResult = null;
        com.samsung.sjs.backend.asts.ir.Function f = buildFunction(node);
        switch(node.getFunctionType()) {
            case FunctionNode.FUNCTION_STATEMENT:
                // Global scope function declaration
                // FALL THROUGH
            case FunctionNode.FUNCTION_EXPRESSION_STATEMENT:
                // local scope
                Var v = mkVar(node.getName());
                v.setType(getType(node));
                assert(v.getType() != null);
                currentScope.declareVariable(v, getType(node));
                com.samsung.sjs.backend.asts.ir.Expression init = mkIntLiteral(0);
                init.setType(v.getType());
                currentBlockScope.prefixStatement(mkVarDecl(v, getType(node), init));
                VarAssignment asgn = mkVarAssignment(v, "=", f);
                asgn.setType(getType(node));
                current_closure_hoist.addStatement(mkExpressionStatement(asgn));
                lastChildResult = mkExpressionStatement(null);
                break;
            case FunctionNode.FUNCTION_EXPRESSION:
            default:
                lastChildResult = f;
                //throw new IllegalArgumentException("Invalid function node type on Rhino AST");
        }
        if (getType(node) != null) {
            lastChildResult.setType(getType(node));
        }
        assert (lastChildResult != null);
    }

    protected void visitObjectLiteral(ObjectLiteral node) {
        List<ObjectProperty> elems = node.getElements();
        Type t = getType(node);
        assert (t != null);
        if (t.isObject()) {
            AllocObjectLiteral obj = mkAllocObjectLiteral();
            ObjectType ty = (ObjectType)getType(node);
            Map<String,Type> map = new HashMap<String,Type>();
            // TODO: Refactor ObjectType inferface to make this less of a mess
            for (Property p : ty.properties()) {
                map.put(p.getName(), p.getType());
            }
            for (ObjectProperty p : elems) {
                visit(p.getRight());
                Name n = (Name)p.getLeft();
                obj.addSlot(n.getIdentifier(),
                            lastChildResult.asExpression(),
                            map.get(n.getIdentifier()));
            }
            obj.setVTable(null);
            obj.setType(ty);
            lastChildResult = obj;
        } else {
            assert(t.isMap());
            MapType ty = (MapType)getType(node);
            AllocMapLiteral m = mkAllocMapLiteral(ty.elemType());
            for (ObjectProperty p : elems) {
                visit(p.getRight());
                StringLiteral n = (StringLiteral)p.getLeft();
                m.addEntry(n.getValue(), lastChildResult.asExpression());
            }
            m.setType(ty);
            lastChildResult = m;
        }
    }

    public static boolean looksLikeMethodCall(org.mozilla.javascript.ast.FunctionCall node,
                                              Map<AstNode,Type> types,
                                              com.samsung.sjs.backend.asts.ir.Scope currentScope,
                                              boolean shouldPatchTypes) {
        boolean isMethodCall = false;

        if (node.getTarget() instanceof PropertyGet) {
            PropertyGet pg = (PropertyGet)node.getTarget();
            Type t = types.get(pg.getTarget());
            if (t == null && currentScope != null) {
                System.err.println("ACK! property access w/o type info: "+node.getTarget().toSource(0));
                if (pg.getTarget().getType() == Token.NAME) {
                    System.err.println("Looking up type for name...");
                    t = currentScope.lookupType(mkVar(pg.getTarget().toSource(0)));
                    System.err.println("||"+(pg.getTarget().toSource(0))+"|| = "+t);
                    if (t == null && pg.getTarget().toSource(0).equals("String")) {
                        t = Types.mkString(); // Technically String does not have type string, but the inlining pass will erase this before it matters
                    }
                }
                assert (t != null);
                if (shouldPatchTypes) {
                    //hack_type(pg.getTarget(), t);
                    types.put(pg.getTarget(), t);
                }
            }
            if (t instanceof ObjectType) {
                ObjectType ot = (ObjectType)t;
                Name field = (Name)pg.getProperty();
                //Type field_type = ot.findMemberType(field.getIdentifier());
                Type field_type = ot.getTypeForProperty(field.getIdentifier());
                assert (field_type != null);
                if (field_type instanceof AttachedMethodType || field_type instanceof UnattachedMethodType) {
                    isMethodCall = true;
                }
            } else {
                // In this case, we're trying to dispatch a member access on a primitive, which
                // means something like (3.4).toFixed(..)
                isMethodCall = true;
            }
        }
        return isMethodCall;
    }

    protected void visitFunctionCall(org.mozilla.javascript.ast.FunctionCall node) {
        /* This method must distinguish function calls from method calls
         * This node represents a method call if and only if:
         * 1. The target is a PropertyGet, and
         * 2. The type of that field of the object is a method type,
         *    rather than a function type.
         */
        boolean isMethodCall = looksLikeMethodCall(node, types, currentScope, true);
        Type t = getType(node);
        if (isMethodCall) {
            PropertyGet pg = (PropertyGet)node.getTarget();
            visit(pg);
            FieldRead fr = (FieldRead)lastChildResult;
            assert (fr.getObject().getType() != null);
            MethodCall call = mkMethodCall(fr.getObject(), fr.getField());
            for (AstNode arg : node.getArguments()) {
                visit(arg);
                call.addArgument(lastChildResult.asExpression());
            }
            lastChildResult = call;
        } else {
            visit(node.getTarget());
            Expression f = lastChildResult.asExpression();
            // TODO: When we remove the print, printInt, printString hacks, this branch should
            // become unnecessary
            if (f.getType() == null) {
                f.setType(getType(node.getTarget()));
            }
            assert (f.getType() != null);
            com.samsung.sjs.backend.asts.ir.FunctionCall call = mkFunctionCall(f);
            for (AstNode arg : node.getArguments()) {
                visit(arg);
                call.addArgument(lastChildResult.asExpression());
            }
            lastChildResult = call;
        }
        lastChildResult.setType(t);
    }

    protected void visitReturn(org.mozilla.javascript.ast.ReturnStatement node) {
        if (node.getReturnValue() != null) {
            visit(node.getReturnValue());
            lastChildResult = mkReturn(lastChildResult.asExpression());
        } else {
            lastChildResult = mkReturn();
        }
    }

    protected void visitCondExpr(org.mozilla.javascript.ast.ConditionalExpression node) {
        Expression test, then, not;
        visit(node.getTestExpression());
        test = lastChildResult.asExpression();
        visit(node.getTrueExpression());
        then = lastChildResult.asExpression();
        visit(node.getFalseExpression());
        not = lastChildResult.asExpression();
        lastChildResult = mkCondExpr(test, then, not);
        assert (not.getType() != null);
        lastChildResult.setType(not.getType());
    }

    protected void visitUnary(org.mozilla.javascript.ast.UnaryExpression node) {
        visit(node.getOperand());
        lastChildResult = mkUnaryOp(lastChildResult.asExpression(), decodeRhinoOperator(node.getOperator()), node.isPostfix());
        Type t = getType(node);
        assert (t != null);
        lastChildResult.setType(t);
    }

    protected void visitPropertyGet(PropertyGet node) {
        visit(node.getTarget());
        Expression target = lastChildResult.asExpression();
        if (target.getType() == null) {
            assert (target.isVar());
            System.err.println("Reconstructing type for member access target "+target.toSource(0));
            Type t = currentScope.lookupType(target.asVar());
            assert (t != null);
            target.setType(t);
        }
        String field = node.getProperty().getIdentifier();
        lastChildResult = mkFieldRead(target, field);
        lastChildResult.setType(getType(node));
        if (getType(node) == null) {
            System.err.println("Reconstructing field access type on "+node.toSource(0)+", target type = "+target.getType());
            if (target.getType().isObject()) {
                ObjectType ot = (ObjectType)target.getType();
                lastChildResult.setType(ot.findMemberType(field));
            } else {
                assert (target.getType() instanceof ArrayType);
                assert (field.equals("length"));
                lastChildResult.setType(Types.mkInt());
            }
        }
    }

    protected void visitString(StringLiteral node) {
        lastChildResult = mkStr(node.getValue(false));
        lastChildResult.setType(Types.mkString());
    }

    protected void visitIf(org.mozilla.javascript.ast.IfStatement n) {
        visit(n.getCondition());
        Expression test = lastChildResult.asExpression();
        visit(n.getThenPart());
        Statement thenpart = lastChildResult.asStatement();
        Statement elsepart = null;
        if (n.getElsePart() != null) {
            visit(n.getElsePart());
            elsepart = lastChildResult.asStatement();
        }
        lastChildResult = mkIfThenElse(test, thenpart, elsepart);
    }

    protected void visitBlock(AstNode node) {
        com.samsung.sjs.backend.asts.ir.Block b = mkBlock();
        for (Node n : node) {
            visit((AstNode)n);
            b.addStatement(lastChildResult.asStatement());
        }
        lastChildResult = b;
    }

    protected void visitArrayLiteral(ArrayLiteral node) {
        ArrayType arrty = (ArrayType)getType(node);
        Type cellType = arrty.elemType();
        AllocArrayLiteral lit = mkAllocArrayLiteral(cellType);
        lit.setType(arrty);
        for (AstNode n : node.getElements()) {
            visit(n);
            lit.addElement(lastChildResult.asExpression());
        }
        lastChildResult = lit;
    }

    protected void visitForLoop(org.mozilla.javascript.ast.ForLoop loop) {
        visit(loop.getInitializer());
        // initializer may be a declaration or an expression
        // TODO: Plumb empty expresion and statement through IR and C ASTs, and override
        // visitEmpty()
        IRNode inits = loop.getInitializer().getType() == Token.EMPTY ? null : lastChildResult;
        assert (loop.getCondition() != null);
        visit(loop.getCondition());
        Expression cond = loop.getCondition().getType() == Token.EMPTY ? null : lastChildResult.asExpression();
        visit(loop.getIncrement());
        Expression incr = loop.getIncrement().getType() == Token.EMPTY ? null : lastChildResult.asExpression();
        assert (loop.getBody() != null);
        visit(loop.getBody());
        Statement body = lastChildResult.asStatement();

        com.samsung.sjs.backend.asts.ir.ForLoop l = mkForLoop(inits, cond, incr);
        l.setBody(body);

        lastChildResult = l;
    }

    protected void visitNewExpr(NewExpression node) {
        // This method is actually polymorphic over any PropertyContainer type
        AllocNewObject alloc = null;
        visit(node.getTarget()); // NewExpression <: FunctionCall
        alloc = mkAllocNewObject(lastChildResult.asExpression());
        alloc.setVTable(null);
        for (AstNode arg : node.getArguments()) {
            visit(arg);
            alloc.addArgument(lastChildResult.asExpression());
        }
        alloc.setType(getType(node));
        assert(alloc.getType() != null);
        lastChildResult = alloc;
    }

    protected void visitParenExpr(ParenthesizedExpression node) {
        // TODO: Clean up this fix!
        assert (getType(node.getExpression()) != null);
        types.put(node.getExpression(), getType(node));
        visit(node.getExpression());
        if (lastChildResult.getType() == null) {
            System.err.println("Missing type on expr in parens: "+lastChildResult.toSource(0));
        }
        assert (lastChildResult.getType() != null);
    }

    protected void visitWhileLoop(org.mozilla.javascript.ast.WhileLoop loop) {
        visit(loop.getCondition());
        Expression cond = lastChildResult.asExpression();
        visit(loop.getBody());
        Statement body = lastChildResult.asStatement();
        com.samsung.sjs.backend.asts.ir.WhileLoop l = mkWhileLoop(cond, body);
        lastChildResult = l;
    }

    private static int iter_count = 0;
    protected void visitForInLoop(org.mozilla.javascript.ast.ForInLoop loop) {
        AstNode obj = loop.getIteratedObject();
        AstNode decl = loop.getIterator();
        assert(getType(obj).isMap());
        MapType t = (MapType)(getType(obj));

        visit(decl);
        assert (lastChildResult instanceof CompoundStatement);
        CompoundStatement cdecl = (CompoundStatement)lastChildResult;
        assert (cdecl.nstatements() == 1);
        Var boundvar = null;
        if (cdecl.getStatement(0) instanceof com.samsung.sjs.backend.asts.ir.ExpressionStatement) {
            VarAssignment d = (VarAssignment)((com.samsung.sjs.backend.asts.ir.ExpressionStatement)cdecl.getStatement(0)).getExpression();
            boundvar = (Var)d.getAssignedVar();
        } else {
            assert (cdecl.getStatement(0) instanceof VarDecl);
            boundvar = ((VarDecl)cdecl.getStatement(0)).getVar();
            currentBlockScope.prefixStatement(cdecl.getStatement(0));
        }
        boundvar.setType(t.elemType());

        visit(obj);
        Expression e = lastChildResult.asExpression();
        e.setType(t);

        com.samsung.sjs.backend.asts.ir.ForInLoop l = mkForInLoop(boundvar, e, t.elemType());

        visit(loop.getBody());
        l.setBody(lastChildResult.asStatement());

        lastChildResult = l;


        //// Synthesize an iterator variable suggestively associated with v, and desugar into:
        //// t v = null/undef;
        //// Iterator v_iter = mkIter(e);
        //// while (v_iter.hasNext()) {
        ////      v = v_iter.next();
        ////      <body>
        //// }
        //CompoundStatement cs = mkCompoundStatement();

        //// Declare body-bound variable
        //visit(decl);
        //cs.addStatement(lastChildResult.asStatement());
        //assert (lastChildResult instanceof CompoundStatement);
        //CompoundStatement cdecl = (CompoundStatement)lastChildResult;
        //assert (cdecl.nstatements() == 1);
        //VarDecl d = (VarDecl)cdecl.getStatement(0);
        //Var boundvar = d.getVar();
        //boundvar.setType(t.elemType());

        //// Declare variable for the iterator
        //Var itv = mkVar("_iter"+(iter_count++));
        //Type iterty = Types.mkMapIteratorType(t.elemType());
        //itv.setType(iterty);
        //visit(obj);
        //com.samsung.sjs.backend.asts.ir.FunctionCall get_iterator_call =
        //    mkFunctionCall(mkIntrinsicName("__get_map_iterator"), lastChildResult.asExpression());
        //get_iterator_call.setType(iterty);
        //cs.addStatement(mkVarDecl(itv,iterty,get_iterator_call));
        //if (currentFunction != null) {
        //    currentScope.declareVariable(itv, iterty);
        //}

        //com.samsung.sjs.backend.asts.ir.Block body = mkBlock();
        //com.samsung.sjs.backend.asts.ir.FunctionCall get_next_call =
        //    mkFunctionCall(mkIntrinsicName("__map_iterator_get_next"), itv);
        //get_next_call.setType(t.elemType());
        //VarAssignment asgn = mkVarAssignment(boundvar, "=", get_next_call);
        //asgn.setType(t.elemType());
        //body.addStatement(mkExpressionStatement(asgn));
        //visit(loop.getBody());
        //body.addStatement(lastChildResult.asStatement());
        //com.samsung.sjs.backend.asts.ir.FunctionCall has_next_call =
        //    mkFunctionCall(mkIntrinsicName("__map_iterator_has_next"), itv);
        //has_next_call.setType(Types.mkBool());
        //com.samsung.sjs.backend.asts.ir.WhileLoop wl = mkWhileLoop(has_next_call, body);

        //cs.addStatement(wl);

        //lastChildResult = cs;
    }

    protected void visitBreak(org.mozilla.javascript.ast.BreakStatement node) {
        lastChildResult = mkBreak();
    }

    protected void visitDoLoop(org.mozilla.javascript.ast.DoLoop loop) {
        visit(loop.getCondition());
        Expression cond = lastChildResult.asExpression();
        visit(loop.getBody());
        Statement body = lastChildResult.asStatement();
        lastChildResult = mkDoLoop(cond, body);
    }

    protected void visitSwitch(SwitchStatement node) {
        visit(node.getExpression());
        Switch s = mkSwitch(lastChildResult.asExpression());
        for (SwitchCase sc : node.getCases()) {
            Case c = null;
            if (sc.getExpression() != null) {
                visit(sc.getExpression());
                c = mkCase(lastChildResult.asExpression());
            } else {
                c = mkCase(null);
            }
            if (sc.getStatements() != null) {
                // may be e.g., empty default case, or fallthrough
                for (AstNode n : sc.getStatements()) {
                    visit(n);
                    c.addStatement(lastChildResult.asStatement());
                }
            }
            s.addCase(c);
        }
        lastChildResult = s;
    }

    protected void visitArrayComp(ArrayComprehension node) {
        throw new UnsupportedOperationException();
    }
    protected void visitContinue(org.mozilla.javascript.ast.ContinueStatement node) {
        lastChildResult = new Continue();
    }
    protected void visitDefaultXmlNamepace(org.mozilla.javascript.ast.UnaryExpression node) {
        throw new UnsupportedOperationException();
    }
    protected void visitGenExpr(GeneratorExpression node) {
        throw new UnsupportedOperationException();
    }
    protected void visitLabeledStatement(LabeledStatement ls) {
        throw new UnsupportedOperationException();
    }
    protected void visitLetNode(LetNode node) {
        throw new UnsupportedOperationException();
    }
    protected void visitRegExp(RegExpLiteral node) {
        throw new UnsupportedOperationException();
    }
    protected void visitScript(ScriptNode node) {
        throw new UnsupportedOperationException();
    }
    protected void visitThrow(ThrowStatement node) {
        throw new UnsupportedOperationException();
    }
    protected void visitTry(TryStatement node) {
        throw new UnsupportedOperationException();
    }
    //protected void visitVariables(org.mozilla.javascript.ast.VariableDeclaration node) {
    //    throw new UnsupportedOperationException();
    //}
    protected void visitXmlLiteral(XmlLiteral node) {
        throw new UnsupportedOperationException();
    }
    protected void visitXmlMemberGet(XmlMemberGet node) {
        throw new UnsupportedOperationException();
    }
    protected void visitXmlRef(XmlRef node) {
        throw new UnsupportedOperationException();
    }
    protected void visitYield(Yield node) {
        throw new UnsupportedOperationException();
    }


}
