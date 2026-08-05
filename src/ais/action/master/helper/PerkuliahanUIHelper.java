package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.ui.util.MyHtml;
import ais.ui.util.MyLabelKecilBold;

public class PerkuliahanUIHelper {

	// =================================================================================================
	// 1. METODE GENERATOR STRING HTML (COCOK UNTUK JSON / API / JSP FRONTEND)
	// =================================================================================================
	
	public static String generateHariJamRuanganPerkuliahanUmumText(Perkuliahan perkuliahan) {
		if (perkuliahan == null) return "";
		
		Session session = null;
		StringBuilder sb = new StringBuilder();
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Tambahkan Jadwal Utama
			sb.append(buildSingleJadwalHtmlString(perkuliahan, false));

			// 2. Tambahkan Jadwal Paralel (Jika Ada)
			List<Perkuliahan> jadwalParalels = perkuliahan.ambilParalelPerkuliahan();
			if (jadwalParalels != null && !jadwalParalels.isEmpty()) {
				sb.append("<div>Paralel dengan : <hr></div>");
				for (Perkuliahan jadwal : jadwalParalels) {
					sb.append(buildSingleJadwalHtmlString(jadwal, false));
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:52");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:55");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:56");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:57");}
			}
		}
		
		return sb.toString();
	}

	public static String generateTeksDosenPerkuliahan(Perkuliahan perkuliahan) {
		if (perkuliahan == null) return "-";
		
		Session session = null;
		StringBuilder sb = new StringBuilder();
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			// 1. Dosen Pengajar
			List<Dosen> map = perkuliahan.populateDosenBuNama();
			if (map != null) {
				for (Dosen dosen : map) {
					if (dosen == null || dosen.getNama() == null) continue;
					if (sb.length() > 0) sb.append(", ");
					sb.append(dosen.getNama().trim());
				}
			}

			// 2. Asisten Mahasiswa (Jika Ada)
			List<Mahasiswa> asistens = perkuliahan.ambilAsisten();
			if (asistens != null) {
				for (Mahasiswa mahasiswa : asistens) {
					if (mahasiswa == null || mahasiswa.getNama() == null) continue;
					if (sb.length() > 0) sb.append(", ");
					sb.append(mahasiswa.getNama().trim()).append(" (Asisten)");
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:93");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:96");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:97");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:98");}
			}
		}
		
		return sb.length() > 0 ? sb.toString() : "-";
	}

	// =================================================================================================
	// 2. PRIVATE HELPER UNTUK MEMBENTUK TEKS HTML JADWAL (Mencegah Duplikasi Kode)
	// =================================================================================================

	private static String buildSingleJadwalHtmlString(Perkuliahan perkuliahan, boolean master) {
		if (perkuliahan == null) return "";
		
		String hari = perkuliahan.getHari() != null ? perkuliahan.getHari().trim() : "";
		String wMulai = perkuliahan.getWaktuMulai() != null ? perkuliahan.getWaktuMulai().trim() : "";
		String wSelesai = perkuliahan.getWaktuSelesai() != null ? perkuliahan.getWaktuSelesai().trim() : "";
		String waktu = (wMulai + " " + wSelesai).trim();

		String ruang = (perkuliahan.getRuang() != null && perkuliahan.getRuang().getKodeRuangan() != null)
				? perkuliahan.getRuang().getKodeRuangan().trim() : "";

		String waktuPerkuliahan = "";
		if (perkuliahan.getMasaPerkuliahan() != null && perkuliahan.getMasaPerkuliahan().getNama() != null) {
			waktuPerkuliahan = perkuliahan.getMasaPerkuliahan().getNama().trim();
			if (!waktuPerkuliahan.isEmpty()) waktuPerkuliahan = "(" + waktuPerkuliahan + ")";
		}

		String tahap = "";
		if ((ConstantValues.aktifkanTahapan || ConstantValues.aktifkanTahapanKurikulum)
				&& perkuliahan.getKurikulumPunyaMatakuliah() != null
				&& perkuliahan.getKurikulumPunyaMatakuliah().getTahap() != null) {
			tahap = " / tahap " + perkuliahan.getKurikulumPunyaMatakuliah().getTahap();
		}

		StringBuilder sb = new StringBuilder("<div style='");
		if (master) {
			sb.append("color:red;font-weight:bolder;'>");
		} else if (perkuliahan.getPerkuliahan_paralel() != null) {
			sb.append("color:blue;'>");
		} else {
			sb.append("color:#333;font-weight: bolder;'>");
		}

		if (!hari.isEmpty()) sb.append("<i class=\"far fa-calendar-alt me-1\"></i> ").append(hari).append(" ");
		if (!waktuPerkuliahan.isEmpty()) sb.append(" / ").append(waktuPerkuliahan).append(" ");
		if (!waktu.isEmpty()) sb.append(" / <i class=\"far fa-clock me-1\"></i> ").append(waktu).append(" ");
		if (!ruang.isEmpty()) sb.append(" / <i class=\"fas fa-map-marker-alt me-1\"></i> Ruang ").append(ruang).append(" ");

		String ketJadwal = perkuliahan.getKeteranganJadwal() != null ? perkuliahan.getKeteranganJadwal().trim() : "";
		if (!ketJadwal.isEmpty()) {
			sb.append(" <div>").append(ketJadwal).append("</div> ");
		}

		if (perkuliahan.getMerupakanRemedial() != null && perkuliahan.getMerupakanRemedial()) {
			sb.append(" / <span>Remedial</span> ");
		}

		String ket = perkuliahan.getKeterangan() != null ? perkuliahan.getKeterangan().trim() : "";
		if (!ket.isEmpty()) sb.append(" / ").append(ket);

		sb.append(tahap);

		String thnAjaran = perkuliahan.getTahunAjaran() != null ? perkuliahan.getTahunAjaran() : "";
		String prog = perkuliahan.getProgram() != null ? perkuliahan.getProgram() : "";

		sb.append(" / ").append(thnAjaran).append(" / ").append(prog).append("</div>");

		return sb.toString();
	}

	// =================================================================================================
	// 3. METODE UI UNTUK ZK FRAMEWORK
	// =================================================================================================

	public static Vbox displayHariJamRuanganPerkuliahan(Perkuliahan perkuliahan, boolean master) {
		Vbox vbox = new Vbox();
		if (perkuliahan == null) return vbox;

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			// Menggunakan method helper di atas agar kode lebih DRY (Don't Repeat Yourself)
			String htmlString = buildSingleJadwalHtmlString(perkuliahan, master);
			vbox.appendChild(new MyHtml(htmlString));
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:184");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:187");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:188");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:189");}
			}
		}
		return vbox;
	}

	public static void displayHariJamRuanganPerkuliahan(Row row, Perkuliahan perkuliahan) {
		displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, null);
	}

	public static void displayHariJamRuanganPerkuliahanUmum(Component row, Perkuliahan perkuliahan) {
		displayHariJamRuanganPerkuliahanUmum(row, perkuliahan, null);
	}

	public static void displayHariJamRuanganPerkuliahanUmum(Component row, Perkuliahan perkuliahan, Detailperkuliahan detailperkuliahan) {
		if (perkuliahan == null) {
			new Label().setParent(row);
			return;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Perkuliahan> jadwalParalels = perkuliahan.ambilParalelPerkuliahan();
			
			if (jadwalParalels != null && !jadwalParalels.isEmpty()) {
				Vbox rowParalel = new Vbox();
				rowParalel.setParent(row);

				Vbox html = displayHariJamRuanganPerkuliahan(perkuliahan, true);
				if (detailperkuliahan != null && detailperkuliahan.getDetailNilaiTambahan() != null
						&& !detailperkuliahan.getDetailNilaiTambahan().trim().isEmpty()) {
					html.appendChild(new Label(detailperkuliahan.getDetailNilaiTambahan().trim()));
				}

				rowParalel.appendChild(html);
				rowParalel.appendChild(new MyHtml("<div>Paralel dengan : <hr></div>"));

				for (Perkuliahan jadwal : jadwalParalels) {
					rowParalel.appendChild(displayHariJamRuanganPerkuliahan(jadwal, false));
				}
			} else {
				Vbox html = displayHariJamRuanganPerkuliahan(perkuliahan, false);
				if (detailperkuliahan != null && detailperkuliahan.getDetailNilaiTambahan() != null
						&& !detailperkuliahan.getDetailNilaiTambahan().trim().isEmpty()) {
					html.appendChild(new Label(detailperkuliahan.getDetailNilaiTambahan().trim()));
				}
				row.appendChild(html);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:241");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:244");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:245");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:246");}
			}
		}
	}

	public static Hbox displayDosenPerkuliahan(Component row, Perkuliahan perkuliahan, Boolean tampilAsistenDiParaelel) {
		return displayDosenPerkuliahanUmum(row, perkuliahan, true, tampilAsistenDiParaelel, null);
	}

	public static Hbox displayDosenPerkuliahan(Component row, Perkuliahan perkuliahan, Boolean displayName, Boolean tampilAsistenDiParaelel) {
		return displayDosenPerkuliahanUmum(row, perkuliahan, displayName, tampilAsistenDiParaelel, null);
	}

	public static Hbox displayDosenPerkuliahanUmum(Component row, Perkuliahan perkuliahan, Boolean tampilAsistenDiParaelel) {
		return displayDosenPerkuliahanUmum(row, perkuliahan, true, tampilAsistenDiParaelel, null);
	}

	public static Hbox displayDosenPerkuliahanUmum(Component row, Perkuliahan perkuliahan, Boolean displayName, Boolean tampilAsistenDiParaelel) {
		return displayDosenPerkuliahanUmum(row, perkuliahan, displayName, tampilAsistenDiParaelel, null);
	}

	public static Hbox displayDosenPerkuliahanUmum(Component row, Perkuliahan perkuliahan, Boolean displayName, Boolean tampilAsistenDiParaelel, Dosen dosenTambahan) {
		Hbox mainHbox = new Hbox();
		
		if (perkuliahan == null) {
			new Label().setParent(mainHbox);
			mainHbox.setParent(row);
			return mainHbox;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			List<Dosen> map = perkuliahan.populateDosenBuNama();
			
			Vbox rowParalel = new Vbox();
			rowParalel.setParent(row);
			rowParalel.appendChild(mainHbox);

			int tampilPerRow = Common.isMobile() ? 2 : 6;
			int size = 0;

			Vbox currentVbox = new Vbox();
			currentVbox.setParent(mainHbox);
			Hbox currentHbox = new Hbox();
			currentHbox.setParent(currentVbox);

			List<StringBuilder> nameRows = new ArrayList<StringBuilder>();
			StringBuilder currentNames = new StringBuilder();
			nameRows.add(currentNames);

			// Rendering Dosen
			if (map != null) {
				for (Dosen dosen : map) {
					if (dosen == null) continue;
					
					if (size > 0 && size % tampilPerRow == 0) {
						currentHbox = new Hbox();
						currentHbox.setParent(currentVbox);
						currentNames = new StringBuilder();
						nameRows.add(currentNames);
					}
					
					try {
						CommonMedia.tampilkanGambarKecil(dosen).setParent(currentHbox);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}

					if (currentNames.length() > 0) currentNames.append(", ");
					currentNames.append(dosen.getNama() != null ? dosen.getNama().trim() : "");
					size++;
				}
			}

			// Rendering Dosen Tambahan
			if (dosenTambahan != null) {
				if (size > 0 && size % tampilPerRow == 0) {
					currentHbox = new Hbox();
					currentHbox.setParent(currentVbox);
					currentNames = new StringBuilder();
					nameRows.add(currentNames);
				}
				
				try {
					CommonMedia.tampilkanGambarKecil(dosenTambahan).setParent(currentHbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
				
				if (currentNames.length() > 0) currentNames.append(", ");
				currentNames.append(dosenTambahan.getNama() != null ? dosenTambahan.getNama().trim() : "");
				size++;
			}

			// Rendering Asisten
			if (tampilAsistenDiParaelel != null && tampilAsistenDiParaelel) {
				List<Mahasiswa> asistens = perkuliahan.ambilAsisten();
				if (asistens != null) {
					for (Mahasiswa mahasiswa : asistens) {
						if (mahasiswa == null) continue;
						
						if (size > 0 && size % tampilPerRow == 0) {
							currentHbox = new Hbox();
							currentHbox.setParent(currentVbox);
							currentNames = new StringBuilder();
							nameRows.add(currentNames);
						}
						
						try {
							CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(currentHbox);
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
						
						if (currentNames.length() > 0) currentNames.append(", ");
						currentNames.append(mahasiswa.getNama() != null ? mahasiswa.getNama().trim() : "").append(" (Asisten)");
						size++;
					}
				}
			}

			// Rendering Nama Teks di bawah gambar
			if (displayName != null && displayName) {
				for (StringBuilder sb : nameRows) {
					String rowNames = sb.toString().trim();
					if (!rowNames.isEmpty()) {
						rowParalel.appendChild(new MyLabelKecilBold(rowNames));
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:380");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:383");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:384");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:385");}
			}
		}

		return mainHbox;
	}

	public static Hbox displayDosen(Component row, TreeMap<Long, Dosen> map, Boolean displayName) {
		Hbox mainHbox = new Hbox();
		
		if (map == null || map.isEmpty()) {
			mainHbox.setParent(row);
			return mainHbox;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Vbox rowParalel = new Vbox();
			rowParalel.setParent(row);
			rowParalel.appendChild(mainHbox);

			int tampilPerRow = Common.isMobile() ? 2 : 6;
			int size = 0;

			Vbox currentVbox = new Vbox();
			currentVbox.setParent(mainHbox);
			Hbox currentHbox = new Hbox();
			currentHbox.setParent(currentVbox);

			List<StringBuilder> nameRows = new ArrayList<StringBuilder>();
			StringBuilder currentNames = new StringBuilder();
			nameRows.add(currentNames);

			for (Dosen dosen : map.values()) {
				if (dosen == null) continue;

				if (size > 0 && size % tampilPerRow == 0) {
					currentHbox = new Hbox();
					currentHbox.setParent(currentVbox);
					currentNames = new StringBuilder();
					nameRows.add(currentNames);
				}
				
				try {
					CommonMedia.tampilkanGambarKecil(dosen).setParent(currentHbox);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}

				if (currentNames.length() > 0) currentNames.append(", ");
				currentNames.append(dosen.getNama() != null ? dosen.getNama().trim() : "");

				size++;
			}

			if (displayName != null && displayName) {
				for (StringBuilder sb : nameRows) {
					String rowNames = sb.toString().trim();
					if (!rowNames.isEmpty()) {
						rowParalel.appendChild(new MyLabelKecilBold(rowNames));
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/PerkuliahanUIHelper.java:452");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:455");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:456");}
				try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/PerkuliahanUIHelper.java:457");}
			}
		}

		return mainHbox;
	}
}
