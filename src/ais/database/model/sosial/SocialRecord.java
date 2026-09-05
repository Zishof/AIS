package ais.database.model.sosial;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * Superclass Hibernate {@code @MappedSuperclass} yang menyediakan kolom teknis bersama untuk
 * SELURUH entitas modul sosial (donasi/zakat) AIS yang bernama {@code Social*}/{@code
 * Sosial*}: identifier ({@link #getId()}), kunci penyewa multi-tenant ({@link #getTenantKey()}),
 * status siklus hidup baris ({@link #getStatus()}), dan jejak audit sederhana ({@link
 * #getCreatedAt()}/{@link #getUpdatedAt()}/{@link #getCreatedBy()}/{@link #getUpdatedBy()}).
 * Kolom-kolom ini dipetakan ulang oleh setiap subclass ke tabelnya masing-masing (pola JPA
 * {@code MappedSuperclass}, bukan {@code @Entity} tersendiri — tidak ada tabel {@code
 * social_record}). Diaudit penuh oleh Hibernate Envers ({@code @Audited}), diwariskan otomatis
 * oleh seluruh subclass.
 *
 * <p>
 * <b>Investigasi arsitektur modul sosial:</b> kelas ini adalah bukti paling konkret bahwa
 * seluruh entitas {@code Social*} (termasuk {@link SosialChannel} yang penamaannya Indonesia)
 * MEMANG satu keluarga desain baru: alih-alih memakai kolom cakupan legacy AIS ({@code
 * yayasan_id}/{@code sekolah_id}/{@code satuan_kerja_id} yang tersebar di banyak entitas induk
 * berbeda dan dipakai {@code GenericCrudAutoEntityAdapter.scopeBindings()} untuk pembatasan
 * akses), modul ini memperkenalkan SATU kolom penyewa generik {@code tenant_key} yang
 * konsisten di semua tabel {@code social_*}/{@code sosial_*} dan divalidasi manual oleh layer
 * service ({@code ais.action.master.sosial.helper.SocialPrivilegeGuard}/{@code
 * SocialRequestContext}), BUKAN oleh mekanisme scoping generik CRUD v2 milik legacy — {@code
 * scopeBindings()} pada {@code GenericCrudAutoEntityAdapter} sama sekali tidak mengenal
 * properti {@code tenantKey} (hanya {@code yayasan}/{@code sekolah}/{@code program}/{@code
 * fakultas}/{@code jurusan}/{@code satuanKerja}/{@code mahasiswa}), sehingga bila entitas modul
 * ini pernah diekspos lewat CRUD generik v2 tanpa gerbang service khusus, isolasi tenant TIDAK
 * akan ditegakkan otomatis oleh adapter generik itu (gejala pola gap tenant yang sama dengan
 * yang sudah tercatat luas di area lain aplikasi ini). Meskipun begitu, kelas ini tetap
 * mewarisi {@link GeneralValueObject} legacy (bukan value object baru yang berdiri sendiri)
 * sehingga tetap kompatibel dengan infrastruktur ORM bersama (clone/copy-dari, {@code
 * HibernateUtil}) dan jejak audit lama ({@link ais.database.hibernate.AuditTimestampInterceptor}).
 * </p>
 */
@MappedSuperclass
@Audited
public abstract class SocialRecord extends GeneralValueObject {
    private static final long serialVersionUID = 1L;
    /** Identifier baris, dibangkitkan basis data (identity column). */
    private Long id;
    /** Kunci penyewa (tenant) multi-tenant modul sosial — mekanisme isolasi data utama modul ini, dipakai/divalidasi manual oleh layer service ({@code SocialRequestContext}/{@code SocialPrivilegeGuard}), BUKAN oleh scoping generik CRUD v2 legacy. */
    private String tenantKey;
    /** Status siklus hidup baris (mis. {@code "DRAFT"}, {@code "ACTIVE"}); default {@code "DRAFT"} bila belum diset. */
    private String status;
    /** Waktu baris ini pertama kali disimpan. */
    private Date createdAt;
    /** Waktu baris ini terakhir diperbarui. */
    private Date updatedAt;
    /** Identitas pembuat baris ini. */
    private String createdBy;
    /** Identitas pengubah terakhir baris ini. */
    private String updatedBy;

    /** Mengembalikan identifier baris (identity column basis data). */
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    /** Menyetel identifier baris; umumnya tidak dipanggil langsung karena kolom dibangkitkan basis data ({@code insertable=false}). */
    public void setId(Long value) { id = value; }

    /** Mengembalikan kunci penyewa (tenant) baris ini. */
    @Column(name = "tenant_key", nullable = false, length = 120)
    public String getTenantKey() { return tenantKey; }
    /** Menyetel kunci penyewa (tenant) baris ini (dipangkas spasi awal/akhir). */
    public void setTenantKey(String value) { tenantKey = trim(value); }

    /** Mengembalikan status siklus hidup baris; default {@code "DRAFT"} bila belum diset di database. */
    @Column(name = "status", nullable = false, length = 40)
    public String getStatus() { return status == null ? "DRAFT" : status; }
    /** Menyetel status siklus hidup baris (dipangkas spasi awal/akhir). */
    public void setStatus(String value) { status = trim(value); }

    /** Mengembalikan waktu baris ini pertama kali disimpan. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
    public Date getCreatedAt() { return createdAt; }
    /** Menyetel waktu baris ini pertama kali disimpan; umumnya diisi otomatis oleh {@link #createAudit()}. */
    public void setCreatedAt(Date value) { createdAt = value; }

    /** Mengembalikan waktu baris ini terakhir diperbarui. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
    public Date getUpdatedAt() { return updatedAt; }
    /** Menyetel waktu baris ini terakhir diperbarui; umumnya diisi otomatis oleh {@link #createAudit()}/{@link #onUpdate()}. */
    public void setUpdatedAt(Date value) { updatedAt = value; }

    /** Mengembalikan identitas pembuat baris ini. */
    @Column(name = "created_by", length = 255)
    public String getCreatedBy() { return createdBy; }
    /** Menyetel identitas pembuat baris ini (dipangkas spasi awal/akhir). */
    public void setCreatedBy(String value) { createdBy = trim(value); }

    /** Mengembalikan identitas pengubah terakhir baris ini. */
    @Column(name = "updated_by", length = 255)
    public String getUpdatedBy() { return updatedBy; }
    /** Menyetel identitas pengubah terakhir baris ini (dipangkas spasi awal/akhir). */
    public void setUpdatedBy(String value) { updatedBy = trim(value); }

    /**
     * Callback JPA {@code @PrePersist}: mengisi {@link #getCreatedAt()} (bila belum diisi) dan
     * {@link #getUpdatedAt()} dengan waktu sekarang ({@link ais.ui.util.WaktuUtil#getDate()}),
     * serta memastikan {@link #getStatus()} tidak kosong (fallback {@code "DRAFT"}) sebelum
     * baris pertama kali disisipkan ke basis data.
     */
    @PrePersist protected void createAudit() {
        Date now = ais.ui.util.WaktuUtil.getDate();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null || status.trim().isEmpty()) status = "DRAFT";
    }

    /**
     * Callback JPA {@code @PreUpdate}: memperbarui {@link #getUpdatedAt()} ke waktu sekarang dan
     * meneruskan baris ini ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
     * (mekanisme jejak audit bersama legacy AIS) setiap kali baris diperbarui.
     */
    @PreUpdate protected void onUpdate() {
        updatedAt = ais.ui.util.WaktuUtil.getDate();
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }
    /** Memangkas spasi awal/akhir {@code value}, atau mengembalikan {@code null} apa adanya bila {@code value} {@code null}. Dipakai seluruh setter String turunan {@link SocialRecord} untuk menormalkan input. */
    protected static String trim(String value) { return value == null ? null : value.trim(); }
}
