package ais.database.dao.akunting;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.akunting.KelompokLaporanPunyaAkun;

/**
 * Implementasi Hibernate {@link KelompokLaporanPunyaAkunDao} untuk entitas
 * {@link ais.database.model.akunting.KelompokLaporanPunyaAkun}. Kelas ini sengaja kosong: seluruh
 * logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public class KelompokLaporanPunyaAkunDaoImpl
		extends
		GenericHibernateDao<KelompokLaporanPunyaAkun, Long, KelompokLaporanPunyaAkunDao>
		implements KelompokLaporanPunyaAkunDao {

}
