package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;

/**
 * @author tim
 */
public class StructInitializerCompiler {
    StructureCompiler structureCompiler;

    public StructInitializerCompiler(StructureCompiler structureCompiler) {
        this.structureCompiler = structureCompiler;
    }

    public void compileStructInitializer(List<AtypicalParser.StructMemberDeclarationContext> members, String fileName, String moduleName){
        ClassNode moduleClassNode = structureCompiler.generatedClassNodes.get(moduleName);
        StringBuilder desc = new StringBuilder("(");
        for (AtypicalParser.StructMemberDeclarationContext structMemberDeclarationContext : members) {
            desc.append(TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText(), structureCompiler.imports.get(fileName)));
        }
        desc.append("Ljava/lang/Void;)V");
        MethodNode structInitializer = ClassNodeUtil.getMethodNodeByNameAndDescriptor(moduleClassNode, "<init>", desc.toString());

        structInitializer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        structInitializer.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V"));

        int i = 1;
        for (AtypicalParser.StructMemberDeclarationContext structMemberDeclarationContext : members) {
            structInitializer.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            structInitializer.instructions.add(new VarInsnNode(getLoadInstructionForType(
                    TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText(), structureCompiler.imports.get(fileName))), i));
            structInitializer.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD,
                    structureCompiler.imports.get(fileName).getOrDefault(moduleName, moduleName),
                    structMemberDeclarationContext.memberName().getText(),
                    TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText(),
                            structureCompiler.imports.get(fileName))
            ));
            i++;
        }
        structInitializer.instructions.add(new InsnNode(Opcodes.RETURN));
    }

    public void compileStructInitializer(AtypicalParser.ModuleStructDeclarationContext struct, String fileName, String moduleName) {
            compileStructInitializer(struct.structMemberDeclaration(), fileName, moduleName);
    }

    public void compileStructInitializer(AtypicalParser.StructDeclarationContext struct, String fileName, String moduleName) {
        compileStructInitializer(struct.structMemberDeclaration(), fileName, moduleName);
    }

    public int getLoadInstructionForType(String varType) {
        return switch (varType) {
            case "I", "Z" -> Opcodes.ILOAD;
            default -> Opcodes.ALOAD;
        };
    }

}
