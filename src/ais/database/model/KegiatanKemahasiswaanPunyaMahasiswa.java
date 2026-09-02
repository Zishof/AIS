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
 * Entity <b>kepesertaan kegiatan kemahasiswaan</b> &mdash; tabel
 * {@code public.kegiatan_kemahasiswaan_punya_mahasiswa}.
 *
 * <p>Satu baris = <b>satu mahasiswa yang terdaftar pada satu kegiatan kemahasiswaan</b>. Inilah
 * entity PENGHUBUNG (junction) antara {@link KegiatanKemahasiswaan} &mdash; katalog/wadah
 * kegiatan, organisasi, UKM, kepanitiaan, seminar, lomba &mdash; dan {@link Mahasiswa}. Nama
 * "punya" pada tabel/kelas dibaca sebagai "kegiatan X <i>punya</i> mahasiswa Y", bukan
 * kepemilikan dalam arti akademik.</p>
 *
 * <p>{@link KegiatanKemahasiswaan} sengaja <b>tidak</b> menyimpan daftar pesertanya; semua
 * kepesertaan hanya ada di sini. Hanya dua entity di paket ini yang menunjuk
 * {@link KegiatanKemahasiswaan} lewat kolom {@code kegiatan_kemahasiswaan}: kelas ini
 * ({@code nullable = false}) dan {@link FormulirKegiatan} ({@code nullable = true}).</p>
 *
 * <h2>Bukan junction murni: baris ini punya muatan sendiri</h2>
 *
 * <p>Selain sepasang kunci asing wajib ({@link #getKegiatanKemahasiswaan()} +
 * {@link #getMahasiswa()}), setiap baris membawa data <i>per peserta</i> yang bisa berbeda dari
 * nilai tingkat kegiatan:</p>
 *
 * <ul>
 *   <li>{@link #getJabatanKegiatanKemahasiswaan()} &mdash; peran peserta dalam kegiatan (ketua,
 *       anggota, panitia, peserta, dsb.);</li>
 *   <li>{@link #getSkalaKegiatanKemahasiswaan()} &mdash; skala/tingkat (prodi, universitas,
 *       nasional, internasional) yang diakui bagi peserta ini;</li>
 *   <li>{@link #getMulai()}/{@link #getSampai()} &mdash; rentang keterlibatan peserta, yang bisa
 *       lebih sempit dari rentang kegiatannya;</li>
 *   <li>{@link #getKeterangan()} &mdash; catatan bebas ({@code text});</li>
 *   <li>{@link #getPersetujuan()} &mdash; verifikasi peserta oleh petugas;</li>
 *   <li>{@link #getTbmuser()}/{@link #getDiubahDari()} &mdash; jejak siapa dan layar mana yang
 *       membuat/mengubah baris ini.</li>
 * </ul>
 *
 * <p>Pasangan jabatan + skala inilah yang kemudian dipakai rubrik angka kredit
 * ({@code NilaiKegiatanKemahasiswaan} memetakan
 * {@link DetailKelompokKegiatanKemahasiswaan} &times; {@link JabatanKegiatanKemahasiswaan}
 * &times; {@link SkalaKegiatanKemahasiswaan} ke sebuah nilai), sehingga baris di tabel ini
 * berdampak langsung ke rekap angka kredit mahasiswa.</p>
 *
 * <h2>Pola "warisan nilai dari kegiatan induk"</h2>
 *
 * <p>Empat getter di kelas ini <b>tidak</b> mengembalikan isi kolom apa adanya: bila kolomnya
 * kosong, nilainya diambil dari {@link KegiatanKemahasiswaan} induk.</p>
 *
 * <ul>
 *   <li>{@link #getJabatanKegiatanKemahasiswaan()} dan {@link #getSkalaKegiatanKemahasiswaan()}
 *       &mdash; menyalin nilai induk <b>ke field</b> bila field masih {@code null};</li>
 *   <li>{@link #getMulai()} dan {@link #getSampai()} &mdash; mengembalikan tanggal induk bila
 *       field {@code null}, <b>tanpa</b> menuliskannya ke field.</li>
 * </ul>
 *
 * <p><b>Konsekuensi penting.</b> Pemetaan entity ini memakai <i>property access</i> (anotasi JPA
 * dipasang pada getter, lihat {@link #getId()}), sehingga Hibernate membaca nilai yang akan
 * disimpan <b>lewat getter</b>. Nilai warisan itu karena itu ikut <b>tertulis permanen</b> ke
 * kolom pada operasi {@code UPDATE}/{@code INSERT} berikutnya: yang semula "kosong = ikut
 * kegiatan" berubah menjadi salinan tetap. Bila tanggal kegiatan induk kemudian digeser, baris
 * peserta yang sudah pernah tersimpan <b>tidak</b> ikut bergeser lagi. Ini perilaku nyata di
 * kode, bukan dugaan &mdash; jangan mengandalkan warisan itu tetap "hidup".</p>
 *
 * <h2>Alur persetujuan dua tingkat</h2>
 *
 * <p>Ada dua gerbang berurutan:</p>
 *
 * <ol>
 *   <li><b>Tingkat kegiatan</b> &mdash; {@link KegiatanKemahasiswaan#getStatus()} harus bernilai
 *       {@link PrestasiMahasiswa#DISETUJUI}. Sebelum itu, {@link #getPersetujuan()} di sini
 *       <b>selalu</b> mengembalikan {@code false} (dan menimpa field-nya, lihat peringatan pada
 *       method tersebut).</li>
 *   <li><b>Tingkat peserta</b> &mdash; checkbox "Setujui" per baris di
 *       {@code KegiatanKemahasiswaanPunyaMahasiswaHelper}. Hanya dirender untuk pengguna
 *       <b>non-mahasiswa</b> ({@code tbmuser.getMahasiswa() == null}) DAN hanya bila kegiatan
 *       induknya sudah {@code DISETUJUI}.</li>
 * </ol>
 *
 * <p>Efek {@code persetujuan = true} pada layar peserta: seluruh field editable (jabatan, skala,
 * keterangan, mulai, sampai) menjadi <i>disabled</i>, tombol "Hapus" disembunyikan, dan tombol
 * "Sertifikat" muncul bila {@link KegiatanKemahasiswaan#getSertifikat()} terisi
 * ({@code SertifikatAction.cetakSertifikat(...)} menerima langsung instance kelas ini).
 * Jadi {@code persetujuan} berfungsi sebagai <b>kunci baris</b>, bukan sekadar label.</p>
 *
 * <h2>Siapa yang MEMBUAT baris di tabel ini</h2>
 *
 * <p>Tidak ada method pabrik/query statis di kelas ini; seluruh pembuatan baris dilakukan dari
 * luar, dan semuanya memakai pola yang sama: cari dulu pasangan (kegiatan, mahasiswa) dengan
 * {@code setMaxResults(1).uniqueResult()}, kalau {@code null} baru {@code new}. Lima jalur yang
 * terverifikasi:</p>
 *
 * <ul>
 *   <li>{@code ais.action.master.helper.AmbilDataMahasiswaForKegiatanKemahasiswaanHelper} &mdash;
 *       petugas memilih banyak mahasiswa untuk satu kegiatan (arah kegiatan &rarr; mahasiswa);</li>
 *   <li>{@code ais.action.master.helper.AmbilDataKegiatanForKegiatanKemahasiswaanHelper} &mdash;
 *       arah sebaliknya, satu mahasiswa mendaftar ke banyak kegiatan;</li>
 *   <li>{@code ais.action.master.KegiatanKemahasiswaanAction} &mdash; unggah Excel massal
 *       (kolom NIM, mulai, sampai, jabatan, keterangan, persetujuan) dan pembuatan otomatis
 *       baris peserta bagi mahasiswa yang mengajukan kegiatan sendiri lewat
 *       {@code onSave()};</li>
 *   <li>{@code ais.action.master.helper.FormulirKegiatanPesertaHelper} &mdash; konversi massal
 *       peserta formulir kegiatan yang sudah di-ACC; baris hasil konversi langsung dibuat dengan
 *       {@code persetujuan = true} dan mewarisi {@code mulai}/{@code sampai} formulir;</li>
 *   <li>{@code ais.action.master.helper.MahasiswaPunyaKegiatanKemahasiswaanHelper} &mdash; layar
 *       mahasiswa (profil/self-service) yang memanggil dua helper "AmbilData" di atas.</li>
 * </ul>
 *
 * <p>Perhatikan bahwa <b>tidak ada</b> {@code unique constraint} atas pasangan
 * ({@code kegiatan_kemahasiswaan}, {@code mahasiswa}) dalam pemetaan ini. Keunikan hanya dijaga
 * oleh pola "query dulu, baru insert" di kelima jalur tersebut &mdash; artinya dua permintaan
 * bersamaan (atau satu jalur baru yang lupa memeriksa) bisa menghasilkan baris peserta ganda,
 * dan rekap angka kredit akan menghitungnya dua kali.</p>
 *
 * <h2>Siapa yang MEMBACA tabel ini</h2>
 *
 * <ul>
 *   <li><b>Layar peserta</b> &mdash; {@code KegiatanKemahasiswaanPunyaMahasiswaHelper} (daftar
 *       peserta satu kegiatan, dengan filter fakultas/jurusan/angkatan/nama-NIM/status
 *       persetujuan) dan {@code MahasiswaPunyaKegiatanKemahasiswaanHelper} (daftar kegiatan satu
 *       mahasiswa, juga dipakai sebagai rekap lintas mahasiswa).</li>
 *   <li><b>Dasbor</b> &mdash; {@code DasboardAktivitasMahasiswa},
 *       {@code DasborPerguruanTinggiTerpadu}, {@code DashboardKegiatanKemahasiswaanUmum},
 *       {@code DashboardRekapKegiatanMahasiswaan}.</li>
 *   <li><b>Integrasi Neo Feeder/PDDIKTI</b> &mdash; {@code FeederExporter} memakai tabel ini dua
 *       kali: mengelompokkan peserta per {@link Jurusan} untuk membuat satu record
 *       {@code AktivitasMahasiswa} per prodi, lalu mengirim tiap peserta sebagai
 *       {@code AnggotaAktivitasMahasiswa}.</li>
 *   <li><b>Cetak</b> &mdash; {@code SertifikatAction.cetakSertifikat(...)} (sertifikat per
 *       peserta) dan {@code LaporanPrestasiMahasiswa} (rekap prestasi per mahasiswa).</li>
 *   <li><b>Profil mahasiswa</b> &mdash; {@code ProfileMahasiswa}/{@code ProfileUiHelper} lewat
 *       cache JSON per mahasiswa, lihat bagian berikut.</li>
 * </ul>
 *
 * <h2>Cache JSON per mahasiswa &amp; {@code AuditListener}</h2>
 *
 * <p>Id baris kelas ini di-<i>denormalisasi</i> ke berkas JSON milik masing-masing mahasiswa:
 * {@link Mahasiswa#populateKegiatanKemahasiswaanPunyaMahasiswa(KegiatanKemahasiswaanPunyaMahasiswa)},
 * {@link Mahasiswa#removeKegiatanKemahasiswaanPunyaMahasiswa(java.io.Serializable)},
 * {@link Mahasiswa#reInitKegiatanKemahasiswaanPunyaMahasiswa(org.hibernate.Session)}, dan
 * {@link Mahasiswa#ambilKegiatanKemahasiswaanPunyaMahasiswa()}. Sinkronisasinya dijalankan
 * {@code ais.database.hibernate.AuditListener} pada event {@code post-insert}/{@code post-update}
 * (memanggil {@code populate...}) dan {@code post-delete} (memanggil {@code remove...}).</p>
 *
 * <p>Kelas ini juga terdaftar di {@code DataUtil.CLASS_IZINKAN}, yaitu daftar putih entity yang
 * boleh disimpan di cache MapDB antar-request. Konsekuensinya: instance kelas ini bisa hidup
 * lebih lama dari session Hibernate yang memuatnya &mdash; itulah sebabnya hampir semua getter
 * relasi di bawah memanggil {@link GeneralValueObject#check(Object)} untuk memulihkan proxy yang
 * sudah <i>detached</i>.</p>
 *
 * <h2>Lampiran bukti kegiatan</h2>
 *
 * <p>Berkas bukti tidak disimpan di tabel ini. Kedua layar peserta memanggil
 * {@code LampiranLain.createDownloadUploadFileLain(..., getId(),
 * KegiatanKemahasiswaanPunyaMahasiswa.class.getName(), "Bukti Kegiatan ...", ...)}, sehingga
 * lampiran tersimpan di tabel {@code lampiran_lain} dengan {@code ref = id} baris ini dan
 * {@code jenis = } nama kelas ini. Tidak ada FK; menghapus baris di sini <b>tidak</b> menghapus
 * lampirannya (lampiran menjadi yatim). Lihat juga catatan keamanan di bawah.</p>
 *
 * <h2>Hubungan dengan {@link GeneralValueObject}</h2>
 *
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa, sehingga Hibernate <b>tidak</b> memetakan properti apa pun yang
 * dideklarasikan di sana. Karena itu {@link #id}, {@link #oleh}, {@link #olehId}, dan
 * {@code tanggal_dirubah} beserta accessor-nya <b>sengaja dideklarasikan ulang</b> di kelas ini.
 * Itu <b>keharusan teknis, bukan duplikasi keliru</b>: tanpa deklarasi ulang, keempat kolom
 * tersebut tidak akan ada di pemetaan tabel ini. Jangan "membersihkannya".</p>
 *
 * <p>Yang tetap diwarisi dan dipakai kelas ini adalah utilitas statis induk, terutama
 * {@link GeneralValueObject#check(Object)} (resolusi proxy lazy tiga tahap: cache in-memory
 * &rarr; session berjalan &rarr; session baru sekali pakai) dan {@code ambilData(...)} yang
 * dipakai {@code ProfileMahasiswa} untuk memuat baris kelas ini dari id di cache JSON.</p>
 *
 * <h2>Catatan pemetaan</h2>
 *
 * <ul>
 *   <li>{@code @org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)}
 *       &mdash; SQL hanya menyertakan kolom yang benar-benar berubah/terisi.</li>
 *   <li>{@code @Audited} (Hibernate Envers) &mdash; setiap perubahan baris direkam ke tabel
 *       riwayat. Penghapusan lewat SQL native (mis. pembersihan anak saat kegiatan induk dihapus)
 *       <b>lolos</b> dari Envers, sama seperti entity lain di paket ini.</li>
 *   <li>Semua relasi {@code @ManyToOne} memakai {@code FetchType.LAZY} +
 *       {@code cascade = { PERSIST, MERGE }} &mdash; tidak ada {@code REMOVE}, jadi menghapus
 *       baris peserta tidak pernah menghapus kegiatan atau mahasiswanya.</li>
 *   <li>Anotasi dipasang pada <b>getter</b> (property access). Semua efek samping yang ditulis
 *       di getter di bawah karena itu ikut terbawa ke nilai yang disimpan.</li>
 * </ul>
 *
 * <h2>Pola berulang yang DIVERIFIKASI pada berkas ini</h2>
 *
 * <ul>
 *   <li><b>Getter yang menulis balik ke field</b>: <b>ADA</b> &mdash;
 *       {@link #getKegiatanKemahasiswaan()}, {@link #getMahasiswa()}, {@link #getTbmuser()},
 *       {@link #getJabatanKegiatanKemahasiswaan()}, {@link #getSkalaKegiatanKemahasiswaan()}
 *       (lewat {@code check()} dan/atau warisan nilai induk), {@link #getPersetujuan()}, serta
 *       {@link #toString()} yang menugaskan ulang field {@link #mahasiswa}.</li>
 *   <li><b>Getter yang menutup session Hibernate</b>: <b>TIDAK ADA</b> secara langsung di berkas
 *       ini. Namun {@link GeneralValueObject#check(Object)} dapat membuka <i>dan</i> menutup satu
 *       session sekali pakai pada tahap terakhirnya, sehingga efek tidak langsung itu tetap ada.</li>
 *   <li><b>Getter destruktif</b>: <b>ADA satu</b> &mdash; {@link #getPersetujuan()} menimpa field
 *       {@code persetujuan} menjadi {@code false} saat kegiatan induk belum {@code DISETUJUI},
 *       dan karena property access nilai itulah yang tersimpan. Persetujuan peserta yang sudah
 *       pernah diberikan bisa hilang permanen bila status kegiatan induk dikembalikan dari
 *       "Disetujui" ke status lain. Selain itu {@link #getTbmuser()} membuang nilainya
 *       (mengembalikan {@code null}) bila pengguna tersimpan adalah akun mahasiswa.</li>
 * </ul>
 *
 * <h2>Catatan keamanan bagi kode pemakai</h2>
 *
 * <ul>
 *   <li>Checkbox "Setujui" di {@code KegiatanKemahasiswaanPunyaMahasiswaHelper} hanya dijaga
 *       dengan {@code tbmuser.getMahasiswa() == null} (bukan mahasiswa) pada saat <i>render</i>;
 *       tidak ada pemeriksaan {@code CommonPrivilages} di dalam listener {@code onCheck}. Setiap
 *       akun non-mahasiswa yang bisa membuka layar kegiatan dapat menyetujui peserta mana pun,
 *       lintas fakultas/prodi.</li>
 *   <li>Berkas bukti kegiatan dijangkau lewat {@code lampiran_lain} dengan {@code ref} = id baris
 *       ini dan {@code jenis} = nama kelas ini; id-nya berurutan. Jalur unduh generik
 *       ({@code ais/action/servlet/AmbilLampiran.java}) sudah tercatat memiliki IDOR terbuka pada
 *       {@code SECURITY_FINDING_AmbilLampiran_IDOR.md} &mdash; tabel ini adalah salah satu sumber
 *       {@code ref} yang terdampak.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota</h2>
 *
 * <ol>
 *   <li><b>Kontrak audit warisan</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()},
 *       {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Pasangan penghubung (wajib)</b> &mdash; {@link #getKegiatanKemahasiswaan()},
 *       {@link #getMahasiswa()}.</li>
 *   <li><b>Muatan per peserta</b> &mdash; {@link #getJabatanKegiatanKemahasiswaan()},
 *       {@link #getSkalaKegiatanKemahasiswaan()}, {@link #getKeterangan()}, {@link #getMulai()},
 *       {@link #getSampai()}.</li>
 *   <li><b>Verifikasi</b> &mdash; {@link #getPersetujuan()}.</li>
 *   <li><b>Jejak asal-usul baris</b> &mdash; {@link #getTbmuser()}, {@link #getDiubahDari()}.</li>
 * </ol>
 *
 * <p><b>Peringatan komentar generator.</b> Komentar hbm2java asli di atas deklarasi kelas ini
 * berbunyi <i>"Bank generated by hbm2java"</i> &mdash; sisa salin-tempel dari entity
 * {@link Bank}, tidak ada hubungannya dengan kepesertaan kegiatan kemahasiswaan. Kekeliruan
 * salin-tempel serupa juga ditemukan pada {@code BuktiPembayaran}, {@code MasaPerkuliahan}, dan
 * {@code GelombangPendaftaranSidangTugasAkhir}. Jangan dijadikan acuan; gunakan javadoc ini.</p>
 *
 * @see KegiatanKemahasiswaan
 * @see Mahasiswa
 * @see JabatanKegiatanKemahasiswaan
 * @see SkalaKegiatanKemahasiswaan
 * @see FormulirKegiatan
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_kemahasiswaan_punya_mahasiswa")

public class KegiatanKemahasiswaanPunyaMahasiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya dihasilkan generator dan tidak boleh diubah: instance kelas
	 * ini ikut diserialisasi ke cache MapDB ({@code DataUtil.CLASS_IZINKAN}) dan ke sesi ZK,
	 * sehingga mengubah nilai ini membuat data cache lama tidak bisa dibaca.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/**
	 * Kunci utama baris ({@code kolom id}, {@code bigserial}). Dideklarasikan ulang di sini karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} &mdash; lihat javadoc kelas.
	 */
	private Long id;

	/** Nama/username pengguna yang terakhir menyimpan baris ini (kolom {@code oleh}). */
	private String oleh;

	/** Identitas (id) pengguna yang terakhir menyimpan baris ini (kolom {@code oleh_id}). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Menolak nilai kosong secara diam-diam:</b> {@code null} maupun string yang hanya berisi
	 * spasi diabaikan (method langsung {@code return}), sehingga nilai lama tetap dipertahankan.
	 * Ini pola audit standar seluruh entity paket ini: jejak yang sudah ada tidak boleh terhapus
	 * oleh proses batch yang tidak punya konteks pengguna.</p>
	 *
	 * @param olehId id pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau kosong diabaikan diam-diam
	 * agar jejak audit lama tidak terhapus.</p>
	 *
	 * @param oleh nama/username pengguna baru; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil Hibernate <b>tepat sebelum</b> baris ini
	 * di-{@code UPDATE}, lalu meneruskan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} untuk memperbarui
	 * {@code tanggal_dirubah} beserta {@link #oleh}/{@link #olehId} dari konteks pengguna yang
	 * sedang aktif.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Tidak ada pasangan
	 * {@code @PrePersist}: pada baris baru, stempel waktu berasal dari inisialisasi field
	 * {@code tanggal_dirubah} ({@code ais.ui.util.WaktuUtil.getDate()}) yang dieksekusi saat
	 * konstruktor berjalan.
	 *
	 * <p><b>Catatan format:</b> deklarasi method ini dan deklarasi field {@code tanggal_dirubah}
	 * (stempel waktu perubahan terakhir, kolom {@code tanggal_dirubah}) sengaja berbagi satu baris
	 * fisik &mdash; pola salin-tempel yang sama ditemukan di ratusan entity paket ini. Jangan
	 * dipecah tanpa alasan; perubahan kosmetik pada baris ini memicu konflik di banyak sesi
	 * paralel.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, method ini <b>tidak</b>
	 * menolak {@code null} &mdash; menyetel {@code null} akan mengosongkan kolomnya. Dalam alur
	 * normal nilai ini diisi otomatis oleh {@link #onUpdate()}, bukan oleh kode pemanggil.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Pada baris yang belum pernah disimpan, nilainya sudah
	 * terisi waktu pembuatan object (inisialisasi field saat konstruktor berjalan), bukan
	 * {@code null}.</p>
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks baris ini dalam bentuk {@code "<kegiatan> - <mahasiswa>"}.
	 *
	 * <p><b>Bukan method murni &mdash; punya dua efek samping yang mudah terlewat:</b></p>
	 *
	 * <ol>
	 *   <li>Memanggil {@link #getMahasiswa()} dan <b>menugaskan ulang</b> hasilnya ke field
	 *       {@link #mahasiswa}. Artinya sekadar mencetak object ini dapat memicu resolusi proxy
	 *       lazi lewat {@link GeneralValueObject#check(Object)} &mdash; termasuk, pada kasus
	 *       terburuk, membuka session Hibernate baru sekali pakai.</li>
	 *   <li>Bagian kegiatan memakai <b>field mentah</b> {@link #kegiatanKemahasiswaan}, bukan
	 *       {@link #getKegiatanKemahasiswaan()}. Field itu tidak melewati {@code check()},
	 *       sehingga pada object yang sudah <i>detached</i> hasilnya bisa berupa teks proxy atau
	 *       memicu {@code LazyInitializationException} &mdash; sementara bagian mahasiswanya
	 *       aman. Ketidakseimbangan ini disengaja atau tidak, yang jelas ia nyata di kode.</li>
	 * </ol>
	 *
	 * <p>Dipakai antara lain sebagai label baris pada layar/kombobox dan pada pesan progres proses
	 * massal.</p>
	 *
	 * @return teks {@code "<kegiatan> - <mahasiswa>"}; kedua bagian bisa berbunyi {@code "null"}
	 *         bila relasinya kosong
	 */
	public String toString() {
		mahasiswa = getMahasiswa();
		return kegiatanKemahasiswaan + " - " + mahasiswa;
	}

	/**
	 * Kegiatan yang diikuti (kolom {@code kegiatan_kemahasiswaan}, <b>wajib</b>). Sisi "kegiatan"
	 * dari pasangan penghubung.
	 */
	private KegiatanKemahasiswaan kegiatanKemahasiswaan;

	/**
	 * Mahasiswa peserta (kolom {@code mahasiswa}, <b>wajib</b>). Sisi "mahasiswa" dari pasangan
	 * penghubung.
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Jejak <b>layar/proses asal</b> baris ini (kolom {@code diubah_dari}), diisi dengan nama kelas
	 * pembuatnya. Nilai yang benar-benar dipakai di kode: {@code "MahasiswaAction"} (dari kedua
	 * helper "AmbilData"), {@code "KegiatanKemahasiswaanAction"} (unggah Excel), dan
	 * {@code "ais.action.master.helper.FormulirKegiatanPesertaHelper oleh <userId>"} (konversi
	 * peserta formulir &mdash; satu-satunya yang memakai nama kelas lengkap dan menempelkan id
	 * pengguna). Karena isinya bebas dan tidak seragam, kolom ini hanya layak dipakai untuk
	 * telusur manual, bukan sebagai kunci pengelompokan.
	 */
	private String diubahDari;

	/**
	 * Akun pengguna yang membuat/mengubah baris ini (kolom {@code tbmuser}, opsional). Perhatikan
	 * penyaringan tidak biasa pada {@link #getTbmuser()}.
	 */
	private Tbmuser tbmuser;

	/**
	 * Peran/jabatan peserta dalam kegiatan (kolom {@code jabatan_kegiatan_kemahasiswaan},
	 * opsional). Bila kosong, {@link #getJabatanKegiatanKemahasiswaan()} mewarisi nilai dari
	 * kegiatan induk.
	 */
	private JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan;

	/**
	 * Skala/tingkat kegiatan yang diakui bagi peserta ini (kolom
	 * {@code skala_kegiatan_kemahasiswaan}, opsional). Bila kosong,
	 * {@link #getSkalaKegiatanKemahasiswaan()} mewarisi nilai dari kegiatan induk.
	 */
	private SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan;

	/**
	 * Catatan bebas peserta (kolom {@code keterangan}, tipe {@code text}). Dapat disunting inline
	 * pada layar peserta selama {@link #getPersetujuan()} belum bernilai {@code true}.
	 */
	private String keterangan;

	/**
	 * Tanggal awal keterlibatan peserta (kolom {@code mulai}, {@code DATE}, opsional). Bila kosong,
	 * {@link #getMulai()} mengembalikan tanggal mulai kegiatan induk.
	 */
	private Date mulai;

	/**
	 * Tanggal akhir keterlibatan peserta (kolom {@code sampai}, {@code DATE}, opsional). Bila
	 * kosong, {@link #getSampai()} mengembalikan tanggal selesai kegiatan induk.
	 */
	private Date sampai;

	/**
	 * Penanda peserta sudah diverifikasi petugas (kolom {@code persetujuan}, opsional/tri-state di
	 * DB: {@code null}/{@code false}/{@code true}). Baca lewat {@link #getPersetujuan()} yang
	 * meratakan {@code null} menjadi {@code false} <i>dan</i> punya efek samping.
	 */
	private Boolean persetujuan;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Tidak mengisi apa pun kecuali {@code tanggal_dirubah}, yang terisi otomatis oleh
	 * inisialisasi field. Seluruh pemanggil di repo mengikuti pola yang sama setelah
	 * {@code new}: menyetel {@link #setKegiatanKemahasiswaan(KegiatanKemahasiswaan)} dan
	 * {@link #setMahasiswa(Mahasiswa)} (keduanya {@code NOT NULL}) sebelum menyimpan.</p>
	 */
	public KegiatanKemahasiswaanPunyaMahasiswa() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan {@code IDENTITY} ({@code bigserial} PostgreSQL) dengan
	 * {@code insertable = false} &mdash; nilainya selalu ditentukan database, bukan aplikasi;
	 * menyetelnya sendiri sebelum {@code save()} tidak berpengaruh pada {@code INSERT}.</p>
	 *
	 * <p>Selain sebagai kunci, id ini dipakai sebagai {@code ref} lampiran bukti kegiatan di tabel
	 * {@code lampiran_lain} dan sebagai kunci pada cache JSON kegiatan milik {@link Mahasiswa}.
	 * Karena itu id baris ini bocor ke URL unduh berkas &mdash; lihat catatan keamanan pada
	 * javadoc kelas.</p>
	 *
	 * @return id baris, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama baris ini.
	 *
	 * <p>Dalam alur normal hanya dipanggil Hibernate. Karena kolomnya {@code insertable = false},
	 * memanggilnya manual pada baris baru tidak akan memaksakan id tertentu.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kegiatan yang diikuti peserta ini.
	 *
	 * <p><b>Bukan getter murni:</b> hasil {@link GeneralValueObject#check(Object)} ditugaskan
	 * kembali ke field. {@code check()} memulihkan proxy lazi yang sudah <i>detached</i> lewat
	 * tiga tahap (cache in-memory &rarr; session yang sedang berjalan &rarr; session baru sekali
	 * pakai yang langsung ditutup lagi). Pada kasus umum operasinya murah, tetapi pada object yang
	 * datang dari cache MapDB ia bisa memicu satu query tambahan.</p>
	 *
	 * <p>Kolomnya {@code NOT NULL}, jadi pada baris yang sudah tersimpan nilainya tidak pernah
	 * {@code null} kecuali barisnya baru dibuat dan belum diisi.</p>
	 *
	 * @return kegiatan induk baris ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kegiatan_kemahasiswaan", nullable = false)
	public KegiatanKemahasiswaan getKegiatanKemahasiswaan() {
		kegiatanKemahasiswaan = check(kegiatanKemahasiswaan);
		return kegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan kegiatan yang diikuti peserta ini.
	 *
	 * <p>Wajib diisi sebelum menyimpan ({@code NOT NULL}). {@code cascade = { PERSIST, MERGE }}
	 * berarti menyimpan baris ini ikut mem-persist/merge kegiatan yang belum tersimpan, tetapi
	 * <b>tidak</b> pernah menghapusnya.</p>
	 *
	 * @param kegiatanKemahasiswaan kegiatan induk
	 */
	public void setKegiatanKemahasiswaan(KegiatanKemahasiswaan kegiatanKemahasiswaan) {
		this.kegiatanKemahasiswaan = kegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan mahasiswa peserta.
	 *
	 * <p><b>Bukan getter murni:</b> sama seperti {@link #getKegiatanKemahasiswaan()}, hasil
	 * {@link GeneralValueObject#check(Object)} ditugaskan kembali ke field sehingga proxy lazi
	 * yang sudah <i>detached</i> ikut dipulihkan.</p>
	 *
	 * <p>Kolomnya {@code NOT NULL}. Nilai inilah yang dipakai {@code AuditListener} untuk
	 * memutakhirkan cache JSON kegiatan milik mahasiswa setiap kali baris ini
	 * disimpan/dihapus.</p>
	 *
	 * @return mahasiswa peserta
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = false)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/**
	 * Menetapkan mahasiswa peserta.
	 *
	 * <p>Wajib diisi sebelum menyimpan ({@code NOT NULL}). Memindahkan baris ke mahasiswa lain
	 * berarti memindahkan pula kepemilikan bukti kegiatan dan angka kreditnya; cache JSON kedua
	 * mahasiswa baru sinkron setelah {@code AuditListener} berjalan pada penyimpanan berikutnya
	 * (dan cache pemilik lama tidak dibersihkan pada perubahan ini).</p>
	 *
	 * @param mahasiswa mahasiswa peserta
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * Mengembalikan akun pengguna yang tercatat membuat/mengubah baris ini &mdash;
	 * <b>dengan penyaringan yang menghilangkan data</b>.
	 *
	 * <p>Perilakunya dua langkah:</p>
	 * <ol>
	 *   <li>field dipulihkan lewat {@link GeneralValueObject#check(Object)} dan
	 *       <b>ditugaskan kembali</b> ke field (getter tidak murni);</li>
	 *   <li>bila pengguna tersimpan ternyata <b>akun mahasiswa</b>
	 *       ({@code tbmuser.getMahasiswa() != null}), method mengembalikan {@code null} alih-alih
	 *       object tersebut.</li>
	 * </ol>
	 *
	 * <p><b>Konsekuensi yang mudah terlewat:</b> karena entity ini memakai property access,
	 * nilai yang dikembalikan getter inilah yang ditulis Hibernate ke kolom {@code tbmuser}.
	 * Jadi begitu baris yang dibuat oleh seorang <i>mahasiswa</i> (mis. lewat layar pendaftaran
	 * mandiri di {@code AmbilDataKegiatanForKegiatanKemahasiswaanHelper}, yang memang menyetel
	 * {@code setTbmuser(Common.getCurrentUser())}) tersimpan/diperbarui, kolom {@code tbmuser}
	 * menjadi <b>NULL</b> di database. Efektifnya kolom ini hanya pernah menyimpan pengguna
	 * non-mahasiswa; jejak pendaftaran mandiri mahasiswa hanya tersisa di {@link #getOleh()}/
	 * {@link #getOlehId()} dan {@link #getDiubahDari()}. Jangan memakai kolom ini untuk
	 * membuktikan siapa yang mendaftarkan seorang mahasiswa.</p>
	 *
	 * @return akun pengguna pencatat, atau {@code null} bila kosong <i>atau</i> bila akun tersebut
	 *         merupakan akun mahasiswa
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = true)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser != null && tbmuser.getMahasiswa() != null ? null : tbmuser;
	}

	/**
	 * Menetapkan akun pengguna pencatat baris ini.
	 *
	 * <p>Seluruh pemanggil di repo mengisinya dengan {@code Common.getCurrentUser()} pada saat
	 * baris dibuat. Nilai yang disetel <b>tidak selalu bertahan</b>: lihat penyaringan pada
	 * {@link #getTbmuser()}.</p>
	 *
	 * @param tbmuser akun pengguna pencatat
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengembalikan penanda layar/proses asal baris ini.
	 *
	 * <p>Getter murni tanpa efek samping. Nilai yang dipakai di kode dijelaskan pada field
	 * {@link #diubahDari}.</p>
	 *
	 * @return nama kelas layar/proses pembuat baris, atau {@code null} bila baris dibuat oleh
	 *         jalur yang tidak mengisinya (mis. konversi lama atau impor langsung)
	 */
	public String getDiubahDari() {
		return diubahDari;
	}

	/**
	 * Menetapkan penanda layar/proses asal baris ini.
	 *
	 * <p>Tidak ada validasi format maupun daftar nilai yang sah &mdash; kolomnya teks bebas.</p>
	 *
	 * @param diubahDari nama kelas layar/proses pembuat baris
	 */
	public void setDiubahDari(String diubahDari) {
		this.diubahDari = diubahDari;
	}

	/**
	 * Mengembalikan jabatan/peran peserta dalam kegiatan, <b>dengan pewarisan dari kegiatan
	 * induk</b>.
	 *
	 * <p>Urutan kerjanya:</p>
	 * <ol>
	 *   <li>bila field {@link #kegiatanKemahasiswaan} terisi <i>dan</i> jabatan peserta masih
	 *       {@code null}, jabatan diambil dari
	 *       {@link KegiatanKemahasiswaan#getJabatanKegiatanKemahasiswaan()} lalu
	 *       <b>disimpan ke field</b>;</li>
	 *   <li>hasilnya dilewatkan {@link GeneralValueObject#check(Object)} dan ditugaskan kembali ke
	 *       field.</li>
	 * </ol>
	 *
	 * <p><b>Efek samping yang penting:</b> karena pemetaan memakai property access, nilai warisan
	 * itu ikut ditulis ke kolom {@code jabatan_kegiatan_kemahasiswaan} pada penyimpanan
	 * berikutnya. Setelah itu baris tidak lagi mengikuti perubahan jabatan di kegiatan induk.</p>
	 *
	 * <p>Perhatikan pula bahwa langkah 1 membaca <b>field mentah</b>
	 * {@link #kegiatanKemahasiswaan}, bukan {@link #getKegiatanKemahasiswaan()}. Bila field itu
	 * masih {@code null} (mis. object hasil {@code new} yang kegiatannya belum disetel), pewarisan
	 * dilewati diam-diam dan method mengembalikan {@code null}.</p>
	 *
	 * @return jabatan peserta; nilai kegiatan induk bila jabatan per peserta belum diisi;
	 *         {@code null} bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kemahasiswaan", nullable = true)
	public JabatanKegiatanKemahasiswaan getJabatanKegiatanKemahasiswaan() {
		if (kegiatanKemahasiswaan != null && jabatanKegiatanKemahasiswaan == null) {
			jabatanKegiatanKemahasiswaan = kegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaan();
		}
		jabatanKegiatanKemahasiswaan = check(jabatanKegiatanKemahasiswaan);
		return jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan jabatan/peran peserta dalam kegiatan.
	 *
	 * <p>Disetel dari kombobox "Jabatan" pada layar peserta (tersimpan langsung lewat
	 * {@code onChange}, tanpa tombol simpan) dan dari kolom jabatan pada unggah Excel. Menyetel
	 * {@code null} akan membuat {@link #getJabatanKegiatanKemahasiswaan()} kembali mewarisi nilai
	 * kegiatan induk pada pembacaan berikutnya.</p>
	 *
	 * @param jabatanKegiatanKemahasiswaan jabatan peserta, boleh {@code null}
	 */
	public void setJabatanKegiatanKemahasiswaan(JabatanKegiatanKemahasiswaan jabatanKegiatanKemahasiswaan) {
		this.jabatanKegiatanKemahasiswaan = jabatanKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan catatan bebas peserta.
	 *
	 * <p>Getter murni. Dipetakan ke kolom bertipe {@code text} (tanpa batas panjang), diisi dari
	 * textbox dua baris pada layar peserta.</p>
	 *
	 * @return keterangan peserta, atau {@code null} bila belum diisi
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan catatan bebas peserta.
	 *
	 * <p>Tidak ada penyaringan/sanitasi di sini (bandingkan dengan
	 * {@code GeneralValueObject.filterTidakBoleh(...)} yang dipakai sebagian entity lain).</p>
	 *
	 * @param keterangan catatan peserta, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan status verifikasi peserta, <b>diratakan menjadi {@code true}/{@code false}</b>.
	 *
	 * <p>Ada dua hal yang terjadi di sini:</p>
	 * <ol>
	 *   <li><b>Penjagaan tingkat kegiatan.</b> Bila field {@link #kegiatanKemahasiswaan} terisi dan
	 *       status kegiatan induk <b>bukan</b> {@link PrestasiMahasiswa#DISETUJUI}, field
	 *       {@code persetujuan} <b>ditimpa</b> menjadi {@code false}. Peserta tidak boleh berstatus
	 *       disetujui selama kegiatannya sendiri belum disetujui.</li>
	 *   <li><b>Perataan null.</b> Nilai {@code null} dikembalikan sebagai {@code false}, sehingga
	 *       pemanggil dapat langsung melakukan auto-unboxing tanpa risiko NPE.</li>
	 * </ol>
	 *
	 * <p><b>PERINGATAN &mdash; getter destruktif.</b> Langkah 1 bukan sekadar penyesuaian nilai
	 * kembalian: ia menulis ke field, dan karena entity ini memakai property access, {@code false}
	 * itulah yang tersimpan ke kolom pada {@code UPDATE} berikutnya. Bila status kegiatan induk
	 * pernah dikembalikan dari "Disetujui" ke status lain, persetujuan seluruh pesertanya
	 * <b>hilang permanen</b> dan harus dicentang ulang satu per satu, meski status kegiatan
	 * dikembalikan lagi ke "Disetujui".</p>
	 *
	 * <p>Perhatikan pula bahwa langkah 1 memakai <b>field mentah</b>, bukan
	 * {@link #getKegiatanKemahasiswaan()}, sehingga tidak melewati
	 * {@link GeneralValueObject#check(Object)}: pada object yang sudah <i>detached</i>, pemanggilan
	 * {@code getStatus()} di sini dapat melempar {@code LazyInitializationException} &mdash;
	 * berbeda dari getter relasi lain di kelas ini yang terlindungi {@code check()}. Nilai
	 * {@code getStatus()} sendiri tidak pernah {@code null} (kelas induk meratakannya menjadi
	 * "Belum diproses").</p>
	 *
	 * <p>Pembaca utama: layar peserta (mengunci field editable, menyembunyikan tombol Hapus,
	 * memunculkan tombol Sertifikat) dan dasbor rekap (menghitung peserta yang sudah
	 * diverifikasi).</p>
	 *
	 * @return {@code true} bila peserta sudah diverifikasi <i>dan</i> kegiatannya sudah disetujui;
	 *         {@code false} selain itu (termasuk saat kolomnya masih {@code null})
	 */
	public Boolean getPersetujuan() {
		if (kegiatanKemahasiswaan != null && !kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI)) {
			persetujuan = false;
		}
		return persetujuan == null ? false : persetujuan;
	}

	/**
	 * Menetapkan status verifikasi peserta.
	 *
	 * <p>Disetel dari tiga tempat: checkbox "Setujui" per baris pada layar peserta (hanya dirender
	 * untuk pengguna non-mahasiswa dan hanya bila kegiatan induk sudah disetujui), penyetujuan
	 * massal saat sebuah kegiatan diubah statusnya menjadi {@code DISETUJUI} di
	 * {@code KegiatanKemahasiswaanAction}, dan konversi peserta formulir kegiatan di
	 * {@code FormulirKegiatanPesertaHelper} yang langsung menyetel {@code true}. Unggah Excel juga
	 * dapat mengisinya dari kolom persetujuan pada berkas.</p>
	 *
	 * <p>Nilai yang disetel di sini bisa dibatalkan lagi oleh {@link #getPersetujuan()} bila status
	 * kegiatan induk belum {@code DISETUJUI}.</p>
	 *
	 * @param persetujuan status verifikasi baru; {@code null} diperlakukan sama dengan
	 *        {@code false} saat dibaca
	 */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Mengembalikan skala/tingkat kegiatan yang diakui bagi peserta ini, <b>dengan pewarisan dari
	 * kegiatan induk</b>.
	 *
	 * <p>Mekanismenya persis sama dengan {@link #getJabatanKegiatanKemahasiswaan()}: bila field
	 * {@link #kegiatanKemahasiswaan} terisi dan skala peserta masih {@code null}, skala kegiatan
	 * induk disalin <b>ke field</b>, lalu hasilnya dilewatkan
	 * {@link GeneralValueObject#check(Object)} dan ditugaskan kembali ke field. Karena property
	 * access, nilai warisan itu ikut tersimpan ke kolom pada penyimpanan berikutnya dan sejak itu
	 * tidak lagi mengikuti kegiatan induk.</p>
	 *
	 * <p>Bersama {@link #getJabatanKegiatanKemahasiswaan()}, nilai inilah yang menentukan bobot
	 * angka kredit peserta pada rubrik {@code NilaiKegiatanKemahasiswaan}.</p>
	 *
	 * @return skala peserta; nilai kegiatan induk bila skala per peserta belum diisi; {@code null}
	 *         bila keduanya kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kemahasiswaan", nullable = true)
	public SkalaKegiatanKemahasiswaan getSkalaKegiatanKemahasiswaan() {
		if (kegiatanKemahasiswaan != null && skalaKegiatanKemahasiswaan == null) {
			skalaKegiatanKemahasiswaan = kegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaan();
		}
		skalaKegiatanKemahasiswaan = check(skalaKegiatanKemahasiswaan);
		return skalaKegiatanKemahasiswaan;
	}

	/**
	 * Menetapkan skala/tingkat kegiatan bagi peserta ini.
	 *
	 * <p>Disetel dari kombobox "Skala" pada layar peserta (tersimpan langsung lewat
	 * {@code onChange}). Menyetel {@code null} membuat {@link #getSkalaKegiatanKemahasiswaan()}
	 * kembali mewarisi nilai kegiatan induk pada pembacaan berikutnya.</p>
	 *
	 * @param skalaKegiatanKemahasiswaan skala peserta, boleh {@code null}
	 */
	public void setSkalaKegiatanKemahasiswaan(SkalaKegiatanKemahasiswaan skalaKegiatanKemahasiswaan) {
		this.skalaKegiatanKemahasiswaan = skalaKegiatanKemahasiswaan;
	}

	/**
	 * Mengembalikan tanggal awal keterlibatan peserta, dengan <b>pewarisan dari kegiatan
	 * induk</b>.
	 *
	 * <p>Bila kolom {@code mulai} kosong, nilai diambil dari
	 * {@link KegiatanKemahasiswaan#getMulai()} melalui <b>field mentah</b>
	 * {@link #kegiatanKemahasiswaan} (bukan lewat {@code check()}); bila field itu pun
	 * {@code null}, hasilnya {@code null}.</p>
	 *
	 * <p>Berbeda dari {@link #getJabatanKegiatanKemahasiswaan()}/
	 * {@link #getSkalaKegiatanKemahasiswaan()}, nilai warisan di sini <b>tidak</b> ditugaskan ke
	 * field &mdash; jadi getter ini murni terhadap state object. Namun karena property access,
	 * nilai yang dikembalikanlah yang ditulis Hibernate ke kolom, sehingga tanggal kegiatan induk
	 * tetap berakhir tersalin permanen ke kolom {@code mulai} pada {@code UPDATE} berikutnya.</p>
	 *
	 * <p>Dipetakan {@code DATE} (tanpa komponen jam).</p>
	 *
	 * @return tanggal mulai peserta; tanggal mulai kegiatan induk bila belum diisi; {@code null}
	 *         bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? (kegiatanKemahasiswaan == null ? null : kegiatanKemahasiswaan.getMulai()) : mulai;
	}

	/**
	 * Menetapkan tanggal awal keterlibatan peserta.
	 *
	 * <p>Disetel dari datebox "Mulai" pada layar peserta, dari kolom tanggal pada unggah Excel, dan
	 * dari {@code FormulirKegiatan.getMulai()} saat peserta formulir dikonversi. Tidak ada validasi
	 * bahwa {@code mulai} mendahului {@link #getSampai()}, dan tidak ada validasi bahwa rentang
	 * peserta berada di dalam rentang kegiatan induk.</p>
	 *
	 * @param mulai tanggal mulai peserta, boleh {@code null} (berarti "ikut kegiatan induk")
	 */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/**
	 * Menetapkan tanggal akhir keterlibatan peserta.
	 *
	 * <p>Pasangan {@link #setMulai(Date)}; sumber pengisian dan ketiadaan validasinya sama.</p>
	 *
	 * @param sampai tanggal selesai peserta, boleh {@code null} (berarti "ikut kegiatan induk")
	 */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * Mengembalikan tanggal akhir keterlibatan peserta, dengan <b>pewarisan dari kegiatan
	 * induk</b>.
	 *
	 * <p>Cermin dari {@link #getMulai()}: bila kolom {@code sampai} kosong, nilai diambil dari
	 * {@link KegiatanKemahasiswaan#getSampai()} lewat field mentah {@link #kegiatanKemahasiswaan},
	 * tanpa ditugaskan ke field, namun tetap berakhir tersimpan ke kolom pada penyimpanan
	 * berikutnya karena pemetaan memakai property access.</p>
	 *
	 * <p>Dipetakan {@code DATE} (tanpa komponen jam). Perhatikan urutan deklarasi di berkas ini:
	 * {@code setSampai} ditulis <i>sebelum</i> {@code getSampai}, kebalikan dari pasangan
	 * {@code mulai} &mdash; murni kosmetik, tidak berpengaruh pada pemetaan.</p>
	 *
	 * @return tanggal selesai peserta; tanggal selesai kegiatan induk bila belum diisi;
	 *         {@code null} bila keduanya kosong
	 */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? (kegiatanKemahasiswaan == null ? null : kegiatanKemahasiswaan.getSampai()) : sampai;
	}

}
