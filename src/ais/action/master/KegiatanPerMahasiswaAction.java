package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.CheckMahasiswaPanel;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogHostToHost;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KegiatanPerMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;
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
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class DetailKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final DetailKegiatan detailKegiatan = (DetailKegiatan) arg1;
			new Label(detailKegiatan.getDetailBiaya() == null || detailKegiatan.getDetailBiaya().getItemBiaya() == null
					? ""
					: detailKegiatan.getDetailBiaya().getItemBiaya().getId() + "").setParent(arg0);
			new Label(detailKegiatan.getDetailBiaya() == null || detailKegiatan.getDetailBiaya().getItemBiaya() == null
					? ""
					: detailKegiatan.getDetailBiaya().getItemBiaya().getNama() + "").setParent(arg0);
			new Label(detailKegiatan.getDetailBiaya() == null ? ""
					: Common.numberFormat.get().format(detailKegiatan.getBiaya())).setParent(arg0);

		}
	}

	class KegiatanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Kegiatan kegiatan = (Kegiatan) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						CheckMahasiswaPanel checkMahasiswaPanel = new CheckMahasiswaPanel(kegiatan);
						checkMahasiswaPanel.setParent(detail);
					}
				}
			});

			new Label(kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()).setParent(arg0);
			new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNim()).setParent(arg0);
			new Label(kegiatan.getMahasiswa() == null ? "" : kegiatan.getMahasiswa().getNama()).setParent(arg0);
			new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null ? ""
					: kegiatan.getMahasiswa().getJurusan().getNama()).setParent(arg0);
			new Label(kegiatan.getMahasiswa() == null || kegiatan.getMahasiswa().getJurusan() == null
					|| kegiatan.getMahasiswa().getJurusan().getFakultas() == null ? ""
							: kegiatan.getMahasiswa().getJurusan().getFakultas().getNama()).setParent(arg0);
			new Label(kegiatan.getSemster() + "").setParent(arg0);
			new Label(Common.dateFormat3.get().format(kegiatan.getTanggal())).setParent(arg0);
			new Label(kegiatan.getJenisKegiatan() == null ? "" : kegiatan.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Lihat Log", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Lihat Log Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					final MyWindow window = new MyWindow();
					window.setHeight("400px");
					window.setWidth("300px");
					window.setClosable(true);
					Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
					borderlayout.setParent(window);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);

					Session session = HibernateUtil.currentSession();
					LogHostToHost logHostToHost = (LogHostToHost) session.createCriteria(LogHostToHost.class)
							.add(Restrictions.eq("kegiatan", kegiatan)).setMaxResults(1).uniqueResult();
					if (logHostToHost != null) {
						new ais.ui.util.MyHtml("<font>Request:<br>" + logHostToHost.getNama()
								+ "<br><br>Response:<br>" + logHostToHost.getKeterangan() + "</font>")
										.setParent(center);
					}

					South south = new South();
					south.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(south, true);
					Toolbar toolbar = new Toolbar();
					// toolbar.setHeight("25px");
					toolbar.setParent(south);
					MyButtonConfig button = new MyButtonConfig("Tutup");
					button.setParent(toolbar);
					button.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});

					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setVisible(true);
					window.onModal();
				}
			});
			button.setParent(toolbar);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Kegiatan.class).add(Restrictions.eq("aktif", true)).add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		if (order)
			criteria.addOrder(Order.desc("id")).createCriteria("mahasiswa")
					.add(Restrictions.eq("nim", searchnama.getValue().trim()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Kegiatan> kegiatan = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kegiatan);
		grid.setRowRenderer(new KegiatanRenderer());
		grid.setModelCheckMobile(strset);

	}

}
