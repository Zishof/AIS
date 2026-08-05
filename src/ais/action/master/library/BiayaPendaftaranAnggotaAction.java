package ais.action.master.library;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.BiayaPendaftaranAnggotaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.library.BiayaPendaftaranAnggota;
import ais.database.model.library.JenisAnggota;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.TipeAnggota;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class BiayaPendaftaranAnggotaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPerpustakaanBanbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private Combobox jenisAnggota;
	private Combobox tipeAnggota;
	private Combobox fakultas;
	private Combobox jurusan;
	private MyDatebox mulaiBerlaku;
	private Doublebox biaya;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private BiayaPendaftaranAnggota biayaPendaftaranAnggota;
	private MyToolbarbuttonConfig add;

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
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class BiayaPendaftaranAnggotaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final BiayaPendaftaranAnggota biayaPendaftaranAnggota = (BiayaPendaftaranAnggota) arg1;

			RevisiHelper.createNewRevisi(BiayaPendaftaranAnggota.class, biayaPendaftaranAnggota,
					biayaPendaftaranAnggota.getPerpustakaan().getNama()).setParent(arg0);
			new Label(biayaPendaftaranAnggota.getMulaiBerlaku() == null ? ""
					: Common.dateFormat2.get().format(biayaPendaftaranAnggota.getMulaiBerlaku())).setParent(arg0);
			new Label(biayaPendaftaranAnggota.getBiaya() == null ? ""
					: Common.numberFormat.get().format(biayaPendaftaranAnggota.getBiaya())).setParent(arg0);

			new Label(biayaPendaftaranAnggota.getTipeAnggota() == null ? "Semua"
					: biayaPendaftaranAnggota.getTipeAnggota().getNama()).setParent(arg0);
			new Label(biayaPendaftaranAnggota.getJenisAnggota() == null ? "Semua"
					: biayaPendaftaranAnggota.getJenisAnggota().getNama()).setParent(arg0);

			new Label(biayaPendaftaranAnggota.getFakultas() == null ? "Semua"
					: biayaPendaftaranAnggota.getFakultas().getNama()).setParent(arg0);
			new Label(biayaPendaftaranAnggota.getJurusan() == null ? "Semua"
					: biayaPendaftaranAnggota.getJurusan().getNama()).setParent(arg0);

			new Label(biayaPendaftaranAnggota.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(biayaPendaftaranAnggota);
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
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(biayaPendaftaranAnggota);

											onSearchDefault(event);
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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new BiayaPendaftaranAnggota());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(BiayaPendaftaranAnggota biayaPendaftaranAnggota) throws Exception {
		this.biayaPendaftaranAnggota = biayaPendaftaranAnggota;
		addWindow.setTitle(biayaPendaftaranAnggota.getId() == null ? "Tambah Biaya Pendaftaran Anggota" : "Ubah Biaya Pendaftaran Anggota");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setAttribute("perpustakaan", biayaPendaftaranAnggota.getPerpustakaan());
		perpustakaan.setValue(biayaPendaftaranAnggota.getPerpustakaan() == null ? ""
				: biayaPendaftaranAnggota.getPerpustakaan().getNama());
		perpustakaan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Anggota"));
		row.appendChild(jenisAnggota = new Combobox());
		jenisAnggota.setWidth("90%");
		jenisAnggota.setReadonly(true);
		Common.insertComboDanSemua(jenisAnggota, "nama", JenisAnggota.class);
		Common.selectComboItem(jenisAnggota, biayaPendaftaranAnggota.getJenisAnggota());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Anggota"));
		row.appendChild(tipeAnggota = new Combobox());
		tipeAnggota.setWidth("90%");
		tipeAnggota.setReadonly(true);
		Common.insertComboDanSemua(tipeAnggota, "nama", TipeAnggota.class);
		Common.selectComboItem(tipeAnggota, biayaPendaftaranAnggota.getTipeAnggota());

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), searchfakultas,
				searchjurusan);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, biayaPendaftaranAnggota.getFakultas());
		fakultas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", biayaPendaftaranAnggota.getFakultas() == null ? tbmuser.ambilFakultas()
						: biayaPendaftaranAnggota.getFakultas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, biayaPendaftaranAnggota.getJurusan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai Berlaku"));
		row.appendChild(mulaiBerlaku = new MyDatebox(biayaPendaftaranAnggota.getMulaiBerlaku()));
		mulaiBerlaku.setFormat(Common.dateFormat1.get().toPattern());
		mulaiBerlaku.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Biaya"));
		row.appendChild(biaya = new MyDoublebox(biayaPendaftaranAnggota.getBiaya()));
		biaya.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				biayaPendaftaranAnggota.getKeterangan() == null ? "" : biayaPendaftaranAnggota.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

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
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			MyMessageboxConfig.show("Perpustakaan harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (mulaiBerlaku.getValue() == null) {
			MyMessageboxConfig.show("Mulai Berlaku harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (biaya.getValue() == null) {
			MyMessageboxConfig.show("Biaya harus diisi", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		BiayaPendaftaranAnggotaDao biayaPendaftaranAnggotaDao = DaoFactory.getInstance()
				.getBiayaPendaftaranAnggotaDao();
		if (biayaPendaftaranAnggota.getId() != null) {
			biayaPendaftaranAnggota = biayaPendaftaranAnggotaDao.load(biayaPendaftaranAnggota.getId());

		}

		biayaPendaftaranAnggota.setBiaya(biaya.getValue());
		biayaPendaftaranAnggota.setMulaiBerlaku(mulaiBerlaku.getValue());
		biayaPendaftaranAnggota.setPerpustakaan((Perpustakaan) perpustakaan.getAttribute("perpustakaan"));
		biayaPendaftaranAnggota.setKeterangan(keterangan.getValue());

		biayaPendaftaranAnggota.setJenisAnggota((JenisAnggota) (jenisAnggota.getSelectedItem() == null ? null
				: jenisAnggota.getSelectedItem().getValue()));
		biayaPendaftaranAnggota.setTipeAnggota((TipeAnggota) (tipeAnggota.getSelectedItem() == null ? null
				: tipeAnggota.getSelectedItem().getValue()));
		biayaPendaftaranAnggota.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue()==null ? null : fakultas.getSelectedItem().getValue()));
		biayaPendaftaranAnggota.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue()==null ? null : jurusan.getSelectedItem().getValue()));

		if (biayaPendaftaranAnggota.getId() != null) {
			biayaPendaftaranAnggotaDao.update(biayaPendaftaranAnggota);
		} else {
			biayaPendaftaranAnggotaDao.save(biayaPendaftaranAnggota);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BiayaPendaftaranAnggota.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getAttribute("perpustakaan") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("perpustakaan", searchnama.getAttribute("perpustakaan"))))
				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BiayaPendaftaranAnggota> biayaPendaftaranAnggota = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(biayaPendaftaranAnggota);
		grid.setRowRenderer(new BiayaPendaftaranAnggotaRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
