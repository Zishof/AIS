package ais.action.master.helper;

import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.model.CicilanPembayaran;
import ais.database.model.KegiatanTemporary;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Helper riwayat revisi (Envers audit trail) untuk entitas {@link CicilanPembayaran} yang terkait
 * dengan Kegiatan Temporary (mis. kegiatan PPDB/Temporary sebelum data resmi dipindah). Kelas ini
 * murni memasang parameter konstruksi ke {@link GenericRevisiHelper} — judul jendela, daftar kolom
 * yang ditampilkan ({@code keterangan}, {@code kode}, {@code nama}), dan opsional
 * {@link GenericRevisiHelper.QueryCustomizer} untuk membatasi hasil audit hanya pada satu atau
 * beberapa {@link KegiatanTemporary}. Tidak ada logika query/agregasi tambahan di luar itu; seluruh
 * mekanisme baca-audit (Hibernate Envers {@link AuditReader}/{@link AuditQuery}) ditangani oleh
 * kelas induk.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class RevisiCicilanPembayaranTemporaryHelper extends GenericRevisiHelper<CicilanPembayaran> {

    private static final long serialVersionUID = 6589578552710016753L;

    /** Menampilkan seluruh riwayat revisi {@link CicilanPembayaran} tanpa penyaringan berdasarkan Kegiatan Temporary. */
    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener) throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" });
    }

    /**
     * Menampilkan riwayat revisi {@link CicilanPembayaran} yang disaring hanya untuk satu
     * {@link KegiatanTemporary}. Bila {@code kegiatanTemporary} bernilai {@code null}, penyaring
     * tidak diterapkan (perilaku sama seperti konstruktor tanpa filter).
     */
    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener, final KegiatanTemporary kegiatanTemporary)
            throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" }, new QueryCustomizer() {
                    public void apply(Session session, AuditQuery query) throws Exception {
                        if (kegiatanTemporary != null) {
                            query.add(AuditEntity.property("kegiatanTemporary").eq(kegiatanTemporary));
                        }
                    }
                });
    }

    /**
     * Menampilkan riwayat revisi {@link CicilanPembayaran} yang disaring untuk sekumpulan
     * {@link KegiatanTemporary} (kondisi OR antar anggota set). Bila {@code kegiatanTemporaries}
     * bernilai {@code null}, tidak ada penyaring diterapkan (semua riwayat tampil). Bila set kosong
     * atau seluruh anggotanya {@code null}, query sengaja dipaksa tidak menghasilkan baris apa pun
     * (filter id {@code = -2}) alih-alih menampilkan semua riwayat secara keliru.
     */
    public RevisiCicilanPembayaranTemporaryHelper(EventListener eventListener,
            final Set<KegiatanTemporary> kegiatanTemporaries) throws Exception {
        super(CicilanPembayaran.class, "Revisi Cicilan Pembayaran Temporary", eventListener,
                new String[] { "keterangan", "kode", "nama" }, new QueryCustomizer() {
                    public void apply(Session session, AuditQuery query) throws Exception {
                        if (kegiatanTemporaries == null) {
                            return;
                        }
                        if (kegiatanTemporaries.isEmpty()) {
                            query.add(AuditEntity.id().eq(Long.valueOf(-2L)));
                            return;
                        }
                        AuditCriterion c = null;
                        for (KegiatanTemporary kegiatanTemporary : kegiatanTemporaries) {
                            if (kegiatanTemporary == null) {
                                continue;
                            }
                            AuditCriterion next = AuditEntity.property("kegiatanTemporary").eq(kegiatanTemporary);
                            c = c == null ? next : AuditEntity.or(c, next);
                        }
                        if (c == null) {
                            query.add(AuditEntity.id().eq(Long.valueOf(-2L)));
                        } else {
                            query.add(c);
                        }
                    }
                });
    }
}
