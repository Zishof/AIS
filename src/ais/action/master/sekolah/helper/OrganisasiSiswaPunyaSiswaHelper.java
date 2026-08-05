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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JabatanOrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
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

public class OrganisasiSiswaPunyaSiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private OrganisasiSiswa organisasiSiswa;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;
	private AmbilDataGuruBanbox searchguru;

	public OrganisasiSiswaPunyaSiswaHelper() {

		tbmuser = Common.getCurrentUser();
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailOrganisasiSiswaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailOrganisasiSiswaRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(OrganisasiSiswaPunyaSiswa.class, organisasiSiswaPunyaSiswa,
					organisasiSiswaPunyaSiswa.getSiswa().getNim()).setParent(row);

			new Label(organisasiSiswaPunyaSiswa.getSiswa().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, organisasiSiswaPunyaSiswa.getId(),
					OrganisasiSiswaPunyaSiswa.class.getName(), "Surat Keputusan (SK) / Surat Keterangan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			hbox.setParent(vbox);

			new Label(organisasiSiswaPunyaSiswa.getSiswa().getTahunMasuk() + "").setParent(row);

			new Label(organisasiSiswaPunyaSiswa.getSiswa().getYayasan() == null ? ""
					: organisasiSiswaPunyaSiswa.getSiswa().getYayasan().getNama() + "").setParent(row);

			new Label(organisasiSiswaPunyaSiswa.getSiswa().getSekolah() == null ? ""
					: organisasiSiswaPunyaSiswa.getSiswa().getSekolah().getNama() + "").setParent(row);

			final MyDatebox mulai = new MyDatebox(organisasiSiswaPunyaSiswa.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(organisasiSiswaPunyaSiswa.getSampai());
			sampai.setWidth("90%");
			final MyTextbox keterangan = new MyTextbox(organisasiSiswaPunyaSiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKesiswaan = new Combobox();
			Common.insertCombo(jabatanKegiatanKesiswaan, "nama", JabatanOrganisasiSiswa.class);
			Common.selectComboItem(jabatanKegiatanKesiswaan, organisasiSiswaPunyaSiswa.getJabatanOrganisasiSiswa());
			jabatanKegiatanKesiswaan.setParent(row);
			jabatanKegiatanKesiswaan.setReadonly(true);
			jabatanKegiatanKesiswaan.setWidth("97%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					organisasiSiswaPunyaSiswa.setMulai(mulai.getValue());
					organisasiSiswaPunyaSiswa.setSampai(sampai.getValue());
					organisasiSiswaPunyaSiswa.setKeterangan(keterangan.getValue());
					organisasiSiswaPunyaSiswa.setJabatanOrganisasiSiswa(
							((JabatanOrganisasiSiswa) (jabatanKegiatanKesiswaan.getSelectedItem() == null ? null
									: jabatanKegiatanKesiswaan.getSelectedItem().getValue())));
					Common.refreshUpdate(organisasiSiswaPunyaSiswa);
				}
			};

			jabatanKegiatanKesiswaan.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
			mulai.addEventListener("onChange", eventListener);
			sampai.addEventListener("onChange", eventListener);

			keterangan.setParent(row);

			jabatanKegiatanKesiswaan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
			keterangan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
			mulai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
			sampai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());

			final Hbox toolbar = new Hbox();
			toolbar.setVisible(!organisasiSiswaPunyaSiswa.getPersetujuan());

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

						jabatanKegiatanKesiswaan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
						keterangan.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
						mulai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
						sampai.setDisabled(organisasiSiswaPunyaSiswa.getPersetujuan());
					}
				});
			} else {
				Label label;
				(label = new Label(
						organisasiSiswaPunyaSiswa.getPersetujuan() == null || organisasiSiswaPunyaSiswa.getPersetujuan()
								? "Ya"
								: "Belum"))
						.setParent(row);
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

		}

	}

	public Criteria initCriteria(boolean order) {

		Guru guru = (Guru) searchguru.getAttribute("guru");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiSiswaPunyaSiswa.class);

		criteria.createAlias("siswa", "siswa")
				.add(guru != null ? Restrictions.eq("siswa.guruPembina", guru.getId()) : Restrictions.sqlRestriction("1=1"))
				.createAlias("siswa.sekolah", "sekolah")

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah.yayasan", searchyayasan, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("siswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("siswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("organisasiSiswa", organisasiSiswa));

		if (order)
			criteria.addOrder(Order.asc("siswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<OrganisasiSiswaPunyaSiswa> myOrganisasiSiswaPunyaSiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myOrganisasiSiswaPunyaSiswas);
				grid.setRowRenderer(new DetailOrganisasiSiswaRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final OrganisasiSiswa organisasiSiswa, final Component component, final MyWindow window) {
		this.organisasiSiswa = organisasiSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar siswa yang mengikuti organisasi " + organisasiSiswa.getNama()));

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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Yayasan") + " : "));
		toolbar.appendChild(searchyayasan);
		searchyayasan.setCols(10);
		searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchyayasan, organisasiSiswa.getYayasan());
		if (organisasiSiswa.getYayasan() != null) {
			searchyayasan.setDisabled(true);
		}

		toolbar.appendChild(new Label(Common.getBahasaConfig("Sekolah") + " : "));
		toolbar.appendChild(searchsekolah);
		searchsekolah.setCols(10);
		searchsekolah.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (organisasiSiswa.getSekolah() != null) {
			Yayasan selectedYayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
					|| searchyayasan.getSelectedItem().getValue() == null
					|| searchyayasan.getSelectedItem().getValue() == null ? null
							: searchyayasan.getSelectedItem().getValue());
			if (selectedYayasan != null) {
				Common.insertComboDanSemua(searchsekolah, new String[] { "nama", "kodeEpsbed" }, "jenjang",
						Sekolah.class, Restrictions.eq("yayasan", selectedYayasan));
				Common.selectComboItem(searchsekolah, organisasiSiswa.getSekolah());
				searchsekolah.setDisabled(true);
			}
		}

		toolbar.appendChild(new Label("Guru BK" + " : "));
		toolbar.appendChild(searchguru = new AmbilDataGuruBanbox());
		searchguru.setCols(10);
		searchguru.setEventListener(new EventListener() {

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

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataSiswaForOrganisasiSiswaHelper dataSiswaHelper = new AmbilDataSiswaForOrganisasiSiswaHelper(
						organisasiSiswa);
				dataSiswaHelper.display(getDataloader(), window);
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
												"delete from organisasi_intra_kampus_punya_siswa where (persetujuan is null or persetujuan = false) and organisasi_intra_kampus = "
														+ organisasiSiswa.getId())
												.executeUpdate();

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
					public void process(XSSFRow row, int index, OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa,
							String jenis) throws Exception {
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

				dataAddingHelper.process(row, 8, organisasiSiswaPunyaSiswa, OrganisasiSiswaPunyaSiswa.class.getName());

			}
		};

		String[] contents = new String[] { "id", "organisasiSiswa", "siswa", "mulai", "sampai",
				"jabatanOrganisasiSiswa", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(OrganisasiSiswaPunyaSiswa.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiSiswaPunyaSiswa.class, contents);
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
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
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
		column.setLabel("Yayasan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

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
