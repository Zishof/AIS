package ais.action.report.kpi;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.master.kpi.helper.ItemKpiTreeModel;
import ais.action.master.kpi.helper.KpiUtil;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.ParameterTambahan;
import ais.database.model.kpi.FormatKpiDetail;
import ais.database.model.kpi.ItemKpi;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * Penyusun/penyaji laporan untuk laporan item kpi. Kelas ini mengubah data domain menjadi bentuk
 * laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan transaksi ke lapisan
 * report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Toolbar
 * toolbar}, {@code FormatKpiDetail formatKpiDetail}, {@code ItemKpiTreeModel itemKpiTreeModel};
 * inisialisasi/lifecycle ({@code init()}); pembacaan/pencarian ({@code getStrings()}); pelaporan/ekspor ({@code
 * onReport()}); operasi domain lain ({@code generateParameter()}, {@code generateParameter()}); konfigurasi
 * constructor: {@code itemKpiTreeModel}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanItemKpi extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private FormatKpiDetail formatKpiDetail;
	private ItemKpiTreeModel itemKpiTreeModel;

	public LaporanItemKpi(FormatKpiDetail formatKpiDetail) {
		super();
		try {
			this.formatKpiDetail = formatKpiDetail;
			itemKpiTreeModel = new ItemKpiTreeModel(true, formatKpiDetail);
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Item Kpi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

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
		}, "kpi/Target", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings("rawtypes")
	private Map generateParameter() throws Exception {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		return generateParameter(null, maps);
	}

	private String getStrings(Integer deep) {
		String d = "";
		for (int i = 0; i < deep; i++) {
			d += "   ";
		}
		return d;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(ItemKpi parent, List<Map<String, Object>> maps) throws Exception {
		Date sekarang = WaktuUtil.getDate();

		List<ItemKpi> workspaces = itemKpiTreeModel.getChildren(parent);

		for (ItemKpi itemKpi : workspaces) {
			ParameterTambahan parameterTambahan = itemKpi.getKpi().getSatuanKpi() == null ? null
					: itemKpi.getKpi().getSatuanKpi().getParameterTambahan();
			List<Long> longs = new ArrayList<Long>();

			itemKpiTreeModel.getParentCount(itemKpi, longs);

			Integer deep = longs.size();

			longs = null;

			Map<String, Object> map = new java.util.HashMap<String, Object>();
			map.put("workspace_id", itemKpi.getId());
			map.put("unique_id", itemKpi.getId());
			map.put("kode", itemKpi.getKode() == null ? "" : itemKpi.getKode());
			map.put("nama", getStrings(deep) + (itemKpi.getNama() == null ? "" : itemKpi.getNama()));

			String target = KpiUtil.ambilTarget(itemKpi.getFormula(), sekarang);
			Double hasil = itemKpiTreeModel.hitungItemKpi(itemKpi, target, false, null);
			Double point = KpiUtil.ambilPoint(itemKpi.getKpi().getFormula(), sekarang, false);
			map.put("target",
					parameterTambahan == null ? ""
							: (itemKpi.getValtampil() + " " + (itemKpi.getKpi().getSatuanKpi() == null ? ""
									: itemKpi.getKpi().getSatuanKpi().getKode())));
			map.put("hitungan", hasil);
			map.put("poin", parameterTambahan == null ? null : point);

			maps.add(map);

			if (!itemKpiTreeModel.isLeaf(itemKpi)) {
				generateParameter(itemKpi, maps);
			}
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("target", formatKpiDetail.getNama());
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "kpi/Target",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Item Kpi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
