package ais.database.dao;

import ais.database.model.Paket;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.Paket} (data paket perkuliahan/registrasi).
 * Pasangan Dao/DaoImpl ini murni memakai perilaku generik {@link ais.database.dao.GenericDao}
 * tanpa method tambahan -- lihat javadoc di sana untuk semantik lengkap tiap operasi CRUD yang
 * tersedia.
 */
public interface PaketDao extends GenericDao<Paket, Long>{

}
