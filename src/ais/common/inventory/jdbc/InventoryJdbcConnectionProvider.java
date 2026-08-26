package ais.common.inventory.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Penyedia koneksi khusus untuk adapter inventory.
 *
 * Setiap pemanggilan harus mengembalikan koneksi yang dimiliki pemanggil.
 * {@link JdbcInventoryLedgerRepository} selalu menutup koneksi tersebut.
 */
public interface InventoryJdbcConnectionProvider {
	Connection openConnection() throws SQLException;
}
