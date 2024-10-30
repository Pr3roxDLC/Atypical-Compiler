package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

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
        if(context.localVariableDeclarationExpression() != null){
            ExpressionCompiler compiler = new ExpressionCompiler(structureCompiler, methodCompiler);
            LocalVariableDeclarationExpressionContext lvde = context.localVariableDeclarationExpression();
            String localVarType = TypeUtil.toDesc(lvde.typeName().getText());
            insnList.add(compiler.compileExpression(lvde.expression()));
            int localVarIndex = methodCompiler.addLocalVar(localVarType);
            insnList.add(new VarInsnNode(Opcodes.ISTORE, localVarIndex));
        }
        return insnList;
    }
}
