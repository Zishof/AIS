package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.StatusMahasiswa;

public class KrsDetailHelper {

	// ============================================================================================
	// 1. AMBIL DETAIL PERKULIAHAN UTAMA
	// ============================================================================================
	public static List<Long> ambilDetailperkuliahan(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial, Boolean semua, Integer persetujuan, boolean sudahDinilaiSaja,
			boolean belumDinilaiSaja) {
		boolean debug = Common.getKonfigurasi("debug_ambil_ambilDetailperkuliahan", Konfigurasi.TIDAK_AKTIF).getNilai()
				.trim().equalsIgnoreCase(Konfigurasi.AKTIF);
		return ambilDetailperkuliahan(mahasiswa, semester, tahapan, semesterPendek, remedial, semua, persetujuan,
				sudahDinilaiSaja, belumDinilaiSaja, debug);
	}

	public static List<Long> ambilDetailperkuliahan(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial, Boolean semua, Integer persetujuan, boolean sudahDinilaiSaja,
			boolean belumDinilaiSaja, boolean debug) {

		List<Long> detailperkuliahans = new ArrayList<Long>();
		if (mahasiswa == null)
			return detailperkuliahans;

		if (semesterPendek != null && semesterPendek.equals(Perkuliahan.SEMESTER_PENDEK)) {
			semua = false;
		}

		if (debug) {
			System.out.println("\n=================================================");
			System.out.println("DEBUG ambilDetailperkuliahan - Input Parameter:");
			System.out.println(
					"Mahasiswa  : " + (mahasiswa.getNim() != null ? mahasiswa.getNim() : "ID " + mahasiswa.getId()));
			System.out.println(
					"Semester   : " + semester + " | Tahapan: " + tahapan + " | SP (Param): " + semesterPendek);
			System.out.println("Remedial   : " + remedial + " | Semua: " + semua + " | Persetujuan: " + persetujuan);
			System.out.println("DinilaiSaja: " + sudahDinilaiSaja + " | BelumDinilaiSaja: " + belumDinilaiSaja);
			System.out.println("=================================================");
		}

		try {
			List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan();
			if (detailperkuliahansTemp == null) {
				return detailperkuliahans;
			}

			for (Long detailperkuliahanid : detailperkuliahansTemp) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());

				if (detail == null)
					continue;

				if (debug)
					System.out.println("\n -> Mengevaluasi Detail ID: " + detail.getId() + " (Smt DB: "
							+ detail.getSemester() + ", Tahap DB: " + detail.getTahap() + ")");

				double totalNilai = detail.getTotalNilai() != null ? detail.getTotalNilai() : 0.0;
				if (belumDinilaiSaja && totalNilai > 0.1) {
					if (debug)
						System.out.println("    [X] Ditolak: Syarat 'belumDinilaiSaja', tapi sudah ada nilai.");
					continue;
				}
				if (sudahDinilaiSaja && totalNilai < 0.01) {
					if (debug)
						System.out.println("    [X] Ditolak: Syarat 'sudahDinilaiSaja', tapi belum ada nilai.");
					continue;
				}

				Matakuliah matakuliah = detail.getMatakuliahKonversi() != null ? detail.getMatakuliahKonversi()
						: (detail.getPerkuliahan() != null ? detail.getPerkuliahan().getMatakuliah() : null);

				if (matakuliah == null) {
					if (debug)
						System.out.println("    [X] Ditolak: Matakuliah bernilai Null.");
					continue;
				}

				if (persetujuan != null && !persetujuan.equals(detail.getPersetujuan())) {
					if (debug)
						System.out.println("    [X] Ditolak: Status persetujuan tidak cocok.");
					continue;
				}

				if (semester != null && semester == 0) {
					if (detail.getMatakuliahKonversi() != null) {
						if (debug)
							System.out
									.println("    [V] Diterima: Matakuliah Konversi (Karena parameter semester = 0).");
						detailperkuliahans.add(detail.getId());
					} else {
						if (debug)
							System.out.println(
									"    [X] Ditolak: Parameter semester = 0, tetapi matakuliah BUKAN konversi.");
					}
					continue;
				}

				Perkuliahan perkuliahan = detail.getPerkuliahan();
				boolean isRemedial = perkuliahan != null && Boolean.TRUE.equals(perkuliahan.getMerupakanRemedial());
				boolean isSp = perkuliahan != null
						&& Perkuliahan.SEMESTER_PENDEK.equals(perkuliahan.getStatusSemesterPendek());

				if (debug)
					System.out.println("    Cek Kelas DB -> isSp: " + isSp + " | isRemedial: " + isRemedial);

				boolean matchTahapan = false;

				if (tahapan != null && tahapan > 0) {
					matchTahapan = tahapan.equals(detail.getTahap());
					if (debug)
						System.out.println("    Cek Match Tahapan -> " + tahapan + " == " + detail.getTahap() + " ? "
								+ matchTahapan);
				} else if (semester != null) {
					matchTahapan = semester.equals(detail.getSemester());
					if (debug)
						System.out.println("    Cek Match Semester -> " + semester + " == " + detail.getSemester()
								+ " ? " + matchTahapan);
				} else {
					matchTahapan = true;
					if (debug)
						System.out.println(
								"    Cek Match Semester -> Parameter Semester Null, Bypass pengecekan semester (Ambil Semua).");
				}

				if (!matchTahapan) {
					if (debug)
						System.out.println("    [X] Ditolak: Tidak match Tahapan/Semester.");
					continue;
				}

				if (remedial) {
					if (isRemedial) {
						if (debug)
							System.out.println("    [V] Diterima: Cocok dengan filter Remedial.");
						detailperkuliahans.add(detail.getId());
					} else {
						if (debug)
							System.out.println("    [X] Ditolak: Filter Remedial aktif, tapi data bukan Remedial.");
					}
				} else if (semua != null && semua) {
					if (!isRemedial) {
						if (debug)
							System.out.println("    [V] Diterima: Filter 'Semua' aktif (Bukan Remedial).");
						detailperkuliahans.add(detail.getId());
					} else {
						if (debug)
							System.out.println("    [X] Ditolak: Data adalah Remedial (diabaikan dari filter Semua).");
					}
				} else if (semesterPendek == null) {
					if (!isSp && !isRemedial) {
						if (debug)
							System.out.println("    [V] Diterima: Filter Regular (Bukan SP, Bukan Remedial).");
						detailperkuliahans.add(detail.getId());
					} else {
						if (debug)
							System.out.println("    [X] Ditolak: Diharapkan Regular, tapi data DB isSp = " + isSp
									+ ", isRemedial = " + isRemedial);
					}
				} else {
					if (isSp && !isRemedial) {
						if (debug)
							System.out.println("    [V] Diterima: Filter SP cocok dengan data DB.");
						detailperkuliahans.add(detail.getId());
					} else {
						if (debug)
							System.out.println("    [X] Ditolak: Diharapkan SP, tapi data DB isSp = " + isSp
									+ ", isRemedial = " + isRemedial);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:188");
		}

		if (debug) {
			System.out.println("=================================================");
			System.out.println("DEBUG ambilDetailperkuliahan - Total Diterima: " + detailperkuliahans.size());
			System.out.println("=================================================\n");
		}

		return detailperkuliahans;
	}

	// ============================================================================================
	// 2. AMBIL DETAIL PERKULIAHAN MK TERTENTU
	// ============================================================================================
	public static List<Long> ambilDetailperkuliahanMkTertentu(Mahasiswa mahasiswa, Integer semester,
			String... kodeAtauNamas) {
		List<Long> detailperkuliahans = new ArrayList<Long>();
		if (mahasiswa == null || kodeAtauNamas == null || kodeAtauNamas.length == 0)
			return detailperkuliahans;

		Set<String> targetSet = new HashSet<String>();
		for (String k : kodeAtauNamas) {
			if (k != null)
				targetSet.add(k.trim().toLowerCase());
		}

		try {
			List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan();

			for (Long detailperkuliahanid : detailperkuliahansTemp) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (detail == null)
					continue;

				if (semester != null && !semester.equals(detail.getSemester())) {
					continue;
				}

				Matakuliah matakuliah = detail.getMatakuliahKonversi() != null ? detail.getMatakuliahKonversi()
						: (detail.getPerkuliahan() != null ? detail.getPerkuliahan().getMatakuliah() : null);

				if (matakuliah == null)
					continue;

				String kode = matakuliah.getKode() != null ? matakuliah.getKode().trim().toLowerCase() : "";
				String nama = matakuliah.getNama() != null ? matakuliah.getNama().trim().toLowerCase() : "";

				if (targetSet.contains(kode) || targetSet.contains(nama)) {
					detailperkuliahans.add(detail.getId());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:242");
		}
		return detailperkuliahans;
	}

	// ============================================================================================
	// 3. AMBIL DETAIL PERKULIAHAN MK SAMPAI SMT TERTENTU
	// ============================================================================================
	public static List<Long> ambilDetailperkuliahanMkSdSmtTertentu(Mahasiswa mahasiswa, Integer semester,
			String... kodeAtauNamas) {
		List<Long> detailperkuliahans = new ArrayList<Long>();
		if (mahasiswa == null || kodeAtauNamas == null || kodeAtauNamas.length == 0)
			return detailperkuliahans;

		Set<String> targetSet = new HashSet<String>();
		for (String k : kodeAtauNamas) {
			if (k != null)
				targetSet.add(k.trim().toLowerCase());
		}

		try {
			List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan();

			for (Long detailperkuliahanid : detailperkuliahansTemp) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (detail == null)
					continue;

				if (semester != null && detail.getSemester() != null && detail.getSemester() > semester) {
					continue;
				}

				Matakuliah matakuliah = detail.getMatakuliahKonversi() != null ? detail.getMatakuliahKonversi()
						: (detail.getPerkuliahan() != null ? detail.getPerkuliahan().getMatakuliah() : null);

				if (matakuliah == null)
					continue;

				String kode = matakuliah.getKode() != null ? matakuliah.getKode().trim().toLowerCase() : "";
				String nama = matakuliah.getNama() != null ? matakuliah.getNama().trim().toLowerCase() : "";

				if (targetSet.contains(kode) || targetSet.contains(nama)) {
					detailperkuliahans.add(detail.getId());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:289");
		}
		return detailperkuliahans;
	}

	// ============================================================================================
	// 4. AMBIL DETAIL PERKULIAHAN KONVERSI
	// ============================================================================================
	public static List<Long> ambilDetailperkuliahanKonversi(Mahasiswa mahasiswa, Integer semester) {
		List<Long> detailperkuliahans = new ArrayList<Long>();
		if (mahasiswa == null)
			return detailperkuliahans;

		try {
			List<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan();

			for (Long detailperkuliahanid : detailperkuliahansTemp) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());

				if (detail != null && detail.getMatakuliahKonversi() != null) {
					if (semester == null || semester.equals(detail.getSemester())) {
						detailperkuliahans.add(detail.getId());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:316");
		}
		return detailperkuliahans;
	}

	// ============================================================================================
	// 5. AMBIL DETAIL PERKULIAHAN SAMPAI SEMESTER TERTENTU
	// ============================================================================================
	public static Collection<Long> ambilDetailperkuliahanSampai(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean semua, boolean saring) {
		List<Long> detailperkuliahans = new ArrayList<Long>();
		if (mahasiswa == null)
			return detailperkuliahans;

		try {
			Collection<Long> detailperkuliahansTemp = mahasiswa.ambilDetailperkuliahan();
			if (detailperkuliahansTemp == null) {
				return detailperkuliahans;
			}

			for (Long detailperkuliahanid : detailperkuliahansTemp) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (detail == null)
					continue;

				if (semester == null || semester == 0) {
					if (detail.getMatakuliahKonversi() != null) {
						detailperkuliahans.add(detail.getId());
					}
					continue;
				}

				Perkuliahan perkuliahan = detail.getPerkuliahan();
				boolean isSp = perkuliahan != null
						&& Perkuliahan.SEMESTER_PENDEK.equals(perkuliahan.getStatusSemesterPendek());

				boolean matchTahapan = false;
				if (tahapan == null || tahapan == 0) {
					matchTahapan = (detail.getSemester() != null && semester >= detail.getSemester());
				} else {
					matchTahapan = (detail.getTahap() != null && tahapan >= detail.getTahap());
				}

				if (!matchTahapan)
					continue;

				if (semua) {
					detailperkuliahans.add(detail.getId());
				} else if (semesterPendek == null) {
					if (!isSp)
						detailperkuliahans.add(detail.getId());
				} else {
					if (isSp)
						detailperkuliahans.add(detail.getId());
				}
			}

			if (saring) {
				return mahasiswa.saringBerdasarNilai(detailperkuliahans);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:376");
		}
		return detailperkuliahans;
	}

	// ============================================================================================
	// 6. KETERANGAN KRS & STATUS KRS
	// ============================================================================================
	public static String rubahKeteranganPengambilanKRSBersih(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, KrsMahasiswa krsMahasiswa, boolean remedial) {
		String krs = rubahKeteranganPengambilanKRS(mahasiswa, semester, tahapan, semesterPendek, krsMahasiswa,
				remedial);
		if (krs == null || krs.isEmpty())
			return "";

		return krs.replace("<font>", "").replace("</font>", "")
				.replace("<font>", "")
				.replace("<font>", "")
				.replace("<font>", "")
				.replace("<font>", "")
				.replace("<font>", "")
				.replace("<font>", "").replace("  ", " ").replace("  ", " ");
	}

	public static String rubahKeteranganPengambilanKRS(KrsMahasiswa krsMahasiswa, boolean remedial) {
		return rubahKeteranganPengambilanKRS(krsMahasiswa.getMahasiswa(), krsMahasiswa.getSemester(),
				krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa, remedial);
	}

	public static String rubahKeteranganPengambilanKRS(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, KrsMahasiswa krsMahasiswa, boolean remedial) {
		if (mahasiswa == null || semester == null || semester < 0)
			return "";

		try {
			PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = mahasiswa.ambilCuti(semester, tahapan,
					semesterPendek != null);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil
					.currentStatus(krsMahasiswa).getStatusMahasiswa();
			if (ConstantValues.CUTI != null && statusMahasiswa != null
					&& statusMahasiswa.getId().equals(ConstantValues.CUTI.getId())) {
				statusMahasiswa = ConstantValues.AKTIF;
			}

			int[] counts = getStatusKrsArray(mahasiswa, semester, tahapan, semesterPendek, remedial);
			int countBelumDisetujui = counts[0];
			int countSudahDisetujui = counts[1];
			int countDinilai = counts[2];
			int countBelumDinilai = counts[3];

			StringBuilder keterangan = new StringBuilder();

			if (tahapan != null && tahapan.equals(-1)) {
				if (countBelumDisetujui == 0 && countSudahDisetujui == 0) {
					keterangan.append("<font>Tidak ada Matakuliah ")
							.append(remedial ? " (remedial)" : "").append(" tanpa tahap</font><br>");
				} else {
					keterangan.append("<font>")
							.append(countBelumDisetujui == 0 ? "tidak ada perkuliahan tanpa tahap yang belum disetujui"
									: countBelumDisetujui + " perkuliahan tanpa tahap belum disetujui")
							.append("</font><br><font>")
							.append(countSudahDisetujui == 0 ? "belum ada perkuliahan tanpa tahap yang disetujui"
									: countSudahDisetujui + " perkuliahan tanpa tahap disetujui")
							.append("</font><br><font>")
							.append(countDinilai == 0 ? "belum ada perkuliahan tanpa tahap yang dinilai"
									: countDinilai + " perkuliahan tanpa tahap dinilai")
							.append("</font><br>");
				}
			} else if (semester.equals(0)) {
				if (countBelumDisetujui == 0 && countSudahDisetujui == 0) {
					keterangan.append("<font>Tidak ada Matakuliah Konversi</font><br>");
				} else {
					keterangan.append(
							"<font>Terdapat <font>")
							.append(countSudahDisetujui).append("</font><br> Matakuliah Konversi</font><br>");
				}
			} else {
				if (countSudahDisetujui > 0 && countBelumDisetujui == 0) {
					keterangan.append("<font>").append(countSudahDisetujui)
							.append(" perkuliahan ").append(remedial ? " (remedial)" : "")
							.append(" yang semuanya disetujui </font><br>")
							.append("<font>")
							.append(countDinilai == 0
									? "belum ada perkuliahan " + (remedial ? " (remedial)" : "") + " yang dinilai"
									: countDinilai + " perkuliahan " + (remedial ? " (remedial)" : "") + " dinilai")
							.append("</font><br>");
				} else if (countBelumDisetujui > 0 && countSudahDisetujui == 0) {
					keterangan.append("<font>").append(countBelumDisetujui)
							.append(" perkuliahan ").append(remedial ? " (remedial)" : "")
							.append(" yang semuanya belum disetujui </font><br>");
				} else {
					if (countBelumDisetujui == 0 && countSudahDisetujui == 0) {
						keterangan.append("<font>Belum pernah mengambil KRS")
								.append(remedial ? " (remedial)" : "").append("</font><br>");
					} else {
						keterangan.append("<font>").append(countBelumDisetujui == 0
								? "tidak ada perkuliahan " + (remedial ? " (remedial)" : "") + " yang belum disetujui"
								: countBelumDisetujui + " perkuliahan " + (remedial ? " (remedial)" : "")
										+ " belum disetujui")
								.append("</font><br><font>")
								.append(countSudahDisetujui == 0
										? "belum ada perkuliahan " + (remedial ? " (remedial)" : "") + " yang disetujui"
										: countSudahDisetujui + " perkuliahan disetujui")
								.append("</font><br><font>")
								.append(countDinilai == 0
										? "belum ada perkuliahan " + (remedial ? " (remedial)" : "") + " yang dinilai"
										: countDinilai + " perkuliahan " + (remedial ? " (remedial)" : "") + " dinilai")
								.append("</font><br>");
					}
				}

				String titleSmt = semester.equals(0) ? "matakuliah konversi"
						: ((tahapan == null || tahapan == 0 ? "semester " + semester : " ")
								+ (tahapan == null || tahapan == 0 ? "" : " tahap " + tahapan));

				keterangan.insert(0, "<font>" + titleSmt + "</font><br> "
						+ (semesterPendek == null ? "" : " semester pendek (SP) "));
			}

			if (countBelumDinilai > 0 && countDinilai > 0) {
				keterangan.append(" <font>").append(countBelumDinilai)
						.append(" perkuliahan ").append(remedial ? " (remedial)" : "")
						.append(" belum dinilai</font><br>");
			}

			StringBuilder result = new StringBuilder();
			if (pendaftaranCutiMahasiswa != null && Boolean.TRUE.equals(pendaftaranCutiMahasiswa.getPersetujuan())) {
				result.append("<font>Cuti karena alasan \"")
						.append(pendaftaranCutiMahasiswa.getKeterangan()).append("\"</font><br>");
			}
			if (statusMahasiswa != null && ConstantValues.AKTIF != null
					&& !statusMahasiswa.getId().equals(ConstantValues.AKTIF.getId())) {
				result.append("<font>Status: ").append(statusMahasiswa.getNama())
						.append("</font><br> ");
			}
			result.append(keterangan);

			return result.toString();

		} catch (Exception e) {
			return "";
		}
	}

	public static Integer[] getStatusKrs(Mahasiswa mahasiswa, Integer semester, Integer tahapan, Integer semesterPendek,
			boolean remedial) {
		int[] counts = getStatusKrsArray(mahasiswa, semester, tahapan, semesterPendek, remedial);
		return new Integer[] { counts[0], counts[1], counts[2], counts[3] };
	}

	private static int[] getStatusKrsArray(Mahasiswa mahasiswa, Integer semester, Integer tahapan,
			Integer semesterPendek, boolean remedial) {
		int countSudahDisetujui = 0;
		int countBelumDisetujui = 0;
		int countDinilai = 0;
		int countBelumDinilai = 0;

		if (mahasiswa == null)
			return new int[] { 0, 0, 0, 0 };

		try {
			List<Long> detailperkuliahans = ambilDetailperkuliahan(mahasiswa, semester, tahapan, semesterPendek,
					remedial, null, null, false, false);

			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detail = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
						detailperkuliahanid.toString());
				if (detail != null) {
					boolean disetujui = Detailperkuliahan.DISETUJUI.equals(detail.getPersetujuan());

					if (disetujui)
						countSudahDisetujui++;
					if (Detailperkuliahan.BELUM_DISETUJUI.equals(detail.getPersetujuan()))
						countBelumDisetujui++;

					double totalNilai = detail.getTotalNilai() != null ? detail.getTotalNilai() : 0.0;

					if (disetujui && totalNilai >= 0.1)
						countDinilai++;
					if (disetujui && totalNilai < 0.01)
						countBelumDinilai++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/KrsDetailHelper.java:561");
		}

		return new int[] { countBelumDisetujui, countSudahDisetujui, countDinilai, countBelumDinilai };
	}
}
