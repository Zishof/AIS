package ais.service.tenant;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.model.tenant.TenantRegistry;

/**
 * <h3>Data-plane per-tenant (P8 TENANT_ONLY cutover, strangler-fig) -- Brand/Toko/Mesin POS.</h3>
 *
 * <p>Utk tenant BER-SCHEMA (HYBRID/TENANT_ONLY): permukaan dashboard yang dimiliki program ini
 * mulai (1) <b>DUAL-WRITE</b> -- setiap tulis ke tabel shared existing ({@code public.brand},
 * {@code koperasi.toko}, {@code koperasi.pedagang}; TETAP ditulis supaya POS runtime kompatibel)
 * juga menulis baris MIRROR ber-id-sama ke schema tenant + baris audit gaya Envers
 * ({@code <slug>__audit.revinfo} + mirror ber-rev/revtype) dalam TRANSAKSI YANG SAMA;
 * (2) <b>READ</b> daftar dibaca dari schema tenant (dgn {@link #sinkronDariShared} menyalin baris
 * pra-cutover yang belum ada -- backfill inkremental). Tenant TANPA schema / akun legacy
 * pra-program tetap memakai jalur shared lama TANPA perubahan.</p>
 *
 * <p><b>Mode platform dibaca via SQL LANGSUNG</b> ({@link #modePlatform}) BUKAN cache
 * Konfigurasi in-JVM (MapDB) -- perubahan mode oleh admin berlaku SEKETIKA pada gerbang
 * data-plane tanpa restart; biaya = satu lookup ter-indeks per aksi mutasi/list (dapat diterima).
 * §3.3: pada mode TENANT_ONLY, tenant program TANPA schema valid DIBLOKIR menjalankan
 * data-plane baru (pesan jujur); akun legacy pra-program tetap fail-open di jalur lama.</p>
 *
 * <p>PG 9.3: TANPA ON CONFLICT (9.5+) -- upsert = UPDATE lalu INSERT bila 0 baris;
 * {@code RETURNING} dipakai utk rev audit (didukung 9.3). Identifier schema SELALU dari registry
 * dan tervalidasi {@link TenantSchemaService#pastikanAman} (invariant #3).</p>
 */
public final class TenantDataPlaneService {

	public static final int REVTYPE_ADD = 0;
	public static final int REVTYPE_MOD = 1;

	private TenantDataPlaneService() {
	}

	/** Mode platform LEGACY/HYBRID/TENANT_ONLY via SQL langsung (bebas cache; default LEGACY). */
	public static String modePlatform(Session session) {
		try {
			Object nilai = session.createSQLQuery(
					"SELECT nilai FROM public.konfigurasi WHERE nama = 'pendaftaran_tenant_mode'")
					.setMaxResults(1).uniqueResult();
			String v = nilai == null ? "" : String.valueOf(nilai).trim();
			if (TenantRegistry.MODE_HYBRID.equalsIgnoreCase(v)) {
				return TenantRegistry.MODE_HYBRID;
			}
			if (TenantRegistry.MODE_TENANT_ONLY.equalsIgnoreCase(v)) {
				return TenantRegistry.MODE_TENANT_ONLY;
			}
			return TenantRegistry.MODE_LEGACY;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantDataPlaneService.modePlatform");
			return TenantRegistry.MODE_LEGACY;
		}
	}

	/** Schema tenant milik pendaftar (owner, schema terisi, READY/ACTIVE); null = tanpa schema. */
	public static String schemaTenantMilik(Session session, Long pendaftarId) {
		try {
			Object schema = session.createSQLQuery("SELECT schema_name FROM public.tenant_registry "
					+ "WHERE owner_pendaftar_id = :p AND schema_name IS NOT NULL "
					+ "AND status IN ('READY','ACTIVE') ORDER BY id LIMIT 1")
					.setParameter("p", pendaftarId).uniqueResult();
			if (schema == null) {
				return null;
			}
			return TenantSchemaService.pastikanAman(String.valueOf(schema));
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantDataPlaneService.schemaTenantMilik");
			return null;
		}
	}

