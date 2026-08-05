package ais.common;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class CicilanPembayaranRecoveryHelper {

	/**
	 * Tombol Recovery CicilanPembayaran: memulihkan cicilan terhapus DAN
	 * menyambung kembali link PPB yang hilang dari data audit.
	 */
	public static MyToolbarbuttonConfig createRecoveryButton(final Kegiatan kegiatan, final EventListener onSuccessListener) {

		MyToolbarbuttonConfig buttonRecovery = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
		buttonRecovery.setTooltiptext("Pulihkan data Cicilan Pembayaran yang hilang dari histori audit");
		buttonRecovery.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (kegiatan == null || kegiatan.getId() == null) {
					MyMessageboxConfig.show("Silakan pilih Kegiatan terlebih dahulu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				MyMessageboxConfig.show(
						"Apakah Anda yakin ingin melakukan Recovery data dari tabel audit? Tindakan ini akan memulihkan data cicilan pembayaran yang hilang pada kegiatan ini berdasarkan Tanggal, Item Biaya, dan Nilai terbaru.",
						"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int response = Integer.parseInt(event.getData().toString());
								if (response == MyMessageboxConfig.OK) {

									final List<String> warnings = java.util.Collections.synchronizedList(new ArrayList<String>());
									final Desktop desktop = Executions.getCurrent().getDesktop();

									if (!desktop.isServerPushEnabled()) {
										desktop.enableServerPush(true);
									}

									final Label label = Common.displayLoadBar(new EventListener() {
										@Override
										public void onEvent(Event arg0) throws Exception {
											Common.createDefaultTimer(new EventListener() {
												@Override
												public void onEvent(Event evTimer) throws Exception {
													if (!warnings.isEmpty()) {
														StringBuilder sb = new StringBuilder();
														synchronized (warnings) {
															for (String w : warnings) {
																sb.append(w).append("\n");
															}
														}
														MyMessageboxConfig.show(sb.toString(), "Peringatan/Info Recovery", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
													} else {
														MyMessageboxConfig.show("Proses Recovery Cicilan Pembayaran berhasil diselesaikan.", "Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
													}
													// Selalu refresh grid — bahkan jika ada warning/info
													if (onSuccessListener != null) {
														onSuccessListener.onEvent(evTimer);
													}
												}
											});
										}
									});

									new Thread(new Runnable() {
										@Override
										public void run() {
											try {
												restoreDeletedDataFromAudit(kegiatan, warnings, new ProgressListener() {
													@Override
													public void onProgress(final int percent, final String message) {
														try {
															Executions.schedule(desktop, new EventListener() {
																@Override
																public void onEvent(Event evSchedule) throws Exception {
																	if (label != null) {
																		label.setValue("Loading... " + percent + "% (" + message + ")");
																	}
																}
															}, null);
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:117");
														}
													}
												});

											} catch (Exception e) {
												warnings.add("Terjadi kesalahan fatal: " + e.getMessage());
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:124");
											} finally {
												try {
													Executions.schedule(desktop, new EventListener() {
														@Override
														public void onEvent(Event evSchedule) throws Exception {
															if (label != null) {
																label.setValue("");
																if (label.getParent() != null && label.getParent().getParent() instanceof org.zkoss.zul.Window) {
																	((org.zkoss.zul.Window) label.getParent().getParent()).detach();
																}
															}
														}
													}, null);
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:139");
												}
											}
										}
									}).start();
								}
							}
						});
			}
		});

		return buttonRecovery;
	}

	/**
	 * Tombol Recovery DetailBiaya.nilaiBiaya=0: memulihkan nilai dari audit.
	 * Untuk halaman Pengaturan Biaya (Screenshot 3).
	 */
	public static MyToolbarbuttonConfig createDetailBiayaRecoveryButton(
			final Jurusan jurusan, final Integer angkatan, final Integer semester,
			final JenisKegiatan jenisKegiatan, final EventListener onSuccessListener) {

		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
		btn.setTooltiptext("Pulihkan nilai DetailBiaya bernilai 0 dari histori audit");
		btn.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Recovery: mencari nilai terakhir (> 0) dari audit untuk semua DetailBiaya pada filter saat ini. Lanjutkan?",
						"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) return;

								final List<String> warnings = java.util.Collections.synchronizedList(new ArrayList<String>());
								final Desktop desktop = Executions.getCurrent().getDesktop();
								if (!desktop.isServerPushEnabled()) desktop.enableServerPush(true);

								final Label label = Common.displayLoadBar(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event evTimer) throws Exception {
												if (!warnings.isEmpty()) {
													StringBuilder sb = new StringBuilder();
													synchronized (warnings) { for (String w : warnings) sb.append(w).append("\n"); }
													MyMessageboxConfig.show(sb.toString(), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
													return;
												}
												MyMessageboxConfig.show("Recovery DetailBiaya selesai.", "Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
												if (onSuccessListener != null) onSuccessListener.onEvent(evTimer);
											}
										});
									}
								});

								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											recoveryDetailBiayaNilai(jurusan, angkatan, semester, jenisKegiatan, warnings, new ProgressListener() {
												@Override
												public void onProgress(final int pct, final String msg) {
													try {
														Executions.schedule(desktop, new EventListener() {
															@Override
															public void onEvent(Event e) throws Exception {
																if (label != null) label.setValue("Loading... " + pct + "% (" + msg + ")");
															}
														}, null);
													} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:211"); }
												}
											});
										} catch (Exception e) {
											warnings.add("Error: " + e.getMessage());
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:216");
										} finally {
											try {
												Executions.schedule(desktop, new EventListener() {
													@Override
													public void onEvent(Event e) throws Exception {
														if (label != null) {
															label.setValue("");
															if (label.getParent() != null && label.getParent().getParent() instanceof org.zkoss.zul.Window)
																((org.zkoss.zul.Window) label.getParent().getParent()).detach();
														}
													}
												}, null);
											} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:229"); }
										}
									}
								}).start();
							}
						});
			}
		});
		return btn;
	}

	/**
	 * Tombol Recovery PengaturanPembayaranBulanan.nominal=0: memulihkan dari audit.
	 * Untuk window Pengaturan Pembayaran Bulanan (Screenshot 4).
	 */
	public static MyToolbarbuttonConfig createPPBRecoveryButton(
			final Jurusan jurusan, final Integer angkatan, final EventListener onSuccessListener) {

		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig("Recovery", "/img/Configure.png");
		btn.setTooltiptext("Pulihkan nominal PPB bernilai 0 dari histori audit");
		btn.addEventListener(org.zkoss.zk.ui.event.Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				MyMessageboxConfig.show(
						"Recovery: mencari nominal terakhir (> 0) dari audit untuk semua Pengaturan Pembayaran Bulanan. Lanjutkan?",
						"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
						MyMessageboxConfig.QUESTION, new EventListener() {
							@Override
							public void onEvent(Event ev) throws Exception {
								if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) return;

								final List<String> warnings = java.util.Collections.synchronizedList(new ArrayList<String>());
								final Desktop desktop = Executions.getCurrent().getDesktop();
								if (!desktop.isServerPushEnabled()) desktop.enableServerPush(true);

								final Label label = Common.displayLoadBar(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event evTimer) throws Exception {
												if (!warnings.isEmpty()) {
													StringBuilder sb = new StringBuilder();
													synchronized (warnings) { for (String w : warnings) sb.append(w).append("\n"); }
													MyMessageboxConfig.show(sb.toString(), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
													return;
												}
												MyMessageboxConfig.show("Recovery Pengaturan Pembayaran Bulanan selesai.", "Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
												if (onSuccessListener != null) onSuccessListener.onEvent(evTimer);
											}
										});
									}
								});

								new Thread(new Runnable() {
									@Override
									public void run() {
										try {
											recoveryPPBNominal(jurusan, angkatan, warnings, new ProgressListener() {
												@Override
												public void onProgress(final int pct, final String msg) {
													try {
														Executions.schedule(desktop, new EventListener() {
															@Override
															public void onEvent(Event e) throws Exception {
																if (label != null) label.setValue("Loading... " + pct + "% (" + msg + ")");
															}
														}, null);
													} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:297"); }
												}
											});
										} catch (Exception e) {
											warnings.add("Error: " + e.getMessage());
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:302");
										} finally {
											try {
												Executions.schedule(desktop, new EventListener() {
													@Override
													public void onEvent(Event e) throws Exception {
														if (label != null) {
															label.setValue("");
															if (label.getParent() != null && label.getParent().getParent() instanceof org.zkoss.zul.Window)
																((org.zkoss.zul.Window) label.getParent().getParent()).detach();
														}
													}
												}, null);
											} catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:315"); }
										}
									}
								}).start();
							}
						});
			}
		});
		return btn;
	}

	/**
	 * Engine utama: pulihkan CicilanPembayaran terhapus + sambungkan kembali
	 * link PPB yang hilang pada cicilan yang masih ada.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void restoreDeletedDataFromAudit(Kegiatan kegiatan, List<String> warnings, ProgressListener progress) {
		Session session = null;
		Transaction tx = null;

		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			AuditReader reader = AuditReaderFactory.get(session);

			if (progress != null) progress.onProgress(5, "Mencari histori Cicilan Pembayaran...");

			// 1. Pulihkan CicilanPembayaran yang terhapus dari audit
			AuditQuery query = reader.createQuery().forRevisionsOfEntity(CicilanPembayaran.class, false, true);
			query.add(AuditEntity.property("kegiatan").eq(kegiatan));
			query.addOrder(AuditEntity.revisionNumber().desc());

			List results = query.getResultList();

			if (results == null || results.isEmpty()) {
				System.out.println("[Recovery] Tidak ada histori CicilanPembayaran untuk Kegiatan #" + (kegiatan != null ? kegiatan.getId() : "null"));
			} else {
				if (progress != null) progress.onProgress(15, "Menganalisa versi data terbaru...");

				Map<String, CicilanPembayaran> latestDataMap = new HashMap<String, CicilanPembayaran>();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

				for (Object obj : results) {
					Object[] tuple = (Object[]) obj;
					CicilanPembayaran cicilan = (CicilanPembayaran) tuple[0];

					String strTanggal = cicilan.getTanggal() != null ? sdf.format(cicilan.getTanggal()) : "null";
					String strItemBiaya = "null";
					try {
						if (cicilan.getItemBiaya() != null && cicilan.getItemBiaya().getId() != null) {
							strItemBiaya = cicilan.getItemBiaya().getId().toString();
						}
					} catch (Exception e) {
						if (warnings != null) warnings.add("getItemBiaya cicilan " + cicilan.getId() + " gagal: " + e.getMessage());
					}
					String strNilai = cicilan.getNilai() != null ? cicilan.getNilai().toString() : "null";

					String groupKey = strTanggal + "_" + strItemBiaya + "_" + strNilai;
					if (!latestDataMap.containsKey(groupKey)) {
						latestDataMap.put(groupKey, cicilan);
					}
				}

				Set<String> processedIds = new HashSet<String>();
				int total = latestDataMap.size();
				int current = 0;

				for (CicilanPembayaran cicilanToRestore : latestDataMap.values()) {
					current++;
					int percent = 15 + ((current * 30) / Math.max(total, 1));
					if (progress != null) progress.onProgress(percent, "Memulihkan Cicilan ID: " + cicilanToRestore.getId());

					Object checkDb = session.get(CicilanPembayaran.class, cicilanToRestore.getId());
					if (checkDb == null) {
						// Pastikan PPB sudah ada di DB sebelum CicilanPembayaran di-restore
						ensurePPBExists(session, reader, cicilanToRestore, processedIds, warnings);
						restoreDependenciesRecursively(session, reader, cicilanToRestore, processedIds, warnings);
						replicateWithSavepoint(session, cicilanToRestore,
							"CicilanPembayaran#" + cicilanToRestore.getId(), warnings);

						// Jika snapshot audit memiliki PPB=null (kondisi korup),
						// cari revisi sebelumnya dan sambungkan langsung — jangan tunggu Step 2
						CicilanPembayaran liveCP = (CicilanPembayaran) session.get(CicilanPembayaran.class, cicilanToRestore.getId());
						if (liveCP != null && liveCP.getPengaturanPembayaranBulanan() == null) {
							restorePPBForSingleCicilan(session, reader, liveCP, processedIds, warnings);
						}
					}
				}
			}

			// 2. flush + clear cache agar query Step 2 melihat semua record terbaru (termasuk
			//    yang baru di-restore di Step 1) langsung dari DB tanpa bias L1 cache
			session.flush();
			session.clear();

			// 3. Sambungkan kembali PPB yang hilang pada cicilan yang sudah ada (termasuk
			//    cicilan yang baru di-restore tapi PPB-nya belum terhubung)
			if (progress != null) progress.onProgress(50, "Memeriksa link PPB pada Cicilan yang ada...");
			restorePPBLinksForCicilan(session, reader, kegiatan, warnings, progress);

			// 3b. Pulihkan detail_biaya DAN pengaturan_pembayaran_bulanan yang KEDUANYA null
			//     dari tabel audit (revisi terbaru non-delete).
			if (progress != null) progress.onProgress(75, "Memulihkan DetailBiaya+PPB yang null dari audit...");
			restoreNullDetailBiayaAndPPBFromAudit(session, kegiatan, warnings);

			// 4. Perbaiki FK detail_biaya yang rusak (pointer ke DetailBiaya yang sudah
			//    terhapus dari DB). Ini menyebabkan ObjectNotFoundException saat getItemBiaya()
			//    dipanggil, sehingga kolom "Item Biaya" kosong di UI.
			if (progress != null) progress.onProgress(95, "Memperbaiki DetailBiaya yang hilang...");
			repairBrokenDetailBiayaFks(session, kegiatan, warnings);

			tx.commit();

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
			if (warnings != null) warnings.add("Gagal memulihkan data: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:433");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
					session.close();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:439");}
			}
		}
	}

	/**
	 * Pastikan PPB yang direferensikan oleh CicilanPembayaran sudah ada di DB.
	 * Dipanggil sebelum session.replicate agar FK tidak gagal.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void ensurePPBExists(Session session, AuditReader reader,
			CicilanPembayaran cicilan, Set<String> processedIds, List<String> warnings) {
		PengaturanPembayaranBulanan ppb = null;
		try { ppb = cicilan.getPengaturanPembayaranBulanan(); } catch (Exception e) { return; }
		if (ppb == null || ppb.getId() == null) return;

		Long ppbId = ppb.getId();
		if (session.get(PengaturanPembayaranBulanan.class, ppbId) != null) return;

		// PPB tidak ada di DB — restore via sesi terpisah agar main session tidak korup
		restorePPBInDedicatedSession(ppbId, warnings);
	}

	/**
	 * Temukan CicilanPembayaran dengan PPB=null, cari PPB dari audit, dan
	 * sambungkan kembali. Jika PPB tidak ada di DB → restore terlebih dahulu.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void restorePPBLinksForCicilan(Session session, AuditReader reader,
			Kegiatan kegiatan, List<String> warnings, ProgressListener progress) {

		// Guard: hanya proses kegiatan yang memang mewajibkan angsuran
		// (jenisKegiatan.hanyaBerupaAngsuran == true) ATAU punya entri PPB di DB
		// (countBulanan > 0 — setara: ada PengaturanPembayaranBulanan untuk jenisKegiatan ini).
		boolean wajibAngsuran = false;
		try {
			JenisKegiatan jk = kegiatan.getJenisKegiatan();
			if (jk != null) {
				wajibAngsuran = Boolean.TRUE.equals(jk.getHanyaBerupaAngsuran());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:479");}

		if (!wajibAngsuran) {
			// Periksa apakah ada PengaturanPembayaranBulanan untuk jenisKegiatan ini
			// (setara dengan countBulanan > 0 dari PembayaranUtilHelper)
			long countPpb = 0;
			try {
				Long jkId = kegiatan.getJenisKegiatan() != null ? kegiatan.getJenisKegiatan().getId() : null;
				if (jkId != null) {
					Object cnt = session.createQuery(
							"select count(*) from PengaturanPembayaranBulanan ppb"
							+ " where ppb.detailBiaya.jenisKegiatan.id = :jkId")
							.setLong("jkId", jkId)
							.uniqueResult();
					if (cnt != null) countPpb = ((Number) cnt).longValue();
				}
			} catch (Exception e) {
				// Jika query gagal, tetap lanjut — lebih aman over-recover daripada skip
				countPpb = -1;
			}
			if (countPpb == 0) {
				System.out.println("[Recovery] Kegiatan #" + kegiatan.getId()
						+ " bukan tipe angsuran dan tidak ada PPB — step restorePPBLinks dilewati.");
				return;
			}
		}

		List<CicilanPembayaran> cicilanTanpaPpb = session.createCriteria(CicilanPembayaran.class)
				.add(Restrictions.eq("kegiatan", kegiatan))
				.add(Restrictions.isNull("pengaturanPembayaranBulanan"))
				.list();

		if (cicilanTanpaPpb == null || cicilanTanpaPpb.isEmpty()) {
			System.out.println("[Recovery] Tidak ada CicilanPembayaran dengan PPB=null untuk Kegiatan #" + kegiatan.getId() + " — link PPB sudah lengkap.");
			return;
		}

		Set<String> processedIds = new HashSet<String>();
		int total = cicilanTanpaPpb.size();
		int current = 0;

		for (CicilanPembayaran cicilan : cicilanTanpaPpb) {
			current++;
			int pct = 50 + (current * 45 / Math.max(total, 1));
			if (progress != null) progress.onProgress(pct, "Pulihkan link PPB cicilan ID: " + cicilan.getId());
			restorePPBForSingleCicilan(session, reader, cicilan, processedIds, warnings);
		}
	}

	/**
	 * Perbaiki baris CicilanPembayaran dan PengaturanPembayaranBulanan yang masih
	 * mereferensikan DetailBiaya yang sudah terhapus dari DB. Tanpa langkah ini,
	 * getItemBiaya() meledak dengan ObjectNotFoundException setiap kali renderer
	 * atau Recovery menyentuh cicilan tersebut, sehingga kolom "Item Biaya" kosong.
	 *
	 * Strategi:
	 *   1. Temukan semua DetailBiaya ID yang rusak (FK ada tapi row tidak ada).
	 *   2. INSERT ulang dari audit (sertakan kolom item_biaya agar rantai
	 *      PPB → DetailBiaya → ItemBiaya kembali bekerja).
	 *   3. Jika audit tidak ada, buat stub minimal dan null-kan FK di
	 *      cicilan_pembayaran (nullable) agar proxy tidak meledak.
	 *   4. UPDATE cicilan_pembayaran.item_biaya dari PPB jika masih null.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void repairBrokenDetailBiayaFks(Session session, Kegiatan kegiatan, List<String> warnings) {
		long kegiatanId = kegiatan.getId();

		Set<Long> missingIds = new HashSet<Long>();
		try {
			List rows = session.createSQLQuery(
				"SELECT DISTINCT cp.detail_biaya FROM cicilan_pembayaran cp"
				+ " LEFT JOIN detail_biaya db ON db.id = cp.detail_biaya"
				+ " WHERE cp.kegiatan = :kgId AND cp.detail_biaya IS NOT NULL AND db.id IS NULL")
				.setLong("kgId", kegiatanId).list();
			if (rows != null) for (Object id : rows) if (id != null) missingIds.add(((Number) id).longValue());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:555");
			if (warnings != null) warnings.add("Scan cicilan detail_biaya rusak gagal: " + e.getMessage());
		}
		try {
			List rows = session.createSQLQuery(
				"SELECT DISTINCT ppb.detail_biaya FROM pengaturan_pembayaran_bulanan ppb"
				+ " JOIN cicilan_pembayaran cp ON cp.pengaturan_pembayaran_bulanan = ppb.id"
				+ " LEFT JOIN detail_biaya db ON db.id = ppb.detail_biaya"
				+ " WHERE cp.kegiatan = :kgId AND ppb.detail_biaya IS NOT NULL AND db.id IS NULL")
				.setLong("kgId", kegiatanId).list();
			if (rows != null) for (Object id : rows) if (id != null) missingIds.add(((Number) id).longValue());
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:567");
			if (warnings != null) warnings.add("Scan PPB detail_biaya rusak gagal: " + e.getMessage());
		}

		if (missingIds.isEmpty()) return;
		if (warnings != null) warnings.add("Memperbaiki " + missingIds.size() + " DetailBiaya hilang: " + missingIds);

		java.sql.Connection conn = null;
		try { conn = session.connection(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:575");}

		for (Long missingId : missingIds) {
			int inserted = 0;
			java.sql.Savepoint sp = null;
			if (conn != null) try { sp = conn.setSavepoint("sp_rdb_" + missingId); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:580");}
			try {
				// INSERT dari audit — sertakan item_biaya agar rantai DetailBiaya→ItemBiaya berfungsi;
				// gunakan CASE WHEN untuk mencegah FK violation jika ItemBiaya juga terhapus.
				inserted = session.createSQLQuery(
					"INSERT INTO public.detail_biaya (id, nama, nilai_biaya, item_biaya)"
					+ " SELECT id, COALESCE(nama, 'Dipulihkan'), COALESCE(nilai_biaya, 0),"
					+ "   CASE WHEN item_biaya IS NOT NULL"
					+ "        AND EXISTS (SELECT 1 FROM public.item_biaya ib WHERE ib.id = item_biaya)"
					+ "        THEN item_biaya ELSE NULL END"
					+ " FROM new_audit.detail_biaya__audit"
					+ " WHERE id = :dbId AND revtype < 2"
					+ " ORDER BY rev DESC LIMIT 1"
					+ " ON CONFLICT (id) DO NOTHING")
					.setLong("dbId", missingId).executeUpdate();
				if (sp != null) { try { conn.releaseSavepoint(sp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:595");} sp = null; }
				if (inserted > 0 && warnings != null)
					warnings.add("DetailBiaya#" + missingId + " dipulihkan dari audit");
			} catch (Exception ex1) {
				ex1.printStackTrace(); ais.common.ErrorAuditUtil.record(ex1, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:599");
				if (sp != null && conn != null) {
					try { conn.rollback(sp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:601");}
					try { conn.releaseSavepoint(sp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:602");}
					sp = null;
				}
				if (warnings != null) warnings.add("Restore DetailBiaya#" + missingId + " dari audit gagal: " + ex1.getMessage());
			}

			if (inserted == 0) {
				java.sql.Savepoint sp2 = null;
				if (conn != null) try { sp2 = conn.setSavepoint("sp_stub_" + missingId); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:610");}
				try {
					inserted = session.createSQLQuery(
						"INSERT INTO public.detail_biaya (id, nama, nilai_biaya)"
						+ " VALUES (:dbId, 'Stub dipulihkan', 0)"
						+ " ON CONFLICT (id) DO NOTHING")
						.setLong("dbId", missingId).executeUpdate();
					if (sp2 != null) { try { conn.releaseSavepoint(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:617");} sp2 = null; }
					if (inserted > 0 && warnings != null)
						warnings.add("Stub DetailBiaya#" + missingId + " dibuat");
				} catch (Exception ex2) {
					ex2.printStackTrace(); ais.common.ErrorAuditUtil.record(ex2, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:621");
					if (sp2 != null && conn != null) {
						try { conn.rollback(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:623");}
						try { conn.releaseSavepoint(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:624");}
					}
					if (warnings != null) warnings.add("Stub DetailBiaya#" + missingId + " gagal: " + ex2.getMessage());
					// Fallback: null-kan FK di cicilan_pembayaran (nullable) agar proxy tidak meledak
					try {
						session.createSQLQuery(
							"UPDATE cicilan_pembayaran SET detail_biaya = NULL"
							+ " WHERE kegiatan = :kgId AND detail_biaya = :dbId")
							.setLong("kgId", kegiatanId).setLong("dbId", missingId).executeUpdate();
						if (warnings != null) warnings.add("detail_biaya #" + missingId + " di-null-kan di cicilan_pembayaran");
					} catch (Exception ex3) {
						ex3.printStackTrace(); ais.common.ErrorAuditUtil.record(ex3, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:635");
						if (warnings != null) warnings.add("Null detail_biaya #" + missingId + " gagal: " + ex3.getMessage());
					}
				}
			}
		}

		// UPDATE item_biaya langsung dari audit (cicilan.detail_biaya → audit)
		// — dibutuhkan ketika DetailBiaya dipulihkan sebagai stub (stub tidak punya item_biaya)
		try {
			int updatedAudit = session.createSQLQuery(
				"UPDATE cicilan_pembayaran cp"
				+ " SET item_biaya = ("
				+ "   SELECT dba.item_biaya FROM new_audit.detail_biaya__audit dba"
				+ "   WHERE dba.id = cp.detail_biaya AND dba.revtype < 2"
				+ "     AND dba.item_biaya IS NOT NULL"
				+ "     AND EXISTS (SELECT 1 FROM item_biaya ib WHERE ib.id = dba.item_biaya)"
				+ "   ORDER BY dba.rev DESC LIMIT 1"
				+ " )"
				+ " WHERE cp.kegiatan = :kgId"
				+ " AND cp.item_biaya IS NULL"
				+ " AND cp.detail_biaya IS NOT NULL")
				.setLong("kgId", kegiatanId).executeUpdate();
			if (updatedAudit > 0 && warnings != null)
				warnings.add("item_biaya dari audit detail_biaya: " + updatedAudit + " cicilan");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:661");
			if (warnings != null) warnings.add("Set item_biaya dari audit detail_biaya gagal: " + e.getMessage());
		}

		// UPDATE item_biaya dari PPB → audit detail_biaya
		// — cicilan dengan detail_biaya=null tapi punya PPB yang detail_biaya-nya ada di audit
		try {
			int updatedPpbAudit = session.createSQLQuery(
				"UPDATE cicilan_pembayaran cp"
				+ " SET item_biaya = ("
				+ "   SELECT dba.item_biaya FROM pengaturan_pembayaran_bulanan ppb"
				+ "   JOIN new_audit.detail_biaya__audit dba ON dba.id = ppb.detail_biaya"
				+ "   WHERE ppb.id = cp.pengaturan_pembayaran_bulanan AND dba.revtype < 2"
				+ "     AND dba.item_biaya IS NOT NULL"
				+ "     AND EXISTS (SELECT 1 FROM item_biaya ib WHERE ib.id = dba.item_biaya)"
				+ "   ORDER BY dba.rev DESC LIMIT 1"
				+ " )"
				+ " WHERE cp.kegiatan = :kgId"
				+ " AND cp.item_biaya IS NULL"
				+ " AND cp.pengaturan_pembayaran_bulanan IS NOT NULL")
				.setLong("kgId", kegiatanId).executeUpdate();
			if (updatedPpbAudit > 0 && warnings != null)
				warnings.add("item_biaya dari PPB-audit: " + updatedPpbAudit + " cicilan");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:685");
			if (warnings != null) warnings.add("Set item_biaya dari PPB-audit gagal: " + e.getMessage());
		}

		// UPDATE item_biaya dari PPB → live DetailBiaya (yang baru dipulihkan dari audit dengan item_biaya)
		try {
			int updated = session.createSQLQuery(
				"UPDATE cicilan_pembayaran cp"
				+ " SET item_biaya = ("
				+ "   SELECT db.item_biaya FROM pengaturan_pembayaran_bulanan ppb"
				+ "   JOIN detail_biaya db ON db.id = ppb.detail_biaya"
				+ "   WHERE ppb.id = cp.pengaturan_pembayaran_bulanan AND db.item_biaya IS NOT NULL"
				+ "   LIMIT 1"
				+ " )"
				+ " WHERE cp.kegiatan = :kgId"
				+ " AND cp.pengaturan_pembayaran_bulanan IS NOT NULL"
				+ " AND cp.item_biaya IS NULL")
				.setLong("kgId", kegiatanId).executeUpdate();
			if (updated > 0 && warnings != null)
				warnings.add("item_biaya dari PPB-live: " + updated + " cicilan");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:706");
			if (warnings != null) warnings.add("Set item_biaya dari PPB-live gagal: " + e.getMessage());
		}

		// Null-kan FK detail_biaya yang masih rusak di PPB agar proxy Hibernate tidak meledak
		try {
			int nulledPpb = session.createSQLQuery(
				"UPDATE pengaturan_pembayaran_bulanan ppb SET detail_biaya = NULL"
				+ " WHERE EXISTS ("
				+ "   SELECT 1 FROM cicilan_pembayaran cp"
				+ "   WHERE cp.pengaturan_pembayaran_bulanan = ppb.id AND cp.kegiatan = :kgId"
				+ " )"
				+ " AND ppb.detail_biaya IS NOT NULL"
				+ " AND NOT EXISTS (SELECT 1 FROM detail_biaya db WHERE db.id = ppb.detail_biaya)")
				.setLong("kgId", kegiatanId).executeUpdate();
			if (nulledPpb > 0 && warnings != null)
				warnings.add("Nullkan detail_biaya rusak di PPB: " + nulledPpb + " baris");
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:724");
			if (warnings != null) warnings.add("Nullkan detail_biaya PPB gagal: " + e.getMessage());
		}
	}

	/**
	 * Untuk satu CicilanPembayaran: cari PPB dari audit (versi dengan PPB!=null),
	 * pastikan PPB sudah di-insert ke DB, lalu sambungkan ke CicilanPembayaran.
	 * Dipanggil baik inline di Step 1 (cicilan baru di-restore) maupun di Step 2
	 * (cicilan lama yang PPB-nya hilang).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void restorePPBForSingleCicilan(Session session, AuditReader reader,
			CicilanPembayaran cicilan, Set<String> processedIds, List<String> warnings) {

		// Ambil semua revisi lalu filter di Java — hindari AuditEntity.property()
		// pada FK column karena Envers menggunakan nama Java property bukan nama kolom DB
		// (@JoinColumn name="pengaturan_pembayaran_bulanan" ≠ "pengaturanPembayaranBulanan_id")
		List allRevisions;
		try {
			allRevisions = reader.createQuery()
					.forRevisionsOfEntity(CicilanPembayaran.class, false, true)
					.add(AuditEntity.id().eq(cicilan.getId()))
					.addOrder(AuditEntity.revisionNumber().desc())
					.getResultList();
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:750");
			return;
		}

		if (allRevisions == null || allRevisions.isEmpty()) return;

		// Cari revisi terbaru dimana PPB tidak null
		CicilanPembayaran auditCicilan = null;
		for (Object obj : allRevisions) {
			Object[] t = (Object[]) obj;
			CicilanPembayaran cp = (CicilanPembayaran) t[0];
			try {
				if (cp.getPengaturanPembayaranBulanan() != null) {
					auditCicilan = cp;
					break;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:767");
				if (warnings != null) warnings.add("Akses PPB revisi cicilan " + cicilan.getId() + " gagal: " + e.getMessage());
			}
		}
		if (auditCicilan == null) {
			if (warnings != null) warnings.add("Cicilan " + cicilan.getId() + ": tidak ada revisi dengan PPB non-null di audit");
			return;
		}

		Long ppbId = null;
		try {
			PengaturanPembayaranBulanan ppb = auditCicilan.getPengaturanPembayaranBulanan();
			ppbId = (ppb != null) ? ppb.getId() : null;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:781");
			if (warnings != null) warnings.add("Ambil id PPB cicilan " + cicilan.getId() + " gagal: " + ex.getMessage());
		}

		if (ppbId == null) {
			if (warnings != null) warnings.add("Cicilan " + cicilan.getId() + ": ppbId null setelah ambil dari revisi audit — dilewati.");
			return;
		}

		// Pastikan PPB sudah ada di DB — restore via sesi terpisah jika belum ada
		PengaturanPembayaranBulanan livePpb = (PengaturanPembayaranBulanan) session.get(PengaturanPembayaranBulanan.class, ppbId);
		if (livePpb == null) {
			if (warnings != null) warnings.add("PPB #" + ppbId + " tidak ada di DB, mencoba restore dari audit (native SQL, sesi sama)...");
			// Gunakan native SQL INSERT SELECT dalam SESI YANG SAMA agar tidak ada masalah
			// isolasi transaksi PostgreSQL (sesi terpisah membutuhkan READ COMMITTED fresh snapshot
			// tapi Hibernate L1 cache + transaction boundary mencegah main session melihatnya).
			restorePPBViaNativeSqlInSession(session, ppbId, warnings);
			livePpb = (PengaturanPembayaranBulanan) session.get(PengaturanPembayaranBulanan.class, ppbId);
		}

		if (livePpb == null) {
			if (warnings != null) warnings.add("Cicilan " + cicilan.getId() + ": PPB #" + ppbId + " tetap tidak ditemukan setelah restore — dilewati.");
			return;
		}

		// Update CicilanPembayaran dengan PPB yang baru saja di-restore / ditemukan.
		// Gunakan saveOrUpdate (bukan update) agar aman baik untuk entity attached maupun detached.
		java.sql.Connection connUp = null;
		java.sql.Savepoint spUp = null;
		try {
			try { connUp = session.connection(); spUp = connUp.setSavepoint(); } catch (Exception spEx) { ais.common.ErrorAuditUtil.record(spEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:811");}
			cicilan.setPengaturanPembayaranBulanan(livePpb);
			DetailBiaya ppbDetailBiaya = null;
			try { ppbDetailBiaya = livePpb.getDetailBiaya(); } catch (Exception _e) {
				_e.printStackTrace(); ais.common.ErrorAuditUtil.record(_e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:815");
				if (warnings != null) warnings.add("livePpb.getDetailBiaya() gagal: " + _e.getMessage());
			}
			if (cicilan.getDetailBiaya() == null && ppbDetailBiaya != null) {
				cicilan.setDetailBiaya(ppbDetailBiaya);
			}
			// Pastikan itemBiaya ikut tersambung agar kolom item_biaya tidak null pasca-recovery
			if (ppbDetailBiaya != null) {
				try {
					ais.database.model.ItemBiaya ib = ppbDetailBiaya.getItemBiaya();
					if (ib != null) cicilan.setItemBiaya(ib);
					else if (warnings != null) warnings.add("ppbDetailBiaya.getItemBiaya() null untuk cicilan " + cicilan.getId());
				} catch (Exception _e) {
					_e.printStackTrace(); ais.common.ErrorAuditUtil.record(_e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:828");
					if (warnings != null) warnings.add("ppbDetailBiaya.getItemBiaya() gagal: " + _e.getMessage());
				}
			}
			session.saveOrUpdate(cicilan);
			session.flush();
			if (spUp != null) try { connUp.releaseSavepoint(spUp); } catch (Exception rEx) { ais.common.ErrorAuditUtil.record(rEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:834");}
			if (warnings != null) warnings.add("OK: PPB #" + ppbId + " berhasil disambungkan ke cicilan #" + cicilan.getId());
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:837");
			if (spUp != null && connUp != null) {
				try { connUp.rollback(spUp); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:839");}
			}
			try { session.clear(); } catch (Exception ex2) { ais.common.ErrorAuditUtil.record(ex2, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:841");}
			if (warnings != null) warnings.add("Gagal sambungkan PPB " + ppbId + " ke cicilan " + cicilan.getId() + ": " + ex.getMessage());
		}
	}

	/**
	 * Pulihkan DetailBiaya.nilaiBiaya=0 dari histori audit (nilai terakhir > 0).
	 * Untuk halaman Pengaturan Biaya (Screenshot 3).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void recoveryDetailBiayaNilai(Jurusan jurusan, Integer angkatan,
			Integer semester, JenisKegiatan jenisKegiatan,
			List<String> warnings, ProgressListener progress) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			AuditReader reader = AuditReaderFactory.get(session);

			Criteria crit = session.createCriteria(DetailBiaya.class);
			if (jurusan != null)
				crit.add(Restrictions.eq("jurusan", jurusan));
			if (angkatan != null)
				crit.add(Restrictions.eq("angkatan", angkatan));
			if (semester != null)
				crit.add(Restrictions.eq("semester", semester));
			if (jenisKegiatan != null)
				crit.add(Restrictions.eq("jenisKegiatan", jenisKegiatan));
			crit.add(Restrictions.or(
					Restrictions.isNull("nilaiBiaya"),
					Restrictions.le("nilaiBiaya", 0.001)));

			List<DetailBiaya> dbs = crit.list();
			if (progress != null) progress.onProgress(10, "Ditemukan " + dbs.size() + " DetailBiaya bernilai nol...");

			int total = dbs.size();
			int current = 0;

			for (DetailBiaya db : dbs) {
				current++;
				int pct = 10 + (current * 80 / Math.max(total, 1));
				if (progress != null) progress.onProgress(pct, "Recovery DetailBiaya ID: " + db.getId());

				List auditResult;
				try {
					auditResult = reader.createQuery()
							.forRevisionsOfEntity(DetailBiaya.class, false, true)
							.add(AuditEntity.id().eq(db.getId()))
							.add(AuditEntity.property("nilaiBiaya").gt(0.001))
							.addOrder(AuditEntity.revisionNumber().desc())
							.setMaxResults(1)
							.getResultList();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:895");
					continue;
				}

				if (auditResult == null || auditResult.isEmpty()) continue;

				Object[] tuple = (Object[]) auditResult.get(0);
				DetailBiaya auditDb = (DetailBiaya) tuple[0];
				if (auditDb.getNilaiBiaya() == null || auditDb.getNilaiBiaya() < 0.001) continue;

				db.setNilaiBiaya(auditDb.getNilaiBiaya());
				session.update(db);
			}

			tx.commit();
			if (progress != null) progress.onProgress(100, "Selesai.");

		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			if (warnings != null) warnings.add("Gagal recoveryDetailBiayaNilai: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:915");
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:918");}
			}
		}
	}

	/**
	 * Pulihkan PengaturanPembayaranBulanan.nominal=0 dari histori audit.
	 * Untuk window Pengaturan Pembayaran Bulanan (Screenshot 4).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void recoveryPPBNominal(Jurusan jurusan, Integer angkatan,
			List<String> warnings, ProgressListener progress) {
		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			AuditReader reader = AuditReaderFactory.get(session);

			Criteria crit = session.createCriteria(PengaturanPembayaranBulanan.class);
			crit.createAlias("detailBiaya", "detailBiaya");
			if (jurusan != null)
				crit.add(Restrictions.eq("detailBiaya.jurusan", jurusan));
			if (angkatan != null)
				crit.add(Restrictions.eq("detailBiaya.angkatan", angkatan));
			crit.add(Restrictions.or(
					Restrictions.isNull("nominal"),
					Restrictions.le("nominal", 0.001)));

			List<PengaturanPembayaranBulanan> ppbs = crit.list();
			if (progress != null) progress.onProgress(10, "Ditemukan " + ppbs.size() + " PPB bernilai nol...");

			int total = ppbs.size();
			int current = 0;

			for (PengaturanPembayaranBulanan ppb : ppbs) {
				current++;
				int pct = 10 + (current * 80 / Math.max(total, 1));
				if (progress != null) progress.onProgress(pct, "Recovery PPB ID: " + ppb.getId());

				List auditResult;
				try {
					auditResult = reader.createQuery()
							.forRevisionsOfEntity(PengaturanPembayaranBulanan.class, false, true)
							.add(AuditEntity.id().eq(ppb.getId()))
							.add(AuditEntity.property("nominal").gt(0.001))
							.addOrder(AuditEntity.revisionNumber().desc())
							.setMaxResults(1)
							.getResultList();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:968");
					continue;
				}

				if (auditResult == null || auditResult.isEmpty()) continue;

				Object[] tuple = (Object[]) auditResult.get(0);
				PengaturanPembayaranBulanan auditPpb = (PengaturanPembayaranBulanan) tuple[0];
				if (auditPpb.getNominal() == null || auditPpb.getNominal() < 0.001) continue;

				ppb.setNominal(auditPpb.getNominal());
				session.update(ppb);
			}

			tx.commit();
			if (progress != null) progress.onProgress(100, "Selesai.");

		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			if (warnings != null) warnings.add("Gagal recoveryPPBNominal: " + e.getMessage());
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:988");
		} finally {
			if (session != null && session.isOpen()) {
				try { session.disconnect(); session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:991");}
			}
		}
	}

	/**
	 * INSERT PengaturanPembayaranBulanan langsung dari tabel audit ke tabel utama
	 * DALAM SESI YANG SAMA (dilindungi JDBC Savepoint). Menghindari masalah isolasi
	 * transaksi PostgreSQL yang terjadi ketika sesi terpisah commit lalu main session
	 * tidak bisa melihatnya karena snapshot REPEATABLE READ atau L1 cache.
	 */
	@SuppressWarnings("deprecation")
	private static void restorePPBViaNativeSqlInSession(Session session, Long ppbId, List<String> warnings) {
		java.sql.Connection conn = null;
		java.sql.Savepoint sp = null;
		try {
			conn = session.connection();
			sp = conn.setSavepoint("sp_ppb_native_" + ppbId);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1009");}

		try {
			// Pastikan detail_biaya yang diperlukan ada dulu — jika tidak, INSERT akan gagal FK
			// dan savepoint menangkapnya tanpa merusak transaksi utama.
			int affected = session.createSQLQuery(
					"INSERT INTO public.pengaturan_pembayaran_bulanan"
					+ " (id, aktif, bulan, deadline, denda, dikalikandengankondisikhusus,"
					+ " keterangan, nama, namabulan, nominal, oleh, olehid, persentase,"
					+ " realbulan, realbulantahun, tanggal_dirubah, tetapditampilkanwalaupunnol,"
					+ " detail_biaya, filelocation, tanggaltagihanselaludibuatawalbulan)"
					+ " SELECT id, aktif, bulan, deadline, denda, dikalikandengankondisikhusus,"
					+ " keterangan, nama, namabulan, nominal, oleh, olehid, persentase,"
					+ " realbulan, realbulantahun, tanggal_dirubah, tetapditampilkanwalaupunnol,"
					+ " detail_biaya, filelocation, tanggaltagihanselaludibuatawalbulan"
					+ " FROM new_audit.pengaturan_pembayaran_bulanan__audit"
					+ " WHERE id = :id AND revtype < 2"
					+ " ORDER BY rev DESC LIMIT 1"
					+ " ON CONFLICT (id) DO NOTHING")
					.setLong("id", ppbId)
					.executeUpdate();

			if (sp != null) try { conn.releaseSavepoint(sp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1031");}

			if (affected > 0) {
				if (warnings != null) warnings.add("PPB #" + ppbId + " berhasil di-INSERT dari audit (native SQL dalam sesi sama).");
			} else {
				// ON CONFLICT DO NOTHING → sudah ada atau tidak ada di audit
				if (warnings != null) warnings.add("PPB #" + ppbId + ": INSERT 0 baris (sudah ada atau tidak ditemukan di audit).");
			}
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1040");
			if (sp != null && conn != null) {
				try { conn.rollback(sp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1042");}
			}
			if (warnings != null) warnings.add("Gagal INSERT native PPB #" + ppbId + ": " + ex.getMessage()
					+ ". Fallback ke sesi terpisah...");
			// Fallback: coba sesi terpisah (mungkin berhasil jika masalahnya bukan isolasi)
			restorePPBInDedicatedSession(ppbId, warnings);
		}
	}

	/**
	 * Restore PengaturanPembayaranBulanan beserta dependensinya (mis. DetailBiaya)
	 * dalam sesi Hibernate TERPISAH dengan transaksi sendiri. Jika FK violation
	 * terjadi (mis. detail_biaya hilang dari DB), hanya sesi ini yang di-rollback —
	 * sesi induk TIDAK terpengaruh sama sekali.
	 * Return true = berhasil & ter-commit; false = gagal (warning dicatat).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean restorePPBInDedicatedSession(Long ppbId, List<String> warnings) {
		Session subSession = null;
		Transaction subTx = null;
		try {
			subSession = HibernateUtil.openSession();
			// MANUAL flush mencegah autoFlush saat proxy Envers diinisialisasi —
			// Envers mendaftarkan entity audit ke session tanpa id, autoFlush menyebabkan
			// AssertionFailure "null id". Dengan MANUAL kita kontrol sendiri kapan flush.
			subSession.setFlushMode(org.hibernate.FlushMode.MANUAL);
			subTx = subSession.beginTransaction();
			AuditReader subReader = AuditReaderFactory.get(subSession);

			// Ambil PPB dari audit menggunakan subReader (bukan main-session reader)
			// sehingga semua proxy lazy terikat ke subSession, bukan main session.
			List ppbAudit = subReader.createQuery()
					.forRevisionsOfEntity(PengaturanPembayaranBulanan.class, true, false)
					.add(AuditEntity.id().eq(ppbId))
					.addOrder(AuditEntity.revisionNumber().desc())
					.setMaxResults(1)
					.getResultList();
			if (ppbAudit == null || ppbAudit.isEmpty()) {
				if (warnings != null) warnings.add("PPB " + ppbId + " tidak ditemukan di audit");
				return false;
			}

			PengaturanPembayaranBulanan ppbFromAudit = (PengaturanPembayaranBulanan) ppbAudit.get(0);
			Set<String> subProcessed = new HashSet<String>();
			// Pre-populasi PPB sendiri ke subProcessed SEBELUM rekursi dependensi.
			// Ini memotong siklus: PPB → DetailBiaya (tidak di DB) → restore DetailBiaya
			// → referensi balik DetailBiaya.ppb → coba replicate PPB sebelum DetailBiaya
			// masuk DB → FK violation. Dengan kunci PPB sudah ada, rekursi melewati PPB.
			if (ppbFromAudit.getId() != null) {
				subProcessed.add(PengaturanPembayaranBulanan.class.getName() + "-" + ppbFromAudit.getId());
			}
			restoreDependenciesRecursively(subSession, subReader, ppbFromAudit, subProcessed, warnings);

			// Verifikasi FK kritis: detail_biaya harus ada di DB sebelum PPB di-INSERT.
			// Jika masih tidak ada setelah restoreDependenciesRecursively (mis. tidak ada di
			// tabel audit), coba restore langsung dari audit sebelum menyerah.
			try {
				Object dbRef = ppbFromAudit.getDetailBiaya();
				if (dbRef instanceof org.hibernate.proxy.HibernateProxy) {
					java.io.Serializable depId = ((org.hibernate.proxy.HibernateProxy) dbRef)
							.getHibernateLazyInitializer().getIdentifier();
					if (depId != null) {
						final long depLongId = ((Number) depId).longValue();
						Number cnt = (Number) subSession.createSQLQuery(
								"SELECT COUNT(*) FROM public.detail_biaya WHERE id = :id")
								.setLong("id", depLongId).uniqueResult();
						if (cnt == null || cnt.longValue() == 0) {
							// detail_biaya tidak ada di DB — restore via sesi terpisah langsung
							// dari tabel audit Envers (native SQL INSERT SELECT).
							// Sesi terpisah menjamin transaksi bersih: Envers API dilewati karena
							// sub-sesi bisa dalam kondisi transaksi PostgreSQL ABORTED sehingga
							// query Envers mengembalikan kosong dan INSERT juga gagal.
							Session fixSession = null;
							Transaction fixTx = null;
							int fixAffected = 0;
							try {
								// Nama tabel audit sesuai konfigurasi Envers di hibernate.cfg.xml:
								// default_schema=new_audit, audit_table_suffix=__audit
								final String auditTable = "new_audit.detail_biaya__audit";

								fixSession = subSession.getSessionFactory().openSession();
								fixTx = fixSession.beginTransaction();
								java.sql.Connection fixConn = fixSession.connection();

								// Fase 1: INSERT SELECT dari tabel audit (hanya kolom aman tanpa FK)
								java.sql.Savepoint sp1 = null;
								try { sp1 = fixConn.setSavepoint("sp_db_audit"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1128");}
								try {
									fixAffected = fixSession.createSQLQuery(
											"INSERT INTO public.detail_biaya (id, nama, nilai_biaya)"
											+ " SELECT id, COALESCE(nama, 'Dipulihkan'), COALESCE(nilai_biaya, 0)"
											+ " FROM " + auditTable
											+ " WHERE id = :auditId AND revtype < 2"
											+ " ORDER BY rev DESC LIMIT 1"
											+ " ON CONFLICT (id) DO NOTHING")
											.setLong("auditId", depLongId)
											.executeUpdate();
									if (sp1 != null) { try { fixConn.releaseSavepoint(sp1); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1139");} }
									if (fixAffected > 0 && warnings != null)
										warnings.add("DetailBiaya#" + depLongId + " dipulihkan dari " + auditTable);
								} catch (Exception ex1) {
									ex1.printStackTrace(); ais.common.ErrorAuditUtil.record(ex1, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1143");
									if (sp1 != null) {
										try { fixConn.rollback(sp1); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1145");}
										try { fixConn.releaseSavepoint(sp1); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1146");}
									}
									if (warnings != null) warnings.add(
											"Restore DetailBiaya#" + depLongId + " dari audit gagal: " + ex1.getMessage());
								}

								// Fase 2: jika audit tidak ada data (atau INSERT gagal), buat stub minimal
								if (fixAffected == 0) {
									java.sql.Savepoint sp2 = null;
									try { sp2 = fixConn.setSavepoint("sp_db_stub"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1155");}
									try {
										fixAffected = fixSession.createSQLQuery(
												"INSERT INTO public.detail_biaya (id, nama, nilai_biaya)"
												+ " VALUES (:stubId, 'Stub dipulihkan', 0)"
												+ " ON CONFLICT (id) DO NOTHING")
												.setLong("stubId", depLongId)
												.executeUpdate();
										if (sp2 != null) { try { fixConn.releaseSavepoint(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1163");} }
										if (fixAffected > 0 && warnings != null)
											warnings.add("Stub DetailBiaya#" + depLongId + " dibuat");
									} catch (Exception ex2) {
										ex2.printStackTrace(); ais.common.ErrorAuditUtil.record(ex2, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1167");
										if (sp2 != null) {
											try { fixConn.rollback(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1169");}
											try { fixConn.releaseSavepoint(sp2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1170");}
										}
										if (warnings != null) warnings.add(
												"Stub DetailBiaya#" + depLongId + " gagal: " + ex2.getMessage());
									}
								}
								fixTx.commit();
							} catch (Exception fixEx) {
								fixEx.printStackTrace(); ais.common.ErrorAuditUtil.record(fixEx, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1178");
								if (fixTx != null) { try { fixTx.rollback(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1179");} }
								if (warnings != null) warnings.add(
										"Fix sesi DetailBiaya#" + depLongId + " error: " + fixEx.getMessage());
							} finally {
								if (fixSession != null) { try { fixSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1183");} }
							}

							if (fixAffected > 0) {
								// INSERT berhasil di fixSession — set cnt=1 tanpa query ulang
								// (sub-sesi mungkin ABORTED, COUNT-nya tidak dapat diandalkan)
								cnt = Long.valueOf(1L);
							}
							if (cnt == null || cnt.longValue() == 0) {
								if (warnings != null) warnings.add("Skip restore PPB " + ppbId
										+ ": DetailBiaya#" + depLongId
										+ " tidak ada di DB, restore dari audit gagal, stub juga gagal");
								if (subTx != null) { try { subTx.rollback(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1195");} }
								return false;
							}
						}
					}
				}
			} catch (Exception fkCheckEx) { ais.common.ErrorAuditUtil.record(fkCheckEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1201");
				// Tidak bisa verifikasi — lanjut, biarkan flush tangkap FK error
			}

			subSession.replicate(ppbFromAudit, ReplicationMode.OVERWRITE);
			subSession.flush();
			subTx.commit();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1210");
			if (subTx != null) { try { subTx.rollback(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1211");} }
			if (warnings != null) warnings.add("Gagal restore PPB " + ppbId + ": " + ex.getMessage());
			return false;
		} finally {
			if (subSession != null && subSession.isOpen()) {
				try { subSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1216");}
			}
		}
	}

	/**
	 * Langkah 3b: CicilanPembayaran dengan detail_biaya=null DAN
	 * pengaturan_pembayaran_bulanan=null — pulihkan keduanya dari tabel audit
	 * (revisi terbaru bukan DELETE). Empat sub-langkah:
	 *   A. UPDATE detail_biaya dari audit (FK target harus sudah ada di DB)
	 *   B1. UPDATE pengaturan_pembayaran_bulanan dari audit (FK target sudah ada)
	 *   B2. Restore entity PPB dulu (via restorePPBViaNativeSqlInSession) jika belum ada,
	 *       lalu coba UPDATE ulang
	 *   C. Sinkronkan detail_biaya dari PPB untuk cicilan yang masih detail_biaya=null
	 *   D. Sinkronkan item_biaya dari detail_biaya jika item_biaya=null
	 */
	@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
	private static void restoreNullDetailBiayaAndPPBFromAudit(
			Session session, Kegiatan kegiatan, List<String> warnings) {

		long kegiatanId = kegiatan.getId();
		java.sql.Connection conn = null;
		try { conn = session.connection(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1238");}

		// ── Langkah A: pulihkan detail_biaya dari audit ──────────────────────
		java.sql.Savepoint spA = null;
		try {
			if (conn != null) spA = conn.setSavepoint("sp_null_db_" + kegiatanId);
			int updDb = session.createSQLQuery(
					"UPDATE cicilan_pembayaran cp"
					+ " SET detail_biaya = ("
					+ "   SELECT a.detail_biaya"
					+ "   FROM new_audit.cicilan_pembayaran__audit a"
					+ "   WHERE a.id = cp.id AND a.detail_biaya IS NOT NULL AND a.revtype < 2"
					+ "     AND EXISTS (SELECT 1 FROM detail_biaya db WHERE db.id = a.detail_biaya)"
					+ "   ORDER BY a.rev DESC LIMIT 1"
					+ " )"
					+ " WHERE cp.kegiatan = :kgId"
					+ " AND cp.detail_biaya IS NULL"
					+ " AND cp.pengaturan_pembayaran_bulanan IS NULL"
					+ " AND EXISTS ("
					+ "   SELECT 1 FROM new_audit.cicilan_pembayaran__audit a"
					+ "   WHERE a.id = cp.id AND a.detail_biaya IS NOT NULL AND a.revtype < 2"
					+ " )")
					.setLong("kgId", kegiatanId).executeUpdate();
			if (conn != null && spA != null) {
				try { conn.releaseSavepoint(spA); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1262");}
				spA = null;
			}
			if (updDb > 0) {
				System.out.println("[Recovery] Restore detail_biaya dari audit: " + updDb + " cicilan untuk Kegiatan #" + kegiatanId);
				if (warnings != null) warnings.add("Restore detail_biaya (keduanya null) dari audit: " + updDb + " cicilan");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1270");
			if (spA != null && conn != null) { try { conn.rollback(spA); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1271");} }
			if (warnings != null) warnings.add("Restore detail_biaya (keduanya null) gagal: " + e.getMessage());
		}

		// ── Langkah B1: pulihkan PPB dari audit (FK target sudah ada di DB) ──
		java.sql.Savepoint spB = null;
		try {
			if (conn != null) spB = conn.setSavepoint("sp_null_ppb_" + kegiatanId);
			int updPpb = session.createSQLQuery(
					"UPDATE cicilan_pembayaran cp"
					+ " SET pengaturan_pembayaran_bulanan = ("
					+ "   SELECT a.pengaturan_pembayaran_bulanan"
					+ "   FROM new_audit.cicilan_pembayaran__audit a"
					+ "   WHERE a.id = cp.id AND a.pengaturan_pembayaran_bulanan IS NOT NULL AND a.revtype < 2"
					+ "     AND EXISTS (SELECT 1 FROM pengaturan_pembayaran_bulanan ppb WHERE ppb.id = a.pengaturan_pembayaran_bulanan)"
					+ "   ORDER BY a.rev DESC LIMIT 1"
					+ " )"
					+ " WHERE cp.kegiatan = :kgId"
					+ " AND cp.pengaturan_pembayaran_bulanan IS NULL"
					+ " AND EXISTS ("
					+ "   SELECT 1 FROM new_audit.cicilan_pembayaran__audit a"
					+ "   WHERE a.id = cp.id AND a.pengaturan_pembayaran_bulanan IS NOT NULL AND a.revtype < 2"
					+ " )")
					.setLong("kgId", kegiatanId).executeUpdate();
			if (conn != null && spB != null) {
				try { conn.releaseSavepoint(spB); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1296");}
				spB = null;
			}
			if (updPpb > 0) {
				System.out.println("[Recovery] Restore PPB (keduanya null) dari audit: " + updPpb + " cicilan untuk Kegiatan #" + kegiatanId);
				if (warnings != null) warnings.add("Restore PPB (keduanya null) dari audit: " + updPpb + " cicilan");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1304");
			if (spB != null && conn != null) { try { conn.rollback(spB); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1305");} }
			if (warnings != null) warnings.add("Restore PPB (keduanya null) gagal: " + e.getMessage());
		}

		// ── Langkah B2: PPB yang audit-nya ada tapi entity belum ada di DB ───
		// Cari PPB ID yang ada di audit cicilan tapi belum ada di tabel PPB
		List ppbMissingList = null;
		try {
			ppbMissingList = session.createSQLQuery(
					"SELECT DISTINCT a.pengaturan_pembayaran_bulanan"
					+ " FROM cicilan_pembayaran cp"
					+ " JOIN new_audit.cicilan_pembayaran__audit a ON a.id = cp.id"
					+ " WHERE cp.kegiatan = :kgId"
					+ " AND cp.pengaturan_pembayaran_bulanan IS NULL"
					+ " AND a.pengaturan_pembayaran_bulanan IS NOT NULL"
					+ " AND a.revtype < 2"
					+ " AND NOT EXISTS ("
					+ "   SELECT 1 FROM pengaturan_pembayaran_bulanan ppb"
					+ "   WHERE ppb.id = a.pengaturan_pembayaran_bulanan"
					+ " )")
					.setLong("kgId", kegiatanId).list();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1327");
			if (warnings != null) warnings.add("Scan PPB hilang (null+null) gagal: " + e.getMessage());
		}
		if (ppbMissingList != null && !ppbMissingList.isEmpty()) {
			for (Object ppbIdObj : ppbMissingList) {
				if (ppbIdObj == null) continue;
				Long ppbId = ((Number) ppbIdObj).longValue();
				restorePPBViaNativeSqlInSession(session, ppbId, warnings);
			}
			// Setelah entity PPB di-restore, coba UPDATE cicilan ulang
			java.sql.Savepoint spB2 = null;
			try {
				if (conn != null) spB2 = conn.setSavepoint("sp_null_ppb2_" + kegiatanId);
				int updPpb2 = session.createSQLQuery(
						"UPDATE cicilan_pembayaran cp"
						+ " SET pengaturan_pembayaran_bulanan = ("
						+ "   SELECT a.pengaturan_pembayaran_bulanan"
						+ "   FROM new_audit.cicilan_pembayaran__audit a"
						+ "   WHERE a.id = cp.id AND a.pengaturan_pembayaran_bulanan IS NOT NULL AND a.revtype < 2"
						+ "     AND EXISTS (SELECT 1 FROM pengaturan_pembayaran_bulanan ppb WHERE ppb.id = a.pengaturan_pembayaran_bulanan)"
						+ "   ORDER BY a.rev DESC LIMIT 1"
						+ " )"
						+ " WHERE cp.kegiatan = :kgId"
						+ " AND cp.pengaturan_pembayaran_bulanan IS NULL"
						+ " AND EXISTS ("
						+ "   SELECT 1 FROM new_audit.cicilan_pembayaran__audit a"
						+ "   WHERE a.id = cp.id AND a.pengaturan_pembayaran_bulanan IS NOT NULL AND a.revtype < 2"
						+ " )")
						.setLong("kgId", kegiatanId).executeUpdate();
				if (conn != null && spB2 != null) {
					try { conn.releaseSavepoint(spB2); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1357");}
					spB2 = null;
				}
				if (updPpb2 > 0) {
					System.out.println("[Recovery] Restore PPB pass-2 dari audit: " + updPpb2 + " cicilan");
					if (warnings != null) warnings.add("Restore PPB pass-2 setelah entity-restore: " + updPpb2 + " cicilan");
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1365");
				if (spB2 != null && conn != null) { try { conn.rollback(spB2); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1366");} }
				if (warnings != null) warnings.add("Restore PPB pass-2 gagal: " + e.getMessage());
			}
		}

		// ── Langkah C: sinkronkan detail_biaya dari PPB jika masih null ──────
		// Cicilan yang baru dapat PPB tapi detail_biaya-nya masih null → isi dari PPB.detail_biaya
		java.sql.Savepoint spC = null;
		try {
			if (conn != null) spC = conn.setSavepoint("sp_db_from_ppb_" + kegiatanId);
			int updDbFromPpb = session.createSQLQuery(
					"UPDATE cicilan_pembayaran cp"
					+ " SET detail_biaya = ("
					+ "   SELECT ppb.detail_biaya FROM pengaturan_pembayaran_bulanan ppb"
					+ "   WHERE ppb.id = cp.pengaturan_pembayaran_bulanan"
					+ "     AND ppb.detail_biaya IS NOT NULL"
					+ "   LIMIT 1"
					+ " )"
					+ " WHERE cp.kegiatan = :kgId"
					+ " AND cp.detail_biaya IS NULL"
					+ " AND cp.pengaturan_pembayaran_bulanan IS NOT NULL")
					.setLong("kgId", kegiatanId).executeUpdate();
			if (conn != null && spC != null) {
				try { conn.releaseSavepoint(spC); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1389");}
				spC = null;
			}
			if (updDbFromPpb > 0) {
				System.out.println("[Recovery] Sinkron detail_biaya dari PPB: " + updDbFromPpb + " cicilan");
				if (warnings != null) warnings.add("Sinkron detail_biaya dari PPB: " + updDbFromPpb + " cicilan");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1397");
			if (spC != null && conn != null) { try { conn.rollback(spC); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1398");} }
			if (warnings != null) warnings.add("Sinkron detail_biaya dari PPB gagal: " + e.getMessage());
		}

		// ── Langkah D: sinkronkan item_biaya jika null ───────────────────────
		java.sql.Savepoint spD = null;
		try {
			if (conn != null) spD = conn.setSavepoint("sp_ib_" + kegiatanId);
			int updIb = session.createSQLQuery(
					"UPDATE cicilan_pembayaran cp"
					+ " SET item_biaya = ("
					+ "   SELECT db.item_biaya FROM detail_biaya db"
					+ "   WHERE db.id = cp.detail_biaya AND db.item_biaya IS NOT NULL"
					+ "   LIMIT 1"
					+ " )"
					+ " WHERE cp.kegiatan = :kgId"
					+ " AND cp.item_biaya IS NULL"
					+ " AND cp.detail_biaya IS NOT NULL")
					.setLong("kgId", kegiatanId).executeUpdate();
			if (conn != null && spD != null) {
				try { conn.releaseSavepoint(spD); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1418");}
				spD = null;
			}
			if (updIb > 0) {
				System.out.println("[Recovery] Sinkron item_biaya dari detail_biaya: " + updIb + " cicilan");
				if (warnings != null) warnings.add("Sinkron item_biaya dari detail_biaya: " + updIb + " cicilan");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1426");
			if (spD != null && conn != null) { try { conn.rollback(spD); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1427");} }
			if (warnings != null) warnings.add("Sinkron item_biaya gagal: " + e.getMessage());
		}
	}

	/**
	 * Lakukan session.replicate + session.flush dilindungi JDBC Savepoint.
	 * Jika FK violation atau error lain terjadi: rollback ke savepoint + clear
	 * L1 cache sehingga transaksi PostgreSQL tetap valid untuk operasi berikutnya.
	 * Return true = berhasil; false = gagal (warning sudah dicatat bila non-null).
	 */
	@SuppressWarnings("deprecation")
	private static boolean replicateWithSavepoint(Session session, Object entity,
			String desc, List<String> warnings) {
		java.sql.Connection conn = null;
		java.sql.Savepoint sp = null;
		try {
			conn = session.connection();
			sp = conn.setSavepoint();
		} catch (Exception spEx) { ais.common.ErrorAuditUtil.record(spEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1446");
			// savepoint tidak tersedia — lanjut tanpa proteksi
		}
		try {
			session.replicate(entity, ReplicationMode.OVERWRITE);
			session.flush();
			if (sp != null) {
				try { conn.releaseSavepoint(sp); } catch (Exception relEx) { ais.common.ErrorAuditUtil.record(relEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1453");}
			}
			return true;
		} catch (Exception ex) {
			ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1457");
			if (sp != null && conn != null) {
				try { conn.rollback(sp); } catch (Exception rbEx) { ais.common.ErrorAuditUtil.record(rbEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1459");}
			}
			try { session.clear(); } catch (Exception clEx) { ais.common.ErrorAuditUtil.record(clEx, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1461");}
			if (warnings != null) {
				warnings.add("Gagal restore " + desc + ": " + ex.getMessage());
			}
			return false;
		}
	}

	/**
	 * Mesin Cerdas Deep Restore (Menggunakan Java Reflection)
	 */
	@SuppressWarnings("rawtypes")
	private static void restoreDependenciesRecursively(Session session, AuditReader reader,
			Object entityObj, Set<String> processedIds, List<String> warnings) {
		if (entityObj == null) return;

		java.lang.reflect.Method[] methods = entityObj.getClass().getMethods();
		for (java.lang.reflect.Method method : methods) {

			if (method.getName().startsWith("get") && GeneralValueObject.class.isAssignableFrom(method.getReturnType()) && !method.getName().equals("getClass")) {
				try {
					GeneralValueObject relation = (GeneralValueObject) method.invoke(entityObj);

					if (relation != null) {
						// Hibernate mengembalikan proxy Javassist (mis. DetailBiaya_$$_javassist_N).
						// session.get() dan forRevisionsOfEntity() butuh class ASLI, bukan proxy class.
						// Gunakan LazyInitializer untuk mendapat persistent class + id tanpa init proxy.
						Class relClass;
						java.io.Serializable relId;
						if (relation instanceof org.hibernate.proxy.HibernateProxy) {
							org.hibernate.proxy.LazyInitializer li =
									((org.hibernate.proxy.HibernateProxy) relation).getHibernateLazyInitializer();
							relClass = li.getPersistentClass();
							relId = li.getIdentifier();
						} else {
							relClass = relation.getClass();
							relId = relation.getId();
						}

						if (relId == null) continue;
						String key = relClass.getName() + "-" + relId;

						if (!processedIds.contains(key)) {
							processedIds.add(key);

							Object checkDb = session.get(relClass, relId);
							if (checkDb == null) {
								List auditResults = reader.createQuery()
										.forRevisionsOfEntity(relClass, true, false)
										.add(AuditEntity.id().eq(relId))
										.addOrder(AuditEntity.revisionNumber().desc())
										.setMaxResults(1)
										.getResultList();

								if (auditResults != null && !auditResults.isEmpty()) {
									Object auditRel = auditResults.get(0);
									restoreDependenciesRecursively(session, reader, auditRel, processedIds, warnings);
									// Gunakan JDBC savepoint agar transaksi tidak masuk state ABORTED
									// jika replicate gagal FK. Tanpa savepoint, satu replicate yang gagal
									// akan membuat SEMUA SQL berikutnya gagal ("current transaction is aborted").
									java.sql.Connection relConn = null;
									java.sql.Savepoint relSp = null;
									try {
										relConn = session.connection();
										relSp = relConn.setSavepoint();
									} catch (Exception spEx) {
										if (warnings != null) warnings.add("setSavepoint dep gagal: " + spEx.getMessage());
									}
									try {
										session.replicate(auditRel, ReplicationMode.OVERWRITE);
										if (relSp != null) { try { relConn.releaseSavepoint(relSp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1531");} }
									} catch (Exception repEx) {
										if (relSp != null && relConn != null) {
											try { relConn.rollback(relSp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1534");}
											try { relConn.releaseSavepoint(relSp); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1535");}
										}
										try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CicilanPembayaranRecoveryHelper.java:1537");}
										repEx.printStackTrace(); ais.common.ErrorAuditUtil.record(repEx, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1538");
											if (warnings != null) warnings.add("replicate dep "
												+ relClass.getSimpleName() + "#" + relId
												+ " gagal: " + repEx.getMessage());
									}
								} else {
									if (warnings != null) warnings.add("Dep "
											+ relClass.getSimpleName() + "#" + relId
											+ " tidak ada di DB maupun audit — dilewati");
								}
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CicilanPembayaranRecoveryHelper.java:1552");
					if (warnings != null) warnings.add("restoreDep proses " + method.getName()
							+ " gagal: " + e.getMessage());
				}
			}
		}
	}
}
