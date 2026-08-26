package ais.common.inventory.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Connection provider sederhana untuk UAT dan proses standalone. */
public final class DriverManagerInventoryConnectionProvider
		implements InventoryJdbcConnectionProvider {

	private final String jdbcUrl;
	private final String username;
	private final String password;

	public DriverManagerInventoryConnectionProvider(String jdbcUrl, String username,
			String password) {
		if (jdbcUrl == null || jdbcUrl.trim().length() == 0) {
			throw new IllegalArgumentException("jdbcUrl wajib diisi");
		}
		this.jdbcUrl = jdbcUrl.trim();
		this.username = username == null ? "" : username;
		this.password = password == null ? "" : password;
	}

	public Connection openConnection() throws SQLException {
		return DriverManager.getConnection(jdbcUrl, username, password);
	}
}
