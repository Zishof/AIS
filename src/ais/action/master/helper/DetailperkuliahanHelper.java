package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.EksporFromFeederAction;
import ais.action.master.MahasiswaAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.feeder.util.FeederConnector;
import ais.action.master.feeder.util.FeederExporter;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecilSekali;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper composer ZK untuk halaman admin/dosen "Daftar Mahasiswa" pada satu {@link Perkuliahan}:
 * menampilkan seluruh {@link Detailperkuliahan} (mahasiswa peserta KRS) dari sudut pandang
 * perkuliahan (kebalikan {@link KrsHelper}/{@link KrsPaketHelper} yang berpusat pada mahasiswa).
 * Setiap baris menampilkan foto, riwayat revisi, status validitas data Feeder Dikti, nama,
 * angkatan, status kemahasiswaan, total nilai, status persetujuan (editable via textbox keterangan
 * + intbox semester/tahap), dan tombol Pindah Data/Ubah Persetujuan/Hapus per baris — hak tampil
 * tombol dikontrol lewat flag {@code delete}/{@code edit}/{@code approve}/{@code reject}/{@code create}
 * yang diberikan lewat konstruktor.
 *
 * <p>
 * Toolbar menyediakan: pencarian NIM/nama; Refresh; "Singkronkan" (menjalankan
 * {@code perkuliahan.singkronkan()} di thread terpisah dengan polling timer); "Ambil Mhs" (buka
 * {@code AmbilDataMahasiswaHelper}); "Transfer"/"Copy mhs" (pindah/salin mahasiswa ke perkuliahan
 * lain); "Setujui"/"Tolak"/"Hapus" massal untuk seluruh mahasiswa (dibatasi role
 * Akademik/AdminFakultas/AdminJurusan/Admin); cetak laporan Absensi/UTS/UAS; unduh/unggah data
 * Excel (format kolom {@code mahasiswa, semester, tahap, persetujuan}, diproses baris-per-baris
 * asinkron via {@link #uploadDataMahasiswa} dengan laporan hasil per baris
 * {@link ais.common.LaporanUpload}); dan "History" (buka {@code RevisiDetailPerkuliahanHelper}).
 * </p>
 *
 * <p>
 * Method statis {@link #kirimKeFeeder} menyediakan tombol pengiriman satu
 * {@link Detailperkuliahan} ke Feeder Dikti (Neo Feeder) — dipakai baik oleh perender baris kelas
 * ini maupun dipanggil dari konteks lain — yang menjalankan proses ekspor di thread terpisah
 * dengan progress bar dan log error yang dapat diunduh sebagai file teks bila gagal.
 * </p>
 *
 * <p>
 * Mengimplementasikan {@link DataCriteria} ({@link #initCriteria(boolean)}, dipakai fitur cetak
 * data) dan {@link DataLoader} ({@link #loadData(Object)}).
 * </p>
 *
 * <h3>Pembagian tanggung jawab dengan DetailperkuliahanForPenilaianHelper</h3>
 * <p>
 * Kedua kelas sama-sama menampilkan grid berisi seluruh {@link Detailperkuliahan} dari satu
 * {@link Perkuliahan}, dan karena kemiripan nama sering tertukar. Keduanya <b>tidak</b> saling
 * memanggil, <b>tidak</b> berbagi kode, dan <b>tidak</b> tumpang tindih fungsi. Pembagiannya tegas:
 * kelas ini mengurus <b>keanggotaan</b> kelas, kelas satunya mengurus <b>nilai</b> peserta.
 * </p>
 * <table border="1">
 * <caption>Perbandingan definitif</caption>
 * <tr><th>Aspek</th><th>{@code DetailperkuliahanHelper} (kelas ini)</th>
 *     <th>{@code DetailperkuliahanForPenilaianHelper}</th></tr>
 * <tr><td>Pertanyaan yang dijawab</td><td><i>Siapa saja peserta kelas ini, dan apakah KRS-nya
 *     sah?</i></td><td><i>Berapa nilai tiap peserta pada tiap komponen penilaian?</i></td></tr>
 * <tr><td>Kolom {@link Detailperkuliahan} yang <b>ditulis</b></td>
 *     <td>{@code persetujuan}, {@code semester}, {@code tahap}, {@code detailNilaiTambahan}
 *         (catatan teks), {@code mahasiswa}/{@code perkuliahan} (saat pendaftaran &amp; transfer),
 *         serta penghapusan baris</td>
 *     <td>{@code totalNilai}, {@code nilaiHuruf}, seluruh {@code DetailperkuliahanPunyaNilai} per
 *         {@code FormatNilai}, status kunci/verifikasi nilai, komentar penilaian</td></tr>
 * <tr><td>Kolom nilai</td><td><b>Hanya dibaca</b> — {@code totalNilai}/{@code nilaiHuruf} tampil
 *     sebagai label, dan {@code totalNilai} dipakai sebagai <i>penjaga</i>: baris bernilai tidak nol
 *     tidak boleh dihapus / dicabut persetujuannya bila konfigurasi
 *     {@code batalkan_persetujuan_harus_memiliki_nilai_nol} aktif</td>
 *     <td>Ditulis; kelas ini juga yang memiliki mesin hitung ulang, kunci, dan verifikasi</td></tr>
 * <tr><td>Kontrak antarmuka</td><td>{@link DataCriteria} <i>dan</i> {@link DataLoader} — punya
 *     {@link #initCriteria(boolean)} sehingga bisa diekspor lewat {@link Common#cetakData}</td>
 *     <td>Hanya {@link DataLoader}</td></tr>
 * <tr><td>Integrasi luar</td><td>Feeder Dikti (kirim data perkuliahan / nilai transfer per
 *     mahasiswa), impor/ekspor Excel daftar peserta, cetak Absensi/UTS/UAS</td>
 *     <td>Format nilai OBE/CPMK, asisten penilai, gerbang pembayaran untuk entri nilai</td></tr>
 * <tr><td>Hak akses</td><td>Lima flag konstruktor ({@code delete}/{@code edit}/{@code approve}/
 *     {@code reject}/{@code create}), ditambah penyaringan peran eksplisit untuk tiga tombol massal
 *     (lihat di bawah)</td>
 *     <td>Flag {@code edit}/{@code aktifPenilaianData} dari Action pemanggil</td></tr>
 * </table>
 * <p>
 * Konsekuensi praktis: perubahan yang berkaitan dengan pendaftaran, pembatalan, pemindahan, atau
 * pengesahan peserta kelas dilakukan <b>di kelas ini</b>; perubahan yang berkaitan dengan angka
 * nilai dilakukan di {@code DetailperkuliahanForPenilaianHelper}. Satu-satunya titik singgung adalah
 * pembacaan {@code totalNilai} sebagai penjaga di atas.
 * </p>
 *
 * <h3>Gerbang otorisasi</h3>
 * <p>
 * Kelas ini memakai <b>dua lapis</b> kendali yang perlu dibedakan:
 * </p>
 * <ol>
 * <li><b>Lima flag konstruktor</b> ({@code delete}, {@code edit}, {@code approve}, {@code reject},
 * {@code create}). Nilainya ditentukan oleh Action pemanggil, umumnya dari hak akses menu. Flag ini
 * hanya mengatur {@code setVisible}/{@code setDisabled} pada tombol — ia menentukan apa yang
 * <i>tampil</i>, bukan apa yang <i>boleh tersimpan</i>.</li>
 * <li><b>Penyaringan peran eksplisit</b> untuk tiga tombol massal "Setujui", "Tolak", dan "Hapus":
 * tombol hanya terlihat bila {@link Common#getApakahAdmin()} atau {@code roleId} pengguna cocok
 * dengan {@code ConstantValues.Akademik}, {@code roleAdminFakultas}, atau {@code roleAdminJurusan}.
 * Ini satu-satunya tempat di kelas ini yang benar-benar memeriksa peran, dan hanya untuk aksi
 * massal — aksi <i>per-baris</i> yang setara (Ubah Persetujuan, Hapus Data) tidak melewatinya dan
 * hanya bergantung pada flag {@code edit}/{@code delete}.</li>
 * </ol>
 * <p>
 * Yang <b>tidak</b> diperiksa di mana pun: apakah pengguna adalah dosen pengampu perkuliahan yang
 * sedang dibuka, dan apakah perkuliahan berada dalam cakupan fakultas/program studi pengguna.
 * Satu-satunya pembeda berbasis profil adalah {@code Common.getCurrentUser().getDosen() == null}
 * yang menyembunyikan tombol "Pindah Data" dari akun dosen. Pola "penjaga UI tanpa penjaga
 * penyimpanan" ini sama dengan yang didokumentasikan pada
 * {@code DetailperkuliahanForPenilaianHelper}.
 * </p>
 *
 * <h3>Jejak audit</h3>
 * <p>
 * {@link Detailperkuliahan} beranotasi Envers, sehingga perubahan persetujuan, semester, tahap, dan
 * keterangan terekam sebagai revisi yang dapat dibaca lewat {@link RevisiHelper#createNewRevisi}
 * per-baris dan tombol "History" ({@code RevisiDetailPerkuliahanHelper}). Sebagaimana seluruh entity
 * ber-audit di aplikasi ini, revisi merekam <i>apa</i> dan <i>kapan</i>, tetapi tidak merekam
 * <i>siapa</i> — repositori tidak mendefinisikan {@code @RevisionEntity}/{@code RevisionListener}
 * kustom sehingga Envers memakai {@code DefaultRevisionEntity} bawaan yang hanya menyimpan nomor
 * revisi dan timestamp.
 * </p>
 *
 * <h3>Model sesi dan transaksi</h3>
 * <p>
 * Kelas ini memakai <b>tiga</b> pola sesi Hibernate yang berbeda, dan pemilihannya bukan kebetulan:
 * </p>
 * <ul>
 * <li>{@code HibernateUtil.currentSession()} + {@code Common.refreshUpdate(...)} — untuk penulisan
 * ringan yang berjalan langsung di thread event ZK (auto-save keterangan/semester/tahap, toggle
 * persetujuan, aksi massal).</li>
 * <li>{@code HibernateUtil.currentNativeSession()} dengan {@code closeSession()} di {@code finally}
 * — untuk operasi yang membutuhkan transaksi eksplisit atau pemanggilan
 * {@code perkuliahan.singkronkan(...)}. Penutupan di {@code finally} dan {@code rollback} pada
 * kegagalan penting: tanpa itu sesi bocor dan transaksi tertinggal aktif.</li>
 * <li>{@code HibernateUtil.getSessionFactory().openSession()} — <b>wajib</b> untuk pekerjaan di
 * {@link Thread} latar ({@link #uploadDataMahasiswa}), karena sesi thread-cache sudah ditutup ketika
 * request asal selesai sehingga pemakaiannya melempar "Session is closed!".</li>
 * </ul>
 * <p>
 * Tidak ada penguncian baris (<i>pessimistic lock</i>) di mana pun; dua pengguna yang membuka kelas
 * yang sama secara bersamaan dapat saling menimpa status persetujuan.
 * </p>
 *
 * @see Detailperkuliahan
 * @see Perkuliahan
 * @see DetailperkuliahanForPenilaianHelper
 */
public class DetailperkuliahanHelper implements DataCriteria, DataLoader {

	/**
	 * Grid daftar peserta perkuliahan — satu baris per {@link Detailperkuliahan}.
	 *
	 * <p>Dibuat di {@link #display} dengan {@code mold="paging"} tetapi {@code pageSize=10000},
	 * sehingga secara praktis seluruh peserta tampil sekaligus tanpa navigasi halaman; angka besar itu
	 * disengaja karena satu kelas perkuliahan tidak pernah mendekati sepuluh ribu mahasiswa, sementara
	 * dosen ingin melihat dan mencetak seluruh daftar dalam satu layar. Konsekuensinya seluruh baris
	 * dirender di sisi server pada setiap {@link #loadData(Object)}.
	 *
	 * <p>Model dan renderer ({@link DetailPerkuliahanRenderer}) dipasang ulang setiap kali
	 * {@link #loadData(Object)} dijalankan.
	 */
	private MyGrid grid;

	/**
	 * Perkuliahan (kelas mata kuliah) yang daftar pesertanya sedang dikelola — konteks tunggal seluruh
	 * kelas ini.
	 *
	 * <p>Diisi lewat {@link #display} atau {@link #setPerkuliahan}. Dibaca oleh
	 * {@link #initCriteria(boolean)} sebagai filter {@code eq("perkuliahan", ...)}, oleh
	 * {@link #loadData(Object)} sebagai sumber daftar id peserta, dan oleh
	 * {@link #uploadDataMahasiswa} sebagai perkuliahan tujuan baris-baris Excel yang diunggah. Juga
	 * menjadi sumber nilai default {@code semester} dan {@code tahap} ketika kotak isian pada baris
	 * dikosongkan pengguna.
	 *
	 * <p>Bernilai {@code null} sampai salah satu dari kedua penyetel dipanggil; seluruh method lain
	 * mengasumsikannya sudah terisi.
	 */
	private Perkuliahan perkuliahan;
	// private Textbox nim;

	/**
	 * Kotak pencarian peserta pada toolbar — dipakai untuk NIM <b>maupun</b> nama, meski labelnya hanya
	 * "Mhs :".
	 *
	 * <p>Nilainya dibaca di dua tempat dengan mekanisme yang berbeda:
	 * {@link #initCriteria(boolean)} memakainya sebagai klausa {@code ilike} berpola
	 * {@link MatchMode#ANYWHERE} pada {@code mahasiswa.nim} <i>atau</i> {@code mahasiswa.nama} (jalur
	 * ekspor/cetak data), sedangkan {@link #loadData(Object)} meneruskannya ke
	 * {@code perkuliahan.ambilDetailperkuliahan(...)} (jalur tampilan grid). Kedua jalur karenanya bisa
	 * saja menerapkan aturan pencocokan yang tidak persis sama.
	 *
	 * <p>Menekan Enter di kotak ini memicu {@code loadData(null)} — memuat ulang tanpa membangun ulang
	 * cache, berbeda dari tombol Refresh yang memanggil {@code loadData(true)}.
	 *
	 * <p>Diinisialisasi di {@link #display}; {@link #initCriteria(boolean)} sudah menjaga kemungkinan
	 * field ini masih {@code null} (menggantinya dengan {@code sqlRestriction("true")}), tetapi
	 * {@link #loadData(Object)} tidak — memanggilnya sebelum {@code display()} akan melempar NPE.
	 */
	private Textbox nama;

	// private Paging paging;

	/**
	 * Penanda konteks <b>semester pendek</b>: {@code null} untuk perkuliahan reguler, selain itu berisi
	 * status semester pendek yang berlaku.
	 *
	 * <p>Diterima lewat konstruktor dan diteruskan ke dua tempat: {@code AmbilDataMahasiswaHelper} saat
	 * mengambil mahasiswa baru ke kelas ini, dan {@code Common.checkStatusPembayaranMahasiswa} pada
	 * {@link #uploadDataMahasiswa} — di sana ia dikirim sebagai boolean {@code semesterPendek != null}
	 * yang menentukan tagihan mana yang diperiksa. Karena hanya kehadiran/ketiadaannya yang dipakai di
	 * jalur unggah, <i>nilai</i> integernya hanya bermakna bagi helper Ambil Mhs.
	 */
	private Integer semesterPendek;

	/**
	 * Izin menghapus: mengatur visibilitas tombol Hapus per-baris dan tombol "Hapus" massal pada
	 * toolbar.
	 *
	 * <p>Sengaja {@code protected} (berbeda dari empat flag lain yang {@code private}) agar subclass
	 * dalam paket turunan dapat menyesuaikannya; pemeliharaan berikutnya sebaiknya memeriksa apakah
	 * pembedaan visibilitas ini masih diperlukan.
	 *
	 * <p><b>Perlu diketahui:</b> flag ini hanya menyembunyikan tombol. Penjaga penghapusan yang
	 * sesungguhnya ada di listener tombol per-baris — baris yang sudah {@code DISETUJUI} ditolak, baris
	 * yang masih dirujuk {@link MahasiswaRequestTugasAkhir} ditolak, dan baris bernilai tidak nol
	 * ditolak bila konfigurasi {@code batalkan_persetujuan_harus_memiliki_nilai_nol} aktif. Tombol
	 * massal menerapkan penjaga yang lebih longgar: ia hanya melewati baris yang belum disetujui, tanpa
	 * memeriksa relasi tugas akhir.
	 */
	protected boolean delete;

	/**
	 * Izin mengubah: mengatur visibilitas tombol "Pindah Data" dan "Ubah Persetujuan" per-baris, serta
	 * status aktif tombol toolbar "Transfer", "Copy mhs", dan "History".
	 *
	 * <p>Tombol "Pindah Data" menambahkan satu syarat lagi di luar flag ini:
	 * {@code Common.getCurrentUser().getDosen() == null}, sehingga akun dosen tidak dapat memindahkan
	 * KRS mahasiswa meski flag {@code edit} bernilai {@code true}.
	 */
	private boolean edit;

	/**
	 * Izin menyetujui: mengatur status aktif ({@code setDisabled}) tombol "Setujui" massal.
	 *
	 * <p>Berbeda dari visibilitasnya, yang ditentukan penyaringan peran terpisah
	 * ({@link Common#getApakahAdmin()} atau peran Akademik / Admin Fakultas / Admin Jurusan). Kedua
	 * kendali bekerja berdampingan: flag ini menentukan tombol <i>aktif</i>, penyaringan peran
	 * menentukan tombol <i>terlihat</i>.
	 *
	 * <p>Perhatikan bahwa persetujuan per-baris (tombol "Ubah Persetujuan") tidak memakai flag ini
	 * melainkan {@link #edit}, dan tidak melewati penyaringan peran sama sekali.
	 */
	private boolean approve;

	/**
	 * Izin menolak: mengatur status aktif tombol "Tolak" massal, yang mengembalikan seluruh peserta ke
	 * status {@code BELUM_DISETUJUI}.
	 *
	 * <p>Sama seperti {@link #approve}, visibilitas tombolnya ditentukan penyaringan peran terpisah.
	 * Perlu dicatat bahwa aksi "Tolak" massal <b>tidak</b> menerapkan penjaga
	 * {@code batalkan_persetujuan_harus_memiliki_nilai_nol} yang berlaku pada pencabutan persetujuan
	 * per-baris — pencabutan massal dapat mengenai baris yang nilainya sudah terisi.
	 */
	private boolean reject;

	/**
	 * Izin menambah peserta: mengatur status aktif tombol "Ambil Mhs", yang membuka
	 * {@code AmbilDataMahasiswaHelper} untuk memasukkan mahasiswa ke perkuliahan ini.
	 *
	 * <p>Tidak berlaku bagi jalur unggah Excel ({@link #uploadDataMahasiswa}), yang juga membuat baris
	 * {@link Detailperkuliahan} baru tetapi tombolnya tidak dikendalikan flag ini.
	 */
	private boolean create;

	/**
	 * Membentuk helper dengan konteks semester dan <b>lima izin aksi</b> yang sudah ditentukan Action
	 * pemanggil. Konstruktor ini hanya menyimpan parameter; tidak ada akses basis data, tidak ada
	 * komponen ZK yang dibuat, dan {@link #perkuliahan} belum diketahui pada tahap ini. Alur pemakaian
	 * yang benar adalah: konstruktor &rarr; {@link #display} (yang sekaligus menetapkan perkuliahan dan
	 * membangun seluruh UI), atau {@link #setPerkuliahan} bila UI dibangun pihak lain.
	 *
	 * <p><b>Sifat kelima izin:</b> semuanya hanya mengatur {@code setVisible}/{@code setDisabled} pada
	 * tombol. Tidak satu pun dari flag ini diperiksa ulang di dalam listener sebelum penulisan ke basis
	 * data terjadi. Pemanggil karenanya bertanggung jawab penuh menetapkannya sesuai hak akses pengguna;
	 * lihat bagian "Gerbang otorisasi" pada Javadoc kelas. Tiga tombol massal (Setujui, Tolak, Hapus)
	 * mendapat lapisan kedua berupa penyaringan peran di dalam {@link #display}, tetapi tombol per-baris
	 * yang setara tidak.
	 *
	 * @param semesterPendek status semester pendek konteks perkuliahan ({@code null} untuk reguler);
	 *                       diteruskan ke {@code AmbilDataMahasiswaHelper} dan menentukan tagihan mana
	 *                       yang diperiksa saat unggah Excel
	 * @param delete         izinkan tombol hapus per baris dan tombol "Hapus" massal
	 * @param edit           izinkan tombol Pindah Data, Ubah Persetujuan, Transfer, Copy mhs, dan History
	 * @param approve        izinkan tombol "Setujui" massal (mengaktifkan; visibilitasnya masih diatur
	 *                       penyaringan peran)
	 * @param reject         izinkan tombol "Tolak" massal (mengaktifkan; visibilitasnya masih diatur
	 *                       penyaringan peran)
	 * @param create         izinkan tombol "Ambil Mhs"; tidak berpengaruh pada jalur unggah Excel
	 */
	public DetailperkuliahanHelper(Integer semesterPendek, boolean delete, boolean edit, boolean approve,
			boolean reject, boolean create) {
		this.semesterPendek = semesterPendek;
		this.delete = delete;
		this.edit = edit;
		this.approve = approve;
		this.reject = reject;
		this.create = create;
	}

	/**
	 * Daftar <b>id</b> {@link Detailperkuliahan} hasil pemuatan terakhir — sekaligus model baris grid dan
	 * cakupan kerja ketiga tombol aksi massal.
	 *
	 * <p>Berisi id ({@link Long}), bukan entity, sesuai pola umum aplikasi: renderer dan listener
	 * me-resolve tiap id lewat {@code GeneralValueObject.ambilData(...)} saat dibutuhkan, sehingga daftar
	 * tetap ringan dan tidak menahan objek Hibernate yang mungkin sudah detached.
	 *
	 * <p><b>Konsekuensi penting untuk aksi massal:</b> tombol "Setujui", "Tolak", dan "Hapus" beriterasi
	 * atas field ini, bukan atas hasil query baru. Artinya cakupannya adalah <i>apa yang sedang tampil</i>
	 * — bila kotak pencarian {@link #nama} sedang terisi, aksi massal hanya mengenai baris yang lolos
	 * pencarian, bukan seluruh peserta kelas. Teks konfirmasi ("semua mahasiswa di dalam perkuliahan
	 * ini") tidak mencerminkan pembatasan tersebut.
	 *
	 * <p>Bernilai {@code null} sebelum pemuatan pertama; setiap listener massal karenanya memeriksa
	 * {@code detailperkuliahan != null} lebih dulu.
	 */
	private List<Long> detailperkuliahan = null;

	/**
	 * Menambahkan tombol "Kirim ke feeder" ke {@code vbox} (hanya tampil bila user login, admin
	 * berhak akses Feeder, fitur {@code aktifkan_terhubung_langsung_ke_feeder} aktif, dan mahasiswa
	 * sudah punya {@code idRegPd}). Saat diklik: memeriksa ketersediaan server Neo Feeder, lalu
	 * login dan mengirim data perkuliahan (via {@code PerkuliahanAction.kirimKeFeeder}) atau nilai
	 * transfer/konversi (via {@code feederImporter.nilaiTransfer}) di thread terpisah dengan
	 * progress bar; kegagalan (koneksi, kredensial, parsing) ditampilkan sebagai pesan error yang
	 * terlihat pada progress bar, bukan gagal diam-diam.
	 *
	 * <p><b>Mengapa {@code static}:</b> method ini adalah <i>titik masuk bersama</i> — selain dipakai
	 * {@link DetailPerkuliahanRenderer} untuk setiap baris grid, ia juga dipanggil dari layar lain yang
	 * menampilkan satu {@link Detailperkuliahan} tanpa memiliki instance
	 * {@code DetailperkuliahanHelper}. Karena itu seluruh state yang dibutuhkan diterima lewat parameter,
	 * termasuk {@code dataLoader} sebagai callback penyegar sehingga pemanggil bebas menentukan apa yang
	 * di-refresh setelah proses selesai.
	 *
	 * <p><b>Tiga syarat visibilitas tombol</b> (semuanya wajib terpenuhi; bila salah satu gagal, method
	 * tidak menambahkan apa pun ke {@code vbox} dan selesai diam-diam):
	 * <ol>
	 *   <li>{@code tbmuser != null} — ada pengguna yang login.</li>
	 *   <li>{@link Common#getApakahAdminBolehAksesFeeder()} dan konfigurasi
	 *       {@code aktifkan_terhubung_langsung_ke_feeder} aktif.</li>
	 *   <li>Mahasiswa sudah memiliki {@code idRegPd} tidak kosong — tanpa identitas registrasi Dikti,
	 *       data tidak dapat dipetakan di sisi Feeder.</li>
	 * </ol>
	 *
	 * <p><b>Dua jalur pengiriman</b> ditentukan isi baris: bila {@code detailperkuliahan.getPerkuliahan()}
	 * ada, dikirim sebagai data perkuliahan biasa lewat {@code PerkuliahanAction.kirimKeFeeder}; bila
	 * tidak ada tetapi {@code getMatakuliahKonversi()} terisi, dikirim sebagai nilai transfer/konversi
	 * lewat {@code feederImporter.nilaiTransfer}. Bila keduanya kosong, tidak ada yang dikirim.
	 *
	 * <p><b>Threading dan sesi:</b> pengiriman berjalan di {@link Thread} latar agar UI tidak membeku;
	 * kemajuannya dilaporkan dengan menulis ke {@link Label} progress yang dibuat
	 * {@code Common.displayLoadBar}. Method ini <b>tidak</b> membuka sesi Hibernate sendiri — ia
	 * menavigasi relasi pada {@code detailperkuliahan} yang diterima dari pemanggil, sehingga entity
	 * tersebut harus masih terhubung ke sesi yang hidup ketika thread berjalan. Galat yang terjadi di
	 * dalam thread ditangkap dan ditulis sebagai teks "Error: ..." pada label progress; ini perbaikan
	 * atas perilaku lama yang mengosongkan label (terbaca sebagai sukses palsu) meski pengiriman gagal.
	 *
	 * <p><b>Berkas log:</b> bila {@code errorLog} terisi, seluruh pesan digabung, ditampilkan sebagai
	 * kotak pesan, lalu ditulis ke berkas bernama acak di {@code /opt/ecampus/} dan langsung diunduhkan
	 * lewat {@link Filedownload}. Berkas tersebut tidak pernah dibersihkan otomatis.
	 *
	 * @param tbmuser           user yang sedang login
	 * @param detailperkuliahan baris KRS yang akan dikirim ke feeder
	 * @param dataLoader        callback penyegar tampilan setelah proses selesai
	 * @param vbox              komponen tujuan penambahan tombol
	 * @param verical           bila {@code true}, tombol dirender dengan orientasi vertikal
	 */
	public static void kirimKeFeeder(Tbmuser tbmuser, final Detailperkuliahan detailperkuliahan,
			final DataLoader dataLoader, Component vbox, boolean verical) {
		Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();

		if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
				&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")
				&& (mahasiswa != null && mahasiswa.getIdRegPd() != null && !mahasiswa.getIdRegPd().isEmpty())) {

			MyToolbarbuttonConfig buttonTagihan = new MyToolbarbuttonConfig("Kirim ke feeder",
					"/img/Finance-Invoice-icon.png");
			buttonTagihan.setStyle("font-size:8px;");
			if (verical) {
				buttonTagihan.setOrient("vertical");
			}
			buttonTagihan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin mengambil data dari feeder ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										String[] kon = EksporFromFeederAction.koneksi();
										final String ip = kon[0];
										final String port = kon[1];
										final String username = kon[2];
										final String password = kon[3];
										final String url = kon[4];
										if (!EksporFromFeederAction.exists(url)) {

											MyMessageboxConfig.show(
													ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalKoneksi(ip, port, Common.bolehKonfigurasi("aktifkan_https_ke_feeder", ais.database.model.Konfigurasi.TIDAK_AKTIF), "Pemeriksaan ketersediaan alamat " + url + " gagal (server Neo Feeder tidak merespons)."),
													"Peringatan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);
											return;
										}

										final List<String> errorLog = new ArrayList<String>();

										final Label myLabelProsesDetail = Common.displayLoadBar(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												if (arg0 != null && !arg0.getName().isEmpty()) {
													EksporFromFeederAction.display();
													MyMessageboxConfig.show(arg0.getName(), "Info",
															MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
												}

												if (!errorLog.isEmpty()) {
													String err = "";
													for (String s : errorLog) {
														err += err.isEmpty() ? s
																: "\n----------------------------------------------------------------------------------------------------------\n"
																		+ s;
													}

													MyMessageboxConfig.show(err, "Error Terjadi", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);

													File file = new File(
															"/opt/ecampus/error_" + Common.randLong() + ".txt");

													if (!file.getParentFile().exists()) {
														file.getParentFile().mkdirs();
													}
													FileUtils.writeStringToFile(file, err);
													Filedownload.save(file, "text/plain");

												}

												dataLoader.loadData(true);
											}
										});

										new Thread(new Runnable() {

											@Override
											public void run() {
												try {
													FeederConnector feederConnector = new FeederConnector(ip,
															Integer.parseInt(port), null);

													String token = feederConnector.getToken(username, password);
													System.out.println("TOKEN => " + token);

													if (token == null || token.trim().isEmpty()
															|| token.trim().toLowerCase().startsWith("error")) {
														myLabelProsesDetail
																.setValue("Error: " + ais.action.master.feeder.util.NeoFeederPesanFormalHelper.pesanGagalLogin(username, null));
														return;
													}

													FeederExporter feederImporter = new FeederExporter(feederConnector,
															token, null, null, null);

													myLabelProsesDetail.setValue("Mengirim data " + detailperkuliahan);

													Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
													if (perkuliahan != null) {
														Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
														PerkuliahanAction.kirimKeFeeder(feederImporter, perkuliahan,
																feederConnector, token, mahasiswa, errorLog);
													} else if (detailperkuliahan.getMatakuliahKonversi() != null) {

														feederImporter.nilaiTransfer(detailperkuliahan, errorLog);
													}

													myLabelProsesDetail.setValue("");
												} catch (Exception e) {
													// FIX "gagal diam-diam": sebelumnya exception di sini (mis. gagal
													// konek/parse port/JSON) hanya dicatat ke log admin lalu progres
													// diset "" (=SUKSES palsu) di luar try, menutupi kegagalan dari
													// pengguna. Sekarang progres diisi pesan error yang terlihat.
													ais.common.Common.tampilErrorJikaAdmin(e);
													myLabelProsesDetail.setValue("Error: "
															+ ais.common.PesanFormalHelper.pesanGagalException(
																	"pengiriman data Detail Perkuliahan \""
																			+ detailperkuliahan + "\" ke Neo Feeder",
																	null, e,
																	new String[] {
																			"Periksa kembali koneksi ke server Neo Feeder (Pengaturan Koneksi) dan coba ulangi.",
																			"Pastikan Username/Password Feeder pada Pengaturan Koneksi masih benar.",
																			"Jika kendala berulang, hubungi Administrator Sistem atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini." })
																	.replace("\n", " "));
												}
											}
										}).start();

									}

								}
							});

				}
			});
			buttonTagihan.setParent(vbox);
		}
	}

	/**
	 * Perender baris grid: menampilkan foto, riwayat revisi, indikator validitas data Feeder
	 * (ikon check/warning bila fitur Feeder aktif), tombol "Kirim ke feeder" ({@link #kirimKeFeeder}),
	 * nama, angkatan, status kemahasiswaan, total nilai, status persetujuan, textbox keterangan
	 * nilai tambahan (auto-save on change), intbox semester dan tahap (auto-save on change), serta
	 * tombol Pindah Data (buka {@code TransferDataMahasiswaHelper} untuk satu mahasiswa), Ubah
	 * Persetujuan (toggle disetujui/belum, dengan pengecekan opsional "nilai harus nol"), dan Hapus
	 * (dengan pengecekan tidak bisa hapus bila sudah disetujui, masih dipakai pengajuan tugas akhir,
	 * atau nilai tidak nol).
	 *
	 * <p>Kelas ini sengaja berupa <b>inner class non-statis</b>, bukan statis: ia perlu membaca flag
	 * {@link DetailperkuliahanHelper#edit} dan {@link DetailperkuliahanHelper#delete} milik instance
	 * pembungkusnya, menyentuh {@link DetailperkuliahanHelper#perkuliahan} sebagai sumber nilai default
	 * semester/tahap, dan memanggil balik
	 * {@code DetailperkuliahanHelper.this.loadData(true)} setelah setiap aksi per-baris.
	 *
	 * <p><b>Kolom nilai bersifat baca-saja di sini.</b> {@code totalNilai} dan {@code nilaiHuruf} hanya
	 * dirender sebagai label lewat {@code NilaiHurufAnalisisPopupHelper}, dan {@code totalNilai} dipakai
	 * sebagai <i>penjaga</i> pada tombol Ubah Persetujuan dan Hapus. Penulisan nilai adalah urusan
	 * {@link DetailperkuliahanForPenilaianHelper}; lihat tabel pembagian tanggung jawab pada Javadoc
	 * kelas pembungkus.
	 *
	 * <p><b>Model penulisan:</b> ketiga kendali auto-save (keterangan, semester, tahap) dan toggle
	 * persetujuan menulis lewat {@code HibernateUtil.currentSession()} + {@code Common.refreshUpdate}
	 * pada thread event ZK, tanpa transaksi eksplisit dan tanpa penguncian baris. Perubahan tersimpan
	 * seketika saat fokus berpindah — tidak ada tombol Simpan dan tidak ada konfirmasi.
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris peserta ke dalam sebelas kolom grid, lengkap dengan kendali edit dan tombol
		 * aksi per-baris.
		 *
		 * <p><b>Penjagaan data rusak di awal:</b> parameter {@code data} adalah <i>id</i>
		 * {@link Detailperkuliahan} (bukan entity — lihat {@link DetailperkuliahanHelper#detailperkuliahan}),
		 * yang di-resolve lewat {@code GeneralValueObject.ambilData}. Bila baris sudah terhapus atau
		 * relasi ke {@link Mahasiswa} sudah putus, method menuliskan satu label penjelas lalu
		 * {@code return} — grid tetap tergambar utuh alih-alih melempar NPE dan mematikan seluruh
		 * tampilan.
		 *
		 * <p><b>Urutan kolom yang dihasilkan</b> (harus tetap sinkron dengan definisi
		 * {@link MyColumnConfig} di {@link DetailperkuliahanHelper#display}):
		 * <ol>
		 *   <li><b>Foto</b> — {@code CommonMedia.tampilkanGambarKecil(mahasiswa)}.</li>
		 *   <li><b>NIM</b> — dirender sebagai tautan riwayat revisi
		 *       ({@link RevisiHelper#createNewRevisi}) sehingga NIM sekaligus menjadi pintu masuk ke
		 *       jejak audit Envers baris ini; di bawahnya indikator "Feeder valid"/"Feeder blm valid"
		 *       (hanya bila fitur Feeder aktif dan pengguna berhak) serta tombol
		 *       {@link DetailperkuliahanHelper#kirimKeFeeder}.</li>
		 *   <li><b>Nama</b> mahasiswa.</li>
		 *   <li><b>Angkatan</b> — {@code tahunangkatan / semesterMulai}.</li>
		 *   <li><b>Status</b> — status awal mahasiswa digabung status berjalan; status berjalan diperoleh
		 *       dengan menyinkronkan {@link KrsMahasiswa} untuk semester/tahap baris ini lebih dulu
		 *       ({@code Common.singkronkanKrsMahasiswa}), lalu membaca
		 *       {@code HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa}. Perhatikan bahwa langkah
		 *       sinkronisasi ini <b>bukan operasi baca murni</b>: ia dapat membuat atau memperbarui baris
		 *       KRS sebagai efek samping dari sekadar menampilkan daftar.</li>
		 *   <li><b>Total Nilai</b> — label {@code totalNilai (nilaiHuruf)}, atau
		 *       {@code "0.0 (Belum dinilai)"}; baca-saja.</li>
		 *   <li><b>Persetujuan</b> — label "Ya"/"Tidak", diwarnai biru/merah.</li>
		 *   <li><b>Keterangan</b> — {@link Textbox} {@code detailNilaiTambahan}, auto-save
		 *       {@code onChange}.</li>
		 *   <li><b>Smt</b> — {@link Intbox} {@code semester}, auto-save; dikosongkan berarti memakai
		 *       semester perkuliahan, dan nilai efektifnya ditulis balik ke kotak setelah simpan.</li>
		 *   <li><b>Tahap</b> — {@link Intbox} {@code tahap}, auto-save; dikosongkan berarti memakai tahap
		 *       dari {@code kurikulumPunyaMatakuliah}, atau {@code 0} bila kurikulum tidak diketahui.</li>
		 *   <li><b>Tombol aksi</b> — Pindah Data, Ubah Persetujuan, Hapus Data.</li>
		 * </ol>
		 *
		 * <p><b>Penjaga pada tombol aksi:</b>
		 * <ul>
		 *   <li><i>Pindah Data</i> — terlihat bila {@code edit} bernilai benar, baris punya perkuliahan,
		 *       <b>dan</b> akun bukan dosen ({@code getDosen() == null}).</li>
		 *   <li><i>Ubah Persetujuan</i> — hanya {@code edit}. Sebelum toggle, bila konfigurasi
		 *       {@code batalkan_persetujuan_harus_memiliki_nilai_nol} aktif dan baris sudah
		 *       {@code DISETUJUI} dengan {@code totalNilai > 1.0}, aksi ditolak. Ambang
		 *       {@code > 1.0} — bukan {@code > 0.0} — berarti nilai kecil tetap dianggap "nol".</li>
		 *   <li><i>Hapus Data</i> — hanya {@code delete}. Tiga penjaga berurutan: baris berstatus
		 *       {@code DISETUJUI} ditolak; baris yang masih dirujuk {@link MahasiswaRequestTugasAkhir}
		 *       ditolak (dihitung lewat {@code Projections.rowCount()}); dan penjaga nilai yang sama
		 *       dengan Ubah Persetujuan. Kegagalan basis data (mis. pelanggaran kunci asing) ditangkap
		 *       dan disampaikan lewat {@code PesanFormalHelper}, bukan gagal diam-diam.</li>
		 * </ul>
		 *
		 * <p><b>Sifat:</b> berjalan di thread event ZK. Setiap aksi yang berhasil menjadwalkan
		 * {@code loadData(true)} lewat {@code Common.createDefaultTimer} sehingga grid dibangun ulang
		 * dari basis data, bukan ditambal di tempat.
		 *
		 * @param row  baris grid tujuan; komponen anak ditambahkan berurutan sesuai daftar kolom di atas
		 * @param data id {@link Detailperkuliahan} sebagai objek; {@code toString()}-nya dipakai untuk
		 *             resolve entity
		 * @throws Exception diteruskan dari pembangunan komponen ZK; kegagalan resolve entity tidak
		 *                   melempar melainkan menghasilkan baris berlabel penjelas
		 */
		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());
			if (detailperkuliahan == null || detailperkuliahan.getMahasiswa() == null) {
				new Label("Data KRS/perkuliahan tidak ditemukan atau mahasiswa sudah tidak terhubung.").setParent(row);
				return;
			}

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);
			Vbox vbox = new Vbox();
			vbox.setParent(row);
			RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim()).setParent(vbox);
