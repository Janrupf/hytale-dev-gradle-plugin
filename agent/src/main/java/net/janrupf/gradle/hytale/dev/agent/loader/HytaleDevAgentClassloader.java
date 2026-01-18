package net.janrupf.gradle.hytale.dev.agent.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

public class HytaleDevAgentClassloader extends URLClassLoader {
    private final List<HytaleDevAgentClassTransformer> transformers;

    public HytaleDevAgentClassloader(String name, URL[] urls, ClassLoader parent) {
        super(name, urls, parent);
        this.transformers = new ArrayList<>();
    }

    public void addTransformer(HytaleDevAgentClassTransformer transformer) {
        this.transformers.add(transformer);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (this.getClassLoadingLock(name)) {
            var loaded = this.findLoadedClass(name);
            if (loaded != null) {
                if (resolve) {
                    this.resolveClass(loaded);
                }

                return loaded;
            }

            var internalName = name.replace('.', '/');
            var classResource = this.findResource(internalName + ".class");

            if (classResource == null) {
                return super.loadClass(name, resolve);
            }

            try (InputStream in = classResource.openStream()) {
                var classData = in.readAllBytes();

                return transformClass(classResource, name, internalName, classData);
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to read class data for " + name, e);
            }
        }
    }

    private Class<?> transformClass(URL url, String name, String internalName, byte[] classData) {
        for (var transformer : transformers) {
            var transformedData = transformer.transform(name, internalName, classData, this);
            if (transformedData != null) {
                classData = transformedData;
            }
        }

        URL codeSourceUrl = getCodeSourceUrl(url, internalName);
        CodeSource codeSource = new CodeSource(codeSourceUrl, (CodeSigner[]) null);

        return this.defineClass(
                name,
                classData,
                0,
                classData.length,
                new ProtectionDomain(codeSource, null, this, null)
        );
    }

    private static URL getCodeSourceUrl(URL resource, String internalName) {
        String urlStr = resource.toString();
        String classPath = internalName + ".class";

        try {
            if (urlStr.startsWith("jar:")) {
                int bangIndex = urlStr.indexOf("!/");

                if (bangIndex > 0) {
                    return URI.create(urlStr.substring(4, bangIndex)).toURL();
                }
            } else if (urlStr.endsWith(classPath)) {
                return URI.create(urlStr.substring(0, urlStr.length() - classPath.length())).toURL();
            }
        } catch (MalformedURLException ignored) {
            // Ignore and return the original resource URL
        }

        return resource;
    }

}
