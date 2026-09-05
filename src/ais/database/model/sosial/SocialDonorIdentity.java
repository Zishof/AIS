package ais.database.model.sosial;

import java.util.Date;
import javax.persistence.*;
import org.hibernate.envers.Audited;
import ais.database.model.Tbmuser;

/**
 * Entitas Hibernate: identitas donor pada portal modul sosial/donasi AIS — dipetakan ke tabel
 * {@code public.social_donor_identity}. Menjembatani dua kemungkinan identitas donor per penyewa
 * (tenant): donor internal AIS yang sudah terdaftar sebagai {@link Donatur} dan/atau donor yang
 * login lewat akun {@link Tbmuser} (mis. donor eksternal/portal publik) — kedua relasi opsional
 * dan independen, dijaga UNIK per {@code (tenant_key, donatur_id)} dan {@code (tenant_key,
 * tbmuser_id)} secara terpisah agar satu donor Donatur atau satu akun Tbmuser tidak terdaftar
 * ganda dalam tenant yang sama. Menyimpan data tampilan (nama, kontak) dan preferensi privasi
 * (anonim default, persetujuan komunikasi, versi consent) independen dari data induk
 * {@link Donatur}/{@link Tbmuser}, sehingga portal publik bisa punya profil donor sendiri tanpa
 * mengubah data induk.
 *
 * <p>
 * <b>Verifikasi data sensitif (KYC/NIK) diminta secara eksplisit:</b> meskipun bernama
 * "identity", kelas ini TIDAK menyimpan nomor identitas resmi (NIK/KTP/paspor) atau dokumen
 * verifikasi apa pun — kolomnya terbatas pada {@link #getDisplayName()}, {@link #getEmail()},
 * {@link #getPhone()}, {@link #getDonorType()}, dan penanda consent. "Identity" di sini berarti
 * identitas TAMPILAN/kontak pada portal publik (analog identitas akun), BUKAN identitas
 * terverifikasi (KYC) seperti dugaan awal — sehingga tidak ditemukan kebocoran/penyimpanan
 * data sensitif jenis NIK/KTP pada entitas ini.
 * </p>
 *
 * <p>
 * <b>Investigasi pemakaian:</b> AKTIF, bukan dorman — {@code
 * ais.action.master.sosial.helper.SocialIdentityService#resolveOrCreate} melakukan
 * auto-provisioning: setiap kali user AIS ({@link Tbmuser}) terautentikasi pertama kali
 * mengakses portal sosial untuk suatu tenant, baris {@code SocialDonorIdentity} baru dibuat
 * otomatis (nama/email/telepon disalin dari {@link Tbmuser}, jenis donatur diresolusi dari
 * peran mahasiswa/siswa/dosen/guru/pegawai) dan {@link #getLastLogin()} diperbarui setiap
 * pemanggilan berikutnya — dipakai al. oleh {@code ZakatCalculatorService} untuk mengaitkan
 * hasil perhitungan zakat ke identitas donatur terautentikasi.
 * </p>
 */
@Entity @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Audited
@Table(schema="public",name="social_donor_identity",uniqueConstraints={@UniqueConstraint(columnNames={"tenant_key","tbmuser_id"}),@UniqueConstraint(columnNames={"tenant_key","donatur_id"})})
public class SocialDonorIdentity extends SocialRecord {
    private static final long serialVersionUID=1L;
    /** Donor internal AIS terkait (opsional); Tbmuser terkait bila donor login lewat akun pengguna (opsional, independen dari {@link #donatur}). */
    private Donatur donatur; private Tbmuser tbmuser;
    /** Nama tampilan publik, email, telepon, jenis donor (mis. individu/lembaga), dan versi dokumen persetujuan privasi yang disetujui donor ini. */
    private String displayName,email,phone,donorType,privacyConsentVersion;
    /** Apakah donor ini anggota eksternal (bukan warga internal institusi), apakah donasi ditampilkan anonim secara default, dan apakah donor menyetujui menerima komunikasi — masing-masing dianggap {@code false} bila {@code null}. */
    private Boolean externalMember,anonymousDefault,communicationConsent;
    /** Waktu login terakhir identitas donor ini pada portal. */
    private Date lastLogin;
    /** Donatur internal AIS ({@link Donatur} legacy) yang berelasi dengan identitas donor ini, bila ada (opsional, unik per tenant). */
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="donatur_id") public Donatur getDonatur(){return donatur;} public void setDonatur(Donatur v){donatur=v;}
    /** Akun {@link Tbmuser} AIS yang login sebagai identitas donor ini, bila ada (opsional, unik per tenant, independen dari {@link #getDonatur()}). */
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="tbmuser_id") public Tbmuser getTbmuser(){return tbmuser;} public void setTbmuser(Tbmuser v){tbmuser=v;}
    /** Nama tampilan publik donor ini pada portal (wajib). */
    @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=trim(v);}
    /** Alamat email kontak donor ini. */
    @Column(name="email",length=320) public String getEmail(){return email;} public void setEmail(String v){email=trim(v);}
    /** Nomor telepon kontak donor ini. */
    @Column(name="phone",length=40) public String getPhone(){return phone;} public void setPhone(String v){phone=trim(v);}
    /** Jenis/kategori donor (mis. {@code "MAHASISWA"}, {@code "SISWA"}, {@code "DOSEN"}, {@code "GURU"}, {@code "PEGAWAI"}, {@code "AIS_USER"}, atau {@code "LEGACY"} bila hasil backfill migrasi). */
    @Column(name="donor_type",length=40) public String getDonorType(){return donorType;} public void setDonorType(String v){donorType=trim(v);}
    /** Versi dokumen persetujuan privasi (consent) yang telah disetujui donor ini. */
    @Column(name="privacy_consent_version",length=40) public String getPrivacyConsentVersion(){return privacyConsentVersion;} public void setPrivacyConsentVersion(String v){privacyConsentVersion=trim(v);}
    /** Menandai apakah donor ini anggota eksternal (bukan warga internal institusi); dianggap {@code false} bila {@code null}. */
    @Column(name="external_member") public Boolean getExternalMember(){return Boolean.TRUE.equals(externalMember);} public void setExternalMember(Boolean v){externalMember=v;}
    /** Menandai apakah donasi donor ini ditampilkan anonim secara default; dianggap {@code false} bila {@code null}. */
    @Column(name="anonymous_default") public Boolean getAnonymousDefault(){return Boolean.TRUE.equals(anonymousDefault);} public void setAnonymousDefault(Boolean v){anonymousDefault=v;}
    /** Menandai apakah donor menyetujui menerima komunikasi (mis. pemberitahuan/marketing); dianggap {@code false} bila {@code null}. */
    @Column(name="communication_consent") public Boolean getCommunicationConsent(){return Boolean.TRUE.equals(communicationConsent);} public void setCommunicationConsent(Boolean v){communicationConsent=v;}
    /** Waktu login terakhir identitas donor ini pada portal; diperbarui setiap kali {@code SocialIdentityService#resolveOrCreate} dipanggil. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_login") public Date getLastLogin(){return lastLogin;} public void setLastLogin(Date v){lastLogin=v;}
}
