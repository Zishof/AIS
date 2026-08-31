package ais.database.dao;

import ais.database.model.QuotaWisudaUntukFakultas;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.QuotaWisudaUntukFakultas} (data kuota
 * wisuda per fakultas). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface QuotaWisudaUntukFakultasDao extends GenericDao<QuotaWisudaUntukFakultas, Long>{

}
