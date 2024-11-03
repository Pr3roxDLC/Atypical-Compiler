package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.antlr.v4.runtime.tree.ParseTree;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class ExpressionCompiler {
    StructureCompiler structureCompiler;
    MethodCompiler methodCompiler;

    public ExpressionCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler) {
        this.structureCompiler = structureCompiler;
        this.methodCompiler = methodCompiler;
    }

    public Result compileExpression(ExpressionContext context) {
        InsnList insnList = new InsnList();
        String resultType = "V";
        if (context.binaryExpression() != null) {
            Result leftResult = compileExpression(context.left);
            insnList.add(leftResult.insnList);
            BinaryExpressionContext binaryExpressionContext = context.binaryExpression();
            BinaryOperatorContext op = binaryExpressionContext.op;
            Result rightResult = compileExpression(binaryExpressionContext.right);
            insnList.add(rightResult.insnList);
            Result opResult = getInsnListForBinaryOperator(op, leftResult.returnType);
            insnList.add(opResult.insnList);
            resultType = opResult.returnType;
        }
        if (context.terminalExpression() != null) {
            TerminalExpressionContext terminalExpressionContext = context.terminalExpression();
            if (terminalExpressionContext.literal() != null) {
                if(terminalExpressionContext.literal().NUMBER() != null) {
                    int value = Integer.parseInt(terminalExpressionContext.literal().getText());
                    insnList.add(new IntInsnNode(Opcodes.BIPUSH, value));
                    resultType = "I";
                }
                if(terminalExpressionContext.literal().string() != null){
                    String content = terminalExpressionContext.literal().string().getText();
                    insnList.add(new LdcInsnNode(content));
                    resultType = "Ljava/lang/String;";
                }
            }
            if (terminalExpressionContext.memberOrVariableName() != null) {
                String name = terminalExpressionContext.memberOrVariableName().getText();
                if (methodCompiler.containsLocalVarWithName(name)) {
                    String type = methodCompiler.getLocalVarTypeByName(name);
                    int varIndex = methodCompiler.getLocalVarIndexByName(name);
                    insnList.add(new VarInsnNode(Opcodes.ILOAD, varIndex));
                    resultType = type;
                } else if (isClassName(name)) {
                    resultType = TypeUtil.toTypePrefixed(TypeUtil.toDesc(name));
                }
            }
        }
        if (context.memberAccessExpression() != null) {
            MemberAccessExpressionContext memberAccessExpression = context.memberAccessExpression();
            ExpressionContext expression = context.expression();
            Result expressionResult = compileExpression(expression);
            if (TypeUtil.isTypePrefixedDesc(expressionResult.returnType)) {
                //Expression evaluated to a Type prefixed descriptor, this means that we will invoke or access a static
                // member on this type;
                String staticType = TypeUtil.fromTypePrefixed(expressionResult.returnType);
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
                                    Result argExpressionResult = compileExpression(argExpression);
                                    insnList.add(argExpressionResult.insnList);
                                    desc.append(argExpressionResult.returnType);
                                }
                            }
                            String owner = TypeUtil.extractClassFromType(staticType);
                            String importMappedOwner = methodCompiler.fullyQualifyType(owner);
                            ClassNode owningClassNode = structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
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
                            String importMappedOwner = methodCompiler.fullyQualifyType(owner);
                            ClassNode owningClassNode = structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
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
                                Result argExpressionResult = compileExpression(argExpression);
                                insnList.add(argExpressionResult.insnList);
                                desc.append(argExpressionResult.returnType);
                            }
                        }
                        String owner = TypeUtil.extractClassFromType(lastType);
                        String importMappedOwner = methodCompiler.fullyQualifyType(owner);
                        ClassNode owningClassNode = structureCompiler.generatedClassNodes.getOrDefault(importMappedOwner, ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedOwner));
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
        }
        return new Result(insnList, resultType);
    }

    private boolean isClassName(String type){
        String importMappedType = methodCompiler.fullyQualifyType(type);
        return structureCompiler.generatedClassNodes.containsKey(importMappedType) || ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedType) != null;
    }

    public Result getInsnListForBinaryOperator(BinaryOperatorContext operator, String type) {
        if (operator.ADD() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {{
                    add(new InsnNode(Opcodes.IADD));
                }}, "I");
                default -> throw new IllegalArgumentException(type);
            };
        }
        if (operator.MUL() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {{
                    add(new InsnNode(Opcodes.IMUL));
                }}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.SUB() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {{
                    add(new InsnNode(Opcodes.ISUB));
                }}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.DIV() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {{
                    add(new InsnNode(Opcodes.IDIV));
                }}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.CMPEQ() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {
                    {
                        LabelNode trueLabel = new LabelNode();
                        LabelNode falseLabel = new LabelNode();
                        add(new JumpInsnNode(Opcodes.IF_ICMPEQ, trueLabel));
                        add(new InsnNode(Opcodes.ICONST_0));
                        add(new JumpInsnNode(Opcodes.GOTO, falseLabel));
                        add(trueLabel);
                        add(new InsnNode(Opcodes.ICONST_1));
                        add(falseLabel);
                    }
                }, "Z");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.CMPNE() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {
                    {
                        LabelNode trueLabel = new LabelNode();
                        LabelNode falseLabel = new LabelNode();
                        add(new JumpInsnNode(Opcodes.IF_ICMPNE, trueLabel));
                        add(new InsnNode(Opcodes.ICONST_0));
                        add(new JumpInsnNode(Opcodes.GOTO, falseLabel));
                        add(trueLabel);
                        add(new InsnNode(Opcodes.ICONST_1));
                        add(falseLabel);
                    }
                }, "Z");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.CMPGT() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {
                    {
                        LabelNode trueLabel = new LabelNode();
                        LabelNode falseLabel = new LabelNode();
                        add(new JumpInsnNode(Opcodes.IF_ICMPGT, trueLabel));
                        add(new InsnNode(Opcodes.ICONST_0));
                        add(new JumpInsnNode(Opcodes.GOTO, falseLabel));
                        add(trueLabel);
                        add(new InsnNode(Opcodes.ICONST_1));
                        add(falseLabel);
                    }
                }, "Z");
                default -> throw new IllegalArgumentException("");
            };
        }
        if (operator.CMPLT() != null) {
            return switch (type) {
                case "I" -> new Result(new InsnList() {
                    {
                        LabelNode trueLabel = new LabelNode();
                        LabelNode falseLabel = new LabelNode();
                        add(new JumpInsnNode(Opcodes.IF_ICMPLT, trueLabel));
                        add(new InsnNode(Opcodes.ICONST_0));
                        add(new JumpInsnNode(Opcodes.GOTO, falseLabel));
                        add(trueLabel);
                        add(new InsnNode(Opcodes.ICONST_1));
                        add(falseLabel);

                    }
                }, "Z");
                default -> throw new IllegalArgumentException("");
            };
        }
        throw new IllegalStateException("Unable to resolve binary operator instructions for op: "
                + operator.getText() + " for type: " + type);
    }

    public record Result(InsnList insnList, String returnType) {
    }
}
