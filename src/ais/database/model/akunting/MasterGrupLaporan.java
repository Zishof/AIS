package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>MasterGrupLaporan &mdash; katalog SEKSI (grup besar) laporan keuangan</h3>
 *
 * <p><b>Peran sesungguhnya (TERVERIFIKASI dari kode pemakainya, bukan asumsi).</b> Entity ini
 * memetakan tabel <code>akunting.master_grup_laporan</code> dan berisi <b>daftar seksi/judul blok</b>
 * yang muncul di laporan keuangan cetak &mdash; misalnya "Aktiva", "Kewajiban", "Pendapatan",
 * "Operasional", "Investasi". Yang perlu ditegaskan sejak awal: entity ini <b>bukan</b> akar sebuah
 * pohon hierarki laporan, dan <b>tidak memiliki satu pun kolom relasi</b> (tidak ada
 * {@code @ManyToOne}/{@code @OneToMany} sama sekali di berkas ini). Ia adalah katalog datar berisi
 * label + nomor urut + satu bendera tampilan. Seluruh keterhubungannya datang dari <b>arah lain</b>:
 * {@code KelompokLaporan} yang menyimpan kolom FK <code>master_grup_laporan</code> ke sini.</p>
 *
 * <p><b>Hierarki laporan keuangan yang SESUNGGUHNYA (verifikasi lintas berkas).</b> Struktur laporan
 * di AIS dirakit dari empat tabel, dan {@code KelompokLaporan} adalah simpul pusatnya karena ia
 * memegang <b>dua FK sekaligus</b> ke dua sumbu yang <b>saling ortogonal</b> (bukan berjenjang):</p>
 * <ol>
 *   <li><b>{@code JenisLaporan}</b> (tabel <code>akunting.jenis_laporan</code>) &mdash; sumbu
 *       "laporan yang mana": Neraca, Rugi Laba, Arus Kas. Dirujuk {@code KelompokLaporan.jenisLaporan}.</li>
 *   <li><b>{@code MasterGrupLaporan}</b> (berkas ini) &mdash; sumbu "seksi di dalam laporan itu":
 *       Aktiva / Kewajiban / Pendapatan / dst. Dirujuk {@code KelompokLaporan.masterGrupLaporan}.</li>
 *   <li><b>{@code KelompokLaporan}</b> &mdash; satu BARIS laporan (mis. "Kas dan Setara Kas"),
 *       membawa {@code urut}, {@code aktif}, dan {@code tampilkanAkunRinci} miliknya sendiri.</li>
 *   <li><b>{@code KelompokLaporanPunyaAkun}</b> &mdash; tabel jembatan yang menempelkan
 *       {@code Akun} (bagan akun / Chart of Accounts) ke sebuah {@code KelompokLaporan},
 *       lengkap dengan {@code nomorurut} akun di dalam baris tersebut.</li>
 * </ol>
 * <p>Sehingga saat dicetak, susunannya menjadi: <em>Jenis Laporan &rarr; Grup Laporan (entity ini,
 * sebagai judul blok) &rarr; Kelompok Laporan (baris) &rarr; Akun (rincian)</em>. Kedua FK di
 * {@code KelompokLaporan} bersifat {@code nullable}, jadi sebuah baris laporan bisa saja yatim
 * &mdash; perender HTML menampung kasus itu dengan grup semu berlabel "Lainnya" (id -1).</p>
 *
 * <p><b>Isi entity.</b> Hanya delapan properti: {@code id}, {@code nama} (judul seksi, wajib),
 * {@code keterangan} (di UI diberi label "Sub Grup", dipakai sebagai anak judul), {@code nomorUrut}
 * (urutan seksi di laporan), {@code tampilkanAkunRinci} (bendera tampilan), ditambah tiga kolom
 * jejak audit yang diulang di setiap entity repo ini ({@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}). <b>Tidak ada satu pun kolom nominal, tarif, tanda (+/-), maupun rumus
 * di entity ini</b> &mdash; ia murni metadata penyajian.</p>
 *
 * <p><b>Konsekuensi penting: tidak ada kolom tenant.</b> Tabel ini tidak punya kolom
 * sekolah/yayasan/satuan kerja sama sekali, dan {@code MasterGrupLaporanAction.initCriteria()} tidak
 * memasang penyaring tenant apa pun. Katalog seksi laporan karena itu bersifat <b>GLOBAL untuk
 * seluruh tenant satu instalasi</b>: satu operator yang mengubah nomor urut atau nama seksi
 * mengubah tata letak laporan keuangan <em>semua</em> tenant. Ini bukan "fail-open bersyarat"
 * (penyaring yang gagal menyala), melainkan ketiadaan kolom &mdash; senapas dengan
 * {@code Closing} dan {@code ProsesTransferStandingInstruction} pada modul yang sama.</p>
 *
 * <p><b>Siapa yang memakai (TERVERIFIKASI lewat grep menyeluruh).</b></p>
 * <ul>
 *   <li><b>CRUD:</b> {@code ais.action.master.akunting.MasterGrupLaporanAction} lewat menu
 *       "Grup Laporan" (<code>/pages/master/akunting/master_grup_laporan.zul</code>), termasuk
 *       jalur impor Excel massal ({@code Common.uploadData}) yang hanya tampil bila pengguna
 *       memegang hak tambah + ubah + hapus sekaligus.</li>
 *   <li><b>Perakit struktur:</b> {@code KelompokLaporanDanDetailAction} dan
 *       {@code KelompokLaporanAction} memakai entity ini sebagai isi combobox dan sebagai kunci
 *       penyaring; impor Excel di sana hanya <em>mencari</em> grup lewat
 *       {@code Common.getContentAsObject} dan tidak pernah membuat baris grup baru.</li>
 *   <li><b>Pemilih laporan cetak:</b> empat kelas {@code ais.action.report.format1.akunting.*}
 *       ({@code LaporanAkuntingSaldoBulanMaster}, {@code LaporanAkunting2Bulan},
 *       {@code LaporanAkunting12Bulan}, {@code LaporanAkunting2Tahun}) yang mengirim
 *       {@code getId()} sebagai parameter Jasper <code>$P{grup}</code> dan
 *       {@code getNama()} sebagai <code>$P{nama}</code>.</li>
 *   <li><b>Perender HTML baru:</b> {@code LaporanKeuanganCoaHelper.susun()} yang mengelompokkan
 *       baris laporan ke dalam objek {@code Grup} berkunci {@code getId()}, dan
 *       {@code NewUiLaporanAkuntingSaldoBulanController} yang membangun daftar pilihan grup.</li>
 *   <li><b>SQL mentah:</b> {@code DashboardAkuntingHelper}, {@code DashboardAkuntingTahunHelper},
 *       {@code DasboardAkunting}, {@code LaporanKantinUtil}, serta dua helper REST
 *       ({@code PemetaanAkunHelper}, {@code TutupBukuHelper}) yang hanya me-<em>join</em> tabel ini
 *       untuk mengambil label; tidak satu pun menulis ke tabel ini.</li>
 *   <li><b>Berkas Jasper:</b> enam <code>*.jrxml</code> di <code>webapp/report/akunting/</code>
 *       melakukan <code>inner join akunting.master_grup_laporan e</code> dan mengurutkan hasil
 *       dengan <code>e.nomor_urut</code>.</li>
 * </ul>
 *
 * <p><b>Tidak ada permukaan REST tulis.</b> Verifikasi negatif yang menenangkan: entity ini tidak
 * tersentuh {@code MasterKeuanganApiHelper} maupun helper API lain yang menjaga master keuangan,
 * sehingga pola fail-open {@code bolehAksi()} yang berulang di modul akunting <b>tidak</b>
 * menjangkau tabel ini. Demikian pula pola "checkbox grid tanpa gerbang hak": kolom "Rinci" pada
 * grid daftar hanyalah {@code Label} "Ya"/"Tidak" yang dibaca, bukan checkbox yang menulis langsung
 * ke basis data.</p>
 *
 * <p><b>Soal risiko nilai/tanda salah pada laporan keuangan.</b> Karena entity ini tidak menyimpan
 * angka maupun tanda, ia <b>tidak</b> bisa merusak nominal laporan secara langsung. Yang perlu
 * diketahui pembaca berkas ini adalah bahwa dua jalur penyajian struktur yang sama memakai
 * <b>konvensi tanda yang berbeda</b>: berkas Jasper menjumlahkan <code>(debet - kredit)</code>
 * mentah tanpa pernah mengalikannya dengan kolom {@code Akun.debetCredit} (di
 * <code>laporan_keuangan_arus_kasi.jrxml</code> kolom itu bahkan di-<em>select</em> dan
 * dideklarasikan sebagai field, tetapi tidak dipakai di satu pun ekspresi), sedangkan
 * {@code LaporanKeuanganCoaHelper.susun()} mengalikan saldo dengan {@code akun.getDebetCredit()}.
 * Akibatnya seksi yang sama bisa tampil dengan tanda berlawanan tergantung layar yang dibuka, dan
 * kerusakan akibat {@code debit_credit} bernilai salah hanya menjalar ke jalur HTML, bukan ke
 * laporan Jasper.</p>
 *
 * <p><b>Kuirk yang perlu diketahui sebelum menyentuh berkas ini.</b></p>
 * <ol>
 *   <li>{@link #reloadDefault()} <b>seluruh badannya dikomentari</b> &mdash; method ini no-op,
 *       padahal masih dipanggil {@code ais.common.InitData} beserta baris {@code System.out.println}
 *       yang mengesankan ada penyemaian data. Struktur Arus Kas bawaan yang dulu dibuatnya
 *       (Piutang Usaha, Kewajiban Penggajian, Pendapatan Operasional, dst.) kini harus dibuat
 *       manual oleh operator.</li>
 *   <li>{@link #compareTo(GeneralValueObject)} menimpa versi induk dengan badan yang identik, tetapi
 *       untuk entity ini <b>hanya cabang pertama yang pernah tereksekusi</b>: {@link #getNomorUrut()}
 *       di kelas ini menyubstitusi {@code null} menjadi {@code 0}, sehingga syarat
 *       {@code getNomorUrut() != null} selalu terpenuhi dan cabang NIM/nama/keterangan menjadi kode
 *       mati. Dua grup dengan nomor urut sama dianggap setara dan urutan relatifnya tidak
 *       deterministik.</li>
 *   <li>{@link #getTampilkanAkunRinci()} adalah satu-satunya properti tanpa anotasi
 *       {@code @Column}, sehingga nama kolomnya mengikuti nama properti apa adanya
 *       (<code>tampilkanakunrinci</code> &mdash; terkonfirmasi dari SQL Jasper
 *       <code>e.tampilkanakunrinci as tampil</code>). Bendera ini <b>hanya dibaca oleh tiga</b>
 *       berkas Jasper (arus kas dan dua varian mutasi); perender HTML
 *       {@code LaporanKeuanganCoaHelper} sama sekali mengabaikannya dan hanya menghormati
 *       {@code KelompokLaporan.tampilkanAkunRinci}. Jadi mematikan "tampilkan akun rinci" di layar
 *       ini tidak berpengaruh pada sebagian besar laporan.</li>
 *   <li>Tidak ada indeks unik maupun pemeriksaan duplikat: dua baris dengan {@code nama} dan
 *       {@code keterangan} identik boleh berdampingan. Di jalur Jasper keduanya diam-diam
 *       <b>menyatu</b> menjadi satu seksi karena SQL mengelompokkan dengan
 *       <code>upper(trim(e.nama)), upper(trim(e.keterangan))</code>, sedangkan di jalur HTML
 *       keduanya tetap terpisah karena dikelompokkan berdasarkan {@code id}. Data yang sama bisa
 *       tampak berbeda strukturnya di dua laporan.</li>
 *   <li>Snapshot menu mendaftarkan entity ini dua kali; entri kedua menunjuk
 *       <code>/pages/master/sirs/akunting/master_grup_laporan.zul</code> yang <b>tidak ada</b> di
 *       repo &mdash; menu mati.</li>
 * </ol>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject},
 * yang <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}: Hibernate tidak memetakan
 * properti induknya. Karena itu {@code nama}, {@code keterangan}, dan {@code nomorUrut} sengaja
 * <b>dideklarasikan ulang</b> di kelas ini &mdash; itu keharusan teknis agar kolomnya terpetakan,
 * bukan duplikasi keliru. Konsekuensinya, field induk yang senama tetap ada namun selamanya
 * {@code null}, dan setiap getter di kelas ini menutupi (<em>shadow</em>) getter induk.</p>
 *
 * <p><b>Pengelompokan method di berkas ini.</b> (1) jejak audit &mdash; {@link #getOleh()},
 * {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, {@link #onUpdate()};
 * (2) identitas &amp; penyajian &mdash; {@link #getId()}, {@link #setId(Long)},
 * {@link #toString()}, {@link #compareTo(GeneralValueObject)}; (3) isi bisnis &mdash;
 * {@link #getNama()}, {@link #setNama(String)}, {@link #getKeterangan()},
 * {@link #setKeterangan(String)}, {@link #getNomorUrut()}, {@link #setNomorUrut(Integer)},
 * {@link #getTampilkanAkunRinci()}, {@link #setTampilkanAkunRinci(Boolean)};
 * (4) penyemaian &mdash; {@link #reloadDefault()} (kini no-op).</p>
 *
 * <p>Entity ditandai {@code @Audited} (Envers), sehingga setiap versi baris digandakan ke tabel
 * revisi <code>master_grup_laporan_aud</code>; grid CRUD menampilkan riwayat itu lewat
 * {@code RevisiHelper}. {@code dynamicInsert}/{@code dynamicUpdate} aktif, jadi hanya kolom yang
 * benar-benar berubah yang ikut dalam pernyataan SQL.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.KelompokLaporan
 * @see ais.database.model.akunting.JenisLaporan
 * @see ais.database.model.akunting.KelompokLaporanPunyaAkun
 * @see ais.database.model.akunting.Akun
 * @see ais.action.master.akunting.MasterGrupLaporanAction
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "master_grup_laporan")
public class MasterGrupLaporan extends GeneralValueObject {

	/**
	 * Nama seksi bawaan untuk kelompok harta/aset pada Neraca.
	 *
	 * <p>Dipakai sebagai <b>pilihan awal</b> combobox "Nama Grup" ketika tabel masih kosong &mdash;
	 * di {@code MasterGrupLaporanAction.init(...)}, {@code KelompokLaporanDanDetailAction}, dan
	 * empat kelas laporan {@code LaporanAkunting*}. Konstanta ini <b>tidak</b> ditegakkan sebagai
	 * daftar tertutup: operator bebas mengetik nama lain, dan sekali ada satu baris di tabel,
	 * daftar pilihan diambil dari basis data sehingga konstanta ini tidak lagi disodorkan.</p>
	 */
	public static final String AKTIVA = "Aktiva";

	/**
	 * Nama seksi bawaan untuk kelompok pendapatan pada laporan Rugi Laba. Sama seperti
	 * {@link #AKTIVA}, hanya berperan sebagai isian awal combobox saat tabel masih kosong.
	 */
	public static final String PENDAPATAN = "Pendapatan";

	/**
	 * Nama seksi bawaan untuk kelompok utang/kewajiban pada Neraca. Sama seperti {@link #AKTIVA},
	 * hanya berperan sebagai isian awal combobox saat tabel masih kosong.
	 */
	public static final String KEWAJIBAN = "Kewajiban";

	/**
	 * Nama seksi bawaan untuk aktivitas operasional pada laporan Arus Kas.
	 *
	 * <p>Berbeda dari tiga konstanta di atas, {@code OPERASIONAL} dan {@link #INVESTASI}
	 * <b>tidak</b> ikut disodorkan oleh formulir CRUD {@code MasterGrupLaporanAction}; keduanya
	 * hanya muncul di daftar cadangan {@code NewUiLaporanAkuntingSaldoBulanController.semuaNama()}
	 * dan di {@code LaporanAkuntingSaldoBulanMaster}. Keduanya juga merupakan nilai yang dulu
	 * ditulis oleh penyemai {@link #reloadDefault()} yang kini sudah dimatikan &mdash; itulah
	 * sebabnya konstanta ini terasa "yatim" dibanding tiga konstanta Neraca/Rugi&nbsp;Laba.</p>
	 */
	public static final String OPERASIONAL = "Operasional";

	/**
	 * Nama seksi bawaan untuk aktivitas investasi pada laporan Arus Kas. Berbagi keterbatasan yang
	 * sama dengan {@link #OPERASIONAL}: tidak ditawarkan formulir CRUD, hanya dipakai daftar
	 * cadangan pemilih laporan.
	 */
	public static final String INVESTASI = "Investasi";

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya <b>identik</b> dengan {@code serialVersionUID} milik {@code KelompokLaporan} dan
	 * {@code JenisLaporan} &mdash; jejak bahwa ketiga entity dihasilkan dari cetakan hbm2java yang
	 * sama pada April 2010, bukan tanda hubungan pewarisan apa pun.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, dipetakan ke kolom <code>id</code> (IDENTITY, diisi basis data). */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; diisi interseptor audit, lihat {@link #setOleh(String)}. */
	private String oleh;

	/** Identitas (id) pengguna terakhir yang mengubah baris; lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyentuh baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah distempel
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>penjaga anti-kosong</b>.
	 *
	 * <p>Bila {@code olehId} bernilai {@code null} atau hanya berisi spasi, method langsung
	 * kembali tanpa mengubah apa pun &mdash; jadi stempel audit yang sudah terisi <b>tidak bisa
	 * dikosongkan lewat setter ini</b>. Pola ini seragam di seluruh entity repo dan disengaja agar
	 * jejak audit tidak hilang saat sebuah objek disalin/di-<em>merge</em> dari sumber yang tidak
	 * membawa informasi pengguna.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris ini dalam bentuk <code>id + "-" + nama</code>.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field</b> {@code nama} secara langsung, bukan
	 * lewat {@link #getNama()}, sehingga hasilnya <b>tidak</b> ter-<em>trim</em>. Nilai yang
	 * dihasilkan dipakai antara lain oleh {@code Common.getContentAsObject} dan jejak
	 * {@code System.out.println} pada jalur impor Excel {@code KelompokLaporanDanDetailAction},
	 * jadi perubahan formatnya berdampak ke sana.</p>
	 *
	 * @return gabungan id dan nama, mis. {@code "12-Aktiva"}; bagian id berbunyi {@code "null"}
	 *         bila baris belum tersimpan
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjaga anti-kosong yang sama dengan
	 * {@link #setOlehId(String)}: nilai {@code null} atau berisi spasi saja diabaikan dan stempel
	 * lama dipertahankan.
	 *
	 * @param oleh nama pengguna; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyentuh baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah distempel
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Pengait siklus hidup JPA {@code @PreUpdate} yang menstempel jejak audit sesaat sebelum
	 * baris diperbarui, sekaligus deklarasi field {@code tanggal_dirubah} yang menampung stempel
	 * waktu tersebut.
	 *
	 * <p>Keduanya sengaja ditulis pada satu baris fisik oleh perkakas pembangkit repo ini, jadi
	 * jangan kaget menemukan deklarasi field di ujung baris method. {@code onUpdate()} adalah
	 * implementasi dari satu-satunya method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}, dan mendelegasikan seluruh pekerjaannya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} &mdash; yang mengisi
	 * {@link #setOleh(String)}/{@link #setOlehId(String)}/{@link #setTanggal_dirubah(Date)} dari
	 * sesi pengguna yang sedang aktif.</p>
	 *
	 * <p>Nilai awal {@code tanggal_dirubah} diambil dari {@code ais.ui.util.WaktuUtil.getDate()}
	 * saat objek dibuat, sehingga baris baru pun selalu memiliki stempel waktu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}, setter ini <b>tanpa penjaga</b>: nilai {@code null}
	 * diterima apa adanya dan akan mengosongkan kolom.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu (presisi TIMESTAMP); praktis tidak pernah {@code null} karena field
	 *         diberi nilai awal saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Judul seksi laporan, mis. "Aktiva"/"Kewajiban"/"Pendapatan". Wajib diisi (kolom
	 * {@code nullable = false}) dan divalidasi ulang di {@code MasterGrupLaporanAction.onSave}.
	 * Dideklarasikan ulang dari induk karena induk tidak dipetakan Hibernate.
	 */
	private String nama;

	/**
	 * Anak judul seksi; di formulir CRUD berlabel <b>"Sub Grup"</b>, bukan "keterangan". Dicetak
	 * bersebelahan dengan {@link #nama} pada laporan HTML dan menjadi kolom
	 * {@code jenis_laporan2} pada berkas Jasper. Boleh {@code null}.
	 */
	private String keterangan;

	/**
	 * Urutan tampil seksi di dalam sebuah laporan (makin kecil makin awal). Dipakai
	 * <code>order by e.nomor_urut</code> pada seluruh berkas Jasper laporan keuangan dan menjadi
	 * kunci tunggal {@link #compareTo(GeneralValueObject)}. Boleh {@code null} di basis data,
	 * tetapi {@link #getNomorUrut()} menyubstitusinya menjadi {@code 0}.
	 */
	private Integer nomorUrut;

	/**
	 * Bendera "tampilkan akun secara rinci" untuk seluruh seksi. <b>Hanya dihormati tiga berkas
	 * Jasper</b> (arus kas dan dua varian mutasi, sebagai kolom <code>e.tampilkanakunrinci as
	 * tampil</code>); perender HTML {@code LaporanKeuanganCoaHelper} mengabaikannya sepenuhnya dan
	 * memakai bendera senama milik {@code KelompokLaporan}. Boleh {@code null}, dan
	 * {@link #getTampilkanAkunRinci()} menyubstitusinya menjadi {@code true}.
	 */
	private Boolean tampilkanAkunRinci;

	/**
	 * Membandingkan dua entity untuk keperluan pengurutan daftar.
	 *
	 * <p><b>Tujuan.</b> Menyediakan urutan alami agar {@code Collections.sort(...)} pada
	 * {@code LaporanAkuntingSaldoBulanMaster} dan
	 * {@code NewUiLaporanAkuntingSaldoBulanController.grupTersedia(...)} menghasilkan daftar
	 * pilihan grup yang berurut sesuai {@link #getNomorUrut()}.</p>
	 *
	 * <p><b>Cara kerja &amp; kasus tepi penting.</b> Badan method ini merupakan salinan persis dari
	 * {@link ais.database.model.GeneralValueObject#compareTo(GeneralValueObject)}: mencoba empat
	 * kunci berurutan &mdash; nomor urut, NIM, nama, lalu keterangan &mdash; dan memakai kunci
	 * pertama yang tersedia di <b>kedua</b> objek. Namun untuk entity ini <b>cabang pertama selalu
	 * menang</b>: {@link #getNomorUrut()} di kelas ini tidak pernah mengembalikan {@code null}
	 * (mengembalikan {@code 0} bila field kosong), sehingga tiga cabang berikutnya menjadi
	 * <b>kode mati</b> selama {@code arg0} juga sebuah {@code MasterGrupLaporan}. Bila {@code arg0}
	 * kebetulan entity lain yang {@code getNomorUrut()}-nya {@code null}, barulah perbandingan
	 * jatuh ke NIM/nama/keterangan.</p>
	 *
	 * <p><b>Efek samping &amp; risiko.</b> Dua grup dengan nomor urut sama menghasilkan {@code 0}
	 * &mdash; urutan relatif keduanya lalu ditentukan kestabilan algoritma pengurutan, bukan oleh
	 * data. Ini bukan kasus hipotetis: penyemai bawaan yang kini dikomentari di
	 * {@link #reloadDefault()} sendiri memberi nomor urut 204 kepada dua grup berbeda. Selain itu
	 * {@code compareTo} di sini <b>tidak konsisten dengan {@code equals}</b>, jadi jangan
	 * memakai entity ini sebagai kunci {@code TreeSet}/{@code TreeMap}.</p>
	 *
	 * <p><b>Penanganan error.</b> Seluruh badan dibungkus {@code try/catch} yang menelan exception
	 * (dicatat ke {@code ErrorAuditUtil}) lalu mengembalikan {@code 0} &mdash; termasuk bila
	 * {@code arg0} bernilai {@code null}, yang tidak melempar {@code NullPointerException} keluar.</p>
	 *
	 * @param arg0 entity pembanding; boleh entity apa pun turunan {@link GeneralValueObject}
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada kunci
	 *         pembanding yang memenuhi syarat atau terjadi exception
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {

			if (getNomorUrut() != null && arg0.getNomorUrut() != null) {
				return getNomorUrut().compareTo(arg0.getNomorUrut());
			} else if (getNim() != null && arg0.getNim() != null) {
				return getNim().compareTo(arg0.getNim());
			} else if (getNama() != null && arg0.getNama() != null) {
				return getNama().compareTo(arg0.getNama());
			} else if (getKeterangan() != null && arg0.getKeterangan() != null) {
				return getKeterangan().compareTo(arg0.getKeterangan());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/MasterGrupLaporan.java:97");

		}

		return 0;
	}

	/**
	 * Penyemai data bawaan &mdash; <b>KINI TIDAK MELAKUKAN APA PUN</b>.
	 *
	 * <p><b>Status TERVERIFIKASI.</b> Seluruh badan method ini dikomentari; yang tersisa hanyalah
	 * method kosong. Meski begitu ia <b>masih dipanggil</b> oleh {@code ais.common.InitData} pada
	 * rangkaian {@code reloadDefaults}, lengkap dengan baris
	 * {@code System.out.println("reloadDefaults: MasterGrupLaporan ...")} yang mendahuluinya
	 * &mdash; sehingga log start-up tetap memberi kesan ada penyemaian yang berjalan padahal
	 * tidak. Jangan mengandalkan method ini untuk menyiapkan struktur laporan pada instalasi baru.</p>
	 *
	 * <p><b>Apa yang dulu dilakukannya</b> (berguna sebagai dokumentasi struktur bawaan yang
	 * diharapkan, dan sebagai contoh bila penyemaian hendak dihidupkan kembali): membuat enam
	 * pasang {@code MasterGrupLaporan} + {@code KelompokLaporan} untuk laporan Arus Kas &mdash;
	 * lima di bawah nama {@link #OPERASIONAL} ("Piutang Usaha" 200, "Kewajiban Penggajian" 201,
	 * "Pendapatan Operasional" 202, "Pendapatan Operasional Lainnya" 203, "Biaya Administrasi
	 * &amp; Umum" 204) dan satu di bawah {@link #INVESTASI} ("Biaya Non Operasional", juga 204).
	 * Setiap pencarian dilakukan dua bahasa (Indonesia dan Inggris) memakai {@code ilike}, dan
	 * setiap {@code KelompokLaporan} yang dibuat langsung ditautkan ke
	 * {@code JenisLaporan.ARUS_KAS}.</p>
	 *
	 * <p><b>Catatan bila hendak dihidupkan kembali.</b> Kode lama itu tidak dapat dikompilasi apa
	 * adanya: ia mengacu pada konstanta {@code JenisLaporan.ARUS_KAS} yang di
	 * {@code JenisLaporan.java} juga sudah dikomentari, memakai {@code Session}/{@code Restrictions}
	 * /{@code MatchMode}/{@code HibernateUtil} yang {@code import}-nya sudah dihapus dari berkas
	 * ini, dan &mdash; yang paling berbahaya &mdash; ia memanggil {@code HibernateUtil.closeSession()}
	 * di akhir, pola yang pada berkas lain repo ini terbukti menutup sesi ZK yang masih dipakai
	 * halaman aktif. Perhatikan pula dua nomor urut 204 yang kembar (lihat
	 * {@link #compareTo(GeneralValueObject)}).</p>
	 */
	public static void reloadDefault() {
//		Session session = HibernateUtil.currentNativeSession();
//		try {
//			MasterGrupLaporan grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//					.add(Restrictions.eq("nama", OPERASIONAL))
//					.add(Restrictions.or(Restrictions.ilike("keterangan", "Account Receivables", MatchMode.ANYWHERE),
//							Restrictions.ilike("keterangan", "Piutang Usaha", MatchMode.ANYWHERE)))
//					.setMaxResults(1).uniqueResult();
//			if (grupLaporan == null) {
//
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(OPERASIONAL);
//					grupLaporan.setKeterangan("Piutang Usaha");
//					grupLaporan.setNomorUrut(200);
//					grupLaporan.setTampilkanAkunRinci(true);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//
//				grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//						.add(Restrictions.eq("nama", OPERASIONAL))
//
//						.add(Restrictions.or(
//								Restrictions.ilike("keterangan", "Kewajiban Penggajian", MatchMode.ANYWHERE),
//								Restrictions.ilike("keterangan", "Payroll Liabilities", MatchMode.ANYWHERE)))
//
//						.setMaxResults(1).uniqueResult();
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(OPERASIONAL);
//					grupLaporan.setKeterangan("Kewajiban Penggajian");
//					grupLaporan.setTampilkanAkunRinci(true);
//					grupLaporan.setNomorUrut(201);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//
//				grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//						.add(Restrictions.eq("nama", OPERASIONAL))
//
//						.add(Restrictions.or(
//								Restrictions.ilike("keterangan", "Pendapatan Operasional", MatchMode.EXACT),
//								Restrictions.ilike("keterangan", "Operating Income", MatchMode.EXACT)))
//
//						.setMaxResults(1).uniqueResult();
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(OPERASIONAL);
//					grupLaporan.setKeterangan("Pendapatan Operasional");
//					grupLaporan.setTampilkanAkunRinci(true);
//					grupLaporan.setNomorUrut(202);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//
//				grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//						.add(Restrictions.eq("nama", OPERASIONAL))
//
//						.add(Restrictions.or(
//								Restrictions.ilike("keterangan", "Pendapatan Operasional Lainnya", MatchMode.ANYWHERE),
//								Restrictions.ilike("keterangan", "Other Operating Income", MatchMode.ANYWHERE)))
//
//						.setMaxResults(1).uniqueResult();
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(OPERASIONAL);
//					grupLaporan.setKeterangan("Pendapatan Operasional Lainnya");
//					grupLaporan.setTampilkanAkunRinci(true);
//					grupLaporan.setNomorUrut(203);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//
//				grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//						.add(Restrictions.eq("nama", OPERASIONAL))
//
//						.add(Restrictions.or(
//								Restrictions.ilike("keterangan", "Biaya Administrasi & Umum", MatchMode.ANYWHERE),
//								Restrictions.ilike("keterangan", "Administration & General Expense",
//										MatchMode.ANYWHERE)))
//
//						.setMaxResults(1).uniqueResult();
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(OPERASIONAL);
//					grupLaporan.setKeterangan("Biaya Administrasi & Umum");
//					grupLaporan.setTampilkanAkunRinci(true);
//					grupLaporan.setNomorUrut(204);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//
//				grupLaporan = (MasterGrupLaporan) session.createCriteria(MasterGrupLaporan.class)
//						.add(Restrictions.eq("nama", INVESTASI))
//
//						.add(Restrictions.or(
//								Restrictions.ilike("keterangan", "Biaya Non Operasional", MatchMode.ANYWHERE),
//								Restrictions.ilike("keterangan", "Non Operating Expense", MatchMode.ANYWHERE)))
//
//						.setMaxResults(1).uniqueResult();
//				if (grupLaporan == null) {
//					grupLaporan = new MasterGrupLaporan();
//					grupLaporan.setNama(INVESTASI);
//					grupLaporan.setKeterangan("Biaya Non Operasional");
//					grupLaporan.setTampilkanAkunRinci(true);
//					grupLaporan.setNomorUrut(204);
//					session.getTransaction().begin();
//					session.save(grupLaporan);
//					session.getTransaction().commit();
//
//					KelompokLaporan kelompokLaporan = new KelompokLaporan();
//					kelompokLaporan.setJenisLaporan(JenisLaporan.ARUS_KAS);
//					kelompokLaporan.setMasterGrupLaporan(grupLaporan);
//					kelompokLaporan.setTampilkanAkunRinci(true);
//					kelompokLaporan.setAktif(true);
//					kelompokLaporan.setUrut(grupLaporan.getNomorUrut().doubleValue());
//					session.getTransaction().begin();
//					session.save(kelompokLaporan);
//					session.getTransaction().commit();
//				}
//			}
//		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/akunting/MasterGrupLaporan.java:286");
//			e.printStackTrace();
//		}
//		HibernateUtil.closeSession();
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung {@code MasterGrupLaporanAction.onAdd(...)} untuk membuka formulir
	 * "Tambah Master Grup Laporan" dengan objek kosong. Seluruh properti bernilai {@code null}
	 * kecuali {@code tanggal_dirubah} yang sudah terisi waktu saat ini.</p>
	 */
	public MasterGrupLaporan() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Nilai ini yang dikirim keempat kelas {@code LaporanAkunting*} sebagai parameter Jasper
	 * <code>$P{grup}</code> (dengan {@code -1L} berarti "semua grup"), dan yang dipakai
	 * {@code LaporanKeuanganCoaHelper.susun()} sebagai kunci pengelompokan seksi. Kolom dipetakan
	 * {@code insertable = false} karena diisi basis data lewat strategi {@code IDENTITY}.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Umumnya hanya dipanggil Hibernate; kode aplikasi memakai
	 * {@code null} untuk menandai baris baru.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan judul seksi laporan, sudah di-<em>trim</em>.
	 *
	 * <p><b>Getter substitusi, bukan destruktif:</b> spasi di tepi dibuang pada nilai yang
	 * dikembalikan saja &mdash; field {@link #nama} <b>tidak ditulis ulang</b>, sehingga membaca
	 * baris ini tidak pernah mengubah isi basis data. (Bandingkan dengan getter penulis-balik yang
	 * ada di beberapa entity lain modul akunting.) Konsekuensinya, spasi tepi tetap tersimpan di
	 * kolom dan tetap ikut dalam pencocokan SQL mentah yang tidak mem-<em>trim</em>.</p>
	 *
	 * <p>Dipanggil dari renderer grid CRUD, dari pembangun combobox
	 * ({@code Projections.groupProperty("nama")} menghasilkan nilai mentah, bukan lewat getter
	 * ini), dari {@code LaporanKeuanganCoaHelper} sebagai judul seksi, dan dari
	 * {@code NewUiLaporanAkuntingSaldoBulanController.semuaNama()}.</p>
	 *
	 * @return judul seksi tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel judul seksi laporan. Tanpa validasi di level entity &mdash; keharusan tidak-kosong
	 * hanya ditegakkan {@code MasterGrupLaporanAction.onSave} dan oleh batasan
	 * {@code nullable = false} di basis data. Tidak ada pemeriksaan duplikat: dua baris dengan
	 * nama sama diperbolehkan (lihat catatan penggabungan seksi pada Javadoc kelas).
	 *
	 * @param nama judul seksi baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan anak judul seksi ("Sub Grup" di formulir).
	 *
	 * <p><b>Perhatikan perbedaan dengan induk:</b> {@link GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""}, sedangkan getter ini mengembalikan nilai apa
	 * adanya &mdash; termasuk {@code null}. Karena itu pemanggil di jalur laporan
	 * ({@code LaporanKeuanganCoaHelper}, {@code NewUiLaporanAkuntingSaldoBulanController}) selalu
	 * memeriksa {@code null} secara eksplisit, sementara renderer grid
	 * {@code MasterGrupLaporanAction} menyerahkan nilai ini langsung ke {@code new Label(...)}
	 * (ZK menampilkannya sebagai teks kosong).</p>
	 *
	 * @return anak judul seksi, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel anak judul seksi ("Sub Grup"). Tanpa validasi; nilai {@code null} diterima.
	 *
	 * @param keterangan anak judul baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi, dengan <b>substitusi {@code 0} bila belum diisi</b>.
	 *
	 * <p><b>Tujuan.</b> Menjamin pemanggil tidak perlu memeriksa {@code null}: renderer grid
	 * memanggil {@code getNomorUrut().toString()} tanpa penjaga, dan
	 * {@code MyIntbox(masterGrupLaporan.getNomorUrut())} pada formulir mengandalkan hal yang sama.</p>
	 *
	 * <p><b>Kasus tepi yang perlu diketahui.</b> Substitusi ini bersifat <em>tampilan saja</em>
	 * &mdash; field tetap {@code null} dan tidak ditulis balik. Namun ia membuat cabang kedua dan
	 * seterusnya {@link #compareTo(GeneralValueObject)} menjadi kode mati (lihat Javadoc method
	 * tersebut), dan menyebabkan seksi yang belum diberi nomor urut selalu tampil paling awal
	 * (nilai 0) alih-alih paling akhir. Perlu dicatat pula bahwa jalur cetak Jasper
	 * <b>tidak</b> melewati getter ini: ia membaca kolom <code>e.nomor_urut</code> langsung dari
	 * SQL, sehingga di sana {@code NULL} mengikuti aturan pengurutan basis data (pada PostgreSQL
	 * {@code NULLS LAST} untuk urutan menaik) &mdash; artinya urutan seksi bisa <b>berbeda</b>
	 * antara layar HTML dan laporan cetak untuk baris yang nomor urutnya kosong.</p>
	 *
	 * @return nomor urut tampil; {@code 0} bila kolom kosong (tidak pernah {@code null})
	 */
	@Column(name = "nomor_urut", nullable = true)
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil seksi. Tanpa validasi &mdash; nilai negatif, nol, maupun kembar
	 * dengan seksi lain semuanya diterima; lihat {@link #compareTo(GeneralValueObject)} untuk
	 * akibat nomor kembar.
	 *
	 * @param nomorUrut nomor urut baru; boleh {@code null}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan bendera "tampilkan akun secara rinci", dengan <b>substitusi {@code true} bila
	 * belum diisi</b> &mdash; jadi perilaku bawaan adalah menampilkan rincian akun.
	 *
	 * <p><b>Catatan pemetaan.</b> Ini satu-satunya properti persisten di kelas ini yang
	 * <b>tanpa anotasi {@code @Column}</b>; nama kolomnya karena itu mengikuti nama properti apa
	 * adanya, yaitu <code>tampilkanakunrinci</code> &mdash; terkonfirmasi dari SQL berkas Jasper
	 * yang menuliskannya sebagai <code>e.tampilkanakunrinci as tampil</code>. Jangan menambahkan
	 * {@code @Column(name = "tampilkan_akun_rinci")} tanpa migrasi basis data; itu akan memutus
	 * pemetaan.</p>
	 *
	 * <p><b>Jangkauan efek yang sebenarnya (verifikasi).</b> Bendera ini hanya dibaca tiga berkas
	 * Jasper (<code>laporan_keuangan_arus_kasi</code>, <code>laporan_keuangan_mutasi</code>,
	 * <code>laporan_keuangan_mutasitidak_rinci</code>) untuk menentukan
	 * {@code printWhenExpression} blok rincian. Perender HTML
	 * {@code LaporanKeuanganCoaHelper.susun()} <b>tidak pernah memanggilnya</b> &mdash; di sana yang
	 * dipakai adalah {@code KelompokLaporan.getTampilkanAkunRinci()}. Jadi mematikan pilihan ini
	 * di layar "Grup Laporan" tidak akan menyembunyikan rincian akun pada mayoritas laporan.</p>
	 *
	 * @return {@code true} bila akun ditampilkan rinci; {@code true} pula bila kolom kosong
	 *         (tidak pernah {@code null})
	 */
	public Boolean getTampilkanAkunRinci() {
		return tampilkanAkunRinci == null ? true : tampilkanAkunRinci;
	}

	/**
	 * Menyetel bendera "tampilkan akun secara rinci". Dipanggil dari checkbox formulir CRUD
	 * ({@code MasterGrupLaporanAction.onSave}) yang selalu mengirim {@code true}/{@code false},
	 * sehingga nilai {@code null} praktis hanya berasal dari baris lama sebelum kolom ini ada.
	 *
	 * @param tampilkanAkunRinci bendera baru; boleh {@code null} (dibaca sebagai {@code true})
	 */
	public void setTampilkanAkunRinci(Boolean tampilkanAkunRinci) {
		this.tampilkanAkunRinci = tampilkanAkunRinci;
	}
}
