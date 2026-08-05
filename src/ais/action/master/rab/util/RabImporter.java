package ais.action.master.rab.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;
import ais.database.model.rab.HasilSatuan;
import ais.database.model.rab.Satuan;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.SumberDana;
import ais.database.model.rab.Workspace;

public class RabImporter {

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
	 * Import anggaran TAHUNAN dari xlsx. {@code gagal}/{@code berhasil} (boleh null) diisi pesan
	 * per-baris agar pemanggil bisa menampilkan laporan jelas (mana sukses & mana gagal + sebab).
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
			String[] objs = new String[12];
			for (int j = 0; j <= 11; j++) {
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

		for (String[] strings : objects) {
			Session session = HibernateUtil.currentNativeSession();
			String labSatuan1 = strings[5] == null ? "" : strings[5].trim();
			Satuan satuan1 = (Satuan) session.createCriteria(Satuan.class)
					.add(Restrictions.ilike("nama", labSatuan1, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
			String labSatuan2 = strings[8] == null ? "" : strings[8].trim();
			Satuan satuan2 = (Satuan) session.createCriteria(Satuan.class)
					.add(Restrictions.ilike("nama", labSatuan2, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

			String labSatuan3 = strings[10] == null ? "" : strings[10].trim();

			if (satuan1 == null) {
				satuan1 = new Satuan();
				satuan1.setNama(labSatuan1);
				satuan1.setKeterangan(labSatuan1);
				session.getTransaction().begin();
				session.save(satuan1);
				session.getTransaction().commit();
			}

			if (satuan2 == null) {
				satuan2 = new Satuan();
				satuan2.setNama(labSatuan2);
				satuan2.setKeterangan(labSatuan2);
				session.getTransaction().begin();
				session.save(satuan2);
				session.getTransaction().commit();
			}

			HasilSatuan hasilSatuan = (HasilSatuan) session.createCriteria(HasilSatuan.class)
					.add(Restrictions.eq("satuan1", satuan1)).add(Restrictions.eq("satuan2", satuan2)).setMaxResults(1)
					.uniqueResult();
			if (hasilSatuan == null) {
				hasilSatuan = new HasilSatuan();
				hasilSatuan.setKeterangan(labSatuan3);
				hasilSatuan.setLabel(labSatuan3);
				hasilSatuan.setSatuan1(satuan1);
				hasilSatuan.setSatuan2(satuan2);
				session.getTransaction().begin();
				session.save(hasilSatuan);
				session.getTransaction().commit();
			}

			HibernateUtil.closeSession();
		}

		Long nilaiMin = Long.MIN_VALUE + 2000000000000000000L;
		Long nilaiMax = nilaiMin + 1000000000000000000L;

		Session session = HibernateUtil.currentNativeSession();
		Long nilaiMinInDb = (Long) session.createCriteria(Workspace.class).add(Restrictions.or(Restrictions.eq("carryOver", true),Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))).add(Restrictions.gt("id", nilaiMin))
				.add(Restrictions.lt("id", nilaiMax)).setProjection(Projections.max("id")).uniqueResult();

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
				String kodeAkun = null;
				try {
					if (strings[13] != null && strings[13].trim().isEmpty()) {
						kodeAkun = strings[13] == null ? "" : strings[13].trim();
					}
				} catch (Exception eAkun) { ais.common.ErrorAuditUtil.record(eAkun, "auto-audit(empty-catch) src/ais/action/master/rab/util/RabImporter.java:152");
				}

				Double volAsli = strings[9] == null || strings[9].trim().equals("") ? 1.0
						: RabBulananImporter.parseNilai(strings[9]);
				Double q1 = strings[4] == null || strings[4].trim().equals("") ? 1.0
						: RabBulananImporter.parseNilai(strings[4]);
				Double q2 = strings[7] == null || strings[7].trim().equals("") ? (q1.equals(1.0) ? volAsli : 1.0)
						: RabBulananImporter.parseNilai(strings[7]);

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
			String labSatuan1 = strings[5] == null ? "" : strings[5].trim();
			Satuan satuan1 = (Satuan) session.createCriteria(Satuan.class)
					.add(Restrictions.ilike("nama", labSatuan1, MatchMode.EXACT)).setMaxResults(1).uniqueResult();
			String labSatuan2 = strings[8] == null ? "" : strings[8].trim();
			Satuan satuan2 = (Satuan) session.createCriteria(Satuan.class)
					.add(Restrictions.ilike("nama", labSatuan2, MatchMode.EXACT)).setMaxResults(1).uniqueResult();

			Akun akun = (Akun) session.createCriteria(Akun.class)
					.add(Restrictions.eq("kode", kodeAkun == null ? kode : kodeAkun)).setMaxResults(1).uniqueResult();

			HibernateUtil.closeSession();

			String labSatuan3 = strings[10] == null ? "" : strings[10].trim();
			Double volume = q1 * q2;
			Double hargaSatuan = strings[11] == null || strings[11].trim().equals("") ? 0.0
					: RabBulananImporter.parseNilai(strings[11]);
			Double hargaTotal = volume * hargaSatuan;

			Workspace workspace = new Workspace();
			workspace.setAkun(akun);
			workspace.setCopyForm(null);
			workspace.setDeep(null);
			workspace.setDurasi(0);
			workspace.setHargaSatuan(hargaSatuan);
			workspace.setHargaTotal(hargaTotal);
			workspace.setJenisWorkspace(null);
			workspace.setJmlDipakai(0L);
			workspace.setJmlWaktu(q2);
			workspace.setKeterangan("");
			workspace.setKode(kode);
			workspace.setLeaf(false);
			workspace.setMerupakanHasilCopy(false);
			workspace.setMulai(null);
			workspace.setNama(nama);
			workspace.setParentId(-1L);
			workspace.setPersenKomplit(0.0);
			workspace.setQty(q1);
			workspace.setRealisasiTotal(0.0);
			workspace.setRevisi(revisi);
			workspace.setSatuan(satuan1);
			workspace.setSatuan1(satuan2);
			workspace.setSatuanKerja(satuanKerja);
			workspace.setSatuanVolume(labSatuan3);
			workspace.setSelesai(null);
			workspace.setSumberDana(sumberDana);
			workspace.setTahunWorkspace(tahunWorkspace);
			workspace.setUnitOrganisasi(null);
			workspace.setVolume(volume);

			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(workspace);
			session.getTransaction().commit();

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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/rab/util/RabImporter.java:244");
			}
		}

		final List<Workspace> workspacesIdBaru = new ArrayList<Workspace>();
		// Penataan id memakai workspaceSource yang SEJAJAR dgn workspaces (perbaikan bug misalign).
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

				HibernateUtil.closeSession();
				if (newworkspace != null) {
					workspacesIdBaru.add(newworkspace);
				}
			} catch (Exception e2) {
				if (gagal != null) {
					gagal.add("Baris " + excelRow + " (Kode: " + workspace.getKode()
							+ ") GAGAL menata ulang ID: " + pesanRingkas(e2));
				}
				e2.printStackTrace(); ais.common.ErrorAuditUtil.record(e2, "auto-audit src/ais/action/master/rab/util/RabImporter.java:292");
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
