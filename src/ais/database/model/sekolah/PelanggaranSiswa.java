package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.ui.util.WaktuUtil;

/**
 * <h2>Transaksi pelanggaran tata tertib seorang siswa (tabel {@code sekolah.pelanggaran_siswa})</h2>
 *
 * <p><b>Peran.</b> Entity ini adalah <b>satu-satunya lapis transaksi</b> dari modul tata tertib
 * siswa: satu baris = <b>satu kejadian pelanggaran oleh satu siswa pada satu waktu</b>. Berbeda
 * dari tiga kerabatnya yang semuanya master/katalog, baris di sini adalah <b>catatan disipliner
 * pribadi anak di bawah umur</b> — data paling sensitif di seluruh modul. Isinya: siapa yang
 * melanggar ({@link #getSiswa()}), kapan ({@link #getWaktu()}), pada tahun ajaran apa
 * ({@link #getTa()}), keterangan bebas dari petugas ({@link #getKeterangan()}), jenis pelanggaran
 * yang dipilih ({@link #getPelanggaranDanHukuman()}), rincian pelanggaran yang benar-benar
 * dilakukan ({@link #getPelanggarans()}), dan sanksi yang benar-benar dijatuhkan
 * ({@link #getHukumans()}).</p>
 *
 * <h3>Posisi dalam rantai modul tata tertib (empat lapis)</h3>
 * <ol>
 *   <li><b>{@link Pelanggaran}</b> — master jenis pelanggaran (apa yang dilanggar), membawa bobot
 *       {@code kredit}.</li>
 *   <li><b>{@link Hukuman}</b> — master jenis sanksi (apa akibatnya), membawa bobot {@code poin}.</li>
 *   <li><b>{@code PelanggaranDanHukuman}</b> — <b>paket/kategori</b> yang mengikat sekumpulan
 *       {@link Pelanggaran} dengan sekumpulan {@link Hukuman} lewat dua tabel silang
 *       {@code sekolah.pelanggaran_dan_hukuman_has_pelanggaran} dan
 *       {@code sekolah.pelanggaran_dan_hukuman_has_hukuman}. Meski namanya mengandung kata "dan",
 *       entity itu <b>masih master</b>, bukan transaksi.</li>
 *   <li><b>{@code PelanggaranSiswa}</b> (kelas ini) — barulah ini transaksinya. Menunjuk
 *       <b>satu</b> paket lapis 3 lewat kolom {@code pelanggaran_dan_hukuman} ({@code NOT NULL}),
 *       lalu menyimpan pilihan konkret operator di dua tabel silangnya sendiri
 *       ({@code sekolah.pelanggaran_siswa_has_pelanggaran} dan
 *       {@code sekolah.pelanggaran_siswa_has_hukuman}) — masing-masing berupa himpunan bagian dari
 *       yang ditawarkan paket lapis 3.</li>
 * </ol>
 *
 * <h3>Siapa saja pembaca data ini (verifikasi dari kode, 5 jalur)</h3>
 * <ul>
 *   <li><b>Layar master</b> {@code ais.action.master.sekolah.PelanggaranSiswaAction} — grid
 *       riwayat + formulir tambah/ubah + tombol cetak &amp; unggah (impor Excel).</li>
 *   <li><b>Dasbor</b> {@code ais.action.master.pelanggaran.DasbordPelanggaran} (lingkup
 *       {@code SISWA} dan {@code SEMUA}) — tren 12 bulan, distribusi jenis, pola harian, dan
 *       daftar 600 baris terakhir; dipasang sebagai tab di layar master lewat {@code onDasbor()}.</li>
 *   <li><b>Popup saat siswa login</b> {@code PelanggaranSiswaAction.checkDanTampil(Siswa)},
 *       dipanggil dari {@code MainAction} dan {@code MainAction2} — memunculkan jendela "Info
 *       Kedisiplinan Siswa" berdasarkan {@link #getTampilkanInfoIniSaatSiswaLogin()} dan
 *       {@link #getBatasWaktuDitampilkan()}.</li>
 *   <li><b>Laporan</b> {@code ais.action.report.format1.sekolah.LaporanPelanggaranSiswa} —
 *       rekap per siswa/rentang tanggal dengan total {@code poin} dan {@code kredit}.</li>
 *   <li><b>Rapor</b> {@code ais.action.report.format1.sekolah.LaporanRaporSiswa.masukkanPoin(...)}
 *       — hanya bila {@code JenisRaporSiswa.getAmbilPelanggaranSiswa()} dicentang; menempelkan
 *       riwayat pelanggaran ke dalam berkas rapor siswa.</li>
 *   <li><b>Statistik agregat</b> {@code ais.action.master.dashboard.admin.DasboardSiswa} dan
 *       {@code ais.action.master.helper.profile.ProfileSekolahLanjutanDashboard} — hitungan total
 *       dan tren, tanpa menampilkan identitas per baris.</li>
 * </ul>
 *
 * <h3>PERINGATAN KEAMANAN/PRIVASI — <b>{@code task_5e93a600}</b> (belum diperbaiki)</h3>
 * <p><b>Baca bagian ini sebelum menambah pemanggil baru.</b> Isi tabel ini adalah riwayat
 * disipliner anak di bawah umur, tetapi <b>tidak ada satu pun lapis yang benar-benar membatasi
 * siapa boleh membaca baris siapa</b>. Tiga cacat berikut sudah diverifikasi langsung dari kode
 * (tiga kali, oleh tiga penelusuran independen) dan seluruhnya masih ADA:</p>
 * <ol>
 *   <li><b>{@code PelanggaranSiswaAction.initCriteria(boolean)} tidak memfilter siswa maupun
 *       guru sama sekali.</b> Satu-satunya penyaring kepemilikan pada method itu adalah cabang
 *       {@code tbmuser.getOrangTua()}; peran {@code Siswa} dan {@code Guru} lolos tanpa syarat
 *       apa pun, sehingga <b>siswa yang login bisa membaca riwayat pelanggaran SELURUH siswa</b>
 *       di semua sekolah pada instalasi yang sama. Yang membuatnya jelas bukan kelalaian sepintas:
 *       {@code doAfterCompose(...)} pada kelas yang sama <b>secara eksplisit mengenali</b> peran
 *       tersebut ({@code tbmuser.ambilGuru() != null || tbmuser.getSiswa() != null}) dan hanya
 *       <b>menyembunyikan tab UI</b>-nya — datanya tetap dimuat utuh ke grid.</li>
 *   <li><b>Filter orang tua bersifat FAIL-OPEN.</b> Bentuk kodenya
 *       {@code if (tbmuser.getOrangTua() != null && !ambilAnakSiswa().isEmpty()) criteria.add(in(...))}.
 *       {@code OrangTua.ambilAnakSiswa()} mengurai kolom JSON {@code anak} dan mengembalikan
 *       <b>daftar kosong</b> pada semua kasus tepi — {@code id} masih {@code null}, kolom
 *       {@code anak} kosong/rusak (exception ditelan diam-diam), atau tidak ada kunci berawalan
 *       {@code "siswa"}. Akibatnya daftar kosong tidak berarti "nol baris" melainkan
 *       <b>"tanpa filter sama sekali" = melihat SEMUA</b>. Arah kegagalan yang benar adalah
 *       sebaliknya.</li>
 *   <li><b>{@code DasbordPelanggaran.muatPelanggaranSiswa(...)} tidak mengenal peran orang tua
 *       sama sekali.</b> Method itu hanya menambahkan {@code Restrictions.eq("siswa", sis)} bila
 *       {@code Tbmuser.getSiswa()} tidak {@code null}. Untuk akun orang tua (dan akun guru, dan
 *       akun apa pun yang bukan siswa) {@code sis} bernilai {@code null}, sehingga query berjalan
 *       <b>tanpa syarat apa pun</b> dan mengembalikan {@code MAX_ROWS = 600} baris terbaru
 *       lintas sekolah/yayasan — lengkap dengan nama siswa, jenis pelanggaran, dan keterangan.
 *       Diperparah {@code loadDataWithCache()}: akun non-personal (termasuk orang tua) dianggap
 *       {@code isPersonal == false} sehingga hasilnya <b>disimpan di cache L3 yang app-wide</b>
 *       dan dibagikan antar pengguna.</li>
 * </ol>
 * <p><b>Amplifier terpisah ({@code task_493423ef}).</b> Tabel ini juga terjangkau <b>tanpa login
 * sama sekali</b>: {@code ais.action.servlet.Data} melewatkan aksi baca ({@code daftar},
 * {@code cari}, {@code load}) begitu klien mengirim {@code tanpaLogin=true} — penanda yang
 * sepenuhnya dikendalikan pemanggil — sedangkan
 * {@code ais.action.servlet.api.DaftarDataService} me-resolve nama kelas entity lewat
 * {@code Class.forName(namaKelas)} dari JSON permintaan, tanpa daftar-putih. Jadi nama kelas
 * {@code ais.database.model.sekolah.PelanggaranSiswa} dapat dikirim langsung oleh pemanggil
 * anonim.</p>
 * <p><b>Kontras positif</b> agar terlihat bahwa ini memang cacat dan bukan desain: laporan
 * {@code LaporanPelanggaranSiswa} <b>memang</b> mengunci bandbox siswa ke akun yang login bila
 * penggunanya siswa, dan {@code HukumanAction} punya rangkaian {@code checkPrevilages} yang
 * lengkap. Pola pengamanannya sudah ada di modul yang sama, hanya tidak dipakai di dua titik di
 * atas.</p>
 *
 * <h3>Pengelompokan method</h3>
 * <ul>
 *   <li><b>Jejak audit manual:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan callback
 *       {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)} dan dua konstruktor
 *       {@link #PelanggaranSiswa()} / {@link #PelanggaranSiswa(long, String)}.</li>
 *   <li><b>Subjek &amp; cakupan kepemilikan:</b> {@link #getSiswa()}, {@link #getSekolah()},
 *       {@link #getYayasan()} beserta setter-nya — dua yang terakhir <b>destruktif</b>, lihat
 *       "Getter destruktif" di bawah.</li>
 *   <li><b>Isi transaksi:</b> {@link #getPelanggaranDanHukuman()}, {@link #getPelanggarans()},
 *       {@link #getHukumans()}, {@link #getWaktu()}, {@link #getTa()},
 *       {@link #getKeterangan()}, {@link #getNama()}, {@link #getAktif()} beserta setter-nya.</li>
 *   <li><b>Kendali tampilan popup ke siswa:</b>
 *       {@link #getTampilkanInfoIniSaatSiswaLogin()} dan {@link #getBatasWaktuDitampilkan()}
 *       beserta setter-nya.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang wajib diketahui</h3>
 *
 * <p><b>1. Javadoc bawaan generator salah.</b> Komentar asli hasil hbm2java berbunyi "JenisGuru
 * generated by hbm2java" — sisa salin-tempel yang sama persis dengan {@link Pelanggaran},
 * {@link Hukuman}, dan {@code PelanggaranDanHukuman}. Abaikan sebagai petunjuk domain.</p>
 *
 * <p><b>2. Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah} BUKAN
 * bug.</b> {@link ais.database.model.GeneralValueObject} adalah POJO abstrak biasa — <b>bukan</b>
 * {@code @Entity} dan <b>bukan</b> {@code @MappedSuperclass} — sehingga Hibernate tidak memetakan
 * properti induk sama sekali. Setiap subclass yang butuh kolom itu <b>harus</b> mendeklarasikannya
 * ulang. Efek sampingnya: {@code nama} dan {@code keterangan} di sini <b>membayangi</b> field
 * bernama sama milik induk, sedangkan {@code nomorUrut} dan {@code nim} milik induk tetap ada tapi
 * <b>selalu {@code null}</b> untuk entity ini.</p>
 *
 * <p><b>3. {@code nama} adalah kolom TURUNAN, dan setter-nya praktis mati.</b>
 * {@link #getNama()} <b>selalu</b> menimpa field dengan
 * {@code getSiswa() + "_" + getPelanggaranDanHukuman() + "_" + getWaktu()} sebelum
 * mengembalikannya, sehingga apa pun yang ditulis {@link #setNama(String)} hilang pada pembacaan
 * berikutnya. Karena pemetaan memakai <i>property access</i>, nilai turunan itulah yang benar-benar
 * masuk ke kolom {@code nama} ({@code NOT NULL}) pada setiap {@code INSERT}/{@code UPDATE}.
 * Bentuk hasilnya: {@code "<id>-<nomorInduk>-<namaSiswa>_<kode> - <namaPaket>_<Date.toString()>"}
 * ({@code Siswa} meng-override {@code toString()}; {@code PelanggaranDanHukuman} tidak, jadi
 * memakai {@code GeneralValueObject.toString()} = {@code kode + " - " + nama}). Konsekuensi
 * praktis: (a) kolom {@code nama} berisi teks gabungan yang tidak enak dibaca dan tidak stabil
 * (ikut berubah bila siswa berganti nama), (b) impor Excel lewat {@code Common.uploadData(...)}
 * yang menyertakan kolom "nama" tidak akan pernah berpengaruh, dan (c) {@code DasbordPelanggaran}
 * memakai {@code getNama()} sebagai <i>fallback</i> label subjek sehingga menampilkan teks gabungan
 * itu apa adanya bila relasi {@code siswa} kosong.</p>
 *
 * <p><b>4. Getter destruktif (write-back diam-diam).</b> Dengan <i>property access</i> +
 * {@code dynamicUpdate=true}, Hibernate membaca state lewat getter saat flush; getter yang punya
 * efek samping karena itu bisa berubah menjadi {@code UPDATE} nyata dan revisi Envers palsu hanya
 * karena barisnya kebetulan dibaca:</p>
 * <ul>
 *   <li>{@link #getSekolah()} <b>menimpa</b> {@code sekolah} dengan {@code getSiswa().getSekolah()}
 *       setiap kali dipanggil.</li>
 *   <li>{@link #getYayasan()} <b>menimpa</b> {@code yayasan} dengan
 *       {@code getSekolah().getYayasan()} — jadi berantai dengan butir di atas.</li>
 *   <li>{@link #getNama()} <b>menimpa</b> {@code nama} (lihat butir 3).</li>
 * </ul>
 * <p>Sisi baiknya: dua kolom cakupan tenant selalu ikut siswa pemiliknya, sehingga filter
 * pencarian {@code sekolah}/{@code yayasan} di layar master tidak bisa "kadaluwarsa". Sisi
 * buruknya: nilai yang disetel manual lewat {@link #setSekolah(Sekolah)} /
 * {@link #setYayasan(Yayasan)} tidak pernah bertahan.</p>
 *
 * <p><b>5. Risiko penciutan senyap {@code TreeSet} pada koleksi anaknya.</b> Kelas ini tidak
 * meng-override {@code compareTo}, jadi berlaku implementasi induk ({@code nomorUrut} → {@code nim}
 * → {@code nama} → {@code keterangan}). Yang lebih penting: {@code PelanggaranSiswaAction}
 * membungkus <b>{@link #getPelanggarans()} dan {@link #getHukumans()}</b> ke dalam
 * {@code new TreeSet<>(...)} sebelum merendernya di grid, sedangkan {@link Pelanggaran} maupun
 * {@link Hukuman} juga jatuh ke perbandingan berdasarkan {@code nama} saja. Dua pelanggaran (atau
 * dua sanksi) berbeda dengan {@code nama} identik — tidak ada batasan {@code unique} yang
 * mencegahnya — akan <b>menciut menjadi satu baris di layar</b>, sehingga satu butir riwayat
 * disipliner lenyap dari tampilan tanpa jejak.</p>
 *
 * <p><b>6. {@code waktu} tidak pernah diisi dari formulir.</b> Layar master memang menampilkan
 * {@code MyDatebox} berlabel "Tanggal dan Waktu Pelanggaran *", tetapi komponen itu
 * {@code setReadonly(true)} dan {@code onSave(Event)} <b>tidak pernah membacanya</b>. Karena
 * {@link #getWaktu()} mengembalikan {@code WaktuUtil.getDate()} bila field masih {@code null},
 * yang tersimpan sebenarnya adalah <b>waktu penyimpanan baris</b>, bukan waktu kejadian yang
 * dipilih petugas. Sama halnya {@link #getTa()} yang mengembalikan tahun akademik berjalan sebagai
 * nilai bawaan — meski ini memang ditimpa {@code onSave(...)} dari combobox.</p>
 *
 * <p><b>7. Popup "Tampil Saat Login" praktis tidak berfungsi seperti namanya.</b> Dua cacat
 * bertumpuk di {@code PelanggaranSiswaAction}, keduanya kembar persis dengan
 * {@code ais.action.master.PelanggaranMahasiswaAction}: (a) pendengar event untuk
 * {@code MyDatebox} batas waktu didaftarkan pada {@code "onCheck"} — event yang tidak pernah
 * dipancarkan sebuah datebox — sehingga nilai {@link #batasWaktuDitampilkan} yang diketik operator
 * tidak pernah tersimpan; dan (b) {@code checkDanTampil(...)} menyaring dengan
 * {@code Restrictions.le("batasWaktuDitampilkan", sekarang)}, yaitu "batas waktu SUDAH LEWAT" —
 * kebalikan dari maksud kolomnya. Baris dengan batas waktu terisi karena itu hanya tampil
 * <b>setelah</b> masa tayangnya habis.</p>
 *
 * <p><b>8. Daftar kolom cetak/impor menyebut properti fiktif.</b> {@code PelanggaranSiswaAction}
 * memakai satu array {@code contents} untuk tombol cetak <i>dan</i> unggah, dan array itu memuat
 * {@code "tampilkanInfoIniSaatMahasiswaLogin"} — properti milik
 * {@code ais.database.model.PelanggaranMahasiswa}, <b>tidak ada</b> pada kelas ini (yang benar
 * {@code "tampilkanInfoIniSaatSiswaLogin"}). Varian bug salin-tempel yang sama seperti yang
 * ditemukan pada keluarga {@code ParameterTambahan*}.</p>
 *
 * <p><b>9. Tidak dipra-muat ke cache startup.</b> Berbeda dari {@link Pelanggaran},
 * {@link Hukuman}, dan {@code PelanggaranDanHukuman} yang ketiganya terdaftar di
 * {@code initClasses(...)} milik {@code ais.common.InitData}, kelas transaksi ini <b>tidak</b>
 * ikut dibaca ke memori saat aplikasi menyala — wajar, karena volumenya tumbuh terus.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Pelanggaran
 * @see Hukuman
 * @see Siswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pelanggaran_siswa", schema = "sekolah")
public class PelanggaranSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilai ini <b>identik</b> dengan milik {@link Pelanggaran},
	 * {@link Hukuman}, dan {@code PelanggaranDanHukuman} — sisa salin-tempel antar entity satu
	 * modul. Tidak berbahaya, karena serialisasi Java hanya membandingkan nilai ini antar versi
	 * kelas yang sama.
	 */
	private static final long serialVersionUID = -7490758846785025664L;

	/** Kunci utama, dibangkitkan database ({@code IDENTITY}). Lihat {@link #getId()}. */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini; diisi otomatis oleh
	 * {@code AuditTimestampInterceptor}. Lihat {@link #getOleh()}.
	 */
	private String oleh;

	/**
	 * ID pengguna terakhir yang mengubah baris ini; pendamping {@link #oleh}. Lihat
	 * {@link #getOlehId()}.
	 */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah terakhir, dengan penjagaan "tolak nilai kosong".
	 *
	 * <p><b>Perhatikan:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan
	 * diam-diam</b> (method langsung {@code return} tanpa menulis apa pun), sehingga jejak audit
	 * yang sudah terisi tidak bisa dikosongkan kembali lewat setter ini. Disengaja, agar
	 * interceptor audit tidak menghapus jejak lama ketika konteks pengguna tidak tersedia
	 * (mis. proses batch/startup tanpa sesi login).</p>
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir, dengan penjagaan "tolak nilai kosong" yang sama
	 * persis dengan {@link #setOlehId(String)} — nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit lama tidak terhapus.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dijalankan otomatis oleh provider persistence tepat sebelum
	 * pernyataan {@code UPDATE} baris ini dikirim ke database.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #oleh}, {@link #olehId}, dan {@link #tanggal_dirubah} dari konteks pengguna yang
	 * sedang aktif.</p>
	 *
	 * <p><b>Jangan dipanggil manual.</b> Method ini {@code protected} dan sepenuhnya milik
	 * provider persistence. Perhatikan pula bahwa <b>tidak ada</b> {@code @PrePersist}, jadi pada
	 * {@code INSERT} jejak {@code oleh}/{@code olehId} hanya terisi bila pemanggil (atau
	 * interceptor sesi) mengisinya sendiri.</p>
	 *
	 * <p><b>Catatan gaya:</b> pada berkas aslinya deklarasi method ini dan deklarasi field
	 * {@link #tanggal_dirubah} ditulis berdempetan dalam satu baris; keduanya dipisah di sini
	 * murni agar terbaca, tanpa perubahan perilaku apa pun.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. Diberi nilai awal "sekarang"
	 * ({@code WaktuUtil.getDate()}) saat objek dibuat, sehingga baris baru selalu punya cap waktu
	 * meski tidak ada {@code @PrePersist}. Lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel cap waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}; jarang
	 * dipanggil kode aplikasi secara langsung. Berbeda dari {@link #setOleh(String)} dan
	 * {@link #setOlehId(String)}, setter ini <b>menerima {@code null}</b> tanpa penjagaan.</p>
	 *
	 * @param tanggal_dirubah cap waktu perubahan yang baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini (kolom {@code tanggal_dirubah},
	 * dipetakan sebagai {@code TIMESTAMP}).
	 *
	 * @return cap waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori, karena field-nya diinisialisasi "sekarang".
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Sekolah pemilik baris (cakupan tenant). <b>Turunan</b>: selalu ditimpa dari
	 * {@code siswa.getSekolah()} oleh {@link #getSekolah()}, sehingga nilai yang disetel manual
	 * tidak bertahan.
	 */
	private Sekolah sekolah;

	/**
	 * Yayasan pemilik baris (cakupan tenant tingkat atas). <b>Turunan</b>: selalu ditimpa dari
	 * {@code sekolah.getYayasan()} oleh {@link #getYayasan()}.
	 */
	private Yayasan yayasan;

	/**
	 * Siswa yang melakukan pelanggaran — <b>subjek</b> transaksi ini. Kolom {@code siswa_id}
	 * bersifat {@code NOT NULL}. Lihat {@link #getSiswa()}.
	 */
	private Siswa siswa;

	/**
	 * Paket pelanggaran-dan-hukuman (master lapis 3) yang dipilih untuk kejadian ini. Kolom
	 * {@code pelanggaran_dan_hukuman} bersifat {@code NOT NULL} dan divalidasi layar. Lihat
	 * {@link #getPelanggaranDanHukuman()}.
	 */
	private PelanggaranDanHukuman pelanggaranDanHukuman;

	/**
	 * Waktu kejadian pelanggaran. Lihat {@link #getWaktu()} — dan perhatikan bahwa formulir tidak
	 * pernah mengisinya, sehingga praktisnya berisi waktu penyimpanan baris.
	 */
	private Date waktu;

	/**
	 * Keterangan bebas dari petugas mengenai kejadian ini (kolom {@code keterangan}). Ditampilkan
	 * di grid, popup info siswa, laporan, rapor, dan dasbor. Membayangi field bernama sama milik
	 * {@link ais.database.model.GeneralValueObject}.
	 */
	private String keterangan;

	/**
	 * Kolom {@code nama} ({@code NOT NULL}) — <b>turunan, bukan masukan</b>. Selalu ditimpa
	 * {@link #getNama()} dengan gabungan siswa + paket + waktu. Membayangi field bernama sama
	 * milik {@link ais.database.model.GeneralValueObject}.
	 */
	private String nama;

	/**
	 * Tahun ajaran tempat kejadian ini dicatat (mis. {@code "2025/2026"}). Dipakai sebagai
	 * penyaring utama saat rapor mengambil riwayat pelanggaran seorang siswa. Lihat
	 * {@link #getTa()}.
	 */
	private String ta;

	/**
	 * Penanda baris masih berlaku. Nilai {@code null} diperlakukan sebagai {@code true} oleh
	 * {@link #getAktif()}. Benar-benar disaring oleh {@code LaporanPelanggaranSiswa} dan
	 * {@code checkDanTampil(...)}, tetapi <b>tidak</b> oleh grid layar master maupun dasbor.
	 */
	private Boolean aktif;

	/**
	 * Sanksi yang benar-benar dijatuhkan pada kejadian ini. Lihat {@link #getHukumans()}.
	 * Diinisialisasi {@code HashSet} kosong agar tidak pernah {@code null}.
	 */
	private Set<Hukuman> hukumans = new HashSet<Hukuman>();

	/**
	 * Mengembalikan himpunan sanksi ({@link Hukuman}) yang benar-benar dijatuhkan pada kejadian
	 * pelanggaran ini.
	 *
	 * <p><b>Pemetaan.</b> {@code @ManyToMany} lewat tabel silang
	 * {@code sekolah.pelanggaran_siswa_has_hukuman} ({@code pelanggaran_siswa} &rarr;
	 * {@code hukuman}), dengan {@code cascade = MERGE} saja — menyimpan transaksi ini <b>tidak</b>
	 * pernah membuat baris master hukuman baru, hanya menyegarkan yang sudah ada.
	 * {@code @OrderBy("nama asc")} membuat Hibernate menambahkan {@code ORDER BY} pada saat
	 * memuat; perhatikan bahwa tipe kembaliannya tetap {@code Set} sehingga urutan itu hanya
	 * bertahan selama implementasi {@code Set} yang dipakai Hibernate mempertahankannya.</p>
	 *
	 * <p><b>Isi yang sah</b> adalah himpunan bagian dari sanksi yang ditawarkan
	 * {@link #getPelanggaranDanHukuman()}; pembatasan itu diberlakukan di layar
	 * ({@code PelanggaranSiswaAction.loadHukuman(...)}), bukan oleh basis data.</p>
	 *
	 * <p><b>Pemanggil penting:</b> {@code LaporanPelanggaranSiswa} dan
	 * {@code LaporanRaporSiswa.masukkanPoin(...)} menjumlahkan {@code Hukuman.getPoin()} dari
	 * koleksi ini menjadi total poin siswa; renderer grid membungkusnya dalam {@code TreeSet}
	 * (lihat peringatan penciutan senyap pada Javadoc kelas).</p>
	 *
	 * @return himpunan sanksi; tidak pernah {@code null} (kosong bila belum ada yang dipilih).
	 */
	@ManyToMany(targetEntity = Hukuman.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_siswa_has_hukuman", schema = "sekolah", joinColumns = @JoinColumn(name = "pelanggaran_siswa"), inverseJoinColumns = @JoinColumn(name = "hukuman"))
	public Set<Hukuman> getHukumans() {
		return hukumans;
	}

	/**
	 * Mengganti seluruh himpunan sanksi kejadian ini.
	 *
	 * <p>Dipakai Hibernate saat hidrasi dan oleh {@code PelanggaranSiswaAction.onSave(Event)},
	 * yang menyerahkan himpunan hasil centang operator apa adanya. <b>Menimpa</b> isi lama —
	 * sanksi yang tidak ikut dikirim akan terhapus dari tabel silang pada flush berikutnya.
	 * Tidak ada penjagaan {@code null}; menyetel {@code null} akan membuat
	 * {@link #getHukumans()} mengembalikan {@code null} dan berpotensi
	 * {@code NullPointerException} di penjumlahan poin laporan.</p>
	 *
	 * @param hukumans himpunan sanksi yang baru.
	 */
	public void setHukumans(Set<Hukuman> hukumans) {
		this.hukumans = hukumans;
	}

	/**
	 * Rincian pelanggaran yang benar-benar dilakukan pada kejadian ini. Lihat
	 * {@link #getPelanggarans()}. Diinisialisasi {@code HashSet} kosong agar tidak pernah
	 * {@code null}.
	 */
	private Set<Pelanggaran> pelanggarans = new HashSet<Pelanggaran>();

	/**
	 * Penanda "tampilkan catatan ini sebagai popup ketika siswa yang bersangkutan login". Nilai
	 * {@code null} diperlakukan sebagai {@code true} oleh
	 * {@link #getTampilkanInfoIniSaatSiswaLogin()} — jadi <b>bawaannya menyala</b>. Lihat
	 * peringatan pada Javadoc kelas mengenai popup yang praktis tidak berfungsi.
	 */
	private Boolean tampilkanInfoIniSaatSiswaLogin;

	/**
	 * Batas akhir masa tayang popup info kedisiplinan. Lihat
	 * {@link #getBatasWaktuDitampilkan()} — nilainya praktis tidak pernah tersimpan (pendengar
	 * event salah) dan perbandingannya di layar terbalik arah.
	 */
	private Date batasWaktuDitampilkan;

	/**
	 * Mengembalikan himpunan jenis pelanggaran ({@link Pelanggaran}) yang benar-benar dilakukan
	 * pada kejadian ini.
	 *
	 * <p><b>Pemetaan.</b> {@code @ManyToMany} lewat tabel silang
	 * {@code sekolah.pelanggaran_siswa_has_pelanggaran} ({@code pelanggaran_siswa} &rarr;
	 * {@code pelanggaran}), {@code cascade = MERGE} saja, dan {@code @OrderBy("nama asc")} —
	 * kembar persis dengan {@link #getHukumans()}.</p>
	 *
	 * <p><b>Isi yang sah</b> adalah himpunan bagian dari pelanggaran yang ditawarkan
	 * {@link #getPelanggaranDanHukuman()}; pembatasan diberlakukan di layar
	 * ({@code PelanggaranSiswaAction.loadPelanggaran(...)}), bukan oleh basis data.</p>
	 *
	 * <p><b>Pemanggil penting:</b> {@code LaporanPelanggaranSiswa} menjumlahkan
	 * {@code Pelanggaran.getKredit()} dari koleksi ini menjadi total kredit pelanggaran; renderer
	 * grid membungkusnya dalam {@code TreeSet} (lihat peringatan penciutan senyap pada Javadoc
	 * kelas).</p>
	 *
	 * @return himpunan jenis pelanggaran; tidak pernah {@code null} (kosong bila belum dipilih).
	 */
	@ManyToMany(targetEntity = Pelanggaran.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_siswa_has_pelanggaran", schema = "sekolah", joinColumns = @JoinColumn(name = "pelanggaran_siswa"), inverseJoinColumns = @JoinColumn(name = "pelanggaran"))
	public Set<Pelanggaran> getPelanggarans() {
		return pelanggarans;
	}

	/**
	 * Mengganti seluruh himpunan jenis pelanggaran kejadian ini.
	 *
	 * <p>Perilakunya sama persis dengan {@link #setHukumans(Set)}: dipakai Hibernate saat hidrasi
	 * dan oleh {@code PelanggaranSiswaAction.onSave(Event)}, <b>menimpa</b> isi lama, tanpa
	 * penjagaan {@code null}.</p>
	 *
	 * @param pelanggarans himpunan jenis pelanggaran yang baru.
	 */
	public void setPelanggarans(Set<Pelanggaran> pelanggarans) {
		this.pelanggarans = pelanggarans;
	}

	/**
	 * Konstruktor kosong. <b>Wajib ada</b> karena Hibernate membutuhkannya untuk membuat instance
	 * saat menghidrasi baris dari database, dan dipakai layar master saat menyiapkan formulir
	 * "Tambah".
	 */
	public PelanggaranSiswa() {
	}

	/**
	 * Konstruktor pintas berisi kolom-kolom {@code NOT NULL} versi generator hbm2java.
	 *
	 * <p><b>Menyesatkan dan tidak dipakai.</b> Kolom {@code NOT NULL} yang sebenarnya pada tabel
	 * ini ada tiga ({@code id}, {@code nama}, {@code siswa_id}, ditambah
	 * {@code pelanggaran_dan_hukuman}), tetapi konstruktor ini hanya menerima {@code id} dan
	 * {@code nama} sehingga tidak pernah menghasilkan objek yang bisa disimpan. Lebih jauh,
	 * {@code nama} yang diberikan di sini <b>akan dibuang</b> pada pembacaan pertama karena
	 * {@link #getNama()} selalu menghitung ulang nilainya (lihat Javadoc kelas butir 3). Tidak
	 * ditemukan pemanggil di dalam basis kode.</p>
	 *
	 * @param id   nilai kunci utama.
	 * @param nama nilai awal kolom {@code nama} — praktis diabaikan.
	 */
	public PelanggaranSiswa(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolom {@code id} dipetakan {@code insertable = false} dengan strategi
	 * {@code GenerationType.IDENTITY}, jadi nilainya sepenuhnya ditentukan database dan baru
	 * terisi setelah {@code INSERT}.</p>
	 *
	 * @return kunci utama, atau {@code null} untuk objek yang belum pernah disimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama secara manual.
	 *
	 * <p>Umumnya hanya dipakai untuk membentuk objek "penunjuk" sebagai parameter kriteria; jangan
	 * dipakai untuk mengganti identitas baris yang sudah tersimpan.</p>
	 *
	 * @param id kunci utama yang baru.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan siswa yang melakukan pelanggaran pada kejadian ini.
	 *
	 * <p><b>Pemetaan.</b> {@code @ManyToOne} lazy ke kolom {@code siswa_id} ({@code NOT NULL})
	 * dengan {@code cascade = {PERSIST, MERGE}}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(siswa)} milik
	 * {@link ais.database.model.GeneralValueObject} untuk meresolusi proxy lazy (cache memori
	 * &rarr; inisialisasi proxy &rarr; muat ulang lewat sesi baru), sehingga getter ini aman
	 * dipanggil di luar sesi Hibernate yang membuat objeknya. Field {@code siswa} ikut ditulis
	 * dengan hasil resolusi.</p>
	 *
	 * <p><b>Getter ini adalah simpul privasi kelas ini</b> — nilainya yang menentukan riwayat
	 * disipliner milik siapa yang sedang dibaca. Lihat peringatan {@code task_5e93a600} pada
	 * Javadoc kelas: tidak ada satu pun pemanggil di layar master maupun dasbor yang
	 * membandingkannya dengan pengguna yang sedang login.</p>
	 *
	 * @return siswa pemilik catatan; secara teori tidak pernah {@code null} untuk baris yang
	 *         tersimpan, tetapi kode pembacanya tetap memeriksa {@code null} karena resolusi proxy
	 *         bisa gagal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa_id", nullable = false)
	public Siswa getSiswa() {
		siswa = check(siswa);
		return this.siswa;
	}

	/**
	 * Menyetel siswa pelaku pelanggaran.
	 *
	 * <p>Dipanggil {@code PelanggaranSiswaAction.onSave(Event)} dari nilai bandbox pencarian
	 * siswa. Tidak ada penjagaan {@code null} maupun pemeriksaan bahwa siswa tersebut memang
	 * berada dalam cakupan sekolah pengguna yang menyimpan.</p>
	 *
	 * @param siswa siswa pelaku pelanggaran.
	 */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/**
	 * Mengembalikan sekolah pemilik baris ini (cakupan tenant) — <b>getter destruktif</b>.
	 *
	 * <p><b>Efek samping:</b> nilai {@code sekolah} <b>selalu ditimpa</b> dengan
	 * {@code getSiswa().getSekolah()} bila siswanya berhasil diresolusi, baru kemudian dilewatkan
	 * {@code check(...)}. Karena pemetaan memakai <i>property access</i> dengan
	 * {@code dynamicUpdate = true}, sekadar membaca baris ini dalam sesi aktif dapat memicu
	 * {@code UPDATE} kolom {@code sekolah_id} beserta revisi Envers baru. Konsekuensi lain: nilai
	 * yang disetel lewat {@link #setSekolah(Sekolah)} tidak akan bertahan selama {@code siswa}
	 * terisi.</p>
	 *
	 * <p>Sisi positifnya, kolom cakupan selalu konsisten dengan siswa pemiliknya sehingga filter
	 * pencarian sekolah di layar master tidak bisa kedaluwarsa.</p>
	 *
	 * @return sekolah pemilik baris; {@code null} bila siswa maupun field-nya tidak dapat
	 *         diresolusi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		siswa = getSiswa();
		if (siswa != null) {
			sekolah = siswa.getSekolah();
		}
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris, dengan penjagaan "tolak objek tanpa id".
	 *
	 * <p>Argumen {@code null} <b>atau</b> objek {@link Sekolah} yang {@code getId()}-nya masih
	 * {@code null} (mis. objek kosong dari combobox yang belum dipilih) sama-sama disimpan sebagai
	 * {@code null}, agar Hibernate tidak mencoba membuat baris sekolah baru lewat
	 * {@code cascade = PERSIST}.</p>
	 *
	 * <p><b>Perhatikan:</b> nilai apa pun yang disetel di sini akan ditimpa pada pembacaan
	 * berikutnya oleh {@link #getSekolah()}.</p>
	 *
	 * @param sekolah sekolah pemilik baris; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik baris ini (cakupan tenant tingkat atas) — <b>getter
	 * destruktif berantai</b>.
	 *
	 * <p><b>Efek samping:</b> memanggil {@link #getSekolah()} lebih dulu (yang sendirinya sudah
	 * destruktif dan ikut meresolusi {@code siswa}), lalu <b>menimpa</b> {@code yayasan} dengan
	 * {@code sekolah.getYayasan()}. Satu pembacaan getter ini karena itu berpotensi menghasilkan
	 * dua penulisan kolom sekaligus ({@code sekolah_id} dan {@code yayasan_id}) pada flush
	 * berikutnya.</p>
	 *
	 * @return yayasan pemilik baris; {@code null} bila rantai siswa &rarr; sekolah &rarr; yayasan
	 *         terputus.
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
	 * Menyetel yayasan pemilik baris, dengan penjagaan "tolak objek tanpa id" yang sama persis
	 * dengan {@link #setSekolah(Sekolah)}.
	 *
	 * <p><b>Perhatikan:</b> nilai apa pun yang disetel di sini akan ditimpa pada pembacaan
	 * berikutnya oleh {@link #getYayasan()}.</p>
	 *
	 * @param yayasan yayasan pemilik baris; {@code null} atau objek tanpa id akan disimpan sebagai
	 *                {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan keterangan bebas mengenai kejadian pelanggaran ini (kolom
	 * {@code keterangan}).
	 *
	 * <p>Berbeda dari sebagian entity keluarga lain, getter ini <b>tidak</b> membalik kontrak:
	 * benar-benar mengembalikan field {@code keterangan} apa adanya, tanpa efek samping.</p>
	 *
	 * <p>Teks ini adalah bagian paling sensitif dari baris — ditulis petugas, ditampilkan mentah
	 * di grid, popup info siswa, dasbor, laporan, dan rapor.</p>
	 *
	 * @return keterangan kejadian, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan bebas mengenai kejadian pelanggaran ini.
	 *
	 * <p>Dipanggil {@code PelanggaranSiswaAction.onSave(Event)} dari isi {@code Textbox}
	 * keterangan, apa adanya (tanpa {@code trim} maupun pembatasan panjang).</p>
	 *
	 * @param keterangan teks keterangan yang baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan label baris (kolom {@code nama}, {@code NOT NULL}) — <b>nilai TURUNAN,
	 * getter destruktif</b>.
	 *
	 * <p><b>Efek samping:</b> field {@code nama} <b>selalu ditimpa</b> dengan
	 * {@code getSiswa() + "_" + getPelanggaranDanHukuman() + "_" + getWaktu()} sebelum
	 * dikembalikan. Karena ketiga getter yang dipanggil sendiri punya efek samping
	 * ({@code check(...)} pada dua relasi, dan nilai bawaan "sekarang" pada {@link #getWaktu()}),
	 * satu pembacaan {@code getNama()} bisa memicu resolusi proxy sekaligus penulisan ulang kolom
	 * {@code nama} pada flush berikutnya.</p>
	 *
	 * <p><b>Bentuk hasilnya</b> mengikuti {@code toString()} masing-masing bagian:
	 * {@link Siswa} meng-override-nya menjadi {@code id + "-" + nomorInduk + "-" + namaSiswa},
	 * sedangkan {@code PelanggaranDanHukuman} tidak meng-override sehingga memakai
	 * {@code GeneralValueObject.toString()} = {@code kode + " - " + nama}. Contoh nilai yang
	 * benar-benar tersimpan: {@code "412-19001-Budi_ - Terlambat Berat_Wed Sep 03 08:12:44 WIB 2026"}.</p>
	 *
	 * <p><b>Akibat yang perlu diketahui:</b> (a) {@link #setNama(String)} praktis mati — apa pun
	 * yang ditulis ke sana hilang pada pembacaan berikutnya; (b) impor Excel lewat
	 * {@code Common.uploadData(...)} yang menyertakan kolom "nama" tidak berpengaruh; (c) nilai
	 * kolom ikut berubah bila nama/nomor induk siswa diperbarui, jadi tidak layak dipakai sebagai
	 * kunci historis; (d) {@code DasbordPelanggaran} memakai nilai ini sebagai <i>fallback</i>
	 * label subjek ketika relasi {@code siswa} kosong, sehingga teks gabungan itu bisa muncul apa
	 * adanya di layar; dan (e) {@code compareTo} warisan induk jatuh ke kunci {@code nama} untuk
	 * entity ini ({@code nomorUrut}/{@code nim} tidak pernah dipetakan), sehingga pengurutan
	 * {@code TreeSet} atas objek {@code PelanggaranSiswa} sebenarnya mengurutkan teks gabungan
	 * tersebut — bukan waktu kejadian.</p>
	 *
	 * @return label gabungan siswa + paket pelanggaran + waktu; tidak pernah {@code null}.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		nama = getSiswa() + "_" + getPelanggaranDanHukuman() + "_" + getWaktu();
		return this.nama;
	}

	/**
	 * Menyetel field {@code nama} secara manual.
	 *
	 * <p><b>Praktis tidak berguna:</b> {@link #getNama()} selalu menghitung ulang nilainya,
	 * sehingga apa pun yang disetel di sini hilang pada pembacaan pertama. Method tetap ada karena
	 * dibutuhkan Hibernate saat hidrasi dan oleh utilitas impor berbasis refleksi.</p>
	 *
	 * @param nama label yang diinginkan — akan diabaikan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan penanda baris masih berlaku, dengan bawaan menyala.
	 *
	 * <p>Nilai {@code null} (baris lama yang belum pernah menyimpan kolom ini) diperlakukan
	 * sebagai {@code true}, sehingga baris warisan tetap terhitung aktif. Perhatikan bahwa
	 * penanda ini <b>tidak</b> disaring oleh grid layar master maupun {@code DasbordPelanggaran};
	 * yang benar-benar memakainya hanya {@code LaporanPelanggaranSiswa} dan
	 * {@code PelanggaranSiswaAction.checkDanTampil(Siswa)}. Menonaktifkan sebuah catatan karena
	 * itu <b>tidak</b> menyembunyikannya dari daftar utama.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> tidak ada anotasi {@code @Column}, jadi Hibernate memakai nama
	 * kolom bawaan {@code aktif}.</p>
	 *
	 * @return {@code true} bila baris masih berlaku (termasuk saat field masih {@code null}).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel penanda baris masih berlaku.
	 *
	 * <p>Dipanggil langsung dari pendengar {@code onCheck} sebuah checkbox di renderer grid layar
	 * master, yang segera menyusulnya dengan {@code Common.refreshSaveOrUpdate(...)} — jadi
	 * mencentang/melepas centang di grid <b>langsung menulis ke database</b> tanpa dialog
	 * konfirmasi. Checkbox itu dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param aktif penanda berlaku yang baru; {@code null} akan dibaca kembali sebagai
	 *              {@code true}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan paket pelanggaran-dan-hukuman (master lapis 3) yang dipilih untuk kejadian
	 * ini.
	 *
	 * <p><b>Pemetaan.</b> {@code @ManyToOne} lazy ke kolom {@code pelanggaran_dan_hukuman}
	 * ({@code NOT NULL}), {@code cascade = {PERSIST, MERGE}}. Paket inilah yang menentukan
	 * <b>himpunan pilihan</b> yang boleh muncul di {@link #getPelanggarans()} dan
	 * {@link #getHukumans()}.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@code check(...)} untuk meresolusi proxy lazy. Berbeda
	 * dari getter relasi lain di kelas ini, tidak ada penulisan nilai turunan.</p>
	 *
	 * <p><b>Perhatikan:</b> nilai getter ini ikut membentuk {@link #getNama()}, dan karena
	 * {@code PelanggaranDanHukuman} tidak meng-override {@code toString()}, yang masuk ke label
	 * adalah {@code kode + " - " + nama} milik paket.</p>
	 *
	 * @return paket pelanggaran-dan-hukuman; secara teori tidak pernah {@code null} untuk baris
	 *         tersimpan, tetapi pembacanya tetap memeriksa {@code null}.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pelanggaran_dan_hukuman", nullable = false)
	public PelanggaranDanHukuman getPelanggaranDanHukuman() {
		pelanggaranDanHukuman = check(pelanggaranDanHukuman);
		return pelanggaranDanHukuman;
	}

	/**
	 * Menyetel paket pelanggaran-dan-hukuman kejadian ini.
	 *
	 * <p>Dipanggil {@code PelanggaranSiswaAction.onSave(Event)} dari item combobox terpilih, yang
	 * sebelumnya sudah divalidasi tidak boleh kosong. Mengganti paket <b>tidak</b> otomatis
	 * membersihkan {@link #getPelanggarans()}/{@link #getHukumans()} yang sudah tersimpan,
	 * sehingga baris bisa berakhir memuat rincian yang tidak lagi ditawarkan paket barunya.</p>
	 *
	 * @param pelanggaranDanHukuman paket master yang dipilih.
	 */
	public void setPelanggaranDanHukuman(PelanggaranDanHukuman pelanggaranDanHukuman) {
		this.pelanggaranDanHukuman = pelanggaranDanHukuman;
	}

	/**
	 * Mengembalikan waktu kejadian pelanggaran (kolom {@code waktu}, {@code TIMESTAMP}), dengan
	 * <b>nilai bawaan "sekarang"</b>.
	 *
	 * <p>Bila field masih {@code null}, method mengembalikan {@code WaktuUtil.getDate()}. Karena
	 * pemetaan memakai <i>property access</i>, nilai bawaan itulah yang benar-benar ditulis ke
	 * kolom pada {@code INSERT} — bukan {@code NULL}. Perhatikan bahwa nilai bawaan ini
	 * <b>tidak</b> ikut tersimpan ke field, jadi dua pemanggilan berturut-turut pada objek yang
	 * belum tersimpan dapat mengembalikan cap waktu yang berbeda.</p>
	 *
	 * <p><b>Kuirk yang penting:</b> layar master menampilkan datebox "Tanggal dan Waktu
	 * Pelanggaran *" tetapi komponen itu {@code setReadonly(true)} dan {@code onSave(Event)}
	 * <b>tidak pernah membacanya</b>. Praktisnya, kolom ini berisi <b>waktu penyimpanan baris</b>,
	 * bukan waktu kejadian yang dimaksud petugas. Nilai ini dipakai untuk pengurutan grid
	 * ({@code Order.desc("waktu")}), rentang tanggal laporan, serta pengelompokan bulanan/harian
	 * di dasbor — semuanya karena itu mencerminkan waktu entri data.</p>
	 *
	 * @return waktu kejadian, atau cap waktu saat ini bila belum pernah diisi; tidak pernah
	 *         {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menyetel waktu kejadian pelanggaran.
	 *
	 * <p>Tidak ditemukan pemanggil di jalur simpan layar master (lihat kuirk pada
	 * {@link #getWaktu()}); praktis hanya dipakai Hibernate saat hidrasi. Menyetel {@code null}
	 * mengembalikan perilaku bawaan "sekarang".</p>
	 *
	 * @param waktu waktu kejadian yang baru.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan tahun ajaran kejadian ini, dengan <b>nilai bawaan tahun akademik
	 * berjalan</b>.
	 *
	 * <p>Bila field masih {@code null}, method mengembalikan
	 * {@code Common.getCurrentTahunAkademik()} — nilai global instalasi, bukan turunan dari
	 * {@link #getWaktu()}. Seperti {@link #getWaktu()}, nilai bawaan itu tidak disimpan ke field
	 * tetapi tetap ikut tertulis ke kolom karena pemetaan property access.</p>
	 *
	 * <p><b>Peran fungsional:</b> ini adalah penyaring utama saat rapor mengambil riwayat
	 * pelanggaran ({@code LaporanRaporSiswa} memakai {@code Restrictions.eq("ta", ta)}), sehingga
	 * salah tahun ajaran berarti pelanggaran tidak muncul di rapor. Di layar master nilainya
	 * memang selalu ditimpa {@code onSave(Event)} dari combobox tahun ajaran, jadi bawaan ini
	 * hanya berlaku untuk baris yang dibuat lewat jalur lain (mis. impor).</p>
	 *
	 * <p><b>Catatan pemetaan:</b> tanpa anotasi {@code @Column}, jadi memakai nama kolom bawaan
	 * {@code ta}.</p>
	 *
	 * @return tahun ajaran kejadian (mis. {@code "2025/2026"}), atau tahun akademik berjalan bila
	 *         belum diisi.
	 */
	public String getTa() {
		return ta == null ? Common.getCurrentTahunAkademik() : ta;
	}

	/**
	 * Menyetel tahun ajaran kejadian ini.
	 *
	 * <p>Dipanggil {@code PelanggaranSiswaAction.onSave(Event)} dari nilai combobox tahun ajaran
	 * (yang diisi {@code Common.generateTahunAjaran(...)}). Tidak ada validasi format maupun
	 * pemeriksaan konsistensi dengan {@link #getWaktu()}.</p>
	 *
	 * @param ta tahun ajaran yang baru.
	 */
	public void setTa(String ta) {
		this.ta = ta;
	}

	/**
	 * Mengembalikan batas akhir masa tayang popup info kedisiplinan (kolom
	 * {@code batasWaktuDitampilkan}, dipetakan sebagai {@code DATE} sehingga komponen jam
	 * dibuang).
	 *
	 * <p><b>Dua cacat yang membuat kolom ini praktis tidak berfungsi</b> (keduanya di lapis layar,
	 * bukan di entity ini, dan keduanya kembar persis dengan
	 * {@code ais.action.master.PelanggaranMahasiswaAction}):</p>
	 * <ol>
	 *   <li>{@code PelanggaranSiswaAction} mendaftarkan pendengar perubahan datebox ini pada event
	 *       {@code "onCheck"} — event yang tidak pernah dipancarkan sebuah {@code Datebox} —
	 *       sehingga tanggal yang diketik operator <b>tidak pernah sampai ke setter</b>.</li>
	 *   <li>{@code checkDanTampil(Siswa)} menyaring dengan
	 *       {@code Restrictions.le("batasWaktuDitampilkan", sekarang)}, yaitu "batas waktu sudah
	 *       lewat" — <b>kebalikan</b> dari maksud kolomnya. Andai nilainya sempat tersimpan, popup
	 *       justru baru muncul setelah masa tayangnya habis.</li>
	 * </ol>
	 * <p>Karena kolom ini praktis selalu {@code null}, cabang {@code Restrictions.isNull(...)} di
	 * query itulah yang selalu terpakai, sehingga popup tampil tanpa batas waktu.</p>
	 *
	 * @return batas akhir masa tayang, atau {@code null} (kondisi yang praktis selalu terjadi).
	 */
	@Temporal(TemporalType.DATE)
	public Date getBatasWaktuDitampilkan() {
		return batasWaktuDitampilkan;
	}

	/**
	 * Menyetel batas akhir masa tayang popup info kedisiplinan.
	 *
	 * <p>Satu-satunya pemanggil di layar master terpasang pada event yang salah (lihat
	 * {@link #getBatasWaktuDitampilkan()}), jadi praktis hanya Hibernate yang memakainya saat
	 * hidrasi.</p>
	 *
	 * @param batasWaktuDitampilkan batas akhir masa tayang yang baru.
	 */
	public void setBatasWaktuDitampilkan(Date batasWaktuDitampilkan) {
		this.batasWaktuDitampilkan = batasWaktuDitampilkan;
	}

	/**
	 * Mengembalikan penanda "tampilkan catatan ini saat siswa yang bersangkutan login", dengan
	 * <b>bawaan MENYALA</b>.
	 *
	 * <p>Nilai {@code null} diperlakukan sebagai {@code true}. Karena {@code onSave(Event)} tidak
	 * pernah mengisi kolom ini, <b>setiap catatan pelanggaran yang baru dibuat langsung berstatus
	 * "tampil saat login"</b> — operator harus melepas centangnya secara sadar di grid bila tidak
	 * menginginkannya. Perhatikan bahwa {@code checkDanTampil(Siswa)} menyaring dengan
	 * {@code Restrictions.eq("tampilkanInfoIniSaatSiswaLogin", true)}, yaitu perbandingan
	 * <b>langsung ke kolom</b>: baris warisan yang kolomnya masih {@code NULL} <b>tidak</b> lolos
	 * saringan itu, meski getter ini melaporkannya {@code true}. Jadi bawaan "menyala" hanya
	 * berlaku di lapis Java, tidak di lapis SQL.</p>
	 *
	 * <p><b>Catatan pemetaan:</b> tanpa anotasi {@code @Column}, memakai nama kolom bawaan hasil
	 * strategi penamaan Hibernate untuk properti {@code tampilkanInfoIniSaatSiswaLogin}.</p>
	 *
	 * @return {@code true} bila catatan ini ditandai untuk ditampilkan saat siswa login (termasuk
	 *         saat field masih {@code null}).
	 */
	public Boolean getTampilkanInfoIniSaatSiswaLogin() {
		return tampilkanInfoIniSaatSiswaLogin == null ? true : tampilkanInfoIniSaatSiswaLogin;
	}

	/**
	 * Menyetel penanda "tampilkan catatan ini saat siswa yang bersangkutan login".
	 *
	 * <p>Dipanggil dari pendengar {@code onCheck} sebuah checkbox di renderer grid layar master
	 * yang segera menyusulnya dengan penyimpanan — jadi perubahan di grid langsung menulis ke
	 * database. Checkbox itu dinonaktifkan bila pengguna tidak punya hak {@code UPDATE}.</p>
	 *
	 * @param tampilkanInfoIniSaatSiswaLogin penanda tampil-saat-login yang baru.
	 */
	public void setTampilkanInfoIniSaatSiswaLogin(Boolean tampilkanInfoIniSaatSiswaLogin) {
		this.tampilkanInfoIniSaatSiswaLogin = tampilkanInfoIniSaatSiswaLogin;
	}
}
