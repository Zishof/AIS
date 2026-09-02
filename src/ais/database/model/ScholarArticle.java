package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity satu <b>artikel/publikasi ilmiah hasil pengambilan otomatis dari Google Scholar</b>
 * ({@code scholar.google.com}). Tabel: {@code public.scholar_article}.
 *
 * <p>Satu baris mewakili satu artikel sebagaimana dikenali Google Scholar &mdash; judul, tautan ke
 * halaman artikel, tautan berkas PDF, deskripsi/metadata, kata kunci pencarian yang pernah
 * menemukannya, dan daftar penulis. Seperti {@link ScholarAuthor}, baris pada tabel ini
 * <b>tidak pernah diketik pengguna</b>; seluruhnya lahir dari dua crawler berbasis Jsoup di paket
 * {@code ais.common.scholar}. Kelas ini adalah anggota ketiga (dan pemegang sisi relasi) trio
 * Google Scholar: {@code ScholarArticle} &harr; {@link ScholarAuthor}, dengan
 * {@link SintaArticle} sebagai padanan untuk sumber SINTA.</p>
 *
 * <h2>Dari mana barisnya berasal</h2>
 * <ol>
 * <li><b>Jalur pencarian kata kunci</b> &mdash;
 * {@code ais.common.scholar.GoogleScholarCrawler#startCrawl(int, String)} mengunduh halaman hasil
 * pencarian {@code /scholar?q=&lt;katakunci&gt;} lalu, untuk tiap entri
 * ({@code div.gs_r.gs_or.gs_scl}), mengambil judul ({@code h3.gs_rt}), tautan ({@code h3.gs_rt
 * a[href]}), dan cuplikan deskripsi ({@code div.gs_rs}). Baris lama dicari dengan
 * {@code Restrictions.eq("link", articleLink)} (urut {@code id} menurun, {@code setMaxResults(1)});
 * bila belum ada dibuat baris baru dan barulah {@link #setKeterangan(String)} +
 * {@link #setScholarAuthors(Set)} diisi. Selanjutnya {@code nama}, {@code link}, {@code headers},
 * dan {@code kewords} diperbarui, lalu {@code saveOrUpdate} dalam transaksi native tersendiri.</li>
 * <li><b>Jalur sinkronisasi per dosen</b> &mdash;
 * {@code ais.common.scholar.GoogleScholarCrawlerByUser#byUser(String)} mengunduh halaman profil
 * {@code /citations?user=&lt;kode&gt;}, lalu untuk tiap tautan artikel ({@code a.gsc_a_at})
 * mengunduh halaman detailnya dan mengambil {@code link} ({@code a.gsc_vcd_title_link}),
 * {@code linkFile} ({@code div.gsc_vcd_title_ggi&gt;a}), serta SELURUH pasangan bidang-nilai
 * ({@code div.gs_scl} &rarr; {@code div.gsc_vcd_field}/{@code div.gsc_vcd_value}) yang dirakit
 * menjadi <b>satu objek JSON</b> dan disimpan ke {@code keterangan}. Penulis profil yang sedang
 * disinkronkan ditambahkan ke koleksi {@link #getScholarAuthors()} bila belum ada.</li>
 * </ol>
 *
 * <h2>Relasi ke {@link ScholarAuthor} &mdash; {@code @ManyToMany} SATU ARAH, dideklarasikan di sini</h2>
 * <p>Kelas ini <b>memegang satu-satunya sisi</b> relasi penulis&harr;artikel, lewat tabel jembatan
 * {@code article_has_scholar_author} ({@code joinColumns = article},
 * {@code inverseJoinColumns = scholar_author}, {@code cascade = MERGE}) yang dideklarasikan pada
 * {@link #getScholarAuthors()}. Di sisi {@link ScholarAuthor} <b>tidak ada</b> properti
 * {@code mappedBy} apa pun, sehingga arah navigasi yang mungkin hanya SATU:
 * <b>artikel &rarr; penulis</b>. Dari sebuah objek {@code ScholarAuthor} tidak bisa dicari daftar
 * artikelnya lewat pemetaan Hibernate; pemanggil yang membutuhkan arah sebaliknya harus menembak
 * tabel jembatan secara manual (dan tidak ada satu pun kode di codebase ini yang melakukannya).</p>
 *
 * <p>Konsekuensi lain: karena {@code cascade} hanya {@code MERGE} (bukan {@code PERSIST}), penulis
 * baru wajib sudah disimpan sendiri sebelum artikel di-{@code saveOrUpdate} &mdash; dan memang
 * kedua crawler menyimpan {@code ScholarAuthor} lebih dulu, masing-masing dalam transaksi
 * terpisah.</p>
 *
 * <h2>Relasi ke {@link ais.database.model.penelitiandanpengabdian.Artikel}</h2>
 * <p>Catatan publikasi "resmi" AIS adalah {@code penelitiandanpengabdian.Artikel}, yang memiliki
 * FK {@code scholar_article} ({@code @ManyToOne}, {@code unique = true} &mdash; jadi praktis 1:1)
 * berdampingan dengan FK sejajar {@code sinta_article} ke {@link SintaArticle}. Arah kepemilikan
 * ada di sisi {@code Artikel}; kelas ini tidak tahu-menahu tentang {@code Artikel} yang
 * merujuknya.</p>
 *
 * <p><b>Beberapa getter {@code Artikel} MEMBACA entity ini setiap kali dipanggil dan menimpa
 * fieldnya sendiri</b> &mdash; {@code Artikel#getJudul()} menyalin {@link #getNama()},
 * {@code Artikel#getReferensi()} menyalin {@link #getLink()}, {@code Artikel#getAbstrak()} dan
 * {@code Artikel#getTahun()} mengurai {@link #getKeterangan()} sebagai JSON (mengambil kunci
 * {@code "Deskripsi"} dan {@code "Tanggal terbit"}). Artinya membaca satu {@code Artikel} yang
 * bertaut ke Scholar bisa memicu rantai write-back berlapis: {@link #getNama()} menulis ke field
 * {@code nama} DI SINI, lalu {@code Artikel#getJudul()} menulis hasilnya ke field {@code judul} di
 * sana &mdash; dua baris berpotensi ter-UPDATE dari operasi yang niatnya cuma membaca.</p>
 *
 * <h2>Perbandingan dengan {@link SintaArticle}</h2>
 * <p>Keduanya menampung publikasi ilmiah hasil crawling, tetapi bentuknya berbeda tajam:</p>
 * <ul>
 * <li><b>Penulis.</b> Di {@link SintaArticle} penulis hanyalah satu kolom teks bebas
 * ({@code SintaArticle#getAuthor()}) dan pemilik publikasi adalah FK {@code dosen} yang sungguhan.
 * Di sini justru sebaliknya: penulis dipromosikan menjadi entity tersendiri
 * ({@link ScholarAuthor}) lewat {@code @ManyToMany}, tetapi <b>kaitan ke {@link Dosen} hilang sama
 * sekali</b> &mdash; tidak ada FK {@code dosen} di kelas ini. Penjodohan ke dosen hanya mungkin
 * secara tidak langsung, lewat pencocokan string {@link Dosen#getGoogleScholar()} &harr;
 * {@code ScholarAuthor#getUserid()} saat runtime.</li>
 * <li><b>Metadata terstruktur.</b> {@link SintaArticle} punya kolom sendiri untuk
 * {@code jurnal}/{@code vol}/{@code issue}/{@code page}/{@code tahun}. Di sini semua itu
 * dimampatkan menjadi satu blob JSON di dalam {@code keterangan} (jalur per-dosen saja) &mdash;
 * tidak bisa dicari/diurutkan lewat SQL.</li>
 * <li><b>Pembersihan judul.</b> {@code SintaArticle#getNama()} membersihkan judul tanpa menulis
 * balik; {@link #getNama()} di sini <b>menulis balik</b> (lihat di bawah).</li>
 * </ul>
 *
 * <h2>Hal-hal non-obvious</h2>
 * <ul>
 * <li><b>{@link #getNama()} MENULIS BALIK ke field terpetakan.</b> Getter judul membuang penanda
 * {@code [PDF]}/{@code [BUKU]}/{@code [B]}/{@code [DOC]} yang disisipkan Google pada teks judul
 * dan <b>menyimpan hasilnya ke {@code this.nama}</b>, bukan sekadar mengembalikannya. Karena
 * seluruh anotasi pemetaan kelas ini ada di getter (<i>property access</i>), Hibernate membaca
 * state entity lewat getter juga saat <i>dirty checking</i> pada {@code flush} &mdash; sehingga
 * sekadar MEMBACA entity persistent dapat menghasilkan {@code UPDATE public.scholar_article SET
 * nama = ...} beserta baris revisi Envers dan pemicuan {@link #onUpdate()}. Ini pola yang sama
 * dengan {@code ScholarAuthor#getUserid()}, dan rinciannya dibahas di {@link #getNama()}.</li>
 * <li><b>{@code keterangan} berisi DUA bentuk data yang sama sekali berbeda.</b> Dari jalur
 * pencarian kata kunci isinya <b>cuplikan deskripsi berupa teks biasa</b>; dari jalur per-dosen
 * isinya <b>string JSON</b> berisi seluruh bidang metadata halaman detail Scholar (mis.
 * {@code {"Penulis":"...","Tanggal terbit":"2019/5/1","Jurnal":"...","Deskripsi":"..."}}). Tidak
 * ada penanda apa pun yang membedakan keduanya. Pemanggil yang mengasumsikan JSON
 * ({@code Artikel#getAbstrak()}, {@code Artikel#getTahun()},
 * {@code DetailArtikelHelper} saat membuat {@code JurnalPenelitian}) menangani kegagalan parse
 * dengan {@code catch} sunyi, sehingga baris hasil jalur kata kunci diam-diam kehilangan
 * jurnal/tahun dan justru <b>menyimpan seluruh cuplikan mentah ke kolom {@code abstrak}</b> milik
 * {@code Artikel}. Lihat {@link #getKeterangan()}.</li>
 * <li><b>Kontrak {@code keterangan} dibalik dari base class.</b> {@link GeneralValueObject#getKeterangan()}
 * menjamin hasil non-{@code null} (mengubah {@code null} menjadi {@code ""}); override di sini
 * mengembalikan nilai apa adanya termasuk {@code null}. Perilaku serupa sudah tercatat pada
 * {@link Bank}, {@link SintaArticle}, {@code PendaftaranSidang}, dan {@link ScholarAuthor}.</li>
 * <li><b>Penulis hanya terisi saat baris DIBUAT (jalur kata kunci).</b> Pada
 * {@code GoogleScholarCrawler#startCrawl}, {@link #setScholarAuthors(Set)} dipanggil <b>hanya di
 * dalam cabang {@code if (tempArticle == null)}</b>. Artikel yang sudah ada di basis data tidak
 * pernah mendapat tambahan penulis dari jalur ini, meski pencarian berikutnya menemukan penulis
 * baru. Hanya jalur per-dosen yang menambah anggota ke koleksi yang sudah ada.</li>
 * <li><b>{@code kewords} adalah riwayat kata kunci, bukan kata kunci artikel.</b> Isinya daftar
 * kata kunci PENCARIAN yang pernah memunculkan artikel ini, disambung dengan {@code ", "} setiap
 * kali crawl kata kunci baru menemukannya kembali. Lihat {@link #getKewords()}.</li>
 * <li><b>{@code headers} menyimpan seluruh header respons HTTP</b> hasil membuka tautan artikel
 * langsung dari server aplikasi (bukan dari peramban pengguna). Lihat catatan di
 * {@link #getHeaders()}.</li>
 * <li><b>Tidak ada layar master/CRUD.</b> Tidak ada {@code ScholarArticleAction}, tidak ada
 * {@code .zul}, tidak ada entri menu. Yang menyentuh entity ini hanyalah dialog modal
 * {@link ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak} dan panel detail
 * {@link ais.action.master.helper.DetailArtikelHelper}. Konsekuensinya tidak ada pemeriksaan
 * {@code checkPrevilages}/{@code MUST_CHECKED} sama sekali pada jalur Scholar; satu-satunya
 * penjaga adalah visibilitas tombol pemicunya.</li>
 * <li><b>Sebagian pemicunya mati, tapi TIDAK semuanya.</b> Tombol "Ambil Artikel dari Google
 * Scholar" di {@code FilePerkuliahanHelper} dan pada cabang PERTAMA
 * {@code DataPunyaArtikelHelper} di-{@code setVisible(false)}, dan tombol "Singkronkan dengan
 * Google Scholar" di {@code DetailArtikelHelper} dijaga {@code if (false &amp;&amp; ...)} sehingga
 * jalur per-dosen ({@code GoogleScholarCrawlerByUser#byUser}) praktis tidak pernah dipicu dari UI.
 * Namun cabang KEDUA {@code DataPunyaArtikelHelper} (blok toolbar untuk pengguna non-mahasiswa)
 * memasang tombol yang sama <b>tanpa {@code setVisible(false)}</b> &mdash; jadi jalur pencarian
 * kata kunci masih hidup, dan baris {@code scholar_article} yang terlanjur ada tetap dibaca dan
 * dirender oleh {@code DetailArtikelHelper}.</li>
 * <li><b>Terdaftar sebagai kelas ber-tautan DSpace.</b> {@code DspaceInformation} memasukkan
 * {@code ScholarArticle.class} ke daftar {@code linksForClass}, sehingga
 * {@code DspaceInformation#showLink} dapat menampilkan tautan repositori untuk baris entity ini.</li>
 * <li><b>Tidak ada mekanisme pembersihan/deduplikasi.</b> Kunci pencocokan satu-satunya adalah
 * {@code link} dengan {@code Restrictions.eq} yang peka besar-kecil huruf dan tanpa normalisasi
 * URL; variasi tautan untuk artikel yang sama (mis. beda parameter query) akan menghasilkan baris
 * duplikat, bukan pembaruan.</li>
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
 * <li><b>Isi artikel</b> &mdash; {@link #getNama()}/{@link #setNama(String)} (judul, satu-satunya
 * anggota berlogika nyata), {@link #getKeterangan()}/{@link #setKeterangan(String)} (deskripsi
 * ATAU blob JSON metadata), {@link #getKewords()}/{@link #setKewords(String)} (riwayat kata kunci
 * pencarian).</li>
 * <li><b>Tautan</b> &mdash; {@link #getLink()}/{@link #setLink(String)} (halaman artikel; sekaligus
 * kunci pencocokan crawler), {@link #getLinkFile()}/{@link #setLinkFile(String)} (berkas PDF),
 * {@link #getHeaders()}/{@link #setHeaders(String)} (header respons HTTP tautan artikel).</li>
 * <li><b>Relasi</b> &mdash; {@link #getScholarAuthors()}/{@link #setScholarAuthors(Set)}.</li>
 * </ol>
 *
 * <h2>Verifikasi pola berulang paket ini</h2>
 * <ul>
 * <li><b>Getter yang menulis balik ke field/DB:</b> ADA satu &mdash; {@link #getNama()}. Dugaan
 * yang tercatat saat mendokumentasikan {@link SintaArticle} dengan demikian TERKONFIRMASI dari
 * kode kelas ini sendiri. Getter lain ({@link #getKewords()}, {@link #getKeterangan()},
 * {@link #getLink()}, {@link #getLinkFile()}, {@link #getHeaders()}) murni membaca.</li>
 * <li><b>Getter yang menutup sesi Hibernate:</b> TIDAK ADA di kelas ini. Seluruh
 * {@code HibernateUtil.closeSession()} terjadi di crawler/helper pemanggil, bukan di entity.</li>
 * <li><b>Getter destruktif</b> (menghapus/mengosongkan data secara permanen): TIDAK ADA.
 * {@link #getNama()} mengubah nilai field, tetapi hanya membuang penanda format &mdash; bukan
 * menghapus baris atau relasi.</li>
 * <li><b>Setter yang menolak nilai kosong:</b> ADA &mdash; {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} mengabaikan {@code null}/string kosong, jadi field audit tidak pernah
 * bisa dikosongkan kembali setelah terisi. Setter lain menerima apa adanya.</li>
 * <li><b>Getter yang menormalkan hasil tanpa menyentuh field:</b> ADA &mdash; {@link #getKewords()}
 * mengembalikan {@code ""} untuk {@code null} (pola pengaman yang justru TIDAK dipakai
 * {@link #getKeterangan()}).</li>
 * </ul>
 *
 * <p><b>Anotasi kelas.</b> {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menyertakan kolom yang benar-benar berubah pada INSERT/UPDATE &mdash; ini meredam (tapi tidak
 * menghilangkan) dampak write-back {@link #getNama()}: yang terkirim hanya kolom {@code nama},
 * bukan seluruh baris. {@code @Audited} (Hibernate Envers) menyalin setiap versi baris ke tabel
 * revisi, sehingga UPDATE yang tidak diniatkan pun meninggalkan jejak permanen; riwayat itu pula
 * yang ditampilkan tombol {@code RevisiHelper.createNewRevisi(ScholarArticle.class, ...)} pada
 * panel detail artikel. Entity terdaftar di {@code hibernate.cfg.xml} sebagai
 * {@code &lt;mapping class="ais.database.model.ScholarArticle" /&gt;}.</p>
 *
 * <p><b>Catatan:</b> komentar generator "Bank generated by hbm2java" yang sebelumnya menempati
 * posisi ini adalah sisa salin-tempel dari {@link Bank} (sumber asli komentar itu) dan tidak
 * menggambarkan kelas ini.</p>
 *
 * @see ScholarAuthor
 * @see SintaArticle
 * @see ais.database.model.penelitiandanpengabdian.Artikel
 * @see ais.common.scholar.GoogleScholarCrawler
 * @see ais.common.scholar.GoogleScholarCrawlerByUser
 * @see ais.action.master.library.helper.AmbilDataDariGoogleScholarBanyak
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "scholar_article")

public class ScholarArticle extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, diwarisi lewat {@link java.io.Serializable} pada
	 * {@link GeneralValueObject}. Nilainya tetap agar objek yang pernah di-serialisasi (mis.
	 * tersimpan di sesi ZK selama dialog {@code AmbilDataDariGoogleScholarBanyak} terbuka) tetap
	 * kompatibel.
	 *
	 * <p><b>Kuirk:</b> nilai konstanta ini identik dengan yang ada di {@link ScholarAuthor},
	 * {@link SintaArticle}, {@link Pesan}, dan {@link DspaceInformation} &mdash; artefak
	 * salin-tempel yang sama seperti komentar generator "Bank generated by hbm2java". Tidak
	 * berdampak fungsional karena {@code serialVersionUID} hanya dibandingkan antar-versi kelas
	 * YANG SAMA.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key ({@code public.scholar_article.id}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila baris belum
	 *         pernah di-update sejak dibuat &mdash; kondisi yang lazim di sini, karena baris
	 *         {@code scholar_article} umumnya ditulis proses crawler, bukan sesi pengguna
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
	 * <p><b>Penting untuk entity ini:</b> sama seperti {@link ScholarAuthor}, callback ini
	 * <b>tidak selalu menandai perubahan yang diniatkan</b>. Karena {@link #getNama()} menulis
	 * balik ke field terpetakan, <i>dirty checking</i> Hibernate saat {@code flush} bisa
	 * memunculkan UPDATE dari operasi yang niatnya cuma membaca &mdash; dan callback ini akan ikut
	 * mencatat pengguna yang kebetulan sedang membuka panel detail artikel sebagai "pengubah
	 * terakhir". Nama yang tercatat di {@code oleh} karena itu tidak bisa dianggap sebagai bukti
	 * seseorang benar-benar menyunting data artikel.</p>
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
	 * Representasi teks singkat berformat {@code "<id>-<judul artikel>"}, dipakai untuk log/debug
	 * dan sebagai label default pada komponen ZK yang menampilkan entity ini &mdash; termasuk
	 * baris {@code System.out.println("volumes => " + volumes)} pada tombol Simpan dialog
	 * {@code AmbilDataDariGoogleScholarBanyak}. Meng-override {@link GeneralValueObject#toString()}
	 * yang berformat {@code "kode - nama"} &mdash; wajar, karena properti {@code kode} milik base
	 * class tidak terpetakan dan selalu kosong di sini.
	 *
	 * <p><b>Catatan:</b> membaca field {@code nama} secara LANGSUNG, bukan lewat {@link #getNama()},
	 * sehingga judul di sini TIDAK dibersihkan dari penanda {@code [PDF]}/{@code [BUKU]}/{@code [B]}/
	 * {@code [DOC]} dan TIDAK di-{@code trim} &mdash; berguna justru karena menampilkan nilai
	 * mentah yang tersimpan. Untuk baris yang belum disimpan {@code id} masih {@code null} sehingga
	 * hasilnya diawali "null-", dan objek yang benar-benar kosong tercetak sebagai "null-null".</p>
	 *
	 * @return gabungan id dan judul artikel (mentah) dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Judul artikel sebagaimana ditampilkan Google Scholar, sering masih memuat penanda jenis
	 * berkas di depannya ({@code "[PDF] Judul ..."}). Dideklarasikan ulang karena properti
	 * {@code nama} milik {@link GeneralValueObject} tidak terpetakan Hibernate. Lihat
	 * {@link #getNama()} &mdash; getter-nya MENULIS BALIK ke field ini.
	 */
	private String nama;
	/**
	 * Deskripsi artikel &mdash; <b>dua bentuk berbeda tergantung jalur crawler</b>: cuplikan teks
	 * biasa (jalur pencarian kata kunci) atau string JSON berisi seluruh bidang metadata halaman
	 * detail Scholar (jalur sinkronisasi per dosen). Dideklarasikan ulang dengan alasan yang sama
	 * seperti {@code nama}. Rincian dan konsekuensinya di {@link #getKeterangan()}.
	 */
	private String keterangan;
	/**
	 * URL halaman artikel di penerbit/repositori (bukan URL Google Scholar), diambil dari atribut
	 * {@code href} judul hasil pencarian atau dari {@code a.gsc_vcd_title_link} pada halaman detail.
	 * <b>Sekaligus kunci pencocokan</b> yang dipakai kedua crawler untuk memutuskan apakah artikel
	 * sudah ada di basis data ({@code Restrictions.eq("link", articleLink)}).
	 */
	private String link;
	/**
	 * URL berkas artikel (biasanya PDF) yang ditawarkan Google Scholar, dari
	 * {@code div.gsc_vcd_title_ggi&gt;a}. Hanya diisi jalur sinkronisasi per dosen; jalur pencarian
	 * kata kunci tidak pernah menyentuhnya. Lihat {@link #getLinkFile()}.
	 */
	private String linkFile;
	/**
	 * Riwayat kata kunci PENCARIAN yang pernah memunculkan artikel ini, disambung dengan
	 * {@code ", "}. <b>Bukan</b> kata kunci (keywords) yang dicantumkan penulis pada artikelnya.
	 * Perhatikan ejaan nama field/kolom yang salah ketik ({@code kewords}, bukan {@code keywords})
	 * &mdash; ejaan itu ikut menjadi nama kolom basis data dan nama properti Hibernate, jadi
	 * dipakai apa adanya pada {@code Restrictions.ilike("kewords", ...)}. Lihat {@link #getKewords()}.
	 */
	private String kewords;
	/**
	 * Seluruh header respons HTTP hasil membuka {@link #getLink()} dari server aplikasi, disimpan
	 * sebagai hasil {@code Map#toString()}. Lihat {@link #getHeaders()} untuk cara pengisian dan
	 * catatan risikonya.
	 */
	private String headers;

	/**
	 * Daftar penulis artikel ini. Inisialisasi ke {@link HashSet} kosong agar
	 * {@code getScholarAuthors().add(...)} pada {@code GoogleScholarCrawlerByUser#byUser} aman
	 * dipanggil untuk artikel yang baru dibuat. Lihat {@link #getScholarAuthors()} untuk pemetaan
	 * dan arah relasinya.
	 */
	private Set<ScholarAuthor> scholarAuthors = new HashSet<ScholarAuthor>();

	/**
	 * Daftar penulis artikel ini &mdash; <b>satu-satunya sisi relasi</b> penulis&harr;artikel di
	 * seluruh codebase.
	 *
	 * <p><b>Pemetaan.</b> {@code @ManyToMany} lewat tabel jembatan
	 * {@code article_has_scholar_author}: kolom {@code article} menunjuk ke baris ini, kolom
	 * {@code scholar_author} menunjuk ke {@link ScholarAuthor}. Karena {@link ScholarAuthor}
	 * <b>tidak</b> mendeklarasikan sisi {@code mappedBy}, relasi ini <b>UNIDIRECTIONAL</b>: dari
	 * artikel bisa dibaca daftar penulisnya, tetapi dari penulis TIDAK bisa dinavigasi ke daftar
	 * artikelnya.</p>
	 *
	 * <p><b>Cascade hanya {@code MERGE}.</b> Menyimpan artikel TIDAK ikut menyimpan penulis baru
	 * yang belum pernah di-{@code persist} &mdash; keduanya crawler karena itu menyimpan
	 * {@code ScholarAuthor} lebih dulu dalam transaksi terpisah sebelum
	 * {@code saveOrUpdate(tempArticle)}. Sebaliknya, karena {@code MERGE} ikut merambat, operasi
	 * merge pada artikel akan menyentuh seluruh penulis dalam koleksi &mdash; dan pada saat itulah
	 * write-back {@code ScholarAuthor#getUserid()} bisa ikut terpicu.</p>
	 *
	 * <p><b>Pemuatan lazy.</b> Tanpa {@code fetch = EAGER}, koleksi ini dimuat malas. Kedua
	 * pemanggil UI ({@code DetailArtikelHelper} dan {@code AmbilDataDariGoogleScholarBanyak})
	 * karena itu memanggil {@code HibernateUtil.currentSession().refresh(scholarArticle)} lebih
	 * dulu sebelum mengiterasi koleksi ini &mdash; {@code refresh} itu juga yang menjadikan objek
	 * penulis persistent pada sesi permintaan ZK.</p>
	 *
	 * <p><b>Kuirk pengisian.</b> Pada {@code GoogleScholarCrawler#startCrawl} (jalur kata kunci),
	 * {@link #setScholarAuthors(Set)} hanya dipanggil ketika artikelnya BARU dibuat; artikel yang
	 * sudah ada tidak pernah mendapat penulis tambahan dari jalur itu. Jalur per-dosen
	 * ({@code GoogleScholarCrawlerByUser#byUser}) berbeda &mdash; ia memeriksa keanggotaan lewat
	 * {@code author.getUserid().equals(user)} lalu menambahkan penulis ke koleksi yang sudah ada
	 * bila belum terdaftar. Karena pemeriksaan itu memanggil {@code ScholarAuthor#getUserid()},
	 * penulis lain dalam koleksi bisa ikut mengalami write-back {@code user_id}.</p>
	 *
	 * @return himpunan penulis artikel; tidak pernah {@code null} (default {@link HashSet} kosong),
	 *         tetapi bisa kosong untuk artikel yang belum/tidak berhasil dipetakan penulisnya
	 */
	@ManyToMany(targetEntity = ScholarAuthor.class, cascade = { CascadeType.MERGE })
	@JoinTable(name = "article_has_scholar_author", joinColumns = @JoinColumn(name = "article"), inverseJoinColumns = @JoinColumn(name = "scholar_author"))
	public Set<ScholarAuthor> getScholarAuthors() {
		return scholarAuthors;
	}

	/**
	 * Mengganti SELURUH himpunan penulis artikel ini (bukan menambah). Dipanggil satu tempat saja:
	 * {@code GoogleScholarCrawler#startCrawl}, dan hanya untuk artikel yang baru dibuat.
	 *
	 * <p><b>Peringatan:</b> menyetel {@code null} akan mematikan jaminan non-{@code null} yang
	 * diberikan inisialisasi field, sehingga {@code getScholarAuthors().add(...)} pada jalur
	 * per-dosen akan melempar {@code NullPointerException}. Tidak ada pemanggil yang melakukannya
	 * saat ini, tetapi setter ini tidak menjaganya.</p>
	 *
	 * @param scholarAuthors himpunan penulis pengganti
	 */
	public void setScholarAuthors(Set<ScholarAuthor> scholarAuthors) {
		this.scholarAuthors = scholarAuthors;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Dipakai juga oleh kedua crawler
	 * ({@code GoogleScholarCrawler#startCrawl} dan {@code GoogleScholarCrawlerByUser#byUser}) saat
	 * artikel hasil crawl belum ada padanannya di basis data. Seluruh field dibiarkan kosong
	 * kecuali {@code tanggal_dirubah} (waktu server) dan {@code scholarAuthors} ({@link HashSet}
	 * kosong).
	 */
	public ScholarArticle() {
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
	 * Judul artikel, dibersihkan dari penanda jenis berkas yang disisipkan Google Scholar. Override
	 * properti {@code nama} milik {@link GeneralValueObject} agar terpetakan ke kolom {@code nama}
	 * ({@code varchar(255)}, {@code NOT NULL}).
	 *
	 * <p><b>Apa yang dibersihkan.</b> Empat penanda dibuang dengan
	 * {@code StringUtils.replace} (peka besar-kecil huruf, mengganti SEMUA kemunculan, bukan hanya
	 * di awal): {@code [PDF]}, {@code [BUKU]}, {@code [B]}, {@code [DOC]}. Google juga memakai
	 * penanda lain yang TIDAK ditangani di sini (mis. {@code [HTML]}, {@code [CITATION]},
	 * {@code [KUTIPAN]}), jadi pembersihan ini tidak lengkap.</p>
	 *
	 * <p><b>Getter ini MENULIS BALIK ke field terpetakan.</b> Hasil pembersihan disimpan ke
	 * {@code this.nama}, bukan sekadar dikembalikan &mdash; berbeda dari
	 * {@code SintaArticle#getNama()} dan {@code ScholarAuthor#getNama()} yang keduanya hanya
	 * menormalkan nilai kembalian. Konsekuensinya:</p>
	 * <ul>
	 * <li><b>Bisa berujung UPDATE basis data.</b> Seluruh anotasi pemetaan kelas ini berada di
	 * getter ({@code @Id} pada {@link #getId()}), jadi Hibernate memakai <i>property access</i> dan
	 * membaca state entity lewat getter &mdash; termasuk saat menyusun <i>snapshot</i> dan saat
	 * <i>dirty checking</i> pada {@code flush}. Untuk baris persistent yang judulnya masih memuat
	 * penanda, nilai bersih akan berbeda dari snapshot dan Hibernate mengirim
	 * {@code UPDATE public.scholar_article SET nama = ?}. Artinya <b>operasi yang niatnya cuma
	 * merender daftar artikel dapat menulis ke basis data</b>.</li>
	 * <li><b>Meninggalkan jejak audit permanen.</b> Kelas ini {@code @Audited}, sehingga tiap
	 * UPDATE tak diniatkan itu menambah baris revisi Envers (yang lalu ikut tampil pada tombol
	 * riwayat {@code RevisiHelper}), dan {@link #onUpdate()} menimpa
	 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} dengan identitas pengguna yang kebetulan
	 * sedang membuka layar.</li>
	 * <li><b>Idempoten setelah pembersihan pertama.</b> Setelah penanda hilang, panggilan
	 * berikutnya tidak lagi mengubah nilai, sehingga UPDATE liar hanya terjadi sekali per baris
	 * bermasalah.</li>
	 * <li><b>Spasi sisa TIDAK ikut tersimpan.</b> {@code replace} hanya membuang penandanya, bukan
	 * spasi yang mengikutinya, sehingga judul {@code "[PDF] Analisis ..."} tersimpan sebagai
	 * {@code " Analisis ..."} (berspasi di depan). {@code trim()} hanya diterapkan pada nilai
	 * <b>kembalian</b> &mdash; nilai di basis data tetap berspasi. Itu sebabnya pencarian
	 * {@code Restrictions.ilike("nama", ..., ANYWHERE)} tetap bekerja, tetapi pengurutan
	 * {@code Order.asc("nama")} di {@code AmbilDataDariGoogleScholarBanyak#initCriteria} menempatkan
	 * judul-judul berspasi awal itu di depan seluruh daftar.</li>
	 * </ul>
	 *
	 * <p><b>Efek berantai lewat {@code Artikel}.</b>
	 * {@code penelitiandanpengabdian.Artikel#getJudul()} memanggil getter ini dan menyalin hasilnya
	 * ke field {@code judul} miliknya sendiri, yang juga terpetakan &mdash; jadi satu pembacaan
	 * dapat memicu UPDATE pada DUA tabel sekaligus.</p>
	 *
	 * <p><b>Catatan panjang kolom.</b> Kolom dibatasi 255 karakter sementara judul yang dipanen
	 * dari Google tidak dibatasi apa pun oleh crawler; judul yang lebih panjang akan gagal pada
	 * tingkat basis data saat {@code saveOrUpdate}, bukan tervalidasi di sini.</p>
	 *
	 * @return judul artikel tanpa penanda {@code [PDF]}/{@code [BUKU]}/{@code [B]}/{@code [DOC]}
	 *         dan tanpa spasi di ujung, atau {@code null} bila field belum terisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama != null) {
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "[PDF]", "");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "[BUKU]", "");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "[B]", "");
			nama = org.apache.commons.lang3.StringUtils.replace(nama, "[DOC]", "");
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan judul artikel, tanpa validasi maupun pembersihan. Dipanggil kedua crawler pada
	 * SETIAP pemrosesan (baik untuk artikel baru maupun yang sudah ada), dengan teks judul mentah
	 * hasil {@code Jsoup} &mdash; termasuk penanda {@code [PDF]}/{@code [BUKU]} yang baru dibuang
	 * kemudian oleh {@link #getNama()}.
	 *
	 * @param nama judul artikel; kolomnya {@code NOT NULL} sepanjang 255 karakter, sehingga
	 *             menyimpan objek dengan nilai {@code null} atau judul yang terlalu panjang akan
	 *             gagal pada tingkat basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Deskripsi/metadata artikel &mdash; kolom {@code keterangan} bertipe {@code text}. Override
	 * properti {@code keterangan} milik {@link GeneralValueObject} agar terpetakan.
	 *
	 * <p><b>Membalik jaminan base class:</b> {@link GeneralValueObject#getKeterangan()} berjanji
	 * tidak pernah mengembalikan {@code null} (mengubahnya menjadi {@code ""}); override ini
	 * mengembalikan nilai apa adanya. Perilaku serupa sudah tercatat pada {@link Bank},
	 * {@link SintaArticle}, {@code PendaftaranSidang}, dan {@link ScholarAuthor}.</p>
	 *
	 * <p><b>DUA bentuk isi yang tidak dibedakan apa pun.</b></p>
	 * <ul>
	 * <li><b>Teks biasa</b> &mdash; dari {@code GoogleScholarCrawler#startCrawl}: cuplikan
	 * deskripsi hasil pencarian ({@code div.gs_rs}). Hanya diisi saat baris DIBUAT; crawl ulang
	 * atas artikel yang sudah ada tidak memperbaruinya.</li>
	 * <li><b>String JSON</b> &mdash; dari {@code GoogleScholarCrawlerByUser#byUser}: seluruh
	 * pasangan bidang-nilai halaman detail Scholar ({@code {"Penulis":"...","Tanggal terbit":
	 * "2019/5/1","Jurnal":"...","Deskripsi":"..."}}). Ditulis ulang setiap sinkronisasi.</li>
	 * </ul>
	 *
	 * <p><b>Konsekuensi bagi pemanggil.</b> Beberapa pemanggil mengasumsikan bentuk JSON dan
	 * membungkus kegagalan parse dengan {@code catch} sunyi:
	 * {@code Artikel#getAbstrak()} (kunci {@code "Deskripsi"}), {@code Artikel#getTahun()} (kunci
	 * {@code "Tanggal terbit"}), dan {@code DetailArtikelHelper} saat menentukan
	 * {@code JurnalPenelitian} (kunci {@code "Jurnal"}). Untuk baris hasil jalur kata kunci,
	 * ketiganya gagal tanpa pesan: jurnal jatuh ke "Jurnal Default", tahun jatuh ke tahun berjalan,
	 * dan &mdash; ini yang paling mengganggu &mdash; {@code Artikel#getAbstrak()} sudah terlanjur
	 * menyalin <b>seluruh string mentah</b> ke field {@code abstrak} sebelum mencoba parse, jadi
	 * cuplikan pencarian tersimpan sebagai "abstrak" publikasi.</p>
	 *
	 * <p><b>Catatan keamanan (di pemanggil, bukan di kelas ini).</b> Nilai kembalian dirangkai
	 * mentah ke dalam markup di {@code AmbilDataDariGoogleScholarBanyak}
	 * ({@code new MyHtml("&lt;div style='font-size:8px'&gt;" + scholarArticle.getKeterangan() +
	 * "&lt;/div&gt;")}) &mdash; komponen {@code MyHtml} merender isinya sebagai HTML mentah,
	 * sementara isi kolom ini berasal dari halaman pihak ketiga yang tidak tervalidasi.
	 * Didokumentasikan apa adanya; tidak diubah di sini.</p>
	 *
	 * @return cuplikan deskripsi, string JSON metadata, atau {@code null} bila belum terisi
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan deskripsi/metadata artikel, tanpa validasi bentuk. Dipanggil dua tempat dengan
	 * bentuk data yang berbeda: {@code GoogleScholarCrawler#startCrawl} mengisinya dengan cuplikan
	 * teks biasa (hanya untuk artikel yang baru dibuat), dan
	 * {@code GoogleScholarCrawlerByUser#byUser} mengisinya dengan {@code jsonObject.toString()}
	 * pada setiap sinkronisasi.
	 *
	 * <p><b>Akibatnya:</b> menjalankan sinkronisasi per dosen atas artikel yang sebelumnya lahir
	 * dari pencarian kata kunci akan <b>menimpa</b> cuplikan deskripsi dengan blob JSON &mdash;
	 * dan tidak ada jalur yang mengembalikannya.</p>
	 *
	 * @param keterangan cuplikan deskripsi, string JSON metadata, atau {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * URL halaman artikel di penerbit/repositori aslinya (bukan URL Google Scholar) &mdash; kolom
	 * {@code link} bertipe {@code text}.
	 *
	 * <p><b>Peran ganda:</b> selain untuk navigasi di UI, nilai ini adalah <b>kunci pencocokan
	 * satu-satunya</b> yang dipakai kedua crawler untuk memutuskan apakah artikel sudah ada
	 * ({@code Restrictions.eq("link", articleLink)}, urut {@code id} menurun,
	 * {@code setMaxResults(1)}). Pencocokan {@code eq} peka besar-kecil huruf dan tanpa normalisasi
	 * URL, jadi perbedaan sepele (skema {@code http}/{@code https}, parameter pelacakan) melahirkan
	 * baris duplikat alih-alih memperbarui yang ada. Artikel yang tautannya kosong dilewati sama
	 * sekali oleh kedua crawler &mdash; tidak pernah tersimpan.</p>
	 *
	 * <p><b>Pemanggil.</b> {@code DetailArtikelHelper} dan {@code AmbilDataDariGoogleScholarBanyak}
	 * memakainya sebagai target tombol "Lihat Isi Artikel";
	 * {@code penelitiandanpengabdian.Artikel#getReferensi()} menyalinnya menjadi referensi
	 * publikasi; {@code FilePerkuliahanHelper} menyalinnya ke {@code PertemuanFileContent#setLink}.</p>
	 *
	 * <p><b>Catatan keamanan (di pemanggil, bukan di kelas ini).</b> Nilai kembalian dirangkai
	 * mentah ke dalam literal string JavaScript
	 * ({@code Clients.evalJavaScript("popupCenter({url: '" + scholarArticle.getLink() + "', ...")})
	 * dan dipakai langsung sebagai target {@code sendRedirect} pada tampilan mobile &mdash; pola
	 * yang persis sama dengan {@code ScholarAuthor#getKeterangan()}. Isi kolom ini berasal dari
	 * atribut {@code href} halaman HTML pihak ketiga, bukan dari input tervalidasi; tanda kutip
	 * tunggal di dalamnya akan keluar dari literal tersebut. Didokumentasikan apa adanya; tidak
	 * diubah di sini.</p>
	 *
	 * @return URL halaman artikel, atau {@code null} bila belum terisi (praktis tidak terjadi untuk
	 *         baris yang tersimpan, karena crawler menolak menyimpan artikel tanpa tautan)
	 */
	@Column(name = "link", columnDefinition = "text")
	public String getLink() {
		return link;
	}

	/**
	 * Menetapkan URL halaman artikel, tanpa validasi bentuk URL. Dipanggil kedua crawler pada setiap
	 * pemrosesan dengan nilai yang sama seperti yang barusan dipakai untuk mencari baris lama, jadi
	 * pada praktiknya tidak pernah mengubah nilai yang sudah ada.
	 *
	 * <p><b>Perhatian:</b> karena field ini adalah kunci pencocokan crawler, menggantinya membuat
	 * crawl berikutnya tidak lagi mengenali baris ini dan membuat duplikat.</p>
	 *
	 * @param link URL halaman artikel
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Riwayat kata kunci pencarian yang pernah memunculkan artikel ini &mdash; kolom {@code kewords}
	 * (ejaan salah ketik, dipertahankan apa adanya karena sudah menjadi nama kolom) bertipe
	 * {@code text}.
	 *
	 * <p><b>Bukan kata kunci artikel.</b> Isinya daftar istilah pencarian yang diketik pengguna di
	 * dialog {@code AmbilDataDariGoogleScholarBanyak} (atau nama matakuliah yang dipakai sebagai
	 * kata kunci default), disambung {@code ", "}. {@code GoogleScholarCrawler#startCrawl}
	 * menambahkan kata kunci baru hanya bila panjangnya lebih dari 3 karakter DAN belum terkandung
	 * di dalam nilai lama (perbandingan {@code contains} huruf kecil) &mdash; pemeriksaan substring,
	 * bukan per-elemen, sehingga kata kunci "data" tidak akan pernah ditambahkan bila daftar sudah
	 * memuat "basis data".</p>
	 *
	 * <p><b>Menormalkan hasil TANPA menulis balik:</b> {@code null} dikembalikan sebagai {@code ""}
	 * (dan hasilnya di-{@code trim}), tetapi field-nya tidak disentuh &mdash; berbeda dari
	 * {@link #getNama()}. Normalisasi ini bukan sekadar kenyamanan: crawler memanggil
	 * {@code tempArticle.getKewords().toLowerCase()} dan {@code getKewords().isEmpty()} secara
	 * langsung, jadi tanpa penjagaan ini setiap artikel yang belum punya kata kunci akan memicu
	 * {@code NullPointerException} di tengah crawl.</p>
	 *
	 * <p>Dipakai juga sebagai kolom pencarian di
	 * {@code AmbilDataDariGoogleScholarBanyak#initCriteria}
	 * ({@code Restrictions.ilike("kewords", katakunci, ANYWHERE)}) &mdash; itulah cara tab "artikel
	 * yang sudah diambil sebelumnya" menyaring artikel yang relevan dengan matakuliah.</p>
	 *
	 * @return daftar kata kunci pencarian tanpa spasi di ujung; {@code ""} bila belum ada, tidak
	 *         pernah {@code null}
	 */
	@Column(name = "kewords", columnDefinition = "text")
	public String getKewords() {
		return kewords == null ? "" : kewords.trim();
	}

	/**
	 * Menetapkan riwayat kata kunci pencarian. Dipanggil satu tempat saja
	 * ({@code GoogleScholarCrawler#startCrawl}) dengan nilai yang sudah dirakit di sana
	 * &mdash; nilai lama ditambah {@code ", "} ditambah kata kunci baru. Method ini sendiri
	 * <b>menimpa</b>, bukan menambahkan; logika penyambungan sepenuhnya ada di pemanggil.
	 *
	 * @param kewords daftar kata kunci pencarian yang sudah dirakit; boleh {@code null}
	 */
	public void setKewords(String kewords) {
		this.kewords = kewords;
	}

	/**
	 * Seluruh header respons HTTP yang dikembalikan {@link #getLink()} saat pertama kali dijamah
	 * crawler &mdash; kolom {@code headers} bertipe {@code text}.
	 *
	 * <p><b>Cara pengisian.</b> Kedua crawler menjalankan blok yang identik: bila
	 * {@code getHeaders() == null}, mereka membuka {@code new URL(articleLink).openConnection()}
	 * lalu menyimpan {@code ua.getHeaderFields().toString()} &mdash; representasi {@code Map} apa
	 * adanya, mis. {@code {null=[HTTP/1.1 200 OK], Content-Type=[application/pdf], ...}}. Bukan
	 * format terstruktur; tidak ada pemanggil yang menguraikannya kembali. Kegagalan koneksi
	 * ditelan {@code catch} kosong, sehingga field tetap {@code null} dan percobaan diulang pada
	 * crawl berikutnya.</p>
	 *
	 * <p><b>Yang membacanya.</b> Hanya satu tempat, dan dengan cara yang tak terduga:
	 * {@code FilePerkuliahanHelper} menyalin nilai ini ke
	 * {@code PertemuanFileContent#setGoogleBook(...)} &mdash; field yang namanya menyiratkan
	 * identitas Google Books, bukan header HTTP. Jalur itu sendiri dorman
	 * ({@code button.setVisible(false)}).</p>
	 *
	 * <p><b>Catatan yang perlu diketahui:</b> permintaan HTTP itu dikirim oleh <b>server
	 * aplikasi</b> ke URL yang berasal dari halaman pihak ketiga (Google Scholar), dan seluruh
	 * header responsnya &mdash; termasuk header seperti {@code Set-Cookie} bila ada &mdash;
	 * tersimpan permanen di kolom {@code text} ini beserta salinan Envers-nya. Tidak ada daftar
	 * putih host maupun penyaringan header. Didokumentasikan apa adanya; tidak diubah di sini.</p>
	 *
	 * @return string representasi peta header respons HTTP, atau {@code null} bila tautan belum
	 *         pernah berhasil dibuka
	 */
	@Column(name = "headers", columnDefinition = "text")
	public String getHeaders() {
		return headers;
	}

	/**
	 * Menetapkan string header respons HTTP. Dipanggil kedua crawler dengan hasil
	 * {@code Map#toString()} atas {@code URLConnection#getHeaderFields()}. Tidak ada validasi
	 * panjang maupun isi.
	 *
	 * @param headers representasi teks peta header respons HTTP
	 */
	public void setHeaders(String headers) {
		this.headers = headers;
	}

	/**
	 * URL berkas artikel (umumnya PDF) yang ditawarkan Google Scholar &mdash; kolom
	 * {@code link_file} bertipe {@code text}.
	 *
	 * <p><b>Hanya jalur per-dosen yang mengisinya</b> ({@code div.gsc_vcd_title_ggi&gt;a} pada
	 * halaman detail Scholar); jalur pencarian kata kunci tidak pernah menyentuh field ini,
	 * sehingga baris hasil pencarian selalu bernilai {@code null} di sini. Karena Jsoup
	 * mengembalikan <b>string kosong</b> (bukan {@code null}) untuk atribut yang tidak ditemukan,
	 * artikel tanpa tautan berkas tersimpan sebagai {@code ""} &mdash; itulah sebabnya pemanggil
	 * memeriksa {@code != null && !trim().isEmpty()}, bukan sekadar {@code != null}.</p>
	 *
	 * <p><b>Pemanggil.</b> Satu tempat: {@code DetailArtikelHelper} membuat komponen {@code A}
	 * dengan {@code href} dan label sama-sama berisi nilai ini, dibuka di tab baru. Tautannya
	 * mengarah langsung ke server pihak ketiga, bukan ke lampiran yang tersimpan di AIS.</p>
	 *
	 * @return URL berkas artikel, string kosong bila halaman detail tidak menawarkan berkas, atau
	 *         {@code null} untuk artikel yang tidak pernah melewati jalur sinkronisasi per dosen
	 */
	@Column(name = "link_file", columnDefinition = "text")
	public String getLinkFile() {
		return linkFile;
	}

	/**
	 * Menetapkan URL berkas artikel, tanpa validasi bentuk URL. Dipanggil satu tempat saja
	 * ({@code GoogleScholarCrawlerByUser#byUser}) pada setiap sinkronisasi &mdash; termasuk dengan
	 * nilai string kosong bila halaman detail tidak memuat tautan berkas, sehingga nilai lama yang
	 * valid bisa tertimpa {@code ""} bila Scholar mengubah tata letak halamannya.
	 *
	 * @param linkFile URL berkas artikel; boleh {@code null} atau string kosong
	 */
	public void setLinkFile(String linkFile) {
		this.linkFile = linkFile;
	}

}
