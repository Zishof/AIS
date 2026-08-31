package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan buku kas umum. Kelas ini mengubah data domain menjadi
 * bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke
 * lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahun}, {@code Combobox
 * bulan}, {@code Combobox bulan2}, {@code Center center}, {@code Toolbar toolbar}, {@code
 * AmbilDataSatuanKerjaBanbox searchsatuanKerja}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor
 * ({@code onReport()}); operasi domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanBukuKasUmum extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private Combobox bulan;
	private Combobox bulan2;

	private Center center;
	private Toolbar toolbar;

	private AmbilDataSatuanKerjaBanbox searchsatuanKerja;

	public LaporanBukuKasUmum() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Buku Kas Umum", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanBukuKasUmum(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
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
		west.setWidth("300px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("40%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		tahun.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan Mulai"));
		row.appendChild(bulan = new Combobox());

		int month = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH);
		int i = 1;
		for (String bln : Common.BULAN) {
			MyComboitemConfig comboitem2 = new MyComboitemConfig(bln);
			comboitem2.setValue(i);
			bulan.appendChild(comboitem2);
			i++;
		}
		bulan.setSelectedIndex(month);
		bulan.setWidth("90%");
		bulan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bulan Sampai"));
		row.appendChild(bulan2 = new Combobox());

		i = 1;
		for (String bln : Common.BULAN) {
			MyComboitemConfig comboitem2 = new MyComboitemConfig(bln);
			comboitem2.setValue(i);
			bulan2.appendChild(comboitem2);
			i++;
		}
		bulan2.setSelectedIndex(month);
		bulan2.setWidth("90%");
		bulan2.addEventListener("onChange", eventListener);
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(searchsatuanKerja = new AmbilDataSatuanKerjaBanbox());
		searchsatuanKerja.setWidth("90%"); 
		searchsatuanKerja.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		Button tampilkan;
		row.appendChild(tampilkan = new Button("Tampilkan"));
		tampilkan.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (bulan.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu bulan mulai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (bulan2.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu bulan sampai", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "akunting/jurnal_buku_kas_umum", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (bulan.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu bulan mulai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}
		if (bulan2.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu bulan sampai", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		if (tahun.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return null;
		}

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		Integer bulan1 = this.bulan.getSelectedIndex();
		Integer bulan2 = this.bulan2.getSelectedIndex();
		String namabulan = this.bulan.getSelectedItem().getValue().toString();
		String namabulan2 = this.bulan2.getSelectedItem().getValue().toString();

		final Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("bulan", (bulan1 + 1));
		parameters.put("bulan1", (bulan2 + 1));
		parameters.put("tahun1", tahun1);
		parameters.put("tahun", tahun1);
		parameters.put("namabulan", namabulan);
		parameters.put("namabulan1", namabulan2);
		SatuanKerja satuanKerja = (SatuanKerja) searchsatuanKerja.getAttribute("satuanKerja");
		parameters.put("satuan_kerja", satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId());
		return parameters;
	}

	@SuppressWarnings({ })
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/jurnal_buku_kas_umum",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Buku Kas Umum", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
