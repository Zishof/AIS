package ais.ui.util;

import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Iframe;

public class MyIframe extends Iframe {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8735575580753564474L;

	public MyIframe() {
		super();
		init();
	}

	public MyIframe(String src) {
		super();
		init();
		setSrc(src);
	}

	/**
	 * URL internal yang diawali slash harus mengikuti context aplikasi aktif.
	 * Tanpa prefix ini, instalasi multi-context seperti /batusangkar akan meminta
	 * /pages/... dari ROOT Tomcat dan iframe justru menampilkan portal publik.
	 */
	public void setSrc(String src) {
		super.setSrc(contextAware(src));
	}

	private static String contextAware(String src) {
		if (src == null || src.length() == 0 || !src.startsWith("/") || src.startsWith("//")) {
			return src;
		}
		String context = "";
		try {
			Execution execution = Executions.getCurrent();
			if (execution != null && execution.getContextPath() != null) context = execution.getContextPath();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "MyIframe.contextAware");
		}
		if (context.length() == 0 && ais.common.Common.ROOT != null) context = ais.common.Common.ROOT;
		if (context == null || context.length() == 0 || "/".equals(context)) return src;
		if (!context.startsWith("/")) context = "/" + context;
		if (context.endsWith("/")) context = context.substring(0, context.length() - 1);
		return src.equals(context) || src.startsWith(context + "/") ? src : context + src;
	}

	private void init() {
		setHeight("100%");
		setWidth("100%");
	}

}
