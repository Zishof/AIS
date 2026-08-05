package ais.action.master.dashboard.admin;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Combobox;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.RingkasanKampusCache;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyHtml;

/**
 * <h2>RekapMahasiswaViewHelper — tampilan ringkasan mahasiswa yang dipakai BERSAMA</h2>
 *
 * <p>Komponen reusable: semua laporan rekap mahasiswa (LaporanRekapJumlahMahasiswa, *Angkatan,
 * *Program, Rasio, DashboardRekapMahasiswa, Rekapitulasi*, dll.) cukup memanggil
 * {@link #ringkasanHtml(String, String, Long, Long)} untuk menampilkan RINGKASAN ringan
 * (kartu angka + chart bar/spider/tren/funnel berbasis HTML/CSS, tanpa jfreechart) dengan
 * deskripsi 1 kalimat yang mudah dipahami end-user.</p>
 *
 * <p>Angka diambil dari {@link RingkasanKampusCache} (memori, dihitung memakai logika
 * LaporanRekapJumlahMahasiswa) — JADI CEPAT &amp; konsisten. Tombol "Hitung Ulang" cukup
 * memanggil {@link RingkasanKampusCache#hitungUlang(String, String)} lalu render ulang.</p>
 *
 * <p>Maksud "maksimalkan reuse": ubah tampilan sekali di sini, semua laporan ikut.</p>
 */
public final class RekapMahasiswaViewHelper {

	private RekapMahasiswaViewHelper() {
	}

	private static Long sid(StatusMahasiswa s) {
		return s == null ? null : s.getId();
	}

	/** Status label + deskripsi end-user (urut tampil). */
	private static final String[] LABELS = { "Aktif", "Cuti", "Lulus", "Keluar / DO", "Tidak Aktif" };

