package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
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
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.spmi.ButirMutuSPMI;
import ais.database.model.spmi.HasilSPMI;
import ais.database.model.spmi.HasilTemuanSPMI;
import ais.database.model.spmi.IndikatorSPMI;
import ais.database.model.spmi.JenisSPMI;
import ais.database.model.spmi.SkenarioSPMI;
import ais.database.model.spmi.StandarSPMI;
import ais.ui.util.MyWindow;

public class LaporanHasilSPMI extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private HasilSPMI hasilSPMI;

	public LaporanHasilSPMI(HasilSPMI hasilSPMI) {
		super();
		this.hasilSPMI = hasilSPMI;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Hasil SPMI", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "format1/lembar_kerja_ami", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map generateParameter() throws Exception {

		if (hasilSPMI != null && hasilSPMI.getId() != null) {
			HibernateUtil.currentSession().refresh(hasilSPMI);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(hasilSPMI.getDisposisiSop(), parameters);

		Common.insertProperty(HasilSPMI.class, hasilSPMI, parameters, "hasilSPMI");

		// --- Header parameters for report cover ---
		parameters.put("namaPerguruanTinggi",
				hasilSPMI.getPerguruanTinggi() != null ? hasilSPMI.getPerguruanTinggi().getNama() : "");
		parameters.put("namaFakultas",
				hasilSPMI.getFakultas() != null ? hasilSPMI.getFakultas().getNama() : "");
		parameters.put("namaJurusan",
				hasilSPMI.getJurusan() != null ? hasilSPMI.getJurusan().getNama() : "");
		parameters.put("tanggalAudit",
				hasilSPMI.getTanggal() != null
						? Common.dateFormat6.get().format(hasilSPMI.getTanggal()) : "");
		parameters.put("ta",
				hasilSPMI.getTa() != null ? hasilSPMI.getTa() : "");
		parameters.put("semester",
				hasilSPMI.getSemester() != null ? hasilSPMI.getSemester() : "");
		parameters.put("auditorNama",
				hasilSPMI.getAuditorNama() != null ? hasilSPMI.getAuditorNama() : "");
		parameters.put("auditeeNama",
				hasilSPMI.getAuditeeNama() != null ? hasilSPMI.getAuditeeNama() : "");
		parameters.put("judulPengajuan",
				hasilSPMI.getNama() != null ? hasilSPMI.getNama() : "");
		parameters.put("jenisSPMI",
				hasilSPMI.getJenisSPMI() != null ? hasilSPMI.getJenisSPMI().getNama() : "");

		List<Map> maps = new ArrayList<Map>();
		Session session = HibernateUtil.currentSession();

		List<HasilTemuanSPMI> standarSPMIs = session.createCriteria(HasilTemuanSPMI.class)
				.add(Restrictions.eq("hasilSPMI", hasilSPMI))
				.createAlias("skenarioSPMI", "skenarioSPMI")
				.createAlias("skenarioSPMI.indikatorSPMI", "indikatorSPMI")
				.createAlias("indikatorSPMI.butirMutuSPMI", "butirMutuSPMI")
				.createAlias("butirMutuSPMI.standarSPMI", "standarSPMI")
				.addOrder(Order.asc("standarSPMI.nomorUrut"))
				.addOrder(Order.asc("butirMutuSPMI.nomorUrut"))
				.addOrder(Order.asc("indikatorSPMI.nomorUrut"))
				.addOrder(Order.asc("skenarioSPMI.nomorUrut"))
				.list();

		// --- Rekapitulasi counts ---
		int jumlahO = 0, jumlahKtsMyr = 0, jumlahKtsMnr = 0, jumlahS = 0, jumlahLs = 0;

		for (HasilTemuanSPMI hasilTemuanSPMI : standarSPMIs) {
			String st = hasilTemuanSPMI.getStatus();
			if (HasilTemuanSPMI.O1.equals(st))         jumlahO++;
			else if (HasilTemuanSPMI.KTS_MYR1.equals(st)) jumlahKtsMyr++;
			else if (HasilTemuanSPMI.KTS_MNR1.equals(st)) jumlahKtsMnr++;
			else if (HasilTemuanSPMI.S1.equals(st))    jumlahS++;
			else if (HasilTemuanSPMI.LS1.equals(st))   jumlahLs++;

			Map map = new java.util.HashMap();
			Common.insertProperty(HasilTemuanSPMI.class, hasilTemuanSPMI, map, "", 1, "skenarioSPMI", "hasilSPMI");

			Common.insertProperty(SkenarioSPMI.class, hasilTemuanSPMI.getSkenarioSPMI(), map, "skenario", 1,
					"indikatorSPMI");

			Common.insertProperty(IndikatorSPMI.class, hasilTemuanSPMI.getSkenarioSPMI().getIndikatorSPMI(), map,
					"indikator", 1, "butirMutuSPMI");

			Common.insertProperty(ButirMutuSPMI.class,
					hasilTemuanSPMI.getSkenarioSPMI().getIndikatorSPMI().getButirMutuSPMI(), map, "butir", 1,
					"standarSPMI");

			Common.insertProperty(StandarSPMI.class,
					hasilTemuanSPMI.getSkenarioSPMI().getIndikatorSPMI().getButirMutuSPMI().getStandarSPMI(), map,
					"standar", 1, "jenisSPMI");

			Common.insertProperty(JenisSPMI.class, hasilTemuanSPMI.getSkenarioSPMI().getIndikatorSPMI()
					.getButirMutuSPMI().getStandarSPMI().getJenisSPMI(), map, "jenis");

			maps.add(map);
		}

		parameters.put("jumlahO", jumlahO);
		parameters.put("jumlahKtsMyr", jumlahKtsMyr);
		parameters.put("jumlahKtsMnr", jumlahKtsMnr);
		parameters.put("jumlahS", jumlahS);
		parameters.put("jumlahLs", jumlahLs);
		parameters.put("jumlahTotal", jumlahO + jumlahKtsMyr + jumlahKtsMnr + jumlahS + jumlahLs);
		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "format1/lembar_kerja_ami",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Hasil SPMI", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
