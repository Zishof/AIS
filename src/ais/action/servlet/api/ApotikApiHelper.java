package ais.action.servlet.api;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.ConstantValues;
import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.sirs.ApotikBatchKonsumsi;
import ais.database.model.sirs.ApotikItemProfile;
import ais.database.model.sirs.ApotikNarkotikaLog;
import ais.database.model.sirs.ApotikPembayaranTransaksi;
import ais.database.model.sirs.AlergiPasien;
import ais.database.model.sirs.AntreanFarmasi;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.Icd;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.Kadaluarsa;
import ais.database.model.sirs.KodeTransaksiMedis;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Resep;
import ais.database.model.sirs.ResepDetail;
import ais.database.model.sirs.TransaksiMedis;
import ais.database.model.sirs.TransaksiMedisDetail;

/**
 * <h3>API kasir "POS Apotik" (FASE A) -- membungkus modul SIRS existing, TIDAK menyalin logikanya.</h3>
 *
 * <p>Fakta survei yang jadi dasar (path di komentar per method): stok item = ledger
 * {@code sirs.detail_transaksi_pasien} x tanda {@code kode_transaksi_medis.jenis} (rumus PERSIS
 * dari {@code AmbilDataItemMedisBanyakBerdasarkanStok}); batch-kedaluwarsa = {@code sirs.kadaluarsa}
 * (qty per batch saat diterima); penjualan existing TIDAK memvalidasi kedaluwarsa -- validasi itu
 * (plus konsumsi batch {@link ApotikBatchKonsumsi} dan register terkendali
 * {@link ApotikNarkotikaLog}) adalah kontribusi FASE A.</p>
 *
 * <p>Aturan keras FASE A: obat kedaluwarsa TIDAK BISA terjual (ditolak server, bukan
 * peringatan); obat terkendali tanpa data register = SELURUH transaksi ditahan (rollback).</p>
 *
 * <p>Penjualan item jadi dilayani {@code apotik_bayar}; racikan memakai jalur khusus
 * {@code ApotikRacikanProduksiHelper} agar formula, konsumsi komponen, batch FEFO,
 * register terkendali, dan pembayaran tetap dibukukan atomik.</p>
 */
public final class ApotikApiHelper {

	private ApotikApiHelper() {
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString();
	}

	private static Long optLong(JSONObject r, String kunci) {
		if (r == null || r.isNull(kunci)) {
			return null;
		}
		try {
			return Long.valueOf((r.get(kunci) + "").trim());
		} catch (Exception e) {
			return null;
		}
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static JSONObject referensiKlinis(Long id, String kode, String nama) throws Exception {
		JSONObject hasil = new JSONObject();
		hasil.put("id", id == null ? JSONObject.NULL : id);
		hasil.put("kode", str(kode));
		hasil.put("nama", str(nama));
		return hasil;
	}

	private static JSONObject icdJson(Icd icd) throws Exception {
		if (icd == null) return null;
		return referensiKlinis(icd.getId(), icd.getKode(), icd.getNama_indonesia());
	}

	private static void tambahIcd(JSONArray tujuan, Icd icd) throws Exception {
		JSONObject nilai = icdJson(icd);
		if (nilai != null) tujuan.put(nilai);
	}

	private static String gabungAsal(String instalasi, String poli, String subPoli,
			String jenis, String sumber) {
		StringBuilder hasil = new StringBuilder();
		String[] nilai = new String[] { instalasi, poli, subPoli, jenis, sumber };
		for (String bagian : nilai) {
			if (bagian == null || bagian.trim().isEmpty()) continue;
			if (hasil.length() > 0) hasil.append(" / ");
			hasil.append(bagian.trim());
		}
		return hasil.toString();
	}

	/**
	 * Ringkasan identitas dan konteks klinis resep dari relasi SIRS existing.
	 * Tidak ada diagnosis, fasilitas, atau dokter yang ditebak dari nama obat.
	 */
	private static JSONObject informasiResep(Resep resep) throws Exception {
		JSONObject hasil = new JSONObject();
		DiagnosaPenyakit diagnosa = resep == null ? null : resep.getDiagnosaPenyakit();
		Pendaftaran pendaftaran = diagnosa == null ? null : diagnosa.getPendaftaran();
		Pasien pasien = diagnosa == null ? null : diagnosa.getPasien();
		if (pasien == null && pendaftaran != null) pasien = pendaftaran.getPasien();
		Dokter dokter = diagnosa == null ? null : diagnosa.getDokter();
		if (dokter == null && pendaftaran != null) dokter = pendaftaran.getDokter();

		java.text.SimpleDateFormat tanggal = new java.text.SimpleDateFormat("yyyy-MM-dd");
		java.text.SimpleDateFormat waktu = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
		Date tanggalResep = diagnosa == null ? null : diagnosa.getTanggal();
		if (tanggalResep == null && pendaftaran != null) tanggalResep = pendaftaran.getTanggalPendaftaran();
		hasil.put("tanggalResep", tanggalResep == null ? "" : waktu.format(tanggalResep));
		hasil.put("catatanResep", resep == null ? "" : str(resep.getKeterangan()));

		if (pasien == null) {
			hasil.put("pasien", JSONObject.NULL);
		} else {
			JSONObject p = referensiKlinis(pasien.getId(), pasien.getKode(), pasien.getNama());
			p.put("nomorRekamMedis", str(pasien.getKode()));
			p.put("jenisKelamin", str(pasien.getJenisKelamin()));
			p.put("tanggalLahir", pasien.getTanggalLahir() == null ? "" : tanggal.format(pasien.getTanggalLahir()));
			p.put("umur", pasien.getUmur() == null ? JSONObject.NULL : pasien.getUmur());
			p.put("tempatLahir", str(pasien.getTempatLahir()));
			p.put("alamat", str(pasien.getAlamatLengkap()).trim().isEmpty()
					? str(pasien.getAlamat()) : str(pasien.getAlamatLengkap()));
			p.put("telepon", str(pasien.getNoHp()).trim().isEmpty()
					? str(pasien.getNoTelp()) : str(pasien.getNoHp()));
			hasil.put("pasien", p);
		}

		if (dokter == null) {
			hasil.put("dokter", JSONObject.NULL);
		} else {
			JSONObject d = referensiKlinis(dokter.getId(), dokter.getKode(), dokter.getNama());
			d.put("kategori", str(dokter.getKategori()));
			d.put("alamatPraktik", str(dokter.getAlamat()));
			d.put("keterangan", str(dokter.getKeterangan()));
			hasil.put("dokter", d);
		}

		JSONObject dx = new JSONObject();
		dx.put("kode", diagnosa == null ? "" : str(diagnosa.getKode()));
		dx.put("ringkasan", diagnosa == null ? "" : str(diagnosa.getKeterangan()));
		dx.put("keluhanPasien", diagnosa == null ? "" : str(diagnosa.getKeluhanPasien()));
		dx.put("hasilAnamnesis", diagnosa == null ? "" : str(diagnosa.getKeluhanDiagnosa()));
		dx.put("kesimpulanPemeriksaan", diagnosa == null ? "" : str(diagnosa.getKesimpulanPemeriksaan()));
		dx.put("statusMenular", diagnosa == null ? "" : str(diagnosa.getApakahMenular()));
		JSONArray awal = new JSONArray();
		JSONArray akhir = new JSONArray();
		if (diagnosa != null) {
			tambahIcd(awal, diagnosa.getDiagnosaAwal1());
			tambahIcd(awal, diagnosa.getDiagnosaAwal2());
			tambahIcd(awal, diagnosa.getDiagnosaAwal3());
			tambahIcd(akhir, diagnosa.getDiagnosaAkhir1());
			tambahIcd(akhir, diagnosa.getDiagnosaAkhir2());
			tambahIcd(akhir, diagnosa.getDiagnosaAkhir3());
		}
		dx.put("icdAwal", awal);
		dx.put("icdAkhir", akhir);
		hasil.put("diagnosis", dx);

		String instalasi = diagnosa != null && diagnosa.getInstalasi() != null
				? str(diagnosa.getInstalasi().getNama()) : "";
		String poli = diagnosa != null && diagnosa.getPoly() != null
				? str(diagnosa.getPoly().getNama())
				: (pendaftaran != null && pendaftaran.getPoly() != null ? str(pendaftaran.getPoly().getNama()) : "");
		String subPoli = diagnosa != null && diagnosa.getSubpoly() != null
				? str(diagnosa.getSubpoly().getNama())
				: (pendaftaran != null && pendaftaran.getSubpoly() != null ? str(pendaftaran.getSubpoly().getNama()) : "");
		String jenis = pendaftaran == null ? "" : str(pendaftaran.getJenis());
		String sumber = pendaftaran == null ? "" : str(pendaftaran.getSumberPasien());
		JSONObject asal = new JSONObject();
		asal.put("instalasi", instalasi);
		asal.put("poli", poli);
		asal.put("subPoli", subPoli);
		asal.put("jenisPelayanan", jenis);
		asal.put("sumberPasien", sumber);
		asal.put("kodeKunjungan", pendaftaran == null ? "" : str(pendaftaran.getKode()));
		asal.put("tanggalKunjungan", pendaftaran == null || pendaftaran.getTanggalPendaftaran() == null
				? "" : waktu.format(pendaftaran.getTanggalPendaftaran()));
		asal.put("statusKunjungan", pendaftaran == null ? "" : str(pendaftaran.getStatusPendaftaran()));
		asal.put("dokterPengirim", pendaftaran == null ? "" : str(pendaftaran.getNamaDokterPengirim()));
		asal.put("penjamin", pendaftaran == null ? "" : str(pendaftaran.getNamaPenjamin()));
		asal.put("ringkasan", gabungAsal(instalasi, poli, subPoli, jenis, sumber));
		hasil.put("asalResep", asal);
		hasil.put("dataSample", resep != null && ("RSP-UJI-1".equals(resep.getKode())
				|| (resep.getKode() != null && resep.getKode().startsWith("RSP-DEMO-"))));
		return hasil;
	}

	/**
	 * Toko/apotek aktif pemanggil -- SATU-SATUNYA sumbu pemisah pasien antar apotek utk
	 * {@code AntreanFarmasi} (lihat javadoc {@link AntreanFarmasi#getTokoId()}).
	 *
	 * <p>Fail-closed dgn sengaja: hanya rantai sesi {@code user.getPedagang().getToko()}
	 * yang dipercaya. Payload TIDAK PERNAH lagi dipakai sbg sumber toko -- sebelum
	 * perbaikan ini, akun tanpa {@code Pedagang}/{@code Toko} (mayoritas pengguna modul
	 * rumah sakit/SIRS, yg memang tidak terikat ke Toko mana pun) jatuh ke fallback yg
	 * membaca {@code toko_id} kiriman klien mentah-mentah, sehingga siapa pun yg lolos
	 * gerbang menu kasar bisa memilih toko sembarang dan membaca nama pasien + nomor
	 * rekam medis apotek lain lewat {@code antreanFarmasiList}. Akibatnya, akun tanpa
	 * Pedagang/Toko kini ditolak pemanggil dgn "Toko/apotek aktif tidak diketahui" alih-
	 * alih diam-diam memakai toko pilihannya sendiri. Bila kelak memang ada pemakai sah
	 * yg perlu memilih toko (mis. penyelia lintas apotek), nilai dari payload HANYA boleh
	 * diterima setelah divalidasi bahwa akun tsb memang berhak atas toko itu -- jangan
	 * kembalikan ke pola baca-mentah lama.</p>
	 */
	private static Long tokoId(Tbmuser user, JSONObject request) {
		try {
			if (user != null && user.getPedagang() != null && user.getPedagang().getToko() != null) {
				return user.getPedagang().getToko().getId();
			}
		} catch (Exception ignored) { }
		return null;
	}

	private static boolean jenisAntreanValid(String jenis) {
		return AntreanFarmasi.JENIS_JADI.equals(jenis) || AntreanFarmasi.JENIS_RACIKAN.equals(jenis)
				|| AntreanFarmasi.JENIS_CAMPURAN.equals(jenis);
	}

	private static boolean statusAntreanValid(String status) {
		return AntreanFarmasi.STATUS_MENUNGGU.equals(status)
				|| AntreanFarmasi.STATUS_DISIAPKAN.equals(status)
				|| AntreanFarmasi.STATUS_SIAP.equals(status)
				|| AntreanFarmasi.STATUS_SELESAI.equals(status);
	}

	private static String samarkanNama(String nama) {
		if (nama == null || nama.trim().isEmpty()) return "PASIEN";
		String[] bagian = nama.trim().toUpperCase().split("\\s+");
		StringBuilder hasil = new StringBuilder();
		for (int i = 0; i < bagian.length; i++) {
			if (i > 0) hasil.append(' ');
			String b = bagian[i];
			hasil.append(b.substring(0, 1));
			for (int x = 1; x < b.length(); x++) hasil.append('*');
		}
		return hasil.toString();
	}

	private static String samarkanRm(String rm) {
		if (rm == null || rm.trim().isEmpty()) return "";
		String v = rm.trim();
		if (v.length() <= 4) return "****";
		return "****" + v.substring(v.length() - 4);
	}

	/** Awal hari ini (00:00) -- batch bertanggal kadaluarsa SEBELUM hari ini = kedaluwarsa. */
	private static Date awalHariIni() {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static boolean kedaluwarsa(Kadaluarsa k) {
		return k.getTanggalKadaluarsa() != null && k.getTanggalKadaluarsa().before(awalHariIni());
	}

	/** Aksi granular menu apotik -- fail-closed: crud kunci apotik default FALSE (lihat
	 *  EbisnisMenuKatalog.defaultObj + KUNCI_DEFAULT_NONAKTIF). Admin global (tanpa role) boleh. */
	/**
	 * Pintu paket untuk {@link #bolehAksi(Tbmuser, String, String)} -- dipakai
	 * {@code ApotikApiDispatcher} menyusun peta hak yang dikirim ke klien.
	 *
	 * <p>Sengaja package-private, bukan public: pemeriksaan ini tetap urusan internal
	 * paket {@code api}, dan membukanya lebih lebar hanya mengundang pemanggil yang
	 * memakainya sebagai gerbang -- padahal gerbang sebenarnya ada di tiap metode.</p>
	 */
	static boolean bolehAksiMenu(Tbmuser tbmuser, String kunciMenu, String aksi) {
		return bolehAksi(tbmuser, kunciMenu, aksi);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String kunciMenu, String aksi) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(EbisnisMenuKatalog.urai(role.getEbisnisMenu()), kunciMenu, aksi);
	}

	/**
	 * Gerbang BACA berbasis visibilitas menu -- BUKAN {@link #bolehAksi(Tbmuser, String, String)}.
	 * Kunci {@code apotik_resep}/{@code apotik_kasir} memang ada di {@code KUNCI_CRUD}, tapi grid
	 * CRUD-nya hanya pernah memuat lima aksi lama ({@code create/update/delete/approve/reject});
	 * "view" bukan salah satunya dan tidak pernah disimpan di sana, jadi memanggil
	 * {@code bolehAksi(user, kunci, "view")} akan SELALU ditolak (fail-closed permanen) utk siapa
	 * pun selain admin/role kosong -- bukan itu yg dimaksud. Yang benar-benar menandakan pengguna
	 * "boleh melihat apotek ini" adalah kunci {@code menu.<kunci>}, yg default TERTUTUP utk kedua
	 * kunci ini ({@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF}) sampai admin menyalakannya utk
	 * peran tsb. Pola identik {@code GrupProdukApiHelper.bolehLihat}.
	 */
	private static boolean bolehLihatMenu(Tbmuser tbmuser, String kunciMenu) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		JSONObject menu = EbisnisMenuKatalog.urai(role.getEbisnisMenu()).optJSONObject("menu");
		return menu != null && menu.optBoolean(kunciMenu, false);
	}

