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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Pilihan jenis usaha satu permohonan tenant (join table = SUMBER KEBENARAN multi-select).</h3>
 *
 * <p>{@code Pendaftar.jenisBisnis} existing HANYA diisi sebagai compatibility snapshot (kode
 * primary, atau daftar kode dipisah koma) -- SEMUA logika membaca pilihan jenis usaha WAJIB
 * dari tabel ini, bukan dari kolom snapshot itu (invariant #5 ERD). Minimal satu baris per
 * permohonan; unique (permohonan, jenis usaha) menolak duplikat.</p>
 *
 * <h4>Kenapa tabel ini penting secara keamanan</h4>
 *
 * <p>Baris-baris di sini adalah SATU-SATUNYA masukan pendaftar yang benar-benar memengaruhi
 * bentuk tenant yang akan dibangun, lewat dua pembaca:</p>
 * <ol>
 * <li>{@code PendaftaranTenantService.perluManualReview(...)} -- bila salah satu jenis usaha
 * terpilih ber-{@link JenisUsahaTenant#getRequiresManualReview()} {@code true}, permohonan
 * dibelokkan ke {@link PendaftaranTenant#STATUS_REVIEW_PENDING} alih-alih langsung masuk antrean
 * provisioning.</li>
 * <li>{@code ais.service.tenant.TenantEntitlementService.terapkanDariJenisUsaha(...)} -- daftar
 * modul tenant dibentuk sebagai union {@link JenisUsahaTenantModule} dari seluruh jenis usaha
 * yang tercatat di sini.</li>
 * </ol>
 *
 * <p>Justru karena itu, yang boleh dikirim pendaftar dibatasi seketat mungkin: formulir hanya
 * mengirim {@code jenisUsahaIds}, dan setiap id dimuat ulang dari database lalu WAJIB ada dan
 * ber-{@code aktif=true} (jika tidak: {@code BUSINESS_TYPE_INVALID}, transaksi di-rollback).
 * Pendaftar tidak pernah mengirim nama modul, harga, maupun flag review -- semuanya diambil
 * server dari baris master. Dengan kata lain, memilih jenis usaha TIDAK bisa dipakai untuk
 * memilih modul secara langsung; yang dapat dipilih hanyalah "kartu" yang memang disediakan
 * Platform Admin di katalog.</p>
 *
 * <h4>Hubungan dengan snapshot legacy</h4>
 *
 * <p>Selain baris di tabel ini, service juga menuliskan CSV kode jenis usaha ke
 * {@code Pendaftar.jenisBisnis} dan menurunkan {@code Pendaftar.merupakanSekolah} dari pilihan
 * pertama. Keduanya snapshot kompatibilitas untuk layar lama; keduanya boleh basi bila pilihan
 * kelak diubah, sedangkan tabel inilah yang harus dibaca ulang.</p>
 *
 * @see PendaftaranTenant
 * @see JenisUsahaTenant
 * @see JenisUsahaTenantModule
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_tenant_jenis_usaha",
		uniqueConstraints = @UniqueConstraint(columnNames = { "pendaftaran_tenant_id", "jenis_usaha_tenant_id" }))
public class PendaftaranTenantJenisUsaha extends GeneralValueObject {

	/** Versi serialisasi Java standar entity AIS. */
	private static final long serialVersionUID = 1L;

	/** Primary key surrogate (identity, di-generate database). */
	private Long id;
	/** Permohonan pemilik pilihan ini. */
	private PendaftaranTenant pendaftaranTenant;
	/** Jenis usaha master yang dipilih. */
	private JenisUsahaTenant jenisUsahaTenant;
	/** Penanda pilihan utama di antara pilihan satu permohonan. */
	private Boolean primaryChoice;
	/** Keterangan bebas untuk pilihan LAINNYA. */
	private String otherDescription;
	/** Waktu baris dibuat. */
	private Date createdAt;

	/** Nama pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir + hook {@code @PreUpdate} lewat
	 * {@code AuditTimestampInterceptor}. Trio {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * adalah keharusan teknis pola entity AIS (dipakai layar audit generik), bukan duplikasi
	 * Envers yang bisa dihapus.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default wajib JavaBean/Hibernate; pengisian dilakukan service pendaftaran. */
	public PendaftaranTenantJenisUsaha() {
	}

	/**
	 * Primary key baris pilihan (identity database).
	 *
	 * @return id baris, {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key -- dipanggil Hibernate. Jangan diisi manual dari kode aplikasi.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Permohonan {@link PendaftaranTenant} pemilik pilihan ini (wajib). Baris dibuat di dalam
	 * transaksi submit yang sama dengan permohonannya, sehingga tidak pernah ada pilihan yatim:
	 * gagal di titik mana pun berarti seluruh submit di-rollback.
	 *
	 * <p>Getter memakai {@code check(...)} milik {@link GeneralValueObject} untuk menetralkan
	 * proxy Hibernate yang tak dapat diinisialisasi menjadi {@code null}, alih-alih melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return permohonan pemilik, atau {@code null} bila proxy tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = false)
	public PendaftaranTenant getPendaftaranTenant() {
		pendaftaranTenant = check(pendaftaranTenant);
		return pendaftaranTenant;
	}

	/**
	 * Tautkan pilihan ke permohonannya.
	 *
	 * @param pendaftaranTenant permohonan pemilik (wajib saat disimpan)
	 */
	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	/**
	 * Jenis usaha master yang dipilih (wajib). Baris master dimuat ulang dari database saat submit
	 * dan ditolak bila tidak ada atau tidak aktif -- jadi isi kolom ini selalu menunjuk kartu
	 * katalog yang sah pada saat pendaftaran, bukan teks bebas dari pendaftar.
	 *
	 * <p>Perhatikan: master boleh dinonaktifkan Platform Admin di kemudian hari, sehingga baris
	 * lama dapat menunjuk jenis usaha yang kini {@code aktif=false}. Itu wajar (jejak historis) dan
	 * tidak membatalkan tenant yang sudah terbentuk.</p>
	 *
	 * <p>Getter memakai {@code check(...)}; pembaca yang melakukan iterasi (mis. pembentukan
	 * entitlement) sudah melewati nilai {@code null} dengan aman.</p>
	 *
	 * @return jenis usaha terpilih, atau {@code null} bila proxy tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = false)
	public JenisUsahaTenant getJenisUsahaTenant() {
		jenisUsahaTenant = check(jenisUsahaTenant);
		return jenisUsahaTenant;
	}

	/**
	 * Tetapkan jenis usaha yang dipilih.
	 *
	 * @param jenisUsahaTenant baris master jenis usaha (wajib saat disimpan)
	 */
	public void setJenisUsahaTenant(JenisUsahaTenant jenisUsahaTenant) {
		this.jenisUsahaTenant = jenisUsahaTenant;
	}

	/** True pada tepat satu baris per permohonan -- jenis usaha utama (dipakai snapshot jenisBisnis).
	 *
	 *  <p>Getter mengembalikan {@code FALSE} bila kolom kosong (default dibaca-saja, tidak ditulis
	 *  balik). Service menandai pilihan PERTAMA pada urutan {@code jenisUsahaIds} yang dikirim
	 *  formulir sebagai primary, artinya urutan array dari klien yang menentukan -- bukan
	 *  peringkat apa pun di sisi server. Dampaknya terbatas dan tidak menyangkut hak akses:
	 *  primary hanya memengaruhi snapshot kompatibilitas {@code Pendaftar.jenisBisnis} dan
	 *  penurunan flag {@code Pendaftar.merupakanSekolah}. Pembentukan entitlement modul TIDAK
	 *  memandang primary sama sekali -- ia mengambil union modul dari SELURUH jenis usaha
	 *  terpilih.</p>
	 *
	 *  <p>Keunikan "tepat satu" adalah invariant yang dijaga service saat menulis, BUKAN oleh
	 *  constraint database (unique constraint tabel ini menjaga pasangan permohonan+jenis usaha,
	 *  bukan jumlah primary). Kode yang kelak mengubah pilihan permohonan harus menjaga sendiri
	 *  invariant itu.</p> */
	@Column(name = "primary_choice")
	public Boolean getPrimaryChoice() {
		return primaryChoice == null ? Boolean.FALSE : primaryChoice;
	}

	/**
	 * Tandai/lepas status pilihan utama.
	 *
	 * @param primaryChoice {@code TRUE} untuk pilihan utama permohonan
	 */
	public void setPrimaryChoice(Boolean primaryChoice) {
		this.primaryChoice = primaryChoice;
	}

	/** Wajib diisi bila jenis usaha = LAINNYA (validasi server, bukan sekadar UI).
	 *
	 *  <p>Pemeriksaannya berada di {@code PendaftaranTenantService.submit(...)}: bila ada jenis
	 *  usaha ber-{@code code} {@code LAINNYA} sementara keterangan kosong, submit ditolak
	 *  ({@code BUSINESS_TYPE_OTHER_REQUIRED}) dan transaksi di-rollback -- sehingga menonaktifkan
	 *  validasi di sisi browser tidak menolong penyerang.</p>
	 *
	 *  <p>Isinya teks bebas dari internet (dipotong 500 karakter): wajib di-escape saat
	 *  ditampilkan di backoffice, dan tidak boleh dijadikan dasar keputusan program. Keterangan
	 *  yang sama juga disalin ke {@code Pendaftar.keterangan} sebagai catatan.</p> */
	@Column(name = "other_description", length = 500)
	public String getOtherDescription() {
		return otherDescription;
	}

	/**
	 * Simpan keterangan jenis usaha lainnya.
	 *
	 * @param otherDescription teks keterangan dari formulir
	 */
	public void setOtherDescription(String otherDescription) {
		this.otherDescription = otherDescription;
	}

	/**
	 * Waktu baris pilihan dibuat -- sama dengan waktu submit permohonannya karena keduanya ditulis
	 * dalam satu transaksi dengan stempel waktu yang sama.
	 *
	 * @return waktu pembuatan baris
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Tetapkan waktu pembuatan baris.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nama pengguna yang terakhir menyentuh baris (shadow audit AIS); pada jalur publik diisi
	 * literal {@code "pendaftaran"} karena tidak ada pengguna login.
	 *
	 * @return penanda pengubah terakhir
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Isi penanda pengubah terakhir. Nilai null/kosong sengaja diabaikan (pola shadow audit AIS)
	 * agar jejak lama tidak terhapus oleh pemanggil yang lupa mengisinya.
	 *
	 * @param oleh nama pengubah; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna yang terakhir menyentuh baris (pendamping {@link #getOleh()}).
	 *
	 * @return id pengubah terakhir
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Isi id pengubah terakhir; nilai null/kosong diabaikan, sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id pengubah; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu perubahan terakhir (shadow audit AIS), disegarkan hook {@code @PreUpdate}
	 * lewat {@code AuditTimestampInterceptor} dan sudah terisi sejak objek dibentuk.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tetapkan stempel waktu perubahan terakhir (umumnya hanya dipanggil interceptor audit).
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
