package ais.action.master.feeder.util;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.NeoFeederSync;
import ais.ui.util.WaktuUtil;

/**
 * <h3>NeoFeederSyncHelper — pencatat (upsert) status sinkronisasi Neo Feeder</h3>
 *
 * <p>Menyimpan jejak setiap operasi ke/dari Neo Feeder PDDikti ke tabel penampung
 * {@link NeoFeederSync}. Pola <i>upsert per aksi</i>: untuk satu aksi/endpoint
 * (mis. {@code GetListDosen}) hanya ada SATU baris yang selalu diperbarui — sehingga
 * tabel menjadi snapshot status terkini tiap entitas Neo Feeder. Setiap kali Refresh
 * (ambil dari feeder) maupun kirim/terima data, baris terkait di-upsert sehingga
 * {@code json_request}, {@code json_response}, {@code status}, jumlah data, dan
 * {@code tanggal_sinkron} selalu mencerminkan operasi terakhir.</p>
 *
 * <p><b>Threading / sesi.</b> Memakai sesi terdedikasi ({@link HibernateUtil#openSession()})
 * dengan transaksi sendiri lalu ditutup di {@code finally} — tidak memakai/menutup
 * session milik request ZK. Bersifat <b>fail-safe</b>: kegagalan apa pun (DB/koneksi)
 * tidak pernah dilempar ke pemanggil agar TIDAK mengganggu alur sinkronisasi utama.</p>
 *
 * <p><b>Gaya Java 1.6/1.7</b> (tanpa lambda/stream/diamond/try-with-resources).</p>
 */
public final class NeoFeederSyncHelper {

	/** Batas panjang payload JSON yang disimpan agar baris tidak membengkak. */
	private static final int MAKS_JSON = 500000;

	private NeoFeederSyncHelper() {
	}

	/** Gerbang konfigurasi (default AKTIF). Bisa dimatikan tanpa redeploy. */
	private static boolean aktif() {
		try {
			return Common.bolehKonfigurasi("aktifkan_neo_feeder_sync_log");
		} catch (Throwable t) {
			return true; // fail-open: jika konfigurasi bermasalah, tetap catat
		}
	}

	private static String potong(String v) {
		if (v == null) {
			return null;
		}
		return v.length() > MAKS_JSON ? v.substring(0, MAKS_JSON) + "...(terpotong)" : v;
	}

	/**
	 * Menurunkan nama entitas dari nama aksi Neo Feeder dengan membuang awalan umum
	 * (GetList/GetCount/Get/Insert/Update/Delete/Save). Mis. {@code GetListDosen} -&gt;
	 * {@code Dosen}. Bila tidak cocok, kembalikan apa adanya.
	 */
	public static String entitasDariAksi(String aksi) {
		if (aksi == null) {
			return null;
		}
		String e = aksi.trim();
		String[] awalan = { "GetCountRekap", "GetListRekap", "GetRekap", "GetCount", "GetList", "GetRecordset",
				"GetRecord", "Get", "Insert", "Update", "Delete", "Save" };
		for (int i = 0; i < awalan.length; i++) {
			if (e.length() > awalan[i].length() && e.startsWith(awalan[i])) {
				return e.substring(awalan[i].length());
			}
		}
		return e;
	}

	/**
	 * Pemetaan nama entitas Neo Feeder ke class entitas lokal eCampus yang menjadi
	 * tujuan sinkronisasi. Hanya entitas utama yang dipetakan saat ini (Dosen,
	 * Mahasiswa, Matakuliah, Kurikulum); selebihnya {@code null} (belum dipetakan) dan
	 * bisa ditambah belakangan. Cocok tanpa peduli besar/kecil huruf.
	 */
	public static String kelasLokalDariEntitas(String entitas) {
		if (entitas == null) {
			return null;
		}
		String e = entitas.trim().toLowerCase();
		if (e.equals("dosen")) {
			return "ais.database.model.Dosen";
		}
		if (e.equals("mahasiswa")) {
			return "ais.database.model.Mahasiswa";
		}
		if (e.equals("matakuliah")) {
			return "ais.database.model.Matakuliah";
		}
		if (e.equals("kurikulum")) {
			return "ais.database.model.Kurikulum";
		}
		return null;
	}

