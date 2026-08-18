package ais.action.master.sekolah;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import ais.ui.util.MyInclude;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.AngketPenilaianGuru;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.GrupChecklistPenilaianGuru;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ChecklistPenilaianGuruAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchsekolah;
	private Combobox searchprogram;
	private Combobox searchyayasan;

	private Textbox nama;
	private MyDoublebox bobot;
	private Combobox grupChecklistPenilaianGuru;
	private Combobox searchGrup;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ChecklistPenilaianGuru checklistPenilaianGuru;
	private MyToolbarbuttonConfig add;

	private Tabpanel angketUmum;

	public void onAngketUmum(Event event) {
		if (angketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(angketUmum);
			MyInclude iframe = new MyInclude("/pages/master/checklist_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel parameterAngketUmum;

	public void onParameterAngketUmum(Event event) {
		if (parameterAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(parameterAngketUmum);
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_angket_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "isi", "grupChecklistPenilaianGuru", "bobot", "aktif",
			"keterangan" };

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

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPrograms(searchprogram);

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.insertCombo(grupChecklistPenilaianGuru = new Combobox(), "isi", "angketPenilaianGuru",
				GrupChecklistPenilaianGuru.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.insertComboDanSemua(searchGrup, "isi", GrupChecklistPenilaianGuru.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ChecklistPenilaianGuru.class, this, contents);
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
												.getClassMetadata(ChecklistPenilaianGuru.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												ChecklistPenilaianGuru checklistPenilaianGuru = id == null
														|| id.equals(-1L)
																? null
																: (ChecklistPenilaianGuru) session
																		.createCriteria(ChecklistPenilaianGuru.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (checklistPenilaianGuru == null) {
													checklistPenilaianGuru = new ChecklistPenilaianGuru();
												}

												Common.setObjectValues(classMetadata, checklistPenilaianGuru, contents,
														1, sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(checklistPenilaianGuru);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + checklistPenilaianGuru.getKode()
														+ " - " + checklistPenilaianGuru.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/ChecklistPenilaianGuruAction.java:275");
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
					MyMessageboxConfig.show(
							"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
									+ media,
							"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
				}
			}
		});
		Common.appendKeToolbar(upload, add, comp);
	}

	class ChecklistPenilaianGuruRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ChecklistPenilaianGuru checklistPenilaianGuru = (ChecklistPenilaianGuru) arg1;

			if (checklistPenilaianGuru.getGrupChecklistPenilaianGuru() == null) {
				checklistPenilaianGuru
						.setGrupChecklistPenilaianGuru((GrupChecklistPenilaianGuru) HibernateUtil.currentSession()
								.createCriteria(GrupChecklistPenilaianGuru.class).setMaxResults(1).uniqueResult());
			}

			RevisiHelper.createNewRevisi(ChecklistPenilaianGuru.class, checklistPenilaianGuru,
					checklistPenilaianGuru.getIsi()).setParent(arg0);
			new Label(checklistPenilaianGuru.getGrupChecklistPenilaianGuru() == null ? ""
					: checklistPenilaianGuru.getGrupChecklistPenilaianGuru().getIsi()).setParent(arg0);
			new Label(Common.numberFormat.get().format(checklistPenilaianGuru.getBobot())).setParent(arg0);

			AngketPenilaianGuru angketPenilaianGuru = checklistPenilaianGuru.getGrupChecklistPenilaianGuru() == null
					? null
					: checklistPenilaianGuru.getGrupChecklistPenilaianGuru().getAngketPenilaianGuru();
			new Label(angketPenilaianGuru == null || angketPenilaianGuru.getYayasan() == null ? "Semua"
					: angketPenilaianGuru.getYayasan().getNama()).setParent(arg0);
			new Label(angketPenilaianGuru == null || angketPenilaianGuru.getSekolah() == null ? "Semua"
					: angketPenilaianGuru.getSekolah().getNama()).setParent(arg0);

			new Label(angketPenilaianGuru == null || angketPenilaianGuru.getProgram() == null
					|| angketPenilaianGuru.getProgram().trim().isEmpty() ? "Semua" : angketPenilaianGuru.getProgram())
					.setParent(arg0);

			new Label(checklistPenilaianGuru.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(checklistPenilaianGuru.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					checklistPenilaianGuru.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(checklistPenilaianGuru);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(checklistPenilaianGuru);
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

											Common.refreshDelete(checklistPenilaianGuru);

											// agamaDao.commitTransaction();
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
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ChecklistPenilaianGuru());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ChecklistPenilaianGuru checklistPenilaianGuru) {
		this.checklistPenilaianGuru = checklistPenilaianGuru;
		addWindow.setTitle(checklistPenilaianGuru.getId() == null ? "Tambah Angket Penilaian Guru" : "Ubah Angket Penilaian Guru");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi"));
		row.appendChild(
				nama = new Textbox(checklistPenilaianGuru.getIsi() == null ? "" : checklistPenilaianGuru.getIsi()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket Guru"));
		row.appendChild(grupChecklistPenilaianGuru);
		Common.selectComboItem(grupChecklistPenilaianGuru,
				checklistPenilaianGuru.getGrupChecklistPenilaianGuru() == null ? null
						: checklistPenilaianGuru.getGrupChecklistPenilaianGuru());
		grupChecklistPenilaianGuru.setWidth("90%");
		grupChecklistPenilaianGuru.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(checklistPenilaianGuru.getBobot()));
		bobot.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				checklistPenilaianGuru.getKeterangan() == null ? "" : checklistPenilaianGuru.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
			MyMessageboxConfig.show("Nama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (grupChecklistPenilaianGuru.getSelectedItem() == null) {
			MyMessageboxConfig.show("Grup harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (checklistPenilaianGuru.getId() != null) {
			checklistPenilaianGuru = (ChecklistPenilaianGuru) session.load(ChecklistPenilaianGuru.class,
					checklistPenilaianGuru.getId());

		}

		checklistPenilaianGuru.setBobot(bobot.getValue());
		checklistPenilaianGuru.setIsi(nama.getValue());
		checklistPenilaianGuru.setGrupChecklistPenilaianGuru(
				(GrupChecklistPenilaianGuru) (grupChecklistPenilaianGuru.getSelectedItem() == null ? null
						: grupChecklistPenilaianGuru.getSelectedItem().getValue()));
		checklistPenilaianGuru.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, checklistPenilaianGuru);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ChecklistPenilaianGuru.class);

		if (order)
			criteria.addOrder(Order.asc("isi"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("isi", searchnama.getValue(), MatchMode.ANYWHERE));
		criteria.add(searchGrup.getSelectedItem() == null || searchGrup.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("grupChecklistPenilaianGuru", searchGrup.getSelectedItem().getValue()));

		criteria.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru", Criteria.LEFT_JOIN)

				.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru", "angketPenilaianGuru",
						Criteria.LEFT_JOIN)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianGuru.sekolah"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianGuru.sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianGuru.yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianGuru.yayasan", searchyayasan, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angketPenilaianGuru.program"), Restrictions.eq(
										"angketPenilaianGuru.program", searchprogram.getSelectedItem().getValue())));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ChecklistPenilaianGuru> checklistPenilaianGuru = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(checklistPenilaianGuru);
		grid.setRowRenderer(new ChecklistPenilaianGuruRenderer());
		grid.setModelCheckMobile(strset);

	}

}
