package ais.database.model.sekolah;

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

import ais.database.model.GeneralValueObject;

/**
 * Master <b>jenis Surat Keputusan (SK) guru</b> pada modul sekolah — tabel
 * {@code sekolah.jenis_sk_guru}.
 *
 * <h2>Peran sebenarnya: profil template cetak, bukan sekadar taksonomi</h2>
 * <p>Nama kelas ini menyarankan sebuah kategori SK bawaan (SK Pengangkatan, SK Mutasi, SK
 * Kenaikan Pangkat, dan sejenisnya). <b>Kode tidak membenarkan tafsir itu.</b> Tidak ada satu
 * pun konstanta, seed, maupun percabangan {@code if} di seluruh basis kode yang membandingkan
 * {@link #getNama() nama} jenis SK dengan literal tertentu. Seluruh isi tabel diketik bebas oleh
 * admin lewat layar master, dan tidak ada penyemaian awal (instalasi baru mulai dengan tabel
 * kosong).</p>
 *
 * <p>Yang benar-benar dilakukan entity ini adalah menjadi <b>profil/preset satu bentuk cetak
 * SK</b>: setiap baris memasangkan sebuah nama jenis SK dengan satu berkas template
 * <b>JasperReports&nbsp;({@code .jrxml})</b> yang diunggah admin. Templatnya sendiri tidak
 * disimpan di tabel ini, melainkan sebagai baris
 * {@link ais.database.model.file.LampiranLain} dengan
 * {@code jenis = }{@link ais.database.model.file.LampiranLain#FILE_JRXML_LAYOUT_JENIS_FORM_SK_GURU}
 * dan {@code ref = }{@link #getId() id} baris ini. Pola ini identik dengan
 * {@link JenisNilaiSiswa} (template rapor) — bukan pola "kategori data".</p>
 *
 * <h2>Alur pemakaian yang terverifikasi</h2>
 * <ol>
 *   <li><b>Pengisian master</b> — {@code ais.action.master.sekolah.JenisSKGuruAction}
 *   ({@code webapp/WEB-INF/z/x/y/pages/master/sekolah/jenis_sk_guru.zul}). Form isian hanya
 *   memuat empat kendali: <i>Nama Jenis SK Guru</i>, <i>Yayasan</i>, <i>Sekolah</i>,
 *   <i>Keterangan</i>, ditambah dua pengunggah berkas (template {@code .jrxml} dan galeri
 *   gambar/latar pendukung). Kolom {@link #getKode() kode}, {@link #getAktif() aktif}, dan
 *   {@link #getGlondongan() glondongan} <b>tidak punya kendali di form</b> — dua yang terakhir
 *   hanya bisa diubah lewat checkbox di baris grid.</li>
 *   <li><b>Pemakaian</b> — {@code ais.action.report.format1.sekolah.LaporanSKGuru}, yaitu tab
 *   <i>SK Mengajar</i> pada layar Manajemen Guru. Combobox <i>Jenis SK *</i> di layar itu diisi
 *   dari entity ini dengan filter {@code sekolah = <sekolah terpilih> AND aktif = true};
 *   pilihan pengguna menentukan template {@code .jrxml} yang dikompilasi dan dipakai untuk
 *   mencetak berkas PDF {@code Daftar_SK_Guru_Mengajar}.</li>
 * </ol>
 *
 * <h2>Registrasi UI: tidak punya menu sendiri</h2>
 * <p>{@code jenis_sk_guru.zul} <b>tidak pernah</b> dirujuk sebagai target menu. Satu-satunya
 * rujukannya di seluruh aplikasi adalah {@code <include>} pada tab ke-4 (<i>Jenis SK</i>) di
 * {@code webapp/WEB-INF/z/x/y/pages/master/sekolah/guru.zul}, yaitu layar menu <i>Manajemen
 * Guru</i>. Konsekuensinya penting dan dibahas di bagian hak akses di bawah.</p>
 *
 * <h2>Struktur anggota</h2>
 * <ul>
 *   <li><b>Identitas &amp; jejak audit</b> — {@link #getId() id}, {@link #getOleh() oleh},
 *   {@link #getOlehId() olehId}, {@link #getTanggal_dirubah() tanggal_dirubah}, {@link #onUpdate()}.</li>
 *   <li><b>Isi master</b> — {@link #getNama() nama} (wajib), {@link #getKode() kode} (tak pernah
 *   diisi layar mana pun), {@link #getKeterangan() keterangan}.</li>
 *   <li><b>Cakupan tenant</b> — {@link #getSekolah() sekolah}, {@link #getYayasan() yayasan}.</li>
 *   <li><b>Saklar perilaku</b> — {@link #getAktif() aktif} (kelayakan tampil di combobox laporan),
 *   {@link #getGlondongan() glondongan} (mode cetak massal vs per-guru).</li>
 *   <li><b>Utilitas</b> — konstruktor {@link #JenisSKGuru()} dan {@link #toString()}. Tidak ada
 *   method bisnis, query statis, maupun {@code populate*}/{@code sinkronkan*} di kelas ini:
 *   seluruh logika ada di Action/laporan pemanggil.</li>
 * </ul>
 *
 * <h2>Kenapa {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} dideklarasikan ulang</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa, sehingga Hibernate tidak memetakan satu pun properti miliknya.
 * Pengulangan deklarasi field dan getter/setter di sini <b>bukan duplikasi keliru</b>, melainkan
 * keharusan teknis agar kolom-kolom tersebut benar-benar terpetakan ke tabel. Hal yang sama
 * berlaku untuk {@code kode}/{@code nama}/{@code keterangan} yang <i>membayangi</i> (shadow) field
 * senama di kelas induk.</p>
 *
 * <h2>Hal non-obvious yang wajib diketahui sebelum menyunting</h2>
 * <ol>
 *   <li><b>{@link #getYayasan()} adalah getter destruktif.</b> Setiap pembacaan menimpa field
 *   {@code yayasan} dengan yayasan milik {@code sekolah}. Nilai yayasan yang berbeda dari induk
 *   sekolahnya mustahil bertahan. Lihat Javadoc method tersebut.</li>
 *   <li><b>{@link #getKeterangan()} membalik kontrak kelas induk.</b>
 *   {@link GeneralValueObject#getKeterangan()} dijamin tidak pernah {@code null} (mengembalikan
 *   {@code ""}); override di sini mengembalikan field mentah sehingga <b>bisa {@code null}</b>.
 *   Ini mengubah perilaku {@link GeneralValueObject#compareTo(GeneralValueObject)} — lihat
 *   Javadoc method tersebut.</li>
 *   <li><b>Kolom {@code aktif} tak pernah ditulis saat penyimpanan.</b> {@code onSave()} hanya
 *   menulis nama/sekolah/yayasan/keterangan, sehingga baris baru tersimpan dengan {@code aktif}
 *   bernilai {@code NULL}, sementara filter combobox laporan memakai SQL {@code aktif = true}.
 *   Akibat praktisnya dijelaskan lengkap di {@link #getAktif()}.</li>
 *   <li><b>Filter "Tampilkan hanya yang aktif" di layar master adalah kendali mati.</b> ZUL
 *   mendeklarasikan {@code <checkbox id="searchaktif" checked="true">}, tetapi
 *   {@code JenisSKGuruAction} tidak punya field bernama {@code searchaktif} sehingga autowire ZK
 *   tidak mengikatnya, dan {@code initCriteria()} tidak pernah menambahkan pembatasan
 *   {@code aktif}. Checkbox tampak tercentang namun grid tetap menampilkan baris nonaktif.</li>
 * </ol>
 *
 * <h2>Hak akses &amp; cakupan tenant (hasil verifikasi kode)</h2>
 * <ul>
 *   <li><b>Gerbang login ADA.</b> {@code JenisSKGuruAction.doBeforeCompose()} memanggil
 *   {@code Common.doCheckSecurity()}, dan tombol Tambah/Ubah/Hapus/Impor dikawal
 *   {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}. Layar ini <b>bukan</b>
 *   contoh "nol {@code checkPrevilages}".</li>
 *   <li><b>Namun hak itu diwarisi dari menu induk.</b> {@code checkPrevilages(kode)} me-resolve
 *   privilese terhadap {@code Common.getCurrentMenu()}. Karena {@code jenis_sk_guru.zul} hanya
 *   hidup sebagai tab di dalam {@code guru.zul}, menu aktifnya selalu <i>Manajemen Guru</i>.
 *   Artinya siapa pun yang diberi hak CREATE/UPDATE/DELETE pada menu Guru otomatis memperoleh
 *   hak penuh atas master jenis SK — tidak ada objek hak akses tersendiri yang bisa dicabut
 *   administrator. Ini instance lanjutan dari pola <i>pewarisan hak lewat menu induk</i> yang
 *   sudah tercatat pada {@code PaketPsb}, {@code KategoriItemPenilaianSiswa}, dan
 *   {@code SubMatapelajaran}.</li>
 *   <li><b>Cakupan tenant fail-open.</b> {@code initCriteria()} menyaring
 *   {@code sekolah}/{@code yayasan} <i>hanya</i> bila combobox pencarian punya item terpilih;
 *   bila tidak, yang ditambahkan adalah {@code Restrictions.sqlRestriction("1=1")}. Combobox itu
 *   diisi {@code Common.initYayasanDanSekolahDanSemua(...)} yang mengunci pilihan ke konteks
 *   sekolah/yayasan aktif <i>bila ada</i>; ketika konteks maupun penugasan sekolah/yayasan
 *   pengguna sama-sama kosong, tidak ada pembatasan apa pun yang tersisa dan grid menampilkan
 *   jenis SK seluruh instalasi. Penguncian juga hanya {@code setDisabled(...)} di sisi klien,
 *   bukan penegakan di sisi server. Ini varian yang sama dengan pola fail-open cakupan tenant
 *   yang sudah tercatat pada banyak layar master lain.</li>
 *   <li><b>Amplifier template JasperReports.</b> Dampak dua poin di atas tidak berhenti pada
 *   metadata: pemegang hak UPDATE menu Guru dapat membuka baris jenis SK milik sekolah lain dan
 *   <b>mengganti berkas {@code .jrxml}-nya</b>. Berkas itu kemudian dikompilasi
 *   ({@code JasperCompileManager.compileReportToFile}) lalu dijalankan di server saat SK dicetak,
 *   sedangkan ekspresi JasperReports adalah kode Java. Ini instance keempat pola "unggah ulang
 *   template JasperReports lintas tenant" (setelah {@code JenisCatatanGuru},
 *   {@code JenisNilaiSiswa}, dan keluarga {@code OrganisasiSiswa}) dan yang paling langsung
 *   terhubung ke kekhawatiran eksekusi kode dari unggahan {@code .jrxml}. Tidak dibuat task baru
 *   — temuan ini memperkuat task audit unggahan {@code .jrxml} yang sudah ada.</li>
 * </ul>
 *
 * <h2>Catatan jejak generator</h2>
 * <p>Javadoc lama berkas ini berbunyi {@code "Bank generated by hbm2java"} — sisa salin-tempel
 * dari entity {@code Bank} yang tersebar ke ratusan berkas model lain; kelas ini tidak ada
 * hubungannya dengan bank. Jejak salin-tempel serupa masih tampak di beberapa tempat lain:
 * judul jendela bawaan di ZUL berbunyi {@code "Tambah Jenis Guru"} (ditimpa saat runtime menjadi
 * "Tambah/Ubah Jenis SK Guru", jadi tidak terlihat pengguna), dan pesan validasi
 * {@code onSave()} berbunyi <i>"Nama Jenis Sekolah harus diisi"</i> — keduanya murni kosmetik.</p>
 *
 * @see ais.action.master.sekolah.JenisSKGuruAction
 * @see ais.action.report.format1.sekolah.LaporanSKGuru
 * @see ais.database.model.file.LampiranLain#FILE_JRXML_LAYOUT_JENIS_FORM_SK_GURU
 * @see JenisNilaiSiswa
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "jenis_sk_guru")
public class JenisSKGuru extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya dibangkitkan otomatis oleh perkakas dan sengaja dipertahankan agar instance
	 * yang tersimpan di session ZK atau cache tetap kompatibel setelah kelas disunting. Jangan
	 * diubah.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci primer baris, kolom {@code id}. Dibangkitkan basis data ({@code IDENTITY}), berurutan
	 * dan mudah ditebak. Dipakai juga sebagai {@code ref} pada baris
	 * {@link ais.database.model.file.LampiranLain} yang menyimpan template {@code .jrxml} dan
	 * gambar pendukung milik jenis SK ini.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini, kolom {@code oleh}. Diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar master.
	 */
	private String oleh;

	/**
	 * Id pengguna terakhir yang mengubah baris ini, kolom {@code oleh_id}. Pasangan teknis dari
	 * {@link #oleh}, juga diisi oleh interceptor audit.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila baris belum pernah melewati
	 *         interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna penyunting terakhir, <b>dengan penjaga anti-penghapusan</b>.
	 *
	 * <p>Nilai {@code null} maupun string kosong/berisi spasi saja <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menulis apa pun), sehingga jejak audit yang sudah
	 * ada tidak bisa terhapus oleh pemanggil yang lalai. Perilaku ini seragam di seluruh entity
	 * AIS dan bukan bug.</p>
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyunting terakhir, dengan penjaga anti-penghapusan yang sama
	 * seperti {@link #setOlehId(String)}: nilai {@code null} atau kosong diabaikan diam-diam.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama pengguna penyunting terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang menyegarkan jejak audit tepat sebelum baris ini
	 * di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan seluruhnya ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String) oleh}/{@link #setOlehId(String) olehId} dari pengguna yang sedang
	 * login dan memutakhirkan {@link #setTanggal_dirubah(Date) tanggal_dirubah}. Dipanggil oleh
	 * penyedia persistensi, <b>bukan</b> oleh kode aplikasi — jangan memanggilnya langsung.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga field audit pada instance ini. Karena entity dianotasi
	 * {@code @Audited} (Hibernate Envers), setiap {@code UPDATE} juga melahirkan satu revisi baru
	 * di tabel riwayat.</p>
	 *
	 * <p><b>Catatan bentuk kode:</b> field {@code tanggal_dirubah} sengaja dibiarkan berbagi baris
	 * fisik dengan deklarasi method ini — bentuk warisan generator yang tidak dirapikan agar diff
	 * berkas ini tetap murni Javadoc. Field tersebut menyimpan waktu perubahan terakhir (kolom
	 * {@code tanggal_dirubah}) dan diberi nilai awal {@code ais.ui.util.WaktuUtil.getDate()}
	 * sehingga baris baru selalu punya stempel waktu meski belum pernah disunting; pemetaan
	 * {@code TIMESTAMP}-nya dideklarasikan pada {@link #getTanggal_dirubah()}.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Tanpa validasi; normalnya hanya dipanggil
	 * {@link #onUpdate()} lewat interceptor audit.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini, dipetakan sebagai kolom
	 * {@code TIMESTAMP}.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang
	 *         dibuat lewat konstruktor karena field-nya diberi nilai awal waktu saat ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris ini dalam bentuk {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} secara langsung (bukan lewat {@link #getNama()}), sehingga
	 * spasi di ujung nama <b>tidak</b> dipangkas di sini dan nilai {@code null} tampil sebagai
	 * literal {@code "null"} — mis. {@code "12-SK Pembagian Tugas Mengajar"} atau
	 * {@code "null-null"} untuk instance yang belum disimpan.</p>
	 *
	 * <p>Dipakai untuk log/debug, dan menjadi label cadangan pada komponen ZK yang tidak
	 * menyetel label secara eksplisit. Tidak dipakai untuk keputusan bisnis apa pun.</p>
	 *
	 * @return {@code id} dan {@code nama} yang digabung dengan tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode ringkas jenis SK, kolom {@code kode}.
	 *
	 * <p><b>Praktis tak terpakai.</b> Tidak ada layar, importer, maupun API yang pernah
	 * mengisinya — form master hanya punya isian Nama/Yayasan/Sekolah/Keterangan, dan daftar
	 * kolom impor/ekspor Excel-nya ({@code id, nama, sekolah, keterangan, glondongan, aktif})
	 * juga tidak menyebut {@code kode}. Satu-satunya pembaca adalah label combobox laporan; lihat
	 * {@link #getKode()}.</p>
	 */
	private String kode;

	/**
	 * Nama jenis SK, kolom {@code nama} bertipe {@code text} dan {@code NOT NULL}. Satu-satunya
	 * isian wajib pada form master dan satu-satunya identitas jenis SK yang dilihat pengguna.
	 */
	private String nama;

	/**
	 * Sekolah pemilik baris ini, kolom {@code sekolah_id}. Menjadi kunci penyaringan combobox
	 * <i>Jenis SK</i> pada layar cetak SK Mengajar.
	 */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik baris ini, kolom {@code yayasan_id}. Nilainya bersifat turunan: selalu
	 * disamakan ulang dengan yayasan milik {@link #sekolah} setiap kali dibaca — lihat
	 * {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Keterangan bebas, kolom {@code keterangan} bertipe {@code text} dan boleh {@code NULL}.
	 * Ditampilkan sebagai kolom grid di layar master dan sebagai deskripsi item pada combobox
	 * <i>Jenis SK</i> di layar cetak.
	 */
	private String keterangan;

	/**
	 * Saklar aktif, kolom {@code aktif}. Menentukan apakah jenis SK ini muncul sebagai pilihan
	 * saat mencetak SK. Perhatikan perbedaan perlakuan {@code NULL} antara getter dan SQL yang
	 * dibahas di {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Saklar mode cetak "glondongan" (borongan/massal), kolom {@code glondongan}. Mengubah
	 * sumber data dan cakupan berkas SK yang dihasilkan — lihat {@link #getGlondongan()}.
	 */
	private Boolean glondongan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Semua field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang sudah diberi
	 * nilai awal waktu saat ini pada deklarasinya. Dipakai pula oleh {@code JenisSKGuruAction}
	 * saat tombol "Tambah" ditekan, untuk menyiapkan baris kosong bagi form isian.</p>
	 */
	public JenisSKGuru() {
	}

	/**
	 * Mengembalikan kunci primer baris ini.
	 *
	 * <p>Dibangkitkan basis data ({@code IDENTITY}) dan <b>tidak</b> disertakan pada
	 * {@code INSERT} ({@code insertable = false}). Bernilai {@code null} untuk instance yang
	 * belum tersimpan — kondisi ini dipakai layar master untuk membedakan modus "Tambah" dari
	 * "Ubah", dan dipakai laporan sebagai {@code ref} saat mencari template {@code .jrxml}
	 * milik jenis SK ini.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris ini. Tanpa validasi; normalnya hanya dipanggil Hibernate.
	 *
	 * @param id kunci primer baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode ringkas jenis SK, dengan normalisasi: {@code null} dikembalikan sebagai
	 * string kosong dan spasi di kedua ujung dipangkas.
	 *
	 * <p><b>Kontras dengan kelas induk.</b> {@link GeneralValueObject#getKode()} mengembalikan
	 * field apa adanya (boleh {@code null}); override di sini menjamin hasil non-{@code null}.</p>
	 *
	 * <p><b>Satu-satunya pembaca nyata</b> adalah pembangun label combobox <i>Jenis SK</i> di
	 * {@code LaporanSKGuru}, yang memanggil
	 * {@code Common.insertCombo(combo, new String[]{"nama","kode"}, "keterangan", ...)}. Helper
	 * itu merangkai label dari properti yang tidak kosong dengan pemisah {@code " - "}. Karena
	 * tidak ada layar yang pernah mengisi {@code kode}, nilainya selalu kosong, dilewati helper,
	 * dan label combobox pada praktiknya selalu berisi nama saja — jadi elemen {@code "kode"}
	 * pada pemanggilan tersebut efektif tidak berpengaruh.</p>
	 *
	 * @return kode jenis SK yang sudah dipangkas, atau {@code ""} bila belum diisi; tidak pernah
	 *         {@code null}
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menyetel kode ringkas jenis SK. Tanpa validasi maupun normalisasi.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama jenis SK dengan spasi di kedua ujung dipangkas, atau {@code null} bila
	 * memang belum diisi.
	 *
	 * <p>Dipetakan ke kolom {@code nama} bertipe {@code text} dan {@code NOT NULL}; keharusan
	 * pengisiannya ditegakkan di sisi UI oleh {@code JenisSKGuruAction.onSave()} sebelum
	 * penyimpanan. Nilai ini dipakai sebagai judul baris di grid master, label item combobox
	 * <i>Jenis SK</i> di layar cetak, dan judul revisi Envers.</p>
	 *
	 * <p><b>Perbedaan dengan {@link #getKode()}:</b> method ini <b>tidak</b> mengubah {@code null}
	 * menjadi {@code ""} — perilakunya mengikuti {@link GeneralValueObject#getNama()} dan menjaga
	 * penjaga null pada cabang ketiga
	 * {@link GeneralValueObject#compareTo(GeneralValueObject) compareTo} tetap bermakna.</p>
	 *
	 * @return nama jenis SK yang sudah dipangkas, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama jenis SK. Tanpa validasi maupun pemangkasan spasi — pemangkasan hanya terjadi
	 * saat pembacaan lewat {@link #getNama()}, sehingga nilai mentah beserta spasinya tetap
	 * tersimpan di basis data.
	 *
	 * @param nama nama jenis SK yang baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas jenis SK, <b>apa adanya</b>.
	 *
	 * <h3>Perhatian: override ini membalik kontrak kelas induk</h3>
	 * <p>{@link GeneralValueObject#getKeterangan()} menormalkan {@code null} menjadi {@code ""}
	 * dan secara eksplisit menjanjikan hasil yang <b>tidak pernah {@code null}</b>. Override di
	 * sini mengembalikan field mentah, sehingga untuk {@code JenisSKGuru} janji itu
	 * <b>tidak berlaku</b> dan pemanggil wajib memeriksa {@code null} sendiri.</p>
	 *
	 * <p>Dua konsekuensi yang terverifikasi:</p>
	 * <ul>
	 *   <li><b>Urutan combobox jadi tak deterministik.</b> {@code Common.insertCombo(...)}
	 *   memanggil {@code Collections.sort(list)} yang jatuh ke
	 *   {@link GeneralValueObject#compareTo(GeneralValueObject)}. Cabang keempat method itu
	 *   ({@code keterangan}) dirancang <i>selalu</i> memenuhi syarat karena getter induknya tidak
	 *   pernah {@code null}. Dengan override ini, dua baris yang {@code keterangan}-nya
	 *   {@code null} melewati keempat cabang dan {@code compareTo} mengembalikan {@code 0}
	 *   ("dianggap setara") — urutan akhirnya bergantung pada urutan hasil query, bukan pada
	 *   isinya. Dampaknya kecil dalam praktik karena cabang {@code nama} lebih dulu memenuhi
	 *   syarat untuk baris yang namanya terisi (dan {@code nama} adalah {@code NOT NULL}).</li>
	 *   <li><b>Pemanggil yang menganggap hasil non-{@code null} bisa NPE.</b> Pemakaian yang ada
	 *   saat ini aman ({@code new Label(...)} di renderer grid menerima {@code null}, dan helper
	 *   combobox memeriksa {@code null} sebelum memakai deskripsi), tetapi kode baru yang
	 *   langsung memanggil {@code .trim()}/{@code .isEmpty()} pada hasil method ini akan gagal
	 *   untuk baris tanpa keterangan.</li>
	 * </ul>
	 *
	 * @return keterangan jenis SK, atau {@code null} bila belum diisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas jenis SK. Tanpa validasi; nilai {@code null} diterima dan akan
	 * terbaca kembali sebagai {@code null} (lihat {@link #getKeterangan()}).
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan saklar aktif jenis SK, dengan default <b>fail-open</b>: {@code null}
	 * dilaporkan sebagai {@code true}.
	 *
	 * <h3>Divergensi getter vs SQL — jenis SK baru tidak pernah muncul di layar cetak</h3>
	 * <p>Ini bukan sekadar kenyamanan pemrograman, melainkan sumber satu bug fungsional nyata
	 * yang mudah disalahartikan sebagai kesalahan pengguna:</p>
	 * <ol>
	 *   <li>{@code JenisSKGuruAction.onSave()} <b>tidak pernah menulis kolom {@code aktif}</b> —
	 *   yang ditulis hanya nama, sekolah, yayasan, dan keterangan. Baris yang baru dibuat karena
	 *   itu tersimpan dengan {@code aktif = NULL} di basis data.</li>
	 *   <li>Renderer grid master menampilkan checkbox "Aktif" dengan
	 *   {@code setChecked(getAktif())}, dan karena method ini memetakan {@code NULL} ke
	 *   {@code true}, checkbox itu <b>tampak sudah tercentang</b>.</li>
	 *   <li>Sementara itu {@code LaporanSKGuru} menyaring pilihan combobox dengan
	 *   {@code Restrictions.eq("aktif", true)} — pembatasan SQL yang dievaluasi basis data, di
	 *   mana {@code NULL = true} bernilai <i>unknown</i> sehingga baris tersebut
	 *   <b>tersaring keluar</b>.</li>
	 * </ol>
	 * <p>Akibatnya jenis SK yang baru saja dibuat tidak muncul sebagai pilihan saat mencetak SK,
	 * meski di layar master terlihat aktif. Pemulihannya tidak intuitif: admin harus
	 * <i>menghilangkan</i> centang "Aktif" di grid (menulis {@code false}) lalu
	 * <i>mencentangnya kembali</i> (menulis {@code true}), karena hanya event {@code onCheck}
	 * pada checkbox grid yang benar-benar memanggil {@link #setAktif(Boolean)}. Hal yang sama
	 * juga membuat penghitung {@code adaJenisSk} di layar laporan bernilai {@code 0}, sehingga
	 * gerbang "Jenis SK harus dipilih" ikut mati dan laporan dicetak tanpa template khusus.</p>
	 * <p>Pola "kolom {@code aktif} tak pernah ditulis layar master" ini kembar dengan yang sudah
	 * tercatat pada {@code JenisCatatanSiswa} dan {@code JenisNilaiSiswa}.</p>
	 *
	 * @return {@code true} bila jenis SK dianggap aktif — termasuk ketika kolomnya masih
	 *         {@code NULL}; {@code false} hanya bila memang pernah disetel demikian
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel saklar aktif jenis SK. Tanpa validasi; nilai {@code null} diterima dan akan
	 * terbaca kembali sebagai {@code true} lewat {@link #getAktif()}.
	 *
	 * <p><b>Satu-satunya pemanggil nyata</b> adalah event {@code onCheck} pada checkbox "Aktif"
	 * di baris grid layar master, yang langsung disusul {@code Common.refreshSaveOrUpdate(...)}
	 * sehingga perubahannya tersimpan seketika tanpa membuka form. Checkbox itu di-{@code
	 * setDisabled(!edit)} sesuai hak UPDATE, namun penonaktifan tersebut hanya berlaku di sisi
	 * klien.</p>
	 *
	 * @param aktif saklar aktif yang baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini, dengan resolusi proxy lazy.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code sekolah_id}. Sebelum dikembalikan, nilai
	 * dilewatkan {@link GeneralValueObject#check(Object)} yang mencoba menyelesaikan proxy
	 * Hibernate yang mungkin sudah lepas dari session-nya, lalu <b>menuliskan kembali hasilnya
	 * ke field</b>. Jadi method ini bukan getter murni: ia memutasi state instance (mengganti
	 * proxy dengan object terinisialisasi). Ini pola standar seluruh entity AIS dan aman
	 * dipanggil berulang — biayanya murah setelah resolusi pertama.</p>
	 *
	 * <p><b>Peran bisnis:</b> menentukan sekolah mana yang boleh memakai jenis SK ini. Combobox
	 * <i>Jenis SK</i> di layar cetak SK Mengajar menyaring dengan
	 * {@code sekolah = <sekolah terpilih>}, sehingga baris tanpa sekolah ({@code NULL}) tidak
	 * akan pernah bisa dipilih untuk mencetak SK. Sekaligus menjadi kolom yang disaring kotak
	 * pencarian "Sekolah" di layar master — dengan catatan fail-open yang dijelaskan pada Javadoc
	 * kelas.</p>
	 *
	 * @return sekolah pemilik yang sudah terinisialisasi, atau {@code null} bila belum disetel
	 *         atau tidak berhasil diselesaikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris ini, dengan penjaga terhadap instance transien.
	 *
	 * <p>Argumen yang {@code null} <b>atau</b> yang {@code id}-nya masih {@code null} (mis. objek
	 * {@code Sekolah} hasil {@code new} yang belum tersimpan, atau nilai kosong dari sebuah
	 * combobox) disimpan sebagai {@code null}. Penjaga ini mencegah
	 * {@code TransientObjectException} saat Hibernate mencoba menyelesaikan identifier relasi
	 * pada waktu {@code flush}.</p>
	 *
	 * <p><b>Efek lanjutan yang mudah terlewat:</b> karena {@link #getYayasan()} selalu menurunkan
	 * yayasan dari sekolah, mengganti sekolah lewat method ini secara efektif juga memindahkan
	 * baris ini ke yayasan milik sekolah yang baru pada penyimpanan berikutnya.</p>
	 *
	 * @param sekolah sekolah pemilik yang baru; {@code null} atau instance tanpa {@code id}
	 *                diperlakukan sebagai "tanpa sekolah"
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini — <b>getter destruktif yang selalu menurunkan
	 * ulang nilainya dari {@link #getSekolah() sekolah}</b>.
	 *
	 * <p>Urutan kerjanya: baca sekolah (yang sekaligus meresolusi proxy-nya); bila sekolah
	 * ada, <b>timpa</b> field {@code yayasan} dengan {@code sekolah.getYayasan()}; baru kemudian
	 * lewatkan hasilnya ke {@link GeneralValueObject#check(Object)} dan tulis kembali ke field.
	 * Jadi setiap pembacaan berpotensi memutasi dua field sekaligus.</p>
	 *
	 * <h3>Konsekuensi yang perlu disadari</h3>
	 * <ul>
	 *   <li><b>Yayasan bukan data yang bisa berdiri sendiri.</b> Nilai apa pun yang disetel lewat
	 *   {@link #setYayasan(Yayasan)} akan hilang begitu sekolah terisi — termasuk nilai yang baru
	 *   saja ditulis {@code onSave()} dari combobox. Ini konsisten dengan UI, karena combobox
	 *   Yayasan pada form master memang {@code readonly} dan terkunci mengikuti sekolah.</li>
	 *   <li><b>Penulisan senyap ke basis data.</b> Entity ini memakai {@code dynamicUpdate}, dan
	 *   Hibernate membaca nilai properti lewat getter saat memeriksa perubahan. Baris lama yang
	 *   {@code yayasan_id}-nya tidak konsisten dengan sekolahnya akan diperbaiki diam-diam pada
	 *   {@code flush} berikutnya — sekaligus melahirkan satu revisi Envers yang tampak seperti
	 *   suntingan pengguna padahal tidak ada yang menyunting apa pun.</li>
	 *   <li><b>Baris tanpa sekolah tidak tersentuh.</b> Bila {@code sekolah} {@code null}, field
	 *   {@code yayasan} dibiarkan apa adanya, sehingga baris "yatim yayasan" tetap mungkin ada
	 *   dan tetap lolos dari filter sekolah di layar cetak.</li>
	 * </ul>
	 *
	 * @return yayasan pemilik — praktis selalu yayasan milik {@link #getSekolah()} bila sekolah
	 *         terisi; {@code null} bila tidak ada satu pun yang bisa diselesaikan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik baris ini, dengan penjaga instance transien yang sama seperti
	 * {@link #setSekolah(Sekolah)}: {@code null} atau instance tanpa {@code id} disimpan sebagai
	 * {@code null}.
	 *
	 * <p><b>Umur nilainya pendek.</b> Selama {@link #getSekolah()} tidak {@code null},
	 * {@link #getYayasan()} akan menimpa apa pun yang disetel di sini dengan yayasan milik
	 * sekolah. Method ini karena itu hanya bermakna untuk baris yang memang tidak punya
	 * sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik yang baru; {@code null} atau instance tanpa {@code id}
	 *                diperlakukan sebagai "tanpa yayasan"
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan saklar mode cetak "glondongan" (borongan/massal), dengan default
	 * <b>fail-closed</b>: {@code null} dilaporkan sebagai {@code false}.
	 *
	 * <p>Perhatikan asimetri yang disengaja dengan {@link #getAktif()} (yang defaultnya
	 * {@code true}): baris yang belum pernah disentuh admin bersikap seperti SK per-guru biasa,
	 * bukan seperti cetakan massal.</p>
	 *
	 * <h3>Apa yang benar-benar diubah saklar ini</h3>
	 * <p>Dibaca di tiga titik pada {@code LaporanSKGuru}, dan menentukan <b>dua alur cetak yang
	 * sepenuhnya berbeda</b>:</p>
	 * <ul>
	 *   <li><b>{@code false} — SK per guru.</b> Isian "Guru" pada panel filter ditampilkan, dan
	 *   data laporan dirakit dari {@link JadwalPelajaran} (sepuluh slot guru per baris jadwal)
	 *   sehingga tiap baris cetakan berisi satu sesi mengajar lengkap dengan hari, jam pelajaran,
	 *   mata pelajaran, kelas, dan ruang.</li>
	 *   <li><b>{@code true} — SK borongan.</b> Isian "Guru" <b>disembunyikan</b> dan nilainya
	 *   dipaksa {@code null} di {@code generateParameter()}, sehingga filter per-guru dilucuti
	 *   dan cetakan mencakup <i>seluruh</i> guru sekaligus. Sumber datanya pun berpindah ke
	 *   {@link PenugasanGuruMengajar} (satu baris per surat tugas mengajar per tahun akademik dan
	 *   semester), bukan lagi jadwal per sesi.</li>
	 * </ul>
	 * <p>Karena itu mengubah saklar ini pada jenis SK yang templatnya sudah terlanjur dirancang
	 * untuk alur yang satunya akan menghasilkan PDF dengan field-field kosong: kedua alur mengisi
	 * kumpulan parameter yang berbeda (mis. {@code hari}, {@code waktu_mulai}, {@code kelas},
	 * {@code mata_kuliah} hanya ada di alur per-guru).</p>
	 *
	 * <p><b>Titik penyuntingan satu-satunya</b> adalah checkbox "Glondongan" di baris grid layar
	 * master — tidak ada isian untuknya di form Tambah/Ubah.</p>
	 *
	 * @return {@code true} bila jenis SK ini dicetak secara borongan untuk seluruh guru;
	 *         {@code false} bila dicetak per guru — termasuk ketika kolomnya masih {@code NULL}
	 */
	public Boolean getGlondongan() {
		return glondongan == null ? false : glondongan;
	}

	/**
	 * Menyetel saklar mode cetak glondongan. Tanpa validasi; nilai {@code null} diterima dan akan
	 * terbaca kembali sebagai {@code false} lewat {@link #getGlondongan()}.
	 *
	 * <p>Sama seperti {@link #setAktif(Boolean)}, satu-satunya pemanggil nyata adalah event
	 * {@code onCheck} checkbox "Glondongan" di baris grid layar master, yang langsung disusul
	 * {@code Common.refreshSaveOrUpdate(...)} sehingga tersimpan seketika.</p>
	 *
	 * @param glondongan saklar mode cetak borongan yang baru
	 */
	public void setGlondongan(Boolean glondongan) {
		this.glondongan = glondongan;
	}

}
