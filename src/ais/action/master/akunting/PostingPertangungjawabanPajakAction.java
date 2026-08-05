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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Pajak;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
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
 * <h3>PostingPertangungjawabanPajakAction — Controller Posting Jurnal Pertanggungjawaban Pajak</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan controller ZK (GenericAutowireComposer) yang mengelola halaman posting
 * jurnal akuntansi untuk dokumen pajak (Pajak). Entitas Pajak merekam kewajiban perpajakan
 * yang timbul dari transaksi pengadaan barang/jasa, termasuk PPh pasal 21/22/23 dan PPN,
 * yang harus disetorkan ke kas negara. Halaman ini memungkinkan petugas keuangan untuk
 * memposting kewajiban pajak tersebut ke buku besar akuntansi, sehingga tercatat sebagai
 * beban pajak pada periode yang bersangkutan. Proses posting menghasilkan jurnal debet
 * pada akun pajak (JenisPajakBarang.akun) dan kredit pada akun titipan atau akun penerimaan
 * sesuai sumber transaksi.</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Setelah halaman dimuat, controller memeriksa parameter URL {@code sudah_posting} yang
 * memungkinkan halaman dibuka dalam mode "filter terkunci" dari dasbor jurnal posted/unposted.
 * Jika parameter ini ada, panel filter disembunyikan dan baris filter posting khusus
 * ditampilkan. Halaman juga mendukung parameter {@code mulai} dan {@code sampai} untuk
 * mengunci rentang tanggal dari pemanggil eksternal. Setiap entitas Pajak yang ditampilkan
 * oleh renderer {@code PajakRenderer} menentukan akun kredit menggunakan prioritas:
 * (1) akunDanaTitipan dari JenisPajakBarang terkait SaldoAwalMasterAssetDetail,
 * (2) akun JenisPajakBarang dari detail, (3) akun JenisPenerimaanBarang, (4) akun
 * JenisUangMuka dari pertanggungjawaban terkait. Akun debet selalu dari
 * JenisPajakBarang.akun milik entitas Pajak itu sendiri. Nilai pajak yang diposting
 * adalah field {@code nilai} dari entitas Pajak.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Posting massal (onPostingSemua) dijalankan dalam thread latar belakang menggunakan
 * {@code HibernateUtil.currentNativeSession()} dengan manajemen transaksi eksplisit.
 * Ada kasus khusus dalam renderer: jika Pajak memiliki SaldoAwalMasterAssetDetail
 * dengan JenisPajakBarang null, baris tersebut dihapus secara hard-delete menggunakan
 * native session langsung di thread ZK renderer (bukan background thread). Ini adalah
 * operasi pembersihan data yang berlangsung saat render baris.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Field {@code ref} bernilai "pajak" digunakan sebagai diskriminator dalam tabel
 * grup_transaksi untuk membedakan jurnal pajak dari jurnal lain pada entitas yang sama.
 * Jika nilai ref berubah, query SQL delete saat batalkan posting juga harus diperbarui.
 * Prioritas penentuan akun kredit pajak kompleks (4 cabang if-else) — pastikan semua
 * kasus diuji saat ada perubahan pada model SaldoAwalMasterAssetDetail atau JenisPajakBarang.
 * Parameter {@code sudah_posting} digunakan untuk integrasi dengan dasbor jurnal yang
 * menampilkan ringkasan posting per periode.</p>
 *
 * @see Pajak
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 * @see SaldoAwalMasterAssetDetail
 */
public class PostingPertangungjawabanPajakAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai Serializable ZK composer.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar Pajak dengan status postingnya. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data pada grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode atau nama pajak. */
	private Textbox searchkode;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang belum diposting.
	 * Jika dicentang, hanya Pajak dengan postingHistory belum di-posting yang ditampilkan.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang sudah diposting.
	 * Jika dicentang, hanya Pajak dengan postingHistory sudah di-posting yang ditampilkan.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model tree satuan kerja untuk filter hierarki organisasi.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilihan satuan kerja sebagai filter.
	 * Perubahan satuan kerja memicu onSearchDefault.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * Flag apakah pengguna memiliki hak UPDATE. Menentukan visibilitas tombol aksi.
	 */
	private boolean edit = false;

	/** Tombol "Posting Semua" di toolbar, hanya tampil jika pengguna memiliki hak UPDATE. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE.
	 */
	public boolean adminLain;

	/** Datebox tanggal mulai filter rentang tanggal transaksi pajak. */
	private MyDatebox tglMulai;

	/** Datebox tanggal sampai filter rentang tanggal transaksi pajak. */
	private MyDatebox tglSampai;

	/**
	 * Diskriminator ref untuk membedakan jurnal pajak dari jurnal lain.
	 * Digunakan dalam SQL delete saat batalkan posting.
	 */
	private String ref = "pajak";

	/** Pengguna yang sedang login. */
	private Tbmuser tbmuser;

	/**
	 * Panel filter utama di bagian North layout. Disembunyikan jika halaman dibuka
	 * dari dasbor dengan parameter sudah_posting.
	 */
	private North filter;

	/**
	 * Baris filter khusus posting yang ditampilkan ketika sudah_posting tidak null.
	 * Menggantikan panel filter standar saat halaman dibuka dari dasbor.
	 */
	private Row rowPosting;

	/**
	 * Status posting yang diterima dari parameter URL {@code sudah_posting}.
	 * Null = mode normal (semua), true = hanya sudah posting, false = hanya belum posting.
	 * Ketika tidak null, panel filter disembunyikan dan mode tampilan terkunci.
	 */
	private Boolean sudah_posting = null;

	/**
	 * Intercept lifecycle ZK sebelum halaman di-compose untuk memeriksa keamanan akses.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman ini.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} lalu
	 * mendelegasikan ke superclass.</p>
	 *
	 * @param page     Halaman ZK yang sedang di-compose.
	 * @param parent   Komponen induk dalam hierarki ZK.
	 * @param compInfo Informasi metadata komponen dari ZUL.
	 * @return ComponentInfo yang diteruskan ke superclass.
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Inisialisasi halaman setelah seluruh komponen ZUL selesai di-compose dan di-wire.
	 * Mendukung dua mode operasi: mode normal dari menu, dan mode terkunci dari dasbor
	 * dengan parameter URL.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua komponen UI: filter tanggal, filter
	 * satuan kerja, hak akses, listener paging, dan memuat data awal. Mendukung
	 * parameter URL untuk integrasi dengan dasbor jurnal.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menjalankan auto-wire ZK via superclass, lalu menginisialisasi bahasa.</li>
	 *   <li>Memeriksa sesi dan hak READ; jika tidak valid, langsung goLogoff.</li>
	 *   <li>Membaca parameter URL {@code sudah_posting}: jika ada, mengunci mode tampilan
	 *       (menyembunyikan filter, menampilkan rowPosting).</li>
	 *   <li>Jika searchparent null (mode embedded/terbatas), langsung return tanpa
	 *       inisialisasi lebih lanjut (safety guard).</li>
	 *   <li>Memasang listener searchparent untuk trigger pencarian saat satuan kerja dipilih.</li>
	 *   <li>Membuat model tree satuan kerja.</li>
	 *   <li>Menetapkan tanggal default (6 bulan terakhir), dengan kemungkinan override
	 *       dari parameter URL {@code mulai} dan {@code sampai} (format dateFormat8).</li>
	 *   <li>Menentukan flag adminLain dan edit dari hak akses.</li>
	 *   <li>Memasang listener paging dan timer auto-refresh.</li>
	 *   <li>Memuat data awal melalui loadDataDenganProgressPosting.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Parameter URL {@code mulai}/{@code sampai} menggunakan
	 * format {@code Common.dateFormat8} — pastikan format ini konsisten dengan format yang
	 * digunakan caller (dasbor atau halaman lain yang membuka halaman ini dengan parameter).</p>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai di-compose.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen.
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
		if (searchparent == null) return;
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
	 * Event handler untuk membatalkan posting semua pajak yang tampil di grid.
	 * Menghapus PostingHistory dan GrupTransaksi terkait secara massal setelah konfirmasi.
	 *
	 * <p><strong>Tujuan:</strong> Membatalkan posting massal untuk semua Pajak yang sudah
	 * diposting sesuai filter aktif saat ini, sehingga dapat diposting ulang jika ada
	 * koreksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi.</li>
	 *   <li>Jika OK: mengambil semua Pajak dengan postingHistory not null.</li>
	 *   <li>Untuk setiap Pajak: set postingHistory = null dan simpan.</li>
	 *   <li>Menghapus GrupTransaksi terkait dengan SQL: {@code ref='pajak' and pajak=id
	 *       and closing is null}. Kondisi {@code ref='pajak'} penting untuk memastikan hanya
	 *       jurnal pajak yang dihapus, karena entitas Pajak mungkin memiliki hubungan dengan
	 *       GrupTransaksi dari sumber lain.</li>
	 *   <li>Memuat ulang data melalui timer ZK.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Nilai {@code ref} harus konsisten dengan field
	 * {@code ref} pada kelas ini (bernilai "pajak").</p>
	 *
	 * @param event Event ZK dari tombol "Batalkan Posting Semua".
	 * @throws Exception Jika terjadi kesalahan database.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pajak ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Pajak> pajaks = initCriteria(true).add(Restrictions.isNotNull("postingHistory"))
									.list();

							for (Pajak pajak : pajaks) {
								pajak.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pajak);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where ref='" + ref
												+ "' and pajak=" + pajak.getId() + " and closing is null")
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
	 * Event handler untuk memposting semua pajak yang belum diposting secara massal.
	 * Menampilkan dialog form isian, lalu menjalankan posting di thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memposting seluruh entitas Pajak yang belum diposting
	 * dalam satu batch, menghasilkan satu PostingHistory dan banyak GrupTransaksi jurnal.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Membuat dialog modal dengan form tanggal, nama pengguna, keterangan.</li>
	 *   <li>Setelah konfirmasi, memulai thread background.</li>
	 *   <li>Thread posting:
	 *     <ul>
	 *       <li>Membuat PostingHistory dengan jenis JENIS_PERTANGGUNGJAWABAN_PAJAK.</li>
	 *       <li>Mengambil semua Pajak belum diposting dari initCriteria.</li>
	 *       <li>Untuk setiap Pajak: menentukan akun kredit menggunakan prioritas 4 cabang
	 *           (akunDanaTitipan > akun JenisPajakBarang detail > akun JenisPenerimaanBarang
	 *           > akun JenisUangMuka pertanggungjawaban).</li>
	 *       <li>Menentukan akun debet dari Pajak.getJenisPajakBarang().getAkun().</li>
	 *       <li>Memanggil CommonAkunting.saveTransaksi dengan parameter {@code ref} untuk
	 *           memberi diskriminator pada GrupTransaksi yang dihasilkan.</li>
	 *       <li>Menautkan PostingHistory ke Pajak dan menyimpan dengan refreshUpdate.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Thread background menggunakan currentNativeSession().
	 * Perhatikan bahwa initCriteria() dipanggil dari dalam thread background menggunakan
	 * currentNativeSession yang sama — ini berbeda dari pola di kelas lain yang mengambil
	 * ID terlebih dahulu di thread ZK.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per entitas tidak ditangani secara
	 * eksplisit di level luar; exception akan hilang diam-diam (blok catch kosong).
	 * Exception di level inner (saveTransaksi) ditangkap dan ditampilkan jika admin.</p>
	 *
	 * @param event Event ZK dari tombol "Posting Semua".
	 * @throws Exception Jika terjadi kesalahan saat membangun komponen UI dialog.
	 */
	public void onPostingSemua(Event event) throws Exception {

		final MyWindow addWindow = new MyWindow();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle("Posting Transaksi Pertanggungjawaban");
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pajak ?", "Pertanyaan",
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
											MyMessageboxConfig.show("Posting transaksi pajak berhasil dilakukan",
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
													PostingHistory.JENIS_PERTANGGUNGJAWABAN_PAJAK);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<Pajak> pajaks = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (Pajak pajak : pajaks) {

												SatuanKerja satuanKerja = (SatuanKerja) (pajak.getSatuanKerja() != null
														? pajak.getSatuanKerja()
														: null);

												if (pajak != null) {

													try {
														Akun akunKredit = pajak.getPertangungjawaban() == null
																|| pajak.getPertangungjawaban().getUangMuka() == null
																|| pajak.getPertangungjawaban().getUangMuka()
																		.getJenisUangMuka() == null
																				? null
																				: pajak.getPertangungjawaban()
																						.getUangMuka()
																						.getJenisUangMuka().getAkun();

														if (pajak.getSaldoAwalMasterAssetDetail() != null && pajak
																.getSaldoAwalMasterAssetDetail()
																.getPenerimaanPengadaanMasterAssetDetail() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getPenerimaanPengadaanMasterAssetDetail()
																		.getPenerimaanPengadaanMasterAsset() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getPenerimaanPengadaanMasterAssetDetail()
																		.getPenerimaanPengadaanMasterAsset()
																		.getJenisPenerimaanBarang() != null) {
															akunKredit = pajak.getSaldoAwalMasterAssetDetail()
																	.getPenerimaanPengadaanMasterAssetDetail()
																	.getPenerimaanPengadaanMasterAsset()
																	.getJenisPenerimaanBarang().getAkun();

														}
														if (pajak.getSaldoAwalMasterAssetDetail() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getJenisPajakBarang() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getJenisPajakBarang().getAkun() != null) {
															akunKredit = pajak.getSaldoAwalMasterAssetDetail()
																	.getJenisPajakBarang().getAkun();
														}

														if (pajak.getSaldoAwalMasterAssetDetail() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getJenisPajakBarang() != null
																&& pajak.getSaldoAwalMasterAssetDetail()
																		.getJenisPajakBarang()
																		.getAkunDanaTitipan() != null) {
															akunKredit = pajak.getSaldoAwalMasterAssetDetail()
																	.getJenisPajakBarang().getAkunDanaTitipan();
														}
														Akun akunDebet = pajak.getJenisPajakBarang().getAkun();

														if (akunDebet != null && akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Laporan pajak \"" + pajak.getKode() + " "
																		+ pajak.getNama() + "\" senilai "
																		+ Common.numberFormat.get().format(pajak.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(
																	ket + " ("
																			+ Common.numberFormat.get().format(
																					rowIndex * 100.0 / pajaks.size())
																			+ " %)");

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																List<Akun> akunsDebets = new ArrayList<Akun>();
																List<Akun> akunsKredits = new ArrayList<Akun>();

																List<Double> nilaiDebets = new ArrayList<Double>();
																List<Double> nilaiKredits = new ArrayList<Double>();

																nilaiDebets.add(pajak.getNilai());
																akunsDebets.add(akunDebet);

																akunsKredits.add(akunKredit);
																nilaiKredits.add(pajak.getNilai());

																session.getTransaction().begin();

																CommonAkunting.saveTransaksi(
																		akunsDebets.toArray(new Akun[] {}),
																		akunsKredits.toArray(new Akun[] {}), akunDenda,
																		akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket, tgl,
																		nilaiDebets.toArray(new Double[] {}),
																		nilaiKredits.toArray(new Double[] {}), denda,
																		pajak, satuanKerja, ref, session);
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pajak.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															Common.refreshUpdate(session, pajak);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingPertangungjawabanPajakAction.java:678");
														// lanjut ke entitas berikutnya
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

	/**
	 * Renderer baris grid untuk menampilkan satu entri Pajak beserta status posting,
	 * detail pembayaran pajak, pratinjau jurnal, dan tombol aksi per baris.
	 *
	 * <p><strong>Tujuan:</strong> Merender setiap baris grid dengan informasi lengkap
	 * satu Pajak: kode, nama, link proses transfer terkait, nilai pajak, NTPN (Nomor
	 * Transaksi Penerimaan Negara), NPWP, nama wajib pajak, tanggal setoran, tanggal
	 * transaksi, pratinjau jurnal debet-kredit, status posting, dan tombol aksi.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Kasus khusus: jika Pajak memiliki SaldoAwalMasterAssetDetail tetapi
	 *       JenisPajakBarang dari detail tersebut null, entitas Pajak dihapus langsung
	 *       dari database menggunakan native session (pembersihan data cacat), baris
	 *       disembunyikan (setVisible(false)), dan return lebih awal.</li>
	 *   <li>Menampilkan kode revisi dengan RevisiHelper, nama Pajak.</li>
	 *   <li>Jika ada link ke ProsesTransfer melalui Pertangungjawaban, tampilkan link.</li>
	 *   <li>Menampilkan nilai, NTPN, NPWP, nama WP, tanggal setoran, tanggal transaksi.</li>
	 *   <li>Menentukan akun kredit dengan prioritas 4 cabang (akunDanaTitipan >
	 *       akun JenisPajakBarang detail > akun JenisPenerimaanBarang > akun JenisUangMuka).</li>
	 *   <li>Menentukan akun debet dari Pajak.getJenisPajakBarang().getAkun().</li>
	 *   <li>Menampilkan pratinjau jurnal atau pesan error jika akun tidak lengkap.</li>
	 *   <li>Menampilkan status posting dan nomor bukti.</li>
	 *   <li>Tombol "Batalkan Posting" dan "Posting Data" per baris.</li>
	 * </ol>
	 *
	 * <p><strong>Perhatian threading:</strong> Kasus khusus penghapusan data cacat menggunakan
	 * native session langsung di thread ZK renderer — ini tidak ideal untuk performa
	 * tetapi diperlukan untuk membersihkan data tanpa menampilkan baris tidak valid ke user.</p>
	 */
	class PajakRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi komponen UI untuk satu baris grid dari objek Pajak.
		 * Dipanggil oleh ZK framework untuk setiap item dalam model data grid.
		 *
		 * <p><strong>Tujuan:</strong> Merender satu baris tabel pajak dengan semua informasi
		 * relevan untuk keperluan review dan aksi posting/pembatalan.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Menangani kasus khusus data cacat, kemudian
		 * menampilkan semua kolom informasi pajak beserta pratinjau jurnal dan tombol aksi.
		 * Lihat dokumentasi kelas {@code PajakRenderer} untuk penjelasan lengkap.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Data cacat (JenisPajakBarang null) dihapus
		 * langsung. Exception lain diteruskan ke ZK framework.</p>
		 *
		 * @param arg0 Baris ZK (Row) wadah komponen kolom.
		 * @param arg1 Objek data, diharapkan bertipe Pajak.
		 * @throws Exception Jika terjadi kesalahan akses database atau pembuatan komponen UI.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pajak pajak = (Pajak) arg1;

			SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = pajak.getSaldoAwalMasterAssetDetail();
			if (pajak != null && saldoAwalMasterAssetDetail != null
					&& saldoAwalMasterAssetDetail.getJenisPajakBarang() == null) {
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshDelete(session, pajak);
				session.getTransaction().commit();
				if (session.isOpen()) {session.disconnect();session.close();}
				HibernateUtil.closeSession();
				arg0.setVisible(false);
				return;
			}

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(Pajak.class, pajak, pajak.getKode() == null ? "" : pajak.getKode()))
					.setParent(arg0);
			new Label(pajak.getNama()).setParent(aaa);

			if (pajak != null && pajak.getPertangungjawaban() != null
					&& pajak.getPertangungjawaban().getDaftarPengajuanTransfer() != null
					&& pajak.getPertangungjawaban().getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(pajak.getPertangungjawaban().getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pajak.getPertangungjawaban().getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);

			}

			new Label(Common.numberFormat.get().format(pajak.getNilai())).setParent(arg0);

			new Label(pajak.getNtpn()).setParent(arg0);
			new Label(pajak.getNpwp()).setParent(arg0);
			new Label(pajak.getNamaWp()).setParent(arg0);
			new Label(pajak.getTanggalStor() == null ? "" : Common.dateFormat3.get().format(pajak.getTanggalStor()))
					.setParent(arg0);

			new Label(pajak.getTanggalTransaksi() == null ? "" : Common.dateFormat1.get().format(pajak.getTanggalTransaksi()))
					.setParent(arg0);

			Akun akunKredit = pajak.getPertangungjawaban() == null || pajak.getPertangungjawaban().getUangMuka() == null
					|| pajak.getPertangungjawaban().getUangMuka().getJenisUangMuka() == null ? null
							: pajak.getPertangungjawaban().getUangMuka().getJenisUangMuka().getAkun();

			if (pajak.getSaldoAwalMasterAssetDetail() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
							.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang() != null) {
				akunKredit = pajak.getSaldoAwalMasterAssetDetail().getPenerimaanPengadaanMasterAssetDetail()
						.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang().getAkun();

			}

			if (pajak.getSaldoAwalMasterAssetDetail() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang().getAkun() != null) {
				akunKredit = pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang().getAkun();
			}

			if (pajak.getSaldoAwalMasterAssetDetail() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang() != null
					&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang().getAkunDanaTitipan() != null) {
				akunKredit = pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang().getAkunDanaTitipan();
			}

			Akun akunDebet = pajak.getJenisPajakBarang().getAkun();

			if (akunKredit != null && akunDebet != null) {

				GrupTransaksi.tampilkanJurnal(akunDebet, pajak.getNilai(), akunKredit, pajak.getNilai())
						.setParent(arg0);

			} else {
				new Label("Transaksi tidak valid."
						+ (akunDebet != null ? " Debet: " + akunDebet.getKode() + "-" + akunDebet.getNama() + "."
								: " Akun debet tidak ada.")
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class).add(Restrictions.eq("pajak", pajak))
					.setMaxResults(1).setProjection(Projections.property("kode")).uniqueResult();

			new Label(pajak.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pajak.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pajak.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pajak.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pajak);
								HibernateUtil.currentSession()
										.createSQLQuery("delete from akunting.grup_transaksi where ref='" + ref
												+ "' and pajak=" + pajak.getId() + " and closing is null")
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
				button.setVisible(edit && pajak.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERTANGGUNGJAWABAN_PAJAK);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunKredit = pajak.getPertangungjawaban() == null
										|| pajak.getPertangungjawaban().getUangMuka() == null
										|| pajak.getPertangungjawaban().getUangMuka().getJenisUangMuka() == null ? null
												: pajak.getPertangungjawaban().getUangMuka().getJenisUangMuka()
														.getAkun();

								if (pajak.getSaldoAwalMasterAssetDetail() != null
										&& pajak.getSaldoAwalMasterAssetDetail()
												.getPenerimaanPengadaanMasterAssetDetail() != null
										&& pajak.getSaldoAwalMasterAssetDetail()
												.getPenerimaanPengadaanMasterAssetDetail()
												.getPenerimaanPengadaanMasterAsset() != null
										&& pajak.getSaldoAwalMasterAssetDetail()
												.getPenerimaanPengadaanMasterAssetDetail()
												.getPenerimaanPengadaanMasterAsset()
												.getJenisPenerimaanBarang() != null) {
									akunKredit = pajak.getSaldoAwalMasterAssetDetail()
											.getPenerimaanPengadaanMasterAssetDetail()
											.getPenerimaanPengadaanMasterAsset().getJenisPenerimaanBarang().getAkun();

								}

								if (pajak.getSaldoAwalMasterAssetDetail() != null
										&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang() != null
										&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang()
												.getAkun() != null) {
									akunKredit = pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang().getAkun();
								}

								if (pajak.getSaldoAwalMasterAssetDetail() != null
										&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang() != null
										&& pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang()
												.getAkunDanaTitipan() != null) {
									akunKredit = pajak.getSaldoAwalMasterAssetDetail().getJenisPajakBarang()
											.getAkunDanaTitipan();
								}

								Akun akunDebet = pajak.getJenisPajakBarang().getAkun();

								if (akunDebet != null && akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Laporan pajak \"" + pajak.getKode() + " " + pajak.getNama()
												+ "\" senilai " + Common.numberFormat.get().format(pajak.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (pajak.getSatuanKerja() != null
											? pajak.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									List<Akun> akunsDebets = new ArrayList<Akun>();
									List<Akun> akunsKredits = new ArrayList<Akun>();

									List<Double> nilaiDebets = new ArrayList<Double>();
									List<Double> nilaiKredits = new ArrayList<Double>();

									nilaiDebets.add(pajak.getNilai());
									akunsDebets.add(akunDebet);

									akunsKredits.add(akunKredit);
									nilaiKredits.add(pajak.getNilai());

									CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
											akunsKredits.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
											postingHistory, apakahUangMasuk, ket, pajak.getTanggalTransaksi(),
											nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
											denda, pajak, satuanKerja, ref, session);

									pajak.setPostingHistory(postingHistory);
									Common.refreshUpdate(session, pajak);
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
	 * Membuat dan mengembalikan objek Criteria Hibernate untuk query Pajak sesuai semua
	 * filter yang aktif, mendukung dua mode: mode normal (dari menu) dan mode sudah_posting
	 * (dari dasbor jurnal).
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan definisi filter data yang terpusat, mendukung
	 * mode tampilan normal dan mode terkunci dari dasbor dengan penanganan filter berbeda.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Jika searchparent null, return null (mode terbatas tanpa filter satuan kerja).</li>
	 *   <li>Mengambil satuan kerja yang dipilih dan child-nya untuk filter hierarki.</li>
	 *   <li>Mode sudah_posting tidak null (dari dasbor):
	 *     <ul>
	 *       <li>Filter nilai > 0, aktif, rentang tanggal transaksi.</li>
	 *       <li>Jika sudah_posting=true: join postingHistory dan filter posting=true.</li>
	 *       <li>Jika sudah_posting=false: filter postingHistory null atau posting=false.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Mode normal (sudah_posting null):
	 *     <ul>
	 *       <li>Checkbox searchtampil/searchtelahtampil saling mengecualikan (set checked
	 *           checkbox lain ke false jika satu dicentang); keduanya filter posting=false
	 *           atau null (logika filter postingHistory berbeda dari kelas lain).</li>
	 *       <li>Filter nilai > 0.01 (lebih ketat dari kelas lain yang pakai != 0.0).</li>
	 *       <li>Filter satuan kerja hierarki.</li>
	 *       <li>Filter tanggal transaksi.</li>
	 *       <li>Filter teks kode dan nama.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika order=true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Logika filter checkbox di mode normal memiliki
	 * perilaku berbeda dari kelas lain — keduanya menghasilkan filter yang sama (posting
	 * false atau null). Ini kemungkinan bug atau kebutuhan bisnis khusus. Jangan mengubah
	 * logika ini tanpa verifikasi dengan stakeholder.</p>
	 *
	 * @param order Jika true, menambahkan ORDER BY id DESC.
	 * @return Criteria siap dieksekusi, atau null jika searchparent tidak tersedia.
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Pajak.class);

		// Konsisten dengan tampilan Data Pajak (PertangungjawabanPajakAction): baris
		// PPh PER-ITEM tidak ikut tampil/diposting bila tagihan vendornya breakdown=ya
		// — pada mode breakdown PPh yang sah = "Bukti Potong" (baris tertaut langsung
		// ke tagihan, tanpa detail). LEFT JOIN agar baris Bukti Potong tetap ikut.
		criteria.createAlias("saldoAwalMasterAssetDetail", "bdDetail", Criteria.LEFT_JOIN)
				.createAlias("bdDetail.saldoAwal", "bdTagihan", Criteria.LEFT_JOIN)
				.add(Restrictions.disjunction()
						.add(Restrictions.isNull("bdDetail.id"))
						.add(Restrictions.isNull("bdTagihan.breakdownAktif"))
						.add(Restrictions.eq("bdTagihan.breakdownAktif", false)));

		if (sudah_posting != null) {

			try {

				criteria.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
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

			criteria.add(Restrictions.gt("nilai", 0.01))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
							satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
									: Restrictions.or(
											parent == null ? Restrictions.isNull("satuanKerja")
													: Restrictions.sqlRestriction("false"),
											Restrictions.in("satuanKerja", satuanKerjas))))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_transaksi) between date('"
							+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
							+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

					.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
									Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat dan menampilkan data Pajak ke grid tanpa progress bar.
	 * Hanya dipanggil dari {@code loadDataDenganProgressPosting} setelah progress bar aktif.
	 *
	 * <p><strong>Tujuan:</strong> Mengeksekusi query dan mengisi grid halaman aktif
	 * dengan renderer PajakRenderer.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika initCriteria mengembalikan null (searchparent
	 * null), pemanggilan list() akan NullPointerException. Caller harus memastikan
	 * searchparent tidak null sebelum memanggil metode ini.</p>
	 *
	 * @param event Event ZK yang memicu reload, dapat null.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pajak> pajak = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pajak);
		grid.setRowRenderer(new PajakRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Event handler publik untuk memuat ulang data grid dengan progress bar.
	 * Dipanggil oleh ZK framework ketika event onSearchDefault diterima.
	 *
	 * <p><strong>Tujuan:</strong> Titik masuk publik untuk refresh data grid.</p>
	 *
	 * @param event Event ZK yang memicu pencarian ulang.
	 */
	public void onSearchDefault(Event event) {
		loadDataDenganProgressPosting(event);
	}

	/**
	 * Flag untuk mencegah dua proses loading data berjalan bersamaan.
	 */
	private boolean postingJurnalLoadingAktif = false;

	/**
	 * Flag yang menandai ada permintaan reload baru saat loading sedang berlangsung.
	 */
	private boolean postingJurnalReloadTertunda = false;

	/**
	 * Memuat data posting jurnal pajak ke grid dengan menampilkan progress bar.
	 * Mencegah pemuatan ganda dan menangani permintaan reload yang masuk saat loading aktif.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan UX yang lebih baik dengan indikator progress
	 * dan mencegah race condition pada load bersamaan akibat interaksi pengguna yang cepat.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Jika sudah ada loading aktif: set flag reload tertunda dan return.</li>
	 *   <li>Set flag loading aktif, tampilkan progress bar awal (7%).</li>
	 *   <li>Jalankan loading via timer ZK: update progress 48%, load data, update 92%.</li>
	 *   <li>Di finally: reset flag. Jika ada reload tertunda, mulai loading baru.
	 *       Jika tidak, tandai selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Seluruh metode berjalan di thread ZK event.
	 * Timer memisahkan render UI dari eksekusi query database.</p>
	 *
	 * @param event Event ZK yang memicu loading, dapat null.
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
