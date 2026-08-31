package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/**
 * Kontrak opsional (paket {@code generic/v2}, framework CRUD generik action-layer) yang boleh
 * diimplementasikan oleh adapter entitas untuk menyesuaikan perilaku impor massal (mis. dari
 * Excel/CSV) di layar CRUD generik: validasi per baris dan penentuan jenis operasi (insert/
 * update/skip) yang akan dijalankan untuk baris tersebut.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudImportAdapter {
    /**
     * Memvalidasi satu baris data impor sebelum diproses.
     *
     * @param values    nilai kolom pada baris, berkunci nama field
     * @param rowNumber nomor baris pada berkas impor (untuk pesan kesalahan)
     * @param context   konteks permintaan (user, tenant, dsb.)
     * @return daftar pesan kesalahan validasi; kosong/{@code null} berarti baris valid
     * @throws Exception bila validasi gagal dijalankan (mis. kesalahan akses data)
     */
    List validateRow(Map values, int rowNumber, GenericCrudRequestContext context) throws Exception;
    /**
     * Menentukan jenis operasi impor (mis. {@code "INSERT"}/{@code "UPDATE"}/{@code "SKIP"}) yang
     * berlaku untuk satu baris data, biasanya berdasarkan kecocokan kunci alami dengan data yang
     * sudah ada.
     *
     * @param values  nilai kolom pada baris, berkunci nama field
     * @param context konteks permintaan (user, tenant, dsb.)
     * @return kode operasi yang akan dijalankan untuk baris tersebut
     * @throws Exception bila penentuan operasi gagal (mis. kesalahan akses data)
     */
    String resolveOperation(Map values, GenericCrudRequestContext context) throws Exception;
}
