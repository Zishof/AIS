package ais.action.master.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DosenMengajarDetailperkuliahanHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Perkuliahan perkuliahan;
	private Textbox nim;
	private Textbox nama;
	private boolean ispaging = false;
	private Paging paging;

	public DosenMengajarDetailperkuliahanHelper() {

	}

	public DosenMengajarDetailperkuliahanHelper(boolean ispaging) {
		this.ispaging = ispaging;
		if (ispaging) {
			paging = new Paging();
			Common.initPaging(paging, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(arg0);
				}
			});
		}
	}

	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) data;

			CommonMedia.tampilkanGambarKecil(detailperkuliahan.getMahasiswa()).setParent(row);

			RevisiHelper.createNewRevisi(Detailperkuliahan.class, detailperkuliahan,
					detailperkuliahan.getMahasiswa().getNim()).setParent(row);

			new Label(detailperkuliahan.getMahasiswa().getNama()).setParent(row);
			new Label(detailperkuliahan.getMahasiswa().getTahunangkatan() + " / "
					+ detailperkuliahan.getMahasiswa().getSemesterMulai()).setParent(row);

			new Label(detailperkuliahan.getTotalNilai() == null ? "0.0 (Belum dinilai)"
					: Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " ("
							+ (detailperkuliahan.getNilaiHuruf() == null
									|| detailperkuliahan.getNilaiHuruf().trim().equals("") ? "Belum dinilai"
											: detailperkuliahan.getNilaiHuruf())
							+ ")").setParent(row);

			final Label semester = new Label(
					detailperkuliahan.getSemester() == null ? "" : detailperkuliahan.getSemester().toString());
			semester.setParent(row);

		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Detailperkuliahan.class)
				.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI));

		criteria.add(Restrictions.isNull("ikutiPerkuliahan")).createAlias("mahasiswa", "mahasiswa")
				.add(nim == null || nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama == null || nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.eq("perkuliahan", perkuliahan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		if (ispaging) {
			Common.initPaging(initCriteria(false), paging);
			List<Detailperkuliahan> myDetailperkuliahans = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(myDetailperkuliahans);
			grid.setRowRenderer(new DetailPerkuliahanRenderer());
			grid.setModelCheckMobile(strset);
		} else {

			List<Detailperkuliahan> myDetailperkuliahans = initCriteria(true).list();
			ListModel strset = new SimpleListModel(myDetailperkuliahans);
			grid.setRowRenderer(new DetailPerkuliahanRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

	public void setPerkuliahan(Perkuliahan perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	public void display(final Perkuliahan perkuliahan, final Component component) {
		this.perkuliahan = perkuliahan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar mahasiswa yang mengikuti perkuliahan " + perkuliahan.toString()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setCols(10);
		nim.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
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

		button = new MyToolbarbuttonConfig("Absensi", "/img/print.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				CommonReportHelper.onLaporanAbsensi(perkuliahan, false);
			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Nilai", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				DetailperkuliahanForPenilaianHelper.onLaporan(perkuliahan);
			}
		});
		print.setParent(toolbar);

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "perkuliahan", "mahasiswa", "semester",
				"tahunAkademik", "persetujuan");
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setStyle("min-height: 200px;");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

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
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Semester");
		column.setWidth("10%");

		if (ispaging) {
			groupbox.appendChild(paging);
		}

		loadData(null);

	}

}
