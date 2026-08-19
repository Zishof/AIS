package ais.action.master.asset;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.PembayaranDpMasterAssetHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.master.sop.helper.SopUtil;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.asset.DokumenPenyediaAsset;
import ais.database.model.asset.JenisPembayaranBarang;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PembayaranDpMasterAsset;
import ais.database.model.asset.PembayaranDpMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>PembayaranDpMasterAssetAction — Aksi Pembayaran Uang Muka (DP) Pengadaan Aset</h3>
 *
 * <p><b>Untuk apa:</b><br>
 * Kelas ini merupakan kontroler ZKoss utama yang mengelola seluruh siklus hidup transaksi
 * Pembayaran Uang Muka (Down Payment / DP) kepada vendor atau penyedia barang dan jasa dalam
 * modul pengadaan aset organisasi. Modul ini digunakan oleh staf keuangan dan bagian
 * pengadaan untuk mencatat, menyetujui, membatalkan persetujuan, mencetak, dan menghapus
 * pembayaran DP yang terkait dengan satu atau lebih pesanan pengadaan aset (PemesananPengadaanMasterAsset).
 * Setiap transaksi pembayaran DP berisi informasi penyedia, nomor kode unik, tanggal pembuatan,
 * jenis pembayaran, total nilai yang dibayar, serta status persetujuan yang dicatat melalui
 * mekanisme persetujuan berbasis pengguna (disetujuiOleh dan tanggalPersetujuan). Setelah
 * persetujuan diberikan, sistem secara otomatis mencetak dokumen PDF pembayaran DP sebagai
 * bukti resmi transaksi.</p>
 *
 * <p><b>Cara kerja:</b><br>
 * Kelas ini mewarisi {@code GenericAutowireComposer} dari ZKoss dan mengimplementasikan
 * antarmuka {@code FormSop} untuk integrasi alur kerja SOP. Komponen-komponen UI (grid,
 * paging, textbox pencarian, datebox rentang tanggal) di-wire secara otomatis oleh ZKoss
 * melalui mekanisme auto-wire saat halaman dimuat. Alur kerja utama adalah sebagai berikut:
 * <ol>
 *   <li>Saat halaman dimuat, {@code doAfterCompose} memvalidasi sesi, memeriksa hak akses
 *       pengguna, menginisialisasi rentang tanggal default (6 bulan ke belakang hingga besok),
 *       serta menyiapkan tombol tambah berdasarkan hak akses CREATE.</li>
 *   <li>Data ditampilkan melalui {@code onSearchDefault} yang mengambil data dari database
 *       menggunakan kriteria Hibernate, lalu dirender oleh inner class
 *       {@code PembayaranDpMasterAssetRenderer}.</li>
 *   <li>Penambahan dan perubahan data dilakukan melalui form modal yang disiapkan oleh
 *       metode {@code init}, yang menampilkan form lengkap termasuk pemilihan penyedia,
 *       tanggal, jenis pembayaran, keterangan, dan daftar detail pesanan (dikelola oleh
 *       {@code PembayaranDpMasterAssetHelper}).</li>
 *   <li>Penyimpanan data dikerjakan oleh {@code onSave} yang memvalidasi input, menyimpan
 *       header pembayaran DP, menyimpan detail per pesanan pengadaan, memperbarui nilai
 *       total yang telah dibayar pada masing-masing pesanan, serta membuat entri
 *       DaftarPengajuanTransfer bila jenis pembayaran membutuhkannya.</li>
 *   <li>Persetujuan dan pembatalan persetujuan dilakukan langsung dari baris grid, dengan
 *       konfirmasi dialog terlebih dahulu.</li>
 *   <li>Penghapusan data menghapus detail pembayaran dan disposisi SOP terkait sebelum
 *       menghapus header, dengan pengecekan integritas referensial.</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b><br>
 * Kelas ini berjalan pada thread utama ZKoss (event thread). Semua operasi database
 * dilakukan secara sinkron menggunakan {@code HibernateUtil.currentSession()} atau
 * {@code HibernateUtil.currentNativeSession()}. Timer default ({@code Common.createDefaultTimer})
 * digunakan untuk menunda operasi cetak PDF sehingga commit database dapat selesai terlebih
 * dahulu sebelum data dibaca ulang untuk laporan. Tidak ada operasi multi-thread eksplisit
 * dalam kelas ini; penggunaan ThreadLocal untuk format tanggal dan angka dikelola oleh
 * {@code Common} secara aman.</p>
 *
 * <p><b>Pemeliharaan:</b><br>
 * Saat menambah kolom baru pada entitas {@code PembayaranDpMasterAsset}, perbarui metode
 * {@code form} untuk menampilkan field baru, metode {@code onSave} untuk validasi dan
 * penyimpanan, serta metode {@code parameter} untuk pemetaan ke template laporan.
 * Pastikan selalu menambahkan DDL ALTER TABLE pada skema {@code public} dan
 * {@code new_audit} (Envers) secara manual karena {@code hbm2ddl} diset {@code none}.
 * Nomor kode unik dikelola melalui {@code NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA}
 * dan {@code KodeUnikUtil.pastikanUnik} untuk menghindari duplikasi pasca penghapusan data.</p>
 *
 * @author AIS Development Team
 * @version 1.0
 * @see PembayaranDpMasterAsset
 * @see PembayaranDpMasterAssetHelper
 * @see FormSop
 */
