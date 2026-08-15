package ais.action.master.helper;

import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.Fakultas;
import ais.database.model.ItemBiaya;
import ais.database.model.ItemBiayaPunyaAkun;
import ais.database.model.ItemBiayaPunyaPiutang;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.ui.util.MyWindow;

/**
 * <h3>AnalisisPemetaanAkunHelper — komponen "Analisis Cerdas" reusable untuk baris posting
 * yang transaksinya TIDAK VALID (akun debet/kredit belum terpetakan).</h3>
 *
 * <p>Dipakai seragam oleh semua layar {@code *Posting*} yang berpola sama: alih-alih menampilkan
 * pesan polos, panggil {@link #tampilkanInvalid(Component, String)} agar pesan tampil MENCOLOK
 * (kotak merah + ikon) plus tombol <b>Analisis Cerdas</b>. Bila konteks {@link ItemBiaya} +
 * {@link Kegiatan} tersedia (posting mahasiswa berbasis pemetaan akun per Jurusan/Program/
 * Angkatan), gunakan {@link #tampilkanInvalid(Component, String, ItemBiaya, Kegiatan)} —
 * tombolnya akan MENG-QUERY tabel pemetaan ({@code ItemBiayaPunyaAkun}/{@code ItemBiayaPunyaPiutang}),
 * mendeteksi pemetaan yang hilang, mencarikan pemetaan terdekat, lalu menyajikan langkah perbaikan.</p>
 */
public class AnalisisPemetaanAkunHelper {

	/** Versi universal (tanpa deep-query) — cukup pesan mencolok + tombol langkah perbaikan. */
	public static void tampilkanInvalid(Component parent, String pesan) {
		tampilkanInvalid(parent, pesan, null, null);
	}

