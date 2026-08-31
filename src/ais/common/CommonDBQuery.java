package ais.common;

import java.util.List;

import org.hibernate.Query;

import ais.database.hibernate.HibernateUtil;


/**
 * Kumpulan query native SQL statis untuk kebutuhan pelaporan anggaran/akunting (modul
 * alokasi-realisasi anggaran satuan kerja) di AIS. Seluruh method di kelas ini bersifat
 * stateless, memakai {@link HibernateUtil#currentSession()} untuk membuka query native SQL
 * (bukan HQL) dengan parameter bernama ({@code satker}, {@code tahun}, dsb.), dan mengembalikan
 * hasil mentah tanpa mapping ke entity Hibernate — cocok dipakai langsung sebagai sumber data
 * grafik/laporan ringkasan.
 *
 * <p>
 * Nama tabel yang diakses ({@code t_akun_rencana}, {@code t_akun_realisasi},
 * {@code t_jadwal_penyerapan}, {@code ref_code_decode}) mengindikasikan domain perencanaan dan
 * realisasi anggaran per satuan kerja (satker) dan tahun anggaran, khas modul akunting/keuangan
 * instansi pemerintah/pendidikan.
 * </p>
 */
public class CommonDBQuery {

	/**
	 * Menghitung total alokasi anggaran (kolom {@code akun_alokasi} pada
	 * {@code t_akun_rencana}) untuk satu satuan kerja pada satu tahun anggaran.
	 *
	 * @param satker id satuan kerja
	 * @param tahun  tahun anggaran
	 * @return total alokasi; {@code 0.0} bila tidak ada baris yang cocok (hasil query
	 *         {@code null})
	 */
	public static double getTotalAlokasi(String satker, String tahun) {
		StringBuilder sBuff = new StringBuilder("SELECT sum( akun_alokasi ) FROM t_akun_rencana ");
		sBuff.append("WHERE id_m_satuan_kerja = :satker AND tahun = :tahun ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("satker", satker);
		oQuery.setParameter("tahun", tahun);
		Double iResult = (Double) oQuery.uniqueResult();
		
		return (iResult == null) ? 0.0 : iResult.doubleValue();
	}
	
	/**
	 * Mengambil total realisasi anggaran (kolom {@code akun_realisasi} pada
	 * {@code t_akun_realisasi}, dijoin ke {@code t_akun_rencana}) yang dikelompokkan per bulan,
	 * untuk satu satuan kerja pada satu tahun anggaran, diurutkan berdasarkan bulan.
	 *
	 * @param satker id satuan kerja
	 * @param tahun  tahun anggaran
	 * @return daftar baris hasil query (masing-masing berisi bulan dan jumlah realisasi),
	 *         dibatasi {@link ais.common.Common#MAX_RESULT} baris
	 */
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

	/**
	 * Mengambil total anggaran terjadwal (kolom {@code anggaran} pada
	 * {@code t_jadwal_penyerapan}) yang dikelompokkan per bulan, untuk satu satuan kerja pada
	 * satu tahun anggaran, diurutkan berdasarkan bulan.
	 *
	 * @param satker id satuan kerja
	 * @param tahun  tahun anggaran
	 * @return daftar baris hasil query (masing-masing berisi bulan dan jumlah anggaran),
	 *         dibatasi {@link ais.common.Common#MAX_RESULT} baris
	 */
	public static List<?> getTotalAnggaran(String satker, String tahun) {
		StringBuilder sBuff = new StringBuilder("SELECT bulan, sum( anggaran ) FROM t_jadwal_penyerapan ");
		sBuff.append("WHERE id_m_satuan_kerja = :satker AND tahun = :tahun ");
		sBuff.append("GROUP BY bulan ORDER BY bulan ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("satker", satker);
		oQuery.setParameter("tahun", tahun);
		
		return oQuery.setMaxResults(ais.common.Common.MAX_RESULT).list();
	}
	
	/**
	 * Mengambil nama bulan (kolom {@code code_name} pada {@code ref_code_decode}, grup kode 1)
	 * berdasarkan kode bulan yang diberikan. Tabel referensi ini dipakai sebagai kamus
	 * kode-ke-label untuk berbagai domain di AIS; grup 1 secara khusus dipakai untuk nama bulan.
	 *
	 * @param monthCode kode bulan yang akan diterjemahkan menjadi nama bulan
	 * @return nama bulan sesuai kode, atau {@code null} bila kode tidak ditemukan
	 */
	public static String getMonthName(String monthCode) {
		StringBuilder sBuff = new StringBuilder("SELECT code_name FROM ref_code_decode ");
		sBuff.append("WHERE code_value=:monthCode and code_group_id=1 ");
		Query oQuery = HibernateUtil.currentSession().createSQLQuery(sBuff.toString());
		oQuery.setParameter("monthCode", monthCode);
		String sResult = (String) oQuery.uniqueResult();
		
		return sResult;
	}

}
