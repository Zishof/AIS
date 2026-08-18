package ais.action.master.sekolah.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.JabatanOrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SiswaPunyaOrganisasiSiswaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Siswa siswa;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private OrganisasiSiswa organisasiSiswa = null;
	private JabatanOrganisasiSiswa jabatanOrganisasiSiswa = null;
	private Integer tahun = null;
	private OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa;

	public SiswaPunyaOrganisasiSiswaHelper() {

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	public SiswaPunyaOrganisasiSiswaHelper(OrganisasiSiswa organisasiSiswa,
			JabatanOrganisasiSiswa jabatanOrganisasiSiswa, Integer tahun) {
		tbmuser = Common.getCurrentUser();
		this.organisasiSiswa = organisasiSiswa;
		this.jabatanOrganisasiSiswa = jabatanOrganisasiSiswa;
		this.tahun = tahun;
		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailSiswaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailSiswaRenderer() {
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) data;

			try {
				if (SiswaPunyaOrganisasiSiswaHelper.this.organisasiSiswaPunyaSiswa != null
						&& SiswaPunyaOrganisasiSiswaHelper.this.organisasiSiswaPunyaSiswa.getId()
								.equals(organisasiSiswaPunyaSiswa.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/SiswaPunyaOrganisasiSiswaHelper.java:121");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(organisasiSiswaPunyaSiswa.getSiswa());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(organisasiSiswaPunyaSiswa.getSiswa().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(organisasiSiswaPunyaSiswa.getSiswa().getNim()));
			vbox.appendChild(
					new MyLabelAgakKecil(organisasiSiswaPunyaSiswa.getSiswa().getSekolah().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(OrganisasiSiswaPunyaSiswa.class,
					organisasiSiswaPunyaSiswa,
					organisasiSiswaPunyaSiswa.getOrganisasiSiswa().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(
					organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa() == null ? ""
							: organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa().getNama()));

			boolean bolehEdit = tbmuser != null && tbmuser.getSiswa() != null
					&& tbmuser.getSiswa().getId().equals(organisasiSiswaPunyaSiswa.getSiswa().getId())
					&& !organisasiSiswaPunyaSiswa.getPersetujuan();

			vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, organisasiSiswaPunyaSiswa.getId(),
					OrganisasiSiswaPunyaSiswa.class.getName(), "Surat Keputusan (SK) / Surat Keterangan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit, null);
			hbox.setParent(vbox);

			if (bolehEdit) {

				final MyDatebox mulai = new MyDatebox(organisasiSiswaPunyaSiswa.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(organisasiSiswaPunyaSiswa.getSampai());
				sampai.setWidth("90%");
				final MyTextbox keterangan = new MyTextbox(organisasiSiswaPunyaSiswa.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				mulai.setParent(row);
				sampai.setParent(row);

				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", JabatanOrganisasiSiswa.class);
				Common.selectComboItem(combobox, organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa());
				combobox.setParent(row);
				combobox.setReadonly(true);
				combobox.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						organisasiSiswaPunyaSiswa.setMulai(mulai.getValue());
						organisasiSiswaPunyaSiswa.setSampai(sampai.getValue());
						organisasiSiswaPunyaSiswa.setKeterangan(keterangan.getValue());
						organisasiSiswaPunyaSiswa.setJabatanOrganisasiSiswa(
								((JabatanOrganisasiSiswa) (combobox.getSelectedItem() == null ? null
										: combobox.getSelectedItem().getValue())));
						Common.refreshUpdate(organisasiSiswaPunyaSiswa);
					}
				};

				combobox.addEventListener("onChange", eventListener);
				keterangan.addEventListener("onChange", eventListener);
				mulai.addEventListener("onChange", eventListener);
				sampai.addEventListener("onChange", eventListener);

				keterangan.setParent(row);

				final Hbox toolbar = new Hbox();
				toolbar.setVisible(!organisasiSiswaPunyaSiswa.getPersetujuan());
				combobox.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
				keterangan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
				mulai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
				sampai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
				if (tbmuser.getSiswa() == null) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(organisasiSiswaPunyaSiswa.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							organisasiSiswaPunyaSiswa.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(organisasiSiswaPunyaSiswa);
							toolbar.setVisible(!organisasiSiswaPunyaSiswa.getPersetujuan());
							combobox.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
							keterangan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
							mulai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
							sampai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
						}
					});
				} else {
					Label label;
					(label = new Label(organisasiSiswaPunyaSiswa.getPersetujuan() == null
							|| organisasiSiswaPunyaSiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setVisible(!organisasiSiswaPunyaSiswa.getPersetujuan());
				button.setTooltiptext("Hapus Data");
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
												
												try {
													if (SiswaPunyaOrganisasiSiswaHelper.this.organisasiSiswaPunyaSiswa != null
															&& SiswaPunyaOrganisasiSiswaHelper.this.organisasiSiswaPunyaSiswa.getId()
																	.equals(organisasiSiswaPunyaSiswa.getId())) {
														SiswaPunyaOrganisasiSiswaHelper.this.organisasiSiswaPunyaSiswa = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/SiswaPunyaOrganisasiSiswaHelper.java:261");
													// TODO: handle exception
												}

												Common.refreshDelete(organisasiSiswaPunyaSiswa);
												loadData(null);

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
				toolbar.setParent(row);

			} else {
				new Label(organisasiSiswaPunyaSiswa.getMulai() == null ? ""
						: Common.dateFormat1.get().format(organisasiSiswaPunyaSiswa.getMulai())).setParent(row);
				new Label(organisasiSiswaPunyaSiswa.getSampai() == null ? ""
						: Common.dateFormat1.get().format(organisasiSiswaPunyaSiswa.getSampai())).setParent(row);
				new Label(organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa() == null ? ""
						: organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa().getNama())
								.setParent(row);

				new Label(organisasiSiswaPunyaSiswa.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(organisasiSiswaPunyaSiswa.getPersetujuan() == null
						|| organisasiSiswaPunyaSiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiSiswaPunyaSiswa.class);

		criteria.createAlias("organisasiSiswa", "organisasiSiswa")

				.add(jabatanOrganisasiSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanOrganisasiSiswa", jabatanOrganisasiSiswa))

				.add(organisasiSiswa == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("organisasiSiswa", organisasiSiswa))

				.add(tahun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahun", tahun))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("organisasiSiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);

				List<OrganisasiSiswaPunyaSiswa> myOrganisasiSiswaPunyaSiswas;

				if (organisasiSiswaPunyaSiswa != null) {
					myOrganisasiSiswaPunyaSiswas = new ArrayList<OrganisasiSiswaPunyaSiswa>();
					myOrganisasiSiswaPunyaSiswas.add(organisasiSiswaPunyaSiswa);
					myOrganisasiSiswaPunyaSiswas.addAll(initCriteria(true)
							.add(Restrictions.ne("id", organisasiSiswaPunyaSiswa.getId()))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list());
				} else {
					myOrganisasiSiswaPunyaSiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myOrganisasiSiswaPunyaSiswas);
				grid.setRowRenderer(new DetailSiswaRenderer());
				grid.setModelCheckMobile(strset);

			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(Siswa siswa, Component component) {
		display(siswa, component, null);
	}

	public void display(final Siswa siswa, final Component component,
			OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa) {
		this.siswa = siswa;
		this.organisasiSiswaPunyaSiswa = organisasiSiswaPunyaSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(Common.tampilanScroll(component));

		final boolean mobileTampil = Common.isMobile();
		org.zkoss.zk.ui.HtmlBasedComponent toolbar;
		if (mobileTampil) {
			org.zkoss.zul.Div barMobile = new org.zkoss.zul.Div();
			barMobile.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px 4px;width:100%;box-sizing:border-box;");
			toolbar = barMobile;
		} else {
			toolbar = new Toolbar();
		}
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Organisasi", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				MyWindow window = new MyWindow();
				window.setHeight("97%");
				window.setWidth("800px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				AmbilDataOrganisasiForOrganisasiSiswaHelper dataSiswaHelper = new AmbilDataOrganisasiForOrganisasiSiswaHelper(
						siswa);
				dataSiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		if (siswa != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Organisasi Siswa", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakOrganisasiSiswa(siswa);
				}
			});
			cetak.setParent(toolbar);
		}

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) objects[0];

				XSSFRow row = (XSSFRow) objects[2];
				XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
				XSSFFont hlink_font = workbook.createFont();
				hlink_font.setUnderline(XSSFFont.U_SINGLE);
				hlink_font.setColor(new XSSFColor(Color.BLUE));

				final XSSFCellStyle hlink_style = workbook.createCellStyle();
				hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
				hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
				hlink_style.setFont(hlink_font);

				class DataAddingHelper {
					public void process(XSSFRow row, int index,
							OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(organisasiSiswaPunyaSiswa.getId(), jenis);
						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}
					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 8, organisasiSiswaPunyaSiswa,
						OrganisasiSiswaPunyaSiswa.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				OrganisasiSiswaPunyaSiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, "id", "organisasiSiswa", "siswa", "jabatanOrganisasiSiswa", "persetujuan",
				"mulai", "sampai", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		// SCROLL (pola Center->Grid->Rows->Row): grid dibungkus Borderlayout -> Center(autoscroll)
		// dgn tinggi terikat agar baris banyak / tabel lebar memunculkan scrollbar. Caption+toolbar
		// tetap di luar borderlayout (hindari North-collapse ZK5.5).
		ais.ui.util.MyBorderlayout blScroll = new ais.ui.util.MyBorderlayout();
		blScroll.setHeight("60vh");
		blScroll.setWidth("100%");
		blScroll.setStyle("min-height:280px;");
		blScroll.setParent(groupbox);
		org.zkoss.zul.Center centerScroll = new org.zkoss.zul.Center();
		centerScroll.setBorder("none");
		centerScroll.setAutoscroll(true);
		centerScroll.setParent(blScroll);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Siswa");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Organisasi");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

}
