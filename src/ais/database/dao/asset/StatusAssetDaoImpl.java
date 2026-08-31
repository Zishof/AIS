package ais.database.dao.asset;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.asset.StatusAsset;

/**
 * Implementasi Hibernate {@link StatusAssetDao} untuk entitas
 * {@link ais.database.model.asset.StatusAsset}. Kelas ini sengaja kosong: seluruh logika CRUD
 * generik diwariskan dari {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana
 * untuk detail perilaku method.
 */
public class StatusAssetDaoImpl extends
		GenericHibernateDao<StatusAsset, Long, StatusAssetDao> implements
		StatusAssetDao {

}
