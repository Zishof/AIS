package ais.action.master.koperasi;

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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class TransaksiKoperasiDetailAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private boolean edit = false;
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

		if (add != null) { add.setVisible(false); }
		if (add != null) { add.setTooltiptext("Tambah"); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		String[] contents = new String[] { "id", "transaksiKoperasi.kode", "transaksiKoperasi.produkKoperasi.nama",
				"transaksiKoperasi.anggotaKoperasi.nama", "transaksiKoperasi.caraPembayaranKoperasi.nama", "ke",
				"tanggal", "pokok", "margin", "sisa", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(TransaksiKoperasiDetail.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

	}

	class TransaksiKoperasiDetailRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final TransaksiKoperasiDetail transaksiKoperasiDetail = (TransaksiKoperasiDetail) arg1;

			Vbox a;
			(a = RevisiHelper.createNewRevisi(TransaksiKoperasiDetail.class, transaksiKoperasiDetail,
					transaksiKoperasiDetail.getTransaksiKoperasi().getProdukKoperasi().getNama())).setParent(arg0);
			new Label(transaksiKoperasiDetail.getTransaksiKoperasi().getKode()).setParent(a);

			new Label(transaksiKoperasiDetail.getTransaksiKoperasi().getAnggotaKoperasi() == null ? ""
					: transaksiKoperasiDetail.getTransaksiKoperasi().getAnggotaKoperasi().getNama()).setParent(arg0);

			new Label(Common.numberFormat.get().format(transaksiKoperasiDetail.getKe())).setParent(arg0);
			new Label(Common.dateFormat4.get().format(transaksiKoperasiDetail.getTanggal())).setParent(arg0);
			new Label(Common.numberFormat.get().format(transaksiKoperasiDetail.getPokok())).setParent(arg0);
			new Label(Common.numberFormat.get().format(transaksiKoperasiDetail.getMargin())).setParent(arg0);
			new Label(Common.numberFormat.get().format(transaksiKoperasiDetail.getSisa())).setParent(arg0);
			new Label(transaksiKoperasiDetail.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(transaksiKoperasiDetail.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksiKoperasiDetail.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(transaksiKoperasiDetail);
				}
			});

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(TransaksiKoperasiDetail.class)
				.createAlias("transaksiKoperasi", "transaksiKoperasi")
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(
						Restrictions.ilike("transaksiKoperasi.nama", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("transaksiKoperasi.kode", searchnama.getValue().trim(),
								MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<TransaksiKoperasiDetail> transaksiKoperasiDetail = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(transaksiKoperasiDetail);
		grid.setRowRenderer(new TransaksiKoperasiDetailRenderer());
		grid.setModelCheckMobile(strset);

	}

}
