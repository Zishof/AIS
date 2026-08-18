package ais.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.ChecklistBaruPenilaianGuruOlehSiswa;
import ais.database.model.sekolah.ChecklistPenilaianGuru;
import ais.database.model.sekolah.Siswa;

public class ChecklistPenilaianGuruHelper {

	private ChecklistPenilaianGuruHelper() {
	}

	@SuppressWarnings("unchecked")
	public static List<Long> getJadwalPelajaranSiswa(Siswa siswa, String tahunAjaran, String ganjilGenap) {
		List<Long> result = new ArrayList<Long>();
		if (siswa == null || siswa.getId() == null) {
			return result;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			StringBuilder sql = new StringBuilder();
			sql.append("select jp.id from sekolah.jadwal_pelajaran jp ");
			sql.append(" where jp.id is not null ");
			if (tahunAjaran != null && tahunAjaran.trim().length() > 0) {
				sql.append(" and jp.tahun_ajaran = :tahunAjaran ");
			}
			if (ganjilGenap != null && ganjilGenap.trim().length() > 0) {
				sql.append(" and (jp.semester % 2) = :semesterMod ");
			}
			sql.append(" and ( ");
			sql.append(" jp.kelas_id in (select a.kelas_id from sekolah.kelas_punya_siswa a ");
			sql.append(" where a.siswa_id = :siswaId and a.kelas_id is not null and (a.aktif=true or a.aktif is null) group by a.kelas_id) ");
			sql.append(" or jp.kelas_les_siswa in (select b.kelas_id from sekolah.kelas_les_punya_siswa b ");
			sql.append(" where b.siswa_id = :siswaId and b.kelas_id is not null and (b.aktif=true or b.aktif is null) group by b.kelas_id) ");
			sql.append(" ) group by jp.id order by jp.id ");

			org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString());
			query.setParameter("siswaId", siswa.getId());
			if (tahunAjaran != null && tahunAjaran.trim().length() > 0) {
				query.setParameter("tahunAjaran", tahunAjaran);
			}
			if (ganjilGenap != null && ganjilGenap.trim().length() > 0) {
				query.setParameter("semesterMod", Perkuliahan.GANJIL.equals(ganjilGenap) ? Integer.valueOf(1)
						: Integer.valueOf(0));
			}

			List<Object> rows = query.list();
			if (rows != null) {
				for (Object row : rows) {
					if (row instanceof Number) {
						result.add(Long.valueOf(((Number) row).longValue()));
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianGuruHelper.java:73");
				}
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static boolean checkStatusChecklistGuru(Siswa siswa, String ganjilGenap, String tahunAjaran) {
		if (siswa == null || siswa.getId() == null) {
			return false;
		}

		Session session = null;
		try {
			List<Long> jadwalPelajaranIds = getJadwalPelajaranSiswa(siswa, tahunAjaran, ganjilGenap);
			if (jadwalPelajaranIds == null || jadwalPelajaranIds.isEmpty()) {
				return false;
			}

			session = HibernateUtil.getSessionFactory().openSession();
			List<Long> checklistIds = session.createCriteria(ChecklistPenilaianGuru.class)
					.createAlias("grupChecklistPenilaianGuru", "grupChecklistPenilaianGuru")
					.createAlias("grupChecklistPenilaianGuru.angketPenilaianGuru", "angketPenilaianGuru")
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianGuru.aktif", true),
							Restrictions.isNull("grupChecklistPenilaianGuru.aktif")))
					.add(Restrictions.or(Restrictions.eq("angketPenilaianGuru.untukSiswa", true),
							Restrictions.isNull("angketPenilaianGuru.untukSiswa")))
					.setProjection(Projections.groupProperty("id")).list();

			if (checklistIds == null || checklistIds.isEmpty()) {
				return false;
			}

			String guruSql = "select jp.id, jp.guru_id, jp.guru2_id, jp.guru3_id, jp.guru4_id, jp.guru5_id, "
					+ "jp.guru6_id, jp.guru7_id, jp.guru8_id, jp.guru9_id, jp.guru10_id, jp.guru11_id, jp.guru12_id "
					+ "from sekolah.jadwal_pelajaran jp where jp.id in (:jadwalPelajaranIds)";
			List<Object[]> jadwalGuruRows = session.createSQLQuery(guruSql)
					.setParameterList("jadwalPelajaranIds", jadwalPelajaranIds).list();
			if (jadwalGuruRows == null || jadwalGuruRows.isEmpty()) {
				return false;
			}

			Set<String> requiredKeys = new HashSet<String>();
			for (Object[] row : jadwalGuruRows) {
				if (row == null || row.length == 0 || !(row[0] instanceof Number)) {
					continue;
				}
				Long jadwalId = Long.valueOf(((Number) row[0]).longValue());
				Set<Long> guruIds = new HashSet<Long>();
				for (int i = 1; i < row.length; i++) {
					if (row[i] instanceof Number) {
						guruIds.add(Long.valueOf(((Number) row[i]).longValue()));
					}
				}
				for (Long guruId : guruIds) {
					for (Long checklistId : checklistIds) {
						requiredKeys.add(jadwalId + "_" + guruId + "_" + checklistId);
					}
				}
			}
			if (requiredKeys.isEmpty()) {
				return false;
			}

			List<ChecklistBaruPenilaianGuruOlehSiswa> saved = session
					.createCriteria(ChecklistBaruPenilaianGuruOlehSiswa.class)
					.createAlias("jadwalPelajaran", "jadwalPelajaran")
					.add(Restrictions.eq("siswa", siswa))
					.add(Restrictions.in("jadwalPelajaran.id", jadwalPelajaranIds)).list();
			Set<String> savedKeys = new HashSet<String>();
			if (saved != null) {
				for (ChecklistBaruPenilaianGuruOlehSiswa data : saved) {
					if (data == null || data.getJadwalPelajaran() == null || data.getGuru() == null) {
						continue;
					}
					Long jadwalId = data.getJadwalPelajaran().getId();
					Long guruId = data.getGuru().getId();
					for (Object[] nilai : data.ambilValue()) {
						if (nilai != null && nilai.length > 0 && nilai[0] instanceof Long) {
							savedKeys.add(jadwalId + "_" + guruId + "_" + nilai[0]);
						}
					}
				}
			}

			return !savedKeys.containsAll(requiredKeys);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return false;
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianGuruHelper.java:168");
				}
			}
		}
	}
}
