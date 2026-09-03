package ais.database.model.sekolah;

import static javax.persistence.GenerationType.IDENTITY;

import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import org.json.JSONObject;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code sekolah.checklist_penilaian_guru}, merepresentasikan
 * satu butir/pertanyaan <b>master</b> pada checklist penilaian guru (modul jenjang sekolah) —
 * isi pertanyaan ({@link #getIsi()}), bobot nilainya ({@link #getBobot()}), dan opsi pilihan
 * jawaban dalam bentuk JSON ({@link #getPilihan()}, default {@code "{}"} bila kosong/tidak
 * valid). Setiap butir tergabung dalam satu {@link #getGrupChecklistPenilaianGuru()}
 * (pengelompokan butir checklist).
 * <p>
 * Id butir pada tabel ini adalah kunci {@code idButir} yang dirujuk oleh format teks
 * terpadatkan pada {@link ChecklistBaruPenilaianGuruOlehSiswa#getKeterangan()} (jawaban siswa
 * atas guru untuk butir-butir ini disimpan di sana, bukan pada tabel relasi terpisah).
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 *
 * <h2>Posisi dalam rantai angket guru (4 lapis)</h2>
 * <p>Angket guru jenjang sekolah disusun berlapis, dan entity ini berada di lapis <b>ketiga</b>
 * (paling dalam dari sisi master; lapis keempat sudah berupa transaksi/jawaban):</p>
 * <ol>
 *   <li>{@link AngketPenilaianGuru} — header/periode angket. Di sinilah cakupan berlakunya
 *       ditentukan: yayasan, sekolah, program, angkatan, penanda {@code untukSiswa}, jumlah
 *       opsi skala ({@code jumlahPilihan}, default 5), dan apakah kotak keterangan per butir
 *       ditampilkan ({@code tampilKeterangan}). <b>Entity ini sendiri tidak menyimpan kolom
 *       sekolah/yayasan apa pun</b> — seluruh penyaringan tenant untuk sebuah butir dilakukan
 *       lewat dua kali join ke atas: butir → grup → angket.</li>
 *   <li>{@link GrupChecklistPenilaianGuru} — kelompok/aspek penilaian (mis. "Penguasaan
 *       Materi", "Kedisiplinan"), lengkap dengan flag {@code aktif} sendiri.</li>
 *   <li><b>{@code ChecklistPenilaianGuru} (kelas ini)</b> — butir pertanyaan yang benar-benar
 *       dibaca siswa dan dijawab dengan satu pilihan skala radio 1..N.</li>
 *   <li>{@link ChecklistBaruPenilaianGuruOlehSiswa} — baris transaksi berisi seluruh jawaban
 *       satu siswa atas satu guru pada satu jadwal pelajaran.</li>
 * </ol>
 * <p>Padanan jenjang perguruan tinggi dari kelas ini adalah
 * {@link ais.database.model.ChecklistPenilaianDosen} (butir angket dosen oleh mahasiswa).
 * Struktur field, anotasi, bahkan nilai {@link #serialVersionUID} disalin kata-per-kata dari
 * sana — kemiripan itu warisan salin-tempel generator, bukan hubungan semantik.</p>
 *
 * <h2>Status pemakaian: HIDUP, dan justru menjadi definisi butir untuk skema BARU</h2>
 * <p>Perlu ditegaskan karena mudah tertukar: entity <b>transaksi</b> versi lama,
 * {@link ChecklistPenilaianGuruOlehSiswa}, sudah <b>yatim</b> (tidak ada satu pun kelas Java
 * lain yang merujuknya). Entity master ini <b>tidak</b> ikut mati bersamanya. Penelusuran
 * seluruh pohon sumber menemukan sekitar dua puluh lima berkas Java yang merujuk tipe ini,
 * dan yang terpenting: <b>jalur angket yang aktif hari ini pun tetap memakai kelas ini sebagai
 * definisi butirnya</b>. Konsumen utamanya:</p>
 * <ul>
 *   <li>{@code ais.action.master.helper.generic.AngketGuruWindow} — layar pengisian angket oleh
 *       siswa; membangun radio group per butir dan menuliskan jawabannya ke
 *       {@link ChecklistBaruPenilaianGuruOlehSiswa} (skema BARU).</li>
 *   <li>{@code ais.common.ChecklistPenilaianGuruHelper#checkStatusChecklistGuru} — gerbang
 *       "angket wajib diisi" yang menghitung butir yang seharusnya sudah dijawab; dipanggil
 *       dari {@code ais.common.Common} dan dari REST {@code ais.action.servlet.api.AngketKewajibanApi}.</li>
 *   <li>{@code ais.action.servlet.api.AngketUtilApi} — endpoint mobile untuk mengambil daftar
 *       butir dan menyimpan jawaban siswa ({@code check_id} pada payload adalah id baris tabel
 *       ini).</li>
 *   <li>{@code ais.action.master.sekolah.ChecklistPenilaianGuruAction} — layar master CRUD
 *       butir (plus impor/ekspor Excel).</li>
 *   <li>{@code ais.action.report.format1.akademik.LaporanAngketGuruDashboardWindow} — dasbor
 *       rekap hasil angket; memetakan id butir hasil pembacaan blob jawaban ke teks
 *       pertanyaannya.</li>
 *   <li>{@code ais.common.InitData} — kelas ini termasuk yang dipra-muat ke cache data awal,
 *       dan {@code ais.common.DataUtil} memasukkannya ke daftar {@code CLASS_JANGAN_DIBERSIHKAN}
 *       (dikecualikan dari utilitas pembersihan data). Keduanya adalah penanda kuat bahwa tabel
 *       ini diperlakukan sebagai <b>data master permanen</b>, bukan sisa skema lama.</li>
 * </ul>
 *
 * <h2>Kolom yang ada di tabel tapi tidak pernah ditulis aplikasi</h2>
 * <p>Dua kolom pada entity ini praktis mati tulis:</p>
 * <ul>
 *   <li><b>{@code pilihan}</b> — lihat {@link #getPilihan()}. Tidak ada satu pun pemanggil
 *       {@link #setPilihan(String)} di seluruh pohon sumber (padanan PT/umumnya punya editor
 *       JSON sendiri, versi guru tidak). Akibatnya {@link #getPilihan()} secara praktis
 *       <b>selalu</b> mengembalikan {@code "{}"}, dan label opsi radio pada layar pengisian
 *       selalu jatuh ke angka telanjang "1".."N", tidak pernah berupa label bermakna seperti
 *       "Sangat Baik".</li>
 *   <li><b>{@code aktif}</b> — tidak ada isian {@code aktif} pada dialog tambah/ubah butir;
 *       kolom ini hanya bisa diubah lewat kotak centang pada baris grid dan lewat impor Excel.
 *       Baris hasil impor/insert lain akan bernilai {@code null}, yang oleh {@link #getAktif()}
 *       diperlakukan sebagai <b>aktif</b> (lihat catatan fail-open di method tersebut).</li>
 * </ul>
 *
 * <h2>{@code bobot} tampil di layar tetapi tidak pernah menghitung apa pun</h2>
 * <p>{@link #getBobot()} punya isian sendiri di formulir master dan kolom sendiri di grid,
 * sehingga admin wajar mengira mengisi bobot 3 akan membuat butir itu tiga kali lebih berpengaruh
 * pada skor guru. Kenyataannya <b>tidak ada satu pun jalur perhitungan jenjang sekolah yang
 * membaca bobot ini</b>: dasbor angket guru menjumlahkan nilai mentah dan membaginya rata per
 * butir/kelompok. Bandingkan dengan padanan PT-nya yang benar-benar memakai bobot pada
 * perhitungan tertimbang ({@code RekapAngketUntukDosen}, {@code LaporanAngketDosenDashboardWindow},
 * {@code LaporanRekapAngketDosenPerJurusanWindow}). Jadi bobot di sini murni dekoratif —
 * fitur yang hilang saat modul dosen disalin menjadi modul guru.</p>
 *
 * <h2>Kuirk dan jebakan bagi pembaca kode</h2>
 * <ul>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 *       bug.</b> {@link GeneralValueObject} bukan {@code @Entity} maupun
 *       {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan
 *       properti induknya. Setiap entity turunan <b>harus</b> mendeklarasikan ulang keempat
 *       properti itu lengkap dengan anotasinya agar terpetakan.</li>
 *   <li><b>{@link #ambilkey()} adalah kode mati.</b> Method kembarannya di
 *       {@link ais.database.model.ChecklistPenilaianDosen} dipakai tiga kali oleh
 *       {@code LaporanRekapAngketDosen}; versi guru ini tidak dipanggil dari mana pun (termasuk
 *       dari JSP/ZUL). Konstanta {@link #NF} pun ikut mati karena hanya dipakai method ini.</li>
 *   <li><b>{@link #getGrupChecklistPenilaianGuru()} adalah getter dengan efek samping</b>
 *       (menulis balik ke field-nya sendiri) — pola berulang di seluruh model AIS, dijelaskan
 *       rinci pada method tersebut.</li>
 *   <li><b>{@link #getKeterangan()} di sini TIDAK membalik kontrak.</b> Kolom ini benar-benar
 *       catatan bebas admin tentang butir soal, diisi lewat kotak "Keterangan" pada formulir
 *       master dan ditampilkan apa adanya di grid. Yang membalik kontrak adalah
 *       {@link ChecklistBaruPenilaianGuruOlehSiswa#getKeterangan()} — di sana "keterangan"
 *       sebenarnya adalah blob seluruh jawaban terpadatkan, bukan catatan.</li>
 *   <li><b>Renderer grid menambal butir yatim dengan grup sembarang.</b> Saat menggambar baris,
 *       {@code ChecklistPenilaianGuruAction.ChecklistPenilaianGuruRenderer} memeriksa apakah
 *       butir punya grup; bila tidak, ia menyetel grup dengan hasil query
 *       {@code createCriteria(GrupChecklistPenilaianGuru.class).setMaxResults(1)} — yakni grup
 *       <b>pertama yang kebetulan dikembalikan basis data, tanpa order dan tanpa filter tenant
 *       apa pun</b>. Karena object yang dirender berasal dari session Hibernate yang sama
 *       (terkelola), perubahan itu berpeluang ikut ter-flush: sekadar <i>membuka halaman daftar</i>
 *       dapat memindahkan butir yatim ke grup milik sekolah lain, dan perpindahannya tercatat
 *       permanen oleh Envers.</li>
 * </ul>
 *
 * <h2>Catatan cakupan tenant (perhatian keamanan)</h2>
 * <p>Tiga hal yang perlu diketahui pembaca sebelum menyentuh entity ini:</p>
 * <ul>
 *   <li><b>Layar master fail-open.</b> {@code ChecklistPenilaianGuruAction.initCriteria()}
 *       memasang {@code Restrictions.sqlRestriction("1=1")} untuk setiap filter yang belum
 *       dipilih pengguna, dan combo yayasan/sekolah diinisialisasi tanpa pemilihan otomatis
 *       berdasarkan tenant pengguna. Nilai bawaan layar karenanya adalah "semua" — pengguna
 *       ber-hak UPDATE di satu sekolah melihat, mengubah, menonaktifkan, dan menghapus butir
 *       angket milik seluruh sekolah/yayasan lain dalam instalasi yang sama.</li>
 *   <li><b>Impor Excel menimpa berdasarkan id.</b> Jalur unggah pada layar yang sama membaca
 *       kolom pertama sebagai id lalu langsung {@code saveOrUpdate} tanpa memeriksa tenant
 *       pemilik baris tersebut. Berkas dengan id milik sekolah lain akan menimpa isi, bobot,
 *       keterangan, dan status aktif butir tersebut secara massal.</li>
 *   <li><b>Gerbang kewajiban angket lebih longgar daripada formulirnya.</b>
 *       {@code ChecklistPenilaianGuruHelper.checkStatusChecklistGuru} menghitung butir wajib
 *       <b>tanpa</b> filter sekolah/yayasan/angkatan, sedangkan
 *       {@code AngketGuruWindow.buildChecklistCriteria} — yang menentukan butir mana yang
 *       benar-benar tampil untuk diisi — memfilter ketiganya. Pada instalasi multi-sekolah
 *       akibatnya nyata: butir milik sekolah lain masuk hitungan "wajib" tetapi tidak pernah
 *       muncul di layar siswa, sehingga status "angket belum lengkap" tidak akan pernah bisa
 *       dituntaskan siswa mana pun.</li>
 * </ul>
 *
 * @see GrupChecklistPenilaianGuru
 * @see AngketPenilaianGuru
 * @see ChecklistBaruPenilaianGuruOlehSiswa
 * @see ChecklistPenilaianGuruOlehSiswa
 * @see ais.database.model.ChecklistPenilaianDosen
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sekolah", name = "checklist_penilaian_guru")
public class ChecklistPenilaianGuru extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya <b>identik</b> dengan {@link GrupChecklistPenilaianGuru},
	 * {@link ChecklistBaruPenilaianGuruOlehSiswa}, {@link ais.database.model.ChecklistPenilaianDosen},
	 * dan sejumlah entity keluarga angket lain — jejak salin-tempel generator, bukan penanda
	 * kompatibilitas biner antar kelas. Jangan diubah tanpa alasan kuat: entity ini ikut
	 * diserialisasi ke sesi ZK dan cache tingkat aplikasi.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Pemformat angka 5 digit berpadding nol ({@code "00000"}) untuk menyusun kunci pada
	 * {@link #ambilkey()}. Dibungkus {@link ThreadLocal} karena {@link DecimalFormat} tidak
	 * aman dipakai bersama antar-thread, sementara instance statis dipakai bersama seluruh
	 * request server.
	 * <p><b>Praktisnya konstanta ini mati</b>: satu-satunya pemakainya adalah
	 * {@link #ambilkey()}, dan method itu sendiri tidak dipanggil dari mana pun.
	 */
	private static final ThreadLocal<NumberFormat> NF = new ThreadLocal<NumberFormat>() {
		@Override
		protected NumberFormat initialValue() {
			return new DecimalFormat("00000");
		}
	};
	/**
	 * Nilai balikan pengganti {@link #getPilihan()} bila kolom {@code pilihan} kosong atau
	 * bukan JSON yang sah: object JSON kosong {@code "{}"}. Dipilih agar pemanggil selalu bisa
	 * melakukan {@code new JSONObject(getPilihan())} tanpa membungkusnya dengan try/catch.
	 */
	private static final String PILIHAN_DEFAULT = "{}";

	/** Kunci primer baris, dipetakan ke kolom {@code id}. Lihat {@link #getId()}. */
	private Long id;
	/**
	 * Nama/keterangan pengguna terakhir yang mengubah baris ini (kolom {@code oleh}).
	 * Dideklarasikan ulang di sini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass} sehingga propertinya tidak diwarisi oleh pemetaan Hibernate.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini (kolom {@code oleh_id}). Sama seperti
	 * {@link #oleh}, wajib dideklarasikan ulang agar terpetakan.
	 */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}). Diinisialisasi ke waktu
	 * server saat object dibuat lewat {@link ais.ui.util.WaktuUtil#getDate()}, lalu diperbarui
	 * otomatis pada setiap update oleh {@link #onUpdate()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Teks pertanyaan yang dibaca siswa (kolom {@code isi}, wajib diisi). */
	private String isi;
	/**
	 * Catatan bebas admin tentang butir ini (kolom {@code keterangan}, boleh kosong). Tidak
	 * ditampilkan ke siswa; hanya muncul di grid dan formulir layar master.
	 */
	private String keterangan;
	/**
	 * Kelompok/aspek penilaian tempat butir ini bernaung (FK {@code grup_checklist_penilaian_guru}).
	 * Lewat grup inilah butir terhubung ke {@link AngketPenilaianGuru} dan karenanya ke
	 * yayasan/sekolah/program/angkatan yang berhak memakainya.
	 */
	private GrupChecklistPenilaianGuru grupChecklistPenilaianGuru;
	/**
	 * String JSON berisi label kustom untuk tiap angka skala, mis.
	 * {@code {"1":"Sangat Baik","2":"Baik"}} (kolom {@code pilihan}). Dalam praktik selalu
	 * {@code null} karena tidak ada layar yang menulisnya — lihat {@link #getPilihan()}.
	 */
	private String pilihan;
	/**
	 * Bobot butir untuk perhitungan skor tertimbang (kolom {@code bobot}). Ditampilkan dan bisa
	 * disunting, tetapi tidak dipakai perhitungan apa pun di jenjang sekolah — lihat
	 * {@link #getBobot()}.
	 */
	private Double bobot;
	/**
	 * Penanda butir masih dipakai (kolom {@code aktif}). {@code null} diperlakukan sebagai
	 * aktif oleh {@link #getAktif()} dan oleh seluruh query pemakainya.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JavaBeans. Dipakai juga oleh layar master
	 * saat pengguna menekan "Tambah" dan oleh jalur impor Excel untuk baris tanpa id.
	 */
	public ChecklistPenilaianGuru() {
	}

	/**
	 * Membuat instance ringan yang hanya membawa kunci primer — berguna sebagai referensi/stub
	 * pada kriteria Hibernate atau saat menyusun relasi tanpa perlu memuat seluruh baris.
	 *
	 * @param id kunci primer baris; boleh {@code null} (menghasilkan object baru yang belum tersimpan)
	 */
	public ChecklistPenilaianGuru(Long id) {
		this.id = id;
	}

	/**
	 * Kunci primer baris. Dibangkitkan basis data ({@link javax.persistence.GenerationType#IDENTITY},
	 * karenanya {@code insertable = false}), berurutan, dan dipakai luas sebagai
	 * <b>{@code idButir}</b>: nilai inilah yang tertanam pada blob jawaban
	 * {@link ChecklistBaruPenilaianGuruOlehSiswa#getKeterangan()} dan yang dikirim klien mobile
	 * sebagai {@code check_id}.
	 *
	 * @return kunci primer, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel kunci primer. Umumnya hanya dipanggil Hibernate; kode aplikasi memakai
	 * konstruktor {@link #ChecklistPenilaianGuru(Long)} bila butuh stub.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Id pengguna terakhir yang mengubah baris ini (kolom {@code oleh_id}), diisi otomatis oleh
	 * lapis penyimpanan bersama.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Menolak nilai kosong secara diam-diam</b>: bila argumen
	 * {@code null} atau hanya berisi spasi, method langsung {@code return} tanpa mengubah
	 * apa pun. Konsekuensinya, jejak audit yang sudah terisi tidak akan pernah bisa dikosongkan
	 * kembali lewat setter ini — sengaja, agar proses batch/impor yang tidak membawa identitas
	 * pengguna tidak menghapus jejak pengubah sebelumnya.
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Nama/keterangan pengguna terakhir yang mengubah baris ini (kolom {@code oleh}).
	 *
	 * @return nama pengguna pengubah, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah, dengan perilaku "tolak kosong" yang sama persis seperti
	 * {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@link javax.persistence.PreUpdate}: dipanggil Hibernate tepat sebelum
	 * perintah {@code UPDATE} baris ini dikirim ke basis data, lalu meneruskan object ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} yang menyetel
	 * {@link #tanggal_dirubah} (dan jejak pengguna bila tersedia) ke nilai terkini.
	 * <p>Tidak dipanggil pada {@code INSERT} — untuk baris baru stempel waktu sudah terisi dari
	 * inisialisasi field. Tidak boleh dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP} (tanggal
	 * beserta jam). Dipakai antara lain untuk pengurutan riwayat dan pelacakan revisi Envers.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir. Biasanya tidak dipanggil kode aplikasi karena
	 * sudah diurus {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Teks pertanyaan butir ini — inilah kalimat yang dibaca siswa di layar pengisian angket,
	 * yang muncul sebagai judul kolom pada rekap, dan yang dipakai
	 * {@code LaporanAngketGuruDashboardWindow} untuk melabeli baris detail hasil angket.
	 * Dipetakan ke kolom {@code isi} bertipe {@code text} yang <b>wajib diisi</b>
	 * ({@code nullable = false}), dan
	 * layar master memvalidasinya lebih dulu ("Nama harus diisi").
	 * <p>Perhatikan bahwa daftar butir pada layar pengisian diurutkan menaik berdasarkan isi
	 * ({@code Order.asc("isi")}), bukan berdasarkan nomor urut tersendiri — tidak ada kolom
	 * urutan pada entity ini. Urutan tampil pertanyaan karenanya mengikuti abjad teksnya.
	 *
	 * @return teks pertanyaan, atau {@code null} pada object yang belum diisi
	 */
	@Column(name = "isi", nullable = false, columnDefinition = "text")
	public String getIsi() {
		return isi;
	}

	/**
	 * Menyetel teks pertanyaan butir. Tanpa validasi maupun pemangkasan spasi — validasi "tidak
	 * boleh kosong" ada di layar pemanggil, bukan di sini.
	 *
	 * @param isi teks pertanyaan baru
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Catatan bebas admin mengenai butir ini (kolom {@code keterangan}, boleh kosong): misalnya
	 * penjelasan maksud pertanyaan atau alasan butir dinonaktifkan. Diisi lewat kotak
	 * "Keterangan" pada dialog tambah/ubah dan ditampilkan pada satu kolom grid layar master.
	 * <b>Tidak</b> ditampilkan kepada siswa saat mengisi angket.
	 * <p><b>Jangan tertukar</b> dengan {@link ChecklistBaruPenilaianGuruOlehSiswa#getKeterangan()}:
	 * di entity transaksi itu, properti bernama sama justru menampung <i>seluruh jawaban siswa
	 * yang dipadatkan</i> — kontraknya terbalik dari kesan namanya. Di kelas ini, "keterangan"
	 * benar-benar sekadar keterangan.
	 *
	 * @return catatan admin, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan admin untuk butir ini. Tanpa validasi.
	 *
	 * @param keterangan catatan baru; boleh {@code null} atau kosong
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kelompok/aspek penilaian tempat butir ini bernaung, dimuat malas
	 * ({@link FetchType#LAZY}) lewat FK {@code grup_checklist_penilaian_guru}. Relasi ini
	 * penting bukan sekadar untuk pengelompokan tampilan: <b>seluruh penyaringan tenant sebuah
	 * butir mengalir lewat sini</b> (butir → grup → {@link AngketPenilaianGuru} → yayasan,
	 * sekolah, program, angkatan, {@code untukSiswa}), karena entity ini sendiri tidak
	 * menyimpan kolom kepemilikan apa pun.
	 *
	 * <p><b>Getter dengan efek samping (pola berulang di seluruh model AIS).</b> Sebelum
	 * mengembalikan nilai, method menugaskan ulang hasil
	 * {@link GeneralValueObject#check(Object)} ke field-nya sendiri
	 * ({@code grupChecklistPenilaianGuru = check(grupChecklistPenilaianGuru)}). {@code check()}
	 * berusaha meresolusi proxy lazy yang mungkin sudah terlepas dari session (cache in-memory
	 * → inisialisasi lewat session berjalan → pembacaan ulang lewat session baru), dan bila
	 * keempat jalurnya gagal ia mengembalikan argumen apa adanya. Penugasan balik itu disengaja:
	 * ia menyimpan hasil resolusi supaya pemanggilan berikutnya murah. Konsekuensi yang wajib
	 * disadari pemanggil:
	 * <ul>
	 *   <li>membaca getter ini <b>mengubah keadaan object</b> — object yang tampak "hanya
	 *       dibaca" bisa berubah isi field-nya, dan pada object terkelola perubahan itu ikut
	 *       terlihat oleh pengecekan kotor Hibernate;</li>
	 *   <li>getter ini <b>bisa menyentuh basis data</b> (bahkan membuka session baru) sehingga
	 *       tidak gratis di dalam perulangan besar — untuk daftar panjang, muat relasinya lewat
	 *       satu query/join, jangan lewat getter per baris;</li>
	 *   <li>hasilnya tetap bisa {@code null} bila FK memang kosong.</li>
	 * </ul>
	 *
	 * @return kelompok checklist pemilik butir ini, atau {@code null} bila butir yatim (FK kosong)
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "grup_checklist_penilaian_guru")
	public GrupChecklistPenilaianGuru getGrupChecklistPenilaianGuru() {
		grupChecklistPenilaianGuru = check(grupChecklistPenilaianGuru);
		return grupChecklistPenilaianGuru;
	}

	/**
	 * Menyetel kelompok pemilik butir ini. Karena relasinya memakai
	 * {@link CascadeType#PERSIST} dan {@link CascadeType#MERGE}, menyimpan butir juga akan
	 * menyimpan/menggabungkan grup yang belum tersimpan — jangan menyodorkan object grup hasil
	 * rakitan manual bila tidak bermaksud membuat grup baru.
	 * <p>Dipanggil dari formulir master (pilihan combo "Grup Angket Guru", wajib dipilih) dan
	 * — perlu diwaspadai — dari renderer grid yang menambal butir yatim dengan grup pertama
	 * yang ditemukan basis data tanpa filter tenant apa pun; lihat pembahasan pada Javadoc
	 * kelas.
	 *
	 * @param grupChecklistPenilaianGuru kelompok baru; boleh {@code null} (menjadikan butir yatim)
	 */
	public void setGrupChecklistPenilaianGuru(GrupChecklistPenilaianGuru grupChecklistPenilaianGuru) {
		this.grupChecklistPenilaianGuru = grupChecklistPenilaianGuru;
	}

	/**
	 * Penanda butir masih dipakai. <b>{@code null} diperlakukan sebagai aktif</b> —
	 * "fail-open by default": butir yang belum pernah dicentang siapa pun tetap ikut tampil di
	 * angket siswa. Seluruh query pemakainya menganut konvensi yang sama
	 * ({@code Restrictions.or(eq("aktif", true), isNull("aktif"))}), sehingga perilakunya
	 * konsisten antara entity, layar pengisian, gerbang kewajiban, REST API, dan dasbor.
	 * <p>Perlu diperhatikan: nilai {@code null} inilah kondisi normal, karena dialog tambah/ubah
	 * butir <b>tidak</b> punya isian aktif. Kolom baru terisi eksplisit bila admin menekan kotak
	 * centang "Aktif" di baris grid (yang langsung menyimpan), atau lewat impor Excel.
	 *
	 * @return {@link Boolean#TRUE} bila kolom {@code aktif} bernilai {@code null} atau benar;
	 *         {@link Boolean#FALSE} hanya bila memang pernah dinonaktifkan secara eksplisit.
	 *         Tidak pernah mengembalikan {@code null}
	 */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Menyetel status aktif butir. Dipanggil dari kotak centang pada baris grid layar master
	 * (yang langsung menyusulinya dengan penyimpanan), dan dari jalur impor Excel.
	 * <p>Perhatikan asimetri dengan {@link #getAktif()}: menyetel {@code null} di sini akan
	 * terbaca kembali sebagai {@code TRUE}, jadi {@code null} bukan cara menonaktifkan butir —
	 * gunakan {@link Boolean#FALSE}.
	 *
	 * @param aktif status baru; {@code null} berarti "kembali ke perilaku bawaan (aktif)"
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Bobot butir untuk perhitungan skor tertimbang, dengan <b>bawaan 1,0</b> bila kolom kosong
	 * sehingga pemanggil tidak perlu menangani {@code null} sendiri.
	 *
	 * <p><b>Peringatan penting bagi pembaca kode dan bagi admin:</b> pada jenjang sekolah nilai
	 * ini <b>tidak pernah dipakai menghitung apa pun</b>. Dua satu-satunya pemakainya adalah
	 * layar master itu sendiri — label kolom "Bobot" pada grid dan kotak isian pada dialog
	 * ubah. Dasbor rekap angket guru menjumlahkan nilai mentah lalu membaginya rata per butir
	 * dan per kelompok, tanpa menyentuh bobot. Bandingkan dengan padanan perguruan tingginya,
	 * {@link ais.database.model.ChecklistPenilaianDosen#getBobot()}, yang benar-benar dipakai
	 * pada perhitungan tertimbang rekap angket dosen. Mengubah bobot di sini karenanya tidak
	 * akan menggeser skor guru sedikit pun.
	 *
	 * @return bobot butir; {@code 1.0} bila kolom {@code bobot} kosong. Tidak pernah {@code null}
	 */
	@Column(name = "bobot")
	public Double getBobot() {
		return bobot == null ? Double.valueOf(1.0) : bobot;
	}

	/**
	 * Menyetel bobot butir. Tanpa validasi rentang — nilai negatif atau nol pun diterima
	 * (tidak berdampak apa pun, lihat {@link #getBobot()}).
	 *
	 * @param bobot bobot baru; {@code null} berarti kembali ke bawaan {@code 1.0} saat dibaca
	 */
	public void setBobot(Double bobot) {
		this.bobot = bobot;
	}

	/**
	 * Label kustom untuk tiap angka pada skala jawaban, berupa string JSON yang memetakan nomor
	 * pilihan ke teksnya, mis. {@code {"1":"Sangat Baik","2":"Baik","3":"Cukup"}}. Layar
	 * pengisian angket membangun radio 1..N (N dari {@code AngketPenilaianGuru.jumlahPilihan},
	 * bawaan 5) dan untuk setiap angka memakai label dari JSON ini bila ada, atau angkanya
	 * sendiri bila tidak.
	 *
	 * <p><b>Getter defensif, bukan pengembali nilai mentah.</b> Method mengembalikan
	 * {@link #PILIHAN_DEFAULT} ({@code "{}"}) dalam dua keadaan: kolom kosong/hanya spasi, atau
	 * isinya gagal diurai menjadi {@link JSONObject}. Pengurai dipanggil hanya untuk
	 * memvalidasi — hasilnya dibuang dan yang dikembalikan tetap string aslinya. Kontrak ini
	 * membuat pemanggil aman menulis {@code new JSONObject(c.getPilihan())} tanpa try/catch,
	 * dengan ongkos: satu kali parsing JSON <b>setiap kali getter dipanggil</b>, termasuk di
	 * dalam perulangan render per butir.
	 *
	 * <p><b>Dalam praktik nilainya selalu {@code "{}"}.</b> Tidak ada satu pun pemanggil
	 * {@link #setPilihan(String)} di seluruh pohon sumber, dan formulir master jenjang sekolah
	 * tidak punya editor pilihan (padanan PT {@code ChecklistPenilaianDosenAction} dan
	 * {@code ChecklistPenilaianUmumAction} punya). Akibatnya opsi radio pada angket guru selalu
	 * berlabel angka telanjang "1".."N" — fitur label bermakna hilang saat modul dosen disalin
	 * menjadi modul guru.
	 *
	 * @return string JSON pilihan yang dijamin sah, atau {@code "{}"} bila kolom kosong maupun
	 *         berisi JSON rusak. Tidak pernah {@code null}
	 */
	@Column(name = "pilihan", columnDefinition = "text")
	public String getPilihan() {
		if (pilihan == null || pilihan.trim().isEmpty()) {
			return PILIHAN_DEFAULT;
		}
		try {
			new JSONObject(pilihan);
			return pilihan;
		} catch (Exception e) {
			return PILIHAN_DEFAULT;
		}
	}

	/**
	 * Menyetel string JSON label pilihan. <b>Tanpa validasi</b> — JSON rusak akan tersimpan apa
	 * adanya ke basis data dan baru "disembunyikan" saat dibaca kembali lewat
	 * {@link #getPilihan()}, yang diam-diam menggantinya dengan {@code "{}"}.
	 * <p>Saat ini tidak dipanggil dari mana pun (lihat {@link #getPilihan()}); dipertahankan
	 * karena dibutuhkan Hibernate sebagai setter properti terpetakan dan oleh mekanisme
	 * pemetaan berbasis refleksi (mis. impor Excel lewat {@code Common.setObjectValues}).
	 *
	 * @param pilihan string JSON baru; boleh {@code null} atau kosong
	 */
	public void setPilihan(String pilihan) {
		this.pilihan = pilihan;
	}

	/**
	 * Menghasilkan kunci pengurutan/pengelompokan tampilan gabungan grup, 5 huruf awal isi butir,
	 * dan id butir (masing-masing id diformat 5 digit lewat {@link #NF}).
	 *
	 * <p>Bentuk hasilnya {@code "<grupId 5 digit>_<5 huruf pertama isi>_<idButir 5 digit>"},
	 * mis. {@code "00003_Penge_00017"}. Padding nol dipakai supaya pengurutan leksikografis
	 * (mis. di dalam {@code TreeMap}) menghasilkan urutan yang sama dengan pengurutan numerik
	 * per grup. Seluruh komponennya tahan {@code null}: isi kosong menjadi string kosong, id
	 * dan grup yang kosong menjadi {@code 0}.
	 *
	 * <p><b>Kode mati.</b> Method ini tidak dipanggil dari mana pun di dalam pohon sumber —
	 * tidak dari Java, tidak dari JSP/ZUL. Yang hidup adalah method kembarannya di
	 * {@link ais.database.model.ChecklistPenilaianDosen#ambilkey()}, yang dipakai tiga kali
	 * oleh {@code LaporanRekapAngketDosen} untuk mencocokkan kolom rekap per butir. Versi guru
	 * ini ikut tersalin ketika modul dosen digandakan menjadi modul sekolah, tetapi laporan
	 * rekap padanannya tidak pernah dibuat. Perhatikan bahwa memanggilnya <b>tidak gratis</b>:
	 * ia melewati {@link #getGrupChecklistPenilaianGuru()} sehingga bisa memicu resolusi proxy
	 * dan query basis data.
	 *
	 * @return kunci gabungan grup/isi/id yang siap diurutkan secara leksikografis; tidak pernah
	 *         {@code null}
	 */
	public String ambilkey() {
		String nama = getIsi() == null ? "" : getIsi().trim();
		String key = nama.length() > 5 ? nama.substring(0, 5) : nama;
		Long myId = getId() == null ? Long.valueOf(0L) : getId();
		GrupChecklistPenilaianGuru grup = getGrupChecklistPenilaianGuru();
		Long grupId = grup == null || grup.getId() == null ? Long.valueOf(0L) : grup.getId();
		return NF.get().format(grupId) + "_" + key + "_" + NF.get().format(myId);
	}

	/**
	 * Representasi teks butir untuk tampilan: teks pertanyaannya sendiri, atau string kosong
	 * bila belum diisi (tidak pernah {@code null}, dan tidak pernah berupa
	 * {@code ClassName@hash} bawaan {@link Object}). Dipakai ZK saat butir dijadikan nilai item
	 * combo/daftar dan pada label sederhana.
	 * <p>Catatan implementasi: method membaca <b>field</b> {@code isi} secara langsung, bukan
	 * lewat {@link #getIsi()}. Untuk properti dasar seperti ini perbedaannya tidak berarti,
	 * tetapi konsekuensinya nyata pada proxy Hibernate yang belum terinisialisasi — pemanggilan
	 * {@code toString()} atas proxy semacam itu bisa menghasilkan string kosong alih-alih teks
	 * pertanyaan, tanpa memicu pemuatan.
	 *
	 * @return teks pertanyaan, atau string kosong bila {@code isi} masih {@code null}
	 */
	public String toString() {
		return isi == null ? "" : isi;
	}
}