	/**
	 * Gerbang §3.3 TENANT_ONLY: pendaftar PROGRAM-TENANT (punya permohonan) tanpa schema valid
	 * diblokir menjalankan data-plane. @return pesan blokir atau null (boleh).
	 */
	public static String alasanBlokirTenantOnly(Session session, Long pendaftarId) {
		if (!TenantRegistry.MODE_TENANT_ONLY.equals(modePlatform(session))) {
			return null;
		}
		Number permohonan = (Number) session.createSQLQuery(
				"SELECT COUNT(*) FROM public.pendaftaran_tenant WHERE pendaftar_id = :p")
				.setParameter("p", pendaftarId).uniqueResult();
		if (permohonan == null || permohonan.longValue() == 0) {
			return null; // akun legacy pra-program: jalur shared lama tetap berlaku
		}
		if (schemaTenantMilik(session, pendaftarId) == null) {
			return "Platform berjalan pada mode TENANT_ONLY: tenant Anda belum memiliki schema "
					+ "terprovision. Hubungi dukungan untuk migrasi tenant.";
		}
		return null;
	}

	// =====================================================================
	// AUDIT (Envers-style di <slug>__audit)
	// =====================================================================

	/**
	 * Terbitkan satu baris {@code revinfo} pada schema audit tenant ({@code <schema>__audit}) dan
	 * kembalikan nomor {@code rev} yang baru dibuat. Dipanggil oleh setiap {@code mirror*} tepat
	 * sekali per operasi mutasi, sebelum baris mirror-audit ({@code brand}/{@code toko}/
	 * {@code pedagang} di schema {@code __audit}) ditulis dengan {@code rev} yang sama --
	 * berbeda dari {@link TenantAuditWriter#mulaiRevisi}, versi ini disederhanakan (hanya
	 * {@code revtstmp}) karena baris audit data-plane menyimpan konteksnya sendiri per kolom,
	 * bukan lewat satu baris {@code revinfo} yang kaya konteks.
	 *
	 * @param session Session pemanggil; ditulis pada transaksi yang sama dengan mutasi datanya.
	 * @param schema  nama schema tenant TANPA akhiran {@code __audit} (akhiran ditambahkan di sini).
	 * @return nomor {@code rev} yang baru diterbitkan, dipakai sebagai kunci baris mirror-audit.
	 */
	private static long auditRev(Session session, String schema) {
		Number rev = (Number) session.createSQLQuery("INSERT INTO \"" + schema
				+ "__audit\".revinfo (revtstmp) VALUES (:ts) RETURNING rev")
				.setParameter("ts", Long.valueOf(System.currentTimeMillis())).uniqueResult();
		return rev.longValue();
	}

	// =====================================================================
	// MIRROR UPSERT + AUDIT (dipanggil dalam TX yang sama dgn tulis shared)
	// =====================================================================

