package ais.action.master.sapto.util;

import java.util.List;
import java.util.Map;

public interface SaptoModel {
	@SuppressWarnings("rawtypes")
	public List<List> generateData(Map<String, Object> parameters);

}
