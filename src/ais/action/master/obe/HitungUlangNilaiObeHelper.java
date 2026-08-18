package ais.action.master.obe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zul.Label;

import ais.action.master.helper.ProsesUjianHelper;
import ais.common.Common;
import ais.common.DbThreadPool;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.ui.util.MyArrayList;

/**
 * <h2>Hitung Ulang Nilai OBE (Ujian &amp; Tugas) &mdash; batch per filter Dasbor OBE</h2>
 *
 * <p>
 * Dipakai oleh dua tombol di Dasbor OBE ("Hitung Ulang Ujian" / "Hitung Ulang Tugas"). Menghitung ulang
 * nilai seluruh perkuliahan berkurikulum OBE sesuai filter, dalam dua tahap:
 * <ol>
 *   <li><b>PRATINJAU</b> ({@link #pratinjauUjian}/{@link #pratinjauTugas}) &mdash; menghitung nilai BARU
 *       tanpa menyimpan (transaksi di-<i>rollback</i>), lalu mengembalikan daftar {@link Baris}
 *       (lama &rarr; baru) untuk ditampilkan &amp; diekspor.</li>
 *   <li><b>SIMPAN</b> ({@link #simpanUjian}/{@link #simpanTugas}) &mdash; menjalankan perhitungan yang
 *       sama namun meng-<i>commit</i> ke DB dan menyegarkan cache.</li>
 * </ol>
 *
 * <p>
 * <b>Ujian</b> ({@link HasilUjianMahasiswa}) dihitung dari jawaban memakai mesin resmi
 * {@link ProsesUjianHelper#hitungObe}/{@link ProsesUjianHelper#hitungPilihanGanda} (hanya jenis
 * PILIHAN_GANDA yang auto-skor; esai dinilai manual). Pola paralel/session-per-thread mengikuti
 * "Hitung Ulang Semua" di HasilUjianMahasiswaHelper. <b>Tugas</b> (individu &amp; kelompok) bernilai
 * OBE dihitung ulang sebagai rata-rata BERBOBOT Sub-CPMK dari {@code keteranganNilai}
 * (Σ nilai&times;bobot / Σ bobot), lalu ditulis ke kunci total {@code <memberKey>_nilai}.
 *
 * <p>
 * Semua sesi Hibernate dibuka per-thread dan ditutup di {@code finally}; kegagalan satu item tidak
 * menggagalkan yang lain (rollback lokal + lanjut).
 */
public final class HitungUlangNilaiObeHelper {

	private HitungUlangNilaiObeHelper() {
	}

	/** Satu baris hasil hitung ulang (untuk grid &amp; ekspor). */
	public static final class Baris {
		public final String kodeMk;
		public final String namaMk;
		public final String item; // nama ujian / tugas
		public final String mahasiswa;
		public final double nilaiLama;
		public final double nilaiBaru;

		public Baris(String kodeMk, String namaMk, String item, String mahasiswa, double nilaiLama, double nilaiBaru) {
			this.kodeMk = kodeMk;
			this.namaMk = namaMk;
			this.item = item;
			this.mahasiswa = mahasiswa;
			this.nilaiLama = nilaiLama;
			this.nilaiBaru = nilaiBaru;
		}
	}

	// =====================================================================================
	// UJIAN
	// =====================================================================================

	/** Pratinjau (tanpa simpan) hitung ulang Ujian PILIHAN GANDA seluruh perkuliahan pada {@code perkuliahanIds}. */
	public static List<Baris> pratinjauUjian(List<Long> perkuliahanIds, Label progress) {
		return prosesUjian(perkuliahanIds, progress, false);
	}

	/** Hitung ulang + SIMPAN Ujian PILIHAN GANDA; mengembalikan daftar baris (lama &rarr; baru tersimpan). */
	public static List<Baris> simpanUjian(List<Long> perkuliahanIds, Label progress) {
		return prosesUjian(perkuliahanIds, progress, true);
	}

