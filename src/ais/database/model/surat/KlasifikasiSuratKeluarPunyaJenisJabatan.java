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




import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.employ.JenisJabatan;
import ais.database.model.library.Perpustakaan;
import ais.database.model.rab.SatuanKerja;



/**
 * <b>Entity JPA — jabatan penanda tangan yang dikonfigurasi pada sebuah klasifikasi surat keluar.</b>
 *
 * <p>Kelas ini memetakan tabel penghubung
 * {@code surat.klasifikasi_surat_keluar_punya_jenis_jabatan}, yang memasangkan satu
 * {@link KlasifikasiSuratKeluar} dengan satu {@link ais.database.model.employ.JenisJabatan} beserta
 * sepasang koordinat ({@link #getPosisiX()} / {@link #getPosisiY()}) untuk menempatkan blok tanda
 * tangan pejabat itu pada layout cetak surat. Idenya: sebuah jenis surat (mis. "Surat Tugas")
 * ditandatangani oleh jabatan tertentu, dan letak tanda tangannya pada halaman dapat diatur
 * per-klasifikasi tanpa mengubah berkas template.</p>
 *
 * <h2>Relasi ke jabatan: JenisJabatan, bukan JabatanStruktural/JabatanFungsional</h2>
 * <p>Perlu ditegaskan karena mudah salah duga: relasi di sini menunjuk
 * {@link ais.database.model.employ.JenisJabatan}, <b>bukan</b>
 * {@code ais.database.model.employ.JabatanStruktural} maupun
 * {@code ais.database.model.employ.JabatanFungsional}. {@code JenisJabatan} adalah kategori jabatan
 * (mis. "Dekan", "Kepala Sekolah", "Wakil Rektor II"); orang yang sedang memangku jabatan tersebut
 * diresolusi terpisah lewat {@code ais.database.model.employ.Pejabat}. Karena yang disimpan adalah
 * jabatannya dan bukan orangnya, konfigurasi penanda tangan tetap sahih ketika pejabat berganti —
 * dan sebaliknya, konfigurasi ini <b>tidak</b> mengikat siapa pun secara pribadi.</p>
 *
 * <h2>Tiga tabel penghubung yang bentuknya kembar</h2>
 * <p>Modul surat keluar memakai tiga tabel yang sama-sama menghubungkan konfigurasi surat ke
 * {@code JenisJabatan}. Membedakannya penting saat menelusuri kode:</p>
 * <ul>
 *   <li>{@code KlasifikasiSuratKeluarPunyaJenisJabatan} (kelas ini) — peran <b>penanda tangan</b>;
 *   kolom relasinya bernama {@code jenis_jabatan}; satu-satunya dari ketiganya yang punya koordinat
 *   {@code posisiX}/{@code posisiY}.</li>
 *   <li>{@link KlasifikasiSuratKeluarPunyaTembusan} — peran <b>penerima tembusan</b>; kolom
 *   relasinya bernama {@code tembusan} walau tipenya sama.</li>
 *   <li>{@link AlurPersetujuanSuratKeluar} (beserta tabel {@code surat.alur_punya_jenis_jabatan})
 *   — peran <b>penyetuju berjenjang</b>; inilah yang benar-benar dieksekusi mesin persetujuan
 *   lewat {@link AlurPersetujuanSuratKeluarStatus}.</li>
 * </ul>
 *
 * <h2>Status pemakaian: entity tidur (dormant)</h2>
 * <p>Hasil penelusuran seluruh pohon sumber {@code src/main/src} pada saat dokumentasi ini ditulis:
 * satu-satunya kode Java yang menyentuh kelas ini di luar berkas ini sendiri adalah
 * {@code ais.action.master.surat.helper.KlasifikasiSuratKeluarPunyaJenisJabatanHelper} — dan
 * helper itu <b>tidak pernah diinstansiasi dari mana pun</b>. Tidak ada Action, dashboard, atau
 * utilitas cetak yang membuat objectnya. Hal yang sama berlaku untuk pasangannya,
 * {@link KlasifikasiSuratKeluarPunyaTembusan}.</p>
 *
 * <p>Konsekuensi praktisnya:</p>
 * <ul>
 *   <li>Jalur cetak yang benar-benar berjalan ({@link SuratKeluar#cetak(ais.database.model.Tbmuser)},
 *   {@code SuratKeluarAction.cetakDisposisi(...)}, {@code SuratUtil.ubahIsiSuratKeluar(...)})
 *   <b>tidak membaca tabel ini sama sekali</b>. Blok tanda tangan pada surat yang tercetak berasal
 *   dari template jrxml dan dari data alur persetujuan, bukan dari baris di sini.</li>
 *   <li>Karena helper editornya tidak terpasang di UI mana pun, dalam kondisi kode saat ini baris
 *   di tabel ini juga tidak bertambah lewat aplikasi. Baris yang ada kemungkinan warisan versi lama
 *   atau hasil impor data.</li>
 *   <li><b>Kualifikasi yang jujur:</b> template jrxml pada sistem ini diunggah administrator
 *   (lihat {@code ais.database.model.file.LampiranLain}), bukan berkas dalam repositori. Secara
 *   teknis sebuah template dapat memuat query SQL sendiri yang membaca tabel ini langsung. Jadi
 *   pernyataan "tidak dipakai" berlaku pasti untuk <b>kode Java</b>; untuk isi template jrxml di
 *   sebuah instalasi, hal itu tidak dapat dipastikan dari repositori.</li>
 *   <li>Sebelum menghapus kelas/tabel ini, periksa lebih dulu isi tabel di basis data produksi dan
 *   isi template jrxml yang terpasang.</li>
 * </ul>
 *
 * <h2>Basis data dan audit</h2>
 * <p>Tabel: skema {@code surat}, nama {@code klasifikasi_surat_keluar_punya_jenis_jabatan}, dengan
 * {@code dynamicInsert}/{@code dynamicUpdate} dan {@link org.hibernate.envers.Audited}. Field
 * {@code oleh}/{@code olehId}/{@code tanggal_dirubah} adalah <b>audit bayangan</b> pendamping
 * Envers — keharusan teknis agar grid ZK dapat menampilkan kolom "Diubah oleh" lewat Criteria
 * biasa, karena tabel revisi Envers hanya terbaca lewat API Envers.</p>
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
@Table(schema = "surat", name = "klasifikasi_surat_keluar_punya_jenis_jabatan")



public class KlasifikasiSuratKeluarPunyaJenisJabatan extends GeneralValueObject {

	/**
	 * 
	 * Versi serialisasi. Nilainya identik dengan hampir seluruh entity lain di paket
	 * {@code ais.database.model.surat} karena berasal dari template hbm2java yang sama; jangan
	 * dipakai sebagai penanda tipe. Dipertahankan agar object yang pernah diserialisasi tetap
	 * terbaca setelah kelas disunting.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama baris. Di-generate database ({@code IDENTITY}). Kondisi {@code null} (belum
	 * pernah disimpan) dipakai {@link #getSatuanKerja()} sebagai penanda "baris baru" untuk
	 * memicu auto-isi satuan kerja.
	 */
	private Long id;
	/**
	 * <b>Baris fisik ini memuat empat anggota sekaligus</b> (warisan pembangkit kode): field
	 * {@code oleh}, field {@code olehId}, method {@code getOlehId()}, dan method
	 * {@code setOlehId(String)}. Javadoc ini berlaku untuk keempatnya karena Javadoc hanya bisa
	 * dilekatkan pada anggota pertama pada satu baris.
	 *
	 * <ul>
	 *   <li>{@code oleh} — nama pengguna terakhir yang mengubah baris ini (audit bayangan, diisi
	 *   {@link ais.database.hibernate.AuditTimestampInterceptor} lewat {@link #onUpdate()}).</li>
	 *   <li>{@code olehId} — id/username pengubah terakhir, pasangan dari {@code oleh}.</li>
	 *   <li>{@code getOlehId()} — getter murni; mengembalikan id pengubah terakhir, {@code null}
	 *   bila baris belum pernah melewati interceptor audit.</li>
	 *   <li>{@code setOlehId(String)} — setter dengan <b>penjaga anti-penghapusan</b>: argumen
	 *   {@code null} atau berisi spasi saja diabaikan sepenuhnya (langsung {@code return}) sehingga
	 *   nilai audit lama tidak tertimpa nilai hampa oleh pemanggil yang tidak punya konteks
	 *   pengguna, mis. proses batch atau thread latar.</li>
	 * </ul>
	 *
	 * <p><b>Peringatan penyuntingan:</b> memecah baris ini menjadi beberapa baris akan mengubah
	 * penomoran baris yang dirujuk catatan audit galat otomatis di berkas lain. Aman dilakukan,
	 * tetapi lakukan sadar.</p>
	 */
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}

	/**
	 * Menyimpan nama pengguna pengubah terakhir, dengan penjaga yang sama seperti
	 * {@code setOlehId(String)}: argumen kosong diabaikan agar jejak audit tidak terhapus.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null} atau hanya spasi.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah tercatat.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: meneruskan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} agar {@code oleh},
	 * {@code olehId}, dan {@code tanggal_dirubah} terisi tepat sebelum Hibernate mengeksekusi
	 * {@code UPDATE}. Tidak berjalan pada INSERT — nilai awal {@code tanggal_dirubah} untuk baris
	 * baru berasal dari inisialisasi field.
	 *
	 * <p><b>Perhatian:</b> deklarasi field {@code tanggal_dirubah} berada pada BARIS FISIK YANG
	 * SAMA dengan method ini, sehingga Javadoc ini sekaligus mendokumentasikan field tersebut:
	 * stempel waktu perubahan terakhir, diinisialisasi ke waktu sekarang lewat
	 * {@code ais.ui.util.WaktuUtil.getDate()} saat object dibuat.</p>
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
	 * Mengembalikan stempel waktu perubahan terakhir baris ini, dipetakan sebagai {@code TIMESTAMP}
	 * (tanggal sekaligus jam), bukan {@code DATE}.
	 *
	 * @return waktu perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris dalam bentuk {@code "<klasifikasi> - <jenis jabatan>"}.
	 *
	 * <p><b>Membaca field langsung, bukan getter.</b> Method ini merujuk field
	 * {@code klasifikasiSuratKeluar} dan {@code jenisJabatan} apa adanya, sehingga
	 * {@code check(...)} tidak pernah dipanggil. Bila kedua relasi masih berupa proxy Hibernate yang
	 * belum ter-inisialisasi dan sesi sudah tertutup, penggabungan string di sini dapat melempar
	 * {@code LazyInitializationException}. Pemanggil yang menampilkan object ini di luar sesi aktif
	 * sebaiknya memanggil {@link #getKlasifikasiSuratKeluar()} dan {@link #getJenisJabatan()} lebih
	 * dulu agar proxy ter-resolve.</p>
	 *
	 * <p>Perhatikan pula bahwa {@code toString()} pada {@link ais.database.model.employ.JenisJabatan}
	 * dan {@link KlasifikasiSuratKeluar} yang ikut terpanggil di sini masing-masing punya
	 * perilakunya sendiri, sehingga hasil akhirnya bisa memuat {@code "null"} literal bila salah
	 * satu sisi belum terisi.</p>
	 *
	 * @return label gabungan klasifikasi dan jabatan penanda tangan.
	 */
	public String toString() {
		return klasifikasiSuratKeluar + " - " + jenisJabatan;
	}

	/**
	 * Klasifikasi surat keluar yang memiliki baris konfigurasi penanda tangan ini.
	 */
	private KlasifikasiSuratKeluar klasifikasiSuratKeluar;
	/**
	 * Jenis jabatan yang berperan sebagai penanda tangan pada klasifikasi surat ini. Bukan orang
	 * tertentu — resolusi ke pejabat yang menjabat dilakukan lewat
	 * {@code ais.database.model.employ.Pejabat}.
	 */
	private JenisJabatan jenisJabatan;
	/**
	 * Satuan kerja pemilik baris (penanda tenant/unit). Diisi otomatis untuk baris baru; lihat
	 * {@link #getSatuanKerja()}.
	 */
	private SatuanKerja satuanKerja;
	/**
	 * Koordinat horizontal blok tanda tangan pada layout cetak. Diinisialisasi {@code 0.0} — bukan
	 * {@code null} — sehingga baris baru selalu punya nilai numerik. Satuan koordinat tidak
	 * ditentukan di level entity; lihat {@link #getPosisiX()}.
	 */
	private Double posisiX = 0.0;
	/**
	 * Koordinat vertikal blok tanda tangan pada layout cetak. Sama seperti {@link #posisiX},
	 * diinisialisasi {@code 0.0}.
	 */
	private Double posisiY = 0.0;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi lewat refleksi.
	 * Field {@code posisiX} dan {@code posisiY} sudah bernilai {@code 0.0} lewat inisialisasi
	 * field, jadi object baru langsung siap dipakai tanpa risiko {@code NullPointerException} pada
	 * kedua koordinat.
	 */
	public KlasifikasiSuratKeluarPunyaJenisJabatan() {
	}

	/**
	 * Mengembalikan kunci utama baris.
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
	 * Menetapkan kunci utama. Praktis hanya dipakai Hibernate.
	 *
	 * @param id kunci utama baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan klasifikasi surat keluar pemilik baris ini, setelah proxy lazy di-resolve lewat
	 * {@code check(...)} warisan {@link GeneralValueObject}. Aman dipanggil di luar sesi aktif,
	 * berbeda dengan {@link #toString()}.
	 *
	 * @return klasifikasi pemilik, atau {@code null} bila baris yatim.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "klasifikasi_surat_keluar", nullable = true)
	public KlasifikasiSuratKeluar getKlasifikasiSuratKeluar() {
		klasifikasiSuratKeluar = check(klasifikasiSuratKeluar);
		return klasifikasiSuratKeluar;
	}

	/**
	 * Menetapkan klasifikasi surat keluar pemilik baris ini. Setter polos tanpa validasi; nilai
	 * {@code null} diterima dan membuat baris menjadi yatim (tidak terhubung ke klasifikasi mana
	 * pun).
	 *
	 * @param klasifikasiSuratKeluar klasifikasi pemilik.
	 */
	public void setKlasifikasiSuratKeluar(
			KlasifikasiSuratKeluar klasifikasiSuratKeluar) {
		this.klasifikasiSuratKeluar = klasifikasiSuratKeluar;
	}

	/**
	 * Mengembalikan jenis jabatan penanda tangan, dengan proxy lazy sudah di-resolve.
	 *
	 * <p>Kolomnya bernama {@code jenis_jabatan} — inilah pembeda terhadap
	 * {@link KlasifikasiSuratKeluarPunyaTembusan} yang memakai nama kolom {@code tembusan} untuk
	 * tipe entity yang persis sama.</p>
	 *
	 * @return jabatan penanda tangan, atau {@code null} bila belum diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_jabatan", nullable = true)
	public JenisJabatan getJenisJabatan() {
		jenisJabatan = check(jenisJabatan);
		return jenisJabatan;
	}

	/**
	 * Menetapkan jenis jabatan penanda tangan. Tidak memvalidasi duplikasi: jabatan yang sama dapat
	 * terdaftar lebih dari sekali pada klasifikasi yang sama karena tidak ada unique constraint
	 * gabungan {@code (klasifikasi_surat_keluar, jenis_jabatan)}.
	 *
	 * @param jenisJabatan jabatan penanda tangan.
	 */
	public void setJenisJabatan(JenisJabatan jenisJabatan) {
		this.jenisJabatan = jenisJabatan;
	}

	/**
	 * Mengembalikan koordinat horizontal blok tanda tangan, dengan <b>normalisasi {@code null}
	 * menjadi {@code 0.0}</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Bila field {@code posisiX} bernilai {@code null} — kondisi yang tidak mungkin terjadi pada
	 * object yang baru dibuat di memori (inisialisasi field sudah {@code 0.0}), tetapi <b>mungkin</b>
	 * terjadi pada baris yang dimuat dari database dengan kolom {@code NULL} — getter menuliskan
	 * {@code 0.0} ke field lalu mengembalikannya. Ini adalah bentuk ringan dari pola <i>getter yang
	 * menulis</i> yang tersebar di basis kode ini: pemanggilan getter mengubah state object, dan
	 * bila object tersebut kemudian tersimpan, kolom yang semula {@code NULL} akan berubah menjadi
	 * {@code 0}. Efeknya kecil di sini (nilai {@code 0.0} memang setara "belum diatur"), tetapi
	 * perlu diketahui saat menganalisis mengapa kolom database berubah tanpa ada aksi pengguna.</p>
	 *
	 * <h2>Satuan koordinat dan siapa yang menafsirkannya</h2>
	 * <p>Entity ini <b>tidak</b> menentukan satuan maupun titik acuan koordinat. Nilainya sekadar
	 * angka pecahan yang, secara desain, dimaksudkan untuk diteruskan ke mesin layout cetak agar
	 * blok tanda tangan pejabat digeser ke posisi yang diinginkan pada halaman. Tidak ada validasi
	 * rentang: nilai negatif, nilai sangat besar, maupun nilai yang menempatkan tanda tangan di luar
	 * area kertas semuanya diterima tanpa keluhan.</p>
	 *
	 * <h2>Status pemakaian nyata</h2>
	 * <p>Penelusuran seluruh pohon sumber menunjukkan bahwa {@code getPosisiX()} hanya dibaca oleh
	 * {@code ais.action.master.surat.helper.KlasifikasiSuratKeluarPunyaJenisJabatanHelper} — kelas
	 * yang menyusun grid editor untuk baris-baris ini — dan helper tersebut tidak pernah
	 * diinstansiasi dari Action mana pun. Perlu diperhatikan pula bahwa nama getter yang sama
	 * ({@code getPosisiX()}/{@code getPosisiY()}) juga dimiliki
	 * {@code ais.database.model.file.FotoGambarTandaTanganPejabat} dan dipakai aktif oleh
	 * {@code FotoGambarTandaTanganPejabatHelper}; hasil pencarian teks yang tidak memperhatikan tipe
	 * mudah tertukar antara keduanya, dan hasil pencarian itulah yang bisa memberi kesan keliru
	 * bahwa koordinat di kelas ini terpakai.</p>
	 *
	 * <p>Dengan kata lain: dalam kode Java yang ada, koordinat ini tidak pernah sampai ke mesin
	 * cetak. Jalur cetak surat keluar yang benar-benar berjalan
	 * ({@link SuratKeluar#cetak(ais.database.model.Tbmuser, java.util.Map)} yang menggabungkan
	 * berkas jrxml lewat {@code LampiranLain}) menyusun parameter laporannya dari
	 * {@code SuratUtil.ubahIsiSuratKeluar(...)}, dan utilitas itu tidak menyertakan {@code posisiX}
	 * maupun {@code posisiY}. Satu-satunya kemungkinan tersisa adalah template jrxml yang diunggah
	 * administrator membaca tabel ini lewat query SQL-nya sendiri — sesuatu yang tidak dapat
	 * dipastikan dari repositori karena template bukan berkas repo.</p>
	 *
	 * <p>Karena itu, jangan menambah fitur baru di atas asumsi bahwa koordinat ini sudah berfungsi.
	 * Bila penempatan tanda tangan memang dibutuhkan, jalur yang hidup adalah lewat template jrxml
	 * atau lewat {@code FotoGambarTandaTanganPejabat}.</p>
	 *
	 * @return koordinat horizontal; tidak pernah {@code null}, minimal {@code 0.0}.
	 */
	public Double getPosisiX() {
		if (posisiX == null) {
			posisiX = 0.0;
		}
		return posisiX;
	}

	/**
	 * Menetapkan koordinat horizontal blok tanda tangan. Tidak memvalidasi rentang; nilai
	 * {@code null} diterima dan akan dinormalisasi menjadi {@code 0.0} pada pembacaan berikutnya
	 * lewat {@link #getPosisiX()}.
	 *
	 * @param posisiX koordinat horizontal.
	 */
	public void setPosisiX(Double posisiX) {
		this.posisiX = posisiX;
	}

	/**
	 * Mengembalikan koordinat vertikal blok tanda tangan, dengan normalisasi {@code null} menjadi
	 * {@code 0.0}. Seluruh catatan pada {@link #getPosisiX()} berlaku sama di sini: getter menulis
	 * balik ke field, tidak ada validasi rentang, dan nilainya tidak terbaca oleh jalur cetak Java
	 * mana pun.
	 *
	 * @return koordinat vertikal; tidak pernah {@code null}, minimal {@code 0.0}.
	 */
	public Double getPosisiY() {
		if (posisiY == null) {
			posisiY = 0.0;
		}
		return posisiY;
	}

	/**
	 * Menetapkan koordinat vertikal blok tanda tangan. Tanpa validasi rentang.
	 *
	 * @param posisiY koordinat vertikal.
	 */
	public void setPosisiY(Double posisiY) {
		this.posisiY = posisiY;
	}

	/**
	 * Mengembalikan satuan kerja pemilik baris, dengan <b>auto-isi untuk baris baru</b>.
	 *
	 * <h2>Perilaku</h2>
	 * <p>Bila field {@code satuanKerja} bernilai {@code null} <b>dan</b> {@code id} juga
	 * {@code null} — yakni object baru dibuat di memori dan belum pernah disimpan — getter mengambil
	 * satuan kerja dari konteks pengguna yang sedang login lewat
	 * {@code Common.getCurrentUser().ambilSatuanKerja()}. Bila hasilnya {@code null}, dicoba jalur
	 * cadangan {@code Common.getCurrentPerpustakaan().getSatuanKerja()}. Nilai yang diperoleh
	 * ditulis balik ke field sehingga panggilan berikutnya tidak mengulang pencarian.</p>
	 *
	 * <h2>Perbedaan halus dengan kembarannya</h2>
	 * <p>Logika ini identik dengan
	 * {@link KlasifikasiSuratKeluarPunyaTembusan#getSatuanKerja()} kecuali satu hal: <b>versi di
	 * sini TIDAK memanggil {@code check(satuanKerja)} lebih dulu</b>. Akibatnya, bila field berisi
	 * proxy Hibernate yang belum ter-inisialisasi, getter ini mengembalikan proxy itu apa adanya
	 * tanpa upaya resolusi. Pada praktiknya perbedaan ini jarang terasa karena pemeriksaan
	 * {@code null} tetap benar (proxy bukan {@code null}), tetapi pemanggil yang langsung membaca
	 * properti dari hasil getter di luar sesi aktif dapat menemui
	 * {@code LazyInitializationException} di sini padahal tidak di kembarannya. Bila kelak ada
	 * perbaikan, menyamakan keduanya dengan menambahkan {@code check(...)} adalah perubahan yang
	 * aman.</p>
	 *
	 * <h2>Getter destruktif dan konsekuensinya</h2>
	 * <ul>
	 *   <li>Memanggil getter ini pada object baru <b>mengubah</b> object tersebut. Bila object
	 *   kemudian disimpan, nilai satuan kerja berasal dari sesi pengguna saat getter pertama kali
	 *   dipanggil, bukan dari input eksplisit.</li>
	 *   <li>Syarat {@code id == null} membuat mekanisme ini tidak pernah berlaku untuk baris yang
	 *   sudah tersimpan. Baris lama dengan kolom {@code satuan_kerja} kosong akan tetap kosong
	 *   selamanya; auto-isi bukan mekanisme perbaikan data historis.</li>
	 *   <li>Urutan pemanggilan berarti: {@code setSatuanKerja(X)} sebelum getter menonaktifkan
	 *   auto-isi karena field sudah tidak {@code null}.</li>
	 * </ul>
	 *
	 * <h2>Penanganan galat dan implikasi tenant</h2>
	 * <p>Blok auto-isi dibungkus {@code try/catch (Exception)} yang mencatat ke
	 * {@code ErrorAuditUtil} lalu melanjutkan. Ini disengaja: {@code Common.getCurrentUser()}
	 * bergantung pada sesi ZK dan akan melempar exception bila dipanggil dari thread latar, proses
	 * batch, atau endpoint API tanpa konteks halaman. Pada jalur-jalur tersebut satuan kerja tetap
	 * {@code null} dan baris yang dibuat menjadi <b>tak-bertenant</b>.</p>
	 *
	 * <p>Field ini adalah <b>penanda kepemilikan, bukan penyaring akses</b>. Entity tidak menolak
	 * pembacaan lintas satuan kerja; penyaringan sepenuhnya bergantung pada Criteria yang disusun
	 * lapisan Action. Karena baris tak-bertenant tidak cocok dengan penyaring
	 * {@code Restrictions.eq("satuanKerja", ...)} mana pun, baris seperti itu cenderung
	 * <i>menghilang</i> dari daftar per-satuan-kerja alih-alih bocor lintas unit — secara kebetulan
	 * aman, tetapi menyebabkan konfigurasi hilang diam-diam tanpa pesan galat.</p>
	 *
	 * @return satuan kerja pemilik; dapat {@code null} pada baris lama atau bila auto-isi gagal
	 *         karena tidak ada konteks pengguna.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		if (this.satuanKerja == null && this.id == null) {
			try {
				SatuanKerja satuanKerja = Common.getCurrentUser()
						.ambilSatuanKerja();
				Perpustakaan currentPerpustakaan = Common
						.getCurrentPerpustakaan();
				if (satuanKerja == null && currentPerpustakaan != null) {
					satuanKerja = currentPerpustakaan.getSatuanKerja();
				}
				this.satuanKerja = satuanKerja;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/surat/KlasifikasiSuratKeluarPunyaJenisJabatan.java:160");
			}
		}
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja pemilik baris secara eksplisit. Memanggil setter ini sebelum
	 * {@link #getSatuanKerja()} menonaktifkan mekanisme auto-isi karena field tidak lagi
	 * {@code null}.
	 *
	 * @param satuanKerja satuan kerja pemilik; boleh {@code null}.
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

}
