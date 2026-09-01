package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.DataUtil;
import ais.common.EntityIdentityMap;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Kelas dasar SELURUH entity Hibernate dan value object model AIS. Tipe ini menyatukan identitas,
 * metadata umum, representasi, serta perilaku lintas entity yang benar-benar berlaku bagi
 * turunannya.
 *
 * <h2>Skala dan kedudukan</h2>
 * <p>Per pengukuran 2 Sep 2026 terdapat <b>1.456 file</b> di pohon sumber ini yang menuliskan
 * {@code extends GeneralValueObject}, baik langsung maupun lewat kelas antara (mis.
 * {@code DataSop} &rarr; {@link VoKunci}). Yang mewarisi bukan hanya entity master/transaksi
 * inti — {@code Mahasiswa}, {@code Dosen}, {@code Perkuliahan}, {@code Tagihan}, {@code Siswa},
 * {@code Guru}, {@link Tbmuser}, {@link Tbmrole} — tetapi juga sejumlah value object non-tabel
 * dan beberapa helper yang memanfaatkan utilitas cache di sini. Konsekuensinya: kelas ini adalah
 * <b>infrastruktur</b>, bukan model biasa. Satu perubahan perilaku di sini merambat ke hampir
 * seluruh aplikasi, dan method statis {@link #check(Object)} di bawah dipanggil dari <b>ribuan</b>
 * titik (lebih dari 800 file di {@code ais.database.model} saja memuat pemanggilan
 * {@code check(...)} di dalam getter relasinya). Perlakukan setiap suntingan sebagai perubahan
 * lintas-modul.</p>
 *
 * <h2>Kontrak untuk subclass</h2>
 * <p>Yang <b>WAJIB</b> dipenuhi subclass hanyalah satu: mengimplementasikan
 * {@link #onUpdate()} (satu-satunya method {@code abstract} di kelas ini). Selebihnya subclass
 * mendapat "gratis":</p>
 * <ul>
 *   <li>Field + accessor generik yang dipakai lintas modul: {@code id}, {@code kode}, {@code nama},
 *   {@code nim}, {@code keterangan}, {@code nomorUrut}, {@code oleh}, {@code olehId},
 *   {@code tanggal_dirubah}, serta {@code copyDari} (referensi object asal saat operasi salin).
 *   Field-field ini <b>tidak</b> otomatis menjadi kolom database: pemetaan kolom ditentukan oleh
 *   anotasi/berkas mapping pada masing-masing subclass. Subclass yang tidak memetakannya tetap
 *   dapat memakainya sebagai state in-memory (mis. {@code nama} untuk keperluan
 *   {@link #toString()} dan {@link #compareTo(GeneralValueObject)}).</li>
 *   <li>Identitas: {@link #equals(Object)} berbasis {@code id}, {@link #compareTo(GeneralValueObject)}
 *   berjenjang, {@link #toString()} berformat {@code "kode - nama"}, dan {@link #clone()} dangkal.</li>
 *   <li>Resolusi proxy lazy: {@link #check(Object)} beserta aliasnya {@link #chek(Object)} dan
 *   {@link #resolveLazy(Object)}.</li>
 *   <li>Cache JSON/berkas sementara per-object: {@link #read()}, {@link #write(String...)},
 *   {@link #delete()}, {@link #put(String)}, {@link #retreive()}, {@link #udah()}, {@link #belum()},
 *   {@link #putBaru(String, String)}, {@link #tulisPutBaru(String)}, {@link #retreiveAll(String)}.</li>
 *   <li>Jejak aktivitas per-pengguna: {@link #masukkanData(String)},
 *   {@link #ambilData(String, String)}, {@link #apakahSedang(String)}.</li>
 *   <li>Penyimpanan sisi-berkas untuk penilaian umum dan angket:
 *   {@code *ChecklistHasilPenilaianUmum*} dan {@code *IsiAngketParameterUmum*}.</li>
 * </ul>
 *
 * <p><b>Peringatan penting soal identitas:</b> {@link #equals(Object)} di-override berbasis
 * {@code id}, tetapi {@code hashCode()} <b>TIDAK</b> di-override — baik di sini maupun di
 * {@link DataUtil} — sehingga tetap memakai identitas object bawaan {@code Object}. Akibatnya dua
 * instance berbeda dengan {@code id} sama akan {@code equals()} tetapi punya hash berbeda, jadi
 * <b>jangan</b> mengandalkan {@code HashSet}/{@code HashMap} berkunci entity untuk deduplikasi.
 * Pola aman yang dipakai di kelas ini sendiri adalah memakai {@code Map<Long, Entity>} berkunci
 * {@code id} (lihat {@link #ambilChecklistHasilPenilaianUmum(Session, Long, String, boolean)} dan
 * {@link #ambilIsiAngketParameterUmum(Session, boolean)}). Untuk menjamin satu instance Java per
 * ID di seluruh JVM gunakan {@link EntityIdentityMap}, bukan koleksi berbasis hash.</p>
 *
 * <h2>Mekanisme {@code check()} / {@code chek()} / {@code resolveLazy()}</h2>
 * <p>Ini bagian paling kritis di kelas ini. Masalah yang diselesaikan: AIS memetakan relasi
 * {@code @ManyToOne} sebagai {@code FetchType.LAZY}, sementara object entity sering hidup lebih
 * lama daripada Hibernate {@link Session} yang memuatnya (disimpan di cache in-memory/MapDB,
 * dibawa lintas request ZK, diserialkan, dsb.). Getter relasi yang menyentuh proxy lazy yang sudah
 * <i>detached</i> akan melempar {@code LazyInitializationException}. Karena itu pola getter standar
 * di seluruh entity AIS adalah:</p>
 * <pre>{@code
 * public Jurusan getJurusan() {
 *     jurusan = check(jurusan);   // resolusi proxy lazy sebelum dikembalikan
 *     return this.jurusan;
 * }
 * }</pre>
 * <p>{@link #check(Object)} mencoba empat sumber secara berurutan, dan berhenti pada yang pertama
 * berhasil:</p>
 * <ol start="0">
 *   <li><b>{@link EntityIdentityMap}</b> — identity map JVM-wide "satu Java object per
 *   kelas+ID". Bila instance kanonik untuk ID tersebut sudah terdaftar, instance itu langsung
 *   dikembalikan tanpa lazy-init maupun query. Ini yang menjamin perubahan field skalar (mis.
 *   {@code NominalBiaya.bukanTagihan = true}) langsung terlihat oleh SEMUA pemegang referensi ke
 *   entity ber-ID sama, termasuk salinan hasil deserialisasi MapDB.</li>
 *   <li><b>Cache {@link ConstantValues}</b> — {@link #ambilDariCacheJikaAman(String, Serializable)}
 *   mengambil dari cache in-memory tanpa fallback ke database ({@code diambilJikaTidakAda=false}),
 *   dan hanya menerima hasilnya bila object itu bisa diinisialisasi dengan aman.</li>
 *   <li><b>Inisialisasi dengan session yang tersedia</b> — {@code Hibernate.initialize(...)}. Jalur
 *   ini berhasil bila object masih <i>attached</i> pada session aktif; bila sudah detached ia gagal
 *   diam-diam (exception ditelan, mengembalikan {@code false}) dan alur lanjut ke tahap berikut.</li>
 *   <li><b>Reload lewat session baru</b> — {@link #reloadDetachedObject(String, Serializable)}
 *   membuka {@code SessionFactory.openSession()} khusus, {@code session.get(kelas, id)}, lalu
 *   menutup session itu di {@code finally}. Ini "penyelamat terakhir" agar pemanggil menerima
 *   entity asli, bukan proxy detached yang meledak.</li>
 * </ol>
 * <p>Bila keempatnya gagal, {@code check()} mengembalikan <b>argumen apa adanya</b>: method ini
 * dirancang tidak pernah melempar exception, karena kegagalannya tidak boleh membuat getter entity
 * ikut gagal. Konsekuensinya kegagalan resolusi bersifat senyap — bila sebuah getter relasi
 * mengembalikan proxy yang tetap meledak di pemanggil, curigai tahap 3 (mis. nama kelas hasil
 * de-proxy tidak bisa di-{@code Class.forName}, atau identifier tidak terbaca).</p>
 * <p>{@link #chek(Object)} dan {@link #resolveLazy(Object)} hanyalah alias tipis yang meneruskan ke
 * {@link #check(Object)}; {@code chek} adalah ejaan historis yang masih dipakai sebagian kode lama,
 * {@code resolveLazy} adalah nama deskriptif untuk kode baru. Tidak ada perbedaan perilaku.</p>
 * <p><b>Biaya:</b> {@code check()} murah pada kasus umum (flag {@code initData} + cache), tetapi
 * tahap 3 membuka koneksi database baru. Jangan memanggilnya dalam loop besar atas object yang
 * sudah pasti detached tanpa lebih dulu memuat ulang koleksinya lewat satu query.</p>
 *
 * <h2>Cache JSON/berkas sementara per-object</h2>
 * <p>Sejumlah besar method di kelas ini ({@code read/write/delete}, {@code put/retreive},
 * {@code udah/belum}, {@code putBaru/tulisPutBaru/retreiveBaru/bersihkanPutBaru},
 * {@code retreiveAll}, serta kelompok {@code *ChecklistHasilPenilaianUmum*} dan
 * {@code *IsiAngketParameterUmum*}) TIDAK menyentuh tabel database. Semuanya membaca/menulis
 * berkas JSON sementara di direktori temp yang ditentukan
 * {@code ConstantValues.ambilLokasiFileTemprorary(...)} dan {@code Common.getFileLocation(...)},
 * dengan nama berkas diturunkan dari nama kelas + ID entity. Artinya:</p>
 * <ul>
 *   <li>{@link #delete()} <b>menghapus berkas cache</b>, bukan menghapus baris database.</li>
 *   <li>Isi cache boleh hilang kapan saja (temp dibersihkan, deploy ulang, disk penuh); kode
 *   pemanggil harus tetap benar ketika cache kosong. Kolom database tetap menjadi sumber
 *   kebenaran/fallback.</li>
 *   <li>Untuk {@link Tbmuser} dan {@link Tbmrole} kunci berkas memakai {@code userId}/{@code roleId}
 *   (String), bukan {@code getId()} — pola percabangan {@code instanceof} ini berulang di hampir
 *   semua method cache di kelas ini.</li>
 * </ul>
 *
 * <h2>Thread-safety</h2>
 * <p>Map akumulator {@code datatemporary} dipakai bersama oleh SELURUH thread/request dalam satu
 * JVM (bukan per-request) — memang disengaja, sebagai penampung batch {@link #putBaru(String, String)}
 * sebelum sekali {@link #tulisPutBaru(String)} di akhir proses sinkronisasi. Karena itu ia
 * memakai {@code ConcurrentHashMap} dan setiap rangkaian baca-ubah-tulis dikunci dengan
 * {@code synchronized (key.intern())} per kunci entity. Riwayat lengkap bug yang memaksa desain ini
 * ({@code ConcurrentModificationException} dan berkas JSON terpotong "Unterminated string")
 * didokumentasikan pada Javadoc {@link #putBaru(String, String)}. Terpisah dari itu,
 * {@link #retreive(String)} punya cache per-instance sendiri yang diinisialisasi lewat
 * double-checked locking di {@link #retreiveCache()}.</p>
 *
 * <h2>Penjagaan saat startup</h2>
 * <p>{@link #put(String, String)} dan {@link #retreive(String)} memeriksa
 * {@code AppStartupListener.isStartupInProgress()} dan melewati I/O berkas selama bootstrap.
 * Sebabnya nyata: Hibernate memanggil setter ter-map saat hidrasi entity
 * ({@code TwoPhaseLoad.initializeEntity}), sehingga memuat ribuan entity saat init berarti ribuan
 * operasi tulis/baca berkas dan membuat startup macet di thread {@code "main"} (terbukti lewat
 * thread dump). Setelah startup selesai, perilaku normal kembali.</p>
 *
 * <h2>Batas tanggung jawab</h2>
 * <p>Perilaku umum, validasi, akses data ter-cache, serta lifecycle audit tetap dimiliki
 * {@link DataUtil} (kelas induk langsung; berisi antara lain {@code ambilData}, {@code masukkanData},
 * {@code ubahDataHistory}, {@code deepRestoreDataDanRelasi}). Kelas ini hanya boleh memuat hal yang
 * benar-benar berlaku bagi seluruh entity; perubahan yang berlaku bagi keluarga model harus
 * ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi di kelas ini dapat membaca/mengubah
 * persistence, membuka session Hibernate sendiri, menulis/menghapus berkas di direktori temp, dan
 * bahkan membentuk komponen UI ZK ({@link #tampilKunci(Component, VoKunci, Tbmuser, EventListener)}).
 * Jangan menganggap model ini selalu murni; panggil operasi tersebut melalui alur service dengan
 * session, transaksi, dan otorisasi yang sesuai agar perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see DataUtil
 * @see EntityIdentityMap
 * @see ConstantValues#ambil(String, Serializable, boolean)
 */
public abstract class GeneralValueObject extends DataUtil
		implements Serializable, Cloneable, Comparable<GeneralValueObject> {

	/**
	 * Versi serialisasi bersama seluruh entity turunan.
	 *
	 * <p>Nilainya tetap sejak lama dan <b>jangan diubah</b>: object entity AIS diserialkan ke
	 * cache MapDB dan session ZK, sehingga mengubah nilai ini membuat data ter-cache lama tidak
	 * bisa dibaca lagi ({@code InvalidClassException}) setelah deploy.</p>
	 */
	private static final long serialVersionUID = -3124378662823718355L;

	/**
	 * Mengembalikan object ini apa adanya sebagai payload JSON untuk endpoint JAX-RS.
	 *
	 * <p>Anotasi {@code @GET}/{@code @Produces(APPLICATION_JSON)} membuat method ini bisa
	 * dijadikan resource REST oleh subclass yang memang dipublikasikan sebagai web service;
	 * serialisasi ke JSON dikerjakan provider JAX-RS, bukan oleh method ini.</p>
	 *
	 * <p><b>Hati-hati:</b> object yang dikembalikan adalah entity Hibernate utuh. Bila masih ada
	 * relasi lazy yang belum diresolusi, serialisasi bisa memicu pemuatan berantai (atau gagal
	 * bila session sudah tertutup). Sediakan projection/VO khusus bila payload harus dibatasi.</p>
	 *
	 * @return object ini sendiri ({@code this})
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON })
	public GeneralValueObject getJson() {
		return this;
	}

	/**
	 * Membuat salinan <b>dangkal</b> (shallow copy) object ini lewat {@link Object#clone()}.
	 *
	 * <p>Karena dangkal, seluruh field referensi (termasuk relasi ke entity lain dan koleksi)
	 * dibagi pakai dengan object asal — mengubah isi koleksi pada salinan juga mengubah koleksi
	 * pada aslinya. Field {@code id} ikut tersalin, jadi salinan akan {@code equals()} dengan
	 * aslinya; setel ulang {@code id} menjadi {@code null} bila salinan dimaksudkan sebagai baris
	 * baru, dan pertimbangkan mengisi {@link #setCopyDari(GeneralValueObject)} agar asal-usul
	 * salinan tetap terlacak.</p>
	 *
	 * <p>{@code CloneNotSupportedException} tidak dilempar ke pemanggil; ia hanya dilaporkan lewat
	 * {@code Common.tampilErrorJikaAdmin(e)} dan method mengembalikan {@code null}. Kondisi ini
	 * praktis tidak terjadi selama kelas ini mengimplementasikan {@link Cloneable}.</p>
	 *
	 * @return salinan dangkal object ini, atau {@code null} bila kloning gagal
	 */
	public GeneralValueObject clone() {
		GeneralValueObject generalValueObject = null;
		try {
			generalValueObject = (GeneralValueObject) super.clone();
		} catch (CloneNotSupportedException e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return generalValueObject;
	}

	/**
	 * Satu-satunya kontrak yang WAJIB diimplementasikan oleh 1.456 subclass.
	 *
	 * <p>Hook yang dipanggil pada jalur pembaruan entity untuk merapikan/menurunkan nilai
	 * turunan sebelum data disimpan — misalnya menormalkan kode, menghitung ulang field ringkasan,
	 * atau menyinkronkan berkas cache milik entity. Isi konkretnya sepenuhnya milik subclass;
	 * banyak subclass sederhana membiarkannya kosong.</p>
	 *
	 * <p>Karena dipanggil di jalur simpan, implementasi sebaiknya murni terhadap state object dan
	 * tidak membuka session/transaksi baru sendiri.</p>
	 */
	protected abstract void onUpdate();

	/**
	 * Referensi ke object asal ketika entity ini dibuat lewat operasi salin/duplikasi. Tidak
	 * dipetakan sebagai kolom database di kelas ini; murni penanda in-memory.
	 */
	private GeneralValueObject copyDari = null;

	/**
	 * Mengembalikan object asal bila entity ini merupakan hasil salinan.
	 *
	 * @return object sumber salinan, atau {@code null} bila entity ini bukan hasil salinan
	 */
	public GeneralValueObject getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai object asal dari mana entity ini disalin.
	 *
	 * @param copyDari object sumber salinan (boleh {@code null} untuk menghapus penanda)
	 */
	public void setCopyDari(GeneralValueObject copyDari) {
		this.copyDari = copyDari;
	}

	/**
	 * Representasi teks standar seluruh entity AIS: {@code "kode - nama"}, atau hanya
	 * {@code "nama"} bila {@link #getKode()} bernilai {@code null}.
	 *
	 * <p>Bentuk ini dipakai luas oleh komponen ZK (isi {@code Combobox}, {@code Listcell},
	 * label bandbox) sehingga mengubah formatnya berdampak ke tampilan banyak layar. Perhatikan
	 * bahwa {@code nama} yang {@code null} akan tercetak sebagai teks {@code "null"}; subclass
	 * yang butuh tampilan lain wajib meng-override method ini.</p>
	 *
	 * @return gabungan kode dan nama entity
	 */
	public String toString() {
		return (getKode() == null ? "" : getKode() + " - ") + getNama();
	}

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk
	 * membuat instance saat hidrasi entity dari hasil query.
	 */
	public GeneralValueObject() {

	}

	/**
	 * Constructor pintas yang langsung menyetel primary key.
	 *
	 * <p>Berguna untuk membuat object "penunjuk" (hanya berisi id) sebagai parameter kriteria
	 * atau referensi relasi tanpa memuat seluruh baris dari database.</p>
	 *
	 * @param id nilai primary key yang disetel lewat {@link #setId(Long)}
	 */
	public GeneralValueObject(Long id) {
		setId(id);
	}

	/**
	 * Mengembalikan primary key entity.
	 *
	 * <p>Nilai inilah yang menjadi dasar {@link #equals(Object)}, kunci
	 * {@link EntityIdentityMap}, dan bagian nama berkas pada seluruh method cache di kelas ini.
	 * Bernilai {@code null} untuk entity yang belum pernah disimpan.</p>
	 *
	 * @return primary key, atau {@code null} bila entity belum tersimpan
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Menyetel primary key entity. Tanpa validasi.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** Primary key entity. Lihat {@link #getId()}. */
	private Long id;
	/** Kode ringkas entity; bagian pertama {@link #toString()}. */
	private String kode;
	/** Nama entity; bagian kedua {@link #toString()} dan kunci urut ketiga {@link #compareTo(GeneralValueObject)}. */
	private String nama;
	/** Nomor induk (NIM/NIS) bila entity mewakili peserta didik; kunci urut kedua {@link #compareTo(GeneralValueObject)}. */
	private String nim;
	/** Keterangan bebas; kunci urut terakhir {@link #compareTo(GeneralValueObject)}. */
	private String keterangan;
	/** Nomor urut tampil; kunci urut PERTAMA {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;

	/** Nama pengguna terakhir yang mengubah entity. Diisi jalur audit; lihat {@link #setOleh(String)}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah entity. Diisi jalur audit; lihat {@link #setOlehId(String)}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah entity ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>.
	 *
	 * <p>Nilai {@code null} atau string kosong/spasi <b>diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa mengubah apa pun). Konsekuensinya nilai lama tidak pernah bisa dihapus
	 * lewat setter ini — perilaku ini disengaja agar jejak audit yang sudah terisi tidak terhapus
	 * oleh jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. proses batch atau
	 * penjadwal yang berjalan tanpa sesi login).</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam sehingga
	 * jejak audit yang sudah terisi tidak bisa terhapus tanpa sengaja.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah entity ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Stempel waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object memakai
	 * {@code WaktuUtil.getDate()} (jam server aplikasi, bukan jam database), sehingga entity baru
	 * selalu punya nilai walau jalur simpan lupa mengisinya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} ({@code @Temporal(TemporalType.TIMESTAMP)}) sehingga
	 * bagian jam ikut tersimpan pada subclass yang memetakan properti ini ke kolom.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} kecuali sengaja disetel demikian
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Filter XSS ringan: menetralkan kata {@code "script"} pada teks masukan.
	 *
	 * <p>Setiap kemunculan {@code script} (tidak peduli besar-kecil huruf, termasuk yang menyatu
	 * dengan kata lain seperti {@code javascript} atau {@code descriptions}) diganti menjadi
	 * {@code __S__}. Masukan {@code null} dinormalkan menjadi string kosong sehingga method ini
	 * tidak pernah mengembalikan {@code null}.</p>
	 *
	 * <p>Berbeda dengan {@link #filterTidakBoleh(String)}, versi "sederhana" ini <b>tidak</b>
	 * mengubah huruf teks menjadi kapital dan tidak membuang daftar kata terlarang lain, sehingga
	 * aman dipakai pada teks yang harus tetap terbaca apa adanya. Ini bukan sanitizer HTML lengkap:
	 * atribut event seperti {@code onerror=} tidak tersentuh, jadi tetap lakukan escaping pada sisi
	 * tampilan.</p>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch}; kegagalan tak terduga dicatat ke audit
	 * error dan teks dikembalikan pada kondisi terakhirnya.</p>
	 *
	 * @param isi teks masukan pengguna; boleh {@code null}
	 * @return teks yang sudah dinetralkan, tidak pernah {@code null}
	 * @see #filterTidakBoleh(String)
	 */
	public static String filterTidakBolehSederhana(String isi) {
		try {
			isi = isi == null ? "" : isi;
			if (!isi.isEmpty()) {
				if (isi != null && isi.toLowerCase().contains("script")) {
					isi = isi.replaceAll("(?i)script", "__S__");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:153");
		}
		return isi;
	}

	/**
	 * Filter XSS/kata terlarang versi penuh.
	 *
	 * <p>Dua tahap:</p>
	 * <ol>
	 *   <li>Setiap token pada {@code ConstantValues.filter_tidak_boleh_ada} (daftar dipisah
	 *   titik koma; per 2 Sep 2026 berisi {@code MARQUEE}, {@code SCRIPT}, {@code FUNCTION},
	 *   {@code JAVA:ALERT}, {@code <BODY}) yang ditemukan pada teks akan <b>dibuang</b>.</li>
	 *   <li>Sisa kemunculan {@code script} diganti {@code __S__}, seperti pada
	 *   {@link #filterTidakBolehSederhana(String)}.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang sering mengejutkan:</b> begitu satu saja token terlarang ditemukan,
	 * teks diproses lewat {@code isi.toUpperCase().replaceAll(...)} sehingga <b>SELURUH teks
	 * berubah menjadi huruf kapital</b> dan tidak pernah dikembalikan ke bentuk semula. Karena itu
	 * gunakan {@link #filterTidakBolehSederhana(String)} bila kapitalisasi asli harus dipertahankan.
	 * Perhatikan pula bahwa token dipakai langsung sebagai ekspresi reguler pada {@code replaceAll},
	 * jadi menambahkan token bermuatan metakarakter regex ke konstanta tersebut dapat mengubah
	 * perilaku filter secara tidak terduga.</p>
	 *
	 * <p>Masukan {@code null} dinormalkan menjadi string kosong; kegagalan tak terduga dicatat ke
	 * audit error dan teks dikembalikan pada kondisi terakhirnya. Sama seperti versi sederhana,
	 * ini bukan pengganti escaping pada lapisan tampilan.</p>
	 *
	 * @param isi teks masukan pengguna; boleh {@code null}
	 * @return teks yang sudah difilter, tidak pernah {@code null}
	 * @see #filterTidakBolehSederhana(String)
	 */
	public static String filterTidakBoleh(String isi) {
		try {
			isi = isi == null ? "" : isi;
			if (!isi.isEmpty()) {
				for (String s : ConstantValues.filter_tidak_boleh_ada.split(";")) {
					if (isi.toUpperCase().contains(s)) {
						isi = isi.toUpperCase().replaceAll(s, "");
					}
				}
//				isi = isi.replaceAll("\\<.*?\\>", "");
				if (isi != null && isi.toLowerCase().contains("script")) {
					isi = isi.replaceAll("(?i)script", "__S__");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:173");
		}
		return isi;
	}


	/**
	 * Penanda bahwa object ini sudah pernah dinyatakan "aman/ter-inisialisasi" oleh
	 * {@link #check(Object)}.
	 *
	 * <p>Berfungsi sebagai jalan pintas: pada pemanggilan {@code check()} berikutnya, object yang
	 * {@code initData == true} DAN masih lolos {@link #isInitializedSafely(Object)} langsung
	 * dikembalikan tanpa menyentuh cache maupun database. Karena {@code check()} dipanggil dari
	 * ribuan getter relasi, jalan pintas inilah yang membuat biayanya tetap wajar.</p>
	 *
	 * <p>Field ini <b>tidak</b> di-{@code transient}, jadi ikut terserialisasi ke cache MapDB/
	 * session ZK. Setelah deserialisasi, nilai {@code true} yang terbawa tetap divalidasi ulang
	 * oleh {@code isInitializedSafely(...)} pada baris yang sama, sehingga object yang sudah
	 * detached tidak lolos begitu saja.</p>
	 */
	private boolean initData = false;

	/**
	 * Menentukan nama kelas entity <b>asli</b> dari sebuah object, dengan membuang lapisan proxy.
	 *
	 * <p>Dibutuhkan {@link #check(Object)} karena nama kelas proxy tidak bisa dipakai untuk
	 * {@code Class.forName(...)} maupun sebagai kunci cache. Urutan usaha:</p>
	 * <ol>
	 *   <li>Bila object adalah {@link HibernateProxy}, ambil
	 *   {@code getHibernateLazyInitializer().getPersistentClass().getName()} — sumber paling
	 *   akurat. Kegagalan dicatat ke audit lalu dilanjutkan ke fallback.</li>
	 *   <li>Potong nama kelas pada penanda proxy {@code "_$$_"} (gaya Javassist).</li>
	 *   <li>Potong pada {@code "$$"} (gaya CGLIB/lainnya).</li>
	 *   <li>Khusus untuk kelas di paket {@code ais.database.model.}, potong pada garis bawah
	 *   pertama — konvensi lokal AIS untuk nama kelas berimbuhan.</li>
	 * </ol>
	 *
	 * <p><b>Catatan kehati-hatian:</b> aturan (4) memotong pada garis bawah <i>mana pun</i>, jadi
	 * kelas entity yang namanya memang mengandung garis bawah akan ikut terpotong. Untuk kelas
	 * di luar paket {@code ais.database.model} aturan ini tidak berlaku.</p>
	 *
	 * @param data object apa pun, proxy maupun bukan; boleh {@code null}
	 * @return nama kelas persisten yang sudah bersih dari lapisan proxy, atau {@code null} bila
	 *         {@code data} bernilai {@code null}
	 */
	private static String getCacheClassName(Object data) {
		if (data == null) {
			return null;
		}

		try {
			if (data instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) data).getHibernateLazyInitializer();
				if (initializer != null && initializer.getPersistentClass() != null) {
					return initializer.getPersistentClass().getName();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:193");
			// Abaikan, lanjut fallback ke nama class biasa.
		}

		String className = data.getClass().getName();
		int proxyIndex = className.indexOf("_$$_");
		if (proxyIndex > 0) {
			return className.substring(0, proxyIndex);
		}

		proxyIndex = className.indexOf("$$");
		if (proxyIndex > 0) {
			return className.substring(0, proxyIndex);
		}

		proxyIndex = className.indexOf("_");
		if (proxyIndex > 0 && className.indexOf("ais.database.model.") == 0) {
			return className.substring(0, proxyIndex);
		}

		return className;
	}

	/**
	 * Menentukan identifier entity untuk keperluan pencarian cache/reload, <b>tanpa memicu
	 * inisialisasi proxy lazy</b>.
	 *
	 * <p>Ini titik yang halus: memanggil {@code getId()} pada proxy detached bisa langsung
	 * melempar {@code LazyInitializationException} — persis masalah yang hendak diperbaiki
	 * {@link #check(Object)}. Karena itu urutannya:</p>
	 * <ol>
	 *   <li>Bila object adalah {@link HibernateProxy}, ambil identifier dari
	 *   {@code LazyInitializer.getIdentifier()}. Identifier tersimpan di proxy itu sendiri
	 *   sehingga aman dibaca tanpa session.</li>
	 *   <li>Tangani dua entity dengan primary key non-standar bertipe {@code String}:
	 *   {@link Tbmuser} memakai {@code getUserId()} dan {@link Tbmrole} memakai {@code getRoleId()}
	 *   (keduanya tidak memakai kolom {@code id}). Bila getter khusus ini gagal — misalnya karena
	 *   object sudah detached — method mengembalikan {@code null} dan {@code check()} akan
	 *   menyerah dengan tenang.</li>
	 *   <li>Selain itu pakai {@link #getId()} biasa, juga dibungkus {@code try/catch}.</li>
	 * </ol>
	 *
	 * @param object entity/proxy yang ingin dicari identifier-nya; boleh {@code null}
	 * @return identifier entity, atau {@code null} bila tidak dapat ditentukan dengan aman
	 */
	private static Serializable getCacheLookupId(GeneralValueObject object) {
		if (object == null) {
			return null;
		}

		try {
			if (object instanceof HibernateProxy) {
				LazyInitializer initializer = ((HibernateProxy) object).getHibernateLazyInitializer();
				if (initializer != null && initializer.getIdentifier() instanceof Serializable) {
					return (Serializable) initializer.getIdentifier();
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:228");
			// Abaikan, lanjut fallback ke getter biasa.
		}

		try {
			if (object instanceof Tbmuser) {
				return ((Tbmuser) object).getUserId();
			} else if (object instanceof Tbmrole) {
				return ((Tbmrole) object).getRoleId();
			}
		} catch (Exception e) {
			// Proxy detached dapat error saat getter id non-standar dipanggil.
			return null;
		}

		try {
			return object.getId();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Membungkus {@code Hibernate.isInitialized(data)} agar tidak pernah melempar exception.
	 *
	 * <p>Hanya <b>memeriksa</b>, tidak menginisialisasi apa pun. Setiap kegagalan diterjemahkan
	 * menjadi {@code false} ("anggap belum aman") sehingga {@link #check(Object)} akan meneruskan
	 * ke jalur resolusi berikutnya alih-alih meledak.</p>
	 *
	 * @param data object yang diperiksa; boleh {@code null}
	 * @return {@code true} bila {@code data} bernilai {@code null} (dianggap aman) atau sudah
	 *         terinisialisasi; {@code false} bila belum atau pemeriksaan gagal
	 */
	private static boolean isInitializedSafely(Object data) {
		if (data == null) {
			return true;
		}
		try {
			return Hibernate.isInitialized(data);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Tahap 2 resolusi {@link #check(Object)}: mencoba menginisialisasi proxy memakai session yang
	 * <i>kebetulan</i> masih menaunginya.
	 *
	 * <p>Memanggil {@code Hibernate.initialize(data)} bila object belum terinisialisasi, lalu
	 * melaporkan hasil akhirnya. Bila object sudah <i>detached</i> (session penaungnya sudah
	 * ditutup), {@code Hibernate.initialize} akan melempar {@code LazyInitializationException};
	 * exception itu <b>ditelan dengan sengaja</b> dan method mengembalikan {@code false} agar
	 * {@code check()} melanjutkan ke tahap reload lewat session baru.</p>
	 *
	 * <p>Perhatikan bahwa method ini <b>tidak</b> membuka session sendiri — namanya sudah
	 * menegaskan itu: hanya memakai session yang tersedia.</p>
	 *
	 * @param data object/proxy yang ingin diinisialisasi; boleh {@code null}
	 * @return {@code true} bila setelah pemanggilan ini object dijamin terinisialisasi (termasuk
	 *         bila {@code data} bernilai {@code null}); {@code false} bila gagal
	 */
	private static boolean initializeWithAvailableSession(Object data) {
		if (data == null) {
			return true;
		}
		try {
			if (!Hibernate.isInitialized(data)) {
				Hibernate.initialize(data);
			}
			return Hibernate.isInitialized(data);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Tahap 1 resolusi {@link #check(Object)}: mengambil entity dari cache in-memory
	 * {@link ConstantValues}, tetapi hanya bila hasilnya benar-benar aman dipakai.
	 *
	 * <p>Memanggil {@code ConstantValues.ambil(kelas, id, false)} — argumen ketiga {@code false}
	 * penting: cache <b>tidak</b> boleh jatuh ke query database di tahap ini, karena akses
	 * database adalah urusan tahap 3 ({@link #reloadDetachedObject(String, Serializable)}). Hasil
	 * cache baru diterima setelah lolos {@link #initializeWithAvailableSession(Object)}; object
	 * yang lolos diberi tanda {@code initData = true} supaya pemanggilan {@code check()}
	 * berikutnya atas object tersebut langsung selesai di jalan pintas.</p>
	 *
	 * <p>Seluruh badan dibungkus {@code try/catch} dengan alasan yang tertulis di kode: cache tidak
	 * boleh membuat getter entity gagal. Kegagalan apa pun berujung {@code null} sehingga alur
	 * berlanjut ke tahap berikutnya.</p>
	 *
	 * @param cacheClassName nama kelas persisten hasil {@link #getCacheClassName(Object)}
	 * @param lookupId       identifier hasil {@link #getCacheLookupId(GeneralValueObject)}
	 * @return entity dari cache yang sudah terinisialisasi, atau {@code null} bila cache kosong,
	 *         argumen tidak lengkap, atau hasilnya tidak aman dipakai
	 */
	private static GeneralValueObject ambilDariCacheJikaAman(String cacheClassName, Serializable lookupId) {
		if (cacheClassName == null || cacheClassName.trim().length() == 0 || lookupId == null) {
			return null;
		}
		try {
			GeneralValueObject fetchedObject = ConstantValues.ambil(cacheClassName, lookupId, false);
			if (fetchedObject == null) {
				return null;
			}

			if (initializeWithAvailableSession(fetchedObject)) {
				fetchedObject.initData = true;
				return fetchedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:289");
			// Cache tidak boleh membuat proses getter gagal.
		}
		return null;
	}

	/**
	 * Tahap 3 (penyelamat terakhir) resolusi {@link #check(Object)}: membaca ulang entity dari
	 * database memakai <b>session baru khusus</b>.
	 *
	 * <p>Inilah inti perbaikan yang membuat proxy lazy detached tidak lagi meledak. Alurnya:
	 * {@code Class.forName(cacheClassName)} &rarr;
	 * {@code HibernateUtil.getSessionFactory().openSession()} &rarr;
	 * {@code session.get(kelas, id)} &rarr; {@code Hibernate.initialize(...)} sebagai pengaman
	 * tambahan &rarr; tandai {@code initData = true} &rarr; tutup session di {@code finally}
	 * lewat {@link #closeSessionCreatedByCheckQuietly(Session)}.</p>
	 *
	 * <p><b>Penting soal biaya:</b> method ini membuka koneksi database sendiri, jadi ia adalah
	 * bagian termahal dari {@code check()}. Entity yang dikembalikan berasal dari session yang
	 * <i>sudah ditutup</i>, sehingga hanya entity utamanya yang dijamin terinisialisasi —
	 * relasi lazi di dalamnya akan mengalami siklus {@code check()} yang sama saat getter-nya
	 * dipanggil. Bila sebuah alur ternyata memanggil ini berulang-ulang untuk banyak baris,
	 * itu tanda koleksinya seharusnya dimuat ulang lewat satu query, bukan lewat {@code check()}.</p>
	 *
	 * <p>Blok {@code catch} sengaja senyap; alasan aslinya tercatat di kode: {@code check(...)}
	 * dipanggil sangat sering dari getter entity, sehingga menampilkan error di sini akan
	 * membanjiri log padahal jalur ini hanya penjaga untuk proxy detached.</p>
	 *
	 * @param cacheClassName nama kelas persisten yang akan dimuat; harus bisa
	 *                       di-{@code Class.forName}
	 * @param lookupId       identifier baris yang akan dimuat
	 * @return entity hasil pemuatan ulang yang sudah terinisialisasi, atau {@code null} bila
	 *         argumen tidak lengkap, kelas tidak ditemukan, baris tidak ada, atau terjadi
	 *         kegagalan lain
	 */
	@SuppressWarnings("rawtypes")
	private static GeneralValueObject reloadDetachedObject(String cacheClassName, Serializable lookupId) {
		if (cacheClassName == null || cacheClassName.trim().length() == 0 || lookupId == null) {
			return null;
		}

		Session session = null;
		try {
			Class entityClass = Class.forName(cacheClassName);
			session = HibernateUtil.getSessionFactory().openSession();
			Object loaded = session.get(entityClass, lookupId);
			if (loaded instanceof GeneralValueObject) {
				try {
					Hibernate.initialize(loaded);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:309");
					// session.get(...) normalnya sudah menginisiasi entity utama.
				}

				GeneralValueObject loadedObject = (GeneralValueObject) loaded;
				loadedObject.initData = true;
				return loadedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:317");
			/*
			 * Method check(...) dipanggil sangat sering dari getter entity. Jangan tampilkan
			 * error berulang-ulang di sini karena fallback ini hanya penjaga proxy detached.
			 */
		} finally {
			closeSessionCreatedByCheckQuietly(session);
		}
		return null;
	}

	/**
	 * Menutup session yang dibuka sendiri oleh {@link #reloadDetachedObject(String, Serializable)}
	 * secara bertahap dan tanpa pernah melempar exception.
	 *
	 * <p>Tiga langkah, masing-masing di blok {@code try/catch} terpisah supaya kegagalan satu
	 * langkah tidak menggagalkan langkah berikutnya:</p>
	 * <ol>
	 *   <li>{@code clear()} — lepaskan entity dari persistence context agar tidak ada auto-flush
	 *   tak sengaja saat penutupan.</li>
	 *   <li>{@code disconnect()} — kembalikan koneksi JDBC ke pool secepatnya.</li>
	 *   <li>{@code close()} — hanya bila session memang masih terbuka.</li>
	 * </ol>
	 *
	 * <p>Kebocoran koneksi di sini akan sangat merusak: {@code check()} berjalan di jalur getter
	 * yang dipanggil ribuan kali, jadi satu session yang lupa ditutup akan cepat menghabiskan pool.
	 * Jangan menghapus atau menyederhanakan pemanggilan ini dari blok {@code finally}.</p>
	 *
	 * @param session session yang akan ditutup; aman bila {@code null}
	 */
	private static void closeSessionCreatedByCheckQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:334");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:338");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:344");
		}
	}

	/**
	 * <b>Method paling kritis di seluruh kelas ini.</b> Meresolusi proxy lazy Hibernate menjadi
	 * object yang benar-benar bisa dipakai, atau mengembalikan argumen apa adanya bila tidak
	 * mungkin.
	 *
	 * <h4>Kenapa ada</h4>
	 * <p>Relasi {@code @ManyToOne} di entity AIS dipetakan {@code FetchType.LAZY}, sedangkan
	 * object entity kerap hidup lebih lama daripada {@link Session} yang memuatnya: disimpan di
	 * cache in-memory/MapDB, dibawa lintas request ZK, diserialkan ke session HTTP. Menyentuh proxy
	 * lazy yang sudah <i>detached</i> akan melempar {@code LazyInitializationException}. Karena itu
	 * pola getter standar di seluruh entity AIS adalah memanggil method ini lebih dulu:</p>
	 * <pre>{@code
	 * public Jurusan getJurusan() {
	 *     jurusan = check(jurusan);
	 *     return this.jurusan;
	 * }
	 * }</pre>
	 * <p>Perhatikan bahwa hasil {@code check()} <b>ditugaskan kembali ke field</b> — itu penting,
	 * karena object yang dikembalikan bisa jadi instance LAIN (kanonik dari identity map, dari
	 * cache, atau hasil reload) dan bukan proxy semula.</p>
	 *
	 * <h4>Urutan resolusi</h4>
	 * <ol>
	 *   <li><b>Jalan pintas</b> — bila {@code initData} sudah {@code true} dan
	 *   {@link #isInitializedSafely(Object)} lolos, object dikembalikan langsung. Inilah yang
	 *   membuat biaya rata-rata tetap murah meski method ini dipanggil dari ribuan getter.</li>
	 *   <li><b>Tahap 0 — {@link EntityIdentityMap}</b>: bila identifier bertipe {@code Long} dan
	 *   kelas aslinya turunan {@code GeneralValueObject}, ambil instance kanonik JVM-wide untuk
	 *   pasangan kelas+ID. Bila ada, itu yang dikembalikan tanpa lazy-init maupun query. Ini yang
	 *   menjamin perubahan field skalar (mis. {@code NominalBiaya.bukanTagihan = true}) langsung
	 *   terlihat oleh SEMUA pemegang referensi ke entity ber-ID sama.</li>
	 *   <li><b>Tahap 1 — cache</b>: {@link #ambilDariCacheJikaAman(String, Serializable)}, tanpa
	 *   fallback database.</li>
	 *   <li><b>Tahap 2 — session yang tersedia</b>:
	 *   {@link #initializeWithAvailableSession(Object)}. Berhasil bila object masih attached;
	 *   gagal diam-diam bila sudah detached.</li>
	 *   <li><b>Tahap 3 — reload lewat session baru</b>:
	 *   {@link #reloadDetachedObject(String, Serializable)} membuka {@code openSession()} sendiri
	 *   dan menutupnya di {@code finally}.</li>
	 * </ol>
	 *
	 * <h4>Kontrak kegagalan</h4>
	 * <p>Method ini <b>tidak pernah melempar exception</b> dan <b>tidak pernah mengembalikan
	 * {@code null}</b> untuk argumen non-null: bila seluruh tahap gagal, {@code data} dikembalikan
	 * apa adanya. Argumen yang {@code null} atau bukan turunan {@code GeneralValueObject}
	 * dikembalikan langsung tanpa diproses. Konsekuensinya kegagalan resolusi bersifat senyap —
	 * bila sebuah getter relasi tetap mengembalikan proxy yang meledak di pemanggil, curigai tahap
	 * 3 (nama kelas hasil de-proxy tidak bisa di-{@code Class.forName}, atau identifier tidak
	 * terbaca oleh {@link #getCacheLookupId(GeneralValueObject)}).</p>
	 *
	 * @param <T>  tipe object yang diperiksa
	 * @param data object yang mungkin berupa proxy lazy; boleh {@code null}
	 * @return object yang sudah teresolusi (bisa jadi instance berbeda dari argumen), atau
	 *         {@code data} apa adanya bila resolusi tidak mungkin
	 * @see #chek(Object)
	 * @see #resolveLazy(Object)
	 * @see EntityIdentityMap#get(Class, Long)
	 */
	@SuppressWarnings("unchecked")
	public static <T> T check(T data) {
		if (data == null || !(data instanceof GeneralValueObject)) {
			return data;
		}

		GeneralValueObject object = (GeneralValueObject) data;
		try {
			if (object.initData && isInitializedSafely(object)) {
				return (T) object;
			}

			Serializable lookupId = getCacheLookupId(object);
			String cacheClassName = getCacheClassName(object);

			/*
			 * 0) EntityIdentityMap: satu Java object per entity ID di seluruh JVM.
			 *    Jika canonical sudah terdaftar (mis. NominalBiaya yang baru disimpan),
			 *    kembalikan langsung tanpa lazy-init atau query. Ini menjamin perubahan
			 *    scalar (mis. bukanTagihan=true) langsung terlihat oleh SEMUA pemegang
			 *    referensi ke entity dengan ID yang sama.
			 */
			if (lookupId instanceof Long && cacheClassName != null) {
				try {
					Class<?> realClass = Class.forName(cacheClassName);
					if (GeneralValueObject.class.isAssignableFrom(realClass)) {
						@SuppressWarnings("unchecked")
						GeneralValueObject canonical = EntityIdentityMap.get(
								(Class<? extends GeneralValueObject>) realClass, (Long) lookupId);
						if (canonical != null) {
							return (T) canonical;
						}
					}
				} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:381");
				}
			}

			/*
			 * 1) Coba ambil dari cache existing. Jika cache berisi object yang sudah aman,
			 *    kembalikan object tersebut agar tidak perlu query database.
			 */
			GeneralValueObject fetchedObject = ambilDariCacheJikaAman(cacheClassName, lookupId);
			if (fetchedObject != null) {
				return (T) fetchedObject;
			}

			/*
			 * 2) Jika object masih attached pada session aktif, initialize normal.
			 *    Jika sudah detached, bagian ini akan gagal diam-diam dan lanjut ke reload.
			 */
			if (initializeWithAvailableSession(object)) {
				object.initData = true;
				return (T) object;
			}

			/*
			 * 3) Inilah enhancement utama: saat proxy lazy sudah detached dan cache tidak
			 *    menyediakan object yang aman, baca ulang entity berdasarkan identifier
			 *    menggunakan openSession() khusus, initialize entity utamanya, lalu tutup
			 *    session di finally. Dengan begitu getter pemanggil menerima object asli,
			 *    bukan proxy detached yang memicu LazyInitializationException.
			 */
			GeneralValueObject reloadedObject = reloadDetachedObject(cacheClassName, lookupId);
			if (reloadedObject != null) {
				return (T) reloadedObject;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:414");
			// check(...) tidak boleh membuat getter entity gagal.
		}

		return data;
	}

	/**
	 * Alias historis {@link #check(Object)} dengan ejaan lama ({@code chek}). Perilakunya
	 * <b>identik</b> — hanya meneruskan pemanggilan.
	 *
	 * <p>Masih dipakai sebagian kode lama. Untuk kode baru gunakan {@link #check(Object)} atau
	 * {@link #resolveLazy(Object)} yang namanya lebih jelas; jangan menghapus method ini karena
	 * masih ada pemanggil aktif.</p>
	 *
	 * @param <T>  tipe object yang diperiksa
	 * @param data object yang mungkin berupa proxy lazy; boleh {@code null}
	 * @return hasil {@link #check(Object)}
	 * @see #check(Object)
	 */
	public static <T> T chek(T data) {
		return (T) check(data);
	}

	/**
	 * Alias deskriptif {@link #check(Object)} untuk kode baru. Perilakunya <b>identik</b> — hanya
	 * meneruskan pemanggilan; namanya menegaskan apa yang sebenarnya terjadi (resolusi relasi
	 * lazy), sehingga lebih mudah dipahami pembaca baru dibanding {@code check}/{@code chek}.
	 *
	 * @param <T>  tipe object yang diperiksa
	 * @param data object yang mungkin berupa proxy lazy; boleh {@code null}
	 * @return hasil {@link #check(Object)}
	 * @see #check(Object)
	 */
	public static <T> T resolveLazy(T data) {
		return (T) check(data);
	}

	/**
	 * Kesetaraan entity berbasis primary key.
	 *
	 * <p>Aturannya: bila {@code obj} juga {@code GeneralValueObject} <b>dan</b> kedua object punya
	 * {@code id} non-null, perbandingan dilakukan atas {@code id} saja. Di luar itu — argumen bukan
	 * {@code GeneralValueObject}, atau salah satu {@code id} masih {@code null} — perbandingan
	 * jatuh ke {@code super.equals(obj)}, yaitu perbandingan identitas referensi bawaan
	 * {@code Object}. Itu tepat untuk entity yang belum tersimpan: dua entity baru yang sama-sama
	 * ber-{@code id} {@code null} tidak boleh dianggap sama.</p>
	 *
	 * <p><b>Batasan yang harus disadari:</b> perbandingan <b>tidak</b> memeriksa kelas, sehingga
	 * dua entity dari tabel BERBEDA yang kebetulan ber-{@code id} sama akan dinyatakan
	 * {@code equals} (mis. {@code Mahasiswa#5} vs {@code Dosen#5}). Jangan mencampur tipe entity
	 * dalam satu {@code List} lalu mengandalkan {@code contains()}/{@code indexOf()}.</p>
	 *
	 * <p><b>Batasan kedua, lebih penting:</b> {@code hashCode()} TIDAK di-override di kelas ini
	 * maupun di {@link DataUtil}, sehingga kontrak {@code equals}/{@code hashCode} tidak
	 * terpenuhi. Dua instance berbeda dengan {@code id} sama akan {@code equals} tetapi punya hash
	 * berbeda. Karena itu <b>jangan</b> memakai entity sebagai elemen {@code HashSet} atau kunci
	 * {@code HashMap} untuk deduplikasi — pakailah {@code Map<Long, Entity>} berkunci {@code id}
	 * seperti yang dilakukan method-method di kelas ini sendiri.</p>
	 *
	 * @param obj object pembanding; boleh {@code null}
	 * @return {@code true} bila dianggap entity yang sama menurut aturan di atas
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof GeneralValueObject && getId() != null) {
			GeneralValueObject workspace = (GeneralValueObject) obj;
			if (workspace.getId() == null) {
				return super.equals(obj);
			} else {
				return getId().equals(workspace.getId());
			}
		} else {
			return super.equals(obj);
		}
	}

	/**
	 * Urutan alami seluruh entity AIS: <b>berjenjang menurut kunci pertama yang tersedia di kedua
	 * belah pihak</b>.
	 *
	 * <p>Prioritas kunci, berhenti pada yang pertama cocok:</p>
	 * <ol>
	 *   <li>{@code nomorUrut} — urutan tampil yang ditentukan pengguna; menang atas segalanya.</li>
	 *   <li>{@code nim} — nomor induk peserta didik (perbandingan String, jadi urutannya
	 *   leksikografis; NIM dengan panjang berbeda tidak terurut secara numerik).</li>
	 *   <li>{@code nama} — perbandingan String peka besar-kecil huruf, sehingga huruf kapital
	 *   selalu mendahului huruf kecil.</li>
	 *   <li>{@code keterangan} — pilihan terakhir. Perhatikan {@link #getKeterangan()} tidak pernah
	 *   mengembalikan {@code null} (mengembalikan {@code ""}), sehingga cabang ini <b>selalu</b>
	 *   terpakai bila ketiga kunci sebelumnya tidak memenuhi syarat.</li>
	 * </ol>
	 *
	 * <p>Sebuah kunci hanya dipakai bila <b>kedua</b> object memilikinya (non-null). Bila tidak
	 * ada satu pun kunci yang memenuhi syarat — atau terjadi exception, yang ditelan dan dicatat ke
	 * audit — method mengembalikan {@code 0}, artinya "dianggap setara untuk keperluan pengurutan".
	 * Nilai {@code 0} ini <b>tidak</b> berarti {@code equals}: {@code compareTo} di sini memang
	 * tidak konsisten dengan {@link #equals(Object)}, jadi hindari {@code TreeSet}/{@code TreeMap}
	 * berkunci entity dan gunakan comparator eksplisit bila urutan harus deterministik.</p>
	 *
	 * @param arg0 entity pembanding
	 * @return negatif/nol/positif sesuai kontrak {@link Comparable}; {@code 0} bila tidak ada kunci
	 *         pembanding yang tersedia
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:456");

		}

		return 0;
	}

	/**
	 * Mengembalikan nama entity. Dipakai {@link #toString()} dan kunci urut ketiga
	 * {@link #compareTo(GeneralValueObject)}.
	 *
	 * @return nama entity, atau {@code null} bila belum diisi
	 */
	public String getNama() {
		return nama;
	}

	/**
	 * Menyetel nama entity. Tanpa validasi.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan entity, dengan <b>normalisasi</b>: nilai {@code null} dikembalikan
	 * sebagai string kosong sehingga pemanggil tidak perlu memeriksa {@code null}.
	 *
	 * <p>Sebagai akibatnya, cabang {@code keterangan} pada {@link #compareTo(GeneralValueObject)}
	 * selalu memenuhi syarat non-null.</p>
	 *
	 * @return keterangan entity, atau {@code ""} bila belum diisi; tidak pernah {@code null}
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan entity. Tanpa validasi; nilai {@code null} diterima dan akan terbaca
	 * sebagai {@code ""} lewat {@link #getKeterangan()}.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kode ringkas entity. Menjadi bagian pertama {@link #toString()}.
	 *
	 * @return kode entity, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel kode ringkas entity. Tanpa validasi.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nomor induk (NIM/NIS) bila entity mewakili peserta didik. Kunci urut kedua
	 * {@link #compareTo(GeneralValueObject)}.
	 *
	 * @return nomor induk, atau {@code null} bila tidak relevan/belum diisi
	 */
	public String getNim() {
		return nim;
	}

	/**
	 * Menyetel nomor induk (NIM/NIS). Tanpa validasi.
	 *
	 * @param nim nomor induk baru
	 */
	public void setNim(String nim) {
		this.nim = nim;
	}

	/**
	 * Mengembalikan nomor urut tampil. Kunci urut PERTAMA
	 * {@link #compareTo(GeneralValueObject)}, jadi mengisinya akan menimpa pengurutan berdasarkan
	 * NIM/nama.
	 *
	 * @return nomor urut, atau {@code null} bila tidak dipakai
	 */
	public Integer getNomorUrut() {
		return nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Tanpa validasi.
	 *
	 * @param nomorUrut nomor urut baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membaca kembali data JSON sementara milik object ini dari berkas cache temp.
	 *
	 * <p>Pasangan baca untuk {@link #write(Integer, String...)}. Kunci berkas dipilih menurut jenis
	 * entity — pola percabangan yang berulang di hampir semua method cache di kelas ini:
	 * {@link Tbmrole} memakai {@code roleId}, {@link Tbmuser} memakai {@code userId}, entity lain
	 * memakai {@link #getId()}.</p>
	 *
	 * <p><b>Ini BUKAN pembacaan database.</b> Isinya adalah cache berkas yang boleh hilang kapan
	 * saja; kolom database tetap menjadi sumber kebenaran.</p>
	 *
	 * @return isi cache JSON milik object ini, atau {@link JSONObject} kosong bila entity belum
	 *         punya identifier
	 * @see #write(Integer, String...)
	 * @see #delete()
	 */
	public JSONObject read() {
		if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null) {
			return Common.getJSONTemporary(this, ((Tbmrole) this).getRoleId());
		} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null) {
			return Common.getJSONTemporary(this, ((Tbmuser) this).getUserId());
		} else if (getId() != null) {
			return Common.getJSONTemporary(this, getId().toString());
		}
		return new JSONObject();
	}

	/**
	 * Menghapus <b>berkas cache JSON</b> milik object ini.
	 *
	 * <p><b>PERINGATAN — jangan tertukar:</b> method ini <b>TIDAK</b> menghapus baris database.
	 * Yang dihapus hanyalah berkas temp hasil {@link #write(Integer, String...)}, lewat
	 * {@code BacaTulisUtil.hapus(file)} disusul {@code File.delete()}; hasil penghapusan dicetak
	 * ke {@code System.out}. Penghapusan baris database adalah urusan lapisan DAO/service.</p>
	 *
	 * <p>Kunci berkas dipilih menurut jenis entity ({@link Tbmrole} &rarr; {@code roleId},
	 * {@link Tbmuser} &rarr; {@code userId}, selain itu {@link #getId()}). Bila entity belum punya
	 * identifier, method tidak melakukan apa pun. Aman dipanggil walau berkasnya memang tidak ada.</p>
	 *
	 * @see #read()
	 * @see #write(Integer, String...)
	 */
	public void delete() {
		if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null) {
			File file = Common.getFileLocation(this, ((Tbmrole) this).getRoleId());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null) {
			File file = Common.getFileLocation(this, ((Tbmuser) this).getUserId());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		} else if (getId() != null) {
			File file = Common.getFileLocation(this, getId().toString());
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				boolean hapus = file.delete();
				System.out.println("Hapus " + file.getAbsolutePath() + " - " + hapus);
			}
		}
	}

	/**
	 * Pintasan {@link #write(Integer, String...)} dengan {@code indexke = 0}.
	 *
	 * @param strings daftar nama properti entity yang ikut diserialkan ke cache JSON
	 * @return berkas cache yang ditulis, atau berkas penanda lokasi bila penulisan dilewati
	 * @see #write(Integer, String...)
	 */
	public File write(String... strings) {
		Integer indexke = 0;
		return write(indexke, strings);
	}

	/**
	 * Menulis snapshot JSON object ini ke berkas cache sementara.
	 *
	 * <p>Isi JSON dibentuk {@code Common.convertToJsonObject(indexke, this, strings)} lalu disimpan
	 * lewat {@code Common.setJSONTemporary(...)}. Parameter {@code indexke} berperan sebagai
	 * <b>penghitung kedalaman rekursi</b> saat relasi antar entity ikut diserialkan — ia dipakai
	 * sebagai penjaga agar penulisan tidak berputar tanpa henti.</p>
	 *
	 * <p>Kapan penulisan benar-benar terjadi (perhatikan, aturannya berlapis):</p>
	 * <ul>
	 *   <li>Untuk sekelompok entity e-learning/akademik tertentu — {@code PertemuanPunyaUjian},
	 *   {@code Perkuliahan}, {@code Skripsi}, {@code MahasiswaRequestTugasAkhir},
	 *   {@code JadwalUjianPMB}, {@code PengumumanAkademis}, {@code Ujian} — penulisan <b>selalu</b>
	 *   dilakukan (asal {@code id} ada), tanpa batas {@code indexke}. Daftar ini adalah keputusan
	 *   historis: entity-entity inilah yang cache-nya memang dibutuhkan.</li>
	 *   <li>Untuk entity lain, penulisan hanya terjadi bila kelasnya <b>tidak</b> terdaftar di
	 *   cache konstanta ({@code !ConstantValues.classExist(...)}) — entity yang sudah ter-cache
	 *   penuh di memori tidak perlu cache berkas — <b>dan</b> {@code indexke < 30}, yaitu batas
	 *   kedalaman rekursi.</li>
	 * </ul>
	 *
	 * <p>Bila tidak satu pun syarat terpenuhi, tidak ada berkas yang ditulis dan method tetap
	 * mengembalikan sebuah {@link File} — yakni penunjuk lokasi
	 * {@code <lokasiTemp> + namaKelasLengkap} — sehingga <b>nilai balik yang tidak {@code null}
	 * bukan jaminan bahwa penulisan terjadi</b>. Berkas itu juga belum tentu ada di disk.</p>
	 *
	 * <p>Kegagalan penulisan ditelan dan dicatat ke audit error; cache tidak boleh menggagalkan
	 * alur pemanggil.</p>
	 *
	 * @param indexke kedalaman rekursi saat ini; {@code 0} untuk pemanggilan dari luar
	 * @param strings daftar nama properti entity yang ikut diserialkan
	 * @return berkas cache hasil penulisan, atau berkas penunjuk lokasi bila penulisan dilewati
	 * @see #read()
	 * @see #delete()
	 */
	public File write(Integer indexke, String... strings) {
		if (((this instanceof PertemuanPunyaUjian) || (this instanceof Perkuliahan) || (this instanceof Skripsi)
				|| (this instanceof MahasiswaRequestTugasAkhir) || (this instanceof JadwalUjianPMB)
				|| (this instanceof PengumumanAkademis) || (this instanceof Ujian)) && getId() != null) {
			try {
				return Common.setJSONTemporary(this, getId().toString(),
						Common.convertToJsonObject(indexke, this, strings));
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:552");
			}
		} else if (!ConstantValues.classExist(this.getClass())) {
			if (this instanceof Tbmrole && ((Tbmrole) this).getRoleId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, ((Tbmrole) this).getRoleId(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:560");
				}
			} else if (this instanceof Tbmuser && ((Tbmuser) this).getUserId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, ((Tbmuser) this).getUserId(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:567");
				}
			} else if (getId() != null && indexke < 30) {
				try {
					return Common.setJSONTemporary(this, getId().toString(),
							Common.convertToJsonObject(indexke, this, strings));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:574");
				}
			}
		}

		String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(this.getClass());

		return new File(lokasiFileTemprorary + this.getClass().getName());
	}

	/**
	 * Pintasan {@link #udah(String)} dengan sufiks kosong.
	 *
	 * @return {@code true} bila penanda sudah pernah dipasang sebelumnya
	 * @see #udah(String)
	 */
	public boolean udah() {
		return udah("");
	}

	/**
	 * Pintasan {@link #belum(String)} dengan sufiks kosong.
	 *
	 * @see #belum(String)
	 */
	public void belum() {
		belum("");
	}

	/**
	 * Penanda "sudah pernah dikerjakan" bergaya <b>test-and-set</b> berbasis berkas.
	 *
	 * <p>Membaca berkas penanda milik object ini. Bila berkas kosong/belum ada, method
	 * <b>menuliskan penanda</b> (isi {@code "true"}) lalu mengembalikan {@code false} — artinya
	 * "belum pernah, dan sekarang sudah ditandai". Bila berkas sudah berisi, method mengembalikan
	 * {@code true} tanpa mengubah apa pun.</p>
	 *
	 * <p>Karena itu pola pemakaian standarnya adalah menjalankan pekerjaan mahal sekali saja:</p>
	 * <pre>{@code
	 * if (refresh || !udah("IsiAngketParameterUmum")) {
	 *     reInitIsiAngketParameterUmum(session);   // hanya sekali
	 * }
	 * }</pre>
	 * <p>Pasangan penghapus penandanya adalah {@link #belum(String)}.</p>
	 *
	 * <p><b>Perilaku saat gagal:</b> exception apa pun (mis. berkas tidak bisa dibaca/ditulis)
	 * dicatat ke audit lalu method mengembalikan {@code true}, yaitu "anggap sudah pernah". Sikap
	 * ini menghindari pengulangan pekerjaan berat tanpa henti ketika I/O bermasalah, dengan
	 * konsekuensi inisialisasi ulang bisa terlewat — pakai parameter {@code refresh} pada pemanggil
	 * bila hasilnya harus dipaksa segar.</p>
	 *
	 * <p><b>Bukan penguncian antar-thread:</b> baca dan tulis di sini tidak atomik, sehingga dua
	 * thread yang bersamaan bisa sama-sama menerima {@code false}.</p>
	 *
	 * @param tambahan sufiks pembeda penanda, agar satu entity bisa punya banyak penanda berbeda
	 * @return {@code true} bila penanda sudah ada sebelumnya (atau terjadi kegagalan I/O);
	 *         {@code false} bila penanda baru saja dipasang oleh pemanggilan ini
	 * @see #belum(String)
	 */
	public boolean udah(String tambahan) {
		try {

			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			File file = Common.getFileLocation(this, getClass().getName() + "_" + tambahan + "udah_" + id);
			String data = ais.common.BacaTulisUtil.baca(file);
			if (data == null || data.trim().isEmpty()) {
				ais.common.BacaTulisUtil.tulis(file, "true");
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:610");
		}
		return true;
	}

	/**
	 * Menghapus penanda yang dipasang {@link #udah(String)}, sehingga pekerjaan bertanda tersebut
	 * akan dikerjakan lagi pada kesempatan berikutnya (invalidasi cache).
	 *
	 * <p>Berkas penanda dihapus lewat {@code BacaTulisUtil.hapus(file)} disusul
	 * {@code File.delete()}. Aman dipanggil walau penandanya memang tidak ada; kegagalan dicatat ke
	 * audit dan tidak dilempar ke pemanggil.</p>
	 *
	 * @param tambahan sufiks pembeda penanda; harus sama persis dengan yang dipakai
	 *                 {@link #udah(String)}
	 * @see #udah(String)
	 */
	public void belum(String tambahan) {
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			File file = Common.getFileLocation(this, getClass().getName() + "_" + tambahan + "udah_" + id);
			if (file != null && file.exists()) {
				BacaTulisUtil.hapus(file);
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:629");
		}
	}

	/**
	 * Pintasan {@link #put(String, String)} dengan sufiks kosong.
	 *
	 * @param data isi yang disimpan; {@code null} berarti hapus
	 * @see #put(String, String)
	 */
	public void put(String data) {
		put(data, "");
	}

	/**
	 * Menyimpan satu nilai teks bebas milik object ini ke berkas cache, sekaligus menyegarkan cache
	 * per-instance {@link #retreive(String)}.
	 *
	 * <p>Pasangan tulis untuk {@link #retreive(String)}. Nilai {@code null} diperlakukan sebagai
	 * <b>penghapusan</b>: berkas ditulis kosong lalu dihapus, dan entri cache in-memory disetel
	 * menjadi {@code ""}. Nilai lain ditulis apa adanya.</p>
	 *
	 * <p><b>Penjaga startup (jangan dihapus).</b> Bila
	 * {@code AppStartupListener.isStartupInProgress()} bernilai {@code true}, method langsung
	 * {@code return} tanpa menulis apa pun. Alasannya nyata dan pernah menyebabkan insiden:
	 * Hibernate memanggil setter ter-map saat hidrasi entity
	 * ({@code TwoPhaseLoad.initializeEntity}), mis. {@code Mahasiswa.setBatasStudi} yang di
	 * dalamnya memanggil {@code put(...)}. Memuat satu entity berarti satu operasi tulis-berkas;
	 * saat init memuat RIBUAN entity, jadilah ribuan tulis-berkas yang membuat startup macet di
	 * thread {@code "main"} (terbukti lewat thread dump). Mirror berkas memang tidak perlu
	 * diperbarui saat load karena kolom database tetap menjadi fallback getter. Setelah startup
	 * selesai, perilaku tulis kembali normal.</p>
	 *
	 * <p>Kunci berkas mengikuti pola cache di kelas ini ({@link Tbmuser} &rarr; {@code userId},
	 * {@link Tbmrole} &rarr; {@code roleId}, selain itu {@link #getId()}); entity tanpa identifier
	 * dilewati. Kegagalan I/O ditelan dan dicatat ke audit error.</p>
	 *
	 * @param data     isi yang disimpan; {@code null} berarti hapus berkas dan kosongkan cache
	 * @param tambahan sufiks pembeda, agar satu entity bisa menyimpan beberapa nilai berbeda
	 * @see #retreive(String)
	 */
	public void put(String data, String tambahan) {
		// Saat STARTUP: LEWATI tulis-file. Hibernate memanggil SETTER ter-map saat HYDRATION
		// (TwoPhaseLoad.initializeEntity), mis. Mahasiswa.setBatasStudi -> put(...). Memuat
		// 1 entity = 1 tulis-file; saat init memuat RIBUAN entity -> ribuan tulis-file
		// (penyebab startup macet di thread "main", lihat thread dump). Mirror file tidak
		// perlu diperbarui saat load (kolom DB tetap jadi fallback getter). Setelah startup,
		// perilaku tulis normal kembali.
		if (ais.common.AppStartupListener.isStartupInProgress()) {
			return;
		}
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (id != null && !id.isEmpty()) {
				File file = Common.getFileLocation(this, getClass().getName() + "_data_" + tambahan + id);
				if (data == null) {
					ais.common.BacaTulisUtil.tulis(file, "");
					ais.common.BacaTulisUtil.hapus(file);
				} else {
					ais.common.BacaTulisUtil.tulis(file, data);
				}
				// jaga cache retreive tetap konsisten dengan tulisan terbaru
				retreiveCache().put(id + "|" + tambahan, data == null ? "" : data);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:666");
		}
	}

	/**
	 * Akumulator batch JSON bersama untuk {@link #putBaru(String, String)} dan
	 * {@link #tulisPutBaru(String)}. Kunci map berbentuk
	 * {@code "<id>_<NamaKelasSederhana>_<tambahan>"}.
	 *
	 * <h4>THREAD-SAFETY — riwayat bug nyata, jangan disederhanakan</h4>
	 * <p>(Lihat audit {@code ConcurrentModificationException} &amp; "Unterminated string" pada
	 * KRS/e-learning; {@code GeneralValueObject.putBaru}/{@code tulisPutBaru} dipanggil dari
	 * {@code AuditListener.prosesUntukElearning}.)</p>
	 *
	 * <p>Map ini di-share OLEH SELURUH THREAD/REQUEST dalam satu JVM (bukan per-request), dan itu
	 * <b>disengaja</b> — ia dipakai sebagai akumulator "batch" ketika satu proses sinkronisasi
	 * (mis. {@code Dosen.singkronkanKrsMahasiswa}) memanggil {@code putBaru()} berkali-kali untuk
	 * entity yang sama (dosen/mahasiswa) SEBELUM {@code tulisPutBaru()} dipanggil sekali di akhir
	 * (lihat {@code voMahasiswaDosens} di {@code Dosen.java}).</p>
	 *
	 * <p>Pada implementasi lama map &amp; {@link JSONObject} di dalamnya bukan thread-safe
	 * ({@code HashMap} biasa), sehingga DUA request BERBEDA yang kebetulan menyentuh
	 * dosen/mahasiswa YANG SAMA secara bersamaan bisa saling mem-mutasi &amp; men-serialize
	 * {@code JSONObject} yang SAMA di saat bersamaan. Akibatnya:</p>
	 * <ul>
	 *   <li>{@code ConcurrentModificationException} saat {@code toString()} meng-iterasi
	 *   {@code HashMap} yang sedang dimutasi thread lain, ATAU</li>
	 *   <li>berkas {@code .json} di disk tertimpa dua tulisan yang beririsan (masing-masing dari
	 *   {@code FileUtils.writeStringToFile} terpisah tanpa penguncian) sehingga isinya terpotong di
	 *   tengah string ("Unterminated string ...") saat dibaca ulang oleh {@code putBaru()}.</li>
	 * </ul>
	 *
	 * <p><b>Perbaikannya:</b> {@code ConcurrentHashMap} (agar operasi {@code get}/{@code put}/
	 * {@code remove} pada map sendiri aman) DITAMBAH {@code synchronized (key.intern())} di kedua
	 * method, supaya seluruh rangkaian get-or-create + mutasi + serialize + tulis-berkas untuk SATU
	 * key ({@code id}+kelas+{@code tambahan}) tidak pernah tumpang tindih antar-thread, sementara
	 * key BERBEDA (entity lain) tetap berjalan paralel tanpa saling menunggu. {@code ConcurrentHashMap}
	 * TIDAK mengizinkan value {@code null}, sehingga "penanda sudah ditulis" di
	 * {@code tulisPutBaru()} memakai {@code remove(key)}, bukan {@code put(key, null)} seperti
	 * sebelumnya.</p>
	 *
	 * @see #putBaru(String, String)
	 * @see #tulisPutBaru(String)
	 */
	private static final Map<String, JSONObject> datatemporary = new java.util.concurrent.ConcurrentHashMap<String, JSONObject>();

	/**
	 * Menambahkan satu entri ke akumulator batch JSON milik object ini (belum menyentuh disk untuk
	 * menulis hasil akhir).
	 *
	 * <p>Alurnya, seluruhnya di dalam {@code synchronized (key.intern())}: ambil {@link JSONObject}
	 * milik key dari {@link #datatemporary}; bila belum ada, muat dari berkas cache yang tersimpan
	 * (berkas rusak/kosong menghasilkan {@code JSONObject} baru dan dicatat ke audit), simpan ke
	 * map; lalu pasang {@code data} sebagai <b>kunci</b> JSON dengan nilai string kosong. Perhatikan
	 * itu: struktur yang dibangun adalah <b>himpunan kunci</b>, bukan pasangan kunci-nilai, sehingga
	 * pemanggilan berulang dengan {@code data} sama bersifat idempoten.</p>
	 *
	 * <p>Hasil akumulasi baru ditulis ke disk oleh {@link #tulisPutBaru(String)} — biasanya sekali
	 * di akhir proses sinkronisasi. Pembacaan kembali daftar kuncinya dilakukan
	 * {@link #retreiveAll(String)}.</p>
	 *
	 * <p>Penjelasan lengkap mengapa penguncian per-key diperlukan ada di
	 * {@link #datatemporary}.</p>
	 *
	 * @param data     nilai yang dicatat; menjadi kunci di dalam JSON batch
	 * @param tambahan sufiks pembeda berkas/batch
	 * @see #tulisPutBaru(String)
	 * @see #retreiveAll(String)
	 */
	public void putBaru(String data, String tambahan) {
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {

				String key = id + "_" + getClass().getSimpleName() + "_" + tambahan;
				synchronized (key.intern()) {
					JSONObject jsonObject = datatemporary.get(key);
					if (jsonObject == null) {
						try {
							File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
							String fileContent = ais.common.BacaTulisUtil.baca(file);
							jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
									: new JSONObject(fileContent);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:691");
							jsonObject = new JSONObject();
						}
						datatemporary.put(key, jsonObject);
					}
					jsonObject.put(data, "");
				}

			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:700");
		}
	}

	/**
	 * Menuliskan akumulator batch {@link #putBaru(String, String)} ke berkas cache, lalu
	 * mengosongkan entri batch tersebut dari memori.
	 *
	 * <p>Dijalankan di dalam {@code synchronized (key.intern())} yang sama dengan
	 * {@code putBaru()}, sehingga serialisasi dan penulisan berkas untuk satu key tidak pernah
	 * beririsan dengan penambahan entri dari thread lain. Setelah berhasil ditulis, entri dihapus
	 * dari {@link #datatemporary} memakai {@code remove(key)} — bukan {@code put(key, null)},
	 * karena {@code ConcurrentHashMap} tidak menerima nilai {@code null}.</p>
	 *
	 * <p>Bila tidak ada batch untuk key tersebut (belum pernah {@code putBaru()}, atau sudah
	 * ditulis sebelumnya), method tidak melakukan apa pun. Kegagalan penulisan ditelan dan dicatat
	 * ke audit; entri batch <b>tetap ada di memori</b> bila penulisan gagal, sehingga percobaan
	 * berikutnya masih membawa data yang sama.</p>
	 *
	 * @param tambahan sufiks pembeda berkas/batch; harus sama dengan yang dipakai
	 *                 {@link #putBaru(String, String)}
	 * @see #putBaru(String, String)
	 * @see #bersihkanPutBaru(String)
	 */
	public void tulisPutBaru(String tambahan) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		} else if (this instanceof Tbmrole) {
			id = ((Tbmrole) this).getRoleId();
		}
		if (!id.isEmpty()) {
			String key = id + "_" + getClass().getSimpleName() + "_" + tambahan;
			synchronized (key.intern()) {
				JSONObject jsonObject = datatemporary.get(key);
				if (jsonObject != null) {
					try {
						File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
						ais.common.BacaTulisUtil.tulis(file, jsonObject.toString());
						datatemporary.remove(key);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:719");
//						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Mengosongkan berkas batch di disk dengan menimpanya memakai JSON kosong ({@code "{}"}).
	 *
	 * <p>Berbeda dari {@link #tulisPutBaru(String)}, method ini <b>tidak</b> menyentuh akumulator
	 * di memori {@link #datatemporary} dan <b>tidak</b> memakai penguncian per-key. Jadi bila masih
	 * ada batch yang belum ditulis untuk key yang sama, pemanggilan {@code tulisPutBaru()}
	 * berikutnya akan menimpa berkas kosong ini kembali. Gunakan untuk memulai ulang pengumpulan
	 * data dari nol, bukan sebagai pembatalan batch yang sedang berjalan.</p>
	 *
	 * <p>Entity tanpa identifier dilewati. Kegagalan penulisan tidak ditangani di sini dan menjadi
	 * urusan {@code BacaTulisUtil.tulis}.</p>
	 *
	 * <p><b>Catatan:</b> per penelusuran 2 Sep 2026 tidak ditemukan pemanggil aktif method ini di
	 * pohon sumber; tampaknya tersisa sebagai utilitas pemeliharaan/perbaikan data manual.</p>
	 *
	 * @param tambahan sufiks pembeda berkas/batch
	 * @see #putBaru(String, String)
	 * @see #tulisPutBaru(String)
	 */
	public void bersihkanPutBaru(String tambahan) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		} else if (this instanceof Tbmrole) {
			id = ((Tbmrole) this).getRoleId();
		}
		if (!id.isEmpty()) {
			File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
			ais.common.BacaTulisUtil.tulis(file, new JSONObject().toString());
		}
	}

	/**
	 * Pintasan {@link #retreiveBaru(String)} dengan sufiks kosong.
	 *
	 * @return nilai yang tersimpan pada berkas batch di bawah kunci {@code id} entity ini, atau
	 *         {@code ""}
	 * @see #retreiveBaru(String)
	 */
	public String retreiveBaru() {
		return retreiveBaru("");
	}

	/**
	 * Membaca berkas batch {@code _put} milik object ini dan mengambil nilai yang tersimpan di
	 * bawah kunci berupa {@code id} entity ini sendiri.
	 *
	 * <p>Berbeda dengan {@link #retreiveAll(String)} yang mengembalikan seluruh <b>kunci</b> pada
	 * berkas batch, method ini mengambil satu <b>nilai</b> pada kunci {@code id}. Perlu dicatat
	 * bahwa {@link #putBaru(String, String)} menyimpan data sebagai kunci dengan nilai string
	 * kosong, sehingga untuk berkas yang diisi lewat {@code putBaru()} method ini secara praktis
	 * akan mengembalikan {@code ""}. Pasangan penulis yang cocok untuknya tampaknya berasal dari
	 * alur lama yang menyimpan pasangan kunci-nilai; perlakukan sebagai peninggalan historis dan
	 * jangan dijadikan contoh untuk kode baru.</p>
	 *
	 * <p>Per penelusuran 2 Sep 2026 tidak ditemukan pemanggil aktif method ini di pohon sumber.</p>
	 *
	 * <p>Berkas yang kosong maupun rusak diperlakukan sebagai JSON kosong (kegagalan parsing
	 * dicatat ke audit), dan entity tanpa identifier langsung menghasilkan {@code ""}.</p>
	 *
	 * @param tambahan sufiks pembeda berkas/batch
	 * @return nilai yang tersimpan, atau {@code ""} bila tidak ada; tidak pernah {@code null}
	 * @see #retreiveAll(String)
	 */
	public String retreiveBaru(String tambahan) {
		String data = "";
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {
				File file = Common.getFileLocation(this, getClass().getSimpleName() + "_" + tambahan + "_put");
				String fileContent = ais.common.BacaTulisUtil.baca(file);
				JSONObject jsonObject;
				try {
					jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
							: new JSONObject(fileContent);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:760");
					jsonObject = new JSONObject();
				}
				data = jsonObject.isNull(id) ? "" : jsonObject.getString(id);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:766");
		}
		return data;
	}

	/**
	 * Pintasan {@link #retreive(String)} dengan sufiks kosong.
	 *
	 * @return isi cache milik object ini, atau {@code ""}
	 * @see #retreive(String)
	 */
	public String retreive() {
		return retreive("");
	}

	/**
	 * Membaca kembali nilai teks yang disimpan {@link #put(String, String)}, dengan
	 * <b>cache per-instance</b> dan penjaga startup.
	 *
	 * <h4>Kenapa ada cache per-instance</h4>
	 * <p>Sebelum cache ini ada, getter Hibernate (mis. {@code Dosen.getIdfinger} yang memanggil
	 * {@code retreive("idfinger")}) memicu I/O berkas SETIAP auto-flush untuk SETIAP entity yang
	 * kotor, sehingga startup dan operasi normal melambat parah — thread dump menunjukkan thread
	 * {@code "main"} macet di {@code File.exists} saat inisialisasi
	 * {@code AppStartupListener}. Dengan cache, nilai yang dikembalikan TETAP SAMA, hanya saja
	 * tidak dibaca ulang dari disk pada pemanggilan berikutnya. Cache-nya dipegang
	 * {@link #retreiveCache()} dan bersifat {@code transient} (tidak ikut terserialisasi), serta
	 * disinkronkan oleh {@link #put(String, String)} setiap kali menulis.</p>
	 *
	 * <h4>Penjaga startup</h4>
	 * <p>Selama {@code AppStartupListener.isStartupInProgress()} bernilai {@code true}, method
	 * langsung mengembalikan {@code ""} tanpa menyentuh disk (nilai berkas tidak dibutuhkan untuk
	 * dirty-check auto-flush Hibernate). Nilai kosong itu <b>sengaja tidak disimpan ke cache</b>,
	 * supaya setelah startup selesai nilai aslinya tetap terbaca.</p>
	 *
	 * <p>Entity tanpa identifier mengembalikan {@code ""}. Kegagalan membaca identifier maupun
	 * berkas ditelan dan dicatat ke audit; nilai {@code null} dari berkas dinormalkan menjadi
	 * {@code ""} sebelum disimpan ke cache.</p>
	 *
	 * @param tambahan sufiks pembeda; harus sama dengan yang dipakai {@link #put(String, String)}
	 * @return isi yang tersimpan, atau {@code ""}; tidak pernah {@code null}
	 * @see #put(String, String)
	 */
	public String retreive(String tambahan) {
		// CACHE per-instance: hindari File.exists/baca berulang. Sebelumnya getter Hibernate
		// (mis. Dosen.getIdfinger -> retreive("idfinger")) memicu I/O file SETIAP auto-flush
		// untuk SETIAP entity dirty -> startup & operasi normal melambat parah (thread dump:
		// main macet di File.exists saat AppStartupListener init). Nilai yang dikembalikan
		// TETAP SAMA, hanya tidak membaca ulang dari disk pada pemanggilan berikutnya.
		String id = "";
		try {
			id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:789");
		}
		if (id == null || id.isEmpty()) {
			return "";
		}
		String cacheKey = id + "|" + tambahan;
		java.util.Map<String, String> cache = retreiveCache();
		String cached = cache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		// Saat STARTUP: lewati I/O file (nilai file tidak dibutuhkan untuk dirty-check
		// auto-flush Hibernate). JANGAN cache "" agar setelah startup nilai asli tetap terbaca.
		if (ais.common.AppStartupListener.isStartupInProgress()) {
			return "";
		}
		String data = "";
		try {
			File file = Common.getFileLocation(this, getClass().getName() + "_data_" + tambahan + id);
			data = ais.common.BacaTulisUtil.baca(file);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:809");
//			e.printStackTrace();
		}
		if (data == null) {
			data = "";
		}
		cache.put(cacheKey, data);
		return data;
	}

	/** Cache lazy per-instance untuk {@link #retreive(String)} (transient: tidak dipersist). */
	private transient java.util.Map<String, String> retreiveCacheMap;

	/**
	 * Mengembalikan cache per-instance untuk {@link #retreive(String)}, membuatnya sekali secara
	 * malas (lazy).
	 *
	 * <p>Memakai <i>double-checked locking</i> dengan {@code synchronized (this)}: dibaca dulu
	 * tanpa kunci, dan hanya bila masih {@code null} barulah kunci diambil dan pemeriksaan diulang.
	 * Map yang dibuat bertipe {@code ConcurrentHashMap} sehingga aman dipakai bersamaan setelah
	 * terbentuk. Karena {@code retreiveCacheMap} dideklarasikan {@code transient} (tidak
	 * {@code volatile}), pola ini secara teori masih menyisakan celah publikasi tak-aman antar
	 * thread pada JMM; risikonya dinilai kecil dan tampaknya diterima karena isinya sekadar cache
	 * yang bisa dibangun ulang.</p>
	 *
	 * <p>Cache bersifat per-object dan tidak ikut terserialisasi, jadi salinan hasil deserialisasi
	 * akan membangun cache-nya sendiri dari berkas.</p>
	 *
	 * @return map cache milik instance ini; tidak pernah {@code null}
	 */
	private java.util.Map<String, String> retreiveCache() {
		java.util.Map<String, String> m = retreiveCacheMap;
		if (m == null) {
			synchronized (this) {
				m = retreiveCacheMap;
				if (m == null) {
					m = new java.util.concurrent.ConcurrentHashMap<String, String>();
					retreiveCacheMap = m;
				}
			}
		}
		return m;
	}
	
	/**
	 * Mengembalikan seluruh nilai yang pernah dicatat lewat {@link #putBaru(String, String)} untuk
	 * sufiks tertentu, dengan <b>migrasi otomatis</b> dari penyimpanan gaya lama bila perlu.
	 *
	 * <p>Dipakai luas oleh entity besar untuk mendapatkan daftar id relasi tanpa query, misalnya
	 * {@code Dosen.retreiveAll(Skripsi.class.getName())} dan padanannya di {@code Mahasiswa}.
	 * Nilai balik biasanya berupa daftar id dalam bentuk String yang lalu di-{@code parse} oleh
	 * pemanggil.</p>
	 *
	 * <h4>Dua jalur</h4>
	 * <ol>
	 *   <li><b>Jalur cepat</b> — bila berkas batch {@code _put.json} sudah berisi, atau penanda
	 *   {@code _put_udah.json} sudah ada, seluruh <b>kunci</b> JSON dikembalikan apa adanya.</li>
	 *   <li><b>Jalur migrasi</b> — bila belum, data dikumpulkan dari penyimpanan gaya lama
	 *   (satu berkas per nilai), setiap nilai ditulis ulang ke batch lewat
	 *   {@code putBaru(...)}, dan di akhir penanda {@code _put_udah.json} diisi {@code "1"} agar
	 *   pemanggilan berikutnya memakai jalur cepat. Sumber datanya sendiri bercabang menurut flag
	 *   {@code BacaTulisUtil.flagDataMenggunakandatabase}: bila {@code true}, dibaca lewat query
	 *   {@code ambilSemuaValueDenganPrefixFile(...)}; bila {@code false} (bawaan), folder induk
	 *   di-{@code listFiles()} dan setiap berkas berawalan kunci dibaca satu per satu.</li>
	 * </ol>
	 *
	 * <p>Nilai yang berakhiran {@code json} dianggap berupa path berkas dan dipangkas menjadi
	 * segmen terakhirnya tanpa ekstensi (pemisah {@code \} dan {@code /} dinormalkan menjadi
	 * {@code _} lebih dulu).</p>
	 *
	 * <p><b>Penjagaan yang sudah ada di kode dan jangan dihapus:</b> kunci kosong/blank tidak
	 * pernah diteruskan ke hasil — ini memperbaiki {@code NumberFormatException} di pemanggil yang
	 * langsung melakukan {@code Long.parseLong(s)} (mis.
	 * {@code Dosen.ambilPerkuliahanDanParalel}) — dan hasil {@code listFiles()} yang {@code null}
	 * (folder tidak dapat diakses OS) ditangani agar tidak menimbulkan
	 * {@code NullPointerException}.</p>
	 *
	 * <p>Seluruh kegagalan ditelan dan dicatat ke audit; method mengembalikan daftar sejauh yang
	 * berhasil dikumpulkan.</p>
	 *
	 * @param tambahan sufiks pembeda berkas/batch, umumnya nama kelas relasi yang didaftar
	 * @return daftar nilai; kosong bila entity belum punya identifier atau tidak ada data
	 * @see #putBaru(String, String)
	 * @see #tulisPutBaru(String)
	 */
	@SuppressWarnings("unchecked")
	public List<String> retreiveAll(String tambahan) {
		List<String> strings = new ArrayList<String>();
		try {
			String id = getId() == null ? "" : getId().toString();
			if (this instanceof Tbmuser) {
				id = ((Tbmuser) this).getUserId();
			} else if (this instanceof Tbmrole) {
				id = ((Tbmrole) this).getRoleId();
			}
			if (!id.isEmpty()) {

				String lokasiFileTemprorary = ConstantValues.ambilLokasiFileTemprorary(this.getClass());

				String fileLocation = lokasiFileTemprorary + this.getClass().getSimpleName() + "/" + this.getId() + "/"
						+ getClass().getSimpleName() + "_" + tambahan + "_put.json";
				File fileBaru = new File(fileLocation);

				String fileLocationUdah = lokasiFileTemprorary + this.getClass().getSimpleName() + "/" + this.getId()
						+ "/" + getClass().getSimpleName() + "_" + tambahan + "_put_udah.json";
				File fileBaruUdah = new File(fileLocationUdah);

				JSONObject jsonObject;
				try {
					String fileContent = ais.common.BacaTulisUtil.baca(fileBaru);
					jsonObject = fileContent == null || fileContent.trim().isEmpty() ? new JSONObject()
							: new JSONObject(fileContent);
				} catch (Exception e) {
					jsonObject = new JSONObject();
				}

				int size = jsonObject.length();
				
				// Memastikan flag DB ikut membaca dari Database, bukan cuma eksistensi file fisik
				boolean isUdah = fileBaruUdah.exists() || "1".equals(ais.common.BacaTulisUtil.baca(fileBaruUdah));

				if (size > 0 || isUdah) {
					Iterator<String> keys = jsonObject.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						// FIX NumberFormatException di caller (mis. Dosen.ambilPerkuliahanDanParalel
						// yang langsung Long.parseLong(s)): key kosong/blank tidak boleh diteruskan,
						// konsisten dengan guard !data.trim().isEmpty() pada 2 cabang lain method ini.
						if (key != null && !key.trim().isEmpty()) {
							strings.add(key);
						}
					}
					jsonObject = null;
				} else {
					String key = getClass().getName() + "_data_" + tambahan;
					File file = Common.getFileLocation(this, key + id);
					File parentFolder = file.getParentFile();
					
					// =========================================================================
					// CEK FLAG: DATABASE vs CARA LAMA (FILE SYSTEM)
					// =========================================================================
					if (ais.common.BacaTulisUtil.flagDataMenggunakandatabase) {
						
						// CARA BARU (Memanggil Query dari Database melalui BacaTulisUtil)
						List<String> dataList = ais.common.BacaTulisUtil.ambilSemuaValueDenganPrefixFile(parentFolder, key);
						for (String data : dataList) {
							try {
								if (data != null && !data.trim().isEmpty()) {
									if (data.endsWith("json")) {
										String[] argv = org.apache.commons.lang.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(data, "\\", "_"), "/", "_").split("_");
										data = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
									}
									putBaru(data, tambahan);
									strings.add(data);
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:902");
							}
						}
						
					} else {
						
						// CARA LAMA (Murni membaca dari File System)
						if (parentFolder != null && parentFolder.exists() && parentFolder.isDirectory()) {
							File[] files = parentFolder.listFiles();
							
							// Proteksi NullPointerException jika folder tidak dapat diakses OS
							if (files != null) {
								for (File s : files) {
									String data = "";
									try {
										if (s.getName().toLowerCase().startsWith(key.toLowerCase())) {
											data = ais.common.BacaTulisUtil.baca(s);
											if (!data.trim().isEmpty()) {
												if (data.endsWith("json")) {
													String[] argv = org.apache.commons.lang.StringUtils.replace(org.apache.commons.lang3.StringUtils.replace(data, "\\", "_"), "/", "_").split("_");
													data = org.apache.commons.lang3.StringUtils.replace(argv[argv.length - 1], ".json", "").trim();
												}
												putBaru(data, tambahan);
												strings.add(data);
											}
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:929");
									}
								}
							}
						}
						
					}
					
					ais.common.BacaTulisUtil.tulis(fileBaruUdah, "1");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:941");
		}
		return strings;
	}
	
	
	/**
	 * Mencatat jejak aktivitas berjenis {@code jenis} atas entity ini oleh <b>pengguna yang sedang
	 * login</b> ({@code Common.getCurrentUser()}).
	 *
	 * <p><b>Jangan tertukar</b> dengan {@code DataUtil.masukkanData(Class, GeneralValueObject)}
	 * yang statis dan mengisi cache entity — kelompok {@code masukkanData}/{@code ambilData}
	 * di kelas ini adalah method instance untuk jejak aktivitas berbasis berkas.</p>
	 *
	 * @param jenis label jenis aktivitas (mis. penanda "sedang membuka"/"sedang mengerjakan")
	 * @see #masukkanData(String, Tbmuser)
	 * @see #ambilData(String, String)
	 */
	public void masukkanData(String jenis) {
		Tbmuser tbmuser = Common.getCurrentUser();
		masukkanData(jenis, tbmuser);
	}

	/**
	 * Mencatat jejak aktivitas atas entity ini untuk pengguna tertentu, setelah menerjemahkan
	 * {@link Tbmuser} menjadi label pengguna berformat {@code "<nama>-<id>-<Tipe>"}.
	 *
	 * <p>Tipe ditentukan menurut profil yang menempel pada akun, diperiksa berurutan:
	 * {@code CalonSiswa}, {@code CalonMahasiswa}, {@code Mahasiswa}, {@code Dosen}, {@code Siswa},
	 * {@code Guru}, {@code Pegawai}. Bila tidak satu pun cocok, label memakai
	 * {@code "<userNama>-1-<hakAkses>"}. Tanda hubung di dalam nama dibuang lebih dulu agar tidak
	 * merusak pemisahan label saat dibaca kembali oleh {@link #ambilData(String, String, String,
	 * Date, Date, String[])}. Bila {@code tbmuser} bernilai {@code null}, dipakai label
	 * {@code "User"}.</p>
	 *
	 * <p>Stempel waktu diambil dari jam server ({@code WaktuUtil.getDate()}) dan diformat memakai
	 * {@code Common.dateFormat3}.</p>
	 *
	 * <p><b>Catatan pemeliharaan:</b> pada cabang {@code Guru} kondisi yang diperiksa adalah
	 * {@code tbmuser.getSiswa() != null} (sama dengan cabang {@code Siswa} sebelumnya), sehingga
	 * cabang itu tampaknya tidak pernah tercapai dan akun guru akan jatuh ke cabang berikutnya.
	 * Ini terlihat seperti kekeliruan historis; jangan "dirapikan" tanpa memverifikasi dampaknya
	 * pada data jejak yang sudah tersimpan.</p>
	 *
	 * @param jenis   label jenis aktivitas
	 * @param tbmuser akun pelaku; boleh {@code null}
	 * @see #masukkanData(String, String, String)
	 */
	public void masukkanData(String jenis, Tbmuser tbmuser) {

		if (tbmuser != null) {
			String user = tbmuser
					.getCalonSiswa() != null
							? (tbmuser.getCalonSiswa().getNama() == null ? ""
									: tbmuser.getCalonSiswa().getNama().replaceAll("-", "")) + "-"
									+ tbmuser.getCalonSiswa().getId() + "-CalonSiswa"
							: tbmuser
									.getBiodataCalonMahasiswa() != null
											? (tbmuser.getBiodataCalonMahasiswa().getNama() == null ? ""
													: tbmuser.getBiodataCalonMahasiswa().getNama().replaceAll("-", ""))
													+ "-" + tbmuser.getBiodataCalonMahasiswa().getId()
													+ "-CalonMahasiswa"
											: tbmuser
													.getMahasiswa() != null
															? (tbmuser.getMahasiswa().getNama() == null ? ""
																	: tbmuser.getMahasiswa().getNama().replaceAll("-",
																			""))
																	+ "-" + tbmuser.getMahasiswa().getId()
																	+ "-Mahasiswa"
															: (tbmuser.ambilDosen() != null
																	? (tbmuser.ambilDosen().getNama() == null
																			? ""
																			: tbmuser
																					.getDosen().getNama()
																					.replaceAll("-", ""))
																			+ "-" + tbmuser.getDosen().getId()
																			+ "-Dosen"
																	: tbmuser.getSiswa() != null
																			? (tbmuser.getSiswa().getNama() == null
																					? ""
																					: tbmuser
																							.getSiswa().getNama()
																							.replaceAll("-", ""))
																					+ "-" + tbmuser.getSiswa().getId()
																					+ "-Siswa"
																			: tbmuser.getSiswa() != null ? (tbmuser
																					.ambilGuru().getNama() == null
																							? ""
																							: tbmuser.ambilGuru()
																									.getNama()
																									.replaceAll("-",
																											""))
																					+ "-" + tbmuser.getGuru().getId()
																					+ "-Guru"
																					: tbmuser.ambilPegawai() != null
																							? (tbmuser.ambilPegawai()
																									.getNama() == null
																											? ""
																											: tbmuser
																													.getPegawai()
																													.getNama()
																													.replaceAll(
																															"-",
																															""))
																									+ "-"
																									+ tbmuser
																											.getPegawai()
																											.getId()
																									+ "-Pegawai"
																							: tbmuser.getUserNama()
																									+ "-1-" + tbmuser
																											.hakAkses());
//			System.out.println("Masukkan data -> " + user);
			if (!user.isEmpty()) {
				masukkanData(jenis, user, Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
			}
		} else {
			masukkanData(jenis, "User", Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
		}
	}

	/**
	 * Inti penulisan jejak aktivitas: menyisipkan atau memperbarui satu baris pada cache teks milik
	 * entity ini.
	 *
	 * <h4>Format penyimpanan</h4>
	 * <p>Seluruh jejak disimpan sebagai satu String di cache {@link #put(String)}/
	 * {@link #retreive()}, berupa daftar rekaman dipisah titik koma, tiap rekaman berisi tiga
	 * bidang dipisah koma:</p>
	 * <pre>{@code <user>,<jenis>,<jam>;<user>,<jenis>,<jam>;...}</pre>
	 * <p>dengan {@code <user>} sendiri berbentuk {@code "<nama>-<id>-<Tipe>"}. Karena koma menjadi
	 * pemisah bidang, setiap koma pada {@code jenis}, {@code user}, dan {@code jam} lebih dulu
	 * diganti menjadi titik ({@code .}).</p>
	 *
	 * <h4>Perilaku</h4>
	 * <p>Daftar lama dibaca lalu ditulis ulang seluruhnya: rekaman dengan pasangan
	 * {@code user}+{@code jenis} yang sama <b>ditimpa</b> dengan waktu terbaru (jadi tiap pengguna
	 * hanya punya satu rekaman per jenis — ini catatan "terakhir kali", bukan riwayat lengkap);
	 * rekaman lain disalin apa adanya; bila belum ada yang cocok, rekaman baru ditambahkan di
	 * akhir. Hasilnya disimpan lewat {@link #put(String)}, sehingga ikut tunduk pada penjaga
	 * startup di method tersebut.</p>
	 *
	 * <p>Rekaman yang tidak bisa diurai dilewati (kesalahannya dilaporkan lewat
	 * {@code Common.tampilErrorJikaAdmin}). Bila {@code user} atau {@code jam} bernilai
	 * {@code null}, method tidak melakukan apa pun.</p>
	 *
	 * @param jenis label jenis aktivitas
	 * @param user  label pelaku, umumnya {@code "<nama>-<id>-<Tipe>"}
	 * @param jam   waktu aktivitas dalam format {@code Common.dateFormat3}
	 * @see #ambilData(String, String, String, Date, Date, String[])
	 */
	public void masukkanData(String jenis, String user, String jam) {

		try {

			if (user != null && jam != null) {
				jam = org.apache.commons.lang3.StringUtils.replace(jam, ",", ".");
				user = org.apache.commons.lang3.StringUtils.replace(user, ",", ".");
				jenis = org.apache.commons.lang3.StringUtils.replace(jenis, ",", ".");
				String[] nilais = retreive().split(";");
				Boolean ada = false;
				String formatBaru = "";
				for (String nn : nilais) {
					try {
						String aformatBaru = "";
						String[] s = nn.split(",");
						if (!s[0].trim().isEmpty()) {
							String userId = s[0];
							String jenisId = s[1];
							if (user.equalsIgnoreCase(userId) && jenis.equalsIgnoreCase(jenisId)) {
								aformatBaru = user + "," + jenis + "," + jam;
								ada = true;
							} else {
								aformatBaru = nn;
							}
							if (!aformatBaru.trim().isEmpty()) {
								formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				if (!ada) {
					String aformatBaru = user + "," + jenis + "," + jam;
					formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
				}

				// System.out.println("masukkan data => " + formatBaru);

				put(formatBaru);
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1068");
			// TODO: handle exception
		}
	}

	/**
	 * Memeriksa apakah ada setidaknya satu pengguna <b>berperan akademik</b> yang tercatat sedang
	 * melakukan aktivitas berjenis {@code jenis} atas entity ini.
	 *
	 * <p>Mengambil seluruh jejak lewat {@link #ambilData(String, String)} (tanpa penyaringan
	 * pengguna dan tanpa rentang waktu), lalu menghitung rekaman yang bagian tipe-nya (segmen
	 * ketiga label {@code "<nama>-<id>-<Tipe>"}) termasuk {@code Dosen}, {@code Mahasiswa},
	 * {@code CalonMahasiswa}, {@code CalonSiswa}, {@code Guru}, atau {@code Siswa}. Peran lain —
	 * termasuk {@code Pegawai} dan label fallback {@code "User"} — sengaja tidak dihitung.</p>
	 *
	 * <p><b>Perhatian:</b> jejak yang dipakai bersifat "terakhir kali", bukan status hidup dengan
	 * kedaluwarsa. Jadi hasil {@code true} berarti "pernah tercatat", bukan jaminan pengguna masih
	 * aktif saat ini. Pakai varian {@code ambilData} yang menerima rentang waktu bila kesegaran
	 * data penting.</p>
	 *
	 * <p>Label yang tidak bisa diurai dilewati dan dicatat ke audit.</p>
	 *
	 * @param jenis label jenis aktivitas yang diperiksa
	 * @return {@code true} bila ada minimal satu pengguna berperan akademik yang tercatat
	 */
	public boolean apakahSedang(String jenis) {
		int jumlah = 0;
		TreeMap<String, String> d = ambilData(jenis, null);
		for (String user : d.keySet()) {
			try {
				String[] u = user.split("-");
				if (u[2].equalsIgnoreCase("Dosen") || u[2].equalsIgnoreCase("Mahasiswa")
						|| u[2].equalsIgnoreCase("CalonMahasiswa") || u[2].equalsIgnoreCase("CalonSiswa")
						|| u[2].equalsIgnoreCase("Guru") || u[2].equalsIgnoreCase("Siswa")) {
					jumlah++;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1085");
			}
		}
		d = null;
		return jumlah > 0;
	}

	/**
	 * Pintasan {@link #ambilData(String, String, String, Date, Date)} tanpa posfix dan tanpa
	 * rentang waktu.
	 *
	 * @param jenis label jenis aktivitas
	 * @param user  id pengguna yang dicari; {@code null}/kosong berarti semua pengguna
	 * @return peta jejak aktivitas; kosong bila tidak ada yang cocok
	 * @see #ambilData(String, String, String, Date, Date, String[])
	 */
	public TreeMap<String, String> ambilData(String jenis, String user) {
		return ambilData(jenis, user, "", null, null);
	}

	/**
	 * Pintasan {@link #ambilData(String, String, String, Date, Date)} tanpa posfix, dengan
	 * penyaringan rentang waktu.
	 *
	 * @param jenis  label jenis aktivitas
	 * @param user   id pengguna yang dicari; {@code null}/kosong berarti semua pengguna
	 * @param mulai  batas bawah waktu (eksklusif); boleh {@code null}
	 * @param sampai batas atas waktu (eksklusif); boleh {@code null}
	 * @return peta jejak aktivitas; kosong bila tidak ada yang cocok
	 * @see #ambilData(String, String, String, Date, Date, String[])
	 */
	public TreeMap<String, String> ambilData(String jenis, String user, Date mulai, Date sampai) {
		return ambilData(jenis, user, "", mulai, sampai);
	}

	/**
	 * Pintasan {@link #ambilData(String, String, String, Date, Date)} tanpa rentang waktu.
	 *
	 * @param jenis  label jenis aktivitas
	 * @param user   id pengguna yang dicari; {@code null}/kosong berarti semua pengguna
	 * @param posfix akhiran yang ditambahkan pada kunci hasil
	 * @return peta jejak aktivitas; kosong bila tidak ada yang cocok
	 * @see #ambilData(String, String, String, Date, Date, String[])
	 */
	public TreeMap<String, String> ambilData(String jenis, String user, String posfix) {
		return ambilData(jenis, user, posfix, null, null);
	}

	/**
	 * Pintasan {@link #ambilData(String, String, String, Date, Date, String[])} dengan daftar
	 * peran bawaan: {@code Mahasiswa}, {@code CalonMahasiswa}, {@code Dosen}, {@code Siswa},
	 * {@code CalonSiswa}, {@code Guru}.
	 *
	 * <p>Karena daftar bawaan ini tidak memuat {@code Pegawai} maupun label fallback
	 * {@code "User"}, jejak dari peran tersebut <b>tidak</b> ikut terbaca lewat overload ini.
	 * Panggil overload berparameter {@code jenisPengguna} bila peran lain juga dibutuhkan.</p>
	 *
	 * @param jenis  label jenis aktivitas
	 * @param user   id pengguna yang dicari; {@code null}/kosong berarti semua pengguna
	 * @param posfix akhiran yang ditambahkan pada kunci hasil
	 * @param mulai  batas bawah waktu (eksklusif); boleh {@code null}
	 * @param sampai batas atas waktu (eksklusif); boleh {@code null}
	 * @return peta jejak aktivitas; kosong bila tidak ada yang cocok
	 * @see #ambilData(String, String, String, Date, Date, String[])
	 */
	public TreeMap<String, String> ambilData(String jenis, String user, String posfix, Date mulai, Date sampai) {
		return ambilData(jenis, user, posfix, mulai, sampai,
				new String[] { "Mahasiswa", "CalonMahasiswa", "Dosen", "Siswa", "CalonSiswa", "Guru" });
	}

	/**
	 * Membaca dan menyaring jejak aktivitas yang tersimpan oleh
	 * {@link #masukkanData(String, String, String)}.
	 *
	 * <p><b>Jangan tertukar</b> dengan {@code DataUtil.ambilData(Class, String)} yang statis dan
	 * membaca entity dari cache/DB. Method ini instance dan hanya membaca cache teks milik entity
	 * ini.</p>
	 *
	 * <h4>Alur penyaringan (dievaluasi berurutan per rekaman)</h4>
	 * <ol>
	 *   <li>Rekaman dipecah dengan koma; rekaman dengan kurang dari tiga bidang <b>dilewati</b>
	 *   (penjaga terhadap {@code ArrayIndexOutOfBoundsException} — jangan dihapus).</li>
	 *   <li>Bila {@code jenisPengguna} diisi, segmen ketiga label pengguna
	 *   ({@code "<nama>-<id>-<Tipe>"}) harus termasuk daftar itu. Label yang tidak punya segmen
	 *   ketiga dianggap bertipe kosong sehingga tidak akan cocok dengan peran mana pun —
	 *   sekali lagi penjaga yang disengaja, bukan pelemparan exception.</li>
	 *   <li>Bila {@code mulai} dan/atau {@code sampai} diisi, waktu rekaman diurai dengan
	 *   {@code Common.dateFormat3} lalu diuji: keduanya diisi &rarr; harus
	 *   {@code mulai < waktu < sampai}; hanya {@code mulai} &rarr; harus {@code waktu > mulai};
	 *   hanya {@code sampai} &rarr; harus {@code waktu < sampai}. Perbandingannya
	 *   <b>eksklusif</b> di kedua ujung. Waktu yang gagal diurai membuat rekaman tetap lolos
	 *   (exception ditelan dan dicatat).</li>
	 *   <li>Penyaringan jenis: bila {@code user} diisi, jenis dicocokkan dengan
	 *   {@code startsWith(jenis)} (cocok awalan) dan id pengguna — segmen kedua label — harus sama
	 *   persis. Bila {@code user} kosong, jenis harus sama persis
	 *   ({@code equalsIgnoreCase}).</li>
	 * </ol>
	 *
	 * <h4>Bentuk hasil</h4>
	 * <p>{@link TreeMap} (jadi terurut menurut kunci) dengan <b>nilai</b> berupa waktu aktivitas,
	 * dan <b>kunci</b> yang bentuknya berbeda menurut jalur pencocokan: {@code userId + jenisId +
	 * posfix} bila menyaring per pengguna, atau {@code userId + posfix} bila menyaring per jenis.
	 * Karena berupa map, hanya satu rekaman yang bertahan untuk kunci yang sama.</p>
	 *
	 * <p>Kegagalan pada satu rekaman tidak menggagalkan keseluruhan: rekaman bermasalah dilewati
	 * dan dicatat ke audit.</p>
	 *
	 * @param jenis         label jenis aktivitas
	 * @param user          id pengguna yang dicari; {@code null}/kosong berarti semua pengguna
	 * @param posfix        akhiran yang ditambahkan pada kunci hasil
	 * @param mulai         batas bawah waktu (eksklusif); boleh {@code null}
	 * @param sampai        batas atas waktu (eksklusif); boleh {@code null}
	 * @param jenisPengguna daftar peran yang diizinkan; {@code null} berarti tanpa penyaringan peran
	 * @return peta jejak aktivitas hasil penyaringan; kosong bila tidak ada yang cocok
	 * @see #masukkanData(String, String, String)
	 */
	public TreeMap<String, String> ambilData(String jenis, String user, String posfix, Date mulai, Date sampai,
			String[] jenisPengguna) {
		TreeMap<String, String> treeMap = new TreeMap<String, String>();
		try {
			jenis = org.apache.commons.lang3.StringUtils.replace(jenis, ",", ".");
			String[] nilais = retreive().split(";");

			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					// fallback aman: data tidak lengkap (kurang dari 3 bagian setelah split) dilewati,
					// bukan melempar ArrayIndexOutOfBoundsException saat mengakses s[1]/s[2].
					if (s.length > 2 && !s[0].trim().isEmpty()) {
						String userId = s[0];
						String jenisId = s[1];
						String jamId = s[2];

						if (jenisPengguna != null) {
							try {
								boolean ada = false;
								String[] u = userId.split("-");
								// fallback aman: bila userId tidak punya bagian ke-3 (mis. tanpa "-tipe"),
								// anggap tipe kosong (tidak akan cocok dengan jenisPengguna manapun)
								// alih-alih melempar ArrayIndexOutOfBoundsException.
								String tipe = u.length > 2 ? u[2] : "";
								for (String d : jenisPengguna) {
									if (d.trim().equalsIgnoreCase(tipe.trim())) {
										ada = true;
										break;
									}
								}
								if (!ada) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1138");
							}
						}

						if (jamId != null && !jamId.trim().isEmpty() && mulai != null && sampai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = mulai.before(waktu) && sampai.after(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " mulai "
								// + Common.dateFormat3.get().format(mulai) + " s.d " +
								// Common.dateFormat3.get().format(sampai)
								// + ", dibandingkan dengan " + waktu + ", masuk " +
								// masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1155");
							}
						} else if (jamId != null && !jamId.trim().isEmpty() && mulai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = mulai.before(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " mulai "
								// + Common.dateFormat3.get().format(mulai) + ",
								// dibandingkan dengan " + waktu + ", masuk "
								// + masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1169");
							}
						} else if (jamId != null && !jamId.trim().isEmpty() && sampai != null) {
							try {
								Date waktu = Common.dateFormat3.get().parse(jamId);
								boolean masuk = sampai.after(waktu);
								// System.out.println("ambilData " + jenis + " user
								// " + user + " sampai "
								// + Common.dateFormat3.get().format(sampai) + ",
								// dibandingkan dengan " + waktu + ", masuk "
								// + masuk);
								if (!masuk) {
									continue;
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1183");
							}
						}

						if (user != null && !user.trim().isEmpty() && jenisId.startsWith(jenis)) {
							String[] u = userId.split("-");
							try {
								String id = u[1];
								if (id.equalsIgnoreCase(user)) {
									treeMap.put(userId + jenisId + posfix, jamId);
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1194");
							}
						} else if (jenis.equalsIgnoreCase(jenisId)) {
							treeMap.put(userId + posfix, jamId);
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1201");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1205");
		}
		return treeMap;
	}

	/**
	 * Membaca isi indeks JSON checklist hasil penilaian umum milik entity ini.
	 *
	 * <p>Berkas indeks dipisahkan per kombinasi entity + {@code userId} yang dinilai +
	 * {@code pertemuanId}, sehingga satu entity bisa punya banyak indeks sekaligus. Kunci berkas
	 * memakai {@code userId} untuk {@link Tbmuser} dan {@link #getId()} untuk entity lain.</p>
	 *
	 * <p>Isi indeks adalah JSON berbentuk peta {@code id -> id} untuk baris yang aktif, dan
	 * {@code id -> ""} untuk baris yang sudah dibuang oleh
	 * {@link #removeChecklistHasilPenilaianUmum(Serializable, Long, String)}.</p>
	 *
	 * @param pertemuanId id pertemuan; {@code null} untuk indeks yang tidak terikat pertemuan
	 * @param userId      id pengguna yang dinilai; {@code null} untuk indeks umum
	 * @return isi indeks JSON, atau {@code VOMahasiswa.dataJSON} (JSON kosong bawaan) bila berkas
	 *         belum ada/kosong/gagal dibaca
	 */
	public String ambilLokasiChecklistHasilPenilaianUmum(Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		try {
			// System.out.println("baca file dari " + file.getAbsolutePath());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1221");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa isi indeks JSON checklist hasil penilaian umum untuk kombinasi entity +
	 * {@code userId} + {@code pertemuanId} tertentu.
	 *
	 * <p>Penulisan bersifat menimpa penuh, bukan menggabung — pemanggil bertanggung jawab membaca
	 * dulu lewat {@link #ambilLokasiChecklistHasilPenilaianUmum(Long, String)}, mengubah JSON-nya,
	 * lalu menuliskannya kembali (pola yang dipakai
	 * {@link #populateChecklistHasilPenilaianUmum(ChecklistHasilPenilaianUmum, Long, String)}).
	 * Kegagalan I/O ditelan dan dicatat ke audit.</p>
	 *
	 * @param data        isi indeks JSON yang akan ditulis
	 * @param pertemuanId id pertemuan; boleh {@code null}
	 * @param userId      id pengguna yang dinilai; boleh {@code null}
	 */
	public void tulisLokasiChecklistHasilPenilaianUmum(String data, Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		try {
			// System.out.println("tulis file ke " + file.getAbsolutePath());
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1236");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks checklist hasil penilaian umum untuk kombinasi entity +
	 * {@code userId} + {@code pertemuanId} tertentu.
	 *
	 * <p>Penghapusan dilakukan lewat {@code BacaTulisUtil.doHapus(file, "checklist_hasil_penilaian_umum")}.
	 * Sekali lagi: yang dihapus adalah <b>berkas indeks</b>, bukan baris
	 * {@code ChecklistHasilPenilaianUmum} di database. Dipakai
	 * {@link #reInitChecklistHasilPenilaianUmum(Session, Long, String)} sebagai langkah bersih-bersih
	 * sebelum indeks dibangun ulang.</p>
	 *
	 * @param pertemuanId id pertemuan; boleh {@code null}
	 * @param userId      id pengguna yang dinilai; boleh {@code null}
	 */
	public void bersihkanLokasiChecklistHasilPenilaianUmum(Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		BacaTulisUtil.doHapus(file, "checklist_hasil_penilaian_umum");

	}

	/**
	 * Membangun ulang indeks berkas checklist hasil penilaian umum dari database.
	 *
	 * <p>Kolom relasi yang dipakai sebagai filter ditentukan dari tipe entity ini:
	 * {@link Tbmuser} &rarr; {@code tbmuser}, {@code Dosen} &rarr; {@code dosen},
	 * {@code Mahasiswa} &rarr; {@code mahasiswa}, {@code Siswa} &rarr; {@code siswa},
	 * {@code Guru} &rarr; {@code guru}. <b>Untuk tipe entity lain method tidak melakukan apa
	 * pun</b> — indeks dibiarkan sebagaimana adanya.</p>
	 *
	 * <p>Alurnya: query {@code Criteria} mengambil daftar {@code id} yang cocok (diurut naik,
	 * memakai {@code Projections.property("id")} sehingga hanya id yang ditarik dari database),
	 * indeks lama dihapus dan diganti JSON kosong, lalu setiap id dimasukkan kembali. Untuk tiap
	 * id, entity coba diambil dari cache lewat {@code ambilData(...)} milik {@link DataUtil}; bila
	 * belum ada, baris dibaca dari {@code session} dan disimpan ke cache lewat
	 * {@code masukkanData(...)}. Terakhir
	 * {@link #populateChecklistHasilPenilaianUmum(ChecklistHasilPenilaianUmum, Long, String)}
	 * mendaftarkannya ke indeks.</p>
	 *
	 * <p><b>Efek samping:</b> menulis dan menghapus berkas indeks, serta mengisi cache entity.
	 * Membutuhkan {@code session} yang masih terbuka; method ini tidak membuka session sendiri dan
	 * tidak mengelola transaksi. Biasanya dipanggil dari
	 * {@link #ambilChecklistHasilPenilaianUmum(Session, Long, String, boolean)} hanya ketika
	 * penanda {@link #udah(String)} belum terpasang atau saat {@code refresh} diminta.</p>
	 *
	 * @param session     session Hibernate aktif
	 * @param pertemuanId id pertemuan; {@code null} berarti menyaring baris yang
	 *                    {@code pertemuanId}-nya {@code null}
	 * @param userId      id pengguna yang dinilai; {@code null} berarti menyaring baris yang
	 *                    {@code tbmuserDinilai}-nya {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void reInitChecklistHasilPenilaianUmum(Session session, Long pertemuanId, String userId) {

		String kolomName = "";
		if (this instanceof Tbmuser) {
			kolomName = "tbmuser";
		} else if (this instanceof Dosen) {
			kolomName = "dosen";
		} else if (this instanceof Mahasiswa) {
			kolomName = "mahasiswa";
		} else if (this instanceof Siswa) {
			kolomName = "siswa";
		} else if (this instanceof Guru) {
			kolomName = "guru";
		}

		if (!kolomName.trim().isEmpty()) {

			List<Long> checklistHasilPenilaianUmumsId = session.createCriteria(ChecklistHasilPenilaianUmum.class)
					.add(userId == null ? Restrictions.isNull("tbmuserDinilai")
							: Restrictions.eq("tbmuserDinilai.userId", userId))
					.add(pertemuanId == null ? Restrictions.isNull("pertemuanId")
							: Restrictions.eq("pertemuanId", pertemuanId))
					.addOrder(Order.asc("id")).setProjection(Projections.property("id"))
					.add(Restrictions.eq(kolomName, this)).list();

//			System.out.println("checklistHasilPenilaianUmumsId ->  " + checklistHasilPenilaianUmumsId + " userId "
//					+ userId + " this " + this);

			bersihkanLokasiChecklistHasilPenilaianUmum(pertemuanId, userId);
			tulisLokasiChecklistHasilPenilaianUmum(new JSONObject().toString(), pertemuanId, userId);
			for (Long checklistHasilPenilaianUmumId : checklistHasilPenilaianUmumsId) {
				ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) ambilData(
						ChecklistHasilPenilaianUmum.class, checklistHasilPenilaianUmumId.toString());
				if (checklistHasilPenilaianUmum == null) {
					checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) session
							.createCriteria(ChecklistHasilPenilaianUmum.class)
							.add(Restrictions.idEq(checklistHasilPenilaianUmumId)).uniqueResult();
					masukkanData(ChecklistHasilPenilaianUmum.class, checklistHasilPenilaianUmum);
				}
				populateChecklistHasilPenilaianUmum(checklistHasilPenilaianUmum, pertemuanId, userId);

			}
			checklistHasilPenilaianUmumsId = null;
		}
	}

	/**
	 * Mencabut satu baris checklist hasil penilaian umum dari indeks berkas.
	 *
	 * <p>Yang terjadi bukan penghapusan kunci, melainkan <b>pengosongan nilainya</b>
	 * ({@code id -> ""}). Itu cukup karena
	 * {@link #ambilChecklistHasilPenilaianUmum(Session, Long, String, boolean)} hanya memproses
	 * kunci yang nilainya tidak kosong. Baris di database tidak tersentuh.</p>
	 *
	 * <p>Kegagalan parsing/penulisan ditelan dan dicatat ke audit.</p>
	 *
	 * @param id          id baris yang dicabut dari indeks
	 * @param pertemuanId id pertemuan; boleh {@code null}
	 * @param userId      id pengguna yang dinilai; boleh {@code null}
	 */
	public void removeChecklistHasilPenilaianUmum(Serializable id, Long pertemuanId, String userId) {
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			c.put(id.toString(), "");
			tulisLokasiChecklistHasilPenilaianUmum(c.toString(), pertemuanId, userId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1305");

		}
	}

	/**
	 * Mendaftarkan satu baris checklist hasil penilaian umum ke indeks berkas.
	 *
	 * <p>Membaca indeks, memasang entri {@code id -> id} (nilai sama dengan kunci, menandakan
	 * "aktif"; bandingkan dengan
	 * {@link #removeChecklistHasilPenilaianUmum(Serializable, Long, String)} yang mengosongkan
	 * nilainya), lalu menuliskan indeks kembali. Argumen {@code null} diabaikan tanpa efek.</p>
	 *
	 * <p><b>Catatan bentuk data:</b> berbeda dari padanannya
	 * {@link #populateIsiAngketParameterUmum(IsiAngketParameterUmum)} yang menyimpan <i>path
	 * berkas</i> sebagai nilai, di sini yang disimpan hanya id — pemuatan datanya belakangan
	 * dilakukan lewat cache/DB, bukan dari berkas.</p>
	 *
	 * <p>Kegagalan parsing/penulisan ditelan dan dicatat ke audit.</p>
	 *
	 * @param checklistHasilPenilaianUmum baris yang didaftarkan; {@code null} diabaikan
	 * @param pertemuanId                 id pertemuan; boleh {@code null}
	 * @param userId                      id pengguna yang dinilai; boleh {@code null}
	 */
	public void populateChecklistHasilPenilaianUmum(ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum,
			Long pertemuanId, String userId) {
		try {
			if (checklistHasilPenilaianUmum == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			c.put(checklistHasilPenilaianUmum.getId().toString(), checklistHasilPenilaianUmum.getId().toString());
			tulisLokasiChecklistHasilPenilaianUmum(c.toString(), pertemuanId, userId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1320");
		}
	}

	/**
	 * Mengembalikan koleksi checklist hasil penilaian umum milik entity ini, memakai indeks berkas
	 * sebagai jalan pintas atas query database.
	 *
	 * <p>Bila {@code refresh} bernilai {@code true} <b>atau</b> penanda {@link #udah(String)} untuk
	 * kombinasi {@code userId}+{@code pertemuanId} belum terpasang, indeks dibangun ulang lebih
	 * dulu lewat {@link #reInitChecklistHasilPenilaianUmum(Session, Long, String)}. Sesudah itu
	 * setiap kunci aktif pada indeks dimuat lewat {@code ambilData(...)} (cache, dengan fallback
	 * DB) dan disaring ulang: {@code pertemuanId} harus cocok bila diminta, {@code userId} peserta
	 * yang dinilai harus cocok bila diminta, dan {@code checklistPenilaianUmum} tidak boleh
	 * {@code null}.</p>
	 *
	 * <p><b>Efek samping yang perlu disadari:</b> setiap baris yang lolos disambungkan balik ke
	 * entity ini lewat setter yang sesuai dengan tipenya ({@code setTbmuser}/{@code setDosen}/
	 * {@code setMahasiswa}/{@code setGuru}/{@code setSiswa}). Object yang dikembalikan karenanya
	 * <b>dimutasi</b>; bila berasal dari cache bersama, mutasi itu terlihat pemakai lain. Method
	 * ini juga mencetak ringkasan hasil ke {@code System.out}.</p>
	 *
	 * <p>Hasil dikumpulkan dalam {@code Map<Long, ...>} berkunci {@code id} — bukan {@code Set} —
	 * karena {@code hashCode()} entity tidak konsisten dengan {@link #equals(Object)}; itulah cara
	 * deduplikasi yang benar di codebase ini.</p>
	 *
	 * @param session     session Hibernate aktif (dibutuhkan bila indeks perlu dibangun ulang)
	 * @param pertemuanId id pertemuan; {@code null} berarti tanpa penyaringan pertemuan
	 * @param userId      id pengguna yang dinilai; {@code null} berarti tanpa penyaringan pengguna
	 * @param refresh     {@code true} untuk memaksa pembangunan ulang indeks dari database
	 * @return koleksi baris checklist yang lolos saringan; kosong bila tidak ada
	 * @see #reInitChecklistHasilPenilaianUmum(Session, Long, String)
	 */
	@SuppressWarnings("unchecked")
	public Collection<ChecklistHasilPenilaianUmum> ambilChecklistHasilPenilaianUmum(Session session, Long pertemuanId,
			String userId, boolean refresh) {
		if (refresh || !udah("ChecklistHasilPenilaianUmum_baru" + (userId == null ? "" : "_" + userId)
				+ (pertemuanId == null ? "" : "_" + pertemuanId))) {
			reInitChecklistHasilPenilaianUmum(session, pertemuanId, userId);
		}

		Map<Long, ChecklistHasilPenilaianUmum> maps = new HashMap<Long, ChecklistHasilPenilaianUmum>();
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			Iterator<String> keys = c.keys();
			Set<String> keyData = new HashSet<String>();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						keyData.add(key);
						ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) ambilData(
								ChecklistHasilPenilaianUmum.class, key, true);
						if (checklistHasilPenilaianUmum != null
								&& (pertemuanId == null || (checklistHasilPenilaianUmum.getPertemuanId() != null
										&& pertemuanId.equals(checklistHasilPenilaianUmum.getPertemuanId())))

								&& (userId == null || (checklistHasilPenilaianUmum.getTbmuserDinilai() != null
										&& checklistHasilPenilaianUmum.getTbmuserDinilai().getUserId() != null
										&& userId.equals(checklistHasilPenilaianUmum.getTbmuserDinilai().getUserId())))

								&& checklistHasilPenilaianUmum.getChecklistPenilaianUmum() != null) {
							if (this instanceof Tbmuser) {
								checklistHasilPenilaianUmum.setTbmuser((Tbmuser) this);
							} else if (this instanceof Dosen) {
								checklistHasilPenilaianUmum.setDosen((Dosen) this);
							} else if (this instanceof Mahasiswa) {
								checklistHasilPenilaianUmum.setMahasiswa((Mahasiswa) this);
							} else if (this instanceof Guru) {
								checklistHasilPenilaianUmum.setGuru((Guru) this);
							} else if (this instanceof Siswa) {
								checklistHasilPenilaianUmum.setSiswa((Siswa) this);
							}
							maps.put(checklistHasilPenilaianUmum.getId(), checklistHasilPenilaianUmum);
						}

					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1370");
				}
			}
			System.out.println("maps data ->  " + maps + " keys " + keyData + " c " + c);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1375");
		}

		return maps.values();
	}

	/**
	 * Membaca isi indeks JSON isian angket parameter umum milik entity ini.
	 *
	 * <p>Berbeda dari indeks checklist yang terpisah per pertemuan/pengguna, indeks angket hanya
	 * satu per entity. Kunci berkas memakai {@code userId} untuk {@link Tbmuser} dan
	 * {@link #getId()} untuk entity lain.</p>
	 *
	 * <p>Isinya berbentuk peta {@code id -> path berkas snapshot} untuk baris aktif, dan
	 * {@code id -> ""} untuk baris yang dicabut oleh
	 * {@link #removeIsiAngketParameterUmum(Serializable)}.</p>
	 *
	 * <p>Method ini mencetak lokasi berkas yang dibaca ke {@code System.out}.</p>
	 *
	 * @return isi indeks JSON, atau {@code VOMahasiswa.dataJSON} (JSON kosong bawaan) bila berkas
	 *         belum ada/kosong/gagal dibaca
	 */
	public String ambilLokasiIsiAngketParameterUmum() {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		try {
			System.out.println("baca file dari " + file.getAbsolutePath());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1391");
		}
		return VOMahasiswa.dataJSON;
	}

	/**
	 * Menimpa isi indeks JSON isian angket parameter umum milik entity ini.
	 *
	 * <p>Penulisan bersifat menimpa penuh; pemanggil bertanggung jawab membaca-ubah-tulis (lihat
	 * {@link #populateIsiAngketParameterUmum(IsiAngketParameterUmum)}). Method ini mencetak lokasi
	 * berkas yang ditulis ke {@code System.out}, dan kegagalan I/O ditelan serta dicatat ke
	 * audit.</p>
	 *
	 * @param data isi indeks JSON yang akan ditulis
	 */
	public void tulisLokasiIsiAngketParameterUmum(String data) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		try {
			System.out.println("tulis file ke " + file.getAbsolutePath());
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1405");
			// TODO Auto-generated catch block

		}
	}

	/**
	 * Menghapus berkas indeks isian angket parameter umum milik entity ini lewat
	 * {@code BacaTulisUtil.doHapus(file, "isi_angket_parameter_umum")}.
	 *
	 * <p>Yang dihapus adalah berkas indeks, bukan baris {@code IsiAngketParameterUmum} di
	 * database. Dipakai {@link #reInitIsiAngketParameterUmum(Session)} sebelum indeks dibangun
	 * ulang.</p>
	 */
	public void bersihkanLokasiIsiAngketParameterUmum() {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		BacaTulisUtil.doHapus(file, "isi_angket_parameter_umum");

	}

	/**
	 * Membangun ulang indeks berkas isian angket parameter umum dari database.
	 *
	 * <p>Kolom relasi penyaring ditentukan dari tipe entity ini: {@link Tbmuser} &rarr;
	 * {@code tbmuser}, {@code Dosen} &rarr; {@code dosen}, {@code Mahasiswa} &rarr;
	 * {@code mahasiswa}. Perhatikan daftarnya <b>lebih sempit</b> daripada padanan checklist
	 * ({@link #reInitChecklistHasilPenilaianUmum(Session, Long, String)}) — {@code Siswa} dan
	 * {@code Guru} tidak termasuk; untuk tipe lain method tidak melakukan apa pun.</p>
	 *
	 * <p>Berbeda pula dari padanan checklist yang hanya menarik kolom {@code id}, query di sini
	 * menarik <b>entity penuh</b> lalu setiap baris disimpan sebagai snapshot berkas oleh
	 * {@link #populateIsiAngketParameterUmum(IsiAngketParameterUmum)}. Indeks lama dihapus dan
	 * diganti JSON kosong lebih dulu.</p>
	 *
	 * <p><b>Efek samping:</b> menghapus/menulis berkas indeks dan menulis satu berkas snapshot per
	 * baris. Membutuhkan {@code session} yang masih terbuka; tidak membuka session atau transaksi
	 * sendiri.</p>
	 *
	 * @param session session Hibernate aktif
	 */
	@SuppressWarnings("unchecked")
	public void reInitIsiAngketParameterUmum(Session session) {

		String kolomName = "";
		if (this instanceof Tbmuser) {
			kolomName = "tbmuser";
		} else if (this instanceof Dosen) {
			kolomName = "dosen";
		} else if (this instanceof Mahasiswa) {
			kolomName = "mahasiswa";
		}

		if (!kolomName.trim().isEmpty()) {

			List<IsiAngketParameterUmum> isiAngketParameterUmums = session.createCriteria(IsiAngketParameterUmum.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq(kolomName, this)).list();
			bersihkanLokasiIsiAngketParameterUmum();
			tulisLokasiIsiAngketParameterUmum(new JSONObject().toString());
			for (IsiAngketParameterUmum isiAngketParameterUmum : isiAngketParameterUmums) {
				populateIsiAngketParameterUmum(isiAngketParameterUmum);
			}
			isiAngketParameterUmums = null;
		}
	}

	/**
	 * Mencabut satu baris isian angket parameter umum dari indeks berkas dengan mengosongkan
	 * nilainya ({@code id -> ""}).
	 *
	 * <p>Kuncinya tetap ada; {@link #ambilIsiAngketParameterUmum(Session, boolean)} melewati entri
	 * bernilai kosong. Baris di database tidak tersentuh, dan berkas snapshot yang mungkin sudah
	 * ditulis sebelumnya <b>tidak</b> ikut dihapus. Kegagalan ditelan dan dicatat ke audit.</p>
	 *
	 * @param id id baris yang dicabut dari indeks
	 */
	public void removeIsiAngketParameterUmum(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			c.put(id.toString(), "");
			tulisLokasiIsiAngketParameterUmum(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1451");

		}
	}

	/**
	 * Mendaftarkan satu baris isian angket parameter umum ke indeks, sekaligus menuliskan snapshot
	 * JSON baris tersebut ke berkas.
	 *
	 * <p>Nilai yang disimpan di indeks adalah <b>path absolut berkas snapshot</b> hasil
	 * {@code isiAngketParameterUmum.write()} — inilah perbedaan penting dari padanan checklist
	 * ({@link #populateChecklistHasilPenilaianUmum(ChecklistHasilPenilaianUmum, Long, String)})
	 * yang hanya menyimpan id. Konsekuensinya
	 * {@link #ambilIsiAngketParameterUmum(Session, boolean)} membaca datanya dari berkas, bukan
	 * dari cache/DB, sehingga isi yang dikembalikan mencerminkan keadaan saat snapshot ditulis —
	 * bukan keadaan terkini di database.</p>
	 *
	 * <p>Argumen {@code null} diabaikan; kegagalan ditelan dan dicatat ke audit.</p>
	 *
	 * @param isiAngketParameterUmum baris yang didaftarkan; {@code null} diabaikan
	 */
	public void populateIsiAngketParameterUmum(IsiAngketParameterUmum isiAngketParameterUmum) {
		try {
			if (isiAngketParameterUmum == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			c.put(isiAngketParameterUmum.getId().toString(), isiAngketParameterUmum.write().getAbsolutePath());
			tulisLokasiIsiAngketParameterUmum(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1465");
		}
	}

	/**
	 * Mengembalikan koleksi isian angket parameter umum milik entity ini, dibaca dari berkas
	 * snapshot yang ditunjuk indeks.
	 *
	 * <p>Bila {@code refresh} bernilai {@code true} <b>atau</b> penanda
	 * {@code udah("IsiAngketParameterUmum")} belum terpasang, indeks dibangun ulang lebih dulu
	 * lewat {@link #reInitIsiAngketParameterUmum(Session)}. Sesudah itu setiap entri indeks yang
	 * nilainya tidak kosong diperlakukan sebagai path berkas: berkas dibaca, JSON-nya diubah
	 * kembali menjadi object lewat {@code Common.convertToObject(...)}, lalu disaring — baris
	 * dibuang bila {@code jadwalChecklistPenilaianUmum}-nya {@code null}.</p>
	 *
	 * <p><b>Perhatian:</b> karena datanya berasal dari snapshot berkas, hasilnya adalah object
	 * <i>detached</i> hasil rekonstruksi, bukan entity yang dikelola {@code session}, dan bisa
	 * tertinggal dari keadaan terkini di database. Setiap baris juga <b>dimutasi</b> agar
	 * menunjuk balik ke entity ini ({@code setTbmuser}/{@code setDosen}/{@code setMahasiswa}).</p>
	 *
	 * <p>Hasil dikumpulkan ke {@code Map} berkunci <b>id jadwal</b>
	 * ({@code jadwalChecklistPenilaianUmum.getId()}) — bukan id barisnya sendiri — sehingga bila
	 * ada lebih dari satu isian untuk jadwal yang sama, <b>hanya yang terakhir dibaca yang
	 * bertahan</b>. Perilaku ini tampaknya disengaja sebagai "satu isian per jadwal", tetapi perlu
	 * disadari saat menelusuri data yang seolah hilang.</p>
	 *
	 * <p>Kegagalan per baris ditelan dan dicatat ke audit; baris bermasalah dilewati.</p>
	 *
	 * @param session session Hibernate aktif (dibutuhkan bila indeks perlu dibangun ulang)
	 * @param refresh {@code true} untuk memaksa pembangunan ulang indeks dari database
	 * @return koleksi isian angket; kosong bila tidak ada
	 * @see #reInitIsiAngketParameterUmum(Session)
	 */
	@SuppressWarnings("unchecked")
	public Collection<IsiAngketParameterUmum> ambilIsiAngketParameterUmum(Session session, boolean refresh) {
		if (refresh || !udah("IsiAngketParameterUmum")) {
			reInitIsiAngketParameterUmum(session);
		}

		Map<Long, IsiAngketParameterUmum> maps = new HashMap<Long, IsiAngketParameterUmum>();
		try {
			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (!s.trim().isEmpty()) {
						File file = new File(s);
						if (file != null && file.exists()) {
							IsiAngketParameterUmum isiAngketParameterUmum = (IsiAngketParameterUmum) Common
									.convertToObject(new JSONObject(ais.common.BacaTulisUtil.baca(file)),
											IsiAngketParameterUmum.class);
							if (isiAngketParameterUmum != null
									&& isiAngketParameterUmum.getJadwalChecklistPenilaianUmum() != null) {
								if (this instanceof Tbmuser) {
									isiAngketParameterUmum.setTbmuser((Tbmuser) this);
								} else if (this instanceof Dosen) {
									isiAngketParameterUmum.setDosen((Dosen) this);
								} else if (this instanceof Mahasiswa) {
									isiAngketParameterUmum.setMahasiswa((Mahasiswa) this);
								}
								maps.put(isiAngketParameterUmum.getJadwalChecklistPenilaianUmum().getId(),
										isiAngketParameterUmum);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1504");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/GeneralValueObject.java:1508");
		}

		return maps.values();
	}

	/**
	 * Pintasan {@link #tampilKunci(Component, VoKunci, Tbmuser, EventListener, boolean)} dengan
	 * label tombol ditampilkan.
	 *
	 * @param toolbar       komponen ZK induk tempat tombol dipasang
	 * @param voKunci       data yang dapat dikunci
	 * @param tbmuser       akun pengguna aktif
	 * @param eventListener listener yang dipicu setelah status kunci berubah
	 * @see #tampilKunci(Component, VoKunci, Tbmuser, EventListener, boolean)
	 */
	public static void tampilKunci(Component toolbar, VoKunci voKunci, Tbmuser tbmuser, EventListener eventListener) {
		tampilKunci(toolbar, voKunci, tbmuser, eventListener, true);
	}

	/**
	 * Memasang sepasang tombol ZK "Kunci"/"Buka Kunci" pada toolbar sebuah baris/window, lengkap
	 * dengan konfirmasi dan aturan hak akses.
	 *
	 * <p>Ini satu-satunya method di kelas ini yang membangun komponen UI; ia ada di sini karena
	 * penguncian data adalah perilaku lintas modul yang berlaku bagi banyak entity turunan
	 * {@link VoKunci}.</p>
	 *
	 * <h4>Perilaku</h4>
	 * <ul>
	 *   <li>Tombol hanya dipasang bila {@code voKunci} tidak {@code null} <b>dan</b> akun aktif
	 *   bukan siswa ({@code tbmuser.getSiswa() == null}).</li>
	 *   <li>Menekan "Kunci" memunculkan konfirmasi; bila disetujui, {@code voKunci} diberi penanda
	 *   pengunci berupa pengguna aktif ({@code Common.getCurrentUser()}) dan disimpan lewat
	 *   {@code Common.refreshUpdate(voKunci)} — jadi <b>ada efek tulis ke database</b> di dalam
	 *   listener ini. Menekan "Buka Kunci" melakukan kebalikannya
	 *   ({@code setDikunci(null)}).</li>
	 *   <li>Sesudah perubahan, visibilitas kedua tombol disegarkan dan
	 *   {@code Common.createDefaultTimer(eventListener)} dipanggil agar layar pemanggil ikut
	 *   dimuat ulang.</li>
	 *   <li>Kedua tombol dinonaktifkan bila {@code Common.getApakahAdminBolehKunci()} bernilai
	 *   {@code false}. Tombol "Buka Kunci" juga dinonaktifkan bila data dikunci oleh pengguna
	 *   <b>lain</b>, sehingga hanya pengunci yang sama yang boleh membukanya.</li>
	 *   <li>Penempatan menyesuaikan tata letak: bila toolbar punya atribut
	 *   {@code "ais_row_actions_popup"} berisi {@code Div}, tombol dimasukkan ke menu popup
	 *   (dengan pemisah dan label penuh); bila tidak, tombol dipasang langsung di toolbar sebagai
	 *   tombol ikon kecil.</li>
	 * </ul>
	 *
	 * @param toolbar       komponen ZK induk tempat tombol dipasang
	 * @param voKunci       data yang dapat dikunci; bila {@code null} tidak ada tombol dipasang
	 * @param tbmuser       akun pengguna aktif, dipakai untuk menentukan kelayakan tampil
	 * @param eventListener listener yang dipicu setelah status kunci berubah
	 * @param tampilLabel   {@code true} untuk menampilkan teks pada tombol toolbar; label tetap
	 *                      dipaksa tampil bila tombol masuk ke menu popup
	 */
	public static void tampilKunci(Component toolbar, final VoKunci voKunci, Tbmuser tbmuser,
			final EventListener eventListener, final boolean tampilLabel) {
		final MyToolbarbuttonConfig bukaKunci = new MyToolbarbuttonConfig(tampilLabel ? "Buka" : "",
				"/img/svg/unlock.svg");
		final MyToolbarbuttonConfig kunci = new MyToolbarbuttonConfig(tampilLabel ? "Kunci" : "", "/img/svg/lock.svg");

		bukaKunci.setTooltiptext("Buka kunci");
		kunci.setTooltiptext("Tutup kunci");

		if (tbmuser.getSiswa() == null && voKunci != null) {

			kunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengunci data ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										voKunci.setDikunci(Common.getCurrentUser());
										Common.refreshUpdate(voKunci);

										kunci.setVisible(voKunci.getDikunci() == null);
										bukaKunci.setVisible(voKunci.getDikunci() != null);
										if (voKunci.getDikunci() != null && tampilLabel) {
											bukaKunci.setLabel(
													"Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
										}
										if (voKunci.getDikunci() != null) {
											bukaKunci.setTooltiptext(
													"Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
										}
										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});

			kunci.setVisible(voKunci.getDikunci() == null);
			kunci.setDisabled(!Common.getApakahAdminBolehKunci());

			bukaKunci.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membuka kunci data ini ?.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										voKunci.setDikunci(null);
										Common.refreshUpdate(voKunci);

										kunci.setVisible(voKunci.getDikunci() == null);
										bukaKunci.setVisible(voKunci.getDikunci() != null);

										Common.createDefaultTimer(eventListener);
									}

								}
							});
				}
			});
			bukaKunci.setVisible(voKunci.getDikunci() != null);
			if (voKunci.getDikunci() != null && tampilLabel) {
				bukaKunci.setLabel("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
			}

			if (voKunci.getDikunci() != null) {
				bukaKunci.setTooltiptext("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
			}
			bukaKunci.setDisabled((voKunci.getDikunci() != null && Common.getCurrentUser().getUserId() != null
					&& !voKunci.getDikunci().getUserId().equals(Common.getCurrentUser().getUserId()))

					|| !Common.getApakahAdminBolehKunci());

			// Tambahkan ke popup kebab jika tersedia; fallback ke toolbar langsung.
			Object popupAttrKunci = toolbar.getAttribute("ais_row_actions_popup");
			if (popupAttrKunci instanceof org.zkoss.zul.Div) {
				org.zkoss.zul.Div popupContent = (org.zkoss.zul.Div) popupAttrKunci;
				org.zkoss.zul.Div divider = new org.zkoss.zul.Div();
				divider.setSclass("ais-row-popup-divider");
				divider.setParent(popupContent);
				kunci.setSclass("ais-row-popup-item ais-row-popup-item-kunci");
				bukaKunci.setSclass("ais-row-popup-item ais-row-popup-item-kunci");
				// Pastikan label selalu tampil di menu popup
				if (voKunci.getDikunci() != null) {
					bukaKunci.setLabel("Buka Kunci (" + voKunci.getDikunci().getUserNama() + ")");
				} else {
					bukaKunci.setLabel("Buka Kunci");
				}
				kunci.setLabel("Kunci Data");
				kunci.setParent(popupContent);
				bukaKunci.setParent(popupContent);
			} else {
				kunci.setSclass("ais-row-action-btn ais-row-action-kunci");
				kunci.setOrient("vertical");
				kunci.setStyle("font-size:9px;");
				bukaKunci.setSclass("ais-row-action-btn ais-row-action-kunci");
				bukaKunci.setOrient("vertical");
				bukaKunci.setStyle("font-size:9px;");
				kunci.setParent(toolbar);
				bukaKunci.setParent(toolbar);
			}

		}
	}
	
	
	/**
	 * Menentukan apakah sebuah nilai properti dianggap "kosong" untuk keperluan pencocokan di
	 * {@link #ambilSatuData(Class, List, String[], Object[])}.
	 *
	 * <p>Aturannya sengaja berbeda per tipe: {@code null} kosong; {@link String} kosong bila hanya
	 * berisi spasi; {@code GeneralValueObject} kosong bila {@code id}-nya {@code null} (entity
	 * yang belum tersimpan dianggap belum menunjuk apa pun). Tipe lain — termasuk angka
	 * {@code 0} dan {@code Boolean.FALSE} — <b>tidak</b> dianggap kosong.</p>
	 *
	 * @param value nilai yang diperiksa; boleh {@code null}
	 * @return {@code true} bila nilai dianggap kosong
	 */
	private static boolean isNilaiKosong(Object value) {
		if (value == null) {
			return true;
		}
		if (value instanceof String) {
			return ((String) value).trim().isEmpty();
		}
		if (value instanceof GeneralValueObject) {
			return ((GeneralValueObject) value).getId() == null;
		}
		return false;
	}

	/**
	 * Membandingkan dua nilai properti secara toleran untuk keperluan
	 * {@link #ambilSatuData(Class, List, String[], Object[])}.
	 *
	 * <p>Aturan per tipe, diperiksa berurutan:</p>
	 * <ul>
	 *   <li>Dua {@code GeneralValueObject} &rarr; sama bila kedua {@code id} non-null dan
	 *   bernilai sama. Sengaja tidak memakai {@link #equals(Object)} agar entity ber-{@code id}
	 *   {@code null} tidak pernah dinyatakan cocok.</li>
	 *   <li>Dua {@link Date} &rarr; dibandingkan lewat hasil format {@code Common.dateFormat3},
	 *   sehingga perbedaan presisi di bawah satuan format itu diabaikan.</li>
	 *   <li>Dua {@link Number} &rarr; dibandingkan sebagai {@code double}, sehingga
	 *   {@code Integer 5}, {@code Long 5L}, dan {@code BigDecimal 5.0} dianggap sama.</li>
	 *   <li>Selain itu &rarr; perbandingan {@code toString()} yang sudah di-{@code trim} dan tidak
	 *   peka besar-kecil huruf.</li>
	 * </ul>
	 *
	 * <p>Method mengasumsikan kedua argumen tidak {@code null} — pemanggilnya sudah menyaring
	 * nilai kosong lebih dulu lewat {@link #isNilaiKosong(Object)}.</p>
	 *
	 * @param valData nilai aktual pada kandidat
	 * @param expData nilai yang diharapkan
	 * @return {@code true} bila kedua nilai dianggap sama
	 */
	private static boolean isNilaiSama(Object valData, Object expData) {
		if (valData instanceof GeneralValueObject && expData instanceof GeneralValueObject) {
			GeneralValueObject vObj = (GeneralValueObject) valData;
			GeneralValueObject eObj = (GeneralValueObject) expData;
			return vObj.getId() != null && eObj.getId() != null && vObj.getId().equals(eObj.getId());
		}
		if (valData instanceof java.util.Date && expData instanceof java.util.Date) {
			return ais.common.Common.dateFormat3.get().format((java.util.Date) valData)
					.equals(ais.common.Common.dateFormat3.get().format((java.util.Date) expData));
		}
		if (valData instanceof Number && expData instanceof Number) {
			return Double.compare(((Number) valData).doubleValue(), ((Number) expData).doubleValue()) == 0;
		}
		return valData.toString().trim().equalsIgnoreCase(expData.toString().trim());
	}

	@SuppressWarnings("rawtypes")
	public static GeneralValueObject ambilSatuData(Class clazz, List<? extends GeneralValueObject> generalValueObjects,
			String[] properties, Object[] datas) {

		if (generalValueObjects == null || generalValueObjects.isEmpty()) {
			return null;
		}

		GeneralValueObject bestMatch = null;
		int highestScore = -1;

		org.hibernate.metadata.ClassMetadata classMetadata = null;
		try {
			classMetadata = HibernateUtil.getSessionFactory().getClassMetadata(clazz);
		} catch (Exception e) {
			classMetadata = HibernateUtil.getClassMetadata(clazz);
		}

		if (classMetadata == null) {
			return null;
		}

		int panjangFilter = properties == null || datas == null ? 0 : Math.min(properties.length, datas.length);
		for (GeneralValueObject d : generalValueObjects) {
			if (d == null) continue;

			int currentScore = 0;
			boolean isEligible = true;

			for (int i = 0; i < panjangFilter; i++) {
				String property = properties[i];
				if (property == null || property.trim().isEmpty()) {
					continue;
				}
				Object expData = datas[i];

				Object valData = null;
				try {
					valData = classMetadata.getPropertyValue(d, property, org.hibernate.EntityMode.POJO);
				} catch (Exception e) {
					isEligible = false;
					break;
				}

				if (isNilaiKosong(valData)) {
					continue;
				}

				if (isNilaiKosong(expData)) {
					isEligible = false;
					break;
				}

				if (isNilaiSama(valData, expData)) {
					currentScore++;
				} else {
					isEligible = false;
					break;
				}
			}

			if (isEligible && currentScore > highestScore) {
				highestScore = currentScore;
				bestMatch = d;
			}
		}

		return bestMatch;
	}
}
