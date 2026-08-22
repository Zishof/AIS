package ais.action.master.sirs;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
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
import ais.action.master.sirs.detail.RacikanDetailAction;
import ais.action.master.sirs.helper.AmbilDataAlatMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataItemMedisBanyak;
import ais.action.master.sirs.helper.AmbilDataRacikanBanyak;
import ais.action.master.sirs.helper.AmbilDataTindakanBanyak;
import ais.action.master.sirs.util.CommonTarifTindakan;
import ais.action.master.sirs.util.CommonTindakan;
import ais.action.master.sirs.util.CommonTindakan.InitHarga;
import ais.action.report.format1.sirs.umum.LaporanBiayaTindakan;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.listener.OnSave;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sirs.AlatMedis;
import ais.database.model.sirs.BiayaTindakanPerKelas;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.database.model.sirs.JenisTindakan;
import ais.database.model.sirs.KelasPerawatan;
import ais.database.model.sirs.PaketPerawatanDetail;
import ais.database.model.sirs.Racikan;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class TindakanAction extends GenericAutowireComposer implements OnSave {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchketerangan;
	private Combobox searchjenisTindakan;

	private MyTextbox nama;
	private MyTextbox keterangan;
	private Combobox jenisTindakan;

	private Checkbox tindakanLab;
	private Checkbox tindakanOperasi;
	private Checkbox tindakanRadiologi;
	private Checkbox tindakanVk;
	private Checkbox tindakanRenalUnit;
	private Checkbox tindakanGizi;
	private Checkbox aktif;

	private boolean edit = false;
	private boolean delete = false;

	private Tindakan tindakan;
	private Toolbarbutton add;

	private Label kode;

	private Tabpanel tabHarga;
	private Tabpanel tabLayanan;

	private BiayaTindakanPerKelasAction biayaTindakanPerKelasAction;
	private LayananTindakanAction layananTindakanAction;

	private InitHarga initHarga = new InitHarga();

	private String jenisPaket = Tindakan.JENIS_PERAWATAN_BUKAN_PAKET;

	private PaketAction paketAction = new PaketAction();

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		if (execution.getParameter("jenisPaket") != null && !execution.getParameter("jenisPaket").trim().equals("")) {
			jenisPaket = execution.getParameter("jenisPaket").trim();
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertCombo(searchjenisTindakan, "nama", JenisTindakan.class,
				Restrictions.and(Restrictions.ne("nama", ""), Restrictions.isNotNull("nama")));

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		biayaTindakanPerKelasAction = new BiayaTindakanPerKelasAction();
		tabHarga.appendChild(biayaTindakanPerKelasAction);
		if (biayaTindakanPerKelasAction != null) { biayaTindakanPerKelasAction.setHeight("100%"); }
		if (biayaTindakanPerKelasAction != null) { biayaTindakanPerKelasAction.setWidth("100%"); }

		layananTindakanAction = new LayananTindakanAction();
		tabLayanan.appendChild(layananTindakanAction);
		if (layananTindakanAction != null) { layananTindakanAction.setHeight("100%"); }
		if (layananTindakanAction != null) { layananTindakanAction.setWidth("100%"); }
	}

	class TindakanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Tindakan tindakan = (Tindakan) arg1;

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

						class BiayaTindakanPerKelasRendere extends ais.ui.util.MyRowRenderer {

							@Override
							public void render(Row row, Object arg1) throws Exception {

								BiayaTindakanPerKelas biayaTindakanPerKelas = (BiayaTindakanPerKelas) arg1;
								new Label(biayaTindakanPerKelas.getKelasPerawatan() == null ? ""
										: biayaTindakanPerKelas.getKelasPerawatan().getNama()).setParent(row);
								new Label(biayaTindakanPerKelas.getBiaya() == null ? ""
										: Common.numberFormat.get().format(biayaTindakanPerKelas.getBiaya())).setParent(row);

								new Label(biayaTindakanPerKelas.getKeterangan()).setParent(row);
							}

						}

						Session session = HibernateUtil.currentSession();
						List<BiayaTindakanPerKelas> biayaTindakanPerKelas = session
								.createCriteria(BiayaTindakanPerKelas.class).add(Restrictions.eq("tindakan", tindakan))
								.list();

						ListModel strset = new SimpleListModel(biayaTindakanPerKelas);
						gridBiaya.setRowRenderer(new BiayaTindakanPerKelasRendere());
						gridBiaya.setModel(strset);
						gridBiaya.renderAll();

					}
				}
			});

			new Label(tindakan.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Tindakan.class, tindakan, tindakan.getNama()).setParent(arg0);
			new Label(tindakan.getJenisTindakan() == null ? "" : tindakan.getJenisTindakan().getNama()).setParent(arg0);

			new Html(tindakan.getKeteranganLayanan()).setParent(arg0);
			new Label(tindakan.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			new Label(tindakan.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(tindakan);
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
					MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											List<BiayaTindakanPerKelas> biayaTindakanPerKelas = session
													.createCriteria(BiayaTindakanPerKelas.class)
													.add(Restrictions.eq("tindakan", tindakan)).list();

											for (BiayaTindakanPerKelas myBiayaTindakanPerKelas : biayaTindakanPerKelas) {
												Common.refreshDelete(session, myBiayaTindakanPerKelas);
											}

											Common.refreshDelete(session, tindakan);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
															e.getMessage()));
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onCetakBiayaTindakan(Event event) throws Exception {
		LaporanBiayaTindakan laporanBiayaTindakan = new LaporanBiayaTindakan();
		laporanBiayaTindakan.setTitle("Laporan Biaya Tindakan / Perawatan / Jasa");
		laporanBiayaTindakan.setClosable(true);
		laporanBiayaTindakan.setWidth("750px");
		laporanBiayaTindakan.setHeight("95%");
		laporanBiayaTindakan.setParent(page.getFirstRoot());
		laporanBiayaTindakan.onModal();
	} 

	public void onUploadBiaya(Event event) throws Exception {
		CommonTindakan.onUploadBiaya(event, null);
	}

	public void onDownloadBiaya(Event event) throws Exception {
		CommonTindakan.onDownloadBiaya(event, null);
	}

	public void onAdd(Event event) throws Exception {
		init(new Tindakan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void generateCode(String nama) {
		String mynama = nama.trim();
		String key = mynama.length() > 5 ? mynama.substring(0, 5) : mynama;

		String countKey = (String) (HibernateUtil.currentSession().createCriteria(Tindakan.class)
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

		kode.setValue((key + num.substring(num.length() - 3, num.length())).toUpperCase());
	}

	@SuppressWarnings("deprecation")
	private void init(final Tindakan tindakan) throws Exception {
		this.tindakan = tindakan;
		addWindow = new Window();
		page.getFirstRoot().appendChild(addWindow);
		addWindow.setTitle(tindakan.getId() == null ? "Tambah Tindakan" : "Ubah Tindakan");
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

		east.appendChild(new Vbox(
				new Component[] { tindakanLab = new Checkbox("Laboratorium"), tindakanOperasi = new Checkbox("Operasi"),
						tindakanRadiologi = new Checkbox("Radiologi"), tindakanVk = new Checkbox("Vk"),
						tindakanRenalUnit = new Checkbox("Renal Unit"), tindakanGizi = new Checkbox("Gizi") }));

		tindakanLab.setChecked(tindakan.getTindakanLab());
		tindakanOperasi.setChecked(tindakan.getTindakanOperasi());
		tindakanRadiologi.setChecked(tindakan.getTindakanRadiologi());
		tindakanVk.setChecked(tindakan.getTindakanVk());
		tindakanRenalUnit.setChecked(tindakan.getTindakanRenalUnit());
		tindakanGizi.setChecked(tindakan.getTindakanGizi());

		Grid grid = new Grid();
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode ")));
		row.appendChild(kode = new Label(tindakan.getKode() == null ? "" : tindakan.getKode()));

		if (kode.getValue().trim().equals("") && !nama.getValue().trim().equals("")) {
			generateCode(nama.getValue());
		}

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama ")));
		row.appendChild(nama = new MyTextbox(tindakan.getNama() == null ? "" : tindakan.getNama()));
		nama.setWidth("90%");

		nama.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String mynama = nama.getValue().trim();
				if (mynama.trim().equals("")) {
					MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan kolom Nama; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					nama.focus();
					return;
				}
				generateCode(nama.getValue());
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis ")));
		row.appendChild(jenisTindakan = new Combobox());
		Common.insertCombo(jenisTindakan, "nama", JenisTindakan.class);
		Common.selectComboItem(jenisTindakan, tindakan.getJenisTindakan());
		jenisTindakan.setWidth("90%");

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Aktif")));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(tindakan.getAktif());

		row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,7");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(tindakan.getKeterangan() == null ? "" : tindakan.getKeterangan()));
		keterangan.setWidth("99%");
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

		if (this.jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET)) {
			Tab tabPaket = new Tab("Daftar Paket");
			tabPaket.setParent(tabs);
		}

		final Tab tabPenjualan = new Tab("Harga");
		tabPenjualan.setParent(tabs);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setStyle("border:0px;background: transparent;");
		tabpanels.setParent(tabbox);

		// Paket
		if (this.jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET)) {
			final Tabpanel tabpanelPaket = new ais.ui.util.MyTabpanel();
			tabpanelPaket.setParent(tabpanels);
			tabpanelPaket.appendChild(paketAction.display());
		}

		Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
		tabpanel.setParent(tabpanels);

		initHarga.initHargaJual(tindakan, tabpanel, jenisPaket, this, null);

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
					biayaTindakanPerKelasAction.loadData(null);
					layananTindakanAction.loadData(null);
					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu melengkapi kolom Nama Tindakan terlebih dahulu karena kolom ini wajib diisi. Langkah yang dapat dilakukan: (1) isikan kolom Nama Tindakan; (2) lanjutkan kembali proses penyimpanan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tindakan.getId() != null) {
			tindakan = (Tindakan) session.load(Tindakan.class, tindakan.getId());

		}

		tindakan.setAktif(aktif.isChecked());

		tindakan.setTindakanGizi(tindakanGizi.isChecked());
		tindakan.setTindakanLab(tindakanLab.isChecked());
		tindakan.setTindakanOperasi(tindakanOperasi.isChecked());
		tindakan.setTindakanRadiologi(tindakanRadiologi.isChecked());
		tindakan.setTindakanRenalUnit(tindakanRenalUnit.isChecked());
		tindakan.setTindakanVk(tindakanVk.isChecked());

		tindakan.setJenisTindakan((JenisTindakan) (jenisTindakan.getSelectedItem() == null ? null
				: jenisTindakan.getSelectedItem().getValue()));
		tindakan.setNama(nama.getValue());
		tindakan.setKeterangan(keterangan.getValue());
		tindakan.setSemuahargasama(initHarga.semuahargasama.isChecked());

		tindakan.setJenisPaket(jenisPaket);

		if (tindakan.getId() != null) {
			Common.refreshUpdate(session, tindakan);
		} else {
			generateCode(tindakan.getNama());
			tindakan.setKode(kode.getValue().trim());
			session.save(tindakan);
		}

		return initHarga.saveDetail(tindakan, null);
	}

	private Criteria initCriteria(boolean order) {

		System.out.println("jenisPaket = " + jenisPaket + ", kode = " + searchkode.getValue() + ", nama = "
				+ searchnama.getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Tindakan.class)

				.add(jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? Restrictions.eq("jenisPaket", jenisPaket)
						: Restrictions.or(Restrictions.eq("jenisPaket", jenisPaket), Restrictions.isNull("jenisPaket")))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchkode.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)))

				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE)))

				.add((searchketerangan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchketerangan.getValue().trim().equals("") ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE)))

				.add(searchjenisTindakan.getSelectedItem() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jenisTindakan", searchjenisTindakan.getSelectedItem().getValue()))

		;

		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Tindakan> tindakan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(tindakan);
		grid.setRowRenderer(new TindakanRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	public class BiayaTindakanPerKelasAction extends Window {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private List<KelasPerawatan> kelasPerawatans;
		private boolean edit = false;
		private MyTextbox kodeTindakanan;
		private MyTextbox nama;
		private Combobox jenisTindakan;

		private Grid grid;

		@SuppressWarnings("unchecked")
		public BiayaTindakanPerKelasAction() {
			super();
			kelasPerawatans = HibernateUtil.currentSession().createCriteria(KelasPerawatan.class)
					.addOrder(Order.asc("id")).list();
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			display();
		}

		class TindakanRenderer extends ais.ui.util.MyRowRenderer {

			public TindakanRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final Tindakan tindakan = (Tindakan) data;
				new Label(tindakan.getKode()).setParent(row);
				new Label(tindakan.getNama()).setParent(row);

				for (final KelasPerawatan kelasPerawatan : kelasPerawatans) {

					final BiayaTindakanPerKelas biayaTindakanPerKelas = CommonTarifTindakan
							.getBiayaTindakanPerKelas(tindakan, kelasPerawatan);

					final Label doublebox = new Label(Common.numberFormat.get()
							.format(biayaTindakanPerKelas == null || biayaTindakanPerKelas.getBiaya() == null ? 0.0
									: biayaTindakanPerKelas.getBiaya()));

					doublebox.setParent(row);

				}

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
				button.setTooltiptext("Rubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(tindakan);
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
						MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();
												List<BiayaTindakanPerKelas> biayaTindakanPerKelas = session
														.createCriteria(BiayaTindakanPerKelas.class)
														.add(Restrictions.eq("tindakan", tindakan)).list();

												for (BiayaTindakanPerKelas myBiayaTindakanPerKelas : biayaTindakanPerKelas) {
													Common.refreshDelete(session, myBiayaTindakanPerKelas);
												}

												Common.refreshDelete(session, tindakan);
												onSearchDefault(event);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
																e.getMessage()));
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				ais.ui.util.MenuAksiBaris.pasang(toolbar);
				toolbar.setParent(row);

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) {
			JenisTindakan jenisTindakan = (JenisTindakan) (this.jenisTindakan.getSelectedItem() == null ? null
					: this.jenisTindakan.getSelectedItem().getValue());
			Session session = HibernateUtil.currentSession();
			List<Tindakan> tindakans = session.createCriteria(Tindakan.class)

					.add(jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? Restrictions.eq("jenisPaket", jenisPaket)
							: Restrictions.or(Restrictions.eq("jenisPaket", jenisPaket),
									Restrictions.isNull("jenisPaket")))

					.addOrder(Order.asc("nama"))
					.add(jenisTindakan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisTindakan", jenisTindakan))
					.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("kode", kodeTindakanan.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT_50).list();

			ListModel strset = new SimpleListModel(tindakans);
			grid.setRowRenderer(new TindakanRenderer());
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
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Tindakan")));
			row.appendChild(kodeTindakanan = new MyTextbox());
			kodeTindakanan.setWidth("90%");
			kodeTindakanan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tindakan")));
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
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Tindakan")));
			row.appendChild(jenisTindakan = new Combobox());
			Common.insertCombo(jenisTindakan, "nama", JenisTindakan.class);
			jenisTindakan.setWidth("90%");
			jenisTindakan.addEventListener("onChange", new EventListener() {
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

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kode Tindakan");
			column.setWidth("100px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Tindakan");
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

	public class LayananTindakanAction extends Window {

		/**
		 * 
		 */
		private static final long serialVersionUID = 5086031585928643232L;

		private boolean edit = false;
		private MyTextbox kodeTindakanan;
		private MyTextbox nama;
		private Combobox jenisTindakan;

		private Grid grid;

		public LayananTindakanAction() {
			super();
			edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
			display();
		}

		class TindakanRenderer extends ais.ui.util.MyRowRenderer {

			public TindakanRenderer() {

			}

			@Override
			public void render(final Row row, Object data) throws Exception {row.setValign("top");
				final Tindakan tindakan = (Tindakan) data;
				new Label(tindakan.getKode()).setParent(row);
				new Label(tindakan.getNama()).setParent(row);

				final Checkbox tindakanLab = new Checkbox();
				final Checkbox tindakanOperasi = new Checkbox();
				final Checkbox tindakanRadiologi = new Checkbox();
				final Checkbox tindakanVk = new Checkbox();
				final Checkbox tindakanRenalUnit = new Checkbox();
				final Checkbox tindakanGizi = new Checkbox();

				tindakanLab.setChecked(tindakan.getTindakanLab());
				tindakanOperasi.setChecked(tindakan.getTindakanOperasi());
				tindakanRadiologi.setChecked(tindakan.getTindakanRadiologi());
				tindakanVk.setChecked(tindakan.getTindakanVk());
				tindakanRenalUnit.setChecked(tindakan.getTindakanRenalUnit());
				tindakanGizi.setChecked(tindakan.getTindakanGizi());

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						Tindakan myTindakan = (Tindakan) session.createCriteria(Tindakan.class)
								.add(Restrictions.idEq(tindakan.getId())).uniqueResult();
						myTindakan.setTindakanGizi(tindakanGizi.isChecked());
						myTindakan.setTindakanLab(tindakanLab.isChecked());
						myTindakan.setTindakanOperasi(tindakanOperasi.isChecked());
						myTindakan.setTindakanRadiologi(tindakanRadiologi.isChecked());
						myTindakan.setTindakanRenalUnit(tindakanRenalUnit.isChecked());
						myTindakan.setTindakanVk(tindakanVk.isChecked());

						session.update(myTindakan);
					}
				};

				tindakanLab.addEventListener("onCheck", eventListener);
				tindakanGizi.addEventListener("onCheck", eventListener);
				tindakanOperasi.addEventListener("onCheck", eventListener);
				tindakanRadiologi.addEventListener("onCheck", eventListener);
				tindakanRenalUnit.addEventListener("onCheck", eventListener);
				tindakanVk.addEventListener("onCheck", eventListener);

				tindakanLab.setParent(row);
				tindakanOperasi.setParent(row);
				tindakanRadiologi.setParent(row);
				tindakanVk.setParent(row);
				tindakanRenalUnit.setParent(row);
				tindakanGizi.setParent(row);

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
				button.setTooltiptext("Rubah Data");
				button.setVisible(edit);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(tindakan);
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
						MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@SuppressWarnings("unchecked")
									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();
												List<BiayaTindakanPerKelas> biayaTindakanPerKelas = session
														.createCriteria(BiayaTindakanPerKelas.class)
														.add(Restrictions.eq("tindakan", tindakan)).list();

												for (BiayaTindakanPerKelas myBiayaTindakanPerKelas : biayaTindakanPerKelas) {
													Common.refreshDelete(session, myBiayaTindakanPerKelas);
												}

												Common.refreshDelete(session, tindakan);
												onSearchDefault(event);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
																e.getMessage()));
											}

										}

									}
								});

					}
				});
				button.setParent(toolbar);
				ais.ui.util.MenuAksiBaris.pasang(toolbar);
				toolbar.setParent(row);

			}
		}

		@SuppressWarnings("unchecked")
		public void loadData(Object value) {
			JenisTindakan jenisTindakan = (JenisTindakan) (this.jenisTindakan.getSelectedItem() == null ? null
					: this.jenisTindakan.getSelectedItem().getValue());
			Session session = HibernateUtil.currentSession();
			List<Tindakan> tindakans = session.createCriteria(Tindakan.class)

					.add(jenisPaket.equals(Tindakan.JENIS_PERAWATAN_PAKET) ? Restrictions.eq("jenisPaket", jenisPaket)
							: Restrictions.or(Restrictions.eq("jenisPaket", jenisPaket),
									Restrictions.isNull("jenisPaket")))

					.addOrder(Order.asc("nama"))
					.add(jenisTindakan == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("jenisTindakan", jenisTindakan))
					.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
					.add(Restrictions.ilike("kode", kodeTindakanan.getValue().trim(), MatchMode.ANYWHERE))
					.setMaxResults(Common.MAX_RESULT_50).list();

			ListModel strset = new SimpleListModel(tindakans);
			grid.setRowRenderer(new TindakanRenderer());
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
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Tindakan")));
			row.appendChild(kodeTindakanan = new MyTextbox());
			kodeTindakanan.setWidth("90%");
			kodeTindakanan.addEventListener("onOK", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});

			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Tindakan")));
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
			row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Tindakan")));
			row.appendChild(jenisTindakan = new Combobox());
			Common.insertCombo(jenisTindakan, "nama", JenisTindakan.class);
			jenisTindakan.setWidth("90%");
			jenisTindakan.addEventListener("onChange", new EventListener() {
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

			Columns columns = new Columns();
			columns.setParent(grid);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("Kode Tindakan");
			column.setWidth("100px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama Tindakan");
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

	public class PaketAction {

		private Grid gridItem;
		private Paging paging;

		public PaketAction() {
			paging = new Paging();
			Common.initPaging(paging, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});
		}

		public Borderlayout init() {
			return display();
		}

		public Borderlayout display() {

			Borderlayout borderlayout = new Borderlayout();

			North north = new North();
			ais.ui.util.ZkCompat.setFlex(north, true);
			north.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("25px");
			toolbar.setParent(north);
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Obat", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!TindakanAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<ItemMedis> items = ConstantValues.simpleList(
							session.createCriteria(PaketPerawatanDetail.class)
									.setProjection(Projections.groupProperty("item.id"))
									.add(Restrictions.eq("paketPerawatan", TindakanAction.this.tindakan)),
							ItemMedis.class, false);

					AmbilDataItemMedisBanyak ambilDataItemBanyak = new AmbilDataItemMedisBanyak(items,
							new JenisItemMedis(JenisItemMedis.OBAT));
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<ItemMedis> items = (List<ItemMedis>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (ItemMedis item : items) {
								PaketPerawatanDetail paketPerawatanDetail = new PaketPerawatanDetail();
								paketPerawatanDetail.setItem(item);
								paketPerawatanDetail.setJumlah(1.0);
								paketPerawatanDetail.setKeterangan("");
								paketPerawatanDetail.setPaketPerawatan(TindakanAction.this.tindakan);
								session.save(paketPerawatanDetail);

							}

							loadData(null);
						}
					});
					ambilDataItemBanyak.setWidth("750px");
					ambilDataItemBanyak.setHeight("97%");
					ambilDataItemBanyak.setVisible(true);
					ambilDataItemBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("Racikan", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!TindakanAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<Racikan> racikans = session.createCriteria(PaketPerawatanDetail.class)
							.setProjection(Projections.groupProperty("racikan"))
							.add(Restrictions.eq("paketPerawatan", TindakanAction.this.tindakan)).list();

					AmbilDataRacikanBanyak ambilDataRacikanBanyak = new AmbilDataRacikanBanyak(racikans);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataRacikanBanyak);
					ambilDataRacikanBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<Racikan> racikans = (List<Racikan>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (Racikan racikan : racikans) {
								PaketPerawatanDetail paketPerawatanDetail = new PaketPerawatanDetail();
								paketPerawatanDetail.setRacikan(racikan);
								paketPerawatanDetail.setJumlah(1.0);
								paketPerawatanDetail.setKeterangan("");
								paketPerawatanDetail.setPaketPerawatan(TindakanAction.this.tindakan);
								session.save(paketPerawatanDetail);

							}

							loadData(null);
						}
					});
					ambilDataRacikanBanyak.setWidth("750px");
					ambilDataRacikanBanyak.setHeight("97%");
					ambilDataRacikanBanyak.setVisible(true);
					ambilDataRacikanBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("Perawatan", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!TindakanAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<Tindakan> tindakans = session.createCriteria(PaketPerawatanDetail.class)
							.setProjection(Projections.groupProperty("tindakan"))
							.add(Restrictions.eq("paketPerawatan", TindakanAction.this.tindakan)).list();

					AmbilDataTindakanBanyak ambilDataTindakanBanyak = new AmbilDataTindakanBanyak(tindakans);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataTindakanBanyak);
					ambilDataTindakanBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<Tindakan> tindakans = (List<Tindakan>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (Tindakan tindakan : tindakans) {
								PaketPerawatanDetail paketPerawatanDetail = new PaketPerawatanDetail();
								paketPerawatanDetail.setTindakan(tindakan);
								paketPerawatanDetail.setJumlah(1.0);
								paketPerawatanDetail.setKeterangan("");
								paketPerawatanDetail.setPaketPerawatan(TindakanAction.this.tindakan);
								session.save(paketPerawatanDetail);

							}

							loadData(null);
						}
					});
					ambilDataTindakanBanyak.setWidth("750px");
					ambilDataTindakanBanyak.setHeight("97%");
					ambilDataTindakanBanyak.setVisible(true);
					ambilDataTindakanBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("Alkes", "/img/add_item.png");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!TindakanAction.this.onSave(event)) {
						return;
					}

					Session session = HibernateUtil.currentSession();

					List<AlatMedis> alatMediss = session.createCriteria(PaketPerawatanDetail.class)
							.setProjection(Projections.groupProperty("alatMedis"))
							.add(Restrictions.eq("paketPerawatan", TindakanAction.this.tindakan)).list();

					AmbilDataAlatMedisBanyak ambilDataAlatMedisBanyak = new AmbilDataAlatMedisBanyak(alatMediss,
							AlatMedis.JENIS_UMUM);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
							.appendChild(ambilDataAlatMedisBanyak);
					ambilDataAlatMedisBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							List<AlatMedis> alatMediss = (List<AlatMedis>) arg0.getData();

							Session session = HibernateUtil.currentSession();
							for (AlatMedis alatMedis : alatMediss) {
								PaketPerawatanDetail paketPerawatanDetail = new PaketPerawatanDetail();
								paketPerawatanDetail.setAlatMedis(alatMedis);
								paketPerawatanDetail.setJumlah(1.0);
								paketPerawatanDetail.setKeterangan("");
								paketPerawatanDetail.setPaketPerawatan(TindakanAction.this.tindakan);
								session.save(paketPerawatanDetail);

							}

							loadData(null);
						}
					});
					ambilDataAlatMedisBanyak.setWidth("750px");
					ambilDataAlatMedisBanyak.setHeight("97%");
					ambilDataAlatMedisBanyak.setVisible(true);
					ambilDataAlatMedisBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(new Space());
			toolbar.appendChild(new Space());
			toolbar.appendChild(new Space());
			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode:")));
			toolbar.appendChild(kode = new MyTextbox());
			kode.setWidth("80px");
			kode.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama:")));
			toolbar.appendChild(nama = new MyTextbox());
			nama.setWidth("80px");
			nama.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			toolbar.appendChild(isItem = new Checkbox("Obat saja"));
			isItem.setChecked(true);
			isItem.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			toolbar.appendChild(isRacikan = new Checkbox("Racikan saja"));
			isRacikan.setChecked(true);
			isRacikan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			toolbar.appendChild(isTindakan = new Checkbox("Perawatan saja"));
			isTindakan.setChecked(true);
			isTindakan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			toolbar.appendChild(isAlatMedis = new Checkbox("Alkes saja"));
			isAlatMedis.setChecked(true);
			isAlatMedis.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			Toolbarbutton search;
			toolbar.appendChild(search = new ais.ui.util.MyToolbarbuttonConfig("", "/img/search.gif"));
			search.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});

			Center center = new Center();
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			gridItem = new Grid();
			gridItem.setMold("paging");
			gridItem.setPageSize(25);
			gridItem.setParent(center);

			Columns columns = new Columns();

			columns.setParent(gridItem);

			Column column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Kode");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Nama");
			column.setWidth("20%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Jenis");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Qty");
			column.setAlign("right");
			column.setWidth("10%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Satuan");
			column.setWidth("15%");

			column = new Column();
			column.setParent(columns);
			column.setLabel("Keterangan");

			column = new Column();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("10%");

			South south = new South();
			south.setParent(borderlayout);
			paging.setParent(south);

			loadData(null);
			return borderlayout;
		}

		class PaketPerawatanDetailRenderer extends ais.ui.util.MyRowRenderer {

			@Override
			public void render(final Row arg0, Object arg1) throws Exception {
				// TODO Auto-generated method stub
				final PaketPerawatanDetail paketPerawatanDetail = (PaketPerawatanDetail) arg1;
				final ItemMedis item = paketPerawatanDetail.getItem();
				final Racikan racikan = paketPerawatanDetail.getRacikan();
				final Tindakan tindakan = paketPerawatanDetail.getTindakan();
				final AlatMedis alatMedis = paketPerawatanDetail.getAlatMedis();

				if (item != null) {
					new Label().setParent(arg0);
					new Label(item.getKode()).setParent(arg0);
					new Label(item.getNama()).setParent(arg0);
					new Label(ais.common.Common.getBahasaConfig("Item dan Obat")).setParent(arg0);
				} else if (racikan != null) {
					new RacikanDetailAction(racikan, false).setParent(arg0);
					new Label(racikan.getKode()).setParent(arg0);
					new Label(racikan.getNama()).setParent(arg0);
					new Label(ais.common.Common.getBahasaConfig("Racikan")).setParent(arg0);
				} else if (tindakan != null) {
					new Label().setParent(arg0);
					new Label(tindakan.getKode()).setParent(arg0);
					new Label(tindakan.getNama()).setParent(arg0);
					new Label(ais.common.Common.getBahasaConfig("Tindakan dan Perawatan")).setParent(arg0);
				} else if (alatMedis != null) {
					new Label().setParent(arg0);
					new Label(alatMedis.getKode()).setParent(arg0);
					new Label(alatMedis.getNama()).setParent(arg0);
					new Label(ais.common.Common.getBahasaConfig("Alat Medis dan Kesehatan")).setParent(arg0);
				}

				final MyDoublebox jumlah;
				jumlah = new MyDoublebox(
						paketPerawatanDetail.getJumlah() == null ? 1.0 : paketPerawatanDetail.getJumlah());
				jumlah.setParent(arg0);
				jumlah.setStyle("text-align:right");
				jumlah.setWidth("90%");
				jumlah.setWidth("90%");
				jumlah.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						paketPerawatanDetail.setJumlah(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
						session.update(paketPerawatanDetail);

					}
				});

				if (item != null) {
					new Label(item.getSatuanItem() == null ? "" : item.getSatuanItem().getNama()).setParent(arg0);
				} else if (racikan != null) {
					new Label(ais.common.Common.getBahasaConfig("racik")).setParent(arg0);
				} else if (tindakan != null) {
					new Label(ais.common.Common.getBahasaConfig("perawatan")).setParent(arg0);
				} else if (alatMedis != null) {
					new Label(alatMedis.getPer()).setParent(arg0);
				}

				final MyTextbox keterangan = new MyTextbox(
						paketPerawatanDetail.getKeterangan() == null ? "" : paketPerawatanDetail.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setParent(arg0);

				keterangan.addEventListener(Events.ON_CHANGE, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						paketPerawatanDetail.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(session, (paketPerawatanDetail));
					}
				});

				Hbox toolbar = new Hbox();
				Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah Bapak/Ibu benar-benar yakin ingin menghapus data ini? Perlu diketahui bahwa data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = new Integer(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												Session session = HibernateUtil.currentSession();

												session.delete((paketPerawatanDetail));
												loadData(null);
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(Common.pesan(
														"Mohon maaf, data ini tidak dapat dihapus karena masih berkaitan dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau pindahkan terlebih dahulu seluruh data yang berkaitan; (2) periksa kembali keterkaitan antar data; (3) hubungi administrator apabila kendala masih berlanjut.",
																e.getMessage()));
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

		private MyTextbox kode;
		private MyTextbox nama;
		private Checkbox isItem;
		private Checkbox isRacikan;
		private Checkbox isTindakan;
		private Checkbox isAlatMedis;

		private Criteria initCriteria(boolean order) {

			Criterion critKode = Restrictions.sqlRestriction("false");
			if (!kode.getValue().trim().equals("")) {
				critKode = Restrictions.or(critKode,
						Restrictions.ilike("item.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
				critKode = Restrictions.or(critKode,
						Restrictions.ilike("tindakan.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
				critKode = Restrictions.or(critKode,
						Restrictions.ilike("alatMedis.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
				critKode = Restrictions.or(critKode,
						Restrictions.ilike("racikan.kode", kode.getValue().trim(), MatchMode.ANYWHERE));
			} else {
				critKode = Restrictions.sqlRestriction("true");
			}

			Criterion critNama = Restrictions.sqlRestriction("false");
			if (!nama.getValue().trim().equals("")) {
				critNama = Restrictions.or(critNama,
						Restrictions.ilike("item.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
				critNama = Restrictions.or(critNama,
						Restrictions.ilike("tindakan.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
				critNama = Restrictions.or(critNama,
						Restrictions.ilike("alatMedis.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
				critNama = Restrictions.or(critNama,
						Restrictions.ilike("racikan.nama", nama.getValue().trim(), MatchMode.ANYWHERE));
			} else {
				critNama = Restrictions.sqlRestriction("true");
			}

			Criterion crit = Restrictions.sqlRestriction("false");
			Boolean ada = false;
			if (isAlatMedis.isChecked()) {
				crit = Restrictions.or(crit, Restrictions.isNotNull("alatMedis"));
				ada = true;
			}
			if (isItem.isChecked()) {
				crit = Restrictions.or(crit, Restrictions.isNotNull("item"));
				ada = true;
			}
			if (isTindakan.isChecked()) {
				crit = Restrictions.or(crit, Restrictions.isNotNull("tindakan"));
				ada = true;
			}
			if (isRacikan.isChecked()) {
				crit = Restrictions.or(crit, Restrictions.isNotNull("racikan"));
				ada = true;
			}

			Session session = HibernateUtil.currentSession();
			Criteria criteria = session.createCriteria(PaketPerawatanDetail.class)

					.createAlias("racikan", "racikan", Criteria.LEFT_JOIN)
					.createAlias("item", "item", Criteria.LEFT_JOIN)
					.createAlias("tindakan", "tindakan", Criteria.LEFT_JOIN)
					.createAlias("alatMedis", "alatMedis", Criteria.LEFT_JOIN)

					.add(ada ? crit : Restrictions.sqlRestriction("false"))

					.add(critKode).add(critNama)

					.add(Restrictions.eq("paketPerawatan", TindakanAction.this.tindakan));
			if (order)
				criteria.addOrder(Order.asc("item.nama")).addOrder(Order.asc("racikan.nama"))
						.addOrder(Order.asc("tindakan.nama")).addOrder(Order.asc("alatMedis.nama"));

			return criteria;
		}

		@SuppressWarnings("unchecked")
		public void loadData(Event event) {
			Common.initPaging(initCriteria(false), paging);

			List<PaketPerawatanDetail> paketPerawatanDetails = TindakanAction.this.tindakan == null
					|| TindakanAction.this.tindakan.getId() == null
							? new ArrayList<PaketPerawatanDetail>()
							: initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
									.setFirstResult(
											Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
									.list();
			ListModel strset = new SimpleListModel(paketPerawatanDetails);
			gridItem.setRowRenderer(new PaketPerawatanDetailRenderer());
			gridItem.setModel(strset);
			gridItem.renderAll();

		}

	}
}
