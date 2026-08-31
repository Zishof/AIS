package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMasaPerkuliahanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.MasaPerkuliahan;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan angket perbandingan dosen window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox fakultas}, {@code Combobox jurusan}, {@code Combobox semesterAbsensi}, {@code AmbilDataDosenBanbox
 * dosen}, {@code Toolbar toolbar}, {@code Center center}, {@code Dosen dsn}; inisialisasi/lifecycle ({@code
 * init()}); operasi domain lain ({@code generateParameter()}, {@code onLaporanAngketDosenPerDosen()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanAngketPerbandinganDosenWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4766478176972379068L;
	private Combobox tahunAkademik;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox semesterAbsensi;
	private AmbilDataDosenBanbox dosen;
	private Toolbar toolbar;
	private Center center;
	private Dosen dsn;
	private Combobox program;

	private AmbilDataMasaPerkuliahanBanbox masaPerkuliahan;
	private MyCheckboxConfig semesterPendek;

	public LaporanAngketPerbandinganDosenWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Perbandingan Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketPerbandinganDosenWindow(Dosen dsn) {
		super();
		this.dsn = dsn;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Angket Perbandingan Dosen Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	public LaporanAngketPerbandinganDosenWindow(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);

		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onLaporanAngketDosenPerDosen(event);
			}
		};

		setHeight("100%");
		setWidth("100%");

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
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		program = Common.initPrograms(null);
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaran(tahunAkademik);
		tahunAkademik.setWidth("90%");

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
		Common.selectComboItem(semesterAbsensi, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		semesterAbsensi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Masa Perkuliahan"));
		row.appendChild(masaPerkuliahan = new AmbilDataMasaPerkuliahanBanbox());
		masaPerkuliahan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (jurusan.getSelectedItem() != null) {
					masaPerkuliahan.setJurusanSelected((Jurusan) jurusan.getSelectedItem().getValue());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(this.semesterPendek = new MyCheckboxConfig("Semester Pendek"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_dosen")));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setWidth("90%");
		dosen.setReadonly(true);

		if (dsn != null) {
			dosen.setAttribute("dosen", dsn);
			dosen.setAttribute("myValue", dsn);
			dosen.setValue(dsn.getNama());
		}

		Common.initKeterangan(rows, "Jika dosen tidak dipilih, maka akan tampil data semua dosen");

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		Hbox hbox = new Hbox();
		hbox.setParent(row);
		MyButtonConfig tombol;
		hbox.appendChild(tombol = new MyButtonConfig("Lihat Laporan", "/img/print.png"));
		hbox.appendChild(LaporanRekapAngketDosenPerJurusanWindow.hitungUlangAngket(tahunAkademik, semesterAbsensi,
				masaPerkuliahan));
		tombol.addEventListener("onClick", eventListener);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "rekap_angket_dosen_perbandingan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onLaporanAngketDosenPerDosen(arg0);
			}
		}));

		// if(dsn != null){
		// onLaporanAngketDosenPerDosen(null);
		// }
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		String genapGanjil = (String) (semesterAbsensi.getSelectedItem() == null
				|| semesterAbsensi.getSelectedItem().getValue() == null ? "Semua"
						: semesterAbsensi.getSelectedItem().getValue());

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? "Semua"
						: this.tahunAkademik.getSelectedItem().getValue());

		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());

		Dosen dosen = (Dosen) this.dosen.getAttribute("dosen");

		// if (jurusan == null && dosen == null) {
		// MyMessageboxConfig.show(
		// Common.getBahasaConfig("Jurusan") + " atau Dosen harus dipilih, jika
		// "
		// + Common.getBahasaConfig("Jurusan")
		// + " tidak dipilih, maka Dosen wajib dipilih, begitu juga sebalik-nya,
		// atau juga bisa memilih dua-dua-nya",
		// "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return null;
		// }

		Long f = fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId();
		Long j = jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId();

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("genapGanjil", genapGanjil == null ? "Semua" : genapGanjil);
		parameters.put("tahun_akademik", tahunAkademik == null ? "Semua" : tahunAkademik);
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());
		parameters.put("nama_dosen", dosen == null ? "" : dosen.getNama());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("jurusan_nama", jurusan == null ? "" : jurusan.getNama());
		parameters.put("program", program == null ? "-1" : program);
		parameters.put("fakultas_nama", fakultas == null ? "" : fakultas.getNama());

		MasaPerkuliahan masaPerkuliahan = (MasaPerkuliahan) this.masaPerkuliahan.getAttribute("masaPerkuliahan");

		parameters.put("semester_pendek", semesterPendek.isChecked() ? Perkuliahan.SEMESTER_PENDEK : -1L);
		parameters.put("masa_perkuliahan", masaPerkuliahan == null || masaPerkuliahan.getId() == null ? -1L : masaPerkuliahan.getId());

		String sqlRata = "select   a.id as isi_id,  sum(x.nilai1) as \"nilai 1\", "
				+ "sum(x.nilai2) as \"nilai 2\",  sum(x.nilai3) as \"nilai 3\", "
				+ "sum(x.nilai4) as \"nilai 4\",  sum(x.nilai5) as \"nilai 5\", "
				+ "sum(x.total) as \"total\", b.id as grup_id  from rekap_angket_dosen x "
				+ "inner join checklist_penilaian_dosen a on (a.id=x.checklist_penilaian_dosen) "
				+ "inner join grup_checklist_penilaian_dosen b on (a.grup_checklist_penilaian_dosen=b.id) "
				+ "inner join perkuliahan c on (x.perkuliahan=c.id) "
				+ "inner join jurusan d on (c.jurusan=d.id) where c.tahun_ajaran='" + tahunAkademik + "' "
				+ "and c.ganjil_genap = '" + genapGanjil.replace("'", "''") + "' "
				+ "and (a.aktif or a.aktif is null) and (b.aktif or b.aktif is null)  and a.aktif=true "
				+ "and b.aktif=true " + (f.equals(-1L) ? "" : " and d.fakultas=" + f)
				+ (j.equals(-1L) ? "" : " and c.jurusan=" + j) + "    GROUP BY b.id,a.id  order by b.id,a.id";

		Session session = HibernateUtil.currentSession();
		List<Object[]> objs = session.createSQLQuery(sqlRata).list();

		Double jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa = 5.0;
		try {
			jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa = Double.parseDouble(
					Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa", "5").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanAngketPerbandinganDosenWindow.java:327");
			// TODO: handle exception
		}

		Double totalNilai = 0.0;
		Double totalNilaiSemua = 0.0;
		Number grup_id_n = -1L;
		int dibagi = 0;
		int dibagiTotal = 0;
		for (Object[] objects : objs) {
			Number id = (Number) objects[0];
			Number n1 = (Number) objects[1];
			Number n2 = (Number) objects[2];
			Number n3 = (Number) objects[3];
			Number n4 = (Number) objects[4];
			Number n5 = (Number) objects[5];
			Number total = (Number) objects[6];
			Number grup_id = (Number) objects[7];

			if (grup_id_n.equals(grup_id.longValue())) {
				totalNilai = 0.0;
				dibagi = 0;
			}

			Double n1_ = n1 == null ? 0.0 : n1.doubleValue();
			Double n2_ = n2 == null ? 0.0 : n2.doubleValue();
			Double n3_ = n3 == null ? 0.0 : n3.doubleValue();
			Double n4_ = n4 == null ? 0.0 : n4.doubleValue();
			Double n5_ = n5 == null ? 0.0 : n5.doubleValue();
			Double total_ = total == null ? 0.0 : total.doubleValue();

			Double nilai = (n1_ * 1) + (n2_ * 2) + (n3_ * 3) + (n4_ * 4) + (n5_ * 5);
			nilai = (nilai * 100.0) / (total_ * jumlah_pilihan_checklist_penilaian_dosen_oleh_mahasiswa);

			parameters.put("n1_" + id, n1_);
			parameters.put("n2_" + id, n2_);
			parameters.put("n3_" + id, n3_);
			parameters.put("n4_" + id, n4_);
			parameters.put("n5_" + id, n5_);
			parameters.put("total_" + id, total_);
			parameters.put("nilai_" + id, nilai);

			dibagi++;
			dibagiTotal++;
			totalNilai += nilai;
			totalNilaiSemua += nilai;
			parameters.put("nilai_grup_" + grup_id, totalNilai / dibagi);
			parameters.put("nilai_total", totalNilaiSemua / dibagiTotal);
		}

		return parameters;

	}

	@SuppressWarnings({})
	public void onLaporanAngketDosenPerDosen(Event event) throws Exception {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
						"rekap_angket_dosen_perbandingan", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		});
	}

}
