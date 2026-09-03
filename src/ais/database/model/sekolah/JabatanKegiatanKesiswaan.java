package ais.database.model.sekolah;

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



import ais.database.model.GeneralValueObject;

/**
 * Katalog master <b>jabatan/status/tugas peserta dalam kegiatan kesiswaan</b>, dipetakan ke tabel
 * fisik {@code sekolah.jabatan_kegiatan_kesiswaan}. Satu baris mewakili satu peran yang dapat
 * dilekatkan pada keikutsertaan seorang siswa di sebuah kegiatan (ekstrakurikuler, lomba, seminar,
 * kepanitiaan).
 *
 * <h3>Isi sebenarnya — bukan jabatan organisasi</h3>
 * <p>Nama kelas mudah disalahartikan sebagai "jabatan pengurus organisasi siswa"
 * (Ketua/Wakil/Sekretaris/Bendahara). <b>Itu keliru.</b> Yang benar terbaca dari dua sumber di
 * dalam repo ini:</p>
 * <ul>
 *   <li><b>Label UI</b> pada {@code ais.action.master.sekolah.JabatanKegiatanKesiswaanAction} dan
 *   {@code /pages/master/sekolah/jabatan_kegiatan_kesiswaan.zul} konsisten menulis
 *   "<i>Jabatan/Status/Tugas Kegiatan Kesiswaan</i>" — tiga kata, bukan sekadar "jabatan".</li>
 *   <li><b>Data auto-seed</b> di {@code ais.common.InitDataHelper} (dijalankan hanya bila tabel
 *   masih kosong) mengisi tujuh baris berikut beserta nomor urutnya: {@code Peserta} (1),
 *   {@code Panitia} (2), {@code Narasumber} (3), {@code Juara I} (4), {@code Juara II} (5),
 *   {@code Juara III} (6), dan {@code Beregu/perorangan} (7).</li>
 * </ul>
 * <p>Jadi satu kolom yang sama menampung tiga hal berbeda sekaligus: <i>peran</i> (Peserta,
 * Panitia, Narasumber), <i>capaian</i> (Juara I/II/III), dan <i>format lomba</i>
 * (Beregu/perorangan). Sekolah bebas menambah baris lain lewat layar masternya. Jabatan pengurus
 * organisasi siswa dikelola entity lain, {@code OrganisasiSiswa} dan kerabatnya.</p>
 *
 * <h3>Posisi dalam rantai kegiatan kesiswaan</h3>
 * <p>Entity ini adalah katalog daun tingkat 3, sejajar dengan {@link SkalaKegiatanKesiswaan}:</p>
 * <pre>
 * KelompokKegiatanKesiswaan  (rumpun besar)
 *   └─ DetailKelompokKegiatanKesiswaan  (rincian aspek)
 *        ├─ many-to-many → JabatanKegiatanKesiswaan   ← kelas ini
 *        └─ many-to-many → SkalaKegiatanKesiswaan
 * </pre>
 * <p>Rujukan yang menunjuk ke kelas ini (semuanya <b>satu arah</b>; kelas ini sendiri tidak
 * menyimpan koleksi balik apa pun):</p>
 * <ul>
 *   <li>{@code DetailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans()} —
 *   {@code @ManyToMany}, menentukan jabatan mana saja yang <i>boleh dipilih</i> untuk aspek
 *   kegiatan tersebut. Ini pintu utama pemakaian entity ini.</li>
 *   <li>{@link KegiatanKesiswaan}{@code .getJabatanKegiatanKesiswaan()} — jabatan default satu
 *   kegiatan.</li>
 *   <li>{@link KegiatanKesiswaanPunyaSiswa}{@code .getJabatanKegiatanKesiswaan()} — jabatan
 *   seorang siswa pada satu kegiatan (kolom FK {@code jabatan_kegiatan_kesiswaan}).</li>
 *   <li>{@link NilaiKegiatanKesiswaan}{@code .getJabatanKegiatanKesiswaan()} — salah satu sumbu
 *   rubrik angka kredit (jabatan × skala).</li>
 * </ul>
 * <p>Hilirnya nyata: {@code ais.action.master.SertifikatAction} mencetak
 * {@code getNama()} entity ini ke parameter {@code jabatan}/{@code jabatan_di_kegiatan} pada
 * template sertifikat siswa.</p>
 *
 * <h3>Katalog global, tanpa kolom tenant</h3>
 * <p>Tidak ada kolom {@code sekolah} maupun {@code yayasan} di sini. Seluruh sekolah dan yayasan
 * dalam satu instalasi berbagi baris yang persis sama, dan menyunting sebuah baris berdampak ke
 * semua tenant sekaligus. Semua query pembacanya juga tanpa filter tenant — konsisten dengan sifat
 * katalog global, bukan kebocoran.</p>
 *
 * <h3>Hubungan dengan {@link GeneralValueObject}</h3>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun properti induknya. Karena itu
 * deklarasi ulang {@link #id}, {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} di
 * kelas ini <b>bukan duplikasi yang salah</b>, melainkan keharusan teknis: tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan ke tabel. Yang tetap diwarisi dan berfungsi
 * adalah perilaku Java murni: {@link GeneralValueObject#equals(Object)} berbasis id,
 * {@link GeneralValueObject#compareTo(GeneralValueObject)} berjenjang, serta helper
 * {@code check()}/{@code resolveLazy()}.</p>
 * <p><b>Konsekuensi yang tidak kentara:</b> properti {@code nama}, {@code keterangan}, dan
 * {@code nomorUrut} di induk memang ada, tetapi yang dipetakan Hibernate adalah <b>deklarasi ulang
 * di kelas ini</b>. Sedangkan {@code getNim()} milik induk tidak dideklarasikan ulang, jadi selalu
 * bernilai {@code null} bagi entity ini.</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ol>
 *   <li><b>Jejak audit</b> — {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)}, {@link #getTanggal_dirubah()}/
 *   {@link #setTanggal_dirubah(Date)}, dan hook {@link #onUpdate()}. Kedua setter "oleh" bersifat
 *   <i>defensif</i>: nilai kosong diabaikan diam-diam (lihat Javadoc masing-masing).</li>
 *   <li><b>Identitas</b> — {@link #getId()}/{@link #setId(Long)} dan {@link #toString()}.</li>
 *   <li><b>Isi katalog</b> — {@link #getNama()}/{@link #setNama(String)} (unik, wajib),
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 *   <li><b>Tampilan/penyaringan</b> — {@link #getNomorUrut()}/{@link #setNomorUrut(Integer)} dan
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}, keduanya getter <i>coalescing</i> yang tidak
 *   pernah mengembalikan {@code null}.</li>
 * </ol>
 *
 * <h3>Catatan operasional yang perlu diketahui pemanggil</h3>
 * <ul>
 *   <li><b>{@code nama} adalah teks bebas yang ikut masuk ke SQL native.</b> Nilai
 *   {@link #getNama()} disisipkan tanpa escaping ke dalam alias kolom berkutip ganda pada query
 *   {@code createSQLQuery} milik {@code ais.action.master.dashboard.helper.DashboardRekapKegiatanKesiswaan}
 *   (dipakai oleh {@code DashboardRekapKegiatanKesiswaanBerdasarJabatan}). Nama yang mengandung
 *   tanda kutip ganda akan merusak/mengubah query. Perlakukan kolom ini sebagai data
 *   ber-privilese, dan hindari menambah pemakaian serupa.</li>
 *   <li><b>Jangan menyimpan entity ini di {@code TreeSet}/{@code TreeMap}.</b>
 *   {@link #getNomorUrut()} tidak pernah mengembalikan {@code null} (jatuh ke {@code 1}), sehingga
 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} berhenti di cabang pertama dan
 *   mengembalikan {@code 0} untuk dua baris ber-nomor urut sama — anggota kedua akan <i>ditolak
 *   diam-diam</i> oleh {@code TreeSet.add()}. {@code DetailKelompokKegiatanKesiswaanHelper} sudah
 *   menghindarinya dengan {@code LinkedHashMap}/{@code LinkedHashSet} (lihat komentar eksplisit di
 *   sana), tetapi {@code NilaiKegiatanKesiswaanAction} masih membungkus koleksi ini ke
 *   {@code TreeSet} di dua tempat. Gunakan {@code List}+{@code Collections.sort} bila urutan
 *   dibutuhkan.</li>
 *   <li><b>Layar masternya tidak punya entri menu sendiri.</b> Satu-satunya rujukan ke
 *   {@code /pages/master/sekolah/jabatan_kegiatan_kesiswaan.zul} di seluruh repo adalah
 *   {@code KelompokKegiatanKesiswaanAction.onJabatanKegiatanKesiswaan()}, yang menyisipkannya
 *   sebagai tab. Hak CRUD atas katalog ini karena itu mengikuti hak menu induk (Kelompok Kegiatan
 *   Kesiswaan), bukan hak menunya sendiri.</li>
 * </ul>
 *
 * <h3>Jejak generator</h3>
 * <p>Javadoc asli berkas ini hanya berbunyi <i>"Bank generated by hbm2java"</i> — nama kelas
 * <b>{@code Bank}</b>, bukan {@code JabatanKegiatanKesiswaan}. Itu sisa salin-tempel dari template
 * hbm2java entity lain dan tidak berarti kelas ini ada hubungannya dengan modul bank/keuangan.
 * Baris komentar tersebut dipertahankan di bawah anotasi kelas sebagai jejak sejarah, bukan sebagai
 * deskripsi yang berlaku.</p>
 *
 * @see GeneralValueObject
 * @see DetailKelompokKegiatanKesiswaan
 * @see SkalaKegiatanKesiswaan
 * @see KegiatanKesiswaanPunyaSiswa
 * @see NilaiKegiatanKesiswaan
 */
