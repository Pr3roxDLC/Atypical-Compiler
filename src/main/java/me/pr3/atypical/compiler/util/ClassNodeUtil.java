package me.pr3.atypical.compiler.util;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Arrays;

/**
 * @author tim
 */
public class ClassNodeUtil {
    public static MethodNode getNodeByNameAndDescriptor(ClassNode classNode, String name, String desc){
        for (MethodNode method : classNode.methods) {
            if(method.name.equals(name) && method.desc.equals(desc))return method;
        }
        throw new IllegalArgumentException("No method found for name: " + name + " and desc: " + desc);
    }

    public static MethodNode getNodeByNameAndParameterTypes(ClassNode classNode, String name, String parameterTypes){
        for (MethodNode method : classNode.methods) {
            if(method.name.equals(name)){
                String tempDesc = "(" + parameterTypes + ")V";
                Type tempMethodDesc = Type.getMethodType(tempDesc);
                Type methodDesc = Type.getMethodType(method.desc);
                if(Arrays.equals(tempMethodDesc.getArgumentTypes(), methodDesc.getArgumentTypes()))return method;
            }
        }
        throw new IllegalArgumentException("No method found for name: " + name + " and parameters: " + parameterTypes);
    }

}
