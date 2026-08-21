package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.criterion.Restrictions;
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
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;

public class LaporanUangMuka extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private UangMuka uangMuka;

	public LaporanUangMuka(UangMuka uangMuka) {
		super();
		this.uangMuka = uangMuka;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Uang Muka", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/uangMuka", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Map parameter(UangMuka uangMuka) throws Exception {

		if (uangMuka != null && uangMuka.getId() != null) {
			HibernateUtil.currentSession().refresh(uangMuka);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		DisposisiAlurSop.parameterMap(uangMuka.getDisposisiSop(), parameters);
		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(uangMuka.getNilai())).toUpperCase());
		parameters.put("jumlah", uangMuka.getNilai());

		if (uangMuka.getAmbilDariPr()) {

			Set<Workspace> workspaces = new HashSet<Workspace>();
			List<PermintaanPengadaanMasterAssetDetail> dataPermintaanPengadaanMasterAssetDetail = new ArrayList<PermintaanPengadaanMasterAssetDetail>();
			if (!uangMuka.getPermintaanPengadaanMasterAssets().isEmpty()) {

				List<Long> data = new ArrayList<Long>();
				for (String s : uangMuka.getPermintaanPengadaanMasterAssets().split(",")) {
					try {
						data.add(Long.parseLong(s.trim()));
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanUangMuka.java:109");
						// TODO: handle exception
					}
				}
				dataPermintaanPengadaanMasterAssetDetail = data.isEmpty()
						? new ArrayList<PermintaanPengadaanMasterAssetDetail>()
						: HibernateUtil.currentSession().createCriteria(PermintaanPengadaanMasterAssetDetail.class)
								.add(Restrictions.in("id", data)).list();

				if (dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {
					dataPermintaanPengadaanMasterAssetDetail = HibernateUtil.currentSession()
							.createCriteria(PermintaanPengadaanMasterAssetDetail.class)
							.add(Restrictions.eq("uangMuka", uangMuka)).list();
				}

				if (!dataPermintaanPengadaanMasterAssetDetail.isEmpty()) {

					for (PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail : dataPermintaanPengadaanMasterAssetDetail) {
						if (permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
								.getWorkspace() != null) {
							workspaces.add(permintaanPengadaanMasterAssetDetail.getPermintaanPengadaanMasterAsset()
									.getWorkspace());
						}
					}

				}
			}

			String akun = "";
			String unit = "";
			Set<String> satker = new HashSet<String>();
			Integer tahun = 0;
			for (Workspace workspace : workspaces) {
				if (workspace.getSumberDana() != null) {
					tahun = workspace.getSumberDana().getTahun();
				}
				String a = workspace.getAkun() == null ? ""
						: workspace.getAkun().getKode() + "-" + workspace.getAkun().getNama();
				if (!a.isEmpty()) {
					akun += akun.isEmpty() ? a : ", " + a;
				}

				if (workspace.getSatuanKerja() != null) {
					satker.add(workspace.getSatuanKerja().getKode() + "-" + workspace.getSatuanKerja().getNama());
				}

			}

			for (String a : satker) {
				if (!a.isEmpty()) {
					unit += unit.isEmpty() ? a : ", " + a;
				}
			}
			parameters.put("tahun_anggaran", tahun);
			parameters.put("akun", akun);
			parameters.put("unit", unit);

			Map<Long, PermintaanPengadaanMasterAsset> lists = new HashMap<Long, PermintaanPengadaanMasterAsset>();
			for (PermintaanPengadaanMasterAssetDetail assetDetail : dataPermintaanPengadaanMasterAssetDetail) {
				lists.put(assetDetail.getPermintaanPengadaanMasterAsset().getId(),
						assetDetail.getPermintaanPengadaanMasterAsset());
			}

			Double saldo = 0.0;
			for (PermintaanPengadaanMasterAsset permintaanPengadaanMasterAsset : lists.values()) {
				saldo += permintaanPengadaanMasterAsset.getSaldo();
			}
			parameters.put("saldo", saldo);
			parameters.put("saldo_terbilang", IndonesianNumberToWords.convert((long) Math.abs(saldo)).toUpperCase());
		} else {
			parameters.put("akun", uangMuka.getWorkspace() == null || uangMuka.getWorkspace().getAkun() == null ? ""
					: uangMuka.getWorkspace().getAkun().getKode() + "-" + uangMuka.getWorkspace().getAkun().getNama());
			parameters.put("unit",
					uangMuka.getWorkspace() == null || uangMuka.getWorkspace().getSatuanKerja() == null ? ""
							: uangMuka.getWorkspace().getSatuanKerja().getNama());

			parameters.put("saldo", uangMuka.getSaldo());
			parameters.put("saldo_terbilang",
					IndonesianNumberToWords.convert((long) Math.abs(uangMuka.getSaldo())).toUpperCase());

			if (uangMuka.getWorkspace() != null && uangMuka.getWorkspace().getSumberDana() != null) {
				parameters.put("tahun_anggaran", uangMuka.getWorkspace().getSumberDana().getTahun());
			}
		}

		parameters.put("caraBayar",
				uangMuka.getJenisUangMuka() == null || uangMuka.getJenisUangMuka().getAkun() == null ? ""
						: uangMuka.getJenisUangMuka().getAkun().getNama());

		parameters.put("tanggal", (uangMuka.getMulai() == null ? "" : Common.dateFormat1.get().format(uangMuka.getMulai())));

		parameters.put("diajukan", (uangMuka.getDibuatOleh() == null ? "" : uangMuka.getDibuatOleh().getUserNama()));
		parameters.put("disetujui",
				(uangMuka.getDisetujuiOleh() == null ? "" : uangMuka.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (uangMuka.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(uangMuka.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (uangMuka.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(uangMuka.getTanggalPersetujuan())));

		parameters.put("status", uangMuka.getStatus());
		parameters.put("judul", uangMuka.getNama());
		parameters.put("kode", uangMuka.getKode());
		Common.insertProperty(UangMuka.class, uangMuka, parameters, "uangMuka");

		if (uangMuka.getWorkspace() != null && uangMuka.getWorkspace().getSumberDana() != null) {
			Common.insertProperty(UangMuka.class, uangMuka.getWorkspace().getSumberDana(), parameters, "sumberDana");
		}

		return parameters;
	}

	/**
	 * Delegasi ke {@link #parameter(UangMuka)} -- dipakai tombol ekspor pada layar ZK.
	 * Isinya dipindah ke method statis supaya kanal lain (API Desktop/Android) dapat
	 * mencetak dokumen yang SAMA tanpa perlu membuka layar ZK-nya.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map generateParameter() throws Exception {
		return parameter(uangMuka);
	}

	/**
	 * Cetak PDF tanpa layar ZK: templat dan parameternya sama persis dengan tombol cetak
	 * pada layar ZK, sehingga lembar cetak dari Desktop/Android identik.
	 */
	public static File cetakPdf(UangMuka uangMuka) throws Exception {
		return Report.generateFileReport(Report.PDF, parameter(uangMuka), "akunting/uangMuka",
				ais.ui.util.WaktuUtil.getDate(), null, new org.zkoss.zul.Toolbar());
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/uangMuka",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Uang Muka", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
