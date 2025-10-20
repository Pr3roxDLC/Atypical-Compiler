package me.pr3.atypical.compiler.statement;

import me.pr3.atypical.compiler.MethodCompiler;
import me.pr3.atypical.compiler.StructureCompiler;
import me.pr3.atypical.compiler.expression.ExpressionCompiler;
import me.pr3.atypical.compiler.typing.Descriptor;
import me.pr3.atypical.compiler.typing.Type;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import static me.pr3.atypical.compiler.expression.ExpressionCompiler.*;
import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class StatementCompiler {
    StructureCompiler structureCompiler;
    MethodCompiler methodCompiler;

    public StatementCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler) {
        this.structureCompiler = structureCompiler;
        this.methodCompiler = methodCompiler;
    }

    public InsnList compileStatement(StatementContext context) {
        InsnList insnList = new InsnList();
        int startLineNumber = context.getStart().getLine();
        LabelNode startLabel = new LabelNode(new Label());
        insnList.add(startLabel);
        ExpressionCompiler compiler = new ExpressionCompiler(structureCompiler, methodCompiler);
        if (context.localVariableDeclarationExpression() != null) {
            LocalVariableDeclarationExpressionContext lvde = context.localVariableDeclarationExpression();
            String localVarType = TypeUtil.toDesc(lvde.typeName().getText(), structureCompiler.imports.get(methodCompiler.fileName));
            Result expressionResult = compiler.compileExpression(lvde.expression());
            insnList.add(expressionResult.insnList());
            int localVarIndex = methodCompiler.addLocalVar(Type.fromDescriptor(localVarType), lvde.variableName().getText());
            insnList.add(new VarInsnNode(getStoreInstruction(expressionResult.returnType()), localVarIndex));
        }
        if (context.asignLocalVariableStatement() != null) {
            AsignLocalVariableStatementContext alvs = context.asignLocalVariableStatement();
            int localVarIndex = methodCompiler.getLocalVarIndexByName(alvs.variableName().getText());
            Result expressionResult = compiler.compileExpression(alvs.expression());
            insnList.add(expressionResult.insnList());
            insnList.add(new VarInsnNode(getStoreInstruction(expressionResult.returnType()), localVarIndex));
        }
        if (context.expression() != null) {
            ExpressionContext expression = context.expression();
            Result expressionResult = compiler.compileExpression(expression);
            insnList.add(expressionResult.insnList());
        }
        if (context.returnStatement() != null) {
            ReturnStatementContext returnStatement = context.returnStatement();
            if (returnStatement.expression() != null) {
                Result expressionResult = compiler.compileExpression(returnStatement.expression());
                if(expressionResult.returnType().getKind() != Descriptor.fromString(methodCompiler.methodNode.desc).getReturnType().getKind()){
                    throw new IllegalArgumentException("Return Type Mismatch");
                }
                insnList.add(expressionResult.insnList());
                insnList.add(new InsnNode(getReturnInstructionForType(expressionResult.returnType())));
            } else {
                if(Descriptor.fromString(methodCompiler.methodNode.desc).getReturnType().getKind() != Type.Kind.VOID){
                    throw new IllegalArgumentException("Return Type Mismatch");
                }
                insnList.add(new InsnNode(Opcodes.RETURN));
            }
        }
        if(context.ifStatement() != null){
            IfStatementCompiler ifStatementCompiler = new IfStatementCompiler(this);
            insnList.add(ifStatementCompiler.compileIfStatement(context.ifStatement()));
        }
        if(context.whileStatement() != null){
            WhileStatementCompiler whileStatementCompiler = new WhileStatementCompiler(this);
            insnList.add(whileStatementCompiler.compileWhileStatement(context.whileStatement()));
        }
        insnList.add(new LineNumberNode(startLineNumber, startLabel));
        return insnList;
    }

    private int getReturnInstructionForType(Type returnType) {
        return switch (returnType.getKind()) {
            case Type.Kind.INT, Type.Kind.BOOLEAN -> Opcodes.IRETURN;
            case Type.Kind.VOID -> Opcodes.RETURN;
            default -> Opcodes.ARETURN;
        };
    }

    private int getStoreInstruction(Type varType){
        return switch (varType.getKind()){
            case Type.Kind.INT, Type.Kind.BOOLEAN -> Opcodes.ISTORE;
            default -> Opcodes.ASTORE;
        };
    }
}
