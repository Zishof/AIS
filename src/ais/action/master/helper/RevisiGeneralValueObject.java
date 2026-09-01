package ais.action.master.helper;

import java.io.Serializable;

import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.criteria.AuditCriterion;

/**
 * Class dasar kompatibilitas (abstrak) untuk arsitektur helper revisi LAMA, sebelum seluruh
 * logika disatukan ke {@link ais.action.master.helper.GenericRevisiHelper}. Class ini
 * mendeklarasikan hook-hook abstrak gaya lama ({@link #buildSearchUI(Rows)},
 * {@link #buildGridColumns(Columns)}, {@link #renderSpecificColumns(Row, Serializable)},
 * {@link #initAuditQuery()}) yang dulu wajib diimplementasikan tiap subclass untuk merakit UI
 * pencarian, kolom grid, dan query Envers-nya sendiri secara manual.
 *
 * <p><b>Status: legacy, tidak lagi dipakai.</b> Per pemeriksaan terakhir tidak ada satu pun class
 * di codebase ini yang meng-extends {@code RevisiGeneralValueObject} — seluruh helper revisi aktif
 * sudah bermigrasi menjadi subclass langsung {@link GenericRevisiHelper} (lihat class
 * {@code Revisi*Helper} lain di package {@code ais.action.master.helper}), yang membangun window
 * lengkap 3-tab dari konstruktor saja tanpa perlu meng-override hook manual apa pun. Empat method
 * abstrak di sini TIDAK pernah dipanggil oleh {@link GenericRevisiHelper} — class ini dipertahankan
 * murni sebagai compatibility shim agar kode lama yang (bila masih ada di luar pohon sumber ini)
 * meng-extends class ini tidak langsung gagal compile. Jangan jadikan class ini contoh pola untuk
 * helper revisi baru; gunakan {@link GenericRevisiHelper} langsung.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class RevisiGeneralValueObject<T extends Serializable> extends GenericRevisiHelper<T> {

    private static final long serialVersionUID = 1L;

    /**
     * Konstruktor kompatibilitas: judul jendela otomatis dibentuk sebagai
     * {@code "Revisi untuk data " + <nama class sederhana>}.
     *
     * @param myClass class entity Hibernate yang diaudit, diteruskan ke {@link GenericRevisiHelper}
     * @param eventListener callback yang diteruskan ke {@link GenericRevisiHelper}, boleh {@code null}
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiGeneralValueObject(Class<?> myClass, EventListener eventListener) throws Exception {
        super(myClass, "Revisi untuk data " + (myClass == null ? "" : myClass.getSimpleName()), eventListener, null);
    }

    /** Hook UI pencarian gaya lama; tidak dipanggil oleh {@link GenericRevisiHelper}. Legacy, lihat Javadoc class. */
    protected abstract void buildSearchUI(Rows searchRows) throws Exception;

    /** Hook kolom grid gaya lama; tidak dipanggil oleh {@link GenericRevisiHelper}. Legacy, lihat Javadoc class. */
    protected abstract void buildGridColumns(Columns columns) throws Exception;

    /** Hook rendering kolom spesifik gaya lama; tidak dipanggil oleh {@link GenericRevisiHelper}. Legacy, lihat Javadoc class. */
    protected abstract void renderSpecificColumns(Row row, Serializable auditObj) throws Exception;

    /** Hook pembentukan {@link AuditQuery} gaya lama; tidak dipanggil oleh {@link GenericRevisiHelper}. Legacy, lihat Javadoc class. */
    protected abstract AuditQuery initAuditQuery();
}
