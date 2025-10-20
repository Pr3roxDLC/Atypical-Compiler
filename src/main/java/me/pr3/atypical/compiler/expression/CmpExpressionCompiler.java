package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.typing.Type;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Optional;

public class CmpExpressionCompiler {
    private ExpressionCompiler expressionCompiler;

    public CmpExpressionCompiler(ExpressionCompiler expressionCompiler) {
        this.expressionCompiler = expressionCompiler;
    }

    public ExpressionCompiler.Result compileCmpExpression(AtypicalParser.ExpressionContext context) {


        ExpressionCompiler.Result lhs = expressionCompiler.compilePostfixExpression(context.postfixExpression(), false);
        InsnList insnList = new InsnList();
        insnList.add(lhs.insnList());

        ExpressionCompiler.Result rhs = expressionCompiler.compileExpression(context.expression());
        int opcode = getCompareOpcode(context, lhs.returnType(), rhs.returnType());
        insnList.add(rhs.insnList());
        LabelNode trueLabel = new LabelNode();
        LabelNode endLabel = new LabelNode();
        insnList.add(new JumpInsnNode(opcode, trueLabel));
        insnList.add(new InsnNode(Opcodes.ICONST_0));
        insnList.add(new JumpInsnNode(Opcodes.GOTO, endLabel));
        insnList.add(trueLabel);
        insnList.add(new InsnNode(Opcodes.ICONST_1));
        insnList.add(endLabel);
        return new ExpressionCompiler.Result(insnList, Type.BOOLEAN, Optional.empty(), ExpressionCompiler.SourceType.UNKNOWN);
    }

    public int getCompareOpcode(AtypicalParser.ExpressionContext context, Type lhsType, Type rhsType) {
        if (context.CMPEQ() != null) {
            switch (lhsType.getKind()) {
                case Type.Kind.INT:
                    return Opcodes.IF_ICMPEQ;
                default:
                    return Opcodes.IF_ACMPEQ;
            }
        }
        if (context.CMPNE() != null) {
            switch (lhsType.getKind()) {
                case Type.Kind.INT:
                    return Opcodes.IF_ICMPNE;
                default:
                    return Opcodes.IF_ACMPNE;
            }
        }
        if (context.CMPGT() != null) {
            switch (lhsType.getKind()) {
                case Type.Kind.INT:
                    return Opcodes.IF_ICMPGT;
                default:
                    throw new IllegalStateException("Cannot use greater than comparison on type " + lhsType);
            }
        }
        if (context.CMPLT() != null) {
            switch (lhsType.getKind()) {
                case Type.Kind.INT:
                    return Opcodes.IF_ICMPLT;
                default:
                    throw new IllegalStateException("Cannot use greater than comparison on type " + lhsType);
            }
        }
        throw new IllegalStateException("Unknown comparison operator");

    }

}
