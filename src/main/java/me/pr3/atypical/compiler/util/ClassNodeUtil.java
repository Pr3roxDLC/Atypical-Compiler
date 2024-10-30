package me.pr3.atypical.compiler.util;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

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
}
