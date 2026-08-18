package ais.action.report.helper;

import java.io.Serializable;
import java.util.Map;

public interface ParameterListener {

	public Map<String, Serializable> generateParameters() throws Exception;

}
