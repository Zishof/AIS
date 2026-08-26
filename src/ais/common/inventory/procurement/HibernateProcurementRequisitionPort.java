package ais.common.inventory.procurement;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.asset.MasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAsset;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.inventory.Toko;

/**
 * Adapter shortage replenishment ke PR existing. Header, detail, dan metadata
 * bridge ditulis dalam satu transaksi. Unique key pada bridge menjadi pagar
 * idempotensi ketika dua worker memproses kebutuhan yang sama bersamaan.
 */
public final class HibernateProcurementRequisitionPort implements ProcurementRequisitionPort {

	private static final String DOCUMENT_TYPE = "PR";
	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private final ProcurementRequisitionLegacyReferenceResolver resolver;

	public HibernateProcurementRequisitionPort(
			ProcurementRequisitionLegacyReferenceResolver resolver) {
		if (resolver == null) throw new IllegalArgumentException("resolver wajib diisi");
		this.resolver = resolver;
	}

	public ProcurementRequisitionResult createOrFind(ProcurementRequisitionDraft draft) {
		List<String> validationErrors = draft == null
				? singleton("draft PR wajib diisi") : draft.validate();
		if (!validationErrors.isEmpty()) {
			return ProcurementRequisitionResult.rejected(gabung(validationErrors));
		}

		ResolvedReferences references;
		try {
			references = resolveReferences(draft);
		} catch (IllegalArgumentException e) {
			return ProcurementRequisitionResult.rejected(e.getMessage());
		}

		ProcurementRequisitionResult existing = findExisting(draft);
		if (existing != null) return existing;

		Session session = null;
		Transaction transaction = null;
		try {
			session = HibernateUtil.openSession();
			Tbmuser requester = (Tbmuser) session.get(Tbmuser.class, references.requesterUserId);
			if (requester == null) {
				return ProcurementRequisitionResult.rejected(
						"Pengguna pembuat PR tidak ditemukan: " + references.requesterUserId);
			}
			Toko targetToko = (Toko) session.get(Toko.class, references.targetTokoId);
			if (targetToko == null) {
				return ProcurementRequisitionResult.rejected(
						"Toko tujuan PR tidak ditemukan: " + references.targetTokoId);
			}
			for (int i = 0; i < references.lines.size(); i++) {
				ResolvedLine line = references.lines.get(i);
				line.masterAsset = (MasterAsset) session.get(MasterAsset.class, line.masterAssetId);
				if (line.masterAsset == null) {
					return ProcurementRequisitionResult.rejected(
							"MasterAsset tidak ditemukan untuk baris "
							+ line.draftLine.getLineNumber() + ": " + line.masterAssetId);
				}
			}

			transaction = session.beginTransaction();
			Date requestedAt = draft.getRequestedAt();
			PermintaanPengadaanMasterAsset header = new PermintaanPengadaanMasterAsset();
			header.setKode(buildRequisitionCode(draft.getIdempotencyKey()));
			header.setKeterangan(buildDescription(draft));
			header.setToko(targetToko);
			header.setDibuatOleh(requester);
			header.setOleh(requester.getUserId());
			header.setOlehId(requester.getUserId());
			header.setTanggalPembuatan(requestedAt);
			header.setTanpaAnggaran(Boolean.TRUE);
			header.setSetujuiManual(Boolean.FALSE);
			header.setAktif(Boolean.TRUE);
			header.setNilai(Double.valueOf(0.0d));
			session.save(header);
			session.flush();

			insertDocumentExtension(session, draft, header);
			for (int i = 0; i < references.lines.size(); i++) {
				ResolvedLine resolvedLine = references.lines.get(i);
				ProcurementRequisitionDraftLine draftLine = resolvedLine.draftLine;
				PermintaanPengadaanMasterAssetDetail detail =
						new PermintaanPengadaanMasterAssetDetail();
				detail.setPermintaanPengadaanMasterAsset(header);
				detail.setMasterAsset(resolvedLine.masterAsset);
				detail.setJumlah(Double.valueOf(draftLine.getRequestedQuantity().doubleValue()));
				detail.setJumlahDatang(Double.valueOf(0.0d));
				detail.setHargaBeli(Double.valueOf(0.0d));
				detail.setHargaTotal(Double.valueOf(0.0d));
				detail.setTanggalPembuatan(requestedAt);
				detail.setOleh(requester.getUserId());
				detail.setOlehId(requester.getUserId());
				detail.setKeterangan("Shortage replenishment baris "
						+ draftLine.getSourceReplenishmentLineNumber());
				session.save(detail);
				session.flush();
				insertItemReference(session, draft, draftLine, header, detail);
			}
			transaction.commit();
			return new ProcurementRequisitionResult(ProcurementRequisitionResult.CREATED,
					header.getId(), header.getKode(), "PR berhasil dibuat", draft);
		} catch (RuntimeException e) {
			rollbackQuietly(transaction);
			HibernateUtil.closeSessionQuietly(session);
			session = null;
			ProcurementRequisitionResult raced = findExisting(draft);
			if (raced != null) return raced;
			return ProcurementRequisitionResult.failed(
					"PR gagal disimpan secara atomik: " + safeMessage(e), draft);
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private ProcurementRequisitionResult findExisting(ProcurementRequisitionDraft draft) {
		Session session = null;
		try {
			session = HibernateUtil.openSession();
			Query query = session.createSQLQuery(
					"select e.legacy_document_id, p.kode "
					+ "from asset.procurement_document_extension e "
					+ "left join asset.permintaan_pengadaan_master_asset p "
					+ "on p.id = e.legacy_document_id "
					+ "where e.tenant_id = :tenantId "
					+ "and e.document_type = :documentType "
					+ "and e.idempotency_key = :idempotencyKey");
			query.setLong("tenantId", draft.getTenantId().longValue());
			query.setString("documentType", DOCUMENT_TYPE);
			query.setString("idempotencyKey", draft.getIdempotencyKey());
			query.setMaxResults(1);
			List<?> rows = query.list();
			if (rows.isEmpty()) return null;
			Object[] row = (Object[]) rows.get(0);
			Long requisitionId = row[0] == null ? null
					: Long.valueOf(((Number) row[0]).longValue());
			String requisitionNumber = row[1] == null ? "" : String.valueOf(row[1]);
			return new ProcurementRequisitionResult(
					ProcurementRequisitionResult.ALREADY_EXISTS, requisitionId,
					requisitionNumber, "PR yang sama sudah tersedia", draft);
		} catch (RuntimeException e) {
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private ResolvedReferences resolveReferences(ProcurementRequisitionDraft draft) {
		String requesterUserId = trim(resolver.resolveRequesterUserId(draft));
		Long targetTokoId = resolver.resolveTargetTokoId(draft);
		if (requesterUserId.length() == 0) {
			throw new IllegalArgumentException("Pemetaan pengguna pembuat PR belum tersedia");
		}
		if (!positive(targetTokoId)) {
			throw new IllegalArgumentException("Pemetaan toko tujuan PR belum tersedia");
		}
		List<ResolvedLine> lines = new ArrayList<ResolvedLine>();
		for (int i = 0; i < draft.getLines().size(); i++) {
			ProcurementRequisitionDraftLine line = draft.getLines().get(i);
			Long masterAssetId = resolver.resolveMasterAssetId(draft, line);
			if (!positive(masterAssetId)) {
				throw new IllegalArgumentException("Pemetaan MasterAsset baris "
						+ line.getLineNumber() + " belum tersedia");
			}
			lines.add(new ResolvedLine(line, masterAssetId));
		}
		return new ResolvedReferences(requesterUserId, targetTokoId, lines);
	}

	private void insertDocumentExtension(Session session,
			ProcurementRequisitionDraft draft,
			PermintaanPengadaanMasterAsset header) {
		Query query = session.createSQLQuery(
				"insert into asset.procurement_document_extension "
				+ "(tenant_id, document_type, legacy_document_id, idempotency_key, "
				+ "source_document_type, source_document_number, source_location_id, "
				+ "destination_location_id, status, correlation_id, created_at, updated_at) "
				+ "values (:tenantId, :documentType, :legacyDocumentId, :idempotencyKey, "
				+ ":sourceDocumentType, :sourceDocumentNumber, :sourceLocationId, "
				+ ":destinationLocationId, :status, :correlationId, current_timestamp, current_timestamp)");
		query.setLong("tenantId", draft.getTenantId().longValue());
		query.setString("documentType", DOCUMENT_TYPE);
		query.setLong("legacyDocumentId", header.getId().longValue());
		query.setString("idempotencyKey", draft.getIdempotencyKey());
		query.setString("sourceDocumentType", "REPLENISHMENT");
		query.setString("sourceDocumentNumber", draft.getSourceReplenishmentRequestNumber());
		query.setLong("sourceLocationId", draft.getSourceWarehouseLocationId().longValue());
		query.setLong("destinationLocationId", draft.getTargetOutletLocationId().longValue());
		query.setString("status", "DRAFT");
		query.setString("correlationId", draft.getIdempotencyKey());
		query.executeUpdate();
	}

	private void insertItemReference(Session session,
			ProcurementRequisitionDraft draft,
			ProcurementRequisitionDraftLine line,
			PermintaanPengadaanMasterAsset header,
			PermintaanPengadaanMasterAssetDetail detail) {
		Query query = session.createSQLQuery(
				"insert into asset.procurement_item_reference "
				+ "(tenant_id, document_type, legacy_document_id, legacy_line_id, "
				+ "line_number, source_line_number, item_id, uom_id, requested_quantity, created_at) "
				+ "values (:tenantId, :documentType, :legacyDocumentId, :legacyLineId, "
				+ ":lineNumber, :sourceLineNumber, :itemId, :uomId, :requestedQuantity, current_timestamp)");
		query.setLong("tenantId", draft.getTenantId().longValue());
		query.setString("documentType", DOCUMENT_TYPE);
		query.setLong("legacyDocumentId", header.getId().longValue());
		query.setLong("legacyLineId", detail.getId().longValue());
		query.setInteger("lineNumber", line.getLineNumber());
		query.setInteger("sourceLineNumber", line.getSourceReplenishmentLineNumber());
		query.setLong("itemId", line.getItemId().longValue());
		query.setLong("uomId", line.getUomId().longValue());
		query.setBigDecimal("requestedQuantity", line.getRequestedQuantity());
		query.executeUpdate();
	}

	static String buildRequisitionCode(String idempotencyKey) {
		String digest = digest(idempotencyKey);
		return "AUTO-PR-" + digest.substring(0, 24).toUpperCase();
	}

	private static String digest(String value) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] bytes = messageDigest.digest(trim(value).getBytes(UTF_8));
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				int unsigned = bytes[i] & 0xff;
				if (unsigned < 16) result.append('0');
				result.append(Integer.toHexString(unsigned));
			}
			return result.toString();
		} catch (Exception e) {
			CRC32 crc = new CRC32();
			crc.update(trim(value).getBytes(UTF_8));
			String fallback = Long.toHexString(crc.getValue());
			StringBuilder padded = new StringBuilder(fallback);
			while (padded.length() < 24) padded.append(fallback);
			return padded.substring(0, 24);
		}
	}

