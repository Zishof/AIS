package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Doublebox;


/**
 * Komponen/konfigurasi ZK khusus AIS untuk my doublebox. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Doublebox}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code boolean paksa}; inisialisasi/lifecycle
 * ({@code init()}); mutasi data ({@code setValue()}, {@code setDisabled()}); operasi domain lain ({@code
 * disabledPaksa()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Doublebox
 */
public class MyDoublebox extends Doublebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;
	private boolean paksa = false;

	public MyDoublebox() {
		super();
		init();
	}

	public MyDoublebox(Double value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		/*
		 * Lebar otomatis mengikuti lebar wadah/kolom: pakai 85% dengan !important agar
		 * mengalahkan lebar tetap bawaan tema ZK, sehingga kotak input menyesuaikan
		 * ukuran kolom yang tersedia (tidak lagi sempit di kolom yang lebar).
		 *
		 * Catatan: bila pemanggil menimpa style via setStyle(...) setelah konstruksi,
		 * sertakan kembali "width:85% !important;" agar lebar tetap mengikuti kolom.
		 */
		setStyle("text-align: right; width:85% !important;");
		setFormat("#,##0.####");
	}

	public void setValue(Double val) {
		super.setValue(val);
	}

	public void disabledPaksa(boolean paksa) {
		this.paksa = paksa;
		super.setDisabled(paksa);
	}

	public void setDisabled(boolean disabled) {
		if (paksa) {
			super.setDisabled(true);
		} else {
			super.setDisabled(disabled);
		}
	}

}
