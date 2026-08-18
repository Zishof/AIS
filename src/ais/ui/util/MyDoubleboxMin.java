package ais.ui.util;

import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

public class MyDoubleboxMin extends MyDoublebox {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5925156244581127226L;

	public MyDoubleboxMin() {
		super();
		init();
	}

	public MyDoubleboxMin(Double value) throws WrongValueException {
		super(value);
		init();
	}

	private void init() {
		setStyle("text-align: right;");
		setFormat("#,##0.##");
		// setWidth("95%");

		addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				setValue(getValue());
			}
		});
	}

	public Double getValue() {
		return super.getValue() == null ? 0.0 : -Math.abs(super.getValue());
	}
}
