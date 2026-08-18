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
import ais.database.model.Konfigurasi;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.rab.SumberDana;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;

public class LaporanPertangungjawaban extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private Pertangungjawaban pertangungjawaban;

	public LaporanPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		super();
		this.pertangungjawaban = pertangungjawaban;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pertangungjawaban", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/pertangungjawaban", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map generateParameter() throws Exception {

		if (pertangungjawaban != null && pertangungjawaban.getId() != null) {
			HibernateUtil.currentSession().refresh(pertangungjawaban);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(pertangungjawaban.getDisposisiSop(), parameters);

		parameters.put("terbilang_uang_muka", IndonesianNumberToWords
				.convert((long) Math.abs(pertangungjawaban.getUangMuka().getNilai())).toUpperCase());
		parameters.put("jumlah_uang_muka", pertangungjawaban.getUangMuka().getNilai());

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(pertangungjawaban.getNilai())).toUpperCase());
		parameters.put("jumlah", pertangungjawaban.getNilai());
		parameters.put("akun",
				pertangungjawaban.getUangMuka().getWorkspace() == null
						|| pertangungjawaban.getUangMuka().getWorkspace().getAkun() == null ? ""
								: pertangungjawaban.getUangMuka().getWorkspace().getAkun().getKode() + "-"
										+ pertangungjawaban.getUangMuka().getWorkspace().getAkun().getNama());
		parameters.put("unit",
				pertangungjawaban.getUangMuka().getWorkspace() == null
						|| pertangungjawaban.getUangMuka().getWorkspace().getSatuanKerja() == null ? ""
								: pertangungjawaban.getUangMuka().getWorkspace().getSatuanKerja().getNama());

		parameters.put("saldo",
				pertangungjawaban.getUangMuka().getWorkspace() == null
						|| pertangungjawaban.getUangMuka().getWorkspace().getAkun() == null ? 0.0
								: pertangungjawaban.getUangMuka().getWorkspace().getHargaTotal());

		parameters.put("tanggal", (pertangungjawaban.getUangMuka().getMulai() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawaban.getUangMuka().getMulai())));

		parameters.put("diajukan",
				(pertangungjawaban.getDibuatOleh() == null ? "" : pertangungjawaban.getDibuatOleh().getUserNama()));
		parameters.put("disetujui", (pertangungjawaban.getDisetujuiOleh() == null ? ""
				: pertangungjawaban.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (pertangungjawaban.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawaban.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (pertangungjawaban.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawaban.getTanggalPersetujuan())));

		parameters.put("status", pertangungjawaban.getStatus());
		parameters.put("judul", pertangungjawaban.getNama());
		parameters.put("kode", pertangungjawaban.getKode());
		Common.insertProperty(Pertangungjawaban.class, pertangungjawaban, parameters, "pertangungjawaban");

		if (pertangungjawaban.getUangMuka() != null) {
			Common.insertProperty(UangMuka.class, pertangungjawaban.getUangMuka(), parameters, "uangMuka");
		}
		if (pertangungjawaban.getUangMuka() != null && pertangungjawaban.getUangMuka().getWorkspace() != null
				&& pertangungjawaban.getUangMuka().getWorkspace().getSumberDana() != null) {
			// BUG FIX: sebelumnya dipassing UangMuka.class padahal objeknya SumberDana ->
			// ManajemenProperty.insertProperty membangun ClassMetadata utk UangMuka lalu
			// classMetadata.getIdentifier(generalValueObject,...) memanggil getter id
			// UangMuka via reflection pada instance SumberDana -> ClassCastException
			// (PropertyAccessException). clazz harus sesuai tipe objek sebenarnya.
			Common.insertProperty(SumberDana.class, pertangungjawaban.getUangMuka().getWorkspace().getSumberDana(),
					parameters, "sumberDana");
		}
		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
		Double totalLpj = 0.0;
		List<Map> maps = new ArrayList<Map>();
		JSONArray array = new JSONArray(pertangungjawaban.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);
			if (!jsonObject.isNull("key")) {
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

				Double ppn = 0.0;
				if (!jsonObject.isNull("ppn")) {
					ppn = jsonObject.getDouble("ppn");
				}

				JenisPajakBarang jenisPajakBarang;
				if (!jsonObject.isNull("pajak")) {
					jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(JenisPajakBarang.class.getName(),
							Long.parseLong(jsonObject.get("pajak") + ""));
				} else {
					jenisPajakBarang = null;
				}

				String ntpn = "";

				if (!jsonObject.isNull("ntpn")) {
					ntpn = jsonObject.get("ntpn") + "";
				}

				String npwp = "";

				if (!jsonObject.isNull("npwp")) {
					npwp = jsonObject.get("npwp") + "";
				}

				String namaWp = "";

				if (!jsonObject.isNull("namaWp")) {
					namaWp = jsonObject.get("namaWp") + "";
				}

				String tanggalStor = "";

				if (!jsonObject.isNull("tanggalStor")) {
					tanggalStor = jsonObject.get("tanggalStor") + "";
				}
				Date tglStor = null;
				try {
					tglStor = tanggalStor.isEmpty() ? null : Common.dateFormat1.get().parse(tanggalStor);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanPertangungjawaban.java:212");
					// TODO: handle exception
				}

				Double pajak_nilai = jenisPajakBarang == null ? 0.0 : ((jenisPajakBarang.getPersen() / 100.0) * jumlah);

				Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak_nilai : 0.0);

				String satuan = "";

				if (!jsonObject.isNull("satuan")) {
					satuan = jsonObject.get("satuan") + "";
				}

				Long satuanId = null;

				if (!jsonObject.isNull("satuanId")) {
					satuanId = Long.parseLong(jsonObject.get("satuanId") + "");
				}

				Map map = new java.util.HashMap();
				map.put("pajak", jenisPajakBarang == null ? "" : jenisPajakBarang.getNama());
				map.put("persen_pajak", jenisPajakBarang == null ? 0.0 : jenisPajakBarang.getPersen());
				map.put("nama", nama);

				map.put("satuanId", satuanId);
				map.put("satuan", satuan);

				map.put("pajak_nilai", pajak_nilai);
				map.put("qty", qty);
				map.put("ppn", ppn);
				map.put("harga", harga);
				map.put("jumlah", jumlah);
				map.put("total", tot);

				map.put("ntpn", ntpn);
				map.put("npwp", npwp);
				map.put("namaWp", namaWp);
				map.put("tanggalStor", tanggalStor);
				map.put("tglStor", tglStor);

				maps.add(map);
				totalLpj += tot;
			}
		}

		parameters.put("terbilang_lpj", IndonesianNumberToWords.convert((long) Math.abs(totalLpj)).toUpperCase());
		parameters.put("jumlah_lpj", totalLpj);

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/pertangungjawaban",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pertangungjawaban", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
