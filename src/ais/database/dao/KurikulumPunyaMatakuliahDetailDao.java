package ais.database.dao;

import ais.database.model.KurikulumPunyaMatakuliahDetail;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.KurikulumPunyaMatakuliahDetail} (detail
 * relasi kurikulum dengan mata kuliah anggotanya). Pasangan Dao/DaoImpl ini murni memakai
 * perilaku generik {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di
 * sana untuk semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface KurikulumPunyaMatakuliahDetailDao extends
		GenericDao<KurikulumPunyaMatakuliahDetail, Long> {

}
