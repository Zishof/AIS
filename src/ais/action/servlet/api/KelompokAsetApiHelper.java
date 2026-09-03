package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
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
import ais.database.model.asset.KelompokAsset;

/**
 * API Kelompok Aset untuk POS Desktop/Android: membaca dan menyimpan pemetaan akun tiap Kelompok
 * Aset supaya layarnya bisa natif, tanpa membuka {@code kelompok_asset.zul} di browser.
 *
 * <p><b>Bentuk datanya.</b> Kolom akun disimpan sebagai teks {@code *_str} berisi JSON array
 * pasangan akun &amp; satuan kerja, bukan satu akun tunggal:</p>
 *
 * <pre>[ { "key": 8412..., "akun": 1234, "satuanKerja": 56 } ]</pre>
 *
 * <p>Artinya satu Kelompok Aset boleh memakai akun BERBEDA untuk satuan kerja yang berbeda. Nilai
 * bawaannya array kosong. Format ini sama persis dengan yang dibaca dan ditulis
 * {@code AssetUtil.reloadDataFormula} pada layar ZK, jadi kedua sisi tetap saling terbaca dan
 * perubahan dari salah satu sisi tidak merusak sisi lainnya.</p>
 *
 * <p><b>Kenapa Kelompok Aset saja, bukan Master Aset.</b> Getter akun pada {@code MasterAsset}
 * mengambil nilai KELOMPOK bila kelompoknya terisi, sehingga akun milik aset hanya terpakai saat
 * kelompoknya kosong &mdash; menyunting Master Aset diam-diam tidak berpengaruh pada keadaan yang
 * normal. Mana yang seharusnya menang adalah keputusan akuntansi, bukan keputusan teknis, dan
 * sedang menunggu jawaban pemilik; rinciannya di
 * {@code docs/pos/103-akun-master-aset-ditimpa-kelompok.md}. Kelompok Aset tidak punya persoalan
 * itu: ia justru sumber yang berwenang.</p>
 */
public final class KelompokAsetApiHelper {

	private KelompokAsetApiHelper() {
	}

	/**
	 * Bidang akun yang boleh disunting. Sengaja berupa daftar putih: nama kolom TIDAK pernah
	 * diambil dari permintaan, supaya klien tidak bisa mengarahkan penyimpanan ke kolom lain.
	 */
	private static final String[] BIDANG = { "pembelian", "penyusutan", "biaya", "hpp" };

	private static boolean bidangDikenal(String bidang) {
		for (int i = 0; i < BIDANG.length; i++) {
			if (BIDANG[i].equals(bidang)) {
				return true;
			}
		}
		return false;
	}

	/** Gerbang yang sama dengan master lain (lihat {@code JenisProdukApiHelper}). */
	private static boolean bolehKelola(Tbmuser tbmuser) {
		try {
			ais.database.model.inventory.Pedagang pedagang = tbmuser == null ? null : tbmuser.getPedagang();
			return pedagang == null || Boolean.TRUE.equals(pedagang.getSupervisor());
		} catch (Exception e) {
			return false;
		}
	}

	private static void tutup(Session session) {
		try {
			if (session != null && session.isOpen()) {
				session.close();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KelompokAsetApiHelper.tutup");
		}
	}

	private static JSONArray urai(String teks) {
		try {
			return (teks == null || teks.trim().isEmpty()) ? new JSONArray() : new JSONArray(teks);
		} catch (Exception e) {
			// Teks rusak diperlakukan sebagai kosong, BUKAN dilempar: satu baris rusak tidak boleh
			// membuat seluruh daftar Kelompok Aset gagal dimuat.
			return new JSONArray();
		}
	}

	private static Long angka(JSONObject o, String kunci) {
		if (o == null || !o.has(kunci) || o.isNull(kunci)) {
			return null;
		}
		try {
			return Long.valueOf(o.getLong(kunci));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Rangkai daftar id menjadi klausa {@code IN (...)}.
	 *
	 * <p>Nilainya sudah berupa {@link Long} hasil penguraian, bukan teks dari permintaan, jadi
	 * tidak ada jalan masuk injeksi. Dirangkai sendiri karena {@code createArrayOf} belum pernah
	 * dipakai di basis kode ini dan perilakunya bergantung pada pembungkus koneksi.</p>
	 */
	private static String klausaIn(Set<Long> id) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<Long> it = id.iterator(); it.hasNext();) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(it.next().longValue());
		}
		return sb.toString();
	}

