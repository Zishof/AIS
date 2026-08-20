package ais.action.servlet.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.koperasi.helper.AkunKantinUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.PostingHistory;
import ais.database.model.akunting.SaldoAwalAkun;

/**
 * Saldo Awal (Neraca Awal) &mdash; angka pembukaan tiap akun pada tanggal mulai pembukuan.
 *
 * <p><b>Celah yang ditutup.</b> Sebelum ini sistem tidak punya proses saldo awal untuk akun
 * (yang ada hanya saldo awal ASET dan saldo awal perpustakaan). Akibatnya Neraca, Buku Besar,
 * dan Neraca Saldo selalu mulai dari nol: kas, persediaan, piutang, utang, dan modal yang sudah
 * ada sebelum sistem dipakai tidak pernah muncul, sehingga laporan tak akan pernah cocok dengan
 * keadaan sebenarnya walau seluruh dokumen sudah diposting.</p>
 *
 * <p><b>Alur:</b> isi/unggah angka per akun &rarr; lihat draf jurnal pembukaan &rarr; posting.
 * Seluruh baris yang belum diposting digabung menjadi SATU jurnal pembukaan. Bila total debet dan
 * kredit tidak sama &mdash; hal yang wajar saat pembukuan pertama karena modal belum dihitung
 * &mdash; selisihnya ditempatkan pada akun <b>Modal/Ekuitas Awal</b> (konfigurasi
 * {@code akun_modal_awal}) sehingga jurnalnya tetap seimbang dan selisihnya terlihat jelas,
 * bukan disembunyikan.</p>
 *
 * <p>Baris yang sudah diposting ditandai {@code posting_history} dan tidak bisa diubah maupun
 * dihapus dari sini; koreksi setelah posting dilakukan lewat jurnal penyesuaian. Semua penulisan
 * lewat Hibernate agar terekam Envers.</p>
 */
public final class SaldoAwalAkunHelper {

	public static final String JENIS = "Saldo Awal (Neraca Awal)";
	public static final String CFG_MODAL_AWAL = "akun_modal_awal";

	private SaldoAwalAkunHelper() {
	}

	public static void proses(String action, Tbmuser tbmuser, JSONObject payload, JSONObject hasil)
			throws Exception {
		if ("saldo_awal_daftar".equals(action)) {
			daftar(payload, hasil);
		} else if ("saldo_awal_simpan".equals(action)) {
			simpan(tbmuser, payload, hasil);
		} else if ("saldo_awal_hapus".equals(action)) {
			hapus(payload, hasil);
		} else if ("saldo_awal_impor".equals(action)) {
			impor(tbmuser, payload, hasil);
		} else if ("saldo_awal_draft".equals(action)) {
			draftAtauPosting(tbmuser, payload, hasil, false);
		} else if ("saldo_awal_posting".equals(action)) {
			draftAtauPosting(tbmuser, payload, hasil, true);
		} else {
			hasil.put("status", "99");
			hasil.put("message", "Aksi saldo awal tidak dikenal: " + action);
		}
	}

	// ================================================================= daftar

