package me.pr3.atypical.compiler.util;

import java.util.stream.Collectors;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class TypeUtil {
    public static String toDesc(String type) {
        return mapArrayType(type);
    }

    public static String extractClassFromType(String type){
        if(!type.startsWith("L") || !type.endsWith(";")){
            throw new IllegalArgumentException("Unable to extract class from type: " + type);
        }
        return type.substring(1).replace(";", "");
    }

    public static String toTypePrefixed(String desc){
        return "T" + desc;
    }

    public static boolean isTypePrefixedDesc(String desc){
        return desc.startsWith("T");
    }

    public static String fromTypePrefixed(String typePrefixedDesc){
        if(typePrefixedDesc.startsWith("T")){
            return typePrefixedDesc.substring(1);
        }
        throw new IllegalArgumentException("Type: " + typePrefixedDesc + "no prefixed with T indicating this being a type placeholder");
    }

    private static String mapArrayType(String type){
        StringBuilder output = new StringBuilder();
        String temp  = type.replace(".", "/");
        while(temp.endsWith("[]")){
            temp = temp.substring(0, temp.length() - 2);
            output.append("[");
        }
        return output.append(mapTypeToJVMType(temp)).toString();
    }

    private static String mapTypeToJVMType(String type){
        return switch (type){
            case "int" -> "I";
            case "long" -> "J";
            case "obj" -> "Ljava/lang/Object;";
            case "bool" -> "Z";
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
