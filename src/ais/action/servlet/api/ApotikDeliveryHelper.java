package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikDeliveryOrder;
import ais.database.model.sirs.TransaksiMedis;

/** API delivery obat khusus apotik; tidak mencampur dokumen distribusi gudang umum. */
public final class ApotikDeliveryHelper {
	private ApotikDeliveryHelper() { }

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static String str(Object nilai) { return nilai == null ? "" : nilai.toString(); }

	private static Long optLong(JSONObject j, String key) {
		if (j == null || j.isNull(key)) return null;
		try { return Long.valueOf(j.getLong(key)); } catch (Exception e) { return null; }
	}

	private static boolean statusSah(String status) {
		return ApotikDeliveryOrder.MENUNGGU.equals(status)
				|| ApotikDeliveryOrder.DISIAPKAN.equals(status)
				|| ApotikDeliveryOrder.DIKIRIM.equals(status)
				|| ApotikDeliveryOrder.TERKIRIM.equals(status)
				|| ApotikDeliveryOrder.GAGAL.equals(status)
				|| ApotikDeliveryOrder.DIBATALKAN.equals(status);
	}

	private static void isi(JSONObject j, ApotikDeliveryOrder d) throws Exception {
		java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
		j.put("id", d.getId());
		j.put("kode", str(d.getKode()));
		TransaksiMedis t = d.getTransaksi();
		j.put("transaksiId", t == null ? JSONObject.NULL : t.getId());
		j.put("kodeTransaksi", t == null ? "" : str(t.getKode()));
		j.put("namaPenerima", str(d.getNamaPenerima()));
		j.put("telepon", str(d.getTelepon()));
		j.put("alamat", str(d.getAlamat()));
		j.put("kurir", str(d.getKurir()));
		j.put("layanan", str(d.getLayanan()));
		j.put("nomorPelacakan", str(d.getNomorPelacakan()));
		j.put("biayaKirim", d.getBiayaKirim().doubleValue());
		j.put("status", d.getStatus());
		j.put("waktuPesan", d.getWaktuPesan() == null ? "" : f.format(d.getWaktuPesan()));
		j.put("waktuKirim", d.getWaktuKirim() == null ? "" : f.format(d.getWaktuKirim()));
		j.put("waktuTerima", d.getWaktuTerima() == null ? "" : f.format(d.getWaktuTerima()));
		j.put("buktiTerima", str(d.getBuktiTerima()));
		j.put("keterangan", str(d.getKeterangan()));
	}

