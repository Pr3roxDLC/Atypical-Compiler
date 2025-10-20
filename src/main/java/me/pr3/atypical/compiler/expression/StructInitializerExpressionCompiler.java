package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.typing.Type;
import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.pr3.atypical.compiler.expression.ExpressionCompiler.*;

/**
 * @author tim
 */
public class StructInitializerExpressionCompiler {
    ExpressionCompiler expressionCompiler;

    public StructInitializerExpressionCompiler(ExpressionCompiler expressionCompiler) {
        this.expressionCompiler = expressionCompiler;
    }

    public Result compileStructInitializerExpression(AtypicalParser.StructInitializerExpressionContext structInitializerExpression){
        InsnList insnList = new InsnList();
        String typeName = structInitializerExpression.typeName().getText();
        Type type = Type.fromDescriptor(TypeUtil.toDesc(typeName, expressionCompiler.structureCompiler.imports.get(expressionCompiler.methodCompiler.fileName)));
        if(!type.isArrayType()) {
            String fullyQualifiedTypeName = type.getInternalName();
            insnList.add(new TypeInsnNode(Opcodes.NEW, fullyQualifiedTypeName));
            insnList.add(new InsnNode(Opcodes.DUP));
            List<Type> argTypes = new ArrayList<>();
            if (structInitializerExpression.argList() != null) {
                AtypicalParser.ArgListContext argList = structInitializerExpression.argList();
                for (AtypicalParser.ExpressionContext expression : argList.expression()) {
                    Result expressionResult = expressionCompiler.compileExpression(expression);
                    insnList.add(expressionResult.insnList());
                    argTypes.add(expressionResult.returnType());
                }
            }
            argTypes.add(Type.fromInternalName("java/lang/Void"));
            insnList.add(new InsnNode(Opcodes.ACONST_NULL));
            ClassNode owner = expressionCompiler.structureCompiler.generatedClassNodes.get(fullyQualifiedTypeName);
            MethodNode methodNode = ClassNodeUtil.getMethodNodeByNameAndParameterTypes(owner, "<init>", argTypes);
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, fullyQualifiedTypeName, "<init>", methodNode.desc));
            return new Result(insnList, type, Optional.empty(), SourceType.UNKNOWN);
        }else {
            Type arrayType = type.getUnderlyingArrayType();
            boolean primitive = arrayType.isPrimitiveType();
            int length = structInitializerExpression.argList() != null ? structInitializerExpression.argList().expression().size() : 0;
            if(structInitializerExpression.argList() != null){
                for (AtypicalParser.ExpressionContext expressionContext : structInitializerExpression.argList().expression()) {
                    insnList.add(expressionCompiler.compileExpression(expressionContext).insnList());
                }
            }
            insnList.add(new IntInsnNode(Opcodes.BIPUSH, length));
            if(!primitive){
                String argType = arrayType.isArrayType() ? arrayType.toString() : arrayType.getInternalName();
                insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, argType));
            }else {
                insnList.add(new IntInsnNode(Opcodes.NEWARRAY, getArrayTypeOpcode(arrayType)));
            }
            if(structInitializerExpression.argList() != null){
                for (int i = structInitializerExpression.argList().expression().size() - 1; i >= 0; i--) {
                    insnList.add(new InsnNode(Opcodes.DUP_X1));
                    insnList.add(new InsnNode(Opcodes.SWAP));
                    insnList.add(new IntInsnNode(Opcodes.BIPUSH, i));
                    insnList.add(new InsnNode(Opcodes.SWAP));
                    if (primitive) {
                        insnList.add(new InsnNode(Opcodes.IASTORE));
                    } else {
                        insnList.add(new InsnNode(Opcodes.AASTORE));
                    }
                }
            }
            return new Result(insnList, type, Optional.empty(), SourceType.UNKNOWN);
        }
    }

    public int getArrayTypeOpcode(Type arrayType){
        switch (arrayType.getKind()) {
            case Type.Kind.INT:
                return Opcodes.T_INT;
            case Type.Kind.BOOLEAN:
                return Opcodes.T_BOOLEAN;
            case Type.Kind.BYTE:
                return Opcodes.T_BYTE;
            case Type.Kind.CHAR:
                return Opcodes.T_CHAR;
            case Type.Kind.DOUBLE:
                return Opcodes.T_DOUBLE;
            case Type.Kind.FLOAT:
                return Opcodes.T_FLOAT;
            case Type.Kind.LONG:
                return Opcodes.T_LONG;
            case Type.Kind.SHORT:
                return Opcodes.T_SHORT;
            default:
                throw new IllegalStateException("Unsupported array type for array initialization: " + arrayType);
        }
    }
}
