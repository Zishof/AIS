package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
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
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.KasKecil;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanKasKecil extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private KasKecil kasKecil;

	public LaporanKasKecil(KasKecil kasKecil) {
		super();
		this.kasKecil = kasKecil;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kas Kecil", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/kasKecil", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map generateParameter() throws Exception {

		if (kasKecil != null && kasKecil.getId() != null) {
			HibernateUtil.currentSession().refresh(kasKecil);
		}

		Double saldo = kasKecil.getSaldo();

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(kasKecil.getDisposisiSop(), parameters);

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(kasKecil.getNilai())).toUpperCase());
		parameters.put("jumlah", kasKecil.getNilai());
		parameters.put("akun",
				kasKecil.getJenisKasKecil() == null || kasKecil.getJenisKasKecil().getAkun() == null ? ""
						: kasKecil.getJenisKasKecil().getAkun().getKode() + " "
								+ kasKecil.getJenisKasKecil().getAkun().getNama());
		parameters.put("unit", kasKecil.getSatuanKerja() == null ? "" : kasKecil.getSatuanKerja().getNama());

		parameters.put("saldo", saldo);
		parameters.put("terbilang_saldo", IndonesianNumberToWords.convert((long) Math.abs(saldo)));

		parameters.put("tanggal",
				(kasKecil.getTanggal() == null ? "" : Common.dateFormat1.get().format(kasKecil.getTanggal())));

		parameters.put("diajukan", (kasKecil.getDibuatOleh() == null ? "" : kasKecil.getDibuatOleh().getUserNama()));
		parameters.put("disetujui",
				(kasKecil.getDisetujuiOleh() == null ? "" : kasKecil.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (kasKecil.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(kasKecil.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (kasKecil.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(kasKecil.getTanggalPersetujuan())));

		parameters.put("status", kasKecil.getStatus());
		parameters.put("judul", kasKecil.getNama());
		parameters.put("kode", kasKecil.getKode());

		Common.insertProperty(KasKecil.class, kasKecil, parameters, "kasKecil");

		List<Map> maps = new ArrayList<Map>();
		JSONArray array = new JSONArray(kasKecil.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);

			if (!jsonObject.isNull("key")) {

				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject,"akun")));

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Date tanggal = WaktuUtil.getDate();
				if (!jsonObject.isNull("tanggal")) {
					tanggal = Common.dateFormat9.get().parse(jsonObject.getString("tanggal"));
				}

				Map map = new java.util.HashMap();
				map.put("tanggal_waktu_hari", Common.dateFormat5.get().format(tanggal));
				map.put("tanggal_waktu", Common.dateFormat9.get().format(tanggal));
				map.put("tanggal", Common.dateFormat1.get().format(tanggal));
				map.put("tanggal_hari", Common.dateFormat4.get().format(tanggal));
				map.put("nama", (akunBiaya == null ? "" : akunBiaya.toString() + ", ") + nama);
				map.put("keterangan", nama);

				map.put("qty", qty);
				map.put("harga", harga);
				map.put("jumlah", jumlah);
				maps.add(map);
			}
		}
		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/kasKecil",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kas Kecil", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
