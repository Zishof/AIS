package ais.ui.util;

import org.zkoss.zul.Bandpopup;

import ais.common.Common;
import ais.common.HeadlessActionContext;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my bandpopup. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Bandpopup}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean pakaiClose};
 * inisialisasi/lifecycle ({@code init()}, {@code initBg()}); mutasi data ({@code setWidth()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Bandpopup
 */
public class MyBandpopup extends Bandpopup {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3794161371195657634L;

	public static boolean pakaiClose = true;

	public MyBandpopup() {
		super();
		init();
	}

	private void init() {
		initBg();

	}

	public void initBg() {
		if (HeadlessActionContext.isActive()) return;

		if (Common.isMobile()) {
			super.setWidth("100%");
		}

	}

	@Override
	public void setWidth(String width) {
		try {
			if (HeadlessActionContext.isActive()) {
				super.setWidth(width);
				return;
			}
			if (Common.isMobile()) {
				super.setWidth("100%");
				return;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyBandpopup.java:41");
			// TODO: handle exception
		}
		super.setWidth(width);
	}

}
