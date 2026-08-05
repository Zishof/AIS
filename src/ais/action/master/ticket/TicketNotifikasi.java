package ais.action.master.ticket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonNotifikasi;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.ticket.Ticket;
import ais.database.model.ticket.TicketKomentar;

/**
 * <h3>Notifikasi in-app untuk modul Ticketing</h3>
 *
 * <p>Memakai infrastruktur notifikasi yang sudah ada ({@link CommonNotifikasi} → lonceng "Info"
 * di header). Peristiwa yang diberitahukan:</p>
 * <ul>
 *   <li><b>Tiket baru</b> → developer (role di {@code ticketing_role_developer}) + petugas yang
 *       ditugaskan.</li>
 *   <li><b>Komentar/pesan baru</b> → pihak lawan (pengaju ⇄ pengelola).</li>
 *   <li><b>Perubahan status</b> → pengaju tiket.</li>
 * </ul>
 *
 * <p>{@code dataObject} selalu diisi entitas {@link Ticket} agar notifikasi lolos filter lonceng
 * (butuh {@code classData}). Seluruhnya try/catch — kegagalan notifikasi tidak boleh menggagalkan
 * operasi utama. Java 1.7.</p>
 */
public final class TicketNotifikasi {

	private TicketNotifikasi() {
	}

	/** Beri tahu developer + petugas bahwa ada tiket baru. */
	public static void tiketBaru(Ticket t) {
		try {
			if (t == null) {
				return;
			}
			List<Tbmuser> penerima = developer();
			tambahPetugas(penerima, t);
			buangDiriSendiri(penerima);
			if (penerima.isEmpty()) {
				return;
			}
			LinkedHashMap<String, String> r = rincian(t);
			CommonNotifikasi.terbitkanKeBanyak(penerima, "Tiket Baru: " + ringkasJudul(t),
					"Sebuah tiket baru telah diajukan dan menunggu penanganan.", r, null, t, null, null,
					CommonNotifikasi.STATUS_INFO);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.tiketBaru");
		}
	}

	/**
	 * Beri tahu pihak lawan atas komentar/pesan baru.
	 *
	 * @param dariPengelola true bila pengirim komentar adalah pengelola/petugas (maka penerima =
	 *                      pengaju); false bila pengirim = pengaju/pihak lain (penerima = developer +
	 *                      petugas).
	 */
	public static void komentarBaru(Ticket t, TicketKomentar k, boolean dariPengelola) {
		try {
			if (t == null) {
				return;
			}
			// Catatan internal tidak diberitahukan ke pengaju.
			if (k != null && Boolean.TRUE.equals(k.getInternal()) && !dariPengelola) {
				return;
			}
			List<Tbmuser> penerima = new ArrayList<Tbmuser>();
			if (dariPengelola) {
				Tbmuser pengaju = pengaju(t);
				if (pengaju != null) {
					penerima.add(pengaju);
				}
			} else {
				penerima = developer();
				tambahPetugas(penerima, t);
			}
			buangDiriSendiri(penerima);
			if (penerima.isEmpty()) {
				return;
			}
			LinkedHashMap<String, String> r = rincian(t);
			if (k != null && k.getIsi() != null) {
				r.put("Pesan", ringkas(k.getIsi(), 160));
			}
			CommonNotifikasi.terbitkanKeBanyak(penerima, "Pesan Baru pada Tiket " + safe(t.getNomorTiket()),
					"Ada pesan baru pada tiket yang berkaitan dengan Anda.", r, null, t, null, null,
					CommonNotifikasi.STATUS_INFO);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.komentarBaru");
		}
	}

	/** Beri tahu pengaju bahwa status tiketnya berubah. */
	public static void statusBerubah(Ticket t, String statusLama) {
		try {
			if (t == null) {
				return;
			}
			Tbmuser pengaju = pengaju(t);
			if (pengaju == null) {
				return;
			}
			List<Tbmuser> penerima = new ArrayList<Tbmuser>();
			penerima.add(pengaju);
			buangDiriSendiri(penerima);
			if (penerima.isEmpty()) {
				return;
			}
			String status = safe(t.getStatus());
			String stt = Ticket.STATUS_SELESAI.equals(status) ? CommonNotifikasi.STATUS_INFO
					: (Ticket.STATUS_DITOLAK.equals(status) ? CommonNotifikasi.STATUS_DANGER
							: CommonNotifikasi.STATUS_WARNING);
			LinkedHashMap<String, String> r = rincian(t);
			if (statusLama != null) {
				r.put("Status Sebelumnya", statusLama);
			}
			CommonNotifikasi.terbitkanKeBanyak(penerima, "Status Tiket " + safe(t.getNomorTiket()) + ": " + status,
					"Status tiket Anda diperbarui menjadi \"" + status + "\".", r, null, t, null, null, stt);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.statusBerubah");
		}
	}

