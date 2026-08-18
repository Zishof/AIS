package ais.action.master.helper;

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
import ais.ui.util.MyGrid;
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

import ais.action.master.MahasiswaAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataKegiatanForKegiatanKemahasiswaanHelper {

	private Mahasiswa mahasiswa;
	private MyGrid grid;

	private Textbox nama;
	// private Combobox searchjurusan;
	// private Combobox searchfakultas;

	private Paging paging;

	public AmbilDataKegiatanForKegiatanKemahasiswaanHelper(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final KegiatanKemahasiswaan kegiatanKemahasiswaan = (KegiatanKemahasiswaan) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("kegiatanKemahasiswaan", kegiatanKemahasiswaan);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(kegiatanKemahasiswaan.getNama()).setParent(arg0);
			new Label(kegiatanKemahasiswaan.getFakultas() == null ? "Semua"
					: kegiatanKemahasiswaan.getFakultas().getNama()).setParent(arg0);
			new Label(
					kegiatanKemahasiswaan.getJurusan() == null ? "Semua" : kegiatanKemahasiswaan.getJurusan().getNama())
					.setParent(arg0);
			new Label(kegiatanKemahasiswaan.getKelompokKegiatanKemahasiswaan().getNama()).setParent(arg0);
			new Label(kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan().getNama()).setParent(arg0);
			new Label(kegiatanKemahasiswaan.getKeterangan()).setParent(arg0);

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
				// Child pertama baris grid tidak selalu MyCheckboxConfig (mis. baris
				// header/separator tanpa checkbox tercampur dalam iterasi Rows). Cast
				// langsung ke MyCheckboxConfig pada child non-checkbox (mis. Label)
				// memicu ClassCastException. Skip baris yang bukan checkbox.
				Object firstChild = data.isEmpty() ? null : data.get(0);
				if (!(firstChild instanceof MyCheckboxConfig)) {
					continue;
				}
				MyCheckboxConfig checkbox = (MyCheckboxConfig) firstChild;
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					KegiatanKemahasiswaan kegiatanKemahasiswaan = (KegiatanKemahasiswaan) checkbox
							.getAttribute("kegiatanKemahasiswaan");
					KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) session
							.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
							.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan))
							.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
					if (kegiatanKemahasiswaanPunyaMahasiswa == null) {
						kegiatanKemahasiswaanPunyaMahasiswa = new KegiatanKemahasiswaanPunyaMahasiswa();
						kegiatanKemahasiswaanPunyaMahasiswa.setKegiatanKemahasiswaan(kegiatanKemahasiswaan);
						kegiatanKemahasiswaanPunyaMahasiswa.setOleh(tbmuser.getUserId());
						kegiatanKemahasiswaanPunyaMahasiswa.setTbmuser(tbmuser);
						kegiatanKemahasiswaanPunyaMahasiswa.setMahasiswa(mahasiswa);
						kegiatanKemahasiswaanPunyaMahasiswa.setDiubahDari(MahasiswaAction.class.getSimpleName());
						Common.refreshSaveOrUpdate(session, kegiatanKemahasiswaanPunyaMahasiswa);
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataKegiatanForKegiatanKemahasiswaanHelper.java:131");
				// TODO: handle exception
			}
		}

	}

	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Kegiatan Kemahasiswaan");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		// FIX (tombol Cari tidak muncul di desktop): North dengan flex TANPA tinggi bisa KOLAPS ke 0
		// pada ZK 5.5 saat Borderlayout dibangun dinamis, sehingga seluruh area pencarian
		// (kotak "Nama Kegiatan" + tombol Cari) ikut tersembunyi. Beri tinggi tetap + autoscroll agar
		// selalu tampil, baik di desktop maupun mobile.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("110px");
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataKegiatanForKegiatanKemahasiswaanHelper.java:230");

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
		Criteria criteria = session.createCriteria(KegiatanKemahasiswaan.class)
				.add(Restrictions.or(Restrictions.isNull("bolehDipilih"), Restrictions.eq("bolehDipilih", true)))
				.createAlias("kelompokKegiatanKemahasiswaan", "kelompokKegiatanKemahasiswaan")
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKemahasiswaan.bisaDipilihMahasiswa"),
						Restrictions.eq("kelompokKegiatanKemahasiswaan.bisaDipilihMahasiswa", true)))
				.add(Restrictions.or(Restrictions.isNull("kelompokKegiatanKemahasiswaan.aktif"),
						Restrictions.eq("kelompokKegiatanKemahasiswaan.aktif", true)))
				.add(Restrictions.eq("status", KegiatanKemahasiswaan.DISETUJUI));

		if (order)
			criteria.addOrder(Order.desc("mulai")).addOrder(Order.desc("id"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
