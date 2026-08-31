package ais.ui.util;

import java.io.File;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.GeneralValueObject;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Kontrak untuk formulir data SOP (Standard Operating Procedure) yang dapat ditempel ke alur
 * disposisi/persetujuan surat SOP di AIS (lihat pemakaian di {@code TicketFormSop} dan
 * kelas-kelas aksi SOP lain seperti asset, sirkulasi surat, SPI, SPMI, PMB). Setiap jenis
 * dokumen SOP menyediakan implementasinya sendiri, memungkinkan mesin disposisi SOP menampilkan
 * form entri, menyimpan data, mencetak dokumen, dan mengetahui kelas data terkait secara
 * seragam tanpa bergantung pada jenis dokumen SOP spesifik.
 */
public interface FormSop {

	/**
	 * Membangun dan mengembalikan grid/form entri data untuk satu disposisi SOP.
	 *
	 * @param generalValueObject objek data SOP yang sedang diproses
	 * @param disposisiSop       record disposisi SOP terkait (langkah persetujuan saat ini)
	 * @param save                konfigurasi tombol simpan yang akan ditempel ke form
	 * @param setujui             listener yang dijalankan saat aksi persetujuan dipicu
	 * @return grid ({@link MyGrid}) berisi komponen form data SOP
	 * @throws Exception diteruskan bila terjadi kegagalan saat membangun form
	 */
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception;

	/**
	 * Menyimpan data yang diisi pada form SOP.
	 *
	 * @param event event ZK pemicu penyimpanan (mis. klik tombol simpan)
	 * @return {@code true} bila penyimpanan berhasil; {@code false} bila dibatalkan/gagal validasi
	 * @throws Exception diteruskan bila terjadi kegagalan saat menyimpan
	 */
	public boolean onSave(Event event) throws Exception;

	/**
	 * Mengembalikan istilah/nama tampilan untuk jenis dokumen SOP ini (dipakai sebagai label
	 * di layar disposisi).
	 *
	 * @return teks istilah/nama jenis dokumen SOP
	 * @throws Exception diteruskan bila terjadi kegagalan
	 */
	public String istilah() throws Exception;

	/**
	 * Mengambil objek data SOP ({@link DataSop}) yang sedang aktif diproses form ini.
	 *
	 * @return objek data SOP terkait
	 * @throws Exception diteruskan bila terjadi kegagalan pengambilan
	 */
	public DataSop ambil() throws Exception;

	/**
	 * Mengembalikan kelas entitas data SOP yang ditangani implementasi ini, dipakai mesin
	 * disposisi untuk keperluan refleksi/query generik.
	 *
	 * @return kelas entitas data SOP
	 * @throws Exception diteruskan bila terjadi kegagalan
	 */
	@SuppressWarnings("rawtypes")
	public Class ambilClass() throws Exception;

	/**
	 * Menandai status persetujuan pada form (mis. mengubah form menjadi mode hanya-baca atau
	 * menampilkan elemen khusus persetujuan) sesuai apakah langkah disposisi saat ini adalah
	 * langkah persetujuan.
	 *
	 * @param persetujuan {@code true} bila form sedang berada pada tahap persetujuan
	 */
	public void setPersetujuan(boolean persetujuan);

	/**
	 * Mencetak data SOP menjadi berkas (umumnya PDF) untuk diunduh/dilampirkan.
	 *
	 * @param generalValueObject objek data SOP yang akan dicetak
	 * @return berkas hasil cetak
	 * @throws Exception diteruskan bila terjadi kegagalan saat mencetak
	 */
	public File cetakData(GeneralValueObject generalValueObject) throws Exception;
}
