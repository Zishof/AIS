package ais.action.master.sapto.util;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.Type;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;

public class SaptoGenerator {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void generateProfileMahasiswaDanLulusan_A_3_2_4(Session session, Label label, int tahun,
			List<List> datas) {

		String newTa = (tahun) + "/" + (tahun + 1);
		label.setValue("Loading data " + newTa);
		try {

			String sql = "select ";
			for (int i = tahun - 4; i <= tahun; i++) {
				sql += "sum(case when a.tahunlulus=" + i + " then 1 else 0 end) as kelulusan_" + i + ",\n";
			}

			sql += "1";

			sql += "from mahasiswa a\r\n" + "inner join jenjang b on (a.jenjang=b.id)\r\n"
					+ "where a.tahunlulus is not null and a.tahunlulus > a.tahunangkatan";

			System.out.println(sql);

			Object[] nn = (Object[]) session.createSQLQuery(sql).uniqueResult();

			int i = 0;
			for (int j = tahun - 4; j <= tahun; j++) {
				List sub = new ArrayList();
				sub.add("");
				sub.add("");
				sub.add("");
				sub.add(nn[i] == null ? 0 : Double.parseDouble(nn[i].toString()));
				i++;

				int jumlah = ((Number) session.createCriteria(ChecklistHasilPenilaianUmum.class)
						.setProjection(Projections.countDistinct("mahasiswa")).createAlias("mahasiswa", "mahasiswa")
						.createAlias("checklistPenilaianUmum", "checklistPenilaianUmum")
						.createAlias("checklistPenilaianUmum.grupChecklistPenilaianUmum", "grupChecklistPenilaianUmum")
						.add(Restrictions.eq("grupChecklistPenilaianUmum.diperuntukkan",
								GrupChecklistPenilaianUmum.UNTUK_ALUMNI))
						.add(Restrictions.eq("mahasiswa.tahunLulus", j)).uniqueResult()).intValue();

				sub.add(jumlah);

				datas.add(sub);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List generateProfileMahasiswaDanLulusan_A_3_2_2(Session session, Label label, String jenjang,
			int tahun, String semester) {
		List sub = new ArrayList();
		sub.add("");
		sub.add("");
		sub.add("");
		String newTa = (tahun) + "/" + (tahun + 1);
		label.setValue("Loading data " + newTa);
		try {

			String sql = "select\n";
			for (int i = tahun - 2; i <= tahun; i++) {
				sql += "(select\r\n" + "avg(a.tahunlulus-a.tahunangkatan) as ts\r\n" + "from mahasiswa a\r\n"
						+ "inner join jenjang b on (a.jenjang=b.id)\r\n"
						+ "where a.tahunlulus is not null and a.tahunlulus > a.tahunangkatan and a.tahunlulus=" + i
						+ " \n" + "and b.nama in (" + jenjang + ")) as rata_rata_kelulusan_" + i + ",\n";
			}

			for (int i = tahun - 2; i <= tahun; i++) {
				sql += "(select\r\n" + "avg(a.ipk) as ipk\r\n" + "from mahasiswa a \r\n"
						+ "inner join jenjang b on (a.jenjang=b.id)\r\n"
						+ "where a.tahunlulus is not null and a.tahunlulus > a.tahunangkatan and a.tahunlulus=" + i
						+ " and b.nama in (" + jenjang + ")) as ipk_kelulusan_" + i + ",";
			}

			sql += "1";

			System.out.println(sql);

			Object[] nn = (Object[]) session.createSQLQuery(sql).uniqueResult();

			for (int i = 0; i < 6; i++) {
				sub.add(nn[i] == null ? 0 : Double.parseDouble(nn[i].toString()));
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return sub;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List generateProfileMahasiswaDanLulusan_A_3_2_1(Session session, Label label, int tahunAngkatan,
			String jenjang, int jumlahTahun, int tahun, String program) {
		List sub = new ArrayList();
		sub.add("");
		sub.add("");
		String newTa = (tahunAngkatan) + "/" + (tahunAngkatan + 1);
		label.setValue("Loading data " + newTa);
		try {

			String sql = "select\n";

			for (int i = jumlahTahun; i >= 0; i--) {
				sql += "sum(case when to_number(tahunakademik,'9999') = " + (tahun - i)
						+ " and to_number(tahunakademik,'9999') >=" + tahunAngkatan + " and aa.status_mahasiswa="
						+ ConstantValues.AKTIF.getId() + " then 1 else 0 end) as ts" + i + ",\n";
			}

			sql += "\nsum(case when aa.status_mahasiswa=" + ConstantValues.LULUS.getId()
					+ " and to_number(tahunakademik,'9999') = " + tahun + " then 1 else 0 end) as lulus\n\n"
					+ "from (select aaa.mahasiswa,aaa.tahunakademik,max(aaa.status_mahasiswa) as status_mahasiswa,max(bbb.jurusan) as jurusan "
					+ "\n from history_status_mahasiswa aaa " + "\n inner join mahasiswa bbb on (bbb.id=aaa.mahasiswa) "
					+ "\n where aaa.semester>=1 " + "\n and bbb.merupakanpindahan=false "
					+ "\n and aaa.status_mahasiswa in (" + ConstantValues.AKTIF.getId() + ","
					+ ConstantValues.LULUS.getId() + ") and bbb.tahunangkatan=" + tahunAngkatan + "  "
					+ (program == null ? "" : " and bbb.program='" + program + "' ")
					+ "  group by mahasiswa,tahunakademik) aa " + "\n inner join jurusan c on (aa.jurusan=c.id) \n"
					+ "\n inner join jenjang d on (c.jenjang=d.id) \n" + "\n where d.nama in (" + jenjang + ")";

			System.out.println(sql);

			Object[] nn = (Object[]) session.createSQLQuery(sql).uniqueResult();

			for (int i = 0; i <= jumlahTahun; i++) {
				sub.add(nn[i] == null ? 0 : Double.parseDouble(nn[i].toString()));
			}

			sub.add(nn[jumlahTahun + 1] == null ? 0 : Double.parseDouble(nn[jumlahTahun + 1].toString()));

			System.out.println("sub =>" + sub + ", newTa => " + newTa);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return sub;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List generateProfileMahasiswaDanLulusanA_3_1_4(Session session, Label label, int tahunAngkatan,
			int jumlahTahun, int tahun, Jurusan jurusan, String program) {
		List sub = new ArrayList();
		sub.add("");
		sub.add("");
		String newTa = (tahunAngkatan) + "/" + (tahunAngkatan + 1);
		label.setValue("Loading data " + newTa);
		try {

			String sql = "select\n";

			for (int i = jumlahTahun; i >= 0; i--) {
				sql += "sum(case when to_number(tahunakademik,'9999') = " + (tahun - i)
						+ " and to_number(tahunakademik,'9999') >=" + tahunAngkatan + " and aa.status_mahasiswa="
						+ ConstantValues.AKTIF.getId() + " then 1 else 0 end) as ts" + i + ",\n";
			}

			sql += "\nsum(case when aa.status_mahasiswa=" + ConstantValues.LULUS.getId()
					+ " and to_number(tahunakademik,'9999') = " + tahun + " then 1 else 0 end) as lulus\n\n"
					+ "from (select aaa.mahasiswa,aaa.tahunakademik,max(aaa.status_mahasiswa) as status_mahasiswa,max(bbb.jurusan) as jurusan "
					+ "\n from history_status_mahasiswa aaa " + "\n inner join mahasiswa bbb on (bbb.id=aaa.mahasiswa) "
					+ "\n where aaa.semester>=1 " + "\n and bbb.merupakanpindahan=false "
					+ "\n and aaa.status_mahasiswa in (" + ConstantValues.AKTIF.getId() + ","
					+ ConstantValues.LULUS.getId() + ") and bbb.tahunangkatan=" + tahunAngkatan + " and bbb.jurusan="
					+ jurusan.getId() + " and bbb.program='" + program + "' group by mahasiswa,tahunakademik) aa";

			System.out.println(sql);

			System.out.println(sql);

			Object[] nn = (Object[]) session.createSQLQuery(sql).uniqueResult();

			sub.add(nn[0] == null ? 0 : Double.parseDouble(nn[0].toString()));
			sub.add(nn[1] == null ? 0 : Double.parseDouble(nn[1].toString()));
			sub.add(nn[2] == null ? 0 : Double.parseDouble(nn[2].toString()));

			sub.add(nn[3] == null ? 0 : Double.parseDouble(nn[3].toString()));
			sub.add(nn[4] == null ? 0 : Double.parseDouble(nn[4].toString()));
			sub.add(nn[5] == null ? 0 : Double.parseDouble(nn[5].toString()));
			sub.add(nn[6] == null ? 0 : Double.parseDouble(nn[6].toString()));
			sub.add(nn[7] == null ? 0 : Double.parseDouble(nn[7].toString()));

			System.out.println("sub =>" + sub + ", newTa => " + newTa);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return sub;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List generateProfileMahasiswaDanLulusan(Session session, Label label, int rowIndex, Jurusan jurusan,
			boolean danLulusan, String program) {
		List sub = new ArrayList();
		sub.add("");
		sub.add("");
		String newTa = (rowIndex) + "/" + (rowIndex + 1);
		label.setValue("Loading data " + newTa);
		try {

			Number kapasitas = (Number) session.createCriteria(KapasitasMahasiswaBaru.class)
					.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunAkademik", newTa))
					.setProjection(Projections.sum("jumlahTargetMahasiswaBaru")).uniqueResult();

			sub.add(kapasitas == null ? 0 : kapasitas.intValue());

			Criterion criterion = Restrictions.or(Restrictions.eq("prodi1", jurusan),
					Restrictions.eq("prodi2", jurusan));
			criterion = Restrictions.or(criterion, Restrictions.eq("prodi3", jurusan));
			criterion = Restrictions.or(criterion, Restrictions.eq("prodi4", jurusan));
			criterion = Restrictions.or(criterion, Restrictions.eq("prodi5", jurusan));

			Number n1 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
							: program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
									: Restrictions.eq("program", program))
					.add(criterion).add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(n1 == null ? 0 : n1.intValue());

			Number n2 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.eq("tahunAkademik", newTa))
					.setProjection(Projections.rowCount()).uniqueResult();

			sub.add(n2 == null ? 0 : n2.intValue());

			Number m1 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
							: Restrictions.eq("program", program))
					.add(Restrictions.isNull("statusKeluar")).createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa.pindahan"),
							Restrictions.eq("statusAwalMahasiswa.pindahan", false)))
					.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunangkatan", rowIndex))
					.setProjection(Projections.rowCount()).uniqueResult();

			sub.add(m1 == null ? 0 : m1.intValue());

			Number m2 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
							: Restrictions.eq("program", program))
					.add(Restrictions.isNull("statusKeluar")).createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(m2 == null ? 0 : m2.intValue());

			Number t1 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
							: Restrictions.eq("program", program))
					.add(Restrictions.isNull("statusKeluar")).createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa.pindahan"),
							Restrictions.eq("statusAwalMahasiswa.pindahan", false)))
					.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.le("tahunangkatan", rowIndex))
					.setProjection(Projections.rowCount()).uniqueResult();

			sub.add(t1 == null ? 0 : t1.intValue());

			Number t2 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
							: Restrictions.eq("program", program))
					.add(Restrictions.isNull("statusKeluar")).createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.le("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(t2 == null ? 0 : t2.intValue());

			if (danLulusan) {

				Number l1 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
								: Restrictions.eq("program", program))
						.createAlias("statusKeluar", "statusKeluar")
						.add(Restrictions.ilike("statusKeluar.nama", "Lulus", MatchMode.EXACT))
						.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
						.add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa.pindahan"),
								Restrictions.eq("statusAwalMahasiswa.pindahan", false)))
						.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunLulus", rowIndex))
						.setProjection(Projections.rowCount()).uniqueResult();

