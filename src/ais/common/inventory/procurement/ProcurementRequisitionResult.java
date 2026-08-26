package ais.common.inventory.procurement;

/** Hasil penyambungan shortage replenishment ke PR existing. */
public final class ProcurementRequisitionResult {

	public static final String CREATED = "CREATED";
	public static final String ALREADY_EXISTS = "ALREADY_EXISTS";
	public static final String NOT_REQUIRED = "NOT_REQUIRED";
	public static final String REJECTED = "REJECTED";
	public static final String FAILED = "FAILED";

	private final String status;
	private final Long requisitionId;
	private final String requisitionNumber;
	private final String message;
	private final ProcurementRequisitionDraft draft;

	public ProcurementRequisitionResult(String status, Long requisitionId,
			String requisitionNumber, String message,
			ProcurementRequisitionDraft draft) {
		this.status = bersihkan(status);
		this.requisitionId = requisitionId;
		this.requisitionNumber = bersihkan(requisitionNumber);
		this.message = bersihkan(message);
		this.draft = draft;
	}

	public static ProcurementRequisitionResult notRequired(String message) {
		return new ProcurementRequisitionResult(NOT_REQUIRED, null, "", message, null);
	}

	public static ProcurementRequisitionResult rejected(String message) {
		return new ProcurementRequisitionResult(REJECTED, null, "", message, null);
	}

	public static ProcurementRequisitionResult failed(String message,
			ProcurementRequisitionDraft draft) {
		return new ProcurementRequisitionResult(FAILED, null, "", message, draft);
	}

	public String getStatus() { return status; }
	public Long getRequisitionId() { return requisitionId; }
	public String getRequisitionNumber() { return requisitionNumber; }
	public String getMessage() { return message; }
	public ProcurementRequisitionDraft getDraft() { return draft; }
	public boolean isSuccessful() {
		return CREATED.equals(status) || ALREADY_EXISTS.equals(status) || NOT_REQUIRED.equals(status);
	}

	private static String bersihkan(String value) {
		return value == null ? "" : value.trim();
	}
}
