package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity penghubung <b>Kurikulum &harr; Jenis Nilai</b> untuk modul Sekolah: satu baris memasangkan
 * sebuah mata pelajaran kurikulum ({@link KurikulumPunyaMatapelajaran}) dengan sebuah butir
 * konfigurasi penilaian ({@link ais.database.model.sekolah.DetailJenisPenilaian}) dan menyimpan
 * satu angka {@link #getNilai() nilai} untuk pasangan itu &mdash; secara struktur sebuah tabel
 * bobot/nilai per (kurikulum-mapel, jenis penilaian).
 *
 * <p>Dipetakan ke tabel fisik <b>{@code sekolah.kurikulum_punya_jenis_nilai}</b>, di-audit Envers
 * ({@code @Audited}) dan memakai {@code dynamicInsert}/{@code dynamicUpdate}. Mewarisi
 * {@link ais.database.model.GeneralValueObject}.</p>
 *
 * <h2>PERINGATAN UTAMA: entity ini MATI (terverifikasi)</h2>
 *
 * <p>Pembacaan menyeluruh atas seluruh repositori (sumber Java, {@code .zul}, {@code .jsp},
 * {@code .js}, {@code .xml}) mengonfirmasi bahwa kelas ini <b>tidak dipakai oleh jalur eksekusi
 * mana pun</b>. Rinciannya:</p>
 *
 * <ul>
 *   <li><b>Nol penulis.</b> Tidak ada satu pun {@code new KurikulumPunyaJenisNilai()} di seluruh
 *       repositori, dan tidak ada {@code session.save}/{@code persist}/{@code merge} atas kelas
 *       ini. Aplikasi tidak pernah membuat baris tabel ini.</li>
 *   <li><b>Nol layar.</b> Tidak ada {@code Action}, {@code Helper}, {@code Window}, berkas
 *       {@code .zul} maupun {@code .jsp} yang menyebut kelas ini. Tidak ada entri menu.</li>
 *   <li><b>Nol pemanggil getter/setter.</b> Baik {@link #getDetailJenisPenilaian()},
 *       {@link #setDetailJenisPenilaian(DetailJenisPenilaian)},
 *       {@link #getKurikulumPunyaMatapelajaran()},
 *       {@link #setKurikulumPunyaMatapelajaran(KurikulumPunyaMatapelajaran)},
 *       {@link #getNilai()} maupun {@link #setNilai(Double)} tidak pernah dipanggil dari kode
 *       aplikasi. Satu-satunya pemanggil adalah Hibernate sendiri (akses properti berbasis
 *       getter).</li>
 *   <li><b>Satu-satunya rujukan yang tersisa mati tiga kali lipat.</b> Lihat bagian berikut.</li>
 * </ul>
 *
 * <h2>Verifikasi klaim "relasi yatim" dari {@code DetailJenisPenilaian}</h2>
 *
 * <p>Javadoc {@link ais.database.model.sekolah.DetailJenisPenilaian} menyatakan bahwa FK masuk
 * {@code KurikulumPunyaJenisNilai.detail_jenis_penilaian_id} "tidak pernah disetel maupun dibaca
 * dari kode aplikasi &mdash; relasi yatim". Diperiksa ulang dari sisi kelas ini:
 * <b>klaim itu BENAR</b>, dengan satu koreksi/penambahan penting yang layak dicatat.</p>
 *
 * <p>Ada tepat <b>satu</b> tempat di seluruh repositori yang menyebut kelas ini di dalam kode
 * eksekusi: {@code JenisPenilaian.hitungNilaiBerdasarkanDetailGrupPenilaian(JadwalPelajaran,
 * DetailGrupPenilaian)}. Di sana sebuah {@code Criteria} mencari satu baris
 * {@code KurikulumPunyaJenisNilai} untuk memutuskan apakah tombol toolbar "Masukkan nilai ke
 * &lt;nama grup&gt;" ditampilkan pada jendela pratinjau Excel. Rujukan itu mati pada
 * <b>tiga</b> lapis sekaligus:</p>
 *
 * <ol>
 *   <li><b>Method pembungkusnya sendiri tidak pernah dipanggil.</b>
 *       {@code hitungNilaiBerdasarkanDetailGrupPenilaian(...)} adalah {@code public static}, tetapi
 *       pencarian seluruh repositori (Java + {@code .zul} + {@code .jsp}) hanya menemukan
 *       deklarasinya, nol pemanggil.</li>
 *   <li><b>Criteria-nya akan melempar eksepsi seandainya dijalankan.</b> Query itu memfilter
 *       {@code Restrictions.eq("detailGrupPenilaian", detailGrupPenilaian)}, padahal kelas ini
 *       <b>tidak memiliki properti bernama {@code detailGrupPenilaian}</b> &mdash; yang ada
 *       hanyalah {@code detailJenisPenilaian} dan {@code kurikulumPunyaMatapelajaran}. Hibernate
 *       akan gagal me-resolve properti tersebut saat {@code uniqueResult()} dieksekusi. (Properti
 *       {@code detailGrupPenilaian} memang ada, tetapi pada kelas lain:
 *       {@code ais.database.model.PertemuanPunyaUjian}. Tampaknya salah-salin.)</li>
 *   <li><b>Tombol yang dihasilkannya pun tidak melakukan apa pun.</b> Isi
 *       {@code EventListener.onEvent} tombol "Masukkan nilai ke ..." dikomentari 100% di
 *       {@code JenisPenilaian}; badan method kosong.</li>
 * </ol>
 *
 * <p>Efek samping menarik dari poin (2): eksepsi itu terjadi <i>sebelum</i>
 * {@code window.setVisible(true)} dan {@code timer.detach()} dipanggil, dan seluruh blok berada di
 * dalam {@code catch (Exception e) { Clients.clearBusy(); }}. Artinya, jika method itu suatu hari
 * dihidupkan kembali, jendela pratinjau tidak akan pernah tampil dan {@code Timer} 200&nbsp;ms
 * yang berulang tidak akan pernah dilepas &mdash; gejalanya "layar diam saja lalu makin berat",
 * bukan pesan kesalahan. Catat ini sebelum siapa pun menghidupkan fitur tersebut.</p>
 *
 * <h2>Tapi barisnya ADA di basis data produksi</h2>
 *
 * <p>Meski aplikasi tidak pernah menulis tabel ini, riwayat SVN membuktikan barisnya nyata:
 * revisi r77038 (8 Agu 2026, pesan <i>"Tolerate orphan school grading relation in Generic CRUD
 * reads"</i>) menambahkan {@code @NotFound(action = NotFoundAction.IGNORE)} pada
 * {@link #getDetailJenisPenilaian()}. Anotasi itu hanya diperlukan bila ada baris nyata yang
 * kolom {@code detail_jenis_penilaian_id}-nya menunjuk baris {@code detail_jenis_penilaian} yang
 * sudah tidak ada. Jadi: tabel ini berisi <b>data warisan</b> dari penulis lama yang sudah dibuang
 * dari kode (atau dari migrasi/skrip di luar repositori), dan sebagian FK-nya sudah menggantung.</p>
 *
 * <p><b>Perbaikan r77038 hanya menambal SATU dari DUA sisi.</b>
 * {@link #getKurikulumPunyaMatapelajaran()} <b>tidak</b> memperoleh {@code @NotFound(IGNORE)}.
 * Bila kolom {@code kurikulum_punya_matapelajaran_id} yang menggantung, pembacaan baris ini tetap
 * akan melempar {@code ObjectNotFoundException} persis seperti sebelum r77038. Asimetri ini
 * kemungkinan besar tidak disengaja.</p>
 *
 * <h2>Posisi dalam rantai kurikulum &amp; penilaian</h2>
 *
 * <pre>
 *   KurikulumSekolah                    (master kurikulum satu sekolah; punya sekolah_id/yayasan_id)
 *        &darr;  KurikulumPunyaMatapelajaran
 *                                       (kolom: kurikulum_sekolah_id, matapelajaran_id,
 *                                        jumlahJamPelajaran, aktif, keterangan)
 *        &darr;  <b>KurikulumPunyaJenisNilai &mdash; KELAS INI</b>
 *                                       (kolom: kurikulum_punya_matapelajaran_id,
 *                                        detail_jenis_penilaian_id, nilai)
 *        &rarr;  DetailJenisPenilaian    (simpul TERTINGGI rantai penilaian:
 *                                        JenisPenilaian &rarr; DetailJenisPenilaian &rarr;
 *                                        GrupPenilaian &rarr; ... &rarr; JenisItemPenilaianSiswa)
 * </pre>
 *
 * <p>Perhatikan bahwa kaitan ke {@link ais.database.model.sekolah.KurikulumSekolah} bersifat
 * <b>tidak langsung</b>: kelas ini tidak punya kolom {@code kurikulum_sekolah_id}. Kurikulum
 * dicapai lewat {@code getKurikulumPunyaMatapelajaran().getKurikulumSekolah()}. Nama kelas
 * ("KurikulumPunya...") karena itu sedikit menyesatkan &mdash; induk langsungnya adalah baris
 * <i>mata pelajaran dalam kurikulum</i>, bukan kurikulumnya.</p>
 *
 * <p>Maksud rancangan yang tersirat dari ketiga kolom: memungkinkan setiap mata pelajaran dalam
 * sebuah kurikulum memiliki <b>bobot/angka sendiri</b> per butir jenis penilaian (mis. bobot
 * Ulangan Harian 30, UTS 30, UAS 40 untuk Matematika, tetapi komposisi berbeda untuk Penjaskes).
 * Rancangan itu tidak pernah selesai diimplementasikan; pembobotan yang benar-benar dipakai
 * runtime ada pada formula {@code GrupPenilaian}/{@code GrupKategoriItemPenilaianSiswa}.</p>
 *
 * <h2>Bentuk relasi</h2>
 *
 * <ul>
 *   <li>Kedua relasi {@code @ManyToOne} memakai {@code @Fetch(FetchMode.SELECT)} dan
 *       <b>tidak</b> menyatakan {@code fetch = FetchType.LAZY}, sehingga default JPA berlaku:
 *       {@code EAGER}. Memuat satu baris entity ini otomatis menembakkan dua SELECT tambahan.</li>
 *   <li>{@code cascade = {PERSIST, MERGE}} pada kedua sisi: menyimpan baris ini dapat ikut
 *       menyimpan {@code DetailJenisPenilaian}/{@code KurikulumPunyaMatapelajaran} di ujungnya.
 *       Tidak ada {@code REMOVE} &mdash; menghapus baris ini tidak pernah menghapus master.</li>
 *   <li>Kedua {@code @JoinColumn} <b>nullable</b> (tidak ada {@code nullable = false}), sehingga
 *       baris "setengah jadi" mungkin secara skema.</li>
 *   <li>Tidak ada koleksi balik ({@code @OneToMany}) di {@link KurikulumPunyaMatapelajaran} maupun
 *       di {@link ais.database.model.sekolah.DetailJenisPenilaian}. Navigasi hanya mungkin lewat
 *       {@code Criteria} eksplisit &mdash; dan satu-satunya {@code Criteria} yang pernah ditulis
 *       adalah yang rusak di atas.</li>
 *   <li>Tidak ada {@code unique constraint} pada pasangan
 *       {@code (kurikulum_punya_matapelajaran_id, detail_jenis_penilaian_id)}, sehingga duplikat
 *       tidak tercegah di level skema.</li>
 *   <li>Entity ini <b>tidak punya kolom tenant sama sekali</b> ({@code sekolah_id}/{@code
 *       yayasan_id} tidak ada). Cakupan tenant sepenuhnya diwarisi lewat rantai
 *       {@code kurikulumPunyaMatapelajaran &rarr; kurikulumSekolah &rarr; sekolah}.</li>
 * </ul>
 *
 * <h2>Risiko laten: terdaftar di manifest Generic CRUD</h2>
 *
 * <p>Kelas ini terdaftar di {@code WEB-INF/generic-crud/manifests/general_value_object_inventory.csv}
 * (dan {@code .json} pasangannya) dengan status
 * {@code ELIGIBLE_METADATA_FIRST} / <i>"Kandidat CRUD metadata; tetap default disabled sampai
 * verifikasi Hibernate/menu/scope"</i> dan label tampilan "Kurikulum Punya Jenis Nilai".
 * Selama tetap <i>disabled</i> hal ini tidak berbahaya, tetapi bila layar CRUD generik untuk
 * entity ini pernah diaktifkan tanpa penyaring tambahan, konsekuensinya perlu diketahui lebih
 * dulu:</p>
 *
 * <ul>
 *   <li>Karena entity ini tidak punya kolom tenant sendiri (lihat di atas), penyaring cakupan
 *       standar berbasis {@code sekolah_id}/{@code yayasan_id} <b>tidak bisa diterapkan langsung</b>
 *       &mdash; daftar barisnya akan lintas sekolah/yayasan kecuali penyaringnya sengaja ditulis
 *       menyeberangi dua tabel. Ini persis kategori "fail-open cakupan tenant" yang berulang di
 *       modul lain, hanya saja di sini masih berupa <b>bom waktu</b>, bukan celah aktif.</li>
 *   <li>Pengguna akan mengira sedang mengelola konfigurasi penilaian kurikulum yang berlaku,
 *       padahal tidak ada satu pun pembaca runtime atas baris ini &mdash; setiap suntingan tidak
 *       berefek apa pun pada rapor/rekap/API. Bandingkan pola serupa pada
 *       {@code KompetensiDasarMatapelajaran} (batch 53).</li>
 * </ul>
 *
 * <h2>Pemeriksaan pola berulang (hasil: sebagian besar TIDAK berlaku)</h2>
 *
 * <ul>
 *   <li><b>Getter write-back/destruktif</b> ({@code sekolah = check(sekolah)} dsb.):
 *       <b>TIDAK ADA</b>. Semua getter di kelas ini murni membaca field. {@link #getNilai()}
 *       memang menormalkan {@code null} menjadi {@code 0.0}, tetapi <b>tidak</b> menugaskan
 *       kembali ke field &mdash; jadi tidak ada penulisan diam-diam ke basis data. Bandingkan
 *       {@code JenisPenilaian.getSekolah()} yang justru menulis balik.</li>
 *   <li><b>{@code getNomorUrut()} non-{@code null} yang meruntuhkan {@code TreeSet}</b> (pola baru
 *       batch 55): <b>TIDAK BERLAKU</b>. Kelas ini tidak punya {@code nomorUrut}, tidak
 *       meng-override {@code compareTo()}, dan tidak pernah dimasukkan ke koleksi terurut mana
 *       pun.</li>
 *   <li><b>Bug "{@code aktif} tak pernah ditulis"</b>: <b>TIDAK BERLAKU</b>. Kelas ini tidak punya
 *       field {@code aktif} sama sekali; tidak ada saklar aktif/nonaktif per baris.</li>
 *   <li><b>Pewarisan hak lewat menu induk</b>: <b>TIDAK BERLAKU</b> &mdash; tidak ada layar
 *       {@code .zul} maupun entri menu untuk entity ini, sehingga tidak ada gerbang hak akses yang
 *       bisa diwarisi maupun dilewati. Verifikasi negatif (menenangkan).</li>
 *   <li><b>Bom waktu {@code aktif} timing 50&nbsp;ms</b> (batch 51/54/55): <b>TIDAK BERLAKU</b>
 *       &mdash; entity ini tidak disentuh {@code onSave()} mana pun.</li>
 * </ul>
 *
 * <h2>Catatan lain</h2>
 *
 * <ul>
 *   <li>Komentar kelas lama berbunyi <i>"JenisGuru generated by hbm2java"</i> &mdash; komentar
 *       generator <b>palsu</b>, hasil salin-tempel dari {@code JenisGuru.java} (sumber asli
 *       dikonfirmasi pada batch 51) dan tidak ada hubungannya dengan kelas ini. Komentar itu
 *       digantikan Javadoc ini. Stempel generator yang sah tetap dipertahankan pada baris kedua
 *       berkas ({@code // Generated 10 Okt 18 ... by Hibernate Tools 5.2.3.Final}).</li>
 *   <li>Nilai {@link #serialVersionUID} ({@code -7490758846785025664L}) dipakai bersama oleh
 *       puluhan entity lain di repositori ini &mdash; boilerplate salin-tempel, <b>bukan</b>
 *       penanda kelas klon seperti pada temuan batch 53.</li>
 *   <li>Field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}/{@code id} sengaja
 *       <b>dideklarasikan ulang</b> di sini meski juga ada di
 *       {@link ais.database.model.GeneralValueObject}. Itu <b>bukan bug</b>, melainkan keharusan
 *       teknis: {@code GeneralValueObject} adalah POJO abstrak biasa &mdash; bukan
 *       {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate tidak
 *       memetakan properti induknya sama sekali.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see KurikulumPunyaMatapelajaran
 * @see ais.database.model.sekolah.KurikulumSekolah
 * @see ais.database.model.sekolah.DetailJenisPenilaian
 * @see JenisPenilaian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kurikulum_punya_jenis_nilai", schema = "sekolah")
public class KurikulumPunyaJenisNilai extends GeneralValueObject {

	/**
	 * Penanda versi serialisasi Java, diwarisi lewat {@link java.io.Serializable} pada
	 * {@link ais.database.model.GeneralValueObject}.
	 *
	 * <p>Nilai {@code -7490758846785025664L} <b>bukan</b> penanda unik kelas ini: nilai yang persis
	 * sama dipakai puluhan entity lain di repositori sebagai boilerplate salin-tempel. Jangan
	 * dijadikan petunjuk hubungan klon antar-kelas.</p>
	 */
	/**
	 * Kunci utama baris, kolom {@code id}. Dideklarasikan ulang di sini (lihat catatan
	 * {@code GeneralValueObject} pada Javadoc kelas). Dipetakan pada {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama/label pengguna terakhir yang mengubah baris ini (kolom {@code oleh}), diisi otomatis oleh
	 * {@code ais.database.hibernate.AuditTimestampInterceptor}. Tidak dipetakan sebagai kolom
	 * eksplisit &mdash; Hibernate memakai nama properti apa adanya.
	 */
	private String oleh;
	/**
	 * Identitas (id pengguna) terakhir yang mengubah baris ini, kolom {@code olehId}. Pasangan
	 * teknis {@link #oleh} untuk keperluan jejak audit.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna terakhir pengubah baris ini.
	 *
	 * @return nilai kolom {@code olehId}, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> penjaga di awal method membuat pemanggilan dengan {@code null} atau
	 * string kosong/spasi <b>diabaikan diam-diam</b> (nilai lama dipertahankan). Konsekuensinya,
	 * jejak audit tidak pernah bisa dikosongkan lewat setter ini. Pola ini seragam di seluruh entity
	 * repositori.</p>
	 *
	 * @param olehId id pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama/label pengguna pengubah baris ini.
	 *
	 * <p><b>Non-obvious:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * kosong/spasi diabaikan diam-diam sehingga nilai lama bertahan.</p>
	 *
	 * @param oleh nama pengguna pengubah; {@code null}/kosong diabaikan tanpa efek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama/label pengguna terakhir pengubah baris ini.
	 *
	 * @return nilai kolom {@code oleh}, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum sebuah
	 * {@code UPDATE} atas baris ini dikirim ke basis data, lalu mendelegasikan pemutakhiran kolom
	 * audit ({@code oleh}, {@code olehId}, {@code tanggal_dirubah}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi &mdash; dan pada praktiknya tidak pernah
	 * terpicu sama sekali untuk entity ini, karena aplikasi tidak pernah menyimpan baris
	 * {@code kurikulum_punya_jenis_nilai} (lihat Javadoc kelas).</p>
	 *
	 * <p>Perhatikan bahwa deklarasi field {@link #tanggal_dirubah} berada pada baris fisik yang sama
	 * dengan method ini (gaya penulisan warisan generator). Field itu memegang waktu perubahan
	 * terakhir dan <b>diinisialisasi</b> ke waktu server saat objek dibuat melalui
	 * {@code ais.ui.util.WaktuUtil.getDate()}, bukan {@code new Date()} &mdash; sehingga tunduk pada
	 * pengaturan/penyesuaian waktu terpusat aplikasi.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * oleh kode bisnis. Berbeda dengan {@link #setOleh(String)}/{@link #setOlehId(String)}, setter
	 * ini <b>tidak</b> punya penjaga {@code null} &mdash; menyetel {@code null} benar-benar
	 * mengosongkan nilainya.</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah}, dipetakan
	 * sebagai {@link javax.persistence.TemporalType#TIMESTAMP}).
	 *
	 * @return stempel waktu perubahan terakhir; secara praktis tidak pernah {@code null} karena
	 *         field-nya diinisialisasi saat objek dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Butir konfigurasi penilaian yang dipasangkan, kolom FK {@code detail_jenis_penilaian_id}.
	 * Lihat {@link #getDetailJenisPenilaian()} untuk catatan {@code @NotFound(IGNORE)} dan status
	 * yatim relasi ini.
	 */
	private DetailJenisPenilaian detailJenisPenilaian;
	/**
	 * Baris mata pelajaran dalam kurikulum yang dipasangkan, kolom FK
	 * {@code kurikulum_punya_matapelajaran_id}. Ini adalah induk langsung baris ini; kurikulumnya
	 * sendiri dicapai lewat {@code getKurikulumPunyaMatapelajaran().getKurikulumSekolah()}.
	 */
	private KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran;
	/**
	 * Angka yang disimpan untuk pasangan (mata pelajaran kurikulum, butir jenis penilaian) &mdash;
	 * pada rancangan aslinya bobot/nilai kontribusi butir tersebut. Kolom {@code nilai}, nullable.
	 * Tidak pernah dibaca maupun ditulis kode aplikasi mana pun.
	 */
	private Double nilai;

	/**
	 * Konstruktor tanpa argumen. Diperlukan Hibernate untuk membentuk instance saat memuat baris;
	 * tidak pernah dipanggil dari kode aplikasi (tidak ada satu pun
	 * {@code new KurikulumPunyaJenisNilai()} di repositori).
	 */
	public KurikulumPunyaJenisNilai() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Dipetakan sebagai {@code @Id} dengan {@code @GeneratedValue(strategy = IDENTITY)} &mdash;
	 * nilai dibangkitkan basis data. Kolom dideklarasikan {@code insertable = false} sehingga
	 * Hibernate tidak pernah menyertakannya pada {@code INSERT}; nilai yang disetel manual lewat
	 * {@link #setId(Long)} tidak akan tersimpan pada baris baru.</p>
	 *
	 * @return kunci utama, atau {@code null} bila objek belum pernah tersimpan
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
	 * <p>Dipakai Hibernate saat memuat/menyimpan. Karena kolomnya {@code insertable = false},
	 * penyetelan manual tidak berpengaruh pada {@code INSERT}.</p>
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan butir konfigurasi penilaian ({@link ais.database.model.sekolah.DetailJenisPenilaian})
	 * yang dipasangkan pada baris ini &mdash; simpul tertinggi rantai penilaian
	 * ({@code JenisPenilaian &rarr; DetailJenisPenilaian &rarr; GrupPenilaian &rarr; ...}).
	 *
	 * <p>Relasi {@code @ManyToOne} dengan {@code cascade = {PERSIST, MERGE}} dan
	 * {@code @Fetch(FetchMode.SELECT)}; karena {@code fetch} tidak dinyatakan, default JPA
	 * {@code EAGER} berlaku sehingga satu SELECT tambahan ditembakkan saat baris ini dimuat.</p>
	 *
	 * <p><b>Non-obvious &mdash; {@code @NotFound(action = NotFoundAction.IGNORE)}:</b> anotasi ini
	 * ditambahkan pada r77038 (<i>"Tolerate orphan school grading relation in Generic CRUD reads"</i>)
	 * dan membuat FK yang <b>menggantung</b> (menunjuk baris {@code detail_jenis_penilaian} yang
	 * sudah terhapus) dikembalikan sebagai {@code null} alih-alih melempar
	 * {@code ObjectNotFoundException}. Keberadaan tambalan itu sendiri adalah bukti bahwa baris
	 * tabel ini nyata ada di basis data produksi sebagai data warisan &mdash; walaupun aplikasi
	 * tidak pernah menulisnya. Perhatikan bahwa
	 * {@link #getKurikulumPunyaMatapelajaran()} <b>tidak</b> memperoleh tambalan yang sama, sehingga
	 * FK menggantung di sisi itu tetap akan melempar eksepsi.</p>
	 *
	 * <p><b>Terverifikasi:</b> nol pemanggil dari kode aplikasi &mdash; memperkuat klaim "relasi
	 * yatim" pada Javadoc {@link ais.database.model.sekolah.DetailJenisPenilaian}. Satu-satunya
	 * rujukan ke kelas ini ada pada {@code JenisPenilaian}, dan rujukan itu mati; lihat Javadoc
	 * kelas.</p>
	 *
	 * @return butir jenis penilaian yang dipasangkan, atau {@code null} bila kolom FK kosong maupun
	 *         menggantung
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "detail_jenis_penilaian_id")
	public DetailJenisPenilaian getDetailJenisPenilaian() {
		return detailJenisPenilaian;
	}

	/**
	 * Menyetel butir konfigurasi penilaian yang dipasangkan pada baris ini.
	 *
	 * <p>Tidak pernah dipanggil dari kode aplikasi; hanya Hibernate yang memakainya saat memuat
	 * baris.</p>
	 *
	 * @param detailJenisPenilaian butir jenis penilaian target; boleh {@code null}
	 */
	public void setDetailJenisPenilaian(DetailJenisPenilaian detailJenisPenilaian) {
		this.detailJenisPenilaian = detailJenisPenilaian;
	}

	/**
	 * Mengembalikan baris mata pelajaran-dalam-kurikulum ({@link KurikulumPunyaMatapelajaran}) yang
	 * menjadi induk langsung baris ini, kolom FK {@code kurikulum_punya_matapelajaran_id}.
	 *
	 * <p>Inilah satu-satunya jalur menuju {@link ais.database.model.sekolah.KurikulumSekolah} dan,
	 * lewat kurikulum itu, menuju tenant ({@code sekolah}/{@code yayasan}): baris ini tidak punya
	 * kolom tenant sendiri. Setiap penyaring cakupan yang ditulis di masa depan harus menyeberangi
	 * dua tabel.</p>
	 *
	 * <p>Relasi {@code @ManyToOne}, {@code cascade = {PERSIST, MERGE}},
	 * {@code @Fetch(FetchMode.SELECT)}, {@code fetch} default {@code EAGER}. Berbeda dengan
	 * {@link #getDetailJenisPenilaian()}, sisi ini <b>tanpa</b> {@code @NotFound(IGNORE)} sehingga
	 * FK menggantung akan melempar {@code ObjectNotFoundException} saat baris dibaca.</p>
	 *
	 * @return baris kurikulum-mata pelajaran induk, atau {@code null} bila kolom FK kosong
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kurikulum_punya_matapelajaran_id")
	public KurikulumPunyaMatapelajaran getKurikulumPunyaMatapelajaran() {
		return kurikulumPunyaMatapelajaran;
	}

	/**
	 * Menyetel baris mata pelajaran-dalam-kurikulum yang menjadi induk baris ini.
	 *
	 * <p>Tidak pernah dipanggil dari kode aplikasi; hanya Hibernate yang memakainya saat memuat
	 * baris.</p>
	 *
	 * @param kurikulumPunyaMatapelajaran baris kurikulum-mata pelajaran induk; boleh {@code null}
	 */
	public void setKurikulumPunyaMatapelajaran(KurikulumPunyaMatapelajaran kurikulumPunyaMatapelajaran) {
		this.kurikulumPunyaMatapelajaran = kurikulumPunyaMatapelajaran;
	}

	/**
	 * Mengembalikan angka (bobot/nilai) yang tersimpan untuk pasangan (mata pelajaran kurikulum,
	 * butir jenis penilaian) pada baris ini.
	 *
	 * <p><b>Non-obvious:</b> getter menormalkan {@code null} menjadi {@code 0.0}, sehingga
	 * "belum pernah diisi" tidak dapat dibedakan dari "sengaja nol" oleh pemanggil. Bila kolom ini
	 * suatu saat benar-benar dipakai sebagai bobot, baris warisan yang {@code nilai}-nya {@code NULL}
	 * akan diam-diam berperilaku sebagai bobot 0 (butir tidak berkontribusi) alih-alih memunculkan
	 * kesalahan. Bandingkan {@link #setNilai(Double)} yang tetap menerima {@code null} apa adanya
	 * &mdash; asimetri getter/setter yang khas di repositori ini.</p>
	 *
	 * <p><b>Penting:</b> normalisasi ini <b>tidak</b> menulis balik ke field; tidak ada efek samping
	 * penyimpanan. Ini berbeda dari pola "getter write-back destruktif" yang ditemukan pada entity
	 * lain (mis. {@code JenisPenilaian.getSekolah()}).</p>
	 *
	 * <p>Tidak ada {@code @Column} eksplisit &mdash; Hibernate memakai nama properti
	 * ({@code nilai}). Tidak pernah dibaca kode aplikasi mana pun.</p>
	 *
	 * @return nilai kolom {@code nilai}, atau {@code 0.0} bila kolom tersebut {@code NULL}
	 */
	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	/**
	 * Menyetel angka (bobot/nilai) untuk pasangan (mata pelajaran kurikulum, butir jenis penilaian).
	 *
	 * <p>Tanpa penjaga {@code null}: nilai {@code null} tersimpan apa adanya dan kemudian akan
	 * terbaca sebagai {@code 0.0} lewat {@link #getNilai()}. Tidak pernah dipanggil dari kode
	 * aplikasi.</p>
	 *
	 * @param nilai bobot/nilai yang disimpan; boleh {@code null}
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

}
