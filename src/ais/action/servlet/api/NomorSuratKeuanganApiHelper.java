package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.EbisnisMenuKatalog;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.WaktuUtil;

/**
 * <h3>API JSON "Penomoran Dokumen Keuangan" untuk POS Desktop/Android.</h3>
 *
 * <p>Memindahkan {@code ais.action.master.akunting.NomorSuratAlurKeuanganAction} — tempat
 * setiap jenis dokumen Keuangan dipasangkan dengan templat nomornya.</p>
 *
 * <p><b>Kenapa ini mendesak.</b> Setiap modul Keuangan membuat kodenya lewat pola yang sama:</p>
 *
 * <pre>
 * if (NomorSuratAlurKeuangan.X_DATA == null
 *         || NomorSuratAlurKeuangan.X_DATA.getNomorSurat() == null) {
 *     return Common.getGeneratedBarCode();          // &lt;-- jatuh ke BARCODE
 * }
 * </pre>
 *
 * <p>Artinya alur yang belum dipasangi templat menghasilkan kode seperti
 * {@code 1041B55F9FAF} alih-alih nomor dokumen yang dapat dibaca — dan itu bukan
 * kemungkinan teoretis: pada basis data uji, dari sepuluh alur yang terdaftar
 * <b>hanya "Uang Muka" yang punya templat</b>. Sembilan sisanya, termasuk DPC yang
 * dipakai Proses Transfer, semuanya berkode barcode. Sampai sekarang pemasangannya
 * hanya bisa dilakukan di layar ZK.</p>
 *
 * <p><b>Pratinjau tidak menghabiskan nomor.</b> {@code NomorSurat.format()} murni —
 * yang menaikkan urutan adalah {@code NomorSurat.tambahIndexNomorSurat()}, dan itu
 * TIDAK dipanggil di sini. Jadi melihat contoh hasil aman diulang berkali-kali.</p>
 *
 * <p><b>Catatan pemanggilan.</b> Dipakai bentuk {@code format(urutan, tanggal, satuanKerja)}
 * yang bersatuan kerja eksplisit, bukan bentuk dua argumennya: bentuk dua argumen memanggil
 * {@code Common.getSatuanKerja()} di luar blok try, sehingga gagal di luar konteks servlet.</p>
 */
public final class NomorSuratKeuanganApiHelper {

	private static final String KUNCI = "nomor_surat_keuangan";

	private NomorSuratKeuanganApiHelper() {
	}

	private static void tolak(JSONObject hasil, String pesan) throws Exception {
		hasil.put("status", "91");
		hasil.put("description", pesan);
	}

	private static boolean bolehAksi(Tbmuser tbmuser, String aksi) {
		if (Common.getApakahAdminLain(tbmuser)) {
			return true;
		}
		Tbmrole role = tbmuser == null ? null : tbmuser.hakAkses();
		if (role == null) {
			return true;
		}
		return EbisnisMenuKatalog.bolehAksi(EbisnisMenuKatalog.urai(role.getEbisnisMenu()), KUNCI, aksi);
	}

