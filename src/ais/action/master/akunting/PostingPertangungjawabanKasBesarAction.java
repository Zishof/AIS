package ais.action.master.akunting;

import java.math.BigDecimal;
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
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>PostingPertangungjawabanKasBesarAction — Controller Posting Jurnal Pertanggungjawaban Kas Besar</h3>
 *
 * <p><strong>Untuk apa:</strong><br>
 * Kelas ini merupakan controller ZK (GenericAutowireComposer) yang mengelola halaman posting
 * jurnal akuntansi untuk transaksi pertanggungjawaban kas besar (petty cash accountability).
 * Pertanggungjawaban kas besar adalah proses dimana pemegang kas besar melaporkan penggunaan
 * dana kas yang telah diterimanya, disertai bukti pengeluaran, agar dapat dicatat sebagai beban
 * di buku besar akuntansi. Kelas ini memproses entitas {@code PertangungjawabanKasBesar} yang
 * sudah disetujui dan mengubahnya menjadi entri jurnal GrupTransaksi di sistem akuntansi.</p>
 *
 * <p><strong>Cara kerja:</strong><br>
 * Setelah halaman dimuat, controller menyiapkan filter tanggal, hak akses, dan model tree
 * satuan kerja untuk filter hierarki organisasi. Data dimuat menggunakan
 * {@code loadDataDenganProgressPosting}. Setiap baris PertangungjawabanKasBesar ditampilkan
 * oleh inner class {@code PertangungjawabanKasBesarRenderer} yang mengurai formula JSON
 * (rincian pengeluaran dengan pajak PPN dan PPh) untuk menghitung nilai total dan menentukan
 * akun-akun yang terlibat. Nilai total dihitung dari setiap item formula: jumlah + PPN -
 * PPh (jika konfigurasi {@code pph_mengurangi_lpj} aktif). Jurnal posting menggunakan
 * akun Workspace sebagai debet dan akun penerima KasBesar (JenisKasBesar.akunPenerima)
 * sebagai kredit utama, ditambah akun pajak di sisi kredit. Jika ada selisih (uang dikembalikan),
 * akun kelebihan dari CaraPembayaranTransfer ditambahkan ke sisi debet.</p>
 *
 * <p><strong>Threading:</strong><br>
 * Proses posting massal (onPostingSemua) dijalankan dalam thread latar belakang menggunakan
 * {@code HibernateUtil.currentNativeSession()}. Thread ini harus mengelola transaksi secara
 * eksplisit dan menutup session di finally. Posting per baris menggunakan currentSession()
 * dan berjalan di thread ZK event melalui createDefaultTimer. Konfigurasi
 * {@code pph_mengurangi_lpj} dibaca sekali saat inisialisasi kelas (field statis) menggunakan
 * {@code Common.getKonfigurasi}.</p>
 *
 * <p><strong>Pemeliharaan:</strong><br>
 * Formula pengeluaran kas besar disimpan sebagai JSON array di field {@code formula} entitas
 * PertangungjawabanKasBesar. Format setiap item: {@code {"jumlah": ..., "ppn": ..., "pajak": id}}.
 * Jika format formula berubah, logika parsing di renderer dan di thread posting massal harus
 * diperbarui bersama-sama. Konfigurasi {@code pph_mengurangi_lpj} menentukan apakah PPh
 * mengurangi nilai LPJ (Laporan Pertanggungjawaban); perubahan konfigurasi ini mempengaruhi
 * perhitungan nilai pada seluruh baris yang ditampilkan.</p>
 *
 * @see PertangungjawabanKasBesar
 * @see PostingHistory
 * @see GrupTransaksi
 * @see CommonAkunting
 */
public class PostingPertangungjawabanKasBesarAction extends GenericAutowireComposer {

	/**
	 * Serial version UID untuk serialisasi kelas ini sebagai Serializable ZK composer.
	 */
	private static final long serialVersionUID = -5779730217402400328L;

	/** Grid utama yang menampilkan daftar PertangungjawabanKasBesar dengan status postingnya. */
	private MyGrid grid;

	/** Komponen paging untuk navigasi halaman data pada grid. */
	private Paging paging;

	/** Kotak teks pencarian berdasarkan kode, nama, atau kode kas besar. */
	private Textbox searchkode;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang belum diposting.
	 */
	private MyCheckboxConfig searchtampil;

	/**
	 * Checkbox filter untuk menampilkan hanya data yang sudah diposting.
	 */
	private MyCheckboxConfig searchtelahtampil;

	/**
	 * Model tree satuan kerja untuk mengisi daftar pilihan filter hierarki organisasi.
	 * Digunakan bersama {@code searchparent} untuk memfilter berdasarkan satuan kerja.
	 */
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	/**
	 * Komponen banbox pemilihan satuan kerja sebagai filter. Perubahan satuan kerja
	 * akan memicu onSearchDefault melalui event listener yang dipasang di doAfterCompose.
	 */
	private AmbilDataSatuanKerjaBanbox searchparent;

	/**
	 * Flag apakah pengguna memiliki hak UPDATE. Menentukan visibilitas tombol aksi per baris.
	 */
	private boolean edit = false;

	/** Tombol "Posting Semua" di toolbar, hanya tampil jika pengguna memiliki hak UPDATE. */
	private MyToolbarbuttonConfig sent;

	/**
	 * Flag apakah pengguna adalah admin atau memiliki hak APPROVE.
	 * Hanya pengguna dengan flag ini yang dapat membatalkan posting.
	 */
	public boolean adminLain;

	/** Datebox tanggal mulai filter rentang tanggal persetujuan pertanggungjawaban. */
	private MyDatebox tglMulai;

	/** Datebox tanggal sampai filter rentang tanggal persetujuan pertanggungjawaban. */
	private MyDatebox tglSampai;

