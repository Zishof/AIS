package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/**
 * Kontrak opsional (paket {@code generic/v2}, framework CRUD generik action-layer) untuk
 * mengatur kebijakan penghapusan permanen (hard delete, berbeda dari soft-delete/nonaktifkan
 * biasa) satu entitas pada layar CRUD generik. Adapter entitas yang mengimplementasikan
 * antarmuka ini dapat menambahkan pengaman berlapis: pemeriksaan kelayakan hapus, pratinjau
 * dampak (preflight), konfirmasi ketik-ulang teks tertentu, kewajiban mengisi alasan, serta hook
 * sebelum/sesudah penghapusan untuk keperluan audit.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudPermanentDeletePolicy {
    /** Menentukan apakah kebijakan hapus permanen ini aktif untuk entitas terkait. */
    boolean isEnabled();
    /** Memeriksa apakah baris aktif tertentu boleh dihapus permanen (mis. tidak punya relasi transaksi yang bergantung padanya). */
    boolean canDeleteActiveRow(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    /** Menyusun ringkasan dampak (preflight) yang ditampilkan ke pengguna sebelum konfirmasi hapus, mis. jumlah data terkait yang akan ikut terpengaruh. */
    Map buildPreflight(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    /** Mengembalikan teks konfirmasi yang harus diketik ulang pengguna untuk menyetujui penghapusan, atau {@code null} bila tidak diwajibkan. */
    String getTypedConfirmation(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    /** Menentukan apakah pengguna wajib mengisi alasan penghapusan. */
    boolean isReasonRequired();
    /** Hook yang dipanggil tepat sebelum baris dihapus permanen, untuk validasi/audit tambahan. */
    void beforeDelete(GenericCrudRequestContext context, Serializable id, Object activeObject) throws Exception;
    /** Hook yang dipanggil setelah baris berhasil dihapus permanen, menerima salinan data sebelum hapus (dengan field sensitif disamarkan) untuk keperluan audit. */
    void afterDelete(GenericCrudRequestContext context, Serializable id, Map beforeMaskedSnapshot) throws Exception;
}
