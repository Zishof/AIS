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
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
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

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper dialog pemilihan massal calon siswa ({@link CalonSiswa}) untuk ditautkan ke satu
 * {@link PengaturanBiaya} (via {@link PengaturanBiayaPunyaSiswa}) modul sekolah. Menampilkan
 * dialog pencarian dengan filter NIS/nama/yayasan/sekolah/tahun angkatan dan paginasi 50
 * baris; combobox sekolah otomatis disaring ulang sesuai yayasan terpilih. Calon siswa yang
 * sudah tertaut ke {@code pengaturanBiaya} ditampilkan tercentang dan checkbox-nya dikunci
 * (tidak dapat dibatalkan lewat dialog ini). Baris terkunci yayasan/sekolah (bila
 * {@code pengaturanBiaya} sudah memiliki nilai tetap untuk keduanya) tidak dapat diubah pengguna.
 * Simpan hanya memproses baris yang tercentang DAN checkbox-nya tidak terkunci (mencegah
 * duplikasi entri yang sudah ada).
 */
public class AmbilDataCalonSiswaForPengaturanBiayaHelper {

	private PengaturanBiaya pengaturanBiaya;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunMasuk;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;

	/**
	 * Membuat helper terikat ke {@code pengaturanBiaya}: menyiapkan combobox yayasan/sekolah (bila
	 * pengaturan biaya sudah memiliki nilai tetap untuk salah satunya, combobox terkait dikunci dan
	 * diprapilih; bila tidak, combobox sekolah otomatis diisi ulang mengikuti pilihan yayasan) serta
	 * komponen paginasi 50 baris.
	 *
	 * @param pengaturanBiaya pengaturan biaya tujuan penautan calon siswa
	 */
	public AmbilDataCalonSiswaForPengaturanBiayaHelper(PengaturanBiaya pengaturanBiaya) {
		this.pengaturanBiaya = pengaturanBiaya;
		Yayasan yayasan = pengaturanBiaya.getYayasan();
		Sekolah sekolah = pengaturanBiaya.getSekolah();
		Common.insertCombo(searchyayasan, new String[] { "nama" }, Yayasan.class);

		/**
		 * Event listener lokal milik {@link AmbilDataCalonSiswaForPengaturanBiayaHelper}. Kelas ini menangani event
		 * untuk komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataCalonSiswaForPengaturanBiayaHelper}
		 * dan dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see AmbilDataCalonSiswaForPengaturanBiayaHelper
		 */
		class SearchYayasanEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(searchsekolah);
				searchsekolah.setSelectedItem(null);
				if (searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null) {
					return;
				}
				Common.insertCombo(searchsekolah, new String[] { "nama" }, "jenisSekolah", Sekolah.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));
			}

		}

		searchyayasan.addEventListener("onChange", new SearchYayasanEventListener());

		if (yayasan != null) {
			Common.selectComboItem(searchyayasan, yayasan);
			Common.clear(searchsekolah);
			Common.insertCombo(searchsekolah, new String[] { "nama" }, "jenisSekolah", Sekolah.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("yayasan", yayasan));
			searchyayasan.setDisabled(true);
		} else {
			searchyayasan.setDisabled(false);
		}

		if (sekolah != null) {
			Common.selectComboItem(searchsekolah, sekolah);
			searchsekolah.setDisabled(true);
		} else {
			searchsekolah.setDisabled(false);
		}

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/** Perender baris grid hasil pencarian: checkbox pilih (tercentang+terkunci bila sudah tertaut ke {@link #pengaturanBiaya}), NIS, nama, dan tahun masuk. */
	class CalonSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CalonSiswa calonSiswa = (CalonSiswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(PengaturanBiayaPunyaSiswa.class)
					.add(Restrictions.eq("calonSiswa", calonSiswa))
					.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("calonSiswa", calonSiswa);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(calonSiswa.getNim()).setParent(arg0);
			new Label(calonSiswa.getNama()).setParent(arg0);
			new Label(calonSiswa.getTahunMasuk() + "").setParent(arg0);

		}
	}

	/**
	 * Menyimpan penautan {@link PengaturanBiayaPunyaSiswa} untuk setiap calon siswa pada baris grid
	 * yang tercentang DAN checkbox-nya tidak terkunci (baris terkunci berarti sudah tertaut
	 * sebelumnya, dilewati untuk mencegah duplikasi). Entri baru dicatat dengan {@code oleh} = user
	 * id pengguna yang sedang login.
	 */
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
					CalonSiswa calonSiswa = (CalonSiswa) checkbox.getAttribute("calonSiswa");
					PengaturanBiayaPunyaSiswa pengaturanBiayaPunyaCalonSiswa = (PengaturanBiayaPunyaSiswa) session
							.createCriteria(PengaturanBiayaPunyaSiswa.class)
							.add(Restrictions.eq("calonSiswa", calonSiswa))
							.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya)).setMaxResults(1).uniqueResult();
					if (pengaturanBiayaPunyaCalonSiswa == null) {
						pengaturanBiayaPunyaCalonSiswa = new PengaturanBiayaPunyaSiswa();
					}
					pengaturanBiayaPunyaCalonSiswa.setPengaturanBiaya(pengaturanBiaya);
					pengaturanBiayaPunyaCalonSiswa.setOleh(tbmuser.getUserId());
					pengaturanBiayaPunyaCalonSiswa.setCalonSiswa(calonSiswa);
					Common.refreshSaveOrUpdate(session, pengaturanBiayaPunyaCalonSiswa);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaForPengaturanBiayaHelper.java:170");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun dan menampilkan dialog modal pemilihan calon siswa: panel filter pencarian (bagian
	 * utara, dapat diciutkan lewat {@link ais.ui.util.BanboxFilterToggle}) dan grid hasil dengan
	 * checkbox pilih-semua di header. Tombol simpan memanggil {@link #save()} lalu memuat ulang
	 * data pemanggil lewat {@code dataLoader} sebelum menutup dialog.
	 *
	 * @param dataLoader callback yang dipanggil untuk memuat ulang data layar pemanggil setelah simpan
	 */
	public void display(final DataLoader dataLoader) {

		final MyWindow window = new MyWindow();
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setTitle("Ambil Data CalonSiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIS"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama CalonSiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));
		row.appendChild(searchsekolah);
		searchsekolah.setWidth("90%");
		searchsekolah.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunMasuk = new Decimalbox());
		tahunMasuk.setWidth("90%");

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataCalonSiswaForPengaturanBiayaHelper.java:322");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIS");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
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
				window.detach();
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
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
	 * Membangun kriteria pencarian calon siswa (wajib memiliki gelombang pendaftaran PSB terisi),
	 * disaring berdasarkan hak akses orang tua (bila user login adalah orang tua, hanya anaknya
	 * sendiri yang muncul), NIS/nama, tahun masuk, sekolah, dan yayasan sesuai filter aktif;
	 * diurutkan berdasarkan tahun masuk terbaru lalu NIM bila {@code order}.
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("siswa.id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order)
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nim"));

		criteria

				.add(

						nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") :

								Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(

						nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("noRegistrasi", nim.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("nomorInduk", nim.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("nomorIndukNasional", nim.getValue().trim(),
														MatchMode.ANYWHERE)))

				)

				.add(tahunMasuk.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunMasuk", tahunMasuk.getValue().intValue()))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.createCriteria("sekolah", Criteria.LEFT_JOIN)

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	/** Menjalankan pencarian calon siswa sesuai filter aktif, memuat ulang paginasi, dan merender hasil ke grid lewat {@link CalonSiswaRenderer}. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<CalonSiswa> calonSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(calonSiswa);
		grid.setRowRenderer(new CalonSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
