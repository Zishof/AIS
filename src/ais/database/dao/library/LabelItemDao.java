package ais.database.dao.library;

import ais.database.dao.GenericDao;
import ais.database.model.library.LabelItem;

/**
 * Kontrak DAO untuk entitas {@link ais.database.model.library.LabelItem} pada modul perpustakaan
 * — label/barcode fisik yang ditempel pada item pustaka. Interface ini sengaja kosong: seluruh
 * operasi CRUD generik sudah disediakan oleh {@link ais.database.dao.GenericDao}, lihat javadoc
 * di sana untuk detail perilakunya.
 *
 * @see ais.database.dao.GenericDao
 */
public interface LabelItemDao extends GenericDao<LabelItem, Long>{

}
