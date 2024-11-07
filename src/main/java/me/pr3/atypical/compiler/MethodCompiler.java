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
    public StructureCompiler structureCompiler;

    public List<String> localVars = new ArrayList<>();//Holds the type of each local var (we do not reuse indices)
    public Map<String, Integer> localVarNameMapping = new HashMap<>();

    public MethodNode methodNode = null;
    public LabelNode startLabel = new LabelNode();
    public LabelNode endLabel = new LabelNode();
    public String fileName = "";
    public String className = "";
    public MethodCompiler(StructureCompiler parent){
        this.structureCompiler = parent;
    }
    public void compileMethod(String fileName, MethodImplementationContext value) {
        compileMethod(fileName, value, fileName.replace(".atp", ""));
    }

    public void compileMethod(String fileName, MethodImplementationContext value, String className) {
        this.fileName = fileName;
        this.className = className;
        ClassNode node = structureCompiler.generatedClassNodes.get(className);
        this.methodNode = ClassNodeUtil.getMethodNodeByNameAndDescriptor(node,
                value.methodSignature().memberName().getText(),
                TypeUtil.extractMethodDescriptor(value.methodSignature(), structureCompiler.imports.get(fileName)));
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

    public String fullyQualifyType(String type){
        return structureCompiler.imports.get(this.fileName).getOrDefault(type, type);
    }

}
