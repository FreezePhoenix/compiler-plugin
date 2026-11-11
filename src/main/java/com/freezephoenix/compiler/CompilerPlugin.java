package com.freezephoenix.compiler;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;

public class CompilerPlugin implements Plugin {


    @Override
    public String getName() {
        return "CompilerPlugin";
    }

    @Override
    public void init(JavacTask task, String... args) {
        if(task instanceof BasicJavacTask basicJavacTask) {
            basicJavacTask.addTaskListener(new OptimizationTaskListener(basicJavacTask.getContext()));
        }
    }
}