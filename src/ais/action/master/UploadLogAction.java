package ais.action.master;

import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.UploadLogInfo;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

import org.zkoss.zul.Html;
import ais.action.master.helper.GenericActionDashboardHelper;
public class UploadLogAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	
	private Html dashboardHtml;
	private Html progressHtml;
private Textbox searchnama;
	private MyDatebox start;
	private MyDatebox end;

	private boolean delete = false;
	private String className = null;
	private MyToolbarbuttonConfig add;
	private Tbmuser tbmuser = null;

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
		refreshDashboardSafe();

		if (execution.getParameter("className") != null) {
			className = execution.getParameter("className");
		}

		tbmuser = Common.getCurrentUser();

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

		String[] contents = new String[] { "id", "nama", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	}
	private void refreshDashboardSafe() {
		try {
			GenericActionDashboardHelper.refresh(dashboardHtml, progressHtml, UploadLogInfo.class,
					"Dasbor Catatan Upload", "Pantauan upload data, pengguna pengunggah, dan aktivitas terbaru untuk membantu memeriksa proses unggah file.");
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/UploadLogAction.java:109");
			}
		}
	}



	class UploadLogRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final UploadLogInfo uploadLog = (UploadLogInfo) arg1;
			new Label(Common.dateFormat5.get().format(uploadLog.getTanggal_dirubah())).setParent(arg0);

			if (uploadLog.getDiuploadOleh() != null) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(uploadLog.getDiuploadOleh()).setParent(vbox);
				new Label(uploadLog.getDiuploadOleh().getUserNama()).setParent(vbox);
			} else {
				new Label(uploadLog.getOlehId()).setParent(arg0);
			}

			RevisiHelper.createNewRevisi(UploadLogInfo.class, uploadLog, uploadLog.getNama()).setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Download", "/img/upload.gif");
			button.setTooltiptext("Download Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					String url = Common.getRequestHostWithProtocol() + "/AmbilFileServer?file="
							+ URLEncoder.encode(uploadLog.getKeterangan(), "UTF-8");

					Executions.getCurrent().sendRedirect(url, "_blank");
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
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

											Common.refreshDelete(uploadLog);

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
			toolbar.setParent(arg0);
			button.setParent(toolbar);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(UploadLogInfo.class);

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(tbmuser == null ? Restrictions.sqlRestriction("false")
				: Restrictions.or(Restrictions.isNull("diuploadOleh"), Restrictions.eq("diuploadOleh", tbmuser)))
				.add(className != null && !className.trim().isEmpty() ? Restrictions.eq("className", className)
						: Restrictions.sqlRestriction("true"))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(start == null || start.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) >= ('"
								+ Common.databaseDateFormat.get().format(start.getValue()) + " 00:00:00')"))

				.add(end == null || end.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("(this_.tanggal_dirubah) <= ('"
								+ Common.databaseDateFormat.get().format(end.getValue()) + " 23:59:59')"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		GenericActionDashboardHelper.showProgress(progressHtml, 15, "Memuat data", "Membaca data sesuai filter yang aktif.");
		Common.initPaging(initCriteria(false), paging);

		List<UploadLogInfo> uploadLog = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(uploadLog);
		grid.setRowRenderer(new UploadLogRenderer());
		grid.setModelCheckMobile(strset);
		refreshDashboardSafe();
		GenericActionDashboardHelper.hideProgress(progressHtml);
	}

}
