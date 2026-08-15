package ais.action.servlet;

/** Uji canonicalization route yang dapat dijalankan tanpa container Tomcat. */
public final class EBisnisFrontControllerSelfTest {
	private EBisnisFrontControllerSelfTest() { }

	public static void main(String[] args) {
		check("/".equals(EBisnisFrontController.normalize(null)), "null root");
		check("/inventory/01".equals(EBisnisFrontController.normalize("/Inventory/01/")),
				"case and trailing slash");
		reject("inventory/01");
		reject("/inventory//01");
		reject("/inventory/../web.xml");
		reject("/assets/%2e%2e/web-inf/web.xml");
		reject("/assets/%2Fweb-inf/web.xml");
		reject("/assets/\\web-inf\\web.xml");
		System.out.println("EBisnisFrontControllerSelfTest: PASS");
	}

	private static void reject(String path) {
		try {
			EBisnisFrontController.normalize(path);
			throw new IllegalStateException("FAILED: accepted " + path);
		} catch (IllegalArgumentException expected) {
			// expected
		}
	}

	private static void check(boolean condition, String label) {
		if (!condition) throw new IllegalStateException("FAILED: " + label);
	}
}