//			new MyLabelKecil((detailperkuliahan.getFeeder() == null ? "" : "Feeder:" + detailperkuliahan.getFeeder()))
//					.setParent(vbox);
//			new MyLabelKecil((detailperkuliahan.getId_kls() == null ? ""
//					: "Feeder:" + detailperkuliahan.getId_kls() + ";" + detailperkuliahan.getId_reg_pd()))
//							.setParent(vbox);

			Hbox myHbox = new Hbox();
			myHbox.setParent(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && Common.getApakahAdminBolehAksesFeeder()
					&& Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder")) {
				if (detailperkuliahan.getFeeder() != null && !detailperkuliahan.getFeeder().trim().isEmpty()) {
					myHbox.appendChild(new Image("/img/svg/check2-circle.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder valid"));
				} else {
					myHbox.appendChild(new Image("/img/svg/warning-outline.svg"));
					myHbox.appendChild(new MyLabelKecilSekali("Feeder blm valid"));
				}
			}
			DetailperkuliahanHelper.kirimKeFeeder(tbmuser, detailperkuliahan, DetailperkuliahanHelper.this, myHbox,
					false);

			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
			new Label(detailperkuliahan.getMahasiswa().getTahunangkatan() + " / "
					+ detailperkuliahan.getMahasiswa().getSemesterMulai()).setParent(row);

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(detailperkuliahan.getMahasiswa(),
					detailperkuliahan.getSemester(), detailperkuliahan.getTahap(),
					detailperkuliahan.getPerkuliahan() == null ? null
							: detailperkuliahan.getPerkuliahan().getStatusSemesterPendek());

			ais.database.model.HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krsMahasiswa);
			StatusMahasiswa statusMahasiswa = historyStatusMahasiswa == null ? null : historyStatusMahasiswa.getStatusMahasiswa();
			new Label((detailperkuliahan.getMahasiswa().getStatusAwalMahasiswa() == null ? ""
					: detailperkuliahan.getMahasiswa().getStatusAwalMahasiswa().getNama()) + " / "
					+ (statusMahasiswa == null ? "" : statusMahasiswa.getNama())).setParent(row);

			ais.ui.util.NilaiHurufAnalisisPopupHelper.buatLabel(detailperkuliahan.getTotalNilai() == null ? "0.0 (Belum dinilai)"
					: Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ (detailperkuliahan.getNilaiHuruf() == null
									|| detailperkuliahan.getNilaiHuruf().trim().equals("") ? "Belum dinilai"
											: detailperkuliahan.getNilaiHuruf())
							+ ")", detailperkuliahan)
					.setParent(row);

			final Label label;
			(label = new Label(detailperkuliahan.getPersetujuan() == null
					|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI) ? "Tidak" : "Ya"))
					.setParent(row);
			label.setStyle(label.getValue().equals("Tidak") ? "color:red;" : "color:blue");

			final Textbox keterangan = new Textbox(detailperkuliahan.getDetailNilaiTambahan());
			keterangan.setRows(3);
			keterangan.setWidth("90%");
			keterangan.setParent(row);
