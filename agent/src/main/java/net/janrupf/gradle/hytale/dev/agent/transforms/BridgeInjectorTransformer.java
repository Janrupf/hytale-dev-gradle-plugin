package net.janrupf.gradle.hytale.dev.agent.transforms;

import net.janrupf.gradle.hytale.dev.agent.BytecodeEntryPoints;
import net.janrupf.gradle.hytale.dev.agent.loader.HytaleDevAgentClassTransformer;

import java.lang.classfile.*;
import java.lang.classfile.instruction.ReturnInstruction;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

/**
 * Transformer that injects bridge initialization into HytaleLogger's static initializer.
 * <p>
 * This ensures the bridge is initialized as early as possible, right after the logger
 * infrastructure is ready but before the main server code runs.
 */
public class BridgeInjectorTransformer implements HytaleDevAgentClassTransformer {
    private static final String TARGET_CLASS = "com.hypixel.hytale.logger.HytaleLogger";
    private static final ClassDesc BYTECODE_ENTRY_POINTS_CLASS = ClassDesc.of(BytecodeEntryPoints.class.getName());

    @Override
    public byte[] transform(String name, String internalName, byte[] classData, ClassLoader loader) {
        if (!name.equals(TARGET_CLASS)) {
            return null;
        }

        var resolver = ClassHierarchyResolver.ofClassLoading(loader);
        var originalClass = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver)).parse(classData);
        return ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver)).build(
                originalClass.thisClass().asSymbol(),
                (builder) -> this.rebuildLoggerClass(originalClass, builder)
        );
    }

    private void rebuildLoggerClass(ClassModel originalClass, ClassBuilder builder) {
        boolean foundClinit = false;

        for (var element : originalClass) {
            if (element instanceof MethodModel method &&
                    method.methodName().equalsString("<clinit>")) {
                foundClinit = true;
                builder.withMethod(
                        method.methodName(),
                        method.methodType(),
                        method.flags().flagsMask(),
                        (methodBuilder) -> this.rebuildClinitMethod(method, methodBuilder)
                );
            } else {
                builder.with(element);
            }
        }

        // If there was no static initializer, create one with just the bridge initialization
        if (!foundClinit) {
            builder.withMethodBody(
                    "<clinit>",
                    MethodTypeDesc.ofDescriptor("()V"),
                    ClassFile.ACC_STATIC,
                    (codeBuilder) -> {
                        codeBuilder.invokestatic(
                                BYTECODE_ENTRY_POINTS_CLASS,
                                "initializeBridge",
                                MethodTypeDesc.ofDescriptor("()V")
                        );
                        codeBuilder.return_();
                    }
            );
        }
    }

    private void rebuildClinitMethod(MethodModel originalMethod, MethodBuilder builder) {
        for (var element : originalMethod) {
            if (element instanceof CodeModel code) {
                builder.withCode(
                        (codeBuilder) -> this.rebuildClinitCode(code, codeBuilder)
                );
            } else {
                builder.with(element);
            }
        }
    }

    private void rebuildClinitCode(CodeModel originalCode, CodeBuilder builder) {
        // Copy all instructions, but inject our call before every return
        for (var element : originalCode) {
            if (element instanceof ReturnInstruction) {
                // Inject bridge initialization before return
                builder.invokestatic(
                        BYTECODE_ENTRY_POINTS_CLASS,
                        "initializeBridge",
                        MethodTypeDesc.ofDescriptor("()V")
                );
            }
            builder.with(element);
        }
    }
}
