package ais.database.model.employ;

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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.payroll.AsuransiPegawai;

/**
 * Model data untuk keluarga. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String ISTRI}, {@code String SUAMI},
 * {@code String ANAK}, {@code String MERTUA}, {@code String ORANG_TUA}, {@code String SAUDARA}, {@code Long id},
 * {@code String oleh}; pemetaan persistence: tabel {@code employ.keluarga}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()},
 * {@code getAsuransiPegawai1()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code
 * setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}); operasi domain lain ({@code toString()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "employ", name = "keluarga")
public class Keluarga extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus istri dari pegawai. */
	public static final String ISTRI = "ISTRI";
	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus suami dari pegawai. */
	public static final String SUAMI = "SUAMI";
	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus anak dari pegawai. */
	public static final String ANAK = "ANAK";
	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus mertua dari pegawai. */
	public static final String MERTUA = "MERTUA";
	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus orang tua dari pegawai. */
	public static final String ORANG_TUA = "ORANG_TUA";
	/** Nilai {@link #getHubungan()} untuk anggota keluarga berstatus saudara dari pegawai. */
	public static final String SAUDARA = "SAUDARA";
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris
	 * data keluarga ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang
	 * diwarisi pola generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state).
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	private String keterangan;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengembalikan primary key baris data keluarga ini.
	 *
	 * @return id baris, atau {@code null} bila belum persisten
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom dipetakan {@code insertable = false} (nilai dihasilkan
	 * database via {@code IDENTITY}), jadi setter ini praktis hanya dipakai saat memuat ulang
	 * entity dari hasil query.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris data
	 * keluarga ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan tanggal terakhir baris ini dirubah. Biasanya diisi otomatis oleh
	 * {@link #onUpdate()}, bukan dipanggil manual.
	 *
	 * @param tanggal_dirubah tanggal perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan tanggal terakhir baris ini dirubah. Nilai awalnya (sebelum pernah di-update)
	 * diinisialisasi ke waktu saat object dibuat, lewat {@code WaktuUtil.getDate()}.
	 *
	 * @return tanggal perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris data keluarga ini: mengembalikan {@link #getKeterangan()} apa
	 * adanya (bisa {@code null} bila belum diisi).
	 *
	 * @return keterangan baris ini
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris data keluarga ini.
	 *
	 * @return teks keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas.
	 *
	 * @param keterangan teks keterangan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	private Pegawai pegawai;
	private String hubungan;
	private String nama;
	private String tempatLahir;
	private Date tanggalLahir;
	private Date tanggalNikah;
	private String jenisKelamin;
	private String alamat;
	private String pekerjaan;
	private String keteranganTambahan;
	private Boolean status = false;
	private Boolean menikah = false;
	private Pendidikan pendidikan;
	private String jurusanPendidikan;

	private AsuransiPegawai asuransiPegawai1;
	private String nomorAsuransiPegawai1;
	private Double premiAsuransi1;

	/**
	 * Mengembalikan produk asuransi ({@link AsuransiPegawai}) yang dipilih untuk anggota keluarga
	 * ini — inilah slot pencocokan yang dimaksud dokumentasi {@code AsuransiPegawai} bagian
	 * "{@code asuransiPegawai1}" pada {@code Keluarga}: berbeda dari {@code Pegawai} yang punya
	 * <b>empat</b> slot ({@code asuransiPegawai1}…{@code asuransiPegawai4}) untuk asuransi pegawai
	 * itu sendiri, setiap baris {@code Keluarga} (satu per anggota keluarga) hanya punya
	 * <b>satu</b> slot asuransi ({@code asuransiPegawai1}) — cukup karena satu baris sudah
	 * mewakili satu anggota keluarga tertentu. Proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return produk asuransi yang dipilih untuk anggota keluarga ini, atau {@code null} bila
	 *         belum diikutsertakan asuransi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai1")
	public AsuransiPegawai getAsuransiPegawai1() {
		asuransiPegawai1 = check(asuransiPegawai1);
		return asuransiPegawai1;
	}

	/**
	 * Menetapkan produk asuransi untuk anggota keluarga ini.
	 *
	 * @param asuransiPegawai1 produk asuransi baru
	 */
	public void setAsuransiPegawai1(AsuransiPegawai asuransiPegawai1) {
		this.asuransiPegawai1 = asuransiPegawai1;
	}

	/**
	 * Mengembalikan status pernikahan anggota keluarga ini (relevan terutama untuk baris dengan
	 * {@link #getHubungan()} {@link #ANAK}, untuk menandai anak yang sudah menikah — biasanya
	 * berhenti menjadi tanggungan tunjangan keluarga).
	 *
	 * @return {@code true} bila menikah, boleh {@code null} bila belum diisi (berbeda dari
	 *         {@link #getStatus()} yang menormalisasi {@code null} menjadi {@code false})
	 */
	@Column(name = "menikah")
	public Boolean getMenikah() {
		return menikah;
	}

	/**
	 * Menetapkan status pernikahan.
	 *
	 * @param menikah status pernikahan baru
	 */
	public void setMenikah(Boolean menikah) {
		this.menikah = menikah;
	}

	/**
	 * Mengembalikan tingkat pendidikan terakhir anggota keluarga ini. Proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} sebelum dikembalikan.
	 *
	 * @return tingkat pendidikan, atau {@code null} bila tidak diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan")
	public Pendidikan getPendidikan() {
		pendidikan = check(pendidikan);
		return pendidikan;
	}

	/**
	 * Menetapkan tingkat pendidikan.
	 *
	 * @param pendidikan tingkat pendidikan baru
	 */
	public void setPendidikan(Pendidikan pendidikan) {
		this.pendidikan = pendidikan;
	}

	/**
	 * Mengembalikan jurusan pendidikan (teks bebas, mis. "Teknik Informatika") yang melengkapi
	 * {@link #getPendidikan()}.
	 *
	 * @return jurusan pendidikan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "jurusan_pendidikan")
	public String getJurusanPendidikan() {
		return jurusanPendidikan;
	}

	/**
	 * Menetapkan jurusan pendidikan.
	 *
	 * @param jurusanPendidikan jurusan pendidikan baru
	 */
	public void setJurusanPendidikan(String jurusanPendidikan) {
		this.jurusanPendidikan = jurusanPendidikan;
	}

	/**
	 * Mengembalikan pegawai pemilik data keluarga ini. <b>Getter dengan efek samping dan
	 * fallback berjenjang:</b> (1) proxy lazy {@link #pegawai} diresolusi lewat
	 * {@link GeneralValueObject#check(Object)}; (2) bila hasilnya masih {@code null}, method
	 * mencoba mengisi dari {@code Common.getCurrentUser().getPegawai()} — pegawai milik user yang
	 * sedang login — sehingga entity baru yang belum diset eksplisit otomatis "memiliki" diri
	 * user yang sedang mengisi form. Kegagalan pada langkah (2) (mis. tidak ada user login, atau
	 * {@code getCurrentUser()} melempar exception) ditelan oleh {@code catch} generik dan direkam
	 * lewat {@code ais.common.ErrorAuditUtil.record(...)} — pola <i>shadow audit field</i> yang
	 * berulang di seluruh model AIS (KEHARUSAN TEKNIS, bukan bug); pada kasus itu method
	 * mengembalikan {@code null} apa adanya.
	 *
	 * @return pegawai pemilik data keluarga ini, kemungkinan diisi otomatis dari user login, atau
	 *         {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/Keluarga.java:174");

		}

		return pegawai;
	}

	/**
	 * Menetapkan pegawai pemilik data keluarga secara eksplisit.
	 *
	 * @param pegawai pegawai baru
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan jenis hubungan keluarga anggota ini terhadap pegawai — salah satu konstanta
	 * {@link #ISTRI}, {@link #SUAMI}, {@link #ANAK}, {@link #MERTUA}, {@link #ORANG_TUA}, {@link
	 * #SAUDARA} (kolom {@code String} bebas, bukan enum yang divalidasi database).
	 *
	 * @return jenis hubungan keluarga, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "hubungan", nullable = false)
	public String getHubungan() {
		return hubungan;
	}

	/**
	 * Menetapkan jenis hubungan keluarga.
	 *
	 * @param hubungan salah satu konstanta {@link #ISTRI}/{@link #SUAMI}/{@link #ANAK}/{@link
	 *                 #MERTUA}/{@link #ORANG_TUA}/{@link #SAUDARA}
	 */
	public void setHubungan(String hubungan) {
		this.hubungan = hubungan;
	}

	/**
	 * Mengembalikan nama anggota keluarga ini.
	 *
	 * @return nama, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	/**
	 * Menetapkan nama anggota keluarga.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan tempat lahir anggota keluarga ini.
	 *
	 * @return tempat lahir, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "tempat_lahir", nullable = false)
	public String getTempatLahir() {
		return tempatLahir;
	}

	/**
	 * Menetapkan tempat lahir.
	 *
	 * @param tempatLahir tempat lahir baru
	 */
	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	/**
	 * Mengembalikan tanggal lahir anggota keluarga ini.
	 *
	 * @return tanggal lahir, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "tanggal_lahir", nullable = false)
	public Date getTanggalLahir() {
		return tanggalLahir;
	}

	/**
	 * Menetapkan tanggal lahir.
	 *
	 * @param tanggalLahir tanggal lahir baru
	 */
	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	/**
	 * Mengembalikan jenis kelamin anggota keluarga ini.
	 *
	 * @return jenis kelamin, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "jenis_kelamin", nullable = false)
	public String getJenisKelamin() {
		return jenisKelamin;
	}

	/**
	 * Menetapkan jenis kelamin.
	 *
	 * @param jenisKelamin jenis kelamin baru
	 */
	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	/**
	 * Mengembalikan alamat anggota keluarga ini. <b>Getter dengan fallback:</b> bila
	 * {@link #alamat} kosong/{@code null}, method mengambil alamat dari {@link #getPegawai()}
	 * (alamat pegawai yang bersangkutan) sebagai pengganti, atau string kosong bila pegawai juga
	 * tidak tersedia — <b>tidak pernah mengembalikan {@code null}</b> meski kolom database
	 * dipetakan {@code nullable = false} tanpa nilai.
	 *
	 * @return alamat anggota keluarga, alamat pegawai sebagai fallback, atau string kosong
	 */
	@Column(name = "alamat", nullable = false)
	public String getAlamat() {
		return alamat == null || alamat.trim().isEmpty() ? (getPegawai() == null ? "" : getPegawai().getAlamat())
				: alamat;
	}

	/**
	 * Menetapkan alamat anggota keluarga secara eksplisit.
	 *
	 * @param alamat alamat baru
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan pekerjaan anggota keluarga ini.
	 *
	 * @return pekerjaan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "pekerjaan", nullable = true)
	public String getPekerjaan() {
		return pekerjaan;
	}

	/**
	 * Menetapkan pekerjaan.
	 *
	 * @param pekerjaan pekerjaan baru
	 */
	public void setPekerjaan(String pekerjaan) {
		this.pekerjaan = pekerjaan;
	}

	/**
	 * Mengembalikan keterangan tambahan bebas untuk anggota keluarga ini (di luar
	 * {@link #getKeterangan()} warisan {@link GeneralValueObject}).
	 *
	 * @return keterangan tambahan, wajib diisi (kolom {@code nullable = false})
	 */
	@Column(name = "keterangan_tambahan", nullable = false)
	public String getKeteranganTambahan() {
		return keteranganTambahan;
	}

	/**
	 * Menetapkan keterangan tambahan.
	 *
	 * @param keteranganTambahan keterangan tambahan baru
	 */
	public void setKeteranganTambahan(String keteranganTambahan) {
		this.keteranganTambahan = keteranganTambahan;
	}

	/**
	 * Mengembalikan status baris data keluarga ini (makna spesifik ditentukan pemanggil, mis.
	 * penanda tanggungan aktif untuk tunjangan keluarga). {@code null} dinormalisasi <b>dan
	 * disimpan ulang ke field</b> menjadi {@code false} — berbeda dari kebanyakan getter
	 * normalisasi lain di klaster ini yang hanya menormalisasi nilai kembalian tanpa mengubah
	 * field itu sendiri.
	 *
	 * @return status baris ini, {@code false} bila belum diset
	 */
	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}
		return status;
	}

	/**
	 * Menetapkan status baris data keluarga.
	 *
	 * @param status status baru
	 */
	public void setStatus(Boolean status) {
		this.status = status;
	}

	/**
	 * Mengembalikan tanggal pernikahan anggota keluarga ini (relevan untuk baris {@link #ISTRI}/
	 * {@link #SUAMI}, atau {@link #ANAK} yang sudah menikah — lihat {@link #getMenikah()}).
	 *
	 * @return tanggal nikah, atau {@code null} bila tidak diisi/tidak relevan
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalNikah() {
		return tanggalNikah;
	}

	/**
	 * Menetapkan tanggal pernikahan.
	 *
	 * @param tanggalNikah tanggal nikah baru
	 */
	public void setTanggalNikah(Date tanggalNikah) {
		this.tanggalNikah = tanggalNikah;
	}

	/**
	 * Mengembalikan nomor polis/kartu asuransi anggota keluarga ini pada produk
	 * {@link #getAsuransiPegawai1()}.
	 *
	 * @return nomor asuransi, atau {@code null} bila tidak diisi
	 */
	public String getNomorAsuransiPegawai1() {
		return nomorAsuransiPegawai1;
	}

	/**
	 * Menetapkan nomor polis/kartu asuransi.
	 *
	 * @param nomorAsuransiPegawai1 nomor asuransi baru
	 */
	public void setNomorAsuransiPegawai1(String nomorAsuransiPegawai1) {
		this.nomorAsuransiPegawai1 = nomorAsuransiPegawai1;
	}

	/**
	 * Mengembalikan nominal premi asuransi yang dibayar untuk anggota keluarga ini. <b>Getter
	 * dengan efek samping:</b> bila {@link #getAsuransiPegawai1()} tersedia dan
	 * {@link #premiAsuransi1} belum diisi atau bernilai {@code 0} (dicek lewat
	 * {@code intValue() == 0}, sehingga nilai pecahan kecil seperti {@code 0.5} <b>tidak</b>
	 * dianggap kosong), method <b>menetapkan sekaligus mengembalikan</b>
	 * {@code getAsuransiPegawai1().getTarif()} — field {@link #premiAsuransi1} diisi permanen di
	 * memori pada pemanggilan pertama, mengikuti pola getter-dengan-efek-samping yang sama seperti
	 * {@link JamKerjaPegawai#getMulai()}/{@link JamKerjaPegawai#getSampai()}. Hasil akhir
	 * dinormalisasi {@code null} menjadi {@code 0.0}.
	 *
	 * @return premi asuransi efektif (manual, atau tarif produk sebagai default), {@code 0.0} bila
	 *         tidak ada produk asuransi dan belum diisi manual
	 */
	public Double getPremiAsuransi1() {
		if (getAsuransiPegawai1() != null && (premiAsuransi1 == null || premiAsuransi1.intValue() == 0)) {
			premiAsuransi1 = getAsuransiPegawai1().getTarif();
		}
		return premiAsuransi1 == null ? 0.0 : premiAsuransi1;
	}

	/**
	 * Menetapkan nominal premi asuransi secara manual, menimpa default dari
	 * {@link #getAsuransiPegawai1()} pada pemanggilan {@link #getPremiAsuransi1()} berikutnya
	 * (selama nilainya bukan {@code null}/{@code 0}).
	 *
	 * @param premiAsuransi1 nominal premi baru
	 */
	public void setPremiAsuransi1(Double premiAsuransi1) {
		this.premiAsuransi1 = premiAsuransi1;
	}

}
