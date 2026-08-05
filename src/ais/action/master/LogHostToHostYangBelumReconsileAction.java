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
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.Kegiatan;
import ais.database.model.LogHostToHost;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class LogHostToHostYangBelumReconsileAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchbank;
	private Textbox searchkode;
	private Textbox searchketerangan;
	private Textbox searchnama;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

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

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "nama", "ip", "kode", "nominal", "keterangan",
				"nim", "bankHost", "tanggal", "responseCode", "responseDescription", "kegiatan", "transactionType",
				"request", "response", "info0", "info1", "info2", "info3");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class LogHostToHostRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final LogHostToHost logHostToHost = (LogHostToHost) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);
						new ais.ui.util.MyHtml(
								"<font>" + logHostToHost.getKeterangan() + "</font>")
								.setParent(detail);
					}
				}
			});

			new Label(logHostToHost.getIp()).setParent(arg0);
			new Label(logHostToHost.getBankHost() == null ? "Tidak terdaftar" : logHostToHost.getBankHost().getNama())
					.setParent(arg0);
			if (logHostToHost.getNim() != null) {
				new Label(logHostToHost.getNim()).setParent(arg0);
			} else {
				Kegiatan kegiatan = logHostToHost.getKegiatan();
				if (kegiatan != null) {
					if (kegiatan.getCalonMahasiswa() != null) {
						new Label(kegiatan.getCalonMahasiswa().getNoRegistrasi()).setParent(arg0);
					} else if (kegiatan.getMahasiswa() != null) {
						new Label(kegiatan.getMahasiswa().getNim()).setParent(arg0);
					}
				}
			}
			new Label(Common.dateFormat3.get().format(logHostToHost.getTanggal())).setParent(arg0);
			new Label(logHostToHost.getResponseDescription()).setParent(arg0);
			new Label(logHostToHost.getNama()).setParent(arg0);
			new Label(logHostToHost.getKode()).setParent(arg0);
			Double nilai = logHostToHost.getNominal();
			new Label(nilai == null ? "" : Common.numberFormat.get().format(nilai)).setParent(arg0);
			new Label(logHostToHost.getItem()).setParent(arg0);

			final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Diyatakan Gagal",
					"/img/svg/warning-outline.svg");
			button.setTooltiptext("Diyatakan Gagal");
			button.setOrient("vertical");

			List<CicilanPembayaran> cicilanPembayarans = null;

			Tbmuser tbmuser = Common.getCurrentUser();
			button.setVisible(tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
					&& tbmuser.hakAkses().getRoleId().trim().equalsIgnoreCase(Tbmrole.ADMINISTRATOR));

			if (button.isVisible()) {
				cicilanPembayarans = Common.ambilCicilanPembayarans(HibernateUtil.currentSession(), logHostToHost,
						logHostToHost.getKode(), logHostToHost.getNim(), logHostToHost.getTanggal());
				if (cicilanPembayarans.isEmpty()) {
					button.setVisible(false);
				}
			}

			final List<CicilanPembayaran> tempCicilanPembayarans = cicilanPembayarans;

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin bahwa transaksi ini dinyatakan gagal ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											if (tempCicilanPembayarans != null) {
												Session session = HibernateUtil.currentSession();
												for (CicilanPembayaran cicilanPembayaran : tempCicilanPembayarans) {

													CicilanPembayaranGagal cicilanPembayaranGagal = Common
															.copyCicilanPembayaranKeGagal(cicilanPembayaran);

													Common.refreshSaveOrUpdate(session, cicilanPembayaranGagal);

													session.createSQLQuery("delete from cicilan_pembayaran where id="
															+ cicilanPembayaran.getId()).executeUpdate();

												}
											}

											MyMessageboxConfig.show(
													"Transaksi ini telah dipindahkan ke transaksi gagal",
													"Pemberitahuan", MyMessageboxConfig.OK,
													MyMessageboxConfig.EXCLAMATION);

											button.setVisible(false);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat Reversal .., error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}

			});
			button.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(LogHostToHost.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.createAlias("bankHost", "bankHost", Criteria.LEFT_JOIN)
				.add(searchbank.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("bankHost.nama", searchbank.getValue(), MatchMode.ANYWHERE))

				.add(Restrictions.isNull("rekonsiliasiHostToHost"))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(Restrictions.eq("responseCode", "00")).add(Restrictions.eq("transactionType", ConstantUtil.PAY))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(searchketerangan.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<LogHostToHost> logHostToHost = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(logHostToHost);
		grid.setRowRenderer(new LogHostToHostRenderer());
		grid.setModelCheckMobile(strset);

	}

}
