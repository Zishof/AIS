package ais.action.report.format1.sirs.kasir;
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
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.West;
import org.zkoss.zul.Window;

import ais.action.master.sirs.helper.AmbilDataPasienBanbox;
import ais.action.master.sirs.helper.AmbilDataPendaftaranSemuaBanbox;
import ais.action.master.sirs.helper.AmbilDataTransaksiBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.TransaksiMedis;

/**
 * Penyusun/penyaji laporan untuk laporan informasi biaya dan retur window. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPendaftaranSemuaBanbox
 * pendaftaran}, {@code AmbilDataPasienBanbox pasien}, {@code AmbilDataTransaksiBanbox transaksi}, {@code Center
 * center}, {@code Pendaftaran myPendaftaran}, {@code Pasien myPasien}, {@code TransaksiMedis myTransaksi};
 * inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onCetakStatusPasien()}); operasi domain lain
 * ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see Window
 */
public class LaporanInformasiBiayaDanReturWindow extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPendaftaranSemuaBanbox pendaftaran;
	private AmbilDataPasienBanbox pasien;
	private AmbilDataTransaksiBanbox transaksi;

	private Center center;

	private Pendaftaran myPendaftaran;

	private Pasien myPasien;

	private TransaksiMedis myTransaksi;

	public LaporanInformasiBiayaDanReturWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanInformasiBiayaDanReturWindow.java:58");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Informasi Biaya Dan Retur Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanInformasiBiayaDanReturWindow(Pendaftaran myPendaftaran,
			Pasien myPasien, TransaksiMedis myTransaksi) {
		super();
		this.myPendaftaran = myPendaftaran;
		this.myPasien = myPasien;
		this.myTransaksi = myTransaksi;
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanInformasiBiayaDanReturWindow.java:71");
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Informasi Biaya Dan Retur Window", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, unit/ruangan, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
				new String[] {
					"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
					"Periksa kembali parameter/filter yang Bapak/Ibu pilih sebelum membuka layar ini.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

	public LaporanInformasiBiayaDanReturWindow(String title, String border,
			boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		West north = new West();
		north.setTitle("Menu");
		north.setCollapsible(true);
		north.setWidth("450px");
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
		column.setWidth("150px");
		column.setParent(columns);
		column = new Column();
		column.setParent(columns);

		// column = new Column();
		// column.setWidth("80px");
		// column.setParent(columns);
		// column = new Column();
		// column.setParent(columns);
		//
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("80px");
		// column.setParent(columns);
		// column = new Column();
		// column.setParent(columns);
		//
		// column.setParent(columns);
		// column = new Column();
		// column.setWidth("80px");
		// column.setParent(columns);
		// column = new Column();
		// column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		boolean disabled = myPendaftaran != null || myPasien != null
				|| myTransaksi != null;

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Pendaftaran")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranSemuaBanbox(true));
		pendaftaran.setWidth("90%");
		pendaftaran.setAttribute("pendaftaran", myPendaftaran);
		pendaftaran.setValue(myPendaftaran == null ? "" : myPendaftaran
				.getKode());
		pendaftaran.setDisabled(disabled);
		pendaftaran.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pendaftaran myPendaftaran = (Pendaftaran) pendaftaran
						.getAttribute("pendaftaran");
				if (myPendaftaran != null && myPendaftaran.getPasien() != null) {
					pasien.setAttribute("pasien", myPendaftaran.getPasien());
					pasien.setValue(myPendaftaran.getPasien().getKode() + "-"
							+ myPendaftaran.getPasien().getNama());
				}

				onCetakStatusPasien(arg0);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("MR. Pasien")));
		row.appendChild(pasien = new AmbilDataPasienBanbox());
		pasien.setWidth("90%");
		pasien.setAttribute("pasien", myPasien);
		pasien.setValue(myPasien == null ? "" : myPasien.getKode() + " - "
				+ myPasien.getNama());
		pasien.setDisabled(disabled);
		pasien.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Pasien pasien = (Pasien) LaporanInformasiBiayaDanReturWindow.this.pasien
						.getAttribute("pasien");
				if (pasien != null) {
					pendaftaran.setAttribute("pendaftaran", null);
					pendaftaran.setValue("");
				}
				onCetakStatusPasien(arg0);
			}
		});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Transaksi")));
		row.appendChild(transaksi = new AmbilDataTransaksiBanbox(false, null,
				null, null, true, null));
		transaksi.setWidth("90%");
		transaksi.setAttribute("transaksi", myTransaksi);
		transaksi.setValue(myTransaksi == null ? "" : myTransaksi.getKode());
		transaksi.setDisabled(disabled);
		transaksi.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				TransaksiMedis myTransaksi = (TransaksiMedis) transaksi
						.getAttribute("transaksi");
				if (myTransaksi != null && myTransaksi.getPasien() != null) {
					pasien.setAttribute("pasien", myTransaksi.getPasien());
					pasien.setValue(myTransaksi.getPasien().getKode() + "-"
							+ myTransaksi.getPasien().getNama());
				}
				onCetakStatusPasien(arg0);
			}
		});

		South south = new South();
		south.setParent(borderlayout);
		south.appendChild(CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters()
					throws Exception {
				Map parameters = generateParameter();
				return parameters;
			}
		}, "sirs/informasi_biaya_dan_retur"));
		onCetakStatusPasien(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		Pendaftaran pendaftaran = (Pendaftaran) this.pendaftaran
				.getAttribute("pendaftaran");
		Pasien pasien = (Pasien) this.pasien.getAttribute("pasien");
		TransaksiMedis transaksi = (TransaksiMedis) this.transaksi
				.getAttribute("transaksi");

		Map parameters = new HashMap();
		parameters.put("nama_pasien", pasien == null ? transaksi.getNama()
				: pasien.getNama());
		parameters.put("mr", pasien == null ? "" : pasien.getKode());
		parameters.put("pendaftaran",
				pendaftaran == null || pendaftaran.getId() == null ? -1L : pendaftaran.getId());
		parameters.put("pasien", pasien == null || pasien.getId() == null ? -1L : pasien.getId());
		parameters
				.put("transaksi", transaksi == null || transaksi.getId() == null ? -1L : transaksi.getId());

		return parameters;
	}

	@SuppressWarnings({ "rawtypes" })
	public void onCetakStatusPasien(Event event) {

		try {
			Map parameters = generateParameter();
			System.out.println(parameters);
			File file = Report.generateFileReportWithProgress("sirs/informasi_biaya_dan_retur",
					Report.PDF, parameters, "sirs/informasi_biaya_dan_retur",
					new Date());
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/sirs/kasir/LaporanInformasiBiayaDanReturWindow.java:262");
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Informasi Biaya Dan Retur Window", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
				new String[] {
					"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
					"Pastikan data yang menjadi sumber laporan ini sudah lengkap dan benar, kemudian coba cetak ulang.",
					"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
				});
		}
	}

}
