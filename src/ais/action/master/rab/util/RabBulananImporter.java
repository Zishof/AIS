package ais.action.master.rab.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;

public class RabBulananImporter {

	private static final String[] NAMA_BULAN = { "Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt",
			"Nop", "Des" };

	/**
	 * Parse nilai anggaran yang TAHAN format Indonesia: membuang "Rp"/spasi/teks lain, mengenali
	 * pemisah ribuan (titik ATAU koma) dan desimal dari posisi paling kanan. Kosong/"-" → 0.0.
	 * (Perbaikan: kode lama {@code replaceAll(",",".")} GAGAL pada angka ber-ribuan titik "1.000.000".)
	 */
	public static double parseNilai(String raw) {
		if (raw == null) {
			return 0.0;
		}
		String s = raw.trim();
		if (s.isEmpty()) {
			return 0.0;
		}
		s = s.replaceAll("[^0-9,.\\-]", "");
		if (s.isEmpty() || s.equals("-")) {
			return 0.0;
		}
		int lc = s.lastIndexOf(',');
		int ld = s.lastIndexOf('.');
		if (lc >= 0 && ld >= 0) {
			if (lc > ld) {
				s = s.replace(".", "").replace(",", "."); // koma desimal, titik ribuan
			} else {
				s = s.replace(",", ""); // titik desimal, koma ribuan
			}
		} else if (lc >= 0) {
			int after = s.length() - lc - 1;
			if (s.indexOf(',') == lc && after > 0 && after <= 2) {
				s = s.replace(",", "."); // satu koma, ≤2 digit → desimal
			} else {
				s = s.replace(",", ""); // ribuan
			}
		} else if (ld >= 0) {
			int after = s.length() - ld - 1;
			if (!(s.indexOf('.') == ld && after > 0 && after <= 2)) {
				s = s.replace(".", ""); // bukan desimal → ribuan (Indonesia)
			}
		}
		return Double.parseDouble(s);
	}

	private static String pesanRingkas(Throwable e) {
		String m = e == null ? null : e.getMessage();
		if (m == null || m.trim().isEmpty()) {
			m = e == null ? "kesalahan tidak diketahui" : e.getClass().getSimpleName();
		}
		return m.length() > 300 ? m.substring(0, 300) + "..." : m;
	}

	/** Overload lama (tanpa laporan rinci). */
	public static List<Workspace> doImport(File file, final Integer tahunWorkspace, final SatuanKerja satuanKerja,
			final SumberDana sumberDana, final Integer revisi) throws Exception {
		return doImport(file, tahunWorkspace, satuanKerja, sumberDana, revisi, null, null);
	}

