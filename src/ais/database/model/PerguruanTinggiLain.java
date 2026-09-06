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

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "kode_yayasan", nullable = false, length = 50)
	public String getKodeYayasan() {
		return this.kodeYayasan;
	}

	public void setKodeYayasan(String kodeYayasan) {
		this.kodeYayasan = kodeYayasan;
	}

	@Column(name = "kode_perguruan_tinggi", nullable = false, length = 50)
	public String getKodePerguruanTinggi() {
		return this.kodePerguruanTinggi;
	}

	public void setKodePerguruanTinggi(String kodePerguruanTinggi) {
		this.kodePerguruanTinggi = kodePerguruanTinggi;
	}

	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat1", nullable = false, length = 150)
	public String getAlamat1() {
		return this.alamat1 == null ? "" : this.alamat1.trim();
	}

	public void setAlamat1(String alamat1) {
		this.alamat1 = alamat1;
	}

	@Column(name = "alamat2", length = 150)
	public String getAlamat2() {
		return this.alamat2 == null ? "" : this.alamat2.trim();
	}

	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	@Column(name = "kota", length = 50)
	public String getKota() {
		return this.kota;
	}

	public void setKota(String kota) {
		this.kota = kota;
	}

	@Column(name = "kode_pos", length = 10)
	public String getKodePos() {
		return this.kodePos;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	@Column(name = "telepon", length = 50)
	public String getTelepon() {
		return this.telepon;
	}

	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	@Column(name = "faksimili", length = 50)
	public String getFaksimili() {
		return this.faksimili;
	}

	public void setFaksimili(String faksimili) {
		this.faksimili = faksimili;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_akta", length = 0)
	public Date getTanggalAkta() {
		return this.tanggalAkta;
	}

	public void setTanggalAkta(Date tanggalAkta) {
		this.tanggalAkta = tanggalAkta;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_awal_pendirian", length = 0)
	public Date getTanggalAwalPendirian() {
		return this.tanggalAwalPendirian;
	}

	public void setTanggalAwalPendirian(Date tanggalAwalPendirian) {
		this.tanggalAwalPendirian = tanggalAwalPendirian;
	}

	@Column(name = "nomor_akta", length = 30)
	public String getNomorAkta() {
		return this.nomorAkta;
	}

	public void setNomorAkta(String nomorAkta) {
		this.nomorAkta = nomorAkta;
	}

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

	public void setEmail(String email) {
		this.email = email;
	}

	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	@Column(name = "website", length = 150)
	public String getWebsite() {
		return this.website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	@Column(name = "luas_tanah_total")
	public Double getLuasTanahTotal() {
		return luasTanahTotal;
	}

	public void setLuasTanahTotal(Double luasTanahTotal) {
		this.luasTanahTotal = luasTanahTotal;
	}

	@Column(name = "luas_kebun_lahan_percobaan_total")
	public Double getLuasKebunLahanPercobaanTotal() {
		return luasKebunLahanPercobaanTotal;
	}

	public void setLuasKebunLahanPercobaanTotal(Double luasKebunLahanPercobaanTotal) {
		this.luasKebunLahanPercobaanTotal = luasKebunLahanPercobaanTotal;
	}

	@Column(name = "luas_total_ruang_kuliah")
	public Double getLuasTotalRuangKuliah() {
		return luasTotalRuangKuliah;
	}

	public void setLuasTotalRuangKuliah(Double luasTotalRuangKuliah) {
		this.luasTotalRuangKuliah = luasTotalRuangKuliah;
	}

	@Column(name = "jumlah_ruang_kuliah")
	public Integer getJumlahRuangKuliah() {
		return jumlahRuangKuliah;
	}

	public void setJumlahRuangKuliah(Integer jumlahRuangKuliah) {
		this.jumlahRuangKuliah = jumlahRuangKuliah;
	}

	@Column(name = "luas_total_lab_studio")
	public Double getLuasTotalLabStudio() {
		return luasTotalLabStudio;
	}

	public void setLuasTotalLabStudio(Double luasTotalLabStudio) {
		this.luasTotalLabStudio = luasTotalLabStudio;
	}

	@Column(name = "jumlah_ruang_lab")
	public Integer getJumlahRuangLab() {
		return jumlahRuangLab;
	}

	public void setJumlahRuangLab(Integer jumlahRuangLab) {
		this.jumlahRuangLab = jumlahRuangLab;
	}

	@Column(name = "luas_total_ruang_dosen_tetap")
	public Double getLuasTotalRuangDosenTetap() {
		return luasTotalRuangDosenTetap;
	}

	public void setLuasTotalRuangDosenTetap(Double luasTotalRuangDosenTetap) {
		this.luasTotalRuangDosenTetap = luasTotalRuangDosenTetap;
	}

	@Column(name = "luas_total_ruang_administrasi")
	public Double getLuasTotalRuangAdministrasi() {
		return luasTotalRuangAdministrasi;
	}

	public void setLuasTotalRuangAdministrasi(Double luasTotalRuangAdministrasi) {
		this.luasTotalRuangAdministrasi = luasTotalRuangAdministrasi;
	}

	@Column(name = "luas_total_ruang_seminar")
	public Double getLuasTotalRuangSeminar() {
		return luasTotalRuangSeminar;
	}

	public void setLuasTotalRuangSeminar(Double luasTotalRuangSeminar) {
		this.luasTotalRuangSeminar = luasTotalRuangSeminar;
	}

	@Column(name = "luas_total_ruang_ekskul")
	public Double getLuasTotalRuangEkskul() {
		return luasTotalRuangEkskul;
	}

	public void setLuasTotalRuangEkskul(Double luasTotalRuangEkskul) {
		this.luasTotalRuangEkskul = luasTotalRuangEkskul;
	}

	@Column(name = "luas_total_pusat_komputer")
	public Double getLuasTotalPusatKomputer() {
		return luasTotalPusatKomputer;
	}

	public void setLuasTotalPusatKomputer(Double luasTotalPusatKomputer) {
		this.luasTotalPusatKomputer = luasTotalPusatKomputer;
	}

	@Column(name = "luas_total_ruang_perpustakaan")
	public Double getLuasTotalRuangPerpustakaan() {
		return luasTotalRuangPerpustakaan;
	}

	public void setLuasTotalRuangPerpustakaan(Double luasTotalRuangPerpustakaan) {
		this.luasTotalRuangPerpustakaan = luasTotalRuangPerpustakaan;
	}

	@Column(name = "jumlah_judul_buku")
	public Integer getJumlahJudulBuku() {
		return jumlahJudulBuku;
	}

	public void setJumlahJudulBuku(Integer jumlahJudulBuku) {
		this.jumlahJudulBuku = jumlahJudulBuku;
	}

	@Column(name = "jumlah_eksemplar_buku")
	public Integer getJumlahEksemplarBuku() {
		return jumlahEksemplarBuku;
	}

	public void setJumlahEksemplarBuku(Integer jumlahEksemplarBuku) {
		this.jumlahEksemplarBuku = jumlahEksemplarBuku;
	}

	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = "";
		}
		return deskripsi;
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/** Id/kode perguruan tinggi ini pada sistem feeder PDDikti, bila hasil impor dari feeder. */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	public String getNamaSingkat() {
		return namaSingkat;
	}

	public void setNamaSingkat(String namaSingkat) {
		this.namaSingkat = namaSingkat;
	}

	public String getDusun() {
		return dusun;
	}

	public void setDusun(String dusun) {
		this.dusun = dusun;
	}

	public String getRt() {
		return rt;
	}

	public void setRt(String rt) {
		this.rt = rt;
	}

	public String getRw() {
		return rw;
	}

	public void setRw(String rw) {
		this.rw = rw;
	}

	public String getKelurahan() {
		return kelurahan;
	}

	public void setKelurahan(String kelurahan) {
		this.kelurahan = kelurahan;
	}

	public String getSkIzinOperasi() {
		return skIzinOperasi;
	}

	public void setSkIzinOperasi(String skIzinOperasi) {
		this.skIzinOperasi = skIzinOperasi;
	}

	@Temporal(TemporalType.DATE)
	public Date getTglSkIzinOperasi() {
		return tglSkIzinOperasi;
	}

	public void setTglSkIzinOperasi(Date tglSkIzinOperasi) {
		this.tglSkIzinOperasi = tglSkIzinOperasi;
	}

	public String getNoRek() {
		return noRek;
	}

	public void setNoRek(String noRek) {
		this.noRek = noRek;
	}

	public String getNmBank() {
		return nmBank;
	}

	public void setNmBank(String nmBank) {
		this.nmBank = nmBank;
	}

	public String getUnitCabang() {
		return unitCabang;
	}

	public void setUnitCabang(String unitCabang) {
		this.unitCabang = unitCabang;
	}

	public String getNmRek() {
		return nmRek;
	}

	public void setNmRek(String nmRek) {
		this.nmRek = nmRek;
	}

	public Double getLuasTanahMilik() {
		return luasTanahMilik;
	}

	public void setLuasTanahMilik(Double luasTanahMilik) {
		this.luasTanahMilik = luasTanahMilik;
	}

	public Double getLuasTanahBukanMilik() {
		return luasTanahBukanMilik;
	}

	public void setLuasTanahBukanMilik(Double luasTanahBukanMilik) {
		this.luasTanahBukanMilik = luasTanahBukanMilik;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public String getPejabatIzinOperasi() {
		return pejabatIzinOperasi;
	}

	public void setPejabatIzinOperasi(String pejabatIzinOperasi) {
		this.pejabatIzinOperasi = pejabatIzinOperasi;
	}

	public Integer getTahunPertamaMenerimaMahasiswa() {
		return tahunPertamaMenerimaMahasiswa == null ? 0 : tahunPertamaMenerimaMahasiswa;
	}

	public void setTahunPertamaMenerimaMahasiswa(Integer tahunPertamaMenerimaMahasiswa) {
		this.tahunPertamaMenerimaMahasiswa = tahunPertamaMenerimaMahasiswa;
	}

	public String getPeringkatAkreditasi() {
		return peringkatAkreditasi;
	}

	public void setPeringkatAkreditasi(String peringkatAkreditasi) {
		this.peringkatAkreditasi = peringkatAkreditasi;
	}

	public String getAkreditasi() {
		return akreditasi;
	}

	public void setAkreditasi(String akreditasi) {
		this.akreditasi = akreditasi;
	}

	public String getNoSkAkreditasi() {
		return noSkAkreditasi;
	}

	public void setNoSkAkreditasi(String noSkAkreditasi) {
		this.noSkAkreditasi = noSkAkreditasi;
	}

	public Date getTanggalAkreditasi() {
		return tanggalAkreditasi;
	}

	public void setTanggalAkreditasi(Date tanggalAkreditasi) {
		this.tanggalAkreditasi = tanggalAkreditasi;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getMotto() {
		return motto;
	}

	public void setMotto(String motto) {
		this.motto = motto;
	}

	public String getRektor() {
		return rektor;
	}

	public void setRektor(String rektor) {
		this.rektor = rektor;
	}

	public String getRektorNip() {
		return rektorNip;
	}

	public void setRektorNip(String rektorNip) {
		this.rektorNip = rektorNip;
	}

	/** Kode institusi pada SINTA (Science and Technology Index) Kemdikbudristek. */
	public String getKodeSinta() {
		return kodeSinta == null ? "" : kodeSinta.trim();
	}

	public void setKodeSinta(String kodeSinta) {
		this.kodeSinta = kodeSinta;
	}

}
