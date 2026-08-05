package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Session;
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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyWindow;

public class LaporanMonitorKknKbm extends MyWindow {

	private Center center;

	private KelompokKkn kelompokKkn;

	private Toolbar toolbar;

	private List<Long> kelompokKkns = null;

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	public LaporanMonitorKknKbm(KelompokKkn kelompokKkn) {
		super();
		this.kelompokKkn = kelompokKkn;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Monitor Kkn Kbm", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanMonitorKknKbm(List<Long> kelompokKkns) {
		super();
		this.kelompokKkns = kelompokKkns;
		try {

			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Monitor Kkn Kbm", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	private void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		if (kelompokKkns == null) {
			org.zkoss.zul.North north = new org.zkoss.zul.North();
			north.setParent(borderlayout);
			north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public Map<String, Serializable> generateParameters() throws Exception {
					Map parameters = generateParameter(kelompokKkn);
					return parameters;
				}
			}, "format1/lembar_monitoring_kelompokKkn_kbm", null, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onCetak(arg0);

				}
			}));
		}

		onCetak(null);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter(KelompokKkn kelompokKkn) throws Exception {
		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("kelompokKkn", kelompokKkn == null || kelompokKkn.getId() == null ? -1L : kelompokKkn.getId());

		String nama_dosen = "";
		String nidn_dosen = "";
		if (kelompokKkn != null) {
			
			Common.insertProperty(KelompokKkn.class, kelompokKkn, parameters, "");
			
			int d = 1;
			for (Dosen dosen : kelompokKkn.populateDosenBuNama()) {
				
				Common.insertProperty(Dosen.class, dosen, parameters, "dosen_" + d);

				
				nama_dosen += nama_dosen.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				nidn_dosen += nidn_dosen.isEmpty() ? dosen.getNidn() : ", " + dosen.getNidn();
				LampiranLain lam = LampiranLain.ambil(dosen.getId(), LampiranLain.TTD_DOSEN);
				String nama = lam == null ? null : lam.getNama();

				if (nama != null) {
					if (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
							|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
							|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp")) {
						String ttd = lam.ambilFile().getAbsolutePath();
						parameters.put("ttd_dosen_" + d, ttd);
						System.out.println("ttd_dosen_" + d + " => " + ttd);
					}
				}
				d++;
			}
			
			
		}
		parameters.put("nidn_dosen", nidn_dosen);
		parameters.put("nama_dosen", nama_dosen);

		ArrayList<Long> pertemuans = new ArrayList<Long>(kelompokKkn.ambilPertemuan().values());
		if (pertemuans.isEmpty()) {
			pertemuans.add(-1L);
		}
		parameters.put("pertemuans", pertemuans.toArray());

		return parameters;
	}

	@SuppressWarnings({})
	public void onCetak(Event event) {

		try {

			if (kelompokKkns != null) {

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						PDFMergerUtility ut = new PDFMergerUtility();
						Session session = HibernateUtil.currentSession();
						File fileD = null;
						for (Long id : kelompokKkns) {
							KelompokKkn kelompokKkn = (KelompokKkn) session.createCriteria(KelompokKkn.class)
									.add(Restrictions.idEq(id)).uniqueResult();
							fileD = Report.generateFileReportWithProgress(Report.PDF, generateParameter(kelompokKkn),
									"format1/lembar_monitoring_kelompokKkn_kbm", ais.ui.util.WaktuUtil.getDate(),
									toolbar);
							ut.addSource(fileD);
						}

						if (fileD != null) {
							File filePdfBaru = new File(fileD.getParentFile().getAbsolutePath() + "/"
									+ Common.getGeneratedBarCode() + ".pdf");
							ut.setDestinationStream(new FileOutputStream(filePdfBaru));
							ut.mergeDocuments();

							CommonReport.tampilkanReportPDF(center, filePdfBaru);
						}
					}
				});

			} else if (kelompokKkn != null) {

				File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(kelompokKkn),
						"format1/lembar_monitoring_kelompokKkn_kbm", ais.ui.util.WaktuUtil.getDate(), toolbar);
				CommonReport.tampilkanReportPDF(center, file);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Monitor Kkn Kbm", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

}
