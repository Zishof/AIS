package ais.action.servlet.api;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;

/**
 * <h3>Seed idempoten menu varian "POS Apotik" &amp; "POS eMedik" (LANGKAH 1.5 perintah awal).</h3>
 *
 * <p>Aturan yang ditegakkan (pemisahan wewenang DISENGAJA -- apoteker menjual obat, tenaga medis
 * melayani pasien; pembanding eMedik NestJS: apoteker memegang PRESCRIPTION READ+REVIEW tanpa
 * CREATE):</p>
 * <ul>
 *   <li>Grup "Apotik" (roleId {@code apotik}) -- dibuat bila belum ada: {@code apotik_*} NYALA
 *       dan {@code emedik_*} NYALA (kasir apotek juga melayani tagihan layanan), kunci lain
 *       eksplisit mati (bungkus deterministik {@link SalesInventoryHelper#bungkusMenuRole}).</li>
 *   <li>Setiap role ber-{@code Tbmrole.emedic = true} (tenaga medis / pendaftaran medis / rekam
 *       medis): {@code emedik_*} NYALA, {@code apotik_*} MATI -- HANYA untuk kunci yang BELUM
 *       pernah tersimpan di JSON role itu. Role yang sudah disunting admin TIDAK ditimpa
 *       (idempoten: run kedua tidak mengubah apa pun).</li>
 *   <li>Grup POS/kantin lama: TIDAK disentuh sama sekali -- kunci baru default mati lewat
 *       {@link EbisnisMenuKatalog#KUNCI_DEFAULT_NONAKTIF} (fail-closed terpusat).</li>
 * </ul>
 */
public final class ApotikEmedikSeedHelper {

	private ApotikEmedikSeedHelper() {
	}

	/** roleId grup apoteker -- PK teks {@link Tbmrole}, pola sama ROLE_PEMILIK/ROLE_SALES_KELILING. */
	public static final String ROLE_APOTIK = "apotik";
	public static final String ROLE_APOTIK_PEMILIK_DEMO = "apotik_pemilik_demo";
	public static final String ROLE_APOTIK_KASIR_DEMO = "apotik_kasir_demo";
	public static final String ROLE_APOTIK_MANAJEMEN_DEMO = "apotik_manajemen_demo";
	public static final String ROLE_APOTIK_GUDANG_DEMO = "apotik_gudang_demo";
	public static final String ROLE_EMEDIK_PENDAFTARAN_DEMO = "emedik_pendaftaran_demo";

	private static final String[] KUNCI_APOTIK = { "apotik_kasir", "apotik_resep", "apotik_racikan",
			"apotik_formularium", "apotik_batch", "apotik_pengadaan", "apotik_stok_opname",
			"apotik_retur", "apotik_narkotika", "apotik_laporan" };
	private static final String[] KUNCI_EMEDIK = { "emedik_kasir", "emedik_pendaftaran", "emedik_tagihan",
			"emedik_deposit", "emedik_penjamin", "emedik_laporan" };

	private static volatile boolean seedSudahDicoba = false;

	/** Idempoten &amp; murah dipanggil berulang -- guard volatile per-JVM; gagal = dicoba ulang
	 *  di load servlet berikutnya, TIDAK menjatuhkan servlet (pola sama seed inventory_sales). */
	public static void pastikanSeed() {
		if (seedSudahDicoba) {
			return;
		}
		seedSudahDicoba = true;
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			boolean adaPerubahan = false;

			// 1. Grup "Apotik" -- buat bila belum ada (nilai eksplisit deterministik).
			Tbmrole apotik = (Tbmrole) session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("roleId", ROLE_APOTIK)).setMaxResults(1).uniqueResult();
			if (apotik == null) {
				apotik = new Tbmrole();
				apotik.setRoleId(ROLE_APOTIK);
				apotik.setRoleName("Apotik");
				apotik.setAktif(Boolean.TRUE);
				apotik.setEbisnisMenu(menuRoleApotikJson());
				session.save(apotik);
				adaPerubahan = true;
			} else {
				// Role sudah ada (mis. dibuat admin manual): lengkapi HANYA kunci yang belum
				// pernah tersimpan -- suntingan admin tidak ditimpa.
				String digabung = gabungKunciBelumTersimpan(apotik.getEbisnisMenu(), KUNCI_APOTIK, true);
				digabung = gabungKunciBelumTersimpan(digabung, KUNCI_EMEDIK, true);
				if (digabung != null && !digabung.equals(apotik.getEbisnisMenu())) {
					apotik.setEbisnisMenu(digabung);
					session.saveOrUpdate(apotik);
					adaPerubahan = true;
				}
			}

