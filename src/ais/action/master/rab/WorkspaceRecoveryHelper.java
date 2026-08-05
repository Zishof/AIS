package ais.action.master.rab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ReplicationMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.ProgressListener;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.MyMessageboxConfig;

/**
 * Recovery item perencanaan anggaran ({@link Workspace}) yang TAK SENGAJA TERHAPUS, dipulihkan
 * dari tabel audit (Hibernate Envers). Polanya MENIRU
 * {@code ais.common.CicilanPembayaranRecoveryHelper} (tombol Recovery pada panel Angsuran
 * DaftarUlangMahasiswaLamaAction): konfirmasi → load-bar → proses di thread latar → restore via
 * {@code session.replicate(ReplicationMode.OVERWRITE)} untuk baris yang HILANG dari tabel utama.
 *
 * <p>Hanya memulihkan baris yang benar-benar HILANG (tidak ada di tabel utama) pada konteks
 * (Satuan Kerja + Tahun + Revisi + Sumber Dana) yang sedang dibuka; baris yang masih ada TIDAK
 * disentuh sehingga aman dijalankan berulang (idempoten).
 */
public class WorkspaceRecoveryHelper {

	private WorkspaceRecoveryHelper() {
	}

	/**
	 * Alur UI lengkap (konfirmasi + load-bar + thread). Dipanggil dari {@code onRecovery} pada
	 * WorkspaceRevisiAction / WorkspaceRevisiBulananAction.
	 */
	public static void jalankanRecovery(final SatuanKerja satuanKerja, final Integer tahun, final int revisi,
			final SumberDana sumberDana, final EventListener onSuccessListener) throws Exception {

		if (satuanKerja == null || tahun == null) {
			MyMessageboxConfig.show("Pilih Satuan Kerja & Tahun Anggaran terlebih dahulu.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		MyMessageboxConfig.show(
				"Apakah Anda yakin ingin melakukan Recovery data anggaran dari tabel audit?\n\n"
						+ "Item perencanaan yang TIDAK SENGAJA TERHAPUS pada Tahun " + tahun + " Revisi "
						+ (revisi < 0 ? 1 : revisi) + " Satuan Kerja " + satuanKerja.getNama()
						+ " akan dipulihkan. Item yang masih ada tidak akan terpengaruh.",
				"Konfirmasi Recovery", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int response = Integer.parseInt(event.getData().toString());
						if (response != MyMessageboxConfig.OK) {
							return;
						}

						final List<String> warnings = Collections.synchronizedList(new ArrayList<String>());
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
											MyMessageboxConfig.show(sb.toString(), "Informasi", MyMessageboxConfig.OK,
													MyMessageboxConfig.INFORMATION);
										} else {
											MyMessageboxConfig.show("Proses Recovery anggaran berhasil diselesaikan.",
													"Sukses", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										}
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
									restoreDeletedWorkspaceFromAudit(satuanKerja, tahun, revisi < 0 ? 1 : revisi,
											sumberDana, warnings, new ProgressListener() {
												@Override
												public void onProgress(final int percent, final String message) {
													try {
														Executions.schedule(desktop, new EventListener() {
															@Override
															public void onEvent(Event evSchedule) throws Exception {
																if (label != null) {
																	label.setValue("Loading... " + percent + "% ("
																			+ message + ")");
																}
															}
														}, null);
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/WorkspaceRecoveryHelper.java:127");
													}
												}
											});
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/WorkspaceRecoveryHelper.java:132");
									if (warnings != null) {
										warnings.add("Gagal memulihkan data anggaran: " + e.getMessage());
									}
								} finally {
									try {
										Executions.schedule(desktop, new EventListener() {
											@Override
											public void onEvent(Event evSchedule) throws Exception {
												if (label != null) {
													label.setValue("");
												}
											}
										}, null);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/WorkspaceRecoveryHelper.java:147");
									}
								}
							}
						}).start();
					}
				});
	}

	/**
	 * Engine recovery: query audit Workspace pada konteks, ambil versi NON-DELETE TERAKHIR per id,
	 * lalu restore (replicate OVERWRITE) hanya baris yang HILANG dari tabel utama.
	 */
	@SuppressWarnings("rawtypes")
	public static void restoreDeletedWorkspaceFromAudit(SatuanKerja satuanKerja, Integer tahun, int revisi,
			SumberDana sumberDana, List<String> warnings, ProgressListener progress) {

		Session session = null;
		Transaction tx = null;
		try {
			session = HibernateUtil.currentNativeSession();
			tx = session.beginTransaction();
			AuditReader reader = AuditReaderFactory.get(session);

			if (progress != null) {
				progress.onProgress(10, "Mencari histori anggaran...");
			}

			AuditQuery query = reader.createQuery().forRevisionsOfEntity(Workspace.class, false, true);
			query.add(AuditEntity.property("satuanKerja").eq(satuanKerja));
			if (sumberDana != null) {
				query.add(AuditEntity.property("sumberDana").eq(sumberDana));
			}
			query.add(AuditEntity.property("revisi").eq(revisi));
			query.add(AuditEntity.property("tahunWorkspace").eq(tahun));
			query.addOrder(AuditEntity.revisionNumber().desc());

			List results = query.getResultList();
			if (results == null || results.isEmpty()) {
				if (warnings != null) {
					warnings.add("Tidak ada histori anggaran yang ditemukan untuk konteks ini.");
				}
				if (tx != null) {
					tx.rollback();
				}
				return;
			}

			// Ambil versi NON-DELETE TERBARU per id (hasil DESC → kemunculan pertama = terbaru).
			// Revisi DELETE dilewati karena datanya bisa kosong (hanya id) tergantung konfigurasi Envers.
			if (progress != null) {
				progress.onProgress(35, "Menganalisa versi data terbaru...");
			}
			Map<Long, Workspace> latestMap = new LinkedHashMap<Long, Workspace>();
			for (Object obj : results) {
				Object[] tuple = (Object[]) obj;
				Workspace ws = (Workspace) tuple[0];
				RevisionType revType = (RevisionType) tuple[2];
				if (ws == null || ws.getId() == null || revType == RevisionType.DEL) {
					continue;
				}
				if (!latestMap.containsKey(ws.getId())) {
					latestMap.put(ws.getId(), ws);
				}
			}

			int total = latestMap.size();
			int current = 0;
			int restored = 0;
			for (Workspace ws : latestMap.values()) {
				current++;
				int percent = 35 + (total == 0 ? 0 : ((current * 60) / total));
				if (progress != null) {
					progress.onProgress(percent, "Memeriksa item: "
							+ (ws.getKode() == null ? String.valueOf(ws.getId()) : ws.getKode()));
				}

				// Hanya pulihkan baris yang HILANG dari tabel utama (idempoten).
				Object exist = session.get(Workspace.class, ws.getId());
				if (exist == null) {
					session.replicate(ws, ReplicationMode.OVERWRITE);
					session.flush();
					restored++;
				}
			}

			tx.commit();

			if (restored == 0 && warnings != null) {
				warnings.add("Tidak ada item anggaran yang perlu dipulihkan (semua item masih lengkap).");
			} else if (progress != null) {
				progress.onProgress(100, "Selesai memulihkan " + restored + " item.");
			}

		} catch (Exception e) {
			if (tx != null && tx.isActive()) {
				try {
					tx.rollback();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/WorkspaceRecoveryHelper.java:245");
				}
			}
			if (warnings != null) {
				warnings.add("Gagal memulihkan data anggaran: " + e.getMessage());
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/WorkspaceRecoveryHelper.java:251");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.disconnect();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/WorkspaceRecoveryHelper.java:256");
				}
				try {
					session.close();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/rab/WorkspaceRecoveryHelper.java:260");
				}
			}
		}
	}
}
