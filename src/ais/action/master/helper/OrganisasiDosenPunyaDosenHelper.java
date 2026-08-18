package ais.action.master.helper;

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

import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JabatanOrganisasiDosen;
import ais.database.model.Jurusan;
import ais.database.model.OrganisasiDosen;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class OrganisasiDosenPunyaDosenHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private OrganisasiDosen organisasiDosen;
	private Textbox nidn;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;

	public OrganisasiDosenPunyaDosenHelper() {

		tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailOrganisasiDosenRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailOrganisasiDosenRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(OrganisasiDosenPunyaDosen.class, organisasiDosenPunyaDosen,
					organisasiDosenPunyaDosen.getDosen().getNidn()).setParent(row);

			new Label(organisasiDosenPunyaDosen.getDosen().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, organisasiDosenPunyaDosen.getId(),
					OrganisasiDosenPunyaDosen.class.getName(), "Surat Keputusan (SK) / Surat Keterangan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			hbox.setParent(vbox);

			new Label(organisasiDosenPunyaDosen.getDosen().getIkatanKerjaDosen() == null ? ""
					: organisasiDosenPunyaDosen.getDosen().getIkatanKerjaDosen().getNama()).setParent(row);

			new Label(organisasiDosenPunyaDosen.getDosen().getStatusKepegawaian() == null ? ""
					: organisasiDosenPunyaDosen.getDosen().getStatusKepegawaian().getNama()).setParent(row);

			new Label(organisasiDosenPunyaDosen.getDosen().getJurusan() == null ? ""
					: organisasiDosenPunyaDosen.getDosen().getJurusan().getFakultas().getNama() + "").setParent(row);

			new Label(organisasiDosenPunyaDosen.getDosen().getJurusan() == null ? ""
					: organisasiDosenPunyaDosen.getDosen().getJurusan().getNama() + "").setParent(row);

			final MyDatebox mulai = new MyDatebox(organisasiDosenPunyaDosen.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(organisasiDosenPunyaDosen.getSampai());
			sampai.setWidth("90%");
			final MyTextbox keterangan = new MyTextbox(organisasiDosenPunyaDosen.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKedosenan = new Combobox();
			Common.insertCombo(jabatanKegiatanKedosenan, "nama", JabatanOrganisasiDosen.class);
			Common.selectComboItem(jabatanKegiatanKedosenan, organisasiDosenPunyaDosen.getJabatanOrganisasiDosen());
			jabatanKegiatanKedosenan.setParent(row);
			jabatanKegiatanKedosenan.setReadonly(true);
			jabatanKegiatanKedosenan.setWidth("97%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					organisasiDosenPunyaDosen.setMulai(mulai.getValue());
					organisasiDosenPunyaDosen.setSampai(sampai.getValue());
					organisasiDosenPunyaDosen.setKeterangan(keterangan.getValue());
					organisasiDosenPunyaDosen.setJabatanOrganisasiDosen(
							((JabatanOrganisasiDosen) (jabatanKegiatanKedosenan.getSelectedItem() == null ? null
									: jabatanKegiatanKedosenan.getSelectedItem().getValue())));
					Common.refreshUpdate(organisasiDosenPunyaDosen);
				}
			};

			jabatanKegiatanKedosenan.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
			mulai.addEventListener("onChange", eventListener);
			sampai.addEventListener("onChange", eventListener);

			keterangan.setParent(row);

			jabatanKegiatanKedosenan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
			keterangan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
			mulai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
			sampai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());

			final Hbox toolbar = new Hbox();
			toolbar.setVisible(!organisasiDosenPunyaDosen.getPersetujuan());

			if (tbmuser.ambilDosen() == null) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(organisasiDosenPunyaDosen.getPersetujuan());
				checkbox.setParent(row);
				row.setValign("top");row.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						organisasiDosenPunyaDosen.setPersetujuan(checkbox.isChecked());
						Common.refreshSaveOrUpdate(organisasiDosenPunyaDosen);
						toolbar.setVisible(!organisasiDosenPunyaDosen.getPersetujuan());

						jabatanKegiatanKedosenan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
						keterangan.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
						mulai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
						sampai.setDisabled(organisasiDosenPunyaDosen.getPersetujuan());
					}
				});
			} else {
				Label label;
				(label = new Label(
						organisasiDosenPunyaDosen.getPersetujuan() == null || organisasiDosenPunyaDosen.getPersetujuan()
								? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
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

											Common.refreshDelete(organisasiDosenPunyaDosen);
											loadData(null);

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
			toolbar.setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiDosenPunyaDosen.class);

		criteria.createAlias("dosen", "dosen")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("dosen.fakultas", searchfakultas, false))

				.add(nidn.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("dosen.nidn", nidn.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("dosen.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("organisasiDosen", organisasiDosen));

		if (order)
			criteria.addOrder(Order.asc("dosen.nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<OrganisasiDosenPunyaDosen> myOrganisasiDosenPunyaDosens = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myOrganisasiDosenPunyaDosens);
				grid.setRowRenderer(new DetailOrganisasiDosenRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final OrganisasiDosen organisasiDosen, final Component component, final MyWindow window) {
		this.organisasiDosen = organisasiDosen;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar dosen yang mengikuti organisasi " + organisasiDosen.getNama()));

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
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIDN : ")));
		toolbar.appendChild(nidn = new Textbox());
		nidn.setCols(10);
		nidn.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchfakultas, organisasiDosen.getFakultas());
		if (organisasiDosen.getFakultas() != null) {
			searchfakultas.setDisabled(true);
		}

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (organisasiDosen.getJurusan() != null) {
			Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
					|| searchfakultas.getSelectedItem().getValue() == null
					|| searchfakultas.getSelectedItem().getValue() == null ? null
							: searchfakultas.getSelectedItem().getValue());
			if (selectedFakultas != null) {
				Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
						Jurusan.class, Restrictions.eq("fakultas", selectedFakultas));
				Common.selectComboItem(searchjurusan, organisasiDosen.getJurusan());
				searchjurusan.setDisabled(true);
			}
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Dosen", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataDosenForOrganisasiDosenHelper dataDosenHelper = new AmbilDataDosenForOrganisasiDosenHelper(
						organisasiDosen);
				dataDosenHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from organisasi_dosen_punya_dosen where (persetujuan is null or persetujuan = false) and organisasi_dosen = "
														+ organisasiDosen.getId())
												.executeUpdate();

										loadData(null);

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

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) objects[0];

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
					public void process(XSSFRow row, int index, OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen,
							String jenis) throws Exception {
						LampiranLain lam = LampiranLain.ambil(organisasiDosenPunyaDosen.getId(), jenis);

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

				dataAddingHelper.process(row, 8, organisasiDosenPunyaDosen, OrganisasiDosenPunyaDosen.class.getName());

			}
		};

		String[] contents = new String[] { "id", "organisasiDosen", "dosen", "mulai", "sampai",
				"jabatanOrganisasiDosen", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(OrganisasiDosenPunyaDosen.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiDosenPunyaDosen.class, contents);
		upload.setVisible(Common.getApakahAdmin() || Common.getApakahAdminLain());
		toolbar.appendChild(upload);

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
		column.setLabel("NIDN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jabatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

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

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
