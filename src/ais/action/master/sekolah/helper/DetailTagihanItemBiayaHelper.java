package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class DetailTagihanItemBiayaHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private PengaturanBiaya pengaturanBiaya;

	private Textbox nama;

	private Paging paging;

	public DetailTagihanItemBiayaHelper() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(arg0);
			}
		});
	}

	class DetailPARenderer extends ais.ui.util.MyRowRenderer {

		public DetailPARenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) data;

			String kodeUnik = NominalBiaya.genCode(pengaturanBiayaItemBiaya.getItemBiayaSekolah(), pengaturanBiaya,
					null, null);

			Session session = HibernateUtil.currentSession();
			NominalBiaya nominalBiaya = (NominalBiaya) session.createCriteria(NominalBiaya.class)
					.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
			if (nominalBiaya == null) {
				nominalBiaya = new NominalBiaya();
				nominalBiaya.setNominal(pengaturanBiayaItemBiaya.getDefaultBiaya());
				nominalBiaya.setItemBiayaSekolah(pengaturanBiayaItemBiaya.getItemBiayaSekolah());
				nominalBiaya.setPengaturanBiaya(pengaturanBiayaItemBiaya.getPengaturanBiaya());
				nominalBiaya.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
				session.save(nominalBiaya);
			}

			if (nominalBiaya.getPengaturanBiayaItemBiaya() == null) {
				nominalBiaya.setPengaturanBiayaItemBiaya(pengaturanBiayaItemBiaya);
				session.update(nominalBiaya);
			}

			RevisiHelper.createNewRevisi(PengaturanBiayaItemBiaya.class, pengaturanBiayaItemBiaya,
					pengaturanBiayaItemBiaya.getItemBiayaSekolah().getNama()).setParent(row);

			final NominalBiaya nb = nominalBiaya;
			final MyDoublebox nilai = new MyDoublebox(nominalBiaya.getNominal());
			nilai.setWidth("90%");

			if (nominalBiaya.getPengaturanBiayaItemBiaya() != null
					&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() != null
					&& nominalBiaya.getPengaturanBiayaItemBiaya().getMinimalBiaya() != null
					&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya() > 0.1
					&& nominalBiaya.getPengaturanBiayaItemBiaya().getMaksimalBiaya().intValue() == nominalBiaya
							.getPengaturanBiayaItemBiaya().getMinimalBiaya().intValue()) {
				new Label(Common.numberFormat.get().format(nominalBiaya.getNominal())).setParent(row);
			}

			else if (nominalBiaya.getItemBiayaSekolah().getParameterTambahan() != null) {
				new Label(Common.numberFormat.get().format(nb.getNominal())).setParent(row);
			} else {
				nilai.setParent(row);
			}

			nilai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					nb.setNominal(nilai.getValue());
					Common.refreshUpdate(nb);
				}
			});

		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PengaturanBiayaItemBiaya.class)
				.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
				.add(Restrictions.eq("itemBiayaSekolah.aktif", true))
				.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("itemBiayaSekolah.nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		if (order) {
			criteria.addOrder(Order.desc("itemBiayaSekolah.nama"));
		}

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPaging(initCriteria(false), paging);
		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new DetailPARenderer());
		grid.setModelCheckMobile(strset);

	}

	public void displayDetailPA(final PengaturanBiaya pengaturanBiaya, final Component component,
			final MyWindow window) {

		this.pengaturanBiaya = pengaturanBiaya;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nominal");
		column.setWidth("15%");

		loadData(null);

	}

}
