package ais.action.report.format1.library;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.master.library.helper.AmbilDataAnggotaBanbox;
import ais.action.master.library.helper.AmbilDataPerpustakaanBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.library.Anggota;
import ais.database.model.library.Perpustakaan;
import ais.ui.util.MyDatebox;

/**
 * Penyusun/penyaji laporan untuk laporan pengembalian per anggota. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code AmbilDataPerpustakaanBanbox
 * perpustakaan}, {@code AmbilDataAnggotaBanbox anggota}, {@code MyDatebox mulai}, {@code MyDatebox sampai},
 * {@code Center center}, {@code Toolbar toolbar}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor
 * ({@code onReport()}); operasi domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPengembalianPerAnggota extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private AmbilDataPerpustakaanBanbox perpustakaan;
	private AmbilDataAnggotaBanbox anggota;
	private MyDatebox mulai;
	private MyDatebox sampai;

	private Center center;
	private Toolbar toolbar;

	public LaporanPengembalianPerAnggota() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pengembalian Per Anggota", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

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
		row.appendChild(new ais.ui.util.MyLabelConfig("Perpustakaan"));
		row.appendChild(perpustakaan = new AmbilDataPerpustakaanBanbox());
		perpustakaan.setWidth("90%");
		perpustakaan.setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Anggota"));
		row.appendChild(anggota = new AmbilDataAnggotaBanbox());
		anggota.setWidth("90%");
		anggota.setEventListener(eventListener);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(calendar.getTime()));
		mulai.setWidth("90%");
		mulai.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate()));
		sampai.setWidth("90%");
		sampai.addEventListener("onChange", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {

						if (perpustakaan.getAttribute("perpustakaan") == null) {
							return null;
						}
						Map parameters = generateParameter();
						return parameters;
					}
				}, "library/pengembalian_per_anggota_pengadaan_semua", null,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onReport(arg0);
					}
				}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (perpustakaan.getAttribute("perpustakaan") == null) {
			return null;
		}

		Date mulai = this.mulai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : this.mulai
				.getValue();
		Date sampai = this.sampai.getValue() == null ? ais.ui.util.WaktuUtil.getDate() : this.sampai
				.getValue();

		Anggota anggota = (Anggota) this.anggota.getAttribute("anggota");

		final Perpustakaan perpustakaan = (Perpustakaan) this.perpustakaan
				.getAttribute("perpustakaan");
		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("anggota", anggota == null || anggota.getId() == null ? -1L : anggota.getId());
		parameters.put("perpustakaan", perpustakaan.getId());
		parameters.put("mulai", Common.databaseDateFormat.get().format(mulai));
		parameters.put("sampai", Common.databaseDateFormat.get().format(sampai));
		
		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(),
					"library/pengembalian_per_anggota_pengadaan_semua",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pengembalian Per Anggota", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
