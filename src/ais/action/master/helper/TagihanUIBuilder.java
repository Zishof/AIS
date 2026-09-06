package ais.action.master.helper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.action.master.InformasiPembayaranMahasiswaAction;
import ais.action.master.pmb.TampilanPaymentGateway;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisDiskonMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.VOMahasiswa;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelStyled2;
import ais.ui.util.MyRowStyled;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.WaktuUtil;

/**
 * Kelas fasad tunggal ({@link #loadTagihan}) yang membangun panel "Rincian Tagihan" — tampilan
 * inti keuangan mahasiswa/calon mahasiswa di seluruh AIS: memuat filter (jenis kegiatan
 * pembayaran, rentang semester mulai-sampai), lalu menghitung dan menampilkan rincian tagihan
 * per item biaya (biaya reguler maupun bulanan/cicilan) lengkap dengan nilai tagihan, jumlah
 * sudah dibayar, sisa, denda, dan persentase, untuk seluruh kombinasi (jenis kegiatan × semester)
 * yang relevan sekaligus.
 *
 * <p>
 * <b>Pemrosesan paralel</b>: setiap kombinasi (jenis kegiatan aktif × semester dalam rentang yang
 * dipilih) diproses sebagai satu {@link java.util.concurrent.Callable} pada
 * {@link java.util.concurrent.ExecutorService} berukuran tetap (dibatasi 1-10 thread, dikonfigurasi
 * lewat {@code tagihan_ui_builder_max_thread}, lihat {@link #getAsyncThreadPoolSize}), dijalankan
 * lewat {@code ais.common.AsyncTaskManager.executeAsync} agar tidak memblokir UI thread ZK. Setiap
 * task membuka sesi Hibernate mandiri sendiri, menghitung {@link DetailBiaya}/
 * {@link PengaturanPembayaranBulanan} yang berlaku (termasuk jalur khusus untuk
 * {@link BiodataCalonMahasiswa} — pendaftaran calon mahasiswa dan daftar ulang mahasiswa baru
 * memiliki logika pengambilan biaya terpisah), menghitung denda lewat
 * {@code checkDenda}/{@link JadwalPembayaran}, jumlah sudah dibayar lewat
 * {@link VOMahasiswa#hitungTotalCicilan}, dan menghasilkan potongan UI ({@link Div} berisi grid
 * rincian) beserta "snapshot" baris laporan (disimpan ke {@code semua}, map yang dipakai bersama
 * oleh pemanggil, mis. untuk pencetakan PDF tagihan) — snapshot ini SENGAJA dipakai apa adanya oleh
 * PDF alih-alih dihitung ulang lewat jalur lain, untuk menghindari selisih angka antara layar dan
 * cetakan (lihat komentar di kode terkait insiden "layar lunas tapi PDF masih menyisakan tagihan").
 * </p>
 *
 * <p>
 * <b>Sinkronisasi UI dari background thread</b>: karena beberapa task membangun potongan komponen
 * ZK di luar UI thread utama, akses ke {@link Desktop} dijaga lewat pola
 * {@code Executions.activate(desktop)}/{@code deactivate(desktop)} (dengan penanganan
 * {@link org.zkoss.zk.ui.DesktopUnavailableException} bila tab sudah ditutup pengguna sebelum
 * task selesai). Progres keseluruhan ditampilkan lewat indikator mengambang
 * ({@code KeuanganDashboardEnhanceUtil.showFloatingProgress}/{@code hideFloatingProgress}) dan
 * kontainer progres inline (label + {@link Progressmeter}) di atas area hasil, dilepas setelah
 * seluruh task selesai lewat callback {@code updateUI}.
 * </p>
 *
 * <p>
 * Kombobox filter jenis kegiatan (dengan opsi tambahan "Belum Lunas"/"Semua Tagihan") dan rentang
 * semester memicu pemuatan ulang otomatis saat berubah; bila {@code actionInstance}
 * ({@link InformasiPembayaranMahasiswaAction}) diberikan, pilihan terakhir pengguna diingat lintas
 * pemanggilan (mis. saat berpindah tab) lewat {@code getLastSelectedJkLabel}/
 * {@code getLastSelectedSmtMulai}/{@code getLastSelectedSmtSampai}. Parameter {@code SMT} yang
 * diberikan langsung oleh pemanggil (bukan dari kombobox) tunduk pada aturan yang sama: tagihan
 * semester yang belum berjalan (lebih besar dari semester berjalan mahasiswa) tidak ditampilkan.
 * </p>
 *
 * <h3>Posisi dalam rantai billing — bukan sekadar penampil</h3>
 * <p>
 * Nama kelas ini ("UIBuilder") mudah disalahpahami sebagai lapisan tampilan murni. Penelusuran
 * memastikan bahwa <b>kelas ini ikut menghitung nominal tagihan</b>, bukan hanya menampilkan angka
 * yang sudah jadi. Pembagian tanggung jawabnya sebagai berikut:
 * </p>
 * <ul>
 * <li><b>Yang didelegasikan</b> (tidak dihitung ulang di sini): resolusi item biaya yang berlaku
 * ({@code PembayaranUtilHelper.getDetailBiayaMahasiswa}/{@code getDetailBiayaCalonMahasiswa},
 * {@code PembayaranUtil.getPengaturanPembayaranSemua}), nominal per item beserta seluruh diskon
 * dan modifikasinya ({@link Kegiatan#ambilJumlahTagihan}), perhitungan denda keterlambatan
 * ({@link DetailBiaya#checkDenda} — yang juga menjadi tujuan akhir
 * {@link PengaturanPembayaranBulanan#checkDenda}), dan akumulasi pembayaran
 * ({@link VOMahasiswa#hitungTotalCicilan}).</li>
 * <li><b>Yang dirakit sendiri di sini</b>: penggabungan keempat primitif di atas menjadi angka
 * akhir per baris — pemilihan antara nominal dan hasil denda, penggantian nominal nol dengan
 * jumlah yang sudah dibayar, pembalikan tanda untuk item {@link ItemBiaya#DIKALI_NILAI_MINUS},
 * penjumlahan {@code totalTagihan}/{@code totalDibayar}/{@code totalBelumDibayar}, persentase
 * pelunasan, serta penentuan baris mana yang layak tampil dan layak masuk laporan.</li>
 * </ul>
 * <p>
 * Perakitan tersebut <b>ditulis dua kali</b> dalam dua cabang yang hampir identik: cabang tagihan
 * <i>bulanan</i> ({@link PengaturanPembayaranBulanan} tidak {@code null}) dan cabang tagihan
 * <i>reguler</i> ({@link DetailBiaya} langsung). Duplikasi ini adalah sumber risiko utama
 * pemeliharaan berkas ini: setiap perbaikan rumus wajib diterapkan pada <b>kedua</b> cabang.
 * </p>
 *
 * <h3>Perbedaan nyata antara kedua cabang perakitan</h3>
 * <p>
 * Kedua cabang tidak sepenuhnya setara. Perbedaan yang sudah terverifikasi:
 * </p>
 * <ol>
 * <li><b>Tanggal acuan denda.</b> Cabang bulanan menelusuri {@link CicilanPembayaran} yang cocok
 * (item biaya sama, {@code bayarKe} sama, kegiatan sama, dan bulan sama) lalu memakai
 * {@code cp.getTanggal()} — <b>tanggal pembayaran yang sebenarnya</b> — sebagai acuan denda.
 * Cabang reguler selalu memakai {@code WaktuUtil.getDate()}, yaitu <b>hari ini</b>, tanpa
 * menelusuri cicilan sama sekali. Karena {@link DetailBiaya#checkDenda} menghitung keterlambatan
 * sebagai {@code deadline.before(tanggalBayar)} dan dapat berlipat menurut jumlah hari terlambat,
 * konsekuensinya: untuk item reguler yang tenggatnya sudah lewat, denda tetap dihitung (dan terus
 * bertambah setiap hari) walaupun mahasiswa sudah melunasinya tepat waktu. Lihat bagian
 * "Catatan integritas finansial" di bawah.</li>
 * <li><b>Argumen konteks bulanan.</b> Cabang bulanan meneruskan objek
 * {@link PengaturanPembayaranBulanan} sehingga {@code checkDenda} dapat memakai
 * {@code pengaturanPembayaranBulanan.getDeadline()} sebagai tenggat; cabang reguler meneruskan
 * {@code null} sehingga tenggat hanya dapat berasal dari {@link JadwalPembayaran} atau, sebagai
 * cadangan, {@code detailBiaya.getDefaultTanggalDeadline()}.</li>
 * <li><b>Urutan syarat pembatalan denda.</b> Cabang bulanan menguji
 * {@code batalkanDenda}/{@code jumlah nol} lebih dulu baru {@code menggunakanDendaCustom}; cabang
 * reguler membalik urutannya. Karena kedua syarat menghasilkan keluaran yang sama
 * ({@code jumlah} apa adanya), pembalikan urutan ini <b>tidak</b> mengubah hasil — semata
 * perbedaan gaya penulisan hasil salin-tempel, dicatat agar tidak disangka bug.</li>
 * </ol>
 *
 * <h3>Catatan integritas finansial (fakta terverifikasi, bukan perubahan)</h3>
 * <ol>
 * <li><b>Denda item reguler dihitung terhadap hari ini, bukan tanggal bayar.</b> Bila sebuah
 * {@link ItemBiaya}/{@link JenisKegiatan} mengaktifkan denda keterlambatan dan tenggatnya terisi,
 * maka setelah tenggat terlampaui nilai {@code jumlah} baris reguler dinaikkan menjadi hasil
 * denda ({@code jumlah = hasilDenda > jumlah ? hasilDenda : jumlah}) tanpa memperhatikan bahwa
 * pembayaran sudah masuk. Akibatnya {@code sisa = jumlah - telahDibayar} menjadi positif dan
 * membesar dari hari ke hari untuk tagihan yang sebenarnya sudah lunas. Penanda
 * {@code DetailKegiatan.batalkanDenda} yang dapat meredamnya <b>tidak</b> diisi otomatis saat
 * pembayaran — satu-satunya penulisnya adalah kotak centang manual pada
 * {@code DetailPembayaranMahasiswaRenderer}. Cabang bulanan tidak terdampak.</li>
 * <li><b>Perbandingan finansial memakai {@code intValue()}.</b> Hampir seluruh keputusan angka di
 * berkas ini membandingkan lewat {@code Double.intValue()}:
 * {@code hasilDenda.intValue() > jumlah.intValue()}, {@code jumlah.intValue() == 0},
 * {@code totalBelumDibayar.intValue() != 0}, dan seterusnya. Dua konsekuensi: pecahan di bawah satu
 * rupiah dipangkas menjadi nol (umumnya justru diinginkan, karena sisa pembulatan dianggap lunas),
 * dan nilai di atas {@code Integer.MAX_VALUE} (sekitar Rp2,15&nbsp;miliar) melipat sehingga
 * perbandingannya tidak lagi bermakna.</li>
 * <li><b>{@code totalValid} secara aljabar sama dengan dua kali {@code jumlah}.</b> Ekspresi
 * {@code jumlah + belumDibayar + telahDibayar} dengan {@code belumDibayar = jumlah - telahDibayar}
 * selalu menyusut menjadi dua kali {@code jumlah}. Jadi syarat {@code totalValid != 0} yang
 * menentukan apakah sebuah baris ditampilkan <i>dan</i> apakah baris tersebut masuk ke snapshot
 * laporan sesungguhnya hanya berarti "nominalnya bukan nol".</li>
 * <li><b>Snapshot laporan dipublikasikan secara atomik.</b> Peta {@code semua} dikosongkan di awal
 * pemuatan, lalu diisi sekali saja di dalam blok {@code synchronized} setelah seluruh
 * {@link Future} terkumpul. Ini perbaikan atas insiden "layar lunas tetapi PDF masih menyisakan
 * tagihan": sebelumnya pekerja paralel menulis langsung ke {@link TreeMap} yang bukan thread-safe,
 * dan snapshot dimasukkan sebelum {@code barisLaporan} selesai diisi. Efek sampingnya: selama
 * pemuatan berlangsung, {@code semua} berada dalam keadaan kosong, sehingga pencetakan yang
 * dipicu di tengah proses menghasilkan laporan hampa, bukan laporan basi.</li>
 * </ol>
 *
 * <h3>Catatan paralelisme (fakta arsitektur)</h3>
 * <p>
 * Walaupun setiap kombinasi dijalankan sebagai {@link java.util.concurrent.Callable} terpisah,
 * <b>bagian terbesar pekerjaan setiap task berjalan di dalam kurungan
 * {@code Executions.activate(desktop)} sampai {@code deactivate(desktop)}</b> — bukan hanya
 * pembaruan label/progress sebagaimana tertulis pada komentar di kode. Kurungan itu mencakup
 * seluruh perulangan per item biaya, termasuk pemanggilan {@link Kegiatan#ambilJumlahTagihan},
 * {@code checkDenda}, dan {@link VOMahasiswa#hitungTotalCicilan}, hingga penyusunan footer total.
 * Karena kunci desktop ZK bersifat eksklusif, para pekerja praktis <b>berjalan bergiliran</b> pada
 * bagian tersebut; keuntungan paralel terbatas pada tahap pengambilan data sebelum kurungan.
 * </p>
 * <p>
 * Konsekuensi penting lainnya: seluruh isi kurungan itu hanya dijalankan bila
 * {@code desktop != null && desktop.isAlive()}. Bila tidak ada desktop hidup, task tetap selesai
 * tetapi mengembalikan peta kosong — tanpa potongan UI <b>dan tanpa snapshot laporan</b>. Jadi
 * kelas ini tidak dapat dipakai sebagai sumber angka pada konteks tanpa sesi ZK aktif (mis. tugas
 * terjadwal atau layanan latar).
 * </p>
 * <p>
 * {@code executor.invokeAll(tasks)} sudah memblokir sampai seluruh task selesai, sehingga
 * {@code awaitTermination(60, SECONDS)} yang menyusul praktis tidak pernah menjadi batas waktu
 * yang efektif; {@code shutdownNow()} pada blok {@code finally} berperan sebagai jaring pengaman
 * bila terjadi pengecualian sebelum penutupan normal.
 * </p>
 *
 * <h3>Catatan otorisasi (fakta arsitektur)</h3>
 * <p>
 * Kelas ini <b>tidak melakukan pemeriksaan otorisasi maupun kepemilikan apa pun</b>. Pemilik
 * tagihan sepenuhnya ditentukan oleh objek {@link VOMahasiswa} yang diserahkan pemanggil; tidak ada
 * verifikasi bahwa pengguna yang sedang masuk berhak melihat keuangan mahasiswa tersebut, dan tidak
 * ada penyempitan satuan kerja. Tombol "Bayar Sekarang"/"Tagihan dan Pembayaran" membuka
 * {@code /common/daftarulang_mahasiswa_lama.zul} dengan id mahasiswa, jenis kegiatan, dan semester
 * sebagai <b>parameter URL</b>; nilai-nilainya berasal dari objek yang sudah dimuat sehingga tidak
 * ada peningkatan hak di sini, tetapi halaman tujuan itulah yang wajib memvalidasi ulang
 * kepemilikan — pola parameter URL semacam ini sudah tercatat berulang sebagai sumber masalah di
 * basis kode ini.
 * </p>
 */
