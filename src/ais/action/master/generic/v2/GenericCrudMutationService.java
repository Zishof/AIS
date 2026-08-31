package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;

import ais.common.CommonPrivilages;
import ais.action.master.generic.v2.adapter.GenericCrudSessionValueAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Mesin mutasi kanonik framework CRUD generik ({@code generic/v2}): satu-satunya tempat yang
 * benar-benar membuat, mengubah, atau menonaktifkan (soft delete) baris entitas lewat definisi
 * {@link GenericCrudDefinition}/adapter entitas. Setiap operasi mengikuti urutan pengaman yang
 * sama: (1) periksa hak akses lewat {@link GenericCrudPrivilegeGuard}; (2) pastikan mode CRUD
 * penuh dan operasi terkait diaktifkan pada definisi entitas (gagal-tertutup bila tidak); (3)
 * verifikasi metadata Hibernate lewat {@link GenericCrudRuntimeMetadataVerifier}; (4) saring nilai
 * yang dikirim klien agar hanya field yang ditandai boleh-diisi/diubah yang diterima (lihat
 * {@link #allowedValues}); (5) jalankan validasi wajib-isi dan validasi khusus adapter; (6)
 * jalankan operasi dalam satu transaksi Hibernate, dengan pemeriksaan cakupan (tenant/pemilik)
 * lewat {@link GenericCrudScopeGuard} sebelum DAN sesudah adapter mengaplikasikan nilai (mencegah
 * adapter memindahkan record ke luar cakupan aktif); (7) catat aktivitas lewat
 * {@link CommonPrivilages#saveActivity}; (8) pada kegagalan, rollback transaksi dan petakan galat
 * ke {@link GenericCrudException} lewat {@link #mapMutationError}. Kelas ini tidak menyimpan
 * state antar-panggilan (semua guard/validator dibuat sekali sebagai bidang final, dipakai ulang
 * untuk setiap operasi).
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudMutationService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();
    private final GenericCrudValidationService validation = new GenericCrudValidationService();

    /**
     * Membuat baris entitas baru sesuai definisi CRUD generik pada {@code context}.
     *
     * @param context   konteks permintaan (definisi entitas, user, cakupan)
     * @param submitted nilai field yang dikirim klien (kunci = nama properti)
     * @return hasil sukses berisi id baris baru, atau hasil galat validasi/bisnis
     * @throws Exception bila operasi buat dinonaktifkan, field tidak diizinkan, atau kegagalan lain (dipetakan ke {@link GenericCrudException})
     */
    public GenericCrudResult create(GenericCrudRequestContext context, Map submitted) throws Exception {
        privilege.require(context, GenericCrudOperation.CREATE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isCreateEnabled()) { deny("CREATE_DISABLED", "Pembuatan data belum diaktifkan."); }
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        Map values = allowedValues(definition, submitted, true);
        List errors = validation.validateRequired(definition, values, true);
        definition.getAdapter().validateCreate(values, context, errors);
        if (!errors.isEmpty()) { return validationError(errors); }
        Session session = HibernateUtil.currentNativeSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = definition.getAdapter().createNew(context);
            if (definition.getAdapter() instanceof GenericCrudSessionValueAdapter) {
                ((GenericCrudSessionValueAdapter) definition.getAdapter()).applyCreateValues(session, target, values, context);
            } else {
                definition.getAdapter().applyCreateValues(target, values, context);
            }
            scope.validateObject(target, context);
            definition.getAdapter().beforeSave(session, target, context);
            if (!session.contains(target)) session.save(target);
            session.flush();
            definition.getAdapter().afterSave(session, target, context);
            Serializable id = metadata.getIdentifier(target, EntityMode.POJO);
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.CREATE, target, "Generic CRUD V2");
            return GenericCrudResult.ok("Data berhasil disimpan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { HibernateUtil.closeSession(); }
    }

    /**
     * Memperbarui baris entitas yang sudah ada, dengan pemeriksaan token optimistic-locking bila
     * definisi entitas memiliki kolom versi (lihat {@link #verifyOptimisticToken}) dan pemeriksaan
     * cakupan ganda (sebelum dan sesudah nilai diterapkan) agar adapter tidak dapat memindahkan
     * record ke luar cakupan aktif.
     *
     * @param context         konteks permintaan (definisi entitas, user, cakupan)
     * @param id              id baris yang akan diubah
     * @param submitted       nilai field yang dikirim klien
     * @param optimisticToken token versi terakhir yang dilihat klien, atau {@code null} bila entitas tidak bervensi
     * @return hasil sukses berisi id baris, atau hasil galat validasi/bisnis
     * @throws Exception bila baris tidak ditemukan, operasi ubah dinonaktifkan, token versi tidak cocok, atau kegagalan lain
     */
    public GenericCrudResult update(GenericCrudRequestContext context, Serializable id, Map submitted, Object optimisticToken) throws Exception {
        privilege.require(context, GenericCrudOperation.UPDATE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isUpdateEnabled()) { deny("UPDATE_DISABLED", "Perubahan data belum diaktifkan."); }
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        Map values = allowedValues(definition, submitted, false);
        List errors = validation.validateRequired(definition, values, false);
        Session session = HibernateUtil.currentNativeSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = (GeneralValueObject) session.get(definition.getEntityClass(), id);
            if (target == null) { throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data tidak ditemukan."); }
            scope.validateObject(target, context);
            verifyOptimisticToken(definition, metadata, target, optimisticToken);
            definition.getAdapter().validateUpdate(target, values, context, errors);
            if (!errors.isEmpty()) { rollback(tx); return validationError(errors); }
            if (definition.getAdapter() instanceof GenericCrudSessionValueAdapter) {
                ((GenericCrudSessionValueAdapter) definition.getAdapter()).applyUpdateValues(session, target, values, context);
            } else {
                definition.getAdapter().applyUpdateValues(target, values, context);
            }
            // Cegah perubahan relasi tenant/pemilik untuk memindahkan record ke luar scope aktif.
            scope.validateObject(target, context);
            definition.getAdapter().beforeSave(session, target, context);
            session.saveOrUpdate(target);
            session.flush();
            definition.getAdapter().afterSave(session, target, context);
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.UPDATE, target, "Generic CRUD V2");
            return GenericCrudResult.ok("Perubahan berhasil disimpan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { HibernateUtil.closeSession(); }
    }

    /**
     * Menonaktifkan (soft delete) satu baris entitas, hanya bila adapter entitas mengizinkannya
     * lewat {@code canDelete} (mis. entitas tidak memiliki data anak yang masih aktif).
     *
     * @param context konteks permintaan (definisi entitas, user, cakupan)
     * @param id      id baris yang akan dinonaktifkan
     * @return hasil sukses berisi id baris, atau melempar galat bila dilarang
     * @throws Exception bila baris tidak ditemukan, operasi hapus dinonaktifkan, adapter menolak penghapusan, atau kegagalan lain
     */
    public GenericCrudResult softDelete(GenericCrudRequestContext context, Serializable id) throws Exception {
        privilege.require(context, GenericCrudOperation.DELETE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isDeleteEnabled()) { deny("DELETE_DISABLED", "Penghapusan belum diaktifkan."); }
        GenericCrudRuntimeMetadataVerifier.verify(definition);
        Session session = HibernateUtil.currentNativeSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = (GeneralValueObject) session.get(definition.getEntityClass(), id);
            if (target == null) { throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data tidak ditemukan."); }
            scope.validateObject(target, context);
            List reasons = new ArrayList();
            if (!definition.getAdapter().canDelete(target, context, reasons)) {
                throw new GenericCrudException(409, "DELETE_BLOCKED", reasons.isEmpty() ? "Data tidak dapat dinonaktifkan." : String.valueOf(reasons.get(0)));
            }
            definition.getAdapter().delete(session, target, context);
            session.flush();
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.DELETE, target, "Soft delete Generic CRUD V2");
            return GenericCrudResult.ok("Data berhasil dinonaktifkan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { HibernateUtil.closeSession(); }
    }

    /** Menyaring {@code submitted} agar hanya berisi field yang ditandai boleh-diisi ({@code create=true}) atau boleh-diubah; melempar galat bila ada field yang tidak diizinkan. */
    private Map allowedValues(GenericCrudDefinition definition, Map submitted, boolean create) throws GenericCrudException {
        Map values = new LinkedHashMap();
        Iterator iterator = submitted.keySet().iterator();
        while (iterator.hasNext()) {
            String key = String.valueOf(iterator.next());
            GenericCrudFieldDefinition field = definition.getField(key);
            if (field == null || (create ? !field.isCreateable() : !field.isUpdateable())) {
                throw new GenericCrudException(400, "FIELD_NOT_ALLOWED", "Field " + key + " tidak diizinkan untuk operasi ini.");
            }
            values.put(key, submitted.get(key));
        }
        return values;
    }

    /** Memastikan {@code token} yang dikirim klien cocok dengan nilai kolom versi entitas saat ini; tidak melakukan apa pun bila entitas tidak memiliki kolom versi. */
    private void verifyOptimisticToken(GenericCrudDefinition definition, ClassMetadata metadata,
            Object target, Object token) throws GenericCrudException {
        if (definition.getVersionProperty() == null) { return; }
        if (token == null) { throw new GenericCrudException(409, "OPTIMISTIC_TOKEN_REQUIRED", "Token versi wajib disertakan."); }
        Object current = metadata.getPropertyValue(target, definition.getVersionProperty(), EntityMode.POJO);
        if (current == null || !String.valueOf(current).equals(String.valueOf(token))) {
            throw new GenericCrudException(409, "OPTIMISTIC_CONFLICT", "Data telah berubah. Muat ulang sebelum menyimpan.");
        }
    }

    /** Mengubah daftar pesan galat mentah ({@code "field:pesan"} atau pesan tanpa field) menjadi {@link GenericCrudResult} gagal dengan peta {@code fieldErrors} siap tampil di formulir. */
    private GenericCrudResult validationError(List errors) {
        GenericCrudResult result = GenericCrudResult.error("VALIDATION_FAILED", "Periksa kembali data yang diisi.");
        Map fieldErrors = new LinkedHashMap();
        for (int i = 0; i < errors.size(); i++) {
            String text = String.valueOf(errors.get(i));
            int separator = text.indexOf(':');
            fieldErrors.put(separator < 0 ? "_global" : text.substring(0, separator), separator < 0 ? text : text.substring(separator + 1));
        }
        result.setFieldErrors(fieldErrors);
        return result;
    }

    private Map idData(Serializable id) { Map result = new LinkedHashMap(); result.put("id", id); return result; }
    private void deny(String code, String message) throws GenericCrudException { throw new GenericCrudException(403, code, message); }
    private void rollback(Transaction tx) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } }
    /** Memetakan galat mentah ke {@link GenericCrudException}: diteruskan apa adanya bila sudah bertipe itu, dipetakan sebagai pelanggaran aturan bisnis (409) untuk {@link IllegalArgumentException}, atau sebagai kegagalan generik (500) untuk galat lainnya. */
    private GenericCrudException mapMutationError(Exception error) {
        if (error instanceof GenericCrudException) { return (GenericCrudException) error; }
        if (error instanceof IllegalArgumentException) { return new GenericCrudException(409, "BUSINESS_RULE", error.getMessage()); }
        return new GenericCrudException(500, "MUTATION_FAILED", "Operasi gagal dan transaksi dibatalkan.", error);
    }
}
