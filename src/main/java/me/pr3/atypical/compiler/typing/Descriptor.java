package me.pr3.atypical.compiler.typing;

import java.util.ArrayList;
import java.util.List;

public class Descriptor {
    List<Type> parameters = new ArrayList<>();
    Type returnType = Type.VOID;

    public Descriptor(List<Type> parameters, Type returnType) {
        this.parameters = parameters;
        this.returnType = returnType;
    }

    public Descriptor(String desc) {
        int idx = 0;
        if (desc.charAt(idx) != '(') {
            throw new IllegalArgumentException("Invalid descriptor: missing '('");
        }
        idx++;

        // parse parameter types
        while (desc.charAt(idx) != ')') {
            TypeAndIndex result = parseType(desc, idx);
            parameters.add(result.type);
            idx = result.nextIndex;
        }

        // skip ')'
        idx++;

        // parse return type
        TypeAndIndex ret = parseType(desc, idx);
        returnType = ret.type;
    }

    private static class TypeAndIndex {
        Type type;
        int nextIndex;

        TypeAndIndex(Type type, int nextIndex) {
            this.type = type;
            this.nextIndex = nextIndex;
        }
    }

    /**
     * Parses a type descriptor starting at the given index and returns both
     * the parsed Type and the next index to continue parsing from.
     */
    private static TypeAndIndex parseType(String desc, int index) {
        char c = desc.charAt(index);
        switch (c) {
            case 'B': return new TypeAndIndex(Type.BYTE, index + 1);
            case 'C': return new TypeAndIndex(Type.CHAR, index + 1);
            case 'D': return new TypeAndIndex(Type.DOUBLE, index + 1);
            case 'F': return new TypeAndIndex(Type.FLOAT, index + 1);
            case 'I': return new TypeAndIndex(Type.INT, index + 1);
            case 'J': return new TypeAndIndex(Type.LONG, index + 1);
            case 'S': return new TypeAndIndex(Type.SHORT, index + 1);
            case 'Z': return new TypeAndIndex(Type.BOOLEAN, index + 1);
            case 'V': return new TypeAndIndex(Type.VOID, index + 1);

            // Object type: L<classname>;
            case 'L': {
                int semicolon = desc.indexOf(';', index);
                if (semicolon == -1) {
                    throw new IllegalArgumentException("Invalid object type descriptor: " + desc.substring(index));
                }
                String internalName = desc.substring(index, semicolon + 1);
                return new TypeAndIndex(new Type(internalName), semicolon + 1);
            }

            // Array type: [<componentType>
            case '[': {
                TypeAndIndex component = parseType(desc, index + 1);
                // Keep array notation exact, e.g., "[I", "[[Ljava/lang/String;"
                String arrayDesc = desc.substring(index, component.nextIndex);
                return new TypeAndIndex(new Type(arrayDesc), component.nextIndex);
            }

            // Type variables (e.g. generics placeholders) or unknowns
            case 'T': { // Static or generic type variable like "TT;"
                int semicolon = desc.indexOf(';', index);
                if (semicolon == -1) {
                    throw new IllegalArgumentException("Invalid generic/static type descriptor: " + desc.substring(index));
                }
                String name = desc.substring(index, semicolon + 1);
                return new TypeAndIndex(new Type(name), semicolon + 1);
            }

            case 'U': { // unknown type (your extension)
                return new TypeAndIndex(new Type("U"), index + 1);
            }

            default:
                throw new IllegalArgumentException("Unknown type in descriptor at index " + index + ": " + c);
        }
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return "Descriptor(" + parameters + " -> " + returnType + ")";
    }

    public static Descriptor fromString(String desc) {
        return new Descriptor(desc);
    }
}
