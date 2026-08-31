package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Html;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi alumni jurusan berdasarkan tahun lulus.
 * Kelas ini mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak
 * tanpa memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Intbox dari}, {@code Intbox sampai},
 * {@code Combobox program}, {@code Center center}, {@code Toolbar toolbar}; inisialisasi/lifecycle ({@code
 * init()}); operasi domain lain ({@code generateParameter()}, {@code onRekap()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Intbox dari;
	private Intbox sampai;
	private Combobox program;

	private Center center;

	private Toolbar toolbar;

	public LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Rekapitulasi Alumni Jurusan Berdasarkan Tahun Lulus", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		program = Common.initPrograms(null);

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
		column.setWidth("45%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dari tahun Lulus"));
		row.appendChild(dari = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR) - 10));
		dari.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai tahun Lulus"));
		row.appendChild(sampai = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)));
		sampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyToolbarbuttonConfig refresh = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		refresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onRekap(event);
			}
		});
		refresh.setParent(row);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		new Html("<div style='padding:40px 24px;text-align:center;color:#999;font-size:13px;'>"
			+ "<div style='font-size:32px;margin-bottom:12px;'>&#128196;</div>"
			+ "<div>Isi filter di panel kiri lalu klik <b>Tampilkan</b> untuk melihat laporan.</div>"
			+ "</div>").setParent(center);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (dari.getValue() == null) {
					MyMessageboxConfig.show("Isi Dari tahun Lulus", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (sampai.getValue() == null) {
					MyMessageboxConfig.show("Isi sampai tahun Lulus", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "Rekapitulasi_alumni_jurusan_berdasar_tahun_lulus", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onRekap(arg0);

			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map generateParameter() {
		if (dari.getValue() == null || sampai.getValue() == null) {
			return null;
		}

		HibernateUtil.currentSession();
		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("dari", dari.getValue() == null ? "" : dari.getValue().intValue());
		parameters.put("sampai", sampai.getValue() == null ? "" : sampai.getValue().intValue());
		parameters.put("program", program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
				? "-1" : program.getSelectedItem().getValue());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onRekap(Event event) {

		try {

			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}

			File file = Report.generateFileReportWithProgress(Report.PDF, parameters,
					"Rekapitulasi_alumni_jurusan_berdasar_tahun_lulus", ais.ui.util.WaktuUtil.getDate(), 
					toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Alumni Jurusan Berdasarkan Tahun Lulus", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

		// try {
		//
		// //
		// System.out.print("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"+tahunAkademik.getSelectedItem().getValue());
		// // parameters.put("tahun_Lulus", tahunAngkatan.getValue()
		// // .intValue());
		//
		// Report.generatePDFReport(
		// reportType == null || reportType.getSelectedItem() == null ?
		// Report.PDF
		// : reportType.getSelectedItem().getValue()
		// .toString(), parameters,
		// "Rekapitulasi_alumni_jurusan_berdasar_tahun_lulus", ais.ui.util.WaktuUtil.getDate(),
		// Sessions
		// .getCurrent().getWebApp());
		//
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanRekapitulasiAlumniJurusanBerdasarkanTahunLulus.java:217");
		// // TODO Auto-generated catch block
		// Common.tampilErrorJikaAdmin(e); 
		// }
	}

}
