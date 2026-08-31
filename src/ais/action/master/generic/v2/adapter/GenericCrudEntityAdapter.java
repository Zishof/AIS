package ais.action.master.generic.v2.adapter;

import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.database.model.GeneralValueObject;

/**
 * Kontrak adaptor entitas untuk framework CRUD generik {@link ais.action.master.generic.v2}
 * (mirip peran {@code GenericDao}, tetapi untuk lapisan aksi/workflow, bukan akses data mentah).
 * Setiap entitas yang ingin diekspos lewat layar CRUD generik menyediakan satu implementasi
 * antarmuka ini yang mengatur validasi, penerapan nilai, kunci alami, dan aksi kustom khusus
 * entitas tersebut — sehingga logika bisnis per entitas tetap terisolasi sementara alur
 * create/update/delete/import umum ditangani seragam oleh mesin generik.
 *
 * <p>
 * Beberapa entitas dengan workflow native yang kompleks (mis. Pembelian, penyusutan aset —
 * lihat paket {@code test}) sengaja menutup total create/update/delete/import generik pada
 * {@link #configure} dan hanya mengekspos entitasnya untuk keperluan tampilan/kunci alami,
 * memaksa perubahan data tetap lewat jalur workflow native aplikasi.
 * </p>
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudEntityAdapter<T extends GeneralValueObject> {
    /** Mengatur {@code definition} (field, kunci alami, flag create/update/delete/import) sesuai kebutuhan entitas ini. */
    void configure(GenericCrudDefinition definition) throws Exception;
    /** Menambahkan filter baku (mis. tenant/scope) ke {@code criteria} pencarian berdasarkan {@code context}. */
    void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) throws Exception;
    /** Membuat instance baru entitas ini, terisi nilai default sesuai {@code context}, sebelum diisi nilai form. */
    T createNew(GenericCrudRequestContext context) throws Exception;
    /** Memvalidasi {@code values} untuk operasi create, menambahkan pesan ke {@code fieldErrors} bila tidak valid. */
    void validateCreate(Map values, GenericCrudRequestContext context, List fieldErrors) throws Exception;
    /** Memvalidasi {@code values} terhadap entitas {@code current} untuk operasi update, menambahkan pesan ke {@code fieldErrors} bila tidak valid. */
    void validateUpdate(T current, Map values, GenericCrudRequestContext context, List fieldErrors) throws Exception;
    /** Menerapkan {@code values} yang sudah lolos validasi ke {@code target} baru saat create. */
    void applyCreateValues(T target, Map values, GenericCrudRequestContext context) throws Exception;
    /** Menerapkan {@code values} yang sudah lolos validasi ke {@code target} yang sudah ada saat update. */
    void applyUpdateValues(T target, Map values, GenericCrudRequestContext context) throws Exception;
    /** Dipanggil tepat sebelum {@code target} disimpan dalam sesi/transaksi {@code session}; tempat menyisipkan efek samping pra-simpan. */
    void beforeSave(Session session, T target, GenericCrudRequestContext context) throws Exception;
    /** Dipanggil tepat setelah {@code target} berhasil disimpan; tempat menyisipkan efek samping pasca-simpan (mis. audit, notifikasi). */
    void afterSave(Session session, T target, GenericCrudRequestContext context) throws Exception;
    /** Mengecek apakah {@code target} boleh dihapus, mengisi {@code blockingReasons} dengan alasan penolakan bila tidak boleh. */
    boolean canDelete(T target, GenericCrudRequestContext context, List blockingReasons) throws Exception;
    /** Menghapus {@code target} dalam sesi/transaksi {@code session}, termasuk pembersihan data terkait bila perlu. */
    void delete(Session session, T target, GenericCrudRequestContext context) throws Exception;
    /** Mengembalikan daftar nama properti yang bersama-sama membentuk kunci alami (natural key) entitas ini. */
    List getNaturalKeyProperties();
    /** Menjalankan aksi kustom {@code actionKey} (di luar create/update/delete baku) atas {@code selectedIds} dengan {@code parameters} tambahan. */
    GenericCrudResult executeCustomAction(String actionKey, List selectedIds, Map parameters, GenericCrudRequestContext context) throws Exception;
}