	/** Pengguna yang sedang login. */
	private Tbmuser tbmuser;

	/**
	 * Intercept lifecycle ZK sebelum halaman di-compose untuk memeriksa keamanan akses.
	 *
	 * <p><strong>Tujuan:</strong> Memastikan hanya pengguna terautentikasi yang dapat
	 * mengakses halaman ini sebelum komponen UI dibangun.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Memanggil {@code Common.doCheckSecurity()} yang
	 * mengarahkan ke login jika sesi tidak valid, lalu mendelegasikan ke superclass.</p>
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
	 *
	 * <p><strong>Tujuan:</strong> Menyiapkan semua komponen UI: filter tanggal, filter
	 * satuan kerja, hak akses, listener paging, dan memuat data awal ke grid.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menjalankan auto-wire ZK via superclass, kemudian menginisialisasi bahasa.</li>
	 *   <li>Memeriksa sesi dan hak READ; jika tidak valid, langsung goLogoff.</li>
	 *   <li>Memasang event listener pada {@code searchparent} banbox satuan kerja agar
	 *       pemilihan satuan kerja langsung memicu pencarian ulang.</li>
	 *   <li>Membuat model tree satuan kerja untuk keperluan filter hierarki.</li>
	 *   <li>Menetapkan rentang tanggal default: 6 bulan terakhir hingga hari ini.</li>
	 *   <li>Menentukan flag {@code adminLain} dan {@code edit} dari hak akses pengguna.</li>
	 *   <li>Memasang listener paging dan timer auto-refresh.</li>
	 *   <li>Memuat data awal melalui {@code loadDataDenganProgressPosting}.</li>
	 * </ol>
	 *
	 * <p><strong>Catatan:</strong> Berbeda dengan PostingProsesTransferAction, kelas ini
	 * mendukung filter satuan kerja hierarki melalui SatuanKerjaTreeModel. Juga tidak
	 * membaca sudahPostingDasbor dari PostingJurnalHelper.</p>
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
	 * Event handler untuk membatalkan posting semua pertanggungjawaban kas besar yang tampil.
	 * Menampilkan konfirmasi lalu menghapus PostingHistory dan GrupTransaksi terkait secara massal.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan pembatalan posting massal untuk semua
	 * PertangungjawabanKasBesar yang sudah diposting sesuai filter aktif saat ini.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Menampilkan dialog konfirmasi dengan pesan yang spesifik untuk kas besar.</li>
	 *   <li>Jika OK, mengambil semua PertangungjawabanKasBesar dengan postingHistory not null
	 *       menggunakan {@code initCriteria(true)}.</li>
	 *   <li>Untuk setiap entitas: set postingHistory = null dan simpan perubahan.</li>
	 *   <li>Menghapus GrupTransaksi terkait dengan SQL native. Filter {@code ref is null}
	 *       memastikan hanya jurnal posting utama yang dihapus (bukan jurnal pajak yang
	 *       memiliki ref berbeda).</li>
	 *   <li>Memuat ulang data melalui timer ZK.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Kolom FK {@code pertangungjawaban_kas_besar} pada
	 * tabel grup_transaksi — jika nama berubah, perbarui SQL di sini. Kondisi
	 * {@code ref is null} memfilter hanya jurnal posting utama, bukan jurnal pajak.</p>
	 *
	 * @param event Event ZK dari tombol "Batalkan Posting Semua" di toolbar.
	 * @throws Exception Jika terjadi kesalahan database atau update entitas.
	 */
	public void onBatalkanPostingSemua(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membatalkan posting transaksi pertangungjawaban kas besar ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							List<PertangungjawabanKasBesar> pertangungjawabanKasBesars = initCriteria(true)
									.add(Restrictions.isNotNull("postingHistory")).list();

							for (PertangungjawabanKasBesar pertangungjawabanKasBesar : pertangungjawabanKasBesars) {
								pertangungjawabanKasBesar.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pertangungjawabanKasBesar);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where ref is null and pertangungjawaban_kas_besar="
												+ pertangungjawabanKasBesar.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where ref is null and pertangungjawaban_kas_besar="
												+ pertangungjawabanKasBesar.getId() + " and closing is null")
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
	 * Event handler untuk memposting semua pertanggungjawaban kas besar secara massal.
	 * Menampilkan dialog form isian, lalu menjalankan proses posting di thread latar belakang.
	 *
	 * <p><strong>Tujuan:</strong> Memungkinkan posting massal seluruh PertangungjawabanKasBesar
	 * yang belum diposting dalam satu batch operasi, menghasilkan satu PostingHistory dan
	 * banyak GrupTransaksi sebagai entri buku besar.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Membuat jendela modal dengan form: tanggal posting, nama pengguna, keterangan.</li>
	 *   <li>Tombol Simpan memvalidasi tanggal, lalu meminta konfirmasi kedua.</li>
	 *   <li>Setelah konfirmasi, menampilkan progress bar dan memulai thread posting.</li>
	 *   <li>Thread posting (background):
	 *     <ul>
	 *       <li>Membuat PostingHistory dengan jenis JENIS_PERTANGGUNGJAWABAN_KAS_BESAR.</li>
	 *       <li>Mengambil semua PertangungjawabanKasBesar belum diposting.</li>
	 *       <li>Untuk setiap entitas: mengurai formula JSON untuk menghitung nilai total
	 *           dan daftar akun pajak.</li>
	 *       <li>Menentukan akun kredit dari JenisKasBesar.akunPenerima.</li>
	 *       <li>Memanggil {@code populateAkun} untuk menyusun daftar debet-kredit.</li>
	 *       <li>Memanggil {@code CommonAkunting.saveTransaksi} untuk menyimpan jurnal.</li>
	 *       <li>Menautkan PostingHistory ke entitas dan menyimpan.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah selesai, menutup native session dan menampilkan pesan sukses.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Proses posting berjalan dalam {@code new Thread} dengan
	 * {@code currentNativeSession()}. Session harus ditutup di akhir via HibernateUtil.closeSession().</p>
	 *
	 * <p><strong>Penanganan error:</strong> Exception per entitas ditangkap dan ditampilkan
	 * jika admin; proses berlanjut ke entitas berikutnya.</p>
	 *
	 * @param event Event ZK dari tombol "Posting Semua" di toolbar.
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

