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

	/**
	 * Membuka window modal "Analisis Cerdas" berisi konten HTML hasil {@link #buildHtml} —
	 * dipanggil dari listener tombol pada {@link #tampilkanInvalid(Component, String, ItemBiaya, Kegiatan)}.
	 *
	 * @param ref       komponen acuan untuk memperoleh {@code Page} tempat window ditempelkan
	 *                  ({@code ref.getPage().getFirstRoot()})
	 * @param pesan     pesan/langkah perbaikan asli yang diteruskan ke {@link #buildHtml}
	 * @param itemBiaya konteks item biaya (boleh {@code null} untuk mode universal)
	 * @param kegiatan  konteks kegiatan untuk resolusi fakultas/jurusan/program/angkatan (boleh {@code null})
	 * @throws Exception diteruskan apa adanya dari operasi ZK (mis. {@code w.doModal()})
	 */
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

	/**
	 * Menyusun seluruh markup HTML window "Analisis Cerdas": kartu konteks (item biaya, mahasiswa/
	 * peserta, fakultas, jurusan, program, angkatan — hasil {@link #resolveKonteks}), lalu — bila
	 * {@code itemBiaya} tidak {@code null} — bagian diagnosis ({@link #statusAkun} untuk akun
	 * pendapatan dan piutang hasil {@link ItemBiaya#ambilAkun(Kegiatan)}/{@link ItemBiaya#ambilPiutang(Kegiatan)}),
	 * tabel pemetaan yang sudah ada ({@link #tabelPendapatan}/{@link #tabelPiutang} dari hasil
	 * {@link #ambilPemetaanAkun}/{@link #ambilPemetaanPiutang}), dan saran perbaikan
	 * ({@link #saranCerdas}). Diakhiri blok "Langkah Perbaikan" ({@link #blokLangkah}) berisi
	 * {@code pesan} asli apa adanya. Bila {@code itemBiaya} {@code null} (mode universal, tanpa
	 * konteks pemetaan), hanya kartu konteks dan blok langkah yang ditampilkan.
	 *
	 * @param pesan     pesan/langkah perbaikan asli dari pemanggil (ditampilkan verbatim di akhir)
	 * @param itemBiaya konteks item biaya; {@code null} berarti mode universal tanpa deep-query
	 * @param kegiatan  konteks kegiatan untuk resolusi fakultas/jurusan/program/angkatan
	 * @return markup HTML lengkap siap ditempatkan pada komponen {@link Html}
	 */
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

	/**
	 * Menyusun satu blok saran perbaikan (HTML) untuk satu jenis akun (Pendapatan atau Piutang).
	 * Bila {@code akun} sudah ter-resolve, mengembalikan pesan konfirmasi tanpa tindakan. Bila
	 * belum, saran berbeda tergantung {@code kosongTotal}: bila BELUM ADA pemetaan sama sekali,
	 * disarankan membuat satu baris default (Jurusan/Program/Angkatan kosong sebagai fallback
	 * universal); bila SUDAH ADA baris lain namun tidak ada yang cocok dengan kombinasi konteks
	 * saat ini, disarankan menambah baris khusus atau mengosongkan sebagian kriteria pada baris
	 * existing agar mencakup kombinasi tersebut.
	 *
	 * @param jenis      label jenis akun ("Pendapatan" atau "Piutang"), dipakai pada teks saran
	 * @param akun       akun hasil resolve saat ini (bila non-{@code null}, tidak ada saran perbaikan)
	 * @param kosongTotal {@code true} bila daftar pemetaan jenis ini sama sekali kosong
	 * @param fak        fakultas konteks (untuk teks saran baris default)
	 * @param jur        jurusan konteks (untuk teks saran kombinasi yang tidak cocok)
	 * @param prog       program konteks (untuk teks saran kombinasi yang tidak cocok)
	 * @param ang        angkatan konteks (untuk teks saran kombinasi yang tidak cocok)
	 * @return markup HTML satu blok saran
	 */
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

	/**
	 * Mengambil hingga 100 baris {@link ItemBiayaPunyaAkun} (pemetaan akun pendapatan) milik
	 * {@code itemBiaya}, terurut menaik berdasarkan id. Kegagalan query dicatat ke
	 * {@link ais.common.ErrorAuditUtil} dan mengembalikan daftar kosong (bukan melempar exception)
	 * agar window analisis tetap dapat ditampilkan meski query gagal.
	 *
	 * @param itemBiaya item biaya yang pemetaannya dicari
	 * @return daftar pemetaan akun pendapatan (bisa kosong, tidak pernah {@code null})
	 */
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

	/**
	 * Seperti {@link #ambilPemetaanAkun(ItemBiaya)}, untuk pemetaan akun piutang
	 * ({@link ItemBiayaPunyaPiutang}).
	 *
	 * @param itemBiaya item biaya yang pemetaannya dicari
	 * @return daftar pemetaan akun piutang (bisa kosong, tidak pernah {@code null})
	 */
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

	/**
	 * Merender tabel HTML baris-baris {@link ItemBiayaPunyaAkun} (pemetaan akun pendapatan) yang
	 * sudah ada, dengan baris yang kombinasi Fakultas/Jurusan/Program/Angkatan-nya cocok dengan
	 * konteks saat ini ditandai hijau (lihat {@link #barisTabel}).
	 *
	 * @param list daftar pemetaan hasil {@link #ambilPemetaanAkun(ItemBiaya)}
	 * @param fak  fakultas konteks saat ini (untuk penandaan baris cocok)
	 * @param jur  jurusan konteks saat ini
	 * @param prog program konteks saat ini
	 * @param ang  angkatan konteks saat ini
	 * @return markup HTML tabel, atau pesan "(belum ada baris)" bila {@code list} kosong
	 */
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

	/**
	 * Seperti {@link #tabelPendapatan}, untuk baris {@link ItemBiayaPunyaPiutang} (pemetaan akun
	 * piutang).
	 *
	 * @param list daftar pemetaan hasil {@link #ambilPemetaanPiutang(ItemBiaya)}
	 * @param fak  fakultas konteks saat ini
	 * @param jur  jurusan konteks saat ini
	 * @param prog program konteks saat ini
	 * @param ang  angkatan konteks saat ini
	 * @return markup HTML tabel, atau pesan "(belum ada baris)" bila {@code list} kosong
	 */
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

	/** Membangun tag pembuka {@code <table>} + {@code <thead>} bersama untuk {@link #tabelPendapatan}/{@link #tabelPiutang} (kolom Fakultas/Jurusan/Program/Angkatan/Akun/Cocok?). */
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

	/**
	 * Merender satu baris {@code <tr>} tabel pemetaan (baik pendapatan maupun piutang), memakai
	 * placeholder {@code (semua)} untuk kolom Fakultas/Jurusan/Program/Angkatan yang kosong
	 * (berarti baris tersebut berlaku sebagai fallback untuk semua nilai kolom itu), dan menandai
	 * baris dengan latar hijau ({@code background:#dcfce7}) plus centang bila baris ini dianggap
	 * "cocok" dengan konteks ({@code fak}/{@code jur}/{@code prog}/{@code ang}) saat ini: jurusan
	 * baris sama dengan jurusan konteks, ATAU (bila jurusan baris kosong) fakultas baris cocok
	 * dengan fakultas konteks — DIGABUNG dengan kecocokan program ({@link #samaProg}) dan angkatan
	 * (baris dengan angkatan kosong selalu cocok; selain itu dicek substring, tidak sensitif huruf
	 * besar/kecil).
	 *
	 * <p><b>Catatan bug (salin-tempel, hanya memengaruhi tampilan diagnosis, bukan data):</b>
	 * {@code cocokFak} seharusnya membandingkan {@code fFak.getId().equals(fak.getId())}, namun
	 * baris kode saat ini membandingkan {@code fak.getId().equals(fak.getId())} — self-comparison
	 * yang SELALU {@code true} bila {@code fFak} dan {@code fak} sama-sama non-{@code null} dan
	 * ber-id, terlepas dari apakah fakultas baris benar-benar sama dengan fakultas konteks. Efeknya
	 * terbatas pada penandaan hijau/centang "Cocok?" di window Analisis Cerdas (bisa salah
	 * menandai baris fallback fakultas lain sebagai cocok); tidak memengaruhi resolusi akun aktual
	 * ({@link ItemBiaya#ambilAkun}/{@link ItemBiaya#ambilPiutang} tetap dipakai terpisah untuk itu).</p>
	 *
	 * @param fFak  fakultas pada baris pemetaan (boleh {@code null} = berlaku semua fakultas)
	 * @param fJur  jurusan pada baris pemetaan (boleh {@code null} = berlaku semua jurusan)
	 * @param fProg program pada baris pemetaan (boleh kosong = berlaku semua program)
	 * @param fAng  angkatan pada baris pemetaan (boleh kosong = berlaku semua angkatan)
	 * @param akun  nama akun tujuan baris ini (boleh {@code null})
	 * @param fak   fakultas konteks transaksi saat ini
	 * @param jur   jurusan konteks transaksi saat ini
	 * @param prog  program konteks transaksi saat ini
	 * @param ang   angkatan konteks transaksi saat ini
	 * @return markup HTML satu baris {@code <tr>}
	 */
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

	/** Membungkus {@code isi} pada satu sel {@code <td>} dengan gaya border seragam tabel pemetaan. */
	private static String td(String isi) {
		return "<td style='border:1px solid #e5e7eb; padding:4px;'>" + isi + "</td>";
	}

	/**
	 * Merender satu "kartu" info inline (label abu-abu + nilai tebal) pada bagian konteks window
	 * Analisis Cerdas. {@code nilai} DIMASUKKAN APA ADANYA (tidak di-escape di sini) — pemanggil
	 * bertanggung jawab meng-escape lewat {@link #esc(String)} bila nilainya berasal dari data
	 * pengguna/database.
	 *
	 * @param label label kolom (di-escape otomatis)
	 * @param nilai nilai yang ditampilkan (HTML mentah, TIDAK di-escape oleh method ini)
	 * @return markup HTML satu kartu info
	 */
	private static String kartuKonteks(String label, String nilai) {
		return "<div style='display:inline-block; min-width:170px; margin:2px 14px 2px 0;'>"
				+ "<span style='color:#6b7280;'>" + esc(label) + ":</span> <b>" + nilai + "</b></div>";
	}

	/**
	 * Merender satu baris {@code <li>} bagian "Diagnosis": centang hijau + nama akun bila
	 * {@code akun} ter-resolve, atau silang merah + "BELUM terpetakan" bila {@code null}.
	 *
	 * @param label label jenis akun ("Akun Pendapatan"/"Akun Piutang")
	 * @param akun  akun hasil resolve; {@code null} berarti belum terpetakan untuk kombinasi ini
	 * @return markup HTML satu item daftar diagnosis
	 */
	private static String statusAkun(String label, Akun akun) {
		if (akun != null) {
			return "<li style='color:#065f46;'>&#10004; " + esc(label) + ": <b>" + esc(akun.getNama()) + "</b> (ditemukan)</li>";
		}
		return "<li style='color:#b91c1c;'>&#10060; " + esc(label) + ": <b>BELUM terpetakan</b> untuk kombinasi ini</li>";
	}

	/**
	 * Merender blok "Langkah Perbaikan" berisi {@code pesan} asli (di-escape, dengan
	 * {@code white-space:pre-wrap} agar baris baru pada pesan tetap tampil). Mengembalikan string
	 * kosong (tidak ada blok sama sekali) bila {@code pesan} kosong/{@code null}.
	 *
	 * @param pesan teks pesan/langkah perbaikan asli dari pemanggil
	 * @return markup HTML blok langkah perbaikan, atau string kosong bila {@code pesan} kosong
	 */
	private static String blokLangkah(String pesan) {
		if (isBlank(pesan)) {
			return "";
		}
		return "<h4 style='margin:10px 0 6px; color:#111827;'>&#128736; Langkah Perbaikan</h4>"
				+ "<div style='background:#f9fafb; border:1px solid #e5e7eb; border-radius:8px; padding:10px; white-space:pre-wrap; font-size:12px; line-height:1.5;'>"
				+ esc(pesan) + "</div>";
	}

	// ================= resolusi konteks dari kegiatan =================

	/**
	 * Mengisi array keluaran {@code fak}/{@code jur}/{@code prog}/{@code ang} (indeks 0) dari data
	 * mahasiswa/calon mahasiswa pada {@code kegiatan} — logika resolusi ini MENCERMINKAN
	 * (mirror) urutan pengecekan pada {@code ItemBiaya.ambilAkun}/{@code ambilPiutang} agar konteks
	 * yang ditampilkan di window analisis konsisten dengan konteks yang sesungguhnya dipakai untuk
	 * resolve akun: (1) bila {@code kegiatan.getMahasiswa()} ada, pakai jurusan/program/tahun
	 * angkatan mahasiswa; (2) selain itu bila ada {@code CalonMahasiswa} dengan
	 * {@code prodiLulus}, pakai itu; (3) selain itu bila ada {@code CalonMahasiswa} dengan
	 * {@code prodi1}, pakai itu. Kegagalan (mis. lazy-load gagal) dicatat ke
	 * {@link ais.common.ErrorAuditUtil} dan diperlakukan seolah tidak ada konteks yang ditemukan.
	 *
	 * @param kegiatan kegiatan sumber konteks; {@code null} langsung mengembalikan {@code null}
	 *                 tanpa mengisi array keluaran
	 * @param fak      keluaran: fakultas hasil resolusi (elemen 0 diisi bila ditemukan)
	 * @param jur      keluaran: jurusan/prodi hasil resolusi
	 * @param prog     keluaran: nama program hasil resolusi
	 * @param ang      keluaran: tahun angkatan (sebagai String) hasil resolusi
	 * @return representasi string mahasiswa/calon mahasiswa (dipakai sebagai label "Mahasiswa/Peserta"),
	 *         atau {@code null} bila tidak ada konteks yang dapat diresolusi
	 */
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

	/**
	 * Membandingkan nama program baris pemetaan ({@code a}) dengan program konteks ({@code b})
	 * tanpa memandang huruf besar/kecil dan spasi di ujung. Program kosong pada baris pemetaan
	 * ({@code a}) dianggap berlaku untuk SEMUA program (baris default/fallback), sehingga selalu
	 * dianggap cocok.
	 *
	 * @param a nama program pada baris pemetaan (boleh kosong = berlaku semua program)
	 * @param b nama program konteks transaksi saat ini
	 * @return {@code true} bila {@code a} kosong, atau {@code a} sama (case-insensitive) dengan {@code b}
	 */
	private static boolean samaProg(String a, String b) {
		if (isBlank(a)) {
			return true; // baris default program-kosong cocok utk semua
		}
		return b != null && a.trim().equalsIgnoreCase(b.trim());
	}

	/**
	 * @param s string yang diperiksa
	 * @return {@code true} bila {@code s} bernilai {@code null} atau, setelah di-trim, kosong
	 */
	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Meng-escape karakter HTML dasar ({@code &}, {@code <}, {@code >}) pada {@code s} agar aman
	 * disisipkan sebagai teks di dalam markup HTML window Analisis Cerdas (mencegah data yang
	 * mengandung tag/markup tak sengaja dirender sebagai HTML).
	 *
	 * @param s string sumber (boleh {@code null})
	 * @return string ter-escape, atau string kosong bila {@code s} bernilai {@code null}
	 */
	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
