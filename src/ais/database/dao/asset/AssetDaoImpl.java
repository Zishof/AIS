package ais.database.dao.asset;

import ais.database.dao.GenericHibernateDao;
import ais.database.model.asset.Asset;

/**
 * Implementasi Hibernate {@link AssetDao} untuk entitas {@link ais.database.model.asset.Asset}.
 * Kelas ini sengaja kosong: seluruh logika CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericHibernateDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public class AssetDaoImpl extends GenericHibernateDao<Asset, Long, AssetDao>
		implements AssetDao {

}
