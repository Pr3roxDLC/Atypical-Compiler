package me.pr3.atypical.compiler;

import me.pr3.atypical.compiler.util.TypeUtil;
import me.pr3.atypical.generated.AtypicalLexer;
import me.pr3.atypical.generated.AtypicalParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class StructureCompiler {

    //Input
    public Map<String, String> inputFiles;

    //Intermediate
    public Map<String, Set<ModuleDeclarationContext>> modules = new HashMap<>();
    public Map<String, Set<TraitDeclarationContext>> traits = new HashMap<>();
    public Map<String, Set<ImplDeclarationContext>> impls = new HashMap<>();
    public Map<String, Set<StructDeclarationContext>> structs = new HashMap<>();
    public Map<String, List<MethodImplementationContext>> globalMethods = new HashMap<>();


    //Map<File, Map<ImportedClass, Alias>>
    public Map<String, Map<String, String>> imports = new HashMap<>();

    public Map<String, Set<String>> implementedTraitsForStruct = new HashMap<>();

    //Output
    public Map<String, ClassNode> generatedClassNodes = new HashMap<>();

    public Map<String, byte[]> generatedClasses = new HashMap<>();

    public StructureCompiler(Map<String, String> files) {
        this.inputFiles = files;
    }

    public Map<String, byte[]> compile() {

        //Generate Java Class Structure
        for (Entry<String, String> inputFile : inputFiles.entrySet()) {
            parseSingleFile(inputFile.getKey(), inputFile.getValue());
        }

        for (Entry<String, Set<ModuleDeclarationContext>> entry : modules.entrySet()) {
            for (ModuleDeclarationContext module : entry.getValue()) {
                generateClassNodeFromModule(module, entry.getKey());
            }
        }

        for (Entry<String, Set<TraitDeclarationContext>> entry : traits.entrySet()) {
            for (TraitDeclarationContext trait : entry.getValue()) {
                generateInterfaceFromTrait(trait, entry.getKey());
            }
        }

        for (Entry<String, Set<ImplDeclarationContext>> entry : impls.entrySet()) {
            for (ImplDeclarationContext impl : entry.getValue()) {
                generateClassFromImpl(impl, entry.getKey());
            }
        }

        for (Entry<String, List<MethodImplementationContext>> globalMethodsForFile : globalMethods.entrySet()) {
            for (MethodImplementationContext methodImplementationContext : globalMethodsForFile.getValue()) {
                generateOrAddToClassForGlobalMethod(globalMethodsForFile.getKey(), methodImplementationContext);
            }
        }

        for (Entry<String, Set<StructDeclarationContext>> structsInFile : structs.entrySet()) {
            for (StructDeclarationContext struct : structsInFile.getValue()) {
                generateClassFromStruct(struct, structsInFile.getKey());
            }
        }

        for (Entry<String, Set<ModuleDeclarationContext>> entry : modules.entrySet()) {
            for (ModuleDeclarationContext moduleDeclarationContext : entry.getValue()) {
                String moduleName = moduleDeclarationContext.typeName().getText();
                for (ModuleMemberDeclarationContext moduleMemberDeclaration : moduleDeclarationContext.moduleMemberDeclaration()) {
                    if(moduleMemberDeclaration.moduleSelfImplDeclaration() != null){
                        ModuleSelfImplDeclarationContext moduleSelfImplDeclaration = moduleMemberDeclaration.moduleSelfImplDeclaration();
                        for (ImplMemberDeclarationContext implMember : moduleSelfImplDeclaration.implMemberDeclaration()) {
                            MethodCompiler methodCompiler = new MethodCompiler(this);
                            methodCompiler.compileMethod(entry.getKey(), implMember.methodImplementation(), moduleName);
                        }
                    }
                    if(moduleMemberDeclaration.moduleStructDeclaration() != null){
                        StructInitializerCompiler structInitializerCompiler = new StructInitializerCompiler(this);
                        structInitializerCompiler.compileStructInitializer(moduleMemberDeclaration.moduleStructDeclaration(), entry.getKey(), moduleName);
                    }
                }
            }
        }

        for (Entry<String, Set<ImplDeclarationContext>> entry : impls.entrySet()) {
            String fileName = entry.getKey();
            for (ImplDeclarationContext impl : entry.getValue()) {
                String implementedTraitName = imports.get(fileName).getOrDefault(impl.itf.getText(), impl.itf.getText());
                String structName = imports.get(fileName).getOrDefault(impl.struct.getText(), impl.struct.getText());
                String implClassName = implementedTraitName +  "$" + structName.replace("/", "_");
                for (ImplMemberDeclarationContext member : impl.implMemberDeclaration()) {
                    MethodCompiler methodCompiler = new MethodCompiler(this);
                    methodCompiler.compileMethod(fileName, member.methodImplementation(), implClassName);
                }
                ImplConstructorCompiler implConstructorCompiler = new ImplConstructorCompiler(this);
                implConstructorCompiler.compileImplConstructor(impl, implClassName, fileName);
            }
        }

        for (Entry<String, List<MethodImplementationContext>> entry : globalMethods.entrySet()) {
            for (MethodImplementationContext methodImplementationContext : entry.getValue()) {
                MethodCompiler methodCompiler = new MethodCompiler(this);
                methodCompiler.compileMethod(entry.getKey(), methodImplementationContext);
            }
        }

        for (Map.Entry<String, ClassNode> entry : generatedClassNodes.entrySet()) {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            entry.getValue().accept(classWriter);
            byte[] output = classWriter.toByteArray();

            generatedClasses.put(entry.getKey(), output);
        }

        return generatedClasses;
    }

    private void generateClassFromStruct(StructDeclarationContext struct, String key) {
        List<FieldNode> structMembers = new ArrayList<>();
        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.version = Opcodes.V17;
        classNode.name = struct.typeName().getText();
        classNode.methods = new ArrayList<>();
        classNode.superName= "java/lang/Object";
        generatedClassNodes.put(struct.typeName().getText(), classNode);
        for (StructMemberDeclarationContext structMemberDeclarationContext : struct.structMemberDeclaration()) {
            structMembers.add(new FieldNode(Opcodes.ACC_PUBLIC,
                    structMemberDeclarationContext.memberName().getText(),
                    TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText(), imports.get(key)), null, null));
        }
        classNode.fields = structMembers;
        //Add the synthetic constructor used by the struct initializer expression
        classNode.methods.add(generateStructInitializerConstructor(structMembers));

        StructInitializerCompiler structInitializerCompiler = new StructInitializerCompiler(this);
        structInitializerCompiler.compileStructInitializer(struct, key, struct.typeName().getText());

    }

    private void generateOrAddToClassForGlobalMethod(String className, MethodImplementationContext method) {
        String trimmed = className.replace(".atp", "");
        ClassNode classNode = null;
        if(generatedClassNodes.containsKey(trimmed)){
            classNode = generatedClassNodes.get(trimmed);
        }else {
            classNode = new ClassNode();
            classNode.access = Opcodes.ACC_PUBLIC;
            classNode.version = Opcodes.V17;
            classNode.name = trimmed;
            classNode.methods = new ArrayList<>();
            classNode.superName= "java/lang/Object";
            generatedClassNodes.put(trimmed, classNode);
        }
        MethodNode methodNode = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.methodSignature().memberName().getText(),
                TypeUtil.extractMethodDescriptor(method.methodSignature(), imports.get(className)),
                null,
                new String[]{}
        );
        classNode.methods.add(methodNode);

    }

    private void generateClassFromImpl(ImplDeclarationContext context, String fileName){
        String className = imports.get(fileName).getOrDefault(context.itf.getText(), context.itf.getText())
                + "$" + imports.get(fileName).getOrDefault(context.struct.getText(), context.struct.getText()).replace("/", "_");

        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.version = Opcodes.V17;
        classNode.name = className;
        classNode.methods = new ArrayList<>();
        classNode.superName = "java/lang/Object";
        classNode.interfaces = List.of(imports.get(fileName).getOrDefault(context.itf.getText(), context.itf.getText()));

        for (ImplMemberDeclarationContext member : context.implMemberDeclaration()) {
            MethodSignatureContext signature = member.methodImplementation().methodSignature();
            String desc = TypeUtil.extractMethodDescriptor(signature, imports.get(fileName));
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, signature.memberName().getText(), desc, null, null);
            classNode.methods.add(methodNode);
        }
        String structType = TypeUtil.toDesc(context.struct.getText(), imports.get(fileName));
        String constructorDesc = "(" + structType + ")V";
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", constructorDesc, null, new String[0]);
        classNode.methods.add(constructor);

        FieldNode thisField = new FieldNode(Opcodes.ACC_PUBLIC, "this_", structType, null, null);
        classNode.fields = List.of(thisField);

        generatedClassNodes.put(className, classNode);
    }

    private void generateInterfaceFromTrait(TraitDeclarationContext context, String fileName){
        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
        classNode.version = Opcodes.V17;
        classNode.name = context.typeName().getText();
        classNode.methods = new ArrayList<>();
        classNode.superName = "java/lang/Object";

        for (TraitMemberDeclarationContext member : context.traitMemberDeclaration()) {
            MethodSignatureContext signature = member.methodDeclaration().methodSignature();
            String desc = TypeUtil.extractMethodDescriptor(signature, imports.get(fileName));
            classNode.methods.add(new MethodNode(Opcodes.ACC_ABSTRACT | Opcodes.ACC_PUBLIC, signature.memberName().getText(), desc, null, null));
        }

        generatedClassNodes.put(context.typeName().getText(), classNode);
    }

    private void generateClassNodeFromModule(ModuleDeclarationContext moduleDeclarationContext, String fileName) {
        String typeName = moduleDeclarationContext.typeName().getText();

        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.version = Opcodes.V17;
        classNode.name = typeName;
        classNode.superName = "java/lang/Object";
        classNode.methods = new ArrayList<>();

        for (ModuleMemberDeclarationContext member : moduleDeclarationContext.moduleMemberDeclaration()) {
            if (member.moduleStructDeclaration() != null) {
                List<FieldNode> structMembers = new ArrayList<>();
                ModuleStructDeclarationContext struct = member.moduleStructDeclaration();
                for (StructMemberDeclarationContext structMemberDeclarationContext : struct.structMemberDeclaration()) {
                    structMembers.add(new FieldNode(Opcodes.ACC_PUBLIC,
                            structMemberDeclarationContext.memberName().getText(),
                            TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText(), imports.get(fileName)), null, null));
                }
                classNode.fields = structMembers;
                //Add the synthetic constructor used by the struct initializer expression
                classNode.methods.add(generateStructInitializerConstructor(structMembers));
            }

            if (member.moduleSelfImplDeclaration() != null) {
                ModuleSelfImplDeclarationContext impl = member.moduleSelfImplDeclaration();
                for (ImplMemberDeclarationContext implMember : impl.implMemberDeclaration()) {
                    MethodImplementationContext method = implMember.methodImplementation();
                    MethodNode methodNode = new MethodNode(
                            Opcodes.ACC_PUBLIC,
                            method.methodSignature().memberName().getText(),
                            TypeUtil.extractMethodDescriptor(method.methodSignature(), imports.get(fileName)),
                            null,
                            new String[]{}
                    );
                    classNode.methods.add(methodNode);
                }

            }

        }

        generatedClassNodes.put(typeName, classNode);

    }

    private MethodNode generateStructInitializerConstructor(List<FieldNode> fields) {
        String args = fields.stream().map(field -> field.desc).collect(Collectors.joining()) + "Ljava/lang/Void;";
        String desc = "(" + args + ")V";
        return new MethodNode(Opcodes.ACC_PUBLIC, "<init>", desc, null, new String[0]);
    }

    private void parseSingleFile(String fileName, String content) {
        AtypicalLexer lexer = new AtypicalLexer(CharStreams.fromString(content));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AtypicalParser parser = new AtypicalParser(tokens);
        FileContext fileContext = parser.file();
        imports.put(fileName, new HashMap<>());
        for (ImportedClassContext importedClass : fileContext.imports().importedClass()) {
            if(!imports.containsKey(fileName)) {
                imports.put(fileName, new HashMap<>(Map.of(importedClass.alias.getText(), importedClass.class_.getText().replace(".", "/"))));
            }else {
                imports.get(fileName).put(importedClass.alias.getText(), importedClass.class_.getText().replace(".", "/"));
            }
        }
        this.modules.put(fileName, new HashSet<>());
        this.traits.put(fileName, new HashSet<>());
        this.impls.put(fileName, new HashSet<>());
        this.structs.put(fileName, new HashSet<>());
        for (FileMemberContext fileMemberContext : fileContext.fileMember()) {
            if(fileMemberContext.moduleDeclaration() != null)this.modules.get(fileName).add(fileMemberContext.moduleDeclaration());
            if(fileMemberContext.traitDeclaration() != null)this.traits.get(fileName).add(fileMemberContext.traitDeclaration());
            if(fileMemberContext.implDeclaration() != null) {
                ImplDeclarationContext implDeclarationContext = fileMemberContext.implDeclaration();
                String structTypeName = imports.get(fileName).getOrDefault(implDeclarationContext.struct.getText(),
                        implDeclarationContext.struct.getText());
                String traitTypeName = imports.get(fileName).getOrDefault(implDeclarationContext.itf.getText(),
                        implDeclarationContext.itf.getText());
                implementedTraitsForStruct.computeIfAbsent(structTypeName, (s) -> new HashSet<>()).add(traitTypeName);

                this.impls.get(fileName).add(fileMemberContext.implDeclaration());
            }
            if(fileMemberContext.structDeclaration() != null) {
                structs.get(fileName).add(fileMemberContext.structDeclaration());
            }

            if(fileMemberContext.methodImplementation() != null) {
                if(globalMethods.containsKey(fileName)){
                    this.globalMethods.get(fileName).add(fileMemberContext.methodImplementation());
                }else {
                    this.globalMethods.put(fileName, new ArrayList<>(List.of(fileMemberContext.methodImplementation())));
                }
            }
        }

    }

    public boolean isClassNameImplClass(String className){
        for (Entry<String, Set<ImplDeclarationContext>> entry : impls.entrySet()) {
            for (ImplDeclarationContext implDeclarationContext : entry.getValue()) {
                String structTypeName = imports.get(entry.getKey()).getOrDefault(implDeclarationContext.struct.getText(),
                        implDeclarationContext.struct.getText());
                String traitTypeName = imports.get(entry.getKey()).getOrDefault(implDeclarationContext.itf.getText(),
                        implDeclarationContext.itf.getText());
                String implClassName = traitTypeName + "$" + structTypeName.replace("/", "_");
                if(implClassName.equals(className))return true;
            }
        }
        return false;
    }

}
