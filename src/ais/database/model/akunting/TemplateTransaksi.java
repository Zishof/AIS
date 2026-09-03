package ais.database.model.akunting;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

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
import ais.database.model.Pegawai;
import ais.database.model.rab.Workspace;

/**
 * Cetakan (<i>template</i>) satu baris jurnal debit/kredit — kembaran struktural
 * {@link ais.database.model.akunting.Transaksi} yang dimaksudkan sebagai pola siap pakai
 * untuk jurnal berulang, tetapi <b>tidak pernah selesai dihubungkan ke aplikasi</b>.
 *
 * <h2>Peran yang dirancang</h2>
 *
 * <p>Struktur paket akunting AIS memakai pasangan induk–anak: satu
 * {@link ais.database.model.akunting.GrupTransaksi} (kepala bukti jurnal) memayungi banyak
 * {@link ais.database.model.akunting.Transaksi} (baris debit/kredit). Kelas ini beserta
 * pasangannya {@link ais.database.model.akunting.TemplateGrupTransaksi} adalah salinan
 * pasangan tersebut ke tabel terpisah (<code>akunting.template_transaksi</code> dan
 * <code>akunting.template_grup_transaksi</code>): sebuah bukti jurnal yang disimpan sebagai
 * pola, agar transaksi yang berulang tiap periode tinggal disalin menjadi jurnal sungguhan
 * alih-alih diketik ulang.</p>
 *
 * <p>Karena itu setiap baris kelas ini menunjuk induknya lewat
 * {@link #getTemplateGrupTransaksi()} (bukan <code>GrupTransaksi</code>), dan membawa
 * seluruh dimensi jurnal yang sama: {@link #getAkun() perkiraan},
 * {@link #getSubAkun() sub-perkiraan}, {@link #getDebet() debet}/{@link #getKredit() kredit},
 * {@link #getDevisi() divisi}, {@link #getPegawai() pegawai},
 * {@link #getMatauang() mata uang} berikut kurs, {@link #getJenisTransaksi() jenis jurnal},
 * serta {@link #getPostingHistory() cap posting}.</p>
 *
 * <h2>Status sesungguhnya: entity TIDUR / YATIM (terverifikasi)</h2>
 *
 * <p><b>Tidak ada satu pun pemanggil kelas ini di seluruh repositori.</b> Penelusuran
 * menyeluruh atas berkas <code>.java</code>, <code>.zul</code>, <code>.jsp</code>,
 * <code>.xml</code>, dan <code>.properties</code> hanya menemukan:</p>
 *
 * <ul>
 *   <li>berkas ini sendiri;</li>
 *   <li>dua rujukan <i>Javadoc</i> saja (bukan kode) di
 *       {@link ais.database.model.akunting.Transaksi} dan
 *       {@link ais.database.model.akunting.JenisTransaksi};</li>
 *   <li>satu baris <code>&lt;mapping class=…&gt;</code> di <code>hibernate.cfg.xml</code>;</li>
 *   <li>satu baris inventaris di manifes <code>generic-crud</code>
 *       (<code>general_value_object_inventory.csv</code>), berstatus
 *       <code>REVIEW_REQUIRED</code> — jadi belum diaktifkan sebagai layar CRUD generik.</li>
 * </ul>
 *
 * <p>Konsekuensinya: <b>tidak ada Action, Helper, DAO, layar ZK/JSP, HQL, maupun Criteria
 * yang menulis atau membaca tabel ini</b>. Tidak ada pula method "bangkitkan jurnal dari
 * template" di kelas ini maupun di {@link ais.database.model.akunting.TemplateGrupTransaksi}
 * — kelas ini murni kumpulan <i>accessor</i> tanpa satu pun method bisnis. Tabelnya tetap
 * dibuat (karena terdaftar di <code>hibernate.cfg.xml</code>) tetapi secara praktis selalu
 * kosong. Pola ini sama dengan entity tidur lain yang sudah terdokumentasi di proyek ini,
 * mis. <code>ais.database.model.sekolah.PengumumanJadwalPelajaran</code>.</p>
 *
 * <p><b>Relasi ke pasangan indukya bersifat satu arah.</b>
 * {@link ais.database.model.akunting.TemplateGrupTransaksi} <i>tidak</i> memiliki koleksi
 * <code>OneToMany</code> balik ke kelas ini — namanya bahkan tidak disebut sama sekali di
 * berkas itu. Jadi tidak ada jalan navigasi header &rarr; baris; satu-satunya arah adalah
 * baris &rarr; header lewat {@link #getTemplateGrupTransaksi()}.</p>
 *
 * <p><b>Fitur jurnal berulang yang benar-benar hidup adalah entity lain.</b> Kebutuhan
 * "template jurnal berkala" dipenuhi oleh
 * {@link ais.database.model.akunting.TemplateJurnalPenyesuaian} (amortisasi, akrual,
 * penyisihan) yang berdiri sendiri, jauh lebih ramping (akun debet + akun kredit + nilai +
 * frekuensi), dan punya penjaga anti-posting-ganda per periode. Kelas ini merupakan
 * peninggalan rancangan lama tahun 2010 yang ditinggalkan.</p>
 *
 * <h2>Perbandingan struktur dengan {@code Transaksi} (terverifikasi baris demi baris)</h2>
 *
 * <p>Kedua berkas lahir dari satu cetakan salin-tempel: <code>serialVersionUID</code>-nya
 * <b>identik</b> (<code>2463821577548439808L</code>), dan nilai yang sama juga dipakai
 * {@link ais.database.model.akunting.GrupTransaksi} serta
 * {@link ais.database.model.akunting.TemplateGrupTransaksi}. Seluruh konstanta
 * <code>STATUS_POSTING_*</code>, <code>STATUS_PEMERIKSAAN_*</code>, dan
 * <code>JURNAL_*</code> digandakan apa adanya. Susunan field, urutan getter/setter, dan
 * bahkan penulisan setter satu baris <code>oleh</code>/<code>olehId</code> sama persis.
 * Yang <b>berbeda</b> hanya empat hal:</p>
 *
 * <ol>
 *   <li><b>Induk.</b> <code>Transaksi</code> menunjuk <code>GrupTransaksi</code> lewat
 *       <code>getGrupTransaksi()</code>; kelas ini menunjuk
 *       {@link ais.database.model.akunting.TemplateGrupTransaksi} lewat
 *       {@link #getTemplateGrupTransaksi()} — nama <i>property</i> berbeda meski nama field
 *       dan nama kolomnya tetap <code>grupTransaksi</code>/<code>grup_transaksi</code>
 *       (lihat kuirk pemetaan di bawah).</li>
 *   <li><b>Tidak ada mekanisme akun pengganti.</b> <code>Transaksi</code> punya field
 *       <code>akunOver</code> beserta <code>getAkunOver()</code>/<code>setAkunOver()</code>,
 *       dan {@code Transaksi.getAkun()} <b>menimpa</b> <code>akun</code> dengan
 *       <code>akunOver</code> secara permanen saat dibaca (getter destruktif terberat pada
 *       entity kembarannya). Kelas ini <b>tidak memilikinya</b>: {@link #getAkun()} hanya
 *       me-resolve proxy lazy lewat <code>check(...)</code>. Jadi kuirk paling berbahaya
 *       pada <code>Transaksi</code> justru <i>tidak</i> menular ke sini.</li>
 *   <li><b>{@code defaultWorkspace} masih hidup di sini, sudah mati di sana.</b> Kelas ini
 *       memetakan {@link #getDefaultWorkspace()} ke kolom <code>default_workspace</code>,
 *       sedangkan di <code>Transaksi</code> deklarasi yang sama sudah
 *       <b>dikomentari</b>. Ini bukti arah silsilahnya: <code>Transaksi</code> terus
 *       dirawat, sedangkan kelas ini membeku pada keadaan lama. Perhatikan juga bahwa
 *       <code>GrupTransaksi</code> memiliki <code>workspace</code> dan
 *       <code>satuanKerja</code> di tingkat <i>header</i>, sementara di keluarga template
 *       dimensi <i>workspace</i> justru menempel di tingkat <i>baris</i> (di sini) dan
 *       hilang dari header-nya.</li>
 *   <li><b>Kolom pajak dikomentari.</b> Blok <code>akunPajak</code>/<code>getAkunPajak()</code>
 *       ada sebagai komentar mati — sisa rancangan yang tidak jadi dipakai.</li>
 * </ol>
 *
 * <p>Sebaliknya, <code>Transaksi</code> membawa muatan yang jauh lebih besar di luar
 * berkasnya (dirujuk puluhan Action, Helper, dan laporan), sementara kelas ini tidak
 * dirujuk siapa pun.</p>
 *
 * <h2>Hal-hal non-obvious pada kelas ini</h2>
 *
 * <ul>
 *   <li><b>Kuirk pemetaan — <code>&#64;JoinColumn</code> pada properti skalar.</b>
 *       {@link #getRandomValue()} bertipe {@link Long} biasa namun dianotasi
 *       <code>&#64;JoinColumn(name = "random_value")</code>, bukan <code>&#64;Column</code>.
 *       Anotasi relasi pada properti skalar bukan pemetaan yang sah, sehingga nama kolom
 *       yang diminta belum tentu dihormati Hibernate. Pola yang persis sama ada di
 *       {@link ais.database.model.akunting.Transaksi#getRandomValue()}.</li>
 *   <li><b>Kuirk pemetaan — nama properti tidak sama dengan nama field.</b> Karena
 *       <code>&#64;Id</code> dipasang pada {@link #getId()}, Hibernate memakai
 *       <i>property access</i>. Nama properti persisten untuk relasi induk karena itu
 *       adalah <code>templateGrupTransaksi</code> (dari nama getter), <b>bukan</b>
 *       <code>grupTransaksi</code> seperti nama fieldnya. Setiap HQL/Criteria yang kelak
 *       ditulis untuk entity ini harus memakai <code>templateGrupTransaksi</code>; memakai
 *       <code>grupTransaksi</code> — nama yang benar pada
 *       {@link ais.database.model.akunting.Transaksi} — akan gagal. Jebakan yang mudah
 *       terpicu justru karena kedua kelas mirip.</li>
 *   <li><b>Getter destruktif (menulis balik ke field).</b> Karena
 *       <code>dynamicUpdate = true</code> dan entity berada dalam sesi Hibernate hidup,
 *       penulisan balik ini bisa ikut ter-<i>flush</i> menjadi <code>UPDATE</code> hanya
 *       karena baris dibaca: {@link #getTanggalTransaksi()} (menimpa dari induk),
 *       {@link #getBulan()} dan {@link #getTahun()} (mengisi dari jam sistem <i>sekarang</i>,
 *       bukan dari tanggal transaksi), {@link #getStatusPosting()} (menghitung ulang dari
 *       ada-tidaknya cap posting), {@link #getTanggalPosting()} (hanya bisa
 *       <i>mengosongkan</i>), {@link #getJenisTransaksi()} (mewarisi dari induk),
 *       {@link #getDebet()}/{@link #getKredit()} (menormalkan <code>null</code> jadi
 *       <code>0.0</code>), {@link #getAkun()}/{@link #getDefaultWorkspace()} (resolusi
 *       proxy), dan {@link #getRandomValue()} (membangkitkan nilai acak baru + mencetak ke
 *       <code>System.out</code>).</li>
 *   <li><b>Setter audit menolak nilai kosong secara diam-diam.</b>
 *       {@link #setOleh(String)} dan {@link #setOlehId(String)} langsung
 *       <code>return</code> untuk <code>null</code>/string kosong, sehingga jejak audit
 *       tidak dapat dikosongkan dan pemanggil tidak diberi umpan balik.</li>
 *   <li><b>Cakupan tenant: tidak ada sama sekali.</b> Entity ini tidak memiliki kolom
 *       <code>sekolah</code>, <code>yayasan</code>, maupun <code>satuanKerja</code>.
 *       Satu-satunya pembatas ruang lingkup adalah {@link #getDefaultWorkspace()}, yang
 *       <b>tidak pernah disetel dari mana pun</b> di repositori — jadi de facto selalu
 *       <code>null</code>. Selama entity ini tidur hal itu tidak berdampak; begitu
 *       dihidupkan, ia akan menjadi katalog global lintas tenant kecuali penyaringnya
 *       ditambahkan lebih dahulu (pola fail-open cakupan tenant yang berulang di modul
 *       lain).</li>
 *   <li><b>Tidak ada gerbang hak akses.</b> Karena tidak ada Action/Helper, tidak ada pula
 *       <code>checkPrevilages()</code> yang menjaga siapa boleh membuat/mengubah template.
 *       Ini penting bila fitur ini kelak dihidupkan: template jurnal yang dieksekusi
 *       otomatis adalah sasaran empuk untuk menyuntikkan jurnal palsu <i>berulang</i>, jadi
 *       layar pembuatnya wajib memakai menu terdaftar sendiri — bukan disisipkan sebagai
 *       tab di bawah menu induk (pola pewarisan hak lewat menu induk yang sudah tercatat
 *       berkali-kali di modul akunting). Jalur kebangkitan yang paling mungkin adalah
 *       mesin <code>generic-crud</code>, yang sudah mendaftarkan entity ini di manifesnya;
 *       status <code>REVIEW_REQUIRED</code> di manifes itu sebaiknya dipertahankan sampai
 *       gerbang haknya ada.</li>
 *   <li><b>Riwayat Envers ikut aktif.</b> <code>&#64;Audited</code> membuat setiap versi
 *       baris digandakan ke tabel <code>template_transaksi_aud</code> — termasuk revisi
 *       yang lahir dari getter destruktif di atas, bukan dari tindakan pengguna.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 *
 * <ol>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()},
 *       {@link #getOlehId()}, {@link #onUpdate()}, {@link #getTanggal_dirubah()},
 *       {@link #toString()}.</li>
 *   <li><b>Penanggalan &amp; periode:</b> {@link #getTanggalTransaksi()},
 *       {@link #getTanggalDimasukkan()}, {@link #getBulan()}, {@link #getTahun()},
 *       {@link #getJatuhTempo()}, {@link #getTanggalBayar()}.</li>
 *   <li><b>Dimensi jurnal:</b> {@link #getAkun()}, {@link #getSubAkun()},
 *       {@link #getDevisi()}, {@link #getPegawai()}, {@link #getJenisTransaksi()},
 *       {@link #getJenisJurnal()}, {@link #getDefaultWorkspace()}.</li>
 *   <li><b>Nominal:</b> {@link #getDebet()}, {@link #getKredit()},
 *       {@link #getMerupakanDebet()}, {@link #getJumlahTransaksi()},
 *       {@link #getMatauang()}, {@link #getCurrencyCurs()}, {@link #getNilaiRupiah()}.</li>
 *   <li><b>Status alur kerja:</b> {@link #getSimpan()}, {@link #getStatusPosting()},
 *       {@link #getStatusPemeriksaan()}, {@link #getTanggalPosting()},
 *       {@link #getPostingHistory()}.</li>
 *   <li><b>Penautan:</b> {@link #getTemplateGrupTransaksi()}, {@link #getKode()},
 *       {@link #getParentCode()}, {@link #getRandomValue()}.</li>
 * </ol>
 *
 * <p>Kelas ini mewarisi {@link ais.database.model.GeneralValueObject}. Perlu diingat bahwa
 * kelas induk tersebut <b>bukan</b> <code>&#64;Entity</code> maupun
 * <code>&#64;MappedSuperclass</code> — ia POJO abstrak biasa, sehingga Hibernate tidak
 * memetakan propertinya. Field yang dideklarasikan ulang di sini (mis.
 * <code>oleh</code>/<code>olehId</code>) karena itu <b>bukan bug duplikasi</b>, melainkan
 * keharusan teknis agar kolomnya benar-benar terpetakan. Fasilitas yang benar-benar
 * dipakai dari induk adalah <code>check(...)</code>, pe-resolve proxy lazy yang dipanggil
 * {@link #getAkun()} dan {@link #getDefaultWorkspace()}.</p>
 *
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.TemplateGrupTransaksi
 * @see ais.database.model.akunting.TemplateJurnalPenyesuaian
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "template_transaksi")

public class TemplateTransaksi extends GeneralValueObject {

	/**
	 * Nilai {@link #getStatusPosting() status posting} untuk baris template yang belum
	 * terikat batch posting mana pun.
	 */
	public static final Integer STATUS_POSTING_BELUM = 0;
	/**
	 * Nilai {@link #getStatusPosting() status posting} untuk baris template yang sudah
	 * terikat sebuah {@link PostingHistory}.
	 */
	public static final Integer STATUS_POSTING_SELESAI = 1;

	/**
	 * Nilai {@link #getStatusPemeriksaan() status pemeriksaan} untuk baris yang belum
	 * diperiksa. Nilai bawaan field.
	 */
	public static final Integer STATUS_PEMERIKSAAN_BELUM = 0;
	/**
	 * Nilai {@link #getStatusPemeriksaan() status pemeriksaan} untuk baris yang sudah
	 * diperiksa.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada kode di repositori yang pernah menyetel nilai
	 * ini pada entity ini — status pemeriksaan template praktis membeku di
	 * {@link #STATUS_PEMERIKSAAN_BELUM}.</p>
	 */
	public static final Integer STATUS_PEMERIKSAAN_SELESAI = 1;

	/** Nilai {@link #getJenisJurnal() jenis jurnal} untuk buku kas masuk (penerimaan). */
	public static final String JURNAL_KAS_MASUK = "Kas Masuk";
	/** Nilai {@link #getJenisJurnal() jenis jurnal} untuk buku kas keluar (pengeluaran). */
	public static final String JURNAL_KAS_KELUAR = "Kas Keluar";
	/** Nilai {@link #getJenisJurnal() jenis jurnal} untuk jurnal umum/memorial. */
	public static final String JURNAL_UMUM = "Umum";
	/**
	 * Nilai {@link #getJenisJurnal() jenis jurnal} untuk jurnal transaksi umum.
	 *
	 * <p>Keempat konstanta ini digandakan apa adanya dari
	 * {@link ais.database.model.akunting.Transaksi}; karena entity ini tidak dipakai layar
	 * mana pun, tidak satu pun di antaranya pernah dibaca dari kelas ini.</p>
	 */
	public static final String JURNAL_TRANSAKSI = "Transaksi";

	/**
	 * Versi serialisasi Java.
	 *
	 * <p><b>Catatan:</b> nilai ini <b>identik</b> dengan
	 * {@link ais.database.model.akunting.Transaksi},
	 * {@link ais.database.model.akunting.GrupTransaksi}, dan
	 * {@link ais.database.model.akunting.TemplateGrupTransaksi} — bukti bahwa keempatnya
	 * lahir dari satu cetakan salin-tempel.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer baris template (kolom <code>id</code>, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi interceptor audit. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris template ini.
	 *
	 * @return id pengguna pengubah, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah baris template ini.
	 *
	 * <p><b>Non-obvious:</b> nilai <code>null</code>, string kosong, atau string yang hanya
	 * berisi spasi <b>diabaikan diam-diam</b> (method langsung <code>return</code>),
	 * sehingga nilai lama dipertahankan. Jejak audit tidak dapat dikosongkan sekali sudah
	 * terisi, dan pemanggil tidak mendapat umpan balik apa pun bahwa nilainya ditolak.
	 * Perilaku ini sama persis dengan
	 * {@link ais.database.model.akunting.Transaksi#setOlehId(String)}.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong/null
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris template, yaitu isi {@link #getKeterangan() keterangan} apa
	 * adanya.
	 *
	 * <p><b>Non-obvious:</b> method membaca field <code>keterangan</code> secara langsung
	 * (bukan lewat getter) dan tidak menjaga <code>null</code>. Karena
	 * {@link #setKeterangan(String)} menerima <code>null</code>, method ini dapat
	 * mengembalikan <code>null</code> — berisiko bagi komponen ZK yang memanggil
	 * <code>toString()</code> untuk label. Bandingkan dengan
	 * {@link ais.database.model.akunting.TemplateGrupTransaksi} yang menampilkan
	 * <code>kode</code>, bukan keterangan.</p>
	 *
	 * @return keterangan baris template, mungkin <code>null</code>
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menyetel nama pengguna pengubah baris template ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai kosong/null <b>diabaikan
	 * diam-diam</b> dan nilai lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong/null
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris template ini.
	 *
	 * @return nama pengguna pengubah, atau <code>null</code> bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait <code>&#64;PreUpdate</code> JPA yang menyerahkan pencatatan jejak audit
	 * (<code>oleh</code>, <code>olehId</code>, <code>tanggal_dirubah</code>) kepada
	 * <code>ais.database.hibernate.AuditTimestampInterceptor</code> sesaat sebelum
	 * Hibernate menerbitkan <code>UPDATE</code>.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Karena kait ini bereaksi pada
	 * <i>setiap</i> UPDATE, getter destruktif yang disebut pada Javadoc kelas juga akan
	 * memicu pembaruan stempel audit — dan karena kelas ini <code>&#64;Audited</code>,
	 * setiap pembaruan semacam itu menambah revisi baru di tabel
	 * <code>template_transaksi_aud</code> yang tidak pernah berasal dari tindakan
	 * pengguna.</p>
	 *
	 * <p><b>Catatan gaya:</b> baris di bawah ini sengaja memuat deklarasi method dan
	 * deklarasi field <code>tanggal_dirubah</code> sekaligus — bentuk salin-tempel yang
	 * seragam di seluruh entity AIS. Jangan dipecah agar mudah dibandingkan antar
	 * berkas.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris template ini.
	 *
	 * <p>Lazimnya dipanggil oleh <code>AuditTimestampInterceptor</code> lewat
	 * {@link #onUpdate()}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris template ini (kolom
	 * <code>tanggal_dirubah</code>, presisi TIMESTAMP).
	 *
	 * @return stempel waktu perubahan terakhir; secara bawaan berisi waktu pembuatan objek
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode bukti jurnal cetakan; padanan <code>Transaksi.kode</code>. */
	private String kode;
	/** Jenis buku jurnal; salah satu dari konstanta <code>JURNAL_*</code>. */
	private String jenisJurnal;
	/** Tanggal efektif transaksi; disegarkan dari induk oleh {@link #getTanggalTransaksi()}. */
	private Date tanggalTransaksi = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal baris template dimasukkan; bawaan waktu pembuatan objek. */
	private Date tanggalDimasukkan = ais.ui.util.WaktuUtil.getDate();
	/** Bulan periode (1&ndash;12); diisi sendiri oleh {@link #getBulan()} bila kosong. */
	private Integer bulan;
	/** Tahun periode; diisi sendiri oleh {@link #getTahun()} bila kosong. */
	private Integer tahun;
	/** Divisi/unit pembeban; dimensi pelaporan segmen. */
	private Devisi devisi;
	/** Pegawai yang terkait baris template ini (mis. penerima/penanggung jawab). */
	private Pegawai pegawai;
	/** Keterangan baris; juga menjadi hasil {@link #toString()}. Bawaan string kosong. */
	private String keterangan = "";
	/** Perkiraan (akun buku besar) yang dibebani baris ini. */
	private Akun akun;
	/** Mata uang nominal baris; dipasangkan dengan {@link #currencyCurs}. */
	private Matauang matauang;
	/** Sub-perkiraan bebas berupa teks. */
	private String subAkun;
	/** Penanda sisi jurnal (debet bila <code>true</code>); tidak dipakai dari kelas ini. */
	private Boolean merupakanDebet;
	/** Nominal sisi debet; dinormalkan ke <code>0.0</code> oleh {@link #getDebet()}. */
	private Double debet = 0.0;
	/** Nominal sisi kredit; dinormalkan ke <code>0.0</code> oleh {@link #getKredit()}. */
	private Double kredit = 0.0;
	/** Tanggal jatuh tempo (presisi DATE). */
	private Date jatuhTempo;
	/** Tanggal pembayaran (presisi DATE). */
	private Date tanggalBayar;
	/** Tanggal posting; hanya bisa <i>dikosongkan</i> oleh {@link #getTanggalPosting()}. */
	private Date tanggalPosting;
	/** Status posting; selalu dihitung ulang oleh {@link #getStatusPosting()}. */
	private Integer statusPosting = STATUS_POSTING_BELUM;
	/** Status pemeriksaan; tidak pernah diubah dari mana pun di repositori. */
	private Integer statusPemeriksaan = STATUS_PEMERIKSAAN_BELUM;

	/**
	 * Penanda baris sudah dinyatakan resmi (bukan draft). Bawaan <code>false</code>.
	 *
	 * <p>Pada {@link ais.database.model.akunting.Transaksi} kolom senama menjadi pembeda
	 * antara baris jurnal draft dan resmi — dan menjadi sumber kebocoran draft ke laporan
	 * karena mayoritas kueri tidak memfilternya. Di sini kolom tersebut ikut tersalin
	 * tetapi tidak pernah dibaca siapa pun.</p>
	 */
	private Boolean simpan = false;

	/** Jumlah transaksi dalam mata uang asal (sebelum konversi). */
	private Double jumlahTransaksi = 0.0;
	/** Kurs mata uang terhadap rupiah pada saat transaksi. */
	private Double currencyCurs = 0.0;
	/** Nilai hasil konversi ke rupiah. */
	private Double nilaiRupiah = 0.0;

	/** Kode bukti induk secara denormalisasi (untuk jurnal yang diketik manual). */
	private String parentCode;
	/**
	 * Header template pemilik baris ini.
	 *
	 * <p><b>Perhatikan:</b> nama field <code>grupTransaksi</code> tidak sama dengan nama
	 * properti persistennya. Karena akses properti dipakai, Hibernate memakai nama getter
	 * — yaitu <code>templateGrupTransaksi</code> — sementara kolomnya tetap
	 * <code>grup_transaksi</code>.</p>
	 */
	private TemplateGrupTransaksi grupTransaksi;
	/**
	 * Ruang kerja (workspace) bawaan baris template.
	 *
	 * <p>Field yang sama sudah <b>dikomentari</b> pada
	 * {@link ais.database.model.akunting.Transaksi}; di sini masih hidup dan terpetakan,
	 * tetapi {@link #setDefaultWorkspace(Workspace)} tidak pernah dipanggil dari mana pun
	 * sehingga kolomnya selalu <code>null</code>.</p>
	 */
	private Workspace defaultWorkspace;
	// private AkunPajak akunPajak;

	/** Jenis/kode seri jurnal; diwarisi dari header oleh {@link #getJenisTransaksi()}. */
	private JenisTransaksi jenisTransaksi;

	/** Nilai acak pembeda baris; dibangkitkan sendiri oleh {@link #getRandomValue()}. */
	private Long randomValue;

	/** Cap batch posting; keberadaannya menentukan {@link #getStatusPosting()}. */
	private PostingHistory postingHistory;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Nilai bawaan field (tanggal = waktu sekarang, keterangan = string kosong,
	 * debet/kredit/kurs = <code>0.0</code>, status = <i>belum</i>) berlaku sejak objek
	 * dibuat.</p>
	 */
	public TemplateTransaksi() {
	}

	/**
	 * Mengembalikan kunci primer baris template ini.
	 *
	 * @return id baris, atau <code>null</code> bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris template ini.
	 *
	 * <p>Lazimnya hanya dipanggil Hibernate saat memuat/menyimpan baris.</p>
	 *
	 * @param id kunci primer baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan baris template (kolom <code>keterangan</code>).
	 *
	 * @return keterangan baris; bawaan string kosong, dapat <code>null</code> bila pernah
	 *         disetel demikian
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan baris template.
	 *
	 * <p>Nilai <code>null</code> diterima apa adanya — inilah yang membuat
	 * {@link #toString()} bisa mengembalikan <code>null</code>.</p>
	 *
	 * @param keterangan keterangan baris
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel tanggal efektif transaksi baris template.
	 *
	 * <p><b>Non-obvious:</b> nilai yang disetel di sini akan <b>ditimpa</b> oleh
	 * {@link #getTanggalTransaksi()} pada pembacaan berikutnya bila baris sudah punya
	 * header {@link #getTemplateGrupTransaksi()} — tanggal header selalu menang.</p>
	 *
	 * @param tanggalTransaksi tanggal efektif transaksi
	 */
	public void setTanggalTransaksi(Date tanggalTransaksi) {
		this.tanggalTransaksi = tanggalTransaksi;
	}

	/**
	 * Mengembalikan tanggal efektif transaksi baris template (kolom
	 * <code>tanggal_transaksi</code>, presisi TIMESTAMP).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #getTemplateGrupTransaksi()
	 * header} tidak <code>null</code>, field <code>tanggalTransaksi</code> <b>ditimpa</b>
	 * dengan tanggal header pada setiap pembacaan. Perilaku ini menjaga baris selalu
	 * mengikuti header, tetapi berarti perubahan tanggal induk merambat ke baris lama
	 * secara senyap — dan karena <code>dynamicUpdate = true</code>, penulisan balik
	 * tersebut dapat ter-<i>flush</i> ke basis data. Identik dengan
	 * {@link ais.database.model.akunting.Transaksi#getTanggalTransaksi()}.</p>
	 *
	 * <p>Bila setelah penyegaran nilainya masih <code>null</code>, method jatuh ke
	 * {@link #getTanggalDimasukkan()} sebagai pengganti (nilai kembalian saja — field tidak
	 * diisi).</p>
	 *
	 * @return tanggal transaksi header bila ada, jika tidak tanggal transaksi baris, dan
	 *         sebagai pilihan terakhir tanggal dimasukkan; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_transaksi", nullable = true)
	public Date getTanggalTransaksi() {
		if (grupTransaksi != null) {
			tanggalTransaksi = grupTransaksi.getTanggalTransaksi();
		}
		return tanggalTransaksi == null ? getTanggalDimasukkan() : tanggalTransaksi;
	}

	/**
	 * Menyetel tanggal baris template dimasukkan ke sistem.
	 *
	 * @param tanggalDimasukkan tanggal pemasukan data
	 */
	public void setTanggalDimasukkan(Date tanggalDimasukkan) {
		this.tanggalDimasukkan = tanggalDimasukkan;
	}

	/**
	 * Mengembalikan tanggal baris template dimasukkan (kolom
	 * <code>tanggal_dimasukkan</code>, presisi TIMESTAMP).
	 *
	 * <p><b>Non-obvious:</b> bila field bernilai <code>null</code>, method mengembalikan
	 * waktu <i>sekarang</i> tanpa menuliskannya ke field. Jadi nilai kembalian bisa
	 * berbeda tiap pemanggilan untuk baris yang kolomnya kosong, dan tidak mencerminkan
	 * kapan baris benar-benar dibuat.</p>
	 *
	 * @return tanggal pemasukan data; tidak pernah <code>null</code>
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dimasukkan", nullable = true)
	public Date getTanggalDimasukkan() {
		return tanggalDimasukkan == null ? ais.ui.util.WaktuUtil.getDate() : tanggalDimasukkan;
	}

	/**
	 * Menyetel bulan periode baris template.
	 *
	 * @param bulan bulan periode (1&ndash;12)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengembalikan bulan periode baris template.
	 *
	 * <p><b>Efek samping (getter penulis):</b> bila field masih <code>null</code>, method
	 * mengisinya dengan bulan <i>saat ini</i> menurut jam sistem
	 * (<code>Calendar.MONTH + 1</code>, sudah dikoreksi dari basis nol) lalu menuliskannya
	 * ke field.</p>
	 *
	 * <p><b>Non-obvious:</b> nilai diambil dari jam sistem, <b>bukan</b> dari
	 * {@link #getTanggalTransaksi()}. Membaca baris template lama pada bulan yang berbeda
	 * karena itu memberi periode yang salah, dan (karena <code>dynamicUpdate</code>)
	 * berpotensi menuliskan periode yang salah tersebut ke basis data. Perhatikan pula
	 * tidak ada <code>&#64;Column</code> pada method ini sehingga Hibernate memakai nama
	 * kolom bawaan.</p>
	 *
	 * @return bulan periode (1&ndash;12); tidak pernah <code>null</code>
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Menyetel tahun periode baris template.
	 *
	 * @param tahun tahun periode (empat digit)
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun periode baris template.
	 *
	 * <p><b>Efek samping (getter penulis):</b> sama seperti {@link #getBulan()} — bila
	 * field masih <code>null</code>, diisi dengan tahun <i>saat ini</i> menurut jam sistem
	 * dan dituliskan ke field, bukan diturunkan dari tanggal transaksi.</p>
	 *
	 * @return tahun periode; tidak pernah <code>null</code>
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Menyetel divisi/unit pembeban baris template.
	 *
	 * @param devisi divisi pembeban, boleh <code>null</code>
	 */
	public void setDevisi(Devisi devisi) {
		this.devisi = devisi;
	}

	/**
	 * Mengembalikan divisi/unit pembeban baris template (kolom <code>devisi</code>).
	 *
	 * <p>Relasi <code>ManyToOne</code> dengan <code>FetchMode.SELECT</code> dan cascade
	 * PERSIST/MERGE — menyimpan baris ini ikut menyimpan objek {@link Devisi} yang belum
	 * tersimpan.</p>
	 *
	 * @return divisi pembeban, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "devisi", nullable = true)
	public Devisi getDevisi() {
		return devisi;
	}

	/**
	 * Menyetel pegawai yang terkait baris template ini.
	 *
	 * @param pegawai pegawai terkait, boleh <code>null</code>
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * Mengembalikan pegawai yang terkait baris template ini (kolom <code>pegawai</code>).
	 *
	 * <p>Relasi <code>ManyToOne</code> dengan cascade PERSIST/MERGE. Tidak ada resolusi
	 * proxy <code>check(...)</code> di sini — berbeda dari {@link #getAkun()} — sehingga
	 * nilai kembalian dapat berupa proxy Hibernate yang belum terinisialisasi bila sesi
	 * sudah tertutup.</p>
	 *
	 * @return pegawai terkait, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai", nullable = true)
	public Pegawai getPegawai() {
		return pegawai;
	}

	/**
	 * Menyetel perkiraan (akun buku besar) yang dibebani baris template ini.
	 *
	 * @param akun perkiraan tujuan, boleh <code>null</code>
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan perkiraan (akun buku besar) yang dibebani baris template ini (kolom
	 * <code>akun</code>).
	 *
	 * <p>Relasi <code>ManyToOne</code> <code>LAZY</code>; field disegarkan lewat
	 * <code>check(...)</code> dari {@link ais.database.model.GeneralValueObject} agar proxy
	 * lazy sudah ter-resolve saat dikembalikan. Penulisan balik ini pada praktiknya
	 * mengganti proxy dengan objek nyata — tidak mengubah identitas akun.</p>
	 *
	 * <p><b>Perbedaan penting dari kembarannya:</b>
	 * {@link ais.database.model.akunting.Transaksi#getAkun()} lebih dulu memeriksa
	 * <code>akunOver</code> dan, bila ada, <b>menimpa</b> <code>akun</code> dengan akun
	 * pengganti tersebut secara permanen — memindahkan atribusi akun buku besar hanya
	 * dengan membaca baris. Mekanisme <code>akunOver</code> itu <b>tidak ada</b> pada kelas
	 * ini, sehingga getter di sini tidak destruktif dalam arti tersebut.</p>
	 *
	 * @return perkiraan baris template, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel sub-perkiraan bebas berupa teks.
	 *
	 * @param subAkun kode/keterangan sub-perkiraan
	 */
	public void setSubAkun(String subAkun) {
		this.subAkun = subAkun;
	}

	/**
	 * Mengembalikan sub-perkiraan bebas berupa teks (kolom <code>sub_akun</code>).
	 *
	 * <p><b>Catatan verifikasi:</b> kolom warisan — tidak ada kode di repositori yang
	 * pernah mengisinya untuk entity ini.</p>
	 *
	 * @return sub-perkiraan, atau <code>null</code>
	 */
	@Column(name = "sub_akun", nullable = true)
	public String getSubAkun() {
		return subAkun;
	}

	/**
	 * Menyetel nominal sisi debet baris template.
	 *
	 * @param debet nominal debet; <code>null</code> akan dinormalkan menjadi
	 *              <code>0.0</code> pada pembacaan berikutnya
	 */
	public void setDebet(Double debet) {
		this.debet = debet;
	}

	/**
	 * Mengembalikan nominal sisi debet baris template.
	 *
	 * <p><b>Efek samping:</b> bila field <code>null</code>, ia <b>ditulis</b> menjadi
	 * <code>0.0</code> lebih dulu — bukan sekadar dikembalikan sebagai nol.</p>
	 *
	 * <p><b>Non-obvious:</b> seperti pada {@link ais.database.model.akunting.Transaksi},
	 * nominal disimpan pada <b>dua kolom terpisah</b> (<code>debet</code> dan
	 * <code>kredit</code>), bukan satu kolom bertanda; sisi yang tidak terpakai berisi
	 * <code>0.0</code>. Tidak ada <code>&#64;Column</code> eksplisit pada method ini.</p>
	 *
	 * @return nominal debet; tidak pernah <code>null</code>
	 */
	public Double getDebet() {
		if (debet == null) {
			debet = 0.0;
		}
		return debet;
	}

	/**
	 * Menyetel nominal sisi kredit baris template.
	 *
	 * @param kredit nominal kredit; <code>null</code> akan dinormalkan menjadi
	 *               <code>0.0</code> pada pembacaan berikutnya
	 */
	public void setKredit(Double kredit) {
		this.kredit = kredit;
	}

	/**
	 * Mengembalikan nominal sisi kredit baris template.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getDebet()} — <code>null</code> ditulis
	 * menjadi <code>0.0</code> pada field.</p>
	 *
	 * <p>Tidak ada penjaga apa pun di kelas ini yang memastikan hanya satu dari
	 * debet/kredit yang terisi, maupun bahwa total debet sama dengan total kredit dalam
	 * satu header — keseimbangan sepenuhnya menjadi tanggung jawab pemanggil (yang saat ini
	 * tidak ada).</p>
	 *
	 * @return nominal kredit; tidak pernah <code>null</code>
	 */
	public Double getKredit() {
		if (kredit == null) {
			kredit = 0.0;
		}
		return kredit;
	}

	/**
	 * Menyetel tanggal jatuh tempo baris template.
	 *
	 * @param jatuhTempo tanggal jatuh tempo, boleh <code>null</code>
	 */
	public void setJatuhTempo(Date jatuhTempo) {
		this.jatuhTempo = jatuhTempo;
	}

	/**
	 * Mengembalikan tanggal jatuh tempo baris template (kolom <code>jatuh_tempo</code>,
	 * presisi DATE — komponen jam dibuang).
	 *
	 * @return tanggal jatuh tempo, atau <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "jatuh_tempo", nullable = true)
	public Date getJatuhTempo() {
		return jatuhTempo;
	}

	/**
	 * Menyetel tanggal pembayaran baris template.
	 *
	 * @param tanggalBayar tanggal pembayaran, boleh <code>null</code>
	 */
	public void setTanggalBayar(Date tanggalBayar) {
		this.tanggalBayar = tanggalBayar;
	}

	/**
	 * Mengembalikan tanggal pembayaran baris template (kolom <code>tanggal_bayar</code>,
	 * presisi DATE).
	 *
	 * @return tanggal pembayaran, atau <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_bayar", nullable = true)
	public Date getTanggalBayar() {
		return tanggalBayar;
	}

	/**
	 * Menyetel tanggal posting baris template.
	 *
	 * <p>Ini satu-satunya jalan mengisi kolom tersebut — {@link #getTanggalPosting()} hanya
	 * bisa mengosongkannya.</p>
	 *
	 * @param tanggalPosting tanggal posting ke buku besar
	 */
	public void setTanggalPosting(Date tanggalPosting) {
		this.tanggalPosting = tanggalPosting;
	}

	/**
	 * Mengembalikan tanggal posting baris template (kolom <code>tanggal_posting</code>,
	 * presisi DATE).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> bila {@link #getPostingHistory()} bernilai
	 * <code>null</code>, field <code>tanggalPosting</code> <b>dinolkan</b> lalu
	 * dikembalikan. Method ini hanya bisa <i>menghapus</i> tanggal, tidak pernah
	 * mengisinya dari batch posting. Karena <code>dynamicUpdate = true</code>, pembacaan di
	 * dalam sesi hidup atas baris yang cap postingnya sudah dilepas akan mengosongkan kolom
	 * di basis data. Perilaku identik dengan
	 * {@link ais.database.model.akunting.Transaksi#getTanggalPosting()}.</p>
	 *
	 * @return tanggal posting bila baris terikat batch posting, jika tidak <code>null</code>
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_posting", nullable = true)
	public Date getTanggalPosting() {
		tanggalPosting = getPostingHistory() == null ? null : tanggalPosting;
		return tanggalPosting;
	}

	/**
	 * Menyetel status posting baris template.
	 *
	 * <p><b>Non-obvious:</b> nilai yang disetel di sini <b>tidak pernah terbaca kembali</b>
	 * lewat {@link #getStatusPosting()}, karena getter tersebut selalu menghitung ulang dari
	 * ada-tidaknya {@link #getPostingHistory()}. Yang tersisa hanyalah nilai kolom
	 * <code>status_posting</code> yang tersimpan — berguna bagi kueri Criteria, meski tidak
	 * ada kueri semacam itu untuk entity ini.</p>
	 *
	 * @param statusPosting {@link #STATUS_POSTING_BELUM} atau {@link #STATUS_POSTING_SELESAI}
	 */
	public void setStatusPosting(Integer statusPosting) {
		this.statusPosting = statusPosting;
	}

	/**
	 * Mengembalikan status posting baris template (kolom <code>status_posting</code>).
	 *
	 * <p><b>Efek samping (getter destruktif):</b> field <b>selalu ditimpa</b> dengan hasil
	 * perhitungan <code>postingHistory == null ? BELUM : SELESAI</code>, apa pun nilai yang
	 * tersimpan. Nilai yang pernah disetel {@link #setStatusPosting(Integer)} karena itu
	 * dapat berubah sendiri saat baris dibaca, dan perubahan tersebut ikut ter-<i>flush</i>
	 * ke basis data.</p>
	 *
	 * <p><b>Non-obvious — status ini tidak menghormati pembatalan posting.</b> Yang
	 * diperiksa hanyalah <i>keberadaan</i> {@link PostingHistory}, bukan bendera
	 * <code>postingHistory.posting</code>. Baris yang batch postingnya sudah dibatalkan
	 * tetap dilaporkan {@link #STATUS_POSTING_SELESAI}. Kekeliruan yang sama sudah
	 * terdokumentasi pada {@link ais.database.model.akunting.Transaksi#getStatusPosting()};
	 * di sini dampaknya nihil selama entity belum dipakai.</p>
	 *
	 * @return {@link #STATUS_POSTING_SELESAI} bila baris terikat batch posting mana pun,
	 *         jika tidak {@link #STATUS_POSTING_BELUM}
	 */
	@Column(name = "status_posting", nullable = true)
	public Integer getStatusPosting() {
		statusPosting = getPostingHistory() == null ? STATUS_POSTING_BELUM : STATUS_POSTING_SELESAI;
		return statusPosting;
	}

	/**
	 * Menyetel kode bukti jurnal cetakan.
	 *
	 * @param kode kode bukti jurnal
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode bukti jurnal cetakan.
	 *
	 * <p>Tidak ada <code>&#64;Column</code> eksplisit, sehingga Hibernate memakai nama
	 * kolom bawaan <code>kode</code>. Tidak ada penjaga keunikan apa pun di tingkat entity
	 * ini.</p>
	 *
	 * @return kode bukti jurnal, atau <code>null</code>
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menyetel status pemeriksaan baris template.
	 *
	 * @param statusPemeriksaan {@link #STATUS_PEMERIKSAAN_BELUM} atau
	 *                          {@link #STATUS_PEMERIKSAAN_SELESAI}
	 */
	public void setStatusPemeriksaan(Integer statusPemeriksaan) {
		this.statusPemeriksaan = statusPemeriksaan;
	}

	/**
	 * Mengembalikan status pemeriksaan baris template (kolom
	 * <code>status_pemeriksaan</code>).
	 *
	 * <p>Berbeda dari {@link #getStatusPosting()}, getter ini <b>tidak</b> menghitung ulang
	 * — ia mengembalikan nilai tersimpan apa adanya. Namun karena tidak ada pemanggil
	 * {@link #setStatusPemeriksaan(Integer)} di repositori, nilainya membeku pada bawaan
	 * {@link #STATUS_PEMERIKSAAN_BELUM}; alur "pemeriksaan" untuk template tidak pernah
	 * diimplementasikan.</p>
	 *
	 * @return status pemeriksaan; bawaan {@link #STATUS_PEMERIKSAAN_BELUM}
	 */
	@Column(name = "status_pemeriksaan", nullable = true)
	public Integer getStatusPemeriksaan() {
		return statusPemeriksaan;
	}

	/**
	 * Menyetel penanda sisi jurnal baris template.
	 *
	 * @param merupakanDebet <code>true</code> bila baris berada di sisi debet
	 */
	public void setMerupakanDebet(Boolean merupakanDebet) {
		this.merupakanDebet = merupakanDebet;
	}

	/**
	 * Mengembalikan penanda sisi jurnal baris template (kolom
	 * <code>merupakan_debet</code>).
	 *
	 * <p><b>Non-obvious:</b> penanda ini <i>berlebihan</i> terhadap pasangan kolom
	 * {@link #getDebet()}/{@link #getKredit()} dan tidak dijaga konsistensinya dengan
	 * keduanya. Bertipe {@link Boolean} sehingga dapat bernilai <code>null</code> (belum
	 * ditentukan) — pemanggil wajib menjaga <code>null</code> sebelum meng-<i>unbox</i>.</p>
	 *
	 * @return <code>true</code> sisi debet, <code>false</code> sisi kredit, atau
	 *         <code>null</code> bila belum ditentukan
	 */
	@Column(name = "merupakan_debet", nullable = true)
	public Boolean getMerupakanDebet() {
		return merupakanDebet;
	}

	/**
	 * Menyetel mata uang nominal baris template.
	 *
	 * @param matauang mata uang, boleh <code>null</code> (dianggap rupiah)
	 */
	public void setMatauang(Matauang matauang) {
		this.matauang = matauang;
	}

	/**
	 * Mengembalikan mata uang nominal baris template (kolom <code>matauang</code>).
	 *
	 * <p>Dipasangkan dengan {@link #getCurrencyCurs() kurs} dan
	 * {@link #getNilaiRupiah() nilai rupiah}. Tidak ada resolusi proxy
	 * <code>check(...)</code>, sehingga nilai kembalian bisa berupa proxy lazy.</p>
	 *
	 * @return mata uang, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matauang", nullable = true)
	public Matauang getMatauang() {
		return matauang;
	}

	/**
	 * Menyetel jumlah transaksi dalam mata uang asal.
	 *
	 * @param jumlahTransaksi nominal dalam mata uang asal
	 */
	public void setJumlahTransaksi(Double jumlahTransaksi) {
		this.jumlahTransaksi = jumlahTransaksi;
	}

	/**
	 * Mengembalikan jumlah transaksi dalam mata uang asal (kolom
	 * <code>jumlah_transaksi</code>).
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link #getDebet()}/{@link #getKredit()}, getter
	 * ini <b>tidak</b> menormalkan <code>null</code> menjadi <code>0.0</code> meski
	 * fieldnya berbawaan <code>0.0</code>. Baris yang kolomnya <code>NULL</code> di basis
	 * data akan mengembalikan <code>null</code> dan berpotensi NPE saat di-<i>unbox</i>.
	 * Ketidakseragaman yang sama ada pada kembarannya.</p>
	 *
	 * @return nominal dalam mata uang asal, dapat <code>null</code>
	 */
	@Column(name = "jumlah_transaksi", nullable = true)
	public Double getJumlahTransaksi() {
		return jumlahTransaksi;
	}

	/**
	 * Menyetel kurs mata uang terhadap rupiah.
	 *
	 * @param currencyCurs kurs konversi
	 */
	public void setCurrencyCurs(Double currencyCurs) {
		this.currencyCurs = currencyCurs;
	}

	/**
	 * Mengembalikan kurs mata uang terhadap rupiah (kolom <code>currency_curs</code>).
	 *
	 * <p>Nilai bawaan <code>0.0</code> — bukan <code>1.0</code>. Bila dipakai sebagai
	 * pengali tanpa penjaga, baris yang kursnya belum diisi menghasilkan nilai rupiah nol,
	 * bukan nilai apa adanya.</p>
	 *
	 * @return kurs konversi, dapat <code>null</code>
	 */
	@Column(name = "currency_curs", nullable = true)
	public Double getCurrencyCurs() {
		return currencyCurs;
	}

	/**
	 * Menyetel nilai hasil konversi ke rupiah.
	 *
	 * @param nilaiRupiah nilai dalam rupiah
	 */
	public void setNilaiRupiah(Double nilaiRupiah) {
		this.nilaiRupiah = nilaiRupiah;
	}

	/**
	 * Mengembalikan nilai hasil konversi ke rupiah (kolom <code>nilai_rupiah</code>).
	 *
	 * <p><b>Non-obvious:</b> kelas ini tidak pernah menghitung sendiri
	 * <code>jumlahTransaksi &times; currencyCurs</code>; nilai ini murni disimpan apa
	 * adanya dan tidak ada penjaga yang memastikan ia konsisten dengan kedua kolom
	 * tersebut.</p>
	 *
	 * @return nilai dalam rupiah, dapat <code>null</code>
	 */
	@Column(name = "nilai_rupiah", nullable = true)
	public Double getNilaiRupiah() {
		return nilaiRupiah;
	}

	/**
	 * Menyetel kode bukti induk secara denormalisasi.
	 *
	 * @param parentCode kode bukti induk
	 */
	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	/**
	 * Mengembalikan kode bukti induk secara denormalisasi (kolom <code>parent_code</code>).
	 *
	 * <p>Pada {@link ais.database.model.akunting.Transaksi} kolom ini menjadi pengikat
	 * baris-baris jurnal yang diketik manual sebelum headernya tersimpan. Di kelas ini ia
	 * ikut tersalin tetapi tidak pernah diisi.</p>
	 *
	 * @return kode bukti induk, atau <code>null</code>
	 */
	@Column(name = "parent_code", nullable = true)
	public String getParentCode() {
		return parentCode;
	}

	/**
	 * Menyetel jenis buku jurnal baris template.
	 *
	 * @param jenisJurnal salah satu konstanta <code>JURNAL_*</code>
	 */
	public void setJenisJurnal(String jenisJurnal) {
		this.jenisJurnal = jenisJurnal;
	}

	/**
	 * Mengembalikan jenis buku jurnal baris template (kolom <code>jenis_jurnal</code>).
	 *
	 * <p>Disimpan sebagai teks bebas, bukan enum — tidak ada validasi bahwa isinya salah
	 * satu dari konstanta <code>JURNAL_*</code>. Sumbu klasifikasi ini terpisah dari
	 * {@link #getJenisTransaksi()}, yang merupakan kode/nomor seri jurnal buatan
	 * operator.</p>
	 *
	 * @return jenis buku jurnal, atau <code>null</code>
	 */
	@Column(name = "jenis_jurnal", nullable = true)
	public String getJenisJurnal() {
		return jenisJurnal;
	}

	/**
	 * Menyetel header template pemilik baris ini.
	 *
	 * <p>Menyetel header berarti baris akan mengikuti tanggal header lewat
	 * {@link #getTanggalTransaksi()} dan mewarisi jenis transaksinya lewat
	 * {@link #getJenisTransaksi()}.</p>
	 *
	 * @param grupTransaksi header template pemilik, boleh <code>null</code>
	 */
	public void setTemplateGrupTransaksi(TemplateGrupTransaksi grupTransaksi) {
		this.grupTransaksi = grupTransaksi;
	}

	/**
	 * Mengembalikan header template pemilik baris ini (kolom <code>grup_transaksi</code>).
	 *
	 * <p>Ini padanan <code>Transaksi.getGrupTransaksi()</code> pada keluarga template.
	 * Relasi <code>ManyToOne</code> dengan cascade PERSIST/MERGE, sehingga menyimpan baris
	 * ikut menyimpan headernya.</p>
	 *
	 * <p><b>Kuirk pemetaan.</b> Nama field tetap <code>grupTransaksi</code> dan nama kolom
	 * tetap <code>grup_transaksi</code>, tetapi karena Hibernate memakai <i>property
	 * access</i> (akibat <code>&#64;Id</code> berada di {@link #getId()}), nama properti
	 * persistennya adalah <code>templateGrupTransaksi</code>. HQL/Criteria untuk entity ini
	 * harus memakai nama tersebut — memakai <code>grupTransaksi</code> (yang benar untuk
	 * {@link ais.database.model.akunting.Transaksi}) akan gagal.</p>
	 *
	 * <p><b>Relasi satu arah.</b> {@link ais.database.model.akunting.TemplateGrupTransaksi}
	 * tidak memiliki koleksi balik ke kelas ini, jadi tidak ada cara menavigasi dari header
	 * ke baris-barisnya selain lewat kueri manual.</p>
	 *
	 * @return header template pemilik, atau <code>null</code>
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "grup_transaksi", nullable = true)
	public TemplateGrupTransaksi getTemplateGrupTransaksi() {
		return grupTransaksi;
	}

	/**
	 * Menyetel penanda baris sudah dinyatakan resmi.
	 *
	 * @param simpan <code>true</code> bila baris bukan lagi draft
	 */
	public void setSimpan(Boolean simpan) {
		this.simpan = simpan;
	}

	/**
	 * Mengembalikan penanda baris sudah dinyatakan resmi (bukan draft).
	 *
	 * <p>Salinan kolom <code>simpan</code> milik
	 * {@link ais.database.model.akunting.Transaksi}, tempat kolom tersebut memisahkan baris
	 * jurnal draft dari baris resmi. Pada kelas ini tidak ada satu pun pembaca, sehingga
	 * penanda ini tidak berfungsi apa-apa.</p>
	 *
	 * <p>Tidak ada <code>&#64;Column</code> eksplisit; Hibernate memakai nama kolom bawaan
	 * <code>simpan</code>.</p>
	 *
	 * @return <code>true</code> bila baris dinyatakan resmi; bawaan <code>false</code>
	 */
	public Boolean getSimpan() {
		return simpan;
	}

	/**
	 * Mengembalikan ruang kerja (workspace) bawaan baris template ini (kolom
	 * <code>default_workspace</code>).
	 *
	 * <p>Relasi <code>ManyToOne</code> <code>LAZY</code>; proxy di-resolve lewat
	 * <code>check(...)</code> sebelum dikembalikan, sehingga field ikut ditulis balik
	 * dengan objek nyata.</p>
	 *
	 * <p><b>Non-obvious — satu-satunya dimensi pembatas ruang lingkup pada entity ini.</b>
	 * Kelas ini tidak punya kolom <code>sekolah</code>, <code>yayasan</code>, maupun
	 * <code>satuanKerja</code>. Namun {@link #setDefaultWorkspace(Workspace)} tidak pernah
	 * dipanggil dari mana pun di repositori, jadi kolom ini de facto selalu
	 * <code>null</code> dan tidak membatasi apa pun. Bila fitur template kelak dihidupkan,
	 * penyaringan per tenant harus ditambahkan lebih dahulu.</p>
	 *
	 * <p><b>Catatan silsilah:</b> deklarasi yang sama pada
	 * {@link ais.database.model.akunting.Transaksi} sudah dikomentari — bukti bahwa berkas
	 * ini membeku pada keadaan lama sementara kembarannya terus dirawat.</p>
	 *
	 * @return ruang kerja bawaan, atau <code>null</code> (kasus normal)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "default_workspace", nullable = true)
	public Workspace getDefaultWorkspace() {
		defaultWorkspace = check(defaultWorkspace);
		return defaultWorkspace;
	}

	/**
	 * Menyetel ruang kerja (workspace) bawaan baris template ini.
	 *
	 * <p><b>Catatan verifikasi:</b> tidak ada pemanggil setter ini di seluruh repositori.</p>
	 *
	 * @param defaultWorkspace ruang kerja bawaan, boleh <code>null</code>
	 */
	public void setDefaultWorkspace(Workspace defaultWorkspace) {
		this.defaultWorkspace = defaultWorkspace;
	}

	// Rancangan kaki pajak untuk baris template — tidak pernah diaktifkan.
	// Dibiarkan sebagai komentar agar sejajar dengan Transaksi, yang juga tidak memilikinya.
	// @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	// @Fetch(FetchMode.SELECT)
	// @JoinColumn(name = "akun_pajak", nullable = true)
	// public AkunPajak getAkunPajak() {
	// return akunPajak;
	// }
	//
	// public void setAkunPajak(AkunPajak akunPajak) {
	// this.akunPajak = akunPajak;
	// }

	/**
	 * Mengembalikan nilai acak pembeda baris (kolom <code>random_value</code>).
	 *
	 * <p><b>Efek samping (getter penulis):</b> bila field masih <code>null</code>, method
	 * membangkitkan bilangan acak baru dengan {@link Random} tanpa benih tetap,
	 * menuliskannya ke field, dan <b>mencetaknya ke <code>System.out</code></b>. Jadi
	 * pembacaan pertama atas baris lama akan menerbitkan <code>UPDATE</code> sekaligus satu
	 * baris log per baris yang dirender.</p>
	 *
	 * <p><b>Kuirk pemetaan — inilah kuirk yang dicatat pada
	 * {@link ais.database.model.akunting.Transaksi#getRandomValue()} dan terverifikasi ada
	 * di sini juga:</b> properti bertipe {@link Long} skalar ini dianotasi
	 * <code>&#64;JoinColumn</code> alih-alih <code>&#64;Column</code>. Anotasi relasi pada
	 * properti skalar bukan pemetaan yang sah, sehingga nama kolom yang diminta
	 * (<code>random_value</code>) belum tentu dipakai Hibernate. Kedua berkas memuat blok
	 * kode yang persis sama, termasuk <code>System.out.println</code>-nya.</p>
	 *
	 * <p><b>Kelemahan mutu acak:</b> {@code (long) (Long.MAX_VALUE * nextDouble())}
	 * kehilangan presisi karena hasil perkalian melewati batas presisi
	 * <code>double</code> (53 bit mantissa) — bit-bit rendah nilai yang dihasilkan
	 * praktis selalu nol. Nilai ini tidak boleh diandalkan sebagai pengenal unik yang
	 * sesungguhnya, apalagi sebagai token keamanan.</p>
	 *
	 * <p>Tidak ada pembaca nilai ini di luar kelas ini dan kembarannya.</p>
	 *
	 * @return nilai acak pembeda baris; tidak pernah <code>null</code> setelah pemanggilan
	 *         pertama
	 */
	@JoinColumn(name = "random_value", nullable = true)
	public Long getRandomValue() {
		if (randomValue == null) {
			Random randomGenerator = new Random();
			long fraction = (long) (Long.MAX_VALUE * randomGenerator.nextDouble());
			randomValue = fraction;
			System.out.println("randomValue = " + randomValue);
		}
		return randomValue;
	}

	/**
	 * Menyetel nilai acak pembeda baris.
	 *
	 * <p>Dipakai Hibernate saat memuat baris dari basis data; menyetelnya secara eksplisit
	 * dari kode aplikasi mencegah pembangkitan otomatis pada {@link #getRandomValue()}.</p>
	 *
	 * @param randomValue nilai acak pembeda baris
	 */
	public void setRandomValue(Long randomValue) {
		this.randomValue = randomValue;
	}

	/**
	 * Mengembalikan cap batch posting baris template ini (kolom
	 * <code>posting_history</code>).
	 *
	 * <p>{@link PostingHistory} adalah entity "cap posting" murni yang dipakai bersama oleh
	 * banyak dokumen: keberadaan cap — bukan isinya — menjadi penentu
	 * {@link #getStatusPosting()} dan {@link #getTanggalPosting()}. Karena kedua getter itu
	 * memanggil method ini, cap posting menjadi simpul kendali kedua getter destruktif
	 * tersebut.</p>
	 *
	 * <p>Cascade PERSIST/MERGE berarti menyimpan baris template ikut menyimpan cap yang
	 * belum tersimpan. Tidak ada resolusi proxy <code>check(...)</code>.</p>
	 *
	 * @return cap batch posting, atau <code>null</code> bila baris belum diposting
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "posting_history", nullable = true)
	public PostingHistory getPostingHistory() {
		return postingHistory;
	}

	/**
	 * Menyetel cap batch posting baris template ini.
	 *
	 * <p><b>Efek samping tak langsung:</b> menyetel atau melepas cap di sini langsung
	 * mengubah hasil {@link #getStatusPosting()} dan dapat membuat
	 * {@link #getTanggalPosting()} mengosongkan kolom <code>tanggal_posting</code> pada
	 * pembacaan berikutnya.</p>
	 *
	 * @param postingHistory cap batch posting, boleh <code>null</code> untuk melepasnya
	 */
	public void setPostingHistory(PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

	/**
	 * Mengembalikan jenis/kode seri jurnal baris template ini (kolom
	 * <code>jenis_transaksi</code>).
	 *
	 * <p><b>Efek samping (getter penulis — pewarisan dari header):</b> bila field masih
	 * <code>null</code> <i>dan</i> {@link #getTemplateGrupTransaksi() header} punya jenis
	 * transaksi, nilai header <b>dituliskan</b> ke field baris ini. Berbeda dari
	 * {@link #getTanggalTransaksi()} yang selalu menimpa, di sini header hanya mengisi
	 * kekosongan — nilai baris yang sudah ada dipertahankan.</p>
	 *
	 * <p>Perhatikan bahwa method membaca field <code>grupTransaksi</code> secara
	 * <b>langsung</b>, bukan lewat {@link #getTemplateGrupTransaksi()}. Bila header berupa
	 * proxy lazy yang belum ter-resolve, <code>grupTransaksi.getJenisTransaksi()</code>
	 * tetap memicu inisialisasi proxy — dan akan melempar
	 * <code>LazyInitializationException</code> bila sesi Hibernate sudah tertutup.</p>
	 *
	 * <p>{@link JenisTransaksi} adalah katalog kode/nomor seri jurnal yang dibuat operator
	 * (PB/JBI/JCO dan sejenisnya), bukan klasifikasi Umum/Penerimaan/Pengeluaran — sumbu
	 * itu ada pada {@link #getJenisJurnal()}.</p>
	 *
	 * @return jenis transaksi baris ini, diwarisi dari header bila baris belum punya; dapat
	 *         <code>null</code> bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenis_transaksi", nullable = true)
	public JenisTransaksi getJenisTransaksi() {
		if (jenisTransaksi == null && grupTransaksi != null && grupTransaksi.getJenisTransaksi() != null) {
			jenisTransaksi = grupTransaksi.getJenisTransaksi();
		}
		return jenisTransaksi;
	}

	/**
	 * Menyetel jenis/kode seri jurnal baris template ini.
	 *
	 * <p>Menyetel nilai bukan-<code>null</code> di sini mematikan pewarisan dari header
	 * pada {@link #getJenisTransaksi()}.</p>
	 *
	 * @param jenisTransaksi jenis transaksi baris, boleh <code>null</code>
	 */
	public void setJenisTransaksi(JenisTransaksi jenisTransaksi) {
		this.jenisTransaksi = jenisTransaksi;
	}

}
