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
import ais.database.model.akunting.PertangungjawabanKasBesar;
import ais.database.model.akunting.KasBesar;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan pertangungjawaban kas besar. Kelas ini mengubah data
 * domain menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan
 * aturan transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Toolbar
 * toolbar}, {@code PertangungjawabanKasBesar pertangungjawabanKasBesar}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onReport()}, {@code cetakPdf()}); operasi domain lain ({@code
 * generateParameter()}, {@code parameter()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanPertangungjawabanKasBesar extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private PertangungjawabanKasBesar pertangungjawabanKasBesar;

	public LaporanPertangungjawabanKasBesar(PertangungjawabanKasBesar pertangungjawabanKasBesar) {
		super();
		this.pertangungjawabanKasBesar = pertangungjawabanKasBesar;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pertangungjawaban Kas Besar", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/pertangungjawabanKasBesar", null, new EventListener() {

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
		return parameter(pertangungjawabanKasBesar);
	}

	/**
	 * Parameter laporan, TANPA menyentuh komponen ZK.
	 *
	 * <p>Konstruktor kelas ini membangun Borderlayout/Center/Toolbar, jadi ia tidak
	 * dapat dipakai di luar konteks halaman ZK. Isi penyusun parameternya sendiri
	 * hanya bergantung pada entitasnya, sehingga dipisahkan ke sini supaya jalur API
	 * (keuangan_cetak) dapat memakainya juga.</p>
	 */
	public static Map parameter(PertangungjawabanKasBesar pertangungjawabanKasBesar) throws Exception {

		if (pertangungjawabanKasBesar != null && pertangungjawabanKasBesar.getId() != null) {
			HibernateUtil.currentSession().refresh(pertangungjawabanKasBesar);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(pertangungjawabanKasBesar.getDisposisiSop(), parameters);

		parameters.put("terbilang_kas_besar", IndonesianNumberToWords
				.convert((long) Math.abs(pertangungjawabanKasBesar.getKasBesar().getNilai())).toUpperCase());
		parameters.put("jumlah_kas_besar", pertangungjawabanKasBesar.getKasBesar().getNilai());

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(pertangungjawabanKasBesar.getNilai())).toUpperCase());
		parameters.put("jumlah", pertangungjawabanKasBesar.getNilai());

		parameters.put("diajukan", (pertangungjawabanKasBesar.getDibuatOleh() == null ? ""
				: pertangungjawabanKasBesar.getDibuatOleh().getUserNama()));
		parameters.put("disetujui", (pertangungjawabanKasBesar.getDisetujuiOleh() == null ? ""
				: pertangungjawabanKasBesar.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (pertangungjawabanKasBesar.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawabanKasBesar.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (pertangungjawabanKasBesar.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(pertangungjawabanKasBesar.getTanggalPersetujuan())));

		parameters.put("status", pertangungjawabanKasBesar.getStatus());
		parameters.put("judul", pertangungjawabanKasBesar.getNama());
		parameters.put("kode", pertangungjawabanKasBesar.getKode());
		Common.insertProperty(PertangungjawabanKasBesar.class, pertangungjawabanKasBesar, parameters,
				"pertangungjawabanKasBesar");

		if (pertangungjawabanKasBesar.getKasBesar() != null) {
			Common.insertProperty(KasBesar.class, pertangungjawabanKasBesar.getKasBesar(), parameters, "kasBesar");
		}

		boolean pph_mengurangi_lpj = Common.bolehKonfigurasi("pph_mengurangi_lpj");
		Double totalLpj = 0.0;
		List<Map> maps = new ArrayList<Map>();
		JSONArray array = new JSONArray(pertangungjawabanKasBesar.getFormula());
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
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akunting/LaporanPertangungjawabanKasBesar.java:192");
					// TODO: handle exception
				}

				Double pajak_nilai = jenisPajakBarang == null ? 0.0 : ((jenisPajakBarang.getPersen() / 100.0) * jumlah);

				Double tot = (jumlah + ((ppn / 100.0) * jumlah)) - (pph_mengurangi_lpj ? pajak_nilai : 0.0);

				Map map = new java.util.HashMap();
				map.put("pajak", jenisPajakBarang == null ? "" : jenisPajakBarang.getNama());
				map.put("persen_pajak", jenisPajakBarang == null ? 0.0 : jenisPajakBarang.getPersen());
				map.put("nama", nama);
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

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/pertangungjawabanKasBesar",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pertangungjawaban Kas Besar", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
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
	public static java.io.File cetakPdf(PertangungjawabanKasBesar pertangungjawabanKasBesar) throws Exception {
		return Report.generateFileReport(Report.PDF, parameter(pertangungjawabanKasBesar), "akunting/pertangungjawabanKasBesar",
				ais.ui.util.WaktuUtil.getDate(), null, new org.zkoss.zul.Toolbar());
	}

}
