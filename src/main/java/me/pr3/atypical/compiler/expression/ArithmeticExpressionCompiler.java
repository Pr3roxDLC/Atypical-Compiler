package me.pr3.atypical.compiler.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

import java.util.Optional;

public class ArithmeticExpressionCompiler {
    private ExpressionCompiler expressionCompiler;
    public ArithmeticExpressionCompiler(ExpressionCompiler expressionCompiler) {
        this.expressionCompiler = expressionCompiler;
    }

    public ExpressionCompiler.Result compileArithmeticExpression(me.pr3.atypical.generated.AtypicalParser.ExpressionContext context) {
        String returnType = "V";
        ExpressionCompiler.Result lhs = expressionCompiler.compilePostfixExpression(context.postfixExpression(), false);
        returnType = lhs.returnType();
        InsnList insnList = new InsnList();
        insnList.add(lhs.insnList());

        ExpressionCompiler.Result rhs = expressionCompiler.compileExpression(context.expression());
        insnList.add(rhs.insnList());

        if(context.ADD() != null){

            switch (lhs.returnType()){
                case "I":
                    insnList.add(new InsnNode(Opcodes.IADD));
                    break;
                case "F":
                    insnList.add(new InsnNode(Opcodes.FADD));
                    break;
                case "J":
                    insnList.add(new InsnNode(Opcodes.LADD));
                    break;
                case "D":
                    insnList.add(new InsnNode(Opcodes.DADD));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type for addition: " + lhs.returnType());
            }
            return new ExpressionCompiler.Result(insnList, returnType, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }

        if(context.SUB() != null){

            switch (lhs.returnType()){
                case "I":
                    insnList.add(new InsnNode(Opcodes.ISUB));
                    break;
                case "F":
                    insnList.add(new InsnNode(Opcodes.FSUB));
                    break;
                case "J":
                    insnList.add(new InsnNode(Opcodes.LSUB));
                    break;
                case "D":
                    insnList.add(new InsnNode(Opcodes.DSUB));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type for subtraction: " + lhs.returnType());
            }
            return new ExpressionCompiler.Result(insnList, returnType, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }

        if(context.MUL() != null){

            switch (lhs.returnType()){
                case "I":
                    insnList.add(new InsnNode(Opcodes.IMUL));
                    break;
                case "F":
                    insnList.add(new InsnNode(Opcodes.FMUL));
                    break;
                case "J":
                    insnList.add(new InsnNode(Opcodes.LMUL));
                    break;
                case "D":
                    insnList.add(new InsnNode(Opcodes.DMUL));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type for multiplication: " + lhs.returnType());
            }
            return new ExpressionCompiler.Result(insnList, returnType, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }

        if(context.DIV() != null){

            switch (lhs.returnType()){
                case "I":
                    insnList.add(new InsnNode(Opcodes.IDIV));
                    break;
                case "F":
                    insnList.add(new InsnNode(Opcodes.FDIV));
                    break;
                case "J":
                    insnList.add(new InsnNode(Opcodes.LDIV));
                    break;
                case "D":
                    insnList.add(new InsnNode(Opcodes.DDIV));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type for division: " + lhs.returnType());
            }
            return new ExpressionCompiler.Result(insnList, returnType, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }

        if(context.MOD() != null){

            switch (lhs.returnType()){
                case "I":
                    insnList.add(new InsnNode(Opcodes.IREM));
                    break;
                case "F":
                    insnList.add(new InsnNode(Opcodes.FREM));
                    break;
                case "J":
                    insnList.add(new InsnNode(Opcodes.LREM));
                    break;
                case "D":
                    insnList.add(new InsnNode(Opcodes.DREM));
                    break;
                default:
                    throw new IllegalStateException("Unsupported type for modulus: " + lhs.returnType());
            }
            return new ExpressionCompiler.Result(insnList, returnType, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }
        if(context.LOGIC_AND() != null){
            if(!lhs.returnType().equals("Z") || !rhs.returnType().equals("Z")){
                throw new IllegalStateException("Logical AND operation requires boolean types");
            }
            insnList.add(new InsnNode(Opcodes.IAND));
            return new ExpressionCompiler.Result(insnList, "Z", Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }
        if(context.LOGIC_OR() != null){
            if(!lhs.returnType().equals("Z") || !rhs.returnType().equals("Z")){
                throw new IllegalStateException("Logical OR operation requires boolean types");
            }
            insnList.add(new InsnNode(Opcodes.IOR));
            return new ExpressionCompiler.Result(insnList, "Z", Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
        }
        return null;
    }

}
