package ais.database.model.employ;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk tabel master <b>gaji pokok</b> — satu baris merepresentasikan besaran gaji
 * pokok yang berlaku untuk kombinasi {@link Golongan} tertentu pada rentang <i>masa kerja</i>
 * (dalam tahun) tertentu, efektif mulai {@link #getTanggalEfektif()}. Tipe ini membawa state yang
 * dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh
 * field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p><b>Cara pemakaian &mdash; dicari, bukan hanya dibaca langsung.</b> Baris {@code GajiPokok}
 * tidak selalu diambil satu-per-satu berdasarkan {@code id}; jalur pemakaian utamanya adalah
 * pencarian berdasarkan golongan + tahun masa kerja pegawai melalui
 * {@code ais.action.master.employ.helper.MasaKerjaUtil#cariGajiPokok(Pegawai, Golongan, Date)}
 * dan {@code ais.database.model.Pegawai#ambilGajiPokok(Date)}. Algoritma pencarian tersebut
 * mengiterasi seluruh baris {@link Golongan} yang sama lalu memilih dengan prioritas: (1) baris
 * yang {@link #getMasaKerja()}-nya <i>persis sama</i> dengan tahun masa kerja pegawai; jika tidak
 * ada, (2) baris dengan {@code masaKerja} terbesar yang masih {@code <=} tahun masa kerja pegawai
 * ("floor"); jika tidak ada juga, (3) baris dengan {@code masaKerja} terendah sebagai cadangan.
 * Baris yang {@link #getTanggalEfektif()}-nya belum berlaku pada tanggal acuan dilewati. Bila dua
 * baris berebut prioritas yang sama, yang {@code tanggalEfektif}-nya lebih baru dimenangkan.
 * Hasil pencarian ini yang ditampilkan sebagai gaji pokok pegawai pada slip gaji
 * ({@code GajiPegawaiAction}, laporan slip gaji).</p>
 *
 * <p><b>Tiga jalur penentuan gaji pokok pegawai.</b> Tabel ini bukan satu-satunya sumber: gaji
 * pokok pegawai bisa (a) ditentukan langsung di SK kenaikan pangkat (mengabaikan tabel ini sama
 * sekali), (b) dihitung otomatis dari golongan + masa kerja terkini bila cekbox "Penggajian
 * Otomatis Berdasarkan Masa Kerja" aktif pada SK, atau (c) baru dicari di tabel master ini. Lihat
 * {@code Pegawai#ambilGajiPokok(Date)} untuk urutan lengkapnya.</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String keterangan}, {@code Golongan golongan}, {@code
 * Peraturan peraturan}, {@code Integer masaKerja}, {@code Double gaji}, {@code Double lain},
 * {@code Date tanggalEfektif}, {@code Date tanggal_dirubah}; pemetaan persistence: tabel
 * {@code employ.gaji_pokok}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getKeterangan()}, {@code getGolongan()},
 * {@code getPeraturan()}, {@code getMasaKerja()}, {@code getGaji()}, {@code getTanggalEfektif()},
 * {@code getLain()}); mutasi data ({@code setOlehId()}, {@code onUpdate()}, {@code setId()},
 * {@code setOleh()}, {@code setTanggal_dirubah()}, {@code setKeterangan()}, {@code setGolongan()},
 * {@code setPeraturan()}, {@code setMasaKerja()}, {@code setGaji()}, {@code setTanggalEfektif()},
 * {@code setLain()}); operasi domain lain ({@code toString()}, {@code compareTo(GeneralValueObject)}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Sisa kode mati.</b> Empat field {@code kode}/{@code kodeInsentif}/{@code kodeMakan}/
 * {@code kodeTransport} tertinggal sebagai komentar di dekat konstanta {@link #serialVersionUID}
 * (tidak dikompilasi, sekadar catatan historis dari desain awal yang tidak jadi dipakai).</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. {@link #getGolongan()} dan {@link #getPeraturan()} memanggil
 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy Hibernate yang mungkin
 * sudah <i>detached</i> (lihat dokumentasi {@link GeneralValueObject} bagian mekanisme
 * {@code check()}); pemanggilan ini transparan tapi bisa memicu pembacaan cache/identity-map.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy lainnya tetap menjadi tanggung
 * jawab DAO/service dengan session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see Golongan
 * @see Insentif
 * @see Makan
 * @see Transport
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "gaji_pokok")
public class GajiPokok extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris gaji
	 * pokok ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang diwarisi pola
	 * generiknya dari {@link GeneralValueObject}.
	 *
	 * @return id pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan secara
	 * diam-diam (tidak melempar exception, tidak mengubah state) sehingga pemanggil tidak bisa
	 * mengosongkan {@code olehId} yang sudah terisi lewat setter ini.
	 *
	 * @param olehId id pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pencatat. Nilai kosong atau hanya berisi whitespace diabaikan
	 * secara diam-diam, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pencatat; {@code null} atau string kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris gaji
	 * pokok ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, dan mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memutakhirkan
	 * {@link #tanggal_dirubah}. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi teks baris gaji pokok ini: nominal {@link #getGaji()} yang diformat memakai
	 * {@code Common.numberFormat}, diikuti nama golongan (hasil {@link Golongan#toString()}) bila
	 * {@link #getGolongan()} tidak {@code null}. Dipakai di combobox/label pemilihan gaji pokok
	 * pada UI master data.
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} generik yang mengembalikan string kosong
	 * bila terjadi exception apa pun (mis. proxy lazy {@link #golongan} gagal diresolusi) — silent
	 * fallback, bukan dilempar ke pemanggil.</p>
	 *
	 * @return teks tampilan gaji pokok, atau string kosong bila gagal memformat
	 */
	public String toString() {
		try {
			golongan = getGolongan();
			return Common.numberFormat.get().format(gaji) + "" + (golongan == null ? "" : " - " + golongan.toString());
		} catch (Exception e) {
			return "";
		}
	}

//	private String kode;
//	private String kodeInsentif;
//	private String kodeMakan;
//	private String kodeTransport;

	private String keterangan;
	private Golongan golongan;
	private Peraturan peraturan;
	private Integer masaKerja;
	private Double gaji;
	private Double lain;
	private Date tanggalEfektif;

	/**
	 * Membandingkan urutan tampil dua baris {@code GajiPokok} untuk keperluan pengurutan daftar
	 * (mis. combobox/grid master data). Mengikuti pola fallback berjenjang generik entity AIS:
	 * kriteria pertama yang tersedia pada <b>kedua</b> operand dipakai, sisanya diabaikan.
	 *
	 * <p>Urutan kriteria: (1) {@link #getMasaKerja()} — kriteria paling spesifik untuk tabel gaji
	 * pokok, dibandingkan sebagai {@link Integer}; (2) {@code getNomorUrut()} warisan
	 * {@link GeneralValueObject}; (3) {@code getNim()}; (4) {@code getNama()}; (5)
	 * {@link #getKeterangan()}. Bila tidak satu pun kriteria terpenuhi pada kedua sisi, method
	 * mengembalikan {@code 0} (dianggap setara) — <b>bukan</b> jaminan bahwa kedua object benar-
	 * benar identik, hanya berarti tidak ada dasar pembanding yang bisa dipakai.</p>
	 *
	 * <p><b>Efek samping tersembunyi:</b> baris pertama badan method men-cast {@code arg0} ke
	 * {@code GajiPokok} untuk memanggil {@code getMasaKerja()}-nya; bila {@code arg0} adalah
	 * instance {@link GeneralValueObject} lain (bukan {@code GajiPokok}), {@code ClassCastException}
	 * akan tertangkap oleh blok {@code catch} generik di bawah dan direkam lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)} — pola <i>shadow audit field</i> yang dipakai
	 * berulang di seluruh model AIS untuk mencatat exception yang sengaja ditelan tanpa mengganggu
	 * alur pemanggil (KEHARUSAN TEKNIS, bukan bug: pengurutan tidak boleh gagal total hanya karena
	 * satu baris tidak bisa dibandingkan). Karena exception ditelan, hasil perbandingan pada kasus
	 * ini jatuh ke {@code return 0} di akhir method, bukan melempar ke pemanggil.</p>
	 *
	 * @param arg0 object pembanding; diasumsikan instance {@code GajiPokok} pada kriteria pertama
	 * @return negatif/nol/positif mengikuti kontrak {@link Comparable}, atau {@code 0} bila tidak
	 *         ada kriteria yang bisa dibandingkan atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getMasaKerja() != null && ((GajiPokok) arg0).getMasaKerja() != null) {
				return getMasaKerja().compareTo(((GajiPokok) arg0).getMasaKerja());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/GajiPokok.java:111");

		}

		return 0;
	}

	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. Tidak menginisialisasi field lain di luar default Java.
	 */
	public GajiPokok() {
	}

	/**
	 * Mengembalikan primary key baris gaji pokok ini.
	 *
	 * @return id baris, atau {@code null} bila belum persisten (belum di-{@code INSERT})
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom dipetakan {@code insertable = false} (nilai dihasilkan
	 * database via {@code IDENTITY}), sehingga setter ini praktis hanya dipakai saat memuat ulang
	 * entity dari hasil query, bukan sebelum insert.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan bebas untuk baris gaji pokok ini.
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

	/**
	 * Mengembalikan golongan (pangkat/golongan kepegawaian) yang menjadi kunci pencarian utama
	 * baris gaji pokok ini. Sebelum dikembalikan, proxy lazy {@link #golongan} diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} (identity map &rarr; cache &rarr; session aktif
	 * &rarr; fallback query baru) agar aman diakses meski entity ini sudah lepas dari
	 * {@code Session} Hibernate yang memuatnya.
	 *
	 * @return golongan terkait, atau {@code null} bila tidak diset
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "golongan", nullable = true)
	public Golongan getGolongan() {
		golongan = check(golongan);
		return golongan;
	}

	/**
	 * Menetapkan golongan.
	 *
	 * @param golongan golongan baru
	 */
	public void setGolongan(Golongan golongan) {
		this.golongan = golongan;
	}

	/**
	 * Mengembalikan peraturan (dasar hukum) yang berlaku untuk baris gaji pokok ini. Bukan getter
	 * murni: bila {@link #getGolongan()} tersedia, nilai {@link #peraturan} <b>ditimpa</b> dengan
	 * {@code getGolongan().getPeraturan()} — peraturan pada golongan selalu menang atas peraturan
	 * yang pernah diset langsung di baris ini. Field {@link #peraturan} hanya dipakai (lewat
	 * {@link GeneralValueObject#check(Object)}) bila golongan tidak tersedia. Pola "getter dengan
	 * fallback yang menimpa field sendiri" ini konsisten dengan {@link Insentif#getPeraturan()},
	 * {@link Makan#getPeraturan()}, dan {@link Transport#getPeraturan()}.
	 *
	 * @return peraturan efektif (dari golongan bila ada, atau dari field lokal)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peraturan", nullable = true)
	public Peraturan getPeraturan() {
		if (getGolongan() != null) {
			peraturan = getGolongan().getPeraturan();
		} else {
			peraturan = check(peraturan);
		}
		return peraturan;
	}

	/**
	 * Menetapkan peraturan secara langsung. Nilai ini efektif hanya bila {@link #getGolongan()}
	 * kembali {@code null} pada saat {@link #getPeraturan()} dipanggil (lihat catatan di sana).
	 *
	 * @param peraturan peraturan baru
	 */
	public void setPeraturan(Peraturan peraturan) {
		this.peraturan = peraturan;
	}

	/**
	 * Mengembalikan tingkat masa kerja (dalam tahun) yang menjadi kunci pencarian baris ini di
	 * dalam golongan yang sama. {@code null} dinormalisasi menjadi {@code 0} — <b>berbeda</b> dari
	 * {@link Insentif#getMasaKerja()}, {@link Makan#getMasaKerja()}, dan
	 * {@link Transport#getMasaKerja()} yang mengembalikan {@code null} apa adanya tanpa
	 * normalisasi. Inkonsistensi kecil ini tidak berdampak pada algoritma pencarian
	 * ({@code MasaKerjaUtil#cariGajiPokok}) karena baris dengan {@code masaKerja} {@code null}
	 * sudah dilewati sebelum getter ini dipanggil, tapi berarti kode lain yang membaca
	 * {@code getMasaKerja()} langsung dari baris "kosong" (belum pernah diset) akan melihat
	 * {@code 0} di sini dan {@code null} di tiga kelas saudaranya.
	 *
	 * @return masa kerja dalam tahun, {@code 0} bila belum diset
	 */
	public Integer getMasaKerja() {
		return masaKerja == null ? 0 : masaKerja;
	}

	/**
	 * Menetapkan tingkat masa kerja (tahun).
	 *
	 * @param masaKerja masa kerja dalam tahun
	 */
	public void setMasaKerja(Integer masaKerja) {
		this.masaKerja = masaKerja;
	}

	/**
	 * Mengembalikan nominal gaji pokok baris ini. {@code null} dinormalisasi menjadi {@code 0.0}.
	 *
	 * @return nominal gaji pokok, {@code 0.0} bila belum diset
	 */
	public Double getGaji() {
		return gaji == null ? 0.0 : gaji;
	}

	/**
	 * Menetapkan nominal gaji pokok.
	 *
	 * @param gaji nominal gaji pokok
	 */
	public void setGaji(Double gaji) {
		this.gaji = gaji;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya baris gaji pokok ini, dipakai sebagai penentu apakah
	 * baris sudah "efektif" pada tanggal acuan pencarian ({@code MasaKerjaUtil#cariGajiPokok}).
	 * {@code null} dinormalisasi menjadi tanggal saat ini via {@code WaktuUtil.getDate()} (bukan
	 * {@code new Date()} langsung seperti pada {@link Insentif#getTanggalEfektif()},
	 * {@link Makan#getTanggalEfektif()}, dan {@link Transport#getTanggalEfektif()} — perbedaan
	 * kecil yang secara fungsional setara selama {@code WaktuUtil.getDate()} tidak dikustomisasi
	 * untuk zona waktu/testing khusus).
	 *
	 * @return tanggal efektif, atau tanggal saat ini bila belum diset
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalEfektif() {
		return tanggalEfektif == null ? WaktuUtil.getDate() : tanggalEfektif;
	}

	/**
	 * Menetapkan tanggal mulai berlaku.
	 *
	 * @param tanggalEfektif tanggal efektif baru
	 */
	public void setTanggalEfektif(Date tanggalEfektif) {
		this.tanggalEfektif = tanggalEfektif;
	}

	/**
	 * Mengembalikan komponen "gaji lain-lain" tambahan pada baris ini (di luar gaji pokok utama).
	 * {@code null} dinormalisasi menjadi {@code 0.0}. Field ini tidak dimiliki
	 * {@link Insentif}/{@link Makan}/{@link Transport} — spesifik untuk {@code GajiPokok}.
	 *
	 * @return nominal gaji lain-lain, {@code 0.0} bila belum diset
	 */
	public Double getLain() {
		return lain == null ? 0.0 : lain;
	}

	/**
	 * Menetapkan komponen gaji lain-lain.
	 *
	 * @param lain nominal gaji lain-lain
	 */
	public void setLain(Double lain) {
		this.lain = lain;
	}

}
