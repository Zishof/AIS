package ais.action.report.format1.rab;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan rkakl1. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Combobox tahun}, {@code
 * AmbilDataSatuanKerjaBanbox satuanKerja}, {@code AmbilDataWorkspaceBanbox workspace}, {@code Center center},
 * {@code Toolbar toolbar}; inisialisasi/lifecycle ({@code init()}); pelaporan/ekspor ({@code onReport()});
 * operasi domain lain ({@code generateParameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanRKAKL1 extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Combobox tahun;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataWorkspaceBanbox workspace;

	private Center center;
	private Toolbar toolbar;

	public LaporanRKAKL1() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan RKAKL1", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanRKAKL1(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Combobox());
		int year = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = year + 5; i > (year - 20); i--) {
			MyComboitemConfig comboitem = new MyComboitemConfig(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
			if (i == year) {
				tahun.setSelectedItem(comboitem);
			}
		}
		tahun.setWidth("90%");
		// tahun.// addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox());
		satuanKerja.setWidth("90%");
		// satuanKerja.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Root"));
		row.appendChild(workspace = new AmbilDataWorkspaceBanbox(true));
		workspace.setWidth("90%");
		// workspace.// setEventListener(eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		MyButtonConfig button = new MyButtonConfig("Tampilkan Laporan");
		button.setParent(row);
		button.addEventListener("onClick", eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {
				if (tahun.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu tahun", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				if (satuanKerja.getAttribute("satuanKerja") == null) {
					MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "rab/RKA-KL_1", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		if (tahun.getSelectedItem() == null) {
			return null;
		}
		if (satuanKerja.getAttribute("satuanKerja") == null) {
			return null;
		}

		Integer tahun1 = (Integer) this.tahun.getSelectedItem().getValue();
		final SatuanKerja satuanKerja = (SatuanKerja) this.satuanKerja.getAttribute("satuanKerja");
		final Workspace parent = (Workspace) workspace.getAttribute("workspace");

		SatuanKerjaTreeModel satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		Set<Long> satuanKerjas = new HashSet<Long>();
		satuanKerjas.add((satuanKerja == null || satuanKerja.getId() == null ? -1L : satuanKerja.getId()));
		satuanKerjaTreeModel.generateChildsByIds(satuanKerja.getId(), satuanKerjas);

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("parent", parent == null || parent.getId() == null ? -1L : parent.getId());
		parameters.put("satuanKerjas", satuanKerjas);
		parameters.put("tahun", tahun1);
		parameters.put("keterangan", "RKA-KL " + tahun1 + "\n" + satuanKerja.toString());

		return parameters;
	}

	@SuppressWarnings({ "unchecked" })
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "rab/RKA-KL_1",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan RKAKL1", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
