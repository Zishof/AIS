package ais.action.master.penelitiandanpengabdian;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
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
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.TingkatArtikel;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TingkatArtikelAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private TingkatArtikel tingkatArtikel;
	private MyToolbarbuttonConfig add;

	public static String[] contents = new String[] { "id", "nama", "keterangan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(TingkatArtikel.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			TingkatArtikel angket = new TingkatArtikel();
			angket.setNama("Lokal");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Nasional non terakreditasi");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Jurnal ilmiah terakreditasi DIKTI");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Jurnal ilmiah internasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Buku tingkat nasional");
			Common.refreshSaveOrUpdate(session, angket);
			//
			// angket = new TingkatArtikel();
			// angket.setNama("Buku tingkat internasional");
			// Common.refreshSaveOrUpdate(session, angket);
			//
			// angket = new TingkatArtikel();
			// angket.setNama("Karya seni tingkat nasional");
			// Common.refreshSaveOrUpdate(session, angket);
			//
			// angket = new TingkatArtikel();
			// angket.setNama("Karya seni tingkat internasional");
			// Common.refreshSaveOrUpdate(session, angket);
			//
			// angket = new TingkatArtikel();
			// angket.setNama("Karya sastra tingkat nasional");
			// Common.refreshSaveOrUpdate(session, angket);
			//
			// angket = new TingkatArtikel();
			// angket.setNama("Karya sastra tingkat internasional");
			// Common.refreshSaveOrUpdate(session, angket);
		}

		count = ((Number) session.createCriteria(TingkatArtikel.class)
				.add(Restrictions.eq("nama", "Buku tingkat internasional")).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) {
			TingkatArtikel angket = new TingkatArtikel();
			angket.setNama("Buku tingkat internasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Karya seni tingkat nasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Karya seni tingkat internasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Karya sastra tingkat nasional");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new TingkatArtikel();
			angket.setNama("Karya sastra tingkat internasional");
			Common.refreshSaveOrUpdate(session, angket);
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
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

							final Label peringatan = new Label("");

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
										System.out.println("loading file " + file.getAbsolutePath());
										MyMessageboxConfig.show("Upload data berhasil dilakukan."
												+ (peringatan.getValue().isEmpty() ? "" : "\n" + peringatan.getValue()),
												"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
												new EventListener() {

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

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(TingkatArtikel.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												TingkatArtikel tingkatArtikel = id == null || id.equals(-1L) ? null
														: (TingkatArtikel) session.createCriteria(TingkatArtikel.class)
																.add(Restrictions.idEq(id)).uniqueResult();

												if (tingkatArtikel == null) {
													tingkatArtikel = new TingkatArtikel();
												}

												Common.setObjectValues(classMetadata, tingkatArtikel, contents, 1,
														sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(tingkatArtikel);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + tingkatArtikel.getKode() + " - "
														+ tingkatArtikel.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e); 
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/penelitiandanpengabdian/TingkatArtikelAction.java:292");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}).start();

						}
					}, "Harap tunggu.. sedang melakukan proses upload data..");

				} else {
					MyMessageboxConfig
							.show("File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media, "Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
	}

	class TingkatArtikelRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final TingkatArtikel tingkatArtikel = (TingkatArtikel) arg1;

			RevisiHelper.createNewRevisi(TingkatArtikel.class, tingkatArtikel, tingkatArtikel.getNama())
					.setParent(arg0);
			new Label(tingkatArtikel.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(tingkatArtikel.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					tingkatArtikel.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(tingkatArtikel);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tingkatArtikel);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(tingkatArtikel);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new TingkatArtikel());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(TingkatArtikel tingkatArtikel) {
		this.tingkatArtikel = tingkatArtikel;
		addWindow.setTitle(tingkatArtikel.getId() == null ? "Tambah Tingkat Publikasi" : "Ubah Tingkat Publikasi");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama "));
		row.appendChild(nama = new Textbox(tingkatArtikel.getNama() == null ? "" : tingkatArtikel.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(
				keterangan = new Textbox(tingkatArtikel.getKeterangan() == null ? "" : tingkatArtikel.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
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
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama  harus di isi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNama();
		if (i) {
			MyMessageboxConfig.show("Nama  sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tingkatArtikel.getId() != null) {
			tingkatArtikel = (TingkatArtikel) session.load(TingkatArtikel.class, tingkatArtikel.getId());

		}

		tingkatArtikel.setNama(nama.getValue());
		tingkatArtikel.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, tingkatArtikel);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TingkatArtikel.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TingkatArtikel> tingkatArtikel = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tingkatArtikel);
		grid.setRowRenderer(new TingkatArtikelRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNama() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(TingkatArtikel.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.tingkatArtikel.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.tingkatArtikel.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
