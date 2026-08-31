package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;

/**
 * Kontrak kebijakan otorisasi & batasan untuk fitur audit/restore (pengembalian riwayat perubahan,
 * berbasis Envers) pada framework CRUD generik v2 (action-layer). Implementasi menentukan siapa
 * yang boleh melihat audit global, memulihkan satu field/revisi/rekaman terhapus, atau melakukan
 * restore massal, sekaligus batasan kedalaman restore berantai (deep restore) dan jumlah maksimum
 * item pada restore massal, serta daftar properti yang harus diabaikan saat restore.
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudRestorePolicy {
    /** @return {@code true} bila konteks pemanggil berhak melihat log audit global (lintas rekaman). */
    boolean canViewGlobalAudit(GenericCrudRequestContext context) throws Exception;
    /** @return {@code true} bila konteks pemanggil berhak memulihkan nilai satu {@code property} pada rekaman {@code id} ke {@code revision} tertentu. */
    boolean canRestoreField(GenericCrudRequestContext context, Serializable id, String property, Number revision) throws Exception;
    /** @return {@code true} bila konteks pemanggil berhak memulihkan seluruh rekaman {@code id} ke {@code revision}; {@code deep} menandai apakah relasi turunan ikut dipulihkan. */
    boolean canRestoreRevision(GenericCrudRequestContext context, Serializable id, Number revision, boolean deep) throws Exception;
    /** @return {@code true} bila konteks pemanggil berhak memulihkan rekaman {@code id} yang sudah terhapus (soft-delete) ke {@code revision} tertentu. */
    boolean canRestoreDeletedRecord(GenericCrudRequestContext context, Serializable id, Number revision) throws Exception;
    /** @return {@code true} bila konteks pemanggil berhak melakukan restore massal terhadap rekaman yang cocok {@code filters}; {@code deep} menandai restore berantai. */
    boolean canMassRestore(GenericCrudRequestContext context, Map filters, boolean deep) throws Exception;
    /** @return batas maksimum kedalaman rantai relasi yang ikut dipulihkan pada deep restore. */
    int getMaximumDeepRestoreDepth();
    /** @return jumlah maksimum rekaman yang boleh diproses dalam satu operasi restore massal. */
    int getMaximumMassRestoreItems();
    /** @return nama-nama properti yang tidak boleh/tidak perlu ikut dipulihkan (mis. field turunan/audit). */
    String[] getIgnoredRestoreProperties();
}
