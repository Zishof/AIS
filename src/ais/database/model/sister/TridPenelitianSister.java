package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * <h2>Entitas Tridharma tersinkron SISTER &mdash; Penelitian</h2>
 *
 * <p>Baris tabel {@code sister_trid_penelitian}, hasil sinkronisasi endpoint SISTER
 * (Sistem Informasi Sumber Daya Terintegrasi, Kemdikbudristek) <b>{@code penelitian}</b> &mdash; salah satu
 * endpoint Tridharma "karier" (query {@code id_sdm}, top-level, BUKAN {@code data_pribadi/*}) yang memuat
 * daftar kegiatan penelitian seorang dosen yang telah dilaporkan/tercatat di SISTER. Kelas ini adalah
 * REPRESENTASI TERSTRUKTUR (satu baris tabel relasional per item penelitian) dari respons JSON SISTER,
 * berdampingan dengan cadangan JSON mentah yang SELALU ditulis ke {@link ais.database.model.DataSister}
 * (lihat catatan mekanisme di bawah) &mdash; keduanya ditulis dalam satu penarikan jaringan yang sama.</p>
 *
 * <h3>Mekanisme sinkronisasi (bukan entity yatim)</h3>
 * <p>Kelas ini BUKAN entity pasif/tidur: ia terdaftar di {@link ais.database.model.sister.SisterEntitasRegistry}
 * di bawah kunci {@code "penelitian"} (peta endpoint&nbsp;&rarr;&nbsp;kelas entitas), dan {@link ais.common.DataSisterApi}
 * &mdash; satu-satunya klien SISTER pada e-Campus (autentikasi + tarik data via {@code curl}, base URL konfigurasi
 * {@code sister_host_url}, bawaan {@code https://sister-api.kemdikbud.go.id/ws.php/1.0}) &mdash; memakai peta itu untuk
 * merutekan tiap baris JSON endpoint {@code penelitian} ke instance kelas ini secara REFLEKTIF: nama field JSON
 * snake_case (mis. {@code bidang_keilmuan}) dikonversi ke PascalCase lalu dicari method {@code setBidangKeilmuan}
 * lewat refleksi ({@code Method[] cls.getMethods()}, di-cache per kelas), nilai JSON di-coerce sesuai tipe
 * parameter setter (String/Integer/Long/Double/Boolean; tipe lain seperti {@code Date} dilewati). Upsert
 * memakai {@link #getKode()} (id item SISTER) sebagai kunci pencocokan terhadap baris yang sudah ada
 * (peta {@code kode -> id} diproyeksi ringan lebih dulu); item baru dibuat via {@code cls.newInstance()}.
 * Endpoint {@code penelitian} adalah salah satu dari HANYA 5 endpoint Tridharma yang BER-PAGINASI di SISTER
 * ({@code penelitian, publikasi, pengabdian, penunjang_lain, kekayaan_intelektual}) &mdash; ditarik per halaman
 * ({@code page}/{@code per_page}, bawaan 100/halaman) hingga sebuah halaman berisi kurang dari {@code per_page}
 * item, tiap halaman langsung di-upsert (tidak menumpuk seluruh data di memori).</p>
 *
 * <h3>Konvensi kolom (berlaku pada seluruh keluarga {@code Trid*Sister}/{@code Ref*Sister}/{@code Dp*Sister})</h3>
 * <ul>
 *   <li>{@link #getKode()} &mdash; id item pada sisi SISTER; KUNCI UPSERT (bukan primary key lokal). Baris dengan
 *       {@code kode} sama pada penarikan berikutnya akan MEMPERBARUI baris ini, bukan membuat baris baru.</li>
 *   <li>{@link #getKeterangan()} &mdash; SALINAN JSON MENTAH satu item (cadangan/audit sumber; lihat juga
 *       tabel {@link ais.database.model.DataSister} yang menyimpan cadangan serupa di tingkat endpoint).</li>
 *   <li>{@link #getAktif()} &mdash; flag aktif, BAWAAN {@code true} bila belum pernah diisi ({@code null}). PERLU
 *       DIWASPADAI: mesin sinkronisasi ({@code DataSisterApi.upsertEntitas}) TIDAK PERNAH memanggil
 *       {@link #setAktif(Boolean)} &mdash; flag ini SATU ARAH (hanya bisa diaktifkan/dinonaktifkan manual, mis. lewat
 *       CRUD generik), dan item yang SUDAH HILANG dari respons SISTER TIDAK otomatis dinonaktifkan
 *       (tak ada soft-delete pada jalur sinkron). Pola arsitektur ini konsisten dengan flag aktif satu-arah
 *       yang sudah tercatat pada domain lain di codebase AIS &mdash; bukan kerentanan baru.</li>
 *   <li>{@link #getIdSdm()} &mdash; id dosen pemilik (sisi SISTER, cocokkan ke {@code referensi/sdm}/
 *       {@code RefSdmSister}); DISIMPAN SEBAGAI STRING POLOS, BUKAN relasi JPA
 *       ({@code @ManyToOne})&nbsp;&mdash; tak ada penegakan integritas referensial di level basis data.</li>
 *   <li>{@code oleh}/{@code olehId}/{@code tanggal_dirubah} &mdash; trio audit yang DITULIS ULANG (shadow) di
 *       setiap subclass {@link ais.database.model.GeneralValueObject}: induk sudah punya implementasi
 *       getter/setter yang sama persis, tetapi TANPA pemetaan kolom ({@code @Column}) yang bisa dipetakan
 *       ke tabel spesifik subclass. Pengulangan ini adalah KEHARUSAN TEKNIS Hibernate (bukan salin-tempel
 *       yang keliru): setiap entity konkret butuh anotasi/naming-nya sendiri. Diisi otomatis oleh
 *       {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} pada setiap {@code @PreUpdate}
 *       (lihat {@link #onUpdate()}).</li>
 *   <li>{@code @Audited} (Hibernate Envers) &mdash; setiap perubahan baris direkam ke tabel bayangan
 *       {@code sister_trid_penelitian_aud} yang dibuat otomatis oleh {@code hbm2ddl} (bukan migrasi manual).</li>
 * </ul>
 *
 * <p>Field bisnis endpoint ini: {@link #getJudul() judul}, {@link #getBidangKeilmuan() bidangKeilmuan},
 * {@link #getTahunPelaksanaan() tahunPelaksanaan}, {@link #getLamaKegiatan() lamaKegiatan} &mdash; lihat javadoc
 * masing-masing getter untuk detail.</p>
 *
 * <p>Kelas-kelas {@code Trid*Sister} lain (representasi endpoint Tridharma lain seperti
 * {@code jabatan_fungsional}, {@code kepangkatan}, {@code diklat}, dst.) mengikuti KERANGKA STRUKTUR YANG
 * IDENTIK dengan kelas ini (field audit, {@code kode}/{@code keterangan}/{@code aktif}/{@code idSdm}, mekanisme
 * upsert reflektif yang sama) &mdash; hanya berbeda pada nama tabel, endpoint, dan field bisnis spesifiknya.
 * Javadoc kelas tersebut MERUJUK BALIK ke sini ({@code @see}) untuk detail mekanisme yang sama, dan
 * menjelaskan sendiri field bisnis spesifiknya.</p>
 *
 * @see ais.database.model.sister.SisterEntitasRegistry
 * @see ais.common.DataSisterApi
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_penelitian")
public class TridPenelitianSister extends GeneralValueObject {
	/** Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;
	/** Primary key lokal (surrogate), digenerate basis data (IDENTITY); TIDAK sama dengan {@link #kode} (id sisi SISTER). */
	private Long id;
	/** Nama pengguna terakhir pengubah baris (audit); lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir pengubah baris (audit); lihat {@link #getOlehId()}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Id item pada sisi SISTER (kunci upsert); lihat {@link #getKode()}. */
	private String kode;
	/** Salinan JSON mentah satu item dari respons SISTER (cadangan/audit sumber). */
	private String keterangan;
	/** Flag aktif; {@code null} diperlakukan sebagai aktif ({@code true}) oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Id dosen pemilik pada sisi SISTER (string polos, bukan relasi JPA); lihat {@link #getIdSdm()}. */
	private String idSdm;
	/** Judul kegiatan penelitian, sebagaimana dilaporkan ke SISTER. */
	private String judul;
	/** Bidang keilmuan penelitian (kode/label sisi SISTER, umumnya merujuk referensi kelompok bidang). */
	private String bidangKeilmuan;
	/** Tahun pelaksanaan kegiatan penelitian. */
	private Integer tahunPelaksanaan;
	/** Lama kegiatan penelitian (satuan sesuai konvensi SISTER, umumnya bulan). */
	private Integer lamaKegiatan;

	/** Konstruktor bawaan (dibutuhkan Hibernate &amp; refleksi upsert {@code DataSisterApi}); field diisi lewat setter. */
	public TridPenelitianSister() {}

	/**
	 * @return primary key lokal (surrogate, IDENTITY), atau {@code null} bila entity belum tersimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }

	/**
	 * Menyetel primary key lokal. Kolom dipetakan {@code insertable = false} &mdash; nilai SELALU berasal
	 * dari basis data (IDENTITY autoincrement); method ini dipakai Hibernate saat memuat entity, bukan
	 * untuk menetapkan id secara manual sebelum simpan.
	 *
	 * @param id primary key lokal
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() { return olehId; }

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong DIABAIKAN DIAM-DIAM (jejak audit
	 * yang sudah terisi tidak bisa terhapus oleh jalur simpan yang kebetulan tak membawa info pengguna,
	 * mis. proses batch {@link ais.common.DataSisterApi}) &mdash; sama seperti kontrak
	 * {@link ais.database.model.GeneralValueObject#setOlehId(String)} yang di-shadow method ini.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }

	/**
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan validasi sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }

	/**
	 * Hook {@code @PreUpdate}: mengisi ulang {@link #tanggal_dirubah}/{@link #oleh}/{@link #olehId} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tiap kali baris di-UPDATE.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

	/**
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi (dipanggil pula oleh {@link #onUpdate()}).
	 *
	 * @param t waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }

	/**
	 * @return id item sisi SISTER (kunci upsert), sudah di-{@code trim()}, atau {@code null} bila kosong/belum diisi
	 */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }

	/**
	 * Menyetel id item sisi SISTER (kunci upsert oleh {@link ais.common.DataSisterApi}). Tanpa validasi;
	 * normalisasi (trim/kosong&rarr;null) dilakukan pada {@link #getKode()}, bukan di sini.
	 *
	 * @param kode id item sisi SISTER
	 */
	public void setKode(String kode) { this.kode = kode; }

	/**
	 * @return salinan JSON mentah item ini (cadangan/audit sumber)
	 */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }

	/**
	 * @param k salinan JSON mentah item ini
	 */
	public void setKeterangan(String k) { this.keterangan = k; }

	/**
	 * @return flag aktif; {@code true} bila belum pernah diisi ({@code null}) &mdash; lihat catatan kelas soal
	 *         flag ini yang TIDAK pernah disetel oleh mesin sinkronisasi otomatis
	 */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }

	/**
	 * @param a flag aktif baru
	 */
	public void setAktif(Boolean a) { this.aktif = a; }

	/**
	 * @return id dosen pemilik pada sisi SISTER (string polos, bukan relasi JPA)
	 */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }

	/**
	 * @param v id dosen pemilik pada sisi SISTER
	 */
	public void setIdSdm(String v) { this.idSdm = v; }

	/**
	 * @return judul kegiatan penelitian
	 */
	@Column(name = "judul", columnDefinition = "text") public String getJudul() { return judul; }

	/**
	 * @param v judul kegiatan penelitian
	 */
	public void setJudul(String v) { this.judul = v; }

	/**
	 * @return bidang keilmuan penelitian
	 */
	@Column(name = "bidang_keilmuan", columnDefinition = "text") public String getBidangKeilmuan() { return bidangKeilmuan; }

	/**
	 * @param v bidang keilmuan penelitian
	 */
	public void setBidangKeilmuan(String v) { this.bidangKeilmuan = v; }

	/**
	 * @return tahun pelaksanaan kegiatan penelitian
	 */
	@Column(name = "tahun_pelaksanaan") public Integer getTahunPelaksanaan() { return tahunPelaksanaan; }

	/**
	 * @param v tahun pelaksanaan kegiatan penelitian
	 */
	public void setTahunPelaksanaan(Integer v) { this.tahunPelaksanaan = v; }

	/**
	 * @return lama kegiatan penelitian (umumnya dalam bulan, mengikuti konvensi SISTER)
	 */
	@Column(name = "lama_kegiatan") public Integer getLamaKegiatan() { return lamaKegiatan; }

	/**
	 * @param v lama kegiatan penelitian
	 */
	public void setLamaKegiatan(Integer v) { this.lamaKegiatan = v; }

	/**
	 * @return representasi ringkas {@code id-kode}, dipakai untuk log/debug
	 */
	@Override public String toString() { return id + "-" + kode; }
}
