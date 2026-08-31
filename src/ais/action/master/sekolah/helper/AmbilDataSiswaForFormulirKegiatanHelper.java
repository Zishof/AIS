package ais.action.master.sekolah.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
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
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
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

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Dialog pemilihan banyak siswa ZK untuk mendaftarkan peserta ({@link FormulirKegiatanPeserta})
 * ke satu {@link FormulirKegiatan} (formulir kegiatan kesiswaan) — pola dasarnya sama dengan
 * {@link AmbilDataSiswaForDiskonSiswaHelper}/{@link AmbilDataSiswaForKegiatanKesiswaanHelper},
 * dengan filter tambahan berupa kelas ({@code kelas}) dan guru wali/pengampu ({@code guru}).
 * Combo yayasan/sekolah otomatis terkunci bila sudah ditetapkan pada formulir kegiatan; siswa
 * yang sudah terdaftar sebagai peserta tampil tercentang dan terkunci.
 */
public class AmbilDataSiswaForFormulirKegiatanHelper {

	private FormulirKegiatan formulirKegiatan;
	private MyGrid grid;

	private Textbox nomorInduk;
	private Textbox nama;
	private Decimalbox tahunMasuk;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private AmbilDataKelasSiswaBanbox kelas;
	private AmbilDataGuruBanbox guru;

