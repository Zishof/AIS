package ais.database.model.jurnal;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Superclass Hibernate/JPA ({@code @MappedSuperclass}, bukan entity mandiri — tidak punya tabel
 * sendiri) yang menyediakan kolom teknis bersama untuk seluruh 12 entity di paket
 * {@code ais.database.model.jurnal} (modul integrasi jurnal ilmiah bergaya OJS/Open Journal
 * Systems di domain {@code penelitiandanpengabdian}): {@link LanggananJurnal},
 * {@link RentangIpLanggananJurnal}, {@link UndanganPeranJurnal}, {@link PenugasanReviewerJurnal},
 * {@link PenugasanTahapJurnal}, {@link PesertaDiskusiJurnal}, dan entity sejenis lain di paket ini
 * (importer OJS, template email, agregat penggunaan).
 * <p>
 * Setiap subclass mewarisi:
 * <ul>
 *   <li>{@link #getId()} — primary key auto-increment ({@code IDENTITY}).</li>
 *   <li>{@link #getTenantKey()} — kunci penyekat tenant/organisasi (wajib diisi, mirip pola
 *       {@code SocialRecord} pada paket {@code sosial}). <b>PENTING:</b> kolom ini hanya
 *       menyediakan TEMPAT untuk nilai tenant; kelas ini sendiri TIDAK menambahkan filter
 *       tenant otomatis pada query manapun (bukan Hibernate filter/interceptor). Setiap query
 *       HQL yang dibuat oleh service pemanggil (mis. {@code JurnalAccessService},
 *       {@code JurnalWorkflowService}) bertanggung jawab menyertakan klausa pembatas sendiri
 *       (biasanya lewat {@code jurnalPenelitianId} atau relasi koleksi/journal, bukan lewat
 *       {@code tenantKey} secara langsung) — kegagalan melakukannya adalah kerentanan
 *       kebocoran lintas tenant pada level pemanggil, bukan sesuatu yang dicegah di sini.</li>
 *   <li>{@link #getJurnalPenelitianId()} — id jurnal ilmiah pemilik baris (FK longgar ke
 *       {@code JurnalPenelitian} pada paket {@code penelitiandanpengabdian}; tidak dipetakan
 *       sebagai relasi {@code @ManyToOne} eksplisit, kolom opsional pada beberapa subclass).</li>
 *   <li>{@link #getLockVersion()} — versi optimistic-locking Hibernate ({@code @Version}); getter
 *       memberi nilai default {@code 0} bila belum diisi agar entity baru (belum di-flush) tetap
 *       memberikan angka non-null ke pemanggil.</li>
 *   <li>{@link #getCreatedBy()}, {@link #getCreatedAt()}, {@link #getUpdatedAt()} — jejak audit
 *       baris: siapa yang membuat dan kapan dibuat/diperbarui terakhir. Diisi otomatis lewat
 *       {@link #create()}/{@link #update()}, bukan oleh kode pemanggil — pola ini KEHARUSAN
 *       TEKNIS (memastikan kolom {@code NOT NULL} selalu terisi meski pemanggil lupa mengisi),
 *       bukan bug tersembunyi.</li>
 *   <li>{@link #getAktif()} — flag aktif/nonaktif satu-arah (soft state, bukan soft-delete
 *       dua-arah dengan kolom terpisah); getter memberi default {@code TRUE} bila belum diisi.
 *       Subclass dan service pemanggil harus konsisten menyaring {@code aktif=true} pada setiap
 *       query pembacaan (lihat contoh di {@code JurnalAccessService}, {@code JurnalWorkflowService})
 *       — kelas ini sendiri tidak memaksakan penyaringan tersebut secara global.</li>
 * </ul>
 * <p>
 * Kolom {@code id} dideklarasikan {@code insertable=false} karena strategi {@code IDENTITY}
 * mengandalkan auto-increment database murni; Hibernate tidak boleh menyertakannya pada
 * statement INSERT.
 */
@MappedSuperclass
public abstract class JurnalEntityBase implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, jurnalPenelitianId, lockVersion;
    private String tenantKey, createdBy;
    private Date createdAt, updatedAt;
    private Boolean aktif;

    /** Primary key auto-increment (strategi {@code IDENTITY}) baris ini. */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false)
    public Long getId(){return id;} public void setId(Long v){id=v;}
    /**
     * Kunci penyekat tenant/organisasi pemilik baris. Wajib diisi ({@code nullable=false}) tapi
     * TIDAK ditegakkan otomatis oleh Hibernate pada query manapun — lihat catatan tenant-scoping
     * pada Javadoc kelas.
     */
    @Column(name="tenant_key",nullable=false,length=120)
    public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    /** Id jurnal ilmiah ({@code JurnalPenelitian}) pemilik baris ini; kolom FK longgar, opsional pada beberapa subclass. */
    @Column(name="jurnal_penelitian_id")
    public Long getJurnalPenelitianId(){return jurnalPenelitianId;} public void setJurnalPenelitianId(Long v){jurnalPenelitianId=v;}
    /**
     * Versi optimistic-locking Hibernate. Getter mengembalikan {@code 0} (bukan {@code null})
     * bila belum diisi, agar entity yang baru dibuat tetap memberi nilai numerik yang aman
     * dipakai pemanggil (mis. dibandingkan dengan versi yang dikirim klien) sebelum baris
     * di-flush ke database.
     */
    @Version @Column(name="lock_version",nullable=false)
    public Long getLockVersion(){return lockVersion==null?Long.valueOf(0):lockVersion;} public void setLockVersion(Long v){lockVersion=v;}
    /** Identitas pengguna (user id) yang membuat baris ini; diisi sekali saat create, tidak berubah setelahnya. */
    @Column(name="created_by",length=255)
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    /** Stempel waktu pembuatan baris; diisi otomatis oleh {@link #create()} bila belum diisi manual. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false)
    public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    /** Stempel waktu perubahan terakhir; diperbarui otomatis setiap update lewat {@link #update()}. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false)
    public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
    /**
     * Flag aktif/nonaktif baris (soft state satu arah). Getter mengembalikan {@code TRUE} bila
     * belum diisi, sehingga baris baru dianggap aktif secara default sebelum {@link #create()}
     * sempat menormalkan nilainya.
     */
    @Column(name="aktif",nullable=false)
    public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
    /**
     * Callback JPA {@code @PrePersist}: menormalkan kolom teknis sebelum baris pertama kali
     * disimpan — mengisi {@link #createdAt}/{@link #updatedAt} dengan waktu kini bila kosong,
     * {@link #aktif} dengan {@code TRUE} bila kosong, dan {@link #lockVersion} dengan {@code 0}
     * bila kosong. Dipanggil otomatis oleh provider JPA/Hibernate, tidak dipanggil manual.
     */
    @PrePersist protected void create(){Date now=new Date();if(createdAt==null)createdAt=now;if(updatedAt==null)updatedAt=now;if(aktif==null)aktif=Boolean.TRUE;if(lockVersion==null)lockVersion=0L;}
    /**
     * Callback JPA {@code @PreUpdate}: memperbarui {@link #updatedAt} ke waktu kini setiap kali
     * baris diubah. Dipanggil otomatis oleh provider JPA/Hibernate, tidak dipanggil manual.
     */
    @PreUpdate protected void update(){updatedAt=new Date();}
}
