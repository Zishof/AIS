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
 * Entity <b>penghubung</b> antara definisi field kustom generik dan seksi/kategori tampilnya pada
 * modul <b>Pengajuan peserta didik</b>, dipetakan ke tabel
 * {@code public.parameter_tambahan_pengajuan}.
 *
 * <h3>Posisi dalam rantai field kustom Pengajuan</h3>
 * <p>AIS mengizinkan tiap institusi menambah pertanyaan/isian sendiri pada formulir pengajuan tanpa
 * mengubah skema database. Rantainya <b>empat lapis</b>:</p>
 * <ol>
 *   <li>{@link ParameterTambahan} &mdash; <b>definisi</b> field kustom generik: label
 *   ({@code labelInputan}), tipe input ({@code tipeDataInputan}), daftar pilihan
 *   ({@code nilaiDataInputan}), wajib/tidak ({@code wajibDiisi}), perlu lampiran/tidak
 *   ({@code harusMenyertakanLampiran}, {@code lampiranWajibDiisi}), keterangan, dan nomor urut.
 *   Tabel ini dipakai bersama oleh banyak modul (alumni, mahasiswa, calon mahasiswa, catatan
 *   pegawai, pengaduan, &hellip;) &mdash; ia tidak tahu-menahu soal pengajuan.</li>
 *   <li><b>Kelas ini</b> &mdash; baris penghubung yang <b>mengadopsi</b> satu
 *   {@link ParameterTambahan} ke dalam satu {@link KelompokParameterTambahanPengajuan}.</li>
 *   <li>{@link KelompokParameterTambahanPengajuan} &mdash; <b>kategori/judul seksi</b>; satu baris
 *   di sana menjadi satu heading pada formulir pengajuan.</li>
 *   <li>{@link JenisPengajuan} &mdash; jenis pengajuan (mis. cuti, pindah prodi, surat keterangan)
 *   yang <b>mencentang</b> kategori mana saja yang berlaku, lewat relasi {@code @ManyToMany}
 *   {@link JenisPengajuan#getKelompokParameterTambahanPengajuans()} (tabel
 *   {@code jenis_pengajuan_has_parameter}). Kategori yang tidak dicentang di sini TIDAK PERNAH
 *   muncul di formulir, seaktif apa pun baris di lapis 2 dan 3.</li>
 * </ol>
 * <p>Karena adopsi terjadi di lapis ini, satu {@link ParameterTambahan} yang sama bisa dipakai
 * berkali-kali (di kategori berbeda) tanpa digandakan definisinya.</p>
 *
 * <h3>Anggota keluarga paling ramping</h3>
 * <p>Dibandingkan saudara-saudaranya di keluarga {@code ParameterTambahan*}, entity ini
 * <b>sengaja minimal</b> &mdash; hanya identitas, jejak audit, dua relasi rantai, dan cache nomor
 * urut. Yang TIDAK dimilikinya (padahal ada di
 * {@link ParameterTambahanAlumni}/{@link ParameterTambahanMahasiswa}):</p>
 * <ul>
 *   <li><b>Tanpa penyaring cakupan</b> &mdash; tidak ada {@code fakultas}, {@code jurusan},
 *   {@code program}, {@code jenjang}, maupun {@code tahunAngkatans}. Penyempitan cakupan di modul
 *   ini dikerjakan sepenuhnya di lapis {@link JenisPengajuan} (pengaju memilih jenis pengajuan
 *   dulu, barulah kategori yang tercentang di jenis itu yang dirakit).</li>
 *   <li><b>Tanpa penimpa {@code wajibDiisi} per-adopsi.</b> Baik perakit formulir
 *   ({@code ParameterTambahanPengajuanListener.onEvent}) maupun validatornya
 *   ({@code ParameterTambahanPengajuanListener.validate()}) membaca
 *   {@link ParameterTambahan#getWajibDiisi()} <b>langsung dari definisi</b>. Konsekuensinya:
 *   mengubah status wajib sebuah parameter berdampak ke SELURUH modul yang mengadopsinya, tidak
 *   bisa dibedakan per kategori pengajuan.</li>
 * </ul>
 *
 * <h3>Dua entity pemilik data (TERVERIFIKASI dari kode pemanggil)</h3>
 * <p>Ini kekhasan terpenting entity ini dan pembedanya dari anggota keluarga lain: domain
 * "Pengajuan" mencakup <b>DUA</b> entity pemilik isian &mdash; {@link PengajuanMahasiswa}
 * (perguruan tinggi) dan {@link ais.database.model.sekolah.PengajuanSiswa} (sekolah). Hasil
 * verifikasi:</p>
 * <ul>
 *   <li><b>Konfigurasi tidak dibedakan sama sekali.</b> Kelas ini TIDAK punya kolom pembeda
 *   jenjang/pemilik, TIDAK punya dua pasang relasi, dan TIDAK punya diskriminator apa pun. Satu
 *   tabel {@code parameter_tambahan_pengajuan} yang sama melayani kedua entity pemilik, lewat
 *   {@link KelompokParameterTambahanPengajuan} dan {@link JenisPengajuan} yang juga dipakai
 *   bersama.</li>
 *   <li><b>Pembedaan terjadi di lapis pemilik data, bukan di sini.</b> Masing-masing entity
 *   pemilik punya <b>pasangan kolom {@code text} sendiri</b> dengan nama, format, dan method
 *   yang identik: {@code parameterTambahan} (versi berlabel) + {@code parameterTambahanInds}
 *   (versi ber-ID), beserta {@code populateParameterTambahan(List)} dan
 *   {@code ambilDataParameterTambahan()} masing-masing. Jadi ada <b>dua pasang kolom, di dua
 *   tabel berbeda</b> &mdash; bukan dua pasang kolom di satu tabel.</li>
 *   <li><b>Pemilih cabang adalah listener, lewat konstruktor.</b>
 *   {@code ais.action.master.helper.ParameterTambahanPengajuanListener} punya dua konstruktor
 *   ({@link PengajuanMahasiswa} dan {@link ais.database.model.sekolah.PengajuanSiswa}) yang saling
 *   eksklusif; {@code onEvent()} memilih cabang berdasarkan field mana yang tidak {@code null}.
 *   <b>Kedua cabang menjalankan query Criteria terhadap kelas ini yang sama persis</b> (disalin
 *   kata-per-kata) &mdash; tidak ada satu pun {@code Restriction} yang membedakan mahasiswa dari
 *   siswa.</li>
 * </ul>
 *
 * <h3>Cara baris ini dibaca saat formulir dirakit</h3>
 * <p>Query yang menjadikan baris ini "hidup" berbentuk sama di seluruh pembaca (listener, layar
 * tampil {@code PengajuanMahasiswaAction}/{@code sekolah.PengajuanSiswaAction}, dan laporan
 * {@code LaporanPengajuan}):</p>
 * <pre>
 * session.createCriteria(ParameterTambahanPengajuan.class)
 *     .add(Restrictions.eq("kelompokParameterTambahanPengajuan", kelompok))
 *     .createAlias("parameterTambahan", "parameterTambahan")
 *     .createAlias("kelompokParameterTambahanPengajuan", "kelompokParameterTambahanPengajuan")
 *     .add(Restrictions.eq("parameterTambahan.aktif", true))
 *     .add(Restrictions.eq("kelompokParameterTambahanPengajuan.aktif", true))
 *     .setProjection(Projections.groupProperty("parameterTambahan.id"))
 * </pre>
 * <p>Perhatikan bahwa proyeksinya adalah {@code groupProperty} atas <b>id definisi</b>: yang
 * dikembalikan bukan baris entity ini, melainkan daftar {@link ParameterTambahan} yang sudah
 * <b>ter-dedup</b>. Jadi bila satu parameter tak sengaja diadopsi dua kali ke kategori yang sama,
 * formulir tetap menampilkannya satu kali &mdash; baris duplikat di tabel ini tidak berbahaya bagi
 * tampilan (tetapi tetap tampak ganda di layar admin). Urutan tampil ditentukan
 * {@code Collections.sort(...)} atas daftar {@link ParameterTambahan} hasil query, yaitu memakai
 * {@code nomorUrut} milik DEFINISI &mdash; bukan {@link #getNomorUrut()} kelas ini.</p>
 *
 * <h3>Di mana isian pengaju sesungguhnya disimpan (terverifikasi)</h3>
 * <p>Entity ini murni <b>konfigurasi</b>; tidak ada satu pun kolom nilai di sini. Isian disimpan
 * sebagai <b>string terserialisasi</b> pada dua kolom {@code text} milik entity pemilik
 * ({@link PengajuanMahasiswa} atau {@link ais.database.model.sekolah.PengajuanSiswa}), ditulis
 * oleh {@code populateParameterTambahan(List&lt;Row&gt;)} masing-masing:</p>
 * <ol>
 *   <li>{@code getParameterTambahan()} &mdash; <b>versi berlabel</b>, 8 ruas per baris, baris
 *   dipisah {@code "\n"} dan ruas dipisah {@code "<=>"}:
 *   <pre>
 *   namaKelompok "-&gt;" labelInputan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; nomorUrut
 *       &lt;=&gt; idParameterTambahan &lt;=&gt; idKelompok &lt;=&gt; indexKe &lt;=&gt; keterangan
 *   </pre>
 *   dipakai layar tampil dan laporan;</li>
 *   <li>{@code getParameterTambahanInds()} &mdash; <b>versi ber-ID</b>, 4 ruas per baris:
 *   <pre>
 *   idKelompok "-&gt;" idParameterTambahan &lt;=&gt; nilai &lt;=&gt; urlLampiran &lt;=&gt; keterangan
 *   </pre>
 *   inilah yang dipakai mesin untuk memulihkan nilai ke dalam komponen ZK saat formulir dibuka
 *   kembali.</li>
 * </ol>
 * <p><b>Kunci penyimpanan.</b> Isian dialamatkan oleh pasangan
 * <code>idKelompok + "-&gt;" + idParameterTambahan</code>. String yang sama juga dipakai sebagai
 * penanda {@code jenis} pada
 * {@link ais.database.model.file.LampiranLain#ambil(Long, String)} untuk berkas lampiran, dengan
 * {@code ref} berupa id baris pengajuan. <b>ID baris entity ini sendiri tidak pernah ikut
 * disimpan.</b> Akibatnya:</p>
 * <ul>
 *   <li>menghapus lalu membuat ulang baris penghubung ini <b>tidak</b> memutus isian lama, selama
 *   pasangan kategori+parameter yang sama dipakai lagi;</li>
 *   <li>sebaliknya, <b>memindahkan</b> sebuah parameter ke kategori lain mengubah kunci, sehingga
 *   seluruh isian historis menjadi yatim di dalam kolom {@code text} &mdash; tetap tersimpan,
 *   tetapi tidak pernah terbaca lagi oleh formulir maupun validator;</li>
 *   <li>karena penyimpanannya string, tidak ada foreign key: menghapus {@link ParameterTambahan}
 *   atau kategori tidak membersihkan isian lama.</li>
 * </ul>
 * <p><b>Konsekuensi khas dua-pemilik (dicatat apa adanya, tidak diperbaiki di sini).</b> Ruang
 * nama kunci {@code idKelompok->idParameter} dipakai bersama oleh kedua entity pemilik, dan
 * {@link ais.database.model.file.LampiranLain#ambil(Long, String)} mencocokkan HANYA pasangan
 * ({@code ref}, {@code jenis}) tanpa pembeda kelas pemilik. Karena {@code ref} diisi
 * {@code pengajuanMahasiswa.getId()} pada satu cabang dan {@code pengajuanSiswa.getId()} pada
 * cabang lain &mdash; dua urutan id dari <b>tabel berbeda</b> yang keduanya mulai dari 1 &mdash;
 * lampiran milik baris {@code PengajuanSiswa} dan {@code PengajuanMahasiswa} ber-id sama pada
 * kategori+parameter yang sama <b>menempati kunci yang identik</b>. Isian teksnya sendiri aman
 * (tersimpan di kolom tabel masing-masing); yang berisiko tertukar hanyalah lampirannya. Risiko
 * ini hanya muncul pada instalasi yang mengaktifkan modul perguruan tinggi DAN sekolah
 * sekaligus.</p>
 *
 * <h3>Siapa yang memakai baris-baris ini</h3>
 * <ul>
 *   <li><b>Layar admin</b> {@code ais.action.master.ParameterTambahanPengajuanAction} (CRUD).
 *   {@code doAfterCompose()} layar tersebut memanggil
 *   {@link KelompokParameterTambahanPengajuan#checkCreateDefault()} &mdash; jadi layar milik
 *   entity INILAH pemicu utama auto-seed kategori bawaan pada rantai ini.</li>
 *   <li><b>Perakit formulir</b> {@code ais.action.master.helper.ParameterTambahanPengajuanListener}
 *   (dua konstruktor, {@code onEvent()}, {@code validate()}, dua overload {@code onSave(...)}),
 *   dipakai {@code ais.action.master.PengajuanMahasiswaAction} dan
 *   {@code ais.action.master.sekolah.PengajuanSiswaAction}.</li>
 *   <li><b>Layar tampil rinci</b> pada kedua Action pengajuan di atas (merender nilai tersimpan +
 *   lampiran per parameter).</li>
 *   <li><b>Laporan</b> {@code ais.action.report.format1.akademik.LaporanPengajuan} (memetakan tiap
 *   parameter menjadi kolom laporan berkunci {@code idKelompok + "_" + idParameter}).</li>
 * </ul>
 *
 * <h3>Kuirk layar dan pemanggil yang mempengaruhi data entity ini</h3>
 * <ul>
 *   <li><b>Alias Criteria rusak di DUA jalur tampil.</b> {@code PengajuanMahasiswaAction} dan
 *   {@code sekolah.PengajuanSiswaAction} mendaftarkan alias
 *   {@code "kelompokParameterTambahanPengajuan"} tetapi menyaring dengan
 *   {@code Restrictions.eq("kelompokParameterTambahanPengajuanMahasiswa.aktif", true)} dan
 *   {@code "kelompokParameterTambahanPengajuanSiswa.aktif"} &mdash; alias yang tidak pernah
 *   didefinisikan. Query itu berpotensi melempar {@code QueryException} yang tidak tertangani di
 *   jalur tampil rinci. Jalur perakit formulir ({@code ParameterTambahanPengajuanListener}) dan
 *   jalur laporan ({@code LaporanPengajuan}) memakai alias yang BENAR, jadi mengisi formulir tetap
 *   berfungsi. Bug kembar terverifikasi di modul Mahasiswa DAN Siswa. Dicatat apa adanya &mdash;
 *   tidak diperbaiki di sini.</li>
 *   <li><b>Penambahan massal tanpa validasi duplikat.</b> {@code onAdd()} membuka dialog
 *   {@code AmbilDataParameterTambahanBanyak} lalu mem-{@code session.save(...)} satu baris baru per
 *   parameter terpilih, tanpa memeriksa apakah pasangan kategori+parameter itu sudah ada. Duplikat
 *   bisa terbentuk; efeknya tertelan {@code groupProperty} pada pembaca (lihat di atas), tetapi
 *   tampak ganda di grid admin.</li>
 *   <li><b>Kategori baris baru diambil dari combobox PENCARIAN</b>, bukan dari form isian &mdash;
 *   {@code onAdd()} menolak berjalan bila filter kategori belum dipilih.</li>
 *   <li><b>Impor/ekspor reflektif percuma untuk {@code nomorUrut}.</b> {@code doAfterCompose()}
 *   mendaftarkan {@code contents = {"id", "parameterTambahan",
 *   "kelompokParameterTambahanPengajuan", "nomorUrut"}} ke {@code Common.uploadData(...)}. Nilai
 *   {@code nomorUrut} hasil impor akan langsung ditimpa lagi oleh {@link #getNomorUrut()} pada
 *   pembacaan berikutnya.</li>
 *   <li><b>Broken access control pada layar admin.</b>
 *   {@code ParameterTambahanPengajuanAction} meng-hardcode {@code private boolean edit = true;} dan
 *   {@code private boolean delete = true;} serta TIDAK memanggil {@code checkPrevilages} sama
 *   sekali di seluruh berkas. Siapa pun yang dapat membuka layar ini dapat mengubah dan menghapus
 *   pemetaan parameter pengajuan tanpa hak apa pun. Ini cacat template yang sama di seluruh
 *   keluarga {@code ParameterTambahan*Action}; sudah tercatat sebagai temuan audit terpisah.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 *   <li><b>Relasi rantai:</b> {@link #getParameterTambahan()} (definisi field),
 *   {@link #getKelompokParameterTambahanPengajuan()} (kategori/heading).</li>
 *   <li><b>Pengurutan:</b> {@link #getNomorUrut()} (satu-satunya kunci urut efektif bagi objek
 *   entity ini; lihat {@link GeneralValueObject#compareTo(GeneralValueObject)}).</li>
 *   <li><b>Konstruktor:</b> {@link #ParameterTambahanPengajuan()}.</li>
 * </ul>
 *
 * <h3>Catatan warisan &amp; pemetaan (non-obvious)</h3>
 * <ul>
 *   <li>{@link GeneralValueObject} BUKAN {@code @Entity} maupun {@code @MappedSuperclass} &mdash;
 *   ia POJO abstrak biasa, sehingga Hibernate <b>tidak memetakan satu pun property induknya</b>.
 *   Karena itu deklarasi ULANG {@code id}, {@code oleh}, {@code olehId}, {@code tanggal_dirubah},
 *   dan {@code nomorUrut} di kelas ini <b>bukan bug</b>, melainkan keharusan teknis agar
 *   kolom-kolom itu benar-benar tersimpan.</li>
 *   <li>Anotasi {@code @Id} berada pada <b>getter</b>, sehingga Hibernate memakai <i>property
 *   access</i> untuk seluruh property. Digabung dengan {@code dynamicUpdate = true},
 *   {@link #getNomorUrut()} yang menulis balik dapat memicu {@code UPDATE} beserta revisi Envers
 *   baru pada baris yang <b>sekadar dibaca</b>.</li>
 *   <li><b>Asimetri {@code nullable} vs saudara keluarga:</b> {@code @JoinColumn} untuk
 *   {@link #getKelompokParameterTambahanPengajuan()} di sini bernilai {@code nullable = true},
 *   sedangkan pada {@link ParameterTambahanAlumni} dan {@link ParameterTambahanMahasiswa} relasi
 *   sepadan bersifat wajib. Database karena itu mengizinkan baris <b>yatim tanpa kategori</b>.
 *   Baris seperti itu tidak akan pernah muncul di formulir mana pun (seluruh pembaca menyaring
 *   dengan {@code Restrictions.eq("kelompokParameterTambahanPengajuan", kelompok)}), dan renderer
 *   grid admin memanggil {@code getKelompokParameterTambahanPengajuan().getNama()} tanpa penjagaan
 *   {@code null} &mdash; sehingga satu baris yatim (mis. hasil SQL mentah/migrasi) cukup untuk
 *   membuat layar daftar melempar {@code NullPointerException}. Jalur UI normal tidak pernah
 *   menghasilkan baris seperti itu karena {@code onAdd()} dan {@code onSave()} sama-sama
 *   mensyaratkan kategori terpilih.</li>
 *   <li>Property {@code nomorUrut} tanpa {@code @Column} eksplisit memakai nama kolom apa adanya
 *   sesuai strategi penamaan bawaan Hibernate.</li>
 *   <li>Kedua relasi {@code @ManyToOne} memakai {@code cascade = {PERSIST, MERGE}}, jadi menyimpan
 *   baris ini bisa ikut mem-{@code persist}/{@code merge} master yang direferensikan.</li>
 *   <li>Kelas ini TIDAK memiliki field {@code keterangan}, {@code nama}, maupun {@code nim}
 *   sendiri; {@link #toString()}, {@code getNama()}, {@code getNim()}, dan {@code getKeterangan()}
 *   sepenuhnya diwarisi dari {@link GeneralValueObject} dan selalu bernilai bawaan karena tidak
 *   dipetakan.</li>
 *   <li>{@code @Audited} (Envers) aktif: setiap perubahan baris konfigurasi ini terekam di tabel
 *   audit, termasuk {@code UPDATE} tak sengaja dari {@link #getNomorUrut()}. Layar admin juga
 *   merender tombol riwayat revisi lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 *   <li>{@link JenisPengajuan} menyimpan hasil {@code getKelompokParameterTambahanPengajuans()}
 *   pada {@code Map} <b>statis</b> ({@code JenisPengajuan.mapParameters}) yang tidak pernah
 *   di-invalidasi. Perubahan pencentangan kategori pada sebuah jenis pengajuan karena itu bisa
 *   tampak belum berlaku sampai JVM di-restart &mdash; gejala yang mudah keliru dikira masalah
 *   pada baris entity ini.</li>
 * </ul>
 *
 * @see ParameterTambahan
 * @see KelompokParameterTambahanPengajuan
 * @see JenisPengajuan
 * @see PengajuanMahasiswa#populateParameterTambahan(java.util.List)
 * @see ais.database.model.sekolah.PengajuanSiswa
 * @see ParameterTambahanAlumni
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_pengajuan")
public class ParameterTambahanPengajuan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialisasi karena disimpan sebagai atribut komponen
	 * ZK dan sebagai nilai item {@code Combobox}/model grid pada layar admin.
	 *
	 * <p>Nilainya kebetulan sama persis dengan milik {@link ParameterTambahanAlumni} &mdash; jejak
	 * bahwa berkas ini lahir dari salin-tempel berkas saudaranya. Tidak berdampak fungsional karena
	 * {@code serialVersionUID} hanya dibandingkan antar-versi kelas yang SAMA.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key {@code IDENTITY}; dideklarasikan ulang karena induk tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna pengubah terakhir; diisi otomatis oleh {@link #onUpdate()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> nilai {@code null}/kosong/hanya spasi diabaikan diam-diam (method langsung
	 * {@code return}), sehingga nilai lama bertahan dan jejak audit tidak pernah bisa dikosongkan.</p>
	 *
	 * @param olehId ID pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kuirk:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong/spasi
	 * diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate TEPAT SEBELUM setiap {@code UPDATE} baris
	 * ini, lalu meneruskan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}
	 * yang mengisi {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang login.
	 *
	 * <p><b>Efek samping:</b> memodifikasi state objek di tengah siklus flush. Jangan dipanggil
	 * manual dari kode aplikasi &mdash; Hibernate yang memicunya.</p>
	 *
	 * <p>Hanya {@code @PreUpdate} yang dipasang; baris BARU ({@code INSERT}) tidak melewati callback
	 * ini. Baris hasil {@code onAdd()} layar admin &mdash; yang di modul ini dibuat MASSAL sekaligus
	 * lewat dialog "ambil data banyak" &mdash; karena itu masuk <b>tanpa jejak</b>
	 * {@code oleh}/{@code olehId} sampai pertama kali diubah.</p>
	 *
	 * <p>Perlu diingat bahwa {@link #getNomorUrut()} dapat mengotori field saat baris sekadar
	 * dibaca, sehingga callback ini bisa ikut terpicu pada {@code UPDATE} yang <b>tidak diminta
	 * pengguna mana pun</b> &mdash; jejak audit lalu mencatat pengguna yang kebetulan sedang membuka
	 * layar atau mengisi formulir pengajuan.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris fisik yang sama dalam kode
	 * aslinya; nilainya diinisialisasi memakai jam aplikasi ({@code ais.ui.util.WaktuUtil.getDate()}),
	 * bukan {@code new Date()}, agar konsisten dengan zona waktu/penyetelan waktu server yang dipakai
	 * seluruh modul.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * @param tanggal_dirubah waktu perubahan; boleh {@code null} (tidak ada penambalan di sini)
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini.
	 *
	 * <p>Tidak pernah {@code null} untuk objek baru karena field-nya diinisialisasi saat deklarasi
	 * (lihat {@link #onUpdate()}).</p>
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kategori/heading tempat field ini ditampilkan pada formulir pengajuan; kolom
	 * {@code kelompok_parameter_tambahan_pengajuan}. Dipetakan {@code nullable = true} &mdash; lihat
	 * catatan asimetri pada dokumentasi kelas.
	 */
	private KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan;
	/** Definisi field kustom yang diadopsi baris ini; wajib, kolom {@code parameter_tambahan}. */
	private ParameterTambahan parameterTambahan;

	/**
	 * Cache nomor urut tampil; nilainya <b>selalu ditimpa</b> dari {@link ParameterTambahan} setiap
	 * kali {@link #getNomorUrut()} dipanggil.
	 */
	private Integer nomorUrut;

	/**
	 * Mengembalikan nomor urut tampil field ini, <b>diturunkan dari definisinya</b>
	 * ({@link ParameterTambahan#getNomorUrut()}).
	 *
	 * <p>Meng-override {@code getNomorUrut()} milik {@link GeneralValueObject} dan merupakan
	 * <b>satu-satunya kunci urut yang efektif</b> bagi objek entity ini: {@code compareTo} induk
	 * mencoba berturut-turut {@code nomorUrut} &rarr; {@code nim} &rarr; {@code nama} &rarr;
	 * {@code keterangan}, dan tiga kunci terakhir selalu bernilai bawaan di sini karena tidak
	 * dipetakan.</p>
	 *
	 * <p><b>Catatan pemakaian nyata:</b> pengurutan field di formulir pengajuan TIDAK memakai method
	 * ini. Pembaca formulir memproyeksikan hasil query menjadi daftar {@link ParameterTambahan}
	 * ({@code Projections.groupProperty("parameterTambahan.id")}) lalu memanggil
	 * {@code Collections.sort(...)} atas daftar itu &mdash; jadi yang menentukan urutan adalah
	 * {@code nomorUrut} milik DEFINISI secara langsung. Kolom {@code nomorUrut} di tabel ini
	 * praktis hanya salinan denormalisasi.</p>
	 *
	 * <p><b>Efek samping (penting).</b> Method ini bukan pembaca murni:</p>
	 * <ul>
	 *   <li>menugaskan ulang field {@code parameterTambahan} dengan hasil
	 *   {@link #getParameterTambahan()} &mdash; artinya proxy lazy ikut diresolusi
	 *   ({@link GeneralValueObject#check(Object)}) dan dapat memicu query;</li>
	 *   <li>menimpa field {@code nomorUrut} dengan nilai dari definisi. Karena Hibernate memakai
	 *   property access dan {@code dynamicUpdate = true}, kolom {@code nomorUrut} baris ini bisa
	 *   ter-{@code UPDATE} (beserta revisi Envers baru) hanya karena baris dibaca dalam sesi aktif.
	 *   {@link #setNomorUrut(Integer)} karena itu tidak punya pengaruh yang bertahan.</li>
	 * </ul>
	 *
	 * @return nomor urut tampil; {@code 1} bila definisi maupun cache belum punya nilai, tidak pernah
	 *         {@code null}
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel cache nomor urut tampil.
	 *
	 * <p>Praktis tidak berguna: {@link #getNomorUrut()} menimpanya lagi dari
	 * {@link ParameterTambahan} pada pembacaan berikutnya. Disediakan karena dibutuhkan Hibernate
	 * (property access) dan jalur impor data reflektif {@code Common.uploadData(...)} yang
	 * mendaftarkan {@code "nomorUrut"} sebagai salah satu kolom impor pada layar admin.</p>
	 *
	 * @param nomorUrut nomor urut tampil
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Dipakai juga oleh {@code ParameterTambahanPengajuanAction.onAdd()}, yang membuat SATU
	 * instance per parameter yang dicentang pengguna di dialog
	 * {@code AmbilDataParameterTambahanBanyak}, mengisi kedua relasinya, lalu langsung
	 * {@code session.save(...)} tanpa memeriksa duplikat.</p>
	 */
	public ParameterTambahanPengajuan() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>{@code insertable = false} karena nilainya dihasilkan database ({@code IDENTITY}).
	 * Perhatikan bahwa ID ini <b>tidak pernah ikut tersimpan</b> pada string isian di
	 * {@link PengajuanMahasiswa}/{@link ais.database.model.sekolah.PengajuanSiswa}; lihat "Kunci
	 * penyimpanan" pada dokumentasi kelas.</p>
	 *
	 * @return ID baris; {@code null} untuk objek yang belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini; normalnya hanya dilakukan Hibernate.
	 *
	 * <p>Dipakai secara tidak langsung oleh {@code onSave()} layar admin lewat
	 * {@code session.load(ParameterTambahanPengajuan.class, id)} saat mengubah baris yang sudah
	 * ada.</p>
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan <b>definisi</b> field kustom yang diadopsi baris ini &mdash; sumber label, tipe
	 * input, daftar pilihan, keterangan, kewajiban isi/lampiran, dan nomor urut.
	 *
	 * <p>Relasi wajib ({@code nullable = false}). Dipakai hampir di setiap pemakai entity ini:
	 * perakit formulir (yang meneruskannya ke {@code ParameterTambahan.initComponent(...)}),
	 * validator wajib-isi, renderer grid admin (kolom "Harus Menyertakan Lampiran", "Tipe Data",
	 * "Nilai Data"), laporan, dan {@link #getNomorUrut()} di kelas ini sendiri.</p>
	 *
	 * <p>Berbeda dari {@link ParameterTambahanAlumni}, di modul ini definisi dipakai <b>apa
	 * adanya</b>: tidak ada penimpa {@code wajibDiisi} tingkat adopsi, sehingga status wajib sebuah
	 * parameter berlaku seragam di semua kategori pengajuan yang mengadopsinya.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field, agar pemanggil
	 * tidak menerima proxy yang meledak saat sesi sudah ditutup.</p>
	 *
	 * @return definisi field kustom; secara teori tidak pernah {@code null} untuk baris tersimpan,
	 *         namun pemanggil di codebase tetap memeriksanya
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Menyetel definisi field kustom yang diadopsi baris ini.
	 *
	 * <p>Dipanggil {@code onAdd()} (baris baru hasil pilihan massal) dan {@code onSave()} (dari
	 * combobox "Nama Parameter" pada dialog Tambah/Ubah) di layar admin.</p>
	 *
	 * <p><b>Perhatian:</b> mengubah nilai ini berarti mengubah kunci penyimpanan isian
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>), sehingga isian pengaju yang sudah
	 * tersimpan untuk pasangan lama menjadi yatim &mdash; berlaku untuk KEDUA entity pemilik
	 * sekaligus, karena keduanya memakai kunci yang sama.</p>
	 *
	 * @param parameterTambahan definisi field kustom; wajib diisi sebelum baris disimpan
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Mengembalikan <b>kategori/heading</b> tempat field ini ditampilkan pada formulir pengajuan.
	 *
	 * <p>Lapis ketiga rantai field kustom. Sifat kategori inilah yang menentukan apakah field ikut
	 * tampil: seluruh query pembaca mensyaratkan
	 * {@code kelompokParameterTambahanPengajuan.aktif = true}, DAN kategori tersebut harus tercentang
	 * pada {@link JenisPengajuan} yang dipilih pengaju.</p>
	 *
	 * <p>ID kategori ini juga menjadi <b>ruas pertama kunci penyimpanan isian</b>
	 * (<code>idKelompok + "-&gt;" + idParameterTambahan</code>) pada kolom {@code text} entity
	 * pemilik, sekaligus penanda {@code jenis} pada
	 * {@link ais.database.model.file.LampiranLain#ambil(Long, String)}.</p>
	 *
	 * <p><b>Dipetakan {@code nullable = true}</b> (berbeda dari saudara-saudara keluarganya).
	 * Renderer grid layar admin memanggil {@code .getNama()} atas hasil method ini tanpa penjagaan
	 * {@code null}, jadi baris yatim tanpa kategori &mdash; yang hanya bisa lahir dari SQL
	 * mentah/migrasi, bukan dari UI &mdash; akan menggagalkan layar daftar.</p>
	 *
	 * <p><b>Efek samping ringan:</b> proxy lazy diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan hasilnya ditulis balik ke field.</p>
	 *
	 * @return kategori/heading pemilik field ini; secara teori dapat {@code null} sesuai pemetaan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_pengajuan", nullable = true)
	public KelompokParameterTambahanPengajuan getKelompokParameterTambahanPengajuan() {
		kelompokParameterTambahanPengajuan = check(kelompokParameterTambahanPengajuan);
		return kelompokParameterTambahanPengajuan;
	}

	/**
	 * Menyetel kategori/heading pemilik field ini.
	 *
	 * <p>Dipanggil {@code onAdd()} (diambil dari combobox PENCARIAN kategori yang sedang aktif, bukan
	 * dari form isian) dan {@code onSave()} (dari combobox "Kelompok Parameter" pada dialog
	 * Tambah/Ubah, yang divalidasi tidak boleh kosong).</p>
	 *
	 * <p><b>Perhatian:</b> memindahkan baris ke kategori lain mengubah kunci penyimpanan isian,
	 * sehingga seluruh isian pengaju yang sudah tersimpan untuk pasangan lama tidak akan terbaca lagi
	 * (tetap ada di kolom {@code text} entity pemilik, tetapi yatim) &mdash; termasuk lampiran yang
	 * dikunci dengan string yang sama.</p>
	 *
	 * @param kelompokParameterTambahanPengajuan kategori/heading; secara pemetaan boleh {@code null},
	 *                                           tetapi baris tanpa kategori tidak pernah tampil di
	 *                                           formulir mana pun
	 */
	public void setKelompokParameterTambahanPengajuan(
			KelompokParameterTambahanPengajuan kelompokParameterTambahanPengajuan) {
		this.kelompokParameterTambahanPengajuan = kelompokParameterTambahanPengajuan;
	}

}
