package ais.action.master.sirs;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataTempatTidurBanbox;
import ais.action.master.sirs.util.CommonAlatMedis;
import ais.action.master.sirs.util.CommonAlatMedis.InitHarga;
import ais.action.report.format1.sirs.umum.LaporanBiayaAlatMedis;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Ruang;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.BiayaAlatMedisPerKelas;
import ais.database.model.sirs.JenisAlatMedis;
import ais.database.model.sirs.Kamar;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.TempatTidur;
import ais.ui.util.MyTextbox;

public class AlatMedisTempatTidurAction extends GenericAutowireComposer implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private org.zkoss.zul.Textbox searchkode;
	private MyTextbox searchketerangan;
	private Combobox searchjenisAlatMedis;
	private Combobox searchsatuan;

	private MyTextbox keterangan;
	private Checkbox aktif;

	private Checkbox alatMedisLab;
	private Checkbox alatMedisOperasi;
	private Checkbox alatMedisRadiologi;
	private Checkbox alatMedisVk;
	private Checkbox alatMedisRenalUnit;
	private Checkbox alatMedisGizi;

	private Combobox ruangPerawatan;
	private Combobox kamarPerawatan;
	private AmbilDataTempatTidurBanbox tempatTidur;

	private boolean edit = false;
	private boolean delete = false;

	private AlatMedis alatMedis;
	private Toolbarbutton add;
	private Combobox per;

	private Tabpanel tabHarga;
	private Tabpanel tabLayanan;

	private BiayaAlatMedisPerKelasAction biayaAlatMedisPerKelasAction;
	private LayananAlatMedisAction layananAlatMedisAction;

	private InitHarga initHarga = new InitHarga();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}
		// main(null);
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Comboitem comboitem = new Comboitem(AlatMedis.PER_JAM);
		if (comboitem != null) { comboitem.setValue(AlatMedis.PER_JAM); }
		searchsatuan.appendChild(comboitem);

		comboitem = new Comboitem(AlatMedis.PER_HARI);
		if (comboitem != null) { comboitem.setValue(AlatMedis.PER_HARI); }
		searchsatuan.appendChild(comboitem);

		comboitem = new Comboitem(AlatMedis.PER_KALI);
		if (comboitem != null) { comboitem.setValue(AlatMedis.PER_KALI); }
		searchsatuan.appendChild(comboitem);

		Common.insertCombo(searchjenisAlatMedis, "nama", JenisAlatMedis.class);

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		biayaAlatMedisPerKelasAction = new BiayaAlatMedisPerKelasAction();
		tabHarga.appendChild(biayaAlatMedisPerKelasAction);
		if (biayaAlatMedisPerKelasAction != null) { biayaAlatMedisPerKelasAction.setHeight("100%"); }
		if (biayaAlatMedisPerKelasAction != null) { biayaAlatMedisPerKelasAction.setWidth("100%"); }

		layananAlatMedisAction = new LayananAlatMedisAction();
		tabLayanan.appendChild(layananAlatMedisAction);
		if (layananAlatMedisAction != null) { layananAlatMedisAction.setHeight("100%"); }
		if (layananAlatMedisAction != null) { layananAlatMedisAction.setWidth("100%"); }
	}

	class AlatMedisRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final AlatMedis alatMedis = (AlatMedis) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setHeight("250px");
						borderlayout.setParent(detail);
						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						Grid gridBiaya = new Grid();
						gridBiaya.setParent(center);
						gridBiaya.setWidth("100%");
						gridBiaya.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(gridBiaya);

						Column column = new Column();
						column.setParent(columns);
						column.setLabel("Kelas");
						column.setWidth("30%");

						column = new Column();
						column.setParent(columns);
						column.setLabel("Biaya");
						column.setWidth("20%");

						column = new Column();
						column.setParent(columns);
						column.setLabel("Keterangan");

						class BiayaAlatMedisPerKelasRendere extends ais.ui.util.MyRowRenderer {

							@Override
							public void render(Row row, Object arg1) throws Exception {

								BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) arg1;
								new Label(biayaAlatMedisPerKelas.getKelasPerawatan() == null ? ""
										: biayaAlatMedisPerKelas.getKelasPerawatan().getNama()).setParent(row);
								new Label(biayaAlatMedisPerKelas.getBiaya() == null ? ""
										: Common.numberFormat.get().format(biayaAlatMedisPerKelas.getBiaya())).setParent(row);

								new Label(biayaAlatMedisPerKelas.getKeterangan()).setParent(row);
							}

						}

						Session session = HibernateUtil.currentSession();
						List<BiayaAlatMedisPerKelas> biayaAlatMedisPerKelas = session
								.createCriteria(BiayaAlatMedisPerKelas.class)
								.add(Restrictions.eq("alatMedis", alatMedis)).list();

						ListModel strset = new SimpleListModel(biayaAlatMedisPerKelas);
						gridBiaya.setRowRenderer(new BiayaAlatMedisPerKelasRendere());
						gridBiaya.setModel(strset);
						gridBiaya.renderAll();

					}
				}
			});

			new Label(alatMedis.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AlatMedis.class, alatMedis, alatMedis.getNama()).setParent(arg0);

			new Label(alatMedis.getPer()).setParent(arg0);

			new Html(alatMedis.getKeteranganLayanan()).setParent(arg0);
			new Label(alatMedis.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(alatMedis.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(alatMedis);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											List<BiayaAlatMedisPerKelas> biayaAlatMedisPerKelas = session
													.createCriteria(BiayaAlatMedisPerKelas.class)
													.add(Restrictions.eq("alatMedis", alatMedis)).list();

											for (BiayaAlatMedisPerKelas myBiayaAlatMedisPerKelas : biayaAlatMedisPerKelas) {
												Common.refreshDelete(session, myBiayaAlatMedisPerKelas);
											}

											Common.refreshDelete(session, alatMedis);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
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

	public void onCetakBiayaAlatMedis(Event event) throws Exception {
		LaporanBiayaAlatMedis laporanBiayaAlatMedis = new LaporanBiayaAlatMedis();
		laporanBiayaAlatMedis.setTitle("Laporan Biaya AlatMedis / Perawatan / Jasa");
		laporanBiayaAlatMedis.setClosable(true);
		laporanBiayaAlatMedis.setWidth("750px");
		laporanBiayaAlatMedis.setHeight("95%");
		laporanBiayaAlatMedis.setParent(page.getFirstRoot());
		laporanBiayaAlatMedis.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new AlatMedis());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public void onUploadBiaya(Event event) throws Exception { 
		CommonAlatMedis.onUploadBiaya(event, null, AlatMedis.JENIS_TEMPAT_TIDUR);
	}

	public void onDownloadBiaya(Event event) throws Exception {
		CommonAlatMedis.onDownloadBiaya(event, null, AlatMedis.JENIS_TEMPAT_TIDUR);
	}

	@SuppressWarnings("deprecation")
	private void init(final AlatMedis alatMedis) throws Exception {
		this.alatMedis = alatMedis;
		addWindow = new Window();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle(alatMedis.getId() == null ? "Tambah Alat Medis" : "Ubah Alat Medis");
		addWindow.setWidth("95%");
		addWindow.setHeight("90%");

		Borderlayout borderlayout = new Borderlayout();

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		East east = new East();
		east.setWidth("150px");
		east.setTitle("Untuk Layanan");
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);

		east.appendChild(new Vbox(new Component[] { alatMedisLab = new Checkbox("Laboratorium"),
				alatMedisOperasi = new Checkbox("Operasi"), alatMedisRadiologi = new Checkbox("Radiologi"),
				alatMedisVk = new Checkbox("Vk"), alatMedisRenalUnit = new Checkbox("Renal Unit"),
				alatMedisGizi = new Checkbox("Gizi") }));

		alatMedisLab.setChecked(alatMedis.getAlatMedisLab());
		alatMedisOperasi.setChecked(alatMedis.getAlatMedisOperasi());
		alatMedisRadiologi.setChecked(alatMedis.getAlatMedisRadiologi());
		alatMedisVk.setChecked(alatMedis.getAlatMedisVk());
		alatMedisRenalUnit.setChecked(alatMedis.getAlatMedisRenalUnit());
		alatMedisGizi.setChecked(alatMedis.getAlatMedisGizi());

		Grid grid = new Grid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ruang")));
		row.appendChild(ruangPerawatan = new Combobox());
		Common.insertCombo(ruangPerawatan, "nama", Ruang.class);
		Common.selectComboItem(ruangPerawatan, alatMedis.getRuang());
		ruangPerawatan.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kamar")));
		row.appendChild(kamarPerawatan = new Combobox());
		kamarPerawatan.setWidth("90%");

		EventListener myEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kamarPerawatan);
				Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class,
						Restrictions.eq("ruang", ruangPerawatan.getSelectedItem() == null ? alatMedis.getRuang()
								: ruangPerawatan.getSelectedItem().getValue()));
				Common.selectComboItem(kamarPerawatan, alatMedis.getKamar());
			}

		};

		ruangPerawatan.addEventListener("onChange", myEventListener);
		myEventListener.onEvent(null);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tempat Tidur (Bed)")));
		row.appendChild(tempatTidur = new AmbilDataTempatTidurBanbox());
		tempatTidur.setAttribute("tempatTidur", alatMedis.getTempatTidur());
		tempatTidur.setValue(alatMedis.getTempatTidur() == null ? "" : alatMedis.getTempatTidur().getNama());
		tempatTidur.setWidth("90%");

		ruangPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Ruang myRuang = (Ruang) (ruangPerawatan.getSelectedItem() == null ? null
						: ruangPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyRuang(myRuang);
			}
		});

		kamarPerawatan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Kamar myKamar = (Kamar) (kamarPerawatan.getSelectedItem() == null ? null
						: kamarPerawatan.getSelectedItem().getValue());
				tempatTidur.setMyKamar(myKamar);
				if (myKamar != null) {

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar.getRuang());
				}

			}
		});

		tempatTidur.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TempatTidur myTempatTidur = (TempatTidur) (tempatTidur.getAttribute("tempatTidur"));
				if (myTempatTidur != null) {
					Kamar myKamar = myTempatTidur.getKamar();

					Common.insertCombo(ruangPerawatan, "nama", "keterangan", Ruang.class);

					Common.selectComboItem(ruangPerawatan, myKamar == null ? null : myKamar.getRuang());

					Common.insertCombo(kamarPerawatan, "nama", "keterangan", Kamar.class,
							Restrictions.and(Restrictions.eq("ruang", myTempatTidur.getRuang()),
									Restrictions.eq("kelasPerawatan", myTempatTidur.getKelasPerawatan())));
					Common.selectComboItem(kamarPerawatan, myTempatTidur.getKamar());

				}

			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Satuan")));
		row.appendChild(per = new Combobox());

		Comboitem comboitem = new Comboitem(AlatMedis.PER_JAM);
		comboitem.setValue(AlatMedis.PER_JAM);
		per.appendChild(comboitem);

		comboitem = new Comboitem(AlatMedis.PER_HARI);
		comboitem.setValue(AlatMedis.PER_HARI);
		per.appendChild(comboitem);

		comboitem = new Comboitem(AlatMedis.PER_KALI);
		comboitem.setValue(AlatMedis.PER_KALI);
		per.appendChild(comboitem);

		Common.selectComboItem(per, alatMedis.getPer());
		per.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(alatMedis.getAktif());

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,9");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(alatMedis.getKeterangan() == null ? "" : alatMedis.getKeterangan()));
		keterangan.setWidth("98%");
		keterangan.setRows(2);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Tabbox tabbox = new Tabbox();
		tabbox.setStyle("border:0px;background: transparent;");
		tabbox.setHeight("240px");
		tabbox.setWidth("100%");
		tabbox.setParent(center);

		Tabs tabs = new Tabs();
		tabs.setStyle("border:0px;background: transparent;");
		tabs.setParent(tabbox);

		final Tab tabPenjualan = new Tab("Harga");
		tabPenjualan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initHarga.initHargaJual(alatMedis, tabpanel, this, null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new ais.ui.util.MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					biayaAlatMedisPerKelasAction.loadData(null);
					layananAlatMedisAction.loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public Boolean checkNamaAlatMedis(String nama) {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AlatMedis.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.trim()))
				.add(this.alatMedis.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.alatMedis.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	private String generateCode(String nama) {
		String mynama = nama.trim();
		String key = mynama.length() > 5 ? mynama.substring(0, 5) : mynama;

		String countKey = (String) (HibernateUtil.currentSession().createCriteria(AlatMedis.class)
				.add(Restrictions.ilike("nama", key, MatchMode.START)).setProjection(Projections.max("kode"))
				.uniqueResult());

		Integer count = 0;
		if (countKey != null) {
			try {
				countKey = countKey.toUpperCase();
				count = Integer.parseInt(countKey.substring(countKey.length() - 3, countKey.length()));
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
		++count;
		String num = "00000000000000" + count;

		return (key + num.substring(num.length() - 3, num.length())).toUpperCase();
	}

	public boolean onSave(Event event) throws Exception {

		if (per.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, satuan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan satuan pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (ruangPerawatan.getSelectedItem() == null && kamarPerawatan.getSelectedItem() == null
				&& tempatTidur.getAttribute("tempatTidur") == null) {
			MyMessageboxConfig.show("Mohon maaf, ruang, kamar, atau tempat tidur wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih salah satu ruang, kamar, atau tempat tidur yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		alatMedis.setRuang((Ruang) (ruangPerawatan.getSelectedItem() == null ? null
				: ruangPerawatan.getSelectedItem().getValue()));
		alatMedis.setKamar((Kamar) (kamarPerawatan.getSelectedItem() == null ? null
				: kamarPerawatan.getSelectedItem().getValue()));
		alatMedis.setTempatTidur((TempatTidur) tempatTidur.getAttribute("tempatTidur"));
		alatMedis.setPer((String) (per.getSelectedItem() == null ? null : per.getSelectedItem().getValue()));
		if (checkNamaAlatMedis(alatMedis.getNama())) {
			MyMessageboxConfig.show("Mohon maaf, tarif untuk ruang, kamar, atau tempat tidur tersebut sudah terdaftar sebelumnya. Langkah yang dapat dilakukan: (1) periksa kembali data tarif yang telah tersedia; (2) atau gunakan kombinasi ruang, kamar, atau tempat tidur yang berbeda.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (alatMedis.getId() != null) {
			alatMedis = (AlatMedis) session.load(AlatMedis.class, alatMedis.getId());

		}

		alatMedis.setRuang((Ruang) (ruangPerawatan.getSelectedItem() == null ? null
				: ruangPerawatan.getSelectedItem().getValue()));
		alatMedis.setKamar((Kamar) (kamarPerawatan.getSelectedItem() == null ? null
				: kamarPerawatan.getSelectedItem().getValue()));
		alatMedis.setTempatTidur((TempatTidur) tempatTidur.getAttribute("tempatTidur"));

		alatMedis.setJenis(AlatMedis.JENIS_TEMPAT_TIDUR);
		alatMedis.setAktif(aktif.isChecked());
		alatMedis.setAlatMedisGizi(alatMedisGizi.isChecked());
		alatMedis.setAlatMedisLab(alatMedisLab.isChecked());
		alatMedis.setAlatMedisOperasi(alatMedisOperasi.isChecked());
		alatMedis.setAlatMedisRadiologi(alatMedisRadiologi.isChecked());
		alatMedis.setAlatMedisRenalUnit(alatMedisRenalUnit.isChecked());
		alatMedis.setAlatMedisVk(alatMedisVk.isChecked());

		alatMedis.setKeterangan(keterangan.getValue());
		alatMedis.setSemuahargasama(initHarga.semuahargasama.isChecked());
		alatMedis.setPer((String) (per.getSelectedItem() == null ? null : per.getSelectedItem().getValue()));

		if (alatMedis.getId() != null) {
			Common.refreshUpdate(session, alatMedis);
		} else {
			alatMedis.setKode(generateCode(alatMedis.getNama()));
			session.save(alatMedis);
		}

		return initHarga.saveDetail(alatMedis, null);
	}

	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AlatMedis.class)

				.add(Restrictions.eq("jenis", AlatMedis.JENIS_TEMPAT_TIDUR))

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))

				.add((searchketerangan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchketerangan.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE)))

				.add(searchjenisAlatMedis.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisAlatMedis", searchjenisAlatMedis.getSelectedItem().getValue()))

				.add(searchsatuan.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("per", searchsatuan.getSelectedItem().getValue()));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<AlatMedis> alatMedis = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(alatMedis);
		grid.setRowRenderer(new AlatMedisRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public class BiayaAlatMedisPerKelasAction extends Window {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private List<KelasPerawatan> kelasPerawatans;
		private boolean edit = false;
		private MyTextbox kodeAlatMedisan;
		private MyTextbox nama;
		private Combobox jenisAlatMedis;

		private Grid grid;

		@SuppressWarnings("unchecked")
		public BiayaAlatMedisPerKelasAction() {
			super();
			kelasPerawatans = HibernateUtil.currentSession().createCriteria(KelasPerawatan.class)
					.addOrder(Order.asc("id")).list();
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			display();
		}

		class AlatMedisRenderer extends ais.ui.util.MyRowRenderer {

			private Session session = HibernateUtil.currentSession();

			public AlatMedisRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final AlatMedis alatMedis = (AlatMedis) data;
				new Label(alatMedis.getKode()).setParent(row);
				new Label(alatMedis.getNama()).setParent(row);

				for (final KelasPerawatan kelasPerawatan : kelasPerawatans) {

					// Grid searchgrid = new Grid();
					// searchgrid.setParent(row);
					//
					// Columns columns = new Columns();
					// columns.setParent(searchgrid);
					//
					// Column column = new Column();
					// column.setParent(columns);
					// column.setWidth("30%");
					//
					// column = new Column();
					// column.setParent(columns);
					// column.setWidth("70%");
					//
					// column = new Column();
					// column.setParent(columns);
					//
					// Rows rows = new Rows();
					// rows.setParent(searchgrid);
					//
					// final MyDoublebox doubleboxDokter = new MyDoublebox();
					// doubleboxDokter.setValue(biayaAlatMedisPerKelas == null
					// || biayaAlatMedisPerKelas.getFeeDokter() == null ? 0.0
					// : biayaAlatMedisPerKelas.getFeeDokter());
					// doubleboxDokter.setWidth("90%");
					// doubleboxDokter.setDisabled(!edit);
					//
					// Row myrow = new Row();
					// myrow.setStyle("border:0px;background: transparent;");
					// myrow.setParent(rows);
					// myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter")));
					// myrow.appendChild(doubleboxDokter);
					//
					// final MyDoublebox doubleboxMedis = new MyDoublebox();
					// doubleboxMedis.setValue(biayaAlatMedisPerKelas == null
					// || biayaAlatMedisPerKelas.getFeeMedis() == null ? 0.0
					// : biayaAlatMedisPerKelas.getFeeMedis());
					// doubleboxMedis.setWidth("90%");
					// doubleboxMedis.setDisabled(!edit);
					//
					// myrow = new Row();
					// myrow.setStyle("border:0px;background: transparent;");
					// myrow.setParent(rows);
					// myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Medis")));
					// myrow.appendChild(doubleboxMedis);
					//
					// final MyDoublebox doubleboxRS = new MyDoublebox();
					// doubleboxRS.setDisabled(!edit);
					// doubleboxRS
					// .setValue(biayaAlatMedisPerKelas == null
					// || biayaAlatMedisPerKelas.getFeeRumahsakit() == null ?
					// 0.0
					// : biayaAlatMedisPerKelas.getFeeRumahsakit());
					// doubleboxRS.setWidth("90%");
					//
					// myrow = new Row();
					// myrow.setStyle("border:0px;background: transparent;");
					// myrow.setParent(rows);
					// myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("RS")));
					// myrow.appendChild(doubleboxRS);
					//
					// final MyDoublebox doublebox = new MyDoublebox(
					// biayaAlatMedisPerKelas == null
					// || biayaAlatMedisPerKelas.getBiaya() == null ? 0.0
					// : biayaAlatMedisPerKelas.getBiaya());
					// doublebox.setDisabled(true);
					// doublebox.setWidth("90%");
					//
					// myrow = new Row();
					// myrow.setStyle("border:0px;background: transparent;");
					// myrow.setParent(rows);
					// myrow.appendChild(new Label(ais.common.Common.getBahasaConfig("Total")));
					// myrow.appendChild(doublebox);
					//
					// EventListener rubahHargaListener = new EventListener() {
					//
					// @Override
					// public void onEvent(Event arg0) throws Exception {
					//
					// Double dokter = doubleboxDokter.getValue() == null ? 0.0
					// : doubleboxDokter.getValue();
					// Double medis = doubleboxMedis.getValue() == null ? 0.0
					// : doubleboxMedis.getValue();
					// Double rs = doubleboxRS.getValue() == null ? 0.0
					// : doubleboxRS.getValue();
					//
					// Double total = dokter + medis + rs;
					// doublebox.setValue(total);
					//
					// Session session = HibernateUtil.currentSession();
					// BiayaAlatMedisPerKelas myBiayaAlatMedisPerKelas =
					// biayaAlatMedisPerKelas;
					// if (myBiayaAlatMedisPerKelas == null) {
					// myBiayaAlatMedisPerKelas = new BiayaAlatMedisPerKelas();
					// myBiayaAlatMedisPerKelas.setAlatMedis(alatMedis);
					// myBiayaAlatMedisPerKelas
					// .setKelasPerawatan(kelasPerawatan);
					// myBiayaAlatMedisPerKelas
					// .setKeterangan("Biaya alatMedis "
					// + alatMedis.getNama()
					// + " untuk kelas perawatan "
					// + kelasPerawatan.getNama());
					// }
					// myBiayaAlatMedisPerKelas.setFeeDokter(dokter);
					// myBiayaAlatMedisPerKelas.setFeeMedis(medis);
					// myBiayaAlatMedisPerKelas.setFeeRumahsakit(rs);
					// myBiayaAlatMedisPerKelas
					// .setBiaya(doublebox.getValue() == null ? 0.0
					// : doublebox.getValue());
					// Common.refreshUpdate(session, (myBiayaAlatMedisPerKelas));
					// }
					// };
					//
					// doubleboxDokter
					// .addEventListener("onChange", rubahHargaListener);
					// doubleboxMedis.addEventListener("onChange",
					// rubahHargaListener);
					// doubleboxRS.addEventListener("onChange",
					// rubahHargaListener);
					//
					// }

					final BiayaAlatMedisPerKelas biayaAlatMedisPerKelas = (BiayaAlatMedisPerKelas) session
							.createCriteria(BiayaAlatMedisPerKelas.class).add(Restrictions.eq("alatMedis", alatMedis))
							.add(Restrictions.eq("kelasPerawatan", kelasPerawatan)).setMaxResults(1).uniqueResult();

					final Label doublebox = new Label(Common.numberFormat.get()
							.format(biayaAlatMedisPerKelas == null || biayaAlatMedisPerKelas.getBiaya() == null ? 0.0
									: biayaAlatMedisPerKelas.getBiaya()));

					doublebox.setParent(row);
					// doublebox.setDisabled(!edit);
					// doublebox.addEventListener("onChange", new
					// EventListener() {
					//
					// @Override
					// public void onEvent(Event arg0) throws Exception {
					// Session session = HibernateUtil.currentSession();
					// BiayaAlatMedisPerKelas myBiayaAlatMedisPerKelas =
					// biayaAlatMedisPerKelas;
					// if (myBiayaAlatMedisPerKelas == null) {
					// myBiayaAlatMedisPerKelas = new BiayaAlatMedisPerKelas();
					// myBiayaAlatMedisPerKelas.setAlatMedis(alatMedis);
					// myBiayaAlatMedisPerKelas
					// .setKelasPerawatan(kelasPerawatan);
					// myBiayaAlatMedisPerKelas
					// .setKeterangan("Biaya alatMedis "
					// + alatMedis.getNama()
					// + " untuk kelas perawatan "
					// + kelasPerawatan.getNama());
					// }
					// myBiayaAlatMedisPerKelas
					// .setBiaya(doublebox.getValue() == null ? 0.0
					// : doublebox.getValue());
					// Common.refreshUpdate(session, (myBiayaAlatMedisPerKelas));
					// }
					// });
				}

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
				button.setTooltiptext("Rubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(alatMedis);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				button.setParent(toolbar);

				button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = HibernateUtil.currentSession();
												List<BiayaAlatMedisPerKelas> biayaAlatMedisPerKelas = session
														.createCriteria(BiayaAlatMedisPerKelas.class)
														.add(Restrictions.eq("alatMedis", alatMedis)).list();

												for (BiayaAlatMedisPerKelas myBiayaAlatMedisPerKelas : biayaAlatMedisPerKelas) {
													Common.refreshDelete(session, myBiayaAlatMedisPerKelas);
												}

												Common.refreshDelete(alatMedis);
												onSearchDefault(event);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
																, e.getMessage()));
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

		@SuppressWarnings("unchecked")
		public void loadData(Object value) {
			JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) (this.jenisAlatMedis.getSelectedItem() == null ? null
					: this.jenisAlatMedis.getSelectedItem().getValue());
			Session session = HibernateUtil.currentSession();
			List<AlatMedis> alatMediss = ConstantValues.simpleList(session.createCriteria(AlatMedis.class)
					.add(Restrictions.eq("jenis", AlatMedis.JENIS_TEMPAT_TIDUR)).addOrder(Order.asc("nama"))
					.add(jenisAlatMedis == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisAlat Medis", jenisAlatMedis))
					.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("kode", kodeAlatMedisan.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT_50), AlatMedis.class);

			ListModel strset = new SimpleListModel(alatMediss);
			grid.setRowRenderer(new AlatMedisRenderer());
			grid.setModel(strset);
			grid.renderAll();

		}

		private void display() {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(this);

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Div div = new Div();
			div.setParent(north);

			Grid searchgrid = new Grid();
			searchgrid.setParent(div);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Row row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Alat Medis")));
			row.appendChild(kodeAlatMedisan = new MyTextbox());
			kodeAlatMedisan.setWidth("90%");
			kodeAlatMedisan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Alat Medis")));
			row.appendChild(nama = new MyTextbox());
			nama.setWidth("90%");
			nama.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Alat Medis")));
			row.appendChild(jenisAlatMedis = new Combobox());
			Common.insertCombo(jenisAlatMedis, "nama", JenisAlatMedis.class);
			jenisAlatMedis.setWidth("90%");
			jenisAlatMedis.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(div);

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setParent(center);
			grid.setHeight("100%");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(25);

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kode Alat Medis");
			column.setWidth("100px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Alat Medis");
			column.setWidth("200px");

			for (KelasPerawatan kelasPerawatan : kelasPerawatans) {
				column = new Column();
				column.setParent(columns);
				column.setLabel(kelasPerawatan.getNama());
				column.setAlign("right");
			}

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("8%");

			loadData(null);
		}

	}

	public class LayananAlatMedisAction extends Window {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private boolean edit = false;
		private MyTextbox kodeAlatMedisan;
		private MyTextbox nama;
		private Combobox jenisAlatMedis;

		private Grid grid;

		public LayananAlatMedisAction() {
			super();
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			display();
		}

		class AlatMedisRenderer extends ais.ui.util.MyRowRenderer {

			public AlatMedisRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final AlatMedis alatMedis = (AlatMedis) data;
				new Label(alatMedis.getKode()).setParent(row);
				new Label(alatMedis.getNama()).setParent(row);

				final Checkbox alatMedisLab = new Checkbox();
				final Checkbox alatMedisOperasi = new Checkbox();
				final Checkbox alatMedisRadiologi = new Checkbox();
				final Checkbox alatMedisVk = new Checkbox();
				final Checkbox alatMedisRenalUnit = new Checkbox();
				final Checkbox alatMedisGizi = new Checkbox();

				alatMedisLab.setChecked(alatMedis.getAlatMedisLab());
				alatMedisOperasi.setChecked(alatMedis.getAlatMedisOperasi());
				alatMedisRadiologi.setChecked(alatMedis.getAlatMedisRadiologi());
				alatMedisVk.setChecked(alatMedis.getAlatMedisVk());
				alatMedisRenalUnit.setChecked(alatMedis.getAlatMedisRenalUnit());
				alatMedisGizi.setChecked(alatMedis.getAlatMedisGizi());

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						AlatMedis myAlatMedis = (AlatMedis) session.createCriteria(AlatMedis.class)
								.add(Restrictions.idEq(alatMedis.getId())).uniqueResult();
						myAlatMedis.setAlatMedisGizi(alatMedisGizi.isChecked());
						myAlatMedis.setAlatMedisLab(alatMedisLab.isChecked());
						myAlatMedis.setAlatMedisOperasi(alatMedisOperasi.isChecked());
						myAlatMedis.setAlatMedisRadiologi(alatMedisRadiologi.isChecked());
						myAlatMedis.setAlatMedisRenalUnit(alatMedisRenalUnit.isChecked());
						myAlatMedis.setAlatMedisVk(alatMedisVk.isChecked());

						session.update(myAlatMedis);
					}
				};

				alatMedisLab.addEventListener("onCheck", eventListener);
				alatMedisGizi.addEventListener("onCheck", eventListener);
				alatMedisOperasi.addEventListener("onCheck", eventListener);
				alatMedisRadiologi.addEventListener("onCheck", eventListener);
				alatMedisRenalUnit.addEventListener("onCheck", eventListener);
				alatMedisVk.addEventListener("onCheck", eventListener);

				alatMedisLab.setParent(row);
				alatMedisOperasi.setParent(row);
				alatMedisRadiologi.setParent(row);
				alatMedisVk.setParent(row);
				alatMedisRenalUnit.setParent(row);
				alatMedisGizi.setParent(row);

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
				button.setTooltiptext("Rubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(alatMedis);
						addWindow.setVisible(true);
						addWindow.onModal();
					}

				});
				button.setParent(toolbar);

				button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();
												List<BiayaAlatMedisPerKelas> biayaAlatMedisPerKelas = session
														.createCriteria(BiayaAlatMedisPerKelas.class)
														.add(Restrictions.eq("alatMedis", alatMedis)).list();

												for (BiayaAlatMedisPerKelas myBiayaAlatMedisPerKelas : biayaAlatMedisPerKelas) {
													Common.refreshDelete(session, myBiayaAlatMedisPerKelas);
												}

												Common.refreshDelete(session, alatMedis);
												onSearchDefault(event);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
																, e.getMessage()));
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

		@SuppressWarnings("unchecked")
		public void loadData(Object value) {
			JenisAlatMedis jenisAlatMedis = (JenisAlatMedis) (this.jenisAlatMedis.getSelectedItem() == null ? null
					: this.jenisAlatMedis.getSelectedItem().getValue());
			Session session = HibernateUtil.currentSession();
			List<AlatMedis> alatMediss = session.createCriteria(AlatMedis.class)
					.add(Restrictions.eq("jenis", AlatMedis.JENIS_TEMPAT_TIDUR)).addOrder(Order.asc("nama"))
					.add(jenisAlatMedis == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisAlat Medis", jenisAlatMedis))
					.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("kode", kodeAlatMedisan.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT_50).list();

			ListModel strset = new SimpleListModel(alatMediss);
			grid.setRowRenderer(new AlatMedisRenderer());
			grid.setModel(strset);
			grid.renderAll();

		}

		private void display() {

			Borderlayout borderlayout = new Borderlayout();
			borderlayout.setParent(this);

			North north = new North();
			north.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(north, true);

			Div div = new Div();
			div.setParent(north);

			Grid searchgrid = new Grid();
			searchgrid.setParent(div);

			Rows rows = new Rows();
			rows.setParent(searchgrid);

			Row row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Alat Medis")));
			row.appendChild(kodeAlatMedisan = new MyTextbox());
			kodeAlatMedisan.setWidth("90%");
			kodeAlatMedisan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Alat Medis")));
			row.appendChild(nama = new MyTextbox());
			nama.setWidth("90%");
			nama.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row = new Row();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Alat Medis")));
			row.appendChild(jenisAlatMedis = new Combobox());
			Common.insertCombo(jenisAlatMedis, "nama", JenisAlatMedis.class);
			jenisAlatMedis.setWidth("90%");
			jenisAlatMedis.addEventListener("onChange", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(div);

			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			grid = new Grid();
			grid.setParent(center);
			grid.setHeight("100%");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(25);

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kode Alat Medis");
			column.setWidth("100px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Alat Medis");
			column.setWidth("200px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Laboratorium");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Operasi");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Radiologi");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Vk");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Renal Unit");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Gizi");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("8%");

			loadData(null);
		}

	}

}
