package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisPeredaranBuku;
import ais.database.model.ParameterUmum;
import ais.database.model.TahapanPenyusunanBuku;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDoublebox;

public class NilaiJenisPeredaranBukuAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

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

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class NilaiKegiatanKedosenanRenderer extends ais.ui.util.MyRowRenderer {

		private void tampilRow(Rows rows, JenisPeredaranBuku jenisPeredaranBuku,
				TahapanPenyusunanBuku tahapanPenyusunanBuku) {

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(tahapanPenyusunanBuku.getNama()));

			String key = "pengaturan_beban_sks_buku";
			String newKey = key + "_" + tahapanPenyusunanBuku.getId() + "_" + jenisPeredaranBuku.getId();

			final ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

			Double n = 0.0;
			try {
				n = Double.parseDouble(konfigurasi.getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/NilaiJenisPeredaranBukuAction.java:87");
				// TODO: handle exception
			}

			final MyDoublebox nilai = new MyDoublebox(n);
			nilai.setWidth("90%");

			row.appendChild(nilai);

			nilai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					konfigurasi.setNilai(nilai.getValue() == null ? "0.0" : nilai.getValue().toString());
					Common.refreshUpdate(konfigurasi);
				}
			});
		}

		List<TahapanPenyusunanBuku> tahapanPenyusunanBukus;

		@SuppressWarnings("unchecked")
		public NilaiKegiatanKedosenanRenderer() {
			tahapanPenyusunanBukus = HibernateUtil.currentSession().createCriteria(TahapanPenyusunanBuku.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("prosentase")).list();
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub

			final JenisPeredaranBuku jenisPeredaranBuku = (JenisPeredaranBuku) arg1;

			new Label(jenisPeredaranBuku.getNama()).setParent(arg0);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			for (TahapanPenyusunanBuku tahapanPenyusunanBuku : tahapanPenyusunanBukus) {
				tampilRow(rows, jenisPeredaranBuku, tahapanPenyusunanBuku);
			}
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPeredaranBuku.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPeredaranBuku> nilaiKegiatanKedosenan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilaiKegiatanKedosenan);
		grid.setRowRenderer(new NilaiKegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
