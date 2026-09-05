package ais.database.model.rab;

// Generated Dec 19, 2009 10:58:09 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>TOR</b> (<i>Term of Reference</i>, dikenal juga sebagai KAK — Kerangka Acuan Kerja)
 * pada modul RAB/perencanaan anggaran, dipetakan ke tabel {@code rab.tor}. Satu baris mewakili satu
 * dokumen kerangka acuan yang menjelaskan <b>mengapa dan bagaimana</b> sebuah keluaran kegiatan akan
 * dihasilkan. Bentuknya mengikuti sistematika baku dokumen TOR pemerintah: identitas
 * (satuan kerja, eselon, program, kegiatan, hasil, indikator, output, volume, satuan) diikuti
 * sembilan bagian uraian naratif yang masing-masing disimpan sebagai kolom {@code text}.
 *
 * <h2>Sembilan bagian naratif</h2>
 * <p>Urutan field mengikuti urutan bab pada format TOR baku, dan seluruhnya berupa teks bebas tanpa
 * validasi isi: {@link #getLatarBelakang() latar belakang}, {@link #getDasarHukum() dasar hukum},
 * {@link #getGambaranUmum() gambaran umum}, {@link #getPenerimaManfaat() penerima manfaat},
 * {@link #getStrategiPencapaianKeluaran() strategi pencapaian keluaran},
 * {@link #getMetodePelaksanaan() metode pelaksanaan},
 * {@link #getTahapanDanWaktuPelaksanaan() tahapan dan waktu pelaksanaan},
 * {@link #getKurunWaktuPencapaianKeluaran() kurun waktu pencapaian keluaran}, dan
 * {@link #getBiayaYangDiperlukan() biaya yang diperlukan}. Perhatikan bahwa "biaya yang diperlukan"
 * adalah <b>uraian naratif berupa {@link String}</b>, bukan angka: entitas ini tidak menyimpan nilai
 * rupiah yang dapat dihitung. Satu-satunya besaran numerik di sini adalah {@link #getVolume()},
 * yang menyatakan banyaknya keluaran, bukan uang.</p>
 *
 * <h2>Posisi dalam klaster perencanaan</h2>
 * <p>TOR adalah simpul yang <b>menautkan paling banyak entitas</b> di klaster ini. Verifikasi atas
 * kode menunjukkan enam relasinya sebagai berikut:</p>
 * <ul>
 *   <li>{@link #getSatuanKerja()} — satuan kerja penyusun dokumen (penanda tenant);</li>
 *   <li>{@link #getProgram()} dan {@link #getKegiatan()} — <b>keduanya bertipe
 *   {@link Workspace}</b>, yaitu simpul pada pohon program/kegiatan beranggaran. Perannya
 *   dibedakan semata oleh nama kolom ({@code program} dan {@code kegiatan}), bukan oleh tipe:
 *   {@code Workspace} adalah pohon rujukan-diri yang jenis tiap simpulnya ditentukan
 *   {@link JenisWorkspace}. Tidak ada penjaga di tingkat entitas yang memastikan simpul yang
 *   ditunjuk {@code kegiatan} benar-benar merupakan anak dari simpul yang ditunjuk
 *   {@code program}, maupun bahwa jenis masing-masing simpul sesuai perannya;</li>
 *   <li>{@link #getIndikator()} — satu entri katalog {@link Indikator} sebagai ukuran keberhasilan.
 *   Ini satu-satunya tempat di klaster ini yang benar-benar memakai entitas {@code Indikator}
 *   sebagai relasi (bandingkan {@link RenstraProgramPunyaIndikator} yang memakai teks bebas);</li>
 *   <li>{@link #getOutputKegiatan()} — keluaran yang diuraikan dokumen ini. Inilah tautan ke
 *   {@link OutputKegiatan}, katalog yang sama yang dipakai
 *   {@link RencanaDanRealisasiOutputKegiatan} untuk mencatat target dan capaian bulanan. Dengan
 *   demikian TOR (rencana naratif) dan rencana-realisasi (rencana numerik) bertemu pada output yang
 *   sama, meski <b>tidak</b> saling merujuk secara langsung;</li>
 *   <li>{@link #getSatuan()} — satuan pengukuran bagi {@link #getVolume()}.</li>
 * </ul>
 * <p>Perlu dicatat asimetri berikut: TOR merujuk {@link Indikator} tetapi <b>tidak</b> merujuk
 * {@link Sasaran}, padahal keduanya adalah pasangan kembar di paket ini. TOR juga tidak berelasi ke
 * {@link Proyek} maupun {@link RenstraProgram}.</p>
 *
 * <h2>PERINGATAN — {@code kode} dan {@code nama} tidak dipetakan pada entitas ini</h2>
 * <p>Berbeda dari hampir seluruh entitas sepaket, {@code Tor} <b>tidak mendeklarasikan</b> field
 * maupun getter {@code kode} dan {@code nama} miliknya sendiri. Kedua properti itu memang tersedia
 * lewat pewarisan dari {@link GeneralValueObject}, tetapi kelas induk tersebut adalah kelas abstrak
 * biasa — bukan {@code @MappedSuperclass} dan bukan {@code @Entity} — sehingga <b>propertinya tidak
 * ikut dipetakan</b> ke kolom basis data oleh Hibernate. Konsekuensinya ada dua, dan keduanya
 * teramati pada {@code ais.action.master.rab.TorAction}:</p>
 * <ul>
 *   <li><b>Pada tampilan.</b> Row renderer layar TOR menampilkan {@code tor.getKode()} dan memakai
 *   {@code tor.getNama()} sebagai label revisi. Karena kedua nilai itu tidak pernah dimuat dari
 *   basis data, keduanya selalu {@code null} — kolom kode pada daftar TOR selalu tampak kosong.</li>
 *   <li><b>Pada kueri.</b> {@code TorAction.initCriteria(...)} menyusun
 *   {@code Order.asc("nama")} dan {@code Restrictions.ilike("nama", ...)} atas
 *   {@code Criteria} bagi {@code Tor.class}. Properti {@code nama} tidak ada dalam metadata
 *   pemetaan entitas ini, sehingga kueri tersebut merujuk properti yang tak dikenal Hibernate.
 *   Jalur ini dipanggil langsung dari {@code doAfterCompose()} lewat {@code onSearchDefault(null)},
 *   yakni saat layar dibuka. Analisis statis ini perlu dikonfirmasi dengan menjalankan layar
 *   tersebut, tetapi bila benar, daftar TOR gagal dimuat sejak pemuatan pertama.</li>
 * </ul>
 * <p>Bila kelak diperbaiki, ada dua arah yang sama-sama masuk akal: menambahkan field {@code kode}
 * dan {@code nama} beserta pemetaannya pada entitas ini (menyeragamkannya dengan
 * {@link OutputKegiatan}/{@link Indikator}/{@link Sasaran}), atau mengubah {@code TorAction} agar
 * mengurutkan dan menyaring memakai properti yang memang dipetakan — misalnya {@code eselon} atau
 * relasi {@code outputKegiatan}. Pilihan pertama mengubah skema; pilihan kedua tidak.</p>
 *
 * <h2>Pembatasan tenant — ada, tetapi jatuh terbuka bila cakupan kosong</h2>
 * <p>{@code TorAction} menyaring dengan bentuk cakupan pohon yang sama seperti
 * {@code IndikatorAction} dan {@code SasaranAction}: {@code Restrictions.in("satuanKerja",
 * satuanKerjas)} di-{@code OR} dengan {@code isNull("satuanKerja")} ketika tidak ada satker induk
 * dipilih, tetapi diganti seluruhnya oleh {@code Restrictions.sqlRestriction("1=1")} bila himpunan
 * cakupan kosong. Perilaku <i>fail-open</i> ini pola berulang yang sudah tercatat. Untuk dokumen
 * TOR — yang memuat uraian strategi dan kebutuhan biaya — keterbukaan itu lebih berdampak
 * dibanding pada katalog nomenklatur.</p>
 *
 * <h2>Pemetaan ORM</h2>
 * <p>Entitas memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis
 * kolom yang benar-benar berubah, dan diberi {@link Audited} sehingga Hibernate Envers merekam
 * setiap revisi ke tabel bayangan pada skema {@code rab}. Kunci utama memakai strategi
 * {@link javax.persistence.GenerationType#IDENTITY}. Strategi fetch relasinya <b>tidak seragam</b>:
 * {@code satuanKerja}, {@code program}, {@code kegiatan}, dan {@code satuan} dinyatakan
 * {@link FetchType#LAZY} sehingga getter-nya memanggil {@link GeneralValueObject#check(Object)}
 * untuk memaksa resolusi proxy, sedangkan {@code indikator} dan {@code outputKegiatan} memakai
 * bawaan {@link ManyToOne} (eager) dengan {@link FetchMode#SELECT} sehingga getter-nya berupa getter
 * murni. Perbedaan ini disengaja mengikuti bentuk masing-masing dan jangan diseragamkan tanpa
 * mengubah anotasinya sekaligus.</p>
 *
 * @see OutputKegiatan
 * @see RencanaDanRealisasiOutputKegiatan
 * @see Workspace
 * @see Indikator
 * @see ais.database.dao.rab.TorDao
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "rab", name = "tor")

public class Tor extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilainya sengaja disamakan dengan hampir seluruh entitas lain
	 * di paket {@code ais.database.model.rab} (hasil salin-tempel templat hbm2java), sehingga
	 * <b>tidak</b> bisa dipakai untuk membedakan tipe saat deserialisasi.
	 */
	private static final long serialVersionUID = -8738027816264807168L;

	/**
	 * Kunci utama basis data, dibangkitkan oleh kolom identity pada {@code rab.tor.id}. Bernilai
	 * {@code null} selama objek belum pernah disimpan.
	 */
	private Long id;

	/**
	 * Field audit bayangan: nama pengguna terakhir yang mengubah baris ini. Diisi lewat
	 * {@link #setOleh(String)} oleh lapisan interceptor/penyimpanan, bukan oleh pengguna.
	 */
	private String oleh;

	/**
	 * Field audit bayangan: identitas (id pengguna) terakhir yang mengubah baris ini. Pasangan dari
	 * {@link #oleh}, diisi oleh lapisan interceptor/penyimpanan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Setter ini <b>menolak diam-diam</b> nilai
	 * {@code null} maupun string kosong/spasi sehingga jejak audit yang sudah terisi tidak terhapus
	 * oleh proses penyalinan objek atau pengikatan form yang mengirim nilai kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan sehingga jejak audit lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau hanya berisi spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang dijalankan tepat sebelum operasi {@code UPDATE}, mendelegasikan
	 * pemutakhiran stempel waktu ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak boleh dipanggil
	 * langsung dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}

	/**
	 * Field audit bayangan: stempel waktu perubahan terakhir. Diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil#getDate()} (bukan {@code new Date()}, agar mengikuti zona
	 * waktu/penyesuaian waktu aplikasi) dan diperbarui otomatis oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya diisi otomatis lewat {@link #onUpdate()};
	 * pemanggilan manual hanya relevan pada skenario impor/migrasi data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir, dipetakan sebagai kolom {@code TIMESTAMP}.
	 *
	 * @return waktu penyimpanan terakhir baris ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Satuan kerja penyusun dokumen TOR ini; berfungsi sebagai penanda tenant. Boleh {@code null}. */
	private SatuanKerja satuanKerja;

	/**
	 * Eselon unit penyusun, disimpan sebagai teks bebas (mis. "Eselon I", "Eselon II"). Divalidasi
	 * wajib isi oleh {@code TorAction} sebelum penyimpanan, tetapi tanpa pembatasan nilai.
	 */
	private String eselon;

	/**
	 * Simpul {@link Workspace} yang berperan sebagai <b>program</b> induk. Peran ini ditentukan oleh
	 * nama kolom saja, bukan oleh tipe — lihat catatan pada dokumentasi kelas.
	 */
	private Workspace program;

	/**
	 * Rumusan <i>hasil</i> (<i>outcome</i>) yang diharapkan dari program, sebagai teks bebas. Tidak
	 * diberi anotasi {@code @Column} sehingga memakai pemetaan bawaan Hibernate — satu-satunya
	 * bidang naratif pada entitas ini yang <b>tidak</b> bertipe {@code text}, sehingga panjangnya
	 * terbatas 255 karakter.
	 */
	private String hasil;

	/**
	 * Simpul {@link Workspace} yang berperan sebagai <b>kegiatan</b>. Sama seperti {@link #program},
	 * peran ini ditentukan oleh nama kolom saja; tidak ada penjaga bahwa simpul ini merupakan anak
	 * dari simpul {@code program}.
	 */
	private Workspace kegiatan;

	/** Indikator keberhasilan kegiatan, merujuk katalog {@link Indikator}. Boleh {@code null}. */
	private Indikator indikator;

	/** Keluaran yang diuraikan dokumen ini, merujuk katalog {@link OutputKegiatan}. Boleh {@code null}. */
	private OutputKegiatan outputKegiatan;

	/**
	 * Banyaknya keluaran yang direncanakan, diukur dalam {@link #satuan}. Diinisialisasi
	 * {@code 0.0}. Ini satu-satunya besaran numerik pada entitas — nilai uang hanya ada sebagai
	 * uraian naratif pada {@link #biayaYangDiperlukan}.
	 */
	private Double volume = 0.0;

	/** Satuan pengukuran bagi {@link #volume}, merujuk master {@link Satuan}. Boleh {@code null}. */
	private Satuan satuan;

	/** Bab "Latar Belakang" dokumen TOR; kolom {@code text}. */
	private String latarBelakang;
	/** Bab "Dasar Hukum" dokumen TOR; kolom {@code text}. */
	private String dasarHukum;
	/** Bab "Gambaran Umum" dokumen TOR; kolom {@code text}. */
	private String gambaranUmum;
	/** Bab "Penerima Manfaat" dokumen TOR; kolom {@code text}. */
	private String penerimaManfaat;
	/** Bab "Strategi Pencapaian Keluaran" dokumen TOR; kolom {@code text}. */
	private String strategiPencapaianKeluaran;
	/** Bab "Metode Pelaksanaan" dokumen TOR; kolom {@code text}. */
	private String metodePelaksanaan;
	/** Bab "Tahapan dan Waktu Pelaksanaan" dokumen TOR; kolom {@code text}. */
	private String tahapanDanWaktuPelaksanaan;
	/** Bab "Kurun Waktu Pencapaian Keluaran" dokumen TOR; kolom {@code text}. */
	private String kurunWaktuPencapaianKeluaran;
	/** Bab "Biaya Yang Diperlukan" dokumen TOR, berupa <b>uraian naratif</b>; kolom {@code text}. */
	private String biayaYangDiperlukan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Juga dipakai {@code TorAction} saat menekan tombol tambah data. Entitas ini tidak memiliki
	 * konstruktor pintas berargumen, karena tidak ada kolom {@code NOT NULL} yang perlu diisi.
	 */
	public Tor() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Normalnya diisi Hibernate setelah {@code INSERT}; pengisian manual hanya
	 * relevan pada skenario impor/migrasi.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan satuan kerja penyusun dokumen TOR ini. Method ini <b>bukan getter murni</b>: ia
	 * memanggil {@link GeneralValueObject#check(Object)} yang memaksa proxy Hibernate yang masih
	 * malas menjadi objek nyata, lalu menuliskan hasilnya kembali ke field. Langkah itu diperlukan
	 * karena relasi dinyatakan {@link FetchType#LAZY}; tanpanya, pemakaian nilai di luar sesi
	 * Hibernate — misalnya saat baris di-<i>render</i> ke layar ZK — berisiko
	 * {@code LazyInitializationException}.
	 *
	 * <p><b>Perbedaan penting dari entitas tetangga.</b> Tidak seperti
	 * {@link Proyek#getSatuanKerja()}, {@link Indikator#getSatuanKerja()},
	 * {@link Sasaran#getSatuanKerja()}, maupun
	 * {@link RencanaDanRealisasiOutputKegiatan#getSatuanKerja()}, method ini <b>tidak</b> mengisi
	 * satuan kerja secara otomatis dari pengguna yang sedang login. Ia hanya menyelesaikan proxy
	 * dan mengembalikan apa adanya. Artinya penentuan pemilik dokumen TOR sepenuhnya bergantung pada
	 * {@code TorAction} yang memanggil {@link #setSatuanKerja(SatuanKerja)} berdasarkan pilihan
	 * pengguna pada form. Ini justru perilaku yang lebih dapat diprediksi: tidak ada kepemilikan
	 * yang berubah sebagai efek samping pembacaan. Konsekuensinya, TOR yang dibuat lewat jalur non-UI
	 * (impor, batch) akan tersimpan dengan {@code satuan_kerja} bernilai NULL kecuali pemanggil
	 * mengisinya sendiri — dan baris NULL semacam itu diperlakukan {@code TorAction} sebagai dokumen
	 * global yang terlihat oleh semua satuan kerja.</p>
	 *
	 * @return satuan kerja penyusun, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menyetel satuan kerja penyusun dokumen TOR. Dipanggil {@code TorAction} saat menyimpan. Tidak
	 * ada validasi bahwa satuan kerja yang diberikan berada dalam cakupan wewenang pengguna.
	 *
	 * @param satuanKerja satuan kerja penyusun; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Mengembalikan eselon unit penyusun sebagai teks bebas, apa adanya tanpa pemangkasan spasi.
	 * {@code TorAction} menampilkannya langsung sebagai label pada daftar, sehingga nilai
	 * {@code null} akan tampil sebagai label kosong.
	 *
	 * @return teks eselon, atau {@code null} bila belum diisi.
	 */
	public String getEselon() {
		return eselon;
	}

	/**
	 * Menyetel eselon unit penyusun. Kewajiban isi ditegakkan di lapisan {@code TorAction}, bukan di
	 * sini, dan tidak ada pembatasan nilai yang diperbolehkan.
	 *
	 * @param eselon teks eselon.
	 */
	public void setEselon(String eselon) {
		this.eselon = eselon;
	}

	/**
	 * Mengembalikan simpul {@link Workspace} yang berperan sebagai program induk. Seperti
	 * {@link #getSatuanKerja()}, method ini memanggil {@link GeneralValueObject#check(Object)}
	 * untuk memaksa resolusi proxy lazy dan menuliskan hasilnya kembali ke field, sehingga ia
	 * mengubah state objek. Tidak ada validasi bahwa simpul yang dikembalikan benar-benar berjenis
	 * program menurut {@link JenisWorkspace}-nya.
	 *
	 * @return simpul program, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "program", nullable = true)
	public Workspace getProgram() {
		program = check(program);
		return program;
	}

	/**
	 * Menyetel simpul {@link Workspace} yang berperan sebagai program induk. Tidak ada penjaga
	 * konsistensi terhadap {@link #setKegiatan(Workspace)} maupun terhadap satuan kerja dokumen.
	 *
	 * @param program simpul program; boleh {@code null}.
	 */
	public void setProgram(Workspace program) {
		this.program = program;
	}

	/**
	 * Mengembalikan rumusan hasil (<i>outcome</i>) yang diharapkan, apa adanya tanpa pemangkasan
	 * spasi. Berbeda dari sembilan bab naratif lainnya, kolom ini tidak bertipe {@code text}
	 * sehingga isinya terbatas 255 karakter — teks yang lebih panjang akan gagal saat penyimpanan.
	 *
	 * @return teks hasil, atau {@code null} bila belum diisi.
	 */
	public String getHasil() {
		return hasil;
	}

	/**
	 * Menyetel rumusan hasil yang diharapkan. Tidak ada pemeriksaan panjang di sini; batas 255
	 * karakter baru ditegakkan saat penyimpanan ke basis data.
	 *
	 * @param hasil teks hasil.
	 */
	public void setHasil(String hasil) {
		this.hasil = hasil;
	}

	/**
	 * Mengembalikan simpul {@link Workspace} yang berperan sebagai kegiatan. Seperti
	 * {@link #getProgram()}, method ini memanggil {@link GeneralValueObject#check(Object)} untuk
	 * memaksa resolusi proxy lazy dan menuliskan hasilnya kembali ke field. Tidak ada validasi bahwa
	 * simpul ini merupakan anak dari simpul {@link #getProgram()}.
	 *
	 * @return simpul kegiatan, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kegiatan", nullable = true)
	public Workspace getKegiatan() {
		kegiatan = check(kegiatan);
		return kegiatan;
	}

	/**
	 * Menyetel simpul {@link Workspace} yang berperan sebagai kegiatan.
	 *
	 * @param kegiatan simpul kegiatan; boleh {@code null}.
	 */
	public void setKegiatan(Workspace kegiatan) {
		this.kegiatan = kegiatan;
	}

	/**
	 * Mengembalikan indikator keberhasilan kegiatan. Getter murni tanpa efek samping: relasi ini
	 * dipetakan eager ({@link ManyToOne} tanpa {@code fetch = LAZY}) dengan
	 * {@link FetchMode#SELECT}, sehingga nilainya sudah berupa objek nyata dan tidak perlu dipaksa
	 * lewat {@link GeneralValueObject#check(Object)}.
	 *
	 * @return indikator terkait, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "indikator", nullable = true)
	public Indikator getIndikator() {
		return indikator;
	}

	/**
	 * Menyetel indikator keberhasilan kegiatan. Tidak ada validasi bahwa indikator yang dipilih
	 * berasal dari satuan kerja yang sama dengan dokumen ini — katalog {@link Indikator} memang
	 * dimaksudkan untuk dipakai bersama.
	 *
	 * @param indikator entri katalog indikator; boleh {@code null}.
	 */
	public void setIndikator(Indikator indikator) {
		this.indikator = indikator;
	}

	/**
	 * Mengembalikan keluaran kegiatan yang diuraikan dokumen ini. Getter murni tanpa efek samping,
	 * dengan alasan pemetaan yang sama seperti {@link #getIndikator()}.
	 *
	 * <p>Relasi inilah jembatan konseptual antara dokumen TOR dan pencatatan capaian: entri
	 * {@link OutputKegiatan} yang sama juga dirujuk {@link RencanaDanRealisasiOutputKegiatan} yang
	 * menyimpan target dan realisasi bulanan. Namun jembatan itu <b>hanya lewat katalog</b> — tidak
	 * ada foreign key langsung antara TOR dan baris rencana-realisasi, dan tidak ada penjaga yang
	 * memastikan {@link #getVolume()} pada TOR sesuai dengan jumlah target bulanan yang dicatat di
	 * sana. Kedua angka dapat berbeda tanpa terdeteksi.</p>
	 *
	 * @return keluaran kegiatan terkait, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "output_kegiatan", nullable = true)
	public OutputKegiatan getOutputKegiatan() {
		return outputKegiatan;
	}

	/**
	 * Menyetel keluaran kegiatan yang diuraikan dokumen ini.
	 *
	 * @param outputKegiatan entri katalog output kegiatan; boleh {@code null}.
	 */
	public void setOutputKegiatan(OutputKegiatan outputKegiatan) {
		this.outputKegiatan = outputKegiatan;
	}

	/**
	 * Mengembalikan banyaknya keluaran yang direncanakan. Berbeda dari getter numerik pada
	 * {@link RenstraProgramPunyaIndikator} dan {@link RencanaDanRealisasiOutputKegiatan}, method ini
	 * <b>tidak</b> menormalkan {@code null} menjadi {@code 0.0}: nilai dikembalikan apa adanya.
	 * Field memang diinisialisasi {@code 0.0} pada objek baru, tetapi baris yang dimuat dari basis
	 * data dengan kolom NULL akan menghasilkan {@code null}. Pemanggil yang menjumlahkan atau
	 * membandingkan nilai ini wajib memeriksa {@code null} lebih dulu — perbedaan perilaku ini mudah
	 * terlewat ketika kode disalin dari entitas tetangga.
	 *
	 * @return volume keluaran, atau {@code null} bila kolomnya kosong di basis data.
	 */
	public Double getVolume() {
		return volume;
	}

	/**
	 * Menyetel banyaknya keluaran yang direncanakan. Tidak ada validasi nilai negatif maupun
	 * pencocokan terhadap target bulanan pada {@link RencanaDanRealisasiOutputKegiatan} untuk output
	 * yang sama.
	 *
	 * @param volume volume keluaran; boleh {@code null}.
	 */
	public void setVolume(Double volume) {
		this.volume = volume;
	}

	/**
	 * Mengembalikan satuan pengukuran bagi {@link #getVolume()}. Seperti {@link #getSatuanKerja()},
	 * method ini memanggil {@link GeneralValueObject#check(Object)} untuk memaksa resolusi proxy
	 * lazy dan menuliskan hasilnya kembali ke field.
	 *
	 * @return satuan pengukuran, atau {@code null} bila belum ditentukan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan", nullable = true)
	public Satuan getSatuan() {
		satuan = check(satuan);
		return satuan;
	}

	/**
	 * Menyetel satuan pengukuran bagi {@link #getVolume()}. Tidak ada penjaga yang memastikan satuan
	 * ini sama dengan satuan yang dipakai {@link RencanaDanRealisasiOutputKegiatan} untuk output
	 * yang sama, sehingga volume rencana pada TOR dan target bulanan dapat memakai satuan berbeda
	 * tanpa terdeteksi.
	 *
	 * @param satuan master satuan; boleh {@code null}.
	 */
	public void setSatuan(Satuan satuan) {
		this.satuan = satuan;
	}

	/**
	 * Mengembalikan bab "Latar Belakang" dokumen TOR, apa adanya tanpa normalisasi {@code null}.
	 *
	 * @return teks latar belakang, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getLatarBelakang() {
		return latarBelakang;
	}

	/**
	 * Menyetel bab "Latar Belakang" dokumen TOR.
	 *
	 * @param latarBelakang teks bab.
	 */
	public void setLatarBelakang(String latarBelakang) {
		this.latarBelakang = latarBelakang;
	}

	/**
	 * Mengembalikan bab "Dasar Hukum" dokumen TOR, apa adanya tanpa normalisasi {@code null}.
	 *
	 * @return teks dasar hukum, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getDasarHukum() {
		return dasarHukum;
	}

	/**
	 * Menyetel bab "Dasar Hukum" dokumen TOR.
	 *
	 * @param dasarHukum teks bab.
	 */
	public void setDasarHukum(String dasarHukum) {
		this.dasarHukum = dasarHukum;
	}

	/**
	 * Mengembalikan bab "Gambaran Umum" dokumen TOR, apa adanya tanpa normalisasi {@code null}.
	 *
	 * @return teks gambaran umum, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getGambaranUmum() {
		return gambaranUmum;
	}

	/**
	 * Menyetel bab "Gambaran Umum" dokumen TOR.
	 *
	 * @param gambaranUmum teks bab.
	 */
	public void setGambaranUmum(String gambaranUmum) {
		this.gambaranUmum = gambaranUmum;
	}

	/**
	 * Mengembalikan bab "Penerima Manfaat" dokumen TOR, apa adanya tanpa normalisasi {@code null}.
	 *
	 * @return teks penerima manfaat, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getPenerimaManfaat() {
		return penerimaManfaat;
	}

	/**
	 * Menyetel bab "Penerima Manfaat" dokumen TOR.
	 *
	 * @param penerimaManfaat teks bab.
	 */
	public void setPenerimaManfaat(String penerimaManfaat) {
		this.penerimaManfaat = penerimaManfaat;
	}

	/**
	 * Mengembalikan bab "Strategi Pencapaian Keluaran" dokumen TOR, apa adanya tanpa normalisasi
	 * {@code null}.
	 *
	 * @return teks strategi pencapaian keluaran, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getStrategiPencapaianKeluaran() {
		return strategiPencapaianKeluaran;
	}

	/**
	 * Menyetel bab "Strategi Pencapaian Keluaran" dokumen TOR.
	 *
	 * @param strategiPencapaianKeluaran teks bab.
	 */
	public void setStrategiPencapaianKeluaran(String strategiPencapaianKeluaran) {
		this.strategiPencapaianKeluaran = strategiPencapaianKeluaran;
	}

	/**
	 * Mengembalikan bab "Metode Pelaksanaan" dokumen TOR, apa adanya tanpa normalisasi {@code null}.
	 *
	 * @return teks metode pelaksanaan, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getMetodePelaksanaan() {
		return metodePelaksanaan;
	}

	/**
	 * Menyetel bab "Metode Pelaksanaan" dokumen TOR.
	 *
	 * @param metodePelaksanaan teks bab.
	 */
	public void setMetodePelaksanaan(String metodePelaksanaan) {
		this.metodePelaksanaan = metodePelaksanaan;
	}

	/**
	 * Mengembalikan bab "Tahapan dan Waktu Pelaksanaan" dokumen TOR, apa adanya tanpa normalisasi
	 * {@code null}. Isinya berupa uraian naratif, bukan tanggal terstruktur — entitas ini tidak
	 * memiliki kolom tanggal mulai/selesai, sehingga jadwal TOR tidak dapat dibandingkan secara
	 * otomatis dengan jadwal {@link Tugas} pada modul yang sama.
	 *
	 * @return teks tahapan dan waktu pelaksanaan, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getTahapanDanWaktuPelaksanaan() {
		return tahapanDanWaktuPelaksanaan;
	}

	/**
	 * Menyetel bab "Tahapan dan Waktu Pelaksanaan" dokumen TOR.
	 *
	 * @param tahapanDanWaktuPelaksanaan teks bab.
	 */
	public void setTahapanDanWaktuPelaksanaan(String tahapanDanWaktuPelaksanaan) {
		this.tahapanDanWaktuPelaksanaan = tahapanDanWaktuPelaksanaan;
	}

	/**
	 * Mengembalikan bab "Kurun Waktu Pencapaian Keluaran" dokumen TOR, apa adanya tanpa normalisasi
	 * {@code null}. Seperti {@link #getTahapanDanWaktuPelaksanaan()}, isinya naratif dan bukan
	 * rentang tanggal yang dapat diolah.
	 *
	 * @return teks kurun waktu pencapaian keluaran, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKurunWaktuPencapaianKeluaran() {
		return kurunWaktuPencapaianKeluaran;
	}

	/**
	 * Menyetel bab "Kurun Waktu Pencapaian Keluaran" dokumen TOR.
	 *
	 * @param kurunWaktuPencapaianKeluaran teks bab.
	 */
	public void setKurunWaktuPencapaianKeluaran(String kurunWaktuPencapaianKeluaran) {
		this.kurunWaktuPencapaianKeluaran = kurunWaktuPencapaianKeluaran;
	}

	/**
	 * Mengembalikan bab "Biaya Yang Diperlukan" dokumen TOR, apa adanya tanpa normalisasi
	 * {@code null}. Perlu ditegaskan bahwa nilainya <b>teks naratif</b>, bukan angka: entitas ini
	 * tidak menyimpan besaran rupiah yang dapat dijumlahkan, dibandingkan dengan pagu pada
	 * {@link Workspace}, atau direkonsiliasi dengan {@link PenggunaanAnggaran}. Setiap laporan yang
	 * membutuhkan angka biaya harus mengambilnya dari entitas anggaran, bukan dari sini.
	 *
	 * @return teks biaya yang diperlukan, atau {@code null} bila belum diisi.
	 */
	@Column(columnDefinition = "text")
	public String getBiayaYangDiperlukan() {
		return biayaYangDiperlukan;
	}

	/**
	 * Menyetel bab "Biaya Yang Diperlukan" dokumen TOR sebagai teks naratif.
	 *
	 * @param biayaYangDiperlukan teks bab.
	 */
	public void setBiayaYangDiperlukan(String biayaYangDiperlukan) {
		this.biayaYangDiperlukan = biayaYangDiperlukan;
	}

}
