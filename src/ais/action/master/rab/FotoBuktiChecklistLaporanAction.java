package ais.action.master.rab;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.FotoBuktiChecklistLaporan;
import ais.database.model.rab.ChecklistLaporanDetail;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class FotoBuktiChecklistLaporanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private boolean delete = false;
	private ChecklistLaporanDetail checklistLaporanDetail;

	private MyToolbarbuttonConfig add;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		checklistLaporanDetail = (ChecklistLaporanDetail) session.getAttribute("checklistLaporanDetail");
		session.removeAttribute("checklistLaporanDetail");

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class FotoBuktiChecklistLaporanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FotoBuktiChecklistLaporan fotoBuktiChecklistLaporan = (FotoBuktiChecklistLaporan) arg1;

			RevisiHelper.createNewRevisi(FotoBuktiChecklistLaporan.class, fotoBuktiChecklistLaporan,
					fotoBuktiChecklistLaporan.getNama()).setParent(arg0);
			new Label(fotoBuktiChecklistLaporan.getKeterangan()).setParent(arg0);

			MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(fotoBuktiChecklistLaporan.getNama(),
					fotoBuktiChecklistLaporan.iconDonwload());
			toolbarbutton.setOrient("vertical");
			toolbarbutton.setParent(arg0);
			toolbarbutton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						Session session = StreamingHibernateUtil.getInstance().currentSession();

						FotoBuktiChecklistLaporan myFotoBuktiChecklistLaporan = (FotoBuktiChecklistLaporan) session
								.createCriteria(FotoBuktiChecklistLaporan.class)
								.add(Restrictions.idEq(fotoBuktiChecklistLaporan.getId())).uniqueResult();
						Filedownload.save(CommonMedia.getFileFotoLangsungOld(myFotoBuktiChecklistLaporan, false),
								myFotoBuktiChecklistLaporan.getKeterangan());

						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
						Common.tampilErrorJikaAdmin(e);
					}

				}

			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = StreamingHibernateUtil.getInstance().currentSession();
											session.getTransaction().begin();
											Common.refreshDelete((fotoBuktiChecklistLaporan));
											session.getTransaction().commit();
											StreamingHibernateUtil.getInstance().closeSession();
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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

	public Criteria initCriteria(boolean order) {
		Session session = StreamingHibernateUtil.getInstance().currentSession();
		Criteria criteria = session.createCriteria(FotoBuktiChecklistLaporan.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("checklistLaporanDetail", checklistLaporanDetail.getId()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FotoBuktiChecklistLaporan> fotoBuktiChecklistLaporan = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(fotoBuktiChecklistLaporan);
		grid.setRowRenderer(new FotoBuktiChecklistLaporanRenderer());
		grid.setModelCheckMobile(strset);

		StreamingHibernateUtil.getInstance().closeSession();

	}

	public void onUploadFile(ForwardEvent event) throws Exception {
		UploadEvent uploadEvent = (UploadEvent) event.getOrigin();
		Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
		FotoBuktiChecklistLaporan fotoBuktiChecklistLaporan = new FotoBuktiChecklistLaporan();
		fotoBuktiChecklistLaporan.setChecklistLaporanDetail(checklistLaporanDetail.getId());
		fotoBuktiChecklistLaporan.setNama(uploadEvent.getMedia().getName());
		fotoBuktiChecklistLaporan.setKeterangan(uploadEvent.getMedia().getContentType());
		fotoBuktiChecklistLaporan.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));
		streamingSession.getTransaction().begin();
		streamingSession.save(fotoBuktiChecklistLaporan);
		streamingSession.getTransaction().commit();
		StreamingHibernateUtil.getInstance().closeSession();
		onSearchDefault(event);

	}

}
