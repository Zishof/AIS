package ais.database.model.sop;

import ais.database.model.GeneralValueObject;

/**
 * Model data untuk data sop. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getDisposisiSop()}, {@code
 * setDisposisiSop}(). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 *
 * @see GeneralValueObject
 */
public abstract class DataSop extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public abstract DisposisiSop getDisposisiSop();

	public abstract void setDisposisiSop(DisposisiSop disposisiSop);
}
