package ais.action.report.format1.koperasi;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import ais.database.model.koperasi.JenisTransaksiKoperasi;
import ais.database.model.koperasi.ProdukKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;
import ais.database.model.sop.DisposisiAlurSop;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

public class LaporanTransaksiKoperasi extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private TransaksiKoperasi transaksiKoperasi;

	public LaporanTransaksiKoperasi(TransaksiKoperasi transaksiKoperasi) {
		super();
		this.transaksiKoperasi = transaksiKoperasi;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transaksi Koperasi", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "koperasi/transaksiKoperasi", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map generateParameter() throws Exception {

		if (transaksiKoperasi != null && transaksiKoperasi.getId() != null) {
			HibernateUtil.currentSession().refresh(transaksiKoperasi);
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		DisposisiAlurSop.parameterMap(transaksiKoperasi.getDisposisiSop(), parameters);

		parameters.put("terbilang",
				IndonesianNumberToWords.convert((long) Math.abs(transaksiKoperasi.getNilai())).toUpperCase());
		parameters.put("jumlah", transaksiKoperasi.getNilai());

		parameters.put("unit",
				transaksiKoperasi.getSatuanKerja() == null ? "" : transaksiKoperasi.getSatuanKerja().getNama());

		parameters.put("tanggal", (transaksiKoperasi.getTanggal() == null ? ""
				: Common.dateFormat1.get().format(transaksiKoperasi.getTanggal())));

		parameters.put("diajukan",
				(transaksiKoperasi.getDibuatOleh() == null ? "" : transaksiKoperasi.getDibuatOleh().getUserNama()));
		parameters.put("disetujui", (transaksiKoperasi.getDisetujuiOleh() == null ? ""
				: transaksiKoperasi.getDisetujuiOleh().getUserNama()));
		parameters.put("tanggal_diajukan", (transaksiKoperasi.getTanggalPembuatan() == null ? ""
				: Common.dateFormat1.get().format(transaksiKoperasi.getTanggalPembuatan())));
		parameters.put("tanggal_disetujui", (transaksiKoperasi.getTanggalPersetujuan() == null ? ""
				: Common.dateFormat1.get().format(transaksiKoperasi.getTanggalPersetujuan())));

		parameters.put("status", transaksiKoperasi.getStatus());
		parameters.put("judul", transaksiKoperasi.getNama());
		parameters.put("kode", transaksiKoperasi.getKode());

		Common.insertProperty(TransaksiKoperasi.class, transaksiKoperasi, parameters, "transaksiKoperasi", 2);

		List<Map> maps = new ArrayList<Map>();
		JSONArray array = new JSONArray(transaksiKoperasi.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);

			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject,"key");
			}

			if (key != null) {

				JenisTransaksiKoperasi jenisTransaksiKoperasi = (JenisTransaksiKoperasi) (jsonObject
						.isNull("jenisTransaksiKoperasi") ? null
								: ConstantValues.ambil(JenisTransaksiKoperasi.class.getName(),
										ais.common.CommonJSONUtil.ambilLong(jsonObject,"jenisTransaksiKoperasi")));

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

				map.put("akun",
						jenisTransaksiKoperasi == null || jenisTransaksiKoperasi.getAkun() == null ? ""
								: jenisTransaksiKoperasi.getAkun().getKode() + " "
										+ jenisTransaksiKoperasi.getAkun().getNama());

				map.put("tanggal_waktu_hari", Common.dateFormat5.get().format(tanggal));
				map.put("tanggal_waktu", Common.dateFormat9.get().format(tanggal));
				map.put("tanggal", Common.dateFormat1.get().format(tanggal));
				map.put("tanggal_hari", Common.dateFormat4.get().format(tanggal));
				map.put("nama", (jenisTransaksiKoperasi == null ? ""
						: jenisTransaksiKoperasi.getNama() + (nama.trim().isEmpty() ? "" : ", ")) + nama);
				map.put("qty", qty);
				map.put("harga", harga);
				map.put("jumlah", jumlah);
				maps.add(map);
			}
		}
		parameters.put("maps", maps);

		List<Map> mapsAngsuran = new ArrayList<Map>();
		ProdukKoperasi work = transaksiKoperasi.getProdukKoperasi();
		if (work != null && work.getTipeProdukKoperasi() != null && ConstantValues.PINJAMAN != null
				&& work.getTipeProdukKoperasi().getId().equals(ConstantValues.PINJAMAN.getId())
				&& transaksiKoperasi.getTanggalMulaiDiangsur() != null) {

			Integer jumlahAngsur = transaksiKoperasi.getJumlahAngsur();

			if (jumlahAngsur != null) {
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(transaksiKoperasi.getTanggalMulaiDiangsur());
				Calendar s = ais.ui.util.WaktuUtil.getCalendar();
				s.setTime(transaksiKoperasi.getTanggalTerakhirDiangsur());
				s.set(Calendar.DATE, s.get(Calendar.DATE) - 1);

				int i = 1;
				Double sisa = transaksiKoperasi.getNilai();

				Double totalPokok = 0.0;
				Double totalMargin = 0.0;

				while (calendar.getTime().before(s.getTime())) {
					Date tanggal = calendar.getTime();
					Double pokok = transaksiKoperasi.getNilai() / jumlahAngsur.doubleValue();
					Double m = transaksiKoperasi.getMargin() / jumlahAngsur.doubleValue();

					totalPokok += pokok;
					totalMargin += m;

					sisa = sisa - (pokok - m);

					Map map = new java.util.HashMap();
					map.put("nama", "Angsuran ke-" + (i));
					map.put("tanggal_format", Common.dateFormat6.get().format(tanggal));
					map.put("tanggal", tanggal);
					map.put("pokok", pokok);
					map.put("margin", m);
					map.put("sisa", sisa);

					mapsAngsuran.add(map);

					if (work.getDurasi().equals(ProdukKoperasi.HARIAN)) {
						calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.MINGGUAN)) {
						calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.BULANAN)) {
						calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
					} else if (work.getDurasi().equals(ProdukKoperasi.TAHUNAN)) {
						calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
					}

					i++;
				}
			}

		}
		parameters.put("mapsAngsuran", mapsAngsuran);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "koperasi/transaksiKoperasi",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transaksi Koperasi", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
