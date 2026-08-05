package ais.action.master.jatelindo;

import java.util.List;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.KegiatanTemporaryAction.DetailKegiatanTemporaryRenderer;
import ais.action.master.helper.RevisiHelper;
import ais.action.servlet.JatelindoCallback;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class JatelindoRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Combobox tahunAkademik;
	private Combobox status;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		TreeMap<String, String> statses = new TreeMap<String, String>();
		statses.put("0", "Belum diproses");
		statses.put("1", "Sedang diproses");
		statses.put("2", "Payment Sukses");
		statses.put("3", "Payment Gagal");
		statses.put("4", "Payment Reversal");
		statses.put("7", "Payment Expired");
		statses.put("8", "Payment Cancelled");
		statses.put("9", "Unknown");

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Status");
		if (comboitem != null) { comboitem.setValue(null); }
		status.appendChild(comboitem);
		if (status != null) { status.setSelectedItem(comboitem); }

		for (String kode : statses.keySet()) {
			comboitem = new MyComboitemConfig(statses.get(kode));
			comboitem.setValue(kode);
			status.appendChild(comboitem);
		}

		if (status != null) { status.setReadonly(true); }

		tbmuser = Common.getCurrentUser();
		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "url", "trxId", "merchant_id",
				"merchant", "response_code", "response_desc", "request", "response", "status", "kodeStatus",
				"mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan", "jadwalPembayaran", "semester", "tahunAkademik",
				"keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "jatelindoResponse", "amount",
				"biayaAdministrasi");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class JatelindoRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JatelindoRequest jatelindoRequest = (JatelindoRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						MyGrid grid = new MyGrid();
						grid.setParent(groupbox);

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Keterangan");
						column.setParent(columns);
						column.setWidth("80%");

						column = new MyColumnConfig("Nominal");
						column.setParent(columns);
						column.setWidth("20%");

						Rows rows = new Rows();
						rows.setParent(grid);

						HibernateUtil.currentSession().refresh(jatelindoRequest);
						if (!jatelindoRequest.getKegiatanTemporarys().isEmpty()) {
							List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.in("kegiatanTemporary", jatelindoRequest.getKegiatanTemporarys()))
									.list();

							ListModel strset = new SimpleListModel(cicilanPembayarans);
							grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
							grid.setModelCheckMobile(strset);
						} else {

							List<JatelindoRequestDetail> jatelindoRequestDetails = HibernateUtil.currentSession()
									.createCriteria(JatelindoRequestDetail.class).add(Restrictions.isNull("idCicilan"))
									.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).list();

							for (JatelindoRequestDetail jatelindoRequestDetail : jatelindoRequestDetails) {
								Row row = new Row();row.setValign("top");
								row.setParent(rows);

								RevisiHelper.createNewRevisi(JatelindoRequestDetail.class, jatelindoRequestDetail,
										jatelindoRequestDetail.getKeterangan()).setParent(row);

								row.appendChild(
										new Label(Common.numberFormat.get().format(jatelindoRequestDetail.getNilai())));
							}
						}
					}
				}
			});

			RevisiHelper.createNewRevisi(JatelindoRequest.class, jatelindoRequest, jatelindoRequest.getTrxId())
					.setParent(arg0);

			if (jatelindoRequest.getMahasiswa() != null) {
				new Label(jatelindoRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (jatelindoRequest.getBiodataCalonMahasiswa() != null) {
				new Label(jatelindoRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			}
			new Label(jatelindoRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(jatelindoRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jatelindoRequest.getAmount())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jatelindoRequest.getBiayaAdministrasi())).setParent(arg0);
			new Label(jatelindoRequest.getJenisKegiatan() == null ? ""
					: jatelindoRequest.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label(jatelindoRequest.getTahunAkademik() + "-" + jatelindoRequest.getSemester()).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(jatelindoRequest.getStatus()).setParent(hbox);

			if (Common.getApakahAdmin() && !jatelindoRequest.getStatus().equalsIgnoreCase("Payment Sukses")
					&& jatelindoRequest.getJatelindoResponse() != null) {
				MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin Cek Pembayaran data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {
												JatelindoCallback
														.prosesResponse(jatelindoRequest.getJatelindoResponse());
												onSearchDefault(event);
											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat Cek Pembayaran .., error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});
				button.setParent(hbox);
			}
		}

	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JatelindoRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.createAlias("jatelindoResponse", "jatelindoResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", searchnim.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("biodataCalonMahasiswa.noUjian", searchnim.getValue(),
												MatchMode.ANYWHERE))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kodeStatus", status.getSelectedItem().getValue()))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchtrxId.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("trxId", searchtrxId.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JatelindoRequest> jatelindoRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jatelindoRequest);
		grid.setRowRenderer(new JatelindoRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
