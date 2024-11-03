package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.*;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class MethodCompiler {
    StructureCompiler structureCompiler;

    List<String> localVars = new ArrayList<>();//Holds the type of each local var (we do not reuse indices)
    Map<String, Integer> localVarNameMapping = new HashMap<>();

    MethodNode methodNode = null;
    LabelNode startLabel = new LabelNode();
    LabelNode endLabel = new LabelNode();
    public MethodCompiler(StructureCompiler parent){
        this.structureCompiler = parent;
    }
    public void compileMethod(String key, MethodImplementationContext value) {
        ClassNode node = structureCompiler.generatedClassNodes.get(key.replace(".atp", ""));
        this.methodNode = ClassNodeUtil.getNodeByNameAndDescriptor(node,
                value.methodSignature().memberName().getText(),
                TypeUtil.extractMethodDescriptor(value.methodSignature()));
        methodNode.instructions.add(startLabel);
        StatementCompiler statementCompiler = new StatementCompiler(structureCompiler, this);
        for (StatementContext statementContext : value.statement()) {
          methodNode.instructions.add(statementCompiler.compileStatement(statementContext));
        }
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        methodNode.instructions.add(endLabel);
    }

    public int addLocalVar(String type, String name){
        localVars.add(type);
        int index = localVars.size() - 1;
        localVarNameMapping.put(name, index);
        methodNode.localVariables.add(new LocalVariableNode(name, type, null, startLabel, endLabel, index));
        return index;
    }

    public String getLocalVar(int i){
        return localVars.get(i);
    }

    public String getLocalVarTypeByName(String name){
        return localVars.get(localVarNameMapping.get(name));
    }

    public boolean containsLocalVarWithName(String name){
        return localVarNameMapping.containsKey(name);
    }
    public int getLocalVarIndexByName(String name){
        return localVarNameMapping.get(name);
    }

}
