package com.example.carriagefix;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class CarriageFixLaunchPlugin implements ILaunchPluginService {

    private static final String TARGET = "com.simibubi.create.content.trains.entity.CarriageContraption";

    @Override
    public String name() {
        return "carriagefix";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        if (TARGET.equals(classType.getClassName())) {
            return EnumSet.of(Phase.BEFORE);
        }
        return EnumSet.noneOf(Phase.class);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        System.out.println("[CarriageFix] Processing " + classType.getClassName());
        try {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);
            byte[] original = cw.toByteArray();

            byte[] transformed = new CarriageClassTransformer().transform(original);
            if (transformed == null) return false;

            // 把修改后的字节码重新解析回 ClassNode
            ClassReader cr = new ClassReader(transformed);
            ClassNode newClass = new ClassNode();
            cr.accept(newClass, ClassReader.EXPAND_FRAMES);

            classNode.version = newClass.version;
            classNode.fields = newClass.fields;
            classNode.methods = newClass.methods;
            return true;
        } catch (Throwable t) {
            System.err.println("[CarriageFix] Failed: " + t);
            t.printStackTrace();
            return false;
        }
    }

    // 需要显式导入
    // import org.objectweb.asm.ClassReader;
    // import org.objectweb.asm.ClassWriter;
}