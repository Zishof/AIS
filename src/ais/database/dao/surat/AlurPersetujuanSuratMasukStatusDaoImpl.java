package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;

/**
 * Implementasi Hibernate {@link AlurPersetujuanSuratMasukStatusDao} untuk entitas
 * {@link ais.database.model.surat.AlurPersetujuanSuratMasukStatus}. Kelas ini sengaja kosong:
 * seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public class AlurPersetujuanSuratMasukStatusDaoImpl
		extends
		GenericHibernateDao<AlurPersetujuanSuratMasukStatus, Long, AlurPersetujuanSuratMasukStatusDao>
		implements AlurPersetujuanSuratMasukStatusDao {

}
