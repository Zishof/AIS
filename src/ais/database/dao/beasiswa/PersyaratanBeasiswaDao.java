package ais.database.dao.beasiswa;

import ais.database.dao.GenericDao;
import ais.database.model.beasiswa.PersyaratanBeasiswa;

/**
 * DAO untuk entitas {@link ais.database.model.beasiswa.PersyaratanBeasiswa} — syarat yang harus
 * dipenuhi mahasiswa untuk mendapatkan suatu beasiswa (modul beasiswa). Interface ini sengaja
 * kosong: seluruh kontrak CRUD generik diwariskan dari {@link ais.database.dao.GenericDao}, lihat
 * Javadoc di sana untuk detail perilaku method.
 */
public interface PersyaratanBeasiswaDao extends
		GenericDao<PersyaratanBeasiswa, Long> {

}
