package ais.database.dao.surat;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;

/**
 * Implementasi Hibernate {@link AlurPersetujuanSuratKeluarStatusDao} untuk entitas
 * {@link ais.database.model.surat.AlurPersetujuanSuratKeluarStatus}. Kelas ini sengaja kosong:
 * seluruh logika CRUD generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public class AlurPersetujuanSuratKeluarStatusDaoImpl
		extends
		GenericHibernateDao<AlurPersetujuanSuratKeluarStatus, Long, AlurPersetujuanSuratKeluarStatusDao>
		implements AlurPersetujuanSuratKeluarStatusDao {

}
