package ais.action.report.format1.penelitiandanpengabdian;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan artikel. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Textbox userid}, {@code Textbox judul},
 * {@code Center center}, {@code Toolbar toolbar}, {@code String username}, {@code Combobox fakultas}, {@code
 * Combobox jurusan}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onReport()}); operasi
 * domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanArtikel extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Textbox userid;
	private Textbox judul;

	private Center center;
	private Toolbar toolbar;

	private String username;

	private Combobox fakultas;

	private Combobox jurusan;

	public LaporanArtikel() {
		super();
		this.username = null;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Artikel", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanArtikel(String username) {
		super();
		this.username = username;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Artikel", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		column.setWidth("50%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initFakultasDanJurusan(fakultas = new Combobox(), jurusan = new Combobox(), null, null);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.setReadonly(true);
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.setReadonly(true);
		jurusan.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("User ID/Nama"));
		row.appendChild(userid = new Textbox(username));
		userid.setWidth("90%");

		if (username != null && !username.trim().isEmpty()) {
			userid.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul penelitian"));
		row.appendChild(judul = new Textbox());
		judul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		MyButtonConfig tombol;
		row.appendChild(tombol = new MyButtonConfig("Lihat Laporan"));
		tombol.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "penelitiandanpengabdian/Rekap_Artikel", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("userid", userid.getValue().trim());
		parameters.put("judul", judul.getValue().trim());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("jurusan_nama", jurusan == null ? "" : jurusan.getNama());
		parameters.put("fakultas_nama", fakultas == null ? "" : fakultas.getNama());

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				try {

					File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
							"penelitiandanpengabdian/Rekap_Artikel", ais.ui.util.WaktuUtil.getDate(), toolbar);
					CommonReport.tampilkanReportPDF(center, file);

				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Artikel", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
							new String[] {
								"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
								"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
								"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
							});
				}
			}
		});

	}

}
