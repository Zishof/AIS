package ais.action.master.inventory;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.hibernate.Session;

/**
 * Pencatat ledger persediaan append-only. Pada tahap awal ledger berjalan dalam
 * mode shadow: rumus stok legacy tetap berwenang, sedangkan ledger memberi jejak
 * audit dan bahan rekonsiliasi. INSERT dibuat idempoten berdasarkan referensi
 * dokumen, produk, batch, dan jenis gerakan.
 */
public final class InventoryLedgerUtil {

	private InventoryLedgerUtil() {
	}

	public static void catatDariSaldoAkhir(Session session, String referenceType, String referenceId,
			String idempotencyKey, Long produkId, Long produkBatchId, Long tokoAsal, Long tokoTujuan,
			String movementType, double signedQuantity, double unitCost, Date occurredAt,
			String createdBy, String deviceId, String reason, Long reversalOf) throws Exception {
		if (session == null || produkId == null || referenceType == null || referenceId == null
				|| movementType == null || Math.abs(signedQuantity) < 0.000001d) {
			return;
		}

		double stockAfter = bacaStok(session, produkId);
		double stockBefore = stockAfter - signedQuantity;
		if (unitCost <= 0d) {
			unitCost = bacaHargaBeli(session, produkId);
		}
		pastikanSaldoAwal(session, produkId, tokoAsal == null ? tokoTujuan : tokoAsal,
				stockBefore, unitCost, occurredAt, createdBy);

		PreparedStatement ps = session.connection().prepareStatement(
				"INSERT INTO koperasi.inventory_movement(reference_type,reference_id,idempotency_key,produk,produk_batch,"
						+ "toko_asal,toko_tujuan,movement_type,quantity,unit_cost,movement_value,stock_before,stock_after,"
						+ "occurred_at,created_by,device_id,reason,reversal_of,status) "
						+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'POSTED') ON CONFLICT DO NOTHING");
		int i = 1;
		ps.setString(i++, referenceType);
		ps.setString(i++, referenceId);
		ps.setString(i++, kosongKeNull(idempotencyKey));
		ps.setLong(i++, produkId.longValue());
		setLongNullable(ps, i++, produkBatchId);
		setLongNullable(ps, i++, tokoAsal);
		setLongNullable(ps, i++, tokoTujuan);
		ps.setString(i++, movementType);
		ps.setBigDecimal(i++, BigDecimal.valueOf(signedQuantity));
		ps.setBigDecimal(i++, BigDecimal.valueOf(unitCost));
		ps.setBigDecimal(i++, BigDecimal.valueOf(signedQuantity * unitCost));
		ps.setBigDecimal(i++, BigDecimal.valueOf(stockBefore));
		ps.setBigDecimal(i++, BigDecimal.valueOf(stockAfter));
		ps.setTimestamp(i++, new java.sql.Timestamp((occurredAt == null ? new Date() : occurredAt).getTime()));
		ps.setString(i++, kosongKeNull(createdBy));
		ps.setString(i++, kosongKeNull(deviceId));
		ps.setString(i++, kosongKeNull(reason));
		setLongNullable(ps, i++, reversalOf);
		ps.executeUpdate();
		ps.close();
	}

	private static double bacaStok(Session session, Long produkId) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(stok,0) FROM koperasi.produk WHERE id=?");
		ps.setLong(1, produkId.longValue());
		ResultSet rs = ps.executeQuery();
		double result = rs.next() ? rs.getDouble(1) : 0d;
		rs.close();
		ps.close();
		return result;
	}

	private static double bacaHargaBeli(Session session, Long produkId) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"SELECT COALESCE(hargabeli,0) FROM koperasi.produk WHERE id=?");
		ps.setLong(1, produkId.longValue());
		ResultSet rs = ps.executeQuery();
		double result = rs.next() ? rs.getDouble(1) : 0d;
		rs.close();
		ps.close();
		return result;
	}

	private static void pastikanSaldoAwal(Session session, Long produkId, Long tokoId,
			double quantity, double unitCost, Date occurredAt, String createdBy) throws Exception {
		PreparedStatement ps = session.connection().prepareStatement(
				"INSERT INTO koperasi.inventory_movement(reference_type,reference_id,produk,toko_tujuan,movement_type,"
						+ "quantity,unit_cost,movement_value,stock_before,stock_after,occurred_at,created_by,reason,status) "
						+ "SELECT 'OPENING_BALANCE','PRODUCT-' || ?,?,?,?,?,?,?,0,?,?,?,'Saldo awal otomatis saat aktivasi ledger','POSTED' "
						+ "WHERE NOT EXISTS (SELECT 1 FROM koperasi.inventory_movement WHERE produk=?) ON CONFLICT DO NOTHING");
		int i = 1;
		ps.setLong(i++, produkId.longValue());
		ps.setLong(i++, produkId.longValue());
		setLongNullable(ps, i++, tokoId);
		ps.setString(i++, "OPENING_BALANCE");
		ps.setBigDecimal(i++, BigDecimal.valueOf(quantity));
		ps.setBigDecimal(i++, BigDecimal.valueOf(unitCost));
		ps.setBigDecimal(i++, BigDecimal.valueOf(quantity * unitCost));
		ps.setBigDecimal(i++, BigDecimal.valueOf(quantity));
		ps.setTimestamp(i++, new java.sql.Timestamp((occurredAt == null ? new Date() : occurredAt).getTime()));
		ps.setString(i++, kosongKeNull(createdBy));
		ps.setLong(i++, produkId.longValue());
		ps.executeUpdate();
		ps.close();
	}

	private static void setLongNullable(PreparedStatement ps, int index, Long value) throws Exception {
		if (value == null) {
			ps.setNull(index, java.sql.Types.BIGINT);
		} else {
			ps.setLong(index, value.longValue());
		}
	}

	private static String kosongKeNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}
}
