package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.PembombotanNilai;
import ais.database.model.Perkuliahan;

/**
 * <h2>Restore Nilai per Perkuliahan (dari revisi Envers)</h2>
 *
 * <p>
 * Mengembalikan bobot penilaian ({@link PembombotanNilai} milik perkuliahan &mdash; kolom
 * {@code pembombotan_nilai} &amp; {@code pembombotan_nilai_backup}) DAN seluruh {@link Detailperkuliahan}
 * (nilai per mahasiswa) ke keadaan pada <b>revisi terakhir (max) dari TANGGAL yang dipilih</b>. Kedua entity
 * ber-{@code @Audited} sehingga snapshot historis dibaca via Hibernate Envers dan ditulis balik dengan
 * {@code session.merge}.
 *
 * <p>
 * Alur UI ({@link #bukaDialog}): popup memuat daftar TANGGAL yang punya revisi &rarr; pengguna memilih satu
 * &rarr; tombol <b>Batal</b>/<b>Restore</b>. Saat Restore: entity dikembalikan lalu nilai OBE dihitung ulang
 * <b>paralel (maks 50 thread)</b> lewat {@link CommonAcademicKrsNilaiHelper#realoadNilaiLangsungParalel}
 * (varian ber-progress), dengan <b>progress bar</b> (sekian dari total, persen). Semua sesi Hibernate dibuka
 * per-operasi dan ditutup di {@code finally}.
 */
public final class RestoreNilaiPerkuliahanHelper {

	private RestoreNilaiPerkuliahanHelper() {
	}

	// =====================================================================================
	// 1) Daftar tanggal revisi (distinct per hari, terbaru dulu)
	// =====================================================================================

