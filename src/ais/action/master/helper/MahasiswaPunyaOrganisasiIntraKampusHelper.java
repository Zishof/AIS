package ais.action.master.helper;
import ais.common.PesanFormalHelper;

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

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JabatanOrganisasiIntraKampus;
import ais.database.model.Mahasiswa;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
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

public class MahasiswaPunyaOrganisasiIntraKampusHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Mahasiswa mahasiswa;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private OrganisasiIntraKampus organisasiIntraKampus = null;
	private JabatanOrganisasiIntraKampus jabatanOrganisasiIntraKampus = null;
	private Integer tahun = null;
	private OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa;

	public MahasiswaPunyaOrganisasiIntraKampusHelper() {

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	public MahasiswaPunyaOrganisasiIntraKampusHelper(OrganisasiIntraKampus organisasiIntraKampus,
			JabatanOrganisasiIntraKampus jabatanOrganisasiIntraKampus, Integer tahun) {
		tbmuser = Common.getCurrentUser();
		this.organisasiIntraKampus = organisasiIntraKampus;
		this.jabatanOrganisasiIntraKampus = jabatanOrganisasiIntraKampus;
		this.tahun = tahun;
		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailMahasiswaRenderer() {
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) data;

			try {
				if (MahasiswaPunyaOrganisasiIntraKampusHelper.this.organisasiIntraKampusPunyaMahasiswa != null
						&& MahasiswaPunyaOrganisasiIntraKampusHelper.this.organisasiIntraKampusPunyaMahasiswa.getId()
								.equals(organisasiIntraKampusPunyaMahasiswa.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MahasiswaPunyaOrganisasiIntraKampusHelper.java:120");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(organisasiIntraKampusPunyaMahasiswa.getMahasiswa());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getNim()));
			vbox.appendChild(
					new MyLabelAgakKecil(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getJurusan().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(OrganisasiIntraKampusPunyaMahasiswa.class,
					organisasiIntraKampusPunyaMahasiswa,
					organisasiIntraKampusPunyaMahasiswa.getOrganisasiIntraKampus().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(
					organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus() == null ? ""
							: organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus().getNama()));

			boolean bolehEdit = tbmuser != null && tbmuser.getMahasiswa() != null
					&& tbmuser.getMahasiswa().getId().equals(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getId())
					&& !organisasiIntraKampusPunyaMahasiswa.getPersetujuan();

			vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, organisasiIntraKampusPunyaMahasiswa.getId(),
					OrganisasiIntraKampusPunyaMahasiswa.class.getName(), "Surat Keputusan (SK) / Surat Keterangan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit, null);
			hbox.setParent(vbox);

			if (bolehEdit) {

				final MyDatebox mulai = new MyDatebox(organisasiIntraKampusPunyaMahasiswa.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(organisasiIntraKampusPunyaMahasiswa.getSampai());
				sampai.setWidth("90%");
				final MyTextbox keterangan = new MyTextbox(organisasiIntraKampusPunyaMahasiswa.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				mulai.setParent(row);
				sampai.setParent(row);

				final Combobox combobox = new Combobox();
				Common.insertCombo(combobox, "nama", JabatanOrganisasiIntraKampus.class);
				Common.selectComboItem(combobox, organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus());
				combobox.setParent(row);
				combobox.setReadonly(true);
				combobox.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						organisasiIntraKampusPunyaMahasiswa.setMulai(mulai.getValue());
						organisasiIntraKampusPunyaMahasiswa.setSampai(sampai.getValue());
						organisasiIntraKampusPunyaMahasiswa.setKeterangan(keterangan.getValue());
						organisasiIntraKampusPunyaMahasiswa.setJabatanOrganisasiIntraKampus(
								((JabatanOrganisasiIntraKampus) (combobox.getSelectedItem() == null ? null
										: combobox.getSelectedItem().getValue())));
						Common.refreshUpdate(organisasiIntraKampusPunyaMahasiswa);
					}
				};

				combobox.addEventListener("onChange", eventListener);
				keterangan.addEventListener("onChange", eventListener);
				mulai.addEventListener("onChange", eventListener);
				sampai.addEventListener("onChange", eventListener);

				keterangan.setParent(row);

				final Hbox toolbar = new Hbox();
				toolbar.setVisible(!organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
				combobox.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
				keterangan.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
				mulai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
				sampai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
				if (tbmuser.getMahasiswa() == null) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							organisasiIntraKampusPunyaMahasiswa.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(organisasiIntraKampusPunyaMahasiswa);
							toolbar.setVisible(!organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
							combobox.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
							keterangan.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
							mulai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
							sampai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
						}
					});
				} else {
					Label label;
					(label = new Label(organisasiIntraKampusPunyaMahasiswa.getPersetujuan() == null
							|| organisasiIntraKampusPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setOrient("vertical");
				button.setVisible(!organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
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
													if (MahasiswaPunyaOrganisasiIntraKampusHelper.this.organisasiIntraKampusPunyaMahasiswa != null
															&& MahasiswaPunyaOrganisasiIntraKampusHelper.this.organisasiIntraKampusPunyaMahasiswa.getId()
																	.equals(organisasiIntraKampusPunyaMahasiswa.getId())) {
														MahasiswaPunyaOrganisasiIntraKampusHelper.this.organisasiIntraKampusPunyaMahasiswa = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/MahasiswaPunyaOrganisasiIntraKampusHelper.java:260");
													// TODO: handle exception
												}

												Common.refreshDelete(organisasiIntraKampusPunyaMahasiswa);
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
				toolbar.setParent(row);

			} else {
				new Label(organisasiIntraKampusPunyaMahasiswa.getMulai() == null ? ""
						: Common.dateFormat1.get().format(organisasiIntraKampusPunyaMahasiswa.getMulai())).setParent(row);
				new Label(organisasiIntraKampusPunyaMahasiswa.getSampai() == null ? ""
						: Common.dateFormat1.get().format(organisasiIntraKampusPunyaMahasiswa.getSampai())).setParent(row);
				new Label(organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus() == null ? ""
						: organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus().getNama())
								.setParent(row);

				new Label(organisasiIntraKampusPunyaMahasiswa.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(organisasiIntraKampusPunyaMahasiswa.getPersetujuan() == null
						|| organisasiIntraKampusPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class);

		criteria.createAlias("organisasiIntraKampus", "organisasiIntraKampus")

				.add(jabatanOrganisasiIntraKampus == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanOrganisasiIntraKampus", jabatanOrganisasiIntraKampus))

				.add(organisasiIntraKampus == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus))

				.add(tahun == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("tahun", tahun))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("organisasiIntraKampus.nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa));

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

				List<OrganisasiIntraKampusPunyaMahasiswa> myOrganisasiIntraKampusPunyaMahasiswas;

				if (organisasiIntraKampusPunyaMahasiswa != null) {
					myOrganisasiIntraKampusPunyaMahasiswas = new ArrayList<OrganisasiIntraKampusPunyaMahasiswa>();
					myOrganisasiIntraKampusPunyaMahasiswas.add(organisasiIntraKampusPunyaMahasiswa);
					myOrganisasiIntraKampusPunyaMahasiswas.addAll(initCriteria(true)
							.add(Restrictions.ne("id", organisasiIntraKampusPunyaMahasiswa.getId()))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list());
				} else {
					myOrganisasiIntraKampusPunyaMahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myOrganisasiIntraKampusPunyaMahasiswas);
				grid.setRowRenderer(new DetailMahasiswaRenderer());
				grid.setModelCheckMobile(strset);

			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(Mahasiswa mahasiswa, Component component) {
		display(mahasiswa, component, null);
	}

	public void display(final Mahasiswa mahasiswa, final Component component,
			OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.organisasiIntraKampusPunyaMahasiswa = organisasiIntraKampusPunyaMahasiswa;
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
				AmbilDataOrganisasiForOrganisasiIntraKampusHelper dataMahasiswaHelper = new AmbilDataOrganisasiForOrganisasiIntraKampusHelper(
						mahasiswa);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		if (mahasiswa != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Organisasi Mahasiswa", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakOrganisasiMahasiswa(mahasiswa);
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
				OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) objects[0];

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
							OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(organisasiIntraKampusPunyaMahasiswa.getId(), jenis);
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

				dataAddingHelper.process(row, 8, organisasiIntraKampusPunyaMahasiswa,
						OrganisasiIntraKampusPunyaMahasiswa.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				OrganisasiIntraKampusPunyaMahasiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, "id", "organisasiIntraKampus", "mahasiswa", "jabatanOrganisasiIntraKampus", "persetujuan",
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
		column.setLabel("Mahasiswa");
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
