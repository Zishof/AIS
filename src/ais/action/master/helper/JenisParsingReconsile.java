package ais.action.master.helper;

import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.file.LampiranLain;

/**
 * Kontrak strategi parsing untuk satu jenis format file rekonsiliasi host-to-host
 * (mis. laporan mutasi/transaksi dari bank tertentu yang diunggah sebagai lampiran).
 * Setiap implementasi menangani satu format/bank spesifik yang dirujuk oleh
 * {@link ais.database.model.JenisRekonsiliasiHostToHost}, sehingga proses rekonsiliasi
 * dapat memilih parser yang sesuai secara dinamis tanpa mengubah kode pemanggil.
 */
public interface JenisParsingReconsile {

	/**
	 * Mem-parsing isi {@code lampiranLain} sesuai format yang berlaku untuk
	 * {@code jenisRekonsiliasiHostToHost} dan menerapkan hasilnya (mis. menyimpan data
	 * transaksi/mutasi hasil parsing ke database).
	 *
	 * @param lampiranLain                  berkas lampiran yang akan diparsing
	 * @param jenisRekonsiliasiHostToHost    jenis/konfigurasi rekonsiliasi yang menentukan format parsing
	 * @throws Exception diteruskan apa adanya bila parsing atau penyimpanan gagal
	 */
	public void parsing(LampiranLain lampiranLain, JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost) throws Exception;
}
