package ais.database.model;

// Generated Apr 5, 2010 1:13:29 AM by Hibernate Tools 3.2.4.CR1

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
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;

/**
 * Entity <b>master ruangan</b> (tabel {@code public.ruang}) &mdash; satu baris mewakili satu
 * ruang fisik: ruang kelas, laboratorium, aula, ruang rapat, ruang ujian, gudang aset, kamar
 * rawat, dan sejenisnya. Kelas ini adalah <b>tabel lookup murni</b>: ia hanya menyimpan
 * identitas dan properti statis sebuah ruangan (kode, nama, luas, kapasitas, gedung induk,
 * kepemilikan unit), <b>tidak</b> menyimpan jadwal, transaksi, maupun status pemakaian.
 * Semua "siapa memakai ruang ini kapan" berada di entity lain yang menunjuk ke sini.
 *
 * <h2>Posisi dalam hierarki</h2>
 *
 * <p>Turunan langsung {@link ais.database.model.GeneralValueObject}. Perhatikan bahwa
 * {@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan satu pun properti induknya.
 * Karena itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} yang
 * dideklarasikan ulang di kelas ini <b>bukan duplikasi keliru</b>, melainkan
 * <b>keharusan teknis</b>: tanpa deklarasi ulang, kolom-kolom itu tidak akan pernah dipetakan.
 * Yang diwarisi dari induk hanyalah <i>perilaku</i>, terutama {@code check(...)} untuk resolusi
 * proxy lazy yang dipakai kelima getter relasi di bawah.</p>
 *
 * <h2>Pemetaan &amp; anotasi</h2>
 *
 * <ul>
 *   <li>{@code @Entity} + {@code @Table(schema = "public", name = "ruang")}.</li>
 *   <li>{@code @org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)} —
 *   hanya kolom yang benar-benar berubah yang ikut di-{@code INSERT}/{@code UPDATE}. Konsekuensi
 *   penting: <b>getter yang menulis balik ke field terpetakan</b> (lihat bagian di bawah) dapat
 *   menghasilkan {@code UPDATE} nyata saat flush, meski pengguna tidak menekan tombol simpan.</li>
 *   <li>{@code @Audited} (Hibernate Envers) — setiap perubahan baris direkam ke tabel revisi;
 *   perubahan yang tidak disengaja pun ikut tercatat di jejak audit.</li>
 *   <li>Akses properti (annotasi berada di getter, karena {@code @Id} ada di
 *   {@link #getId()}), sehingga <b>Hibernate membaca nilai lewat getter</b> — termasuk getter
 *   yang mengandung logika turunan.</li>
 *   <li>Beberapa properti sengaja <b>tanpa</b> {@code @Column} ({@code aktif}, {@code keterangan},
 *   {@code ikutiIpGedung}, {@code ip}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah}).
 *   Karena aplikasi memakai {@code ais.database.hibernate.MyNamingStrategy} (turunan
 *   {@code DefaultNamingStrategy}, nama kolom = nama properti apa adanya tanpa konversi ke
 *   <i>snake_case</i>), nama kolomnya persis sama dengan nama properti — mis. kolom
 *   {@code ikutiIpGedung}, bukan {@code ikuti_ip_gedung}.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Jejak audit &amp; identitas</b> — {@link #getId()}, {@link #getOleh()},
 *   {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@code @PreUpdate}, dan
 *   {@link #toString()}.</li>
 *   <li><b>Identitas ruangan</b> — {@link #getKodeRuangan()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #getAktif()}.</li>
 *   <li><b>Properti fisik</b> — {@link #getLuas()} (m&sup2;), {@link #getKapasitasRuangan()}
 *   (jumlah kursi), {@link #getMerupakanRuangKelas()} (boleh dipakai perkuliahan atau tidak).</li>
 *   <li><b>Lokasi &amp; kepemilikan</b> — {@link #getGedung()}, lalu empat relasi unit pemilik:
 *   {@link #getFakultas()}/{@link #getJurusan()} (jalur perguruan tinggi) dan
 *   {@link #getYayasan()}/{@link #getSekolah()} (jalur sekolah). Keempatnya {@code nullable} —
 *   ruangan tanpa pemilik dianggap milik bersama dan muncul untuk semua unit.</li>
 *   <li><b>Alamat IP</b> — {@link #getIkutiIpGedung()} dan {@link #getIp()}; nilai IP diwarisi
 *   dari {@link Gedung} bila bendera "ikuti" menyala.</li>
 *   <li><b>Utilitas statis</b> — {@link #getDefaultKapasitas()}, satu-satunya method statis di
 *   kelas ini. <b>Tidak ada</b> method query statis (tidak ada {@code Criteria}/{@code Session}
 *   yang dibuka langsung oleh kelas ini).</li>
 * </ol>
 *
 * <h2>Pola pemakaian umum</h2>
 *
 * <p>Kelas ini adalah salah satu master yang paling banyak dirujuk di aplikasi: sekitar
 * <b>33 entity</b> di {@code ais.database.model} memiliki properti bertipe {@code Ruang}, dan
 * hampir <b>200 berkas Java</b> menyebutnya. Rujukannya hampir selalu berbentuk
 * {@code @ManyToOne} satu arah menuju kelas ini (tidak ada koleksi balik di sini), dengan
 * kelompok pemakai:</p>
 *
 * <ul>
 *   <li><b>Penjadwalan perkuliahan</b> — {@link Perkuliahan} (ruang default satu kelas),
 *   {@link Pertemuan} (ruang per tatap muka; getter-nya mewarisi ruang dari
 *   {@code Perkuliahan} bila kosong), {@link GrupPertemuan}, {@link KelasPertemuan},
 *   {@link TemplatePerkuliahanDetail}.</li>
 *   <li><b>Ujian &amp; penerimaan mahasiswa baru</b> — {@link JadwalUjianPMB} (lewat daftar
 *   ruang CSV), {@link RuangPaketPMB}, serta padanan jalur sekolah/rekrutmen
 *   ({@code sekolah.RuangGelombangPendaftaranPsbPSB},
 *   {@code recruitment.RuangGelombangPendaftaranPegawaiPegawai}).</li>
 *   <li><b>Tugas akhir</b> — {@link Skripsi}, {@link JadwalSidangTugasAkhir},
 *   {@link JadwalSeminarTugasAkhir}.</li>
 *   <li><b>Sekolah</b> — {@code sekolah.JadwalPelajaran}, {@code sekolah.KelasSiswa},
 *   {@code sekolah.KelasLesSiswa}.</li>
 *   <li><b>Aset &amp; inventaris</b> — {@code asset.Asset}, {@code asset.AssetDetail}, dan
 *   seluruh dokumen pengadaan/pemakaian/retur aset memakai {@code Ruang} sebagai lokasi
 *   penyimpanan.</li>
 *   <li><b>Rumah sakit (SIRS)</b> — {@code sirs.Kamar}, {@code sirs.TempatTidur},
 *   {@code sirs.AlatMedis}, {@code sirs.Pendaftaran}.</li>
 *   <li><b>Lain-lain</b> — {@code library.Rak} (rak perpustakaan), {@code surat.LokerSurat},
 *   {@link Dosen}, dan {@link PesanRuangan} (pemesanan/booking ruangan).</li>
 * </ul>
 *
 * <p>Layar pengelolanya adalah {@code ais.action.master.RuangAction} (form "Tambah/Ubah Ruang":
 * Nama Ruangan, Kode Ruangan, Luas Ruangan (m2), Kapasitas Ruangan, Merupakan Ruang Kelas,
 * Yayasan/Sekolah, Fakultas/Jurusan, Gedung, "Ikuti Ip Gedung", Alamat IP Gedung), lengkap
 * dengan fasilitas cetak dan unggah massal atas properti
 * {@code id, kodeRuangan, nama, luas, gedung, kapasitasRuangan, merupakanRuangKelas,
 * ikutiIpGedung, ip, yayasan, sekolah, fakultas, jurusan, aktif}. Layar pencarian ruang bebas
 * ada di {@code ais.action.master.CariRuangKosongAction}.</p>
 *
 * <h2>Hal non-obvious yang perlu diketahui</h2>
 *
 * <ul>
 *   <li><b>Empat getter menulis balik ke field terpetakan</b> — {@link #getKapasitasRuangan()}
 *   (mengisi dari konfigurasi bila null), {@link #getFakultas()} (<b>selalu</b> menimpa dari
 *   {@code jurusan.getFakultas()} bila jurusan terisi), {@link #getIp()} (menimpa dari
 *   {@code gedung.getIp()} bila "ikuti IP gedung" menyala), dan efek samping penugasan field
 *   {@code gedung} di dalam {@link #getIp()}. Pada instance <i>managed</i>, membaca getter ini
 *   dapat memicu {@code UPDATE} + baris revisi Envers tanpa aksi simpan dari pengguna.</li>
 *   <li><b>Lima getter relasi memanggil {@code check(...)}</b> dan menugaskan hasilnya kembali
 *   ke field: {@link #getGedung()}, {@link #getJurusan()}, {@link #getFakultas()},
 *   {@link #getYayasan()}, {@link #getSekolah()}. Ini pola baku repo untuk melindungi getter
 *   dari {@code LazyInitializationException}; biayanya adalah kemungkinan pembukaan session
 *   sendiri di dalam {@code check(...)}.</li>
 *   <li><b>Tidak ada getter yang menutup sesi Hibernate secara langsung</b> dan tidak ada getter
 *   destruktif (tidak ada yang menghapus/mengosongkan data). Jalur tak langsung ke database
 *   hanya lewat {@code check(...)} dan {@link Common#getKonfigurasi(String, String)}.</li>
 *   <li><b>Dua setter menelan nilai secara diam-diam</b> — {@link #setOleh(String)} dan
 *   {@link #setOlehId(String)} mengabaikan argumen {@code null}/kosong (mempertahankan nilai
 *   lama), sedangkan {@link #setYayasan(Yayasan)} dan {@link #setSekolah(Sekolah)} memaksa
 *   {@code null} bila object yang diberikan belum punya id (belum tersimpan).</li>
 *   <li><b>Nilai default banyak tersembunyi di getter</b>, bukan di deklarasi field:
 *   {@code aktif} &rarr; {@code true}, {@code ikutiIpGedung} &rarr; {@code true},
 *   {@code merupakanRuangKelas} &rarr; {@code 1}, {@code luas} &rarr; {@code 0.0},
 *   {@code keterangan}/{@code kodeRuangan}/{@code ip} &rarr; string kosong. Perbedaan antara
 *   "belum diisi" dan "sengaja dikosongkan" karena itu <b>tidak terlihat</b> dari getter.</li>
 *   <li><b>Kode mati yang sengaja dipertahankan</b> — properti {@code untukPmb} dan relasi
 *   {@code @ManyToMany} ke {@code FasilitasRuangan} (tabel gabungan
 *   {@code ruang_punya_fasilitas}) masih ada dalam bentuk komentar. Entity
 *   {@code FasilitasRuangan} sendiri tetap terdaftar di {@code hibernate.cfg.xml}, tetapi
 *   keterkaitannya dengan ruangan saat ini tidak aktif.</li>
 * </ul>
 *
 * <p><b>Catatan konkurensi.</b> Instance kelas ini tidak <i>thread-safe</i>; ia mengikuti siklus
 * hidup session Hibernate / desktop ZK tempat ia dimuat. Jangan menyimpannya dalam cache statis
 * lintas pengguna — getter-getter berefek samping di atas akan membuat perilakunya sulit
 * ditebak.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Gedung
 * @see Perkuliahan
 * @see Pertemuan
 * @see PesanRuangan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ruang")

public class Ruang extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi. Nilainya di-<i>hardcode</i> agar instance yang sudah pernah
	 * diserialisasi (mis. tersimpan di session ZK atau dikirim antar node) tetap kompatibel
	 * meski struktur field berubah. Jangan diubah tanpa alasan yang sangat kuat.
	 */
	private static final long serialVersionUID = -7550466125892447098L;

	/** Kunci utama baris (kolom {@code id}, {@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit). Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}; lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (jejak audit). Diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}; lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> argumen {@code null} atau berisi spasi saja <b>diabaikan diam-diam</b>
	 * — nilai lama dipertahankan. Jadi jejak audit tidak bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Kapasitas ruangan bawaan (jumlah peserta) yang dipakai saat sebuah ruang atau kelas belum
	 * menetapkan kapasitasnya sendiri.
	 *
	 * <p><b>Sumber nilai.</b> Konfigurasi aplikasi bernama
	 * {@code default_kapasitas_perkuliahan}, dengan default {@code "30"}. Perlu diingat bahwa
	 * {@link Common#getKonfigurasi(String, String)} <b>menulis baris konfigurasi baru ke
	 * database</b> berisi nilai default bila kunci tersebut belum pernah ada — pemanggilan
	 * pertama karena itu bersifat menyemai data, bukan sekadar membaca.</p>
	 *
	 * <p><b>Cara kerja.</b> Nilai konfigurasi diurai sebagai {@code Double} lalu dipangkas ke
	 * {@code int} melalui {@code intValue()}, sehingga nilai berkoma seperti {@code "30.9"}
	 * menjadi {@code 30}. Bila terjadi kegagalan apa pun (konfigurasi tidak terbaca, nilai bukan
	 * angka, {@code null}), exception ditelan dan dicatat ke audit error, lalu method
	 * mengembalikan <b>{@code 0}</b> — <i>bukan</i> 30. Pemanggil yang memakai hasil method ini
	 * sebagai batas atas jumlah peserta perlu menyadari kemungkinan nilai 0 tersebut.</p>
	 *
	 * <p><b>Dipanggil dari.</b> Banyak jalur penjadwalan dan pengisian kelas, antara lain
	 * {@code Perkuliahan.getKapasitasKelas()}, {@link #getKapasitasRuangan()} di kelas ini,
	 * {@code RuangAction}/{@code CariRuangKosongAction} (tampilan &amp; form),
	 * {@code AmbilDataIkutPerkuliahanHelper}, {@code AmbilDataPerkuliahanNonPaketHelper},
	 * {@code AmbilDataMahasiswaForPaketPerkuliahanHelper}, {@code TransferDataMahasiswaHelper},
	 * {@code GenerateKRSPaketMahasiswaOtomatisWindow}, dan {@code PenjadwalanUtil}.</p>
	 *
	 * @return kapasitas bawaan hasil pembulatan ke bawah dari konfigurasi, atau {@code 0} bila
	 *         konfigurasi gagal dibaca/diurai
	 */
	public static Integer getDefaultKapasitas() {
		Double defaultKapasitas = 0.0;
		try {
			defaultKapasitas = Double
					.parseDouble(Common.getKonfigurasi("default_kapasitas_perkuliahan", "30").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Ruang.java:61");

		}
		return defaultKapasitas.intValue();
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, argumen {@code null} atau
	 * kosong <b>diabaikan diam-diam</b> sehingga nilai lama tetap bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait siklus hidup JPA {@code @PreUpdate} sekaligus deklarasi field
	 * {@code tanggal_dirubah} (keduanya ditulis pada satu baris oleh generator/penyunting
	 * terdahulu).
	 *
	 * <p><b>Kait.</b> {@code onUpdate()} dijalankan Hibernate <b>tepat sebelum</b> pernyataan
	 * {@code UPDATE} baris ini dikirim ke database, dan mendelegasikan pengisian jejak audit
	 * ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Karena itu ketiga
	 * kolom audit tidak perlu diisi manual oleh layar mana pun. Jangan memanggil method ini
	 * dari kode aplikasi.</p>
	 *
	 * <p><b>Field.</b> {@code tanggal_dirubah} diinisialisasi ke waktu server saat object dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), sehingga baris baru pun sudah punya stempel
	 * waktu sebelum sempat di-{@code UPDATE}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya <b>tidak perlu dipanggil</b> dari kode layar: nilainya diisi otomatis oleh kait
	 * {@code @PreUpdate} di atas. Setter ini terutama dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, tipe
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya diinisialisasi dengan waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berbentuk <i>"&lt;id&gt;-&lt;nama&gt;"</i>, mis.
	 * {@code "12-Ruang Kuliah A.1.2"}.
	 *
	 * <p>Dipakai antara lain sebagai label pada komponen ZK, isi combobox, dan keluaran log.
	 * Perhatikan dua hal: (1) method ini membaca <b>field</b> {@code nama} secara langsung,
	 * bukan {@link #getNama()}, sehingga nilainya <b>tidak di-{@code trim}</b>; dan (2) untuk
	 * baris yang belum tersimpan hasilnya berawalan {@code "null-"}.</p>
	 *
	 * @return gabungan id dan nama ruangan dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat ruangan (kolom {@code kode_ruangan}). Lihat {@link #getKodeRuangan()}. */
	private String kodeRuangan;

	/** Nama ruangan sebagaimana tampil di layar (kolom {@code nama}). Lihat {@link #getNama()}. */
	private String nama;

	/** Catatan bebas tentang ruangan. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Luas ruangan dalam meter persegi (kolom {@code luas_ruang}). Lihat {@link #getLuas()}. */
	private Double luas;

	/** Gedung tempat ruangan berada. Relasi lazy; lihat {@link #getGedung()}. */
	private Gedung gedung;

	/** Daya tampung ruangan (kolom {@code kapasitas_ruangan}). Lihat {@link #getKapasitasRuangan()}. */
	private Integer kapasitasRuangan;

	/**
	 * Penanda ruang kelas: {@code 1} = ya, {@code 0} = tidak (kolom
	 * {@code merupakan_ruang_kelas}). Lihat {@link #getMerupakanRuangKelas()}.
	 */
	private Integer merupakanRuangKelas;
	// private Integer untukPmb;

	/**
	 * Fakultas pemilik ruangan. <b>Diturunkan</b> dari {@link #jurusan} setiap kali
	 * {@link #getFakultas()} dipanggil.
	 */
	private Fakultas fakultas;

	/** Jurusan/program studi pemilik ruangan. Relasi lazy; lihat {@link #getJurusan()}. */
	private Jurusan jurusan;

	/** Status aktif ruangan; {@code null} diperlakukan sebagai aktif. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Bila {@code true} (juga bila {@code null}), alamat IP ruangan mengikuti alamat IP gedung
	 * induknya. Lihat {@link #getIkutiIpGedung()} dan {@link #getIp()}.
	 */
	private Boolean ikutiIpGedung;

	/**
	 * Daftar alamat IP yang diizinkan untuk ruangan ini, dipisah titik koma; kosong berarti
	 * berlaku untuk semua alamat. Lihat {@link #getIp()}.
	 */
	private String ip;

//	private Set<FasilitasRuangan> fasilitasRuangans = new TreeSet<FasilitasRuangan>();

	/** Yayasan pemilik ruangan (jalur sekolah). Relasi lazy; lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Sekolah pemilik ruangan (jalur sekolah). Relasi lazy; lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

//	@ManyToMany(targetEntity = FasilitasRuangan.class, cascade = { CascadeType.MERGE })
//	@JoinTable(name = "ruang_punya_fasilitas", joinColumns = @JoinColumn(name = "ruang"), inverseJoinColumns = @JoinColumn(name = "fasilitas"))
//	public Set<FasilitasRuangan> getFasilitasRuangans() {
//		return fasilitasRuangans;
//	}
//
//	public void setFasilitasRuangans(Set<FasilitasRuangan> fasilitasRuangans) {
//		this.fasilitasRuangans = fasilitasRuangans;
//	}

	/**
	 * Konstruktor tanpa argumen. Wajib ada untuk Hibernate dan dipakai layar
	 * {@code RuangAction} saat menambah ruangan baru.
	 *
	 * <p>Seluruh properti dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang langsung
	 * diisi waktu server. Nilai bawaan lain (aktif, ikuti IP gedung, kapasitas, luas, dsb.)
	 * baru muncul ketika getter masing-masing dipanggil.</p>
	 */
	public Ruang() {
	}

	/**
	 * Kunci utama baris ruangan.
	 *
	 * <p>Dihasilkan database ({@code IDENTITY}); kolomnya {@code insertable = false} sehingga
	 * nilai yang diisi manual tidak akan dikirim pada {@code INSERT}.</p>
	 *
	 * @return id ruangan, atau {@code null} bila baris belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Umumnya hanya dipanggil Hibernate saat memuat baris.
	 *
	 * @param id kunci utama ruangan
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode singkat ruangan (kolom {@code kode_ruangan}, maksimal 50 karakter, {@code NOT NULL}),
	 * mis. {@code "A.1.2"} atau {@code "LAB-KOM-1"}. Dipakai pada daftar ruang, cetakan jadwal,
	 * dan unggah massal.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} dikembalikan sebagai <b>string kosong</b>, dan
	 * nilai yang ada di-{@code trim}. Karena kolomnya {@code NOT NULL} sementara getter
	 * menyembunyikan {@code null}, pemanggil tidak dapat membedakan "belum diisi" dari "diisi
	 * spasi". Hasil {@code trim} <b>tidak</b> ditulis balik ke field, jadi getter ini tidak
	 * berefek samping.</p>
	 *
	 * @return kode ruangan tanpa spasi tepi, atau string kosong bila belum diisi
	 */
	@Column(name = "kode_ruangan", nullable = false, length = 50)
	public String getKodeRuangan() {
		return this.kodeRuangan == null ? "" : kodeRuangan.trim();
	}

	/**
	 * Mengisi kode singkat ruangan.
	 *
	 * @param kodeRuangan kode ruangan (maksimal 50 karakter); disimpan apa adanya tanpa
	 *                    normalisasi
	 */
	public void setKodeRuangan(String kodeRuangan) {
		this.kodeRuangan = kodeRuangan;
	}

	/**
	 * Nama ruangan (kolom {@code nama}, maksimal 150 karakter, {@code NOT NULL}). Inilah label
	 * yang muncul pada jadwal perkuliahan, jadwal ujian, kartu aset, dan seluruh combobox
	 * pemilih ruangan.
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link #getKodeRuangan()}, getter ini
	 * <b>mengembalikan {@code null} apa adanya</b> (tidak diubah menjadi string kosong), tetapi
	 * tetap men-{@code trim} nilai yang ada. Pemanggil wajib menjaga {@code null} sendiri —
	 * pola {@code ruang.getNama() == null ? "" : ruang.getNama()} banyak dipakai di layar.</p>
	 *
	 * @return nama ruangan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama ruangan.
	 *
	 * @param nama nama ruangan (maksimal 150 karakter); disimpan apa adanya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Gedung tempat ruangan ini berada (kolom {@code gedung}, {@code nullable}).
	 *
	 * <p><b>Efek samping.</b> Getter memanggil {@code check(gedung)} milik
	 * {@link ais.database.model.GeneralValueObject} dan <b>menugaskan hasilnya kembali ke
	 * field</b>. Ini menyelesaikan proxy lazy yang mungkin sudah terlepas dari session
	 * ({@code LazyInitializationException}); dalam kasus terburuk {@code check(...)} membuka
	 * session baru untuk memuat ulang entity. Karena hasilnya ditulis ke field, pemanggilan
	 * berikutnya menjadi murah.</p>
	 *
	 * <p>Relasi ini juga menjadi sumber alamat IP ruangan, lihat {@link #getIp()}.</p>
	 *
	 * @return gedung induk, atau {@code null} bila ruangan tidak dikaitkan ke gedung mana pun
	 * @see Gedung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gedung", nullable = true)
	public Gedung getGedung() {
		gedung = check(gedung);
		return this.gedung;
	}

	/**
	 * Mengaitkan ruangan ke sebuah gedung.
	 *
	 * @param gedung gedung induk; boleh {@code null}
	 */
	public void setGedung(Gedung gedung) {
		this.gedung = gedung;
	}

	/**
	 * Mengisi daya tampung ruangan.
	 *
	 * @param kapasitasRuangan jumlah peserta maksimal; boleh {@code null}, dan bila demikian
	 *                         {@link #getKapasitasRuangan()} akan mengisinya dari konfigurasi
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Daya tampung ruangan dalam jumlah peserta (kolom {@code kapasitas_ruangan},
	 * {@code NOT NULL}).
	 *
	 * <p><b>Efek samping penting.</b> Bila field masih {@code null}, getter mengisinya dari
	 * {@link #getDefaultKapasitas()} dan <b>menulis nilai itu ke field terpetakan</b>. Pada
	 * instance yang masih <i>managed</i>, sekadar <i>membaca</i> kapasitas sebuah ruangan lama
	 * yang kolomnya kosong dapat memicu {@code UPDATE} nyata (kelas ini
	 * {@code dynamicUpdate = true}) beserta satu baris revisi Envers, tanpa pengguna menekan
	 * tombol simpan. Ditambah lagi, {@link #getDefaultKapasitas()} sendiri dapat menyemai baris
	 * konfigurasi baru ke database pada pemanggilan pertama.</p>
	 *
	 * <p>Dipakai luas oleh pengecekan kuota kelas: {@code CariRuangKosongAction},
	 * {@code TransferDataMahasiswaHelper}, dan berbagai helper pengisian KRS/perkuliahan.</p>
	 *
	 * @return kapasitas ruangan; bila belum diisi, nilai bawaan konfigurasi (yang bisa
	 *         bernilai {@code 0} saat konfigurasi gagal dibaca)
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = Ruang.getDefaultKapasitas();
		}
		return kapasitasRuangan;
	}

	/**
	 * Menetapkan apakah ruangan ini merupakan ruang kelas.
	 *
	 * @param merupakanRuangKelas {@code 1} untuk "Ya", {@code 0} untuk "Tidak"; {@code null}
	 *                            akan dibaca sebagai "Ya" oleh
	 *                            {@link #getMerupakanRuangKelas()}
	 */
	public void setMerupakanRuangKelas(Integer merupakanRuangKelas) {
		this.merupakanRuangKelas = merupakanRuangKelas;
	}

	/**
	 * Penanda apakah ruangan ini dipakai untuk perkuliahan/kelas (kolom
	 * {@code merupakan_ruang_kelas}, panjang 1): {@code 1} = "Ya", {@code 0} = "Tidak".
	 *
	 * <p>Dipakai layar {@code RuangAction} dan {@code CariRuangKosongAction} untuk menandai
	 * baris — dan untuk memisahkan ruang kelas dari ruang non-kelas (gudang aset, kamar rawat,
	 * loker surat, rak perpustakaan) yang juga tersimpan di tabel yang sama.</p>
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} diperlakukan sebagai {@code 1} ("Ya"), sehingga
	 * <b>data lama yang belum pernah diisi otomatis dianggap ruang kelas</b>. Berbeda dengan
	 * {@link #getKapasitasRuangan()}, nilai bawaan ini <b>tidak</b> ditulis balik ke field —
	 * getter ini tidak berefek samping.</p>
	 *
	 * @return {@code 1} bila ruang kelas, {@code 0} bila bukan; tidak pernah {@code null}
	 */
	@Column(name = "merupakan_ruang_kelas", length = 1)
	public Integer getMerupakanRuangKelas() {
		return merupakanRuangKelas == null ? 1 : merupakanRuangKelas;
	}

	/**
	 * Mengaitkan ruangan ke sebuah jurusan/program studi pemilik.
	 *
	 * <p><b>Perhatikan:</b> karena {@link #getFakultas()} selalu menurunkan fakultas dari
	 * jurusan, mengubah jurusan efektif juga mengubah fakultas pada pembacaan berikutnya.</p>
	 *
	 * @param jurusan jurusan pemilik; {@code null} berarti ruangan tidak terikat jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Jurusan/program studi pemilik ruangan (kolom {@code jurusan}, {@code nullable}).
	 *
	 * <p>Dipakai untuk membatasi daftar ruangan yang terlihat oleh pengguna sesuai unitnya:
	 * {@code RuangAction} menyaring dengan {@code jurusan = <jurusan pengguna> OR jurusan IS
	 * NULL}, sehingga ruangan tanpa jurusan berlaku umum untuk semua unit.</p>
	 *
	 * <p><b>Efek samping.</b> Memanggil {@code check(jurusan)} dan menulis hasilnya kembali ke
	 * field (resolusi proxy lazy), sama seperti {@link #getGedung()}.</p>
	 *
	 * @return jurusan pemilik, atau {@code null} bila ruangan berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengaitkan ruangan ke sebuah fakultas pemilik.
	 *
	 * <p><b>Peringatan penting:</b> nilai yang diisi lewat setter ini <b>akan ditimpa</b> oleh
	 * {@link #getFakultas()} bila {@link #getJurusan()} tidak {@code null} — fakultas selalu
	 * diturunkan ulang dari jurusan. Setter ini hanya benar-benar menentukan hasil untuk
	 * ruangan yang <i>tidak</i> punya jurusan.</p>
	 *
	 * @param fakultas fakultas pemilik; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Fakultas pemilik ruangan (kolom {@code fakultas}, {@code nullable}).
	 *
	 * <p><b>Nilai turunan, bukan sekadar pembacaan.</b> Urutan kerjanya: (1) panggil
	 * {@link #getJurusan()} — yang sendirinya menyelesaikan proxy lazy dan menulis balik ke
	 * field {@code jurusan}; (2) bila jurusan terisi, <b>timpa</b> field {@code fakultas}
	 * dengan {@code jurusan.getFakultas()}; (3) jalankan {@code check(fakultas)} dan tulis
	 * hasilnya ke field.</p>
	 *
	 * <p>Konsekuensinya ada dua. Pertama, <b>fakultas yang diisi manual akan hilang</b> setiap
	 * kali getter dipanggil selama jurusan terisi — konsistensi fakultas&ndash;jurusan dipaksa
	 * di lapisan model, bukan dipercayakan pada data. Kedua, karena {@code fakultas} adalah
	 * kolom terpetakan dan kelas ini {@code dynamicUpdate = true}, koreksi tersebut dapat
	 * ter-{@code UPDATE} ke database (dan tercatat di Envers) hanya karena getter dibaca pada
	 * instance <i>managed</i> — misalnya saat merender satu baris di daftar ruangan.</p>
	 *
	 * @return fakultas pemilik hasil penurunan dari jurusan, atau {@code null} bila ruangan
	 *         tidak terikat fakultas mana pun
	 * @see #getJurusan()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengaitkan ruangan ke sebuah yayasan pemilik (jalur sekolah).
	 *
	 * <p><b>Non-obvious:</b> object yang <b>belum tersimpan</b> (id-nya masih {@code null})
	 * diperlakukan sama dengan {@code null} — relasi dikosongkan, bukan disimpan lalu
	 * di-{@code cascade}. Ini mencegah combobox "Semua Yayasan" (yang nilainya berupa instance
	 * kosong) tak sengaja membuat baris yayasan baru.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau instance tanpa id akan mengosongkan
	 *                relasi
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Yayasan pemilik ruangan (kolom {@code yayasan}, {@code nullable}) &mdash; dipakai pada
	 * instalasi yang mengelola sekolah, bukan perguruan tinggi.
	 *
	 * <p>Seperti {@link #getJurusan()}, kolom ini menjadi penyaring visibilitas di
	 * {@code RuangAction} ({@code yayasan = <yayasan pengguna> OR yayasan IS NULL}).</p>
	 *
	 * <p><b>Efek samping.</b> Memanggil {@code check(yayasan)} dan menulis hasilnya kembali ke
	 * field (resolusi proxy lazy).</p>
	 *
	 * @return yayasan pemilik, atau {@code null} bila ruangan berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Sekolah pemilik ruangan (kolom {@code sekolah}, {@code nullable}) &mdash; pasangan
	 * {@link #getYayasan()} pada jalur sekolah, sebagaimana {@link #getJurusan()} berpasangan
	 * dengan {@link #getFakultas()} pada jalur perguruan tinggi.
	 *
	 * <p><b>Perbedaan penting dengan {@link #getFakultas()}:</b> getter ini <b>tidak</b>
	 * menurunkan nilainya dari yayasan; sekolah dan yayasan diisi mandiri, dan tidak ada
	 * pemaksaan konsistensi di lapisan model.</p>
	 *
	 * <p><b>Efek samping.</b> Memanggil {@code check(sekolah)} dan menulis hasilnya kembali ke
	 * field (resolusi proxy lazy).</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila ruangan berlaku umum
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Mengaitkan ruangan ke sebuah sekolah pemilik.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setYayasan(Yayasan)}, instance yang belum
	 * punya id diperlakukan sebagai {@code null}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau instance tanpa id akan mengosongkan
	 *                relasi
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Status aktif ruangan. Ruangan non-aktif disembunyikan dari daftar dan dari pilihan
	 * penjadwalan; {@code RuangAction} menyaring dengan {@code aktif IS NULL OR aktif = true}
	 * dan menyediakan kotak centang "Aktif" langsung pada baris daftar.
	 *
	 * <p><b>Non-obvious:</b> {@code null} diperlakukan sebagai <b>aktif</b>, sehingga seluruh
	 * data lama yang kolomnya belum pernah diisi tetap terlihat. Nilai bawaan ini tidak ditulis
	 * balik ke field (tanpa efek samping). Properti ini tanpa {@code @Column} sehingga dipetakan
	 * ke kolom bernama {@code aktif} lewat penamaan default.</p>
	 *
	 * @return {@code true} bila ruangan aktif (termasuk saat kolomnya masih kosong)
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengubah status aktif ruangan.
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menonaktifkan;
	 *              {@code null} akan dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Luas ruangan dalam meter persegi (kolom {@code luas_ruang}). Ditampilkan sebagai
	 * <i>"&hellip; m2"</i> pada daftar ruangan dan dipakai laporan sarana-prasarana akreditasi
	 * ({@code LaporanDkps_5_3_PrasaranaPendidikan}).
	 *
	 * <p><b>Non-obvious:</b> {@code null} dikembalikan sebagai {@code 0.0} tanpa ditulis balik
	 * ke field, sehingga "belum diukur" tidak bisa dibedakan dari "luas nol".</p>
	 *
	 * @return luas ruangan dalam m&sup2;, atau {@code 0.0} bila belum diisi
	 */
	@Column(name = "luas_ruang")
	public Double getLuas() {
		return luas == null ? 0.0 : luas;
	}

	/**
	 * Mengisi luas ruangan.
	 *
	 * @param luas luas dalam meter persegi; boleh {@code null}
	 */
	public void setLuas(Double luas) {
		this.luas = luas;
	}

	/**
	 * Catatan bebas tentang ruangan.
	 *
	 * <p><b>Non-obvious:</b> {@code null} dikembalikan sebagai string kosong (tanpa ditulis
	 * balik ke field), dan nilainya <b>tidak</b> di-{@code trim}. Properti ini tanpa
	 * {@code @Column}, jadi kolomnya bernama {@code keterangan} sesuai penamaan default.</p>
	 *
	 * @return keterangan ruangan, atau string kosong bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Mengisi catatan bebas tentang ruangan.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Bendera "Ikuti Ip Gedung" pada form ruangan: bila menyala, alamat IP ruangan tidak diisi
	 * sendiri melainkan diambil dari {@link Gedung} induknya setiap kali {@link #getIp()}
	 * dipanggil.
	 *
	 * <p><b>Non-obvious:</b> {@code null} diperlakukan sebagai {@code true}, sehingga
	 * <b>seluruh ruangan lama secara bawaan mewarisi IP gedung</b> — dan karenanya nilai
	 * {@code ip} yang pernah diisi manual pada data lama akan tertimpa saat {@link #getIp()}
	 * dibaca. Nilai bawaan ini tidak ditulis balik ke field.</p>
	 *
	 * @return {@code true} bila ruangan mengikuti IP gedung (termasuk saat kolomnya kosong)
	 * @see #getIp()
	 */
	public Boolean getIkutiIpGedung() {
		return ikutiIpGedung == null ? true : ikutiIpGedung;
	}

	/**
	 * Mengatur apakah alamat IP ruangan mengikuti alamat IP gedung.
	 *
	 * <p>Diisi dari kotak centang "Ikuti Ip Gedung" di {@code RuangAction}; saat dicentang,
	 * layar menyembunyikan kotak isian alamat IP.</p>
	 *
	 * @param ikutiIpGedung {@code true} untuk mewarisi IP gedung, {@code false} untuk memakai
	 *                      IP sendiri; {@code null} dibaca sebagai {@code true}
	 */
	public void setIkutiIpGedung(Boolean ikutiIpGedung) {
		this.ikutiIpGedung = ikutiIpGedung;
	}

	/**
	 * Daftar alamat IP yang dikaitkan dengan ruangan ini. Pada form ruangan berlabel
	 * <i>"Alamat IP Gedung"</i> dengan petunjuk: <i>beberapa alamat dipisah titik koma
	 * ({@code ;}), dan dikosongkan bila berlaku untuk semua alamat IP</i>.
	 *
	 * <p><b>Nilai turunan + efek samping ganda.</b> Getter ini: (1) memanggil
	 * {@link #getGedung()} dan menugaskan hasilnya ke field {@code gedung} (resolusi proxy
	 * lazy); (2) bila {@link #getIkutiIpGedung()} bernilai {@code true} <i>dan</i> gedung
	 * terisi, <b>menimpa field {@code ip}</b> dengan {@code gedung.getIp()}; lalu (3)
	 * mengembalikan nilai yang sudah di-{@code trim}, atau string kosong bila {@code null}.</p>
	 *
	 * <p>Karena {@code ip} adalah kolom terpetakan (nama kolom {@code ip} lewat penamaan
	 * default) dan kelas ini {@code dynamicUpdate = true}, <b>membaca</b> IP sebuah ruangan yang
	 * masih <i>managed</i> dapat menyimpan IP gedung ke baris ruangan tersebut — sekali lagi
	 * tanpa aksi simpan dari pengguna, dan tercatat di jejak audit Envers. Nilai IP yang pernah
	 * diisi manual pada ruangan yang bendera "ikuti"-nya menyala (atau masih {@code null}) akan
	 * hilang secara permanen dengan cara ini.</p>
	 *
	 * <p><b>Catatan cakupan.</b> Sejauh yang terlihat di basis kode, kolom {@code ip} milik
	 * ruangan hanya dibaca/ditulis oleh layar masternya sendiri ({@code RuangAction}: satu
	 * label pada daftar dan satu kotak isian pada form). Tidak ditemukan kode yang benar-benar
	 * <i>menegakkan</i> pembatasan berdasarkan IP ruangan — nilai ini praktis berupa data
	 * pendataan, bukan kontrol akses. Jangan mengandalkannya sebagai pengaman.</p>
	 *
	 * @return daftar alamat IP dipisah titik koma tanpa spasi tepi, atau string kosong bila
	 *         tidak dibatasi
	 * @see #getIkutiIpGedung()
	 * @see Gedung#getIp()
	 */
	public String getIp() {
		gedung = getGedung();
		if (getIkutiIpGedung() && gedung != null) {
			ip = gedung.getIp();
		}
		return ip == null ? "" : ip.trim();
	}

	/**
	 * Mengisi daftar alamat IP ruangan.
	 *
	 * <p>Nilai ini hanya bertahan selama {@link #getIkutiIpGedung()} bernilai {@code false};
	 * bila tidak, {@link #getIp()} akan menimpanya dengan IP gedung.</p>
	 *
	 * @param ip daftar alamat IP dipisah titik koma; kosong berarti tanpa pembatasan
	 */
	public void setIp(String ip) {
		this.ip = ip;
	}

}
