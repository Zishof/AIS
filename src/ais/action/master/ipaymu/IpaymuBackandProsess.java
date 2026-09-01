package ais.action.master.ipaymu;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.servlet.IPayMuResponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.ipaymu.IpaymuResponse;

/**
 * Tugas terjadwal ({@link TimerTask}) yang secara berkala mengecek ulang status transaksi
 * pembayaran iPaymu yang masih tertunda ({@code PENDING}/{@code SEDANG_DIPROSES}) dalam 3 bulan
 * terakhir, lalu menyinkronkan status lokal ({@link IpaymuResponse}) dengan hasil API
 * "CekTransaksi" iPaymu. Hanya berjalan bila konfigurasi {@code aktifkan_pembayaran_via_ipaymu}
 * aktif (default TIDAK AKTIF). Bila transaksi berhasil (status 1), memicu tindak lanjut lewat
 * {@link IPayMuResponse#prosesResponse(IpaymuResponse)} (mis. pelunasan tagihan terkait).
 *
 * <p>
 * <b>DIPERBAIKI 2026-09-01</b> — {@link #run()} sebelumnya memakai API key iPaymu hardcoded
 * sebagai nilai default/fallback bila konfigurasi {@code ipaymu_key} belum diisi di database
 * (nilai yang sama dengan default lama {@code ais.common.IpaymuCommon}, sudah diperbaiki
 * terpisah). Default itu sudah dihapus (kini string kosong). Key lama yang sebelumnya tertanam
 * sudah lama berada di riwayat SVN dan WAJIB dianggap bocor — perlu dirotasi di sisi iPaymu bila
 * masih aktif di produksi.
 * </p>
 */
public class IpaymuBackandProsess extends TimerTask {

	/**
	 * Menjalankan satu siklus pengecekan status transaksi iPaymu yang masih tertunda dalam 3 bulan
	 * terakhir dan menyinkronkan hasilnya ke database lokal. Lihat javadoc kelas untuk alur dan
	 * peringatan keamanan terkait API key default. Tidak melakukan apa-apa bila konfigurasi
	 * {@code aktifkan_pembayaran_via_ipaymu} tidak aktif. Kegagalan per-transaksi (mis. request
	 * HTTP gagal) ditangkap per item sehingga tidak menghentikan pemrosesan transaksi lain dalam
	 * siklus yang sama.
	 */
	@Override
	public void run() {
		try {

		// int jam = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.HOUR_OF_DAY);
		// int minute = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MINUTE);
		// if (jam == 4 && minute == 30) {
		// try {
		// BackupUtil.deleteDatabase();
		// BackupUtil.backupPGSQL(null);
		// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/ipaymu/IpaymuBackandProsess.java:32");
		// Common.tampilErrorJikaAdmin(e); 
		// }
		// }

		boolean aktifkan_pembayaran_via_ipaymu = Common.bolehKonfigurasi("aktifkan_pembayaran_via_ipaymu", Konfigurasi.TIDAK_AKTIF);

		// System.out.println("==========================IpaymuBackandProsess==>"
		// + aktifkan_pembayaran_via_ipaymu);
		if (aktifkan_pembayaran_via_ipaymu) {

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 3);
			Date kemarin = calendar.getTime();

			String sqlRes = "this_.tanggal_dirubah > '" + Common.databaseDateFormat1.get().format(kemarin) + "'";

			String key = Common.getKonfigurasi("ipaymu_key", "").getNilai();
			String urlCheck = Common
					.getKonfigurasi("ipaymu_cek_transaksi_url", "https://my.ipaymu.com/api/CekTransaksi.php").getNilai()
					+ "?key=" + key + "&format=json";
			Session session = HibernateUtil.currentNativeSession();
			int jumlah = ((Number) session.createCriteria(IpaymuResponse.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("status", IpaymuResponse.PENDING)).add(Restrictions.sqlRestriction(sqlRes))
					.uniqueResult()).intValue();
			if (jumlah > 0) {
				@SuppressWarnings("unchecked")
				List<IpaymuResponse> ipaymuResponses = session.createCriteria(IpaymuResponse.class)
						.add(Restrictions.sqlRestriction(sqlRes)).addOrder(Order.asc("id"))
						.add(Restrictions.or(Restrictions.eq("status", IpaymuResponse.SEDANG_DIPROSES),
								Restrictions.eq("status", IpaymuResponse.PENDING)))
						.list();
				for (IpaymuResponse ipaymuResponse : ipaymuResponses) {
					try {
						String hasil = Common.sendGet(urlCheck + "&id=" + ipaymuResponse.getTrxId());
						JSONObject jsonObject = new JSONObject(hasil);
						// System.out.println("ipaymu jsonObject => " +
						// jsonObject);
						int status = 0;
						if (!jsonObject.isNull("status")) {
							status = jsonObject.getInt("status");
						}
						if (!jsonObject.isNull("Status")) {
							status = jsonObject.getInt("Status");
						}
						if (status == -1) {
							ipaymuResponse.setStatus(IpaymuResponse.SEDANG_DIPROSES);
						} else if (status == 0) {
							ipaymuResponse.setStatus(IpaymuResponse.PENDING);
						} else if (status == 1) {
							ipaymuResponse.setStatus(IpaymuResponse.BERHASIL);
						} else if (status == 2) {
							ipaymuResponse.setStatus(IpaymuResponse.BATAL);
						} else if (status == 3) {
							ipaymuResponse.setStatus(IpaymuResponse.REFUND);
						} else {
							ipaymuResponse.setStatus(IpaymuResponse.BATAL);
						}

						ipaymuResponse.setKeterangan(jsonObject == null ? "" : jsonObject.toString());
						session.getTransaction().begin();
						Common.refreshUpdate(session, ipaymuResponse);
						session.getTransaction().commit();

						if (ipaymuResponse.getStatus().equalsIgnoreCase(IpaymuResponse.BERHASIL)) {
							IPayMuResponse.prosesResponse(ipaymuResponse);
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e); 
					}
				}

			}
			HibernateUtil.closeSession();
		}
			} finally {
			ais.database.hibernate.HibernateUtil.closeSession();
		}
	}

}
