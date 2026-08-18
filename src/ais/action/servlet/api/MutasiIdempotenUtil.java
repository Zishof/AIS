package ais.action.servlet.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.MutasiIdempoten;
import ais.database.model.Tbmuser;

/**
 * Idempotensi mutasi offline-first (Tahap C).
 *
 * Berlaku hanya untuk action mutasi yang memang diantre klien mobile dengan
 * {@code clientMutationId} (daftar yang sama dengan OfflineRequestPolicy di
 * aplikasi Flutter). Action baca dan transaksi kritis tidak tersentuh.
 */
public final class MutasiIdempotenUtil {

	private MutasiIdempotenUtil() {
	}

	/** Selaras dengan `_queueableMutations` OfflineRequestPolicy Flutter. */
	private static final Set<String> AKSI_MUTASI = new HashSet<String>(Arrays.asList(
			"simpanangket",
			"simpanangketumum",
			"simpandatarinci",
			"simpandatabanyak",
			"pengaduanmahasiswa",
			"sop_ajukan",
			"disposisi_simpan_surat_keluar",
			"disposisi_simpan_surat_masuk"));

	public static boolean aksiMutasi(String action) {
		return action != null && AKSI_MUTASI.contains(action.trim().toLowerCase(Locale.ENGLISH));
	}

	private static String penggunaDari(JSONObject request, HttpServletRequest servletRequest) {
		try {
			Tbmuser tbmuser = ApiUtil.currentUser(request, servletRequest);
			if (tbmuser == null || tbmuser.getUserId() == null) {
				return null;
			}
			return String.valueOf(tbmuser.getUserId());
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Respons tersimpan untuk kunci ini, atau {@code null} bila belum pernah
	 * dieksekusi (atau token tidak valid — biarkan route yang menolaknya).
	 */
	public static JSONObject ambil(JSONObject request, HttpServletRequest servletRequest, String action,
			String clientMutationId) {
		String pengguna = penggunaDari(request, servletRequest);
		if (pengguna == null) {
			return null;
		}
		Session session = HibernateUtil.openSession();
		try {
			MutasiIdempoten tersimpan = (MutasiIdempoten) session.createCriteria(MutasiIdempoten.class)
					.add(Restrictions.eq("pengguna", pengguna))
					.add(Restrictions.eq("aksi", action))
					.add(Restrictions.eq("clientMutationId", clientMutationId))
					.uniqueResult();
			if (tersimpan == null || tersimpan.getRespons() == null) {
				return null;
			}
			JSONObject respons = new JSONObject(tersimpan.getRespons());
			respons.put("idempoten_replay", true);
			return respons;
		} catch (Exception e) {
			// Idempotensi tidak boleh menggagalkan request; jatuh ke eksekusi.
			ais.common.ErrorAuditUtil.record(e, "MutasiIdempotenUtil.ambil");
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menyimpan respons sukses eksekusi pertama. Bila kunci sudah ada (balapan
	 * dua retry), simpanan pertama menang dan kegagalan insert diabaikan —
	 * unique constraint database yang menjadi wasitnya.
	 */
	public static void simpan(JSONObject request, HttpServletRequest servletRequest, String action,
			String clientMutationId, JSONObject respons) {
		String pengguna = penggunaDari(request, servletRequest);
		if (pengguna == null) {
			return;
		}
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			MutasiIdempoten catatan = new MutasiIdempoten();
			catatan.setPengguna(pengguna);
			catatan.setAksi(action);
			catatan.setClientMutationId(clientMutationId);
			catatan.setRespons(respons.toString());
			session.save(catatan);
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) {
					// Tidak ada yang bisa dilakukan lagi pada transaksi ini.
				}
			}
			// Kemungkinan besar pelanggaran unique constraint karena balapan;
			// respons pertama yang tersimpan tetap menjadi kebenaran.
			ais.common.ErrorAuditUtil.record(e, "MutasiIdempotenUtil.simpan");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
