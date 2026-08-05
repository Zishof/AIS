package ais.action.master.epsbed;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;

public class CommonEpsbed extends Common {

	public static SimpleDateFormat dateFormatEpsbed = new SimpleDateFormat("yyyyMMdd", new Locale("in", "ID"));

	public static String getTahunSemesterPelaporan(String tahunAkademik, String ganjilgenap) {
		String tahunSemesterPelaporan = "";
		String tahun = tahunAkademik.split("/")[0];
		String semesterPelaporan = ganjilgenap.equals(Perkuliahan.GANJIL) ? "1" : "2";
		tahunSemesterPelaporan = tahun.toString() + semesterPelaporan;
		return tahunSemesterPelaporan;
	}

	public static Integer hitungJumlahPeminat(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.or(Restrictions.eq("prodi1", jurusan), Restrictions.eq("prodi2", jurusan)))
				.uniqueResult()).intValue();
		return jumlah;
	}

	public static Integer hitungJumlahLulus(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).uniqueResult()).intValue();
		return jumlah;
	}

	public static Integer hitungJumlahDaftarUlang(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.isNotNull("nim")).uniqueResult())
						.intValue();
		return jumlah;
	}

	public static Integer hitungJumlahMundur(Jurusan jurusan, String tahunAkademik) {
		Integer jumlah = 0;
		String tahun = tahunAkademik.split("/")[0];
		jumlah = ((Number) HibernateUtil.currentSession().createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.rowCount()).add(Restrictions.eq("tahun", Integer.parseInt(tahun)))
				.add(Restrictions.eq("prodiLulus", jurusan)).add(Restrictions.isNull("nim")).uniqueResult()).intValue();
		return jumlah;
	}

}
