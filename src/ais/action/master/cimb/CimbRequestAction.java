package ais.action.master.cimb;

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

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyToolbarbuttonConfig;

public class CimbRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Combobox tahunAkademik;

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

		tbmuser = Common.getCurrentUser();
		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "trxId", "status", "kodeStatus",
				"mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan", "jadwalPembayaran", "semester", "tahunAkademik",
				"keterangan", "pengurangan", "nilaiBiayaHarusDiBayars");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class CimbRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final CimbRequest cimbRequest = (CimbRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						List<CimbRequestDetail> cimbRequestDetails = HibernateUtil.currentSession()
								.createCriteria(CimbRequestDetail.class).add(Restrictions.isNull("idCicilan"))
								.add(Restrictions.eq("cimbRequest", cimbRequest)).list();

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
						for (CimbRequestDetail cimbRequestDetail : cimbRequestDetails) {
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig(cimbRequestDetail.getKeterangan()));
							row.appendChild(new ais.ui.util.MyLabelConfig(
									Common.numberFormat.get().format(cimbRequestDetail.getNilai())));
						}
					}
				}
			});

			new Label(cimbRequest.getTrxId()).setParent(arg0);
			if (cimbRequest.getMahasiswa() != null) {
				new Label(cimbRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (cimbRequest.getBiodataCalonMahasiswa() != null) {
				new Label(cimbRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			}
			new Label(cimbRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(cimbRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(cimbRequest.getAmount())).setParent(arg0);
			new Label(cimbRequest.getJenisKegiatan() == null ? "" : cimbRequest.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			new Label(cimbRequest.getTahunAkademik() + "-" + cimbRequest.getSemester()).setParent(arg0);

		}

	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CimbRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.createAlias("cimbResponse", "cimbResponse", Criteria.LEFT_JOIN)
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

		.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchtrxId.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("trxId", searchtrxId.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CimbRequest> cimbRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(cimbRequest);
		grid.setRowRenderer(new CimbRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
