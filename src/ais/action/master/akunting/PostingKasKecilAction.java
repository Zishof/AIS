package ais.action.master.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingKasKecilAction — Pengelola Posting Jurnal Kas Kecil ke Buku Besar</h3>
 *
 * <p><strong>Untuk apa:</strong> Kelas ini adalah ZK GenericAutowireComposer yang
 * mengelola proses posting transaksi {@link KasKecil} (kas kecil/petty cash) ke jurnal
 * umum (buku besar) dalam sistem akuntansi AIS. Kas Kecil adalah dana kecil yang
 * dikelola oleh kasir untuk pengeluaran operasional sehari-hari yang nilainya relatif
 * kecil. Posting menghasilkan entri jurnal akuntansi (GrupTransaksi + GrupTransaksiDetail
 * debet dan kredit) yang merepresentasikan transaksi tersebut dalam buku besar, sehingga
 * laporan keuangan dapat dihasilkan secara akurat.</p>
 *
 * <p><strong>Cara kerja:</strong> Alur kerja utama kelas ini adalah:
 * <ol>
 *   <li>Halaman menampilkan daftar KasKecil yang telah disetujui (disetujuiOleh tidak null)
 *       dengan nilai tidak nol, difilter berdasarkan rentang tanggal, satuan kerja, kode/nama,
 *       dan status posting.</li>
 *   <li>Setiap baris grid menampilkan kode KasKecil, link proses transfer (jika ada), nama,
 *       link SOP, nilai, tanggal persetujuan, preview jurnal multi-akun (dari formula JSON),
 *       status posting, dan tombol aksi posting/batalkan.</li>
 *   <li>Tombol "Posting Semua" ({@link #onPostingSemua}) membuka dialog konfirmasi dan
 *       menjalankan posting massal dalam thread latar belakang menggunakan native session.</li>
 *   <li>Tombol per baris "Posting Data" dan "Batalkan Posting" untuk operasi individual.</li>
 *   <li>Tombol "Batalkan Posting Semua" ({@link #onBatalkanPostingSemua}) membatalkan semua
 *       posting dalam filter aktif.</li>
 *   <li>Halaman mendukung parameter URL {@code sudah_posting} untuk mode tampilan terfokus.</li>
 * </ol>
 * </p>
 *
 * <p><strong>Fitur Unik — Formula JSON:</strong> Berbeda dengan KasBesar dan UangMuka yang
 * menggunakan akun tunggal untuk debet, KasKecil menggunakan field {@code formula} (JSON
 * array) untuk mendukung multiple akun debet. Format: {@code [{"akun": 123, "jumlah": 50000},
 * {"akun": 456, "jumlah": 30000}]}. Setiap elemen mewakili satu baris debet dengan akun
 * berbeda. Akun diambil dari cache {@code ConstantValues.ambil(Akun.class.getName(), id)}.
 * Kredit selalu satu akun: {@code jenisKasKecil.akun}.</p>
 *
 * <p><strong>Fitur Unik — Penutupan Kas Kecil:</strong> Jika {@code kasKecil.getMerupakanPenutupanKasKecil()}
 * true, ada akun debet tambahan yaitu {@code jenisKasKecil.akunPenutupKasKecil} dengan nilai
 * {@code kasKecil.getSisa()} (sisa dana kas kecil yang belum terpakai). Nilai kredit juga
 * bertambah: {@code nilai + sisa}. Ini mencerminkan penutupan kas kecil di akhir periode.</p>
 *
 * <p><strong>Threading:</strong> Proses posting massal ({@link #onPostingSemua}) menggunakan
 * thread Java terpisah dengan {@code HibernateUtil.currentNativeSession()} yang harus dikelola
 * manual (begin/commit/close). Thread ZK event (posting per baris, batalkan) menggunakan
 * {@code currentSession()} managed session.</p>
 *
 * <p><strong>Mode sudah_posting:</strong> Jika parameter URL {@code sudah_posting} ada,
 * panel filter disembunyikan, rowPosting ditampilkan, dan criteria menggunakan logika berbeda
 * (filter hanya berdasarkan nilai, disetujuiOleh, rentang tanggal, dan status posting boolean).</p>
 *
 * <p><strong>Pemeliharaan:</strong> Jika struktur formula JSON berubah (misalnya penambahan
 * field baru), perbarui parsing di {@link #onPostingSemua} dan {@link KasKecilRenderer}. Kedua
 * tempat harus selalu sinkron. Jika logika penutupan kas kecil berubah, perbarui juga di
 * kedua tempat tersebut. Jika ada filter baru, perbarui {@link #initCriteria}.</p>
 *
 * @see KasKecil
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingKasKecilAction extends GenericAutowireComposer {

	/** Serial version UID untuk serialisasi kelas. */
	private static final long serialVersionUID = -5779730217402400328L;

	private MyGrid grid;

	private Paging paging;

	private Textbox searchkode;
	private MyCheckboxConfig searchtampil;
	private MyCheckboxConfig searchtelahtampil;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private boolean edit = false;

	private MyToolbarbuttonConfig sent;

	/** Flag apakah pengguna memiliki hak admin atau APPROVE untuk batalkan posting. */
	public boolean adminLain;

	private MyDatebox tglMulai;
	private MyDatebox tglSampai;

	private North filter;
	private Tbmuser tbmuser;
	private Row rowPosting;

	/** Nilai dari parameter URL sudah_posting; null jika tidak ada parameter tersebut. */
	private Boolean sudah_posting = null;

	/**
	 * Dipanggil sebelum komponen ZK dirakit untuk memeriksa keamanan akses halaman.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna dengan sesi valid yang dapat
	 * mengakses halaman posting kas kecil sebelum komponen UI dibangun oleh ZK framework.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} untuk
	 * verifikasi sesi, kemudian melanjutkan ke implementasi parent ZK yang memproses
	 * metadata komponen.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Ditangani oleh {@code Common.doCheckSecurity()}
	 * yang akan redirect ke halaman login jika sesi tidak valid.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Boilerplate keamanan standar; tidak perlu diubah
	 * kecuali ada perubahan pada mekanisme keamanan global.</p>
	 *
	 * @param page     halaman ZK yang sedang dimuat
	 * @param parent   komponen induk dalam pohon ZK
	 * @param compInfo metadata komponen ZK
	 * @return ComponentInfo dari pemanggilan super
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Dipanggil setelah semua komponen ZK berhasil dirakit. Melakukan inisialisasi lengkap
	 * halaman posting kas kecil termasuk mode sudah_posting dari URL, filter tanggal default,
	 * listener satuan kerja, hak akses, dan pemuatan data awal.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua aspek halaman agar siap digunakan,
	 * termasuk menangani mode khusus yang diaktifkan oleh parameter URL {@code sudah_posting},
	 * menyetel rentang tanggal default 6 bulan, dan mendaftarkan semua listener yang
	 * diperlukan untuk operasi filter dan paging.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Langkah inisialisasi berurutan:
	 * <ol>
	 *   <li>Verifikasi sesi dan hak READ; logoff jika tidak valid.</li>
	 *   <li>Menyimpan pengguna saat ini ke field {@code tbmuser}.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting}. Jika ada, menyembunyikan panel
	 *       filter dan menampilkan rowPosting untuk mode tampilan terfokus.</li>
	 *   <li>Mendaftarkan listener onChange pada searchparent (filter satuan kerja).</li>
	 *   <li>Membuat SatuanKerjaTreeModel untuk navigasi hierarki satuan kerja.</li>
	 *   <li>Mengatur rentang tanggal default: 6 bulan lalu hingga sekarang.</li>
	 *   <li>Membaca parameter URL {@code mulai} dan {@code sampai} untuk override tanggal
	 *       (format yyyyMMdd); jika ada, field tanggal di-disable agar pengguna tidak mengubah.</li>
	 *   <li>Mengatur flag adminLain berdasarkan hak admin atau APPROVE.</li>
	 *   <li>Mengatur visibilitas tombol sent berdasarkan hak UPDATE.</li>
	 *   <li>Memuat data awal via {@link #loadDataDenganProgressPosting}.</li>
	 *   <li>Mendaftarkan listener paging dan membuat timer default untuk refresh.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari parsing parameter URL ditangkap
	 * dan ditampilkan ke admin. Jika sesi tidak valid, langsung return.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada parameter URL baru, tambahkan pembacaannya
	 * setelah blok sudah_posting. Pastikan logika visibilitas filter konsisten dengan
	 * {@link #initCriteria} yang juga membaca field {@code sudah_posting}.</p>
	 *
	 * @param comp komponen root ZK yang telah dirakit
	 * @throws Exception jika terjadi error saat inisialisasi komponen ZK
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();
		if (execution.getParameter("sudah_posting") != null
				&& !execution.getParameter("sudah_posting").trim().isEmpty()) {
			sudah_posting = Boolean.parseBoolean(execution.getParameter("sudah_posting").trim());
		}

		if (sudah_posting != null && filter != null) {
			filter.setVisible(false);
			if (rowPosting != null) rowPosting.setVisible(true);
		}
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (tglSampai != null) tglSampai.setValue(ais.ui.util.WaktuUtil.getDate());
		if (tglMulai != null) tglMulai.setValue(calendar.getTime());
		if (tglMulai != null) tglMulai.setReadonly(true);
		if (tglSampai != null) tglSampai.setReadonly(true);

		if (execution.getParameter("mulai") != null) {
			try {
				if (tglMulai != null) tglMulai.setValue(Common.dateFormat8.get().parse(execution.getParameter("mulai")));
				if (tglMulai != null) tglMulai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		if (execution.getParameter("sampai") != null) {
			try {
				if (tglSampai != null) tglSampai.setValue(Common.dateFormat8.get().parse(execution.getParameter("sampai")));
				if (tglSampai != null) tglSampai.setDisabled(true);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		adminLain = Common.getApakahAdmin() || CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		if (sent != null) { sent.setVisible(edit); }

		loadDataDenganProgressPosting(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataDenganProgressPosting(null);

			}
		});

	}

	/**
	 * Menangani event klik tombol "Batalkan Posting Semua" untuk membatalkan seluruh
	 * posting kas kecil dalam filter aktif setelah konfirmasi pengguna.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan cara massal untuk membatalkan semua posting
	 * kas kecil dalam filter yang aktif, misalnya ketika terjadi kesalahan penentuan akun
	 * pada formula JSON atau perlu koreksi periode jurnal secara keseluruhan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menampilkan dialog konfirmasi MyMessageboxConfig.
	 * Jika pengguna mengonfirmasi (OK), mengambil semua KasKecil yang sudah diposting
	 * (postingHistory tidak null) dalam filter aktif, kemudian untuk setiap KasKecil:
	 * <ol>
	 *   <li>Menyetel postingHistory ke null.</li>
	 *   <li>Menyimpan perubahan dengan {@code Common.refreshSaveOrUpdate}.</li>
	 *   <li>Menghapus GrupTransaksi terkait menggunakan SQL native:
	 *       {@code DELETE FROM akunting.grup_transaksi WHERE kas_kecil=id AND closing IS NULL}.</li>
	 * </ol>
	 * Setelah selesai, memuat ulang data grid via timer default.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari operasi database diteruskan ke
	 * framework ZK melalui signature throws di EventListener.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Filter {@code closing IS NULL} pada SQL hapus
	 * penting untuk tidak menghapus entri yang sudah di-closing. Nama kolom SQL
	 * {@code kas_kecil} harus sesuai dengan nama kolom FK di tabel grup_transaksi.</p>
	 *
	 * @param event event ZK dari klik tombol Batalkan Posting Semua
	 * @throws Exception jika terjadi error saat dialog atau operasi database
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi kas kecil ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<KasKecil> kasKecils = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (KasKecil kasKecil : kasKecils) {
								kasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(kasKecil);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where kas_kecil=" + kasKecil.getId()
												+ " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where kas_kecil=" + kasKecil.getId() + " and closing is null")
										.executeUpdate();
							}
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadDataDenganProgressPosting(null);
							}
						});
					}
				});

	}

	/**
	 * Menangani event klik tombol "Posting Semua" untuk membuka dialog konfirmasi dan
	 * menjalankan proses posting massal semua KasKecil yang belum diposting dalam filter
	 * aktif menggunakan thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan staf akuntansi memposting semua transaksi
	 * kas kecil yang memenuhi filter aktif dalam satu operasi massal. Dialog meminta
	 * tanggal posting, menampilkan nama poster otomatis, dan opsional keterangan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Membuka popup dialog MyWindow dengan field tanggal
	 * posting (wajib), nama poster (read-only), keterangan (opsional multi-baris). Setelah
	 * pengguna mengklik Simpan dan mengonfirmasi:
	 * <ol>
	 *   <li>Membuat PostingHistory baru dengan jenis JENIS_PENGGUNAAN_KAS_KECIL.</li>
	 *   <li>Menyimpan PostingHistory dalam native session dengan begin/commit terpisah.</li>
	 *   <li>Mengambil semua KasKecil belum diposting dalam filter aktif.</li>
	 *   <li>Untuk setiap KasKecil: parsing formula JSON ({@code kasKecil.getFormula()})
	 *       untuk mendapatkan daftar akun debet dan nilainya. Setiap elemen JSON berisi
	 *       field "akun" (Long ID) dan "jumlah" (Double). Akun dimuat via
	 *       {@code ConstantValues.ambil(Akun.class.getName(), id)} dari cache.</li>
	 *   <li>Jika KasKecil merupakan penutupan (getMerupakanPenutupanKasKecil() true):
	 *       tambahkan akunPenutupKasKecil sebagai debet tambahan dengan nilai sisa; dan
	 *       nilai kredit = nilai + sisa.</li>
	 *   <li>Akun kredit selalu dari jenisKasKecil.akun.</li>
	 *   <li>Memanggil {@code CommonAkunting.saveTransaksi} dengan array akun debet dan
	 *       satu kredit. Jika nilai positif: Debet→Kredit; Jika negatif: swap posisi.</li>
	 *   <li>Menyetel postingHistory ke KasKecil dan menyimpan perubahan.</li>
	 *   <li>Label progress diperbarui dengan persentase kemajuan per iterasi.</li>
	 *   <li>Setelah semua selesai, menutup native session dan menampilkan pesan sukses.</li>
	 * </ol>
	 * Proses berjalan dalam thread Java terpisah ({@code new Thread().start()}) menggunakan
	 * {@code HibernateUtil.currentNativeSession()} — harus begin/commit/close manual.
	 * {@code HibernateUtil.closeSession()} dipanggil di akhir thread.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per KasKecil ditangkap secara diam-diam
	 * (catch kosong) agar satu KasKecil gagal tidak menghentikan proses. Exception saat
	 * saveTransaksi ditampilkan ke admin. Exception saat parsing JSON per elemen menyebabkan
	 * elemen tersebut dilewati (akunBiaya null = tidak ditambahkan).</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika struktur formula JSON berubah, perbarui parsing
	 * di sini dan di {@link KasKecilRenderer}. Pastikan logika penutupan kas kecil (sisa)
	 * konsisten antara kedua tempat. Field "akun" dan "jumlah" adalah nama yang di-hardcode
	 * dalam JSON; jangan ubah tanpa memperbarui semua tempat yang menghasilkan JSON ini.</p>
	 *
	 * @param event event ZK dari klik tombol Posting Semua
	 * @throws Exception jika terjadi error saat membangun dialog popup
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Kas Kecil");
		addWindow.setWidth("800px");
		addWindow.setHeight("300px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");
		mygrid.setParent(center);
		mygrid.setWidth("100%");
		mygrid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("100px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");

		Rows rows = new Rows();
		rows.setParent(mygrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal / Waktu"));
		final MyDatebox tanggal;
		row.appendChild(tanggal = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diposting oleh"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				Common.getCurrentUser().ambilPegawai() == null ? Common.getCurrentUser().getUserId()
						: Common.getCurrentUser().ambilPegawai().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		final MyTextbox keterangan;
		row.appendChild(keterangan = new MyTextbox());
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Date tgl = tanggal.getValue();
				if (tgl == null) {
					MyMessageboxConfig.show("Mohon maaf, Tanggal Posting belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Posting menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses posting. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}
				// if (keterangan.getValue().trim().equals("")) {
				// MyMessageboxConfig.show("Keterangan harus diisi",
				// "Peringatan", MyMessageboxConfig.OK,
				// MyMessageboxConfig.EXCLAMATION);
				// return;
				// }

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi kas kecil ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Tbmuser tbmuser = Common.getCurrentUser();

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											MyMessageboxConfig.show("Posting transaksi kas kecil berhasil dilakukan",
													"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
													new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															onSearchDefault(arg0);
														}
													});

											addWindow.detach();
										}
									});

									new Thread(new Runnable() {

										@Override
										public void run() {
											try {

											Session session = HibernateUtil.currentNativeSession();

											PostingHistory postingHistory = new PostingHistory(
													PostingHistory.JENIS_PENGGUNAAN_KAS_KECIL);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<KasKecil> kasKecils = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (KasKecil kasKecil : kasKecils) {

												SatuanKerja satuanKerja = (SatuanKerja) (kasKecil
														.getSatuanKerja() != null ? kasKecil.getSatuanKerja() : null);

												if (kasKecil != null) {

													try {
														List<Akun> akunDebet = new ArrayList<Akun>();
														List<Double> nilaiDebets = new ArrayList<Double>();
														JSONArray array = new JSONArray(kasKecil.getFormula());
														StringBuilder rincianSb = new StringBuilder();
														for (int i = 0; i < array.length(); i++) {

															JSONObject jsonObject = array.getJSONObject(i);

															Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
																	: ConstantValues.ambil(Akun.class.getName(),
																			ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

															Double jumlah = 0.0;
															if (!jsonObject.isNull("jumlah")) {
																jumlah = jsonObject.getDouble("jumlah");
															}

															String namaItem = "";
															if (!jsonObject.isNull("nama")) {
																namaItem = jsonObject.get("nama") + "";
															}

															if (akunBiaya != null) {
																akunDebet.add(akunBiaya);
																nilaiDebets.add(jumlah);
																if (!namaItem.trim().isEmpty() && jumlah > 0.01) {
																	if (rincianSb.length() > 0) rincianSb.append("; ");
																	rincianSb.append(namaItem.trim()).append(" ")
																			.append(Common.numberFormat.get().format(jumlah));
																}
															}
														}

														if (kasKecil.getMerupakanPenutupanKasKecil()
																&& kasKecil.getJenisKasKecil() != null
																&& kasKecil.getJenisKasKecil()
																		.getAkunPenutupKasKecil() != null
																&& kasKecil.getSisa().intValue() != 0) {
															akunDebet.add(kasKecil.getJenisKasKecil()
																	.getAkunPenutupKasKecil());
															nilaiDebets.add(kasKecil.getSisa());
														}

														Akun akunKredit = kasKecil.getJenisKasKecil().getAkun();

														if (!akunDebet.isEmpty() && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {
																ket = "Kas Kecil \"" + kasKecil.getKode()
																		+ "\" " + kasKecil.getNama()
																		+ (rincianSb.length() > 0 ? ": " + rincianSb.toString()
																			: " senilai " + Common.numberFormat.get().format(kasKecil.getNilai()));

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get()
																			.format(rowIndex * 100.0 / kasKecils.size())
																	+ " %)");

															Double nilai = kasKecil.getNilai();

															if (kasKecil.getMerupakanPenutupanKasKecil()) {
																nilai = nilai + kasKecil.getSisa();
															}

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																session.getTransaction().begin();

																if (nilai > 0.1) {
																	CommonAkunting.saveTransaksi(
																			akunDebet.toArray(new Akun[] {}),
																			new Akun[] { akunKredit }, akunDenda,
																			akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			kasKecil.getTanggalPersetujuan(),
																			nilaiDebets.toArray(new Double[] {}),
																			new Double[] { nilai }, denda, kasKecil,
																			satuanKerja, session);
																} else {
																	CommonAkunting.saveTransaksi(
																			new Akun[] { akunKredit },
																			akunDebet.toArray(new Akun[] {}), akunDenda,
																			akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			kasKecil.getTanggalPersetujuan(),
																			new Double[] { nilai },
																			nilaiDebets.toArray(new Double[] {}), denda,
																			kasKecil, satuanKerja, session);
																}

																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															kasKecil.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(kasKecil);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingKasKecilAction.java:675");
														// Exception per-baris ditangkap untuk melanjutkan proses berikutnya
													}

												}
												rowIndex++;
											}

											label.setValue("");
											HibernateUtil.closeSession();
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
		addWindow.onModal();
	}

	// =====================================================================
	// REUSE STATIC — dipanggil dasbor DrafJurnalAction (Posting / Batalkan
	// satu-klik). Logikanya IDENTIK dengan onPostingSemua / onBatalkanPostingSemua,
	// tetapi tanpa komponen UI sehingga bisa dipakai ulang dari mana saja.
	// =====================================================================

	/** Kriteria KasKecil yang sah untuk diposting dalam rentang tanggal persetujuan (mulai..sampai). */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		// Cakupan penyewa (satuan kerja): tanpa ini, jalur API men-scan/memposting
		// dokumen kas kecil SELURUH instalasi (lintas Yayasan), bukan hanya milik
		// penyewa yang sedang memanggil -- lihat catatan sama pada
		// PostingTransaksiPembayaranGajiAction.kriteriaPostingStatic(). Himpunan kosong
		// (Yayasan tidak teridentifikasi) fail-CLOSED, bukan fail-open seperti
		// initCriteria(boolean) pada layar ZK.
		Set<SatuanKerja> satuanKerjasPengguna = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		Criteria c = session.createCriteria(KasKecil.class)
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
				.add(satuanKerjasPengguna.isEmpty() ? Restrictions.sqlRestriction("false")
						: Restrictions.or(Restrictions.isNull("satuanKerja"),
								Restrictions.in("satuanKerja", satuanKerjasPengguna)));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA kas kecil terposting dalam rentang. Mengikuti perilaku tombol lama:
	 * hapus baris grup_transaksi yang BELUM closing (closing is null) lalu kosongkan postingHistory —
	 * jurnal yang sudah closing TIDAK terhapus. Memakai currentSession (tidak ditutup manual).
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		// Transaksi dibuka sendiri. Memakai currentSession() seperti tombol di layar hanya
		// berhasil di dalam permintaan ZK, yang kerangkanya meng-commit sesi berjalan;
		// dipanggil dari API perubahannya tidak pernah tersimpan sehingga pembatalan
		// melaporkan sukses padahal jurnalnya masih utuh.
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<KasKecil> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (KasKecil dok : daftar) {
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi where kas_kecil=" + dok.getId()
							+ "  and closing is null)").executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi where kas_kecil="
							+ dok.getId() + " and closing is null").executeUpdate();
					dok.setPostingHistory(null);
					session.update(dok);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingKasKecilAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil pembatalan
			}
		}
		return n;
	}

	/**
	 * Posting SEMUA kas kecil yang belum diposting dalam rentang. Logika per-record IDENTIK dengan
	 * {@link #onPostingSemua}. Memakai currentNativeSession dan ditutup di finally
	 * (disconnect + closeSession). Mengembalikan jumlah record yang berhasil diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			PostingHistory postingHistory = new PostingHistory(PostingHistory.JENIS_PENGGUNAAN_KAS_KECIL);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal kas kecil dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			List<KasKecil> kasKecils = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();

			for (KasKecil kasKecil : kasKecils) {
				if (kasKecil == null) {
					continue;
				}
				SatuanKerja satuanKerja = kasKecil.getSatuanKerja() != null ? kasKecil.getSatuanKerja() : null;
				try {
					List<Akun> akunDebet = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					JSONArray array = new JSONArray(kasKecil.getFormula());
					StringBuilder rincianSb = new StringBuilder();
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
								: ConstantValues.ambil(Akun.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));
						Double jumlah = 0.0;
						if (!jsonObject.isNull("jumlah")) {
							jumlah = jsonObject.getDouble("jumlah");
						}
						String namaItem = "";
						if (!jsonObject.isNull("nama")) {
							namaItem = jsonObject.get("nama") + "";
						}
						if (akunBiaya != null) {
							akunDebet.add(akunBiaya);
							nilaiDebets.add(jumlah);
							if (!namaItem.trim().isEmpty() && jumlah > 0.01) {
								if (rincianSb.length() > 0) rincianSb.append("; ");
								rincianSb.append(namaItem.trim()).append(" ")
										.append(Common.numberFormat.get().format(jumlah));
							}
						}
					}

					if (kasKecil.getMerupakanPenutupanKasKecil() && kasKecil.getJenisKasKecil() != null
							&& kasKecil.getJenisKasKecil().getAkunPenutupKasKecil() != null
							&& kasKecil.getSisa().intValue() != 0) {
						akunDebet.add(kasKecil.getJenisKasKecil().getAkunPenutupKasKecil());
						nilaiDebets.add(kasKecil.getSisa());
					}

					Akun akunKredit = kasKecil.getJenisKasKecil().getAkun();

					if (!akunDebet.isEmpty() && akunKredit != null) {
						Boolean apakahUangMasuk = true;
						String ket = "";
						try {
							ket = "Kas Kecil \"" + kasKecil.getKode() + "\" " + kasKecil.getNama()
									+ (rincianSb.length() > 0 ? ": " + rincianSb.toString()
										: " senilai " + Common.numberFormat.get().format(kasKecil.getNilai()));
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "PostingKasKecilAction jalur API");
						}

						Double nilai = kasKecil.getNilai();
						if (kasKecil.getMerupakanPenutupanKasKecil()) {
							nilai = nilai + kasKecil.getSisa();
						}

						try {
							Akun akunDenda = null;
							Akun akunPiutangDenda = null;
							Double denda = 0.0;
							session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							if (nilai > 0.1) {
								CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
										new Akun[] { akunKredit }, akunDenda, akunPiutangDenda, postingHistory,
										apakahUangMasuk, ket, kasKecil.getTanggalPersetujuan(),
										nilaiDebets.toArray(new Double[] {}), new Double[] { nilai }, denda, kasKecil,
										satuanKerja, session);
							} else {
								CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
										akunDebet.toArray(new Akun[] {}), akunDenda, akunPiutangDenda, postingHistory,
										apakahUangMasuk, ket, kasKecil.getTanggalPersetujuan(), new Double[] { nilai },
										nilaiDebets.toArray(new Double[] {}), denda, kasKecil, satuanKerja, session);
							}
							session.getTransaction().commit();
						} catch (Exception e) {
							ais.common.ErrorAuditUtil.record(e, "PostingKasKecilAction jalur API");
						}

						kasKecil.setPostingHistory(postingHistory);
						session = HibernateUtil.currentNativeSession();
						session.getTransaction().begin();
						session.update(kasKecil);
						session.getTransaction().commit();
						n++;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingKasKecilAction.java:857");
					// per-baris: lanjutkan ke record berikutnya
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingKasKecilAction jalur API");
		} finally {
			try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingKasKecilAction.java:864");}
			try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingKasKecilAction.java:865");}
		}
		return n;
	}

	/**
	 * Kelas inner untuk merender setiap baris data {@link KasKecil} pada grid utama
	 * halaman posting kas kecil.
	 *
	 * <p><strong>Tujuan:</strong> Mengubah setiap entitas KasKecil menjadi baris tampilan
	 * grid yang informatif dengan kolom kode+link proses transfer, nama+SOP, nilai, tanggal
	 * persetujuan, preview jurnal multi-akun (dari formula JSON), status posting, dan
	 * tombol aksi posting/batalkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Metode {@code render} mengisi row dengan:
	 * <ul>
	 *   <li>Kolom kode: Vbox berisi link revisi kode KasKecil dan link proses transfer
	 *       (jika ada melalui penggantianKasKecil → daftarPengajuanTransfer → prosesTransfer).</li>
	 *   <li>Kolom nama: Vbox berisi nama KasKecil dan link SOP (jika disposisiSop ada).</li>
	 *   <li>Kolom nilai: nilai total KasKecil diformat.</li>
	 *   <li>Kolom tanggal: tanggal persetujuan diformat.</li>
	 *   <li>Parsing formula JSON dari {@code kasKecil.getFormula()} untuk mendapatkan list
	 *       akun debet. Setiap elemen JSON diambil akun dari cache ConstantValues dan jumlah
	 *       dari field "jumlah". Elemen dengan akun null dilewati.</li>
	 *   <li>Jika penutupan kas kecil: tambahkan akunPenutupKasKecil sebagai debet ekstra.</li>
	 *   <li>Kolom jurnal: preview multi-akun menggunakan {@code GrupTransaksi.tampilkanJurnal}.
	 *       Nilai kredit = nilai + sisa jika penutupan, = nilai jika bukan penutupan.</li>
	 *   <li>Kolom status: "Belum diposting" atau nama poster + no. bukti dari GrupTransaksi.</li>
	 *   <li>Kolom aksi: tombol Batalkan Posting (jika edit+admin+sudah posting) dan
	 *       Posting Data (jika edit+belum posting+tbmuser valid).</li>
	 * </ul>
	 * </p>
	 *
	 * <p><strong>Threading:</strong> Render dipanggil pada thread ZK event; aman mengakses
	 * currentSession(). Posting per baris juga menggunakan managed session.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Parsing formula JSON harus sinkron dengan logika di
	 * {@link #onPostingSemua}. Jika ada perubahan field JSON ("akun", "jumlah"), perbarui
	 * di kedua tempat.</p>
	 */
	class KasKecilRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link KasKecil} ke dalam komponen {@link Row} ZK,
		 * lengkap dengan parsing formula JSON untuk preview jurnal multi-akun, penanganan
		 * penutupan kas kecil, dan tombol aksi posting.
		 *
		 * <p><strong>Tujuan:</strong> Menampilkan semua informasi relevan KasKecil dalam
		 * satu baris grid yang komprehensif, termasuk semua akun debet dari formula JSON,
		 * dan memungkinkan operasi posting atau pembatalan langsung dari baris tersebut.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Setelah menampilkan kolom kode/nama/nilai/tanggal,
		 * metode ini mem-parsing {@code kasKecil.getFormula()} sebagai JSONArray. Untuk setiap
		 * elemen JSON, mengambil Akun dari cache dan jumlahnya. Kemudian menentukan apakah
		 * perlu menambahkan akun penutup kas kecil (jika merupakanPenutupanKasKecil). Akun
		 * kredit dari jenisKasKecil.akun. Jurnal preview ditampilkan dengan semua akun debet
		 * yang telah dikumpulkan. Tombol posting per baris menggunakan managed session dan
		 * melakukan parsing ulang formula serta logika penutupan yang sama.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Jika formula JSON tidak bisa diparsing atau
		 * akunDebet kosong, pesan "Transaksi tidak valid." ditampilkan di kolom jurnal.
		 * Exception saat posting per baris ditangkap dan ditampilkan ke admin.</p>
		 *
		 * <p><strong>Pemeliharaan:</strong> Logika parsing formula dan penanganan penutupan
		 * harus selalu identik dengan yang ada di {@link #onPostingSemua}. Jika CommonJSONUtil
		 * atau cara parsing berubah, perbarui di kedua tempat tersebut.</p>
		 *
		 * @param arg0 komponen Row ZK yang akan diisi
		 * @param arg1 objek data yang harus berupa instansi KasKecil
		 * @throws Exception jika terjadi error saat render komponen ZK atau parsing JSON
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KasKecil kasKecil = (KasKecil) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(KasKecil.class, kasKecil,
					kasKecil.getKode() == null ? "" : kasKecil.getKode())).setParent(arg0);
			if (kasKecil.getPenggantianKasKecil() != null
					&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer() != null
					&& kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer() != null) {
				A a = new A(
						kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, kasKecil.getPenggantianKasKecil().getDaftarPengajuanTransfer().getProsesTransfer(), edit);

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);
			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(kasKecil.getNama()).setParent(a);
			if (kasKecil.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + kasKecil.getDisposisiSop().getKeterangan()
						+ " (" + kasKecil.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kasKecil.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Double nilai = kasKecil.getNilai();

			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.dateFormat3.get().format(kasKecil.getTanggalPersetujuan())).setParent(arg0);

			List<Akun> akunDebets = new ArrayList<Akun>();
			List<Double> nilaiDebets = new ArrayList<Double>();
			List<String> keteranganBiayaDebets = new ArrayList<String>();
			JSONArray array = new JSONArray(kasKecil.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				if (akunBiaya != null) {
					akunDebets.add(akunBiaya);
					nilaiDebets.add(jumlah);
					keteranganBiayaDebets.add(jsonObject.isNull("nama") ? "" : (jsonObject.get("nama") + ""));
				}
			}

			if (kasKecil.getMerupakanPenutupanKasKecil() && kasKecil.getJenisKasKecil() != null
					&& kasKecil.getJenisKasKecil().getAkunPenutupKasKecil() != null
					&& kasKecil.getSisa().intValue() != 0) {
				akunDebets.add(kasKecil.getJenisKasKecil().getAkunPenutupKasKecil());
				nilaiDebets.add(kasKecil.getSisa());
				keteranganBiayaDebets.add("Sisa penutupan kas kecil");
			}

			Akun akunKredit = kasKecil.getJenisKasKecil().getAkun();

			if (!akunDebets.isEmpty() && akunKredit != null) {

				List<Akun> akunsKredits = new ArrayList<Akun>();
				List<Double> nilaiKredits = new ArrayList<Double>();
				akunsKredits.add(akunKredit);

				if (kasKecil.getMerupakanPenutupanKasKecil()) {
					nilaiKredits.add(nilai + kasKecil.getSisa());
				} else {
					nilaiKredits.add(nilai);
				}

				GrupTransaksi.tampilkanJurnal(akunDebets, nilaiDebets, akunsKredits, nilaiKredits, null, keteranganBiayaDebets).setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (!akunDebets.isEmpty() ? " Debet: " + akunDebets.toString() + "." : " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("kasKecil", kasKecil))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(kasKecil.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: kasKecil.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunDebets.isEmpty() && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && kasKecil.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								kasKecil.setPostingHistory(null);
								Common.refreshSaveOrUpdate(kasKecil);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where kas_kecil=" + kasKecil.getId()
												+ " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where kas_kecil=" + kasKecil.getId() + " and closing is null")
										.executeUpdate();

								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										loadDataDenganProgressPosting(null);
									}
								});
							}
						});

					}

				});
				button.setParent(toolbar);

				button = new MyToolbarbuttonConfig("", "/img/svg/check2-circle.svg");
				button.setTooltiptext("Posting Data");
				button.setVisible(edit && kasKecil.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PENGGUNAAN_KAS_KECIL);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);
								List<Akun> akunDebet = new ArrayList<Akun>();
								List<Double> nilaiDebets = new ArrayList<Double>();
								StringBuilder rincianSb = new StringBuilder();
								JSONArray array = new JSONArray(kasKecil.getFormula());
								for (int i = 0; i < array.length(); i++) {

									JSONObject jsonObject = array.getJSONObject(i);

									Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
											: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

									Double jumlah = 0.0;
									if (!jsonObject.isNull("jumlah")) {
										jumlah = jsonObject.getDouble("jumlah");
									}

									String namaItem = "";
									if (!jsonObject.isNull("nama")) {
										namaItem = jsonObject.get("nama") + "";
									}

									if (akunBiaya != null) {
										akunDebet.add(akunBiaya);
										nilaiDebets.add(jumlah);
										if (!namaItem.trim().isEmpty() && jumlah > 0.01) {
											if (rincianSb.length() > 0) rincianSb.append("; ");
											rincianSb.append(namaItem.trim()).append(" ")
												.append(Common.numberFormat.get().format(jumlah));
										}
									}
								}

								if (kasKecil.getMerupakanPenutupanKasKecil() && kasKecil.getJenisKasKecil() != null
										&& kasKecil.getJenisKasKecil().getAkunPenutupKasKecil() != null
										&& kasKecil.getSisa().intValue() != 0) {
									akunDebet.add(kasKecil.getJenisKasKecil().getAkunPenutupKasKecil());
									nilaiDebets.add(kasKecil.getSisa());
								}

								Akun akunKredit = kasKecil.getJenisKasKecil().getAkun();

								if (!akunDebet.isEmpty() && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = kasKecil.getNilai();

									if (kasKecil.getMerupakanPenutupanKasKecil()) {
										nilai = nilai + kasKecil.getSisa();
									}

									String ket = "";
									try {

										ket = "Kas Kecil \"" + kasKecil.getKode() + "\" " + kasKecil.getNama()
												+ (rincianSb.length() > 0 ? ": " + rincianSb.toString()
													: " senilai " + Common.numberFormat.get().format(kasKecil.getNilai()));
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (kasKecil.getSatuanKerja() != null
											? kasKecil.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									if (nilai > 0.1) {
										CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
												new Akun[] { akunKredit }, akunDenda, akunPiutangDenda, postingHistory,
												apakahUangMasuk, ket, kasKecil.getTanggalPersetujuan(),
												nilaiDebets.toArray(new Double[] {}), new Double[] { nilai }, denda,
												kasKecil, satuanKerja, session);
									} else {
										CommonAkunting.saveTransaksi(new Akun[] { akunKredit },
												akunDebet.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket, kasKecil.getTanggalPersetujuan(),
												new Double[] { nilai }, nilaiDebets.toArray(new Double[] {}), denda,
												kasKecil, satuanKerja, session);
									}

									kasKecil.setPostingHistory(postingHistory);
									session.update(kasKecil);
								}

								loadDataDenganProgressPosting(null);
							}
						});

					}

				});
				button.setParent(toolbar);
			}

		}
	}

	/**
	 * Membangun Hibernate {@link Criteria} untuk query {@link KasKecil} berdasarkan filter
	 * aktif di halaman atau berdasarkan parameter URL {@code sudah_posting}.
	 *
	 * <p><strong>Tujuan:</strong> Menyatukan logika pembangunan query KasKecil agar dapat
	 * digunakan untuk paging, pengambilan data aktual, maupun operasi massal (batalkan
	 * posting semua, posting semua) dengan satu titik perubahan filter.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Terdapat dua jalur logika berdasarkan nilai
	 * {@code sudah_posting}:
	 * <ul>
	 *   <li>Jika {@code sudah_posting} tidak null (mode terfokus dari URL): filter berdasarkan
	 *       nilai tidak nol, disetujuiOleh tidak null, rentang tanggal, dan status posting
	 *       sesuai nilai boolean sudah_posting (true = sudah diposting, false = belum).</li>
	 *   <li>Jika {@code sudah_posting} null (mode normal dari halaman biasa): tambahkan filter
	 *       satuan kerja (dengan hierarki dari SatuanKerjaTreeModel), disetujuiOleh tidak null,
	 *       nilai tidak nol, rentang tanggal persetujuan menggunakan fungsi SQL date(), dan filter
	 *       kode/nama/keterangan menggunakan ILIKE case-insensitive. Checkbox searchtampil (belum
	 *       posting) dan searchtelahtampil (sudah posting) menambahkan join ke postingHistory
	 *       dan filter status posting terkait.</li>
	 * </ul>
	 * Jika {@code order} true, menambahkan pengurutan berdasarkan ID descending.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dalam blok sudah_posting ditangkap
	 * dan ditampilkan ke admin.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jika ada filter baru, tambahkan di blok yang sesuai.
	 * Filter satuan kerja menggunakan kombinasi OR(isNull, in(satuanKerjas)) untuk mendukung
	 * KasKecil yang tidak memiliki satuan kerja (satuanKerja null).</p>
	 *
	 * @param order true untuk menambahkan pengurutan berdasarkan ID desc
	 * @return Criteria Hibernate yang sudah dikonfigurasi sesuai filter aktif
	 */
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(KasKecil.class);

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
						.add(Restrictions.isNotNull("disetujuiOleh"))

						.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
								+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
								+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")));

				if (sudah_posting) {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.eq("postingHistory.posting", true));
				} else {
					criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN)
							.add(Restrictions.or(Restrictions.isNull("postingHistory.id"),
									Restrictions.eq("postingHistory.posting", false)));
				}

			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		} else {

			if (searchtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtelahtampil.setChecked(false);
			}

			else if (searchtelahtampil.isChecked()) {
				criteria.createAlias("postingHistory", "postingHistory", Criteria.LEFT_JOIN).add(Restrictions.or(
						Restrictions.isNull("postingHistory.id"), Restrictions.eq("postingHistory.posting", false)));

				searchtampil.setChecked(false);
			}

			criteria.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
					satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(
									parent == null ? Restrictions.isNull("satuanKerja")
											: Restrictions.sqlRestriction("false"),
									Restrictions.in("satuanKerja", satuanKerjas))))

					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

					.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
											Restrictions.ilike("keterangan", searchkode.getValue(),
													MatchMode.ANYWHERE))));
		}
		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat data KasKecil ke grid tanpa tampilan progress bar secara langsung.
	 *
	 * <p><strong>Tujuan:</strong> Melakukan query dan render grid aktual sebagai metode
	 * internal yang dipanggil dari {@link #loadDataDenganProgressPosting} setelah progress
	 * bar ditampilkan, agar pengguna mendapat feedback visual selama proses loading.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menghitung total untuk paging menggunakan criteria
	 * tanpa order melalui {@code Common.initPaging}, kemudian mengambil data halaman aktif
	 * dengan batasan ROWS_COUNT_ON_PAGE dan offset halaman aktif. Membungkus hasil dalam
	 * SimpleListModel dan memuat ke grid dengan {@link KasKecilRenderer}.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception Hibernate diteruskan ke pemanggil.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Metode ini hanya dipanggil dari
	 * {@link #loadDataDenganProgressPosting}; tidak dimaksudkan untuk pemanggilan langsung
	 * dari handler event atau kode eksternal.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KasKecil> kasKecil = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kasKecil);
		grid.setRowRenderer(new KasKecilRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Handler event pencarian default yang mendelegasikan ke
	 * {@link #loadDataDenganProgressPosting} untuk pembaruan grid dengan progress bar.
	 *
	 * <p><strong>Tujuan:</strong> Menjadi titik masuk terpusat untuk semua pembaruan grid
	 * dari berbagai sumber event (filter, paging, timer, SOP callback), memastikan progress
	 * bar selalu ditampilkan saat data dimuat.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Hanya memanggil
	 * {@link #loadDataDenganProgressPosting(Event)} dengan event yang diterima.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Jangan menaruh logika langsung di sini; selalu
	 * delegasikan ke loadDataDenganProgressPosting untuk konsistensi tampilan progress.</p>
	 *
	 * @param event event ZK pemicu (dapat null)
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/** Flag yang menandakan apakah proses loading data sedang berlangsung. */
	private boolean postingJurnalLoadingAktif = false;

	/** Flag yang menandakan ada permintaan reload tertunda saat loading berlangsung. */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat ulang data grid dengan menampilkan progress bar dan mengelola concurrent
	 * reload menggunakan mekanisme flag untuk mencegah race condition pada grid KasKecil.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan feedback visual kepada pengguna saat data grid
	 * sedang dimuat (progress bar dengan pesan status bertahap), sekaligus mencegah multiple
	 * reload yang terjadi bersamaan yang dapat menyebabkan inkonsistensi tampilan. Ini
	 * terutama penting pada grid KasKecil karena setiap render baris melakukan parsing JSON
	 * yang relatif berat.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menggunakan dua flag boolean:
	 * {@code postingJurnalLoadingAktif} dan {@code postingJurnalReloadTertunda}.
	 * <ol>
	 *   <li>Jika {@code postingJurnalLoadingAktif} true saat dipanggil, set
	 *       {@code postingJurnalReloadTertunda = true} dan tampilkan pesan "tertunda",
	 *       kemudian return — tidak ada reload baru dimulai.</li>
	 *   <li>Jika tidak sedang loading, set flag aktif dan jalankan proses loading melalui
	 *       {@code Common.createDefaultTimer} (timer ZK singkat).</li>
	 *   <li>Dalam callback timer: tampilkan progress 48%, panggil
	 *       {@link #onSearchDefaultTanpaProgress} untuk query+render aktual, tampilkan 92%.</li>
	 *   <li>Di blok finally: reset flag aktif. Jika ada reload tertunda, jadwalkan ulang
	 *       dan tampilkan 96%; jika tidak, tampilkan selesai 100%.</li>
	 * </ol>
	 * Progress ditampilkan menggunakan {@code PostingJurnalLoadingUtil} dengan nilai 7%, 12%,
	 * 48%, 92%, 96%, 100%.</p>
	 *
	 * <p><strong>Threading:</strong> Semua operasi pada thread ZK event melalui timer;
	 * tidak ada thread Java tambahan di metode ini sendiri. Aman dari race condition ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception dari onSearchDefaultTanpaProgress
	 * diteruskan ke framework ZK; blok finally memastikan flag selalu direset.</p>
	 *
	 * <p><strong>Pemeliharaan:</strong> Persentase progress bersifat estimasi visual saja.
	 * Mekanisme flag ini mengasumsikan ZK single-thread per desktop; aman untuk ZK standard.</p>
	 *
	 * @param event event ZK yang memicu reload (dapat null untuk pemanggilan programatik)
	 */
	private void loadDataDenganProgressPosting(final org.zkoss.zk.ui.event.Event event) {
		if (postingJurnalLoadingAktif) {
			postingJurnalReloadTertunda = true;
			ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Ulang Data Posting Jurnal",
					"Permintaan reload baru diterima. Data akan dimuat ulang setelah proses yang berjalan selesai.", 12);
			return;
		}
		postingJurnalLoadingAktif = true;
		postingJurnalReloadTertunda = false;
		ais.ui.util.PostingJurnalLoadingUtil.show("Memuat Data Posting Jurnal",
				"Menyiapkan filter dan tabel data jurnal.", 7);
		Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event timerEvent) throws Exception {
				try {
					ais.ui.util.PostingJurnalLoadingUtil.update("Mengambil Data Posting Jurnal",
							"Mencari data sesuai tanggal, status posting, dan filter halaman.", 48);
					onSearchDefaultTanpaProgress(event);
					ais.ui.util.PostingJurnalLoadingUtil.update("Merapikan Tampilan",
							"Menyusun tabel, paging, status posting, dan preview jurnal.", 92);
				} finally {
					boolean reloadLagi = postingJurnalReloadTertunda;
					postingJurnalReloadTertunda = false;
					postingJurnalLoadingAktif = false;
					if (reloadLagi) {
						ais.ui.util.PostingJurnalLoadingUtil.update("Memuat Ulang Data Posting Jurnal",
								"Filter atau halaman berubah saat data sedang diproses. Data akan dimuat ulang sekarang.", 96);
						Common.createDefaultTimer(new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ulangEvent) throws Exception {
								loadDataDenganProgressPosting(event);
							}
						});
					} else {
						ais.ui.util.PostingJurnalLoadingUtil.complete("Data Posting Jurnal Siap",
								"Tabel sudah selesai dimuat dan siap digunakan.", 100);
					}
				}
			}
		});
	}

}
