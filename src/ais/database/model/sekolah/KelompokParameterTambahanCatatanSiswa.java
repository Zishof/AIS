package ais.database.model.sekolah;

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




import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;



import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Kategori (heading/seksi) untuk field kustom pada modul <b>Catatan Siswa</b> jenjang
 * <b>SEKOLAH</b>.
 *
 * <p>Baris entity ini <b>bukan</b> field isian itu sendiri, melainkan <i>judul kelompok</i> yang
 * memayungi sekumpulan field kustom. Rantai lengkapnya empat lapis:</p>
 *
 * <ol>
 *   <li>{@link ais.database.model.ParameterTambahan} &mdash; definisi field kustom generik
 *       (nama, tipe isian, pilihan, dsb.) yang dipakai bersama oleh SELURUH modul AIS.</li>
 *   <li><b>{@code KelompokParameterTambahanCatatanSiswa}</b> (kelas ini) &mdash; kategori/heading
 *       tempat field-field tersebut dikelompokkan pada formulir Catatan Siswa.</li>
 *   <li>{@link ais.database.model.sekolah.ParameterTambahanCatatanSiswa} &mdash; tabel penghubung
 *       yang memetakan pasangan (kelompok &rarr; parameter); layar masternya
 *       {@code ais.action.master.sekolah.ParameterTambahanCatatanSiswaAction}.</li>
 *   <li>{@link ais.database.model.sekolah.JenisCatatanSiswa} &mdash; lapis PALING LUAR: sebuah
 *       kelompok baru muncul di formulir Catatan Siswa apabila kelompok itu <b>dicentang</b> pada
 *       jenis catatan yang bersangkutan (relasi
 *       {@code JenisCatatanSiswa.kelompokParameterTambahanCatatanSiswas}, dirakit
 *       {@code JenisCatatanSiswaAction.initKelompokParameterTambahanCatatanSiswa()}).</li>
 * </ol>
 *
 * <p>Nilai isian yang diketik pengguna <b>tidak</b> disimpan di sini maupun di lapis penghubung,
 * melainkan didenormalisasi ke kolom teks pada entity pemilik data
 * ({@code CatatanSiswa.parameterTambahanInds} dan kembarannya), dengan format ruas dipisah
 * {@code "\n"} antarbaris dan {@code "<=>"} antar-ruas &mdash; lihat
 * {@code ais.action.master.sekolah.CatatanSiswaAction} serta
 * {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanSiswaListener}.</p>
 *
 * <h2>Padanan versi PT</h2>
 * <p>Kelas ini adalah padanan jenjang SEKOLAH dari
 * {@link ais.database.model.KelompokParameterTambahanCatatanMahasiswa}. Keduanya <b>identik
 * struktur intinya</b> (field, auto-seed, getter berefek samping, {@code compareTo()} yang
 * dipangkas) &mdash; jelas hasil salin-tempel. Hanya ada dua perbedaan nyata:</p>
 * <ul>
 *   <li>Versi sekolah ini <b>menambahkan sepasang relasi cakupan multi-tenant</b>
 *       {@link #getYayasan()} dan {@link #getSekolah()}; versi PT tidak punya kolom cakupan
 *       apa pun (satu instalasi = satu perguruan tinggi).</li>
 *   <li>Nama tabelnya berbeda &mdash; lihat catatan pemetaan di bawah.</li>
 * </ul>
 *
 * <h2>Catatan pemetaan: nama tabel salah salin-tempel</h2>
 * <p>Entity ini dipetakan ke {@code sekolah.kelompok_parameter_tambahan_alur_sop} &mdash; nama
 * yang jelas berasal dari modul <b>SOP</b>, bukan dari domain Catatan Siswa. Ini
 * <b>bukan</b> sekadar kolom FK yang salah nama (pola yang sudah dikenal di keluarga
 * {@code ParameterTambahan*}), melainkan <b>nama tabelnya sendiri</b>. Tiga saudara sekandungnya
 * di paket yang sama justru diberi nama benar
 * ({@code kelompok_parameter_tambahan_catatan_guru},
 * {@code kelompok_parameter_tambahan_catatan_kelas_siswa},
 * {@code kelompok_parameter_tambahan_calon_siswa}), jadi entity inilah yang menyimpang.</p>
 * <p><b>Tidak terjadi tabrakan data</b> dengan
 * {@link ais.database.model.sop.KelompokParameterTambahanAlurSop} karena schema-nya berbeda
 * ({@code sekolah} vs {@code public}). Yang tersisa hanyalah jebakan pemeliharaan: pencarian
 * teks atas nama tabel SOP akan ikut menyeret modul sekolah, dan sebaliknya. Layar UI-nya sendiri
 * konsisten memakai penamaan yang benar
 * ({@code /pages/master/sekolah/kelompok_parameter_tambahan_catatan_siswa.zul}).</p>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan properti induknya sama sekali</b>.
 * Karena itu {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} <b>harus</b> dideklarasikan ulang di kelas ini. Duplikasi tersebut
 * adalah KEHARUSAN TEKNIS, bukan bug &mdash; jangan "dirapikan" dengan menghapusnya.</p>
 * <p>Konsekuensi lain: nilai {@code nama}/{@code keterangan}/{@code nomorUrut}/{@code nim} yang
 * dipakai method induk (mis. {@code compareTo()} bawaan) SELALU {@code null} pada instance ini
 * karena field induk tidak pernah diisi Hibernate. Kelas ini menutupinya dengan meng-{@code
 * override} {@link #compareTo(GeneralValueObject)} dan menyediakan {@code nama}/{@code keterangan}
 * sendiri.</p>
 *
 * <h2>Kelompok method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Identitas &amp; representasi:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #toString()}.</li>
 *   <li><b>Atribut kategori:</b> {@link #getNama()}, {@link #setNama(String)},
 *       {@link #getKeterangan()}, {@link #setKeterangan(String)}.</li>
 *   <li><b>Bendera &amp; urutan (getter berefek samping):</b> {@link #getDefaultData()},
 *       {@link #setDefaultData(Boolean)}, {@link #getAktif()}, {@link #setAktif(Boolean)},
 *       {@link #getNomorUrut()}, {@link #setNomorUrut(Integer)},
 *       {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getYayasan()}, {@link #setYayasan(Yayasan)},
 *       {@link #getSekolah()}, {@link #setSekolah(Sekolah)}.</li>
 *   <li><b>Penyiapan data awal:</b> {@link #checkCreateDefault()}.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; catatan penting</h2>
 * <ul>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak base class.</b>
 *       {@code GeneralValueObject.getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
 *       menjanjikan hasil non-null; override di sini mengembalikan field mentah, jadi
 *       <b>bisa {@code null}</b>. Pemanggil wajib berjaga sendiri.</li>
 *   <li><b>{@link #compareTo(GeneralValueObject)} dipangkas jadi satu baris.</b> Tidak ada
 *       {@code try/catch}, tidak ada fallback ke {@code nim}/{@code nama}/{@code keterangan}
 *       seperti versi induk. Berpasangan dengan fakta bahwa {@code nomorUrut} tidak pernah diisi
 *       lewat formulir Tambah/Ubah, ini melahirkan <b>bug penciutan senyap {@code TreeSet}</b>
 *       yang dijelaskan pada dokumentasi {@link #compareTo(GeneralValueObject)}.</li>
 *   <li><b>Getter berefek samping.</b> {@link #getDefaultData()}, {@link #getAktif()},
 *       {@link #getNomorUrut()}, dan {@link #getYayasan()} MENULIS ke field saat dibaca. Karena
 *       kelas ini memakai <i>property access</i> + {@code dynamicUpdate = true} + {@code @Audited},
 *       sekadar MEMBACA baris di dalam session aktif dapat memicu {@code UPDATE} sungguhan
 *       beserta revisi Envers palsu.</li>
 *   <li><b>Layar master parameter tidak bergerbang hak akses.</b> Lihat catatan keamanan pada
 *       {@link #getNomorUrut()} dan {@link #checkCreateDefault()}.</li>
 * </ul>
 *
 * @see ais.database.model.KelompokParameterTambahanCatatanMahasiswa
 * @see ais.database.model.sekolah.ParameterTambahanCatatanSiswa
 * @see ais.database.model.sekolah.JenisCatatanSiswa
 * @see ais.database.model.ParameterTambahan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "kelompok_parameter_tambahan_alur_sop")
public class KelompokParameterTambahanCatatanSiswa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris; lihat {@link #getId()}. Dideklarasikan ulang karena induk tidak dipetakan. */
	private Long id;
	/** Nama pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}, kosong, atau hanya spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah
	 * bisa dikosongkan lewat setter ini.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Dideklarasikan ulang di kelas ini karena {@link GeneralValueObject} bukan
	 * {@code @MappedSuperclass}; lihat catatan warisan pada dokumentasi kelas.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan {@link #setTanggal_dirubah(Date)}
	 * dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini. Karena itu baris hasil auto-seed {@link #checkCreateDefault()} masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId}.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getDefaultData()}, {@link #getAktif()},
	 * {@link #getNomorUrut()}, dan {@link #getYayasan()} dapat mengotori field saat baris sekadar
	 * DIBACA, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta
	 * pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang
	 * membuka layar.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam
	 * kode aslinya; nilainya diinisialisasi memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Tanpa validasi.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; pemanggilan manual akan tertimpa pada
	 * {@code UPDATE} berikutnya.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (presisi {@code TIMESTAMP}).
	 *
	 * <p>Tidak pernah {@code null} pada objek yang baru dibuat di JVM (diinisialisasi saat
	 * konstruksi), tetapi <b>bisa</b> {@code null} untuk baris lama hasil impor/migrasi.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam format {@code "<id>-<nama>"}.
	 *
	 * <p><b>Perhatikan:</b> yang dipakai adalah <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()}, sehingga hasilnya TIDAK di-{@code trim}. Untuk baris yang belum
	 * tersimpan, {@code id} masih {@code null} sehingga keluarannya berbentuk
	 * {@code "null-Form Tambahan"}.</p>
	 *
	 * <p>Dipakai antara lain oleh keluaran diagnostik {@code System.out.println} di
	 * {@code JenisCatatanSiswaAction}; jangan diandalkan sebagai format yang stabil.</p>
	 *
	 * @return gabungan id dan nama, dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama kategori; wajib diisi &amp; unik per instalasi. Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas kategori; boleh {@code null}. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Penanda baris bawaan sistem yang tidak boleh dihapus. Lihat {@link #getDefaultData()}. */
	private Boolean defaultData;
	/** Bendera aktif/nonaktif kategori pada formulir. Lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Nomor urut tampil seksi pada formulir. Lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Cakupan sekolah; {@code null} berarti berlaku untuk semua. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Cakupan yayasan; {@code null} berarti berlaku untuk semua. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/**
	 * Memastikan minimal ada SATU kategori bawaan di database, dan membuatnya bila belum ada
	 * (auto-seed).
	 *
	 * <p>Alurnya: mencari baris pertama dengan {@code defaultData = true}. Bila tidak ketemu,
	 * membuat baris baru bernama {@code "Form Tambahan"} (keterangan sama), menandainya
	 * {@code defaultData = true}, lalu <b>membuka transaksi sendiri</b>, {@code save}, dan
	 * {@code commit}. Terakhir &mdash; baik baris baru dibuat maupun tidak &mdash;
	 * {@link HibernateUtil#closeSession()} dipanggil.</p>
	 *
	 * <p><b>Pemanggil nyata:</b> satu-satunya adalah
	 * {@code ais.action.master.sekolah.ParameterTambahanCatatanSiswaAction.doAfterCompose()}
	 * &mdash; artinya baris bawaan lahir saat layar master "Parameter Tambahan Catatan Siswa"
	 * pertama kali dibuka, BUKAN saat layar kelompoknya sendiri dibuka. Layar
	 * {@code KelompokParameterTambahanCatatanSiswaAction} tidak pernah memanggil method ini.</p>
	 *
	 * <p><b>Efek samping berat.</b> Method ini menulis ke database dan MENUTUP session milik
	 * request yang sedang berjalan. Setiap kode yang masih memegang entity dari session tersebut
	 * akan mendapat {@code LazyInitializationException}/proxy mati sesudahnya. Karena dipanggil
	 * dari {@code doAfterCompose()}, biayanya dibayar SETIAP KALI layar dibuka (satu query
	 * tambahan per kunjungan), meski umumnya tidak menulis apa-apa.</p>
	 *
	 * <p><b>Kuirk cakupan multi-tenant.</b> Baris bawaan lahir tanpa {@link #getSekolah()} maupun
	 * {@link #getYayasan()} (keduanya {@code null}). Layar daftar kelompok memang menampilkannya
	 * (filternya berbentuk {@code OR isNull(...)}), tetapi daftar centang di
	 * {@code JenisCatatanSiswaAction.initKelompokParameterTambahanCatatanSiswa()} memakai syarat
	 * yang berbeda: kelompok hanya lolos bila {@code yayasan}/{@code sekolah}-nya TIDAK null dan
	 * cocok dengan konteks pengguna. Akibatnya, pada instalasi yang konteks sekolahnya terisi
	 * ({@code SekolahUtil.getSekolah()} mengembalikan sekolah), kategori bawaan "Form Tambahan"
	 * <b>tidak pernah muncul</b> sebagai pilihan centang, sehingga tidak bisa dipakai pada
	 * formulir Catatan Siswa mana pun sampai admin mengisi cakupannya secara manual.</p>
	 *
	 * <p><b>Nol gerbang hak akses.</b> Method dipanggil sebelum pemeriksaan hak apa pun; lebih
	 * jauh, {@code ParameterTambahanCatatanSiswaAction} sama sekali tidak memanggil
	 * {@code CommonPrivilages.checkPrevilages(...)} (lihat catatan pada
	 * {@link #getNomorUrut()}).</p>
	 *
	 * @return baris kategori bawaan yang sudah ada, atau baris yang baru saja dibuat; tidak pernah
	 *         {@code null}
	 */
	public static KelompokParameterTambahanCatatanSiswa checkCreateDefault() {
		Session session = HibernateUtil.currentNativeSession();
		KelompokParameterTambahanCatatanSiswa kelompokParameterTambahanCatatanSiswa = (KelompokParameterTambahanCatatanSiswa) session
				.createCriteria(KelompokParameterTambahanCatatanSiswa.class).add(Restrictions.eq("defaultData", true))
				.setMaxResults(1).uniqueResult();
		if (kelompokParameterTambahanCatatanSiswa == null) {
			kelompokParameterTambahanCatatanSiswa = new KelompokParameterTambahanCatatanSiswa();
			kelompokParameterTambahanCatatanSiswa.setDefaultData(true);
			kelompokParameterTambahanCatatanSiswa.setNama("Form Tambahan");
			kelompokParameterTambahanCatatanSiswa.setKeterangan("Form Tambahan");
			session.getTransaction().begin();
			session.save(kelompokParameterTambahanCatatanSiswa);
			session.getTransaction().commit();
		}

		HibernateUtil.closeSession();
		return kelompokParameterTambahanCatatanSiswa;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang diisi jam
	 * aplikasi saat konstruksi. Pemanggil dari kode aplikasi (mis.
	 * {@code KelompokParameterTambahanCatatanSiswaAction.onAdd()}) wajib mengisi {@link #setNama(String)}
	 * sebelum menyimpan karena kolomnya {@code NOT NULL}.</p>
	 */
	public KelompokParameterTambahanCatatanSiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan database ({@code IDENTITY}); {@code insertable = false} sehingga nilai yang
	 * diset manual tidak akan ikut dikirim pada {@code INSERT}. Bernilai {@code null} untuk objek
	 * yang belum pernah disimpan &mdash; kondisi ini dipakai layar master untuk membedakan modus
	 * "Tambah" dari "Ubah".</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris ini. Tanpa validasi.
	 *
	 * <p>Umumnya hanya dipanggil Hibernate. Mengubah id pada objek yang sudah terkelola session
	 * akan merusak identitas entity.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama kategori, sudah di-{@code trim}.
	 *
	 * <p>Inilah teks yang tampil sebagai judul seksi pada formulir Catatan Siswa (dirender
	 * {@code ParameterTambahanCatatanSiswaListener}) dan sebagai label checkbox pada layar
	 * Jenis Catatan Siswa.</p>
	 *
	 * <p><b>Perhatikan:</b> {@code trim} dilakukan pada nilai yang DIKEMBALIKAN saja &mdash; field
	 * dan kolom database tetap menyimpan spasi aslinya. Karena kolomnya {@code nullable = false},
	 * hasil {@code null} praktis hanya terjadi pada objek yang belum tersimpan.</p>
	 *
	 * @return nama kategori tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama kategori. Tanpa validasi maupun {@code trim}.
	 *
	 * <p>Keunikan nama TIDAK ditegakkan constraint database, melainkan hanya oleh pemeriksaan di
	 * aplikasi ({@code KelompokParameterTambahanCatatanSiswaAction
	 * .checkNamaKelompokParameterTambahanCatatanSiswa()}, memakai perbandingan {@code eq} persis
	 * atas nilai yang sudah di-{@code trim} form). Duplikat masih mungkin lolos lewat jalur non-UI,
	 * lewat perbedaan besar-kecil huruf, atau lewat spasi tepi.</p>
	 *
	 * @param nama nama kategori baru; {@code null} diterima entity tetapi akan ditolak database
	 *             saat {@code INSERT}/{@code UPDATE}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas kategori.
	 *
	 * <p><b>Kuirk penting &mdash; override ini MEMBALIK kontrak base class.</b>
	 * {@code GeneralValueObject.getKeterangan()} menormalkan {@code null} menjadi {@code ""} dan
	 * secara eksplisit menjanjikan hasil non-null. Versi di sini mengembalikan field mentah apa
	 * adanya, sehingga <b>bisa {@code null}</b> (kolomnya memang {@code nullable = true}). Kode
	 * yang mengandalkan janji base class akan {@code NullPointerException}.</p>
	 *
	 * <p>Nilai ini dirender apa adanya ke kolom "Keterangan" pada grid daftar kelompok.</p>
	 *
	 * @return keterangan kategori, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas kategori. Tanpa validasi.
	 *
	 * @param keterangan keterangan baru; {@code null} diterima dan akan terbaca kembali sebagai
	 *                   {@code null} (lihat {@link #getKeterangan()})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan penanda "baris bawaan sistem", dengan nilai bawaan {@code false}.
	 *
	 * <p>Hanya baris hasil {@link #checkCreateDefault()} yang bernilai {@code true}. Dipakai layar
	 * master untuk <b>menyembunyikan tombol Hapus</b>
	 * ({@code button.setVisible(delete && !...getDefaultData())}) sehingga kategori bawaan tidak
	 * bisa dihapus lewat UI.</p>
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method MENULIS {@code false} ke field
	 * sebelum mengembalikannya. Pada entity ber-<i>property access</i> dengan
	 * {@code dynamicUpdate = true} dan {@code @Audited}, sekadar membaca baris lama (mis. hasil
	 * migrasi yang kolomnya {@code NULL}) di dalam session aktif dapat memicu {@code UPDATE}
	 * sungguhan beserta revisi Envers &mdash; padahal pengguna tidak mengubah apa pun.</p>
	 *
	 * <p>Tidak ada anotasi {@code @Column}: pemetaannya mengandalkan konvensi nama bawaan
	 * Hibernate.</p>
	 *
	 * @return {@code true} bila baris ini kategori bawaan sistem; tidak pernah {@code null}
	 */
	public Boolean getDefaultData() {
		if (defaultData == null) {
			defaultData = false;
		}
		return defaultData;
	}

	/**
	 * Menyetel penanda "baris bawaan sistem". Tanpa validasi.
	 *
	 * <p>Tidak pernah dipanggil dari layar master mana pun &mdash; satu-satunya pemanggil adalah
	 * {@link #checkCreateDefault()}. Menyetelnya {@code false} pada baris bawaan akan membuat
	 * tombol Hapus muncul dan kategori bawaan bisa dilenyapkan; menyetelnya {@code true} pada
	 * baris kedua membuat instalasi punya lebih dari satu "kategori bawaan", yang tidak diantisipasi
	 * {@link #checkCreateDefault()} (method itu hanya mengambil hasil pertama).</p>
	 *
	 * @param defaultData penanda baru; {@code null} diterima dan akan dinormalkan
	 *                    {@code false} saat dibaca
	 */
	public void setDefaultData(Boolean defaultData) {
		this.defaultData = defaultData;
	}

	/**
	 * Mengembalikan bendera aktif kategori, dengan nilai bawaan {@code true}.
	 *
	 * <p>Kategori nonaktif disaring keluar dari formulir Catatan Siswa oleh query perakit form
	 * ({@code Restrictions.eq("kelompokParameterTambahanCatatanSiswa.aktif", true)} di
	 * {@code ParameterTambahanCatatanSiswaListener} dan {@code CatatanSiswaAction}), sehingga
	 * seluruh seksinya lenyap dari tampilan. Data isian yang sudah tersimpan TIDAK ikut terhapus
	 * &mdash; hanya tidak lagi ditampilkan.</p>
	 *
	 * <p><b>Efek samping:</b> menulis {@code true} ke field bila masih {@code null}; lihat catatan
	 * {@code UPDATE} tak diminta pada {@link #getDefaultData()}. Bawaan {@code true} berarti baris
	 * lama yang kolomnya {@code NULL} dianggap AKTIF (fail-open), bukan tersembunyi.</p>
	 *
	 * @return {@code true} bila kategori aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menyetel bendera aktif kategori. Tanpa validasi.
	 *
	 * <p>Dipanggil dari checkbox "Aktif" di grid layar master. Checkbox tersebut <b>bergerbang
	 * benar</b> ({@code checkbox.setDisabled(!edit)} dengan {@code edit} berasal dari
	 * {@code CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE)}) &mdash; kontras dengan
	 * komponen nomor urut di baris yang sama; lihat {@link #getNomorUrut()}.</p>
	 *
	 * @param aktif bendera baru; {@code null} diterima dan akan dinormalkan {@code true} saat
	 *              dibaca
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil seksi kategori pada formulir, dengan nilai bawaan {@code 1}.
	 *
	 * <p><b>Efek samping:</b> menulis {@code 1} ke field bila masih {@code null}; lihat catatan
	 * {@code UPDATE} tak diminta pada {@link #getDefaultData()}. Baris {@code return} mengandung
	 * <b>ternary yang mubazir</b> ({@code nomorUrut == null ? 1 : nomorUrut}) &mdash; cabang
	 * {@code null} mustahil tercapai karena blok {@code if} di atasnya sudah mengisi field.</p>
	 *
	 * <p><b>Kolom ini praktis tidak pernah terisi lewat UI.</b> Formulir Tambah/Ubah kelompok
	 * ({@code KelompokParameterTambahanCatatanSiswaAction.init()}) hanya menyediakan isian
	 * <i>Nama&nbsp;Kelompok</i>, <i>Yayasan</i>, <i>Sekolah</i>, dan <i>Keterangan</i> &mdash; tidak
	 * ada isian nomor urut. Satu-satunya jalan mengubahnya adalah {@code Intbox} kecil di grid
	 * daftar. Konsekuensi bug-nya dijelaskan pada {@link #compareTo(GeneralValueObject)}.</p>
	 *
	 * <p><b>CATATAN KEAMANAN &mdash; broken access control (terverifikasi di kode).</b> Pada
	 * renderer grid {@code KelompokParameterTambahanCatatanSiswaAction
	 * .KelompokParameterTambahanCatatanSiswaRenderer.render()}, seluruh kontrol lain dijaga hak
	 * akses: checkbox "Aktif" memakai {@code setDisabled(!edit)}, tombol Ubah memakai
	 * {@code setVisible(edit)}, tombol Hapus memakai {@code setVisible(delete && ...)}. Namun
	 * {@code Intbox} nomor urut dibuat <b>tanpa guard apa pun</b> &mdash; tidak
	 * {@code setDisabled}, tidak {@code setVisible}, tidak {@code setReadonly} &mdash; dan
	 * listener {@code onChange}-nya langsung memanggil
	 * {@code Common.refreshSaveOrUpdate(...)}. Pengguna yang hanya punya hak READ karena itu dapat
	 * MENGUBAH DAN MENYIMPAN urutan seksi formulir. Ini instance ke sekian dari pola template yang
	 * sama di seluruh keluarga {@code KelompokParameterTambahan*Action}.</p>
	 *
	 * <p>Lapis di bawahnya lebih parah lagi:
	 * {@code ais.action.master.sekolah.ParameterTambahanCatatanSiswaAction} meng-<i>hardcode</i>
	 * {@code private boolean edit = true;} dan {@code private boolean delete = true;} serta
	 * <b>tidak pernah memanggil {@code CommonPrivilages.checkPrevilages(...)} sama sekali</b>
	 * &mdash; termasuk tidak ada gerbang READ di {@code doAfterCompose()}. Siapa pun yang dapat
	 * membuka layar itu bisa mengubah dan menghapus pemetaan parameter. Perilaku ini identik
	 * dengan padanan versi PT-nya.</p>
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		if (nomorUrut == null) {
			nomorUrut = 1;
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil kategori. Tanpa validasi (nilai negatif maupun duplikat
	 * diterima).
	 *
	 * <p>Satu-satunya pemanggil dari UI adalah listener {@code onChange} pada {@code Intbox} grid
	 * daftar &mdash; komponen yang tidak bergerbang hak akses; lihat catatan keamanan pada
	 * {@link #getNomorUrut()}.</p>
	 *
	 * @param nomorUrut nomor urut baru; {@code null} diterima dan akan dinormalkan {@code 1} saat
	 *                  dibaca
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Membandingkan dua kategori <b>hanya</b> berdasarkan {@link #getNomorUrut()}.
	 *
	 * <p><b>Versi yang DIPANGKAS.</b> Implementasi induk
	 * {@code GeneralValueObject.compareTo(...)} membandingkan berjenjang
	 * ({@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr; {@code keterangan}) dan
	 * dibungkus {@code try/catch}. Override di sini memangkasnya jadi satu baris: tanpa fallback,
	 * tanpa penjagaan {@code null}, dan tanpa {@code try/catch}. Argumen di-<i>cast</i> paksa ke
	 * {@code KelompokParameterTambahanCatatanSiswa}, sehingga membandingkan dengan
	 * {@link GeneralValueObject} jenis lain melempar {@code ClassCastException} (bukan
	 * mengembalikan 0 seperti versi induk yang menelan exception).</p>
	 *
	 * <p><b>BUG PENCIUTAN SENYAP {@code TreeSet} &mdash; ini kondisi DEFAULT, bukan kasus
	 * langka.</b> Karena {@code nomorUrut} tidak pernah diisi formulir Tambah/Ubah (lihat
	 * {@link #getNomorUrut()}), SEMUA kategori baru bernilai {@code 1}. Bagi {@code TreeSet},
	 * {@code compareTo() == 0} berarti "elemen yang sama", sehingga
	 * {@code CatatanSiswaAction} yang menyalin koleksi kategori ke
	 * {@code new TreeSet<KelompokParameterTambahanCatatanSiswa>()} akan <b>menyisakan tepat SATU
	 * kategori</b> dan membuang sisanya <b>tanpa error apa pun</b>. Akibatnya seksi-seksi field
	 * kustom lain tidak pernah dirender pada formulir Catatan Siswa. Gejalanya menyesatkan: data
	 * pemetaan di layar master terlihat lengkap, tetapi formulirnya kekurangan seksi.</p>
	 *
	 * <p>Bug ini berpasangan langsung dengan celah hak akses pada {@link #getNomorUrut()}: karena
	 * {@code Intbox} nomor urut tidak dijaga, pengguna READ-saja bisa menyamakan nomor urut dua
	 * kategori dan MELENYAPKAN salah satu seksi formulir dari tampilan SEMUA pengguna.</p>
	 *
	 * @param arg0 kategori pembanding; harus instance
	 *             {@code KelompokParameterTambahanCatatanSiswa}, tidak boleh {@code null}
	 * @return negatif/nol/positif sesuai perbandingan {@link #getNomorUrut()}
	 * @throws ClassCastException bila {@code arg0} bukan
	 *         {@code KelompokParameterTambahanCatatanSiswa}
	 * @throws NullPointerException bila {@code arg0} {@code null}
	 */
	@Override
	public int compareTo(GeneralValueObject arg0) {
		return getNomorUrut().compareTo(((KelompokParameterTambahanCatatanSiswa) arg0).getNomorUrut());
	}

	/**
	 * Menyetel cakupan yayasan kategori ini.
	 *
	 * <p><b>Kuirk:</b> yayasan yang {@code null} <i>atau</i> yang belum punya {@code id} (mis.
	 * item pilihan "Semua" pada combobox) disimpan sebagai {@code null} &mdash; artinya "berlaku
	 * untuk semua yayasan". Objek transien dengan id kosong sengaja dibuang agar Hibernate tidak
	 * mencoba men-{@code cascade} penyimpanan yayasan baru.</p>
	 *
	 * @param yayasan yayasan cakupan; {@code null} atau objek tanpa id berarti "semua"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan yayasan yang menaungi kategori ini, atau {@code null} bila berlaku untuk semua.
	 *
	 * <p>Relasi lazy; proxy diselesaikan lewat {@link GeneralValueObject#check(Object)} sebelum
	 * dikembalikan.</p>
	 *
	 * <p><b>Efek samping &mdash; getter yang MENULIS.</b> Bila {@link #getSekolah()} tidak
	 * {@code null}, field {@code yayasan} DITIMPA dengan {@code getSekolah().getYayasan()}. Jadi
	 * kolom {@code yayasan} bukan data yang berdiri sendiri melainkan turunan dari sekolah, dan
	 * penimpaannya terjadi saat baris sekadar DIBACA. Pada entity ber-<i>property access</i>
	 * dengan {@code dynamicUpdate = true} dan {@code @Audited}, membuka layar daftar sudah cukup
	 * untuk memicu {@code UPDATE} kolom ini beserta revisi Envers palsu &mdash; bila nilai yang
	 * tersimpan berbeda dari yayasan sekolahnya. Sifatnya <i>self-healing</i> (nilai selalu
	 * dikoreksi ke yayasan sekolah), tetapi juga berarti yayasan yang disetel manual berbeda dari
	 * sekolahnya <b>tidak akan pernah bertahan</b>.</p>
	 *
	 * <p>Dipakai sebagai filter pada layar daftar kelompok ({@code initCriteria()}, berbentuk
	 * {@code isNull(...) OR eq(...)}) dan &mdash; dengan syarat yang lebih ketat &mdash; pada
	 * daftar centang di {@code JenisCatatanSiswaAction}; lihat catatan asimetri pada
	 * {@link #checkCreateDefault()}.</p>
	 *
	 * @return yayasan cakupan, atau {@code null} bila kategori berlaku untuk semua yayasan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		if (getSekolah() != null) {
			yayasan = getSekolah().getYayasan();
		}
		return yayasan;
	}

	/**
	 * Mengembalikan sekolah yang menaungi kategori ini, atau {@code null} bila berlaku untuk semua.
	 *
	 * <p>Relasi lazy; proxy diselesaikan lewat {@link GeneralValueObject#check(Object)} sebelum
	 * dikembalikan. Hasil {@code check(...)} ditugaskan kembali ke field &mdash; itu memang pola
	 * baku resolusi proxy di seluruh entity AIS, bukan efek samping yang tak disengaja.</p>
	 *
	 * <p>Kolom ini adalah sumber kebenaran cakupan multi-tenant kategori: {@link #getYayasan()}
	 * menurunkan nilainya dari sini. Nilai {@code null} berarti kategori berlaku untuk semua
	 * sekolah &mdash; kondisi bawaan baris hasil {@link #checkCreateDefault()}, dengan konsekuensi
	 * yang dicatat di sana.</p>
	 *
	 * @return sekolah cakupan, atau {@code null} bila kategori berlaku untuk semua sekolah
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menyetel cakupan sekolah kategori ini.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setYayasan(Yayasan)}, sekolah yang {@code null} atau
	 * yang belum punya {@code id} (mis. pilihan "Semua" pada combobox) disimpan sebagai
	 * {@code null} &mdash; artinya "berlaku untuk semua sekolah".</p>
	 *
	 * <p>Perhatikan bahwa menyetel sekolah secara tidak langsung juga menentukan yayasan, karena
	 * {@link #getYayasan()} menurunkan nilainya dari sekolah ini.</p>
	 *
	 * @param sekolah sekolah cakupan; {@code null} atau objek tanpa id berarti "semua"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}
}
