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
import org.zkoss.zul.Caption;
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
import ais.database.model.sirs.Kecamatan;
import ais.database.model.sirs.Kelurahan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;

public class KelurahanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5424568964769538572L;
	private Window addWindow;
	private Grid grid;
	private Paging paging;
	private MyTextbox searchnamakelurahan;

	private Combobox kecamatan;
	private MyTextbox nama;
	private Toolbarbutton add;

	private boolean edit = false;
	private boolean delete = false;

	private Kelurahan kelurahan;
	private Combobox propinsi;
	private Combobox kota;

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

	class KelurahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Kelurahan kelurahan = (Kelurahan) arg1;

			RevisiHelper.createNewRevisi(Kelurahan.class, kelurahan, kelurahan.getNama()).setParent(arg0);
			new Label(kelurahan.getKecamatan().getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(kelurahan);
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
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data kelurahan ini? Data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Common.refreshDelete(kelurahan);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data kelurahan ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis: {V1}. Langkah yang dapat dilakukan: (1) hapus terlebih dahulu data lain yang berkaitan dengan data ini; (2) pastikan data tidak sedang digunakan pada transaksi lain; (3) hubungi administrator apabila kendala masih berlanjut.",
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

	public void onAdd(Event event) throws Exception {
		init(new Kelurahan());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Kelurahan kelurahan) {
		this.kelurahan = kelurahan;
		addWindow.setTitle(kelurahan.getId() == null ? "Tambah Kelurahan" : "Ubah Kelurahan");
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
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Kelurahan")));
		row.appendChild(nama = new MyTextbox(kelurahan.getNama() == null ? "" : kelurahan.getNama()));
		nama.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Propinsi")));
		Common.insertCombo(propinsi = new Combobox(), "nama", Propinsi.class,Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(propinsi,
				kelurahan.getKecamatan() == null ? null : kelurahan.getKecamatan().getKota().getPropinsi());
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
		Common.insertCombo(kota = new Combobox(), "nama", Kota.class, Restrictions.and(
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("propinsi",
						kelurahan.getKecamatan() == null ? null : kelurahan.getKecamatan().getKota().getPropinsi())));
		Common.selectComboItem(kota, kelurahan.getKecamatan() == null ? null : kelurahan.getKecamatan().getKota());
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
		Common.insertCombo(kecamatan = new Combobox(), "nama", Kecamatan.class,
				Restrictions.eq("kota", kelurahan.getKecamatan() == null ? null : kelurahan.getKecamatan().getKota()));
		Common.selectComboItem(kecamatan, kelurahan.getKecamatan() == null ? null : kelurahan.getKecamatan());
		row.appendChild(kecamatan);
		kecamatan.setWidth("90%");

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
		Caption caption = new Caption();
		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig(" X ");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}

		});
		button.setParent(caption);
		// caption.setParent(addWindow);
	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelurahan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Kelurahan pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data setelah kolom terisi.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (kecamatan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Kecamatan wajib dipilih terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih Kecamatan pada daftar yang tersedia; (2) pastikan pilihan tidak dikosongkan; (3) simpan kembali data setelah pilihan ditentukan.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		boolean i = checkNamaKelurahan();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, data kelurahan dengan nama tersebut sudah terdaftar di dalam sistem. Langkah yang dapat dilakukan: (1) gunakan nama kelurahan yang berbeda; (2) periksa kembali data yang telah ada melalui pencarian; (3) lakukan perubahan pada data yang sudah ada apabila diperlukan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelurahan.getId() != null) {
			kelurahan = (Kelurahan) session.load(Kelurahan.class, kelurahan.getId());

		}

		kelurahan.setNama(nama.getValue());
		kelurahan.setKecamatan(
				(Kecamatan) (kecamatan.getSelectedItem() == null ? null : kecamatan.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, kelurahan);
		return true;
	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kelurahan.class)
				.add((searchnamakelurahan == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnamakelurahan.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.asc("nama"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<Kelurahan> kelurahan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelurahan);
		grid.setRowRenderer(new KelurahanRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	public Boolean checkNamaKelurahan() {

		Integer kelurahanCount = null;
		Session session = HibernateUtil.currentSession();
		kelurahanCount = ((Number) session.createCriteria(Kelurahan.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kecamatan", kecamatan.getSelectedItem().getValue()))
				.add(Restrictions.eq("nama", nama.getValue().trim()).ignoreCase())
				.add(this.kelurahan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelurahan.getId()))
				.uniqueResult()).intValue();

		return !kelurahanCount.equals(0);
	}

}
