package ais.database.model.sekolah;

// Generated Dec 22, 2009 12:14:16 PM by Hibernate Tools 3.2.4.CR1

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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;



import ais.database.model.GeneralValueObject;
import ais.database.model.library.Item;

/**
 * Baris penghubung <b>daftar referensi perpustakaan</b> milik satu jadwal pelajaran: memasangkan
 * sebuah {@link JadwalPelajaran} (satu mata pelajaran pada satu kelas/kelas les, satu guru, satu
 * tahun ajaran + semester) dengan sebuah {@link Item} — <b>entry katalog perpustakaan</b> di
 * {@code library.item} (buku/majalah/terbitan ber-ISBN/ISSN, lengkap dengan pengarang, penerbit,
 * sampul, dan tautan Google Books). Satu baris = satu buku rujukan untuk satu jadwal, ditambah
 * catatan bebas ({@link #getKeterangan()}) yang diisi guru/petugas.
 *
 * <h2>Ini BUKAN model jadwal alternatif</h2>
 * <p>Nama "PunyaItem" mudah disalahartikan sebagai "baris slot jadwal", seolah-olah ada model
 * jadwal baru yang memecah 12 kolom slot {@code jamPelajaran1..12} milik {@link JadwalPelajaran}
 * menjadi baris-baris terpisah. <b>Bukan.</b> Sudah diverifikasi dari kode: kata "Item" di sini
 * merujuk {@link ais.database.model.library.Item} (paket <i>library</i>, tabel
 * {@code library.item}), bukan slot jam pelajaran. {@link JadwalPelajaran} tetap satu-satunya
 * model jadwal dan tetap memakai 12 kolom tetap {@code jamPelajaranN}/{@code guruN}/{@code hariN};
 * kelas ini hidup berdampingan dengan model itu sebagai relasi many-to-many terwujud
 * (<i>association table</i>) ke katalog pustaka, bukan sebagai penggantinya. Padanan konseptualnya
 * adalah "daftar pustaka/daftar rujukan" pada silabus atau RPP.
 *
 * <h2>Pemetaan</h2>
 * <ul>
 *   <li>Tabel {@code sekolah.jadwal_pelajaran_punya_item}, {@code dynamicInsert}/{@code dynamicUpdate}
 *   aktif (hanya kolom yang berubah yang ikut ke SQL).</li>
 *   <li>{@code @Audited} (Hibernate Envers) — setiap penambahan/perubahan/penghapusan referensi
 *   ikut tercatat di tabel revisi, dan itulah yang dibaca tombol "Revisi" pada grid.</li>
 *   <li>Kolom: {@code id} (IDENTITY), {@code jadwal_pelajaran} (FK wajib), {@code item} (FK wajib),
 *   {@code keterangan} ({@code text}), {@code oleh}, {@code oleh_id}, {@code tanggal_dirubah}.</li>
 *   <li><b>Tidak ada kolom {@code sekolah}/{@code yayasan}.</b> Cakupan tenant sepenuhnya
 *   diturunkan dari {@link #getJadwalPelajaran()}; seluruh pembacaan produktif memfilter
 *   {@code Restrictions.eq("jadwalPelajaran", ...)} sehingga isolasi terjaga secara tidak langsung.</li>
 *   <li><b>Tidak ada unique constraint</b> pada pasangan ({@code jadwal_pelajaran}, {@code item}) —
 *   lihat catatan duplikasi di bawah.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ol>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan kait
 *   {@link #onUpdate()}.</li>
 *   <li><b>Relasi</b> — {@link #getJadwalPelajaran()} (sisi jadwal) dan {@link #getItem()} (sisi
 *   katalog pustaka), keduanya {@code @ManyToOne} wajib.</li>
 *   <li><b>Muatan sendiri</b> — hanya {@link #getKeterangan()}: satu-satunya data yang benar-benar
 *   milik baris penghubung ini.</li>
 * </ol>
 * <p>Tidak ada method dengan logika bisnis di kelas ini; seluruh perilaku (pengambilan referensi,
 * penyalinan antar tahun ajaran, pencetakan daftar rujukan) berada di pemanggil.</p>
 *
 * <h2>Siapa yang memakai (terverifikasi)</h2>
 * <ul>
 *   <li>{@code ais.action.master.sekolah.helper.JadwalPelajaranPunyaItemHelper} — layar utamanya:
 *   grid "Daftar Referensi Perpustakaan" (label tab "Buku Referensi (n)") dengan tombol
 *   <i>Ambil Referensi</i> (pilih dari katalog perpustakaan lokal) dan <i>Ambil Google Book</i>
 *   (cari di Google Books, hasilnya dijadikan {@link Item} baru), serta aksi per baris:
 *   kutipan/sitasi, pratinjau Google Books, baca hasil pindai per halaman, dan hapus.</li>
 *   <li>{@code ais.action.master.sekolah.helper.AktifitasPembelajaranHelper} — menyisipkan helper
 *   di atas sebagai tab "Buku" pada layar Aktivitas Pembelajaran (e-learning). Layar ini juga
 *   dibuka dari kalender mingguan siswa
 *   ({@code CalendarJadwalPelajaranMingguIniComposer}/{@code CalendarPerkuliahanMingguIniComposer}),
 *   jadi baris-baris ini ikut terlihat oleh siswa.</li>
 *   <li>{@code JadwalPelajaranAction} dan {@code PertemuanJadwalPelajaranAction} — bagian
 *   "Daftar Rujukan" pada cetak silabus/RPP; keduanya memakai
 *   {@code Projections.groupProperty("item")} sehingga item yang kebetulan terdaftar dua kali
 *   tetap tercetak sekali.</li>
 *   <li>{@code PenjadwalanSiswaHelper} — saat menyalin jadwal/agenda ke jadwal baru, daftar
 *   referensi ikut disalin; jalur ini <b>memeriksa duplikat</b> lebih dulu
 *   ({@code item} + {@code jadwalPelajaran} yang sama dilewati).</li>
 *   <li>{@code DashboardRekapPertemuanJadwalPelajaran} — native SQL menghitung jumlah referensi
 *   per jadwal ({@code select count(*) ... group by jadwal_pelajaran}) untuk kolom rekap dasbor
 *   e-learning.</li>
 * </ul>
 *
 * <h2>Hal non-obvious &amp; jebakan</h2>
 * <ul>
 *   <li><b>Menyimpan baris ini bisa MENULIS ke master katalog perpustakaan.</b> Kedua relasi
 *   memakai {@code cascade = {PERSIST, MERGE}}. Pada jalur "Ambil Google Book",
 *   {@code CheckISBN.simpanVolume(...)} menghasilkan {@link Item} yang <i>tidak</i> disimpan
 *   secara eksplisit — baris {@code library.item} baru lahir semata-mata lewat cascade dari
 *   penyimpanan objek ini. Jadi "menambah buku referensi pada satu jadwal pelajaran" adalah
 *   operasi tulis terhadap katalog perpustakaan global, bukan sekadar penambahan relasi.</li>
 *   <li><b>Tidak ada {@code @PrePersist}.</b> Hanya {@link #onUpdate()} ({@code @PreUpdate}) yang
 *   ada, sehingga {@code oleh}/{@code oleh_id} masih {@code null} pada INSERT dan baru terisi saat
 *   baris pertama kali di-UPDATE. Pada praktiknya baris referensi jarang di-update (hanya bila
 *   catatannya disunting), jadi kolom atribusi banyak yang permanen kosong; Envers tetap merekam
 *   revisi INSERT-nya.</li>
 *   <li><b>Tanpa unique constraint, duplikat mungkin.</b> Jalur "Ambil Google Book" pada
 *   {@code JadwalPelajaranPunyaItemHelper} tidak memeriksa apakah item sudah terdaftar, sehingga
 *   buku yang sama bisa muncul berkali-kali di grid (cetak silabus tetap aman karena memakai
 *   {@code groupProperty}). Bandingkan dengan {@code PenjadwalanSiswaHelper} yang memeriksa.</li>
 *   <li><b>{@link #getKeterangan()} di sini MENIMPA versi {@link GeneralValueObject}</b> dan
 *   menghilangkan normalisasi {@code null}&rarr;{@code ""} milik induk. Efek lanjutannya ada di
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)}: karena kelas ini tidak memetakan
 *   {@code nomorUrut}/{@code nim}/{@code nama} sama sekali, cabang {@code keterangan} adalah
 *   satu-satunya yang bisa terpakai — dan cabang itu mati begitu {@code keterangan} bernilai
 *   {@code null}. Akibatnya {@code compareTo} praktis selalu mengembalikan {@code 0}, sehingga
 *   {@code TreeSet}/{@code TreeMap} berisi entity ini akan menciut jadi satu elemen. Seluruh kode
 *   yang ada memakai {@code List} + {@code Order.asc("id")}, jadi bug ini <b>laten</b>, bukan
 *   aktif — tetapi jangan pernah menaruh entity ini di koleksi terurut.</li>
 *   <li><b>{@link #toString()} membaca field langsung, bukan getter.</b> Karena tidak melewati
 *   {@link GeneralValueObject#check(Object)}, {@code toString()} atas object yang sudah lepas dari
 *   {@code Session} dapat melempar {@code LazyInitializationException} lewat proxy {@code item}.
 *   Jangan pakai {@code toString()} kelas ini di jalur logging.</li>
 *   <li><b>{@link GeneralValueObject} BUKAN {@code @Entity}/{@code @MappedSuperclass}</b> — Hibernate
 *   tidak memetakan properti induk. Karena itu {@code id}, {@code oleh}, {@code olehId},
 *   {@code tanggal_dirubah}, dan {@code keterangan} <b>sengaja dideklarasikan ulang</b> di kelas
 *   ini. Itu keharusan teknis, bukan duplikasi yang perlu "dibersihkan"; menghapusnya membuat
 *   kolom-kolom tersebut hilang dari skema. Lihat {@link ais.database.model.GeneralValueObject}.</li>
 * </ul>
 *
 * <h2>Catatan keamanan pada layar pemakainya</h2>
 * <p>Bukan cacat pada entity ini, tetapi relevan bagi siapa pun yang menyentuhnya:
 * pada {@code JadwalPelajaranPunyaItemHelper.DetailJadwalPelajaranRenderer} tombol <i>Hapus</i>
 * hanya disembunyikan ketika {@code Common.getCurrentUser().getMahasiswa() != null}, padahal
 * toolbar penambahan dan kotak catatan disembunyikan untuk {@code getSiswa() != null}. Akun
 * <b>siswa</b> lolos dari syarat "mahasiswa" tersebut, sehingga siswa yang membuka tab "Buku"
 * dari kalender mingguannya dapat menghapus baris daftar referensi milik gurunya; jalur hapus
 * ({@code Common.refreshDelete}) tidak melakukan pemeriksaan hak apa pun. Selain itu tombol
 * "Revisi" pada grid dipanggil dengan {@code SaldoAwalDetail.class} — bukan kelas ini — sehingga
 * riwayat yang ditampilkan adalah riwayat baris {@code SaldoAwalDetail} yang kebetulan ber-id sama.
 *
 * @see JadwalPelajaran
 * @see ais.database.model.library.Item
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jadwal_pelajaran_punya_item")
public class JadwalPelajaranPunyaItem extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan sekali saat kelas dibuat dan
	 * <b>tidak boleh diubah</b>: instance entity AIS diserialkan ke cache in-memory/MapDB dan
	 * dibawa lintas request ZK, sehingga mengubah nilai ini membuat data cache lama gagal dibaca.
	 */
	private static final long serialVersionUID = 1950126270979098967L;
	/** Primary key baris penghubung; diisi database (IDENTITY). Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang meng-UPDATE baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna terakhir yang meng-UPDATE baris ini; diisi otomatis lewat {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah di-UPDATE
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/hanya spasi: pada kasus itu method langsung kembali tanpa mengubah apa pun, sehingga
	 * nilai lama dipertahankan dan atribusi audit tidak pernah "terhapus" oleh konteks anonim
	 * (mis. job latar atau permintaan tanpa pengguna login).
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan diam-diam</b> agar atribusi lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila baris belum pernah di-UPDATE
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} — dipanggil Hibernate/JPA <b>otomatis sebelum setiap UPDATE</b>
	 * terhadap baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dari konteks pengguna aktif. Tidak pernah
	 * dipanggil manual dari kode aplikasi. Ini juga implementasi wajib dari satu-satunya method
	 * {@code abstract} milik {@link GeneralValueObject}.
	 *
	 * <p><b>Tidak berjalan pada INSERT</b> dan kelas ini tidak punya {@code @PrePersist}, sehingga
	 * baris referensi yang dibuat lewat tombol "Ambil Referensi"/"Ambil Google Book" atau lewat
	 * penyalinan jadwal masuk ke database tanpa {@code oleh}/{@code oleh_id}.</p>
	 *
	 * <p><b>Perhatian pemeliharaan:</b> baris fisik yang sama juga mendeklarasikan field
	 * {@code tanggal_dirubah} (gaya asli berkas, dipertahankan). Nilai awalnya diambil dari
	 * {@code ais.ui.util.WaktuUtil.getDate()} sehingga object baru sudah bertanggal sejak dibuat,
	 * bukan {@code null}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir. Umumnya tidak dipanggil aplikasi secara langsung —
	 * pengisiannya dilakukan {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code timestamp}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya sudah diinisialisasi dengan waktu saat ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks pasangan jadwal&ndash;item, berformat {@code "<jadwalPelajaran>_<item>"}.
	 * Meng-override {@link GeneralValueObject#toString()} (yang berformat {@code "kode - nama"} dan
	 * tidak berarti apa-apa untuk baris penghubung tanpa kode/nama).
	 *
	 * <p><b>Hati-hati:</b> kedua nilai dibaca dari <b>field</b> secara langsung, bukan lewat
	 * {@link #getJadwalPelajaran()}/{@link #getItem()}, sehingga proxy lazy tidak diresolusi
	 * {@link GeneralValueObject#check(Object)} lebih dulu. Pada object yang sudah lepas dari
	 * {@code Session}, memanggil method ini dapat melempar {@code LazyInitializationException}.</p>
	 *
	 * @return gabungan {@code toString()} jadwal pelajaran dan item, dipisah garis bawah
	 */
	public String toString() {
		return jadwalPelajaran + "_" + item;
	}

	/** Jadwal pelajaran pemilik daftar referensi ini (FK wajib). Lihat {@link #getJadwalPelajaran()}. */
	private JadwalPelajaran jadwalPelajaran;
	/** Entry katalog perpustakaan yang dirujuk (FK wajib, dimuat lazy). Lihat {@link #getItem()}. */
	private Item item;

	/** Catatan bebas guru/petugas atas referensi ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Mengembalikan catatan bebas atas referensi ini — satu-satunya muatan data milik baris
	 * penghubung ini sendiri. Ditampilkan pada kolom "Catatan" grid referensi, dan dapat disunting
	 * langsung di grid (event {@code onChange} &rarr; {@code Common.refreshUpdate}) oleh pengguna
	 * yang bukan siswa maupun mahasiswa; siswa/mahasiswa hanya melihatnya sebagai label.
	 *
	 * <p><b>Meng-override {@link GeneralValueObject#getKeterangan()}</b> dan sengaja
	 * mengembalikan nilai apa adanya, <b>tanpa</b> normalisasi {@code null}&rarr;{@code ""} milik
	 * induk — lihat catatan {@code compareTo} pada Javadoc kelas. Dipetakan ke kolom
	 * {@code text} (panjang bebas), berbeda dari kebanyakan entity yang memakai {@code varchar}.</p>
	 *
	 * @return catatan referensi, atau {@code null} bila belum pernah diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas atas referensi ini. Tanpa validasi maupun pemangkasan; pemanggil di
	 * grid ({@code JadwalPelajaranPunyaItemHelper}) sudah mem-{@code trim()} sendiri, dan jalur
	 * penambahan massal mengisinya dengan string kosong.
	 *
	 * @param keterangan catatan baru; {@code null} diterima apa adanya
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate memakainya saat menghidrasi
	 * baris dari database, dan juga dipakai langsung oleh jalur penambahan referensi
	 * ({@code JadwalPelajaranPunyaItemHelper}, {@code PenjadwalanSiswaHelper}) sebelum
	 * {@link #setItem(Item)}/{@link #setJadwalPelajaran(JadwalPelajaran)} dipanggil.
	 */
	public JadwalPelajaranPunyaItem() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} dengan {@code insertable = false}: nilainya sepenuhnya
	 * ditentukan sequence/serial database dan tidak pernah dikirim pada INSERT. Kolom ini juga
	 * dasar {@link GeneralValueObject#equals(Object)} — perhatikan bahwa {@code hashCode()} tidak
	 * di-override di hierarki ini, jadi jangan mendeduplikasi entity ini lewat {@code HashSet}.</p>
	 *
	 * @return id baris, atau {@code null} bila object belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Praktis hanya dipakai Hibernate saat hidrasi, atau untuk membuat
	 * object "penunjuk" berisi id saja sebagai parameter kriteria.
	 *
	 * @param id nilai primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan jadwal pelajaran pemilik referensi ini.
	 *
	 * <p>Relasi {@code @ManyToOne} <b>wajib</b> ({@code nullable = false}) ke kolom
	 * {@code jadwal_pelajaran}, dengan {@code FetchMode.SELECT} (dimuat lewat query terpisah,
	 * bukan {@code join} pada query induk) dan cascade {@code PERSIST}/{@code MERGE}. Inilah
	 * satu-satunya pembawa konteks tenant baris ini: sekolah/yayasan, tahun ajaran, semester,
	 * kelas, mata pelajaran, dan guru semuanya dibaca dari objek jadwal ini — antara lain oleh
	 * renderer grid yang menampilkan blok guru + hari/jam/ruangan di kolom "Catatan".</p>
	 *
	 * <p>Berbeda dari {@link #getItem()}, getter ini <b>tidak</b> memanggil
	 * {@link GeneralValueObject#check(Object)}; relasi ini memakai fetch EAGER bawaan
	 * {@code @ManyToOne} sehingga umumnya sudah terisi penuh saat baris dimuat.</p>
	 *
	 * @return jadwal pelajaran pemilik; secara skema tidak pernah {@code null} untuk baris
	 *         tersimpan, meski pemanggil di renderer tetap memeriksanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jadwal_pelajaran", nullable = false)
	public JadwalPelajaran getJadwalPelajaran() {
		return this.jadwalPelajaran;
	}

	/**
	 * Menyetel jadwal pelajaran pemilik referensi ini. Dipanggil oleh seluruh jalur penambahan
	 * (grid referensi, impor Google Books, penyalinan jadwal antar tahun ajaran) sebelum baris
	 * disimpan.
	 *
	 * <p><b>Efek samping cascade:</b> karena relasi ini ber-cascade {@code PERSIST}/{@code MERGE},
	 * menyimpan baris penghubung juga akan mem-persist/merge objek jadwal yang dipasang di sini.</p>
	 *
	 * @param jadwalPelajaran jadwal pelajaran pemilik; secara skema tidak boleh {@code null}
	 *                        (INSERT dengan {@code null} akan ditolak constraint kolom)
	 */
	public void setJadwalPelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * Mengembalikan entry katalog perpustakaan yang dirujuk, dengan <b>resolusi proxy lazy</b>
	 * lebih dulu.
	 *
	 * <p>Relasi {@code @ManyToOne} <b>wajib</b> ({@code nullable = false}) ke kolom {@code item}
	 * tabel {@code library.item}, dipetakan {@code FetchType.LAZY}. Karena itu getter memakai pola
	 * standar AIS {@code item = check(item)}: {@link GeneralValueObject#check(Object)} menukar
	 * proxy Hibernate dengan instance kanonik (identity map / cache / database) sehingga getter
	 * tetap aman dipanggil atas object yang sudah <i>detached</i> — situasi normal di AIS karena
	 * entity hidup lebih lama daripada {@code Session} yang memuatnya.</p>
	 *
	 * <p><b>Penulisan balik ke field bukan efek samping destruktif:</b> nilai yang ditulis kembali
	 * adalah objek yang setara-identitas dengan proxy semula (kelas + id sama), hanya sudah
	 * terinisialisasi. Ini pola yang dipakai ribuan getter relasi di {@code ais.database.model}
	 * dan bukan varian "getter write-back destruktif" yang menimpa data baris lain.</p>
	 *
	 * <p>Nilai kembalinya dibaca luas: sampul, ISBN/ISSN, judul, pengarang, penerbit, tombol
	 * kutipan/sitasi, tautan pratinjau Google Books, jumlah halaman hasil pindai, serta daftar
	 * rujukan pada cetak silabus/RPP.</p>
	 *
	 * @return entry katalog perpustakaan yang dirujuk; secara skema tidak pernah {@code null}
	 *         untuk baris tersimpan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item", nullable = false)
	public Item getItem() {
		item = check(item);
		return item;
	}

	/**
	 * Menyetel entry katalog perpustakaan yang dirujuk.
	 *
	 * <p><b>Efek samping paling penting di kelas ini:</b> relasi ber-cascade
	 * {@code PERSIST}/{@code MERGE}, sehingga menyimpan baris penghubung ikut menyimpan
	 * {@link Item} yang dipasang di sini. Jalur "Ambil Google Book" mengandalkan persis perilaku
	 * itu — {@code Item} hasil {@code CheckISBN.simpanVolume(...)} tidak disimpan eksplisit,
	 * melainkan lahir di {@code library.item} lewat cascade dari objek ini. Konsekuensinya
	 * menambahkan referensi pada satu jadwal pelajaran adalah operasi tulis terhadap katalog
	 * perpustakaan yang dipakai bersama seluruh instalasi.</p>
	 *
	 * @param item entry katalog perpustakaan; secara skema tidak boleh {@code null}
	 */
	public void setItem(Item item) {
		this.item = item;
	}

}
