package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
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
import ais.database.model.akunting.KasBesar;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanKasBesar extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private KasBesar kasBesar;

	public LaporanKasBesar(KasBesar kasBesar) {
		super();
		this.kasBesar = kasBesar;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kas Besar", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/kasBesar", null, new EventListener() {

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
		return parameter(kasBesar);
	}

	/**
	 * Parameter laporan, TANPA menyentuh komponen ZK.
	 *
	 * <p>Konstruktor kelas ini membangun Borderlayout/Center/Toolbar, jadi ia tidak
	 * dapat dipakai di luar konteks halaman ZK. Isi penyusun parameternya sendiri
	 * hanya bergantung pada entitasnya, sehingga dipisahkan ke sini supaya jalur API
	 * (keuangan_cetak) dapat memakainya juga.</p>
	 */
	public static Map parameter(KasBesar kasBesar) throws Exception {

		if (kasBesar != null && kasBesar.getId() != null) {
			HibernateUtil.currentSession().refresh(kasBesar);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(kasBesar.getDisposisiSop(), parameters);

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(kasBesar.getNilai())).toUpperCase());
		parameters.put("jumlah", kasBesar.getNilai());
		parameters.put("akun",
				kasBesar.getJenisKasBesar() == null || kasBesar.getJenisKasBesar().getAkun() == null ? ""
						: kasBesar.getJenisKasBesar().getAkun().getKode() + " "
								+ kasBesar.getJenisKasBesar().getAkun().getNama());
		parameters.put("unit", kasBesar.getSatuanKerja() == null ? "" : kasBesar.getSatuanKerja().getNama());

		parameters.put("tanggal",
				(kasBesar.getTanggal() == null ? "" : Common.dateFormat1.get().format(kasBesar.getTanggal())));

		parameters.put("diajukan", (kasBesar.getDibuatOleh() == null ? "" : kasBesar.getDibuatOleh().getUserNama()));
		parameters.put("disetujui",
				(kasBesar.getDisetujuiOleh() == null ? "" : kasBesar.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (kasBesar.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(kasBesar.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (kasBesar.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(kasBesar.getTanggalPersetujuan())));

		parameters.put("status", kasBesar.getStatus());
		parameters.put("judul", kasBesar.getNama());
		parameters.put("kode", kasBesar.getKode());

		Common.insertProperty(KasBesar.class, kasBesar, parameters, "kasBesar");

		List<Map> maps = new ArrayList<Map>();
		JSONArray array = new JSONArray(kasBesar.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);
			if (!jsonObject.isNull("key")) {
				Workspace workspace = (Workspace) (jsonObject.isNull("workspace") ? null
						: ConstantValues.ambil(Workspace.class.getName(),
								new BigDecimal(jsonObject.get("workspace") + "").longValue()));
				System.out.println("workspace -> " + workspace);

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
				map.put("nama", (workspace == null ? "" : workspace.toString() + ", ") + nama);
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

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/kasBesar",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kas Besar", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
	public static java.io.File cetakPdf(KasBesar kasBesar) throws Exception {
		return Report.generateFileReport(Report.PDF, parameter(kasBesar), "akunting/kasBesar",
				ais.ui.util.WaktuUtil.getDate(), null, new org.zkoss.zul.Toolbar());
	}

}
