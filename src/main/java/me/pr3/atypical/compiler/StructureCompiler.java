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

import static me.pr3.atypical.generated.AtypicalParser.*;

/**
 * @author tim
 */
public class StructureCompiler {

    //Input
    Map<String, String> inputFiles;

    //Intermediate
    Set<ModuleDeclarationContext> modules = new HashSet<>();
    Set<TraitDeclarationContext> traits = new HashSet<>();
    Set<ImplDeclarationContext> impls = new HashSet<>();
    Map<String, MethodImplementationContext> globalMethods = new HashMap<>();

    //Output
    Map<String, ClassNode> generatedClassNodes = new HashMap<>();

    Map<String, byte[]> generatedClasses = new HashMap<>();

    public StructureCompiler(Map<String, String> files) {
        this.inputFiles = files;
    }

    public Map<String, byte[]> compile() {

        //Generate Java Class Structure
        for (Entry<String, String> inputFile : inputFiles.entrySet()) {
            parseSingleFile(inputFile.getKey(), inputFile.getValue());
        }

        for (ModuleDeclarationContext module : modules) {
            generateClassNodeFromModule(module);
        }

        for (TraitDeclarationContext trait : traits) {
            generateInterfaceFromTrait(trait);
        }

        for (ImplDeclarationContext impl : impls) {
            generateClassFromImpl(impl);
        }

        for (Entry<String, MethodImplementationContext> globalMethod : globalMethods.entrySet()) {
            generateOrAddToClassForGlobalMethod(globalMethod.getKey(), globalMethod.getValue());
        }

        for (Entry<String, MethodImplementationContext> entry : globalMethods.entrySet()) {
            MethodCompiler methodCompiler = new MethodCompiler(this);
            methodCompiler.compileMethod(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, ClassNode> entry : generatedClassNodes.entrySet()) {
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            entry.getValue().accept(classWriter);
            byte[] output = classWriter.toByteArray();

            generatedClasses.put(entry.getKey(), output);
        }


        return generatedClasses;
    }

    private void generateOrAddToClassForGlobalMethod(String className, MethodImplementationContext method) {
        String trimmed = className.replace(".atp", "");
        ClassNode classNode = null;
        if(generatedClassNodes.containsKey(trimmed)){
            classNode = generatedClassNodes.get(trimmed);
        }else {
            classNode = new ClassNode();
            classNode.access = Opcodes.ACC_PUBLIC;
            classNode.version = Opcodes.V1_8;
            classNode.name = trimmed;
            classNode.methods = new ArrayList<>();
            generatedClassNodes.put(trimmed, classNode);
        }
        MethodNode methodNode = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.methodSignature().memberName().getText(),
                TypeUtil.extractMethodDescriptor(method.methodSignature()),
                null,
                new String[]{}
        );
        classNode.methods.add(methodNode);

    }

    private void generateClassFromImpl(ImplDeclarationContext context){
        String className = context.struct.getText() + "$" + context.itf.getText();

        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.version = Opcodes.V1_8;
        classNode.name = className;
        classNode.methods = new ArrayList<>();
        classNode.interfaces = List.of(context.itf.getText());

        for (ImplMemberDeclarationContext member : context.implMemberDeclaration()) {
            MethodSignatureContext signature = member.methodImplementation().methodSignature();
            String desc = TypeUtil.extractMethodDescriptor(signature);
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, signature.memberName().getText(), desc, null, null);
            classNode.methods.add(methodNode);
        }

        generatedClassNodes.put(className, classNode);
    }

    private void generateInterfaceFromTrait(TraitDeclarationContext context){
        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE;
        classNode.version = Opcodes.V1_8;
        classNode.name = context.typeName().getText();
        classNode.methods = new ArrayList<>();

        for (TraitMemberDeclarationContext member : context.traitMemberDeclaration()) {
            MethodSignatureContext signature = member.methodDeclaration().methodSignature();
            String desc = TypeUtil.extractMethodDescriptor(signature);
            classNode.methods.add(new MethodNode(Opcodes.ACC_ABSTRACT, signature.memberName().getText(), desc, null, null));
        }

        generatedClassNodes.put(context.typeName().getText(), classNode);
    }

    private void generateClassNodeFromModule(ModuleDeclarationContext moduleDeclarationContext) {
        String typeName = moduleDeclarationContext.typeName().getText();

        ClassNode classNode = new ClassNode();
        classNode.access = Opcodes.ACC_PUBLIC;
        classNode.version = Opcodes.V1_8;
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
                            TypeUtil.toDesc(structMemberDeclarationContext.typeName().getText()), null, null));
                }
                classNode.fields = structMembers;
            }

            if (member.moduleSelfImplDeclaration() != null) {
                ModuleSelfImplDeclarationContext impl = member.moduleSelfImplDeclaration();
                for (ImplMemberDeclarationContext implMember : impl.implMemberDeclaration()) {
                    MethodImplementationContext method = implMember.methodImplementation();
                    MethodNode methodNode = new MethodNode(
                            Opcodes.ACC_PUBLIC,
                            method.methodSignature().memberName().getText(),
                            TypeUtil.extractMethodDescriptor(method.methodSignature()),
                            null,
                            new String[]{}
                    );
                    classNode.methods.add(methodNode);
                }

            }

        }

        generatedClassNodes.put(typeName, classNode);

    }

    private void parseSingleFile(String fileName, String content) {
        AtypicalLexer lexer = new AtypicalLexer(CharStreams.fromString(content));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        AtypicalParser parser = new AtypicalParser(tokens);
        FileContext fileContext = parser.file();

        for (FileMemberContext fileMemberContext : fileContext.fileMember()) {
            if(fileMemberContext.moduleDeclaration() != null)this.modules.add(fileMemberContext.moduleDeclaration());
            if(fileMemberContext.traitDeclaration() != null)this.traits.add(fileMemberContext.traitDeclaration());
            if(fileMemberContext.implDeclaration() != null)this.impls.add(fileMemberContext.implDeclaration());

            if(fileMemberContext.methodImplementation() != null)this.globalMethods.put(fileName, fileMemberContext.methodImplementation());
        }

    }

}