	/** Menyiapkan dialog untuk {@code formulirKegiatan}; combo yayasan/sekolah otomatis terisi dan terkunci bila sudah ditetapkan pada formulir kegiatan, atau bebas dipilih bila belum. */
	public AmbilDataSiswaForFormulirKegiatanHelper(FormulirKegiatan formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
		Yayasan yayasan = formulirKegiatan.getYayasan();
		Sekolah sekolah = formulirKegiatan.getSekolah();
		Common.insertCombo(searchyayasan, new String[] { "nama" }, Yayasan.class, Restrictions.eq("aktif", true));

		/**
		 * Event listener lokal milik {@link AmbilDataSiswaForFormulirKegiatanHelper}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link AmbilDataSiswaForFormulirKegiatanHelper} dan
		 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see AmbilDataSiswaForFormulirKegiatanHelper
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

	/** Renderer baris grid untuk {@link Siswa}: checkbox pilih (tercentang dan terkunci bila siswa sudah terdaftar sebagai peserta {@link #formulirKegiatan}), nomor induk, nama, dan tahun masuk. */
	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Siswa siswa = (Siswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("siswa", siswa)).add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("siswa", siswa);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(siswa.getNomorInduk()).setParent(arg0);
			new Label(siswa.getNama()).setParent(arg0);
			new Label(siswa.getTahunMasuk() + "").setParent(arg0);

		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/** Menyimpan {@link FormulirKegiatanPeserta} untuk setiap siswa yang tercentang dan belum terkunci pada grid. */
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
					Siswa siswa = (Siswa) checkbox.getAttribute("siswa");
					
					
					
					if (formulirKegiatan.getGrupFormulirKegiatan() != null) {
						FormulirKegiatanPeserta kegiatanLainSatuGrup = ((FormulirKegiatanPeserta) session
								.createCriteria(FormulirKegiatanPeserta.class)
								.createAlias("formulirKegiatan", "formulirKegiatan")
								.add(Restrictions.eq("formulirKegiatan.grupFormulirKegiatan",
										formulirKegiatan.getGrupFormulirKegiatan()))
								.add(Restrictions.or(Restrictions.isNotNull("siswa"),
										Restrictions.or(Restrictions.isNotNull("guru"),
												Restrictions.or(Restrictions.isNotNull("mahasiswa"),
														Restrictions.isNotNull("dosen")))))
								.add(Restrictions.ne("formulirKegiatan", formulirKegiatan))

								.add(Restrictions.eq("siswa", siswa))

								.setMaxResults(1).uniqueResult());
						if (siswa != null && kegiatanLainSatuGrup != null) {
							MyMessageboxConfig.showFormat(
									"Siswa dengan nama {V1} tidak dapat didaftarkan karena telah terdaftar pada kegiatan \"{V2}\". Langkah yang dapat dilakukan: (1) periksa kembali data pendaftaran siswa yang bersangkutan; (2) pilih siswa lain yang belum terdaftar pada kegiatan dalam grup yang sama; (3) hubungi administrator apabila memerlukan informasi lebih lanjut.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
									siswa.getNama(), kegiatanLainSatuGrup.getFormulirKegiatan().getNama());

							return;
						}
					}
					
					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) session
							.createCriteria(FormulirKegiatanPeserta.class).add(Restrictions.eq("siswa", siswa))
							.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).setMaxResults(1).uniqueResult();
					if (formulirKegiatanPeserta == null) {
						formulirKegiatanPeserta = new FormulirKegiatanPeserta();
						int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
								.setProjection(Projections.rowCount())
								.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).uniqueResult()).intValue();
						count++;
						String kode = "0000000000000" + count;
						kode = kode.substring(kode.length() - 5);
						formulirKegiatanPeserta.setKode(kode);
					}

					formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
					formulirKegiatanPeserta.setOleh(tbmuser.getUserId());
					formulirKegiatanPeserta.setSiswa(siswa);
					Common.refreshSaveOrUpdate(session, formulirKegiatanPeserta);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaForFormulirKegiatanHelper.java:210");
				// TODO: handle exception
			}
		}

	}

	/** Membangun kerangka dialog: panel filter pencarian (nomor induk/nama/yayasan/sekolah/tahun angkatan/kelas/guru) di utara, grid siswa berpaging di tengah, dan tombol Simpan/Batal di selatan, lalu langsung memuat data dan membuka dialog sebagai modal. */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Data Siswa");
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

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIS"));
		row.appendChild(nomorInduk = new Textbox());
		nomorInduk.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Siswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan/Sekolah"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(searchyayasan);
		searchyayasan.setCols(8);
		hbox.appendChild(searchsekolah);
		searchsekolah.setCols(8);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunMasuk = new Decimalbox());
		tahunMasuk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wali Kelas"));
		row.appendChild(guru = new AmbilDataGuruBanbox());
		guru.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new AmbilDataKelasSiswaBanbox());
		kelas.setWidth("90%");

		guru.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		kelas.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaForFormulirKegiatanHelper.java:377");

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

	/** Menyusun kriteria pencarian {@link Siswa} berdasarkan nomor induk (ilike), nama (ilike), tahun masuk, sekolah, yayasan, kelas ({@link KelasSiswaPunyaSiswa}), dan guru wali/pengampu; terurut tahun masuk menurun lalu NIM bila {@code order} true. */
	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		List<Long> longs = null;
		if (kelas.getAttribute("kelasSiswa") != null) {
			longs = session.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.property("siswa.id"))
					.add(Restrictions.eq("kelasSiswa", kelas.getAttribute("kelasSiswa"))).list();
		}

		List<Long> longsa = null;
		if (guru.getAttribute("guru") != null) {
			longsa = session.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.property("siswa.id"))
					.createAlias("kelasSiswa", "kelasSiswa")
					.add(Restrictions.eq("kelasSiswa.guruPembina", guru.getAttribute("guru"))).list();

			if (!longsa.isEmpty()) {
				if (longs == null) {
					longs = new ArrayList<Long>();
				}
				longs.addAll(longsa);
			}
		}

		Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah")).add(
				longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", longs));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order)
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorInduk"));

		criteria.add(

				nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") :

						Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(

						nomorInduk.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										Restrictions.ilike("nomorIndukSantri", nomorInduk.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("nomorInduk", nomorInduk.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("nomorIndukNasional", nomorInduk.getValue().trim(),
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

	@SuppressWarnings("unchecked")
	/** Memuat ulang daftar siswa sesuai filter aktif (dipaginasi 50 baris via {@link #paging}) ke grid. */
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