// Komentar generator asli (dipertahankan sebagai jejak sejarah; nama "Bank" adalah sisa
// salin-tempel template hbm2java, lihat bagian "Jejak generator" pada Javadoc di atas):
// Bank generated by hbm2java
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jabatan_kegiatan_kesiswaan")



public class JabatanKegiatanKesiswaan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini <b>berbeda</b> dari entity lain di paket ini, jadi kelas ini
	 * bukan hasil salin-tempel sebuah klon (bandingkan dengan pasangan klon yatim yang pernah
	 * ditemukan di modul {@code sekolah}). Jangan diubah tanpa alasan kompatibilitas biner.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Primary key {@code sekolah.jabatan_kegiatan_kesiswaan.id}. Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; tanpa deklarasi ini kolom id tidak
	 * akan terpetakan. Lihat {@link #getId()}.
	 */
	private Long id;

	/**
	 * Nama tampilan pengguna terakhir yang menyentuh baris ini (jejak audit). Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar master. Lihat
	 * {@link #setOleh(String)} untuk perilaku defensifnya.
	 */
	private String oleh;

	/**
	 * Identitas teknis (id akun) pengguna terakhir yang menyentuh baris ini, pendamping
	 * {@link #oleh}. Lihat {@link #setOlehId(String)} untuk perilaku defensifnya.
	 */
	private String olehId;

	/**
	 * Mengembalikan id akun pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Tidak dipetakan lewat {@code @Column} eksplisit — Hibernate memakai konvensi nama properti
	 * ({@code oleh_id}). Nilainya bisa {@code null} untuk baris hasil auto-seed
	 * {@code InitDataHelper} atau baris yang ditulis lewat SQL mentah/migrasi, karena interceptor
	 * audit hanya berjalan pada alur ORM biasa.</p>
	 *
	 * @return id akun pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id akun pengubah terakhir, dengan <b>penjagaan anti-penimpaan</b>.
	 *
	 * <p><b>Perilaku non-obvious:</b> bila {@code olehId} bernilai {@code null} atau hanya berisi
	 * spasi, method ini <i>langsung keluar tanpa melakukan apa pun</i> dan tanpa melempar
	 * exception. Jejak audit lama karena itu tidak pernah bisa dihapus dengan menyetel nilai
	 * kosong; ini disengaja agar interceptor yang berjalan di luar konteks pengguna (job batch,
	 * seed) tidak menghapus informasi yang sudah ada.</p>
	 *
	 * @param olehId id akun pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan yang sama seperti
	 * {@link #setOlehId(String)}.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} atau string kosong/spasi diabaikan
	 * diam-diam, sehingga nama pengubah sebelumnya tetap bertahan.</p>
	 *
	 * @param oleh nama tampilan pengubah; nilai {@code null}/kosong diabaikan diam-diam
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah terisi (mis. baris hasil
	 *         auto-seed {@code InitDataHelper})
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil <b>otomatis oleh Hibernate</b> tepat sebelum setiap
	 * {@code UPDATE} baris ini di-flush ke database, lalu mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks pengguna aktif.
	 *
	 * <p><b>Jangan dipanggil manual.</b> Method ini {@code protected} dan hanya bermakna di dalam
	 * lifecycle persistence. Karena hanya terpasang pada {@code @PreUpdate} (bukan
	 * {@code @PrePersist}), stempel audit pada baris yang <i>baru disisipkan</i> berasal dari nilai
	 * awal field, bukan dari hook ini.</p>
	 *
	 * <p><b>Catatan:</b> pada baris fisik yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah} — waktu ubah terakhir, diinisialisasi ke waktu server saat objek
	 * dibuat lewat {@code ais.ui.util.WaktuUtil.getDate()} sehingga entity baru sudah punya stempel
	 * yang masuk akal sebelum tersimpan. Formatnya digabung dalam satu baris oleh generator kode;
	 * jangan dipecah agar diff terhadap kelas sejenis tetap rapi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu ubah terakhir baris ini.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, method ini <b>tidak</b>
	 * menjaga nilai lama: menyetel {@code null} benar-benar mengosongkan stempel waktu. Normalnya
	 * dipanggil oleh {@code AuditTimestampInterceptor}, bukan oleh kode layar.</p>
	 *
	 * @param tanggal_dirubah waktu ubah terakhir; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu ubah terakhir baris ini, disimpan sebagai {@code TIMESTAMP}.
	 *
	 * <p>Nilainya praktis tidak pernah {@code null} untuk objek yang dibuat lewat konstruktor Java
	 * karena field-nya sudah diinisialisasi ke waktu server; {@code null} hanya mungkin muncul pada
	 * baris lama hasil migrasi/SQL mentah atau bila sengaja dikosongkan lewat
	 * {@link #setTanggal_dirubah(Date)}.</p>
	 *
	 * @return waktu ubah terakhir, atau {@code null} untuk baris yang tidak pernah distempel
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "<id>-<nama>"}, mis. {@code "4-Juara I"}.
	 *
	 * <p><b>Penting:</b> format ini bukan sekadar bantuan debug. Fitur ekspor/impor Excel pada
	 * {@code KegiatanKesiswaanAction} dan {@code NilaiKegiatanKesiswaanAction} menulis sel dengan
	 * pola {@code id + "-" + nama} lalu membacanya kembali lewat
	 * {@code Common.getSheetContentAsObject(...)} untuk merekonstruksi referensi entity. Mengubah
	 * format ini akan merusak siklus unduh-ubah-unggah tersebut.</p>
	 *
	 * <p>Menggunakan field {@link #nama} secara langsung (bukan {@link #getNama()}), jadi hasilnya
	 * tidak di-{@code trim} dan bernilai {@code "null"} bila nama belum diisi. Untuk objek yang
	 * belum tersimpan, bagian id juga terbaca {@code "null"}.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama jabatan/status/tugas — isi utama katalog ini, mis. {@code "Peserta"}, {@code "Panitia"},
	 * {@code "Juara I"}. Wajib diisi dan <b>unik</b> di seluruh instalasi. Lihat {@link #getNama()}
	 * untuk peringatan pemakaiannya di SQL native.
	 */
	private String nama;

	/**
	 * Keterangan bebas, opsional. Dalam data auto-seed berisi kalimat penjelas seperti
	 * {@code "Jabatan/Status/Tugas Kegiatan Kesiswaan Sebagai Panitia"}. Hanya ditampilkan sebagai
	 * kolom informasi di layar daftar; tidak dipakai logika bisnis apa pun.
	 */
	private String keterangan;

	/**
	 * Penanda baris masih boleh dipakai. Lihat {@link #getAktif()} untuk perilaku coalescing dan
	 * catatan siapa saja yang benar-benar menulis kolom ini.
	 */
	private Boolean aktif;

	/**
	 * Nomor urut tampil. Lihat {@link #getNomorUrut()} — nilainya ikut menentukan urutan alami
	 * entity dan punya dampak pada koleksi terurut.
	 */
	private Integer nomorUrut;

	/**
	 * Konstruktor tanpa argumen yang dibutuhkan Hibernate untuk instansiasi lewat refleksi, dan
	 * dipakai langsung oleh {@code JabatanKegiatanKesiswaanAction.onAdd()} serta auto-seed
	 * {@code InitDataHelper} untuk membuat baris baru.
	 *
	 * <p>Semua field kecuali {@link #tanggal_dirubah} dibiarkan {@code null}. Perhatikan bahwa
	 * {@link #getAktif()} dan {@link #getNomorUrut()} tetap mengembalikan nilai non-null
	 * ({@code true} dan {@code 1}) untuk objek baru seperti ini.</p>
	 */
	public JabatanKegiatanKesiswaan() {
	}

	/**
	 * Primary key baris katalog, kolom {@code id} bertipe identity/serial.
	 *
	 * <p>Dianotasi {@code insertable = false} sehingga nilainya sepenuhnya ditentukan database saat
	 * {@code INSERT}; menyetel id secara manual pada objek baru tidak akan tersimpan. Nilai
	 * {@code null} artinya baris belum pernah disimpan — inilah yang dipakai
	 * {@code JabatanKegiatanKesiswaanAction} untuk membedakan mode Tambah dan Ubah (judul dialog
	 * serta pengecualian dirinya sendiri saat memeriksa duplikasi nama).</p>
	 *
	 * <p>Id ini juga menjadi dasar {@link GeneralValueObject#equals(Object)} dan menjadi kunci FK di
	 * {@link KegiatanKesiswaanPunyaSiswa}, {@link KegiatanKesiswaan}, {@link NilaiKegiatanKesiswaan},
	 * serta tabel penghubung many-to-many milik {@link DetailKelompokKegiatanKesiswaan}.</p>
	 *
	 * @return primary key baris ini, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Praktis hanya berguna untuk kode infrastruktur (deserialisasi, salinan
	 * objek); pada alur simpan normal nilai id datang dari database.
	 *
	 * @param id primary key baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama jabatan/status/tugas, sudah di-{@code trim} dari spasi tepi.
	 *
	 * <p>Kolom {@code nama} dianotasi {@code nullable = false} dan {@code unique = true}, jadi
	 * database menolak nama kosong maupun duplikat. Sisi aplikasi menjaganya lebih dulu:
	 * {@code JabatanKegiatanKesiswaanAction.onSave()} menolak nama kosong, lalu
	 * {@code checkNamaJabatanKegiatanKesiswaan()} menghitung baris ber-nama sama (mengecualikan
	 * dirinya sendiri saat mengubah) dan menampilkan peringatan bila sudah ada.</p>
	 *
	 * <p><b>Perhatian keamanan.</b> Nilai ini adalah teks bebas yang diketik pengguna, dan
	 * {@code DashboardRekapKegiatanKesiswaan.initSpreadsheet()} menyisipkannya <i>tanpa escaping</i>
	 * ke dalam alias kolom berkutip ganda pada query {@code createSQLQuery} (lewat
	 * {@code Common.getBahasaConfig(...)}, yang menerjemahkan tetapi tidak meng-escape). Nama yang
	 * mengandung {@code "} keluar dari konteks alias tersebut. Jangan menambah pemakaian nilai ini
	 * di SQL yang dirakit dengan penggabungan string.</p>
	 *
	 * <p>Nilai yang dikembalikan juga dipakai sebagai label combo pemilih jabatan, label checkbox di
	 * {@code DetailKelompokKegiatanKesiswaanHelper}, dan dicetak ke sertifikat siswa oleh
	 * {@code SertifikatAction}.</p>
	 *
	 * @return nama jabatan tanpa spasi tepi, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jabatan/status/tugas.
	 *
	 * <p>Menyimpan nilai <b>apa adanya</b> — pemangkasan spasi terjadi di sisi baca
	 * ({@link #getNama()}), bukan di sini, sehingga nilai fisik di database bisa saja masih
	 * mengandung spasi tepi. Pemanggil utama adalah {@code JabatanKegiatanKesiswaanAction.onSave()}
	 * (yang sudah memvalidasi kosong/duplikat) dan auto-seed {@code InitDataHelper}.</p>
	 *
	 * @param nama nama jabatan yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas baris ini, apa adanya (tanpa {@code trim}, boleh {@code null}).
	 *
	 * <p>Perhatikan bahwa override ini <b>menggantikan</b> {@code GeneralValueObject.getKeterangan()}
	 * yang tidak pernah mengembalikan {@code null}. Akibatnya cabang keempat
	 * {@link GeneralValueObject#compareTo(GeneralValueObject)} bisa terlewati bagi entity ini bila
	 * keterangan kosong — dalam praktik hal itu tidak pernah tercapai karena cabang pertama
	 * ({@code nomorUrut}) selalu memenuhi syarat (lihat {@link #getNomorUrut()}).</p>
	 *
	 * <p>Kolom ini murni informatif: ditampilkan sebagai kolom "Keterangan" di layar daftar dan
	 * sebagai {@code Textbox} 3 baris di dialog Tambah/Ubah. Tidak ada logika bisnis yang
	 * membacanya.</p>
	 *
	 * @return keterangan, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas. Dipanggil dari dialog Tambah/Ubah layar master dan dari auto-seed.
	 *
	 * @param keterangan keterangan baru; {@code null} diterima
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan nomor urut tampil, dengan <b>coalescing ke {@code 1}</b> bila field belum diisi.
	 *
	 * <p>Tidak dianotasi {@code @Column}; Hibernate memetakannya lewat konvensi nama properti
	 * ({@code nomor_urut}). Karena AIS memakai <i>property access</i>, nilai yang <b>ditulis</b> ke
	 * kolom saat {@code INSERT}/{@code UPDATE} adalah hasil coalescing ini, bukan {@code null} —
	 * kecuali baris tersebut masuk lewat SQL mentah/migrasi yang melewati ORM.</p>
	 *
	 * <p><b>Dua konsekuensi yang mudah terlewat:</b></p>
	 * <ol>
	 *   <li>Karena tidak pernah {@code null},
	 *   {@link GeneralValueObject#compareTo(GeneralValueObject)} <i>selalu</i> berhenti di cabang
	 *   pertama. Dua baris dengan nomor urut sama dinilai {@code 0} alias "setara", padahal
	 *   {@code equals} keduanya berbeda. Menaruh baris-baris seperti itu di {@code TreeSet}
	 *   membuat yang kedua ditolak diam-diam. {@code DetailKelompokKegiatanKesiswaanHelper} sudah
	 *   memakai {@code LinkedHashMap}/{@code LinkedHashSet} untuk menghindarinya (dengan komentar
	 *   eksplisit), tetapi {@code NilaiKegiatanKesiswaanAction} masih membungkus koleksi jabatan ke
	 *   {@code TreeSet} pada dua tempat.</li>
	 *   <li>Auto-seed memberi nomor urut unik 1–7, sementara dialog Tambah/Ubah pada layar master
	 *   <b>tidak menyediakan isian nomor urut sama sekali</b> — satu-satunya cara mengubahnya adalah
	 *   {@code Intbox} inline di kolom "No Urut" pada layar daftar. Baris baru karena itu berangkat
	 *   dengan nomor urut 1, sama dengan baris seed {@code "Peserta"}.</li>
	 * </ol>
	 *
	 * @return nomor urut tampil; {@code 1} bila belum pernah diisi (tidak pernah {@code null})
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil. Menerima {@code null} apa adanya di field, meski
	 * {@link #getNomorUrut()} akan tetap membacanya sebagai {@code 1}.
	 *
	 * <p>Satu-satunya pemanggil dari UI adalah listener {@code onChange} pada {@code Intbox} kolom
	 * "No Urut" di renderer {@code JabatanKegiatanKesiswaanAction}, yang langsung menyusulkan
	 * {@code Common.refreshUpdate(...)} — jadi perubahan tersimpan seketika tanpa menekan tombol
	 * Simpan. Pemanggil lainnya adalah auto-seed {@code InitDataHelper}.</p>
	 *
	 * @param nomorUrut nomor urut tampil baru
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan status aktif baris, dengan <b>coalescing ke {@code true}</b> bila field belum
	 * diisi — sehingga baris tanpa nilai eksplisit dianggap aktif.
	 *
	 * <p>Tidak dianotasi {@code @Column}; dipetakan lewat konvensi nama properti. Sama seperti
	 * {@link #getNomorUrut()}, property access berarti nilai hasil coalescing inilah yang ditulis
	 * ke kolom pada alur ORM biasa.</p>
	 *
	 * <p><b>Observasi mentah tentang penulisan kolom ini</b> (dicatat tanpa kesimpulan, karena
	 * proyek ini sedang mengumpulkan bukti lintas berkas):</p>
	 * <ul>
	 *   <li>{@code JabatanKegiatanKesiswaanAction.onSave()} hanya menulis {@code nama} dan
	 *   {@code keterangan}; ia tidak pernah memanggil {@link #setAktif(Boolean)}.</li>
	 *   <li>Auto-seed {@code InitDataHelper} untuk ketujuh baris awal juga tidak memanggil
	 *   {@link #setAktif(Boolean)}.</li>
	 *   <li>Satu-satunya penulis eksplisit adalah checkbox "Aktif" di renderer layar daftar, yang
	 *   dinonaktifkan bila pengguna tidak punya hak UPDATE.</li>
	 *   <li>Satu-satunya pembaca lewat query, {@code DetailKelompokKegiatanKesiswaanHelper}, memakai
	 *   penyaring <i>toleran</i> {@code (aktif IS NULL OR aktif = true)}, sehingga baris tanpa nilai
	 *   tetap muncul dalam daftar pilihan.</li>
	 * </ul>
	 * <p>Nilai kolom yang sesungguhnya tersimpan di database belum diverifikasi secara empiris di
	 * sini.</p>
	 *
	 * @return {@code true} bila baris aktif atau belum pernah diisi; {@code false} hanya bila
	 *         dinonaktifkan secara eksplisit (tidak pernah {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif baris.
	 *
	 * <p>Satu-satunya pemanggil di seluruh repo adalah listener {@code onCheck} checkbox "Aktif"
	 * pada renderer {@code JabatanKegiatanKesiswaanAction}, yang langsung menyusulkan
	 * {@code Common.refreshSaveOrUpdate(...)} — perubahan tersimpan seketika. Checkbox tersebut
	 * di-{@code setDisabled} bila pengguna tidak memegang hak UPDATE.</p>
	 *
	 * <p>Menonaktifkan sebuah baris <b>tidak</b> menghapus referensi yang sudah terlanjur menunjuk
	 * ke sana: {@link KegiatanKesiswaanPunyaSiswa}, {@link KegiatanKesiswaan}, dan
	 * {@link NilaiKegiatanKesiswaan} yang sudah memakai baris ini tetap menampilkan namanya.
	 * Efeknya hanya pada daftar pilihan baru.</p>
	 *
	 * @param aktif status aktif baru; {@code null} diterima di field dan akan terbaca sebagai
	 *              {@code true} oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	
}
