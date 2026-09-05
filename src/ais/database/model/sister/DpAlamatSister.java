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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/alamat</b>. Kolom bernama = field SISTER;
 * {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <h2>Mekanisme sinkronisasi &amp; relasi antar entitas</h2>
 * <p>Mekanisme sinkronisasi, ketiadaan relasi header/detail antar tujuh kelas {@code Dp*Sister}, serta
 * ketiadaan foreign key ke {@code Pegawai}/{@code Dosen} internal SAMA PERSIS dengan yang dijelaskan lengkap
 * pada javadoc {@link DpProfilSister} — lihat kelas itu untuk rincian (diisi oleh
 * {@code ais.common.DataSisterApi#synDataDosen(java.util.List)}, dipetakan lewat
 * {@link ais.database.model.sister.SisterEntitasRegistry} berdasarkan kunci {@code "data_pribadi/alamat"},
 * {@code kode} dipaksa sama dengan {@link #getIdSdm()}). Kelas ini adalah SAUDARA {@code DpProfilSister},
 * bukan detailnya.</p>
 *
 * <h2>Peringatan privasi — kelas paling sensitif di klaster ini</h2>
 * <p>Dari ketujuh kelas {@code Dp*Sister}, tabel ini menyimpan kontak pribadi paling langsung dapat
 * dieksploitasi: {@link #getEmail()}, {@link #getTeleponRumah()}, {@link #getTeleponHp()}, serta alamat
 * lengkap ({@link #getAlamat()}, RT/RW, dusun, kelurahan, kota/kabupaten, kode pos). Sama seperti
 * {@code DpProfilSister}, tidak ada kolom scoping tenant/satuan-kerja dan (per 5 Sep 2026) tidak ditemukan
 * Action/JSP yang membaca balik tabel ini untuk ditampilkan — risiko murni pada sisi tulis lewat layar "Data
 * SISTER". Memperkuat pola tercatat (filter tenant lemah/hilang pada entitas SISTER), bukan temuan baru yang
 * berdiri sendiri.</p>
 *
 * @see DpProfilSister
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_alamat")
public class DpAlamatSister extends GeneralValueObject {
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
	/** JSON mentah respons endpoint SISTER untuk satu dosen ({@code data_pribadi/alamat/{id_sdm}} apa adanya), disimpan berdampingan dengan kolom terstruktur sebagai cadangan. */
	private String keterangan;
	/** Penanda baris aktif; lihat {@link #getAktif()} untuk perilaku default {@code true}. */
	private Boolean aktif;
	/** Id SDM SISTER milik pemilik baris ini; lihat penjelasan lengkap ketiadaan foreign key pada javadoc {@link DpProfilSister}. */
	private String idSdm;
	/** Alamat surel (email) dosen/SDM sebagaimana tercatat di SISTER. */
	private String email;
	/** Alamat jalan/rumah dosen/SDM sebagaimana tercatat di SISTER (teks bebas, tidak terpisah per komponen selain field lain di bawah). */
	private String alamat;
	/** Nomor RT (rukun tetangga) tempat tinggal dosen/SDM. */
	private Integer rt;
	/** Nomor RW (rukun warga) tempat tinggal dosen/SDM. */
	private Integer rw;
	/** Nama dusun tempat tinggal dosen/SDM, bila ada. */
	private String dusun;
	/** Nama kelurahan/desa tempat tinggal dosen/SDM. */
	private String kelurahan;
	/** Nama kota/kabupaten tempat tinggal dosen/SDM sebagaimana tercatat di SISTER. */
	private String kotaKabupaten;
	/** Kode referensi kota/kabupaten SISTER; padanan id ke tabel referensi wilayah, disimpan sebagai teks mentah tanpa FK. */
	private String idKotaKabupaten;
	/** Kode pos tempat tinggal dosen/SDM. */
	private String kodePos;
	/** Nomor telepon rumah dosen/SDM. */
	private String teleponRumah;
	/** Nomor telepon genggam (HP) dosen/SDM. */
	private String teleponHp;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil query. */
	public DpAlamatSister() {}
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
	 * Mengembalikan alamat surel dosen/SDM.
	 *
	 * @return alamat email, atau {@code null} bila belum terisi
	 */
	@Column(name = "email", columnDefinition = "text") public String getEmail() { return email; }
	/**
	 * Menyetel alamat surel dosen/SDM. Tanpa validasi format email.
	 *
	 * @param v alamat email baru
	 */
	public void setEmail(String v) { this.email = v; }
	/**
	 * Mengembalikan alamat jalan/rumah dosen/SDM.
	 *
	 * @return alamat, atau {@code null} bila belum terisi
	 */
	@Column(name = "alamat", columnDefinition = "text") public String getAlamat() { return alamat; }
	/**
	 * Menyetel alamat jalan/rumah dosen/SDM. Tanpa validasi.
	 *
	 * @param v alamat baru
	 */
	public void setAlamat(String v) { this.alamat = v; }
	/**
	 * Mengembalikan nomor RT tempat tinggal dosen/SDM.
	 *
	 * @return nomor RT, atau {@code null} bila belum terisi
	 */
	@Column(name = "rt") public Integer getRt() { return rt; }
	/**
	 * Menyetel nomor RT tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor RT baru
	 */
	public void setRt(Integer v) { this.rt = v; }
	/**
	 * Mengembalikan nomor RW tempat tinggal dosen/SDM.
	 *
	 * @return nomor RW, atau {@code null} bila belum terisi
	 */
	@Column(name = "rw") public Integer getRw() { return rw; }
	/**
	 * Menyetel nomor RW tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor RW baru
	 */
	public void setRw(Integer v) { this.rw = v; }
	/**
	 * Mengembalikan nama dusun tempat tinggal dosen/SDM.
	 *
	 * @return nama dusun, atau {@code null} bila belum terisi/tidak berlaku
	 */
	@Column(name = "dusun", columnDefinition = "text") public String getDusun() { return dusun; }
	/**
	 * Menyetel nama dusun tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama dusun baru
	 */
	public void setDusun(String v) { this.dusun = v; }
	/**
	 * Mengembalikan nama kelurahan/desa tempat tinggal dosen/SDM.
	 *
	 * @return nama kelurahan, atau {@code null} bila belum terisi
	 */
	@Column(name = "kelurahan", columnDefinition = "text") public String getKelurahan() { return kelurahan; }
	/**
	 * Menyetel nama kelurahan/desa tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama kelurahan baru
	 */
	public void setKelurahan(String v) { this.kelurahan = v; }
	/**
	 * Mengembalikan nama kota/kabupaten tempat tinggal dosen/SDM.
	 *
	 * @return nama kota/kabupaten, atau {@code null} bila belum terisi
	 */
	@Column(name = "kota_kabupaten", columnDefinition = "text") public String getKotaKabupaten() { return kotaKabupaten; }
	/**
	 * Menyetel nama kota/kabupaten tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama kota/kabupaten baru
	 */
	public void setKotaKabupaten(String v) { this.kotaKabupaten = v; }
	/**
	 * Mengembalikan kode referensi kota/kabupaten SISTER (teks mentah, tanpa FK ke tabel wilayah mana pun).
	 *
	 * @return kode kota/kabupaten, atau {@code null} bila belum terisi
	 */
	@Column(name = "id_kota_kabupaten", columnDefinition = "text") public String getIdKotaKabupaten() { return idKotaKabupaten; }
	/**
	 * Menyetel kode referensi kota/kabupaten SISTER. Tanpa validasi.
	 *
	 * @param v kode kota/kabupaten baru
	 */
	public void setIdKotaKabupaten(String v) { this.idKotaKabupaten = v; }
	/**
	 * Mengembalikan kode pos tempat tinggal dosen/SDM.
	 *
	 * @return kode pos, atau {@code null} bila belum terisi
	 */
	@Column(name = "kode_pos", columnDefinition = "text") public String getKodePos() { return kodePos; }
	/**
	 * Menyetel kode pos tempat tinggal dosen/SDM. Tanpa validasi.
	 *
	 * @param v kode pos baru
	 */
	public void setKodePos(String v) { this.kodePos = v; }
	/**
	 * Mengembalikan nomor telepon rumah dosen/SDM.
	 *
	 * @return nomor telepon rumah, atau {@code null} bila belum terisi
	 */
	@Column(name = "telepon_rumah", columnDefinition = "text") public String getTeleponRumah() { return teleponRumah; }
	/**
	 * Menyetel nomor telepon rumah dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor telepon rumah baru
	 */
	public void setTeleponRumah(String v) { this.teleponRumah = v; }
	/**
	 * Mengembalikan nomor telepon genggam (HP) dosen/SDM.
	 *
	 * @return nomor HP, atau {@code null} bila belum terisi
	 */
	@Column(name = "telepon_hp", columnDefinition = "text") public String getTeleponHp() { return teleponHp; }
	/**
	 * Menyetel nomor telepon genggam (HP) dosen/SDM. Tanpa validasi.
	 *
	 * @param v nomor HP baru
	 */
	public void setTeleponHp(String v) { this.teleponHp = v; }
	/**
	 * Representasi teks ringkas: {@code "id-kode"}.
	 *
	 * @return gabungan id dan kode
	 */
	@Override public String toString() { return id + "-" + kode; }
}
