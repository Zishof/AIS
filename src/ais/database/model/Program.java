package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Entity MASTER <b>program perkuliahan</b> &mdash; jalur/kelas penyelenggaraan kuliah seperti
 * "Reguler", "Non Reguler", "Karyawan", "Kelas Jauh", "Ekstensi", dan sejenisnya. Dipetakan ke
 * tabel {@code public.program}.
 *
 * <h3>Bukan program studi, bukan jenjang</h3>
 * <p>Meski namanya "Program", entity ini <b>BUKAN</b> program studi (itu {@link Jurusan}) dan
 * <b>BUKAN</b> jenjang pendidikan (itu {@link Jenjang}). Ia adalah dimensi ketiga yang berdiri
 * sendiri: satu program studi yang sama bisa diselenggarakan dalam beberapa program (mis. Teknik
 * Informatika kelas Reguler dan kelas Karyawan), dengan jumlah tahapan pembayaran, kurikulum,
 * paket matakuliah, dan aturan batas IPK yang bisa berbeda per program.</p>
 *
 * <h3>Kunci primer adalah NAMA, bukan id angka</h3>
 * <p>Berbeda dari mayoritas entity AIS yang memakai surrogate key {@code id}, entity ini memakai
 * <b>natural key</b>: {@link #getNama()} sendiri yang beranotasi {@code @Id} pada kolom
 * {@code nama} ({@code length = 50}, {@code unique}, {@code nullable = false}). Konsekuensinya:</p>
 * <ul>
 *   <li>seluruh referensi ke program &mdash; baik FK sejati maupun kolom teks &mdash; menyimpan
 *   <b>teks nama program</b>, bukan angka;</li>
 *   <li>mengubah nama program berarti mengubah kunci primer, yang akan memutus semua referensi.
 *   Karena itu {@code ProgramAction#init(Program)} sengaja mengunci textbox nama
 *   ({@code nama.setDisabled(...)}) begitu barisnya sudah pernah tersimpan &mdash; rename
 *   praktis tidak mungkin lewat UI;</li>
 *   <li>{@code ProgramDao}/{@code ProgramDaoImpl} dideklarasikan sebagai
 *   {@code GenericDao<Program, Long>} padahal tipe kunci sebenarnya {@link String}. Parameter tipe
 *   itu tidak dipakai karena {@code GenericHibernateDao#load(Serializable)} menerima
 *   {@link java.io.Serializable}, sehingga {@code programDao.load(program.getNama())} tetap
 *   berjalan &mdash; deklarasi {@code Long} adalah sisa template yang menyesatkan pembaca.</li>
 * </ul>
 *
 * <h3>Penamaan kolom yang menyesatkan</h3>
 * <p>Label di layar {@code /pages/master/program.zul} tidak sejalan dengan nama properti:</p>
 * <ul>
 *   <li>{@link #getNum()} &rarr; label <b>"ID Program"</b> (angka, dipakai sebagai digit NIM);</li>
 *   <li>{@link #getNama()} &rarr; label <b>"Kode Program"</b> &mdash; jadi properti bernama
 *   "nama" sebenarnya berperan sebagai KODE sekaligus kunci primer;</li>
 *   <li>{@link #getNamaBaru()} &rarr; label <b>"Nama Program"</b> &mdash; inilah nama tampil yang
 *   sebenarnya dilihat pengguna di combobox;</li>
 *   <li>{@link #getNamaEn()} &rarr; label <b>"Nama Program (English)"</b>.</li>
 * </ul>
 *
 * <h3>Keanggotaan mahasiswa &harr; program: BERBASIS TEKS (terverifikasi)</h3>
 * <p>Pertanyaan apakah keanggotaan mahasiswa dilacak lewat FK atau lewat teks sudah diperiksa
 * langsung pada kode dan jawabannya adalah <b>teks</b>, persis pola {@link Kelas}:</p>
 * <ul>
 *   <li>{@code Mahasiswa.getProgram()} adalah {@link String} pada kolom {@code program}
 *   ({@code length = 50}) dengan default {@code "Reguler"} bila kosong;</li>
 *   <li>{@code ProgramMahasiswaDetailAction#initCriteria(boolean)} menyaring anggota dengan
 *   {@code Restrictions.eq("program", program.getNama())} &mdash; perbandingan STRING, bukan join
 *   ke tabel ini;</li>
 *   <li>penambahan/pelepasan anggota dilakukan dengan {@code mahasiswa.setProgram(nama)} /
 *   {@code mahasiswa.setProgram(null)}, lagi-lagi teks.</li>
 * </ul>
 * <p>Kolom FK {@code mahasiswa.program_baru} memang ADA ({@code Mahasiswa.getProgramBaru()},
 * {@code @ManyToOne} ke entity ini), tetapi ia <b>turunan, bukan sumber kebenaran</b>: getternya
 * menelusuri cache {@code Common.programs} dan menimpa isi field dengan master yang NAMANYA sama
 * (abaikan besar-kecil huruf) dengan teks {@code Mahasiswa.getProgram()}. Kolom itu efektif
 * berfungsi sebagai cache hasil pencocokan nama &mdash; jembatan migrasi dari teks ke relasi yang
 * belum pernah diselesaikan. Selama teks program tidak cocok dengan satu pun master, mahasiswa
 * tetap "punya program" secara teks tapi tidak terhubung ke baris manapun di sini.</p>
 *
 * <h3>Hubungan dengan {@link ProgramMahasiswa}</h3>
 * <p>Nama keduanya mirip, tetapi {@link ais.database.model.ProgramMahasiswa} <b>bukan</b> tabel
 * penghubung mahasiswa&harr;program. Ia master terpisah berisi aturan rentang semester (3 slot)
 * yang kebetulan juga menyimpan nama program sebagai teks (nilai kunci {@code Common.programs}).
 * Karena itu {@code Mahasiswa} punya DUA properti berbeda yang mudah tertukar:
 * {@code Mahasiswa.getProgram()}/{@code getProgramBaru()} (program perkuliahan = entity ini) dan
 * {@code Mahasiswa.getProgramMahasiswa()} (FK ke {@link ProgramMahasiswa}, kolom
 * {@code program_mahasiswa}).</p>
 *
 * <h3>Entity yang benar-benar mereferensikan lewat FK</h3>
 * <p>Berbeda dari {@code Mahasiswa}, entity berikut memakai relasi {@code @ManyToOne} sungguhan ke
 * tabel ini lewat {@code @JoinColumn(name = "program")}: {@link Kurikulum},
 * {@link PaketPunyaProgram}, {@link PembatasanNilaiIPKUntukPengambilanKRS}, {@link Tbmuser}, dan
 * {@link Tbmrole}. Dua yang terakhir menentukan <b>cakupan multi-tenant</b>: bila akun atau
 * rolenya terikat ke satu program, {@code Common.checkProgramString(Combobox)} akan memilih dan
 * MENGUNCI combobox program pada nilai itu.</p>
 *
 * <h3>Cache statis {@code Common.programs}</h3>
 * <p>Hampir semua pembacaan program di aplikasi tidak menyentuh DB langsung melainkan lewat peta
 * statis {@code Common.programs} ({@code Map<String, Program>} berkunci {@link #getNama()}), yang
 * diisi ulang oleh {@code CommonMaintenanceHelper.reInitProgram()}. Dua hal penting:</p>
 * <ul>
 *   <li>peta itu <b>hanya memuat program aktif</b> ({@code aktif = true} atau {@code null}) &mdash;
 *   menonaktifkan sebuah program membuatnya lenyap dari seluruh combobox dan dari pencocokan
 *   {@code getProgramBaru()};</li>
 *   <li>pengisian ulang tidak langsung: {@code ProgramAction} menjadwalkannya lewat
 *   {@code Common.createDefaultTimer(...)} setelah simpan atau setelah checkbox Aktif diubah, dan
 *   {@code reInitProgram()} juga memicu {@code ConstantValues.initJumlahTahapan()} yang membaca
 *   konfigurasi jumlah tahapan pembayaran per (program, jurusan).</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; label:</b> {@link #getNama()}/{@link #setNama(String)} (kunci primer),
 *   {@link #getNamaBaru()}/{@link #setNamaBaru(String)} (nama tampil),
 *   {@link #getNamaEn()}/{@link #setNamaEn(String)}, {@link #getKeterangan()}/
 *   {@link #setKeterangan(String)}, {@link #toString()}.</li>
 *   <li><b>Perilaku bisnis:</b> {@link #getNum()}/{@link #setNum(Integer)} (angka pembentuk NIM),
 *   {@link #getAktif()}/{@link #setAktif(Boolean)} (penyaring cache &amp; combobox).</li>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *   {@link #onUpdate()}.</li>
 *   <li><b>Konstruktor:</b> {@link #Program()} (wajib Hibernate) dan {@link #Program(String)}.</li>
 * </ul>
 * <p>Entity ini <b>tidak punya</b> method utilitas/query statis sama sekali; seluruh pencarian dan
 * penyimpanan dikerjakan {@code ProgramAction}, {@code ProgramDao}, dan {@code Common}.</p>
 *
 * <h3>Kuirk pola berulang &mdash; hasil verifikasi pada file ini sendiri</h3>
 * <ul>
 *   <li><b>Getter yang menulis balik ke field (dan berpotensi ke DB): ADA satu</b>, yaitu
 *   {@link #getAktif()}. Lihat javadoc method itu untuk dampaknya.</li>
 *   <li><b>Getter yang menutup sesi Hibernate: TIDAK ADA.</b> Tidak ada satu pun method di file
 *   ini yang menyentuh {@code Session}/{@code HibernateUtil}.</li>
 *   <li><b>Getter destruktif (menghapus/mengosongkan data): TIDAK ADA.</b></li>
 *   <li><b>Getter dengan fallback yang TIDAK menulis balik:</b> {@link #getNamaBaru()} dan
 *   {@link #getNama()}. Keduanya menghitung nilai kembalian di tempat tanpa menyentuh field,
 *   sehingga aman dari efek samping flush &mdash; berbeda dari {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Pemetaan Hibernate</h3>
 * <p>Karena {@code @Id} dipasang pada <i>getter</i>, Hibernate memakai <b>property access</b>:
 * setiap getter publik dianggap properti persisten kecuali ditandai {@code @Transient}. Hanya
 * {@code nama} dan {@code keterangan} yang punya {@code @Column} eksplisit; {@code num},
 * {@code aktif}, {@code namaEn}, {@code namaBaru}, {@code oleh}, {@code olehId}, dan
 * {@code tanggal_dirubah} jatuh ke penamaan default {@code ais.database.hibernate.MyNamingStrategy}
 * (turunan {@code DefaultNamingStrategy}: nama kolom = nama properti apa adanya, tanpa konversi ke
 * snake_case). Anotasi kelas {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menulis kolom yang benar-benar berubah, dan {@code @Audited} (Hibernate Envers) merekam setiap
 * versi baris ke tabel audit.</p>
 *
 * <h3>Catatan arsitektural: induk {@link GeneralValueObject} TIDAK dipetakan</h3>
 * <p>{@link ais.database.model.GeneralValueObject} bukan {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b>
 * memetakan properti apa pun yang hanya diwarisi darinya. Karena itu {@code oleh}, {@code olehId},
 * dan {@code tanggal_dirubah} <b>harus</b> dideklarasikan ulang di sini agar tersimpan; itu
 * KEHARUSAN TEKNIS, bukan duplikasi yang perlu "dirapikan". Perhatikan bahwa entity ini
 * <b>tidak</b> mendeklarasikan ulang {@code id} &mdash; memang tidak perlu, karena kunci primernya
 * adalah {@link #getNama()}; namun akibatnya {@code getId()} warisan selalu {@code null} untuk
 * entity ini, sehingga kode pemanggil generik yang mengandalkan {@code getId()} tidak akan
 * berfungsi terhadap {@code Program}.</p>
 *
 * <h3>Kuirk &amp; risiko yang terpantau di jalur pemakai (didokumentasikan, tidak diperbaiki)</h3>
 * <ul>
 *   <li><b>Panel detail nol-otorisasi.</b> {@code ProgramAction.ProgramRenderer} menanamkan
 *   {@code ProgramMahasiswaDetailAction} pada SETIAP baris grid. Panel itu tidak punya satu pun
 *   panggilan {@code checkPrevilages}/{@code doCheckSecurity} di seluruh 603 barisnya, padahal
 *   menyediakan "Ambil Data Mahasiswa" (assign massal), unggah Excel, dan "Hapus Semua" yang
 *   melepas program hingga 5.000 mahasiswa sekali klik. Ini instance lain dari pola berulang yang
 *   sama dengan panel detail keluarga {@code Kelompok*}/{@code Program*}.</li>
 *   <li><b>Layar master sendiri justru contoh positif.</b> {@code /pages/master/program.zul} tidak
 *   ada di daftar putih {@code CommonPrivilages.MUST_CHECKED}, tetapi {@code ProgramAction} punya
 *   {@code Common.doCheckSecurity()} di {@code doBeforeCompose} plus gerbang READ eksplisit, dan
 *   checkbox Aktif ikut {@code setDisabled(!edit)} &mdash; tidak terjadi inversi hak akses seperti
 *   pada beberapa layar master lain.</li>
 *   <li><b>Hapus baris tidak membersihkan anggota.</b> Karena keanggotaan berbasis teks, menghapus
 *   sebuah {@code Program} tidak mengosongkan {@code Mahasiswa.program}; mahasiswa akan menyimpan
 *   nama program yang tidak lagi punya master (dan hilang dari seluruh combobox). Tombol hapus
 *   hanya melindungi dua nama hardcoded, "Reguler" dan "Non Reguler".</li>
 *   <li><b>{@code onSave} tidak pernah memanggil {@code save()}.</b> Setelah
 *   {@code program.setNama(...)} dijalankan, cabang {@code if (program.getNama() != null)} selalu
 *   benar, sehingga program BARU pun disimpan lewat {@code programDao.update(...)}, bukan
 *   {@code save(...)}.</li>
 *   <li><b>Cakupan multi-tenant hanya di UI.</b> {@code CommonCurrentSessionHelper
 *   .checkProgramString(Combobox, boolean)} membatasi pilihan dengan {@code selectComboItem(...)}
 *   + {@code combobox.setDisabled(true)}; item program lain tetap ada di komponen dan
 *   pembatasannya tidak ditegakkan ulang di sisi kriteria query.</li>
 *   <li><b>NPE potensial di pembuat NIM.</b> {@code DefaultNimGenerator}, {@code UbbNimGenerator},
 *   dan varian {@code YY_*NimGenerator} memanggil {@code Common.programs.get(nama).getNum()} tanpa
 *   pemeriksaan null &mdash; bila program calon mahasiswa dinonaktifkan atau salah eja, pembuatan
 *   NIM gagal dengan {@link NullPointerException}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.ProgramMahasiswa
 * @see ais.database.model.Mahasiswa
 * @see ais.database.model.Jurusan
 * @see ais.database.model.Kelas
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "program")
public class Program extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dibangkitkan sekali oleh generator dan tidak boleh diubah
	 * agar objek yang tersimpan di sesi/cache lama tetap dapat dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * KUNCI PRIMER: kode/nama program (mis. {@code "Reguler"}). Di layar berlabel "Kode Program".
	 * Lihat {@link #getNama()}.
	 */
	private String nama;
	/** Nama tampil program; di layar berlabel "Nama Program". Lihat {@link #getNamaBaru()}. */
	private String namaBaru;
	/** Nama program dalam bahasa Inggris, untuk dokumen/laporan berbahasa Inggris. */
	private String namaEn;
	/** Catatan bebas tentang program ini; hanya ditampilkan, tidak dipakai logika apa pun. */
	private String keterangan;
	/** Angka urut/kode numerik program; di layar berlabel "ID Program". Lihat {@link #getNum()}. */
	private Integer num;

	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit). Lihat {@link #getOlehId()}. */
	private String olehId;

	/** Penanda program masih dipakai; menentukan masuk-tidaknya baris ke cache dan combobox. */
	private Boolean aktif;

	/**
	 * Id pengguna terakhir yang mengubah baris ini &mdash; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}.
	 *
	 * <p>Properti ini dideklarasikan ulang di kelas ini (bukan hanya diwarisi) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass}; tanpa deklarasi ulang, kolomnya
	 * tidak akan dipetakan Hibernate sama sekali.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong:</b> {@code null} atau string yang hanya berisi spasi diabaikan
	 * diam-diam dan field lama dipertahankan. Jejak audit yang sudah ada karena itu tidak pernah
	 * bisa dihapus lewat setter ini, termasuk saat Hibernate menghidrasi objek dari baris yang
	 * kolom auditnya masih {@code NULL}.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan sehingga
	 * jejak audit terakhir tidak bisa terhapus tanpa sengaja.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini &mdash; diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Dideklarasikan ulang di sini karena
	 * alasan yang sama dengan {@link #getOlehId()}.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE}
	 * baris ini, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Perhatikan bahwa hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak
	 * melewati callback ini, sehingga jejak audit pertama hanya terisi bila pemanggil mengisinya
	 * sendiri atau bila baris tersebut kemudian pernah diubah.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. Nilai awal diisi saat objek dibuat memakai jam aplikasi
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), bukan {@code new Date()}, agar konsisten dengan
	 * zona waktu/penyetelan waktu server yang dipakai seluruh modul.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan waktu perubahan terakhir. Umumnya dipanggil
	 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan oleh kode layar.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir baris ini, disimpan sebagai {@code TIMESTAMP}. Dideklarasikan ulang
	 * di kelas ini karena {@link GeneralValueObject} tidak dipetakan Hibernate.
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks objek = isi field {@code nama} APA ADANYA.
	 *
	 * <p>Dipakai ZK saat objek {@code Program} dijadikan nilai komponen tanpa label eksplisit, dan
	 * oleh berbagai laporan. Perhatikan dua kehalusan: (a) yang dikembalikan adalah <b>kode</b>
	 * program, bukan nama tampil {@link #getNamaBaru()}; (b) berbeda dari {@link #getNama()},
	 * method ini membaca field langsung sehingga TIDAK di-{@code trim} dan bisa mengembalikan
	 * {@code null} untuk objek yang belum diisi.</p>
	 *
	 * @return kode program apa adanya, mungkin {@code null}
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Konstruktor kosong yang WAJIB ada agar Hibernate dapat menginstansiasi entity saat
	 * menghidrasi hasil query. Juga dipakai {@code ProgramAction#onAdd(Event)} untuk membuat baris
	 * baru dari layar master.
	 */
	public Program() {
	}

	/**
	 * Konstruktor praktis yang langsung menetapkan kunci primer.
	 *
	 * @param nama kode/nama program yang sekaligus menjadi kunci primer baris ini
	 */
	public Program(String nama) {
		this.nama = nama;
	}

	/**
	 * KUNCI PRIMER entity ini: kode/nama program, mis. {@code "Reguler"} atau {@code "Karyawan"}.
	 * Di layar master berlabel <b>"Kode Program"</b>, bukan "Nama Program".
	 *
	 * <p>Nilai inilah yang disalin sebagai teks ke {@code Mahasiswa.program},
	 * {@code Kkn.program}, {@code ProgramMahasiswa.program}, dan menjadi kunci peta
	 * {@code Common.programs}. Karena itu nilai ini praktis tidak boleh berubah setelah dipakai
	 * &mdash; lihat pembahasan natural key pada javadoc kelas.</p>
	 *
	 * <p><b>Hasilnya di-{@code trim}, tetapi field TIDAK ditulis ulang</b>: method ini
	 * mengembalikan salinan terpangkas tanpa mengubah state objek, sehingga aman dari efek samping
	 * flush. Konsekuensinya nilai yang dikembalikan getter bisa berbeda dari isi field bila data
	 * lama mengandung spasi pinggir; karena Hibernate memakai property access, yang dianggap
	 * identifier adalah versi terpangkas ini.</p>
	 *
	 * @return kode program tanpa spasi pinggir, atau {@code null} bila belum diisi
	 */
	@Id
	@Column(name = "nama", nullable = false, length = 50, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan kode/nama program (kunci primer). Disimpan APA ADANYA tanpa {@code trim}; yang
	 * memangkas spasi adalah pemanggil ({@code ProgramAction#onSave(Event)} memakai
	 * {@code .trim()}) dan getter-nya.
	 *
	 * <p><b>Hati-hati:</b> mengubah nilai ini pada objek yang sudah tersimpan berarti mengubah
	 * kunci primer dan akan memutus seluruh referensi teks maupun FK yang menunjuk ke nilai lama.
	 * UI mencegahnya dengan menonaktifkan textbox kode pada baris yang sudah ada.</p>
	 *
	 * @param nama kode program; menjadi kunci primer baris
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas tentang program ini, diisi lewat textarea "Keterangan" di layar master. Murni
	 * informatif &mdash; tidak ada logika bisnis yang membacanya.
	 *
	 * @return keterangan program, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan program.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan angka urut/kode numerik program.
	 *
	 * @param num angka program; wajib diisi lewat layar master (divalidasi {@code onSave})
	 */
	public void setNum(Integer num) {
		this.num = num;
	}

	/**
	 * Angka urut/kode numerik program &mdash; di layar master berlabel <b>"ID Program"</b> meski
	 * BUKAN kunci primer (kunci primernya {@link #getNama()}).
	 *
	 * <p><b>Dipakai untuk membentuk NIM.</b> Seluruh implementasi pembuat NIM di
	 * {@code ais.action.master.pmb.nim} (mis. {@code DefaultNimGenerator},
	 * {@code UbbNimGenerator}, {@code YY_PRODI_PROGRAM_URUT_NimGenerator}) mengambil angka ini
	 * lewat {@code Common.programs.get(namaProgram).getNum()} dan menyisipkannya sebagai salah
	 * satu digit NIM calon mahasiswa. Mengubah nilai ini setelah ada mahasiswa terdaftar akan
	 * membuat NIM lama dan NIM baru tidak lagi konsisten; pencarian program dari cache juga tanpa
	 * pemeriksaan null, sehingga program yang dinonaktifkan membuat pembuatan NIM melempar
	 * {@link NullPointerException}.</p>
	 *
	 * <p>Tanpa {@code @Column}, sehingga nama kolomnya mengikuti {@code MyNamingStrategy}:
	 * {@code num} apa adanya.</p>
	 *
	 * @return angka program, atau {@code null} bila belum diisi
	 */
	public Integer getNum() {
		return num;
	}

	/**
	 * Penanda program masih dipakai. <b>Getter dengan penulisan balik ke field</b>: bila nilainya
	 * masih {@code null}, method ini <i>menetapkan</i> {@code aktif = true} sebelum
	 * mengembalikannya, bukan sekadar mengembalikan {@code true}.
	 *
	 * <p><b>Efek samping yang perlu disadari.</b> Karena Hibernate memakai property access dan
	 * memanggil getter ini saat dirty-checking, sekadar MEMBACA baris yang kolom {@code aktif}-nya
	 * {@code NULL} di dalam sesi yang masih terbuka sudah membuat objek dianggap kotor, sehingga
	 * flush berikutnya menerbitkan {@code UPDATE} yang mengubah {@code NULL} menjadi {@code true}
	 * di database &mdash; tanpa ada operator yang menekan tombol simpan. Ini pola "getter yang
	 * menulis balik" yang sudah berulang kali ditemui di entity AIS lain. Dalam kasus ini
	 * dampaknya jinak (semantik {@code NULL} dan {@code true} memang sama di seluruh query
	 * penyaring, yang selalu berbentuk {@code isNull(aktif) OR eq(aktif, true)}), tetapi ia tetap
	 * menimbulkan penulisan tak terduga plus satu revisi Envers baru per baris.</p>
	 *
	 * <p><b>Kegunaan.</b> Program tidak aktif tetap ada di tabel tetapi lenyap dari cache
	 * {@code Common.programs} ({@code reInitProgram()} hanya memuat yang aktif/{@code null}) dan
	 * dari daftar pencarian layar master saat checkbox "Tampilkan hanya yang aktif" dicentang
	 * &mdash; efektif menyembunyikan program tanpa menghapus baris dan tanpa memutus data
	 * historis yang menyimpan namanya sebagai teks.</p>
	 *
	 * @return status aktif; tidak pernah {@code null} (dinormalkan menjadi {@code true})
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Menetapkan status aktif program. Dipanggil dari checkbox "Aktif" pada grid layar master
	 * ({@code ProgramAction.ProgramRenderer}), yang langsung menyimpan lewat
	 * {@code Common.refreshSaveOrUpdate(program)} lalu menjadwalkan {@code Common.reInitProgram()}
	 * agar cache program ikut menyesuaikan.
	 *
	 * @param aktif {@code true} bila program masih dipakai; {@code false} untuk menyembunyikannya
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Nama program dalam bahasa Inggris, untuk transkrip/ijazah/laporan berbahasa Inggris. Bersifat
	 * opsional dan tidak punya fallback &mdash; berbeda dari {@link #getNamaBaru()}, method ini
	 * mengembalikan {@code null} apa adanya bila kolomnya kosong, sehingga pemanggil wajib
	 * mengantisipasi {@code null}.
	 *
	 * @return nama program dalam bahasa Inggris, atau {@code null} bila belum diisi
	 */
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan nama program dalam bahasa Inggris.
	 *
	 * @param namaEn nama berbahasa Inggris; boleh {@code null}
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

	/**
	 * NAMA TAMPIL program &mdash; di layar master berlabel <b>"Nama Program"</b>, dan inilah yang
	 * dipakai sebagai label item combobox oleh {@code Common.initPrograms(Combobox)} (nilainya
	 * tetap {@link #getNama()}). Jadi pengguna memilih berdasarkan nilai ini, sementara yang
	 * tersimpan ke {@code Mahasiswa.program} dan sejenisnya adalah kodenya.
	 *
	 * <p><b>Fallback tanpa penulisan balik:</b> bila field {@code namaBaru} masih {@code null},
	 * method ini mengembalikan {@link #getNama()} sebagai gantinya <i>tanpa</i> menetapkan field.
	 * Ini penting &mdash; berbeda dari {@link #getAktif()}, method ini TIDAK membuat objek menjadi
	 * kotor, sehingga nilai fallback tidak pernah "menetap" ke database. Pemanggil karena itu
	 * dijamin tidak pernah menerima {@code null} selama kode programnya terisi, tetapi juga tidak
	 * bisa membedakan "nama tampil sengaja disamakan dengan kode" dari "nama tampil belum
	 * diisi".</p>
	 *
	 * <p>Nama properti "namaBaru" adalah sisa migrasi historis (kolom nama tampil yang ditambahkan
	 * belakangan agar kolom {@code nama}/kunci primer tidak perlu diubah), bukan penanda "nama
	 * yang akan berlaku nanti".</p>
	 *
	 * @return nama tampil program; jatuh ke kode program bila belum diisi
	 */
	public String getNamaBaru() {
		return namaBaru == null ? getNama() : namaBaru;
	}

	/**
	 * Menetapkan nama tampil program. Divalidasi wajib isi oleh
	 * {@code ProgramAction#onSave(Event)}, sehingga baris yang dibuat lewat layar master selalu
	 * punya nilai eksplisit dan fallback pada {@link #getNamaBaru()} hanya relevan bagi data lama
	 * atau baris yang dibuat di luar layar itu.
	 *
	 * @param namaBaru nama tampil program; boleh {@code null} (akan memicu fallback pada getter)
	 */
	public void setNamaBaru(String namaBaru) {
		this.namaBaru = namaBaru;
	}

}
