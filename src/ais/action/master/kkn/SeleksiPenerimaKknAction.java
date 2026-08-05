package ais.action.master.kkn;

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
import ais.action.master.helper.PendaftarKknHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Kkn;
import ais.database.model.Mahasiswa;
import ais.database.model.kkn.MahasiswaDaftarKkn;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class SeleksiPenerimaKknAction extends GenericAutowireComposer implements DataCriteria {

	/** 
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private boolean approve = false;
	private AmbilDataMahasiswaBanbox searchmahasiswa;
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
		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		// delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class KknRenderer extends ais.ui.util.MyRowRenderer {

		PendaftarKknHelper pendaftarKknHelper = new PendaftarKknHelper();

		// Peta ringkasan seleksi (id kkn -> {belum, diterima, ditolak, total}) yang SUDAH dihitung
		// SEKALI untuk seluruh halaman di onSearchDefault. Menggantikan 4 query COUNT per baris.
		private final java.util.Map<Long, int[]> petaInfo;

		KknRenderer(java.util.Map<Long, int[]> petaInfo) {
			this.petaInfo = petaInfo == null ? new java.util.HashMap<Long, int[]>() : petaInfo;
		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kkn kkn = (Kkn) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					// System.out.println("opened");
					pendaftarKknHelper.displayPrasyaratKkn(kkn, detail, addWindow, approve);
				}
			});

			RevisiHelper.createNewRevisi(Kkn.class, kkn, kkn.getNama_kelompok()).setParent(arg0);
			new Label(kkn.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_mulai()))
					.setParent(arg0);
			new Label(kkn.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(kkn.getTanggal_selesai()))
					.setParent(arg0);

			new Label(kkn.getFakultas() == null ? "" : kkn.getFakultas().getNama()).setParent(arg0);
			new Label(kkn.getJurusan() == null ? "" : kkn.getJurusan().getNama()).setParent(arg0);
			new Label(kkn.getProgram() == null || kkn.getProgram().trim().isEmpty() ? "Semua" : kkn.getProgram())
					.setParent(arg0);
			final Html html = new ais.ui.util.MyHtml();
			html.setParent(arg0);
			// Langsung dari peta yang sudah dihitung 1x untuk seluruh halaman -- tanpa query per baris.
			html.setContent(ais.common.helper.HitungSeleksiPenerimaHelper.htmlInformasi(
					kkn.getId() == null ? null : petaInfo.get(kkn.getId()), true));
		}

	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kkn.class);

		if (mahasiswa != null) {
			criteria = session.createCriteria(MahasiswaDaftarKkn.class).add(Restrictions.eq("mahasiswa", mahasiswa))
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

		List<Kkn> kkn = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		// Hitung ringkasan seleksi (belum/diterima/ditolak/total) untuk SEMUA baris halaman ini
		// dalam SATU query GROUP BY, bukan 4 query per baris (penyebab lambat).
		java.util.Map<Long, int[]> petaInfo = ais.common.helper.HitungSeleksiPenerimaHelper.hitung(
				HibernateUtil.currentSession(), "MahasiswaDaftarKkn", "kkn",
				ais.common.helper.HitungSeleksiPenerimaHelper.kumpulkanId(kkn));
		ListModel strset = new SimpleListModel(kkn);
		grid.setRowRenderer(new KknRenderer(petaInfo));
		grid.setModelCheckMobile(strset);

	}

}
