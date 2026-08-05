package ais.action.master.pmb;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.common.Common;

public class FormBiodataCalonMahasiswaAction extends GenericAutowireComposer  {

	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3103131919318540977L;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
	}
}