	/** Stok per item dari ledger -- SQL PERSIS pola AmbilDataItemMedisBanyakBerdasarkanStok
	 *  (sirs/helper, baris 340-346): SUM((qty+qty_bonus)*jenis), opsional per lokasi.
	 *  Package-visible: dipakai juga ApotikPersediaanHelper (opname hitung selisih). */
	/* package */ static java.util.Map<Long, Double> stokPerItem(Session session, List<Long> itemIds, Long lokasiId)
			throws Exception {
		java.util.Map<Long, Double> stok = new java.util.HashMap<Long, Double>();
		if (itemIds.isEmpty()) {
			return stok;
		}
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < itemIds.size(); i++) {
			if (i > 0) in.append(",");
			in.append(itemIds.get(i));
		}
		String sql = "select a.item, sum((a.qty+a.qty_bonus)*b.jenis) from sirs.detail_transaksi_pasien a "
				+ "inner join sirs.kode_transaksi_medis b on (a.kode_transaksi = b.id) "
				+ "where a.item in (" + in + ") "
				+ (lokasiId == null ? "" : " and a.lokasi = " + lokasiId + " ")
				+ "group by a.item";
		java.sql.PreparedStatement ps = session.connection().prepareStatement(sql);
		java.sql.ResultSet rs = ps.executeQuery();
		while (rs.next()) {
			stok.put(Long.valueOf(rs.getLong(1)), Double.valueOf(rs.getDouble(2)));
		}
		rs.close();
		ps.close();
		return stok;
	}

	/** Konsumsi batch ter-agregasi per kadaluarsa id (sisa = Kadaluarsa.qty - nilai peta ini). */
	/* package */ static java.util.Map<Long, Double> konsumsiPerBatch(Session session, List<Long> kadaluarsaIds) {
		java.util.Map<Long, Double> peta = new java.util.HashMap<Long, Double>();
		if (kadaluarsaIds.isEmpty()) {
			return peta;
		}
		@SuppressWarnings("unchecked")
		List<Object[]> rows = session.createQuery(
				"select bk.kadaluarsa.id, sum(bk.qty) from ApotikBatchKonsumsi bk "
						+ "where bk.kadaluarsa.id in (:ids) group by bk.kadaluarsa.id")
				.setParameterList("ids", kadaluarsaIds).list();
		for (Object[] row : rows) {
			peta.put((Long) row[0], row[1] == null ? Double.valueOf(0) : Double.valueOf(((Number) row[1]).doubleValue()));
		}
		return peta;
	}

	private static ApotikItemProfile profilItem(Session session, ItemMedis item) {
		return (ApotikItemProfile) session.createCriteria(ApotikItemProfile.class)
				.add(Restrictions.eq("item", item)).setMaxResults(1).uniqueResult();
	}

	// =============================================================================================
	// apotik_item_cari -- pencarian obat + stok + profil golongan/LASA (kasir & formularium)
	// =============================================================================================

	public static void itemCari(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;
		Long lokasiId = optLong(request, "lokasi_id");

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(ItemMedis.class);
			org.hibernate.Criteria hitung = session.createCriteria(ItemMedis.class);
			if (!keyword.isEmpty()) {
				c.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("barcode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%")));
				hitung.add(Restrictions.disjunction()
						.add(Restrictions.ilike("kode", "%" + keyword + "%"))
						.add(Restrictions.ilike("barcode", "%" + keyword + "%"))
						.add(Restrictions.ilike("nama", "%" + keyword + "%")));
			}
			long total = ((Number) hitung.setProjection(Projections.rowCount()).uniqueResult()).longValue();
			c.addOrder(Order.asc("nama"));
			c.setFirstResult((page - 1) * size);
			c.setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<ItemMedis> items = c.list();

			List<Long> ids = new java.util.ArrayList<Long>();
			for (ItemMedis it : items) {
				ids.add(it.getId());
			}
			java.util.Map<Long, Double> stok = stokPerItem(session, ids, lokasiId);
			java.util.Map<Long, ApotikItemProfile> profil = new java.util.HashMap<Long, ApotikItemProfile>();
			if (!ids.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<ApotikItemProfile> profiles = session.createCriteria(ApotikItemProfile.class)
						.createAlias("item", "item").add(Restrictions.in("item.id", ids)).list();
				for (ApotikItemProfile p : profiles) {
					profil.put(p.getItem().getId(), p);
				}
			}

			JSONArray arr = new JSONArray();
			for (ItemMedis it : items) {
				ApotikItemProfile p = profil.get(it.getId());
				JSONObject j = new JSONObject();
				j.put("id", it.getId());
				j.put("kode", str(it.getKode()));
				j.put("barcode", str(it.getBarcode()));
				j.put("nama", str(it.getNama()));
				j.put("satuan", it.getSatuanItem() == null ? "" : str(it.getSatuanItem().getNama()));
				j.put("jenisKode", it.getJenisItem() == null ? "" : str(it.getJenisItem().getKode()));
				j.put("jenisNama", it.getJenisItem() == null ? "" : str(it.getJenisItem().getNama()));
				j.put("bahanRacikan", str(it.getKode()).startsWith("DEMO-BHN-")
						|| (it.getJenisItem() != null && "BRC".equalsIgnoreCase(str(it.getJenisItem().getKode()))));
				j.put("kandungan", str(it.getKandungan()));
				j.put("hargaJual", it.getDefaultHargaJual() == null ? 0 : it.getDefaultHargaJual().doubleValue());
				j.put("stok", stok.containsKey(it.getId()) ? stok.get(it.getId()).doubleValue() : 0);
				j.put("golonganObat", p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat());
				j.put("terkendali", p != null && ApotikItemProfile.terkendali(p.getGolonganObat()));
				j.put("lasa", p != null && Boolean.TRUE.equals(p.getLasa()));
				// IR-01: atribut pembeda & penanda risiko utk kartu obat kasir.
				j.put("bentukSediaan", p == null ? "" : str(p.getBentukSediaan()));
				j.put("kekuatan", p == null ? "" : str(p.getKekuatan()));
				j.put("highAlert", p != null && Boolean.TRUE.equals(p.getHighAlert()));
				j.put("coldChain", p != null && Boolean.TRUE.equals(p.getColdChain()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("total", total);
			hasil.put("totalPages", total == 0 ? 0 : (long) Math.ceil(total / (double) size));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_item_batch -- daftar batch per item, urut FEFO; kedaluwarsa ditandai TAK BISA dipilih
	// =============================================================================================

	/**
	 * IR-07 -- daftar metode pembayaran yang boleh dipakai kasir apotik.
	 *
	 * <p>Memakai ULANG master {@code CaraPembayaranKoperasi} milik POS (tidak
	 * membuat master baru yang harus dipelihara terpisah): hanya yang berstatus
	 * aktif yang dikirim. UI WAJIB menampilkan metode dari daftar ini saja --
	 * tidak boleh menampilkan metode yang tidak dikonfigurasi server.</p>
	 */
	public static void caraBayarList(JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<ais.database.model.koperasi.CaraPembayaranKoperasi> daftar = session
					.createCriteria(ais.database.model.koperasi.CaraPembayaranKoperasi.class)
					.add(Restrictions.eq("aktif", Boolean.TRUE))
					.addOrder(Order.asc("nama")).list();
			JSONArray arr = new JSONArray();
			for (ais.database.model.koperasi.CaraPembayaranKoperasi cb : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", cb.getId());
				j.put("kode", str(cb.getKode()));
				j.put("nama", str(cb.getNama()));
				j.put("manual", Boolean.TRUE.equals(cb.getManual()));
				// Penentu kembalian: kasir apotik hanya boleh menampilkan kolom
				// "uang diterima" + kembalian bila metode ini MEMANG memberi
				// kembalian. Tanpa flag ini klien terpaksa menebak dari nama
				// (ilike "tunai") -- tebakan yg salah utk metode tunai bernama
				// lain. Dikirim apa adanya dari entitas, bukan dihitung ulang.
				j.put("adaKembalian", Boolean.TRUE.equals(cb.getAdaKembalian()));
				j.put("online", Boolean.TRUE.equals(cb.getOnline()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void itemBatch(JSONObject request, JSONObject hasil) throws Exception {
		Long itemId = optLong(request, "item_id");
		Long lokasiId = optLong(request, "lokasi_id");
		if (itemId == null) {
			tolak(hasil, "item_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
			if (item == null) {
				tolak(hasil, "Item tidak ditemukan.");
				return;
			}
			org.hibernate.Criteria c = session.createCriteria(Kadaluarsa.class)
					.add(Restrictions.eq("item", item));
			if (lokasiId != null) {
				c.createAlias("lokasi", "lokasi").add(Restrictions.eq("lokasi.id", lokasiId));
			}
			c.addOrder(Order.asc("tanggalKadaluarsa")); // FEFO: terdekat kedaluwarsa didahulukan
			@SuppressWarnings("unchecked")
			List<Kadaluarsa> batches = c.list();
			List<Long> ids = new java.util.ArrayList<Long>();
			for (Kadaluarsa k : batches) {
				ids.add(k.getId());
			}
			java.util.Map<Long, Double> konsumsi = konsumsiPerBatch(session, ids);
			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
			JSONArray arr = new JSONArray();
			for (Kadaluarsa k : batches) {
				double awal = k.getQty() == null ? 0 : k.getQty().doubleValue();
				Double pakai = konsumsi.get(k.getId());
				double sisa = awal - (pakai == null ? 0 : pakai.doubleValue());
				JSONObject j = new JSONObject();
				j.put("kadaluarsaId", k.getId());
				j.put("tanggalKadaluarsa",
						k.getTanggalKadaluarsa() == null ? "" : fmt.format(k.getTanggalKadaluarsa()));
				j.put("qtyAwal", awal);
				j.put("sisa", sisa);
				j.put("lokasiId", k.getLokasi() == null ? JSONObject.NULL : k.getLokasi().getId());
				j.put("kedaluwarsa", kedaluwarsa(k));
				// IR-02: status lot + alasan manusiawi bila tidak dapat dipilih.
				j.put("statusLot", k.getStatusLot());
				j.put("lotLayak", Kadaluarsa.lotLayak(k.getStatusLot()));
				String alasanLot = Kadaluarsa.alasanLotDitahan(k.getStatusLot());
				j.put("alasanLot", alasanLot == null ? "" : alasanLot);
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_resep_list / apotik_resep_detail -- tebus resep: pilih resep, bukan ketik obat
	// =============================================================================================

	public static void resepList(JSONObject request, JSONObject hasil) throws Exception {
		String keyword = request == null ? "" : request.optString("keyword", "").trim();
		boolean hanyaMenunggu = request == null || request.optBoolean("hanya_menunggu", true);
		int page = Math.max(1, request == null ? 1 : request.optInt("page", 1));
		int size = request == null ? 20 : request.optInt("page_size", 20);
		if (size < 1) size = 20;
		if (size > 100) size = 100;

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder syarat = new StringBuilder(" from Resep r where 1=1");
			if (!keyword.isEmpty()) syarat.append(" and lower(r.kode) like :keyword");
			if (hanyaMenunggu) {
				syarat.append(" and not exists (select tm.id from TransaksiMedis tm where tm.resep = r)");
			}
			org.hibernate.Query qJumlah = session.createQuery("select count(r)" + syarat.toString());
			org.hibernate.Query qData = session.createQuery("select r" + syarat.toString() + " order by r.id desc");
			if (!keyword.isEmpty()) {
				String pola = "%" + keyword.toLowerCase() + "%";
				qJumlah.setString("keyword", pola);
				qData.setString("keyword", pola);
			}
			long total = ((Number) qJumlah.uniqueResult()).longValue();
			qData.setFirstResult((page - 1) * size);
			qData.setMaxResults(size);
			@SuppressWarnings("unchecked")
			List<Resep> reseps = qData.list();

			// Status "sudah ditebus" = sudah ada TransaksiMedis yang menunjuk resep itu
			// (FACT_SOURCE: TransaksiMedis.resep FK; Resep sendiri tanpa kolom status).
			java.util.Set<Long> sudahDitebus = new java.util.HashSet<Long>();
			java.util.Map<Long, Long> jumlahDetail = new java.util.HashMap<Long, Long>();
			if (!reseps.isEmpty()) {
				List<Long> ids = new java.util.ArrayList<Long>();
				for (Resep r : reseps) {
					ids.add(r.getId());
				}
				@SuppressWarnings("unchecked")
				List<Long> tebus = session.createQuery(
						"select distinct tm.resep.id from TransaksiMedis tm where tm.resep.id in (:ids)")
						.setParameterList("ids", ids).list();
				sudahDitebus.addAll(tebus);
				@SuppressWarnings("unchecked")
				List<Object[]> hitungDetail = session.createQuery(
						"select rd.resep.id, count(rd) from ResepDetail rd where rd.resep.id in (:ids) group by rd.resep.id")
						.setParameterList("ids", ids).list();
				for (Object[] baris : hitungDetail) {
					jumlahDetail.put((Long) baris[0], Long.valueOf(((Number) baris[1]).longValue()));
				}
			}

			java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
			JSONArray arr = new JSONArray();
			for (Resep r : reseps) {
				boolean ditebus = sudahDitebus.contains(r.getId());
				JSONObject j = new JSONObject();
				j.put("id", r.getId());
				j.put("kode", str(r.getKode()));
				j.put("keterangan", str(r.getKeterangan()));
				j.put("ditebus", ditebus);
				j.put("jumlahBaris", jumlahDetail.containsKey(r.getId())
						? jumlahDetail.get(r.getId()).longValue() : 0);
				try {
					JSONObject informasi = informasiResep(r);
					JSONObject pasien = informasi.optJSONObject("pasien");
					JSONObject dokter = informasi.optJSONObject("dokter");
					JSONObject diagnosa = informasi.optJSONObject("diagnosis");
					JSONObject asal = informasi.optJSONObject("asalResep");
					j.put("diagnosa", diagnosa == null ? "" : diagnosa.optString("ringkasan", ""));
					j.put("indikasi", diagnosa == null ? "" : diagnosa.optString("kesimpulanPemeriksaan",
							diagnosa.optString("ringkasan", "")));
					j.put("pasienNama", pasien == null ? "" : pasien.optString("nama", ""));
					j.put("nomorRekamMedis", pasien == null ? "" : pasien.optString("nomorRekamMedis", ""));
					j.put("dokterNama", dokter == null ? "" : dokter.optString("nama", ""));
					j.put("asalPelayanan", asal == null ? "" : asal.optString("ringkasan", ""));
					j.put("tanggalResep", informasi.optString("tanggalResep", ""));
					j.put("dataSample", informasi.optBoolean("dataSample", false));
				} catch (Exception e) {
					j.put("diagnosa", "");
					j.put("indikasi", "");
					j.put("pasienNama", "");
					j.put("nomorRekamMedis", "");
					j.put("dokterNama", "");
					j.put("asalPelayanan", "");
					j.put("tanggalResep", "");
					j.put("dataSample", false);
				}
				j.put("oleh", str(r.getOleh()));
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("page", page);
			hasil.put("pageSize", size);
			hasil.put("total", total);
			hasil.put("totalPages", total == 0 ? 0 : (long) Math.ceil(total / (double) size));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void resepDetail(JSONObject request, JSONObject hasil) throws Exception {
		Long resepId = optLong(request, "resep_id");
		if (resepId == null) {
			tolak(hasil, "resep_id wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Resep resep = (Resep) session.get(Resep.class, resepId);
			if (resep == null) {
				tolak(hasil, "Resep tidak ditemukan.");
				return;
			}
			@SuppressWarnings("unchecked")
			List<ResepDetail> details = session.createCriteria(ResepDetail.class)
					.add(Restrictions.eq("resep", resep)).addOrder(Order.asc("id")).list();
			boolean adaRacikan = false;
			List<Long> itemIds = new java.util.ArrayList<Long>();
			for (ResepDetail d : details) {
				if (d.getItem() != null) {
					itemIds.add(d.getItem().getId());
				}
			}
			java.util.Map<Long, Double> stok = stokPerItem(session, itemIds, null);
			JSONArray arr = new JSONArray();
			for (ResepDetail d : details) {
				JSONObject j = new JSONObject();
				j.put("resepDetailId", d.getId());
				j.put("jumlah", d.getJumlah() == null ? 0 : d.getJumlah().doubleValue());
				j.put("keterangan", str(d.getKeterangan()));
				if (d.getRacikan() != null) {
					adaRacikan = true;
					JSONObject ringkas = ApotikRacikanProduksiHelper.ringkasRacikan(
							session, d.getRacikan(), null);
					for (java.util.Iterator<?> it = ringkas.keys(); it.hasNext();) {
						String kunci = str(it.next());
						j.put(kunci, ringkas.get(kunci));
					}
				} else if (d.getItem() != null) {
					ItemMedis it = d.getItem();
					ApotikItemProfile p = profilItem(session, it);
					j.put("racikan", false);
					j.put("itemId", it.getId());
					j.put("kode", str(it.getKode()));
					j.put("nama", str(it.getNama()));
					j.put("satuan", it.getSatuanItem() == null ? "" : str(it.getSatuanItem().getNama()));
					j.put("hargaJual", it.getDefaultHargaJual() == null ? 0 : it.getDefaultHargaJual().doubleValue());
					j.put("stok", stok.containsKey(it.getId()) ? stok.get(it.getId()).doubleValue() : 0);
					j.put("golonganObat", p == null ? ApotikItemProfile.GOLONGAN_BEBAS : p.getGolonganObat());
					j.put("terkendali", p != null && ApotikItemProfile.terkendali(p.getGolonganObat()));
					j.put("bentukSediaan", p == null ? "" : str(p.getBentukSediaan()));
					j.put("kekuatan", p == null ? "" : str(p.getKekuatan()));
					j.put("highAlert", p != null && Boolean.TRUE.equals(p.getHighAlert()));
					j.put("coldChain", p != null && Boolean.TRUE.equals(p.getColdChain()));
					j.put("lasa", p != null && Boolean.TRUE.equals(p.getLasa()));
				}
				arr.put(j);
			}
			hasil.put("status", "00");
			hasil.put("resepId", resep.getId());
			hasil.put("kode", str(resep.getKode()));
			hasil.put("data", arr);
			JSONObject informasi = informasiResep(resep);
			hasil.put("informasiResep", informasi);
			JSONObject klinis = profilKeselamatanResep(session, resep, details);
			hasil.put("pasien", informasi.opt("pasien"));
			hasil.put("telaahKlinis", klinis);
			// Klien baru memakai ringkasan racikan di atas untuk tebus resep campuran;
			// flag dipertahankan agar klien lama tetap dapat memberi peringatan.
			hasil.put("adaRacikan", adaRacikan);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// Antrean farmasi -- konsol petugas + layar publik obat jadi/racikan
	// =============================================================================================

	/**
	 * Daftar antrean hari ini. Payload {@code untuk_layar=true} selalu menyamarkan identitas.
	 *
	 * <p>Gerbang baca: {@link #bolehLihatMenu(Tbmuser, String)} atas {@code apotik_resep} ATAU
	 * {@code apotik_kasir} -- sebelumnya method ini sama sekali tidak memeriksa hak apa pun
	 * (berbeda dari simpan/status/hapus yg sudah menjaga lewat {@link #bolehAksi}), dan satu-
	 * satunya penyaring adalah gerbang menu kasar {@code PosApi.bolehAksesActionKantin} di
	 * dispatcher. Digabung dgn fallback lama pada {@link #tokoId} yg kini sudah ditutup, celah
	 * itu dulu membuat siapa pun yg lolos gerbang menu kasar bisa memilih toko sembarang dan
	 * membaca nama pasien + nomor rekam medis apotek lain.</p>
	 *
	 * <p><b>Keputusan default {@code untuk_layar}.</b> Bentuk paling sensitif (identitas
	 * terang) adalah bentuk default (dipakai saat {@code untuk_layar} tidak dikirim), bukan
	 * bentuk tersamarkan -- sengaja TIDAK dibalik di perbaikan ini krn akan memutus konsol
	 * petugas yg memang menampilkan identitas asli dan tidak selalu mengirim flag ini secara
	 * eksplisit. Risiko sisa dari pilihan ini kini ditahan oleh gerbang baca di atas (hanya
	 * akun berhak apotik yg bisa memanggil endpoint ini sama sekali), bukan lagi oleh flag ini.</p>
	 */
	public static void antreanFarmasiList(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = tokoId(user, request);
		if (tokoId == null) { tolak(hasil, "Toko/apotek aktif tidak diketahui."); return; }
		if (!bolehLihatMenu(user, "apotik_resep") && !bolehLihatMenu(user, "apotik_kasir")) {
			tolak(hasil, "Akun tidak berhak melihat antrean farmasi.");
			return;
		}
		boolean layar = request != null && request.optBoolean("untuk_layar", false);
		String jenis = request == null ? "" : request.optString("jenis", "").trim().toUpperCase();
		Date awal = awalHariIni();
		java.util.Calendar besok = java.util.Calendar.getInstance();
		besok.setTime(awal);
		besok.add(java.util.Calendar.DAY_OF_MONTH, 1);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			org.hibernate.Criteria c = session.createCriteria(AntreanFarmasi.class)
					.add(Restrictions.eq("tokoId", tokoId))
					.add(Restrictions.ge("tanggalDibuat", awal))
					.add(Restrictions.lt("tanggalDibuat", besok.getTime()));
			if (jenisAntreanValid(jenis)) c.add(Restrictions.eq("jenis", jenis));
			if (layar) c.add(Restrictions.ne("status", AntreanFarmasi.STATUS_SELESAI));
			c.addOrder(Order.asc("urutan")).addOrder(Order.asc("id"));
			@SuppressWarnings("unchecked")
			List<AntreanFarmasi> daftar = c.list();
			JSONArray data = new JSONArray();
			for (AntreanFarmasi a : daftar) {
				JSONObject j = new JSONObject();
				j.put("id", a.getId());
				j.put("kodeAntrean", str(a.getKodeAntrean()));
				j.put("namaPasien", layar ? samarkanNama(a.getNamaPasien()) : str(a.getNamaPasien()));
				j.put("nomorRekamMedis", layar ? samarkanRm(a.getNomorRekamMedis()) : str(a.getNomorRekamMedis()));
				j.put("jenis", str(a.getJenis()));
				j.put("status", str(a.getStatus()));
				j.put("loket", str(a.getLoket()));
				j.put("catatanPublik", str(a.getCatatanPublik()));
				j.put("resepId", a.getResepId() == null ? JSONObject.NULL : a.getResepId());
				j.put("urutan", a.getUrutan() == null ? 0 : a.getUrutan());
				j.put("waktuMasuk", a.getTanggalDibuat() == null ? ""
						: new java.text.SimpleDateFormat("HH:mm").format(a.getTanggalDibuat()));
				JSONArray obat = new JSONArray();
				try { obat = new JSONArray(a.getDaftarObat() == null ? "[]" : a.getDaftarObat()); }
				catch (Exception ignored) { }
				j.put("obat", obat);
				data.put(j);
			}
			hasil.put("status", "00");
			hasil.put("data", data);
			hasil.put("total", data.length());
			hasil.put("privasi", layar ? "IDENTITAS_DISAMARKAN" : "KONSOL_PETUGAS");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ringkasan keselamatan resep dari model SIRS existing. Pemeriksaan otomatis
	 * sengaja hanya menyatakan kecocokan yang eksak melalui FK
	 * {@link AlergiPasien#getItemMedis()}; alergi teks bebas tidak ditebak dan
	 * tidak pernah menghasilkan klaim "aman" karena basis pengetahuan interaksi
	 * serta dosis belum dikonfigurasi.
	 */
	@SuppressWarnings("unchecked")
	private static JSONObject profilKeselamatanResep(Session session, Resep resep,
			List<ResepDetail> details) throws Exception {
		JSONObject hasil = new JSONObject();
		hasil.put("evaluasiOtomatisLengkap", false);
		hasil.put("basisPengetahuanInteraksiTersedia", false);
		hasil.put("kesimpulan", "PERLU_TELAAH_APOTEKER");
		JSONArray alergiJson = new JSONArray();
		JSONArray peringatan = new JSONArray();
		DiagnosaPenyakit diagnosa = resep.getDiagnosaPenyakit();
		Pasien pasien = diagnosa == null ? null : diagnosa.getPasien();
		if (pasien == null) {
			hasil.put("pasien", JSONObject.NULL);
			hasil.put("alergiAktif", alergiJson);
			peringatan.put(new JSONObject()
					.put("tingkat", "PERINGATAN")
					.put("kode", "PROFIL_PASIEN_BELUM_TERHUBUNG")
					.put("pesan", "Profil pasien belum terhubung ke resep; alergi wajib dikonfirmasi manual."));
			hasil.put("peringatan", peringatan);
			return hasil;
		}

		JSONObject informasi = informasiResep(resep);
		hasil.put("pasien", informasi.opt("pasien"));
		hasil.put("informasiResep", informasi);

		java.util.Set<Long> itemResep = new java.util.HashSet<Long>();
		for (ResepDetail detail : details) {
			if (detail.getItem() != null) itemResep.add(detail.getItem().getId());
		}
		List<AlergiPasien> alergi = session.createCriteria(AlergiPasien.class)
				.add(Restrictions.eq("pasien", pasien))
				.add(Restrictions.eq("statusKlinis", AlergiPasien.STATUS_AKTIF))
				.addOrder(Order.desc("tanggalCatat")).list();
		boolean cocokEksak = false;
		for (AlergiPasien a : alergi) {
			boolean cocok = a.getItemMedis() != null && itemResep.contains(a.getItemMedis().getId());
			cocokEksak = cocokEksak || cocok;
			JSONObject baris = new JSONObject();
			baris.put("id", a.getId());
			baris.put("kategori", str(a.getKategori()));
			baris.put("substansi", str(a.getSubstansi()));
			baris.put("reaksi", str(a.getReaksi()));
			baris.put("keparahan", str(a.getKeparahan()));
			baris.put("cocokEksakDenganResep", cocok);
			baris.put("itemId", a.getItemMedis() == null ? JSONObject.NULL : a.getItemMedis().getId());
			alergiJson.put(baris);
			if (cocok) {
				peringatan.put(new JSONObject()
						.put("tingkat", "BAHAYA")
						.put("kode", "ALERGI_OBAT_COCOK_EKSAK")
						.put("pesan", "Alergi aktif cocok dengan item resep: " + str(a.getSubstansi())));
			}
		}
		if (!alergi.isEmpty() && !cocokEksak) {
			peringatan.put(new JSONObject()
					.put("tingkat", "PERINGATAN")
					.put("kode", "ALERGI_AKTIF_PERLU_VERIFIKASI")
					.put("pesan", "Pasien memiliki alergi aktif; substansi teks wajib diverifikasi apoteker."));
		}
		peringatan.put(new JSONObject()
				.put("tingkat", "INFO")
				.put("kode", "TELAAH_INTERAKSI_MANUAL")
				.put("pesan", "Interaksi, duplikasi terapi, dan dosis belum dinilai otomatis."));
		hasil.put("alergiAktif", alergiJson);
		hasil.put("peringatan", peringatan);
		if (cocokEksak) hasil.put("kesimpulan", "ALERGI_TERDETEKSI");
		return hasil;
	}

	/** Buat atau perbarui satu antrean; daftar obat hanya berisi nama/jumlah yang layak ditampilkan. */
	public static void antreanFarmasiSimpan(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = tokoId(user, request);
		if (tokoId == null) { tolak(hasil, "Toko/apotek aktif tidak diketahui."); return; }
		Long id = optLong(request, "id");
		String nama = request == null ? "" : request.optString("nama_pasien", "").trim();
		String jenis = request == null ? "" : request.optString("jenis", "").trim().toUpperCase();
		if (nama.isEmpty()) { tolak(hasil, "Nama pasien wajib diisi pada konsol petugas."); return; }
		if (!jenisAntreanValid(jenis)) { tolak(hasil, "Jenis antrean wajib JADI, RACIKAN, atau CAMPURAN."); return; }
		if (id == null && !bolehAksi(user, "apotik_resep", "create")
				&& !bolehAksi(user, "apotik_kasir", "create")) {
			tolak(hasil, "Akun tidak berhak menambah antrean farmasi."); return;
		}
		if (id != null && !bolehAksi(user, "apotik_resep", "update")
				&& !bolehAksi(user, "apotik_kasir", "update")) {
			tolak(hasil, "Akun tidak berhak mengubah antrean farmasi."); return;
		}
		JSONArray obat = request == null ? new JSONArray() : request.optJSONArray("obat");
		if (obat == null) obat = new JSONArray();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		try {
			AntreanFarmasi a = id == null ? new AntreanFarmasi()
					: (AntreanFarmasi) session.get(AntreanFarmasi.class, id);
			if (a == null || (a.getTokoId() != null && !tokoId.equals(a.getTokoId()))) {
				tolak(hasil, "Antrean tidak ditemukan pada apotek aktif."); tx.rollback(); return;
			}
			if (id == null) {
				a.setTokoId(tokoId);
				a.setTanggalDibuat(new Date());
				a.setStatus(AntreanFarmasi.STATUS_MENUNGGU);
				Number maks = (Number) session.createQuery(
						"select max(a.urutan) from AntreanFarmasi a where a.tokoId=:toko and a.tanggalDibuat>=:awal")
						.setLong("toko", tokoId.longValue()).setTimestamp("awal", awalHariIni()).uniqueResult();
				int urutan = maks == null ? 1 : maks.intValue() + 1;
				a.setUrutan(Integer.valueOf(urutan));
				String kode = request.optString("kode_antrean", "").trim().toUpperCase();
				a.setKodeAntrean(kode.isEmpty() ? String.format("F%03d", Integer.valueOf(urutan)) : kode);
			}
			a.setResepId(optLong(request, "resep_id"));
			a.setNamaPasien(nama);
			a.setNomorRekamMedis(request.optString("nomor_rekam_medis", "").trim());
			a.setJenis(jenis);
			a.setLoket(request.optString("loket", "").trim());
			a.setCatatanPublik(request.optString("catatan_publik", "").trim());
			a.setDaftarObat(obat.toString());
			a.setOleh(user == null ? "" : str(user.getUserNama()));
			a.setOlehId(user == null ? "" : str(user.getUserId()));
			session.saveOrUpdate(a);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", a.getId());
			hasil.put("kodeAntrean", a.getKodeAntrean());
		} catch (Exception e) {
			if (tx.isActive()) tx.rollback();
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Ubah tahap pelayanan tanpa memperbolehkan nilai status bebas. */
	public static void antreanFarmasiStatus(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = tokoId(user, request);
		Long id = optLong(request, "id");
		String status = request == null ? "" : request.optString("status", "").trim().toUpperCase();
		if (tokoId == null || id == null) { tolak(hasil, "Toko dan id antrean wajib diisi."); return; }
		if (!statusAntreanValid(status)) { tolak(hasil, "Status antrean tidak valid."); return; }
		if (!bolehAksi(user, "apotik_resep", "update") && !bolehAksi(user, "apotik_kasir", "update")) {
			tolak(hasil, "Akun tidak berhak mengubah status antrean."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		try {
			AntreanFarmasi a = (AntreanFarmasi) session.get(AntreanFarmasi.class, id);
			if (a == null || !tokoId.equals(a.getTokoId())) {
				tolak(hasil, "Antrean tidak ditemukan pada apotek aktif."); tx.rollback(); return;
			}
			a.setStatus(status);
			session.update(a);
			tx.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			if (tx.isActive()) tx.rollback();
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void antreanFarmasiHapus(Tbmuser user, JSONObject request, JSONObject hasil) throws Exception {
		Long tokoId = tokoId(user, request);
		Long id = optLong(request, "id");
		if (tokoId == null || id == null) { tolak(hasil, "Toko dan id antrean wajib diisi."); return; }
		if (!bolehAksi(user, "apotik_resep", "delete") && !bolehAksi(user, "apotik_kasir", "delete")) {
			tolak(hasil, "Akun tidak berhak menghapus antrean."); return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();
		try {
			AntreanFarmasi a = (AntreanFarmasi) session.get(AntreanFarmasi.class, id);
			if (a == null || !tokoId.equals(a.getTokoId())) {
				tolak(hasil, "Antrean tidak ditemukan pada apotek aktif."); tx.rollback(); return;
			}
			session.delete(a);
			tx.commit();
			hasil.put("status", "00");
		} catch (Exception e) {
			if (tx.isActive()) tx.rollback();
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_item_profil_simpan -- golongan obat + LASA (formularium)
	// =============================================================================================

	public static void itemProfilSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if (!bolehAksi(tbmuser, "apotik_formularium", "update")
				&& !bolehAksi(tbmuser, "apotik_formularium", "create")) {
			tolak(hasil, "Akun Anda tidak berhak mengubah profil obat (Formularium).");
			return;
		}
		Long itemId = optLong(request, "item_id");
		String golongan = request == null ? null : request.optString("golongan_obat", "").trim();
		if (itemId == null) {
			tolak(hasil, "item_id wajib diisi.");
			return;
		}
		if (golongan == null || golongan.isEmpty() || !ApotikItemProfile.golonganValid(golongan)) {
			tolak(hasil, "golongan_obat wajib salah satu: BEBAS, BEBAS_TERBATAS, KERAS, NARKOTIKA, PSIKOTROPIKA.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
			if (item == null) {
				tolak(hasil, "Item tidak ditemukan.");
				return;
			}
			tx = session.beginTransaction();
			ApotikItemProfile p = profilItem(session, item);
			if (p == null) {
				p = new ApotikItemProfile();
				p.setItem(item);
			}
			p.setGolonganObat(golongan);
			if (!request.isNull("lasa")) {
				p.setLasa(Boolean.valueOf(request.optBoolean("lasa", false)));
			}
			if (!request.isNull("keterangan")) {
				p.setKeterangan(request.optString("keterangan", "").trim());
			}
			// IR-01: atribut pembeda & penanda risiko. Pola sama seperti lasa --
			// hanya disentuh bila field DIKIRIM, sehingga klien lama yang tidak
			// mengenal field ini tidak menghapus nilai yang sudah diisi.
			if (!request.isNull("bentuk_sediaan")) {
				p.setBentukSediaan(request.optString("bentuk_sediaan", "").trim());
			}
			if (!request.isNull("kekuatan")) {
				p.setKekuatan(request.optString("kekuatan", "").trim());
			}
			if (!request.isNull("high_alert")) {
				p.setHighAlert(Boolean.valueOf(request.optBoolean("high_alert", false)));
			}
			if (!request.isNull("cold_chain")) {
				p.setColdChain(Boolean.valueOf(request.optBoolean("cold_chain", false)));
			}
			p.setOleh(tbmuser.getUserId());
			p.setOlehId(tbmuser.getUserId());
			session.saveOrUpdate(p);
			tx.commit();
			hasil.put("status", "00");
			hasil.put("profilId", p.getId());
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// =============================================================================================
	// apotik_bayar -- penjualan atomic: validasi kedaluwarsa MENAHAN, terkendali MENAHAN
	// =============================================================================================

	public static void bayar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray items = request == null ? null : request.optJSONArray("items");
		if (items == null || items.length() == 0) {
			tolak(hasil, "Minimal satu baris obat.");
			return;
		}
		Long lokasiId = optLong(request, "lokasi_id");
		Long resepId = optLong(request, "resep_id");
		JSONObject pembeli = request.optJSONObject("pembeli");
		String namaPembeli = pembeli == null ? "" : pembeli.optString("nama", "").trim();
		String alamatPembeli = pembeli == null ? "" : pembeli.optString("alamat", "").trim();
		String namaDokter = request.optString("nama_dokter", "").trim();
		String kodeIdem = request.optString("kode", "").trim();

		// Jangan bergantung pada seed/startup: server demo maupun tenant baru harus
		// dapat melakukan transaksi pertama secara deterministik. Helper ini tetap
		// fail-closed bila pembuatan kode ledger sungguh gagal.
		Long apotikJualId = ApotikKodeTransaksiHelper.pastikanId("AJ", "Apotik Jual", -1);
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null;
		try {
			KodeTransaksiMedis apotikJual = (KodeTransaksiMedis) session.get(
					KodeTransaksiMedis.class, apotikJualId);
			if (apotikJual == null) {
				tolak(hasil, "Kode transaksi 'apotik jual' tidak dapat dimuat. Transaksi ditahan.");
				return;
			}
			// Idempoten: retry perangkat dgn kode yang sama TIDAK membuat transaksi kedua.
			if (!kodeIdem.isEmpty()) {
				TransaksiMedis sudahAda = (TransaksiMedis) session.createCriteria(TransaksiMedis.class)
						.add(Restrictions.eq("kode", kodeIdem)).setMaxResults(1).uniqueResult();
				if (sudahAda != null) {
					hasil.put("status", "00");
					hasil.put("id", sudahAda.getId());
					hasil.put("kode", str(sudahAda.getKode()));
					hasil.put("idempoten", true);
					return;
				}
			}

			Resep resep = null;
			if (resepId != null) {
				resep = (Resep) session.get(Resep.class, resepId);
				if (resep == null) {
					tolak(hasil, "Resep tidak ditemukan.");
					return;
				}
			}
			Object lokasi = lokasiId == null ? null
					: session.get(ais.database.model.asset.Lokasi.class, lokasiId);

			// ---- Muat & validasi SELURUH baris dulu (fail-fast sebelum menulis apa pun) ----
			List<ItemMedis> itemList = new java.util.ArrayList<ItemMedis>();
			List<ApotikItemProfile> profilList = new java.util.ArrayList<ApotikItemProfile>();
			List<Double> qtyList = new java.util.ArrayList<Double>();
			List<Double> hargaList = new java.util.ArrayList<Double>();
			List<Double> diskonList = new java.util.ArrayList<Double>();
			List<List<Kadaluarsa>> batchList = new java.util.ArrayList<List<Kadaluarsa>>();
			List<List<Double>> batchQtyList = new java.util.ArrayList<List<Double>>();
			List<Long> semuaItemId = new java.util.ArrayList<Long>();

			for (int i = 0; i < items.length(); i++) {
				JSONObject baris = items.getJSONObject(i);
				Long itemId = optLong(baris, "item_id");
				double qty = baris.optDouble("qty", 0);
				if (itemId == null || qty <= 0) {
					tolak(hasil, "Baris " + (i + 1) + ": item_id dan qty (>0) wajib.");
					return;
				}
				ItemMedis item = (ItemMedis) session.get(ItemMedis.class, itemId);
				if (item == null) {
					tolak(hasil, "Baris " + (i + 1) + ": item tidak ditemukan.");
					return;
				}
				ApotikItemProfile profil = profilItem(session, item);
				String golongan = profil == null ? ApotikItemProfile.GOLONGAN_BEBAS : profil.getGolonganObat();

				// Obat terkendali: register WAJIB bisa dibuat -- tanpa identitas pembeli dan
				// (resep ATAU nama dokter), transaksi DITAHAN. Bukan peringatan.
				if (ApotikItemProfile.terkendali(golongan)) {
					if (namaPembeli.isEmpty()) {
						tolak(hasil, "\"" + str(item.getNama()) + "\" adalah obat terkendali (" + golongan
								+ "): nama pembeli/pasien WAJIB untuk register. Transaksi ditahan.");
						return;
					}
					if (resep == null && namaDokter.isEmpty()) {
						tolak(hasil, "\"" + str(item.getNama()) + "\" adalah obat terkendali (" + golongan
								+ "): wajib resep atau nama dokter penulis. Transaksi ditahan.");
						return;
					}
				}

				// Batch: bila item PUNYA catatan batch-kedaluwarsa, penjualan WAJIB memilih batch
				// (FEFO disarankan klien; server menegakkan sisa & tanggal). Item lama tanpa
				// catatan batch tetap bisa dijual (data historis tidak menghalangi operasional).
				long jumlahBatch = ((Number) session.createQuery(
						"select count(k) from Kadaluarsa k where k.item.id = :id")
						.setParameter("id", item.getId()).uniqueResult()).longValue();
				JSONArray batchJson = baris.optJSONArray("batch");
				List<Kadaluarsa> batchTerpilih = new java.util.ArrayList<Kadaluarsa>();
				List<Double> batchQty = new java.util.ArrayList<Double>();
				if (jumlahBatch > 0) {
					if (batchJson == null || batchJson.length() == 0) {
						tolak(hasil, "\"" + str(item.getNama())
								+ "\" ber-batch: pilih batch (FEFO) sebelum menjual.");
						return;
					}
					double totalBatch = 0;
					for (int b = 0; b < batchJson.length(); b++) {
						JSONObject bj = batchJson.getJSONObject(b);
						Long kadId = optLong(bj, "kadaluarsa_id");
						double bq = bj.optDouble("qty", 0);
						if (kadId == null || bq <= 0) {
							tolak(hasil, "Batch tidak valid pada \"" + str(item.getNama()) + "\".");
							return;
						}
						Kadaluarsa k = (Kadaluarsa) session.get(Kadaluarsa.class, kadId);
						if (k == null || k.getItem() == null || !k.getItem().getId().equals(item.getId())) {
							tolak(hasil, "Batch bukan milik item \"" + str(item.getNama()) + "\".");
							return;
						}
						// ATURAN KERAS (IR-02): lot karantina/recall/rusak/ditahan TIDAK BISA
						// terjual -- sejajar dengan aturan kedaluwarsa di bawah. Penahan,
						// bukan peringatan; UI tidak boleh melewatinya.
						if (!Kadaluarsa.lotLayak(k.getStatusLot())) {
							tolak(hasil, "DITOLAK: "
									+ Kadaluarsa.alasanLotDitahan(k.getStatusLot())
									+ " pada batch \"" + str(item.getNama())
									+ "\" -- tidak boleh dijual.");
							return;
						}
						// ATURAN KERAS: kedaluwarsa TIDAK BISA terjual. Penahan, bukan peringatan.
						if (kedaluwarsa(k)) {
							java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
							tolak(hasil, "DITOLAK: batch \"" + str(item.getNama()) + "\" kedaluwarsa "
									+ (k.getTanggalKadaluarsa() == null ? "-" : fmt.format(k.getTanggalKadaluarsa()))
									+ " tidak boleh dijual sama sekali.");
							return;
						}
						java.util.Map<Long, Double> pakai = konsumsiPerBatch(session,
								java.util.Collections.singletonList(k.getId()));
						double sisa = (k.getQty() == null ? 0 : k.getQty().doubleValue())
								- (pakai.containsKey(k.getId()) ? pakai.get(k.getId()).doubleValue() : 0);
						if (bq > sisa) {
							tolak(hasil, "Sisa batch \"" + str(item.getNama()) + "\" hanya "
									+ sisa + ", diminta " + bq + ".");
							return;
						}
						batchTerpilih.add(k);
						batchQty.add(Double.valueOf(bq));
						totalBatch += bq;
					}
					if (Math.abs(totalBatch - qty) > 0.0001) {
						tolak(hasil, "Jumlah batch (" + totalBatch + ") harus sama dgn qty ("
								+ qty + ") pada \"" + str(item.getNama()) + "\".");
						return;
					}
				}

				double harga = baris.optDouble("harga_satuan",
						item.getDefaultHargaJual() == null ? 0 : item.getDefaultHargaJual().doubleValue());
				double diskon = baris.optDouble("diskon", 0);

				itemList.add(item);
				profilList.add(profil);
				qtyList.add(Double.valueOf(qty));
				hargaList.add(Double.valueOf(harga));
				diskonList.add(Double.valueOf(diskon));
				batchList.add(batchTerpilih);
				batchQtyList.add(batchQty);
				semuaItemId.add(item.getId());
			}

			// Stok ledger cukup? (per item, akumulasi qty baris duplikat)
			java.util.Map<Long, Double> stok = stokPerItem(session, semuaItemId, lokasiId);
			java.util.Map<Long, Double> butuh = new java.util.HashMap<Long, Double>();
			for (int i = 0; i < itemList.size(); i++) {
				Long id = itemList.get(i).getId();
				Double b = butuh.get(id);
				butuh.put(id, Double.valueOf((b == null ? 0 : b.doubleValue()) + qtyList.get(i).doubleValue()));
			}
			for (java.util.Map.Entry<Long, Double> e : butuh.entrySet()) {
				double ada = stok.containsKey(e.getKey()) ? stok.get(e.getKey()).doubleValue() : 0;
				if (e.getValue().doubleValue() > ada) {
					tolak(hasil, "Stok tidak cukup utk item id " + e.getKey() + " (stok " + ada
							+ ", diminta " + e.getValue() + ").");
					return;
				}
			}

			// ---- Tulis SEMUA dalam SATU transaksi -- gagal satu = batal semua ----
			tx = session.beginTransaction();
			TransaksiMedis trx = new TransaksiMedis();
			trx.setKode(kodeIdem.isEmpty() ? "APT" + System.currentTimeMillis() : kodeIdem);
			// jenis_transaksi NOT NULL (kolom wajib) -- penjualan item apotek = TRX_ITEM, sama
			// dgn jalur rumah sakit (TransaksiAction). sumber=APOTIK menandai asal transaksi.
			trx.setJenisTransaksi(TransaksiMedis.TRX_ITEM);
			trx.setSumber(TransaksiMedis.SUMBER_APOTIK);
			trx.setBebas(Boolean.TRUE);
			trx.setLunas(Boolean.TRUE);
			trx.setTanggalTransaksi(new Date());
			if (resep != null) {
				trx.setResep(resep);
			}
			if (!namaPembeli.isEmpty()) {
				trx.setNama(namaPembeli);
			}
			if (!alamatPembeli.isEmpty()) {
				trx.setAlamat(alamatPembeli);
			}
			if (lokasi != null) {
				trx.setLokasi((ais.database.model.asset.Lokasi) lokasi);
			}
			// Keterbatasan FASE A (disengaja): entity Pembayaran SIRS belum dibuat di sini --
			// dicatat tunai lunas; integrasi kasir Pembayaran menyusul fase kasir medis.
			trx.setKeterangan(("Kasir Apotik Flutter; tunai lunas. "
					+ request.optString("keterangan", "")).trim());
			trx.setOleh(tbmuser.getUserId());
			trx.setOlehId(tbmuser.getUserId());

			// IR-07: metode pembayaran (opsional demi kompatibilitas klien lama).
			// Bila dikirim, WAJIB metode yang benar-benar ada & aktif -- klien
			// tidak boleh menyodorkan metode di luar konfigurasi server.
			Long caraBayarId = optLong(request, "cara_bayar_id");
			ais.database.model.koperasi.CaraPembayaranKoperasi caraBayar = null;
			if (caraBayarId != null) {
				caraBayar = (ais.database.model.koperasi.CaraPembayaranKoperasi) session
						.get(ais.database.model.koperasi.CaraPembayaranKoperasi.class, caraBayarId);
				if (caraBayar == null || !Boolean.TRUE.equals(caraBayar.getAktif())) {
					tolak(hasil, "Metode pembayaran tidak dikenal atau sudah nonaktif.");
					return;
				}
			}

			// IR-11: pembayaran TERPISAH (split). Bila klien mengirim larik
			// "pembayaran", ia menang atas cara_bayar_id tunggal di atas.
			// Klien baru mengirim KEDUANYA supaya server lama tetap membukukan
			// metode pertama alih-alih kehilangan jejak metode sama sekali.
			JSONArray pembayaranArr = request != null && !request.isNull("pembayaran")
					? request.optJSONArray("pembayaran") : null;
			java.util.List<ais.database.model.koperasi.CaraPembayaranKoperasi> bayarCara =
					new java.util.ArrayList<ais.database.model.koperasi.CaraPembayaranKoperasi>();
			java.util.List<Double> bayarNominal = new java.util.ArrayList<Double>();
			java.util.List<Double> bayarTunai = new java.util.ArrayList<Double>();
			java.util.List<Double> bayarKembalian = new java.util.ArrayList<Double>();
			java.util.List<String> bayarReferensi = new java.util.ArrayList<String>();
			if (pembayaranArr != null && pembayaranArr.length() > 0) {
				for (int i = 0; i < pembayaranArr.length(); i++) {
					JSONObject b = pembayaranArr.getJSONObject(i);
					Long cbId = optLong(b, "cara_bayar_id");
					if (cbId == null) {
						tolak(hasil, "Baris pembayaran ke-" + (i + 1)
								+ " tidak menyebut metode pembayaran.");
						return;
					}
					ais.database.model.koperasi.CaraPembayaranKoperasi cb =
							(ais.database.model.koperasi.CaraPembayaranKoperasi) session.get(
									ais.database.model.koperasi.CaraPembayaranKoperasi.class, cbId);
					if (cb == null || !Boolean.TRUE.equals(cb.getAktif())) {
						tolak(hasil, "Metode pembayaran pada baris ke-" + (i + 1)
								+ " tidak dikenal atau sudah nonaktif.");
						return;
					}
					double nominalBaris = b.optDouble("nominal", 0);
					if (nominalBaris <= 0) {
						tolak(hasil, "Nominal pembayaran pada baris ke-" + (i + 1)
								+ " harus lebih dari 0.");
						return;
					}
					bayarCara.add(cb);
					bayarNominal.add(Double.valueOf(nominalBaris));
					bayarTunai.add(b.isNull("tunai") ? null
							: Double.valueOf(b.optDouble("tunai", 0)));
					bayarKembalian.add(b.isNull("kembalian") ? null
							: Double.valueOf(b.optDouble("kembalian", 0)));
					bayarReferensi.add(b.optString("referensi", "").trim());
				}
			}

			session.save(trx);

			double total = 0;
			for (int i = 0; i < itemList.size(); i++) {
				ItemMedis item = itemList.get(i);
				double qty = qtyList.get(i).doubleValue();
				double harga = hargaList.get(i).doubleValue();
				double diskon = diskonList.get(i).doubleValue();
				double subtotal = qty * harga - diskon;
				total += subtotal;

				TransaksiMedisDetail detail = new TransaksiMedisDetail();
				detail.setTransaksi(trx);
				detail.setItem(item);
				detail.setQty(Double.valueOf(qty));
				detail.setAmount(Double.valueOf(harga));
				detail.setDiskon(Double.valueOf(diskon));
				detail.setHasilPenghitunganTotal(Double.valueOf(subtotal));
				detail.setTanggal(new Date());
				detail.setOleh(tbmuser.getUserId());
				detail.setOlehId(tbmuser.getUserId());
				session.save(detail);

				// Baris ledger stok -- pola PERSIS CommonPendaftaranUtil (kodeTransaksi apotikJual).
				ais.database.model.sirs.DetailTransaksiPasien ledger =
						new ais.database.model.sirs.DetailTransaksiPasien();
				ledger.setKodeTransaksi(apotikJual);
				ledger.setItem(item);
				ledger.setQty(Double.valueOf(qty));
				ledger.setAmount(Double.valueOf(harga));
				ledger.setDiskon(Double.valueOf(diskon));
				ledger.setHasilPenghitunganTotal(Double.valueOf(subtotal));
				ledger.setTransaksiDetail(detail);
				ledger.setTanggal(new Date());
				ledger.setLunas(Boolean.TRUE);
				if (lokasi != null) {
					ledger.setLokasi((ais.database.model.asset.Lokasi) lokasi);
				}
				ledger.setOleh(tbmuser.getUserId());
				ledger.setOlehId(tbmuser.getUserId());
				session.save(ledger);

				List<Kadaluarsa> batches = batchList.get(i);
				List<Double> bqty = batchQtyList.get(i);
				for (int b = 0; b < batches.size(); b++) {
					ApotikBatchKonsumsi konsumsi = new ApotikBatchKonsumsi();
					konsumsi.setKadaluarsa(batches.get(b));
					konsumsi.setTransaksiDetail(detail);
					konsumsi.setQty(bqty.get(b));
					konsumsi.setWaktu(new Date());
					konsumsi.setOleh(tbmuser.getUserId());
					konsumsi.setOlehId(tbmuser.getUserId());
					session.save(konsumsi);
				}

				ApotikItemProfile profil = profilList.get(i);
				String golongan = profil == null ? ApotikItemProfile.GOLONGAN_BEBAS : profil.getGolonganObat();
				if (ApotikItemProfile.terkendali(golongan)) {
					ApotikNarkotikaLog log = new ApotikNarkotikaLog();
					log.setItem(item);
					log.setTransaksiDetail(detail);
					log.setResep(resep);
					log.setQty(Double.valueOf(qty));
					log.setGolonganObat(golongan);
					log.setNamaPembeli(namaPembeli);
					log.setAlamatPembeli(alamatPembeli);
					log.setNamaDokter(namaDokter);
					log.setKeterangan(items.getJSONObject(i).optString("keterangan_terkendali", "").trim());
					log.setWaktu(new Date());
					log.setOleh(tbmuser.getUserId());
					log.setOlehId(tbmuser.getUserId());
					session.save(log);
				}
			}
			// IR-07/IR-11: catat pembayaran DI DALAM transaksi yang sama supaya
			// tidak pernah ada transaksi tanpa jejak metode saat metode dikirim.
			JSONArray metodeDipakai = new JSONArray();
			StringBuilder namaGabung = new StringBuilder();
			if (!bayarCara.isEmpty()) {
				// Jumlah seluruh baris WAJIB sama dengan total. Tanpa pagar ini
				// pembayaran terpisah bisa membukukan penjualan yang uangnya
				// tidak pernah lengkap -- dan selisihnya baru ketahuan saat
				// tutup kas, ketika transaksinya sudah tidak dapat ditelusuri.
				double jumlahBayar = 0;
				for (int i = 0; i < bayarNominal.size(); i++) {
					jumlahBayar += bayarNominal.get(i).doubleValue();
				}
				if (Math.abs(jumlahBayar - total) > 0.5) {
					tolak(hasil, "Jumlah pembayaran (Rp " + Math.round(jumlahBayar)
							+ ") tidak sama dengan total transaksi (Rp " + Math.round(total)
							+ "). Selisih Rp " + Math.round(Math.abs(jumlahBayar - total)) + ".");
					return;
				}
				for (int i = 0; i < bayarCara.size(); i++) {
					ais.database.model.koperasi.CaraPembayaranKoperasi cb = bayarCara.get(i);
					ApotikPembayaranTransaksi bayarRow = new ApotikPembayaranTransaksi();
					bayarRow.setTransaksi(trx);
					bayarRow.setCaraBayar(cb);
					bayarRow.setNamaCaraBayar(str(cb.getNama()));
					bayarRow.setNominal(bayarNominal.get(i));
					bayarRow.setTunai(bayarTunai.get(i));
					bayarRow.setKembalian(bayarKembalian.get(i));
					bayarRow.setReferensi(bayarReferensi.get(i));
					bayarRow.setWaktu(new Date());
					bayarRow.setOleh(tbmuser.getUserId());
					bayarRow.setOlehId(tbmuser.getUserId());
					session.save(bayarRow);

					JSONObject m = new JSONObject();
					m.put("nama", str(cb.getNama()));
					m.put("nominal", bayarNominal.get(i).doubleValue());
					metodeDipakai.put(m);
					if (namaGabung.length() > 0) namaGabung.append(" + ");
					namaGabung.append(str(cb.getNama()));
				}
			} else if (caraBayar != null) {
				ApotikPembayaranTransaksi bayarRow = new ApotikPembayaranTransaksi();
				bayarRow.setTransaksi(trx);
				bayarRow.setCaraBayar(caraBayar);
				bayarRow.setNamaCaraBayar(str(caraBayar.getNama()));
				bayarRow.setNominal(Double.valueOf(total));
				if (request != null && !request.isNull("tunai")) {
					bayarRow.setTunai(Double.valueOf(request.optDouble("tunai", 0)));
				}
				if (request != null && !request.isNull("kembalian")) {
					bayarRow.setKembalian(Double.valueOf(request.optDouble("kembalian", 0)));
				}
				bayarRow.setReferensi(request == null ? null
						: request.optString("referensi_bayar", "").trim());
				bayarRow.setWaktu(new Date());
				bayarRow.setOleh(tbmuser.getUserId());
				bayarRow.setOlehId(tbmuser.getUserId());
				session.save(bayarRow);

				JSONObject m = new JSONObject();
				m.put("nama", str(caraBayar.getNama()));
				m.put("nominal", total);
				metodeDipakai.put(m);
				namaGabung.append(str(caraBayar.getNama()));
			}
			tx.commit();
			hasil.put("status", "00");
			hasil.put("id", trx.getId());
			hasil.put("kode", str(trx.getKode()));
			hasil.put("total", total);
			hasil.put("caraBayar", namaGabung.toString());
			hasil.put("pembayaran", metodeDipakai);
		} catch (Exception e) {
			try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) { }
			ais.common.ErrorAuditUtil.record(e, "ApotikApiHelper.bayar");
			// Surface penyebab NYATA ke klien (bukan "Terjadi kesalahan sistem" generik) --
			// pesan spesifik jauh lebih berguna utk kasir & diagnosa. Sertakan sebab-akar bila ada.
			Throwable akar = e;
			while (akar.getCause() != null && akar.getCause() != akar) {
				akar = akar.getCause();
			}
			tolak(hasil, "Gagal menyimpan penjualan: " + e.getClass().getSimpleName()
					+ (akar != e ? " -> " + akar.getClass().getSimpleName() : "")
					+ ": " + (akar.getMessage() == null ? "(tanpa pesan)" : akar.getMessage()));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}
}
