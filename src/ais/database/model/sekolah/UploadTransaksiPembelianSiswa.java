package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.GeneralValueObject;

/**
 * Entity <b>kepala (header) satu berkas unggahan massal transaksi belanja siswa</b>, dipetakan ke
 * tabel {@code sekolah.upload_transaksi_pembelian_siswa}. Satu baris mewakili satu kali proses
 * impor: keterangan/hasil prosesnya ({@link #getKeterangan()}), lokasi berkas sumber yang diunggah
 * ({@link #getPathFile()}), dan kapan unggahan itu terjadi ({@link #getWaktu()}). Isi transaksinya
 * sendiri tidak disimpan di sini melainkan pada baris-baris {@link PembelianSiswa} yang menunjuk
 * balik ke baris ini.
 *
 * <h2>Domain yang terverifikasi dari kode</h2>
 * <p>Peran "log unggahan batch" bukan dugaan dari namanya, melainkan dibaca dari satu-satunya
 * relasi yang menyentuh kelas ini:
 * {@link PembelianSiswa#getUploadTransaksiPembelianSiswa()} adalah {@code @ManyToOne} dengan
 * {@code @JoinColumn(name = "upload_transaksi_pembelian_siswa_id", nullable = true)}. Artinya
 * <b>arah relasinya</b>: BANYAK baris belanja siswa menunjuk ke SATU baris entity ini, dan kolom
 * FK-nya berada di tabel {@code sekolah.pembelian_siswa} — bukan sebaliknya. Boleh {@code null}
 * pada sisi belanja, sehingga transaksi yang direkam satu per satu (mis. langsung dari kasir
 * kantin) tidak punya induk unggahan, sedangkan transaksi hasil impor massal berbagi satu induk.
 * Kombinasi ketiga kolom bisnisnya ({@code keterangan} wajib sepanjang 1000 karakter,
 * {@code path_file} opsional, {@code waktu} wajib) adalah bentuk klasik sebuah <i>catatan
 * pekerjaan impor</i>: apa berkasnya, kapan diproses, dan ringkasan/pesan hasilnya.
 *
 * <p>Karena {@link PembelianSiswa} adalah sisi PENGELUARAN tabungan/deposit siswa di kantin
 * sekolah (dibuktikan lewat {@code webapp/report/sekolah/pembayaran/laporan_saldo_rinci.jrxml}
 * yang merangkai {@code pembelian_siswa} &rarr; {@code pembelian_siswa_detail} &rarr;
 * {@code produk} &rarr; {@code kantin}), maka entity ini secara konsep adalah <b>jangkar audit
 * untuk pemasukan data KEUANGAN secara borongan</b>: satu berkas (mis. rekap mesin kasir/EDC
 * kantin atau ekspor sistem pihak ketiga) yang sekali proses melahirkan banyak baris pengurang
 * saldo siswa.
 *
 * <h2>PENTING: fitur unggah ini TIDAK PERNAH DIIMPLEMENTASIKAN</h2>
 * <p>Hasil penelusuran seluruh pohon sumber, {@code webapp} dan berkas konfigurasi (3 Sep 2026):
 * <b>tidak ada satu pun</b> baris kode di luar berkas ini yang menyebut tipe
 * {@code UploadTransaksiPembelianSiswa} selain deklarasi relasi di {@link PembelianSiswa}. Tidak
 * ada {@code new UploadTransaksiPembelianSiswa()}, tidak ada {@code save}/{@code saveOrUpdate}/
 * {@code persist}/{@code merge}, tidak ada {@code createCriteria}/HQL yang menargetkannya, tidak
 * ada kelas {@code Action}, layar {@code .zul}/{@code .jsp}, servlet unggah, route API, laporan
 * Jasper, maupun migrasi tenant ({@code ais.service.tenant.*}) yang menyentuhnya. Bahkan properti
 * {@code pathFile} — beserta literal nama kolom {@code "path_file"} — hanya muncul di berkas ini
 * pada SELURUH pohon sumber; nol pemanggil {@link #getPathFile()} maupun
 * {@link #setPathFile(String)}.
 *
 * <p>Kelasnya tetap terdaftar di {@code hibernate.cfg.xml} (baris 2306) sehingga Hibernate tetap
 * memvalidasi/menurunkan DDL-nya dan Envers tetap menyiapkan tabel {@code _aud}-nya, tetapi tidak
 * ada aliran data yang mengisinya: kolom {@code pembelian_siswa.upload_transaksi_pembelian_siswa_id}
 * selamanya {@code NULL}. Hal yang sama berlaku bagi kedua kerabatnya — {@link PembelianSiswa} dan
 * {@link Kantin} juga nol referensi di luar berkas modelnya sendiri.
 *
 * <h2>Bandingkan dengan keluarga "log unggah" yang HIDUP</h2>
 * <p>AIS punya dua entity sejenis yang benar-benar dipakai:
 * {@code ais.database.model.UploadVirtualAccount} (unggah tagihan virtual account bank, dipakai
 * belasan servlet bank) dan {@code ais.database.model.UploadBiodataCalonMahasiswa} (unggah biodata
 * calon mahasiswa). Keduanya berbeda desain dari berkas ini dalam satu hal penting: <b>keduanya
 * tidak menyimpan path berkas sama sekali</b> melainkan menaruh isi berkas di entity pendamping
 * ({@code ais.database.model.file.UploadVirtualAccountFileContent} dan
 * {@code ais.database.model.file.UploadBiodataCalonMahasiswaFileContent}), dan keduanya punya
 * kolom-kolom kendali proses ({@code jenisUpload}, {@code terupload}, {@code peringatan}) yang di
 * sini tidak ada. Entity ini adalah generasi desain yang lebih tua dan ditinggalkan sebelum
 * sempat dipakai; tidak ada kelas {@code UploadTransaksiPembelianSiswaFileContent}.
 *
 * <h2>Hasil pemeriksaan keamanan data keuangan (per 3 Sep 2026)</h2>
 * <p>Karena entity ini menyentuh data finansial siswa, jalur aksesnya diperiksa khusus. Ringkasan
 * temuannya sebagian besar <b>negatif/menenangkan</b>, dengan sisanya berupa risiko LATEN yang baru
 * menggigit bila modul ini kelak dihidupkan:</p>
 * <ol>
 *   <li><b>Siapa yang boleh mengunggah — tidak ada gerbang, karena tidak ada layarnya.</b> Tidak
 *       ada kelas {@code Action}/{@code Helper} pengelola sama sekali, sehingga tidak ada
 *       {@code checkPrevilages} yang salah maupun yang benar untuk dinilai. Ini BUKAN temuan
 *       broken access control; ia hanya berarti pemeriksaan hak akses harus dirancang dari nol
 *       bila fitur ini dibangun.</li>
 *   <li><b>Tidak ada cakupan tenant sama sekali (risiko laten, bukan fail-open aktif).</b> Berbeda
 *       dari {@link PembelianSiswa} yang punya {@code sekolah_id}/{@code yayasan_id}, entity ini
 *       <b>tidak punya kolom sekolah maupun yayasan</b>. Secara struktural tidak mungkin menyaring
 *       daftar unggahan per sekolah/yayasan tanpa menempuh {@code join} ke baris belanja anaknya.
 *       Bila sebuah layar daftar dibuat mengikuti pola umum repo (kriteria dibangun dari kolom
 *       entity), layar itu akan menampilkan seluruh riwayat unggahan seluruh instalasi kepada
 *       operator sekolah mana pun.</li>
 *   <li><b>Tidak ada pengaman anti-proses-ulang (risiko duplikasi transaksi finansial, laten).</b>
 *       Kelas ini tidak punya flag status ({@code sudahDiproses}/{@code terupload}), tidak punya
 *       hash/checksum berkas, dan tidak ada {@code unique constraint} pada {@code path_file}
 *       maupun {@code waktu}. Tidak ada pula {@code @OneToMany} balik ke {@link PembelianSiswa}
 *       (lihat kuirk 6) sehingga tidak ada cara murah memeriksa "berkas ini sudah menghasilkan
 *       berapa baris". Konsekuensinya, implementasi impor apa pun yang dibangun di atas skema ini
 *       tidak akan otomatis menolak berkas yang sama diunggah dua kali — dan setiap pengulangan
 *       berarti pengurangan ganda saldo tabungan siswa. Perlu diingat pula bahwa relasi dari sisi
 *       belanja memakai {@code cascade = PERSIST, MERGE}, sehingga baris header BARU tercipta
 *       diam-diam mengikuti baris belanja yang menunjuknya, bukan lewat penyimpanan eksplisit yang
 *       mudah diberi penjagaan.</li>
 *   <li><b>Pewarisan hak lewat menu induk: TIDAK BERLAKU di sini.</b> Entity ini tidak muncul di
 *       {@code MenuInitializer} maupun paket menu mana pun, jadi ia bukan instance baru dari pola
 *       tersebut.</li>
 *   <li><b>Generic CRUD: terdaftar di inventaris, tetapi tidak dapat dirutekan.</b> Baris 1231
 *       {@code webapp/WEB-INF/generic-crud/manifests/general_value_object_inventory.csv} mencatatnya
 *       sebagai {@code ELIGIBLE_METADATA_FIRST} dengan status aktif {@code False}. Pendaftaran
 *       runtime lewat {@code GenericCrudDefinitionRegistry.tryAutoRegister(...)} hanya terjadi bila
 *       ada scaffold JSP yang memasok kandidat kelas dari sisi server — dan tidak ada JSP semacam
 *       itu untuk entity ini. Sebagai lapis tambahan, {@code GenericCrudAutoDefinitionFactory}
 *       memblokir field yang namanya mengandung {@code "path"}, sehingga {@link #getPathFile()}
 *       tidak akan pernah masuk formulir hasil generate sekali pun definisinya diaktifkan.</li>
 *   <li><b>Servlet data generik {@code /Data}.</b> Sebagai entity terdaftar Hibernate, kelas ini
 *       terjangkau {@code ais.action.servlet.Data} yang me-{@code Class.forName} nama kelas dari
 *       payload. Aksi TULIS sudah ditutup untuk pemanggil anonim (baris 440-446), tetapi aksi BACA
 *       ({@code daftar}/{@code load}/{@code cari}/{@code sql}) masih lolos bila klien mengirim
 *       {@code tanpaLogin=true} (baris 448-455). Ini bukan temuan baru melainkan penguat masalah
 *       yang sudah tercatat pada audit endpoint {@code /Data}; risiko datanya nihil selama tabel
 *       ini kosong, tetapi akan menjadi kebocoran metadata kebiasaan belanja anak di bawah umur
 *       bila modul ini dihidupkan.</li>
 * </ol>
 *
 * <h2>Pengelompokan anggota</h2>
 * <ul>
 *   <li><b>Jejak audit warisan {@link GeneralValueObject}</b> — {@link #getOleh()}/
 *       {@link #setOleh(String)}, {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@code @PreUpdate}.</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Isi berkas unggahan</b> — {@link #getKeterangan()} (wajib),
 *       {@link #getPathFile()} (opsional), {@link #getWaktu()} (wajib).</li>
 *   <li><b>Konstruktor</b> — {@link #UploadTransaksiPembelianSiswa()} (dipakai Hibernate) dan
 *       {@link #UploadTransaksiPembelianSiswa(long, String, Date)} (varian ringkas hbm2java).</li>
 * </ul>
 *
 * <h2>Kuirk dan hal non-obvious</h2>
 * <ol>
 *   <li><b>{@link #getKeterangan()} MEMBALIK kontrak kelas dasar.</b>
 *       {@link GeneralValueObject#getKeterangan()} menjamin nilai non-{@code null} (ia
 *       mengembalikan {@code ""} bila field-nya kosong), sedangkan override di kelas ini
 *       mengembalikan field apa adanya sehingga <b>dapat bernilai {@code null}</b>. Pemanggil kode
 *       generik yang mempercayai kontrak kelas dasar — mis. langsung merantai
 *       {@code getKeterangan().trim()} — akan terkena {@code NullPointerException} pada object
 *       kelas ini. Risikonya laten selama tidak ada pemanggil, tetapi pola serupa sudah pernah
 *       menimbulkan NPE reflektif pada entity lain di repo ini.</li>
 *   <li><b>Dampak pembalikan itu pada pengurutan.</b> {@link GeneralValueObject#compareTo}
 *       memakai empat kunci berurutan: {@code nomorUrut}, {@code nim}, {@code nama}, lalu
 *       {@code keterangan}. Ketiga kunci pertama TIDAK dipetakan di kelas ini (kolomnya tidak ada,
 *       nilainya selalu {@code null}), jadi seluruh pengurutan bergantung pada {@code keterangan}
 *       saja. Karena override di atas boleh mengembalikan {@code null}, cabang keempat itu pun
 *       bisa terlewat sehingga {@code compareTo} mengembalikan {@code 0} untuk sembarang pasangan
 *       baris. Setiap {@code TreeSet}/{@code SortedSet} yang menampung entity ini akan menciut
 *       menjadi satu elemen — pola penciutan yang sama sudah ditemukan pada entity lain di repo
 *       ini. Bahkan bila {@code keterangan} terisi, dua unggahan berketerangan sama tetap
 *       dianggap identik.</li>
 *   <li><b>{@code toString()} mencetak literal {@code "null"}.</b> Kelas ini tidak meng-override
 *       {@link GeneralValueObject#toString()}, yang berbunyi {@code getKode() + " - " + getNama()};
 *       {@code kode} dan {@code nama} sama-sama tidak dipetakan di sini sehingga selalu
 *       {@code null}. Hasilnya string {@code "null"} — yang persis itulah yang akan tampil bila
 *       entity ini pernah dimasukkan ke {@code Combobox}/{@code Listcell} ZK.</li>
 *   <li><b>Konstruktor ringkas menerima {@code id} yang akan diabaikan.</b>
 *       {@link #UploadTransaksiPembelianSiswa(long, String, Date)} menyetel {@link #id}, padahal
 *       {@link #getId()} dipetakan {@code insertable = false} dengan {@code GenerationType.IDENTITY}
 *       — nilai itu tidak pernah ikut pada {@code INSERT}. Parameternya juga primitif {@code long}
 *       sehingga tidak bisa dipakai untuk membuat object baru "tanpa id".</li>
 *   <li><b>{@code path_file} menyimpan LOKASI, bukan isi.</b> Tidak ada kolom biner maupun entity
 *       {@code FileContent} pendamping, sehingga berkas sumber hidup di luar basis data. Nilainya
 *       bebas dan tidak divalidasi kelas ini; bila kelak ada kode yang membuka berkas berdasarkan
 *       kolom ini, jalur itu wajib memvalidasi path (risiko path traversal / pembacaan berkas
 *       server) karena entity tidak menjaminnya sama sekali.</li>
 *   <li><b>Relasi searah — tidak ada koleksi balik.</b> Kelas ini tidak punya
 *       {@code @OneToMany List&lt;PembelianSiswa&gt;}, jadi dari satu baris unggahan tidak ada cara
 *       Java untuk menelusuri transaksi apa saja yang dihasilkannya; harus lewat query eksplisit
 *       atas {@code pembelian_siswa.upload_transaksi_pembelian_siswa_id}. Efek sampingnya: menghapus
 *       baris unggahan tidak akan pernah ikut menghapus transaksinya (tidak ada cascade dari sisi
 *       ini), dan sebaliknya {@code cascade = PERSIST, MERGE} di sisi {@link PembelianSiswa}
 *       membuat header ini ikut tersimpan mengikuti baris belanja pertama yang menunjuknya.</li>
 *   <li><b>Hanya ada {@code @PreUpdate}, tanpa {@code @PrePersist}.</b> Baris BARU tidak mendapat
 *       pengisian otomatis {@link #oleh}/{@link #olehId} — jejak "siapa yang mengunggah" baru
 *       terisi pada perubahan BERIKUTNYA, bukan pada saat unggahan dibuat. Untuk sebuah tabel yang
 *       tujuannya adalah audit pemasukan data keuangan, ini kelemahan desain yang layak dicatat.
 *       Yang otomatis hanyalah {@link #tanggal_dirubah} lewat nilai awal field.</li>
 *   <li><b>{@code waktu} tidak punya nilai bawaan.</b> Berbeda dari {@link #tanggal_dirubah} yang
 *       langsung terisi waktu sekarang saat object dibuat, {@code waktu} tetap {@code null} sampai
 *       {@link #setWaktu(Date)} dipanggil padahal kolomnya {@code nullable = false} — penyimpanan
 *       tanpa memanggil setter itu berujung {@code PropertyValueException} saat flush.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate} + {@code @Audited}.</b> Hibernate hanya
 *       menyertakan kolom yang benar-benar berubah pada {@code INSERT}/{@code UPDATE}, dan Envers
 *       menyiapkan tabel revisi {@code upload_transaksi_pembelian_siswa_aud} — biaya skema yang
 *       tetap ditanggung meskipun tabelnya tidak pernah terisi.</li>
 * </ol>
 *
 * <h2>Catatan warisan {@link GeneralValueObject}</h2>
 * <p>Kelas dasar {@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} — ia POJO abstrak biasa, sehingga Hibernate tidak memetakan properti
 * miliknya. Karena itu deklarasi ulang {@code id}, {@code oleh}, {@code olehId},
 * {@code tanggal_dirubah} dan {@code keterangan} di berkas ini <b>bukan duplikasi keliru</b>,
 * melainkan keharusan teknis agar kolom-kolom tersebut ikut terpetakan. Menghapusnya akan
 * menghilangkan kolom dari tabel. Sebaliknya, utilitas yang tidak berkaitan dengan pemetaan —
 * {@link GeneralValueObject#check(Object)}, {@link GeneralValueObject#compareTo(GeneralValueObject)},
 * {@link GeneralValueObject#toString()} — tetap diwarisi apa adanya; kelas ini tidak punya relasi
 * lazy sehingga {@code check(...)} tidak dipakai di sini.</p>
 *
 * @see GeneralValueObject
 * @see PembelianSiswa
 * @see Kantin
 * @see Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "upload_transaksi_pembelian_siswa", schema = "sekolah")
public class UploadTransaksiPembelianSiswa extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, diwarisi dari kontrak {@link java.io.Serializable} milik
	 * {@link GeneralValueObject}.
	 *
	 * <p>Nilainya dibangkitkan sekali oleh perkakas dan harus dipertahankan apa adanya: object
	 * entity ikut terserialisasi ketika ZK menyimpan state desktop/sesi ke disk atau ketika baris
	 * ini masuk cache tingkat kedua. Mengubah angka ini membuat state lama tidak dapat dibaca
	 * kembali ({@code InvalidClassException}).</p>
	 */
	private static final long serialVersionUID = -3085494845496684413L;

	/** Kunci utama baris, dibangkitkan basis data. Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor} lewat callback {@code @PreUpdate}. Lihat
	 * {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * Identitas (user id) pengguna terakhir yang mengubah baris ini, pendamping {@link #oleh}.
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return user id pengubah terakhir, atau {@code null} bila baris belum pernah diperbarui
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} atau string kosong (setelah {@code trim})
	 * <b>diabaikan diam-diam</b> — method langsung {@code return} tanpa menyentuh field. Jadi nilai
	 * lama tidak pernah bisa dikosongkan lewat setter ini, dan pemanggil yang mengira berhasil
	 * menghapus jejak audit akan keliru.</p>
	 *
	 * @param olehId user id pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Perilaku identik {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit bersifat "hanya tambah/timpa", tidak pernah bisa dikosongkan.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Dua anggota yang oleh perkakas pembangkit ditulis pada SATU baris fisik; dokumentasi
	 * keduanya digabung di sini agar susunan baris berkas tidak berubah.
	 *
	 * <p><b>1. {@code onUpdate()} — callback JPA {@code @PreUpdate}.</b> Dipanggil kontainer
	 * persistence tepat sebelum pernyataan {@code UPDATE} dikirim ke basis data, lalu
	 * mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #oleh}/{@link #olehId} dari pengguna sesi aktif dan menyegarkan
	 * {@link #tanggal_dirubah}. <b>Tidak ada {@code @PrePersist} berpasangan</b>, sehingga baris
	 * BARU — yakni justru saat sebuah unggahan transaksi dibuat — tidak mendapat pengisian
	 * otomatis apa pun; lihat kuirk 7 pada dokumentasi kelas.</p>
	 *
	 * <p><b>2. Field {@code tanggal_dirubah}.</b> Stempel waktu perubahan terakhir, diinisialisasi
	 * saat object dibuat memakai {@code ais.ui.util.WaktuUtil.getDate()} (jam server aplikasi,
	 * bukan jam basis data). Jangan dikelirukan dengan {@link #waktu} yang mencatat kapan
	 * unggahannya terjadi. Lihat {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Normalnya tidak perlu dipanggil kode aplikasi: {@code AuditTimestampInterceptor} sudah
	 * memperbaruinya otomatis lewat callback {@code @PreUpdate}. Nilai {@code null} diterima apa
	 * adanya (tidak ada penolakan seperti pada {@link #setOleh(String)}).</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@link TemporalType#TIMESTAMP} ke kolom bernama sama
	 * ({@code tanggal_dirubah}) karena tidak ada {@code @Column} yang menimpanya.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Keterangan/ringkasan hasil proses unggahan. Wajib terisi ({@code keterangan nullable = false},
	 * maksimal 1000 karakter). Field ini <b>membayangi</b> field bernama sama milik
	 * {@link GeneralValueObject}; lihat {@link #getKeterangan()}.
	 */
	private String keterangan;

	/**
	 * Lokasi berkas sumber yang diunggah, disimpan sebagai path teks (bukan isi berkas). Opsional
	 * ({@code path_file} tanpa {@code nullable = false}). Lihat {@link #getPathFile()}.
	 */
	private String pathFile;

	/**
	 * Waktu terjadinya unggahan. Wajib terisi ({@code waktu nullable = false}) dan TIDAK punya nilai
	 * bawaan. Lihat {@link #getWaktu()}.
	 */
	private Date waktu;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate untuk membuat instance saat memuat baris
	 * dari basis data.
	 *
	 * <p>Object hasil konstruktor ini punya {@link #keterangan} dan {@link #waktu} bernilai
	 * {@code null}, padahal kedua kolomnya dipetakan {@code nullable = false} — menyimpannya tanpa
	 * memanggil kedua setter akan gagal dengan {@code PropertyValueException} saat flush.</p>
	 */
	public UploadTransaksiPembelianSiswa() {
	}

	/**
	 * Konstruktor ringkas bawaan hbm2java yang menyetel seluruh kolom WAJIB sekaligus.
	 *
	 * <p><b>Non-obvious:</b> parameter {@code id} praktis tidak berguna. Kolomnya dipetakan
	 * {@code @GeneratedValue(strategy = IDENTITY)} dengan {@code insertable = false}, sehingga nilai
	 * yang disetel di sini tidak pernah ikut dikirim pada {@code INSERT} — urutan basis data selalu
	 * menang. Tipenya juga primitif {@code long}, jadi konstruktor ini tidak bisa dipakai untuk
	 * membuat object baru yang id-nya sengaja dikosongkan; untuk itu pakai
	 * {@link #UploadTransaksiPembelianSiswa()} lalu setter masing-masing.</p>
	 *
	 * <p>Perhatikan {@link #pathFile} sengaja tidak termasuk — perkakas pembangkit hanya
	 * memasukkan kolom {@code nullable = false} ke varian ini, yang sekaligus menjadi bukti bahwa
	 * path berkas memang opsional pada desain aslinya.</p>
	 *
	 * @param id         kunci utama; diabaikan pada {@code INSERT} (lihat penjelasan di atas)
	 * @param keterangan keterangan/ringkasan hasil unggahan; wajib
	 * @param waktu      waktu terjadinya unggahan; wajib
	 */
	public UploadTransaksiPembelianSiswa(long id, String keterangan, Date waktu) {
		this.id = id;
		this.keterangan = keterangan;
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan dipetakan {@code insertable = false},
	 * artinya nilai yang di-{@link #setId(Long)} secara manual TIDAK akan ikut dikirim pada
	 * {@code INSERT} — urutan/sequence basis data selalu menang. Inilah nilai yang disimpan kolom
	 * {@code pembelian_siswa.upload_transaksi_pembelian_siswa_id} pada setiap baris belanja hasil
	 * unggahan ini.</p>
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
	 * Menyetel kunci utama baris ini.
	 *
	 * <p>Dipakai Hibernate saat memuat baris. Pemanggilan manual pada object baru tidak berpengaruh
	 * pada {@code INSERT} (lihat {@link #getId()}), tetapi tetap menentukan baris mana yang
	 * di-{@code UPDATE} bila object dianggap sudah persisten.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan keterangan/ringkasan hasil proses unggahan.
	 *
	 * <p>Kolomnya {@code nullable = false} dengan panjang 1000 karakter — ukuran yang jauh lebih
	 * lapang daripada sekadar judul, dan mengisyaratkan isinya dimaksudkan sebagai pesan hasil
	 * proses (mis. "120 transaksi berhasil, 3 baris ditolak") ketimbang label pendek.</p>
	 *
	 * <p><b>KUIRK PENTING — override ini MEMBALIK kontrak kelas dasar.</b>
	 * {@link GeneralValueObject#getKeterangan()} menjamin hasil non-{@code null} (mengembalikan
	 * {@code ""} bila field kosong), sedangkan method ini mengembalikan field apa adanya sehingga
	 * <b>bisa {@code null}</b>. Kode generik yang mempercayai kontrak kelas dasar dan langsung
	 * merantai pemanggilan (mis. {@code getKeterangan().trim()}) akan terkena
	 * {@code NullPointerException} pada object kelas ini.</p>
	 *
	 * <p>Efek lanjutannya menyentuh pengurutan: {@link GeneralValueObject#compareTo} memakai
	 * {@code keterangan} sebagai kunci urut TERAKHIR, sementara tiga kunci sebelumnya
	 * ({@code nomorUrut}, {@code nim}, {@code nama}) tidak dipetakan di kelas ini dan selalu
	 * {@code null}. Bila {@code keterangan} juga {@code null}, {@code compareTo} mengembalikan
	 * {@code 0} untuk sembarang pasangan baris sehingga {@code TreeSet}/{@code SortedSet} apa pun
	 * yang menampung entity ini menciut menjadi satu elemen.</p>
	 *
	 * @return keterangan hasil unggahan; <b>boleh {@code null}</b> berbeda dari kontrak kelas dasar
	 */
	@Column(name = "keterangan", nullable = false, length = 1000)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan/ringkasan hasil proses unggahan.
	 *
	 * <p>Tanpa validasi apa pun: {@code null} diterima (dan akan ditolak basis data saat flush
	 * karena kolomnya {@code nullable = false}), dan string yang lebih panjang dari 1000 karakter
	 * baru gagal di tingkat basis data, bukan di sini.</p>
	 *
	 * <p>Override ini juga membuat field {@code keterangan} milik {@link GeneralValueObject}
	 * <b>tidak pernah terisi</b> — seluruh baca/tulis dialihkan ke field milik kelas ini. Karena
	 * semua pembaca melewati getter (termasuk Hibernate yang memakai <i>property access</i>),
	 * pembayangan field itu tidak menimbulkan ketidakcocokan nilai.</p>
	 *
	 * @param keterangan keterangan hasil unggahan; tidak boleh {@code null} bila baris akan disimpan
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan lokasi berkas sumber yang diunggah.
	 *
	 * <p>Yang disimpan adalah <b>path teks</b>, bukan isi berkas: tidak ada kolom biner maupun
	 * entity {@code FileContent} pendamping di kelas ini — berbeda dari keluarga log unggah yang
	 * masih hidup ({@code UploadVirtualAccount}, {@code UploadBiodataCalonMahasiswa}) yang menaruh
	 * isinya di basis data. Kolomnya boleh {@code null}, dan memang tidak termasuk dalam
	 * konstruktor ringkas {@link #UploadTransaksiPembelianSiswa(long, String, Date)}.</p>
	 *
	 * <p><b>Status nyata:</b> nol pemanggil. Baik nama properti {@code pathFile} maupun literal
	 * nama kolom {@code "path_file"} hanya muncul di berkas ini pada seluruh pohon sumber. Bila
	 * kelak ada kode yang membuka berkas berdasarkan nilai ini, jalur itu wajib memvalidasi path
	 * sendiri (risiko path traversal/pembacaan berkas server) — entity ini tidak menyaring apa
	 * pun.</p>
	 *
	 * @return path berkas sumber unggahan, atau {@code null} bila tidak dicatat
	 */
	@Column(name = "path_file")
	public String getPathFile() {
		return this.pathFile;
	}

	/**
	 * Menyetel lokasi berkas sumber yang diunggah.
	 *
	 * <p>Tanpa validasi: nilai apa pun diterima apa adanya, termasuk {@code null} dan path relatif
	 * yang mengandung {@code ".."}. Lihat catatan keamanan pada {@link #getPathFile()}.</p>
	 *
	 * @param pathFile path berkas sumber; boleh {@code null}
	 */
	public void setPathFile(String pathFile) {
		this.pathFile = pathFile;
	}

	/**
	 * Mengembalikan waktu terjadinya unggahan.
	 *
	 * <p>Dipetakan {@link TemporalType#TIMESTAMP} sehingga menyimpan jam sampai detik; inilah
	 * penanda periode sebuah batch impor, dan calon kunci urut/penyaring alami bagi layar daftar
	 * unggahan bila kelak dibuat. Jangan dikelirukan dengan {@link #getTanggal_dirubah()} (jejak
	 * audit perubahan baris) maupun dengan {@code PembelianSiswa.tanggal} (waktu transaksi belanja
	 * yang sesungguhnya, yang dipakai seluruh penyaringan periode laporan saldo).</p>
	 *
	 * <p><b>Kuirk:</b> berbeda dari {@link #tanggal_dirubah}, field ini tidak diinisialisasi ke
	 * waktu sekarang. Object baru punya {@code waktu} {@code null} padahal kolomnya
	 * {@code nullable = false}, sehingga penyimpanan tanpa memanggil {@link #setWaktu(Date)}
	 * berujung {@code PropertyValueException}.</p>
	 *
	 * @return waktu unggahan; menurut pemetaan tidak boleh {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu", nullable = false, length = 29)
	public Date getWaktu() {
		return this.waktu;
	}

	/**
	 * Menyetel waktu terjadinya unggahan.
	 *
	 * <p>Wajib dipanggil sebelum baris disimpan (lihat {@link #getWaktu()}). Tidak ada validasi:
	 * waktu di masa depan maupun {@code null} sama-sama diterima di tingkat Java.</p>
	 *
	 * @param waktu waktu unggahan
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

}