public class TagihanUIBuilder {

	/**
	 * Menentukan ukuran kolam thread paralel untuk satu kali pemuatan rincian tagihan.
	 *
	 * <p>
	 * Nilainya dibaca dari konfigurasi {@code tagihan_ui_builder_max_thread} (baku
	 * {@code "4"}), lalu dijepit ke rentang 1&ndash;10, dan terakhir tidak dibiarkan
	 * melebihi jumlah task yang benar-benar akan dijalankan — tidak ada gunanya membuka
	 * sepuluh thread untuk tiga kombinasi (jenis kegiatan, semester).
	 * </p>
	 *
	 * <p>
	 * Batas atas 10 bersifat keras dan disengaja: setiap task membuka <b>sesi Hibernate
	 * mandiri</b> sendiri, sehingga ukuran kolam ini berbanding lurus dengan jumlah
	 * koneksi basis data yang dipakai serentak oleh satu pengguna yang membuka satu
	 * layar. Menaikkan konfigurasi di luar rentang tersebut tidak akan berpengaruh.
	 * </p>
	 * <p>
	 * Kegagalan membaca atau mengurai konfigurasi ditelan diam-diam dan mengembalikan
	 * nilai baku 4. Perlu diingat bahwa {@code Common.getKonfigurasi(kunci, baku)} pada
	 * basis kode ini akan <b>menuliskan nilai baku ke basis data</b> bila kunci belum
	 * ada, jadi pemanggilan pertama method ini dapat membuat baris konfigurasi baru.
	 * </p>
	 * <p>
	 * Perlu dicatat pula bahwa manfaat nyata dari menaikkan angka ini terbatas: seperti
	 * diuraikan pada Javadoc kelas, bagian terbesar pekerjaan setiap task berjalan di
	 * dalam kurungan kunci desktop ZK yang bersifat eksklusif, sehingga para pekerja
	 * tetap bergiliran pada bagian tersebut.
	 * </p>
	 *
	 * @param totalTasks jumlah kombinasi (jenis kegiatan, semester) yang akan dihitung;
	 *                   nilai nol atau negatif diperlakukan sebagai satu
	 * @return ukuran kolam thread, dijamin minimal 1 dan maksimal 10
	 */
	private static int getAsyncThreadPoolSize(int totalTasks) {
		int maxThread = 4;
		try {
			maxThread = Integer.parseInt(Common.getKonfigurasi("tagihan_ui_builder_max_thread", "4").getNilai().trim());
		} catch (Exception e) {
			maxThread = 4;
		}
		if (maxThread < 1) {
			maxThread = 1;
		}
		if (maxThread > 10) {
			maxThread = 10;
		}
		return Math.max(1, Math.min(maxThread, Math.max(1, totalTasks)));
	}

