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
 * <h2>Batch unggahan biodata calon mahasiswa (PMB) — kepala dari satu berkas Excel yang diimpor
 * massal</h2>
 *
 * <p>Satu baris tabel {@code public.upload_biodata_calon_mahasiswa} mewakili <b>satu kali kegiatan
 * unggah</b> berkas Excel (.xlsx) berisi daftar calon mahasiswa. Baris ini <b>bukan</b> data
 * pendaftar; ia adalah <i>kepala batch</i> yang menyimpan parameter impor (gelombang tujuan, paket,
 * apakah nomor registrasi digenerate sistem) beserta log hasil impor, sedangkan data pendaftar
 * sesungguhnya lahir sebagai baris-baris {@link BiodataCalonMahasiswa} yang menunjuk balik ke sini
 * lewat {@link BiodataCalonMahasiswa#getUploadBiodataCalonMahasiswa()} (kolom
 * {@code biodata_calon_mahasiswa.upload_biodata_calon_mahasiswa}).</p>
 *
 * <h3>Posisi dalam alur PMB</h3>
 * <ol>
 * <li>Petugas membuka layar {@code /pages/master/upload_biodata_calon_mahasiswa_span_ptkin.zul}
 * (controller {@code ais.action.master.UploadBiodataCalonMahasiswaSPANPTKINAction}), menekan
 * <i>Tambah</i>, lalu mengunggah berkas .xlsx hasil ekspor SPAN-PTKIN/UM-PTKIN.</li>
 * <li>Berkas fisik disalin ke {@code <REAL_PATH>/tmp/}, lalu <b>isi berkasnya disimpan utuh sebagai
 * BLOB</b> pada entity {@code ais.database.model.file.UploadBiodataCalonMahasiswaFileContent}
 * (tabel {@code upload_biodata_calon_mahasiswa_file_content}, kolom {@code ref} = {@link #getId()}
 * baris ini). Relasi itu <b>tidak dipetakan sebagai asosiasi Hibernate</b> — hanya kolom
 * {@code Long ref} lepas, sehingga pencariannya selalu berupa
 * {@code Restrictions.eq("ref", upload.getId())} yang manual.</li>
 * <li>Baris batch ini disimpan, lalu {@code uploadFormat1()} membaca sheet pertama dan untuk setiap
 * baris data melakukan <i>upsert</i> {@link BiodataCalonMahasiswa} (dicari berdasar
 * {@code noRegistrasi} + {@link #getGelombangPendaftaran()}; bila belum ada dibuat baru).</li>
 * <li>Ringkasan kegagalan per baris dikumpulkan sebagai teks panjang dan disimpan kembali ke
 * {@link #getPeringatan()} pada baris batch ini, sehingga tombol <i>Log</i> di grid dapat
 * menampilkannya kapan saja tanpa mengulang impor.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 * <li><b>Identitas &amp; jejak audit</b> — {@link #getId()}/{@link #setId(Long)},
 * {@link #getOleh()}/{@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@code onUpdate()},
 * {@link #toString()}.</li>
 * <li><b>Identitas berkas sumber</b> — {@link #getNama()}/{@link #setNama(String)} (nama berkas
 * asli) dan {@link #getTipe()}/{@link #setTipe(String)} (MIME type berkas).</li>
 * <li><b>Parameter impor</b> — {@link #getGelombangPendaftaran()}/{@link
 * #setGelombangPendaftaran(GelombangPendaftaran)}, {@link #getPaket()}/{@link #setPaket(Paket)},
 * {@link #getGenerateNoRegistrasiOlehSistem()}/{@link
 * #setGenerateNoRegistrasiOlehSistem(Boolean)}.</li>
 * <li><b>Catatan bebas &amp; hasil</b> — {@link #getKeterangan()}/{@link #setKeterangan(String)}
 * (diisi petugas) dan {@link #getPeringatan()}/{@link #setPeringatan(String)} (diisi mesin
 * impor).</li>
 * </ul>
 * <p>Tidak ada satu pun method bisnis, query statis, maupun helper perhitungan di kelas ini: seluruh
 * logika impor tinggal di controller. Kelas ini murni <i>value object</i> terpetakan.</p>
 *
 * <h3>Kuirk &amp; jebakan yang harus diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 * <li><b>{@link #getNama()} punya nilai cadangan dari relasi.</b> Bila kolom {@code nama} kosong,
 * getter mengembalikan nama {@link GelombangPendaftaran} — jadi kolom "File" di grid bisa
 * menampilkan nama gelombang, bukan nama berkas. Karena kolomnya {@code nullable = false} dan
 * Hibernate memetakan kelas ini lewat <i>property access</i> (anotasi menempel di getter), nilai
 * cadangan itulah yang benar-benar tertulis ke basis data. Efek nyatanya: menyunting batch lama
 * lewat tombol <i>Edit</i> lalu menyimpan <b>tanpa</b> mengunggah ulang berkas akan menimpa nama
 * berkas asli dengan nama gelombang (controller memanggil {@code setNama(namaFile)} dengan
 * {@code namaFile} yang masih {@code null}), sekaligus mengosongkan {@link #getTipe()}. Riwayat
 * nama berkas asli hanya tersisa di tabel Envers dan pada
 * {@code UploadBiodataCalonMahasiswaFileContent.nama}.</li>
 * <li><b>{@link #toString()} memakai field, bukan getter.</b> Ia membaca {@code nama} langsung
 * sehingga <i>tidak</i> ikut memakai nilai cadangan di atas — {@code toString()} bisa menghasilkan
 * {@code "12-null"} untuk baris yang di grid tampil bernama gelombang.</li>
 * <li><b>{@link #getGenerateNoRegistrasiOlehSistem()} adalah getter yang MENULIS ke field.</b> Nilai
 * {@code null} dinormalkan menjadi {@code false} dan hasil normalisasi itu <b>ditugaskan kembali ke
 * field</b>. Pada instance yang masih terkelola (managed) di dalam session terbuka, sekadar
 * <i>membaca</i> properti ini mengubah {@code null} → {@code false} dan dapat memunculkan
 * {@code UPDATE} saat flush (entity ini {@code dynamicUpdate}) berikut satu baris revisi Envers.
 * Pembacaan itu memang terjadi di jalur render grid dan di {@code uploadFormat1()}.</li>
 * <li><b>{@link #getGelombangPendaftaran()} dan {@link #getPaket()} memanggil {@code check()} lalu
 * menugaskan hasilnya kembali ke field</b> — pola getter relasi standar AIS (lihat
 * {@link GeneralValueObject#check(Object)}). Instance yang dikembalikan bisa objek <i>lain</i>
 * (kanonik dari {@code EntityIdentityMap}, cache, atau hasil reload), bukan proxy semula. Ini bukan
 * efek samping yang boleh dihapus: tanpa itu, relasi lazy pada objek yang sudah <i>detached</i>
 * akan melempar {@code LazyInitializationException} saat grid dirender.</li>
 * <li><b>Tidak ada getter destruktif dan tidak ada getter yang menutup session Hibernate di kelas
 * ini.</b> Sudah diperiksa langsung atas seluruh isi berkas — satu-satunya efek samping getter
 * adalah dua pola di atas ({@code check()} dan normalisasi boolean).</li>
 * <li><b>{@link #setOleh(String)}/{@link #setOlehId(String)} menolak nilai kosong secara diam-diam</b>
 * — jejak audit tidak bisa dikosongkan kembali dan pemanggil tidak diberi tahu bahwa nilainya
 * diabaikan.</li>
 * <li><b>Tidak ada {@code @PrePersist}.</b> Hanya {@code @PreUpdate} yang terpasang; pada
 * {@code INSERT} stempel waktu berasal dari nilai inisialisasi field {@code tanggal_dirubah}, dan
 * kolom {@code oleh}/{@code olehId} hanya terisi bila ada kode lain yang menstempelnya.</li>
 * <li><b>{@link #getTipe()} dan {@link #getGenerateNoRegistrasiOlehSistem()} tidak punya
 * {@code @Column}.</b> Keduanya jatuh ke penamaan bawaan JPA (nama properti apa adanya), berbeda
 * dari properti lain di kelas ini yang namanya ditulis eksplisit. Jangan berasumsi kolomnya
 * bernama {@code snake_case} saat menulis SQL native atau migrasi.</li>
 * <li><b>Field {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah}
 * dideklarasikan ULANG di sini walau {@link GeneralValueObject} juga memilikinya — ini KEHARUSAN
 * TEKNIS, bukan duplikasi yang perlu "dirapikan".</b> {@link GeneralValueObject} adalah POJO
 * abstrak biasa, <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, sehingga Hibernate
 * sama sekali tidak memetakan properti milik induk. Menghapus deklarasi ulang ini akan menghapus
 * kolomnya dari pemetaan.</li>
 * <li><b>Penghapusan batch memakai SQL native berantai, bukan cascade Hibernate.</b> Tombol
 * <i>Hapus</i> di grid menjalankan {@code delete from biodata_calon_mahasiswa where
 * upload_biodata_calon_mahasiswa=:id} lebih dulu, baru menghapus baris batch — artinya menghapus
 * satu baris kelas ini <b>ikut menghapus seluruh calon mahasiswa hasil unggahan tersebut</b>,
 * termasuk yang sudah terlanjur diproses lebih lanjut. Baris
 * {@code UploadBiodataCalonMahasiswaFileContent} yang menyimpan BLOB berkas <b>tidak</b> ikut
 * dihapus dan menjadi yatim.</li>
 * <li><b>Tombol <i>Ulangi</i> menjalankan ulang seluruh impor dari BLOB tersimpan.</b> Karena
 * {@code uploadFormat1()} bersifat <i>upsert</i>, mengulang batch akan menimpa kembali data
 * pendaftar yang mungkin sudah disunting manual setelah impor pertama.</li>
 * </ol>
 *
 * <h3>Catatan kontrol akses (per pembacaan kode, bukan uji runtime)</h3>
 * <p>Layar pengelola batch ini memanggil {@code ais.common.Common.doCheckSecurity()} pada
 * {@code doBeforeCompose}. Rantai pemanggilannya bermuara di
 * {@code CommonPrivilages.doCheckPrevilagesRead()}, yang hanya benar-benar menegakkan pemeriksaan
 * untuk daftar putih {@code MUST_CHECKED} berisi 12 halaman; berkas ZUL layar ini tidak termasuk di
 * dalamnya, sehingga panggilan tersebut efektif tanpa efek. Pada tingkat tombol, hanya
 * <i>Tambah</i> ({@code CREATE}) dan <i>Hapus</i> ({@code DELETE}) yang bergerbang
 * {@code CommonPrivilages}; tombol <i>Download</i> (mengunduh berkas Excel sumber berisi daftar
 * lengkap pendaftar), <i>Ulangi</i> (mengulang impor massal), <i>Log</i>, dan <i>Edit</i> dipasang
 * tanpa gerbang hak akses sama sekali. Pola "aksi berdampak besar justru tidak dijaga" ini sama
 * dengan yang sudah dicatat pada beberapa entity lain di inisiatif dokumentasi ini.</p>
 *
 * @see BiodataCalonMahasiswa
 * @see GelombangPendaftaran
 * @see Paket
 * @see GeneralValueObject
 * @see ais.database.model.file.UploadBiodataCalonMahasiswaFileContent
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "upload_biodata_calon_mahasiswa")

public class UploadBiodataCalonMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi kelas. Nilainya dipertahankan agar instance yang tersimpan di session ZK
	 * (layar ini menyimpan batch terpilih sebagai state komponen) tetap dapat dibaca setelah
	 * aplikasi di-<i>redeploy</i>. Jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci primer batch unggahan (kolom {@code id}, {@code IDENTITY}). */
	private Long id;

	/**
	 * Nama tampil pengguna yang terakhir mengubah baris ini. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate — lihat catatan pada Javadoc kelas.
	 */
	private String oleh;

	/**
	 * Identitas (username/id) pengguna yang terakhir mengubah baris ini. Sama seperti {@link #oleh},
	 * wajib dideklarasikan ulang agar ikut terpetakan.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengubah terakhir apa adanya.
	 *
	 * @return isi kolom {@code olehId}, atau {@code null} bila belum pernah distempel
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengubah terakhir.
	 *
	 * <p><b>Efek samping / kuirk:</b> nilai {@code null}, string kosong, atau yang hanya berisi
	 * whitespace <b>diabaikan diam-diam</b> — nilai lama dipertahankan dan pemanggil tidak diberi
	 * tahu. Nilai yang lolos disimpan tanpa di-trim.</p>
	 *
	 * @param olehId id/username pengubah; {@code null}/kosong/whitespace diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampil pengubah terakhir.
	 *
	 * <p>Berperilaku persis sama dengan {@link #setOlehId(String)}: {@code null}/kosong/whitespace
	 * diabaikan diam-diam dan nilai yang lolos tidak di-trim.</p>
	 *
	 * @param oleh nama tampil pengubah; {@code null}/kosong/whitespace diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampil pengubah terakhir apa adanya.
	 *
	 * <p>Dipakai langsung sebagai isi kolom "Diupload oleh" pada grid daftar batch.</p>
	 *
	 * @return isi kolom {@code oleh}, atau {@code null} bila belum pernah distempel
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} dan deklarasi field {@link #tanggal_dirubah} — keduanya berada
	 * pada satu baris fisik yang sama (bentuk asli hasil penyisipan otomatis; jangan dirapikan tanpa
	 * alasan, agar diff tetap bersih).
	 *
	 * <p><b>{@code onUpdate()}</b> dipanggil Hibernate <i>tepat sebelum</i> setiap {@code UPDATE}
	 * baris ini dan meneruskan instance ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang menyegarkan stempel
	 * waktu perubahan. Tidak ada padanan {@code @PrePersist}, jadi pada {@code INSERT} stempel yang
	 * dipakai adalah nilai inisialisasi field di bawah. Method sengaja {@code protected}: hanya
	 * provider persistensi yang boleh memanggilnya, bukan kode aplikasi.</p>
	 *
	 * <p><b>{@code tanggal_dirubah}</b> menyimpan waktu perubahan terakhir. Diinisialisasi saat objek
	 * dibuat dengan {@code ais.ui.util.WaktuUtil.getDate()} — jam server aplikasi yang sudah
	 * disesuaikan zona waktu, bukan {@code new Date()} mentah — sehingga baris batch baru tetap punya
	 * stempel meski {@code @PrePersist} tidak ada. Nilai inilah yang tampil sebagai kolom "Tanggal
	 * Upload" di grid; perhatikan bahwa maknanya sebenarnya "terakhir diubah", bukan "waktu unggah
	 * pertama", sehingga menekan <i>Ulangi</i> akan memajukan tanggal tersebut.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual.
	 *
	 * <p>Umumnya tidak perlu dipanggil kode aplikasi: {@code onUpdate()} sudah menyegarkannya
	 * otomatis pada setiap {@code UPDATE}. Setter ini terutama dipakai Hibernate saat memuat baris
	 * dan oleh utilitas impor/migrasi yang ingin mempertahankan stempel asal.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin disimpan; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * <p>Dirender apa adanya pada kolom "Tanggal Upload" grid batch lewat
	 * {@code Common.dateFormat}. Karena tidak pernah bernilai {@code null} untuk objek yang dibuat
	 * lewat konstruktor, pemanggil di grid tidak melakukan penjagaan null.</p>
	 *
	 * @return waktu perubahan terakhir baris ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat batch: {@code "<id>-<nama>"}.
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()}, sehingga <b>tidak</b> memakai nilai cadangan dari
	 * {@link #getGelombangPendaftaran()}. Untuk baris yang kolom {@code nama}-nya kosong, hasilnya
	 * berupa {@code "12-null"} walaupun grid menampilkan nama gelombang. Perbedaan ini nyata bagi
	 * kode yang memakai {@code toString()} sebagai label combobox atau pesan.</p>
	 *
	 * @return gabungan id dan nama berkas, tanpa penjagaan {@code null} pada kedua bagian
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama berkas Excel yang diunggah (kolom {@code nama}). Lihat {@link #getNama()} untuk perilaku
	 * nilai cadangan yang tidak biasa.
	 */
	private String nama;

	/**
	 * MIME type berkas yang diunggah, disalin dari {@code Media.getContentType()} saat unggah
	 * (biasanya {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}).
	 */
	private String tipe;

	/** Catatan bebas yang diketik petugas saat membuat/menyunting batch (kolom {@code keterangan}). */
	private String keterangan;

	/**
	 * Gelombang pendaftaran tujuan impor — penentu ke gelombang mana seluruh
	 * {@link BiodataCalonMahasiswa} hasil batch ini dimasukkan, sekaligus penyaring jenjang saat
	 * mencocokkan nama/kode program studi.
	 */
	private GelombangPendaftaran gelombangPendaftaran;

	/**
	 * Paket pendaftaran yang dipaksakan untuk seluruh pendaftar hasil batch ini; boleh
	 * {@code null} yang berarti "berlaku untuk semua paket".
	 */
	private Paket paket;

	/**
	 * Log hasil impor terakhir dalam bentuk teks bebas multi-baris (kolom bertipe {@code text}).
	 * Ditulis oleh mesin impor, bukan oleh petugas.
	 */
	private String peringatan;

	/**
	 * Penanda apakah nomor registrasi dibangkitkan sistem, bukan diambil dari berkas Excel. Lihat
	 * {@link #getGenerateNoRegistrasiOlehSistem()} — getter-nya menormalkan {@code null} dan
	 * menuliskannya balik ke field ini.
	 */
	private Boolean generateNoRegistrasiOlehSistem;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai layar pengelola batch saat menekan tombol <i>Tambah</i>. Seluruh properti
	 * dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah terisi dari inisialisasi
	 * field.</p>
	 */
	public UploadBiodataCalonMahasiswa() {
	}

	/**
	 * Mengembalikan kunci primer batch unggahan.
	 *
	 * <p>Nilai ini dipakai sebagai kunci penghubung manual ke dua tempat: kolom {@code ref} pada
	 * {@code UploadBiodataCalonMahasiswaFileContent} (BLOB berkas sumber) dan kolom
	 * {@code upload_biodata_calon_mahasiswa} pada tabel {@code biodata_calon_mahasiswa}. Keduanya
	 * dibaca lewat kriteria/SQL native, bukan lewat asosiasi Hibernate.</p>
	 *
	 * @return id baris; {@code null} selama batch belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer batch. Praktis hanya dipanggil Hibernate saat memuat/menyimpan baris;
	 * kode aplikasi tidak boleh menetapkannya sendiri karena kolomnya {@code IDENTITY}.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama berkas Excel sumber batch ini.
	 *
	 * <p><b>Bukan getter sepele.</b> Perilakunya berlapis:</p>
	 * <ul>
	 * <li>bila field {@code nama} terisi → dikembalikan sudah di-{@code trim()};</li>
	 * <li>bila field {@code nama} {@code null} → dikembalikan nama {@link GelombangPendaftaran}
	 * sebagai <b>nilai cadangan</b>;</li>
	 * <li>bila keduanya kosong → {@code null}.</li>
	 * </ul>
	 * <p>Karena kelas ini dipetakan lewat <i>property access</i> dan kolomnya
	 * {@code nullable = false}, nilai cadangan itulah yang sungguh-sungguh ditulis ke basis data
	 * saat baris disimpan. Konsekuensi praktisnya sudah dijelaskan pada Javadoc kelas: menyimpan
	 * ulang batch lama tanpa mengunggah berkas baru akan mengganti nama berkas asli dengan nama
	 * gelombang. Nilai kembalian juga dipakai sebagai label tautan revisi di grid dan sebagai
	 * {@code nama} pada baris {@code UploadBiodataCalonMahasiswaFileContent} yang baru dibuat.</p>
	 *
	 * @return nama berkas yang sudah di-trim, nama gelombang sebagai cadangan, atau {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? (gelombangPendaftaran == null ? null : gelombangPendaftaran.getNama())
				: this.nama.trim();
	}

	/**
	 * Menyetel nama berkas sumber.
	 *
	 * <p>Diisi controller dari {@code Media.getName()} berkas yang baru diunggah. <b>Perhatian:</b>
	 * controller memanggil setter ini pada setiap penyimpanan, termasuk ketika petugas hanya
	 * menyunting keterangan tanpa mengunggah berkas — pada kasus itu argumennya {@code null} dan nama
	 * berkas asli hilang (lihat {@link #getNama()}).</p>
	 *
	 * @param nama nama berkas; {@code null} diterima apa adanya dan mengaktifkan nilai cadangan pada
	 *             getter
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas petugas untuk batch ini apa adanya.
	 *
	 * <p>Dirender langsung pada kolom "Keterangan" grid; {@code null} akan tampil sebagai label
	 * kosong.</p>
	 *
	 * @return isi kolom {@code keterangan}, boleh {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel catatan bebas petugas.
	 *
	 * @param keterangan teks bebas; {@code null} diterima
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan gelombang pendaftaran tujuan impor batch ini.
	 *
	 * <p><b>Efek samping (pola relasi standar AIS):</b> hasil {@link GeneralValueObject#check(Object)}
	 * <b>ditugaskan kembali ke field</b> sebelum dikembalikan, sehingga objek yang keluar bisa
	 * instance lain (kanonik dari {@code EntityIdentityMap}, dari cache, atau hasil reload) dan bukan
	 * proxy lazy semula. Tanpa langkah itu, pembacaan relasi pada objek yang sudah <i>detached</i>
	 * — persis yang terjadi saat grid batch dirender di luar session asal — akan melempar
	 * {@code LazyInitializationException}.</p>
	 * <p>Nilai ini dibaca berkali-kali oleh mesin impor: sebagai penyaring jenjang saat mencari
	 * {@code Jurusan}, sebagai kriteria pencarian {@link BiodataCalonMahasiswa} yang sudah ada, dan
	 * sebagai nilai yang ditanamkan ke setiap pendaftar hasil impor. Kolomnya {@code nullable}, tetapi
	 * layar mewajibkan pemilihan gelombang sebelum menyimpan, dan mesin impor akan gagal dengan
	 * {@code NullPointerException} bila nilainya kosong.</p>
	 *
	 * @return gelombang pendaftaran tujuan; {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran", nullable = true)
	public GelombangPendaftaran getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Menyetel gelombang pendaftaran tujuan impor.
	 *
	 * <p>Diambil dari combobox layar, yang hanya menampilkan gelombang dengan {@code aktif} bernilai
	 * {@code true} atau {@code null}. Mengubah nilai ini pada batch yang sudah pernah diimpor tidak
	 * memindahkan pendaftar yang terlanjur dibuat — pemindahan baru terjadi bila impor dijalankan
	 * ulang lewat tombol <i>Ulangi</i>.</p>
	 *
	 * @param gelombangPendaftaran gelombang tujuan; {@code null} diterima oleh pemetaan tetapi akan
	 *                             menggagalkan proses impor
	 */
	public void setGelombangPendaftaran(GelombangPendaftaran gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Mengembalikan MIME type berkas sumber apa adanya.
	 *
	 * <p>Dirender pada kolom "Tipe" grid dan disalin ke
	 * {@code UploadBiodataCalonMahasiswaFileContent.fileMimeType} sehingga tombol <i>Download</i>
	 * dapat mengirim header tipe konten yang benar. Properti ini <b>tidak</b> punya
	 * {@code @Column}, jadi nama kolomnya mengikuti penamaan bawaan JPA atas nama properti.</p>
	 *
	 * @return MIME type berkas, atau {@code null} bila batch disimpan tanpa unggahan berkas
	 */
	public String getTipe() {
		return tipe;
	}

	/**
	 * Menyetel MIME type berkas sumber.
	 *
	 * <p>Seperti {@link #setNama(String)}, controller memanggilnya pada setiap penyimpanan dengan
	 * nilai hasil unggahan terakhir — yang berarti {@code null} bila petugas hanya menyunting batch
	 * lama.</p>
	 *
	 * @param tipe MIME type berkas; {@code null} diterima
	 */
	public void setTipe(String tipe) {
		this.tipe = tipe;
	}

	/**
	 * Mengembalikan log hasil impor terakhir.
	 *
	 * <p>Isinya teks multi-baris yang dirakit mesin impor: satu baris per program studi yang tidak
	 * dapat dicocokkan, lengkap dengan nomor pendaftaran, nama calon mahasiswa, dan asal sekolah.
	 * Teks ini ditampilkan mentah pada jendela <i>Log</i> di grid setelah {@code "\n"} ditukar
	 * dengan {@code "<br>"} dan dibungkus komponen {@code Html}.</p>
	 * <p><b>Catatan privasi:</b> karena memuat nama dan nomor pendaftaran calon mahasiswa, kolom ini
	 * berisi data pribadi meski tampak seperti sekadar log teknis. Karena isinya juga dirender sebagai
	 * HTML mentah, nilai yang berasal dari berkas Excel pihak luar ikut masuk ke halaman tanpa
	 * penyaringan.</p>
	 *
	 * @return log impor; {@code null} bila batch belum pernah dijalankan
	 */
	@Column(columnDefinition = "text")
	public String getPeringatan() {
		return peringatan;
	}

	/**
	 * Menyetel log hasil impor.
	 *
	 * <p>Hanya dipanggil mesin impor ({@code onSave()} dan tombol <i>Ulangi</i>) sesaat sebelum
	 * {@code Common.refreshUpdate(...)} menyimpan baris ini. Setiap kali impor diulang, log lama
	 * <b>ditimpa seluruhnya</b> — riwayat kegagalan sebelumnya hanya tersisa di tabel Envers.</p>
	 *
	 * @param peringatan teks log baru; string kosong berarti impor berjalan tanpa baris gagal
	 */
	public void setPeringatan(String peringatan) {
		this.peringatan = peringatan;
	}

	/**
	 * Mengembalikan penanda apakah nomor registrasi dibangkitkan sistem (bukan diambil dari kolom
	 * nomor registrasi pada berkas Excel).
	 *
	 * <p><b>Efek samping — getter ini MENULIS ke field.</b> Nilai {@code null} dinormalkan menjadi
	 * {@code false} dan hasilnya ditugaskan kembali ke {@link #generateNoRegistrasiOlehSistem}.
	 * Karena kelas ini dipetakan lewat <i>property access</i> dan ber-{@code dynamicUpdate}, membaca
	 * properti ini pada instance yang masih terkelola dalam session terbuka dapat memicu
	 * {@code UPDATE} beserta satu baris revisi Envers saat flush. Pembacaan semacam itu memang
	 * terjadi di renderer grid (kolom "Generate Otomatis No. Reg.") dan di dalam perulangan impor.</p>
	 * <p><b>Arti bisnisnya:</b> bila {@code true}, mesin impor mengganti nomor registrasi dari berkas
	 * dengan hasil {@code CommonPMB.generateNoRegistrasi(...)} <i>setelah</i> pendaftar dicari
	 * berdasarkan nomor asli — nomor asli tetap dipakai untuk pencocokan dan tetap disimpan sebagai
	 * nomor ujian. Nomor akhir selalu diberi awalan dari konfigurasi
	 * {@code prefix_no_registrasi_upload}.</p>
	 *
	 * @return {@code true} bila nomor registrasi dibangkitkan sistem; tidak pernah {@code null}
	 */
	public Boolean getGenerateNoRegistrasiOlehSistem() {
		if (generateNoRegistrasiOlehSistem == null) {
			generateNoRegistrasiOlehSistem = false;
		}
		return generateNoRegistrasiOlehSistem;
	}

	/**
	 * Menyetel penanda pembangkitan nomor registrasi otomatis.
	 *
	 * <p>Diisi dari checkbox "Generate No. Registrasi otomatis oleh sistem" pada dialog batch.
	 * Menerima {@code null}, tetapi getter akan menormalkannya menjadi {@code false} pada pembacaan
	 * berikutnya.</p>
	 *
	 * @param generateNoRegistrasiOlehSistem penanda baru; {@code null} setara {@code false}
	 */
	public void setGenerateNoRegistrasiOlehSistem(Boolean generateNoRegistrasiOlehSistem) {
		this.generateNoRegistrasiOlehSistem = generateNoRegistrasiOlehSistem;
	}

	/**
	 * Mengembalikan paket pendaftaran yang dipaksakan untuk seluruh pendaftar hasil batch ini.
	 *
	 * <p><b>Efek samping:</b> sama seperti {@link #getGelombangPendaftaran()} — hasil
	 * {@link GeneralValueObject#check(Object)} ditugaskan kembali ke field sebelum dikembalikan,
	 * sehingga instance yang keluar bisa berbeda dari proxy semula.</p>
	 * <p><b>Dampak yang jauh lebih besar dari yang terlihat:</b> nilai ini dibaca oleh
	 * {@link BiodataCalonMahasiswa#getPaket()}, yang memberi <b>prioritas</b> pada paket batch di
	 * atas paket yang tersimpan pada baris pendaftar itu sendiri. Artinya mengubah paket di sini
	 * langsung mengubah paket <i>seluruh</i> pendaftar hasil batch — termasuk yang sudah lama
	 * terdaftar — tanpa satu pun {@code UPDATE} pada tabel pendaftar, sehingga perubahan itu tidak
	 * meninggalkan jejak pada riwayat baris pendaftar. Karena paket menentukan jumlah pilihan program
	 * studi dan menjadi acuan Setting Biaya, dampaknya menyentuh sisi akademik sekaligus keuangan.
	 * Mengosongkan nilai ini ({@code null}) mengembalikan setiap pendaftar ke paketnya masing-masing;
	 * dialog batch menjelaskannya sebagai "Kosongkan paket jika berlaku tidak hanya salah satu
	 * paket".</p>
	 *
	 * @return paket yang dipaksakan untuk batch ini, atau {@code null} bila tidak dibatasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "paket")
	public Paket getPaket() {
		paket = check(paket);
		return paket;
	}

	/**
	 * Menyetel paket pendaftaran yang dipaksakan untuk batch ini.
	 *
	 * <p>Diambil dari combobox yang hanya memuat paket aktif milik perguruan tinggi terpilih (atau
	 * yang perguruan tingginya kosong). Perhatikan konsekuensi retroaktif yang dijelaskan pada
	 * {@link #getPaket()} sebelum mengubah nilai ini pada batch lama.</p>
	 *
	 * @param paket paket baru; {@code null} berarti "berlaku untuk semua paket"
	 */
	public void setPaket(Paket paket) {
		this.paket = paket;
	}

}
