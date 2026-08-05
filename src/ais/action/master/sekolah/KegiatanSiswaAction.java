package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.sekolah.DashboardRekapKegiatanSiswaData;
import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.ParameterTambahanKegiatanSiswaListener;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.KegiatanSiswa;
import ais.database.model.sekolah.KelompokKegiatanSiswa;
import ais.database.model.sekolah.PembinaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class KegiatanSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private AmbilDataTbmuserBanbox searchpembina;
	private Combobox searchjenis;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KegiatanSiswa kegiatanSiswa;
	private MyToolbarbuttonConfig add;
	private Combobox kelompokKegiatanSiswa;
	private AmbilDataSiswaBanbox siswa;
	private MyDatebox waktu;
	private Row rowParameterTambahan;
	private ParameterTambahanKegiatanSiswaListener parameterTambahanKegiatanSiswaListener;

	private Tabpanel rekapDataPanel;

	public void onRekapData(Event event) {
		if (rekapDataPanel.getChildren().size() == 0) {
			DashboardRekapKegiatanSiswaData window = new DashboardRekapKegiatanSiswaData();
			ais.ui.util.BaseDasbordPortal.mountWrapped(window, rekapDataPanel,
				"Rekap Kegiatan Siswa", "Gambaran kegiatan yang diikuti siswa beserta hasilnya.");
		}
	}

	private MyTabConfig parameterJenisKegiatanTab;
	private MyTabConfig jenisKegiatanTab;
	private MyTabConfig parameterTab;
	private MyTabConfig pembinaTab;

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

		Common.insertComboDanSemua(searchjenis, "nama", KelompokKegiatanSiswa.class, Restrictions.eq("aktif", true));

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Tbmuser tbmuser = Common.getCurrentUser();

		parameterJenisKegiatanTab
				.setVisible(tbmuser != null && tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null);
		if (jenisKegiatanTab != null) { jenisKegiatanTab.setVisible(parameterJenisKegiatanTab.isVisible()); }
		if (parameterTab != null) { parameterTab.setVisible(parameterJenisKegiatanTab.isVisible()); }
		if (pembinaTab != null) { pembinaTab.setVisible(parameterJenisKegiatanTab.isVisible()); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		searchpembina.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "siswa", "sekolah", "ta", "kelompokKegiatanSiswa", "nilai",
				"keterangan", "aktif", "pembina1", "pembina2", "pembina3" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	        FilterLanjutHelper.setup(comp);
}

	class KegiatanSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanSiswa kegiatanSiswa = (KegiatanSiswa) arg1;

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			CommonMedia.tampilkanGambarKecil(kegiatanSiswa.getSiswa()).setParent(hbox);
			Vbox vbox = new Vbox();
			vbox.setParent(hbox);
			vbox.appendChild(new Label(kegiatanSiswa.getSiswa().getNomorInduk()));
			vbox.appendChild(new Label(kegiatanSiswa.getSiswa().getNamaSiswa()));
			vbox.appendChild(new Label(kegiatanSiswa.getSiswa().getSekolah().getNama()));

			String pembina1 = kegiatanSiswa.getPembina1() == null ? "" : kegiatanSiswa.getPembina1().getUserNama();
			String pembina2 = kegiatanSiswa.getPembina2() == null ? "" : kegiatanSiswa.getPembina2().getUserNama();
			String pembina3 = kegiatanSiswa.getPembina3() == null ? "" : kegiatanSiswa.getPembina3().getUserNama();

			RevisiHelper.createNewRevisi(KegiatanSiswa.class, kegiatanSiswa,
					kegiatanSiswa.getKelompokKegiatanSiswa().getNama()).setParent(arg0);

			new Label(pembina1 + " " + pembina2 + " " + pembina3).setParent(arg0);

			new Label(Common.dateFormat5.get().format(kegiatanSiswa.getWaktu())).setParent(arg0);
			new Label(kegiatanSiswa.getTa()).setParent(arg0);

			new Label(kegiatanSiswa.getKeterangan()).setParent(arg0);

			vbox = new Vbox();
			vbox.setParent(arg0);
			int i = 1;
			for (CommonVO commonVO : kegiatanSiswa.ambilDataParameterTambahan()) {
				String lbl = commonVO.getName();
				String url = commonVO.getName2();
				String val = commonVO.getName1();

				try {
					String[] d = StringUtils.split(val, ":");
					if (Common.isNumber(d[1].trim())) {
						val = d[0];
					}
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

				if ((!val.trim().isEmpty() && !val.trim().equalsIgnoreCase("null")) || !url.trim().isEmpty()) {
					String[] param = lbl.split("->");

					String label = param.length > 1 ? param[1] : "";

					vbox.appendChild(new MyLabelAgakKecil(i + ". " + label + " : " + val));

					if (!url.trim().isEmpty()) {
						A a;
						vbox.appendChild(a = new A("  Lampiran \"" + label + " : " + val + "\""));
						a.setHref(url);
						a.setTarget("_blank");
						a.setStyle("font-size:9px;");
					}

					i++;
				}
			}

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kegiatanSiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanSiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kegiatanSiswa);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kegiatanSiswa, KegiatanSiswaAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KegiatanSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kegiatanSiswa = (KegiatanSiswa) obj;
		init(kegiatanSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	private AmbilDataTbmuserBanbox pembina1;
	private AmbilDataTbmuserBanbox pembina2;
	private AmbilDataTbmuserBanbox pembina3;
	private Combobox ta;
	private Label poin;

	@SuppressWarnings({})
	private void init(KegiatanSiswa kegiatanSiswa) {
		this.kegiatanSiswa = kegiatanSiswa;
		addWindow.setTitle(kegiatanSiswa.getId() == null ? "Tambah Kegiatan Siswa" : "Ubah Kegiatan Siswa");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", kegiatanSiswa.getSiswa());
		siswa.setValue(kegiatanSiswa.getSiswa() == null ? "" : kegiatanSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembina Utama *"));
		row.appendChild(pembina1 = new AmbilDataTbmuserBanbox());
		pembina1.setReadonly(true);
		pembina1.setWidth("90%");

		pembina1.setAttribute("tbmuser", kegiatanSiswa.getPembina1());
		pembina1.setValue(kegiatanSiswa.getPembina1() == null ? "" : kegiatanSiswa.getPembina1().getUserNama());

		if (!parameterJenisKegiatanTab.isVisible()) {
			Tbmuser tbmuser = Common.getCurrentUser();
			pembina1.setAttribute("tbmuser", tbmuser);
			pembina1.setValue(tbmuser.getUserNama());
			pembina1.setDisabled(true);
		}

		EventListener eventListener = new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				pembina1.setDisabled(false);
				PembinaSiswa pembinaSiswa = null;
				Siswa s = (Siswa) siswa.getAttribute("siswa");
				if (s != null) {
					Map<Long, PembinaSiswa> p = ConstantValues.ambilBerdasarClass(PembinaSiswa.class);
					for (PembinaSiswa pembinaSiswa2 : p.values()) {
						if (pembinaSiswa2 != null && pembinaSiswa2.getSiswa() != null
								&& pembinaSiswa2.getSiswa().getId().equals(s.getId())) {
							pembinaSiswa = pembinaSiswa2;
						}
					}

					if (pembinaSiswa != null && pembinaSiswa.getPembina() != null) {
						pembina1.setAttribute("tbmuser", pembinaSiswa.getPembina());
						pembina1.setValue(pembinaSiswa.getPembina().getUserNama());
						pembina1.setDisabled(true);
					} else if (!parameterJenisKegiatanTab.isVisible()) {
						Tbmuser tbmuser = Common.getCurrentUser();
						pembina1.setAttribute("tbmuser", tbmuser);
						pembina1.setValue(tbmuser.getUserNama());
						pembina1.setDisabled(true);
					}
				} else if (!parameterJenisKegiatanTab.isVisible()) {
					Tbmuser tbmuser = Common.getCurrentUser();
					pembina1.setAttribute("tbmuser", tbmuser);
					pembina1.setValue(tbmuser.getUserNama());
					pembina1.setDisabled(true);
				}
			}
		};

		siswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembina Kedua"));
		row.appendChild(pembina2 = new AmbilDataTbmuserBanbox());
		pembina2.setReadonly(true);
		pembina2.setWidth("90%");
		pembina2.setAttribute("tbmuser", kegiatanSiswa.getPembina2());
		pembina2.setValue(kegiatanSiswa.getPembina2() == null ? "" : kegiatanSiswa.getPembina2().getUserNama());

		try {
			eventListener.onEvent(null);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pembina Ketiga"));
		row.appendChild(pembina3 = new AmbilDataTbmuserBanbox());
		pembina3.setReadonly(true);
		pembina3.setWidth("90%");
		pembina3.setAttribute("tbmuser", kegiatanSiswa.getPembina3());
		pembina3.setValue(kegiatanSiswa.getPembina3() == null ? "" : kegiatanSiswa.getPembina3().getUserNama());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu *"));
		row.appendChild(waktu = new MyDatebox(kegiatanSiswa.getWaktu()));
		waktu.setFormat(Common.dateFormat3.get().toPattern());
		waktu.setWidth("90%");
		waktu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran *"));
		row.appendChild(ta = new Combobox());
		ta.setWidth("90%");
		ta.setReadonly(true);
		Common.generateTahunAjaran(ta);
		Common.selectComboItem(ta, kegiatanSiswa.getTa());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kegiatanSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kegiatan *"));
		row.appendChild(kelompokKegiatanSiswa = new Combobox());
		Common.insertCombo(kelompokKegiatanSiswa, "nama", KelompokKegiatanSiswa.class);
		Common.selectComboItem(kelompokKegiatanSiswa, kegiatanSiswa.getKelompokKegiatanSiswa());
		kelompokKegiatanSiswa.setWidth("90%");
		kelompokKegiatanSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Poin"));
		row.appendChild(poin = new Label(kegiatanSiswa.getKelompokKegiatanSiswa() == null ? "0"
				: Common.numberFormat.get().format(kegiatanSiswa.getKelompokKegiatanSiswa().getPoin())));

		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);

		List<Row> parameterRows = new ArrayList<Row>();

		lampiranLains = new HashMap<String, LampiranLain>();
		parameterTambahanKegiatanSiswaListener = new ParameterTambahanKegiatanSiswaListener(kegiatanSiswa,
				parameterRows, lampiranLains, rows);

		boolean visible = parameterTambahanKegiatanSiswaListener.check();
		rowParameterTambahan.setVisible(visible);

		EventListener evn = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelompokKegiatanSiswa k = (KelompokKegiatanSiswa) (kelompokKegiatanSiswa.getSelectedItem() == null
						? null
						: kelompokKegiatanSiswa.getSelectedItem().getValue());
				parameterTambahanKegiatanSiswaListener.onEvent(new Event("", kelompokKegiatanSiswa, k));

				poin.setValue(k == null ? "0" : Common.numberFormat.get().format(k.getPoin()));
			}
		};

		kelompokKegiatanSiswa.addEventListener("onChange", evn);

		Common.createDefaultTimer(evn);

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
		if (siswa.getAttribute("siswa") == null) {
			MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (pembina1.getAttribute("tbmuser") == null) {
			MyMessageboxConfig.show("Pembina Utama harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kelompokKegiatanSiswa.getSelectedItem() == null
				|| kelompokKegiatanSiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Jenis kegiatan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kegiatanSiswa.getId() != null) {
			kegiatanSiswa = (KegiatanSiswa) session.load(KegiatanSiswa.class, kegiatanSiswa.getId());

		}

		kegiatanSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		kegiatanSiswa
				.setKelompokKegiatanSiswa((KelompokKegiatanSiswa) kelompokKegiatanSiswa.getSelectedItem().getValue());
		kegiatanSiswa.setKeterangan(keterangan.getValue());

		kegiatanSiswa.setPembina1((Tbmuser) pembina1.getAttribute("tbmuser"));
		kegiatanSiswa.setPembina2((Tbmuser) pembina2.getAttribute("tbmuser"));
		kegiatanSiswa.setPembina3((Tbmuser) pembina3.getAttribute("tbmuser"));

		kegiatanSiswa.setTa((String) ta.getSelectedItem().getValue());

		if (kegiatanSiswa.getId() == null) {
			session.save(kegiatanSiswa);
		}

		if (lampiranLains != null && !lampiranLains.isEmpty()) {
			for (LampiranLain lampiranLain : lampiranLains.values()) {
				if (lampiranLain != null && lampiranLain.getId() != null) {
					try {
						session = StreamingHibernateUtil.getInstance().currentSession();

						session.refresh(lampiranLain);
						lampiranLain.setRef(kegiatanSiswa.getId());

						session.getTransaction().begin();
						session.update(lampiranLain);
						session.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}
			}

		}

		parameterTambahanKegiatanSiswaListener.onSave(kegiatanSiswa);
		Common.refreshSaveOrUpdate(session, kegiatanSiswa);

		EventListener evn = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				KelompokKegiatanSiswa k = (KelompokKegiatanSiswa) (kelompokKegiatanSiswa.getSelectedItem() == null
						? null
						: kelompokKegiatanSiswa.getSelectedItem().getValue());
				parameterTambahanKegiatanSiswaListener.onEvent(new Event("", kelompokKegiatanSiswa, k));
			}
		};

		if (!ParameterTambahanKegiatanSiswaListener.validate(kegiatanSiswa, evn, true)) {
			return false;
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Tbmuser pembina = (Tbmuser) this.searchpembina.getAttribute("tbmuser");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanSiswa.class).createAlias("siswa", "siswa");

		if (order)
			criteria.addOrder(Order.desc("waktu"));

		criteria

				.add(pembina == null ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.eq("pembina1", pembina),
								Restrictions.or(Restrictions.eq("pembina2", pembina),
										Restrictions.eq("pembina3", pembina))))

				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("siswa.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchjenis.getSelectedItem() == null || searchjenis.getSelectedItem().getValue() == null
						|| searchjenis.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("kelompokKegiatanSiswa", searchjenis.getSelectedItem().getValue()))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KegiatanSiswa> kegiatanSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatanSiswa);
		grid.setRowRenderer(new KegiatanSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
