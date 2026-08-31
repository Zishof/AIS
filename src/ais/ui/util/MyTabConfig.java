package ais.ui.util;

import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zul.Tab;

import ais.common.Common;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my tab config. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Tab}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String labelLokal}; mutasi data ({@code
 * setTooltiptext()}, {@code setLabel()}, {@code setLabelData()}); operasi domain lain ({@code service()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Tab
 */
public class MyTabConfig extends Tab {

	private String labelLokal = null;
	
	public MyTabConfig() {
		super();
	}

	public MyTabConfig(String label, String image) {
		super(Common.getBahasaConfig(label), image);
		this.labelLokal = label;
	}

	public MyTabConfig(String label) {
		super(Common.getBahasaConfig(label));
		this.labelLokal = label;
	}
	
	public MyTabConfig(String prefix, String label, String denganPrefix) {
		super(Common.getBahasaConfig(prefix, label));
		this.labelLokal = label;
	}

	public void setTooltiptext(String text) {
		super.setTooltiptext(Common.getBahasaConfig(text));
	}

	public void setLabel(String text) {
		super.setLabel(Common.getBahasaConfig(text));
	}

	/**
	 * Setel label TANPA menerjemahkan — untuk DATA DINAMIS. Lihat {@link MyLabelConfig#setValueData(String)}.
	 */
	public MyTabConfig setLabelData(String text) {
		this.labelLokal = text;
		super.setLabel(text);
		return this;
	}

	/**
	 * Telan error tampilan "Exactly one selected tab is required: []" (race klien vs
	 * tabbox server yang sudah kosong/dibangun ulang) -- sama seperti {@link MyTab#service}.
	 * Lihat memori project: tab-exactly-one-selected-guard.
	 */
	@Override
	public void service(AuRequest request, boolean everError) {
		try {
			super.service(request, everError);
		} catch (UiException e) {
			String msg = e.getMessage() == null ? "" : e.getMessage();
			if (msg.indexOf("Exactly one selected tab is required") >= 0) {
				return;
			}
			throw e;
		}
	}

}
