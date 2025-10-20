package me.pr3.atypical.compiler.typing;

import java.util.List;
import java.util.Objects;

public class Type {

    public static final Type VOID = new Type("V");
    public static final Type INT = new Type("I");
    public static final Type FLOAT = new Type("F");
    public static final Type DOUBLE = new Type("D");
    public static final Type LONG = new Type("J");
    public static final Type BOOLEAN = new Type("Z");
    public static final Type BYTE = new Type("B");
    public static final Type CHAR = new Type("C");
    public static final Type SHORT = new Type("S");
    public static final Type UNKNOWN = new Type("U");
    public static final Type INVALID = new Type(null, null);

    public enum Kind {
        VOID(Type.VOID),
        INT(Type.INT),
        FLOAT(Type.FLOAT),
        DOUBLE(Type.DOUBLE),
        LONG(Type.LONG),
        BOOLEAN(Type.BOOLEAN),
        BYTE(Type.BYTE),
        CHAR(Type.CHAR),
        SHORT(Type.SHORT),
        UNKNOWN(Type.UNKNOWN),
        OBJECT(null);

        private final Type type;

        Kind(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }

    String desc;
    List<Type> genericTypes;
    Kind kind;

    public boolean isGenericType() {
        return genericTypes != null && !genericTypes.isEmpty();
    }

    public List<Type> getGenericTypes() {
        return genericTypes;
    }

    public boolean isStaticType() {
        return desc != null && desc.startsWith("T");
    }

    public boolean isUnknownType() {
        return "U".equals(desc);
    }

    public boolean isArrayType() {
        return desc != null && desc.startsWith("[");
    }

    public boolean isPrimitiveType() {
        if (desc == null || desc.isEmpty()) return false;
        switch (desc.charAt(0)) {
            case 'I', 'B', 'C', 'D', 'F', 'J', 'S', 'Z', 'V' -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public Type getTypeFromStaticType() {
        if (!isStaticType()) {
            throw new IllegalStateException("Type is not a static type");
        }
        return new Type(desc.substring(1), genericTypes);
    }

    public String getInternalName() {
        if (this == INVALID || desc == null) {
            throw new IllegalStateException("Cannot get internal name of invalid type");
        }

        if (isArrayType()) {
            return desc;
        }

        if (isPrimitiveType()) {
            return desc;
        }

        // Object type
        if (desc.startsWith("L") && desc.endsWith(";")) {
            return desc.substring(1, desc.length() - 1);
        }

        throw new IllegalStateException("Invalid object type descriptor: " + desc);
    }

    public Type getUnderlyingArrayType() {
        return new Type(desc.substring(1), genericTypes);
    }

    public Type toStaticType() {
        if (isStaticType()) {
            throw new IllegalArgumentException("Type is already a static type");
        }
        return new Type("T" + desc, genericTypes);
    }

    /**
     * Returns the JVM slot size of this type:
     * - long/double = 2
     * - void = 0
     * - all other valid types = 1
     */
    public int getSize() {
        if (this == INVALID || desc == null) {
            throw new IllegalStateException("Cannot get size of invalid type");
        }

        switch (kind) {
            case VOID -> { return 0; }
            case LONG, DOUBLE -> { return 2; }
            default -> { return 1; }
        }
    }

    /**
     * Infers the Kind enum from the descriptor string.
     */
    private static Kind inferKind(String desc) {
        if (desc == null) return Kind.UNKNOWN;
        return switch (desc.charAt(0)) {
            case 'V' -> Kind.VOID;
            case 'I' -> Kind.INT;
            case 'F' -> Kind.FLOAT;
            case 'D' -> Kind.DOUBLE;
            case 'J' -> Kind.LONG;
            case 'Z' -> Kind.BOOLEAN;
            case 'B' -> Kind.BYTE;
            case 'C' -> Kind.CHAR;
            case 'S' -> Kind.SHORT;
            case 'U' -> Kind.UNKNOWN;
            default -> Kind.OBJECT; // arrays, objects, generics, etc.
        };
    }

    @Override
    public String toString() {
        return desc;
    }

    public Type(String desc) {
        this.desc = desc;
        this.kind = inferKind(desc);
    }

    public Type(String desc, List<Type> genericTypes) {
        this.desc = desc;
        this.genericTypes = genericTypes;
        this.kind = inferKind(desc);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Type type = (Type) o;
        return Objects.equals(desc, type.desc) && Objects.equals(genericTypes, type.genericTypes) && kind == type.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(desc, genericTypes, kind);
    }

    public Kind getKind() {
        return kind;
    }

    public static Type fromDescriptor(String descriptor) {
        return new Type(descriptor);
    }

    public static Type fromInternalName(String internalName) {
        return new Type("L" + internalName + ";");
    }
}
