package ais.ui.util;

import org.zkoss.zul.Script;

public class CheckForParentScript extends Script {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5411499273013627526L;

	public CheckForParentScript() {
		super();
//		setContent("<SCRIPT language=\"JavaScript\">window.onbeforeunload = function() { return \"Your work will be lost.\"; };</SCRIPT>");
//		setContent("var isInIFrame = (window.location != window.parent.location && window.name != 'main') ? true : false;if(isInIFrame){parent.location.reload();}");
	}

}
