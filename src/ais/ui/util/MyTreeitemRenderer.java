package ais.ui.util;

import org.zkoss.zul.Treeitem;
import org.zkoss.zul.TreeitemRenderer;

public abstract class MyTreeitemRenderer implements TreeitemRenderer {

	public void render(Treeitem arg0, Object arg1, int arg2) throws Exception {
		render(arg0, arg1);
	}

	public abstract void render(Treeitem arg0, Object arg1) throws Exception;
}