	private static int[] statusCountsProdi(String ta, String sem, Long jurusanId) {
		int aktif = RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.AKTIF), jurusanId);
		int cuti = RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.CUTI), jurusanId);
		int lulus = RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.LULUS), jurusanId);
		int doo = RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.KELUAR), jurusanId)
				+ RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.DROP_OUT), jurusanId);
		int tidak = ConstantValues.TIDAK_AKTIF == null ? 0
				: RingkasanKampusCache.countMahasiswaProdi(ta, sem, sid(ConstantValues.TIDAK_AKTIF), jurusanId);
		return new int[] { aktif, cuti, lulus, doo, tidak };
	}

	/**
	 * HTML ringkasan lengkap (kartu + bar per prodi + spider profil prodi + tren per angkatan +
	 * funnel) untuk lingkup (TA, semester, fakultas, jurusan). null = semua.
	 */
	public static String ringkasanHtml(String ta, String sem, Long fakultasId, Long jurusanId) {
		StringBuffer html = new StringBuffer();

		// ── Total lingkup (untuk kartu & funnel) ──
		int tAktif = RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.AKTIF), fakultasId, jurusanId);
		int tCuti = RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.CUTI), fakultasId, jurusanId);
		int tLulus = RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.LULUS), fakultasId, jurusanId);
		int tDO = RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.KELUAR), fakultasId, jurusanId)
				+ RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.DROP_OUT), fakultasId, jurusanId);
		int tTidak = ConstantValues.TIDAK_AKTIF == null ? 0
				: RingkasanKampusCache.countMahasiswa(ta, sem, sid(ConstantValues.TIDAK_AKTIF), fakultasId, jurusanId);

		StringBuffer stats = new StringBuffer("<div class=\"ais-akad-stats\">");
		stats.append(DashboardAkademikHtmlCssHelper.stat("Mahasiswa Aktif", DashboardAkademikHtmlCssHelper.fmt(tAktif),
				"Mahasiswa yang masih kuliah pada semester ini."));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Cuti", DashboardAkademikHtmlCssHelper.fmt(tCuti),
				"Berhenti sementara (cuti) pada semester ini."));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Lulus", DashboardAkademikHtmlCssHelper.fmt(tLulus),
				"Sudah lulus sampai semester ini."));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Keluar / DO", DashboardAkademikHtmlCssHelper.fmt(tDO),
				"Keluar atau dikeluarkan (drop out)."));
		stats.append(DashboardAkademikHtmlCssHelper.stat("Tidak Aktif", DashboardAkademikHtmlCssHelper.fmt(tTidak),
				"Tidak mendaftar ulang / tidak aktif."));
		stats.append("</div>");
		html.append(DashboardAkademikHtmlCssHelper.panel("Ringkasan Mahasiswa",
				"Jumlah mahasiswa menurut keadaannya pada tahun &amp; semester yang dipilih.", stats.toString()));

		// ── Per prodi (bar + spider) ──
		List<Long> jids = RingkasanKampusCache.jurusanIds(ta, sem, fakultasId);
		List<String> labels = new ArrayList<String>();
		List<int[]> values = new ArrayList<int[]>();
		List<String> spiderNames = new ArrayList<String>();
		List<int[]> spiderVals = new ArrayList<int[]>();
		int idx = 0;
		for (Long jid : jids) {
			int[] c = statusCountsProdi(ta, sem, jid);
			String nama = "Prodi " + jid;
			labels.add(nama);
			values.add(c);
			if (idx < 6) { // spider: maksimal 6 prodi agar tetap terbaca
				spiderNames.add(nama);
				spiderVals.add(c);
			}
			idx++;
		}
		if (!labels.isEmpty()) {
			html.append(DashboardAkademikHtmlCssHelper.groupedBarChart("Perbandingan per Program Studi",
					"Membandingkan keadaan mahasiswa di tiap program studi.", labels, LABELS, values));
			html.append(DashboardAkademikHtmlCssHelper.spiderChart("Profil Tiap Prodi",
					"Bentuk 'sidik jari' tiap prodi untuk dibandingkan sekilas.", LABELS, spiderNames, spiderVals));
		}

		// ── Tren per angkatan (aktif & lulus) ──
		List<Integer> angks = RingkasanKampusCache.angkatanList(ta, sem);
		if (angks != null && !angks.isEmpty()) {
			List<String> xs = new ArrayList<String>();
			int[] aktifSeri = new int[angks.size()];
			int[] lulusSeri = new int[angks.size()];
			for (int i = 0; i < angks.size(); i++) {
				Integer a = angks.get(i);
				xs.add(String.valueOf(a));
				aktifSeri[i] = RingkasanKampusCache.countAngkatan(ta, sem, sid(ConstantValues.AKTIF), a);
				lulusSeri[i] = RingkasanKampusCache.countAngkatan(ta, sem, sid(ConstantValues.LULUS), a);
			}
			List<String> sn = new ArrayList<String>();
			sn.add("Aktif");
			sn.add("Lulus");
			List<int[]> sv = new ArrayList<int[]>();
			sv.add(aktifSeri);
			sv.add(lulusSeri);
			html.append(DashboardAkademikHtmlCssHelper.trendLineChart("Tren per Angkatan",
					"Perkembangan jumlah mahasiswa aktif &amp; lulusan dari tahun ke tahun.", xs, sn, sv));
		}

		// ── Funnel: aktif -> lulus ──
		html.append(DashboardAkademikHtmlCssHelper.funnelChart("Dari Aktif sampai Lulus",
				"Gambaran berapa yang masih aktif dibanding yang sudah lulus.",
				new String[] { "Aktif", "Lulus" }, new int[] { tAktif, tLulus }));

		return html.toString();
	}

	private static Long idFakultas(Combobox c) {
		Object v = (c == null || c.getSelectedItem() == null) ? null : c.getSelectedItem().getValue();
		return (v instanceof Fakultas) ? ((Fakultas) v).getId() : null;
	}

	private static Long idJurusan(Combobox c) {
		Object v = (c == null || c.getSelectedItem() == null) ? null : c.getSelectedItem().getValue();
		return (v instanceof Jurusan) ? ((Jurusan) v).getId() : null;
	}

	private static String valTa(Combobox c) {
		Object v = (c == null || c.getSelectedItem() == null) ? null : c.getSelectedItem().getValue();
		return v == null ? Common.getCurrentTahunAkademik() : v.toString();
	}

	private static String valSem(Combobox c) {
		Object v = (c == null || c.getSelectedItem() == null) ? null : c.getSelectedItem().getValue();
		return v == null ? Common.getSemesterString() : v.toString();
	}

	/**
	 * Bangun cache di THREAD LATAR + tampilkan PROGRESS BAR (fase + persen) di {@code center},
	 * lalu render ringkasan saat selesai. Tidak memblokir UI (tidak ada lagi "Harap tunggu" yang
	 * seakan tak berhenti). {@code recompute=true} = "Hitung Ulang" (paksa hitung ulang dari DB).
	 */
	public static void renderDenganProgress(final Component center, final String ta, final String sem,
			final Long fakId, final Long jurId, final boolean recompute) {
		if (center == null) {
			return;
		}
		Common.clear(center);
		final org.zkoss.zul.Vbox box = new org.zkoss.zul.Vbox();
		box.setWidth("100%");
		box.setStyle("padding:28px 16px;align-items:center;");
		box.setParent(center);
		final org.zkoss.zul.Label lblFase = new org.zkoss.zul.Label(
				recompute ? "Menghitung ulang dari database…" : "Menyiapkan data…");
		lblFase.setStyle("font-size:13px;font-weight:bold;color:#1e40af;margin-bottom:10px;");
		lblFase.setParent(box);
		final org.zkoss.zul.Progressmeter meter = new org.zkoss.zul.Progressmeter(0);
		meter.setWidth("440px");
		meter.setStyle("max-width:80%;height:16px;");
		meter.setParent(box);
		final org.zkoss.zul.Label lblPct = new org.zkoss.zul.Label("0%");
		lblPct.setStyle("font-size:12px;color:#475569;margin-top:8px;");
		lblPct.setParent(box);

		final boolean[] selesai = { false };
		final Throwable[] gagal = { null };
		Thread bg = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (recompute) {
						RingkasanKampusCache.hitungUlang(ta, sem);
					} else {
						RingkasanKampusCache.get(ta, sem);
					}
				} catch (Throwable t) {
					gagal[0] = t;
				} finally {
					selesai[0] = true;
				}
			}
		}, "ringkasan-ui-build");
		bg.setDaemon(true);

		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer();
		timer.setDelay(500);
		timer.setRepeats(true);
		timer.setParent(box);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int pct = RingkasanKampusCache.progressPersen();
				String fase = RingkasanKampusCache.progressFase;
				int total = RingkasanKampusCache.progressTotal;
				int proses = RingkasanKampusCache.progressProses;
				lblFase.setValue(fase == null || fase.length() == 0 ? "Memproses…" : fase);
				meter.setValue(selesai[0] ? 100 : pct);
				lblPct.setValue((selesai[0] ? 100 : pct) + "%"
						+ (total > 0 ? "  (" + proses + " / " + total + " mahasiswa)" : ""));
				if (selesai[0]) {
					timer.stop();
					Common.clear(center);
					if (gagal[0] != null) {
						org.zkoss.zul.Label err = new org.zkoss.zul.Label(
								"Gagal memuat ringkasan: " + gagal[0].getMessage());
						err.setStyle("color:#b91c1c;");
						err.setParent(center);
					} else {
						MyHtml h = new MyHtml();
						h.setContent(ringkasanHtml(ta, sem, fakId, jurId));
						h.setParent(center);
					}
				}
			}
		});
		bg.start();
		timer.start();
	}

	/**
	 * Pasang tombol "Ringkasan" (tampil cepat dari cache ke {@code center}) dan "Hitung Ulang"
	 * (recompute DB lalu render ulang) ke {@code anchorRow}. Filter combo boleh null (= semua).
	 * Dipakai SEMUA laporan rekap mahasiswa -&gt; reuse penuh (ubah sekali, semua ikut).
	 */
	public static void pasangTombolRingkasan(Component anchorRow, final Component center, final Combobox tahun,
			final Combobox semester, final Combobox fakultas, final Combobox jurusan) {
		if (anchorRow == null || center == null) {
			return;
		}
		// Rapikan posisi: kumpulkan SEMUA isi baris tombol (mis. "Lihat Laporan") + 2 tombol baru
		// ke dalam 1 Hbox yang span 2 kolom -> bar tombol sejajar (hindari banyak sel meluap di
		// grid 2 kolom yang membuat posisi tombol tak rata).
		org.zkoss.zul.Hbox bar = new org.zkoss.zul.Hbox();
		bar.setSpacing("6px");
		java.util.List<Component> isiLama = new java.util.ArrayList<Component>(anchorRow.getChildren());
		for (Component c : isiLama) {
			c.setParent(bar);
		}
		anchorRow.appendChild(bar);
		ais.ui.util.ZkCompat.setSpans(anchorRow, "2");

		MyButtonConfig bRingkasan = new MyButtonConfig("Ringkasan");
		bRingkasan.setTooltiptext("Tampilan ringkas & cepat (dari memori): kartu + grafik.");
		bar.appendChild(bRingkasan);
		MyButtonConfig bHitungUlang = new MyButtonConfig("Hitung Ulang");
		bHitungUlang.setTooltiptext("Hitung ulang dari database bila data berubah.");
		bar.appendChild(bHitungUlang);

		final EventListener render = new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				final boolean recompute = ev != null && Boolean.TRUE.equals(ev.getData());
				final String ta = valTa(tahun);
				final String sem = valSem(semester);
				final Long fakId = idFakultas(fakultas);
				final Long jurId = idJurusan(jurusan);
				// Build di LATAR + progress bar (tidak memblokir UI; menampilkan fase & persen).
				renderDenganProgress(center, ta, sem, fakId, jurId, recompute);
			}
		};
		bRingkasan.addEventListener("onClick", render);
		bHitungUlang.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				render.onEvent(new Event("onClick", null, Boolean.TRUE));
			}
		});
	}
}
