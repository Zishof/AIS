package ais.action.report.format1.sirs.inventory;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Window;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.common.Common;
import ais.database.model.asset.Lokasi;

/**
 * Penyusun/penyaji laporan untuk laporan kadaluarsa window. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox lokasi}, {@code Center
 * center}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetakStatusPasien()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanKadaluarsaWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox lokasi;
	// private MyDatebox perTanggal;
	private Center center;

	public LaporanKadaluarsaWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanKadaluarsaWindow.java:46");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kadaluarsa Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanKadaluarsaWindow(String title, String border, boolean closable)
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

		Columns columns = new Columns();
		columns.setParent(grid);
		Column column = new Column();
		column.setWidth("30%");
		column.setParent(columns);
		column = new Column();
		column.setWidth("70%");
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Lokasi")));
		row.appendChild(lokasi = new Combobox());
		Common.insertCombo(lokasi, "nama", Lokasi.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Lokasi myLokasi = Common.getCurrentLokasi();
		Common.selectComboItem(lokasi, myLokasi);
		// lokasi.setDisabled(myLokasi != null);
		lokasi.setWidth("90%");
		lokasi.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onCetakStatusPasien(arg0);
			}
		});

		// row = new Row();
		// row.setStyle("border:0px;background: transparent;");
		// row.setParent(rows);
		// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Per Tanggal")));
		// row.appendChild(perTanggal = new MyDatebox(new Date()));
		// perTanggal.setWidth("90%");
		// perTanggal.addEventListener("onChange", new EventListener() {
		//
		// @Override
		// public void onEvent(Event arg0) throws Exception {
		// onCetakStatusPasien(arg0);
		//
		// }
		// });
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {
			Lokasi lokasi = (Lokasi) (this.lokasi.getSelectedItem() == null ? null
					: this.lokasi.getSelectedItem().getValue());
			// Date perTgl = perTanggal.getValue();

			Map parameters = new HashMap();
			parameters.put("lokasi", lokasi == null || lokasi.getId() == null ? -1L : lokasi.getId());
			// parameters.put("tanggal", perTgl == null ? new Date() : perTgl);

			File file = Report.generateFileReportWithProgress("sirs/laporan_kadaluarsa",
					Report.XLS, parameters, "sirs/laporan_kadaluarsa", new Date(),
					Sessions.getCurrent().getWebApp());
			CommonReport.tampilkanReportXLS(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/inventory/LaporanKadaluarsaWindow.java:141");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas Excel Laporan Kadaluarsa Window", "Sistem mengalami kendala teknis saat menyusun berkas Excel laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap atau format datanya tidak sesuai dengan yang diharapkan oleh template ekspor.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mengekspor laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba ekspor ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
