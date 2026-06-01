package io.xjar.boot;

import io.xjar.XDecryptor;
import io.xjar.XEncryptor;
import io.xjar.XKit;
import io.xjar.key.XKey;
import io.xjar.reflection.XReflection;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.springframework.boot.loader.launch.LaunchedClassLoader;

import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Enumeration;

import static org.objectweb.asm.ClassReader.EXPAND_FRAMES;
import static org.objectweb.asm.ClassReader.SKIP_DEBUG;
import static org.objectweb.asm.Opcodes.ASM9;

/**
 * X类加载器
 *
 * @author Payne 646742615@qq.com
 * 2018/11/23 23:04
 */
public class XBootClassLoader extends LaunchedClassLoader {
    private final XBootURLHandler xBootURLHandler;
    private final Object urlClassPath;
    private final Method getResource;
    private final Method getCodeSourceURL;
    private final Method getCodeSigners;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public XBootClassLoader(URL[] urls, ClassLoader parent, XDecryptor xDecryptor, XEncryptor xEncryptor, XKey xKey) throws Exception {
        super(true, urls, parent);
        this.xBootURLHandler = new XBootURLHandler(xDecryptor, xEncryptor, xKey, this);
        this.urlClassPath = XReflection.field(URLClassLoader.class, "ucp").get(this).value();
        this.getResource = XReflection.method(urlClassPath.getClass(), "getResource", String.class).method();
        this.getCodeSourceURL = XReflection.method(getResource.getReturnType(), "getCodeSourceURL").method();
        this.getCodeSigners = XReflection.method(getResource.getReturnType(), "getCodeSigners").method();
    }

    @Override
    public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null) {
            return null;
        }
        try {
            return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), xBootURLHandler);
        } catch (MalformedURLException e) {
            return url;
        }
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Enumeration<URL> enumeration = super.findResources(name);
        if (enumeration == null) {
            return null;
        }
        return new XBootEnumeration(enumeration);
    }
    private final String SPRING_PATCH = "org.springframework.core.io.UrlResource";
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> aClass = null;
        try {
            if(SPRING_PATCH.equals(name)) {
                throw new ClassFormatError("do patch code");
            }
            aClass = super.findClass(name);
        } catch (ClassFormatError e) {
            String path = name.replace('.', '/').concat(".class");
            URL url = findResource(path);
            if (url == null) {
                throw new ClassNotFoundException(name, e);
            }
            try (InputStream in = url.openStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XKit.transfer(in, bos);
                byte[] bytes = bos.toByteArray();
                if(SPRING_PATCH.equals(name)) {
                    System.out.println("org.springframework.core.io.UrlResource.createRelative 已经回退到老版本");
                    bytes = injectcode(bytes);
                }
                Object resource = getResource.invoke(urlClassPath, path);
                URL codeSourceURL = (URL) getCodeSourceURL.invoke(resource);
                CodeSigner[] codeSigners = (CodeSigner[]) getCodeSigners.invoke(resource);
                CodeSource codeSource = new CodeSource(codeSourceURL, codeSigners);
                aClass = defineClass(name, bytes, 0, bytes.length, codeSource);
            } catch (Throwable t) {
                throw new ClassNotFoundException(name, t);
            }
        }

        return aClass;
    }
    protected Class<?> findClass1(String name) throws ClassNotFoundException {
        Class<?> aClass = null;
        try {
            aClass = super.findClass(name);
        } catch (ClassFormatError e) {
            // 统一处理：从资源中读取字节码并定义
            String path = name.replace('.', '/').concat(".class");
            URL url = findResource(path);
            if (url == null) {
                throw new ClassNotFoundException(name, e);
            }
            try (InputStream in = url.openStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                XKit.transfer(in, bos);
                byte[] bytes = bos.toByteArray();
                // 可选的字节码注入（如果需要）
                // if (需要特殊注入的类) bytes = injectcode(bytes);
                Object resource = getResource.invoke(urlClassPath, path);
                URL codeSourceURL = (URL) getCodeSourceURL.invoke(resource);
                CodeSigner[] codeSigners = (CodeSigner[]) getCodeSigners.invoke(resource);
                CodeSource codeSource = new CodeSource(codeSourceURL, codeSigners);
                aClass = defineClass(name, bytes, 0, bytes.length, codeSource);
            } catch (Throwable t) {
                throw new ClassNotFoundException(name, t);
            }
        }
        return aClass;
    }
    private byte[] injectcode(byte[] data) {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS |ClassWriter.COMPUTE_FRAMES );
        ClassReader classReader = new ClassReader(data);
        // 使用自定义的ClassVisitor访问者对象，访问该类文件的结构
        classReader.accept(new AVisitor(ASM9, classWriter), SKIP_DEBUG);
        byte[] bytes = classWriter.toByteArray();
        return bytes;

    }

    private class XBootEnumeration implements Enumeration<URL> {
        private final Enumeration<URL> enumeration;

        XBootEnumeration(Enumeration<URL> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasMoreElements() {
            return enumeration.hasMoreElements();
        }

        @Override
        public URL nextElement() {
            URL url = enumeration.nextElement();
            if (url == null) {
                return null;
            }
            try {
                return new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile(), xBootURLHandler);
            } catch (MalformedURLException e) {
                return url;
            }
        }
    }
}
