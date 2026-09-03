package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.hibernate.Session;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Konfigurasi <b>pola penomoran Nomor Induk Siswa (NIS)</b> per sekolah &mdash; tabel
 * {@code sekolah.format_nis}. Entity ini bukan sekadar master pasif: method
 * {@link #format(Long, Integer)} adalah mesin perakit string NIS yang sesungguhnya, dan
 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)} adalah pencacah nomor urut yang dipersistensikan.
 * Verifikasi rantai pemanggil (lihat bagian &quot;Siapa memakai&quot;) mengonfirmasi bahwa entity
 * inilah yang menentukan NIS resmi seorang siswa saat calon siswa PSB dikonversi menjadi
 * {@link Siswa}.
 *
 * <h3>Model data: 10 kolom + 10 tanda</h3>
 * <p>
 * Satu format NIS disusun dari <b>sepuluh pasang</b> {@code (kolomN, tandaN)} yang dirangkai
 * berurutan dari 1 sampai 10. {@code kolomN} memilih JENIS potongan lewat salah satu dari lima
 * konstanta {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS}, {@link #TAHUN},
 * {@link #TAHUN_2_DIGIT}; {@code tandaN} adalah teks pemisah yang ditempel SESUDAH potongan itu
 * (bawaan {@code "/"}). Contoh: {@code kolom1=Tahun}, {@code tanda1="."},
 * {@code kolom2=Nomor Urut}, {@code tanda2=""} menghasilkan {@code "2026.0007"}.
 * </p>
 * <p>
 * <b>Hal paling non-obvious pada model ini:</b> pada mode {@link #KATA_STATIS}, teks statisnya
 * TIDAK disimpan di {@code kolomN} (yang hanya memuat kata kunci {@code "Kata Statis"}) melainkan
 * di {@code tandaN}. Jadi field &quot;tanda&quot; punya dua peran berbeda tergantung nilai kolom
 * pasangannya: pemisah biasa, atau muatan teks itu sendiri. Lihat {@link #format(Long, Integer)}.
 * </p>
 *
 * <h3>Dua mode pencacah nomor urut</h3>
 * <ol>
 * <li><b>Mode indeks manual</b> &mdash; {@link #getGunakanIndexUrut()} bernilai {@code true}.
 * Nomor urut diambil dari kolom {@link #getNomorIndex()} pada baris format ini sendiri, dibaca
 * dan dinaikkan ATOMIK oleh {@link #ambilLaluNaikkanNomorIndex(FormatNis)}. Karena pencacah
 * menempel pada baris format (yang punya kolom {@code sekolah}), mode ini <b>terisolasi per
 * sekolah</b>.</li>
 * <li><b>Mode hitung otomatis</b> &mdash; {@code gunakanIndexUrut = false} (bawaan). Nomor urut
 * diambil dari pencacah dipersistensikan {@link NisCounter} milik sekolah bersangkutan lewat
 * {@code ais.common.CommonPSB#ambilNomorUrutOtomatis(FormatNis, CalonSiswa)}, dinaikkan atomik
 * per (sekolah, tahun) &mdash; lihat {@link NisCounter}.</li>
 * </ol>
 *
 * <h3>Siapa memakai (verifikasi rantai pemanggil)</h3>
 * <ul>
 * <li>{@code ais.common.CommonPSB#onGenerateNis(CalonSiswa, NisGenerator, boolean)} mencari SATU
 * baris {@code FormatNis} milik sekolah calon siswa yang {@code aktif} (atau {@code aktif} masih
 * {@code null}) dengan {@code id} terbesar. Bila ketemu &rarr; NIS dibangkitkan lewat entity ini;
 * bila tidak &rarr; jatuh ke {@code NisGenerator} bawaan
 * ({@code ais.action.master.sekolah.psb.nis.DefaultNisGenerator}).</li>
 * <li>{@code ais.common.CommonPSB#generateCode(FormatNis, CalonSiswa)} memilih mode pencacah lalu
 * memanggil {@link #format(Long, Integer)}.</li>
 * <li>Alur pemicu: tombol generate NIS individual di {@code CalonSiswaAction}, upload kelulusan
 * massal ({@code CommonPSB#uploadKelulusan}), dan &mdash; bila gelombang PSB dikonfigurasi
 * {@code langsungDapatNisSaatDaftar} &mdash; SETIAP pendaftaran mandiri lewat formulir publik PPDB
 * ({@code PPDB1}, {@code PPDB2}, {@code PPDB_Alumni}, {@code PPDB_Simple}
 * sampai {@code PPDB_Simple6}).</li>
 * <li>Layar pengelolanya adalah {@code ais.action.master.sekolah.FormatNisAction}
 * ({@code /pages/master/sekolah/format_nis.zul}).</li>
 * <li>Entity ini juga didaftarkan pada seeding master {@code ais.common.InitData}.</li>
 * </ul>
 *
 * <h3>Riwayat &mdash; risiko NIS kembar yang SUDAH DIPERBAIKI (r83851 audit, perbaikan menyusul)</h3>
 * <p>
 * NIS adalah identitas resmi siswa: dipakai untuk login, pencetakan, kartu, dan penagihan. Audit
 * kode menemukan lima celah yang bersama-sama membuat {@code sekolah.siswa.nomor_induk} &mdash;
 * dideklarasikan {@code nullable = false} tetapi dahulu TIDAK unik &mdash; dapat kembar. Kelimanya
 * sudah ditutup:
 * </p>
 * <ol>
 * <li><b>Mode hitung otomatis kini memakai pencacah dipersistensikan.</b> Dahulu nomor urut
 * dihitung ulang dari {@code rowCount} <i>pendaftar</i> {@link CalonSiswa} (bisa mengembalikan
 * angka yang sama berulang kali selama tidak ada pendaftar baru). Sekarang
 * {@code ais.common.CommonPSB#ambilNomorUrutOtomatis(FormatNis, CalonSiswa)} menaikkan satu baris
 * {@link NisCounter} milik sekolah tersebut secara atomik (lihat butir 3).</li>
 * <li><b>Pencacah otomatis kini terisolasi per sekolah.</b> {@link NisCounter} punya kolom
 * {@code sekolah} wajib &mdash; pendaftaran sekolah lain di instalasi yang sama tidak lagi ikut
 * menaikkan nomor urut sekolah ini.</li>
 * <li><b>Race condition pada mode indeks manual ditutup dengan kunci baris database.</b>
 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)} menggantikan pasangan lama
 * &quot;{@code getNomorIndex()} di luar kunci, lalu {@code tambahIndexNomorSurat} terpisah&quot;.
 * Pembacaan DAN kenaikan kini terjadi dalam SATU transaksi yang mengunci baris {@link FormatNis}
 * lewat {@code KunciEntityHelper.jalankanDenganKunci} ({@code FOR NO KEY UPDATE NOWAIT} + retry),
 * aman lintas thread MAUPUN lintas node aplikasi (bukan sekadar {@code synchronized} Java yang
 * hanya berlaku satu JVM).</li>
 * <li><b>Kegagalan kenaikan pencacah tidak lagi ditelan.</b>
 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)} tidak membungkus dirinya dengan
 * {@code catch (Exception)} generik; kegagalan (mis. deadlock, koneksi putus) dilempar ke
 * pemanggil ({@code CommonPSB#generateCode}) alih-alih dibiarkan diam-diam membuat nomor terpakai
 * ulang.</li>
 * <li><b>Pemotongan diam-diam saat nomor melampaui lebar pad kini melempar exception.</b>
 * {@link #format(Long, Integer)} memeriksa lebih dulu apakah nomor urut muat pada lebar
 * {@link #getJumlahAngkaNolDiDepanNomorUrut()} sebelum memotong; bila tidak muat, method melempar
 * {@code IllegalStateException} alih-alih diam-diam menghasilkan NIS yang bertabrakan dengan
 * nomor urut lain. {@link #setJumlahAngkaNolDiDepanNomorUrut(Integer)} juga kini menolak nilai di
 * luar rentang 1&ndash;72 sejak awal (validasi rentang), bukan menunggu meledak saat generate.</li>
 * </ol>
 * <p>
 * <b>Lapisan pertahanan terakhir: unique constraint + pemeriksaan bentrok.</b> Migrasi startup
 * {@code ais.common.InitIndex#initNisCounterDanKeunikanSiswa()} membersihkan duplikat NIS lama
 * (disalin ke {@code sekolah.nis_duplikat_audit} sebelum diberi sufiks pembeda) lalu memasang
 * unique index {@code (sekolah_id, nomor_induk)} pada {@code sekolah.siswa} &mdash; jaring
 * pengaman lapisan basis data yang dahulu tidak ada sama sekali. Di lapisan aplikasi,
 * {@code CommonPSB#generateCode(FormatNis, CalonSiswa)} kini memeriksa apakah NIS hasil rakitan
 * sudah dipakai sekolah yang sama sebelum mengembalikannya, dan mencoba nomor urut berikutnya
 * bila bentrok (dibatasi jumlah percobaan) &mdash; pola yang sama dengan yang sudah lama dipakai
 * generator cadangan {@code DefaultNisGenerator}, kini diterapkan juga pada jalur
 * {@link FormatNis}.
 * </p>
 *
 * <h3>PERINGATAN &mdash; cakupan hak akses layar pengelola</h3>
 * <ul>
 * <li><b>Pewarisan hak lewat menu induk.</b> {@code format_nis.zul} tidak terdaftar sebagai menu
 * tersendiri di mana pun; satu-satunya jalur masuknya adalah tab &quot;Pengaturan NIS&quot; yang
 * disisipkan {@code GelombangPendaftaranPsbAction} lewat {@code MyInclude}. Karena
 * {@code CommonPrivilages.checkPrevilages} membaca menu yang sedang aktif di sesi, gerbang
 * READ/CREATE/UPDATE/DELETE layar format NIS sebenarnya adalah hak atas menu <i>Gelombang
 * Pendaftaran PSB</i> &mdash; siapa pun yang boleh mengubah gelombang pendaftaran otomatis boleh
 * mengubah pola penomoran identitas siswa.</li>
 * <li><b>Fail-open cakupan tenant pada daftar.</b> {@code FormatNisAction.initCriteria()} memakai
 * {@code Restrictions.sqlRestriction("1=1")} ketika combo Yayasan/Sekolah berada pada pilihan
 * bawaan &quot;Semua&quot;, sehingga grid menampilkan format NIS SELURUH sekolah di instalasi.
 * Kotak centang &quot;Aktif&quot; pada tiap baris langsung menyimpan perubahan, dan tombol
 * ubah/hapus bekerja atas baris apa pun yang tampil &mdash; termasuk milik tenant lain.</li>
 * <li>{@code FormatNisAction.checkNamaFormatNis()} memeriksa keunikan {@link #getNama()} secara
 * GLOBAL tanpa filter tenant, sehingga nama format satu sekolah memblokir sekolah lain sekaligus
 * membocorkan keberadaannya.</li>
 * <li>Daftar kolom cetak/unggah pada action menyertakan {@code "id"}, pola yang sama dengan
 * temuan unggah Excel pada entity lain (satu berkas unggahan dapat menimpa baris format bernomor
 * berapa pun). Daftar itu juga memuat properti yang tidak ada pada kelas ini
 * ({@code resetUrutanTiapBulan}, {@code resetTiapBulan}, {@code resetTiapTanggal},
 * {@code jurusan}) &mdash; sisa salin-tempel dari entity sejenis.</li>
 * </ul>
 *
 * <h3>Getter yang menulis balik (write-back)</h3>
 * <p>
 * Beberapa getter di kelas ini bukan pembacaan murni &mdash; mereka mengubah state objek saat
 * dipanggil, dan karena entity dipetakan {@code dynamicUpdate}, perubahan itu dapat ikut
 * ter-flush ke basis data pada akhir transaksi:
 * {@link #getContohFormat()} (menghitung ulang lalu MENIMPA kolom {@code contohFormat} setiap
 * kali dibaca &mdash; termasuk saat grid dirender), {@link #getSekolah()} dan
 * {@link #getYayasan()} (menulis balik hasil {@code check()}), serta
 * {@link #getMulaiUrutanKe()}, {@link #getResetUrutanTiapTahun()} dan
 * {@link #getJumlahAngkaNolDiDepanNomorUrut()} yang mengisi nilai bawaan ke field ketika masih
 * {@code null}. Sebaliknya {@link #getUrutBerdasarkanNomor()}, {@link #getGunakanIndexUrut()},
 * {@link #getNomorIndex()} dan {@link #getAktif()} hanya menormalkan nilai balik tanpa menyentuh
 * field.
 * </p>
 *
 * <h3>Catatan pemetaan Hibernate</h3>
 * <p>
 * Induk {@link ais.database.model.GeneralValueObject} <b>bukan</b> {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; ia POJO abstrak biasa sehingga Hibernate tidak memetakan
 * propertinya. Karena itu {@code id}, {@code oleh}, {@code olehId} dan {@code tanggal_dirubah}
 * WAJIB dideklarasikan ulang di kelas ini; pengulangan tersebut bukan bug melainkan keharusan
 * teknis. Entity memakai akses berbasis properti (anotasi pada getter),
 * {@code dynamicInsert}/{@code dynamicUpdate}, serta {@code @Audited} (Hibernate Envers) sehingga
 * setiap perubahan pola penomoran terekam di tabel revisi.
 * </p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 * <li><b>Mesin penomoran</b>: {@link #format(Long, Integer)},
 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)}, {@link #getContohFormat()}.</li>
 * <li><b>Identitas &amp; audit</b>: {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, {@link #onUpdate()}, {@link #toString()}.</li>
 * <li><b>Deskripsi format</b>: {@link #getNama()}, {@link #getKeterangan()},
 * {@code getKolom1()}&hellip;{@code getKolom10()}, {@code getTanda1()}&hellip;{@code getTanda10()}.</li>
 * <li><b>Aturan pencacah</b>: {@link #getMulaiUrutanKe()},
 * {@link #getJumlahAngkaNolDiDepanNomorUrut()}, {@link #getUrutBerdasarkanNomor()},
 * {@link #getResetUrutanTiapTahun()}, {@link #getResetTiap()},
 * {@link #getGunakanIndexUrut()}, {@link #getNomorIndex()}.</li>
 * <li><b>Relasi &amp; status</b>: {@link #getSekolah()}, {@link #getYayasan()},
 * {@link #getAktif()}.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Siswa
 * @see Sekolah
 * @see Yayasan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "format_nis")
public class FormatNis extends GeneralValueObject {

	/**
	 * Kata kunci {@code kolomN} yang berarti &quot;potongan ini tidak dipakai&quot;. Nilai bawaan
	 * seluruh {@code kolom1}&hellip;{@code kolom10}. Pada {@link #format(Long, Integer)} potongan
	 * ber-nilai ini dilewati sepenuhnya &mdash; {@code tandaN} pasangannya pun TIDAK ikut
	 * ditempel.
	 */
	public static final String KOSONG = "Kosong";
	/**
	 * Kata kunci {@code kolomN} yang berarti &quot;sisipkan nomor urut&quot;. Nomor diambil dari
	 * argumen {@code urutanke} {@link #format(Long, Integer)}, ditambah offset
	 * {@link #getMulaiUrutanKe()} bila {@link #getUrutBerdasarkanNomor()} aktif, lalu dipad/dipotong
	 * ke lebar {@link #getJumlahAngkaNolDiDepanNomorUrut()}.
	 */
	public static final String NOMOR_URUT = "Nomor Urut";
	/**
	 * Kata kunci {@code kolomN} yang berarti &quot;sisipkan teks tetap&quot;. <b>Teks tetapnya
	 * diambil dari {@code tandaN}, bukan dari {@code kolomN}</b> &mdash; pada mode ini field
	 * &quot;tanda&quot; berhenti berperan sebagai pemisah dan berubah menjadi muatan. Konsekuensinya
	 * potongan {@code KATA_STATIS} tidak dapat memiliki pemisah sendiri; pemisah harus ditaruh di
	 * dalam teks statis itu atau pada potongan berikutnya.
	 */
	public static final String KATA_STATIS = "Kata Statis";
	/**
	 * Kata kunci {@code kolomN} yang berarti &quot;sisipkan tahun penuh 4 digit&quot;, diambil dari
	 * argumen {@code tahun} {@link #format(Long, Integer)} (pada alur PSB: {@code tahunMasuk} calon
	 * siswa; pada pratinjau {@link #getContohFormat()}: tahun berjalan).
	 */
	public static final String TAHUN = "Tahun";
	/**
	 * Kata kunci {@code kolomN} yang berarti &quot;sisipkan dua digit terakhir tahun&quot;.
	 *
	 * <p><b>Perhatian ejaan:</b> nilai harfiahnya adalah {@code "Tahun 2 DIgit"} dengan huruf
	 * {@code I} kapital di tengah kata &quot;Digit&quot;. Combobox layar pengelola dibangun dari
	 * konstanta ini sehingga entri lewat UI selalu cocok, tetapi nilai yang masuk lewat jalur lain
	 * (unggah Excel, penyuntingan basis data langsung) yang mengeja &quot;Tahun 2 Digit&quot; tidak
	 * akan cocok dengan cabang mana pun di {@link #format(Long, Integer)} sehingga potongan itu
	 * hilang tanpa pesan kesalahan.</p>
	 */
	public static final String TAHUN_2_DIGIT = "Tahun 2 DIgit";
	/**
	 * Penanda versi serialisasi Java. Nilainya harus tetap agar objek yang tersimpan di sesi ZK
	 * atau replikasi sesi tetap dapat dibaca lintas deployment.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Field pendukung kunci utama; dideklarasikan ulang karena {@link GeneralValueObject} tidak
	 * dipetakan Hibernate. Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Field pendukung nama pengguna terakhir yang mengubah baris ini (jejak audit). Diisi
	 * {@code AuditTimestampInterceptor}; lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Field pendukung identitas (id) pengguna terakhir yang mengubah baris ini. Lihat
	 * {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Kasus tepi:</b> nilai {@code null} atau yang hanya berisi spasi <b>diabaikan diam-diam</b>
	 * &mdash; field mempertahankan nilai lamanya. Ini disengaja agar jejak audit tidak terhapus oleh
	 * proses latar yang tidak punya konteks pengguna, tetapi berarti setter ini tidak bisa dipakai
	 * untuk mengosongkan kolom.</p>
	 *
	 * @param olehId id pengguna pengubah; nilai kosong/spasi diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Representasi ringkas berbentuk {@code <id>-<nama>-<contoh NIS tahun berjalan>}, dipakai pada
	 * combobox, log, dan pesan kesalahan.
	 *
	 * <p><b>Efek samping:</b> method ini memanggil {@link #format(Long, Integer)}, jadi ia ikut
	 * merakit sebuah string NIS setiap kali objek di-{@code toString()}. Ia juga memanggil
	 * {@link #getMulaiUrutanKe()} yang menulis balik nilai bawaan {@code 1L} ke field bila masih
	 * {@code null}.</p>
	 *
	 * <p><b>Kuirk:</b> angka yang dipakai sebagai nomor urut contoh adalah
	 * {@link #getMulaiUrutanKe()} &mdash; berbeda dari {@link #getContohFormat()} yang memakai
	 * {@code 0L} atau {@link #getNomorIndex()}. Kedua &quot;contoh&quot; itu karena itu bisa
	 * menampilkan angka yang berlainan untuk baris yang sama.</p>
	 *
	 * @return string ringkas identitas format; komponen yang {@code null} ikut tercetak sebagai
	 *         {@code "null"}
	 */
	public String toString() {
		return id + "-" + nama + "-" + format(getMulaiUrutanKe(), Calendar.getInstance().get(Calendar.YEAR));
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Kasus tepi:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null} atau
	 * spasi-saja diabaikan diam-diam sehingga nilai lama dipertahankan.</p>
	 *
	 * @param oleh nama pengguna pengubah; nilai kosong/spasi diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mendelegasikan pencatatan jejak audit
	 * (pengubah &amp; waktu ubah) ke {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum
	 * Hibernate menulis pembaruan baris ini.
	 *
	 * <p>Pada baris yang sama dideklarasikan field pendukung {@code tanggal_dirubah}, diinisialisasi
	 * ke waktu sekarang lewat {@code WaktuUtil.getDate()} sehingga objek baru sudah punya cap waktu
	 * walau belum pernah disimpan. Deklarasi ulang field audit ini diperlukan karena
	 * {@link GeneralValueObject} tidak dipetakan Hibernate.</p>
	 *
	 * <p><b>Efek samping:</b> mengubah {@code oleh}/{@code olehId}/{@code tanggal_dirubah} objek ini
	 * saat siklus flush berjalan. Jangan dipanggil manual.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi otomatis oleh {@link #onUpdate()}; pemanggilan manual hanya dipakai alur
	 * impor/migrasi yang ingin mempertahankan cap waktu asal.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan (presisi {@code TIMESTAMP}); tidak {@code null} untuk objek baru
	 *         karena field diinisialisasi saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Field pendukung nama format (label yang dilihat pengguna, sekaligus kunci keunikan versi
	 * aplikasi). Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Field pendukung pratinjau format. <b>Nilai turunan</b> &mdash; selalu dihitung ulang dan
	 * ditimpa oleh {@link #getContohFormat()}, jadi isi kolom di basis data tidak pernah menjadi
	 * sumber kebenaran.
	 */
	private String contohFormat;
	/**
	 * Field pendukung saklar &quot;nomor urut kembali ke awal tiap ganti tahun&quot;. Bawaan
	 * {@code true}. Lihat {@link #getResetUrutanTiapTahun()}.
	 */
	private Boolean resetUrutanTiapTahun = true;

	/**
	 * Field pendukung tanggal reset kustom. Bila terisi dan sudah terlampaui, pencacah otomatis
	 * hanya menghitung pendaftar sejak tanggal tersebut. Lihat {@link #getResetTiap()}.
	 */
	private Date resetTiap;

	/**
	 * Field pendukung saklar &quot;tambahkan {@code mulaiUrutanKe} sebagai offset&quot;. Bawaan
	 * {@code false} &mdash; perhatikan bahwa {@link #getUrutBerdasarkanNomor()} justru
	 * mengembalikan {@code true} untuk nilai {@code null}, sehingga bawaan objek baru dan bawaan
	 * baris lama berbeda perilaku.
	 */
	private Boolean urutBerdasarkanNomor = false;
	/**
	 * Field pendukung offset awal nomor urut. Bawaan {@code 1L}. Lihat {@link #getMulaiUrutanKe()}.
	 */
	private Long mulaiUrutanKe = 1L;
	/**
	 * Field pendukung lebar tetap potongan nomor urut (jumlah karakter, bukan hanya jumlah nol di
	 * depan). Bawaan {@code 3}. Lihat {@link #getJumlahAngkaNolDiDepanNomorUrut()}.
	 */
	private Integer jumlahAngkaNolDiDepanNomorUrut = 3;
	/** Jenis potongan ke-1; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom1 = KOSONG;
	/** Pemisah setelah potongan ke-1, atau teks statisnya bila {@code kolom1} = {@link #KATA_STATIS}. */
	private String tanda1 = "/";
	/** Jenis potongan ke-2; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom2 = KOSONG;
	/** Pemisah setelah potongan ke-2, atau teks statisnya bila {@code kolom2} = {@link #KATA_STATIS}. */
	private String tanda2 = "/";
	/** Jenis potongan ke-3; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom3 = KOSONG;
	/** Pemisah setelah potongan ke-3, atau teks statisnya bila {@code kolom3} = {@link #KATA_STATIS}. */
	private String tanda3 = "/";
	/** Jenis potongan ke-4; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom4 = KOSONG;
	/** Pemisah setelah potongan ke-4, atau teks statisnya bila {@code kolom4} = {@link #KATA_STATIS}. */
	private String tanda4 = "/";
	/** Jenis potongan ke-5; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom5 = KOSONG;
	/** Pemisah setelah potongan ke-5, atau teks statisnya bila {@code kolom5} = {@link #KATA_STATIS}. */
	private String tanda5 = "/";
	/** Jenis potongan ke-6; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom6 = KOSONG;
	/** Pemisah setelah potongan ke-6, atau teks statisnya bila {@code kolom6} = {@link #KATA_STATIS}. */
	private String tanda6 = "/";
	/** Jenis potongan ke-7; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom7 = KOSONG;
	/** Pemisah setelah potongan ke-7, atau teks statisnya bila {@code kolom7} = {@link #KATA_STATIS}. */
	private String tanda7 = "/";
	/** Jenis potongan ke-8; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom8 = KOSONG;
	/** Pemisah setelah potongan ke-8, atau teks statisnya bila {@code kolom8} = {@link #KATA_STATIS}. */
	private String tanda8 = "/";
	/** Jenis potongan ke-9; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom9 = KOSONG;
	/** Pemisah setelah potongan ke-9, atau teks statisnya bila {@code kolom9} = {@link #KATA_STATIS}. */
	private String tanda9 = "/";
	/** Jenis potongan ke-10; salah satu konstanta kata kunci. Bawaan {@link #KOSONG}. */
	private String kolom10 = KOSONG;
	/** Pemisah setelah potongan ke-10, atau teks statisnya bila {@code kolom10} = {@link #KATA_STATIS}. */
	private String tanda10 = "/";
	/** Field pendukung catatan bebas administratif. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/**
	 * Field pendukung relasi ke sekolah pemilik format. Inilah satu-satunya pengait tenant yang
	 * dipakai pencari format di {@code CommonPSB#onGenerateNis}. Lihat {@link #getSekolah()}.
	 */
	private Sekolah sekolah;
	/**
	 * Field pendukung relasi ke yayasan. Hanya dipakai sebagai penyaring pada layar pengelola;
	 * pencari format saat pembangkitan NIS TIDAK menyaring berdasarkan yayasan. Lihat
	 * {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Field pendukung saklar mode pencacah. {@code true} = indeks manual tersimpan
	 * ({@link #getNomorIndex()}), {@code false} = hitung otomatis dari jumlah pendaftar. Bawaan
	 * {@code false}.
	 */
	private Boolean gunakanIndexUrut = false;
	/**
	 * Field pendukung nilai pencacah pada mode indeks manual. Dibaca dan dinaikkan atomik oleh
	 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)}. Bawaan {@code 1L}.
	 */
	private Long nomorIndex = 1L;
	/**
	 * Field pendukung status aktif. Hanya baris aktif (atau yang nilainya masih {@code null}) yang
	 * dipilih sebagai format NIS sekolah. Lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Mengembalikan nomor urut yang harus dipakai untuk pembangkitan NIS BERIKUTNYA, sekaligus
	 * MENAIKKAN pencacah {@link #getNomorIndex()} sebesar satu secara atomik. Ini adalah
	 * satu-satunya titik di seluruh kelas yang melakukan penulisan ke basis data.
	 *
	 * <p><b>Perbaikan dibanding {@code tambahIndexNomorSurat(FormatNis)} yang lama:</b> versi lama
	 * memisahkan &quot;baca {@link #getNomorIndex()}&quot; (dilakukan pemanggil, DI LUAR kunci apa
	 * pun) dari &quot;naikkan &amp; simpan&quot; (method terpisah ini, {@code synchronized} tapi
	 * hanya berlaku satu JVM) &mdash; celah di antara keduanya membuat dua permintaan bersamaan
	 * dapat membaca angka yang sama dan menerbitkan NIS identik. Method ini menyatukan baca+naikkan
	 * dalam SATU transaksi yang mengunci baris {@link FormatNis} di basis data lewat
	 * {@code ais.database.hibernate.KunciEntityHelper#jalankanDenganKunci(Class,
	 * java.io.Serializable, ais.database.hibernate.KunciEntityHelper.PekerjaanTransaksi)}
	 * ({@code SELECT ... FOR NO KEY UPDATE NOWAIT} + retry backoff), sehingga aman lintas thread
	 * MAUPUN lintas node aplikasi &mdash; bukan sekadar {@code synchronized} Java.</p>
	 *
	 * <p><b>Alur:</b> (1) kembalikan {@link #getNomorIndex()} objek yang diterima apa adanya bila
	 * {@code nomorSurat} {@code null} atau tidak memakai mode indeks manual (tidak ada penguncian
	 * atau penulisan basis data untuk kasus ini); (2) selain itu, kunci baris {@link FormatNis}
	 * ber-{@code id} sama di basis data, baca ULANG {@link #getNomorIndex()} dari baris yang
	 * terkunci (bukan dari objek {@code nomorSurat} yang mungkin sudah basi), naikkan satu, simpan,
	 * lalu kembalikan nilai LAMA (sebelum dinaikkan) sebagai nomor urut yang dipakai pemanggil.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.common.CommonPSB#generateCode(FormatNis, CalonSiswa)},
	 * MENGGANTIKAN pasangan lama &quot;baca {@link #getNomorIndex()} lalu panggil
	 * {@code tambahIndexNomorSurat} terpisah&quot;.</p>
	 *
	 * <p><b>Kegagalan tidak ditelan.</b> Berbeda dari method lama, tidak ada
	 * {@code catch (Exception)} generik di sini; kegagalan kunci/commit dilempar apa adanya ke
	 * pemanggil sehingga NIS yang gagal dinaikkan pencacahnya tidak akan diam-diam terpakai
	 * ulang.</p>
	 *
	 * @param nomorSurat baris format yang pencacahnya akan dibaca &amp; dinaikkan
	 * @return nomor urut yang harus dipakai pembangkitan NIS kali ini; {@code null} bila
	 *         {@code nomorSurat} {@code null}
	 * @throws Exception diteruskan dari kegagalan penguncian/transaksi basis data, atau bila baris
	 *                    {@code nomorSurat} sudah dihapus tepat sebelum dikunci
	 */
	public static Long ambilLaluNaikkanNomorIndex(final FormatNis nomorSurat) throws Exception {
		if (nomorSurat == null) {
			return null;
		}
		if (!nomorSurat.getGunakanIndexUrut()) {
			return nomorSurat.getNomorIndex();
		}
		final Long[] hasil = new Long[1];
		boolean adaBaris = ais.database.hibernate.KunciEntityHelper.jalankanDenganKunci(FormatNis.class,
				nomorSurat.getId(), new ais.database.hibernate.KunciEntityHelper.PekerjaanTransaksi() {
					@Override
					public void kerjakan(Session session, Object entityTerkunci) throws Exception {
						FormatNis terkunci = (FormatNis) entityTerkunci;
						Long nomorDipakai = terkunci.getNomorIndex();
						terkunci.setNomorIndex(nomorDipakai + 1L);
						session.update(terkunci);
						hasil[0] = nomorDipakai;
					}
				});
		if (!adaBaris) {
			throw new IllegalStateException(
					"FormatNis id=" + nomorSurat.getId() + " sudah dihapus saat pembangkitan NIS.");
		}
		return hasil[0];
	}

	/**
	 * <b>Mesin perakit NIS.</b> Menyusun satu string nomor dengan menyulam sepuluh pasang
	 * {@code (kolomN, tandaN)} secara berurutan, mengganti kata kunci pada {@code kolomN} dengan
	 * potongan yang sesuai lalu menempelkan {@code tandaN} sebagai pemisah.
	 *
	 * <p><b>Aturan tiap kata kunci:</b></p>
	 * <ul>
	 * <li>{@link #KOSONG} &rarr; potongan dilewati; {@code tandaN} pasangannya <b>tidak</b> ikut
	 * ditempel.</li>
	 * <li>{@link #NOMOR_URUT} &rarr; menghitung {@code urutanke + (getUrutBerdasarkanNomor() ?
	 * getMulaiUrutanKe() : 0)}, memberi nol di depan hingga selebar
	 * {@link #getJumlahAngkaNolDiDepanNomorUrut()}, lalu menempel {@code tandaN}.</li>
	 * <li>{@link #KATA_STATIS} &rarr; menempel {@code tandaN} SAJA (field tanda berperan sebagai
	 * muatan teks, lihat Javadoc konstanta).</li>
	 * <li>{@link #TAHUN} &rarr; menempel {@code tahun} penuh diikuti {@code tandaN}.</li>
	 * <li>{@link #TAHUN_2_DIGIT} &rarr; menempel dua karakter terakhir dari {@code tahun} diikuti
	 * {@code tandaN}.</li>
	 * <li>Nilai lain (mis. ejaan tak dikenal hasil unggah Excel) &rarr; tidak ada cabang yang
	 * cocok, sehingga potongan DAN pemisahnya hilang tanpa pesan kesalahan.</li>
	 * </ul>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.common.CommonPSB#generateCode(FormatNis, CalonSiswa)}
	 * (pembangkitan NIS sungguhan, dengan {@code tahun} = {@code calonSiswa.getTahunMasuk()}),
	 * {@link #getContohFormat()} dan {@link #toString()} (pratinjau, dengan tahun berjalan).</p>
	 *
	 * <p><b>Method ini murni menghitung</b> &mdash; ia tidak menaikkan pencacah dan tidak menulis
	 * ke basis data; kenaikan pencacah adalah tanggung jawab
	 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)} (mode indeks manual) atau
	 * {@code CommonPSB#ambilNomorUrutOtomatis(FormatNis, CalonSiswa)} (mode otomatis), keduanya
	 * dipanggil terpisah oleh {@code CommonPSB} SEBELUM method ini.
	 * Ia juga membaca field {@code kolomN}/{@code tandaN} secara langsung (bukan lewat getter),
	 * jadi tidak memicu pemulihan lazy apa pun.</p>
	 *
	 * <p><b>Kasus tepi yang perlu diwaspadai:</b></p>
	 * <ul>
	 * <li><b>Overflow lebar kini gagal eksplisit, bukan diam-diam bertabrakan.</b> Dahulu baris
	 * {@code nomor.substring(nomor.length() - lebar)} memotong kelebihan digit dari sebelah KIRI
	 * &mdash; dengan lebar 3, urutan ke-1234 menghasilkan {@code "234"}, identik dengan urutan
	 * ke-234, dan karena {@code siswa.nomor_induk} dahulu tidak unik, tabrakan itu tidak tertangkap
	 * basis data. Sekarang method ini memeriksa lebih dulu apakah representasi desimal
	 * {@code urutanke + offset} muat dalam {@link #getJumlahAngkaNolDiDepanNomorUrut()} digit; bila
	 * TIDAK muat, method melempar {@code IllegalStateException} alih-alih menghasilkan NIS yang
	 * pasti bertabrakan. Admin sekolah perlu memperbesar &quot;Jumlah Karakter Nomor Urutan&quot;
	 * pada layar Format NIS begitu populasi mendekati {@code 10^lebar}.</li>
	 * <li><b>Rentang lebar kini divalidasi di setter.</b> {@code lebar 0}/negatif/{@code > 72}
	 * ditolak sejak {@link #setJumlahAngkaNolDiDepanNomorUrut(Integer)} dipanggil (lihat javadoc
	 * setter itu), sehingga kasus tepi lama ({@code StringIndexOutOfBoundsException} saat generate)
	 * seharusnya tidak lagi tercapai lewat jalur normal aplikasi.</li>
	 * <li><b>{@code NullPointerException}</b> bila ada {@code kolomN} bernilai {@code null} di
	 * basis data (kolom tidak dijamin {@code NOT NULL}; nilai bawaan hanya berlaku untuk objek
	 * yang dibuat lewat Java), karena {@code data[i].equals(...)} dipanggil pada elemen tersebut.
	 * Efeknya terasa jauh dari sumbernya: pembangkitan NIS gagal untuk seluruh sekolah pemilik
	 * baris format itu.</li>
	 * <li><b>{@code tahun} bernilai {@code null}</b> menghasilkan potongan {@code "null"} pada
	 * mode {@link #TAHUN} dan {@code "ll"} pada mode {@link #TAHUN_2_DIGIT}, bukan kesalahan.</li>
	 * <li><b>Semantik &quot;Mulai Urutan Ke&quot;.</b> Nilai itu diperlakukan sebagai OFFSET yang
	 * DITAMBAHKAN, bukan sebagai angka awal. Digabung dengan pencacah otomatis yang sudah
	 * mengembalikan {@code jumlah + 1}, pengaturan bawaan {@code mulaiUrutanKe = 1} membuat
	 * pendaftar pertama bernomor 2, bukan 1 &mdash; dan hanya bila
	 * {@link #getUrutBerdasarkanNomor()} aktif.</li>
	 * <li>Hasil dirakit dengan {@code +=} pada {@code String} di dalam loop; tidak jadi masalah
	 * untuk 10 iterasi, tetapi jangan dijadikan contoh.</li>
	 * </ul>
	 *
	 * @param urutanke nomor urut mentah yang akan disisipkan pada potongan {@link #NOMOR_URUT};
	 *                 berasal dari {@link #getNomorIndex()} (mode indeks manual) atau dari hasil
	 *                 hitung {@code CommonPSB#ambilNomorUrutOtomatis} (mode otomatis). Tidak
	 *                 boleh {@code null} bila ada potongan bertipe {@link #NOMOR_URUT}
	 * @param tahun    tahun yang dipakai potongan {@link #TAHUN}/{@link #TAHUN_2_DIGIT}
	 * @return string NIS/nomor hasil rakitan; string kosong bila seluruh kolom bernilai
	 *         {@link #KOSONG} atau tidak dikenali
	 * @throws IllegalStateException bila ada potongan {@link #NOMOR_URUT} dan representasi
	 *         desimal {@code urutanke + offset} lebih panjang dari
	 *         {@link #getJumlahAngkaNolDiDepanNomorUrut()} digit &mdash; menggantikan pemotongan
	 *         diam-diam yang dahulu menghasilkan NIS kembar
	 */
	public String format(Long urutanke, Integer tahun) {
		String hasil = "";

		String[] data = new String[] { kolom1, tanda1, kolom2, tanda2, kolom3, tanda3, kolom4, tanda4, kolom5, tanda5,
				kolom6, tanda6, kolom7, tanda7, kolom8, tanda8, kolom9, tanda9, kolom10, tanda10, };

		for (int i = 0; i < data.length; i += 2) {
			if (data[i].equals(KOSONG)) {
				continue;
			} else if (data[i].equals(NOMOR_URUT)) {
				long nilaiUrut = urutanke + ((getUrutBerdasarkanNomor() ? getMulaiUrutanKe() : 0L));
				String angkaMentah = Long.toString(nilaiUrut);
				int lebar = getJumlahAngkaNolDiDepanNomorUrut();
				if (angkaMentah.length() > lebar) {
					throw new IllegalStateException("Nomor urut " + nilaiUrut + " (" + angkaMentah.length()
							+ " digit) melebihi lebar yang dikonfigurasi (" + lebar
							+ " digit) pada Format NIS \"" + nama
							+ "\"; NIS akan bertabrakan bila dipotong. Perbesar \"Jumlah Karakter Nomor Urutan\" pada layar Format NIS.");
				}
				String nomor = "000000000000000000000000000000000000000000000000000000000000000000000000"
						+ nilaiUrut;
				nomor = nomor.substring(nomor.length() - lebar);
				hasil += nomor + data[i + 1];
			} else if (data[i].equals(KATA_STATIS)) {
				hasil += data[i + 1];
			} else if (data[i].equals(TAHUN_2_DIGIT)) {
				String tgl = "" + tahun;
				tgl = tgl.substring(tgl.length() - 2);
				hasil += tgl + data[i + 1];
			} else if (data[i].equals(TAHUN)) {
				hasil += tahun + data[i + 1];
			}
		}

		return hasil;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA sekaligus dipakai layar pengelola
	 * saat menekan tombol &quot;Tambah&quot;. Seluruh nilai bawaan (sepuluh kolom
	 * {@link #KOSONG} bertanda {@code "/"}, lebar nomor 3, reset tiap tahun, mulai urutan 1)
	 * berasal dari inisialisasi field, bukan dari konstruktor ini.
	 */
	public FormatNis() {
	}

	/**
	 * Mengembalikan kunci utama baris format ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} dengan strategi
	 * {@code IDENTITY} &mdash; nilainya dibangkitkan basis data saat penyisipan.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Dipakai kerangka persistensi dan alur impor; jangan diubah manual
	 * pada objek yang sudah terkelola.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama format NIS (label &quot;Nama Format NIS&quot; pada layar pengelola,
	 * sekaligus judul kolom pertama grid).
	 *
	 * <p>Kolom bersifat {@code NOT NULL}. Keunikannya TIDAK dijaga basis data melainkan hanya oleh
	 * pemeriksaan aplikasi {@code FormatNisAction.checkNamaFormatNis()} &mdash; dan pemeriksaan itu
	 * berlaku GLOBAL lintas sekolah/yayasan, sehingga satu instalasi multi-sekolah tidak dapat
	 * memakai nama format yang sama di dua sekolah.</p>
	 *
	 * @return nama format yang sudah di-{@code trim}, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama format NIS. Nilai disimpan apa adanya (pemangkasan spasi dilakukan layar
	 * pengelola sebelum memanggil setter ini, dan sekali lagi saat dibaca getter).
	 *
	 * @param nama nama format; wajib terisi karena kolom {@code NOT NULL}
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas administratif tentang format ini (ditampilkan sebagai kolom
	 * terakhir grid layar pengelola).
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas administratif.
	 *
	 * @param keterangan teks keterangan; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan jenis potongan ke-1.
	 *
	 * @return salah satu dari {@link #KOSONG}, {@link #NOMOR_URUT}, {@link #KATA_STATIS},
	 *         {@link #TAHUN}, {@link #TAHUN_2_DIGIT}
	 */
	public String getKolom1() {
		return kolom1;
	}

	/**
	 * Menetapkan jenis potongan ke-1.
	 *
	 * @param kolom1 kata kunci jenis potongan; nilai di luar daftar konstanta membuat potongan
	 *               diabaikan diam-diam oleh {@link #format(Long, Integer)}
	 */
	public void setKolom1(String kolom1) {
		this.kolom1 = kolom1;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-1 &mdash; atau teks statisnya bila
	 * {@link #getKolom1()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda1() {
		return tanda1;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-1.
	 *
	 * @param tanda1 teks pemisah/muatan
	 */
	public void setTanda1(String tanda1) {
		this.tanda1 = tanda1;
	}

	/**
	 * Mengembalikan jenis potongan ke-2.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom2() {
		return kolom2;
	}

	/**
	 * Menetapkan jenis potongan ke-2.
	 *
	 * @param kolom2 kata kunci jenis potongan
	 */
	public void setKolom2(String kolom2) {
		this.kolom2 = kolom2;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-2 &mdash; atau teks statisnya bila
	 * {@link #getKolom2()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda2() {
		return tanda2;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-2.
	 *
	 * @param tanda2 teks pemisah/muatan
	 */
	public void setTanda2(String tanda2) {
		this.tanda2 = tanda2;
	}

	/**
	 * Mengembalikan jenis potongan ke-3.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom3() {
		return kolom3;
	}

	/**
	 * Menetapkan jenis potongan ke-3.
	 *
	 * @param kolom3 kata kunci jenis potongan
	 */
	public void setKolom3(String kolom3) {
		this.kolom3 = kolom3;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-3 &mdash; atau teks statisnya bila
	 * {@link #getKolom3()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda3() {
		return tanda3;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-3.
	 *
	 * @param tanda3 teks pemisah/muatan
	 */
	public void setTanda3(String tanda3) {
		this.tanda3 = tanda3;
	}

	/**
	 * Mengembalikan jenis potongan ke-4.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom4() {
		return kolom4;
	}

	/**
	 * Menetapkan jenis potongan ke-4.
	 *
	 * @param kolom4 kata kunci jenis potongan
	 */
	public void setKolom4(String kolom4) {
		this.kolom4 = kolom4;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-4 &mdash; atau teks statisnya bila
	 * {@link #getKolom4()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda4() {
		return tanda4;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-4.
	 *
	 * @param tanda4 teks pemisah/muatan
	 */
	public void setTanda4(String tanda4) {
		this.tanda4 = tanda4;
	}

	/**
	 * Mengembalikan jenis potongan ke-5.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom5() {
		return kolom5;
	}

	/**
	 * Menetapkan jenis potongan ke-5.
	 *
	 * @param kolom5 kata kunci jenis potongan
	 */
	public void setKolom5(String kolom5) {
		this.kolom5 = kolom5;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-5 &mdash; atau teks statisnya bila
	 * {@link #getKolom5()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda5() {
		return tanda5;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-5.
	 *
	 * @param tanda5 teks pemisah/muatan
	 */
	public void setTanda5(String tanda5) {
		this.tanda5 = tanda5;
	}

	/**
	 * Mengembalikan jenis potongan ke-6.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom6() {
		return kolom6;
	}

	/**
	 * Menetapkan jenis potongan ke-6.
	 *
	 * @param kolom6 kata kunci jenis potongan
	 */
	public void setKolom6(String kolom6) {
		this.kolom6 = kolom6;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-6 &mdash; atau teks statisnya bila
	 * {@link #getKolom6()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda6() {
		return tanda6;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-6.
	 *
	 * @param tanda6 teks pemisah/muatan
	 */
	public void setTanda6(String tanda6) {
		this.tanda6 = tanda6;
	}

	/**
	 * Mengembalikan jenis potongan ke-7.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom7() {
		return kolom7;
	}

	/**
	 * Menetapkan jenis potongan ke-7.
	 *
	 * @param kolom7 kata kunci jenis potongan
	 */
	public void setKolom7(String kolom7) {
		this.kolom7 = kolom7;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-7 &mdash; atau teks statisnya bila
	 * {@link #getKolom7()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda7() {
		return tanda7;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-7.
	 *
	 * @param tanda7 teks pemisah/muatan
	 */
	public void setTanda7(String tanda7) {
		this.tanda7 = tanda7;
	}

	/**
	 * Mengembalikan jenis potongan ke-8.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom8() {
		return kolom8;
	}

	/**
	 * Menetapkan jenis potongan ke-8.
	 *
	 * @param kolom8 kata kunci jenis potongan
	 */
	public void setKolom8(String kolom8) {
		this.kolom8 = kolom8;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-8 &mdash; atau teks statisnya bila
	 * {@link #getKolom8()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda8() {
		return tanda8;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-8.
	 *
	 * @param tanda8 teks pemisah/muatan
	 */
	public void setTanda8(String tanda8) {
		this.tanda8 = tanda8;
	}

	/**
	 * Mengembalikan jenis potongan ke-9.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom9() {
		return kolom9;
	}

	/**
	 * Menetapkan jenis potongan ke-9.
	 *
	 * @param kolom9 kata kunci jenis potongan
	 */
	public void setKolom9(String kolom9) {
		this.kolom9 = kolom9;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-9 &mdash; atau teks statisnya bila
	 * {@link #getKolom9()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda9() {
		return tanda9;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-9.
	 *
	 * @param tanda9 teks pemisah/muatan
	 */
	public void setTanda9(String tanda9) {
		this.tanda9 = tanda9;
	}

	/**
	 * Mengembalikan jenis potongan ke-10.
	 *
	 * @return salah satu konstanta kata kunci kolom
	 */
	public String getKolom10() {
		return kolom10;
	}

	/**
	 * Menetapkan jenis potongan ke-10.
	 *
	 * @param kolom10 kata kunci jenis potongan
	 */
	public void setKolom10(String kolom10) {
		this.kolom10 = kolom10;
	}

	/**
	 * Mengembalikan pemisah setelah potongan ke-10 &mdash; atau teks statisnya bila
	 * {@link #getKolom10()} bernilai {@link #KATA_STATIS}.
	 *
	 * @return teks pemisah/muatan; bawaan {@code "/"}
	 */
	public String getTanda10() {
		return tanda10;
	}

	/**
	 * Menetapkan pemisah (atau teks statis) potongan ke-10.
	 *
	 * @param tanda10 teks pemisah/muatan
	 */
	public void setTanda10(String tanda10) {
		this.tanda10 = tanda10;
	}

	/**
	 * Mengembalikan offset nomor urut (label layar: &quot;Mulai Urutan Ke&quot;).
	 *
	 * <p><b>Semantik sebenarnya:</b> nilai ini bukan &quot;nomor pertama&quot; melainkan angka yang
	 * DITAMBAHKAN ke nomor urut hasil hitung, dan hanya bila {@link #getUrutBerdasarkanNomor()}
	 * bernilai {@code true}. Lihat {@link #format(Long, Integer)}.</p>
	 *
	 * <p><b>Efek samping:</b> getter menulis balik &mdash; bila field masih {@code null}, nilai
	 * {@code 1L} ditugaskan ke field sehingga baris ikut ditandai kotor dan dapat ter-flush ke
	 * basis data.</p>
	 *
	 * @return offset nomor urut; tidak pernah {@code null}
	 */
	public Long getMulaiUrutanKe() {
		if (mulaiUrutanKe == null) {
			mulaiUrutanKe = 1L;
		}
		return mulaiUrutanKe;
	}

	/**
	 * Menetapkan offset nomor urut.
	 *
	 * @param mulaiUrutanKe offset yang ditambahkan ke nomor urut; boleh {@code null} (getter akan
	 *                      mengembalikannya ke {@code 1L})
	 */
	public void setMulaiUrutanKe(Long mulaiUrutanKe) {
		this.mulaiUrutanKe = mulaiUrutanKe;
	}

	/**
	 * Mengembalikan saklar &quot;urutan kembali ke awal tiap ganti tahun&quot;.
	 *
	 * <p>Saklar ini hanya berpengaruh pada mode pencacah OTOMATIS: bila {@code true},
	 * {@code CommonPSB#getindex} menambahkan syarat {@code tahunMasuk = <tahun calon siswa>} pada
	 * query penghitung; bila {@code false}, seluruh pendaftar sepanjang sejarah dihitung. Mode
	 * indeks manual ({@link #getGunakanIndexUrut()}) mengabaikan saklar ini sepenuhnya &mdash;
	 * pencacahnya tidak pernah reset otomatis dan harus disetel ulang manual di layar
	 * pengelola.</p>
	 *
	 * <p><b>Efek samping:</b> getter menulis balik nilai bawaan {@code true} ke field bila masih
	 * {@code null}.</p>
	 *
	 * @return {@code true} bila nomor urut direset tiap tahun; tidak pernah {@code null}
	 */
	public Boolean getResetUrutanTiapTahun() {
		if (resetUrutanTiapTahun == null) {
			resetUrutanTiapTahun = true;
		}
		return resetUrutanTiapTahun;
	}

	/**
	 * Menetapkan saklar reset nomor urut tiap tahun.
	 *
	 * @param resetUrutanTiapTahun {@code true} untuk mereset tiap ganti tahun; boleh {@code null}
	 *                             (getter mengembalikannya ke {@code true})
	 */
	public void setResetUrutanTiapTahun(Boolean resetUrutanTiapTahun) {
		this.resetUrutanTiapTahun = resetUrutanTiapTahun;
	}

	/**
	 * Mengembalikan pratinjau NIS yang akan dihasilkan format ini, memakai tahun berjalan.
	 *
	 * <p><b>Getter destruktif / write-back.</b> Method ini TIDAK membaca kolom
	 * {@code contohFormat} di basis data &mdash; ia menghitung ulang lewat
	 * {@link #format(Long, Integer)} lalu MENIMPA field dengan hasil hitung tersebut. Karena entity
	 * dipetakan {@code dynamicUpdate}, pembacaan biasa (mis. saat baris dirender di grid layar
	 * pengelola melalui {@code FormatNisRenderer}) dapat membuat baris ditandai kotor dan
	 * nilai barunya ditulis ke basis data. Kolom {@code contohFormat} dengan demikian sepenuhnya
	 * turunan; jangan diperlakukan sebagai data yang diisi pengguna.</p>
	 *
	 * <p><b>Nomor urut yang dipakai contoh</b> adalah {@link #getNomorIndex()} pada mode indeks
	 * manual, atau {@code 0L} pada mode otomatis &mdash; berbeda dari {@link #toString()} yang
	 * memakai {@link #getMulaiUrutanKe()}. Pratinjau mode otomatis karena itu tidak mencerminkan
	 * nomor sebenarnya yang akan terbit.</p>
	 *
	 * @return string contoh NIS; tidak pernah {@code null} (bisa berupa string kosong bila semua
	 *         kolom {@link #KOSONG})
	 */
	public String getContohFormat() {
		contohFormat = getGunakanIndexUrut() ? format(getNomorIndex(), Calendar.getInstance().get(Calendar.YEAR)) : format(0L, Calendar.getInstance().get(Calendar.YEAR));
		return contohFormat;
	}

	/**
	 * Menetapkan pratinjau format.
	 *
	 * <p>Praktis tidak berguna: nilai apa pun yang ditetapkan di sini akan ditimpa pada pembacaan
	 * {@link #getContohFormat()} berikutnya. Setter ini ada agar kerangka persistensi dan alur
	 * impor/ekspor kolom tetap dapat memetakan properti.</p>
	 *
	 * @param contohFormat nilai pratinjau; akan ditimpa oleh getter
	 */
	public void setContohFormat(String contohFormat) {
		this.contohFormat = contohFormat;
	}

	/**
	 * Mengembalikan lebar tetap potongan nomor urut (label layar: &quot;Jumlah Karakter Nomor
	 * Urutan&quot;).
	 *
	 * <p><b>Bukan sekadar jumlah nol di depan meski namanya begitu</b> &mdash; nilai ini adalah
	 * panjang AKHIR potongan nomor: angka yang lebih pendek diberi nol di depan, angka yang lebih
	 * panjang DIPOTONG dari kiri. Baca peringatan tabrakan NIS pada {@link #format(Long, Integer)}
	 * dan pada Javadoc kelas.</p>
	 *
	 * <p><b>Efek samping:</b> getter menulis balik nilai bawaan {@code 3} ke field bila masih
	 * {@code null}.</p>
	 *
	 * @return lebar potongan nomor urut; tidak pernah {@code null}
	 */
	public Integer getJumlahAngkaNolDiDepanNomorUrut() {
		if (jumlahAngkaNolDiDepanNomorUrut == null) {
			jumlahAngkaNolDiDepanNomorUrut = 3;
		}
		return jumlahAngkaNolDiDepanNomorUrut;
	}

	/**
	 * Menetapkan lebar tetap potongan nomor urut.
	 *
	 * <p><b>Rentang kini divalidasi di sini.</b> Nilai harus {@code null} atau di antara 1 dan 72
	 * (inklusif); di luar itu method melempar {@code IllegalArgumentException} SEBELUM nilai
	 * tersimpan. Dahulu tidak ada validasi sama sekali di sini maupun di layar pengelola: nilai
	 * {@code 0} membuat nomor urut hilang dari NIS, dan nilai negatif atau lebih besar dari 72
	 * membuat {@link #format(Long, Integer)} melempar {@code StringIndexOutOfBoundsException} baru
	 * saat NIS digenerate untuk siswa sungguhan &mdash; jauh dari titik input yang salah. Validasi
	 * di sini menggagalkan input yang salah SAAT disimpan di layar Format NIS, bukan belakangan.</p>
	 *
	 * @param jumlahAngkaNolDiDepanNomorUrut lebar potongan nomor urut; boleh {@code null} (getter
	 *                                       mengembalikannya ke {@code 3})
	 * @throws IllegalArgumentException bila nilai bukan {@code null} dan di luar rentang 1&ndash;72
	 */
	public void setJumlahAngkaNolDiDepanNomorUrut(Integer jumlahAngkaNolDiDepanNomorUrut) {
		if (jumlahAngkaNolDiDepanNomorUrut != null
				&& (jumlahAngkaNolDiDepanNomorUrut < 1 || jumlahAngkaNolDiDepanNomorUrut > 72)) {
			throw new IllegalArgumentException("Jumlah Karakter Nomor Urutan harus antara 1 dan 72 (nilai diberikan: "
					+ jumlahAngkaNolDiDepanNomorUrut + ").");
		}
		this.jumlahAngkaNolDiDepanNomorUrut = jumlahAngkaNolDiDepanNomorUrut;
	}

	/**
	 * Mengembalikan saklar &quot;urutkan nomor berdasar Format NIS&quot; &mdash; yang secara
	 * teknis berarti &quot;tambahkan {@link #getMulaiUrutanKe()} sebagai offset pada nomor
	 * urut&quot;.
	 *
	 * <p><b>Kuirk ketidakkonsistenan nilai bawaan:</b> field diinisialisasi {@code false} untuk
	 * objek baru, tetapi getter ini mengembalikan {@code true} ketika field bernilai {@code null}.
	 * Baris lama di basis data yang kolomnya masih {@code NULL} (mis. dibuat sebelum kolom ini
	 * ada) karena itu berperilaku SEBALIKNYA dari format yang baru dibuat lewat layar pengelola,
	 * dan NIS-nya bergeser sebesar {@code mulaiUrutanKe}. Berbeda dari getter serupa di kelas ini,
	 * method ini TIDAK menulis balik nilai bawaan ke field &mdash; jadi ketidakkonsistenan itu
	 * bertahan sampai baris disimpan ulang lewat layar pengelola.</p>
	 *
	 * @return {@code true} bila offset {@code mulaiUrutanKe} ikut ditambahkan; tidak pernah
	 *         {@code null}
	 */
	public Boolean getUrutBerdasarkanNomor() {
		return urutBerdasarkanNomor == null ? true : urutBerdasarkanNomor;
	}

	/**
	 * Menetapkan saklar penambahan offset {@link #getMulaiUrutanKe()}.
	 *
	 * @param urutBerdasarkanNomor {@code true} untuk menambahkan offset; {@code null} akan dibaca
	 *                             getter sebagai {@code true}
	 */
	public void setUrutBerdasarkanNomor(Boolean urutBerdasarkanNomor) {
		this.urutBerdasarkanNomor = urutBerdasarkanNomor;
	}


	/**
	 * Mengembalikan tanggal reset nomor urut kustom (label layar: &quot;Urutan kembali ke-awal
	 * saat tanggal&quot;).
	 *
	 * <p>Dipakai HANYA oleh mode pencacah otomatis. Bila terisi dan tanggalnya sama dengan atau
	 * sudah terlampaui oleh tanggal pendaftaran calon siswa, {@code CommonPSB#getindex} membatasi
	 * hitungan hanya pada pendaftar dengan {@code tanggal >= resetTiap}. Bila {@code null} atau
	 * belum tiba, syarat itu tidak dipasang.</p>
	 *
	 * <p>Kolom bertipe {@code DATE} (tanpa jam). Nilai ini adalah tanggal tunggal, bukan pola
	 * berulang &mdash; menjadikannya reset tahunan menuntut pengubahan manual tiap tahun.</p>
	 *
	 * @return tanggal reset kustom, atau {@code null} bila tidak dipakai
	 */
	@Temporal(TemporalType.DATE)
	public Date getResetTiap() {
		return resetTiap;
	}

	/**
	 * Menetapkan tanggal reset nomor urut kustom.
	 *
	 * @param resetTiap tanggal reset; {@code null} untuk menonaktifkan
	 */
	public void setResetTiap(Date resetTiap) {
		this.resetTiap = resetTiap;
	}

	/**
	 * Mengembalikan sekolah pemilik format ini &mdash; satu-satunya pengait tenant yang dipakai
	 * saat memilih format untuk membangkitkan NIS.
	 *
	 * <p>{@code CommonPSB#onGenerateNis} mencari format dengan {@code Restrictions.eq("sekolah",
	 * calonSiswa.getSekolah())}. Konsekuensinya: baris dengan {@code sekolah} bernilai
	 * {@code null} (&quot;format global&quot;) TIDAK PERNAH terpilih, karena perbandingan
	 * kesamaan terhadap {@code NULL} pada SQL tidak pernah benar. Format yang tersimpan tanpa
	 * sekolah efektif menjadi data mati.</p>
	 *
	 * <p><b>Efek samping:</b> getter menulis balik &mdash; hasil
	 * {@link GeneralValueObject#check(Object)} (resolusi proxy lazy lewat cache identitas/session/
	 * reload) ditugaskan kembali ke field sebelum dikembalikan. {@code check} tidak pernah melempar
	 * dan tidak pernah mengembalikan {@code null} untuk argumen non-null; kegagalan resolusi
	 * bersifat senyap.</p>
	 *
	 * @return sekolah pemilik format, atau {@code null} bila baris tidak terikat sekolah mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik format.
	 *
	 * <p><b>Kasus tepi:</b> objek {@link Sekolah} yang masih transient (ber-{@code id}
	 * {@code null}) diubah menjadi {@code null} secara diam-diam. Pola ini dipakai luas di
	 * repo untuk menolak objek tenant hasil resolusi yang gagal &mdash; tetapi di sini akibatnya
	 * spesifik dan mahal: format yang disimpan saat resolusi tenant gagal akan tersimpan tanpa
	 * sekolah, dan seperti dijelaskan pada {@link #getSekolah()}, format tanpa sekolah tidak akan
	 * pernah dipakai membangkitkan NIS. Gejalanya di lapangan adalah &quot;format sudah dibuat
	 * tetapi NIS tetap keluar dengan pola bawaan&quot;.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau objek ber-{@code id} {@code null} disimpan
	 *                sebagai {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik format.
	 *
	 * <p>Berbeda dari {@link #getSekolah()}, relasi ini TIDAK dipakai sama sekali saat memilih
	 * format untuk pembangkitan NIS; perannya terbatas sebagai penyaring pada layar pengelola.
	 * Ketidaksesuaian antara {@code yayasan} dan {@code sekolah} pada satu baris karena itu tidak
	 * terdeteksi maupun divalidasi di mana pun.</p>
	 *
	 * <p><b>Efek samping:</b> getter menulis balik hasil
	 * {@link GeneralValueObject#check(Object)} ke field, sama seperti {@link #getSekolah()}.</p>
	 *
	 * @return yayasan pemilik format, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik format.
	 *
	 * <p><b>Kasus tepi:</b> objek {@link Yayasan} transient (ber-{@code id} {@code null}) disimpan
	 * sebagai {@code null}. Ini relevan karena {@code SekolahUtil.getYayasan()} pada repo ini
	 * mengembalikan objek ber-{@code id} {@code null} (bukan {@code null}) ketika resolusi tenant
	 * gagal.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau objek ber-{@code id} {@code null} disimpan
	 *                sebagai {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan saklar pemilih mode pencacah (label layar: &quot;Urutankan nomor surat
	 * menggunakan indeks&quot;).
	 *
	 * <p>{@code true} = mode indeks manual, memakai {@link #getNomorIndex()} yang dibaca+dinaikkan
	 * atomik oleh {@link #ambilLaluNaikkanNomorIndex(FormatNis)} setiap pembangkitan. {@code false}
	 * (bawaan) = mode hitung otomatis, nomor diambil dari pencacah dipersistensikan
	 * {@link NisCounter} milik sekolah lewat {@code CommonPSB#ambilNomorUrutOtomatis}.</p>
	 *
	 * <p>Saklar ini juga menentukan apakah kotak isian &quot;Saat ini indeks ke&quot; tampil di
	 * dialog tambah/ubah, dan angka mana yang dipakai {@link #getContohFormat()}. Tidak menulis
	 * balik ke field.</p>
	 *
	 * @return {@code true} untuk mode indeks manual; tidak pernah {@code null}
	 */
	public Boolean getGunakanIndexUrut() {
		return gunakanIndexUrut == null ? false : gunakanIndexUrut;
	}

	/**
	 * Menetapkan mode pencacah.
	 *
	 * @param gunakanIndexUrut {@code true} untuk mode indeks manual; {@code null} dibaca getter
	 *                         sebagai {@code false}
	 */
	public void setGunakanIndexUrut(Boolean gunakanIndexUrut) {
		this.gunakanIndexUrut = gunakanIndexUrut;
	}

	/**
	 * Mengembalikan nilai pencacah pada mode indeks manual (label layar: &quot;Saat ini indeks
	 * ke&quot;) &mdash; yaitu nomor urut yang akan dipakai pada pembangkitan NIS BERIKUTNYA.
	 *
	 * <p>Nilai ini dibaca DAN dinaikkan atomik dalam satu transaksi terkunci oleh
	 * {@link #ambilLaluNaikkanNomorIndex(FormatNis)} yang dipanggil {@code CommonPSB#generateCode}
	 * &mdash; lihat method itu untuk penjelasan perbaikan race condition yang dahulu ada di sini.
	 * Angka ini tetap dapat disetel bebas oleh siapa pun yang boleh mengubah layar Format NIS
	 * &mdash; menurunkannya membuat NIS berikutnya berpotensi mengulang nomor yang sudah pernah
	 * terbit, tetapi sejak {@code ais.common.InitIndex#initNisCounterDanKeunikanSiswa()} terpasang,
	 * unique index {@code (sekolah_id, nomor_induk)} pada {@code sekolah.siswa} DAN pemeriksaan
	 * bentrok+percobaan ulang pada {@code CommonPSB#generateCode} menahan akibatnya &mdash;
	 * generator akan mencoba nomor berikutnya, bukan menerbitkan NIS kembar.</p>
	 *
	 * <p>Tidak menulis balik ke field.</p>
	 *
	 * @return nilai pencacah berikutnya; {@code 1L} bila belum diisi
	 */
	public Long getNomorIndex() {
		return nomorIndex == null ? 1L : nomorIndex;
	}

	/**
	 * Menetapkan nilai pencacah mode indeks manual.
	 *
	 * <p>Dipanggil {@link #ambilLaluNaikkanNomorIndex(FormatNis)} (kenaikan otomatis) dan
	 * {@code FormatNisAction.onSave} (penyetelan manual dari layar pengelola). Tidak ada validasi
	 * bahwa nilai baru lebih besar dari nilai lama.</p>
	 *
	 * @param nomorIndex nilai pencacah berikutnya; {@code null} dibaca getter sebagai {@code 1L}
	 */
	public void setNomorIndex(Long nomorIndex) {
		this.nomorIndex = nomorIndex;
	}

	/**
	 * Mengembalikan status aktif baris format.
	 *
	 * <p>Pencari format pada {@code CommonPSB#onGenerateNis} menerima baris yang {@code aktif =
	 * true} MAUPUN yang {@code aktif} masih {@code null} (kompatibilitas baris lama), lalu
	 * mengambil satu baris dengan {@code id} TERBESAR. Jadi bila sebuah sekolah punya lebih dari
	 * satu format aktif, yang menang adalah yang paling baru dibuat &mdash; bukan pilihan
	 * eksplisit pengguna. Menonaktifkan satu format tidak menjamin format &quot;yang benar&quot;
	 * terpakai kecuali seluruh format lain sekolah itu juga dinonaktifkan.</p>
	 *
	 * <p>Nilai ini dapat dibalik langsung dari kotak centang pada grid layar pengelola, yang
	 * menyimpan perubahan seketika lewat {@code Common.refreshSaveOrUpdate} tanpa dialog
	 * konfirmasi. Tidak menulis balik ke field.</p>
	 *
	 * @return {@code true} bila format aktif; {@code true} pula bila nilainya belum pernah diisi
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif baris format.
	 *
	 * @param aktif {@code true} untuk mengaktifkan; {@code null} dibaca getter sebagai
	 *              {@code true}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