	public static void daftar(JSONObject request, JSONObject hasil) throws Exception {
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 100 : request.optInt("page_size", 100);
		if (size < 1) size = 100;
		if (size > 100) size = 100;
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		String cari = request == null ? "" : request.optString("keyword", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ApotikDeliveryOrder.class)
					.addOrder(Order.desc("id"));
			if (status.length() > 0) c.add(Restrictions.eq("status", status));
			if (cari.length() > 0) {
				c.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + cari + "%"))
						.add(Restrictions.ilike("namaPenerima", "%" + cari + "%"))
						.add(Restrictions.ilike("nomorPelacakan", "%" + cari + "%")));
			}
			c.setFirstResult((page - 1) * size).setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<ApotikDeliveryOrder> list = c.list();
			JSONArray data = new JSONArray();
			for (ApotikDeliveryOrder d : list) { JSONObject j = new JSONObject(); isi(j, d); data.put(j); }
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("page", page);
			hasil.put("pageSize", size);
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void simpan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		String nama = request == null ? "" : request.optString("nama_penerima", "").trim();
		String alamat = request == null ? "" : request.optString("alamat", "").trim();
		if (nama.length() == 0 || alamat.length() == 0) {
			tolak(hasil, "Nama penerima dan alamat wajib diisi."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Long id = optLong(request, "id");
			ApotikDeliveryOrder d = id == null ? new ApotikDeliveryOrder()
					: (ApotikDeliveryOrder) session.get(ApotikDeliveryOrder.class, id);
			if (d == null) { tolak(hasil, "Delivery order tidak ditemukan."); tx.rollback(); return; }
			if (id == null) {
				d.setKode("ADO-" + System.currentTimeMillis());
				d.setStatus(ApotikDeliveryOrder.MENUNGGU);
				d.setWaktuPesan(new Date());
			}
			Long transaksiId = optLong(request, "transaksi_id");
			if (transaksiId != null) d.setTransaksi((TransaksiMedis) session.get(TransaksiMedis.class, transaksiId));
			d.setNamaPenerima(nama); d.setAlamat(alamat);
			d.setTelepon(request.optString("telepon", "").trim());
			d.setKurir(request.optString("kurir", "").trim());
			d.setLayanan(request.optString("layanan", "").trim());
			d.setNomorPelacakan(request.optString("nomor_pelacakan", "").trim());
			d.setBiayaKirim(Double.valueOf(request.optDouble("biaya_kirim", 0)));
			d.setKeterangan(request.optString("keterangan", "").trim());
			d.setOleh(user.getUserId()); d.setOlehId(user.getUserId());
			session.saveOrUpdate(d); tx.commit();
			JSONObject j = new JSONObject(); isi(j, d);
			hasil.put("status", "00"); hasil.put("data", j);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	public static void ubahStatus(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long id = optLong(request, "id");
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		if (id == null || !statusSah(status)) { tolak(hasil, "ID dan status delivery yang sah wajib diisi."); return; }
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			ApotikDeliveryOrder d = (ApotikDeliveryOrder) session.get(ApotikDeliveryOrder.class, id);
			if (d == null) { tolak(hasil, "Delivery order tidak ditemukan."); tx.rollback(); return; }
			d.setStatus(status);
			if (ApotikDeliveryOrder.DIKIRIM.equals(status) && d.getWaktuKirim() == null) d.setWaktuKirim(new Date());
			if (ApotikDeliveryOrder.TERKIRIM.equals(status)) {
				d.setWaktuTerima(new Date());
				d.setBuktiTerima(request.optString("bukti_terima", "").trim());
			}
			d.setOleh(user.getUserId()); d.setOlehId(user.getUserId());
			session.update(d); tx.commit();
			JSONObject j = new JSONObject(); isi(j, d);
			hasil.put("status", "00"); hasil.put("data", j);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}

	/** Menyiapkan 100 delivery sintetis hanya pada instalasi demo. */
	public static void provisionDemo(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		if (!Common.bolehKonfigurasi(Konfigurasi.DATA_SAMPLE_EBISNIS, Konfigurasi.TIDAK_AKTIF)
				|| user == null || !Common.getApakahAdminLain(user)
				|| request == null || !"SEED-DEMO-APOTIK".equals(request.optString("konfirmasi", ""))) {
			tolak(hasil, "Provisioning delivery hanya tersedia untuk admin pada server demo."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession(); Transaction tx = null;
		try {
			tx = session.beginTransaction();
			@SuppressWarnings("unchecked")
			List<TransaksiMedis> transaksi = session.createQuery(
					"from TransaksiMedis t where t.kode like :kode order by t.kode")
					.setString("kode", "TRX-APT-UAT-CTL-%").setMaxResults(100).list();
			int dibuat = 0;
			for (int i = 0; i < transaksi.size(); i++) {
				String kode = String.format("DO-APT-UAT-%04d", Integer.valueOf(i + 1));
				Object ada = session.createCriteria(ApotikDeliveryOrder.class)
						.add(Restrictions.eq("kode", kode)).setMaxResults(1).uniqueResult();
				if (ada != null) continue;
				TransaksiMedis t = transaksi.get(i);
				ApotikDeliveryOrder d = new ApotikDeliveryOrder();
				d.setKode(kode); d.setTransaksi(t);
				d.setNamaPenerima(str(t.getNama()).length() == 0 ? "Penerima Demo " + (i + 1) : str(t.getNama()));
				d.setTelepon("0812" + String.format("%08d", Integer.valueOf(i + 1)));
				d.setAlamat(str(t.getAlamat()).length() == 0 ? "Alamat delivery DATA SAMPLE/UAT " + (i + 1) : str(t.getAlamat()));
				d.setKurir(i % 3 == 0 ? "Kurir Apotik" : i % 3 == 1 ? "Motor Instan" : "Ambil Terjadwal");
				d.setLayanan(i % 2 == 0 ? "SAME DAY" : "REGULER");
				d.setNomorPelacakan("TRACK-APT-" + String.format("%05d", Integer.valueOf(i + 1)));
				d.setBiayaKirim(Double.valueOf(5000 + (i % 5) * 2500));
				d.setStatus(i % 4 == 0 ? ApotikDeliveryOrder.TERKIRIM : i % 4 == 1 ? ApotikDeliveryOrder.DIKIRIM : i % 4 == 2 ? ApotikDeliveryOrder.DISIAPKAN : ApotikDeliveryOrder.MENUNGGU);
				d.setWaktuPesan(new Date(System.currentTimeMillis() - (long) i * 3600000L));
				if (ApotikDeliveryOrder.DIKIRIM.equals(d.getStatus()) || ApotikDeliveryOrder.TERKIRIM.equals(d.getStatus())) d.setWaktuKirim(new Date());
				if (ApotikDeliveryOrder.TERKIRIM.equals(d.getStatus())) { d.setWaktuTerima(new Date()); d.setBuktiTerima("Diterima penerima DATA SAMPLE/UAT"); }
				d.setKeterangan("DATA SAMPLE/UAT — bukan pengiriman pasien nyata.");
				d.setOleh(user.getUserId()); d.setOlehId(user.getUserId());
				session.save(d); dibuat++;
				if (i > 0 && i % 25 == 0) session.flush();
			}
			tx.commit(); hasil.put("status", "00"); hasil.put("dibuat", dibuat); hasil.put("total", transaksi.size());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally { HibernateUtil.closeSessionQuietly(session); }
	}
}
