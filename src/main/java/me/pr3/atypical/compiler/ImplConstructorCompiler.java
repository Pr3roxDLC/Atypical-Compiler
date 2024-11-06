package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author tim
 */
public class ImplConstructorCompiler {

    private final StructureCompiler structureCompiler;

    public ImplConstructorCompiler(StructureCompiler structureCompiler) {
        this.structureCompiler = structureCompiler;
    }

    public void compileImplConstructor(AtypicalParser.ImplDeclarationContext context, String className, String fileName){
        ClassNode implClassNode = structureCompiler.generatedClassNodes.get(className);
        String desc = "(" + TypeUtil.toDesc(context.struct.getText(), structureCompiler.imports.get(fileName)) + ")V";
        MethodNode constructor = ClassNodeUtil.getMethodNodeByNameAndDescriptor(implClassNode, "<init>", desc);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        constructor.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, className, "this_", TypeUtil.toDesc(context.struct.getText(), structureCompiler.imports.get(fileName))));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
    }
}
