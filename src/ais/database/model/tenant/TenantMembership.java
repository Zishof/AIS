package ais.database.model.tenant;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Pendaftar;
import ais.database.model.Tbmuser;

/**
 * <h3>Keanggotaan satu {@link Pendaftar} pada satu tenant (owner/anggota).</h3>
 *
 * <p>Memisahkan registration account (Pendaftar), tenant namespace ({@link TenantRegistry}),
 * dan tenant membership (tabel ini) -- satu Pendaftar bisa punya beberapa tenant, tenant
 * switcher membaca daftar membership AKTIF (invariant #14 ERD). {@code tbmuser} nullable:
 * diisi HANYA bila owner diberi akun aplikasi ZK penuh lewat adapter yang aman (bukan
 * menyalin hash PBKDF2 ke jalur DES).</p>
 *
 * <h3>Peran sebagai gerbang akses lintas tenant</h3>
 *
 * <p>Baris di tabel inilah yang menjawab pertanyaan "boleh atau tidak aktor ini masuk ke
 * tenant tersebut", sehingga ia adalah titik penegakan isolasi antar-tenant pada arsitektur
 * pendaftaran mandiri. Penegakannya dilakukan {@code TenantMembershipResolver} dan bersifat
 * gagal-tertutup: aktor yang tidak dinyatakan langsung ditolak, baris yang statusnya bukan
 * {@link #STATUS_ACTIVE} dilewati, dan baris yang aktif pun masih harus lolos pemeriksaan
 * rentang berlaku {@link #getValidFrom()}..{@link #getValidUntil()} sebelum akses diberikan.
 * Resolver juga sengaja membedakan "bukan anggota" dari "keanggotaan sudah tidak berlaku"
 * agar pesan yang diterima pengguna dapat menuntun, tanpa membocorkan keberadaan tenant
 * kepada pihak yang memang bukan anggota.</p>
 *
 * <p>Dua kontrol integritas melekat pada definisi tabel. Pertama, unique constraint atas
 * pasangan ({@code tenant_id}, {@code pendaftar_id}) menjamin seorang pendaftar tidak dapat
 * memiliki dua baris keanggotaan pada tenant yang sama — yang mencegah munculnya keanggotaan
 * bayangan dengan peran berbeda yang dapat dipakai memutar pencabutan akses. Penegakan di
 * sisi database ini penting karena kode pembuat keanggotaan pada mesin provisioning memang
 * memeriksa lebih dulu apakah baris sudah ada (demi sifat idempoten saat langkah diulang),
 * tetapi pemeriksaan-lalu-menulis semacam itu tidak aman terhadap eksekusi konkuren; unique
 * constraint-lah yang menutup celahnya. Kedua, kolom {@link Version} mengaktifkan optimistic
 * locking sehingga dua perubahan bersamaan atas satu keanggotaan — misalnya penangguhan oleh
 * administrator yang beririsan dengan perubahan peran — tidak dapat saling menimpa secara
 * diam-diam. Ditambah anotasi {@link Audited}, setiap perubahan status dan peran terekam
 * dalam riwayat Envers dan tidak dapat dihapus tanpa jejak.</p>
 *
 * <p>Keputusan desain yang paling perlu dipahami adalah dibiarkannya {@link #getTbmuser()}
 * bernilai {@code null}. Alur provisioning yang berjalan saat ini membuat keanggotaan pemilik
 * TANPA mengisi kolom tersebut: identitas dan kredensial pemilik tetap berada pada
 * {@link Pendaftar}, yang menyimpan password sebagai hash PBKDF2-HMAC-SHA256 dengan salt
 * per-pengguna. Menautkannya ke akun aplikasi ZK lama berarti bersinggungan dengan jalur
 * kredensial warisan yang jauh lebih lemah, dan menyalin hash modern ke jalur itu jelas tidak
 * mungkin dilakukan dengan benar. Karena itu penautan hanya boleh dilakukan lewat adapter
 * yang membuat kredensial pada jalur tujuan secara semestinya — bukan dengan memindahkan
 * bahan rahasia antar skema penyimpanan. Sikap menahan diri ini adalah sisi positif yang
 * layak dicatat, mengingat pola sebaliknya (menyalin atau menurunkan kekuatan kredensial demi
 * kompatibilitas) merupakan sumber masalah yang berulang di modul-modul lama.</p>
 *
 * @see TenantRegistry
 * @see PendaftaranTenant
 * @see PendaftaranAuditEvent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_membership",
		uniqueConstraints = @UniqueConstraint(columnNames = { "tenant_id", "pendaftar_id" }))
public class TenantMembership extends GeneralValueObject {

	/** Versi serialisasi Java standar untuk seluruh entity model AIS. */
	private static final long serialVersionUID = 1L;

	/** Keanggotaan berlaku; satu-satunya status yang meloloskan pemeriksaan akses. */
	public static final String STATUS_ACTIVE = "ACTIVE";
	/** Keanggotaan ditangguhkan sementara; akses ditolak tanpa menghapus barisnya. */
	public static final String STATUS_SUSPENDED = "SUSPENDED";
	/** Keanggotaan dicabut permanen; akses ditolak, riwayatnya tetap tersimpan. */
	public static final String STATUS_REVOKED = "REVOKED";

	/** Peran pemilik tenant; sekaligus nilai default {@link #getRoleCode()}. */
	public static final String ROLE_OWNER = "OWNER";

	/** Primary key surrogate, IDENTITY dari sequence PostgreSQL. */
	private Long id;
	/** Tenant yang diikuti (wajib). */
	private TenantRegistry tenant;
	/** Pendaftar yang menjadi anggota (wajib). */
	private Pendaftar pendaftar;
	/** Akun aplikasi ZK bila ditautkan; sengaja dibiarkan {@code null} pada alur saat ini. */
	private Tbmuser tbmuser;
	/** Status keanggotaan (ACTIVE/SUSPENDED/REVOKED). */
	private String status;
	/** Penanda pemilik tenant. */
	private Boolean isOwner;
	/** Kode peran pada tenant. */
	private String roleCode;
	/** Awal masa berlaku keanggotaan; {@code null} berarti berlaku sejak dulu. */
	private Date validFrom;
	/** Akhir masa berlaku keanggotaan; {@code null} berarti tidak kedaluwarsa. */
	private Date validUntil;
	/** Waktu baris keanggotaan dibuat. */
	private Date createdAt;
	/** Penghitung optimistic locking Hibernate. */
	private Integer version;

	/** Nama pengguna pembuat/pengubah baris — field audit shadow wajib pola AIS. */
	private String oleh;
	/** Id pengguna pembuat/pengubah baris — field audit shadow wajib pola AIS. */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir. Deklarasi satu baris bersama {@code @PreUpdate}
	 * di bawah ini adalah KEHARUSAN TEKNIS pola AIS (bukan gaya penulisan yang keliru):
	 * interceptor {@code AuditTimestampInterceptor.ubah} dipanggil Hibernate sebelum setiap
	 * update sehingga stempel waktu terisi tanpa campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi reflektif. */
	public TenantMembership() {
	}

	/**
	 * Primary key keanggotaan. Dibangkitkan database (IDENTITY) saat insert, sehingga
	 * bernilai {@code null} selama objek masih transient.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Dipakai Hibernate; kode aplikasi normal tidak perlu memanggilnya.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Tenant yang diikuti oleh keanggotaan ini (kolom {@code tenant_id}, {@code NOT NULL},
	 * bagian dari unique constraint bersama {@code pendaftar_id}). Relasi {@code LAZY}; getter
	 * melewatkan nilainya ke {@code check(...)} milik {@code GeneralValueObject} yang
	 * meng-unwrap proxy Hibernate dan mengembalikan {@code null} secara aman bila proxy sudah
	 * tidak dapat diinisialisasi. Perhatikan pola "getter destruktif" khas model AIS: hasil
	 * {@code check(...)} ditulis balik ke field, sehingga getter ini tidak bebas efek samping.
	 *
	 * @return tenant terkait, atau {@code null} bila proxy tak dapat diinisialisasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = false)
	public TenantRegistry getTenant() {
		tenant = check(tenant);
		return tenant;
	}

	/**
	 * Menetapkan tenant yang diikuti. Wajib diisi sebelum {@code save} karena kolom FK
	 * bersifat {@code NOT NULL}.
	 *
	 * @param tenant tenant terkait
	 */
	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	/**
	 * Pendaftar yang menjadi anggota tenant (kolom {@code pendaftar_id}, {@code NOT NULL},
	 * bagian dari unique constraint bersama {@code tenant_id}). Inilah identitas utama pada
	 * arsitektur pendaftaran mandiri: kredensial login pemilik tenant berada di entity
	 * {@link Pendaftar}, bukan pada {@link #getTbmuser()}. Relasi {@code LAZY} dengan pola
	 * getter destruktif yang sama seperti {@link #getTenant()}.
	 *
	 * @return pendaftar anggota, atau {@code null} bila proxy tak dapat diinisialisasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar_id", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Menetapkan pendaftar anggota. Wajib diisi sebelum {@code save} karena kolom FK bersifat
	 * {@code NOT NULL}.
	 *
	 * @param pendaftar pendaftar anggota
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * Akun aplikasi ZK ({@link Tbmuser}) yang ditautkan pada keanggotaan ini, kolom
	 * {@code tbmuser_id} yang secara eksplisit {@code nullable}.
	 *
	 * <p>Pada alur provisioning yang berjalan saat ini kolom ini TIDAK PERNAH diisi: langkah
	 * pembuatan keanggotaan hanya menetapkan tenant, pendaftar, status, penanda pemilik,
	 * peran, dan waktu. Keadaan {@code null} karena itu adalah kondisi normal, bukan data
	 * yang belum lengkap, dan kode pembaca wajib menanganinya tanpa mengasumsikan adanya akun
	 * ZK di balik setiap keanggotaan.</p>
	 *
	 * <p>Alasan di balik pilihan itu bersifat keamanan dan layak dipertahankan. Kredensial
	 * pemilik tenant hidup di {@link Pendaftar} dalam bentuk hash PBKDF2-HMAC-SHA256 dengan
	 * salt per-pengguna dan jumlah iterasi yang tercatat, sedangkan akun aplikasi lama berada
	 * pada jalur kredensial warisan yang jauh lebih lemah. Menautkan keduanya dengan cara
	 * memindahkan bahan rahasia — menyalin hash, atau lebih buruk lagi menurunkan
	 * kekuatannya agar cocok dengan jalur lama — akan meniadakan seluruh manfaat skema
	 * modern tersebut sekaligus menghasilkan dua salinan kredensial yang harus dijaga
	 * konsisten. Karena itu penautan hanya sah dilakukan lewat adapter yang MEMBUAT kredensial
	 * baru pada jalur tujuan melalui mekanismenya sendiri, bukan memindahkannya. Selama
	 * adapter tersebut belum ada, membiarkan kolom ini kosong adalah pilihan yang benar.</p>
	 *
	 * <p>Relasi {@code LAZY} dengan pola getter destruktif yang sama seperti
	 * {@link #getTenant()}; perhatikan bahwa {@code null} yang dikembalikan di sini bermakna
	 * ganda — bisa berarti memang tidak ditautkan, bisa pula berarti proxy tidak dapat
	 * diinisialisasi.</p>
	 *
	 * @return akun aplikasi tertaut, atau {@code null} bila tidak ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser_id", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menetapkan akun aplikasi ZK tertaut. Lihat peringatan pada {@link #getTbmuser()}
	 * sebelum mengisinya: penautan hanya boleh lewat adapter yang membuat kredensial pada
	 * jalur tujuan secara semestinya, tidak dengan memindahkan bahan rahasia.
	 *
	 * @param tbmuser akun aplikasi tertaut, boleh {@code null}
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Status keanggotaan: {@link #STATUS_ACTIVE}, {@link #STATUS_SUSPENDED}, atau
	 * {@link #STATUS_REVOKED}. Kolom {@code NOT NULL}; getter mem-default ke
	 * {@link #STATUS_ACTIVE} bila field {@code null}/kosong.
	 *
	 * <p>Status inilah saringan pertama pemeriksaan akses: {@code TenantMembershipResolver}
	 * melewati setiap baris yang statusnya bukan {@link #STATUS_ACTIVE}, sehingga penangguhan
	 * maupun pencabutan berlaku seketika tanpa perlu menghapus barisnya — riwayat
	 * keanggotaan tetap utuh dan terekam Envers. Perlu dicatat bahwa arah default getter ini
	 * bersifat "terbuka": baris yang kolom statusnya kosong akan terbaca sebagai aktif.
	 * Pada praktiknya hal itu tidak menjadi celah karena kolomnya {@code NOT NULL} di
	 * database dan seluruh penulis mengisinya secara eksplisit dengan {@link #STATUS_ACTIVE},
	 * dan karena baris yang lolos saringan status masih harus melewati pemeriksaan rentang
	 * berlaku. Meski demikian, arah default ini patut diingat bila logika status kelak
	 * diperluas: untuk sebuah gerbang akses, default yang lebih aman adalah menolak.</p>
	 *
	 * @return kode status, tidak pernah {@code null}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_ACTIVE : status;
	}

	/**
	 * Menetapkan status keanggotaan. Gunakan konstanta {@code STATUS_*} kelas ini.
	 *
	 * @param status kode status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Penanda bahwa anggota ini adalah pemilik tenant. Getter mem-default ke
	 * {@link Boolean#FALSE} bila field {@code null} — arah default yang aman untuk sebuah
	 * penanda hak istimewa, karena data yang tidak lengkap dibaca sebagai bukan pemilik.
	 * Diisi {@code true} oleh langkah provisioning yang membuat keanggotaan pemilik tenant
	 * baru, berbarengan dengan {@link #getRoleCode()} bernilai {@link #ROLE_OWNER}.
	 *
	 * @return {@code true} bila pemilik tenant
	 */
	@Column(name = "is_owner")
	public Boolean getIsOwner() {
		return isOwner == null ? Boolean.FALSE : isOwner;
	}

	/**
	 * Menetapkan penanda pemilik tenant.
	 *
	 * @param isOwner {@code true} bila pemilik tenant
	 */
	public void setIsOwner(Boolean isOwner) {
		this.isOwner = isOwner;
	}

	/**
	 * Kode peran anggota pada tenant. Getter mem-default ke {@link #ROLE_OWNER} bila field
	 * {@code null}/kosong. Berbeda dari {@link #getIsOwner()} yang mem-default ke nilai paling
	 * tidak berhak, default di sini justru mengarah ke peran paling berhak — konsekuensi dari
	 * kenyataan bahwa satu-satunya penulis saat ini memang hanya membuat keanggotaan pemilik.
	 * Bila kelak peran non-pemilik (undangan anggota biasa) ditambahkan, default ini perlu
	 * ditinjau ulang agar baris berperan kosong tidak terbaca sebagai pemilik; resolver
	 * sendiri menentukan peran efektif dengan mempertimbangkan {@link #getIsOwner()}, sehingga
	 * kedua field itu sebaiknya selalu diisi bersamaan dan konsisten.
	 *
	 * @return kode peran, tidak pernah {@code null}
	 */
	@Column(name = "role_code", length = 64)
	public String getRoleCode() {
		return roleCode == null || roleCode.trim().isEmpty() ? ROLE_OWNER : roleCode;
	}

	/**
	 * Menetapkan kode peran anggota.
	 *
	 * @param roleCode kode peran, mis. {@link #ROLE_OWNER}
	 */
	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	/**
	 * Awal masa berlaku keanggotaan. Nilai {@code null} diperlakukan sebagai "sudah berlaku
	 * sejak dulu" oleh pemeriksaan akses. Diisi dengan waktu pembuatan oleh langkah
	 * provisioning keanggotaan pemilik.
	 *
	 * @return awal masa berlaku, atau {@code null} bila tanpa batas awal
	 */
	@Column(name = "valid_from")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getValidFrom() {
		return validFrom;
	}

	/**
	 * Menetapkan awal masa berlaku keanggotaan.
	 *
	 * @param validFrom awal masa berlaku
	 */
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	/**
	 * Akhir masa berlaku keanggotaan. Nilai {@code null} berarti keanggotaan tidak pernah
	 * kedaluwarsa, dan itulah kondisi keanggotaan pemilik yang dibuat alur provisioning.
	 * Bersama {@link #getValidFrom()} membentuk saringan kedua pemeriksaan akses setelah
	 * status: keanggotaan yang aktif tetapi berada di luar rentang berlaku tetap ditolak,
	 * dengan pesan yang membedakannya dari kasus bukan-anggota.
	 *
	 * @return akhir masa berlaku, atau {@code null} bila tidak kedaluwarsa
	 */
	@Column(name = "valid_until")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getValidUntil() {
		return validUntil;
	}

	/**
	 * Menetapkan akhir masa berlaku keanggotaan.
	 *
	 * @param validUntil akhir masa berlaku, {@code null} bila tidak kedaluwarsa
	 */
	public void setValidUntil(Date validUntil) {
		this.validUntil = validUntil;
	}

	/**
	 * Waktu baris keanggotaan dibuat. Untuk keanggotaan pemilik, nilainya sama dengan
	 * {@link #getValidFrom()} karena keduanya diisi pada langkah provisioning yang sama.
	 *
	 * @return waktu pembuatan baris, atau {@code null} bila belum diisi
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan waktu pembuatan baris keanggotaan.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Penghitung optimistic locking Hibernate ({@link Version}). Dinaikkan otomatis pada
	 * setiap update, dan update yang membawa nilai versi usang akan gagal dengan
	 * {@code StaleObjectStateException} alih-alih menimpa perubahan pihak lain.
	 *
	 * <p>Keberadaannya penting justru karena entity ini adalah gerbang akses: tanpa
	 * optimistic locking, penangguhan keanggotaan oleh administrator yang berjalan bersamaan
	 * dengan perubahan lain atas baris yang sama berisiko hilang karena tertimpa, sehingga
	 * akses yang seharusnya sudah dicabut tetap berlaku. Perhatikan bahwa kolom ini
	 * satu-satunya di antara lima entity klaster verifikasi/audit/consent/kredensial yang
	 * memakai {@code @Version} — pilihan yang wajar mengingat hanya baris keanggotaan yang
	 * memang dimaksudkan untuk diubah berulang kali sepanjang hidupnya, sementara baris audit
	 * dan bukti persetujuan hanya disisipkan.</p>
	 *
	 * <p>Kode aplikasi tidak boleh menetapkan nilai ini secara manual; biarkan Hibernate yang
	 * mengelolanya.</p>
	 *
	 * @return nomor versi baris, atau {@code null} bila belum tersimpan
	 */
	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	/**
	 * Menetapkan nomor versi optimistic locking. Dipakai Hibernate; jangan dipanggil kode
	 * aplikasi karena dapat merusak deteksi perubahan bersamaan.
	 *
	 * @param version nomor versi baris
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Nama pengguna yang membuat/mengubah baris (field audit shadow standar AIS). Untuk
	 * keanggotaan yang dibuat mesin provisioning, nilainya adalah penanda sistem
	 * {@code "provisioning"}.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna pembuat/pengubah. Setter sengaja MENGABAIKAN nilai
	 * {@code null} maupun string kosong/spasi — pola baku audit shadow AIS yang mencegah
	 * jejak pelaku yang sudah terisi tertimpa nilai kosong. Untuk entity gerbang akses
	 * seperti ini, mempertahankan jejak siapa yang terakhir mengubah status keanggotaan
	 * bernilai khusus saat menelusuri pemberian atau pencabutan akses.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna yang membuat/mengubah baris (field audit shadow standar AIS).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pembuat/pengubah. Sama seperti {@link #setOleh(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak pelaku tidak terhapus.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu perubahan terakhir, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor.ubah} lewat callback {@code @PreUpdate}. Berbeda dari
	 * entity audit dan consent yang barisnya hanya disisipkan, nilai ini pada keanggotaan
	 * memang berubah secara sah setiap kali status atau peran diperbarui.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi
	 * karena sudah ditangani interceptor.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
