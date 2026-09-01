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

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNim() {
		return nim;
	}

	public void setNim(String nim) {
		this.nim = nim;
	}

	public Integer getNomorUrut() {
		return nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

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

	public File write(String... strings) {
		Integer indexke = 0;
		return write(indexke, strings);
	}

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

	public boolean udah() {
		return udah("");
	}

	public void belum() {
		belum("");
	}

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

	public void put(String data) {
		put(data, "");
	}

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

	// THREAD-SAFETY (lihat ConcurrentModificationException & "Unterminated string" audit
	// KRS/elearning, GeneralValueObject.putBaru/tulisPutBaru dipanggil dari AuditListener.
	// prosesUntukElearning): map ini di-share OLEH SELURUH THREAD/REQUEST dalam satu JVM
	// (bukan per-request), sengaja — dipakai sebagai akumulator "batch" ketika satu proses
	// sinkronisasi (mis. Dosen.singkronkanKrsMahasiswa) memanggil putBaru() berkali-kali
	// untuk entity yang sama (dosen/mahasiswa) SEBELUM tulisPutBaru() dipanggil sekali di
	// akhir (lihat voMahasiswaDosens di Dosen.java). Karena map & JSONObject di dalamnya
	// bukan thread-safe (HashMap biasa), DUA request BERBEDA yang kebetulan menyentuh
	// dosen/mahasiswa YANG SAMA secara bersamaan bisa saling mem-mutasi & men-serialize
	// JSONObject yang SAMA di saat bersamaan, menyebabkan:
	//  - ConcurrentModificationException saat toString() meng-iterasi HashMap yang sedang
	//    dimutasi thread lain, ATAU
	//  - file .json di disk tertimpa dua tulisan yang beririsan (masing-masing dari
	//    FileUtils.writeStringToFile terpisah tanpa penguncian) sehingga isinya terpotong
	//    di tengah string ("Unterminated string ...") saat dibaca ulang oleh putBaru().
	// Fix: ConcurrentHashMap (agar operasi get/put/remove pada map sendiri aman) DITAMBAH
	// synchronized(key.intern()) di kedua method supaya seluruh rangkaian get-or-create +
	// mutasi + serialize + tulis-berkas untuk SATU key (id+kelas+tambahan) tidak pernah
	// tumpang tindih antar-thread, sementara key BERBEDA (entity lain) tetap berjalan
	// paralel tanpa saling menunggu. ConcurrentHashMap TIDAK mengizinkan value null,
	// sehingga "penanda sudah ditulis" di tulisPutBaru() memakai remove(key), bukan
	// put(key, null) seperti sebelumnya.
	private static final Map<String, JSONObject> datatemporary = new java.util.concurrent.ConcurrentHashMap<String, JSONObject>();

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

	public String retreiveBaru() {
		return retreiveBaru("");
	}

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

	public String retreive() {
		return retreive("");
	}

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
	
	
	public void masukkanData(String jenis) {
		Tbmuser tbmuser = Common.getCurrentUser();
		masukkanData(jenis, tbmuser);
	}

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

	public TreeMap<String, String> ambilData(String jenis, String user) {
		return ambilData(jenis, user, "", null, null);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, Date mulai, Date sampai) {
		return ambilData(jenis, user, "", mulai, sampai);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, String posfix) {
		return ambilData(jenis, user, posfix, null, null);
	}

	public TreeMap<String, String> ambilData(String jenis, String user, String posfix, Date mulai, Date sampai) {
		return ambilData(jenis, user, posfix, mulai, sampai,
				new String[] { "Mahasiswa", "CalonMahasiswa", "Dosen", "Siswa", "CalonSiswa", "Guru" });
	}

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

	public void bersihkanLokasiChecklistHasilPenilaianUmum(Long pertemuanId, String userId) {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "checklist_hasil_penilaian_umum_" + id
				+ (userId == null ? "" : "_" + userId) + (pertemuanId == null ? "" : "_" + pertemuanId));
		BacaTulisUtil.doHapus(file, "checklist_hasil_penilaian_umum");

	}

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

	public void removeChecklistHasilPenilaianUmum(Serializable id, Long pertemuanId, String userId) {
		try {
			JSONObject c = new JSONObject(ambilLokasiChecklistHasilPenilaianUmum(pertemuanId, userId));
			c.put(id.toString(), "");
			tulisLokasiChecklistHasilPenilaianUmum(c.toString(), pertemuanId, userId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1305");

		}
	}

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

	public void bersihkanLokasiIsiAngketParameterUmum() {
		String id = getId() == null ? "" : getId().toString();
		if (this instanceof Tbmuser) {
			id = ((Tbmuser) this).getUserId();
		}
		File file = Common.getFileLocation(this, "isi_angket_parameter_umum_" + id);
		BacaTulisUtil.doHapus(file, "isi_angket_parameter_umum");

	}

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

	public void removeIsiAngketParameterUmum(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiIsiAngketParameterUmum());
			c.put(id.toString(), "");
			tulisLokasiIsiAngketParameterUmum(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/GeneralValueObject.java:1451");

		}
	}

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

	public static void tampilKunci(Component toolbar, VoKunci voKunci, Tbmuser tbmuser, EventListener eventListener) {
		tampilKunci(toolbar, voKunci, tbmuser, eventListener, true);
	}

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
