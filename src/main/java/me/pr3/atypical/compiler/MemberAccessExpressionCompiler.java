package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
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
    public MemberAccessExpressionCompiler(ExpressionCompiler compiler){
        this.expressionCompiler = compiler;
    }

    public Result compileMemberAccessExpression(ExpressionContext context){
        String resultType = "V";
        InsnList insnList = new InsnList();
        MemberAccessExpressionContext memberAccessExpression = context.memberAccessExpression();
        ExpressionContext expression = context.expression();
        Result expressionResult = expressionCompiler.compileExpression(expression);
        insnList.add(expressionResult.insnList());
        if (TypeUtil.isTypePrefixedDesc(expressionResult.returnType())) {
            //Expression evaluated to a Type prefixed descriptor, this means that we will invoke or access a static
            // member on this type;
            String staticType = TypeUtil.fromTypePrefixed(expressionResult.returnType());
            String lastType = staticType;
            for (ParseTree child : memberAccessExpression.children) {
                //Primary access is special because and is more complex than the rest of the chain
                if (child instanceof PrimaryMemberAccessContext primaryMemberAccessContext) {
                    if (primaryMemberAccessContext.methodInvocationExpression() != null) {
                        MethodInvocationExpressionContext methodInvocationExpression = primaryMemberAccessContext.methodInvocationExpression();
                        String methodName = methodInvocationExpression.memberName().getText();
                        StringBuilder desc = new StringBuilder();
                        if (methodInvocationExpression.argList() != null) {
                            for (ExpressionContext argExpression : methodInvocationExpression.argList().expression()) {
                                Result argExpressionResult = expressionCompiler.compileExpression(argExpression);
                                insnList.add(argExpressionResult.insnList());
                                desc.append(argExpressionResult.returnType());
                            }
                        }
                        String owner = TypeUtil.extractClassFromType(staticType);
                        String importMappedOwner = expressionCompiler.methodCompiler.fullyQualifyType(owner);
                        ClassNode owningClassNode = expressionCompiler.structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
                        MethodNode invokedMethod = ClassNodeUtil.getMethodNodeByNameAndParameterTypes(
                                owningClassNode,
                                methodName,
                                desc.toString());
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, invokedMethod.name, invokedMethod.desc));
                        resultType = Type.getMethodType(invokedMethod.desc).getReturnType().toString();
                        lastType = resultType;
                    }
                    if(primaryMemberAccessContext.fieldAccessExpression() != null) {
                        FieldAccessExpressionContext fieldAccessExpression = primaryMemberAccessContext.fieldAccessExpression();
                        String fieldName = fieldAccessExpression.memberName().getText();
                        String owner = TypeUtil.extractClassFromType(staticType);
                        String importMappedOwner = expressionCompiler.methodCompiler.fullyQualifyType(owner);
                        ClassNode owningClassNode = expressionCompiler.structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
                        FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(owningClassNode, fieldName);
                        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, importMappedOwner, fieldName, fieldNode.desc));
                        resultType = fieldNode.desc;
                        lastType = resultType;
                    }
                }
                if(child instanceof MethodInvocationExpressionContext methodInvocationExpression){
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
                    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, invokedMethod.name, invokedMethod.desc));
                    resultType = Type.getMethodType(invokedMethod.desc).getReturnType().toString();
                    lastType = resultType;
                }
            }
        }else {
            //Expression does not start with a static type reference, so no method invocations or field access
            // will be done statically
            String lastType = expressionResult.returnType();
            for (ParseTree child : memberAccessExpression.children) {
                //Primary access is special because and is more complex than the rest of the chain
                if (child instanceof PrimaryMemberAccessContext primaryMemberAccessContext) {
                    if (primaryMemberAccessContext.methodInvocationExpression() != null) {
                        MethodInvocationExpressionContext methodInvocationExpression = primaryMemberAccessContext.methodInvocationExpression();
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
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, invokedMethod.name, invokedMethod.desc));
                        resultType = Type.getMethodType(invokedMethod.desc).getReturnType().toString();
                        lastType = resultType;
                    }
                    if(primaryMemberAccessContext.fieldAccessExpression() != null) {
                        FieldAccessExpressionContext fieldAccessExpression = primaryMemberAccessContext.fieldAccessExpression();
                        String fieldName = fieldAccessExpression.memberName().getText();
                        String owner = TypeUtil.extractClassFromType(lastType);
                        String importMappedOwner = expressionCompiler.methodCompiler.fullyQualifyType(owner);
                        ClassNode owningClassNode = expressionCompiler.structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
                        FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(owningClassNode, fieldName);
                        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, importMappedOwner, fieldName, fieldNode.desc));
                        resultType = fieldNode.desc;
                        lastType = resultType;
                    }
                }
                if(child instanceof MethodInvocationExpressionContext methodInvocationExpression){
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
                    insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, invokedMethod.name, invokedMethod.desc));
                    resultType = Type.getMethodType(invokedMethod.desc).getReturnType().toString();
                    lastType = resultType;
                }
            }
        }
        return new Result(insnList, resultType);
    }

}