	// ==================================================================================
	// Resolusi penerima
	// ==================================================================================

	/** Daftar Tbmuser developer (role di konfigurasi {@code ticketing_role_developer}). */
	@SuppressWarnings("unchecked")
	private static List<Tbmuser> developer() {
		List<Tbmuser> hasil = new ArrayList<Tbmuser>();
		try {
			String csv = Common.getKonfigurasi(TicketKonfigurasiAction.KEY_ROLE_DEV, "").getNilai();
			if (csv == null || csv.trim().isEmpty()) {
				return hasil;
			}
			List<String> roleIds = new ArrayList<String>();
			for (String s : csv.split(",")) {
				if (s != null && s.trim().length() > 0) {
					roleIds.add(s.trim());
				}
			}
			if (roleIds.isEmpty()) {
				return hasil;
			}
			Session session = HibernateUtil.currentSession();
			hasil = session.createCriteria(Tbmuser.class).createAlias("userRole", "r")
					.add(Restrictions.in("r.roleId", roleIds)).setMaxResults(200).list();
			if (hasil == null) {
				hasil = new ArrayList<Tbmuser>();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.developer");
		}
		return hasil;
	}

	/** Tbmuser pengaju tiket (berdasarkan userId yang tersimpan). */
	private static Tbmuser pengaju(Ticket t) {
		try {
			if (t.getPengajuUserId() == null) {
				return null;
			}
			Session session = HibernateUtil.currentSession();
			return (Tbmuser) session.createCriteria(Tbmuser.class)
					.add(Restrictions.eq("userId", t.getPengajuUserId())).setMaxResults(1).uniqueResult();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.pengaju");
			return null;
		}
	}

	private static void tambahPetugas(List<Tbmuser> penerima, Ticket t) {
		try {
			if (t.getDitugaskanKeUserId() == null) {
				return;
			}
			Session session = HibernateUtil.currentSession();
			Tbmuser petugas = (Tbmuser) session.createCriteria(Tbmuser.class)
					.add(Restrictions.eq("userId", t.getDitugaskanKeUserId())).setMaxResults(1).uniqueResult();
			if (petugas != null && !mengandung(penerima, petugas)) {
				penerima.add(petugas);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.tambahPetugas");
		}
	}

	/** Buang pengguna yang sedang login agar tak menerima notifikasi atas aksinya sendiri. */
	private static void buangDiriSendiri(List<Tbmuser> penerima) {
		try {
			Tbmuser aku = Common.getCurrentUser();
			if (aku == null || aku.getUserId() == null || penerima == null) {
				return;
			}
			for (int i = penerima.size() - 1; i >= 0; i--) {
				Tbmuser u = penerima.get(i);
				if (u != null && aku.getUserId().equals(u.getUserId())) {
					penerima.remove(i);
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) TicketNotifikasi.buangDiriSendiri");
		}
	}

	private static boolean mengandung(List<Tbmuser> list, Tbmuser u) {
		if (u == null || u.getUserId() == null) {
			return false;
		}
		for (Tbmuser x : list) {
			if (x != null && u.getUserId().equals(x.getUserId())) {
				return true;
			}
		}
		return false;
	}

	// ==================================================================================
	// Util isi
	// ==================================================================================

	private static LinkedHashMap<String, String> rincian(Ticket t) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Nomor Tiket", safe(t.getNomorTiket()));
		r.put("Judul", safe(t.getJudul()));
		r.put("Tipe", safe(t.getTipe()));
		r.put("Prioritas", safe(t.getPrioritas()));
		r.put("Status", safe(t.getStatus()));
		if (t.getPengajuNama() != null) {
			r.put("Pengaju", t.getPengajuNama());
		}
		return r;
	}

	private static String ringkasJudul(Ticket t) {
		return ringkas(safe(t.getJudul()), 80);
	}

	private static String ringkas(String s, int maks) {
		if (s == null) {
			return "";
		}
		s = s.trim();
		return s.length() <= maks ? s : s.substring(0, maks) + "…";
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}
}
