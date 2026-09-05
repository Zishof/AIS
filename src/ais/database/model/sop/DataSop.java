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
	 * Nomor versi serialisasi untuk kontrak {@link java.io.Serializable} yang diwarisi dari
	 * {@link GeneralValueObject}. Dipertahankan konstan agar instance subclass yang pernah
	 * diserialisasi tetap dapat dibaca kembali.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Kontrak wajib bagi seluruh subclass {@code DataSop}: setiap baris data yang berasosiasi
	 * dengan mesin SOP (mis. lampiran, catatan, atau data tambahan yang melekat pada satu
	 * langkah proses) harus dapat menunjuk balik ke {@link DisposisiSop} pemiliknya — instance
	 * pengajuan SOP konkret yang sedang berjalan. Relasi ini yang memungkinkan data turunan
	 * tersebut dikaitkan kembali ke SOP dan pengajuan yang benar saat ditampilkan/ditelusuri.
	 *
	 * @return {@link DisposisiSop} yang menjadi induk/pemilik baris data ini, atau {@code null}
	 *         bila belum/tidak diasosiasikan.
	 */
	public abstract DisposisiSop getDisposisiSop();

	/**
	 * @param disposisiSop {@link DisposisiSop} induk/pemilik baru untuk baris data ini; lihat
	 *                      javadoc {@link #getDisposisiSop()}.
	 */
	public abstract void setDisposisiSop(DisposisiSop disposisiSop);
}
