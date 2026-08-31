package ais.action.report.format1.sirs.rajal;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataDokterBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.ui.util.MyMessageboxConfig;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JenisPasien;
import ais.ui.util.MyDatebox;

/**
 * Penyusun/penyaji laporan untuk laporan kunjungan pasien rawat jalan per dokter window. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code MyDatebox
 * tanggal_mulai}, {@code MyDatebox tanggal_sampai}, {@code AmbilDataDokterBanbox dokter}, {@code Combobox
 * jenisPasien}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetak()}); operasi domain
 * lain ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanKunjunganPasienRawatJalanPerDokterWindow extends Window {

	private Center center;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	private MyDatebox tanggal_mulai;
	private MyDatebox tanggal_sampai;
	private AmbilDataDokterBanbox dokter;
	private Combobox jenisPasien;

	public LaporanKunjunganPasienRawatJalanPerDokterWindow() {
		super();
		try {

			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienRawatJalanPerDokterWindow.java:52");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kunjungan Pasien Rawat Jalan Per Dokter Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKunjunganPasienRawatJalanPerDokterWindow(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);

		init();
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);

		Div div = new Div();
		div.setParent(north);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new Grid();
		grid.setParent(div);
		grid.setWidth("100%");
		grid.setHeight("100%");

		// Columns columns = new Columns();
		// columns.setParent(grid);
		// Column column = new Column();
		// column.setWidth("30%");
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("30%");
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("40%");
		// column.setParent(columns);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onCetak(event);

			}
		};

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Dokter")));
		row.appendChild(dokter = new AmbilDataDokterBanbox());
		dokter.setWidth("90%");
		dokter.setEventListener(eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Mulai")));
		row.appendChild(tanggal_mulai = new MyDatebox(new Date()));
		tanggal_mulai.setWidth("90%");
		tanggal_mulai.addEventListener("onChange", eventListener);

		// row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Tanggal Sampai")));
		row.appendChild(tanggal_sampai = new MyDatebox(new Date()));
		tanggal_sampai.setWidth("90%");
		tanggal_sampai.addEventListener("onChange", eventListener);

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Pilih Jenis Pasien")));
		row.appendChild(jenisPasien = new Combobox());
		Common.insertCombo(jenisPasien, "nama", JenisPasien.class);
		jenisPasien.setSelectedIndex(0);
		jenisPasien.setWidth("90%");
		jenisPasien.addEventListener("onChange", eventListener);

		onCetak(null);

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/laporan_kunjungan_pasien_rawat_jalan_per_dokter"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (dokter.getAttribute("dokter") == null) {
			return null;
		}
		if (tanggal_mulai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum menentukan tanggal mulai. Silakan pilih terlebih dahulu tanggal mulai pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (tanggal_sampai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum menentukan tanggal sampai. Silakan pilih terlebih dahulu tanggal sampai pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (jenisPasien.getSelectedItem() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Bapak/Ibu belum memilih jenis pasien. Silakan pilih terlebih dahulu salah satu jenis pasien pada kolom yang tersedia sebelum melanjutkan proses pencetakan laporan.",
					"Peringatan", 1, MyMessageboxConfig.INFORMATION);
			return null;
		}

		JenisPasien myJenisPasien = (JenisPasien) jenisPasien.getSelectedItem().getValue();
		Dokter dokter = (Dokter) this.dokter.getAttribute("dokter");

		Map parameters = new HashMap();
		parameters.put("tanggalMulai", tanggal_mulai.getValue());
		parameters.put("tanggalSelesai", tanggal_sampai.getValue());
		parameters.put("nama_jenis_pasien", myJenisPasien.getNama());
		parameters.put("jenis_pasien", myJenisPasien.getId());
		parameters.put("dokter", dokter.getId());
		parameters.put("namaDokter", dokter.getNama());
		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetak(Event event) {

		try {
			Map parameters = generateParameter();
			if (parameters == null) {
				return;
			}
			File file = Report.generateFileReportWithProgress("sirs/laporan_kunjungan_pasien_rawat_jalan_per_dokter", Report.PDF,
					parameters, "sirs/laporan_kunjungan_pasien_rawat_jalan_per_dokter", new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/rajal/LaporanKunjunganPasienRawatJalanPerDokterWindow.java:204");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kunjungan Pasien Rawat Jalan Per Dokter Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
