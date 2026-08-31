package ais.action.master.generic.v2.adapter;

import org.hibernate.Criteria;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;

/**
 * Kontrak pembatas cakupan data (row-level scoping) untuk framework CRUD generik
 * {@code ais.action.master.generic.v2}: implementasi menyisipkan pembatasan (mis. berdasarkan unit
 * kerja, cabang, atau tahun ajaran pengguna yang login) ke query Hibernate agar operasi baca/hitung
 * hanya menyentuh baris yang berada dalam wewenang {@link GenericCrudRequestContext} yang sedang
 * aktif, dan memvalidasi bahwa satu objek tertentu memang berada dalam cakupan tersebut sebelum
 * diizinkan diakses/diubah.
 */
public interface GenericCrudScopeAdapter {
    /**
     * Menambahkan kriteria pembatas cakupan ke query pengambilan daftar/detail.
     *
     * @param criteria kriteria Hibernate yang akan dimodifikasi di tempat
     * @param context  konteks permintaan CRUD generik yang sedang berjalan (identitas pengguna, dsb.)
     * @throws Exception diteruskan apa adanya bila penerapan cakupan gagal
     */
    void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    /**
     * Menambahkan kriteria pembatas cakupan ke query penghitungan jumlah baris (biasanya untuk
     * paging), harus konsisten dengan {@link #applyReadScope} agar total dan data yang ditampilkan
     * tidak berbeda cakupannya.
     *
     * @param criteria kriteria Hibernate proyeksi hitung yang akan dimodifikasi di tempat
     * @param context  konteks permintaan CRUD generik yang sedang berjalan
     * @throws Exception diteruskan apa adanya bila penerapan cakupan gagal
     */
    void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    /**
     * Memvalidasi bahwa satu objek data tertentu berada dalam cakupan yang diizinkan bagi
     * {@code context} — dipakai sebelum operasi tulis/hapus pada satu baris spesifik untuk mencegah
     * pengguna memodifikasi data di luar wewenangnya walau tahu id-nya.
     *
     * @param object  objek data yang divalidasi
     * @param context konteks permintaan CRUD generik yang sedang berjalan
     * @throws Exception dilempar (biasanya) bila objek berada di luar cakupan yang diizinkan
     */
    void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception;
}
