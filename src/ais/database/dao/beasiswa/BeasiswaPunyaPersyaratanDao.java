package ais.database.dao.beasiswa;

import ais.database.dao.GenericDao;
import ais.database.model.beasiswa.BeasiswaPunyaPersyaratan;

/**
 * DAO untuk entitas {@link ais.database.model.beasiswa.BeasiswaPunyaPersyaratan} — relasi
 * persyaratan yang dimiliki suatu program beasiswa (modul beasiswa). Interface ini sengaja kosong:
 * seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat Javadoc
 * di sana untuk detail perilaku method.
 */
public interface BeasiswaPunyaPersyaratanDao extends
		GenericDao<BeasiswaPunyaPersyaratan, Long> {

}
