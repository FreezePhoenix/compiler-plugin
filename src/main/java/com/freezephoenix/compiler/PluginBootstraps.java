package com.freezephoenix.compiler;

import java.lang.invoke.*;

@SuppressWarnings("unused")
public class PluginBootstraps {
    public static CallSite bindTo(MethodHandles.Lookup unusedLookup, String unusedName, MethodType type, MethodHandle baseBindTo, MethodHandle handle) {
        return new ConstantCallSite(baseBindTo.bindTo(handle).asType(type));
    }
    public static CallSite invoke(MethodHandles.Lookup unusedLookup, String unusedName, MethodType type, MethodHandle handle) {
        return new ConstantCallSite(handle.asType(type));
    }
}
