package ais.common.inventory.controltower;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.common.inventory.controltower.ControlTowerTypes.Alert;
import ais.common.inventory.controltower.ControlTowerTypes.Filter;
import ais.common.inventory.controltower.ControlTowerTypes.Metric;
import ais.common.inventory.controltower.ControlTowerTypes.Snapshot;

public final class ControlTowerService {
	private final ControlTowerReadModelPort readModel;

	public ControlTowerService(ControlTowerReadModelPort readModel) {
		if (readModel == null) throw new IllegalArgumentException("readModel wajib diisi");
		this.readModel = readModel;
	}

	public Snapshot loadInitial(Filter filter, Date now) {
		validate(filter, now);
		Snapshot snapshot = readModel.findLatest(filter);
		if (snapshot != null) return snapshot;
		return new Snapshot("EMPTY-" + now.getTime(), filter.key(), ControlTowerTypes.STATUS_STALE,
				now, new Date(0L), null, null);
	}

	public Snapshot refresh(Filter filter, Date now) {
		validate(filter, now);
		List<Metric> metrics = safeMetrics(readModel.aggregateMetrics(filter));
		List<Alert> alerts = safeAlerts(readModel.aggregateAlerts(filter), filter.getLimit());
		validateMetrics(metrics);
		Snapshot snapshot = new Snapshot(buildSnapshotId(filter, now), filter.key(),
				ControlTowerTypes.STATUS_READY, now, now, metrics, alerts);
		readModel.save(snapshot);
		return snapshot;
	}

	public Snapshot loadForExport(String snapshotId) {
		if (snapshotId == null || snapshotId.trim().length() == 0)
			throw new IllegalArgumentException("snapshotId wajib diisi");
		Snapshot snapshot = readModel.findById(snapshotId.trim());
		if (snapshot == null) throw new IllegalStateException("snapshot laporan tidak ditemukan");
		if (!ControlTowerTypes.STATUS_READY.equals(snapshot.getStatus()))
			throw new IllegalStateException("snapshot laporan belum siap diekspor");
		return snapshot;
	}

	private static void validate(Filter filter, Date now) {
		if (filter == null) throw new IllegalArgumentException("filter wajib diisi");
		if (now == null) throw new IllegalArgumentException("waktu proses wajib diisi");
	}

	private static List<Metric> safeMetrics(List<Metric> metrics) {
		return metrics == null ? new ArrayList<Metric>() : new ArrayList<Metric>(metrics);
	}

	private static List<Alert> safeAlerts(List<Alert> alerts, int limit) {
		List<Alert> result = new ArrayList<Alert>();
		if (alerts == null) return result;
		for (int i = 0; i < alerts.size() && result.size() < limit; i++) result.add(alerts.get(i));
		return result;
	}

	private static void validateMetrics(List<Metric> metrics) {
		Set<String> codes = new HashSet<String>();
		for (Metric metric : metrics) {
			if (metric == null) throw new IllegalArgumentException("KPI tidak boleh null");
			String code = metric.getDefinition().getCode();
			if (!codes.add(code)) throw new IllegalArgumentException("kode KPI duplikat: " + code);
		}
	}

	private static String buildSnapshotId(Filter filter, Date now) {
		return "CT-" + filter.getTenantId() + "-" + now.getTime();
	}
}
