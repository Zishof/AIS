package ais.database.model.akunting;

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
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.surat.NomorSurat;

/**
 * <h2>JenisTransaksi — katalog master JENIS/BUKU JURNAL pada mesin akuntansi</h2>
 *
 * <p><b>Apa ini sebenarnya.</b> Meskipun namanya "Jenis Transaksi", label yang dilihat
 * operator di layar master ini adalah <i>"Kode Jenis Jurnal"</i> dan <i>"Nama Jenis
 * Jurnal"</i> (lihat {@code JenisTransaksiAction.init(JenisTransaksi)}). Jadi entity ini
 * adalah <b>katalog kategori jurnal akuntansi</b>: satu baris = satu jenis buku jurnal
 * seperti "Jurnal Penerimaan Kas", "Jurnal Pengeluaran Bank", "Jurnal Memorial",
 * "Jurnal Penyesuaian". Ia dipakai untuk (1) mengklasifikasikan setiap kepala jurnal
 * {@code ais.database.model.akunting.GrupTransaksi}, (2) menentukan <b>seri penomoran</b>
 * dokumen jurnal, dan (3) menyimpan <b>akun default</b> yang dipakai mesin posting untuk
 * menebak jenis jurnal secara otomatis.</p>
 *
 * <p><b>PENTING — jangan tertukar dengan {@code jenisJurnal}.</b> Ada DUA sumbu
 * klasifikasi jurnal yang berbeda dan saling ortogonal di modul ini:
 * <ul>
 *   <li><b>Sumbu 1 — entity ini</b> ({@code grup_transaksi.jenis_transaksi}, FK ke tabel
 *       {@code akunting.jenis_transaksi}): katalog yang dikelola operator, isinya bebas
 *       ditambah/diubah/dihapus lewat layar master.</li>
 *   <li><b>Sumbu 2 — kolom string {@code jenisJurnal}</b> pada
 *       {@code ais.database.model.akunting.Transaksi} /
 *       {@code ais.database.model.akunting.GrupTransaksi}: nilai HARDCODE dari konstanta
 *       {@code Transaksi.JURNAL_KAS_MASUK} ("Kas Masuk"),
 *       {@code Transaksi.JURNAL_KAS_KELUAR} ("Kas Keluar"),
 *       {@code Transaksi.JURNAL_UMUM} ("Umum") dan
 *       {@code Transaksi.JURNAL_TRANSAKSI} ("Transaksi") — inilah yang memisahkan layar
 *       Jurnal Umum / Jurnal Penerimaan / Jurnal Pengeluaran, BUKAN entity ini.</li>
 * </ul>
 * Artinya "Jurnal Umum"/"Jurnal Penerimaan"/"Jurnal Pengeluaran" sebagai <i>menu</i>
 * berasal dari sumbu 2, sedangkan baris-baris entity ini adalah rincian yang lebih halus
 * di dalamnya (mis. di layar Jurnal Umum, combo "Jenis Transaksi *" berisi baris-baris
 * entity INI).</p>
 *
 * <h3>Isi katalog: 8 baris yang di-<i>seed</i> otomatis</h3>
 *
 * <p>Katalog ini TIDAK diisi lewat skrip SQL instalasi, melainkan <b>ditulis diam-diam
 * dari jalur render</b>: {@code GrupTransaksiAction.doAfterCompose} menghitung
 * {@code rowCount()} tabel ini dan, bila NOL, langsung menyimpan 8 baris berikut
 * (kode, nama):</p>
 * <ol>
 *   <li>{@code PB}  — Penerimaan Bank</li>
 *   <li>{@code JBI} — Jurnal Penerimaan Bank</li>
 *   <li>{@code JCO} — Jurnal Pengeluaran Kas</li>
 *   <li>{@code JMM} — Jurnal Memorial</li>
 *   <li>{@code JBO} — Jurnal Pengeluaran Bank</li>
 *   <li>{@code JCI} — Jurnal Penerimaan Kas</li>
 *   <li>{@code AJE} — Jurnal Penyesuaian</li>
 *   <li>{@code GJP} — General Jurnal Voucer</li>
 * </ol>
 * <p>Karena disimpan berurutan pada tabel kosong ber-{@code IDENTITY}, urutan itu
 * biasanya menghasilkan {@code id} 1..8 — dan beberapa dasbor MENGANDALKAN angka itu
 * (lihat "Ketergantungan tersembunyi" di bawah). Perhatikan juga bahwa {@code keterangan}
 * hasil seed dibentuk sebagai {@code kode + " " + barisMentah}, sehingga isinya menjadi
 * teks rusak seperti {@code "AJE AJE,Jurnal Penyesuaian"}; nilai itu bukan sekadar
 * kosmetik karena {@code keterangan} yang dicetak sebagai kolom "Jenis" pada Bukti
 * Pengeluaran Kas ({@code NewUiJournalService.receipt} dan
 * {@code LaporanBuktiPengeluaranKas}).</p>
 *
 * <h3>Peran 1 — klasifikasi kepala jurnal</h3>
 *
 * <p>Relasi TERVERIFIKASI ke {@code ais.database.model.akunting.GrupTransaksi}: kelas itu
 * mendeklarasikan {@code private JenisTransaksi jenisTransaksi} (kolom
 * {@code jenis_transaksi}) dengan getter yang <b>jatuh ke {@link #DEFAULT}</b> bila kolom
 * kosong. Artinya jurnal yang di database berkolom NULL tetap "terlihat" bertipe default
 * di layar dan laporan. Relasi serupa ada pada {@code TemplateGrupTransaksi} dan
 * {@code TemplateTransaksi} (yang bahkan mewarisi nilai dari {@code grupTransaksi} bila
 * kolomnya sendiri kosong), serta pada {@code Transaksi} (baris debit/kredit).</p>
 *
 * <h3>Peran 2 — seri penomoran dokumen jurnal</h3>
 *
 * <p>{@link #getNomorSurat()} menghubungkan tiap jenis jurnal ke satu template
 * {@code ais.database.model.surat.NomorSurat}.
 * {@code CommonAkunting.generateNoJurnal(JenisTransaksi, boolean)} bercabang di sini:</p>
 * <ul>
 *   <li>jenis {@code null} <b>atau</b> {@code nomorSurat} {@code null} &rarr; jatuh ke
 *       penomoran darurat berbasis {@code max(id)} tabel {@code grup_transaksi}
 *       (format tanggal + 8 digit), di bawah kunci {@code KODE_JURNAL_LOCK};</li>
 *   <li>ada {@code nomorSurat} &rarr; memakai index/format template, dan bila
 *       {@code tambah=true} menaikkan penghitung template.</li>
 * </ul>
 * <p>Jadi <b>mengosongkan kolom Nomor Surat sebuah baris di sini diam-diam memindahkan
 * seluruh jurnal bertipe itu ke skema penomoran yang sama sekali berbeda</b>. Perubahan
 * itu bisa dilakukan tanpa membuka form apa pun: kolom Nomor Surat dan Akun dapat diedit
 * INLINE langsung di grid master (lihat {@code JenisTransaksiAction.JenisTransaksiRenderer}),
 * satu klik langsung tersimpan.</p>
 * <p>Bila {@code nomorSurat} kosong, layar jurnal memakai jalur cadangan kedua:
 * {@code TransaksiJurnalUmumHelper.generateCode(jt.getKode(), ...)} — yaitu
 * {@link #getKode()} dipakai sebagai <b>prefiks nomor jurnal</b>. API JSON
 * {@code JurnalUmumApiHelper.buatKode} melakukan hal yang sama dan memakai {@code "JU"}
 * bila kode kosong.</p>
 *
 * <h3>Peran 3 — akun default &amp; penebakan jenis oleh mesin posting</h3>
 *
 * <p>{@link #getAkun()} adalah "pointer pembukuan" ke bagan akun
 * {@code ais.database.model.akunting.Akun}. {@code CommonAkunting} memakainya untuk
 * MENEBAK jenis jurnal sebuah posting otomatis: ia menelusuri seluruh isi katalog ini
 * dari cache ({@code ConstantValues.ambilBerdasarClass(JenisTransaksi.class)}) dan
 * memilih baris pertama yang {@code akun}-nya sama dengan akun debit, lalu akun kredit,
 * lalu akun piutang denda; bila tak ada yang cocok dipakai {@link #DEFAULT}. Konsekuensi:
 * <b>mengubah kolom Akun satu baris master di sini mengubah penomoran dan klasifikasi
 * jurnal yang akan datang untuk seluruh modul yang memposting lewat
 * {@code CommonAkunting}</b> (SPP, koperasi, payroll, apotek, SIRS, dst.), dan urutan
 * iterasi map cache menentukan pemenang bila dua baris memakai akun yang sama.</p>
 *
 * <h3>Ketergantungan tersembunyi pada NILAI data (bukan pada skema)</h3>
 * <ul>
 *   <li><b>ID angka di-hardcode.</b> {@code DasboardAkuntansi} dan
 *       {@code DasboardNeracaLajur} menulis SQL mentah
 *       {@code gt.jenis_transaksi = 7} untuk kolom "Jurnal Penyesuaian" dan
 *       {@code gt.jenis_transaksi = 9} untuk "Jurnal Penutup". Angka 7 memang cocok
 *       dengan baris seed ke-7 ({@code AJE} Jurnal Penyesuaian), tetapi <b>seed hanya
 *       menghasilkan 8 baris — id 9 tidak pernah dibuat</b>; kolom "Jurnal Penutup" pada
 *       neraca lajur baru terisi bila operator kebetulan menambah baris ke-9. Sebaliknya,
 *       instalasi yang menghapus/menambah baris master akan membuat kedua kolom dasbor
 *       itu mengklasifikasikan jurnal ke ember yang salah tanpa pesan error.</li>
 *   <li><b>Nama dipakai sebagai penanda perilaku.</b>
 *       {@code TransaksiJurnalUmumHelper} menampilkan/menyembunyikan kontrol jurnal
 *       penutup dengan {@code jt.getNama().toLowerCase().contains("utup")} — cocok
 *       substring pada NAMA. Mengganti nama "Jurnal Penutup" menjadi mis. "Closing
 *       Entry" mematikan fitur itu secara diam-diam.</li>
 * </ul>
 *
 * <h3>Cakupan tenant — FAIL-OPEN STRUKTURAL</h3>
 *
 * <p>Entity ini <b>tidak memiliki kolom {@code sekolah} maupun {@code yayasan} sama
 * sekali</b>. Tidak ada penyaring tenant di {@code JenisTransaksiAction.initCriteria},
 * tidak ada di {@code NewUiJournalService.options}, tidak ada di endpoint API
 * {@code jurnal_umum_jenis_transaksi}. Katalog ini de facto GLOBAL untuk seluruh
 * instalasi multi-tenant: setiap operator yang bisa membuka salah satu jalur di bawah
 * melihat dan dapat mengubah jenis jurnal milik semua tenant, dan perubahan seri
 * penomoran/akun default langsung berlaku lintas tenant. Pola fail-open yang sama sudah
 * tercatat pada beberapa master lain di repo ini.</p>
 *
 * <h3>Jalur akses &amp; pewarisan hak menu induk (DIVERIFIKASI ULANG)</h3>
 *
 * <p>Catatan batch sebelumnya (pada {@code Akun.java}) menyebut master ini "menumpang"
 * hak menu Setup Kode Akun. Verifikasi dari sisi master ini <b>mengoreksi dan memperkuat
 * sekaligus</b>: master ini PUNYA menu sendiri, tetapi menu itu bukan satu-satunya pintu.
 * Ada empat jalur berbeda:</p>
 * <ol>
 *   <li><b>Menu sendiri.</b> {@code MenuSnapshotData} baris
 *       {@code "36345555|400000000|600000005|Jenis Transaksi|/pages/master/akunting/jenis_transaksi.zul|...|0"}
 *       — jadi ADA entri menu "Jenis Transaksi" yang hak CRUD-nya dapat diberikan atau
 *       dicabut per peran.</li>
 *   <li><b>Tumpangan tab "Setup Kode Akun" (pewarisan hak menu induk).</b>
 *       {@code AkunAction.onJenisTransaksi} menyisipkan
 *       {@code MyInclude("/pages/master/akunting/jenis_transaksi.zul")} ke dalam sebuah
 *       tabpanel halaman {@code akun.zul} (menu {@code 3633} "Setup Kode Akun"; tab
 *       kedua, {@code banktab}, melakukan hal serupa untuk master Bank). Gerbang di
 *       {@code JenisTransaksiAction} memakai {@code CommonPrivilages.checkPrevilages(...)}
 *       tanpa argumen menu, yang meminta {@code Common.getCurrentMenu()}; implementasi
 *       {@code CommonMenuAccessHelper.getCurrentMenu()} mengambil atribut SESI
 *       {@code "currentMenu"} — atribut yang sudah diset ke menu <i>Setup Kode Akun</i>
 *       saat pengguna mengklik menu itu, dan TIDAK di-resolve ulang untuk halaman yang
 *       disisipkan. Akibatnya <b>hak CREATE/UPDATE/DELETE yang dievaluasi untuk master
 *       jenis jurnal adalah hak atas menu "Setup Kode Akun", bukan hak atas menu "Jenis
 *       Transaksi"</b>. Peran yang secara eksplisit TIDAK diberi menu "Jenis Transaksi"
 *       tetap memperoleh CRUD penuh atasnya — termasuk mengganti seri penomoran dan akun
 *       default seluruh jurnal — hanya dengan membuka tab di layar Setup Kode Akun.
 *       Kebalikannya juga berlaku: peran yang HANYA punya menu "Jenis Transaksi" tetap
 *       aman, karena pada jalur itu {@code currentMenu} memang menunjuk menu yang benar.
 *       Catatan menenangkan: {@code checkPrevilages} FAIL-CLOSED bila menu {@code null}
 *       (mengembalikan {@code false}), jadi kebocorannya adalah salah-menu, bukan
 *       tanpa-menu.</li>
 *   <li><b>CRUD generik "baru".</b> {@code webapp/WEB-INF/baru/modul/}
 *       {@code pagesmasterakuntingjenistransaksizul/index.jsp} memanggil
 *       {@code DynamicJspCrudGenerator.generate(JenisTransaksi.class)} — generator CRUD
 *       reflektif. Berkas generator itu (3069 baris) TIDAK memanggil
 *       {@code checkPrevilages} sama sekali; gerbangnya hanya autentikasi Spring Security
 *       tingkat {@code /*}, bukan otorisasi per menu.</li>
 *   <li><b>API JSON.</b> {@code JurnalUmumApiHelper} aksi
 *       {@code "jurnal_umum_jenis_transaksi"} membaca seluruh katalog (id, kode, nama,
 *       kode/nama akun) tanpa penyaring tenant. Baca-saja, tetapi id yang dibocorkannya
 *       adalah id yang dipakai {@code importCsv} untuk membentuk jurnal.</li>
 * </ol>
 *
 * <h3>Getter dengan efek tulis-balik</h3>
 *
 * <p>{@link #getNomorSurat()} dan {@link #getAkun()} menugaskan ulang field-nya sendiri
 * ({@code akun = check(akun)}). Karena kelas ini dipetakan lewat PROPERTY access dan
 * memakai {@code dynamicUpdate=true}, penugasan pada jalur baca itu layak dicurigai.
 * Verifikasi: {@code GeneralValueObject.check(T)} mengembalikan instance KANONIK untuk id
 * yang sama (de-proxy / {@code EntityIdentityMap}), bukan objek lain — jadi berbeda dari
 * getter destruktif {@code Transaksi.getAkun()} yang menimpa {@code akun} dengan
 * {@code akunOver}. Di sini nilai bisnisnya tidak berubah; risikonya terbatas pada
 * pengetatan identitas objek, bukan pemindahan atribusi akun.</p>
 *
 * <h3>Warisan kelas</h3>
 *
 * <p>Kelas ini {@code extends} {@link ais.database.model.GeneralValueObject}. Base class
 * itu <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} — ia POJO abstrak
 * biasa, sehingga Hibernate TIDAK memetakan propertinya. Karena itu field audit
 * {@code oleh}, {@code olehId} dan {@code tanggal_dirubah} <b>wajib</b> dideklarasikan
 * ulang di sini; itu bukan duplikasi yang bisa dibersihkan, melainkan keharusan teknis.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.akunting.GrupTransaksi
 * @see ais.database.model.akunting.Transaksi
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.surat.NomorSurat
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "akunting", name = "jenis_transaksi")
public class JenisTransaksi extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Instance entity ini ikut tersimpan di sesi ZK (mis. sebagai {@code value} item
	 * combobox pada layar jurnal), sehingga kelas harus tetap serializable dan nilai ini
	 * tidak boleh diubah tanpa alasan.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama; lihat {@link #getId()}. */
	private Long id;

	/** Nama/identitas pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;

	/** ID pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir menyimpan baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()},
	 * bukan oleh layar master.</p>
	 *
	 * @return ID pengguna terakhir, atau {@code null} bila baris belum pernah tersentuh
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini.
	 *
	 * <p><b>Perhatikan:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} atau
	 * string kosong/spasi — ia langsung {@code return} tanpa mengubah apa pun. Jadi nilai
	 * lama TIDAK dapat dihapus lewat setter ini; sekali terisi, jejak audit hanya bisa
	 * ditimpa oleh nilai lain yang tidak kosong. Perilaku ini disengaja agar
	 * {@code AuditTimestampInterceptor} tidak menghapus jejak saat konteks pengguna tidak
	 * tersedia (mis. proses latar belakang).</p>
	 *
	 * @param olehId ID pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Representasi teks baris master ini, dipakai luas oleh komponen ZK (label, item
	 * combobox jenis jurnal, tooltip) dan oleh ekspor data.
	 *
	 * <p><b>Dua kuirk yang perlu diketahui:</b></p>
	 * <ol>
	 *   <li>Method ini membaca <b>field</b> {@code nama} secara langsung, bukan
	 *       {@link #getNama()}. Karena kelas ini dipetakan lewat property access, proxy
	 *       Hibernate yang belum ter-inisialisasi hanya mencegat GETTER; membaca field
	 *       langsung pada proxy menghasilkan {@code null}. Jadi {@code toString()} pada
	 *       referensi lazy yang belum tersentuh dapat mengembalikan {@code null}
	 *       alih-alih nama jenis jurnal.</li>
	 *   <li>Nilai dikembalikan apa adanya (tidak di-{@code trim()}), berbeda dengan
	 *       {@link #getNama()}.</li>
	 * </ol>
	 *
	 * @return nama jenis jurnal apa adanya, berpotensi {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama/identitas pengguna yang mengubah baris ini.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null} atau kosong
	 * <b>diabaikan diam-diam</b> sehingga jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan tanpa error
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pengisian jejak audit
	 * ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum setiap UPDATE.
	 *
	 * <p>Ini adalah implementasi dari satu-satunya method {@code abstract} milik
	 * {@link ais.database.model.GeneralValueObject}.</p>
	 *
	 * <p><b>Catatan gaya:</b> deklarasi method ini dan deklarasi field
	 * {@code tanggal_dirubah} sengaja ditulis pada SATU baris fisik (hasil penyisipan
	 * otomatis lintas-entity). Jangan memecahnya menjadi beberapa baris tanpa alasan —
	 * bentuk satu baris itu dipakai konsisten di seluruh paket model. Perhatikan juga
	 * bahwa field diinisialisasi <b>eager</b> ke waktu konstruksi objek
	 * ({@code WaktuUtil.getDate()}), sehingga baris yang belum pernah di-UPDATE tetap
	 * membawa stempel waktu — yaitu saat instance dibuat, bukan saat data dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Normalnya dipanggil oleh {@code AuditTimestampInterceptor} dari
	 * {@link #onUpdate()}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris master ini.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada instance yang baru
	 *         dibuat di JVM ini karena field-nya diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Cache statis global berisi baris master yang ditandai {@code defaultItem = true}.
	 *
	 * <p><b>Siapa yang memakainya.</b> Ini bukan sekadar kenyamanan UI — nilai ini adalah
	 * jaring pengaman mesin akuntansi:</p>
	 * <ul>
	 *   <li>{@code GrupTransaksi.getJenisTransaksi()} mengembalikan {@code DEFAULT} bila
	 *       kolom jurnal kosong, sehingga jurnal ber-kolom NULL "terlihat" bertipe
	 *       default di layar dan laporan padahal di database tidak begitu;</li>
	 *   <li>{@code CommonAkunting} memakai {@code generateNoJurnal(DEFAULT, true)} untuk
	 *       memberi nomor jurnal hasil posting otomatis yang akunnya tidak cocok dengan
	 *       baris master mana pun.</li>
	 * </ul>
	 *
	 * <p><b>Risiko yang melekat.</b> Field ini {@code public static} dan tidak
	 * {@code final} maupun {@code volatile}: satu nilai untuk SELURUH JVM (jadi juga
	 * lintas tenant, sejalan dengan ketiadaan kolom tenant pada entity ini), dapat ditulis
	 * dari mana saja, dan pembaruan oleh satu thread tidak dijamin terlihat oleh thread
	 * lain tanpa sinkronisasi. Nilainya berupa entity yang diambil dari sesi yang sudah
	 * ditutup ({@link #reinitDefault()}), sehingga relasi lazy-nya hanya aman diakses
	 * lewat {@link #getAkun()}/{@link #getNomorSurat()} yang melakukan de-proxy via cache.
	 * Nilai {@code null} adalah kondisi normal — mis. sebelum {@code InitData} selesai,
	 * atau bila tidak ada satu pun baris ber-{@code defaultItem}.</p>
	 */
	public static JenisTransaksi DEFAULT = null;

	/**
	 * Memuat ulang cache {@link #DEFAULT} dari database.
	 *
	 * <p><b>Kapan dipanggil.</b> (1) Saat boot aplikasi, dari task paralel "TASK-A" di
	 * {@code InitData.doInitData()}; (2) setelah setiap penyimpanan di layar master,
	 * lewat {@code Common.createDefaultTimer} pada {@code JenisTransaksiAction.onSave};
	 * (3) setelah checkbox "Aktif" diubah inline di grid master.</p>
	 *
	 * <p><b>Cara kerja.</b> Membuka session BARU lewat {@code openSession()} — sengaja
	 * terpisah dari session HTTP request — lalu mencari lewat
	 * {@code ConstantValues.simpleObject} satu baris dengan {@code defaultItem = true},
	 * dibatasi {@code setMaxResults(1)} dan diurutkan {@code id} menurun (jadi bila
	 * ada lebih dari satu baris default — kondisi yang seharusnya dicegah oleh
	 * {@code onSave} lewat {@code UPDATE ... SET defaultitem = false} global — yang
	 * menang adalah id TERBESAR, yaitu yang paling baru dibuat). {@code simpleObject}
	 * hanya memproyeksikan {@code id} lalu mengambil instance dari cache memori
	 * {@code ConstantValues.ambil}, sehingga hasilnya bisa {@code null} bila cache
	 * entity belum hangat meski barisnya ada di database.</p>
	 *
	 * <p><b>Efek samping yang perlu diwaspadai.</b> Blok {@code finally} menutup session
	 * yang dibuka di sini, TETAPI juga memanggil {@code HibernateUtil.closeSession()} —
	 * dan method itu mengambil serta menutup session THREAD-LOCAL, yaitu session milik
	 * request yang sedang berjalan, bukan session yang dibuka method ini. Pada pemanggilan
	 * dari timer ZK setelah simpan, efeknya adalah session request pengguna ikut ditutup
	 * (transaksi di-rollback, koneksi dilepas). Kode setelahnya yang mengandalkan
	 * {@code HibernateUtil.currentSession()} akan mendapat session baru.</p>
	 *
	 * <p><b>Penanganan error.</b> Semua {@code Exception} ditelan (hanya dicetak dan
	 * direkam {@code ErrorAuditUtil}); bila gagal, {@link #DEFAULT} tetap memegang nilai
	 * lamanya — jadi kegagalan muat ulang tidak pernah terlihat oleh pengguna.</p>
	 */
	public static void reinitDefault() {
		// Menggunakan openSession() agar terpisah dari session HTTP request
		Session session = HibernateUtil.getSessionFactory().openSession();

		try {
			DEFAULT = (JenisTransaksi) ConstantValues.simpleObject(session.createCriteria(JenisTransaksi.class)
					.add(Restrictions.eq("defaultItem", true)).setMaxResults(1).addOrder(Order.desc("id")),
					JenisTransaksi.class);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/akunting/JenisTransaksi.java:96");
		} finally {
			// 2. WAJIB Tutup Session
			if (session != null && session.isOpen()) {
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			}
			HibernateUtil.closeSession();
		}
	}

	/** Kode singkat jenis jurnal (mis. {@code AJE}); lihat {@link #getKode()}. */
	private String kode;

	/** Nama jenis jurnal yang tampil di layar dan laporan; lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan bebas, ikut tercetak di bukti kas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Penanda baris default katalog. Sengaja diinisialisasi {@code false} agar baris baru
	 * tidak pernah lahir sebagai default; lihat {@link #getDefaultItem()}.
	 */
	private Boolean defaultItem = false;

	/** Akun default untuk penebakan jenis oleh mesin posting; lihat {@link #getAkun()}. */
	private Akun akun;

	/** Penanda aktif/nonaktif katalog; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Template penomoran dokumen jurnal; lihat {@link #getNomorSurat()}. */
	private NomorSurat nomorSurat;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate dan dipakai layar master saat
	 * menekan tombol "Tambah" ({@code JenisTransaksiAction.onAdd}).
	 *
	 * <p>Instance yang dihasilkan memiliki {@code defaultItem = false} dan
	 * {@code tanggal_dirubah} berisi waktu saat ini; seluruh field lain {@code null}
	 * (termasuk {@code aktif}, yang oleh {@link #getAktif()} dibaca sebagai
	 * {@code true}).</p>
	 */
	public JenisTransaksi() {
	}

	/**
	 * Kunci utama baris master, dibangkitkan database ({@code IDENTITY}).
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya berasal dari sekuens
	 * database, bukan dari aplikasi. Nilai id inilah yang disimpan pada kolom
	 * {@code grup_transaksi.jenis_transaksi}, dibocorkan oleh endpoint API
	 * {@code jurnal_umum_jenis_transaksi}, dipakai sebagai {@code typeId} oleh
	 * {@code NewUiJournalService}/{@code importCsv}, dan — sayangnya — di-hardcode
	 * sebagai angka 7 dan 9 pada SQL dasbor akuntansi (lihat Javadoc kelas).</p>
	 *
	 * @return id baris, atau {@code null} untuk objek yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama.
	 *
	 * <p>Hanya untuk Hibernate dan kode migrasi/impor. Jangan dipanggil dari layar.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama jenis jurnal — kolom wajib ({@code nullable = false}), panjang maksimum 255.
	 *
	 * <p>Nilai ini muncul di combo "Jenis Transaksi *" pada layar jurnal, di kolom grid
	 * master, di label layar Jurnal Penerimaan/Pengeluaran, dan pada baris hasil
	 * {@code NewUiJournalService}. Getter melakukan {@code trim()} pada saat BACA, tetapi
	 * {@link #setNama(String)} menyimpan apa adanya — jadi spasi tepi tetap tersimpan di
	 * database dan hanya tersembunyi di jalur getter (bandingkan {@link #toString()} yang
	 * tidak melakukan trim).</p>
	 *
	 * <p><b>Ketergantungan perilaku:</b> {@code TransaksiJurnalUmumHelper} mendeteksi
	 * jurnal penutup dengan memeriksa apakah nama (huruf kecil) memuat substring
	 * {@code "utup"}. Nama bukan sekadar label.</p>
	 *
	 * @return nama jenis jurnal yang sudah di-trim, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama jenis jurnal.
	 *
	 * <p>Nilai disimpan apa adanya tanpa {@code trim()} dan tanpa validasi kosong;
	 * validasi "Nama Jenis Jurnal belum diisi" dilakukan di
	 * {@code JenisTransaksiAction.onSave}, bukan di sini.</p>
	 *
	 * @param nama nama jenis jurnal
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas jenis jurnal.
	 *
	 * <p>Getter mengembalikan string kosong (bukan {@code null}) bila kolom kosong,
	 * sehingga aman dipakai langsung sebagai label ZK. Nilai ini <b>ikut tercetak</b>
	 * sebagai kolom "Jenis" pada Bukti Pengeluaran Kas
	 * ({@code NewUiJournalService.receipt}, {@code combinedReceipt}, dan
	 * {@code LaporanBuktiPengeluaranKas}) — bukan {@link #getNama()}. Karena baris hasil
	 * seed otomatis mengisi keterangan dengan teks rusak seperti
	 * {@code "AJE AJE,Jurnal Penyesuaian"}, instalasi yang tidak pernah merapikan katalog
	 * akan mencetak teks itu di dokumen resmi.</p>
	 *
	 * @return keterangan yang sudah di-trim, atau {@code ""} bila kosong; tidak pernah
	 *         {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menetapkan keterangan jenis jurnal.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menandakan apakah baris ini adalah jenis jurnal default katalog.
	 *
	 * <p>Getter bersifat null-safe: kolom {@code null} dibaca sebagai {@code false}.
	 * Tidak ada anotasi {@code @Column}, sehingga Hibernate memakai nama properti sebagai
	 * nama kolom ({@code defaultItem}); PostgreSQL melipat identifier tak-berkutip menjadi
	 * huruf kecil, itulah sebabnya SQL mentah di {@code JenisTransaksiAction.onSave}
	 * menulis {@code defaultitem}.</p>
	 *
	 * <p><b>Keunikan tidak dijamin database.</b> Tidak ada indeks unik parsial; keunikan
	 * "hanya satu default" ditegakkan secara prosedural di {@code onSave} dengan
	 * {@code UPDATE akunting.jenis_transaksi SET defaultitem = false} untuk SELURUH tabel
	 * (global, lintas tenant) sebelum menyimpan yang baru — dan UPDATE itu berjalan di
	 * luar penjagaan transaksi terhadap kegagalan simpan berikutnya. Bila sampai ada dua
	 * baris default, {@link #reinitDefault()} memilih yang id-nya terbesar. Perlu dicatat
	 * pula bahwa baris "Default" pada form master di-{@code setVisible(false)}, jadi flag
	 * ini praktis tidak dapat dinyalakan dari UI saat ini dan setiap penyimpanan justru
	 * menuliskan {@code false} dari checkbox yang tersembunyi.</p>
	 *
	 * @return {@code true} bila baris ini default; {@code false} bila tidak atau kolom
	 *         {@code null}
	 */
	public Boolean getDefaultItem() {
		return defaultItem == null ? false : defaultItem;
	}

	/**
	 * Menetapkan penanda default.
	 *
	 * @param defaultItem {@code true} untuk menjadikan baris ini default katalog
	 */
	public void setDefaultItem(Boolean defaultItem) {
		this.defaultItem = defaultItem;
	}

	/**
	 * Kode singkat jenis jurnal (mis. {@code AJE}, {@code JCI}, {@code GJP}).
	 *
	 * <p>Berbeda dengan {@link #getNama()} dan {@link #getKeterangan()}, getter ini
	 * <b>tidak</b> melakukan {@code trim()} maupun null-guard. Kode dipakai untuk:</p>
	 * <ul>
	 *   <li>prefiks nomor jurnal bila {@link #getNomorSurat()} kosong
	 *       ({@code TransaksiJurnalUmumHelper.generateCode}, dan
	 *       {@code JurnalUmumApiHelper.buatKode} yang memakai {@code "JU"} sebagai
	 *       cadangan);</li>
	 *   <li>nilai kolom "sifat" dan potongan nomor pada dokumen SPP/SPTB
	 *       ({@code LaporanAkuntingHelper}, {@code LaporanSPP}, {@code LaporanSPTB});</li>
	 *   <li>kunci tampilan riwayat revisi Envers di grid master
	 *       ({@code RevisiHelper.createNewRevisi(..., getKode())}).</li>
	 * </ul>
	 *
	 * <p><b>Kuirk validasi:</b> {@code JenisTransaksiAction.checkKodeJenisTransaksi()}
	 * mencari duplikat dengan {@code Restrictions.eq("kode", kode.getValue().trim())},
	 * sedangkan {@code onSave} menyimpan lewat {@code setKode(kode.getValue())} TANPA
	 * trim. Akibatnya kode {@code "AJE "} (berspasi) lolos pemeriksaan duplikat terhadap
	 * {@code "AJE"} yang sudah ada, lalu tersimpan sebagai baris kedua — menghasilkan dua
	 * jenis jurnal yang terlihat identik di layar dan dua prefiks nomor jurnal yang
	 * berbeda secara byte.</p>
	 *
	 * @return kode jenis jurnal apa adanya (tidak di-trim), berpotensi {@code null}
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan kode jenis jurnal.
	 *
	 * <p>Disimpan apa adanya; lihat catatan kuirk trim pada {@link #getKode()}.</p>
	 *
	 * @param kode kode singkat jenis jurnal
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Template penomoran dokumen ({@code ais.database.model.surat.NomorSurat}) yang
	 * dipakai untuk menerbitkan nomor jurnal bagi seluruh kepala jurnal bertipe ini.
	 *
	 * <p>Relasi {@code ManyToOne} lazy, kolom {@code nomor_surat}, opsional, dengan
	 * cascade {@code PERSIST}/{@code MERGE} — artinya menyimpan baris master ini dapat
	 * ikut mem-persist/merge objek {@code NomorSurat} yang tertaut.</p>
	 *
	 * <p><b>Efek pada penomoran.</b> {@code CommonAkunting.generateNoJurnal} bercabang
	 * pada null-tidaknya nilai ini: ada &rarr; format/index template (dan penghitung
	 * template dinaikkan bila {@code tambah=true}); kosong &rarr; penomoran darurat
	 * berbasis {@code max(id)} tabel {@code grup_transaksi}. {@code CommonAkunting.getindex}
	 * bahkan mem-{@code createAlias} rantai {@code jenisTransaksi.nomorSurat} untuk
	 * menghitung urutan berjalan, jadi relasi ini adalah bagian dari kunci pengurutan
	 * nomor, bukan sekadar atribut tampilan.</p>
	 *
	 * <p><b>Getter dengan tulis-balik.</b> Field ditugaskan ulang dengan hasil
	 * {@code check(...)} (de-proxy ke instance kanonik). Nilai bisnis tidak berubah — ini
	 * varian jinak dari pola getter tulis-balik, bukan getter destruktif seperti
	 * {@code Transaksi.getAkun()}.</p>
	 *
	 * @return template nomor dokumen yang tertaut, atau {@code null} bila jenis jurnal ini
	 *         tidak memakai seri penomoran khusus
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "nomor_surat", nullable = true)
	public NomorSurat getNomorSurat() {
		nomorSurat = check(nomorSurat);
		return nomorSurat;
	}

	/**
	 * Menetapkan template penomoran dokumen jurnal.
	 *
	 * <p><b>Dipanggil dari mana:</b> selain Hibernate, dari listener banbox pada
	 * {@code JenisTransaksiAction.JenisTransaksiRenderer} — pemilihan nomor surat di grid
	 * langsung diikuti {@code Common.refreshUpdate(jenisTransaksi)} dan pemuatan ulang
	 * cache {@code NomorSuratAlurKeuangan.reloadDefault()}. Tidak ada dialog konfirmasi,
	 * padahal perubahan ini mengganti skema nomor seluruh jurnal bertipe ini ke depan.</p>
	 *
	 * @param nomorSurat template penomoran; {@code null} berarti memakai penomoran darurat
	 */
	public void setNomorSurat(NomorSurat nomorSurat) {
		this.nomorSurat = nomorSurat;
	}

	/**
	 * Akun buku besar default yang mewakili jenis jurnal ini.
	 *
	 * <p>Relasi {@code ManyToOne} lazy ke {@code ais.database.model.akunting.Akun}, kolom
	 * {@code akun}, opsional, cascade {@code PERSIST}/{@code MERGE}.</p>
	 *
	 * <p><b>Bukan sekadar informasi.</b> {@code CommonAkunting} memakai kolom ini sebagai
	 * TABEL PENCARIAN BALIK saat memposting dokumen non-jurnal menjadi jurnal: ia
	 * menelusuri seluruh isi katalog dari cache memori dan memilih baris pertama yang
	 * akunnya sama dengan akun debit — lalu akun kredit, lalu akun piutang denda — untuk
	 * menentukan jenis jurnal DAN nomor jurnal dokumen tersebut. Bila tidak ada yang
	 * cocok, dipakai {@link #DEFAULT}. Dua konsekuensi praktis: (1) mengisi/mengubah akun
	 * satu baris master di sini mengubah klasifikasi dan penomoran jurnal seluruh modul
	 * yang memposting lewat {@code CommonAkunting}; (2) bila dua baris master memakai akun
	 * yang sama, pemenangnya ditentukan oleh urutan iterasi map cache — tidak
	 * deterministik dari sudut pandang operator.</p>
	 *
	 * <p>Kolom ini juga menjadi kriteria pencarian di layar master
	 * ({@code initCriteria} mem-{@code createAlias("akun")} lalu menerapkan ILIKE pada
	 * {@code akun.nama} DAN {@code akun.kode} sekaligus — konjungsi, bukan disjungsi,
	 * sehingga satu kata kunci jarang cocok pada keduanya) dan ikut dibocorkan oleh
	 * endpoint API {@code jurnal_umum_jenis_transaksi}.</p>
	 *
	 * <p><b>Getter dengan tulis-balik:</b> sama seperti {@link #getNomorSurat()} — de-proxy
	 * ke instance kanonik, nilai bisnis tidak berubah.</p>
	 *
	 * @return akun default jenis jurnal ini, atau {@code null} bila tidak ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menetapkan akun buku besar default jenis jurnal ini.
	 *
	 * <p><b>Dipanggil dari mana:</b> selain Hibernate, dari listener banbox akun pada
	 * {@code JenisTransaksiAction.JenisTransaksiRenderer} — perubahan inline di grid
	 * langsung tersimpan lewat {@code Common.refreshUpdate}. Lihat {@link #getAkun()}
	 * untuk dampaknya pada mesin posting.</p>
	 *
	 * @param akun akun buku besar; boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Penanda apakah jenis jurnal ini masih boleh dipilih.
	 *
	 * <p>Getter <b>fail-open</b>: kolom {@code null} dibaca sebagai {@code true}. Ini
	 * konsisten dengan seluruh penyaring di repo yang selalu menulis
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} — dipakai di
	 * {@code JenisTransaksiAction.initCriteria}, di combo layar Jurnal Umum
	 * ({@code Common.insertComboDanSemua}), di {@code GrupTransaksiAction}, di
	 * {@code NewUiJournalService.options}, dan di SQL endpoint API
	 * ({@code COALESCE(j.aktif,true) = true}). Karena 8 baris hasil seed otomatis tidak
	 * pernah mengisi kolom ini, seluruhnya bernilai {@code null} di database dan tetap
	 * aktif.</p>
	 *
	 * <p><b>Menonaktifkan tidak menghapus jejak.</b> Menurunkan flag ini hanya
	 * menyembunyikan baris dari pilihan baru; jurnal lama yang sudah menunjuk ke sini
	 * tetap menampilkannya, dan {@link #reinitDefault()} tidak memeriksa {@code aktif}
	 * sama sekali — baris nonaktif yang kebetulan ber-{@code defaultItem} tetap bisa
	 * menjadi {@link #DEFAULT}.</p>
	 *
	 * @return {@code true} bila aktif atau kolom {@code null}; {@code false} hanya bila
	 *         dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif jenis jurnal.
	 *
	 * <p><b>Dipanggil dari mana:</b> checkbox "Aktif" inline pada grid master. Listener
	 * {@code onCheck} langsung memanggil {@code Common.refreshSaveOrUpdate(jenisTransaksi)}
	 * disusul timer yang menjalankan {@code NomorSuratAlurKeuangan.reloadDefault()} — satu
	 * klik, tanpa konfirmasi, langsung tersimpan.</p>
	 *
	 * @param aktif {@code true} untuk mengaktifkan, {@code false} untuk menyembunyikan
	 *              dari pilihan baru; {@code null} dibaca sebagai aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