	/**
	 * Render kotak "Transaksi Tidak Valid" yang mencolok + tombol Analisis Cerdas.
	 *
	 * @param itemBiaya bila non-null (posting berbasis pemetaan akun), tombol melakukan query
	 *                  pemetaan untuk diagnosis mendalam.
	 */
	public static void tampilkanInvalid(final Component parent, final String pesan, final ItemBiaya itemBiaya,
			final Kegiatan kegiatan) {
		Div box = new Div();
		box.setStyle("background:#fef2f2; border:1px solid #fca5a5; border-left:5px solid #dc2626; padding:6px 9px;"
				+ " border-radius:6px; color:#7f1d1d; margin:2px 0;");
		box.setParent(parent);

		Div judul = new Div();
		judul.setStyle("font-weight:bold; color:#b91c1c; margin-bottom:3px;");
		new Label("⚠ Transaksi Tidak Valid").setParent(judul);
		judul.setParent(box);

		Label lp = new Label(pesan == null ? "" : pesan);
		lp.setMultiline(true);
		lp.setStyle("font-size:11px; color:#7f1d1d;");
		lp.setParent(box);

		Button btn = new Button("🔍 Analisis Cerdas");
		btn.setStyle("margin-top:6px; background:#dc2626; color:#fff; border:none; padding:4px 12px;"
				+ " border-radius:6px; cursor:pointer; font-weight:bold;");
		btn.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				try {
					bukaAnalisis(parent, pesan, itemBiaya, kegiatan);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}
		});
		btn.setParent(box);
	}

	private static void bukaAnalisis(Component ref, String pesan, ItemBiaya itemBiaya, Kegiatan kegiatan)
			throws Exception {
		MyWindow w = new MyWindow();
		ref.getPage().getFirstRoot().appendChild(w);
		w.setTitle("Analisis Cerdas — Transaksi Tidak Valid");
		w.setWidth("780px");
		w.setBorder("normal");
		w.setClosable(true);
		w.setSizable(true);
		w.setMaximizable(true);

		Div wrap = new Div();
		wrap.setStyle("padding:12px; max-height:72vh; overflow:auto;");
		wrap.setParent(w);

		Html html = new Html();
		html.setContent(buildHtml(pesan, itemBiaya, kegiatan));
		html.setParent(wrap);

		w.doModal();
	}

	// ================= penyusun konten HTML =================

	private static String buildHtml(String pesan, ItemBiaya itemBiaya, Kegiatan kegiatan) {
		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-family:Segoe UI,Arial,sans-serif; font-size:13px; color:#1f2937;'>");

		// Konteks
		Fakultas[] fh = new Fakultas[1];
		Jurusan[] jh = new Jurusan[1];
		String[] ph = new String[1];
		String[] ah = new String[1];
		String mhs = resolveKonteks(kegiatan, fh, jh, ph, ah);
		Fakultas fak = fh[0];
		Jurusan jur = jh[0];
		String prog = ph[0];
		String ang = ah[0];

		sb.append("<h3 style='margin:0 0 6px; color:#b91c1c;'>&#9888; Transaksi Tidak Valid</h3>");
		sb.append("<div style='background:#fef2f2; border:1px solid #fca5a5; border-radius:8px; padding:10px; margin-bottom:12px;'>");
		sb.append(kartuKonteks("Item Biaya", itemBiaya == null ? "-" : esc(itemBiaya.getNama())));
		if (mhs != null) {
			sb.append(kartuKonteks("Mahasiswa/Peserta", esc(mhs)));
		}
		sb.append(kartuKonteks("Fakultas", fak == null ? "-" : esc(fak.getNama())));
		sb.append(kartuKonteks("Jurusan / Prodi", jur == null ? "-" : esc(jur.getNama())));
		sb.append(kartuKonteks("Program", isBlank(prog) ? "-" : esc(prog)));
		sb.append(kartuKonteks("Angkatan", isBlank(ang) ? "-" : esc(ang)));
		sb.append("</div>");

		if (itemBiaya == null) {
			// Mode universal (non-pemetaan): tampilkan pesan + langkah sebagaimana adanya.
			sb.append(blokLangkah(pesan));
			sb.append("</div>");
			return sb.toString();
		}

		// Query pemetaan yang benar-benar ada
		List<ItemBiayaPunyaAkun> pendapatan = ambilPemetaanAkun(itemBiaya);
		List<ItemBiayaPunyaPiutang> piutang = ambilPemetaanPiutang(itemBiaya);

		Akun akunPendapatan = null;
		Akun akunPiutang = null;
		try {
			akunPendapatan = itemBiaya.ambilAkun(kegiatan);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnalisisPemetaanAkunHelper.ambilAkun");
		}
		try {
			akunPiutang = itemBiaya.ambilPiutang(kegiatan);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnalisisPemetaanAkunHelper.ambilPiutang");
		}

		// Diagnosis ringkas
		sb.append("<h4 style='margin:6px 0; color:#111827;'>&#128269; Diagnosis</h4><ul style='margin:0 0 12px; padding-left:18px;'>");
		sb.append(statusAkun("Akun Pendapatan", akunPendapatan));
		sb.append(statusAkun("Akun Piutang", akunPiutang));
		sb.append("</ul>");

		// Tabel pemetaan yang sudah ada + tandai yang cocok
		sb.append("<h4 style='margin:6px 0; color:#111827;'>&#128203; Pemetaan Akun Pendapatan yang sudah ada (" + pendapatan.size() + ")</h4>");
		sb.append(tabelPendapatan(pendapatan, fak, jur, prog, ang));
		sb.append("<h4 style='margin:10px 0 6px; color:#111827;'>&#128203; Pemetaan Akun Piutang yang sudah ada (" + piutang.size() + ")</h4>");
		sb.append(tabelPiutang(piutang, fak, jur, prog, ang));

		// Saran cerdas
		sb.append("<h4 style='margin:12px 0 6px; color:#065f46;'>&#128161; Saran Cerdas</h4>");
		sb.append("<div style='background:#ecfdf5; border:1px solid #6ee7b7; border-radius:8px; padding:10px; margin-bottom:12px; line-height:1.5;'>");
		sb.append(saranCerdas("Pendapatan", akunPendapatan, pendapatan.isEmpty(), fak, jur, prog, ang));
		sb.append(saranCerdas("Piutang", akunPiutang, piutang.isEmpty(), fak, jur, prog, ang));
		sb.append("</div>");

		// Langkah perbaikan konkret
		sb.append(blokLangkah(pesan));

		sb.append("</div>");
		return sb.toString();
	}

	private static String saranCerdas(String jenis, Akun akun, boolean kosongTotal, Fakultas fak, Jurusan jur,
			String prog, String ang) {
		if (akun != null) {
			return "<div style='color:#065f46;'>&#10004; Akun " + jenis + " sudah dapat di-resolve untuk kombinasi ini. Tidak perlu tindakan.</div>";
		}
		StringBuilder s = new StringBuilder();
		s.append("<div style='color:#7f1d1d; margin-bottom:6px;'>");
		if (kosongTotal) {
			s.append("&#10148; <b>Belum ada pemetaan Akun " + jenis + " sama sekali</b> untuk item biaya ini. ")
					.append("Buat <b>1 baris default</b>: pilih Fakultas <b>").append(fak == null ? "(sesuai)" : esc(fak.getNama()))
					.append("</b>, <u>kosongkan Jurusan &amp; Program &amp; Angkatan</u> agar berlaku sebagai fallback untuk SEMUA kombinasi, lalu pilih Akun ")
					.append(jenis).append(" yang sesuai.");
		} else {
			s.append("&#10148; Ada pemetaan lain, tetapi <b>tidak ada yang cocok</b> untuk Jurusan <b>")
					.append(jur == null ? "-" : esc(jur.getNama())).append("</b> / Program <b>")
					.append(isBlank(prog) ? "-" : esc(prog)).append("</b> / Angkatan <b>").append(isBlank(ang) ? "-" : esc(ang))
					.append("</b>. Pilihan: (a) <b>tambah baris</b> khusus kombinasi ini, atau (b) pada salah satu baris yang ada, ")
					.append("<u>kosongkan kolom Angkatan</u> (berlaku semua angkatan) atau <u>kosongkan Jurusan/Program</u> (jadi default).");
		}
		s.append("</div>");
		return s.toString();
	}

	// ================= query pemetaan =================

	@SuppressWarnings("unchecked")
	private static List<ItemBiayaPunyaAkun> ambilPemetaanAkun(ItemBiaya itemBiaya) {
		try {
			return HibernateUtil.currentSession().createCriteria(ItemBiayaPunyaAkun.class)
					.add(Restrictions.eq("itemBiaya", itemBiaya)).addOrder(Order.asc("id")).setMaxResults(100).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnalisisPemetaanAkunHelper.ambilPemetaanAkun");
			return new java.util.ArrayList<ItemBiayaPunyaAkun>();
		}
	}

	@SuppressWarnings("unchecked")
	private static List<ItemBiayaPunyaPiutang> ambilPemetaanPiutang(ItemBiaya itemBiaya) {
		try {
			return HibernateUtil.currentSession().createCriteria(ItemBiayaPunyaPiutang.class)
					.add(Restrictions.eq("itemBiaya", itemBiaya)).addOrder(Order.asc("id")).setMaxResults(100).list();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnalisisPemetaanAkunHelper.ambilPemetaanPiutang");
			return new java.util.ArrayList<ItemBiayaPunyaPiutang>();
		}
	}

	// ================= util render =================

	private static String tabelPendapatan(List<ItemBiayaPunyaAkun> list, Fakultas fak, Jurusan jur, String prog,
			String ang) {
		if (list.isEmpty()) {
			return "<div style='color:#9ca3af; font-style:italic;'>(belum ada baris)</div>";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(headTabel());
		for (ItemBiayaPunyaAkun r : list) {
			sb.append(barisTabel(r.getFakultas(), r.getJurusan(), r.getProgram(), r.getAngkatan(),
					r.getAkun() == null ? null : r.getAkun().getNama(), fak, jur, prog, ang));
		}
		sb.append("</tbody></table>");
		return sb.toString();
	}

	private static String tabelPiutang(List<ItemBiayaPunyaPiutang> list, Fakultas fak, Jurusan jur, String prog,
			String ang) {
		if (list.isEmpty()) {
			return "<div style='color:#9ca3af; font-style:italic;'>(belum ada baris)</div>";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(headTabel());
		for (ItemBiayaPunyaPiutang r : list) {
			sb.append(barisTabel(r.getFakultas(), r.getJurusan(), r.getProgram(), r.getAngkatan(),
					r.getAkun() == null ? null : r.getAkun().getNama(), fak, jur, prog, ang));
		}
		sb.append("</tbody></table>");
		return sb.toString();
	}

	private static String headTabel() {
		return "<table style='width:100%; border-collapse:collapse; font-size:12px;'>"
				+ "<thead><tr style='background:#eef2ff;'>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px; text-align:left;'>Fakultas</th>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px; text-align:left;'>Jurusan</th>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px; text-align:left;'>Program</th>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px; text-align:left;'>Angkatan</th>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px; text-align:left;'>Akun</th>"
				+ "<th style='border:1px solid #c7d2fe; padding:4px;'>Cocok?</th></tr></thead><tbody>";
	}

	private static String barisTabel(Fakultas fFak, Jurusan fJur, String fProg, String fAng, String akun, Fakultas fak,
			Jurusan jur, String prog, String ang) {
		boolean cocokJur = fJur != null && jur != null && fJur.getId() != null && fJur.getId().equals(jur.getId());
		boolean cocokFak = fFak != null && fak != null && fFak.getId() != null && fak.getId().equals(fak.getId());
		boolean cocokProg = samaProg(fProg, prog);
		boolean cocokAng = isBlank(fAng) || (ang != null && ang.toLowerCase().contains(fAng.toLowerCase()));
		boolean match = ((fJur != null && cocokJur) || (fJur == null && fFak != null && cocokFak)) && cocokProg && cocokAng;
		String bg = match ? "background:#dcfce7;" : "";
		return "<tr style='" + bg + "'>"
				+ td(fFak == null ? "<i>(semua)</i>" : esc(fFak.getNama()))
				+ td(fJur == null ? "<i>(semua)</i>" : esc(fJur.getNama()))
				+ td(isBlank(fProg) ? "<i>(semua)</i>" : esc(fProg))
				+ td(isBlank(fAng) ? "<i>(semua)</i>" : esc(fAng))
				+ td(akun == null ? "-" : esc(akun))
				+ "<td style='border:1px solid #e5e7eb; padding:4px; text-align:center;'>" + (match ? "&#9989;" : "") + "</td></tr>";
	}

	private static String td(String isi) {
		return "<td style='border:1px solid #e5e7eb; padding:4px;'>" + isi + "</td>";
	}

	private static String kartuKonteks(String label, String nilai) {
		return "<div style='display:inline-block; min-width:170px; margin:2px 14px 2px 0;'>"
				+ "<span style='color:#6b7280;'>" + esc(label) + ":</span> <b>" + nilai + "</b></div>";
	}

	private static String statusAkun(String label, Akun akun) {
		if (akun != null) {
			return "<li style='color:#065f46;'>&#10004; " + esc(label) + ": <b>" + esc(akun.getNama()) + "</b> (ditemukan)</li>";
		}
		return "<li style='color:#b91c1c;'>&#10060; " + esc(label) + ": <b>BELUM terpetakan</b> untuk kombinasi ini</li>";
	}

	private static String blokLangkah(String pesan) {
		if (isBlank(pesan)) {
			return "";
		}
		return "<h4 style='margin:10px 0 6px; color:#111827;'>&#128736; Langkah Perbaikan</h4>"
				+ "<div style='background:#f9fafb; border:1px solid #e5e7eb; border-radius:8px; padding:10px; white-space:pre-wrap; font-size:12px; line-height:1.5;'>"
				+ esc(pesan) + "</div>";
	}

	// ================= resolusi konteks dari kegiatan =================

	/** Isi fak/jur/prog/ang dari kegiatan (mirror ItemBiaya.ambilAkun). Return nama mhs/peserta atau null. */
	private static String resolveKonteks(Kegiatan kegiatan, Fakultas[] fak, Jurusan[] jur, String[] prog, String[] ang) {
		if (kegiatan == null) {
			return null;
		}
		try {
			if (kegiatan.getMahasiswa() != null) {
				jur[0] = kegiatan.getMahasiswa().getJurusan();
				fak[0] = jur[0] == null ? null : jur[0].getFakultas();
				prog[0] = kegiatan.getMahasiswa().getProgram();
				ang[0] = kegiatan.getMahasiswa().getTahunangkatan() == null ? null
						: kegiatan.getMahasiswa().getTahunangkatan().toString();
				return kegiatan.getMahasiswa().toString();
			}
			if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
				jur[0] = kegiatan.getCalonMahasiswa().getProdiLulus();
				fak[0] = jur[0] == null ? null : jur[0].getFakultas();
				prog[0] = kegiatan.getCalonMahasiswa().getProgram();
				ang[0] = kegiatan.getCalonMahasiswa().getTahun() == null ? null
						: kegiatan.getCalonMahasiswa().getTahun().toString();
				return kegiatan.getCalonMahasiswa().toString();
			}
			if (kegiatan.getCalonMahasiswa() != null && kegiatan.getCalonMahasiswa().getProdi1() != null) {
				jur[0] = kegiatan.getCalonMahasiswa().getProdi1();
				fak[0] = jur[0] == null ? null : jur[0].getFakultas();
				prog[0] = kegiatan.getCalonMahasiswa().getProgram();
				ang[0] = kegiatan.getCalonMahasiswa().getTahun() == null ? null
						: kegiatan.getCalonMahasiswa().getTahun().toString();
				return kegiatan.getCalonMahasiswa().toString();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit AnalisisPemetaanAkunHelper.resolveKonteks");
		}
		return null;
	}

	private static boolean samaProg(String a, String b) {
		if (isBlank(a)) {
			return true; // baris default program-kosong cocok utk semua
		}
		return b != null && a.trim().equalsIgnoreCase(b.trim());
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
