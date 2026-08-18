package ais.common.listener;

import org.zkoss.zk.ui.event.Event;

public interface OnSave {
	public boolean onSave(Event event) throws Exception;
}
