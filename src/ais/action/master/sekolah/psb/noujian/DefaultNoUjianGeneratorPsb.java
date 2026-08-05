package ais.action.master.sekolah.psb.noujian;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonPSB;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB;
import ais.database.model.sekolah.RuangPSB;
import ais.ui.util.MyMessageboxConfig;

public class DefaultNoUjianGeneratorPsb implements NoUjianGeneratorPsb {

	@Override
	public String generateNoUjian(CalonSiswa calonSiswa) throws Exception {
		return generateNoUjian(calonSiswa, new ArrayList<String>());
	}

	@Override
	public String generateNoUjian(CalonSiswa calonSiswa, List<String> jumlahPengecualian) throws Exception {

		if (calonSiswa.getNoUjian() != null && !calonSiswa.getNoUjian().trim().isEmpty()) {
			return calonSiswa.getNoUjian().trim();
		}

		Session session = HibernateUtil.currentSession();
		Long idmin = (Long) session.createCriteria(RuangPSB.class).createAlias("ujianPSB", "ujianPSB")
				.add(Restrictions.eq("gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPSB.gelombangPendaftaranPsb", calonSiswa.getGelombangPendaftaranPsb()))
				.setProjection(Projections.min("id")).uniqueResult();

		if (idmin == null) {
			MyMessageboxConfig.show(
					"Kuota / Ruangan ujian untuk gelombang " + calonSiswa.getGelombangPendaftaranPsb()
							+ " tahun penerimaan siswa baru " + calonSiswa.getGelombangPendaftaranPsb()
							+ " tidak ditemukan atau sudah penuh",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		RuangPSB ruangSelected = (RuangPSB) session.createCriteria(RuangPSB.class).add(Restrictions.idEq(idmin))
				.uniqueResult();

		Number t = ((Number) (session.createCriteria(RuangGelombangPendaftaranPsbPSB.class)
				.createAlias("calonSiswa", "calonSiswa").add(Restrictions.ne("calonSiswa.noUjian", ""))
				.add(Restrictions.isNotNull("calonSiswa.noUjian")).add(Restrictions.eq("ruangPSB", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult())).intValue();
		int isiRuang = t == null ? 0 : t.intValue();
		String noUjianFinal = "";

		if (isiRuang < ruangSelected.getKapasitasRuangan()) {

			// String noUjian = "00000000000000000000000000000000000000" +
			// calonSiswa.getId();
			String digitSatuDanDua = calonSiswa.getTahunMasuk().toString().substring(2, 4);

			Integer jumlahIncrements = 8;
			try {
				jumlahIncrements = Integer
						.parseInt(Common.getKonfigurasi("jumlah_increments_no_ujian_psb", "8").getNilai());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			int penambahan = jumlahPengecualian.size();

			Number count = ((Number) session
					.createSQLQuery(
							"select max(to_number(substr(noujian,5),'99999999999999')) from sekolah.calon_siswa where noujian != '' and noujian is not null and substr(noujian,5)!=''")
					.uniqueResult());
			String noUjian = "00000000000" + (((count == null ? 0 : count.intValue()) + 1) + penambahan);
			System.out.println("noUjian => " + noUjian);
			noUjianFinal = digitSatuDanDua + noUjian.substring(noUjian.length() - jumlahIncrements, noUjian.length());

			session.refresh(calonSiswa);
			calonSiswa.setNoUjian(noUjianFinal);
			Common.refreshUpdate(session, calonSiswa);

			System.out.println("noUjianFinal => " + noUjianFinal);

		} else {
			MyMessageboxConfig.show("Kuota / Ruangan " + ruangSelected + " telah penuh", "Informasi",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}

		Integer count = ((Number) session.createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb")).add(Restrictions.eq("noUjian", noUjianFinal))
				.setProjection(Projections.rowCount()).uniqueResult()).intValue();

		if (!count.equals(0)) {
			jumlahPengecualian.add(noUjianFinal);
			return generateNoUjian(calonSiswa, jumlahPengecualian);
		} else {

			CommonPSB.dapatkanRuangUjian(calonSiswa);

			return noUjianFinal;
		}

	}

}
