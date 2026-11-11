package com.freezephoenix.compiler;

import com.freezephoenix.compiler.codegen.MethodBindScanner;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

public class OptimizationTaskListener implements TaskListener {
    final Context context;
    public OptimizationTaskListener(Context context) {
        this.context = context;
    }

    @Override
    public void finished(TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.ANALYZE) {
            JCTree.JCCompilationUnit tree = (JCTree.JCCompilationUnit) e.getCompilationUnit();
            new MethodBindScanner(context, tree).translate(tree);
        }
    }
}
