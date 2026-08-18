package ais.action.master.pkl;

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
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.PendaftarPklHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Pkl;
import ais.database.model.pkl.MahasiswaDaftarPkl;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class SeleksiPenerimaPklAction extends GenericAutowireComposer implements DataCriteria {

	/** 
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
	private boolean approve = false;

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
		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class PklRenderer extends ais.ui.util.MyRowRenderer {

		PendaftarPklHelper pendaftarPklHelper = new PendaftarPklHelper();

		// Peta ringkasan seleksi (id pkl -> {belum, diterima, ditolak, total}) yang SUDAH dihitung
		// SEKALI untuk seluruh halaman di onSearchDefault. Menggantikan 4 query COUNT per baris.
		private final java.util.Map<Long, int[]> petaInfo;

		PklRenderer(java.util.Map<Long, int[]> petaInfo) {
			this.petaInfo = petaInfo == null ? new java.util.HashMap<Long, int[]>() : petaInfo;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pkl pkl = (Pkl) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					// System.out.println("opened");
					pendaftarPklHelper.displayPrasyaratPkl(pkl, detail, addWindow, approve);
				}
			});

			RevisiHelper.createNewRevisi(Pkl.class, pkl, pkl.getNama_kelompok()).setParent(arg0);
			new Label(pkl.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_mulai()))
					.setParent(arg0);
			new Label(pkl.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_selesai()))
					.setParent(arg0);

			new Label(pkl.getFakultas() == null ? "" : pkl.getFakultas().getNama()).setParent(arg0);
			new Label(pkl.getJurusan() == null ? "" : pkl.getJurusan().getNama()).setParent(arg0);
			new Label(pkl.getProgram() == null || pkl.getProgram().trim().isEmpty() ? "Semua" : pkl.getProgram())
					.setParent(arg0);
			final Html html = new ais.ui.util.MyHtml();
			html.setParent(arg0);
			html.setContent(ais.common.helper.HitungSeleksiPenerimaHelper.htmlInformasi(
					pkl.getId() == null ? null : petaInfo.get(pkl.getId()), true));

		}

	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pkl.class);

		if (mahasiswa != null) {
			criteria = session.createCriteria(MahasiswaDaftarPkl.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.setProjection(Projections.property("kkn")).createCriteria("kkn");
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Pkl> pkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		// Satu query GROUP BY untuk seluruh baris halaman, bukan 4 query COUNT per baris.
		java.util.Map<Long, int[]> petaInfo = ais.common.helper.HitungSeleksiPenerimaHelper.hitung(
				HibernateUtil.currentSession(), "MahasiswaDaftarPkl", "pkl",
				ais.common.helper.HitungSeleksiPenerimaHelper.kumpulkanId(pkl));
		ListModel strset = new SimpleListModel(pkl);
		grid.setRowRenderer(new PklRenderer(petaInfo));
		grid.setModelCheckMobile(strset);

	}

}
