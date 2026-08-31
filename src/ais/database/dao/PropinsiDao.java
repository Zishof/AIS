package ais.database.dao;

import ais.database.model.Propinsi;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Propinsi} (data referensi propinsi).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface PropinsiDao extends GenericDao<Propinsi, Long>{

}
