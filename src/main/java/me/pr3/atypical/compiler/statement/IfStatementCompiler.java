package me.pr3.atypical.compiler.statement;

import me.pr3.atypical.compiler.MethodCompiler;
import me.pr3.atypical.compiler.StructureCompiler;
import me.pr3.atypical.compiler.expression.ExpressionCompiler;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

import static me.pr3.atypical.compiler.expression.ExpressionCompiler.*;
import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class IfStatementCompiler {

    private final StatementCompiler statementCompiler;

    public IfStatementCompiler(StatementCompiler statementCompiler) {
        this.statementCompiler = statementCompiler;
    }

    public InsnList compileIfStatement(IfStatementContext ifStatementContext){
        InsnList insnList = new InsnList();
        ExpressionCompiler expressionCompiler = new ExpressionCompiler(statementCompiler.structureCompiler, statementCompiler.methodCompiler);
        Result expressionResult = expressionCompiler.compileExpression(ifStatementContext.expression());
        if(!expressionResult.returnType().equals("Z"))throw new IllegalStateException("If statement expression did not evaluate to boolean");
        LabelNode endIfBlock = new LabelNode();
        LabelNode endElseBlocks = new LabelNode();
        insnList.add(expressionResult.insnList());
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, endIfBlock));
        for (StatementContext statementContext : ifStatementContext.statement()) {
            insnList.add(statementCompiler.compileStatement(statementContext));
        }
        if(!ifStatementContext.elseIfStatement().isEmpty() && ifStatementContext.elseStatement() != null){
            insnList.add(new JumpInsnNode(Opcodes.GOTO, endElseBlocks));
        }
        insnList.add(endIfBlock);
        for (ElseIfStatementContext elseIfStatementContext : ifStatementContext.elseIfStatement()) {
            Result elseIfResult = expressionCompiler.compileExpression(elseIfStatementContext.expression());
            if(!elseIfResult.returnType().equals("Z"))throw new IllegalStateException("Else if statement expression did not evaluate to boolean");
            LabelNode endElseIfBlock = new LabelNode();
            insnList.add(elseIfResult.insnList());
            insnList.add(new JumpInsnNode(Opcodes.IFEQ, endElseIfBlock));
            for (StatementContext statementContext : elseIfStatementContext.statement()) {
                insnList.add(statementCompiler.compileStatement(statementContext));
            }
            insnList.add(new JumpInsnNode(Opcodes.GOTO, endElseBlocks));
            insnList.add(endElseIfBlock);
        }
        if(ifStatementContext.elseStatement() != null){
            ElseStatementContext elseStatementContext  =ifStatementContext.elseStatement();
            for (StatementContext statementContext : elseStatementContext.statement()) {
                insnList.add(statementCompiler.compileStatement(statementContext));
            }
        }
        insnList.add(endElseBlocks);
        return insnList;
    }
}
