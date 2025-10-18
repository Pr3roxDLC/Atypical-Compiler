package me.pr3.atypical.compiler.statement;

import me.pr3.atypical.compiler.expression.ExpressionCompiler;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.List;

public class WhileStatementCompiler {

    private StatementCompiler statementCompiler;
    public WhileStatementCompiler(StatementCompiler statementCompiler) {
        this.statementCompiler = statementCompiler;
    }

    public InsnList compileWhileStatement(AtypicalParser.WhileStatementContext whileStatementContext) {
        InsnList insnList = new InsnList();
        ExpressionCompiler expressionCompiler = new ExpressionCompiler(statementCompiler.structureCompiler, statementCompiler.methodCompiler);
        AtypicalParser.ExpressionContext expressionContext = whileStatementContext.expression();
        List<AtypicalParser.StatementContext> statementContexts = whileStatementContext.statement();
        LabelNode startOfLoop = new LabelNode();
        LabelNode endOfLoop = new LabelNode();
        insnList.add(startOfLoop);
        ExpressionCompiler.Result expressionResult = expressionCompiler.compileExpression(expressionContext);
        if (!expressionResult.returnType().equals("Z"))
            throw new IllegalStateException("While statement expression did not evaluate to boolean");
        insnList.add(expressionResult.insnList());
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, endOfLoop));
        for (AtypicalParser.StatementContext statementContext : statementContexts) {
            insnList.add(statementCompiler.compileStatement(statementContext));
        }
        insnList.add(new JumpInsnNode(Opcodes.GOTO, startOfLoop));
        insnList.add(endOfLoop);
        return insnList;
    }

}