	/**
	 * Menghitung jumlah record lokal untuk {@code classReference} memakai {@code session}
	 * yang diberikan (mis. session request ZK — JANGAN ditutup oleh pemanggil bila itu
	 * {@code currentSession}). Fail-safe: mengembalikan {@code null} bila class tidak ada
	 * atau query gagal.
	 */
	public static Long hitungLokal(Session session, String classReference) {
		if (session == null || classReference == null || classReference.trim().isEmpty()) {
			return null;
		}
		try {
			Class kelas = Class.forName(classReference.trim());
			Object hasil = session.createCriteria(kelas).setProjection(Projections.rowCount()).uniqueResult();
			if (hasil instanceof Number) {
				return Long.valueOf(((Number) hasil).longValue());
			}
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Memuat SEMUA baris tersimpan (per host) menjadi peta {@code aksi -> NeoFeederSync}
	 * dalam SATU sesi terdedikasi (efisien untuk halaman dashboard yang membuka banyak
	 * panel). Entitas dikembalikan dalam keadaan detached; hanya field skalar yang dipakai
	 * sehingga aman. Fail-safe: mengembalikan peta kosong bila gagal.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, NeoFeederSync> ambilSemuaTersimpan(String host) {
		Map<String, NeoFeederSync> map = new LinkedHashMap<String, NeoFeederSync>();
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Criteria c = session.createCriteria(NeoFeederSync.class);
			if (host != null && host.trim().length() > 0) {
				c.add(Restrictions.eq("host", host));
			}
			c.addOrder(Order.asc("id"));
			List<NeoFeederSync> list = c.list();
			if (list != null) {
				for (int i = 0; i < list.size(); i++) {
					NeoFeederSync r = list.get(i);
					if (r != null && r.getAksi() != null) {
						map.put(r.getAksi(), r); // id asc -> entri belakang (id terbesar) = terbaru
					}
				}
			}
		} catch (Throwable t) {
			try {
				System.err.println("[NeoFeederSync] Gagal memuat data tersimpan: " + t);
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederSyncHelper.java:159");
			}
		} finally {
			tutup(session);
		}
		return map;
	}

	/**
	 * Upsert satu baris {@link NeoFeederSync} untuk {@code aksi} tertentu.
	 *
	 * @param aksi         nama aksi/endpoint Neo Feeder (kunci upsert), mis. "GetListDosen"
	 * @param nama         label panel/keterangan ringkas (boleh null)
	 * @param arah         {@link NeoFeederSync#ARAH_AMBIL} / {@link NeoFeederSync#ARAH_KIRIM}
	 * @param status       salah satu konstanta {@code NeoFeederSync.STATUS_*}
	 * @param jsonRequest  payload permintaan (JSON, token sudah disamarkan) — boleh null
	 * @param jsonResponse payload tanggapan (JSON) — boleh null
	 * @param jumlahData   jumlah record pada tanggapan (boleh null)
	 * @param jumlahFeeder total record di feeder/GetCount (boleh null)
	 * @param pesan        pesan info/error (boleh null)
	 * @param host         host Neo Feeder yang dipakai (boleh null)
	 */
	public static void catat(String aksi, String nama, String arah, String status, String jsonRequest,
			String jsonResponse, Integer jumlahData, Integer jumlahFeeder, String pesan, String host) {
		if (aksi == null || aksi.trim().isEmpty()) {
			return;
		}
		if (!aktif()) {
			return;
		}
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			session.beginTransaction();

			NeoFeederSync row = cari(session, aksi, host);
			boolean baru = false;
			if (row == null) {
				row = new NeoFeederSync();
				row.setAksi(aksi);
				baru = true;
			}
			row.setEntitas(entitasDariAksi(aksi));
			// Petakan class entitas lokal tujuan sinkronisasi (bila dikenal).
			String clazz = kelasLokalDariEntitas(row.getEntitas());
			if (clazz != null) {
				row.setClassReference(clazz);
			}
			if (nama != null && nama.trim().length() > 0) {
				row.setNama(nama);
			}
			row.setArah(arah);
			row.setStatus(status);
			row.setJsonRequest(potong(jsonRequest));
			row.setJsonResponse(potong(jsonResponse));
			row.setJumlahData(jumlahData);
			row.setJumlahFeeder(jumlahFeeder);
			row.setKeterangan(pesan);
			if (host != null && host.trim().length() > 0) {
				row.setHost(host);
			}
			row.setTanggalSinkron(WaktuUtil.getDate());
			row.setTanggal_dirubah(WaktuUtil.getDate());

			if (baru) {
				session.save(row);
			} else {
				session.update(row);
			}
			session.getTransaction().commit();
		} catch (Throwable t) {
			rollback(session);
			try {
				System.err.println("[NeoFeederSync] Gagal mencatat aksi=" + aksi + ", status=" + status + ": " + t);
			} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederSyncHelper.java:233");
			}
		} finally {
			tutup(session);
		}
	}

	/** Versi ringkas tanpa host/jumlahFeeder/id-record. */
	public static void catat(String aksi, String nama, String arah, String status, String jsonRequest,
			String jsonResponse, Integer jumlahData, String pesan) {
		catat(aksi, nama, arah, status, jsonRequest, jsonResponse, jumlahData, null, pesan, null);
	}

	/** Tandai sebuah aksi sedang diproses (status Proses) sebelum operasi dimulai. */
	public static void catatProses(String aksi, String nama, String arah, String jsonRequest, String host) {
		catat(aksi, nama, arah, NeoFeederSync.STATUS_PROSES, jsonRequest, null, null, null, null, host);
	}

	@SuppressWarnings("unchecked")
	private static NeoFeederSync cari(Session session, String aksi, String host) {
		Criteria c = session.createCriteria(NeoFeederSync.class).add(Restrictions.eq("aksi", aksi));
		if (host != null && host.trim().length() > 0) {
			c.add(Restrictions.eq("host", host));
		}
		c.addOrder(Order.desc("id")).setMaxResults(1);
		List<NeoFeederSync> list = c.list();
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	private static void rollback(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederSyncHelper.java:267");
		}
	}

	private static void tutup(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				session.disconnect();
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederSyncHelper.java:279");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/feeder/util/NeoFeederSyncHelper.java:285");
		}
	}
}