				sub.add(l1 == null ? 0 : l1.intValue());

				Number l2 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
								: Restrictions.eq("program", program))
						.createAlias("statusKeluar", "statusKeluar")
						.add(Restrictions.ilike("statusKeluar.nama", "Lulus", MatchMode.EXACT))
						.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
						.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true))
						.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunLulus", rowIndex))
						.setProjection(Projections.rowCount()).uniqueResult();

				sub.add(l2 == null ? 0 : l2.intValue());

				Object[] nn = (Object[]) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(program.equalsIgnoreCase("Non Reguler") ? Restrictions.ne("program", "Reguler")
								: Restrictions.eq("program", program))
						.createAlias("statusKeluar", "statusKeluar")
						.add(Restrictions.ilike("statusKeluar.nama", "Lulus", MatchMode.EXACT))
						.add(Restrictions.eq("jurusan", jurusan)).add(Restrictions.eq("tahunLulus", rowIndex))
						.setProjection(Projections.projectionList().add(Projections.min("ipk"))
								.add(Projections.avg("ipk")).add(Projections.max("ipk")).add(Projections.sqlProjection(
										"(sum(case when this_.ipk < 2.75 then 1 else 0 end)*100.0/count(*)) as dibawah,"
												+ "(sum(case when this_.ipk >= 2.75 and this_.ipk <= 3.5 then 1 else 0 end)*100.0/count(*)) as tengah,"
												+ "(sum(case when this_.ipk > 3.5 then 1 else 0 end)*100.0/count(*)) as diatas",
										new String[] { "dibawah", "tengah", "diatas" },
										new Type[] { org.hibernate.type.StandardBasicTypes.DOUBLE, org.hibernate.type.StandardBasicTypes.DOUBLE,
												org.hibernate.type.StandardBasicTypes.DOUBLE })))
						.uniqueResult();

				sub.add(nn[0] == null ? 0 : Double.parseDouble(nn[0].toString()));
				sub.add(nn[1] == null ? 0 : Double.parseDouble(nn[1].toString()));
				sub.add(nn[2] == null ? 0 : Double.parseDouble(nn[2].toString()));

				sub.add(nn[3] == null ? 0 : Double.parseDouble(nn[3].toString()));
				sub.add(nn[4] == null ? 0 : Double.parseDouble(nn[4].toString()));
				sub.add(nn[5] == null ? 0 : Double.parseDouble(nn[5].toString()));
			}

			System.out.println("sub =>" + sub + ", newTa => " + newTa);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return sub;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List generateProfileMahasiswa(Session session, Label label, int rowIndex, String... jenjang) {

		List sub = new ArrayList();
		sub.add("");
		sub.add("");
		String newTa = (rowIndex) + "/" + (rowIndex + 1);
		label.setValue("Loading data " + newTa);
		try {

			Number kapasitas = (Number) session.createCriteria(KapasitasMahasiswaBaru.class)
					.createAlias("jurusan", "jurusan").createAlias("jurusan.jenjang", "jenjang")
					.add(Restrictions.in("jenjang.nama", jenjang)).add(Restrictions.eq("tahunAkademik", newTa))
					.setProjection(Projections.sum("jumlahTargetMahasiswaBaru")).uniqueResult();

			sub.add(kapasitas == null ? 0 : kapasitas.intValue());

			Criterion criterion = Restrictions.or(Restrictions.in("jenjang1.nama", jenjang),
					Restrictions.in("jenjang2.nama", jenjang));
			criterion = Restrictions.or(criterion, Restrictions.in("jenjang3.nama", jenjang));
			criterion = Restrictions.or(criterion, Restrictions.in("jenjang4.nama", jenjang));
			criterion = Restrictions.or(criterion, Restrictions.in("jenjang5.nama", jenjang));

			Number n1 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

					.createAlias("prodi1", "prodi1", Criteria.LEFT_JOIN)
					.createAlias("prodi2", "prodi2", Criteria.LEFT_JOIN)
					.createAlias("prodi3", "prodi3", Criteria.LEFT_JOIN)
					.createAlias("prodi4", "prodi4", Criteria.LEFT_JOIN)
					.createAlias("prodi5", "prodi5", Criteria.LEFT_JOIN)

					.createAlias("prodi1.jenjang", "jenjang1", Criteria.LEFT_JOIN)
					.createAlias("prodi2.jenjang", "jenjang2", Criteria.LEFT_JOIN)
					.createAlias("prodi3.jenjang", "jenjang3", Criteria.LEFT_JOIN)
					.createAlias("prodi4.jenjang", "jenjang4", Criteria.LEFT_JOIN)
					.createAlias("prodi5.jenjang", "jenjang5", Criteria.LEFT_JOIN)

					.add(criterion)

					.add(Restrictions.eq("tahunAkademik", newTa)).setProjection(Projections.rowCount()).uniqueResult();

			sub.add(n1 == null ? 0 : n1.intValue());

			Number n2 = (Number) session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("prodiLulus", "prodiLulus").createAlias("prodiLulus.jenjang", "jenjang")
					.add(Restrictions.in("jenjang.nama", jenjang)).add(Restrictions.eq("tahunAkademik", newTa))
					.setProjection(Projections.rowCount()).uniqueResult();

			sub.add(n2 == null ? 0 : n2.intValue());

			Number m1 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa.pindahan"),
							Restrictions.eq("statusAwalMahasiswa.pindahan", false)))

					.createAlias("jurusan", "jurusan").createAlias("jurusan.jenjang", "jenjang")
					.add(Restrictions.in("jenjang.nama", jenjang))

					.add(Restrictions.eq("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(m1 == null ? 0 : m1.intValue());

			Number m2 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true)).createAlias("jurusan", "jurusan")
					.createAlias("jurusan.jenjang", "jenjang").add(Restrictions.in("jenjang.nama", jenjang))

					.add(Restrictions.eq("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(m2 == null ? 0 : m2.intValue());

			Number t1 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.or(Restrictions.isNull("statusAwalMahasiswa.pindahan"),
							Restrictions.eq("statusAwalMahasiswa.pindahan", false)))
					.createAlias("jurusan", "jurusan").createAlias("jurusan.jenjang", "jenjang")
					.add(Restrictions.in("jenjang.nama", jenjang))

					.add(Restrictions.le("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(t1 == null ? 0 : t1.intValue());

			Number t2 = (Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("statusAwalMahasiswa", "statusAwalMahasiswa")
					.add(Restrictions.eq("statusAwalMahasiswa.pindahan", true)).createAlias("jurusan", "jurusan")
					.createAlias("jurusan.jenjang", "jenjang").add(Restrictions.in("jenjang.nama", jenjang))

					.add(Restrictions.le("tahunangkatan", rowIndex)).setProjection(Projections.rowCount())
					.uniqueResult();

			sub.add(t2 == null ? 0 : t2.intValue());

			System.out.println("sub =>" + sub + ", newTa => " + newTa);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return sub;
	}

}
