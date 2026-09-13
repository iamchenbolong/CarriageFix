package com.example.carriagefix;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import static org.objectweb.asm.Opcodes.*;

public class CarriageClassTransformer {

    private static final String TARGET_CLASS = "com/simibubi/create/content/trains/entity/CarriageContraption";
    private static final String CACHE_HOLDER = "com/example/carriagefix/CacheHolder";

    public byte[] transform(byte[] classfileBuffer) {
        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new CarriageClassVisitor(cw);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class CarriageClassVisitor extends ClassVisitor {
        CarriageClassVisitor(ClassVisitor cv) {
            super(ASM9, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("getRenderedBlocks".equals(name) && "()Ljava/util/Collection;".equals(descriptor)) {
                return new GetRenderedBlocksRewriter(mv, access, name, descriptor);
            }
            return mv;
        }

        @Override
        public void visitEnd() {
            super.visitField(ACC_PRIVATE | ACC_TRANSIENT, CacheHolder.CACHE_FIELD,
                    "Ljava/util/Collection;", null, null);
            super.visitField(ACC_PRIVATE, CacheHolder.VERSION_FIELD, "I", null, 0);
            super.visitEnd();
        }
    }

    private static class GetRenderedBlocksRewriter extends AdviceAdapter {
        private final Label skipCache = new Label();
        private final Label loopStart = new Label();
        private final Label loopEnd = new Label();
        private final Label addVisible = new Label();
        private final Label notInPortalLabel = new Label();
        private final Label updateCache = new Label();

        protected GetRenderedBlocksRewriter(MethodVisitor mv, int access, String name, String descriptor) {
            super(ASM9, mv, access, name, descriptor);
        }

        @Override
        protected void onMethodEnter() {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, TARGET_CLASS, "portalCutoffMin", "I");
            int minLocal = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(ISTORE, minLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, TARGET_CLASS, "portalCutoffMax", "I");
            int maxLocal = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(ISTORE, maxLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, TARGET_CLASS, "getBlocks",
                    "()Ljava/util/Map;", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "values",
                    "()Ljava/util/Collection;", true);
            int fullLocal = newLocal(Type.getObjectType("java/util/Collection"));
            mv.visitVarInsn(ASTORE, fullLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, TARGET_CLASS, CacheHolder.CACHE_FIELD,
                    "Ljava/util/Collection;");
            int cacheLocal = newLocal(Type.getObjectType("java/util/Collection"));
            mv.visitVarInsn(ASTORE, cacheLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, TARGET_CLASS, CacheHolder.VERSION_FIELD, "I");
            int versionLocal = newLocal(Type.INT_TYPE);
            mv.visitVarInsn(ISTORE, versionLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, minLocal);
            mv.visitVarInsn(ILOAD, maxLocal);
            mv.visitVarInsn(ALOAD, cacheLocal);
            mv.visitVarInsn(ILOAD, versionLocal);
            mv.visitInsn(ICONST_0);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ILOAD, versionLocal);
            mv.visitMethodInsn(INVOKESTATIC, CACHE_HOLDER, "getCachedIfValid",
                    "(Ljava/lang/Object;IILjava/util/Collection;IIII)Ljava/util/Collection;",
                    false);
            int resultLocal = newLocal(Type.getObjectType("java/util/Collection"));
            mv.visitVarInsn(ASTORE, resultLocal);

            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitJumpInsn(IFNULL, skipCache);
            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitInsn(ARETURN);
            mv.visitLabel(skipCache);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, TARGET_CLASS, "notInPortal", "()Z", false);
            mv.visitJumpInsn(IFNE, notInPortalLabel);

            mv.visitTypeInsn(NEW, "java/util/ArrayList");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
            mv.visitVarInsn(ASTORE, resultLocal);

            mv.visitVarInsn(ALOAD, fullLocal);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "iterator",
                    "()Ljava/util/Iterator;", true);
            int iterLocal = newLocal(Type.getObjectType("java/util/Iterator"));
            mv.visitVarInsn(ASTORE, iterLocal);

            mv.visitLabel(loopStart);
            mv.visitVarInsn(ALOAD, iterLocal);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
            mv.visitJumpInsn(IFEQ, loopEnd);

            mv.visitVarInsn(ALOAD, iterLocal);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo");
            int infoLocal = newLocal(Type.getObjectType("net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo"));
            mv.visitVarInsn(ASTORE, infoLocal);

            mv.visitVarInsn(ALOAD, infoLocal);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                    "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo",
                    "pos", "()Lnet/minecraft/core/BlockPos;", false);
            int posLocal = newLocal(Type.getObjectType("net/minecraft/core/BlockPos"));
            mv.visitVarInsn(ASTORE, posLocal);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, posLocal);
            mv.visitMethodInsn(INVOKEVIRTUAL, TARGET_CLASS, "withinVisible",
                    "(Lnet/minecraft/core/BlockPos;)Z", false);
            mv.visitJumpInsn(IFNE, addVisible);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, posLocal);
            mv.visitMethodInsn(INVOKEVIRTUAL, TARGET_CLASS, "atSeam",
                    "(Lnet/minecraft/core/BlockPos;)Z", false);
            mv.visitJumpInsn(IFEQ, loopStart);

            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitTypeInsn(NEW, "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, posLocal);
            mv.visitFieldInsn(GETSTATIC, "net/minecraft/world/level/block/Blocks", "PURPLE_STAINED_GLASS",
                    "Lnet/minecraft/world/level/block/Block;");
            mv.visitMethodInsn(INVOKEVIRTUAL, "net/minecraft/world/level/block/Block", "defaultBlockState",
                    "()Lnet/minecraft/world/level/block/state/BlockState;", false);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKESPECIAL,
                    "net/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate$StructureBlockInfo",
                    "<init>", "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;)V", false);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            mv.visitJumpInsn(GOTO, loopStart);

            mv.visitLabel(addVisible);
            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitVarInsn(ALOAD, infoLocal);
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            mv.visitJumpInsn(GOTO, loopStart);

            mv.visitLabel(loopEnd);
            mv.visitJumpInsn(GOTO, updateCache);

            mv.visitLabel(notInPortalLabel);
            mv.visitVarInsn(ALOAD, fullLocal);
            mv.visitVarInsn(ASTORE, resultLocal);

            mv.visitLabel(updateCache);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitFieldInsn(PUTFIELD, TARGET_CLASS, CacheHolder.CACHE_FIELD,
                    "Ljava/util/Collection;");

            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ILOAD, versionLocal);
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, TARGET_CLASS, CacheHolder.VERSION_FIELD, "I");

            mv.visitVarInsn(ALOAD, resultLocal);
            mv.visitInsn(ARETURN);
        }
    }
}