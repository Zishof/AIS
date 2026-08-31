package ais.database.dao.asset;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.asset.JenisAsset;

/**
 * Implementasi Hibernate {@link JenisAssetDao} untuk entitas {@link ais.database.model.asset.JenisAsset}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class JenisAssetDaoImpl extends
		GenericHibernateDao<JenisAsset, Long, JenisAssetDao> implements
		JenisAssetDao {

}
