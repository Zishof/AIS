package ais.ui.util;

import org.zkoss.zk.ui.event.Event;

import ais.database.model.GeneralValueObject;

public interface DataInitDefault {

	public void init(GeneralValueObject obj) throws Exception;

	public void onSearchDefault(Event event);
}
