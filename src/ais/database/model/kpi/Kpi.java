package ais.database.model.kpi;

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
import org.json.JSONArray;

import ais.common.Common;
import ais.database.model.GeneralValueObject;

/**
 * Entitas JPA/Hibernate untuk tabel {@code public.kpi} — definisi <b>master</b> satu indikator
 * KPI (Key Performance Indicator) yang dapat dipakai ulang di banyak konteks penilaian.
 *
 * <p><b>Peran dalam model KPI (diverifikasi dari field &amp; relasi, BUKAN diasumsikan dari nama
 * kelas/paket yang kebetulan identik):</b> {@code Kpi} TIDAK terkait langsung dengan pegawai
 * atau periode penilaian tertentu — kelas ini murni data referensi/master: kode, nama, formula
 * default, satuan pengukuran ({@link #satuanKpi}), kategori ({@link #kategoriKpi}), nilai
 * default, dan atribut styling tampilan (font besar/tebal, warna teks/latar). Instansiasi KPI
 * ini ke dalam konteks konkret seorang pegawai pada suatu periode dilakukan oleh
 * {@link ItemKpi} — satu baris {@code item_kpi} MERUJUK satu baris {@code Kpi} lewat kolom
 * {@code kpi} (lihat {@link ItemKpi#getKpi()}), sementara nilai realisasi, target, dan riwayat
 * per-pegawai disimpan pada {@link ItemKpi} dan {@link FormatKpiDetail}, bukan pada {@code Kpi}
 * itu sendiri. Singkatnya:</p>
 *
 * <pre>
 * Kpi (master indikator: kode, nama, formula default, satuan, kategori, styling)
 *   &lt;-- dirujuk oleh -- ItemKpi (instansiasi konkret per pegawai/format, menyimpan nilai aktual)
 * </pre>
 *
 * <p><b>Pola arsitektur berulang yang perlu diwaspadai:</b></p>
 * <ul>
 *   <li><b>Getter destruktif normalisasi kode:</b> {@link #getKode()} tidak sekadar membaca
 *   field — setiap pemanggilan menghapus spasi dan mengganti tanda hubung menjadi garis bawah
 *   dari kode master, TANPA menuliskan hasilnya kembali ke field {@code kode} (berbeda dari pola
 *   getter destruktif di {@link ItemKpi}/{@link FormatKpi} yang menugaskan ulang ke field). Efek
 *   normalisasi di sini murni pada nilai kembalian, bukan pada apa yang tersimpan.</li>
 *   <li><b>Field relasi yang di-"check()" (shadow re-resolve):</b> {@link #getSatuanKpi()} dan
 *   {@link #getKategoriKpi()} — lihat {@link ais.database.model.GeneralValueObject#check(Object)};
 *   KEHARUSAN TEKNIS, bukan bug.</li>
 *   <li><b>Flag {@code aktif} satu-arah:</b> {@link #getAktif()} men-default {@code null} ke
 *   {@code true} tanpa menuliskannya kembali ke field — konsisten dengan {@link ItemKpi},
 *   {@link FormatKpi}, {@link FormatKpiDetail}. Field boolean styling lain
 *   ({@link #fontBesar}, {@link #fontBold}, {@link #tanpaWarnaBackgrond}) mengikuti pola sama.</li>
 *   <li><b>Field bayangan audit:</b> {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   dideklarasikan ulang lokal — KEHARUSAN TEKNIS untuk Hibernate Envers.</li>
 *   <li><b>{@link #DEFAULT_FORMULA} bersifat {@code public static} non-{@code final}:</b> field
 *   ini dibagikan (shared) ke SELURUH instance kelas ini sebagai default representasi JSON array
 *   kosong; karena bukan {@code final}, secara teknis kode lain di luar kelas ini dapat
 *   menggantinya (mengubah default untuk semua instance sekaligus), meski dalam praktiknya tidak
 *   ada pemanggil yang melakukan itu saat ini.</li>
 * </ul>
 *
 * @see ItemKpi
 * @see FormatKpi
 * @see FormatKpiDetail
 * @see SatuanKpi
 * @see KategoriKpi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kpi")

public class Kpi extends GeneralValueObject {

	/**
	 * Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. Nilai identik dengan
	 * entitas-entitas lain dalam paket {@code kpi} — peninggalan hasil generate hbm2java.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer (identity, auto-increment) baris {@code kpi}. */
	private Long id;

	/**
	 * Nama/username pengguna yang melakukan perubahan terakhir pada baris ini. Field bayangan
	 * audit, diisi oleh interceptor Hibernate — lihat catatan kelas.
	 */
	private String oleh;

	/**
	 * Id/identifier pengguna yang melakukan perubahan terakhir pada baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh mekanisme audit yang sama.
	 */
	private String olehId;

	/**
	 * Membandingkan urutan tampil dua {@link GeneralValueObject} dengan mencoba berurutan:
	 * kode, nomor urut, NIM, nama, lalu keterangan — memakai kriteria pertama yang tersedia
	 * (tidak null) pada KEDUA sisi perbandingan. Kegagalan pada blok percobaan ditelan secara
	 * sengaja dan dicatat ke {@link ais.common.ErrorAuditUtil} agar pengurutan tidak melempar
	 * exception ke pemanggil; hasil fallback adalah 0 (dianggap setara/tidak terurutkan).
	 *
	 * @param arg0 objek pembanding
	 * @return hasil {@code compareTo} kriteria pertama yang cocok, atau 0 bila tidak ada kriteria
	 *         yang bisa dibandingkan atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getKode() != null && arg0.getKode() != null) {
				return getKode().compareTo(arg0.getKode());
			} else if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kpi/Kpi.java:63");

		}

		return 0;
	}

	/**
	 * Mengembalikan id/identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Menolak (no-op) bila argumen {@code null} atau
	 * kosong/spasi saja, sehingga nilai lama tetap dipertahankan.
	 *
	 * @param olehId id pengguna baru; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama/username pengguna yang melakukan perubahan terakhir. Menolak (no-op) bila
	 * argumen {@code null} atau kosong/spasi saja.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/username pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
	 * sebelum operasi UPDATE dieksekusi, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}
	/** Stempel waktu perubahan terakhir; diinisialisasi ke waktu saat ini pada saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir secara eksplisit. Biasanya dipanggil oleh
	 * mekanisme audit ({@link #onUpdate()}), bukan oleh kode aplikasi.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir pada baris ini.
	 *
	 * @return tanggal/waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string untuk debugging/log: {@code id-nama}.
	 *
	 * @return string ringkas identitas KPI master ini
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode identifikasi unik KPI master ini; dinormalisasi (spasi dihapus, tanda hubung jadi garis bawah) saat dibaca lewat {@link #getKode()}. */
	private String kode;
	/** Formula perhitungan default (JSON array) untuk KPI ini, dipakai sebagai acuan bila {@link ItemKpi} tidak mendefinisikan formula sendiri. */
	private String formula;
	/** Nama KPI master ini. */
	private String nama;
	/** Keterangan/catatan bebas untuk KPI master ini. */
	private String keterangan;
	/** Satuan pengukuran (mis. persen, rupiah, jumlah) untuk KPI ini. */
	private SatuanKpi satuanKpi;
	/** Kategori/klasifikasi KPI ini. */
	private KategoriKpi kategoriKpi;
	/** Penanda aktif/tidak; default {@code true} bila belum diisi — lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nilai default (string numerik) yang dipakai sebagai fallback saat realisasi belum diisi — lihat {@link #getValDefault()}. */
	private String valDefault;
	/** Penanda apakah KPI ini merupakan nilai final/akhir (bukan komponen antara); default {@code false}. */
	private Boolean nilaifinal = false;

	/** Penanda tampilan: apakah nama/label KPI ini ditampilkan dengan ukuran font besar; default {@code false}. */
	private Boolean fontBesar = false;
	/** Penanda tampilan: apakah nama/label KPI ini ditampilkan dengan font tebal; default {@code false}. */
	private Boolean fontBold = false;
	/** Kode warna teks (format {@code #RRGGBB}) untuk tampilan KPI ini; default {@code #000000} bila belum diisi. */
	private String color;
	/** Penanda tampilan: apakah warna latar belakang DIABAIKAN (tidak dipakai); default {@code true} — lihat {@link #getTanpaWarnaBackgrond()}. */
	private Boolean tanpaWarnaBackgrond = true;
	/** Kode warna latar belakang (format {@code #RRGGBB}) untuk tampilan KPI ini; default {@code #FFFFFF} bila belum diisi. */
	private String background;

	/** Konstruktor tanpa argumen, dipakai Hibernate untuk membentuk instance via reflection. */
	public Kpi() {
	}

	/**
	 * Mengembalikan kunci primer baris {@code kpi}. Kolom identity ({@code insertable = false})
	 * — nilainya dibuat oleh basis data saat INSERT.
	 *
	 * @return id baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi id secara manual. Jarang dipakai aplikasi karena kolom bersifat
	 * {@code insertable = false}.
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode identifikasi unik KPI master ini, TERNORMALISASI: spasi dihapus dan
	 * tanda hubung ({@code -}) diganti garis bawah ({@code _}) — mis. {@code "kpi-01 a"} menjadi
	 * {@code "kpi_01a"}. Kolom ini di-{@code unique = true} pada basis data, sehingga normalisasi
	 * ini penting agar variasi penulisan (spasi/tanda hubung) tidak menghasilkan kode yang secara
	 * visual berbeda namun idealnya mewakili entitas KPI yang sama.
	 *
	 * <p>Berbeda dari pola getter destruktif di {@link ItemKpi}/{@link FormatKpi}, normalisasi
	 * di sini TIDAK ditugaskan kembali ke field {@code kode} — hanya nilai kembalian yang
	 * ternormalisasi, field mentah tetap seperti yang di-set lewat {@link #setKode(String)}.</p>
	 *
	 * @return kode ternormalisasi, atau {@code null} bila field kosong/belum diisi
	 */
	@Column(name = "kode", nullable = true, unique = true)
	public String getKode() {
		return kode == null || kode.trim().isEmpty() ? null
				: org.apache.commons.lang3.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(kode, " ", ""), "-", "_");
	}

	/**
	 * Mengisi kode identifikasi unik KPI master ini (nilai mentah, sebelum normalisasi).
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama KPI master ini, sudah di-{@code trim()}.
	 *
	 * @return nama KPI tanpa spasi di awal/akhir, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama KPI master ini.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/catatan bebas untuk KPI master ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/catatan bebas untuk KPI master ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif KPI master ini.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; nilai field bila sudah eksplisit di-set
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif KPI master ini.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Representasi JSON array kosong ({@code "[]"}), dipakai sebagai nilai default
	 * {@link #getFormula()} saat field {@code formula} belum diisi.
	 *
	 * <p><b>Perhatian:</b> field ini {@code public static} namun BUKAN {@code final} — secara
	 * teknis dapat ditugaskan ulang oleh kode mana pun di luar kelas ini, yang akan mengubah
	 * nilai default formula untuk SELURUH instance {@code Kpi} sekaligus (bukan cuma satu
	 * instance). Tidak ada pemanggil yang melakukan ini di kode saat ini — dicatat sebagai
	 * kerapuhan desain, bukan sebagai kerentanan aktif.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONArray().toString();

	/**
	 * Mengembalikan formula perhitungan default (JSON array) untuk KPI ini.
	 *
	 * @return formula JSON, atau {@link #DEFAULT_FORMULA} (representasi array kosong) bila
	 *         field belum diisi/kosong
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {
		return formula == null || formula.isEmpty() ? DEFAULT_FORMULA : formula;
	}

	/**
	 * Mengisi formula perhitungan default (JSON array) untuk KPI ini.
	 *
	 * @param formula formula JSON baru
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan satuan pengukuran KPI ini. Field di-refresh lewat {@code check()} sebelum
	 * dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas. Nilai ini juga yang
	 * dirujuk oleh {@link ItemKpi#getVal()}/{@link ItemKpi#getValtampil()} untuk menentukan
	 * apakah satuan memiliki {@link ais.database.model.ParameterTambahan} (parameter tambahan)
	 * yang memengaruhi logika fallback nilai.
	 *
	 * @return satuan pengukuran KPI ini, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kpi", nullable = true)
	public SatuanKpi getSatuanKpi() {
		satuanKpi = check(satuanKpi);
		return satuanKpi;
	}

	/**
	 * Mengisi satuan pengukuran KPI ini.
	 *
	 * @param satuanKpi satuan pengukuran baru
	 */
	public void setSatuanKpi(SatuanKpi satuanKpi) {
		this.satuanKpi = satuanKpi;
	}

	/**
	 * Mengembalikan kategori/klasifikasi KPI ini. Field di-refresh lewat {@code check()} sebelum
	 * dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return kategori KPI ini, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kategori_kpi", nullable = true)
	public KategoriKpi getKategoriKpi() {
		kategoriKpi = check(kategoriKpi);
		return kategoriKpi;
	}

	/**
	 * Mengisi kategori/klasifikasi KPI ini.
	 *
	 * @param kategoriKpi kategori baru
	 */
	public void setKategoriKpi(KategoriKpi kategoriKpi) {
		this.kategoriKpi = kategoriKpi;
	}

	/**
	 * Mengembalikan penanda tampilan ukuran font besar untuk nama/label KPI ini.
	 *
	 * @return {@code true} bila diaktifkan; {@code false} bila belum diisi/dinonaktifkan
	 */
	public Boolean getFontBesar() {
		return fontBesar == null ? false : fontBesar;
	}

	/**
	 * Mengisi penanda tampilan ukuran font besar untuk nama/label KPI ini.
	 *
	 * @param fontBesar status baru
	 */
	public void setFontBesar(Boolean fontBesar) {
		this.fontBesar = fontBesar;
	}

	/**
	 * Mengembalikan penanda tampilan font tebal untuk nama/label KPI ini.
	 *
	 * @return {@code true} bila diaktifkan; {@code false} bila belum diisi/dinonaktifkan
	 */
	public Boolean getFontBold() {
		return fontBold == null ? false : fontBold;
	}

	/**
	 * Mengisi penanda tampilan font tebal untuk nama/label KPI ini.
	 *
	 * @param fontBold status baru
	 */
	public void setFontBold(Boolean fontBold) {
		this.fontBold = fontBold;
	}

	/**
	 * Mengembalikan kode warna teks untuk tampilan KPI ini.
	 *
	 * @return kode warna format {@code #RRGGBB}, default {@code "#000000"} (hitam) bila belum diisi
	 */
	public String getColor() {
		return color == null ? "#000000" : color;
	}

	/**
	 * Mengisi kode warna teks untuk tampilan KPI ini.
	 *
	 * @param color kode warna baru, format {@code #RRGGBB}
	 */
	public void setColor(String color) {
		this.color = color;
	}

	/**
	 * Mengembalikan kode warna latar belakang untuk tampilan KPI ini.
	 *
	 * @return kode warna format {@code #RRGGBB}, default {@code "#FFFFFF"} (putih) bila belum diisi
	 */
	public String getBackground() {
		return background == null ? "#FFFFFF" : background;
	}

	/**
	 * Mengisi kode warna latar belakang untuk tampilan KPI ini.
	 *
	 * @param background kode warna baru, format {@code #RRGGBB}
	 */
	public void setBackground(String background) {
		this.background = background;
	}

	/**
	 * Mengembalikan penanda apakah warna latar belakang ({@link #getBackground()}) DIABAIKAN
	 * (tidak dipakai) saat menampilkan KPI ini — mis. dipakai UI untuk memutuskan apakah
	 * menerapkan {@link #getBackground()} sebagai warna latar sel/baris atau membiarkan
	 * transparan/default.
	 *
	 * @return {@code true} (warna latar diabaikan) bila belum diisi; nilai field bila sudah eksplisit di-set
	 */
	public Boolean getTanpaWarnaBackgrond() {
		return tanpaWarnaBackgrond == null ? true : tanpaWarnaBackgrond;
	}

	/**
	 * Mengisi penanda apakah warna latar belakang diabaikan saat menampilkan KPI ini.
	 *
	 * @param tanpaWarnaBackgrond status baru
	 */
	public void setTanpaWarnaBackgrond(Boolean tanpaWarnaBackgrond) {
		this.tanpaWarnaBackgrond = tanpaWarnaBackgrond;
	}

	/**
	 * Mengembalikan penanda apakah KPI ini merupakan nilai final/akhir (bukan komponen antara
	 * dalam suatu perhitungan komposit).
	 *
	 * @return {@code true} bila ditandai final; {@code false} bila belum diisi/bukan final
	 */
	public Boolean getNilaifinal() {
		return nilaifinal == null ? false : nilaifinal;
	}

	/**
	 * Mengisi penanda apakah KPI ini merupakan nilai final/akhir.
	 *
	 * @param nilaifinal status baru
	 */
	public void setNilaifinal(Boolean nilaifinal) {
		this.nilaifinal = nilaifinal;
	}

	/**
	 * Mengembalikan nilai default (string numerik) yang dipakai sebagai fallback oleh
	 * {@link ItemKpi#getVal()} dan {@link ItemKpi#getValtampil()} saat realisasi belum diisi
	 * atau satuan KPI tidak mendefinisikan {@link ais.database.model.ParameterTambahan}.
	 * Divalidasi dengan {@link ais.common.Common#isNumber(String)} — hasil dijamin selalu berupa
	 * string angka valid.
	 *
	 * @return nilai default sebagai string numerik, fallback {@code "0"} bila kosong/tidak valid
	 */
	public String getValDefault() {
		return valDefault == null || valDefault.isEmpty() || !Common.isNumber(valDefault) ? "0" : valDefault;
	}

	/**
	 * Mengisi nilai default (string numerik) untuk KPI master ini.
	 *
	 * @param valDefault nilai default baru
	 */
	public void setValDefault(String valDefault) {
		this.valDefault = valDefault;
	}

}
