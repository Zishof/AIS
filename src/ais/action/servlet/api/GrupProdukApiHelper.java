package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.inventory.GrupProdukUtil;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.GrupProduk;

/**
 * <h3>API JSON Grup Produk (harga terpusat lintas toko) -- dipakai JSP e-Kantin dan POS
 * Desktop/Android.</h3>
 *
 * <p>Logika penyalinan harga SATU dengan layar ZK admin: {@link GrupProdukUtil} (per-baris
 * agar ter-audit Envers, lihat javadoc di sana). Seluruh handler self-guard menu key
 * {@code grup_produk} + aksi granular {@code EbisnisMenuKatalog.bolehAksi} -- fail-closed
 * ({@code grup_produk} terdaftar di {@code KUNCI_DEFAULT_NONAKTIF}): role existing TIDAK
 * mendadak bisa mengubah harga massal lintas outlet; admin menyalakannya per-role lewat
 * grid CRUD TbmroleAction.</p>
 */
public final class GrupProdukApiHelper {

	private GrupProdukApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	/** Menu key grup_produk tampil utk pemanggil? (Read-gate; pola ApotikApiHelper.bolehAksi,
	 *  TETAPI pemanggil anonim ditolak eksplisit -- jangan mengandalkan gate dispatcher.) */
	private static boolean bolehLihat(Tbmuser tbmuser) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optJSONObject("menu");
		return menu != null && menu.optBoolean("grup_produk", false);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (tbmuser == null) {
			return false;
		}
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(
				EbisnisMenuKatalog.urai(role.getEbisnisMenu()), "grup_produk", aksi);
	}

	/** Daftar grup + jumlah produk anggota. Param opsional: {@code cari} (ilike nama/kode),
	 *  {@code hanya_aktif} (default true). */
	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Grup Produk tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria kriteria = session.createCriteria(GrupProduk.class);
			if (request == null || request.optBoolean("hanya_aktif", true)) {
				kriteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			}
			String cari = request == null ? "" : request.optString("cari", "").trim();
			if (!cari.isEmpty()) {
				kriteria.add(Restrictions.or(
						Restrictions.ilike("nama", cari, MatchMode.ANYWHERE),
						Restrictions.ilike("kode", cari, MatchMode.ANYWHERE)));
			}
			kriteria.addOrder(Order.asc("nama"));
			@SuppressWarnings("unchecked")
			List<GrupProduk> daftar = kriteria.list();
			JSONArray arr = new JSONArray();
			for (GrupProduk g : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", g.getId());
				o.put("kode", g.getKode() == null ? "" : g.getKode());
				o.put("nama", g.getNama());
				o.put("keterangan", g.getKeterangan() == null ? "" : g.getKeterangan());
				o.put("harga_beli", g.getHargaBeli() == null ? JSONObject.NULL : g.getHargaBeli());
				o.put("harga_jual", g.getHargaJual() == null ? JSONObject.NULL : g.getHargaJual());
				// Toggle NULL (baris lama) dilaporkan mengikuti derivasi legacy yang sama
				// dgn logika penyalinan server, supaya UI menampilkan perilaku sebenarnya.
				o.put("ikut_hpp", GrupProdukUtil.ikutHpp(g));
				o.put("ikut_harga_jual", GrupProdukUtil.ikutHargaJual(g));
				String bahan = g.getBahanBaku();
				o.put("bahan_baku", bahan == null || bahan.trim().isEmpty()
						? new JSONArray() : new JSONArray(bahan));
				ais.database.model.koperasi.AturanDiskon ad = g.getAturanDiskon();
				o.put("aturan_diskon", ad == null ? JSONObject.NULL : ad.getId());
				o.put("aturan_diskon_nama", ad == null ? "" : ad.getNamaAturan());
				o.put("aktif", Boolean.TRUE.equals(g.getAktif()));
				o.put("jumlah_anggota", GrupProdukUtil.jumlahAnggota(session, g));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tambah ({@code id} kosong, aksi {@code create}) / ubah ({@code id} terisi, aksi
	 * {@code update}) grup, lalu salin harga terisi ke seluruh produk anggota dalam SATU
	 * transaksi. Respons memuat {@code diterapkan} = jumlah produk yang harganya berubah.
	 */
	public static void simpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Long id = (request == null || request.isNull("id") || (request.get("id") + "").trim().isEmpty())
				? null : Long.valueOf((request.get("id") + "").trim());
		if (!bolehAksi(tbmuser, id == null ? "create" : "update")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak "
					+ (id == null ? "menambah" : "mengubah") + " Grup Produk.");
			return;
		}
		String nama = request == null ? "" : request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama grup wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Number duplikat = (Number) session.createCriteria(GrupProduk.class)
					.setProjection(Projections.rowCount())
					.add(Restrictions.eq("nama", nama))
					.add(id == null ? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", id))
					.uniqueResult();
			if (duplikat != null && duplikat.intValue() > 0) {
				tolak(hasil, "Nama grup sudah terdaftar; gunakan nama lain.");
				return;
			}
			GrupProduk g;
			if (id != null) {
				g = (GrupProduk) session.get(GrupProduk.class, id);
				if (g == null) {
					tolak(hasil, "Grup Produk tidak ditemukan.");
					return;
				}
			} else {
				g = new GrupProduk();
			}
			g.setKode(request.optString("kode", "").trim());
			g.setNama(nama);
			g.setKeterangan(request.optString("keterangan", "").trim());
			// GERBANG UBAH HARGA (kebijakan per toko): grup menyalin harga ke SELURUH produk
			// anggota lintas outlet, jadi justru paling perlu dijaga. Diperiksa hanya bila
			// nilainya berubah; pemeriksaan memakai toko aktif pemanggil karena grup sendiri
			// bersifat lintas toko.
			Double hargaBeliBaru = request.isNull("harga_beli") ? null : Double.valueOf(request.getDouble("harga_beli"));
			Double hargaJualBaru = request.isNull("harga_jual") ? null : Double.valueOf(request.getDouble("harga_jual"));
			boolean hargaGrupBerubah =
					ais.action.master.inventory.HargaAksesUtil.berubah(g.getHargaBeli(),
							hargaBeliBaru == null ? 0.0 : hargaBeliBaru.doubleValue())
					|| ais.action.master.inventory.HargaAksesUtil.berubah(g.getHargaJual(),
							hargaJualBaru == null ? 0.0 : hargaJualBaru.doubleValue());
			if (hargaGrupBerubah) {
				Long tokoPemanggil = (tbmuser != null && tbmuser.getPedagang() != null
						&& tbmuser.getPedagang().getToko() != null)
								? tbmuser.getPedagang().getToko().getId()
								: tbmuser == null ? null : tbmuser.getTokoAktifMultiToko();
				if (!ais.action.master.inventory.HargaAksesUtil.bolehUbahHarga(session, tokoPemanggil, tbmuser)) {
					tolak(hasil, ais.action.master.inventory.HargaAksesUtil.pesanDitolak());
					return;
				}
			}
			g.setHargaBeli(hargaBeliBaru);
			g.setHargaJual(hargaJualBaru);
			// Field baru dikirim klien versi terbaru saja -- klien lama tidak mengirim
			// kuncinya sama sekali sehingga nilai tersimpan TIDAK tersentuh (kompat mundur).
			if (request.has("ikut_hpp")) {
				g.setIkutHpp(request.isNull("ikut_hpp") ? null
						: Boolean.valueOf(request.optBoolean("ikut_hpp", false)));
			}
			if (request.has("ikut_harga_jual")) {
				g.setIkutHargaJual(request.isNull("ikut_harga_jual") ? null
						: Boolean.valueOf(request.optBoolean("ikut_harga_jual", false)));
			}
			if (request.has("bahan_baku")) {
				JSONArray resep = request.isNull("bahan_baku") ? null : request.optJSONArray("bahan_baku");
				g.setBahanBaku(resep == null || resep.length() == 0 ? null : resep.toString());
			}
			if (request.has("aturan_diskon")) {
				g.setAturanDiskon(request.isNull("aturan_diskon") ? null
						: (ais.database.model.koperasi.AturanDiskon) session.get(
								ais.database.model.koperasi.AturanDiskon.class,
								Long.valueOf((request.get("aturan_diskon") + "").trim())));
			}
			if (!request.isNull("aktif")) {
				g.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			}
			if (tbmuser != null) {
				g.setOleh(tbmuser.getUserNama());
				g.setOlehId(tbmuser.getUserId());
			}

			session.beginTransaction();
			session.saveOrUpdate(g);
			// Set keanggotaan penuh (kunci "produk" berisi [{id}...]) -- pola replace ala
			// DiskonGrupHelper.simpan, tapi per-baris via session supaya ter-audit Envers.
			int anggotaDitambah = 0;
			int anggotaDilepas = 0;
			if (request.has("produk") && !request.isNull("produk")) {
				JSONArray target = request.optJSONArray("produk");
				java.util.Set<Long> targetId = new java.util.LinkedHashSet<Long>();
				for (int i = 0; target != null && i < target.length(); i++) {
					JSONObject x = target.optJSONObject(i);
					if (x != null && !x.isNull("id")) {
						targetId.add(Long.valueOf((x.get("id") + "").trim()));
					}
				}
				@SuppressWarnings("unchecked")
				List<ais.database.model.inventory.Produk> anggotaKini = session
						.createCriteria(ais.database.model.inventory.Produk.class)
						.add(Restrictions.eq("grupProduk", g)).list();
				for (ais.database.model.inventory.Produk p : anggotaKini) {
					if (!targetId.remove(p.getId())) {
						p.setGrupProduk(null);
						session.saveOrUpdate(p);
						anggotaDilepas++;
					}
				}
				for (Long pid : targetId) {
					ais.database.model.inventory.Produk p = (ais.database.model.inventory.Produk) session
							.get(ais.database.model.inventory.Produk.class, pid);
					if (p != null) {
						p.setGrupProduk(g);
						session.saveOrUpdate(p);
						anggotaDitambah++;
					}
				}
				session.flush();
			}
			int diterapkan = GrupProdukUtil.terapkanHargaKeAnggota(session, g);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", g.getId());
			hasil.put("diterapkan", diterapkan);
			hasil.put("anggota_ditambah", anggotaDitambah);
			hasil.put("anggota_dilepas", anggotaDilepas);
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "GrupProdukApiHelper.simpan rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Hapus grup -- DITOLAK bila masih punya produk anggota (lepas dulu anggotanya lewat layar
	 *  Produk); pola referential-guard yang sama dengan purge master lain. */
	public static void hapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Grup pengguna Anda tidak memiliki hak menghapus Grup Produk.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			GrupProduk g = (GrupProduk) session.get(GrupProduk.class, id);
			if (g == null) {
				tolak(hasil, "Grup Produk tidak ditemukan.");
				return;
			}
			int anggota = GrupProdukUtil.jumlahAnggota(session, g);
			if (anggota > 0) {
				tolak(hasil, "Grup masih memiliki " + anggota
						+ " produk anggota. Lepaskan dahulu produk dari grup ini (layar Produk), atau nonaktifkan grup.");
				return;
			}
			session.beginTransaction();
			session.delete(g);
			session.getTransaction().commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception eRollback) {
				ais.common.ErrorAuditUtil.record(eRollback, "GrupProdukApiHelper.hapus rollback");
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Daftar produk anggota satu grup (utk panel anggota + unduh Excel). Param: {@code id}. */
	public static void anggotaDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Grup Produk tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		Long id = (request == null || request.isNull("id")) ? null
				: Long.valueOf((request.get("id") + "").trim());
		if (id == null) {
			tolak(hasil, "Parameter id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.barcode,''), p.nama, COALESCE(t.nama,''), "
							+ "COALESCE(p.hargabeli,0), COALESCE(p.hargajual,0) "
							+ "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id = p.toko "
							+ "WHERE p.grup_produk = ? ORDER BY t.nama, p.nama");
			ps.setLong(1, id.longValue());
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject o = new JSONObject();
				o.put("id", rs.getLong(1));
				o.put("kode", rs.getString(2));
				o.put("barcode", rs.getString(3));
				o.put("nama", rs.getString(4));
				o.put("tokoNama", rs.getString(5));
				o.put("harga_beli", rs.getDouble(6));
				o.put("harga_jual", rs.getDouble(7));
				arr.put(o);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Cari produk LINTAS toko utk dipilih jadi anggota grup -- beda dari
	 * {@code diskon_grup_produk_cari} (per-toko): grup produk memang lintas outlet.
	 * Param: {@code keyword}.
	 */
	public static void cariProduk(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Grup Produk tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		String q = request == null ? "" : request.optString("keyword", "").trim().toLowerCase();
		// Filter opsional jenis item (mis. "BAHAN" utk picker resep grup) -- nilai
		// dibatasi whitelist supaya aman disisipkan (bukan dari input bebas).
		String jenisItem = request == null ? "" : request.optString("jenis_item", "").trim().toUpperCase();
		if (!"BAHAN".equals(jenisItem) && !"JUAL".equals(jenisItem) && !"EKSTRA".equals(jenisItem)) {
			jenisItem = "";
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.barcode,''), p.nama, COALESCE(t.nama,''), p.grup_produk "
							+ "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id = p.toko "
							+ "WHERE COALESCE(p.aktif,true) AND (?='' OR LOWER(COALESCE(p.kode,'')||' '||COALESCE(p.barcode,'')||' '||COALESCE(p.nama,'')) LIKE ?) "
							+ (jenisItem.isEmpty() ? ""
									: "AND UPPER(COALESCE(NULLIF(TRIM(p.jenis_item),''),'JUAL')) = '" + jenisItem + "' ")
							+ "ORDER BY p.nama, t.nama LIMIT 100");
			ps.setString(1, q);
			ps.setString(2, "%" + q + "%");
			java.sql.ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject o = new JSONObject();
				o.put("id", rs.getLong(1));
				o.put("kode", rs.getString(2));
				o.put("barcode", rs.getString(3));
				o.put("nama", rs.getString(4));
				o.put("tokoNama", rs.getString(5));
				long gid = rs.getLong(6);
				o.put("grup_produk", rs.wasNull() ? JSONObject.NULL : Long.valueOf(gid));
				arr.put(o);
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Resolve kode/barcode hasil unggah Excel jadi produk -- LINTAS toko dan SEMUA yang
	 * cocok ikut (satu kode di 90 outlet = 90 baris produk, memang itu tujuannya grup).
	 * Param: {@code kunci} (array string kode/barcode). Respons: {@code data} + {@code tidakDitemukan}.
	 */
	public static void resolveProduk(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehLihat(tbmuser)) {
			tolak(hasil, "Menu Grup Produk tidak diaktifkan untuk grup pengguna Anda.");
			return;
		}
		JSONArray keys = request == null ? null : request.optJSONArray("kunci");
		if (keys == null) {
			tolak(hasil, "Daftar kode/barcode tidak valid.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			java.sql.Connection conn = session.connection();
			java.sql.PreparedStatement ps = conn.prepareStatement(
					"SELECT p.id, COALESCE(p.kode,''), COALESCE(p.barcode,''), p.nama, COALESCE(t.nama,'') "
							+ "FROM koperasi.produk p LEFT JOIN koperasi.toko t ON t.id = p.toko "
							+ "WHERE p.kode = ? OR p.barcode = ? ORDER BY p.id");
			JSONArray found = new JSONArray();
			JSONArray missing = new JSONArray();
			java.util.Set<Long> seen = new java.util.LinkedHashSet<Long>();
			for (int i = 0; i < keys.length(); i++) {
				String k = String.valueOf(keys.get(i)).trim();
				if (k.isEmpty()) {
					continue;
				}
				ps.setString(1, k);
				ps.setString(2, k);
				java.sql.ResultSet rs = ps.executeQuery();
				boolean ada = false;
				while (rs.next()) {
					ada = true;
					if (seen.add(Long.valueOf(rs.getLong(1)))) {
						JSONObject o = new JSONObject();
						o.put("id", rs.getLong(1));
						o.put("kode", rs.getString(2));
						o.put("barcode", rs.getString(3));
						o.put("nama", rs.getString(4));
						o.put("tokoNama", rs.getString(5));
						found.put(o);
					}
				}
				rs.close();
				if (!ada) {
					missing.put(k);
				}
			}
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", found);
			hasil.put("tidakDitemukan", missing);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Dipakai dispatcher: setiap aksi berawalan {@code grup_produk_} diarahkan ke sini. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		// Alias: klien Flutter Desktop/Android (commit 1c9d7d7) memanggil grup_produk_list --
		// kontrak & bentuk respons sama persis dgn daftar().
		if ("grup_produk_daftar".equals(action) || "grup_produk_list".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_simpan".equals(action)) {
			simpan(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_hapus".equals(action)) {
			hapus(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_anggota_daftar".equals(action)) {
			anggotaDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_produk_cari".equals(action)) {
			cariProduk(tbmuser, request, hasil);
			return true;
		}
		if ("grup_produk_produk_resolve".equals(action)) {
			resolveProduk(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