	private static void daftar(JSONObject payload, JSONObject hasil) throws Exception {
		String cari = payload == null ? "" : payload.optString("cari", "").trim();
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			Connection conn = session.connection();
			StringBuilder sql = new StringBuilder(
					"SELECT s.id, COALESCE(a.kode,''), COALESCE(a.nama,''), COALESCE(s.debet,0),"
							+ " COALESCE(s.kredit,0), s.tanggal, COALESCE(s.keterangan,''), s.posting_history"
							+ " FROM akunting.saldo_awal_akun s LEFT JOIN akunting.akun a ON a.id = s.akun");
			if (!cari.isEmpty()) {
				sql.append(" WHERE (a.kode ILIKE ? OR a.nama ILIKE ?)");
			}
			sql.append(" ORDER BY a.kode");
			PreparedStatement ps = conn.prepareStatement(sql.toString());
			if (!cari.isEmpty()) {
				ps.setString(1, "%" + cari + "%");
				ps.setString(2, "%" + cari + "%");
			}
			ResultSet rs = ps.executeQuery();
			JSONArray arr = new JSONArray();
			double totalD = 0, totalK = 0, belumD = 0, belumK = 0;
			while (rs.next()) {
				JSONObject j = new JSONObject();
				j.put("id", rs.getLong(1));
				j.put("kodeAkun", rs.getString(2));
				j.put("namaAkun", rs.getString(3));
				double d = rs.getDouble(4);
				double k = rs.getDouble(5);
				j.put("debet", d);
				j.put("kredit", k);
				java.sql.Timestamp t = rs.getTimestamp(6);
				j.put("tanggal", t == null ? "" : Common.dateFormat3.get().format(t));
				j.put("keterangan", rs.getString(7));
				long ph = rs.getLong(8);
				boolean sudah = !rs.wasNull() && ph > 0;
				j.put("sudahDiposting", sudah);
				arr.put(j);
				totalD += d;
				totalK += k;
				if (!sudah) {
					belumD += d;
					belumK += k;
				}
			}
			rs.close();
			ps.close();
			hasil.put("status", "00");
			hasil.put("data", arr);
			hasil.put("totalDebet", totalD);
			hasil.put("totalKredit", totalK);
			hasil.put("selisih", totalD - totalK);
			hasil.put("belumDipostingDebet", belumD);
			hasil.put("belumDipostingKredit", belumK);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	// ================================================================= simpan / hapus

	private static Akun akunByKode(Session session, String kode) {
		if (kode == null || kode.trim().isEmpty()) {
			return null;
		}
		return (Akun) session.createCriteria(Akun.class)
				.add(org.hibernate.criterion.Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
	}

	private static Date tanggalDari(String teks) {
		try {
			if (teks == null || teks.trim().isEmpty()) {
				return ais.ui.util.WaktuUtil.getDate();
			}
			return new java.text.SimpleDateFormat("yyyy-MM-dd").parse(teks.trim());
		} catch (Exception e) {
			return ais.ui.util.WaktuUtil.getDate();
		}
	}

	private static void simpan(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			String kode = payload.optString("kodeAkun", "").trim();
			Akun akun = akunByKode(session, kode);
			if (akun == null) {
				hasil.put("status", "99");
				hasil.put("message", "Akun dengan kode '" + kode + "' tidak ditemukan.");
				return;
			}
			SaldoAwalAkun baris = barisAkun(session, akun);
			if (baris != null && baris.getPostingHistory() != null) {
				hasil.put("status", "99");
				hasil.put("message", "Saldo awal akun " + kode + " sudah diposting; koreksi lewat jurnal penyesuaian.");
				return;
			}
			if (baris == null) {
				baris = new SaldoAwalAkun();
				baris.setAkun(akun);
			}
			baris.setDebet(Double.valueOf(payload.optDouble("debet", 0)));
			baris.setKredit(Double.valueOf(payload.optDouble("kredit", 0)));
			baris.setTanggal(tanggalDari(payload.optString("tanggal", "")));
			baris.setKeterangan(payload.optString("keterangan", ""));
			baris.setSatuanKerja(AkunKantinUtil.satkerKantin());
			isiOleh(baris, tbmuser);

			session.beginTransaction();
			session.saveOrUpdate(baris);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("id", baris.getId());
			hasil.put("message", "Saldo awal akun " + kode + " tersimpan.");
		} catch (Exception e) {
			batalkanDiam(session);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void hapus(JSONObject payload, JSONObject hasil) throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			long id = payload.optLong("id", 0);
			SaldoAwalAkun baris = (SaldoAwalAkun) session.get(SaldoAwalAkun.class, Long.valueOf(id));
			if (baris == null) {
				hasil.put("status", "99");
				hasil.put("message", "Baris saldo awal tidak ditemukan.");
				return;
			}
			if (baris.getPostingHistory() != null) {
				hasil.put("status", "99");
				hasil.put("message", "Baris ini sudah diposting dan tidak dapat dihapus.");
				return;
			}
			session.beginTransaction();
			session.delete(baris);
			session.getTransaction().commit();
			hasil.put("status", "00");
			hasil.put("message", "Baris saldo awal dihapus.");
		} catch (Exception e) {
			batalkanDiam(session);
			throw e;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Satu akun hanya boleh punya SATU baris saldo awal. */
	private static SaldoAwalAkun barisAkun(Session session, Akun akun) {
		return (SaldoAwalAkun) session.createCriteria(SaldoAwalAkun.class)
				.add(org.hibernate.criterion.Restrictions.eq("akun", akun)).setMaxResults(1).uniqueResult();
	}

	// ================================================================= impor Excel

	private static void impor(Tbmuser tbmuser, JSONObject payload, JSONObject hasil) throws Exception {
		JSONArray baris = payload.optJSONArray("baris");
		if (baris == null || baris.length() == 0) {
			hasil.put("status", "99");
			hasil.put("message", "Tidak ada baris untuk diproses.");
			return;
		}
		Session session = HibernateUtil.getSessionFactory().openSession();
		int dibuat = 0, diperbarui = 0, ditolak = 0;
		JSONArray masalah = new JSONArray();
		try {
			String tanggal = payload.optString("tanggal", "");
			for (int i = 0; i < baris.length(); i++) {
				int nomorBaris = i + 2;   // baris 1 = judul kolom
				JSONObject b = baris.optJSONObject(i);
				if (b == null) {
					continue;
				}
				String kode = b.optString("kodeAkun", "").trim();
				if (kode.isEmpty()) {
					ditolak++;
					masalah.put("Baris " + nomorBaris + ": kode akun wajib diisi.");
					continue;
				}
				try {
					Akun akun = akunByKode(session, kode);
					if (akun == null) {
						ditolak++;
						masalah.put("Baris " + nomorBaris + ": akun '" + kode + "' tidak ada di bagan akun.");
						continue;
					}
					SaldoAwalAkun row = barisAkun(session, akun);
					if (row != null && row.getPostingHistory() != null) {
						ditolak++;
						masalah.put("Baris " + nomorBaris + ": saldo awal akun " + kode
								+ " sudah diposting, dilewati.");
						continue;
					}
					boolean baru = row == null;
					if (baru) {
						row = new SaldoAwalAkun();
						row.setAkun(akun);
					}
					row.setDebet(Double.valueOf(angka(b.optString("debet", "0"))));
					row.setKredit(Double.valueOf(angka(b.optString("kredit", "0"))));
					row.setTanggal(tanggalDari(tanggal));
					row.setKeterangan(b.optString("keterangan", ""));
					row.setSatuanKerja(AkunKantinUtil.satkerKantin());
					isiOleh(row, tbmuser);
					session.beginTransaction();
					session.saveOrUpdate(row);
					session.getTransaction().commit();
					if (baru) {
						dibuat++;
					} else {
						diperbarui++;
					}
				} catch (Exception ex) {
					batalkanDiam(session);
					ditolak++;
					ais.common.ErrorAuditUtil.record(ex, "auto-audit SaldoAwalAkunHelper.impor");
					masalah.put("Baris " + nomorBaris + ": " + ex.getMessage());
				}
			}
			hasil.put("status", "00");
			hasil.put("dibuat", dibuat);
			hasil.put("diperbarui", diperbarui);
			hasil.put("ditolak", ditolak);
			hasil.put("masalah", masalah);
			hasil.put("message", dibuat + " dibuat, " + diperbarui + " diperbarui, " + ditolak + " ditolak.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/** Angka dari teks Excel: titik/koma ribuan dibuang, koma desimal diterima. */
	private static double angka(String teks) {
		if (teks == null) {
			return 0;
		}
		String t = teks.trim().replace(" ", "");
		if (t.isEmpty()) {
			return 0;
		}
		t = t.replace(".", "");
		t = t.replace(",", ".");
		try {
			return Double.parseDouble(t);
		} catch (Exception e) {
			return 0;
		}
	}

	// ================================================================= draf & posting

	private static void draftAtauPosting(Tbmuser tbmuser, JSONObject payload, JSONObject hasil, boolean terapkan)
			throws Exception {
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {
			@SuppressWarnings("unchecked")
			List<SaldoAwalAkun> semua = session.createCriteria(SaldoAwalAkun.class)
					.add(org.hibernate.criterion.Restrictions.isNull("postingHistory")).list();

			List<Akun> akunDebet = new ArrayList<Akun>();
			List<Double> nilaiDebet = new ArrayList<Double>();
			List<Akun> akunKredit = new ArrayList<Akun>();
			List<Double> nilaiKredit = new ArrayList<Double>();
			List<Long> idBaris = new ArrayList<Long>();
			JSONArray rincian = new JSONArray();
			double totalD = 0, totalK = 0;
			Date tanggal = null;

			for (int i = 0; i < semua.size(); i++) {
				SaldoAwalAkun b = semua.get(i);
				Akun a = b.getAkun();
				if (a == null) {
					continue;
				}
				double d = b.getDebet() == null ? 0 : b.getDebet().doubleValue();
				double k = b.getKredit() == null ? 0 : b.getKredit().doubleValue();
				if (Math.abs(d) < 0.005 && Math.abs(k) < 0.005) {
					continue;
				}
				if (tanggal == null) {
					tanggal = b.getTanggal();
				}
				if (d > 0) {
					akunDebet.add(a);
					nilaiDebet.add(Double.valueOf(d));
					totalD += d;
				}
				if (k > 0) {
					akunKredit.add(a);
					nilaiKredit.add(Double.valueOf(k));
					totalK += k;
				}
				idBaris.add(b.getId());
				JSONObject j = new JSONObject();
				j.put("id", b.getId());
				j.put("kodeAkun", a.getKode());
				j.put("namaAkun", a.getNama());
				j.put("debet", d);
				j.put("kredit", k);
				rincian.put(j);
			}

			double selisih = totalD - totalK;
			Akun akunModal = AkunKantinUtil.akunKonfigurasi(CFG_MODAL_AWAL);
			String alasan = "";
			if (rincian.length() == 0) {
				alasan = "Belum ada saldo awal yang perlu diposting.";
			} else if (Math.abs(selisih) >= 0.005 && akunModal == null) {
				alasan = "Total debet dan kredit belum sama (selisih "
						+ Common.numberFormat.get().format(Math.abs(selisih))
						+ ") dan akun Modal/Ekuitas Awal belum diatur (konfigurasi " + CFG_MODAL_AWAL
						+ "). Isi konfigurasi itu atau seimbangkan angkanya lebih dulu.";
			}
			if (Math.abs(selisih) >= 0.005 && akunModal != null) {
				// Selisih ditempatkan pada Modal Awal: debet lebih besar berarti modal bertambah
				// (dikredit), sebaliknya modal berkurang (didebet).
				if (selisih > 0) {
					akunKredit.add(akunModal);
					nilaiKredit.add(Double.valueOf(selisih));
					totalK += selisih;
				} else {
					akunDebet.add(akunModal);
					nilaiDebet.add(Double.valueOf(-selisih));
					totalD += -selisih;
				}
			}

			hasil.put("status", "00");
			hasil.put("rincian", rincian);
			hasil.put("totalDebet", totalD);
			hasil.put("totalKredit", totalK);
			hasil.put("selisihKeModal", selisih);
			hasil.put("akunModal", akunModal == null ? "" : AkunKantinUtil.label(akunModal));
			hasil.put("siap", alasan.isEmpty());
			hasil.put("alasan", alasan);

			if (!terapkan) {
				hasil.put("message", alasan.isEmpty()
						? (rincian.length() + " akun siap diposting sebagai jurnal pembukaan.")
						: alasan);
				return;
			}
			if (!alasan.isEmpty()) {
				hasil.put("status", "99");
				hasil.put("message", alasan);
				return;
			}
			Tbmuser pengguna = tbmuser == null ? Common.getCurrentUser() : tbmuser;
			if (pengguna == null) {
				hasil.put("status", "01");
				hasil.put("message", "Sesi pengguna tidak ditemukan. Silakan masuk kembali sebelum memposting.");
				return;
			}

			String ket = "Saldo awal (neraca awal) " + rincian.length() + " akun";
			PostingHistory ph = new PostingHistory(JENIS);
			ph.setTanggal(tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal);
			ph.setTbmuser(pengguna);
			ph.setKeterangan(ket);

			session.beginTransaction();
			try {
				session.save(ph);
				boolean ok = CommonAkunting.saveTransaksi(akunDebet.toArray(new Akun[] {}),
						akunKredit.toArray(new Akun[] {}), null, null, ph, true, ket,
						tanggal == null ? ais.ui.util.WaktuUtil.getDate() : tanggal,
						nilaiDebet.toArray(new Double[] {}), nilaiKredit.toArray(new Double[] {}),
						Double.valueOf(0.0), null, AkunKantinUtil.satkerKantin(), session);
				if (!ok) {
					session.getTransaction().rollback();
					hasil.put("status", "99");
					hasil.put("message", "Jurnal pembukaan ditolak (periode mungkin sudah ditutup).");
					return;
				}
				StringBuilder ids = new StringBuilder();
				for (int i = 0; i < idBaris.size(); i++) {
					if (ids.length() > 0) {
						ids.append(",");
					}
					ids.append(idBaris.get(i).longValue());
				}
				if (ids.length() > 0) {
					session.createSQLQuery("UPDATE akunting.saldo_awal_akun SET posting_history = "
							+ ph.getId().longValue() + " WHERE posting_history IS NULL AND id IN (" + ids + ")")
							.executeUpdate();
				}
				session.getTransaction().commit();
			} catch (Exception e) {
				batalkanDiam(session);
				throw e;
			}
			hasil.put("diposting", idBaris.size());
			hasil.put("message", "Jurnal pembukaan terbentuk untuk " + idBaris.size() + " akun.");
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private static void isiOleh(SaldoAwalAkun baris, Tbmuser tbmuser) {
		try {
			if (tbmuser != null) {
				baris.setOleh(tbmuser.getUserId());
				baris.setOlehId(tbmuser.getUserId());
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SaldoAwalAkunHelper.isiOleh");
		}
	}

	private static void batalkanDiam(Session session) {
		try {
			if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
				session.getTransaction().rollback();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) SaldoAwalAkunHelper.batalkanDiam");
		}
	}
}
