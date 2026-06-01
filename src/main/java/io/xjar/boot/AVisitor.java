package io.xjar.boot;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;


public class AVisitor extends ClassVisitor {
    protected AVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (name.equals("createRelative") && desc.equals("(Ljava/lang/String;)Lorg/springframework/core/io/Resource;")) {
            // 修改原方法的字节码
            mv = new MyMethodVisitor(mv) ;
        }

        return mv;
    }

    //MyMethodVisitor的结构
    public class MyMethodVisitor extends MethodVisitor {
        private final MethodVisitor target;

        public MyMethodVisitor(MethodVisitor mv) {
            super(ASM9, null);
            this.target = mv;
        }

        //此方法在目标方法调用之前调用，所以前置操作可以在这处理
        @Override
        public void visitCode() {
            target.visitCode();

            target.visitCode();
            target.visitVarInsn(ALOAD, 1);
            Label A = new Label();
            Label B = new Label();

            target.visitLdcInsn("/");
            target.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
            target.visitJumpInsn(IFEQ, A);

            target.visitVarInsn(ALOAD, 1);
            target.visitInsn(ICONST_1);
            target.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
            target.visitVarInsn(ASTORE, 1);
            target.visitJumpInsn(GOTO,B);

            target.visitLabel(A);
            target.visitLabel(B);



            target.visitTypeInsn(Opcodes.NEW, "org/springframework/core/io/UrlResource");
            target.visitInsn(DUP);
            target.visitTypeInsn(Opcodes.NEW, "java/net/URL");
            target.visitInsn(DUP);
            target.visitVarInsn(ALOAD, 0);
            target.visitFieldInsn(GETFIELD, "org/springframework/core/io/UrlResource", "url", "Ljava/net/URL;");
            target.visitVarInsn(ALOAD, 1);
            target.visitMethodInsn(INVOKESPECIAL, "java/net/URL", "<init>", "(Ljava/net/URL;Ljava/lang/String;)V", false);
            target.visitMethodInsn(INVOKESPECIAL, "org/springframework/core/io/UrlResource", "<init>", "(Ljava/net/URL;)V", false);
            target.visitInsn(ARETURN); //

            target.visitMaxs(6, 2);
            target.visitEnd();


        }
        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + 1, maxLocals);
        }

    }

}
