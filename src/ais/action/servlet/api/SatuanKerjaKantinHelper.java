package ais.action.servlet.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Yayasan;

/**
 * Menu "Satuan Kerja" pada halaman Pelanggan: kelola satuan kerja dan pilih
 * member mana saja yang tergolong di dalamnya.
 *
 * <p><b>Logikanya SENGAJA mengikuti layar ZKoss</b>
 * ({@code ais.action.master.rab.SatuanKerjaAction}) supaya kedua kanal
 * menampilkan daftar yang sama:</p>
 * <ul>
 * <li><b>Aktif</b> = {@code defaultItem == true}. Tabel {@code rab.satuan_kerja}
 * TIDAK punya kolom {@code aktif}; ZKoss memakai {@code defaultItem} sebagai
 * penandanya, jadi di sini pun begitu -- bukan menambah kolom baru yang justru
 * menciptakan dua sumber kebenaran.</li>
 * <li><b>Cakupan</b> = per {@link Yayasan}; bila yayasan tidak diketahui, hanya
 * baris ber-yayasan NULL yang tampil (persis {@code Restrictions.isNull} di
 * ZKoss). Penyaring inilah yang membuat daftar tetap kecil walau tabelnya
 * berisi puluhan ribu satker nasional milik modul RAB.</li>
 * </ul>
 *
 * <p>Tambahannya di sini: bila akun punya {@code pendaftar} (tenant), daftar
 * disaring lagi ke satuan kerja milik pendaftar itu -- tanpa penyaring ini satu
 * tenant dapat melihat satuan kerja tenant lain.</p>
 *
 * <p><b>Penugasan member</b> ditulis ke DUA tempat sekaligus:
 * {@code AnggotaKoperasi.satuanKerja} (agar member tanpa akun login tetap dapat
 * dikelompokkan) dan {@code Tbmuser.satuanKerja} bila member punya akun (agar
 * sejalan dgn modul kepegawaian). Keduanya diisi bersamaan supaya tidak muncul
 * dua jawaban berbeda untuk pertanyaan yang sama.</p>
 */
public final class SatuanKerjaKantinHelper {

	private SatuanKerjaKantinHelper() {
	}

	private static boolean isi(String v) {
		return v != null && v.trim().length() > 0;
	}

	private static String teks(String v) {
		return v == null ? "" : v.trim();
	}

	/** Kriteria daftar -- padanan {@code SatuanKerjaAction.criteria()} di ZKoss. */
	private static Criteria kriteria(Session session, Yayasan yayasan, Tbmuser tbmuser,
			boolean hanyaAktif, String cari) {
		Criteria c = session.createCriteria(SatuanKerja.class)
				.add(yayasan == null || yayasan.getId() == null ? Restrictions.isNull("yayasan")
						: Restrictions.eq("yayasan", yayasan));
		if (hanyaAktif) {
			c.add(Restrictions.eq("defaultItem", Boolean.TRUE));
		}
		if (tbmuser != null && tbmuser.getPendaftar() != null
				&& tbmuser.getPendaftar().getId() != null) {
			c.add(Restrictions.eq("pendaftar", tbmuser.getPendaftar()));
		}
		if (isi(cari)) {
			c.add(Restrictions.or(
					Restrictions.ilike("nama", cari.trim(), MatchMode.ANYWHERE),
					Restrictions.ilike("kode", cari.trim(), MatchMode.ANYWHERE)));
		}
		return c.addOrder(Order.asc("nama"));
	}

