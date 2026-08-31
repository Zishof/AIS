package ais.database.model;

import ais.database.model.sop.DataSop;

/**
 * Model data untuk vo kunci. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * DataSop}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah operasi lokal: {@code getDikunci()}, {@code setDikunci}(). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * @see DataSop
 */
public abstract class VoKunci extends DataSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public abstract Tbmuser getDikunci();

	public abstract void setDikunci(Tbmuser dikunci);
}