			// 2. Seluruh role ber-emedic=true: emedik_* nyala, apotik_* mati -- hanya kunci
			// yang belum pernah tersimpan.
			@SuppressWarnings("unchecked")
			List<Tbmrole> roleMedis = session.createCriteria(Tbmrole.class)
					.add(Restrictions.eq("emedic", Boolean.TRUE)).list();
			for (Tbmrole r : roleMedis) {
				if (ROLE_APOTIK.equals(r.getRoleId())) {
					continue; // sudah ditangani blok 1 (apotik_* nyala di sana).
				}
				String digabung = gabungKunciBelumTersimpan(r.getEbisnisMenu(), KUNCI_EMEDIK, true);
				digabung = gabungKunciBelumTersimpan(digabung, KUNCI_APOTIK, false);
				if (digabung != null && !digabung.equals(r.getEbisnisMenu())) {
					r.setEbisnisMenu(digabung);
					session.saveOrUpdate(r);
					adaPerubahan = true;
				}
			}
			tx.commit();
			if (adaPerubahan) {
				System.out.println("[APOTIK-SEED] Menu apotik/emedik disemai (role apotik + "
						+ roleMedis.size() + " role emedic; hanya kunci yang belum pernah tersimpan).");
			}
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			seedSudahDicoba = false;
			ais.common.ErrorAuditUtil.record(e, "ApotikEmedikSeedHelper.pastikanSeed");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Kembalikan JSON {@code ebisnisMenu} dengan {@code kunci} di-set {@code nilai} HANYA bila kunci
	 * itu belum pernah tersimpan -- kunci yang sudah ada (di-set seed sebelumnya ATAU disunting
	 * admin) dibiarkan apa adanya. {@code raw} null/kosong/rusak diperlakukan sebagai objek kosong:
	 * hasilnya JSON berisi HANYA kunci baru; kunci lama yang tak disebut tetap mengikuti default
	 * {@link EbisnisMenuKatalog#urai} (legacy true, varian fail-closed false) -- perilaku role
	 * tidak berubah di luar kunci yang disemai.
	 */
	private static String gabungKunciBelumTersimpan(String raw, String[] kunci, boolean nilai)
			throws Exception {
		JSONObject obj;
		try {
			obj = raw == null || raw.trim().isEmpty() ? new JSONObject() : new JSONObject(raw);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApotikEmedikSeedHelper: JSON ebisnisMenu rusak, mulai dari kosong");
			obj = new JSONObject();
		}
		JSONObject menu = obj.optJSONObject("menu");
		if (menu == null) {
			menu = new JSONObject();
			obj.put("menu", menu);
		}
		boolean berubah = false;
		for (int i = 0; i < kunci.length; i++) {
			if (!menu.has(kunci[i])) {
				menu.put(kunci[i], nilai);
				berubah = true;
			}
		}
		return berubah ? obj.toString() : raw;
	}

	/** Bungkus deterministik role "Apotik": apotik_* + emedik_* nyala + CRUD penuh pada menu
	 *  apotik/emedik ber-CRUD; SELURUH kunci lain eksplisit mati (termasuk menu POS/kantin lama --
	 *  kasir apotek bukan kasir toko kelontong). */
	private static String menuRoleApotikJson() throws Exception {
		java.util.Set<String> menuAktif = SalesInventoryHelper.set();
		for (int i = 0; i < KUNCI_APOTIK.length; i++) menuAktif.add(KUNCI_APOTIK[i]);
		for (int i = 0; i < KUNCI_EMEDIK.length; i++) menuAktif.add(KUNCI_EMEDIK[i]);
		// Pendukung operasional harian kasir: konfigurasi perangkat + log + sinkronisasi.
		menuAktif.add("konfigurasi");
		menuAktif.add("logerror");
		menuAktif.add("riwayatsinkronisasi");
		java.util.Map<String, java.util.Set<String>> crudAktif = new java.util.LinkedHashMap<String, java.util.Set<String>>();
		String[] crudPenuh = { "apotik_kasir", "apotik_resep", "apotik_racikan", "apotik_formularium",
				"apotik_pengadaan", "apotik_stok_opname", "apotik_retur", "apotik_narkotika",
				"emedik_kasir", "emedik_pendaftaran", "emedik_tagihan", "emedik_deposit", "emedik_penjamin" };
		for (int i = 0; i < crudPenuh.length; i++) {
			crudAktif.put(crudPenuh[i], SalesInventoryHelper.SEMUA_AKSI_CRUD);
		}
		return SalesInventoryHelper.bungkusMenuRole(menuAktif, crudAktif);
	}

	/**
	 * Pastikan role khusus akun sample tersedia di transaksi pemanggil. Method ini sengaja
	 * package-visible dan hanya dipanggil provisioning yang sudah dilindungi konfigurasi
	 * {@code data_sample_ebisnis}; seed menu normal tidak membuat akun demo.
	 */
	/* package */ static java.util.Map<String, Tbmrole> pastikanRoleDemo(Session session)
			throws Exception {
		java.util.Map<String, Tbmrole> hasil = new java.util.LinkedHashMap<String, Tbmrole>();

		// Tiga jenis pengguna Apotik baku (diminta eksplisit): Pemilik (akses penuh,
		// terpisah dari role "apotik" lama yang dipakai instalasi nyata lewat
		// pastikanSeed() -- supaya label/menu demo bisa disetel deterministik tanpa
		// menimpa role produksi yang mungkin sudah disunting admin), Kasir (transaksi
		// harian), dan Manajemen (formularium/pengadaan/opname/retur/laporan, tanpa
		// transaksi kasir harian).
		Tbmrole pemilik = pastikanRole(session, ROLE_APOTIK_PEMILIK_DEMO, "Pemilik Apotik",
				menuRoleApotikJson(), Tbmrole.HALAMAN_UTAMA_APOTIK, false, true);
		hasil.put(ROLE_APOTIK_PEMILIK_DEMO, pemilik);

		Tbmrole apoteker = pastikanRole(session, ROLE_APOTIK, "Apoteker (Akses Penuh)",
				menuRoleApotikJson(), Tbmrole.HALAMAN_UTAMA_APOTIK, false, false);
		hasil.put(ROLE_APOTIK, apoteker);

		Tbmrole kasir = pastikanRole(session, ROLE_APOTIK_KASIR_DEMO, "Kasir Apotik",
				menuRoleTerbatas(new String[] { "apotik_kasir", "apotik_resep", "apotik_batch",
						"emedik_kasir", "emedik_tagihan" },
						new String[] { "apotik_kasir", "emedik_kasir", "emedik_tagihan" }),
				Tbmrole.HALAMAN_UTAMA_APOTIK, false, true);
		hasil.put(ROLE_APOTIK_KASIR_DEMO, kasir);

		Tbmrole manajemen = pastikanRole(session, ROLE_APOTIK_MANAJEMEN_DEMO, "Manajemen Apotik",
				menuRoleTerbatas(new String[] { "apotik_formularium", "apotik_batch",
						"apotik_pengadaan", "apotik_stok_opname", "apotik_retur",
						"apotik_narkotika", "apotik_laporan", "emedik_laporan" },
						new String[] { "apotik_formularium", "apotik_pengadaan",
								"apotik_stok_opname", "apotik_retur", "apotik_laporan" }),
				Tbmrole.HALAMAN_UTAMA_APOTIK, false, true);
		hasil.put(ROLE_APOTIK_MANAJEMEN_DEMO, manajemen);

		Tbmrole gudang = pastikanRole(session, ROLE_APOTIK_GUDANG_DEMO, "Gudang dan Pengadaan Apotik",
				menuRoleTerbatas(new String[] { "apotik_formularium", "apotik_batch",
						"apotik_pengadaan", "apotik_stok_opname", "apotik_retur", "apotik_laporan" },
						new String[] { "apotik_formularium", "apotik_pengadaan",
								"apotik_stok_opname", "apotik_retur" }),
				Tbmrole.HALAMAN_UTAMA_APOTIK, false, true);
		hasil.put(ROLE_APOTIK_GUDANG_DEMO, gudang);

		Tbmrole pendaftaran = pastikanRole(session, ROLE_EMEDIK_PENDAFTARAN_DEMO,
				"Pendaftaran dan Administrasi eMedik",
				menuRoleTerbatas(new String[] { "emedik_pendaftaran", "emedik_tagihan",
						"emedik_deposit", "emedik_penjamin", "emedik_laporan" },
						new String[] { "emedik_pendaftaran", "emedik_tagihan",
								"emedik_deposit", "emedik_penjamin" }),
				Tbmrole.HALAMAN_UTAMA_EMEDIK, true, true);
		hasil.put(ROLE_EMEDIK_PENDAFTARAN_DEMO, pendaftaran);
		return hasil;
	}

	private static Tbmrole pastikanRole(Session session, String roleId, String nama, String menu,
			String halamanUtama, boolean emedic, boolean perbaruiMenu) {
		Tbmrole role = (Tbmrole) session.get(Tbmrole.class, roleId);
		if (role == null) {
			role = new Tbmrole();
			role.setRoleId(roleId);
			role.setRoleName(nama);
			role.setAktif(Boolean.TRUE);
			role.setEbisnisMenu(menu);
			role.setHalamanUtama(halamanUtama);
			role.setEmedic(Boolean.valueOf(emedic));
			session.save(role);
		} else {
			role.setAktif(Boolean.TRUE);
			if (role.getHalamanUtama() == null || role.getHalamanUtama().trim().isEmpty()) {
				role.setHalamanUtama(halamanUtama);
			}
			if (emedic) role.setEmedic(Boolean.TRUE);
			// Role berakhiran _demo dikelola provisioning, sehingga aman dibuat deterministik.
			// Role apotik lama tetap mempertahankan suntingan admin.
			if (perbaruiMenu) role.setEbisnisMenu(menu);
			session.saveOrUpdate(role);
		}
		return role;
	}

	private static String menuRoleTerbatas(String[] menu, String[] crudPenuh) throws Exception {
		java.util.Set<String> menuAktif = SalesInventoryHelper.set();
		for (int i = 0; i < menu.length; i++) menuAktif.add(menu[i]);
		menuAktif.add("konfigurasi");
		menuAktif.add("logerror");
		menuAktif.add("riwayatsinkronisasi");
		java.util.Map<String, java.util.Set<String>> crudAktif =
				new java.util.LinkedHashMap<String, java.util.Set<String>>();
		for (int i = 0; i < crudPenuh.length; i++) {
			crudAktif.put(crudPenuh[i], SalesInventoryHelper.SEMUA_AKSI_CRUD);
		}
		return SalesInventoryHelper.bungkusMenuRole(menuAktif, crudAktif);
	}

	/* package */ static String menuRoleTenagaMedisDemo() throws Exception {
		return menuRoleTerbatas(new String[] { "emedik_kasir", "emedik_pendaftaran",
				"emedik_laporan" }, new String[] { "emedik_kasir", "emedik_pendaftaran" });
	}
}
