package me.pr3.atypical.compiler.util;

import java.util.Map;
import java.util.stream.Collectors;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class TypeUtil {
    public static String toDesc(String type) {
        return mapArrayType(type);
    }

    public static String toDesc(String type, Map<String, String> importMapping) {
        return mapArrayType(type, importMapping);
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

    private static String mapArrayType(String type, Map<String, String> importMapping){
        StringBuilder output = new StringBuilder();
        String temp  = type.replace(".", "/");
        while(temp.endsWith("[]")){
            temp = temp.substring(0, temp.length() - 2);
            output.append("[");
        }
        temp = importMapping.getOrDefault(temp, temp);
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


    public static String extractMethodDescriptor(MethodSignatureContext context, Map<String, String> importMapping) {
        String parameterTypes = context.parameterDeclaration().stream()
                .map(c -> c.typeName().getText())
                .map(t -> toDesc(t, importMapping))
                .collect(Collectors.joining());
        if(context.methodReturnTypeDeclaration() != null){
            String returnType = context.methodReturnTypeDeclaration().typeName().getText();
            String fullyQualifiedReturnType = importMapping.getOrDefault(returnType, returnType);
            return "(" + parameterTypes + ")" + mapTypeToJVMType(fullyQualifiedReturnType);
        }else {
            return "(" + parameterTypes + ")V";
        }
    }
}
