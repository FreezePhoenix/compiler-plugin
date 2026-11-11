package com.freezephoenix.compiler.codegen;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.PoolConstant;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import java.util.HashMap;
import java.util.Map;

public final class MethodBindScanner extends TreeTranslator {

    private final JCTree.JCCompilationUnit compilationUnit;
    private final Resolve rs;
    private final JavacTrees trees;
    private final PluginNames names;
    private final PluginSymtab syms;
    private final TreeMaker make;
    private final Types types;

    private MethodBindScanner.KlassInfo kInfo;

    public record KlassInfo(Map<Object, Symbol.MethodSymbol> mss) {
        public KlassInfo() {
            this(new HashMap<>());
        }
    }

    public MethodBindScanner(Context context, JCTree.JCCompilationUnit compilationUnit) {
        this.syms = PluginSymtab.instance(context);
        this.names = PluginNames.instance(context);
        this.make = TreeMaker.instance(context);
        this.types = Types.instance(context);
        this.rs = Resolve.instance(context);
        this.trees = JavacTrees.instance(context);
        this.compilationUnit = compilationUnit;
    }

    private Env<AttrContext> env(JCTree tree) {
        return trees.getScope(trees.getPath(compilationUnit, tree)).getEnv();
    }

    private boolean isOverrides(Symbol a, Symbol.MethodSymbol b) {
        return a.name.equals(b.name) && a.overrides(b, b.enclClass(), types, true);
    }

    private Symbol.ClassSymbol getClassLiteralNullable(JCTree.JCExpression expression) {
        if (expression instanceof JCTree.JCFieldAccess select && select.name == names.base._class) {
            if (select.selected.type.tsym instanceof Symbol.ClassSymbol classSymbol) {
                return classSymbol;
            }
        }
        return null;
    }

    @Override public void visitVarDef(JCTree.JCVariableDecl tree) {
        super.visitVarDef(tree);
        if(tree.init instanceof JCTree.JCIdent ident) {
            if(getMethodTypeConstantNullable(ident) instanceof Type.MethodType methodType) {
                tree.sym.setData(methodType);
            }
        }
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl tree) {
        KlassInfo prevKlassInfo = kInfo;
        try {
            kInfo = new KlassInfo();
            super.visitClassDef(tree);
        } finally {
            kInfo = prevKlassInfo;
        }
    }

    private Symbol.MethodHandleSymbol getMethodHandleConstantNullable(JCTree.JCExpression expression) {
        if (expression instanceof JCTree.JCIdent ident
                && ident.sym instanceof Symbol.DynamicVarSymbol dvs && types.isSameType(dvs.type, syms.base.methodHandleType) && dvs.staticArgs[0] instanceof Symbol.MethodHandleSymbol methodHandleSymbol) {
            return methodHandleSymbol;
        }
        return null;
    }


    private boolean isMethodHandlesLookup(JCTree.JCExpression expression) {
        return expression instanceof JCMethodInvocation invocation && isOverrides(TreeInfo.symbol(invocation.meth), syms.lookup);
    }

    private boolean isDynalinkHandlesLookup(JCTree.JCExpression expression) {
        return expression instanceof JCTree.JCNewClass newClass && newClass.constructor.isMemberOf(syms.dynalinkLookup, types)
                && newClass.args.size() == 1 && isMethodHandlesLookup(newClass.args.getFirst());
    }

    private Name getNameConstantNullable(JCTree.JCExpression expression) {
        return expression instanceof JCTree.JCLiteral literal && types.isSameType(literal.type, syms.base.stringType) ? names.base.fromString((String) literal.value) : null;
    }

    private Type.MethodType getMethodTypeConstantNullable(JCTree.JCExpression expression) {
        return expression instanceof JCTree.JCIdent ident && ident.sym instanceof Symbol.DynamicVarSymbol dvs && dvs.bsm.equals(syms.explicitCast) && types.isSameType(dvs.type, syms.base.methodTypeType) && dvs.staticArgs[0] instanceof Type.MethodType methodType ? methodType : null;
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        super.visitApply(tree);
        if (result instanceof JCTree.JCMethodInvocation invocation) {
            if (invocation.meth instanceof JCTree.JCFieldAccess access) {
                if (isOverrides(access.sym, syms.bindTo)) {
                    if (getMethodHandleConstantNullable(access.selected) instanceof Symbol.MethodHandleSymbol mhs) {
                        result = make.App(
                                make.Ident(new Symbol.DynamicMethodSymbol(
                                        names.base.underscore,
                                        syms.base.noSymbol,
                                        syms.bindingCallsite,
                                        access.sym.type,
                                        new PoolConstant.LoadableConstant[]{mhs}
                                )),
                                invocation.args
                        );
                    }
                } else if (isOverrides(access.sym.baseSymbol(), syms.invoke)) {
                    if (getMethodHandleConstantNullable(access.selected) instanceof Symbol.MethodHandleSymbol mhs) {
                        result = make.App(
                                make.Ident(new Symbol.DynamicMethodSymbol(
                                        names.base.underscore,
                                        syms.base.noSymbol,
                                        syms.invokingCallsite,
                                        access.sym.type,
                                        new PoolConstant.LoadableConstant[]{mhs}
                                )),
                                invocation.args
                        );
                    }
                } else if (
                        (isMethodHandlesLookup(access.selected) && (isOverrides(access.sym, syms.findVirtual) || isOverrides(access.sym, syms.findStatic))) ||
                        (isDynalinkHandlesLookup(access.selected) && (isOverrides(access.sym, syms.dynaFindVirtual) || isOverrides(access.sym, syms.dynaFindStatic)))
                ) {

                    if (
                            getClassLiteralNullable(tree.args.get(0)) instanceof Symbol.ClassSymbol klass &&
                            getNameConstantNullable(tree.args.get(1)) instanceof Name name &&
                            getMethodTypeConstantNullable(tree.args.get(2)) instanceof Type.MethodType mt
                    ) {
                        result = make.Ident(
                                new Symbol.DynamicVarSymbol(
                                        names.base.underscore,
                                        syms.base.noSymbol,
                                        syms.explicitCast,
                                        syms.base.methodHandleType,
                                        new PoolConstant.LoadableConstant[]{ rs.resolveInternalMethod(invocation, env(tree), klass.type, name, mt.argtypes, List.nil()).asHandle() }
                                )
                        );
                    }
                } else if (
                        isOverrides(access.sym, syms.methodType1) ||
                        isOverrides(access.sym, syms.methodType2) ||
                        isOverrides(access.sym, syms.methodType3)
                ) {
                    ListBuffer<Type> passed = new ListBuffer<>();
                    for (JCTree.JCExpression arg : invocation.args) {
                        Symbol.ClassSymbol argumentSymbol = getClassLiteralNullable(arg);
                        if (argumentSymbol == null) {
                            super.visitApply(invocation);
                            return;
                        }
                        passed.append(argumentSymbol.type);
                    }

                    List<Type> types = passed.toList();

                    result = make.Ident(
                            new Symbol.DynamicVarSymbol(
                                    names.base.underscore,
                                    syms.base.noSymbol,
                                    syms.explicitCast,
                                    syms.base.methodTypeType,
                                    new PoolConstant.LoadableConstant[]{ new Type.MethodType(types.tail, types.head, List.nil(), syms.base.methodClass) }
                            )
                    );
                }
            }
        }
    }
}
