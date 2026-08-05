package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;

import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Mahasiswa;
import ais.database.model.PaketPerkuliahan;
import ais.database.model.StatusMahasiswa;

public class DetailPaketPerkuliahanHelper implements DataLoader {

	private MyGrid grid;
	private PaketPerkuliahan paketPerkuliahan;
	private Textbox nim;
	private Textbox nama;
	private Integer semester;

	public DetailPaketPerkuliahanHelper() {
	}

	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		// private boolean delete = false;

		public DetailPerkuliahanRenderer() {
			// delete =
			// CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Mahasiswa mahasiswa = (Mahasiswa) data;

			final MyDetail detail = new MyDetail();
			detail.setParent(row);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						final TampilStudiMahasiswaHelper tampilStudiMahasiswaHelper = new TampilStudiMahasiswaHelper(
								null, null, false, true);

						Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
						borderlayout.setParent(detail);
						borderlayout.setHeight("450px");

						North north = new North();
						north.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(north, true);

						Center center = new Center();
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);

						center.appendChild(tampilStudiMahasiswaHelper.initMain(mahasiswa, new DataLoader() {

							@Override
							public void loadData(Object value) {

							}
						}, north));
					}
				}
			});

			RevisiHelper.createNewRevisi(Mahasiswa.class, mahasiswa, mahasiswa.getNim()).setParent(row);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(row);
			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();

			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label("").setParent(row);

		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<Detailperkuliahan> detailperkuliahan = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.addOrder(Order.asc("mahasiswa.nim"))
				.add(Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("paketPerkuliahan", paketPerkuliahan)).add(Restrictions.eq("semester", semester))
				.list();

		List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
		for (Detailperkuliahan d : detailperkuliahan) {
			if (!mahasiswas.contains(d.getMahasiswa())) {
				mahasiswas.add(d.getMahasiswa());
			}
		}

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);

		mahasiswas = null;
		detailperkuliahan = null;
	}

	public void display(final PaketPerkuliahan paketPerkuliahan, final Integer semester, final MyWindow component) {
		this.paketPerkuliahan = paketPerkuliahan;
		this.semester = semester;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti paket perkuliahan " + paketPerkuliahan.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbar.appendChild(nim = new Textbox());

		nim.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil data Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyWindow window = new MyWindow("Mahasiswa", "none", true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setHeight("90%");
				window.setWidth("80%");

				AmbilDataMahasiswaForPaketPerkuliahanHelper dataMahasiswaHelper = new AmbilDataMahasiswaForPaketPerkuliahanHelper(
						paketPerkuliahan, null);
				dataMahasiswaHelper.display(DetailPaketPerkuliahanHelper.this, semester, window);
			}

		});
		button.setParent(toolbar);

		grid = new MyGrid();//grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(15);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Status");
		column.setWidth("10%");

		loadData(null);

	}

}
