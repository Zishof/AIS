package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.North;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Transaksi;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyWindow;

public class LaporanBuktiPengeluaranKasBanyak extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private Textbox kodeGrupTransaksis;

	private double jumlahDebet;

	public LaporanBuktiPengeluaranKasBanyak() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pengeluaran Kas Banyak", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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

		North north = new North();
		north.setParent(borderlayout);
		north.setHeight("70px");
		Hbox hbox = new Hbox();
		hbox.setParent(north);
		Vbox vbox = new Vbox();
		vbox.setParent(hbox);
		vbox.appendChild(new MyLabelBold(
				"Masukkan kode transaksi, jika lebih dari satu, pisahkan menggunakan tanda simikolon (;)"));
		vbox.appendChild(kodeGrupTransaksis = new Textbox());
		kodeGrupTransaksis.setRows(3);
		kodeGrupTransaksis.setCols(20);
		kodeGrupTransaksis.setWidth("90%");
		vbox.setWidth("90%");

		kodeGrupTransaksis.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(null);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		vbox.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Map parameters = generateParameter();
				return parameters;
			}
		}, "akunting/bukti_pengeluaran_kas", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

	}

	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	private Map generateParameter() throws Exception {

		String[] kodes = StringUtils.split(kodeGrupTransaksis.getValue().trim(), ";");

		Session session = HibernateUtil.currentSession();
		List<Transaksi> transaksis = kodes.length == 0 ? new ArrayList<Transaksi>()
				: session.createCriteria(Transaksi.class).addOrder(Order.desc("debet"))
						.addOrder(Order.asc("tanggalTransaksi")).createAlias("grupTransaksi", "grupTransaksi")
						.add(Restrictions.in("grupTransaksi.kode", kodes)).list();

		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		List<Long> kodeAkuns = new ArrayList<Long>();
		List<Long> kodeGrup = new ArrayList<Long>();
		String akun = "";
		String kepadas = "";
		String jenises = "";
		String nomors = "";
		Date tanggal = ais.ui.util.WaktuUtil.getDate();

		int debet = 0;
		int kredit = 0;
		for (Transaksi transaksi : transaksis) {
			if (transaksi.getDebet() > 0.1) {
				debet++;
			}
			if (transaksi.getKredit() > 0.1) {
				kredit++;
			}
		}

		jumlahDebet = 0.0;
		for (Transaksi transaksi : transaksis) {

			if (!kodeGrup.contains(transaksi.getGrupTransaksi().getId())) {
				if (!transaksi.getGrupTransaksi().getKepada().isEmpty()) {
					kepadas += kepadas.isEmpty() ? transaksi.getGrupTransaksi().getKepada()
							: ", " + transaksi.getGrupTransaksi().getKepada();
				}
				if (transaksi.getGrupTransaksi().getJenisTransaksi() != null) {
					jenises += jenises.isEmpty() ? transaksi.getGrupTransaksi().getJenisTransaksi().getKeterangan()
							: ", " + transaksi.getGrupTransaksi().getJenisTransaksi().getKeterangan();
				}
				if (!transaksi.getGrupTransaksi().getNomorTagihan().isEmpty()) {
					nomors += nomors.isEmpty() ? transaksi.getGrupTransaksi().getNomorTagihan()
							: ", " + transaksi.getGrupTransaksi().getNomorTagihan();
				}
				kodeGrup.add(transaksi.getGrupTransaksi().getId());
			}

			if (debet < kredit || debet == kredit) {
				if (transaksi.getDebet() > 0.1) {
					if (!kodeAkuns.contains(transaksi.getAkun().getId())) {
						akun += akun.isEmpty() ? transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama()
								: ", " + transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama();
						kodeAkuns.add(transaksi.getAkun().getId());
					}
					jumlahDebet += transaksi.getDebet();
				} else {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("akun", transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama());
					map.put("uraian", transaksi.getKeterangan());
					map.put("nominal", transaksi.getKredit() + transaksi.getDebet());
					maps.add(map);
				}
			} else {
				if (transaksi.getKredit() > 0.1) {
					if (!kodeAkuns.contains(transaksi.getAkun().getId())) {
						akun += akun.isEmpty() ? transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama()
								: ", " + transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama();
						kodeAkuns.add(transaksi.getAkun().getId());
					}
					jumlahDebet += transaksi.getKredit();
				} else {
					Map<String, Object> map = new java.util.HashMap<String, Object>();
					map.put("akun", transaksi.getAkun().getKode() + " " + transaksi.getAkun().getNama());
					map.put("uraian", transaksi.getKeterangan());
					map.put("nominal", transaksi.getKredit() + transaksi.getDebet());
					maps.add(map);
				}
			}
		}

		if (kodeGrup.isEmpty()) {
			return null;
		}

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", Common.dateFormat4.get().format(tanggal));
		parameters.put("nomor", org.apache.commons.lang3.StringUtils.replace(kodeGrupTransaksis.getValue().trim(), ";", ", "));
		parameters.put("akun", akun);
		parameters.put("atas_nama", kepadas);
		parameters.put("jenis", jenises);
		parameters.put("jumlah", Math.abs(jumlahDebet));
		parameters.put("grup_ids", kodeGrup.toArray());
		parameters.put("grup_id", -1L);
		parameters.put("cak", nomors);
		Connection conn = session.connection();
		parameters.put("koneksi_db", conn);

		parameters.put("terbilang", IndonesianNumberToWords.convert((long) Math.abs(jumlahDebet)).toUpperCase());

		parameters.put("jumlahDebet", jumlahDebet);
		parameters.put("maps", maps);
		return parameters;
	}

	@SuppressWarnings({})
	public void onReport(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(), "akunting/bukti_pengeluaran_kas",
					ais.ui.util.WaktuUtil.getDate(), null, toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Bukti Pengeluaran Kas Banyak", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
