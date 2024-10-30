package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class MethodCompiler {
    StructureCompiler structureCompiler;

    List<String> localVars = new ArrayList<>();//Holds the type of each local var (we do not reuse indices)
    public MethodCompiler(StructureCompiler parent){
        this.structureCompiler = parent;
    }
    public void compileMethod(String key, MethodImplementationContext value) {
        ClassNode node = structureCompiler.generatedClassNodes.get(key.replace(".atp", ""));
        MethodNode methodNode = ClassNodeUtil.getNodeByNameAndDescriptor(node,
                value.methodSignature().memberName().getText(),
                TypeUtil.extractMethodDescriptor(value.methodSignature()));
        StatementCompiler statementCompiler = new StatementCompiler(structureCompiler, this);
        for (StatementContext statementContext : value.statement()) {
          methodNode.instructions.add(statementCompiler.compileStatement(statementContext));
        }
    }

    public int addLocalVar(String var){
        localVars.add(var);
        return localVars.size() - 1;
    }

    public String getLocalVar(int i){
        return localVars.get(i);
    }

}