				MyMessageboxConfig.show("Apakah yakin ingin memposting semua transaksi pertangungjawaban kas besar ?",
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
													"Posting transaksi pertangungjawaban kas besar berhasil dilakukan",
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
													PostingHistory.JENIS_PERTANGGUNGJAWABAN_KAS_BESAR);
											postingHistory.setTanggal(tgl);
											postingHistory.setTbmuser(tbmuser);
											postingHistory.setKeterangan(keterangan.getValue().trim() + " \nTgl:"
													+ Common.dateFormat.get().format(tglMulai.getValue()) + " s.d "
													+ Common.dateFormat.get().format(tglSampai.getValue()));
											session.getTransaction().begin();
											session.save(postingHistory);
											session.getTransaction().commit();

											List<PertangungjawabanKasBesar> pertangungjawabanKasBesars = initCriteria(
													true).add(Restrictions.isNull("postingHistory")).list();

											int rowIndex = 1;
											for (PertangungjawabanKasBesar pertangungjawabanKasBesar : pertangungjawabanKasBesars) {

												SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawabanKasBesar
														.getSatuanKerja() != null
																? pertangungjawabanKasBesar.getSatuanKerja()
																: null);

												if (pertangungjawabanKasBesar != null) {

													try {

														Akun akunKredit = pertangungjawabanKasBesar
																.getKasBesar() == null ? null
																		: pertangungjawabanKasBesar.getKasBesar()
																				.getJenisKasBesar().getAkunPenerima();

														List<Akun> akunPajak = new ArrayList<Akun>();
														List<Double> nilaiPajak = new ArrayList<Double>();

														Double nilai = 0.0;
														JSONArray array = new JSONArray(
																pertangungjawabanKasBesar.getFormula());
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
														pertangungjawabanKasBesar.setNilai(nilai);

														if (akunKredit != null) {
															Boolean apakahUangMasuk = true;

															String ket = "";
															try {

																ket = "Laporan pertangungjawaban kas besar \""
																		+ pertangungjawabanKasBesar.getKasBesar()
																				.getKode()
																		+ " "
																		+ pertangungjawabanKasBesar.getKasBesar()
																				.getNama()
																		+ "\" senilai " + Common.numberFormat.get().format(
																				pertangungjawabanKasBesar.getNilai());

															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															label.setValue(ket + " ("
																	+ Common.numberFormat.get().format(rowIndex * 100.0
																			/ pertangungjawabanKasBesars.size())
																	+ " %)");

															nilai = pertangungjawabanKasBesar.getNilai();

															try {

																Akun akunDenda = null;
																Akun akunPiutangDenda = null;
																Double denda = 0.0;

																List<Akun> akunsDebets = new ArrayList<Akun>();
																List<Akun> akunsKredits = new ArrayList<Akun>();

																List<Double> nilaiDebets = new ArrayList<Double>();
																List<Double> nilaiKredits = new ArrayList<Double>();

																populateAkun(akunsDebets, nilaiDebets, akunsKredits,
																		nilaiKredits, akunKredit, akunPajak, nilaiPajak,
																		pertangungjawabanKasBesar);
																if (!nilaiDebets.isEmpty()) {
																	session.getTransaction().begin();

																	CommonAkunting.saveTransaksi(
																			akunsDebets.toArray(new Akun[] {}),
																			akunsKredits.toArray(new Akun[] {}),
																			akunDenda, akunPiutangDenda, postingHistory,
																			apakahUangMasuk, ket,
																			pertangungjawabanKasBesar
																					.getTanggalPersetujuan(),
																			nilaiDebets.toArray(new Double[] {}),
																			nilaiKredits.toArray(new Double[] {}),
																			denda, pertangungjawabanKasBesar,
																			satuanKerja, session);
																	session.getTransaction().commit();
																}
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}

															pertangungjawabanKasBesar.setPostingHistory(postingHistory);
															session.getTransaction().begin();
															session.update(pertangungjawabanKasBesar);
															session.getTransaction().commit();
														}
													} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/PostingPertangungjawabanKasBesarAction.java:636");
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
	 * Dibaca sekali dari tabel konfigurasi saat kelas diinisialisasi. Jika true, nilai PPh
	 * dikurangi dari total pengeluaran yang dilaporkan. Jika false, PPh tidak mengurangi nilai LPJ.
	 */
	private boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");

	/**
	 * Renderer baris grid untuk menampilkan satu entri PertangungjawabanKasBesar beserta
	 * status posting, pratinjau jurnal debet-kredit, dan tombol aksi per baris.
	 *
	 * <p><strong>Tujuan:</strong> Merender setiap baris grid dengan informasi lengkap
	 * satu PertangungjawabanKasBesar: kode, nama, link proses transfer terkait, nilai
	 * pengajuan, nilai aktual, selisih, tanggal, pratinjau jurnal, status posting, dan
	 * tombol aksi posting/batalkan.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Mengurai formula JSON dari {@code pertangungjawabanKasBesar.getFormula()} untuk
	 *       menghitung nilai total dan membangun daftar akun pajak beserta nilainya.
	 *       Setiap item formula dihitung: jumlah + PPN% - PPh% (jika pph_mengurangi_lpj).</li>
	 *   <li>Menampilkan nilai pengajuan (dari KasBesar), nilai aktual (dari formula),
	 *       dan selisih (uang yang harus dikembalikan).</li>
	 *   <li>Menentukan akun kredit dari JenisKasBesar.akunPenerima.</li>
	 *   <li>Memanggil {@code populateAkun} untuk menyusun daftar akun debet-kredit
	 *       dengan mempertimbangkan pajak dan selisih.</li>
	 *   <li>Menampilkan pratinjau jurnal menggunakan {@code GrupTransaksi.tampilkanJurnal}.</li>
	 *   <li>Menampilkan status posting dan nomor bukti GrupTransaksi.</li>
	 *   <li>Tombol "Batalkan Posting": hanya untuk admin/edit jika sudah diposting.</li>
	 *   <li>Tombol "Posting Data": hanya untuk edit jika belum diposting; menggunakan
	 *       currentSession() tanpa thread background.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Jika akun debet atau kredit kosong, tampilkan
	 * pesan error dan sembunyikan tombol aksi untuk mencegah posting data tidak valid.</p>
	 */
	class PertangungjawabanKasBesarRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Mengisi komponen UI untuk satu baris grid dari objek PertangungjawabanKasBesar.
		 * Dipanggil oleh ZK framework untuk setiap item dalam model data grid.
		 *
		 * <p><strong>Tujuan:</strong> Merender satu baris tabel pertanggungjawaban kas besar
		 * dengan semua informasi relevan untuk keperluan review dan aksi posting.</p>
		 *
		 * <p><strong>Cara kerja:</strong> Mengurai formula JSON, menghitung nilai, menentukan
		 * akun, dan menyusun komponen ZK sebagai anak dari Row. Lihat dokumentasi kelas
		 * {@code PertangungjawabanKasBesarRenderer} untuk penjelasan lengkap.</p>
		 *
		 * <p><strong>Penanganan error:</strong> Exception diteruskan ke ZK framework.
		 * Formula JSON yang cacat akan menyebabkan JSONException yang tidak ditangkap
		 * di level ini.</p>
		 *
		 * @param arg0 Baris ZK (Row) wadah komponen kolom.
		 * @param arg1 Objek data, diharapkan bertipe PertangungjawabanKasBesar.
		 * @throws Exception Jika terjadi kesalahan parsing JSON, akses database, atau UI.
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PertangungjawabanKasBesar pertangungjawabanKasBesar = (PertangungjawabanKasBesar) arg1;

			Vbox aaa;
			(aaa = RevisiHelper.createNewRevisi(PertangungjawabanKasBesar.class, pertangungjawabanKasBesar,
					pertangungjawabanKasBesar.getKode() == null ? "" : pertangungjawabanKasBesar.getKode()))
					.setParent(arg0);

			if (pertangungjawabanKasBesar != null && pertangungjawabanKasBesar.getDaftarPengajuanTransfer() != null
					&& pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A a = new A(pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, pertangungjawabanKasBesar.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				a.setStyle("font-size:12px;");
				a.setParent(aaa);

			}

			Vbox a = new Vbox();
			a.setParent(arg0);
			new Label(pertangungjawabanKasBesar.getNama()).setParent(a);
			if (pertangungjawabanKasBesar.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(a);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pertangungjawabanKasBesar.getDisposisiSop().getKeterangan()
						+ " (" + pertangungjawabanKasBesar.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pertangungjawabanKasBesar.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			final List<Akun> akunPajak = new ArrayList<Akun>();
			final List<Double> nilaiPajak = new ArrayList<Double>();

			Double nilai = 0.0;
			JSONArray array = new JSONArray(pertangungjawabanKasBesar.getFormula());
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
			pertangungjawabanKasBesar.setNilai(nilai);
			Double nilaiPengajuan = pertangungjawabanKasBesar.getKasBesar().getNilai();
			Double selisih = nilaiPengajuan - nilai;

			new Label(Common.numberFormat.get().format(nilaiPengajuan)).setParent(arg0);
			new Label(Common.numberFormat.get().format(nilai)).setParent(arg0);

			new Label(Common.numberFormat.get().format(selisih)).setParent(arg0);
			new Label(Common.dateFormat3.get().format(pertangungjawabanKasBesar.getTanggalPersetujuan())).setParent(arg0);

			Akun akunKredit = pertangungjawabanKasBesar.getKasBesar() == null ? null
					: pertangungjawabanKasBesar.getKasBesar().getJenisKasBesar().getAkunPenerima();

			List<Akun> akunsDebets = new ArrayList<Akun>();
			if (akunKredit != null) {

				List<Akun> akunsKredits = new ArrayList<Akun>();

				List<Double> nilaiDebets = new ArrayList<Double>();
				List<Double> nilaiKredits = new ArrayList<Double>();

				populateAkun(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, akunKredit, akunPajak, nilaiPajak,
						pertangungjawabanKasBesar);

				if (akunsDebets.isEmpty() || akunsKredits.isEmpty()) {
					new Label("Transaksi tidak valid."
							+ (!akunsDebets.isEmpty() ? " Debet: " + akunsDebets + "." : " Akun debet tidak ada.")
							+ (akunKredit != null
									? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
									: " Akun kredit tidak ada."))
							.setParent(arg0);
				} else {
					GrupTransaksi.tampilkanJurnal(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits).setParent(arg0);
				}
			} else {
				new Label("Transaksi tidak valid." + " Akun debet tidak ada."
						+ (akunKredit != null ? " Kredit: " + akunKredit.getKode() + "-" + akunKredit.getNama() + "."
								: " Akun kredit tidak ada."))
						.setParent(arg0);
			}

			String bukti = "";
			Session session = HibernateUtil.currentSession();
			bukti = (String) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.eq("pertangungjawabanKasBesar", pertangungjawabanKasBesar)).setMaxResults(1)
					.setProjection(Projections.property("kode")).uniqueResult();

			new Label(pertangungjawabanKasBesar.getPostingHistory() == null ? Common.getBahasaConfig("Belum diposting")
					: pertangungjawabanKasBesar.getPostingHistory().toString() + ", no. bukti : " + bukti)
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);

			if (!akunsDebets.isEmpty() && akunKredit != null) {
				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
				button.setTooltiptext("Batalkan Posting Data");
				button.setVisible(edit && adminLain && pertangungjawabanKasBesar.getPostingHistory() != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pertangungjawabanKasBesar.setPostingHistory(null);
								Common.refreshSaveOrUpdate(pertangungjawabanKasBesar);
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.transaksi where grup_transaksi in"
												+ " (select id from akunting.grup_transaksi where ref is null and pertangungjawaban_kas_besar="
												+ pertangungjawabanKasBesar.getId() + " and closing is null)")
										.executeUpdate();
								HibernateUtil.currentSession().createSQLQuery(
										"delete from akunting.grup_transaksi where ref is null and pertangungjawaban_kas_besar="
												+ pertangungjawabanKasBesar.getId() + " and closing is null")
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
				button.setVisible(edit && pertangungjawabanKasBesar.getPostingHistory() == null && tbmuser != null);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Session session = HibernateUtil.currentSession();

								PostingHistory postingHistory = new PostingHistory(
										PostingHistory.JENIS_PERTANGGUNGJAWABAN_KAS_BESAR);
								postingHistory.setTbmuser(Common.getCurrentUser());
								postingHistory.setTanggal(ais.ui.util.WaktuUtil.getDate());
								postingHistory.setKeterangan(
										"Posting manual oleh " + Common.getCurrentUser().getUserNama() + " pada waktu "
												+ Common.dateFormat5.get().format(ais.ui.util.WaktuUtil.getDate()));
								session.save(postingHistory);

								Akun akunKredit = pertangungjawabanKasBesar.getKasBesar() == null ? null
										: pertangungjawabanKasBesar.getKasBesar().getJenisKasBesar().getAkunPenerima();

								if (akunKredit != null) {
									Boolean apakahUangMasuk = true;

									String ket = "";
									try {

										ket = "Laporan pertangungjawaban kas besar penggunaan \""
												+ pertangungjawabanKasBesar.getKasBesar().getKode() + " "
												+ pertangungjawabanKasBesar.getKasBesar().getNama() + "\" senilai "
												+ Common.numberFormat.get().format(pertangungjawabanKasBesar.getNilai());

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
									}

									Akun akunDenda = null;
									Akun akunPiutangDenda = null;
									Double denda = 0.0;

									SatuanKerja satuanKerja = (SatuanKerja) (pertangungjawabanKasBesar
											.getSatuanKerja() != null ? pertangungjawabanKasBesar.getSatuanKerja()
													: tbmuser.ambilSatuanKerja());

									List<Akun> akunsDebets = new ArrayList<Akun>();
									List<Akun> akunsKredits = new ArrayList<Akun>();

									List<Double> nilaiDebets = new ArrayList<Double>();
									List<Double> nilaiKredits = new ArrayList<Double>();

									populateAkun(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, akunKredit,
											akunPajak, nilaiPajak, pertangungjawabanKasBesar);

									if (!nilaiDebets.isEmpty()) {
										CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
												akunsKredits.toArray(new Akun[] {}), akunDenda, akunPiutangDenda,
												postingHistory, apakahUangMasuk, ket,
												pertangungjawabanKasBesar.getTanggalPersetujuan(),
												nilaiDebets.toArray(new Double[] {}),
												nilaiKredits.toArray(new Double[] {}), denda, pertangungjawabanKasBesar,
												satuanKerja, session);

										pertangungjawabanKasBesar.setPostingHistory(postingHistory);
										session.update(pertangungjawabanKasBesar);
									}
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


	// ================================================================ mesin posting massal

	/**
	 * Penyaring dokumen yang layak diposting: sudah disetujui, nilainya tidak nol, dan
	 * berada dalam rentang tanggal persetujuan.
	 */
	private static Criteria kriteriaPostingStatic(Session session, java.util.Date mulai, java.util.Date sampai) {
		Criteria c = session.createCriteria(PertangungjawabanKasBesar.class)
				.add(Restrictions.isNotNull("disetujuiOleh"))
				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"));
		if (mulai != null && sampai != null) {
			c.add(Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
					+ Common.databaseDateFormat.get().format(mulai) + "') and date('"
					+ Common.databaseDateFormat.get().format(sampai) + "')"));
		}
		return c;
	}

	/**
	 * Batalkan posting SEMUA pertanggungjawaban kas besar terposting dalam rentang:
	 * hapus jurnal yang belum closing, lalu kosongkan penanda postingnya.
	 *
	 * @return jumlah dokumen yang berhasil dibatalkan postingnya.
	 */
	@SuppressWarnings("unchecked")
	public static int batalkanPostingSemua(java.util.Date mulai, java.util.Date sampai) {
		int n = 0;
		// Transaksi dibuka sendiri: dipanggil dari API tidak ada kerangka ZK yang
		// meng-commit-kan currentSession, sehingga pembatalan hanya akan tampak berhasil.
		Session session = HibernateUtil.currentNativeSession();
		try {
			List<PertangungjawabanKasBesar> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNotNull("postingHistory")).list();
			for (PertangungjawabanKasBesar pj : daftar) {
				try {
					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					// Saringan "ref is null" menyamakan mesin ini dengan kedua tombol batal di
					// layarnya sendiri: yang dibatalkan hanya kaki UTAMA. Entitas ini membawa cap
					// postingHistoryPajak dan postingHistoryPengembalian yang belum dipakai siapa
					// pun; begitu salah satunya diimplementasikan (seperti yang sudah terjadi pada
					// LPJ uang muka), penghapusan tanpa saringan akan ikut melenyapkan jurnalnya.
					session.createSQLQuery("delete from akunting.transaksi where grup_transaksi in"
							+ " (select id from akunting.grup_transaksi"
							+ "  where ref is null and pertangungjawaban_kas_besar=" + pj.getId()
							+ " and closing is null)")
							.executeUpdate();
					session.createSQLQuery("delete from akunting.grup_transaksi"
							+ " where ref is null and pertangungjawaban_kas_besar=" + pj.getId()
							+ " and closing is null")
							.executeUpdate();
					pj.setPostingHistory(null);
					session.update(pj);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					try {
						session.getTransaction().rollback();
					} catch (Exception ex) {
						// rollback gagal: kegagalan aslinya yang dilaporkan
					}
					ais.common.ErrorAuditUtil.record(e, "PostingPertangungjawabanKasBesarAction jalur API");
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
	 * Posting SEMUA pertanggungjawaban kas besar yang belum diposting dalam rentang.
	 * Penyusunan jurnalnya memakai {@link #populateAkun} yang sama dengan layar: debet
	 * dari anggaran pada rincian kas besar, kredit ke akun penerima jenis kas besarnya,
	 * pajak per baris dikreditkan, dan dana yang dikembalikan didebet ke akun kelebihan.
	 *
	 * @return jumlah dokumen yang berhasil diposting.
	 */
	@SuppressWarnings("unchecked")
	public static int postingSemua(java.util.Date mulai, java.util.Date sampai, Tbmuser oleh,
			java.util.Date tglPosting) {
		int n = 0;
		Session session = HibernateUtil.currentNativeSession();
		try {
			PostingHistory postingHistory = new PostingHistory(
					PostingHistory.JENIS_PERTANGGUNGJAWABAN_KAS_BESAR);
			postingHistory.setTanggal(tglPosting == null ? new java.util.Date() : tglPosting);
			postingHistory.setTbmuser(oleh);
			postingHistory.setKeterangan("Posting massal pertanggungjawaban kas besar dari dasbor jurnal"
					+ (mulai != null && sampai != null ? " \nTgl:" + Common.dateFormat.get().format(mulai)
							+ " s.d " + Common.dateFormat.get().format(sampai) : ""));
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(postingHistory);
			session.getTransaction().commit();

			List<PertangungjawabanKasBesar> daftar = kriteriaPostingStatic(session, mulai, sampai)
					.add(Restrictions.isNull("postingHistory")).list();

			for (PertangungjawabanKasBesar pj : daftar) {
				if (pj == null || pj.getKasBesar() == null || pj.getKasBesar().getJenisKasBesar() == null) {
					continue;
				}
				try {
					Akun akunKredit = pj.getKasBesar().getJenisKasBesar().getAkunPenerima();
					if (akunKredit == null) {
						continue;
					}

					List<Akun> akunPajak = new ArrayList<Akun>();
					List<Double> nilaiPajak = new ArrayList<Double>();
					JSONArray array = new JSONArray(pj.getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Double jumlah = jsonObject.isNull("jumlah") ? 0.0 : jsonObject.getDouble("jumlah");
						JenisPajakBarang barang = jsonObject.isNull("pajak") ? null
								: (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
										Long.parseLong(jsonObject.get("pajak") + ""));
						if (barang != null && barang.getId() != null && barang.getAkun() != null) {
							akunPajak.add(barang.getAkun());
							nilaiPajak.add(Double.valueOf((barang.getPersen() / 100.0) * jumlah));
						}
					}

					List<Akun> akunsDebets = new ArrayList<Akun>();
					List<Akun> akunsKredits = new ArrayList<Akun>();
					List<Double> nilaiDebets = new ArrayList<Double>();
					List<Double> nilaiKredits = new ArrayList<Double>();
					populateAkun(akunsDebets, nilaiDebets, akunsKredits, nilaiKredits, akunKredit, akunPajak,
							nilaiPajak, pj);
					if (nilaiDebets.isEmpty()) {
						// Sama seperti layar: tanpa akun debet, dokumen dilewati.
						continue;
					}

					String ket = "Laporan pertangungjawaban kas besar penggunaan \""
							+ pj.getKasBesar().getKode() + " " + pj.getKasBesar().getNama() + "\" senilai "
							+ Common.numberFormat.get().format(pj.getNilai());

					session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					CommonAkunting.saveTransaksi(akunsDebets.toArray(new Akun[] {}),
							akunsKredits.toArray(new Akun[] {}), null, null, postingHistory, true, ket,
							pj.getTanggalPersetujuan(), nilaiDebets.toArray(new Double[] {}),
							nilaiKredits.toArray(new Double[] {}), 0.0, pj, pj.getSatuanKerja(), session);
					pj.setPostingHistory(postingHistory);
					session.update(pj);
					session.getTransaction().commit();
					n++;
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e, "PostingPertangungjawabanKasBesarAction jalur API");
				}
			}
		} finally {
			try {
				session.disconnect();
				HibernateUtil.closeSession();
			} catch (Exception e) {
				// penutupan sesi manual: kegagalannya tidak menutupi hasil posting
			}
		}
		return n;
	}

	/**
	 * Menyusun daftar akun debet dan kredit untuk jurnal pertanggungjawaban kas besar,
	 * mempertimbangkan workspace (sumber dana), pajak, dan selisih pengembalian.
	 *
	 * <p><strong>Tujuan:</strong> Memusatkan logika penentuan akun-akun jurnal pertanggungjawaban
	 * kas besar agar dapat digunakan secara konsisten baik di renderer maupun di proses
	 * posting massal, menghindari duplikasi kode.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Mengurai formula JSON dari KasBesar (bukan PertangungjawabanKasBesar) untuk
	 *       mendapatkan Workspace yang terkait. Workspace menentukan akun debet utama.</li>
	 *   <li>Jika Workspace dan akunnya ada: tambahkan akun Workspace ke akunsDebets dengan
	 *       nilai total pertanggungjawaban (getNilai()).</li>
	 *   <li>Jika ada pajak dalam daftar akunPajak: tambahkan setiap akun pajak ke akunsKredits
	 *       dengan nilai pajak masing-masing, dan akumulasikan ke totalPajak.</li>
	 *   <li>Mengambil akun kelebihan dari rantai:
	 *       KasBesar → DaftarPengajuanTransfer → ProsesTransfer → CaraPembayaranTransfer → Akun.
	 *       Ini adalah akun kas/bank tempat sisa uang dikembalikan.</li>
	 *   <li>Menghitung selisih = getDikembalikan() (field di entitas).</li>
	 *   <li>Jika selisih = 0 (tidak ada pengembalian): kredit ke akunKredit (penerima)
	 *       sejumlah nilai dikurangi totalPajak.</li>
	 *   <li>Jika selisih > 0 (ada pengembalian): debet ke akunKelebihan sejumlah selisih,
	 *       dan kredit ke akunKredit sejumlah nilai pengajuan awal dikurangi totalPajak.</li>
	 * </ol>
	 *
	 * <p><strong>Penanganan error:</strong> Exception saat parsing JSON workspace ditangkap
	 * dan ditampilkan hanya jika admin. Jika Workspace null atau akunnya null, bagian
	 * akunsDebets tidak diisi, sehingga nilaiDebets akan kosong dan caller dapat mendeteksi
	 * kondisi ini dengan {@code nilaiDebets.isEmpty()}.</p>
	 *
	 * @param akunsDebets                 List akun debet yang akan diisi oleh metode ini.
	 * @param nilaiDebets                 List nilai debet yang sesuai dengan akunsDebets.
	 * @param akunsKredits               List akun kredit yang akan diisi oleh metode ini.
	 * @param nilaiKredits               List nilai kredit yang sesuai dengan akunsKredits.
	 * @param akunKredit                 Akun kredit utama (JenisKasBesar.akunPenerima).
	 * @param akunPajak                  Daftar akun pajak yang harus dikreditkan.
	 * @param nilaiPajak                 Daftar nilai pajak yang sesuai dengan akunPajak.
	 * @param pertangungjawabanKasBesar  Entitas pertanggungjawaban kas besar yang sedang diproses.
	 * @throws Exception Jika terjadi kesalahan akses database atau parsing JSON formula.
	 */
	private static void populateAkun(List<Akun> akunsDebets, List<Double> nilaiDebets, List<Akun> akunsKredits,
			List<Double> nilaiKredits, Akun akunKredit, List<Akun> akunPajak, List<Double> nilaiPajak,
			PertangungjawabanKasBesar pertangungjawabanKasBesar) throws Exception {

		Workspace workspace = null;
		JSONArray array = new JSONArray(pertangungjawabanKasBesar.getKasBesar().getFormula());
		for (int i = 0; i < array.length(); i++) {

			try {
				JSONObject jsonObject = array.getJSONObject(i);
				workspace = (Workspace) (jsonObject.isNull("workspace") ? null
						: ConstantValues.ambil(Workspace.class.getName(),
								new BigDecimal(jsonObject.get("workspace") + "").longValue()));
			} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PostingPertangungjawabanKasBesarAction jalur API");
		}
		}

		if (workspace != null && workspace.getAkun() != null) {
			akunsDebets.add(workspace.getAkun());
			nilaiDebets.add(pertangungjawabanKasBesar.getNilai());
		}

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

		// Rantai kas besar -> DPC -> proses transfer -> cara pembayaran dibaca satu per
		// satu. Sebelumnya daftarPengajuanTransfer langsung dideferensiasi, sehingga
		// dokumen yang belum masuk DPC melempar NullPointerException -- padahal akun ini
		// hanya dipakai ketika ada dana yang dikembalikan.
		Akun akunKelebihan = null;
		ais.database.model.akunting.KasBesar kasBesarInduk = pertangungjawabanKasBesar.getKasBesar();
		if (kasBesarInduk != null && kasBesarInduk.getDaftarPengajuanTransfer() != null
				&& kasBesarInduk.getDaftarPengajuanTransfer().getProsesTransfer() != null
				&& kasBesarInduk.getDaftarPengajuanTransfer().getProsesTransfer()
						.getCaraPembayaranTransfer() != null) {
			akunKelebihan = kasBesarInduk.getDaftarPengajuanTransfer().getProsesTransfer()
					.getCaraPembayaranTransfer().getAkun();
		}

		Double selisih = pertangungjawabanKasBesar.getDikembalikan();
		if (selisih == null) {
			selisih = 0.0;
		}

		if (selisih.intValue() == 0) {
			akunsKredits.add(akunKredit);
			nilaiKredits.add(pertangungjawabanKasBesar.getNilai() - totalPajak);
		} else if (akunKelebihan != null) {
			akunsDebets.add(akunKelebihan);
			akunsKredits.add(akunKredit);
			nilaiDebets.add(selisih);
			nilaiKredits.add(kasBesarInduk.getNilai() - totalPajak);
		}
		// Ada selisih tetapi akun kelebihannya belum diketahui (transfernya belum
		// diproses): jurnal sengaja dibiarkan kosong supaya pemanggil melewati dokumen
		// ini, bukan menulis jurnal yang tidak seimbang.

	}

	/**
	 * Membuat dan mengembalikan objek Criteria Hibernate untuk query PertangungjawabanKasBesar
	 * sesuai semua filter yang aktif di UI, termasuk filter satuan kerja hierarki.
	 *
	 * <p><strong>Tujuan:</strong> Menyediakan satu titik definisi filter data yang digunakan
	 * konsisten untuk count paging dan pengambilan data halaman aktif.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Mengambil satuan kerja yang dipilih di searchparent banbox, lalu mengambil
	 *       seluruh child-nya menggunakan SatuanKerjaTreeModel.getChildsSet. Jika tidak
	 *       ada pilihan, menggunakan semua satuan kerja yang tersedia.</li>
	 *   <li>Membuat Criteria pada PertangungjawabanKasBesar dengan filter satuan kerja
	 *       menggunakan Restrictions.in atau null check sesuai kondisi.</li>
	 *   <li>Melakukan inner join ke kasBesar (alias).</li>
	 *   <li>Filter checkbox belum posting (searchtampil) dan sudah posting (searchtelahtampil).</li>
	 *   <li>Filter disetujuiOleh not null (hanya yang sudah disetujui).</li>
	 *   <li>Filter nilai tidak nol dan tidak null.</li>
	 *   <li>Filter tanggal persetujuan dalam rentang menggunakan SQL native PostgreSQL.</li>
	 *   <li>Filter pencarian teks: kode, nama, atau kode kas besar.</li>
	 *   <li>Jika order=true, tambahkan ORDER BY id DESC.</li>
	 * </ol>
	 *
	 * <p><strong>Pemeliharaan:</strong> Kolom {@code this_.tanggal_persetujuan} menggunakan
	 * nama tabel alias Hibernate. Jika mapping entitas berubah, nama kolom mungkin
	 * perlu disesuaikan. Filter satuan kerja menggunakan pola OR(isNull, in) untuk
	 * mendukung entitas yang tidak memiliki satuan kerja (null = semua satuan kerja).</p>
	 *
	 * @param order Jika true, menambahkan ORDER BY id DESC.
	 * @return Criteria siap dieksekusi untuk list atau count.
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

		Criteria criteria = session.createCriteria(PertangungjawabanKasBesar.class)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.createAlias("kasBesar", "kasBesar")

				.add(!searchtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNull("postingHistory"))

				.add(!searchtelahtampil.isChecked() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.isNotNull("postingHistory"))

				.add(Restrictions.isNotNull("disetujuiOleh"))

				.add(Restrictions.ne("nilai", 0.0)).add(Restrictions.isNotNull("nilai"))

				.add((tglMulai == null || tglSampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_persetujuan) between date('"
						+ Common.databaseDateFormat.get().format(tglMulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(tglSampai.getValue()) + "')")))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", searchkode.getValue(), MatchMode.ANYWHERE),
										Restrictions.ilike("kasBesar.kode", searchkode.getValue(),
												MatchMode.ANYWHERE))));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	/**
	 * Memuat dan menampilkan data PertangungjawabanKasBesar ke grid tanpa progress bar.
	 * Hanya dipanggil dari {@code loadDataDenganProgressPosting} setelah progress bar aktif.
	 *
	 * <p><strong>Tujuan:</strong> Mengeksekusi query database dan mengisi grid dengan data
	 * halaman aktif beserta renderer yang sesuai.</p>
	 *
	 * <p><strong>Cara kerja:</strong> Menghitung total untuk paging, mengambil data halaman
	 * aktif, dan menetapkan PertangungjawabanKasBesarRenderer ke grid.</p>
	 *
	 * @param event Event ZK yang memicu reload, dapat null.
	 */
	@SuppressWarnings("unchecked")
	private void onSearchDefaultTanpaProgress(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PertangungjawabanKasBesar> pertangungjawabanKasBesar = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pertangungjawabanKasBesar);
		grid.setRowRenderer(new PertangungjawabanKasBesarRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Event handler publik untuk memuat ulang data grid dengan progress bar.
	 * Dipanggil oleh ZK framework ketika event onSearchDefault diterima.
	 *
	 * <p><strong>Tujuan:</strong> Titik masuk publik untuk refresh data grid,
	 * mendelegasikan ke loadDataDenganProgressPosting.</p>
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
	 * Memuat data posting jurnal ke grid dengan menampilkan progress bar selama proses berlangsung.
	 * Mencegah pemuatan ganda dengan flag {@code postingJurnalLoadingAktif} dan menangani
	 * permintaan reload yang masuk saat loading aktif dengan flag {@code postingJurnalReloadTertunda}.
	 *
	 * <p><strong>Tujuan:</strong> Memberikan UX yang lebih baik dengan indikator progress
	 * dan mencegah race condition pada load bersamaan.</p>
	 *
	 * <p><strong>Cara kerja:</strong></p>
	 * <ol>
	 *   <li>Jika sudah ada loading aktif: set flag reload tertunda dan return.</li>
	 *   <li>Set flag loading aktif, tampilkan progress bar awal.</li>
	 *   <li>Jalankan loading via createDefaultTimer: update progress 48%, load data,
	 *       update progress 92%.</li>
	 *   <li>Di finally: reset flag. Jika ada reload tertunda, mulai loading baru.
	 *       Jika tidak, tandai progress selesai.</li>
	 * </ol>
	 *
	 * <p><strong>Threading:</strong> Seluruh metode berjalan di thread ZK event.
	 * Timer digunakan untuk memisahkan render UI dari query database.</p>
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
