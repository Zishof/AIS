package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
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
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GrupPertemuan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaGrupPertemuan;
import ais.database.model.Skripsi;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Helper ZK untuk memasukkan mahasiswa perwalian (Dosen PA) ke dalam satu {@link GrupPertemuan}
 * (kelompok pertemuan dosen PA, dipakai antara lain untuk bimbingan/konsultasi kolektif). Jendela
 * pencarian menampilkan mahasiswa yang menjadi anak wali dosen pemilik {@code grupPertemuan}
 * (disaring lebih lanjut oleh NIM/rentang NIM, nama, Fakultas, Jurusan, tahun angkatan, dan status
 * kemahasiswaan), dengan mahasiswa yang sudah tergabung dalam grup ditandai tercentang dan dikunci.
 *
 * <p>
 * {@link #save()} membuat (atau menemukan kembali) satu baris {@link Pertemuan} yang sesuai untuk
 * setiap mahasiswa terpilih, dengan sumber pertemuan berbeda tergantung {@code grupPertemuan.getJenis()}:
 * </p>
 * <ul>
 * <li>{@link GrupPertemuan#KRS_MAHASISWA} — pertemuan terikat ke {@link KrsMahasiswa} mahasiswa
 * pada semester berjalan (disinkronkan lewat {@code Common#singkronkanKrsMahasiswa}).</li>
 * <li>{@link GrupPertemuan#BIMBINGAN} — pertemuan terikat ke
 * {@link MahasiswaRequestTugasAkhir} aktif/mengulang/seminar/lulus milik mahasiswa.</li>
 * <li>{@link GrupPertemuan#SIDANG} — pertemuan terikat ke {@link Skripsi} terbaru milik mahasiswa.</li>
 * <li>Jenis lainnya — pertemuan generik baru tanpa keterikatan entitas spesifik.</li>
 * </ul>
 * <p>
 * Pertemuan yang sudah dibuat/ditemukan lalu dihubungkan ke {@code grupPertemuan} lewat baris
 * {@link PertemuanPunyaGrupPertemuan} (hanya dibuat bila relasi mahasiswa+grup belum ada, mencegah
 * duplikasi). Setiap mahasiswa diproses independen dalam try/catch tersendiri sehingga kegagalan
 * satu baris tidak menggagalkan seluruh proses simpan.
 * </p>
 */
public class AmbilDataMahasiswaForGrupPertemuanDosenPaHelper {

	private GrupPertemuan grupPertemuan;
	private MyGrid grid;

	private Textbox nim;
	private Textbox nama;
	private Decimalbox tahunangkatan;
	private Textbox dariNim;
	private Textbox sampaiNim;

	private Combobox searchstatusmahasiswa = new Combobox();

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Paging paging;

	/** @param grupPertemuan grup pertemuan tujuan; mahasiswa terpilih akan dimasukkan ke grup ini. */
	public AmbilDataMahasiswaForGrupPertemuanDosenPaHelper(GrupPertemuan grupPertemuan) {
		this.grupPertemuan = grupPertemuan;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		paging = new Paging();
		Common.initPaging100(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	}

	/**
	 * Renderer lokal untuk layar/komponen {@link AmbilDataMahasiswaForGrupPertemuanDosenPaHelper}. Kelas ini
	 * menerjemahkan satu item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik
	 * kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link
	 * AmbilDataMahasiswaForGrupPertemuanDosenPaHelper} dan dapat mengakses state kelas induk. Jangan menyimpan
	 * atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see AmbilDataMahasiswaForGrupPertemuanDosenPaHelper
	 */
	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			Session session = HibernateUtil.currentSession();
			int count = ((Number) session.createCriteria(PertemuanPunyaGrupPertemuan.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("grupPertemuan", grupPertemuan))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			checkbox.setChecked(count != 0);
			checkbox.setDisabled(count != 0);

			new Label(mahasiswa.getNim()).setParent(arg0);
			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);

		}
	}

	/**
	 * Memproses seluruh baris grid yang dicentang (dan belum terkunci) untuk menghubungkan
	 * mahasiswa terkait ke {@link #grupPertemuan}; lihat dokumentasi kelas untuk rincian sumber
	 * {@link Pertemuan} per jenis grup. Kegagalan pada satu baris ditelan dan dicatat lewat audit
	 * tanpa menghentikan proses baris lainnya.
	 *
	 * @throws InterruptedException tidak pernah dilempar dalam praktiknya; dipertahankan pada
	 *                              signature untuk kompatibilitas pemanggil
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() throws InterruptedException {
		Session session = HibernateUtil.currentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked() && !checkbox.isDisabled()) {
					Mahasiswa mahasiswa = (Mahasiswa) checkbox.getAttribute("mahasiswa");
					Pertemuan pertemuan = (Pertemuan) session.createCriteria(PertemuanPunyaGrupPertemuan.class)
							.setProjection(Projections.property("pertemuan"))
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(Restrictions.eq("grupPertemuan", grupPertemuan)).setMaxResults(1)
							.addOrder(Order.desc("id")).uniqueResult();
					if (pertemuan == null) {
						if (grupPertemuan.getJenis().equals(GrupPertemuan.KRS_MAHASISWA)) {
							Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(),
									grupPertemuan.getTahunAkademik(), grupPertemuan.getJenisSemester(),
									mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());

							KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);

							pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
									: session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("krsMahasiswa", krsMahasiswa))
											.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal()))
											.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult());
							if (pertemuan == null) {
								pertemuan = new Pertemuan();
								pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
							}
							pertemuan.setTanggal(grupPertemuan.getTanggal());
							pertemuan.setKrsMahasiswa(krsMahasiswa);
							pertemuan.setRuang(grupPertemuan.getRuang());
							pertemuan.setWaktuMulai(grupPertemuan.getWaktuMulai());
							pertemuan.setWaktuSelesai(grupPertemuan.getWaktuSelesai());
							session.save(pertemuan);
						} else if (grupPertemuan.getJenis().equals(GrupPertemuan.BIMBINGAN)) {
							MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) session
									.createCriteria(MahasiswaRequestTugasAkhir.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa))
									.add(Restrictions.or(
											Restrictions.eq("status", MahasiswaRequestTugasAkhir.MENGULANG_STATUS),
											Restrictions.or(
													Restrictions.eq("status", MahasiswaRequestTugasAkhir.LULUS_STATUS),
													Restrictions.or(
															Restrictions.eq("status",
																	MahasiswaRequestTugasAkhir.SEMINAR_STATUS),
															Restrictions.eq("status",
																	MahasiswaRequestTugasAkhir.AKTIF_STATUS)))))
									.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
							pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
									: session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("mahasiswaRequestTugasAkhir",
													mahasiswaRequestTugasAkhir))
											.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal()))
											.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult());
							if (pertemuan == null) {
								pertemuan = new Pertemuan();
								pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
							}
							pertemuan.setTanggal(grupPertemuan.getTanggal());
							pertemuan.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
							pertemuan.setRuang(grupPertemuan.getRuang());
							pertemuan.setWaktuMulai(grupPertemuan.getWaktuMulai());
							pertemuan.setWaktuSelesai(grupPertemuan.getWaktuSelesai());
							session.save(pertemuan);
						} else if (grupPertemuan.getJenis().equals(GrupPertemuan.SIDANG)) {
							Skripsi skripsi = (Skripsi) session.createCriteria(Skripsi.class)
									.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id"))
									.setMaxResults(1).uniqueResult();
							pertemuan = (Pertemuan) (pertemuan != null ? pertemuan
									: session.createCriteria(Pertemuan.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.eq("skripsi", skripsi))
											.add(Restrictions.eq("tanggal", grupPertemuan.getTanggal()))
											.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult());
							if (pertemuan == null) {
								pertemuan = new Pertemuan();
								pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
							}
							pertemuan.setTanggal(grupPertemuan.getTanggal());
							pertemuan.setSkripsi(skripsi);
							pertemuan.setRuang(grupPertemuan.getRuang());
							pertemuan.setWaktuMulai(grupPertemuan.getWaktuMulai());
							pertemuan.setWaktuSelesai(grupPertemuan.getWaktuSelesai());
							session.save(pertemuan);
						} else {
							if (pertemuan == null) {
								pertemuan = new Pertemuan();
								pertemuan.setStatusPertemuan(ConstantValues.TATAP_MUKA);
							}
							pertemuan.setTanggal(grupPertemuan.getTanggal());
							pertemuan.setRuang(grupPertemuan.getRuang());
							pertemuan.setWaktuMulai(grupPertemuan.getWaktuMulai());
							pertemuan.setWaktuSelesai(grupPertemuan.getWaktuSelesai());
							session.save(pertemuan);
						}
					}

					if (pertemuan != null && pertemuan.getId() != null) {
						int n = ((Number) session.createCriteria(PertemuanPunyaGrupPertemuan.class)
								.add(Restrictions.eq("pertemuan", pertemuan))
								.add(Restrictions.eq("grupPertemuan", grupPertemuan))
								.setProjection(Projections.rowCount()).uniqueResult()).intValue();
						if (n == 0) {
							PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = new PertemuanPunyaGrupPertemuan();
							pertemuanPunyaGrupPertemuan.setPertemuan(pertemuan);
							pertemuanPunyaGrupPertemuan.setMahasiswa(mahasiswa);
							pertemuanPunyaGrupPertemuan.setGrupPertemuan(grupPertemuan);
							session.save(pertemuanPunyaGrupPertemuan);
							pertemuan.setPertemuanPunyaGrupPertemuan(pertemuanPunyaGrupPertemuan);
							session.update(pertemuan);
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForGrupPertemuanDosenPaHelper.java:237");
				// TODO: handle exception
			}
		}

	}

	/**
	 * Membangun jendela pencarian dan pemilihan mahasiswa perwalian untuk dimasukkan ke
	 * {@link #grupPertemuan}. Kotak centang pada header kolom memilih/membatalkan pilih semua baris
	 * yang belum terkunci sekaligus. Tombol Simpan memanggil {@link #save()} lalu menyegarkan
	 * tampilan pemanggil lewat {@code dataLoader}.
	 *
	 * @param dataLoader dipanggil setelah simpan untuk menyegarkan tampilan grup pertemuan pemanggil
	 */
	public void display(final DataLoader dataLoader) {

		final Window window = new Window();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setTitle("Ambil Data Mahasiswa");
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
		north.setHeight("240px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dari Nim"));
		row.appendChild(dariNim = new Textbox());
		dariNim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai Nim"));
		row.appendChild(sampaiNim = new Textbox());
		sampaiNim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan Mahasiswa (kosong = semua)"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.setTooltiptext("Isi dengan tahun masuk mahasiswa. Kosongkan untuk menampilkan semua angkatan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.setWidth("90%");

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(searchstatusmahasiswa, new String[] { "nama", "kodeEpsbed" }, StatusMahasiswa.class);
		row.appendChild(searchstatusmahasiswa);
		searchstatusmahasiswa.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.setTooltiptext("Cari kandidat mahasiswa sesuai filter");
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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaForGrupPertemuanDosenPaHelper.java:399");

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
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						dataLoader.loadData(null);
						window.detach();
					}
				});
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
	 * Membangun kriteria Hibernate untuk mahasiswa anak wali dosen pemilik {@link #grupPertemuan}
	 * (baris kosong bila grup atau dosennya belum ada), disaring lebih lanjut oleh filter toolbar
	 * pencarian dan opsional status kemahasiswaan pada semester berjalan (dicek lewat subquery SQL
	 * native ke tabel {@code history_status_mahasiswa}).
	 *
	 * @param order {@code true} untuk mengurutkan hasil berdasarkan tahun angkatan descending lalu NIM ascending
	 * @return kriteria Hibernate siap eksekusi/paginasi
	 */
	public Criteria initCriteria(boolean order) {

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatusmahasiswa.getSelectedItem() == null ? null
				: searchstatusmahasiswa.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "mahasiswa in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";
			System.out.println("sql=>" + sql);
			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(grupPertemuan == null || grupPertemuan.getDosen() == null ? Restrictions.sqlRestriction("false")
						: Restrictions.eq("dosen", grupPertemuan.getDosen().getId()));

		if (order)
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));

		criteria.add(criteriaStatus).add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))
				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
				.add(dariNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ge("nim", dariNim.getValue()))
				.add(sampaiNim.getValue().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.le("nim", sampaiNim.getValue()))
				.createCriteria("jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));

		return criteria;
	}

	/**
	 * Mengisi ulang grid hasil pencarian mahasiswa (paginasi 100 baris per halaman, pola
	 * {@code Common#ROWS_COUNT_ON_PAGE_100}) sesuai kriteria pencarian saat ini.
	 *
	 * @param event tidak dipakai isinya
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Common.initPaging100(initCriteria(false), paging);

		List<Mahasiswa> mahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_100)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_100 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setEmptyMessage(mahasiswa.isEmpty()
				? "Tidak ada kandidat mahasiswa yang sesuai. Kosongkan Angkatan Mahasiswa atau sesuaikan filter pencarian."
				: "Tidak ada kandidat mahasiswa yang cocok dengan filter.");
		grid.setModelCheckMobile(strset);

	}

}
