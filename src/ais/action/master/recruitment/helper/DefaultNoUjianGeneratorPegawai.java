package ais.action.master.recruitment.helper;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.common.CommonPegawai;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.recruitment.RuangGelombangPendaftaranPegawaiPegawai;
import ais.database.model.recruitment.RuangPegawai;
import ais.ui.util.MyMessageboxConfig;

public class DefaultNoUjianGeneratorPegawai implements NoUjianGeneratorPegawai {

	private static final int MAX_ATTEMPT = 10000;

	@Override
	public String generateNoUjian(CalonPegawai calonPegawai) throws Exception {
		return generateNoUjian(calonPegawai, new ArrayList<String>());
	}

	@Override
	public String generateNoUjian(CalonPegawai calonPegawai, List<String> jumlahPengecualian) throws Exception {
		if (calonPegawai == null) {
			return "";
		}
		if (calonPegawai.getNoUjian() != null && !calonPegawai.getNoUjian().trim().isEmpty()) {
			return calonPegawai.getNoUjian().trim();
		}
		List<String> pengecualian = jumlahPengecualian == null ? new ArrayList<String>() : jumlahPengecualian;
		Session session = HibernateUtil.currentSession();
		Long idmin = (Long) session.createCriteria(RuangPegawai.class).createAlias("ujianPegawai", "ujianPegawai")
				.add(Restrictions.eq("gelombangPendaftaranPegawai", calonPegawai.getGelombangPendaftaranPegawai()))
				.add(Restrictions.eq("penuh", 0))
				.add(Restrictions.eq("ujianPegawai.gelombangPendaftaranPegawai",
						calonPegawai.getGelombangPendaftaranPegawai()))
				.setProjection(Projections.min("id")).uniqueResult();
		if (idmin == null) {
			MyMessageboxConfig.show(
					"Kuota / Ruangan ujian untuk gelombang " + calonPegawai.getGelombangPendaftaranPegawai()
							+ " tidak ditemukan atau sudah penuh",
					"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return "";
		}
		RuangPegawai ruangSelected = (RuangPegawai) session.createCriteria(RuangPegawai.class)
				.add(Restrictions.idEq(idmin)).uniqueResult();
		if (ruangSelected == null || !ruangMasihTersedia(session, ruangSelected)) {
			MyMessageboxConfig.show("Kuota / Ruangan ujian telah penuh", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return "";
		}
		String noUjianFinal = buatNomorUjianUnik(session, pengecualian);
		if (noUjianFinal == null || noUjianFinal.trim().isEmpty()) {
			return "";
		}
		session.refresh(calonPegawai);
		calonPegawai.setNoUjian(noUjianFinal);
		Common.refreshUpdate(session, calonPegawai);
		CommonPegawai.dapatkanRuangUjian(calonPegawai);
		return noUjianFinal;
	}

	private boolean ruangMasihTersedia(Session session, RuangPegawai ruangSelected) {
		Number total = (Number) session.createCriteria(RuangGelombangPendaftaranPegawaiPegawai.class)
				.createAlias("calonPegawai", "calonPegawai").add(Restrictions.ne("calonPegawai.noUjian", ""))
				.add(Restrictions.isNotNull("calonPegawai.noUjian")).add(Restrictions.eq("ruangPegawai", ruangSelected))
				.setProjection(Projections.rowCount()).uniqueResult();
		int isiRuang = total == null ? 0 : total.intValue();
		return isiRuang < ruangSelected.getKapasitasRuangan();
	}

	private String buatNomorUjianUnik(Session session, List<String> pengecualian) {
		int digit = ambilJumlahDigit();
		Number max = (Number) session.createSQLQuery(
				"select max(to_number(substr(noujian,5),'99999999999999')) from calon_pegawai where noujian != '' and noujian is not null and substr(noujian,5)!=''")
				.uniqueResult();
		int dasar = max == null ? 0 : max.intValue();
		for (int attempt = 0; attempt < MAX_ATTEMPT; attempt++) {
			String noUjian = formatNomor(dasar + pengecualian.size() + attempt + 1, digit);
			if (pengecualian.contains(noUjian)) {
				continue;
			}
			Number count = (Number) session.createCriteria(CalonPegawai.class).add(Restrictions.eq("noUjian", noUjian))
					.setProjection(Projections.rowCount()).uniqueResult();
			if (count == null || count.intValue() == 0) {
				return noUjian;
			}
			pengecualian.add(noUjian);
		}
		return "";
	}

	private int ambilJumlahDigit() {
		try {
			return Integer.parseInt(Common.getKonfigurasi("jumlah_increments_no_ujian_pegawai", "8").getNilai());
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return 8;
		}
	}

	private String formatNomor(int nomor, int digit) {
		String hasil = "00000000000000000000" + nomor;
		return hasil.substring(hasil.length() - digit);
	}
}
