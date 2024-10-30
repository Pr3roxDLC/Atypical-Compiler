package me.pr3.atypical.compiler;

import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class ExpressionCompiler {
    StructureCompiler structureCompiler;

    public ExpressionCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler){
        this.structureCompiler = structureCompiler;
    }

    public InsnList compileExpression(ExpressionContext context){
        InsnList insnList = new InsnList();
        if(context.binaryExpression() != null){
            insnList.add(compileExpression(context.left));
            BinaryExpressionContext binaryExpressionContext = context.binaryExpression();
            BinaryOperatorContext op = binaryExpressionContext.op;
            insnList.add(compileExpression(binaryExpressionContext.right));
            insnList.add(getInsnForBinaryOperator(op));
        }
        if(context.terminalExpression() != null){
            TerminalExpressionContext terminalExpressionContext = context.terminalExpression();
            int value = Integer.parseInt(terminalExpressionContext.literal().getText());
            insnList.add(new IntInsnNode(Opcodes.BIPUSH, value));
        }
        return insnList;
    }

    public InsnNode getInsnForBinaryOperator(BinaryOperatorContext operator){
        if(operator.ADD() != null)return new InsnNode(Opcodes.IADD);
        return null;
    }

}
