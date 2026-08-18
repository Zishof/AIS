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
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kota;
import ais.database.model.Propinsi;
import ais.database.model.sirs.Dusun;
import ais.database.model.sirs.Kecamatan;
import ais.database.model.sirs.Kelurahan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class DusunAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5424568964769538572L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;
	private MyTextbox searchnamadusun;

	private Combobox kelurahan;
	private MyTextbox nama;
	private Toolbarbutton add;

	private boolean edit = false;
	private boolean delete = false;

	private Dusun dusun;
	private Combobox propinsi;
	private Combobox kota;
	private Combobox kecamatan;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
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

	class DusunRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Dusun dusun = (Dusun) arg1;

			RevisiHelper.createNewRevisi(Dusun.class, dusun, dusun.getNama()).setParent(arg0);
			new Label(dusun.getKelurahan().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(dusun);
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
					MyMessageboxConfig.show(
							"Apakah Bapak/Ibu yakin ingin menghapus data ini? Perlu diketahui, data yang telah dihapus tidak dapat dikembalikan.",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(dusun);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) pastikan tidak ada data lain yang masih terkait dengan data ini; (2) hapus terlebih dahulu seluruh data yang berelasi; (3) apabila kendala masih berlanjut, mohon hubungi administrator sistem.",
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

	public void onAdd(Event event) throws Exception {
		init(new Dusun());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Dusun dusun) {
		this.dusun = dusun;
		addWindow.setTitle(dusun.getId() == null ? "Tambah Dusun" : "Ubah Dusun");
		Common.clear(addWindow);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Nama Kampung / Dusun"));
		row.appendChild(nama = new MyTextbox(dusun.getNama() == null ? "" : dusun.getNama()));
		nama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
		Common.insertCombo(propinsi = new Combobox(), "nama", Propinsi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(propinsi,
				dusun.getKelurahan() == null ? null : dusun.getKelurahan().getKecamatan().getKota().getPropinsi());
		row.appendChild(propinsi);
		propinsi.setWidth("90%");

		propinsi.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kota);
				if (propinsi.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(kota, "nama", Kota.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								Restrictions.eq("propinsi", propinsi.getSelectedItem().getValue())));
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Kota/Kabupaten"));
		Common.insertCombo(kota = new Combobox(), "nama", Kota.class,
				Restrictions.and(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						Restrictions.eq("propinsi", dusun.getKelurahan() == null ? null
								: dusun.getKelurahan().getKecamatan().getKota().getPropinsi())));
		Common.selectComboItem(kota,
				dusun.getKelurahan() == null ? null : dusun.getKelurahan().getKecamatan().getKota());
		row.appendChild(kota);
		kota.setWidth("90%");

		kota.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kecamatan);
				if (kota.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(kecamatan, "nama", Kecamatan.class,
						Restrictions.eq("kota", kota.getSelectedItem().getValue()));
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kecamatan")));
		Common.insertCombo(kecamatan = new Combobox(), "nama", Kecamatan.class, Restrictions.eq("kota",
				dusun.getKelurahan() == null ? null : dusun.getKelurahan().getKecamatan().getKota()));
		Common.selectComboItem(kecamatan, dusun.getKelurahan() == null ? null : dusun.getKelurahan().getKecamatan());
		row.appendChild(kecamatan);
		kecamatan.setWidth("90%");

		kecamatan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(kelurahan);
				if (kecamatan.getSelectedItem() == null) {
					return;
				}
				Common.insertCombo(kelurahan, "nama", Kelurahan.class,
						Restrictions.eq("kecamatan", kecamatan.getSelectedItem().getValue()));
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelurahan")));
		Common.insertCombo(kelurahan = new Combobox(), "nama", Kelurahan.class, Restrictions.eq("kecamatan",
				dusun.getKelurahan() == null ? null : dusun.getKelurahan().getKecamatan()));
		Common.selectComboItem(kelurahan, dusun.getKelurahan());
		row.appendChild(kelurahan);
		kelurahan.setWidth("90%");

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
				addWindow.setVisible(false);
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
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Kampung / Dusun belum diisi. Mohon Bapak/Ibu mengisi terlebih dahulu nama dusun sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kelurahan.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Kelurahan belum diisi. Mohon Bapak/Ibu memilih terlebih dahulu kelurahan yang sesuai sebelum menyimpan data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaDusun();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Dusun yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan nama dusun yang berbeda; (2) periksa kembali data dusun yang telah ada; (3) pastikan tidak terjadi duplikasi data.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (dusun.getId() != null) {
			dusun = (Dusun) session.load(Dusun.class, dusun.getId());

		}

		dusun.setNama(nama.getValue());
		dusun.setKelurahan(
				(Kelurahan) (kelurahan.getSelectedItem() == null ? null : kelurahan.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, dusun);
		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Dusun> dusun = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(dusun);
		grid.setRowRenderer(new DusunRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dusun.class)

				.add((searchnamadusun == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnamadusun.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	public Boolean checkNamaDusun() {

		Integer dusunCount = null;
		Session session = HibernateUtil.currentSession();
		dusunCount = ((Number) session.createCriteria(Dusun.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kelurahan", kelurahan.getSelectedItem().getValue()))
				.add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
				.add(this.dusun.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.dusun.getId()))
				.uniqueResult()).intValue();

		return !dusunCount.equals(0);
	}

}
