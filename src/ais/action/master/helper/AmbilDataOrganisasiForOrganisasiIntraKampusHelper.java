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
import org.zkoss.zul.Vbox;

import ais.action.master.MahasiswaAction;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Mahasiswa;
import ais.database.model.OrganisasiIntraKampus;
import ais.database.model.OrganisasiIntraKampusPunyaMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK dengan arah kebalikan dari {@link AmbilDataMahasiswaForOrganisasiIntraKampusHelper}:
 * dipakai dari sisi satu {@link Mahasiswa} untuk memilih dan mendaftar ke satu atau lebih
 * {@link OrganisasiIntraKampus}. Jendela pencarian menyaring organisasi lewat nama, Fakultas, dan
 * Jurusan (organisasi tanpa Fakultas/Jurusan spesifik berlaku untuk semua, sehingga selalu lolos
 * filter), dan menampilkan syarat keanggotaan tiap organisasi (minimal IPK/SKS/Angka Kredit) bila
 * ada. Organisasi yang sudah diikuti mahasiswa ditandai tercentang dan dikunci.
 *
 * <p>
 * {@link #save()} memvalidasi kelayakan lewat
 * {@code Common#checkApakahMemenuhiSyaratOrganisasiKemahasiswaan} untuk tiap organisasi yang
 * dicentang sebelum menyimpan baris {@link OrganisasiIntraKampusPunyaMahasiswa}; organisasi yang
 * syaratnya belum terpenuhi dilewati dengan pesan peringatan yang dikumpulkan dan ditampilkan
 * sekaligus di akhir. Pengambilan komponen checkbox per baris sengaja memakai atribut Row (bukan
 * indeks anak), karena pada tampilan mobile struktur baris dirombak menjadi kartu bersarang
 * sehingga indeks anak pertama tidak lagi konsisten berupa checkbox.
 * </p>
 */
public class AmbilDataOrganisasiForOrganisasiIntraKampusHelper {

	private Mahasiswa mahasiswa;
	private MyGrid grid;

	private Textbox nama;
	private Combobox searchjurusan;
	private Combobox searchfakultas;

	private Paging paging;

	/** @param mahasiswa mahasiswa yang akan didaftarkan ke organisasi terpilih. */
	public AmbilDataOrganisasiForOrganisasiIntraKampusHelper(Mahasiswa mahasiswa) {
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
			final OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("organisasiIntraKampus", organisasiIntraKampus);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(organisasiIntraKampus.getNama()).setParent(arg0);
			new Label(organisasiIntraKampus.getFakultas() == null ? "Semua"
					: organisasiIntraKampus.getFakultas().getNama()).setParent(arg0);
			new Label(
					organisasiIntraKampus.getJurusan() == null ? "Semua" : organisasiIntraKampus.getJurusan().getNama())
					.setParent(arg0);

			if (organisasiIntraKampus.getMinimalIpk() > 0.1 || organisasiIntraKampus.getMinimalSks() > 0.1
					|| organisasiIntraKampus.getMinimalSkkm() > 0.1) {
				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				if (organisasiIntraKampus.getMinimalIpk() > 0.1) {
					new MyLabelAgakKecil("IPK >= " + Common.numberFormat.get().format(organisasiIntraKampus.getMinimalIpk()))
							.setParent(vbox);
				}
				if (organisasiIntraKampus.getMinimalSks() > 0.1) {
					new MyLabelAgakKecil(
							"SKS Total >= " + Common.numberFormat.get().format(organisasiIntraKampus.getMinimalSks()))
							.setParent(vbox);
				}
				if (organisasiIntraKampus.getMinimalSkkm() > 0.1) {
					new MyLabelAgakKecil(
							"Angka Kredit >= " + Common.numberFormat.get().format(organisasiIntraKampus.getMinimalSkkm()))
							.setParent(vbox);
				}
			} else {
				new Label(ais.common.Common.getBahasaConfig("Tidak ada syarat")).setParent(arg0);
			}

			new Label(organisasiIntraKampus.getKeterangan()).setParent(arg0);

		}
	}

	/**
	 * Memproses seluruh baris grid yang dicentang (dan belum terkunci): organisasi yang syaratnya
	 * dipenuhi {@link #mahasiswa} disimpan sebagai {@link OrganisasiIntraKampusPunyaMahasiswa} baru;
	 * yang tidak memenuhi syarat dilewati dan pesannya dikumpulkan lalu ditampilkan sekaligus di akhir.
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
			try {
				// PENTING: jangan ambil komponen anak berdasarkan index posisi (row.getChildren().get(0)),
				// karena pada mode mobile (lihat UIUtil.checkGrigMobile) struktur Row dibongkar ulang
				// menjadi kartu dengan Grid bersarang sehingga anak pertama Row bisa berupa
				// org.zkoss.zul.Grid, bukan lagi checkbox-nya (menyebabkan ClassCastException).
				// Checkbox selalu dititipkan sebagai attribute "checkbox" pada Row saat render
				// (lihat MahasiswaRenderer.render) dan attribute ini ikut dipindahkan saat mode mobile,
				// jadi ambil melalui attribute agar konsisten di semua mode tampilan.
				MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");
				if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
					OrganisasiIntraKampus organisasiIntraKampus = (OrganisasiIntraKampus) checkbox
							.getAttribute("organisasiIntraKampus");
					if (Common.checkApakahMemenuhiSyaratOrganisasiKemahasiswaan(mahasiswa, organisasiIntraKampus)) {
						OrganisasiIntraKampusPunyaMahasiswa organisasiIntraKampusPunyaMahasiswa = (OrganisasiIntraKampusPunyaMahasiswa) session
								.createCriteria(OrganisasiIntraKampusPunyaMahasiswa.class)
								.add(Restrictions.eq("organisasiIntraKampus", organisasiIntraKampus))
								.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();
						if (organisasiIntraKampusPunyaMahasiswa == null) {
							organisasiIntraKampusPunyaMahasiswa = new OrganisasiIntraKampusPunyaMahasiswa();
							organisasiIntraKampusPunyaMahasiswa.setOrganisasiIntraKampus(organisasiIntraKampus);
							organisasiIntraKampusPunyaMahasiswa.setOleh(tbmuser.getUserId());
							organisasiIntraKampusPunyaMahasiswa.setTbmuser(tbmuser);
							organisasiIntraKampusPunyaMahasiswa.setMahasiswa(mahasiswa);
							organisasiIntraKampusPunyaMahasiswa.setDiubahDari(MahasiswaAction.class.getSimpleName());
							Common.refreshSaveOrUpdate(session, organisasiIntraKampusPunyaMahasiswa);
						}
					} else {
						warning += Common.pesan(
								"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} belum memenuhi syarat untuk mengikuti organisasi \"{V3}\". Langkah yang dapat dilakukan: (1) periksa kembali pemenuhan syarat organisasi tersebut (mis. IPK, jumlah SKS, atau Angka Kredit minimal); (2) lengkapi atau perbarui data mahasiswa yang belum sesuai; (3) ulangi proses penyimpanan setelah syarat terpenuhi.\n\n",
								mahasiswa.getNim(), mahasiswa.getNama(), organisasiIntraKampus.getNama());
					}

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataOrganisasiForOrganisasiIntraKampusHelper.java:165");
				// TODO: handle exception
			}
		}

		if (!warning.isEmpty()) {
			MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		}
	}

	/**
	 * Membangun jendela pencarian dan pemilihan organisasi untuk didaftarkan oleh {@link #mahasiswa}.
	 * Tombol Simpan memanggil {@link #save()} lalu menyegarkan tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param dataLoader dipanggil setelah simpan untuk menyegarkan tampilan daftar organisasi mahasiswa
	 * @param window     jendela ({@link MyWindow}) yang dipakai ulang untuk menampilkan layar ini
	 */
	public void display(final DataLoader dataLoader, final MyWindow window) {

		Common.clear(window);
		window.setTitle("Ambil Organisasi Kemahasiswaan");
		window.setWidth("90%");
		window.setHeight("90%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
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

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Organisasi"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas = new Combobox());
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan = new Combobox());
		searchjurusan.setWidth("90%");

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

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
						if (myCheckbox == null) {
							continue;
						}
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());

						if (myCheckbox.isDisabled()) {
							continue;
						}

						myCheckbox.setChecked(checkbox.isChecked());
						if (!checkbox.isChecked()) {
							continue;
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataOrganisasiForOrganisasiIntraKampusHelper.java:281");

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
	 * Membangun kriteria Hibernate untuk pencarian {@link OrganisasiIntraKampus}, sesuai filter
	 * toolbar (nama, Fakultas, Jurusan). Organisasi tanpa Fakultas/Jurusan spesifik selalu lolos
	 * filter tersebut (berlaku untuk semua unit).
	 *
	 * @param order {@code true} untuk mengurutkan hasil berdasarkan nama ascending
	 * @return kriteria Hibernate siap eksekusi/paginasi
	 */
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(OrganisasiIntraKampus.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));

		criteria.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("jurusan"),
								CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))
				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("fakultas"),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)));

		return criteria;
	}

	/**
	 * Mengisi ulang grid hasil pencarian organisasi (paginasi 50 baris per halaman) sesuai kriteria
	 * pencarian saat ini.
	 *
	 * @param event tidak dipakai isinya
	 */
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