	private static JSONObject hakAksesJson(Tbmuser tbmuser) throws Exception {
		JSONObject j = new JSONObject();
		j.put("create", bolehAksi(tbmuser, "create"));
		j.put("update", bolehAksi(tbmuser, "update"));
		j.put("delete", bolehAksi(tbmuser, "delete"));
		return j;
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			// rollback gagal: kegagalan aslinya yang dilaporkan ke pemanggil
		}
	}

	/** Jenis segmen yang dikenal mesin penomoran; urutannya sama dengan layar ZK. */
	private static final String[] JENIS_SEGMEN = { NomorSurat.KOSONG, NomorSurat.NOMOR_URUT,
			NomorSurat.KATA_STATIS, NomorSurat.TANGGAL, NomorSurat.BULAN, NomorSurat.BULAN_ROMAWI,
			NomorSurat.TAHUN };

	private static boolean jenisSah(String jenis) {
		for (int i = 0; i < JENIS_SEGMEN.length; i++) {
			if (JENIS_SEGMEN[i].equals(jenis)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Contoh hasil penomoran TANPA menaikkan urutannya. Bila templatnya belum ada,
	 * dikembalikan string kosong — pemanggil yang memutuskan bagaimana menampilkannya.
	 */
	private static String pratinjau(NomorSurat ns) {
		if (ns == null) {
			return "";
		}
		try {
			Long urut = Boolean.TRUE.equals(ns.getGunakanIndexUrut()) ? ns.getNomorIndex() : Long.valueOf(1);
			if (urut == null) {
				urut = Long.valueOf(1);
			}
			// Bentuk tiga argumen: bentuk dua argumen memanggil Common.getSatuanKerja()
			// di luar try sehingga meledak di luar konteks servlet.
			return ns.format(urut, WaktuUtil.getDate(), null);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit NomorSuratKeuanganApiHelper.pratinjau");
			return "";
		}
	}

	// ============================================================ daftar alur

	public static void daftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT a.id, COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(a.keterangan,''),"
							+ " a.nomor_surat, COALESCE(n.nama,''), COALESCE(n.contohformat,'')"
							+ " FROM akunting.nomor_surat_alur_keuangan a"
							+ " LEFT JOIN surat.nomor_surat n ON n.id = a.nomor_surat"
							+ " ORDER BY a.kode");
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			int belumDipasang = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kode", rs.getString(2));
				j.put("nama", rs.getString(3));
				j.put("keterangan", rs.getString(4));
				long nsId = rs.getLong(5);
				boolean ada = !rs.wasNull() && nsId != 0;
				j.put("nomorSuratId", ada ? (Object) Long.valueOf(nsId) : JSONObject.NULL);
				j.put("nomorSuratNama", rs.getString(6));
				j.put("contohFormat", rs.getString(7));
				// Bendera inilah inti layar ini: alur tanpa templat menghasilkan barcode,
				// bukan nomor dokumen -- dan itu tidak terlihat di mana pun sampai
				// dokumennya terlanjur terbit.
				j.put("pakaiBarcode", !ada);
				if (!ada) {
					belumDipasang++;
					j.put("akibat", "Dokumen jenis ini terbit dengan kode barcode "
							+ "(mis. 1041B55F9FAF), bukan nomor dokumen yang dapat dibaca.");
				}
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("belumDipasang", belumDipasang);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ opsi & templat

	public static void opsi(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		JSONArray jenis = new JSONArray();
		for (int i = 0; i < JENIS_SEGMEN.length; i++) {
			JSONObject j = new JSONObject();
			j.put("nilai", JENIS_SEGMEN[i]);
			j.put("label", JENIS_SEGMEN[i]);
			// "Tanda" adalah teks yang menempel SESUDAH segmen; pada Kata Statis, tanda
			// itulah isinya. Dijelaskan di sini supaya layarnya tidak perlu menebak.
			j.put("tandaAdalahIsi", NomorSurat.KATA_STATIS.equals(JENIS_SEGMEN[i]));
			jenis.put(j);
		}
		hasil.put("status", "00");
		hasil.put("jenisSegmen", jenis);
		hasil.put("jumlahSegmen", 10);
		hasil.put("hak", hakAksesJson(tbmuser));
		hasil.put("catatanAlur",
				"Setiap jenis dokumen Keuangan mengambil nomornya dari templat yang dipasang "
						+ "di sini. Alur tanpa templat akan terbit berkode barcode.");
	}

	public static void templatDaftar(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		String cari = request == null ? "" : request.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT n.id, COALESCE(n.nama,''), COALESCE(n.keterangan,''),"
							+ " COALESCE(n.contohformat,''), COALESCE(n.aktif,true),"
							+ " COALESCE(n.gunakanindexurut,false), COALESCE(n.nomorindex,1),"
							+ " COALESCE(n.jumlahangkanoldidepannomorurut,3),"
							+ " COALESCE(n.reseturutantiaptahun,false), COALESCE(n.reseturutantiapbulan,false),"
							+ " COALESCE(n.urutberdasarkannomor,false), COALESCE(n.urutberdasarkankelompok,false),"
							+ " COALESCE(n.mulaiurutanke,0),"
							+ " (SELECT count(*) FROM akunting.nomor_surat_alur_keuangan a"
							+ "  WHERE a.nomor_surat = n.id)"
							+ " FROM surat.nomor_surat n WHERE 1 = 1");
			if (!cari.isEmpty()) {
				sql.append(" AND (COALESCE(n.nama,'') ILIKE ? OR COALESCE(n.contohformat,'') ILIKE ?)");
			}
			sql.append(" ORDER BY n.nama LIMIT 300");
			PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				String kw = "%" + cari + "%";
				ps.setString(1, kw);
				ps.setString(2, kw);
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("nama", rs.getString(2));
				j.put("keterangan", rs.getString(3));
				j.put("contohFormat", rs.getString(4));
				j.put("aktif", rs.getBoolean(5));
				j.put("gunakanIndexUrut", rs.getBoolean(6));
				j.put("nomorIndex", rs.getLong(7));
				j.put("jumlahNolDepan", rs.getInt(8));
				j.put("resetTiapTahun", rs.getBoolean(9));
				j.put("resetTiapBulan", rs.getBoolean(10));
				j.put("urutBerdasarkanNomor", rs.getBoolean(11));
				j.put("urutBerdasarkanKelompok", rs.getBoolean(12));
				j.put("mulaiUrutanKe", rs.getLong(13));
				j.put("dipakaiAlur", rs.getLong(14));
				arr.put(j);
			}
			rs.close();
			ps.close();

			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("hak", hakAksesJson(tbmuser));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Segmen satu templat, sepuluh pasang jenis+tanda. */
	public static void templatDetail(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		if (id == 0) {
			tolak(hasil, "Templat belum dipilih.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			NomorSurat ns = (NomorSurat) session.get(NomorSurat.class, Long.valueOf(id));
			if (ns == null) {
				tolak(hasil, "Templat tidak ditemukan.");
				return;
			}
			hasil.put("status", "00");
			hasil.put("segmen", segmenJson(ns));
			hasil.put("contoh", pratinjau(ns));
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static JSONArray segmenJson(NomorSurat ns) throws Exception {
		String[] kolom = { ns.getKolom1(), ns.getKolom2(), ns.getKolom3(), ns.getKolom4(), ns.getKolom5(),
				ns.getKolom6(), ns.getKolom7(), ns.getKolom8(), ns.getKolom9(), ns.getKolom10() };
		String[] tanda = { ns.getTanda1(), ns.getTanda2(), ns.getTanda3(), ns.getTanda4(), ns.getTanda5(),
				ns.getTanda6(), ns.getTanda7(), ns.getTanda8(), ns.getTanda9(), ns.getTanda10() };
		JSONArray arr = new JSONArray();
		for (int i = 0; i < kolom.length; i++) {
			JSONObject j = new JSONObject();
			j.put("jenis", kolom[i] == null ? NomorSurat.KOSONG : kolom[i]);
			j.put("tanda", tanda[i] == null ? "" : tanda[i]);
			arr.put(j);
		}
		return arr;
	}

	private static void terapkanSegmen(NomorSurat ns, JSONArray segmen) {
		for (int i = 0; i < 10; i++) {
			JSONObject s = segmen == null ? null : segmen.optJSONObject(i);
			String jenis = s == null ? NomorSurat.KOSONG : s.optString("jenis", NomorSurat.KOSONG);
			if (!jenisSah(jenis)) {
				jenis = NomorSurat.KOSONG;
			}
			String tanda = s == null ? "" : s.optString("tanda", "");
			switch (i) {
			case 0:
				ns.setKolom1(jenis);
				ns.setTanda1(tanda);
				break;
			case 1:
				ns.setKolom2(jenis);
				ns.setTanda2(tanda);
				break;
			case 2:
				ns.setKolom3(jenis);
				ns.setTanda3(tanda);
				break;
			case 3:
				ns.setKolom4(jenis);
				ns.setTanda4(tanda);
				break;
			case 4:
				ns.setKolom5(jenis);
				ns.setTanda5(tanda);
				break;
			case 5:
				ns.setKolom6(jenis);
				ns.setTanda6(tanda);
				break;
			case 6:
				ns.setKolom7(jenis);
				ns.setTanda7(tanda);
				break;
			case 7:
				ns.setKolom8(jenis);
				ns.setTanda8(tanda);
				break;
			case 8:
				ns.setKolom9(jenis);
				ns.setTanda9(tanda);
				break;
			default:
				ns.setKolom10(jenis);
				ns.setTanda10(tanda);
				break;
			}
		}
	}

	public static void templatSimpan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		long id = request == null ? 0 : request.optLong("id", 0);
		boolean baru = id == 0;
		if (!bolehAksi(tbmuser, baru ? "create" : "update")) {
			tolak(hasil, baru ? "Anda tidak memiliki hak membuat templat penomoran."
					: "Anda tidak memiliki hak mengubah templat penomoran.");
			return;
		}
		String nama = request.optString("nama", "").trim();
		if (nama.isEmpty()) {
			tolak(hasil, "Nama templat wajib diisi.");
			return;
		}
		JSONArray segmen = request.optJSONArray("segmen");
		// Tanpa segmen Nomor Urut, tiap dokumen menerima teks yang SAMA -- dan kode
		// dokumen wajib unik, sehingga penyimpanannya akan gagal berulang kali di
		// KodeUnikUtil tanpa penjelasan yang bisa dimengerti pengguna.
		boolean adaNomorUrut = false;
		for (int i = 0; segmen != null && i < segmen.length(); i++) {
			JSONObject s = segmen.optJSONObject(i);
			if (s != null && NomorSurat.NOMOR_URUT.equals(s.optString("jenis"))) {
				adaNomorUrut = true;
			}
		}
		if (!adaNomorUrut) {
			tolak(hasil, "Templat wajib memuat satu segmen \"Nomor Urut\"; tanpa itu semua "
					+ "dokumen akan menerima nomor yang sama persis.");
			return;
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			NomorSurat ns = baru ? new NomorSurat() : (NomorSurat) session.get(NomorSurat.class, Long.valueOf(id));
			if (ns == null) {
				tolak(hasil, "Templat tidak ditemukan.");
				return;
			}
			session.beginTransaction();
			ns.setNama(nama);
			ns.setKeterangan(request.optString("keterangan", "").trim());
			ns.setAktif(Boolean.valueOf(request.optBoolean("aktif", true)));
			ns.setJumlahAngkaNolDiDepanNomorUrut(
					Integer.valueOf(Math.max(1, Math.min(12, request.optInt("jumlahNolDepan", 3)))));
			ns.setMulaiUrutanKe(Long.valueOf(request.optLong("mulaiUrutanKe", 0)));
			ns.setGunakanIndexUrut(Boolean.valueOf(request.optBoolean("gunakanIndexUrut", false)));
			ns.setNomorIndex(Long.valueOf(Math.max(1, request.optLong("nomorIndex", 1))));
			ns.setResetUrutanTiapTahun(Boolean.valueOf(request.optBoolean("resetTiapTahun", false)));
			ns.setResetUrutanTiapBulan(Boolean.valueOf(request.optBoolean("resetTiapBulan", false)));
			ns.setUrutBerdasarkanNomor(Boolean.valueOf(request.optBoolean("urutBerdasarkanNomor", false)));
			ns.setUrutBerdasarkanKelompok(Boolean.valueOf(request.optBoolean("urutBerdasarkanKelompok", false)));
			terapkanSegmen(ns, segmen);
			session.saveOrUpdate(ns);
			session.flush();
			String contoh = pratinjau(ns);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("id", ns.getId());
			hasil.put("contoh", contoh);
			hasil.put("message", (baru ? "Templat penomoran dibuat" : "Templat penomoran diperbarui")
					+ (contoh.isEmpty() ? "." : "; contoh hasilnya: " + contoh));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Templat belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public static void templatHapus(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "delete")) {
			tolak(hasil, "Anda tidak memiliki hak menghapus templat penomoran.");
			return;
		}
		long id = request == null ? 0 : request.optLong("id", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			NomorSurat ns = id == 0 ? null : (NomorSurat) session.get(NomorSurat.class, Long.valueOf(id));
			if (ns == null) {
				tolak(hasil, "Templat tidak ditemukan.");
				return;
			}
			// Templat yang masih terpasang tidak boleh dihapus: alurnya akan langsung
			// jatuh ke barcode tanpa siapa pun menyadarinya.
			PreparedStatement ps = session.connection().prepareStatement(
					"SELECT count(*) FROM akunting.nomor_surat_alur_keuangan WHERE nomor_surat = ?");
			ps.setLong(1, id);
			ResultSet rs = ps.executeQuery();
			rs.next();
			long dipakai = rs.getLong(1);
			rs.close();
			ps.close();
			if (dipakai > 0) {
				tolak(hasil, "Templat ini masih dipasang pada " + dipakai + " alur dokumen. "
						+ "Lepaskan dulu, atau ganti dengan templat lain — bila dihapus begitu saja, "
						+ "dokumennya akan terbit berkode barcode.");
				return;
			}
			session.beginTransaction();
			String nama = ns.getNama();
			session.delete(ns);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("message", "Templat " + nama + " dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Templat belum dapat dihapus: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ pasang templat

	public static void pasang(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		if (!bolehAksi(tbmuser, "update")) {
			tolak(hasil, "Anda tidak memiliki hak mengatur penomoran dokumen Keuangan.");
			return;
		}
		long alurId = request == null ? 0 : request.optLong("alurId", 0);
		long nsId = request == null ? 0 : request.optLong("nomorSuratId", 0);
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			NomorSuratAlurKeuangan alur = alurId == 0 ? null
					: (NomorSuratAlurKeuangan) session.get(NomorSuratAlurKeuangan.class, Long.valueOf(alurId));
			if (alur == null) {
				tolak(hasil, "Alur dokumen tidak ditemukan.");
				return;
			}
			NomorSurat ns = null;
			if (nsId != 0) {
				ns = (NomorSurat) session.get(NomorSurat.class, Long.valueOf(nsId));
				if (ns == null) {
					tolak(hasil, "Templat penomoran tidak ditemukan.");
					return;
				}
			}
			session.beginTransaction();
			alur.setNomorSurat(ns);
			session.update(alur);
			session.flush();
			String contoh = pratinjau(ns);
			session.getTransaction().commit();

			hasil.put("status", "00");
			hasil.put("contoh", contoh);
			hasil.put("message", ns == null
					? ("Templat dilepas dari " + alur.getNama()
							+ "; mulai sekarang dokumennya terbit berkode barcode.")
					: ("Penomoran " + alur.getNama() + " memakai templat " + ns.getNama()
							+ (contoh.isEmpty() ? "." : "; contoh hasilnya: " + contoh)));
		} catch (Exception e) {
			batalkanDiam(session);
			tolak(hasil, "Pemasangan templat belum dapat disimpan: " + e.getMessage());
			hasil.put("teknis", e.toString());
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ============================================================ pratinjau lepas

	/**
	 * Contoh hasil dari rancangan yang BELUM disimpan. Objeknya tidak pernah dilekatkan ke
	 * sesi Hibernate, jadi tidak ada yang tersimpan dan tidak ada nomor yang terpakai.
	 */
	public static void pratinjauRancangan(Tbmuser tbmuser, JSONObject request, JSONObject hasil) throws Exception {
		NomorSurat ns = new NomorSurat();
		ns.setJumlahAngkaNolDiDepanNomorUrut(
				Integer.valueOf(Math.max(1, Math.min(12, request.optInt("jumlahNolDepan", 3)))));
		ns.setMulaiUrutanKe(Long.valueOf(request.optLong("mulaiUrutanKe", 0)));
		ns.setGunakanIndexUrut(Boolean.valueOf(request.optBoolean("gunakanIndexUrut", false)));
		ns.setNomorIndex(Long.valueOf(Math.max(1, request.optLong("nomorIndex", 1))));
		ns.setUrutBerdasarkanNomor(Boolean.valueOf(request.optBoolean("urutBerdasarkanNomor", false)));
		ns.setUrutBerdasarkanKelompok(Boolean.valueOf(request.optBoolean("urutBerdasarkanKelompok", false)));
		terapkanSegmen(ns, request.optJSONArray("segmen"));
		Date tgl = WaktuUtil.getDate();
		String contoh;
		try {
			contoh = ns.format(Long.valueOf(Math.max(1, request.optLong("nomorIndex", 1))), tgl, null);
		} catch (Exception e) {
			contoh = "";
		}
		hasil.put("status", "00");
		hasil.put("contoh", contoh);
	}

	// ============================================================ dispatcher

	/** Dipakai dispatcher: seluruh aksi berawalan {@code nomor_surat_keuangan_}. */
	public static boolean proses(String action, Tbmuser tbmuser, JSONObject request, JSONObject hasil)
			throws Exception {
		if ("nomor_surat_keuangan_daftar".equals(action)) {
			daftar(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_opsi".equals(action)) {
			opsi(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_templat_daftar".equals(action)) {
			templatDaftar(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_templat_detail".equals(action)) {
			templatDetail(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_templat_simpan".equals(action)) {
			templatSimpan(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_templat_hapus".equals(action)) {
			templatHapus(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_pasang".equals(action)) {
			pasang(tbmuser, request, hasil);
			return true;
		}
		if ("nomor_surat_keuangan_pratinjau".equals(action)) {
			pratinjauRancangan(tbmuser, request, hasil);
			return true;
		}
		return false;
	}
}