	/**
	 * Daftar tanggal (level HARI, distinct, terbaru dulu) yang memiliki revisi untuk PembombotanNilai
	 * perkuliahan ini dan Detailperkuliahan-nya.
	 */
	@SuppressWarnings("unchecked")
	public static List<Date> tanggalRevisi(Perkuliahan perkuliahan, Collection<Long> detailIds) {
		Set<Long> hariSet = new java.util.TreeSet<Long>(Collections.reverseOrder());
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			AuditReader reader = AuditReaderFactory.get(session);

			// Detailperkuliahan: filter berdasarkan relasi perkuliahan (semua mahasiswa sekaligus).
			try {
				AuditQuery q = reader.createQuery().forRevisionsOfEntity(Detailperkuliahan.class, false, true);
				if (perkuliahan != null && perkuliahan.getId() != null) {
					q.add(AuditEntity.relatedId("perkuliahan").eq(perkuliahan.getId()));
				}
				q.addOrder(AuditEntity.revisionNumber().desc());
				q.setMaxResults(20000);
				for (Object row : q.getResultList()) {
					kumpulkanHari(hariSet, row);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			// PembombotanNilai: filter per id (bobot + backup).
			for (Long pbId : pembobotanIds(perkuliahan)) {
				try {
					AuditQuery q = reader.createQuery().forRevisionsOfEntity(PembombotanNilai.class, false, true)
							.add(AuditEntity.id().eq(pbId)).addOrder(AuditEntity.revisionNumber().desc());
					q.setMaxResults(5000);
					for (Object row : q.getResultList()) {
						kumpulkanHari(hariSet, row);
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:88");
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mencari daftar tanggal revisi nilai perkuliahan yang dapat di-restore",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}

		List<Date> out = new ArrayList<Date>();
		for (Long ms : hariSet) {
			out.add(new Date(ms));
		}
		return out;
	}

	private static void kumpulkanHari(Set<Long> hariSet, Object row) {
		Date d = tanggalRevisiDari(row);
		if (d != null) {
			hariSet.add(awalHari(d).getTime());
		}
	}

	/** Ekstrak tanggal revisi dari baris {@code Object[]{entity, revEntity, revType}}. */
	private static Date tanggalRevisiDari(Object row) {
		try {
			if (row instanceof Object[]) {
				Object[] arr = (Object[]) row;
				for (Object o : arr) {
					if (o instanceof DefaultRevisionEntity) {
						return ((DefaultRevisionEntity) o).getRevisionDate();
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:122");
		}
		return null;
	}

	// =====================================================================================
	// 2) Restore ke revisi terakhir <= akhir tanggal terpilih
	// =====================================================================================

	/**
	 * Restore PembombotanNilai + seluruh Detailperkuliahan ke revisi terakhir yang timestamp-nya &le; akhir
	 * hari {@code tglDipilih}. {@code diproses} (bila non-null) di-increment tiap 1 entity berhasil di-merge
	 * agar pemanggil bisa menampilkan progress. Mengembalikan jumlah entity yang berhasil dikembalikan.
	 */
	public static int restore(Perkuliahan perkuliahan, Collection<Long> detailIds, Date tglDipilih,
			AtomicInteger diproses) {
		if (tglDipilih == null) {
			return 0;
		}
		final long batas = akhirHari(tglDipilih).getTime();
		int n = 0;
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			AuditReader reader = AuditReaderFactory.get(session);

			// PembombotanNilai (bobot + backup).
			for (Long pbId : pembobotanIds(perkuliahan)) {
				if (restoreRevisiTerakhir(session, reader, PembombotanNilai.class, pbId, batas)) {
					n++;
				}
				if (diproses != null) {
					diproses.incrementAndGet();
				}
			}

			// Detailperkuliahan (per mahasiswa).
			if (detailIds != null) {
				for (Long id : detailIds) {
					if (id == null) {
						continue;
					}
					if (restoreRevisiTerakhir(session, reader, Detailperkuliahan.class, id, batas)) {
						n++;
					}
					if (diproses != null) {
						diproses.incrementAndGet();
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"melakukan restore nilai perkuliahan ke revisi sebelumnya",
					e, new String[] {
							"Periksa kembali data yang sudah berhasil di-restore sebelum proses ini gagal.",
							"Muat ulang (refresh) halaman ini lalu coba proses restore kembali.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
		return n;
	}

	/**
	 * Cari snapshot revisi terakhir (&le; batas) dari entity, lalu kembalikan nilainya ke baris LIVE.
	 *
	 * <p><b>Penting (kebenaran restore):</b> TIDAK memakai {@code session.merge(snapshotUtuh)} — merge entity
	 * utuh akan mengikutkan relasi ({@code mahasiswa}/{@code perkuliahan}) dengan cascade {@code MERGE} yang
	 * bisa menimpa data lain. Sebagai gantinya: baris LIVE di-{@code get}, lalu HANYA field SKALAR
	 * (nilai/bobot: {@code detailNilai}, {@code detailNilaiKunci}, {@code totalNilai}, dst / {@code absen},
	 * {@code uts}, {@code uas}, {@code tugas1..5}, dll) disalin dari snapshot ke baris live, baru
	 * {@code update}. Relasi &amp; koleksi tidak disentuh. Bila baris sudah terhapus, snapshot di-{@code
	 * replicate} (OVERWRITE) untuk membuatnya kembali.
	 */
	@SuppressWarnings("unchecked")
	private static boolean restoreRevisiTerakhir(Session session, AuditReader reader, Class<?> clazz, Long id,
			long batasMillis) {
		Transaction tx = null;
		try {
			AuditQuery q = reader.createQuery().forRevisionsOfEntity(clazz, true, true)
					.add(AuditEntity.id().eq(id))
					.add(AuditEntity.revisionProperty("timestamp").le(Long.valueOf(batasMillis)))
					.addOrder(AuditEntity.revisionNumber().desc());
			q.setMaxResults(1);
			List<Object> hasil = q.getResultList();
			if (hasil == null || hasil.isEmpty() || hasil.get(0) == null) {
				return false; // tak ada revisi sebelum tanggal itu → biarkan apa adanya.
			}
			Object historis = hasil.get(0);

			tx = session.beginTransaction();
			Object live = session.get(clazz, id);
			if (live == null) {
				// Baris sudah tak ada di tabel live → buat ulang dari snapshot.
				session.replicate(historis, org.hibernate.ReplicationMode.OVERWRITE);
			} else {
				salinFieldSkalar(clazz, historis, live);
				session.update(live);
			}
			session.flush();
			tx.commit();
			tx = null;
			return true;
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:224");
				}
			}
			Common.tampilErrorJikaAdmin(e);
			return false;
		}
	}

	/**
	 * Salin semua field SKALAR (String / angka / Boolean / Date) yang dideklarasikan langsung di {@code clazz}
	 * dari {@code src} (snapshot historis) ke {@code dst} (baris live). Melewati {@code id} dan SEMUA relasi/
	 * koleksi (tipe non-skalar), sehingga nilai &amp; bobot dikembalikan tanpa menyentuh relasi.
	 */
	private static void salinFieldSkalar(Class<?> clazz, Object src, Object dst) {
		for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
			int mod = f.getModifiers();
			if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isFinal(mod)
					|| java.lang.reflect.Modifier.isTransient(mod)) {
				continue;
			}
			if ("id".equalsIgnoreCase(f.getName())) {
				continue;
			}
			Class<?> t = f.getType();
			boolean skalar = t == String.class || t == Boolean.class || t == java.util.Date.class
					|| Number.class.isAssignableFrom(t) || t.isPrimitive();
			if (!skalar) {
				continue; // lewati relasi/koleksi (mahasiswa, perkuliahan, mahasiswa lain, dsb).
			}
			try {
				f.setAccessible(true);
				f.set(dst, f.get(src));
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:256");
			}
		}
	}

	// =====================================================================================
	// util
	// =====================================================================================

	/** Id PembombotanNilai yang perlu di-restore: bobot aktif + backup (bila ada &amp; berbeda). */
	private static Set<Long> pembobotanIds(Perkuliahan perkuliahan) {
		Set<Long> ids = new LinkedHashSet<Long>();
		if (perkuliahan == null) {
			return ids;
		}
		try {
			PembombotanNilai pb = perkuliahan.getPembombotanNilai();
			if (pb != null && pb.getId() != null) {
				ids.add(pb.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:276");
		}
		try {
			PembombotanNilai bk = perkuliahan.getPembombotanNilaiBackup();
			if (bk != null && bk.getId() != null) {
				ids.add(bk.getId());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:283");
		}
		return ids;
	}

	private static Date awalHari(Date d) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(d);
		c.set(java.util.Calendar.HOUR_OF_DAY, 0);
		c.set(java.util.Calendar.MINUTE, 0);
		c.set(java.util.Calendar.SECOND, 0);
		c.set(java.util.Calendar.MILLISECOND, 0);
		return c.getTime();
	}

	private static Date akhirHari(Date d) {
		java.util.Calendar c = java.util.Calendar.getInstance();
		c.setTime(d);
		c.set(java.util.Calendar.HOUR_OF_DAY, 23);
		c.set(java.util.Calendar.MINUTE, 59);
		c.set(java.util.Calendar.SECOND, 59);
		c.set(java.util.Calendar.MILLISECOND, 999);
		return c.getTime();
	}

	// =====================================================================================
	// 3) Dialog: pilih tanggal → Batal / Restore (+ progress bar recompute paralel)
	// =====================================================================================

	/**
	 * Membuka popup pilih-tanggal-revisi dengan tombol Batal &amp; Restore. Saat Restore: kembalikan entity
	 * lalu hitung ulang nilai paralel (maks 50 thread) sambil menampilkan progress. {@code onSelesai}
	 * dipanggil setelah selesai (untuk me-reload grid pemanggil).
	 */
	public static void bukaDialog(final Perkuliahan perkuliahan, final Collection<Long> detailIds,
			final org.zkoss.zk.ui.event.EventListener onSelesai) {
		final ais.ui.util.MyWindow window = new ais.ui.util.MyWindow("Restore Nilai — Pilih Tanggal Revisi", "normal",
				true);
		window.setWidth("540px");
		window.setPosition("center,center");
		window.setContentStyle("overflow:auto;");

		org.zkoss.zul.Vbox vbox = new org.zkoss.zul.Vbox();
		vbox.setWidth("100%");
		vbox.setStyle("padding:10px;");
		vbox.setParent(window);

		org.zkoss.zul.Label info = new org.zkoss.zul.Label(
				"Pilih tanggal. Bobot penilaian & seluruh nilai mahasiswa akan dikembalikan ke revisi TERAKHIR "
						+ "pada tanggal tersebut. Nilai saat ini akan DITIMPA.");
		info.setMultiline(true);
		info.setStyle("color:#475569;");
		info.setParent(vbox);

		final java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy");
		final List<Date> tanggal = tanggalRevisi(perkuliahan, detailIds);

		final org.zkoss.zul.Listbox listbox = new org.zkoss.zul.Listbox();
		listbox.setMold("select");
		listbox.setRows(1);
		listbox.setWidth("100%");
		listbox.setParent(vbox);
		for (Date d : tanggal) {
			org.zkoss.zul.Listitem it = new org.zkoss.zul.Listitem(fmt.format(d));
			it.setValue(d);
			it.setParent(listbox);
		}
		if (!tanggal.isEmpty()) {
			listbox.setSelectedIndex(0);
		}

		final org.zkoss.zul.Label progress = new org.zkoss.zul.Label(
				tanggal.isEmpty() ? "Tidak ada revisi tersimpan untuk perkuliahan ini." : "");
		progress.setStyle("font-weight:600;color:#2563eb;margin-top:6px;");
		progress.setParent(vbox);

		org.zkoss.zul.Hbox hbox = new org.zkoss.zul.Hbox();
		hbox.setStyle("margin-top:10px;");
		hbox.setSpacing("8px");
		hbox.setParent(vbox);

		final org.zkoss.zul.Button btnBatal = new org.zkoss.zul.Button("Batal");
		btnBatal.setParent(hbox);
		btnBatal.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				window.detach();
			}
		});

		final org.zkoss.zul.Button btnRestore = new org.zkoss.zul.Button("Restore");
		btnRestore.setParent(hbox);
		btnRestore.setDisabled(tanggal.isEmpty());
		btnRestore.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			@Override
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				org.zkoss.zul.Listitem sel = listbox.getSelectedItem();
				final Date dipilih = sel == null ? null : (Date) sel.getValue();
				if (dipilih == null) {
					ais.ui.util.MyMessageboxConfig.show("Pilih tanggal terlebih dahulu.");
					return;
				}
				ais.ui.util.MyMessageboxConfig.show(
						"Kembalikan nilai & bobot ke revisi terakhir tanggal " + fmt.format(dipilih)
								+ "?\nNilai saat ini akan ditimpa.",
						"Konfirmasi", ais.ui.util.MyMessageboxConfig.OK | ais.ui.util.MyMessageboxConfig.CANCEL,
						ais.ui.util.MyMessageboxConfig.QUESTION, new org.zkoss.zk.ui.event.EventListener() {
							@Override
							public void onEvent(org.zkoss.zk.ui.event.Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != ais.ui.util.MyMessageboxConfig.OK) {
									return;
								}
								btnRestore.setDisabled(true);
								btnBatal.setDisabled(true);
								final int totalDetail = detailIds == null ? 0 : detailIds.size();
								final int totalPb = pembobotanIds(perkuliahan).size();
								final int total = totalPb + totalDetail + totalDetail; // restore + recompute
								final AtomicInteger diproses = new AtomicInteger(0);
								final boolean[] done = { false };

								Thread t = new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											restore(perkuliahan, detailIds, dipilih, diproses);
											ais.common.CommonAcademicKrsNilaiHelper.realoadNilaiLangsungParalel(
													perkuliahan, Boolean.FALSE, null, detailIds, 50, diproses);
										} catch (Throwable t2) { ais.common.ErrorAuditUtil.record(t2, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:410");
										} finally {
											done[0] = true;
										}
									}
								}, "restore-nilai-perkuliahan");
								t.setDaemon(true);
								t.start();

								final org.zkoss.zul.Timer timer = new org.zkoss.zul.Timer(400);
								timer.setRepeats(true);
								timer.setParent(window);
								timer.addEventListener("onTimer", new org.zkoss.zk.ui.event.EventListener() {
									@Override
									public void onEvent(org.zkoss.zk.ui.event.Event e3) throws Exception {
										int cur = Math.min(diproses.get(), total);
										int pct = total > 0 ? (int) Math.round(cur * 100.0 / total) : 100;
										progress.setValue("Memproses " + cur + " dari " + total + " (" + pct + "%)...");
										if (!done[0]) {
											return;
										}
										timer.stop();
										timer.detach();
										progress.setValue("Selesai — nilai berhasil dikembalikan & dihitung ulang.");
										ais.ui.util.MyMessageboxConfig.show("Restore selesai.", "Informasi",
												ais.ui.util.MyMessageboxConfig.OK,
												ais.ui.util.MyMessageboxConfig.INFORMATION);
										window.detach();
										if (onSelesai != null) {
											try {
												onSelesai.onEvent(null);
											} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RestoreNilaiPerkuliahanHelper.java:441");
											}
										}
									}
								});
								timer.start();
							}
						});
			}
		});

		org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.doHighlighted();
	}
}
