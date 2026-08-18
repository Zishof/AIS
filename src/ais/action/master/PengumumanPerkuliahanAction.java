package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataPerkuliahanBandbox;
import ais.action.master.helper.DetailPengumumanPerkuliahanHelper;
import ais.action.master.helper.generic.AmbilDataTbmuserBanyak;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KategoriPengumuman;
import ais.database.model.PengumumanPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PengumumanPerkuliahanAction extends GenericAutowireComposer {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyWindow addWindowAttachment;
	private MyGrid grid;

	private Textbox judul;
	private MyCkEditor catatan;
	private MyDatebox tanggal;
	private MyDatebox sampai;

	private Combobox jurusan;
	private Combobox fakultas;
	private Textbox searchjudul;
	private Textbox searchisi;
	private Combobox searchjurusan;
	private Combobox searchfakultas;
	private Combobox searchTahunAjaran;

	private MyCheckboxConfig aktif;
	private MyCheckboxConfig bolehDiberiKomentar;
	private MyCheckboxConfig broadcastKeMahasiswaAktif;
	private MyCheckboxConfig broadcastKeDosen;
	private PengumumanPerkuliahan pengumumanPerkuliahan;

	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private Textbox korespondensi;
	private AmbilDataPerkuliahanBandbox perkuliahan;
	private EventListener eventListener;

	private DetailPengumumanPerkuliahanHelper detailPengumumanPerkuliahanHelper = new DetailPengumumanPerkuliahanHelper();
	private Combobox kategoriPengumuman;
	private JSONArray isiPollings;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class FakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(jurusan);
				jurusan.setSelectedItem(null);
				if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
					return;
				}

				Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));

			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		class SearchFakultasEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchjurusan);
				searchjurusan.setSelectedItem(null);
				if (searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null) {
					return;
				}

				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}

		}

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		searchfakultas.addEventListener("onChange", new SearchFakultasEventListener());

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.clear(searchjurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
			searchfakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
			searchfakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
			searchjurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
			searchjurusan.setDisabled(false);
		}

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PengumumanPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final PengumumanPerkuliahan pengumumanPerkuliahan = (PengumumanPerkuliahan) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Tabbox tabbox = new Tabbox();
						tabbox.setParent(detail);
						tabbox.setHeight("100%");
						tabbox.setWidth("100%");

						Tabs tabs = new Tabs();
						tabs.setParent(tabbox);

						MyTabConfig tab2 = new MyTabConfig("Lampiran");
						tab2.setParent(tabs);

						MyTabConfig tab1 = new MyTabConfig("Diskusi");
						tab1.setParent(tabs);

						Tabpanels tabpanels = new Tabpanels();
						tabpanels.setParent(tabbox);

						final Tabpanel tabpanel2 = new ais.ui.util.MyTabpanel();
						tabpanel2.setParent(tabpanels);
						detailPengumumanPerkuliahanHelper.displayAttachment(pengumumanPerkuliahan, tabpanel2,
								addWindowAttachment);

						Tabpanel tabpanel1 = new ais.ui.util.MyTabpanel();
						tabpanel1.setParent(tabpanels);

						detailPengumumanPerkuliahanHelper.displayDetailPengumuman(pengumumanPerkuliahan, tabpanel1);

					}

				}
			});
			new Label(pengumumanPerkuliahan.getPerkuliahan().info()
					+ (pengumumanPerkuliahan.getKategoriPengumuman() == null ? ""
							: " (" + pengumumanPerkuliahan.getKategoriPengumuman().getNama() + ")")).setParent(arg0);

			new Label(pengumumanPerkuliahan.getTanggal() == null ? ""
					: Common.dateFormat2.get().format(pengumumanPerkuliahan.getTanggal())).setParent(arg0);
			new Label(pengumumanPerkuliahan.getSampai() == null ? ""
					: Common.dateFormat2.get().format(pengumumanPerkuliahan.getSampai())).setParent(arg0);
			new Label(pengumumanPerkuliahan.getJudul()).setParent(arg0);
			new Label(pengumumanPerkuliahan.getOleh()).setParent(arg0);

			long diff = 0L;
			if (pengumumanPerkuliahan.getTanggal() != null && pengumumanPerkuliahan.getSampai() != null) {
				diff = pengumumanPerkuliahan.getSampai().getTime() - pengumumanPerkuliahan.getTanggal().getTime();
				diff = (diff / (1000 * 60 * 60 * 24));
			}
			new Label(diff + " hari").setParent(arg0);

			new Label(pengumumanPerkuliahan.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(pengumumanPerkuliahan.getBolehDiberiKomentar() ? "Ya" : "Tidak").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pengumumanPerkuliahan);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pengumumanPerkuliahan);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
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
		init(new PengumumanPerkuliahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public static void onAddExternal(Event event, EventListener eventListener,
			PengumumanPerkuliahan pengumumanPerkuliahan) throws Exception {
		PengumumanPerkuliahanAction pengumumanPerkuliahanAction = new PengumumanPerkuliahanAction();
		pengumumanPerkuliahanAction.eventListener = eventListener;
		pengumumanPerkuliahanAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(pengumumanPerkuliahanAction.addWindow);
		pengumumanPerkuliahanAction.addWindow.setHeight("95%");
		pengumumanPerkuliahanAction.addWindow.setWidth("90%");

		pengumumanPerkuliahanAction.init(pengumumanPerkuliahan);

		pengumumanPerkuliahanAction.addWindow.setVisible(true);
		pengumumanPerkuliahanAction.addWindow.onModal();
	}

	private void init(PengumumanPerkuliahan pengumumanPerkuliahan) throws Exception {
		this.pengumumanPerkuliahan = pengumumanPerkuliahan;
		Common.clear(addWindow);

		Borderlayout borderlayoutLampiran = new Borderlayout();
		borderlayoutLampiran.setParent(addWindow);
		Center centerLampiran = new Center();
		centerLampiran.setParent(borderlayoutLampiran);
		ais.ui.util.ZkCompat.setFlex(centerLampiran, true);

		MyGrid gridLampiran = new MyGrid();
		gridLampiran.setWidth("100%");
		gridLampiran.setParent(centerLampiran);
		gridLampiran.setWidth("100%");
		gridLampiran.setHeight("100%");
		gridLampiran.setSclass("fgrid");

		Rows rowsLampiran = new Rows();
		rowsLampiran.setParent(gridLampiran);

		MyFormRow rowLampiran = new MyFormRow();
		rowLampiran.setParent(rowsLampiran);

		Tabbox tabbox = new Tabbox();
		tabbox.setParent(rowLampiran);
		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabData = new MyTabConfig("Data Pengumuman");
		tabData.setParent(tabs);

		MyTabConfig tabBiodata = new MyTabConfig("Lampiran Pengumuman");
		tabBiodata.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setHeight("3000px");
		tabpanel.setParent(tabpanels);

		final Tabpanel tabpanelBiodata = new ais.ui.util.MyTabpanel();
		tabpanelBiodata.setParent(tabpanels);
		tabpanelBiodata.setHeight("3000px");
		tabBiodata.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(tabpanelBiodata);
				if (!onSave(arg0)) {
					tabData.setSelected(true);
					Common.clear(tabpanelBiodata);
					return;
				}
				detailPengumumanPerkuliahanHelper.displayAttachment(
						PengumumanPerkuliahanAction.this.pengumumanPerkuliahan, tabpanelBiodata, addWindowAttachment);

			}
		});

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(tabpanel);
		Center mycenter = new Center();
		mycenter.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(mycenter, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(mycenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("15%");
		columns.appendChild(column);
		column = new MyColumnConfig();
		columns.appendChild(column);
		grid.appendChild(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setVisible(pengumumanPerkuliahan.getPerkuliahan() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Perkuliahan"));
		row.appendChild(perkuliahan = new AmbilDataPerkuliahanBandbox());
		perkuliahan.setAttribute("perkuliahan", pengumumanPerkuliahan.getPerkuliahan());
		perkuliahan.setAttribute("myValue", pengumumanPerkuliahan.getPerkuliahan());
		perkuliahan.setValue(Common.getDeskripsiPerkuliahan(pengumumanPerkuliahan.getPerkuliahan()));
		perkuliahan.setWidth("90%");
		// perkuliahan.setConstraint("no empty");
		perkuliahan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pengumuman ini valid mulai "));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(tanggal = new MyDatebox(pengumumanPerkuliahan.getTanggal() == null
				? ais.ui.util.WaktuUtil.getDate() : pengumumanPerkuliahan.getTanggal()));
		// tanggal.setConstraint("no empty");

		hbox.appendChild(new Label(ais.common.Common.getBahasaConfig("s.d")));
		hbox.appendChild(sampai = new MyDatebox(pengumumanPerkuliahan.getSampai() == null
				? ais.ui.util.WaktuUtil.getDate() : pengumumanPerkuliahan.getSampai()));
		// sampai.setConstraint("no empty");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif / Komentar"));

		Hbox hboxKomentar = new Hbox();
		row.appendChild(hboxKomentar);

		hboxKomentar.appendChild(aktif = new MyCheckboxConfig("Pengumuman ini aktif atau ditampilkan"));
		aktif.setChecked(pengumumanPerkuliahan.getAktif());

		hboxKomentar.appendChild(bolehDiberiKomentar = new MyCheckboxConfig("Pengumuman ini boleh diberi komentar"));
		bolehDiberiKomentar.setChecked(pengumumanPerkuliahan.getBolehDiberiKomentar());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Broadcast"));

		Hbox hboxBroadcast = new Hbox();
		row.appendChild(hboxBroadcast);

		hboxBroadcast.appendChild(broadcastKeMahasiswaAktif = new MyCheckboxConfig("ke mahasiswa"));
		broadcastKeMahasiswaAktif.setChecked(pengumumanPerkuliahan.getBroadcastKeMahasiswaAktif());

		hboxBroadcast.appendChild(broadcastKeDosen = new MyCheckboxConfig("ke dosen"));
		broadcastKeDosen.setChecked(pengumumanPerkuliahan.getBroadcastKeDosen());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengumuman"));
		row.appendChild(
				judul = new Textbox(pengumumanPerkuliahan.getJudul() == null ? "" : pengumumanPerkuliahan.getJudul()));
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kategori Pengumuman"));
		row.appendChild(kategoriPengumuman = new Combobox());
		Common.insertComboDanSemua(kategoriPengumuman, new String[] { "nama" }, "keterangan", KategoriPengumuman.class,
				"== Tanpa Kategori ==", Restrictions.sqlRestriction("true"));
		Common.selectComboItem(kategoriPengumuman, pengumumanPerkuliahan.getKategoriPengumuman());
		kategoriPengumuman.setWidth("90%");
		kategoriPengumuman.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Isi Pengumuman"));
		catatan = new MyCkEditor();
		catatan.setValue(pengumumanPerkuliahan.getCatatan() == null ? "" : pengumumanPerkuliahan.getCatatan());
		catatan.setWidth("90%");
		catatan.setHeight("180px");
		row.appendChild(catatan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Polling / Jejak Pendapat"));

		isiPollings = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
		row.appendChild(PengumumanPerkuliahanAction.initIsiPolling(pengumumanPerkuliahan, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PengumumanPerkuliahan pengumumanPerkuliahan = (PengumumanPerkuliahan) arg0.getData();
				isiPollings = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
			}
		}));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Koresponden"));
		row.appendChild(korespondensi = new Textbox(pengumumanPerkuliahan.getKorespondensi()));
		korespondensi.setWidth("90%");
		korespondensi.setRows(3);

		if (korespondensi.getValue().trim().isEmpty()) {
			korespondensi.setValue(Common.getCurrentUser().getUserId());
		}

		Common.initKeterangan(rows,
				"Untuk memasukkan banyak Koresponden, masukkan username masing-masing pengguna dengan pemisah tanda koma (,)");

		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Ambil Koresponden", "/img/user_male_add.png");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tambah Koresponden"));
		row.appendChild(toolbarbutton);
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				AmbilDataTbmuserBanyak ambil = new AmbilDataTbmuserBanyak(new ArrayList<Tbmuser>());
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambil);
				ambil.setEventListener(new EventListener() {

					@SuppressWarnings("unchecked")
					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						List<Tbmuser> tbmusers = (List<Tbmuser>) arg0.getData();
						if (tbmusers != null && tbmusers.size() != 0) {
							for (Tbmuser tbmuser : tbmusers) {
								korespondensi.setValue(korespondensi.getValue() + (korespondensi.getValue().isEmpty()
										? tbmuser.getUserId() : "," + tbmuser.getUserId()));
							}
						}
					}
				});
				ambil.setWidth("850px");
				ambil.setHeight("97%");
				ambil.setVisible(true);
				ambil.onModal();
			}
		});

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutLampiran);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					eventListener.onEvent(event);
				}
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
					if (eventListener != null) {
						eventListener.onEvent(event);
					}
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

	}

	public boolean onSave(Event event) throws Exception {
		if (judul.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Judul",
					"Kolom Judul belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Judul.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
//		if (catatan.getValue().trim().equals("")) {
//			MyMessageboxConfig.show("Catatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
//					MyMessageboxConfig.INFORMATION);
//			return false;
//		}
		if (perkuliahan.getAttribute("perkuliahan") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Perkuliahan",
					"Kolom Perkuliahan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Perkuliahan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		String myoleh = "";
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null) {
			if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.getMahasiswa().getNim() + " - " + tbmuser.getMahasiswa().getNama() + " (Mahasiswa)";
			} else if (tbmuser.getMahasiswa() != null) {
				myoleh = tbmuser.ambilDosen().getNama() + " (Dosen)";
			} else {
				myoleh = tbmuser.getUserId() + " (" + tbmuser.hakAkses().getRoleName() + ")";
			}
		}

		Session session = HibernateUtil.currentSession();
		if (pengumumanPerkuliahan.getId() != null) {
			pengumumanPerkuliahan = (PengumumanPerkuliahan) session.load(PengumumanPerkuliahan.class,
					pengumumanPerkuliahan.getId());
		}
		pengumumanPerkuliahan.setKategoriPengumuman((KategoriPengumuman) (kategoriPengumuman.getSelectedItem() == null
				? null : kategoriPengumuman.getSelectedItem().getValue()));
		pengumumanPerkuliahan.setDibuatOleh(Common.getCurrentUser().getUserId());
		pengumumanPerkuliahan.setSampai(sampai.getValue());
		pengumumanPerkuliahan.setTanggal(tanggal.getValue());
		pengumumanPerkuliahan.setJudul(judul.getValue());
		pengumumanPerkuliahan.setOleh(myoleh);
		pengumumanPerkuliahan.setCatatan(catatan.getValue());
		pengumumanPerkuliahan.setPerkuliahan((Perkuliahan) perkuliahan.getAttribute("perkuliahan"));
		pengumumanPerkuliahan.setAktif(aktif.isChecked());
		pengumumanPerkuliahan.setBolehDiberiKomentar(bolehDiberiKomentar.isChecked());

		pengumumanPerkuliahan.setKorespondensi(
				korespondensi.getValue().trim().isEmpty() ? tbmuser.getUserId() : korespondensi.getValue().trim());

		pengumumanPerkuliahan.setBroadcastKeDosen(broadcastKeDosen.isChecked());
		pengumumanPerkuliahan.setBroadcastKeMahasiswaAktif(broadcastKeMahasiswaAktif.isChecked());
		pengumumanPerkuliahan.setIsiPolling(isiPollings == null ? null : isiPollings.toString());

		Common.refreshSaveOrUpdate(session, pengumumanPerkuliahan);

		TampilanPengumumanPerkuliahanAction.kirimEmailKeKorespondensi(pengumumanPerkuliahan);
		TampilanPengumumanPerkuliahanAction.broadcastEmail(pengumumanPerkuliahan);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengumumanPerkuliahan.class)
				.createAlias("perkuliahan", "perkuliahan").createAlias("perkuliahan.jurusan", "jurusan");
		if (order)
			criteria.addOrder(Order.asc("tanggal"));
		criteria.add(searchjudul.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("judul", searchjudul.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchisi.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("catatan", searchisi.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("perkuliahan.jurusan", searchjurusan, false))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("perkuliahan.tahunAjaran",
										searchTahunAjaran.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchjudul == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<PengumumanPerkuliahan> pengumumanPerkuliahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pengumumanPerkuliahan);
		grid.setRowRenderer(new PengumumanPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static String tampilPengumuman(PengumumanPerkuliahan pengumumanPerkuliahan) {
		String pengumuman = "<p align='justify'>" + pengumumanPerkuliahan.getCatatan() + "</p><br>";

		return pengumuman;
	}

	public static Grid initIsiPolling(final PengumumanPerkuliahan pengumumanPerkuliahan,
			final EventListener eventListener) throws Exception {

		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumnRef = new MyColumnConfig();
		subcolumnRef.setParent(subcolumns);
		subcolumnRef.setWidth("90%");

		MyColumnConfig subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);

		final Rows subrowsRefs = new Rows();
		subrowsRefs.setParent(subGrid);
		JSONArray isiPollings = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
		for (int i = 0; i < isiPollings.length(); i++) {
			JSONObject jsonObject = isiPollings.getJSONObject(i);
			addIsiPolling(jsonObject, pengumumanPerkuliahan, subrowsRefs, eventListener);
		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Polling", "/img/add_item.png");
		button.setTooltiptext("Tambah IsiPolling");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final MyWindow addWindow = new MyWindow("Tambah Polling", "none", true);
				addWindow.setHeight("90%");
				addWindow.setWidth("900px");
				addWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

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
				column.setWidth("15%");

				column = new MyColumnConfig();
				column.setParent(columns);

				Rows rows = new Rows();
				rows.setParent(grid);

				final Long ref = Common.randLong();

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Judul *"));
				final Textbox nama;
				row.appendChild(nama = new Textbox());
				nama.setWidth("90%");
				nama.setRows(2);

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new ais.ui.util.MyLabelConfig("Isi / Keterangan *"));
				final MyCkEditor pengarang;
				row.appendChild(pengarang = new MyCkEditor());
				pengarang.setWidth("90%");
				pengarang.setHeight("500px");

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
						addWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						if (nama.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Judul polling",
									"Kolom Judul polling belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Judul polling.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}
						if (pengarang.getValue().trim().isEmpty()) {
							PesanFormalHelper.tampilkanGagal("penyimpanan data Isi polling",
									"Kolom Isi polling belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
									new String[] {
											"Isi/pilih terlebih dahulu Isi polling.",
											"Ulangi proses penyimpanan setelah kolom tersebut terisi."
									});
							return;
						}

						addWindow.detach();

						JSONObject jsonObject = new JSONObject();

						jsonObject.put("ref", ref);
						jsonObject.put("judul", nama.getValue().trim());
						jsonObject.put("isi", pengarang.getValue().trim());

						JSONArray jsonArray = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
						jsonArray.put(jsonObject);
						pengumumanPerkuliahan.setIsiPolling(jsonArray.toString());

						if (pengumumanPerkuliahan.getId() != null) {
							Common.refreshUpdate(pengumumanPerkuliahan);
						}

						eventListener.onEvent(new Event("", null, pengumumanPerkuliahan));

						addIsiPolling(jsonObject, pengumumanPerkuliahan, subrowsRefs, eventListener);
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(addWindow);
				addWindow.onModal();
			}
		});
		button.setParent(subcolumnRef);

		return subGrid;
	}

	private static void addIsiPolling(final JSONObject jsonObject, final PengumumanPerkuliahan pengumumanPerkuliahan,
			Rows subrowsRefs, final EventListener eventListener) throws Exception {
		final Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject,"ref");
		final MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrowsRefs);
		subrow.setValign("top");subrow.setAttribute("o", jsonObject.toString());

		Groupbox vbox = new ais.ui.util.MyGroupboxStyled();
		vbox.appendChild(new MyCaptionStyled(jsonObject.getString("judul")));
		vbox.appendChild(new ais.ui.util.MyHtml(jsonObject.getString("isi")));
		subrow.appendChild(vbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
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

									JSONArray jsonArrayCopy = new JSONArray();
									JSONArray jsonArray = new JSONArray(pengumumanPerkuliahan.getIsiPolling());

									for (int ii = 0; ii < jsonArray.length(); ii++) {
										JSONObject o = jsonArray.getJSONObject(ii);
										Long refO = ais.common.CommonJSONUtil.ambilLong(o,"ref");
										if (!refO.equals(ref)) {
											jsonArrayCopy.put(o);
										}
									}

									pengumumanPerkuliahan.setIsiPolling(jsonArrayCopy.toString());

									if (pengumumanPerkuliahan.getId() != null) {
										Common.refreshUpdate(pengumumanPerkuliahan);
									}

									eventListener.onEvent(new Event("", null, pengumumanPerkuliahan));
									subrow.detach();
								}

							}
						});

			}
		});
		button.setParent(subrow);
	}

	@SuppressWarnings("unchecked")
	public static void tampilkanPolling(final PengumumanPerkuliahan pengumumanPerkuliahan, final Component vbox) {
		try {
			Common.clear(vbox);
			Tbmuser tbmuser = Common.getCurrentUser();
			JSONArray isiPollings = new JSONArray(pengumumanPerkuliahan.getIsiPolling());
			if (isiPollings.length() > 0 && tbmuser != null && tbmuser.getUserId() != null) {
				JSONObject jawabanPolling = new JSONObject(pengumumanPerkuliahan.getJawabanPolling());

				Iterator<String> jumlahPemilih = jawabanPolling.keys();
				Map<Long, Integer> jumlahs = new HashMap<Long, Integer>();
				int total = 0;
				while (jumlahPemilih.hasNext()) {
					total++;
					try {
						String key = jumlahPemilih.next();
						Long refJawabanKey = ais.common.CommonJSONUtil.ambilLong(jawabanPolling,key);
						if (jumlahs.containsKey(refJawabanKey)) {
							jumlahs.put(refJawabanKey, jumlahs.get(refJawabanKey) + 1);
						} else {
							jumlahs.put(refJawabanKey, 1);
						}

					} catch (Exception e) {
						ais.common.Common.tampilErrorJikaAdmin(e);
					}
				}

				Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
				groupbox.setParent(vbox);
				groupbox.appendChild(
						new MyCaptionStyled("POLLING / JEJAK PENDAPAT, TOTAL PEMILIH : " + Common.numberFormat.get().format(total)));

				Radiogroup radiogroup = new Radiogroup();
				radiogroup.setParent(groupbox);

				for (int i = 0; i < isiPollings.length(); i++) {

					final JSONObject jsonObject = isiPollings.getJSONObject(i);
					boolean terjawab = !jawabanPolling.isNull(tbmuser.getUserId());
					final Component vboxPolling;
					if (terjawab) {
						vboxPolling = new ais.ui.util.MyGroupboxStyled();
						Long ref = ais.common.CommonJSONUtil.ambilLong(jsonObject,"ref");
						int jumlahpemilih = jumlahs.containsKey(ref) ? jumlahs.get(ref) : 0;
						double persen = (jumlahpemilih * 100.0) / total;
						vboxPolling.appendChild(new MyCaptionStyled(jsonObject.getString("judul") + ", total pemilih : "
								+ Common.numberFormat.get().format(jumlahpemilih) + " (" + Common.numberFormat.get().format(persen)
								+ "%)"));

						Long refJawaban = ais.common.CommonJSONUtil.ambilLong(jawabanPolling,tbmuser.getUserId());

						if (refJawaban.equals(ref)) {
							((Groupbox) vboxPolling).setStyle("background:#e6fffe;");
							vboxPolling.appendChild(new MyLabelBold("Pilihan Anda"));
						}
						
						Progressmeter progressmeter = new Progressmeter((int) persen);
						vboxPolling.appendChild(progressmeter);

					} else {
						vboxPolling = new Vbox();
						final Radio radio;
						vboxPolling.appendChild(radio = new Radio(jsonObject.getString("judul")));
						radio.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Tbmuser tbmuser = Common.getCurrentUser();
								JSONObject jawabanPolling = new JSONObject(pengumumanPerkuliahan.getJawabanPolling());
								jawabanPolling.put(tbmuser.getUserId(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"ref"));
								pengumumanPerkuliahan.setJawabanPolling(jawabanPolling.toString());
								Common.refreshUpdate(pengumumanPerkuliahan);
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										tampilkanPolling(pengumumanPerkuliahan, vbox);
									}
								});
							}
						});
					}

					vboxPolling.appendChild(new ais.ui.util.MyHtml(jsonObject.getString("isi")));
					radiogroup.appendChild(vboxPolling);

				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

}
