package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_author_authority}
 * — rekaman "authority control" (konsep DSpace Author/Name Authority, mirip
 * peran ORCID) untuk menyatukan berbagai variasi penulisan nama penulis
 * ({@link #getNameVariants()}) menjadi satu identitas kanonik
 * ({@link #getCanonicalName()}), sekaligus menautkannya ke identifier
 * eksternal (ORCID, ROR institusi) dan identifier internal (NIDN/NIP/NIM,
 * akun pengguna, referensi data mahasiswa). Dipakai
 * {@code RepositoryAuthorityService} untuk disambiguasi penulis saat
 * menyimpan/menampilkan metadata item (mis. menyatukan "Budi S." dan
 * "Budi Santoso" sebagai penulis yang sama).
 *
 * <p>
 * Unik per {@code (tenant_key, normalized_name)} — satu nama penulis yang
 * sudah dinormalisasi hanya boleh punya satu rekaman authority per penyewa
 * (tenant), mencegah duplikasi identitas penulis yang sama dalam satu
 * institusi.
 * </p>
 *
 * <p>
 * Sebagian besar getter String di kelas ini mengembalikan string kosong
 * ({@code ""}) alih-alih {@code null} bila field belum diisi — pola
 * "null-safe getter" yang menyederhanakan pemakaian di sisi tampilan
 * (menghindari {@code NullPointerException}/cek null berulang), BUKAN
 * berarti field tersebut memang wajib diisi (lihat anotasi {@code @Column}
 * masing-masing untuk nullability sesungguhnya di database).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_author_authority",
        uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","normalized_name"}))
public class RepoAuthorAuthority implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,mahasiswaRefId; private String userRefId,tenantKey,canonicalName,normalizedName,nameVariants,orcid,nidn,nip,nim,
            affiliation,rorId,institutionalEmail,topics; private Boolean verified,aktif; private Date createdAt,updatedAt;
    /** Id baris authority ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    /** Kunci penyewa (tenant) pemilik rekaman authority ini — bersama {@link #getNormalizedName()} membentuk kunci unik antar-institusi. */
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    /** Id akun pengguna aplikasi yang tertaut ke identitas penulis ini, bila penulis adalah pengguna terdaftar (mis. dosen/staf dengan akun login). */
    @Column(name="user_ref_id",length=255) public String getUserRefId(){return userRefId;} public void setUserRefId(String v){userRefId=v;}
    /** Id referensi data mahasiswa yang tertaut ke identitas penulis ini, bila penulis adalah mahasiswa (skripsi/tesis) tanpa akun aplikasi terpisah. */
    @Column(name="mahasiswa_ref_id") public Long getMahasiswaRefId(){return mahasiswaRefId;} public void setMahasiswaRefId(Long v){mahasiswaRefId=v;}
    /** Nama kanonik/resmi penulis yang dipakai sebagai representasi tunggal dari seluruh {@link #getNameVariants()}. */
    @Column(name="canonical_name",nullable=false,length=255) public String getCanonicalName(){return canonicalName==null?"":canonicalName;} public void setCanonicalName(String v){canonicalName=v;}
    /** Bentuk nama yang sudah dinormalisasi (mis. huruf kecil, tanpa gelar/spasi ganda) — dasar pencocokan/dedup dan bagian dari kunci unik bersama {@link #getTenantKey()}. */
    @Column(name="normalized_name",nullable=false,length=255) public String getNormalizedName(){return normalizedName==null?"":normalizedName;} public void setNormalizedName(String v){normalizedName=v;}
    /** Daftar variasi penulisan nama penulis yang pernah ditemukan (mis. terpisah delimiter) yang seluruhnya diketahui merujuk ke identitas kanonik yang sama — inti dari fungsi "authority control": menyatukan variasi nama menjadi satu identitas. */
    @Column(name="name_variants",columnDefinition="TEXT") public String getNameVariants(){return nameVariants==null?"":nameVariants;} public void setNameVariants(String v){nameVariants=v;}
    /** Identifier ORCID penulis (padanan DSpace/OJS untuk identitas peneliti lintas-institusi), bila sudah ditautkan. */
    @Column(name="orcid",length=40) public String getOrcid(){return orcid==null?"":orcid;} public void setOrcid(String v){orcid=v;}
    /** Nomor Induk Dosen Nasional (NIDN) penulis, bila penulis adalah dosen. */
    @Column(name="nidn",length=80) public String getNidn(){return nidn==null?"":nidn;} public void setNidn(String v){nidn=v;}
    /** Nomor Induk Pegawai (NIP) penulis, bila penulis adalah pegawai/dosen ber-NIP. */
    @Column(name="nip",length=100) public String getNip(){return nip==null?"":nip;} public void setNip(String v){nip=v;}
    /** Nomor Induk Mahasiswa (NIM) penulis, bila penulis adalah mahasiswa. */
    @Column(name="nim",length=100) public String getNim(){return nim==null?"":nim;} public void setNim(String v){nim=v;}
    /** Afiliasi institusi penulis pada saat rekaman ini dibuat/diperbarui (field teks bebas, mis. nama fakultas/program studi). */
    @Column(name="affiliation",length=500) public String getAffiliation(){return affiliation==null?"":affiliation;} public void setAffiliation(String v){affiliation=v;}
    /** Identifier ROR (Research Organization Registry) institusi afiliasi penulis, bila sudah ditautkan ke registry organisasi riset standar. */
    @Column(name="ror_id",length=100) public String getRorId(){return rorId==null?"":rorId;} public void setRorId(String v){rorId=v;}
    /** Alamat surel institusional penulis (bukan surel pribadi), dipakai antara lain untuk verifikasi identitas. */
    @Column(name="institutional_email",length=255) public String getInstitutionalEmail(){return institutionalEmail==null?"":institutionalEmail;} public void setInstitutionalEmail(String v){institutionalEmail=v;}
    /** Daftar topik/bidang minat riset penulis (teks bebas/terpisah delimiter), dipakai untuk pencarian dan rekomendasi terkait penulis. */
    @Column(name="topics",columnDefinition="TEXT") public String getTopics(){return topics==null?"":topics;} public void setTopics(String v){topics=v;}
    /** Menandakan apakah identitas penulis ini sudah diverifikasi (mis. oleh admin/petugas repositori) sebagai identitas yang benar dan bukan hasil disambiguasi otomatis semata; {@code null} ditafsirkan {@code false} (belum terverifikasi, fail-closed). */
    @Column(name="verified") public Boolean getVerified(){return verified==null?Boolean.FALSE:verified;} public void setVerified(Boolean v){verified=v;}
    /** Status aktif rekaman authority ini; {@code null} ditafsirkan {@code true} (default aktif) — rekaman tidak aktif tidak lagi dipakai untuk pencocokan penulis baru. */
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
    /** Waktu rekaman authority ini pertama kali dibuat. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    /** Waktu rekaman authority ini terakhir diperbarui (mis. penambahan varian nama baru atau penautan identifier eksternal). */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false) public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
}
