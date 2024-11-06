package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.antlr.v4.runtime.tree.ParseTree;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static me.pr3.atypical.compiler.ExpressionCompiler.*;
import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class MemberAccessExpressionCompiler {
    ExpressionCompiler expressionCompiler;

    public MemberAccessExpressionCompiler(ExpressionCompiler compiler) {
        this.expressionCompiler = compiler;
    }

    public Result compileMemberAccessExpression(ExpressionContext context) {
        String resultType = "V";
        InsnList insnList = new InsnList();
        MemberAccessExpressionContext memberAccessExpression = context.memberAccessExpression();
        ExpressionContext expression = context.expression();
        Result expressionResult = expressionCompiler.compileExpression(expression);
        insnList.add(expressionResult.insnList());
        boolean startsWithStaticClassReference = TypeUtil.isTypePrefixedDesc(expressionResult.returnType());
        String lastType = startsWithStaticClassReference ?
                TypeUtil.fromTypePrefixed(expressionResult.returnType())
                : expressionResult.returnType();
        int opcodeForFirstMethodInvoke = startsWithStaticClassReference ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        int opcodeForFirstFieldAccess = startsWithStaticClassReference ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
        for (ParseTree child : memberAccessExpression.children) {
            //Primary access is special because it can contain a static class reference
            if (child instanceof PrimaryMemberAccessContext primaryMemberAccessContext) {
                if (primaryMemberAccessContext.methodInvocationExpression() != null) {
                    MethodInvocationExpressionContext methodInvocationExpression = primaryMemberAccessContext.methodInvocationExpression();
                    resultType = insertMethodInvocationInstructions(insnList, lastType, methodInvocationExpression, opcodeForFirstMethodInvoke);
                    lastType = resultType;
                }
                if (primaryMemberAccessContext.fieldAccessExpression() != null) {
                    resultType = addFieldAccessInstructions(primaryMemberAccessContext.fieldAccessExpression(), lastType, insnList, opcodeForFirstFieldAccess);
                    lastType = resultType;
                }
            }
            if (child instanceof MethodInvocationExpressionContext methodInvocationExpression) {
                resultType = insertMethodInvocationInstructions(insnList, lastType, methodInvocationExpression, Opcodes.INVOKEVIRTUAL);
                lastType = resultType;
            }
            if (child instanceof FieldAccessExpressionContext fieldAccessExpressionContext) {
                resultType = addFieldAccessInstructions(fieldAccessExpressionContext, lastType, insnList, Opcodes.GETFIELD);
                lastType = resultType;
            }
        }
        return new Result(insnList, resultType);
    }

    private String insertMethodInvocationInstructions(InsnList insnList, String lastType, MethodInvocationExpressionContext methodInvocationExpression, int opcode) {
        String methodName = methodInvocationExpression.memberName().getText();
        StringBuilder desc = new StringBuilder();
        if (methodInvocationExpression.argList() != null) {
            for (ExpressionContext argExpression : methodInvocationExpression.argList().expression()) {
                Result argExpressionResult = expressionCompiler.compileExpression(argExpression);
                insnList.add(argExpressionResult.insnList());
                desc.append(argExpressionResult.returnType());
            }
        }
        String owner = TypeUtil.extractClassFromType(lastType);
        String importMappedOwner = expressionCompiler.methodCompiler.fullyQualifyType(owner);
        ClassNode owningClassNode = expressionCompiler.structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
        MethodNode invokedMethod = ClassNodeUtil.getMethodNodeByNameAndParameterTypes(
                owningClassNode,
                methodName,
                desc.toString());
        insnList.add(new MethodInsnNode(opcode, owner, invokedMethod.name, invokedMethod.desc));
        return Type.getMethodType(invokedMethod.desc).getReturnType().toString();
    }

    private String addFieldAccessInstructions(FieldAccessExpressionContext fieldAccessExpression, String staticType, InsnList insnList, int getstatic) {
        String fieldName = fieldAccessExpression.memberName().getText();
        String owner = TypeUtil.extractClassFromType(staticType);
        String importMappedOwner = expressionCompiler.methodCompiler.fullyQualifyType(owner);
        ClassNode owningClassNode = expressionCompiler.structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
        FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(owningClassNode, fieldName);
        insnList.add(new FieldInsnNode(getstatic, importMappedOwner, fieldName, fieldNode.desc));
        return fieldNode.desc;
    }

}
