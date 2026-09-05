package ais.database.model.surat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import javax.persistence.Transient;

import org.hibernate.Session;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONObject;

import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jabatan;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.VoKunci;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.sop.DisposisiSop;

/**
 * HEADER satu dokumen SURAT MASUK: surat yang diterima instansi, diregistrasi ke buku agenda,
 * lalu didisposisikan berjenjang kepada para pejabat untuk ditindaklanjuti.
 *
 * <h3>Kedudukan dalam silsilah kelas</h3>
 *
 * <p>Kelas ini turunan {@link ais.database.model.VoKunci}, yang turunan
 * {@link ais.database.model.sop.DataSop}, yang turunan
 * {@link ais.database.model.GeneralValueObject}. Dua kelas induk itu MEWAJIBKAN dua pasang
 * operasi: penguncian dokumen ({@link #getDikunci()}/{@link #setDikunci}) dan keterikatan pada
 * simpul disposisi SOP ({@link #getDisposisiSop()}/{@link #setDisposisiSop}). Jadi surat masuk
 * bukan sekadar baris arsip, melainkan dokumen berjalan yang bisa dibekukan dan yang statusnya
 * diturunkan dari alur SOP.</p>
 *
 * <h3>Dua nomor yang mudah tertukar</h3>
 *
 * <ul>
 *   <li>{@link #getKode()} adalah NOMOR AGENDA internal -- nomor registrasi yang diberikan
 *       instansi penerima. Nomor ini dibangkitkan {@code SuratMasukAction.generateCode(...)},
 *       yang menyerahkan formatnya kepada mesin {@link NomorSurat} bila klasifikasi surat
 *       memilikinya, atau merakit format cadangan {@code prefix/NNNN/BulanRomawi/Tahun/postfix}
 *       bila tidak. Jadi penomoran surat masuk memang TERHUBUNG ke {@link NomorSurat}, tetapi
 *       secara tidak langsung: lewat {@link KlasifikasiSuratMasuk#getNomorSurat()}, bukan lewat
 *       relasi dari kelas ini.</li>
 *   <li>{@link #getNoSurat()} adalah nomor surat MILIK PENGIRIM, yang tercetak pada kop surat
 *       yang diterima. Nilainya diketik operator -- kecuali bila surat ini terhubung ke sebuah
 *       {@link SuratKeluar}, yang membuat getter-nya menimpa nilai ketikan itu.</li>
 * </ul>
 *
 * <h3>Surat masuk yang lahir sendiri dari surat keluar</h3>
 *
 * <p>Selain diregistrasi manual, baris surat masuk dapat DIBUAT OTOMATIS oleh
 * {@code AlurPersetujuanSuratKeluarStatusAction}: ketika sebuah surat keluar disetujui dan
 * definisi alurnya menunjuk sebuah alur surat masuk, sistem membuat satu {@code SuratMasuk}
 * baru yang menyalin fakultas, jurusan, satuan kerja, sekolah, dan yayasan dari surat keluar,
 * menandai asalnya sebagai "Surat keluar dengan nomor ...", serta menautkan
 * {@link #getSuratKeluar()}. Ini jalur korespondensi antar unit di dalam satu instansi. Dua hal
 * yang perlu diketahui tentang jalur itu: nomor agendanya diambil langsung dari
 * {@code suratKeluar.getAgenda()} sehingga MELEWATI {@code generateCode(...)} dan mesin
 * {@link NomorSurat}; dan tanggalnya diisi dari {@code getWaktuDitolak()} -- stempel waktu
 * PENOLAKAN -- padahal kode itu berada di dalam cabang "disetujui", sehingga nilainya selalu
 * {@code null} dan {@link #getTanggal()} memulihkannya menjadi tanggal hari ini.</p>
 *
 * <h3>Atribut yang diturunkan dari klasifikasi</h3>
 *
 * <p>Delapan getter di kelas ini MENIMPA nilai miliknya sendiri dengan nilai
 * {@link KlasifikasiSuratMasuk} bila klasifikasi mengisinya: {@link #getJurusan()},
 * {@link #getFakultas()}, {@link #getSekolah()}, {@link #getYayasan()},
 * {@link #getSatuanKerja()}, {@link #getSifat()}, {@link #getSifatSurat()}, dan
 * {@link #getAlurPersetujuanSuratMasuk()}; {@link #getPerihal()} mengisi diri dari perihal
 * bawaan klasifikasi selama masih kosong. Karena kelas dipetakan {@code dynamicUpdate = true}
 * dengan akses properti lewat getter, penimpaan itu bukan sekadar tampilan -- ia ter-flush ke
 * basis data. Akibatnya cakupan unit organisasi sebuah surat masuk TIDAK DAPAT menyimpang dari
 * klasifikasinya, dan mengubah klasifikasi memindahkan seluruh surat lamanya secara surut.</p>
 *
 * <h3>Gerbang disposisi: siapa boleh mendisposisikan kepada siapa</h3>
 *
 * <p>Pemilihan tujuan disposisi dirakit {@code SuratMasukAction.initJenisJabatan(...)}. Yang
 * BENAR-BENAR menjadi gerbang di sana hanya satu lapis: daftar {@code JenisJabatan} disaring
 * dengan syarat "{@code usernamePengguna} kosong ATAU memuat token id pengguna saat ini" DAN
 * "{@code jenisPengguna} kosong ATAU memuat token role pengguna saat ini", ditambah syarat
 * {@code aktif} kosong atau benar. Di dalam setiap {@code JenisJabatan} yang lolos, SELURUH
 * {@code Pejabat} aktif ditawarkan tanpa penyaringan lebih lanjut. Mencentang sebuah kotak
 * mencatat id pejabat ke dalam JSON {@link #getJenisSurats()} milik surat.</p>
 *
 * <p>Yang TIDAK ADA, dan perlu diketahui sebelum mengandalkan mekanisme ini sebagai kendali:
 * tidak ada pemeriksaan hierarki jabatan (pengguna tidak perlu berada di atas pejabat tujuan),
 * tidak ada pemeriksaan bahwa pengguna sendiri berada dalam alur surat tersebut, dan tidak ada
 * pemeriksaan cakupan unit organisasi antara pengguna dan pejabat tujuan. Lebih jauh, syarat
 * penyaring {@code JenisJabatan} berbentuk "kosong berarti terbuka": jenis jabatan yang
 * {@code usernamePengguna} DAN {@code jenisPengguna}-nya sama-sama kosong -- yaitu keadaan
 * bawaan sebuah baris baru -- tampil bagi SETIAP pengguna yang bisa membuka formulir. Kendali
 * yang tersisa karena itu berada di dua tempat lain: hak membuka menu surat masuk itu sendiri,
 * dan konfigurasi manual {@code JenisJabatan} per instalasi.</p>
 *
 * <p>Dua penjagaan yang memang ada dan berfungsi: kotak centang untuk pejabat yang SUDAH
 * didisposisi dimatikan (dihitung sekaligus dari {@link AlurPersetujuanSuratMasukStatus} agar
 * tidak menjadi kueri per pejabat), dan seluruh kotak centang dimatikan ketika surat terkunci
 * ({@link #getDikunci()} tidak {@code null}).</p>
 *
 * <h3>{@code usernamePengguna} BUKAN penyaring hak akses</h3>
 *
 * <p>Meskipun namanya mengesankan daftar pengguna yang berhak, penelusuran seluruh pemanggil
 * menunjukkan {@link #getUsernamePengguna()} hanya dibaca {@code BroadcastHelper} untuk memilih
 * penerima surel pemberitahuan. Tidak ada satu pun kriteria Hibernate yang memakainya untuk
 * membatasi surat masuk yang boleh dilihat. Penyaring hak lihat surat masuk yang sesungguhnya
 * ada di {@code DasboardSurat.createSuratMasukVisibilityCriterion(...)} dan bertumpu pada
 * {@code konseptor} surat serta {@link KlasifikasiSuratMasuk#getKodeGrupPengguna()}.</p>
 *
 * @see KlasifikasiSuratMasuk katalog yang menetapkan penomoran, alur, dan cakupan surat ini
 * @see AlurPersetujuanSuratMasuk definisi simpul alur disposisi
 * @see AlurPersetujuanSuratMasukStatus catatan status per simpul alur untuk surat ini
 * @see SuratKeluar surat keluar yang melahirkan surat masuk ini pada korespondensi antar unit
 * @see NomorSurat mesin format penomoran agenda, dicapai lewat klasifikasi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "surat", name = "surat_masuk")
public class SuratMasuk extends VoKunci {

	/**
	 * Penanda versi serialisasi Java; nilainya sama dengan hampir seluruh entitas hasil templat
	 * hbm2java di basis kode ini karena disalin dari templat generator yang sama, bukan karena
	 * kelas-kelas tersebut kompatibel secara biner satu sama lain.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris, kolom {@code id}; dibangkitkan basis data (IDENTITY). */
	private Long id;
	/** Nomor urut agenda dalam bentuk angka, dipakai jalur penomoran cadangan. */
	private Long index;
	/** Nama tampil pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String oleh;
	/** Id pengguna yang terakhir menyunting baris ini (jejak audit bayangan). */
	private String olehId;

	/**
	 * Id pengguna penyunting terakhir.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * <p>Pengabaian nilai kosong adalah KEHARUSAN TEKNIS pada kelas ini, bukan cacat. Surat
	 * masuk ikut tersimpan ulang oleh jalur-jalur otomatis yang berjalan tanpa konteks pengguna
	 * aktif: pembuatan surat masuk dari persetujuan surat keluar, penulisan balik nilai turunan
	 * oleh getter-getter yang menimpa atribut dari klasifikasi, serta penyimpanan nilai
	 * parameter tambahan oleh listener widget. Bila setter ini menerima nilai kosong, jejak
	 * "diubah oleh siapa" akan terhapus oleh proses yang bukan perbuatan manusia.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks surat, yaitu NOMOR AGENDA-nya apa adanya.
	 *
	 * <p>Nilai diambil langsung dari field {@code kode}, bukan lewat {@link #getKode()},
	 * sehingga bisa {@code null} dan tidak di-{@code trim}. Metode ini yang muncul pada kotak
	 * pilihan surat masuk, pada dialog pemilih lampiran surat keluar, dan pada label ringkas
	 * di berbagai dasbor -- karena itu surat yang belum bernomor agenda tampil sebagai teks
	 * kosong di sana.</p>
	 *
	 * @return nomor agenda surat, mungkin {@code null}
	 */
	public String toString() {
		return kode;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong dengan alasan yang
	 * sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau hanya spasi
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna penyunting terakhir.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA: menyerahkan pembaruan stempel waktu dan identitas pengubah
	 * ke {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate menulis perubahan
	 * baris ini ke basis data.
	 *
	 * <p>Kait hanya berjalan pada UPDATE, bukan INSERT; nilai awal {@code tanggal_dirubah}
	 * karena itu diberikan lewat inisialisasi field pada deklarasinya. Kelas beranotasi
	 * {@code @Audited} sehingga Envers tetap menyimpan riwayat versi terpisah; trio
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah jejak audit BAYANGAN yang
	 * menempel pada baris hidup agar daftar dan formulir tidak perlu menyentuh tabel revisi
	 * hanya untuk menampilkan "diubah oleh siapa, kapan" -- keharusan teknis, bukan duplikasi
	 * yang keliru.</p>
	 *
	 * <p>Pada kelas ini kait tersebut sering terpicu tanpa perbuatan pengguna, karena banyak
	 * getter di sini menulis nilai turunan dari klasifikasi ke field miliknya sendiri. Sebuah
	 * surat bisa tercatat "diubah" hanya karena pernah dibaca.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diperbarui {@link #onUpdate()} pada setiap UPDATE. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi karena
	 * {@link #onUpdate()} sudah mengurusnya; disediakan untuk impor dan perbaikan data.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Selain sebagai jejak audit, nilai ini menjadi cadangan {@link #getWaktu()} bila waktu
	 * dokumen belum pernah diisi.</p>
	 *
	 * @return stempel waktu perubahan terakhir; untuk objek baru berisi waktu pembuatan objek
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nomor surat MILIK PENGIRIM yang tercetak pada kop surat yang diterima. */
	private String noSurat;
	/** NOMOR AGENDA internal hasil registrasi; dibangkitkan lewat klasifikasi dan {@link NomorSurat}. */
	private String kode;
	/** Status keaslian berkas fisik ("Asli", salinan, dan sejenisnya); bawaan "Asli". */
	private String status;
	/** Sifat surat dalam bentuk teks; ditimpa nama {@link SifatSurat} milik klasifikasi. */
	private String sifat;
	/** Sifat surat sebagai entitas; ditimpa nilai milik klasifikasi bila klasifikasi mengisi. */
	private SifatSurat sifatSurat;
	/** Tingkat kerahasiaan dalam bentuk teks; bawaan "Biasa". Tidak diturunkan dari klasifikasi. */
	private String kerahasiaan;
	/** Judul atau nama dokumen surat masuk ini. */
	private String nama;
	/** Catatan bebas mengenai surat ini. */
	private String keterangan;
	/** Klasifikasi surat; sumber kebenaran bagi penomoran, alur, sifat, dan cakupan unit. */
	private KlasifikasiSuratMasuk klasifikasiSuratMasuk;
	/** Simpul alur disposisi yang dipakai surat ini; ditimpa nilai milik klasifikasi. */
	private AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk;
	/** Tanggal surat DITERIMA dan diregistrasi ke buku agenda. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();

	/** Tanggal yang tertera PADA surat itu sendiri, yaitu tanggal penulisan oleh pengirim. */
	private Date tanggalSurat = ais.ui.util.WaktuUtil.getDate();
	/** Tahun agenda; dipakai penyaring pencacahan nomor urut dan pelaporan statistik. */
	private Integer tahun;
	/** Bulan agenda (1-12); dipakai pelaporan statistik. */
	private Integer bulan;
	/** Instansi atau orang pengirim surat, dalam bentuk teks bebas. */
	private String asal;
	/** Jabatan yang dituju surat ini menurut alamat pada surat. */
	private Jabatan tujuan;
	/** Loker arsip fisik tempat berkas surat disimpan. */
	private LokerSurat loker;
	/** Perihal surat; terisi otomatis dari perihal bawaan klasifikasi selama masih kosong. */
	private String perihal;
	/** Keterangan lampiran berkas; ditampilkan "Tanpa berkas" bila kosong. */
	private String lampiran;
	/** Tanggal surat diteruskan kepada pejabat tujuan disposisi. */
	private Date tanggalDiteruskan = ais.ui.util.WaktuUtil.getDate();

	/** Kotak centang lembar disposisi: "Simpan". */
	private Boolean simpan = false;
	/** Kotak centang lembar disposisi: "Balas". */
	private Boolean balas = false;
	/** Kotak centang lembar disposisi: "Perbanyak". */
	private Boolean perbanyak = false;
	/** Kotak centang lembar disposisi: "Teliti". */
	private Boolean teliti = false;
	/** Kotak centang lembar disposisi: "Ikuti perkembangan". */
	private Boolean ikutiPerkembangan = false;
	/** Kotak centang lembar disposisi: "Harap penjelasan masalah". */
	private Boolean harapPenjelasanMasalah = false;
	/** Kotak centang lembar disposisi: "Untuk diproses". */
	private Boolean untukDiproses = false;
	/** Kotak centang lembar disposisi: "Saran-saran". */
	private Boolean saranSaran = false;
	/** Kotak centang lembar disposisi: "Pakai sebagai pedoman". */
	private Boolean pakaiSebagaiPedoman = false;
	/** Kotak centang lembar disposisi: "Bicarakan dengan saya". */
	private Boolean bicarakanDenganSaya = false;
	/** Kotak centang lembar disposisi: "Fotokopi untuk saya". */
	private Boolean fotocopyUntukSaya = false;

	/** Ringkasan isi surat dalam HTML; disunting lewat editor kaya teks dan dikunci saat surat dibekukan. */
	private String ringkasan;
	/** Catatan koreksi terhadap isi surat. */
	private String koreksi;
	/** Penanda pejabat berwenang dalam bentuk teks bebas. */
	private String pejabatBerwenang;
	/** Nama pejabat berwenang dalam bentuk teks bebas. */
	private String namaPejabatBerwenang;

	/** Isi lengkap surat dalam bentuk teks panjang. */
	private String isi;
	/**
	 * Instruksi disposisi dari pimpinan. Field ini menopang DUA kolom sekaligus
	 * ({@code catatan_disposisi} dan {@code catatan_revisi}); lihat {@link #getCatatanRevisi()}.
	 */
	private String catatanDisposisi;
	/** Cakupan jurusan; ditimpa nilai milik klasifikasi bila klasifikasi mengisi. */
	private Jurusan jurusan;
	/** Cakupan fakultas; ditimpa nilai milik klasifikasi bila klasifikasi mengisi. */
	private Fakultas fakultas;
	// private SatuanKerja satuanKerja;
	/** Cakupan yayasan; ditimpa nilai milik klasifikasi bila klasifikasi mengisi. */
	private Yayasan yayasan;
	/** Cakupan sekolah; ditimpa nilai milik klasifikasi bila klasifikasi mengisi. */
	private Sekolah sekolah;
	/** Cakupan satuan kerja; klasifikasi DIDAHULUKAN atas nilai milik surat sendiri. */
	private SatuanKerja satuanKerja;
	/**
	 * Daftar id pengguna penerima surel pemberitahuan, berpembatas koma. BUKAN penyaring hak
	 * akses; lihat javadoc kelas dan {@link #getUsernamePengguna()}.
	 */
	private String usernamePengguna;
	/** Penanda surat ini disiarkan lewat surel kepada daftar penerima. */
	private Boolean broadcast;

	/** Surat keluar yang melahirkan surat masuk ini pada korespondensi antar unit. */
	private SuratKeluar suratKeluar;

	/** JSON berisi himpunan id {@code Pejabat} tujuan disposisi yang dicentang operator. */
	private String jenisSurats;
	/** Simpul disposisi SOP; diwajibkan kelas induk {@code DataSop} dan menentukan {@link #getAktif()}. */
	private DisposisiSop disposisiSop;
	/** Pengguna yang meregistrasi surat ini; dipakai penyaring hak lihat pada dasbor. */
	private Tbmuser konseptor;
	/** Stempel waktu dokumen; jatuh ke {@code tanggal_dirubah} bila belum diisi. */
	private Date waktu;
	/** Simpul alur tempat surat ini ditolak, bila pernah ditolak. */
	private AlurPersetujuanSuratMasukStatus alurDitolak;
	/** Penanda surat masih berjalan; DITURUNKAN dari status disposisi SOP oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Pengguna yang membekukan surat ini; selama terisi, formulir dan disposisi dimatikan. */
	private Tbmuser dikunci;

	/** Penanda ragam dokumen, sejajar dengan {@link KlasifikasiSuratMasuk#getTipe()}. */
	private String tipe;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate. Objek yang dibuat lewat konstruktor ini
	 * sudah membawa nilai awal untuk {@code tanggal}, {@code tanggalSurat},
	 * {@code tanggalDiteruskan}, {@code tanggal_dirubah}, dan seluruh kotak centang lembar
	 * disposisi, tetapi belum sah disimpan sampai {@link #setKode} diisi karena kolom
	 * {@code kode} beranotasi {@code nullable = false}.
	 */
	public SuratMasuk() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * @return id baris, atau {@code null} bila surat belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Hanya dipakai Hibernate dan proses impor.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * NOMOR AGENDA surat, dinormalkan: teks kosong dipulangkan sebagai {@code null}.
	 *
	 * <h3>Dari mana nomor ini berasal</h3>
	 *
	 * <p>Nilainya tidak dibangkitkan kelas ini, melainkan diisi
	 * {@code SuratMasukAction.generateCode(...)} pada saat surat disimpan. Metode itu memilih
	 * jalur berdasarkan {@link KlasifikasiSuratMasuk#getNomorSurat()}: bila klasifikasi punya
	 * mesin penomoran, format diserahkan ke {@link NomorSurat#format(Long, java.util.Date)}
	 * dengan urutan yang diambil dari indeks tersimpan mesin itu -- dan dinaikkan lewat
	 * {@code NomorSurat.tambahIndexNomorSurat(...)} -- atau dari pencacahan baris surat masuk
	 * sejenis; bila tidak, dipakai format cadangan
	 * {@code prefix/NNNN/BulanRomawi/Tahun/postfix}. Pada kedua jalur, penanda teks
	 * {@code KODE_KLASIFIKASI} digantikan {@link KlasifikasiSuratMasuk#getKode()}.</p>
	 *
	 * <p>Jalur ketiga melewati keduanya: surat masuk yang lahir otomatis dari persetujuan surat
	 * keluar mendapat nomor agenda dengan menyalin {@code suratKeluar.getAgenda()} apa adanya.</p>
	 *
	 * <p>Normalisasi ke {@code null} di getter ini penting karena {@link #toString()} membaca
	 * field mentah, bukan hasil getter, sehingga surat tanpa nomor tampil sebagai teks kosong
	 * pada kotak pilihan alih-alih sebagai spasi.</p>
	 *
	 * @return nomor agenda tanpa spasi tepi, atau {@code null} bila belum bernomor
	 */
	@Column(name = "kode", nullable = false, length = 50)
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * Mengisi nomor agenda surat.
	 *
	 * <p>Tidak ada penjagaan keunikan di sini maupun di basis data; pencegahan nomor kembar
	 * bertumpu sepenuhnya pada mekanisme urutan di {@link NomorSurat} dan pada penyimpanan
	 * yang disinkronkan ({@code generateCode} dideklarasikan {@code synchronized}).</p>
	 *
	 * @param kode nomor agenda surat
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Catatan bebas mengenai surat ini.
	 *
	 * @return catatan bebas, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", columnDefinition = "text", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas surat.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Cakupan jurusan surat, DITIMPA nilai milik klasifikasi bila klasifikasi mengisinya.
	 *
	 * <p>Relasi LAZY, sehingga proxy dibongkar lebih dulu lewat {@code check(...)} milik
	 * {@code GeneralValueObject}. Sesudah itu, bila {@link #getKlasifikasiSuratMasuk()}
	 * memiliki jurusan, nilai klasifikasi DITULIS ke field ini. Penulisan tersebut ter-flush ke
	 * basis data karena kelas dipetakan {@code dynamicUpdate = true} dengan akses properti,
	 * sehingga jurusan yang pernah diisi berbeda pada surat akan tergantikan secara menetap
	 * pada pembacaan pertama.</p>
	 *
	 * @return jurusan cakupan surat, atau {@code null} bila tidak dibatasi jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);

		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getJurusan() != null) {
			jurusan = getKlasifikasiSuratMasuk().getJurusan();
		}

		return jurusan;
	}

	/**
	 * Menetapkan cakupan jurusan surat. Nilai ini tidak bertahan bila klasifikasi surat
	 * menetapkan jurusan sendiri; lihat {@link #getJurusan()}.
	 *
	 * @param jurusan jurusan cakupan; boleh {@code null}
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Cakupan fakultas surat, DITIMPA nilai milik klasifikasi bila klasifikasi mengisinya,
	 * dengan mekanisme yang sama seperti {@link #getJurusan()}.
	 *
	 * @return fakultas cakupan surat, atau {@code null} bila tidak dibatasi fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getFakultas() != null) {
			fakultas = getKlasifikasiSuratMasuk().getFakultas();
		}

		return fakultas;
	}

	/**
	 * Menetapkan cakupan fakultas surat. Nilai ini tidak bertahan bila klasifikasi surat
	 * menetapkan fakultas sendiri.
	 *
	 * @param fakultas fakultas cakupan; boleh {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Menetapkan klasifikasi surat.
	 *
	 * <p>Ini setter paling berdampak pada kelas ini: klasifikasi menentukan penomoran agenda,
	 * alur disposisi, sifat surat, perihal bawaan, cakupan unit organisasi, dan daftar field
	 * tambahan dinamis yang muncul pada formulir. Mengubahnya pada surat yang sudah berjalan
	 * mengubah pembacaan hampir seluruh atribut surat tersebut.</p>
	 *
	 * @param klasifikasiSuratMasuk klasifikasi surat; boleh {@code null}
	 */
	public void setKlasifikasiSuratMasuk(KlasifikasiSuratMasuk klasifikasiSuratMasuk) {
		this.klasifikasiSuratMasuk = klasifikasiSuratMasuk;
	}

	/**
	 * Klasifikasi surat ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Getter ini dipanggil
	 * dari dalam delapan getter lain di kelas ini yang menurunkan atributnya dari klasifikasi,
	 * sehingga membaca satu atribut surat dapat memicu pemuatan klasifikasi beserta relasi
	 * turunannya.</p>
	 *
	 * @return klasifikasi surat, atau {@code null} bila surat belum diklasifikasikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_masuk", nullable = true)
	public KlasifikasiSuratMasuk getKlasifikasiSuratMasuk() {
		klasifikasiSuratMasuk = check(klasifikasiSuratMasuk);
		return klasifikasiSuratMasuk;
	}

	/**
	 * Mengisi tanggal surat diterima dan diregistrasi.
	 *
	 * @param tanggal tanggal terima; {@code null} akan dipulihkan menjadi tanggal hari ini
	 *                pada pembacaan berikutnya
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Tanggal surat DITERIMA dan diregistrasi ke buku agenda, dengan pemulihan otomatis.
	 *
	 * <p>Getter destruktif ringan: bila field masih {@code null}, tanggal hari ini ditulis ke
	 * field lalu dikembalikan. Pemulihan ini menutup satu jalur yang benar-benar terjadi --
	 * surat masuk yang dibuat otomatis dari persetujuan surat keluar diisi tanggalnya dari
	 * {@code getWaktuDitolak()}, yaitu stempel waktu PENOLAKAN, padahal kode itu berada di
	 * cabang "disetujui" sehingga nilainya selalu {@code null}. Tanpa pemulihan di sini, surat
	 * hasil jalur otomatis akan tersimpan tanpa tanggal terima.</p>
	 *
	 * <p>Bedakan dari {@link #getTanggalSurat()}, yang mencatat tanggal penulisan oleh pengirim.</p>
	 *
	 * @return tanggal terima surat; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggal;
	}

	/**
	 * Tanggal yang tertera PADA surat itu sendiri, yaitu tanggal penulisan oleh pengirim.
	 *
	 * <p>Getter destruktif ringan dengan pola sama seperti {@link #getTanggal()}: {@code null}
	 * dipulihkan menjadi tanggal hari ini dan ditulis ke field. Perlu disadari bahwa pemulihan
	 * itu MENYAMARKAN data yang hilang -- surat lama yang tanggal suratnya memang tidak
	 * tercatat akan tampak seolah bertanggal hari pertama kali barisnya dibaca, dan nilai itu
	 * tersimpan permanen.</p>
	 *
	 * @return tanggal penulisan surat oleh pengirim; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSurat() {
		if (tanggalSurat == null) {
			tanggalSurat = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalSurat;
	}

	/**
	 * Mengisi tanggal penulisan surat oleh pengirim.
	 *
	 * @param tanggalSurat tanggal pada surat; {@code null} akan dipulihkan menjadi tanggal
	 *                     hari ini pada pembacaan berikutnya
	 */
	public void setTanggalSurat(Date tanggalSurat) {
		this.tanggalSurat = tanggalSurat;
	}

	/**
	 * Instansi atau orang pengirim surat, dalam bentuk teks bebas.
	 *
	 * <p>Untuk surat masuk yang lahir otomatis dari persetujuan surat keluar, nilai ini diisi
	 * mesin dengan kalimat "Surat keluar dengan nomor &lt;kode surat keluar&gt;", sehingga asal
	 * surat internal dapat dibedakan dari surat eksternal hanya dari pola teksnya -- tidak ada
	 * kolom penanda tersendiri untuk membedakan keduanya selain {@link #getSuratKeluar()}.</p>
	 *
	 * @return asal surat, atau {@code null} bila belum diisi
	 */
	public String getAsal() {
		return asal;
	}

	/**
	 * Mengisi asal surat.
	 *
	 * @param asal instansi atau orang pengirim; boleh {@code null}
	 */
	public void setAsal(String asal) {
		this.asal = asal;
	}

	/**
	 * Jabatan yang dituju surat ini menurut alamat pada surat.
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, relasi ini TIDAK dipetakan LAZY -- tidak ada
	 * {@code fetch = FetchType.LAZY} sehingga berlaku bawaan {@code @ManyToOne} yang EAGER;
	 * panggilan {@code check(...)} di sini karena itu praktis tidak melakukan apa-apa selain
	 * berjaga-jaga bila objek datang dari sesi lain.</p>
	 *
	 * <p>Perlu dibedakan dari tujuan DISPOSISI: jabatan di sini hanya keterangan alamat surat,
	 * sedangkan pejabat yang benar-benar didisposisi dicatat pada JSON
	 * {@link #getJenisSurats()} dan pada baris-baris {@link AlurPersetujuanSuratMasukStatus}.</p>
	 *
	 * @return jabatan tujuan surat, atau {@code null} bila tidak dicatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "tujuan", nullable = true)
	public Jabatan getTujuan() {
		tujuan = check(tujuan);
		return tujuan;
	}

	/**
	 * Menetapkan jabatan tujuan surat menurut alamat pada surat.
	 *
	 * @param tujuan jabatan tujuan; boleh {@code null}
	 */
	public void setTujuan(Jabatan tujuan) {
		this.tujuan = tujuan;
	}

	/**
	 * Loker arsip fisik tempat berkas surat disimpan.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Loker inilah yang
	 * menjadi dimensi pengelompokan pada laporan statistik surat per loker, dan yang menjadi
	 * acuan modul sirkulasi surat saat berkas dipinjam.</p>
	 *
	 * @return loker arsip, atau {@code null} bila belum ditempatkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "loker", nullable = true)
	public LokerSurat getLoker() {
		loker = check(loker);
		return loker;
	}

	/**
	 * Menetapkan loker arsip fisik surat.
	 *
	 * @param loker loker arsip; boleh {@code null}
	 */
	public void setLoker(LokerSurat loker) {
		this.loker = loker;
	}

	/**
	 * Perihal surat, dengan pengisian otomatis dari perihal bawaan klasifikasi.
	 *
	 * <p>Getter DESTRUKTIF: selama perihal masih kosong dan klasifikasi sudah ditetapkan,
	 * {@link KlasifikasiSuratMasuk#getPerihalDefault()} DITULIS ke field ini, bukan sekadar
	 * dikembalikan. Karena kelas dipetakan {@code dynamicUpdate = true}, perihal bawaan
	 * "menempel" secara menetap pada surat sejak pembacaan pertama. Konsekuensi yang perlu
	 * diketahui: mengubah perihal bawaan pada klasifikasi TIDAK memengaruhi surat lama yang
	 * sudah pernah dibaca, dan mengosongkan perihal sebuah surat tidak mengembalikannya menjadi
	 * "ikut klasifikasi" secara permanen -- pembacaan berikutnya akan mengisinya lagi.</p>
	 *
	 * <p>Perlu dicatat bahwa {@code getPerihalDefault()} pada klasifikasi sendiri jatuh ke nama
	 * klasifikasi bila perihal bawaan belum diisi, sehingga surat yang perihalnya dibiarkan
	 * kosong akan berperihal sama dengan nama klasifikasinya.</p>
	 *
	 * @return perihal surat, atau {@code null} bila belum diisi dan klasifikasi belum ditetapkan
	 */
	public String getPerihal() {
		if ((perihal == null || perihal.trim().isEmpty()) && getKlasifikasiSuratMasuk() != null) {
			perihal = getKlasifikasiSuratMasuk().getPerihalDefault();
		}
		return perihal;
	}

	/**
	 * Mengisi perihal surat.
	 *
	 * @param perihal perihal surat; mengosongkannya hanya bertahan sampai pembacaan berikutnya
	 *                bila klasifikasi sudah ditetapkan
	 */
	public void setPerihal(String perihal) {
		this.perihal = perihal;
	}

	/**
	 * Keterangan lampiran berkas, dengan teks pengganti bila kosong.
	 *
	 * <p>Berbeda dari kebanyakan getter bernilai bawaan di kelas ini, metode ini TIDAK
	 * destruktif: teks {@code "Tanpa berkas"} hanya dikembalikan, tidak ditulis ke field,
	 * sehingga kolomnya tetap {@code null} di basis data. Perlu disadari bahwa teks pengganti
	 * itu tidak dapat dibedakan dari keterangan yang memang diketik operator dengan bunyi sama,
	 * dan formulir surat masuk memuat kotak isian lampiran langsung dari getter ini -- jadi
	 * membuka lalu menyimpan surat yang lampirannya kosong akan MENGABADIKAN teks
	 * {@code "Tanpa berkas"} sebagai isi kolom yang sesungguhnya.</p>
	 *
	 * <p>Keterangan ini murni teks; berkas lampiran yang sebenarnya dikelola entitas terpisah
	 * ({@code FotoGambarSuratMasuk} dan kerabatnya).</p>
	 *
	 * @return keterangan lampiran; {@code "Tanpa berkas"} bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	public String getLampiran() {
		return lampiran == null ? "Tanpa berkas" : lampiran;
	}

	/**
	 * Mengisi keterangan lampiran berkas.
	 *
	 * @param lampiran keterangan lampiran; boleh {@code null}
	 */
	public void setLampiran(String lampiran) {
		this.lampiran = lampiran;
	}

	/**
	 * Tanggal surat diteruskan kepada pejabat tujuan disposisi.
	 *
	 * <p>Getter destruktif ringan: {@code null} dipulihkan menjadi tanggal hari ini dan ditulis
	 * ke field. Nilai ini bersifat catatan tunggal pada header surat dan TIDAK bertingkat --
	 * ia tidak mencatat kapan setiap pejabat menerima disposisinya. Riwayat per pejabat ada di
	 * {@link AlurPersetujuanSuratMasukStatus}, yang menyimpan waktu persetujuan dan waktu
	 * penolakan masing-masing simpul.</p>
	 *
	 * @return tanggal penerusan surat; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalDiteruskan() {
		if (tanggalDiteruskan == null) {
			tanggalDiteruskan = ais.ui.util.WaktuUtil.getDate();
		}
		return tanggalDiteruskan;
	}

	/**
	 * Mengisi tanggal surat diteruskan kepada pejabat tujuan disposisi.
	 *
	 * @param tanggalDiteruskan tanggal penerusan; {@code null} akan dipulihkan menjadi tanggal
	 *                          hari ini pada pembacaan berikutnya
	 */
	public void setTanggalDiteruskan(Date tanggalDiteruskan) {
		this.tanggalDiteruskan = tanggalDiteruskan;
	}

	/**
	 * Kotak centang lembar disposisi "Simpan".
	 *
	 * <h3>Tentang sebelas kotak centang lembar disposisi</h3>
	 *
	 * <p>Kesebelas properti boolean berurutan mulai dari sini ({@code simpan}, {@code balas},
	 * {@code perbanyak}, {@code teliti}, {@code ikutiPerkembangan},
	 * {@code harapPenjelasanMasalah}, {@code untukDiproses}, {@code saranSaran},
	 * {@code pakaiSebagaiPedoman}, {@code bicarakanDenganSaya}, {@code fotocopyUntukSaya})
	 * adalah salinan digital dari kotak-kotak instruksi pada lembar disposisi kertas. Semuanya
	 * berpola sama dan sengaja dibiarkan sebagai kolom terpisah, bukan sebagai satu kolom
	 * himpunan, agar dapat langsung dipetakan ke kotak centang pada templat cetak lembar
	 * disposisi.</p>
	 *
	 * <p>Perhatikan bahwa getter-getter ini -- berbeda dari kebanyakan getter boolean di basis
	 * kode ini -- TIDAK menormalkan {@code null} menjadi {@code false}. Nilai awal
	 * {@code false} hanya diberikan pada deklarasi field, sehingga objek baru aman, tetapi
	 * baris lama yang tersimpan sebelum kolom-kolom ini ada dapat memulangkan {@code null}.
	 * Pemanggil yang membandingkan hasilnya secara langsung harus memakai
	 * {@code Boolean.TRUE.equals(...)} atau memeriksa {@code null} lebih dulu agar tidak
	 * melempar {@code NullPointerException} saat auto-unboxing.</p>
	 *
	 * <p>Instruksi-instruksi ini bersifat catatan pada header surat: tidak satu pun di antaranya
	 * mengubah alur disposisi, memicu pemberitahuan, atau menjadi syarat bagi langkah
	 * berikutnya. Pengaruhnya murni pada lembar disposisi yang dicetak.</p>
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getSimpan() {
		return simpan;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Simpan".
	 *
	 * @param simpan {@code true} untuk mencentang instruksi ini
	 */
	public void setSimpan(Boolean simpan) {
		this.simpan = simpan;
	}

	/**
	 * Kotak centang lembar disposisi "Balas".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getBalas() {
		return balas;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Balas".
	 *
	 * @param balas {@code true} untuk mencentang instruksi ini
	 */
	public void setBalas(Boolean balas) {
		this.balas = balas;
	}

	/**
	 * Kotak centang lembar disposisi "Perbanyak".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getPerbanyak() {
		return perbanyak;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Perbanyak".
	 *
	 * @param perbanyak {@code true} untuk mencentang instruksi ini
	 */
	public void setPerbanyak(Boolean perbanyak) {
		this.perbanyak = perbanyak;
	}

	/**
	 * Kotak centang lembar disposisi "Teliti".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getTeliti() {
		return teliti;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Teliti".
	 *
	 * @param teliti {@code true} untuk mencentang instruksi ini
	 */
	public void setTeliti(Boolean teliti) {
		this.teliti = teliti;
	}

	/**
	 * Kotak centang lembar disposisi "Ikuti perkembangan".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getIkutiPerkembangan() {
		return ikutiPerkembangan;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Ikuti perkembangan".
	 *
	 * @param ikutiPerkembangan {@code true} untuk mencentang instruksi ini
	 */
	public void setIkutiPerkembangan(Boolean ikutiPerkembangan) {
		this.ikutiPerkembangan = ikutiPerkembangan;
	}

	/**
	 * Kotak centang lembar disposisi "Harap penjelasan masalah".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getHarapPenjelasanMasalah() {
		return harapPenjelasanMasalah;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Harap penjelasan masalah".
	 *
	 * @param harapPenjelasanMasalah {@code true} untuk mencentang instruksi ini
	 */
	public void setHarapPenjelasanMasalah(Boolean harapPenjelasanMasalah) {
		this.harapPenjelasanMasalah = harapPenjelasanMasalah;
	}

	/**
	 * Kotak centang lembar disposisi "Untuk diproses".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getUntukDiproses() {
		return untukDiproses;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Untuk diproses".
	 *
	 * @param untukDiproses {@code true} untuk mencentang instruksi ini
	 */
	public void setUntukDiproses(Boolean untukDiproses) {
		this.untukDiproses = untukDiproses;
	}

	/**
	 * Kotak centang lembar disposisi "Saran-saran".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getSaranSaran() {
		return saranSaran;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Saran-saran".
	 *
	 * @param saranSaran {@code true} untuk mencentang instruksi ini
	 */
	public void setSaranSaran(Boolean saranSaran) {
		this.saranSaran = saranSaran;
	}

	/**
	 * Kotak centang lembar disposisi "Pakai sebagai pedoman".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getPakaiSebagaiPedoman() {
		return pakaiSebagaiPedoman;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Pakai sebagai pedoman".
	 *
	 * @param pakaiSebagaiPedoman {@code true} untuk mencentang instruksi ini
	 */
	public void setPakaiSebagaiPedoman(Boolean pakaiSebagaiPedoman) {
		this.pakaiSebagaiPedoman = pakaiSebagaiPedoman;
	}

	/**
	 * Kotak centang lembar disposisi "Bicarakan dengan saya".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getBicarakanDenganSaya() {
		return bicarakanDenganSaya;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Bicarakan dengan saya".
	 *
	 * @param bicarakanDenganSaya {@code true} untuk mencentang instruksi ini
	 */
	public void setBicarakanDenganSaya(Boolean bicarakanDenganSaya) {
		this.bicarakanDenganSaya = bicarakanDenganSaya;
	}

	/**
	 * Kotak centang lembar disposisi "Fotokopi untuk saya".
	 *
	 * @return {@code true} bila instruksi ini dicentang; dapat {@code null} untuk baris lama
	 *         yang tersimpan sebelum kolomnya ada
	 */
	public Boolean getFotocopyUntukSaya() {
		return fotocopyUntukSaya;
	}

	/**
	 * Mencentang atau melepas instruksi lembar disposisi "Fotokopi untuk saya".
	 *
	 * @param fotocopyUntukSaya {@code true} untuk mencentang instruksi ini
	 */
	public void setFotocopyUntukSaya(Boolean fotocopyUntukSaya) {
		this.fotocopyUntukSaya = fotocopyUntukSaya;
	}

	/**
	 * Ringkasan isi surat dalam bentuk HTML.
	 *
	 * <p>Disunting lewat editor kaya teks pada tab "Ringkasan" formulir surat masuk. Formulir
	 * itu MENGGANTI editor dengan tampilan HTML statis ketika surat terkunci
	 * ({@link #getDikunci()} tidak {@code null}), sehingga penguncian benar-benar menghentikan
	 * penyuntingan ringkasan, bukan sekadar menyembunyikan tombol simpan.</p>
	 *
	 * <p>Karena isinya HTML mentah dari editor, pemakai nilai ini pada perakitan tampilan
	 * ringkas surat melewatkannya dulu ke penyaring {@code MyHtml.bersihkan(...)} sebelum
	 * dirender.</p>
	 *
	 * @return ringkasan isi surat dalam HTML, atau {@code null} bila belum diisi
	 */
	@Column(name = "ringkasan", columnDefinition = "text", nullable = true)
	public String getRingkasan() {
		return ringkasan;
	}

	/**
	 * Mengisi ringkasan isi surat.
	 *
	 * @param ringkasan ringkasan dalam bentuk HTML; boleh {@code null}
	 */
	public void setRingkasan(String ringkasan) {
		this.ringkasan = ringkasan;
	}

	/**
	 * Catatan koreksi terhadap isi surat.
	 *
	 * @return catatan koreksi, atau {@code null} bila tidak ada
	 */
	@Column(name = "koreksi", columnDefinition = "text", nullable = true)
	public String getKoreksi() {
		return koreksi;
	}

	/**
	 * Mengisi catatan koreksi terhadap isi surat.
	 *
	 * @param koreksi catatan koreksi; boleh {@code null}
	 */
	public void setKoreksi(String koreksi) {
		this.koreksi = koreksi;
	}

	/**
	 * Penanda pejabat berwenang atas surat ini, dalam bentuk teks bebas.
	 *
	 * <p>Berpasangan dengan {@link #getNamaPejabatBerwenang()}. Keduanya teks lepas, TIDAK
	 * bertaut ke entitas {@code Pejabat} maupun {@code Tbmuser} mana pun, sehingga tidak dapat
	 * dipakai sebagai dasar kendali akses atau penelusuran tanggung jawab -- isinya sekadar
	 * apa yang diketik operator untuk dicetak pada lembar disposisi.</p>
	 *
	 * @return penanda pejabat berwenang, atau {@code null} bila tidak diisi
	 */
	public String getPejabatBerwenang() {
		return pejabatBerwenang;
	}

	/**
	 * Mengisi penanda pejabat berwenang.
	 *
	 * @param pejabatBerwenang penanda pejabat berwenang; boleh {@code null}
	 */
	public void setPejabatBerwenang(String pejabatBerwenang) {
		this.pejabatBerwenang = pejabatBerwenang;
	}

	/**
	 * Nama pejabat berwenang atas surat ini, dalam bentuk teks bebas.
	 *
	 * @return nama pejabat berwenang, atau {@code null} bila tidak diisi
	 */
	public String getNamaPejabatBerwenang() {
		return namaPejabatBerwenang;
	}

	/**
	 * Mengisi nama pejabat berwenang.
	 *
	 * @param namaPejabatBerwenang nama pejabat berwenang; boleh {@code null}
	 */
	public void setNamaPejabatBerwenang(String namaPejabatBerwenang) {
		this.namaPejabatBerwenang = namaPejabatBerwenang;
	}

	/**
	 * Mengisi teks lengkap surat.
	 *
	 * @param isi isi surat; boleh {@code null}
	 */
	public void setIsi(String isi) {
		this.isi = isi;
	}

	/**
	 * Isi lengkap surat dalam bentuk teks panjang.
	 *
	 * <p>Bedakan dari {@link #getRingkasan()}, yang memuat ringkasan HTML hasil editor kaya
	 * teks. Kolom ini menampung salinan teks utuh surat -- misalnya hasil pemindaian yang
	 * sudah dikenali sebagai teks -- dan tidak dirender sebagai HTML di mana pun.</p>
	 *
	 * @return isi lengkap surat, atau {@code null} bila belum diisi
	 */
	@Column(name = "isi", columnDefinition = "text", nullable = true)
	public String getIsi() {
		return isi;
	}

	/**
	 * Mengisi nomor urut agenda dalam bentuk angka.
	 *
	 * @param index nomor urut agenda; boleh {@code null}
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Nomor urut agenda dalam bentuk angka, terpisah dari {@link #getKode()} yang sudah
	 * berformat.
	 *
	 * <p>Nilai ini dipakai jalur penomoran CADANGAN saja: {@code SuratMasukAction.getindex(...)}
	 * mencari nilai TERBESAR kolom ini di antara surat sekelompok pada tahun berjalan, lalu
	 * menambahkannya satu untuk nomor berikutnya. Jalur bermesin {@link NomorSurat} tidak
	 * membacanya sama sekali, melainkan mencacah jumlah baris atau memakai indeks tersimpan
	 * pada mesin penomoran.</p>
	 *
	 * <p>Perlu diketahui bahwa kolom ini tidak dijamin unik dan tidak dijamin terisi: surat
	 * yang dibuat lewat jalur bermesin penomoran maupun lewat pembuatan otomatis dari surat
	 * keluar meninggalkannya {@code null}. Pencarian nilai terbesar karena itu hanya bermakna
	 * pada instalasi yang seluruh klasifikasinya memakai jalur cadangan.</p>
	 *
	 * @return nomor urut agenda, atau {@code null} bila surat tidak melewati jalur penomoran
	 *         cadangan
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Simpul alur disposisi yang berlaku bagi surat ini, DITIMPA nilai milik klasifikasi.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Bila
	 * {@link #getKlasifikasiSuratMasuk()} membawa alur, nilai klasifikasi DITULIS ke field ini
	 * dan karenanya tersimpan secara menetap. Formulir surat masuk memperkuat penimpaan itu
	 * dengan me-{@code setDisabled(true)} kotak pilihan alur begitu klasifikasi membawa alur,
	 * sehingga operator tidak dapat menyimpang dari alur yang ditetapkan klasifikasi.</p>
	 *
	 * <p>Ingat bahwa {@link KlasifikasiSuratMasuk#getAlurPersetujuanSuratMasuk()} sendiri
	 * memulangkan {@code null} bila klasifikasi ditandai "tanpa alur" -- dan penandaan itu
	 * menghapus relasi alur pada klasifikasi secara permanen. Jadi surat yang klasifikasinya
	 * kemudian ditandai tanpa alur akan mempertahankan alur yang sudah tertulis pada dirinya
	 * sendiri, sementara surat baru berklasifikasi sama tidak lagi mendapat alur.</p>
	 *
	 * <p>Dari simpul yang dikembalikan di sini,
	 * {@code SuratMasukAction.checkAlurPersetujuanSuratMasukStatus(...)} membentangkan rantai
	 * disposisi: ia membuat satu {@link AlurPersetujuanSuratMasukStatus} untuk simpul tersebut,
	 * lalu menelusuri {@code getParent()} sampai akar dan membuat satu baris status untuk
	 * setiap tingkat di atasnya.</p>
	 *
	 * @return simpul alur disposisi surat ini, atau {@code null} bila surat berjalan tanpa alur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "alur_persetujuan_surat_masuk", nullable = true)
	public AlurPersetujuanSuratMasuk getAlurPersetujuanSuratMasuk() {
		alurPersetujuanSuratMasuk = check(alurPersetujuanSuratMasuk);

		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getAlurPersetujuanSuratMasuk() != null) {
			alurPersetujuanSuratMasuk = getKlasifikasiSuratMasuk().getAlurPersetujuanSuratMasuk();
		}

		return alurPersetujuanSuratMasuk;
	}

	/**
	 * Menetapkan simpul alur disposisi surat ini.
	 *
	 * <p>Nilai yang diisi di sini akan tertimpa oleh alur milik klasifikasi pada pembacaan
	 * berikutnya bila klasifikasi membawa alur.</p>
	 *
	 * @param alurPersetujuanSuratMasuk simpul alur disposisi; boleh {@code null}
	 */
	public void setAlurPersetujuanSuratMasuk(AlurPersetujuanSuratMasuk alurPersetujuanSuratMasuk) {
		this.alurPersetujuanSuratMasuk = alurPersetujuanSuratMasuk;
	}

	/**
	 * Judul atau nama dokumen surat masuk ini.
	 *
	 * <p>Berbeda dari {@link #getPerihal()}, nilai ini TIDAK memiliki pengisian otomatis dari
	 * klasifikasi dan tidak dinormalkan. Ia ikut disertakan pada isi kode QR yang dibangkitkan
	 * {@link #ttdQr()}, sehingga surat tanpa nama menghasilkan baris kosong di sana.</p>
	 *
	 * @return judul dokumen, atau {@code null} bila belum diisi
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Mengisi judul atau nama dokumen surat masuk.
	 *
	 * @param nama judul dokumen; boleh {@code null}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Tahun agenda surat, dengan pemulihan otomatis ke tahun berjalan.
	 *
	 * <p>Getter destruktif ringan: bila field masih {@code null}, tahun berjalan ditulis ke
	 * field lalu dikembalikan. Kolom ini bukan sekadar keterangan -- ia dipakai sebagai
	 * PENYARING pada pencacahan nomor urut di {@code SuratMasukAction.getindexSurat(...)} dan
	 * {@code getindex(...)} ketika mesin penomoran menyalakan {@code resetUrutanTiapTahun},
	 * serta menjadi dimensi pengelompokan pada laporan statistik surat.</p>
	 *
	 * <p>Perlu diketahui bahwa pemulihan di sini memakai TAHUN SAAT DIBACA, bukan tahun
	 * {@link #getTanggal()}. Untuk surat lama yang kolom tahunnya kosong, pembacaan pertama di
	 * tahun berjalan akan menandainya sebagai surat tahun ini secara permanen -- dan karena
	 * kolom ini menjadi penyaring pencacahan nomor urut, penandaan yang keliru itu ikut
	 * menggeser nomor agenda yang dibangkitkan berikutnya.</p>
	 *
	 * @return tahun agenda; tahun berjalan bila belum pernah diisi, tidak pernah {@code null}
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi tahun agenda surat.
	 *
	 * @param tahun tahun agenda; {@code null} akan dipulihkan menjadi tahun berjalan pada
	 *              pembacaan berikutnya
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Bulan agenda surat dalam penomoran manusia (1 sampai 12), dengan pemulihan otomatis.
	 *
	 * <p>Getter destruktif ringan dengan pola sama seperti {@link #getTahun()}. Perhatikan
	 * penambahan satu pada nilai pemulihannya: {@code Calendar.MONTH} berbasis nol, sehingga
	 * nilai yang disimpan di kolom ini memang satu lebih besar daripada nilai {@code Calendar}
	 * -- Januari bernilai 1, bukan 0. Penyesuaian itu perlu diingat saat menulis kueri laporan
	 * langsung ke basis data.</p>
	 *
	 * <p>Berbeda dari {@link #getTahun()}, kolom bulan TIDAK dipakai sebagai penyaring
	 * pencacahan nomor urut; ia hanya menjadi dimensi pengelompokan pada laporan statistik.</p>
	 *
	 * @return bulan agenda dalam rentang 1 sampai 12; bulan berjalan bila belum pernah diisi,
	 *         tidak pernah {@code null}
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Mengisi bulan agenda surat dalam penomoran manusia (1 sampai 12).
	 *
	 * @param bulan bulan agenda; {@code null} akan dipulihkan menjadi bulan berjalan pada
	 *              pembacaan berikutnya
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Nomor surat MILIK PENGIRIM, dengan penimpaan dari surat keluar yang tertaut.
	 *
	 * <h3>Getter destruktif: nomor ketikan operator dapat tergantikan</h3>
	 *
	 * <p>Bila {@link #getSuratKeluar()} terisi -- yaitu pada surat masuk hasil korespondensi
	 * antar unit di dalam instansi -- metode ini MENULIS {@code suratKeluar.getKode()} ke field
	 * {@code noSurat} lalu mengembalikannya. Penulisan itu ter-flush ke basis data karena kelas
	 * dipetakan {@code dynamicUpdate = true} dengan akses properti lewat getter. Logikanya
	 * masuk akal (nomor surat pengirim memang nomor surat keluar itu), tetapi konsekuensinya
	 * nomor yang mungkin sudah diketik operator tergantikan secara menetap pada pembacaan
	 * pertama, dan tidak ada cara mengembalikannya selain lewat tabel revisi Envers.</p>
	 *
	 * <h3>Jangan tertukar dengan nomor agenda</h3>
	 *
	 * <p>Nilai di sini BUKAN {@link #getKode()}. {@code getKode()} adalah nomor registrasi yang
	 * diberikan instansi penerima lewat klasifikasi dan mesin {@link NomorSurat}; nilai di sini
	 * adalah nomor yang tercetak pada kop surat yang diterima. Formulir surat masuk menolak
	 * penyimpanan bila kotak isian nomor surat ini kosong, sehingga kolomnya praktis wajib
	 * untuk surat yang diregistrasi manual -- tetapi TIDAK wajib untuk surat yang lahir
	 * otomatis dari persetujuan surat keluar, yang melewati validasi formulir.</p>
	 *
	 * @return nomor surat pengirim; disalin dari kode surat keluar bila surat ini lahir dari
	 *         korespondensi antar unit, atau {@code null} bila belum diisi
	 */
	public String getNoSurat() {
		if (getSuratKeluar() != null) {
			noSurat = getSuratKeluar().getKode();
		}
		return noSurat;
	}

	/**
	 * Mengisi nomor surat milik pengirim.
	 *
	 * <p>Nilai ini tidak bertahan bila surat tertaut ke sebuah {@link SuratKeluar}; lihat
	 * {@link #getNoSurat()}.</p>
	 *
	 * @param noSurat nomor surat pengirim; boleh {@code null}
	 */
	public void setNoSurat(String noSurat) {
		this.noSurat = noSurat;
	}

	/**
	 * INSTRUKSI disposisi dari pimpinan, yaitu apa yang harus dilakukan penerima disposisi.
	 *
	 * <p>Nilai ini disalin menjadi keterangan awal simpul alur ketika rantai disposisi
	 * dibentangkan {@code SuratMasukAction.checkAlurPersetujuanSuratMasukStatus(...)}, sehingga
	 * simpul pertama mewarisi instruksi pimpinan sebagai catatannya. Karena itu pula
	 * {@link #getJawabanPenerimaDisposisi()} menyaring keluar jawaban yang isinya sama persis
	 * dengan instruksi ini -- lihat javadoc metode tersebut.</p>
	 *
	 * <p>PENTING: field yang menopang getter ini juga menopang {@link #getCatatanRevisi()},
	 * yang dipetakan ke kolom BERBEDA. Lihat javadoc metode itu untuk akibatnya.</p>
	 *
	 * @return instruksi disposisi pimpinan, atau {@code null} bila belum diisi
	 */
	@Column(name = "catatan_disposisi", columnDefinition = "text", nullable = true)
	public String getCatatanDisposisi() {
		return catatanDisposisi;
	}

	/**
	 * Mengisi instruksi disposisi dari pimpinan.
	 *
	 * <p>Setter ini menulis ke field yang sama dengan {@link #setCatatanRevisi(String)};
	 * memanggil salah satunya mengubah nilai yang dibaca keduanya.</p>
	 *
	 * @param catatanDisposisi instruksi disposisi; boleh {@code null}
	 */
	public void setCatatanDisposisi(String catatanDisposisi) {
		this.catatanDisposisi = catatanDisposisi;
	}

	/**
	 * Instruksi disposisi pimpinan dalam bentuk yang aman untuk templat cetak, yaitu
	 * {@link #getCatatanDisposisi()} dengan {@code null} diganti teks kosong.
	 *
	 * <p>Properti {@code @Transient} ini ada semata-mata agar templat laporan dan mekanisme
	 * ekspor -- yang memanggil properti lewat nama dan tidak menangani {@code null} -- dapat
	 * memakainya langsung. Namanya sengaja disebut "isi disposisi pimpinan" agar terbaca
	 * berpasangan dengan {@link #getJawabanPenerimaDisposisi()} pada daftar kolom ekspor.</p>
	 *
	 * @return instruksi disposisi pimpinan; teks kosong bila belum diisi, tidak pernah
	 *         {@code null}
	 */
	@Transient
	public String getIsiDisposisiPimpinan() {
		return getCatatanDisposisi() == null ? "" : getCatatanDisposisi();
	}

	/**
	 * Rangkuman JAWABAN seluruh penerima disposisi surat ini dalam satu blok teks, satu baris
	 * per penerima.
	 *
	 * <h3>Apa yang dirangkum</h3>
	 *
	 * <p>Metode ini membaca seluruh {@link AlurPersetujuanSuratMasukStatus} milik surat ini yang
	 * {@code kodeUnik}-nya terisi -- syarat itu menyaring keluar baris setengah jadi -- lalu
	 * mengurutkannya menurut id (yaitu urutan pembuatan, yang mengikuti urutan pembentangan
	 * rantai alur dari simpul terpilih ke atas). Untuk setiap baris dirangkai satu baris teks
	 * berbentuk {@code "<jenis jabatan> - <nama penerima> | <status> | <waktu> | <catatan>"},
	 * dengan bagian yang kosong dilewati. Status diterjemahkan menjadi "Disetujui", "Ditolak",
	 * atau "Menunggu Persetujuan", dan stempel waktunya diambil dari waktu penolakan bila
	 * ditolak, selain itu dari waktu persetujuan.</p>
	 *
	 * <h3>Penyaringan gema instruksi pimpinan</h3>
	 *
	 * <p>Baris yang catatannya SAMA PERSIS dengan {@link #getCatatanDisposisi()} dilewati
	 * seluruhnya. Penyaringan itu diperlukan karena pembentangan rantai alur menyalin instruksi
	 * pimpinan menjadi keterangan simpul pertama; tanpa penyaringan, instruksi yang sama akan
	 * muncul kembali seolah-olah jawaban seorang penerima. Perbandingannya berbasis kesamaan
	 * teks setelah {@code trim}, sehingga penerima yang kebetulan menjawab dengan kalimat
	 * persis sama dengan instruksi pimpinan akan IKUT TERSARING dan jawabannya tidak tampil --
	 * batasan yang melekat pada pendekatan ini.</p>
	 *
	 * <h3>Sifat teknis</h3>
	 *
	 * <p>Properti ini {@code @Transient}, jadi tidak dipetakan ke kolom mana pun, tetapi ia
	 * MELAKUKAN AKSES BASIS DATA: satu kueri {@code Criteria} per pemanggilan, ditambah
	 * pemuatan jenis jabatan dan penerima tiap baris. Karena namanya ikut terdaftar pada daftar
	 * kolom ekspor surat masuk, memanggilnya untuk banyak surat sekaligus berarti satu kueri
	 * per surat. Metode ini memulangkan teks kosong lebih awal bila surat belum bersimpan
	 * ({@code getId()} masih {@code null}), sehingga aman dipanggil pada objek baru.</p>
	 *
	 * <p>Seluruh badan metode dibungkus {@code try/catch} yang mencatat galat ke
	 * {@code ErrorAuditUtil} lalu memulangkan teks kosong. Perilaku gagal-diam itu disengaja
	 * agar satu surat bermasalah tidak menggagalkan pencetakan atau ekspor seluruh daftar --
	 * tetapi berarti kolom yang kosong pada hasil ekspor tidak dapat dibedakan antara "memang
	 * belum ada jawaban" dan "terjadi galat saat merangkum".</p>
	 *
	 * @return rangkuman jawaban penerima disposisi, satu baris per penerima; teks kosong bila
	 *         surat belum tersimpan, belum ada jawaban, atau terjadi galat
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public String getJawabanPenerimaDisposisi() {
		if (getId() == null) {
			return "";
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<AlurPersetujuanSuratMasukStatus> daftar = session
					.createCriteria(AlurPersetujuanSuratMasukStatus.class)
					.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.eq("suratMasuk", this))
					.addOrder(Order.asc("id")).list();
			StringBuilder hasil = new StringBuilder();
			String instruksi = getCatatanDisposisi() == null ? "" : getCatatanDisposisi().trim();
			for (AlurPersetujuanSuratMasukStatus jawaban : daftar) {
				String catatan = jawaban.getKeterangan() == null ? "" : jawaban.getKeterangan().trim();
				if (!catatan.isEmpty() && catatan.equals(instruksi)) {
					continue;
				}
				String penerima = ais.action.master.surat.SuratMasukAction.namaPenerimaDisposisiMasuk(jawaban);
				String jabatan = jawaban.getJenisJabatan() == null ? "" : jawaban.getJenisJabatan().getNama();
				String statusJawaban = Boolean.TRUE.equals(jawaban.getDisetujui()) ? "Disetujui"
						: Boolean.TRUE.equals(jawaban.getDitolak()) ? "Ditolak" : "Menunggu Persetujuan";
				Date waktuStatus = Boolean.TRUE.equals(jawaban.getDitolak()) ? jawaban.getWaktuDitolak()
						: jawaban.getWaktuPersetujuan();
				if (hasil.length() > 0) {
					hasil.append("\n");
				}
				hasil.append(jabatan == null ? "" : jabatan);
				if (penerima != null && !penerima.isEmpty()) {
					hasil.append(jabatan == null || jabatan.isEmpty() ? "" : " - ").append(penerima);
				}
				hasil.append(" | ").append(statusJawaban);
				if (waktuStatus != null) {
					hasil.append(" | ").append(Common.dateFormat3.get().format(waktuStatus));
				}
				if (!catatan.isEmpty()) {
					hasil.append(" | ").append(catatan.replace('\n', ' '));
				}
			}
			return hasil.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit SuratMasuk.getJawabanPenerimaDisposisi");
			return "";
		}
	}

	/**
	 * Status keaslian berkas fisik surat, dengan bawaan {@code "Asli"}.
	 *
	 * <p>Getter destruktif ringan: {@code null} ditulis menjadi {@code "Asli"}. Nilainya teks
	 * bebas yang dipilih dari kotak pilihan pada formulir, bukan relasi ke tabel referensi,
	 * sehingga tidak ada yang menjamin isinya termasuk dalam daftar pilihan yang sah.</p>
	 *
	 * @return status keaslian berkas; {@code "Asli"} bila belum ditetapkan
	 */
	public String getStatus() {
		if (status == null) {
			status = "Asli";
		}
		return status;
	}

	/**
	 * Mengisi status keaslian berkas fisik surat.
	 *
	 * @param status status keaslian; {@code null} akan dipulihkan menjadi {@code "Asli"}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Sifat surat dalam bentuk teks, dengan bawaan {@code "Biasa"} dan penimpaan dari
	 * klasifikasi.
	 *
	 * <p>Dua penulisan terjadi di sini, keduanya menetap karena kelas dipetakan
	 * {@code dynamicUpdate = true}. Pertama, {@code null} dipulihkan menjadi {@code "Biasa"}.
	 * Kedua, bila klasifikasi surat memiliki entitas {@link SifatSurat}, NAMA entitas itu
	 * ditulis menimpa nilai yang ada. Penimpaan kedua inilah yang menentukan: sifat sebuah
	 * surat masuk tidak dapat menyimpang dari sifat yang ditetapkan klasifikasinya, dan
	 * mengubah sifat pada klasifikasi mengubah sifat seluruh surat lamanya secara surut.</p>
	 *
	 * <p>Ingat pula bahwa {@link KlasifikasiSuratMasuk#getSifatSurat()} sendiri melakukan
	 * pencarian berbasis NAMA terhadap cache entitas sifat, sehingga rantai penurunan sifat
	 * surat bertumpu pada kesamaan nama, bukan pada kunci asing.</p>
	 *
	 * @return sifat surat dalam bentuk teks; {@code "Biasa"} bila tidak ditetapkan di mana pun
	 */
	public String getSifat() {
		if (sifat == null) {
			sifat = "Biasa";
		}

		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getSifatSurat() != null) {
			sifat = getKlasifikasiSuratMasuk().getSifatSurat().getNama();
		}

		return sifat;
	}

	/**
	 * Mengisi sifat surat dalam bentuk teks.
	 *
	 * <p>Nilai ini tidak bertahan bila klasifikasi surat menetapkan sifatnya sendiri.</p>
	 *
	 * @param sifat sifat surat; {@code null} akan dipulihkan menjadi {@code "Biasa"}
	 */
	public void setSifat(String sifat) {
		this.sifat = sifat;
	}

	/**
	 * Tingkat kerahasiaan surat dalam bentuk teks, dengan bawaan {@code "Biasa"}.
	 *
	 * <p>Berbeda dari {@link #getSifat()}, nilai ini TIDAK diturunkan dari klasifikasi dan
	 * tetap milik surat sepenuhnya. Yang perlu diketahui: penanda ini murni keterangan yang
	 * dicetak dan ditampilkan -- tidak satu pun penyaring hak lihat surat masuk membacanya.
	 * Menandai sebuah surat sebagai rahasia karena itu TIDAK membatasi siapa yang dapat
	 * membukanya; pembatasan yang sesungguhnya hanya datang dari
	 * {@link KlasifikasiSuratMasuk#getKodeGrupPengguna()} dan dari kepemilikan konseptor.</p>
	 *
	 * <p>Getter destruktif ringan: {@code null} ditulis menjadi {@code "Biasa"}.</p>
	 *
	 * @return tingkat kerahasiaan; {@code "Biasa"} bila belum ditetapkan
	 */
	public String getKerahasiaan() {
		if (kerahasiaan == null) {
			kerahasiaan = "Biasa";
		}
		return kerahasiaan;
	}

	/**
	 * Mengisi tingkat kerahasiaan surat.
	 *
	 * @param kerahasiaan tingkat kerahasiaan; {@code null} akan dipulihkan menjadi
	 *                    {@code "Biasa"}
	 */
	public void setKerahasiaan(String kerahasiaan) {
		this.kerahasiaan = kerahasiaan;
	}

	/**
	 * Cakupan satuan kerja surat, dengan klasifikasi DIDAHULUKAN atas nilai milik surat sendiri.
	 *
	 * <p>Perhatikan urutan cabangnya, yang berbeda dari getter cakupan lain di kelas ini: bila
	 * klasifikasi memiliki satuan kerja, nilai klasifikasi langsung ditulis ke field DAN
	 * {@code check(...)} tidak dijalankan sama sekali; hanya bila klasifikasi tidak mengisi,
	 * proxy milik surat sendiri dibongkar. Susunan itu berarti satuan kerja surat tidak pernah
	 * dapat menyimpang dari klasifikasinya.</p>
	 *
	 * <p>Konsekuensi yang perlu diketahui: satuan kerja adalah dimensi pemisah antar unit di
	 * banyak modul AIS, dan di sini ia bukan atribut surat melainkan atribut klasifikasi.
	 * Memindahkan sebuah klasifikasi ke satuan kerja lain memindahkan pula seluruh surat
	 * lamanya secara surut, dan penulisan menetap di getter ini membuat perpindahan itu
	 * tercatat permanen pada setiap surat yang dibaca sesudahnya.</p>
	 *
	 * @return satuan kerja cakupan surat, atau {@code null} bila tidak ditetapkan di mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getSatuanKerja() != null) {
			satuanKerja = getKlasifikasiSuratMasuk().getSatuanKerja();
		} else {
			satuanKerja = check(satuanKerja);
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan cakupan satuan kerja surat. Nilai ini tidak bertahan bila klasifikasi surat
	 * menetapkan satuan kerjanya sendiri.
	 *
	 * @param satuanKerja satuan kerja cakupan; boleh {@code null}
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/**
	 * Cakupan sekolah surat, DITIMPA nilai milik klasifikasi bila klasifikasi mengisinya.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)} lebih dulu, baru
	 * ditimpa nilai klasifikasi -- urutan yang berlawanan dengan {@link #getSatuanKerja()},
	 * tanpa perbedaan hasil yang berarti.</p>
	 *
	 * @return sekolah cakupan surat, atau {@code null} bila tidak ditetapkan di mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);

		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getSekolah() != null) {
			sekolah = getKlasifikasiSuratMasuk().getSekolah();
		}

		return sekolah;
	}

	/**
	 * Menetapkan cakupan sekolah surat, MENOLAK objek yang belum tersimpan.
	 *
	 * <p>Objek {@link Sekolah} yang id-nya masih {@code null} diperlakukan sama dengan
	 * {@code null}. Penjagaan itu mencegah cascade {@code PERSIST} menyimpan diam-diam baris
	 * sekolah baru yang sebetulnya hanya wadah kosong dari kotak pilihan.</p>
	 *
	 * @param sekolah sekolah cakupan; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Cakupan yayasan surat, DITIMPA nilai milik klasifikasi bila klasifikasi mengisinya.
	 *
	 * @return yayasan cakupan surat, atau {@code null} bila tidak ditetapkan di mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);

		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getYayasan() != null) {
			yayasan = getKlasifikasiSuratMasuk().getYayasan();
		}

		return yayasan;
	}

	/**
	 * Menetapkan cakupan yayasan surat, MENOLAK objek yang belum tersimpan dengan alasan yang
	 * sama seperti {@link #setSekolah(Sekolah)}.
	 *
	 * @param yayasan yayasan cakupan; objek tanpa id diperlakukan sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Daftar id pengguna PENERIMA SUREL PEMBERITAHUAN, dinormalkan menjadi rangkaian token
	 * berpembatas koma.
	 *
	 * <h3>Ini BUKAN penyaring hak akses</h3>
	 *
	 * <p>Namanya mudah disalahpahami. Penelusuran seluruh pemanggil menunjukkan satu-satunya
	 * pembaca nilai ini adalah {@code BroadcastHelper}, yang memeriksa apakah token
	 * {@code ",<userId>,"} ada di dalamnya untuk memutuskan siapa yang menerima surel
	 * pemberitahuan surat masuk. TIDAK ADA satu pun kriteria Hibernate yang memakai kolom ini
	 * untuk membatasi surat masuk yang boleh dilihat seseorang. Penyaring hak lihat yang
	 * sesungguhnya berada di {@code DasboardSurat.createSuratMasukVisibilityCriterion(...)} dan
	 * bertumpu pada {@link #getKonseptor()} serta
	 * {@link KlasifikasiSuratMasuk#getKodeGrupPengguna()}. Menambahkan seseorang di sini
	 * memberinya pemberitahuan, bukan hak baca; dan menghapusnya dari sini tidak menutup akses
	 * yang sudah dimilikinya lewat jalur lain.</p>
	 *
	 * <h3>Bentuk normal dan alasannya</h3>
	 *
	 * <p>Isi kolom dibungkus koma di kedua ujung lalu deretan koma ganda diciutkan tiga kali,
	 * dan beberapa bentuk sisa yang hanya berisi koma dipulangkan menjadi teks kosong.
	 * Pembungkusan itu KEHARUSAN TEKNIS: pencocokan di {@code BroadcastHelper} memakai
	 * {@code contains("," + userId + ",")}, sehingga tanpa koma pembungkus, pengguna
	 * {@code "ad"} akan ikut cocok dengan entri {@code "admin"}. Penciutan bertingkat tiga kali
	 * hanya menangani sampai tujuh koma berurutan; masukan yang lebih kotor lolos dengan token
	 * kosong di tengah, yang tidak berbahaya karena tidak ada pengguna ber-id kosong.</p>
	 *
	 * <p>Getter ini destruktif -- bentuk normal ditulis kembali ke field dan karena itu
	 * tersimpan. Untuk kolom ini sifat tersebut memang diperlukan agar data lama ikut
	 * ternormalkan. Perhatikan pula bahwa pemeriksaan {@code null} pada baris terakhir tidak
	 * pernah lagi bernilai benar setelah penugasan di atasnya; cabang itu mati secara efektif.</p>
	 *
	 * @return daftar penerima pemberitahuan dalam bentuk {@code ",user1,user2,"}, atau teks
	 *         kosong; tidak pernah {@code null}
	 */
	@Column(name = "username_pengguna", nullable = true, columnDefinition = "text")
	public String getUsernamePengguna() {
		usernamePengguna = (usernamePengguna == null || usernamePengguna.trim().equalsIgnoreCase(",") ? ""
				: "," + usernamePengguna.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (usernamePengguna.equals(",")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,")) {
			usernamePengguna = "";
		} else if (usernamePengguna.equals(",,,,")) {
			usernamePengguna = "";
		}

		return usernamePengguna == null ? "" : usernamePengguna.trim();
	}

	/**
	 * Mengisi daftar id pengguna penerima surel pemberitahuan.
	 *
	 * <p>Nilai mentah apa pun boleh dikirim; {@link #getUsernamePengguna()} yang akan
	 * menormalkannya menjadi bentuk berkoma pada pembacaan berikutnya. Formulir surat masuk
	 * mengisinya dari sebuah kotak teks bebas, sehingga tidak ada yang menjamin id yang diketik
	 * benar-benar milik pengguna yang ada.</p>
	 *
	 * @param usernamePengguna daftar id pengguna, dipisah koma
	 */
	public void setUsernamePengguna(String usernamePengguna) {
		this.usernamePengguna = usernamePengguna;
	}

	/**
	 * Penanda surat ini disiarkan lewat surel kepada daftar penerima, dengan bawaan
	 * {@code false}.
	 *
	 * <p>Dibaca {@code BroadcastHelper} sebagai gerbang pertama sebelum daftar penerima pada
	 * {@link #getUsernamePengguna()} ditelusuri: bila penanda ini tidak menyala, tidak ada surel
	 * yang dikirim sekalipun daftar penerimanya terisi. Bawaan {@code false} hanya diterapkan
	 * pada nilai kembalian; field tidak ditulis, sehingga kolomnya tetap kosong di basis data.</p>
	 *
	 * <p>Perlu diketahui bahwa jalur penyiaran dari {@code SuratMasukAction} sendiri sedang
	 * dinonaktifkan -- pemeriksaan penanda ini di sana berada dalam baris yang dikomentari --
	 * sehingga penyiaran surat masuk kini hanya berjalan lewat jalur pembentangan alur
	 * disposisi.</p>
	 *
	 * @return {@code true} bila surat disiarkan lewat surel; tidak pernah {@code null}
	 */
	public Boolean getBroadcast() {
		return broadcast == null ? false : broadcast;
	}

	/**
	 * Menyalakan atau mematikan penyiaran surel untuk surat ini.
	 *
	 * @param broadcast {@code true} untuk menyiarkan; boleh {@code null}, dibaca sebagai
	 *                  {@code false}
	 */
	public void setBroadcast(Boolean broadcast) {
		this.broadcast = broadcast;
	}

	/**
	 * Surat keluar yang MELAHIRKAN surat masuk ini pada korespondensi antar unit.
	 *
	 * <p>Relasi ini tidak dipetakan LAZY dan dipertegas {@code @Fetch(FetchMode.SELECT)},
	 * sehingga Hibernate memuat objek nyata lewat SELECT terpisah -- itulah sebabnya getter ini
	 * tidak memanggil {@code check(...)} seperti getter relasi lain di kelas ini.</p>
	 *
	 * <p>Nilainya terisi hanya lewat satu jalur:
	 * {@code AlurPersetujuanSuratKeluarStatusAction} membuat surat masuk baru ketika sebuah
	 * surat keluar disetujui dan definisi alurnya menunjuk sebuah alur surat masuk. Jalur itu
	 * lebih dulu mencari surat masuk yang sudah tertaut ke surat keluar yang sama
	 * ({@code setMaxResults(1)}) agar tidak membuat duplikat, lalu menyalin fakultas, jurusan,
	 * satuan kerja, sekolah, dan yayasan dari surat keluar. Karena pencarian itu satu-satunya
	 * penjaga, tidak ada kendala unik di basis data yang mencegah dua surat masuk menunjuk satu
	 * surat keluar yang sama bila dua permintaan berjalan bersamaan.</p>
	 *
	 * <p>Keberadaan nilai di sini mengubah perilaku {@link #getNoSurat()}, yang akan menimpa
	 * nomor surat pengirim dengan kode surat keluar ini.</p>
	 *
	 * @return surat keluar asal, atau {@code null} untuk surat masuk dari luar instansi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_keluar", nullable = true)
	public SuratKeluar getSuratKeluar() {
		return suratKeluar;
	}

	/**
	 * Menautkan surat masuk ini ke surat keluar yang melahirkannya.
	 *
	 * @param suratKeluar surat keluar asal; boleh {@code null}
	 */
	public void setSuratKeluar(SuratKeluar suratKeluar) {
		this.suratKeluar = suratKeluar;
	}

	/**
	 * Nilai bawaan {@link #getJenisSurats()}, yaitu objek JSON kosong dalam bentuk teks.
	 *
	 * <p>PERHATIAN: field ini {@code public static} dan TIDAK {@code final}, sehingga secara
	 * teknis dapat diubah dari mana saja dan perubahan itu berlaku bagi seluruh instance di
	 * dalam JVM. Nilainya dibangun sekali saat kelas dimuat dari
	 * {@code new JSONObject().toString()}, bukan ditulis sebagai konstanta teks.</p>
	 */
	public static String DEFAULT_FORMULA = new JSONObject().toString();

	/**
	 * Himpunan id {@code Pejabat} tujuan disposisi dalam bentuk teks JSON, dengan bawaan objek
	 * kosong.
	 *
	 * <h3>Bentuk dan pengisian</h3>
	 *
	 * <p>Isinya objek JSON yang kuncinya adalah id pejabat dalam bentuk teks dan nilainya id
	 * yang sama dalam bentuk angka -- praktis sebuah himpunan yang diwujudkan sebagai peta.
	 * {@code SuratMasukAction.initJenisJabatan(...)} yang mengisinya: setiap kotak centang
	 * pejabat pada tab "Disposisi ke" menambahkan atau membuang satu kunci saat diklik.</p>
	 *
	 * <p>Menyimpan daftar tujuan disposisi sebagai JSON di dalam satu kolom teks, alih-alih
	 * sebagai tabel penghubung, berarti tidak ada kunci asing yang menjaganya: id pejabat yang
	 * kemudian dihapus tetap tertinggal di sini sebagai kunci yang tidak menunjuk apa pun, dan
	 * tidak ada kueri yang dapat menyaring surat berdasarkan pejabat tujuan tanpa membaca dan
	 * mengurai seluruh baris.</p>
	 *
	 * <h3>Hubungannya dengan catatan disposisi yang sesungguhnya</h3>
	 *
	 * <p>Kolom ini hanya menyimpan PILIHAN pada formulir. Catatan disposisi yang berumur
	 * panjang -- siapa menyetujui atau menolak, kapan, dengan catatan apa -- ada di
	 * baris-baris {@link AlurPersetujuanSuratMasukStatus}, dan itulah yang dibaca
	 * {@link #getJawabanPenerimaDisposisi()} serta yang dipakai formulir untuk mematikan kotak
	 * centang pejabat yang sudah didisposisi.</p>
	 *
	 * @return teks JSON himpunan id pejabat tujuan; {@code "{}"} bila belum ada yang dipilih,
	 *         tidak pernah {@code null}
	 */
	@Column(name = "jenis_surats", columnDefinition = "text")
	public String getJenisSurats() {
		return jenisSurats == null || jenisSurats.isEmpty() ? DEFAULT_FORMULA : jenisSurats;
	}

	/**
	 * Mengisi himpunan id pejabat tujuan disposisi dalam bentuk teks JSON.
	 *
	 * <p>Tidak ada validasi bentuk JSON di sini; teks yang tidak dapat diurai baru akan
	 * menimbulkan galat pada saat formulir merakit kotak centang.</p>
	 *
	 * @param jenisSurats teks JSON himpunan id pejabat; kosong dibaca sebagai objek kosong
	 */
	public void setJenisSurats(String jenisSurats) {
		this.jenisSurats = jenisSurats;
	}

	/**
	 * Simpul disposisi SOP yang mengikat surat ini, diwajibkan kelas induk
	 * {@code ais.database.model.sop.DataSop}.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Simpul inilah yang
	 * menentukan {@link #getAktif()}: surat dianggap berhenti berjalan bila simpul disposisinya
	 * tidak aktif, atau bila simpul akhir alurnya menandai adanya penolakan.</p>
	 *
	 * @return simpul disposisi SOP, atau {@code null} bila surat tidak berjalan lewat SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan simpul disposisi SOP surat ini, MENOLAK penghapusan tautan.
	 *
	 * <p>Setter ini berpenjagaan ketat sekaligus berlebihan. Baris pertama memulangkan lebih
	 * awal bila argumen {@code null} atau belum bersimpan, sehingga tautan yang sudah ada TIDAK
	 * DAPAT dilepas lewat setter ini -- perilaku yang disengaja agar jejak alur SOP tidak
	 * hilang karena pemuatan ulang formulir yang belum memilih simpul. Baris kedua berupa
	 * ternary yang memeriksa ulang syarat yang sama; karena pemulangan awal di atasnya sudah
	 * menyingkirkan kedua kemungkinan itu, cabang penjaga pada ternary tersebut TIDAK PERNAH
	 * dipilih dan efektifnya hanya menugaskan argumen apa adanya. Cabang mati itu tidak
	 * berbahaya, tetapi menyesatkan saat dibaca sepintas.</p>
	 *
	 * @param disposisiSop simpul disposisi SOP; diabaikan bila {@code null} atau belum
	 *                     bersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Pengguna yang meregistrasi surat masuk ini.
	 *
	 * <p>Relasi LAZY yang proxy-nya dibongkar lewat {@code check(...)}. Berbeda dari
	 * {@link #getUsernamePengguna()} yang hanya menentukan penerima pemberitahuan, kolom INI
	 * benar-benar dipakai sebagai penyaring hak lihat: {@code DasboardSurat} menyusun kriteria
	 * "{@code konseptor} adalah pengguna saat ini ATAU klasifikasi surat memuat role pengguna
	 * saat ini" bagi pengguna yang role-nya tidak memiliki {@code melihatSemuaSurat}.</p>
	 *
	 * <p>Konsekuensinya, surat yang konseptornya kosong -- misalnya surat yang lahir otomatis
	 * dari persetujuan surat keluar, yang jalurnya tidak mengisi konseptor -- hanya terlihat
	 * lewat cabang klasifikasi, dan menjadi tak terlihat sama sekali bila klasifikasinya juga
	 * tidak membatasi role. Nama pada {@code konseptor} juga ikut tercetak pada kode QR yang
	 * dibangkitkan {@link #ttdQr()}.</p>
	 *
	 * @return pengguna yang meregistrasi surat, atau {@code null} bila tidak tercatat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "konseptor", nullable = true)
	public Tbmuser getKonseptor() {
		konseptor = check(konseptor);
		return konseptor;
	}

	/**
	 * Menetapkan pengguna yang meregistrasi surat ini.
	 *
	 * @param konseptor pengguna peregistrasi; boleh {@code null}
	 */
	public void setKonseptor(Tbmuser konseptor) {
		this.konseptor = konseptor;
	}

	/**
	 * Stempel waktu dokumen, dengan cadangan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Berbeda dari getter bernilai bawaan lain di kelas ini, metode ini TIDAK destruktif:
	 * cadangan hanya dikembalikan, tidak ditulis ke field, sehingga kolom {@code waktu} tetap
	 * kosong dan tautan "ikut waktu perubahan terakhir" tetap hidup. Akibat yang perlu
	 * disadari: untuk surat yang kolom {@code waktu}-nya tidak pernah diisi, nilai yang
	 * dikembalikan BERGESER setiap kali baris diperbarui -- termasuk oleh pembaruan yang
	 * dipicu getter-getter destruktif di kelas ini, bukan oleh perbuatan pengguna. Nilai ini
	 * ikut tercetak pada kode QR yang dibangkitkan {@link #ttdQr()}.</p>
	 *
	 * @return stempel waktu dokumen; waktu perubahan terakhir bila belum pernah diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? getTanggal_dirubah() : waktu;
	}

	/**
	 * Mengisi stempel waktu dokumen.
	 *
	 * @param waktu stempel waktu dokumen; {@code null} membuat pembacaan jatuh ke waktu
	 *              perubahan terakhir
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Catatan revisi surat -- yang secara fisik adalah NILAI YANG SAMA dengan
	 * {@link #getCatatanDisposisi()}.
	 *
	 * <h3>Satu field Java menopang dua kolom basis data</h3>
	 *
	 * <p>Getter ini mengembalikan field {@code catatanDisposisi}, dan
	 * {@link #setCatatanRevisi(String)} menulis ke field yang sama -- padahal pasangan metode
	 * ini dipetakan ke kolom {@code catatan_revisi}, sedangkan
	 * {@link #getCatatanDisposisi()}/{@link #setCatatanDisposisi(String)} dipetakan ke kolom
	 * {@code catatan_disposisi}. Karena Hibernate memakai akses properti, akibatnya berlipat
	 * dua:</p>
	 * <ul>
	 *   <li>saat MENYIMPAN, kedua kolom selalu menerima nilai yang sama persis, sehingga
	 *       {@code catatan_revisi} tidak pernah dapat berisi sesuatu yang berbeda dari
	 *       {@code catatan_disposisi};</li>
	 *   <li>saat MEMUAT, Hibernate memanggil kedua setter dan keduanya menulis ke satu field.
	 *       Untuk baris warisan yang kedua kolomnya terlanjur BERBEDA, nilai yang akhirnya
	 *       terbaca adalah nilai kolom yang setter-nya dipanggil belakangan -- dan urutan itu
	 *       tidak dijamin oleh kontrak JPA mana pun.</li>
	 * </ul>
	 *
	 * <p>Pasangan metode ini karena itu bukan atribut kedua yang berdiri sendiri, melainkan
	 * nama alias bagi atribut yang sama, dengan biaya satu kolom tambahan yang isinya
	 * berulang. Menambahkan makna baru pada salah satu dari keduanya menuntut memisahkan
	 * field-nya lebih dulu; sampai itu dikerjakan, keduanya harus diperlakukan sebagai satu
	 * nilai.</p>
	 *
	 * @return catatan revisi, yang identik dengan instruksi disposisi pimpinan
	 */
	@Column(name = "catatan_revisi", columnDefinition = "text", nullable = true)
	public String getCatatanRevisi() {
		return catatanDisposisi;
	}

	/**
	 * Mengisi catatan revisi, yang menulis ke field yang sama dengan
	 * {@link #setCatatanDisposisi(String)}.
	 *
	 * @param catatanRevisi catatan revisi; boleh {@code null}
	 */
	public void setCatatanRevisi(String catatanRevisi) {
		this.catatanDisposisi = catatanRevisi;
	}

	/**
	 * Simpul alur tempat surat ini DITOLAK, bila pernah ditolak.
	 *
	 * <p>Relasi tidak LAZY dan dipertegas {@code @Fetch(FetchMode.SELECT)}, sehingga getter ini
	 * tidak perlu membongkar proxy. Nilainya berupa penunjuk pintas ke satu baris
	 * {@link AlurPersetujuanSuratMasukStatus} agar tampilan tidak perlu menelusuri seluruh
	 * rantai alur hanya untuk mengetahui di mana surat berhenti.</p>
	 *
	 * <p>Karena ini penunjuk TUNGGAL, ia hanya dapat menyimpan satu penolakan; bila surat
	 * ditolak, direvisi, lalu ditolak lagi, nilai lama tergantikan. Riwayat penolakan yang
	 * lengkap tetap ada pada baris-baris {@link AlurPersetujuanSuratMasukStatus} beserta
	 * stempel waktu penolakan masing-masing.</p>
	 *
	 * @return simpul alur tempat surat ditolak, atau {@code null} bila belum pernah ditolak
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "alur_ditolak", nullable = true)
	public AlurPersetujuanSuratMasukStatus getAlurDitolak() {
		return alurDitolak;
	}

	/**
	 * Menetapkan simpul alur tempat surat ini ditolak.
	 *
	 * @param alurDitolak simpul alur penolakan; boleh {@code null}
	 */
	public void setAlurDitolak(AlurPersetujuanSuratMasukStatus alurDitolak) {
		this.alurDitolak = alurDitolak;
	}

	/**
	 * Membangkitkan (atau memakai ulang) berkas gambar kode QR berisi ringkasan identitas surat,
	 * lalu memulangkan lokasi berkasnya di cakram.
	 *
	 * <h3>Cara kerja</h3>
	 *
	 * <p>Berkas disimpan di direktori laporan dengan nama tetap
	 * {@code s_m_<id surat>.png}. Bila berkas dengan nama itu SUDAH ADA, metode ini langsung
	 * memulangkan lokasinya tanpa membangkitkan apa pun; hanya bila belum ada, isi kode dirakit
	 * lalu diserahkan ke {@code BarcodeCommon.generateCRCode(...)}. Isi kode berupa beberapa
	 * baris teks: nomor agenda, perihal, waktu dokumen, nama dokumen, nama klasifikasi, nama
	 * konseptor, jurusan, fakultas, sekolah, dan alamat host aplikasi.</p>
	 *
	 * <h3>Dua hal yang perlu diketahui sebelum mengandalkan hasilnya</h3>
	 *
	 * <p>Pertama, penyimpanan berkas bersifat cache permanen berbasis id, bukan berbasis isi.
	 * Surat yang perihalnya, klasifikasinya, atau konseptornya diubah setelah kode QR-nya
	 * pernah dibangkitkan akan tetap memakai gambar LAMA selamanya, karena berkasnya sudah ada
	 * dan tidak pernah dihapus atau diberi penanda kedaluwarsa. Kode QR karena itu merekam
	 * keadaan surat pada saat pencetakan pertama, bukan keadaan terkininya.</p>
	 *
	 * <p>Kedua, baris kedua perakitan isi kode berbunyi
	 * {@code (getNoSurat() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n")}:
	 * syaratnya memeriksa {@link #getNoSurat()}, tetapi yang dicetak adalah {@link #getKode()}.
	 * Akibatnya nomor agenda tercetak DUA KALI berturut-turut, sementara nomor surat pengirim
	 * -- yang jelas dimaksudkan untuk baris itu -- tidak pernah masuk ke dalam kode QR sama
	 * sekali. Bentuknya khas kekeliruan salin-tempel dari baris pertama di atasnya. Dampaknya
	 * terbatas pada isi kode QR; tidak ada logika lain yang membaca kembali isi tersebut.</p>
	 *
	 * <p>Metode ini melakukan operasi berkas dan pemuatan beberapa relasi, jadi tidak murah;
	 * pemanggilnya adalah jalur pencetakan lembar disposisi, bukan perakitan daftar.</p>
	 *
	 * @return lokasi absolut berkas gambar kode QR di cakram
	 */
	public String ttdQr() {

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/s_m_" + getId() + ".png");
		if (!myfilebarcode.exists()) {
			String code = (getKode() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n")
					+ (getNoSurat() == null || getKode().trim().isEmpty() ? "" : getKode() + "\n")
					+ (getPerihal() == null || getPerihal().trim().isEmpty() ? "" : getPerihal() + "\n")
					+ (getWaktu() == null ? "" : Common.dateFormat3.get().format(getWaktu()) + "\n") + getNama() + "\n"
					+ (getKlasifikasiSuratMasuk() == null ? "" : getKlasifikasiSuratMasuk().getNama() + "\n")

					+ (getKonseptor() == null ? "" : getKonseptor().getUserNama() + "\n")

					+ (getJurusan() == null ? "" : getJurusan().getNama() + "\n")
					+ (getFakultas() == null ? "" : getFakultas().getNama() + "\n")
					+ (getSekolah() == null ? "" : getSekolah().getNama() + "\n") + Common.getRequestHostWithProtocol();
			BarcodeCommon.generateCRCode(code, myfilebarcode);
		}
		return myfilebarcode.getAbsolutePath();
	}

	/**
	 * Penanda surat masih BERJALAN, DITURUNKAN dari status disposisi SOP-nya.
	 *
	 * <h3>Aturan penurunan</h3>
	 *
	 * <p>Nilai flag disimpulkan, bukan sekadar dibaca. Dua keadaan menjadikannya
	 * {@code false}: (1) simpul {@link #getDisposisiSop()} sendiri sudah tidak aktif; atau
	 * (2) simpul akhir alur SOP menandai bahwa penolakan berada di titik itu
	 * ({@code getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}). Bila tidak satu pun
	 * berlaku, nilai field dipakai apa adanya, dengan {@code null} dibaca sebagai
	 * {@code true}.</p>
	 *
	 * <h3>Sifat destruktif dan arah satu jalan</h3>
	 *
	 * <p>Metode ini MENULIS ke field {@code aktif} setiap kali salah satu keadaan di atas
	 * terpenuhi, dan penulisan itu tersimpan karena kelas dipetakan {@code dynamicUpdate}. Ia
	 * juga menugaskan ulang field {@code disposisiSop} dari hasil {@link #getDisposisiSop()}.
	 * Yang perlu diketahui: penurunan ini SATU ARAH. Tidak ada cabang yang mengembalikan flag
	 * menjadi {@code true}, sehingga surat yang pernah dibaca dalam keadaan tidak aktif akan
	 * tetap tersimpan tidak aktif meskipun simpul SOP-nya kemudian diaktifkan kembali atau
	 * penolakannya dianulir. Mengaktifkan kembali surat semacam itu menuntut
	 * {@link #setAktif(Boolean)} dipanggil secara sengaja.</p>
	 *
	 * <p>Rantai pembacaan pada cabang kedua menelusuri empat tingkat objek berturut-turut,
	 * masing-masing dijaga pemeriksaan {@code null}, sehingga alur SOP yang belum lengkap tidak
	 * menimbulkan galat -- surat semacam itu sekadar dianggap masih berjalan.</p>
	 *
	 * @return {@code true} bila surat masih berjalan; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan penanda surat masih berjalan secara langsung.
	 *
	 * <p>Ini satu-satunya jalan mengembalikan surat yang terlanjur ditandai tidak aktif oleh
	 * {@link #getAktif()}, karena metode itu tidak pernah menaikkan flag kembali.</p>
	 *
	 * @param aktif {@code true} bila surat masih berjalan; boleh {@code null}, dibaca sebagai
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Sifat surat sebagai entitas, dengan klasifikasi DIDAHULUKAN atas nilai milik surat sendiri.
	 *
	 * <p>Susunan cabangnya sama seperti {@link #getSatuanKerja()}: bila klasifikasi memiliki
	 * entitas sifat, nilai klasifikasi ditulis ke field dan {@code check(...)} dilewati; hanya
	 * bila tidak, proxy milik surat sendiri dibongkar. Berpasangan dengan {@link #getSifat()},
	 * yang menurunkan bentuk TEKS dari sumber yang sama.</p>
	 *
	 * <p>Ingat bahwa {@link KlasifikasiSuratMasuk#getSifatSurat()} sendiri dapat memulangkan
	 * entitas hasil pencarian berbasis NAMA terhadap cache, bukan hasil kunci asing, sehingga
	 * seluruh rantai penurunan sifat surat bertumpu pada kesamaan nama.</p>
	 *
	 * @return entitas sifat surat, atau {@code null} bila tidak ditetapkan di mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sifat_surat", nullable = true)
	public SifatSurat getSifatSurat() {
		if (getKlasifikasiSuratMasuk() != null && getKlasifikasiSuratMasuk().getSifatSurat() != null) {
			sifatSurat = getKlasifikasiSuratMasuk().getSifatSurat();
		} else {
			sifatSurat = check(sifatSurat);
		}
		return sifatSurat;
	}

	/**
	 * Menetapkan entitas sifat surat. Nilai ini tidak bertahan bila klasifikasi surat
	 * menetapkan sifatnya sendiri.
	 *
	 * @param sifatSurat entitas sifat surat; boleh {@code null}
	 */
	public void setSifatSurat(SifatSurat sifatSurat) {
		this.sifatSurat = sifatSurat;
	}

	/**
	 * Pengguna yang MEMBEKUKAN surat ini; keberadaannya menandai surat terkunci.
	 *
	 * <p>Pasangan {@code getDikunci()}/{@code setDikunci()} diwajibkan kelas induk
	 * {@link ais.database.model.VoKunci}, dan pada surat masuk penguncian benar-benar
	 * berpengaruh pada formulir: seluruh kotak centang tujuan disposisi dimatikan, dan editor
	 * ringkasan digantikan tampilan HTML statis sehingga isinya tidak lagi dapat disunting.</p>
	 *
	 * <p>Perlu diketahui bahwa penguncian ini bekerja pada lapisan PERAKITAN ANTARMUKA, bukan
	 * pada entitas: seluruh setter di kelas ini tetap menerima nilai baru pada surat yang
	 * terkunci, dan tidak ada pemeriksaan kunci di dalam model. Jalur mana pun yang tidak
	 * melewati formulir surat masuk -- impor, CRUD generik, maupun jalur otomatis dari
	 * persetujuan surat keluar -- karena itu tidak terhalang oleh kunci ini.</p>
	 *
	 * <p>Kolom {@code dikunci} adalah satu-satunya {@code @JoinColumn} di kelas ini yang tidak
	 * menyebut {@code nullable}; bawaan JPA untuk atribut itu adalah dapat kosong, yang memang
	 * diperlukan karena surat yang tidak terkunci meninggalkannya {@code null}.</p>
	 *
	 * @return pengguna yang membekukan surat, atau {@code null} bila surat tidak terkunci
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dikunci")
	public Tbmuser getDikunci() {
		dikunci = check(dikunci);
		return dikunci;
	}

	/**
	 * Menetapkan pengguna yang membekukan surat ini; mengisinya dengan {@code null} membuka
	 * kembali kunci.
	 *
	 * <p>Tidak ada pemeriksaan wewenang di sini -- siapa yang boleh mengunci dan membuka kunci
	 * ditentukan sepenuhnya oleh lapisan pemanggil.</p>
	 *
	 * @param dikunci pengguna yang membekukan surat; {@code null} untuk membuka kunci
	 */
	public void setDikunci(Tbmuser dikunci) {
		this.dikunci = dikunci;
	}

	/**
	 * Penanda ragam dokumen, sejajar dengan {@link KlasifikasiSuratMasuk#getTipe()}.
	 *
	 * <p>Nilainya tidak dinormalkan dan dapat {@code null} untuk baris lama. Sama seperti
	 * padanannya pada klasifikasi, penanda ini berperan sebagai pengelompokan tampilan --
	 * memisahkan beberapa daftar dokumen yang berbagi satu tabel -- dan TIDAK dipakai satu pun
	 * penyaring hak lihat, sehingga bukan kendali akses.</p>
	 *
	 * @return penanda ragam dokumen, atau {@code null} untuk baris yang belum ditandai
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Mengisi penanda ragam dokumen.
	 *
	 * @param tipe penanda ragam dokumen; boleh {@code null}
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

}
