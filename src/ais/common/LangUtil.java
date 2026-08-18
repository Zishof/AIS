package ais.common;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.zkoss.util.resource.LabelLocator;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.IdSpace;
import org.zkoss.zk.ui.Sessions;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Label;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Utility for Language Purpose
 * 
 * @author Achtartuanda
 * 
 */
public class LangUtil implements LabelLocator, Serializable {

	// public static Logger logger = Logger.getLogger(LangUtil.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1924881061635394611L;
	private ServletContext _svlctx;
	private String _name;

	private boolean init = false;
	private Locale locale;

	public LangUtil(String module, Locale locale) {
		try {
			this.locale = locale;
			ServletContext srvctx = (ServletContext) Sessions.getCurrent()
					.getWebApp().getNativeContext();
			new LangUtil(srvctx, module);
			Labels.register(new LangUtil(srvctx, module));
			init = true;
		} catch (Exception e) {
			init = false;
			// Common.tampilErrorJikaAdmin(e); 
		}
	}

	public LangUtil(ServletContext svlctx, String _name) {
		this._svlctx = svlctx;
		this._name = _name;
	}

	@Override
	public URL locate(Locale locale) throws MalformedURLException {

		if (locale == null) {
			locale = this.locale;
		}

		// if (locale == null) {
		String local = (String) Sessions.getCurrent().getAttribute(
				"langSession") == null ? "en" : (String) Sessions.getCurrent()
				.getAttribute("langSession");
		locale = new Locale(local);
		// }
		String url = "/WEB-INF/lang/" + _name + "/" + _name + "_" + locale
				+ ".properties";
		URL result = _svlctx.getResource(url);
		return result;
	}

	public String getLabel(String key) {
		String label = Labels.getLabel(key);
		if (label == null || label.length() == 0) {
			// logger.warn("No value of label found with key: " + key);
		}
		return label;
	}

	@SuppressWarnings("unchecked")
	public void setLanguage(Component rootComponent) {
		List<Component> components = (List<Component>) rootComponent
				.getChildren();
		for (int i = 0; i < components.size(); i++) {
			Component component = components.get(i);
			setComponentLanguage(component);
			if (component.getChildren().size() > 0)
				setLanguage(component);
		}
	}

	public static void setComponentLanguage(Component component) {
		IdSpace spaceOwner = component.getSpaceOwner();
		if (spaceOwner instanceof Component) {

			String label = Labels.getLabel(component.getId());

			if (label != null) {
				if (component instanceof Label)
					((Label) component).setValue(label);
				else if (component instanceof MyButtonConfig)
					((MyButtonConfig) component).setLabel(label);
				else if (component instanceof MyColumnConfig)
					((MyColumnConfig) component).setLabel(label);
				else if (component instanceof MyWindow)
					((MyWindow) component).setTitle(label);
				else if (component instanceof MyToolbarbuttonConfig)
					((MyToolbarbuttonConfig) component).setLabel(label);
			}
		}
	}

	public void setInit(boolean init) {
		this.init = init;
	}

	public boolean isInit() {
		return init;
	}

}
