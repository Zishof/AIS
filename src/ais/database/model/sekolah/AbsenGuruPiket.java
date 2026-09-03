package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Statusabsensi;
import ais.database.model.Tbmuser;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>presensi kehadiran GURU</b> pada satu sesi piket &mdash; tabel
 * {@code sekolah.absen_guru_piket}.
 *
 * <p><b>Satu baris = satu SESI presensi</b>, bukan satu kehadiran seorang guru. Kunci logisnya
 * adalah kombinasi <i>sekolah</i> + <i>tanggal</i> + <i>jam-ke</i> (+ tahun ajaran + semester);
 * status kehadiran SELURUH guru sekolah tersebut pada sesi itu disimpan bersama-sama di dalam
 * SATU kolom teks {@code absensi} yang diserialisasi sendiri (lihat "Format kolom absensi" di
 * bawah). Entity ini <b>tidak punya entity baris-anak</b> sama sekali &mdash; berbeda dengan
 * pasangan {@code AbsenPiket}/{@code AbsenPiketDetail} yang memakai tabel detail sungguhan.</p>
 *
 * <h3>Jangan tertukar dengan {@code AbsenPiket}</h3>
 * <p>Nama keduanya sangat mirip dan sebagian besar kode di kelas ini adalah salinan
 * ({@code toString()}, {@code populateGuru*()}, {@code retreiveAbsensi*()}, {@code populate(...)}
 * berasal dari keluarga {@code Pertemuan}/{@code AbsenPiketDetail}), tetapi <b>subjek yang
 * diabsen berbeda total</b>:</p>
 * <ul>
 *   <li>{@code ais.database.model.sekolah.AbsenPiket} &mdash; presensi <b>SISWA</b> per kelas.
 *       Kolom {@code guru_id}..{@code guru5_id} di sana berisi <b>petugas piket</b> (sampai lima
 *       guru) yang mengepalai sesi; status per siswa disimpan pada
 *       {@code ais.database.model.sekolah.AbsenPiketDetail}.</li>
 *   <li>Entity <b>ini</b> &mdash; presensi <b>GURU</b>. Kolom {@code guru_id} diberi label UI
 *       <b>"Guru Pembuat"</b> (pada {@code absen_guru_piket.zul} maupun pada form tambah/ubah
 *       {@code AbsenGuruPiketAction.init(...)}), yaitu petugas yang <i>membuat</i> sesi presensi,
 *       BUKAN salah satu guru yang diabsen. Guru yang diabsen hanya muncul sebagai id di dalam
 *       kolom teks {@code absensi}.</li>
 * </ul>
 * <p><b>Kolom {@code guru2_id}..{@code guru5_id} pada tabel ini adalah kolom mati.</b> Setter
 * {@code setGuru2(...)}..{@code setGuru5(...)} tidak punya satu pun pemanggil di basis kode, dan
 * getter-nya hanya dipakai oleh {@code populateGuru()}/{@code populateGuruBuNama()} yang juga tak
 * berpemanggil. Kolomnya diwarisi dari salinan {@code AbsenPiket} tempat kelima slot itu memang
 * dipakai. Konsekuensi yang perlu diketahui: statistik "top guru piket" pada
 * {@code ais.action.master.dashboard.admin.DasboardGuru} membaca kelima kolom
 * ({@code PIKET_GURU_COLUMNS}) langsung lewat SQL atas {@code sekolah.absen_guru_piket}, sehingga
 * empat dari lima kolom itu selalu kosong dan angkanya selalu timpang.</p>
 *
 * <h3>Format kolom {@code absensi}</h3>
 * <p>Satu string panjang berisi nol atau lebih rekaman yang dipisah titik koma ({@code ;}); tiap
 * rekaman berisi sembilan ruas yang dipisah koma:</p>
 * <pre>
 *   &lt;idGuru&gt;,&lt;idStatusabsensi&gt;,&lt;kodeStatus&gt;,&lt;namaStatus&gt;,0,&lt;keterangan&gt;,&lt;mulai&gt;,&lt;sampai&gt;,&lt;jenis&gt;
 *      [0]        [1]                 [2]            [3]          [4]     [5]           [6]      [7]        [8]
 * </pre>
 * <ul>
 *   <li>[0] {@code ref} &mdash; id {@link Guru} sebagai TEKS; inilah kunci pencarian seluruh
 *       method {@code retreiveAbsensi*}.</li>
 *   <li>[1] id {@link Statusabsensi}; [2] kodenya ({@code "M"} = Masuk, dst.); [3] namanya.</li>
 *   <li>[4] konstanta literal {@code "0"} &mdash; ruas sisa salinan dari keluarga
 *       {@code Pertemuan} yang tidak pernah dibaca kembali oleh kelas ini.</li>
 *   <li>[5] keterangan bebas, sudah dinetralkan oleh {@link #populate} ({@code ";"} menjadi
 *       {@code "..\n"} dan {@code ","} menjadi {@code "_"} agar tidak merusak pemisah);
 *       pemanggil UI mengembalikan {@code "_"} menjadi {@code ","} saat menampilkan.</li>
 *   <li>[6] jam masuk, [7] jam pulang &mdash; hanya terisi lewat jalur mesin absensi.</li>
 *   <li>[8] jenis/asal rekaman; seluruh pemanggil mengisinya {@code "AbsenGuruPiket"}.</li>
 * </ul>
 * <p>Karena keterangan boleh mengandung koma yang sudah diubah menjadi {@code "_"}, sebagian
 * method pembaca memakai {@code split(",", 9)} (batas sembilan ruas) dan sebagian lagi
 * {@code split(",")} biasa &mdash; lihat catatan pada masing-masing method.</p>
 *
 * <h3>Siapa yang mengisi entity ini</h3>
 * <ol>
 *   <li><b>Layar "Absen Piket Guru"</b> ({@code /pages/master/sekolah/absen_guru_piket.zul},
 *       composer {@code ais.action.master.sekolah.AbsenGuruPiketAction}) &mdash; tombol
 *       Tambah/Ubah membuat kepala sesi (tanggal, jam-ke, sekolah, guru pembuat, keterangan).</li>
 *   <li><b>Panel detail</b> {@code ais.action.master.sekolah.helper.DetailAbsenGuruPiketHelper}
 *       &mdash; grid seluruh guru aktif milik {@link #getSekolah()}, masing-masing dengan radio
 *       status kehadiran + keterangan; setiap perubahan langsung memanggil {@link #populate} lalu
 *       {@code Common.refreshUpdate(...)}. Tersedia juga aksi massal "Semua hadir" dan "Reset"
 *       serta ekspor "Download Kehadiran".</li>
 *   <li><b>Mesin absensi / payroll</b> &mdash; {@code ais.common.CommonPayroll.simpanDetail(...)}
 *       menyalin setiap baris {@code StatuskehadiranKaryawanHarian} milik seorang guru ke sesi
 *       {@code AbsenGuruPiket} hari itu (dibuat bila belum ada), lengkap dengan jam masuk/pulang.
 *       Jalur masuk ke {@code simpanDetail} mencakup layar kehadiran pegawai, layar
 *       {@code ScanBerhasilAction}, REST bertoken {@code ais.action.servlet.api.AbsensiApiAction},
 *       dan REST kios/fingerprint {@code ais.action.master.resources.PosResource} (yang sejak
 *       tambalan 1 Sep 2026 mewajibkan {@code ?secret=} dan gagal-tertutup tanpanya).</li>
 * </ol>
 *
 * <h3>Siapa yang membaca entity ini</h3>
 * <p>{@code ais.action.report.format1.sekolah.LaporanRekapAbsenGuruPiket} (rekap rentang tanggal),
 * {@code LaporanRekapAbsenGuruPiketHarian} (rekap harian, juga dipakai ulang oleh
 * {@code ais.action.master.payroll.KehadiranPegawaiAction}), dan panel-panel piket pada
 * {@code ais.action.master.dashboard.admin.DasboardGuru}.</p>
 *
 * <h3>Konfigurasi yang mengendalikan entity ini</h3>
 * <p>Seluruhnya <b>per sekolah</b>, berprefiks id sekolah, dibaca oleh {@link #jamKes(Long)} dan
 * {@link #jamKe(Long)}: {@code <idSekolah>_absen_piket_guru_jam_ke_1..5} (saklar aktif/tidak,
 * bawaan TIDAK AKTIF), {@code <idSekolah>_absen_piket_guru_waktu_jam_ke_1..5} (jam acuan, bawaan
 * 07.30/09.30/12.30/14.30/16.30) dan {@code <idSekolah>_absen_piket_guru_toleransi_jam_ke_1..5}
 * (toleransi menit, bawaan 30). Layar penyuntingnya adalah tab "Konfigurasi" pada layar yang sama
 * ({@code konfigurasi_absen_piket_guru.zul}). Satu konfigurasi global tambahan,
 * {@code absen_piket_otomatis_belum} (bawaan AKTIF), menentukan status bawaan guru yang belum
 * diabsen di panel detail. <b>Perhatian:</b> pembacaan konfigurasi lewat
 * {@code Common.getKonfigurasi(kunci, default)} <i>menuliskan</i> nilai bawaan ke basis data bila
 * kunci belum ada &mdash; sekadar membuka layar ini menyemai sepuluh baris konfigurasi baru per
 * sekolah.</p>
 *
 * <h3>VERIFIKASI: entity ini TIDAK terjangkau kios pra-otentikasi {@code /welsis}</h3>
 * <p>Temuan "atribusi palsu guru piket" yang tercatat pada audit rantai {@code /welsis}
 * <b>merujuk pada {@code AbsenPiket}, bukan entity ini</b>. Verifikasi yang dilakukan:</p>
 * <ul>
 *   <li>{@code /WEB-INF/baru/modul/welsis/_welsis_service.jsp} &mdash; satu-satunya berkas yang
 *       menjalankan aksi kios &mdash; hanya mengimpor dan menulis {@code AbsenPiket} dan
 *       {@code AbsenPiketDetail}. Baris {@code absenPiket.setGuru(kelas.getGuruPembina())} yang
 *       menjadi sumber temuan "atribusi palsu" mengisi kolom petugas piket pada tabel
 *       <b>{@code sekolah.absen_piket}</b>. Tidak ada satu pun rujukan ke
 *       {@code AbsenGuruPiket}/{@code absen_guru_piket} pada seluruh jalur kios tersebut.</li>
 *   <li>{@code AbsenGuruPiketAction.onAbsen(Event)} memang menyisipkan iframe {@code /welsis.zul}
 *       (yang oleh {@code FilterJSP} dialihkan ke {@code /welsis}), tetapi handler itu
 *       <b>tidak pernah tereksekusi</b>: komponen tujuannya, {@code absenPanel}, tidak ada di
 *       {@code absen_guru_piket.zul} sehingga tidak pernah ter-<i>autowire</i>, dan tidak ada
 *       pendaftaran event {@code onAbsen} di ZUL mana pun. Kalaupun tereksekusi, yang dimuat
 *       adalah layar kios siswa yang menulis {@code AbsenPiket}, bukan entity ini.</li>
 *   <li>Satu-satunya jalur tulis tanpa sesi staf yang pernah menyentuh entity ini adalah REST
 *       kios/fingerprint {@code PosResource} melalui {@code CommonPayroll.simpanDetail(...)}, dan
 *       jalur itu sudah bergerbang {@code pos_api_secret} (fail-closed) sejak 1 Sep 2026.</li>
 * </ul>
 * <p><b>Kesimpulan verifikasi: NEGATIF</b> &mdash; entity ini adalah entity terpisah yang tidak
 * berada di rantai pra-otentikasi {@code /welsis}.</p>
 *
 * <h3>Catatan keamanan &amp; integritas yang berlaku pada entity ini</h3>
 * <ul>
 *   <li><b>Fail-open cakupan tenant.</b> {@code AbsenGuruPiketAction.initCriteria(boolean)} dan
 *       kriteria pada {@code LaporanRekapAbsenGuruPiket} memasang
 *       {@code Restrictions.sqlRestriction("1=1")} untuk filter yayasan/sekolah selama kombobox
 *       terkait belum dipilih &mdash; artinya tampilan bawaan grid dan rekap laporan mencakup
 *       SELURUH baris {@code absen_guru_piket} pada instalasi, lintas sekolah dan lintas yayasan.
 *       Pola yang sama muncul di dasbor: {@code DasboardGuru.applyTahunAjaranSemesterSekolahFilter}
 *       hanya menambahkan {@code Restrictions.eq("sekolah", ...)} bila {@code currentSekolah != null}
 *       dan tidak pernah memfilter {@code yayasan}.</li>
 *   <li><b>Pewarisan hak lewat menu induk.</b> Menu "Absen Piket Guru"
 *       ({@code MenuSnapshotData}, dua pendaftaran: {@code 431/43117} dan {@code 5701/570121})
 *       menunjuk satu ZUL yang berisi EMPAT tab: grid CRUD, dua layar laporan
 *       ({@code LaporanRekapAbsenGuruPiket}, {@code LaporanRekapAbsenGuruPiketHarian}), dan tab
 *       <b>Konfigurasi</b> ({@code konfigurasi_absen_piket_guru.zul}). Ketiga tab terakhir tidak
 *       punya menu sendiri sehingga tidak punya gerbang hak sendiri &mdash; hak BACA atas satu
 *       menu ini otomatis memberi akses laporan lintas guru sekaligus kemampuan mengubah
 *       konfigurasi jam-ke/toleransi presensi sekolah.</li>
 *   <li><b>Unggahan Excel menyertakan kolom {@code id}.</b> {@code AbsenGuruPiketAction} memasang
 *       tombol unggah dengan daftar kolom yang diawali {@code "id"}, pola yang pada entity lain
 *       terbukti memungkinkan penimpaan baris manapun berdasarkan id yang disertakan di berkas.
 *       Di sini tombolnya baru tampil bila pengguna memegang hak CREATE+UPDATE+DELETE sekaligus,
 *       sehingga eskalasinya terbatas &mdash; tetapi cakupan barisnya tetap seluas fail-open di
 *       atas.</li>
 *   <li><b>Tulis saat render (write-on-read).</b> {@code DetailAbsenGuruPiketHelper.DetailPARenderer}
 *       memanggil {@link #populate} + {@code Common.refreshUpdate(...)} untuk setiap guru yang
 *       belum punya rekaman, <i>di dalam</i> {@code render()}. Sekadar membuka panel detail
 *       menulis hingga 50 kali ke baris yang sama dan menciptakan rekaman presensi untuk guru
 *       yang tidak pernah diabsen siapa pun. Bila konfigurasi
 *       {@code absen_piket_otomatis_belum} dimatikan, status yang ditulis adalah
 *       {@code ConstantValues.MASUK} &mdash; membuka layar saja menandai seluruh guru HADIR,
 *       dan karena status berkode {@code "M"} juga memicu blok notifikasi di {@link #populate},
 *       ikut melahirkan satu {@code Thread} baru per guru.</li>
 *   <li><b>Getter destruktif.</b> {@link #getYayasan()} menimpa {@code yayasan} dengan yayasan
 *       milik {@link #getSekolah()} setiap kali dipanggil, dan {@link #getAbsensi()} menulis ulang
 *       isi kolom teks (perbaikan cetak {@code "9.400"} &rarr; {@code "09.40"}). Rinciannya ada
 *       pada javadoc masing-masing method.</li>
 * </ul>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 * <ul>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@code onUpdate()}.</li>
 *   <li><b>Utilitas jam-ke (statis)</b> &mdash; {@link #jamKes(Long)}, {@link #jamKe(Long)}.</li>
 *   <li><b>Identitas &amp; kepemilikan</b> &mdash; {@link #getId()}, {@link #getSekolah()},
 *       {@link #getYayasan()}, {@link #getTahunAjaran()}, {@link #getSemester()},
 *       {@link #getTanggal()}, {@link #getJamke()}.</li>
 *   <li><b>Petugas</b> &mdash; {@link #getGuru()} sampai {@link #getGuru5()},
 *       {@link #populateGuruBuNama()}, {@link #populateGuru()}.</li>
 *   <li><b>Blob presensi</b> &mdash; {@link #getAbsensi()}, tujuh method {@code retreiveAbsensi*},
 *       dan {@link #populate}.</li>
 *   <li><b>Lain-lain</b> &mdash; {@link #getKeterangan()}, {@link #toString()}.</li>
 * </ul>
 *
 * <p><b>Catatan pewarisan.</b> Kelas ini memperluas {@link ais.database.model.GeneralValueObject},
 * yang <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa.
 * Hibernate karena itu tidak memetakan properti apa pun milik induk, sehingga deklarasi ULANG
 * {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} di kelas ini adalah
 * <b>keharusan teknis</b>, bukan duplikasi yang keliru.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.AbsenPiket
 * @see ais.database.model.sekolah.AbsenPiketDetail
 * @see ais.database.model.Statusabsensi
 * @see ais.database.model.sekolah.Guru
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "absen_guru_piket", schema = "sekolah")
public class AbsenGuruPiket extends GeneralValueObject {

	/**
	 * Label semester genap. Konstanta ini <b>tidak dipakai di mana pun</b> (baik di kelas ini
	 * maupun di seluruh basis kode) &mdash; sisa salinan dari entity sejenis. Layar pemakai
	 * memakai {@code ais.database.model.Perkuliahan#GENAP} untuk label yang sama. Semester pada
	 * entity ini disimpan sebagai angka ({@code 2} = genap), lihat {@link #getSemester()}.
	 */
	public static final String GENAP = "Genap";
	/**
	 * Label semester ganjil. Sama seperti {@link #GENAP}: tidak dipakai di mana pun; semester
	 * pada entity ini disimpan sebagai angka ({@code 1} = ganjil).
	 */
	public static final String GANJIL = "Ganjil";

	/**
	 * Versi serialisasi Java. Dipertahankan apa adanya agar objek yang pernah diserialisasi
	 * (mis. ke dalam sesi ZK) tetap kompatibel.
	 */
	private static final long serialVersionUID = 7154228487700348608L;
	/**
	 * Kunci utama baris, dipetakan ke kolom {@code id} ({@code IDENTITY}). Dideklarasikan ulang di
	 * kelas ini karena {@link ais.database.model.GeneralValueObject} bukan superclass ber-mapping.
	 * Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir mengubah baris (jejak audit). Diisi oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;
	/**
	 * Identitas teknis ({@code user_id}) pengguna yang terakhir mengubah baris (jejak audit).
	 * Lihat {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas teknis pengguna yang terakhir mengubah baris ini.
	 *
	 * @return {@code user_id} pengubah terakhir, atau {@code null} bila baris belum pernah
	 *         melewati interceptor audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas teknis pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> setter ini <b>menolak diam-diam</b> nilai {@code null} maupun teks
	 * kosong/spasi &mdash; nilai lama dipertahankan. Jejak audit karena itu tidak pernah bisa
	 * dikosongkan lewat setter ini, dan pemanggil yang mengira sudah membersihkan jejak akan
	 * keliru.</p>
	 *
	 * @param olehId {@code user_id} pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pengguna pengubah terakhir. Sama seperti
	 * {@link #setOlehId(String)}, nilai {@code null}/kosong <b>diabaikan diam-diam</b>.
	 *
	 * @param oleh nama pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengubah terakhir, atau {@code null} bila belum pernah tercatat
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: menyerahkan pengisian jejak audit
	 * ({@link #setOleh(String)}/{@link #setOlehId(String)}/{@link #setTanggal_dirubah(Date)}) ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor} tepat sebelum Hibernate menjalankan
	 * {@code UPDATE}. Tidak dipanggil manual dari kode mana pun.
	 *
	 * <p><b>Perhatian pembaca:</b> pada baris fisik yang sama juga dideklarasikan field
	 * {@code tanggal_dirubah}, diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru selalu punya stempel waktu meski belum
	 * pernah di-{@code update}. Tata letak satu baris ini dipertahankan apa adanya.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya hanya dipanggil oleh
	 * {@code AuditTimestampInterceptor}; menyetelnya manual akan tertimpa pada {@code UPDATE}
	 * berikutnya.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan; boleh {@code null}
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini (jejak audit, bukan tanggal sesi
	 * presensi &mdash; untuk itu pakai {@link #getTanggal()}).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} pada objek yang baru dibuat
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengembalikan daftar nomor sesi presensi ("jam ke") yang <b>diaktifkan</b> untuk satu
	 * sekolah, dibaca dari konfigurasi {@code <idSekolah>_absen_piket_guru_jam_ke_1} sampai
	 * {@code ..._jam_ke_5}.
	 *
	 * <p>Setiap saklar dibaca dengan {@code Common.bolehKonfigurasi(kunci, Konfigurasi.TIDAK_AKTIF)}
	 * sehingga nilai bawaannya adalah <b>tidak aktif</b>: pada instalasi yang belum dikonfigurasi
	 * method ini mengembalikan daftar KOSONG, dan seluruh pemanggil memperlakukan daftar kosong
	 * sebagai "fitur jam-ke tidak dipakai" (mode satu sesi per hari dengan {@code jamke = 0}).</p>
	 *
	 * <p><b>Efek samping:</b> pembacaan konfigurasi lewat {@code Common.getKonfigurasi} menuliskan
	 * baris konfigurasi bernilai bawaan ke basis data bila kuncinya belum ada &mdash; memanggil
	 * method ini pada sekolah baru menyemai lima baris konfigurasi.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code AbsenGuruPiketAction.init(...)} (mengisi kombobox "Jam Ke"
	 * pada form tambah/ubah, dijalankan ulang setiap kombobox Sekolah berubah) dan
	 * {@code LaporanRekapAbsenGuruPiket} (menentukan kolom rekap per sesi).</p>
	 *
	 * <p><b>Non-obvious:</b> parameter {@code sekolahId} dipakai apa adanya sebagai bagian teks
	 * kunci konfigurasi. Pemanggil yang tidak punya sekolah mengirim {@code 0L}, sehingga kunci
	 * yang terbentuk adalah {@code "0_absen_piket_guru_jam_ke_1"} dan seterusnya &mdash; sebuah
	 * "sekolah nol" semu yang punya konfigurasinya sendiri di basis data.</p>
	 *
	 * @param sekolahId id sekolah pemilik konfigurasi; boleh {@code 0L} untuk konteks tanpa
	 *                  sekolah, tidak boleh {@code null} (akan menghasilkan kunci berawalan
	 *                  {@code "null_"}, bukan {@code NullPointerException})
	 * @return daftar nomor sesi aktif urut menaik (subset dari 1..5); kosong bila tidak ada yang
	 *         aktif, tidak pernah {@code null}
	 * @see #jamKe(Long)
	 */
	public static List<Integer> jamKes(Long sekolahId) {
		List<Integer> integers = new ArrayList<Integer>();
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_1", Konfigurasi.TIDAK_AKTIF)) {
			integers.add(1);
		}
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_2", Konfigurasi.TIDAK_AKTIF)) {
			integers.add(2);
		}
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_3", Konfigurasi.TIDAK_AKTIF)) {
			integers.add(3);
		}
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_4", Konfigurasi.TIDAK_AKTIF)) {
			integers.add(4);
		}
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_5", Konfigurasi.TIDAK_AKTIF)) {
			integers.add(5);
		}
		return integers;
	}

	/**
	 * Menentukan sesi presensi ("jam ke") mana yang <b>sedang berlangsung SEKARANG</b> pada satu
	 * sekolah, berdasarkan jam server saat pemanggilan.
	 *
	 * <p>Untuk setiap sesi 1..5 yang saklarnya aktif
	 * ({@code <idSekolah>_absen_piket_guru_jam_ke_N}), method ini menghitung selisih menit antara
	 * jam sekarang dan jam acuan sesi tersebut
	 * ({@code <idSekolah>_absen_piket_guru_waktu_jam_ke_N}, bawaan berturut-turut
	 * {@code 07.30}/{@code 09.30}/{@code 12.30}/{@code 14.30}/{@code 16.30}) memakai
	 * {@code AbsenPiketDetail.waktu(Date, String)}, lalu membandingkan nilai mutlaknya dengan
	 * ambang toleransi {@code <idSekolah>_absen_piket_guru_toleransi_jam_ke_N} (bawaan
	 * {@code 30} menit). Sesi yang lolos ambang menetapkan nilai kembalian.</p>
	 *
	 * <p><b>Non-obvious &mdash; sesi terakhir yang menang, bukan yang terdekat.</b> Kelima blok
	 * dievaluasi berurutan tanpa {@code else}/{@code break} dan masing-masing menimpa variabel
	 * hasil. Bila dua jendela toleransi bertumpang tindih (mis. acuan 09.30 dan 12.30 dengan
	 * toleransi 120 menit), yang dipakai selalu sesi <b>bernomor tertinggi</b> yang cocok, bukan
	 * yang selisihnya paling kecil.</p>
	 *
	 * <p><b>Non-obvious &mdash; toleransi bersifat dua arah.</b> Karena yang dibandingkan adalah
	 * {@code Math.abs(selisih)}, jam acuan berperilaku sebagai TITIK TENGAH: absen 29 menit
	 * <i>sebelum</i> jam acuan sama-sama diterima dengan absen 29 menit sesudahnya.</p>
	 *
	 * <p><b>Non-obvious &mdash; komponen tanggal diabaikan.</b> {@code AbsenPiketDetail.waktu}
	 * menormalkan kedua sisi ke tanggal hari ini, dan method ini selalu memakai
	 * {@code WaktuUtil.getDate()} (waktu server saat dipanggil), bukan tanggal baris yang sedang
	 * diproses. Konsekuensinya: penyimpanan susulan/koreksi kehadiran kemarin tetap memperoleh
	 * jam-ke menurut jam dinding SAAT PENYIMPANAN.</p>
	 *
	 * <p><b>Efek samping:</b> menulis lima baris konfigurasi bawaan bila belum ada (lihat
	 * {@link #jamKes(Long)}), dan mencetak baris diagnostik ke {@code System.out} untuk setiap
	 * sesi aktif. Setiap blok dibungkus {@code try}/{@code catch} sendiri: konfigurasi jam atau
	 * toleransi yang tidak dapat diurai hanya dicatat ke {@code ErrorAuditUtil} dan sesi
	 * bersangkutan dilewati, tidak menggagalkan penyimpanan kehadiran.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ais.common.CommonPayroll.simpanDetail(...)} &mdash; untuk
	 * mencari/membuat baris {@code AbsenGuruPiket} milik sesi yang sedang berjalan saat sebuah
	 * kehadiran guru dicatat (layar kehadiran pegawai, {@code ScanBerhasilAction}, REST bertoken
	 * {@code AbsensiApiAction}, dan REST kios/fingerprint {@code PosResource} yang bergerbang
	 * {@code pos_api_secret}).</p>
	 *
	 * @param sekolahId id sekolah pemilik konfigurasi; pemanggil mengirim {@code 0L} bila guru
	 *                  tidak punya sekolah
	 * @return nomor sesi yang sedang berlangsung (1..5), atau {@code 0} bila tidak ada sesi aktif
	 *         yang cocok &mdash; nilai {@code 0} inilah yang dipakai sebagai "tanpa jam-ke"
	 * @see #jamKes(Long)
	 * @see ais.database.model.sekolah.AbsenPiketDetail#waktu(Date, String)
	 */
	public static Integer jamKe(Long sekolahId) {
		Integer jamke = 0;
		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_1", Konfigurasi.TIDAK_AKTIF)) {

			try {
				double waktu = AbsenPiketDetail.waktu(WaktuUtil.getDate(), Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_waktu_jam_ke_1", "07.30").getNilai().trim());

				double wk = Double.parseDouble(Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_toleransi_jam_ke_1", "30").getNilai().trim());

				if (Math.abs(waktu) < wk) {
					jamke = 1;
				}
				System.out.println("_absen_piket_guru_jam_ke_1 " + waktu + " wk " + wk + " jamke " + jamke);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/AbsenGuruPiket.java:126");
			}

		}

		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_2", Konfigurasi.TIDAK_AKTIF)) {

			try {
				double waktu = AbsenPiketDetail.waktu(WaktuUtil.getDate(), Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_waktu_jam_ke_2", "09.30").getNilai().trim());

				double wk = Double.parseDouble(Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_toleransi_jam_ke_2", "30").getNilai().trim());

				if (Math.abs(waktu) < wk) {
					jamke = 2;
				}
				System.out.println("_absen_piket_guru_jam_ke_2 " + waktu + " wk " + wk + " jamke " + jamke);

			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/AbsenGuruPiket.java:146");
			}

		}

		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_3", Konfigurasi.TIDAK_AKTIF)) {

			try {
				double waktu = AbsenPiketDetail.waktu(WaktuUtil.getDate(), Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_waktu_jam_ke_3", "12.30").getNilai().trim());
				double wk = Double.parseDouble(Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_toleransi_jam_ke_3", "30").getNilai().trim());

				if (Math.abs(waktu) < wk) {
					jamke = 3;
				}
				System.out.println("_absen_piket_guru_jam_ke_3 " + waktu + " wk " + wk + " jamke " + jamke);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/AbsenGuruPiket.java:164");
			}

		}

		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_4", Konfigurasi.TIDAK_AKTIF)) {

			try {
				double waktu = AbsenPiketDetail.waktu(WaktuUtil.getDate(), Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_waktu_jam_ke_4", "14.30").getNilai().trim());
				double wk = Double.parseDouble(Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_toleransi_jam_ke_4", "30").getNilai().trim());

				if (Math.abs(waktu) < wk) {
					jamke = 4;
				}
				System.out.println("_absen_piket_guru_jam_ke_4 " + waktu + " wk " + wk + " jamke " + jamke);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/AbsenGuruPiket.java:182");
			}

		}

		if (Common.bolehKonfigurasi(sekolahId + "_absen_piket_guru_jam_ke_5", Konfigurasi.TIDAK_AKTIF)) {

			try {
				double waktu = AbsenPiketDetail.waktu(WaktuUtil.getDate(), Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_waktu_jam_ke_5", "16.30").getNilai().trim());
				double wk = Double.parseDouble(Common
						.getKonfigurasi(sekolahId + "_absen_piket_guru_toleransi_jam_ke_5", "30").getNilai().trim());

				if (Math.abs(waktu) < wk) {
					jamke = 5;
				}
				System.out.println("_absen_piket_guru_jam_ke_5 " + waktu + " wk " + wk + " jamke " + jamke);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/sekolah/AbsenGuruPiket.java:200");
			}

		}

		return jamke;
	}

	/** Tanggal (dan waktu) sesi presensi. Lihat {@link #getTanggal()}. */
	private Date tanggal;
	/**
	 * Guru pembuat sesi (kolom {@code guru_id}) &mdash; label UI "Guru Pembuat", BUKAN guru yang
	 * diabsen. Lihat {@link #getGuru()}.
	 */
	private Guru guru;
	/** Slot petugas ke-2 (kolom {@code guru2_id}) &mdash; <b>kolom mati</b>, tidak pernah diisi. Lihat {@link #getGuru2()}. */
	private Guru guru2;
	/** Slot petugas ke-3 (kolom {@code guru3_id}) &mdash; <b>kolom mati</b>. Lihat {@link #getGuru3()}. */
	private Guru guru3;
	/** Slot petugas ke-4 (kolom {@code guru4_id}) &mdash; <b>kolom mati</b>. Lihat {@link #getGuru4()}. */
	private Guru guru4;
	/** Slot petugas ke-5 (kolom {@code guru5_id}) &mdash; <b>kolom mati</b>. Lihat {@link #getGuru5()}. */
	private Guru guru5;
	/** Sekolah pemilik sesi (kolom {@code sekolah_id}); menentukan daftar guru yang diabsen. Lihat {@link #getSekolah()}. */
	private Sekolah sekolah;
	/** Yayasan pemilik sesi (kolom {@code yayasan_id}); turunan dari {@link #sekolah}. Lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Semester pemilik sesi: {@code 1} ganjil, {@code 2} genap. Lihat {@link #getSemester()}. */
	private Integer semester;
	/** Tahun ajaran pemilik sesi, format {@code "2025/2026"}. Lihat {@link #getTahunAjaran()}. */
	private String tahunAjaran;
	/** Catatan bebas tingkat sesi (bukan per guru). Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/**
	 * Blob presensi seluruh guru pada sesi ini, diserialisasi ke satu kolom {@code text}. Format
	 * sembilan-ruas dijelaskan pada javadoc kelas. Lihat {@link #getAbsensi()}.
	 */
	private String absensi;
	/** Nomor sesi dalam sehari ({@code 0} bila fitur jam-ke tidak dipakai). Lihat {@link #getJamke()}. */
	private Integer jamke;

	/**
	 * Representasi teks ringkas untuk keperluan diagnostik/log.
	 *
	 * <p><b>Non-obvious:</b> memakai getter, sehingga pemanggilan {@code toString()} ikut memicu
	 * seluruh efek samping getter tersebut &mdash; {@link #getGuru()} me-resolve proxy lazy,
	 * sedangkan {@link #getSemester()} dan {@link #getTahunAjaran()} MENGISI field yang masih
	 * {@code null} dengan semester/tahun ajaran berjalan. Mencetak objek ini ke log dapat
	 * mengubah keadaan objek.</p>
	 *
	 * @return teks berbentuk {@code "<id>_<guru>_<semester>_<tahunAjaran>"}; bagian {@code guru}
	 *         adalah {@code Guru.toString()} atau {@code "null"} bila guru pembuat kosong
	 */
	public String toString() {
		return getId() + "_" + getGuru() + "_" + getSemester() + "_" + getTahunAjaran();
	}

	/**
	 * Mengumpulkan seluruh slot petugas yang terisi ({@link #getGuru()} sampai
	 * {@link #getGuru5()}) menjadi satu daftar berurutan.
	 *
	 * <p><b>Tidak punya pemanggil di basis kode.</b> Method ini adalah salinan dari
	 * {@code AbsenPiket}/{@code JadwalPelajaran} (tempat kelima slot memang dipakai dan dirender
	 * lewat {@code Common.displayGuruAbsenPiket}). Karena {@code setGuru2(...)} sampai
	 * {@code setGuru5(...)} tidak pernah dipanggil pada entity ini, hasilnya praktis tidak pernah
	 * berisi lebih dari satu elemen.</p>
	 *
	 * <p><b>Efek samping:</b> setiap getter yang dipanggil me-resolve proxy lazy lewat
	 * {@code check(...)} dan dapat mengganti isi field dengan instance kanonik.</p>
	 *
	 * @return daftar petugas yang terisi urut slot 1..5; kosong (bukan {@code null}) bila tidak
	 *         ada yang terisi
	 * @see #populateGuru()
	 */
	public List<Guru> populateGuruBuNama() {
		List<Guru> gurus = new ArrayList<Guru>();

		if (getGuru() != null) {
			gurus.add(getGuru());
		}
		if (getGuru2() != null) {
			gurus.add(getGuru2());
		}
		if (getGuru3() != null) {
			gurus.add(getGuru3());
		}
		if (getGuru4() != null) {
			gurus.add(getGuru4());
		}
		if (getGuru5() != null) {
			gurus.add(getGuru5());
		}

		return gurus;
	}

	/**
	 * Varian {@link #populateGuruBuNama()} yang mengembalikan peta ber-deduplikasi, berkunci
	 * {@code "<idSesi>-<idGuru>"} sehingga guru yang sama terdaftar di dua slot hanya muncul
	 * sekali.
	 *
	 * <p><b>Tidak punya pemanggil di basis kode</b> (sama seperti {@link #populateGuruBuNama()}).
	 * Bentuk kuncinya menyertakan id sesi karena di entity asalnya peta dari beberapa sesi
	 * digabung ke dalam satu {@code Map}.</p>
	 *
	 * <p><b>Non-obvious:</b> implementasinya {@link HashMap}, jadi urutan iterasi tidak
	 * ditentukan &mdash; jangan diandalkan untuk urutan tampil. Bila {@link #getId()} masih
	 * {@code null} (sesi belum tersimpan), kuncinya berawalan teks {@code "null-"}.</p>
	 *
	 * @return peta petugas yang terisi; kosong (bukan {@code null}) bila tidak ada yang terisi
	 * @see #populateGuruBuNama()
	 */
	public Map<String, Guru> populateGuru() {
		Map<String, Guru> gurus = new HashMap<String, Guru>();

		if (getGuru() != null) {
			gurus.put(getId() + "-" + getGuru().getId(), getGuru());
		}
		if (getGuru2() != null) {
			gurus.put(getId() + "-" + getGuru2().getId(), getGuru2());
		}
		if (getGuru3() != null) {
			gurus.put(getId() + "-" + getGuru3().getId(), getGuru3());
		}
		if (getGuru4() != null) {
			gurus.put(getId() + "-" + getGuru4().getId(), getGuru4());
		}
		if (getGuru5() != null) {
			gurus.put(getId() + "-" + getGuru5().getId(), getGuru5());
		}

		return gurus;
	}

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA. Objek yang dihasilkan sudah punya
	 * {@code tanggal_dirubah} berisi waktu server; {@link #getTanggal()},
	 * {@link #getSemester()} dan {@link #getTahunAjaran()} akan mengisi dirinya sendiri saat
	 * pertama dibaca.
	 */
	public AbsenGuruPiket() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * @return id baris, atau {@code null} bila sesi belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama baris. Normalnya diisi Hibernate; disetel manual hanya oleh jalur
	 * unggah/impor Excel yang menyertakan kolom {@code "id"}.
	 *
	 * @param id kunci utama; {@code null} berarti baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan sekolah pemilik sesi presensi ini.
	 *
	 * <p>Nilai ini menentukan <b>daftar guru yang diabsen</b>: panel detail
	 * ({@code DetailAbsenGuruPiketHelper.initCriteria}) dan laporan rekap mencari
	 * {@code Guru} dengan {@code sekolah = } nilai ini. Sekolah yang {@code null} berarti panel
	 * detail tidak menemukan guru mana pun.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} milik
	 * {@link ais.database.model.GeneralValueObject} dan <b>menugaskan ulang</b> hasilnya ke field
	 * &mdash; proxy lazy diganti instance yang sudah terinisialisasi (atau instance kanonik dari
	 * {@code EntityIdentityMap}). Ini resolusi identitas, bukan perubahan data.</p>
	 *
	 * @return sekolah pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik sesi.
	 *
	 * <p><b>Non-obvious:</b> objek sekolah yang belum tersimpan (ber-{@code id} {@code null})
	 * diperlakukan sama dengan {@code null} &mdash; relasi dikosongkan, bukan disimpan sebagai
	 * transient. Idiom ini konsisten di seluruh entity dan mencegah {@code CascadeType.PERSIST}
	 * ikut membuat baris {@code Sekolah} baru secara tak sengaja.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau ber-id {@code null} mengosongkan relasi
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik sesi presensi ini.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; menulis ulang field.</b> Sebelum mengembalikan nilai, method
	 * ini memanggil {@link #getSekolah()} dan, bila sekolahnya tidak {@code null},
	 * <b>menimpa</b> {@code yayasan} dengan {@code sekolah.getYayasan()}. Nilai yayasan yang
	 * tersimpan di kolom {@code yayasan_id} karena itu tidak pernah menang atas yayasan milik
	 * sekolah: begitu grid, laporan, atau ekspor merender baris ini dan objeknya kemudian
	 * ikut ter-{@code flush}/{@code update} (mis. oleh {@code Common.refreshUpdate} yang dipanggil
	 * panel detail), kolom {@code yayasan_id} di basis data ikut berubah &mdash; baris berpindah
	 * tenant tanpa ada aksi pengguna. Bila {@link #getSekolah()} {@code null}, nilai lama
	 * dipertahankan.</p>
	 *
	 * <p><b>Efek samping tambahan:</b> {@code check(...)} juga diterapkan pada hasil akhir, dengan
	 * konsekuensi resolusi proxy yang sama seperti {@link #getSekolah()}.</p>
	 *
	 * @return yayasan pemilik (diturunkan dari sekolah bila ada), atau {@code null}
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
	 * Menyetel yayasan pemilik sesi. Sama seperti {@link #setSekolah(Sekolah)}, objek ber-id
	 * {@code null} diperlakukan sebagai {@code null}.
	 *
	 * <p><b>Non-obvious:</b> nilai yang disetel di sini bisa segera tertimpa oleh
	 * {@link #getYayasan()} pada pembacaan berikutnya bila {@link #getSekolah()} terisi.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau ber-id {@code null} mengosongkan relasi
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan semester pemilik sesi ({@code 1} ganjil, {@code 2} genap).
	 *
	 * <p><b>Getter yang menulis field (isian malas).</b> Bila {@code semester} masih {@code null},
	 * method ini MENGISINYA dengan semester berjalan menurut jam server
	 * ({@code Common.isNowSemensterGanjil()}) lalu mengembalikannya. Karena kolomnya
	 * {@code nullable = false}, isian malas inilah yang mencegah kegagalan {@code INSERT} pada
	 * baris yang dibuat program tanpa menyetel semester. Konsekuensinya: baris yang diselamatkan
	 * dengan cara ini memperoleh semester <b>saat disimpan</b>, bukan semester milik
	 * {@link #getTanggal()} &mdash; koreksi kehadiran lintas semester bisa tercatat di semester
	 * yang keliru.</p>
	 *
	 * @return {@code 1} atau {@code 2}; tidak pernah {@code null}
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menyetel semester pemilik sesi.
	 *
	 * @param semester {@code 1} untuk ganjil, {@code 2} untuk genap; {@code null} akan diisi
	 *                 otomatis oleh {@link #getSemester()} pada pembacaan berikutnya
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun ajaran pemilik sesi (format {@code "2025/2026"}, maksimal 9 karakter).
	 *
	 * <p><b>Getter yang menulis field (isian malas)</b> &mdash; persis seperti
	 * {@link #getSemester()}: bila masih {@code null}, diisi dengan tahun akademik berjalan
	 * ({@code Common.getCurrentTahunAkademik()}) agar kolom {@code nullable = false} tidak
	 * gagal saat {@code INSERT}, dengan konsekuensi yang sama (nilai mengikuti saat penyimpanan,
	 * bukan {@link #getTanggal()}).</p>
	 *
	 * @return tahun ajaran; tidak pernah {@code null}
	 */
	@Column(name = "tahun_ajaran", nullable = false, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * Menyetel tahun ajaran pemilik sesi.
	 *
	 * @param tahunAjaran tahun ajaran format {@code "2025/2026"} (maksimal 9 karakter);
	 *                    {@code null} akan diisi otomatis oleh {@link #getTahunAjaran()}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * Mengembalikan <b>guru pembuat</b> sesi presensi (kolom {@code guru_id}).
	 *
	 * <p><b>Penting:</b> ini BUKAN guru yang diabsen. Label kolom pada grid dan pada form
	 * tambah/ubah {@code AbsenGuruPiketAction} sama-sama berbunyi "Guru Pembuat" &mdash; yaitu
	 * petugas yang membuka/mencatat sesi. Kehadiran para guru tersimpan di dalam blob
	 * {@link #getAbsensi()}. Pada baris yang dibuat otomatis oleh
	 * {@code CommonPayroll.simpanDetail(...)} field ini tidak pernah diisi, sehingga kolom
	 * "Guru Pembuat" kosong untuk seluruh sesi hasil mesin absensi.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@code check(...)} dengan penugasan ulang
	 * field, sama seperti {@link #getSekolah()}.</p>
	 *
	 * @return guru pembuat sesi, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_id")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menyetel guru pembuat sesi.
	 *
	 * <p><b>Non-obvious:</b> berbeda dari {@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)},
	 * setter ini menerima objek apa adanya &mdash; termasuk {@code Guru} transient ber-id
	 * {@code null}, yang bersama {@code CascadeType.PERSIST} dapat ikut menyisipkan baris
	 * {@code Guru} baru saat sesi disimpan.</p>
	 *
	 * @param guru guru pembuat; boleh {@code null}
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan catatan bebas tingkat sesi (kolom "Keterangan" pada form dan grid).
	 *
	 * <p>Berbeda dengan keterangan per guru yang tersimpan di ruas [5] blob {@link #getAbsensi()}
	 * dan dibaca lewat {@link #retreiveAbsensiKeterangan(String)}.</p>
	 *
	 * @return keterangan sesi, atau {@code null} bila tidak diisi
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas tingkat sesi.
	 *
	 * @param keterangan keterangan sesi; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan slot petugas ke-2 (kolom {@code guru2_id}).
	 *
	 * <p><b>Kolom mati pada entity ini</b> &mdash; {@link #setGuru2(Guru)} tidak punya pemanggil,
	 * jadi nilainya selalu {@code null} kecuali diisi langsung lewat SQL atau unggahan. Slot ini
	 * berguna pada {@code AbsenPiket}, bukan di sini. Lihat javadoc kelas.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy dengan penugasan ulang field.</p>
	 *
	 * @return petugas slot 2, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru2_id")
	public Guru getGuru2() {
		guru2 = check(guru2);
		return guru2;
	}

	/**
	 * Menyetel slot petugas ke-2. Tidak ada pemanggil di basis kode.
	 *
	 * @param guru2 petugas slot 2; boleh {@code null}
	 */
	public void setGuru2(Guru guru2) {
		this.guru2 = guru2;
	}

	/**
	 * Mengembalikan slot petugas ke-3 (kolom {@code guru3_id}). Kolom mati &mdash; lihat catatan
	 * pada {@link #getGuru2()}.
	 *
	 * @return petugas slot 3, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru3_id")
	public Guru getGuru3() {
		guru3 = check(guru3);
		return guru3;
	}

	/**
	 * Menyetel slot petugas ke-3. Tidak ada pemanggil di basis kode.
	 *
	 * @param guru3 petugas slot 3; boleh {@code null}
	 */
	public void setGuru3(Guru guru3) {
		this.guru3 = guru3;
	}

	/**
	 * Mengembalikan slot petugas ke-4 (kolom {@code guru4_id}). Kolom mati &mdash; lihat catatan
	 * pada {@link #getGuru2()}.
	 *
	 * @return petugas slot 4, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru4_id")
	public Guru getGuru4() {
		guru4 = check(guru4);
		return guru4;
	}

	/**
	 * Menyetel slot petugas ke-4. Tidak ada pemanggil di basis kode.
	 *
	 * @param guru4 petugas slot 4; boleh {@code null}
	 */
	public void setGuru4(Guru guru4) {
		this.guru4 = guru4;
	}

	/**
	 * Mengembalikan slot petugas ke-5 (kolom {@code guru5_id}). Kolom mati &mdash; lihat catatan
	 * pada {@link #getGuru2()}.
	 *
	 * @return petugas slot 5, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru5_id")
	public Guru getGuru5() {
		guru5 = check(guru5);
		return guru5;
	}

	/**
	 * Menyetel slot petugas ke-5. Tidak ada pemanggil di basis kode.
	 *
	 * @param guru5 petugas slot 5; boleh {@code null}
	 */
	public void setGuru5(Guru guru5) {
		this.guru5 = guru5;
	}

	/**
	 * Mengembalikan tanggal (dan waktu) sesi presensi ini.
	 *
	 * <p><b>Non-obvious &mdash; menyamarkan nilai kosong.</b> Bila kolom {@code tanggal} masih
	 * {@code null}, method ini mengembalikan <b>waktu server saat ini</b>
	 * ({@code WaktuUtil.getDate()}) tanpa menyimpannya ke field. Akibatnya: (a) baris tanpa
	 * tanggal tidak pernah terlihat kosong di layar/laporan, melainkan tampak "hari ini";
	 * (b) berbeda dengan {@link #getSemester()}/{@link #getTahunAjaran()}, di sini TIDAK ada
	 * penulisan balik, sehingga dua pembacaan berurutan bisa mengembalikan nilai yang berbeda
	 * (jam berjalan) selama field tetap {@code null}.</p>
	 *
	 * <p>Kolom ini juga menjadi salah satu kunci pencarian sesi: {@code CommonPayroll} dan
	 * laporan rekap memfilternya lewat {@code date(this_.tanggal)} pada SQL mentah.</p>
	 *
	 * @return tanggal sesi, atau waktu sekarang bila kolomnya kosong; tidak pernah {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * Menyetel tanggal (dan waktu) sesi presensi.
	 *
	 * @param tanggal tanggal sesi; {@code null} akan disamarkan sebagai "sekarang" oleh
	 *                {@link #getTanggal()}
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan blob presensi seluruh guru pada sesi ini (kolom {@code text}
	 * {@code absensi}), sudah di-{@code trim}. Format sembilan-ruas dijelaskan pada javadoc
	 * kelas.
	 *
	 * <p><b>GETTER DESTRUKTIF &mdash; menulis ulang isi kolom.</b> Bila blob mengandung
	 * substring {@code "9.400"}, seluruh kemunculannya <b>diganti permanen</b> menjadi
	 * {@code "09.40"} pada field sebelum nilai dikembalikan. Ini tambalan cetak untuk jam yang
	 * pernah tersimpan salah format; karena penggantian dilakukan pada field (bukan pada salinan),
	 * perubahannya ikut tersimpan begitu objek di-{@code flush}/{@code update} berikutnya &mdash;
	 * dan pemanggil terbesarnya, panel detail, memang memanggil {@code Common.refreshUpdate(...)}
	 * segera sesudahnya. Penggantian bersifat substring polos: nilai lain yang kebetulan memuat
	 * {@code "9.400"} (mis. keterangan bebas yang menyebut angka itu) ikut berubah.</p>
	 *
	 * <p><b>Kontrak nilai kosong:</b> mengembalikan string kosong {@code ""}, bukan {@code null},
	 * sehingga seluruh pemanggil dapat langsung memanggil {@code split(";")}.</p>
	 *
	 * @return blob presensi; {@code ""} bila belum ada rekaman, tidak pernah {@code null}
	 * @see #populate(String, Statusabsensi, String, String, String, String)
	 */
	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		if (absensi != null && StringUtils.contains(absensi, "9.400")) {
			absensi = org.apache.commons.lang3.StringUtils.replace(absensi, "9.400", "09.40");
		}
		return absensi == null ? "" : absensi.trim();
	}

	/**
	 * Menyetel blob presensi mentah, menimpa seluruh rekaman yang ada.
	 *
	 * <p><b>Peringatan:</b> tidak ada validasi format sama sekali. Jalur normal untuk mengubah
	 * kehadiran seorang guru adalah {@link #populate(String, Statusabsensi, String, String, String, String)},
	 * yang memelihara rekaman guru lain. Setter ini dipakai jalur unggah/impor dan reset penuh.</p>
	 *
	 * @param absensi blob presensi berformat sembilan-ruas dipisah {@code ";"}; boleh {@code null}
	 */
	public void setAbsensi(String absensi) {
		this.absensi = absensi;
	}

	/**
	 * Mengambil <b>kode</b> status kehadiran (ruas [2], mis. {@code "M"} untuk Masuk) milik satu
	 * guru dari blob presensi.
	 *
	 * <p><b>Tidak punya pemanggil di basis kode</b> &mdash; salinan dari keluarga
	 * {@code Pertemuan}, tempat method senama memang dipakai luas. Pemakai entity ini membaca
	 * status lewat {@link #retreiveAbsensiId(String)} lalu memetakannya ke
	 * {@link Statusabsensi} melalui cache {@code ConstantValues}.</p>
	 *
	 * <p><b>Non-obvious:</b> memakai {@code split(",")} tanpa batas ruas, sehingga aman untuk
	 * indeks kecil seperti [2]. Rekaman yang cacat (ruas kurang) hanya dicatat ke
	 * {@code ErrorAuditUtil} dan dilewati &mdash; pencarian dilanjutkan ke rekaman berikutnya.
	 * Pembacaan blob dilakukan lewat {@link #getAbsensi()}, jadi efek samping getter destruktif
	 * itu ikut terpicu.</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return kode status, atau {@code "-"} bila guru tidak punya rekaman pada sesi ini
	 */
	public String retreiveAbsensiKode(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[2];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:431");

				}
			}
		}

		return "-";
	}

	/**
	 * Mengambil <b>nama</b> status kehadiran (ruas [3], mis. {@code "Masuk"}, {@code "Sakit"})
	 * milik satu guru dari blob presensi.
	 *
	 * <p><b>Tidak punya pemanggil di basis kode</b> &mdash; lihat catatan pada
	 * {@link #retreiveAbsensiKode(String)}. Nama status yang dipakai layar dan laporan diambil
	 * dari objek {@link Statusabsensi} hasil pemetaan {@link #retreiveAbsensiId(String)}, bukan
	 * dari salinan teks di dalam blob (yang bisa basi bila master status pernah diganti
	 * namanya).</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return nama status, atau {@code "-"} bila guru tidak punya rekaman pada sesi ini
	 */
	public String retreiveAbsensiNama(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[3];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:451");

				}
			}
		}

		return "-";
	}

	/**
	 * Mengambil <b>keterangan per guru</b> (ruas [5]) dari blob presensi.
	 *
	 * <p><b>Non-obvious &mdash; pemisahan berbatas.</b> Berbeda dari
	 * {@link #retreiveAbsensiKode(String)}, method ini memakai {@code split(",", 9)} sehingga
	 * ruas terakhir menampung sisa teks apa adanya. Ini melindungi ruas [6]..[8] bila keterangan
	 * pernah tersimpan sebelum {@link #populate} menetralkan koma menjadi {@code "_"}.</p>
	 *
	 * <p><b>Bentuk nilai:</b> koma di dalam keterangan sudah diganti {@code "_"} dan titik koma
	 * menjadi {@code "..\n"} oleh {@link #populate}. Pemanggil UI
	 * ({@code DetailAbsenGuruPiketHelper}) mengembalikan {@code "_"} menjadi {@code ","} sebelum
	 * menampilkan &mdash; sehingga garis bawah yang <i>memang</i> diketik pengguna ikut berubah
	 * menjadi koma pada tampilan (transformasi tidak reversibel).</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code DetailAbsenGuruPiketHelper} (render baris, aksi massal
	 * "Semua hadir"/"Reset", dan ekspor "Download Kehadiran"), serta dari {@link #populate}
	 * sendiri sebagai nilai jatuh-balik.</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return keterangan, atau {@code ""} (bukan {@code "-"}) bila tidak ada rekaman
	 */
	public String retreiveAbsensiKeterangan(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[5];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:471");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>id {@link Statusabsensi}</b> (ruas [1]) milik satu guru dari blob presensi.
	 * Inilah method pembaca yang benar-benar dipakai aplikasi.
	 *
	 * <p><b>Dipanggil dari:</b> {@code DetailAbsenGuruPiketHelper} (render baris grid dan ekspor
	 * Excel) serta {@code LaporanRekapAbsenGuruPiket}/{@code LaporanRekapAbsenGuruPiketHarian}.
	 * Semua pemanggil menyerahkan hasilnya ke
	 * {@code ConstantValues.ambil(Statusabsensi.class.getName(), id)} untuk memperoleh objek
	 * status dari cache.</p>
	 *
	 * <p><b>Non-obvious &mdash; sentinel {@code -1L}.</b> Bila guru tidak punya rekaman (atau
	 * ruas [1] tidak dapat diurai sebagai angka), nilai kembaliannya {@code -1L}, bukan
	 * {@code null}. Pencarian di cache dengan id {@code -1L} menghasilkan {@code null}, dan
	 * pemanggil menafsirkannya sebagai "belum diabsen" &mdash; di panel detail penafsiran itulah
	 * yang memicu penulisan status bawaan ke basis data saat baris dirender.</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan sentinel
	 * @return id status kehadiran, atau {@code -1L} bila tidak ada rekaman yang cocok
	 */
	public Long retreiveAbsensiId(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",");
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return Long.parseLong(s[1]);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:491");

				}
			}
		}

		return -1L;
	}

	/**
	 * Mengambil <b>jam masuk</b> (ruas [6]) milik satu guru dari blob presensi.
	 *
	 * <p>Ruas ini hanya terisi lewat jalur mesin absensi
	 * ({@code CommonPayroll.simpanDetail(...)} meneruskan
	 * {@code StatuskehadiranKaryawanHarian.ambilMasukjam()} berformat
	 * {@code Common.timeFormat2}); panel detail selalu mengirim string kosong.</p>
	 *
	 * <p><b>Tidak ada pemanggil di luar kelas ini</b> &mdash; satu-satunya pengguna adalah
	 * {@link #populate} sebagai nilai jatuh-balik ketika parameter {@code mulai} kosong.</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return jam masuk sebagai teks, atau {@code ""} bila tidak ada rekaman/ruasnya kosong
	 */
	public String retreiveAbsensiMulai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[6];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:511");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>jam pulang</b> (ruas [7]) milik satu guru dari blob presensi. Perilaku,
	 * pengisi, dan pemanggilnya identik dengan {@link #retreiveAbsensiMulai(String)} &mdash;
	 * hanya dipakai {@link #populate} sebagai nilai jatuh-balik.
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return jam pulang sebagai teks, atau {@code ""} bila tidak ada rekaman/ruasnya kosong
	 */
	public String retreiveAbsensiSampai(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[7];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:531");

				}
			}
		}

		return "";
	}

	/**
	 * Mengambil <b>jenis/asal rekaman</b> (ruas [8]) milik satu guru dari blob presensi.
	 *
	 * <p>Seluruh pemanggil {@link #populate} pada entity ini mengisinya dengan konstanta teks
	 * {@code "AbsenGuruPiket"}, sehingga ruas ini praktis tidak membedakan apa pun di sini. Ruas
	 * yang sama dipakai lebih berarti pada {@code AbsenPiketDetail} (yang menerima rekaman dari
	 * beberapa asal berbeda, termasuk kios).</p>
	 *
	 * <p><b>Tidak ada pemanggil di luar kelas ini</b> &mdash; hanya {@link #populate} sebagai
	 * nilai jatuh-balik.</p>
	 *
	 * @param ref id {@link Guru} sebagai teks; {@code null} langsung menghasilkan nilai bawaan
	 * @return jenis rekaman, atau {@code ""} bila tidak ada rekaman yang cocok
	 */
	public String retreiveAbsensiJenis(String ref) {

		if (ref != null) {
			String[] nilais = getAbsensi().split(";");
			for (String nn : nilais) {
				try {
					String[] s = nn.split(",", 9);
					String formatId = (s[0]);
					if (ref.equals(formatId)) {
						return s[8];
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/AbsenGuruPiket.java:551");

				}
			}
		}

		return "";
	}

	/**
	 * Menyisipkan atau memperbarui rekaman kehadiran <b>satu guru</b> di dalam blob presensi
	 * {@link #getAbsensi()}, memelihara rekaman guru lain apa adanya. Ini satu-satunya method
	 * mutasi bisnis pada entity ini.
	 *
	 * <h4>Alur</h4>
	 * <ol>
	 *   <li>Tidak melakukan apa pun bila {@code ref} atau {@code statusabsensi} {@code null}
	 *       (gagal senyap &mdash; pemanggil tidak diberi tahu).</li>
	 *   <li>Bila kode status BUKAN {@code "M"} (Masuk), {@code mulai} dan {@code sampai}
	 *       dikosongkan lebih dulu.</li>
	 *   <li>Bila kode status {@code "M"}, <b>satu {@code Thread} baru dijalankan</b> untuk
	 *       mengirim notifikasi (lihat "Efek samping" di bawah).</li>
	 *   <li>Keterangan dinetralkan: {@code ";"} &rarr; {@code "..\n"} dan {@code ","} &rarr;
	 *       {@code "_"} agar tidak merusak pemisah blob.</li>
	 *   <li>Blob lama dipecah per {@code ";"}; rekaman milik {@code ref} ditulis ulang, rekaman
	 *       lain disalin apa adanya, rekaman kosong dibuang. Bila {@code ref} belum ada, rekaman
	 *       baru ditambahkan di akhir.</li>
	 *   <li>Hasilnya ditugaskan ke field {@code absensi} secara langsung (bukan lewat setter).
	 *       <b>Method ini TIDAK menyimpan ke basis data</b> &mdash; pemanggil wajib memanggil
	 *       {@code Common.refreshUpdate(...)}/{@code session.saveOrUpdate(...)} sendiri.</li>
	 * </ol>
	 *
	 * <h4>Efek samping</h4>
	 * <ul>
	 *   <li><b>Notifikasi untuk status "Masuk".</b> Sebuah {@code Thread} mentah (bukan pool)
	 *       membuka sesi Hibernate native sendiri, mencari seluruh {@code Tbmuser} aktif milik
	 *       guru {@code ref}, lalu memanggil {@code MailSender.simpanNotif(...)} dengan judul
	 *       "Info kehadiran guru" dan objek {@code AbsenGuruPiket.this} sebagai rujukan. Karena
	 *       satu thread dibuat per pemanggilan, aksi massal ("Semua hadir" pada panel detail)
	 *       membangkitkan satu thread per guru sekaligus; dan bila konfigurasi
	 *       {@code absen_piket_otomatis_belum} dimatikan, sekadar MERENDER panel detail juga
	 *       memicunya untuk setiap guru yang belum diabsen. Sesi Hibernate ditutup di
	 *       {@code finally}, tetapi objek {@code this} dipakai lintas thread dalam keadaan
	 *       berpotensi detached.</li>
	 *   <li>Membaca blob lewat {@link #getAbsensi()}, sehingga tambalan destruktif
	 *       {@code "9.400"} &rarr; {@code "09.40"} ikut terpicu.</li>
	 *   <li>Rekaman yang cacat saat diurai ditangani {@code Common.tampilErrorJikaAdmin(e)}
	 *       &mdash; bagi pengguna admin galat MUNCUL DI LAYAR, bagi pengguna lain diabaikan.
	 *       Rekaman yang gagal diurai tidak ikut tersalin ke blob baru, jadi <b>data rusak akan
	 *       hilang</b> pada penyimpanan berikutnya.</li>
	 * </ul>
	 *
	 * <h4>Kuirk yang perlu diketahui</h4>
	 * <p><b>Pengosongan jam masuk/pulang tidak pernah terjadi.</b> Langkah 2 menyetel
	 * {@code mulai}/{@code sampai} menjadi {@code ""} untuk status non-Masuk, tetapi perakitan
	 * rekaman di langkah 5 memakai pola
	 * {@code (mulai == null || mulai.trim().isEmpty() ? retreiveAbsensiMulai(ref) : mulai)}
	 * &mdash; string kosong justru memicu pengambilan NILAI LAMA dari blob. Akibatnya mengubah
	 * status seorang guru dari Masuk menjadi Izin/Sakit tetap mempertahankan jam masuk dan jam
	 * pulang lamanya. Pola yang sama membuat parameter {@code keterangan}/{@code jenis} bernilai
	 * {@code null} berarti "pertahankan nilai lama", bukan "kosongkan".</p>
	 *
	 * <p><b>Ruas [4] selalu literal {@code "0"}.</b> Ditulis tetap dan tidak pernah dibaca
	 * kembali oleh kelas ini &mdash; sisa format warisan keluarga {@code Pertemuan}.</p>
	 *
	 * <h4>Dipanggil dari</h4>
	 * <ul>
	 *   <li>{@code DetailAbsenGuruPiketHelper} &mdash; saat radio status/keterangan diubah, saat
	 *       baris dirender untuk guru yang belum punya rekaman, dan pada aksi massal "Semua
	 *       hadir"/"Reset".</li>
	 *   <li>{@code CommonPayroll.simpanDetail(...)} &mdash; menyalin kehadiran harian pegawai/guru
	 *       dari mesin absensi, lengkap dengan jam masuk/pulang.</li>
	 * </ul>
	 *
	 * @param ref           id {@link Guru} sebagai teks &mdash; kunci rekaman; wajib, {@code null}
	 *                      membuat method tidak melakukan apa pun
	 * @param statusabsensi status kehadiran yang dipasang; wajib, {@code null} membuat method
	 *                      tidak melakukan apa pun
	 * @param keterangan    keterangan per guru; {@code null} berarti "pertahankan keterangan lama"
	 * @param mulai         jam masuk sebagai teks; {@code null}/kosong berarti "pertahankan nilai
	 *                      lama" (lihat kuirk di atas)
	 * @param sampai        jam pulang sebagai teks; perilaku sama dengan {@code mulai}
	 * @param jenis         penanda asal rekaman; seluruh pemanggil mengisi {@code "AbsenGuruPiket"};
	 *                      {@code null} berarti "pertahankan nilai lama"
	 */
	public void populate(final String ref, Statusabsensi statusabsensi, String keterangan, String mulai, String sampai,
			String jenis) {
		if (ref != null && statusabsensi != null) {

			if (statusabsensi.getKode() == null || !statusabsensi.getKode().equals("M")) {
				mulai = "";
				sampai = "";
			}

			if (statusabsensi.getKode() != null && statusabsensi.getKode().equals("M")) {
				final String ket = keterangan;
				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {

						Session session = HibernateUtil.currentNativeSession();
						List<String> usernames = session.createCriteria(Tbmuser.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("guru.id", Long.parseLong(ref)))
								.setProjection(Projections.property("userId")).list();
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();
						if (!usernames.isEmpty()) {
							JSONArray userIds = new JSONArray();
							for (String s : usernames) {
								userIds.put(s);
							}
							String recipientsTemp = null;
							MailSender.simpanNotif(userIds, recipientsTemp, "Info kehadiran guru", ket,
									AbsenGuruPiket.this);
						}

											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();
			}

			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ";", "..\n");
			keterangan = org.apache.commons.lang3.StringUtils.replace(keterangan, ",", "_");
			String formatBaru = "";
			String[] nilais = getAbsensi().split(";");
			Boolean ada = false;
			for (String nn : nilais) {
				try {
					String aformatBaru = "";
					String[] s = nn.split(",");
					if (!s[0].trim().isEmpty()) {
						String formatId = (s[0]);
						if (ref.equals(formatId)) {
							aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
									+ statusabsensi.getNama() + ",0,"
									+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
									+ (mulai == null || mulai.trim().isEmpty() ? retreiveAbsensiMulai(ref) : mulai)
									+ ","
									+ (sampai == null || sampai.trim().isEmpty() ? retreiveAbsensiSampai(ref) : sampai)
									+ "," + (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
							ada = true;
						} else {
							aformatBaru = nn;
						}
						if (!aformatBaru.trim().isEmpty()) {
							formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
						}
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

			if (!ada) {
				String aformatBaru = ref + "," + statusabsensi.getId() + "," + statusabsensi.getKode() + ","
						+ statusabsensi.getNama() + ",0,"
						+ (keterangan == null ? retreiveAbsensiKeterangan(ref) : keterangan) + ","
						+ (mulai == null || mulai.trim().isEmpty() ? retreiveAbsensiMulai(ref) : mulai) + ","
						+ (sampai == null || sampai.trim().isEmpty() ? retreiveAbsensiSampai(ref) : sampai) + ","
						+ (jenis == null ? retreiveAbsensiJenis(ref) : jenis);
				formatBaru += formatBaru.isEmpty() ? aformatBaru : ";" + aformatBaru;
			}

			// System.out.println("formatBaru => " + formatBaru);

			absensi = formatBaru;
		}
	}

	/**
	 * Mengembalikan nomor sesi presensi dalam sehari ("jam ke") untuk baris ini.
	 *
	 * <p>Nilai {@code 0} berarti "tanpa jam-ke" &mdash; kondisi bawaan pada sekolah yang belum
	 * mengaktifkan satu pun saklar {@code <idSekolah>_absen_piket_guru_jam_ke_N}, dan juga hasil
	 * {@link #jamKe(Long)} bila jam absen berada di luar seluruh jendela toleransi.</p>
	 *
	 * <p><b>Kontrak nilai kosong:</b> kolom {@code null} dikembalikan sebagai {@code 0} tanpa
	 * menulis balik ke field, sehingga pemanggil dapat langsung membandingkan angka. Baris lama
	 * ber-kolom {@code null} karena itu tidak dapat dibedakan dari baris ber-jam-ke nol lewat
	 * getter ini &mdash; kriteria pencarian di {@code CommonPayroll} sengaja memakai
	 * {@code Restrictions.or(isNull("jamke"), eq("jamke", jamke))} untuk menangani keduanya.</p>
	 *
	 * <p><b>Kuirk layar yang perlu diketahui:</b> pada {@code AbsenGuruPiketAction.init(...)},
	 * setiap {@code Comboitem} "Jam ke N" dibuat dengan {@code comboitem.setValue(0)} &mdash;
	 * nilai nol yang sama untuk semua pilihan, bukan nomor sesinya. Akibatnya penyimpanan lewat
	 * form tambah/ubah selalu menghasilkan {@code jamke = 0} berapa pun pilihan pengguna, dan
	 * baris hasil entri manual tidak pernah cocok dengan pencarian rekap yang memfilter
	 * {@code Restrictions.in("jamke", jamKes)} pada sekolah yang mengaktifkan fitur jam-ke.
	 * Baris yang dibuat {@code CommonPayroll} (yang memakai {@link #jamKe(Long)}) tidak
	 * terpengaruh.</p>
	 *
	 * @return nomor sesi 0..5; tidak pernah {@code null}
	 * @see #jamKe(Long)
	 * @see #jamKes(Long)
	 */
	public Integer getJamke() {
		return jamke == null ? 0 : jamke;
	}

	/**
	 * Menyetel nomor sesi presensi dalam sehari.
	 *
	 * @param jamke nomor sesi (0..5); {@code null} dibaca sebagai {@code 0} oleh
	 *              {@link #getJamke()}
	 */
	public void setJamke(Integer jamke) {
		this.jamke = jamke;
	}
}
