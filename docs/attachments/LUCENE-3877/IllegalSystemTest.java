import java.io.PrintStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;

import sun.reflect.Reflection;
import java.util.Set;
import java.util.HashSet;

import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

public class IllegalSystemTest {

	public static void main(String... args) {

		Enhancer enhancer = new Enhancer();
		enhancer.setCallback(new MethodInterceptor() {

			// As an example, essentially method and class are available
			// at the proxy time, here we disallow Test3
			private Set<Class> allowedClasses = new HashSet<Class>(){{
				add(Test1.class);
				add(Test2.class);
			}};

			public Object intercept(Object target, Method method, 
					Object[] args, MethodProxy methodProxy) throws Throwable {

				// This is (I think_ hotspot specifc, its nice in that it provides a fast
				// way to grab the caller class from the stack, but might need reworking
				// for other vendors JVMs
				Class callee = Reflection.getCallerClass(2);
				if (allowedClasses.contains(callee)) {
					methodProxy.invokeSuper(target, args);
				}
				throw new IllegalStateException("Disallowed class attempted to use System printers");
			}
		});
		enhancer.setSuperclass(PrintStream.class);
		PrintStream breakingPrintStream = (PrintStream) enhancer.create(
			new Class[] { OutputStream.class }, new Object[] { System.out });

		System.setOut(breakingPrintStream);
		(new Test1()).run();
		(new Test2()).run();
		(new Test3()).run();
	}
}

class Test1 {
	public void run() {
		System.out.println("Test1");
	}
}

class Test2 {
	public void run() {
		System.out.println("Test2");
	}
}

class Test3 {
	public void run() {
		System.out.println("Test3");
	}
}
