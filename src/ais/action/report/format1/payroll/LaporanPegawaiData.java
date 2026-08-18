package ais.action.report.format1.payroll;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Toolbar;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Pegawai;
import ais.ui.util.MyWindow;

public class LaporanPegawaiData extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Center center;

	private Toolbar toolbar;

	private Criteria criteria;

	public LaporanPegawaiData(Criteria criteria) {
		super();
		try {
			this.criteria = criteria;
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pegawai Data", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "payroll/LaporanPegawaiData", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		onKHS(null);

	}

	private List<Pegawai> pegawais = null;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map generateParameter() throws Exception {

		Map parameters = ais.common.HashMapGenerator.getRand();

		if (pegawais == null) {
			pegawais = ConstantValues.simpleList(criteria, Pegawai.class);
		}
		List<Map> maps = new ArrayList<Map>();
		long now = System.currentTimeMillis();
		for (Pegawai pegawai : pegawais) {
			Map map = new HashMap();
			Common.insertProperty(Pegawai.class, pegawai, map, "", 2);

			if (pegawai.getTanggallahir() != null) {

				long lahir = pegawai.getTanggallahir().getTime();
				long selisih = now - lahir;
				final Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
				cal.setTimeInMillis(selisih);

				String pensiun = (cal.get(Calendar.YEAR) - 1970) + " tahun " + cal.get(Calendar.MONTH) + " bulan";
				map.put("usia", pensiun);

				map.put("usia_tahun", (cal.get(Calendar.YEAR) - 1970));
				map.put("usia_bulan", cal.get(Calendar.MONTH));
			} else {
				map.put("usia", "");
				map.put("usia_tahun", 0);
				map.put("usia_bulan", 0);
			}
			maps.add(map);
		}
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "payroll/LaporanPegawaiData",
					ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pegawai Data", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
