package ais.database.model.tenant;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Katalog Jenis Usaha/Instansi tenant (control-plane, schema public).</h3>
 *
 * <p>Sumber pilihan multi-select pada wizard {@code Common.ROOT + "/pendaftaran"} --
 * form TIDAK meng-hard-code daftar jenis usaha; seed awal 14 jenis diisi idempoten oleh
 * {@code ais.service.registration.JenisUsahaTenantSeedService} dan selebihnya dikelola
 * Platform Admin. {@link #getCode()} IMMUTABLE (kunci logika/mapping), {@link #getNama()}
 * bebas diubah utk tampilan.</p>
 *
 * <p>CATATAN NAMA: kelas ini TIDAK ada hubungannya dgn {@code inventory.SetoranTenant}
 * ("tenant" = penyewa stan/kios bagi-hasil) -- "tenant" di paket ini = workspace
 * multi-tenant SaaS (lihat docs/pendaftaran-tenant/01-source-audit.md §8).</p>
 *
 * <h4>Entity ini hanya-baca bagi publik</h4>
 *
 * <p>Baris di tabel ini SELALU dibuat/diubah dari sisi platform, tidak pernah dari formulir
 * pendaftaran: penulisnya hanya {@code JenisUsahaTenantSeedService.pastikanSeed()} (dipanggil
 * sekali saat {@code init()} servlet pendaftaran, idempoten) dan pengelolaan Platform Admin.
 * Jalur publik hanya MEMBACA: {@code PendaftaranTenantService.katalog()} menyusun payload kartu
 * jenis usaha untuk wizard, dan {@code submit(...)} memuat ulang baris terpilih untuk
 * memvalidasi bahwa id yang dikirim pendaftar benar-benar ada dan aktif.</p>
 *
 * <p>Karena itu setiap kolom di sini adalah data tepercaya (admin-controlled) dari sudut pandang
 * pendaftar -- dan sebaliknya, seluruhnya bersifat menentukan bagi pendaftar: mengubah
 * {@link #getRequiresManualReview()} atau {@link #getAktif()} pada satu baris langsung mengubah
 * perilaku pendaftaran semua orang yang memilih jenis usaha itu.</p>
 *
 * @see JenisUsahaTenantModule
 * @see PendaftaranTenantJenisUsaha
 * @see PendaftaranTenant
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_usaha_tenant")
public class JenisUsahaTenant extends GeneralValueObject {

	/** Versi serialisasi Java standar entity AIS. */
	private static final long serialVersionUID = 1L;

	/** Primary key surrogate (identity, di-generate database). */
	private Long id;
	/** Kode stabil dan immutable yang menjadi kunci logika. */
	private String code;
	/** Nama tampilan kartu jenis usaha. */
	private String nama;
	/** Penjelasan panjang untuk kartu wizard. */
	private String deskripsi;
	/** Nama/kelas ikon kartu. */
	private String icon;
	/** Urutan tampil kartu pada wizard. */
	private Integer displayOrder;
	/** Penanda jenis usaha masih boleh dipilih. */
	private Boolean aktif;
	/** Penanda permohonan wajib direview manual admin. */
	private Boolean requiresManualReview;
	/** Kode bundel modul bawaan (belum dibaca kode mana pun). */
	private String defaultModuleBundleCode;
	/** Waktu baris master dibuat. */
	private Date createdAt;
	/** Nomor versi optimistic locking Hibernate. */
	private Integer version;

	/** Nama pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir + hook {@code @PreUpdate} lewat
	 * {@code AuditTimestampInterceptor}. Trio {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * adalah keharusan teknis pola entity AIS (dipakai layar audit generik), berdampingan dengan
	 * riwayat Envers dan bukan penggantinya.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default wajib JavaBean/Hibernate. */
	public JenisUsahaTenant() {
	}

	/**
	 * Primary key baris master. Inilah nilai yang dikirim wizard sebagai {@code jenisUsahaIds} dan
	 * yang dipakai {@link PendaftaranTenantJenisUsaha} serta {@link JenisUsahaTenantModule} sebagai
	 * kunci join.
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

	/** Kode stabil huruf besar (mis. {@code APOTEK}, {@code INVENTORY_SALES}) -- IMMUTABLE setelah seed.
	 *
	 *  <p>Unique di seluruh tabel dan menjadi kunci pencocokan idempoten seed: {@code pastikanSeed()}
	 *  mencari baris berdasarkan kode, sehingga menjalankan seed berkali-kali tidak menggandakan
	 *  katalog. Kode juga dibaca logika bisnis secara langsung -- mis. pemeriksaan {@code LAINNYA}
	 *  yang mewajibkan keterangan tambahan saat submit, dan penurunan flag
	 *  {@code Pendaftar.merupakanSekolah} dari kode {@code SEKOLAH}. Mengganti kode baris yang
	 *  sudah dipakai akan memutus pencocokan itu sekaligus membuat seed berikutnya menyisipkan
	 *  baris duplikat; ubah {@link #getNama()} saja bila yang diinginkan hanya perubahan
	 *  tampilan.</p> */
	@Column(name = "code", unique = true, nullable = false, length = 64)
	public String getCode() {
		return code;
	}

	/**
	 * Tetapkan kode stabil jenis usaha (hanya saat pembuatan baris baru).
	 *
	 * @param code kode huruf besar tanpa spasi
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * Nama tampilan kartu jenis usaha pada wizard. Getter mengembalikan versi ter-{@code trim}
	 * (dan {@code null} tetap {@code null}) -- perapian dibaca-saja yang tidak menulis balik ke
	 * field, sehingga spasi berlebih di database tidak ikut merusak tampilan.
	 *
	 * @return nama tampilan tanpa spasi tepi, atau {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	/**
	 * Tetapkan nama tampilan jenis usaha (bebas diubah kapan pun, tidak memutus logika mana pun).
	 *
	 * @param nama nama tampilan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Penjelasan panjang yang ditampilkan pada kartu wizard (kolom {@code text}, tanpa batas
	 * panjang praktis). Berasal dari admin/seed, bukan dari pendaftar; tetap harus dirender aman
	 * di sisi klien -- wizard menyalurkannya lewat {@code textContent}, bukan {@code innerHTML}.
	 *
	 * @return deskripsi jenis usaha, atau {@code null}
	 */
	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi;
	}

	/**
	 * Tetapkan deskripsi jenis usaha.
	 *
	 * @param deskripsi teks penjelasan
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Nama/kelas ikon kartu jenis usaha yang dipakai antarmuka wizard. Murni kosmetik.
	 *
	 * @return nama ikon, atau {@code null}
	 */
	@Column(name = "icon", length = 100)
	public String getIcon() {
		return icon;
	}

	/**
	 * Tetapkan nama ikon kartu.
	 *
	 * @param icon nama/kelas ikon
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Urutan tampil kartu pada wizard (menaik). Getter mengembalikan {@code 0} bila kolom kosong
	 * -- default dibaca-saja yang tidak menulis balik ke field, sehingga baris tanpa urutan
	 * berkumpul di awal daftar alih-alih menyebabkan pengurutan gagal.
	 *
	 * @return urutan tampil, {@code 0} bila belum diisi
	 */
	@Column(name = "display_order")
	public Integer getDisplayOrder() {
		return displayOrder == null ? Integer.valueOf(0) : displayOrder;
	}

	/**
	 * Tetapkan urutan tampil kartu.
	 *
	 * @param displayOrder nomor urut menaik
	 */
	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	/**
	 * Penanda jenis usaha masih boleh dipilih pendaftar. Getter mengembalikan {@code TRUE} bila
	 * kolom kosong -- default dibaca-saja yang bersifat PERMISIF: baris baru hasil seed/insert
	 * manual yang lupa mengisi kolom ini langsung tampil di wizard. Untuk menyembunyikan sebuah
	 * jenis usaha, kolom harus diisi {@code false} secara eksplisit; mengosongkannya tidak cukup.
	 *
	 * <p>Flag ini ditegakkan di dua tempat, keduanya server-side: katalog hanya menyertakan baris
	 * {@code aktif=true}, dan {@code submit(...)} menolak id yang menunjuk baris nonaktif
	 * ({@code BUSINESS_TYPE_INVALID}) sehingga penyerang tidak dapat memilih jenis usaha
	 * tersembunyi hanya dengan menebak/menyusun ulang id.</p>
	 *
	 * @return {@code TRUE} bila jenis usaha aktif (termasuk saat kolom kosong)
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Aktifkan/nonaktifkan jenis usaha di katalog. Menonaktifkan hanya menghentikan pemilihan
	 * BARU; permohonan dan tenant lama yang sudah menunjuk baris ini tetap utuh.
	 *
	 * @param aktif {@code FALSE} untuk menyembunyikan dari wizard
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** True = permohonan yang memilih jenis ini masuk REVIEW_PENDING (perlu persetujuan manual admin).
	 *
	 *  <p>Ditegakkan {@code PendaftaranTenantService.perluManualReview(...)} yang dipanggil dari
	 *  dalam {@code verifikasiEmail}/{@code verifikasiTanpaToken}: pemeriksaan membaca ulang baris
	 *  master lewat pilihan {@link PendaftaranTenantJenisUsaha} milik permohonan, jadi flag ini
	 *  tidak pernah dikirim atau bisa dimatikan oleh klien. Cukup SATU jenis usaha terpilih
	 *  ber-nilai {@code true} untuk membelokkan permohonan ke
	 *  {@link PendaftaranTenant#STATUS_REVIEW_PENDING}; keluar dari sana hanya lewat aksi admin
	 *  yang digerbangi {@code adminBerwenang}. Konfigurasi platform
	 *  {@code pendaftaran_wajib_review_manual} dapat memaksa review untuk SEMUA jenis usaha,
	 *  terlepas dari nilai kolom ini.</p>
	 *
	 *  <p>Getter mengembalikan {@code FALSE} bila kolom kosong (default dibaca-saja). Perhatikan
	 *  arah defaultnya: baris yang lupa diisi akan LOLOS otomatis tanpa review manual, sehingga
	 *  jenis usaha yang memang berisiko harus ditandai {@code true} secara eksplisit.</p> */
	@Column(name = "requires_manual_review")
	public Boolean getRequiresManualReview() {
		return requiresManualReview == null ? Boolean.FALSE : requiresManualReview;
	}

	/**
	 * Tetapkan kewajiban review manual untuk jenis usaha ini.
	 *
	 * @param requiresManualReview {@code TRUE} agar permohonan wajib disetujui admin
	 */
	public void setRequiresManualReview(Boolean requiresManualReview) {
		this.requiresManualReview = requiresManualReview;
	}

	/**
	 * Kode bundel modul bawaan jenis usaha ini.
	 *
	 * <p><strong>FIELD TIDUR.</strong> Satu-satunya penulis adalah
	 * {@code JenisUsahaTenantSeedService} yang mengisinya sama persis dengan {@link #getCode()},
	 * dan tidak ada satu pun pembaca di seluruh kode. Daftar modul yang benar-benar dipakai
	 * dibentuk dari baris {@link JenisUsahaTenantModule} per jenis usaha, bukan dari kolom ini.
	 * Jangan menjadikannya acuan entitlement tanpa lebih dulu membangun mekanisme bundel yang
	 * sesungguhnya.</p>
	 *
	 * @return kode bundel modul, atau {@code null}
	 */
	@Column(name = "default_module_bundle_code", length = 64)
	public String getDefaultModuleBundleCode() {
		return defaultModuleBundleCode;
	}

	/**
	 * Tetapkan kode bundel modul bawaan.
	 *
	 * @param defaultModuleBundleCode kode bundel
	 */
	public void setDefaultModuleBundleCode(String defaultModuleBundleCode) {
		this.defaultModuleBundleCode = defaultModuleBundleCode;
	}

	/**
	 * Waktu baris master dibuat (diisi seed/admin saat penyisipan).
	 *
	 * @return waktu pembuatan baris, atau {@code null} untuk baris lama
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Tetapkan waktu pembuatan baris master.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nomor versi optimistic locking Hibernate ({@code @Version}), naik otomatis setiap update.
	 * Menjaga agar penyuntingan katalog oleh admin tidak bertabrakan diam-diam dengan proses seed
	 * yang berjalan saat startup. Jangan diisi manual.
	 *
	 * @return nomor versi baris
	 */
	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	/**
	 * Setter versi -- eksklusif untuk Hibernate.
	 *
	 * @param version nomor versi
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Nama pengguna yang terakhir menyentuh baris master (shadow audit AIS).
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
	 * Id pengguna yang terakhir menyentuh baris master (pendamping {@link #getOleh()}).
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
