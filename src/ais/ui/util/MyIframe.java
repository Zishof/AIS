package ais.ui.util;

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Iframe;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my iframe. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Iframe}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code init()}); mutasi data ({@code
 * setSrc()}); operasi domain lain ({@code contextAware()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Iframe
 */
public class MyIframe extends Iframe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8735575580753564474L;

	public MyIframe() {
		super();
		init();
	}

	public MyIframe(String src) {
		super();
		init();
		setSrc(src);
	}

	/**
	 * URL internal yang diawali slash harus mengikuti context aplikasi aktif.
	 * Tanpa prefix ini, instalasi multi-context seperti /batusangkar akan meminta
	 * /pages/... dari ROOT Tomcat dan iframe justru menampilkan portal publik.
	 */
	public void setSrc(String src) {
		super.setSrc(contextAware(src));
	}

	private static String contextAware(String src) {
		if (src == null || src.length() == 0 || !src.startsWith("/") || src.startsWith("//")) {
			return src;
		}
		String context = "";
		try {
			Execution execution = Executions.getCurrent();
			if (execution != null && execution.getContextPath() != null) context = execution.getContextPath();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MyIframe.contextAware");
		}
		if (context.length() == 0 && ais.common.Common.ROOT != null) context = ais.common.Common.ROOT;
		if (context == null || context.length() == 0 || "/".equals(context)) return src;
		if (!context.startsWith("/")) context = "/" + context;
		if (context.endsWith("/")) context = context.substring(0, context.length() - 1);
		return src.equals(context) || src.startsWith(context + "/") ? src : context + src;
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
	}

}
