package ais.database.model;

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

/**
 * Entity satu <b>profil penulis (author) Google Scholar</b> hasil pengambilan otomatis
 * (<i>crawling</i>) dari {@code scholar.google.com}. Tabel: {@code public.scholar_author}.
 *
 * <p>Satu baris mewakili satu identitas penulis sebagaimana dikenali Google Scholar &mdash;
 * bukan seorang dosen AIS. Isinya minimalis: nama tampil, tautan profil, tautan foto, dan kode
 * user Scholar. Baris pada tabel ini <b>tidak pernah diketik pengguna</b>; seluruhnya lahir dari
 * dua crawler berbasis Jsoup di paket {@code ais.common.scholar}.</p>
 *
 * <h2>Dari mana barisnya berasal</h2>
 * <ol>
 * <li><b>Jalur pencarian kata kunci</b> &mdash;
 * {@code ais.common.scholar.GoogleScholarCrawler#startCrawl} mengunduh halaman hasil pencarian
 * {@code /scholar?q=&lt;katakunci&gt;} lalu, untuk setiap artikel, mengambil elemen
 * {@code .gs_a&gt;a} (penulis yang punya tautan profil). Untuk tiap penulis: tautan profil
 * dirakit menjadi {@code "https://scholar.google.com" + href}, kode user diekstrak dengan
 * {@link #ambilId(String)}, baris lama dicari dengan {@code Restrictions.eq("userid", userid)}
 * (urut {@code id} menurun, {@code setMaxResults(1)}), dan bila belum ada dibuat baris baru
 * lalu {@code saveOrUpdate}. Bila artikel <b>tidak punya</b> penulis bertautan, dibuat satu baris
 * penampung yang dicocokkan hanya berdasarkan {@code nama}, dengan {@code keterangan} diisi
 * teks sentinel {@code "empty"} dan {@code userid} dibiarkan {@code null}.</li>
 * <li><b>Jalur sinkronisasi per dosen</b> &mdash;
 * {@code ais.common.scholar.GoogleScholarCrawlerByUser#byUser(String)} mengunduh halaman profil
 * {@code /citations?user=&lt;kode&gt;}, mencari baris dengan {@code Restrictions.eq("userid",
 * user).uniqueResult()}, lalu mengisi {@code nama} dari {@code div#gsc_prf_in},
 * {@code imageLink} dari {@code img#gsc_prf_pup-img}, dan {@code keterangan} dari URL profil
 * yang dirakit sendiri ({@code BaseURL + "/citations?hl=id&amp;user=" + user}).</li>
 * <li><b>Pelengkapan foto</b> &mdash; setelah jalur (1) selesai, tiap penulis unik dilewatkan ke
 * {@code GoogleScholarCrawlerByUser#updateDataAuthor(ScholarAuthor)} yang mengunduh halaman
 * profil hanya untuk mengambil nama lengkap dan foto, lalu {@code session.refresh} +
 * {@code session.update}.</li>
 * </ol>
 *
 * <h2>Kaitan ke {@link Dosen} &mdash; tidak ada FK</h2>
 * <p>Kelas ini <b>tidak memiliki relasi apa pun</b> ke {@link Dosen} maupun {@code Pegawai}.
 * Penjodohan terjadi murni saat <i>runtime</i> dengan pencocokan string: {@link Dosen#getGoogleScholar()}
 * menyimpan <b>kode user Scholar</b> (mis. {@code Z8ZcJboAAAAJ}, bukan URL) yang diketik manual
 * dosen pada layar biodata, dan kode itu dicocokkan ke kolom {@code user_id} kelas ini. Tidak ada
 * integritas referensial: kode yang salah ketik menghasilkan pencarian nihil tanpa error, dan
 * baris {@code scholar_author} bisa hidup tanpa dosen manapun yang mengklaimnya (lazimnya memang
 * begitu &mdash; jalur pencarian kata kunci memanen SEMUA rekan penulis, termasuk peneliti luar).</p>
 *
 * <h2>Kaitan ke artikel</h2>
 * <p>Relasi ke {@link ScholarArticle} bersifat <b>unidirectional {@code @ManyToMany}</b> lewat
 * tabel jembatan {@code article_has_scholar_author} yang dideklarasikan di sisi
 * {@code ScholarArticle} ({@code ScholarArticle#getScholarAuthors()}, {@code cascade = MERGE}).
 * Karena tidak ada sisi {@code mappedBy} di sini, <b>dari objek penulis tidak bisa dinavigasi ke
 * daftar artikelnya</b> &mdash; arah baca satu-satunya adalah artikel &rarr; penulis.</p>
 *
 * <p>{@link SintaArticle} + {@code SintaCrawler} adalah <b>padanan konseptual</b> rangkaian ini
 * untuk sumber SINTA (indeks publikasi nasional Kemdikbudristek), dan
 * {@link ais.database.model.penelitiandanpengabdian.Artikel} memiliki DUA FK sejajar
 * ({@code sinta_article} dan {@code scholar_article}) sehingga satu catatan publikasi "resmi" AIS
 * bisa berasal dari salah satu sumber. Perbedaan struktur yang mencolok: di sisi SINTA, penulis
 * hanyalah satu kolom teks bebas ({@code SintaArticle#getAuthor()}) dan pemilik publikasi adalah
 * FK {@code dosen} yang sungguhan; di sisi Google Scholar justru sebaliknya &mdash; penulis
 * dipromosikan menjadi entity tersendiri (kelas ini) tetapi kaitan ke dosen hilang sama sekali.
 * {@code ScholarArticle} sendiri <b>belum digarap</b> dalam inisiatif Javadoc ini; rujuk kelas itu
 * untuk struktur artikelnya.</p>
 *
 * <h2>Hal-hal non-obvious</h2>
 * <ul>
 * <li><b>{@link #getUserid()} MENULIS BALIK ke field terpetakan.</b> Bila {@code userid} masih
 * {@code null}, getter ini menurunkannya dari {@link #getKeterangan()} lewat
 * {@link #ambilId(String)} dan <b>menyimpan hasilnya ke field</b>. Karena seluruh anotasi
 * pemetaan kelas ini ada di getter (<i>property access</i>), Hibernate membaca state entity lewat
 * getter juga saat <i>dirty checking</i> pada {@code flush} &mdash; sehingga sekadar MEMBACA
 * entity persistent dapat menghasilkan {@code UPDATE public.scholar_author SET user_id = ...}
 * beserta baris revisi Envers dan pemicuan {@link #onUpdate()}. Ini bukan mutasi in-memory belaka.
 * Rincian dan risikonya dibahas di {@link #getUserid()}.</li>
 * <li><b>Kontrak {@code keterangan} dibalik dari base class.</b> {@link GeneralValueObject#getKeterangan()}
 * menjamin hasil non-{@code null} (mengubah {@code null} menjadi {@code ""}); override di sini
 * mengembalikan nilai apa adanya termasuk {@code null}. Pemanggil memang bergantung pada perilaku
 * override itu &mdash; {@code DetailArtikelHelper} menguji {@code getKeterangan() == null ||
 * ... equalsIgnoreCase("empty")} untuk memutuskan apakah nama penulis ditampilkan sebagai label
 * mati atau sebagai tombol yang membuka profil Scholar. Perilaku serupa sudah tercatat pada
 * {@link Bank} dan {@link SintaArticle}.</li>
 * <li><b>{@code keterangan} bukan catatan bebas, melainkan URL (atau sentinel).</b> Isinya salah
 * satu dari: URL profil Google Scholar, teks literal {@code "empty"}, atau {@code null}. Nilai
 * itu dipakai langsung sebagai target navigasi di UI, jadi jangan diperlakukan sebagai teks biasa
 * (lihat catatan keamanan di {@link #getKeterangan()}).</li>
 * <li><b>Tidak ada layar master/CRUD.</b> Tidak ada {@code ScholarAuthorAction}, tidak ada
 * {@code .zul}, tidak ada entri menu. Satu-satunya layar yang menyentuh entity ini adalah dialog
 * modal {@link ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak} (read-only
 * terhadap penulis) dan panel detail
 * {@link ais.action.master.helper.DetailArtikelHelper}. Konsekuensinya tidak ada pemeriksaan
 * {@code checkPrevilages}/{@code MUST_CHECKED} sama sekali pada jalur Scholar; satu-satunya
 * penjaga adalah visibilitas tombol pemicunya.</li>
 * <li><b>Sebagian besar pemicunya mati.</b> Tombol Scholar di {@code DataPunyaArtikelHelper} dan
 * {@code FilePerkuliahanHelper} di-{@code setVisible(false)}, dan cabang sinkronisasi per dosen
 * di {@code DetailArtikelHelper} dijaga {@code if (false &amp;&amp; ...)} sehingga
 * {@link Dosen#getGoogleScholar()} yang diketik dosen praktis tidak pernah dipakai. Yang masih
 * hidup hanyalah jalur pencarian kata kunci lewat {@code DataPunyaArtikelHelper}. Baris
 * {@code scholar_author} yang terlanjur ada tetap dibaca dan dirender.</li>
 * <li><b>{@code user_id} bertanda {@code unique = true}.</b> Digabung dengan write-back di
 * {@link #getUserid()}, sebuah nilai turunan yang bentrok dengan baris lain akan memicu
 * pelanggaran <i>unique constraint</i> pada {@code flush} &mdash; yaitu di tempat yang sama sekali
 * tak terduga, misalnya saat merender panel detail artikel. Pada PostgreSQL beberapa baris
 * ber-{@code user_id} {@code NULL} tetap sah, sehingga baris sentinel {@code "empty"} tidak saling
 * bentrok.</li>
 * <li><b>Dua gaya pencarian yang tidak konsisten.</b> {@code GoogleScholarCrawler} memakai
 * {@code addOrder(desc id).setMaxResults(1)} (tahan duplikat), sedangkan
 * {@code GoogleScholarCrawlerByUser#byUser} memakai {@code uniqueResult()} (melempar
 * {@code NonUniqueResultException} bila duplikat sempat lolos).</li>
 * </ul>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * hanya POJO abstrak biasa, sehingga Hibernate TIDAK memetakan properti induknya. Karena itu
 * deklarasi ULANG {@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>BUKAN bug melainkan keharusan teknis</b>: tanpa
 * deklarasi ulang, kolom-kolom tersebut tidak akan terpetakan sama sekali. Konsekuensinya field
 * bernama sama di {@code GeneralValueObject} (yang {@code private}) tetap ada namun selalu kosong
 * untuk instance kelas ini &mdash; kode yang memakai jalur akses milik base class (bukan getter
 * yang di-override di sini) akan membaca {@code null}. Properti base lain yang TIDAK
 * dideklarasikan ulang di sini ({@code kode}, {@code nim}, {@code nomorUrut}) karena itu tidak
 * pernah tersimpan; {@link GeneralValueObject#toString()} pun sengaja di-override agar tidak
 * mencetak {@code kode} yang selalu kosong.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Audit &amp; identitas</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 * {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 * <li><b>Data profil Scholar</b> &mdash; {@link #getNama()}/{@link #setNama(String)} (nama tampil),
 * {@link #getKeterangan()}/{@link #setKeterangan(String)} (URL profil/sentinel),
 * {@link #getImageLink()}/{@link #setImageLink(String)} (URL foto).</li>
 * <li><b>Kode user Scholar</b> &mdash; {@link #ambilId(String)} (utilitas statis pengurai URL) dan
 * {@link #getUserid()}/{@link #setUserid(String)} (satu-satunya anggota berlogika nyata).</li>
 * </ol>
 *
 * <h2>Verifikasi pola berulang paket ini</h2>
 * <ul>
 * <li><b>Getter yang menulis balik ke field/DB:</b> ADA satu &mdash; {@link #getUserid()}.
 * Berbeda dari {@code getDosen()} pada {@link SintaArticle} yang hanya menormalkan proxy lazy di
 * memori, write-back di sini menyentuh kolom terpetakan sungguhan sehingga bisa berujung
 * {@code UPDATE}. Sebaliknya {@link #getNama()} di kelas ini <b>TIDAK</b> menulis balik (hanya
 * {@code trim} pada nilai kembalian) &mdash; berbeda dari kembarannya
 * {@code ScholarArticle#getNama()} yang membersihkan penanda {@code [PDF]}/{@code [BUKU]}/
 * {@code [B]}/{@code [DOC]} lalu MENYIMPAN hasilnya ke field.</li>
 * <li><b>Getter yang menutup sesi Hibernate:</b> TIDAK ADA di kelas ini. Seluruh
 * {@code HibernateUtil.closeSession()} terjadi di crawler pemanggil, bukan di entity.</li>
 * <li><b>Getter destruktif</b> (menghapus/mengosongkan data secara permanen): TIDAK ADA.</li>
 * <li><b>Setter yang menolak nilai kosong:</b> ADA &mdash; {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} mengabaikan {@code null}/string kosong, jadi field audit tidak pernah
 * bisa dikosongkan kembali setelah terisi. Setter lain menerima apa adanya.</li>
 * </ul>
 *
 * <p><b>Anotasi kelas.</b> {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menyertakan kolom yang benar-benar berubah pada INSERT/UPDATE &mdash; ini meredam (tapi tidak
 * menghilangkan) dampak write-back {@link #getUserid()}: yang terkirim hanya kolom
 * {@code user_id}, bukan seluruh baris. {@code @Audited} (Hibernate Envers) menyalin setiap versi
 * baris ke tabel revisi, sehingga UPDATE yang tidak diniatkan pun meninggalkan jejak permanen.
 * Entity terdaftar di {@code hibernate.cfg.xml} sebagai
 * {@code &lt;mapping class="ais.database.model.ScholarAuthor" /&gt;}.</p>
 *
 * <p><b>Catatan:</b> komentar generator "Bank generated by hbm2java" yang sebelumnya menempati
 * posisi ini adalah sisa salin-tempel dari {@link Bank} (sumber asli komentar itu) dan tidak
 * menggambarkan kelas ini.</p>
 *
 * @see ScholarArticle
 * @see SintaArticle
 * @see ais.database.model.penelitiandanpengabdian.Artikel
 * @see ais.common.scholar.GoogleScholarCrawler
 * @see ais.common.scholar.GoogleScholarCrawlerByUser
 * @see Dosen#getGoogleScholar()
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "scholar_author")

public class ScholarAuthor extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, diwarisi lewat {@link java.io.Serializable} pada
	 * {@link GeneralValueObject}. Nilainya tetap agar objek yang pernah di-serialisasi (mis.
	 * tersimpan di sesi ZK saat dialog {@code AmbilDataDariGoogleScholarBanyak} terbuka) tetap
	 * kompatibel.
	 *
	 * <p><b>Kuirk:</b> nilai konstanta ini identik dengan yang ada di {@link SintaArticle},
	 * {@link Pesan}, dan {@link DspaceInformation} &mdash; artefak salin-tempel yang sama seperti
	 * komentar generator "Bank generated by hbm2java". Tidak berdampak fungsional karena
	 * {@code serialVersionUID} hanya dibandingkan antar-versi kelas YANG SAMA.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key ({@code public.scholar_author.id}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila baris belum
	 *         pernah di-update sejak dibuat &mdash; kondisi yang lazim di sini, karena baris
	 *         {@code scholar_author} umumnya ditulis proses crawler, bukan sesi pengguna
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong/whitespace
	 * diabaikan senyap</b> (method langsung kembali tanpa mengubah apa pun), sehingga field ini
	 * tidak pernah bisa dikosongkan lagi setelah sekali terisi. Dipanggil dari
	 * {@code AuditTimestampInterceptor.ubah(this)} lewat {@link #onUpdate()}.
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong = tidak melakukan apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * <b>{@code null}/string kosong diabaikan senyap</b> sehingga nilai lama dipertahankan.
	 * Dipanggil dari {@code AuditTimestampInterceptor.ubah(this)} lewat {@link #onUpdate()}.
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong = tidak melakukan apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah
	 *         di-update
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi <i>pembuat</i> baris tidak tercatat
	 * pada kolom-kolom ini.
	 *
	 * <p><b>Penting untuk entity ini:</b> berbeda dari kebanyakan entity AIS, callback ini
	 * <b>tidak selalu menandai perubahan yang diniatkan</b>. Karena {@link #getUserid()} menulis
	 * balik ke field terpetakan, <i>dirty checking</i> Hibernate saat {@code flush} bisa
	 * memunculkan UPDATE dari operasi yang niatnya cuma membaca &mdash; dan callback ini akan ikut
	 * mencatat pengguna yang kebetulan sedang membuka layar itu sebagai "pengubah terakhir".
	 * Nama yang tercatat di {@code oleh} karena itu tidak bisa dianggap sebagai bukti seseorang
	 * benar-benar menyunting data penulis.</p>
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()})
	 * sehingga baris baru tetap punya stempel waktu meski belum pernah di-update.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Berbeda dari {@link #setOleh(String)}, setter
	 * ini menerima {@code null} apa adanya.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini; untuk baris yang belum pernah di-update
	 *         berisi waktu objek dibangun (lihat {@link #onUpdate()})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama penulis>"}, dipakai untuk log/debug
	 * dan sebagai label default pada komponen ZK yang menampilkan entity ini. Meng-override
	 * {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"} &mdash; wajar,
	 * karena properti {@code kode} milik base class tidak terpetakan dan selalu kosong di sini.
	 *
	 * <p><b>Catatan:</b> membaca field {@code nama} secara LANGSUNG, bukan lewat
	 * {@link #getNama()}, sehingga nama di sini TIDAK di-{@code trim}. Untuk baris yang belum
	 * disimpan {@code id} masih {@code null} sehingga hasilnya diawali "null-", dan objek yang
	 * benar-benar kosong tercetak sebagai "null-null".</p>
	 *
	 * @return gabungan id dan nama penulis dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode user Google Scholar (segmen {@code user=} pada URL profil, mis. {@code Z8ZcJboAAAAJ}).
	 * Kolom {@code user_id} bertanda {@code unique}. Bisa {@code null} untuk baris penampung
	 * penulis tanpa profil. Lihat {@link #getUserid()} &mdash; getter-nya menulis balik ke field
	 * ini.
	 */
	private String userid;
	/**
	 * Nama tampil penulis. Untuk baris hasil jalur profil berasal dari {@code div#gsc_prf_in};
	 * untuk baris hasil jalur pencarian berasal dari teks tautan penulis pada blok hasil, atau
	 * &mdash; bila artikel tak punya penulis bertautan &mdash; dari seluruh teks baris
	 * {@code div[class=gs_a]} (jadi bisa berupa gabungan beberapa nama plus nama jurnal, bukan
	 * satu orang). Dideklarasikan ulang karena properti {@code nama} milik
	 * {@link GeneralValueObject} tidak terpetakan Hibernate.
	 */
	private String nama;
	/**
	 * URL profil Google Scholar penulis, teks sentinel {@code "empty"} (penulis tanpa profil), atau
	 * {@code null}. <b>Bukan catatan bebas pengguna.</b> Dipakai sebagai target navigasi di UI dan
	 * sebagai sumber penurunan {@link #getUserid()}. Dideklarasikan ulang dengan alasan yang sama
	 * seperti {@code nama}.
	 */
	private String keterangan;
	/**
	 * URL foto profil penulis di Google ({@code img#gsc_prf_pup-img}, biasanya berdomain
	 * {@code googleusercontent.com}). Dipakai apa adanya sebagai {@code src} komponen
	 * {@code Image} ZK, jadi gambarnya diambil langsung dari server Google oleh peramban pengguna.
	 * Berfungsi juga sebagai <i>penanda sudah dilengkapi</i>:
	 * {@code GoogleScholarCrawlerByUser#updateDataAuthor} melewati baris yang nilainya sudah tidak
	 * {@code null}.
	 */
	private String imageLink;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Dipakai juga oleh kedua crawler
	 * ({@code GoogleScholarCrawler#startCrawl} dan {@code GoogleScholarCrawlerByUser#byUser}) saat
	 * penulis hasil crawl belum ada padanannya di basis data. Seluruh field dibiarkan kosong
	 * kecuali {@code tanggal_dirubah}, yang langsung diisi waktu server.
	 */
	public ScholarAuthor() {
	}

	/**
	 * @return primary key baris ({@code IDENTITY}, dibangkitkan basis data), atau {@code null}
	 *         bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Kolom ditandai {@code insertable = false} sehingga nilai yang
	 * diisikan manual di sini tidak ikut dikirim pada INSERT &mdash; setter ini praktis hanya
	 * dipakai Hibernate saat menghidrasi objek dari basis data.
	 *
	 * @param id primary key baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama tampil penulis. Override properti {@code nama} milik {@link GeneralValueObject} agar
	 * terpetakan ke kolom {@code nama} ({@code varchar(255)}, {@code NOT NULL}).
	 *
	 * <p>Dipakai {@code DetailArtikelHelper} dan
	 * {@code AmbilDataDariGoogleScholarBanyak} sebagai teks label/tombol penulis, serta oleh
	 * {@code GoogleScholarCrawler} sebagai kunci pencocokan
	 * ({@code Restrictions.eq("nama", authorName)}) khusus untuk baris penampung penulis tanpa
	 * profil &mdash; pencocokan {@code eq} yang peka besar-kecil huruf, sehingga variasi penulisan
	 * nama dari Google akan menghasilkan baris duplikat, bukan pembaruan.</p>
	 *
	 * <p><b>Murni membaca:</b> berbeda dari kembarannya {@code ScholarArticle#getNama()} yang
	 * membersihkan penanda {@code [PDF]}/{@code [BUKU]}/{@code [B]}/{@code [DOC]} dan MENULIS
	 * BALIK hasilnya ke field, getter ini hanya melakukan {@code trim} pada nilai yang
	 * dikembalikan tanpa mengubah state objek. Nilai di basis data karena itu bisa saja
	 * mengandung spasi di ujung tanpa pernah dinormalkan.</p>
	 *
	 * @return nama penulis tanpa spasi di ujung, atau {@code null} bila field belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama tampil penulis, tanpa validasi maupun normalisasi. Dipanggil kedua crawler
	 * setiap kali halaman Scholar diproses, jadi nama bisa berubah menyusul perubahan di sisi
	 * Google.
	 *
	 * @param nama nama tampil penulis; kolomnya {@code NOT NULL}, sehingga menyimpan objek dengan
	 *             nilai {@code null} akan gagal pada tingkat basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * URL profil Google Scholar penulis &mdash; kolom {@code keterangan} bertipe {@code text}.
	 * Override properti {@code keterangan} milik {@link GeneralValueObject} agar terpetakan.
	 *
	 * <p><b>Membalik jaminan base class:</b> {@link GeneralValueObject#getKeterangan()} berjanji
	 * tidak pernah mengembalikan {@code null} (mengubahnya menjadi {@code ""}); override ini
	 * mengembalikan nilai apa adanya. Pemanggil memang mengandalkan itu &mdash;
	 * {@code DetailArtikelHelper} dan {@code AmbilDataDariGoogleScholarBanyak} menguji
	 * {@code getKeterangan() == null || getKeterangan().equalsIgnoreCase("empty")} untuk memilih
	 * antara label mati dan tombol yang membuka profil.</p>
	 *
	 * <p>Selain untuk UI, nilai ini menjadi masukan {@link #ambilId(String)} di dalam
	 * {@link #getUserid()}; bentuk isinya karena itu ikut menentukan apakah write-back di getter
	 * tersebut terjadi.</p>
	 *
	 * <p><b>Catatan keamanan (di pemanggil, bukan di kelas ini).</b> Nilai kembalian dirangkai
	 * mentah ke dalam literal string JavaScript di {@code DetailArtikelHelper}
	 * ({@code Clients.evalJavaScript("popupCenter({url: '" + scholarAuthor.getKeterangan() +
	 * "', ...")}) dan dipakai langsung sebagai target {@code sendRedirect} pada tampilan mobile.
	 * Isi kolom ini berasal dari atribut {@code href} halaman HTML pihak ketiga, bukan dari input
	 * yang tervalidasi &mdash; tanda kutip tunggal di dalamnya akan keluar dari literal tersebut.
	 * Didokumentasikan apa adanya; tidak diubah di sini.</p>
	 *
	 * @return URL profil Scholar, teks {@code "empty"}, atau {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan URL profil Scholar (atau sentinel {@code "empty"}), tanpa validasi bentuk URL.
	 * Dipanggil {@code GoogleScholarCrawler} dengan tautan penulis hasil <i>scraping</i>, dan
	 * {@code GoogleScholarCrawlerByUser#byUser} dengan URL yang dirakit sendiri
	 * ({@code "https://scholar.google.com/citations?hl=id&amp;user=" + user}).
	 *
	 * <p><b>Efek tidak langsung:</b> setter ini TIDAK menyetel ulang {@code userid}. Bila
	 * {@code userid} sudah terisi, mengganti {@code keterangan} ke profil orang lain tidak akan
	 * memperbarui kode user &mdash; keduanya bisa jadi tidak konsisten. Sebaliknya bila
	 * {@code userid} masih {@code null}, panggilan {@link #getUserid()} berikutnya akan
	 * menurunkannya dari nilai baru ini dan menuliskannya ke field.</p>
	 *
	 * @param keterangan URL profil Google Scholar, {@code "empty"}, atau {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Utilitas statis: mengekstrak <b>kode user Google Scholar</b> dari sebuah URL profil, mis.
	 * mengembalikan {@code "Z8ZcJboAAAAJ"} untuk masukan
	 * {@code "https://scholar.google.com/citations?user=Z8ZcJboAAAAJ&amp;hl=id&amp;oi=sra"}.
	 *
	 * <p><b>Cara kerja.</b> URL dipecah dengan {@code split("&")} lalu tiap potongan dibaca sebagai
	 * pasangan {@code kunci=nilai} berdasarkan tanda {@code "="} PERTAMA, kedua sisinya
	 * di-{@code URLDecoder.decode} sebagai UTF-8. Potongan diterima bila kuncinya
	 * <b>{@code endsWith("user")}</b>, dan nilai pasangan TERAKHIR yang cocok itulah yang menang.</p>
	 *
	 * <p><b>Kenapa {@code endsWith}, bukan {@code equals}.</b> Karena URL dipecah hanya pada
	 * {@code "&"} (bukan lebih dulu pada {@code "?"}), potongan pertama masih membawa seluruh
	 * awalan skema+host+path. Untuk URL {@code ".../citations?user=XXX&hl=id"}, potongan pertama
	 * adalah {@code ".../citations?user=XXX"} sehingga kuncinya menjadi
	 * {@code "https://scholar.google.com/citations?user"} &mdash; hanya lolos berkat pencocokan
	 * akhiran. Konsekuensinya pencocokan ini <b>longgar</b>: kunci lain yang kebetulan berakhiran
	 * "user" (mis. {@code as_sdt_user}, {@code olduser}) juga akan diterima dan, karena yang
	 * menang adalah kecocokan terakhir, bisa menimpa nilai {@code user} yang benar.</p>
	 *
	 * <p><b>Kuirk dan keterbatasan yang perlu diketahui:</b></p>
	 * <ul>
	 * <li>Masukan yang tidak diawali {@code "https"} (setelah {@code trim}) langsung menghasilkan
	 * {@code null} tanpa diproses &mdash; termasuk URL {@code http://} biasa dan sentinel
	 * {@code "empty"} yang dipakai untuk penulis tanpa profil. Ini yang membuat baris sentinel
	 * aman dari pengurai ini.</li>
	 * <li>Potongan yang <b>tidak mengandung {@code "="}</b> membuat {@code indexOf} bernilai
	 * {@code -1} sehingga {@code substring(0, -1)} melempar
	 * {@code StringIndexOutOfBoundsException}. Pengecualian itu ditangkap {@code catch} di dalam
	 * loop &mdash; hasilnya bukan kegagalan, melainkan {@code printStackTrace} + satu entri
	 * {@code ErrorAuditUtil.record} untuk SETIAP potongan bermasalah, pada SETIAP pemanggilan.
	 * URL profil sesingkat {@code "https://scholar.google.com"} karena itu menghasilkan derau
	 * audit yang berulang, bukan error yang terlihat pengguna.</li>
	 * <li>Menulis {@code System.out.println("key ... value ...")} untuk setiap pasangan yang
	 * diproses &mdash; keluaran debug yang tertinggal di kode produksi. Karena
	 * {@link #getUserid()} memanggil method ini ulang setiap kali hasilnya {@code null},
	 * baris yang URL-nya tidak memuat kunci berakhiran "user" akan mencetak ulang log ini pada
	 * tiap pembacaan.</li>
	 * <li>Baris {@code userid = null;} di awal blok {@code try} adalah penugasan mubazir (variabel
	 * lokal sudah diinisialisasi {@code null} di deklarasinya).</li>
	 * <li>Bersifat {@code static} dan tidak menyentuh state instance maupun basis data; aman
	 * dipanggil dari mana saja.</li>
	 * </ul>
	 *
	 * <p><b>Pemanggil.</b> Dua tempat saja: {@link #getUserid()} di kelas ini, dan
	 * {@code ais.common.scholar.GoogleScholarCrawler#startCrawl} yang memakainya untuk memutuskan
	 * apakah seorang penulis hasil pencarian layak disimpan (penulis dengan {@code userid}
	 * {@code null}/kosong dilewati sama sekali pada cabang itu).</p>
	 *
	 * @param urlLink URL profil Google Scholar yang akan diurai; boleh {@code null}
	 * @return kode user Scholar yang ditemukan, atau {@code null} bila masukan {@code null}, tidak
	 *         diawali {@code "https"}, atau tidak memuat kunci berakhiran "user"
	 */
	public static String ambilId(String urlLink) {
		String userid = null;
		if (urlLink != null && urlLink.trim().startsWith("https")) {
			try {
				userid = null;
				String[] pairs = urlLink.split("&");

				for (String pair : pairs) {
					try {
						int idx = pair.indexOf("=");
						String key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8");
						String value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
						System.out.println("key " + key + " value " + value);
						if (key.endsWith("user"))
							userid = value;
					} catch (Exception ex) {
						ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/database/model/ScholarAuthor.java:124");
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/ScholarAuthor.java:128");
			}
		}
		return userid;
	}

	/**
	 * Kode user Google Scholar penulis ini &mdash; kolom {@code user_id}, bertanda
	 * {@code unique = true}. Inilah nilai yang dicocokkan dengan
	 * {@link Dosen#getGoogleScholar()} untuk mengaitkan sebuah profil Scholar dengan dosen AIS
	 * (pencocokan string, tanpa FK).
	 *
	 * <p><b>Getter ini MENULIS BALIK ke field.</b> Bila {@code userid} masih {@code null}, nilainya
	 * diturunkan dari {@link #getKeterangan()} lewat {@link #ambilId(String)} dan
	 * <b>disimpan ke field {@code this.userid}</b>, bukan sekadar dikembalikan. Niat aslinya
	 * tampak sebagai <i>lazy backfill</i> untuk baris lama yang terlanjur tersimpan tanpa kode
	 * user, tetapi konsekuensinya jauh melampaui memoisasi biasa:</p>
	 * <ul>
	 * <li><b>Bisa berujung UPDATE basis data.</b> Seluruh anotasi pemetaan kelas ini berada di
	 * getter ({@code @Id} pada {@link #getId()}), jadi Hibernate memakai <i>property access</i> dan
	 * membaca state entity lewat getter &mdash; termasuk saat menyusun <i>snapshot</i> dan saat
	 * <i>dirty checking</i> pada {@code flush}. Untuk entity persistent yang kolom
	 * {@code user_id}-nya {@code NULL} sementara {@code keterangan}-nya memuat URL profil yang
	 * bisa diurai, nilai turunan akan berbeda dari snapshot dan Hibernate mengirim
	 * {@code UPDATE public.scholar_author SET user_id = ?}. Artinya <b>operasi yang niatnya cuma
	 * membaca dapat menulis ke basis data</b>.</li>
	 * <li><b>Meninggalkan jejak audit permanen.</b> Kelas ini {@code @Audited}, sehingga tiap
	 * UPDATE tak diniatkan itu menambah baris revisi Envers, dan {@link #onUpdate()} ikut menimpa
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dengan identitas pengguna yang kebetulan
	 * sedang membuka layar.</li>
	 * <li><b>Berpotensi melanggar unique constraint di tempat tak terduga.</b> Karena
	 * {@code user_id} unik, nilai turunan yang bentrok dengan baris lain memunculkan
	 * {@code ConstraintViolationException} pada {@code flush} &mdash; mis. saat panel detail
	 * artikel dirender, bukan saat ada yang menyunting data.</li>
	 * <li><b>Tidak pernah men-cache hasil negatif.</b> Penjagaannya {@code if (userid == null)},
	 * sedangkan {@link #ambilId(String)} mengembalikan {@code null} untuk {@code keterangan}
	 * bernilai {@code null}/{@code "empty"}/URL tanpa kunci "user". Baris seperti itu menjalankan
	 * ulang seluruh pengurai (beserta {@code System.out.println}-nya) pada SETIAP pembacaan,
	 * termasuk setiap siklus dirty checking.</li>
	 * </ul>
	 *
	 * <p><b>Jalur nyata yang memicunya.</b> {@code GoogleScholarCrawlerByUser#updateDataAuthor}
	 * membaca {@code getUserid()} untuk menyusun URL profil, lalu memanggil
	 * {@code session.refresh(scholarAuthor)} (yang mengembalikan {@code userid} ke nilai basis
	 * data), menyetel nama/foto, dan {@code session.update} + {@code commit} &mdash; pada
	 * {@code flush} inilah getter dipanggil ulang dan nilai turunan ikut tertulis. Jalur kedua
	 * lewat koleksi: {@code ScholarArticle#getScholarAuthors()} di-{@code saveOrUpdate} bersama
	 * artikelnya, sehingga seluruh penulis dalam koleksi ikut ter-dirty-check. Jalur ketiga lewat
	 * UI: {@code DetailArtikelHelper} dan {@code AmbilDataDariGoogleScholarBanyak} memanggil
	 * {@code HibernateUtil.currentSession().refresh(scholarArticle)} lalu mengiterasi koleksi
	 * penulis, menjadikan objek-objek itu persistent pada sesi permintaan ZK.</p>
	 *
	 * <p><b>Kuirk turunan.</b> Bila {@code userid} maupun hasil penguraian {@code keterangan}
	 * sama-sama {@code null}, {@code updateDataAuthor} tetap melanjutkan dengan {@code user} =
	 * {@code null}: berkas cache menjadi {@code ".../null.txt.gz"} dan permintaan ke Scholar
	 * dikirim dengan parameter {@code user} bernilai {@code null}.</p>
	 *
	 * @return kode user Google Scholar, atau {@code null} bila field kosong dan tidak bisa
	 *         diturunkan dari {@code keterangan}
	 */
	@Column(name = "user_id", unique = true)
	public String getUserid() {
		if (userid == null) {
			userid = ScholarAuthor.ambilId(getKeterangan());
		}
		return userid;
	}

	/**
	 * Menetapkan kode user Google Scholar secara eksplisit, tanpa validasi. Dipanggil kedua
	 * crawler dengan nilai yang sudah mereka tentukan sendiri
	 * ({@code GoogleScholarCrawler} memakai hasil {@link #ambilId(String)} atas tautan penulis;
	 * {@code GoogleScholarCrawlerByUser#byUser} memakai kode yang diminta pemanggil).
	 *
	 * <p>Menyetel nilai non-{@code null} di sini sekaligus <b>mematikan</b> jalur penurunan
	 * otomatis pada {@link #getUserid()}, karena penjagaannya hanya menguji {@code userid == null}.
	 * Menyetel {@code null} kembali menghidupkannya lagi.</p>
	 *
	 * @param userid kode user Google Scholar; kolomnya unik, jadi nilai duplikat akan ditolak
	 *               basis data pada saat {@code flush}
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * @return URL foto profil penulis di server Google, atau {@code null} bila baris belum pernah
	 *         dilengkapi {@code GoogleScholarCrawlerByUser#updateDataAuthor}. Nilai {@code null}
	 *         inilah yang dipakai crawler sebagai penanda "belum dilengkapi", dan dipakai
	 *         {@code DetailArtikelHelper} untuk memutuskan apakah komponen {@code Image} dibuat
	 */
	@Column(name = "image_link", columnDefinition = "text")
	public String getImageLink() {
		return imageLink;
	}

	/**
	 * Menetapkan URL foto profil penulis, tanpa validasi. Diisi dari atribut {@code src} elemen
	 * {@code img#gsc_prf_pup-img} halaman profil Scholar. Perhatikan bahwa Jsoup mengembalikan
	 * <b>string kosong</b> (bukan {@code null}) bila elemen/atributnya tidak ditemukan &mdash;
	 * baris yang gagal di-parse karena itu tersimpan dengan {@code ""}, yang berbeda dari
	 * {@code null} dan membuat baris tersebut dianggap "sudah dilengkapi" sehingga tidak pernah
	 * dicoba ulang.
	 *
	 * @param imageLink URL foto profil; boleh {@code null} atau string kosong
	 */
	public void setImageLink(String imageLink) {
		this.imageLink = imageLink;
	}

}
