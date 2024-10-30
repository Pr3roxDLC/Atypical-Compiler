package me.pr3.atypical.compiler.util;

import java.util.stream.Collectors;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class TypeUtil {
    public static String toDesc(String type) {
        return mapTypeToJVMType(type);
    }

    private static String mapTypeToJVMType(String type){
        return switch (type){
            case "int" -> "I";
            case "long" -> "J";
            case "obj" -> "Ljava/lang/Object;";
            default -> "L" + type + ";";
        };
    }


    public static String extractMethodDescriptor(MethodSignatureContext context) {
        String parameterTypes = context.parameterDeclaration().stream()
                .map(c -> c.typeName().getText())
                .map(TypeUtil::toDesc)
                .collect(Collectors.joining());
        if(context.methodReturnTypeDeclaration() != null){
            return "(" + parameterTypes + ")" + mapTypeToJVMType(context.methodReturnTypeDeclaration().typeName().getText());
        }else {
            return "(" + parameterTypes + ")V";
        }
    }
}
