package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.MethodCompiler;
import me.pr3.atypical.compiler.StructureCompiler;
import me.pr3.atypical.compiler.expression.ExpressionCompiler.Result;
import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalParser;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;

/**
 * @author tim
 */
public class CastExpressionCompiler {

    private final ExpressionCompiler expressionCompiler;
    private final StructureCompiler structureCompiler;
    private final MethodCompiler methodCompiler;

    public CastExpressionCompiler(ExpressionCompiler expressionCompiler){
        this.expressionCompiler = expressionCompiler;
        structureCompiler = expressionCompiler.structureCompiler;
        methodCompiler = expressionCompiler.methodCompiler;
    }

    public Result compileCastExpression(AtypicalParser.CastExpressionContext context){
        InsnList insnList = new InsnList();
        String resultType = "V";
        String castTypeName = context.typeName().getText();
        String fullyQualifiedTypeName = TypeUtil.extractTypeNameFromDescriptor(TypeUtil.toDesc(castTypeName, structureCompiler.imports.get(methodCompiler.fileName)));
        Result expressionResult = expressionCompiler.compileExpression(context.expression());
        insnList.add(expressionResult.insnList());
        if(!isTypeTrait(fullyQualifiedTypeName)) {
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, fullyQualifiedTypeName));

        }else {
            //Generates the following java code:
            /*
            (Cloneable)Class.forName(x.getClass().getName().replace(".", "/") + "$Cloneable").getDeclaredConstructor(x.getClass()).newInstance(x);
             */
            //for the simple atp code: (Cloneable)x
            //This is required for trait implementations to ensure that we get the correct implementation for our struct.
            //We do this by first getting the Name of the Trait implementation class from the class name of the object
            //we want to cast, then from that the implementation class, on which we then use reflection to invoke the
            //ImplConstructor. We then can invoke methods on the Trait type as all Trait Implementations implement
            //their trait as an interface.
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuffer"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "()V"));
            insnList.add(new InsnNode(Opcodes.SWAP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;"));
            insnList.add(new LdcInsnNode("."));
            insnList.add(new LdcInsnNode("/"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "replace", "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;"));
            insnList.add(new LdcInsnNode("$" + fullyQualifiedTypeName.replace("/", ".")));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;"));
            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;"));
            insnList.add(new InsnNode(Opcodes.SWAP));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;"));
            insnList.add(new InsnNode(Opcodes.ICONST_1));
            insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new InsnNode(Opcodes.DUP2_X1));
            insnList.add(new InsnNode(Opcodes.POP2));
            insnList.add(new InsnNode(Opcodes.ICONST_0));
            insnList.add(new InsnNode(Opcodes.SWAP));
            insnList.add(new InsnNode(Opcodes.AASTORE));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;"));
            insnList.add(new InsnNode(Opcodes.SWAP));
            insnList.add(new InsnNode(Opcodes.ICONST_1));
            insnList.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
            insnList.add(new InsnNode(Opcodes.DUP));
            insnList.add(new InsnNode(Opcodes.DUP2_X1));
            insnList.add(new InsnNode(Opcodes.POP2));
            insnList.add(new InsnNode(Opcodes.ICONST_0));
            insnList.add(new InsnNode(Opcodes.SWAP));
            insnList.add(new InsnNode(Opcodes.AASTORE));
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;"));
            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, fullyQualifiedTypeName));
        }
        resultType = TypeUtil.toDesc(fullyQualifiedTypeName);
        return new Result(insnList, resultType);
    }

    private boolean isTypeTrait(String typeName){
        ClassNode classNode = structureCompiler.generatedClassNodes.get(typeName);
        if(classNode == null) classNode = ClassNodeUtil.loadClassNodeFromJDKCLasses(typeName);
        return Modifier.isInterface(classNode.access);
    }

}