	/**
	 * Implementasi kanonik pola DUAL-WRITE mirror+audit untuk entitas {@code brand}: dipanggil
	 * SETELAH kode pemanggil menulis baris ke tabel shared ({@code public.brand}) dalam TRANSAKSI
	 * YANG SAMA, untuk menjaga schema tenant tetap sinkron sebagai salinan ber-id-sama.
	 *
	 * <p>
	 * Urutan kerja: (1) UPDATE baris {@code <schema>.brand} dengan {@code id} yang sama seperti
	 * baris shared-nya; (2) bila UPDATE menyentuh 0 baris (belum ada -- baris baru atau backfill
	 * lewat {@link #sinkronDariShared}), INSERT baris baru dengan {@code id} eksplisit yang sama
	 * (bukan auto-increment, sebab id HARUS identik dengan baris shared agar keduanya tetap dapat
	 * dikorelasikan); (3) terbitkan satu {@code rev} audit lewat {@link #auditRev} lalu tulis satu
	 * baris mirror-audit ke {@code <schema>__audit.brand} dengan {@code revtype} sesuai parameter
	 * {@code baru} ({@link #REVTYPE_ADD} atau {@link #REVTYPE_MOD}). UPDATE-lalu-INSERT (bukan
	 * {@code ON CONFLICT}) dipakai karena target berjalan di atas PostgreSQL 9.3 yang belum
	 * mendukung {@code ON CONFLICT} (baru ada di 9.5+) -- lihat catatan PG 9.3 di javadoc kelas.
	 * </p>
	 *
	 * @param session Session pemanggil; wajib berjalan pada transaksi yang sama dengan tulis
	 *                shared-nya supaya mirror dan shared selalu konsisten.
	 * @param schema  nama schema tenant, divalidasi ulang di sini lewat
	 *                {@link TenantSchemaService#pastikanAman} (invariant #3) sekalipun sudah
	 *                divalidasi di pemanggil -- identifier schema tidak pernah dipercaya begitu
	 *                saja sebelum disisipkan ke SQL.
	 * @param id      id baris, sama dengan id baris {@code public.brand} yang dicerminkan.
	 * @param nama    nama brand.
	 * @param aktif   status aktif brand.
	 * @param baru    {@code true} bila ini baris yang baru pertama kali dibuat (menentukan
	 *                {@code revtype} audit: {@link #REVTYPE_ADD} vs {@link #REVTYPE_MOD}); tidak
	 *                menentukan jalur UPDATE-vs-INSERT itu sendiri, yang selalu diputuskan dari
	 *                jumlah baris ter-UPDATE.
	 */
	public static void mirrorBrand(Session session, String schema, Long id, String nama, Boolean aktif,
			boolean baru) {
		String s = TenantSchemaService.pastikanAman(schema);
		int diubah = session.createSQLQuery("UPDATE \"" + s + "\".brand SET nama=:n, aktif=:a, "
				+ "tanggal_dirubah=now() WHERE id=:id")
				.setParameter("n", nama).setParameter("a", aktif).setParameter("id", id).executeUpdate();
		if (diubah == 0) {
			session.createSQLQuery("INSERT INTO \"" + s + "\".brand (id, nama, aktif, dibuat_pada, "
					+ "tanggal_dirubah, oleh, olehid) VALUES (:id, :n, :a, now(), now(), 'dataplane', 'dataplane')")
					.setParameter("id", id).setParameter("n", nama).setParameter("a", aktif).executeUpdate();
		}
		long rev = auditRev(session, s);
		session.createSQLQuery("INSERT INTO \"" + s + "__audit\".brand (id, rev, revtype, nama, aktif, "
				+ "oleh, olehid) VALUES (:id, :rev, :rt, :n, :a, 'dataplane', 'dataplane')")
				.setParameter("id", id).setParameter("rev", Long.valueOf(rev))
				.setParameter("rt", Integer.valueOf(baru ? REVTYPE_ADD : REVTYPE_MOD))
				.setParameter("n", nama).setParameter("a", aktif).executeUpdate();
	}

	/**
	 * Seperti {@link #mirrorBrand} tetapi untuk entitas {@code toko}: UPDATE-lalu-INSERT ke
	 * {@code <schema>.toko} dengan {@code id} sama seperti baris {@code koperasi.toko}, diikuti
	 * satu baris mirror-audit ke {@code <schema>__audit.toko}. Pola UPSERT dan alasan
	 * PG-9.3-nya identik dengan {@link #mirrorBrand} -- lihat javadoc method itu untuk penjelasan
	 * lengkap.
	 *
	 * @param session Session pemanggil, dalam transaksi yang sama dengan tulis shared-nya.
	 * @param schema  nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @param id      id baris, sama dengan {@code koperasi.toko.id}.
	 * @param nama    nama toko.
	 * @param brandId id brand pemilik toko, boleh {@code null}.
	 * @param alamat  alamat toko.
	 * @param kota    kota toko.
	 * @param telp    nomor telepon toko.
	 * @param aktif   status aktif toko.
	 * @param baru    {@code true} untuk baris baru ({@link #REVTYPE_ADD}), {@code false} untuk
	 *                perubahan ({@link #REVTYPE_MOD}).
	 */
	public static void mirrorToko(Session session, String schema, Long id, String nama, Long brandId,
			String alamat, String kota, String telp, Boolean aktif, boolean baru) {
		String s = TenantSchemaService.pastikanAman(schema);
		int diubah = session.createSQLQuery("UPDATE \"" + s + "\".toko SET nama=:n, brand_id=:b, "
				+ "alamat=:al, kota=:k, telp=:t, aktif=:a, tanggal_dirubah=now() WHERE id=:id")
				.setParameter("n", nama).setParameter("b", brandId).setParameter("al", alamat)
				.setParameter("k", kota).setParameter("t", telp).setParameter("a", aktif)
				.setParameter("id", id).executeUpdate();
		if (diubah == 0) {
			session.createSQLQuery("INSERT INTO \"" + s + "\".toko (id, nama, brand_id, alamat, kota, "
					+ "telp, aktif, dibuat_pada, tanggal_dirubah, oleh, olehid) VALUES "
					+ "(:id, :n, :b, :al, :k, :t, :a, now(), now(), 'dataplane', 'dataplane')")
					.setParameter("id", id).setParameter("n", nama).setParameter("b", brandId)
					.setParameter("al", alamat).setParameter("k", kota).setParameter("t", telp)
					.setParameter("a", aktif).executeUpdate();
		}
		long rev = auditRev(session, s);
		session.createSQLQuery("INSERT INTO \"" + s + "__audit\".toko (id, rev, revtype, nama, brand_id, "
				+ "alamat, kota, telp, aktif, oleh, olehid) VALUES (:id, :rev, :rt, :n, :b, :al, :k, :t, "
				+ ":a, 'dataplane', 'dataplane')")
				.setParameter("id", id).setParameter("rev", Long.valueOf(rev))
				.setParameter("rt", Integer.valueOf(baru ? REVTYPE_ADD : REVTYPE_MOD))
				.setParameter("n", nama).setParameter("b", brandId).setParameter("al", alamat)
				.setParameter("k", kota).setParameter("t", telp).setParameter("a", aktif).executeUpdate();
	}

