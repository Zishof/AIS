package ais.action.master.bkd;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.bkd.helper.AssesorAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AsesorPenunjangKinerjaDosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AsesorPenunjangKinerjaDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

//	
//	

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;

	private Combobox jurusan;
	private Combobox fakultas;
	// private Combobox perguruanTinggi;

	private boolean edit = false;
	private boolean delete = false;

	private AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen;
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
		

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(AsesorPenunjangKinerjaDosen.class)
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();
		Tbmuser tbmuser = Common.getCurrentUser();

		if (count == 0) {
			AsesorPenunjangKinerjaDosen angket = new AsesorPenunjangKinerjaDosen();
			angket.setKode("A");
			angket.setNama("Asesor I");
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			Common.refreshSaveOrUpdate(session, angket);

			angket = new AsesorPenunjangKinerjaDosen();
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			angket.setKode("B");
			angket.setNama("Asesor II");
			Common.refreshSaveOrUpdate(session, angket);

			angket = new AsesorPenunjangKinerjaDosen();
			angket.setFakultas(tbmuser.ambilFakultas());
			angket.setJurusan(tbmuser.ambilJurusan());
			angket.setKode("C");
			angket.setNama("Asesor III");
			Common.refreshSaveOrUpdate(session, angket);

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

	class AsesorPenunjangKinerjaDosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) arg1;

			new AssesorAction(asesorPenunjangKinerjaDosen).setParent(arg0);

			new Label(asesorPenunjangKinerjaDosen.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(AsesorPenunjangKinerjaDosen.class, asesorPenunjangKinerjaDosen,
					asesorPenunjangKinerjaDosen.getNama()).setParent(arg0);
			new Label(asesorPenunjangKinerjaDosen.getFakultas() == null ? "Semua"
					: asesorPenunjangKinerjaDosen.getFakultas().getNama()).setParent(arg0);
			new Label(asesorPenunjangKinerjaDosen.getJurusan() == null ? "Semua"
					: asesorPenunjangKinerjaDosen.getJurusan().getNama()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(asesorPenunjangKinerjaDosen.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					asesorPenunjangKinerjaDosen.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(asesorPenunjangKinerjaDosen);
				}
			});

			new Label(asesorPenunjangKinerjaDosen.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(asesorPenunjangKinerjaDosen);
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
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(asesorPenunjangKinerjaDosen);

											onSearchDefault(event);
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
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen = new AsesorPenunjangKinerjaDosen();
		Tbmuser tbmuser = Common.getCurrentUser();
		asesorPenunjangKinerjaDosen.setFakultas(tbmuser.ambilFakultas());
		asesorPenunjangKinerjaDosen.setJurusan(tbmuser.ambilJurusan());
		init(asesorPenunjangKinerjaDosen);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(AsesorPenunjangKinerjaDosen asesorPenunjangKinerjaDosen) throws Exception {

		

		this.asesorPenunjangKinerjaDosen = asesorPenunjangKinerjaDosen;
		addWindow.setTitle(asesorPenunjangKinerjaDosen.getId() == null ? "Tambah Asesor Penunjang Kinerja Dosen" : "Ubah Asesor Penunjang Kinerja Dosen");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Asesor"));
		row.appendChild(kode = new Textbox(asesorPenunjangKinerjaDosen.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Istilah Asesor"));
		row.appendChild(nama = new Textbox(
				asesorPenunjangKinerjaDosen.getNama() == null ? "" : asesorPenunjangKinerjaDosen.getNama()));
		nama.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		MyFormRow rowFakultas = new MyFormRow();
		rowFakultas.setStyle("border:0px;background: transparent;");
		rowFakultas.setParent(rows);
		rowFakultas.appendChild(new Label(ais.common.Common.getBahasaConfig("Fakultas")));
		rowFakultas.appendChild(fakultas);
		Common.selectComboItem(fakultas, asesorPenunjangKinerjaDosen.getFakultas());
		fakultas.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();
		Common.insertComboDanSemua(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", asesorPenunjangKinerjaDosen.getFakultas() == null ? tbmuser.ambilFakultas()
						: asesorPenunjangKinerjaDosen.getFakultas()));

		MyFormRow rowJurusan = new MyFormRow();
		rowJurusan.setStyle("border:0px;background: transparent;");
		rowJurusan.setParent(rows);
		rowJurusan.appendChild(new Label(ais.common.Common.getBahasaConfig("Jurusan")));
		rowJurusan.appendChild(jurusan);
		jurusan.setWidth("90%");
		Common.pilihJurusan(jurusan, asesorPenunjangKinerjaDosen.getJurusan());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(asesorPenunjangKinerjaDosen.getKeterangan() == null ? ""
				: asesorPenunjangKinerjaDosen.getKeterangan()));
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

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Asesor Penunjang Kinerja Dosen harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Asesor Penunjang Kinerja Dosen harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkKodeAsesorPenunjangKinerjaDosen();
		if (i) {
			MyMessageboxConfig.show("Kode Asesor Penunjang Kinerja Dosen sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNamaAsesorPenunjangKinerjaDosen();
		if (i) {
			MyMessageboxConfig.show("Nama Asesor Penunjang Kinerja Dosen sudah ada di database", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (asesorPenunjangKinerjaDosen.getId() != null) {
			asesorPenunjangKinerjaDosen = (AsesorPenunjangKinerjaDosen) session.load(AsesorPenunjangKinerjaDosen.class,
					asesorPenunjangKinerjaDosen.getId());

		}

		asesorPenunjangKinerjaDosen.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		asesorPenunjangKinerjaDosen.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		asesorPenunjangKinerjaDosen.setKode(kode.getValue());
		asesorPenunjangKinerjaDosen.setNama(nama.getValue());
		asesorPenunjangKinerjaDosen.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, asesorPenunjangKinerjaDosen);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(AsesorPenunjangKinerjaDosen.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<AsesorPenunjangKinerjaDosen> asesorPenunjangKinerjaDosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(asesorPenunjangKinerjaDosen);
		grid.setRowRenderer(new AsesorPenunjangKinerjaDosenRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeAsesorPenunjangKinerjaDosen() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AsesorPenunjangKinerjaDosen.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.asesorPenunjangKinerjaDosen.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.asesorPenunjangKinerjaDosen.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaAsesorPenunjangKinerjaDosen() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(AsesorPenunjangKinerjaDosen.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.asesorPenunjangKinerjaDosen.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.asesorPenunjangKinerjaDosen.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
