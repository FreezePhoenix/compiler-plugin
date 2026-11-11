package com.freezephoenix.compiler.codegen;

import com.freezephoenix.compiler.PluginBootstraps;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.lang.invoke.CallSite;

public final class PluginNames {
    public static final Context.Key<PluginNames> namesKey = new Context.Key<>();

    public static PluginNames instance(Context context) {
        PluginNames instance = context.get(namesKey);
        if (instance == null) {
            context.put(namesKey, instance = new PluginNames(context));
        }
        return instance;
    }

    public final Names base;

    public final Name com_freezephoenix_compiler_PluginBootstraps;
    public final Name bindTo;
    public final Name explicitCast;
    public final Name methodType;
    public final Name findVirtual;
    public final Name findStatic;

    public final Name java_lang_NoSuchMethodException;
    public final Name java_lang_IllegalAccessException;
    public final Name java_lang_ClassCastException;
    public final Name java_lang_invoke_CallSite;

    public final Name jdk_dynalink_linker_support_Lookup;

    public final Name lookup;

    public PluginNames(Context context) {
        base = Names.instance(context);
        bindTo = base.fromString("bindTo");
        explicitCast = base.fromString("explicitCast");
        methodType = base.fromString("methodType");
        lookup = base.fromString("lookup");
        findVirtual = base.fromString("findVirtual");
        findStatic = base.fromString("findStatic");

        com_freezephoenix_compiler_PluginBootstraps = base.fromString(PluginBootstraps.class.getName());

        java_lang_NoSuchMethodException = base.fromString(NoSuchMethodException.class.getName());
        java_lang_IllegalAccessException = base.fromString(IllegalAccessException.class.getName());
        java_lang_ClassCastException = base.fromString(ClassCastException.class.getName());
        java_lang_invoke_CallSite = base.fromString(CallSite.class.getName());

        jdk_dynalink_linker_support_Lookup = base.fromString("jdk.dynalink.linker.support.Lookup");
    }
}
