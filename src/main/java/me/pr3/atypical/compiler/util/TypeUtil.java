package me.pr3.atypical.compiler.util;

import org.objectweb.asm.Type;

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

    public static String extractTypeNameFromDescriptor(String desc){
        if(desc.startsWith("L") && desc.endsWith(";")){
            return desc.substring(1).replace(";", "");
        }
        //TODO make this work for arrays as well
        return desc;
    }

    public static int getArrayTypeDims(String arrayType){
        String temp  = arrayType;
        int dims = 0;
        while(temp.startsWith("[")){
            temp = temp.substring(1);
            dims++;
        }
        return dims;
    }

    public static String getUnderlyingTypeOfArray(String arrayType){
        String temp  = arrayType;
        while(temp.startsWith("[")){
            temp = temp.substring(1);
        }
        return temp;
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

    public static boolean isArrayType(String type){
        return type.startsWith("[");
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
        String parameterTypes = "";

        if(context.parameterList()!= null) {
            parameterTypes = context.parameterList().parameterDeclaration().stream()
                    .map(c -> c.typeName().getText())
                    .map(t -> toDesc(t, importMapping))
                    .collect(Collectors.joining());
        }
        if(context.methodReturnTypeDeclaration() != null){
            String returnType = context.methodReturnTypeDeclaration().typeName().getText();
            String fullyQualifiedReturnType = importMapping.getOrDefault(returnType, returnType);
            return "(" + parameterTypes + ")" + mapTypeToJVMType(fullyQualifiedReturnType);
        }else {
            return "(" + parameterTypes + ")V";
        }
    }

    public static String getReturnType(String methodDesc){
        return methodDesc.split("\\)")[1];
    }

    public static boolean isPrimitiveType(String type) {
        return !type.startsWith("[") && !type.startsWith("L");
    }
}
