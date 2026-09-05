package ais.database.model.surat;

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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;

/**
 * <b>Entity JPA — baris tembusan (carbon copy) pada sebuah klasifikasi surat keluar.</b>
 *
 * <p>Kelas ini adalah tabel penghubung {@code surat.klasifikasi_surat_keluar_punya_tembusan} yang
 * memasangkan satu {@link KlasifikasiSuratKeluar} (jenis/klasifikasi surat, mis. "Surat Tugas",
 * "Surat Keterangan Aktif Kuliah") dengan satu {@link ais.database.model.employ.JenisJabatan} yang
 * harus menerima <i>tembusan</i> surat tersebut. Dengan kata lain: daftar "Tembusan:" yang lazim
 * tercetak di kaki surat resmi tidak disimpan sebagai teks bebas per-surat, melainkan
 * <b>dikonfigurasi sekali di tingkat klasifikasi</b> dan berlaku untuk seluruh
 * {@link SuratKeluar} yang memakai klasifikasi itu.</p>
 *
 * <h2>Posisi dalam model surat keluar</h2>
 * <p>Modul surat keluar memakai tiga tabel penghubung yang bentuknya nyaris kembar dan mudah
 * tertukar saat membaca kode:</p>
 * <ul>
 *   <li>{@link KlasifikasiSuratKeluarPunyaJenisJabatan} — jabatan <b>penanda tangan</b>; punya
 *   {@code posisiX}/{@code posisiY} untuk menempatkan blok tanda tangan pada layout cetak.</li>
 *   <li>{@code KlasifikasiSuratKeluarPunyaTembusan} (kelas ini) — jabatan <b>penerima tembusan</b>;
 *   tidak punya koordinat karena tembusan dirender sebagai daftar, bukan blok tanda tangan.</li>
 *   <li>{@link AlurPersetujuanSuratKeluar} beserta tabel {@code alur_punya_jenis_jabatan} — jabatan
 *   yang <b>menyetujui</b> surat secara berjenjang.</li>
 * </ul>
 * <p>Ketiganya menunjuk ke {@link ais.database.model.employ.JenisJabatan} yang sama, sehingga satu
 * jabatan bisa sekaligus menjadi penyetuju, penanda tangan, dan penerima tembusan. Tidak ada
 * batasan di level entity yang mencegah kombinasi tersebut; bila sebuah instansi menganggap
 * "penyetuju tidak boleh sekaligus penerima tembusan", aturan itu harus ditegakkan di lapisan
 * Action/UI karena entity ini tidak menegakkannya.</p>
 *
 * <h2>Kardinalitas dan arah relasi</h2>
 * <p>Relasi ke {@link KlasifikasiSuratKeluar} maupun ke {@link ais.database.model.employ.JenisJabatan}
 * sama-sama {@code @ManyToOne} dan sama-sama {@code nullable = true}. Konsekuensinya:</p>
 * <ul>
 *   <li>Satu klasifikasi boleh punya banyak baris tembusan (itulah tujuan tabel ini).</li>
 *   <li>Satu jabatan boleh menjadi tembusan bagi banyak klasifikasi.</li>
 *   <li><b>Tidak ada unique constraint gabungan</b> {@code (klasifikasi_surat_keluar, tembusan)} di
 *   level anotasi, sehingga jabatan yang sama dapat terdaftar dua kali sebagai tembusan untuk
 *   klasifikasi yang sama dan akan tercetak ganda pada surat. Deduplikasi harus dilakukan pemanggil
 *   (lihat {@code ais.action.master.surat.helper.KlasifikasiSuratKeluarPunyaTembusanHelper}).</li>
 *   <li>Karena kedua sisi boleh {@code null}, baris "yatim" (tanpa klasifikasi maupun tanpa jabatan)
 *   dapat tersimpan tanpa keluhan dari database; baris seperti itu diam-diam tidak berpengaruh
 *   apa-apa pada hasil cetak.</li>
 * </ul>
 *
 * <h2>Status pemakaian: entity tidur (dormant)</h2>
 * <p>Penelusuran seluruh pohon sumber {@code src/main/src} menunjukkan bahwa satu-satunya kode Java
 * di luar berkas entity ini yang menyentuh kelas ini adalah
 * {@code ais.action.master.surat.helper.KlasifikasiSuratKeluarPunyaTembusanHelper} — dan helper itu
 * <b>tidak pernah diinstansiasi dari Action, dashboard, atau utilitas mana pun</b>. Lebih jauh,
 * kata "tembusan" tidak muncul sama sekali di jalur cetak/laporan
 * ({@code ais.action.master.surat.util}, {@code ais.action.report}).</p>
 *
 * <p>Konsekuensinya:</p>
 * <ul>
 *   <li>Jalur cetak surat keluar yang benar-benar berjalan
 *   ({@link SuratKeluar#cetak(ais.database.model.Tbmuser)},
 *   {@code SuratKeluarAction.cetakDisposisi(...)}, {@code SuratUtil.ubahIsiSuratKeluar(...)})
 *   <b>tidak membaca tabel ini</b>. Daftar "Tembusan:" pada surat yang tercetak — bila ada —
 *   berasal dari isi template jrxml, bukan dari baris di tabel ini.</li>
 *   <li>Karena helper editornya tidak terpasang di UI, dalam kondisi kode saat ini baris di tabel
 *   ini juga tidak bertambah lewat aplikasi.</li>
 *   <li><b>Kualifikasi yang jujur:</b> template jrxml diunggah administrator (lihat
 *   {@code ais.database.model.file.LampiranLain}) dan bukan berkas repositori, sehingga secara
 *   teknis sebuah template dapat membaca tabel ini lewat query SQL-nya sendiri. Pernyataan "tidak
 *   dipakai" berlaku pasti untuk <b>kode Java</b> saja.</li>
 *   <li>Nasib yang sama berlaku untuk {@link KlasifikasiSuratKeluarPunyaJenisJabatan}. Sebelum
 *   menghapus kelas/tabel ini, periksa lebih dulu isi tabel di basis data produksi dan isi template
 *   jrxml yang terpasang.</li>
 * </ul>
 *
 * <h2>Basis data dan audit</h2>
 * <p>Tabel: skema {@code surat}, nama {@code klasifikasi_surat_keluar_punya_tembusan}. Kelas
 * memakai {@code dynamicInsert}/{@code dynamicUpdate} sehingga Hibernate hanya menulis kolom yang
 * benar-benar berubah, dan {@link org.hibernate.envers.Audited} sehingga seluruh perubahan
 * konfigurasi tembusan terekam di tabel revisi Envers — perubahan siapa yang menerima tembusan
 * surat resmi memang perlu jejak audit.</p>
 *
 * <p>Selain audit Envers, kelas ini membawa <b>field audit bayangan</b> {@code oleh},
 * {@code olehId}, dan {@code tanggal_dirubah} yang diisi oleh
 * {@link ais.database.hibernate.AuditTimestampInterceptor}. Duplikasi ini adalah <b>keharusan
 * teknis</b>, bukan cacat: tabel revisi Envers hanya bisa dibaca lewat API Envers, sementara
 * daftar/laporan ZK di aplikasi ini membaca kolom biasa lewat Criteria. Tanpa field bayangan,
 * kolom "Diubah oleh" pada grid tidak bisa ditampilkan tanpa query silang ke tabel revisi.</p>
 *
 * <h2>Pewarisan</h2>
 * <p>Meng-extend {@link GeneralValueObject}, sehingga mewarisi {@code check(...)} untuk resolusi
 * proxy lazy, {@code equals()} berbasis {@code id}, serta sejumlah field generik. Perhatikan
 * peringatan pada {@link GeneralValueObject}: {@code hashCode()} <b>tidak</b> di-override, jadi
 * jangan memakai {@code HashSet}/{@code HashMap} berkunci entity ini untuk deduplikasi tembusan;
 * pakai {@code Map<Long, ...>} berkunci id jabatan.</p>
 *
 * <h2>Catatan pembangkitan</h2>
 * Bank generated by hbm2java
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "klasifikasi_surat_keluar_punya_tembusan")
public class KlasifikasiSuratKeluarPunyaTembusan extends GeneralValueObject {

	/**
	 * 
	 * Versi serialisasi. Nilai {@code 2463821577548439808L} dipakai ulang secara identik oleh
	 * hampir seluruh entity di paket {@code ais.database.model.surat} (hasil salin-tempel dari
	 * template hbm2java yang sama). Karena nilainya sama, dua entity berbeda TIDAK bisa dibedakan
	 * lewat serialVersionUID; jangan pernah memakai nilai ini sebagai penanda tipe. Nilainya
	 * sengaja dipertahankan agar object yang sudah pernah diserialisasi (mis. ke dalam sesi ZK yang
	 * dipersistensi) tetap dapat dibaca kembali setelah kelas ini disunting.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris tembusan. Di-generate database ({@code IDENTITY}); bernilai {@code null}
	 * selama object belum pernah disimpan — kondisi {@code null} inilah yang dipakai
	 * {@link #getSatuanKerja()} untuk mendeteksi "baris baru" dan mengisi satuan kerja default.
	 */
	private Long id;
	/**
	 * Nama pengguna terakhir yang mengubah baris ini (field audit bayangan). Diisi otomatis oleh
	 * {@link ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}.
	 */
	private String oleh;
	/**
	 * Id/username pengguna terakhir yang mengubah baris ini (field audit bayangan, pasangan dari
	 * {@link #oleh}).
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang mengubah baris tembusan ini. Getter murni, tanpa
	 * normalisasi — nilai {@code null} berarti baris belum pernah melewati interceptor audit.
	 *
	 * @return id/username pengubah terakhir, atau {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pengguna pengubah terakhir, dengan <b>penjaga anti-penghapusan</b>: bila
	 * argumen {@code null} atau hanya berisi spasi, setter langsung {@code return} tanpa menulis
	 * apa pun sehingga nilai lama dipertahankan. Pola ini konsisten di seluruh entity AIS dan
	 * disengaja: jejak audit tidak boleh terhapus oleh pemanggil yang lalai mengisi konteks
	 * pengguna (mis. proses batch atau thread latar tanpa sesi ZK).
	 *
	 * @param olehId id/username pengubah; diabaikan bila kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, argumen
	 * kosong diabaikan agar nilai audit yang sudah ada tidak tertimpa nilai hampa.
	 *
	 * @param oleh nama pengubah; diabaikan bila kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris tembusan ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah tercatat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi tepat sebelum Hibernate menulis
	 * {@code UPDATE}. Hanya berjalan pada UPDATE, bukan INSERT: nilai awal {@code tanggal_dirubah}
	 * untuk baris baru berasal dari inisialisasi field di bawah.
	 *
	 * <p><b>Perhatian saat menyunting berkas ini:</b> deklarasi field {@code tanggal_dirubah}
	 * ditulis pada BARIS FISIK YANG SAMA dengan method ini (warisan pembangkit kode). Menyisipkan
	 * atau memindahkan teks di baris tersebut berisiko memisahkan keduanya secara tidak sengaja.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Umumnya dipanggil interceptor audit, bukan kode domain.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris tembusan ini. Dipetakan sebagai
	 * {@code TIMESTAMP} (tanggal + jam), bukan {@code DATE}.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris tembusan dalam bentuk {@code "<klasifikasi> - <jabatan tembusan>"}.
	 *
	 * <p><b>Penting:</b> method ini membaca <b>field</b> {@code klasifikasiSuratKeluar} dan
	 * {@code tembusan} secara langsung, bukan lewat getter-nya. Akibatnya {@code check(...)} tidak
	 * dipanggil, sehingga bila kedua relasi masih berupa proxy Hibernate yang belum ter-inisialisasi
	 * dan sesi sudah tertutup, penggabungan string di sini dapat melempar
	 * {@code LazyInitializationException} — bukan sekadar menghasilkan teks apa adanya. Pemanggil
	 * yang menampilkan object ini di grid ZK di luar sesi aktif sebaiknya memanggil
	 * {@link #getKlasifikasiSuratKeluar()} dan {@link #getTembusan()} lebih dulu.</p>
	 *
	 * @return label gabungan klasifikasi dan jabatan penerima tembusan.
	 */
	public String toString() {
		return klasifikasiSuratKeluar + " - " + tembusan;
	}

	/**
	 * Klasifikasi surat keluar yang memiliki baris tembusan ini (sisi "banyak" dari relasi).
	 */
	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;
	/**
	 * Jabatan penerima tembusan. Bertipe {@link ais.database.model.employ.JenisJabatan}, yaitu
	 * <b>jenis</b> jabatan (mis. "Wakil Rektor II"), bukan orang tertentu — pemetaan jabatan ke
	 * pejabat yang menjabat dilakukan terpisah lewat {@code ais.database.model.employ.Pejabat}.
	 * Konsekuensinya, konfigurasi tembusan tetap sahih ketika pejabatnya berganti.
	 */
	private JenisJabatan tembusan;
	/**
	 * Satuan kerja pemilik baris tembusan (penanda tenant/unit). Diisi otomatis untuk baris baru;
	 * lihat {@link #getSatuanKerja()}.
	 */
	private SatuanKerja satuanKerja;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Tidak melakukan inisialisasi apa pun selain default field.
	 */
	public KlasifikasiSuratKeluarPunyaTembusan() {
	}

	/**
	 * Mengembalikan kunci utama baris tembusan.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Praktis hanya dipakai Hibernate; kode domain tidak perlu memanggilnya.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan klasifikasi surat keluar pemilik baris tembusan ini, setelah proxy lazy
	 * di-resolve lewat {@code check(...)} warisan {@link GeneralValueObject}.
	 *
	 * <p>Berbeda dengan {@link #toString()}, getter ini aman dipanggil dari konteks di luar sesi
	 * karena {@code check(...)} menangani proxy yang belum ter-inisialisasi. Nilai {@code null}
	 * berarti baris tembusan ini yatim — tidak terikat ke klasifikasi mana pun dan karenanya tidak
	 * akan pernah ikut tercetak pada surat.</p>
	 *
	 * @return klasifikasi pemilik, atau {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_keluar", nullable = true)
	public KlasifikasiSuratKeluar getKlasifikasiSuratKeluar() {
		klasifikasiSuratKeluar = check(klasifikasiSuratKeluar);
		return klasifikasiSuratKeluar;
	}

	/**
	 * Menetapkan klasifikasi surat keluar pemilik baris tembusan ini. Setter polos tanpa validasi:
	 * nilai {@code null} diterima dan akan membuat baris menjadi yatim.
	 *
	 * @param klasifikasiSuratKeluar klasifikasi pemilik.
	 */
	public void setKlasifikasiSuratKeluar(KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
	}

	/**
	 * Mengembalikan jenis jabatan penerima tembusan, dengan proxy lazy sudah di-resolve.
	 *
	 * <p>Nama kolomnya memang {@code tembusan} (bukan {@code jenis_jabatan}) walaupun tipenya
	 * {@link ais.database.model.employ.JenisJabatan} — penamaan ini yang membedakan tabel ini dari
	 * {@link KlasifikasiSuratKeluarPunyaJenisJabatan} yang memakai kolom {@code jenis_jabatan}
	 * untuk peran penanda tangan.</p>
	 *
	 * @return jabatan penerima tembusan, atau {@code null} bila baris belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tembusan", nullable = true)
	public JenisJabatan getTembusan() {
		tembusan = check(tembusan);
		return tembusan;
	}

	/**
	 * Menetapkan jenis jabatan penerima tembusan. Tidak memvalidasi apakah jabatan yang sama sudah
	 * terdaftar sebagai tembusan pada klasifikasi ini — pencegahan duplikat adalah tanggung jawab
	 * pemanggil.
	 *
	 * @param tembusan jabatan penerima tembusan.
	 */
	public void setTembusan(JenisJabatan tembusan) {
		this.tembusan = tembusan;
	}

	/**
	 * Mengembalikan satuan kerja pemilik baris tembusan, dengan <b>auto-isi untuk baris baru</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Langkah pertama adalah {@code check(satuanKerja)} yang me-resolve proxy lazy. Sesudah itu
	 * berlaku sebuah cabang yang hanya aktif pada kondisi sangat spesifik: bila field
	 * {@code satuanKerja} <b>masih</b> {@code null} <b>dan</b> {@code id} juga {@code null} — yaitu
	 * object ini baru dibuat di memori dan belum pernah disimpan — maka getter mengambil satuan
	 * kerja dari konteks pengguna yang sedang login melalui
	 * {@code Common.getCurrentUser().ambilSatuanKerja()}. Bila hasilnya {@code null} (pengguna tidak
	 * terikat satuan kerja mana pun), getter mencoba jalur cadangan: mengambil satuan kerja dari
	 * perpustakaan yang sedang aktif lewat {@code Common.getCurrentPerpustakaan().getSatuanKerja()}.
	 * Nilai yang didapat ditulis kembali ke field, sehingga panggilan berikutnya tidak mengulang
	 * pencarian.</p>
	 *
	 * <h2>Mengapa ini "getter yang menulis"</h2>
	 * <p>Ini adalah salah satu instansi dari pola <b>getter destruktif</b> yang tersebar luas di
	 * basis kode AIS: getter tidak hanya membaca, tetapi juga memutasi state object. Efek sampingnya
	 * nyata dan perlu diketahui pemanggil:</p>
	 * <ul>
	 *   <li>Memanggil getter ini pada object baru akan <b>mengubah</b> object tersebut. Bila object
	 *   kemudian di-{@code save()}, nilai satuan kerja yang tersimpan berasal dari sesi pengguna
	 *   saat getter pertama kali dipanggil — bukan dari input eksplisit siapa pun.</li>
	 *   <li>Urutan pemanggilan menjadi berarti. Bila kode melakukan {@code setSatuanKerja(X)} lebih
	 *   dulu, cabang auto-isi tidak jalan (field sudah tidak {@code null}) dan nilai X dipakai. Bila
	 *   getter dipanggil lebih dulu lalu setter menimpa, hasilnya juga X. Tetapi bila getter
	 *   dipanggil dari kode render UI di antara keduanya, nilai sementara yang berbeda bisa terbaca
	 *   dan tersalin ke tempat lain.</li>
	 *   <li>Syarat {@code id == null} membuat perilaku ini <b>tidak berlaku</b> untuk baris yang
	 *   sudah tersimpan. Baris lama yang kolom {@code satuan_kerja}-nya {@code null} akan tetap
	 *   {@code null} selamanya — auto-isi tidak pernah memperbaiki data historis. Ini penting saat
	 *   menilai kualitas data: kolom {@code satuan_kerja} yang kosong pada tabel ini bukan berarti
	 *   "tidak ada pemilik", melainkan "dibuat sebelum mekanisme auto-isi ini ada, atau dibuat dari
	 *   konteks tanpa pengguna".</li>
	 * </ul>
	 *
	 * <h2>Penanganan galat dan implikasi keamanan</h2>
	 * <p>Seluruh blok auto-isi dibungkus {@code try/catch (Exception)} yang mencatat galat ke
	 * {@code ErrorAuditUtil} lalu melanjutkan. Ini disengaja karena {@code Common.getCurrentUser()}
	 * bergantung pada sesi ZK dan akan melempar exception bila dipanggil dari thread latar, dari
	 * proses batch, atau dari endpoint API tanpa konteks halaman. Konsekuensinya: pada jalur-jalur
	 * tersebut satuan kerja tetap {@code null} dan baris tembusan yang dibuat menjadi
	 * <b>tak-bertenant</b>.</p>
	 *
	 * <p>Perlu ditekankan bahwa field {@code satuanKerja} di sini berfungsi sebagai <b>penanda
	 * kepemilikan, bukan sebagai penyaring akses</b>. Entity tidak menolak pembacaan lintas satuan
	 * kerja; penyaringan sepenuhnya bergantung pada Criteria yang disusun lapisan Action. Karena
	 * baris tak-bertenant ({@code satuan_kerja IS NULL}) tidak akan cocok dengan penyaring
	 * {@code Restrictions.eq("satuanKerja", ...)} mana pun, baris seperti itu cenderung
	 * <i>menghilang</i> dari daftar per-satuan-kerja alih-alih bocor — perilaku yang secara
	 * kebetulan aman, tetapi membuat konfigurasi tembusan diam-diam tidak berlaku tanpa pesan galat.
	 * Bila sebuah surat resmi seharusnya bertembusan tetapi barisnya tak-bertenant, tembusan itu
	 * hilang dari cetakan tanpa jejak yang kasatmata bagi operator.</p>
	 *
	 * <p>Bandingkan dengan {@link KlasifikasiSuratKeluarPunyaJenisJabatan#getSatuanKerja()} yang
	 * menjalankan logika sama persis tetapi <b>tanpa</b> memanggil {@code check(...)} lebih dulu —
	 * perbedaan kecil yang berarti versi di sana tidak me-resolve proxy lazy sebelum memeriksa
	 * {@code null}.</p>
	 *
	 * @return satuan kerja pemilik baris; dapat {@code null} pada baris lama atau bila auto-isi
	 *         gagal karena tidak ada konteks pengguna.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser().ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/KlasifikasiSuratKeluarPunyaTembusan.java:132");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik baris tembusan secara eksplisit. Memanggil setter ini sebelum
	 * {@link #getSatuanKerja()} akan menonaktifkan mekanisme auto-isi karena field tidak lagi
	 * {@code null}.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
