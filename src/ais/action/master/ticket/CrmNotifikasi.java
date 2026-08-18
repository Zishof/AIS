package ais.action.master.ticket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import ais.common.Common;
import ais.common.CommonNotifikasi;
import ais.database.model.Tbmuser;
import ais.database.model.crm.CrmActivity;
import ais.database.model.crm.CrmLead;

/**
 * <h3>Notifikasi in-app untuk modul CRM (Pipeline)</h3>
 *
 * <p>Memakai infrastruktur notifikasi yang sudah ada ({@link CommonNotifikasi}), mengikuti pola
 * {@link TicketNotifikasi}. Peristiwa yang diberitahukan (fase 1 — tanpa job terjadwal baru):</p>
 * <ul>
 *   <li><b>Lead/Peluang ditugaskan</b> → petugas yang ditugaskan ({@link CrmLead#getDitugaskanUser()}).</li>
 *   <li><b>Aktivitas baru dijadwalkan</b> → PIC aktivitas ({@link CrmActivity#getPicUser()}).</li>
 * </ul>
 *
 * <p>Seluruhnya try/catch — kegagalan notifikasi tidak boleh menggagalkan operasi utama. Java 1.7.</p>
 */
public final class CrmNotifikasi {

	private CrmNotifikasi() {
	}

	/** Beri tahu petugas bahwa sebuah lead/peluang ditugaskan kepadanya. */
	public static void leadDitugaskan(CrmLead lead) {
		try {
			if (lead == null || lead.getDitugaskanUser() == null) {
				return;
			}
			List<Tbmuser> penerima = new ArrayList<Tbmuser>();
			penerima.add(lead.getDitugaskanUser());
			buangDiriSendiri(penerima);
			if (penerima.isEmpty()) {
				return;
			}
			LinkedHashMap<String, String> r = rincian(lead);
			CommonNotifikasi.terbitkanKeBanyak(penerima,
					(CrmLead.TIPE_PELUANG.equals(lead.getTipe()) ? "Peluang" : "Lead") + " Baru: " + ringkas(safe(lead.getJudul()), 80),
					"Sebuah " + (CrmLead.TIPE_PELUANG.equals(lead.getTipe()) ? "peluang" : "lead") + " ditugaskan kepada Anda.",
					r, null, lead, null, null, CommonNotifikasi.STATUS_INFO);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmNotifikasi.leadDitugaskan");
		}
	}

	/** Beri tahu PIC bahwa sebuah aktivitas baru dijadwalkan untuknya. */
	public static void aktivitasDitugaskan(CrmActivity aktivitas) {
		try {
			if (aktivitas == null || aktivitas.getPicUser() == null || aktivitas.getLead() == null) {
				return;
			}
			List<Tbmuser> penerima = new ArrayList<Tbmuser>();
			penerima.add(aktivitas.getPicUser());
			buangDiriSendiri(penerima);
			if (penerima.isEmpty()) {
				return;
			}
			LinkedHashMap<String, String> r = rincian(aktivitas.getLead());
			r.put("Jenis Aktivitas", safe(aktivitas.getJenis()));
			if (aktivitas.getTargetDate() != null) {
				r.put("Target Tanggal", Common.dateFormat5.get().format(aktivitas.getTargetDate()));
			}
			CommonNotifikasi.terbitkanKeBanyak(penerima, "Aktivitas CRM Baru: " + ringkas(safe(aktivitas.getLead().getJudul()), 80),
					"Ada aktivitas tindak lanjut baru yang dijadwalkan untuk Anda.", r, null, aktivitas.getLead(), null,
					null, CommonNotifikasi.STATUS_INFO);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmNotifikasi.aktivitasDitugaskan");
		}
	}

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
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) CrmNotifikasi.buangDiriSendiri");
		}
	}

	private static LinkedHashMap<String, String> rincian(CrmLead lead) {
		LinkedHashMap<String, String> r = new LinkedHashMap<String, String>();
		r.put("Judul", safe(lead.getJudul()));
		if (lead.getKontakInstansi() != null) {
			r.put("Instansi", lead.getKontakInstansi());
		}
		if (lead.getPipelineType() != null) {
			r.put("Pipeline", safe(lead.getPipelineType().getNama()));
		}
		return r;
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