	/**
	 * Seperti {@link #mirrorBrand} tetapi untuk entitas {@code pedagang} (mesin POS/kasir): UPDATE-
	 * lalu-INSERT ke {@code <schema>.pedagang} dengan {@code id} sama seperti baris
	 * {@code koperasi.pedagang}, diikuti satu baris mirror-audit ke
	 * {@code <schema>__audit.pedagang}. Pola UPSERT identik dengan {@link #mirrorBrand}.
	 *
	 * <p>
	 * <b>Kredensial tidak masuk audit</b>: kolom {@code pass} ditulis ke baris data
	 * ({@code <schema>.pedagang}) tetapi SENGAJA tidak disertakan pada INSERT ke
	 * {@code <schema>__audit.pedagang} (lihat komentar inline di badan method) -- selaras dengan
	 * larangan menyimpan rahasia pada jejak audit (semangat invariant #8 / &sect;11.6, sama
	 * seperti yang berlaku pada {@link TenantAuditWriter}).
	 * </p>
	 *
	 * @param session    Session pemanggil, dalam transaksi yang sama dengan tulis shared-nya.
	 * @param schema     nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @param id         id baris, sama dengan {@code koperasi.pedagang.id}.
	 * @param userid     userid login mesin POS/kasir.
	 * @param pass       kata sandi (hash/terenkripsi sesuai konvensi tabel shared); ditulis ke
	 *                   baris data saja, TIDAK ke baris audit.
	 * @param nama       nama pedagang/kasir.
	 * @param tokoId     id toko tempat pedagang ini bertugas.
	 * @param supervisor status hak supervisor.
	 * @param aktif      status aktif.
	 * @param baru       {@code true} untuk baris baru ({@link #REVTYPE_ADD}), {@code false} untuk
	 *                   perubahan ({@link #REVTYPE_MOD}).
	 */
	public static void mirrorPedagang(Session session, String schema, Long id, String userid, String pass,
			String nama, Long tokoId, Boolean supervisor, Boolean aktif, boolean baru) {
		String s = TenantSchemaService.pastikanAman(schema);
		int diubah = session.createSQLQuery("UPDATE \"" + s + "\".pedagang SET userid=:u, pass=:p, "
				+ "nama=:n, toko_id=:t, supervisor=:sv, aktif=:a, tanggal_dirubah=now() WHERE id=:id")
				.setParameter("u", userid).setParameter("p", pass).setParameter("n", nama)
				.setParameter("t", tokoId).setParameter("sv", supervisor).setParameter("a", aktif)
				.setParameter("id", id).executeUpdate();
		if (diubah == 0) {
			session.createSQLQuery("INSERT INTO \"" + s + "\".pedagang (id, userid, pass, nama, toko_id, "
					+ "supervisor, aktif, tanggal_dirubah, oleh, olehid) VALUES (:id, :u, :p, :n, :t, :sv, "
					+ ":a, now(), 'dataplane', 'dataplane')")
					.setParameter("id", id).setParameter("u", userid).setParameter("p", pass)
					.setParameter("n", nama).setParameter("t", tokoId).setParameter("sv", supervisor)
					.setParameter("a", aktif).executeUpdate();
		}
		long rev = auditRev(session, s);
		// Audit TANPA kolom pass (kredensial tidak masuk audit -- invariant #8 semangatnya).
		session.createSQLQuery("INSERT INTO \"" + s + "__audit\".pedagang (id, rev, revtype, userid, nama, "
				+ "toko_id, supervisor, aktif, oleh, olehid) VALUES (:id, :rev, :rt, :u, :n, :t, :sv, :a, "
				+ "'dataplane', 'dataplane')")
				.setParameter("id", id).setParameter("rev", Long.valueOf(rev))
				.setParameter("rt", Integer.valueOf(baru ? REVTYPE_ADD : REVTYPE_MOD))
				.setParameter("u", userid).setParameter("n", nama).setParameter("t", tokoId)
				.setParameter("sv", supervisor).setParameter("a", aktif).executeUpdate();
	}

