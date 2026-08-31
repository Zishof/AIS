package ais.action.report.format1.akunting;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
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
import ais.common.IndonesianNumberToWords;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.ui.util.MyWindow;

/**
 * Penyusun/penyaji laporan untuk laporan bukti pengeluaran kas. Kelas ini mengubah data domain
 * menjadi bentuk laporan yang dipakai UI, ekspor, atau proses cetak tanpa memindahkan aturan
 * transaksi ke lapisan report.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * MyWindow}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Center center}, {@code Toolbar
 * toolbar}, {@code GrupTransaksi grupTransaksi}, {@code double jumlahDebet}; inisialisasi/lifecycle ({@code
 * init()}); pelaporan/ekspor ({@code onReport()}); operasi domain lain ({@code generateParameter()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 *
 * @see MyWindow
 */
public class LaporanBuktiPengeluaranKas extends MyWindow {

	/**
	 *  
	 */
	private static final long serialVersionUID = 3331244819198611604L;

	private Center center;
	private Toolbar toolbar;

	private GrupTransaksi grupTransaksi;

	private double jumlahDebet;

	public LaporanBuktiPengeluaranKas(GrupTransaksi grupTransaksi) {
		super();
		this.grupTransaksi = grupTransaksi;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Bukti Pengeluaran Kas", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
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
		}, "akunting/bukti_pengeluaran_kas", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onReport(arg0);
			}
		}));

		onReport(null);

	}

	@SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
	private Map generateParameter() throws Exception {

		Session session = HibernateUtil.currentSession();
		List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
				.addOrder(Order.desc("debet")).addOrder(Order.asc("tanggalTransaksi"))
				.add(Restrictions.eq("grupTransaksi", grupTransaksi)).list();
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();

		List<Long> kodeAkuns = new ArrayList<Long>();
		String akun = "";
		jumlahDebet = 0.0;
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

		for (Transaksi transaksi : transaksis) {

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

		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("tanggal", Common.dateFormat4.get().format(grupTransaksi.getTanggalTransaksi()));
		parameters.put("nomor", grupTransaksi.getKode());
		parameters.put("akun", akun);
		parameters.put("atas_nama", grupTransaksi.getKepada());
		parameters.put("jenis",
				grupTransaksi.getJenisTransaksi() == null ? null : grupTransaksi.getJenisTransaksi().getKeterangan());
		parameters.put("jumlah", Math.abs(jumlahDebet));
		parameters.put("grup_id", grupTransaksi.getId());
		parameters.put("grup_ids", new Long[] { -1L });
		parameters.put("cak", grupTransaksi.getNomorTagihan());
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
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Bukti Pengeluaran Kas", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
