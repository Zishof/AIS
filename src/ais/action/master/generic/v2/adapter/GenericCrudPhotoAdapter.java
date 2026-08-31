package ais.action.master.generic.v2.adapter;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/**
 * Kontrak adapter untuk menangani unggah/hapus foto pada entitas yang dikelola kerangka kerja
 * CRUD generik ({@code generic/v2}). Implementasi per entitas menyediakan aturan validasi (tipe
 * konten, ukuran maksimum), lokasi/mekanisme penyimpanan berkas, serta cara penghapusan foto —
 * dipanggil dari lapisan aksi generik saat pengguna mengunggah atau menghapus foto lewat form
 * hasil generate framework ini.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudPhotoAdapter {
    /**
     * Memvalidasi berkas foto sebelum disimpan.
     *
     * @param fileName    nama berkas asli
     * @param contentType tipe MIME yang dilaporkan klien
     * @param length      ukuran berkas dalam byte
     * @param context     konteks permintaan (entitas, pengguna, sesi)
     * @return peta hasil validasi (mis. pesan galat bila tidak valid), implementasi menentukan isi
     * @throws Exception bila validasi gagal karena kesalahan teknis
     */
    Map validate(String fileName, String contentType, long length, GenericCrudRequestContext context) throws Exception;
    /**
     * Menyimpan aliran data foto untuk entitas dengan id tertentu.
     *
     * @param id          id entitas pemilik foto
     * @param input       aliran byte isi berkas
     * @param fileName    nama berkas asli
     * @param contentType tipe MIME berkas
     * @param context     konteks permintaan
     * @return referensi/URL foto yang tersimpan, sesuai implementasi
     * @throws Exception bila penyimpanan gagal
     */
    String store(Serializable id, InputStream input, String fileName, String contentType, GenericCrudRequestContext context) throws Exception;
    /**
     * Menghapus foto milik entitas dengan id tertentu.
     *
     * @param id      id entitas pemilik foto
     * @param reason  alasan penghapusan (untuk audit), boleh {@code null}
     * @param context konteks permintaan
     * @throws Exception bila penghapusan gagal
     */
    void remove(Serializable id, String reason, GenericCrudRequestContext context) throws Exception;
}
