package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;

public class LaporanDanaTalangan extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private DanaTalangan danaTalangan;

	public LaporanDanaTalangan(DanaTalangan danaTalangan) {
		super();
		this.danaTalangan = danaTalangan;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Dana Talangan", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/danaTalangan", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	/** Dipakai tombol ekspor pada layar ZK; isinya di {@link #parameter}. */
	public Map generateParameter() throws Exception {
		return parameter(danaTalangan);
	}

	/**
	 * Parameter laporan, TANPA menyentuh komponen ZK.
	 *
	 * <p>Konstruktor kelas ini membangun Borderlayout/Center/Toolbar, jadi ia tidak
	 * dapat dipakai di luar konteks halaman ZK. Isi penyusun parameternya sendiri
	 * hanya bergantung pada entitasnya, sehingga dipisahkan ke sini supaya jalur API
	 * (keuangan_cetak) dapat memakainya juga.</p>
	 */
	public static Map parameter(DanaTalangan danaTalangan) throws Exception {
		
		if (danaTalangan != null && danaTalangan.getId() != null) {
			HibernateUtil.currentSession().refresh(danaTalangan);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		
		DisposisiAlurSop.parameterMap(danaTalangan.getDisposisiSop(), parameters);

		parameters.put("terbilang_uang_muka",
				IndonesianNumberToWords.convert((long) Math.abs(danaTalangan.getUangMuka().getNilai())).toUpperCase());
		parameters.put("jumlah_uang_muka", danaTalangan.getUangMuka().getNilai());

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(danaTalangan.getNilai())).toUpperCase());
		parameters.put("jumlah", danaTalangan.getNilai());
		parameters.put("akun",
				danaTalangan.getUangMuka().getWorkspace() == null
						|| danaTalangan.getUangMuka().getWorkspace().getAkun() == null ? ""
								: danaTalangan.getUangMuka().getWorkspace().getAkun().getKode() + "-"
										+ danaTalangan.getUangMuka().getWorkspace().getAkun().getNama());
		parameters.put("unit",
				danaTalangan.getUangMuka().getWorkspace() == null
						|| danaTalangan.getUangMuka().getWorkspace().getSatuanKerja() == null ? ""
								: danaTalangan.getUangMuka().getWorkspace().getSatuanKerja().getNama());

		parameters.put("caraBayar",
				danaTalangan.getJenisUangMuka() == null ? "" : danaTalangan.getJenisUangMuka().getNama());

		parameters.put("saldo",
				danaTalangan.getUangMuka().getWorkspace() == null
						|| danaTalangan.getUangMuka().getWorkspace().getAkun() == null ? 0.0
								: danaTalangan.getUangMuka().getWorkspace().getHargaTotal());

		parameters.put("tanggal", (danaTalangan.getUangMuka().getMulai() == null ? ""
				: Common.dateFormat1.get().format(danaTalangan.getUangMuka().getMulai())));

		parameters.put("diajukan",
				(danaTalangan.getDibuatOleh() == null ? "" : danaTalangan.getDibuatOleh().getUserNama()));
		parameters.put("disetujui",
				(danaTalangan.getDisetujuiOleh() == null ? "" : danaTalangan.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (danaTalangan.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(danaTalangan.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (danaTalangan.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(danaTalangan.getTanggalPersetujuan())));

		parameters.put("status", danaTalangan.getStatus());
		parameters.put("judul", danaTalangan.getNama());
		parameters.put("kode", danaTalangan.getKode());
		Common.insertProperty(DanaTalangan.class, danaTalangan, parameters, "danaTalangan");

		if (danaTalangan.getUangMuka() != null) {
			Common.insertProperty(UangMuka.class, danaTalangan.getUangMuka(), parameters, "uangMuka");
		}
		if (danaTalangan.getUangMuka() != null && danaTalangan.getUangMuka().getWorkspace() != null
				&& danaTalangan.getUangMuka().getWorkspace().getSumberDana() != null) {
			Common.insertProperty(UangMuka.class, danaTalangan.getUangMuka().getWorkspace().getSumberDana(), parameters,
					"sumberDana");
		}

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/danaTalangan",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Dana Talangan", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

	/**
	 * Cetak PDF tanpa layar ZK: templat dan parameternya sama persis dengan tombol
	 * cetak pada layar ZK, sehingga lembar cetak dari Desktop/Android identik.
	 */
	public static java.io.File cetakPdf(DanaTalangan danaTalangan) throws Exception {
		return Report.generateFileReport(Report.PDF, parameter(danaTalangan), "akunting/danaTalangan",
				ais.ui.util.WaktuUtil.getDate(), null, new org.zkoss.zul.Toolbar());
	}

}