//			keterangan.setDisabled(tbmuser == null || tbmuser.getRoot() == null || !tbmuser.getRoot());
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan.setDetailNilaiTambahan(keterangan.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
				}
			});

			final Intbox semester = new Intbox(detailperkuliahan.getSemester());
			semester.setParent(row);
			semester.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan
							.setSemester(semester.getValue() == null ? perkuliahan.getSemester() : semester.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
					semester.setValue(detailperkuliahan.getSemester());
				}
			});

			final Intbox tahap = new Intbox(detailperkuliahan == null || detailperkuliahan.getTahap() == null ? 0
					: detailperkuliahan.getTahap());
			tahap.setParent(row);
//			tahap.setDisabled(tbmuser == null || tbmuser.getRoot() == null || !tbmuser.getRoot());
			tahap.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					detailperkuliahan.setTahap(
							tahap.getValue() == null
									? (perkuliahan.getKurikulumPunyaMatakuliah() == null ? 0
											: perkuliahan.getKurikulumPunyaMatakuliah().getTahap())
									: tahap.getValue());
					Common.refreshUpdate(session, (detailperkuliahan));
					tahap.setValue(detailperkuliahan.getTahap());
				}
			});

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/stock_data_edit_table.png");
			button.setTooltiptext("Pindah Data");
			button.setVisible(
					edit && detailperkuliahan.getPerkuliahan() != null && Common.getCurrentUser().getDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show(
							"Apakah yakin ingin memindahkan krs mahasiswa " + detailperkuliahan.getMahasiswa().getNama()
									+ " matakuliah " + detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
									+ " ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											TransferDataMahasiswaHelper transferDataMahasiswaHelper = new TransferDataMahasiswaHelper(
													detailperkuliahan.getPerkuliahan(),
													detailperkuliahan.getMahasiswa());
											MyWindow window = new MyWindow();
											window.setParent(
													ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
											transferDataMahasiswaHelper.display(new DataLoader() {

												@Override
												public void loadData(Object value) {
													DetailperkuliahanHelper.this.loadData(true);
												}
											}, window);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);

										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			button.setVisible(edit);
			button.setTooltiptext("Ubah Persetujuan");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
						if (detailperkuliahan.getPersetujuan() != null
								&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)
								&& detailperkuliahan.getTotalNilai() > 1.0) {
							MyMessageboxConfig.show("Jika nilai tidak nol, anda tidak bisa mengubah persetujuan",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							return;
						}
					}

					String pertanyaan = "Apakah anda ingin mengubah mahasiswa dengan NIM "
							+ detailperkuliahan.getMahasiswa().getNim() + " dan nama "
							+ detailperkuliahan.getMahasiswa().getNama() + " yang mengikuti perkulihaan "
							+ detailperkuliahan.getPerkuliahan().getMatakuliah().getNama()
							+ (detailperkuliahan.getPersetujuan() == null
									|| detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)
											? " dari tidak disetujui menjadi disetujui ?"
											: " dari disetujui menjadi tidak disetujui ?");

					MyMessageboxConfig.show(pertanyaan, "Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										Session session = HibernateUtil.currentSession();
										detailperkuliahan.setPersetujuan(
												detailperkuliahan.getPersetujuan() == null || detailperkuliahan
														.getPersetujuan().equals(Detailperkuliahan.BELUM_DISETUJUI)
																? Detailperkuliahan.DISETUJUI
																: Detailperkuliahan.BELUM_DISETUJUI);

										Common.refreshUpdate(session, detailperkuliahan);

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);

											}
										});
									}

								}
							});
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (detailperkuliahan.getPersetujuan() != null
							&& detailperkuliahan.getPersetujuan().equals(Detailperkuliahan.DISETUJUI)) {
						MyMessageboxConfig.show("Mahasiswa yang sudah disetujui tidak bisa dihapus", "Peringatan",
								MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Number jumlahRequestTugasAkhir = (Number) HibernateUtil.currentSession()
													.createCriteria(MahasiswaRequestTugasAkhir.class)
													.setProjection(org.hibernate.criterion.Projections.rowCount())
													.add(Restrictions.eq("detailperkuliahan", detailperkuliahan))
													.uniqueResult();
											if (jumlahRequestTugasAkhir != null
													&& jumlahRequestTugasAkhir.longValue() > 0L) {
												MyMessageboxConfig.show(
														"Data perkuliahan tidak dapat dihapus karena masih digunakan pada pengajuan tugas akhir mahasiswa. Batalkan atau pindahkan pengajuan tugas akhir tersebut terlebih dahulu.",
														"Peringatan", MyMessageboxConfig.OK,
														MyMessageboxConfig.EXCLAMATION);
												return;
											}

											if (Common.bolehKonfigurasi("batalkan_persetujuan_harus_memiliki_nilai_nol")) {
												if (detailperkuliahan.getPersetujuan() != null
														&& detailperkuliahan.getPersetujuan()
																.equals(Detailperkuliahan.DISETUJUI)
														&& detailperkuliahan.getTotalNilai() > 1.0) {
													MyMessageboxConfig.show(
															"Jika nilai tidak nol, anda tidak bisa menghapus mahasiswa ini",
															"Peringatan", MyMessageboxConfig.OK,
															MyMessageboxConfig.EXCLAMATION);
													return;
												}
											}

											Common.refreshDelete(detailperkuliahan);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													DetailperkuliahanHelper.this.loadData(true);

												}
											});

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			button.setParent(toolbar);

			toolbar.setParent(row);

		}

	}

	/**
	 * <b>Tujuan:</b> Membangun kriteria {@link Detailperkuliahan} untuk peserta {@link #perkuliahan}
	 * yang sedang dibuka. Ini implementasi kontrak {@link DataCriteria}, dan di kelas ini
	 * <b>tidak</b> dipakai untuk mengisi grid — grid diisi {@link #loadData(Object)} lewat jalur lain.
	 * Satu-satunya konsumennya adalah fitur cetak/ekspor data ({@link Common#cetakData}), yang memanggil
	 * method ini untuk mendapatkan kumpulan baris yang akan dituangkan ke berkas.
	 *
	 * <p><b>Tiga klausa yang dipasang:</b>
	 * <ol>
	 *   <li>{@code isNull("ikutiPerkuliahan")} — hanya baris perkuliahan <i>asli</i>. Baris yang
	 *       "mengikuti" perkuliahan lain (kelas gabung/paralel yang menumpang pada kelas induk)
	 *       dikecualikan agar peserta tidak terhitung ganda.</li>
	 *   <li>Pencarian opsional: bila {@link #nama} kosong atau {@code null}, dipasang
	 *       {@code sqlRestriction("true")} sebagai penahan tempat yang tidak menyaring apa pun — pola ini
	 *       menjaga rantai pemanggilan {@code add(...)} tetap seragam tanpa percabangan. Bila terisi,
	 *       dipasang {@code or(ilike(nim), ilike(nama))} dengan {@link MatchMode#ANYWHERE} lewat alias
	 *       {@code mahasiswa}.</li>
	 *   <li>{@code eq("perkuliahan", perkuliahan)} — pembatas konteks utama.</li>
	 * </ol>
	 *
	 * <p><b>Perbedaan dengan jalur grid:</b> {@link #loadData(Object)} tidak memakai kriteria ini
	 * melainkan {@code perkuliahan.ambilDetailperkuliahan(null, null, teksPencarian)}. Kedua jalur
	 * membaca kotak pencarian yang sama tetapi menerapkannya lewat mekanisme yang berbeda, sehingga
	 * himpunan baris hasil cetak tidak dijamin identik dengan yang tampil di layar. Bila perilaku
	 * keduanya perlu diselaraskan, penyelarasan harus dilakukan di kedua tempat.
	 *
	 * <p><b>Sifat:</b> membaca {@code currentSession()} sehingga wajib dipanggil dari thread event ZK.
	 * Tidak mengeksekusi query — hanya menyusun objek {@link Criteria}. Mengasumsikan
	 * {@link #perkuliahan} sudah terisi.
	 *
	 * @param order bila {@code true}, tambahkan pengurutan menaik berdasarkan NIM mahasiswa
	 * @return kriteria Hibernate siap dieksekusi
	 * @see #loadData(Object)
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Detailperkuliahan.class);

		criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(Restrictions.eq("perkuliahan", perkuliahan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	/**
	 * <b>Tujuan:</b> Memuat ulang grid dengan seluruh {@link Detailperkuliahan} milik
	 * {@link #perkuliahan} saat ini, sesuai teks pencarian pada kotak {@link #nama}. Implementasi
	 * kontrak {@link DataLoader}, dan satu-satunya jalur pengisian grid di kelas ini.
	 *
	 * <p><b>Dua mode pemuatan — perbedaannya penting:</b>
	 * <ul>
	 *   <li>{@code loadData(true)} — memanggil {@code perkuliahan.reInitDetailperkuliahan(session)}
	 *       lebih dulu, yaitu membangun ulang cache daftar peserta pada entity {@link Perkuliahan} dari
	 *       basis data. Dipakai setiap kali data <i>berubah</i>: setelah simpan, hapus, transfer,
	 *       persetujuan massal, sinkronisasi, unggah Excel, dan tombol Refresh.</li>
	 *   <li>{@code loadData(null)} — memakai cache yang sudah ada. Dipakai untuk perubahan yang hanya
	 *       menyaring tampilan, yaitu menekan Enter pada kotak pencarian. Bila dipanggil setelah data
	 *       berubah di sesi lain, grid akan menampilkan keadaan lama.</li>
	 * </ul>
	 * Perbandingan dilakukan dengan {@code value.equals(true)}, sehingga nilai apa pun selain
	 * {@link Boolean} {@code true} diperlakukan sebagai mode cache.
	 *
	 * <p><b>Langkah:</b> hasil {@code perkuliahan.ambilDetailperkuliahan(null, null, teksPencarian)}
	 * disalin ke {@link ArrayList} baru dan disimpan di {@link #detailperkuliahan}. Penyalinan itu bukan
	 * sekadar konversi tipe: daftar ini juga menjadi cakupan kerja ketiga tombol aksi massal, sehingga
	 * ia perlu menjadi snapshot mandiri, bukan pandangan langsung ke cache entity. Selanjutnya renderer
	 * {@link DetailPerkuliahanRenderer} yang baru dipasang dan model grid diperbarui.
	 *
	 * <p><b>Renderer dibuat ulang setiap pemuatan</b> — bukan sekali di {@link #display} — karena
	 * renderer adalah inner class yang menangkap keadaan terkini instance pembungkusnya.
	 *
	 * <p><b>Sifat:</b> berjalan di thread event ZK dan memakai {@code currentSession()}. Mengasumsikan
	 * {@link #perkuliahan}, {@link #nama}, dan {@link #grid} sudah terisi oleh {@link #display};
	 * memanggilnya lebih awal akan melempar {@link NullPointerException}. Signature-nya tidak
	 * mendeklarasikan {@code throws}, sehingga kegagalan basis data merambat sebagai runtime exception.
	 *
	 * @param value bila {@code true} (sebagai {@link Boolean}), paksa cache {@code Detailperkuliahan}
	 *              milik {@link #perkuliahan} dibangun ulang dari database via
	 *              {@code reInitDetailperkuliahan} sebelum diambil
	 * @see #initCriteria(boolean)
	 */
	public void loadData(Object value) {
		if (value != null && value.equals(true)) {
			perkuliahan.reInitDetailperkuliahan(HibernateUtil.currentSession());
		}
		detailperkuliahan = new ArrayList<Long>(perkuliahan.ambilDetailperkuliahan(null, null, nama.getValue().trim()));
		ListModel strset = new SimpleListModel(detailperkuliahan);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengganti konteks {@link Perkuliahan} helper <b>tanpa</b> membangun ulang tampilan.
	 *
	 * <p>Berbeda dari {@link #display}, method ini hanya menyetel field dan tidak menyentuh apa pun yang
	 * lain: toolbar, kolom, dan isi grid tetap seperti sebelumnya. Pemanggil yang berpindah kelas
	 * karenanya <b>wajib</b> memanggil {@code loadData(true)} sesudahnya; tanpa itu layar masih
	 * menampilkan peserta perkuliahan lama sementara seluruh aksi (simpan, hapus, persetujuan, unggah)
	 * sudah mengacu ke perkuliahan baru — kombinasi yang berbahaya karena baris yang terlihat tidak lagi
	 * mewakili baris yang akan terpengaruh.
	 *
	 * <p><b>Parameter kedua tidak dipakai.</b> {@code perkuliahanAsli} diterima semata agar signature
	 * sejajar dengan {@link #display}, yang memang membutuhkannya untuk cetak laporan absensi. Badan
	 * method mengabaikannya sepenuhnya. Jangan berasumsi memanggil method ini akan menetapkan konteks
	 * "perkuliahan asli" di mana pun.
	 *
	 * <p><b>Sifat:</b> murni penyetel field; tanpa akses basis data, tanpa komponen ZK, aman dipanggil
	 * kapan saja.
	 *
	 * @param perkuliahan      perkuliahan konteks baru
	 * @param perkuliahanAsli  tidak dipakai dalam badan method; diterima untuk kompatibilitas signature
	 * @see #display(Perkuliahan, Perkuliahan, Component, MyWindow)
	 */
	public void setPerkuliahan(Perkuliahan perkuliahan, Perkuliahan perkuliahanAsli) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * Membangun dan menampilkan seluruh UI "Daftar Mahasiswa" untuk {@code perkuliahan} ke dalam
	 * {@code component}: toolbar pencarian dan aksi (lihat javadoc kelas untuk daftar lengkap
	 * tombol dan hak yang mengaturnya) serta grid berisi seluruh peserta perkuliahan (page size
	 * besar — 10000 — sehingga efektif menampilkan semua baris sekaligus).
	 *
	 * <p>Ini adalah titik masuk utama kelas: ia menetapkan {@link #perkuliahan}, membersihkan
	 * {@code component}, membangun toolbar dan kolom, lalu menutup dengan {@code loadData(null)} untuk
	 * pengisian pertama. Seluruh field UI ({@link #nama}, {@link #grid}) baru ada setelah method ini
	 * selesai.
	 *
	 * <p><b>Toolbar, dikelompokkan menurut kendali aksesnya:</b>
	 * <ul>
	 *   <li><i>Tanpa kendali</i> — kotak pencarian "Mhs :", Refresh, cetak Absensi/UTS/UAS, tombol
	 *       cetak/ekspor data ({@link Common#cetakData}), Upload Data (.xlsx), serta unduh/unggah data
	 *       mahasiswa dari {@code MahasiswaAction.createUploadDanDownloadData}. Perhatikan bahwa Upload
	 *       Data <b>membuat baris {@link Detailperkuliahan} baru</b> tetapi tidak dikendalikan flag
	 *       {@link #create} — lihat {@link #uploadDataMahasiswa}. Seluruh toolbar disembunyikan bila
	 *       tidak ada pengguna login ({@code toolbar.setVisible(tbmuser != null)}).</li>
	 *   <li><i>Konfigurasi</i> — "Singkronkan" hanya terlihat bila
	 *       {@code aktifkan_tombol_sinkronkan_semua} aktif; ia menjalankan
	 *       {@code perkuliahan.singkronkan(session)} di {@link Thread} latar dan memantau
	 *       penyelesaiannya dengan {@link Timer} 500&nbsp;ms yang menunggu label progres menjadi kosong.
	 *       Sesi {@code currentNativeSession()} ditutup di {@code finally} agar tidak bocor bila
	 *       sinkronisasi gagal.</li>
	 *   <li><i>Flag konstruktor saja</i> — "Ambil Mhs" ({@link #create}), "Transfer" dan "Copy mhs"
	 *       ({@link #edit}), "History" ({@link #edit}).</li>
	 *   <li><i>Flag konstruktor <b>dan</b> penyaringan peran</i> — "Setujui", "Tolak", "Hapus". Flag
	 *       mengatur {@code setDisabled}, sedangkan {@code setVisible} ditentukan
	 *       {@link Common#getApakahAdmin()} atau kecocokan {@code roleId} pengguna dengan
	 *       {@code ConstantValues.Akademik}, {@code roleAdminFakultas}, atau {@code roleAdminJurusan}.
	 *       Ketiga pemeriksaan itu dibungkus {@code try-catch} yang mencatat ke {@code ErrorAuditUtil}:
	 *       bila {@code hakAkses()} gagal dimuat, tombol mempertahankan visibilitas bawaannya
	 *       (<i>terlihat</i>) alih-alih disembunyikan — perilaku fail-open yang perlu diingat.</li>
	 * </ul>
	 *
	 * <p><b>Cakupan aksi massal.</b> Ketiga tombol massal beriterasi atas {@link #detailperkuliahan},
	 * yaitu daftar yang sedang tampil — bukan hasil query ulang. Bila kotak pencarian sedang terisi,
	 * aksi hanya mengenai baris yang lolos pencarian, meski teks konfirmasinya berbunyi "semua mahasiswa
	 * di dalam perkuliahan ini". "Setujui" dan "Tolak" menulis lewat {@code Common.refreshUpdate} tanpa
	 * penjaga tambahan; "Hapus" hanya menyentuh baris {@code BELUM_DISETUJUI} dan tidak memeriksa relasi
	 * {@link MahasiswaRequestTugasAkhir} sebagaimana dilakukan tombol hapus per-baris. Ketiganya ditutup
	 * dengan {@code perkuliahan.belum("detailperkulaiahan")} untuk membatalkan cache
	 * (perhatikan ejaan kunci cache tersebut, yang memang demikian di seluruh basis kode) lalu
	 * {@code loadData(true)}.
	 *
	 * <p><b>Sebelas kolom</b> didefinisikan dengan lebar yang bergantung konteks: enam kolom terakhir
	 * disetel {@code "0%"} bila tidak ada pengguna login; kolom "Smt" juga {@code "0%"} untuk pra-
	 * perkuliahan; dan kolom "Tahap" hanya berlebar bila {@code ConstantValues.aktifkanTahapanKurikulum}
	 * aktif. Urutan kolom di sini <b>wajib</b> tetap sejalan dengan urutan komponen yang ditambahkan
	 * {@link DetailPerkuliahanRenderer#render}.
	 *
	 * <p><b>Sifat:</b> berjalan di thread event ZK; membangun komponen dan memasang listener saja,
	 * tanpa penulisan basis data sendiri. Aman dipanggil ulang — {@code Common.clear(component)}
	 * membersihkan tampilan sebelumnya lebih dulu.
	 *
	 * @param perkuliahan      perkuliahan yang daftar mahasiswanya ditampilkan
	 * @param perkuliahanAsli  perkuliahan asli (sebelum kemungkinan substitusi/redirect), dipakai
	 *                         untuk cetak laporan absensi
	 * @param component        komponen ZK tujuan tampilan (dibersihkan lebih dulu)
	 * @param window           window pemanggil, diteruskan ke helper Ambil Mhs/Transfer/Copy mhs
	 */
	public void display(final Perkuliahan perkuliahan, final Perkuliahan perkuliahanAsli, final Component component,
			final MyWindow window) {
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		Tbmuser tbmuser = Common.getCurrentUser();

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.setVisible(tbmuser != null);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mhs : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Refresh", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				DetailperkuliahanHelper.this.loadData(true);
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig cetakSksDosen = new MyToolbarbuttonConfig("Singkronkan", "/img/svg/check2.svg");
		cetakSksDosen.setVisible(Common.bolehKonfigurasi("aktifkan_tombol_sinkronkan_semua"));
		toolbar.appendChild(cetakSksDosen);
		cetakSksDosen.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						final Label label = new Label(ais.common.Common.getBahasaConfig("Proses singkronisasi perkuliahan"));

						new Thread(new Runnable() {

							@Override
							public void run() {
								try {
								// currentNativeSession() ditutup sekali saja di finally (hindari close ganda).
								Session session = HibernateUtil.currentNativeSession();
								perkuliahan.singkronkan(session);
								label.setValue("");
							} finally {
								ais.database.hibernate.HibernateUtil.closeSession();
							}
							}
						}).start();

						final Timer timer = new Timer(500);
						timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
						timer.setRepeats(true);
						timer.addEventListener("onTimer", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								// System.out.println("process = " +
								// label.getValue());
								Clients.showBusy(label.getValue());
								if (label.getValue().isEmpty()) {

									DetailperkuliahanHelper.this.loadData(true);
									Clients.clearBusy();
									MyMessageboxConfig.show("Singkronisasi perkuliahan berhasil dilakukan",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									timer.detach();

								}

							}
						});
						timer.start();

					}
				});
			}
		});

		button = new MyToolbarbuttonConfig("Ambil Mhs", "/img/new.gif");
		button.setDisabled(!create);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaHelper dataMahasiswaHelper = new AmbilDataMahasiswaHelper(perkuliahan,
						semesterPendek);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {

						// currentNativeSession() WAJIB ditutup di finally agar tidak bocor bila singkronkan() gagal.
						Session session = HibernateUtil.currentNativeSession();
						try {
							perkuliahan.singkronkan(session);
						} finally {
							HibernateUtil.closeSession();
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								DetailperkuliahanHelper.this.loadData(true);
							}
						});
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Transfer", "/img/group.gif");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				TransferDataMahasiswaHelper dataMahasiswaHelper = new TransferDataMahasiswaHelper(perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						DetailperkuliahanHelper.this.loadData(true);
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Copy mhs", "/img/group.gif");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CopyDataMahasiswaHelper dataMahasiswaHelper = new CopyDataMahasiswaHelper(perkuliahan);
				dataMahasiswaHelper.display(new DataLoader() {

					@Override
					public void loadData(Object value) {
						DetailperkuliahanHelper.this.loadData(true);
					}
				}, window);
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Setujui", "/img/svg/edit-box-line.svg");
		button.setDisabled(!approve);

		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:727");
			// TODO: handle exception
		}
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show("Apakah anda ingin men-setujui semua mahasiswa di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);
												Common.refreshUpdate(detailperkuliahan);
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});

									}
								}
							});
				}
			}

		});
		button.setParent(toolbar);
		button = new MyToolbarbuttonConfig("Tolak", "/img/svg/warning-outline.svg");
		button.setDisabled(!reject);
		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:777");
			// TODO: handle exception
		}

		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show("Apakah anda ingin menolak semua mahasiswa di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												detailperkuliahan.setPersetujuan(Detailperkuliahan.BELUM_DISETUJUI);
												Common.refreshUpdate(detailperkuliahan);
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});

									}
								}
							});

				}
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		button.setDisabled(!delete);
		try {
			button.setVisible(Common.getApakahAdmin() || (tbmuser != null && tbmuser.hakAkses() != null
					&& ((ConstantValues.Akademik != null && ConstantValues.Akademik.getRoleId() != null
							&& ConstantValues.Akademik.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminFakultas != null
									&& ConstantValues.roleAdminFakultas.getRoleId() != null
									&& ConstantValues.roleAdminFakultas.getRoleId().equals(tbmuser.hakAkses().getRoleId()))
							|| (ConstantValues.roleAdminJurusan != null
									&& ConstantValues.roleAdminJurusan.getRoleId() != null
									&& ConstantValues.roleAdminJurusan.getRoleId().equals(tbmuser.hakAkses().getRoleId())))));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:830");
			// TODO: handle exception
		}
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (detailperkuliahan != null) {

					MyMessageboxConfig.show(
							"Apakah anda ingin menghapus semua mahasiswa yang belum disetujui di dalam perkuliahan ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {
								public void onEvent(Event event) throws Exception {

									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {

										for (Long detailperkuliahanid : detailperkuliahan) {
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
													.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
											if (detailperkuliahan != null) {
												if (detailperkuliahan.getPersetujuan()
														.equals(Detailperkuliahan.BELUM_DISETUJUI)) {
													// currentNativeSession() ditutup di finally + rollback bila commit gagal,
													// supaya sesi tak bocor & transaksi tak tertinggal aktif.
													Session session = HibernateUtil.currentNativeSession();
													try {
														session.refresh(detailperkuliahan);
														session.getTransaction().begin();
														session.delete(detailperkuliahan);
														session.getTransaction().commit();
													} catch (Exception e) {
														try {
															session.getTransaction().rollback();
														} catch (Exception er) {
															ais.common.ErrorAuditUtil.record(er,
																	"rollback-gagal src/ais/action/master/helper/DetailperkuliahanHelper.java:hapusBelumDisetujui");
														}
														ais.common.ErrorAuditUtil.record(e,
																"auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:862");
													} finally {
														HibernateUtil.closeSession();
													}
												}
											}
										}
										perkuliahan.belum("detailperkulaiahan");
										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {
												DetailperkuliahanHelper.this.loadData(true);
											}
										});
									}
								}
							});

				}
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahanAsli, false);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("UTS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UTS");

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("UAS", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, "UAS");
			}

		});
		button.setParent(toolbar);

		final String[] contents = new String[] { "mahasiswa", "semester", "tahap", "persetujuan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload Data" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		upload.setUpload(Common.ukuranFileUpload());
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				if (media.getName().toLowerCase().endsWith("xlsx")) {

					InputStream inputStream = media.getStreamData();
					// System.out.println("media = " + media);
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					// System.out.println("file = " + file.getAbsolutePath());
					file.getParentFile().mkdirs();
					FileOutputStream fileOutputStream = new FileOutputStream(file);
					int c;
					while ((c = inputStream.read()) != -1) {
						fileOutputStream.write(c);
					}
					fileOutputStream.close();
					inputStream.close();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							uploadDataMahasiswa(file, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(arg0);
									Clients.clearBusy();
								}
							}, contents);
						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		toolbar.appendChild(upload);

		MahasiswaAction.createUploadDanDownloadData(toolbar, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		}, new DataCriteria() {

			@Override
			public Object initCriteria(boolean order) {
				return perkuliahan.ambilMahasiswa();
			}
		}, false, false);

		button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.setDisabled(!edit);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiDetailPerkuliahanHelper revisiHelper = new RevisiDetailPerkuliahanHelper(perkuliahan,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								perkuliahan.belum("detailperkulaiahan");
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										DetailperkuliahanHelper.this.loadData(true);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10000);
		grid.setParent(groupbox);
		grid.setSclass("fgrid");
		// paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		// column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth(tbmuser == null ? "0%" : "10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Smt");
		column.setWidth(tbmuser == null ? "0%" : (perkuliahan.getMerupakanPraPerkuliahan() ? "0%" : "5%"));

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahap");
		column.setWidth(tbmuser == null ? "0%" : (ConstantValues.aktifkanTahapanKurikulum ? "5%" : "0%"));

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth(tbmuser == null ? "0%" : "12%");

		loadData(null);

	}

	/**
	 * Memproses berkas Excel (.xlsx) unggahan berisi kolom mahasiswa/semester/tahap/persetujuan:
	 * untuk setiap baris, mencari mahasiswa (via objek sel atau fallback NIM), memeriksa status
	 * pembayaran semester terkait (baris dilewati dengan catatan bila belum bayar), lalu
	 * membuat/menemukan baris {@link Detailperkuliahan} yang sesuai. Dijalankan di thread terpisah
	 * dengan sesi Hibernate dedikasi (bukan sesi thread-local, karena thread ini berjalan setelah
	 * request asal selesai) yang ditutup rapi di {@code finally}; kegagalan simpan per baris di-
	 * rollback agar tidak menggagalkan baris berikutnya. Hasil akhir dilaporkan per baris via
	 * {@link ais.common.LaporanUpload}, lalu {@code eventListener} dipanggil.
	 *
	 * <p><b>Format berkas yang diharapkan:</b> baris pertama adalah judul dan dilewati; pembacaan mulai
	 * dari indeks baris 1. Empat kolom, sesuai konstanta {@code contents} pada {@link #display} dan
	 * berkas unduhan yang dihasilkan {@link Common#cetakData}:
	 * <ol start="0">
	 *   <li>mahasiswa — dibaca dua tahap: {@code Common.getSheetContentAsObject(..., Mahasiswa.class)}
	 *       lebih dulu, lalu <b>fallback</b> ke pencarian NIM {@code ConstantValues.ambilByNim(...)}.
	 *       Fallback itu penting: tanpanya, satu sel yang tidak terbaca sebagai objek/ID membuat baris
	 *       gagal dicocokkan meski NIM-nya jelas benar.</li>
	 *   <li>semester — bila kosong, dipakai {@code perkuliahan.getSemester()}.</li>
	 *   <li>tahap — dipakai apa adanya, boleh {@code null}.</li>
	 *   <li>persetujuan — bila kosong, dipakai {@code Detailperkuliahan.BELUM_DISETUJUI}.</li>
	 * </ol>
	 *
	 * <p><b>Gerbang pembayaran.</b> Sebelum baris dibuat, {@code Common.checkStatusPembayaranMahasiswa}
	 * memeriksa status pembayaran mahasiswa pada semester bersangkutan (dengan penanda semester pendek
	 * dari {@link #semesterPendek}). Bila belum bayar, baris dicatat sebagai "dilewati" beserta
	 * alasannya dan proses lanjut ke baris berikutnya. Pemeriksaan ini dibungkus {@code try-catch}
	 * sendiri yang hanya mencatat ke {@code ErrorAuditUtil} — artinya bila pemeriksaan itu sendiri
	 * gagal, baris <b>tetap diproses</b> (fail-open).
	 *
	 * <p><b>Idempoten terhadap baris yang sudah ada.</b> Untuk tiap mahasiswa dicari
	 * {@link Detailperkuliahan} yang sudah ada pada perkuliahan ini ({@code setMaxResults(1)}, urut
	 * {@code id} menurun). Bila ketemu, baris tersebut <b>dibiarkan apa adanya</b> — nilai semester,
	 * tahap, dan persetujuan dari berkas <b>tidak</b> menimpanya, meskipun tetap dilaporkan sebagai
	 * "berhasil". Baris baru hanya dibuat bila belum ada. Karena itu berkas ini berfungsi sebagai alat
	 * <i>pendaftaran</i> peserta, bukan alat pembaruan massal.
	 *
	 * <p><b>Isolasi kegagalan per baris.</b> Setiap penyimpanan dibungkus transaksi tersendiri, dan
	 * kegagalan wajib memicu {@code rollback} sebelum dilempar ulang. Tanpa rollback, transaksi
	 * tertinggal aktif sehingga {@code begin()} pada baris berikutnya melempar "Transaction already
	 * active" — satu baris bermasalah akan menggagalkan seluruh baris sesudahnya tanpa jejak. Exception
	 * yang dilempar ulang ditangkap {@code catch} tingkat baris, dicatat ke laporan lewat
	 * {@code catatGagal}, dan iterasi lanjut.
	 *
	 * <p><b>Model sesi.</b> Thread latar ini <b>wajib</b> memakai
	 * {@code HibernateUtil.getSessionFactory().openSession()}, bukan {@code currentNativeSession()}:
	 * sesi thread-cache sudah ditutup ketika request unggah selesai, sehingga pemakaiannya melempar
	 * "Session is closed!" pada {@code createCriteria}. Sesi ditutup bertahap di {@code finally}
	 * ({@code clear} &rarr; {@code disconnect} &rarr; {@code close}), masing-masing dibungkus
	 * {@code try-catch} agar kegagalan satu langkah tidak menghalangi langkah berikutnya.
	 *
	 * <p><b>Pelaporan kemajuan.</b> Thread latar menulis persentase ke sebuah {@link Label}; di thread
	 * ZK sebuah {@link Timer} 200&nbsp;ms membaca label itu untuk memperbarui indikator sibuk. Label
	 * kosong menjadi penanda selesai — saat itu timer melepas dirinya, indikator dibersihkan, dan
	 * {@code laporan.selesaikan(eventListener)} menampilkan rincian per baris sekaligus memanggil
	 * callback. Label sengaja dikosongkan di akhir blok {@code run()}, <i>di luar</i> {@code finally},
	 * dan {@code catch} terluar hanya mencatat — sehingga kegagalan pada tingkat berkas (mis. .xlsx
	 * rusak) tetap menutup indikator dengan laporan kosong, bukan menggantung.
	 *
	 * <p><b>Sifat:</b> kembali seketika; pekerjaan sesungguhnya berlangsung asinkron. Pemanggil tidak
	 * boleh berasumsi data sudah tersimpan saat method ini selesai — gunakan {@code eventListener}.
	 *
	 * @param file          berkas .xlsx yang diunggah
	 * @param eventListener callback dipanggil setelah laporan hasil selesai disusun
	 * @param contents      nama-nama kolom (tidak dipakai langsung; bagian dari signature yang
	 *                      dibagi dengan pemanggil unduh data)
	 */
	public void uploadDataMahasiswa(final File file, final EventListener eventListener, final String[] contents)
			throws Exception {

		// Laporan hasil per baris. Menggantikan pemakaian Label "peringatan" untuk pesan akhir,
		// sekaligus mencatat baris yang tak cocok / belum bayar yang sebelumnya hanya jadi teks
		// gabungan tanpa rincian per baris.
		final ais.common.LaporanUpload laporan = new ais.common.LaporanUpload(
				"Upload Peserta Perkuliahan");
		laporan.setNamaBerkasSumber(file.getName());

		final Tbmuser tbmuser = Common.getCurrentUser();

		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses upload data data .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					Clients.clearBusy();
					timer.detach();
					laporan.selesaikan(eventListener);
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = null;
				try {

					XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
					XSSFSheet sheet = workbook.getSheetAt(0);

					// Thread latar: JANGAN pakai currentNativeSession (session thread-cache bisa sudah
					// DITUTUP saat request selesai → "Session is closed!" ketika createCriteria). Buka
					// session dedikasi utk thread ini lalu tutup di finally (clear/disconnect/close).
					session = HibernateUtil.getSessionFactory().openSession();

					int rowCount = (sheet.getLastRowNum() + 1);
					for (int i = 1; i < rowCount; i++) {
						String nimBaris = "";
						try {

							nimBaris = Common.getSheetContentAsString(sheet, 0, i);

							Mahasiswa mahasiswa = (Mahasiswa) Common.getSheetContentAsObject(sheet, 0, i,
									Mahasiswa.class);
							if (mahasiswa == null) {
								// Fallback pencarian lewat NIM. Layar ini sebelumnya TIDAK punya fallback,
								// sehingga baris gagal dicocokkan begitu sel tak terbaca sebagai objek/ID.
								mahasiswa = ConstantValues.ambilByNim(nimBaris);
							}

							if (mahasiswa != null && mahasiswa.getId() != null) {

								Integer semester = Common.getSheetContentAsInteger(sheet, 1, i);
								Integer tahap = Common.getSheetContentAsInteger(sheet, 2, i);
								Integer persetujuan = Common.getSheetContentAsInteger(sheet, 3, i);
								if (persetujuan == null) {
									persetujuan = Detailperkuliahan.BELUM_DISETUJUI;
								}

								if (semester == null) {
									semester = perkuliahan.getSemester();
								}

								try {
									if (!Common.checkStatusPembayaranMahasiswa(semester, tahap, mahasiswa, false,
											semesterPendek != null)) {
										laporan.catatDilewati(i, mahasiswa.getNim(),
												"Belum melakukan pembayaran di semester " + semester
														+ (semesterPendek != null ? " (semester pendek)" : ""));
										continue;
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1162");
									// TODO: handle exception
								}

								Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
										.createCriteria(Detailperkuliahan.class)
										.add(Restrictions.eq("mahasiswa", mahasiswa))
										.add(Restrictions.eq("perkuliahan", perkuliahan)).addOrder(Order.desc("id"))
										.setMaxResults(1).uniqueResult();

								if (detailperkuliahan == null) {
									detailperkuliahan = new Detailperkuliahan(tbmuser, DetailperkuliahanHelper.class);
									detailperkuliahan.setMahasiswa(mahasiswa);
									detailperkuliahan.setSemester(semester);
									detailperkuliahan.setTahap(tahap);
									detailperkuliahan.setPersetujuan(persetujuan);
									detailperkuliahan.setPerkuliahan(perkuliahan);

									session.getTransaction().begin();
									try {
										session.saveOrUpdate(detailperkuliahan);
										session.getTransaction().commit();
									} catch (Exception eSimpan) {
										// WAJIB rollback: tanpa ini transaksi tetap AKTIF sehingga begin() berikutnya
										// melempar "Transaction already active" -- satu baris bermasalah membuat
										// SELURUH baris sesudahnya ikut gagal tanpa jejak.
										try {
											session.getTransaction().rollback();
										} catch (Exception eRoll) {
											ais.common.ErrorAuditUtil.record(eRoll, "rollback-gagal-upload");
										}
										throw eSimpan;
									}
								}

								laporan.catatBerhasil(i, mahasiswa.getNim(), mahasiswa.getNama());

								label.setValue("Upload data \"" + mahasiswa.getNim() + " - " + mahasiswa.getNama()
										+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");
							} else if (nimBaris == null || nimBaris.trim().isEmpty()) {
								laporan.catatDilewati(i, "", "Kolom NIM/NPM kosong");
							} else {
								laporan.catatDilewati(i, nimBaris,
										"NIM/NPM tidak ditemukan pada data mahasiswa -- periksa penulisannya, "
											+ "atau mahasiswa memang belum terdaftar");
							}

						} catch (Exception e) {
							laporan.catatGagal(i, nimBaris, e);
							Common.tampilErrorJikaAdmin(e);
						}

					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/helper/DetailperkuliahanHelper.java:1196");
				} finally {
					if (session != null) {
						try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1199");}
						try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1200");}
						try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DetailperkuliahanHelper.java:1201");}
					}
				}

				label.setValue("");
			}
		}).start();
	}
}
