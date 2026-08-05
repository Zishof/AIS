package ais.action.master;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AbsensiKehadiranDosenHarianHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanAbsensiDosen;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonOnSearchdefault;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Tbmuser;
import ais.action.master.helper.FilterLanjutHelper;

public class AbsensKehadiranDosenHarianAction extends GenericAutowireComposer
		implements CommonOnSearchdefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyGrid grid;
	private Paging paging;
	private Textbox searchcode;
	private Textbox searchnama;
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	protected Tabpanel laporanAbsensiDosen;

	public void onLaporanAbsensiDosen(Event event) {

		if (laporanAbsensiDosen.getChildren().size() == 0) {
			LaporanAbsensiDosen laporan = new LaporanAbsensiDosen();
			laporan.setHeight("100%");
			laporan.setWidth("100%");
			laporan.setParent(this.laporanAbsensiDosen);
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null
				|| !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan); 

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;

			final AbsensiKehadiranDosenHarianHelper detail = new AbsensiKehadiranDosenHarianHelper(
					dosen);
			detail.setParent(arg0);
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser.ambilDosen() != null
					&& tbmuser.getDosen().getId() != null) {
				detail.setOpen(true);
				detail.display();
			}

			CommonMedia.tampilkanGambarKecil(dosen).setParent(arg0);
			

			new Label(dosen.getCode() == null ? "" : dosen.getCode())
					.setParent(arg0);
			
			new Label(dosen.getNidn() == null ? "" : dosen.getNidn())
					.setParent(arg0);
			RevisiHelper.createNewRevisi(Dosen.class, dosen, dosen.getNama())
					.setParent(arg0);
			new Label(dosen.getGolongan()).setParent(arg0);
			new Label((dosen.getIkatanKerjaDosen() == null ? "" : dosen
					.getIkatanKerjaDosen().getNama())
					+ " "
					+ (dosen.getStatusKepegawaian() == null ? "" : dosen
							.getStatusKepegawaian().getNama())).setParent(arg0);
			new Label(dosen.getEmail()).setParent(arg0);
			new Label(dosen.getTelp()).setParent(arg0);
			// new Label(dosen.getAlamat()).setParent(arg0);

			new Label(dosen.getPangkat()).setParent(arg0);
			new Label(dosen.getJabatan()).setParent(arg0);

			new Label(dosen.getFakultas() == null ? "" : dosen.getFakultas()
					.getNama()).setParent(arg0);

			new Label(dosen.getJurusan() == null ? "" : dosen.getJurusan()
					.getNama()).setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {
		Tbmuser tbmuser = Common.getCurrentUser();
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(
				searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions
						.sqlRestriction("1=1") : CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(Restrictions.ilike("nama", searchnama.getValue(),
						MatchMode.ANYWHERE))
				.add(Restrictions.ilike("code", searchcode.getValue(),
						MatchMode.ANYWHERE))

				.add(tbmuser.ambilDosen() == null
						|| tbmuser.getDosen().getId() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("id", tbmuser
						.getDosen().getId()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Dosen> dosen = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
