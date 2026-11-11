package com.freezephoenix.compiler;

import jdk.dynalink.linker.support.Lookup;

import java.lang.invoke.*;

@SuppressWarnings("unused")
public enum PluginBootstraps {;
    public static CallSite bindingCallsite(MethodHandles.Lookup unusedLookup, String unusedName, MethodType type, MethodHandle handle) {
        final MethodHandle bindTo = new Lookup(MethodHandles.lookup()).findVirtual(MethodHandle.class, "bindTo", MethodType.methodType(MethodHandle.class, Object.class));
        return new ConstantCallSite(bindTo.bindTo(handle));
    }

    public static CallSite invokingCallsite(MethodHandles.Lookup unusedLookup, String unusedName, MethodType type, MethodHandle handle) {
        return new ConstantCallSite(handle.asType(type));
    }
}
