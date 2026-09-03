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

/**
 * Model data untuk tabel master <b>tunjangan makan</b> — satu baris merepresentasikan besaran
 * tunjangan makan yang berlaku untuk kombinasi {@link Golongan} tertentu pada rentang <i>masa
 * kerja</i> (dalam tahun) tertentu, efektif mulai {@link #getTanggalEfektif()}. Tipe ini membawa
 * state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya
 * ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap
 * dimiliki {@link GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar
 * spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di
 * kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 *
 * <p><b>Cara pemakaian &mdash; dicari, bukan hanya dibaca langsung.</b> Baris ini dicari lewat
 * {@code ais.database.model.Pegawai#ambilMakan(Date)} dengan aturan cocok-persis (masa kerja sama
 * persis dengan tahun masa kerja pegawai) lalu ambil-yang-lebih-besar, sejajar dengan
 * {@code Pegawai#ambilGajiPokok(Date)}, tapi <b>tidak</b> mengenal jalur "nilai ditentukan
 * langsung di SK" maupun kenaikan berkala — tabel master ini satu-satunya sumber nilai tunjangan
 * makan. Hasil pencarian ditampilkan pada slip gaji ({@code GajiPegawaiAction}) dan laporan uang
 * makan ({@code LaporanUangMakanPegawai}).</p>
 *
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String
 * oleh}, {@code String olehId}, {@code String keterangan}, {@code Golongan golongan}, {@code
 * Peraturan peraturan}, {@code Integer masaKerja}, {@code Double makan}, {@code Date
 * tanggalEfektif}, {@code Date tanggal_dirubah}; pemetaan persistence: tabel {@code employ.makan};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code
 * getTanggal_dirubah()}, {@code getKeterangan()}, {@code getGolongan()}, {@code getPeraturan()},
 * {@code getMasaKerja()}, {@code getMakan()}, {@code getTanggalEfektif()}); mutasi data ({@code
 * setOlehId()}, {@code onUpdate()}, {@code setId()}, {@code setOleh()}, {@code
 * setTanggal_dirubah()}, {@code setKeterangan()}, {@code setGolongan()}, {@code setPeraturan()},
 * {@code setMasaKerja()}, {@code setMakan()}, {@code setTanggalEfektif()}); operasi domain lain
 * ({@code toString()}, {@code compareTo(GeneralValueObject)}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 *
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value
 * object di memori. {@link #getGolongan()} dan {@link #getPeraturan()} memanggil
 * {@link GeneralValueObject#check(Object)} untuk meresolusi proxy lazy Hibernate (lihat
 * dokumentasi {@link GeneralValueObject} bagian mekanisme {@code check()}). Persistence, transaksi,
 * otorisasi, dan pemuatan relasi lazy lainnya tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see GajiPokok
 * @see Insentif
 * @see Transport
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "makan")
public class Makan extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Mengembalikan id pengguna (kolom {@code oleh_id}) yang terakhir membuat/mengubah baris
	 * tunjangan makan ini. Bagian dari pasangan field audit {@code oleh}/{@code olehId} yang
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
	 * Mengembalikan nama pengguna (kolom {@code oleh}) yang terakhir membuat/mengubah baris
	 * tunjangan makan ini.
	 *
	 * @return nama pengguna pencatat, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan ke
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
	 * Representasi teks baris tunjangan makan ini: nominal {@link #getMakan()} yang diformat
	 * memakai {@code Common.numberFormat}, diikuti nama golongan (hasil {@link
	 * Golongan#toString()}) bila {@link #getGolongan()} tidak {@code null}. Dipakai di
	 * combobox/label pemilihan tunjangan makan pada UI master data.
	 *
	 * <p>Sama seperti {@link Insentif#toString()} dan {@link Transport#toString()}, badan method
	 * ini <i>tidak</i> dibungkus {@code try/catch} — berbeda dari {@link GajiPokok#toString()}
	 * yang meredam exception menjadi string kosong.</p>
	 *
	 * @return teks tampilan tunjangan makan
	 */
	public String toString() {
		golongan = getGolongan();
		return Common.numberFormat.get().format(makan) + "" + (golongan == null ? "" : " - " + golongan.toString());
	}

	private String keterangan;
	private Golongan golongan;
	private Peraturan peraturan;
	private Integer masaKerja;
	private Double makan;
	private Date tanggalEfektif;

	/**
	 * Membandingkan urutan tampil dua baris {@code Makan} untuk keperluan pengurutan daftar (mis.
	 * combobox/grid master data). Mengikuti pola fallback berjenjang generik entity AIS: kriteria
	 * pertama yang tersedia pada <b>kedua</b> operand dipakai, sisanya diabaikan.
	 *
	 * <p>Urutan kriteria: (1) {@link #getMasaKerja()}, dibandingkan sebagai {@link Integer}; (2)
	 * {@code getNomorUrut()} warisan {@link GeneralValueObject}; (3) {@code getNim()}; (4)
	 * {@code getNama()}; (5) {@link #getKeterangan()}. Bila tidak satu pun kriteria terpenuhi pada
	 * kedua sisi, method mengembalikan {@code 0} — bukan jaminan kesamaan, hanya berarti tidak ada
	 * dasar pembanding.</p>
	 *
	 * <p><b>Efek samping tersembunyi:</b> baris pertama badan method men-cast {@code arg0} ke
	 * {@code Makan}; bila {@code arg0} bukan instance {@code Makan}, {@code ClassCastException}
	 * tertangkap oleh blok {@code catch} generik dan direkam lewat
	 * {@code ais.common.ErrorAuditUtil.record(...)} — pola <i>shadow audit field</i> yang berulang
	 * di seluruh model AIS untuk mencatat exception yang sengaja ditelan (KEHARUSAN TEKNIS, bukan
	 * bug). Hasilnya jatuh ke {@code return 0}.</p>
	 *
	 * @param arg0 object pembanding; diasumsikan instance {@code Makan} pada kriteria pertama
	 * @return negatif/nol/positif mengikuti kontrak {@link Comparable}, atau {@code 0} bila tidak
	 *         ada kriteria yang bisa dibandingkan atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getMasaKerja() != null && ((Makan) arg0).getMasaKerja() != null) {
				return getMasaKerja().compareTo(((Makan) arg0).getMasaKerja());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/Makan.java:100");

		}

		return 0;
	}

	/**
	 * Konstruktor tanpa argumen yang dipersyaratkan Hibernate/JPA untuk instansiasi entity lewat
	 * reflection. Tidak menginisialisasi field lain di luar default Java.
	 */
	public Makan() {
	}

	/**
	 * Mengembalikan primary key baris tunjangan makan ini.
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
	 * Mengembalikan keterangan bebas untuk baris tunjangan makan ini.
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
	 * Mengembalikan golongan yang menjadi kunci pencarian utama baris tunjangan makan ini. Proxy
	 * lazy {@link #golongan} diresolusi lewat {@link GeneralValueObject#check(Object)} sebelum
	 * dikembalikan agar aman diakses meski entity ini sudah lepas dari {@code Session} Hibernate
	 * yang memuatnya.
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
	 * Mengembalikan peraturan (dasar hukum) yang berlaku untuk baris tunjangan makan ini. Bukan
	 * getter murni: bila {@link #getGolongan()} tersedia, nilai {@link #peraturan} <b>ditimpa</b>
	 * dengan {@code getGolongan().getPeraturan()}. Field {@link #peraturan} hanya dipakai (lewat
	 * {@link GeneralValueObject#check(Object)}) bila golongan tidak tersedia. Pola ini konsisten
	 * dengan {@link GajiPokok#getPeraturan()}, {@link Insentif#getPeraturan()}, dan
	 * {@link Transport#getPeraturan()}.
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
	 * kembali {@code null} pada saat {@link #getPeraturan()} dipanggil.
	 *
	 * @param peraturan peraturan baru
	 */
	public void setPeraturan(Peraturan peraturan) {
		this.peraturan = peraturan;
	}

	/**
	 * Mengembalikan tingkat masa kerja (dalam tahun) yang menjadi kunci pencarian baris ini di
	 * dalam golongan yang sama. Sama seperti {@link Insentif#getMasaKerja()}, {@code null}
	 * dikembalikan apa adanya (tidak dinormalisasi ke {@code 0}, berbeda dari
	 * {@link GajiPokok#getMasaKerja()}).
	 *
	 * @return masa kerja dalam tahun, atau {@code null} bila belum diset
	 */
	public Integer getMasaKerja() {
		return masaKerja;
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
	 * Mengembalikan nominal tunjangan makan baris ini. {@code null} dinormalisasi menjadi
	 * {@code 0.0}.
	 *
	 * @return nominal tunjangan makan, {@code 0.0} bila belum diset
	 */
	public Double getMakan() {
		return makan == null ? 0.0 : makan;
	}

	/**
	 * Menetapkan nominal tunjangan makan.
	 *
	 * @param makan nominal tunjangan makan
	 */
	public void setMakan(Double makan) {
		this.makan = makan;
	}

	/**
	 * Mengembalikan tanggal mulai berlakunya baris tunjangan makan ini. {@code null}
	 * dinormalisasi memakai {@code new Date()} langsung — sama seperti
	 * {@link Insentif#getTanggalEfektif()} dan {@link Transport#getTanggalEfektif()}, berbeda dari
	 * {@link GajiPokok#getTanggalEfektif()} yang memakai {@code WaktuUtil.getDate()}.
	 *
	 * @return tanggal efektif, atau tanggal saat ini bila belum diset
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalEfektif() {
		return tanggalEfektif == null ? new Date() : tanggalEfektif;
	}

	/**
	 * Menetapkan tanggal mulai berlaku.
	 *
	 * @param tanggalEfektif tanggal efektif baru
	 */
	public void setTanggalEfektif(Date tanggalEfektif) {
		this.tanggalEfektif = tanggalEfektif;
	}

}
