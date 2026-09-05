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
import org.json.JSONObject;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.ui.util.WaktuUtil;

/**
 * Entitas JPA/Hibernate untuk tabel {@code public.item_kpi} — baris item/indikator KPI konkret
 * di dalam satu berkas penilaian kinerja pegawai.
 *
 * <p><b>Peran dalam model KPI (diverifikasi dari field &amp; relasi, bukan diasumsikan dari nama):</b>
 * {@link Kpi} adalah data <i>master</i> — definisi satu indikator KPI yang bersifat generik/dapat
 * dipakai ulang (kode, nama, formula, satuan, kategori, styling tampilan). {@code ItemKpi} adalah
 * <b>instansiasi</b> dari sebuah {@link Kpi} master ke dalam konteks tertentu: satu
 * {@link FormatKpiDetail} (yang mengaitkan {@link FormatKpi} dengan satu {@code Pegawai} dan satu
 * tanggal efektif). Dengan kata lain, alur relasinya adalah:</p>
 *
 * <pre>
 * FormatKpi (template per unit kerja/jurusan/fakultas/yayasan/sekolah)
 *   -&gt; FormatKpiDetail (penugasan template ke satu Pegawai, efektif sejak tanggal tertentu)
 *        -&gt; ItemKpi (baris item KPI konkret milik Pegawai tsb, merujuk definisi Kpi master)
 * </pre>
 *
 * <p>{@code ItemKpi} juga membentuk struktur pohon melalui {@link #getParent()} (kolom
 * {@code bagian_dari}) dan {@link #getDeep()} — memungkinkan satu item KPI menjadi induk dari
 * beberapa sub-item (mis. KPI komposit yang nilainya diturunkan dari beberapa sub-indikator).
 * Nilai aktual/realisasi disimpan di {@link #getVal()} (nilai numerik mentah untuk perhitungan)
 * dan {@link #getValtampil()} (nilai untuk ditampilkan ke pengguna, bisa berbeda formatnya),
 * dengan {@link #getTarget()} sebagai nilai target yang hendak dicapai.</p>
 *
 * <p><b>Pola arsitektur berulang yang perlu diwaspadai saat mengubah kelas ini:</b></p>
 * <ul>
 *   <li><b>Getter destruktif ganda ({@link #getKode()}, {@link #getNama()}):</b> kedua getter ini
 *   TIDAK sekadar membaca field — setiap kali dipanggil, keduanya menimpa field instance
 *   ({@code kode}, {@code nama}) dengan nilai turunan dari {@link Kpi} induk ({@code kpi.getKode()}
 *   + nomor urut, dan {@code kpi.getNama()}). Karena kelas ini memakai
 *   {@code dynamicUpdate = true} dan strategi akses berbasis properti (getter/setter), efek
 *   samping ini BUKAN sekadar caching di memori: nilai hasil overwrite tersebut yang akhirnya
 *   ditulis balik ke kolom {@code kode}/{@code nama} pada operasi simpan berikutnya, sehingga
 *   {@link #setKode(String)}/{@link #setNama(String)} eksplisit menjadi tidak berpengaruh — nilai
 *   apa pun yang di-set akan tertimpa begitu getter dipanggil (mis. dari {@code compareTo},
 *   {@code toString}, atau serialisasi JSON view). Ini konsisten dengan pola getter destruktif
 *   yang sudah tercatat berulang di paket-paket lain — bukan bug baru, tapi WAJIB diperhitungkan
 *   sebelum menambah logika yang bergantung pada nilai field mentah {@code kode}/{@code nama}.</li>
 *   <li><b>Field relasi yang di-"check()" (shadow re-resolve):</b> {@link #getFormatKpi()},
 *   {@link #getParent()}, {@link #getKpi()}, {@link #getFormatKpiDetail()} memanggil
 *   {@code check(field)} lalu menugaskan kembali hasilnya ke field — pola wajib-teknis yang sama
 *   seperti dijelaskan di {@link ais.database.model.GeneralValueObject#check(Object)}, untuk
 *   menghindari {@code LazyInitializationException} pada proxy yang session-nya sudah tertutup.
 *   Ini KEHARUSAN TEKNIS, bukan bug.</li>
 *   <li><b>Flag {@code aktif} satu-arah:</b> {@link #getAktif()} melakukan null-to-true baik pada
 *   field default ({@code = true}) maupun pada pengecekan runtime — dua lapis default yang sama,
 *   konsisten dengan pola di {@link Kpi}, {@link FormatKpi}, {@link FormatKpiDetail}.</li>
 *   <li><b>Field bayangan audit {@code oleh}/{@code olehId}/{@code tanggal_dirubah}:</b> dideklarasikan
 *   ulang secara lokal (bukan mewarisi dari induk) di setiap entitas ber-{@code @Audited} — ini
 *   KEHARUSAN TEKNIS agar Hibernate Envers mencatat kolom-kolom tersebut per tabel, bukan
 *   duplikasi kode yang keliru.</li>
 * </ul>
 *
 * @see Kpi
 * @see FormatKpi
 * @see FormatKpiDetail
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "item_kpi")
public class ItemKpi extends GeneralValueObject {

	/**
	 * Versi serialisasi untuk kompatibilitas {@link java.io.Serializable}. Nilai ini identik
	 * dengan {@code serialVersionUID} pada entitas-entitas lain dalam paket {@code kpi} —
	 * peninggalan hasil generate hbm2java yang menyalin nilai yang sama ke banyak kelas, bukan
	 * indikasi hubungan pewarisan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer (identity, auto-increment) baris {@code item_kpi}. */
	private Long id;

	/**
	 * Nama/username pengguna yang melakukan perubahan terakhir pada baris ini. Field bayangan
	 * audit — diisi oleh interceptor Hibernate ({@code AuditTimestampInterceptor}), bukan oleh
	 * kode aplikasi secara langsung. Lihat catatan kelas mengenai field bayangan audit.
	 */
	private String oleh;

	/**
	 * Id/identifier pengguna yang melakukan perubahan terakhir pada baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh mekanisme audit yang sama.
	 */
	private String olehId;

	/**
	 * Konstruktor kenyamanan untuk langsung mengaitkan item KPI baru dengan
	 * {@link FormatKpiDetail} induknya (penugasan template KPI ke seorang pegawai) tanpa harus
	 * memanggil {@link #setFormatKpiDetail(FormatKpiDetail)} secara terpisah.
	 *
	 * @param formatKpiDetail detail format KPI (pegawai + tanggal efektif) yang menaungi item ini
	 */
	public ItemKpi(FormatKpiDetail formatKpiDetail) {
		this.formatKpiDetail = formatKpiDetail;
	}

	/**
	 * Membandingkan urutan tampil dua {@link GeneralValueObject} dengan mencoba berurutan:
	 * kode, nomor urut, NIM, nama, lalu keterangan — memakai kriteria pertama yang tersedia
	 * (tidak null) pada KEDUA sisi perbandingan.
	 *
	 * <p><b>Efek samping penting:</b> pemanggilan {@link #getKode()} di sini memicu getter
	 * destruktif yang dijelaskan di javadoc kelas — setiap kali objek ini diurutkan (mis. dalam
	 * {@code Collections.sort} atau komponen tabel ZK), field {@code kode} lokal akan ditimpa
	 * ulang dari {@link Kpi} induknya. Kegagalan pada blok percobaan (mis. {@link Kpi} induk
	 * belum ter-set / proxy lazy gagal di-resolve) ditelan secara sengaja dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil} agar pengurutan tidak melempar exception ke pemanggil;
	 * hasil fallback adalah 0 (dianggap setara/tidak terurutkan).</p>
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kpi/ItemKpi.java:69");

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
	 * string kosong/berisi spasi saja — sehingga nilai lama tetap dipertahankan dan baris audit
	 * tidak pernah "kehilangan" siapa penanggung jawab perubahan sebelumnya.
	 *
	 * @param olehId id pengguna baru; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi string untuk debugging/log: {@code id - kode Kpi induk - nama Kpi induk}.
	 * Memaksa resolusi {@link #getKpi()} terlebih dahulu (bukan lewat {@link #getKode()}/
	 * {@link #getNama()} milik {@code ItemKpi} sendiri, melainkan langsung dari {@link Kpi}
	 * induknya) sehingga tidak terpengaruh oleh efek samping getter destruktif kelas ini.
	 *
	 * @return string ringkas identitas item KPI ini
	 */
	public String toString() {
		kpi = getKpi();
		return id + " - " + kpi.getKode() + " - " + kpi.getNama();
	}

	/**
	 * Mengisi nama/username pengguna yang melakukan perubahan terakhir. Menolak (no-op) bila
	 * argumen {@code null} atau kosong/spasi saja, dengan alasan yang sama seperti
	 * {@link #setOlehId(String)}.
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
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Ini bagian dari
	 * mekanisme audit baris yang seragam di seluruh entitas ber-{@code @Audited} pada modul ini.
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
	 * Kode item KPI ini, biasanya berbentuk {@code kode-Kpi-induk.nomor-urut}. Field ini
	 * ditimpa setiap kali {@link #getKode()} dipanggil selama {@link Kpi} induk tersedia — lihat
	 * catatan "getter destruktif" pada javadoc kelas.
	 */
	private String kode;
	/** Nama item KPI ini, disalin ulang dari nama {@link Kpi} induk setiap kali dibaca — lihat catatan getter destruktif pada javadoc kelas. */
	private String nama;
	/** Nilai realisasi/aktual dalam bentuk string numerik mentah, dipakai untuk perhitungan formula/skor (lihat {@link #getVal()}). */
	private String val;
	/** Format KPI (template) yang menaungi item ini secara langsung — bisa ditimpa oleh format milik {@link #formatKpiDetail} (lihat {@link #getFormatKpi()}). */
	private FormatKpi formatKpi;
	/** Item KPI induk dalam struktur pohon (kolom {@code bagian_dari}), untuk item KPI komposit yang tersusun dari beberapa sub-item. */
	private ItemKpi parent;
	/** Detail format KPI (penugasan template ke satu pegawai, efektif sejak tanggal tertentu) yang menaungi item ini. */
	private FormatKpiDetail formatKpiDetail;
	/** Nomor urut tampil item ini di antara item-item sejenis; juga dipakai untuk menyusun {@link #getKode()}. */
	private Integer nomorUrut;
	/** Penanda aktif/tidak; default {@code true} baik di inisialisasi field maupun di {@link #getAktif()} — lihat catatan pola flag aktif satu-arah pada javadoc kelas. */
	private Boolean aktif = true;
	/** Keterangan/catatan bebas untuk item KPI ini. */
	private String keterangan;
	/** Kedalaman item ini dalam struktur pohon {@link #parent}; {@code 0}/{@code null} berarti item tingkat teratas. */
	private Integer deep;
	/** Penghitung berapa kali item ini "dipakai" (mis. dirujuk oleh proses penilaian/skor lain); default {@code 0}. */
	private Long jmlDipakai = 0L;
	/** Definisi KPI master yang diinstansiasi oleh item ini — sumber kode, nama, formula default, satuan, dan kategori. */
	private Kpi kpi;
	/** Formula perhitungan (JSON) khusus item ini; bila kosong, dihasilkan otomatis oleh {@link #getFormula()} dari kode item dan kode {@link Kpi} induk. */
	private String formula;

	/** Nilai target yang hendak dicapai untuk item KPI ini; default {@code 0.0} bila belum diisi. */
	private Double target;
	/** Nilai realisasi dalam bentuk yang siap ditampilkan ke pengguna (bisa berbeda format dari {@link #val}). */
	private String valtampil;

	/** Konstruktor tanpa argumen, dipakai Hibernate untuk membentuk instance via reflection. */
	public ItemKpi() {
	}

	/**
	 * Mengembalikan kunci primer baris {@code item_kpi}. Kolom identity ({@code insertable = false}) —
	 * nilainya dibuat oleh basis data saat INSERT, bukan diisi manual oleh aplikasi.
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
	 * {@code insertable = false} — nilai sesungguhnya selalu berasal dari basis data.
	 *
	 * @param id id baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode item KPI ini, berbentuk {@code <kode-Kpi-induk>.<nomorUrut>}.
	 *
	 * <p><b>Getter destruktif:</b> setiap pemanggilan me-resolve {@link #getKpi()} (yang
	 * sendiri me-refresh field {@code kpi} lewat {@code check()}) lalu, bila {@link Kpi} induk
	 * tersedia, MENIMPA field lokal {@code kode} dengan nilai turunan tersebut — mengabaikan
	 * apa pun yang sebelumnya di-set lewat {@link #setKode(String)}. Karena entitas ini
	 * memakai {@code dynamicUpdate}, pemanggilan getter ini sebelum flush Hibernate (mis. dari
	 * {@link #compareTo(GeneralValueObject)} atau tampilan ZK) akan membuat nilai turunan ini
	 * yang tersimpan ke kolom {@code kode}, bukan nilai yang secara eksplisit di-set.</p>
	 *
	 * @return kode gabungan kode {@link Kpi} induk dan nomor urut, atau nilai field mentah bila
	 *         {@link Kpi} induk belum/tidak tersedia
	 */
	public String getKode() {
		kpi = getKpi();
		if (kpi != null) {
			kode = kpi.getKode() + "." + getNomorUrut();
		}
		return kode;
	}

	/**
	 * Mengisi field kode secara manual. Efeknya dapat langsung tertimpa oleh pemanggilan
	 * berikutnya ke {@link #getKode()} selama {@link Kpi} induk tersedia — lihat javadoc getter.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama item KPI ini, disalin dari nama {@link Kpi} induk.
	 *
	 * <p><b>Getter destruktif:</b> sama seperti {@link #getKode()}, setiap pemanggilan menimpa
	 * field lokal {@code nama} dengan {@code kpi.getNama()} selama {@link Kpi} induk tersedia,
	 * membuat {@link #setNama(String)} eksplisit tidak berpengaruh dalam praktiknya.</p>
	 *
	 * @return nama {@link Kpi} induk, atau nilai field mentah bila induk belum/tidak tersedia
	 */
	public String getNama() {
		kpi = getKpi();
		if (kpi != null) {
			nama = kpi.getNama();
		}
		return nama;
	}

	/**
	 * Mengisi field nama secara manual. Efeknya dapat langsung tertimpa oleh pemanggilan
	 * berikutnya ke {@link #getNama()} — lihat javadoc getter.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan/catatan bebas item KPI ini.
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan/catatan bebas item KPI ini.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan {@link FormatKpi} (template) yang menaungi item ini.
	 *
	 * <p>Resolusi mengikuti prioritas: bila {@link #getFormatKpiDetail()} tersedia dan memiliki
	 * {@link FormatKpi} sendiri, nilai TERSEBUT yang dipakai — menimpa field {@code formatKpi}
	 * lokal (setelah lebih dulu di-resolve lewat {@code check()}). Ini berarti secara desain,
	 * format KPI efektif dari sebuah item selalu mengikuti format milik detail penugasannya
	 * (pegawai + tanggal efektif), bukan referensi langsung {@code format_kpi} pada baris
	 * {@code item_kpi} itu sendiri kecuali {@link #formatKpiDetail} kosong/tidak
	 * memiliki format.</p>
	 *
	 * @return {@link FormatKpi} efektif untuk item ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_kpi", nullable = false)
	public FormatKpi getFormatKpi() {
		formatKpi = check(formatKpi);
		if (getFormatKpiDetail() != null && getFormatKpiDetail().getFormatKpi() != null) {
			formatKpi = getFormatKpiDetail().getFormatKpi();
		}
		return formatKpi;
	}

	/**
	 * Mengisi referensi {@link FormatKpi} langsung pada baris ini. Nilai ini dapat ditimpa
	 * oleh {@link #getFormatKpi()} bila {@link #formatKpiDetail} memiliki format sendiri —
	 * lihat javadoc getter.
	 *
	 * @param formatKpi format KPI baru
	 */
	public void setFormatKpi(FormatKpi formatKpi) {
		this.formatKpi = formatKpi;
	}

	/**
	 * Mengembalikan item KPI induk dalam struktur pohon (kolom {@code bagian_dari}), untuk
	 * item KPI komposit yang tersusun dari beberapa sub-item. Field di-refresh lewat
	 * {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas.
	 *
	 * @return item KPI induk, atau {@code null} bila item ini adalah tingkat teratas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bagian_dari", nullable = true)
	public ItemKpi getParent() {
		parent = check(parent);
		return parent;
	}

	/**
	 * Mengisi item KPI induk dalam struktur pohon.
	 *
	 * @param parent item KPI induk baru, atau {@code null} untuk menjadikan item ini tingkat teratas
	 */
	public void setParent(ItemKpi parent) {
		this.parent = parent;
	}

	/**
	 * Mengembalikan nomor urut tampil item ini, dipakai juga oleh {@link #getKode()} untuk
	 * menyusun kode gabungan.
	 *
	 * @return nomor urut, default {@code 0} bila belum diisi
	 */
	@Column(name = "urutan")
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Mengisi nomor urut tampil item ini.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan status aktif item KPI ini. Bila field belum pernah diisi ({@code null}),
	 * getter ini SEKALIGUS menetapkan field ke {@code true} (bukan sekadar mengembalikan nilai
	 * default tanpa efek samping) sebelum mengembalikannya — pola flag aktif yang tercatat
	 * berulang pada entitas-entitas modul KPI lain.
	 *
	 * @return {@code true} bila aktif atau belum pernah diisi; nilai field bila sudah eksplisit di-set
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengisi status aktif item KPI ini.
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan kedalaman item ini dalam struktur pohon {@link #getParent()}.
	 *
	 * @return kedalaman, atau {@code null} bila belum diisi/dihitung
	 */
	public Integer getDeep() {
		return deep;
	}

	/**
	 * Mengisi kedalaman item ini dalam struktur pohon.
	 *
	 * @param deep kedalaman baru
	 */
	public void setDeep(Integer deep) {
		this.deep = deep;
	}

	/**
	 * Mengembalikan jumlah pemakaian item KPI ini (mis. berapa kali dirujuk oleh proses
	 * penilaian/skor lain di modul KPI).
	 *
	 * @return jumlah pemakaian, default {@code 0}
	 */
	public Long getJmlDipakai() {
		return jmlDipakai;
	}

	/**
	 * Mengisi jumlah pemakaian item KPI ini.
	 *
	 * @param jmlDipakai jumlah pemakaian baru
	 */
	public void setJmlDipakai(Long jmlDipakai) {
		this.jmlDipakai = jmlDipakai;
	}

	/**
	 * Mengembalikan definisi {@link Kpi} master yang diinstansiasi oleh item ini. Field
	 * di-refresh lewat {@code check()} sebelum dikembalikan — lihat catatan shadow re-resolve
	 * pada javadoc kelas; ini juga yang dipakai oleh {@link #getKode()}, {@link #getNama()},
	 * {@link #getVal()}, {@link #getValtampil()}, dan {@link #getFormula()} untuk mengambil
	 * data induk (kode, nama, satuan, nilai default).
	 *
	 * @return {@link Kpi} master untuk item ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kpi", nullable = false)
	public Kpi getKpi() {
		kpi = check(kpi);
		return kpi;
	}

	/**
	 * Mengisi definisi {@link Kpi} master yang diinstansiasi oleh item ini.
	 *
	 * @param kpi definisi KPI master baru
	 */
	public void setKpi(Kpi kpi) {
		this.kpi = kpi;
	}

	/**
	 * Mengembalikan nilai realisasi/aktual item KPI ini dalam bentuk string numerik, dipakai
	 * sebagai input perhitungan formula/skor.
	 *
	 * <p>Logikanya bertingkat: pertama, kelas ini mencoba mengambil
	 * {@link ais.database.model.ParameterTambahan} dari satuan {@link Kpi} induk
	 * ({@code getKpi().getSatuanKpi().getParameterTambahan()}) — bila satuan KPI mendefinisikan
	 * parameter tambahan (mis. daftar pilihan nilai terstruktur), keberadaannya memengaruhi
	 * apakah nilai kosong/nol dianggap "belum diisi" dan perlu di-fallback ke
	 * {@link Kpi#getValDefault()}. Pengambilan ini dibungkus percobaan-tangkap yang SENGAJA
	 * menelan exception (dicatat ke {@link ais.common.ErrorAuditUtil}) dengan komentar eksplisit
	 * di kode: karena {@code kpi}/{@code satuanKpi} bisa jadi instance kanonik/bersama yang
	 * dipegang oleh {@code AuditTimestampInterceptor} dengan proxy Hibernate yang terikat ke
	 * sesi lain yang sudah tertutup, memicu {@code LazyInitializationException}
	 * — getter TIDAK BOLEH ikut gagal karenanya, sehingga bagian itu dilewati dan nilai fallback
	 * yang sudah ada dipertahankan. Bila {@code parameterTambahan} tetap {@code null} setelah
	 * percobaan tersebut (baik karena satuan tidak mendefinisikannya, atau karena exception
	 * tertelan), ATAU bila nilai field {@code val} kosong/nol dan {@code kpi} tersedia, nilai
	 * di-fallback ke {@link Kpi#getValDefault()} milik induk. Akhirnya, nilai dikembalikan
	 * hanya bila lolos validasi {@link ais.common.Common#isNumber(String)}; jika tidak, hasilnya
	 * {@code "0"}.</p>
	 *
	 * <p><b>Catatan risiko:</b> baris {@code val = kpi.getValDefault();} memakai field {@code kpi}
	 * mentah (bukan {@link #getKpi()}) — ini aman selama pemanggilan {@code getKpi()} di dalam
	 * blok percobaan di atas berhasil mengisi field tersebut sebelum baris ini dieksekusi
	 * (assignment terjadi sebelum exception mana pun bisa dilempar dari {@code check()}), namun
	 * berpotensi {@code NullPointerException} pada skenario tepi bila {@code check(kpi)} sendiri
	 * melempar exception SEBELUM sempat menugaskan hasil resolusi ke field (mis. kegagalan total
	 * mengambil sesi Hibernate) sementara field {@code kpi} belum pernah di-set sama sekali.</p>
	 *
	 * @return nilai realisasi sebagai string numerik, dijamin selalu berupa angka valid
	 *         (fallback {@code "0"} bila tidak valid)
	 */
	@Column(name = "val", nullable = true, columnDefinition = "text")
	public String getVal() {

		ParameterTambahan parameterTambahan = null;
		try {
			// FIX LazyInitializationException: kpi/satuanKpi bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			parameterTambahan = getKpi().getSatuanKpi() == null ? null
					: getKpi().getSatuanKpi().getParameterTambahan();
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/ItemKpi.java:getVal-lazy");
		}
		if (parameterTambahan == null) {
			val = kpi.getValDefault();
		} else if ((val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("0")) && kpi != null) {
			val = kpi.getValDefault();
		}

		return val == null || val.trim().isEmpty() || !Common.isNumber(val) ? "0" : val;
	}

	/**
	 * Mengisi nilai realisasi/aktual item KPI ini dalam bentuk string numerik mentah.
	 *
	 * @param val nilai realisasi baru
	 */
	public void setVal(String val) {
		this.val = val;
	}

	/** Cache string JSON array kosong, dipakai {@link #getFormula()} untuk mendeteksi formula yang belum pernah diisi secara berarti. */
	private static String string_kosong_json = new JSONArray().toString();

	/**
	 * Mengembalikan formula perhitungan (JSON array berisi satu objek {@code tgl}/{@code target})
	 * untuk item KPI ini.
	 *
	 * <p>Bila field {@code formula} kosong, {@code null}, atau masih berupa representasi array
	 * JSON kosong ({@link #string_kosong_json}), method ini MEMBANGKITKAN formula default secara
	 * on-the-fly: satu objek JSON dengan {@code tgl} berisi tanggal saat ini (format
	 * {@link ais.common.Common#dateFormat1}) dan {@code target} berisi ekspresi
	 * {@code "<kode item ini> * <kode Kpi induk>"} (memanggil {@link #getKode()}, sehingga ikut
	 * memicu efek samping getter destruktif tersebut). Formula yang dibangkitkan ini
	 * DITUGASKAN KEMBALI ke field {@code formula} — sehingga nilai auto-generated ini yang akan
	 * ikut tersimpan ke kolom {@code formula} pada operasi simpan berikutnya (konsisten dengan
	 * {@code dynamicUpdate = true}). Kegagalan penyusunan JSON ditelan diam-diam dan dicatat ke
	 * {@link ais.common.ErrorAuditUtil}, dalam hal ini formula tetap bernilai apa pun yang sudah
	 * ada di field sebelum percobaan (bisa saja masih kosong).</p>
	 *
	 * @return formula JSON array untuk item ini, dibangkitkan otomatis bila sebelumnya kosong
	 */
	@Column(name = "formula", nullable = true, columnDefinition = "text")
	public String getFormula() {

		if (formula == null || formula.isEmpty() || formula.equalsIgnoreCase(string_kosong_json)) {
			JSONArray array = new JSONArray();
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("tgl", Common.dateFormat1.get().format(WaktuUtil.getDate()));
				jsonObject.put("target", getKode() + " * " + kpi.getKode());
				array.put(jsonObject);
				formula = array.toString();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/kpi/ItemKpi.java:275");

			}
		}

		return formula;
	}

	/**
	 * Mengisi formula perhitungan (JSON) khusus item ini secara manual. Nilai ini dapat
	 * ditimpa kembali oleh {@link #getFormula()} bila dianggap "kosong" pada pemanggilan
	 * berikutnya — lihat javadoc getter.
	 *
	 * @param formula formula JSON baru
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Mengembalikan nilai target yang hendak dicapai untuk item KPI ini.
	 *
	 * @return nilai target, default {@code 0.0} bila belum diisi
	 */
	public Double getTarget() {
		return target == null ? 0.0 : target;
	}

	/**
	 * Mengisi nilai target yang hendak dicapai untuk item KPI ini.
	 *
	 * @param target nilai target baru
	 */
	public void setTarget(Double target) {
		this.target = target;
	}

	/**
	 * Mengembalikan nilai realisasi item KPI ini dalam bentuk yang siap ditampilkan ke
	 * pengguna. Logikanya cermin dari {@link #getVal()}: mencoba mengambil
	 * {@link ais.database.model.ParameterTambahan} dari satuan {@link Kpi} induk (dibungkus
	 * percobaan-tangkap yang sengaja menelan {@code LazyInitializationException} dengan alasan
	 * yang sama seperti dijelaskan di {@link #getVal()}), lalu meng-fallback ke
	 * {@link Kpi#getValDefault()} bila parameter tambahan tidak ada atau nilai tampil masih
	 * kosong/nol. Berbeda dari {@link #getVal()}, method ini TIDAK memvalidasi hasil akhir
	 * dengan {@link ais.common.Common#isNumber(String)} — bila field {@code valtampil} tetap
	 * {@code null} setelah seluruh langkah di atas, method ini melimpahkan ke {@link #getVal()}
	 * sebagai fallback terakhir (bukan mengembalikan {@code null} begitu saja).
	 *
	 * @return nilai realisasi untuk ditampilkan; fallback ke {@link #getVal()} bila belum diisi
	 */
	@Column(name = "valtampil", nullable = true, columnDefinition = "text")
	public String getValtampil() {

		ParameterTambahan parameterTambahan = null;
		try {
			// FIX LazyInitializationException: kpi/satuanKpi bisa berupa instance canonical/shared
			// (AuditTimestampInterceptor) yang proxy-nya terikat ke Session lain yang sudah closed
			// -> jangan biarkan getter ini crash, cukup lewati bagian ini (nilai fallback dipertahankan).
			parameterTambahan = getKpi().getSatuanKpi() == null ? null
					: getKpi().getSatuanKpi().getParameterTambahan();
		} catch (Exception exLazy) { ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) src/ais/database/model/kpi/ItemKpi.java:getValtampil-lazy");
		}
		if (parameterTambahan == null) {
			valtampil = kpi.getValDefault();
		} else if ((valtampil == null || valtampil.trim().isEmpty() || valtampil.trim().equalsIgnoreCase("0"))
				&& kpi != null) {
			valtampil = kpi.getValDefault();
		}

		return valtampil == null ? getVal() : valtampil;
	}

	/**
	 * Mengisi nilai realisasi tampil item KPI ini secara manual.
	 *
	 * @param valtampil nilai tampil baru
	 */
	public void setValtampil(String valtampil) {
		this.valtampil = valtampil;
	}

	/**
	 * Mengembalikan {@link FormatKpiDetail} (penugasan template KPI ke satu pegawai, efektif
	 * sejak tanggal tertentu) yang menaungi item ini. Field di-refresh lewat {@code check()}
	 * sebelum dikembalikan — lihat catatan shadow re-resolve pada javadoc kelas; nilai ini juga
	 * yang dipakai {@link #getFormatKpi()} untuk menentukan format KPI efektif.
	 *
	 * @return detail format KPI yang menaungi item ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "format_kpi_detail", nullable = true)
	public FormatKpiDetail getFormatKpiDetail() {
		formatKpiDetail = check(formatKpiDetail);
		return formatKpiDetail;
	}

	/**
	 * Mengisi {@link FormatKpiDetail} yang menaungi item ini.
	 *
	 * @param formatKpiDetail detail format KPI baru
	 */
	public void setFormatKpiDetail(FormatKpiDetail formatKpiDetail) {
		this.formatKpiDetail = formatKpiDetail;
	}
}
