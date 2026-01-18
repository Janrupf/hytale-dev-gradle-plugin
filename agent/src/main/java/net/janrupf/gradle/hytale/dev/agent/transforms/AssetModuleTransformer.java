package net.janrupf.gradle.hytale.dev.agent.transforms;

import net.janrupf.gradle.hytale.dev.agent.BytecodeEntryPoints;
import net.janrupf.gradle.hytale.dev.agent.loader.HytaleDevAgentClassTransformer;

import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;

public class AssetModuleTransformer implements HytaleDevAgentClassTransformer {
    private static final ClassDesc NIO_PATH_CLASS = ClassDesc.of(Path.class.getName());
    private static final TypeKind NIO_PATH_TYPE_KIND = TypeKind.from(NIO_PATH_CLASS);
    private static final ClassDesc BYTECODE_ENTRY_POINTS_CLASS = ClassDesc.of(BytecodeEntryPoints.class.getName());

    @Override
    public byte[] transform(String name, String internalName, byte[] classData, ClassLoader loader) {
        if (!name.equals("com.hypixel.hytale.server.core.asset.AssetModule")) {
            return null;
        }

        var resolver = ClassHierarchyResolver.ofClassLoading(loader);
        var originalClass = ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver)).parse(classData);
        return ClassFile.of(ClassFile.ClassHierarchyResolverOption.of(resolver)).build(
                originalClass.thisClass().asSymbol(),
                (builder) -> this.rebuildAssetModuleClass(originalClass, builder)
        );
    }

    private void rebuildAssetModuleClass(ClassModel originalClass, ClassBuilder builder) {
        for (var element : originalClass) {
            int pathParamIndex;
            if (!(element instanceof MethodModel method) ||
                    !method.methodName().equalsString("registerPack") ||
                    (pathParamIndex = this.pathParameter(method)) == -1
            ) {
                builder.with(element);
                continue;
            }

            builder.withMethod(
                    method.methodName(),
                    method.methodType(),
                    method.flags().flagsMask(),
                    (methodBuilder) -> this.rebuildRegisterPackMethod(
                            method,
                            methodBuilder,
                            pathParamIndex
                    )
            );
        }
    }

    private int pathParameter(MethodModel model) {
        var parameters = model.methodTypeSymbol().parameterArray();

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].equals(NIO_PATH_CLASS)) {
                return i;
            }
        }

        return -1;
    }

    private void rebuildRegisterPackMethod(MethodModel originalMethod, MethodBuilder builder, int pathParamIndex) {
        for (var element : originalMethod) {
            if (element instanceof CodeModel code) {
                builder.withCode(
                        (codeBuilder) -> this.rebuildRegisterPackMethodCode(
                                code,
                                codeBuilder,
                                pathParamIndex + (originalMethod.flags().has(AccessFlag.STATIC) ? 0 : 1)
                        )
                );
            } else {
                builder.with(element);
            }
        }
    }

    private void rebuildRegisterPackMethodCode(CodeModel originalCode, CodeBuilder builder, int pathParamIndex) {
        builder.loadLocal(NIO_PATH_TYPE_KIND, pathParamIndex);
        builder.invokestatic(
                BYTECODE_ENTRY_POINTS_CLASS,
                "redirectAssetPackPath",
                MethodTypeDesc.of(
                        NIO_PATH_CLASS,
                        NIO_PATH_CLASS
                )
        );
        builder.storeLocal(NIO_PATH_TYPE_KIND, pathParamIndex);

        for (var element : originalCode) {
            builder.with(element);
        }
    }
}
