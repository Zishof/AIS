package ais.database.model;

/*
 * author: Zulkifli, April 17, 2010
 */

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Entitas Hibernate untuk tabel {@code public.perguruan_tinggi_lain}, merepresentasikan satu
 * perguruan tinggi <b>eksternal</b> (di luar institusi induk AIS sendiri, yang datanya
 * tersimpan pada entitas terpisah {@link PerguruanTinggi}). Baris pada tabel ini berfungsi
 * sebagai data referensi/master perguruan tinggi lain, dipakai antara lain untuk mencatat:
 * <ul>
 * <li>asal perguruan tinggi calon mahasiswa/mahasiswa pindahan (lihat
 * {@link ais.database.model.BiodataCalonMahasiswa} dan {@link ais.database.model.Mahasiswa}),</li>
 * <li>perguruan tinggi tempat dosen mengajar di luar institusi induk (lihat
 * {@link ais.database.model.MengajarDiPerguruanTinggiLain}), dan</li>
 * <li>data hasil impor feeder PDDikti ({@code ais.action.master.feeder.util.FeederJSONImport}).</li>
 * </ul>
 * <p>
 * Struktur field-nya sengaja meniru profil {@link PerguruanTinggi} (identitas legal, alamat,
 * kontak, data sarana-prasarana untuk pelaporan EPSBED/PDDikti, akreditasi BAN-PT, rekening
 * bank, dan pejabat rektor) namun tanpa relasi ke entitas internal AIS lain (tidak ada
 * {@code @ManyToOne} ke {@link Pendaftar}/{@link Pegawai}/dsb.) karena institusi yang dicatat
 * di sini bukan bagian dari organisasi yang dikelola AIS.
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 *
 * <h3>Pola arsitektur yang perlu diwaspadai di kelas ini</h3>
 * <ul>
 *   <li><b>Getter yang mengubah state.</b> {@link #getEmail()} dan {@link #getDeskripsi()} menulis
 *       balik ke field-nya saat dibaca. Pada objek yang dikelola Hibernate, sekadar membaca
 *       properti tersebut dapat menandai entity kotor sehingga memicu {@code UPDATE} dan revisi
 *       Envers palsu. Ini instance dari pola getter-mutasi-field yang tersebar luas pada model
 *       AIS.</li>
 *   <li><b>Setter satu arah.</b> {@link #setOleh(String)} dan {@link #setOlehId(String)} mengabaikan
 *       argumen {@code null}/kosong secara senyap, sehingga jejak pelaku tidak bisa dikosongkan
 *       lewat jalur normal.</li>
 *   <li><b>Nilai baku pengganti yang tidak seragam.</b> Sebagian getter mengembalikan {@code null}
 *       apa adanya, sebagian mengganti dengan string kosong ({@link #getAlamat1()},
 *       {@link #getKodeSinta()}), satu mengganti dengan {@code null} justru ketika kosong
 *       ({@link #getFeeder()}), satu dengan {@code true} ({@link #getAktif()}), dan satu dengan
 *       angka {@code 0} yang bukan tahun yang sah
 *       ({@link #getTahunPertamaMenerimaMahasiswa()}). Jangan berasumsi seragam — periksa getter
 *       yang bersangkutan.</li>
 *   <li><b>Tanpa penjagaan duplikat.</b> {@link #getKodeYayasan()} dan
 *       {@link #getKodePerguruanTinggi()} dipetakan {@code nullable = false} tetapi <b>tanpa</b>
 *       batasan unik, dan lapisan model tidak memeriksa tabrakan. Karena baris di sini juga
 *       dibuat oleh impor feeder PDDikti, satu perguruan tinggi yang sama dapat masuk berkali-kali
 *       dengan kode yang sama; kode yang mengambil satu baris berdasarkan kode tersebut perlu
 *       bersiap menghadapi lebih dari satu hasil.</li>
 *   <li><b>Tanpa penyaring tenant/kepemilikan.</b> Entity ini murni data referensi bersama dan
 *       tidak punya kolom satuan kerja maupun pemilik, sehingga seluruh baris terlihat oleh semua
 *       penyewa. Perlakukan isinya sebagai data publik institusi, bukan data rahasia.</li>
 *   <li><b>Pemetaan kolom tidak seragam.</b> Sekitar separuh properti memakai {@code @Column}
 *       eksplisit, sisanya (mis. {@link #getNamaSingkat()}, {@link #getRektor()},
 *       {@link #getNoRek()}) mengandalkan strategi penamaan bawaan Hibernate. Saat menambah
 *       properti baru, sebutkan nama kolomnya secara eksplisit agar tidak bergantung pada
 *       konfigurasi global.</li>
 * </ul>
 *
 * @see PerguruanTinggi
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "perguruan_tinggi_lain")

public class PerguruanTinggiLain extends GeneralValueObject {
	/** Penanda versi serialisasi Java untuk entity ini. */
	private static final long serialVersionUID = -7550455125892447098L;

	/** Kunci utama baris perguruan tinggi eksternal (kolom {@code id}, IDENTITY). */
	private Long id;

	/** Nama pengguna terakhir yang membuat/mengubah baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang membuat/mengubah baris ini, pasangan dari {@link #oleh}. */
	private String olehId;

	/**
	 * Id pengguna yang terakhir membuat/mengubah baris ini (audit jejak perubahan).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah tercatat
	 * @see #getOleh()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mencatat id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Setter satu arah:</b> nilai {@code null} maupun kosong diabaikan secara senyap, jadi id
	 * pelaku yang sudah tercatat tidak dapat dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mencatat nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, ini <b>setter satu arah</b> yang mengabaikan nilai
	 * {@code null}/kosong secara senyap.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna yang terakhir membuat/mengubah baris ini (audit jejak perubahan).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah tercatat
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait lifecycle JPA yang dijalankan tepat sebelum {@code UPDATE}; mendelegasikan pembaruan
	 * stempel waktu ke {@code AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} ditulis pada baris yang sama dengan method ini
	 * (hasil penyuntingan otomatis) sehingga mudah terlewat saat membaca sekilas. Nilai awalnya
	 * adalah waktu objek dibuat di memori, bukan {@code null}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir secara manual. Nilainya akan ditimpa oleh
	 * {@link #onUpdate()} pada operasi {@code UPDATE} berikutnya.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu; untuk objek baru berisi waktu objek dibuat di memori, bukan
	 *         {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks perguruan tinggi ini, yaitu namanya saja.
	 *
	 * <p>Membaca field {@link #nama} <b>mentah</b> dan bukan lewat {@link #getNama()}, sehingga
	 * hasilnya tidak di-trim dan dapat berupa {@code null} — berhati-hatilah bila nilai ini
	 * disambung ke string lain. Membaca field langsung juga membuatnya aman dipanggil pada objek
	 * yang belum sepenuhnya terinisialisasi.</p>
	 *
	 * @return nama perguruan tinggi apa adanya, dapat {@code null}
	 */
	public String toString() {
		return nama;
	}

	/** Kode yayasan/badan penyelenggara (kolom wajib, maks. 50 karakter, <b>tidak unik</b>). */
	private String kodeYayasan;

	/**
	 * Kode perguruan tinggi menurut penomoran Kemdikbud/PDDikti (kolom wajib, maks. 50 karakter,
	 * <b>tidak unik</b> — lihat catatan duplikat pada Javadoc kelas).
	 */
	private String kodePerguruanTinggi;

	/** Nama resmi perguruan tinggi (kolom wajib, maks. 150 karakter). */
	private String nama;

	/** Singkatan/akronim nama perguruan tinggi, mis. untuk tampilan ringkas. */
	private String namaSingkat;

	/** Baris pertama alamat perguruan tinggi (kolom wajib, maks. 150 karakter). */
	private String alamat1;

	/** Baris kedua alamat, untuk alamat yang tidak muat pada {@link #alamat1}. */
	private String alamat2;

	/** Nama dusun pada alamat, bagian dari rincian wilayah administratif. */
	private String dusun;

	/** Nama kelurahan/desa pada alamat. */
	private String kelurahan;

	/** Nomor rukun tetangga pada alamat, disimpan sebagai teks agar nol di depan tidak hilang. */
	private String rt;

	/** Nomor rukun warga pada alamat, disimpan sebagai teks seperti {@link #rt}. */
	private String rw;

	/** Kota/kabupaten kedudukan perguruan tinggi (maks. 50 karakter). */
	private String kota;

	/** Kode pos alamat (maks. 10 karakter). */
	private String kodePos;

	/** Nomor telepon perguruan tinggi (maks. 50 karakter). */
	private String telepon;

	/** Nomor faksimili perguruan tinggi (maks. 50 karakter). */
	private String faksimili;

	/** Tanggal akta pendirian badan hukum penyelenggara. */
	private Date tanggalAkta;

	/** Tanggal awal pendirian perguruan tinggi, dapat berbeda dari {@link #tanggalAkta}. */
	private Date tanggalAwalPendirian;

	/** Nomor akta pendirian (maks. 30 karakter). */
	private String nomorAkta;

	/**
	 * Daftar alamat surel dipisah koma — satu kolom menampung banyak alamat. Dirapikan saat dibaca
	 * oleh {@link #getEmail()} dan ditambah satu per satu oleh {@link #appendEmail(String)}.
	 */
	private String email;

	/** Alamat situs web resmi perguruan tinggi (maks. 150 karakter). */
	private String website;

	/** Nama domain internet perguruan tinggi, terpisah dari {@link #website} yang berupa URL. */
	private String domain;

	/** Motto/semboyan perguruan tinggi. */
	private String motto;

	/** Kode institusi pada SINTA (Science and Technology Index) Kemdikbudristek. */
	private String kodeSinta;

	/** Luas seluruh tanah yang dikuasai perguruan tinggi, dalam meter persegi. */
	private Double luasTanahTotal;

	/** Luas kebun/lahan percobaan, dalam meter persegi. */
	private Double luasKebunLahanPercobaanTotal;

	/** Luas seluruh ruang kuliah, dalam meter persegi. */
	private Double luasTotalRuangKuliah;

	/** Cacah ruang kuliah yang tersedia. */
	private Integer jumlahRuangKuliah;

	/** Luas seluruh laboratorium dan studio, dalam meter persegi. */
	private Double luasTotalLabStudio;

	/** Cacah ruang laboratorium yang tersedia. */
	private Integer jumlahRuangLab;

	/** Luas seluruh ruang kerja dosen tetap, dalam meter persegi. */
	private Double luasTotalRuangDosenTetap;

	/** Luas seluruh ruang administrasi, dalam meter persegi. */
	private Double luasTotalRuangAdministrasi;

	/** Luas seluruh ruang seminar, dalam meter persegi. */
	private Double luasTotalRuangSeminar;

	/** Luas seluruh ruang kegiatan ekstrakurikuler, dalam meter persegi. */
	private Double luasTotalRuangEkskul;

	/** Luas seluruh pusat komputer, dalam meter persegi. */
	private Double luasTotalPusatKomputer;

	/** Luas seluruh ruang perpustakaan, dalam meter persegi. */
	private Double luasTotalRuangPerpustakaan;

	/** Cacah judul buku koleksi perpustakaan (judul berbeda, bukan fisik). */
	private Integer jumlahJudulBuku;

	/** Cacah eksemplar buku koleksi perpustakaan (fisik, biasanya lebih besar dari judul). */
	private Integer jumlahEksemplarBuku;

	/** Uraian bebas mengenai perguruan tinggi (kolom {@code text}). */
	private String deskripsi;

	/** Nomor surat keputusan izin operasional perguruan tinggi. */
	private String skIzinOperasi;

	/** Tanggal surat keputusan izin operasional. */
	private Date tglSkIzinOperasi;

	/** Nama pejabat yang menerbitkan surat keputusan izin operasional. */
	private String pejabatIzinOperasi;

	/** Nomor rekening bank resmi perguruan tinggi. */
	private String noRek;

	/** Nama bank tempat rekening {@link #noRek} dibuka. */
	private String nmBank;

	/** Unit/cabang bank tempat rekening dibuka. */
	private String unitCabang;

	/** Nama pemilik rekening sebagaimana tercatat di bank. */
	private String nmRek;

	/** Luas tanah berstatus milik sendiri, dalam meter persegi. */
	private Double luasTanahMilik;

	/** Luas tanah berstatus bukan milik (sewa/pinjam pakai), dalam meter persegi. */
	private Double luasTanahBukanMilik;

	/**
	 * Tahun pertama perguruan tinggi menerima mahasiswa. Perhatikan bahwa getter-nya mengganti
	 * {@code null} dengan {@code 0} yang bukan tahun sah — lihat
	 * {@link #getTahunPertamaMenerimaMahasiswa()}.
	 */
	private Integer tahunPertamaMenerimaMahasiswa;

	/** Peringkat akreditasi institusi (mis. A/B/C atau Unggul/Baik Sekali/Baik). */
	private String peringkatAkreditasi;

	/**
	 * Keterangan akreditasi tambahan. Perannya tumpang tindih dengan
	 * {@link #peringkatAkreditasi} dan tidak dibedakan di lapisan model.
	 */
	private String akreditasi;

	/** Nomor surat keputusan akreditasi BAN-PT. */
	private String noSkAkreditasi;

	/** Tanggal penetapan akreditasi; masa berlakunya tidak disimpan sebagai kolom tersendiri. */
	private Date tanggalAkreditasi;

	/**
	 * Penanda baris masih dipakai. Bernilai baku {@code true} bila kolomnya {@code null} — lihat
	 * {@link #getAktif()}.
	 */
	private Boolean aktif;

	/** Nama rektor/pimpinan perguruan tinggi yang menjabat. */
	private String rektor;

	/** NIP atau nomor induk rektor {@link #rektor}. */
	private String rektorNip;

	/** Id/kode perguruan tinggi ini pada sistem feeder PDDikti, bila hasil impor dari feeder. */
	private String feeder;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate. Seluruh field dibiarkan pada nilai
	 * awalnya; nilai baku pengganti baru muncul saat getter masing-masing dipanggil.
	 */
	public PerguruanTinggiLain() {
	}

	/**
	 * Kunci utama baris perguruan tinggi eksternal.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan ditandai {@code insertable = false}, jadi
	 * nilai yang disetel manual tidak ikut pada {@code INSERT}.</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris. Umumnya hanya dipakai Hibernate atau kode yang menyusun objek
	 * detached.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode yayasan/badan penyelenggara apa adanya (tanpa trim).
	 *
	 * <p>Kolomnya wajib di basis data tetapi <b>tidak unik</b>, dan setter-nya tidak memvalidasi
	 * apa pun — dua baris dapat memakai kode yayasan yang sama.</p>
	 *
	 * @return kode yayasan, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_yayasan", nullable = false, length = 50)
	public String getKodeYayasan() {
		return this.kodeYayasan;
	}

	/**
	 * Menetapkan kode yayasan/badan penyelenggara. Tidak divalidasi terhadap batas 50 karakter
	 * maupun terhadap keberadaan yayasan tersebut.
	 *
	 * @param kodeYayasan kode yayasan
	 */
	public void setKodeYayasan(String kodeYayasan) {
		this.kodeYayasan = kodeYayasan;
	}

	/**
	 * Mengembalikan kode perguruan tinggi menurut penomoran Kemdikbud/PDDikti, apa adanya.
	 *
	 * <p><b>Bukan pengenal unik.</b> Meski kolomnya wajib, tidak ada batasan unik di basis data dan
	 * tidak ada penjagaan tabrakan di lapisan model. Kode yang mencari satu baris berdasarkan nilai
	 * ini harus bersiap menerima lebih dari satu hasil — lebih-lebih karena impor feeder PDDikti
	 * juga menulis ke tabel yang sama.</p>
	 *
	 * @return kode perguruan tinggi, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_perguruan_tinggi", nullable = false, length = 50)
	public String getKodePerguruanTinggi() {
		return this.kodePerguruanTinggi;
	}

	/**
	 * Menetapkan kode perguruan tinggi. Tidak ada pemeriksaan duplikat terhadap baris lain.
	 *
	 * @param kodePerguruanTinggi kode perguruan tinggi
	 */
	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	/**
	 * Mengembalikan nama resmi perguruan tinggi tanpa spasi di kedua ujungnya.
	 *
	 * <p>Berbeda dari {@link #toString()} yang membaca field mentah, method ini melakukan
	 * {@code trim()} — jadi keduanya dapat menghasilkan string berbeda untuk objek yang sama.</p>
	 *
	 * @return nama yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama resmi perguruan tinggi. Disimpan apa adanya; trim baru terjadi saat dibaca.
	 *
	 * @param nama nama resmi
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan baris pertama alamat, sudah di-trim.
	 *
	 * <p><b>Mengganti {@code null} dengan string kosong</b> — berbeda dari {@link #getNama()} yang
	 * meneruskan {@code null}. Pemanggil karena itu tidak dapat membedakan alamat yang belum diisi
	 * dari alamat yang memang kosong.</p>
	 *
	 * @return baris pertama alamat; tidak pernah {@code null}
	 */
	@Column(name = "alamat1", nullable = false, length = 150)
	public String getAlamat1() {
		return this.alamat1 == null ? "" : this.alamat1.trim();
	}

	/**
	 * Menetapkan baris pertama alamat perguruan tinggi.
	 *
	 * @param alamat1 baris pertama alamat
	 */
	public void setAlamat1(String alamat1) {
		this.alamat1 = alamat1;
	}

	/**
	 * Mengembalikan baris kedua alamat, sudah di-trim dan dengan {@code null} diganti string
	 * kosong seperti {@link #getAlamat1()}.
	 *
	 * @return baris kedua alamat; tidak pernah {@code null}
	 */
	@Column(name = "alamat2", length = 150)
	public String getAlamat2() {
		return this.alamat2 == null ? "" : this.alamat2.trim();
	}

	/**
	 * Menetapkan baris kedua alamat perguruan tinggi.
	 *
	 * @param alamat2 baris kedua alamat
	 */
	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	/**
	 * Mengembalikan kota/kabupaten kedudukan perguruan tinggi, apa adanya.
	 *
	 * @return nama kota, atau {@code null} bila belum diisi
	 */
	@Column(name = "kota", length = 50)
	public String getKota() {
		return this.kota;
	}

	/**
	 * Menetapkan kota/kabupaten kedudukan. Teks bebas, tidak dicocokkan ke tabel wilayah mana pun.
	 *
	 * @param kota nama kota/kabupaten
	 */
	public void setKota(String kota) {
		this.kota = kota;
	}

	/**
	 * Mengembalikan kode pos alamat, apa adanya.
	 *
	 * @return kode pos, atau {@code null} bila belum diisi
	 */
	@Column(name = "kode_pos", length = 10)
	public String getKodePos() {
		return this.kodePos;
	}

	/**
	 * Menetapkan kode pos alamat. Disimpan sebagai teks agar nol di depan tidak hilang.
	 *
	 * @param kodePos kode pos
	 */
	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	/**
	 * Mengembalikan nomor telepon perguruan tinggi, apa adanya.
	 *
	 * @return nomor telepon, atau {@code null} bila belum diisi
	 */
	@Column(name = "telepon", length = 50)
	public String getTelepon() {
		return this.telepon;
	}

	/**
	 * Menetapkan nomor telepon perguruan tinggi, tanpa normalisasi format.
	 *
	 * @param telepon nomor telepon
	 */
	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	/**
	 * Mengembalikan nomor faksimili perguruan tinggi, apa adanya.
	 *
	 * @return nomor faksimili, atau {@code null} bila belum diisi
	 */
	@Column(name = "faksimili", length = 50)
	public String getFaksimili() {
		return this.faksimili;
	}

	/**
	 * Menetapkan nomor faksimili perguruan tinggi.
	 *
	 * @param faksimili nomor faksimili
	 */
	public void setFaksimili(String faksimili) {
		this.faksimili = faksimili;
	}

	/**
	 * Mengembalikan tanggal akta pendirian badan hukum penyelenggara.
	 *
	 * <p>Dipetakan sebagai {@code DATE} (tanpa komponen jam). Referensi {@link Date} dikembalikan
	 * langsung tanpa penyalinan, jadi mengubahnya lewat {@code setTime()} ikut mengubah state
	 * entity.</p>
	 *
	 * @return tanggal akta, atau {@code null} bila belum diisi
	 * @see #getTanggalAwalPendirian()
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_akta", length = 0)
	public Date getTanggalAkta() {
		return this.tanggalAkta;
	}

	/**
	 * Menetapkan tanggal akta pendirian. Referensi disimpan langsung tanpa penyalinan defensif.
	 *
	 * @param tanggalAkta tanggal akta
	 */
	public void setTanggalAkta(Date tanggalAkta) {
		this.tanggalAkta = tanggalAkta;
	}

	/**
	 * Mengembalikan tanggal awal pendirian perguruan tinggi.
	 *
	 * <p>Dapat berbeda dari {@link #getTanggalAkta()}: akta menandai pendirian badan hukum
	 * penyelenggara, sedangkan nilai ini menandai mulai berdirinya perguruan tingginya. Urutan
	 * keduanya tidak divalidasi.</p>
	 *
	 * @return tanggal awal pendirian, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_awal_pendirian", length = 0)
	public Date getTanggalAwalPendirian() {
		return this.tanggalAwalPendirian;
	}

	/**
	 * Menetapkan tanggal awal pendirian perguruan tinggi.
	 *
	 * @param tanggalAwalPendirian tanggal awal pendirian
	 */
	public void setTanggalAwalPendirian(Date tanggalAwalPendirian) {
		this.tanggalAwalPendirian = tanggalAwalPendirian;
	}

	/**
	 * Mengembalikan nomor akta pendirian, apa adanya.
	 *
	 * @return nomor akta, atau {@code null} bila belum diisi
	 */
	@Column(name = "nomor_akta", length = 30)
	public String getNomorAkta() {
		return this.nomorAkta;
	}

	/**
	 * Menetapkan nomor akta pendirian.
	 *
	 * @param nomorAkta nomor akta
	 */
	public void setNomorAkta(String nomorAkta) {
		this.nomorAkta = nomorAkta;
	}

	/**
	 * Mengembalikan daftar alamat surel perguruan tinggi (dipisah koma) setelah
	 * <b>merapikannya</b>.
	 *
	 * <p><b>Peringatan — getter ini mengubah state.</b> Method ini menulis balik ke field
	 * {@link #email}: koma ganda {@code ",,"} dimampatkan (maksimal lima iterasi), {@code null}
	 * diganti string kosong, dan nilai yang hanya berisi {@code ","} dikosongkan. Pada objek yang
	 * dikelola Hibernate, sekadar membaca properti ini dapat menandai entity kotor dan memicu
	 * {@code UPDATE} beserta revisi Envers palsu.</p>
	 *
	 * <p>Batas lima iterasi membuat perapian tidak tuntas untuk rentetan koma yang sangat panjang.
	 * Implementasi ini identik dengan {@code Karyawan.getEmail()} — salinan pola yang sama, jadi
	 * perbaikan pada salah satunya tidak merambat ke yang lain.</p>
	 *
	 * @return daftar surel dipisah koma; string kosong bila tidak ada (tidak pernah {@code null}
	 *         setelah pemanggilan pertama)
	 * @see #appendEmail(String)
	 */
	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	/**
	 * Mengganti <b>seluruh</b> daftar surel dengan nilai baru, tanpa validasi.
	 *
	 * <p>Berbeda dengan {@link #appendEmail(String)} yang menambah satu alamat, method ini menimpa
	 * daftar yang ada; {@code null} maupun string kosong diterima dan menghapus seluruh alamat.</p>
	 *
	 * @param email satu alamat surel, atau beberapa alamat dipisah koma
	 * @see #appendEmail(String)
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Menambahkan satu alamat surel ke daftar tanpa menghapus alamat yang sudah ada.
	 *
	 * <p>Tiga penjagaan diterapkan sebelum alamat ditambahkan:</p>
	 * <ol>
	 *   <li><b>Uji duplikat</b> memakai {@code StringUtils.contains} (substring), bukan pencocokan
	 *       per elemen — sehingga {@code "adi@x.ac.id"} dianggap sudah ada bila daftar memuat
	 *       {@code "wahyuadi@x.ac.id"}, dan alamat itu diam-diam tidak jadi ditambahkan.</li>
	 *   <li><b>Uji format</b> lewat {@code Common.isValidEmailAddress()}.</li>
	 *   <li><b>Uji domain telanjang:</b> alamat yang diawali {@code "@"} ditolak.</li>
	 * </ol>
	 * <p>Seluruh penolakan bersifat senyap — tidak ada nilai kembalian maupun exception, jadi
	 * pemanggil tidak dapat membedakan alamat yang berhasil ditambahkan dari yang ditolak.
	 * Perhatikan pula bahwa kolomnya dibatasi 255 karakter sementara method ini terus menyambung
	 * tanpa memeriksa panjang, sehingga penambahan berulang akhirnya gagal saat {@code flush}.</p>
	 *
	 * <p>Implementasi ini identik dengan {@code Karyawan.appendEmail(String)}.</p>
	 *
	 * @param email alamat surel yang hendak ditambahkan; diabaikan bila {@code null}, kosong, tidak
	 *              valid, diawali {@code "@"}, atau sudah termuat sebagai substring
	 * @see #setEmail(String)
	 */
	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	/**
	 * Mengembalikan alamat situs web resmi perguruan tinggi, apa adanya.
	 *
	 * <p>Tidak divalidasi sebagai URL dan tidak dinormalkan (skema {@code http://} bisa ada atau
	 * tidak), jadi pemanggil yang hendak menautkannya perlu memeriksanya sendiri. Bandingkan dengan
	 * {@link #getDomain()} yang menyimpan nama domain saja.</p>
	 *
	 * @return alamat situs web, atau {@code null} bila belum diisi
	 */
	@Column(name = "website", length = 150)
	public String getWebsite() {
		return this.website;
	}

	/**
	 * Menetapkan alamat situs web resmi perguruan tinggi, tanpa validasi format.
	 *
	 * @param website alamat situs web
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * Mengembalikan luas seluruh tanah yang dikuasai perguruan tinggi.
	 *
	 * <p>Bagian dari kelompok data sarana-prasarana untuk pelaporan EPSBED/PDDikti. Seluruh getter
	 * kelompok ini murni (tanpa efek samping) dan dapat mengembalikan {@code null} bila belum
	 * diisi — lakukan penjagaan {@code null} sebelum meng-unbox atau menjumlahkan.</p>
	 *
	 * <p>Nilai ini tidak diverifikasi terhadap {@link #getLuasTanahMilik()} dan
	 * {@link #getLuasTanahBukanMilik()}; jumlah keduanya tidak harus sama dengan nilai ini.</p>
	 *
	 * @return luas tanah total dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_tanah_total")
	public Double getLuasTanahTotal() {
		return luasTanahTotal;
	}

	/**
	 * Menetapkan luas seluruh tanah yang dikuasai perguruan tinggi. Nilai negatif tidak ditolak.
	 *
	 * @param luasTanahTotal luas dalam meter persegi
	 */
	public void setLuasTanahTotal(Double luasTanahTotal) {
		this.luasTanahTotal = luasTanahTotal;
	}

	/**
	 * Mengembalikan luas kebun/lahan percobaan.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_kebun_lahan_percobaan_total")
	public Double getLuasKebunLahanPercobaanTotal() {
		return luasKebunLahanPercobaanTotal;
	}

	/**
	 * Menetapkan luas kebun/lahan percobaan.
	 *
	 * @param luasKebunLahanPercobaanTotal luas dalam meter persegi
	 */
	public void setLuasKebunLahanPercobaanTotal(Double luasKebunLahanPercobaanTotal) {
		this.luasKebunLahanPercobaanTotal = luasKebunLahanPercobaanTotal;
	}

	/**
	 * Mengembalikan luas seluruh ruang kuliah.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 * @see #getJumlahRuangKuliah()
	 */
	@Column(name = "luas_total_ruang_kuliah")
	public Double getLuasTotalRuangKuliah() {
		return luasTotalRuangKuliah;
	}

	/**
	 * Menetapkan luas seluruh ruang kuliah.
	 *
	 * @param luasTotalRuangKuliah luas dalam meter persegi
	 */
	public void setLuasTotalRuangKuliah(Double luasTotalRuangKuliah) {
		this.luasTotalRuangKuliah = luasTotalRuangKuliah;
	}

	/**
	 * Mengembalikan cacah ruang kuliah yang tersedia.
	 *
	 * @return jumlah ruang kuliah, atau {@code null} bila belum diisi
	 * @see #getLuasTotalRuangKuliah()
	 */
	@Column(name = "jumlah_ruang_kuliah")
	public Integer getJumlahRuangKuliah() {
		return jumlahRuangKuliah;
	}

	/**
	 * Menetapkan cacah ruang kuliah yang tersedia.
	 *
	 * @param jumlahRuangKuliah jumlah ruang kuliah
	 */
	public void setJumlahRuangKuliah(Integer jumlahRuangKuliah) {
		this.jumlahRuangKuliah = jumlahRuangKuliah;
	}

	/**
	 * Mengembalikan luas seluruh laboratorium dan studio.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 * @see #getJumlahRuangLab()
	 */
	@Column(name = "luas_total_lab_studio")
	public Double getLuasTotalLabStudio() {
		return luasTotalLabStudio;
	}

	/**
	 * Menetapkan luas seluruh laboratorium dan studio.
	 *
	 * @param luasTotalLabStudio luas dalam meter persegi
	 */
	public void setLuasTotalLabStudio(Double luasTotalLabStudio) {
		this.luasTotalLabStudio = luasTotalLabStudio;
	}

	/**
	 * Mengembalikan cacah ruang laboratorium yang tersedia.
	 *
	 * @return jumlah ruang laboratorium, atau {@code null} bila belum diisi
	 */
	@Column(name = "jumlah_ruang_lab")
	public Integer getJumlahRuangLab() {
		return jumlahRuangLab;
	}

	/**
	 * Menetapkan cacah ruang laboratorium yang tersedia.
	 *
	 * @param jumlahRuangLab jumlah ruang laboratorium
	 */
	public void setJumlahRuangLab(Integer jumlahRuangLab) {
		this.jumlahRuangLab = jumlahRuangLab;
	}

	/**
	 * Mengembalikan luas seluruh ruang kerja dosen tetap.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_total_ruang_dosen_tetap")
	public Double getLuasTotalRuangDosenTetap() {
		return luasTotalRuangDosenTetap;
	}

	/**
	 * Menetapkan luas seluruh ruang kerja dosen tetap.
	 *
	 * @param luasTotalRuangDosenTetap luas dalam meter persegi
	 */
	public void setLuasTotalRuangDosenTetap(Double luasTotalRuangDosenTetap) {
		this.luasTotalRuangDosenTetap = luasTotalRuangDosenTetap;
	}

	/**
	 * Mengembalikan luas seluruh ruang administrasi.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_total_ruang_administrasi")
	public Double getLuasTotalRuangAdministrasi() {
		return luasTotalRuangAdministrasi;
	}

	/**
	 * Menetapkan luas seluruh ruang administrasi.
	 *
	 * @param luasTotalRuangAdministrasi luas dalam meter persegi
	 */
	public void setLuasTotalRuangAdministrasi(Double luasTotalRuangAdministrasi) {
		this.luasTotalRuangAdministrasi = luasTotalRuangAdministrasi;
	}

	/**
	 * Mengembalikan luas seluruh ruang seminar.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_total_ruang_seminar")
	public Double getLuasTotalRuangSeminar() {
		return luasTotalRuangSeminar;
	}

	/**
	 * Menetapkan luas seluruh ruang seminar.
	 *
	 * @param luasTotalRuangSeminar luas dalam meter persegi
	 */
	public void setLuasTotalRuangSeminar(Double luasTotalRuangSeminar) {
		this.luasTotalRuangSeminar = luasTotalRuangSeminar;
	}

	/**
	 * Mengembalikan luas seluruh ruang kegiatan ekstrakurikuler.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_total_ruang_ekskul")
	public Double getLuasTotalRuangEkskul() {
		return luasTotalRuangEkskul;
	}

	/**
	 * Menetapkan luas seluruh ruang kegiatan ekstrakurikuler.
	 *
	 * @param luasTotalRuangEkskul luas dalam meter persegi
	 */
	public void setLuasTotalRuangEkskul(Double luasTotalRuangEkskul) {
		this.luasTotalRuangEkskul = luasTotalRuangEkskul;
	}

	/**
	 * Mengembalikan luas seluruh pusat komputer.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	@Column(name = "luas_total_pusat_komputer")
	public Double getLuasTotalPusatKomputer() {
		return luasTotalPusatKomputer;
	}

	/**
	 * Menetapkan luas seluruh pusat komputer.
	 *
	 * @param luasTotalPusatKomputer luas dalam meter persegi
	 */
	public void setLuasTotalPusatKomputer(Double luasTotalPusatKomputer) {
		this.luasTotalPusatKomputer = luasTotalPusatKomputer;
	}

	/**
	 * Mengembalikan luas seluruh ruang perpustakaan.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 * @see #getJumlahJudulBuku()
	 */
	@Column(name = "luas_total_ruang_perpustakaan")
	public Double getLuasTotalRuangPerpustakaan() {
		return luasTotalRuangPerpustakaan;
	}

	/**
	 * Menetapkan luas seluruh ruang perpustakaan.
	 *
	 * @param luasTotalRuangPerpustakaan luas dalam meter persegi
	 */
	public void setLuasTotalRuangPerpustakaan(Double luasTotalRuangPerpustakaan) {
		this.luasTotalRuangPerpustakaan = luasTotalRuangPerpustakaan;
	}

	/**
	 * Mengembalikan cacah <b>judul</b> buku koleksi perpustakaan.
	 *
	 * <p>Berbeda dari {@link #getJumlahEksemplarBuku()} yang menghitung fisik buku: satu judul
	 * dapat memiliki banyak eksemplar, sehingga nilai ini lazimnya lebih kecil. Kedua angka tidak
	 * saling divalidasi.</p>
	 *
	 * @return jumlah judul buku, atau {@code null} bila belum diisi
	 */
	@Column(name = "jumlah_judul_buku")
	public Integer getJumlahJudulBuku() {
		return jumlahJudulBuku;
	}

	/**
	 * Menetapkan cacah judul buku koleksi perpustakaan.
	 *
	 * @param jumlahJudulBuku jumlah judul buku
	 */
	public void setJumlahJudulBuku(Integer jumlahJudulBuku) {
		this.jumlahJudulBuku = jumlahJudulBuku;
	}

	/**
	 * Mengembalikan cacah <b>eksemplar</b> (fisik) buku koleksi perpustakaan.
	 *
	 * @return jumlah eksemplar buku, atau {@code null} bila belum diisi
	 * @see #getJumlahJudulBuku()
	 */
	@Column(name = "jumlah_eksemplar_buku")
	public Integer getJumlahEksemplarBuku() {
		return jumlahEksemplarBuku;
	}

	/**
	 * Menetapkan cacah eksemplar buku koleksi perpustakaan.
	 *
	 * @param jumlahEksemplarBuku jumlah eksemplar buku
	 */
	public void setJumlahEksemplarBuku(Integer jumlahEksemplarBuku) {
		this.jumlahEksemplarBuku = jumlahEksemplarBuku;
	}

	/**
	 * Mengembalikan uraian bebas mengenai perguruan tinggi.
	 *
	 * <p><b>Getter ini mengubah state:</b> bila kolomnya {@code null}, field diisi string kosong
	 * dan penugasan itu bertahan pada objek — pada entity yang dikelola Hibernate hal ini dapat
	 * menandainya kotor dan memicu {@code UPDATE} beserta revisi Envers palsu meski isinya tidak
	 * berubah secara berarti. Ini instance dari pola getter-mutasi-field pada model AIS.</p>
	 *
	 * @return uraian bebas; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = "";
		}
		return deskripsi;
	}

	/**
	 * Menetapkan uraian bebas mengenai perguruan tinggi. Tidak dibatasi panjang maupun disaring.
	 *
	 * @param deskripsi uraian bebas
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Id/kode perguruan tinggi ini pada sistem feeder PDDikti, bila hasil impor dari feeder.
	 *
	 * <p><b>Arah pengganti terbalik dari getter lain di kelas ini:</b> nilai kosong dinormalkan
	 * menjadi {@code null} (bukan sebaliknya), sehingga {@code null} berarti "bukan hasil impor
	 * feeder" dan dapat dipakai sebagai penanda asal data. Bandingkan dengan
	 * {@link #getKodeSinta()} yang justru mengubah {@code null} menjadi string kosong.</p>
	 *
	 * <p>Nilai ini tidak dijamin unik antar baris.</p>
	 *
	 * @return kode feeder yang sudah di-trim, atau {@code null} bila baris bukan hasil impor feeder
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Menetapkan id/kode perguruan tinggi pada sistem feeder PDDikti.
	 *
	 * @param feeder kode feeder; {@code null}/kosong menandai baris sebagai bukan hasil impor
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Mengembalikan singkatan/akronim nama perguruan tinggi, apa adanya (tanpa trim).
	 *
	 * @return singkatan nama, atau {@code null} bila belum diisi
	 * @see #getNama()
	 */
	public String getNamaSingkat() {
		return namaSingkat;
	}

	/**
	 * Menetapkan singkatan/akronim nama perguruan tinggi.
	 *
	 * @param namaSingkat singkatan nama
	 */
	public void setNamaSingkat(String namaSingkat) {
		this.namaSingkat = namaSingkat;
	}

	/**
	 * Mengembalikan nama dusun pada alamat perguruan tinggi.
	 *
	 * @return nama dusun, atau {@code null} bila belum diisi
	 */
	public String getDusun() {
		return dusun;
	}

	/**
	 * Menetapkan nama dusun pada alamat perguruan tinggi.
	 *
	 * @param dusun nama dusun
	 */
	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	/**
	 * Mengembalikan nomor rukun tetangga pada alamat.
	 *
	 * @return nomor RT sebagai teks, atau {@code null} bila belum diisi
	 */
	public String getRt() {
		return rt;
	}

	/**
	 * Menetapkan nomor rukun tetangga pada alamat. Disimpan sebagai teks agar nol di depan tidak
	 * hilang.
	 *
	 * @param rt nomor RT
	 */
	public void setRt(String rt) {
		this.rt = rt;
	}

	/**
	 * Mengembalikan nomor rukun warga pada alamat.
	 *
	 * @return nomor RW sebagai teks, atau {@code null} bila belum diisi
	 */
	public String getRw() {
		return rw;
	}

	/**
	 * Menetapkan nomor rukun warga pada alamat, disimpan sebagai teks seperti {@link #setRt(String)}.
	 *
	 * @param rw nomor RW
	 */
	public void setRw(String rw) {
		this.rw = rw;
	}

	/**
	 * Mengembalikan nama kelurahan/desa pada alamat.
	 *
	 * @return nama kelurahan, atau {@code null} bila belum diisi
	 */
	public String getKelurahan() {
		return kelurahan;
	}

	/**
	 * Menetapkan nama kelurahan/desa pada alamat. Teks bebas, tidak dicocokkan ke tabel wilayah.
	 *
	 * @param kelurahan nama kelurahan/desa
	 */
	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	/**
	 * Mengembalikan nomor surat keputusan izin operasional perguruan tinggi.
	 *
	 * @return nomor SK izin operasional, atau {@code null} bila belum diisi
	 * @see #getTglSkIzinOperasi()
	 * @see #getPejabatIzinOperasi()
	 */
	public String getSkIzinOperasi() {
		return skIzinOperasi;
	}

	/**
	 * Menetapkan nomor surat keputusan izin operasional.
	 *
	 * @param skIzinOperasi nomor SK izin operasional
	 */
	public void setSkIzinOperasi(String skIzinOperasi) {
		this.skIzinOperasi = skIzinOperasi;
	}

	/**
	 * Mengembalikan tanggal surat keputusan izin operasional.
	 *
	 * <p>Dipetakan sebagai {@code DATE} (tanpa komponen jam). Masa berlaku izin tidak disimpan
	 * sebagai kolom tersendiri, jadi kedaluwarsanya tidak dapat disimpulkan dari entity ini.</p>
	 *
	 * @return tanggal SK izin operasional, atau {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTglSkIzinOperasi() {
		return tglSkIzinOperasi;
	}

	/**
	 * Menetapkan tanggal surat keputusan izin operasional.
	 *
	 * @param tglSkIzinOperasi tanggal SK izin operasional
	 */
	public void setTglSkIzinOperasi(Date tglSkIzinOperasi) {
		this.tglSkIzinOperasi = tglSkIzinOperasi;
	}

	/**
	 * Mengembalikan nomor rekening bank resmi perguruan tinggi.
	 *
	 * <p>Disimpan sebagai teks biasa tanpa penyamaran maupun penyandian, dan entity ini tidak punya
	 * penyaring kepemilikan — jangan tampilkan nilainya pada antarmuka publik tanpa pertimbangan.</p>
	 *
	 * @return nomor rekening, atau {@code null} bila belum diisi
	 * @see #getNmBank()
	 * @see #getNmRek()
	 */
	public String getNoRek() {
		return noRek;
	}

	/**
	 * Menetapkan nomor rekening bank resmi perguruan tinggi, tanpa validasi format.
	 *
	 * @param noRek nomor rekening
	 */
	public void setNoRek(String noRek) {
		this.noRek = noRek;
	}

	/**
	 * Mengembalikan nama bank tempat rekening {@link #getNoRek()} dibuka.
	 *
	 * @return nama bank, atau {@code null} bila belum diisi
	 */
	public String getNmBank() {
		return nmBank;
	}

	/**
	 * Menetapkan nama bank tempat rekening dibuka. Teks bebas, bukan relasi ke master bank.
	 *
	 * @param nmBank nama bank
	 */
	public void setNmBank(String nmBank) {
		this.nmBank = nmBank;
	}

	/**
	 * Mengembalikan unit/cabang bank tempat rekening dibuka.
	 *
	 * @return unit/cabang bank, atau {@code null} bila belum diisi
	 */
	public String getUnitCabang() {
		return unitCabang;
	}

	/**
	 * Menetapkan unit/cabang bank tempat rekening dibuka.
	 *
	 * @param unitCabang unit/cabang bank
	 */
	public void setUnitCabang(String unitCabang) {
		this.unitCabang = unitCabang;
	}

	/**
	 * Mengembalikan nama pemilik rekening sebagaimana tercatat di bank.
	 *
	 * <p>Tidak harus sama dengan {@link #getNama()}; sebagian institusi memakai nama yayasan atau
	 * nama bendahara pada rekeningnya. Kesesuaian keduanya tidak diperiksa.</p>
	 *
	 * @return nama pemilik rekening, atau {@code null} bila belum diisi
	 */
	public String getNmRek() {
		return nmRek;
	}

	/**
	 * Menetapkan nama pemilik rekening.
	 *
	 * @param nmRek nama pemilik rekening
	 */
	public void setNmRek(String nmRek) {
		this.nmRek = nmRek;
	}

	/**
	 * Mengembalikan luas tanah berstatus milik sendiri.
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 * @see #getLuasTanahBukanMilik()
	 * @see #getLuasTanahTotal()
	 */
	public Double getLuasTanahMilik() {
		return luasTanahMilik;
	}

	/**
	 * Menetapkan luas tanah berstatus milik sendiri.
	 *
	 * @param luasTanahMilik luas dalam meter persegi
	 */
	public void setLuasTanahMilik(Double luasTanahMilik) {
		this.luasTanahMilik = luasTanahMilik;
	}

	/**
	 * Mengembalikan luas tanah berstatus bukan milik (sewa/pinjam pakai).
	 *
	 * <p>Penjumlahan nilai ini dengan {@link #getLuasTanahMilik()} tidak diwajibkan sama dengan
	 * {@link #getLuasTanahTotal()}; ketiganya diisi terpisah dan tidak saling divalidasi.</p>
	 *
	 * @return luas dalam meter persegi, atau {@code null} bila belum diisi
	 */
	public Double getLuasTanahBukanMilik() {
		return luasTanahBukanMilik;
	}

	/**
	 * Menetapkan luas tanah berstatus bukan milik.
	 *
	 * @param luasTanahBukanMilik luas dalam meter persegi
	 */
	public void setLuasTanahBukanMilik(Double luasTanahBukanMilik) {
		this.luasTanahBukanMilik = luasTanahBukanMilik;
	}

	/**
	 * Mengembalikan penanda baris masih dipakai.
	 *
	 * <p><b>Baku aktif:</b> kolom {@code null} dibaca sebagai {@code true}, jadi baris lama —
	 * termasuk hasil impor feeder yang tidak mengisi kolom ini — ikut dianggap aktif. Penggantian
	 * tersebut tidak ditulis balik ke field.</p>
	 *
	 * <p>Penyaringan berdasarkan penanda ini adalah tanggung jawab pemanggil; lapisan model tidak
	 * menghalangi pemakaian baris yang tidak aktif.</p>
	 *
	 * @return {@code true} bila baris masih dipakai; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda baris masih dipakai.
	 *
	 * @param aktif {@code false} untuk menonaktifkan; {@code null} dibaca kembali sebagai
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nama pejabat yang menerbitkan surat keputusan izin operasional.
	 *
	 * @return nama pejabat, atau {@code null} bila belum diisi
	 * @see #getSkIzinOperasi()
	 */
	public String getPejabatIzinOperasi() {
		return pejabatIzinOperasi;
	}

	/**
	 * Menetapkan nama pejabat penerbit surat keputusan izin operasional.
	 *
	 * @param pejabatIzinOperasi nama pejabat
	 */
	public void setPejabatIzinOperasi(String pejabatIzinOperasi) {
		this.pejabatIzinOperasi = pejabatIzinOperasi;
	}

	/**
	 * Mengembalikan tahun pertama perguruan tinggi menerima mahasiswa.
	 *
	 * <p><b>Peringatan:</b> nilai {@code null} diganti dengan {@code 0}, yang <i>bukan</i> tahun
	 * yang sah. Pemanggil tidak dapat membedakan "belum diisi" dari data yang benar, dan angka
	 * {@code 0} yang ikut masuk ke perhitungan umur institusi atau ke pelaporan akan menghasilkan
	 * nilai yang keliru. Periksa {@code 0} secara eksplisit sebelum memakainya.</p>
	 *
	 * @return tahun pertama menerima mahasiswa, atau {@code 0} bila belum diisi
	 */
	public Integer getTahunPertamaMenerimaMahasiswa() {
		return tahunPertamaMenerimaMahasiswa == null ? 0 : tahunPertamaMenerimaMahasiswa;
	}

	/**
	 * Menetapkan tahun pertama perguruan tinggi menerima mahasiswa. Kewajaran tahunnya tidak
	 * divalidasi.
	 *
	 * @param tahunPertamaMenerimaMahasiswa tahun dalam format empat digit
	 */
	public void setTahunPertamaMenerimaMahasiswa(Integer tahunPertamaMenerimaMahasiswa) {
		this.tahunPertamaMenerimaMahasiswa = tahunPertamaMenerimaMahasiswa;
	}

	/**
	 * Mengembalikan peringkat akreditasi institusi.
	 *
	 * <p>Teks bebas — tidak dibatasi ke daftar nilai tertentu, sehingga isinya dapat berupa
	 * {@code "A"}/{@code "B"}/{@code "C"} maupun {@code "Unggul"}/{@code "Baik Sekali"} tergantung
	 * sumber datanya. Perannya tumpang tindih dengan {@link #getAkreditasi()} dan keduanya tidak
	 * dibedakan di lapisan model, jadi periksa keduanya bila mencari peringkat akreditasi.</p>
	 *
	 * @return peringkat akreditasi, atau {@code null} bila belum diisi
	 */
	public String getPeringkatAkreditasi() {
		return peringkatAkreditasi;
	}

	/**
	 * Menetapkan peringkat akreditasi institusi.
	 *
	 * @param peringkatAkreditasi peringkat akreditasi
	 */
	public void setPeringkatAkreditasi(String peringkatAkreditasi) {
		this.peringkatAkreditasi = peringkatAkreditasi;
	}

	/**
	 * Mengembalikan keterangan akreditasi.
	 *
	 * <p>Kolom kedua bagi informasi akreditasi yang perannya tumpang tindih dengan
	 * {@link #getPeringkatAkreditasi()}; pembagian isi antara keduanya bergantung pada kebiasaan
	 * pengisi data, bukan pada aturan yang ditegakkan kode.</p>
	 *
	 * @return keterangan akreditasi, atau {@code null} bila belum diisi
	 */
	public String getAkreditasi() {
		return akreditasi;
	}

	/**
	 * Menetapkan keterangan akreditasi.
	 *
	 * @param akreditasi keterangan akreditasi
	 */
	public void setAkreditasi(String akreditasi) {
		this.akreditasi = akreditasi;
	}

	/**
	 * Mengembalikan nomor surat keputusan akreditasi BAN-PT.
	 *
	 * @return nomor SK akreditasi, atau {@code null} bila belum diisi
	 * @see #getTanggalAkreditasi()
	 */
	public String getNoSkAkreditasi() {
		return noSkAkreditasi;
	}

	/**
	 * Menetapkan nomor surat keputusan akreditasi BAN-PT.
	 *
	 * @param noSkAkreditasi nomor SK akreditasi
	 */
	public void setNoSkAkreditasi(String noSkAkreditasi) {
		this.noSkAkreditasi = noSkAkreditasi;
	}

	/**
	 * Mengembalikan tanggal penetapan akreditasi.
	 *
	 * <p>Tidak beranotasi {@code @Temporal}, berbeda dari {@link #getTanggalAkta()} dan
	 * {@link #getTglSkIzinOperasi()} yang dipetakan {@code DATE} — jadi kolom ini menyimpan
	 * komponen jam juga. Masa berlaku akreditasi (lazimnya lima tahun) tidak disimpan, sehingga
	 * kedaluwarsanya harus dihitung pemanggil.</p>
	 *
	 * @return tanggal penetapan akreditasi, atau {@code null} bila belum diisi
	 */
	public Date getTanggalAkreditasi() {
		return tanggalAkreditasi;
	}

	/**
	 * Menetapkan tanggal penetapan akreditasi.
	 *
	 * @param tanggalAkreditasi tanggal penetapan akreditasi
	 */
	public void setTanggalAkreditasi(Date tanggalAkreditasi) {
		this.tanggalAkreditasi = tanggalAkreditasi;
	}

	/**
	 * Mengembalikan nama domain internet perguruan tinggi.
	 *
	 * <p>Terpisah dari {@link #getWebsite()} yang berupa URL lengkap; keduanya tidak dijaga agar
	 * konsisten satu sama lain.</p>
	 *
	 * @return nama domain, atau {@code null} bila belum diisi
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * Menetapkan nama domain internet perguruan tinggi, tanpa validasi format.
	 *
	 * @param domain nama domain
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * Mengembalikan motto/semboyan perguruan tinggi.
	 *
	 * @return motto, atau {@code null} bila belum diisi
	 */
	public String getMotto() {
		return motto;
	}

	/**
	 * Menetapkan motto/semboyan perguruan tinggi.
	 *
	 * @param motto motto/semboyan
	 */
	public void setMotto(String motto) {
		this.motto = motto;
	}

	/**
	 * Mengembalikan nama rektor/pimpinan perguruan tinggi yang menjabat.
	 *
	 * <p>Disimpan sebagai teks, bukan relasi ke entity pegawai mana pun — memang disengaja karena
	 * institusi ini di luar organisasi yang dikelola AIS. Hanya satu pejabat yang dapat dicatat dan
	 * tidak ada riwayat masa jabatan.</p>
	 *
	 * @return nama rektor, atau {@code null} bila belum diisi
	 * @see #getRektorNip()
	 */
	public String getRektor() {
		return rektor;
	}

	/**
	 * Menetapkan nama rektor/pimpinan perguruan tinggi.
	 *
	 * @param rektor nama rektor
	 */
	public void setRektor(String rektor) {
		this.rektor = rektor;
	}

	/**
	 * Mengembalikan NIP atau nomor induk rektor.
	 *
	 * @return NIP rektor, atau {@code null} bila belum diisi
	 * @see #getRektor()
	 */
	public String getRektorNip() {
		return rektorNip;
	}

	/**
	 * Menetapkan NIP atau nomor induk rektor.
	 *
	 * @param rektorNip NIP rektor
	 */
	public void setRektorNip(String rektorNip) {
		this.rektorNip = rektorNip;
	}

	/**
	 * Kode institusi pada SINTA (Science and Technology Index) Kemdikbudristek.
	 *
	 * <p>Mengembalikan nilai yang sudah di-trim, dengan {@code null} <b>diganti string kosong</b> —
	 * arah yang berlawanan dengan {@link #getFeeder()} yang justru mengubah kosong menjadi
	 * {@code null}. Pemanggil karena itu tidak dapat membedakan institusi yang belum punya kode
	 * SINTA dari yang kodenya memang kosong.</p>
	 *
	 * @return kode SINTA yang sudah di-trim; string kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	public String getKodeSinta() {
		return kodeSinta == null ? "" : kodeSinta.trim();
	}

	/**
	 * Menetapkan kode institusi pada SINTA.
	 *
	 * @param kodeSinta kode SINTA
	 */
	public void setKodeSinta(String kodeSinta) {
		this.kodeSinta = kodeSinta;
	}

}
