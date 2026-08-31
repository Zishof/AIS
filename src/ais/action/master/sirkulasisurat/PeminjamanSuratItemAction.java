
package ais.action.master.sirkulasisurat;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.master.sirkulasisurat.helper.AmbilDataPeminjamSuratBanbox;
import ais.action.master.sirkulasisurat.helper.PeminjamanSuratItemDetailAction;
import ais.action.master.sirkulasisurat.helper.PeminjamanSuratItemPunyaItemHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sirkulasisurat.PeminjamSurat;
import ais.database.model.sirkulasisurat.PeminjamanSuratItem;
import ais.database.model.sirkulasisurat.PeminjamanSuratItemDetail;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.SuratMasuk;
import ais.delivery.email.sender.MailSender;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import net.sourceforge.barbecue.Barcode;
import net.sourceforge.barbecue.BarcodeFactory;
import net.sourceforge.barbecue.BarcodeImageHandler;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk peminjaman surat item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code MyGrid
 * grid}, {@code Paging paging}, {@code Textbox searchkode}, {@code Textbox searchkodesurat}, {@code Textbox
 * searchjudul}, {@code Textbox searchkodeangota}, {@code AmbilDataPeminjamSuratBanbox searchpeminjamSurat};
 * inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code onSearchDefault()}, {@code ambil()}, {@code ambilClass()});
 * mutasi data ({@code setujui()}, {@code onSave()}, {@code setPersetujuan()}); pelaporan/ekspor ({@code
 * cetakData()}); operasi domain lain ({@code parameter()}, {@code onAdd()}, {@code form()}, {@code kirim()},
 * {@code onKodePeminjamSurat()}, {@code istilah()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PeminjamanSuratItemAction extends GenericAutowireComposer implements FormSop {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchkodesurat;
	private Textbox searchjudul;
	private Textbox searchkodeangota;
	private AmbilDataPeminjamSuratBanbox searchpeminjamSurat;
	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private MyCheckboxConfig searchBelumDikembalikan;
	private AmbilDataSatuanKerjaBanbox searchparent;
	private MyTextbox kode;
	private AmbilDataPeminjamSuratBanbox peminjamSurat;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private MyDatebox mulai;
	private MyDatebox sampai;
	private MyTextbox tujuanPeminjaman;

	private boolean edit = false;
	private boolean delete = false;

	private PeminjamanSuratItem peminjamanSuratItem;
	private MyToolbarbuttonConfig add;

	private MyGrid gridItem;

	private String tipe = "surat";
	private DisposisiSop disposisiSop;
	private boolean persetujuan = false;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataSatuanKerjaBanbox kepadaSatuanKerja;
	private SatuanKerjaTreeModel satuanKerjaTreeModel;

	public PeminjamanSuratItemAction() {
		super();
	}

	public PeminjamanSuratItemAction(String tipe) {
		super();
		this.tipe = tipe;
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (execution.getParameter("tipe") != null && !execution.getParameter("tipe").trim().isEmpty()) {
			tipe = execution.getParameter("tipe").trim();
		}
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		searchpeminjamSurat.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		searchkodeangota.addEventListener("onFocus", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchkodeangota.select();
			}
		});

		if (searchmulai != null) { searchmulai.setReadonly(true); }
		if (searchsampai != null) { searchsampai.setReadonly(true); }

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) - 7);
		if (searchmulai != null) { searchmulai.setValue(calendar.getTime()); }
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (searchsampai != null) { searchsampai.setValue(calendar.getTime()); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}
//		add.setVisible(false);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		// approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		// reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Proses ulang keterlambatan", "/img/excel.png");
		Common.appendKeToolbar(upload, add, comp);
		upload.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {

				final Label label = new Label(ais.common.Common.getBahasaConfig("Proses ulang keterlambatan .."));
				Clients.showBusy(label.getValue());
				final Timer timer = new Timer(200);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.showBusy(label.getValue());
						if (label.getValue().isEmpty()) {
							MyMessageboxConfig.show("Proses ulang keterlambatan berhasil dilakukan.", "Pemberitahuan",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
										}
									});
							Clients.clearBusy();
							timer.detach();
						}

					}
				});
				timer.start();

				new Thread(new Runnable() {

					@Override
					public void run() {
						try {
						Session session = HibernateUtil.currentNativeSession();
						List<Long> ids = session.createCriteria(PeminjamanSuratItemDetail.class)

								.createAlias("suratMasuk", "suratMasuk", Criteria.LEFT_JOIN)

								.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.ilike("suratMasuk.perihal", searchjudul.getValue().trim(),
												MatchMode.ANYWHERE))

								.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.or(
												Restrictions.ilike("suratMasuk.kode", searchkode.getValue().trim(),
														MatchMode.EXACT),
												Restrictions.ilike("suratMasuk.noSurat", searchkode.getValue().trim(),
														MatchMode.EXACT)))

								.add(searchBelumDikembalikan.isChecked() ? Restrictions.isNull("kembaliSuratItemDetail")
										: Restrictions.sqlRestriction("true"))

								.setProjection(Projections.property("id")).list();
						try {

							System.out.println("Proses ids " + ids.size());
							int rowCount = ids.size();
							for (int i = 0; i < rowCount; i++) {
								try {
									PeminjamanSuratItemDetail peminjamanSuratItemDetail = (PeminjamanSuratItemDetail) session
											.createCriteria(PeminjamanSuratItemDetail.class)
											.add(Restrictions.idEq(ids.get(i))).uniqueResult();
									if (peminjamanSuratItemDetail != null) {
										session.getTransaction().begin();
										session.update(peminjamanSuratItemDetail);
										session.getTransaction().commit();

									}

									label.setValue("Memproses data \""
											+ (peminjamanSuratItemDetail == null ? ""
													: peminjamanSuratItemDetail.getPeminjamanSuratItem()
															.getPeminjamSurat().getNama() + " - "
															+ peminjamanSuratItemDetail.getSuratMasuk().getKode())
											+ "\" (" + Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							}
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sirkulasisurat/PeminjamanSuratItemAction.java:305");
						}
						ids = null;
						HibernateUtil.closeSession();

						label.setValue("");
											} finally {
							ais.database.hibernate.HibernateUtil.closeSession();
						}
					}
				}).start();

			}

		});
	        FilterLanjutHelper.setup(comp);
}

	private void setujui(PeminjamanSuratItem peminjamanSuratItem) {
		Session session = HibernateUtil.currentSession();
		peminjamanSuratItem.setDisetujuiOleh(Common.getCurrentUser());
		peminjamanSuratItem.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
		Common.refreshUpdate(session, peminjamanSuratItem);

	}

	class PeminjamanSuratItemRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PeminjamanSuratItem peminjamanSuratItem = (PeminjamanSuratItem) arg1;

			(new PeminjamanSuratItemDetailAction(peminjamanSuratItem, tipe)).setParent(arg0);

			RevisiHelper.createNewRevisi(PeminjamanSuratItem.class, peminjamanSuratItem, peminjamanSuratItem.getKode())
					.setParent(arg0);

			new Label(peminjamanSuratItem.getPeminjamSurat() == null ? ""
					: peminjamanSuratItem.getPeminjamSurat().toString()).setParent(arg0);

			new Label(peminjamanSuratItem.getKembaliSuratItem() == null
					|| peminjamanSuratItem.getKembaliSuratItem().getDisetujuiOleh() == null ? "Belum dikembalikan"
							: "Sudah dikembalikan (" + peminjamanSuratItem.getKembaliSuratItem() + ")")
					.setParent(arg0);

			new Label(
					(peminjamanSuratItem.getSatuanKerja() == null ? ""
							: "dari " + peminjamanSuratItem.getSatuanKerja().getNama())
							+ " "
							+ (peminjamanSuratItem.getKepadaSatuanKerja() == null ? ""
									: " kepada " + peminjamanSuratItem.getKepadaSatuanKerja().getNama()))
					.setParent(arg0);

			new Label(peminjamanSuratItem.getTujuanPeminjaman()).setParent(arg0);

			new Label((peminjamanSuratItem.getMulai() == null ? ""
					: Common.dateFormat1.get().format(peminjamanSuratItem.getMulai()))
					+ (peminjamanSuratItem.getSampai() == null ? ""
							: " sd " + Common.dateFormat1.get().format(peminjamanSuratItem.getSampai())))
					.setParent(arg0);

			new Label(peminjamanSuratItem.getDibuatOleh() == null ? ""
					: peminjamanSuratItem.getDibuatOleh().getUserNama()).setParent(arg0);
			new Label(peminjamanSuratItem.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanSuratItem.getTanggalPembuatan())).setParent(arg0);

			final Html htmldenda = new ais.ui.util.MyHtml();
			htmldenda.setParent(arg0);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String content = LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanSuratItem);
					htmldenda.setContent(content);
				}
			});

			(new Label(peminjamanSuratItem.getDisetujuiOleh() == null ? ""
					: peminjamanSuratItem.getDisetujuiOleh().getUserNama())).setParent(arg0);

			(new Label(peminjamanSuratItem.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(peminjamanSuratItem.getTanggalPersetujuan()))).setParent(arg0);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(Common.simpleString(peminjamanSuratItem.getKeterangan())).setParent(vbox1);
			if (peminjamanSuratItem.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + peminjamanSuratItem.getDisposisiSop().getKeterangan() + " ("
						+ peminjamanSuratItem.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(peminjamanSuratItem.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Peminjaman Item");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {

					Report.generatePDFReport(Report.PDF, parameter(peminjamanSuratItem), "peminjaman_arsip",
							peminjamanSuratItem.getTanggalPembuatan());
				}

			});
			button.setParent(toolbar);

			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && (peminjamanSuratItem.getKembaliSuratItem() == null
					|| peminjamanSuratItem.getKembaliSuratItem().getDisetujuiOleh() == null));
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(peminjamanSuratItem);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			rubah.setParent(toolbar);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && (peminjamanSuratItem.getKembaliSuratItem() == null
					|| peminjamanSuratItem.getKembaliSuratItem().getDisetujuiOleh() == null));
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();
											List<PeminjamanSuratItemDetail> peminjamanSuratItemDetails = session
													.createCriteria(PeminjamanSuratItemDetail.class)
													.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem))
													.list();
											for (PeminjamanSuratItemDetail peminjamanSuratItemDetail : peminjamanSuratItemDetails) {
												Common.refreshDelete(session, peminjamanSuratItemDetail);
											}

											session.createSQLQuery("delete from surat.peminjaman_surat_item where id="
													+ peminjamanSuratItem.getId()).executeUpdate();

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			hapus.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameter(PeminjamanSuratItem peminjamanSuratItem) throws Exception {
		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/barcode_" + peminjamanSuratItem.getKode() + ".png");
		Barcode mybarcode = BarcodeFactory.createCode128B(peminjamanSuratItem.getKode());
		BarcodeImageHandler.savePNG(mybarcode, myfilebarcode);
		String kodeBarcode = myfilebarcode.getAbsolutePath();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", peminjamanSuratItem.getId());
		parameters.put("kode_barcode", kodeBarcode);

		parameters.put("info", LibraryUtil.tampilanSummaryPeminjaman(null, peminjamanSuratItem));

		Common.insertProperty(PeminjamanSuratItem.class, peminjamanSuratItem, parameters, "");

		List<Map> maps = new ArrayList<Map>();
		List<PeminjamanSuratItemDetail> objects = HibernateUtil.currentSession()
				.createCriteria(PeminjamanSuratItemDetail.class)
				.add(Restrictions.eq("peminjamanSuratItem", peminjamanSuratItem)).list();

		for (PeminjamanSuratItemDetail peminjamanSuratItemDetail : objects) {
			SuratMasuk suratMasuk = peminjamanSuratItemDetail.getSuratMasuk();
			Map map = new HashMap();
			Common.insertProperty(PeminjamanSuratItemDetail.class, peminjamanSuratItemDetail, map, "", 1, "suratMasuk");
			Common.insertProperty(SuratMasuk.class, suratMasuk, map, "suratMasuk", 2);
			maps.add(map);
		}
		parameters.put("maps", maps);

		DisposisiAlurSop.parameterMap(peminjamanSuratItem.getDisposisiSop(), parameters);

		return parameters;
	}

	public void onAdd(Event event) throws Exception {
		init(new PeminjamanSuratItem());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		this.peminjamanSuratItem = (PeminjamanSuratItem) generalValueObject;
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Peminjaman"));
		String mykode = peminjamanSuratItem.getKode();
		row.appendChild(kode = new MyTextbox(mykode));
		kode.setWidth("90%");
		kode.setDisabled(true);

		if (mykode == null || mykode.trim().isEmpty()) {
			mykode = LibraryUtil.generateCode(PeminjamanSuratItem.class, 8, "PNJ");
			kode.setValue(mykode);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Peminjaman *"));
		tanggalPembuatan = new MyDatebox(
				peminjamanSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: peminjamanSuratItem.getTanggalPembuatan());

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat3.get()
					.format(peminjamanSuratItem.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
							: peminjamanSuratItem.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");
		tanggalPembuatan.setReadonly(true);

		Tbmuser tbmuser = Common.getCurrentUser();
		if (peminjamanSuratItem.getPeminjamSurat() == null && tbmuser != null && tbmuser.getPegawai() != null) {
			PeminjamSurat peminjamSurat = (PeminjamSurat) HibernateUtil.currentSession()
					.createCriteria(PeminjamSurat.class).add(Restrictions.eq("pegawai", tbmuser.getPegawai()))
					.setMaxResults(1).uniqueResult();
			if (peminjamSurat != null && peminjamSurat.getId() != null) {
				peminjamanSuratItem.setPeminjamSurat(peminjamSurat);
			}
		}

		MyFormRow rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Dari Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(
				peminjamanSuratItem.getSatuanKerja() == null ? "" : peminjamanSuratItem.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", peminjamanSuratItem.getSatuanKerja());
		satuanKerja.setReadonly(true);
		if (persetujuan) {
			rowSatker.appendChild(new Label(peminjamanSuratItem.getSatuanKerja() == null ? ""
					: peminjamanSuratItem.getSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		rowSatker = new MyFormRow();
		rowSatker.setParent(rows);
		rowSatker.appendChild(new ais.ui.util.MyLabelConfig("Kepada Satuan Kerja *"));
		kepadaSatuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		kepadaSatuanKerja.setValue(peminjamanSuratItem.getKepadaSatuanKerja() == null ? ""
				: peminjamanSuratItem.getKepadaSatuanKerja().getNama());
		kepadaSatuanKerja.setAttribute("satuanKerja", peminjamanSuratItem.getKepadaSatuanKerja());
		kepadaSatuanKerja.setReadonly(true);
		if (persetujuan) {
			rowSatker.appendChild(new Label(peminjamanSuratItem.getKepadaSatuanKerja() == null ? ""
					: peminjamanSuratItem.getKepadaSatuanKerja().getNama()));
		} else {
			rowSatker.appendChild(kepadaSatuanKerja);
		}
		kepadaSatuanKerja.setWidth("90%");
		kepadaSatuanKerja.setDisabled(false);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Peminjam " + tipe));
		peminjamSurat = new AmbilDataPeminjamSuratBanbox();

		if (persetujuan) {
			row.appendChild(new Label(peminjamanSuratItem.getPeminjamSurat() == null ? ""
					: peminjamanSuratItem.getPeminjamSurat().getNama()));
		} else {
			row.appendChild(peminjamSurat);
		}

		peminjamSurat.setAttribute("peminjamSurat", peminjamanSuratItem.getPeminjamSurat());
		peminjamSurat.setValue(peminjamanSuratItem.getPeminjamSurat() == null ? ""
				: peminjamanSuratItem.getPeminjamSurat().toString());
		peminjamSurat.setWidth("90%");
		peminjamSurat.setReadonly(true);

		if (peminjamanSuratItem.getPeminjamSurat() != null) {
			peminjamSurat.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lama Peminjaman *"));
		mulai = new MyDatebox(peminjamanSuratItem.getMulai());
		sampai = new MyDatebox(peminjamanSuratItem.getSampai());

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		if (persetujuan) {

			hbox.appendChild(new Label(peminjamanSuratItem.getMulai() == null ? ""
					: Common.dateFormat1.get().format(peminjamanSuratItem.getMulai())));
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			hbox.appendChild(new Label(peminjamanSuratItem.getSampai() == null ? ""
					: Common.dateFormat1.get().format(peminjamanSuratItem.getSampai())));
		} else {
			hbox.appendChild(mulai);
			hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
			hbox.appendChild(sampai);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tujuan Peminjaman"));

		tujuanPeminjaman = new MyTextbox(peminjamanSuratItem.getTujuanPeminjaman());

		if (persetujuan) {
			row.appendChild(new Label(peminjamanSuratItem.getTujuanPeminjaman()));
		} else {
			row.appendChild(tujuanPeminjaman);
		}

		tujuanPeminjaman.setWidth("90%");
		tujuanPeminjaman.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));

		keterangan = new MyTextbox(
				peminjamanSuratItem.getKeterangan() == null ? "" : peminjamanSuratItem.getKeterangan());

		if (persetujuan) {
			row.appendChild(new Label(peminjamanSuratItem.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild((new PeminjamanSuratItemPunyaItemHelper(tipe, persetujuan)).initDetail(
				PeminjamanSuratItemAction.this.peminjamanSuratItem, gridItem = new MyGrid(), kepadaSatuanKerja));

		return grid;
	}

	private void init(final PeminjamanSuratItem peminjamanSuratItem) throws Exception {
		this.peminjamanSuratItem = peminjamanSuratItem;
		addWindow.setTitle("Pendataan Peminjaman " + tipe);
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop=null;center.appendChild(form(peminjamanSuratItem, disposisiSop, save, null));

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

					addWindow.setVisible(false);
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							setujui(PeminjamanSuratItemAction.this.peminjamanSuratItem);

							kirim(PeminjamanSuratItemAction.this.peminjamanSuratItem);

							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Peminjaman Surat belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Peminjaman; (2) isikan kode peminjaman secara lengkap; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (peminjamSurat.getAttribute("peminjamSurat") == null) {
			MyMessageboxConfig.show("Mohon maaf, Peminjam Surat belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Peminjam Surat; (2) cari dan pilih peminjam yang sesuai dari daftar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggalPembuatan.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Pembuatan Peminjaman belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Pembuatan; (2) pilih tanggal yang sesuai dari kalender; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja Asal (Dari) belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Dari Satuan Kerja; (2) pilih satuan kerja asal yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kepadaSatuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja Tujuan (Kepada) belum dipilih. Langkah yang dapat dilakukan: (1) klik kolom Kepada Satuan Kerja; (2) pilih satuan kerja tujuan yang sesuai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (mulai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Mulai Peminjaman belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Mulai; (2) pilih tanggal mulai peminjaman dari kalender; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (sampai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Sampai Peminjaman belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Sampai; (2) pilih tanggal akhir peminjaman dari kalender; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (Common.bolehKonfigurasi("peminjamSurat_tidak_boleh_meminjam_lagi_meskipun_peminjaman_sebelumnya_belum_dikembalikan")) {
			PeminjamSurat myPeminjamSurat = (PeminjamSurat) peminjamSurat.getAttribute("peminjamSurat");
			Session session = HibernateUtil.currentSession();
			Number count = (Number) session.createCriteria(PeminjamanSuratItemDetail.class)
					.add(Restrictions.isNull("kembaliSuratItemDetail"))
					.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
					.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", myPeminjamSurat))
					.add(peminjamanSuratItem.getId() != null
							? Restrictions.ne("peminjamanSuratItem.id", peminjamanSuratItem.getId())
							: Restrictions.sqlRestriction("true"))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (count.intValue() > 0) {
				MyMessageboxConfig.show("PeminjamSurat dengan kode " + myPeminjamSurat.getKode() + " dan nama "
						+ myPeminjamSurat.getNama()
						+ " masih ada peminjaman.\n\nPeminjam Surat tersebut harus mengembalikan item yang masih dipinjam.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsItem = gridItem.getRows().getChildren();

		int jumlahPeminjamanSuratItemDetails = ((Number) HibernateUtil.currentSession()
				.createCriteria(PeminjamanSuratItemDetail.class)
				.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
				.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamanSuratItem.getPeminjamSurat()))
				.setProjection(Projections.rowCount()).add(Restrictions.isNull("kembaliSuratItemDetail"))
				.uniqueResult()).intValue();

		Integer jumlahmaksimal = PeminjamanSuratItemAction.this.peminjamanSuratItem.getJumlahMaksimalPeminjaman();
		System.out.println("jumlahmaksimal = " + jumlahmaksimal);
		if (jumlahmaksimal != null
				&& jumlahmaksimal.intValue() < (jumlahPeminjamanSuratItemDetails + rowsItem.size())) {
			MyMessageboxConfig.show(
					"Jumlah maksimal item yang boleh dipinjam adalah " + jumlahmaksimal
							+ " buah.\nItem yang telah dipinjam : " + jumlahPeminjamanSuratItemDetails
							+ " buah, dan item yang akan dimpinjam adalah " + rowsItem.size(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		for (Row row : rowsItem) {
			PeminjamanSuratItemDetail peminjamanSuratItemDetail = (PeminjamanSuratItemDetail) row
					.getAttribute("peminjamanSuratItemDetail");
			if (peminjamanSuratItemDetail.getSuratMasuk() == null) {
				MyMessageboxConfig.show("Mohon maaf, terdapat baris Item Surat yang belum dipilih. Langkah yang dapat dilakukan: (1) periksa daftar item pada tabel; (2) hapus baris yang kosong atau pilih surat yang akan dipinjam; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();

		if (peminjamanSuratItem.getId() != null) {
			peminjamanSuratItem = (PeminjamanSuratItem) session.load(PeminjamanSuratItem.class,
					peminjamanSuratItem.getId());

		}

		System.out.println("mulai menyimpan --> 0");
		peminjamanSuratItem.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		peminjamanSuratItem.setKepadaSatuanKerja((SatuanKerja) kepadaSatuanKerja.getAttribute("satuanKerja"));
		peminjamanSuratItem.setPeminjamSurat((PeminjamSurat) peminjamSurat.getAttribute("peminjamSurat"));
		peminjamanSuratItem.setKode(kode.getValue());
		peminjamanSuratItem.setKeterangan(keterangan.getValue());
		peminjamanSuratItem.setTanggalPembuatan(tanggalPembuatan.getValue());
		peminjamanSuratItem.setTipe(tipe);
		peminjamanSuratItem.setTujuanPeminjaman(tujuanPeminjaman.getValue());
		peminjamanSuratItem.setMulai(mulai.getValue());
		peminjamanSuratItem.setSampai(sampai.getValue());

		System.out.println("setSatuanKerja --> " + peminjamanSuratItem.getSatuanKerja());
		System.out.println("setKepadaSatuanKerja --> " + peminjamanSuratItem.getKepadaSatuanKerja());
		System.out.println("setTujuanPeminjaman --> " + peminjamanSuratItem.getTujuanPeminjaman());

		if (disposisiSop != null && disposisiSop.getId() != null) {
			peminjamanSuratItem.setDisposisiSop(disposisiSop);
		}

		Integer jumlahHariBatas = 0;
		for (Row row : rowsItem) {
			PeminjamanSuratItemDetail peminjamanSuratItemDetail = (PeminjamanSuratItemDetail) row
					.getAttribute("peminjamanSuratItemDetail");
			SuratMasuk suratMasuk = peminjamanSuratItemDetail.getSuratMasuk();
			if (suratMasuk != null && suratMasuk.getId() != null) {
				session.refresh(suratMasuk);
				if (suratMasuk != null && suratMasuk.getKlasifikasiSuratMasuk() != null
						&& suratMasuk.getKlasifikasiSuratMasuk().getMaksimalHariPinjam() != null) {

					if (jumlahHariBatas < suratMasuk.getKlasifikasiSuratMasuk().getMaksimalHariPinjam()) {
						jumlahHariBatas = suratMasuk.getKlasifikasiSuratMasuk().getMaksimalHariPinjam();
					}
					peminjamanSuratItemDetail.setJumlahMaxPerpanjangan(
							suratMasuk.getKlasifikasiSuratMasuk().getMaksimalJumlahPerpanjaangan());
				}
			}
		}

		if (peminjamanSuratItem.getId() != null) {
			Common.refreshUpdate(session, peminjamanSuratItem);
			session.flush();
		} else {
			peminjamanSuratItem.setDibuatOleh(Common.getCurrentUser());

			peminjamanSuratItem.setIndex(LibraryUtil.generateMaxByPerpustakaan(PeminjamanSuratItem.class) + 1);
			String mykode = LibraryUtil.generateCode(PeminjamanSuratItem.class, 8, "PNJ");
			kode.setValue(mykode);
			peminjamanSuratItem.setKode(mykode);

			peminjamanSuratItem.setJumlahHariBatas(jumlahHariBatas == null ? 0 : jumlahHariBatas.intValue());

			session.save(peminjamanSuratItem);
			session.flush();

		}

		for (Row row : rowsItem) {
			PeminjamanSuratItemDetail peminjamanSuratItemDetail = (PeminjamanSuratItemDetail) row
					.getAttribute("peminjamanSuratItemDetail");
			peminjamanSuratItemDetail.setPeminjamanSuratItem(peminjamanSuratItem);

			System.out.println("peminjamanSuratItemDetail --> " + peminjamanSuratItemDetail);

			Common.refreshSaveOrUpdate(session, peminjamanSuratItemDetail);
		}

		System.out.println("mulai selesai -->");

		return true;
	}

	@SuppressWarnings({})
	public static void kirim(final PeminjamanSuratItem peminjamanSuratItem) throws Exception {

		final File file = Report.generatePDFReport(Report.PDF, parameter(peminjamanSuratItem), "peminjaman_arsip",
				peminjamanSuratItem.getTanggalPembuatan(), null, Common.locale, null);

		PeminjamSurat peminjamSurat = peminjamanSuratItem.getPeminjamSurat();

		Siswa siswa = peminjamSurat.getSiswa();
		Mahasiswa mahasiswa = peminjamSurat.getMahasiswa();
		Tbmuser tbmuser = peminjamSurat.getTbmuser();

		JSONArray userIds = new JSONArray();
		if (siswa != null && !siswa.getNomorIndukNasional().isEmpty()) {
			userIds.put(siswa.getNomorIndukNasional());
		} else if (mahasiswa != null && mahasiswa.getNim() != null) {
			userIds.put(mahasiswa.getNim());
		} else if (tbmuser != null && tbmuser.getUserId() != null) {
			userIds.put(tbmuser.getUserId());
		}

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Sekolah sekolahD = SekolahUtil.getSekolah();

		final String sekolah = siswa != null ? siswa.getSekolah().getNama()
				: mahasiswa != null && mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null
						&& mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
								? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama()
								: peminjamSurat.getDosen() != null
										&& peminjamSurat.getDosen().getPerguruanTinggi() != null
												? peminjamSurat.getDosen().getPerguruanTinggi().getNama()
												: peminjamSurat.getGuru() != null
														&& peminjamSurat.getGuru().getSekolah() != null
																? peminjamSurat.getGuru().getSekolah().getNama()
																: sekolahD != null ? sekolahD.getNama()
																		: perguruanTinggi != null
																				? perguruanTinggi.getNama()
																				: "";

		final String nama = (siswa != null ? siswa.getNama()
				: mahasiswa != null ? mahasiswa.getNama()
						: peminjamSurat.getDosen() != null ? peminjamSurat.getDosen().getNama()
								: peminjamSurat.getGuru() != null ? peminjamSurat.getGuru().getNama()
										: peminjamSurat.getPegawai() != null ? peminjamSurat.getPegawai().getNama()
												: peminjamSurat.getTbmuser() != null
														? peminjamSurat.getTbmuser().getUserNama()
														: "");

		String info = "Atas nama: " + nama;
		String subject = "Informasi Peminjaman Berhasil => " + info;

		String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
				+ "Kami memberitahukan bahwa peminjaman " + peminjamanSuratItem.getTipe() + " di " + sekolah
				+ " telah berhasil dilakukan pada "
				+ Common.dateFormat61.get().format(peminjamanSuratItem.getTanggalPembuatan()) + ".\r\n" + "\r\n"
				+ "Berikut adalah rincian peminjaman Anda:\r\n" + "\r\n" + "*   **Nama Peminjam:** " + nama + "\r\n"
				+ "*   **Tanggal Peminjaman:** " + Common.dateFormat51.get().format(peminjamanSuratItem.getTanggalPembuatan())
				+ "\r\n" + "\r\n"
				+ "Mohon untuk menjaga dan merawat buku yang dipinjam, serta mengembalikan buku sesuai dengan tenggat waktu yang telah ditentukan. Detail mengenai buku yang Anda pinjam beserta tenggat waktu pengembalian dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
				+ "\r\n" + "Jika Anda memiliki pertanyaan lebih lanjut, jangan ragu untuk menghubungi kami.\r\n"
				+ "\r\n" + "Terima kasih atas partisipasi Anda.";

		String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

		String emailUser = "";
		if (siswa != null && siswa.getAlamatEmail() != null && Common.isValidEmailAddress(siswa.getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? siswa.getAlamatEmail().trim()
					: "," + siswa.getAlamatEmail().trim();
		}
		if (mahasiswa != null && mahasiswa.getEmail() != null && Common.isValidEmailAddress(mahasiswa.getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? mahasiswa.getEmail().trim() : "," + mahasiswa.getEmail().trim();
		}
		if (peminjamSurat.getDosen() != null && peminjamSurat.getDosen().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getDosen().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getDosen().getEmail().trim()
					: "," + peminjamSurat.getDosen().getEmail().trim();
		}
		if (peminjamSurat.getGuru() != null && peminjamSurat.getGuru().getAlamatEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getGuru().getAlamatEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getGuru().getAlamatEmail().trim()
					: "," + peminjamSurat.getGuru().getAlamatEmail().trim();
		}
		if (peminjamSurat.getPegawai() != null && peminjamSurat.getPegawai().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getPegawai().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getPegawai().getEmail().trim()
					: "," + peminjamSurat.getPegawai().getEmail().trim();
		}
		if (peminjamSurat.getTbmuser() != null && peminjamSurat.getTbmuser().getEmail() != null
				&& Common.isValidEmailAddress(peminjamSurat.getTbmuser().getEmail())) {
			emailUser += emailUser.trim().isEmpty() ? peminjamSurat.getTbmuser().getEmail().trim()
					: "," + peminjamSurat.getTbmuser().getEmail().trim();
		}
		JSONArray attachmentsData = null;

		MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser, peminjamanSuratItem, attachmentsData,
				false, file);

		if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pinjam_buku_perpustakaan_ke_wa")) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal",
							"*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n")
							.getNilai();

					Set<String> forms = new HashSet<String>();
					forms.add(peminjamanSuratItem.getPeminjamSurat().getTelp());
					forms.add(peminjamanSuratItem.getPeminjamSurat().getHp());

					if (peminjamanSuratItem.getPeminjamSurat().getSiswa() != null) {
						forms.addAll(peminjamanSuratItem.getPeminjamSurat().getSiswa().ambilTelp());
					}

					for (String from : forms) {

						if (from != null && !from.trim().isEmpty()
								&& !(from == null || from.toString().trim().isEmpty()
										|| from.toString().trim().equals("00000000000000000000")
										|| from.toString().trim().equals("000000000"))) {

							String body = "**Yth. " + nama + ",**\r\n" + "\r\n" + "Dengan hormat,\r\n" + "\r\n"
									+ "Kami memberitahukan bahwa peminjaman " + peminjamanSuratItem.getTipe() + " di "
									+ sekolah + " telah berhasil dilakukan pada "
									+ Common.dateFormat61.get().format(peminjamanSuratItem.getTanggalPembuatan()) + ".\r\n"
									+ "\r\n" + "Berikut adalah rincian peminjaman Anda:\r\n" + "\r\n"
									+ "*   **Nama Peminjam:** " + nama + "\r\n" + "*   **Tanggal Peminjaman:** "
									+ Common.dateFormat51.get().format(peminjamanSuratItem.getTanggalPembuatan()) + "\r\n"
									+ "\r\n"
									+ "Mohon untuk menjaga dan merawat buku yang dipinjam, serta mengembalikan buku sesuai dengan tenggat waktu yang telah ditentukan. Detail mengenai buku yang Anda pinjam beserta tenggat waktu pengembalian dapat Anda lihat pada sistem/portal perpustakaan atau pada file terlampir.\r\n"
									+ "\r\n"
									+ "Jika Anda memiliki pertanyaan lebih lanjut, jangan ragu untuk menghubungi kami.\r\n"
									+ "\r\n" + "Terima kasih atas partisipasi Anda.";

							String urlD = Common.getRequestHostWithProtocolSimple()
									+ file.getAbsolutePath().split("webapps")[1];

							Wa.kirimWaViaUltramsg(from, dawal + body, "Bukti_Peminjaman.pdf", urlD);
						}
					}
				}
			}, "", false, 2000);
		}
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		System.out.println("satuanKerjas -> " + satuanKerjas);

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PeminjamanSuratItem.class)

				.add(Restrictions.or(
						Restrictions.or(Restrictions.isNull("satuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												parent == null ? Restrictions.isNull("satuanKerja")
														: Restrictions.sqlRestriction("false"),
												Restrictions.in("satuanKerja", satuanKerjas))),
						Restrictions.or(Restrictions.isNull("kepadaSatuanKerja"),
								satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
										: Restrictions.or(
												parent == null ? Restrictions.isNull("kepadaSatuanKerja")
														: Restrictions.sqlRestriction("false"),
												Restrictions.in("kepadaSatuanKerja", satuanKerjas)))))

		;

		boolean ada = !searchkodesurat.getValue().trim().isEmpty() || !searchjudul.getValue().trim().isEmpty()
				|| !searchkodesurat.getValue().trim().isEmpty() || searchBelumDikembalikan.isChecked();

		if (ada) {
			List<Long> ids = session.createCriteria(PeminjamanSuratItemDetail.class)

					.createAlias("suratMasuk", "suratMasuk", Criteria.LEFT_JOIN)

					.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.ilike("suratMasuk.perihal", searchjudul.getValue().trim(),
									MatchMode.ANYWHERE))

					.add(searchkodesurat.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("suratMasuk.kode", searchkodesurat.getValue().trim(),
											MatchMode.EXACT),
									Restrictions.ilike("suratMasuk.noSurat", searchkodesurat.getValue().trim(),
											MatchMode.EXACT)))

					.add(searchBelumDikembalikan.isChecked() ? Restrictions.isNull("kembaliSuratItemDetail")
							: Restrictions.sqlRestriction("true"))

					.setMaxResults(32760)

					.setProjection(Projections.groupProperty("peminjamanSuratItem.id")).list();

			if (!ids.isEmpty()) {
				criteria.add(Restrictions.in("id", ids));
			}
		}

		criteria

				.add((searchpeminjamSurat == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpeminjamSurat.getAttribute("peminjamSurat") == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("peminjamSurat", searchpeminjamSurat.getAttribute("peminjamSurat"))))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE))

				.add((searchmulai == null || searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "') and date('"
						+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")));
		if (order)
			criteria.addOrder(Order.desc("tanggalPembuatan"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchkode == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);
		List<PeminjamanSuratItem> peminjamanSuratItem = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(peminjamanSuratItem);
		grid.setRowRenderer(new PeminjamanSuratItemRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void onKodePeminjamSurat(Event event) throws Exception {
		if (searchkodeangota.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Peminjam belum diisi. Langkah yang dapat dilakukan: (1) klik kolom Kode Peminjam; (2) isikan kode peminjam secara lengkap; (3) klik tombol Cari untuk mencari data peminjam. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		PeminjamSurat peminjamSurat = (PeminjamSurat) session.createCriteria(PeminjamSurat.class)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("dosen", "dosen", Criteria.LEFT_JOIN).createAlias("pegawai", "pegawai", Criteria.LEFT_JOIN)
				.add(Restrictions.or(
						Restrictions.ilike("pegawai.mycode", searchkodeangota.getValue().trim(), MatchMode.EXACT),
						Restrictions.or(
								Restrictions.ilike("pegawai.code", searchkodeangota.getValue().trim(), MatchMode.EXACT),
								Restrictions.or(Restrictions.or(
										Restrictions.ilike("kode", searchkodeangota.getValue().trim(), MatchMode.EXACT),
										Restrictions.ilike("mahasiswa.nim", searchkodeangota.getValue().trim(),
												MatchMode.EXACT)),
										Restrictions.ilike("dosen.mycode", searchkodeangota.getValue().trim(),
												MatchMode.EXACT)

								)))).setMaxResults(1).uniqueResult();
		if (peminjamSurat == null) {
			MyMessageboxConfig.show("Kode Peminjam \"" + searchkodeangota.getValue().trim() + "\" tidak ditemukan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return;
		}

		if (!peminjamSurat.getAktif()) {
			MyMessageboxConfig.show("Peminjam ini tidak aktif, sehingga tidak diizinkan untuk meminjam " + tipe,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			searchkodeangota.focus();
			searchkodeangota.select();
			return;
		}

		searchkodeangota.setValue("");

		PeminjamanSuratItem peminjamanSuratItem = new PeminjamanSuratItem();
		peminjamanSuratItem.setPeminjamSurat(peminjamSurat);

		int jumlahPeminjamanSuratItemDetails = ((Number) HibernateUtil.currentSession()
				.createCriteria(PeminjamanSuratItemDetail.class)
				.createAlias("peminjamanSuratItem", "peminjamanSuratItem")
				.add(Restrictions.eq("peminjamanSuratItem.peminjamSurat", peminjamanSuratItem.getPeminjamSurat()))
				.setProjection(Projections.rowCount()).add(Restrictions.isNull("kembaliSuratItemDetail"))
				.uniqueResult()).intValue();

		Integer jumlahmaksimal = 5;
		try {
			jumlahmaksimal = Integer.parseInt(
					Common.getKonfigurasi("jumlah_" + tipe + "_maksimal_dalam_sekali_pinjam", "5").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sirkulasisurat/PeminjamanSuratItemAction.java:1243");
			// TODO: handle exception
		}
		System.out.println("jumlahmaksimal = " + jumlahmaksimal);
		if (jumlahmaksimal != null && jumlahmaksimal.intValue() <= (jumlahPeminjamanSuratItemDetails)) {
			MyMessageboxConfig.show(
					"Jumlah maksimal " + tipe + " yang boleh dipinjam adalah " + jumlahmaksimal + " buah.\n" + tipe
							+ " yang telah dipinjam : " + jumlahPeminjamanSuratItemDetails,
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		init(peminjamanSuratItem);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Peminjaman " + tipe;
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return peminjamanSuratItem;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PeminjamanSuratItem.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		PeminjamanSuratItem peminjamanSuratItem = (PeminjamanSuratItem) generalValueObject;
		File file = Report.generateFileReport(Report.PDF, parameter(peminjamanSuratItem), "peminjaman_arsip",
				ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}
}
