package ais.action.master.penelitiandanpengabdian;

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
import ais.database.model.ParameterUmum;
import ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian;
import ais.database.model.penelitiandanpengabdian.TahapanPelaporanPenelitianDanPengabdian;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyDoublebox;

public class NilaiTahapanPelaporanPenelitianDanPengabdianAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault {

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

		private void tampilRow(Rows rows, PenelitianDanPengabdian penelitianDanPengabdian,
				TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian) {

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			row.appendChild(new Label(tahapanPelaporanPenelitianDanPengabdian.getNama()));

			String key = "pengaturan_beban_sks_penelitian_dan_pengabdian";
			String newKey = key + "_" + penelitianDanPengabdian.getId() + "_"
					+ tahapanPelaporanPenelitianDanPengabdian.getId();

			final ParameterUmum konfigurasi = Common.getParameterUmum(newKey, "0.0");

			Double n = 0.0;
			try {
				n = Double.parseDouble(konfigurasi.getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/penelitiandanpengabdian/NilaiTahapanPelaporanPenelitianDanPengabdianAction.java:89");
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

		public NilaiKegiatanKedosenanRenderer() {

		}

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub

			final PenelitianDanPengabdian penelitianDanPengabdian = (PenelitianDanPengabdian) arg1;

			new Label(penelitianDanPengabdian.getJudul()).setParent(arg0);

			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(arg0);
			grid.setWidth("100%");
			grid.setHeight("100%");

			Rows rows = new Rows();
			rows.setParent(grid);

			List<TahapanPelaporanPenelitianDanPengabdian> tahapanPelaporanPenelitianDanPengabdians = HibernateUtil
					.currentSession().createCriteria(TahapanPelaporanPenelitianDanPengabdian.class)
					.add(Restrictions.eq("penelitianDanPengabdian", penelitianDanPengabdian))
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.addOrder(Order.asc("mulai")).list();
			for (TahapanPelaporanPenelitianDanPengabdian tahapanPelaporanPenelitianDanPengabdian : tahapanPelaporanPenelitianDanPengabdians) {
				tampilRow(rows, penelitianDanPengabdian, tahapanPelaporanPenelitianDanPengabdian);
			}
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenelitianDanPengabdian.class)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));

		if (order)
			criteria.addOrder(Order.desc("tanggalMulaiPengajuan"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("judul", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PenelitianDanPengabdian> nilaiKegiatanKedosenan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(nilaiKegiatanKedosenan);
		grid.setRowRenderer(new NilaiKegiatanKedosenanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