	/** Daftar satuan kerja + jumlah member pada masing-masing. */
	@SuppressWarnings("unchecked")
	public static void satuanKerjaList(Tbmuser tbmuser, HttpServletRequest request, JSONObject req,
			JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Yayasan yayasan = ais.action.master.sekolah.util.SekolahUtil.getYayasan(request);
			boolean hanyaAktif = !req.optBoolean("tampilkan_nonaktif", false);
			List<SatuanKerja> daftar = kriteria(session, yayasan, tbmuser, hanyaAktif,
					req.optString("cari", "")).list();

			JSONArray arr = new JSONArray();
			for (SatuanKerja sk : daftar) {
				JSONObject o = new JSONObject();
				o.put("id", sk.getId());
				o.put("kode", teks(sk.getKode()));
				o.put("nama", teks(sk.getNama()));
				o.put("keterangan", teks(sk.getKeterangan()));
				o.put("alamat", teks(sk.getAlamat()));
				o.put("aktif", Boolean.TRUE.equals(sk.getDefaultItem()));
				o.put("jumlahMember", jumlahMember(session, sk.getId()));
				arr.put(o);
			}
			hasil.put("status", "00");
			hasil.put("data", arr);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Jumlah member pada satu satuan kerja. Dihitung lewat KOLOM, bukan getter
	 * entity: getter {@code AnggotaKoperasi.getSatuanKerja()} MENURUNKAN nilai dari
	 * pegawai/dosen bila kolomnya kosong, sehingga memakai getter akan ikut
	 * menghitung member yang tidak pernah ditugaskan lewat menu ini.
	 */
	private static long jumlahMember(Session session, Long idSatuanKerja) throws Exception {
		if (idSatuanKerja == null) {
			return 0;
		}
		PreparedStatement ps = session.connection()
				.prepareStatement("SELECT COUNT(*) FROM koperasi.anggota_koperasi WHERE satuan_kerja = ?");
		try {
			ps.setLong(1, idSatuanKerja.longValue());
			ResultSet rs = ps.executeQuery();
			long n = rs.next() ? rs.getLong(1) : 0;
			rs.close();
			return n;
		} finally {
			try {
				ps.close();
			} catch (Exception abaikan) {
				ais.common.ErrorAuditUtil.record(abaikan,
						"SatuanKerjaKantinHelper.jumlahMember: gagal menutup statement");
			}
		}
	}

	/** Tambah/ubah satuan kerja. */
	public static void satuanKerjaSimpan(Tbmuser tbmuser, HttpServletRequest request, JSONObject req,
			JSONObject hasil) throws Exception {
		String nama = teks(req.optString("nama", ""));
		if (nama.length() == 0) {
			hasil.put("status", "91");
			hasil.put("description", "Nama satuan kerja wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			SatuanKerja sk;
			if (!req.isNull("id") && isi(req.optString("id", ""))) {
				sk = (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(req.getString("id")));
				if (sk == null) {
					hasil.put("status", "91");
					hasil.put("description", "Satuan kerja tidak ditemukan.");
					return;
				}
			} else {
				sk = new SatuanKerja();
				// Cakupan diikat saat DIBUAT supaya baris ini kelak lolos kriteria daftar
				// yang sama (yayasan + pendaftar). Tanpa ini, data yang baru dibuat
				// langsung hilang dari layar begitu daftar dimuat ulang.
				sk.setYayasan(ais.action.master.sekolah.util.SekolahUtil.getYayasan(request));
				if (tbmuser != null && tbmuser.getPendaftar() != null) {
					sk.setPendaftar(tbmuser.getPendaftar());
				}
			}
			sk.setNama(nama);
			sk.setKode(teks(req.optString("kode", "")));
			sk.setKeterangan(teks(req.optString("keterangan", "")));
			sk.setAlamat(teks(req.optString("alamat", "")));
			sk.setDefaultItem(Boolean.valueOf(req.optBoolean("aktif", true)));
			session.saveOrUpdate(sk);
			session.flush();
			tx.commit();
			tx = null;
			hasil.put("status", "00");
			hasil.put("id", sk.getId());
			hasil.put("description", "Satuan kerja tersimpan.");
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan, "SatuanKerjaKantinHelper.simpan: rollback gagal");
				}
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Nonaktifkan satuan kerja ({@code defaultItem = false}) -- BUKAN menghapus
	 * baris. Barisnya dapat dirujuk member, pegawai, dan dokumen RAB; menghapusnya
	 * akan memutus rujukan itu. Menonaktifkan juga persis cara ZKoss
	 * menyembunyikan satuan kerja lewat penyaring "aktif".
	 */
	public static void satuanKerjaHapus(JSONObject req, JSONObject hasil) throws Exception {
		if (req.isNull("id") || !isi(req.optString("id", ""))) {
			hasil.put("status", "91");
			hasil.put("description", "Id satuan kerja wajib diisi.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			SatuanKerja sk = (SatuanKerja) session.get(SatuanKerja.class, Long.valueOf(req.getString("id")));
			if (sk == null) {
				hasil.put("status", "91");
				hasil.put("description", "Satuan kerja tidak ditemukan.");
				return;
			}
			sk.setDefaultItem(Boolean.FALSE);
			session.update(sk);
			session.flush();
			tx.commit();
			tx = null;
			hasil.put("status", "00");
			hasil.put("description", "Satuan kerja dinonaktifkan. Member yang sudah ditugaskan tidak diubah.");
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan, "SatuanKerjaKantinHelper.hapus: rollback gagal");
				}
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Daftar member berikut penanda apakah sudah masuk satuan kerja tertentu.
	 * Dibaca lewat KOLOM {@code satuan_kerja} -- lihat catatan pada
	 * {@link #jumlahMember} soal getter yang menurunkan nilai sendiri.
	 */
	public static void satuanKerjaAnggotaList(JSONObject req, JSONObject hasil) throws Exception {
		Long idSk = req.isNull("satuan_kerja_id") || !isi(req.optString("satuan_kerja_id", "")) ? null
				: Long.valueOf(req.getString("satuan_kerja_id"));
		String cari = teks(req.optString("cari", ""));
		boolean hanyaAnggota = req.optBoolean("hanya_anggota", false);

		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			StringBuilder sql = new StringBuilder(
					"SELECT a.id, COALESCE(a.kode,'') kode, COALESCE(a.nama,'') nama,"
							+ " a.satuan_kerja, a.tbmuser, COALESCE(sk.nama,'') nama_sk"
							+ " FROM koperasi.anggota_koperasi a"
							+ " LEFT JOIN rab.satuan_kerja sk ON sk.id = a.satuan_kerja"
							+ " WHERE COALESCE(a.aktif, true) = true ");
			List<Object> prm = new ArrayList<Object>();
			if (hanyaAnggota && idSk != null) {
				sql.append(" AND a.satuan_kerja = ? ");
				prm.add(idSk);
			}
			if (isi(cari)) {
				sql.append(" AND (COALESCE(a.nama,'') ILIKE ? OR COALESCE(a.kode,'') ILIKE ?) ");
				prm.add("%" + cari + "%");
				prm.add("%" + cari + "%");
			}
			sql.append(" ORDER BY a.nama LIMIT 500");

			PreparedStatement ps = session.connection().prepareStatement(sql.toString());
			try {
				for (int i = 0; i < prm.size(); i++) {
					Object v = prm.get(i);
					if (v instanceof Long) {
						ps.setLong(i + 1, ((Long) v).longValue());
					} else {
						ps.setString(i + 1, String.valueOf(v));
					}
				}
				ResultSet rs = ps.executeQuery();
				JSONArray arr = new JSONArray();
				while (rs.next()) {
					JSONObject o = new JSONObject();
					o.put("id", rs.getLong(1));
					o.put("kode", teks(rs.getString(2)));
					o.put("nama", teks(rs.getString(3)));
					long skBaris = rs.getLong(4);
					boolean adaSk = !rs.wasNull();
					o.put("satuanKerjaId", adaSk ? (Object) Long.valueOf(skBaris) : JSONObject.NULL);
					o.put("satuanKerjaNama", teks(rs.getString(6)));
					rs.getLong(5);
					o.put("punyaAkun", !rs.wasNull());
					o.put("terpilih", adaSk && idSk != null && skBaris == idSk.longValue());
					arr.put(o);
				}
				rs.close();
				hasil.put("status", "00");
				hasil.put("data", arr);
			} finally {
				try {
					ps.close();
				} catch (Exception abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan,
							"SatuanKerjaKantinHelper.anggotaList: gagal menutup statement");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tetapkan anggota satuan kerja. Id yang dikirim menjadi anggota; anggota lama
	 * yang TIDAK ada dalam daftar dilepas.
	 */
	public static void satuanKerjaAnggotaSimpan(JSONObject req, JSONObject hasil) throws Exception {
		if (req.isNull("satuan_kerja_id") || !isi(req.optString("satuan_kerja_id", ""))) {
			hasil.put("status", "91");
			hasil.put("description", "Satuan kerja wajib dipilih.");
			return;
		}
		Long idSk = Long.valueOf(req.getString("satuan_kerja_id"));
		JSONArray arr = req.optJSONArray("anggota_id");
		List<Long> dipilih = new ArrayList<Long>();
		if (arr != null) {
			for (int i = 0; i < arr.length(); i++) {
				String v = String.valueOf(arr.get(i)).trim();
				if (ais.common.Common.isNumber(v)) {
					dipilih.add(Long.valueOf(v));
				}
			}
		}

		Session session = HibernateUtil.getSessionFactory().openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			SatuanKerja sk = (SatuanKerja) session.get(SatuanKerja.class, idSk);
			if (sk == null) {
				hasil.put("status", "91");
				hasil.put("description", "Satuan kerja tidak ditemukan.");
				return;
			}
			int dilepas = 0;
			@SuppressWarnings("unchecked")
			List<AnggotaKoperasi> lama = session.createCriteria(AnggotaKoperasi.class)
					.add(Restrictions.eq("satuanKerja", sk)).list();
			for (AnggotaKoperasi a : lama) {
				if (!dipilih.contains(a.getId())) {
					a.setSatuanKerja(null);
					sinkronkanTbmuser(session, a, null);
					session.update(a);
					dilepas++;
				}
			}
			int ditambah = 0;
			for (Long id : dipilih) {
				AnggotaKoperasi a = (AnggotaKoperasi) session.get(AnggotaKoperasi.class, id);
				if (a == null) {
					continue;
				}
				a.setSatuanKerja(sk);
				sinkronkanTbmuser(session, a, sk);
				session.update(a);
				ditambah++;
			}
			session.flush();
			tx.commit();
			tx = null;
			hasil.put("status", "00");
			hasil.put("ditambah", ditambah);
			hasil.put("dilepas", dilepas);
			hasil.put("description", ditambah + " member berada di satuan kerja ini"
					+ (dilepas > 0 ? ", " + dilepas + " dilepas." : "."));
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) {
					ais.common.ErrorAuditUtil.record(abaikan,
							"SatuanKerjaKantinHelper.anggotaSimpan: rollback gagal");
				}
			}
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Samakan {@code Tbmuser.satuanKerja} dgn penugasan member, HANYA bila member
	 * punya akun. Member tanpa akun cukup memakai kolom di {@code AnggotaKoperasi}
	 * -- itulah alasan kedua tempat diisi bersamaan.
	 */
	private static void sinkronkanTbmuser(Session session, AnggotaKoperasi a, SatuanKerja sk) {
		try {
			Tbmuser u = a.getTbmuser();
			if (u == null) {
				return;
			}
			u.setSatuanKerja(sk);
			session.update(u);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"SatuanKerjaKantinHelper.sinkronkanTbmuser: gagal menyamakan satuan kerja akun");
		}
	}
}
