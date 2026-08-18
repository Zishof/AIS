package ais.common;

import java.util.List;

import org.hibernate.Query;

import ais.database.hibernate.HibernateUtil;


public class CommonDBQuery {

	public static double getTotalAlokasi(String satker, String tahun) {
		StringBuilder sBuff = new StringBuilder("SELECT sum( akun_alokasi ) FROM t_akun_rencana ");
		sBuff.append("WHERE id_m_satuan_kerja = :satker AND tahun = :tahun ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("satker", satker);
		oQuery.setParameter("tahun", tahun);
		Double iResult = (Double) oQuery.uniqueResult();
		
		return (iResult == null) ? 0.0 : iResult.doubleValue();
	}
	
	public static List<?> getTotalRealisasi(String satker, String tahun) {
		StringBuilder sBuff = new StringBuilder("SELECT a.bulan, sum( a.akun_realisasi ) ");
		sBuff.append("FROM t_akun_realisasi a INNER JOIN t_akun_rencana b ON a.id_t_akun_rencana = b.id_t_akun_rencana ");
		sBuff.append("WHERE b.id_m_satuan_kerja = :satker AND b.tahun = :tahun ");
		sBuff.append("GROUP BY a.bulan ORDER BY a.bulan ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("satker", satker);
		oQuery.setParameter("tahun", tahun);
		
		return oQuery.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}

	public static List<?> getTotalAnggaran(String satker, String tahun) {
		StringBuilder sBuff = new StringBuilder("SELECT bulan, sum( anggaran ) FROM t_jadwal_penyerapan ");
		sBuff.append("WHERE id_m_satuan_kerja = :satker AND tahun = :tahun ");
		sBuff.append("GROUP BY bulan ORDER BY bulan ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("satker", satker);
		oQuery.setParameter("tahun", tahun);
		
		return oQuery.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}
	
	public static String getMonthName(String monthCode) {
		StringBuilder sBuff = new StringBuilder("SELECT code_name FROM ref_code_decode ");
		sBuff.append("WHERE code_value=:monthCode and code_group_id=1 ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("monthCode", monthCode);
		String sResult = (String) oQuery.uniqueResult();
		
		return sResult;
	}

}
