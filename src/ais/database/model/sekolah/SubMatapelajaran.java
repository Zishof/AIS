package ais.database.model.sekolah;
// Generated 30 Sep 18 6:57:43 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.GeneralValueObject;

/**
 * Master <b>Sub Mata Pelajaran</b> — komponen/pecahan dari sebuah mata pelajaran pada modul
 * sekolah, dipetakan ke tabel {@code sekolah.sub_matapelajaran}.
 *
 * <h3>Peran yang TERVERIFIKASI</h3>
 * Satu baris entity ini adalah <b>nama sebuah bagian dari satu mata pelajaran</b> (relasi wajib
 * {@link #getMatapelajaran()} ke {@code Matapelajaran} versi sekolah). Isinya murni label:
 * {@code nama} + {@code keterangan} + saklar {@code aktif} + penanda tenant. Peran itu
 * dipastikan dari sumber berikut di dalam repo, bukan dari nama kelasnya:
 * <ol>
 * <li><b>Judul dan label layar</b> — {@code ais.action.master.sekolah.SubMatapelajaranAction#init(
 * SubMatapelajaran)} menyetel judul dialog menjadi <i>"Tambah Sub Matapelajaran"</i> /
 * <i>"Ubah Sub Matapelajaran"</i> dengan isian <i>"Nama Sub Matapelajaran *"</i>,
 * <i>"Yayasan *"</i>, <i>"Sekolah *"</i>, <i>"Mata Pelajaran *"</i>, dan <i>"Keterangan"</i>.
 * Kolom grid daftarnya: <i>Nama Sub Matapelajaran</i>, <i>Nama Matapelajaran</i>, <i>Sekolah</i>,
 * <i>Keterangan</i>, <i>Aktif</i>.</li>
 * <li><b>Selalu tergantung pada satu mata pelajaran</b> — kolom {@code matapelajaran} berstatus
 * {@code nullable = false} dan combo pemilihnya wajib diisi ({@code onSave} menolak dengan pesan
 * <i>"Matapelajaran harus diisi"</i>). Jadi sub mata pelajaran tidak pernah berdiri sendiri.</li>
 * <li><b>Dipakai sebagai dimensi tambahan pada jadwal mengajar</b> — dua entity konsumen
 * menyimpan referensi ke entity ini sebagai <i>deretan slot</i>:
 * {@code GuruMengajar} punya <b>25 slot</b> ({@code sub_matapelajaran},
 * {@code sub_matapelajaran_2} … {@code sub_matapelajaran_25}) dan {@code JadwalPelajaran} punya
 * <b>12 slot</b> ({@code sub_matapelajaran} … {@code sub_matapelajaran_12}). Slot ke-N sejajar
 * dengan pasangan {@code hari}/{@code jamPelajaran} ke-N pada baris yang sama, sehingga makna
 * praktisnya: <i>"pada pertemuan ke-N, mata pelajaran ini diajarkan pada bagian/komponen
 * apa"</i>.</li>
 * <li><b>Penyaringan pilihan</b> — pada {@code GuruMengajarAction} dan
 * {@code JadwalPelajaranAction}, isi combo sub mata pelajaran diambil dengan kriteria
 * {@code (aktif IS NULL OR aktif = true) AND matapelajaran = <mapel yang sedang dipilih>} diurut
 * menaik berdasarkan {@code nama}. Bila mata pelajaran terpilih <b>tidak punya sub mata pelajaran
 * sama sekali</b>, seluruh combo slot itu di-{@code setVisible(false)} — jadi fitur ini memang
 * dirancang opsional per mata pelajaran.</li>
 * </ol>
 *
 * <p>Contoh pemakaian yang konsisten dengan struktur di atas: memecah "Bahasa Indonesia" menjadi
 * "Membaca"/"Menulis", atau "IPA Terpadu" menjadi "Fisika"/"Biologi"/"Kimia" agar jadwal dan
 * penugasan guru bisa menunjuk komponen yang berbeda pada jam yang berbeda. Perlu ditegaskan:
 * pembagian itu <b>tidak</b> memiliki bobot, KKM, jenis penilaian, maupun kolom nilai sendiri —
 * semua itu tetap milik {@code Matapelajaran} (lihat {@code kkm},
 * {@code terdapatNilaiKeterampilan}, {@code jenisPenilaian}, {@code kelompokMatapelajaran} di
 * sana). Entity ini murni label penjadwalan.</p>
 *
 * <h3>Padanan di modul lain</h3>
 * Sisi perguruan tinggi tidak memiliki padanan langsung; kerabat terdekatnya adalah
 * {@code ais.database.model.Matakuliah} yang memecah beban lewat entity lain. Sisa istilah PT
 * masih terlihat pada layar sekolah ini: label filter pencarian berbunyi <i>"Nama Sub MK"</i> dan
 * <i>"Nama MK"</i>, pesan validasinya <i>"Nama Sub Mk harus diisi"</i>, dan judul statis pada
 * berkas {@code sub_matapelajaran.zul} masih <i>"Tambah Jenis Penilaian"</i> (tidak terlihat
 * pengguna karena selalu ditimpa saat {@code init(...)} berjalan). Semuanya sisa salin-tempel,
 * bukan indikasi fungsi lain.
 *
 * <h3>Layar pengelola dan konsekuensi hak akses</h3>
 * Layar CRUD-nya adalah {@code /pages/master/sekolah/sub_matapelajaran.zul}
 * ({@code SubMatapelajaranAction}). <b>Halaman itu tidak terdaftar sebagai menu mandiri</b> —
 * satu-satunya jalan masuk adalah tab di dalam layar master Mata Pelajaran
 * ({@code MatapelajaranAction#onSubMatapelajaran(Event)} menyisipkannya lewat {@code MyInclude}).
 * Karena {@code CommonPrivilages.checkPrevilages(...)} selalu memakai
 * {@code Common.getCurrentMenu()}, hak CREATE/UPDATE/DELETE yang berlaku di tab ini sesungguhnya
 * adalah hak pada menu <b>Mata Pelajaran</b>, bukan hak khusus sub mata pelajaran. Ini mekanisme
 * <i>pewarisan hak lewat menu</i> yang sama dengan yang sudah tercatat pada {@code PaketPsb};
 * di sini akibatnya jauh lebih ringan karena induk dan anak memang satu domain, tetapi tetap
 * perlu diingat saat menyusun peran: tidak ada cara memberi hak ubah mata pelajaran <i>tanpa</i>
 * sekaligus memberi hak hapus seluruh sub mata pelajarannya.
 *
 * <h3>Cakupan tenant (sekolah/yayasan)</h3>
 * {@code SubMatapelajaranAction#initCriteria(boolean)} <b>tidak memiliki penyaring tenant
 * bawaan</b>: penyaringan hanya terjadi bila combo {@code searchsekolah}/{@code searchyayasan}
 * kebetulan terisi, selebihnya dipakai {@code Restrictions.sqlRestriction("1=1")}. Pengisian
 * combo itu dikerjakan {@code InitComboUtil.initYayasanDanSekolahDanSemua(...)} yang
 * <b>gagal-terbuka</b>: seluruh badannya dibungkus {@code try/catch} yang menelan exception, dan
 * bila pengguna tidak punya sekolah/yayasan melekat serta tidak ada konteks sekolah aktif maka
 * tidak ada apa pun yang terpilih. Pada kondisi itu daftar menampilkan sub mata pelajaran
 * SELURUH sekolah dan yayasan, lengkap dengan tombol Ubah/Hapus dan centang Aktif per baris.
 * Pola ini identik dengan keluarga temuan fail-open yang sudah tercatat pada audit repo ini.
 * Keparahannya di sini <b>rendah</b> — isinya metadata katalog (nama komponen mapel), bukan data
 * pribadi — namun permukaan <b>tulis</b> lintas tenant tetap nyata.
 *
 * <p>Sebaliknya, jalur pemakaian hilir aman secara tidak sengaja: combo pada
 * {@code GuruMengajarAction}/{@code JadwalPelajaranAction} menyaring dengan
 * {@code matapelajaran = <mapel terpilih>}, sedangkan {@code Matapelajaran.sekolah} sendiri
 * {@code nullable = false}, sehingga daftar sub mata pelajaran ikut terkurung pada satu sekolah
 * tanpa filter tenant eksplisit.</p>
 *
 * <h3>Bug hilir yang TERVERIFIKASI — pilihan sub mata pelajaran terhapus senyap</h3>
 * Pada ketiga titik penyusun formulir jadwal ({@code GuruMengajarAction} untuk 25 slot, dan
 * {@code JadwalPelajaranAction} pada dua layar untuk 12 slot), combo sub mata pelajaran memang
 * <b>diisi</b> lewat {@code Common.insertComboItems(...)}, tetapi <b>nilai yang sudah tersimpan
 * tidak pernah dipilih ulang</b> — tidak ada satu pun pemanggilan
 * {@code Common.selectComboItem(..., subMatapelajaranN, ...)} di seluruh repo, padahal pola itu
 * dipakai konsisten untuk tetangganya ({@code jamPelajaran}, {@code masaJadwalPelajaran},
 * {@code matapelajaran}, {@code hari}). Sementara itu penyimpanannya berbunyi
 * {@code setSubMatapelajaranN(combo.getSelectedItem() == null ? null : …)}. Gabungan keduanya
 * berarti: <b>setiap kali baris jadwal atau penugasan mengajar disimpan ulang — walau yang
 * diubah hanya jam atau guru — seluruh kolom sub mata pelajaran ditulis kembali menjadi
 * {@code NULL}</b>. Pengguna tidak menerima peringatan apa pun; nilai lama hanya hilang, dan
 * karena entity induknya ber-{@code @Audited} kehilangan itu ikut terekam sebagai revisi Envers
 * yang tampak sah. Ini bug penulis, bukan bug entity ini — dicatat di sini karena entity inilah
 * satu-satunya tempat kaitannya terlihat utuh.
 *
 * <h3>Status pemakaian hilir: terisi tetapi nyaris tidak pernah dibaca</h3>
 * Penelusuran seluruh repo atas {@code getSubMatapelajaran*()} menemukan <b>satu-satunya</b>
 * pembaca: label baca-saja di {@code JadwalPelajaranAction} yang menampilkan nama sub mata
 * pelajaran ketika baris jadwal dimiliki guru lain. Tidak ada laporan, rapor, rekap nilai,
 * cetakan JasperReports, maupun REST API yang membacanya. Ke-25 slot pada {@code GuruMengajar}
 * bahkan tidak punya pembaca sama sekali. Jadi lapis ini praktis <b>yatim fungsional di sisi
 * hilir</b> — mirip pola yang sudah tercatat pada {@code NilaiKegiatanKesiswaan}: datanya
 * dikumpulkan lewat UI lengkap, tetapi tidak pernah memengaruhi perhitungan apa pun. Konsekuensi
 * praktis: bug penghapusan senyap di atas saat ini <b>tidak terlihat</b> di laporan mana pun,
 * sehingga bisa berjalan lama tanpa terdeteksi.
 *
 * <h3>Bug layar master yang TERVERIFIKASI — filter "Nama MK" tidak berfungsi sendirian</h3>
 * Pada {@code SubMatapelajaranAction#initCriteria(boolean)}, penerapan filter <i>"Nama MK"</i>
 * ({@code searchnamamk}) bersarang di dalam {@code if (!searchnama.getValue().trim().isEmpty())}
 * — yaitu di dalam pemeriksaan isian <i>"Nama Sub MK"</i>. Akibatnya mengetik nama mata pelajaran
 * saja (tanpa mengisi nama sub mata pelajaran) tidak berpengaruh sama sekali: daftar tetap
 * menampilkan seluruh baris. Penjaga luar itu seharusnya menguji {@code searchnamamk}. Ketika
 * kedua isian terisi barulah alias {@code matapelajaran} dibuat dan filternya dipasang.
 *
 * <h3>Hal non-obvious yang wajib diketahui sebelum mengubah berkas ini</h3>
 * <ul>
 * <li><b>Field induk sengaja dideklarasikan ulang.</b> {@link GeneralValueObject} bukan
 * {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate
 * TIDAK memetakan properti apa pun miliknya. Maka {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah}, dan juga {@code nama}/{@code keterangan} <b>harus</b> dideklarasikan
 * ulang di sini agar terpetakan. Ini KEHARUSAN TEKNIS, bukan duplikasi yang bisa
 * "dibersihkan".</li>
 * <li><b>Akibat sampingannya: field induk selamanya kosong.</b> Field lokal {@code nama} dan
 * {@code keterangan} membayangi ({@code shadow}) field bernama sama di induk, sehingga kode yang
 * membaca field induk secara langsung (bukan lewat getter) selalu mendapat {@code null}. Saat ini
 * aman: {@code GeneralValueObject#toString()} dan {@code compareTo(...)} mengaksesnya lewat
 * getter yang di-override kelas ini.</li>
 * <li><b>Kontrak {@code getKeterangan()} DIBALIK.</b> Lihat {@link #getKeterangan()}.</li>
 * <li><b>{@code getYayasan()} menurunkan ulang nilainya setiap kali dibaca.</b> Lihat
 * {@link #getYayasan()} — nilai yang disetel {@link #setYayasan(Yayasan)} akan ditimpa selama
 * {@code sekolah} terisi.</li>
 * <li><b>Kolom {@code aktif} tidak pernah ditulis saat pembuatan.</b>
 * {@code SubMatapelajaranAction#onSave(Event)} tidak menyentuh {@code aktif} sama sekali; baris
 * baru tersimpan dengan {@code NULL}. Berbeda dengan beberapa entity lain yang punya bug
 * "baris hantu" karena hal ini, di sini <b>tidak ada bug</b>: {@link #getAktif()} membaca
 * {@code NULL} sebagai {@code true}, dan SELURUH query penyaring — layar master maupun kedua
 * layar jadwal — memakai {@code (aktif IS NULL OR aktif = true)}. Kesepakatan itu konsisten di
 * semua titik; jangan menyederhanakannya menjadi {@code aktif = true} tanpa lebih dulu
 * mem-<i>backfill</i> kolomnya.</li>
 * <li><b>Tidak ada penyemaian (seed) bawaan.</b> Entity ini terdaftar di
 * {@code ais.common.InitData} hanya untuk <i>pemanasan cache</i> ({@code InitDataHelper.initData}
 * memuat baris yang sudah ada) dan di {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} agar tidak
 * dibuang oleh pembersihan cache berkala. Tidak ada baris default yang dibuat: instalasi baru
 * dimulai dengan tabel kosong, sehingga combo sub mata pelajaran pada layar jadwal
 * <b>tersembunyi</b> sampai admin mengisinya sendiri.</li>
 * <li><b>Konstruktor dua argumen tidak pernah dipakai.</b> Lihat
 * {@link #SubMatapelajaran(long, String)}.</li>
 * </ul>
 *
 * <h3>Verifikasi pola arsitektur berulang milik repo ini</h3>
 * <ul>
 * <li><b>Getter destruktif/write-back</b> — <b>ADA, dua tingkat.</b> {@link #getMatapelajaran()}
 * dan {@link #getSekolah()} menulis balik hasil {@code check(...)} ke field-nya (ringan, sekadar
 * de-proxy), sedangkan {@link #getYayasan()} benar-benar <i>menurunkan ulang</i> isi field
 * {@code yayasan} dari {@code sekolah.getYayasan()} pada setiap pembacaan.</li>
 * <li><b>{@code getKeterangan()} membalik kontrak induk</b> — <b>ADA.</b> Induk menjamin tidak
 * pernah {@code null}; override di sini mengembalikan nilai mentah sehingga bisa {@code null}.
 * Lihat {@link #getKeterangan()}.</li>
 * <li><b>{@code compareTo()} dipangkas</b> — <b>TIDAK ADA.</b> Kelas ini tidak meng-override
 * {@code compareTo}/{@code equals}/{@code hashCode}; seluruhnya diwarisi apa adanya. Yang ada
 * hanya efek tidak langsung dari poin sebelumnya (cabang {@code keterangan} pada
 * {@code compareTo} induk kehilangan jaminan non-null).</li>
 * <li><b>Penciutan {@code TreeSet}</b> — <b>TIDAK ADA.</b> Entity ini tidak memiliki koleksi
 * apa pun; seluruh relasinya {@code @ManyToOne} tunggal.</li>
 * <li><b>Fail-open cakupan tenant</b> — <b>ADA, di layar masternya</b> (bukan di entity ini).
 * Rinciannya pada bagian "Cakupan tenant" di atas.</li>
 * <li><b>Kolom {@code aktif} yang tak pernah ditulis layar master</b> — <b>ADA sebagai fakta,
 * TIDAK sebagai bug</b>, karena semua pembacanya sepakat memperlakukan {@code NULL} sebagai
 * aktif. Lihat {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Persistensi</h3>
 * Ber-{@code @Audited} (riwayat perubahan disimpan Hibernate Envers ke skema audit) dan memakai
 * {@code dynamicInsert}/{@code dynamicUpdate} sehingga hanya kolom yang berubah yang ikut dalam
 * pernyataan SQL. Id memakai strategi {@code IDENTITY} — berurutan dan mudah ditebak, jadi jangan
 * pernah menjadikan id sebagai satu-satunya pembatas akses.
 *
 * @see ais.database.model.GeneralValueObject
 * @see Matapelajaran
 * @see GuruMengajar
 * @see JadwalPelajaran
 * @see Sekolah
 * @see Yayasan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "sub_matapelajaran", schema = "sekolah")
public class SubMatapelajaran extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan sejak berkas dibuat; jangan diubah karena
	 * instance entity ikut diserialisasi ke sesi ZK dan ke cache.
	 */
	private static final long serialVersionUID = 2662544030302108496L;
	/**
	 * Kunci utama baris, dibangkitkan basis data ({@code IDENTITY}). Dideklarasikan ulang di sini
	 * karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id pengguna terakhir yang mengubah baris ini; pendamping {@link #oleh}, diisi dari jalur
	 * yang sama.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir, dengan <b>validasi non-trivial</b>: argumen
	 * {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b> (method langsung
	 * {@code return} tanpa menyentuh field).
	 *
	 * <p>Akibatnya jejak audit hanya bisa diisi atau ditimpa, tidak pernah bisa dikosongkan
	 * kembali lewat setter ini. Perilaku sama persis dengan {@link #setOleh(String)} dan dengan
	 * pola yang dipakai seluruh entity turunan {@link GeneralValueObject}.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan <b>validasi non-trivial</b> yang sama
	 * dengan {@link #setOlehId(String)}: argumen {@code null} atau kosong/spasi diabaikan
	 * diam-diam sehingga jejak audit tidak dapat dihapus.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong/spasi saja
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil kontainer persistence TEPAT SEBELUM pernyataan
	 * UPDATE dikirim, dan meneruskan objek ini ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari pengguna sesi aktif serta memperbarui
	 * {@code tanggal_dirubah}.
	 *
	 * <p><b>Hanya UPDATE.</b> Tidak ada {@code @PrePersist}, sehingga baris yang baru dibuat
	 * mengandalkan nilai awal field {@code tanggal_dirubah} (disetel
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat objek dibuat) dan tidak pernah mendapat
	 * {@code oleh}/{@code olehId} dari jalur ini.</p>
	 *
	 * <p><b>Catatan tata letak:</b> baris kode ini memuat DUA deklarasi sekaligus — method
	 * {@code onUpdate()} dan field {@code tanggal_dirubah} — hasil penyisipan otomatis oleh
	 * perkakas migrasi. Jangan dipisah tanpa alasan; Javadoc ini mendokumentasikan keduanya.</p>
	 *
	 * <p>Perlu disadari bahwa satu klik centang "Aktif" pada grid daftar (lihat
	 * {@link #setAktif(Boolean)}) sudah cukup untuk memicu jalur ini, menimpa jejak audit, dan
	 * menciptakan satu revisi Envers baru.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Tanpa validasi; nilai {@code null} diterima apa adanya.
	 *
	 * <p>Dalam pemakaian normal setter ini tidak perlu dipanggil kode aplikasi — pengisiannya
	 * dikerjakan {@link #onUpdate()} pada setiap UPDATE.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}.
	 *
	 * <p>Tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor Java (field-nya
	 * diinisialisasi {@code WaktuUtil.getDate()}), tetapi bisa {@code null} untuk baris lama yang
	 * dimuat dari basis data sebelum kolom ini terisi.</p>
	 *
	 * @return waktu perubahan terakhir, atau {@code null} bila kolomnya kosong di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nama sub mata pelajaran, mis. "Membaca" atau "Fisika". Wajib diisi ({@code nullable = false})
	 * dan dipakai sebagai label combo pada layar jadwal serta kunci urut daftar.
	 */
	private String nama;
	/** Keterangan bebas; satu-satunya field yang benar-benar opsional pada formulir. */
	private String keterangan;
	/**
	 * Mata pelajaran induk yang dipecah oleh baris ini. Wajib ({@code nullable = false}) dan
	 * menjadi kunci penyaring seluruh combo sub mata pelajaran di layar jadwal.
	 */
	private Matapelajaran matapelajaran;
	/**
	 * Saklar aktif. {@code null} berarti aktif — lihat {@link #getAktif()}; layar master tidak
	 * pernah mengisinya saat pembuatan baris.
	 */
	private Boolean aktif;
	/** Sekolah pemilik baris (penanda tenant). Boleh {@code null} di skema, tetapi diwajibkan UI. */
	private Sekolah sekolah;
	/** Yayasan pemilik baris; diturunkan ulang dari {@link #sekolah} setiap kali dibaca. */
	private Yayasan yayasan;

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA. Juga dipakai
	 * {@code SubMatapelajaranAction#onAdd(Event)} untuk membuat objek kosong yang mengisi dialog
	 * "Tambah Sub Matapelajaran".
	 *
	 * <p>Efek samping yang perlu disadari: field {@code tanggal_dirubah} langsung terisi waktu
	 * saat ini melalui inisialisasi field, bukan lewat konstruktor ini.</p>
	 */
	public SubMatapelajaran() {
	}

	/**
	 * Konstruktor peninggalan hbm2java yang mengisi kunci utama dan nama sekaligus.
	 *
	 * <p><b>Tidak dipakai di mana pun</b> di dalam repo (penelusuran {@code new SubMatapelajaran(}
	 * hanya menemukan pemanggilan konstruktor kosong). Sebaiknya juga tidak dipakai kode baru:
	 * kolom {@code id} memakai strategi {@code IDENTITY} dan berstatus
	 * {@code insertable = false}, sehingga id yang diisi manual di sini akan diabaikan saat
	 * INSERT dan hanya membingungkan — objek tampak "punya id" padahal belum tersimpan, yang bisa
	 * menyesatkan pemeriksaan bergaya {@code getId() == null} untuk membedakan tambah dan ubah
	 * (persis pemeriksaan yang dipakai {@code SubMatapelajaranAction#init(SubMatapelajaran)} dan
	 * {@code onSave(Event)}).</p>
	 *
	 * @param id   nilai kunci utama yang diinginkan; diabaikan saat INSERT
	 * @param nama nama sub mata pelajaran
	 */
	public SubMatapelajaran(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Nilai {@code null} berarti objek belum pernah disimpan — inilah pembeda "tambah" vs
	 * "ubah" yang dipakai layar masternya.</p>
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
	 * Menyetel kunci utama. Tanpa validasi.
	 *
	 * <p>Dalam pemakaian normal tidak perlu dipanggil kode aplikasi: id dibangkitkan basis data.
	 * Menyetelnya manual pada objek baru membuatnya salah dikira baris lama.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama sub mata pelajaran.
	 *
	 * <p>Meng-override {@code GeneralValueObject#getNama()} agar membaca field lokal yang
	 * terpetakan Hibernate — tanpa override ini {@code toString()} dan {@code compareTo(...)}
	 * induk akan selalu melihat {@code null}. Berbeda dengan beberapa entity lain di modul ini,
	 * getter ini <b>tidak</b> melakukan penulisan balik apa pun: murni pengembalian nilai.</p>
	 *
	 * @return nama sub mata pelajaran, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama sub mata pelajaran. Tanpa validasi di tingkat entity.
	 *
	 * <p>Validasi wajib-isi hanya ada di UI ({@code SubMatapelajaranAction#onSave(Event)} menolak
	 * nilai kosong dengan pesan <i>"Nama Sub Mk harus diisi"</i>); kolomnya sendiri
	 * {@code nullable = false}, jadi menyetel {@code null} dari jalur non-UI baru gagal saat
	 * flush.</p>
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini.
	 *
	 * <p><b>Perhatian — kontrak induk DIBALIK.</b> {@code GeneralValueObject#getKeterangan()}
	 * menormalkan {@code null} menjadi {@code ""} dan menjamin tidak pernah mengembalikan
	 * {@code null}; override di sini mengembalikan field mentah, sehingga <b>bisa</b>
	 * {@code null}. Konsekuensi yang sudah diperiksa:</p>
	 * <ul>
	 * <li>Cabang {@code keterangan} pada {@code GeneralValueObject#compareTo(...)} kehilangan
	 * jaminan non-null-nya; untuk entity ini cabang tersebut praktis tak pernah tercapai karena
	 * {@code nama} selalu terisi.</li>
	 * <li>Renderer daftar ({@code SubMatapelajaranRenderer#render(...)}) memanggilnya langsung
	 * sebagai {@code new Label(getKeterangan())} — aman karena {@code Label} menerima
	 * {@code null}, tetapi pemanggil baru yang langsung merantai {@code .trim()} atau
	 * {@code .isEmpty()} akan terkena {@code NullPointerException}.</li>
	 * </ul>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi (berbeda dari kelas induk)
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Tanpa validasi; nilai {@code null} maupun string kosong
	 * diterima apa adanya (tidak dinormalkan — lihat {@link #getKeterangan()}).
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status aktif baris, dengan <b>nilai bawaan {@code true} untuk {@code null}</b>.
	 *
	 * <p>Ini penting karena {@code SubMatapelajaranAction#onSave(Event)} <b>tidak pernah</b>
	 * mengisi kolom {@code aktif} saat baris dibuat, sehingga sub mata pelajaran baru selalu
	 * tersimpan dengan {@code NULL}. Tanpa normalisasi ini, centang "Aktif" pada grid akan tampak
	 * kosong untuk setiap baris baru dan {@code checkbox.setChecked(...)} akan melempar
	 * {@code NullPointerException} saat auto-unboxing.</p>
	 *
	 * <p>Kesepakatan "{@code NULL} = aktif" berlaku <b>konsisten di seluruh pembaca</b>: layar
	 * master maupun kedua layar jadwal menyaring dengan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))}. Karena itu entity ini
	 * <b>tidak</b> mengalami bug "baris hantu" yang menimpa beberapa master lain di modul sekolah
	 * (di sana penyaring SQL memakai {@code eq("aktif", true)} saja sehingga baris ber-{@code NULL}
	 * hilang dari combo). Jangan menyeragamkan salah satu sisi tanpa mem-<i>backfill</i> kolomnya
	 * lebih dulu.</p>
	 *
	 * @return {@code true} bila aktif atau belum pernah disetel; {@code false} hanya bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif. Tanpa validasi; {@code null} diterima dan akan terbaca sebagai
	 * {@code true} lewat {@link #getAktif()}.
	 *
	 * <p><b>Efek samping:</b> satu-satunya pemanggil dalam repo adalah listener {@code onCheck}
	 * pada centang "Aktif" di grid daftar, yang langsung diikuti
	 * {@code Common.refreshSaveOrUpdate(subMatapelajaran)} — jadi mengklik centang <b>langsung
	 * menyimpan ke basis data</b> tanpa dialog konfirmasi, memicu {@link #onUpdate()}, dan
	 * menciptakan satu revisi Envers. Centang itu dinonaktifkan bila pengguna tidak punya hak
	 * UPDATE (yang, sesuai catatan hak akses di dokumentasi kelas, sebenarnya hak pada menu Mata
	 * Pelajaran).</p>
	 *
	 * <p>Menonaktifkan sebuah sub mata pelajaran hanya menyembunyikannya dari combo layar jadwal;
	 * baris {@code GuruMengajar}/{@code JadwalPelajaran} yang sudah terlanjur menunjuknya
	 * <b>tidak</b> dibersihkan dan tetap menyimpan referensinya.</p>
	 *
	 * @param aktif status aktif baru; {@code null} diperlakukan sama dengan {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan mata pelajaran induk yang dipecah oleh baris ini.
	 *
	 * <p><b>Getter dengan penulisan balik.</b> Hasil {@code check(...)} milik
	 * {@link GeneralValueObject} ditugaskan kembali ke field sebelum dikembalikan. Ini bukan
	 * sekadar gaya penulisan: {@code check(...)} me-resolusi proxy lazy (relasi ini
	 * {@code FetchType.LAZY}) lewat rantai cache &rarr; session yang tersedia &rarr; reload lewat
	 * session baru, dan objek yang dikembalikan <b>bisa berbeda instance</b> dari proxy semula.
	 * Efeknya: sekadar <i>membaca</i> getter ini mengubah state objek. Karena yang berubah hanya
	 * referensi ke entity yang sama (bukan nilai skalar), Hibernate tidak menganggapnya perubahan
	 * yang perlu di-flush — jadi tidak ada revisi Envers palsu dari sini.</p>
	 *
	 * <p>Kontrak {@code check(...)}: tidak pernah melempar exception dan tidak pernah
	 * mengembalikan {@code null} untuk argumen non-null; bila seluruh tahap resolusi gagal, proxy
	 * dikembalikan apa adanya. Jadi kegagalan bersifat senyap.</p>
	 *
	 * <p>Kolomnya {@code nullable = false}, sehingga secara praktis nilai ini selalu ada untuk
	 * baris yang berhasil tersimpan.</p>
	 *
	 * @return mata pelajaran induk; secara praktis tidak pernah {@code null} untuk baris tersimpan
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "matapelajaran", nullable = false)
	public Matapelajaran getMatapelajaran() {
		matapelajaran = check(matapelajaran);
		return matapelajaran;
	}

	/**
	 * Menyetel mata pelajaran induk. Tanpa validasi apa pun di tingkat entity.
	 *
	 * <p>Berbeda dengan {@link #setSekolah(Sekolah)} dan {@link #setYayasan(Yayasan)}, setter ini
	 * <b>tidak</b> menormalkan objek transien ber-id {@code null} menjadi {@code null}. Dipadukan
	 * dengan {@code cascade = PERSIST/MERGE}, menyetel objek {@code Matapelajaran} yang belum
	 * tersimpan akan ikut menyimpannya. Menyetel {@code null} baru gagal saat flush karena
	 * kolomnya {@code nullable = false} — tidak ada peringatan lebih awal.</p>
	 *
	 * <p><b>Tidak ada penegakan konsistensi tenant.</b> Tidak ada yang memaksa
	 * {@code matapelajaran.getSekolah()} sama dengan {@link #getSekolah()} milik baris ini; layar
	 * master hanya membatasinya lewat isi combo (yang disaring
	 * {@code isNull("sekolah") OR eq("sekolah", <sekolah terpilih>)}), tanpa pemeriksaan ulang di
	 * sisi server saat menyimpan.</p>
	 *
	 * @param matapelajaran mata pelajaran induk yang baru
	 */
	public void setMatapelajaran(Matapelajaran matapelajaran) {
		this.matapelajaran = matapelajaran;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini (penanda tenant).
	 *
	 * <p><b>Getter dengan penulisan balik</b>, mekanisme dan konsekuensinya identik dengan
	 * {@link #getMatapelajaran()}: hasil {@code check(...)} ditugaskan kembali ke field sebelum
	 * dikembalikan.</p>
	 *
	 * <p>Nilainya boleh {@code null} di tingkat skema, tetapi layar master mewajibkannya
	 * (<i>"Sekolah harus diisi"</i>). Baris ber-{@code sekolah} {@code null} hanya bisa lahir dari
	 * jalur non-UI (mis. impor data) — dan karena penyaring daftar memakai kesetaraan biasa,
	 * baris seperti itu tidak akan pernah muncul saat filter sekolah terisi.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila baris tidak terikat sekolah
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan <b>normalisasi non-trivial</b>: objek {@code Sekolah} yang
	 * {@code null} <i>atau</i> yang ber-{@code getId() == null} (mis. baris "=Semua=" pada combo,
	 * atau objek transien) disimpan sebagai {@code null}.
	 *
	 * <p>Tujuannya mencegah {@code cascade = PERSIST/MERGE} ikut menyimpan objek sekolah kosong
	 * sebagai baris baru di tabel sekolah. Konsekuensi yang perlu diketahui pemanggil: menyetel
	 * objek sekolah yang <b>belum tersimpan</b> tidak akan memunculkan error — nilainya hanya
	 * hilang diam-diam.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini — dengan <b>penurunan ulang setiap kali dibaca</b>.
	 *
	 * <p>Bila {@link #sekolah} terisi, field {@code yayasan} <b>ditimpa</b> oleh
	 * {@code sekolah.getYayasan()} sebelum dikembalikan; barulah hasilnya dilewatkan
	 * {@code check(...)} dan ditulis balik. Artinya:</p>
	 * <ul>
	 * <li>Nilai apa pun yang disetel {@link #setYayasan(Yayasan)} <b>tidak bertahan</b> selama
	 * {@code sekolah} terisi — sekolah selalu menang. Ini disengaja: yayasan adalah turunan dari
	 * sekolah, bukan dimensi bebas.</li>
	 * <li>Karena pembacaan getter ini mengubah field yang dipetakan Hibernate, sebuah entity yang
	 * masih <i>attached</i> dan kebetulan punya kolom {@code yayasan_id} yang tidak sinkron dengan
	 * yayasan sekolahnya akan <b>ikut ter-UPDATE saat flush berikutnya</b> — sekaligus memicu
	 * {@link #onUpdate()} dan satu revisi Envers baru. Efek ini hanya muncul sekali (setelah itu
	 * kolomnya sudah konsisten), tetapi cukup untuk menjelaskan revisi audit yang seolah tidak
	 * berasal dari tindakan pengguna mana pun.</li>
	 * <li>Bila {@code sekolah} {@code null}, nilai yang disetel manual tetap dipertahankan.</li>
	 * </ul>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)}: {@code null} atau objek ber-{@code getId() == null} disimpan
	 * sebagai {@code null}.
	 *
	 * <p><b>Nilai yang disetel di sini bersifat sementara</b> bila {@link #sekolah} terisi —
	 * {@link #getYayasan()} akan menurunkannya ulang dari sekolah pada pembacaan berikutnya.
	 * Untuk mengubah yayasan secara efektif, ubah sekolahnya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}
}
