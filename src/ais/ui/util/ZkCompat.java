package ais.ui.util;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jembatan API ZK 5 -> ZK CE 9/10 untuk method yang DIHAPUS sejak ZK 7:
 * setFlex(boolean), setSpans(String), setFixedLayout(boolean).
 *
 * - Di ZK 5 (deploy saat ini): method asli dipanggil via refleksi sehingga
 *   perilaku 100% sama seperti sebelum transform.
 * - Di ZK 9/10 CE: method tidak ada; fallback yang setara dipakai
 *   (setFixedLayout -> setSizedByContent kebalikan nilainya; flex memang
 *   sudah menjadi perilaku bawaan borderlayout ZK 7+; spans disimpan
 *   sebagai atribut untuk emulasi/diagnosa).
 *
 * Method di-cache per class supaya refleksi tidak membebani render.
 * Kompatibel Java 1.7.
 */
public final class ZkCompat {

	private static final Map<String, Method> CACHE = new ConcurrentHashMap<String, Method>();
	private static final Method TIDAK_ADA;

	static {
		Method m = null;
		try {
			m = ZkCompat.class.getDeclaredMethod("penanda", new Class[0]);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/ZkCompat.java:30");
		}
		TIDAK_ADA = m;
	}

	@SuppressWarnings("unused")
	private static void penanda() {
	}

	private ZkCompat() {
	}

	/** ZK5: Center/West/East/North/South.setFlex; ZK7+ sudah perilaku bawaan. */
	public static void setFlex(Object komponen, boolean flex) {
		panggil(komponen, "setFlex", boolean.class, Boolean.valueOf(flex));
	}

	/** ZK5: Row/Group.setSpans; ZK7+ diganti Cell colspan. */
	public static void setSpans(Object komponen, String spans) {
		if (!panggil(komponen, "setSpans", String.class, spans)) {
			try {
				((org.zkoss.zk.ui.Component) komponen).setAttribute("ais_spans", spans);
			} catch (Throwable e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/ZkCompat.java:52");
			}
		}
	}

	/** Kembalikan Page aktif dari eksekusi saat ini; null jika tidak ada. */
	public static org.zkoss.zk.ui.Page getCurrentPage() {
		try {
			org.zkoss.zk.ui.Execution exec = org.zkoss.zk.ui.Executions.getCurrent();
			if (exec == null) return null;
			org.zkoss.zk.ui.Desktop dt = exec.getDesktop();
			if (dt == null) return null;
			java.util.Collection<org.zkoss.zk.ui.Page> pages = dt.getPages();
			return (pages == null || pages.isEmpty()) ? null : pages.iterator().next();
		} catch (Throwable t) {
			return null;
		}
	}

	/** ZK5: Grid/Listbox.setFixedLayout; ZK7+ = setSizedByContent(kebalikan). */
	public static void setFixedLayout(Object komponen, boolean fixedLayout) {
		if (!panggil(komponen, "setFixedLayout", boolean.class, Boolean.valueOf(fixedLayout))) {
			panggil(komponen, "setSizedByContent", boolean.class, Boolean.valueOf(!fixedLayout));
		}
	}

	private static boolean panggil(Object objek, String namaMethod, Class<?> tipeArg, Object arg) {
		if (objek == null) {
			return false;
		}
		String kunci = objek.getClass().getName() + "#" + namaMethod;
		Method method = CACHE.get(kunci);
		if (method == null) {
			try {
				method = objek.getClass().getMethod(namaMethod, new Class[] { tipeArg });
			} catch (Throwable e) {
				method = TIDAK_ADA;
			}
			if (method != null) {
				CACHE.put(kunci, method);
			}
		}
		if (method == null || method == TIDAK_ADA) {
			return false;
		}
		try {
			method.invoke(objek, new Object[] { arg });
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
}