	/**
	 * Menutup sesi Hibernate mandiri (yang dibuka lewat {@code openSession()}, bukan sesi
	 * per-permintaan milik {@code HibernateUtil.currentSession()}) secara bertahap dan
	 * defensif.
	 *
	 * <p>
	 * Urutannya {@code clear()} &rarr; {@code disconnect()} &rarr; {@code close()}, dan
	 * <b>setiap langkah dibungkus blok {@code try/catch} tersendiri</b> sehingga
	 * kegagalan pada satu langkah tidak menghalangi langkah berikutnya. Ini penting
	 * karena method ini dipanggil dari blok {@code finally} pada jalur penanganan galat:
	 * bila sebuah pengecualian sudah membuat sesi berada dalam keadaan tidak konsisten,
	 * yang paling dibutuhkan adalah koneksi tetap dikembalikan ke kolam, bukan
	 * pengecualian kedua yang menutupi pengecualian pertama.
	 * </p>
	 * <p>
	 * Peran tiap langkah: {@code clear()} melepas seluruh entity dari konteks persistensi
	 * (mencegah flush tak sengaja atas objek yang sudah tidak relevan dan melepas memori
	 * lebih awal), {@code disconnect()} mengembalikan koneksi JDBC ke kolam, dan
	 * {@code close()} menutup sesinya. {@code close()} dijaga dengan pemeriksaan
	 * {@code isOpen()} agar tidak menutup sesi yang sudah tertutup.
	 * </p>
	 * <p>
	 * Setiap kegagalan dicatat ke {@code ais.common.ErrorAuditUtil} alih-alih ditelan
	 * sepenuhnya, sehingga masalah kebocoran koneksi tetap dapat ditelusuri.
	 * </p>
	 * <p>
	 * Perhatikan bahwa method ini <b>tidak</b> melakukan {@code commit} maupun
	 * {@code rollback}; pengelolaan transaksi sepenuhnya menjadi tanggung jawab
	 * pemanggil. Di berkas ini pola yang sama juga ditulis ulang secara sebaris untuk
	 * sesi anak (di dalam blok {@code finally} milik {@code call()}) alih-alih memanggil
	 * method ini — duplikasi yang layak disatukan bila berkas ini dirapikan kelak.
	 * </p>
	 *
	 * @param session sesi yang akan ditutup; {@code null} diperlakukan sebagai
	 *                "tidak ada yang perlu dilakukan"
	 */
	private static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:88");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:92");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:98");
		}
	}


	/**
	 * Bentuk ringkas sembilan argumen: sama persis dengan
	 * {@link #loadTagihan(Component, JenisKegiatan, VOMahasiswa, VOMahasiswa, TreeMap, boolean, boolean, Integer, boolean, InformasiPembayaranMahasiswaAction)}
	 * dengan {@code actionInstance} bernilai {@code null}.
	 *
	 * <p>
	 * Konsekuensi memakai bentuk ini alih-alih bentuk kanonik:
	 * </p>
	 * <ul>
	 * <li>pilihan filter terakhir pengguna (jenis kegiatan, semester mulai/sampai)
	 * <b>tidak diingat</b> lintas pemanggilan, sehingga setiap kali panel dibangun ulang
	 * filter kembali ke nilai baku;</li>
	 * <li>{@code actionInstance.loadKegiatan(...)} tidak dipanggil, sehingga daftar
	 * kegiatan pada layar pemanggil tidak ikut disegarkan mengikuti rentang semester
	 * yang sedang ditampilkan.</li>
	 * </ul>
	 * <p>
	 * Gunakan bentuk ini untuk panel tagihan yang berdiri sendiri (mis. disematkan pada
	 * dialog atau kartu ringkas), dan bentuk kanonik untuk layar
	 * {@link InformasiPembayaranMahasiswaAction} yang memerlukan kesinambungan pilihan
	 * pengguna.
	 * </p>
	 *
	 * @param west               lihat bentuk kanonik
	 * @param selectedJenisKegiatan lihat bentuk kanonik
	 * @param mhsAtas            lihat bentuk kanonik
	 * @param mahasiswa          lihat bentuk kanonik
	 * @param semua              lihat bentuk kanonik
	 * @param vertical           lihat bentuk kanonik
	 * @param sederhana          lihat bentuk kanonik
	 * @param SMT                lihat bentuk kanonik
	 * @param refresh            lihat bentuk kanonik
	 * @return kombobox jenis kegiatan yang dipasang ke {@code west}
	 * @throws Exception diteruskan dari bentuk kanonik
	 */
	public static Combobox loadTagihan(final Component west, final JenisKegiatan selectedJenisKegiatan,
			final VOMahasiswa mhsAtas, final VOMahasiswa mahasiswa, final TreeMap<String, Object[]> semua,
			final boolean vertical, final boolean sederhana, final Integer SMT, final boolean refresh)
			throws Exception {
		return loadTagihan(west, selectedJenisKegiatan, mhsAtas, mahasiswa, semua, vertical, sederhana, SMT, refresh,
				null);
	}

	/**
	 * Implementasi kanonik: membangun form filter ke dalam {@code west}, memasang pendengar
	 * perubahan pada ketiga kombobox filter, lalu memicu pemuatan pertama secara langsung.
	 * Seluruh perhitungan rincian tagihan berjalan asinkron ke dalam sebuah {@link Div}
	 * internal yang menjadi anak baris terakhir form. Lihat Javadoc kelas untuk uraian
	 * lengkap posisi kelas ini dalam rantai billing, perakitan angka, paralelisme, dan
	 * catatan integritas finansial.
	 *
	 * <h4>Kerangka yang dibangun</h4>
	 * <p>
	 * Isi {@code west} dibersihkan lebih dahulu lewat {@link Common#clear(Component)}, lalu
	 * diisi sebuah {@link Grid} dua kolom (label 35% + kendali). Tinggi grid sengaja diatur
	 * {@code vflex="min"} dan <b>bukan</b> {@code height="100%"}: panel "Rincian Tagihan"
	 * adalah host ber-tinggi otomatis, dan tinggi 100% di dalamnya membuat badan grid
	 * kolaps sehingga baris rincian terpotong. Baris-barisnya:
	 * </p>
	 * <ol>
	 * <li><b>"Pembayaran *"</b> — kombobox jenis kegiatan. Sumber isinya ditentukan
	 * {@code vertical}: {@link Common#initJenisPembayaranMahasiswa} bila {@code true},
	 * atau varian yang juga menyertakan kegiatan calon mahasiswa bila {@code false}. Dua
	 * opsi sintetis selalu ditambahkan di belakang: <b>"Belum Lunas"</b> dan
	 * <b>"Semua Tagihan"</b>, keduanya ber-{@code value} {@code null} sehingga dibedakan
	 * hanya lewat teks labelnya. Bila {@code selectedJenisKegiatan} tidak diberikan,
	 * "Semua Tagihan" menjadi pilihan baku.</li>
	 * <li><b>"Smt Mulai"</b> — pilihan {@code 1..maxSemesterPilihan-1} untuk
	 * {@link Mahasiswa}, atau {@code 0..maxSemesterPilihan-1} untuk
	 * {@link BiodataCalonMahasiswa} (semester 0 mewakili tahap pendaftaran, sebelum
	 * perkuliahan dimulai). Nilai baku 1 atau 0 sesuai jenis peserta.</li>
	 * <li><b>"Smt Sampai"</b> — pilihan {@code 1..maxSemesterPilihan-1}; nilai bakunya
	 * adalah semester berjalan mahasiswa ({@code currentSemester()}) atau 1.</li>
	 * <li>baris terakhir ber-{@code colspan} 2 yang menampung {@link Div} hasil.</li>
	 * </ol>
	 * <p>
	 * Kedua baris semester disembunyikan bila {@code SMT} sudah ditentukan pemanggil atau
	 * bila {@code sederhana} bernilai {@code true}. Batas {@code maxSemesterPilihan}
	 * dibaca dari konfigurasi {@code max_semester_pilihan} (baku 25).
	 * </p>
	 *
	 * <h4>Mengapa daftar semester TIDAK dipotong pada semester berjalan</h4>
	 * <p>
	 * Jumlah opsi kedua kombobox sengaja mengikuti {@code max_semester_pilihan} dan bukan
	 * {@code currentSemester()}. Pembatasan berbasis semester berjalan pernah diterapkan
	 * untuk menyembunyikan tagihan masa depan, tetapi menimbulkan kemacetan: pada data
	 * nyata {@code currentSemester()} dapat menghasilkan angka yang lebih kecil daripada
	 * semester yang sedang perlu diproses administrasi, sehingga semester tersebut tidak
	 * pernah tersedia di dropdown dan proses tagihan lanjutan tidak dapat dijalankan.
	 * Pengaman terhadap tagihan masa depan tetap ada, tetapi diletakkan di dua tempat
	 * lain: penyaringan pada {@link InformasiPembayaranMahasiswaAction}, dan penjaga
	 * di awal pemuatan yang menolak parameter {@code SMT} yang melampaui semester
	 * berjalan. Artinya, tagihan semester mendatang hanya muncul bila petugas memang
	 * sengaja memilih rentang tersebut lewat kombobox.
	 * </p>
	 *
	 * <h4>Pemulihan pilihan terakhir pengguna</h4>
	 * <p>
	 * Bila {@code actionInstance} diberikan, jenis kegiatan dipulihkan dengan mencocokkan
	 * <b>teks label</b> ({@code getLastSelectedJkLabel()}) — bukan id — karena dua opsi
	 * sintetis "Belum Lunas"/"Semua Tagihan" tidak memiliki nilai objek. Bila labelnya
	 * tidak lagi ditemukan (mis. jenis kegiatan dinonaktifkan), pemilihan jatuh kembali ke
	 * perilaku baku. "Smt Sampai" dipulihkan dari {@code getLastSelectedSmtSampai()}.
	 * </p>
	 * <p>
	 * <b>Ketidaksimetrisan yang perlu diketahui:</b> "Smt Mulai" <b>tidak</b> dipulihkan.
	 * Nilainya disimpan setiap kali pengguna mengubahnya
	 * ({@code setLastSelectedSmtMulai(...)} dipanggil dari pendengar {@code onChange}),
	 * tetapi {@code getLastSelectedSmtMulai()} tidak pernah dibaca di sini, sehingga
	 * "Smt Mulai" selalu kembali ke 1 (atau 0) pada setiap pembangunan ulang panel.
	 * </p>
	 *
	 * <h4>Pendengar dan pemicu pemuatan</h4>
	 * <p>
	 * Ketiga kombobox diberi pendengar {@code onChange} yang menyimpan pilihan ke
	 * {@code actionInstance} (bila ada) lalu meneruskan ke pendengar pemuatan bersama.
	 * Pemuatan pertama dipicu di akhir method dengan memanggil pendengar tersebut
	 * memakai {@code null} sebagai peristiwa, yang berarti "jalankan sekarang juga"
	 * alih-alih ditunda lewat timer. Method mengembalikan kombobox jenis kegiatan agar
	 * pemanggil dapat membacanya atau memasang pendengar tambahan.
	 * </p>
	 *
	 * @param west               kontainer ZK tujuan form filter dan hasil rincian; isi
	 *                           sebelumnya dibersihkan lewat {@link Common#clear(Component)},
	 *                           jadi method ini mengambil alih kontainer tersebut sepenuhnya
	 * @param selectedJenisKegiatan jenis kegiatan yang dipilih sebagai baku pada kombobox
	 *                           filter; bila {@code null}, opsi sintetis "Semua Tagihan" yang
	 *                           dipilih. Diabaikan bila {@code actionInstance} berhasil
	 *                           memulihkan pilihan terakhir pengguna
	 * @param mhsAtas            <b>tidak dipakai sama sekali</b> di badan method ini — hanya
	 *                           diteruskan dari bentuk ringkas ke bentuk kanonik lalu berhenti.
	 *                           Dipertahankan demi keseragaman kontrak antar-helper serupa.
	 *                           Pemanggil nyata ({@link InformasiPembayaranMahasiswaAction})
	 *                           mengisinya dengan objek mahasiswa yang sama atau dengan calon
	 *                           mahasiswa; keduanya tidak berpengaruh pada hasil
	 * @param mahasiswa          pemilik tagihan: {@link Mahasiswa} atau
	 *                           {@link BiodataCalonMahasiswa}. Inilah satu-satunya penentu
	 *                           data siapa yang ditampilkan — <b>tidak ada pemeriksaan
	 *                           kepemilikan di dalam kelas ini</b>, lihat catatan otorisasi
	 *                           pada Javadoc kelas. Untuk {@link Mahasiswa} yang memiliki
	 *                           {@code biodataCalonMahasiswa}, kegiatan pendaftaran dan daftar
	 *                           ulang dialihkan ke objek calon tersebut secara otomatis
	 * @param semua              peta hasil perhitungan berkunci
	 *                           {@code "<idJenisKegiatan>-<semester>"} yang <b>DITULISI</b>
	 *                           oleh method ini. Nilainya berupa {@code Object[]} empat elemen:
	 *                           daftar {@link DetailBiaya}/{@link PengaturanPembayaranBulanan}
	 *                           yang berlaku, data cicilan, objek {@link Kegiatan}, dan
	 *                           {@code barisLaporan} (snapshot angka per baris). Peta ini
	 *                           dipakai bersama pemanggil untuk mencetak PDF tagihan dengan
	 *                           angka yang identik dengan layar. Ia dikosongkan di awal
	 *                           pemuatan dan baru diisi sekali secara atomik setelah seluruh
	 *                           task selesai, sehingga selama pemuatan berlangsung isinya
	 *                           kosong
	 * @param vertical           menandai tata letak sempit. Pemanggil nyata mengisinya dengan
	 *                           {@code Common.isMobile()}, jadi praktis bermakna "tampilan
	 *                           mobile". Bila {@code true}: kombobox jenis kegiatan diisi
	 *                           {@link Common#initJenisPembayaranMahasiswa} (tanpa kegiatan
	 *                           calon mahasiswa) dan setiap item biaya dirender sebagai
	 *                           groupbox bertumpuk. Bila {@code false}: dipakai varian yang
	 *                           juga menyertakan kegiatan calon mahasiswa, dan rincian
	 *                           dirender sebagai tabel empat kolom
	 *                           (Tagihan/Dibayar/Sisa/%) dengan baris footer total
	 * @param sederhana          tampilan ringkas: menyembunyikan baris-baris form filter,
	 *                           judul per kombinasi, dan seluruh baris rincian per item —
	 *                           menyisakan baris total saja, dan itu pun disembunyikan bila
	 *                           totalnya nol
	 * @param SMT                bila diberikan, mengunci tampilan pada satu semester ini saja
	 *                           dan menyembunyikan kedua kombobox semester. Tunduk pada
	 *                           penjaga tagihan masa depan: bila pemilik tagihan adalah
	 *                           {@link Mahasiswa} dan nilainya melampaui
	 *                           {@code currentSemester()}, pemuatan dibatalkan dan area hasil
	 *                           dikosongkan
	 * @param refresh            memaksa pengambilan ulang dari basis data alih-alih dari
	 *                           cache. Diteruskan ke {@code reInitKegiatan},
	 *                           {@code ambilDetailKegiatanSaja}, {@code ambilCicilan},
	 *                           {@code ambilKegiatans}, resolusi detail biaya, dan
	 *                           {@link Kegiatan#ambilJumlahTagihan}. Menyalakannya membuat
	 *                           pemuatan jauh lebih berat, jadi hanya dipakai saat pengguna
	 *                           menekan segarkan atau setelah data pembayaran berubah
	 * @param actionInstance     bila diberikan, dipakai untuk mengingat dan memulihkan pilihan
	 *                           filter terakhir pengguna dan untuk menyegarkan daftar kegiatan
	 *                           layar pemanggil lewat {@code loadKegiatan(...)}. Perhatikan
	 *                           bahwa "Smt Mulai" disimpan tetapi tidak pernah dipulihkan
	 * @return kombobox jenis kegiatan yang dipasang ke {@code west} (referensi yang sama dengan
	 *         yang tampil pada form filter), agar pemanggil dapat membaca atau memasang
	 *         pendengar tambahan padanya
	 * @throws Exception diteruskan dari pembangunan komponen ZK dan dari pendengar pemuatan
	 *                   pertama yang dipanggil langsung di akhir method
	 */
	@SuppressWarnings("deprecation")
	public static Combobox loadTagihan(final Component west, final JenisKegiatan selectedJenisKegiatan,
			final VOMahasiswa mhsAtas, final VOMahasiswa mahasiswa, final TreeMap<String, Object[]> semua,
			final boolean vertical, final boolean sederhana, final Integer SMT, final boolean refresh,
			final InformasiPembayaranMahasiswaAction actionInstance) throws Exception {

		Common.clear(west);

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setParent(west);
		// Tinggi grid mengikuti isi (natural height), JANGAN 100%. Panel "Rincian Tagihan"
		// adalah host ber-tinggi otomatis; height 100% pada grid di dalam div auto-height
		// membuat body grid kolaps -> baris rincian TERPOTONG. vflex "min" = grid setinggi
		// kontennya sehingga seluruh baris tampil & panel ikut memanjang (tidak terpotong).
		grid.setVflex("min");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new MyRowStyled();
		row.setVisible(!sederhana);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembayaran *"));

		final Combobox jenisKegiatan;
		if (vertical) {
			jenisKegiatan = Common.initJenisPembayaranMahasiswa(null);
		} else {
			jenisKegiatan = Common.initJenisPembayaranMahasiswaDanBiodataCalonMahasiswa(null);
		}
		row.appendChild(jenisKegiatan);

		Comboitem comboitemSemua = new Comboitem("Belum Lunas");
		comboitemSemua.setValue(null);
		jenisKegiatan.appendChild(comboitemSemua);

		Comboitem comboitemSemua2 = new Comboitem("Semua Tagihan");
		comboitemSemua2.setValue(null);
		jenisKegiatan.appendChild(comboitemSemua2);

		if (actionInstance != null && actionInstance.getLastSelectedJkLabel() != null) {
			boolean found = false;
			for (Object cs : jenisKegiatan.getChildren()) {
				Component c = (Component) cs;
				Comboitem ci = (Comboitem) c;
				if (ci.getLabel().equalsIgnoreCase(actionInstance.getLastSelectedJkLabel())) {
					jenisKegiatan.setSelectedItem(ci);
					found = true;
					break;
				}
			}
			if (!found) {
				if (selectedJenisKegiatan == null)
					jenisKegiatan.setSelectedItem(comboitemSemua2);
				else
					Common.selectComboItem(jenisKegiatan, selectedJenisKegiatan);
			}
		} else {
			if (selectedJenisKegiatan == null) {
				jenisKegiatan.setSelectedItem(comboitemSemua2);
			} else {
				Common.selectComboItem(jenisKegiatan, selectedJenisKegiatan);
			}
		}
		jenisKegiatan.setWidth("90%");
		jenisKegiatan.setReadonly(true);

		row = new MyRowStyled();
		row.setVisible(!sederhana && SMT == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt Mulai"));

		final Combobox semesterMulai = new Combobox();
		row.appendChild(semesterMulai);

		int maxSemesterPilihan = 25;
		try {
			maxSemesterPilihan = Integer
					.parseInt(Common.getKonfigurasi("max_semester_pilihan", "25").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:202");
		}
		if (mahasiswa instanceof Mahasiswa) {
			for (int i = 1; i < maxSemesterPilihan; i++) {
				Comboitem ci = new Comboitem(String.valueOf(i));
				ci.setValue(i);
				semesterMulai.appendChild(ci);
			}
		} else {
			for (int i = 0; i < maxSemesterPilihan; i++) {
				Comboitem ci = new Comboitem(String.valueOf(i));
				ci.setValue(i);
				semesterMulai.appendChild(ci);
			}
		}

		Common.selectComboItem(semesterMulai, (mahasiswa instanceof Mahasiswa) ? 1 : 0);

		semesterMulai.setWidth("90%");
		semesterMulai.setReadonly(true);

		row = new MyRowStyled();
		row.setVisible(!sederhana && SMT == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Smt Sampai"));

		final Combobox semesterSampai = new Combobox();
		row.appendChild(semesterSampai);

		for (int i = 1; i < maxSemesterPilihan; i++) {
			Comboitem ci = new Comboitem(String.valueOf(i));
			ci.setValue(i);
			semesterSampai.appendChild(ci);
		}

		if (actionInstance != null && actionInstance.getLastSelectedSmtSampai() != null) {
			Integer smtTerakhir = actionInstance.getLastSelectedSmtSampai();
			Common.selectComboItem(semesterSampai, smtTerakhir);
		} else {
			Common.selectComboItem(semesterSampai,
					(mahasiswa instanceof Mahasiswa) ? ((Mahasiswa) mahasiswa).currentSemester() : 1);
		}

		semesterSampai.setWidth("90%");
		semesterSampai.setReadonly(true);

		final Div myDiv = new MyDiv();
		MyRowStyled gripRincianBiayaGlobal = new MyRowStyled();
		gripRincianBiayaGlobal.appendChild(myDiv);
		gripRincianBiayaGlobal.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(gripRincianBiayaGlobal, "2");

		final EventListener itemBiayaEventListener = new EventListener() {
			private EventListener getThis() {
				return this;
			}

			/**
			 * Titik masuk setiap pemuatan ulang rincian tagihan — dipanggil sekali di akhir
			 * {@code loadTagihan} dan setiap kali salah satu dari ketiga kombobox filter
			 * berubah.
			 *
			 * <h4>Urutan kerja</h4>
			 * <ol>
			 * <li><b>Penjaga tagihan masa depan.</b> Bila pemilik tagihan adalah
			 * {@link Mahasiswa}, {@code SMT} diberikan pemanggil, dan nilainya melampaui
			 * {@code currentSemester()}, area hasil dikosongkan dan method langsung
			 * kembali. Penjaga ini sengaja diletakkan di sini agar parameter langsung dari
			 * pemanggil tunduk pada aturan yang sama dengan kombobox: rincian tagihan
			 * semester yang belum berjalan tidak boleh terbuka dengan sendirinya. Perhatikan
			 * bahwa penjaga ini <b>tidak</b> berlaku untuk {@link BiodataCalonMahasiswa}
			 * (yang memang beroperasi di semester 0/1) dan tidak berlaku pada jalur kombobox.</li>
			 * <li><b>Penguncian semester menurut jenis kegiatan.</b> Untuk pemilik bertipe
			 * {@link Mahasiswa}, bila jenis kegiatan terpilih adalah
			 * {@code PENDAFTARAN_CALON_MAHASISWA}, kedua kombobox semester dipaksa ke 0 dan
			 * <b>dinonaktifkan</b>; bila {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}, dipaksa ke
			 * 1 tetapi tetap dapat diubah. Untuk jenis kegiatan lain, keduanya diaktifkan
			 * kembali. Ini mencerminkan kenyataan bahwa biaya pendaftaran melekat pada tahap
			 * pra-perkuliahan, bukan pada semester perkuliahan mana pun.
			 * <br>
			 * Catatan implementasi: kombobox "Smt Sampai" hanya dibangun dengan opsi mulai
			 * dari 1, sehingga permintaan memilih 0 padanya tidak menemukan item yang cocok.
			 * Hal ini tidak menimbulkan masalah karena rentang untuk
			 * {@code PENDAFTARAN_CALON_MAHASISWA} kemudian dipaksa ulang menjadi 0..0 di
			 * dalam penyusunan daftar task, terlepas dari isi kombobox.</li>
			 * <li><b>Pembacaan rentang.</b> {@code sMulaiOpt}/{@code sSampaiOpt} dibaca dari
			 * kombobox dengan nilai cadangan 1. Bila pemilik tagihan ada tetapi "Smt Sampai"
			 * belum memiliki item terpilih, method kembali tanpa berbuat apa-apa —
			 * melindungi dari pemuatan setengah jadi saat komponen belum selesai
			 * diinisialisasi.</li>
			 * <li><b>Sinkronisasi layar pemanggil.</b> Bila {@code actionInstance} ada,
			 * {@code loadKegiatan(false, sMulaiOpt, sSampaiOpt)} dipanggil agar daftar
			 * kegiatan di layar induk mengikuti rentang yang sedang ditampilkan.</li>
			 * <li><b>Penentuan himpunan jenis kegiatan.</b> Bila label terpilih adalah
			 * "Semua Tagihan" atau "Belum Lunas", <b>seluruh</b> item kombobox yang memiliki
			 * nilai objek dikumpulkan ke {@code mapsJk}; selain itu hanya satu jenis kegiatan
			 * terpilih. Kedua label sintetis menghasilkan himpunan task yang <b>identik</b> —
			 * perbedaannya baru muncul jauh di belakang, saat menentukan apakah blok hasil
			 * suatu kombinasi ditampilkan: "Belum Lunas" menyembunyikan blok yang sisanya
			 * nol, sedangkan "Semua Tagihan" menyembunyikan blok yang tagihan, dibayar, dan
			 * sisanya sama-sama nol. Jadi memilih "Belum Lunas" tidak menghemat
			 * perhitungan.</li>
			 * <li><b>Pengosongan dan indikator.</b> Peta {@code semua} dan area hasil
			 * dikosongkan, indikator mengambang
			 * {@code KeuanganDashboardEnhanceUtil.showFloatingProgress} dinyalakan, dan
			 * sebuah kontainer progres inline (judul, keterangan, {@link Progressmeter},
			 * pencacah) ditempelkan sebagai anak pertama area hasil. Kontainer inline itu
			 * kelak dilepas oleh {@code updateUI}.</li>
			 * <li><b>Delegasi.</b> Seluruh perhitungan diserahkan ke
			 * {@code ais.common.AsyncTaskManager.executeAsync} dengan sepasang
			 * {@code BackgroundTask}/{@code UITask}.</li>
			 * </ol>
			 *
			 * <h4>Hal yang perlu diketahui pemelihara</h4>
			 * <ul>
			 * <li>{@code Executions.getCurrent().getDesktop()} diambil <b>di sini</b>, selagi
			 * masih berada di benang UI, lalu ditutup ke dalam seluruh task. Inilah satu-satunya
			 * jalan bagi pekerja latar untuk menyentuh komponen ZK.</li>
			 * <li>Karena {@code semua.clear()} dilakukan sebelum task berjalan sedangkan
			 * pengisiannya baru terjadi di akhir, setiap pencetakan yang dipicu di tengah
			 * pemuatan akan membaca peta kosong.</li>
			 * <li>{@code gateway} ({@code TampilanPaymentGateway.adaPaymentGatewayYangAktif()})
			 * dibaca sekali di awal dan hanya menentukan teks serta ikon tombol aksi
			 * ("Bayar Sekarang" versus "Tagihan dan Pembayaran"), bukan perilakunya — kedua
			 * varian membuka halaman tujuan yang sama.</li>
			 * <li>Tidak ada mekanisme pembatalan: bila pengguna mengubah filter selagi
			 * pemuatan sebelumnya masih berjalan, pemuatan lama tetap berlanjut dan hasilnya
			 * dapat ikut ditempelkan ke area yang sudah dikosongkan pemuatan baru.</li>
			 * </ul>
			 *
			 * @throws Exception diteruskan dari pembangunan komponen ZK dan dari
			 *                   {@code AsyncTaskManager.executeAsync}
			 */
			@SuppressWarnings({ "unchecked", "rawtypes" })
			private void load() throws Exception {
				final boolean gateway = TampilanPaymentGateway.adaPaymentGatewayYangAktif();
				if (mahasiswa instanceof Mahasiswa && SMT != null
						&& SMT > ((Mahasiswa) mahasiswa).currentSemester()) {
					// Parameter langsung dari pemanggil juga tidak boleh membuka rincian tagihan
					// semester masa depan sebelum semester tersebut menjadi semester berjalan.
					Common.clear(myDiv);
					return;
				}
				final String label = (jenisKegiatan.getSelectedItem() == null ? ""
						: jenisKegiatan.getSelectedItem().getLabel()).trim();
				final JenisKegiatan jenisKegiatanData = (JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
						: jenisKegiatan.getSelectedItem().getValue());

				final VOMahasiswa mhsUtama = mahasiswa;

				if (mahasiswa instanceof Mahasiswa) {
					if (jenisKegiatanData != null && jenisKegiatanData.getId() != null) {
						boolean isPendaftaranCalon = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
								&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
										.equals(jenisKegiatanData.getId()));
						boolean isDaftarUlang = (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
								&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId()
										.equals(jenisKegiatanData.getId()));

						if (isPendaftaranCalon || isDaftarUlang) {
							if (isPendaftaranCalon) {
								Common.selectComboItem(true, semesterMulai, 0);
								Common.selectComboItem(true, semesterSampai, 0);
								semesterMulai.setDisabled(true);
								semesterSampai.setDisabled(true);
							} else if (isDaftarUlang) {
								Common.selectComboItem(true, semesterMulai, 1);
								Common.selectComboItem(true, semesterSampai, 1);
								semesterMulai.setDisabled(false);
								semesterSampai.setDisabled(false);
							}
						} else {
							semesterMulai.setDisabled(false);
							semesterSampai.setDisabled(false);
						}
					}
				}

				if (mhsUtama != null && semesterSampai.getSelectedItem() == null) {
					return;
				}

				final Integer sMulaiOpt = semesterMulai.getSelectedItem() != null
						? (Integer) semesterMulai.getSelectedItem().getValue()
						: 1;
				final Integer sSampaiOpt = semesterSampai.getSelectedItem() != null
						? (Integer) semesterSampai.getSelectedItem().getValue()
						: 1;

				if (actionInstance != null) {
					actionInstance.loadKegiatan(false, sMulaiOpt, sSampaiOpt);
				}

				final Map<Long, JenisKegiatan> mapsJk = new HashMap<Long, JenisKegiatan>();
				if (label.trim().equalsIgnoreCase("Semua Tagihan") || label.trim().equalsIgnoreCase("Belum Lunas")) {
					List<Comboitem> ci = jenisKegiatan.getChildren();
					for (Comboitem c : ci) {
						JenisKegiatan tempJk = (JenisKegiatan) c.getValue();
						if (tempJk != null) {
							mapsJk.put(tempJk.getId(), tempJk);
						}
					}
				} else if (jenisKegiatanData != null) {
					mapsJk.put(jenisKegiatanData.getId(), jenisKegiatanData);
				}

				semua.clear();
				Common.clear(myDiv);
				ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("tagihan_ui_builder",
						"Memuat Tagihan",
						"Mengambil item biaya, tagihan bulanan, cicilan, denda, dan status pembayaran.", 10);

				final Desktop desktop = Executions.getCurrent().getDesktop();

				// ==============================================================================
				// PERSIAPAN INDIKATOR PROGRESS UI (INLINE DI ATAS myDiv)
				// ==============================================================================
				final Div progressContainer = new Div();
				progressContainer.setWidth("100%");
				progressContainer.setStyle("background:linear-gradient(135deg,#eff6ff,#ffffff); border: 1px solid #bfdbfe; padding: 16px; margin-bottom: 15px; border-radius: 16px; box-shadow:0 10px 26px rgba(37,99,235,.10); box-sizing:border-box;");
				progressContainer.setParent(myDiv); 

				Vbox vboxPb = new Vbox();
				vboxPb.setWidth("100%");
				vboxPb.setAlign("center");
				vboxPb.setParent(progressContainer);

				final Label lblTitle = new Label(ais.common.Common.getBahasaConfig("Mempersiapkan Data Tagihan"));
				lblTitle.setStyle("font-weight:bold; font-size:15px; margin-bottom:5px; color:#0f172a;");
				lblTitle.setParent(vboxPb);

				final Label lblInfo = new Label(ais.common.Common.getBahasaConfig("Menghitung tagihan dan pembayaran. Mohon tunggu sebentar."));
				lblInfo.setStyle("font-weight:bold; font-size:12px; margin-bottom:10px; color:#2563eb;");
				lblInfo.setParent(vboxPb);

				final Progressmeter progressMeter = new Progressmeter();
				progressMeter.setWidth("95%");
				progressMeter.setValue(0);
				progressMeter.setParent(vboxPb);

				final Label lblCounter = new Label(ais.common.Common.getBahasaConfig("Menghitung total antrean..."));
				lblCounter.setStyle("font-size: 11px; color: gray; margin-top:8px;");
				lblCounter.setParent(vboxPb);

				// ==============================================================================
				// MENGGUNAKAN ASYNCTASKMANAGER DENGAN TETAP MEMPERTAHANKAN INDIKATOR
				// ==============================================================================
				ais.common.AsyncTaskManager.executeAsync(desktop, null, null,
						new ais.common.AsyncTaskManager.BackgroundTask() {

							/**
							 * Badan pekerjaan latar: menyiapkan cache bersama, menyusun dan menjalankan
							 * seluruh task per kombinasi (jenis kegiatan, semester), lalu memublikasikan
							 * hasilnya ke layar dan ke peta {@code semua}.
							 *
							 * <h4>Tahap 1 — sesi induk dan cache bersama</h4>
							 * <p>
							 * Sebuah sesi Hibernate mandiri ("parent") dibuka semata untuk menyegarkan cache
							 * kegiatan pemilik tagihan ({@code reInitKegiatan}) dan mengambil dua koleksi
							 * yang akan <b>dipakai bersama oleh seluruh task</b>:
							 * {@code detailKegiatansTempGlobal} ({@link DetailKegiatan}) dan
							 * {@code cicilanPembayaransTemp} ({@link CicilanPembayaran}). Untuk pemilik
							 * bertipe {@link Mahasiswa} yang memiliki {@code biodataCalonMahasiswa}, koleksi
							 * milik objek calon tersebut <b>ikut digabungkan</b> — inilah yang memungkinkan
							 * satu layar menampilkan tagihan pendaftaran/daftar ulang bersama tagihan
							 * semester berjalan.
							 * </p>
							 * <p>
							 * Transaksi induk <b>sengaja di-{@code commit} lebih awal</b>, tepat setelah
							 * pengambilan di atas dan sebelum task paralel dimulai. Alasannya tercatat pada
							 * komentar di kode: kueri anak dan pemanggilan payment gateway dapat berlangsung
							 * lama, sehingga menahan koneksi induk selama itu berisiko koneksi keburu
							 * ditutup kolam sebelum commit akhir.
							 * </p>
							 * <p>
							 * Perlu dicatat bahwa kedua koleksi bersama itu adalah {@link ArrayList} biasa
							 * yang kemudian dibaca oleh banyak thread sekaligus. Ini aman selama tidak ada
							 * yang mengubahnya setelah task dimulai — dan memang tidak ada — tetapi
							 * merupakan asumsi implisit yang mudah dilanggar oleh perubahan kelak.
							 * </p>
							 *
							 * <h4>Tahap 2 — menyusun daftar task</h4>
							 * <p>
							 * Perulangan dijalankan <b>dua kali</b> atas {@code mapsJk.values()}: sekali
							 * untuk menghitung {@code calcTotalTasks} (dipakai sebagai penyebut persentase
							 * kemajuan), sekali lagi untuk benar-benar membuat objek
							 * {@link java.util.concurrent.Callable}. Kedua perulangan memakai syarat saring
							 * yang sama: jenis kegiatan dilewati bila {@code null}, tanpa id, atau
							 * <b>tidak aktif</b>. Rentang semesternya adalah {@code SMT..SMT} bila pemanggil
							 * menentukannya, selain itu {@code sMulaiOpt..sSampaiOpt}; khusus
							 * {@code PENDAFTARAN_CALON_MAHASISWA} rentangnya dipaksa {@code 0..0}. Bila
							 * ternyata tidak ada task sama sekali, penyebut dijaga minimal 1 agar
							 * perhitungan persentase tidak membagi nol.
							 * </p>
							 * <p>
							 * Karena {@code mapsJk} adalah {@link java.util.HashMap}, urutan pemrosesan
							 * jenis kegiatan tidak ditentukan. Urutan tampilan akhir juga mengikuti urutan
							 * penyelesaian {@link Future} (yaitu urutan penyusunan task), bukan urutan
							 * alfabet atau urutan kombobox — sehingga susunan blok pada layar dapat berbeda
							 * antar pemuatan.
							 * </p>
							 *
							 * <h4>Tahap 3 — eksekusi</h4>
							 * <p>
							 * {@code executor.invokeAll(tasks)} menjalankan seluruh task dan <b>memblokir
							 * sampai semuanya selesai</b>. Karena itu {@code awaitTermination(60, SECONDS)}
							 * yang menyusul praktis selalu langsung terpenuhi dan bukan batas waktu yang
							 * efektif; {@code shutdownNow()} berperan sebagai jaring pengaman pada blok
							 * {@code finally}.
							 * </p>
							 *
							 * <h4>Tahap 4 — publikasi atomik</h4>
							 * <p>
							 * Seluruh {@link Future} dipanen lebih dahulu ke dalam koleksi lokal
							 * ({@code hasilSelesai} dan {@code semuaSelesai}), baru kemudian peta
							 * {@code semua} dikosongkan dan diisi sekaligus di dalam blok
							 * {@code synchronized}. Pola ini adalah perbaikan atas dua masalah yang
							 * tercatat pada komentar di kode: {@link TreeMap} bukan struktur yang aman
							 * lintas-thread, dan pada rancangan sebelumnya snapshot dimasukkan
							 * <b>sebelum</b> {@code barisLaporan} selesai diisi, sehingga "Cetak Semua
							 * Tagihan" kadang membaca snapshot kosong atau sebagian walaupun rincian di
							 * layar sudah lengkap.
							 * </p>
							 * <p>
							 * Setelah itu potongan UI setiap task dipindahkan ke area hasil di dalam satu
							 * kurungan {@code Executions.activate}/{@code deactivate}. Anak-anak
							 * {@code localDiv} disalin ke daftar sementara lebih dahulu sebelum
							 * dipindahkan, karena memindahkan komponen ZK mengubah daftar anak induknya
							 * selagi di-iterasi. Bila tidak ada satu pun task yang menghasilkan UI, sebuah
							 * kotak "Info Pembayaran" berisi pesan bahwa belum ada informasi yang dapat
							 * ditampilkan dipasang sebagai gantinya.
							 * </p>
							 *
							 * <h4>Penanganan galat dan pelepasan sumber daya</h4>
							 * <ul>
							 * <li>{@link org.zkoss.zk.ui.DesktopUnavailableException} ditangkap dan
							 * <b>diabaikan tanpa dicatat</b>: itu berarti pengguna menutup tab selagi
							 * perhitungan berjalan, kondisi normal yang tidak perlu memenuhi tabel audit.</li>
							 * <li>{@link InterruptedException} ditangkap lalu status interupsi benang
							 * dipulihkan dengan {@code Thread.currentThread().interrupt()}.</li>
							 * <li>Galat lain di-{@code rollback} (bila transaksi induk masih aktif) dan
							 * dicatat ke {@code ais.common.ErrorAuditUtil}.</li>
							 * <li>Blok {@code finally} terluar selalu menutup sesi induk lewat
							 * {@link #closeOpenedSession(Session)}, dan kedua koleksi bersama dikosongkan
							 * setelah dipakai sebagai pelepasan memori.</li>
							 * </ul>
							 *
							 * @return selalu {@code null}; hasil nyata dipublikasikan lewat efek samping ke
							 *         peta {@code semua} dan ke pohon komponen ZK
							 * @throws Exception dinyatakan demi kontrak
							 *                   {@code AsyncTaskManager.BackgroundTask}; pada praktiknya
							 *                   seluruh pengecualian sudah ditangkap di dalam
							 */
							@Override
							public Object doInBackground() throws Exception {

								Session parentSession = null;
								boolean isParentTransaction = false;
								java.util.concurrent.ExecutorService executor = null;

								try {
									parentSession = HibernateUtil.getSessionFactory().openSession();
									if (!parentSession.getTransaction().isActive()) {
										parentSession.beginTransaction();
										isParentTransaction = true;
									}

									final Collection<DetailKegiatan> detailKegiatansTempGlobal = new ArrayList<DetailKegiatan>();
									final List<CicilanPembayaran> cicilanPembayaransTemp = new ArrayList<CicilanPembayaran>();

									if (mhsUtama != null) {
										if (refresh)
											mhsUtama.reInitKegiatan(parentSession);

										if (mhsUtama instanceof BiodataCalonMahasiswa) {
											detailKegiatansTempGlobal.addAll(((BiodataCalonMahasiswa) mhsUtama)
													.ambilDetailKegiatanSaja(refresh));
											cicilanPembayaransTemp
													.addAll(((BiodataCalonMahasiswa) mhsUtama).ambilCicilan(refresh));
										} else {
											detailKegiatansTempGlobal.addAll(mhsUtama.ambilDetailKegiatanSaja(refresh));
											Mahasiswa objMhs = (Mahasiswa) mhsUtama;
											cicilanPembayaransTemp.addAll(objMhs.ambilCicilan(refresh));

											if (objMhs.getBiodataCalonMahasiswa() != null) {
												BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) ConstantValues
														.ambil(BiodataCalonMahasiswa.class.getName(),
																objMhs.getBiodataCalonMahasiswa());
												if (bcm != null) {
													if (refresh)
														bcm.reInitKegiatan(parentSession);
													detailKegiatansTempGlobal
															.addAll(bcm.ambilDetailKegiatanSaja(refresh));
													cicilanPembayaransTemp.addAll(bcm.ambilCicilan(refresh));
												}
											}
										}
									}
									/* Parent session hanya dipakai untuk re-init di atas. Jangan tahan koneksi dan
									 * transaksi selama seluruh child task berjalan karena gateway/query child dapat
									 * lama dan koneksi parent keburu ditutup oleh pool sebelum commit akhir. */
									if (isParentTransaction && parentSession.getTransaction().isActive()) {
										parentSession.getTransaction().commit();
										isParentTransaction = false;
									}

									int calcTotalTasks = 0;
									for (JenisKegiatan jk : mapsJk.values()) {
										if (jk == null || jk.getId() == null
												|| (jk.getAktif() != null && !jk.getAktif()))
											continue;
										Integer mulai = SMT != null ? SMT : sMulaiOpt;
										Integer sampai = SMT != null ? SMT : sSampaiOpt;
										if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
												&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
														.equals(jk.getId())) {
											mulai = 0;
											sampai = 0;
										}
										for (int smt = mulai; smt <= sampai; smt++)
											calcTotalTasks++;
									}

									final int totalTasks = calcTotalTasks > 0 ? calcTotalTasks : 1;
									final java.util.concurrent.atomic.AtomicInteger completedTasks = new java.util.concurrent.atomic.AtomicInteger(
											0);

									executor = java.util.concurrent.Executors.newFixedThreadPool(getAsyncThreadPoolSize(totalTasks));
									List<java.util.concurrent.Callable<Map<String, Object>>> tasks = new ArrayList<java.util.concurrent.Callable<Map<String, Object>>>();

									for (final JenisKegiatan jk : mapsJk.values()) {
										if (jk == null || jk.getId() == null
												|| (jk.getAktif() != null && !jk.getAktif()))
											continue;

										Integer mulai = SMT != null ? SMT : sMulaiOpt;
										Integer sampai = SMT != null ? SMT : sSampaiOpt;

										if (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
												&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA.getId()
														.equals(jk.getId())) {
											mulai = 0;
											sampai = 0;
										}

										for (int smt = mulai; smt <= sampai; smt++) {
											final int fSmt = smt;

											tasks.add(new java.util.concurrent.Callable<Map<String, Object>>() {
												/**
												 * Inti perhitungan: menghasilkan rincian tagihan lengkap untuk <b>satu</b>
												 * kombinasi (jenis kegiatan, semester). Inilah tempat angka yang dilihat
												 * pengguna dan angka yang dicetak ke PDF benar-benar dirakit.
												 *
												 * <h4>Langkah 1 — menentukan objek pemilik yang tepat</h4>
												 * <p>
												 * Pemilik baku adalah {@code mhsUtama}. Namun bila pemiliknya
												 * {@link Mahasiswa} dan jenis kegiatan yang sedang diproses adalah
												 * {@code PENDAFTARAN_CALON_MAHASISWA} atau
												 * {@code PENDAFTARAN_ULANG_MAHASISWA_BARU}, sasaran dialihkan ke
												 * {@link BiodataCalonMahasiswa} miliknya. Sebabnya biaya pendaftaran
												 * melekat pada berkas calon, bukan pada mahasiswa yang sudah ber-NIM.
												 * Bila objek calon tidak ditemukan, sasaran tetap mahasiswa.
												 * </p>
												 *
												 * <h4>Langkah 2 — resolusi item biaya yang berlaku</h4>
												 * <p>
												 * Untuk sasaran {@link Mahasiswa}, daftar diambil lewat
												 * {@code PembayaranUtilHelper.getDetailBiayaMahasiswa}. Untuk sasaran
												 * {@link BiodataCalonMahasiswa}, program studi acuan ditentukan berjenjang:
												 * {@code prodiLulus} bila ada, selain itu {@code prodi1}, selain itu
												 * {@code prodi2} — lalu dipakai
												 * {@code getDetailBiayaCalonMahasiswa} dengan varian berbeda untuk
												 * pendaftaran (tanpa semester) dan daftar ulang (dengan semester).
												 * Perhatikan bahwa untuk sasaran calon, jenis kegiatan <b>selain</b> kedua
												 * jenis tersebut tidak menghasilkan daftar apa pun sehingga kombinasi itu
												 * berakhir kosong.
												 * </p>
												 * <p>
												 * Berikutnya {@code PembayaranUtilHelper.countBulanan} memeriksa apakah
												 * kombinasi ini memiliki {@link PengaturanPembayaranBulanan}. Bila ada,
												 * daftar item biaya <b>diambil ulang</b> dalam mode bulanan, sehingga
												 * elemen koleksi berubah dari {@link DetailBiaya} menjadi
												 * {@link PengaturanPembayaranBulanan}. Karena itulah perulangan utama
												 * memeriksa tipe setiap elemen dengan {@code instanceof} dan bercabang
												 * menjadi dua jalur perakitan. Bila daftar akhirnya kosong, method segera
												 * mengembalikan peta kosong dan kombinasi ini tidak muncul di layar
												 * maupun di laporan.
												 * </p>
												 *
												 * <h4>Langkah 3 — konteks kegiatan, cicilan, dan jadwal denda</h4>
												 * <p>
												 * {@link Kegiatan} untuk kombinasi ini diambil lewat
												 * {@code ambilKegiatans}, daftar cicilan disaring dari koleksi bersama, dan
												 * {@code dataCicilan} disusun sebagai {@code Object[]} enam elemen untuk
												 * dititipkan ke snapshot laporan. Tahun akademik dihitung lewat
												 * {@code Common.getTahunAkademik} dari semester, tahun angkatan, semester
												 * masuk pindahan, dan nama semester mulai; hasilnya menjadi teks
												 * {@code "<tahun>/<tahun+1>"} yang dipakai untuk mencari
												 * {@link JadwalPembayaran} beserta aturan dendanya.
												 * </p>
												 * <p>
												 * Identitas peserta dikumpulkan lewat dua cabang (mahasiswa versus calon).
												 * Khusus {@code gelombangPendaftaran} milik mahasiswa diambil lewat
												 * <b>refleksi</b> ({@code getMethod("getGelombangPendaftaran")}) di dalam
												 * blok {@code try} — pola bertahan hidup terhadap perbedaan versi model,
												 * yang berarti hilangnya method tersebut tidak akan terdeteksi saat
												 * kompilasi melainkan berubah menjadi nilai {@code null} secara diam-diam.
												 * </p>
												 * <p>
												 * Variabel {@code jdw} bernilai objek jadwal <b>hanya bila</b> jadwal
												 * tersebut memiliki {@code khususUntukNim} yang memuat
												 * {@code ","+identitas+","}; selain itu {@code null}. Jadi untuk mayoritas
												 * peserta, argumen jadwal yang diteruskan ke perhitungan denda adalah
												 * {@code null}, dan tenggat akhirnya bersandar pada
												 * {@code detailBiaya.getDefaultTanggalDeadline()} atau pada deadline
												 * bulanan. Pola pencocokan berbasis teks yang diapit koma ini menuntut
												 * kolom {@code khususUntukNim} ditulis dengan koma di kedua ujungnya.
												 * </p>
												 *
												 * <h4>Langkah 4 — perakitan angka per item (dua cabang)</h4>
												 * <p>
												 * Seluruh perulangan per item biaya berjalan <b>di dalam</b> kurungan
												 * {@code Executions.activate(desktop)} … {@code deactivate(desktop)}.
												 * Komentar di kode menyebut kurungan itu hanya untuk memperbarui
												 * label/progress, tetapi kenyataannya mencakup seluruh perhitungan dan
												 * perenderan — lihat catatan paralelisme pada Javadoc kelas. Bila tidak ada
												 * desktop hidup, <b>seluruh</b> blok ini dilewati sehingga peta hasil
												 * kembali kosong: tanpa UI dan tanpa snapshot laporan.
												 * </p>
												 * <p>
												 * Untuk setiap elemen, {@link DetailKegiatan} pasangannya dicari lewat
												 * {@code ambilSatuDetailKegiatan}. Item yang ditandai
												 * {@code bukanTagihan} dilewati seluruhnya. Selebihnya dirakit lewat salah
												 * satu dari dua cabang dengan urutan operasi yang sama:
												 * </p>
												 * <ol>
												 * <li>nominal pokok lewat {@link Kegiatan#ambilJumlahTagihan} ({@code null}
												 * dinormalkan menjadi 0);</li>
												 * <li>hasil denda lewat {@code checkDenda}, kecuali bila
												 * {@code batalkanDenda} aktif, nominalnya nol, atau
												 * {@code menggunakanDendaCustom} aktif — pada ketiga keadaan itu nominal
												 * dipakai apa adanya. Bila {@code menggunakanDendaCustom} aktif, teks
												 * {@code infoDenda} diisi keterangan besaran denda kustom;</li>
												 * <li>jumlah yang sudah dibayar lewat
												 * {@link VOMahasiswa#hitungTotalCicilan} ({@code null} dinormalkan
												 * menjadi 0);</li>
												 * <li>bila nominal nol tetapi ada pembayaran, nominal <b>disamakan</b>
												 * dengan jumlah yang sudah dibayar — agar item yang tagihannya sudah
												 * dihapus tidak tampak seperti kelebihan bayar;</li>
												 * <li>nominal dinaikkan ke hasil denda bila denda lebih besar;</li>
												 * <li>untuk item ber-{@code penghitungan}
												 * {@link ItemBiaya#DIKALI_NILAI_MINUS} (potongan/pengurang), nominal dan
												 * jumlah dibayar dijadikan <b>negatif</b> lewat {@code -Math.abs(...)}.</li>
												 * </ol>
												 * <p>
												 * <b>Perbedaan penting antara kedua cabang</b> ada pada acuan tanggal
												 * denda: cabang bulanan menelusuri cicilan yang cocok dan memakai tanggal
												 * pembayaran sebenarnya, sedangkan cabang reguler selalu memakai hari ini.
												 * Uraian lengkap beserta konsekuensinya ada pada Javadoc kelas, bagian
												 * "Perbedaan nyata antara kedua cabang perakitan" dan "Catatan integritas
												 * finansial". Pada penelusuran cicilan cabang bulanan, pencocokan
												 * dilakukan berulang tanpa berhenti pada kecocokan pertama, sehingga bila
												 * ada lebih dari satu cicilan yang cocok maka <b>yang terakhir</b> yang
												 * menentukan tanggal acuan.
												 * </p>
												 *
												 * <h4>Langkah 5 — akumulasi dan snapshot laporan</h4>
												 * <p>
												 * Ketiga total ({@code totalTagihan}, {@code totalDibayar},
												 * {@code totalBelumDibayar}) diakumulasi sebagai {@code double} tanpa
												 * pembulatan; pembulatan hanya terjadi saat pemformatan tampilan.
												 * {@code belumDibayar} adalah selisih sederhana nominal dikurangi jumlah
												 * dibayar, dan dapat bernilai negatif bila terjadi kelebihan bayar.
												 * </p>
												 * <p>
												 * Sebuah baris dimasukkan ke {@code barisLaporan} — snapshot yang kelak
												 * dibaca PDF — hanya bila {@code totalValid != 0} dan item biayanya
												 * dikenali. Seperti diuraikan pada Javadoc kelas, {@code totalValid}
												 * secara aljabar sama dengan dua kali nominal, jadi syarat itu sebenarnya
												 * berarti "nominalnya bukan nol". Syarat yang sama juga dipakai untuk
												 * menyembunyikan dan melepas baris dari grid. Nama item pada snapshot
												 * diberi imbuhan ", Bulan X" untuk tagihan bulanan atau ", ke-N" untuk
												 * tagihan yang dibayar bertahap.
												 * </p>
												 * <p>
												 * Snapshot ini <b>sengaja</b> dipakai apa adanya oleh pencetakan PDF dan
												 * tidak dihitung ulang lewat jalur atau cache lain — komentar di kode
												 * mencatat bahwa perhitungan ulang pernah membuat layar menunjukkan lunas
												 * sementara PDF masih menyisakan tagihan.
												 * </p>
												 *
												 * <h4>Langkah 6 — perenderan</h4>
												 * <p>
												 * Bentuknya mengikuti bendera {@code vertical}: susunan groupbox bertumpuk
												 * untuk tampilan sempit, atau baris tabel empat kolom untuk tampilan
												 * lebar. Kedua bentuk menampilkan nominal sebelum diskon dengan coretan
												 * bila nominal akhir lebih kecil, memasang tautan riwayat revisi untuk
												 * jenis diskon yang berlaku (baik diskon kelompok mahasiswa maupun hingga
												 * tiga diskon pada {@link DetailKegiatan}), dan menandai tagihan bertahap.
												 * Persentase pelunasan dihitung {@code (dibayar * 100) / nominal} dengan
												 * penjaga pembagi positif, sehingga item pengurang bernominal negatif
												 * selalu menampilkan 0%.
												 * </p>
												 * <p>
												 * Setiap baris grid dititipi atribut {@code pengaturanPembayaranBulanan},
												 * {@code itemBiaya}, dan {@code detailBiaya} agar layar lain dapat
												 * membacanya kembali dari komponen. Blok ditutup dengan baris total dan,
												 * pada tampilan sempit, tombol aksi yang membuka halaman pembayaran.
												 * Penyembunyian blok mengikuti label filter: "Belum Lunas" menyembunyikan
												 * blok yang sisanya nol, sedangkan filter lain menyembunyikan blok yang
												 * tagihan, dibayar, dan sisanya sama-sama nol.
												 * </p>
												 *
												 * <h4>Sesi dan galat</h4>
												 * <p>
												 * Task ini membuka {@code childSession} mandiri beserta transaksinya,
												 * meng-{@code commit} di akhir jalur normal, dan me-{@code rollback} pada
												 * galat. Blok {@code finally} menutup sesi bertahap
												 * ({@code clear}/{@code disconnect}/{@code close}) dengan pola yang sama
												 * seperti {@link #closeOpenedSession(Session)}, hanya ditulis ulang secara
												 * sebaris. Seluruh galat ditangkap di dalam sehingga satu kombinasi yang
												 * bermasalah tidak menggagalkan kombinasi lain; akibatnya kombinasi
												 * tersebut hilang dari layar <b>dan</b> dari laporan tanpa pesan kepada
												 * pengguna.
												 * </p>
												 *
												 * @return peta hasil berisi kunci {@code "semuaKey"} (teks
												 *         {@code "<idJenisKegiatan>-<semester>"}), {@code "semuaVal"}
												 *         ({@code Object[]} berisi daftar item biaya, data cicilan,
												 *         {@link Kegiatan}, dan {@code barisLaporan}), serta {@code "ui"}
												 *         (potongan {@link Div} siap tempel). Peta <b>kosong</b> bila tidak
												 *         ada item biaya yang berlaku, bila tidak ada desktop hidup, atau
												 *         bila terjadi galat
												 */
												@Override
												public Map<String, Object> call() {
													Map<String, Object> resultDTO = new HashMap<String, Object>();
													Session childSession = null;
													boolean isChildTransaction = false;

													try {
														childSession = HibernateUtil.getSessionFactory().openSession();
														if (!childSession.getTransaction().isActive()) {
															childSession.beginTransaction();
															isChildTransaction = true;
														}

														VOMahasiswa mhsTarget = mhsUtama;
														if (mhsUtama instanceof Mahasiswa) {
															boolean isCalonAct = (ConstantValues.PENDAFTARAN_CALON_MAHASISWA != null
																	&& ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																			.getId().equals(jk.getId()));
															boolean isDaftarAct = (ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
																	&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																			.getId().equals(jk.getId()));

															if (isCalonAct || isDaftarAct) {
																Mahasiswa objMhs = (Mahasiswa) mhsUtama;
																if (objMhs.getBiodataCalonMahasiswa() != null) {
																	BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) ConstantValues
																			.ambil(BiodataCalonMahasiswa.class
																					.getName(),
																					objMhs.getBiodataCalonMahasiswa());
																	if (bcm != null)
																		mhsTarget = bcm;
																}
															}
														}

														Collection detailBiayas;
														if (mhsTarget instanceof Mahasiswa) {
															detailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(
																	(Mahasiswa) mhsTarget, fSmt, jk, refresh);
														} else {
													detailBiayas = new ArrayList();
															BiodataCalonMahasiswa calonMhs = (BiodataCalonMahasiswa) mhsTarget;
															Jurusan prodiLulus = calonMhs.getProdiLulus();
															Jurusan targetJur = (prodiLulus == null
																	|| prodiLulus.getId() == null)
																			? (calonMhs.getProdi1() == null
																					? calonMhs.getProdi2()
																					: calonMhs.getProdi1())
																			: prodiLulus;

															if (jk.getId().equals(
																	ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU
																			.getId())) {
														detailBiayas = PembayaranUtilHelper
																.getDetailBiayaCalonMahasiswa(calonMhs, jk,
																		targetJur, fSmt, refresh);
															} else if (jk.getId()
																	.equals(ConstantValues.PENDAFTARAN_CALON_MAHASISWA
																			.getId())) {
														detailBiayas = PembayaranUtilHelper
																.getDetailBiayaCalonMahasiswa(calonMhs, jk,
																		targetJur, refresh);
															}
														}

														int countPengaturanBulanan = (mhsTarget instanceof Mahasiswa)
																? PembayaranUtilHelper.countBulanan(childSession,
																		(Mahasiswa) mhsTarget, jk, fSmt, detailBiayas,
																		refresh, false)
																: PembayaranUtilHelper.countBulanan(childSession,
																		(BiodataCalonMahasiswa) mhsTarget, jk, fSmt,
																		detailBiayas, refresh, false);

														if (countPengaturanBulanan > 0) {
															detailBiayas = (mhsTarget instanceof Mahasiswa)
																	? PembayaranUtilHelper.getDetailBiayaMahasiswa(
																			(Mahasiswa) mhsTarget, fSmt, jk, "-1", true,
																			refresh)
																	: PembayaranUtil.getInstance()
																			.getPengaturanPembayaranSemua(
																					(BiodataCalonMahasiswa) mhsTarget,
																					childSession, fSmt, jk,
																					detailBiayas, refresh, false);
														}

														if (detailBiayas == null || detailBiayas.isEmpty()) {
															return resultDTO;
														}

														Kegiatan kegiatan = mhsTarget.ambilKegiatans(fSmt, jk, refresh);
														List<Object[]> dataCicilan = new ArrayList<Object[]>();

														List<CicilanPembayaran> cicilanPembayarans = mhsTarget
																.ambilCicilanPembayaran(kegiatan,
																		cicilanPembayaransTemp);
														for (CicilanPembayaran cp : cicilanPembayarans) {
															try {
																if (cp.getKegiatan() != null && cp.getKegiatan()
																		.getJenisKegiatan() != null) {
																	dataCicilan.add(new Object[] { cp.getId(),
																			cp.getItemBiaya().getId(),
																			cp.getPengaturanPembayaranBulanan() == null
																					? -1
																					: cp.getPengaturanPembayaranBulanan()
																							.getRealBulan(),
																			cp.getNilai(), cp.getKegiatan().getId(),
																			cp.getKegiatan().getJenisKegiatan() });
																}
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:566");
															}
														}

														Collection<DetailKegiatan> detailKegiatans = KegiatanPersistenceHelper
																.ambilDetailKegiatan(detailKegiatansTempGlobal,
																		kegiatan, refresh);

														Integer tahunAngkatan = 0;
														String semesterMulaiNama = "";
														ais.database.model.Jenjang jenjangObj = null;
														ais.database.model.JenisSeleksi seleksiObj = null;
														String programMhs = "";
														String identitasMhs = "";
														ais.database.model.GelombangPendaftaran gelombangPendaftaran = null;

														if (mhsTarget instanceof Mahasiswa) {
															Mahasiswa m = (Mahasiswa) mhsTarget;
															tahunAngkatan = m.getTahunangkatan() != null
																	? m.getTahunangkatan()
																	: 0;
															semesterMulaiNama = m.getSemesterMulai();
															jenjangObj = (m.getJurusan() != null)
																	? m.getJurusan().getJenjang()
																	: null;
															seleksiObj = m.getJenisSeleksi();
															programMhs = m.getProgram();
															identitasMhs = m.getNim();
															try {
																java.lang.reflect.Method mtd = m.getClass()
																		.getMethod("getGelombangPendaftaran");
																gelombangPendaftaran = (ais.database.model.GelombangPendaftaran) mtd
																		.invoke(m);
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:599");
															}
														} else {
															BiodataCalonMahasiswa c = (BiodataCalonMahasiswa) mhsTarget;
															tahunAngkatan = c.getTahun() != null ? c.getTahun() : 0;
															semesterMulaiNama = c.getSemesterMulai();
															jenjangObj = c.getJenjang();
															seleksiObj = c.getJenisSeleksi();
															programMhs = c.getProgram();
															identitasMhs = c.getNoRegistrasi();
															gelombangPendaftaran = c.getGelombangPendaftaran();
														}

														Integer smtMasuk = (mhsTarget instanceof Mahasiswa
																&& ((Mahasiswa) mhsTarget)
																		.getPindahKeKampusIniMasukSemester() != null)
																				? ((Mahasiswa) mhsTarget)
																						.getPindahKeKampusIniMasukSemester()
																				: 0;
														Integer thnAkademikMulai = Common.getTahunAkademik(fSmt,
																tahunAngkatan, smtMasuk, semesterMulaiNama);
														String taJadwalStr = thnAkademikMulai + "/"
																+ (thnAkademikMulai + 1);

														Date tanggalSekarang = WaktuUtil.getDate();
														java.io.Serializable[] serials = PembayaranUtil.getInstance()
																.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
																		tanggalSekarang, jk, jenjangObj, taJadwalStr,
																		("Ganjil".equalsIgnoreCase(semesterMulaiNama)
																				|| ais.database.model.Perkuliahan.GANJIL
																						.equalsIgnoreCase(
																								semesterMulaiNama)),
																		seleksiObj, programMhs, identitasMhs,
																		gelombangPendaftaran);

														JadwalPembayaran jadwal = (serials != null
																&& serials.length > 0) ? (JadwalPembayaran) serials[0]
																		: null;
														if (jadwal == null && kegiatan != null)
															jadwal = kegiatan.getJadwalPembayaran();
														JadwalPembayaran jdw = jadwal != null
																&& jadwal.getKhususUntukNim() != null
																&& jadwal.getKhususUntukNim()
																		.contains("," + identitasMhs + ",") ? jadwal
																				: null;

														// Kunci UI sesaat HANYA untuk update Label / ProgressBar
														if (desktop != null && desktop.isAlive()) {
															try {
																org.zkoss.zk.ui.Executions.activate(desktop);
																try {
																	Div localDiv = new Div();
																	MyLabelStyled2 labelBolder = new MyLabelStyled2(
																			jk.getNamaKegiatan() + ", Semester : "
																					+ fSmt);
																	if (!sederhana)
																		localDiv.appendChild(labelBolder);

																	Grid gripRincianBiaya = new Grid();
																	gripRincianBiaya
																			.setSclass(vertical ? "fgrid" : "dgrid");
																	gripRincianBiaya.setWidth("98%");
																	gripRincianBiaya.setParent(localDiv);

																	if (!vertical) {
																		Columns columns = new Columns();
																		columns.setParent(gripRincianBiaya);

																		new MyColumnConfig("Item Biaya")
																				.setParent(columns);

																		MyColumnConfig cTagihan = new MyColumnConfig(
																				"Tagihan");
																		cTagihan.setParent(columns);
																		cTagihan.setWidth("20%");
																		cTagihan.setAlign("right");

																		MyColumnConfig cDibayar = new MyColumnConfig(
																				"Dibayar");
																		cDibayar.setParent(columns);
																		cDibayar.setWidth("20%");
																		cDibayar.setAlign("right");

																		MyColumnConfig cSisa = new MyColumnConfig(
																				"Sisa");
																		cSisa.setParent(columns);
																		cSisa.setWidth("20%");
																		cSisa.setAlign("right");

																		MyColumnConfig cPersen = new MyColumnConfig(
																				"%");
																		cPersen.setParent(columns);
																		cPersen.setWidth("10%");
																		cPersen.setAlign("right");
																	}

																	Rows rowsGrid = new Rows();
																	gripRincianBiaya.appendChild(rowsGrid);

																	Double totalTagihan = 0.0;
																	Double totalDibayar = 0.0;
																	Double totalBelumDibayar = 0.0;
																	List<Map<String, Serializable>> barisLaporan = new ArrayList<Map<String, Serializable>>();

															resultDTO.put("semuaKey", jk.getId() + "-" + fSmt);
															resultDTO.put("semuaVal", new Object[] {
																	detailBiayas, dataCicilan, kegiatan, barisLaporan });

															for (Object o : detailBiayas) {
																		DetailBiaya tempdetailBiaya = null;
																		PengaturanPembayaranBulanan pb = null;

																		if (o instanceof DetailBiaya) {
																			tempdetailBiaya = (DetailBiaya) o;
																		} else if (o instanceof PengaturanPembayaranBulanan) {
																			pb = (PengaturanPembayaranBulanan) o;
																			if (pb != null)
																				tempdetailBiaya = pb.getDetailBiaya();
																		}

																		DetailKegiatan dk = kegiatan == null ? null
																				: (pb != null ? kegiatan
																						.ambilSatuDetailKegiatan(pb,
																								detailKegiatans)
																						: kegiatan
																								.ambilSatuDetailKegiatan(
																										tempdetailBiaya));

																		if (dk != null && dk.getBukanTagihan() != null
																				&& dk.getBukanTagihan()) {
																			continue;
																		}

																		Double jumlah = 0.0;
																		Double telahDibayar = 0.0;

																		if (pb != null) {
																			jumlah = (mhsTarget instanceof Mahasiswa)
																					? Kegiatan.ambilJumlahTagihan(dk,
																							pb.getDetailBiaya(),
																							kegiatan,
																							(Mahasiswa) mhsTarget, fSmt,
																							pb)
																					: Kegiatan.ambilJumlahTagihan(dk,
																							pb.getDetailBiaya(),
																							kegiatan, fSmt, pb);
																			if (jumlah == null)
																				jumlah = 0.0;

																			Date tanggalBayarItem = tanggalSekarang;
																			if (cicilanPembayarans != null) {
																				for (CicilanPembayaran cp : cicilanPembayarans) {
																					try {
																						if (cp.getItemBiaya() != null
																								&& cp.getItemBiaya()
																										.getId()
																										.equals(pb
																												.getDetailBiaya()
																												.getItemBiaya()
																												.getId())
																								&& pb.getDetailBiaya()
																										.getBayarKe()
																										.equals(cp
																												.getBayarKe())
																								&& cp.getKegiatan()
																										.getId()
																										.equals(kegiatan
																												.getId())) {
																							if (cp.getPengaturanPembayaranBulanan() != null) {
																								PengaturanPembayaranBulanan p = cp
																										.getPengaturanPembayaranBulanan();
																								if (p.getDetailBiaya()
																										.getItemBiaya()
																										.getId()
																										.equals(pb
																												.getDetailBiaya()
																												.getItemBiaya()
																												.getId())
																										&& p.getRealBulan()
																												.equals(pb
																														.getRealBulan())) {
																									tanggalBayarItem = cp
																											.getTanggal();
																								}
																							}
																						}
																					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:786");
																					}
																				}
																			}

																			Double hasilDenda = dk != null && (dk
																					.getBatalkanDenda() != null
																					&& dk.getBatalkanDenda()
																					|| jumlah.intValue() == 0)
																							? jumlah
																							: dk != null && dk
																									.getMenggunakanDendaCustom() != null
																									&& dk.getMenggunakanDendaCustom()
																											? jumlah
																											: pb.checkDenda(
																													jumlah,
																													tanggalBayarItem,
																													jdw,
																													jadwal == null
																															? null
																															: jadwal.getJenisKegiatan());

																			if (dk != null && dk
																					.getMenggunakanDendaCustom() != null
																					&& dk.getMenggunakanDendaCustom()) {
																				pb.setInfoDenda(
																						" Penambahan denda senilai "
																								+ Common.numberFormat
																										.get()
																										.format(dk
																												.getDendaCustom() != null
																														? dk.getDendaCustom()
																														: 0.0)
																								+ ".");
																			}

																			telahDibayar = VOMahasiswa
																					.hitungTotalCicilan(kegiatan, pb,
																							cicilanPembayarans);
																			if (telahDibayar == null)
																				telahDibayar = 0.0;

																			if (jumlah.intValue() == 0
																					&& telahDibayar.intValue() > 0)
																				jumlah = telahDibayar;
																			jumlah = hasilDenda.intValue() > jumlah
																					.intValue() ? hasilDenda : jumlah;

																			if (pb.getDetailBiaya() != null
																					&& pb.getDetailBiaya()
																							.getItemBiaya() != null
																					&& ItemBiaya.DIKALI_NILAI_MINUS
																							.equals(pb.getDetailBiaya()
																									.getItemBiaya()
																									.getPenghitungan())) {
																				jumlah = -Math.abs(jumlah);
																				telahDibayar = -Math.abs(telahDibayar);
																			}
																		} else if (tempdetailBiaya != null) {
																			jumlah = Kegiatan.ambilJumlahTagihan(dk,
																					kegiatan, tempdetailBiaya, refresh);
																			if (jumlah == null)
																				jumlah = 0.0;

																			Double hasilDenda = dk != null && dk
																					.getMenggunakanDendaCustom() != null
																					&& dk.getMenggunakanDendaCustom()
																							? jumlah
																							: dk != null && (dk
																									.getBatalkanDenda() != null
																									&& dk.getBatalkanDenda()
																									|| jumlah
																											.intValue() == 0)
																													? jumlah
																													: tempdetailBiaya
																															.checkDenda(
																																	jumlah,
																																	tanggalSekarang,
																																	jdw,
																																	jadwal == null
																																			? null
																																			: jadwal.getJenisKegiatan(),
																																	null);

																			if (dk != null && dk
																					.getMenggunakanDendaCustom() != null
																					&& dk.getMenggunakanDendaCustom()) {
																				tempdetailBiaya.setInfoDenda(
																						" Penambahan denda senilai "
																								+ Common.numberFormat
																										.get()
																										.format(dk
																												.getDendaCustom() != null
																														? dk.getDendaCustom()
																														: 0.0)
																								+ ".");
																			}

																			telahDibayar = VOMahasiswa
																					.hitungTotalCicilan(kegiatan,
																							tempdetailBiaya,
																							cicilanPembayarans);
																			if (telahDibayar == null)
																				telahDibayar = 0.0;

																			if (jumlah.intValue() == 0
																					&& telahDibayar.intValue() > 0)
																				jumlah = telahDibayar;
																			jumlah = hasilDenda.intValue() > jumlah
																					.intValue() ? hasilDenda : jumlah;

																			if (tempdetailBiaya.getItemBiaya() != null
																					&& ItemBiaya.DIKALI_NILAI_MINUS
																							.equals(tempdetailBiaya
																									.getItemBiaya()
																									.getPenghitungan())) {
																				jumlah = -Math.abs(jumlah);
																				telahDibayar = -Math.abs(telahDibayar);
																			}
																		}

																		totalTagihan += jumlah;
																		totalDibayar += telahDibayar;
																		Double belumDibayar = jumlah - telahDibayar;
																		totalBelumDibayar += belumDibayar;

																		// Simpan snapshot angka yang PERSIS dipakai layar. PDF Tagihan harus memakai
																		// snapshot ini dan tidak menghitung ulang lewat jalur/cache lain, karena itu
																		// pernah membuat layar lunas tetapi PDF masih menyisakan tagihan Rp350.000.
																		int totalValid = jumlah.intValue() + belumDibayar.intValue()
																				+ telahDibayar.intValue();
																		DetailBiaya dbLaporan = pb != null ? pb.getDetailBiaya() : tempdetailBiaya;
																		if (totalValid != 0 && dbLaporan != null && dbLaporan.getItemBiaya() != null) {
																			Map<String, Serializable> baris = new HashMap<String, Serializable>();
																			baris.put("kode", dbLaporan.getItemBiaya().getKode());
																			String namaItem = dbLaporan.getItemBiaya().getNama();
																			if (pb != null) {
																				namaItem += ", Bulan " + pb.getNamaBulan();
																			} else if (dbLaporan.getDetailSettingBiaya() != null
																					&& dbLaporan.getDetailSettingBiaya().getSettingBiaya() != null
																					&& dbLaporan.getDetailSettingBiaya().getSettingBiaya()
																							.getJumlahPembayaran() > 1) {
																				namaItem += ", ke-" + dbLaporan.getBayarKe();
																			}
																			baris.put("item_biaya", namaItem);
																			baris.put("biaya", jumlah);
																			baris.put("dibayar", telahDibayar);
																			baris.put("sisa", belumDibayar);
																			baris.put("nama_kegiatan_semester", jk.getNamaKegiatan() + "-" + fSmt);
																			baris.put("semester", Integer.valueOf(fSmt));
																			baris.put("tahun_ajaran", taJadwalStr);
																			baris.put("nama_kegiatan", jk.getNamaKegiatan());
																			barisLaporan.add(baris);
																		}

																		Row rowG = new MyRowStyled();
																		rowG.setVisible(!sederhana);
																		rowG.setValign("top");
																		if (pb != null) {
																			rowG.setAttribute(
																					"pengaturanPembayaranBulanan", pb);
																			rowG.setAttribute("itemBiaya",
																					pb.getDetailBiaya() != null
																							? pb.getDetailBiaya()
																									.getItemBiaya()
																							: null);
																			rowG.setAttribute("detailBiaya",
																					pb.getDetailBiaya());
																		} else if (tempdetailBiaya != null) {
																			rowG.setAttribute("itemBiaya",
																					tempdetailBiaya.getItemBiaya());
																			rowG.setAttribute("detailBiaya",
																					tempdetailBiaya);
																		}
																		rowG.setParent(rowsGrid);

																		if (totalValid == 0) {
																			rowG.setVisible(false);
																			rowG.detach();
																		}

																		Double persen = (jumlah > 0)
																				? (telahDibayar * 100.0) / jumlah
																				: 0.0;

																		if (vertical) {
																			MyGroupboxStyled gb = new MyGroupboxStyled();
																			gb.setWidth("85%");
																			if (pb != null)
																				gb.appendChild(new MyCaptionStyled(pb
																						.getDetailBiaya().getItemBiaya()
																						.getNama() + ", Bulan "
																						+ pb.getNamaBulan()));
																			else
																				gb.appendChild(new MyCaptionStyled(
																						tempdetailBiaya
																								.getKeterangan()));
																			gb.setParent(rowG);

																			Vbox vboxGrid = new Vbox();
																			vboxGrid.setParent(gb);
																			vboxGrid.appendChild(
																					new MyLabelAgakKecilBold(
																							"Tagihan : "
																									+ Common.numberFormat
																											.get()
																											.format(jumlah)));

																			if (pb != null && jumlah.intValue() < pb
																					.getNominal().intValue()) {
																				Label tagDiskon = new Label(
																						Common.numberFormat.get()
																								.format(pb
																										.getNominal()));
																				tagDiskon.setWidth("100%");
																				tagDiskon.setStyle(
																						"text-decoration: line-through;");
																				vboxGrid.appendChild(tagDiskon);
																			} else if (pb == null
																					&& tempdetailBiaya != null
																					&& jumlah
																							.intValue() < tempdetailBiaya
																									.getNilaiBiaya()
																									.intValue()) {
																				Label tagDiskon = new Label(
																						Common.numberFormat.get()
																								.format(tempdetailBiaya
																										.getNilaiBiaya()));
																				tagDiskon.setWidth("100%");
																				tagDiskon.setStyle(
																						"text-decoration: line-through;");
																				vboxGrid.appendChild(tagDiskon);
																			}

																			DetailBiaya referenceDb = pb != null
																					? pb.getDetailBiaya()
																					: tempdetailBiaya;

																			if (pb == null && kegiatan != null
																					&& kegiatan.getMahasiswa() != null
																					&& referenceDb != null
																					&& referenceDb
																							.getItemBiaya() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtMulai() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtMulai() <= kegiatan
																									.getSemster()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtSampai() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtSampai() >= kegiatan
																									.getSemster()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa() != null
																					&& !kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa()
																							.ambilItemBiayaIds()
																							.isEmpty()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa()
																							.ambilItemBiayaIds()
																							.contains(referenceDb
																									.getItemBiaya()
																									.getId())) {
																				ais.action.master.helper.RevisiHelper
																						.createNewRevisi(
																								JenisDiskonMahasiswa.class,
																								kegiatan.getMahasiswa()
																										.getKelompokMahasiswa()
																										.getJenisDiskonMahasiswa(),
																								kegiatan.getMahasiswa()
																										.getKelompokMahasiswa()
																										.getJenisDiskonMahasiswa()
																										.getNama())
																						.setParent(vboxGrid);
																			} else {
																				if (dk != null && dk.adaDiskon()) {
																					if (dk.getDiskonMahasiswaData() != null
																							&& dk.getDiskonMahasiswaData()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																					if (dk.getDiskonMahasiswaData2() != null
																							&& dk.getDiskonMahasiswaData2()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData2()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																					if (dk.getDiskonMahasiswaData3() != null
																							&& dk.getDiskonMahasiswaData3()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData3()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																				}
																			}

																			if (referenceDb != null && referenceDb
																					.getDetailSettingBiaya() != null
																					&& referenceDb
																							.getDetailSettingBiaya()
																							.getSettingBiaya() != null
																					&& referenceDb
																							.getDetailSettingBiaya()
																							.getSettingBiaya()
																							.getJumlahPembayaran() > 1) {
																				ais.action.master.helper.RevisiHelper
																						.createNewRevisi(
																								DetailSettingBiaya.class,
																								referenceDb
																										.getDetailSettingBiaya(),
																								"Tagihan ke-"
																										+ referenceDb
																												.getBayarKe())
																						.setParent(vboxGrid);
																			}

																			vboxGrid.appendChild(
																					new MyLabelAgakKecilBold(
																							"Dibayar : "
																									+ Common.numberFormat
																											.get()
																											.format(telahDibayar)));
																			vboxGrid.appendChild(
																					new MyLabelAgakKecilBold("Sisa : "
																							+ Common.numberFormat.get()
																									.format(belumDibayar)));
																			vboxGrid.appendChild(
																					new MyLabelAgakKecilBold("Persen : "
																							+ Common.numberFormat.get()
																									.format(persen)
																							+ "%"));
																		} else {
																			if (dk != null) {
																				ais.action.master.helper.RevisiHelper
																						.createNewRevisi(
																								DetailKegiatan.class,
																								dk,
																								pb != null ? (pb
																										.getDetailBiaya()
																										.getItemBiaya()
																										.getNama()
																										+ ", Bulan "
																										+ pb.getNamaBulan())
																										: tempdetailBiaya
																												.getKeterangan())
																						.setParent(rowG);
																			} else {
																				new Label(pb != null
																						? (pb.getDetailBiaya()
																								.getItemBiaya()
																								.getNama() + ", Bulan "
																								+ pb.getNamaBulan())
																						: tempdetailBiaya
																								.getKeterangan())
																						.setParent(rowG);
																			}

																			Vbox vboxGrid = new Vbox();
																			vboxGrid.setWidth("100%");
																			vboxGrid.setPack("end");
																			vboxGrid.setAlign("end");
																			rowG.appendChild(vboxGrid);
																			vboxGrid.appendChild(
																					new Label(Common.numberFormat.get()
																							.format(jumlah)));

																			if (pb != null && jumlah.intValue() < pb
																					.getNominal().intValue()) {
																				Label tagDiskon = new Label(
																						Common.numberFormat.get()
																								.format(pb
																										.getNominal()));
																				tagDiskon.setWidth("100%");
																				tagDiskon.setStyle(
																						"text-decoration: line-through;");
																				vboxGrid.appendChild(tagDiskon);
																			} else if (pb == null
																					&& tempdetailBiaya != null
																					&& jumlah
																							.intValue() < tempdetailBiaya
																									.getNilaiBiaya()
																									.intValue()) {
																				Label tagDiskon = new Label(
																						Common.numberFormat.get()
																								.format(tempdetailBiaya
																										.getNilaiBiaya()));
																				tagDiskon.setWidth("100%");
																				tagDiskon.setStyle(
																						"text-decoration: line-through;");
																				vboxGrid.appendChild(tagDiskon);
																			}

																			DetailBiaya referenceDb = pb != null
																					? pb.getDetailBiaya()
																					: tempdetailBiaya;

																			if (pb == null && kegiatan != null
																					&& kegiatan.getMahasiswa() != null
																					&& referenceDb != null
																					&& referenceDb
																							.getItemBiaya() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtMulai() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtMulai() <= kegiatan
																									.getSemster()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtSampai() != null
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getSmtSampai() >= kegiatan
																									.getSemster()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa() != null
																					&& !kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa()
																							.ambilItemBiayaIds()
																							.isEmpty()
																					&& kegiatan.getMahasiswa()
																							.getKelompokMahasiswa()
																							.getJenisDiskonMahasiswa()
																							.ambilItemBiayaIds()
																							.contains(referenceDb
																									.getItemBiaya()
																									.getId())) {
																				ais.action.master.helper.RevisiHelper
																						.createNewRevisi(
																								JenisDiskonMahasiswa.class,
																								kegiatan.getMahasiswa()
																										.getKelompokMahasiswa()
																										.getJenisDiskonMahasiswa(),
																								kegiatan.getMahasiswa()
																										.getKelompokMahasiswa()
																										.getJenisDiskonMahasiswa()
																										.getNama())
																						.setParent(vboxGrid);
																			} else {
																				if (dk != null && dk.adaDiskon()) {
																					if (dk.getDiskonMahasiswaData() != null
																							&& dk.getDiskonMahasiswaData()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																					if (dk.getDiskonMahasiswaData2() != null
																							&& dk.getDiskonMahasiswaData2()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData2()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																					if (dk.getDiskonMahasiswaData3() != null
																							&& dk.getDiskonMahasiswaData3()
																									.getJenisDiskonMahasiswa() != null)
																						ais.action.master.helper.RevisiHelper
																								.createNewRevisi(
																										DetailKegiatan.class,
																										dk,
																										dk.getDiskonMahasiswaData3()
																												.getJenisDiskonMahasiswa()
																												.getNama())
																								.setParent(vboxGrid);
																				}
																			}

																			if (referenceDb != null && referenceDb
																					.getDetailSettingBiaya() != null
																					&& referenceDb
																							.getDetailSettingBiaya()
																							.getSettingBiaya() != null
																					&& referenceDb
																							.getDetailSettingBiaya()
																							.getSettingBiaya()
																							.getJumlahPembayaran() > 1) {
																				ais.action.master.helper.RevisiHelper
																						.createNewRevisi(
																								DetailSettingBiaya.class,
																								referenceDb
																										.getDetailSettingBiaya(),
																								"Tagihan ke-"
																										+ referenceDb
																												.getBayarKe())
																						.setParent(vboxGrid);
																			}

																			rowG.appendChild(new MyLabelBoldAja(
																					Common.numberFormat.get()
																							.format(telahDibayar)));
																			rowG.appendChild(new MyLabelBoldAja(
																					Common.numberFormat.get()
																							.format(belumDibayar)));
																			rowG.appendChild(new MyLabelAgakKecilBold(
																					Common.numberFormat.get()
																							.format(persen) + "%"));
																		}
																	}

																	if (vertical) {
																		Row foot = new MyRowStyled();
																		rowsGrid.appendChild(foot);
																		MyGroupboxStyled gb = new MyGroupboxStyled();
																		gb.setWidth("85%");
																		gb.setParent(foot);
																		Vbox vboxGrid = new Vbox();
																		vboxGrid.setParent(gb);

																		if (!sederhana)
																			vboxGrid.appendChild(
																					new MyLabelBold("Total"));
																		else
																			vboxGrid.appendChild(new MyLabelBold(
																					jk.getNamaKegiatan()));

																		Double persenTotal = (totalTagihan > 0)
																				? (totalDibayar * 100.0) / totalTagihan
																				: 0.0;
																		vboxGrid.appendChild(new MyLabelAgakKecilBold(
																				"Tagihan : " + Common.numberFormat.get()
																						.format(totalTagihan)));
																		vboxGrid.appendChild(new MyLabelAgakKecilBold(
																				"Dibayar : " + Common.numberFormat.get()
																						.format(totalDibayar)));
																		vboxGrid.appendChild(new MyLabelAgakKecilBold(
																				"Sisa : " + Common.numberFormat.get()
																						.format(totalBelumDibayar)));
																		vboxGrid.appendChild(
																				new MyLabelAgakKecilBold("Persen : "
																						+ Common.numberFormat.get()
																								.format(persenTotal)
																						+ "%"));

																		if (sederhana && totalTagihan.intValue() == 0)
																			foot.setVisible(false);

																		MyToolbarbuttonConfig btnLihat = new MyToolbarbuttonConfig(
																				gateway && totalBelumDibayar > 0.1
																						? "Bayar Sekarang"
																						: "Tagihan dan Pembayaran",
																				gateway && totalBelumDibayar > 0.1
																						? "/img/money-wallet-icon.png"
																						: "/img/album.png");
																		vboxGrid.appendChild(btnLihat);

																		btnLihat.addEventListener("onClick",
																				new EventListener() {
																					@Override
																					public void onEvent(Event arg0)
																							throws Exception {
																						String url = "/common/daftarulang_mahasiswa_lama.zul?mahasiswa="
																								+ mahasiswa.getId()
																								+ "&jk=" + jk.getId()
																								+ "&smt=" + fSmt;
																						Common.displayWindow(url, false,
																								"1500px", "95%",
																								getThis(),
																								"Tagihan dan Proses Pembayaran");
																					}
																				});
																	} else {
																		Double persenTotal = (totalTagihan > 0)
																				? (totalDibayar * 100.0) / totalTagihan
																				: 0.0;
																		Foot foot = new Foot();
																		gripRincianBiaya.appendChild(foot);

																		Footer footer = new Footer();
																		foot.appendChild(footer);
																		footer.appendChild(new MyLabelBold("Total"));

																		footer = new Footer();
																		foot.appendChild(footer);
																		footer.setAlign("right");
																		MyLabelBold lblTotalTagihan = new MyLabelBold(
																				Common.numberFormat.get()
																						.format(totalTagihan));
																		lblTotalTagihan.setStyle("text-align: right;");
																		footer.appendChild(lblTotalTagihan);

																		footer = new Footer();
																		foot.appendChild(footer);
																		footer.setAlign("right");
																		MyLabelBold lblTotalDibayar = new MyLabelBold(
																				Common.numberFormat.get()
																						.format(totalDibayar));
																		lblTotalDibayar.setStyle("text-align: right;");
																		footer.appendChild(lblTotalDibayar);

																		footer = new Footer();
																		foot.appendChild(footer);
																		footer.setAlign("right");
																		MyLabelBold lblTotalBelum = new MyLabelBold(
																				Common.numberFormat.get()
																						.format(totalBelumDibayar));
																		lblTotalBelum.setStyle("text-align: right;");
																		footer.appendChild(lblTotalBelum);

																		footer = new Footer(Common.numberFormat.get()
																				.format(persenTotal) + "%");
																		foot.appendChild(footer);
																		footer.setAlign("right");

																		if (label.equalsIgnoreCase("Belum Lunas")) {
																			labelBolder.setVisible(
																					totalBelumDibayar.intValue() != 0);
																			gripRincianBiaya.setVisible(
																					totalBelumDibayar.intValue() != 0);
																			if (sederhana && totalBelumDibayar
																					.intValue() == 0)
																				foot.setVisible(false);
																		} else {
																			labelBolder.setVisible(
																					totalTagihan.intValue() != 0
																							|| totalDibayar
																									.intValue() != 0
																							|| totalBelumDibayar
																									.intValue() != 0);
																			gripRincianBiaya.setVisible(
																					totalTagihan.intValue() != 0
																							|| totalDibayar
																									.intValue() != 0
																							|| totalBelumDibayar
																									.intValue() != 0);
																			if (sederhana
																					&& (totalTagihan.intValue() == 0
																							&& totalDibayar
																									.intValue() == 0
																							&& totalBelumDibayar
																									.intValue() == 0))
																				foot.setVisible(false);
																		}
																	}

																	resultDTO.put("ui", localDiv);
																	int curr = completedTasks.incrementAndGet();
																	int prg = (curr * 100) / totalTasks;
																	lblTitle.setValue("Proses " + prg + "%");
																	progressMeter.setValue(prg);
																	lblInfo.setValue("Selesai: " + jk.getNamaKegiatan()
																			+ " Smt " + fSmt);
																	lblCounter.setValue(
																			curr + " dari " + totalTasks + " Selesai");

																} finally {
																	org.zkoss.zk.ui.Executions.deactivate(desktop);
																}
																	} catch (org.zkoss.zk.ui.DesktopUnavailableException due) {
																		// Window/browser ditutup ketika worker masih selesai menghitung. Ini kondisi
																		// normal, bukan error aplikasi dan tidak perlu memenuhi tabel audit.
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1450");
															}
														}

														if (isChildTransaction) {
															childSession.getTransaction().commit();
														}

													} catch (Exception e) {
														if (isChildTransaction && childSession != null
																&& childSession.getTransaction().isActive()) {
															try {
																childSession.getTransaction().rollback();
															} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1463");
															}
														}
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TagihanUIBuilder.java:1466");
													} finally {
														if (childSession != null) {
															try {
																if (childSession.isOpen())
																	childSession.clear();
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1472");
															}
															try {
																childSession.disconnect();
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1476");
															}
															try {
																if (childSession.isOpen())
																	childSession.close();
															} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1481");
															}
														}
													}
													return resultDTO;
												}
											});
										}
									}

									try {
									List<Future<Map<String, Object>>> futures = executor.invokeAll(tasks);
									executor.shutdown();

										try {
											if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
												executor.shutdownNow();
											}
										} catch (InterruptedException e) {
											executor.shutdownNow();
											Thread.currentThread().interrupt();
										}

									/*
									 * Worker paralel tidak boleh menulis langsung ke TreeMap "semua".
									 * Selain TreeMap tidak thread-safe, snapshot sebelumnya dimasukkan sebelum
									 * barisLaporan selesai diisi. Akibatnya Cetak Semua Tagihan kadang membaca
									 * snapshot kosong/sebagian walaupun rincian di layar sebenarnya tersedia.
									 * Kumpulkan seluruh hasil Future lebih dahulu, lalu publikasikan secara atomik.
									 */
									List<Map<String, Object>> hasilSelesai = new ArrayList<Map<String, Object>>();
									TreeMap<String, Object[]> semuaSelesai = new TreeMap<String, Object[]>();
									for (Future<Map<String, Object>> future : futures) {
										if (Thread.currentThread().isInterrupted()) {
											break;
										}
										Map<String, Object> res = future.get();
										if (res != null && !res.isEmpty()) {
											hasilSelesai.add(res);
											Object semuaKey = res.get("semuaKey");
											Object semuaVal = res.get("semuaVal");
											if (semuaKey instanceof String && semuaVal instanceof Object[]) {
												semuaSelesai.put((String) semuaKey, (Object[]) semuaVal);
											}
										}
									}
									synchronized (semua) {
										semua.clear();
										semua.putAll(semuaSelesai);
									}

									if (desktop != null && desktop.isAlive()) {
										try {
											org.zkoss.zk.ui.Executions.activate(desktop);
											try {
												boolean adaInfopmbayaranB = false;
												for (Map<String, Object> res : hasilSelesai) {
													if (res != null && !res.isEmpty()) {
															Div localUi = (Div) res.get("ui");
															if (localUi != null && !localUi.getChildren().isEmpty()) {
																List<org.zkoss.zk.ui.Component> children = new ArrayList<org.zkoss.zk.ui.Component>(
																		localUi.getChildren());
																for (org.zkoss.zk.ui.Component c : children) {
																	myDiv.appendChild(c);
																}
																adaInfopmbayaranB = true;
															}
														}
													}

													if (!adaInfopmbayaranB) {
														MyGroupboxStyled gb = new MyGroupboxStyled();
														gb.setParent(myDiv);
														gb.setWidth("85%");
														gb.appendChild(new MyCaptionStyled("Info Pembayaran"));
														Label a = new Label(
																ais.common.Common.getBahasaConfig("Untuk saat ini, belum ada Info Pembayaran yang bisa ditampilkan"));
														a.setStyle("font-size:12px;font-weight: bolder;color:red;");
														gb.appendChild(a);
													}
												} finally {
													org.zkoss.zk.ui.Executions.deactivate(desktop);
												}
											} catch (org.zkoss.zk.ui.DesktopUnavailableException due) {
												// Tab keburu diclose
											} catch (InterruptedException e) {
												Thread.currentThread().interrupt();
											} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1539");
											}
										}
									} finally {
										if (executor != null && !executor.isShutdown()) {
											executor.shutdownNow();
										}
									}

									detailKegiatansTempGlobal.clear();
									cicilanPembayaransTemp.clear();

									if (isParentTransaction && parentSession != null
											&& parentSession.getTransaction().isActive()) {
										parentSession.getTransaction().commit();
									}

								} catch (InterruptedException ex) {
									Thread.currentThread().interrupt();
								} catch (Exception e) {
									if (isParentTransaction && parentSession != null
											&& parentSession.getTransaction().isActive()) {
										try {
											parentSession.getTransaction().rollback();
										} catch (Exception eq) { ais.common.ErrorAuditUtil.record(eq, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1562");
										}
									}
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TagihanUIBuilder.java:1565");
								} finally {
									closeOpenedSession(parentSession);
								}

								return null;
							}
						}, new ais.common.AsyncTaskManager.UITask() {
							/** Dijalankan kembali di UI thread setelah {@code doInBackground} selesai: menandai progres 100% lalu melepas kontainer progres inline dan indikator mengambang. */
							@Override
							public void updateUI(Object backgroundResult) throws Exception {
								try {
									ais.ui.util.KeuanganDashboardEnhanceUtil.showFloatingProgress("tagihan_ui_builder",
										"Tagihan Siap", "Data tagihan dan pembayaran berhasil ditampilkan.", 100);
								} finally {
									// Hapus kontainer progress inline dari myDiv setelah selesai
									if (progressContainer != null) {
										progressContainer.detach();
									}
									ais.ui.util.KeuanganDashboardEnhanceUtil.hideFloatingProgress("tagihan_ui_builder");
								}
							}
						});
			}

			/** Kontrak {@link EventListener}: pemanggilan langsung ({@code arg0=null}, mis. dari akhir {@link #loadTagihan}) memuat segera; pemanggilan dari event UI (perubahan kombobox) ditunda lewat timer default agar state komponen (mis. item terpilih) sudah settle sebelum dibaca. */
			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 == null) {
					load();
				} else {
					Common.createDefaultTimer(new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							load();
						}
					});
				}
			}
		};

		if (semesterSampai != null) {
			semesterSampai.addEventListener("onChange", new EventListener() {
				public void onEvent(Event e) {
					if (actionInstance != null && semesterSampai.getSelectedItem() != null) {
						actionInstance.setLastSelectedSmtSampai((Integer) semesterSampai.getSelectedItem().getValue());
					}
					try {
						itemBiayaEventListener.onEvent(e);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1612");
					}
				}
			});
		}
		if (semesterMulai != null) {
			semesterMulai.addEventListener("onChange", new EventListener() {
				public void onEvent(Event e) {
					if (actionInstance != null && semesterMulai.getSelectedItem() != null) {
						actionInstance.setLastSelectedSmtMulai((Integer) semesterMulai.getSelectedItem().getValue());
					}
					try {
						itemBiayaEventListener.onEvent(e);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1625");
					}
				}
			});
		}
		if (jenisKegiatan != null) {
			jenisKegiatan.addEventListener("onChange", new EventListener() {
				public void onEvent(Event e) {
					if (actionInstance != null && jenisKegiatan.getSelectedItem() != null) {
						actionInstance.setLastSelectedJkLabel(jenisKegiatan.getSelectedItem().getLabel());
						actionInstance.setLastSelectedJenisKegiatan(
								(JenisKegiatan) jenisKegiatan.getSelectedItem().getValue());
					}
					try {
						itemBiayaEventListener.onEvent(e);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TagihanUIBuilder.java:1640");
					}
				}
			});
		}

		itemBiayaEventListener.onEvent(null);
		return jenisKegiatan;
	}
}
