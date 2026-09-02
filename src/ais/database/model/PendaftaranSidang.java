package ais.database.model;

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

/**
 * Baris pendaftaran seorang mahasiswa untuk mengikuti <b>sidang tugas akhir/skripsi</b>, beserta
 * dua bendera persetujuan (prodi dan keuangan) yang harus dipenuhi sebelum yang bersangkutan
 * dinyatakan boleh maju sidang. Tabel {@code public.pendaftaran_sidang}.
 *
 * <p>Baris ini <b>tidak</b> menyimpan identitas mahasiswa secara langsung: mahasiswa dijangkau
 * lewat rantai {@code pendaftaranSidang → }{@link #getSkripsi() skripsi}{@code  → }
 * {@link Skripsi#getMahasiswa() mahasiswa}. Konsekuensinya, satu baris pendaftaran selalu terikat
 * pada satu baris {@link Skripsi} tertentu, dan mahasiswa yang belum punya baris {@code Skripsi}
 * tidak dapat diwakili sama sekali.</p>
 *
 * <h3>PERINGATAN — fitur ini praktis MATI (kerangka yang tidak pernah diselesaikan)</h3>
 * <p>Penelusuran menyeluruh atas seluruh source tree menunjukkan bahwa <b>tidak ada satu pun jalur
 * kode yang membuat baris {@code PendaftaranSidang}</b>: tidak ada {@code new PendaftaranSidang()}
 * di luar konstruktor entity ini sendiri, dan tidak ada pemanggilan {@code save()}/{@code persist()}
 * terhadapnya. Yang ada hanya operasi <i>baca</i> dan <i>UPDATE dua bendera persetujuan</i>. Dengan
 * kata lain, tabel {@code pendaftaran_sidang} hanya bisa terisi lewat SQL manual, migrasi data
 * lama, atau modul di luar codebase ini. Bila tabel kosong, seluruh layar yang memakai entity ini
 * tidak akan pernah menemukan data.</p>
 * <p>Beberapa bukti pendukung bahwa modul ini berhenti di tengah jalan:</p>
 * <ul>
 *   <li>Berkas layar {@code WEB-INF/z/x/y/pages/master/pendaftaran_sidang_mahasiswa.zul} — satu-satunya
 *   kandidat layar pendaftaran sisi mahasiswa — menunjuk composer
 *   {@code ais.action.master.PendaftaranSidangMahasiswaAction} yang <b>tidak ada di codebase</b>.
 *   Layar itu hanya berisi window kosong dan akan gagal saat di-{@code apply}.</li>
 *   <li>Dua layar pemeriksaan yang ada ({@code cek_daftarsidang_prodi.zul} dan
 *   {@code cek_daftarsidang_keuangan.zul}) <b>tidak memuat tombol "Setuju"/"Tolak" sama sekali</b>,
 *   sehingga {@code onSetuju()}/{@code onTolak()} pada composer-nya tidak pernah dapat dipicu —
 *   lihat "Bug nyata pada jalur pemakaian" di bawah.</li>
 *   <li>Struktur entity ini jauh lebih miskin daripada saudara kembarnya yang benar-benar hidup,
 *   {@link PendaftaranWisuda} (lihat tabel perbandingan di bawah).</li>
 * </ul>
 *
 * <h3>Alur pendaftaran sidang yang BENAR-BENAR dipakai sistem</h3>
 * <p>Pendaftaran sidang tugas akhir yang berjalan hari ini <b>tidak melewati entity ini</b>,
 * melainkan lewat kolom pada {@link Skripsi} itu sendiri:</p>
 * <ol>
 *   <li>Operator/mahasiswa memilih gelombang lewat {@code SkripsiAction} sehingga
 *   {@link Skripsi#getGelombangPendaftaranSidangTugasAkhir()} terisi
 *   ({@link GelombangPendaftaranSidangTugasAkhir} membawa rentang tanggal, kuota, dan cakupan
 *   fakultas/jurusan/program).</li>
 *   <li>Persetujuan maju sidang dicatat di {@link Skripsi} ({@code Skripsi.getSetujuiSidang()}),
 *   <b>bukan</b> di {@link #getDisetujuiOlehProdi()}/{@link #getDisetujuiOlehKeuangan()} di sini.</li>
 *   <li>Penjadwalan tanggal dan ruang ditangani {@link JadwalSidangTugasAkhir}.</li>
 *   <li>Pelaporan gelombang sidang ({@code LaporanGelombangSidang},
 *   {@code LaporanRekapitulasiGelombangSidang}, {@code LaporanRekapitulasiJudisium}) dan dashboard
 *   {@code DashboardStatistikPengajuanSidangPerJurusan} semuanya membaca {@link Skripsi}, bukan
 *   tabel ini.</li>
 * </ol>
 * <p>Karena itu, angka pada laporan gelombang sidang <b>tidak berkorelasi</b> dengan isi tabel
 * {@code pendaftaran_sidang}. Jangan memakai tabel ini sebagai sumber data pendaftaran sidang.</p>
 *
 * <h3>Perbandingan dengan {@link PendaftaranWisuda} (saudara yang hidup)</h3>
 * <table border="1" summary="Perbandingan PendaftaranSidang vs PendaftaranWisuda">
 *   <tr><th>&nbsp;</th><th>{@code PendaftaranSidang} (class ini)</th><th>{@code PendaftaranWisuda}</th></tr>
 *   <tr><td>Relasi mahasiswa</td><td>hanya lewat {@code skripsi.mahasiswa}</td>
 *       <td>kolom {@code mahasiswa} <i>dan</i> {@code skripsi}</td></tr>
 *   <tr><td>Bendera persetujuan</td><td>2 ({@code prodi}, {@code keuangan})</td>
 *       <td>5+ (keuangan, administrasi, perpustakaan, perpustakaan fakultas, administrasi fakultas)
 *       ditambah belasan bendera kelengkapan berkas</td></tr>
 *   <tr><td>Data operasional</td><td>tidak ada</td>
 *       <td>nomor registrasi, nomor kursi, ukuran toga, tanggal daftar, relasi {@code Wisuda}</td></tr>
 *   <tr><td>Layar mahasiswa</td><td>ZUL menunjuk class yang tidak ada</td>
 *       <td>{@code PendaftaranWisudaMahasiswaAction} (aktif, membuat baris)</td></tr>
 *   <tr><td>Jalur pembuatan baris</td><td><b>tidak ada</b></td>
 *       <td>3 class ({@code PendaftaranWisudaMahasiswaAction},
 *       {@code AmbilDataMahasiswaMendaftarWisudaHelper}, {@code DetailwisudaHelper})</td></tr>
 * </table>
 * <p>{@code serialVersionUID} keduanya pun hanya berbeda satu digit
 * ({@code 2463822577548439808L} di sini vs {@code 2463852577548439808L} di sana) — indikasi kuat
 * bahwa salah satu adalah salinan dari yang lain.</p>
 *
 * <h3>Titik sentuh kode (lengkap)</h3>
 * <ul>
 *   <li>{@code ais.database.dao.PendaftaranSidangDao} / {@code PendaftaranSidangDaoImpl} — DAO
 *   generik tanpa method tambahan, diperoleh lewat
 *   {@code DaoFactory.getInstance().getPendaftaranSidangDao()}.</li>
 *   <li>{@code ais.action.master.PengecekanPendaftaranSidangProdiAction} — layar
 *   {@code cek_daftarsidang_prodi.zul}, menyetel {@link #setDisetujuiOlehProdi(Integer)}.</li>
 *   <li>{@code ais.action.master.PengecekanPendaftaranSidangKeuanganAction} — layar
 *   {@code cek_daftarsidang_keuangan.zul}, menyetel {@link #setDisetujuiOlehKeuangan(Integer)}.
 *   Kedua composer itu praktis identik hasil salin-tempel (bahkan {@code serialVersionUID}-nya
 *   sama persis) dan sama-sama masih membawa field {@code PendaftaranWisuda pendaftaranWisuda}
 *   yang tidak pernah dipakai.</li>
 *   <li>{@code ais.action.master.helper.AmbilDataMahasiswaDaftarSidangBanbox} — bandbox pemilih
 *   mahasiswa pada kedua layar di atas; ia mencari pada {@code PendaftaranSidang} lalu
 *   mengembalikan {@link Mahasiswa} hasil {@code skripsi.mahasiswa}. Karena kriterianya
 *   {@code createCriteria("skripsi").createCriteria("mahasiswa")} (INNER JOIN), baris dengan
 *   {@code skripsi} kosong tidak akan pernah muncul.</li>
 *   <li>{@code hibernate.cfg.xml} baris 946 — pendaftaran mapping.</li>
 * </ul>
 *
 * <h3>Bug nyata pada jalur pemakaian (didokumentasikan, TIDAK diperbaiki)</h3>
 * <ol>
 *   <li><b>Layar pemeriksaan pasti melempar {@code NullPointerException}.</b> Kedua composer
 *   mendeklarasikan {@code MyButtonConfig btnSetuju} dan {@code btnTolak} dan memanggil
 *   {@code btnTolak.setDisabled(...)} di akhir {@code onPilihMahasiswa()}, tetapi kedua ZUL tidak
 *   punya komponen ber-id {@code btnSetuju}/{@code btnTolak} (juga tidak punya {@code rowConfirm}).
 *   Autowire ZK membiarkan field itu {@code null}, sehingga menekan tombol "C H E C K" selalu
 *   berakhir NPE — setelah kolom nama/fakultas/jurusan/judul/pembimbing sempat terisi.</li>
 *   <li><b>{@code onSetuju()}/{@code onTolak()} tidak terjangkau.</b> Tidak ada
 *   {@code forward="onClick=onSetuju"} di ZUL manapun, jadi kedua bendera persetujuan di entity ini
 *   tidak pernah bisa diubah lewat UI.</li>
 *   <li><b>Query pemilihan baris tidak deterministik dan tanpa pengaman null.</b> Kedua composer
 *   memakai {@code createCriteria(PendaftaranSidang.class).createCriteria("skripsi")
 *   .add(eq("mahasiswa", mhs)).setMaxResults(1).uniqueResult()} — tanpa {@code addOrder} dan tanpa
 *   penyaringan gelombang/tahun akademik. Mahasiswa yang punya lebih dari satu baris {@code Skripsi}
 *   (mis. mengulang tugas akhir) akan dicocokkan ke baris <i>sembarang</i> menurut urutan database.
 *   Hasil {@code null} (kasus normal karena tabel tidak pernah terisi) langsung
 *   di-dereference pada baris berikutnya.</li>
 *   <li><b>Transaksi eksplisit dikomentari.</b> Pada kedua composer, {@code beginTransaction()} dan
 *   {@code commitTransaction()} di sekitar {@code update()} dinonaktifkan sebagai komentar; operasi
 *   bergantung sepenuhnya pada session/flush milik request.</li>
 *   <li><b>Gerbang hak akses hanya untuk baca.</b> {@code doAfterCompose()} kedua composer
 *   memeriksa {@code CommonPrivilages.checkPrevilages(CommonPrivilages.READ)}, sedangkan
 *   {@code onSetuju()}/{@code onTolak()} — yang menulis ke database — tidak memeriksa apa pun. Ini
 *   pola "inversi hak akses" yang berulang di banyak layar AIS. Dampak praktisnya nihil selama
 *   tombolnya memang tidak ada di ZUL, tetapi cacatnya akan langsung aktif begitu seseorang
 *   menambahkan tombol tersebut.</li>
 * </ol>
 *
 * <h3>Kuirk struktural</h3>
 * <ul>
 *   <li><b>Komentar generator menyesatkan.</b> Javadoc asli class ini berbunyi
 *   "Bank generated by hbm2java" — {@code serialVersionUID}-nya pun sama persis dengan
 *   {@link Bank}, {@code DetailKegiatan}, {@code JadwalPembayaran}, {@code Judisium},
 *   {@code LogUserActifity}, dan {@code ais.database.model.sirs.Pemeriksaan}. Ini jejak salin-tempel
 *   dari {@link Bank}, bukan petunjuk asal-usul tabel.</li>
 *   <li><b>{@code keterangan} membayangi field induk.</b> Class ini mendeklarasikan ulang
 *   {@code private String keterangan} sekaligus meng-override {@link #getKeterangan()} dan
 *   {@link #setKeterangan(String)}. Akibatnya field {@code keterangan} milik
 *   {@link GeneralValueObject} permanen {@code null}, dan — berbeda dari kontrak yang dijanjikan
 *   {@link GeneralValueObject#getKeterangan()} — getter di sini <b>bisa mengembalikan
 *   {@code null}</b>. Ini pengecualian kedua atas klaim itu yang ditemukan sejauh ini (yang pertama
 *   {@link Bank}).</li>
 *   <li><b>Properti warisan tidak terpetakan.</b> {@link GeneralValueObject} bukan
 *   {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 *   tidak memetakan properti induknya. Karena itu {@code id}, {@code oleh}, {@code olehId}, dan
 *   {@code tanggal_dirubah} <b>wajib</b> dideklarasikan ulang di sini; itu keharusan teknis, bukan
 *   duplikasi yang perlu "dirapikan". Konsekuensi lanjutannya: {@code nama} dan {@code nomorUrut}
 *   milik induk juga tidak terpetakan dan selalu {@code null} pada entity ini, sehingga
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} atas dua {@code PendaftaranSidang}
 *   praktis selalu mengembalikan {@code 0} (mengurutkan daftar entity ini adalah operasi kosong).</li>
 *   <li><b>Relasi satu arah.</b> {@link Skripsi} tidak punya koleksi balik ke entity ini, jadi
 *   satu-satunya arah navigasi adalah {@code PendaftaranSidang → Skripsi}.</li>
 *   <li><b>Diaudit Envers.</b> Anotasi {@link Audited} membuat setiap perubahan tersalin ke tabel
 *   revisi {@code pendaftaran_sidang_AUD}. Karena entity terdaftar di {@code hibernate.cfg.xml},
 *   ia juga terjangkau oleh endpoint reflektif generik AIS ({@code /Data}, {@code /Api}) walau
 *   tidak punya layar aktif — isinya sendiri tidak sensitif (referensi skripsi + dua bendera).</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Audit &amp; identitas</b>: {@link #getId()}, {@link #setId(Long)}, {@link #getOleh()},
 *   {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 *   {@link #toString()}.</li>
 *   <li><b>Relasi</b>: {@link #getSkripsi()}, {@link #setSkripsi(Skripsi)} — satu-satunya relasi,
 *   sekaligus satu-satunya getter di class ini yang berefek samping.</li>
 *   <li><b>Data pendaftaran</b>: {@link #getTanggalPengajuan()}, {@link #setTanggalPengajuan(Date)},
 *   {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Bendera persetujuan</b>: {@link #getDisetujuiOlehProdi()},
 *   {@link #setDisetujuiOlehProdi(Integer)}, {@link #getDisetujuiOlehKeuangan()},
 *   {@link #setDisetujuiOlehKeuangan(Integer)}.</li>
 * </ul>
 * <p>Tidak ada method bisnis, method utilitas statis, maupun query di class ini; seluruh logika
 * berada di composer pemakainya.</p>
 *
 * @see Skripsi
 * @see GelombangPendaftaranSidangTugasAkhir
 * @see JadwalSidangTugasAkhir
 * @see PendaftaranWisuda
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pendaftaran_sidang")

public class PendaftaranSidang extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai tetap agar instance yang tersimpan di sesi HTTP/ZK tetap dapat
	 * dibaca setelah class dikompilasi ulang.
	 *
	 * <p>Nilainya <b>sama persis</b> dengan {@link Bank}, {@code DetailKegiatan},
	 * {@code JadwalPembayaran}, {@code Judisium}, {@code LogUserActifity}, dan
	 * {@code ais.database.model.sirs.Pemeriksaan} — jejak salin-tempel yang sama dengan komentar
	 * "Bank generated by hbm2java" pada Javadoc asli class ini. Praktik ini tidak berbahaya
	 * (class-class itu berbeda dan tidak pernah saling dideserialisasi), tetapi menghilangkan
	 * manfaat {@code serialVersionUID} sebagai penanda versi per class.
	 */
	private static final long serialVersionUID = 2463822577548439808L;

	/**
	 * Kunci utama (kolom {@code id}, {@code IDENTITY}). Bernilai {@code null} selama baris belum
	 * pernah disimpan. Menyembunyikan properti {@code id} milik {@link GeneralValueObject} yang
	 * tidak terpetakan Hibernate.
	 */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah, dengan <b>penjagaan anti-timpa</b>: nilai {@code null} atau
	 * yang hanya berisi spasi <b>diabaikan</b> (nilai lama dipertahankan), bukan disimpan. Pola ini
	 * dipakai seragam di seluruh entity AIS agar interceptor audit tidak menghapus jejak yang sudah
	 * ada ketika konteks pengguna kebetulan kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah, dengan penjagaan anti-timpa yang sama seperti
	 * {@link #setOlehId(String)}: {@code null}/kosong diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code UPDATE}
	 * dieksekusi, dan mendelegasikan pengisian jejak audit (pengguna + waktu) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 *
	 * <p>Catatan tata letak: deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang
	 * sama oleh generator sehingga tidak dapat diberi Javadoc tersendiri. Nilai awalnya adalah waktu
	 * server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()}), bukan waktu JVM mentah.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Umumnya dipanggil oleh interceptor audit, bukan oleh kode
	 * layar.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks entity: isi kolom {@code keterangan} apa adanya.
	 *
	 * <p>Dua kuirk yang perlu diketahui: (1) method ini membaca <b>field mentah</b> milik class ini,
	 * bukan {@link #getKeterangan()}, dan tidak melakukan normalisasi apa pun — jadi ia
	 * <b>dapat mengembalikan {@code null}</b>, berbeda dari kebanyakan entity AIS lain yang
	 * mengembalikan string kosong; (2) karena {@code keterangan} tidak pernah diisi oleh kode mana
	 * pun di codebase ini (lihat catatan "fitur mati" pada Javadoc class), praktisnya nilai
	 * {@code null} adalah kasus normal. Jangan pakai method ini sebagai label layar, dan jangan
	 * menyisipkan hasilnya langsung ke komponen ZK yang tidak menoleransi {@code null}.
	 *
	 * @return isi {@code keterangan}, atau {@code null} bila belum diisi
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Baris {@link Skripsi} yang didaftarkan untuk sidang — <b>satu-satunya jalan</b> untuk sampai
	 * ke mahasiswa, jurusan, fakultas, judul, dan dosen pembimbing dari sebuah pendaftaran. Kolom
	 * {@code skripsi}, {@code nullable = true}.
	 */
	private Skripsi skripsi;

	/**
	 * Catatan bebas atas pendaftaran ini (kolom {@code keterangan}). <b>Membayangi</b> field
	 * dengan nama sama pada {@link GeneralValueObject}, yang karena itu permanen {@code null}.
	 * Tidak ada kode di codebase ini yang pernah mengisinya.
	 */
	private String keterangan;

	/**
	 * Waktu pengajuan pendaftaran (kolom {@code tanggal_pengajuan}); diinisialisasi ke waktu server
	 * saat objek dibuat. Tidak pernah disetel ulang oleh kode mana pun, sehingga nilainya selalu
	 * "saat baris dibuat" — dan karena tidak ada jalur pembuatan baris di codebase ini, nilainya
	 * hanya bermakna untuk baris yang berasal dari luar aplikasi.
	 */
	private Date tanggalPengajuan = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Bendera "bagian keuangan menyetujui" (kolom {@code disetujui_oleh_keuangan}), tri-state
	 * de-facto: {@code 0} = belum/ditolak (default), {@code 1} = disetujui, {@code null} = tidak
	 * pernah diisi. Diubah dari {@code PengecekanPendaftaranSidangKeuanganAction}.
	 */
	private Integer disetujuiOlehKeuangan = 0;

	/**
	 * Bendera "bagian prodi menyetujui" (kolom {@code disetujui_oleh_prodi}) dengan semantik yang
	 * sama seperti {@link #disetujuiOlehKeuangan}. Diubah dari
	 * {@code PengecekanPendaftaranSidangProdiAction}.
	 */
	private Integer disetujuiOlehProdi = 0;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Objek baru sudah membawa
	 * {@code tanggalPengajuan} dan {@code tanggal_dirubah} berisi waktu server serta kedua bendera
	 * persetujuan bernilai {@code 0}; {@code skripsi} dan {@code keterangan} dibiarkan {@code null}.
	 */
	public PendaftaranSidang() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom dipetakan dengan {@code insertable = false} karena nilainya dihasilkan basis data
	 * ({@code IDENTITY}/{@code serial}), sehingga tidak pernah ikut pada pernyataan {@code INSERT}.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Praktis hanya dipanggil Hibernate; kode layar tidak pernah menetapkan id
	 * sendiri.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan catatan bebas atas pendaftaran ini.
	 *
	 * <p><b>Perhatian — meng-override kontrak induk.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil yang tidak pernah {@code null}
	 * (mengembalikan {@code ""} bila kosong); override di sini <b>tidak</b> menormalkan apa pun dan
	 * dapat mengembalikan {@code null}. Pemanggil generik yang mengandalkan janji induk (mis. kode
	 * yang langsung memanggil {@code .trim()} atau {@code .isEmpty()} atas hasilnya) bisa melempar
	 * {@code NullPointerException} untuk entity ini.
	 *
	 * @return isi {@code keterangan}, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas atas pendaftaran ini. Tanpa validasi; {@code null} diterima. Menulis ke
	 * field milik class ini, bukan field {@link GeneralValueObject} yang dibayanginya.
	 *
	 * @param keterangan catatan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menautkan pendaftaran ini ke satu baris tugas akhir.
	 *
	 * @param skripsi baris {@link Skripsi} yang didaftarkan, boleh {@code null}
	 */
	public void setSkripsi(Skripsi skripsi) {
		this.skripsi = skripsi;
	}

	/**
	 * Mengembalikan baris {@link Skripsi} yang didaftarkan untuk sidang.
	 *
	 * <p><b>Getter ini tidak murni.</b> Ia memanggil {@link GeneralValueObject#check(Object)} dan
	 * <b>menugaskan hasilnya kembali ke field</b>. Bila entity sudah terlepas dari sesi
	 * ({@code detached}) dan proxy lazy belum terinisialisasi, {@code check()} dapat membuka sesi
	 * Hibernate sendiri untuk memuat ulang objek lalu menutupnya kembali — biaya I/O yang tidak
	 * terlihat dari sisi pemanggil. Objek yang dikembalikan bisa merupakan instance <i>lain</i>
	 * (kanonik dari {@code EntityIdentityMap}, dari cache, atau hasil reload) dan bukan proxy
	 * semula. {@code check()} tidak pernah melempar exception dan tidak pernah mengembalikan
	 * {@code null} untuk argumen non-null; kegagalan resolusi bersifat senyap.
	 *
	 * <p>Relasi ini bersifat <b>lazy</b> dengan cascade {@code PERSIST}/{@code MERGE} — menyimpan
	 * pendaftaran ikut menyimpan/menggabungkan baris {@code Skripsi} yang tertaut, tetapi
	 * <b>tidak</b> menghapusnya ({@code REMOVE} sengaja tidak disertakan).
	 *
	 * @return baris tugas akhir yang didaftarkan, atau {@code null} bila kolom {@code skripsi}
	 *         kosong
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skripsi", nullable = true)
	public Skripsi getSkripsi() {
		skripsi = check(skripsi);
		return skripsi;
	}

	/**
	 * Menyetel bendera persetujuan bagian keuangan.
	 *
	 * <p>Satu-satunya pemanggil adalah {@code PengecekanPendaftaranSidangKeuanganAction}:
	 * {@code onSetuju()} mengirim {@code 1} dan {@code onTolak()} mengirim {@code 0}, masing-masing
	 * setelah dialog konfirmasi, lalu memanggil {@code PendaftaranSidangDao.update(...)}. Kedua
	 * handler itu <b>tidak dapat dipicu dari UI</b> karena tombol pemicunya tidak ada di
	 * {@code cek_daftarsidang_keuangan.zul}, dan keduanya tidak memeriksa hak akses tulis.
	 *
	 * @param disetujuiOlehKeuangan {@code 1} bila disetujui, {@code 0} bila tidak; {@code null}
	 *                              diterima tanpa validasi
	 */
	public void setDisetujuiOlehKeuangan(Integer disetujuiOlehKeuangan) {
		this.disetujuiOlehKeuangan = disetujuiOlehKeuangan;
	}

	/**
	 * Mengembalikan bendera persetujuan bagian keuangan.
	 *
	 * <p>Catatan pemetaan: atribut {@code length = 1} pada {@code @Column} tidak berpengaruh untuk
	 * kolom numerik — ia hanya bermakna bagi tipe string. Pembacaan yang benar adalah
	 * {@code equals(1)} (seperti dilakukan composer-nya), bukan pengecekan "tidak nol", karena
	 * nilainya bisa {@code null} untuk baris warisan.
	 *
	 * @return {@code 1} bila keuangan sudah menyetujui, {@code 0} bila belum/ditolak, atau
	 *         {@code null} bila kolom tidak pernah diisi
	 */
	@Column(name = "disetujui_oleh_keuangan", length = 1)
	public Integer getDisetujuiOlehKeuangan() {
		return disetujuiOlehKeuangan;
	}

	/**
	 * Menyetel bendera persetujuan program studi.
	 *
	 * <p>Satu-satunya pemanggil adalah {@code PengecekanPendaftaranSidangProdiAction}, dengan
	 * mekanisme dan keterbatasan yang persis sama seperti
	 * {@link #setDisetujuiOlehKeuangan(Integer)} — kedua layar itu memang salinan satu sama lain,
	 * sampai-sampai id komponen centang di layar prodi pun masih bernama {@code cekKeuangan}.
	 *
	 * @param disetujuiOlehProdi {@code 1} bila disetujui, {@code 0} bila tidak; {@code null}
	 *                           diterima tanpa validasi
	 */
	public void setDisetujuiOlehProdi(Integer disetujuiOlehProdi) {
		this.disetujuiOlehProdi = disetujuiOlehProdi;
	}

	/**
	 * Mengembalikan bendera persetujuan program studi. Semantik dan catatan pemetaannya sama dengan
	 * {@link #getDisetujuiOlehKeuangan()}.
	 *
	 * @return {@code 1} bila prodi sudah menyetujui, {@code 0} bila belum/ditolak, atau {@code null}
	 *         bila kolom tidak pernah diisi
	 */
	@Column(name = "disetujui_oleh_prodi", length = 1)
	public Integer getDisetujuiOlehProdi() {
		return disetujuiOlehProdi;
	}

	/**
	 * Menyetel waktu pengajuan pendaftaran. Tidak dipanggil dari mana pun di codebase ini.
	 *
	 * @param tanggalPengajuan waktu pengajuan baru
	 */
	public void setTanggalPengajuan(Date tanggalPengajuan) {
		this.tanggalPengajuan = tanggalPengajuan;
	}

	/**
	 * Mengembalikan waktu pengajuan pendaftaran (dipetakan sebagai {@code TIMESTAMP} pada kolom
	 * {@code tanggal_pengajuan}).
	 *
	 * @return waktu pengajuan; tidak pernah {@code null} pada objek baru karena field
	 *         diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pengajuan")
	public Date getTanggalPengajuan() {
		return tanggalPengajuan;
	}

}