	@SuppressWarnings("unchecked")
	private static List<Baris> prosesUjian(List<Long> perkuliahanIds, final Label progress, final boolean simpan) {
		final ConcurrentLinkedQueue<Baris> hasil = new ConcurrentLinkedQueue<Baris>();
		if (perkuliahanIds == null || perkuliahanIds.isEmpty()) {
			return new ArrayList<Baris>();
		}

		// 1) Kumpulkan id HasilUjianMahasiswa untuk semua perkuliahan (jenis ujian PILIHAN GANDA saja).
		List<Object[]> daftar = new ArrayList<Object[]>(); // [humId, kodeMk, namaMk, namaUjian, namaMhs]
		Session s = null;
		try {
			s = HibernateUtil.openSession();
			Criteria c = s.createCriteria(HasilUjianMahasiswa.class, "hum")
					.createAlias("hum.pertemuanPunyaUjian", "ppu")
					.createAlias("ppu.ujian", "u")
					.createAlias("ppu.pertemuan", "p")
					.createAlias("p.perkuliahan", "pk")
					.createAlias("pk.kurikulumPunyaMatakuliah", "kpm")
					.createAlias("kpm.matakuliah", "mk")
					.add(Restrictions.in("pk.id", perkuliahanIds))
					.add(Restrictions.eq("u.jenis", BankSoal.PILIHAN_GANDA));
			List<HasilUjianMahasiswa> list = c.list();
			for (HasilUjianMahasiswa hum : list) {
				if (hum == null || hum.getId() == null) {
					continue;
				}
				daftar.add(new Object[] { hum.getId(), amanKode(hum), amanNama(hum), amanUjian(hum), amanMhs(hum) });
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		if (daftar.isEmpty()) {
			return new ArrayList<Baris>();
		}

		// 2) Proses paralel, satu session per item; PRATINJAU = rollback, SIMPAN = commit.
		final int total = daftar.size();
		final AtomicInteger diproses = new AtomicInteger(0);
		ExecutorService ex = Executors.newFixedThreadPool(DbThreadPool.safe(50));
		final CountDownLatch latch = new CountDownLatch(total);
		for (final Object[] meta : daftar) {
			ex.submit(new Runnable() {
				@Override
				public void run() {
					Session session = null;
					Transaction tx = null;
					try {
						Long id = (Long) meta[0];
						session = HibernateUtil.getSessionFactory().openSession();
						HasilUjianMahasiswa hum = (HasilUjianMahasiswa) session.get(HasilUjianMahasiswa.class, id);
						if (hum == null) {
							return;
						}
						double lama = amanNilai(hum);

						tx = session.beginTransaction();
						PertemuanPunyaUjian ppu = hum.getPertemuanPunyaUjian();
						int jml = ppu.getJmlDitampilkan() == null ? 0 : ppu.getJmlDitampilkan();
						MyArrayList<Long> soals = hum.ambilUjianPunyaSoals(jml, new Label(), true);
						java.util.Map<Long, Set<Long>> det = hum.ambilHasilUjianMahasiswaDetail(jml, soals, false);
						hum.setJumlahSoal(jml * 1.0);
						ProsesUjianHelper.hitungObe(hum, det);
						try {
							ProsesUjianHelper.hitungPilihanGanda(hum, det);
						} catch (Exception ePg) { ais.common.ErrorAuditUtil.record(ePg, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:165");
							// Kegagalan nilai PG tak boleh menggagalkan OBE.
						}
						double baru = amanNilai(hum);

						if (simpan) {
							session.update(hum);
							tx.commit();
							tx = null;
							try {
								GeneralValueObject.masukkanDataLangsung(HasilUjianMahasiswa.class, hum,
										hum.getKeyhasil());
							} catch (Exception eCache) { ais.common.ErrorAuditUtil.record(eCache, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:177");
							}
						} else {
							tx.rollback(); // PRATINJAU: jangan simpan.
							tx = null;
						}

						hasil.add(new Baris((String) meta[1], (String) meta[2], (String) meta[3], (String) meta[4], lama,
								baru));
					} catch (Exception e) {
						if (tx != null) {
							try {
								tx.rollback();
							} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:190");
							}
						}
					} finally {
						int cur = diproses.incrementAndGet();
						setProgress(progress, cur, total, simpan);
						if (session != null) {
							try {
								if (session.isOpen()) {
									session.clear();
								}
							} catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:201");
							}
							try {
								session.disconnect();
							} catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:205");
							}
							try {
								if (session.isOpen()) {
									session.close();
								}
							} catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:211");
							}
						}
						latch.countDown();
					}
				}
			});
		}
		ex.shutdown();
		try {
			latch.await(2, TimeUnit.HOURS);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return urut(hasil);
	}

	// =====================================================================================
	// TUGAS (individu + kelompok) — agregasi ulang OBE Sub-CPMK
	// =====================================================================================

	/** Pratinjau (tanpa simpan) hitung ulang nilai Tugas &amp; Tugas Kelompok OBE. */
	public static List<Baris> pratinjauTugas(List<Long> perkuliahanIds, Label progress) {
		return prosesTugas(perkuliahanIds, progress, false);
	}

	/** Hitung ulang + SIMPAN nilai Tugas &amp; Tugas Kelompok OBE. */
	public static List<Baris> simpanTugas(List<Long> perkuliahanIds, Label progress) {
		return prosesTugas(perkuliahanIds, progress, true);
	}

	@SuppressWarnings("unchecked")
	private static List<Baris> prosesTugas(List<Long> perkuliahanIds, Label progress, boolean simpan) {
		List<Baris> hasil = new ArrayList<Baris>();
		if (perkuliahanIds == null || perkuliahanIds.isEmpty()) {
			return hasil;
		}
		Session s = null;
		Transaction tx = null;
		try {
			s = HibernateUtil.openSession();
			tx = s.beginTransaction();

			List<Tugas> tugasSemua = new ArrayList<Tugas>();
			// Tugas Kelompok: relasi perkuliahan langsung.
			tugasSemua.addAll(s.createCriteria(TugasKelompok.class, "tk").createAlias("tk.perkuliahan", "pk")
					.add(Restrictions.in("pk.id", perkuliahanIds)).list());
			// Tugas Pertemuan: kolom `pertemuan` = Long FK (bukan relasi) → ambil id pertemuan milik
			// perkuliahan dulu, lalu tugas_pertemuan yang pertemuan-nya termasuk.
			List<Long> pertemuanIds = s.createCriteria(ais.database.model.Pertemuan.class, "p")
					.createAlias("p.perkuliahan", "pk").add(Restrictions.in("pk.id", perkuliahanIds))
					.setProjection(org.hibernate.criterion.Projections.id()).list();
			if (pertemuanIds != null && !pertemuanIds.isEmpty()) {
				tugasSemua.addAll(s.createCriteria(TugasPertemuan.class, "tp")
						.add(Restrictions.in("tp.pertemuan", pertemuanIds)).list());
			}

			int total = tugasSemua.size();
			int idx = 0;
			for (Tugas tugas : tugasSemua) {
				idx++;
				setProgress(progress, idx, total, simpan);
				if (tugas == null) {
					continue;
				}
				JSONObject bobot = amanJson(tugas.getFormatNilais());
				JSONObject ket = amanJson(tugas.getKeteranganNilai());
				if (bobot.length() == 0 || ket.length() == 0) {
					continue; // bukan tugas OBE / belum dinilai.
				}
				String kodeMk = amanKodeMk(tugas);
				String namaMk = amanNamaMk(tugas);
				String namaTugas = amanNamaTugas(tugas);

				boolean berubah = false;
				for (String memberKey : memberKeys(ket)) {
					double lama = ket.optDouble(memberKey + "_nilai", 0.0);
					double baru = rataBerbobotObe(bobot, ket, memberKey);
					if (simpan) {
						ket.put(memberKey + "_nilai", baru);
						berubah = true;
					}
					hasil.add(new Baris(kodeMk, namaMk, namaTugas, labelMember(memberKey), lama, baru));
				}
				if (simpan && berubah) {
					tugas.setKeteranganNilai(ket.toString());
					s.update(tugas);
				}
			}
			if (simpan) {
				tx.commit();
			} else {
				tx.rollback();
			}
			tx = null;
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:310");
				}
			}
			Common.tampilErrorJikaAdmin(e);
		} finally {
			HibernateUtil.closeSessionQuietly(s);
		}
		return hasil;
	}

	/** Rata-rata berbobot Sub-CPMK (Σ nilai&times;bobot / Σ bobot) — replika hitungNilaiObeMember. */
	private static double rataBerbobotObe(JSONObject bobot, JSONObject ket, String memberKey) {
		double berbobot = 0.0, totalBobot = 0.0;
		java.util.Iterator<String> keys = bobot.keys();
		while (keys.hasNext()) {
			String fnId = keys.next();
			double b = bobot.optDouble(fnId, 0.0);
			if (b <= 0) {
				continue;
			}
			double n = ket.optDouble(memberKey + "_nilai_" + fnId, 0.0);
			berbobot += n * b;
			totalBobot += b;
		}
		return totalBobot > 0 ? berbobot / totalBobot : 0.0;
	}

	/** Semua memberKey (mis. "123_mhs") yang punya minimal satu komponen "_nilai_<fnId>" di keteranganNilai. */
	private static Set<String> memberKeys(JSONObject ket) {
		Set<String> out = new LinkedHashSet<String>();
		java.util.Iterator<String> keys = ket.keys();
		while (keys.hasNext()) {
			String k = keys.next();
			int pos = k.indexOf("_nilai_");
			if (pos > 0) {
				String rest = k.substring(pos + "_nilai_".length());
				// hanya komponen Sub-CPMK (suffix numerik) → ambil memberKey di depan.
				// (loop manual; hindari String.chars()/method-reference yang butuh Java 8 — Ant pakai -source 1.6)
				boolean semuaDigit = rest.length() > 0;
				for (int ci = 0; ci < rest.length(); ci++) {
					if (!Character.isDigit(rest.charAt(ci))) {
						semuaDigit = false;
						break;
					}
				}
				if (semuaDigit) {
					out.add(k.substring(0, pos));
				}
			}
		}
		return out;
	}

	// =====================================================================================
	// util
	// =====================================================================================

	private static void setProgress(Label label, int cur, int total, boolean simpan) {
		if (label == null || total <= 0) {
			return;
		}
		try {
			label.setValue((simpan ? "Menyimpan " : "Menghitung ") + cur + " dari " + total + " ("
					+ Common.numberFormat.get().format(cur * 100.0 / total) + " %)");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:374");
		}
	}

	private static List<Baris> urut(java.util.Collection<Baris> c) {
		List<Baris> l = new ArrayList<Baris>(c);
		java.util.Collections.sort(l, new java.util.Comparator<Baris>() {
			@Override
			public int compare(Baris a, Baris b) {
				int x = safe(a.kodeMk).compareToIgnoreCase(safe(b.kodeMk));
				if (x != 0) {
					return x;
				}
				x = safe(a.item).compareToIgnoreCase(safe(b.item));
				if (x != 0) {
					return x;
				}
				return safe(a.mahasiswa).compareToIgnoreCase(safe(b.mahasiswa));
			}
		});
		return l;
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static JSONObject amanJson(String s) {
		try {
			if (s != null && !s.trim().isEmpty()) {
				return new JSONObject(s);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:406");
		}
		return new JSONObject();
	}

	private static double amanNilai(HasilUjianMahasiswa hum) {
		try {
			Double n = hum.getNilai();
			return n == null ? 0.0 : n;
		} catch (Exception e) {
			return 0.0;
		}
	}

	private static String amanKode(HasilUjianMahasiswa hum) {
		try {
			return hum.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulumPunyaMatakuliah()
					.getMatakuliah().getKode();
		} catch (Exception e) {
			return "";
		}
	}

	private static String amanNama(HasilUjianMahasiswa hum) {
		try {
			return hum.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulumPunyaMatakuliah()
					.getMatakuliah().getNama();
		} catch (Exception e) {
			return "";
		}
	}

	private static String amanUjian(HasilUjianMahasiswa hum) {
		try {
			String n = hum.getPertemuanPunyaUjian().getNama();
			return n == null ? "Ujian" : n;
		} catch (Exception e) {
			return "Ujian";
		}
	}

	private static String amanMhs(HasilUjianMahasiswa hum) {
		try {
			if (hum.getMahasiswa() != null) {
				return safe(hum.getMahasiswa().getNim()) + " - " + safe(hum.getMahasiswa().getNama());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:452");
		}
		return "";
	}

	/** Resolusi Perkuliahan sesuai subtipe: TugasKelompok punya FK langsung; TugasPertemuan lewat pertemuan. */
	private static Perkuliahan perkuliahanDari(Tugas tugas) {
		try {
			if (tugas instanceof TugasKelompok) {
				return ((TugasKelompok) tugas).getPerkuliahan();
			}
			if (tugas instanceof TugasPertemuan) {
				Long pid = ((TugasPertemuan) tugas).getPertemuan();
				if (pid == null) {
					return null;
				}
				ais.database.model.Pertemuan p = (ais.database.model.Pertemuan) GeneralValueObject
						.ambilData(ais.database.model.Pertemuan.class, pid.toString(), false);
				return p == null ? null : p.getPerkuliahan();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:472");
		}
		return null;
	}

	private static String amanKodeMk(Tugas tugas) {
		try {
			return perkuliahanDari(tugas).getKurikulumPunyaMatakuliah().getMatakuliah().getKode();
		} catch (Exception e) {
			return "";
		}
	}

	private static String amanNamaMk(Tugas tugas) {
		try {
			return perkuliahanDari(tugas).getKurikulumPunyaMatakuliah().getMatakuliah().getNama();
		} catch (Exception e) {
			return "";
		}
	}

	private static String amanNamaTugas(Tugas tugas) {
		try {
			String n = tugas.getJudultugas();
			return n == null || n.trim().isEmpty() ? "Tugas" : n;
		} catch (Exception e) {
			return "Tugas";
		}
	}

	private static String labelMember(String memberKey) {
		return memberKey == null ? "" : memberKey.replace("_mhs", "").replace("_siswa", "");
	}

	// =====================================================================================
	// Dialog progres siap-pakai (REUSE) — hitung ulang + SIMPAN untuk satu/lebih perkuliahan
	// =====================================================================================

	/**
	 * Dialog progres siap-pakai untuk <b>hitung ulang &amp; SIMPAN</b> nilai OBE (Ujian
	 * {@link #simpanUjian} + Tugas {@link #simpanTugas}) atas {@code perkuliahanIds}, lalu memanggil
	 * {@code onSelesai} di event-thread ketika rampung (mis. untuk me-refresh grid pemanggil).
	 *
	 * <p>
	 * Dibuat agar layar mana pun (Dasbor OBE, "Singkronkan Nilai OBE", dll.) bisa MEMAKAI ULANG satu alur
	 * yang sama tanpa menyalin logika thread/timer:
	 * <ul>
	 *   <li>perhitungan berat berjalan di <b>thread latar daemon</b> (masing-masing {@code simpanUjian}/
	 *       {@code simpanTugas} sudah paralel &plusmn;50 thread di dalamnya) &mdash; UI tidak membeku;</li>
	 *   <li>sebuah <b>poll {@link org.zkoss.zul.Timer}</b> (event-thread) memperbarui indikator fase
	 *       ("Menghitung ulang nilai Ujian..." &rarr; "...Tugas...") lalu, saat selesai, menutup dialog dan
	 *       menjalankan {@code onSelesai};</li>
	 *   <li>tidak ada akses komponen ZK dari thread latar (progress label diberi {@code null} ke helper).</li>
	 * </ul>
	 *
	 * @param perkuliahanIds daftar id perkuliahan yang nilainya dihitung ulang (mis.
	 *                       {@code java.util.Collections.singletonList(perkuliahan.getId())}).
	 * @param judul          judul jendela dialog progres.
	 * @param onSelesai      listener yang dipanggil (boleh {@code null}) setelah selesai &amp; dialog tertutup.
	 */
	public static void hitungUlangSimpanDialog(final java.util.List<Long> perkuliahanIds, final String judul,
			final org.zkoss.zk.ui.event.EventListener onSelesai) {
		final ais.ui.util.MyWindow w = new ais.ui.util.MyWindow(judul == null ? "Hitung Ulang Nilai OBE" : judul,
				"normal", true);
		w.setWidth("460px");
		w.setPosition("center,center");
		w.setContentStyle("overflow:auto;");

		org.zkoss.zul.Vbox vb = new org.zkoss.zul.Vbox();
		vb.setStyle("padding:14px;");
		vb.setWidth("100%");
		vb.setParent(w);
		final org.zkoss.zul.Label info = new org.zkoss.zul.Label(
				ais.common.Common.getBahasaConfig("Menghitung ulang nilai Ujian & Tugas OBE... mohon tunggu."));
		info.setStyle("font-weight:600;color:#2563eb;");
		info.setParent(vb);
		vb.appendChild(new ais.ui.util.MyHtml(
				"<div style='margin:8px auto;width:30px;height:30px;border:3px solid #e2e8f0;border-top-color:#2563eb;"
						+ "border-radius:50%;animation:aisSpin .8s linear infinite;'></div>"));

		final boolean[] done = { false };
		final int[] fase = { 0 }; // 0=ujian, 1=tugas, 2=selesai

		Thread bg = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					simpanUjian(perkuliahanIds, null);
					fase[0] = 1;
					simpanTugas(perkuliahanIds, null);
					fase[0] = 2;
				} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:563");
					// non-fatal: poll tetap menutup dialog.
				} finally {
					done[0] = true;
				}
			}
		}, "obe-hitung-ulang-simpan-dialog");
		bg.setDaemon(true);
		bg.start();

		final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(400);
		timer.setRepeats(true);
		timer.setParent(w);
		timer.addEventListener("onTimer", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				info.setValue(fase[0] == 0 ? "Menghitung ulang nilai Ujian (HasilUjianMahasiswa)..."
						: fase[0] == 1 ? "Menghitung ulang nilai Tugas & Tugas Kelompok OBE..." : "Menyelesaikan...");
				if (!done[0]) {
					return;
				}
				timer.stop();
				timer.detach();
				w.detach();
				if (onSelesai != null) {
					try {
						onSelesai.onEvent(null);
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/obe/HitungUlangNilaiObeHelper.java:590");
					}
				}
			}
		});
		timer.start();

		org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(w);
		w.doHighlighted();
	}
}
