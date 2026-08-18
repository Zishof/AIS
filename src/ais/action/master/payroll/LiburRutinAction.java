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
import org.zkoss.zul.Checkbox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.payroll.LiburRutin;
import ais.ui.util.MyTextbox;

public class LiburRutinAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Session session = HibernateUtil.currentSession();
		Integer count = ((Number) session.createCriteria(LiburRutin.class)
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (count.equals(0)) {
			int i = 1;
			for (String s : Common.haris) {
				LiburRutin liburRutin = new LiburRutin();
				liburRutin.setHari(i);
				liburRutin.setNama(s);
				liburRutin.setKeterangan("Hari " + s);
				liburRutin.setLibur(s.equals("Minggu") || s.equals("Sabtu"));
				session.save(liburRutin);
				i++;
			}
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class LiburRutinRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final LiburRutin liburRutin = (LiburRutin) arg1;

			RevisiHelper.createNewRevisi(LiburRutin.class, liburRutin,
					liburRutin.getNama()).setParent(arg0);
			final Checkbox libur = new Checkbox();
			libur.setChecked(liburRutin.getLibur());
			libur.setParent(arg0);
			libur.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					liburRutin.setLibur(libur.isChecked());
					Session session = HibernateUtil.currentSession();
					Common.refreshUpdate(session, (liburRutin));
				}
			});
			new Label(liburRutin.getKeterangan()).setParent(arg0);

		}

	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<LiburRutin> liburRutin = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(liburRutin);
		grid.setRowRenderer(new LiburRutinRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(LiburRutin.class);
		if (order)
			criteria.addOrder(Order.asc("hari"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("nama", searchnama.getValue(),
				MatchMode.ANYWHERE)));
		return criteria;
	}

}
