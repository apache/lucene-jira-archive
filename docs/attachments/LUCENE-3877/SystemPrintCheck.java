import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;
import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

/**
 * Static analysis that looks for any usage in bytecode of
 * GETSTATIC #<TABLE_NUM> (Where tablenum is a field, and asm thankfully resolves it for us)
 *
 * The opcodes for a System.(out|err) call are like so
 *
 * getstatic #2;    //Field java/lang/System.out:Ljava/io/PrintStream;
 * ldc #3; // some string constant (or other opcodes to put the string onto the value stack
 * invokevirtual #4; //Method java/io/PrintStream.println:(Ljava/lang/String;)V
 *
 * This means that we can look for GETSTATIC #2 // java/lang/System ....
 * it might mean that we get a weird error message if the code does something like
 *
 * ....
 * 1: private final PrintStream printStream = System.err;
 * 2:
 * 3: public class Something(printStream) {
 * 4:      PrintStream printer;
 * 5:      public Something(PrintStream printStream) {
 * 6:          this.printer = printStream;
 * 7:          this.printer.println("silly");
 * 8:      }
 * 9: }
 * ....
 *
 * The static analysis tool will fail on line 1 where the GETSTATIC is performed, rather than where the println method
 * is called.
 *
 * I think that is probably sane to fail on the GETSTATIC
 *
 * @author Greg Bowyer
 */
public class SystemPrintCheck {
    public final List<String> callees = new LinkedList<String>();

    private final class SystemOutClassVisitor extends EmptyVisitor {
        private String source;
        private String className;
        private String methodName;

        private final MethodVisitor methodVisitor = new SystemOutMethodVisitor(this);

        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            this.methodName = name;
            return methodVisitor;
        }

        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.className = name;
        }

        public void visitSource(String source, String debug) {
            this.source = source;
        }
    }

    private final class SystemOutMethodVisitor extends EmptyVisitor {
        private int lineNo;
        
        private boolean callsTarget;
        private SystemOutClassVisitor parent;

        public SystemOutMethodVisitor(SystemOutClassVisitor parent) {
            this.parent = parent;
        }

        public void visitFieldInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.GETSTATIC &&
                owner.equals("java/lang/System") &&
                (name.equals("out") | name.equals("err"))) {
                    this.callsTarget = true;
            }
        }

        // If we want to trace through the invoke on the object we can do it like so
        /*
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            if (opcode == Opcodes.INVOKEVIRTUAL) {
                if (owner.equals("java/io/PrintStream")) {
                    this.callsTarget = true;
                }
            }
        }
        */

        public void visitCode() {
            callsTarget = false;
        }

        public void visitLineNumber(int lineNo, Label start) {
            this.lineNo = lineNo;
        }

        public void visitEnd() {
            if (callsTarget) {
                callees.add(String.format("%s.%s @ %s +%d", parent.className, parent.methodName, parent.source, lineNo));
            }
        }
    }

    public void findCallingMethodsInJar(String jarPath) throws Exception {

        SystemOutClassVisitor visitor = new SystemOutClassVisitor();

        JarFile jarFile = new JarFile(jarPath);
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            if (entry.getName().endsWith(".class")) {
                InputStream stream = new BufferedInputStream(jarFile.getInputStream(entry), 1024);
                try {
                    ClassReader reader = new ClassReader(stream);
                    reader.accept(visitor, 0);
                } finally {
                    stream.close();
                }
            }
        }
    }

    public static void main(String... args) {
        try {
            SystemPrintCheck checker = new SystemPrintCheck();
            checker.findCallingMethodsInJar(args[0]);

            for (String c : checker.callees) {
                System.out.println(c);
            }

            if (!checker.callees.isEmpty()) {
                System.exit(1);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
