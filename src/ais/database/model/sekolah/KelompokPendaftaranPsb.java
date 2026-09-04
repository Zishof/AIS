package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Entity master <b>Kelompok Pendaftaran PSB</b> &mdash; satuan pengelompokan calon siswa di
 * dalam sebuah {@link GelombangPendaftaranPsb} (Penerimaan Siswa Baru / PSB tingkat sekolah).
 * Dipetakan ke tabel {@code sekolah.kelompok_pendaftaran_psb}.
 *
 * <p><b>Peran domain (TERVERIFIKASI dari kode, bukan dugaan).</b> Satu baris entity ini
 * mendefinisikan sebuah "kantong" pendaftaran: nama kelompok, <i>kuota</i> calon siswa yang
 * boleh masuk, sekolah/yayasan pemilik, dan &mdash; opsional &mdash; {@link PenjurusanSekolah}
 * yang dituju. Layar pengelolanya adalah
 * {@code /pages/master/sekolah/kelompok_pendaftaran.zul} yang di-<i>apply</i> ke
 * {@code ais.action.master.sekolah.KelompokPendaftaranPsbAction}; menu "Kelompok Pendaftaran"
 * (id 187623) berada tepat di bawah grup menu "Penerimaan Siswa Baru" bersama "Gelombang
 * Pendaftaran" dan "Calon Siswa" (lihat {@code ais.common.MenuInitializer}). Istilah resmi
 * yang dipakai mesin SOP untuk entity ini adalah <i>"Penentuan kelompok dan kuota calon
 * siswa"</i> ({@code KelompokPendaftaranPsbAction.istilah()}).</p>
 *
 * <p><b>Analogi yang sering keliru.</b> Entity ini adalah padanan PSB (jenjang sekolah) dari
 * {@code ais.database.model.KelompokCalonMahasiswa} (jenjang perguruan tinggi, modul PMB) dan
 * {@code ais.database.model.recruitment.KelompokPendaftaranPegawai} (rekrutmen pegawai).
 * <b>Ia BUKAN</b> padanan PSB dari
 * {@link ais.database.model.sekolah.KelompokGelombang}: {@code KelompokGelombang}
 * mengelompokkan <i>gelombang</i> PMB, sementara entity ini mengelompokkan <i>pendaftar</i> di
 * dalam satu gelombang PSB. Arah relasinya pun berlawanan &mdash; di sini FK
 * {@code gelombang_pendaftaran_id} ada di sisi kelompok dan bersifat WAJIB
 * ({@code nullable = false}).</p>
 *
 * <h3>Relasi</h3>
 * <ul>
 *   <li><b>Ke atas &mdash; wajib:</b> {@link #getGelombangPendaftaran()} &rarr;
 *       {@link GelombangPendaftaranPsb}. Kolom {@code gelombang_pendaftaran_id},
 *       {@code nullable = false}; layar menolak simpan bila belum dipilih.</li>
 *   <li><b>Tenant:</b> {@link #getSekolah()} dan {@link #getYayasan()} (keduanya wajib di
 *       layar, tetapi {@code nullable} di level kolom). {@code yayasan} bersifat
 *       <i>turunan</i> &mdash; lihat catatan pada {@link #getYayasan()}.</li>
 *   <li><b>Opsional:</b> {@link #getPenjurusanSekolah()} &rarr; {@link PenjurusanSekolah},
 *       hanya ditawarkan bila {@code Sekolah.getPenjurusanBolehDipilihSaatPsb()} bernilai
 *       true dan penjurusannya {@code aktif && tampilkanDiPpdb}.</li>
 *   <li><b>Alur persetujuan:</b> {@link #getDisposisiSop()} &rarr;
 *       {@link DisposisiSop} (kolom {@code disposisi_sop}); kelas ini turunan
 *       {@link DataSop} sehingga dapat dipasang sebagai formulir lampiran pada alur SOP.</li>
 *   <li><b>Ke bawah &mdash; pemakai satu-satunya:</b> {@link CalonSiswa} memegang
 *       {@code @ManyToOne} ke entity ini lewat kolom {@code kelompok_pendaftaran_psb}
 *       ({@code CalonSiswa.getKelompokPendaftaranPsb()}). Tidak ada koleksi balik di sisi
 *       ini.</li>
 * </ul>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Identitas &amp; audit:</b> {@link #getId()}, {@link #setId(Long)},
 *       {@link #getOleh()}, {@link #setOleh(String)}, {@link #getOlehId()},
 *       {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Relasi:</b> {@link #getGelombangPendaftaran()}, {@link #getSekolah()},
 *       {@link #getYayasan()}, {@link #getPenjurusanSekolah()},
 *       {@link #getDisposisiSop()} beserta setter masing-masing.</li>
 *   <li><b>Atribut bisnis:</b> {@link #getNama()}, {@link #getKuota()},
 *       {@link #getDeskripsi()}, {@link #getSkorSampai()}, {@link #getAktif()}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui sebelum menyentuh kelas ini</h3>
 * <ol>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *       BUKAN duplikasi yang bisa dihapus.</b> Rantai pewarisannya adalah
 *       {@code KelompokPendaftaranPsb} &rarr; {@link DataSop} &rarr;
 *       {@link ais.database.model.GeneralValueObject}, dan
 *       {@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 *       {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa. Hibernate karena itu TIDAK
 *       memetakan satu pun properti induknya. Semua kolom audit harus dideklarasikan ulang di
 *       tiap entity konkret; ini keharusan teknis, bukan bug.</li>
 *   <li><b>Konsekuensi langsung: kolom "Keterangan" di layar TIDAK PERNAH TERSIMPAN.</b>
 *       {@code KelompokPendaftaranPsbAction} membaca dan menulis properti
 *       {@code keterangan} ({@code setKeterangan(keterangan.getValue())} di {@code onSave()},
 *       {@code getKeterangan()} di renderer dan di daftar kolom cetak/unggah). Properti
 *       {@code keterangan} itu milik {@code GeneralValueObject} yang &mdash; sesuai butir di
 *       atas &mdash; tidak dipetakan Hibernate, sehingga nilainya hanya hidup selama satu
 *       request lalu hilang. Sementara itu entity ini memiliki kolom yang benar-benar
 *       dipetakan untuk keperluan yang sama, yaitu {@link #getDeskripsi()}
 *       ({@code @Column(name = "deskripsi")}), yang <b>tidak dipakai oleh satu pun layar,
 *       Action, JSP, atau API</b> di codebase. Efek yang terlihat pengguna: keterangan yang
 *       diketik saat menambah/mengubah kelompok tampil normal sampai layar di-refresh, lalu
 *       kolom "Keterangan" pada grid selamanya kosong. Perbaikan yang benar adalah
 *       mengalihkan layar ke {@link #getDeskripsi()}/{@link #setDeskripsi(String)}, bukan
 *       menambah pemetaan baru.</li>
 *   <li><b>Nilai relasi yang belum ada di kolom {@code CalonSiswa}.</b> Pencarian menyeluruh
 *       atas codebase menunjukkan {@code CalonSiswa.setKelompokPendaftaranPsb(..)} tidak
 *       pernah dipanggil dari mana pun (tidak dari layar Calon Siswa, tidak dari jalur
 *       pendaftaran publik {@code /ppdb}, tidak dari API). Artinya kuota, penjurusan, dan
 *       {@link #getSkorSampai()} yang diisi rapi di layar ini <b>belum dikonsumsi oleh logika
 *       apa pun</b>: tidak ada penegakan kuota, tidak ada penempatan otomatis pendaftar ke
 *       kelompok. Bandingkan dengan padanan PMB-nya yang sudah lengkap
 *       ({@code KelompokCalonMahasiswaDetailAction} memakai {@code skorMulai}/{@code skorSampai}
 *       untuk menempatkan calon berdasarkan skor). Perlakukan entity ini sebagai master yang
 *       sudah siap tetapi mesin penempatannya belum ditulis.</li>
 *   <li><b>Getter yang MENULIS (write-back).</b> {@link #getYayasan()} dan
 *       {@link #getAktif()} bukan getter murni: keduanya menimpa field instance saat dibaca.
 *       Karena Hibernate memakai <i>property access</i> pada entity ini (anotasi {@code @Id}
 *       berada pada getter), nilai hasil timpa itulah yang ikut terbawa ke {@code UPDATE}
 *       pada flush berikutnya bila entity sedang <i>managed</i>. Rincian pada masing-masing
 *       method.</li>
 *   <li><b>Setter yang menolak nilai kosong (tidak bisa dikosongkan kembali).</b>
 *       {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
 *       {@link #setDisposisiSop(DisposisiSop)} melakukan <i>early return</i> untuk argumen
 *       null/kosong, sehingga nilai lama dipertahankan. Untuk {@code disposisiSop} ini berarti
 *       kaitan ke alur SOP <b>tidak dapat dilepas lewat setter</b>.</li>
 *   <li><b>Penghapusan disposisi SOP menghapus baris master ini.</b>
 *       {@code DisposisiSop.hapus()} menjalankan SQL native
 *       {@code delete from sekolah.kelompok_pendaftaran_psb where disposisi_sop=&lt;id&gt;}
 *       &mdash; jadi membatalkan/menghapus sebuah disposisi SOP <b>ikut menghapus kelompok
 *       pendaftarannya</b>, bukan sekadar memutus FK. Karena berupa SQL native, penghapusan
 *       itu juga melewati Envers sehingga tidak tercatat di riwayat {@code @Audited}.</li>
 *   <li><b>Penamaan kolom.</b> Properti tanpa {@code @Column} ({@code oleh}, {@code olehId},
 *       {@code tanggal_dirubah}, {@code aktif}, {@code skorSampai}) memakai
 *       {@code ais.database.hibernate.MyNamingStrategy} (turunan
 *       {@code DefaultNamingStrategy}) &mdash; nama kolom = nama properti apa adanya, tanpa
 *       konversi ke {@code snake_case}.</li>
 * </ol>
 *
 * <h3>Catatan otorisasi (hasil audit, dicatat agar tidak diverifikasi ulang)</h3>
 * <p><b>Layar utama BERGERBANG BENAR.</b> Berbeda dari beberapa layar lain di keluarga PSB,
 * {@code KelompokPendaftaranPsbAction.doAfterCompose()} memanggil
 * {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)} dan memakai hasilnya untuk
 * tombol Tambah, tombol Ubah/Hapus per baris, checkbox "Aktif"
 * ({@code checkbox.setDisabled(!edit)}), serta tombol unggah massal
 * ({@code upload.setVisible(add.isVisible() && edit && delete)}). Ini <b>verifikasi negatif</b>
 * untuk pola "nol {@code checkPrevilages}" &mdash; entity ini bukan instance pola tersebut.</p>
 * <p>Yang tetap perlu diwaspadai pada layar/entity ini:</p>
 * <ul>
 *   <li><b>Tombol "Cetak" tidak ikut digerbangi.</b> {@code Common.cetakData(..)} dilekatkan
 *       lewat {@code Common.appendKeToolbar(..)} yang hanya menyisipkan komponen ke toolbar
 *       tanpa menyalin {@code isVisible()} dari tombol jangkarnya, sehingga ekspor kolom
 *       {@code id/gelombangPendaftaran/nama/kuota/sekolah/keterangan/aktif} tersedia bagi hak
 *       BACA saja. Isinya data master non-PII, tetapi mencakup seluruh instalasi (lihat butir
 *       berikut).</li>
 *   <li><b>Cakupan tenant fail-open.</b> {@code initCriteria()} tidak menambahkan pembatas
 *       {@code sekolah}/{@code yayasan} bawaan; pembatas baru muncul bila pengguna memilih
 *       nilai pada combo pencarian. Selama combo pada posisi "Semua", daftar (dan ekspor)
 *       memuat kelompok pendaftaran milik <b>semua sekolah dan semua yayasan</b> di
 *       instalasi.</li>
 *   <li><b>Jalur SOP melewati gerbang menu ini sepenuhnya.</b> Karena
 *       {@code KelompokPendaftaranPsbAction} mengimplementasikan {@code ais.ui.util.FormSop},
 *       kelas itu terdaftar otomatis di {@code ConstantValues.treeMapFormSop}
 *       ({@code InitDataHelper}) dan dapat dipilih sebagai {@code AlurSop.formInputan}. Bila
 *       dipilih, {@code DisposisiAlurSopAction}/{@code TampilanAlurSopAction} membuat
 *       instance Action ini lewat {@code Class.forName(..).newInstance()} lalu memanggil
 *       {@code form(..)} dan menyediakan tombol Simpan sendiri &mdash; sehingga
 *       {@code onSave()} berjalan <b>tanpa satu pun {@code checkPrevilages} milik menu
 *       "Kelompok Pendaftaran"</b>, karena {@code doAfterCompose()} (satu-satunya tempat
 *       gerbang dipasang) tidak pernah dijalankan pada jalur itu. Hak yang berlaku hanyalah
 *       hak layar SOP. Ini varian <i>pewarisan hak</i> lewat pendaftaran {@code FormSop},
 *       berbeda dari pewarisan lewat menu induk yang sudah dicatat di batch-batch
 *       sebelumnya.</li>
 *   <li><b>Verifikasi negatif: nol keterlibatan pra-otentikasi.</b> Entity ini tidak dirujuk
 *       sama sekali oleh jalur pendaftaran publik {@code /ppdb} maupun endpoint API/JAX-RS
 *       mana pun; seluruh pemakainya adalah {@code KelompokPendaftaranPsbAction},
 *       {@link CalonSiswa}, {@link PenjurusanSekolah} (hanya di Javadoc), dan
 *       {@code InitData}.</li>
 * </ul>
 *
 * <h3>Bug lain yang sudah terverifikasi pada layar pengelolanya</h3>
 * <ul>
 *   <li><b>Filter "Tampilkan hanya yang aktif" mati.</b> ZUL mendeklarasikan
 *       {@code <checkbox id="searchaktif" ... checked="true">} yang mem-<i>forward</i>
 *       {@code onSearchDefault}, tetapi Action tidak mendeklarasikan field
 *       {@code searchaktif} dan {@code initCriteria()} tidak pernah menyaring kolom
 *       {@code aktif}. Kelompok non-aktif tetap ikut tampil walau kotak tercentang.</li>
 *   <li><b>Mengosongkan "Kuota" lalu menyimpan melempar {@code NullPointerException}.</b>
 *       {@code onSave()} memanggil {@code setKuota(kuota.getValue())}; {@code MyIntbox}
 *       mewarisi {@code Intbox.getValue()} yang mengembalikan {@code Integer} {@code null}
 *       untuk kotak kosong, sedangkan {@link #setKuota(int)} menerima {@code int} primitif
 *       sehingga terjadi unboxing atas {@code null}. Validasi wajib-isi di {@code onSave()}
 *       hanya mencakup gelombang, nama, yayasan, dan sekolah &mdash; kuota tidak
 *       divalidasi.</li>
 * </ul>
 *
 * <p><b>Audit:</b> kelas ber-{@code @Audited} (Envers), sehingga perubahan lewat session
 * Hibernate tercatat pada tabel revisi &mdash; dipakai tombol "Revisi" di grid
 * ({@code RevisiHelper.createNewRevisi(KelompokPendaftaranPsb.class, ..)}). Operasi SQL native
 * (mis. {@code DisposisiSop.hapus()} di atas) tidak tercatat.</p>
 *
 * <p><b>Insert/update dinamis:</b> {@code dynamicInsert}/{@code dynamicUpdate} aktif, sehingga
 * hanya kolom yang benar-benar berubah yang ikut dalam pernyataan SQL.</p>
 *
 * @see GelombangPendaftaranPsb
 * @see CalonSiswa
 * @see PenjurusanSekolah
 * @see DataSop
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kelompok_pendaftaran_psb", schema = "sekolah")
public class KelompokPendaftaranPsb extends DataSop {

	/**
	 * Versi serialisasi Java. Nilainya dibekukan agar instance yang tersimpan di session ZK
	 * atau dikirim antar node tetap dapat dideserialisasi setelah kelas berubah.
	 */
	private static final long serialVersionUID = 5909958736690383653L;
	/** Primary key {@code sekolah.kelompok_pendaftaran_psb.id} (IDENTITY, diisi database). */
	private Long id;
	/**
	 * Nama tampil pengguna yang terakhir menyimpan baris ini (kolom {@code oleh}). Diisi
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}, bukan oleh layar.
	 */
	private String oleh;
	/**
	 * Identitas (id akun) pengguna yang terakhir menyimpan baris ini (kolom {@code olehId}).
	 * Pasangan teknis dari {@link #oleh}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id akun pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return id akun penyimpan terakhir, atau {@code null} bila baris belum pernah melewati
	 *         interceptor audit (mis. hasil migrasi/SQL mentah)
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id akun pengguna penyimpan terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> argumen {@code null} atau berisi spasi saja
	 * <b>diabaikan</b> (early return) sehingga nilai lama dipertahankan. Konsekuensinya kolom
	 * ini tidak dapat dikosongkan kembali lewat setter; ini disengaja agar jejak audit tidak
	 * hilang ketika sebuah alur menyimpan entity tanpa konteks pengguna.</p>
	 *
	 * @param olehId id akun penyimpan; {@code null}/kosong = tidak mengubah apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna penyimpan terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> sama seperti {@link #setOlehId(String)}, argumen
	 * {@code null}/kosong diabaikan sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna penyimpan; {@code null}/kosong = tidak mengubah apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: sebelum setiap {@code UPDATE}, mendelegasikan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang mengisi ulang
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p><b>Efek samping:</b> memutakhirkan tiga kolom audit pada instance ini. Tidak
	 * dipanggil untuk {@code INSERT} (tidak ada {@code @PrePersist}); untuk baris baru nilai
	 * {@link #tanggal_dirubah} berasal dari inisialisasi field
	 * {@code ais.ui.util.WaktuUtil.getDate()}. Jangan panggil manual.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja berada pada baris yang sama dengan
	 * method ini (gaya asli berkas, dipertahankan apa adanya): stempel waktu perubahan
	 * terakhir, diinisialisasi ke waktu server saat instance dibuat.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi &mdash; nilainya diisi
	 * {@link #onUpdate()} lewat interceptor audit. Menimpanya manual akan memalsukan jejak
	 * audit.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, tipe
	 * {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat
	 *         lewat konstruktor Java, karena field diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Gelombang PSB induk (WAJIB). FK {@code gelombang_pendaftaran_id}.
	 */
	private GelombangPendaftaranPsb gelombangPendaftaran;
	/** Sekolah pemilik kelompok ini (kolom tenant utama, FK {@code sekolah_id}). */
	private Sekolah sekolah;
	/**
	 * Yayasan pemilik (FK {@code yayasan_id}). Nilai turunan dari {@link #sekolah} &mdash;
	 * lihat {@link #getYayasan()}.
	 */
	private Yayasan yayasan;
	/**
	 * Penjurusan yang dituju kelompok ini (opsional, FK {@code penjurusan_sekolah_id}).
	 */
	private PenjurusanSekolah penjurusanSekolah;
	/**
	 * Keterangan bebas (kolom {@code deskripsi}). <b>Satu-satunya kolom teks bebas yang
	 * benar-benar dipetakan</b> pada entity ini, namun tidak dipakai layar mana pun &mdash;
	 * lihat butir 2 pada Javadoc kelas.
	 */
	private String deskripsi;
	/** Kuota calon siswa untuk kelompok ini (kolom {@code kuota}, wajib di level kolom). */
	private Integer kuota;
	/**
	 * Batas atas skor untuk kelompok ini (kolom {@code skorSampai}). Belum dikonsumsi logika
	 * mana pun di modul sekolah &mdash; lihat butir 3 pada Javadoc kelas.
	 */
	private Double skorSampai;
	/** Nama kelompok pendaftaran (kolom {@code nama}, wajib). */
	private String nama;
	/**
	 * Penanda aktif (kolom {@code aktif}). Dapat dipaksa {@code false} oleh status alur SOP
	 * &mdash; lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Disposisi SOP yang melampirkan baris ini ke sebuah alur persetujuan (kolom
	 * {@code disposisi_sop}, opsional). Kontrak {@link DataSop}.
	 */
	private DisposisiSop disposisiSop;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate dan dipakai layar saat menekan
	 * "Tambah" ({@code KelompokPendaftaranPsbAction.onAdd()}). Tidak menyetel nilai apa pun;
	 * default yang berlaku berasal dari getter ({@link #getKuota()} &rarr; 30,
	 * {@link #getAktif()} &rarr; {@code true}, {@link #getSkorSampai()} &rarr; {@code 0.0}).
	 */
	public KelompokPendaftaranPsb() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * <p>Kolom {@code id} dideklarasikan {@code insertable = false} karena nilainya
	 * dibangkitkan database ({@code IDENTITY}/sequence). Nilai {@code null} berarti entity
	 * belum pernah disimpan &mdash; dipakai layar untuk membedakan judul dialog "Tambah" vs
	 * "Ubah" dan untuk memutuskan {@code session.load(..)} pada {@code onSave()}.</p>
	 *
	 * @return id baris, atau {@code null} bila entity masih transient
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel primary key. Hanya untuk Hibernate dan kode yang merakit referensi entity
	 * secara manual; menimpa id entity yang sudah tersimpan akan merusak identitas baris.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan gelombang PSB induk dari kelompok ini.
	 *
	 * <p>Relasi WAJIB ({@code nullable = false}) dan {@code LAZY}: pemanggil harus berada di
	 * dalam session Hibernate yang masih terbuka, atau bekerja dengan instance yang sudah
	 * di-<i>resolve</i>. Dipakai layar untuk menampilkan kolom "Nama Gelombang" dan "Tahun
	 * Ajaran" pada grid, dan sebagai kriteria pencarian
	 * ({@code createAlias("gelombangPendaftaran", ..)} lalu filter atas
	 * {@code gelombangPendaftaran.tahunAjaran}).</p>
	 *
	 * @return gelombang PSB induk; secara skema tidak boleh {@code null} untuk baris
	 *         tersimpan, namun bisa {@code null} pada instance baru yang belum diisi
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_id", nullable = false)
	public GelombangPendaftaranPsb getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Menyetel gelombang PSB induk. Dipanggil {@code KelompokPendaftaranPsbAction.onSave()}
	 * dari combo "Gelombang Pendaftaran"; combo itu hanya memuat gelombang dengan
	 * {@code aktif IS NULL OR aktif = true}, sehingga gelombang yang sudah dinonaktifkan tidak
	 * dapat dipilih ulang saat mengubah kelompok lama.
	 *
	 * <p>Tanpa validasi: menyetel {@code null} akan lolos di level Java dan baru gagal saat
	 * {@code INSERT}/{@code UPDATE} karena {@code NOT NULL} di database. Layar mencegahnya
	 * lebih dulu lewat pesan "Gelombang pendaftaran harus diisi".</p>
	 *
	 * @param gelombangPendaftaran gelombang PSB induk
	 */
	public void setGelombangPendaftaran(GelombangPendaftaranPsb gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Mengembalikan sekolah pemilik kelompok ini.
	 *
	 * <p><b>Efek samping:</b> memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} yang me-<i>resolve</i> proxy
	 * lazy menjadi instance nyata (lewat {@code EntityIdentityMap}, inisialisasi Hibernate,
	 * atau query ulang) dan <b>menimpa field {@link #sekolah}</b> dengan hasilnya. Ini
	 * mengganti instance, bukan nilai bisnis, sehingga tidak mengubah data yang tersimpan;
	 * tujuannya mencegah {@code LazyInitializationException} di renderer ZK yang membaca
	 * relasi setelah session ditutup.</p>
	 *
	 * <p>Dipakai renderer grid untuk kolom "Sekolah" dan oleh {@link #getYayasan()} sebagai
	 * sumber turunan.</p>
	 *
	 * @return sekolah pemilik, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik, dengan normalisasi: sekolah {@code null} <i>atau</i> sekolah
	 * yang belum punya id (transient) disimpan sebagai {@code null}.
	 *
	 * <p>Normalisasi ini penting karena relasi ber-{@code CascadeType.PERSIST} &mdash; tanpa
	 * penyaringan tersebut, memilih item combo "Semua" (bernilai objek kosong) bisa memicu
	 * Hibernate menyimpan baris {@code Sekolah} baru yang tidak diinginkan.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik kelompok ini.
	 *
	 * <p><b>Getter yang menulis (write-back) &mdash; baca sebelum memakai.</b> Method ini
	 * tidak sekadar membaca:</p>
	 * <ol>
	 *   <li>memanggil {@link #getSekolah()} (yang sendirinya menimpa field {@link #sekolah}
	 *       dengan hasil {@code check(..)});</li>
	 *   <li>bila sekolah terisi, <b>menimpa field {@link #yayasan}</b> dengan
	 *       {@code sekolah.getYayasan()} &mdash; yayasan yang tersimpan di kolom
	 *       {@code yayasan_id} diabaikan dan digantikan yayasan milik sekolah;</li>
	 *   <li>menjalankan {@code check(..)} atas hasilnya.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi:</b> karena entity ini memakai <i>property access</i>, nilai hasil
	 * timpa itulah yang dianggap Hibernate sebagai nilai properti saat pemeriksaan
	 * <i>dirty</i>. Bila baris lama pernah menyimpan yayasan yang berbeda dari yayasan
	 * sekolahnya (mis. akibat pemindahan sekolah antar yayasan atau perbaikan data lewat SQL
	 * mentah), membuka layar apa pun yang membaca getter ini pada entity yang <i>managed</i>
	 * cukup untuk menulis ulang kolom {@code yayasan_id} secara senyap. Efek praktisnya
	 * konsisten &mdash; yayasan selalu mengikuti sekolah &mdash; tetapi ini bukan sekadar
	 * pembacaan, dan setiap upaya menyetel yayasan yang berbeda dari sekolahnya akan
	 * hilang.</p>
	 *
	 * <p>Bila {@link #sekolah} {@code null}, nilai {@link #yayasan} yang ada dipertahankan
	 * (hanya di-{@code check}).</p>
	 *
	 * @return yayasan pemilik &mdash; yayasan milik {@link #getSekolah()} bila sekolah terisi,
	 *         selain itu nilai kolom {@code yayasan_id}; dapat {@code null}
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
	 * Menyetel yayasan pemilik, dengan normalisasi yang sama seperti
	 * {@link #setSekolah(Sekolah)}: {@code null} atau yayasan transient (tanpa id) disimpan
	 * sebagai {@code null}, agar {@code CascadeType.PERSIST} tidak menciptakan baris
	 * {@code Yayasan} baru.
	 *
	 * <p><b>Perhatikan:</b> nilai yang disetel di sini akan <b>ditimpa</b> oleh
	 * {@link #getYayasan()} pada pembacaan berikutnya selama {@link #getSekolah()} tidak
	 * {@code null}. Untuk mengubah yayasan efektif sebuah kelompok, ubah sekolahnya.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas kelompok ini (kolom {@code deskripsi}).
	 *
	 * <p><b>Tidak dipakai layar mana pun.</b> Layar "Kelompok Pendaftaran" mengisi dan
	 * menampilkan {@code keterangan} milik {@code GeneralValueObject} yang tidak dipetakan
	 * Hibernate, bukan properti ini &mdash; lihat butir 2 pada Javadoc kelas. Selama kondisi
	 * itu belum diperbaiki, kolom {@code deskripsi} praktis selalu {@code null} pada instalasi
	 * berjalan.</p>
	 *
	 * @return keterangan kelompok, atau {@code null} bila belum diisi (kondisi normal saat
	 *         ini)
	 */
	@Column(name = "deskripsi")
	public String getDeskripsi() {
		return this.deskripsi;
	}

	/**
	 * Menyetel keterangan bebas kelompok ini. Tanpa validasi maupun normalisasi; {@code null}
	 * diterima apa adanya. Belum ada pemanggil di codebase.
	 *
	 * @param deskripsi keterangan baru
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengembalikan kuota calon siswa untuk kelompok ini, dengan <b>default 30</b> bila kolom
	 * {@code kuota} bernilai {@code null}.
	 *
	 * <p><b>Non-obvious:</b> tipe kembaliannya {@code int} primitif walau field-nya
	 * {@code Integer}, jadi coalescing ke 30 bukan sekadar kenyamanan tampilan &mdash; karena
	 * Hibernate memakai property access, angka 30 itulah yang tertulis ke kolom saat entity
	 * disimpan berikutnya. Baris hasil migrasi/SQL mentah yang kuotanya sengaja dikosongkan
	 * akan diam-diam menjadi 30 begitu tersentuh alur simpan.</p>
	 *
	 * <p>Dipakai renderer grid (kolom "Kuota") dan sebagai nilai awal {@code MyIntbox} pada
	 * dialog Tambah/Ubah. Angka ini <b>belum menjadi pembatas apa pun</b> &mdash; tidak ada
	 * kode yang membandingkan jumlah {@link CalonSiswa} terhadap kuota (lihat butir 3 pada
	 * Javadoc kelas).</p>
	 *
	 * @return kuota kelompok; 30 bila kolom belum diisi
	 */
	@Column(name = "kuota", nullable = false)
	public int getKuota() {
		return this.kuota == null ? 30 : kuota;
	}

	/**
	 * Menyetel kuota kelompok.
	 *
	 * <p><b>Perhatikan tipe primitif.</b> Parameter {@code int} membuat pemanggil wajib
	 * menyediakan angka. {@code KelompokPendaftaranPsbAction.onSave()} memanggilnya dengan
	 * {@code kuota.getValue()} yang bertipe {@code Integer} dan bernilai {@code null} untuk
	 * kotak isian yang dikosongkan &mdash; sehingga mengosongkan field "Kuota" lalu menekan
	 * Simpan melempar {@code NullPointerException} sebelum data tersimpan. Tidak ada validasi
	 * wajib-isi untuk kuota di layar.</p>
	 *
	 * @param kuota kuota calon siswa untuk kelompok ini
	 */
	public void setKuota(int kuota) {
		this.kuota = kuota;
	}

	/**
	 * Mengembalikan nama kelompok pendaftaran (kolom {@code nama}, {@code NOT NULL}).
	 *
	 * <p>Dipakai sebagai label tombol Revisi pada grid
	 * ({@code RevisiHelper.createNewRevisi(.., getNama())}), kunci pengurutan bawaan daftar
	 * ({@code Order.asc("nama")}), dan target filter pencarian ({@code ilike} dengan
	 * {@code MatchMode.ANYWHERE}).</p>
	 *
	 * @return nama kelompok; {@code null} hanya pada instance baru yang belum diisi
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel nama kelompok pendaftaran. Tanpa validasi maupun trim; layar sudah menolak
	 * string kosong lebih dulu (pesan validasinya sendiri masih menyebut "Nama Jenis Sekolah",
	 * sisa salin-tempel dari layar lain).
	 *
	 * @param nama nama kelompok
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan status aktif kelompok ini, <b>dikombinasikan dengan status alur SOP</b>.
	 *
	 * <p>Urutan yang dijalankan:</p>
	 * <ol>
	 *   <li>membaca {@link #getDisposisiSop()} (yang sendirinya me-<i>resolve</i> proxy lazy
	 *       dan menimpa field {@link #disposisiSop});</li>
	 *   <li>bila disposisi ada dan {@code disposisiSop.getAktif()} bernilai {@code false},
	 *       <b>menimpa field {@link #aktif} menjadi {@code false}</b>;</li>
	 *   <li>bila alur SOP sudah berhenti di langkah penolakan
	 *       ({@code disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()}),
	 *       juga <b>menimpa {@link #aktif} menjadi {@code false}</b>;</li>
	 *   <li>mengembalikan {@code true} bila field masih {@code null} (default "aktif").</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang harus disadari:</b> penimpaan pada langkah 2 dan 3 mengubah
	 * field instance, bukan sekadar nilai kembalian. Dengan property access, sekali entity
	 * yang <i>managed</i> dibaca dalam kondisi SOP tidak aktif/ditolak, kolom {@code aktif}
	 * akan ikut ter-{@code UPDATE} menjadi {@code false} pada flush berikutnya &mdash; dan
	 * nilai itu <b>tidak dipulihkan otomatis</b> bila alur SOP kemudian diperbaiki atau
	 * dijalankan ulang. Pemulihan harus dilakukan manual lewat checkbox "Aktif" di grid.</p>
	 *
	 * <p><b>Risiko {@code NullPointerException}:</b> langkah 2 memanggil
	 * {@code disposisiSop.getAktif()} tanpa penjagaan null; bila implementasi di sisi
	 * {@link DisposisiSop} mengembalikan {@code Boolean} {@code null}, unboxing pada operator
	 * {@code !} akan melempar NPE saat baris dirender.</p>
	 *
	 * <p>Dipakai renderer grid (checkbox "Aktif", dinonaktifkan bila pengguna tidak punya hak
	 * {@code UPDATE}) dan termasuk dalam kolom ekspor/unggah massal.</p>
	 *
	 * @return {@code true} bila kelompok dianggap aktif; {@code false} bila dinonaktifkan
	 *         manual atau dipaksa nonaktif oleh status alur SOP. Tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif kelompok.
	 *
	 * <p>Dipanggil dari event {@code onCheck} checkbox "Aktif" di grid, yang langsung
	 * menyusulkan {@code Common.refreshSaveOrUpdate(..)} &mdash; jadi perubahan tersimpan
	 * seketika tanpa tombol Simpan. Nilai {@code null} diterima dan akan terbaca sebagai
	 * {@code true} lewat {@link #getAktif()}.</p>
	 *
	 * <p><b>Perhatikan:</b> nilai {@code true} yang disetel di sini akan kembali dipaksa
	 * {@code false} oleh {@link #getAktif()} selama disposisi SOP terkait masih tidak
	 * aktif/ditolak.</p>
	 *
	 * @param aktif status aktif baru; {@code null} berarti "belum ditentukan" (dibaca sebagai
	 *              aktif)
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan batas atas skor untuk kelompok ini, dengan default {@code 0.0} bila kolom
	 * {@code skorSampai} belum diisi.
	 *
	 * <p><b>Belum terpakai.</b> Tidak ada layar, Action, atau API di modul sekolah yang
	 * membaca atau menulis properti ini &mdash; dialog Tambah/Ubah bahkan tidak memuat field
	 * untuknya. Padanan PMB-nya, {@code KelompokCalonMahasiswa.getSkorSampai()}, dipakai
	 * {@code KelompokCalonMahasiswaDetailAction} untuk menempatkan calon mahasiswa ke kelompok
	 * berdasarkan rentang skor; mekanisme sepadan untuk PSB belum ditulis.</p>
	 *
	 * <p><b>Non-obvious:</b> coalescing ke {@code 0.0} berarti &mdash; sekali mesin
	 * penempatan berbasis skor benar-benar dibuat &mdash; kelompok yang batasnya belum diisi
	 * akan berperilaku sebagai "batas atas nol", bukan "tanpa batas". Perlu diperhitungkan
	 * sebelum properti ini diaktifkan.</p>
	 *
	 * @return batas atas skor; {@code 0.0} bila belum diisi. Tidak pernah {@code null}
	 */
	public Double getSkorSampai() {
		return skorSampai == null ? 0.0 : skorSampai;
	}

	/**
	 * Menyetel batas atas skor untuk kelompok ini. Tanpa validasi; belum ada pemanggil di
	 * codebase.
	 *
	 * @param skorSampai batas atas skor
	 */
	public void setSkorSampai(Double skorSampai) {
		this.skorSampai = skorSampai;
	}

	/**
	 * Mengembalikan penjurusan sekolah yang dituju kelompok ini (opsional).
	 *
	 * <p>Berbeda dari relasi lain di kelas ini, relasi ini <b>tidak</b> {@code LAZY}: ia
	 * memakai fetch default {@code @ManyToOne} ({@code EAGER}) dengan
	 * {@code @Fetch(FetchMode.SELECT)}, sehingga setiap kelompok memicu satu SELECT tambahan
	 * ke {@code penjurusan_sekolah} saat dimuat &mdash; pola N+1 pada daftar berhalaman.</p>
	 *
	 * <p>Di layar, combo "Penjurusan" hanya muncul bila sekolah terpilih menyalakan
	 * {@code Sekolah.getPenjurusanBolehDipilihSaatPsb()}, dan hanya memuat penjurusan yang
	 * {@code aktif} sekaligus {@code tampilkanDiPpdb}; bila sekolah punya penjurusan, sebuah
	 * item "Semua" bernilai {@code null} ikut ditambahkan. Renderer grid menampilkan namanya
	 * sebagai suffix dalam kurung pada kolom "Sekolah".</p>
	 *
	 * @return penjurusan yang dituju, atau {@code null} bila kelompok berlaku untuk semua
	 *         penjurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penjurusan_sekolah_id", nullable = true)
	public PenjurusanSekolah getPenjurusanSekolah() {
		return penjurusanSekolah;
	}

	/**
	 * Menyetel penjurusan yang dituju kelompok ini.
	 *
	 * <p>Tanpa normalisasi transient seperti pada {@link #setSekolah(Sekolah)}/
	 * {@link #setYayasan(Yayasan)}; karena relasi ini ber-{@code CascadeType.PERSIST},
	 * menyerahkan objek {@link PenjurusanSekolah} yang belum tersimpan akan membuat Hibernate
	 * ikut menyimpannya. Layar aman dari hal itu karena selalu menyerahkan nilai item combo
	 * (entity tersimpan) atau {@code null}.</p>
	 *
	 * @param penjurusanSekolah penjurusan yang dituju; {@code null} = semua penjurusan
	 */
	public void setPenjurusanSekolah(PenjurusanSekolah penjurusanSekolah) {
		this.penjurusanSekolah = penjurusanSekolah;
	}

	/**
	 * Mengembalikan disposisi SOP yang melampirkan baris ini ke sebuah alur persetujuan.
	 * Implementasi kontrak abstrak {@link DataSop#getDisposisiSop()}.
	 *
	 * <p><b>Efek samping:</b> memanggil
	 * {@link ais.database.model.GeneralValueObject#check(Object)} dan <b>menimpa field</b>
	 * {@link #disposisiSop} dengan hasil resolusi proxy &mdash; sama seperti
	 * {@link #getSekolah()}, ini penggantian instance, bukan perubahan nilai bisnis.</p>
	 *
	 * <p>Dipakai renderer grid untuk menampilkan tautan "SOP &lt;keterangan&gt; (&lt;nama
	 * SOP&gt;)" yang membuka {@code TampilanAlurSopAction}, dan dipakai {@link #getAktif()}
	 * untuk memaksa kelompok menjadi nonaktif saat alurnya belum/tidak disetujui.</p>
	 *
	 * <p><b>Ingat:</b> {@code DisposisiSop.hapus()} menghapus baris
	 * {@code sekolah.kelompok_pendaftaran_psb} yang menunjuk disposisi tersebut lewat SQL
	 * native &mdash; lihat butir 6 pada Javadoc kelas.</p>
	 *
	 * @return disposisi SOP terkait, atau {@code null} bila kelompok ini tidak melalui alur
	 *         SOP
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP terkait. Implementasi kontrak abstrak
	 * {@link DataSop#setDisposisiSop(DisposisiSop)}.
	 *
	 * <p><b>Perilaku non-obvious &mdash; kaitan SOP tidak bisa dilepas.</b> Method melakukan
	 * <i>early return</i> untuk argumen {@code null} maupun disposisi transient (tanpa id),
	 * sehingga nilai lama selalu dipertahankan. Karena itu pemanggilan
	 * {@code setDisposisiSop(null)} &mdash; termasuk yang dilakukan
	 * {@code KelompokPendaftaranPsbAction.onSave()} pada alur Tambah/Ubah biasa, di mana
	 * field {@code disposisiSop} milik Action masih {@code null} &mdash; tidak berpengaruh
	 * apa pun. Untuk memutus kaitan SOP diperlukan intervensi di luar setter ini.</p>
	 *
	 * <p>Ekspresi ternary di badan method adalah sisa kode yang tidak pernah tercapai: pada
	 * titik itu {@code disposisiSop} dijamin bukan {@code null} dan pasti punya id, sehingga
	 * hasilnya selalu argumen yang diberikan. Bentuknya dipertahankan apa adanya (dokumentasi
	 * ini tidak mengubah logika).</p>
	 *
	 * @param disposisiSop disposisi SOP baru; {@code null} atau tanpa id = tidak mengubah
	 *                     apa pun
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}
	
	
}
