package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import static me.pr3.atypical.compiler.ExpressionCompiler.*;
import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class StatementCompiler {
    StructureCompiler structureCompiler;
    MethodCompiler methodCompiler;
    public StatementCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler){
        this.structureCompiler = structureCompiler;
        this.methodCompiler = methodCompiler;
    }

    public InsnList compileStatement(StatementContext context){
        InsnList insnList = new InsnList();
        ExpressionCompiler compiler = new ExpressionCompiler(structureCompiler, methodCompiler);
        if(context.localVariableDeclarationExpression() != null){
            LocalVariableDeclarationExpressionContext lvde = context.localVariableDeclarationExpression();
            String localVarType = TypeUtil.toDesc(lvde.typeName().getText());
            Result expressionResult = compiler.compileExpression(lvde.expression());
            insnList.add(expressionResult.insnList());
            int localVarIndex = methodCompiler.addLocalVar(localVarType, lvde.variableName().getText());
            insnList.add(new VarInsnNode(Opcodes.ISTORE, localVarIndex));
        }
        if(context.asignLocalVariableStatement() != null){
            AsignLocalVariableStatementContext alvs = context.asignLocalVariableStatement();
            int localVarIndex = methodCompiler.getLocalVarIndexByName(alvs.variableName().getText());
            Result expressionResult = compiler.compileExpression(alvs.expression());
            insnList.add(expressionResult.insnList());
            insnList.add(new VarInsnNode(Opcodes.ISTORE, localVarIndex));
        }
        return insnList;
    }
}
