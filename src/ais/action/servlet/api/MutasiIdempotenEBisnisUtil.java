package ais.action.servlet.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.MutasiIdempoten;
import ais.database.model.Tbmuser;

/**
 * Idempotensi antrean master offline-first lini eBisnis (servlet
 * {@link ais.action.servlet.ApiEBisnis}/{@link ais.action.servlet.PosApi}).
 *
 * <p>Klien Flutter POS ({@code apps/ebisnis}, service {@code MasterOffline})
 * menyertakan {@code client_mutation_id} HANYA pada payload mutasi master yang
 * diantre saat offline. Kiriman ulang dengan id yang sama — respons pertama
 * hilang di jaringan, atau flush ganda — mendapat kembali respons tersimpan
 * (ditandai {@code idempoten_replay: true}) alih-alih dieksekusi dua kali,
 * sehingga create yang di-replay tidak menciptakan baris ganda.</p>
 *
 * <p>Memakai tabel/entity {@link MutasiIdempoten} yang sama dengan
 * {@link MutasiIdempotenUtil} (jalur ais_mobile); dibedakan secara alami oleh
 * kombinasi unik (pengguna, aksi, clientMutationId). Seluruh jalur fail-open:
 * gangguan pencatatan idempotensi tidak pernah menggagalkan aksi pengguna.</p>
 */
public final class MutasiIdempotenEBisnisUtil {

	private MutasiIdempotenEBisnisUtil() {
	}

	/**
	 * Selaras dengan daftar aksi yang dilewatkan layar-layar master Flutter ke
	 * {@code MasterOffline.simpanAtauAntre} (dijaga kontrak test
	 * {@code master_offline_kontrak_test.dart} di repo klien). Aksi transaksi
	 * (bayar, retur, opname, mutasi stok, dst.) SENGAJA tidak ada di sini —
	 * jalur idempotensinya sendiri ({@code transaksi_pending}/{@code outbox_is}).
	 */
	private static final Set<String> AKSI_MASTER_ANTREAN = new HashSet<String>(Arrays.asList(
			"akun_tambah",
			"anggota_hapus",
			"anggota_foto_hapus",
			"anggota_foto_upload",
			"anggota_simpan",
			"anggota_simpan_cepat",
			"apotik_item_profil_simpan",
			"cara_bayar_hapus",
			"cara_bayar_simpan",
			"diskon_grup_hapus",
			"diskon_grup_simpan",
			"diskon_hapus",
			"diskon_simpan",
			"grup_produk_hapus",
			"grup_produk_simpan",
			"hotel_kontrak_pemilik_simpan",
			"hotel_properti_simpan",
			"hotel_tamu_simpan",
			"jenis_anggota_hapus",
			"jenis_anggota_simpan",
			"jenis_produk_hapus",
			"jenis_produk_simpan",
			"kebijakan_retur_hapus",
			"kebijakan_retur_simpan",
			"layar_pelanggan_screensaver_config_simpan",
			"layar_pelanggan_slide_hapus",
			"layar_pelanggan_slide_ubah",
			"notifikasi_hapus",
			"pedagang_ubah",
			"pencairan_diskon_hapus",
			"pencairan_diskon_simpan",
			"penyedia_hapus",
			"penyedia_simpan",
			"produk_batch_simpan",
			"produk_foto_hapus",
			"produk_simpan",
			"si_customer_create",
			"si_customer_deactivate",
			"si_customer_update",
			"si_sales_create",
			"si_sales_deactivate",
			"si_sales_update",
			"si_supplier_create",
			"si_supplier_deactivate",
			"si_supplier_update",
			"tipe_anggota_hapus",
			"tipe_anggota_simpan",
			"toko_kelola_hapus",
			"toko_kelola_simpan",
			"toko_profil_simpan"));

	public static boolean aksiMasterAntrean(String action) {
		return action != null && AKSI_MASTER_ANTREAN.contains(action.trim().toLowerCase(Locale.ENGLISH));
	}

	/** Status sukses jalur PosApi: "success" (gerbang klien Flutter) atau "00" (helper lama). */
	public static boolean responsSukses(JSONObject hasil) {
		String status = hasil == null ? null : hasil.optString("status", "");
		return "success".equals(status) || "00".equals(status);
	}

	private static String penggunaDari(Tbmuser tbmuser) {
		if (tbmuser == null || tbmuser.getUserId() == null) {
			return null;
		}
		return String.valueOf(tbmuser.getUserId());
	}

	/**
	 * Respons tersimpan untuk kunci (pengguna, aksi, clientMutationId), atau
	 * {@code null} bila mutasi ini belum pernah sukses dieksekusi.
	 */
	public static JSONObject ambil(Tbmuser tbmuser, String action, String clientMutationId) {
		String pengguna = penggunaDari(tbmuser);
		if (pengguna == null || clientMutationId == null || clientMutationId.length() == 0) {
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
			// Fail-open: idempotensi tidak boleh menggagalkan request.
			ais.common.ErrorAuditUtil.record(e, "MutasiIdempotenEBisnisUtil.ambil");
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Menyimpan respons sukses eksekusi pertama. Balapan dua retry diwasiti
	 * unique constraint database — simpanan pertama menang, kegagalan insert
	 * berikutnya diabaikan.
	 */
	public static void simpan(Tbmuser tbmuser, String action, String clientMutationId, JSONObject respons) {
		String pengguna = penggunaDari(tbmuser);
		if (pengguna == null || clientMutationId == null || clientMutationId.length() == 0) {
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
					// Tidak ada lagi yang bisa dilakukan pada transaksi ini.
				}
			}
			// Kemungkinan besar pelanggaran unique constraint karena balapan.
			ais.common.ErrorAuditUtil.record(e, "MutasiIdempotenEBisnisUtil.simpan");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
