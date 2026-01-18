package net.janrupf.gradle.hytale.dev.agent.loader;

/**
 * Interface for transforming class bytecode in the Hytale Dev Agent.
 */
public interface HytaleDevAgentClassTransformer {
    /**
     * Transform the given class data.
     *
     * @param name         the class name (e.g., "com.example.MyClass")
     * @param internalName the internal class name (e.g., "com/example/MyClass")
     * @param classData    the original class bytecode
     * @param loader       the class loader loading the class
     * @return the transformed class bytecode, or null if no transformation is applied
     */
    byte[] transform(String name, String internalName, byte[] classData, ClassLoader loader);
}
