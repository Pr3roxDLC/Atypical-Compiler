package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

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
        String typeDesc = TypeUtil.toDesc(typeName, expressionCompiler.structureCompiler.imports.get(expressionCompiler.methodCompiler.fileName));
        if(!TypeUtil.isArrayType(typeDesc)) {
            String fullyQualifiedTypeName = TypeUtil.extractTypeNameFromDescriptor(typeDesc);
            insnList.add(new TypeInsnNode(Opcodes.NEW, fullyQualifiedTypeName));
            insnList.add(new InsnNode(Opcodes.DUP));
            StringBuilder desc = new StringBuilder("(");
            if (structInitializerExpression.argList() != null) {
                AtypicalParser.ArgListContext argList = structInitializerExpression.argList();
                for (AtypicalParser.ExpressionContext expression : argList.expression()) {
                    Result expressionResult = expressionCompiler.compileExpression(expression);
                    insnList.add(expressionResult.insnList());
                    desc.append(expressionResult.returnType());
                }
            }
            desc.append("Ljava/lang/Void;");
            insnList.add(new InsnNode(Opcodes.ACONST_NULL));
            desc.append(")V");
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, fullyQualifiedTypeName, "<init>", desc.toString()));
            return new Result(insnList, typeDesc, Optional.empty(), SourceType.UNKNOWN);
        }else {
            String arrayType = typeDesc.substring(1);
            boolean primitive = TypeUtil.isPrimitiveType(arrayType);
            int length = structInitializerExpression.argList() != null ? structInitializerExpression.argList().expression().size() : 0;
            if(structInitializerExpression.argList() != null){
                for (AtypicalParser.ExpressionContext expressionContext : structInitializerExpression.argList().expression()) {
                    insnList.add(expressionCompiler.compileExpression(expressionContext).insnList());
                }
            }
            insnList.add(new IntInsnNode(Opcodes.BIPUSH, length));
            if(!primitive){
                String type = TypeUtil.isArrayType(arrayType) ? arrayType : TypeUtil.extractTypeNameFromDescriptor(arrayType);
                insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, type));
            }else {
                insnList.add(new TypeInsnNode(Opcodes.NEWARRAY, arrayType));
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
            return new Result(insnList, typeDesc, Optional.empty(), SourceType.UNKNOWN);
        }
    }
}
