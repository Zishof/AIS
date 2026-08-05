package ais.action.master.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.admin.RekapHasilTugasMahasiswa;
import ais.action.master.dashboard.admin.RekapHasilUjianMahasiswa;
import ais.action.report.CommonReportHelper;
import ais.action.report.format1.akademik.LaporanKurikulumMahasiswa;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.FormatNilai;
import ais.database.model.FormatNilaiTambahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDiv;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PenilaianMahasiswaHelper implements DataLoader {

	private MyGrid grid;
	private MyGrid gridBelum;
	private Mahasiswa mahasiswa;
	private Integer semester;
	private boolean remedial = false;
	private List<Long> detailperkuliahans;
	private List<Long> detailperkuliahansbelum;

	private Integer semesterPendek;

	private Integer tahapan;

	private boolean tampilkanNilaiRinci = true;

	public PenilaianMahasiswaHelper(Integer semesterPendek, boolean remedial) {
		this.semesterPendek = semesterPendek;
		this.remedial = remedial;
		tampilkanNilaiRinci = Common.bolehKonfigurasi("tampilkan_nilai_rinci_di_mahasiswa");
	}

	class DetailMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		private boolean refresh;
		private boolean rinci;

		public DetailMahasiswaRenderer(boolean refresh, boolean rinci) {
			this.refresh = refresh;
			this.rinci = rinci;
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");

			final Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
					.ambilData(Detailperkuliahan.class, data.toString());

			Matakuliah matakuliah = detailperkuliahan.getPerkuliahan() == null
					? detailperkuliahan.getMatakuliahKonversi()
					: detailperkuliahan.getPerkuliahan().getMatakuliah();
			Matakuliah[] matakuliahs = Common.getMatakuliahApakahEkivalen(matakuliah,
					mahasiswa == null ? null : mahasiswa.getNim(), refresh);
			matakuliah = matakuliahs[0];
			Matakuliah matakuliahAsli = matakuliahs[1];
			if (matakuliah == null) {
				row.setVisible(false);
				return;
			}

			if (tampilkanNilaiRinci && rinci) {
				final MyDetail detail = new MyDetail();
				detail.setParent(row);
				detail.addEventListener("onOpen", new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						tampilNilai(detailperkuliahan, detail);
					}
				});
			} else {
				new Label().setParent(row);
			}

			Perkuliahan perkuliahan = detailperkuliahan.getPerkuliahan();
			if (perkuliahan != null) {
				Kurikulum kurikulum = perkuliahan.getKurikulum();
				RevisiHelper
						.createNewRevisi(Perkuliahan.class, perkuliahan,
								perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama()
										+ " " + perkuliahan.getMatakuliah().getSks() + " sks "
										+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"))
						.setParent(row);

				Vbox vbox = new Vbox();
				vbox.setParent(row);
				ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);
			} else {
				new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getKode()
						: (matakuliah.getKode() + " (" + matakuliahAsli.getKode() + ")") + " "
								+ (matakuliah.getId().equals(matakuliahAsli.getId()) ? matakuliah.getNama()
										: (matakuliah.getNama() + " (" + matakuliahAsli.getNama() + ")")))
						.setParent(row);
				new Label().setParent(row);
			}

			new Label(matakuliah.getId().equals(matakuliahAsli.getId()) ? (matakuliah.getSks() + "")
					: (matakuliah.getSks() + " (" + matakuliahAsli.getSks() + ")")).setParent(row);

			if (perkuliahan != null) {
				ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(row, perkuliahan, true);
			} else {
				new Label().setParent(row);
			}

			if (detailperkuliahan.getTotalNilai() != null && detailperkuliahan.getTotalNilai() > 1.0) {
				new Label(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai())).setParent(row);

				new Label((detailperkuliahan.getNilaiHuruf())).setParent(row);
			} else {
				new Label().setParent(row);
				new Label().setParent(row);
			}

		}
	}

	@SuppressWarnings({ "unchecked" })
	public static void tampilNilai(final Detailperkuliahan detailperkuliahan, Component detail) {

		if (!PenilaianMahasiswaHelper.checkBolehLihatNilai(detailperkuliahan.getMahasiswa(),
				detailperkuliahan.getSemester())) {
			return;
		}

		Borderlayout borderlayout1 = new Borderlayout();
		borderlayout1.setHeight("500px");
		borderlayout1.setParent(detail instanceof MyDetail ? detail : Common.tampilanScroll(detail));

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		Tabbox tabbox1 = new Tabbox();
		tabbox1.setParent(center1);

		final Tabs tabs1 = new Tabs();
		tabs1.setParent(tabbox1);

		boolean tampilUjian = Common.bolehKonfigurasi("tampil_nilai_ujian_di_mahasiswa");
		boolean tampilTugas = Common.bolehKonfigurasi("tampil_nilai_tugas_di_mahasiswa");

		Tab tabTugas1 = new Tab("Nilai Total", "/img/options-icon.png");
		tabs1.appendChild(tabTugas1);

		Tab tabTugas2 = new Tab("Nilai Tugas", "/img/calendar-view-month-icon.png");
		tabs1.appendChild(tabTugas2);
		tabTugas2.setVisible(tampilTugas);

		Tab tabTugas3 = new Tab("Nilai Ujian", "/img/Text-Edit-icon.png");
		tabs1.appendChild(tabTugas3);
		tabTugas3.setVisible(tampilUjian);

		MyTabConfig tab1LihatRekapNilai = new MyTabConfig("Rekap Total Nilai",
				"/img/Actions-view-calendar-tasks-icon.png");
		tab1LihatRekapNilai.setVisible(detailperkuliahan.getPerkuliahan() != null);
		tab1LihatRekapNilai.setParent(tabs1);

		Tabpanels tabpanels1 = new Tabpanels();
		tabpanels1.setParent(tabbox1);
		Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
		tabpanelUtama1.setParent(tabpanels1);

		final Tabpanel tabpanelUtama2 = new ais.ui.util.MyTabpanel();
		tabpanelUtama2.setParent(tabpanels1);
		tabpanelUtama2.setVisible(tampilTugas);

		tabTugas2.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					Common.clear(tabpanelUtama2);

					RekapHasilTugasMahasiswa addWindow = new RekapHasilTugasMahasiswa(true,
							detailperkuliahan.getMahasiswa(), null, detailperkuliahan.ambilVOPembelajaran());
					addWindow.setClosable(false);
					addWindow.setHeight("100%");
					addWindow.setWidth("100%");
					tabpanelUtama2.appendChild(addWindow);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianMahasiswaHelper.java:236");
				}
			}
		});

		final Tabpanel tabpanelUtama3 = new ais.ui.util.MyTabpanel();
		tabpanelUtama3.setParent(tabpanels1);
		tabpanelUtama3.setVisible(tampilUjian);
		tabTugas3.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					Common.clear(tabpanelUtama3);

					RekapHasilUjianMahasiswa addWindow = new RekapHasilUjianMahasiswa(true,
							detailperkuliahan.getMahasiswa(), null, detailperkuliahan.ambilVOPembelajaran());
					addWindow.setClosable(false);
					addWindow.setHeight("100%");
					addWindow.setWidth("100%");
					tabpanelUtama3.appendChild(addWindow);

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PenilaianMahasiswaHelper.java:260");
				}
			}
		});

		final Tabpanel detailRekapNilai = new ais.ui.util.MyTabpanel();
		detailRekapNilai.setParent(tabpanels1);
		detailRekapNilai.setVisible(detailperkuliahan.getPerkuliahan() != null);
		tab1LihatRekapNilai.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(detailRekapNilai);
				ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(detailRekapNilai,
						detailperkuliahan.getPerkuliahan(), detailperkuliahan.getMahasiswa(),
						detailperkuliahan.getPerkuliahan().ambilFormatNilai(HibernateUtil.currentSession())
								.toArray(new FormatNilai[] {}));
				detailRekapNilai.setStyle("min-height: 500px;");
			}
		});

		Session session = HibernateUtil.currentSession();
		List<FormatNilai> formatNilais = Common.getFormatNilais(session, detailperkuliahan.getPerkuliahan());

		List<FormatNilaiTambahan> formatNilaiTambahans = session.createCriteria(FormatNilaiTambahan.class)
				.add(Restrictions.eq("perkuliahan", detailperkuliahan.getPerkuliahan()))
				.add(Restrictions.isNull("parent")).addOrder(Order.asc("id")).list();

		MyGrid mygrid = new MyGrid();
		mygrid.setWidth("100%");

		// my//grid.setOddRowSclass("non-odd");
		mygrid.setParent(tabpanelUtama1);

		Columns columns = new Columns();
		columns.setParent(mygrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jenis Penilaian");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persentase");
		column.setWidth("10%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("50%");
		column.setAlign("right");

		Rows rows = new Rows();
		rows.setParent(mygrid);
		synchronized (formatNilais) {
			for (FormatNilai formatNilai : formatNilais) {

				try {
					Double n = detailperkuliahan.retreiveDetailNilai(formatNilai);
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					new Label(formatNilai.getStatusPertemuan() == null ? "" : formatNilai.getNama()).setParent(row);
					new Label(formatNilai.getPersen() + "%").setParent(row);

					boolean verif = detailperkuliahan.retreiveDetailVerifikasiNilai(formatNilai);

					if (detailperkuliahan.getPerkuliahan() == null || verif
							|| !detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()) {
						new Label(Common.numberFormat.get().format(n)).setParent(row);
					} else {
						new Label("-").setParent(row);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianMahasiswaHelper.java:335");
					// TODO: handle exception
				}
			}
		}

		synchronized (formatNilaiTambahans) {
			for (FormatNilaiTambahan formatNilaiTambahan : formatNilaiTambahans) {
				try {
					Double n = detailperkuliahan.retreiveDetailNilaiTambahan(formatNilaiTambahan);
					if (n > 0.1) {
						MyFormRow row = new MyFormRow();
						row.setValign("top");
						row.setStyle("border:0px;background: transparent;color:blue;");
						row.setParent(rows);
						new Label(formatNilaiTambahan.getNamaNilai()).setParent(row);
						new Label("-").setParent(row);
						if (detailperkuliahan.getPerkuliahan() == null
								|| !detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()) {
							new Label(Common.numberFormat.get().format(n)).setParent(row);
						} else {
							new Label("-").setParent(row);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PenilaianMahasiswaHelper.java:359");
					// TODO: handle exception
				}

			}
		}
		Foot foot = new Foot();
		foot.setParent(mygrid);
		Footer footer = new Footer();
		footer.setParent(foot);
		footer = new Footer("Total");
		footer.setParent(foot);
		if (detailperkuliahan.getPerkuliahan() == null
				|| detailperkuliahan.getVerify().equals(Detailperkuliahan.VERIFIED)
				|| !detailperkuliahan.getPerkuliahan().getSembunyikanNilaiJikaBelumDiverifikasi()) {
			footer = new Footer(Common.numberFormat.get().format(detailperkuliahan.getTotalNilai()) + " "
					+ detailperkuliahan.getNilaiHuruf());
		} else {
			footer = new Footer("-");
		}
		footer.setParent(foot);
	}

	public void loadData(Object value) {
		boolean refresh = (value != null && value instanceof Boolean) ? (Boolean) value : false;
		detailperkuliahans = Common.getDetailperkuliahansSudahDinilai(mahasiswa, semester, tahapan, semesterPendek,
				refresh);
		// Matakuliah yang saling ekivalen cukup tampil SATU kali (nilai tertinggi).
		detailperkuliahans = EkivalenNilaiUtil.saringNilaiTertinggi(detailperkuliahans,
				mahasiswa == null ? null : mahasiswa.getNim());

		ListModel strset = new SimpleListModel(detailperkuliahans);
		grid.setRowRenderer(new DetailMahasiswaRenderer(refresh, true));
		grid.setModelCheckMobile(strset);

		detailperkuliahansbelum = Common.getDetailperkuliahansBelumDinilai(mahasiswa, semester, tahapan, semesterPendek,
				false);

		strset = new SimpleListModel(detailperkuliahansbelum);
		gridBelum.setRowRenderer(new DetailMahasiswaRenderer(refresh, false));
		gridBelum.setModelCheckMobile(strset);

		gridBelum.renderAll();

	}

	public void display(final Mahasiswa mahasiswa, final String tahunAjaran, final Integer semester,
			final Integer tahapan, final Component component, final MyWindow window, boolean keDatabase) {
		this.mahasiswa = mahasiswa;
		this.semester = semester;

		this.tahapan = tahapan;

		Common.clear(component);

		MyDiv vbox = new MyDiv();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(component);

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, tahapan, semesterPendek,
				keDatabase);

		Groupbox groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setWidth("95%");
		groupbox.setParent(vbox);
		groupbox.appendChild(new Caption("Informasi Penilaian Mahasiswa"));

		Row rowUtama = Common.tampilanScroll1(groupbox);

		rowUtama.getGrid().setVisible(semester > 0);
		if (tahapan != null && tahapan.equals(-1)) {
			rowUtama.getGrid().setVisible(false);
		}
		rowUtama.getGrid().setHeight("100%");
		rowUtama.getGrid().setWidth("100%");

		rowUtama.appendChild(new MyLabelConfig("Dosen Pembimbing Akademik"));
		Dosen dosenPembimbingAkademik = krsMahasiswa.getDosenPa();
		Label dosenPembimbing = new Label(dosenPembimbingAkademik == null ? "Belum memiliki dosen pembimbing akademik"
				: dosenPembimbingAkademik.getNama());
		dosenPembimbing.setParent(rowUtama);

		Row rowUtama1;
		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		} else {
			rowUtama1 = rowUtama;
		}

		rowUtama1.appendChild(new MyLabelConfig("IPS"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIps())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Semester"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksYangDiambil())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("IPK"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getIpk())));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("SKS Kumulatif"));
		rowUtama1.appendChild(new Label(Common.numberFormat.get().format(krsMahasiswa.getSksk())));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		String krs = mahasiswa.rubahKeteranganPengambilanKRS(semester, tahapan, semesterPendek, krsMahasiswa, remedial);
		rowUtama1.appendChild(new MyLabelConfig("Keterangan"));
		rowUtama1.appendChild(new Html(krs));

		rowUtama1 = new MyFormRow();
		rowUtama1.setStyle("border:0px;background: transparent;");
		rowUtama1.setParent(rowUtama.getParent());

		rowUtama1.appendChild(new MyLabelConfig("Tahun Akademik"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getTahunAkademik()));

		if (Common.isMobile()) {
			rowUtama1 = new MyFormRow();
			rowUtama1.setStyle("border:0px;background: transparent;");
			rowUtama1.setParent(rowUtama.getParent());
		}

		rowUtama1.appendChild(new MyLabelConfig("Semester"));
		rowUtama1.appendChild(new Label(krsMahasiswa.getSemester() + " / "
				+ (krsMahasiswa.getSemesterPendek() == null
						? (krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL)
						: Common.getBahasaConfig("Semester Pendek"))));

		groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setWidth("95%");
		groupbox.setParent(vbox);
		groupbox.appendChild(new Caption("Daftar matakuliah yang telah dinilai"));

		Toolbar toolbar = new Toolbar();
		toolbar.setVisible(semester != null && semester > 0);
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Cetak Nilai", "/img/print.png");
		toolbarbutton.setParent(toolbar);
		toolbarbutton.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				CommonReportHelper.cetakNilai(mahasiswa, semester, tahapan, semesterPendek, remedial, tahunAjaran);

			}
		});

		final MyToolbarbuttonConfig kurikulum = new MyToolbarbuttonConfig("Lihat Kurikulum", "/img/excel.png");
		toolbar.appendChild(kurikulum);
		kurikulum.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				LaporanKurikulumMahasiswa laporanKurikulum = new LaporanKurikulumMahasiswa(mahasiswa, semester);
				laporanKurikulum.setTitle("Riwayat Kurikulum");
				laporanKurikulum.setHeight("95%");
				laporanKurikulum.setWidth("90%");
				laporanKurikulum.setClosable(true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(laporanKurikulum);
				laporanKurikulum.onModal();
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(20);
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		if (tampilkanNilaiRinci) {
			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");
		}

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matakuliah");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jadwal");
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Total Nilai");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai Huruf");
		column.setWidth("10%");

		groupbox = new ais.ui.util.MyGroupboxStyled();
		groupbox.setWidth("95%");
		groupbox.setParent(vbox);
		groupbox.appendChild(new Caption("Daftar matakuliah yang belum dinilai"));

		gridBelum = new MyGrid();
		gridBelum.setMold("paging");
		gridBelum.setPageSize(20);
		gridBelum.setParent(groupbox);

		columns = new Columns();

		columns.setParent(gridBelum);

		if (tampilkanNilaiRinci) {
			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");
		}

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Matakuliah");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jadwal");
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(Common.getBahasa("label_dosen"));
		column.setWidth("35%");

		loadData(null);

	}

	public Integer getSemesterPendek() {
		return semesterPendek;
	}

	public void setSemesterPendek(Integer semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa) {
		return (mahasiswa == null) || checkBolehLihatNilai(mahasiswa, mahasiswa.currentSemester());
	}

	public static boolean checkBolehLihatNilai(Mahasiswa mahasiswa, int semester) {
		return NilaiValidator.checkBolehLihatNilai(mahasiswa, semester);

	}

}