	/**
	 * Daftar Kelompok Aset beserta keempat pemetaan akunnya, lengkap dengan kode/nama akun dan nama
	 * satuan kerja supaya layar tidak perlu memanggil balik satu per satu.
	 */
	public static void kelompokAsetList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int limit = Math.min(500, Math.max(1, request == null ? 200 : request.optInt("limit", 200)));

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT k.id, COALESCE(k.nama,''), COALESCE(k.keterangan,''),"
							+ " k.akun_transaksi_str, k.akun_penyusutan_str,"
							+ " k.akun_biaya_penyusutan_str, k.akun_beban_pokok_penjualan_str"
							+ " FROM asset.kelompok_asset k");
			if (!keyword.isEmpty()) {
				sql.append(" WHERE (k.nama ILIKE ? OR k.keterangan ILIKE ?)");
			}
			sql.append(" ORDER BY k.nama ASC LIMIT ?");

			PreparedStatement ps = conn.prepareStatement(sql.toString());
			int idx = 1;
			if (!keyword.isEmpty()) {
				String kw = "%" + keyword + "%";
				ps.setString(idx++, kw);
				ps.setString(idx++, kw);
			}
			ps.setInt(idx++, limit);
			ResultSet rs = ps.executeQuery();

			List<JSONObject> baris = new ArrayList<JSONObject>();
			Set<Long> idAkun = new HashSet<Long>();
			Set<Long> idSatker = new HashSet<Long>();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				String[] kolom = { rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7) };
				for (int b = 0; b < BIDANG.length; b++) {
					JSONArray arr = urai(kolom[b]);
					for (int i = 0; i < arr.length(); i++) {
						JSONObject e = arr.optJSONObject(i);
						Long a = angka(e, "akun");
						Long s = angka(e, "satuanKerja");
						if (a != null) {
							idAkun.add(a);
						}
						if (s != null) {
							idSatker.add(s);
						}
					}
					j.put(BIDANG[b], arr);
				}
				baris.add(j);
			}
			rs.close();
			ps.close();

			// Nama akun & satuan kerja diambil SEKALI untuk semua baris, bukan per entri.
			Map<Long, String[]> akun = petaAkun(conn, idAkun);
			Map<Long, String> satker = petaSatuanKerja(conn, idSatker);

			JSONArray daftar = new JSONArray();
			for (int n = 0; n < baris.size(); n++) {
				JSONObject j = baris.get(n);
				for (int b = 0; b < BIDANG.length; b++) {
					JSONArray arr = j.getJSONArray(BIDANG[b]);
					for (int i = 0; i < arr.length(); i++) {
						JSONObject e = arr.optJSONObject(i);
						if (e == null) {
							continue;
						}
						Long a = angka(e, "akun");
						String[] ka = a == null ? null : akun.get(a);
						e.put("kodeAkun", ka == null ? "" : ka[0]);
						e.put("namaAkun", ka == null ? "" : ka[1]);
						e.put("akunDaun", ka == null ? true : "1".equals(ka[2]));
						Long s = angka(e, "satuanKerja");
						String ns = s == null ? null : satker.get(s);
						e.put("namaSatuanKerja", ns == null ? "" : ns);
					}
				}
				daftar.put(j);
			}
			hasil.put("status", "00");
			hasil.put("daftar", daftar);
		} finally {
			tutup(session);
		}
	}

	/** id akun -&gt; {kode, nama, "1" bila akun DAUN}. */
	private static Map<Long, String[]> petaAkun(Connection conn, Set<Long> id) throws Exception {
		Map<Long, String[]> peta = new HashMap<Long, String[]>();
		if (id.isEmpty()) {
			return peta;
		}
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT a.id, COALESCE(a.kode,''), COALESCE(a.nama,''),"
				+ " NOT EXISTS (SELECT 1 FROM akunting.akun b WHERE b.parent = a.id)"
				+ " FROM akunting.akun a WHERE a.id IN (" + klausaIn(id) + ")");
		while (rs.next()) {
			peta.put(Long.valueOf(rs.getLong(1)),
					new String[] { rs.getString(2), rs.getString(3), rs.getBoolean(4) ? "1" : "0" });
		}
		rs.close();
		st.close();
		return peta;
	}

	private static Map<Long, String> petaSatuanKerja(Connection conn, Set<Long> id) throws Exception {
		Map<Long, String> peta = new HashMap<Long, String>();
		if (id.isEmpty()) {
			return peta;
		}
		Statement st = conn.createStatement();
		ResultSet rs = st.executeQuery("SELECT s.id, COALESCE(s.nama,'') FROM rab.satuan_kerja s"
				+ " WHERE s.id IN (" + klausaIn(id) + ")");
		while (rs.next()) {
			peta.put(Long.valueOf(rs.getLong(1)), rs.getString(2));
		}
		rs.close();
		st.close();
		return peta;
	}

	/**
	 * Simpan satu bidang akun milik satu Kelompok Aset.
	 *
	 * <p>Permintaan: {@code id}, {@code bidang} (pembelian|penyusutan|biaya|hpp), dan {@code baris}
	 * berupa array {@code {akun, satuanKerja}}. Baris yang kedua nilainya kosong dibuang.
	 * {@code key} dibangkitkan ulang di sini supaya klien tidak perlu mengarangnya.</p>
	 *
	 * <p>Akun yang BUKAN daun tetap disimpan tetapi dilaporkan lewat {@code peringatan}. Akun induk
	 * tidak pernah menampung transaksi, jadi memilihnya menghasilkan jurnal salah tempat &mdash;
	 * tetapi menolak simpan akan mengunci pengguna yang datanya sudah terlanjur begitu dan hanya
	 * ingin memperbaiki baris lain.</p>
	 */
	public static void kelompokAsetAkunSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (!bolehKelola(tbmuser)) {
			hasil.put("status", "91");
			hasil.put("description",
					"Hanya admin/manager atau supervisor toko yang dapat mengubah akun Kelompok Aset.");
			return;
		}
		Long id = ais.common.Common.angkaAtauNull(request, "id");
		if (id == null) {
			hasil.put("status", "91");
			hasil.put("description", "Kelompok Aset belum dipilih.");
			return;
		}
		String bidang = request.optString("bidang", "").trim();
		if (!bidangDikenal(bidang)) {
			hasil.put("status", "91");
			hasil.put("description", "Bidang akun tidak dikenal: " + bidang);
			return;
		}

		JSONArray masuk = request.optJSONArray("baris");
		if (masuk == null) {
			masuk = new JSONArray();
		}
		JSONArray simpan = new JSONArray();
		Set<Long> idAkun = new HashSet<Long>();
		for (int i = 0; i < masuk.length(); i++) {
			JSONObject e = masuk.optJSONObject(i);
			Long a = angka(e, "akun");
			Long s = angka(e, "satuanKerja");
			if (a == null && s == null) {
				continue; // baris kosong
			}
			JSONObject j = new JSONObject();
			j.put("key", Long.valueOf(Math.abs(ais.common.Common.randLong())));
			j.put("akun", a == null ? JSONObject.NULL : a);
			j.put("satuanKerja", s == null ? JSONObject.NULL : s);
			simpan.put(j);
			if (a != null) {
				idAkun.add(a);
			}
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			KelompokAsset k = (KelompokAsset) session.get(KelompokAsset.class, id);
			if (k == null) {
				hasil.put("status", "91");
				hasil.put("description", "Kelompok Aset tidak ditemukan.");
				return;
			}
			String teks = simpan.toString();
			if ("pembelian".equals(bidang)) {
				k.setAkunTransaksi(teks);
			} else if ("penyusutan".equals(bidang)) {
				k.setAkunPenyusutan(teks);
			} else if ("biaya".equals(bidang)) {
				k.setAkunBiayaPenyusutan(teks);
			} else {
				k.setAkunBebanPokokPenjualan(teks);
			}

			JSONArray peringatan = new JSONArray();
			Map<Long, String[]> akun = petaAkun(session.connection(), idAkun);
			for (Iterator<Long> it = idAkun.iterator(); it.hasNext();) {
				String[] ka = akun.get(it.next());
				if (ka != null && !"1".equals(ka[2])) {
					peringatan.put("Akun " + ka[0] + " - " + ka[1]
							+ " bukan akun daun; akun induk tidak menampung transaksi.");
				}
			}

			session.beginTransaction();
			session.saveOrUpdate(k);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", k.getId());
			hasil.put("jumlah", simpan.length());
			if (peringatan.length() > 0) {
				hasil.put("peringatan", peringatan);
			}
		} finally {
			tutup(session);
		}
	}
}
