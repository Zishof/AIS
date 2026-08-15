package ais.common.ebisnis;

/** Verifikasi mandiri registry yang dapat dijalankan tanpa Tomcat/database. */
public final class EBisnisRouteRegistrySelfTest {
	private EBisnisRouteRegistrySelfTest() { }

	public static void main(String[] args) {
		check(EBisnisRouteRegistry.resolve("/") != null, "root");
		check(EBisnisRouteRegistry.resolve("/api/v1") != null, "api");
		check("/Api_eBisnis".equals(EBisnisRouteRegistry.resolve("/api/v1/sales/order").getTarget()),
				"api compatibility path");
		check(EBisnisRouteRegistry.resolve("/inventory") != null, "inventory root");
		check("/presentasi".equals(EBisnisRouteRegistry.resolve("/dokumen/presentasi").getTarget()),
				"commercial document servlet");
		check(EBisnisRouteRegistry.resolve("/belanja") != null, "public storefront");
		check(EBisnisRouteRegistry.resolve("/platform").isAisLoginRequired(), "platform auth guard");
		check(EBisnisRouteRegistry.resolve("/app/pos").isAisLoginRequired(), "tenant app auth guard");
		check(EBisnisRouteRegistry.inventoryRoutes().size() == 96, "48 screen aliases");
		for (int i = 1; i <= 48; i++) {
			String number = i < 10 ? "0" + i : String.valueOf(i);
			check(EBisnisRouteRegistry.resolve("/inventory/screen/" + number) != null,
					"inventory screen " + number);
		}
		check(EBisnisRouteRegistry.resolve("/inventory/screen/49") == null, "unknown screen");
		check(EBisnisRouteRegistry.resolve("/assets/css/baru/base-theme.css") != null, "safe asset");
		check(EBisnisRouteRegistry.resolve("/assets/../web-inf/web.xml") == null, "asset traversal");
		check(EBisnisRouteRegistry.resolve("/apotik/resep") != null, "apotik subroute");
		check(EBisnisRouteRegistry.resolve("/emedik/pendaftaran") != null, "emedik subroute");
		System.out.println("EBisnisRouteRegistrySelfTest: PASS");
	}

	private static void check(boolean condition, String label) {
		if (!condition) throw new IllegalStateException("FAILED: " + label);
	}
}
