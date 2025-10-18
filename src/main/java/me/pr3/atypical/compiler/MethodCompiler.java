package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.statement.StatementCompiler;
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

    public MethodCompiler(StructureCompiler parent) {
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

        // Add local vars for parameters (and `this` for non-static methods)
        org.objectweb.asm.Type[] argTypes = org.objectweb.asm.Type.getArgumentTypes(methodNode.desc);
        if ((methodNode.access & Opcodes.ACC_STATIC) == 0 && !structureCompiler.isClassNameImplClass(className)) {
            // instance method: slot 0 is `this`
            addLocalVar("L" + node.name + ";", "this");
        }
        for (int i = 0; i < argTypes.length; i++) {
            org.objectweb.asm.Type t = argTypes[i];
            String desc = t.getDescriptor();
            addLocalVar(desc, "param" + i);
            // wide types (long/double) are accounted for inside addLocalVar
        }

        methodNode.instructions.add(startLabel);
        StatementCompiler statementCompiler = new StatementCompiler(structureCompiler, this);
        for (StatementContext statementContext : value.statement()) {
            methodNode.instructions.add(statementCompiler.compileStatement(statementContext));
        }
        methodNode.instructions.add(new InsnNode(Opcodes.RETURN));
        methodNode.instructions.add(endLabel);
    }


    public int addLocalVar(String type, String name){
        int index = localVars.size();
        localVars.add(type);
        // If type occupies two slots (long/double), add a placeholder for the second slot
        org.objectweb.asm.Type t = org.objectweb.asm.Type.getType(type);
        if (t.getSize() == 2) {
            localVars.add("TOP"); // placeholder for second slot
        }
        localVarNameMapping.put(name, index);
        methodNode.localVariables.add(new LocalVariableNode(name, type, null, startLabel, endLabel, index));
        return index;
    }

    public String getLocalVar(int i) {
        return localVars.get(i);
    }

    public String getLocalVarTypeByName(String name) {
        return localVars.get(localVarNameMapping.get(name));
    }

    public boolean containsLocalVarWithName(String name) {
        return localVarNameMapping.containsKey(name);
    }

    public int getLocalVarIndexByName(String name) {
        return localVarNameMapping.get(name);
    }

    public String fullyQualifyType(String type) {
        return structureCompiler.imports.get(this.fileName).getOrDefault(type, type);
    }

}
