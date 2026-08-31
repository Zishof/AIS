package ais.database.dao;

import ais.database.model.ItemBiaya;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.ItemBiaya} (item/jenis biaya). Pasangan
 * Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao} tanpa
 * method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface ItemBiayaDao extends GenericDao<ItemBiaya, Long>{

}
