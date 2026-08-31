package ais.action.report.format1.keuangan;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
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
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;
import ais.ui.util.MyWindow;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Jurusan;

/**
 * Penyusun/penyaji laporan untuk laporan rekapitulasi tunggakan mahasiswa detail. Kelas ini
 * mengubah data domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa
 * memindahkan aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahunAkademik}, {@code
 * Combobox jenisSemester}, {@code Combobox fakultas}, {@code Combobox jurusan}, {@code Center center}, {@code
 * Toolbar toolbar}; inisialisasi/lifecycle ({@code init()}); operasi domain lain ({@code generateParameter()},
 * {@code onRekap()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRekapitulasiTunggakanMahasiswaDetail extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1662498263126327093L;

	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Combobox fakultas;
	private Combobox jurusan;

	private Center center;
	private Toolbar toolbar;

	public LaporanRekapitulasiTunggakanMahasiswaDetail() {
		super();

		init();

	}

	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onRekap(event);

			}
		};

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();grid.setWidth("100%");
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

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		jenisSemester = Common.initJenisSemester(jenisSemester);
		row.appendChild(jenisSemester);jenisSemester.setWidth("90%");
		jenisSemester.setWidth("90%");
		jenisSemester.addEventListener("onChange", eventListener);

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);

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

						if (tahunAkademik.getValue() == null) {
							MyMessageboxConfig.show("Tahun akademik harus diisi",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return null;
						}

						if (jurusan.getValue() == null) {
							MyMessageboxConfig.show("Jurusan"
									+ " harus diisi", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}

						Map parameters = generateParameter();
						return parameters;
					}
				}, "RekapTunggakanMahasiswaDetail", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onRekap(arg0);
					}
				}));

		onRekap(null);

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map generateParameter() throws Exception {

		if (tahunAkademik.getValue() == null) {
			return null;
		}

		if (jurusan.getValue() == null) {
			return null;
		}

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null || this.jurusan.getSelectedItem().getValue()==null ? null
				: this.jurusan.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tahun_akademik", tahunAkademik.getSelectedItem()
				.getValue() == null ? "" : tahunAkademik.getSelectedItem()
				.getValue());
		parameters.put("jenis_semester", jenisSemester.getSelectedItem()
				.getValue() == null ? "" : jenisSemester.getSelectedItem()
				.getValue());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		return parameters;
	}

	@SuppressWarnings("unchecked")
	public void onRekap(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "RekapTunggakanMahasiswaDetail",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Rekapitulasi Tunggakan Mahasiswa Detail", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