	// =====================================================================
	// SINKRON BACKFILL shared -> tenant schema (baris pra-cutover)
	// =====================================================================

	/**
	 * Salin baris shared milik pendaftar yang BELUM ada di schema tenant (+audit revtype ADD).
	 *
	 * <p>
	 * Ini adalah mekanisme <b>backfill inkremental</b> untuk baris yang dibuat SEBELUM cutover
	 * tenant ke mode ber-schema (pra-existing di {@code public.brand}/{@code koperasi.toko}/
	 * {@code koperasi.pedagang}) dan karenanya belum pernah melalui jalur DUAL-WRITE
	 * ({@code mirror*}). Untuk masing-masing dari tiga entitas (brand, toko, pedagang, dalam
	 * urutan itu -- toko bergantung pada brand, pedagang bergantung pada toko, jadi urutannya
	 * BUKAN kebetulan), method ini mencari baris shared milik {@code pendaftarId} yang
	 * {@code id}-nya belum ada di schema tenant ({@code NOT EXISTS}) lalu memanggil
	 * {@code mirror*} yang bersangkutan dengan {@code baru=true} (revtype ADD) untuk setiap baris
	 * yang hilang. Idempoten: dipanggil berulang kali hanya memproses baris yang benar-benar
	 * belum tersalin, sehingga aman dipanggil di setiap READ (lihat
	 * {@link #sinkronDenganTransaksi}) tanpa membuat duplikat maupun baris audit berulang.
	 * </p>
	 *
	 * @param session     Session pemanggil.
	 * @param schema      nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @param pendaftarId id pendaftar pemilik data shared yang akan dibackfill.
	 */
	@SuppressWarnings("rawtypes")
	public static void sinkronDariShared(Session session, String schema, Long pendaftarId) {
		String s = TenantSchemaService.pastikanAman(schema);
		List brands = session.createSQLQuery("SELECT b.id, b.nama, b.aktif FROM public.brand b "
				+ "WHERE b.pendaftar = :p AND NOT EXISTS (SELECT 1 FROM \"" + s
				+ "\".brand t WHERE t.id = b.id)").setParameter("p", pendaftarId).list();
		for (int i = 0; i < brands.size(); i++) {
			Object[] r = (Object[]) brands.get(i);
			mirrorBrand(session, s, Long.valueOf(((Number) r[0]).longValue()), String.valueOf(r[1]),
					r[2] == null ? Boolean.TRUE : (Boolean) r[2], true);
		}
		List tokos = session.createSQLQuery("SELECT t.id, t.nama, t.brand, t.alamat, t.kota, t.telp, "
				+ "t.aktif FROM koperasi.toko t WHERE t.pendaftar = :p AND NOT EXISTS "
				+ "(SELECT 1 FROM \"" + s + "\".toko x WHERE x.id = t.id)")
				.setParameter("p", pendaftarId).list();
		for (int i = 0; i < tokos.size(); i++) {
			Object[] r = (Object[]) tokos.get(i);
			mirrorToko(session, s, Long.valueOf(((Number) r[0]).longValue()), String.valueOf(r[1]),
					r[2] == null ? null : Long.valueOf(((Number) r[2]).longValue()),
					r[3] == null ? "" : String.valueOf(r[3]), r[4] == null ? "" : String.valueOf(r[4]),
					r[5] == null ? "" : String.valueOf(r[5]),
					r[6] == null ? Boolean.TRUE : (Boolean) r[6], true);
		}
		List pedagangs = session.createSQLQuery("SELECT pd.id, pd.userid, pd.pass, pd.nama, pd.toko, "
				+ "pd.supervisor, pd.aktif FROM koperasi.pedagang pd JOIN koperasi.toko t ON t.id = pd.toko "
				+ "WHERE t.pendaftar = :p AND NOT EXISTS (SELECT 1 FROM \"" + s
				+ "\".pedagang x WHERE x.id = pd.id)").setParameter("p", pendaftarId).list();
		for (int i = 0; i < pedagangs.size(); i++) {
			Object[] r = (Object[]) pedagangs.get(i);
			mirrorPedagang(session, s, Long.valueOf(((Number) r[0]).longValue()), String.valueOf(r[1]),
					r[2] == null ? "" : String.valueOf(r[2]), r[3] == null ? "" : String.valueOf(r[3]),
					r[4] == null ? null : Long.valueOf(((Number) r[4]).longValue()),
					r[5] == null ? Boolean.FALSE : (Boolean) r[5],
					r[6] == null ? Boolean.TRUE : (Boolean) r[6], true);
		}
	}

