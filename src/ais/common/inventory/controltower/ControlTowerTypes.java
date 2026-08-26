package ais.common.inventory.controltower;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public final class ControlTowerTypes {
	public static final String STATUS_READY = "READY";
	public static final String STATUS_STALE = "STALE";
	public static final String STATUS_FAILED = "FAILED";
	public static final String SEVERITY_INFO = "INFO";
	public static final String SEVERITY_WARNING = "WARNING";
	public static final String SEVERITY_CRITICAL = "CRITICAL";

	private ControlTowerTypes() { }

	public static final class Filter {
		private final long tenantId;
		private final Long locationId;
		private final Date fromDate;
		private final Date toDate;
		private final int limit;
		private final int offset;

		public Filter(long tenantId, Long locationId, Date fromDate, Date toDate, int limit, int offset) {
			if (tenantId <= 0L) throw new IllegalArgumentException("tenantId wajib diisi");
			if (fromDate == null || toDate == null) throw new IllegalArgumentException("periode wajib diisi");
			if (fromDate.after(toDate)) throw new IllegalArgumentException("periode tidak valid");
			if (limit < 1 || limit > 500) throw new IllegalArgumentException("limit harus 1 sampai 500");
			if (offset < 0) throw new IllegalArgumentException("offset tidak boleh negatif");
			this.tenantId = tenantId;
			this.locationId = locationId;
			this.fromDate = new Date(fromDate.getTime());
			this.toDate = new Date(toDate.getTime());
			this.limit = limit;
			this.offset = offset;
		}

		public long getTenantId() { return tenantId; }
		public Long getLocationId() { return locationId; }
		public Date getFromDate() { return new Date(fromDate.getTime()); }
		public Date getToDate() { return new Date(toDate.getTime()); }
		public int getLimit() { return limit; }
		public int getOffset() { return offset; }
		public String key() {
			return tenantId + "|" + (locationId == null ? "ALL" : locationId.toString()) + "|"
					+ fromDate.getTime() + "|" + toDate.getTime() + "|" + limit + "|" + offset;
		}
	}

	public static final class MetricDefinition {
		private final String code;
		private final String label;
		private final String owner;
		private final String source;
		private final String drillDownRoute;
		private final String reconciliationQuery;

		public MetricDefinition(String code, String label, String owner, String source,
				String drillDownRoute, String reconciliationQuery) {
			this.code = required(code, "kode KPI");
			this.label = required(label, "label KPI");
			this.owner = required(owner, "owner KPI");
			this.source = required(source, "sumber KPI");
			this.drillDownRoute = required(drillDownRoute, "rute drill-down");
			this.reconciliationQuery = required(reconciliationQuery, "kueri rekonsiliasi");
		}

		public String getCode() { return code; }
		public String getLabel() { return label; }
		public String getOwner() { return owner; }
		public String getSource() { return source; }
		public String getDrillDownRoute() { return drillDownRoute; }
		public String getReconciliationQuery() { return reconciliationQuery; }
	}

	public static final class Metric {
		private final String module;
		private final MetricDefinition definition;
		private final long count;
		private final BigDecimal amount;

		public Metric(String module, MetricDefinition definition, long count, BigDecimal amount) {
			this.module = required(module, "modul KPI");
			if (definition == null) throw new IllegalArgumentException("definisi KPI wajib diisi");
			if (count < 0L) throw new IllegalArgumentException("jumlah KPI tidak boleh negatif");
			this.definition = definition;
			this.count = count;
			this.amount = amount == null ? BigDecimal.ZERO : amount;
		}

		public String getModule() { return module; }
		public MetricDefinition getDefinition() { return definition; }
		public long getCount() { return count; }
		public BigDecimal getAmount() { return amount; }
	}

	public static final class Alert {
		private final String module;
		private final String referenceType;
		private final String referenceId;
		private final String severity;
		private final String title;
		private final String message;
		private final String drillDownRoute;
		private final Date occurredAt;

		public Alert(String module, String referenceType, String referenceId, String severity,
				String title, String message, String drillDownRoute, Date occurredAt) {
			this.module = required(module, "modul alert");
			this.referenceType = required(referenceType, "tipe referensi alert");
			this.referenceId = required(referenceId, "ID referensi alert");
			if (!SEVERITY_INFO.equals(severity) && !SEVERITY_WARNING.equals(severity)
					&& !SEVERITY_CRITICAL.equals(severity)) throw new IllegalArgumentException("severity tidak valid");
			this.severity = severity;
			this.title = required(title, "judul alert");
			this.message = required(message, "pesan alert");
			this.drillDownRoute = required(drillDownRoute, "rute drill-down alert");
			if (occurredAt == null) throw new IllegalArgumentException("waktu alert wajib diisi");
			this.occurredAt = new Date(occurredAt.getTime());
		}

		public String getModule() { return module; }
		public String getReferenceType() { return referenceType; }
		public String getReferenceId() { return referenceId; }
		public String getSeverity() { return severity; }
		public String getTitle() { return title; }
		public String getMessage() { return message; }
		public String getDrillDownRoute() { return drillDownRoute; }
		public Date getOccurredAt() { return new Date(occurredAt.getTime()); }
	}

	public static final class Snapshot {
		private final String snapshotId;
		private final String filterKey;
		private final String status;
		private final Date generatedAt;
		private final Date watermark;
		private final List<Metric> metrics;
		private final List<Alert> alerts;

		public Snapshot(String snapshotId, String filterKey, String status, Date generatedAt,
				Date watermark, List<Metric> metrics, List<Alert> alerts) {
			this.snapshotId = required(snapshotId, "snapshotId");
			this.filterKey = required(filterKey, "filterKey");
			if (!STATUS_READY.equals(status) && !STATUS_STALE.equals(status)
					&& !STATUS_FAILED.equals(status)) throw new IllegalArgumentException("status snapshot tidak valid");
			if (generatedAt == null || watermark == null) throw new IllegalArgumentException("waktu snapshot wajib diisi");
			this.status = status;
			this.generatedAt = new Date(generatedAt.getTime());
			this.watermark = new Date(watermark.getTime());
			this.metrics = immutableCopy(metrics);
			this.alerts = immutableCopy(alerts);
		}

		public String getSnapshotId() { return snapshotId; }
		public String getFilterKey() { return filterKey; }
		public String getStatus() { return status; }
		public Date getGeneratedAt() { return new Date(generatedAt.getTime()); }
		public Date getWatermark() { return new Date(watermark.getTime()); }
		public List<Metric> getMetrics() { return metrics; }
		public List<Alert> getAlerts() { return alerts; }
	}

	private static String required(String value, String label) {
		if (value == null || value.trim().length() == 0) throw new IllegalArgumentException(label + " wajib diisi");
		return value.trim();
	}

	private static <T> List<T> immutableCopy(List<T> values) {
		if (values == null) return Collections.emptyList();
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}
}
