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

	private Date tanggal;
	private Guru guru;
	private Guru guru2;
	private Guru guru3;
	private Guru guru4;
	private Guru guru5;
	private Sekolah sekolah;
	private Yayasan yayasan;

	private Integer semester;
	private String tahunAjaran;
	private String keterangan;
	private String absensi;
	private Integer jamke;

	public String toString() {
		return getId() + "_" + getGuru() + "_" + getSemester() + "_" + getTahunAjaran();
	}

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

	public AbsenGuruPiket() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

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

	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	@Column(name = "tahun_ajaran", nullable = false, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru_id")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru2_id")
	public Guru getGuru2() {
		guru2 = check(guru2);
		return guru2;
	}

	public void setGuru2(Guru guru2) {
		this.guru2 = guru2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru3_id")
	public Guru getGuru3() {
		guru3 = check(guru3);
		return guru3;
	}

	public void setGuru3(Guru guru3) {
		this.guru3 = guru3;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru4_id")
	public Guru getGuru4() {
		guru4 = check(guru4);
		return guru4;
	}

	public void setGuru4(Guru guru4) {
		this.guru4 = guru4;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru5_id")
	public Guru getGuru5() {
		guru5 = check(guru5);
		return guru5;
	}

	public void setGuru5(Guru guru5) {
		this.guru5 = guru5;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Column(name = "absensi", columnDefinition = "text")
	public String getAbsensi() {
		if (absensi != null && StringUtils.contains(absensi, "9.400")) {
			absensi = org.apache.commons.lang3.StringUtils.replace(absensi, "9.400", "09.40");
		}
		return absensi == null ? "" : absensi.trim();
	}

	public void setAbsensi(String absensi) {
		this.absensi = absensi;
	}

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

	public Integer getJamke() {
		return jamke == null ? 0 : jamke;
	}

	public void setJamke(Integer jamke) {
		this.jamke = jamke;
	}
}
