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
import ais.action.master.asset.helper.PembayaranPengadaanMasterAssetHelper;
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
import ais.database.model.asset.PembayaranPengadaanMasterAsset;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.PenyediaAssetPunyaDokumen;
import ais.database.model.asset.SaldoAwalMasterAsset;
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
 * <h3>PembayaranPengadaanMasterAssetAction</h3>
 *
 * <b>Untuk apa:</b><br>
 * Kelas ini merupakan Action ZKoss (GenericAutowireComposer) yang mengelola seluruh alur
 * pelunasan pembayaran vendor dalam proses pengadaan aset. Halaman ini diakses oleh bagian
 * keuangan atau pejabat yang berwenang untuk mencatat dan menyetujui pembayaran final kepada
 * penyedia barang/jasa setelah barang diterima. Setiap pembayaran dikaitkan dengan satu atau
 * lebih catatan penerimaan barang (PenerimaanPengadaanMasterAsset) atau saldo awal aset
 * (SaldoAwalMasterAsset) sebagai dasar tagihan yang harus dilunasi.<br><br>
 *
 * <b>Cara kerja:</b><br>
 * Ketika halaman ZUL terkait di-load oleh ZKoss, metode {@link #doAfterCompose(Component)}
 * dipanggil secara otomatis untuk menginisialisasi hak akses pengguna (baca, ubah, hapus,
 * setujui, tolak), menyiapkan rentang tanggal default 6 bulan ke belakang, mendaftarkan event
 * paging, dan memuat data awal. Pengguna dapat menambah pembayaran baru melalui tombol Tambah
 * yang memanggil {@link #onAdd(Event)}, atau mengubah/menghapus/menyetujui pembayaran yang sudah
 * ada melalui tombol aksi pada setiap baris grid yang dirender oleh kelas inner
 * {@link PembayaranPengadaanMasterAssetRenderer}. Kelas ini juga mengimplementasikan antarmuka
 * {@link FormSop} sehingga formulir pembayaran dapat diintegrasikan dengan alur persetujuan SOP
 * digital yang sudah dikonfigurasi dalam sistem. Kode pembayaran dibangkitkan secara otomatis
 * menggunakan pola nomor surat yang dikonfigurasi di {@link NomorSuratAlurPengadaan}, dengan
 * jaminan keunikan melalui {@code KodeUnikUtil.pastikanUnik}. Setelah data tersimpan, sistem
 * secara otomatis memicu pencetakan dokumen pembayaran dalam format PDF.<br><br>
 *
 * <b>Threading:</b><br>
 * Kelas ini berjalan sepenuhnya di thread ZKoss event-thread. Tidak ada thread latar yang
 * dibuat di dalam kelas ini secara langsung. Pembuatan timer menggunakan
 * {@link Common#createDefaultTimer} yang berbasis mekanisme timer ZKoss dan tetap aman dijalankan
 * dalam konteks event-thread tunggal. Sesi Hibernate dikelola melalui
 * {@link HibernateUtil#currentNativeSession()} dan {@link HibernateUtil#currentSession()},
 * sehingga pengelolaan transaksi harus dilakukan dengan hati-hati agar tidak terjadi konflik
 * sesi lintas-request.<br><br>
 *
 * <b>Pemeliharaan:</b><br>
 * Kelas ini erat kaitannya dengan {@link PembayaranPengadaanMasterAssetHelper} yang menangani
 * tampilan detail tagihan di dalam formulir. Perubahan pada entitas
 * {@link PembayaranPengadaanMasterAsset} atau {@link PembayaranPengadaanMasterAssetDetail}
 * perlu diselaraskan dengan logika render di {@link PembayaranPengadaanMasterAssetRenderer}.
 * Kode pembayaran bergantung pada konfigurasi {@link NomorSuratAlurPengadaan#PEMBAYARAN_PEMBELIAN_DATA};
 * jika konfigurasi ini null atau nomorSurat-nya null, sistem akan jatuh ke barcode acak.
 * Blok komentar pajak yang di-comment-out (comboboxPajak / JenisPajakBarang) sebaiknya tidak
 * dihapus sebelum dipastikan tidak dibutuhkan lagi dalam roadmap fitur.
 *
 * @author AIS Team
 * @version 1.0
 * @see PembayaranPengadaanMasterAsset
 * @see PembayaranPengadaanMasterAssetHelper
 * @see FormSop
 */
public class PembayaranPengadaanMasterAssetAction extends GenericAutowireComposer implements FormSop {

	/**
	 * Serial version UID untuk serialisasi kelas ini. Nilai ini tidak boleh diubah
	 * kecuali ada perubahan struktur field yang tidak kompatibel ke belakang.
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	/** Jendela modal ZKoss yang digunakan untuk formulir tambah/ubah pembayaran. */
	private MyWindow addWindow;

	/** Grid utama yang menampilkan daftar pembayaran pengadaan aset. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman pada grid utama. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode pembayaran. */
	private Textbox searchkode;

	/** Kotak teks pencarian berdasarkan nama atau kode penyedia barang/jasa. */
	private Textbox searchpenyedia;

	/** Kotak teks pencarian berdasarkan keterangan pembayaran. */
	private Textbox searchketerangan;

	/** Komponen datebox batas awal rentang tanggal pencarian. */
	private MyDatebox start;

	/** Komponen datebox batas akhir rentang tanggal pencarian. */
	private MyDatebox end;

	/** Label yang menampilkan kode pembayaran (hanya-baca di formulir). */
	private Label kode;

	/** Kotak teks multi-baris untuk keterangan pembayaran. */
	private MyTextbox keterangan;

	/** Datebox untuk tanggal pembuatan dokumen pembayaran. */
	private MyDatebox tanggalPembuatan;

	/** Flag yang menunjukkan apakah pengguna saat ini memiliki hak ubah (UPDATE). */
	private boolean edit = false;

	/** Flag yang menunjukkan apakah pengguna saat ini memiliki hak hapus (DELETE). */
	private boolean delete = false;

	/** Flag yang menunjukkan apakah pengguna saat ini memiliki hak menyetujui (APPROVE). */
	private boolean approve = false;

	/** Flag yang menunjukkan apakah pengguna saat ini memiliki hak menolak/membatalkan (REJECT). */
	private boolean reject = false;

	/** Entitas pembayaran pengadaan aset yang sedang diproses dalam formulir aktif. */
	private PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset;

	/** Tombol toolbar untuk menambah data pembayaran baru. */
	private MyToolbarbuttonConfig add;

	/** Grid detail yang menampilkan daftar tagihan/penerimaan dalam formulir tambah/ubah. */
	private MyGrid gridMasterAsset;

	/** Referensi disposisi SOP yang terkait dengan pembayaran yang sedang diproses. */
	private DisposisiSop disposisiSop = null;

	/**
	 * Flag yang menunjukkan apakah Action ini dijalankan dalam mode persetujuan (true)
	 * atau mode input biasa (false). Dalam mode persetujuan, beberapa field ditampilkan
	 * sebagai label hanya-baca dan otomatis menetapkan pengguna saat ini sebagai penyetuju.
	 */
	private boolean persetujuan = false;

	/** Komponen banbox untuk memilih penyedia barang/jasa dari daftar tersedia. */
	private AmbilDataPenyediaAssetBanbox penyediaAsset;

	/** Helper yang mengelola tampilan dan logika detail tagihan di dalam formulir. */
	private PembayaranPengadaanMasterAssetHelper pembayaranPengadaanMasterAssetHelper;

	/** Combobox untuk memilih jenis pembayaran barang (misalnya transfer bank atau tunai). */
	private Combobox jenisPembayaranBarang;

	/** Checkbox filter untuk menampilkan pembayaran yang belum disetujui. */
	private MyCheckboxConfig blmDisetujui;

	/** Checkbox filter untuk menampilkan pembayaran yang sudah disetujui. */
	private MyCheckboxConfig disetujui;

	/**
	 * Konstruktor tanpa argumen yang membuat instance Action dalam mode input biasa
	 * (persetujuan = false). Mode ini digunakan ketika halaman diakses oleh operator
	 * yang bertugas memasukkan data pembayaran.
	 */
	public PembayaranPengadaanMasterAssetAction() {

	}

	/**
	 * Konstruktor yang memungkinkan pengaturan mode persetujuan secara eksplisit.
	 * Mode persetujuan (persetujuan = true) digunakan ketika halaman diakses oleh
	 * pejabat yang berwenang untuk menyetujui pembayaran yang telah diinput oleh operator.
	 * Dalam mode ini, sebagian besar field formulir ditampilkan sebagai label hanya-baca
	 * dan identitas penyetuju beserta tanggal persetujuan langsung diisi secara otomatis.
	 *
	 * @param persetujuan {@code true} jika Action ini dijalankan dalam mode persetujuan;
	 *                    {@code false} untuk mode input biasa.
	 */
	public PembayaranPengadaanMasterAssetAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/**
	 * Dipanggil oleh ZKoss sebelum komposisi komponen dimulai, sebagai interceptor
	 * awal untuk validasi keamanan. Metode ini memanggil {@link Common#doCheckSecurity()}
	 * yang memeriksa token sesi dan memastikan request berasal dari sesi yang valid.
	 * Jika pemeriksaan gagal, pengguna akan diarahkan ke halaman logoff. Metode ini
	 * selalu memanggil implementasi super untuk melanjutkan proses komposisi ZKoss.
	 *
	 * <p><b>Tujuan:</b> Mencegah akses tidak sah ke halaman pembayaran pengadaan aset
	 * sebelum komponen ZUL sepenuhnya terbentuk.</p>
	 *
	 * <p><b>Cara kerja:</b> Dipanggil oleh framework ZKoss sebagai lifecycle callback.
	 * Memanggil {@code Common.doCheckSecurity()} yang memeriksa atribut sesi, kemudian
	 * mendelegasikan ke {@code super.doBeforeCompose} untuk proses autowiring standar.</p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code doCheckSecurity} melempar exception atau
	 * mengarahkan ke logoff, proses komposisi akan dihentikan oleh ZKoss framework.</p>
	 *
	 * @param page     halaman ZKoss yang sedang dimuat.
	 * @param parent   komponen induk dalam hierarki komponen ZKoss.
	 * @param compInfo metadata komponen yang akan dibuat.
	 * @return {@code ComponentInfo} dari super yang diteruskan ke framework ZKoss.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Lifecycle callback ZKoss yang dipanggil setelah seluruh komponen halaman selesai
	 * dirakit (compose). Metode ini merupakan titik inisialisasi utama untuk halaman
	 * pembayaran pengadaan aset.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi hak akses pengguna, menyiapkan rentang tanggal
	 * default, mendaftarkan listener paging, dan memuat data awal ke dalam grid.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Pertama, metode memeriksa atribut sesi {@code usersTemp} dan hak READ. Jika tidak
	 * ada, sesi dibersihkan dan pengguna diarahkan ke halaman logoff. Jika valid, metode
	 * mengatur:
	 * <ol>
	 *   <li>Datebox {@code start} dan {@code end} menjadi readonly dan diisi dengan
	 *       rentang 6 bulan ke belakang hingga hari berikutnya.</li>
	 *   <li>Visibilitas tombol {@code add} berdasarkan hak CREATE pengguna.</li>
	 *   <li>Flag {@code edit}, {@code delete}, {@code approve}, dan {@code reject}
	 *       berdasarkan hak UPDATE, DELETE, APPROVE, dan REJECT pengguna.</li>
	 *   <li>Listener paging menggunakan {@link Common#initPaging} agar navigasi halaman
	 *       memicu {@link #onSearchDefault(Event)}.</li>
	 *   <li>Timer default menggunakan {@link Common#createDefaultTimer} untuk memuat
	 *       data pertama kali setelah halaman selesai dirender.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika komponen {@code add} adalah null (halaman dikonfigurasi
	 * tanpa tombol tambah), metode keluar lebih awal tanpa error. Exception dari super
	 * diteruskan ke caller (ZKoss framework).</p>
	 *
	 * @param comp komponen root hasil komposisi ZKoss.
	 * @throws Exception jika terjadi error pada proses inisialisasi komponen super.
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

		if (add == null) return;
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
	 * Kelas inner yang bertanggung jawab merender setiap baris grid daftar pembayaran
	 * pengadaan aset. Kelas ini mengimplementasikan {@code MyRowRenderer} sehingga dapat
	 * digunakan langsung oleh komponen {@link MyGrid}.
	 *
	 * <p><b>Untuk apa:</b> Menampilkan informasi lengkap satu catatan
	 * {@link PembayaranPengadaanMasterAsset} dalam sebuah baris grid, meliputi kode,
	 * keterangan, penyedia, nilai tagihan, pajak, nilai dibayar, jenis pembayaran, pembuat,
	 * penyetuju, status aktif, serta tombol aksi (cetak, setujui, batalkan, ubah, hapus).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Metode {@link #render(Row, Object)} dipanggil oleh {@link MyGrid} untuk setiap elemen
	 * dalam model. Setiap pemanggilan membuat elemen UI secara programatik menggunakan ZKoss
	 * komponen (Label, Vbox, Hbox, A, MyToolbarbuttonConfig) dan menambahkannya ke baris.
	 * Lampiran dokumen ditampilkan melalui {@link LampiranLain#createDownloadUploadFileLain}.
	 * Detail tagihan dimuat melalui query Hibernate untuk
	 * {@link PembayaranPengadaanMasterAssetDetail}. Status daftar pengajuan transfer ditampilkan
	 * menggunakan {@link DaftarPengajuanTransfer#tampilStatus}. Tombol setujui dan batalkan
	 * memperbarui field {@code disetujuiOleh} dan {@code tanggalPersetujuan} pada entitas
	 * secara langsung dan menyimpannya ke database, lalu memperbarui tampilan label di baris
	 * yang sama tanpa reload penuh.</p>
	 *
	 * <p><b>Threading:</b> Renderer ini dijalankan di thread ZKoss event-thread. Query
	 * Hibernate dilakukan menggunakan {@link HibernateUtil#currentSession()} yang terikat
	 * ke sesi Hibernate aktif thread tersebut.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika kolom grid diubah di ZUL, jumlah dan urutan elemen
	 * yang ditambahkan ke baris dalam metode {@code render} harus disesuaikan. Listener
	 * inline pada tombol menggunakan referensi ke variabel lokal yang bersifat
	 * effectively-final karena berjalan di Java 1.7 dengan kelas anonim.</p>
	 */
	class PembayaranPengadaanMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris grid untuk entitas {@link PembayaranPengadaanMasterAsset}
		 * yang diberikan. Metode ini membangun seluruh sel baris secara programatik,
		 * termasuk ikon lampiran, detail penerimaan/saldo awal, nama penyedia, angka
		 * keuangan, informasi pembuat dan penyetuju, keterangan SOP, status aktif,
		 * dan toolbar aksi dengan tombol cetak, setujui, batalkan, ubah, dan hapus.
		 *
		 * <p><b>Tujuan:</b> Mengubah satu objek entitas pembayaran pengadaan menjadi
		 * representasi visual baris grid yang interaktif dan informatif.</p>
		 *
		 * <p><b>Cara kerja:</b><br>
		 * Urutan sel yang dihasilkan:
		 * <ol>
		 *   <li>Kolom kode: Vbox berisi helper revisi dan lampiran dokumen pembayaran.</li>
		 *   <li>Kolom tagihan: Vbox berisi daftar kode + keterangan penerimaan/saldo awal
		 *       beserta lampiran tagihan masing-masing.</li>
		 *   <li>Kolom penyedia: nama penyedia barang/jasa.</li>
		 *   <li>Kolom nilai tagihan: angka dengan format ribuan.</li>
		 *   <li>Kolom total pajak: angka dengan format ribuan.</li>
		 *   <li>Kolom nilai dibayar: angka dengan format ribuan.</li>
		 *   <li>Kolom jenis pembayaran: nama jenis atau teks default "Daftar Pengajuan Transfer".</li>
		 *   <li>Kolom pembuat: nama dan tanggal pembuatan.</li>
		 *   <li>Kolom penyetuju: nama dan tanggal persetujuan (label yang dapat diperbarui
		 *       secara reaktif oleh tombol setujui/batalkan).</li>
		 *   <li>Kolom keterangan: teks keterangan, link SOP jika ada, dan status transfer.</li>
		 *   <li>Kolom status aktif: checkbox jika belum disetujui dan punya hak ubah,
		 *       atau label "Ya"/"Tidak"/"Tidak aktif".</li>
		 *   <li>Kolom toolbar: tombol cetak, setujui, batalkan, ubah, hapus.</li>
		 * </ol>
		 * </p>
		 *
		 * <p><b>Penanganan error:</b> Hapus data dilindungi oleh blok try-catch;
		 * jika terjadi error (misalnya foreign key constraint), pesan error ditampilkan
		 * menggunakan {@link Common#tampilErrorJikaAdmin} dan dialog informatif ditampilkan
		 * kepada pengguna.</p>
		 *
		 * @param arg0 baris ZKoss {@link Row} yang akan diisi dengan komponen UI.
		 * @param arg1 objek {@link PembayaranPengadaanMasterAsset} yang akan dirender.
		 * @throws Exception jika terjadi error saat membangun komponen UI atau mengakses database.
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset = (PembayaranPengadaanMasterAsset) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PembayaranPengadaanMasterAsset.class, pembayaranPengadaanMasterAsset,
					pembayaranPengadaanMasterAsset.getKode())).setParent(arg0);

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, pembayaranPengadaanMasterAsset.getId(),
					PembayaranPengadaanMasterAsset.class.getName(), "Dokumen", false, null, null, false, false, false,
					false);

			Session session = HibernateUtil.currentSession();
			List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = session
					.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pembayaranPengadaanMasterAsset", pembayaranPengadaanMasterAsset)).list();
			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {

				if (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null) {

					vbox.appendChild(new Label(
							pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode()));
					vbox.appendChild(new Space());
					vbox.appendChild(new Label(
							pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKeterangan()));

					Hbox hboxa = new Hbox();
					hboxa.setParent(vbox);
					LampiranLain.createDownloadUploadFileLain(hboxa,
							pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getId(),
							PenerimaanPengadaanMasterAsset.class.getName(), "Tagihan", false, null, null, false, false,
							false, false);
				} else if (pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset() != null) {
					vbox.appendChild(
							new Label(pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getKode()));
					vbox.appendChild(new Space());
					vbox.appendChild(
							new Label(pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getKeterangan()));
				}
			}

			new Label(pembayaranPengadaanMasterAsset.getPenyedia() == null ? ""
					: pembayaranPengadaanMasterAsset.getPenyedia().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(pembayaranPengadaanMasterAsset.getNilaiTagihan()))
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(pembayaranPengadaanMasterAsset.getTotalPajak())).setParent(arg0);
			new Label(Common.numberFormat.get().format(pembayaranPengadaanMasterAsset.getNilaiDibayar()))
					.setParent(arg0);

			new Label(pembayaranPengadaanMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranPengadaanMasterAsset.getJenisPembayaranBarang().getNama()).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(pembayaranPengadaanMasterAsset.getDibuatOleh() == null ? ""
					: pembayaranPengadaanMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(pembayaranPengadaanMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranPengadaanMasterAsset.getTanggalPembuatan()))
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null ? ""
					: pembayaranPengadaanMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(pembayaranPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(pembayaranPengadaanMasterAsset.getTanggalPersetujuan())))
					.setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(pembayaranPengadaanMasterAsset.getKeterangan())).setParent(vbox1);
			if (pembayaranPengadaanMasterAsset.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa,
						"SOP " + pembayaranPengadaanMasterAsset.getDisposisiSop().getKeterangan() + " ("
								+ pembayaranPengadaanMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pembayaranPengadaanMasterAsset.getDisposisiSop().getId(), null,
								null, true, arg0.getTarget());
					}
				});
			}

			if (pembayaranPengadaanMasterAsset.getDisposisiSop() != null
					&& !pembayaranPengadaanMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(pembayaranPengadaanMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pembayaranPengadaanMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(pembayaranPengadaanMasterAsset);
					}
				});
			} else {
				new Label(pembayaranPengadaanMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			DaftarPengajuanTransfer daftarPengajuanTransfer = (DaftarPengajuanTransfer) HibernateUtil.currentSession()
					.createCriteria(DaftarPengajuanTransfer.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.desc("id"))
					.createAlias("pembayaranPengadaanMasterAssetDetail", "pembayaranPengadaanMasterAssetDetail")
					.add(Restrictions.eq("pembayaranPengadaanMasterAssetDetail.pembayaranPengadaanMasterAsset",
							pembayaranPengadaanMasterAsset))
					.setMaxResults(1).uniqueResult();
			DaftarPengajuanTransfer.tampilStatus(daftarPengajuanTransfer, vbox1);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Pembayaran Pengadaan Asset");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(pembayaranPengadaanMasterAsset);
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && pembayaranPengadaanMasterAsset.getDisetujuiOleh() != null);

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

										pembayaranPengadaanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										pembayaranPengadaanMasterAsset
												.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());

										Common.refreshUpdate(session, pembayaranPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												pembayaranPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(pembayaranPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: pembayaranPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranPengadaanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);

										cetak(pembayaranPengadaanMasterAsset);
									}
								}
							});
				}

			});
			disetujui.setParent(toolbar);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Pembayaran Pengadaan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										pembayaranPengadaanMasterAsset.setDisetujuiOleh(null);
										pembayaranPengadaanMasterAsset.setTanggalPersetujuan(null);

										Common.refreshUpdate(session, pembayaranPengadaanMasterAsset);

										disetujuiTanggal.setValue(
												pembayaranPengadaanMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get().format(pembayaranPengadaanMasterAsset
																.getTanggalPersetujuan()));
										disetujuiOleh.setValue(pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null
												? ""
												: pembayaranPengadaanMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui.setVisible(
												approve && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
										dibatalkan.setVisible(
												reject && pembayaranPengadaanMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(
												edit && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(
												delete && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);

									}
								}
							});
				}

			});
			dibatalkan.setParent(toolbar);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembayaranPengadaanMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null);
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
													pembayaranPengadaanMasterAsset.getDisposisiSop())) {
												List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = session
														.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
														.add(Restrictions.eq("pembayaranPengadaanMasterAsset",
																pembayaranPengadaanMasterAsset))
														.list();
												for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {
													session.delete(pembayaranPengadaanMasterAssetDetail);
												}
												session.flush();

												Common.createDefaultTimer(new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Session session = HibernateUtil.currentSession();
														Common.refreshDelete(session, pembayaranPengadaanMasterAsset);
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
			hapus.setParent(toolbar);
			toolbar.setParent(arg0);
		}
	}

	/**
	 * Event handler yang dipanggil ketika pengguna mengklik tombol Tambah pada toolbar
	 * halaman utama. Metode ini membuat instance entitas pembayaran baru (kosong) dan
	 * membuka jendela modal formulir dalam mode input baru.
	 *
	 * <p><b>Tujuan:</b> Menyediakan jalur masuk untuk penginputan data pembayaran
	 * pengadaan aset baru oleh operator.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link #init(PembayaranPengadaanMasterAsset)}
	 * dengan objek {@link PembayaranPengadaanMasterAsset} baru (ID null) untuk mengosongkan
	 * formulir, lalu menampilkan {@code addWindow} sebagai modal.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception diteruskan ke framework ZKoss untuk ditangani
	 * oleh error handler global.</p>
	 *
	 * @param event event ZKoss dari komponen yang memanggil handler ini (tombol Tambah).
	 * @throws Exception jika terjadi error saat inisialisasi formulir atau membuka modal.
	 */
	public void onAdd(Event event) throws Exception {
		init(new PembayaranPengadaanMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Menginisialisasi dan mengisi jendela modal formulir dengan data pembayaran pengadaan
	 * yang diberikan. Metode ini digunakan baik untuk mode tambah (entitas baru) maupun
	 * mode ubah (entitas yang sudah ada).
	 *
	 * <p><b>Tujuan:</b> Menyiapkan semua komponen formulir (field input, grid detail,
	 * tombol simpan dan batal) dalam jendela modal {@code addWindow} sebelum ditampilkan
	 * kepada pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Menyimpan referensi entitas dan mereset {@code disposisiSop} ke null.</li>
	 *   <li>Membersihkan konten {@code addWindow} menggunakan {@link Common#clear}.</li>
	 *   <li>Membangun tata letak Borderlayout dengan Center (formulir) dan South (toolbar).</li>
	 *   <li>Memanggil {@link #form(GeneralValueObject, DisposisiSop, MyToolbarbuttonConfig, EventListener)}
	 *       untuk membuat komponen formulir input dan meletakkannya di Center.</li>
	 *   <li>Menambahkan tombol Batal (menutup modal) dan Simpan (memanggil {@link #onSave(Event)})
	 *       ke toolbar South.</li>
	 *   <li>Jika simpan berhasil, memperbarui paging dan menutup modal.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari {@code form()} diteruskan ke caller.
	 * Tombol Batal tidak melempar exception karena hanya mengatur visibilitas.</p>
	 *
	 * @param pembayaranPengadaanMasterAsset entitas pembayaran yang akan diisikan ke formulir;
	 *                                       gunakan objek baru dengan ID null untuk mode tambah,
	 *                                       atau entitas yang sudah ada untuk mode ubah.
	 * @throws Exception jika terjadi error saat membangun komponen formulir.
	 */
	private void init(PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset) throws Exception {
		this.pembayaranPengadaanMasterAsset = pembayaranPengadaanMasterAsset;
		addWindow.setTitle("Proses Pembayaran Barang/Jasa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(pembayaranPengadaanMasterAsset, disposisiSop, save, null));

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
	 * Memproses penyimpanan data pembayaran pengadaan aset dari formulir. Metode ini
	 * melakukan validasi input, menghitung total tagihan dan pajak, menyimpan header
	 * pembayaran, menyimpan atau memperbarui setiap detail tagihan, memperbarui status
	 * pembayaran pada pemesanan terkait, dan memicu pencetakan dokumen otomatis.
	 *
	 * <p><b>Tujuan:</b> Menjadi titik simpan tunggal yang mengorkestrasi seluruh operasi
	 * database untuk satu transaksi pembayaran pengadaan aset secara atomik.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Validasi: penyedia harus dipilih, kode tidak boleh kosong, harus ada minimal
	 *       satu tagihan yang dipilih dengan nilai > 0, dan harus ada nilai pembayaran > 0.</li>
	 *   <li>Mengambil entitas dari database jika mode ubah (ID tidak null), menggunakan
	 *       {@code currentNativeSession} untuk menghindari konflik sesi.</li>
	 *   <li>Menetapkan disposisi SOP, jenis pembayaran, nilai dibayar, kode, keterangan,
	 *       tanggal, dan penyedia ke entitas header.</li>
	 *   <li>Jika mode persetujuan, menetapkan penyetuju dan tanggal persetujuan secara
	 *       otomatis.</li>
	 *   <li>Menghitung total pajak dari semua detail yang dipilih.</li>
	 *   <li>Menyimpan header dengan {@code session.save} (baru) atau {@code session.update}
	 *       (ubah) dalam transaksi terpisah.</li>
	 *   <li>Untuk setiap detail yang dipilih: menyimpan/memperbarui detail, memperbarui
	 *       field {@code dibayar} pada {@link SaldoAwalMasterAsset} terkait, dan memperbarui
	 *       field {@code dibayar} pada {@link PemesananPengadaanMasterAsset} terkait.</li>
	 *   <li>Jika jenis pembayaran null atau detail tidak punya daftar pengajuan transfer,
	 *       membuat entri {@link DaftarPengajuanTransfer} baru.</li>
	 *   <li>Menutup sesi native dan memicu pencetakan otomatis dengan delay 2500ms.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Validasi menampilkan dialog peringatan dan mengembalikan
	 * {@code false} tanpa melempar exception. Exception database diteruskan ke caller.</p>
	 *
	 * @param event event ZKoss dari tombol Simpan (tidak digunakan langsung dalam metode ini).
	 * @return {@code true} jika data berhasil disimpan dan modal boleh ditutup;
	 *         {@code false} jika validasi gagal dan pengguna perlu memperbaiki input.
	 * @throws Exception jika terjadi error database selama proses penyimpanan.
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {

		if (penyediaAsset.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari dari daftar penyedia terdaftar; (2) Jika penyedia belum ada, daftarkan melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Pembayaran Pengadaan belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode Pembayaran atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Double nilai = 0.0;
		Double d = 0.0;
		List<Row> rowsMasterAsset = gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {

			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = (PembayaranPengadaanMasterAssetDetail) row
						.getAttribute("pembayaranPengadaanMasterAssetDetail");

				PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
						.getPenerimaanPengadaanMasterAsset();

				Double nilaitagihan = penerimaanPengadaanMasterAsset == null
						? pembayaranPengadaanMasterAssetDetail.getSaldoAwalMasterAsset().getNilai()
						:

						penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset() == null
								? penerimaanPengadaanMasterAsset.getNilai()
								: penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset().getNilai();

				nilai += nilaitagihan;
				d += pembayaranPengadaanMasterAssetDetail.getDibayar();
			}
		}

		if (nilai.intValue() == 0) {
			MyMessageboxConfig.show("Mohon maaf, belum ada tagihan pengadaan yang dipilih. Langkah yang dapat dilakukan: (1) Centang tagihan yang ingin dibayarkan dari daftar tagihan penerimaan; (2) Pastikan tagihan sudah tersedia dan belum lunas; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (d.intValue() == 0) {
			MyMessageboxConfig.show("Mohon maaf, jumlah riwayat pembayaran belum diisi. Langkah yang dapat dilakukan: (1) Isi nilai pembayaran pada field yang tersedia; (2) Pastikan nilai yang dimasukkan lebih dari nol; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentNativeSession();
		if (pembayaranPengadaanMasterAsset.getId() != null) {
			pembayaranPengadaanMasterAsset = (PembayaranPengadaanMasterAsset) session
					.load(PembayaranPengadaanMasterAsset.class, pembayaranPengadaanMasterAsset.getId());

		}

		if (disposisiSop != null && disposisiSop.getId() != null) {
			pembayaranPengadaanMasterAsset.setDisposisiSop(disposisiSop);
		}

		pembayaranPengadaanMasterAsset.setJenisPembayaranBarang(
				(JenisPembayaranBarang) (jenisPembayaranBarang.getSelectedItem() == null ? null
						: jenisPembayaranBarang.getSelectedItem().getValue()));
		pembayaranPengadaanMasterAsset.setNilaiDibayar(d);

		pembayaranPengadaanMasterAsset.setKode(kode.getValue());
		pembayaranPengadaanMasterAsset.setKeterangan(keterangan.getValue());
		pembayaranPengadaanMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
		pembayaranPengadaanMasterAsset.setPenyedia((PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset"));

		if (persetujuan) {
			pembayaranPengadaanMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
			pembayaranPengadaanMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		}

		Double totalPajak = 0.0;

		for (Row row : rowsMasterAsset) {

			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = (PembayaranPengadaanMasterAssetDetail) row
						.getAttribute("pembayaranPengadaanMasterAssetDetail");
				totalPajak += pembayaranPengadaanMasterAssetDetail.hitungPajak();
			}
		}
		pembayaranPengadaanMasterAsset.setTotalPajak(totalPajak);
		pembayaranPengadaanMasterAsset.setNilaiTagihan(nilai);

		if (pembayaranPengadaanMasterAsset.getId() != null) {
			session.getTransaction().begin();
			session.update(pembayaranPengadaanMasterAsset);
			session.getTransaction().commit();
		} else {
			pembayaranPengadaanMasterAsset.setDibuatOleh(Common.getCurrentUser());
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			pembayaranPengadaanMasterAsset.setKode(kode.getValue());
			session.getTransaction().begin();
			session.save(pembayaranPengadaanMasterAsset);
			session.getTransaction().commit();
		}

		for (Row row : rowsMasterAsset) {
			MyCheckboxConfig pilih = (MyCheckboxConfig) row.getAttribute("pilih");

			if (pilih.isChecked()) {
				PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail = (PembayaranPengadaanMasterAssetDetail) row
						.getAttribute("pembayaranPengadaanMasterAssetDetail");
				pembayaranPengadaanMasterAssetDetail.setPembayaranPengadaanMasterAsset(pembayaranPengadaanMasterAsset);

				session.getTransaction().begin();
				if (pembayaranPengadaanMasterAssetDetail.getId() == null) {
					session.save(pembayaranPengadaanMasterAssetDetail);
				} else {
					Common.refreshUpdate(session, pembayaranPengadaanMasterAssetDetail);
				}
				session.getTransaction().commit();

				SaldoAwalMasterAsset saldoAwalMasterAsset = pembayaranPengadaanMasterAssetDetail
						.getSaldoAwalMasterAsset();
				if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
					Number nilaiTagihan = (Number) session.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
							.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset))
							.setProjection(Projections.sum("dibayar")).uniqueResult();

					pembayaranPengadaanMasterAssetDetail
							.setTotalTelahDibayar(nilaiTagihan == null ? 0.0 : nilaiTagihan.doubleValue());

					SaldoAwalMasterAsset saldoAwalMasterAsset1 = (SaldoAwalMasterAsset) session
							.createCriteria(SaldoAwalMasterAsset.class)
							.add(Restrictions.idEq(saldoAwalMasterAsset.getId())).uniqueResult();

					saldoAwalMasterAsset1.setDibayar(pembayaranPengadaanMasterAssetDetail.getTotalTelahDibayar());

					session.getTransaction().begin();
					Common.refreshUpdate(session, saldoAwalMasterAsset1);
					Common.refreshUpdate(session, pembayaranPengadaanMasterAssetDetail);
					session.getTransaction().commit();
				}

				if (pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset() != null) {
					PemesananPengadaanMasterAsset pemesananPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset();
					session.refresh(pemesananPengadaanMasterAsset);
					Double dibayar = pemesananPengadaanMasterAsset.hitungDibayar(session);

					if (dibayar.intValue() != pemesananPengadaanMasterAsset.getDibayar().intValue()) {
						pemesananPengadaanMasterAsset.setDibayar(dibayar);
						session.getTransaction().begin();
						Common.refreshUpdate(session, pemesananPengadaanMasterAsset);
						session.getTransaction().commit();
					}
				}
				if (pembayaranPengadaanMasterAsset.getJenisPembayaranBarang() == null
						|| pembayaranPengadaanMasterAssetDetail.getDaftarPengajuanTransfer() == null) {
					DaftarPengajuanTransfer
							.simpanPembayaranPengadaanMasterAssetDetail(pembayaranPengadaanMasterAssetDetail);
				}
			}
		}

		ais.common.Common.closeOpenedSession(session);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				cetak(pembayaranPengadaanMasterAsset);
			}
		}, "Proses cetak", false, 2500);

		return true;
	}

	/**
	 * Membangun peta parameter yang akan digunakan oleh template laporan Jasper untuk
	 * mencetak dokumen pembayaran pengadaan aset. Metode ini mengumpulkan seluruh data
	 * yang diperlukan: informasi header pembayaran, data penyedia termasuk akun bank dan
	 * dokumen yang dimiliki, alur disposisi SOP, dan daftar detail tagihan.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu titik agregasi data laporan sehingga template
	 * Jasper ("asset/pembayaran_pengadaan") mendapatkan semua informasi yang dibutuhkan
	 * tanpa perlu query tambahan dari sisi template.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Me-refresh entitas header dari database jika ID-nya tidak null.</li>
	 *   <li>Membuat Map parameter menggunakan {@code HashMapGenerator.getRand()} dan memasukkan
	 *       properti header menggunakan {@link Common#insertProperty}.</li>
	 *   <li>Jika penyedia ada, mem-parse JSON bank penyedia dan memasukkan setiap field
	 *       dengan prefix {@code bank_N.fieldName} ke dalam parameter.</li>
	 *   <li>Memuat semua jenis dokumen dari {@link ConstantValues} dan mencari dokumen
	 *       yang dimiliki penyedia, memasukkan keterangannya dengan prefix {@code dokumen.nama}.</li>
	 *   <li>Memanggil {@link DisposisiAlurSop#parameterMap} untuk menambahkan parameter
	 *       terkait alur SOP.</li>
	 *   <li>Untuk setiap {@link PembayaranPengadaanMasterAssetDetail}, membangun sub-map
	 *       dengan properti detail, nilai tagihan, nilai dibayar, tanggal, status persetujuan,
	 *       kode, penyedia, jenis penerimaan, dan grup ("Data Tagihan").</li>
	 *   <li>Menghapus key yang mengandung "disposisiSop" dari parameter untuk menghindari
	 *       referensi objek Hibernate yang tidak dapat diserialisasi oleh Jasper.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Parsing JSON bank penyedia dibungkus dalam try-catch
	 * sehingga jika format bank tidak valid, parameter bank akan diabaikan tanpa menghentikan
	 * proses cetak.</p>
	 *
	 * @param pembayaranPengadaanMasterAsset entitas pembayaran yang akan dicetak.
	 * @return {@code Map} berisi semua parameter yang dibutuhkan template laporan Jasper.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map parameter(PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset) {
		if (pembayaranPengadaanMasterAsset != null && pembayaranPengadaanMasterAsset.getId() != null) {
			HibernateUtil.currentSession().refresh(pembayaranPengadaanMasterAsset);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", pembayaranPengadaanMasterAsset.getId());

		Common.insertProperty(PembayaranPengadaanMasterAsset.class, pembayaranPengadaanMasterAsset, parameters, "data");

		if (pembayaranPengadaanMasterAsset.getPenyedia() != null) {

			try {
				JSONArray array = new JSONArray(pembayaranPengadaanMasterAsset.getPenyedia().getBank());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Iterator<String> iter = jsonObject.keys();
					while (iter.hasNext()) {
						String key = iter.next();
						parameters.put("bank_" + i + "." + key, jsonObject.get(key));
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/PembayaranPengadaanMasterAssetAction.java:1122");
				// Jika format JSON bank penyedia tidak valid, abaikan parameter bank
			}

			Map<Long, DokumenPenyediaAsset> map = ConstantValues.ambilBerdasarClass(DokumenPenyediaAsset.class);
			List<DokumenPenyediaAsset> dokumenPenyediaAssets = new ArrayList<DokumenPenyediaAsset>();
			for (DokumenPenyediaAsset dokumenPenyediaAsset : map.values()) {
				dokumenPenyediaAssets.add(dokumenPenyediaAsset);
			}
			PenyediaAsset penyediaAsset = pembayaranPengadaanMasterAsset.getPenyedia();
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

		DisposisiAlurSop.parameterMap(pembayaranPengadaanMasterAsset.getDisposisiSop(), parameters);
		Session session = HibernateUtil.currentSession();

		List<Map> maps = new ArrayList<Map>();

		List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = session
				.createCriteria(PembayaranPengadaanMasterAssetDetail.class)
				.add(Restrictions.eq("pembayaranPengadaanMasterAsset", pembayaranPengadaanMasterAsset)).list();

		for (PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {

			PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = pembayaranPengadaanMasterAssetDetail
					.getPenerimaanPengadaanMasterAsset();
			Map map = new HashMap();
			Common.insertProperty(PembayaranPengadaanMasterAssetDetail.class, pembayaranPengadaanMasterAssetDetail, map,
					"data");
			Double nilaitagihan = penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset() == null
					? penerimaanPengadaanMasterAsset.getNilai()
					: penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset().getNilai();

			map.put("tagihan", nilaitagihan);
			map.put("nilai", pembayaranPengadaanMasterAssetDetail.getDibayar());
			map.put("tgl",
					penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset() != null
							? penerimaanPengadaanMasterAsset.getSaldoAwalMasterAsset().getTanggalPembuatan()
							: penerimaanPengadaanMasterAsset.getTanggalPembuatan());

			map.put("status_persetujuan", pembayaranPengadaanMasterAsset.getDisetujuiOleh() == null ? "Belum disetujui"
					: "Telah disetujui oleh " + pembayaranPengadaanMasterAsset.getDisetujuiOleh().getUserNama());

			map.put("perpustakaan", pembayaranPengadaanMasterAsset.getKeterangan());
			map.put("tanggal_persetujuan", pembayaranPengadaanMasterAsset.getTanggalPersetujuan());
			map.put("kode_tagihan", penerimaanPengadaanMasterAsset.getKodeTagihan());
			map.put("kode", pembayaranPengadaanMasterAsset.getKode());
			map.put("penyedia", pembayaranPengadaanMasterAsset.getPenyedia() == null ? ""
					: pembayaranPengadaanMasterAsset.getPenyedia().getNama());

			map.put("jenisPembayaranBarang",
					pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKode() + " "
							+ pembayaranPengadaanMasterAssetDetail.getPenerimaanPengadaanMasterAsset().getKeterangan());

			map.put("grup", "Data Tagihan");

			map.put("jenis_pemesanan_pengadaan_asset", pembayaranPengadaanMasterAssetDetail.getKeterangan());
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
	 * Mengimplementasikan metode dari antarmuka {@link FormSop} untuk mencetak data
	 * pembayaran pengadaan aset ke dalam file laporan PDF. Metode ini dipanggil oleh
	 * framework SOP ketika laporan perlu dibuat sebagai file yang dapat diunduh.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan file PDF laporan pembayaran pengadaan berdasarkan
	 * template "asset/pembayaran_pengadaan" menggunakan parameter yang dibangun oleh
	 * {@link #parameter(PembayaranPengadaanMasterAsset)}.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengkonversi parameter {@code generalValueObject} ke tipe
	 * {@link PembayaranPengadaanMasterAsset}, membangun peta parameter laporan, lalu
	 * mendelegasikan ke {@link Report#generateFileReport} untuk membuat file PDF.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari {@code Report.generateFileReport}
	 * diteruskan ke caller (framework SOP).</p>
	 *
	 * @param generalValueObject entitas {@link PembayaranPengadaanMasterAsset} yang datanya
	 *                           akan dicetak ke dalam laporan PDF.
	 * @return objek {@link File} yang menunjuk ke file PDF laporan yang telah dibuat.
	 * @throws Exception jika terjadi error saat pembuatan laporan PDF.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset = (PembayaranPengadaanMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(pembayaranPengadaanMasterAsset),
				"asset/pembayaran_pengadaan", pembayaranPengadaanMasterAsset.getTanggalPembuatan(), maps,
				Common.locale);
		return file;
	}

	/**
	 * Memicu pencetakan dan penampilan laporan PDF pembayaran pengadaan aset langsung
	 * di browser pengguna. Metode ini bersifat statis sehingga dapat dipanggil dari
	 * konteks yang tidak memiliki referensi ke instance Action, seperti dari event handler
	 * anonim dalam renderer.
	 *
	 * <p><b>Tujuan:</b> Menampilkan dokumen bukti pembayaran kepada pengguna segera setelah
	 * data berhasil disimpan atau setelah pengguna mengklik tombol cetak pada baris grid.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil {@link Report#generatePDFReport} dengan template
	 * "asset/pembayaran_pengadaan" dan parameter yang dibangun oleh
	 * {@link #parameter(PembayaranPengadaanMasterAsset)}. Laporan langsung dikirimkan
	 * ke browser melalui mekanisme download ZKoss.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception diteruskan ke caller. Pemanggil yang bersifat
	 * event-handler harus menangani exception secara lokal.</p>
	 *
	 * @param pembayaranPengadaanMasterAsset entitas pembayaran yang akan dicetak.
	 * @throws Exception jika terjadi error saat membuat atau mengirimkan laporan PDF.
	 */
	public static void cetak(PembayaranPengadaanMasterAsset pembayaranPengadaanMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(pembayaranPengadaanMasterAsset), "asset/pembayaran_pengadaan",
				pembayaranPengadaanMasterAsset.getTanggalPembuatan());
	}

	/** Checkbox filter untuk menampilkan atau menyembunyikan data aktif/tidak aktif. */
	private Checkbox searchaktif;

	/**
	 * Membangun dan mengembalikan objek {@link Criteria} Hibernate yang digunakan untuk
	 * query data {@link PembayaranPengadaanMasterAsset} sesuai filter yang aktif saat ini.
	 * Kriteria ini digunakan baik untuk menghitung total record (paging) maupun mengambil
	 * data per halaman.
	 *
	 * <p><b>Tujuan:</b> Memusatkan logika filter pencarian sehingga query untuk paging
	 * (count) dan query untuk data (list) menggunakan kriteria yang identik dan konsisten.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Filter yang diterapkan secara berurutan:
	 * <ol>
	 *   <li>Filter status aktif: jika {@code searchaktif} dicentang, tampilkan yang aktif
	 *       atau yang aktif-nya null; jika tidak dicentang, tampilkan semua.</li>
	 *   <li>Filter rentang tanggal: menggunakan SQL date() untuk membandingkan tanggal
	 *       pembuatan dalam rentang {@code start} hingga {@code end}.</li>
	 *   <li>Filter status persetujuan: kombinasi dari {@code blmDisetujui} dan {@code disetujui}
	 *       untuk menampilkan belum disetujui, sudah disetujui, atau keduanya.</li>
	 *   <li>Join ke tabel penyedia dengan alias "penyedia".</li>
	 *   <li>Filter penyedia: pencarian substring pada nama atau kode penyedia.</li>
	 *   <li>Filter kode pembayaran: pencarian substring pada kode.</li>
	 *   <li>Filter keterangan: pencarian substring pada keterangan.</li>
	 *   <li>Pengurutan DESC berdasarkan ID jika parameter {@code order} adalah true.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code start} atau {@code end} adalah null,
	 * metode mengembalikan null segera.</p>
	 *
	 * @param order {@code true} untuk menambahkan pengurutan DESC by ID (digunakan untuk
	 *              query data); {@code false} untuk query count tanpa pengurutan.
	 * @return {@link Criteria} Hibernate yang sudah dikonfigurasi, atau {@code null} jika
	 *         komponen tanggal belum siap.
	 */
	public Criteria initCriteria(boolean order) {
		if (start == null || end == null) return null;
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PembayaranPengadaanMasterAsset.class)

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
	 * Event handler default yang dipanggil untuk memperbarui tampilan grid dengan data
	 * yang sesuai filter pencarian saat ini. Metode ini juga dipanggil oleh listener paging
	 * dan timer inisialisasi halaman.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu titik refresh data grid yang dapat dipanggil
	 * dari berbagai konteks (inisialisasi halaman, perubahan filter, navigasi paging,
	 * dan setelah operasi simpan/hapus).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Memanggil {@link Common#initPaging} dengan kriteria tanpa urutan untuk menghitung
	 *       total record dan memperbarui komponen paging.</li>
	 *   <li>Mengambil daftar {@link PembayaranPengadaanMasterAsset} sesuai halaman aktif
	 *       dengan limit {@code Common.ROWS_COUNT_ON_PAGE}.</li>
	 *   <li>Membungkus hasil dalam {@link SimpleListModel} dan menetapkan
	 *       {@link PembayaranPengadaanMasterAssetRenderer} sebagai renderer grid.</li>
	 *   <li>Memperbarui model grid menggunakan {@code setModelCheckMobile} yang otomatis
	 *       menyesuaikan tampilan untuk perangkat mobile jika diperlukan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari query Hibernate atau renderer
	 * diteruskan ke framework ZKoss. Jika {@code initCriteria} mengembalikan null
	 * (karena komponen tanggal belum siap), {@link Common#initPaging} akan menanganinya
	 * dengan aman.</p>
	 *
	 * @param event event ZKoss dari komponen yang memicu pencarian; dapat berupa null
	 *              ketika dipanggil secara programatik dari inisialisasi atau setelah simpan.
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<PembayaranPengadaanMasterAsset> pembayaranPengadaanMasterAsset = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranPengadaanMasterAsset);
		grid.setRowRenderer(new PembayaranPengadaanMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Membangun dan mengembalikan komponen grid formulir isian untuk data pembayaran
	 * pengadaan aset. Metode ini merupakan implementasi dari antarmuka {@link FormSop}
	 * dan dipanggil baik dari {@link #init(PembayaranPengadaanMasterAsset)} (mode internal)
	 * maupun dari framework SOP (mode eksternal).
	 *
	 * <p><b>Tujuan:</b> Membuat tampilan formulir yang lengkap dan interaktif untuk
	 * penginputan atau peninjuan data pembayaran, dengan tampilan yang berbeda antara
	 * mode input biasa dan mode persetujuan.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun grid dua kolom dengan baris-baris berikut:
	 * <ol>
	 *   <li>Baris Penyedia: {@link AmbilDataPenyediaAssetBanbox} (input) atau Label (persetujuan).
	 *       Listener pada banbox memblokir pemilihan ulang setelah penyedia dipilih dan
	 *       memuat ulang data tagihan melalui {@link PembayaranPengadaanMasterAssetHelper}.</li>
	 *   <li>Baris Kode: Label hanya-baca yang menampilkan kode yang dibangkitkan oleh
	 *       {@link #generateCode(boolean)}.</li>
	 *   <li>Baris Tanggal: {@link MyDatebox} (input) atau Label (persetujuan).</li>
	 *   <li>Baris Jenis Pembayaran: Combobox berisi {@link JenisPembayaranBarang} aktif
	 *       (input baru) atau Label (edit/persetujuan).</li>
	 *   <li>Baris Keterangan: {@link MyTextbox} multi-baris (input) atau Label (persetujuan).</li>
	 *   <li>Baris Detail Tagihan: span 2 kolom berisi grid detail dari
	 *       {@link PembayaranPengadaanMasterAssetHelper#initDetail}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari helper detail diteruskan ke caller.</p>
	 *
	 * @param generalValueObject entitas {@link PembayaranPengadaanMasterAsset} yang datanya
	 *                           akan ditampilkan dan dapat diedit di formulir.
	 * @param disposisiSop       disposisi SOP yang terkait dengan pembayaran ini;
	 *                           dapat null jika tidak ada alur SOP.
	 * @param save               tombol simpan yang akan diaktifkan setelah formulir valid.
	 * @param setujui            listener untuk tombol setujui dari framework SOP; dapat null.
	 * @return {@link MyGrid} yang berisi semua komponen formulir input pembayaran.
	 * @throws Exception jika terjadi error saat membangun komponen formulir.
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.pembayaranPengadaanMasterAsset = (PembayaranPengadaanMasterAsset) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
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
			row.appendChild(new Label(pembayaranPengadaanMasterAsset.getPenyedia() == null ? ""
					: pembayaranPengadaanMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyediaAsset);
		}

		penyediaAsset.setAttribute("penyediaAsset", pembayaranPengadaanMasterAsset.getPenyedia());
		penyediaAsset.setValue(pembayaranPengadaanMasterAsset.getPenyedia() == null ? ""
				: pembayaranPengadaanMasterAsset.getPenyedia().getNama());
		penyediaAsset.setReadonly(true);
		penyediaAsset.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode "));

		tanggalPembuatan = new MyDatebox(
				pembayaranPengadaanMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: pembayaranPengadaanMasterAsset.getTanggalPembuatan());
		if (pembayaranPengadaanMasterAsset.getKode() == null) {
			String noAgenda = generateCode(false);
			pembayaranPengadaanMasterAsset.setKode(noAgenda);
		}

		kode = new Label(pembayaranPengadaanMasterAsset.getKode());
		if (persetujuan) {
			row.appendChild(new Label(pembayaranPengadaanMasterAsset.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal "));

		if (persetujuan) {
			row.appendChild(new Label(tanggalPembuatan.getValue() == null ? ""
					: Common.dateFormat.get().format(tanggalPembuatan.getValue())));
		} else {
			row.appendChild(tanggalPembuatan);
		}
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
		Common.selectComboItem(jenisPembayaranBarang, pembayaranPengadaanMasterAsset.getJenisPembayaranBarang());

		if (pembayaranPengadaanMasterAsset.getId() != null) {

			row.appendChild(new Label(pembayaranPengadaanMasterAsset.getJenisPembayaranBarang() == null
					? Common.getBahasaConfig("Daftar Pengajuan Transfer")
					: pembayaranPengadaanMasterAsset.getJenisPembayaranBarang().getNama()));
		} else {
			row.appendChild(jenisPembayaranBarang);
		}

		row = new MyFormRow();
		if (persetujuan) {
			row.appendChild(new Label(pembayaranPengadaanMasterAsset.getKeterangan()));
		} else {
			row.setParent(rows);
		}
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Pembayaran"));
		row.appendChild(keterangan = new MyTextbox(pembayaranPengadaanMasterAsset.getKeterangan() == null ? ""
				: pembayaranPengadaanMasterAsset.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild((pembayaranPengadaanMasterAssetHelper = new PembayaranPengadaanMasterAssetHelper(
				gridMasterAsset = new MyGrid())).initDetail(pembayaranPengadaanMasterAsset, persetujuan));

		penyediaAsset.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				PenyediaAsset a = (PenyediaAsset) penyediaAsset.getAttribute("penyediaAsset");
				if (a != null) {
					penyediaAsset.setDisabled(true);
					pembayaranPengadaanMasterAssetHelper.setPenyediaAsset(a);
				} else {
					penyediaAsset.setDisabled(false);
				}
			}
		});

		return grid;
	}

	/**
	 * Mengimplementasikan metode {@link FormSop#istilah()} untuk mengembalikan label
	 * istilah yang digunakan dalam konteks alur SOP digital. Teks ini ditampilkan oleh
	 * framework SOP untuk mengidentifikasi jenis data yang sedang diproses dalam alur
	 * persetujuan.
	 *
	 * <p><b>Tujuan:</b> Memberikan nama yang deskriptif dan ramah pengguna untuk modul
	 * pembayaran ini dalam konteks alur SOP multi-modul.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan string literal tetap "Pembayaran Barang / Jasa"
	 * yang merepresentasikan jenis transaksi yang dikelola oleh Action ini.</p>
	 *
	 * @return string "Pembayaran Barang / Jasa" sebagai istilah modul dalam alur SOP.
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak antarmuka.
	 */
	@Override
	public String istilah() throws Exception {
		return "Pembayaran Barang / Jasa";
	}

	/**
	 * Mengimplementasikan metode {@link FormSop#ambil()} untuk mengembalikan entitas
	 * {@link DataSop} yang saat ini aktif dalam formulir. Metode ini digunakan oleh
	 * framework SOP untuk mendapatkan referensi entitas yang perlu dikaitkan dengan
	 * disposisi alur persetujuan.
	 *
	 * <p><b>Tujuan:</b> Menyediakan akses ke entitas aktif bagi framework SOP tanpa
	 * perlu mengetahui tipe konkret dari entitas tersebut.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan referensi ke field instance
	 * {@code pembayaranPengadaanMasterAsset} yang sudah diset oleh metode
	 * {@link #init(PembayaranPengadaanMasterAsset)} atau {@link #form}.</p>
	 *
	 * @return {@link DataSop} yang merupakan entitas {@link PembayaranPengadaanMasterAsset}
	 *         yang sedang aktif dalam sesi pengeditan saat ini.
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak antarmuka.
	 */
	@Override
	public DataSop ambil() throws Exception {
		return pembayaranPengadaanMasterAsset;
	}

	/**
	 * Mengimplementasikan metode {@link FormSop#ambilClass()} untuk mengembalikan kelas
	 * Java dari entitas yang dikelola oleh Action ini. Metode ini digunakan oleh framework
	 * SOP untuk operasi yang memerlukan refleksi tipe, seperti pembuatan query Hibernate
	 * generik atau serialisasi data SOP.
	 *
	 * <p><b>Tujuan:</b> Memberikan informasi tipe runtime entitas kepada framework SOP
	 * tanpa memerlukan casting atau pemeriksaan instanceof.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengembalikan literal kelas
	 * {@code PembayaranPengadaanMasterAsset.class} secara langsung.</p>
	 *
	 * @return {@code Class} untuk {@link PembayaranPengadaanMasterAsset}.
	 * @throws Exception tidak dilempar; deklarasi exception mengikuti kontrak antarmuka.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return PembayaranPengadaanMasterAsset.class;
	}

	/**
	 * Membangkitkan kode dokumen pembayaran pengadaan yang unik berdasarkan konfigurasi
	 * nomor surat yang berlaku. Kode dibentuk dari pola format yang didefinisikan dalam
	 * {@link NomorSuratAlurPengadaan#PEMBAYARAN_PEMBELIAN_DATA} dengan mempertimbangkan
	 * urutan, tahun, bulan, dan tanggal reset yang dikonfigurasi.
	 *
	 * <p><b>Tujuan:</b> Menghasilkan kode pembayaran yang unik dan sesuai aturan tata
	 * naskah instansi, menggantikan mekanisme lama yang hanya menggunakan rowCount
	 * (rawan duplikat pasca hapus).</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * <ol>
	 *   <li>Jika konfigurasi nomor surat tidak tersedia (null), mengembalikan barcode acak
	 *       dari {@link Common#getGeneratedBarCode()}.</li>
	 *   <li>Menentukan index urut: jika nomor surat dikonfigurasi untuk menggunakan index
	 *       urut manual ({@code gunakanIndexUrut}), mengambil {@code nomorIndex} langsung;
	 *       jika tidak, menghitung dari database menggunakan {@link #getindex(NomorSurat)}.</li>
	 *   <li>Jika parameter {@code tambah} adalah true, menginkrementasi index nomor surat
	 *       secara permanen menggunakan {@link NomorSurat#tambahIndexNomorSurat}.</li>
	 *   <li>Memformat kode menggunakan metode {@code format(index, tanggal)} pada entitas
	 *       nomor surat.</li>
	 *   <li>Memastikan keunikan kode menggunakan {@code KodeUnikUtil.pastikanUnik} yang
	 *       akan menambahkan sufiks jika kode sudah digunakan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA}
	 * atau {@code getNomorSurat()} null, metode jatuh ke barcode acak secara aman.</p>
	 *
	 * @param tambah {@code true} untuk menginkrementasi counter nomor surat secara permanen
	 *               (digunakan saat benar-benar menyimpan); {@code false} untuk pratinjau
	 *               kode tanpa mengubah counter (digunakan saat membuka formulir baru).
	 * @return string kode pembayaran yang sudah diformat dan dijamin unik dalam tabel
	 *         {@link PembayaranPengadaanMasterAsset}.
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA == null
				|| NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat());

		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat());
		}

		String noAgenda = NomorSuratAlurPengadaan.PEMBAYARAN_PEMBELIAN_DATA.getNomorSurat().format(index,
				tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue());
		return ais.action.master.KodeUnikUtil.pastikanUnik(PembayaranPengadaanMasterAsset.class, noAgenda);
	}

	/**
	 * Menghitung index urut berikutnya untuk pembangkitan kode pembayaran berdasarkan
	 * jumlah record yang sudah ada di database, dengan mempertimbangkan aturan reset
	 * urutan yang dikonfigurasi dalam {@link NomorSurat}.
	 *
	 * <p><b>Tujuan:</b> Menentukan nomor urut yang tepat untuk kode dokumen baru dengan
	 * mempertimbangkan aturan penomoran seperti reset per tahun, reset per bulan, reset
	 * per tanggal, pengelompokan per nomor surat, atau pengelompokan per kelompok nomor
	 * surat.</p>
	 *
	 * <p><b>Cara kerja:</b><br>
	 * Membangun query Hibernate Criteria dengan join ke alias
	 * {@code nomorSuratAlurPengadaan} dan {@code nomorSurat}, kemudian menerapkan
	 * filter berdasarkan konfigurasi {@link NomorSurat}:
	 * <ul>
	 *   <li>{@code urutBerdasarkanNomor}: filter berdasarkan nomor surat yang sama.</li>
	 *   <li>{@code urutBerdasarkanKelompok}: filter berdasarkan kelompok nomor surat.</li>
	 *   <li>{@code resetUrutanTiapTahun}: filter hanya tahun berjalan.</li>
	 *   <li>{@code resetUrutanTiapBulan}: filter hanya bulan berjalan.</li>
	 *   <li>{@code resetTiap}: filter hanya record setelah tanggal reset tertentu.</li>
	 * </ul>
	 * Hasil rowCount ditambah 1 untuk mendapatkan index berikutnya.
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Jika {@code nomorSurat} null, mengembalikan 0L segera.
	 * Jika query mengembalikan null (tidak ada record), index dimulai dari 1.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat yang menentukan aturan penomoran;
	 *                   jika null, metode mengembalikan 0L.
	 * @return nilai index {@code Long} yang merupakan urutan berikutnya untuk kode dokumen,
	 *         dimulai dari 1 jika belum ada record yang cocok.
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(PembayaranPengadaanMasterAsset.class)
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
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
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
	 * Mengimplementasikan metode {@link FormSop#setPersetujuan(boolean)} untuk mengubah
	 * mode operasi Action ini antara mode input biasa dan mode persetujuan. Metode ini
	 * dipanggil oleh framework SOP ketika halaman perlu beralih konteks, misalnya ketika
	 * pejabat penyetuju mengakses formulir yang sama dari alur disposisi SOP.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan satu kelas Action digunakan untuk dua peran berbeda
	 * (operator input dan pejabat penyetuju) tanpa perlu membuat subkelas terpisah.</p>
	 *
	 * <p><b>Cara kerja:</b> Menetapkan nilai parameter ke field instance {@code persetujuan}.
	 * Perubahan ini akan berpengaruh pada tampilan formulir berikutnya yang dibangun oleh
	 * metode {@link #form}.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk kembali ke mode input biasa.
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
