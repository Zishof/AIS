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
import org.zkoss.zul.Decimalbox;
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

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.sekolah.SiswaAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
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
 * Helper ZK jendela "Ambil Data Siswa" untuk menambahkan anggota ke satu {@link OrganisasiSiswa}
 * (organisasi/ekstrakurikuler siswa): grid pencarian siswa dengan filter lengkap (NIM/rentang NIM,
 * nama, dosen PA, yayasan/sekolah, tahun angkatan), checkbox per baris (dikunci bila siswa sudah
 * jadi anggota) plus checkbox "pilih semua" di header, dan tombol simpan yang membuat baris
 * {@link OrganisasiSiswaPunyaSiswa} untuk setiap siswa yang dicentang.
 *
 * <p>
 * Dipakai sebagai popup modal dipanggil dari layar detail organisasi siswa; hasil penyimpanan
 * memicu {@link DataLoader#loadData} pemanggil untuk menyegarkan grid anggota di layar induk.
 * </p>
 */
public class AmbilDataSiswaForOrganisasiSiswaHelper {

	private OrganisasiSiswa organisasiSiswa;
	private MyGrid grid;

	private Textbox nomorInduk;
	private Textbox nama;
	private Decimalbox tahunMasuk;
	private Textbox dariNomorInduk;
	private Textbox sampaiNomorInduk;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private AmbilDataDosenBanbox searchdosen;

	/** Membuat helper terikat {@code organisasiSiswa} target dan menyiapkan combo yayasan/sekolah serta paging (50 baris/halaman) awal. */
	public AmbilDataSiswaForOrganisasiSiswaHelper(OrganisasiSiswa organisasiSiswa) {
		this.organisasiSiswa = organisasiSiswa;
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		paging = new Paging();
		Common.initPaging50(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/** Renderer baris grid pencarian: checkbox pilih (dicentang &amp; dikunci bila siswa sudah anggota organisasi), NIM, nama, tahun masuk. */
	class SiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Siswa siswa = (Siswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(OrganisasiSiswaPunyaSiswa.class)
					.add(Restrictions.eq("siswa", siswa)).add(Restrictions.eq("organisasiSiswa", organisasiSiswa))
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
	/** Menyimpan {@link OrganisasiSiswaPunyaSiswa} untuk setiap baris grid yang checkbox-nya dicentang dan tidak dikunci (siswa baru, belum jadi anggota), mencatat {@code oleh}/{@code tbmuser} dari user yang login dan {@code diubahDari} = {@link SiswaAction}. */
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
					Siswa siswa = (Siswa) checkbox.getAttribute("siswa");

					OrganisasiSiswaPunyaSiswa organisasiSiswaPunyaSiswa = (OrganisasiSiswaPunyaSiswa) session
							.createCriteria(OrganisasiSiswaPunyaSiswa.class).add(Restrictions.eq("siswa", siswa))
							.add(Restrictions.eq("organisasiSiswa", organisasiSiswa)).setMaxResults(1).uniqueResult();
					if (organisasiSiswaPunyaSiswa == null) {
						organisasiSiswaPunyaSiswa = new OrganisasiSiswaPunyaSiswa();
					}
					organisasiSiswaPunyaSiswa.setOrganisasiSiswa(organisasiSiswa);
					organisasiSiswaPunyaSiswa.setOleh(tbmuser.getUserId());
					organisasiSiswaPunyaSiswa.setTbmuser(tbmuser);
					organisasiSiswaPunyaSiswa.setSiswa(siswa);
					organisasiSiswaPunyaSiswa.setDiubahDari(SiswaAction.class.getSimpleName());
					Common.refreshSaveOrUpdate(session, organisasiSiswaPunyaSiswa);

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaForOrganisasiSiswaHelper.java:140");
				// TODO: handle exception
			}
		}

		if (!warning.isEmpty()) {
			MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}

	}

	/**
	 * Membangun dan membuka {@code window} sebagai modal "Ambil Data Siswa": panel filter (bisa
	 * disembunyikan/ditampilkan via {@link ais.ui.util.BanboxFilterToggle}), grid hasil dengan
	 * checkbox pilih-semua di header, dan toolbar Simpan/Batal. Tombol Simpan memanggil
	 * {@link #save()} lalu memicu {@code dataLoader.loadData} pemanggil dan menutup jendela.
	 */
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nomorInduk = new Textbox());
		nomorInduk.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dari NomorInduk"));
		row.appendChild(dariNomorInduk = new Textbox());
		dariNomorInduk.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai NomorInduk"));
		row.appendChild(sampaiNomorInduk = new Textbox());
		sampaiNomorInduk.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Siswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		row.appendChild(searchdosen = new AmbilDataDosenBanbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));
		row.appendChild(searchyayasan);
		searchyayasan.setWidth("90%");
		searchyayasan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/AmbilDataSiswaForOrganisasiSiswaHelper.java:310");

					}

				}
			}
		});

		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen PA");
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

	/** Membentuk criteria pencarian {@link Siswa} aktif berdasarkan filter dosen PA, nama, nomor induk (persis/rentang), tahun masuk, sekolah, dan yayasan; dibatasi ke anak kandung bila user login adalah orang tua. Diurut tahun masuk menurun lalu nomor induk bila {@code order} true. */
	public Criteria initCriteria(boolean order) {
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"));

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getOrangTua() != null && !tbmuser.getOrangTua().ambilAnakSiswa().isEmpty()) {
			criteria.add(Restrictions.in("id", tbmuser.getOrangTua().ambilAnakSiswa()));
		}

		if (order)
			criteria.addOrder(Order.desc("tahunMasuk")).addOrder(Order.asc("nomorInduk"));

		criteria

				.add(dosen != null ? Restrictions.eq("dosen", dosen.getId()) : Restrictions.sqlRestriction("1=1"))

				.add(

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

				.add(CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))
				.add(dariNomorInduk.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nomorInduk", dariNomorInduk.getValue()))
				.add(sampaiNomorInduk.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nomorInduk", sampaiNomorInduk.getValue()))
				.createCriteria("sekolah", Criteria.LEFT_JOIN)

				.add(CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Menjalankan ulang pencarian dan memuat ulang {@link #grid} serta {@link #paging} (50 baris/halaman). */
	public void onSearchDefault(Event event) {

		Common.initPaging50(initCriteria(false), paging);

		List<Siswa> siswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_50)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_50 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(siswa);
		grid.setRowRenderer(new SiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
