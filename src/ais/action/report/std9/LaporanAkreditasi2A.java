package ais.action.report.std9;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

public class LaporanAkreditasi2A extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox searchFakultas;
	private Combobox searchJurusan;

	private Center center;
	private Toolbar toolbar;

	@SuppressWarnings("rawtypes")
	private Map parameters = null;

	private Combobox tahunAjaran;

	public LaporanAkreditasi2A() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Akreditasi2 A", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanAkreditasi2A(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		searchFakultas = new Combobox();
		searchJurusan = new Combobox();
		Common.initFakultasDanJurusan(searchFakultas, searchJurusan, null, null);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onReport(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchFakultas);
		searchFakultas.setWidth("90%");
		searchFakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchJurusan);
		searchJurusan.setWidth("90%");
		searchJurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		Common.selectComboItem(tahunAjaran = Common.generateTahunAjaran(tahunAjaran), Common.getCurrentTahunAkademik());
		row.appendChild(tahunAjaran);
		tahunAjaran.setWidth("90%");
		tahunAjaran.setReadonly(true);
		tahunAjaran.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan Ulang"));
		tampilkan.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				return parameters;
			}
		}, "std9/2a", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		try {
			onReport(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi2A.java:177");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi2 A", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(Label label) throws Exception {

		Fakultas fakultas = (Fakultas) (searchFakultas.getSelectedItem() == null ? null
				: searchFakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (searchJurusan.getSelectedItem() == null ? null
				: searchJurusan.getSelectedItem().getValue());
		String tahunAkademik = (String) (this.tahunAjaran.getSelectedItem().getValue());
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas", fakultas == null ? "" : fakultas.getNama());
		parameters.put("jurusan", jurusan == null ? "" : jurusan.getNama());

		initData(label, parameters, fakultas, jurusan, tahunAkademik);

		return parameters;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initData(final Label label, final Map parameters, final Fakultas fakultas, final Jurusan jurusan,
			final String tahunAkademik) {

		new Thread(new Runnable() {

			@Override
			public void run() {

				Session session = ais.action.report.Report.openNativeSession();

				List<Map> maps = new ArrayList<Map>();

				String[][] dataJenjang = new String[][] {
						new String[] { "Program Doktor/Doktor Terapan/Subspesialis", "S3", "s3", "S3 Terapan", "S-3T",
								"Sp-2", "Strata 3" },
						new String[] { "Program Magister/Magister Terapan/Spesialis", "S2", "s2", "S2 Terapan", "S-2T",
								"Sp-1", "Strata 2" },
						new String[] { "Program Profesi", "Profesi" },
						new String[] { "Program Sarjana", "S1", "s1", "S1 Terapan", "S-1T", "Strata 1" },
						new String[] { "Program Diploma Empat / Sarjana Terapan", "D4" },
						new String[] { "Program Diploma Tiga", "D3" }, new String[] { "Program Diploma Dua", "D2" },
						new String[] { "Program Diploma Satu", "D1" } };

				for (String[] s : dataJenjang) {
					int size = 4;
					int index = 1;
					int ts = 4;
					int tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
					for (int rowIndex = tahun - 4; rowIndex <= tahun; rowIndex++) {
						String newTa = (rowIndex) + "/" + (rowIndex + 1);
						label.setValue("Sedang mem-proses data " + newTa + " (" + ((index * 100) / size) + " %)");
						index++;
						try {

							List<String> jenjangs = new ArrayList<String>();
							for (int i = 1; i < s.length; i++) {
								jenjangs.add(s[i]);
							}

							Number kapasitas = (Number) session.createCriteria(KapasitasMahasiswaBaru.class)
									.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
									.createAlias("jurusan.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))

									.add(jurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan", jurusan))
									.add(fakultas == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("jurusan.fakultas", fakultas))
									.add(Restrictions.eq("tahunAkademik", newTa))
									.setProjection(Projections.sum("jumlahTargetMahasiswaBaru")).uniqueResult();

							Criterion criterion = Restrictions.or(Restrictions.eq("prodi1", jurusan),
									Restrictions.eq("prodi2", jurusan));
							criterion = Restrictions.or(criterion, Restrictions.eq("prodi3", jurusan));
							criterion = Restrictions.or(criterion, Restrictions.eq("prodi4", jurusan));
							criterion = Restrictions.or(criterion, Restrictions.eq("prodi5", jurusan));

							Criterion criterionFak = Restrictions.or(Restrictions.eq("prodi1.fakultas", fakultas),
									Restrictions.eq("prodi2.fakultas", fakultas));
							criterionFak = Restrictions.or(criterionFak, Restrictions.eq("prodi3.fakultas", fakultas));
							criterionFak = Restrictions.or(criterionFak, Restrictions.eq("prodi4.fakultas", fakultas));
							criterionFak = Restrictions.or(criterionFak, Restrictions.eq("prodi5.fakultas", fakultas));

							Criterion criterionJenjang = Restrictions.or(Restrictions.in("jenjang1.nama", jenjangs),
									Restrictions.in("jenjang2.nama", jenjangs));
							criterionJenjang = Restrictions.or(criterionJenjang,
									Restrictions.in("jenjang3.nama", jenjangs));
							criterionJenjang = Restrictions.or(criterionJenjang,
									Restrictions.in("jenjang4.nama", jenjangs));
							criterionJenjang = Restrictions.or(criterionJenjang,
									Restrictions.in("jenjang5.nama", jenjangs));

							Number n1 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("prodi1", "prodi1", Criteria.LEFT_JOIN)
									.createAlias("prodi2", "prodi2", Criteria.LEFT_JOIN)
									.createAlias("prodi3", "prodi3", Criteria.LEFT_JOIN)
									.createAlias("prodi4", "prodi4", Criteria.LEFT_JOIN)
									.createAlias("prodi5", "prodi5", Criteria.LEFT_JOIN)

									.createAlias("prodi1.jenjang", "jenjang1", Criteria.LEFT_JOIN)
									.createAlias("prodi2.jenjang", "jenjang2", Criteria.LEFT_JOIN)
									.createAlias("prodi3.jenjang", "jenjang3", Criteria.LEFT_JOIN)
									.createAlias("prodi4.jenjang", "jenjang4", Criteria.LEFT_JOIN)
									.createAlias("prodi5.jenjang", "jenjang5", Criteria.LEFT_JOIN)

									.add(jurusan == null ? Restrictions.sqlRestriction("true") : criterion)
									.add(criterionFak == null ? Restrictions.sqlRestriction("true") : criterionFak)

									.add(criterionJenjang)

									.add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount())
									.uniqueResult();

							Number n2 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.isNotNull("prodiLulus"))
									.add(jurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("prodiLulus", jurusan))

									.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
									.createAlias("prodiLulus.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))
									.add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount())
									.uniqueResult();

							Number n3 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("merupakanPindahan", false))
									.add(Restrictions.isNotNull("mahasiswa"))
									.add(jurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("prodiLulus", jurusan))

									.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
									.createAlias("prodiLulus.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))
									.add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount())
									.uniqueResult();

							Number n4 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("merupakanPindahan", true))
									.add(Restrictions.isNotNull("mahasiswa"))
									.add(jurusan == null ? Restrictions.sqlRestriction("true")
											: Restrictions.eq("prodiLulus", jurusan))

									.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
									.createAlias("prodiLulus.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))
									.add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount())
									.uniqueResult();

							Number n5 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("merupakanPindahan", false))
									.add(Restrictions.isNotNull("jurusan")).add(jurusan == null
											? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))

									.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
									.createAlias("jurusan.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))
									.add(Restrictions.eq("tahunangkatan", rowIndex))
									.setProjection(Projections.rowCount()).uniqueResult();

							Number n6 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("merupakanPindahan", true))
									.add(Restrictions.isNotNull("jurusan")).add(jurusan == null
											? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))

									.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)
									.createAlias("jurusan.jenjang", "jenjang", Criteria.LEFT_JOIN)

									.add(Restrictions.in("jenjang.nama", jenjangs))
									.add(Restrictions.eq("tahunangkatan", rowIndex))
									.setProjection(Projections.rowCount()).uniqueResult();

							Map map = new java.util.HashMap();
							map.put("program", s[0]);

							map.put("tahunAkademik", "TS" + (ts > 0 ? "-" + (ts--) : ""));
							map.put("dayaTampung", kapasitas == null ? 0.0 : kapasitas.doubleValue());
							map.put("jumlahPendaftar", n1 == null ? 0.0 : n1.doubleValue());
							map.put("jumlahLulusSeleksi", n2 == null ? 0.0 : n2.doubleValue());
							map.put("jumlahRegulerBaru", n3 == null ? 0.0 : n3.doubleValue());
							map.put("jumlahTransferBaru", n4 == null ? 0.0 : n4.doubleValue());

							map.put("jumlahReguler", n5 == null ? 0.0 : n5.doubleValue());
							map.put("jumlahTransfer", n6 == null ? 0.0 : n6.doubleValue());

							maps.add(map);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/std9/LaporanAkreditasi2A.java:373");
							PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Akreditasi2 A", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
								new String[] {
									"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
									"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
									"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
								});
						}
					}
				}

				parameters.put("maps", maps);

				ais.action.report.Report.closeCurrentSessionQuietly();
				ais.action.report.helper.LoadingReportUtil.selesai(label);
			}
		}).start();

	}

	@SuppressWarnings({ "rawtypes" })
	public void onReport(Event event) throws Exception {

		final Label label = new Label(ais.common.Common.getBahasaConfig("Sedang memproses data ...."));
		final Map parameters = generateParameter(label);

		ais.action.report.helper.LoadingReportUtil.showBusy(label);
		final Timer timer = new Timer(500);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ais.action.report.helper.LoadingReportUtil.showBusy(label);
				if (ais.action.report.helper.LoadingReportUtil.isError(label)) {

					ais.action.report.helper.LoadingReportUtil.clearBusy();
					ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);
				} else if (ais.action.report.helper.LoadingReportUtil.isSelesai(label)) {

					ais.action.report.helper.LoadingReportUtil.clearBusy();
					ais.action.report.helper.LoadingReportUtil.stopAndDetach(timer);

					File file = Report.generateFileReportWithProgress(Report.PDF, parameters, "std9/2a", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
					CommonReport.tampilkanReportPDF(center, file);
				}

			}
		});
		timer.start();

	}

}
