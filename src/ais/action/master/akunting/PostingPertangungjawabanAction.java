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
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.JenisPajakBarang;
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
 * <h3>PostingPertangungjawabanAction — Controller Posting Jurnal Pertanggungjawaban Uang Muka</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan controller ZK (GenericAutowireComposer) yang mengelola halaman posting
 * jurnal akuntansi untuk laporan pertanggungjawaban uang muka (advance payment accountability).
 * Uang muka adalah dana yang diberikan terlebih dahulu kepada pegawai atau pihak ketiga untuk
 * melaksanakan kegiatan atau pengadaan tertentu, yang kemudian harus dipertanggungjawabkan
 * dengan menyerahkan bukti pengeluaran. Pertanggungjawaban ini mencatat berapa yang benar-benar
 * digunakan, berapa sisa yang dikembalikan, berapa yang berasal dari sponsor, dan berapa
 * kewajiban pajak yang timbul. Kelas ini memproses entitas {@code Pertangungjawaban} yang
 * sudah disetujui (disetujuiOleh tidak null) dan mengubahnya menjadi entri jurnal
 * GrupTransaksi di sistem akuntansi.</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Setelah halaman dimuat, controller memeriksa parameter URL {@code sudah_posting},
 * {@code mulai}, dan {@code sampai} untuk mendukung mode terkunci dari dasbor. Data
 * Pertangungjawaban ditampilkan oleh renderer {@code PertangungjawabanRenderer} yang mengurai
 * formula JSON pengeluaran untuk menghitung nilai total aktual. Setiap item formula terdiri
 * dari jumlah, persentase PPN, dan referensi JenisPajakBarang untuk PPh. Nilai total
 * dihitung sebagai: jumlah + PPN% - PPh% (jika konfigurasi pph_mengurangi_lpj aktif).
 * Jurnal posting melibatkan akun uang muka (UangMuka.akun) sebagai debet, akun penerima
 * (JenisUangMuka.akun) sebagai kredit utama, akun kelebihan (JenisUangMuka.akunKelebihan)
 * jika ada selisih yang dikembalikan, akun sponsor (JenisUangMuka.akunSponsor) jika ada
 * dana dari sponsor, serta akun pajak per JenisPajakBarang di sisi kredit.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Posting massal (onPostingSemua) dijalankan dalam thread latar belakang menggunakan
 * {@code HibernateUtil.currentNativeSession()} dengan manajemen transaksi eksplisit.
 * Konfigurasi {@code pph_mengurangi_lpj} dibaca sekali saat inisialisasi kelas.
 * Logika pembentukan akun yang kompleks dipisahkan ke metode {@code populateAkun}
 * yang dapat dipanggil baik dari thread background maupun dari thread ZK event.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Logika {@code populateAkun} menangani 4 kombinasi: (ada/tidak ada sponsor) x
 * (ada/tidak ada selisih). Perubahan kebijakan akuntansi terkait uang muka, sponsor,
 * atau pengembalian harus direfleksikan di metode ini. Formula JSON disimpan di field
 * {@code formula} pada entitas Pertangungjawaban — format: {@code [{"jumlah":..., "ppn":...,
 * "pajak":id}, ...]}. Jika format berubah, logika parsing di renderer dan thread posting
 * harus diperbarui bersama. Mode filter berbasis parameter URL (sudah_posting) digunakan
 * untuk integrasi dengan dasbor ringkasan jurnal.</p>
 *
 * @see Pertangungjawaban
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingPertangungjawabanAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai Serializable ZK composer.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar Pertangungjawaban dengan status postingnya. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data pada grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode, nama, atau kode uang muka. */
	private Textbox searchkode;

	/**
	 * Checkbox filter untuk menampilkan hanya pertanggungjawaban yang belum diposting.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter untuk menampilkan hanya pertanggungjawaban yang sudah diposting.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model tree satuan kerja untuk filter hierarki organisasi.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilihan satuan kerja sebagai filter.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * Flag apakah pengguna memiliki hak UPDATE.
	 */
	private boolean edit = false;

	/** Tombol "Posting Semua" di toolbar. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE.
	 */
	public boolean adminLain;

	/** Datebox tanggal mulai filter rentang tanggal persetujuan pertanggungjawaban. */
	private MyDatebox tglMulai;

	/** Datebox tanggal sampai filter rentang tanggal persetujuan pertanggungjawaban. */
	private MyDatebox tglSampai;

	/**
	 * Panel filter utama (North layout). Disembunyikan jika halaman dibuka dengan
	 * parameter sudah_posting dari dasbor jurnal.
	 */
	private North filter;

	/** Pengguna yang sedang login. */
	private Tbmuser tbmuser;

	/**
	 * Baris filter khusus posting yang ditampilkan ketika sudah_posting tidak null.
	 * Menggantikan panel filter standar saat halaman dibuka dari dasbor.
	 */
	private Row rowPosting;

	/**
	 * Status posting dari parameter URL {@code sudah_posting}.
	 * Null = mode normal (semua), true = hanya sudah posting, false = hanya belum posting.
	 */
	private Boolean sudah_posting = null;

	/**
	 * Intercept lifecycle ZK sebelum halaman di-compose untuk memeriksa keamanan akses.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman posting pertanggungjawaban uang muka.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} lalu
	 * mendelegasikan ke superclass untuk melanjutkan proses compose normal.</p>
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
	 * Mendukung dua mode: mode normal dari menu navigasi, dan mode terkunci dari dasbor
	 * dengan parameter URL.
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua komponen UI halaman posting
	 * pertanggungjawaban uang muka, mendukung integrasi dengan dasbor jurnal melalui
	 * parameter URL.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Auto-wire ZK via superclass, kemudian inisialisasi bahasa.</li>
	 *   <li>Periksa sesi dan hak READ; jika tidak valid, langsung goLogoff.</li>
	 *   <li>Baca parameter URL {@code sudah_posting}: jika ada, sembunyikan filter,
	 *       tampilkan rowPosting (mode terkunci dari dasbor).</li>
	 *   <li>Guard: jika searchparent null (mode embedding), langsung return.</li>
	 *   <li>Pasang listener searchparent untuk trigger pencarian saat satuan kerja berubah.</li>
	 *   <li>Buat model tree satuan kerja.</li>
	 *   <li>Tetapkan tanggal default (6 bulan terakhir), dengan override dari parameter
	 *       URL {@code mulai}/{@code sampai} jika ada (datebox dinonaktifkan setelah diisi).</li>
	 *   <li>Tentukan flag adminLain dan edit dari hak akses pengguna saat ini.</li>
	 *   <li>Pasang listener paging dan timer auto-refresh.</li>
	 *   <li>Muat data awal via loadDataDenganProgressPosting.</li>
	 * </ol>
	 *
	 * @param comp Komponen root halaman ZK yang telah selesai di-compose.
	 * @throws Exception Jika terjadi kesalahan saat inisialisasi komponen ZK.
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
	 * Event handler untuk membatalkan posting semua pertanggungjawaban yang tampil di grid.
	 * Menghapus PostingHistory dan GrupTransaksi terkait secara massal setelah konfirmasi.
	 *
	 * <p><strong>Tujuan:</strong> Membatalkan posting massal untuk semua Pertangungjawaban
	 * yang sudah diposting sesuai filter aktif, agar dapat dilakukan koreksi atau posting
	 * ulang dengan data yang benar.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi dengan pesan yang menyebutkan "pertanggungjawaban".</li>
	 *   <li>Jika OK: mengambil semua Pertangungjawaban dengan postingHistory not null.</li>
	 *   <li>Untuk setiap entitas: set postingHistory = null, simpan, lalu hapus GrupTransaksi
	 *       terkait menggunakan SQL native dengan filter {@code ref is null} untuk membedakan
	 *       dari jurnal pajak yang memiliki ref berbeda.</li>
	 *   <li>Memuat ulang data via timer ZK.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Kondisi {@code ref is null} mengidentifikasi jurnal
	 * posting utama (bukan jurnal pajak yang menggunakan ref berbeda). Jika logika ref
	 * berubah pada saveTransaksi, kondisi delete ini harus diperbarui bersama.</p>
	 *
	 * @param event Event ZK dari tombol "Batalkan Posting Semua".
	 * @throws Exception Jika terjadi kesalahan database.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pertanggungjawaban ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<Pertangungjawaban> pertangungjawabans = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (Pertangungjawaban pertangungjawaban : pertangungjawabans) {
								pertangungjawaban.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pertangungjawaban);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where ref is null and pertangungjawaban="
												+ pertangungjawaban.getId() + " and closing is null")
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
	 * Event handler untuk memposting semua pertanggungjawaban uang muka secara massal.
	 * Menampilkan dialog form isian dengan tanggal dan keterangan, lalu menjalankan
	 * posting di thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memposting seluruh Pertangungjawaban yang belum diposting
	 * dalam satu batch, menghasilkan satu PostingHistory (JENIS_PERTANGGUNGJAWABAN_UANG_MUKA)
	 * dan banyak GrupTransaksi sebagai entri buku besar akuntansi.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Membuat jendela modal dengan form: tanggal posting, nama pengguna, keterangan.</li>
	 *   <li>Tombol Simpan memvalidasi tanggal, lalu meminta konfirmasi kedua.</li>
	 *   <li>Setelah konfirmasi, menampilkan progress bar dan memulai thread background.</li>
	 *   <li>Thread posting:
	 *     <ul>
	 *       <li>Membuat PostingHistory baru dan menyimpannya dalam transaksi tersendiri.</li>
	 *       <li>Mengambil semua Pertangungjawaban belum diposting dari initCriteria.</li>
	 *       <li>Untuk setiap Pertangungjawaban: mengurai formula JSON untuk menghitung
	 *           nilai total dan daftar akun pajak (PPh per JenisPajakBarang).</li>
	 *       <li>Menentukan akun debet (UangMuka.akun), akun kredit utama
	 *           (JenisUangMuka.akun), akun kelebihan (JenisUangMuka.akunKelebihan),
	 *           akun sponsor (JenisUangMuka.akunSponsor), dan nilai sponsor.</li>
	 *       <li>Hanya memposting jika akun debet, kredit, DAN kelebihan semuanya tidak null
	 *           (berbeda dengan kelas lain yang hanya memeriksa debet dan kredit).</li>
	 *       <li>Memanggil populateAkun untuk menyusun daftar debet-kredit lengkap.</li>
	 *       <li>Memanggil CommonAkunting.saveTransaksi untuk menyimpan jurnal.</li>
	 *       <li>Menautkan PostingHistory ke Pertangungjawaban dan menyimpan.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah semua selesai, menutup native session dan menampilkan pesan sukses.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Thread background menggunakan currentNativeSession().
	 * Semua akses ke entitas Hibernate dilakukan melalui session ini. Session HARUS
	 * ditutup di akhir run() via HibernateUtil.closeSession().</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per entitas ditangkap di blok catch
	 * luar (silent — tidak ditampilkan). Exception di saveTransaksi ditangkap dan
	 * ditampilkan jika admin.</p>
	 *
	 * @param event Event ZK dari tombol "Posting Semua".
	 * @throws Exception Jika terjadi kesalahan membangun komponen UI dialog.
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pertanggungjawaban ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
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
											MyMessageboxConfig.show(
													"Posting transaksi pertanggungjawaban berhasil dilakukan",
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
													PostingHistory.JENIS_PERTANGGUNGJAWABAN_UANG_MUKA);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<Pertangungjawaban> pertangungjawabans = initCriteria(true)
													.add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (Pertangungjawaban pertangungjawaban : pertangungjawabans) {

												SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawaban
														.getSatuanKerja() != null ? pertangungjawaban.getSatuanKerja()
																: null);

												if (pertangungjawaban != null) {

													try {
														Akun akunDebet = pertangungjawaban.getUangMuka().getAkun();
														Akun akunKredit = pertangungjawaban.getUangMuka()
																.getJenisUangMuka() == null ? null
																		: pertangungjawaban.getUangMuka()
																				.getJenisUangMuka().getAkun();

														Akun akunKelebihan = pertangungjawaban.getUangMuka()
																.getJenisUangMuka() == null ? null
																		: pertangungjawaban.getUangMuka()
																				.getJenisUangMuka().getAkunKelebihan();

														List<Akun> akunPajak = new ArrayList<Akun>();
														List<Double> nilaiPajak = new ArrayList<Double>();

														Double nilai = 0.0;
														JSONArray array = new JSONArray(pertangungjawaban.getFormula());
														for (int i = 0; i < array.length(); i++) {
															JSONObject jsonObject = array.getJSONObject(i);
															Double jumlah = 0.0;
															if (!jsonObject.isNull("jumlah")) {
																jumlah = jsonObject.getDouble("jumlah");
															}

															Double ppn = 0.0;
															if (!jsonObject.isNull("ppn")) {
																ppn = jsonObject.getDouble("ppn");
															}

															JenisPajakBarang barang;
															if (!jsonObject.isNull("pajak")) {
																barang = (JenisPajakBarang) ConstantValues.ambil(
																		JenisPajakBarang.class.getName(),
																		Long.parseLong(jsonObject.get("pajak") + ""));
															} else {
																barang = null;
															}

															Double pajak = barang == null ? 0.0
																	: ((barang.getPersen() / 100.0) * jumlah);

															Double tot = (jumlah + ((ppn / 100.0) * jumlah))
																	- (pph_mengurangi_lpj ? pajak : 0.0);

															nilai += tot;

															if (barang != null && barang.getId() != null
																	&& barang.getAkun() != null) {
																akunPajak.add(barang.getAkun());
																nilaiPajak.add(pajak);
															}
														}
														pertangungjawaban.setNilai(nilai);

														if (akunDebet != null && akunKredit != null
																&& akunKelebihan != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Laporan pertanggungjawaban penggunaan anggaran \""
																		+ pertangungjawaban
																				.getUangMuka().getWorkspace().getKode()
																		+ " "
																		+ pertangungjawaban.getUangMuka().getWorkspace()
																				.getNama()
																		+ "\" senilai " + Common.numberFormat.get()
																				.format(pertangungjawaban.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(
																	ket + " ("
																			+ Common.numberFormat.get().format(rowIndex
																					* 100.0 / pertangungjawabans.size())
																			+ " %)");

															nilai = pertangungjawaban.getNilai();
															Double nilaiPengajuan = pertangungjawaban.getUangMuka()
																	.getNilai();
															Double selisih = nilaiPengajuan - nilai;

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																Akun akunSponsor = pertangungjawaban.getUangMuka()
																		.getJenisUangMuka() == null
																				? null
																				: pertangungjawaban.getUangMuka()
																						.getJenisUangMuka()
																						.getAkunSponsor();
																Double sponsor = pertangungjawaban.getDariSponsor();

																List<Akun> akunsDebets = new ArrayList<Akun>();
																List<Akun> akunsKredits = new ArrayList<Akun>();

																List<Double> nilaiDebets = new ArrayList<Double>();
																List<Double> nilaiKredits = new ArrayList<Double>();

																populateAkun(akunsDebets, nilaiDebets, akunsKredits,
																		nilaiKredits, akunSponsor, selisih, akunDebet,
																		akunKredit, sponsor, nilai, akunKelebihan,
																		nilaiPengajuan, akunPajak, nilaiPajak);

																session.getTransaction().begin();

																CommonAkunting.saveTransaksi(
																		akunsDebets.toArray(new Akun[] {}),
																		akunsKredits.toArray(new Akun[] {}), akunDenda,
																		akunPiutangDenda, postingHistory,
																		apakahUangMasuk, ket,
																		pertangungjawaban.getTanggalPersetujuan(),
																		nilaiDebets.toArray(new Double[] {}),
																		nilaiKredits.toArray(new Double[] {}), denda,
																		pertangungjawaban, satuanKerja, session);
																session.getTransaction().commit();
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pertangungjawaban.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pertangungjawaban);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingPertangungjawabanAction.java:701");
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
	 * Flag konfigurasi apakah PPh mengurangi nilai LPJ (Laporan Pertanggungjawaban).
	 * Dibaca sekali dari tabel konfigurasi saat kelas diinisialisasi. Pengaruh: jika true,
	 * nilai PPh dikurangi dari total pengeluaran yang dipertanggungjawabkan, sehingga
	 * nilai LPJ lebih kecil dari jumlah nominal bruto pengeluaran.
	 */
	private boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");

	/**
	 * Renderer baris grid untuk menampilkan satu entri Pertangungjawaban beserta nilai
	 * pengajuan, nilai aktual, selisih, sponsor, pratinjau jurnal, status posting, dan
	 * tombol aksi per baris.
	 *
	 * <p><strong>Tujuan:</strong> Merender setiap baris grid dengan informasi lengkap
	 * satu Pertangungjawaban: kode, nama, link proses transfer, nilai pengajuan (uang muka
	 * awal), nilai aktual (dari formula pengeluaran), selisih (sisa yang dikembalikan),
	 * nilai sponsor, tanggal persetujuan, pratinjau jurnal debet-kredit, status posting,
	 * dan tombol aksi posting/batalkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Mengurai formula JSON dari {@code pertangungjawaban.getFormula()} untuk menghitung
	 *       nilai total aktual dan daftar akun pajak. Perhitungan: jumlah + PPN% - PPh%
	 *       (jika pph_mengurangi_lpj aktif).</li>
	 *   <li>Menampilkan nilai pengajuan, nilai aktual, selisih (pengajuan - aktual),
	 *       nilai sponsor, dan tanggal persetujuan.</li>
	 *   <li>Menentukan akun debet (UangMuka.akun), kredit (JenisUangMuka.akun),
	 *       kelebihan (JenisUangMuka.akunKelebihan), dan sponsor (JenisUangMuka.akunSponsor).</li>
	 *   <li>Memanggil populateAkun untuk menyusun daftar akun debet-kredit lengkap
	 *       berdasarkan kombinasi sponsor dan selisih.</li>
	 *   <li>Menampilkan pratinjau jurnal atau pesan error jika akun tidak lengkap.</li>
	 *   <li>Menampilkan status posting dan nomor bukti GrupTransaksi.</li>
	 *   <li>Tombol "Batalkan Posting": hanya untuk admin/edit jika sudah diposting.</li>
	 *   <li>Tombol "Posting Data" per baris: menggunakan currentSession() dan populateAkun
	 *       yang sama untuk konsistensi. Syarat posting: akun debet, kredit, DAN kelebihan
	 *       semua tidak null.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Jika akun debet atau kredit null, tampilkan
	 * pesan "Transaksi tidak valid". Formula JSON yang cacat akan menyebabkan exception
	 * yang diteruskan ke ZK framework.</p>
	 */
	class PertangungjawabanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi komponen UI untuk satu baris grid dari objek Pertangungjawaban.
		 * Dipanggil oleh ZK framework untuk setiap item dalam model data grid.
		 *
		 * <p><strong>Tujuan:</strong> Merender satu baris tabel pertanggungjawaban dengan
		 * semua informasi relevan untuk review dan aksi posting/pembatalan.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Mengurai formula, menghitung nilai, menentukan
		 * akun via populateAkun, dan menyusun komponen ZK sebagai anak Row.
		 * Lihat dokumentasi kelas PertangungjawabanRenderer untuk penjelasan lengkap.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception diteruskan ke ZK framework.
		 * Akun tidak lengkap ditampilkan sebagai pesan error informatif di baris grid.</p>
		 *
		 * @param arg0 Baris ZK (Row) wadah komponen kolom.
		 * @param arg1 Objek data, diharapkan bertipe Pertangungjawaban.
		 * @throws Exception Jika terjadi kesalahan parsing JSON, akses database, atau UI.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final Pertangungjawaban pertangungjawaban = (Pertangungjawaban) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(Pertangungjawaban.class, pertangungjawaban,
					pertangungjawaban.getKode() == null ? "" : pertangungjawaban.getKode())).setParent(arg0);

			if (pertangungjawaban != null && pertangungjawaban.getDaftarPengajuanTransfer() != null
					&& pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pertangungjawaban.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);

			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawaban.getNama()).setParent(a);
			if (pertangungjawaban.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawaban.getDisposisiSop().getKeterangan()
						+ " (" + pertangungjawaban.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawaban.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			final List<Akun> akunPajak = new ArrayList<Akun>();
			final List<Double> nilaiPajak = new ArrayList<Double>();

			Double nilai = 0.0;
			JSONArray array = new JSONArray(pertangungjawaban.getFormula());
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}

				JenisPajakBarang barang;
				if (!jsonObject.isNull("pajak")) {
					barang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					barang = null;
				}

				Double pajak = barang == null ? 0.0 : ((barang.getPersen() / 100.0) * jumlah);

				Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak : 0.0);

				nilai += tot;

				if (barang != null && barang.getId() != null && barang.getAkun() != null) {
					akunPajak.add(barang.getAkun());
					nilaiPajak.add(pajak);
				}
			}
			pertangungjawaban.setNilai(nilai);

			Double sponsor = pertangungjawaban.getDariSponsor();
			Double nilaiPengajuan = pertangungjawaban.getUangMuka().getNilai();
			Double selisih = nilaiPengajuan - nilai;

			new Label(Common.numberFormat.get().format(nilaiPengajuan)).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.numberFormat.get().format(selisih)).setParent(arg0);
			new Label(Common.numberFormat.get().format(sponsor)).setParent(arg0);
			new Label(Common.dateFormat3.get().format(pertangungjawaban.getTanggalPersetujuan())).setParent(arg0);

			Akun akunDebet = pertangungjawaban.getUangMuka().getAkun();
			Akun akunKredit = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
					: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkun();

			Akun akunKelebihan = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
					: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunKelebihan();

			Akun akunSponsor = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
					: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunSponsor();

			if (akunDebet != null && akunKredit != null) {
				List<Akun> akunsDebets = new ArrayList<Akun>();
				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				populateAkun(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, akunSponsor, selisih, akunDebet,
						akunKredit, sponsor, nilai, akunKelebihan, nilaiPengajuan, akunPajak, nilaiPajak);

				GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);
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
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pertangungjawaban", pertangungjawaban)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(pertangungjawaban.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pertangungjawaban.getPostingHistory().toString() + ", no. bukti : " + bukti).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (akunDebet != null && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pertangungjawaban.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pertangungjawaban.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pertangungjawaban);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where ref is null and pertangungjawaban="
												+ pertangungjawaban.getId() + " and closing is null")
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
				button.setVisible(edit && pertangungjawaban.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERTANGGUNGJAWABAN_UANG_MUKA);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunDebet = pertangungjawaban.getUangMuka().getAkun();
								Akun akunKredit = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
										: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkun();

								Akun akunKelebihan = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
										: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunKelebihan();

								if (akunDebet != null && akunKredit != null && akunKelebihan != null) {
									Boolean apakahUangMasuk = true;

									Double nilai = pertangungjawaban.getNilai();
									Double nilaiPengajuan = pertangungjawaban.getUangMuka().getNilai();
									Double selisih = nilaiPengajuan - nilai;

									String ket = "";
									try {

										ket = "Laporan pertanggungjawaban penggunaan anggaran \""
												+ pertangungjawaban.getUangMuka().getWorkspace().getKode() + " "
												+ pertangungjawaban.getUangMuka().getWorkspace().getNama()
												+ "\" senilai "
												+ Common.numberFormat.get().format(pertangungjawaban.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawaban.getSatuanKerja() != null
											? pertangungjawaban.getSatuanKerja()
											: tbmuser.ambilSatuanKerja());

									Akun akunSponsor = pertangungjawaban.getUangMuka().getJenisUangMuka() == null ? null
											: pertangungjawaban.getUangMuka().getJenisUangMuka().getAkunSponsor();
									Double sponsor = pertangungjawaban.getDariSponsor();

									List<Akun> akunsDebets = new ArrayList<Akun>();
									List<Akun> akunsKredits = new ArrayList<Akun>();

									List<Double> nilaiDebets = new ArrayList<Double>();
									List<Double> nilaiKredits = new ArrayList<Double>();

									populateAkun(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, akunSponsor,
											selisih, akunDebet, akunKredit, sponsor, nilai, akunKelebihan,
											nilaiPengajuan, akunPajak, nilaiPajak);

									CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
											akunsKredits.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
											postingHistory, apakahUangMasuk, ket,
											pertangungjawaban.getTanggalPersetujuan(),
											nilaiDebets.toArray(new Double[] {}), nilaiKredits.toArray(new Double[] {}),
											denda, pertangungjawaban, satuanKerja, session);

									pertangungjawaban.setPostingHistory(postingHistory);
									session.update(pertangungjawaban);
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
	 * Menyusun daftar akun debet dan kredit untuk jurnal pertanggungjawaban uang muka,
	 * mempertimbangkan pajak, selisih pengembalian, dan dana dari sponsor.
	 *
	 * <p><strong>Tujuan:</strong> Memusatkan logika pembentukan pasangan akun debet-kredit
	 * yang kompleks untuk jurnal pertanggungjawaban uang muka, digunakan konsisten baik di
	 * renderer maupun di thread posting massal untuk menghindari duplikasi kode.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menangani 4 kombinasi kasus berdasarkan ada/tidaknya
	 * sponsor dan selisih pengembalian:</p>
	 * <ol>
	 *   <li><strong>Ada pajak:</strong> Tambahkan setiap akun pajak ke kredit dengan nilai
	 *       pajaknya masing-masing, dan akumulasikan ke totalPajak. Pajak selalu dikreditkan
	 *       karena merupakan kewajiban yang akan dibayarkan ke kas negara.</li>
	 *   <li><strong>Ada sponsor DAN tidak ada selisih (selisih = 0):</strong>
	 *       Debet: akunDebet (nilai aktual + sponsor).
	 *       Kredit: akunKredit (nilai aktual - totalPajak) + akunSponsor (nilai sponsor).</li>
	 *   <li><strong>Ada sponsor DAN ada selisih (selisih > 0):</strong>
	 *       Debet: akunDebet (nilai aktual + sponsor) + akunKelebihan (selisih).
	 *       Kredit: akunKredit (nilai aktual - totalPajak) + akunSponsor (nilai sponsor).</li>
	 *   <li><strong>Tidak ada sponsor DAN tidak ada selisih:</strong>
	 *       Debet: akunDebet (nilai aktual).
	 *       Kredit: akunKredit (nilai aktual - totalPajak).</li>
	 *   <li><strong>Tidak ada sponsor DAN ada selisih:</strong>
	 *       Debet: akunDebet (nilai aktual) + akunKelebihan (selisih).
	 *       Kredit: akunKredit (nilaiPengajuan - totalPajak).</li>
	 * </ol>
	 *
	 * <p><strong>Catatan akuntansi:</strong> Nilai kredit utama (akunKredit) dikurangi
	 * totalPajak karena pajak dikreditkan ke akun pajak tersendiri. Total debet harus
	 * selalu sama dengan total kredit (prinsip keseimbangan jurnal). Selisih positif
	 * berarti uang sisa yang harus dikembalikan ke kas; selisih negatif (pengeluaran
	 * melebihi uang muka) tidak diharapkan terjadi dalam alur normal.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Tidak ada penanganan exception eksplisit.
	 * Jika akunSponsor null tetapi sponsor.intValue() != 0, akunSponsor null akan
	 * menyebabkan NullPointerException saat CommonAkunting.saveTransaksi memproses array.
	 * Pastikan konfigurasi JenisUangMuka lengkap sebelum posting.</p>
	 *
	 * @param akunsDebets    List akun debet yang akan diisi oleh metode ini.
	 * @param nilaiDebets    List nilai debet yang sesuai posisi dengan akunsDebets.
	 * @param akunsKredits   List akun kredit yang akan diisi oleh metode ini.
	 * @param nilaiKredits   List nilai kredit yang sesuai posisi dengan akunsKredits.
	 * @param akunSponsor    Akun sponsor (JenisUangMuka.akunSponsor), dapat null.
	 * @param selisih        Selisih = nilaiPengajuan - nilaiAktual. Positif = ada pengembalian.
	 * @param akunDebet      Akun uang muka debet (UangMuka.akun).
	 * @param akunKredit     Akun penerima utama kredit (JenisUangMuka.akun).
	 * @param sponsor        Nilai dana dari sponsor. 0 jika tidak ada sponsor.
	 * @param nilai          Nilai aktual pengeluaran yang dipertanggungjawabkan.
	 * @param akunKelebihan  Akun kas/bank tempat sisa uang dikembalikan (JenisUangMuka.akunKelebihan).
	 * @param nilaiPengajuan Nilai uang muka awal yang diberikan.
	 * @param akunPajak      Daftar akun pajak PPh yang harus dikreditkan.
	 * @param nilaiPajak     Daftar nilai pajak yang sesuai posisi dengan akunPajak.
	 */
	private void populateAkun(List<Akun> akunsDebets, List<Double> nilaiDebets, List<Akun> akunsKredits,
			List<Double> nilaiKredits, Akun akunSponsor, Double selisih, Akun akunDebet, Akun akunKredit,
			Double sponsor, Double nilai, Akun akunKelebihan, Double nilaiPengajuan, List<Akun> akunPajak,
			List<Double> nilaiPajak) {

		Double totalPajak = 0.0;
		if (!akunPajak.isEmpty()) {
			for (int i = 0; i < akunPajak.size(); i++) {
				Akun akun = akunPajak.get(i);
				Double pajak = nilaiPajak.get(i);

				akunsKredits.add(akun);
				nilaiKredits.add(pajak);
				totalPajak += pajak;
			}
		}

		if (akunSponsor != null && sponsor.intValue() != 0) {

			if (selisih.intValue() == 0) {

				akunsDebets.add(akunDebet);
				akunsKredits.add(akunKredit);
				akunsKredits.add(akunSponsor);

				nilaiDebets.add(nilai + sponsor);
				nilaiKredits.add(nilai - totalPajak);
				nilaiKredits.add(sponsor);

			} else {

				akunsDebets.add(akunDebet);
				akunsDebets.add(akunKelebihan);

				akunsKredits.add(akunKredit);
				akunsKredits.add(akunSponsor);

				nilaiDebets.add(nilai + sponsor);
				nilaiDebets.add(selisih);
				nilaiKredits.add(nilai - totalPajak);
				nilaiKredits.add(sponsor);

			}

		} else {

			if (selisih.intValue() == 0) {
				akunsDebets.add(akunDebet);
				akunsKredits.add(akunKredit);

				nilaiDebets.add(nilai);
				nilaiKredits.add(nilai - totalPajak);
			} else {
				akunsDebets.add(akunDebet);
				akunsDebets.add(akunKelebihan);

				akunsKredits.add(akunKredit);

				nilaiDebets.add(nilai);
				nilaiDebets.add(selisih);

				nilaiKredits.add(nilaiPengajuan - totalPajak);
			}
		}

	}

	/**
	 * Membuat dan mengembalikan objek Criteria Hibernate untuk query Pertangungjawaban
	 * sesuai semua filter yang aktif, mendukung mode normal dan mode sudah_posting dari dasbor.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan definisi filter terpusat untuk count paging
	 * dan pengambilan data halaman aktif. Mendukung dua mode filter berbeda.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Guard: jika searchparent null, return null.</li>
	 *   <li>Kumpulkan satuan kerja dan child-nya untuk filter hierarki.</li>
	 *   <li>Mode sudah_posting tidak null (dari dasbor jurnal):
	 *     <ul>
	 *       <li>Filter nilai != 0, disetujuiOleh not null, rentang tanggal persetujuan.</li>
	 *       <li>Join postingHistory dan filter posting=true (jika sudah_posting=true)
	 *           atau posting=false atau null (jika sudah_posting=false).</li>
	 *     </ul>
	 *   </li>
	 *   <li>Mode normal:
	 *     <ul>
	 *       <li>Checkbox searchtampil/searchtelahtampil saling mengecualikan; keduanya
	 *           menghasilkan filter posting=false atau null (lihat catatan pemeliharaan).</li>
	 *       <li>Filter satuan kerja hierarki dengan OR(isNull, in).</li>
	 *       <li>Join ke uangMuka (inner join, alias).</li>
	 *       <li>Filter disetujuiOleh not null (hanya yang sudah disetujui).</li>
	 *       <li>Filter nilai != 0 dan not null.</li>
	 *       <li>Filter tanggal persetujuan dengan kondisi tambahan
	 *           {@code OR tanggal_persetujuan IS NULL} untuk menampilkan data lama
	 *           tanpa tanggal persetujuan.</li>
	 *       <li>Filter teks: kode, nama, atau kode uang muka.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika order=true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Filter tanggal di mode normal menggunakan kondisi
	 * {@code this_.tanggal_persetujuan is null OR date(...) between ...} — ini berbeda
	 * dari kelas lain yang tidak mengizinkan null. Perhatikan ini jika ada perubahan
	 * kebijakan terkait data lama tanpa tanggal persetujuan.</p>
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

		Criteria criteria = session.createCriteria(Pertangungjawaban.class);

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

					.createAlias("uangMuka", "uangMuka")

					.add(Restrictions.isNotNull("disetujuiOleh"))

					.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

					.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction(
							"this_.tanggal_persetujuan is null or date(this_.tanggal_persetujuan) between date('"
									+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
									+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

					.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
							: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
											Restrictions.ilike("uangMuka.kode", searchkode.getValue(),
													MatchMode.ANYWHERE))));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat dan menampilkan data Pertangungjawaban ke grid tanpa progress bar.
	 * Hanya dipanggil dari {@code loadDataDenganProgressPosting} setelah progress bar aktif.
	 *
	 * <p><strong>Tujuan:</strong> Mengeksekusi query dan mengisi grid halaman aktif
	 * dengan renderer PertangungjawabanRenderer.</p>
	 *
	 * <p><strong>Penanganan error:</strong> Jika initCriteria mengembalikan null (searchparent
	 * null), pemanggilan setMaxResults akan NullPointerException. Guard searchparent null
	 * ada di doAfterCompose.</p>
	 *
	 * @param event Event ZK yang memicu reload, dapat null.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pertangungjawaban> pertangungjawaban = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pertangungjawaban);
		grid.setRowRenderer(new PertangungjawabanRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Event handler publik untuk memuat ulang data grid dengan progress bar.
	 * Dipanggil oleh ZK framework ketika event onSearchDefault diterima.
	 *
	 * <p><strong>Tujuan:</strong> Titik masuk publik untuk refresh data grid pertanggungjawaban.</p>
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
	 * Memuat data posting jurnal pertanggungjawaban ke grid dengan progress bar.
	 * Mencegah pemuatan ganda dan menangani permintaan reload yang masuk saat loading aktif.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan UX yang lebih baik dengan indikator progress
	 * saat memuat data yang mungkin banyak, dan mencegah race condition akibat
	 * interaksi pengguna yang cepat (klik paging berkali-kali).</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Jika sudah ada loading aktif: set flag reload tertunda, tampilkan notifikasi,
	 *       dan return tanpa memulai loading baru.</li>
	 *   <li>Set flag loading aktif dan tampilkan progress bar awal (7%).</li>
	 *   <li>Jalankan loading via createDefaultTimer (agar UI dapat render progress bar
	 *       terlebih dahulu):
	 *     <ul>
	 *       <li>Update progress ke 48% — ambil data dari database.</li>
	 *       <li>Panggil onSearchDefaultTanpaProgress untuk query dan render grid.</li>
	 *       <li>Update progress ke 92% — merapikan tampilan selesai.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Di blok finally: reset semua flag. Jika ada reload tertunda, mulai loading
	 *       baru di timer berikutnya. Jika tidak, tandai progress selesai (100%).</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Seluruh metode berjalan di thread ZK event.
	 * Timer ZK digunakan untuk memisahkan render progress bar dari query database,
	 * bukan untuk concurrency antar thread.</p>
	 *
	 * @param event Event ZK yang memicu loading. Diteruskan ke onSearchDefaultTanpaProgress.
	 *              Dapat null jika dipanggil dari timer otomatis atau inisialisasi.
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
