package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.MethodCompiler;
import me.pr3.atypical.compiler.StructInitializerCompiler;
import me.pr3.atypical.compiler.StructureCompiler;
import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

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
                    insnList.add(new VarInsnNode(getLoadInstructionForType(type), varIndex));
                    resultType = type;
                }else if(name.equals("this")){
                    if(Modifier.isStatic(methodCompiler.methodNode.access)) {
                        throw new IllegalStateException("Reserved keyword 'this' not allowed in non static methods");
                    }
                    if(structureCompiler.isClassNameImplClass(methodCompiler.className)){
                        String type = TypeUtil.toDesc(methodCompiler.fullyQualifyType(methodCompiler.className.split("\\$")[0]));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, methodCompiler.className, "this_", type));
                        resultType = type;
                    }else {
                        String type = TypeUtil.toDesc(methodCompiler.fullyQualifyType(methodCompiler.className));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        resultType = type;
                    }
                }else if(isLocalFieldName(name)){
                    FieldNode fieldNode = getLocalFieldByName(name);
                    String type = fieldNode.desc;
                    if(Modifier.isStatic(methodCompiler.methodNode.access)){
                        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, methodCompiler.className, name, type));
                    }else{
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new FieldInsnNode(Opcodes.GETFIELD, methodCompiler.className, name, type));
                    }
                    resultType = type;
                } else if (isClassName(name)) {
                    resultType = TypeUtil.toTypePrefixed(TypeUtil.toDesc(name));
                }
            }
        }
        if (context.memberAccessExpression() != null) {
            MemberAccessExpressionCompiler memberAccessExpressionCompiler = new MemberAccessExpressionCompiler(this);
            return memberAccessExpressionCompiler.compileMemberAccessExpression(context);
        }
        if(context.castExpression() != null){
            CastExpressionCompiler castExpressionCompiler = new CastExpressionCompiler(this);
            return castExpressionCompiler.compileCastExpression(context.castExpression());
        }
        if(context.structInitializerExpression() != null){
            StructInitializerExpressionCompiler structInitializerExpressionCompiler = new StructInitializerExpressionCompiler(this);
            Result result = structInitializerExpressionCompiler.compileStructInitializerExpression(context.structInitializerExpression());
            insnList.add(result.insnList);
            resultType = result.returnType;
        }
        if(context.parenthesesExpression() != null){
            ParenthesesExpressionContext parenthesesExpression = context.parenthesesExpression();
            Result expressionResult = compileExpression(parenthesesExpression.expression());
            insnList.add(expressionResult.insnList);
            resultType = expressionResult.returnType;
        }
        if(context.arrayAccessExpression() != null){
            Result leftExpressionResult = compileExpression(context.left);
            if(!TypeUtil.isArrayType(leftExpressionResult.returnType))throw new IllegalStateException("Cant use array access operator on non array type");
            insnList.add(leftExpressionResult.insnList);
            Result indexExpressionResult = compileExpression(context.arrayAccessExpression().expression());
            if(!indexExpressionResult.returnType.equals("I"))
                throw new IllegalStateException("Index expression for array access operator has to evaluate to int, evaluated to: " + indexExpressionResult.returnType);
            insnList.add(indexExpressionResult.insnList);
            insnList.add(new InsnNode(Opcodes.AALOAD));
            resultType = leftExpressionResult.returnType.substring(1);
        }
        return new Result(insnList, resultType);
    }

    private boolean isLocalFieldName(String name) {
        return structureCompiler.generatedClassNodes.get(methodCompiler.className).fields.stream().anyMatch(f -> f.name.equals(name));
    }

    private FieldNode getLocalFieldByName(String name) {
        return structureCompiler.generatedClassNodes.get(methodCompiler.className).fields.stream().filter(f -> f.name.equals(name)).findAny().orElseThrow();
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

    public int getLoadInstructionForType(String varType){
        return switch (varType){
            case "I", "Z" -> Opcodes.ILOAD;
            default -> Opcodes.ALOAD;
        };
    }

    public record Result(InsnList insnList, String returnType) {
    }
}
