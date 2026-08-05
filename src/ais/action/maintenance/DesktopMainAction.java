package ais.action.maintenance;

import javax.servlet.http.HttpServletRequest;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

public class DesktopMainAction extends MainAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8476860927340423852L;

	public DesktopMainAction() {
		super();
	}

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		request.getSession(true).setAttribute("is_mobile", false);
		super.doAfterCompose(comp);
	}

}
