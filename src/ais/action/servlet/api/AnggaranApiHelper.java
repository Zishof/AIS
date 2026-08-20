package ais.action.servlet.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;

/**
 * <h3>API JSON "Anggaran" (RAB Bulanan) untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan menu yang selama ini hanya ada di layar ZK ke Desktop/Android, dengan ZK sebagai
 * RUJUKAN logika:</p>
 * <ul>
 * <li>{@code workspace_bulanan.zul} ({@code WorkspaceBulananAction}) &mdash; pemilihan Tahun
 *     Anggaran, Satuan Kerja, dan Sumber Dana, lalu daftar <b>Revisi</b>.</li>
 * <li>{@code workspace_revisi_bulanan.zul} ({@code WorkspaceRevisiBulananAction}) &mdash; pohon item
 *     anggaran satu revisi dengan rincian belanja per bulan (bulan1..bulan12), tambah/ubah/hapus
 *     item, dan pemeliharaan agregat induk.</li>
 * <li>{@code realisasi_bulanan.zul} ({@code RealisasiBulananAction}) &mdash; rekap pagu vs realisasi
 *     per bulan.</li>
 * <li>{@code penggunaan_anggaran.zul} ({@code PenggunaanAnggaranAction}) &mdash; daftar transaksi
 *     pemakaian anggaran yang menjadi sumber angka realisasi.</li>
 * </ul>
 *
 * <h3>Aturan yang SENGAJA disamakan dengan layar ZK</h3>
 * <ol>
 * <li><b>Agregat induk</b>: {@code bulanN} induk = JUMLAH {@code bulanN} seluruh anak, dan
 *     {@code hargaTotal} = jumlah dua belas bulan &mdash; dihitung ulang naik sampai akar setiap kali
 *     item disimpan/dihapus (padanan {@code WorkspaceTreeModel.ubahHargaTotalParentss}).</li>
 * <li><b>Penyaring keaktifan</b>: {@code carryOver = true OR aktif IS NULL OR aktif = true}, sama
 *     dengan kriteria {@code WorkspaceTreeModel}.</li>
 * <li><b>Realisasi</b> dihitung dari {@code PenggunaanAnggaran} dengan keaktifan mengikuti LOGIKA
 *     OBJECT ({@code PenggunaanAnggaran.getAktif()}), bukan sekadar kolom {@code aktif} &mdash;
 *     inilah sebabnya pembacaan memakai Hibernate, bukan SUM di SQL.</li>
 * <li><b>Revisi baru</b> menyalin seluruh pohon revisi tertinggi menjadi revisi berikutnya beserta
 *     hierarkinya (padanan {@code WorkspaceTreeModel.copy}/{@code copyChild}); nilai realisasi pada
 *     salinan dinolkan karena realisasi milik revisi lama.</li>
 * <li><b>Nilai bulanan yang belum diisi</b> dibaca lewat {@code Workspace.getBulanN()} sehingga item
 *     lama yang hanya punya {@code hargaTotal} tetap tampil terbagi rata dua belas bulan, persis
 *     seperti di layar ZK.</li>
 * </ol>
 *
 * <p>Revisi {@code -1} ({@code RabUtil.DEFAULT_REVISI}) adalah baris kerangka internal ZK
 * (akar satuan kerja), BUKAN revisi anggaran; baris itu tidak pernah ditampilkan sebagai pilihan.</p>
 *
 * <p>Gerbang hak akses memakai kunci menu {@code anggaran} pada {@code EbisnisMenuKatalog}
 * (grid CRUD {@code TbmroleAction}) dan dicek DI SERVER, bukan sekadar menyembunyikan tombol.</p>
 */
public final class AnggaranApiHelper {

	/** Revisi kerangka internal ZK (RabUtil.DEFAULT_REVISI) -- bukan revisi anggaran. */
	private static final int REVISI_KERANGKA = -1;

	/** Kunci menu pada EbisnisMenuKatalog; dipakai gerbang aksi granular. */
	private static final String KUNCI_MENU = "anggaran";