	/**
	 * Import anggaran BULANAN dari xlsx. {@code gagal}/{@code berhasil} (boleh null) diisi pesan
	 * per-baris agar pemanggil dapat menampilkan laporan JELAS: mana yang sukses & mana yang gagal
	 * beserta penyebab + saran.
	 */
	public static List<Workspace> doImport(File file, final Integer tahunWorkspace, final SatuanKerja satuanKerja,
			final SumberDana sumberDana, final Integer revisi, final List<String> gagal, final List<String> berhasil)
			throws Exception {

		XSSFWorkbook workbook = new XSSFWorkbook(file.getAbsolutePath());
		XSSFSheet sheet = workbook.getSheetAt(0);

		List<String[]> objects = new ArrayList<String[]>();
		int i = 1;
		String cell = null;
		while (true) {
			try {
				cell = Common.getCellContent(Common.getCell(sheet, 0, i));
			} catch (Exception e) {
				// Common.tampilErrorJikaAdmin(e);
				break;
			}
			if (cell == null || cell.trim().isEmpty()) {
				break;
			}
			String[] objs = new String[16];
			for (int j = 0; j <= 15; j++) {
				try {
					cell = Common.getCellContent(Common.getCell(sheet, j, i));
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					continue;
				}
				objs[j] = cell;
			}
			objects.add(objs);
			i++;
		}

		Long nilaiMin = Long.MIN_VALUE + 2000000000000000000L;
		Long nilaiMax = nilaiMin + 1000000000000000000L;

		Session session = HibernateUtil.currentNativeSession();
		Long nilaiMinInDb = (Long) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))).add(Restrictions.gt("id", nilaiMin))
				.add(Restrictions.lt("id", nilaiMax)).setProjection(Projections.max("id")).uniqueResult();
		// session.disconnect();
		if (session.isOpen()) {session.disconnect();session.close();}
		HibernateUtil.closeSession();

		if (nilaiMinInDb == null) {
			nilaiMinInDb = nilaiMin;
		}
		nilaiMinInDb += 10000;

		List<Workspace> workspaces = new ArrayList<Workspace>();
		final List<String[]> workspaceSource = new ArrayList<String[]>();
		final List<Integer> workspaceExcelRow = new ArrayList<Integer>();
		for (int idxBaris = 0; idxBaris < objects.size(); idxBaris++) {
			String[] strings = objects.get(idxBaris);
			int excelRow = idxBaris + 2; // baris 1 = header, data mulai baris 2
			String kode = strings[2] == null ? "" : strings[2].trim();
			String nama = strings[3] == null ? "" : strings[3].trim();

			try {
				int bln = 3;
				double[] b = new double[12];
				for (int m = 0; m < 12; m++) {
					String raw = strings[bln + 1 + m];
					try {
						b[m] = parseNilai(raw);
					} catch (Exception exNum) {
						throw new RuntimeException("nilai bulan " + NAMA_BULAN[m] + " = \""
								+ (raw == null ? "" : raw.trim()) + "\" bukan angka yang valid");
					}
				}
				Double bulan1 = b[0], bulan2 = b[1], bulan3 = b[2], bulan4 = b[3], bulan5 = b[4], bulan6 = b[5];
				Double bulan7 = b[6], bulan8 = b[7], bulan9 = b[8], bulan10 = b[9], bulan11 = b[10], bulan12 = b[11];

				Double total = bulan1 + bulan2 + bulan3 + bulan4 + bulan5 + bulan6 + bulan7 + bulan8 + bulan9 + bulan10
						+ bulan11 + bulan12;

				// Validasi DINI kolom "ID Workspace" & "No" (parent) — dipakai fase penataan id.
				// (Bug lama: baris rusak yang lolos membuat objects.get(i) di fase-2 MISALIGN → id kacau.)
				try {
					Long.parseLong(strings[0].trim());
				} catch (Exception exId) {
					throw new RuntimeException("kolom 'ID Workspace' = \""
							+ (strings[0] == null ? "" : strings[0].trim()) + "\" kosong/tidak valid");
				}
				try {
					Long.parseLong(strings[1].trim());
				} catch (Exception exP) {
					throw new RuntimeException("kolom 'No' (parent) = \""
							+ (strings[1] == null ? "" : strings[1].trim()) + "\" kosong/tidak valid");
				}

				session = HibernateUtil.currentNativeSession();

				Akun akun = (Akun) ConstantValues.simpleObject(
						session.createCriteria(Akun.class).add(Restrictions.eq("kode", kode)).setMaxResults(1),
						Akun.class);

				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}

				HibernateUtil.closeSession();

				Workspace workspace = new Workspace();
				workspace.setAkun(akun);
				workspace.setCopyForm(null);
				workspace.setDeep(null);
				workspace.setDurasi(0);
				workspace.setHargaSatuan(total);
				workspace.setHargaTotal(total);
				workspace.setJenisWorkspace(null);
				workspace.setJmlDipakai(0L);
				workspace.setJmlWaktu(1.0);
				workspace.setKeterangan("");
				workspace.setKode(kode);
				workspace.setLeaf(false);
				workspace.setMerupakanHasilCopy(false);
				workspace.setMulai(null);
				workspace.setNama(nama);
				workspace.setParentId(-1L);
				workspace.setPersenKomplit(0.0);
				workspace.setQty(1.0);
				workspace.setRealisasiTotal(0.0);
				workspace.setRevisi(revisi);
				workspace.setSatuan(null);
				workspace.setSatuan1(null);
				workspace.setSatuanKerja(satuanKerja);
				workspace.setSatuanVolume(null);
				workspace.setSelesai(null);
				workspace.setSumberDana(sumberDana);
				workspace.setTahunWorkspace(tahunWorkspace);
				workspace.setUnitOrganisasi(null);
				workspace.setVolume(1.0);

				workspace.setBulan1(bulan1);
				workspace.setBulan2(bulan2);
				workspace.setBulan3(bulan3);
				workspace.setBulan4(bulan4);
				workspace.setBulan5(bulan5);
				workspace.setBulan6(bulan6);
				workspace.setBulan7(bulan7);
				workspace.setBulan8(bulan8);
				workspace.setBulan9(bulan9);
				workspace.setBulan10(bulan10);
				workspace.setBulan11(bulan11);
				workspace.setBulan12(bulan12);

				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(workspace);
				session.getTransaction().commit();

				// session.disconnect();
				if (session.isOpen()) {session.disconnect();session.close();}

				HibernateUtil.closeSession();

				workspaces.add(workspace);
				workspaceSource.add(strings);
				workspaceExcelRow.add(Integer.valueOf(excelRow));
				if (berhasil != null) {
					berhasil.add("Baris " + excelRow + " — " + (kode.isEmpty() ? "(tanpa kode)" : kode)
							+ (nama.isEmpty() ? "" : " : " + nama));
				}
			} catch (Exception e) {
				if (gagal != null) {
					gagal.add("Baris " + excelRow + " (Kode: " + (kode.isEmpty() ? "-" : kode)
							+ (nama.isEmpty() ? "" : ", Nama: " + nama) + ") GAGAL: " + pesanRingkas(e));
				}
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/RabBulananImporter.java:254");
			}
		}

		final List<Workspace> workspacesIdBaru = new ArrayList<Workspace>();
		// FASE PENATAAN ID: pakai workspaceSource yang SEJAJAR dgn workspaces (bukan objects.get(i))
		// supaya baris gagal di fase-1 tidak menggeser pemetaan id (perbaikan bug misalign).
		for (int k = 0; k < workspaces.size(); k++) {
			Workspace workspace = workspaces.get(k);
			String[] src = workspaceSource.get(k);
			int excelRow = workspaceExcelRow.get(k).intValue();
			try {
				Long id = nilaiMinInDb + Long.parseLong(src[0].trim());
				Long parent = Long.parseLong(src[1].trim());
				if (!parent.equals(0L)) {
					parent = nilaiMinInDb + parent;
				} else {
					String s = (satuanKerja.getId() + "").length() >= "2000000001".length()
							? (satuanKerja.getId() + "").substring((satuanKerja.getId() + "").length() - 5)
							: (satuanKerja.getId() + "");

					parent = -(Long.MAX_VALUE
							- (Long.parseLong("" + RabUtil.DEFAULT_SATUAN_KERJA + tahunWorkspace + "" + s)));
				}

				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				String sql = "update rab.workspace set id = " + id + ", parent_id = " + parent + " where id = "
						+ workspace.getId();
				session.createSQLQuery(sql).executeUpdate();
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				session = HibernateUtil.currentNativeSession();
				Workspace newworkspace = (Workspace) session.createCriteria(Workspace.class)
						.add(Restrictions.or(Restrictions.eq("carryOver", true),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.idEq(id)).uniqueResult();
				if (session.isOpen()) {session.disconnect();session.close();}

				HibernateUtil.closeSession();
				if (newworkspace != null) {
					workspacesIdBaru.add(newworkspace);
				}
			} catch (Exception e2) {
				if (gagal != null) {
					gagal.add("Baris " + excelRow + " (Kode: " + workspace.getKode()
							+ ") GAGAL menata ulang ID: " + pesanRingkas(e2));
				}
				e2.printStackTrace(); ais.common.ErrorAuditUtil.record(e2, "auto-audit src/ais/action/master/rab/util/RabBulananImporter.java:304");
			}
		}
		workspaces = null;

		Runnable runnable = new Runnable() {

			private void getParentAkun(Long parentId, List<Akun> akuns) {
				Session session = HibernateUtil.currentNativeSession();
				Workspace workspace = (Workspace) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
						.add(Restrictions.eq("id", parentId)).uniqueResult();

				HibernateUtil.closeSession();
				if (workspace != null && (workspace != null && workspace.getAkun() != null)) {
					akuns.add(workspace.getAkun());
				} else if (workspace != null && akuns.size() == 0) {
					getParentAkun(workspace.getParentId(), akuns);
				}
			}

			@Override
			public void run() {
				try {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Common.tampilErrorJikaAdmin(e);
				}
				WorkspaceTreeModel workspaceTreeModel = new WorkspaceTreeModel(tahunWorkspace, revisi, satuanKerja,
						sumberDana);
				for (Workspace workspace : workspacesIdBaru) {
					if (workspace.getAkun() == null && workspaceTreeModel.isLeaf(workspace)) {
						List<Akun> akuns = new ArrayList<Akun>();
						getParentAkun(workspace.getParentId(), akuns);
						if (akuns.size() != 0) {
							Session session = HibernateUtil.currentNativeSession();
							workspace.setAkun(akuns.get(0));
							session.getTransaction().begin();
							session.update(workspace);
							session.getTransaction().commit();

							HibernateUtil.closeSession();
						}
					}
				}
							} finally {
					ais.database.hibernate.HibernateUtil.closeSession();
				}
			}
		};
		new Thread(runnable).start();

		return workspacesIdBaru;

	}
}
