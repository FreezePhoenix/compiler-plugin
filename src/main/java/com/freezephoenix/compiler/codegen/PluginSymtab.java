package com.freezephoenix.compiler.codegen;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

public class PluginSymtab {

    public static final Context.Key<PluginSymtab> symtabKey = new Context.Key<>();

    public static PluginSymtab instance(Context context) {
        PluginSymtab instance = context.get(symtabKey);
        if (instance == null) {
            context.put(symtabKey, instance = new PluginSymtab(context));
        }
        return instance;
    }

    public final Symtab base;

    public final Symbol.MethodHandleSymbol findVirtual;
    public final Symbol.MethodHandleSymbol findStatic;
    public final Symbol.MethodHandleSymbol dynaFindVirtual;
    public final Symbol.MethodHandleSymbol dynaFindStatic;

    public final Symbol.MethodHandleSymbol bindTo;
    public final Symbol.MethodHandleSymbol invoke;

    public final Symbol.MethodHandleSymbol lookup;
    public final Symbol.MethodHandleSymbol explicitCast;

    public final Symbol.MethodHandleSymbol baseBindTo;
    public final Symbol.MethodHandleSymbol baseInvoke;

    public final Symbol.MethodHandleSymbol methodType1;
    public final Symbol.MethodHandleSymbol methodType2;
    public final Symbol.MethodHandleSymbol methodType3;

    public final Symbol.ClassSymbol dynalinkLookup;

    public PluginSymtab(Context context) {
        final PluginNames names = PluginNames.instance(context);
        final Types types = Types.instance(context);

        base = Symtab.instance(context);

        final Type unboundClassType = new Type.ClassType(
                base.classType.getEnclosingType(),
                List.of(new Type.WildcardType(base.objectType, BoundKind.UNBOUND, base.boundClass)),
                base.classType.tsym
        );

        final Symbol.ModuleSymbol jdk_dynalink = base.enterModule(names.base.fromString("jdk.dynalink"));

        final Symbol.ClassSymbol pluginBootstraps = base.enterClass(base.unnamedModule, names.com_freezephoenix_compiler_PluginBootstraps);

        dynalinkLookup = base.enterClass(jdk_dynalink, names.jdk_dynalink_linker_support_Lookup);


        final Symbol.ClassSymbol callSite = base.enterClass(base.java_base, names.java_lang_invoke_CallSite);

        final Symbol.ClassSymbol noSuchMethodException = base.enterClass(base.java_base, names.java_lang_NoSuchMethodException);
        final Symbol.ClassSymbol illegalAccessException = base.enterClass(base.java_base, names.java_lang_IllegalAccessException);
        final Symbol.ClassSymbol classCastException = base.enterClass(base.java_base, names.java_lang_ClassCastException);

        explicitCast = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.explicitCast,
                new Type.MethodType(
                        List.of(base.methodHandleLookupType, base.stringType, unboundClassType, base.objectType),
                        base.objectType,
                        List.of(classCastException.type),
                        base.methodClass
                ),
                base.constantBootstrapsType.tsym
        ).asHandle();

        this.bindTo = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.bindTo,
                new Type.MethodType(
                        List.of(base.methodHandleLookupType, base.stringType, base.methodTypeType, base.methodHandleType, base.methodHandleType),
                        callSite.type,
                        List.nil(),
                        base.methodClass
                ),
                pluginBootstraps
        ).asHandle();

        this.invoke = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.base.invoke,
                new Type.MethodType(
                        List.of(base.methodHandleLookupType, base.stringType, base.methodTypeType, base.methodHandleType),
                        callSite.type,
                        List.nil(),
                        base.methodClass
                ),
                pluginBootstraps
        ).asHandle();

        final Type.MethodType throwingLookupType = new Type.MethodType(
                List.of(unboundClassType, base.stringType, base.methodTypeType),
                base.methodHandleType,
                List.of(noSuchMethodException.type, illegalAccessException.type),
                base.methodClass
        );

        this.findVirtual = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                names.findVirtual,
                throwingLookupType,
                base.methodHandleLookupType.tsym
        ).asHandle();

        this.findStatic = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                names.findStatic,
                throwingLookupType,
                base.methodHandleLookupType.tsym
        ).asHandle();

        final Type nonThrowingLookupType = new Type.MethodType(
                List.of(unboundClassType, base.stringType, base.methodTypeType),
                base.methodHandleType,
                List.nil(),
                base.methodClass
        );

        this.dynaFindVirtual = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                names.findVirtual,
                nonThrowingLookupType,
                dynalinkLookup
        ).asHandle();

        this.dynaFindStatic = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                names.findStatic,
                nonThrowingLookupType,
                dynalinkLookup
        ).asHandle();

        this.methodType1 = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.methodType,
                new Type.MethodType(
                        List.of(unboundClassType),
                        base.methodTypeType,
                        List.nil(),
                        base.methodClass
                ),
                base.methodTypeType.tsym
        ).asHandle();

        this.methodType2 = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.methodType,
                new Type.MethodType(
                        List.of(unboundClassType, unboundClassType),
                        base.methodTypeType,
                        List.nil(),
                        base.methodClass
                ),
                base.methodTypeType.tsym
        ).asHandle();

        this.methodType3 = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC | Flags.VARARGS,
                names.methodType,
                new Type.MethodType(
                        List.of(unboundClassType, unboundClassType, types.makeArrayType(unboundClassType).makeVarargs()),
                        base.methodTypeType,
                        List.nil(),
                        base.methodClass
                ),
                base.methodTypeType.tsym
        ).asHandle();

        this.lookup = new Symbol.MethodSymbol(
                Flags.STATIC | Flags.PUBLIC,
                names.lookup,
                new Type.MethodType(
                        List.nil(),
                        base.methodHandleLookupType,
                        List.nil(),
                        base.methodClass
                ),
                base.methodHandlesType.tsym
        ).asHandle();

        this.baseBindTo = new Symbol.MethodSymbol(
                Flags.PUBLIC,
                names.bindTo,
                new Type.MethodType(
                        List.of(base.objectType),
                        base.methodHandleType,
                        List.nil(),
                        base.methodClass
                ),
                base.methodHandleType.tsym
        ).asHandle();

        this.baseInvoke = new Symbol.MethodSymbol(
                Flags.PUBLIC | Flags.FINAL | Flags.NATIVE | Flags.VARARGS | Flags.SIGNATURE_POLYMORPHIC,
                names.base.invoke,
                new Type.MethodType(
                        List.of(types.makeArrayType(base.objectType).makeVarargs()),
                        base.objectType,
                        List.of(base.throwableType),
                        base.methodClass
                ),
                base.methodHandleType.tsym
        ).asHandle();
    }
}
