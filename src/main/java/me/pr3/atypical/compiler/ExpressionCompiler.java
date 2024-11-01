package me.pr3.atypical.compiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class ExpressionCompiler {
    StructureCompiler structureCompiler;

    public ExpressionCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler){
        this.structureCompiler = structureCompiler;
    }

    public Result compileExpression(ExpressionContext context){
        InsnList insnList = new InsnList();
        String resultType = "V";
        if(context.binaryExpression() != null){
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
        if(context.terminalExpression() != null){
            TerminalExpressionContext terminalExpressionContext = context.terminalExpression();
            int value = Integer.parseInt(terminalExpressionContext.literal().getText());
            insnList.add(new IntInsnNode(Opcodes.BIPUSH, value));
            resultType = "I";
        }
        return new Result(insnList, resultType);
    }

    public Result getInsnListForBinaryOperator(BinaryOperatorContext operator, String type){
        if(operator.ADD() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){{add(new InsnNode(Opcodes.IADD));}}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if(operator.MUL() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){{add(new InsnNode(Opcodes.IMUL));}}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if(operator.SUB() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){{add(new InsnNode(Opcodes.ISUB));}}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if(operator.DIV() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){{add(new InsnNode(Opcodes.IDIV));}}, "I");
                default -> throw new IllegalArgumentException("");
            };
        }
        if(operator.CMPEQ() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){
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
        if(operator.CMPNE() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){
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
        if(operator.CMPGT() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){
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
        if(operator.CMPLT() != null){
            return switch (type) {
                case "I" -> new Result(new InsnList(){
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

    public record Result(InsnList insnList, String returnType){}
}
