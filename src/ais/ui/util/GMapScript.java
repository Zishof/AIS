package ais.ui.util;

import org.zkoss.zul.Script;

import ais.common.Common;

public class GMapScript extends Script {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4176272766128987234L;

	@SuppressWarnings("deprecation")
	public GMapScript() {
		super();
		setType("text/javascript");
		setContent("zk.googleAPIkey='" + Common.getGoogleMapKey() + "'");
	}

}
