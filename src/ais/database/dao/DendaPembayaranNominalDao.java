package ais.database.dao;

import ais.database.model.DendaPembayaranNominal;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.DendaPembayaranNominal} (data nominal
 * denda pembayaran). Pasangan Dao/DaoImpl ini murni memakai perilaku generik
 * {@link ais.database.dao.GenericDao} tanpa method tambahan -- lihat javadoc di sana untuk
 * semantik lengkap tiap operasi CRUD yang tersedia.
 */
public interface DendaPembayaranNominalDao extends GenericDao<DendaPembayaranNominal, Long> {

}
