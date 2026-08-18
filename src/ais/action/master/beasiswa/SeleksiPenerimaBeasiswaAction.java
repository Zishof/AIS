package ais.action.master.beasiswa;

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
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.ui.util.MyWindow;

import ais.action.master.helper.PendaftarBeasiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.ui.util.DataCriteria;

public class SeleksiPenerimaBeasiswaAction extends GenericAutowireComposer implements DataCriteria {

	/** 
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private boolean approve;

	// private MyToolbarbuttonConfig find;
	
	

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
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE); 
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class BeasiswaRenderer extends ais.ui.util.MyRowRenderer {

		PendaftarBeasiswaHelper pendaftarBeasiswaHelper = new PendaftarBeasiswaHelper();

		// Peta ringkasan seleksi (id beasiswa -> {belum, diterima, ditolak, total}) yang SUDAH
		// dihitung SEKALI untuk seluruh halaman di onSearchDefault. Ganti 4 query COUNT per baris.
		private final java.util.Map<Long, int[]> petaInfo;

		BeasiswaRenderer(java.util.Map<Long, int[]> petaInfo) {
			this.petaInfo = petaInfo == null ? new java.util.HashMap<Long, int[]>() : petaInfo;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Beasiswa beasiswa = (Beasiswa) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					// System.out.println("opened");
					pendaftarBeasiswaHelper.displayPrasyaratBeasiswa(beasiswa, detail, addWindow, approve);
				}
			});

			new Label(beasiswa.getTahun() == null ? "" : beasiswa.getTahun().toString()).setParent(arg0);
			Vbox v = RevisiHelper.createNewRevisi(Beasiswa.class, beasiswa, beasiswa.getNama());
			v.setParent(arg0);
			v.appendChild(new Label(beasiswa.getTahunAkademik() + "/" + beasiswa.getSemester()));
			new Label(beasiswa.getTanggalBuka() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalBuka()))
					.setParent(arg0);
			new Label(beasiswa.getTanggalTutup() == null ? "" : Common.dateFormat2.get().format(beasiswa.getTanggalTutup()))
					.setParent(arg0);
			new Label(beasiswa.getInstansi()).setParent(arg0);

			final Html html = new ais.ui.util.MyHtml();
			html.setParent(arg0);
			html.setContent(ais.common.helper.HitungSeleksiPenerimaHelper.htmlInformasi(
					beasiswa.getId() == null ? null : petaInfo.get(beasiswa.getId()), false));

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Beasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(Restrictions.eq("dibukaUtkMahasiswa", 1));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Beasiswa> beasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		// Satu query GROUP BY untuk seluruh baris halaman, bukan 4 query COUNT per baris.
		java.util.Map<Long, int[]> petaInfo = ais.common.helper.HitungSeleksiPenerimaHelper.hitung(
				HibernateUtil.currentSession(), "MahasiswaDaftarBeasiswa", "beasiswa",
				ais.common.helper.HitungSeleksiPenerimaHelper.kumpulkanId(beasiswa));
		ListModel strset = new SimpleListModel(beasiswa);
		grid.setRowRenderer(new BeasiswaRenderer(petaInfo));
		grid.setModelCheckMobile(strset);

		

	}

}
