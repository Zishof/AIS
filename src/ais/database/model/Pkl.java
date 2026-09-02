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

import ais.common.Common;
import ais.common.ConstantValues;

/**
 * Entity <b>program PKL</b> (Praktik Kerja Lapangan / kerja praktek) &mdash; satu baris tabel
 * {@code public.pkl} mewakili <b>satu gelaran/angkatan PKL</b>: kapan periodenya berjalan, siapa
 * yang boleh ikut (fakultas / jurusan / program studi), ambang SKS dan IPK minimalnya, apakah
 * peserta harus lunas biaya dulu, dan siapa yang boleh mengubah agenda kegiatannya.
 *
 * <p><b>Yang BUKAN tanggung jawab kelas ini.</b> Kelas ini adalah lapisan <i>program</i>, bukan
 * lapisan <i>penempatan</i>. Tempat magang (alamat, lokasi geografis, instansi mitra), kuota
 * peserta, daftar sampai sepuluh dosen pembimbing, sertifikat, dan flag {@code aktif} semuanya
 * ada di {@link ais.database.model.pkl.KelompokPkl}, bukan di sini. Karena itu kelas ini
 * <b>tidak punya field {@code aktif}, {@code kuota}, maupun {@code dosen pembimbing}</b> &mdash;
 * jangan mencarinya di sini dan jangan menambahkannya di sini.</p>
 *
 * <h2>Posisi dalam alur PKL</h2>
 * <p>Seperti hampir semua entity repo ini, relasi ke {@code Pkl} bersifat <b>searah dari sisi
 * anak</b>: kelas ini tidak mendeklarasikan satu pun koleksi {@code @OneToMany}, sehingga untuk
 * mengambil "semua pendaftar / semua kelompok / semua syarat PKL X" kode selalu menjalankan
 * Criteria atau HQL sendiri dengan {@code Restrictions.eq("pkl", pkl)}.</p>
 * <ol>
 * <li><b>Pembuatan program</b> &mdash; layar admin {@code ais.action.master.PklAction} mengisi
 * seluruh field kelas ini (periode, sasaran, ambang syarat, biaya, izin agenda).</li>
 * <li><b>Definisi syarat &amp; komponen nilai</b> &mdash; katalog butir syarat
 * {@link ais.database.model.pkl.PersyaratanPkl} dikaitkan ke program lewat
 * {@link ais.database.model.pkl.PklPunyaPersyaratan}; katalog komponen penilaian
 * {@link ais.database.model.pkl.KomponenPenilaianPkl} dikaitkan lewat
 * {@link ais.database.model.pkl.PklPunyaKomponenPenilaianPkl}.</li>
 * <li><b>Pendaftaran mahasiswa</b> &mdash; layar
 * {@code ais.action.master.pkl.PklUntukMahasiswaAction} menyaring program yang boleh dilihat
 * seorang mahasiswa (lihat "Pola filter opsional" di bawah), memeriksa tagihan, lalu membuat
 * {@link ais.database.model.pkl.MahasiswaDaftarPkl} dengan kolom {@code terima} bernilai
 * {@code BELUM_DIPROSES} / {@code DITERIMA} / {@code DITOLAK}. Jawaban per butir syarat disimpan
 * di {@link ais.database.model.pkl.MahasiswaPklPersyaratan}.</li>
 * <li><b>Seleksi</b> &mdash; {@code ais.action.master.pkl.SeleksiPenerimaPklAction} dan
 * {@code ais.action.master.helper.PendaftarPklHelper} mengubah kolom {@code terima} tersebut.</li>
 * <li><b>Penempatan kelompok</b> &mdash; {@link ais.database.model.pkl.KelompokPkl} (menunjuk balik
 * ke {@code Pkl}) menampung tempat magang dan pembimbing; keanggotaannya di
 * {@link MahasiswaDapatKelompokPkl} (dan {@link SiswaDapatKelompokPkl} untuk peserta jalur
 * sekolah).</li>
 * <li><b>Pelaksanaan &amp; agenda</b> &mdash; {@code ais.action.master.helper.AktifitasPklHelper}
 * membangun agenda {@link Pertemuan} per kelompok; hak sunting agendanya ditentukan
 * {@link #getMahasiswaBolehMerubahAgenda()} / {@link #getDosenBolehMerubahAgenda()} di kelas
 * ini.</li>
 * <li><b>Penilaian</b> &mdash; {@code ais.action.master.helper.PenilaianPklHelper}.</li>
 * <li><b>Jalur samping</b> &mdash; {@link MahasiswaDapatPkl} mengaitkan mahasiswa langsung ke
 * program tanpa lewat kelompok (dipakai {@code PklHelper} dan
 * {@code AmbilDataMahasiswaPklHelper}), sementara {@link PengecualianPklMahasiswa} membebaskan
 * mahasiswa tertentu dari pemeriksaan syarat akademis.</li>
 * <li><b>Ekspor Feeder/PDDikti</b> &mdash; {@code EksporAktifitasPklFeeder},
 * {@code EksporPesertaDosenPklFeeder}, dan {@code EksporPesertaMahasiswaPklFeeder} memakai
 * {@link #getJenisAktfitasMahasiswa()} sebagai kode jenis aktivitas mahasiswa.</li>
 * </ol>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 * <p>Kelas induk <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}, melainkan POJO
 * abstrak biasa; Hibernate <b>tidak memetakan properti milik induk</b>. Karena itu deklarasi ulang
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini
 * <b>bukan duplikasi yang keliru, melainkan keharusan teknis</b> &mdash; tanpa deklarasi ulang,
 * kolom-kolom tersebut tidak akan pernah dipetakan. Pola yang sama muncul di hampir semua entity
 * repo ini. Manfaat yang benar-benar diwarisi dari induk adalah kumpulan utilitas statis, terutama
 * {@link GeneralValueObject#check(Object)} untuk resolusi proxy lazy yang dipakai ketiga getter
 * relasi di kelas ini.</p>
 *
 * <h2>Pemetaan Hibernate</h2>
 * <p>{@code @Entity} + {@code @Table(schema = "public", name = "pkl")},
 * {@code dynamicInsert}/{@code dynamicUpdate} aktif (hanya kolom yang benar-benar berubah ikut
 * dalam {@code INSERT}/{@code UPDATE}), dan {@code @Audited} sehingga setiap perubahan direkam
 * Hibernate Envers ke tabel bayangan {@code pkl_AUD}.</p>
 * <p>Pemetaan memakai <b>property access</b> (anotasi {@code @Id} menempel pada
 * {@link #getId()}), sehingga <b>setiap pasangan getter/setter yang tidak dianotasi
 * {@code @Transient} tetap dipetakan</b> &mdash; dan di kelas ini <b>tidak ada satu pun
 * {@code @Transient}</b>. Karena {@code ais.database.hibernate.MyNamingStrategy} adalah turunan
 * {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi ke
 * {@code under_score}), properti tanpa {@code @Column} jatuh ke kolom bernama persis seperti
 * propertinya: {@code nama}, {@code nama_kelompok}, {@code keterangan}, {@code program},
 * {@code semester}, {@code tahunAkademik}, {@code kodeItemBiaya}, {@code nimMhsTanpaBiaya},
 * {@code minimalSksBolehIkutPkl}, {@code minimalIpkBolehIkutPkl},
 * {@code minimalSksBolehIkutPkl2}, {@code minimalIpkBolehIkutPkl2}, {@code aktifkanSyaratLain},
 * {@code harusBayar}, {@code mahasiswaBolehMerubahAgenda}, {@code dosenBolehMerubahAgenda},
 * {@code tanggal_dirubah}. Konfigurasi {@code hbm2ddl.auto=update} di {@code hibernate.cfg.xml}
 * berarti kolom-kolom itu memang benar-benar ada di tabel (dibuat otomatis saat startup), termasuk
 * kolom {@code nama} yang isinya selalu tertimpa duplikat {@code nama_kelompok} &mdash; lihat
 * {@link #getNama()}.</p>
 *
 * <h2>Pengelompokan method</h2>
 * <ol>
 * <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 * {@link #getOlehId()}/{@link #setOlehId(String)},
 * {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan hook {@code @PreUpdate}
 * {@link #onUpdate()}.</li>
 * <li><b>Identitas &amp; deskripsi</b> &mdash; {@link #getId()},
 * {@link #getNama_kelompok()} (nama program yang sebenarnya), {@link #getNama()} (turunan),
 * {@link #getKeterangan()}, {@link #toString()}.</li>
 * <li><b>Periode</b> &mdash; {@link #getTanggal_mulai()}, {@link #getTanggal_selesai()}, beserta
 * turunannya {@link #getSemester()} dan {@link #getTahunAkademik()}.</li>
 * <li><b>Penargetan peserta (filter opsional)</b> &mdash; {@link #getFakultas()},
 * {@link #getJurusan()}, {@link #getProgram()}. Ketiganya bersifat "{@code null} berarti
 * <i>tidak dibatasi</i>".</li>
 * <li><b>Ambang syarat akademis</b> &mdash; {@link #getMinimalSksBolehIkutPkl()} /
 * {@link #getMinimalIpkBolehIkutPkl()} (syarat utama) dan {@link #getAktifkanSyaratLain()} /
 * {@link #getMinimalSksBolehIkutPkl2()} / {@link #getMinimalIpkBolehIkutPkl2()} (syarat
 * alternatif). Dievaluasi di {@code Common.checkSyaratPkl(Mahasiswa, Pkl)}.</li>
 * <li><b>Syarat keuangan</b> &mdash; {@link #getHarusBayar()}, {@link #getKodeItemBiaya()},
 * {@link #getNimMhsTanpaBiaya()} (daftar putih pembebasan).</li>
 * <li><b>Integrasi &amp; izin</b> &mdash; {@link #getJenisAktfitasMahasiswa()} (kode Feeder),
 * {@link #getMahasiswaBolehMerubahAgenda()}, {@link #getDosenBolehMerubahAgenda()}.</li>
 * </ol>
 *
 * <h2>Pola filter opsional (fakultas / jurusan / program)</h2>
 * <p>{@code PklUntukMahasiswaAction.initCriteria()} menyusun kriteria berbentuk
 * {@code Restrictions.or(Restrictions.isNull("x"), Restrictions.eq("x", nilaiMahasiswa))} untuk
 * ketiga properti tersebut. Artinya kolom yang {@code NULL} berarti "berlaku untuk semua", dan
 * kolom yang terisi mempersempit ke satu nilai saja. Konsekuensi penting: nilai <b>string kosong
 * bukan {@code NULL}</b> dan akan menyaring habis seluruh mahasiswa &mdash; itulah sebabnya
 * {@link #getProgram()} sengaja memetakan string kosong menjadi {@code null} (lihat method
 * tersebut).</p>
 *
 * <h2>Pola "getter yang menulis balik" (penting)</h2>
 * <p>Seperti banyak entity lain di repo ini, sejumlah getter di sini <b>bukan getter polos</b>:
 * mereka mengubah state object saat dibaca. Karena entity yang dibaca dari session Hibernate
 * bersifat <i>managed</i>, perubahan itu ikut ter-{@code UPDATE} ke database pada flush berikutnya
 * <b>meskipun tidak ada layar yang secara sadar menyimpan apa pun</b>. Selain itu, karena kelas ini
 * memakai property access, <b>nilai yang dilihat Hibernate saat dirty-check adalah nilai kembalian
 * getter</b> &mdash; jadi getter yang "hanya menormalkan kembalian" tanpa menyentuh field pun tetap
 * bisa mengubah isi kolom saat flush.</p>
 * <ul>
 * <li><b>Mengisi field hanya saat {@code null}</b> (nilai default lalu ikut tersimpan):
 * {@link #getMinimalSksBolehIkutPkl()} ({@code 100}), {@link #getMinimalIpkBolehIkutPkl()}
 * ({@code 3.0}), {@link #getMinimalSksBolehIkutPkl2()} ({@code 0}),
 * {@link #getMinimalIpkBolehIkutPkl2()} ({@code 0.0}), {@link #getAktifkanSyaratLain()}
 * ({@code false}), {@link #getHarusBayar()} ({@code false}), {@link #getKodeItemBiaya()}
 * (string kosong), {@link #getSemester()}, {@link #getTahunAkademik()}.</li>
 * <li><b>Selalu menimpa field</b>, bukan hanya saat {@code null}: {@link #getNama()} (disalin dari
 * {@code nama_kelompok}) dan {@link #getNimMhsTanpaBiaya()} (dinormalkan menjadi bentuk
 * {@code ,nim1,nim2,}).</li>
 * <li><b>Menulis balik hasil resolusi proxy</b> ({@code x = check(x); return x;}):
 * {@link #getJurusan()}, {@link #getFakultas()}, {@link #getJenisAktfitasMahasiswa()}.</li>
 * <li><b>Tidak menulis balik</b>, hanya menormalkan kembalian: {@link #getTanggal_mulai()}
 * (mengembalikan tanggal hari ini bila field {@code null}), {@link #getProgram()} (trim, kosong
 * jadi {@code null}), dan cabang fallback {@link #getJenisAktfitasMahasiswa()} ke
 * {@link ConstantValues#PKL}. Perhatikan asimetri {@link #getTanggal_mulai()} (punya fallback)
 * versus {@link #getTanggal_selesai()} (tidak punya) &mdash; keduanya <b>dibiarkan apa adanya</b>
 * karena mengubah perilakunya berisiko pada data lama.</li>
 * </ul>
 * <p><b>Sesi Hibernate:</b> tidak ada satu pun method di kelas ini yang membuka atau menutup
 * {@code Session} secara langsung (kelas ini bahkan tidak meng-import {@code HibernateUtil}).
 * Satu-satunya akses database implisit terjadi di dalam {@link GeneralValueObject#check(Object)}
 * yang dipakai ketiga getter relasi; pembukaan dan penutupan sesi penyelamat di sana sudah
 * ditangani kelas induk.</p>
 *
 * <h2>Kuirk yang perlu diketahui sebelum menyunting</h2>
 * <ul>
 * <li><b>Nama program disimpan di {@code nama_kelompok}, bukan {@code nama}.</b> Penamaan
 * {@code nama_kelompok} adalah sisa cetakan {@code hbm2java} yang dipakai bersama
 * {@link ais.database.model.pkl.KelompokPkl}; di kelas ini isinya adalah nama <i>program PKL</i>,
 * bukan nama kelompok. Kolom {@code nama} hanyalah salinan bayangan yang ditulis ulang setiap kali
 * {@link #getNama()} dipanggil.</li>
 * <li><b>Default syarat utama ketat, default syarat alternatif longgar.</b> Object baru mendapat
 * nilai awal field {@code minimalSksBolehIkutPkl2 = 110} dan {@code minimalIpkBolehIkutPkl2 = 2.0}
 * dari inisialisasi field, tetapi baris <i>lama</i> yang kolomnya masih {@code NULL} akan
 * di-default oleh getter menjadi <b>{@code 0} SKS dan IPK {@code 0.0}</b>. Kombinasi
 * {@code aktifkanSyaratLain = true} pada baris warisan semacam itu membuat syarat alternatif
 * <b>selalu terpenuhi siapa pun pendaftarnya</b> &mdash; efektif mematikan seluruh penyaringan SKS
 * dan IPK. Lihat {@link #getMinimalSksBolehIkutPkl2()}.</li>
 * <li><b>{@link #toString()} memakai field mentah</b> {@code nama_kelompok} (bukan getter), jadi
 * bisa mengembalikan {@code null} untuk program yang belum diberi nama. Beberapa komponen ZK
 * memanggil {@code toString()} secara implisit.</li>
 * <li><b>Fallback {@link ConstantValues#PKL} bisa {@code null}.</b> Konstanta itu hanya terisi bila
 * sinkronisasi Neo Feeder ({@code ais.common.InitDataHelper}) pernah berjalan dan menemukan jenis
 * aktivitas bernama "Kerja praktek/PKL". Pada instalasi yang belum pernah sinkron, fallback ini
 * tetap {@code null}.</li>
 * </ul>
 *
 * @see ais.database.model.pkl.KelompokPkl
 * @see ais.database.model.pkl.MahasiswaDaftarPkl
 * @see MahasiswaDapatPkl
 * @see PengecualianPklMahasiswa
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pkl")

public class Pkl extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java. Nilai ini disalin apa adanya dari cetakan
	 * {@code hbm2java} dan sengaja dipertahankan agar object {@code Pkl} yang pernah
	 * diserialisasi (mis. ke dalam session ZK yang dipulihkan) tetap bisa dibaca.
	 */
	private static final long serialVersionUID = 2413821571548439808L;
	/** Kunci utama baris, dibangkitkan database. Lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Identitas (login/NIP/NIM) pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor} lewat hook {@link #onUpdate()}.</p>
	 *
	 * @return identitas pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string kosong <b>diabaikan diam-diam</b> (method
	 * langsung {@code return} tanpa menyentuh field), supaya jejak audit lama tidak terhapus oleh
	 * penyimpanan yang kebetulan tidak membawa konteks pengguna.</p>
	 *
	 * @param olehId identitas pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau string
	 * kosong <b>diabaikan diam-diam</b> supaya jejak audit lama tetap utuh.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir; {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum {@code UPDATE} baris ini
	 * dieksekusi.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@code oleh}/{@code olehId} dari konteks pengguna aktif dan memperbarui
	 * {@link #getTanggal_dirubah()}. Karena hook ini hanya terikat pada {@code @PreUpdate} (bukan
	 * {@code @PrePersist}), baris yang baru pertama kali di-{@code INSERT} mengandalkan nilai awal
	 * field dan setter yang dipanggil layar penyimpan.</p>
	 *
	 * <p>Jangan panggil manual dari kode aplikasi.</p>
	 *
	 * <p>Field {@code tanggal_dirubah} sengaja diinisialisasi ke waktu "sekarang" versi kampus
	 * ({@code WaktuUtil.getDate()}, sudah dikoreksi zona waktu WIB/WITA/WIT) agar baris baru pun
	 * punya stempel waktu yang masuk akal sebelum hook ini pernah berjalan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis lewat {@link #onUpdate()}; panggilan manual hanya dipakai importir
	 * yang ingin mempertahankan waktu asal data.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} (tanggal + jam), berbeda dengan
	 * {@link #getTanggal_mulai()}/{@link #getTanggal_selesai()} yang hanya {@code DATE}.</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang baru dibuat
	 *         karena field-nya diinisialisasi ke waktu sekarang
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks program PKL, dipakai komponen ZK (combobox, banbox, label) yang
	 * menampilkan object ini apa adanya.
	 *
	 * <p><b>Perhatian:</b> method ini membaca <b>field mentah</b> {@code nama_kelompok}, bukan
	 * getter-nya, sehingga bisa mengembalikan {@code null} untuk program yang belum diberi nama.
	 * Pemanggil yang menyusun string ({@code "..." + pkl}) akan menampilkan teks
	 * {@code "null"}.</p>
	 *
	 * @return nama program PKL; {@code null} bila belum diisi
	 */
	public String toString() {
		return nama_kelompok;
	}

	/**
	 * Salinan bayangan {@code nama_kelompok}. Diisi ulang setiap kali {@link #getNama()}
	 * dipanggil; jangan diperlakukan sebagai sumber kebenaran.
	 */
	private String nama;

	/**
	 * Mengembalikan nama program PKL lewat properti generik {@code nama}.
	 *
	 * <p><b>Tujuan.</b> Menyediakan nama dengan penamaan properti yang seragam dengan entity lain,
	 * sehingga helper generik (mis. {@code AmbilDataSyaratPklHelper} yang merangkai label
	 * {@code pkl.getNama() + " --> " + persyaratanPkl.getNama()}, atau
	 * {@code CalendarPerkuliahanMingguIniComposer}) tidak perlu tahu bahwa nama sebenarnya
	 * tersimpan di {@code nama_kelompok}.</p>
	 *
	 * <p><b>Efek samping (penting).</b> Method ini <b>selalu menimpa</b> field {@code nama} dengan
	 * hasil {@link #getNama_kelompok()} &mdash; bukan hanya saat {@code nama} masih {@code null}.
	 * Karena properti {@code nama} juga dipetakan Hibernate (tidak ada {@code @Transient} di kelas
	 * ini) dan kolomnya benar-benar ada berkat {@code hbm2ddl.auto=update}, sekadar
	 * <i>membaca</i> nama program pada entity yang <i>managed</i> bisa memicu {@code UPDATE} kolom
	 * {@code nama} pada flush berikutnya. Nilai apa pun yang pernah ditulis lewat
	 * {@link #setNama(String)} karena itu bersifat sementara.</p>
	 *
	 * @return nama program PKL (isi {@code nama_kelompok}); {@code null} bila belum diisi
	 * @see #getNama_kelompok()
	 */
	public String getNama() {
		nama = getNama_kelompok();
		return nama;
	}

	/**
	 * Mengisi field bayangan {@code nama}.
	 *
	 * <p><b>Perhatian:</b> nilai yang diisi di sini <b>tidak bertahan</b> &mdash; panggilan
	 * {@link #getNama()} berikutnya akan langsung menimpanya dengan {@code nama_kelompok}. Untuk
	 * mengubah nama program yang sesungguhnya gunakan {@link #setNama_kelompok(String)}.</p>
	 *
	 * @param nama nama bayangan; akan tertimpa pada pembacaan berikutnya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** Nama program PKL yang sesungguhnya. Lihat {@link #getNama_kelompok()}. */
	private String nama_kelompok;
	/** Tanggal mulai periode PKL; diinisialisasi ke hari ini. Lihat {@link #getTanggal_mulai()}. */
	private Date tanggal_mulai = ais.ui.util.WaktuUtil.getDate();
	/** Tanggal selesai periode PKL; diinisialisasi ke hari ini. Lihat {@link #getTanggal_selesai()}. */
	private Date tanggal_selesai = ais.ui.util.WaktuUtil.getDate();
	/** Catatan bebas tentang program. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Pembatas fakultas peserta; {@code null} berarti semua fakultas. Lihat {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Pembatas jurusan peserta; {@code null} berarti semua jurusan. Lihat {@link #getJurusan()}. */
	private Jurusan jurusan;
	/** Pembatas program studi (D3/S1/...); {@code null}/kosong berarti semua. Lihat {@link #getProgram()}. */
	private String program;
	/** Ambang SKS lulus minimal (syarat utama). Lihat {@link #getMinimalSksBolehIkutPkl()}. */
	private Integer minimalSksBolehIkutPkl;
	/** Ambang IPK minimal (syarat utama). Lihat {@link #getMinimalIpkBolehIkutPkl()}. */
	private Double minimalIpkBolehIkutPkl;

	/** Saklar syarat alternatif. Lihat {@link #getAktifkanSyaratLain()}. */
	private Boolean aktifkanSyaratLain;
	/**
	 * Ambang SKS minimal jalur alternatif; nilai awal {@code 110} hanya berlaku untuk object baru,
	 * baris lama yang {@code NULL} akan di-default {@code 0} oleh getter. Lihat
	 * {@link #getMinimalSksBolehIkutPkl2()}.
	 */
	private Integer minimalSksBolehIkutPkl2 = 110;
	/**
	 * Ambang IPK minimal jalur alternatif; nilai awal {@code 2.0} hanya berlaku untuk object baru,
	 * baris lama yang {@code NULL} akan di-default {@code 0.0} oleh getter. Lihat
	 * {@link #getMinimalIpkBolehIkutPkl2()}.
	 */
	private Double minimalIpkBolehIkutPkl2 = 2.0;

	/** Saklar "harus lunas biaya perkuliahan". Lihat {@link #getHarusBayar()}. */
	private Boolean harusBayar;
	/** Daftar kode {@link ItemBiaya} yang wajib dilunasi, dipisah koma. Lihat {@link #getKodeItemBiaya()}. */
	private String kodeItemBiaya;
	/** Semester penyelenggaraan (Ganjil/Genap). Lihat {@link #getSemester()}. */
	private String semester;
	/** Tahun akademik penyelenggaraan ({@code 2025/2026}). Lihat {@link #getTahunAkademik()}. */
	private String tahunAkademik;
	/** Daftar putih NIM yang dibebaskan dari pemeriksaan biaya. Lihat {@link #getNimMhsTanpaBiaya()}. */
	private String nimMhsTanpaBiaya;
	/** Kode jenis aktivitas mahasiswa untuk ekspor Feeder. Lihat {@link #getJenisAktfitasMahasiswa()}. */
	private JenisAktfitasMahasiswa jenisAktfitasMahasiswa;
	/** Izin mahasiswa menyunting agenda kegiatan. Lihat {@link #getMahasiswaBolehMerubahAgenda()}. */
	private Boolean mahasiswaBolehMerubahAgenda;
	/** Izin dosen pembimbing menyunting agenda kegiatan. Lihat {@link #getDosenBolehMerubahAgenda()}. */
	private Boolean dosenBolehMerubahAgenda;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate untuk instansiasi entity.
	 *
	 * <p>Object yang dibuat lewat konstruktor ini sudah membawa nilai awal field:
	 * {@code tanggal_mulai} dan {@code tanggal_selesai} berisi hari ini,
	 * {@code minimalSksBolehIkutPkl2} berisi {@code 110}, {@code minimalIpkBolehIkutPkl2} berisi
	 * {@code 2.0}, dan {@code tanggal_dirubah} berisi waktu sekarang. Seluruh field lain masih
	 * {@code null} sampai getter berdefault (lihat "Pola getter yang menulis balik" pada Javadoc
	 * kelas) atau layar {@code PklAction} mengisinya.</p>
	 */
	public Pkl() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dibangkitkan database ({@code GenerationType.IDENTITY}) dan dipetakan
	 * {@code insertable = false}, sehingga nilainya baru terisi <b>setelah</b> {@code INSERT}
	 * berhasil. Object {@code Pkl} yang belum tersimpan mengembalikan {@code null} &mdash; kode
	 * pemanggil yang memakai id sebagai penanda (mis. {@code PklUntukMahasiswaAction} yang
	 * menyusun {@code Restrictions.ne("pkl", pkl)} hanya bila {@code getId() != null}) harus
	 * memperhitungkan hal ini.</p>
	 *
	 * @return kunci utama; {@code null} bila baris belum pernah tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama secara manual.
	 *
	 * <p>Umumnya tidak dipanggil kode aplikasi (nilainya dibangkitkan database); disediakan untuk
	 * Hibernate dan proses impor/rekonstruksi data.</p>
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengisi nama program PKL.
	 *
	 * <p>Ini adalah setter nama yang sesungguhnya; {@link #setNama(String)} hanya menyentuh field
	 * bayangan. Dipanggil layar {@code ais.action.master.PklAction} saat menyimpan.</p>
	 *
	 * @param nama_kelompok nama program PKL
	 */
	public void setNama_kelompok(String nama_kelompok) {
		this.nama_kelompok = nama_kelompok;
	}

	/**
	 * Mengembalikan nama program PKL.
	 *
	 * <p>Meskipun namanya berbunyi "kelompok" (sisa cetakan {@code hbm2java} yang berbagi bentuk
	 * dengan {@link ais.database.model.pkl.KelompokPkl}), isi kolom ini adalah nama <i>program</i>
	 * PKL &mdash; misalnya "PKL Ganjil 2025/2026 Fakultas Teknik". Nama kelompok/penempatan yang
	 * sebenarnya ada di {@code KelompokPkl.getNama_kelompok()}.</p>
	 *
	 * <p>Dipakai antara lain sebagai judul revisi ({@code RevisiHelper.createNewRevisi(Pkl.class,
	 * pkl, pkl.getNama_kelompok())}) dan sebagai label pada grid daftar PKL.</p>
	 *
	 * @return nama program PKL; {@code null} bila belum diisi (getter ini tidak berdefault)
	 */
	public String getNama_kelompok() {
		return nama_kelompok;
	}

	/**
	 * Mengisi tanggal mulai periode PKL.
	 *
	 * @param tanggal_mulai tanggal mulai; boleh {@code null} (getter akan mengembalikan hari ini)
	 */
	public void setTanggal_mulai(Date tanggal_mulai) {
		this.tanggal_mulai = tanggal_mulai;
	}

	/**
	 * Mengembalikan tanggal mulai periode PKL.
	 *
	 * <p><b>Punya fallback.</b> Bila field masih {@code null}, method mengembalikan tanggal
	 * <i>hari ini</i> versi kampus ({@code WaktuUtil.getDate()}). Fallback ini <b>tidak</b>
	 * ditulis balik ke field, tetapi karena kelas ini memakai property access, nilai itulah yang
	 * dilihat Hibernate saat dirty-check &mdash; jadi kolomnya tetap bisa terisi tanggal hari ini
	 * pada flush berikutnya.</p>
	 *
	 * <p><b>Bukan sekadar tanggal.</b> Nilai ini menjadi acuan turunan
	 * {@link #getSemester()} dan {@link #getTahunAkademik()}, yang pada gilirannya dipakai
	 * {@code Common.checkSyaratPkl} untuk menghitung semester mahasiswa dan menyinkronkan KRS.
	 * Mengubah tanggal mulai program yang sudah berjalan karena itu ikut menggeser periode
	 * akademik yang dibaca mesin pemeriksa syarat.</p>
	 *
	 * <p>Dipakai juga sebagai kolom pengurut daftar PKL
	 * ({@code Order.desc("tanggal_mulai")}).</p>
	 *
	 * @return tanggal mulai; tanggal hari ini bila field {@code null}
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_mulai() {
		return tanggal_mulai == null ? ais.ui.util.WaktuUtil.getDate() : tanggal_mulai;
	}

	/**
	 * Mengisi tanggal selesai periode PKL.
	 *
	 * @param tanggal_selesai tanggal selesai; boleh {@code null}
	 */
	public void setTanggal_selesai(Date tanggal_selesai) {
		this.tanggal_selesai = tanggal_selesai;
	}

	/**
	 * Mengembalikan tanggal selesai periode PKL.
	 *
	 * <p><b>Tanpa fallback</b>, berbeda dengan {@link #getTanggal_mulai()} yang mengembalikan hari
	 * ini saat {@code null}. Asimetri ini disengaja dibiarkan; pemanggil (mis. perender grid pada
	 * {@code PklAction} dan {@code PklUntukMahasiswaAction}) sudah menjaga {@code null} sendiri
	 * sebelum memformat tanggal.</p>
	 *
	 * @return tanggal selesai; {@code null} bila belum diisi
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal_selesai() {
		return tanggal_selesai;
	}

	/**
	 * Mengisi catatan bebas tentang program PKL.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan catatan bebas tentang program PKL (deskripsi, pengumuman, syarat naratif).
	 *
	 * @return catatan bebas; {@code null} bila belum diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi pembatas jurusan peserta.
	 *
	 * @param jurusan jurusan sasaran; {@code null} berarti program terbuka untuk semua jurusan
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Mengembalikan jurusan yang boleh mengikuti program ini.
	 *
	 * <p><b>Semantik {@code null}:</b> {@code null} berarti <i>tidak dibatasi</i> (semua jurusan).
	 * {@code PklUntukMahasiswaAction.initCriteria()} menyaring dengan
	 * {@code or(isNull("jurusan"), eq("jurusan", jurusanMahasiswa))}, dan
	 * {@code Common.checkSyaratPkl} menolak pendaftaran bila jurusan mahasiswa berbeda dari
	 * nilai ini.</p>
	 *
	 * <p><b>Efek samping:</b> berpola {@code jurusan = check(jurusan); return jurusan;} &mdash;
	 * proxy lazy diresolusi lebih dulu dan hasilnya ditulis balik ke field, sehingga getter ini
	 * bisa memicu pembacaan database (lihat {@link GeneralValueObject#check(Object)}).</p>
	 *
	 * @return jurusan sasaran; {@code null} bila program terbuka untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * Mengisi pembatas fakultas peserta.
	 *
	 * @param fakultas fakultas sasaran; {@code null} berarti program terbuka untuk semua fakultas
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/**
	 * Mengembalikan fakultas yang boleh mengikuti program ini.
	 *
	 * <p><b>Semantik {@code null}:</b> sama seperti {@link #getJurusan()} &mdash; {@code null}
	 * berarti tidak dibatasi. Penyaringan dan penolakan dilakukan di
	 * {@code PklUntukMahasiswaAction.initCriteria()} serta {@code Common.checkSyaratPkl}
	 * (yang membandingkan dengan fakultas dari jurusan mahasiswa).</p>
	 *
	 * <p><b>Efek samping:</b> berpola {@code fakultas = check(fakultas); return fakultas;} &mdash;
	 * resolusi proxy lazy dengan penulisan balik ke field.</p>
	 *
	 * @return fakultas sasaran; {@code null} bila program terbuka untuk semua fakultas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/**
	 * Mengembalikan jumlah SKS lulus minimal yang harus dimiliki mahasiswa untuk mendaftar
	 * program ini (syarat utama).
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method <b>menulis {@code 100} ke
	 * field</b> lalu mengembalikannya. Pada entity yang <i>managed</i>, nilai default itu ikut
	 * ter-{@code UPDATE} ke database pada flush berikutnya walaupun tidak ada layar yang menekan
	 * tombol simpan.</p>
	 *
	 * <p><b>Dipakai di.</b> {@code Common.checkSyaratPkl(Mahasiswa, Pkl)} membandingkan nilai ini
	 * dengan {@code KrsMahasiswa.getSksk()}; bila lolos bersama
	 * {@link #getMinimalIpkBolehIkutPkl()}, pendaftaran diteruskan. Juga ditampilkan sebagai label
	 * ringkasan syarat di {@code PklAction} dan {@code PklUntukMahasiswaAction}.</p>
	 *
	 * @return ambang SKS minimal; {@code 100} bila belum pernah diisi
	 */
	public Integer getMinimalSksBolehIkutPkl() {
		if (minimalSksBolehIkutPkl == null) {
			minimalSksBolehIkutPkl = 100;
		}
		return minimalSksBolehIkutPkl;
	}

	/**
	 * Mengisi ambang SKS minimal (syarat utama).
	 *
	 * @param minimalSksBolehIkutPkl ambang SKS; {@code null} akan berdefault {@code 100} saat
	 *                               dibaca kembali
	 */
	public void setMinimalSksBolehIkutPkl(Integer minimalSksBolehIkutPkl) {
		this.minimalSksBolehIkutPkl = minimalSksBolehIkutPkl;
	}

	/**
	 * Mengembalikan IPK minimal yang harus dimiliki mahasiswa untuk mendaftar program ini
	 * (syarat utama).
	 *
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method <b>menulis {@code 3.0} ke
	 * field</b> lalu mengembalikannya &mdash; default yang cukup ketat, dan ikut tersimpan ke
	 * database pada flush berikutnya. Perhatikan kontrasnya dengan
	 * {@link #getMinimalIpkBolehIkutPkl2()} yang berdefault {@code 0.0}.</p>
	 *
	 * <p><b>Dipakai di.</b> {@code Common.checkSyaratPkl} membandingkannya dengan
	 * {@code KrsMahasiswa.getIpk()}.</p>
	 *
	 * @return ambang IPK minimal; {@code 3.0} bila belum pernah diisi
	 */
	public Double getMinimalIpkBolehIkutPkl() {
		if (minimalIpkBolehIkutPkl == null) {
			minimalIpkBolehIkutPkl = 3.0;
		}
		return minimalIpkBolehIkutPkl;
	}

	/**
	 * Mengisi ambang IPK minimal (syarat utama).
	 *
	 * @param minimalIpkBolehIkutPkl ambang IPK; {@code null} akan berdefault {@code 3.0} saat
	 *                               dibaca kembali
	 */
	public void setMinimalIpkBolehIkutPkl(Double minimalIpkBolehIkutPkl) {
		this.minimalIpkBolehIkutPkl = minimalIpkBolehIkutPkl;
	}

	/**
	 * Mengembalikan ambang SKS minimal untuk <b>jalur syarat alternatif</b>.
	 *
	 * <p><b>Kapan dipakai.</b> Hanya diperiksa {@code Common.checkSyaratPkl} ketika mahasiswa
	 * <i>gagal</i> memenuhi syarat utama <b>dan</b> {@link #getAktifkanSyaratLain()} bernilai
	 * {@code true}. Idenya: memberi jalur kedua yang lebih longgar (mis. SKS lebih tinggi tapi IPK
	 * lebih rendah) tanpa melonggarkan syarat utama.</p>
	 *
	 * <p><b>Jebakan penting.</b> Nilai awal <i>field</i> adalah {@code 110}, tetapi default
	 * <i>getter</i> ini adalah <b>{@code 0}</b>. Keduanya berlaku pada situasi berbeda: object
	 * baru dari konstruktor membawa {@code 110}, sedangkan baris lama yang kolomnya masih
	 * {@code NULL} di database akan dibaca sebagai {@code 0} &mdash; dan {@code 0} ditulis balik
	 * ke field sehingga ikut tersimpan. Bila baris semacam itu juga punya
	 * {@code aktifkanSyaratLain = true}, jalur alternatif menjadi {@code sks >= 0 && ipk >= 0.0}
	 * yang <b>selalu benar</b>, sehingga seluruh penyaringan SKS/IPK program tersebut efektif
	 * mati. Periksa dulu isi kolom sebelum mengaktifkan syarat alternatif pada program lama.</p>
	 *
	 * @return ambang SKS jalur alternatif; {@code 0} bila kolomnya {@code NULL}
	 */
	public Integer getMinimalSksBolehIkutPkl2() {
		if (minimalSksBolehIkutPkl2 == null) {
			minimalSksBolehIkutPkl2 = 0;
		}
		return minimalSksBolehIkutPkl2;
	}

	/**
	 * Mengisi ambang SKS jalur syarat alternatif.
	 *
	 * @param minimalSksBolehIkutPkl2 ambang SKS alternatif; {@code null} akan berdefault
	 *                                {@code 0} saat dibaca kembali
	 */
	public void setMinimalSksBolehIkutPkl2(Integer minimalSksBolehIkutPkl2) {
		this.minimalSksBolehIkutPkl2 = minimalSksBolehIkutPkl2;
	}

	/**
	 * Mengembalikan ambang IPK minimal untuk <b>jalur syarat alternatif</b>.
	 *
	 * <p>Berpasangan dengan {@link #getMinimalSksBolehIkutPkl2()} dan tunduk pada jebakan yang
	 * sama: nilai awal field {@code 2.0} hanya berlaku untuk object baru, sedangkan kolom
	 * {@code NULL} pada baris lama dibaca (dan disimpan ulang) sebagai <b>{@code 0.0}</b>.</p>
	 *
	 * @return ambang IPK jalur alternatif; {@code 0.0} bila kolomnya {@code NULL}
	 */
	public Double getMinimalIpkBolehIkutPkl2() {
		if (minimalIpkBolehIkutPkl2 == null) {
			minimalIpkBolehIkutPkl2 = 0.0;
		}
		return minimalIpkBolehIkutPkl2;
	}

	/**
	 * Mengisi ambang IPK jalur syarat alternatif.
	 *
	 * @param minimalIpkBolehIkutPkl2 ambang IPK alternatif; {@code null} akan berdefault
	 *                                {@code 0.0} saat dibaca kembali
	 */
	public void setMinimalIpkBolehIkutPkl2(Double minimalIpkBolehIkutPkl2) {
		this.minimalIpkBolehIkutPkl2 = minimalIpkBolehIkutPkl2;
	}

	/**
	 * Menyatakan apakah jalur syarat alternatif (pasangan {@code ...BolehIkutPkl2}) diaktifkan.
	 *
	 * <p><b>Saklar satu arah dalam arti pelonggaran:</b> {@code false} berarti hanya syarat utama
	 * yang berlaku; {@code true} berarti mahasiswa yang gagal syarat utama masih diberi kesempatan
	 * lolos lewat ambang kedua. Flag ini <b>tidak pernah memperketat</b> apa pun &mdash; ia hanya
	 * bisa menambah pendaftar yang lolos, tidak pernah mengurangi.</p>
	 *
	 * <p>Di layar {@code PklAction}, mencentang saklar ini juga mengaktifkan kembali dua kotak isian
	 * ambang alternatif ({@code setDisabled(!aktifkanSyaratLain.isChecked())}).</p>
	 *
	 * <p><b>Efek samping:</b> menulis {@code false} ke field bila masih {@code null}, sehingga
	 * default itu ikut tersimpan pada flush berikutnya.</p>
	 *
	 * @return {@code true} bila jalur syarat alternatif aktif; {@code false} bila belum pernah
	 *         diisi
	 */
	public Boolean getAktifkanSyaratLain() {
		if (aktifkanSyaratLain == null) {
			aktifkanSyaratLain = false;
		}
		return aktifkanSyaratLain;
	}

	/**
	 * Mengaktifkan atau menonaktifkan jalur syarat alternatif.
	 *
	 * @param aktifkanSyaratLain {@code true} untuk mengaktifkan jalur alternatif; {@code null}
	 *                           akan berdefault {@code false} saat dibaca kembali
	 */
	public void setAktifkanSyaratLain(Boolean aktifkanSyaratLain) {
		this.aktifkanSyaratLain = aktifkanSyaratLain;
	}

	/**
	 * Menyatakan apakah peserta wajib lunas <b>biaya perkuliahan semester berjalan</b> sebelum
	 * boleh mendaftar.
	 *
	 * <p><b>Cara kerja di pemanggil.</b> Bila {@code true},
	 * {@code PklUntukMahasiswaAction} menghitung semester mahasiswa lewat
	 * {@code Common.getSemester(...)} lalu memanggil
	 * {@code Common.checkStatusPembayaranMahasiswa(...)}; bila belum lunas, jendela pendaftaran
	 * ditutup dengan pesan agar menghubungi bagian keuangan.</p>
	 *
	 * <p><b>Berbeda dari {@link #getKodeItemBiaya()}.</b> Flag ini memeriksa status pembayaran
	 * kuliah secara umum, sedangkan {@code kodeItemBiaya} memeriksa cicilan atas item biaya
	 * tertentu. Keduanya bisa aktif bersamaan, dan keduanya <b>dilewati sepenuhnya</b> untuk NIM
	 * yang terdaftar di {@link #getNimMhsTanpaBiaya()}.</p>
	 *
	 * <p><b>Efek samping:</b> menulis {@code false} ke field bila masih {@code null}.</p>
	 *
	 * @return {@code true} bila kelunasan biaya perkuliahan diwajibkan; {@code false} bila belum
	 *         pernah diisi
	 */
	public Boolean getHarusBayar() {
		if (harusBayar == null) {
			harusBayar = false;
		}
		return harusBayar;
	}

	/**
	 * Mengatur kewajiban lunas biaya perkuliahan bagi peserta.
	 *
	 * @param harusBayar {@code true} untuk mewajibkan; {@code null} akan berdefault {@code false}
	 *                   saat dibaca kembali
	 */
	public void setHarusBayar(Boolean harusBayar) {
		this.harusBayar = harusBayar;
	}

	/**
	 * Mengembalikan daftar kode {@link ItemBiaya} yang harus sudah pernah dicicil/dibayar peserta,
	 * ditulis sebagai <b>satu string dipisah koma</b> (mis. {@code "PKL,ALMAMATER"}).
	 *
	 * <p><b>Cara dikonsumsi.</b> {@code PklUntukMahasiswaAction} (dan layar admin
	 * {@code PklAction}) melakukan {@code getKodeItemBiaya().trim().split(",")}, mencari
	 * {@code ItemBiaya} per kode, lalu menghitung baris {@code CicilanPembayaran} milik mahasiswa
	 * untuk item tersebut. Bila hitungannya {@code 0}, pendaftaran ditolak dengan pesan agar
	 * menghubungi bagian keuangan. Kode yang tidak ditemukan di master {@code ItemBiaya}
	 * <b>diabaikan diam-diam</b> &mdash; salah ketik kode berarti syaratnya hilang tanpa
	 * peringatan.</p>
	 *
	 * <p><b>Efek samping:</b> menulis string kosong ke field bila masih {@code null}. Berkat
	 * default ini, pemanggil boleh langsung memanggil {@code .trim()} tanpa takut NPE &mdash;
	 * pola {@code getKodeItemBiaya().trim().isEmpty()} dipakai di beberapa layar.</p>
	 *
	 * @return daftar kode item biaya dipisah koma; string kosong bila tidak ada syarat item biaya
	 */
	public String getKodeItemBiaya() {
		if (kodeItemBiaya == null) {
			kodeItemBiaya = "";
		}
		return kodeItemBiaya;
	}

	/**
	 * Mengisi daftar kode item biaya yang wajib dibayar peserta.
	 *
	 * @param kodeItemBiaya kode-kode {@link ItemBiaya} dipisah koma; {@code null} akan berdefault
	 *                      string kosong saat dibaca kembali
	 */
	public void setKodeItemBiaya(String kodeItemBiaya) {
		this.kodeItemBiaya = kodeItemBiaya;
	}

	/**
	 * Mengembalikan semester penyelenggaraan program ({@code Perkuliahan.GANJIL} atau
	 * {@code Perkuliahan.GENAP}).
	 *
	 * <p><b>Efek samping &amp; turunan.</b> Bila field masih {@code null}, nilainya
	 * <b>dihitung dari {@link #getTanggal_mulai()}</b> memakai
	 * {@code Common.isNowSemensterGanjil(...)} (ejaan asli method di {@code Common} memang begitu),
	 * lalu <b>ditulis ke field</b> sehingga ikut tersimpan pada flush berikutnya. Karena
	 * {@code getTanggal_mulai()} sendiri berdefault ke hari ini, program yang belum diberi tanggal
	 * mulai akan "membeku" ke semester saat pertama kali dibaca.</p>
	 *
	 * <p><b>Dipakai di.</b> {@code Common.checkSyaratPkl} meneruskannya ke
	 * {@code Common.getSemester(...)} untuk menghitung semester mahasiswa; juga dipakai saat
	 * memeriksa status pembayaran di {@code PklUntukMahasiswaAction}.</p>
	 *
	 * @return {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}
	 */
	public String getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil(getTanggal_mulai()) ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return semester;
	}

	/**
	 * Mengisi semester penyelenggaraan program.
	 *
	 * @param semester {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}; {@code null} akan
	 *                 dihitung ulang dari tanggal mulai saat dibaca kembali
	 */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun akademik penyelenggaraan program dalam bentuk {@code "2025/2026"}.
	 *
	 * <p><b>Efek samping &amp; turunan.</b> Sama polanya dengan {@link #getSemester()}: bila field
	 * masih {@code null}, nilainya dihitung dari {@link #getTanggal_mulai()} lewat
	 * {@code Common.getCurrentTahunAkademik(...)} dan <b>ditulis ke field</b> sehingga ikut
	 * tersimpan pada flush berikutnya.</p>
	 *
	 * <p><b>Diurai pemanggil.</b> {@code Common.checkSyaratPkl} memecah string ini dengan
	 * {@code StringUtils.split(ta, "/")[0]} lalu {@code Integer.parseInt} &mdash; format selain
	 * {@code "tahun/tahun"} akan melempar {@code NumberFormatException} di jalur pendaftaran.
	 * Jangan mengisi kolom ini dengan teks bebas.</p>
	 *
	 * @return tahun akademik bentuk {@code "2025/2026"}
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik(getTanggal_mulai());
		}
		return tahunAkademik;
	}

	/**
	 * Mengisi tahun akademik penyelenggaraan program.
	 *
	 * @param tahunAkademik tahun akademik bentuk {@code "2025/2026"}; {@code null} akan dihitung
	 *                      ulang dari tanggal mulai saat dibaca kembali
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan pembatas program studi peserta (mis. {@code "D3"}, {@code "S1"}), dicocokkan
	 * dengan {@code Mahasiswa.getProgram()}.
	 *
	 * <p><b>Normalisasi penting.</b> Method mengembalikan {@code null} bila field {@code null}
	 * <b>atau</b> berisi spasi belaka, dan mengembalikan versi {@code trim()} bila terisi. Ini
	 * bukan kosmetik: {@code PklUntukMahasiswaAction.initCriteria()} menyaring dengan
	 * {@code or(isNull("program"), eq("program", programMahasiswa))}, sehingga string kosong yang
	 * tersimpan di kolom <b>tidak</b> akan dianggap "semua program" dan justru menyembunyikan
	 * program itu dari <i>semua</i> mahasiswa. Karena kelas ini memakai property access, nilai
	 * {@code null} yang dikembalikan getter ini ikut menyembuhkan kolom tersebut saat flush.</p>
	 *
	 * <p><b>Tidak menulis balik ke field</b> &mdash; normalisasi hanya terjadi pada nilai
	 * kembalian.</p>
	 *
	 * @return kode program studi sasaran yang sudah di-{@code trim}; {@code null} bila program
	 *         terbuka untuk semua program studi
	 */
	public String getProgram() {
		return program == null || program.trim().isEmpty() ? null : program.trim();
	}

	/**
	 * Mengisi pembatas program studi peserta.
	 *
	 * <p>Layar {@code PklAction} mengisinya dari combobox dan sengaja mengirim {@code null} bila
	 * tidak ada item terpilih, yang berarti "semua program studi".</p>
	 *
	 * @param program kode program studi; {@code null} atau kosong berarti tidak dibatasi
	 */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Mengembalikan daftar putih NIM mahasiswa yang <b>dibebaskan dari seluruh pemeriksaan
	 * biaya</b> pada saat mendaftar program ini.
	 *
	 * <p><b>Dampak.</b> {@code PklUntukMahasiswaAction} memecah nilai ini dengan
	 * {@code split(",")} dan, bila NIM pendaftar ditemukan di dalamnya, <b>melewati sekaligus</b>
	 * pemeriksaan {@link #getKodeItemBiaya()} dan {@link #getHarusBayar()}. Pemeriksaan syarat
	 * akademis ({@code Common.checkSyaratPkl}) tetap berjalan. Kolom ini hanya bisa disunting dari
	 * layar admin {@code PklAction}.</p>
	 *
	 * <p><b>Efek samping (normalisasi destruktif).</b> Method ini <b>selalu menimpa field</b>,
	 * bukan sekadar membaca. Isi dinormalkan menjadi bentuk berpagar koma {@code ,nim1,nim2,}:
	 * nilai {@code null} atau {@code ","} menjadi string kosong, selebihnya dibungkus koma di
	 * kedua ujung lalu koma ganda diringkas lewat tiga kali {@code replaceAll(",,", ",")}.
	 * Bentuk itu stabil (memanggil ulang getter menghasilkan string yang sama), tetapi karena
	 * properti ini dipetakan Hibernate, <b>membaca saja sudah mengubah isi kolom</b> dari
	 * {@code "nim1,nim2"} menjadi {@code ",nim1,nim2,"} pada flush berikutnya.</p>
	 *
	 * <p><b>Kuirk.</b> Karena hasilnya berpagar koma, {@code split(",")} pada sisi pemanggil
	 * selalu menghasilkan elemen pertama berupa string kosong; pemanggil aman karena NIM
	 * mahasiswa tidak pernah cocok dengan string kosong. Tiga cabang {@code if} pembersih
	 * ({@code ","}, {@code ",,"}, {@code ",,,"}) serta pemeriksaan {@code null} pada baris
	 * {@code return} praktis tidak terjangkau setelah tiga kali {@code replaceAll} di atas
	 * &mdash; sisa kode defensif yang dibiarkan apa adanya.</p>
	 *
	 * @return daftar NIM bebas biaya berpagar koma ({@code ,nim1,nim2,}); string kosong bila tidak
	 *         ada pembebasan
	 */
	public String getNimMhsTanpaBiaya() {
		nimMhsTanpaBiaya = (nimMhsTanpaBiaya == null || nimMhsTanpaBiaya.trim().equalsIgnoreCase(",") ? ""
				: "," + nimMhsTanpaBiaya.trim() + ",").replaceAll(",,", ",").replaceAll(",,", ",")
				.replaceAll(",,", ",");

		if (nimMhsTanpaBiaya.equals(",")) {
			nimMhsTanpaBiaya = "";
		} else if (nimMhsTanpaBiaya.equals(",,")) {
			nimMhsTanpaBiaya = "";
		} else if (nimMhsTanpaBiaya.equals(",,,")) {
			nimMhsTanpaBiaya = "";
		}
		return nimMhsTanpaBiaya == null ? "" : nimMhsTanpaBiaya.trim();
	}

	/**
	 * Mengisi daftar putih NIM yang dibebaskan dari pemeriksaan biaya.
	 *
	 * <p>Nilai boleh ditulis sebagai daftar dipisah koma tanpa pagar ({@code "nim1,nim2"});
	 * {@link #getNimMhsTanpaBiaya()} akan menormalkannya sendiri pada pembacaan pertama.</p>
	 *
	 * @param nimMhsTanpaBiaya daftar NIM dipisah koma; {@code null} berarti tidak ada pembebasan
	 */
	public void setNimMhsTanpaBiaya(String nimMhsTanpaBiaya) {
		this.nimMhsTanpaBiaya = nimMhsTanpaBiaya;
	}

	/**
	 * Mengembalikan jenis aktivitas mahasiswa (kode referensi Neo Feeder/PDDikti) yang mewakili
	 * program ini saat pelaporan.
	 *
	 * <p><b>Untuk apa.</b> Dipakai eksportir {@code EksporAktifitasPklFeeder},
	 * {@code EksporPesertaDosenPklFeeder}, dan {@code EksporPesertaMahasiswaPklFeeder} sebagai
	 * {@code id_jns_akt_mhs} ketika mengirim aktivitas PKL ke Feeder.</p>
	 *
	 * <p><b>Dua perilaku sekaligus.</b> Pertama, {@code jenisAktfitasMahasiswa =
	 * check(jenisAktfitasMahasiswa);} meresolusi proxy lazy dan <b>menulis balik</b> ke field.
	 * Kedua, bila hasilnya tetap {@code null}, method mengembalikan konstanta global
	 * {@link ConstantValues#PKL} <b>tanpa</b> menulisnya ke field. Nilai fallback itu tetap bisa
	 * berakhir di kolom {@code jenis_aktfitas_mahasiswa} karena pemetaan property access membuat
	 * Hibernate melihat nilai kembalian getter saat dirty-check.</p>
	 *
	 * <p><b>Fallback bisa {@code null}.</b> {@code ConstantValues.PKL} hanya terisi bila
	 * sinkronisasi referensi Feeder di {@code ais.common.InitDataHelper} pernah berjalan dan
	 * menemukan jenis aktivitas bernama "Kerja praktek/PKL". Pada instalasi yang belum pernah
	 * sinkron, method ini tetap mengembalikan {@code null}.</p>
	 *
	 * @return jenis aktivitas mahasiswa untuk program ini; {@link ConstantValues#PKL} bila field
	 *         kosong, dan {@code null} bila konstanta itu pun belum terisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_aktfitas_mahasiswa", nullable = true)
	public JenisAktfitasMahasiswa getJenisAktfitasMahasiswa() {
		jenisAktfitasMahasiswa = check(jenisAktfitasMahasiswa);
		return (JenisAktfitasMahasiswa) (jenisAktfitasMahasiswa == null ? ConstantValues.PKL : jenisAktfitasMahasiswa);
	}

	/**
	 * Mengisi jenis aktivitas mahasiswa untuk pelaporan Feeder.
	 *
	 * @param jenisAktfitasMahasiswa jenis aktivitas; {@code null} berarti memakai fallback
	 *                               {@link ConstantValues#PKL}
	 */
	public void setJenisAktfitasMahasiswa(JenisAktfitasMahasiswa jenisAktfitasMahasiswa) {
		this.jenisAktfitasMahasiswa = jenisAktfitasMahasiswa;
	}

	/**
	 * Menyatakan apakah <b>mahasiswa peserta</b> boleh menyunting agenda kegiatan PKL
	 * (menambah/mengubah {@link Pertemuan} pada kelompoknya).
	 *
	 * <p><b>Default {@code true} (fail-open).</b> Bila field masih {@code null}, method
	 * mengembalikan {@code true} &mdash; berbeda dari flag boolean lain di kelas ini yang
	 * berdefault {@code false}. Program lama yang kolomnya belum pernah diisi karena itu
	 * <b>mengizinkan</b> mahasiswa menyunting agenda. Nilai default ini <b>tidak</b> ditulis balik
	 * ke field.</p>
	 *
	 * <p><b>Dipakai di.</b> {@code AktifitasPklHelper.initDetail(...)} menghitung flag
	 * {@code edit}: pengguna yang bukan mahasiswa dan bukan dosen (mis. petugas) selalu boleh,
	 * admin selalu boleh, mahasiswa boleh hanya bila method ini {@code true}, dan dosen boleh
	 * hanya bila {@link #getDosenBolehMerubahAgenda()} {@code true}. Flag {@code edit} itulah yang
	 * menentukan tampil/tidaknya tombol-tombol pengubah agenda.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh menyunting agenda; {@code true} pula bila belum
	 *         pernah diisi
	 */
	public Boolean getMahasiswaBolehMerubahAgenda() {
		return mahasiswaBolehMerubahAgenda == null ? true : mahasiswaBolehMerubahAgenda;
	}

	/**
	 * Mengatur izin mahasiswa menyunting agenda kegiatan PKL.
	 *
	 * <p>Diisi dari checkbox "Mahasiswa boleh mengubah agenda" di layar {@code PklAction}.</p>
	 *
	 * @param mahasiswaBolehMerubahAgenda {@code false} untuk mengunci agenda dari mahasiswa;
	 *                                    {@code null} akan dibaca sebagai {@code true}
	 */
	public void setMahasiswaBolehMerubahAgenda(Boolean mahasiswaBolehMerubahAgenda) {
		this.mahasiswaBolehMerubahAgenda = mahasiswaBolehMerubahAgenda;
	}

	/**
	 * Menyatakan apakah <b>dosen pembimbing</b> boleh menyunting agenda kegiatan PKL.
	 *
	 * <p>Berperilaku identik dengan {@link #getMahasiswaBolehMerubahAgenda()}, termasuk default
	 * {@code true} (fail-open) yang tidak ditulis balik ke field, dan dipakai pada perhitungan
	 * flag {@code edit} yang sama di {@code AktifitasPklHelper.initDetail(...)}.</p>
	 *
	 * @return {@code true} bila dosen boleh menyunting agenda; {@code true} pula bila belum pernah
	 *         diisi
	 */
	public Boolean getDosenBolehMerubahAgenda() {
		return dosenBolehMerubahAgenda == null ? true : dosenBolehMerubahAgenda;
	}

	/**
	 * Mengatur izin dosen pembimbing menyunting agenda kegiatan PKL.
	 *
	 * <p>Diisi dari checkbox "Dosen boleh mengubah agenda" di layar {@code PklAction}.</p>
	 *
	 * @param dosenBolehMerubahAgenda {@code false} untuk mengunci agenda dari dosen; {@code null}
	 *                                akan dibaca sebagai {@code true}
	 */
	public void setDosenBolehMerubahAgenda(Boolean dosenBolehMerubahAgenda) {
		this.dosenBolehMerubahAgenda = dosenBolehMerubahAgenda;
	}
}
