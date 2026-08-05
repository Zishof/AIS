package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataKegiatanForKegiatanKesiswaanHelper {

	private Siswa siswa;
	private MyGrid grid;

	private Textbox nama;
	// private Combobox searchjurusan;
	// private Combobox searchfakultas;

	private Paging paging;

	public AmbilDataKegiatanForKegiatanKesiswaanHelper(Siswa siswa) {
		this.siswa = siswa;

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKesiswaan kegiatanKesiswaan = (KegiatanKesiswaan) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
					.add(Restrictions.eq("siswa", siswa)).add(Restrictions.eq("kegiatanKesiswaan", kegiatanKesiswaan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("kegiatanKesiswaan", kegiatanKesiswaan);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(kegiatanKesiswaan.getNama()).setParent(arg0);
			new Label(kegiatanKesiswaan.getYayasan() == null ? "Semua" : kegiatanKesiswaan.getYayasan().getNama())
					.setParent(arg0);
			new Label(kegiatanKesiswaan.getSekolah() == null ? "Semua" : kegiatanKesiswaan.getSekolah().getNama())
					.setParent(arg0);
			new Label(kegiatanKesiswaan.getKelompokKegiatanKesiswaan().getNama()).setParent(arg0);
			new Label(kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan().getNama()).setParent(arg0);
			new Label(kegiatanKesiswaan.getKeterangan()).setParent(arg0);

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		final Tbmuser tbmuser = Common.getCurrentUser();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					KegiatanKesiswaan kegiatanKesiswaan = (KegiatanKesiswaan) checkbox
							.getAttribute("kegiatanKesiswaan");
					KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) session
							.createCriteria(KegiatanKesiswaanPunyaSiswa.class)
							.add(Restrictions.eq("kegiatanKesiswaan", kegiatanKesiswaan))
							.add(Restrictions.eq("siswa", siswa)).setMaxResults(1).uniqueResult();
					if (kegiatanKesiswaanPunyaSiswa == null) {
						kegiatanKesiswaanPunyaSiswa = new KegiatanKesiswaanPunyaSiswa();
						kegiatanKesiswaanPunyaSiswa.setKegiatanKesiswaan(kegiatanKesiswaan);
						kegiatanKesiswaanPunyaSiswa.setOleh(tbmuser.getUserId());
						kegiatanKesiswaanPunyaSiswa.setTbmuser(tbmuser);
						kegiatanKesiswaanPunyaSiswa.setSiswa(siswa);
						kegiatanKesiswaanPunyaSiswa.setDiubahDari(SiswaAction.class.getSimpleName());
						Common.refreshSaveOrUpdate(session, kegiatanKesiswaanPunyaSiswa);
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataKegiatanForKegiatanKesiswaanHelper.java:129");
				// TODO: handle exception
			}
		}

	}

	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Kegiatan Kesiswaan");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kegiatan"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(myCenter1);

		paging.setParent(mySouth);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataKegiatanForKegiatanKesiswaanHelper.java:231");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Aspek");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rincian Aspek");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("20%");

		onSearchDefault(null);

		South south = new South();
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		// button = new MyToolbarbuttonConfig("Ambil Semua", "/img/save.gif");
		// button.setTooltiptext("Simpan");
		// button.addEventListener("onClick", new EventListener() {
		// @Override
		// public void onEvent(Event event) throws Exception {
		// saveSemua();
		// dataLoader.loadData(null);
		// window.setVisible(false);
		// }
		// });
		// button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKesiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("bolehDipilih"), Restrictions.eq("bolehDipilih", true)))
				.createAlias("kelompokKegiatanKesiswaan", "kelompokKegiatanKesiswaan")
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKesiswaan.bisaDipilihSiswa"),
						Restrictions.eq("kelompokKegiatanKesiswaan.bisaDipilihSiswa", true)))
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKesiswaan.aktif"),
						Restrictions.eq("kelompokKegiatanKesiswaan.aktif", true)))
				.add(Restrictions.eq("status", KegiatanKesiswaan.DISETUJUI));

		if (order)
			criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("id"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
