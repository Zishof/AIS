package ais.action.master.payroll;

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
import org.zkoss.zul.Intbox;
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

import ais.action.master.payroll.detail.RencanaGajiPunyaPegawaiAction;
import ais.action.master.payroll.util.RencanaItemGajiPegawaiTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.payroll.RencanaGaji;
import ais.database.model.payroll.RencanaGajiPunyaPegawai;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class RencanaGajiAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;

	private MyWindow addWindow;
	private RencanaGaji rencanaGaji;

	private Intbox thnByr;
	private Textbox keterangan;

	private boolean delete = false;
	private boolean edit = false;
	private RencanaGaji rencanaGajiCopy = null;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class RencanaGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final RencanaGaji rencanaGaji = (RencanaGaji) arg1;

			new RencanaGajiPunyaPegawaiAction(rencanaGaji).setParent(arg0);

			new Label(rencanaGaji.getTahun() + "").setParent(arg0);

			new Label(rencanaGaji.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, rencanaGaji, RencanaGajiAction.this).setParent(arg0);
		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<RencanaGaji> rencanaGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(rencanaGaji);
		grid.setRowRenderer(new RencanaGajiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(RencanaGaji.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("keterangan", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	public void onAdd(Event event) throws Exception {
		init(new RencanaGaji());
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		// TODO Auto-generated method stub
		this.rencanaGaji = (RencanaGaji) obj;

		this.rencanaGajiCopy = (RencanaGaji) obj.getCopyDari();

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun *"));
		row.appendChild(thnByr = new Intbox(rencanaGaji.getTahun()));

		if (rencanaGaji.getId() != null) {
			Common.freezeGanti(rows, true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(rencanaGaji.getKeterangan()));
		keterangan.setWidth("90%");
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
		addWindow.onModal();
		addWindow.setVisible(true);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (thnByr.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, kolom Tahun Rencana Gaji belum diisi. Langkah yang dapat dilakukan: (1) isikan Tahun pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = check();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Rencana Gaji untuk tahun yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan tahun yang berbeda; (2) periksa kembali daftar rencana gaji yang telah ada; (3) gunakan fitur edit jika perlu memperbarui data.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (rencanaGaji.getId() != null) {
			rencanaGaji = (RencanaGaji) session.load(RencanaGaji.class, rencanaGaji.getId());

		}

		rencanaGaji.setKeterangan(keterangan.getValue());
		rencanaGaji.setTahun(thnByr.getValue());

		Common.refreshSaveOrUpdate(session, rencanaGaji);
		session.flush();

		if (rencanaGajiCopy != null) {

			final Label label = Common.displayLoadBar(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
					Session session = HibernateUtil.currentNativeSession();
					List<Pegawai> pegawais = session.createCriteria(RencanaGajiPunyaPegawai.class)
							.setProjection(Projections.groupProperty("pegawai")).add(Restrictions.isNotNull("pegawai"))
							.createAlias("pegawai", "pegawai").add(Restrictions.isNotNull("pegawai.formatItemGaji")).add(Restrictions.eq("pegawai.aktif",true))
							.add(Restrictions.eq("rencanaGaji", rencanaGajiCopy)).list();
					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}
					HibernateUtil.closeSession();

					int size = pegawais.size();
					int index = 0;

					for (Pegawai pegawai : pegawais) {

						index++;
						label.setValue("Memproses data rencana gaji " + pegawai.getNama() + " ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");

						session = HibernateUtil.currentNativeSession();
						RencanaGajiPunyaPegawai rencanaGajiPunyaPegawai = (RencanaGajiPunyaPegawai) session
								.createCriteria(RencanaGajiPunyaPegawai.class)
								.add(Restrictions.eq("rencanaGaji", rencanaGaji))
								.add(Restrictions.eq("pegawai", pegawai)).setMaxResults(1).uniqueResult();

						if (rencanaGajiPunyaPegawai == null) {
							rencanaGajiPunyaPegawai = new RencanaGajiPunyaPegawai();
							rencanaGajiPunyaPegawai.setPegawai(pegawai);
							rencanaGajiPunyaPegawai.setKeterangan("");
							rencanaGajiPunyaPegawai.setRencanaGaji(rencanaGaji);
							session.getTransaction().begin();
							session.save(rencanaGajiPunyaPegawai);
							session.getTransaction().commit();
						}
						// session.disconnect();
						if (session.isOpen()) {session.disconnect();session.close();}
						HibernateUtil.closeSession();

						if (rencanaGajiPunyaPegawai.getPegawai() != null
								&& rencanaGajiPunyaPegawai.getPegawai().getFormatItemGaji() != null) {
							RencanaItemGajiPegawaiTreeModel rencanaItemGajiPegawaiTreeModel = new RencanaItemGajiPegawaiTreeModel(
									false, rencanaGajiPunyaPegawai);
							try {
								rencanaItemGajiPegawaiTreeModel.reset(WaktuUtil.getDate(), null,
										rencanaGaji.getTahun());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								ais.common.Common.tampilErrorJikaAdmin(e);
							}

						}
					}

					label.setValue("");
									} finally {
						ais.database.hibernate.HibernateUtil.closeSession();
					}
				}
			}).start();

		}

		return true;
	}

	public Boolean check() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(RencanaGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("tahun", thnByr.getValue()))
				.add(this.rencanaGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.rencanaGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
