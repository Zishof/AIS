package ais.database.model;

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

import org.hibernate.envers.Audited;

/**
 * Entity satu <b>artikel/publikasi ilmiah milik seorang dosen yang diambil (di-<i>crawl</i>) dari
 * SINTA</b> &mdash; <i>Science and Technology Index</i>, basis data indeksasi publikasi ilmiah
 * nasional milik Kemdikbudristek/DIKTI. Tabel: {@code public.sinta_article}.
 *
 * <p>Baris pada tabel ini <b>bukan data yang diketik pengguna</b>: seluruhnya lahir dari proses
 * sinkronisasi otomatis yang men-<i>scrape</i> halaman HTML publik situs SINTA. Rantainya:</p>
 * <ol>
 * <li>{@link ais.common.sinta.SintaCrawler#populateData} mengunduh halaman detail penulis SINTA
 * (berdasarkan {@link Dosen#getKodeSinta()}) dengan Jsoup dan mem-parsing tiap blok publikasi
 * menjadi {@code JSONObject} berisi kunci {@code judul}, {@code link}, {@code author},
 * {@code jurnal}, {@code page}, {@code vol}, {@code issue}, dan {@code tahun}.</li>
 * <li>{@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel} mencari baris
 * {@code SintaArticle} yang sudah ada dengan kombinasi <b>dosen + link + judul</b>
 * ({@code ilike}, case-insensitive) &mdash; membuat baris baru bila belum ada &mdash; lalu
 * memetakan tiap kunci JSON ke setter di kelas ini <b>satu per satu dalam blok
 * {@code try/catch} terpisah</b>, sehingga kegagalan parsing satu field tidak menggagalkan
 * field lain maupun penyimpanan barisnya.</li>
 * <li>Dari baris {@code SintaArticle} yang tersimpan, dibuat/diperbarui catatan
 * {@link ais.database.model.penelitiandanpengabdian.Artikel} pada modul
 * penelitian-dan-pengabdian AIS (lewat FK {@code artikel.sinta_article}), lengkap dengan
 * {@code JurnalPenelitian} yang dibuat otomatis bila nama jurnal belum dikenal.</li>
 * </ol>
 *
 * <p>Dengan kata lain kelas ini berperan sebagai <b>lapisan data mentah/staging</b> hasil impor
 * SINTA, sedangkan {@code Artikel} adalah representasi "resmi" publikasi di dalam AIS yang
 * dipakai untuk pelaporan (BKD, akreditasi, dsb.). Satu {@code Artikel} menunjuk paling banyak
 * satu {@code SintaArticle}.</p>
 *
 * <h2>Padanan Google Scholar</h2>
 * <p>{@link ScholarArticle} (bersama {@link ScholarAuthor}) adalah <b>kembaran struktural</b>
 * kelas ini untuk sumber Google Scholar: pola field hampir identik ({@code nama}/{@code keterangan}/
 * {@code link} + blok audit yang sama), dan {@code Artikel} memiliki DUA FK sejajar
 * ({@code sinta_article} dan {@code scholar_article}) sehingga satu catatan artikel bisa berasal
 * dari salah satu sumber. {@link ais.action.master.helper.DetailArtikelHelper} merender panel
 * detail artikel dengan percabangan {@code if (getSintaArticle() != null) ... else if
 * (getScholarArticle() != null) ...}. Perbedaan isi: {@code SintaArticle} menyimpan metadata
 * bibliografis terstruktur (jurnal/volume/issue/halaman/tahun), sedangkan {@code ScholarArticle}
 * menyimpan kata kunci dan tautan berkas.</p>
 *
 * <h2>Hal-hal non-obvious</h2>
 * <ul>
 * <li><b>{@code keterangan} berisi dump JSON mentah.</b> {@code SintaPtCrawler} memanggil
 * {@code setKeterangan(jsonObject.toString())} &mdash; jadi kolom ini bukan catatan bebas
 * pengguna melainkan salinan utuh objek JSON hasil <i>scraping</i>, berguna sebagai jejak
 * audit/forensik saat parsing per-field gagal. Jangan asumsikan isinya teks yang layak
 * ditampilkan apa adanya.</li>
 * <li><b>Kontrak {@code null} tidak seragam.</b> {@link #getVol()}, {@link #getJurnal()}, dan
 * {@link #getPage()} mengembalikan string kosong sebagai pengganti {@code null} (dan
 * di-{@code trim}), sementara {@link #getIssue()}, {@link #getAuthor()},
 * {@link #getKeterangan()}, dan {@link #getLink()} mengembalikan nilai apa adanya termasuk
 * {@code null}. Pemanggil di {@code DetailArtikelHelper} merangkai keempatnya menjadi satu
 * label tanpa penjagaan {@code null}, sehingga label bisa memuat teks literal "null".</li>
 * <li><b>{@link #getKeterangan()} membalik jaminan base class.</b> Javadoc
 * {@link GeneralValueObject#getKeterangan()} menjanjikan hasil non-{@code null}; override di
 * sini bisa mengembalikan {@code null}. Perilaku sama sudah tercatat pada {@code Bank}.</li>
 * <li><b>Fitur sinkronisasi SINTA saat ini DORMAN di UI.</b> Kedua tombol pemicunya
 * ("Singkronkan dg SINTA" di {@code DosenAction} dan di {@code DetailArtikelHelper}) dibuat dan
 * diberi {@code EventListener}, tetapi baris yang memasangnya ke toolbar
 * ({@code add.getParent().appendChild(singkron)} / {@code toolbar.appendChild(singkron)})
 * DIKOMENTARI, sehingga tombolnya tidak pernah punya induk dan tidak pernah tampil. Baris
 * {@code sinta_article} yang sudah ada tetap dibaca dan dirender oleh
 * {@code DetailArtikelHelper}; yang mati hanyalah jalur pengisian barunya.</li>
 * <li><b>Beberapa field tidak beranotasi {@code @Column}.</b> {@code vol}, {@code issue},
 * {@code tahun}, dan {@code page} mengandalkan penamaan kolom default Hibernate (sama dengan
 * nama properti). Perubahan nama properti otomatis mengubah nama kolom yang diharapkan.</li>
 * </ul>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 * hanya POJO abstrak biasa, sehingga Hibernate TIDAK memetakan properti induknya. Karena itu
 * deklarasi ULANG {@code id}, {@code nama}, {@code keterangan}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} di kelas ini <b>BUKAN bug melainkan keharusan teknis</b>: tanpa
 * deklarasi ulang, kolom-kolom tersebut tidak akan terpetakan sama sekali. Konsekuensinya field
 * bernama sama di {@code GeneralValueObject} (yang {@code private}) tetap ada namun selalu
 * kosong untuk instance kelas ini &mdash; kode yang memakai jalur akses milik base class (bukan
 * getter yang di-override di sini) akan membaca {@code null}.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Audit &amp; identitas</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 * {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 * <li><b>Metadata bibliografis</b> &mdash; {@link #getNama()} (judul artikel),
 * {@link #getJurnal()}, {@link #getVol()}, {@link #getIssue()}, {@link #getPage()},
 * {@link #getTahun()}, {@link #getAuthor()}, {@link #getLink()}, beserta setter-nya.</li>
 * <li><b>Relasi &amp; data mentah</b> &mdash; {@link #getDosen()}/{@link #setDosen(Dosen)} dan
 * {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * </ol>
 *
 * <h2>Verifikasi pola berulang paket ini</h2>
 * <ul>
 * <li><b>Getter yang menulis balik ke field:</b> ADA satu &mdash; {@link #getDosen()}
 * ({@code dosen = check(dosen)}), tetapi hanya menormalkan proxy lazy di MEMORI, tidak
 * menyentuh basis data. Berbeda dari kembarannya {@link ScholarArticle#getNama()} yang
 * benar-benar menulis balik hasil pembersihan penanda ({@code [PDF]}, {@code [BUKU]}, ...) ke
 * field &mdash; {@link #getNama()} di kelas ini TIDAK melakukan itu (hanya {@code trim} pada
 * nilai kembalian).</li>
 * <li><b>Getter yang menutup sesi Hibernate:</b> TIDAK ADA di kelas ini. Penutupan sesi
 * ({@code HibernateUtil.closeSession()}) terjadi di pemanggil ({@code SintaPtCrawler},
 * {@code DetailArtikelHelper}), bukan di entity.</li>
 * <li><b>Getter destruktif</b> (menghapus/mengosongkan data permanen): TIDAK ADA.</li>
 * <li><b>Setter yang menolak nilai kosong:</b> ADA &mdash; {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} mengabaikan {@code null}/string kosong, jadi field audit tidak
 * pernah bisa dikosongkan kembali setelah terisi.</li>
 * </ul>
 *
 * <p><b>Anotasi kelas.</b> {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menyertakan kolom yang benar-benar berubah pada INSERT/UPDATE &mdash; penting untuk entity ini
 * karena sinkronisasi sering hanya memperbarui sebagian field. {@code @Audited} (Hibernate Envers)
 * menyalin setiap versi baris ke tabel revisi; riwayatnya bisa ditelusuri pengguna lewat
 * {@code RevisiHelper.createNewRevisi(SintaArticle.class, ...)} pada panel detail artikel.
 * {@code SintaArticle} juga terdaftar pada {@code DspaceInformation.linksForClass}, sehingga
 * artikel dapat dipetakan ke <i>handle</i> repositori DSpace institusi.</p>
 *
 * <p><b>Catatan:</b> komentar generator "Bank generated by hbm2java" di bawah adalah sisa
 * salin-tempel dari {@link Bank} (sumber asli komentar itu) dan tidak menggambarkan kelas ini.</p>
 *
 * @see ScholarArticle
 * @see ais.database.model.penelitiandanpengabdian.Artikel
 * @see ais.common.sinta.SintaCrawler
 * @see ais.common.sinta.SintaPtCrawler
 * @see Dosen#getKodeSinta()
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "sinta_article")

public class SintaArticle extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, diwarisi lewat {@link java.io.Serializable} pada
	 * {@link GeneralValueObject}. Nilainya tetap agar objek yang pernah di-serialisasi (mis.
	 * tersimpan di sesi ZK) tetap kompatibel.
	 *
	 * <p><b>Kuirk:</b> nilai konstanta ini identik dengan yang ada di {@link Pesan} dan
	 * {@link DspaceInformation} &mdash; artefak salin-tempel yang sama seperti komentar generator
	 * "Bank generated by hbm2java" (lihat javadoc kelas). Tidak berdampak fungsional karena
	 * {@code serialVersionUID} hanya dibandingkan antar-versi kelas YANG SAMA.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key ({@code public.sinta_article.id}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi otomatis, lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila baris belum
	 *         pernah di-update sejak dibuat
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
	 * pada kolom-kolom ini &mdash; untuk entity ini praktis semua baris dibuat oleh proses
	 * sinkronisasi SINTA, bukan oleh pengguna.
	 *
	 * <p>Pada baris deklarasi yang sama juga dideklarasikan field {@code tanggal_dirubah}, yang
	 * diinisialisasi ke waktu server saat objek dibuat ({@code ais.ui.util.WaktuUtil.getDate()})
	 * sehingga baris baru tetap punya stempel waktu meski belum pernah di-update.</p>
	 *
	 * <p>Karena tidak ada getter di kelas ini yang menulis balik ke field terpetakan (lihat
	 * javadoc kelas), callback ini hanya terpicu oleh perubahan yang benar-benar diniatkan &mdash;
	 * sekadar merender panel detail artikel tidak menghasilkan UPDATE.</p>
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
	 * Representasi teks singkat berformat {@code "<id>-<judul>"}, dipakai untuk log/debug dan
	 * sebagai label default pada komponen ZK yang menampilkan entity ini.
	 *
	 * <p><b>Catatan:</b> membaca field {@code nama} secara LANGSUNG, bukan lewat
	 * {@link #getNama()}, sehingga judul di sini TIDAK di-{@code trim}. Untuk baris yang belum
	 * disimpan {@code id} masih {@code null} sehingga hasilnya diawali "null-".</p>
	 *
	 * @return gabungan id dan judul artikel dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Judul artikel hasil crawl ({@code jsonObject.getString("judul")}). Dideklarasikan ulang
	 * karena properti {@code nama} milik {@link GeneralValueObject} tidak terpetakan Hibernate.
	 */
	private String nama;
	/**
	 * Dump JSON mentah seluruh objek hasil <i>scraping</i> SINTA untuk artikel ini
	 * ({@code jsonObject.toString()}) &mdash; bukan catatan bebas pengguna. Lihat javadoc kelas.
	 */
	private String keterangan;
	/** Tautan ke halaman artikel pada situs SINTA/penerbit; dipakai juga sebagai kunci pencocokan. */
	private String link;
	/** Volume jurnal, sebagaimana ter-parse dari teks {@code indexedby} halaman SINTA. */
	private String vol;
	/** Nomor issue/edisi jurnal, sebagaimana ter-parse dari teks {@code indexedby} halaman SINTA. */
	private String issue;
	/** Nama jurnal/prosiding tempat artikel terbit; dipakai menurunkan {@code JurnalPenelitian}. */
	private String jurnal;
	/** Daftar penulis sebagai satu string bebas apa adanya dari SINTA (tidak dipecah per orang). */
	private String author;
	/** Rentang/nomor halaman artikel dalam jurnal, mis. {@code "12-20"}. */
	private String page;
	/** Tahun terbit; hasil {@code Integer.parseInt} atas teks tahun dari SINTA (gagal parse = tetap {@code null}). */
	private Integer tahun;
	/** Dosen pemilik publikasi &mdash; FK {@code dosen}, wajib terisi. Lihat {@link #getDosen()}. */
	private Dosen dosen;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Dipakai juga oleh
	 * {@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel} saat artikel hasil crawl belum
	 * ada padanannya di basis data. Seluruh field dibiarkan kosong kecuali
	 * {@code tanggal_dirubah}, yang langsung diisi waktu server.
	 */
	public SintaArticle() {
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
	 * Judul artikel. Override properti {@code nama} milik {@link GeneralValueObject} agar
	 * terpetakan ke kolom {@code nama} (bertipe {@code text}, {@code NOT NULL}).
	 *
	 * <p>Dipakai sebagai salah satu kunci pencocokan artikel lama vs hasil crawl baru
	 * ({@code Restrictions.ilike("nama", judul)}) di
	 * {@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel}, dan sebagai label riwayat revisi
	 * pada panel detail artikel.</p>
	 *
	 * <p>Berbeda dari {@link ScholarArticle#getNama()} yang membersihkan penanda seperti
	 * {@code [PDF]}/{@code [BUKU]} dan MENULIS BALIK hasilnya ke field, getter ini murni membaca:
	 * hanya melakukan {@code trim} pada nilai yang dikembalikan tanpa mengubah state objek.</p>
	 *
	 * @return judul artikel tanpa spasi di ujung, atau {@code null} bila field belum terisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan judul artikel. Dipanggil oleh sinkronisasi SINTA dengan nilai kunci
	 * {@code "judul"} dari data hasil crawl. Tidak ada normalisasi/validasi apa pun di sini
	 * meskipun kolomnya {@code NOT NULL}.
	 *
	 * @param nama judul artikel
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Isi kolom {@code keterangan}: pada praktiknya <b>dump JSON mentah</b> hasil <i>scraping</i>
	 * SINTA untuk artikel ini, bukan keterangan yang ditulis pengguna (lihat javadoc kelas).
	 * Berguna sebagai jejak forensik saat parsing per-field gagal.
	 *
	 * <p><b>Menyimpang dari kontrak base class:</b> {@link GeneralValueObject#getKeterangan()}
	 * menjanjikan hasil non-{@code null}, sedangkan override ini mengembalikan nilai apa adanya
	 * termasuk {@code null}.</p>
	 *
	 * @return string JSON mentah data SINTA artikel ini, atau {@code null} bila belum pernah diisi
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan isi kolom {@code keterangan}. Satu-satunya pemanggil di alur aplikasi adalah
	 * {@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel}, yang mengisinya dengan
	 * {@code jsonObject.toString()} &mdash; seluruh objek JSON hasil crawl.
	 *
	 * @param keterangan isi keterangan (dalam praktik: JSON mentah data SINTA)
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Tautan ke halaman artikel (halaman perantara SINTA atau situs penerbit) hasil atribut
	 * {@code href} pada judul artikel di halaman SINTA.
	 *
	 * <p>Selain ditampilkan, nilai ini dipakai sebagai kunci pencocokan
	 * ({@code Restrictions.ilike("link", ...)}) saat sinkronisasi memutuskan membuat baris baru
	 * atau memperbarui yang lama, dan dibuka langsung oleh pengguna dari panel detail artikel
	 * ({@code sendRedirect} pada perangkat mobile, {@code popupCenter} pada desktop).</p>
	 *
	 * @return tautan artikel apa adanya, bisa {@code null} bila parsing gagal
	 */
	@Column(name = "link", columnDefinition = "text")
	public String getLink() {
		return link;
	}

	/**
	 * Menetapkan tautan artikel. Dipanggil sinkronisasi SINTA dengan kunci {@code "link"} hasil
	 * crawl; tidak ada validasi format URL.
	 *
	 * @param link tautan artikel
	 */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Tahun terbit artikel. Kolom tanpa anotasi {@code @Column} sehingga memakai penamaan default
	 * Hibernate ({@code tahun}).
	 *
	 * @return tahun terbit, atau {@code null} bila teks tahun dari SINTA tidak ada atau gagal
	 *         di-{@code parseInt} (kegagalan itu ditelan {@code try/catch} per-field di
	 *         {@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel})
	 */
	public Integer getTahun() {
		return tahun;
	}

	/**
	 * Menetapkan tahun terbit artikel.
	 *
	 * @param tahun tahun terbit; boleh {@code null}
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Dosen pemilik publikasi ini &mdash; FK {@code dosen} yang wajib terisi
	 * ({@code nullable = false}), diambil {@code LAZY}.
	 *
	 * <p><b>Efek samping (menulis balik ke field):</b> getter ini menugaskan ulang
	 * {@code dosen = check(dosen)}. {@link GeneralValueObject#check(Object)} meresolusi proxy
	 * Hibernate yang belum ter-inisialisasi (lewat {@code EntityIdentityMap}, cache, sesi aktif,
	 * atau muat ulang) dan dapat mengembalikan <b>instance yang berbeda</b> dari argumennya.
	 * Penulisan ini murni di MEMORI (menormalkan referensi objek), <b>tidak</b> melakukan UPDATE
	 * basis data dan tidak mengubah nilai FK yang tersimpan.</p>
	 *
	 * <p>Dipakai antara lain oleh {@link ais.action.master.helper.DetailArtikelHelper} untuk
	 * menampilkan foto, nama, dan NIDN dosen pada panel detail artikel, serta sebagai kunci
	 * pencarian artikel milik dosen tertentu saat sinkronisasi.</p>
	 *
	 * @return dosen pemilik artikel, sudah diresolusi dari proxy lazy bila perlu
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen", nullable = false)
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/**
	 * Menetapkan dosen pemilik artikel. Wajib diisi sebelum penyimpanan karena kolom FK-nya
	 * {@code NOT NULL}; sinkronisasi SINTA selalu mengisinya dengan dosen yang sedang diproses.
	 *
	 * @param dosen dosen pemilik artikel
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * Volume jurnal. Kolom tanpa anotasi {@code @Column} (penamaan default {@code vol}).
	 *
	 * @return volume yang sudah di-{@code trim}, atau string kosong bila field {@code null}
	 *         &mdash; getter ini TIDAK pernah mengembalikan {@code null}
	 */
	public String getVol() {
		return vol == null ? "" : vol.trim();
	}

	/**
	 * Menetapkan volume jurnal. Diisi sinkronisasi SINTA dari potongan teks {@code indexedby}
	 * setelah tanda {@code ":"}.
	 *
	 * @param vol volume jurnal
	 */
	public void setVol(String vol) {
		this.vol = vol;
	}

	/**
	 * Nomor issue/edisi jurnal. Kolom tanpa anotasi {@code @Column} (penamaan default
	 * {@code issue}).
	 *
	 * <p><b>Tidak konsisten</b> dengan {@link #getVol()}/{@link #getPage()}: getter ini
	 * mengembalikan nilai apa adanya termasuk {@code null} dan tanpa {@code trim}.</p>
	 *
	 * @return nomor issue, bisa {@code null}
	 */
	public String getIssue() {
		return issue;
	}

	/**
	 * Menetapkan nomor issue/edisi jurnal. Diisi sinkronisasi SINTA dari potongan teks
	 * {@code indexedby} setelah tanda {@code ":"}.
	 *
	 * @param issue nomor issue jurnal
	 */
	public void setIssue(String issue) {
		this.issue = issue;
	}

	/**
	 * Nama jurnal/prosiding tempat artikel terbit.
	 *
	 * <p>Nilai ini menentukan {@code JurnalPenelitian} yang ditautkan ke {@code Artikel} turunan:
	 * {@link ais.common.sinta.SintaPtCrawler#singkronkanArtikel} memakai fallback
	 * {@code "Jurnal Default"} bila hasil getter ini kosong, lalu menurunkan {@code path} jurnal
	 * (huruf kecil, spasi diganti garis bawah) dan membuat entity {@code JurnalPenelitian} baru
	 * secara otomatis bila {@code path} itu belum dikenal. Jadi variasi penulisan nama jurnal dari
	 * SINTA bisa memunculkan baris jurnal duplikat.</p>
	 *
	 * @return nama jurnal yang sudah di-{@code trim}, atau string kosong bila {@code null}
	 *         &mdash; tidak pernah {@code null}
	 */
	@Column(name = "jurnal", columnDefinition = "text")
	public String getJurnal() {
		return jurnal == null ? "" : jurnal.trim();
	}

	/**
	 * Menetapkan nama jurnal. Diisi sinkronisasi SINTA dari bagian pertama teks {@code indexedby}
	 * sebelum tanda koma.
	 *
	 * @param jurnal nama jurnal/prosiding
	 */
	public void setJurnal(String jurnal) {
		this.jurnal = jurnal;
	}

	/**
	 * Daftar penulis artikel sebagai satu string bebas persis seperti yang tertulis di SINTA
	 * (baris pertama blok {@code dd} pada halaman); tidak dipecah menjadi entity penulis
	 * tersendiri &mdash; berbeda dari {@link ScholarArticle} yang punya relasi
	 * {@code Set<ScholarAuthor>}.
	 *
	 * <p><b>Perhatian:</b> nilainya berasal dari {@code Element.html()}, jadi bisa memuat markup
	 * HTML mentah dari situs eksternal, dan dikembalikan apa adanya (bisa {@code null}, tanpa
	 * {@code trim} maupun sanitasi).</p>
	 *
	 * @return string daftar penulis, bisa {@code null}
	 */
	@Column(name = "author", columnDefinition = "text")
	public String getAuthor() {
		return author;
	}

	/**
	 * Menetapkan daftar penulis. Diisi sinkronisasi SINTA dengan kunci {@code "author"} hasil
	 * crawl.
	 *
	 * @param author string daftar penulis
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Rentang/nomor halaman artikel dalam jurnal. Kolom tanpa anotasi {@code @Column} (penamaan
	 * default {@code page}).
	 *
	 * @return halaman yang sudah di-{@code trim}, atau string kosong bila {@code null} &mdash;
	 *         tidak pernah {@code null}
	 */
	public String getPage() {
		return page == null ? "" : page.trim();
	}

	/**
	 * Menetapkan rentang/nomor halaman. Diisi sinkronisasi SINTA dari potongan teks
	 * {@code indexedby} setelah tanda koma pertama.
	 *
	 * @param page rentang/nomor halaman artikel
	 */
	public void setPage(String page) {
		this.page = page;
	}

}