	// =====================================================================
	// READ dari tenant schema
	// =====================================================================

	/**
	 * Daftar brand untuk dashboard, dibaca langsung dari schema tenant ({@code <schema>.brand})
	 * -- BUKAN dari {@code public.brand} shared -- sesuai pola READ data-plane dijelaskan di
	 * javadoc kelas: dashboard membaca dari schema tenant, sedangkan tabel shared tetap ditulis
	 * (dual-write) demi kompatibilitas runtime POS lama. Pemanggil bertanggung jawab memastikan
	 * {@link #sinkronDariShared}/{@link #sinkronDenganTransaksi} sudah dijalankan sebelumnya agar
	 * daftar ini lengkap termasuk baris pra-cutover.
	 *
	 * @param session Session pemanggil.
	 * @param schema  nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @return array JSON berisi {@code id}, {@code nama}, {@code aktif} per brand, terurut nama.
	 * @throws Exception diteruskan dari kegagalan membangun {@link JSONObject}/{@link JSONArray}.
	 */
	@SuppressWarnings("rawtypes")
	public static JSONArray listBrand(Session session, String schema) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		JSONArray arr = new JSONArray();
		List rows = session.createSQLQuery(
				"SELECT id, nama, aktif FROM \"" + s + "\".brand ORDER BY nama").list();
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = (Object[]) rows.get(i);
			JSONObject j = new JSONObject();
			j.put("id", ((Number) r[0]).longValue());
			j.put("nama", String.valueOf(r[1]));
			j.put("aktif", Boolean.TRUE.equals(r[2]));
			arr.put(j);
		}
		return arr;
	}

	/**
	 * Daftar toko untuk dashboard, dibaca dari schema tenant ({@code <schema>.toko} di-JOIN
	 * {@code <schema>.brand}), termasuk jumlah mesin POS (baris {@code pedagang}) per toko lewat
	 * subquery {@code COUNT(*)}. Lihat catatan inline pada badan method soal label kolom: setiap
	 * kolom pada SELECT native ini WAJIB berlabel unik ({@code t_id}, {@code t_nama}, dst.)
	 * karena driver Hibernate membaca hasil query native by-label -- dua kolom berlabel sama
	 * (mis. dua {@code nama} dari tabel berbeda) akan membuat salah satunya salah terbaca. Bug
	 * ini pernah tertangkap saat UAT P8, jadi jangan menghapus alias kolom saat mengubah query ini.
	 *
	 * @param session Session pemanggil.
	 * @param schema  nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @return array JSON berisi {@code id}, {@code nama}, {@code brandNama}, {@code kota},
	 *         {@code aktif}, {@code jumlahMesinPos} per toko, terurut nama.
	 * @throws Exception diteruskan dari kegagalan membangun {@link JSONObject}/{@link JSONArray}.
	 */
	@SuppressWarnings("rawtypes")
	public static JSONArray listToko(Session session, String schema) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		JSONArray arr = new JSONArray();
		// Label kolom WAJIB unik: Hibernate native query membaca by-label via driver --
		// dua kolom berlabel "nama" mengembalikan nilai yang sama (bug tertangkap UAT P8).
		List rows = session.createSQLQuery("SELECT t.id AS t_id, t.nama AS t_nama, b.nama AS b_nama, "
				+ "t.kota AS t_kota, t.aktif AS t_aktif, "
				+ "(SELECT COUNT(*) FROM \"" + s + "\".pedagang pd WHERE pd.toko_id = t.id) AS jml "
				+ "FROM \"" + s + "\".toko t LEFT JOIN \"" + s + "\".brand b ON b.id = t.brand_id "
				+ "ORDER BY t.nama").list();
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = (Object[]) rows.get(i);
			JSONObject j = new JSONObject();
			j.put("id", ((Number) r[0]).longValue());
			j.put("nama", String.valueOf(r[1]));
			j.put("brandNama", r[2] == null ? null : String.valueOf(r[2]));
			j.put("kota", r[3] == null ? "" : String.valueOf(r[3]));
			j.put("aktif", Boolean.TRUE.equals(r[4]));
			j.put("jumlahMesinPos", ((Number) r[5]).longValue());
			arr.put(j);
		}
		return arr;
	}

	/**
	 * Daftar mesin POS (baris {@code pedagang}) milik satu toko, dibaca dari
	 * {@code <schema>.pedagang}. Kata sandi ({@code pass}) sengaja TIDAK disertakan pada SELECT
	 * ini, konsisten dengan larangan kredensial keluar ke response yang dibahas di javadoc
	 * {@link TenantAccessException} dan {@link TenantAuditWriter}.
	 *
	 * @param session Session pemanggil.
	 * @param schema  nama schema tenant, divalidasi ulang lewat {@link TenantSchemaService#pastikanAman}.
	 * @param tokoId  id toko yang mesin POS-nya ingin didaftar.
	 * @return array JSON berisi {@code id}, {@code nama}, {@code userid}, {@code aktif} per mesin
	 *         POS, terurut nama.
	 * @throws Exception diteruskan dari kegagalan membangun {@link JSONObject}/{@link JSONArray}.
	 */
	@SuppressWarnings("rawtypes")
	public static JSONArray listMesinPos(Session session, String schema, Long tokoId) throws Exception {
		String s = TenantSchemaService.pastikanAman(schema);
		JSONArray arr = new JSONArray();
		List rows = session.createSQLQuery("SELECT id, nama, userid, aktif FROM \"" + s
				+ "\".pedagang WHERE toko_id = :t ORDER BY nama").setParameter("t", tokoId).list();
		for (int i = 0; i < rows.size(); i++) {
			Object[] r = (Object[]) rows.get(i);
			JSONObject j = new JSONObject();
			j.put("id", ((Number) r[0]).longValue());
			j.put("nama", r[1] == null ? "" : String.valueOf(r[1]));
			j.put("userid", String.valueOf(r[2]));
			j.put("aktif", Boolean.TRUE.equals(r[3]));
			arr.put(j);
		}
		return arr;
	}

	/** Sinkron backfill dgn transaksi sendiri -- dipanggil sebelum READ pada method list tanpa TX. */
	public static void sinkronDenganTransaksi(Session session, String schema, Long pendaftarId) {
		try {
			session.beginTransaction();
			sinkronDariShared(session, schema, pendaftarId);
			session.getTransaction().commit();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit TenantDataPlaneService.sinkronDenganTransaksi");
			try {
				if (session.getTransaction() != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackEx) { ais.common.ErrorAuditUtil.record(rollbackEx, "auto-audit(empty-catch) TenantDataPlaneService.sinkron.rollback");
			}
		}
	}

	/** Timestamp bantu (dipakai uji unit tanggal tidak diperlukan; disediakan utk konsistensi). */
	static Date sekarang() {
		return new Date();
	}
}
