package ais.database.dao.asset;

import ais.database.dao.GenericDao;
import ais.database.model.asset.JenisAsset;

/**
 * DAO untuk entitas {@link ais.database.model.asset.JenisAsset} — jenis/tipe aset (modul
 * manajemen aset). Interface ini sengaja kosong: seluruh kontrak CRUD generik diwariskan dari
 * {@link ais.database.dao.GenericDao}, lihat Javadoc di sana untuk detail perilaku method.
 */
public interface JenisAssetDao extends GenericDao<JenisAsset, Long> {
	
}