public class PembayaranDpMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Versi serialisasi kelas untuk keperluan serialisasi Java.
	 * Nilai ini tetap konstan selama tidak ada perubahan struktur kelas yang tidak kompatibel.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal yang digunakan untuk form tambah dan ubah data pembayaran DP. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar pembayaran DP beserta kontrol aksi per baris. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada daftar pembayaran DP. */
	private Paging paging;

	/** Textbox pencarian berdasarkan kode pembayaran DP. */
	private Textbox searchkode;

	/** Textbox pencarian berdasarkan nama atau kode penyedia/vendor. */
	private Textbox searchpenyedia;

	/** Textbox pencarian berdasarkan keterangan pembayaran DP. */
	private Textbox searchketerangan;

	/** Datebox filter tanggal awal rentang tanggal pembuatan pembayaran DP. */
	private MyDatebox start;

	/** Datebox filter tanggal akhir rentang tanggal pembuatan pembayaran DP. */
	private MyDatebox end;

	/** Label yang menampilkan kode unik pembayaran DP yang sedang diedit atau dibuat. */
	private Label kode;

	/** Textbox untuk keterangan atau catatan terkait pembayaran DP. */
	private MyTextbox keterangan;

	/** Datebox tanggal pembuatan pembayaran DP pada form tambah/ubah. */
	private MyDatebox tanggalPembuatan;

	/** Flag yang menunjukkan apakah pengguna memiliki hak akses UPDATE (ubah data). */
	private boolean edit = false;

	/** Flag yang menunjukkan apakah pengguna memiliki hak akses DELETE (hapus data). */
	private boolean delete = false;

	/** Flag yang menunjukkan apakah pengguna memiliki hak akses APPROVE (setujui). */
	private boolean approve = false;

	/** Flag yang menunjukkan apakah pengguna memiliki hak akses REJECT (batalkan persetujuan). */
	private boolean reject = false;

	/** Referensi ke entitas {@code PembayaranDpMasterAsset} yang sedang diproses oleh form. */
	private PembayaranDpMasterAsset pembayaranDpMasterAsset;

	/** Tombol tambah data pembayaran DP baru, visibilitasnya bergantung pada hak akses CREATE. */
	private MyToolbarbuttonConfig add;

	/** Grid yang menampilkan daftar detail pesanan pengadaan yang dapat dipilih untuk pembayaran DP. */
	private MyGrid gridMasterAsset;

	/** Disposisi SOP yang terkait dengan pembayaran DP, digunakan jika alur SOP aktif. */
	private DisposisiSop disposisiSop = null;

	/**
	 * Flag yang menunjukkan apakah mode yang aktif adalah mode persetujuan langsung.
	 * Jika {@code true}, form akan langsung menyimpan data dengan status disetujui.
	 */
	private boolean persetujuan = false;

	/** Komponen banbox (combo dengan pencarian) untuk memilih penyedia/vendor aset. */
	private AmbilDataPenyediaAssetBanbox penyediaAsset;

	/** Helper yang mengelola detail pembayaran DP pada form, termasuk grid pemilihan pesanan. */
	private PembayaranDpMasterAssetHelper pembayaranDpMasterAssetHelper;

	/** Combobox untuk memilih jenis pembayaran barang (tunai, transfer, dll). */
	private Combobox jenisPembayaranBarang;

	/** Checkbox filter untuk menampilkan data yang belum disetujui. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter untuk menampilkan data yang sudah disetujui. */
	private MyCheckboxConfig disetujui;

	/**
	 * <p><b>Tujuan:</b><br>
	 * Konstruktor default tanpa argumen yang diperlukan oleh ZKoss untuk instansiasi otomatis
	 * kontroler saat halaman ZUL dimuat. Tidak melakukan inisialisasi apapun; semua inisialisasi
	 * dilakukan di {@code doAfterCompose}.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Konstruktor kosong — ZKoss memanggil konstruktor ini saat me-wire kontroler ke komponen
	 * ZUL root. Semua field diinisialisasi dengan nilai default Java (null/false/0).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menambahkan logika inisialisasi di sini. Gunakan {@code doAfterCompose} untuk
	 * inisialisasi yang bergantung pada komponen UI yang sudah ter-wire.</p>
	 */
	public PembayaranDpMasterAssetAction() {

	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Konstruktor dengan parameter yang memungkinkan instansiasi kontroler dalam mode
	 * persetujuan langsung. Mode ini digunakan ketika pembayaran DP langsung dibuat sekaligus
	 * disetujui oleh pengguna yang berwenang, misalnya melalui alur SOP atau pemanggilan
	 * programatis dari modul lain.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menyimpan nilai parameter {@code persetujuan} ke field instance. Ketika nilai {@code true},
	 * metode {@code onSave} akan secara otomatis mengisi field {@code disetujuiOleh} dengan
	 * pengguna saat ini dan {@code tanggalPersetujuan} dengan waktu saat ini, sehingga
	 * data langsung tersimpan dalam status disetujui tanpa perlu tindakan persetujuan terpisah.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param persetujuan {@code true} jika kontroler dijalankan dalam mode persetujuan langsung,
	 *                    {@code false} untuk mode normal tanpa persetujuan otomatis.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan pemanggil dari modul SOP atau lainnya menggunakan konstruktor ini dengan
	 * nilai yang tepat agar status persetujuan tersimpan dengan benar.</p>
	 */
	public PembayaranDpMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode hook ZKoss yang dipanggil sebelum komponen halaman di-compose. Digunakan untuk
	 * melakukan pemeriksaan keamanan (security check) sebelum halaman ditampilkan kepada
	 * pengguna, memastikan hanya pengguna yang terautentikasi yang dapat mengakses halaman ini.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} untuk memverifikasi bahwa sesi pengguna aktif
	 * dan valid. Jika pemeriksaan gagal, ZKoss akan mengarahkan pengguna ke halaman login atau
	 * menampilkan pesan error sesuai konfigurasi. Setelah pemeriksaan, memanggil implementasi
	 * superclass untuk melanjutkan proses compose normal.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param page     Objek halaman ZKoss yang sedang di-compose.
	 * @param parent   Komponen induk dari komponen yang sedang di-compose.
	 * @param compInfo Informasi metadata komponen dari ZKoss.
	 *
	 * <p><b>Return:</b></p>
	 * @return {@code ComponentInfo} dari superclass yang berisi informasi konfigurasi komponen.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, {@code Common.doCheckSecurity()} akan melempar exception atau
	 * melakukan redirect sehingga eksekusi tidak berlanjut ke superclass.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jangan menghapus pemanggilan {@code Common.doCheckSecurity()} karena ini adalah
	 * garis pertahanan pertama untuk keamanan akses halaman.</p>
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode inisialisasi utama yang dipanggil oleh ZKoss setelah seluruh komponen UI pada
	 * halaman berhasil di-compose dan di-wire ke field kontroler. Metode ini menyiapkan
	 * semua status awal halaman, termasuk validasi sesi, pengecekan hak akses, inisialisasi
	 * filter tanggal, konfigurasi tombol, serta pemuatan data awal ke grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Langkah-langkah yang dilakukan secara berurutan:
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose(comp)} untuk menyelesaikan proses wire ZKoss.</li>
	 *   <li>Memvalidasi sesi pengguna: jika atribut {@code usersTemp} tidak ada atau pengguna
	 *       tidak memiliki hak READ, sesi dibersihkan dan pengguna diarahkan ke halaman logoff.</li>
	 *   <li>Mengatur datebox {@code start} dan {@code end} menjadi readonly agar pengguna tidak
	 *       bisa mengetik langsung, hanya memilih melalui kalender.</li>
	 *   <li>Menginisialisasi rentang tanggal default: {@code start} diset ke 6 bulan lalu,
	 *       {@code end} diset ke besok (tanggal saat ini + 1 hari).</li>
	 *   <li>Mengatur visibilitas tombol tambah berdasarkan hak akses CREATE pengguna.</li>
	 *   <li>Membaca dan menyimpan flag hak akses UPDATE, DELETE, APPROVE, dan REJECT.</li>
	 *   <li>Menginisialisasi komponen paging dengan event listener yang memanggil
	 *       {@code onSearchDefault} saat halaman berubah.</li>
	 *   <li>Membuat timer default yang akan memanggil {@code onSearchDefault} untuk memuat
	 *       data awal ke grid setelah halaman selesai di-render.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param comp Komponen root dari halaman ZUL yang sudah ter-compose.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika sesi tidak valid, metode akan langsung return tanpa melakukan inisialisasi lebih lanjut.
	 * Exception dari superclass dibiarkan merambat ke atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah filter baru, inisialisasi nilai default filter tersebut di sini.
	 * Pastikan urutan langkah-langkah dipertahankan karena beberapa langkah bergantung
	 * pada hasil langkah sebelumnya (misalnya wire komponen harus selesai sebelum bisa
	 * mengakses field UI).</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat compose komponen.
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * <h3>PembayaranDpMasterAssetRenderer — Renderer Baris Grid Pembayaran DP</h3>
	 *
	 * <p><b>Tujuan:</b><br>
	 * Inner class yang bertanggung jawab merender setiap baris data {@code PembayaranDpMasterAsset}
	 * pada grid utama. Setiap baris menampilkan informasi lengkap pembayaran DP termasuk kode,
	 * daftar detail pesanan yang terkait, nama penyedia, nilai yang dibayar, jenis pembayaran,
	 * informasi pembuat dan penyetuju, keterangan, status aktif, serta tombol-tombol aksi
	 * (cetak, setujui, batalkan persetujuan, ubah, hapus).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengimplementasikan {@code ais.ui.util.MyRowRenderer} dan method {@code render} yang
	 * dipanggil oleh ZKoss untuk setiap baris data. Setiap baris dibangun secara programatis
	 * menggunakan komponen ZKoss: Label, Vbox, Hbox, MyToolbarbuttonConfig. Tombol persetujuan
	 * dan pembatalan persetujuan memiliki dialog konfirmasi sebelum eksekusi. Tombol ubah
	 * dan hapus hanya ditampilkan jika data belum disetujui. Status aktif dapat diubah
	 * langsung melalui checkbox di baris grid jika data belum disetujui dan pengguna
	 * memiliki hak ubah.</p>
	 *
	 * <p><b>Threading:</b><br>
	 * Berjalan pada event thread ZKoss. Operasi database dilakukan secara sinkron.
	 * Pemanggilan {@code Common.createDefaultTimer} untuk hapus data menggunakan timer
	 * ZKoss agar commit dapat diselesaikan sebelum refresh UI.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah kolom baru pada grid, tambahkan di sini sekaligus di definisi kolom
	 * pada file ZUL yang menggunakan renderer ini. Urutan kolom harus konsisten.</p>
	 */
	class PembayaranDpMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <p><b>Tujuan:</b><br>
		 * Merender satu baris data {@code PembayaranDpMasterAsset} ke dalam komponen-komponen
		 * ZKoss yang ditampilkan pada grid. Metode ini dipanggil oleh engine ZKoss untuk
		 * setiap entitas yang ada dalam model data grid.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * <ol>
		 *   <li>Membuat Vbox utama menggunakan {@code RevisiHelper.createNewRevisi} untuk
		 *       menampilkan kode dan informasi revisi entitas.</li>
		 *   <li>Menambahkan link unduh/unggah bukti pembayaran menggunakan
		 *       {@code LampiranLain.createDownloadUploadFileLain}.</li>
		 *   <li>Mengambil semua detail pembayaran DP dari database dan menampilkan kode
		 *       serta keterangan setiap pesanan pengadaan yang terkait, beserta link
		 *       unduh tagihan per pesanan.</li>
		 *   <li>Menampilkan nama penyedia, nilai yang dibayar (diformat dengan numberFormat),
		 *       dan jenis pembayaran.</li>
		 *   <li>Menampilkan informasi pembuat (nama dan tanggal) serta penyetuju (nama dan tanggal).</li>
		 *   <li>Menampilkan keterangan dan link SOP jika disposisi SOP terkait ada.</li>
		 *   <li>Menampilkan checkbox aktif (jika data belum disetujui dan pengguna punya hak ubah)
		 *       atau label "Ya"/"Tidak" untuk status aktif.</li>
		 *   <li>Membuat toolbar dengan tombol: Cetak, Setujui, Batalkan, Ubah, Hapus;
		 *       masing-masing dengan event listener dan dialog konfirmasi yang sesuai.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Parameter:</b></p>
		 * @param arg0 Baris ({@code Row}) ZKoss tempat komponen akan ditempatkan.
		 * @param arg1 Objek data, harus berupa instance {@code PembayaranDpMasterAsset}.
		 *
		 * <p><b>Penanganan error:</b><br>
		 * Error saat penghapusan ditangkap dan ditampilkan menggunakan
		 * {@code Common.tampilErrorJikaAdmin} dan {@code MyMessageboxConfig} dengan pesan
		 * yang informatif tentang relasi data yang mencegah penghapusan.</p>
		 *
		 * <p><b>Pemeliharaan:</b><br>
		 * Perhatikan bahwa setelah persetujuan diberikan, tombol Ubah dan Hapus disembunyikan
		 * secara otomatis. Logika visibilitas tombol diperbarui secara langsung setelah
		 * operasi setuju/batalkan tanpa perlu memuat ulang seluruh grid.</p>
		 *
		 * @throws Exception jika terjadi kesalahan saat membangun komponen UI.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranDpMasterAsset pembayaranDpMasterAsset = (PembayaranDpMasterAsset) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PembayaranDpMasterAsset.class, pembayaranDpMasterAsset,
					pembayaranDpMasterAsset.getKode())).setParent(arg0);
			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pembayaranDpMasterAsset.getId(),
					PembayaranDpMasterAsset.class.getName(), "Bukti Pembayaran", false, null, null, false, false, false,
					false);

			Session session = HibernateUtil.currentSession();
			List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssetDetails = session
					.createCriteria(PembayaranDpMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranDpMasterAsset", pembayaranDpMasterAsset)).list();
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssetDetails) {

				Hbox dataInvoice = new Hbox();
				vbox.appendChild(dataInvoice);
				dataInvoice.appendChild(
						new Label(pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode()));
				dataInvoice.appendChild(new Space());
				dataInvoice.appendChild(
						new Label(pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKeterangan()));

				Hbox hboxa = new Hbox();
				hboxa.setParent(vbox);
				LampiranLain.createDownloadUploadFileLain(hboxa,
						pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getId(),
						PemesananPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false,
						false, false);
			}

			new Label(pembayaranDpMasterAsset.getPenyedia() == null ? ""
					: pembayaranDpMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(pembayaranDpMasterAsset.getNilaiDibayar())).setParent(arg0);

			new Label(pembayaranDpMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranDpMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pembayaranDpMasterAsset.getDibuatOleh() == null ? ""
					: pembayaranDpMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pembayaranDpMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranDpMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(pembayaranDpMasterAsset.getDisetujuiOleh() == null ? ""
					: pembayaranDpMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(pembayaranDpMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranDpMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pembayaranDpMasterAsset.getKeterangan())).setParent(vbox1);
			if (pembayaranDpMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pembayaranDpMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ pembayaranDpMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranDpMasterAsset.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			if (pembayaranDpMasterAsset.getDisposisiSop() != null
					&& !pembayaranDpMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pembayaranDpMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pembayaranDpMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pembayaranDpMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pembayaranDpMasterAsset);
					}
				});
			} else {
				new Label(pembayaranDpMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pembayaran Dp Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pembayaranDpMasterAsset);
				}

			});
			aksiButtons.add(button);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pembayaranDpMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Pembayaran ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranDpMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pembayaranDpMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pembayaranDpMasterAsset);

										disetujuiTanggal
												.setValue(pembayaranDpMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pembayaranDpMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranDpMasterAsset.getDisetujuiOleh() == null ? ""
												: pembayaranDpMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranDpMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pembayaranDpMasterAsset.getDisetujuiOleh() == null);

										cetak(pembayaranDpMasterAsset);
									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pembayaran Dp Aset ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranDpMasterAsset.setDisetujuiOleh(null);
										pembayaranDpMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, pembayaranDpMasterAsset);

										disetujuiTanggal
												.setValue(pembayaranDpMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(
																pembayaranDpMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranDpMasterAsset.getDisetujuiOleh() == null ? ""
												: pembayaranDpMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranDpMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && pembayaranDpMasterAsset.getDisetujuiOleh() == null);

									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembayaranDpMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pembayaranDpMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											if (SopUtil.hapusDisposisi(session,
													pembayaranDpMasterAsset.getDisposisiSop())) {
												List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssetDetails = session
														.createCriteria(PembayaranDpMasterAssetDetail.class)
														.add(Restrictions.eq("pembayaranDpMasterAsset",
																pembayaranDpMasterAsset))
														.list();
												for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssetDetails) {
													session.delete(pembayaranDpMasterAssetDetail);
												}
												session.flush();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, pembayaranDpMasterAsset);
														session.flush();
														onSearchDefault(arg0);
													}
												});
											}
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			aksiButtons.add(hapus);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Event handler yang dipanggil ketika pengguna mengklik tombol "Tambah" untuk membuat
	 * data pembayaran DP baru. Metode ini menginisialisasi form dengan entitas kosong dan
	 * menampilkan jendela modal form pengisian.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membuat instance baru {@code PembayaranDpMasterAsset} (tanpa ID, artinya data baru),
	 * kemudian memanggil {@code init} untuk membangun form dan menyiapkan semua komponen UI.
	 * Setelah form siap, jendela modal {@code addWindow} ditampilkan kepada pengguna.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu pemanggilan metode ini (biasanya onClick dari tombol Tambah).
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari {@code init} dibiarkan merambat ke atas untuk ditangani oleh framework ZKoss.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan komponen {@code addWindow} sudah ter-wire dari ZUL sebelum metode ini dipanggil.
	 * Nama event handler harus sesuai dengan konvensi ZKoss (diawali dengan "on" + nama event).</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat menginisialisasi form.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PembayaranDpMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode privat yang mempersiapkan jendela modal form pembayaran DP, baik untuk penambahan
	 * data baru maupun pengeditan data yang sudah ada. Metode ini membangun struktur layout
	 * form secara programatis menggunakan komponen ZKoss.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas yang akan diedit ke field instance.</li>
	 *   <li>Mengatur judul jendela modal menjadi "Proses Pembayaran DP".</li>
	 *   <li>Membersihkan konten jendela yang ada menggunakan {@code Common.clear}.</li>
	 *   <li>Membangun layout BorderLayout dengan Center untuk form dan South untuk toolbar tombol.</li>
	 *   <li>Membuat tombol Simpan dan memanggil {@code form} untuk membangun isi form
	 *       di dalam panel Center.</li>
	 *   <li>Menambahkan tombol Batal dan Simpan di toolbar South dengan event listener masing-masing:
	 *       Batal menyembunyikan jendela, Simpan memanggil {@code onSave} dan jika berhasil
	 *       memuat ulang data, mereset paging, dan menyembunyikan jendela.</li>
	 *   <li>Mereset {@code disposisiSop} menjadi null sebelum memanggil form.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param pembayaranDpMasterAsset Entitas {@code PembayaranDpMasterAsset} yang akan ditampilkan
	 *                                pada form. Jika ID-nya null, form dalam mode tambah baru;
	 *                                jika ID-nya ada, form dalam mode edit.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari operasi form dan penyimpanan dibiarkan merambat ke atas.
	 * Event listener Simpan yang gagal ({@code onSave} mengembalikan false) tidak akan
	 * menutup jendela, memungkinkan pengguna memperbaiki inputnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah tombol aksi baru di toolbar, tambahkan di dalam blok South setelah
	 * tombol Batal dan Simpan yang sudah ada. Pastikan tombol baru memiliki tooltip yang jelas.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI.
	 */
	private void init(PembayaranDpMasterAsset pembayaranDpMasterAsset) throws Exception {
		this.pembayaranDpMasterAsset = pembayaranDpMasterAsset;
		addWindow.setTitle("Proses Pembayaran DP");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(pembayaranDpMasterAsset, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode penyimpanan utama yang memvalidasi seluruh input form dan menyimpan data
	 * pembayaran DP ke database. Metode ini menangani baik pembuatan data baru maupun
	 * pembaruan data yang sudah ada, termasuk penyimpanan detail per pesanan pengadaan
	 * dan pembaruan nilai pembayaran yang sudah dibayarkan pada masing-masing pesanan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li><b>Validasi input:</b> Memastikan penyedia dipilih, kode tidak kosong,
	 *       minimal satu tagihan dipilih dengan nilai lebih dari nol, dan nilai yang
	 *       dibayarkan tidak nol.</li>
	 *   <li><b>Persiapan entitas:</b> Jika data sudah ada (ID tidak null), reload dari
	 *       native session untuk menghindari state yang basi. Set disposisi SOP jika ada.</li>
	 *   <li><b>Pengisian field header:</b> Mengisi jenis pembayaran, nilai dibayar, kode,
	 *       keterangan, tanggal, dan penyedia. Jika mode persetujuan aktif, langsung mengisi
	 *       disetujuiOleh dan tanggalPersetujuan.</li>
	 *   <li><b>Simpan header:</b> Jika data baru, generate kode unik dan save. Jika edit,
	 *       lakukan update.</li>
	 *   <li><b>Simpan detail:</b> Iterasi semua baris di gridMasterAsset yang dicentang,
	 *       simpan atau update setiap {@code PembayaranDpMasterAssetDetail}, lalu hitung
	 *       ulang total yang sudah dibayar pada {@code PemesananPengadaanMasterAsset}.</li>
	 *   <li><b>Integrasi akunting:</b> Jika jenis pembayaran kosong atau DaftarPengajuanTransfer
	 *       belum ada, panggil {@code DaftarPengajuanTransfer.simpanPembayaranDpMasterAssetDetail}.</li>
	 *   <li><b>Pasca simpan:</b> Menutup dan membersihkan sesi Hibernate, lalu menjalankan
	 *       timer untuk mencetak dokumen PDF pembayaran DP secara otomatis.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu operasi simpan (biasanya onClick dari tombol Simpan).
	 *
	 * <p><b>Return:</b></p>
	 * @return {@code true} jika penyimpanan berhasil dan form boleh ditutup;
	 *         {@code false} jika validasi gagal dan form harus tetap terbuka.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Validasi input menampilkan pesan peringatan kepada pengguna melalui {@code MyMessageboxConfig}
	 * dengan ikon EXCLAMATION. Setelah validasi, tidak ada try-catch eksplisit sehingga
	 * exception database akan merambat ke atas dan ditangani oleh framework ZKoss.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Kode komentar yang menonaktifkan validasi jumlah bayar vs nilai tagihan sengaja dibiarkan
	 * sebagai referensi; jangan hapus komentar tersebut tanpa diskusi dengan tim bisnis.
	 * Sesi Hibernate harus selalu ditutup di akhir metode; pastikan blok penutupan sesi
	 * tidak terlewat jika logika diubah.</p>
	 *
	 * @throws Exception jika terjadi kesalahan database atau komponen UI.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (penyediaAsset.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari dari daftar penyedia terdaftar; (2) Jika penyedia belum ada, daftarkan melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pembayaran DP belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pembayaran atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Double nilai = 0.0;
		Double d = 0.0;
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {

			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail = (PembayaranDpMasterAssetDetail) row
						.getAttribute("pembayaranDpMasterAssetDetail");

				PemesananPengadaanMasterAsset penerimaanDpMasterAsset = pembayaranDpMasterAssetDetail
						.getPemesananPengadaanMasterAsset();

				Double nilaitagihan = penerimaanDpMasterAsset == null ? 0.0 : penerimaanDpMasterAsset.getDptotal();

				nilai += nilaitagihan;
				d += pembayaranDpMasterAssetDetail.getDibayar();
			}
		}

		if (nilai.intValue() == 0) {
			MyMessageboxConfig.show("Mohon maaf, belum ada tagihan DP yang dipilih. Langkah yang dapat dilakukan: (1) Centang tagihan DP yang ingin dibayarkan dari daftar tagihan; (2) Pastikan tagihan sudah tersedia dan belum lunas; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (d.intValue() == 0) {
			MyMessageboxConfig.show("Mohon maaf, jumlah riwayat pembayaran belum diisi. Langkah yang dapat dilakukan: (1) Isi nilai pembayaran pada field yang tersedia; (2) Pastikan nilai yang dimasukkan lebih dari nol; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

//		if (d.intValue() > nilai.intValue()) {
//			MyMessageboxConfig.show("Jumlah total pembayaran tidak boleh lebih besar dari pada nilai tagihan",
//					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
//			return false;
//		}

		Session session = HibernateUtil.currentNativeSession();
		if (pembayaranDpMasterAsset.getId() != null) {
			pembayaranDpMasterAsset = (PembayaranDpMasterAsset) session.load(PembayaranDpMasterAsset.class,
					pembayaranDpMasterAsset.getId());

		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pembayaranDpMasterAsset.setDisposisiSop(disposisiSop);
		}

		pembayaranDpMasterAsset.setJenisPembayaranBarang(
				(JenisPembayaranBarang) (jenisPembayaranBarang.getSelectedItem() == null ? null
						: jenisPembayaranBarang.getSelectedItem().getValue()));
		pembayaranDpMasterAsset.setNilaiDibayar(d);
		pembayaranDpMasterAsset.setKode(kode.getValue());
		pembayaranDpMasterAsset.setKeterangan(keterangan.getValue());
		pembayaranDpMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		pembayaranDpMasterAsset.setPenyedia((PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset"));

		if (persetujuan) {
			pembayaranDpMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
			pembayaranDpMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		}

		if (pembayaranDpMasterAsset.getId() != null) {
			session.getTransaction().begin();
			session.update(pembayaranDpMasterAsset);
			session.getTransaction().commit();
		} else {
			pembayaranDpMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pembayaranDpMasterAsset.setKode(kode.getValue());
			session.getTransaction().begin();
			session.save(pembayaranDpMasterAsset);
			session.getTransaction().commit();
		}

		for (Row row : rowsMasterAsset) {
			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail = (PembayaranDpMasterAssetDetail) row
						.getAttribute("pembayaranDpMasterAssetDetail");
				pembayaranDpMasterAssetDetail.setPembayaranDpMasterAsset(pembayaranDpMasterAsset);

				session.getTransaction().begin();
				if (pembayaranDpMasterAssetDetail.getId() == null) {
					session.save(pembayaranDpMasterAssetDetail);
				} else {
					Common.refreshUpdate(session, pembayaranDpMasterAssetDetail);
				}
				session.getTransaction().commit();

				PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranDpMasterAssetDetail
						.getPemesananPengadaanMasterAsset();
				if (pemesananPengadaanMasterAsset != null && pemesananPengadaanMasterAsset.getId() != null) {
					Number nilaiTagihan = (Number) session.createCriteria(PembayaranDpMasterAssetDetail.class)
							.add(Restrictions.eq("pemesananPengadaanMasterAsset", pemesananPengadaanMasterAsset))
							.setProjection(Projections.sum("dibayar")).uniqueResult();

					pembayaranDpMasterAssetDetail
							.setTotalTelahDibayar(nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue());

					PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset1 = (PemesananPengadaanMasterAsset) session
							.createCriteria(PemesananPengadaanMasterAsset.class)
							.add(Restrictions.idEq(pemesananPengadaanMasterAsset.getId())).uniqueResult();

					Double dibayar = pemesananPengadaanMasterAsset1.hitungDibayar(session);

					pemesananPengadaanMasterAsset1.setDibayar(dibayar);

					session.getTransaction().begin();
					Common.refreshUpdate(session, pemesananPengadaanMasterAsset1);
					Common.refreshUpdate(session, pembayaranDpMasterAssetDetail);
					session.getTransaction().commit();
				}
				if (pembayaranDpMasterAsset.getJenisPembayaranBarang() == null
						|| pembayaranDpMasterAssetDetail.getDaftarPengajuanTransfer() == null) {
					DaftarPengajuanTransfer.simpanPembayaranDpMasterAssetDetail(pembayaranDpMasterAssetDetail);
				}
			}
		}

		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cetak(pembayaranDpMasterAsset);
			}
		}, "Proses cetak", false, 2500);

		return true;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode privat yang mengumpulkan dan memetakan semua parameter yang diperlukan untuk
	 * menghasilkan laporan PDF pembayaran DP. Parameter mencakup data header pembayaran,
	 * informasi bank penyedia, dokumen yang dimiliki penyedia, data alur SOP, dan detail
	 * daftar tagihan per pesanan pengadaan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Me-refresh entitas {@code PembayaranDpMasterAsset} dari database jika sudah ada ID-nya,
	 *       untuk memastikan data yang digunakan adalah data terkini.</li>
	 *   <li>Menggunakan {@code Common.insertProperty} untuk memetakan semua getter entitas
	 *       ke dalam Map parameter dengan prefix "data.".</li>
	 *   <li>Jika penyedia ada, mem-parsing data bank (JSON array) dan menambahkan setiap field
	 *       bank ke parameter dengan format "bank_{index}.{key}".</li>
	 *   <li>Memuat semua jenis dokumen penyedia dari cache konstan dan mengambil data
	 *       keterangan dokumen yang dimiliki penyedia tersebut.</li>
	 *   <li>Memanggil {@code DisposisiAlurSop.parameterMap} untuk menambahkan parameter SOP.</li>
	 *   <li>Membangun daftar Map untuk setiap detail pembayaran, mencakup nilai tagihan,
	 *       nilai dibayar, tanggal, status persetujuan, kode, dan keterangan.</li>
	 *   <li>Menghapus semua parameter yang mengandung "disposisiSop" untuk menghindari
	 *       masalah serialisasi pada template laporan JasperReports.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param pembayaranDpMasterAsset Entitas pembayaran DP yang akan digunakan sebagai
	 *                                sumber data laporan.
	 *
	 * <p><b>Return:</b></p>
	 * @return Map berisi semua parameter untuk template laporan JasperReports "asset/pembayaran_dp".
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Parsing JSON bank penyedia dibungkus try-catch karena data bank bisa tidak valid atau kosong.
	 * Kegagalan parsing bank tidak menghentikan proses pembuatan parameter lainnya.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika template laporan diperbarui dengan parameter baru, tambahkan pemetaan parameter
	 * yang sesuai di metode ini. Pastikan kunci parameter Map konsisten dengan nama parameter
	 * yang digunakan di template JRXML.</p>
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map parameter(PembayaranDpMasterAsset pembayaranDpMasterAsset) {
		if (pembayaranDpMasterAsset != null && pembayaranDpMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(pembayaranDpMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pembayaranDpMasterAsset.getId());

		Common.insertProperty(PembayaranDpMasterAsset.class, pembayaranDpMasterAsset, parameters, "data");

		if (pembayaranDpMasterAsset.getPenyedia() != null) {

			try {
				JSONArray array = new JSONArray(pembayaranDpMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PembayaranDpMasterAssetAction.java:1135");
				// Data bank penyedia tidak valid atau kosong, lewati tanpa error
			}

			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = pembayaranDpMasterAsset.getPenyedia();
			Session session = HibernateUtil.currentSession();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : dokumenPenyediaAssets) {

				PenyediaAssetPunyaDokumen temp = (PenyediaAssetPunyaDokumen) (penyediaAsset == null
						|| penyediaAsset.getId() == null
								? new PenyediaAssetPunyaDokumen()
								: session.createCriteria(PenyediaAssetPunyaDokumen.class)
										.add(Restrictions.eq("dokumenPenyediaAsset", dokumenPenyediaAsset))
										.add(Restrictions.eq("penyediaAsset", penyediaAsset)).setMaxResults(1)
										.uniqueResult());

				parameters.put("dokumen." + dokumenPenyediaAsset.getNama(), temp == null ? "" : temp.getKeterangan());
			}

		}

		DisposisiAlurSop.parameterMap(pembayaranDpMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<Map> maps = new ArrayList<Map>();

		List<PembayaranDpMasterAssetDetail> pembayaranDpMasterAssetDetails = session
				.createCriteria(PembayaranDpMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranDpMasterAsset", pembayaranDpMasterAsset)).list();

		for (PembayaranDpMasterAssetDetail pembayaranDpMasterAssetDetail : pembayaranDpMasterAssetDetails) {

			PemesananPengadaanMasterAsset penerimaanDpMasterAsset = pembayaranDpMasterAssetDetail
					.getPemesananPengadaanMasterAsset();
			Map map = new HashMap();
			Common.insertProperty(PembayaranDpMasterAssetDetail.class, pembayaranDpMasterAssetDetail, map, "data");
			Double nilaitagihan = penerimaanDpMasterAsset == null ? 0.0 : penerimaanDpMasterAsset.getDptotal();

			map.put("tagihan", nilaitagihan);
			map.put("nilai", pembayaranDpMasterAssetDetail.getDibayar());
			map.put("tgl", penerimaanDpMasterAsset.getTanggalPembuatan());

			map.put("status_persetujuan", pembayaranDpMasterAsset.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Telah disetujui oleh " + pembayaranDpMasterAsset.getDisetujuiOleh().getUserNama());

			map.put("perpustakaan", pembayaranDpMasterAsset.getKeterangan());
			map.put("tanggal_persetujuan", pembayaranDpMasterAsset.getTanggalPersetujuan());

			map.put("kode", pembayaranDpMasterAsset.getKode());
			map.put("penyedia", pembayaranDpMasterAsset.getPenyedia() == null ? ""
					: pembayaranDpMasterAsset.getPenyedia().getNama());

			map.put("jenisPembayaranBarang", pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKode()
					+ " " + pembayaranDpMasterAssetDetail.getPemesananPengadaanMasterAsset().getKeterangan());

			map.put("grup", "Data Tagihan");

			map.put("jenis_pemesanan_dp_asset", pembayaranDpMasterAssetDetail.getKeterangan());
			maps.add(map);
		}

		parameters.put("maps", maps);

		for (Object o : parameters.keySet()) {
			if (o.toString().contains("disposisiSop")) {
				parameters.put(o.toString(), null);
			}
		}

		return parameters;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang menghasilkan file laporan PDF
	 * untuk data pembayaran DP yang diberikan. Digunakan oleh sistem SOP untuk mencetak
	 * dokumen pembayaran DP sebagai bagian dari alur persetujuan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menerima entitas {@code GeneralValueObject} (yang di-cast ke {@code PembayaranDpMasterAsset}),
	 * mengumpulkan parameter laporan menggunakan metode {@code parameter}, lalu menghasilkan
	 * file PDF menggunakan {@code Report.generateFileReport} dengan template "asset/pembayaran_dp".</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param generalValueObject Objek data, harus berupa instance {@code PembayaranDpMasterAsset}
	 *                           yang akan dicetak.
	 *
	 * <p><b>Return:</b></p>
	 * @return Objek {@code File} yang menunjuk ke file PDF yang dihasilkan dan siap untuk diunduh.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari pembuatan laporan dibiarkan merambat ke atas untuk ditangani oleh
	 * pemanggil (biasanya sistem SOP atau framework ZKoss).</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Nama template laporan "asset/pembayaran_dp" harus sesuai dengan file JRXML yang ada
	 * di direktori laporan. Jika template diganti, perbarui string ini secara konsisten
	 * dengan metode {@code cetak}.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat menghasilkan laporan.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PembayaranDpMasterAsset pembayaranDpMasterAsset = (PembayaranDpMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(pembayaranDpMasterAsset), "asset/pembayaran_dp",
				pembayaranDpMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Metode privat yang menghasilkan dan menampilkan laporan PDF pembayaran DP langsung
	 * di browser pengguna (inline PDF viewer atau download). Dipanggil setelah penyimpanan
	 * data atau saat pengguna mengklik tombol cetak pada baris grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Memanggil {@code Report.generatePDFReport} dengan parameter laporan yang dikumpulkan
	 * dari {@code parameter(pembayaranDpMasterAsset)} dan template "asset/pembayaran_dp".
	 * Tanggal yang digunakan untuk laporan adalah tanggal pembuatan pembayaran DP.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param pembayaranDpMasterAsset Entitas pembayaran DP yang akan dicetak sebagai laporan PDF.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari pembuatan laporan dibiarkan merambat ke atas. Pemanggil (event listener
	 * atau timer) bertanggung jawab untuk menangani exception ini.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini sengaja dibuat privat karena hanya digunakan secara internal. Jika perlu
	 * dipanggil dari luar kelas, pertimbangkan untuk menggunakan {@code cetakData} yang
	 * merupakan metode publik dari antarmuka {@code FormSop}.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat menghasilkan atau menampilkan laporan PDF.
	 */
	@SuppressWarnings({})
	private void cetak(PembayaranDpMasterAsset pembayaranDpMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(pembayaranDpMasterAsset), "asset/pembayaran_dp",
				pembayaranDpMasterAsset.getTanggalPembuatan());
	}

	/** Checkbox pencarian yang memfilter data berdasarkan status aktif entitas. */
	private Checkbox searchaktif;

	/**
	 * <p><b>Tujuan:</b><br>
	 * Membangun dan mengembalikan objek {@code Criteria} Hibernate yang digunakan untuk
	 * query data pembayaran DP sesuai dengan semua filter yang aktif di halaman. Metode ini
	 * digunakan baik untuk menghitung jumlah total data (untuk paging) maupun untuk mengambil
	 * data yang ditampilkan di grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun kriteria Hibernate dengan filter berikut secara berurutan:
	 * <ol>
	 *   <li><b>Filter aktif:</b> Jika checkbox searchaktif dicentang, hanya tampilkan data
	 *       yang aktif (aktif = true atau null); jika tidak dicentang, tampilkan semua.</li>
	 *   <li><b>Filter tanggal:</b> Membatasi data berdasarkan rentang tanggal pembuatan
	 *       antara {@code start} dan {@code end}; jika salah satu null, tidak ada filter tanggal.</li>
	 *   <li><b>Filter status persetujuan:</b> Berdasarkan kombinasi checkbox blmDisetujui
	 *       dan disetujui — keduanya dicentang berarti tampilkan semua, tidak ada yang dicentang
	 *       berarti tidak ada yang ditampilkan, hanya blmDisetujui berarti filter isNull(disetujuiOleh),
	 *       hanya disetujui berarti filter isNotNull(disetujuiOleh).</li>
	 *   <li><b>Filter penyedia:</b> Pencarian ILIKE pada nama atau kode penyedia.</li>
	 *   <li><b>Filter kode:</b> Pencarian ILIKE pada kode pembayaran DP.</li>
	 *   <li><b>Filter keterangan:</b> Pencarian ILIKE pada field keterangan.</li>
	 *   <li><b>Pengurutan:</b> Jika parameter {@code order} true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param order {@code true} untuk menambahkan ORDER BY id DESC pada kriteria;
	 *              {@code false} untuk kriteria tanpa pengurutan (biasanya digunakan untuk count).
	 *
	 * <p><b>Return:</b></p>
	 * @return Objek {@code Criteria} Hibernate yang siap dieksekusi untuk query data.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception Hibernate akan merambat ke atas.
	 * Filter yang null (misal saat halaman pertama kali dimuat sebelum wire selesai) ditangani
	 * dengan guard null check pada setiap komponen filter.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika menambah filter baru, tambahkan klausa {@code .add(Restrictions...)} sebelum
	 * blok pengurutan. Perhatikan bahwa join alias "penyedia" sudah dibuat; jangan
	 * membuat alias yang sama dua kali.</p>
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranDpMasterAsset.class)

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add((start == null || end == null || start.getValue() == null || end.getValue() == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(start.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(end.getValue()) + "')")))

				.add(blmDisetujui == null || disetujui == null ? Restrictions.sqlRestriction("true") : blmDisetujui.isChecked() && disetujui.isChecked() ? Restrictions.sqlRestriction("true")
						: !blmDisetujui.isChecked() && !disetujui.isChecked() ? Restrictions.sqlRestriction("false")
								: blmDisetujui.isChecked() ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.createAlias("penyedia", "penyedia")

				.add(searchpenyedia.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("penyedia.nama", searchpenyedia.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.ilike("penyedia.kode", searchpenyedia.getValue().trim(),
										MatchMode.ANYWHERE)))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Event handler utama untuk pencarian dan pemuatan ulang data pada grid. Dipanggil
	 * ketika pengguna mengklik tombol cari, mengubah filter, atau berpindah halaman pada
	 * paging. Juga dipanggil secara otomatis oleh timer saat halaman pertama kali dimuat.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@code Common.initPaging} dengan kriteria tanpa order untuk menghitung
	 *       total data dan memperbarui komponen paging.</li>
	 *   <li>Mengeksekusi criteria dengan order descending, membatasi hasil sesuai jumlah
	 *       baris per halaman ({@code Common.ROWS_COUNT_ON_PAGE}) dan offset halaman aktif.</li>
	 *   <li>Membungkus hasil query dalam {@code SimpleListModel} dan mengatur renderer
	 *       {@code PembayaranDpMasterAssetRenderer} pada grid.</li>
	 *   <li>Memanggil {@code grid.setModelCheckMobile} untuk menampilkan data dengan
	 *       dukungan tampilan mobile.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param event Event ZKoss yang memicu pencarian (bisa null jika dipanggil secara programatis).
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Tidak ada penanganan error eksplisit; exception Hibernate akan merambat ke atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konstanta {@code Common.ROWS_COUNT_ON_PAGE} menentukan jumlah baris per halaman secara global.
	 * Jika halaman ini memerlukan jumlah baris yang berbeda, gunakan nilai konstanta lokal.</p>
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembayaranDpMasterAsset> pembayaranDpMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranDpMasterAsset);
		grid.setRowRenderer(new PembayaranDpMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang membangun dan mengembalikan
	 * form input data pembayaran DP sebagai sebuah {@code MyGrid}. Form ini digunakan baik
	 * untuk penambahan data baru maupun pengeditan data yang sudah ada, dan dapat diintegrasikan
	 * ke dalam alur kerja SOP melalui antarmuka {@code FormSop}.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun form berbasis grid ZKoss dengan baris-baris berikut:
	 * <ol>
	 *   <li><b>Penyedia:</b> Banbox {@code AmbilDataPenyediaAssetBanbox} untuk memilih vendor.
	 *       Dalam mode persetujuan, ditampilkan sebagai label read-only.</li>
	 *   <li><b>Kode:</b> Label kode unik yang di-generate otomatis dari {@code generateCode(false)}.
	 *       Dalam mode persetujuan, ditampilkan sebagai label biasa.</li>
	 *   <li><b>Tanggal:</b> Datebox tanggal pembuatan dengan format sesuai konfigurasi.</li>
	 *   <li><b>Jenis Pembayaran:</b> Combobox {@code JenisPembayaranBarang} yang diisi dari
	 *       database. Untuk data yang sudah ada, ditampilkan sebagai label read-only.</li>
	 *   <li><b>Keterangan:</b> Textbox multiline untuk catatan pembayaran DP.</li>
	 *   <li><b>Detail Pesanan:</b> Sub-grid pesanan pengadaan yang dikelola oleh
	 *       {@code PembayaranDpMasterAssetHelper}, ditampilkan dalam span 2 kolom penuh.</li>
	 * </ol>
	 * Event listener pada banbox penyedia akan menonaktifkan pemilihan penyedia setelah
	 * dipilih dan memuat ulang daftar pesanan pengadaan milik penyedia tersebut.
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param generalValueObject Entitas data, harus berupa {@code PembayaranDpMasterAsset}.
	 *                           Nilainya bisa kosong (baru) atau sudah berisi data (edit).
	 * @param disposisiSop       Disposisi SOP yang terkait; bisa null jika tidak ada alur SOP.
	 * @param save               Tombol simpan yang akan ditambahkan event listener-nya oleh
	 *                           metode pemanggil (biasanya {@code init}).
	 * @param setujuiData        Event listener untuk aksi persetujuan dari SOP; bisa null.
	 *
	 * <p><b>Return:</b></p>
	 * @return Objek {@code MyGrid} yang berisi seluruh form input pembayaran DP,
	 *         siap untuk ditambahkan ke container UI.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Exception dari pembuatan komponen UI dan query database dibiarkan merambat ke atas.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Urutan baris form harus konsisten dengan definisi kolom yang ada. Jika menambah
	 * field baru, tambahkan baris baru sebelum baris Detail Pesanan (baris terakhir yang
	 * menggunakan span 2 kolom) agar layout tidak rusak.</p>
	 *
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujuiData) throws Exception {
		this.pembayaranDpMasterAsset = (PembayaranDpMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia *"));

		penyediaAsset = new AmbilDataPenyediaAssetBanbox();
		if (persetujuan) {
			row.appendChild(new Label(pembayaranDpMasterAsset.getPenyedia() == null ? ""
					: pembayaranDpMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyediaAsset);
		}

		penyediaAsset.setAttribute("penyediaAsset", pembayaranDpMasterAsset.getPenyedia());
		penyediaAsset.setValue(
				pembayaranDpMasterAsset.getPenyedia() == null ? "" : pembayaranDpMasterAsset.getPenyedia().getNama());
		penyediaAsset.setReadonly(true);
		penyediaAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode "));

		tanggalPembuatan = new MyDatebox(
				pembayaranDpMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pembayaranDpMasterAsset.getTanggalPembuatan());
		if (pembayaranDpMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pembayaranDpMasterAsset.setKode(noAgenda);
		}

		kode = new Label(pembayaranDpMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pembayaranDpMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal "));
		row.appendChild(tanggalPembuatan);
		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembayaran *"));

		jenisPembayaranBarang = new Combobox();

		Common.insertComboDanSemua(jenisPembayaranBarang, new String[] { "nama", "akun" }, "keterangan",
				JenisPembayaranBarang.class, Common.getBahasaConfig("Daftar Pengajuan Transfer"),
				Restrictions.eq("aktif", true));
		jenisPembayaranBarang.setReadonly(true);
		Common.selectComboItem(jenisPembayaranBarang, pembayaranDpMasterAsset.getJenisPembayaranBarang());

		if (pembayaranDpMasterAsset.getId() != null) {

			row.appendChild(new Label(pembayaranDpMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranDpMasterAsset.getJenisPembayaranBarang().getNama()));
		} else {
			row.appendChild(jenisPembayaranBarang);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pembayaran"));
		row.appendChild(keterangan = new MyTextbox(
				pembayaranDpMasterAsset.getKeterangan() == null ? "" : pembayaranDpMasterAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(
				(pembayaranDpMasterAssetHelper = new PembayaranDpMasterAssetHelper(gridMasterAsset = new MyGrid()))
						.initDetail(pembayaranDpMasterAsset));

		penyediaAsset.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PenyediaAsset a = (PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset");
				if (a != null) {
					penyediaAsset.setDisabled(true);
					pembayaranDpMasterAssetHelper.setPenyediaAsset(a);
				} else {
					penyediaAsset.setDisabled(false);
				}
			}
		});

		return grid;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang mengembalikan istilah atau
	 * label yang mendeskripsikan jenis dokumen ini untuk keperluan tampilan dalam alur kerja SOP.
	 * String yang dikembalikan digunakan sebagai label pada daftar pengajuan SOP dan
	 * notifikasi alur persetujuan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengembalikan string literal "Pembayaran DP Barang / Jasa" yang merupakan istilah
	 * deskriptif untuk jenis transaksi yang dikelola oleh kelas ini.</p>
	 *
	 * <p><b>Return:</b></p>
	 * @return String "Pembayaran DP Barang / Jasa" sebagai label jenis dokumen SOP.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika terminologi bisnis berubah, perbarui string yang dikembalikan di sini. Perubahan
	 * ini akan berdampak pada label yang muncul di seluruh alur kerja SOP yang menggunakan
	 * jenis dokumen ini.</p>
	 *
	 * @throws Exception tidak akan dilempar dalam implementasi ini.
	 */
	@Override
	public String istilah() throws Exception {
		return "Pembayaran DP Barang / Jasa";
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang mengembalikan entitas data
	 * yang sedang aktif diproses oleh form. Digunakan oleh sistem SOP untuk mendapatkan
	 * referensi ke objek data yang perlu diasosiasikan dengan disposisi SOP.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengembalikan field {@code pembayaranDpMasterAsset} yang merupakan referensi ke
	 * entitas {@code PembayaranDpMasterAsset} yang sedang aktif di form. Entitas ini
	 * diset saat {@code init} atau {@code form} dipanggil.</p>
	 *
	 * <p><b>Return:</b></p>
	 * @return Entitas {@code PembayaranDpMasterAsset} yang sedang aktif, atau null jika
	 *         belum ada entitas yang diinisialisasi.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Pastikan field {@code pembayaranDpMasterAsset} selalu diset sebelum sistem SOP
	 * memanggil metode ini, yaitu melalui {@code init} atau {@code form}.</p>
	 *
	 * @throws Exception tidak akan dilempar dalam implementasi ini.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pembayaranDpMasterAsset;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang mengembalikan kelas Java
	 * dari entitas yang dikelola oleh form ini. Digunakan oleh sistem SOP untuk operasi
	 * refleksi dan penanganan entitas secara generik, seperti logging audit atau
	 * penentuan jenis dokumen dalam alur kerja.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Mengembalikan {@code PembayaranDpMasterAsset.class} secara langsung sebagai referensi
	 * kelas dari entitas yang dikelola oleh kontroler ini.</p>
	 *
	 * <p><b>Return:</b></p>
	 * @return {@code Class} dari {@code PembayaranDpMasterAsset}, digunakan untuk refleksi
	 *         dan operasi generik oleh sistem SOP.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Jika entitas utama berganti, perbarui nilai yang dikembalikan di sini.</p>
	 *
	 * @throws Exception tidak akan dilempar dalam implementasi ini.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PembayaranDpMasterAsset.class;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Menghasilkan kode unik untuk pembayaran DP baru berdasarkan konfigurasi nomor surat
	 * pengadaan ({@code NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA}). Kode yang
	 * dihasilkan mengikuti format yang dikonfigurasi administrator, termasuk prefix, tahun,
	 * bulan, dan nomor urut yang disesuaikan dengan aturan reset berkala.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memeriksa apakah konfigurasi nomor surat tersedia. Jika tidak, menggunakan
	 *       {@code Common.getGeneratedBarCode()} sebagai fallback (kode acak).</li>
	 *   <li>Menentukan index nomor berdasarkan konfigurasi: jika menggunakan index urut manual,
	 *       ambil dari konfigurasi; jika tidak, hitung dari jumlah data yang ada menggunakan
	 *       {@code getindex}.</li>
	 *   <li>Jika parameter {@code tambah} true, menambah (increment) counter nomor surat
	 *       melalui {@code NomorSurat.tambahIndexNomorSurat}.</li>
	 *   <li>Memformat kode menggunakan aturan format nomor surat dan tanggal pembuatan.</li>
	 *   <li>Memastikan kode unik menggunakan {@code KodeUnikUtil.pastikanUnik} untuk
	 *       menambahkan suffix jika kode sudah dipakai.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param tambah {@code true} jika counter nomor surat harus di-increment (saat benar-benar
	 *               menyimpan data); {@code false} jika hanya untuk preview (saat form dibuka).
	 *
	 * <p><b>Return:</b></p>
	 * @return String kode unik pembayaran DP yang sudah dipastikan tidak duplikat di database.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika konfigurasi nomor surat null, fallback ke kode barcode acak tanpa melempar exception.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Konfigurasi nomor surat diambil dari {@code NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA}
	 * yang merupakan konstanta statis yang dimuat saat aplikasi start. Jika format nomor perlu
	 * diubah, lakukan melalui antarmuka administrasi nomor surat, bukan melalui kode ini.</p>
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA == null
				|| NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PEMBAYARAN_DP_PEMBELIAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PembayaranDpMasterAsset.class, noAgenda);
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Menghitung index nomor urut berikutnya untuk kode pembayaran DP berdasarkan jumlah
	 * data yang sudah ada di database, dengan mempertimbangkan berbagai aturan reset urutan
	 * seperti reset per tahun, per bulan, per tanggal tertentu, atau berdasarkan kelompok
	 * nomor surat.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun query Hibernate Criteria pada entitas {@code PembayaranDpMasterAsset} dengan
	 * filter yang disesuaikan berdasarkan konfigurasi {@code NomorSurat}:
	 * <ol>
	 *   <li><b>Filter nomor surat:</b> Jika menggunakan urut berdasarkan nomor, filter by
	 *       {@code nomorSuratAlurPengadaan.nomorSurat}; jika berdasarkan kelompok, filter by
	 *       {@code kelompokNomorSurat}; jika tidak ada konfigurasi, tidak ada filter.</li>
	 *   <li><b>Filter reset tahunan:</b> Jika reset per tahun, hanya hitung data tahun ini.</li>
	 *   <li><b>Filter reset bulanan:</b> Jika reset per bulan, hanya hitung data bulan ini.</li>
	 *   <li><b>Filter reset per tanggal:</b> Jika ada tanggal reset dan tanggal tersebut
	 *       sudah lewat atau hari ini, hanya hitung data sejak tanggal reset.</li>
	 *   <li>Menggunakan {@code Projections.rowCount()} untuk mendapatkan jumlah data.</li>
	 *   <li>Mengembalikan jumlah data + 1 sebagai index urut berikutnya.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param nomorSurat Objek konfigurasi nomor surat yang menentukan aturan penomoran.
	 *                   Jika null, mengembalikan 0 tanpa query database.
	 *
	 * <p><b>Return:</b></p>
	 * @return Nilai {@code Long} yang merupakan index nomor urut berikutnya (jumlah data + 1).
	 *         Mengembalikan 1 jika belum ada data atau parameter null.
	 *
	 * <p><b>Penanganan error:</b><br>
	 * Jika parameter {@code nomorSurat} null, mengembalikan 0 langsung tanpa query. Hasil
	 * query null juga ditangani dengan mengembalikan nilai 1.</p>
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini menggunakan join ke {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}
	 * dengan LEFT_JOIN untuk menghindari hasil kosong ketika beberapa data tidak memiliki
	 * relasi tersebut. Pastikan konfigurasi join tidak berubah jika model data dimodifikasi.</p>
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PembayaranDpMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * <p><b>Tujuan:</b><br>
	 * Implementasi metode dari antarmuka {@code FormSop} yang memungkinkan sistem SOP
	 * untuk mengatur mode persetujuan dari luar kelas. Digunakan ketika kontroler di-instansiasi
	 * tanpa konstruktor berparameter namun mode persetujuan perlu diaktifkan secara programatis
	 * oleh pengelola alur SOP.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Menyimpan nilai parameter {@code persetujuan} ke field instance. Efek dari perubahan
	 * ini akan dirasakan saat {@code onSave} berikutnya dipanggil, di mana field
	 * {@code disetujuiOleh} dan {@code tanggalPersetujuan} akan diisi otomatis.</p>
	 *
	 * <p><b>Parameter:</b></p>
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan otomatis saat simpan;
	 *                    {@code false} untuk mode normal tanpa persetujuan otomatis.
	 *
	 * <p><b>Pemeliharaan:</b><br>
	 * Metode ini harus dipanggil sebelum {@code form} atau {@code onSave} agar efeknya
	 * terasa. Pemanggilan setelah form dibuka mungkin tidak memberikan efek yang diharapkan
	 * pada tampilan form (misalnya field yang di-render berbeda untuk mode persetujuan).</p>
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
