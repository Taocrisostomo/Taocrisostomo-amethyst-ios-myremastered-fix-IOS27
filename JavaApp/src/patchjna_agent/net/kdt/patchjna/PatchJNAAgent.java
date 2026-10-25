package net.kdt.patchjna;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class PatchJNAAgent implements ClassFileTransformer {

    private byte[] loadPatch(String resourceName) throws IOException {
        InputStream inputStream =
                PatchJNAAgent.class.getClassLoader().getResourceAsStream(resourceName);

        if (inputStream == null) {
            throw new FileNotFoundException("Patch resource not found: " + resourceName);
        }

        try (InputStream in = inputStream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {

        try {
            if ("com/sun/jna/Platform".equals(className)) {
                System.out.println("PatchJNAAgent: Replacing com.sun.jna.Platform");
                return loadPatch("com/sun/jna/Platform.class.patch");
            }

            if ("com/mojang/blaze3d/platform/MacosUtil".equals(className)) {
                System.out.println("PatchJNAAgent: Replacing Minecraft MacosUtil with iOS stub");
                return loadPatch("com/mojang/blaze3d/platform/MacosUtil.class.patch");
            }
        } catch (Exception e) {
            System.err.println("PatchJNAAgent: Failed to patch " + className);
            e.printStackTrace();
        }

        return classfileBuffer;
    }

    public static void premain(String args, Instrumentation instrumentation) {
        System.out.println("PatchJNAAgent: premain called");
        instrumentation.addTransformer(new PatchJNAAgent());
    }
}
