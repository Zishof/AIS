package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/**
 * Kontrak adapter audit/revisi untuk kerangka CRUD generik {@code generic/v2}: menyediakan akses
 * histori revisi entitas (mis. berbasis Hibernate Envers) — daftar revisi global/per baris,
 * perbandingan dua revisi, pengembalian (restore) satu field atau seluruh baris ke revisi tertentu
 * (opsional "deep" mencakup entitas terkait), koreksi manual satu field dengan alasan audit, serta
 * operasi massal "restore ke kondisi pada tanggal tertentu" (baik pratinjau maupun dieksekusi
 * asinkron via antrean, diidentifikasi lewat {@code idempotencyKey} agar aman diulang).
 *
 * <p>
 * Method {@code supports*} memungkinkan implementasi memberi tahu action-layer kapabilitas mana yang
 * benar-benar didukung (audit tersedia, restore per-field, restore per-revisi, restore mendalam,
 * restore massal) sehingga UI dapat menyembunyikan aksi yang tidak relevan untuk entitas tertentu.
 * Parameter {@code token} pada operasi yang mengubah data umumnya dipakai sebagai token konfirmasi/
 * verifikasi (mis. OTP atau token aksi sensitif), dan {@code reason} sebagai catatan alasan yang
 * disimpan pada jejak audit.
 * </p>
 */
@SuppressWarnings("rawtypes")
public interface GenericCrudAuditRevisionAdapter {
    /** @return {@code true} bila entitas ini memiliki jejak audit/revisi yang bisa ditelusuri. */
    boolean supportsAudit();
    /** @return {@code true} bila pengembalian (restore) satu field tunggal ke nilai pada revisi lampau didukung. */
    boolean supportsFieldRestore();
    /** @return {@code true} bila pengembalian seluruh baris ke satu revisi tertentu didukung. */
    boolean supportsRevisionRestore();
    /** @return {@code true} bila restore "deep" (mencakup entitas terkait, bukan hanya baris itu sendiri) didukung. */
    boolean supportsDeepRestore();
    /** @return {@code true} bila operasi restore massal (banyak baris sekaligus berdasarkan tanggal/filter) didukung. */
    boolean supportsMassRestore();
    /** Mendaftar revisi lintas seluruh baris entitas (bukan satu baris tertentu), difilter/diurut/dipaginasi. */
    List listGlobalRevisions(GenericCrudRequestContext context, Map filters, int first, int max, String sort, boolean ascending) throws Exception;
    /** Mendaftar riwayat revisi milik satu baris ber-{@code id}, difilter dan dipaginasi. */
    List listRowRevisions(GenericCrudRequestContext context, Serializable id, Map filters, int first, int max) throws Exception;
    /** Membandingkan dua nomor revisi ({@code left} vs {@code right}) milik baris ber-{@code id}, mengembalikan peta perbedaan per field. */
    Map compareRevisions(GenericCrudRequestContext context, Serializable id, Number left, Number right) throws Exception;
    /** Mengembalikan nilai satu {@code property} pada baris ber-{@code id} ke nilainya pada {@code revision} tertentu. */
    GenericCrudResult restoreField(GenericCrudRequestContext context, Serializable id, Number revision, String property, Object token, String reason) throws Exception;
    /** Mengoreksi nilai satu {@code property} secara manual (bukan restore dari revisi lampau) pada baris ber-{@code id}, dicatat dengan {@code reason}. */
    GenericCrudResult correctField(GenericCrudRequestContext context, Serializable id, String property, Object value, Object token, String reason) throws Exception;
    /** Mengembalikan seluruh baris ber-{@code id} ke kondisi pada {@code revision} tertentu; {@code deep=true} turut mengembalikan entitas terkait. */
    GenericCrudResult restoreRevision(GenericCrudRequestContext context, Serializable id, Number revision, boolean deep, Object token, String reason) throws Exception;
    /** Menghasilkan pratinjau (tanpa menerapkan perubahan) dampak restore massal ke kondisi terakhir sebelum tanggal {@code from}, untuk baris yang cocok {@code activeFilter}. */
    Map previewRestoreLatestFromDate(GenericCrudRequestContext context, Date from, Map activeFilter, boolean deep) throws Exception;
    /** Mengantrekan eksekusi asinkron restore massal (padanan {@link #previewRestoreLatestFromDate} yang benar-benar diterapkan), diidentifikasi lewat {@code idempotencyKey} agar permintaan ulang tidak dieksekusi ganda; mengembalikan id/token tugas antrean. */
    String enqueueRestoreLatestFromDate(GenericCrudRequestContext context, Date from, Map activeFilter, boolean deep, String reason, String idempotencyKey) throws Exception;
}
