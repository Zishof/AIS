package ais.action.master;

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
import org.json.JSONObject;
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
import org.zkoss.zul.Checkbox;
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
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AngketPenilaianDosen;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ChecklistPenilaianDosenAction extends GenericAutowireComposer implements DataCriteria, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchjurusan;
	private Combobox searchprogram;
	private Checkbox searchaktif;
	private Combobox searchfakultas;

	private Textbox nama;
	private MyDoublebox bobot;
	private Combobox grupChecklistPenilaianDosen;
	private Combobox searchGrup;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private ChecklistPenilaianDosen checklistPenilaianDosen;
	private MyToolbarbuttonConfig add;

	private Tabpanel angketUmum;

	public void onAngketUmum(Event event) {
		if (angketUmum == null) {
			return;
		}
		if (angketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(angketUmum);
			window.setContentStyle("overflow:auto;");
			MyInclude iframe = new MyInclude("/pages/master/checklist_penilaian_umum.zul");
			iframe.setParent(window);
		}
	}

	private Tabpanel parameterAngketUmum;
	private JSONObject pilihan;
	private Row rowPilihan;

	public void onParameterAngketUmum(Event event) {
		if (parameterAngketUmum == null) {
			return;
		}
		if (parameterAngketUmum.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(parameterAngketUmum);
			window.setContentStyle("overflow:auto;");
			MyInclude iframe = new MyInclude("/pages/master/parameter_tambahan_angket_umum.zul");
			iframe.setParent(window);
		}
	}

	public static String[] contents = new String[] { "id", "isi", "grupChecklistPenilaianDosen", "bobot", "aktif",
			"keterangan", "pilihan" };

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
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

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.insertCombo(grupChecklistPenilaianDosen = new Combobox(), "isi", "angketPenilaianDosen",
				GrupChecklistPenilaianDosen.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.insertComboDanSemua(searchGrup, "isi", GrupChecklistPenilaianDosen.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(ChecklistPenilaianDosen.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = new MyToolbarbuttonConfig("Upload" + Common.ukuranLabelFileUpload(),
				"/img/excel.png");
		if (upload != null) { upload.setUpload(Common.ukuranFileUpload()); }
		upload.addEventListener("onUpload", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				Media media = uploadEvent.getMedia();
				if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) {
					return;
				}
				if (media != null && media.getName() != null && media.getName().toLowerCase().endsWith("xlsx")) {

					final File file = simpanMediaKeFileSementara(media);

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

							Thread uploadThread = new Thread(new Runnable() {

								@Override
								public void run() {
									try {

									try {

										XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
										XSSFSheet sheet = workbook.getSheetAt(0);

										ClassMetadata classMetadata = HibernateUtil
												.getClassMetadata(ChecklistPenilaianDosen.class);
										Session session = HibernateUtil.currentNativeSession();

										int rowCount = (sheet.getLastRowNum() + 1);
										for (int i = 1; i < rowCount; i++) {
											try {

												Long id = Common.getSheetContentAsLong(sheet, 0, i);
												ChecklistPenilaianDosen checklistPenilaianDosen = id == null
														|| id.equals(-1L)
																? null
																: (ChecklistPenilaianDosen) session
																		.createCriteria(ChecklistPenilaianDosen.class)
																		.add(Restrictions.idEq(id)).uniqueResult();

												if (checklistPenilaianDosen == null) {
													checklistPenilaianDosen = new ChecklistPenilaianDosen();
												}

												Common.setObjectValues(classMetadata, checklistPenilaianDosen, contents,
														1, sheet, i);

												session.getTransaction().begin();
												session.saveOrUpdate(checklistPenilaianDosen);
												session.getTransaction().commit();

												label.setValue("Upload data \"" + checklistPenilaianDosen.getKode()
														+ " - " + checklistPenilaianDosen.getNama() + "\" ("
														+ Common.numberFormat.get().format(i * 100.0 / rowCount) + " %)");

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
											}

										}
									} catch (Exception e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/ChecklistPenilaianDosenAction.java:281");
									}

									HibernateUtil.closeSession();

									label.setValue("");
																	} finally {
										ais.database.hibernate.HibernateUtil.closeSession();
									}
								}
							}, "ais-upload-checklist-penilaian-dosen");
							uploadThread.setDaemon(true);
							uploadThread.start();

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

	private File simpanMediaKeFileSementara(Media media) throws Exception {
		InputStream inputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			String fileName = media == null || media.getName() == null ? "upload.xlsx" : media.getName();
			final File file = new File(Sessions.getCurrent().getWebApp().getRealPath("/temp/" + fileName));
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			inputStream = media.getStreamData();
			fileOutputStream = new FileOutputStream(file);
			byte[] buffer = new byte[8192];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, length);
			}
			fileOutputStream.flush();
			return file;
		} finally {
			if (fileOutputStream != null) {
				try {
					fileOutputStream.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	class ChecklistPenilaianDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
				final ChecklistPenilaianDosen checklistPenilaianDosen = (ChecklistPenilaianDosen) arg1;

			if (checklistPenilaianDosen.getGrupChecklistPenilaianDosen() == null) {
				checklistPenilaianDosen
						.setGrupChecklistPenilaianDosen((GrupChecklistPenilaianDosen) HibernateUtil.currentSession()
								.createCriteria(GrupChecklistPenilaianDosen.class).setMaxResults(1).uniqueResult());
			}

			RevisiHelper.createNewRevisi(ChecklistPenilaianDosen.class, checklistPenilaianDosen,
					checklistPenilaianDosen.getIsi()).setParent(arg0);
			new Label(checklistPenilaianDosen.getGrupChecklistPenilaianDosen() == null ? ""
					: checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getIsi()).setParent(arg0);
			new Label(Common.numberFormat.get().format(checklistPenilaianDosen.getBobot())).setParent(arg0);

			AngketPenilaianDosen angketPenilaianDosen = checklistPenilaianDosen.getGrupChecklistPenilaianDosen() == null
					? null
					: checklistPenilaianDosen.getGrupChecklistPenilaianDosen().getAngketPenilaianDosen();
			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getFakultas() == null ? "Semua"
					: angketPenilaianDosen.getFakultas().getNama()).setParent(arg0);
			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getJurusan() == null ? "Semua"
					: angketPenilaianDosen.getJurusan().getNama()).setParent(arg0);

			new Label(angketPenilaianDosen == null || angketPenilaianDosen.getProgram() == null
					|| angketPenilaianDosen.getProgram().trim().isEmpty() ? "Semua" : angketPenilaianDosen.getProgram())
					.setParent(arg0);

			JSONObject pilihan = new JSONObject(checklistPenilaianDosen.getPilihan());
			Hbox hbox = new Hbox();
			arg0.appendChild(hbox);
			GrupChecklistPenilaianDosen grup = checklistPenilaianDosen.getGrupChecklistPenilaianDosen();
			if (grup != null && grup.getAngketPenilaianDosen() != null) {
				for (int i = 1; i <= grup.getAngketPenilaianDosen().getJumlahPilihan(); i++) {
					Label myTextbox = new Label(pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					hbox.appendChild(myTextbox);
				}
			}

			new Label(checklistPenilaianDosen.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(checklistPenilaianDosen.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					checklistPenilaianDosen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(checklistPenilaianDosen);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, checklistPenilaianDosen, ChecklistPenilaianDosenAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new ChecklistPenilaianDosen());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(ChecklistPenilaianDosen checklistPenilaianDosen) throws Exception {
		this.checklistPenilaianDosen = checklistPenilaianDosen;
		addWindow.setTitle(checklistPenilaianDosen.getId() == null ? "Tambah Angket Penilaian Dosen" : "Ubah Angket Penilaian Dosen");
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
				nama = new Textbox(checklistPenilaianDosen.getIsi() == null ? "" : checklistPenilaianDosen.getIsi()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Angket Dosen"));
		row.appendChild(grupChecklistPenilaianDosen);
		Common.selectComboItem(grupChecklistPenilaianDosen,
				checklistPenilaianDosen.getGrupChecklistPenilaianDosen() == null ? null
						: checklistPenilaianDosen.getGrupChecklistPenilaianDosen());
		grupChecklistPenilaianDosen.setWidth("90%");
		grupChecklistPenilaianDosen.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bobot"));
		row.appendChild(bobot = new MyDoublebox(checklistPenilaianDosen.getBobot()));
		bobot.setCols(3);

		pilihan = new JSONObject(checklistPenilaianDosen.getPilihan());
		rowPilihan = new MyFormRow();
		rowPilihan.setParent(rows);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowPilihan);

				GrupChecklistPenilaianDosen grup = (GrupChecklistPenilaianDosen) (grupChecklistPenilaianDosen
						.getSelectedItem() == null ? null : grupChecklistPenilaianDosen.getSelectedItem().getValue());

				if (grup != null && grup.getAngketPenilaianDosen() != null) {
					rowPilihan.appendChild(new ais.ui.util.MyLabelConfig("Pilihan"));
					Hbox hbox = new Hbox();
					rowPilihan.appendChild(hbox);
					for (int i = 1; i <= grup.getAngketPenilaianDosen().getJumlahPilihan(); i++) {
						final MyTextbox myTextbox = new MyTextbox(
								pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
						final int index = i;
						myTextbox.setParent(hbox);
						myTextbox.setCols(10);
						myTextbox.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pilihan.put(index + "", myTextbox.getValue().trim());
							}
						});
					}
				}
			}
		};

		eventListener.onEvent(null);
		grupChecklistPenilaianDosen.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				checklistPenilaianDosen.getKeterangan() == null ? "" : checklistPenilaianDosen.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (grupChecklistPenilaianDosen.getSelectedItem() == null) {
			MyMessageboxConfig.show("Grup harus dipilih", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (checklistPenilaianDosen.getId() != null) {
			checklistPenilaianDosen = (ChecklistPenilaianDosen) session.load(ChecklistPenilaianDosen.class,
					checklistPenilaianDosen.getId());

		}

		checklistPenilaianDosen.setBobot(bobot.getValue());
		checklistPenilaianDosen.setIsi(nama.getValue());
		checklistPenilaianDosen.setGrupChecklistPenilaianDosen(
				(GrupChecklistPenilaianDosen) (grupChecklistPenilaianDosen.getSelectedItem() == null ? null
						: grupChecklistPenilaianDosen.getSelectedItem().getValue()));
		checklistPenilaianDosen.setKeterangan(keterangan.getValue());
		checklistPenilaianDosen.setPilihan(pilihan.toString());

		Common.refreshSaveOrUpdate(session, checklistPenilaianDosen);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(ChecklistPenilaianDosen.class).add(searchaktif == null || searchaktif.isChecked()
				? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
				: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("isi"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("isi", searchnama.getValue(), MatchMode.ANYWHERE));
		criteria.add(searchGrup.getSelectedItem() == null || searchGrup.getSelectedItem().getValue() == null
				? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("grupChecklistPenilaianDosen", searchGrup.getSelectedItem().getValue()));

		criteria.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen", Criteria.LEFT_JOIN)

				.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen",
						Criteria.LEFT_JOIN)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianDosen.jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("angketPenilaianDosen.fakultas", searchfakultas, false)))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						|| searchprogram.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angketPenilaianDosen.program"), Restrictions.eq(
										"angketPenilaianDosen.program", searchprogram.getSelectedItem().getValue())));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<ChecklistPenilaianDosen> checklistPenilaianDosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(checklistPenilaianDosen);
		grid.setRowRenderer(new ChecklistPenilaianDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		ChecklistPenilaianDosen checklistPenilaianDosen = (ChecklistPenilaianDosen) obj;
		init(checklistPenilaianDosen);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
