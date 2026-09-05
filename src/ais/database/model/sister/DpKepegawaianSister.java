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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/kepegawaian</b>. Kolom bernama = field
 * SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto
 * hbm2ddl).
 *
 * <h2>Mekanisme sinkronisasi &amp; relasi antar entitas</h2>
 * <p>Mekanisme sinkronisasi, ketiadaan relasi header/detail antar tujuh kelas {@code Dp*Sister}, serta
 * ketiadaan foreign key ke {@code Pegawai}/{@code Dosen} internal SAMA PERSIS dengan yang dijelaskan lengkap
 * pada javadoc {@link DpProfilSister} — lihat kelas itu untuk rincian (diisi oleh
 * {@code ais.common.DataSisterApi#synDataDosen(java.util.List)}, dipetakan lewat
 * {@link ais.database.model.sister.SisterEntitasRegistry} berdasarkan kunci
 * {@code "data_pribadi/kepegawaian"}, {@code kode} dipaksa sama dengan {@link #getIdSdm()}). Kelas ini
 * adalah SAUDARA {@code DpProfilSister}, bukan detailnya. Perlu dicatat: identifier di kelas ini
 * ({@link #getNip()}, {@link #getNidn()}, {@link #getNuptk()}) TIDAK dipakai kode mana pun untuk mencari
 * baris {@code Pegawai}/{@code Dosen} — satu-satunya pencocokan silang yang ditemukan (NIDN, di
 * {@code DataSisterAction}) bekerja terhadap {@code RefSdmSister}, bukan tabel ini.</p>
 *
 * <h2>Peringatan privasi</h2>
 * <p>Tabel ini menyimpan identifier kepegawaian resmi (NIP, NIDN, NUPTK, nomor SK CPNS) yang bisa dipakai
 * untuk korelasi identitas lintas sistem pemerintah. Sama seperti {@code DpProfilSister}, tidak ada kolom
 * scoping tenant/satuan-kerja dan (per 5 Sep 2026) tidak ditemukan Action/JSP yang membaca balik tabel ini
 * untuk ditampilkan — risiko murni pada sisi tulis lewat layar "Data SISTER". Memperkuat pola tercatat
 * (filter tenant lemah/hilang pada entitas SISTER), bukan temuan baru yang berdiri sendiri.</p>
 *
 * @see DpProfilSister
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_kepegawaian")
public class DpKepegawaianSister extends GeneralValueObject {
	/** Versi serialisasi tetap; lihat konvensi {@link GeneralValueObject#serialVersionUID}. */
	private static final long serialVersionUID = 1L;
	/** Primary key baris (surrogate, auto-generate). Bukan {@code idSdm} SISTER — lihat {@link #getIdSdm()}. */
	private Long id;
	/** Nama pengguna/proses yang terakhir menulis baris ini; diisi otomatis lewat jalur audit, lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna/proses yang terakhir menulis baris ini; diisi otomatis lewat jalur audit, lihat {@link #setOlehId(String)}. */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat, diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kunci upsert baris; pada jalur sinkronisasi objek data_pribadi nilainya selalu dipaksa sama dengan {@link #getIdSdm()}. */
	private String kode;
	/** JSON mentah respons endpoint SISTER untuk satu dosen ({@code data_pribadi/kepegawaian/{id_sdm}} apa adanya), disimpan berdampingan dengan kolom terstruktur sebagai cadangan. */
	private String keterangan;
	/** Penanda baris aktif; lihat {@link #getAktif()} untuk perilaku default {@code true}. */
	private Boolean aktif;
	/** Id SDM SISTER milik pemilik baris ini; lihat penjelasan lengkap ketiadaan foreign key pada javadoc {@link DpProfilSister}. */
	private String idSdm;
	/** Nomor Induk Pegawai (NIP) dosen/SDM sebagaimana tercatat di SISTER. */
	private String nip;
	/** Nomor Surat Keputusan (SK) pengangkatan Calon Pegawai Negeri Sipil (CPNS) dosen/SDM. */
	private String skCpns;
	/** Tanggal SK CPNS, disimpan sebagai teks mentah (bukan {@link Date}), format mengikuti apa pun yang dikirim API. */
	private String tanggalSkCpns;
	/** Nomor SK TMMD (terhitung mulai melaksanakan diklat/dinas — sesuai istilah SISTER) dosen/SDM. */
	private String skTmmd;
	/** Nilai TMMD (terhitung mulai) yang menyertai {@link #getSkTmmd()}, disimpan sebagai teks mentah. */
	private String tmmd;
	/** Kode referensi sumber gaji SISTER; padanan id ke tabel referensi sumber gaji, disimpan sebagai angka mentah tanpa FK. */
	private Integer idSumberGaji;
	/** Nama/label sumber gaji dosen/SDM sebagaimana tercatat di SISTER. */
	private String sumberGaji;
	/** Nomor Induk Dosen Nasional (NIDN) dosen/SDM sebagaimana tercatat di SISTER. */
	private String nidn;
	/** Nomor Unik Pendidik dan Tenaga Kependidikan (NUPTK) dosen/SDM sebagaimana tercatat di SISTER. */
	private String nuptk;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil query. */
	public DpKepegawaianSister() {}
	/**
	 * Mengembalikan primary key baris.
	 *
	 * @return primary key, atau {@code null} bila baris belum tersimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel primary key baris. Tanpa validasi; kolom dipetakan {@code insertable = false} sehingga nilai
	 * ini murni hasil generate database.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) { this.id = id; }
	/**
	 * Mengembalikan id pengguna/proses yang terakhir menulis baris ini.
	 *
	 * @return id pengguna/proses pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna/proses pengubah terakhir, dengan validasi non-trivial: nilai {@code null} atau
	 * string kosong/spasi diabaikan diam-diam sehingga jejak audit yang sudah terisi tidak bisa terhapus.
	 * Field ini mendeklarasikan ulang &amp; menimpa {@code oleh}/{@code olehId} milik
	 * {@link GeneralValueObject} agar terpetakan sebagai kolom pada tabel entitas ini — pola "audit shadow
	 * field" yang berulang di seluruh entity Sister, keharusan teknis pemetaan Hibernate, bukan bug.
	 *
	 * @param olehId id pengguna/proses pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/**
	 * Mengembalikan nama pengguna/proses yang terakhir menulis baris ini.
	 *
	 * @return nama pengguna/proses pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }
	/**
	 * Menyetel nama pengguna/proses pengubah terakhir, dengan validasi non-trivial yang sama seperti
	 * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna/proses pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/**
	 * Hook {@code @PreUpdate}: dipanggil Hibernate tepat sebelum UPDATE dieksekusi, menyerahkan penetapan
	 * {@code tanggal_dirubah}/{@code oleh}/{@code olehId} terkini kepada
	 * {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} kecuali sengaja disetel demikian
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi; biasanya diserahkan ke {@link #onUpdate()}.
	 *
	 * @param t waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/**
	 * Mengembalikan kunci upsert baris, dinormalkan: string kosong diubah menjadi {@code null}, selain itu
	 * di-{@code trim()}.
	 *
	 * @return kode ter-trim, atau {@code null} bila belum terisi/kosong
	 */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/**
	 * Menyetel kunci upsert baris. Tanpa validasi/normalisasi (normalisasi dilakukan di {@link #getKode()}).
	 *
	 * @param kode nilai kode baru
	 */
	public void setKode(String kode) { this.kode = kode; }
	/**
	 * Mengembalikan JSON mentah respons SISTER untuk baris ini.
	 *
	 * @return JSON mentah sebagai teks, atau {@code null} bila belum terisi
	 */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/**
	 * Menyetel JSON mentah respons SISTER untuk baris ini. Tanpa validasi format.
	 *
	 * @param k teks JSON baru
	 */
	public void setKeterangan(String k) { this.keterangan = k; }
	/**
	 * Mengembalikan status aktif baris. Nilai {@code null} pada kolom dianggap {@code true} (default aktif)
	 * — pola default-aktif yang berulang di seluruh entity Sister/DataSister.
	 *
	 * @return {@code true} bila baris aktif atau kolom {@code null}; {@code false} hanya bila eksplisit disetel demikian
	 */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/**
	 * Menyetel status aktif baris. Tanpa validasi.
	 *
	 * @param a status aktif baru
	 */
	public void setAktif(Boolean a) { this.aktif = a; }
	/**
	 * Mengembalikan id SDM SISTER milik pemilik baris ini.
	 *
	 * @return id SDM SISTER, atau {@code null} bila belum terisi
	 */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	/**
	 * Menyetel id SDM SISTER milik pemilik baris ini. Tanpa validasi.
	 *
	 * @param v id SDM SISTER baru
	 */
	public void setIdSdm(String v) { this.idSdm = v; }
	/**
	 * Mengembalikan Nomor Induk Pegawai (NIP) dosen/SDM.
	 *
	 * @return NIP, atau {@code null} bila belum terisi
	 */
	@Column(name = "nip", columnDefinition = "text") public String getNip() { return nip; }
	/**
	 * Menyetel Nomor Induk Pegawai (NIP) dosen/SDM. Tanpa validasi format.
	 *
	 * @param v NIP baru
	 */
	public void setNip(String v) { this.nip = v; }
	/**
	 * Mengembalikan nomor SK pengangkatan CPNS dosen/SDM.
	 *
	 * @return nomor SK CPNS, atau {@code null} bila belum terisi
	 */
	@Column(name = "sk_cpns", columnDefinition = "text") public String getSkCpns() { return skCpns; }
	/**
	 * Menyetel nomor SK pengangkatan CPNS dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor SK CPNS baru
	 */
	public void setSkCpns(String v) { this.skCpns = v; }
	/**
	 * Mengembalikan tanggal SK CPNS sebagai teks mentah (bukan {@link Date}) — format mengikuti apa pun
	 * yang dikirim API, tidak diparse/divalidasi di sini.
	 *
	 * @return tanggal SK CPNS mentah, atau {@code null} bila belum terisi
	 */
	@Column(name = "tanggal_sk_cpns", columnDefinition = "text") public String getTanggalSkCpns() { return tanggalSkCpns; }
	/**
	 * Menyetel tanggal SK CPNS. Tanpa validasi/parsing.
	 *
	 * @param v tanggal SK CPNS mentah baru
	 */
	public void setTanggalSkCpns(String v) { this.tanggalSkCpns = v; }
	/**
	 * Mengembalikan nomor SK TMMD dosen/SDM.
	 *
	 * @return nomor SK TMMD, atau {@code null} bila belum terisi
	 */
	@Column(name = "sk_tmmd", columnDefinition = "text") public String getSkTmmd() { return skTmmd; }
	/**
	 * Menyetel nomor SK TMMD dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor SK TMMD baru
	 */
	public void setSkTmmd(String v) { this.skTmmd = v; }
	/**
	 * Mengembalikan nilai TMMD yang menyertai {@link #getSkTmmd()}, sebagai teks mentah.
	 *
	 * @return nilai TMMD mentah, atau {@code null} bila belum terisi
	 */
	@Column(name = "tmmd", columnDefinition = "text") public String getTmmd() { return tmmd; }
	/**
	 * Menyetel nilai TMMD. Tanpa validasi/parsing.
	 *
	 * @param v nilai TMMD mentah baru
	 */
	public void setTmmd(String v) { this.tmmd = v; }
	/**
	 * Mengembalikan kode referensi sumber gaji SISTER (padanan id ke tabel referensi sumber gaji, tanpa FK).
	 *
	 * @return kode sumber gaji, atau {@code null} bila belum terisi
	 */
	@Column(name = "id_sumber_gaji") public Integer getIdSumberGaji() { return idSumberGaji; }
	/**
	 * Menyetel kode referensi sumber gaji SISTER. Tanpa validasi.
	 *
	 * @param v kode sumber gaji baru
	 */
	public void setIdSumberGaji(Integer v) { this.idSumberGaji = v; }
	/**
	 * Mengembalikan nama/label sumber gaji dosen/SDM.
	 *
	 * @return nama sumber gaji, atau {@code null} bila belum terisi
	 */
	@Column(name = "sumber_gaji", columnDefinition = "text") public String getSumberGaji() { return sumberGaji; }
	/**
	 * Menyetel nama/label sumber gaji dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama sumber gaji baru
	 */
	public void setSumberGaji(String v) { this.sumberGaji = v; }
	/**
	 * Mengembalikan Nomor Induk Dosen Nasional (NIDN) dosen/SDM.
	 *
	 * @return NIDN, atau {@code null} bila belum terisi
	 */
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	/**
	 * Menyetel Nomor Induk Dosen Nasional (NIDN) dosen/SDM. Tanpa validasi format.
	 *
	 * @param v NIDN baru
	 */
	public void setNidn(String v) { this.nidn = v; }
	/**
	 * Mengembalikan Nomor Unik Pendidik dan Tenaga Kependidikan (NUPTK) dosen/SDM.
	 *
	 * @return NUPTK, atau {@code null} bila belum terisi
	 */
	@Column(name = "nuptk", columnDefinition = "text") public String getNuptk() { return nuptk; }
	/**
	 * Menyetel Nomor Unik Pendidik dan Tenaga Kependidikan (NUPTK) dosen/SDM. Tanpa validasi format.
	 *
	 * @param v NUPTK baru
	 */
	public void setNuptk(String v) { this.nuptk = v; }
	/**
	 * Representasi teks ringkas: {@code "id-kode"}.
	 *
	 * @return gabungan id dan kode
	 */
	@Override public String toString() { return id + "-" + kode; }
}
