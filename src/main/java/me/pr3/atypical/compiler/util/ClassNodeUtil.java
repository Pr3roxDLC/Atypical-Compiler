package me.pr3.atypical.compiler.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author tim
 */
public class ClassNodeUtil {
    public static MethodNode getMethodNodeByNameAndDescriptor(ClassNode classNode, String name, String desc){
        for (MethodNode method : classNode.methods) {
            if(method.name.equals(name) && method.desc.equals(desc))return method;
        }
        return null;
    }

    public static MethodNode getMethodNodeByNameAndParameterTypes(ClassNode classNode, String name, List<String> parameterTypes){
        for (MethodNode method : classNode.methods) {
            if(method.name.equals(name)){
                Type methodDesc = Type.getMethodType(method.desc);
                List<String> methodParamTypeList = Arrays.stream(methodDesc.getArgumentTypes())
                        .map(Type::getDescriptor)
                        .toList();
                boolean allMatch = true;
                if(methodParamTypeList.size() != parameterTypes.size()){continue;}
                for (int i = 0; i < parameterTypes.size(); i++) {
                    String parameterType = parameterTypes.get(i);
                    if (!parameterType.equals(methodParamTypeList.get(i))) {
                        if (!parameterType.equals("U")) {
                            allMatch = false;
                        }
                    }
                }
                if(allMatch){
                    return method;
                }
            }
        }
        return null;
    }

    public static FieldNode getFieldNodeByName(ClassNode classNode, String name){
        for (FieldNode fieldNode : classNode.fields) {
            if(fieldNode.name.equals(name) )return fieldNode;
        }
        throw new IllegalArgumentException("No field found for name: " + name);
    }

    public static ClassNode loadClassNodeFromJDKCLasses(String className)  {
        // Load the class bytes using the system class loader
        byte[] classBytes = new byte[0];
        try {
            classBytes = getClassBytes(className);
        } catch (IOException e) {
            return null;
        }

        // Create a ClassNode and ClassReader to read the class bytes
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);

        // Populate the ClassNode with the class data
        classReader.accept(classNode, 0);

        return classNode;
    }

    private static byte[] getClassBytes(String className) throws IOException {
        // Convert the class name to a resource path
        String resourcePath = className.replace('.', '/') + ".class";

        // Load the class bytes from the classpath
        try (var inputStream = ClassLoader.getSystemResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Class not found: " + className);
            }

            return inputStream.readAllBytes();
        }
    }

}
