package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Combobox;


/**
 * Komponen/konfigurasi ZK khusus AIS untuk my combobox. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Combobox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah inisialisasi/lifecycle ({@code initDefaultSearchable()}); mutasi
 * data ({@code setSelectedIndex()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Combobox
 */
public class MyCombobox extends Combobox {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7795947945271873137L;

	public MyCombobox() {
		super();
		initDefaultSearchable();
		// TODO Auto-generated constructor stub
	}

	public MyCombobox(String value) throws WrongValueException {
		super(value);
		initDefaultSearchable();
		// TODO Auto-generated constructor stub
	}
	private void initDefaultSearchable() {
		setWidth("90%");
		setReadonly(false);
		setAutodrop(true);
		String sclass = getSclass();
		if (sclass == null || sclass.indexOf("ecampus-combobox-searchable") < 0) {
			setSclass((sclass == null || sclass.trim().length() == 0) ? "ecampus-combobox-searchable" : (sclass + " ecampus-combobox-searchable"));
		}
	}

	/**
	 * Guard "Out of bound: N while size=M": ZUL/model kadang menyetel selectedIndex sebelum
	 * comboitem tersedia (mis. selectedIndex="0" saat data masih kosong / Include.afterCompose).
	 * Abaikan bila indeks di luar batas atas; ZK akan menyetel ulang saat item sudah ada.
	 * selectedIndex = -1 (deselect) dan indeks valid tetap berperilaku normal.
	 */
	@Override
	public void setSelectedIndex(int jsel) {
		if (jsel >= getItemCount()) {
			return;
		}
		super.setSelectedIndex(jsel);
	}


}
