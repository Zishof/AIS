package ais.common.inventory.jdbc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import ais.common.inventory.InventoryMovementCommand;
import ais.common.inventory.InventoryMovementResult;
import ais.common.inventory.ledger.InventoryBalanceKey;
import ais.common.inventory.ledger.InventoryLedgerRecord;
import ais.common.inventory.ledger.InventoryLedgerRepository;

/**
 * Adapter PostgreSQL untuk ledger dan saldo inventory.
 *
 * Posting memakai satu transaksi database, row lock pada saldo, dan unique
 * constraint tenant + idempotency key. Karena itu dua koneksi yang mem-posting
 * perintah sama tidak dapat menggandakan mutasi ataupun saldo.
 */
public final class JdbcInventoryLedgerRepository implements InventoryLedgerRepository {

	private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";
	private final InventoryJdbcConnectionProvider connectionProvider;
	private final String schema;

	public JdbcInventoryLedgerRepository(InventoryJdbcConnectionProvider connectionProvider,
			String schema) {
		if (connectionProvider == null) {
			throw new IllegalArgumentException("connectionProvider wajib diisi");
		}
		if (schema == null || !schema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			throw new IllegalArgumentException("nama schema inventory tidak valid");
		}
		this.connectionProvider = connectionProvider;
		this.schema = schema;
	}

	public InventoryMovementResult post(InventoryMovementCommand command) {
		if (command == null) {
			return rejected("perintah inventory wajib diisi");
		}
		List<String> errors = command.validate();
		if (!errors.isEmpty()) {
			return rejected(gabung(errors));
		}

		Connection connection = null;
		boolean selesai = false;
		try {
			connection = connectionProvider.openConnection();
			connection.setAutoCommit(false);

			ExistingMovement existing = findExisting(connection, command.getTenantId(),
					command.getIdempotencyKey());
			if (existing != null) {
				connection.commit();
				selesai = true;
				return replayResult(existing, command);
			}

			InventoryBalanceKey balanceKey = new InventoryBalanceKey(command.getTenantId(),
					command.getLocationId(), command.getItemId(), command.getLotId());
			createBalanceIfAbsent(connection, balanceKey);
			BigDecimal before = lockBalance(connection, balanceKey);
			BigDecimal after = before.add(command.getQuantity());
			PostedMovement posted = insertLedger(connection, command, before, after);
			updateBalance(connection, balanceKey, after);
			connection.commit();
			selesai = true;
			return new InventoryMovementResult(InventoryMovementResult.POSTED,
					posted.movementId, "mutasi inventory berhasil diposting");
		} catch (SQLException e) {
			rollbackQuietly(connection);
			if (isUniqueViolation(e) && connection != null) {
				try {
					ExistingMovement existing = findExisting(connection, command.getTenantId(),
							command.getIdempotencyKey());
					if (existing != null) {
						connection.commit();
						selesai = true;
						return replayResult(existing, command);
					}
				} catch (SQLException lookupFailure) {
					e.setNextException(lookupFailure);
				}
			}
			throw new InventoryPersistenceException("gagal mem-posting ledger inventory", e);
		} finally {
			if (!selesai) rollbackQuietly(connection);
			closeQuietly(connection);
		}
	}

	public InventoryLedgerRecord findByIdempotencyKey(Long tenantId,
			String idempotencyKey) {
		if (tenantId == null || kosong(idempotencyKey)) return null;
		Connection connection = null;
		try {
			connection = connectionProvider.openConnection();
			ExistingMovement existing = findExisting(connection, tenantId,
					idempotencyKey.trim());
			return existing == null ? null : existing.record;
		} catch (SQLException e) {
			throw new InventoryPersistenceException("gagal membaca ledger inventory", e);
		} finally {
			closeQuietly(connection);
		}
	}

