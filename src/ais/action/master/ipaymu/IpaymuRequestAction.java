package ais.action.master.ipaymu;

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
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import ais.ui.util.MyToolbarbuttonConfig;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyLabelKecil;

public class IpaymuRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Textbox searchbuyer;
	private Combobox tahunAkademik;
	private Textbox status;

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

		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "url", "trxId", "product", "merchant",
				"buyer", "noRekeningDeposit", "comments", "mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan",
				"jadwalPembayaran", "semester", "tahunAkademik", "keterangan", "pengurangan", "nilaiBiayaHarusDiBayars",
				"ipaymuResponse");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class IpaymuRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final IpaymuRequest ipaymuRequest = (IpaymuRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						List<IpaymuRequestDetail> ipaymuRequestDetails = HibernateUtil.currentSession()
								.createCriteria(IpaymuRequestDetail.class).add(Restrictions.isNull("idCicilan"))
								.add(Restrictions.eq("ipaymuRequest", ipaymuRequest)).list();

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
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
						for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig(ipaymuRequestDetail.getKeterangan()));
							row.appendChild(new ais.ui.util.MyLabelConfig(Common.numberFormat.get().format(ipaymuRequestDetail.getNilai())));
						}
					}
				}
			});

			new Label(ipaymuRequest.getNoRekeningDeposit() + " / " + ipaymuRequest.getBuyer()).setParent(arg0);
			new Label(ipaymuRequest.getComments()).setParent(arg0);
			if (ipaymuRequest.getMahasiswa() != null) {
				new Label(ipaymuRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (ipaymuRequest.getBiodataCalonMahasiswa() != null) {
				new Label(ipaymuRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			}
			new Label(ipaymuRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(ipaymuRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(ipaymuRequest.getAmount())).setParent(arg0);
			new Label(
					ipaymuRequest.getJenisKegiatan() == null ? "" : ipaymuRequest.getJenisKegiatan().getNamaKegiatan())
							.setParent(arg0);
			new Label(ipaymuRequest.getTahunAkademik() + "-" + ipaymuRequest.getSemester()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(ipaymuRequest.getIpaymuResponse() == null ? "" : ipaymuRequest.getIpaymuResponse().getStatus())
					.setParent(vbox);
			new MyLabelKecil(
					ipaymuRequest.getIpaymuResponse() == null ? "" : ipaymuRequest.getIpaymuResponse().getKeterangan())
							.setParent(vbox);
			new Label(ipaymuRequest.getIpaymuResponse() == null ? "" : ipaymuRequest.getIpaymuResponse().getComments())
					.setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(IpaymuRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.createAlias("ipaymuResponse", "ipaymuResponse", Criteria.LEFT_JOIN)
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

				.add(status.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("ipaymuResponse.resultCode", status.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchbuyer.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("buyer", searchbuyer.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("noRekeningDeposit", searchbuyer.getValue(), MatchMode.ANYWHERE)))
				.add(searchtrxId.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("trxId", searchtrxId.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<IpaymuRequest> ipaymuRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(ipaymuRequest);
		grid.setRowRenderer(new IpaymuRequestRenderer());
		grid.setModelCheckMobile(strset);

		

	}

}
