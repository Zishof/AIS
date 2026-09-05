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
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>data_pribadi/profil</b>. Kolom bernama = field SISTER;
 * {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <h2>Mekanisme sinkronisasi (terverifikasi aktif, BUKAN entity yatim)</h2>
 * <p>Diisi oleh {@code ais.common.DataSisterApi#synDataDosen(java.util.List)} lewat method privat
 * {@code sinkronEntitasObjek(...)}: untuk tiap {@code id_sdm} dosen yang dipilih admin, endpoint REST SISTER
 * {@code data_pribadi/profil/{id_sdm}} ditarik sebagai SATU objek JSON, dipetakan lewat refleksi setter
 * ({@code pemetaRefleksi}) ke kelas ini berdasarkan pendaftaran {@code "data_pribadi/profil"} &rarr;
 * {@code DpProfilSister.class} pada {@link ais.database.model.sister.SisterEntitasRegistry}, lalu di-upsert
 * (dicari baris existing lewat {@code Restrictions.eq("idSdm", idSdm)}, disimpan bila belum ada) dengan
 * {@code kode} yang DIPAKSA sama dengan {@code idSdm} sehingga satu dosen hanya pernah punya satu baris di
 * tabel ini. Tombol pemicu: layar "Data SISTER" ({@code ais.action.master.DataSisterAction}) dan
 * {@code ais.action.master.sister.SisterAksiHelper}, keduanya memanggil {@code synDataDosen(pilihan)} untuk
 * daftar {@code id_sdm} yang dicentang admin di dasbor sinkronisasi. Salinan JSON mentah paralel tetap
 * ditulis ke tabel generik {@code DataSister} (kode {@code "data_pribadi/profil?id_sdm=..."}) sebagai
 * cadangan lama, terlepas dari entitas terstruktur ini.</p>
 *
 * <h2>Bukan header — enam kelas {@code Dp*Sister} lain adalah SAUDARA, bukan detail</h2>
 * <p>Kelas ini tidak memiliki relasi JPA ({@code @OneToMany}/{@code @ManyToOne}) ke {@code DpAlamatSister},
 * {@code DpKependudukanSister}, {@code DpKeluargaSister}, {@code DpKepegawaianSister},
 * {@code DpBidangIlmuSister}, atau {@code DpLainSister}. Ketujuhnya adalah entitas MANDIRI, masing-masing
 * disinkronkan dari endpoint {@code data_pribadi/*} miliknya sendiri lewat mekanisme yang identik di atas,
 * dan hanya "bertaut" secara implisit lewat kesamaan nilai {@link #getIdSdm()} — bukan foreign key database.
 * Menggabungkan data satu dosen dari ketujuh tabel berarti melakukan query terpisah per tabel dengan
 * {@code idSdm} yang sama; tidak ada satu pun query/join siap pakai yang ditemukan untuk itu.</p>
 *
 * <h2>Tidak terhubung ke {@code Pegawai}/{@code Dosen} internal lewat foreign key</h2>
 * <p>{@link #getIdSdm()} adalah id SDM SISTER (nilai {@code kode} pada {@code RefSdmSister}, hasil tarikan
 * endpoint referensi SDM Kemdikbudristek), BUKAN primary key {@code Pegawai}/{@code Dosen} AIS — kedua kelas
 * model tersebut tidak memiliki kolom {@code idSdm} sama sekali. Satu-satunya jembatan yang ditemukan
 * bersifat tekstual dan hanya untuk tampilan: layar pemilihan dosen di {@code DataSisterAction} mencocokkan
 * NIDN dari JSON {@code RefSdmSister.keterangan} dengan {@code Dosen.getNidn()} semata agar bisa menampilkan
 * kolom prodi/fakultas pada daftar centang sebelum sinkronisasi — pencocokan ini tidak menyentuh baris
 * {@code Dp*Sister} dan tidak ada di jalur simpan entitas ini.</p>
 *
 * <h2>Peringatan privasi</h2>
 * <p>Baris pada tabel ini menyimpan data pribadi dosen (nama, tempat/tanggal lahir, jenis kelamin) tanpa
 * kolom scoping tenant/satuan-kerja apa pun — satu-satunya kunci pembatas adalah {@code idSdm}. Per 5 Sep
 * 2026 tidak ditemukan Action/JSP yang membaca balik tabel ini untuk ditampilkan; risiko saat ini murni pada
 * sisi tulis: siapa pun yang berwenang membuka layar "Data SISTER" dan menekan "Sinkronkan Terpilih" dapat
 * menarik data pribadi dosen mana pun di PT ke tabel ini. Data yang jauh lebih sensitif (NIK, alamat,
 * kontak, status kawin, NPWP) berada di enam kelas saudaranya — lihat javadoc masing-masing. Ini memperkuat
 * pola yang sudah tercatat pada inisiatif ini (filter tenant lemah/hilang pada entitas SISTER), bukan temuan
 * berdiri sendiri yang baru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_dp_profil")
public class DpProfilSister extends GeneralValueObject {
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
	/** JSON mentah respons endpoint SISTER untuk satu dosen ({@code data_pribadi/profil/{id_sdm}} apa adanya), disimpan berdampingan dengan kolom terstruktur sebagai cadangan. */
	private String keterangan;
	/** Penanda baris aktif; lihat {@link #getAktif()} untuk perilaku default {@code true}. */
	private Boolean aktif;
	/** Id SDM SISTER milik pemilik baris ini; lihat penjelasan lengkap ketiadaan foreign key pada javadoc kelas. */
	private String idSdm;
	/** Nama lengkap dosen/SDM sebagaimana tercatat di SISTER. */
	private String nama;
	/** Jenis kelamin dosen/SDM sebagaimana tercatat di SISTER (kode/label mentah dari respons, tidak dinormalkan). */
	private String jenisKelamin;
	/** Nama tempat lahir dosen/SDM sebagaimana tercatat di SISTER. */
	private String tempatLahir;
	/** Tanggal lahir dosen/SDM sebagaimana tercatat di SISTER; disimpan sebagai teks mentah (bukan {@link Date}), format mengikuti apa pun yang dikirim API. */
	private String tanggalLahir;

	/** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity dari hasil query. */
	public DpProfilSister() {}
	/**
	 * Mengembalikan primary key baris.
	 *
	 * @return primary key, atau {@code null} bila baris belum tersimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel primary key baris. Tanpa validasi; kolom dipetakan {@code insertable = false} sehingga nilai
	 * ini murni hasil generate database, bukan yang ditulis lewat INSERT eksplisit.
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
	 * string kosong/spasi diabaikan diam-diam sehingga jejak audit yang sudah terisi tidak bisa terhapus
	 * oleh pemanggil yang kebetulan tidak membawa informasi pengguna (mis. proses sinkronisasi latar
	 * belakang). Field ini mendeklarasikan ulang &amp; menimpa {@code oleh}/{@code olehId} milik
	 * {@link GeneralValueObject} agar terpetakan sebagai kolom pada tabel entitas ini — pola "audit shadow
	 * field" yang berulang di seluruh entity Sister, merupakan keharusan teknis pemetaan Hibernate, bukan
	 * bug.
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
	 * — pola default-aktif yang berulang di seluruh entity Sister/DataSister. Tidak ditemukan jalur yang
	 * secara eksplisit menyetel {@code false} pada baris hasil sinkronisasi.
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
	 * Mengembalikan nama lengkap dosen/SDM sebagaimana tercatat di SISTER.
	 *
	 * @return nama dosen/SDM, atau {@code null} bila belum terisi
	 */
	@Column(name = "nama", columnDefinition = "text") public String getNama() { return nama; }
	/**
	 * Menyetel nama lengkap dosen/SDM. Tanpa validasi.
	 *
	 * @param v nama baru
	 */
	public void setNama(String v) { this.nama = v; }
	/**
	 * Mengembalikan jenis kelamin dosen/SDM sebagaimana tercatat di SISTER (kode/label mentah, tidak dinormalkan).
	 *
	 * @return jenis kelamin, atau {@code null} bila belum terisi
	 */
	@Column(name = "jenis_kelamin", columnDefinition = "text") public String getJenisKelamin() { return jenisKelamin; }
	/**
	 * Menyetel jenis kelamin dosen/SDM. Tanpa validasi.
	 *
	 * @param v jenis kelamin baru
	 */
	public void setJenisKelamin(String v) { this.jenisKelamin = v; }
	/**
	 * Mengembalikan nama tempat lahir dosen/SDM sebagaimana tercatat di SISTER.
	 *
	 * @return tempat lahir, atau {@code null} bila belum terisi
	 */
	@Column(name = "tempat_lahir", columnDefinition = "text") public String getTempatLahir() { return tempatLahir; }
	/**
	 * Menyetel tempat lahir dosen/SDM. Tanpa validasi.
	 *
	 * @param v tempat lahir baru
	 */
	public void setTempatLahir(String v) { this.tempatLahir = v; }
	/**
	 * Mengembalikan tanggal lahir dosen/SDM sebagaimana tercatat di SISTER, sebagai teks mentah (bukan
	 * {@link Date}) — format mengikuti apa pun yang dikirim API, tidak diparse/divalidasi di sini.
	 *
	 * @return tanggal lahir mentah, atau {@code null} bila belum terisi
	 */
	@Column(name = "tanggal_lahir", columnDefinition = "text") public String getTanggalLahir() { return tanggalLahir; }
	/**
	 * Menyetel tanggal lahir dosen/SDM. Tanpa validasi/parsing.
	 *
	 * @param v tanggal lahir mentah baru
	 */
	public void setTanggalLahir(String v) { this.tanggalLahir = v; }
	/**
	 * Representasi teks ringkas: {@code "id-kode"}.
	 *
	 * @return gabungan id dan kode
	 */
	@Override public String toString() { return id + "-" + kode; }
}
