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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/kependudukan</b>. Kolom bernama = field
 * SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto
 * hbm2ddl).
 *
 * <h2>Mekanisme sinkronisasi &amp; relasi antar entitas</h2>
 * <p>Mekanisme sinkronisasi, ketiadaan relasi header/detail antar tujuh kelas {@code Dp*Sister}, serta
 * ketiadaan foreign key ke {@code Pegawai}/{@code Dosen} internal SAMA PERSIS dengan yang dijelaskan lengkap
 * pada javadoc {@link DpProfilSister} — lihat kelas itu untuk rincian (diisi oleh
 * {@code ais.common.DataSisterApi#synDataDosen(java.util.List)}, dipetakan lewat
 * {@link ais.database.model.sister.SisterEntitasRegistry} berdasarkan kunci
 * {@code "data_pribadi/kependudukan"}, {@code kode} dipaksa sama dengan {@link #getIdSdm()}). Kelas ini
 * adalah SAUDARA {@code DpProfilSister}, bukan detailnya.</p>
 *
 * <h2>Peringatan privasi — memuat NIK</h2>
 * <p>{@link #getNik()} menyimpan Nomor Induk Kependudukan mentah, salah satu data pribadi paling sensitif
 * menurut UU PDP. Sama seperti {@code DpProfilSister}, tidak ada kolom scoping tenant/satuan-kerja dan (per
 * 5 Sep 2026) tidak ditemukan Action/JSP yang membaca balik tabel ini untuk ditampilkan — risiko murni pada
 * sisi tulis lewat layar "Data SISTER". Memperkuat pola tercatat (filter tenant lemah/hilang pada entitas
 * SISTER), bukan temuan baru yang berdiri sendiri.</p>
 *
 * @see DpProfilSister
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_kependudukan")
public class DpKependudukanSister extends GeneralValueObject {
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
	/** JSON mentah respons endpoint SISTER untuk satu dosen ({@code data_pribadi/kependudukan/{id_sdm}} apa adanya), disimpan berdampingan dengan kolom terstruktur sebagai cadangan. */
	private String keterangan;
	/** Penanda baris aktif; lihat {@link #getAktif()} untuk perilaku default {@code true}. */
	private Boolean aktif;
	/** Id SDM SISTER milik pemilik baris ini; lihat penjelasan lengkap ketiadaan foreign key pada javadoc {@link DpProfilSister}. */
	private String idSdm;
	/** Nomor Induk Kependudukan (NIK) dosen/SDM sebagaimana tercatat di SISTER — data pribadi paling sensitif pada tabel ini. */
	private String nik;
	/** Nama agama dosen/SDM sebagaimana tercatat di SISTER. */
	private String agama;
	/** Kode referensi agama SISTER; padanan id ke tabel referensi {@code referensi/agama}, disimpan sebagai teks/angka mentah tanpa FK. */
	private Integer idAgama;
	/** Nama kewarganegaraan dosen/SDM sebagaimana tercatat di SISTER. */
	private String kewarganegaraan;
	/** Kode negara SISTER untuk kewarganegaraan dosen/SDM, disimpan sebagai teks mentah tanpa FK. */
	private String kodeNegara;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil query. */
	public DpKependudukanSister() {}
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
	 * Mengembalikan Nomor Induk Kependudukan (NIK) dosen/SDM.
	 *
	 * @return NIK, atau {@code null} bila belum terisi
	 */
	@Column(name = "nik", columnDefinition = "text") public String getNik() { return nik; }
	/**
	 * Menyetel Nomor Induk Kependudukan (NIK) dosen/SDM. Tanpa validasi format.
	 *
	 * @param v NIK baru
	 */
	public void setNik(String v) { this.nik = v; }
	/**
	 * Mengembalikan nama agama dosen/SDM.
	 *
	 * @return nama agama, atau {@code null} bila belum terisi
	 */
	@Column(name = "agama", columnDefinition = "text") public String getAgama() { return agama; }
	/**
	 * Menyetel nama agama dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama agama baru
	 */
	public void setAgama(String v) { this.agama = v; }
	/**
	 * Mengembalikan kode referensi agama SISTER (padanan id ke {@code referensi/agama}, tanpa FK).
	 *
	 * @return kode agama, atau {@code null} bila belum terisi
	 */
	@Column(name = "id_agama") public Integer getIdAgama() { return idAgama; }
	/**
	 * Menyetel kode referensi agama SISTER. Tanpa validasi.
	 *
	 * @param v kode agama baru
	 */
	public void setIdAgama(Integer v) { this.idAgama = v; }
	/**
	 * Mengembalikan nama kewarganegaraan dosen/SDM.
	 *
	 * @return nama kewarganegaraan, atau {@code null} bila belum terisi
	 */
	@Column(name = "kewarganegaraan", columnDefinition = "text") public String getKewarganegaraan() { return kewarganegaraan; }
	/**
	 * Menyetel nama kewarganegaraan dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama kewarganegaraan baru
	 */
	public void setKewarganegaraan(String v) { this.kewarganegaraan = v; }
	/**
	 * Mengembalikan kode negara SISTER untuk kewarganegaraan dosen/SDM (teks mentah, tanpa FK).
	 *
	 * @return kode negara, atau {@code null} bila belum terisi
	 */
	@Column(name = "kode_negara", columnDefinition = "text") public String getKodeNegara() { return kodeNegara; }
	/**
	 * Menyetel kode negara SISTER untuk kewarganegaraan dosen/SDM. Tanpa validasi.
	 *
	 * @param v kode negara baru
	 */
	public void setKodeNegara(String v) { this.kodeNegara = v; }
	/**
	 * Representasi teks ringkas: {@code "id-kode"}.
	 *
	 * @return gabungan id dan kode
	 */
	@Override public String toString() { return id + "-" + kode; }
}
