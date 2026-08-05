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
import org.zkoss.zul.Intbox;
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
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.JabatanOrganisasiIntraKampus;
import ais.database.model.Jurusan;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.StatusMahasiswa;
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

public class OrganisasiIntraKampusPunyaMahasiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private OrganisasiIntraKampus organisasiIntraKampus;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;
	private AmbilDataDosenBanbox searchdosen;

	public OrganisasiIntraKampusPunyaMahasiswaHelper() {

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

	class DetailOrganisasiIntraKampusRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailOrganisasiIntraKampusRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(OrganisasiIntraKampusPunyaMahasiswa.class, organisasiIntraKampusPunyaMahasiswa,
					organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getNim()).setParent(row);

			new Label(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, organisasiIntraKampusPunyaMahasiswa.getId(),
					OrganisasiIntraKampusPunyaMahasiswa.class.getName(), "Surat Keputusan (SK) / Surat Keterangan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			hbox.setParent(vbox);

			new Label(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getTahunangkatan() + "").setParent(row);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(organisasiIntraKampusPunyaMahasiswa.getMahasiswa())
					.getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getJurusan().getFakultas().getNama() + "")
							.setParent(row);

			new Label(organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: organisasiIntraKampusPunyaMahasiswa.getMahasiswa().getJurusan().getNama() + "").setParent(row);

			final MyDatebox mulai = new MyDatebox(organisasiIntraKampusPunyaMahasiswa.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(organisasiIntraKampusPunyaMahasiswa.getSampai());
			sampai.setWidth("90%");
			final MyTextbox keterangan = new MyTextbox(organisasiIntraKampusPunyaMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKemahasiswaan = new Combobox();
			Common.insertCombo(jabatanKegiatanKemahasiswaan, "nama", JabatanOrganisasiIntraKampus.class);
			Common.selectComboItem(jabatanKegiatanKemahasiswaan,
					organisasiIntraKampusPunyaMahasiswa.getJabatanOrganisasiIntraKampus());
			jabatanKegiatanKemahasiswaan.setParent(row);
			jabatanKegiatanKemahasiswaan.setReadonly(true);
			jabatanKegiatanKemahasiswaan.setWidth("97%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					organisasiIntraKampusPunyaMahasiswa.setMulai(mulai.getValue());
					organisasiIntraKampusPunyaMahasiswa.setSampai(sampai.getValue());
					organisasiIntraKampusPunyaMahasiswa.setKeterangan(keterangan.getValue());
					organisasiIntraKampusPunyaMahasiswa.setJabatanOrganisasiIntraKampus(
							((JabatanOrganisasiIntraKampus) (jabatanKegiatanKemahasiswaan.getSelectedItem() == null
									? null : jabatanKegiatanKemahasiswaan.getSelectedItem().getValue())));
					Common.refreshUpdate(organisasiIntraKampusPunyaMahasiswa);
				}
			};

			jabatanKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
			mulai.addEventListener("onChange", eventListener);
			sampai.addEventListener("onChange", eventListener);

			keterangan.setParent(row);

			jabatanKegiatanKemahasiswaan.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
			keterangan.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
			mulai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
			sampai.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());

			final Hbox toolbar = new Hbox();
			toolbar.setVisible(!organisasiIntraKampusPunyaMahasiswa.getPersetujuan());

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

						jabatanKegiatanKemahasiswaan.setDisabled(organisasiIntraKampusPunyaMahasiswa.getPersetujuan());
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

											Common.refreshDelete(organisasiIntraKampusPunyaMahasiswa);
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

		Dosen dosen = (Dosen) searchdosen.getAttribute("dosen");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class);

		criteria.createAlias("mahasiswa", "mahasiswa").add(
				dosen != null ? Restrictions.eq("mahasiswa.dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))
				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))

				.add(Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<OrganisasiIntraKampusPunyaMahasiswa> myOrganisasiIntraKampusPunyaMahasiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myOrganisasiIntraKampusPunyaMahasiswas);
				grid.setRowRenderer(new DetailOrganisasiIntraKampusRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final OrganisasiIntraKampus organisasiIntraKampus, final Component component,
			final MyWindow window) {
		this.organisasiIntraKampus = organisasiIntraKampus;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti organisasi " + organisasiIntraKampus.getNama()));

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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa : ")));
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

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchfakultas, organisasiIntraKampus.getFakultas());
		if (organisasiIntraKampus.getFakultas() != null) {
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

		if (organisasiIntraKampus.getJurusan() != null) {
			Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
					|| searchfakultas.getSelectedItem().getValue() == null
					|| searchfakultas.getSelectedItem().getValue() == null ? null
							: searchfakultas.getSelectedItem().getValue());
			if (selectedFakultas != null) {
				Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
						Jurusan.class, Restrictions.eq("fakultas", selectedFakultas));
				Common.selectComboItem(searchjurusan, organisasiIntraKampus.getJurusan());
				searchjurusan.setDisabled(true);
			}
		}

		toolbar.appendChild(new Label("Dosen PA" + " : "));
		toolbar.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setCols(10);
		searchdosen.setEventListener(new EventListener() {

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

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForOrganisasiIntraKampusHelper dataMahasiswaHelper = new AmbilDataMahasiswaForOrganisasiIntraKampusHelper(
						organisasiIntraKampus);
				dataMahasiswaHelper.display(getDataloader(), window);
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
												"delete from organisasi_intra_kampus_punya_mahasiswa where (persetujuan is null or persetujuan = false) and organisasi_intra_kampus = "
														+ organisasiIntraKampus.getId())
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

		String[] contents = new String[] { "id", "organisasiIntraKampus", "mahasiswa", "mulai", "sampai",
				"jabatanOrganisasiIntraKampus", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				OrganisasiIntraKampusPunyaMahasiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, contents);

		MyToolbarbuttonConfig upload = Common.uploadData(this, OrganisasiIntraKampusPunyaMahasiswa.class, contents);
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