	public BigDecimal getBalance(InventoryBalanceKey balanceKey) {
		if (balanceKey == null) throw new IllegalArgumentException("balanceKey wajib diisi");
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			connection = connectionProvider.openConnection();
			statement = connection.prepareStatement("SELECT quantity_on_hand FROM " + schema
					+ ".stock_balance WHERE tenant_id=? AND location_id=? AND item_id=? "
					+ "AND lot_id IS NOT DISTINCT FROM ?");
			setBalanceKey(statement, balanceKey);
			result = statement.executeQuery();
			return result.next() ? result.getBigDecimal(1) : BigDecimal.ZERO;
		} catch (SQLException e) {
			throw new InventoryPersistenceException("gagal membaca saldo inventory", e);
		} finally {
			closeQuietly(result);
			closeQuietly(statement);
			closeQuietly(connection);
		}
	}

	private ExistingMovement findExisting(Connection connection, Long tenantId,
			String idempotencyKey) throws SQLException {
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			statement = connection.prepareStatement("SELECT id, tenant_id, location_id, item_id, "
					+ "uom_id, lot_id, quantity, balance_before, balance_after, idempotency_key, "
					+ "posted_at, source_type, source_id, event_type FROM " + schema
					+ ".stock_ledger WHERE tenant_id=? AND idempotency_key=?");
			statement.setLong(1, tenantId.longValue());
			statement.setString(2, idempotencyKey);
			result = statement.executeQuery();
			if (!result.next()) return null;
			Long lotId = nullableLong(result, "lot_id");
			InventoryBalanceKey key = new InventoryBalanceKey(
					Long.valueOf(result.getLong("tenant_id")),
					Long.valueOf(result.getLong("location_id")),
					Long.valueOf(result.getLong("item_id")), lotId);
			Timestamp postedAt = result.getTimestamp("posted_at");
			InventoryLedgerRecord record = new InventoryLedgerRecord(
					Long.valueOf(result.getLong("id")), key,
					result.getString("idempotency_key"), result.getBigDecimal("quantity"),
					result.getBigDecimal("balance_before"), result.getBigDecimal("balance_after"),
					new Date(postedAt.getTime()));
			return new ExistingMovement(record, Long.valueOf(result.getLong("uom_id")),
					result.getString("source_type"), result.getString("source_id"),
					result.getString("event_type"));
		} finally {
			closeQuietly(result);
			closeQuietly(statement);
		}
	}

	private void createBalanceIfAbsent(Connection connection, InventoryBalanceKey key)
			throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("INSERT INTO " + schema
					+ ".stock_balance (tenant_id, location_id, item_id, lot_id, "
					+ "quantity_on_hand, quantity_reserved, version_number, updated_at) "
					+ "VALUES (?, ?, ?, ?, 0, 0, 0, now()) ON CONFLICT DO NOTHING");
			setBalanceKey(statement, key);
			statement.executeUpdate();
		} finally {
			closeQuietly(statement);
		}
	}

	private BigDecimal lockBalance(Connection connection, InventoryBalanceKey key)
			throws SQLException {
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			statement = connection.prepareStatement("SELECT quantity_on_hand FROM " + schema
					+ ".stock_balance WHERE tenant_id=? AND location_id=? AND item_id=? "
					+ "AND lot_id IS NOT DISTINCT FROM ? FOR UPDATE");
			setBalanceKey(statement, key);
			result = statement.executeQuery();
			if (!result.next()) throw new SQLException("baris saldo inventory tidak terbentuk");
			return result.getBigDecimal(1);
		} finally {
			closeQuietly(result);
			closeQuietly(statement);
		}
	}

	private PostedMovement insertLedger(Connection connection, InventoryMovementCommand command,
			BigDecimal before, BigDecimal after) throws SQLException {
		PreparedStatement statement = null;
		ResultSet result = null;
		try {
			statement = connection.prepareStatement("INSERT INTO " + schema
					+ ".stock_ledger (tenant_id, location_id, item_id, uom_id, lot_id, quantity, "
					+ "balance_before, balance_after, source_type, source_id, event_type, "
					+ "idempotency_key, business_at, posted_at) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now()) RETURNING id, posted_at");
			statement.setLong(1, command.getTenantId().longValue());
			statement.setLong(2, command.getLocationId().longValue());
			statement.setLong(3, command.getItemId().longValue());
			statement.setLong(4, command.getUomId().longValue());
			setNullableLong(statement, 5, command.getLotId());
			statement.setBigDecimal(6, command.getQuantity());
			statement.setBigDecimal(7, before);
			statement.setBigDecimal(8, after);
			statement.setString(9, command.getSourceType());
			statement.setString(10, command.getSourceId());
			statement.setString(11, command.getEventType());
			statement.setString(12, command.getIdempotencyKey());
			statement.setTimestamp(13, new Timestamp(command.getBusinessAt().getTime()));
			result = statement.executeQuery();
			if (!result.next()) throw new SQLException("insert ledger tidak mengembalikan id");
			return new PostedMovement(Long.valueOf(result.getLong("id")));
		} finally {
			closeQuietly(result);
			closeQuietly(statement);
		}
	}

	private void updateBalance(Connection connection, InventoryBalanceKey key, BigDecimal after)
			throws SQLException {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("UPDATE " + schema
					+ ".stock_balance SET quantity_on_hand=?, version_number=version_number+1, "
					+ "updated_at=now() WHERE tenant_id=? AND location_id=? AND item_id=? "
					+ "AND lot_id IS NOT DISTINCT FROM ?");
			statement.setBigDecimal(1, after);
			statement.setLong(2, key.getTenantId().longValue());
			statement.setLong(3, key.getLocationId().longValue());
			statement.setLong(4, key.getItemId().longValue());
			setNullableLong(statement, 5, key.getLotId());
			if (statement.executeUpdate() != 1) {
				throw new SQLException("saldo inventory tidak berhasil diperbarui tepat satu baris");
			}
		} finally {
			closeQuietly(statement);
		}
	}

	private InventoryMovementResult replayResult(ExistingMovement existing,
			InventoryMovementCommand command) {
		if (!existing.matches(command)) {
			return new InventoryMovementResult(InventoryMovementResult.REJECTED,
					existing.record.getMovementId(),
					"idempotency key sudah dipakai oleh payload mutasi yang berbeda");
		}
		return new InventoryMovementResult(InventoryMovementResult.ALREADY_POSTED,
				existing.record.getMovementId(), "mutasi inventory sudah pernah diposting");
	}

	private static void setBalanceKey(PreparedStatement statement, InventoryBalanceKey key)
			throws SQLException {
		statement.setLong(1, key.getTenantId().longValue());
		statement.setLong(2, key.getLocationId().longValue());
		statement.setLong(3, key.getItemId().longValue());
		setNullableLong(statement, 4, key.getLotId());
	}

	private static void setNullableLong(PreparedStatement statement, int index, Long value)
			throws SQLException {
		if (value == null) statement.setNull(index, java.sql.Types.BIGINT);
		else statement.setLong(index, value.longValue());
	}

	private static Long nullableLong(ResultSet result, String column) throws SQLException {
		long value = result.getLong(column);
		return result.wasNull() ? null : Long.valueOf(value);
	}

	private static boolean isUniqueViolation(SQLException exception) {
		SQLException current = exception;
		while (current != null) {
			if (SQLSTATE_UNIQUE_VIOLATION.equals(current.getSQLState())) return true;
			current = current.getNextException();
		}
		return false;
	}

	private static String gabung(List<String> values) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) result.append("; ");
			result.append(values.get(i));
		}
		return result.toString();
	}

	private static boolean kosong(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static boolean sama(Object left, Object right) {
		return left == null ? right == null : left.equals(right);
	}

	private static InventoryMovementResult rejected(String message) {
		return new InventoryMovementResult(InventoryMovementResult.REJECTED, null, message);
	}

	private static void rollbackQuietly(Connection connection) {
		if (connection == null) return;
		try {
			if (!connection.getAutoCommit()) connection.rollback();
		} catch (SQLException ignored) {
		}
	}

	private static void closeQuietly(ResultSet result) {
		if (result == null) return;
		try {
			result.close();
		} catch (SQLException ignored) {
		}
	}

	private static void closeQuietly(PreparedStatement statement) {
		if (statement == null) return;
		try {
			statement.close();
		} catch (SQLException ignored) {
		}
	}

	private static void closeQuietly(Connection connection) {
		if (connection == null) return;
		try {
			connection.close();
		} catch (SQLException ignored) {
		}
	}

	private static final class PostedMovement {
		private final Long movementId;

		private PostedMovement(Long movementId) {
			this.movementId = movementId;
		}
	}

	private static final class ExistingMovement {
		private final InventoryLedgerRecord record;
		private final Long uomId;
		private final String sourceType;
		private final String sourceId;
		private final String eventType;

		private ExistingMovement(InventoryLedgerRecord record, Long uomId,
				String sourceType, String sourceId, String eventType) {
			this.record = record;
			this.uomId = uomId;
			this.sourceType = sourceType;
			this.sourceId = sourceId;
			this.eventType = eventType;
		}

		private boolean matches(InventoryMovementCommand command) {
			InventoryBalanceKey key = record.getBalanceKey();
			return key.getTenantId().equals(command.getTenantId())
					&& key.getLocationId().equals(command.getLocationId())
					&& key.getItemId().equals(command.getItemId())
					&& sama(key.getLotId(), command.getLotId())
					&& uomId.equals(command.getUomId())
					&& record.getQuantity().compareTo(command.getQuantity()) == 0
					&& sourceType.equals(command.getSourceType())
					&& sourceId.equals(command.getSourceId())
					&& eventType.equals(command.getEventType());
		}
	}
}
