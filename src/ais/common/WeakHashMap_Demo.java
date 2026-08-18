package ais.common;

import java.util.Map;
import java.util.WeakHashMap;

public class WeakHashMap_Demo {

	@SuppressWarnings("rawtypes")
	private static Map map;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void main(String args[]) {
		map = new WeakHashMap();
		map.put(new String("Maine"), "Augusta");

		Runnable runner = new Runnable() {
			public void run() {
				while (map.containsKey("Maine")) {
					try {
						Thread.sleep(1500);
					} catch (InterruptedException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/WeakHashMap_Demo.java:21");
					}
					System.out.println("Thread waiting");
					System.gc();
				}
				
			}
		};
		Thread t = new Thread(runner);
		t.start();
		System.out.println("Main waiting");
		try {
			t.join();
		} catch (InterruptedException ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/WeakHashMap_Demo.java:34");
		}
	}
}