	private static String buildDescription(ProcurementRequisitionDraft draft) {
		return "PR otomatis dari replenishment "
				+ draft.getSourceReplenishmentRequestNumber()
				+ "; lokasi asal=" + draft.getSourceWarehouseLocationId()
				+ "; lokasi tujuan=" + draft.getTargetOutletLocationId();
	}

	private static List<String> singleton(String message) {
		List<String> messages = new ArrayList<String>();
		messages.add(message);
		return messages;
	}

	private static String gabung(List<String> messages) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < messages.size(); i++) {
			if (i > 0) result.append("; ");
			result.append(messages.get(i));
		}
		return result.toString();
	}

	private static boolean positive(Long value) {
		return value != null && value.longValue() > 0L;
	}

	private static String trim(String value) {
		return value == null ? "" : value.trim();
	}

	private static String safeMessage(Throwable throwable) {
		String message = throwable == null ? "" : throwable.getMessage();
		return trim(message).length() == 0
				? throwable == null ? "kesalahan tidak diketahui"
						: throwable.getClass().getName()
				: trim(message);
	}

	private static void rollbackQuietly(Transaction transaction) {
		if (transaction == null) return;
		try {
			transaction.rollback();
		} catch (Exception ignored) {
			// Kegagalan rollback tidak boleh menutupi akar error penyimpanan.
		}
	}

	private static final class ResolvedReferences {
		private final String requesterUserId;
		private final Long targetTokoId;
		private final List<ResolvedLine> lines;

		private ResolvedReferences(String requesterUserId, Long targetTokoId,
				List<ResolvedLine> lines) {
			this.requesterUserId = requesterUserId;
			this.targetTokoId = targetTokoId;
			this.lines = lines;
		}
	}

	private static final class ResolvedLine {
		private final ProcurementRequisitionDraftLine draftLine;
		private final Long masterAssetId;
		private MasterAsset masterAsset;

		private ResolvedLine(ProcurementRequisitionDraftLine draftLine,
				Long masterAssetId) {
			this.draftLine = draftLine;
			this.masterAssetId = masterAssetId;
		}
	}
}
