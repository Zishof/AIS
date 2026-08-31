package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.DosenAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.OrganisasiDosen;
import ais.database.model.OrganisasiDosenPunyaDosen;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK untuk mendaftarkan dosen ke dalam satu {@link OrganisasiDosen} (organisasi/himpunan
 * dosen). Pola dan struktur kelas ini identik dengan
 * {@link AmbilDataDosenForOrganisasiIntraKampusHelper} versi mahasiswa, hanya diterapkan pada
 * entitas {@link Dosen}: jendela pencarian menyaring dosen lewat NIDN, nama, Fakultas, dan Jurusan
 * (dosen dengan {@code milikUniversitas=true} lolos filter Fakultas/Jurusan apa pun karena berlaku
 * lintas unit), dengan dosen yang sudah tergabung ditandai tercentang dan dikunci.
 *
 * <p>
 * {@link #save()} menyimpan baris {@link OrganisasiDosenPunyaDosen} baru (atau memperbarui yang
 * sudah ada) untuk setiap dosen yang dicentang pada grid, tanpa syarat kelayakan tambahan (berbeda
 * dari varian mahasiswa yang mengecek syarat organisasi kemahasiswaan).
 * </p>
 */
public class AmbilDataDosenForOrganisasiDosenHelper {

	private OrganisasiDosen organisasiDosen;
	private MyGrid grid;

	private Textbox nidn;
	private Textbox nama;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	/** @param organisasiDosen organisasi dosen tujuan; dosen terpilih akan didaftarkan ke organisasi ini. */
	public AmbilDataDosenForOrganisasiDosenHelper(OrganisasiDosen organisasiDosen) {
		this.organisasiDosen = organisasiDosen;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	class DosenRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Dosen dosen = (Dosen) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(OrganisasiDosenPunyaDosen.class)
					.add(Restrictions.eq("dosen", dosen)).add(Restrictions.eq("organisasiDosen", organisasiDosen))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("dosen", dosen);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(dosen.getNidn()).setParent(arg0);
			new Label(dosen.getNama()).setParent(arg0);
			new Label(dosen.getIkatanKerjaDosen() == null ? "" : dosen.getIkatanKerjaDosen().getNama()).setParent(arg0);

		}
	}

	/**
	 * Menyimpan baris {@link OrganisasiDosenPunyaDosen} baru (atau memperbarui yang sudah ada) untuk
	 * setiap dosen yang dicentang (dan belum terkunci) pada grid.
	 *
	 * @throws InterruptedException tidak pernah dilempar dalam praktiknya; dipertahankan pada
	 *                              signature untuk kompatibilitas pemanggil
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();
		final Tbmuser tbmuser = Common.getCurrentUser();

		String warning = "";

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					Dosen dosen = (Dosen) checkbox.getAttribute("dosen");

					OrganisasiDosenPunyaDosen organisasiDosenPunyaDosen = (OrganisasiDosenPunyaDosen) session
							.createCriteria(OrganisasiDosenPunyaDosen.class).add(Restrictions.eq("dosen", dosen))
							.add(Restrictions.eq("organisasiDosen", organisasiDosen)).setMaxResults(1).uniqueResult();
					if (organisasiDosenPunyaDosen == null) {
						organisasiDosenPunyaDosen = new OrganisasiDosenPunyaDosen();
					}
					organisasiDosenPunyaDosen.setOrganisasiDosen(organisasiDosen);
					organisasiDosenPunyaDosen.setOleh(tbmuser.getUserId());
					organisasiDosenPunyaDosen.setTbmuser(tbmuser);
					organisasiDosenPunyaDosen.setDosen(dosen);
					organisasiDosenPunyaDosen.setDiubahDari(DosenAction.class.getSimpleName());
					Common.refreshSaveOrUpdate(session, organisasiDosenPunyaDosen);

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenForOrganisasiDosenHelper.java:133");
				// TODO: handle exception
			}
		}

		if (!warning.isEmpty()) {
			MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Membangun jendela pencarian dan pemilihan dosen untuk didaftarkan ke {@link #organisasiDosen}.
	 * Tombol Simpan memanggil {@link #save()} lalu menyegarkan tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param dataLoader dipanggil setelah simpan untuk menyegarkan tampilan daftar anggota organisasi
	 * @param window     jendela ({@link MyWindow}) yang dipakai ulang untuk menampilkan layar ini
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Dosen");
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
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);
		//
		//
		//
		//

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();
		columns.setParent(searchgrid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setWidth("35%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIDN"));
		row.appendChild(nidn = new Textbox());
		nidn.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Dosen"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

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

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataDosenForOrganisasiDosenHelper.java:274");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIDN");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Ikatan Kerja");
		column.setWidth("25%");

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

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				searchfakultas.setDisabled(false);
				searchjurusan.setDisabled(false);

			}
		});
	}

	/**
	 * Membangun kriteria Hibernate untuk pencarian dosen calon anggota, sesuai filter toolbar (NIDN,
	 * nama, Fakultas, Jurusan). Dosen dengan {@code milikUniversitas=true} selalu lolos filter
	 * Fakultas/Jurusan apa pun.
	 *
	 * @param order {@code true} untuk mengurutkan hasil berdasarkan nama ascending
	 * @return kriteria Hibernate siap eksekusi/paginasi
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Dosen.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(nidn.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nidn", nidn.getValue().trim(), MatchMode.ANYWHERE))
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(Restrictions.or(
						searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
								|| searchjurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false),
						Restrictions.eq("milikUniversitas", true)))

				.add(Restrictions.or(Restrictions.eq("milikUniversitas", true),
						searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
								|| searchfakultas.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("1=1")
										: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		return criteria;
	}

	/**
	 * Mengisi ulang grid hasil pencarian dosen (paginasi 50 baris per halaman) sesuai kriteria
	 * pencarian saat ini.
	 *
	 * @param event tidak dipakai isinya
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Dosen> dosen = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(dosen);
		grid.setRowRenderer(new DosenRenderer());
		grid.setModelCheckMobile(strset);

	}

}
