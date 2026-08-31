package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.KonfigurasiNewAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.RekapUjianDosenBulanan;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan penilaian per dosen window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox semesterAbsensi}, {@code Combobox program}, {@code Combobox fakultas}, {@code Combobox jurusan},
 * {@code AmbilDataDosenBanbox dosen}, {@code Toolbar toolbar}, {@code Center center}; inisialisasi/lifecycle
 * ({@code init()}); operasi domain lain ({@code generateParameter()}, {@code generateDataDanImageAlbum()},
 * {@code onLaporanAngketDosenPerDosen()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPenilaianPerDosenWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private Combobox program;
	private Combobox fakultas;
	private Combobox jurusan;
	// private MyDoublebox nilai;
	private AmbilDataDosenBanbox dosen;
	private Toolbar toolbar;
	private Center center;
	private Row rowHonor;
	private MyCheckboxConfig semeterpendek;

	private MyDatebox mulai;
	private Row rowHonorPertemuan;

	public LaporanPenilaianPerDosenWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Penilaian Per Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanPenilaianPerDosenWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("500px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");
		// tahunAkademik.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(semesterAbsensi = new Combobox());
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterAbsensi.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterAbsensi.appendChild(comboitem);
		semesterAbsensi.setWidth("90%");

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semesterAbsensi.appendChild(comboitem);

		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas = new Combobox());
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan = new Combobox());
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		program = Common.initPrograms(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester Pendek"));
		row.appendChild(semeterpendek = new MyCheckboxConfig("Tampilkan perkuliahan semester pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);
		Common.initKeterangan(rows, "Kosongkan data dosen jika untuk semua dosen");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masuk Payrol Per Tanggal/Bulan"));
		row.appendChild(mulai = new MyDatebox(WaktuUtil.getDate()));
		mulai.setReadonly(true);

		// dosen.setEventListener(eventListener);

		// nilai = new MyDoublebox(Double.parseDouble(defaultHonor));

		// rowHonor.setParent(rows);
		// rowHonor.appendChild(new Label(ais.common.Common.getBahasaConfig("Honor Koreksi")));

		if (Common.getCurrentUser().getDosen() != null) {

			// rowHonor.appendChild(new
			// Label(Common.numberFormat.get().format(Double.parseDouble(defaultHonor))));
		} else {

			final Combobox jenisUjian = new Combobox();
			String[] data = new String[] { "UTS", "UAS" };
			for (String d : data) {
				comboitem = new MyComboitemConfig(d);
				comboitem.setValue(d);
				jenisUjian.appendChild(comboitem);
			}
			jenisUjian.setReadonly(true);

			MyComboitemConfig semuaUjian = new MyComboitemConfig("Semua Ujian");
			semuaUjian.setValue("");
			jenisUjian.appendChild(semuaUjian);
			jenisUjian.setSelectedItem(semuaUjian);

			rowHonor = KonfigurasiNewAction.createRowNilaiProgramDanJurusan("Honor koreksi",
					"default_nilai_honor_koreksi", "0", 1, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, jenisUjian);
			ais.ui.util.ZkCompat.setSpans(rowHonor, "2");
			rowHonor.setParent(rows);
			rowHonor.setVisible(
					Common.bolehKonfigurasi("tampilkan_honor_koreksi_yang_telah_diverifikasi", Konfigurasi.TIDAK_AKTIF));
			rowHonor.setStyle("border:0px;background: transparent;");

			final Combobox pertemuanKe = new Combobox();
			for (int p = 1; p <= 16; p++) {
				comboitem = new MyComboitemConfig(p + "");
				comboitem.setValue(p);
				pertemuanKe.appendChild(comboitem);
			}
			pertemuanKe.setReadonly(true);

			semuaUjian = new MyComboitemConfig("Semua Pertemuan");
			semuaUjian.setValue(null);
			pertemuanKe.appendChild(semuaUjian);
			pertemuanKe.setSelectedItem(semuaUjian);

			rowHonorPertemuan = KonfigurasiNewAction.createRowNilaiProgramDanJurusan("Honor pertemuan",
					"default_nilai_honor_pertemuan", "0", 1, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, pertemuanKe);
			ais.ui.util.ZkCompat.setSpans(rowHonorPertemuan, "2");
			rowHonorPertemuan.setParent(rows);
			rowHonorPertemuan.setVisible(
					Common.bolehKonfigurasi("tampilkan_honor_pertemuan_yang_telah_diverifikasi"));
			rowHonorPertemuan.setStyle("border:0px;background: transparent;");

		}

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		});
		print.setParent(row);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		row.appendChild(new MyLabelAgakKecil("Jumlah Mhs UTS => V_SUM_UTS, Jumlah Mhs UAS => V_SUM_UAS"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		row.appendChild(new MyLabelAgakKecil(
				"Jumlah Mhs UTS/Jml Dosen => V_SUM_UTS_PER_DOSEN, Jumlah Mhs UAS/Jml Dosen => V_SUM_UAS_PER_DOSEN, Jumlah Dosen => V_SUM_JML_DOSEN_UJIAN"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		row.appendChild(
				new MyLabelAgakKecil("Jumlah Ujian UTS ESSAY => V_SUM_UTS_ESSAY, Jumlah Ujian UTS PG => V_SUM_UTS_PG"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		row.appendChild(new MyLabelAgakKecil(
				"Jumlah Ujian UTS ESSAY/Jml Dosen => V_SUM_UTS_ESSAY_PER_DOSEN, Jumlah Ujian UTS PG/Jml Dosen => V_SUM_UTS_PG_PER_DOSEN"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(
				new MyLabelAgakKecil("Jumlah Ujian UAS ESSAY => V_SUM_UAS_ESSAY, Jumlah Ujian UAS PG => V_SUM_UAS_PG"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelAgakKecil(
				"Jumlah Ujian UAS ESSAY/Jml Dosen => V_SUM_UAS_ESSAY_PER_DOSEN, Jumlah Ujian UAS PG/Jml Dosen => V_SUM_UAS_PG_PER_DOSEN"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelAgakKecil("Jumlah Ujian UTS TUGAS => V_SUM_UTS_TUGAS"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new MyLabelAgakKecil("Jumlah Ujian UAS TUGAS => V_SUM_UAS_TUGAS"));

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_koreksi_nilai_dosen_per_dosen", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

		// Common.createDefaultTimer(new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onLaporanAngketDosenPerDosen(arg0);
		// }
		// });
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		parameters.put("tahun_akademik", tahunAkademik);
		parameters.put("semester", genapGanjil);

		if (maps != null) {
			parameters.put("maps", maps);
		}
		return parameters;

	}

	@SuppressWarnings("rawtypes")
	private ArrayList<Map> maps = null;

	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	protected void generateDataDanImageAlbum(Label label) {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(mulai.getValue());
		final int bln = calendar.get(Calendar.MONTH);
		final int tahun = calendar.get(Calendar.YEAR);

		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? null
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? null
						: this.tahunAkademik.getSelectedItem().getValue());

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		Criterion criterion = Restrictions.eq("dosen1", dosen);
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen2", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		Session session1 = ais.action.report.Report.openNativeSession();
		List<Perkuliahan> perkuliahans = ConstantValues.simpleList(session1.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(!semeterpendek.isChecked() ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(fakultas == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jurusan.fakultas", fakultas))
				.add(jurusan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("jurusan", jurusan))
				.add(program == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("program", program))

				.add(dosen == null ? Restrictions.sqlRestriction("true") : criterion)

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("tahunAjaran", tahunAkademik))

				.add(genapGanjil == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("ganjilGenap", genapGanjil))

				.addOrder(Order.asc("dosen1")).addOrder(Order.asc("dosen2")).addOrder(Order.asc("dosen3"))
				.addOrder(Order.asc("dosen4")).addOrder(Order.asc("dosen5")).addOrder(Order.asc("dosen6"))
				.addOrder(Order.asc("dosen7")).addOrder(Order.asc("dosen8")).addOrder(Order.asc("dosen9"))
				.addOrder(Order.asc("dosen10"))

				, Perkuliahan.class);
		session1.disconnect();
		session1.close();
		ais.action.report.Report.closeCurrentSessionQuietly();

		String sql = "sum(case when jenis='" + BankSoal.ESAY + "' then 1 else 0 end) as essay,"
				+ "sum(case when jenis!='" + BankSoal.ESAY + "' then 1 else 0 end) as pg";
		String ss = "";
		if (dosen != null && dosen.getId() != null) {
			ss = dosen.getId().toString();
		} else {
			for (Perkuliahan perkuliahan : perkuliahans) {
				for (Dosen d : perkuliahan.populateDosenBuNama()) {
					ss += ss.isEmpty() ? d.getId().toString() : "," + d.getId();
				}
			}
		}

		try {
			if (!ss.isEmpty()) {

				String hapus = "delete from rekap_ujian_dosen_bulanan where dosen in (" + ss + ") and tahun = " + tahun
						+ " and bulan = " + (bln + 1);

				Session session = ais.action.report.Report.openNativeSession();
				session.getTransaction().begin();
				int hasil = session.createSQLQuery(hapus).executeUpdate();
				session.getTransaction().commit();
				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}
				ais.action.report.Report.closeCurrentSessionQuietly();

				System.out.println("hapus -> " + hapus + " hasil -> " + hasil);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:501");
			PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Penilaian Per Dosen Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
				new String[] {
					"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
					"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
		int size = 1;
		Map<Long, List<Perkuliahan>> list = new HashMap<Long, List<Perkuliahan>>();
		for (Perkuliahan perkuliahan : perkuliahans) {

			for (Dosen dsn : perkuliahan.populateDosenBuNama()) {
				if (dosen == null || (dosen != null && dosen.getId().equals(dsn.getId()))) {
					List<Perkuliahan> p = list.get(dsn.getId());
					if (p == null) {
						p = new ArrayList<Perkuliahan>();
						list.put(dsn.getId(), p);
					}
					p.add(perkuliahan);
					size++;
				}
			}
		}

		maps = new ArrayList<Map>();
		int index = 0;
		Set<String> strings = new HashSet<String>();
		for (Long dosenId : list.keySet()) {
			Dosen dsn = (Dosen) ConstantValues.ambil(Dosen.class.getName(), dosenId);
			List<Perkuliahan> p = list.get(dosenId);
			for (Perkuliahan perkuliahan : p) {

				try {
					Session session = ais.action.report.Report.openNativeSession();
					label.setValue("Memproses data " + perkuliahan + " ("
							+ Common.numberFormat.get().format((index * 100.0) / size) + "%)");
					index++;

					Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
							.createCriteria(Detailperkuliahan.class).add(Restrictions.eq("perkuliahan", perkuliahan))
							.setMaxResults(1).add(Restrictions.isNotNull("waktuVerifikasi"))
							.add(Restrictions.isNotNull("verifikator")).addOrder(Order.desc("waktuVerifikasi"))
							.uniqueResult();
					Map<Integer, Integer> nilias = new HashMap<Integer, Integer>();
					int uts = 0;
					int uas = 0;
					int jumlah = 0;

					List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
					if (detailperkuliahan != null) {

						Collection<Long> detailperkuliahans = perkuliahan.ambilDetailperkuliahanDisetujui();
						int ke = 1;
						for (FormatNilai formatNilai : formatNilais) {
							for (Long detailperkuliahanid : detailperkuliahans) {
								Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject
										.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
								if (d != null) {
									Boolean verify = d.retreiveDetailVerifikasiNilai(formatNilai);
									uts += (formatNilai.getNama().trim().equalsIgnoreCase("UTS")
											|| (formatNilai.getJenisEvaluasi() != null
													&& formatNilai.getJenisEvaluasi().getNama() != null
													&& formatNilai.getJenisEvaluasi().getNama().trim()
															.equalsIgnoreCase("UTS")))
											&& verify ? 1 : 0;
									uas += ((formatNilai.getNama().trim().equalsIgnoreCase("UAS")
											|| (formatNilai.getJenisEvaluasi() != null
													&& formatNilai.getJenisEvaluasi().getNama() != null
													&& formatNilai.getJenisEvaluasi().getNama().trim()
															.equalsIgnoreCase("UAS")))
											|| formatNilai.getNama().trim().equalsIgnoreCase("TOTAL")
											|| formatNilais.size() == 1) && verify ? 1 : 0;

									Integer nn = nilias.get(ke);
									if (nn == null) {
										nn = 0;
									}
									nn++;
									nilias.put(ke, nn);
								}
							}
							ke++;
						}
						jumlah = uts + uas;
					}
					Map map = new java.util.HashMap();

					Common.insertProperty(Perkuliahan.class, perkuliahan, map, "perkuliahan");
					map.put("fakultas",
							perkuliahan.getJurusan() == null || perkuliahan.getJurusan().getFakultas() == null ? ""
									: perkuliahan.getJurusan().getFakultas().getNama());
					map.put("jurusan", perkuliahan.getJurusan() == null ? "" : perkuliahan.getJurusan().getNama());
					map.put("kelas", perkuliahan.getSemester() + " " + perkuliahan.getKelas());
					map.put("matakuliah", perkuliahan.getMatakuliah().getNama());
					map.put("tanggal_verifikasi", detailperkuliahan == null ? "Belum diverifikasi"
							: Common.dateFormat5.get().format(detailperkuliahan.getWaktuVerifikasi()));
					map.put("nama_verifikasi", detailperkuliahan == null ? "-" : detailperkuliahan.getVerifikator());
					map.put("uts", uts);
					map.put("uas", uas);
					map.put("jumlah", jumlah);

					for (Integer ke : nilias.keySet()) {
						Integer nn = nilias.get(ke);
						if (nn == null) {
							nn = 0;
						}
						map.put("nilai_ke_" + ke, nn);
					}

					map.put("obe",
							perkuliahan.getKurikulum() == null ? false
									: perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
											perkuliahan.getGanjilGenap()));

					Integer utsTugas = 0;
					Integer uasTugas = 0;

					Integer utsUjianPg = 0;
					Integer uasUjianPg = 0;

					Integer utsUjianEssay = 0;
					Integer uasUjianEssay = 0;

					List<Pertemuan> pertemuans = perkuliahan.ambilPertemuanList();
					for (Pertemuan pertemuan : pertemuans) {
						if (pertemuan.getStatusPertemuan() != null && ((pertemuan.getFormatNilai() != null
								&& pertemuan.getFormatNilai().getNama().trim().equalsIgnoreCase("UTS"))
								|| pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("UTS"))) {
							Object[] ujians = (Object[]) session.createCriteria(PertemuanPunyaUjian.class)
									.createAlias("ujian", "ujian").add(Restrictions.eq("pertemuan", pertemuan))
									.setProjection(Projections.sqlProjection(sql, new String[] { "essay", "pg" },
											new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE }))
									.uniqueResult();

							int pg = ((Number) (ujians[1] == null ? 0 : ujians[1])).intValue();
							int es = ((Number) (ujians[0] == null ? 0 : ujians[0])).intValue();
							map.put("utsUjianPg_" + pertemuan.getPertemuanKe(), pg);
							map.put("utsUjianEssay_" + pertemuan.getPertemuanKe(), es);

							utsUjianPg += pg;
							utsUjianEssay += es;

							if (!pertemuan.getJudultugas().isEmpty()) {
								utsTugas++;
							}

							int tgs = ((Number) session.createCriteria(TugasPertemuan.class)
									.add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas"))
									.add(Restrictions.eq("pertemuan", pertemuan.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							map.put("utsTgs_" + pertemuan.getPertemuanKe(), tgs);
							utsTugas += tgs;

							tgs = ((Number) session.createCriteria(TugasKelompok.class)
									.add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul"))
									.add(Restrictions.eq("pertemuan", pertemuan.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							map.put("utsTgsKel_" + pertemuan.getPertemuanKe(), tgs);
							utsTugas += tgs;

						} else if (pertemuan.getStatusPertemuan() != null && ((pertemuan.getFormatNilai() != null
								&& (pertemuan.getFormatNilai().getNama().trim().equalsIgnoreCase("UAS"))
								|| pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("UAS")
								|| pertemuan.getStatusPertemuan().getNama().equalsIgnoreCase("TOTAL")
								|| formatNilais.size() == 1))) {
							Object[] ujians = (Object[]) session.createCriteria(PertemuanPunyaUjian.class)
									.createAlias("ujian", "ujian").add(Restrictions.eq("pertemuan", pertemuan))
									.setProjection(Projections.sqlProjection(sql, new String[] { "essay", "pg" },
											new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE }))
									.uniqueResult();

							int pg = ((Number) (ujians[1] == null ? 0 : ujians[1])).intValue();
							int es = ((Number) (ujians[0] == null ? 0 : ujians[0])).intValue();

							map.put("uasUjianPg_" + pertemuan.getPertemuanKe(), pg);
							map.put("uasUjianEssay_" + pertemuan.getPertemuanKe(), es);

							uasUjianPg += pg;
							uasUjianEssay += es;

							if (!pertemuan.getJudultugas().isEmpty()) {
								uasTugas++;
							}

							int tgs = ((Number) session.createCriteria(TugasPertemuan.class)
									.add(Restrictions.ne("judultugas", "")).add(Restrictions.isNotNull("judultugas"))
									.add(Restrictions.eq("pertemuan", pertemuan.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							map.put("uasTgs_" + pertemuan.getPertemuanKe(), tgs);
							uasTugas += tgs;

							tgs = ((Number) session.createCriteria(TugasKelompok.class)
									.add(Restrictions.ne("judul", "")).add(Restrictions.isNotNull("judul"))
									.add(Restrictions.eq("pertemuan", pertemuan.getId()))
									.setProjection(Projections.rowCount()).uniqueResult()).intValue();
							map.put("uasTgsKel_" + pertemuan.getPertemuanKe(), tgs);
							uasTugas += tgs;
						}
					}

					double dibagi = perkuliahan.getJumlahDosen().doubleValue();
					map.put("dibagi", dibagi);

					map.put("utsUjianPg", ((double) utsUjianPg) / dibagi);
					map.put("utsUjianEssay", ((double) utsUjianEssay) / dibagi);
					map.put("uasUjianPg", ((double) uasUjianPg) / dibagi);
					map.put("uasUjianEssay", ((double) uasUjianEssay) / dibagi);

					map.put("utsTugas", ((double) utsTugas) / dibagi);
					map.put("uasTugas", ((double) uasTugas) / dibagi);

					map.put("utsUjian", ((double) (utsUjianPg + utsUjianEssay)) / dibagi);
					map.put("uasUjian", ((double) (uasUjianPg + uasUjianEssay)) / dibagi);

					try {
						RekapUjianDosenBulanan rekapUjianDosenBulanan = new RekapUjianDosenBulanan();

						rekapUjianDosenBulanan.setUtsTugas(utsTugas);
						rekapUjianDosenBulanan.setUasTugas(uasTugas);

						rekapUjianDosenBulanan.setNama(dsn.getNama());
						rekapUjianDosenBulanan.setBulan(bln + 1);
						rekapUjianDosenBulanan.setTahun(tahun);
						rekapUjianDosenBulanan.setDosen(dsn.getId());
						rekapUjianDosenBulanan.setPerkuliahan(perkuliahan.getId());
						rekapUjianDosenBulanan.setSmt(perkuliahan.getIdSmt());
						rekapUjianDosenBulanan.setUas(uas);
						rekapUjianDosenBulanan.setUts(uts);

						rekapUjianDosenBulanan
								.setUasDibagiJmlDosen(((double) uas) / perkuliahan.getJumlahDosen().doubleValue());

						rekapUjianDosenBulanan
								.setUtsDibagiJmlDosen(((double) uts) / perkuliahan.getJumlahDosen().doubleValue());

						rekapUjianDosenBulanan.setJmlDosen(perkuliahan.getJumlahDosen());

						rekapUjianDosenBulanan.setUtsUjianPg(utsUjianPg);
						rekapUjianDosenBulanan.setUtsUjianEssay(utsUjianEssay);
						rekapUjianDosenBulanan.setUasUjianPg(uasUjianPg);
						rekapUjianDosenBulanan.setUasUjianEssay(uasUjianEssay);

						rekapUjianDosenBulanan.setUtsUjianPgJmlDosen(
								utsUjianPg.doubleValue() / perkuliahan.getJumlahDosen().doubleValue());
						rekapUjianDosenBulanan.setUtsUjianEssayJmlDosen(
								utsUjianEssay.doubleValue() / perkuliahan.getJumlahDosen().doubleValue());
						rekapUjianDosenBulanan.setUasUjianPgJmlDosen(
								uasUjianPg.doubleValue() / perkuliahan.getJumlahDosen().doubleValue());
						rekapUjianDosenBulanan.setUasUjianEssayJmlDosen(
								uasUjianEssay.doubleValue() / perkuliahan.getJumlahDosen().doubleValue());

						rekapUjianDosenBulanan.setTanggalMulai(mulai.getValue());

						session.getTransaction().begin();
						session.save(rekapUjianDosenBulanan);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:753");
						PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Penilaian Per Dosen Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
							new String[] {
								"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
								"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
					}

					String defaultHonor = Common.getKonfigurasi("default_nilai_honor_koreksi", "0",
							perkuliahan.getProgram(), perkuliahan.getJurusan(), "UTS").getNilai().trim();
					Double nilai_honor = 0.0;
					try {
						nilai_honor = Double.parseDouble(defaultHonor);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:761");
//					Common.tampilErrorJikaAdmin(e);
					}
					map.put("nilai_honor_uts", nilai_honor);

					defaultHonor = Common.getKonfigurasi("default_nilai_honor_koreksi", "0", perkuliahan.getProgram(),
							perkuliahan.getJurusan(), "UAS").getNilai().trim();
					nilai_honor = 0.0;
					try {
						nilai_honor = Double.parseDouble(defaultHonor);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:771");
//					Common.tampilErrorJikaAdmin(e);
					}
					map.put("nilai_honor_uas", nilai_honor);

					for (int pe = 1; pe <= 16; pe++) {
						defaultHonor = Common.getKonfigurasi("default_nilai_honor_pertemuan", "0",
								perkuliahan.getProgram(), perkuliahan.getJurusan(), pe + "").getNilai().trim();
						nilai_honor = 0.0;
						try {
							nilai_honor = Double.parseDouble(defaultHonor);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:782");
//							Common.tampilErrorJikaAdmin(e);
						}
						map.put("nilai_pertemuan_" + pe, nilai_honor);
					}

					defaultHonor = Common.getKonfigurasi("default_nilai_honor_pertemuan", "0", perkuliahan.getProgram(),
							perkuliahan.getJurusan(), "").getNilai().trim();
					nilai_honor = 0.0;
					try {
						nilai_honor = Double.parseDouble(defaultHonor);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:793");
//						Common.tampilErrorJikaAdmin(e);
					}
					map.put("nilai_pertemuan", nilai_honor);

					String namaDosen = dsn.getNama();
					String dosenIds = dsn.getId() + "";

					map.put("jumlah_dosen", dibagi);
					map.put("nama_dosen", namaDosen);
					map.put("id_dosen", dosenIds);

					strings.add(dosenIds);

					map.put("nomor", strings.size());

					maps.add(map);

					// session.disconnect();
					if (session.isOpen()) {session.disconnect();session.close();}

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:815");
					PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Penilaian Per Dosen Window", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
						new String[] {
							"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
							"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
			}
		}

		ais.action.report.helper.LoadingReportUtil.selesai(label);
	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {

		generateParameter();

		final Label label = Common.displayLoadBar(new EventListener() {

			@SuppressWarnings({})
			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"rekap_koreksi_nilai_dosen_per_dosen", ais.ui.util.WaktuUtil.getDate(), null, toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					generateDataDanImageAlbum(label);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanPenilaianPerDosenWindow.java:847");
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Penilaian Per Dosen Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
						new String[] {
							"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
							"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
							"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
						});
				}
			}
		}).start();

	}

}
