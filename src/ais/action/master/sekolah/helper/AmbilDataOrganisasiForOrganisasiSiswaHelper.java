package ais.action.master.sekolah.helper;


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

import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.OrganisasiSiswa;
import ais.database.model.sekolah.OrganisasiSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper jendela pemilihan ("picker") organisasi siswa ({@link OrganisasiSiswa}) untuk didaftarkan
 * sebagai keanggotaan {@code siswa} — menampilkan daftar organisasi (dapat difilter sekolah/yayasan
 * dan kata kunci nama) dengan checkbox per baris; organisasi yang sudah pernah didaftarkan untuk
 * siswa tersebut ditandai dan dikunci (tidak dapat dicentang ulang) untuk mencegah duplikasi.
 *
 * <p>
 * {@link #display(DataLoader, MyWindow)} membangun jendela lengkap (filter + grid + tombol
 * Simpan/Batal) dan memasangnya ke {@code window}; tombol Simpan memanggil {@link #save()} yang
 * membuat baris {@link OrganisasiSiswaPunyaSiswa} baru untuk setiap organisasi yang dicentang
 * (dan belum terdaftar sebelumnya), mencatat pengguna yang menambahkan
 * ({@code oleh}/{@code tbmuser}) dan asal perubahan ({@code diubahDari="SiswaAction"}), lalu
 * memanggil {@code dataLoader} untuk menyegarkan tampilan pemanggil.
 * </p>
 */
public class AmbilDataOrganisasiForOrganisasiSiswaHelper {

	private Siswa siswa;
	private MyGrid grid;

	private Textbox nama;
	private Combobox searchsekolah;
	private Combobox searchyayasan;

	private Paging paging;

	/** Membuat helper untuk memilih organisasi yang akan didaftarkan ke {@code siswa}. */
	public AmbilDataOrganisasiForOrganisasiSiswaHelper(Siswa siswa) {
		this.siswa = siswa;

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataOrganisasiForOrganisasiSiswaHelper}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataOrganisasiForOrganisasiSiswaHelper}
	 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataOrganisasiForOrganisasiSiswaHelper
	 */
	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final OrganisasiSiswa organisasiSiswa = (OrganisasiSiswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(OrganisasiSiswaPunyaSiswa.class)
					.add(Restrictions.eq("siswa", siswa)).add(Restrictions.eq("organisasiSiswa", organisasiSiswa))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("organisasiSiswa", organisasiSiswa);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(organisasiSiswa.getNama()).setParent(arg0);
			new Label(organisasiSiswa.getYayasan() == null ? "Semua" : organisasiSiswa.getYayasan().getNama())
					.setParent(arg0);
			new Label(organisasiSiswa.getSekolah() == null ? "Semua" : organisasiSiswa.getSekolah().getNama())
					.setParent(arg0);

			new Label(organisasiSiswa.getKeterangan()).setParent(arg0);

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/**
	 * Menyimpan pendaftaran {@link #siswa} ke setiap organisasi yang dicentang (dan tidak
	 * dinonaktifkan, yaitu belum terdaftar sebelumnya) sebagai baris {@link OrganisasiSiswaPunyaSiswa}
	 * baru.
	 */
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
					OrganisasiSiswa organisasiSiswa = (OrganisasiSiswa) checkbox.getAttribute("organisasiSiswa");

					OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) session
							.createCriteria(OrganisasiSiswaPunyaSiswa.class)
							.add(Restrictions.eq("organisasiSiswa", organisasiSiswa))
							.add(Restrictions.eq("siswa", siswa)).setMaxResults(1).uniqueResult();
					if (organisasiSiswaPunyaSiswa == null) {
						organisasiSiswaPunyaSiswa = new OrganisasiSiswaPunyaSiswa();
						organisasiSiswaPunyaSiswa.setOrganisasiSiswa(organisasiSiswa);
						organisasiSiswaPunyaSiswa.setOleh(tbmuser.getUserId());
						organisasiSiswaPunyaSiswa.setTbmuser(tbmuser);
						organisasiSiswaPunyaSiswa.setSiswa(siswa);
						organisasiSiswaPunyaSiswa.setDiubahDari(SiswaAction.class.getSimpleName());
						Common.refreshSaveOrUpdate(session, organisasiSiswaPunyaSiswa);
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataOrganisasiForOrganisasiSiswaHelper.java:133");
				// TODO: handle exception
			}
		}

		if (!warning.isEmpty()) {
			MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	/**
	 * Membangun jendela lengkap picker organisasi (filter + grid + toolbar Simpan/Batal) dan
	 * memasangnya ke {@code window}; tombol Simpan memicu {@link #save()} lalu memanggil ulang
	 * {@code dataLoader} untuk menyegarkan tampilan pemanggil.
	 *
	 * @param dataLoader pemuat data pemanggil yang disegarkan setelah penyimpanan berhasil
	 * @param window     jendela target tempat picker dibangun
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Organisasi Kesiswaan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan = new Combobox());
		searchyayasan.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchsekolah = new Combobox());
		searchsekolah.setWidth("90%");

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataOrganisasiForOrganisasiSiswaHelper.java:248");

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
		column.setLabel("Yayasan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Syarat");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("30%");

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

	/**
	 * Membangun kueri Hibernate untuk daftar organisasi siswa yang dapat dipilih, difilter
	 * sekolah/yayasan dan kata kunci nama.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan hasil
	 * @return kriteria Hibernate siap dieksekusi/dipaginasi
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiSiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("sekolah"),
								CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))
				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("yayasan"),
								CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Mengeksekusi ulang pencarian ({@link #initCriteria(boolean)}) untuk halaman aktif dan merender hasilnya ke grid pilihan organisasi. */
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