	private AnggaranApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (ais.common.Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		ais.database.model.Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return ais.common.EbisnisMenuKatalog.bolehAksi(
				ais.common.EbisnisMenuKatalog.urai(role.getEbisnisMenu()), KUNCI_MENU, aksi);
	}

	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksi(tbmuser, "create"));
		j.put("update", bolehAksi(tbmuser, "update"));
		j.put("delete", bolehAksi(tbmuser, "delete"));
		return j;
	}

	private static double d(Double v) {
		return v == null ? 0.0 : v.doubleValue();
	}

	private static void batalkan(Session session) {
		try {
			if (session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnggaranApiHelper.batalkan");
		}
	}

	// ============================================================ konteks & daftar

	/**
	 * Isi ketiga penyaring di kepala halaman: Tahun Anggaran, Satuan Kerja, dan Sumber Dana.
	 * Sama seperti layar ZK, Sumber Dana hanya relevan bila satuan kerja memilikinya.
	 */
	public static void konteks(Tbmuser tbmuser, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			JSONArray tahun = new JSONArray();
			List<?> tahunList = session
					.createQuery("select distinct w.tahunWorkspace from Workspace w"
							+ " where w.tahunWorkspace is not null order by w.tahunWorkspace desc")
					.list();
			for (Iterator<?> it = tahunList.iterator(); it.hasNext();) {
				Object o = it.next();
				if (o != null) {
					tahun.put(((Number) o).intValue());
				}
			}

			JSONArray satker = new JSONArray();
			List<?> satkerList = session
					.createQuery("select s.id, s.kode, s.nama, s.parent.id from SatuanKerja s"
							+ " where s.id in (select distinct w.satuanKerja.id from Workspace w)"
							+ " order by s.kode, s.nama")
					.list();
			for (Iterator<?> it = satkerList.iterator(); it.hasNext();) {
				Object[] r = (Object[]) it.next();
				JSONObject j = new JSONObject();
				j.put("id", ((Number) r[0]).longValue());
				j.put("kode", r[1] == null ? "" : r[1]);
				j.put("nama", r[2] == null ? "" : r[2]);
				j.put("parentId", r[3] == null ? JSONObject.NULL : Long.valueOf(((Number) r[3]).longValue()));
				satker.put(j);
			}

			JSONArray sumberDana = new JSONArray();
			List<?> sdList = session
					.createQuery("select s.id, s.kode, s.nama, s.tahun, s.satuanKerja.id from SumberDana s"
							+ " order by s.tahun desc, s.nama")
					.list();
			for (Iterator<?> it = sdList.iterator(); it.hasNext();) {
				Object[] r = (Object[]) it.next();
				JSONObject j = new JSONObject();
				j.put("id", ((Number) r[0]).longValue());
				j.put("kode", r[1] == null ? "" : r[1]);
				j.put("nama", r[2] == null ? "" : r[2]);
				j.put("tahun", r[3] == null ? JSONObject.NULL : Integer.valueOf(((Number) r[3]).intValue()));
				j.put("satkerId", r[4] == null ? JSONObject.NULL : Long.valueOf(((Number) r[4]).longValue()));
				sumberDana.put(j);
			}

			hasil.put("status", "00");
			hasil.put("tahun", tahun);
			hasil.put("satuanKerja", satker);
			hasil.put("sumberDana", sumberDana);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Satuan kerja terpilih + seluruh keturunannya (layar ZK memakai SatuanKerjaTreeModel). */
	private static Set<Long> satkerDanTurunan(Session session, long satkerId, boolean termasukAnak) {
		Set<Long> hasil = new HashSet<Long>();
		if (satkerId <= 0) {
			return hasil;
		}
		hasil.add(Long.valueOf(satkerId));
		if (!termasukAnak) {
			return hasil;
		}
		List<Long> lapis = new ArrayList<Long>();
		lapis.add(Long.valueOf(satkerId));
		int pagar = 0;
		while (!lapis.isEmpty() && pagar++ < 30) {
			List<?> anak = session.createQuery("select s.id from SatuanKerja s where s.parent.id in (:induk)")
					.setParameterList("induk", lapis).list();
			List<Long> berikut = new ArrayList<Long>();
			for (Iterator<?> it = anak.iterator(); it.hasNext();) {
				Long id = Long.valueOf(((Number) it.next()).longValue());
				if (hasil.add(id)) {
					berikut.add(id);
				}
			}
			lapis = berikut;
		}
		return hasil;
	}

	/** Potongan HQL penyaring keaktifan -- SAMA dengan kriteria WorkspaceTreeModel. */
	private static String klausaAktif(String alias) {
		return " and (" + alias + ".carryOver = true or " + alias + ".aktif is null or " + alias + ".aktif = true)";
	}

	@SuppressWarnings("unchecked")
	private static List<Workspace> ambilItem(Session session, int tahun, Set<Long> satkerIds, long sumberDanaId,
			int revisi, boolean termasukNonAktif) {
		StringBuilder hql = new StringBuilder("from Workspace w where w.tahunWorkspace = :tahun and w.revisi = :revisi");
		if (!satkerIds.isEmpty()) {
			hql.append(" and w.satuanKerja.id in (:satker)");
		}
		if (sumberDanaId > 0) {
			hql.append(" and w.sumberDana.id = :sd");
		}
		if (!termasukNonAktif) {
			hql.append(klausaAktif("w"));
		}
		hql.append(" order by w.kode asc, w.id asc");
		org.hibernate.Query q = session.createQuery(hql.toString());
		q.setInteger("tahun", tahun);
		q.setInteger("revisi", revisi);
		if (!satkerIds.isEmpty()) {
			q.setParameterList("satker", satkerIds);
		}
		if (sumberDanaId > 0) {
			q.setLong("sd", sumberDanaId);
		}
		return q.list();
	}

	/**
	 * Daftar revisi anggaran untuk satu (tahun, satuan kerja, sumber dana), lengkap dengan jumlah
	 * item dan pagunya. Revisi kerangka ({@value #REVISI_KERANGKA}) tidak ikut.
	 */
	public static void revisiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		long satkerId = request == null ? 0 : request.optLong("satkerId", 0);
		long sumberDanaId = request == null ? 0 : request.optLong("sumberDanaId", 0);
		boolean termasukAnak = request != null && request.optBoolean("termasukAnakSatker", true);
		if (tahun <= 0) {
			tolak(hasil, "Tahun Anggaran belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Set<Long> satkerIds = satkerDanTurunan(session, satkerId, termasukAnak);
			StringBuilder hql = new StringBuilder(
					"select w.revisi, count(w.id), sum(w.hargaTotal) from Workspace w"
							+ " where w.tahunWorkspace = :tahun and w.revisi <> :kerangka");
			if (!satkerIds.isEmpty()) {
				hql.append(" and w.satuanKerja.id in (:satker)");
			}
			if (sumberDanaId > 0) {
				hql.append(" and w.sumberDana.id = :sd");
			}
			hql.append(klausaAktif("w"));
			hql.append(" group by w.revisi order by w.revisi asc");
			org.hibernate.Query q = session.createQuery(hql.toString());
			q.setInteger("tahun", tahun);
			q.setInteger("kerangka", REVISI_KERANGKA);
			if (!satkerIds.isEmpty()) {
				q.setParameterList("satker", satkerIds);
			}
			if (sumberDanaId > 0) {
				q.setLong("sd", sumberDanaId);
			}
			JSONArray arr = new JSONArray();
			for (Iterator<?> it = q.list().iterator(); it.hasNext();) {
				Object[] r = (Object[]) it.next();
				JSONObject j = new JSONObject();
				j.put("revisi", ((Number) r[0]).intValue());
				j.put("jumlahItem", ((Number) r[1]).longValue());
				j.put("pagu", r[2] == null ? 0.0 : ((Number) r[2]).doubleValue());
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Satu item anggaran menjadi JSON (dipakai daftar rencana maupun realisasi). */
	private static JSONObject itemJson(Workspace w) throws Exception {
		JSONObject j = new JSONObject();
		j.put("id", w.getId());
		j.put("parentId", w.getParentId() == null ? JSONObject.NULL : w.getParentId());
		j.put("deep", w.getDeep() == null ? 0 : w.getDeep());
		j.put("kode", w.getKode() == null ? "" : w.getKode());
		j.put("nama", w.getNama() == null ? "" : w.getNama());
		j.put("keterangan", w.getKeterangan() == null ? "" : w.getKeterangan());
		j.put("qty", d(w.getQty()));
		j.put("satuanVolume", w.getSatuanVolume() == null ? "" : w.getSatuanVolume());
		j.put("hargaSatuan", d(w.getHargaSatuan()));
		j.put("hargaTotal", d(w.getHargaTotal()));
		j.put("realisasiTotal", d(w.getRealisasiTotal()));
		j.put("realisasiProses", d(w.getRealisasiProses()));
		j.put("revisi", w.getRevisi() == null ? 0 : w.getRevisi());
		// getBulanN() sengaja dipakai (bukan field): item lama yang hanya punya hargaTotal
		// tetap tampil terbagi rata dua belas bulan, persis seperti layar ZK.
		JSONArray bulan = new JSONArray();
		bulan.put(d(w.getBulan1())).put(d(w.getBulan2())).put(d(w.getBulan3())).put(d(w.getBulan4()))
				.put(d(w.getBulan5())).put(d(w.getBulan6())).put(d(w.getBulan7())).put(d(w.getBulan8()))
				.put(d(w.getBulan9())).put(d(w.getBulan10())).put(d(w.getBulan11())).put(d(w.getBulan12()));
		j.put("bulan", bulan);
		try {
			j.put("akunId", w.getAkun() == null ? JSONObject.NULL : w.getAkun().getId());
			j.put("akunLabel", w.getAkun() == null ? ""
					: ((w.getAkun().getKode() == null ? "" : w.getAkun().getKode()) + " - "
							+ (w.getAkun().getNama() == null ? "" : w.getAkun().getNama())));
		} catch (Exception e) {
			j.put("akunId", JSONObject.NULL);
			j.put("akunLabel", "");
		}
		try {
			j.put("satkerId", w.getSatuanKerja() == null ? JSONObject.NULL : w.getSatuanKerja().getId());
			j.put("satkerNama", w.getSatuanKerja() == null ? "" : w.getSatuanKerja().getNama());
		} catch (Exception e) {
			j.put("satkerId", JSONObject.NULL);
			j.put("satkerNama", "");
		}
		try {
			j.put("sumberDanaId", w.getSumberDana() == null ? JSONObject.NULL : w.getSumberDana().getId());
		} catch (Exception e) {
			j.put("sumberDanaId", JSONObject.NULL);
		}
		j.put("aktif", w.getAktif() == null ? true : w.getAktif().booleanValue());
		return j;
	}

	/**
	 * Pohon item anggaran satu revisi + ringkasan dua belas bulan. Klien menyusun hierarkinya dari
	 * {@code parentId} (baris yang induknya tidak ada di daftar diperlakukan sebagai akar).
	 */
	public static void itemList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		int revisi = request == null ? 1 : request.optInt("revisi", 1);
		long satkerId = request == null ? 0 : request.optLong("satkerId", 0);
		long sumberDanaId = request == null ? 0 : request.optLong("sumberDanaId", 0);
		boolean termasukAnak = request != null && request.optBoolean("termasukAnakSatker", true);
		boolean termasukNonAktif = request != null && request.optBoolean("termasukNonAktif", false);
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		if (tahun <= 0) {
			tolak(hasil, "Tahun Anggaran belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Set<Long> satkerIds = satkerDanTurunan(session, satkerId, termasukAnak);
			List<Workspace> items = ambilItem(session, tahun, satkerIds, sumberDanaId, revisi, termasukNonAktif);
			JSONArray arr = new JSONArray();
			double[] totalBulan = new double[12];
			double totalPagu = 0.0;
			for (Iterator<Workspace> it = items.iterator(); it.hasNext();) {
				Workspace w = it.next();
				if (!cari.isEmpty()) {
					String teks = ((w.getKode() == null ? "" : w.getKode()) + " "
							+ (w.getNama() == null ? "" : w.getNama())).toLowerCase();
					if (teks.indexOf(cari) < 0) {
						continue;
					}
				}
				JSONObject j = itemJson(w);
				arr.put(j);
				// Ringkasan memakai baris AKAR saja supaya tidak berlipat: induk sudah berisi
				// jumlah seluruh anaknya (lihat aturan agregat di JavaDoc kelas ini).
				boolean akar = w.getParentId() == null || w.getParentId().longValue() <= 0;
				if (akar) {
					JSONArray b = j.getJSONArray("bulan");
					for (int i = 0; i < 12; i++) {
						totalBulan[i] += b.getDouble(i);
					}
					totalPagu += d(w.getHargaTotal());
				}
			}
			JSONArray ringkasBulan = new JSONArray();
			for (int i = 0; i < 12; i++) {
				ringkasBulan.put(totalBulan[i]);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("ringkasanBulan", ringkasBulan);
			hasil.put("totalPagu", totalPagu);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ tulis item

	/** Hitung ulang agregat induk ke atas: bulanN induk = jumlah bulanN anak, total = jumlah 12 bulan. */
	private static void hitungUlangInduk(Session session, Long parentId) {
		Long kini = parentId;
		int pagar = 0;
		// id induk BOLEH negatif: baris akar satuan kerja buatan ZK (checkRootSatuanKerja)
		// memakai id/penunjuk negatif besar, dan layar ZK tetap memperbarui agregatnya.
		while (kini != null && kini.longValue() != 0 && pagar++ < 50) {
			Workspace induk = (Workspace) session.get(Workspace.class, kini);
			if (induk == null) {
				return;
			}
			Object[] jumlah = (Object[]) session
					.createQuery("select sum(w.bulan1), sum(w.bulan2), sum(w.bulan3), sum(w.bulan4),"
							+ " sum(w.bulan5), sum(w.bulan6), sum(w.bulan7), sum(w.bulan8), sum(w.bulan9),"
							+ " sum(w.bulan10), sum(w.bulan11), sum(w.bulan12) from Workspace w"
							+ " where w.parentId = :induk and w.id <> :induk" + klausaAktif("w"))
					.setLong("induk", kini.longValue()).uniqueResult();
			double[] b = new double[12];
			double total = 0.0;
			for (int i = 0; i < 12; i++) {
				b[i] = jumlah == null || jumlah[i] == null ? 0.0 : ((Number) jumlah[i]).doubleValue();
				total += b[i];
			}
			induk.setBulan1(Double.valueOf(b[0]));
			induk.setBulan2(Double.valueOf(b[1]));
			induk.setBulan3(Double.valueOf(b[2]));
			induk.setBulan4(Double.valueOf(b[3]));
			induk.setBulan5(Double.valueOf(b[4]));
			induk.setBulan6(Double.valueOf(b[5]));
			induk.setBulan7(Double.valueOf(b[6]));
			induk.setBulan8(Double.valueOf(b[7]));
			induk.setBulan9(Double.valueOf(b[8]));
			induk.setBulan10(Double.valueOf(b[9]));
			induk.setBulan11(Double.valueOf(b[10]));
			induk.setBulan12(Double.valueOf(b[11]));
			induk.setHargaTotal(Double.valueOf(total));
			session.saveOrUpdate(induk);
			kini = induk.getParentId();
		}
	}

	/**
	 * Tambah/ubah satu item anggaran. Nilai dua belas bulan yang dikirim klien menjadi sumber
	 * kebenaran untuk item DAUN; {@code hargaTotal} selalu dihitung ulang dari jumlah bulan agar
	 * tidak pernah berbeda dengan rinciannya, lalu agregat induk diperbarui sampai akar.
	 */
	public static void itemSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah item anggaran."
					: "Anda tidak memiliki hak mengubah item anggaran.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama item anggaran belum diisi.");
			return;
		}
		int tahun = request.optInt("tahun", 0);
		int revisi = request.optInt("revisi", 1);
		long satkerId = request.optLong("satkerId", 0);
		if (baru && (tahun <= 0 || satkerId <= 0)) {
			tolak(hasil, "Tahun Anggaran dan Satuan Kerja wajib dipilih untuk item baru.");
			return;
		}
		if (revisi == REVISI_KERANGKA) {
			tolak(hasil, "Revisi kerangka tidak boleh disunting dari layar ini.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Workspace w = baru ? new Workspace() : (Workspace) session.get(Workspace.class, Long.valueOf(id));
			if (w == null) {
				tolak(hasil, "Item anggaran tidak ditemukan (mungkin sudah dihapus pengguna lain).");
				return;
			}
			Long indukLama = baru ? null : w.getParentId();
			long parentId = request.optLong("parentId", 0);
			if (!baru && parentId == id) {
				tolak(hasil, "Induk tidak boleh item itu sendiri.");
				return;
			}
			if (parentId > 0) {
				Workspace induk = (Workspace) session.get(Workspace.class, Long.valueOf(parentId));
				if (induk == null) {
					tolak(hasil, "Item induk tidak ditemukan.");
					return;
				}
				if (!baru && keturunanDari(session, parentId, id)) {
					tolak(hasil, "Induk tidak boleh item turunannya sendiri (hierarki akan melingkar).");
					return;
				}
				w.setDeep(Integer.valueOf((induk.getDeep() == null ? 0 : induk.getDeep().intValue()) + 1));
			} else {
				w.setDeep(Integer.valueOf(0));
			}
			w.setParentId(Long.valueOf(parentId));
			w.setKode(request.optString("kode", "").trim());
			w.setNama(nama);
			w.setKeterangan(request.optString("keterangan", "").trim());
			w.setQty(Double.valueOf(request.optDouble("qty", 1.0)));
			w.setSatuanVolume(request.optString("satuanVolume", "").trim());
			w.setHargaSatuan(Double.valueOf(request.optDouble("hargaSatuan", 0.0)));

			JSONArray bulan = request.optJSONArray("bulan");
			double total = 0.0;
			double[] nilai = new double[12];
			for (int i = 0; i < 12; i++) {
				nilai[i] = bulan == null ? 0.0 : bulan.optDouble(i, 0.0);
				total += nilai[i];
			}
			w.setBulan1(Double.valueOf(nilai[0]));
			w.setBulan2(Double.valueOf(nilai[1]));
			w.setBulan3(Double.valueOf(nilai[2]));
			w.setBulan4(Double.valueOf(nilai[3]));
			w.setBulan5(Double.valueOf(nilai[4]));
			w.setBulan6(Double.valueOf(nilai[5]));
			w.setBulan7(Double.valueOf(nilai[6]));
			w.setBulan8(Double.valueOf(nilai[7]));
			w.setBulan9(Double.valueOf(nilai[8]));
			w.setBulan10(Double.valueOf(nilai[9]));
			w.setBulan11(Double.valueOf(nilai[10]));
			w.setBulan12(Double.valueOf(nilai[11]));
			w.setHargaTotal(Double.valueOf(total));

			if (baru) {
				w.setTahunWorkspace(Integer.valueOf(tahun));
				w.setRevisi(Integer.valueOf(revisi));
				w.setSatuanKerja((SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(satkerId)));
				long sdId = request.optLong("sumberDanaId", 0);
				w.setSumberDana(sdId > 0 ? (SumberDana) session.get(SumberDana.class, Long.valueOf(sdId)) : null);
				w.setRealisasiTotal(Double.valueOf(0));
				w.setRealisasiProses(Double.valueOf(0));
				w.setAktif(Boolean.TRUE);
			}
			long akunId = request.optLong("akunId", 0);
			w.setAkun(akunId > 0
					? (ais.database.model.akunting.Akun) session.get(ais.database.model.akunting.Akun.class,
							Long.valueOf(akunId))
					: null);
			if (request.has("aktif")) {
				w.setAktifManual(Boolean.valueOf(request.optBoolean("aktif", true)));
				w.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			}
			if (tbmuser != null) {
				w.setOleh(tbmuser.getUserNama());
				w.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(w);
			session.flush();
			// Induk lama ikut dihitung ulang bila item dipindah, supaya jumlahnya ikut turun.
			hitungUlangInduk(session, w.getParentId());
			if (indukLama != null && !indukLama.equals(w.getParentId())) {
				hitungUlangInduk(session, indukLama);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", w.getId());
			hasil.put("message", baru ? "Item anggaran berhasil ditambahkan." : "Item anggaran berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Item anggaran belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** true bila {@code calonIndukId} berada di bawah {@code id} (cegah hierarki melingkar). */
	private static boolean keturunanDari(Session session, long calonIndukId, long id) {
		Long kini = Long.valueOf(calonIndukId);
		int pagar = 0;
		while (kini != null && kini.longValue() != 0 && pagar++ < 50) {
			if (kini.longValue() == id) {
				return true;
			}
			Workspace w = (Workspace) session.get(Workspace.class, kini);
			if (w == null) {
				return false;
			}
			kini = w.getParentId();
		}
		return false;
	}

	/** Hapus satu item anggaran; ditolak bila masih punya turunan atau sudah dipakai realisasi. */
	public static void itemHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus item anggaran.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id <= 0) {
			tolak(hasil, "Item yang dihapus belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Workspace w = (Workspace) session.get(Workspace.class, Long.valueOf(id));
			if (w == null) {
				tolak(hasil, "Item anggaran tidak ditemukan.");
				return;
			}
			Number anak = (Number) session
					.createQuery("select count(w.id) from Workspace w where w.parentId = :id and w.id <> :id")
					.setLong("id", id).uniqueResult();
			if (anak != null && anak.longValue() > 0) {
				tolak(hasil, "Item ini masih punya " + anak.longValue()
						+ " turunan. Hapus atau pindahkan turunannya lebih dulu.");
				return;
			}
			Number dipakai = (Number) session
					.createQuery("select count(p.id) from PenggunaanAnggaran p where p.workspace.id = :id")
					.setLong("id", id).uniqueResult();
			if (dipakai != null && dipakai.longValue() > 0) {
				tolak(hasil, "Item ini sudah dipakai " + dipakai.longValue()
						+ " transaksi penggunaan anggaran sehingga tidak boleh dihapus.");
				return;
			}
			Long induk = w.getParentId();
			session.beginTransaction();
			session.delete(w);
			session.flush();
			hitungUlangInduk(session, induk);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Item \"" + w.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Item anggaran tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Buat revisi baru: menyalin SELURUH pohon revisi tertinggi menjadi revisi berikutnya
	 * (padanan tombol "Buat Revisi Baru" pada layar ZK). Nilai realisasi pada salinan dinolkan
	 * karena realisasi melekat pada revisi tempat transaksinya dicatat.
	 */
	public static void revisiBaru(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "create")) {
			tolak(hasil, "Anda tidak memiliki hak membuat revisi anggaran.");
			return;
		}
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		long satkerId = request == null ? 0 : request.optLong("satkerId", 0);
		long sumberDanaId = request == null ? 0 : request.optLong("sumberDanaId", 0);
		if (tahun <= 0 || satkerId <= 0) {
			tolak(hasil, "Tahun Anggaran dan Satuan Kerja wajib dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Set<Long> satkerIds = new HashSet<Long>();
			satkerIds.add(Long.valueOf(satkerId));
			Number maks = (Number) session
					.createQuery("select max(w.revisi) from Workspace w where w.tahunWorkspace = :tahun"
							+ " and w.satuanKerja.id = :satker and w.revisi <> :kerangka")
					.setInteger("tahun", tahun).setLong("satker", satkerId)
					.setInteger("kerangka", REVISI_KERANGKA).uniqueResult();
			if (maks == null) {
				tolak(hasil, "Belum ada revisi yang bisa disalin untuk tahun dan satuan kerja ini."
						+ " Tambahkan item anggaran lebih dulu.");
				return;
			}
			int revisiLama = maks.intValue();
			int revisiBaru = revisiLama + 1;
			List<Workspace> sumber = ambilItem(session, tahun, satkerIds, sumberDanaId, revisiLama, false);
			if (sumber.isEmpty()) {
				tolak(hasil, "Revisi " + revisiLama + " tidak punya item untuk disalin.");
				return;
			}
			session.beginTransaction();
			Map<Long, Long> petaId = new HashMap<Long, Long>();
			// Dua lintasan: salin dulu seluruh baris (induk sementara 0), baru tautkan induknya
			// setelah semua id baru diketahui -- aman untuk urutan pohon apa pun.
			List<Workspace> salinan = new ArrayList<Workspace>();
			for (Iterator<Workspace> it = sumber.iterator(); it.hasNext();) {
				Workspace asal = it.next();
				Workspace baru = new Workspace();
				baru.setKode(asal.getKode());
				baru.setNama(asal.getNama());
				baru.setKeterangan(asal.getKeterangan());
				baru.setQty(asal.getQty());
				baru.setSatuanVolume(asal.getSatuanVolume());
				baru.setHargaSatuan(asal.getHargaSatuan());
				baru.setHargaTotal(asal.getHargaTotal());
				baru.setDeep(asal.getDeep());
				baru.setBulan1(asal.getBulan1());
				baru.setBulan2(asal.getBulan2());
				baru.setBulan3(asal.getBulan3());
				baru.setBulan4(asal.getBulan4());
				baru.setBulan5(asal.getBulan5());
				baru.setBulan6(asal.getBulan6());
				baru.setBulan7(asal.getBulan7());
				baru.setBulan8(asal.getBulan8());
				baru.setBulan9(asal.getBulan9());
				baru.setBulan10(asal.getBulan10());
				baru.setBulan11(asal.getBulan11());
				baru.setBulan12(asal.getBulan12());
				baru.setTahunWorkspace(Integer.valueOf(tahun));
				baru.setRevisi(Integer.valueOf(revisiBaru));
				baru.setSatuanKerja(asal.getSatuanKerja());
				baru.setSumberDana(asal.getSumberDana());
				baru.setAkun(asal.getAkun());
				baru.setAktif(Boolean.TRUE);
				baru.setRealisasiTotal(Double.valueOf(0));
				baru.setRealisasiProses(Double.valueOf(0));
				baru.setParentId(Long.valueOf(0));
				baru.setCopyForm(asal.getId());
				baru.setMerupakanHasilCopy(Boolean.TRUE);
				if (tbmuser != null) {
					baru.setOleh(tbmuser.getUserNama());
					baru.setOlehId(tbmuser.getUserId());
				}
				session.save(baru);
				session.flush();
				petaId.put(asal.getId(), baru.getId());
				salinan.add(baru);
			}
			int i = 0;
			for (Iterator<Workspace> it = sumber.iterator(); it.hasNext(); i++) {
				Workspace asal = it.next();
				Workspace baru = salinan.get(i);
				Long indukBaru = asal.getParentId() == null ? null : petaId.get(asal.getParentId());
				baru.setParentId(indukBaru == null ? Long.valueOf(0) : indukBaru);
				session.saveOrUpdate(baru);
			}
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("revisi", revisiBaru);
			hasil.put("jumlahItem", salinan.size());
			hasil.put("message", "Revisi " + revisiBaru + " dibuat dari revisi " + revisiLama + " ("
					+ salinan.size() + " item).");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Revisi baru belum dapat dibuat: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ realisasi

	private static int bulanDari(java.util.Date waktu) {
		if (waktu == null) {
			return -1;
		}
		Calendar c = Calendar.getInstance();
		c.setTime(waktu);
		return c.get(Calendar.MONTH); // 0..11
	}

	/**
	 * Rekap pagu vs realisasi per bulan untuk satu revisi (padanan {@code realisasi_bulanan.zul}).
	 *
	 * <p>Realisasi diambil dari {@code PenggunaanAnggaran} yang menunjuk item bersangkutan dan
	 * LOLOS {@code getAktif()} object &mdash; transaksi yang dokumen sumbernya ditolak/dibatalkan
	 * otomatis tidak terhitung, sama seperti layar ZK.</p>
	 */
	@SuppressWarnings("unchecked")
	public static void realisasiList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		int revisi = request == null ? 1 : request.optInt("revisi", 1);
		long satkerId = request == null ? 0 : request.optLong("satkerId", 0);
		long sumberDanaId = request == null ? 0 : request.optLong("sumberDanaId", 0);
		boolean termasukAnak = request != null && request.optBoolean("termasukAnakSatker", true);
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		if (tahun <= 0) {
			tolak(hasil, "Tahun Anggaran belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Set<Long> satkerIds = satkerDanTurunan(session, satkerId, termasukAnak);
			List<Workspace> items = ambilItem(session, tahun, satkerIds, sumberDanaId, revisi, false);
			Map<Long, double[]> realisasiPerItem = new HashMap<Long, double[]>();
			Map<Long, Integer> jumlahTransaksi = new HashMap<Long, Integer>();
			List<Long> ids = new ArrayList<Long>();
			for (Iterator<Workspace> it = items.iterator(); it.hasNext();) {
				ids.add(it.next().getId());
			}
			int dilewatiTidakAktif = 0;
			if (!ids.isEmpty()) {
				List<PenggunaanAnggaran> pas = session
						.createQuery("from PenggunaanAnggaran p where p.workspace.id in (:ids)")
						.setParameterList("ids", ids).list();
				for (Iterator<PenggunaanAnggaran> it = pas.iterator(); it.hasNext();) {
					PenggunaanAnggaran pa = it.next();
					if (pa == null) {
						continue;
					}
					boolean aktif = false;
					try {
						aktif = Boolean.TRUE.equals(pa.getAktif());
					} catch (Exception e) {
						aktif = false;
					}
					if (!aktif) {
						dilewatiTidakAktif++;
						continue;
					}
					Long wId = null;
					try {
						wId = pa.getWorkspace() == null ? null : pa.getWorkspace().getId();
					} catch (Exception e) {
						wId = null;
					}
					if (wId == null) {
						continue;
					}
					double[] b = realisasiPerItem.get(wId);
					if (b == null) {
						b = new double[12];
						realisasiPerItem.put(wId, b);
					}
					int bln = bulanDari(pa.getWaktu());
					double nilai = d(pa.getNilai());
					if (bln >= 0 && bln < 12) {
						b[bln] += nilai;
					}
					Integer n = jumlahTransaksi.get(wId);
					jumlahTransaksi.put(wId, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
				}
			}

			JSONArray arr = new JSONArray();
			double[] totalPaguBulan = new double[12];
			double[] totalRealisasiBulan = new double[12];
			for (Iterator<Workspace> it = items.iterator(); it.hasNext();) {
				Workspace w = it.next();
				if (!cari.isEmpty()) {
					String teks = ((w.getKode() == null ? "" : w.getKode()) + " "
							+ (w.getNama() == null ? "" : w.getNama())).toLowerCase();
					if (teks.indexOf(cari) < 0) {
						continue;
					}
				}
				JSONObject j = itemJson(w);
				double[] real = realisasiPerItem.get(w.getId());
				JSONArray realisasiBulan = new JSONArray();
				double totalReal = 0.0;
				for (int i = 0; i < 12; i++) {
					double v = real == null ? 0.0 : real[i];
					realisasiBulan.put(v);
					totalReal += v;
				}
				double pagu = d(w.getHargaTotal());
				j.put("realisasiBulan", realisasiBulan);
				j.put("realisasi", totalReal);
				j.put("sisa", pagu - totalReal);
				j.put("persen", pagu == 0.0 ? 0.0 : (totalReal / pagu) * 100.0);
				Integer n = jumlahTransaksi.get(w.getId());
				j.put("jumlahTransaksi", n == null ? 0 : n.intValue());
				arr.put(j);

				boolean akar = w.getParentId() == null || w.getParentId().longValue() <= 0;
				if (akar) {
					JSONArray b = j.getJSONArray("bulan");
					for (int i = 0; i < 12; i++) {
						totalPaguBulan[i] += b.getDouble(i);
					}
				}
				// Realisasi dijumlahkan dari SELURUH item (bukan hanya akar): angka realisasi
				// tidak diagregasi ke induk seperti pagu, melainkan melekat pada item terpakai.
				for (int i = 0; i < 12; i++) {
					totalRealisasiBulan[i] += real == null ? 0.0 : real[i];
				}
			}
			JSONArray paguBulanArr = new JSONArray();
			JSONArray realisasiBulanArr = new JSONArray();
			double totalPagu = 0.0;
			double totalRealisasi = 0.0;
			for (int i = 0; i < 12; i++) {
				paguBulanArr.put(totalPaguBulan[i]);
				realisasiBulanArr.put(totalRealisasiBulan[i]);
				totalPagu += totalPaguBulan[i];
				totalRealisasi += totalRealisasiBulan[i];
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("paguBulan", paguBulanArr);
			hasil.put("realisasiBulan", realisasiBulanArr);
			hasil.put("totalPagu", totalPagu);
			hasil.put("totalRealisasi", totalRealisasi);
			hasil.put("totalSisa", totalPagu - totalRealisasi);
			hasil.put("dilewatiTidakAktif", dilewatiTidakAktif);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ penggunaan anggaran

	private static final String FORMAT_WAKTU = "yyyy-MM-dd HH:mm:ss";
	private static final String FORMAT_TANGGAL = "yyyy-MM-dd";

	private static java.util.Date uraiWaktu(String teks) {
		if (teks == null || teks.trim().isEmpty()) {
			return null;
		}
		String t = teks.trim();
		try {
			return new SimpleDateFormat(t.length() > 10 ? FORMAT_WAKTU : FORMAT_TANGGAL).parse(t);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Daftar transaksi pemakaian anggaran (padanan {@code penggunaan_anggaran.zul}). Kolom
	 * {@code aktif} memakai logika object, sehingga baris yang dokumen sumbernya ditolak terlihat
	 * jelas tidak terhitung walaupun kolom di basis data masih {@code true}.
	 */
	@SuppressWarnings("unchecked")
	public static void penggunaanList(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long workspaceId = request == null ? 0 : request.optLong("workspaceId", 0);
		int tahun = request == null ? 0 : request.optInt("tahun", 0);
		long satkerId = request == null ? 0 : request.optLong("satkerId", 0);
		int revisi = request == null ? 0 : request.optInt("revisi", 0);
		String cari = request == null ? "" : request.optString("cari", "").trim().toLowerCase();
		int batas = request == null ? 300 : request.optInt("limit", 300);
		if (batas <= 0 || batas > 2000) {
			batas = 300;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder hql = new StringBuilder("from PenggunaanAnggaran p where 1=1");
			if (workspaceId > 0) {
				hql.append(" and p.workspace.id = :ws");
			} else {
				if (tahun > 0) {
					hql.append(" and p.workspace.tahunWorkspace = :tahun");
				}
				if (satkerId > 0) {
					hql.append(" and p.workspace.satuanKerja.id = :satker");
				}
				if (revisi != 0) {
					hql.append(" and p.workspace.revisi = :revisi");
				}
			}
			hql.append(" order by p.waktu desc, p.id desc");
			org.hibernate.Query q = session.createQuery(hql.toString());
			if (workspaceId > 0) {
				q.setLong("ws", workspaceId);
			} else {
				if (tahun > 0) {
					q.setInteger("tahun", tahun);
				}
				if (satkerId > 0) {
					q.setLong("satker", satkerId);
				}
				if (revisi != 0) {
					q.setInteger("revisi", revisi);
				}
			}
			q.setMaxResults(batas);
			List<PenggunaanAnggaran> list = q.list();
			SimpleDateFormat fmt = new SimpleDateFormat(FORMAT_WAKTU);
			JSONArray arr = new JSONArray();
			double totalAktif = 0.0;
			for (Iterator<PenggunaanAnggaran> it = list.iterator(); it.hasNext();) {
				PenggunaanAnggaran p = it.next();
				if (p == null) {
					continue;
				}
				String nama = p.getNama() == null ? "" : p.getNama();
				String kode = p.getKode() == null ? "" : p.getKode();
				if (!cari.isEmpty() && (nama + " " + kode).toLowerCase().indexOf(cari) < 0) {
					continue;
				}
				JSONObject j = new JSONObject();
				j.put("id", p.getId());
				j.put("kode", kode);
				j.put("ref", p.getRef() == null ? "" : p.getRef());
				j.put("nama", nama);
				j.put("keterangan", p.getKeterangan() == null ? "" : p.getKeterangan());
				j.put("nilai", d(p.getNilai()));
				j.put("waktu", p.getWaktu() == null ? "" : fmt.format(p.getWaktu()));
				boolean aktif = false;
				try {
					aktif = Boolean.TRUE.equals(p.getAktif());
				} catch (Exception e) {
					aktif = false;
				}
				j.put("aktif", aktif);
				j.put("sumber", sumberDokumen(p));
				try {
					j.put("workspaceId", p.getWorkspace() == null ? JSONObject.NULL : p.getWorkspace().getId());
					j.put("workspaceLabel", p.getWorkspace() == null ? ""
							: ((p.getWorkspace().getKode() == null ? "" : p.getWorkspace().getKode()) + " - "
									+ (p.getWorkspace().getNama() == null ? "" : p.getWorkspace().getNama())));
				} catch (Exception e) {
					j.put("workspaceId", JSONObject.NULL);
					j.put("workspaceLabel", "");
				}
				if (aktif) {
					totalAktif += d(p.getNilai());
				}
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalAktif", totalAktif);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dokumen asal transaksi -- membantu pengguna tahu angka realisasi datang dari mana. */
	private static String sumberDokumen(PenggunaanAnggaran p) {
		try {
			if (p.getUangMuka() != null) {
				return "Uang Muka";
			}
			if (p.getPermintaanPengadaanMasterAssetDetail() != null) {
				return "Permintaan Pengadaan";
			}
			if (p.getSaldoAwalMasterAssetDetail() != null) {
				return "Saldo Awal Aset";
			}
			if (p.getPembayaranGaji() != null) {
				return "Pembayaran Gaji";
			}
			if (p.getKasKecil() != null) {
				return "Kas Kecil";
			}
			if (p.getKasBesar() != null) {
				return "Kas Besar";
			}
			if (p.getPertangungjawaban() != null) {
				return "Pertanggungjawaban";
			}
			if (p.getGrupTransaksi() != null) {
				return "Jurnal";
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnggaranApiHelper.sumberDokumen");
		}
		return "Entri Manual";
	}

	/** Simpan entri penggunaan anggaran MANUAL (tanpa dokumen sumber). */
	public static void penggunaanSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id <= 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak menambah penggunaan anggaran."
					: "Anda tidak memiliki hak mengubah penggunaan anggaran.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama/uraian penggunaan belum diisi.");
			return;
		}
		double nilai = request.optDouble("nilai", 0);
		if (nilai <= 0) {
			tolak(hasil, "Nilai penggunaan harus lebih dari 0.");
			return;
		}
		long workspaceId = request.optLong("workspaceId", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenggunaanAnggaran p = baru ? new PenggunaanAnggaran()
					: (PenggunaanAnggaran) session.get(PenggunaanAnggaran.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Data penggunaan anggaran tidak ditemukan.");
				return;
			}
			if (baru || workspaceId > 0) {
				if (workspaceId <= 0) {
					tolak(hasil, "Item anggaran belum dipilih.");
					return;
				}
				Workspace w = (Workspace) session.get(Workspace.class, Long.valueOf(workspaceId));
				if (w == null) {
					tolak(hasil, "Item anggaran tidak ditemukan.");
					return;
				}
				p.setWorkspace(w);
			}
			if (!baru && sumberDokumen(p).equals("Entri Manual") == false) {
				tolak(hasil, "Baris ini berasal dari dokumen " + sumberDokumen(p)
						+ " sehingga hanya boleh diubah dari dokumen asalnya.");
				return;
			}
			p.setKode(request.optString("kode", "").trim());
			// Kolom `ref` WAJIB terisi (NOT NULL di basis data) dan menjadi kunci dedup
			// baris milik dokumen (lihat PenggunaanAnggaran.refData: "<id>_<TIPE>").
			// Entri manual tidak punya dokumen sumber, jadi dibuatkan ref sendiri sekali
			// saat dibuat dan dipertahankan saat diubah.
			String ref = request.optString("ref", "").trim();
			if (ref.isEmpty()) {
				ref = p.getRef() == null ? "" : p.getRef().trim();
			}
			if (ref.isEmpty()) {
				ref = System.currentTimeMillis() + "_MANUAL";
			}
			p.setRef(ref);
			p.setNama(nama);
			p.setKeterangan(request.optString("keterangan", "").trim());
			p.setNilai(Double.valueOf(nilai));
			java.util.Date waktu = uraiWaktu(request.optString("waktu", ""));
			p.setWaktu(waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu);
			p.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			if (tbmuser != null) {
				p.setOleh(tbmuser.getUserNama());
				p.setOlehId(tbmuser.getUserId());
			}
			session.beginTransaction();
			session.saveOrUpdate(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", p.getId());
			hasil.put("message", baru ? "Penggunaan anggaran berhasil dicatat."
					: "Penggunaan anggaran berhasil diperbarui.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Penggunaan anggaran belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus entri penggunaan anggaran MANUAL; baris milik dokumen lain tidak boleh dihapus di sini. */
	public static void penggunaanHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus penggunaan anggaran.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PenggunaanAnggaran p = id <= 0 ? null
					: (PenggunaanAnggaran) session.get(PenggunaanAnggaran.class, Long.valueOf(id));
			if (p == null) {
				tolak(hasil, "Data penggunaan anggaran tidak ditemukan.");
				return;
			}
			String sumber = sumberDokumen(p);
			if (!"Entri Manual".equals(sumber)) {
				tolak(hasil, "Baris ini berasal dari dokumen " + sumber
						+ " sehingga harus dibatalkan dari dokumen asalnya, bukan dihapus di sini.");
				return;
			}
			session.beginTransaction();
			session.delete(p);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Penggunaan anggaran \"" + p.getNama() + "\" dihapus.");
		} catch (Exception e) {
			batalkan(session);
			tolak(hasil, "Penggunaan anggaran tidak dapat dihapus karena masih berelasi dengan data lain.");
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher PosApi: seluruh aksi berawalan {@code anggaran_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("anggaran_konteks".equals(action)) {
			konteks(tbmuser, hasil);
			return true;
		}
		if ("anggaran_revisi_list".equals(action)) {
			revisiList(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_item_list".equals(action)) {
			itemList(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_item_simpan".equals(action)) {
			itemSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_item_hapus".equals(action)) {
			itemHapus(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_revisi_baru".equals(action)) {
			revisiBaru(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_realisasi_list".equals(action)) {
			realisasiList(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_penggunaan_list".equals(action)) {
			penggunaanList(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_penggunaan_simpan".equals(action)) {
			penggunaanSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("anggaran_penggunaan_hapus".equals(action)) {
			penggunaanHapus(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
