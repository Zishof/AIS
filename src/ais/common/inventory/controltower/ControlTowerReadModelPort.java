package ais.common.inventory.controltower;

import java.util.List;

import ais.common.inventory.controltower.ControlTowerTypes.Alert;
import ais.common.inventory.controltower.ControlTowerTypes.Filter;
import ais.common.inventory.controltower.ControlTowerTypes.Metric;
import ais.common.inventory.controltower.ControlTowerTypes.Snapshot;

public interface ControlTowerReadModelPort {
	Snapshot findLatest(Filter filter);
	Snapshot findById(String snapshotId);
	List<Metric> aggregateMetrics(Filter filter);
	List<Alert> aggregateAlerts(Filter filter);
	void save(Snapshot snapshot);
}
