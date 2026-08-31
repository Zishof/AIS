package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/**
 * Titik ekstensi (hook) opsional pada framework generic-CRUD-v2 bagi entitas yang butuh perlakuan
 * khusus saat data-nya diekspor (mis. Excel/CSV) lewat layar CRUD generik. Implementasi kelas ini
 * didaftarkan per entitas dan dipanggil oleh mesin ekspor generik di dua titik: sekali sebelum
 * proses ekspor dimulai, dan sekali per baris data yang sudah dimasking sesuai hak akses.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudExportAdapter {
    /**
     * Dipanggil sekali sebelum ekspor dimulai, memberi kesempatan mengubah/memvalidasi filter atau
     * daftar kolom yang akan diekspor (mis. menolak kolom terlarang, menambah filter tenant).
     *
     * @param context konteks permintaan (user, entitas, parameter request)
     * @param filters filter aktif yang dipakai untuk query ekspor, dapat diubah di tempat
     * @param columns daftar kolom yang akan disertakan dalam file ekspor, dapat diubah di tempat
     * @throws Exception dapat dilempar untuk membatalkan proses ekspor
     */
    void beforeExport(GenericCrudRequestContext context, Map filters, List columns) throws Exception;
    /**
     * Dipanggil per baris data (yang sudah melalui masking hak akses standar) untuk memberi
     * kesempatan mengubah nilai sebelum ditulis ke file ekspor (mis. format ulang, decode kode
     * menjadi label).
     *
     * @param maskedRow baris data yang sudah dimasking, berupa peta nama-kolom ke nilai
     * @param context   konteks permintaan ekspor
     * @return baris data final yang akan ditulis ke file ekspor
     * @throws Exception dapat dilempar untuk membatalkan proses ekspor
     */
    Map transformRow(Map maskedRow, GenericCrudRequestContext context) throws Exception;
}
