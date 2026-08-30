package com.mas6y6.musmeta.plugin.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.transformers.MixinClassReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class MusMetaBytecodeProvider implements IClassBytecodeProvider {

    @Override
    public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
        return getClassNode(name, false);
    }

    @Override
    public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException, IOException {
        byte[] bytes = getClassBytes(name);
        if (bytes == null) {
            throw new ClassNotFoundException("Class bytes not found for " + name);
        }
        ClassNode classNode = new ClassNode(Opcodes.ASM9);
        new MixinClassReader(bytes, name).accept(classNode, ClassReader.EXPAND_FRAMES);
        return classNode;
    }

    private byte[] getClassBytes(String name) throws IOException {
        String resource = name.replace('.', '/') + ".class";
        for (ClassLoader loader : MusMetaMixinService.loaders()) {
            try (InputStream in = loader.getResourceAsStream(resource)) {
                if (in == null) {
                    continue;
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            }
        }
        return null;
    }
}