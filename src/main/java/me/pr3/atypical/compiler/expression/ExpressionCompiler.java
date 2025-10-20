package me.pr3.atypical.compiler.expression;

import me.pr3.atypical.compiler.MethodCompiler;
import me.pr3.atypical.compiler.StructureCompiler;
import me.pr3.atypical.compiler.util.ClassNodeUtil;
import me.pr3.atypical.compiler.util.TypeUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class ExpressionCompiler {
    StructureCompiler structureCompiler;
    MethodCompiler methodCompiler;

    public ExpressionCompiler(StructureCompiler structureCompiler, MethodCompiler methodCompiler) {
        this.structureCompiler = structureCompiler;
        this.methodCompiler = methodCompiler;
    }

    public Result compileExpression(ExpressionContext context) {
        String returnType = "V";
        String returnFieldName = null;

        if(context.CMPLT() != null ||context.CMPGT() != null || context.CMPNE() != null || context.CMPEQ() != null) {
            CmpExpressionCompiler cmpExpressionCompiler = new CmpExpressionCompiler(this);
            return cmpExpressionCompiler.compileCmpExpression(context);
        }

        if(context.ADD() != null || context.SUB() != null || context.MUL() != null || context.DIV() != null || context.MOD() != null || context.LOGIC_AND() != null || context.LOGIC_OR() != null) {
            ArithmeticExpressionCompiler arithmeticExpressionCompiler = new ArithmeticExpressionCompiler(this);
            return arithmeticExpressionCompiler.compileArithmeticExpression(context);
        }

        if(context.ASSIGN() != null){
            Result lhs = compilePostfixExpression(context.postfixExpression(), true);
            Result rhs = compileExpression(context.expression());
            InsnList insnList = new InsnList();
            insnList.add(lhs.insnList());
            insnList.add(rhs.insnList());
            if(lhs.sourceType == SourceType.LOCAL_VARIABLE){
                int localVarIndex = methodCompiler.getLocalVarIndexByName(lhs.memberName.orElseThrow());
                insnList.add(new VarInsnNode(
                        switch (rhs.returnType) {
                            case "I", "Z" -> Opcodes.ISTORE;
                            default -> Opcodes.ASTORE;
                        }, localVarIndex));
            }
            if(lhs.sourceType == SourceType.STRUCT_MEMBER){
                String memberName = lhs.memberName.orElseThrow();
                String typeName = TypeUtil.extractTypeNameFromDescriptor(lhs.returnType);
                FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(getClassNodeByName(typeName), memberName);
                insnList.add(new FieldInsnNode(
                        Opcodes.PUTFIELD,
                        typeName,
                        fieldNode.name,
                        fieldNode.desc
                ));
            }
            if(lhs.sourceType == SourceType.ARRAY){
                String arrayType = TypeUtil.getUnderlyingTypeOfArray(TypeUtil.extractTypeNameFromDescriptor(lhs.returnType));
                boolean primitive = TypeUtil.isPrimitiveType(arrayType);
                if (primitive) {
                    insnList.add(new InsnNode(Opcodes.IASTORE));
                } else {
                    insnList.add(new InsnNode(Opcodes.AASTORE));
                }
            }
            return new Result(insnList, rhs.returnType, Optional.empty(), SourceType.UNKNOWN);
        }

         return compilePostfixExpression(context.postfixExpression(), false);
    }

    public Result compilePostfixExpression(PostfixExpressionContext context, boolean isAssignLhs) {
        String returnType = "V";
        String returnFieldName = null;
        SourceType sourceType = SourceType.UNKNOWN;
        InsnList insnList = new InsnList();
        if(context.primary() != null){
            Result primaryResult = compilePrimary(context, isAssignLhs);
            insnList.add(primaryResult.insnList());
            returnType = primaryResult.returnType;
            sourceType = primaryResult.sourceType;
            returnFieldName = primaryResult.memberName.orElse(null);
        }

        List<PostfixOperatorContext> postfixOperator = context.postfixOperator();
        for (int i = 0; i < postfixOperator.size(); i++) {
            PostfixOperatorContext postfixOperatorContext = postfixOperator.get(i);
            if (postfixOperatorContext.DOT() != null) {
                MemberAccessContext memberAccessContext = postfixOperatorContext.memberAccess();
                if (memberAccessContext.memberName() != null) {
                    if (TypeUtil.isTypePrefixedDesc(returnType)) {
                        String typeDesc = TypeUtil.fromTypePrefixed(returnType);
                        String typeName = TypeUtil.extractTypeNameFromDescriptor(typeDesc);
                        FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(getClassNodeByName(typeName), memberAccessContext.memberName().getText());
                        //The last member access in the chain must not be done as it is just the field we want to assign to
                        if(i < postfixOperator.size() -1 || !isAssignLhs) {
                            insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, typeName, fieldNode.name, fieldNode.desc));
                            returnType = fieldNode.desc;
                        }
                        returnFieldName = fieldNode.name;
                        sourceType = SourceType.STATIC_STRUCT_MEMBER;
                    } else {
                        String typeName = TypeUtil.extractTypeNameFromDescriptor(returnType);
                        FieldNode fieldNode = ClassNodeUtil.getFieldNodeByName(getClassNodeByName(typeName), memberAccessContext.memberName().getText());
                        if(i < postfixOperator.size() -1 || !isAssignLhs) {
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, typeName, fieldNode.name, fieldNode.desc));
                            returnType = fieldNode.desc;
                        }
                        returnFieldName = fieldNode.name;
                        sourceType = SourceType.STRUCT_MEMBER;
                    }
                }
                if (memberAccessContext.methodInvocation() != null) {
                    MethodInvocationContext methodInvocationContext = memberAccessContext.methodInvocation();
                    String methodName = methodInvocationContext.memberName().getText();
                    StringBuilder desc = new StringBuilder();
                    if (methodInvocationContext.argList() != null) {
                        for (ExpressionContext argExpression : methodInvocationContext.argList().expression()) {
                            Result argExpressionResult = compileExpression(argExpression);
                            insnList.add(argExpressionResult.insnList());
                            desc.append(argExpressionResult.returnType);
                        }
                    }
                    String owner;
                    int opcode;
                    if (TypeUtil.isTypePrefixedDesc(returnType)) {
                        String typeDesc = TypeUtil.fromTypePrefixed(returnType);
                        owner = TypeUtil.extractTypeNameFromDescriptor(typeDesc);
                        opcode = Opcodes.INVOKESTATIC;
                    } else {
                        owner = TypeUtil.extractTypeNameFromDescriptor(returnType);
                        if(isTypeTrait(owner)){
                            opcode = Opcodes.INVOKEINTERFACE;
                        }
                        else {
                            opcode = Opcodes.INVOKEVIRTUAL;
                        }
                    }
                    ClassNode owningClassNode = getClassNodeByName(owner);
                    MethodNode invokedMethod = ClassNodeUtil.getMethodNodeByNameAndParameterTypes(
                            owningClassNode,
                            methodName,
                            desc.toString());
                    if (invokedMethod != null) {
                        insnList.add(new MethodInsnNode(opcode, owner, invokedMethod.name, invokedMethod.desc));
                        returnType = TypeUtil.getReturnType(invokedMethod.desc);
                        sourceType = SourceType.METHOD;
                    }else {
                        Result traitImplInvocationResult = getTraitImplMethodInvocation(methodName, desc, owner);
                        insnList.add(traitImplInvocationResult.insnList());
                        returnType = traitImplInvocationResult.returnType;
                        sourceType = SourceType.METHOD;
                        returnFieldName = null;
                    }
                }
            }
            if (postfixOperatorContext.arrayAccess() != null) {
                ArrayAccessContext arrayAccessContext = postfixOperatorContext.arrayAccess();
                Result indexExpressionResult = compileExpression(arrayAccessContext.expression());
                insnList.add(indexExpressionResult.insnList());
                String arrayType = TypeUtil.getUnderlyingTypeOfArray(TypeUtil.extractTypeNameFromDescriptor(returnType));
                boolean primitive = TypeUtil.isPrimitiveType(arrayType);
                if(i < postfixOperator.size() -1 || !isAssignLhs) {
                    if (primitive) {
                        insnList.add(new InsnNode(Opcodes.IALOAD));
                        returnType = "I";
                    } else {
                        insnList.add(new InsnNode(Opcodes.AALOAD));
                        returnType = arrayType;
                    }
                }
                sourceType = SourceType.ARRAY;
            }
        }
        if(isAssignLhs){
            int postFixOperatorSize = postfixOperator.size();
            if(postFixOperatorSize > 0){
                if(postfixOperator.get(postFixOperatorSize - 1).arrayAccess() != null){
                    sourceType = SourceType.ARRAY;
                }
                if(postfixOperator.get(postFixOperatorSize - 1).memberAccess() != null) {
                    returnFieldName = postfixOperator.get(postfixOperator.size() - 1).memberAccess().memberName().getText();
                }
            }
        }

        return new Result(insnList, returnType, Optional.ofNullable(returnFieldName), sourceType);
    }

    private Result compilePrimary(PostfixExpressionContext postfixExpressionContext, boolean isLhsAssignment) {
        PrimaryContext context = postfixExpressionContext.primary();
        boolean isJustLocalVarAssignment = postfixExpressionContext.postfixOperator().isEmpty() && isLhsAssignment;
        if(context.literal() != null){
            if(context.literal().NUMBER() != null){
                InsnList insnList = new InsnList();
                int value = Integer.parseInt(context.literal().NUMBER().getText());
                insnList.add(new IntInsnNode(Opcodes.BIPUSH, value));
                return new Result(insnList, "I", Optional.empty(), SourceType.LITERAL);
            }
            if(context.literal().STRING() != null){
                InsnList insnList = new InsnList();
                String value = context.literal().STRING().getText().replace("\"", "");
                insnList.add(new LdcInsnNode(value));
                return new Result(insnList, "Ljava/lang/String;", Optional.empty(), SourceType.LITERAL);
            }
            if(context.literal().NULL() != null){
                InsnList insnList = new InsnList();
                insnList.add(new InsnNode(Opcodes.ACONST_NULL));
                return new Result(insnList, "Ljava/lang/Object;", Optional.empty(), SourceType.LITERAL);
            }
        }
        if(context.memberOrVariableName() != null){
            String memberName = context.memberOrVariableName().getText();
            if(methodCompiler.structureCompiler.isClassNameImplClass(methodCompiler.className) && memberName.equals("this")) {
                //Special Case for the "this" keyword in impl as we need to capture this and use the "this_" variable instead
                InsnList insnList = new InsnList();
                String type = TypeUtil.toDesc(methodCompiler.fullyQualifyType(methodCompiler.className.split("\\$")[1]));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, methodCompiler.className, "this_", type));
                return new Result(insnList, type, Optional.of("this_"), SourceType.STATIC_STRUCT_MEMBER);
            } else if(methodCompiler.containsLocalVarWithName(memberName)){
                InsnList insnList = new InsnList();
                int localVarIndex = methodCompiler.getLocalVarIndexByName(memberName);
                String localVarType = methodCompiler.getLocalVarTypeByName(memberName);
                if(!isJustLocalVarAssignment) {
                    insnList.add(new VarInsnNode(
                            switch (localVarType) {
                                case "I", "Z" -> Opcodes.ILOAD;
                                default -> Opcodes.ALOAD;
                            }, localVarIndex));
                }
                return new Result(insnList, localVarType, Optional.ofNullable(memberName), SourceType.LOCAL_VARIABLE);
            }else if(isClassName(memberName)){
                InsnList insnList = new InsnList();
                String importMappedType = methodCompiler.fullyQualifyType(memberName);
                return new Result(insnList, TypeUtil.toTypePrefixed(importMappedType), Optional.empty(), SourceType.STATIC_STRUCT_MEMBER);
            } else {
                throw new IllegalArgumentException("Invalid member name: " + memberName + " at: " + postfixExpressionContext.getText());
            }
        }
        if(context.structInitializerExpression() != null){
            StructInitializerExpressionCompiler structInitializerExpressionCompiler = new StructInitializerExpressionCompiler(this);
            return structInitializerExpressionCompiler.compileStructInitializerExpression(context.structInitializerExpression());
        }
        if(context.castExpression() != null){
            CastExpressionCompiler castExpressionCompiler = new CastExpressionCompiler(this);
            return castExpressionCompiler.compileCastExpression(context.castExpression());
        }
        if(context.parenthesesExpression() != null){
            return compileExpression(context.parenthesesExpression().expression());
        }
        return null;
    }

    private Result getTraitImplMethodInvocation(String methodName, StringBuilder desc, String owner) {
        Set<String> implementedTraits = structureCompiler.implementedTraitsForStruct
                .getOrDefault(owner, new HashSet<>());
        InsnList insnList = new InsnList();
        for (String trait : implementedTraits) {
            ClassNode traitClass = this.structureCompiler.generatedClassNodes.get(trait);
            MethodNode invokedTraitMethod = ClassNodeUtil.getMethodNodeByNameAndParameterTypes(
                    traitClass,
                    methodName,
                    desc.toString());
            if(invokedTraitMethod != null){
                String implClassName =  traitClass.name + "$" + owner.replace("/", "_");
                String implClassConstructorDesc = "(" + TypeUtil.toDesc(owner) + ")V";
                insnList.add(new TypeInsnNode(Opcodes.NEW, implClassName));
                insnList.add(new InsnNode(Opcodes.DUP));
                insnList.add(new InsnNode(Opcodes.DUP2_X1));
                insnList.add(new InsnNode(Opcodes.POP2));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, implClassName, "<init>", implClassConstructorDesc));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, implClassName, invokedTraitMethod.name, invokedTraitMethod.desc));
                return new Result(insnList, Type.getMethodType(invokedTraitMethod.desc).getReturnType().toString(), Optional.empty(), SourceType.STATIC_STRUCT_MEMBER);
            }
        }
        return null;
    }

    private ClassNode getClassNodeByName(String typeName) {
        return structureCompiler.generatedClassNodes.getOrDefault(typeName, ClassNodeUtil.loadClassNodeFromJDKCLasses(typeName));
    }

    private boolean isClassName(String type){
        String importMappedType = methodCompiler.fullyQualifyType(type);
        return structureCompiler.generatedClassNodes.containsKey(importMappedType) || ClassNodeUtil.loadClassNodeFromJDKCLasses(importMappedType) != null;
    }

    private boolean isTypeTrait(String typeName){
        ClassNode classNode = structureCompiler.generatedClassNodes.get(typeName);
        if(classNode == null) classNode = ClassNodeUtil.loadClassNodeFromJDKCLasses(typeName);
        return Modifier.isInterface(classNode.access);
    }

    public enum SourceType {
        LOCAL_VARIABLE,
        STRUCT_MEMBER,
        STATIC_STRUCT_MEMBER,
        METHOD,
        LITERAL,
        ARRAY,
        UNKNOWN
    }

    public record Result(InsnList insnList, String returnType, Optional<String> memberName, SourceType sourceType) {

    }

}
