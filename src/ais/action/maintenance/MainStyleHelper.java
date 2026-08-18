package ais.action.maintenance;

import java.lang.reflect.Method;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.impl.XulElement;

import ais.common.Common;

/**
 * Helper khusus styling untuk MainAction.
 *
 * Tujuan:
 * - MainAction tetap fokus ke logic/event.
 * - Seluruh akses setStyle/setSclass/setZclass dan style dinamis utama
 *   dikumpulkan di satu class agar lebih mudah dipusatkan ke css_utama.css.
 * - Tetap kompatibel Java 1.6/1.7 dan ZK lama.
 */
public final class MainStyleHelper {

	public static final String TRANSPARENT = "border:0px;background: transparent;";
	public static final String TRANSPARENT_NO_BORDER = "border:0;background:transparent;";
	public static final String CUSTOMER_SERVICE_LABEL = "font-size:11px;font-weight: bolder;";
	public static final String ONLINE_MOBILE = "font-size:8px;";
	public static final String SEARCH_LABEL = "font-size:11px;";
	public static final String MOBILE_TITLE = "font-size: 14px;";
	public static final String MOBILE_MOTTO = "font-size: 12px;";
	public static final String MOBILE_ADDRESS = "font-size: 9px;";
	public static final String MOBILE_MODULE_BUTTON = "font-size:10px;font-size:9px;color:white;";
	public static final String DEFAULT_FOOTER_COPYRIGHT_STYLE = "font-family: 'Open Sans', sans-serif;font-size: 12px;color:white;font-weight: bold;";

	private MainStyleHelper() {
	}

	public static void setStyle(Component component, String style) {
		if (component == null) {
			return;
		}
		try {
			Method setter = component.getClass().getMethod("setStyle", new Class[] { String.class });
			setter.invoke(component, new Object[] { style });
		} catch (Exception e) {
			showError(e);
		}
	}

	public static String getStyle(Component component) {
		if (component == null) {
			return "";
		}
		try {
			Method getter = component.getClass().getMethod("getStyle", new Class[] {});
			Object value = getter.invoke(component, new Object[] {});
			return value == null ? "" : String.valueOf(value);
		} catch (Exception e) {
			return "";
		}
	}

	public static void setSclass(Component component, String css) {
		if (component == null) {
			return;
		}
		try {
			Method setter = component.getClass().getMethod("setSclass", new Class[] { String.class });
			setter.invoke(component, new Object[] { css });
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:74");
			// Beberapa komponen lama tidak punya sclass; abaikan agar tetap aman di ZK 5.x.
		}
	}

	public static void setZclass(Component component, String css) {
		if (component == null) {
			return;
		}
		try {
			Method setter = component.getClass().getMethod("setZclass", new Class[] { String.class });
			setter.invoke(component, new Object[] { css });
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:86");
			// Beberapa komponen lama tidak punya zclass; abaikan.
		}
	}

	public static void appendSclassOnce(Component component, String css) {
		if (component == null || css == null || css.trim().length() == 0) {
			return;
		}
		try {
			Method getter = component.getClass().getMethod("getSclass", new Class[] {});
			Method setter = component.getClass().getMethod("setSclass", new Class[] { String.class });
			Object oldObj = getter.invoke(component, new Object[] {});
			String old = oldObj == null ? "" : String.valueOf(oldObj);
			String result = old;
			String[] parts = css.trim().split("\\s+");
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i] == null ? "" : parts[i].trim();
				if (part.length() > 0 && (" " + result + " ").indexOf(" " + part + " ") < 0) {
					result = result == null || result.trim().length() == 0 ? part : result + " " + part;
				}
			}
			setter.invoke(component, new Object[] { result });
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:109");
			// Beberapa komponen lama tidak punya sclass; abaikan supaya tetap aman di ZK 5.x.
		}
	}

	public static void appendStyleOnce(Component component, String style) {
		if (component == null || style == null || style.trim().length() == 0) {
			return;
		}
		try {
			String old = getStyle(component);
			if (old.indexOf(style) >= 0) {
				return;
			}
			String newStyle = old == null || old.trim().length() == 0 ? style : old + ";" + style;
			setStyle(component, newStyle);
		} catch (Exception e) {
			showError(e);
		}
	}

	public static void applyTransparent(Component component) {
		setStyle(component, TRANSPARENT);
	}

	public static void applyTransparentNoBorder(Component component) {
		setStyle(component, TRANSPARENT_NO_BORDER);
	}

	public static void applyFullSize(Component component) {
		applyFullSize(component, null);
	}

	public static void applyFullSize(Component component, String minimumHeight) {
		if (component == null) {
			return;
		}
		try {
			component.setVisible(true);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:148");
		}
		if (component instanceof org.zkoss.zul.Window) {
			org.zkoss.zul.Window window = (org.zkoss.zul.Window) component;
			try {
				window.setBorder("none");
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:154");
			}
			try {
				window.setClosable(false);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:158");
			}
			try {
				window.setSizable(false);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:162");
			}
		}
		if (component instanceof XulElement) {
			XulElement xul = (XulElement) component;
			boolean naturalHeight = component instanceof ais.ui.util.MyPortallayout;
			xul.setWidth("100%");
			xul.setHeight(naturalHeight ? "auto" : "100%");
			String style = xul.getStyle();
			if (style == null) {
				style = "";
			}
			if (naturalHeight && style.indexOf("height") < 0) {
				style = style + ";height:auto;";
			}
			if (naturalHeight && style.indexOf("min-height") < 0) {
				style = style + ";min-height:100%;";
			}
			if (naturalHeight && style.indexOf("overflow") < 0) {
				style = style + ";overflow:visible;";
			}
			if (minimumHeight != null && minimumHeight.trim().length() > 0 && style.indexOf("min-height") < 0) {
				style = style + ";min-height:" + minimumHeight.trim() + ";";
			}
			if (style.indexOf("box-sizing") < 0) {
				style = style + ";box-sizing:border-box;";
			}
			if (style.indexOf("max-width") < 0) {
				style = style + ";max-width:100%;";
			}
			xul.setStyle(style);
		}
	}

	public static void applyStandardTabbox(Tabbox tabbox, String resolvedHeight) {
		if (tabbox == null) {
			return;
		}
		appendSclassOnce(tabbox, "main-dashboard-tabbox");
		tabbox.setHeight(resolvedHeight);
		tabbox.setWidth("100%");
		setStyle(tabbox, "width:100%; max-width:100%; height:" + resolvedHeight + "; min-height:" + resolvedHeight
				+ "; max-height:" + resolvedHeight + "; overflow:hidden; box-sizing:border-box;");
	}

	public static void applyStandardTabpanels(Tabpanels panels, String resolvedHeight) {
		if (panels == null) {
			return;
		}
		appendSclassOnce(panels, "main-dashboard-tabpanels");
		panels.setHeight(resolvedHeight);
		panels.setWidth("100%");
		setStyle(panels, "width:100%; max-width:100%; height:" + resolvedHeight + "; min-height:" + resolvedHeight
				+ "; max-height:" + resolvedHeight + "; overflow-y:auto; overflow-x:hidden; box-sizing:border-box;");
	}

	public static void applyAutoScrollableTabpanel(Tabpanel panel, String resolvedHeight) {
		if (panel == null) {
			return;
		}
		appendSclassOnce(panel, "main-dashboard-tabpanel");
		panel.setHeight(resolvedHeight);
		panel.setWidth("100%");
		setStyle(panel, "height:" + resolvedHeight + "; min-height:" + resolvedHeight + "; max-height:" + resolvedHeight
				+ "; max-width:100%; overflow-y:auto; overflow-x:hidden; padding:0; margin:0; box-sizing:border-box; background:#f8fafc;");
	}

	public static void applyDashboardFrame(Component box) {
		appendSclassOnce(box, "main-dashboard-frame");
		/* Mode satu-scroll: frame mengalir setinggi isinya, scroll
		 * ditangani kanvas utama (index.zul), bukan frame ini. */
		setStyle(box, "height:auto;min-height:100%;max-height:none;background:#f8fafc;overflow:visible;"
				+ "padding:10px;box-sizing:border-box;max-width:100%;");
	}

	public static void applyDashboardContent(Div contentWrapper, String contentHeight) {
		if (contentWrapper == null) {
			return;
		}
		appendSclassOnce(contentWrapper, "main-dashboard-content");
		contentWrapper.setWidth("100%");
		/* Mode satu-scroll: tidak ada lagi clamp tinggi + overflow auto
		 * (sumber scrollbar lokal per-dashboard). contentHeight dipakai
		 * sebagai tinggi MINIMUM saja agar konten pendek tetap mengisi. */
		setStyle(contentWrapper, "width:100%;max-width:100%;height:auto;min-height:" + contentHeight
				+ ";max-height:none;overflow-x:hidden;overflow-y:visible;box-sizing:border-box;"
				+ "border-radius:14px;padding:0 2px 10px 0;");
	}

	public static void applyDashboardMessage(Div message, String borderColor) {
		if (message == null) {
			return;
		}
		String color = borderColor == null || borderColor.trim().length() == 0 ? "#bfdbfe" : borderColor;
		message.setWidth("100%");
		setStyle(message, "background:#ffffff;border:1px solid " + color
				+ ";border-radius:14px;padding:14px 16px;margin:8px 0;box-sizing:border-box;"
				+ "box-shadow:0 8px 18px rgba(15,23,42,0.08);");
	}

	public static void applyDashboardMessageTitle(Label label) {
		setStyle(label, "display:block;font-size:14px;font-weight:bold;color:#0f172a;margin-bottom:5px;");
	}

	public static void applyDashboardMessageDescription(Label label) {
		setStyle(label, "display:block;font-size:12px;color:#475569;line-height:1.45;white-space:normal;");
	}

	public static void applyDashboardHeader(Div header) {
		if (header == null) {
			return;
		}
		header.setWidth("100%");
		setStyle(header, "background:linear-gradient(135deg,#ffffff,#eef6ff);border:1px solid #dbeafe;"
				+ "border-radius:14px;padding:12px 14px;margin-bottom:10px;box-sizing:border-box;"
				+ "box-shadow:0 6px 16px rgba(15,23,42,0.08);");
	}

	public static void applyDashboardHeaderTitle(Label label) {
		setStyle(label, "display:block;font-size:15px;font-weight:bold;color:#0f172a;margin-bottom:4px;");
	}

	public static void applyDashboardHeaderDescription(Label label) {
		setStyle(label, "display:block;font-size:12px;color:#475569;line-height:1.45;white-space:normal;");
	}

	public static void applyBackground(Center center, String imageUrl) {
		if (center == null || imageUrl == null || imageUrl.trim().length() == 0) {
			return;
		}
		setStyle(center, "background:url('" + imageUrl + "') no-repeat center center fixed;-webkit-background-size: cover;"
				+ "-moz-background-size: cover;background-size: cover;-o-background-size: cover;");
	}



	public static void applyResponsiveFrameWidth(Component component) {
		appendStyleOnce(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;overflow:hidden;");
	}

	public static void applyResponsiveCenterWidth(Component component) {
		appendStyleOnce(component, "min-width:0;box-sizing:border-box;overflow:hidden;padding:0;margin:0;");
	}

	public static void applyResponsiveTabsWidth(Component component) {
		appendStyleOnce(component, "width:100%;max-width:100%;box-sizing:border-box;overflow-x:auto;overflow-y:hidden;");
	}

	public static void applyResponsivePanelWidth(Component component) {
		appendStyleOnce(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;overflow:hidden;");
	}

	public static void applyResponsiveHomePanelWidth(Component component) {
		appendStyleOnce(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;overflow:auto;");
	}

	public static void applyResponsiveGridHeaderWidth(Component component) {
		appendStyleOnce(component, "width:100%;max-width:100%;box-sizing:border-box;");
	}

	public static void applyResponsiveFrameHeight(Component component, String frameHeightPx, String minHeightPx) {
		appendStyleOnce(component, "height:" + frameHeightPx + ";min-height:" + minHeightPx
				+ ";max-width:100%;overflow:hidden;box-sizing:border-box;");
	}

	public static void applyResponsivePanelHeight(Component component, String frameHeightPx, String minHeightPx) {
		appendStyleOnce(component, "height:" + frameHeightPx + ";min-height:" + minHeightPx
				+ ";overflow:hidden;box-sizing:border-box;");
	}

	public static void applyResponsiveHomePanelHeight(Component component, String frameHeightPx, String minHeightPx) {
		appendStyleOnce(component, "height:" + frameHeightPx + ";min-height:" + minHeightPx
				+ ";overflow:auto;box-sizing:border-box;");
	}

	public static void applyResponsiveSidebarHeight(Component component, String frameHeightPx) {
		appendStyleOnce(component, "height:" + frameHeightPx + ";max-height:" + frameHeightPx
				+ ";overflow-y:auto;overflow-x:hidden;box-sizing:border-box;");
	}

	public static String menuBackgroundStyle(String menuBgColor) {
		String bg = menuBgColor == null || menuBgColor.trim().length() == 0 ? "transparent" : menuBgColor.trim();
		return "border:0px;background:" + bg + " repeat-x 0 0;";
	}


	public static void applyFooterText(Component component, String style) {
		setStyle(component, style);
	}

	public static void applyChatTransparent(Component component) {
		if (component == null) {
			return;
		}
		String style = getStyle(component);
		if (style.indexOf(";background: transparent;") < 0) {
			style = style + ";background: transparent;";
		}
		style = org.apache.commons.lang3.StringUtils.replace(style, ";background: yellow;", ";background: transparent;");
		setStyle(component, style);
	}

	public static void applyHomeStack(Component component) {
		setStyle(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;padding:10px 12px 36px 12px;");
	}

	public static void applyAnnouncementPanel(Component component) {
		setStyle(component, "width:100%;max-width:100%;box-sizing:border-box;margin:0 0 14px 0;");
	}

	public static void applyAnnouncementHeader(Component component) {
		setStyle(component, "width:100%;box-sizing:border-box;");
	}

	public static void applyAnnouncementSearchPanel(Component component) {
		setStyle(component, "width:100%;box-sizing:border-box;padding:12px 14px;");
	}

	public static void applyAnnouncementSearchInput(Component component) {
		setStyle(component, "max-width:100%;height:30px;border-radius:999px;padding:5px 12px;box-sizing:border-box;");
	}

	public static void applyAnnouncementSearchPrimaryButton(Component component) {
		setStyle(component, "margin-left:8px;font-weight:800;border-radius:999px;padding:5px 14px;");
	}

	public static void applyAnnouncementSearchSecondaryButton(Component component) {
		setStyle(component, "margin-left:6px;font-weight:800;border-radius:999px;padding:5px 14px;");
	}

	public static void applyAnnouncementGrid(Component component) {
		setStyle(component, "width:100%;max-width:100%;border:0;background:transparent;box-sizing:border-box;");
	}

	public static void applyAnnouncementEmpty(Component component) {
		setStyle(component, "display:block;padding:24px;text-align:center;font-weight:700;border-radius:14px;");
	}

	public static void applyAnnouncementGroupBox(Component component) {
		setStyle(component, "width:100%;box-sizing:border-box;margin:12px 0 5px 0;padding:4px 2px;");
	}

	public static void applyAnnouncementGroupTitle(Component component) {
		setStyle(component, "font-size:12px;font-weight:900;letter-spacing:.06em;text-transform:uppercase;");
	}

	public static void applyAnnouncementCard(Component component) {
		setStyle(component, "width:100%;max-width:100%;box-sizing:border-box;margin:6px 0;padding:13px 15px;border-radius:14px;");
	}

	public static void applyAnnouncementMeta(Component component) {
		setStyle(component, "width:100%;box-sizing:border-box;gap:6px;flex-wrap:wrap;margin-bottom:6px;");
	}

	public static void applyAnnouncementLink(Component component) {
		setStyle(component, "display:block;width:100%;max-width:100%;background:transparent;border:0;padding:0;"
				+ "white-space:normal;text-align:left;font-size:14px;font-weight:900;line-height:1.35;");
	}

	public static void applyAnnouncementDescription(Component component) {
		setStyle(component, "display:block;margin-top:7px;font-size:12px;line-height:1.5;white-space:normal;");
	}

	public static void applyAnnouncementChip(Component component) {
		setStyle(component, "display:inline-block;padding:4px 8px;border-radius:999px;font-size:10px;font-weight:800;line-height:1.2;");
	}

	public static void applyProfileBottomPanel(Component component) {
		setStyle(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;margin:0;");
	}

	public static void applyProfileBottomBody(Component component) {
		setStyle(component, "width:100%;max-width:100%;min-width:0;box-sizing:border-box;");
	}

	public static void applyMobileMenuTree(Component component, String menuBgColor) {
		String bg = menuBgColor == null || menuBgColor.trim().length() == 0 ? "transparent" : menuBgColor.trim();
		setStyle(component, "border-color:#EEEEEE;border:0px;background:" + bg + " repeat-x 0 0;");
	}


	public static void injectResponsiveShellCss(String root, int minMobile, int minDesktop) {
		try {
			/* main-responsive-zk.css tidak lagi dimuat dari Java karena style eksternal itu
			 * membuat layout ZK 5.x tertentu melebar/berantakan. Pengaturan sizing
			 * ringan di bawah tetap dipertahankan untuk menjaga shell tidak overflow. */
			String safeRoot = root == null ? "" : root;
			String minMobileText = String.valueOf(minMobile);
			String minDesktopText = String.valueOf(minDesktop);
			StringBuilder js = new StringBuilder();
			js.append("(function(){");
			/* popupCenter aslinya hanya ada di /js/common.js (template JSP).
			 * Banyak action ZK (GDrive, KasBesar, dll.) memanggilnya lewat
			 * evalJavaScript; tanpa definisi ini klik tombolnya tidak
			 * melakukan apa-apa (ReferenceError). */
			js.append("if(typeof window.popupCenter==='undefined'){window.popupCenter=function(o){try{var u=o&&o.url?o.url:'';var w=o&&o.w?o.w:900;var h=o&&o.h?o.h:600;var sl=window.screenLeft!==undefined?window.screenLeft:window.screenX||0;var st=window.screenTop!==undefined?window.screenTop:window.screenY||0;var wd=window.innerWidth||document.documentElement.clientWidth||screen.width;var ht=window.innerHeight||document.documentElement.clientHeight||screen.height;var l=Math.max(0,(wd-w)/2+sl);var t=Math.max(0,(ht-h)/2+st);var win=window.open(u,'_blank','scrollbars=yes,resizable=yes,width='+w+',height='+h+',top='+t+',left='+l);if(win&&window.focus){try{win.focus();}catch(e){}}return win;}catch(e){try{return window.open(o&&o.url?o.url:'');}catch(e2){return null;}}};}");
			/* Alat bantu scroll dihapus: scrollbar normal dipakai kembali
			 * (lihat juga pembersihan elemen lama bila masih tersisa). */
			js.append("function buatScrollAssist(){try{var lama=document.getElementById('aisScrollAssist');if(lama&&lama.parentNode){lama.parentNode.removeChild(lama);}}catch(e){}}");
			js.append("function px(v,d){v=String(v||'');var m=v.match(/\\d+/);return m?parseInt(m[0],10):d;}");
			js.append("function fixZkHidden(){try{var rs=document.querySelectorAll('tr.z-row,tr.z-listitem');for(var i=0;i<rs.length;i++){var r=rs[i];var attr=r.getAttribute?String(r.getAttribute('style')||''):'';if(attr.indexOf('display:none')>=0||attr.indexOf('display: none')>=0){if(r.style.display!=='none'){r.style.display='none';}}}}catch(e){}}");
			js.append("function fix(){try{var de=document.documentElement,bd=document.body;if(de){de.style.overflowX='hidden';de.style.width='100%';}if(bd){bd.style.overflowX='hidden';bd.style.width='100%';}var root=document.querySelector('.main-responsive-root');var mobile=(window.innerWidth||0)<=768;var min=mobile?")
					.append(minMobileText).append(":").append(minDesktopText).append(";if(min<20000){min=20000;}try{document.documentElement.style.setProperty('--ais-kanvas-tinggi',min+'px');}catch(e){}var header=document.querySelector('.main-responsive-header');var hh=header?Math.ceil(header.getBoundingClientRect().height):(mobile?230:112);var vh=window.innerHeight||document.documentElement.clientHeight||min;var fh=Math.max(min,vh-hh-8);var boxes=document.querySelectorAll('.main-responsive-frame,.main-responsive-tabbox,.main-responsive-tabpanels,.main-responsive-home-panel');for(var i=0;i<boxes.length;i++){boxes[i].style.width='100%';boxes[i].style.maxWidth='100%';boxes[i].style.minWidth='0';boxes[i].style.boxSizing='border-box';boxes[i].style.height=min+'px';boxes[i].style.minHeight=min+'px';boxes[i].style.maxHeight='none';boxes[i].style.overflowY='visible';}var centers=document.querySelectorAll('.main-responsive-center');for(var c=0;c<centers.length;c++){centers[c].style.minWidth='0';centers[c].style.boxSizing='border-box';centers[c].style.overflowX='hidden';centers[c].style.overflowY='visible';centers[c].style.maxWidth='none';}var mg=document.querySelector('.main-responsive-main-grid');if(mg&&mg.parentNode){var sh=mg.parentNode;sh.style.overflowY='auto';sh.style.overflowX='hidden';sh.className=String(sh.className||'').replace(' ais-scroll-host','');window.aisScrollHost=sh;buatScrollAssist();}var panels=document.querySelectorAll('.main-responsive-tabpanels,.main-responsive-tabpanels .z-tabpanel,.main-responsive-home-panel,.main-responsive-home-panel .z-tabpanel-cnt');for(var p=0;p<panels.length;p++){panels[p].style.maxWidth='100%';panels[p].style.overflowX='hidden';panels[p].style.overflowY='hidden';panels[p].style.boxSizing='border-box';}var dash=document.querySelectorAll('.main-dashboard-frame,.main-dashboard-content');for(var d=0;d<dash.length;d++){dash[d].style.width='100%';dash[d].style.maxWidth='100%';dash[d].style.minWidth='0';dash[d].style.boxSizing='border-box';dash[d].style.height='auto';dash[d].style.maxHeight='none';dash[d].style.minHeight='0';dash[d].style.overflowY='visible';dash[d].style.overflowX='hidden';}var grids=document.querySelectorAll('.main-responsive-home-panel .z-grid-body,.main-responsive-tabpanels .z-grid-body');for(var g=0;g<grids.length;g++){grids[g].style.maxWidth='100%';grids[g].style.overflowX='auto';}if(root){root.style.width='100%';root.style.maxWidth='100vw';root.style.overflowX='hidden';try{de.style.overflowY='hidden';if(bd){bd.style.overflowY='hidden';}}catch(e){}}try{if(window.zUtl){zUtl.fireSized();}}catch(e){}fixZkHidden();}catch(e){}}fix();setTimeout(fix,150);setTimeout(fix,700);setTimeout(fixZkHidden,800);try{if(!window.ecampusMainResponsiveResize){window.ecampusMainResponsiveResize=true;var old=window.onresize;window.onresize=function(){try{if(old){old();}}catch(e){}fix();fixZkHidden();};document.addEventListener('click',function(ev){var t=ev.target||ev.srcElement;var n=t;for(var i=0;n&&i<5;i++,n=n.parentNode){var cn=(' '+(n.className||'')+' ');if(cn.indexOf(' z-west')>=0||cn.indexOf('z-borderlayout-icon')>=0||cn.indexOf('z-west-collapsed')>=0||cn.indexOf('main-responsive-sidebar')>=0){setTimeout(fix,80);setTimeout(fix,350);break;}}},true);}}catch(e){}})();");
			js.append("(function(){function fixSubTabs(){try{var mobile=(window.innerWidth||0)<=768;var min=mobile?")
					.append(minMobileText).append(":").append(minDesktopText).append(";if(min<20000){min=20000;}var h=min+'px';var els=document.querySelectorAll('.main-dashboard-tabbox,.main-dashboard-tabpanels,.main-dashboard-tabpanel,.main-dashboard-frame .z-tabbox,.main-dashboard-frame .z-tabpanels,.main-dashboard-frame .z-tabpanel,.main-dashboard-frame .z-tabpanel-cnt,.main-dashboard-content .z-tabbox,.main-dashboard-content .z-tabpanels,.main-dashboard-content .z-tabpanel,.main-dashboard-content .z-tabpanel-cnt');for(var i=0;i<els.length;i++){var e=els[i];var cn=' '+(e.className||'')+' ';e.style.width='100%';e.style.maxWidth='100%';e.style.minWidth='0';e.style.height=h;e.style.minHeight=h;e.style.maxHeight='none';e.style.boxSizing='border-box';e.style.overflowX='hidden';if(cn.indexOf(' z-tabpanels ')>=0||cn.indexOf(' z-tabpanel ')>=0||cn.indexOf(' z-tabpanel-cnt ')>=0||cn.indexOf(' main-dashboard-tabpanels ')>=0||cn.indexOf(' main-dashboard-tabpanel ')>=0){e.style.overflowY='visible';}}}catch(e){}}fixSubTabs();setTimeout(fixSubTabs,200);setTimeout(fixSubTabs,900);try{if(!window.ecampusDashboardSubTabsResize){window.ecampusDashboardSubTabsResize=true;window.addEventListener('resize',function(){fixSubTabs();});document.addEventListener('click',function(){setTimeout(fixSubTabs,120);setTimeout(fixSubTabs,500);},true);}}catch(e){}})();");
			js.append("(function(){function fe(n){if(!n){return null;}for(var i=0;i<n.childNodes.length;i++){if(n.childNodes[i].nodeType===1){return n.childNodes[i];}}return null;}function has(e,c){return e&&(' '+(e.className||'')+' ').indexOf(' '+c+' ')>=0;}function px(v){var m=String(v||'').match(/-?\\d+/);return m?parseInt(m[0],10):0;}function full(e){if(!e){return;}e.style.width='100%';e.style.maxWidth='100%';e.style.minWidth='0';e.style.boxSizing='border-box';}function cave(r){if(!r){return null;}for(var i=0;i<r.childNodes.length;i++){var x=r.childNodes[i];if(x&&x.nodeType===1&&(' '+(x.className||'')+' ').indexOf('-body ')>=0){return x;}}return fe(r);}function fixBl(bl){try{full(bl);var pw=bl.clientWidth||(bl.parentNode?bl.parentNode.clientWidth:0)||px(bl.style.width);var ph=bl.clientHeight||(bl.parentNode?bl.parentNode.clientHeight:0)||px(bl.style.height);if(pw<260){var w=bl;while(w&&w.parentNode&&!has(w,'z-window-modal')&&!has(w,'z-window-overlapped')&&!has(w,'z-window-highlighted')&&!has(w,'z-window-popup')){w=w.parentNode;}if(w&&w.clientWidth){pw=w.clientWidth-8;}}var west=0,east=0,north=0,south=0,centers=[];for(var i=0;i<bl.childNodes.length;i++){var wrap=bl.childNodes[i];if(!wrap||wrap.nodeType!==1){continue;}var r=fe(wrap);if(!r){continue;}if(has(r,'z-west')){west=Math.max(west,r.offsetWidth||px(r.style.width));}else if(has(r,'z-east')){east=Math.max(east,r.offsetWidth||px(r.style.width));}else if(has(r,'z-north')){north=Math.max(north,r.offsetHeight||px(r.style.height));}else if(has(r,'z-south')){south=Math.max(south,r.offsetHeight||px(r.style.height));}else if(has(r,'z-center')){centers[centers.length]=r;}}for(var c=0;c<centers.length;c++){var ce=centers[c],cw=Math.max(0,pw-west-east),ch=Math.max(0,ph-north-south),cv=cave(ce);ce.style.left=west+'px';ce.style.top=north+'px';ce.style.width=cw+'px';if(ch>0){ce.style.height=ch+'px';}ce.style.boxSizing='border-box';ce.style.overflowX='hidden';if(cv){cv.style.width='100%';if(ch>0){cv.style.height=ch+'px';}cv.style.boxSizing='border-box';cv.style.overflowX='hidden';}}for(var j=0;j<bl.childNodes.length;j++){var ww=bl.childNodes[j];if(!ww||ww.nodeType!==1){continue;}var rr=fe(ww);if(!rr){continue;}if(has(rr,'z-south')||has(rr,'z-north')){rr.style.left='0px';rr.style.width=pw+'px';var rc=cave(rr);if(rc){rc.style.width='100%';}}else if(has(rr,'z-west')||has(rr,'z-east')){var rh=Math.max(0,ph-north-south);if(rh>0){rr.style.height=rh+'px';var rcv=cave(rr);if(rcv){rcv.style.height=rh+'px';}}}}}catch(e){}}function fixDialogs(){try{var wins=document.querySelectorAll('body>.z-window-modal,body>.z-window-highlighted,body>.z-window-overlapped,body>.z-window-popup');for(var i=0;i<wins.length;i++){var w=wins[i];w.style.maxWidth='calc(100vw - 32px)';w.style.boxSizing='border-box';var els=w.querySelectorAll('.z-window-modal-cnt,.z-window-modal-cnt-noborder,.z-window-highlighted-cnt,.z-window-highlighted-cnt-noborder,.z-window-overlapped-cnt,.z-window-overlapped-cnt-noborder,.z-window-popup-cnt,.z-window-popup-cnt-noborder,.z-tabbox,.z-tabs,.z-tabpanels,.z-tabpanel,.z-tabpanel-cnt,.z-toolbar');for(var e=0;e<els.length;e++){full(els[e]);els[e].style.overflowX='hidden';}var bls=w.querySelectorAll('.z-borderlayout');for(var b=0;b<bls.length;b++){fixBl(bls[b]);}}try{if(window.zUtl){zUtl.fireSized();}}catch(e){}}catch(e){}}fixDialogs();setTimeout(fixDialogs,100);setTimeout(fixDialogs,350);setTimeout(fixDialogs,900);try{if(!window.ecampusZkDialogLayoutFix){window.ecampusZkDialogLayoutFix=true;window.addEventListener('resize',fixDialogs);document.addEventListener('click',function(){setTimeout(fixDialogs,120);setTimeout(fixDialogs,450);setTimeout(fixDialogs,1000);},true);var n=0,t=setInterval(function(){fixDialogs();n++;if(n>20){clearInterval(t);}},500);}}catch(e){}})();");
			// Filter pencarian sekarang dipindahkan langsung di file ZUL ke addWindowPencarian.
			// Tidak memakai timer/DOM mover lagi agar komponen ZK langsung tersedia saat halaman dibuka.

			/* Combobox readonly bisa diketik untuk MENYARING pilihan.
			 * Nilai akhir tetap harus dipilih dari daftar: jika ketikan tidak
			 * cocok dengan item mana pun, nilai dikembalikan ke nilai semula
			 * saat blur, sehingga onChange/getSelectedItem di server aman. */
			js.append("(function(){if(window.ecampusComboSearch){return;}window.ecampusComboSearch=true;");
			js.append("function isCombo(t){return t&&String(t.tagName).toLowerCase()==='input'&&(' '+(t.className||'')+' ').indexOf(' z-combobox-inp ')>=0;}");
			js.append("function pp(t){var i=String(t.id||'');if(i.length>5&&i.substring(i.length-5)==='-real'){return document.getElementById(i.substring(0,i.length-5)+'-pp');}return null;}");
			js.append("function rows(p){if(!p){return [];}var out=[],trs=p.getElementsByTagName('tr');for(var i=0;i<trs.length;i++){if((' '+(trs[i].className||'')+' ').indexOf(' z-comboitem ')>=0){out[out.length]=trs[i];}}return out;}");
			js.append("function txt(r){return String(r.innerText||r.textContent||'').replace(/\\s+/g,' ').replace(/^\\s+|\\s+$/g,'');}");
			js.append("function setHide(r,h){var cn=String(r.className||'').replace(/\\s*ecsb-hide\\s*/g,' ');r.className=h?(cn+' ecsb-hide'):cn;}");
			js.append("function filter(t){try{var p=pp(t);if(!p){return;}var q=String(t.value||'').toLowerCase();var rs=rows(p);for(var i=0;i<rs.length;i++){setHide(rs[i],q.length>0&&txt(rs[i]).toLowerCase().indexOf(q)<0);}}catch(e){}}");
			js.append("function reset(t){try{var p=pp(t);if(!p){return;}var rs=rows(p);for(var i=0;i<rs.length;i++){setHide(rs[i],false);}}catch(e){}}");
			js.append("document.addEventListener('focus',function(ev){var t=ev.target||ev.srcElement;if(!isCombo(t)){return;}if(t.readOnly){t.readOnly=false;t.setAttribute('data-ecsb','1');}if(t.getAttribute('data-ecsb')==='1'){t.setAttribute('data-ecsb-orig',String(t.value||''));}},true);");
			js.append("document.addEventListener('input',function(ev){var t=ev.target||ev.srcElement;if(isCombo(t)&&t.getAttribute('data-ecsb')==='1'){filter(t);}},true);");
			js.append("document.addEventListener('keyup',function(ev){var t=ev.target||ev.srcElement;if(isCombo(t)&&t.getAttribute('data-ecsb')==='1'){filter(t);}},true);");
			js.append("document.addEventListener('blur',function(ev){var t=ev.target||ev.srcElement;if(!isCombo(t)||t.getAttribute('data-ecsb')!=='1'){return;}");
			js.append("setTimeout(function(){try{var v=String(t.value||'');var p=pp(t);var ok=v.length===0;if(p&&!ok){var rs=rows(p);for(var i=0;i<rs.length;i++){if(txt(rs[i]).toLowerCase()===v.toLowerCase()){ok=true;break;}}}");
			js.append("if(!ok){t.value=String(t.getAttribute('data-ecsb-orig')||'');}reset(t);}catch(e){}},220);},true);");
			js.append("})();");

			/* Dropdown otomatis tombol modul (rowHeader2) di layar sempit.
			 * Strip hbox ZK tidak diubah strukturnya; JS hanya menambah tombol
			 * toggle <button class="ais-modul-dd-toggle"> di depan strip dan
			 * memasang/melepas class ais-modul-dd-buka pada sel induknya.
			 * Klik toggle ditangani DELEGASI di document (fase capture), bukan
			 * onclick langsung pada elemen, supaya tetap hidup walau ZK
			 * membangun ulang DOM header setelah ClientInfo/Timer.
			 * Panel memakai position:fixed dengan koordinat dihitung dari
			 * tombol toggle agar tidak terpotong ancestor overflow:hidden.
			 * Perilaku visual diatur CSS media query (<=900px) di css_utama.css
			 * blok "MODUL DROPDOWN OTOMATIS". */
			js.append("(function(){");
			js.append("function cariStrip(){return document.querySelector('.main-responsive-module-strip');}");
			js.append("function tutup(){try{var h=document.querySelector('.ais-modul-dd-buka');if(h){h.className=String(h.className||'').replace(/\\s*ais-modul-dd-buka\\s*/g,' ');}var s=cariStrip();if(s){s.style.top='';s.style.left='';}}catch(e){}}");
			js.append("function buka(tgl){try{var h=tgl.parentNode,s=cariStrip();if(!h||!s){return;}var r=tgl.getBoundingClientRect();var vw=window.innerWidth||document.documentElement.clientWidth||320;var lf=Math.round(r.left);if(lf+264>vw){lf=Math.max(6,vw-270);}s.style.top=Math.round(r.bottom+4)+'px';s.style.left=lf+'px';if((' '+(h.className||'')+' ').indexOf(' ais-modul-dd-buka ')<0){h.className=h.className+' ais-modul-dd-buka';}}catch(e){}}");
			/* iOS Safari: delegasi 'click' di document TIDAK selalu terpicu untuk tombol
			 * toggle → di iPhone menu tidak terbuka, padahal di Android jalan. Solusi: SATU
			 * fungsi toggle bersama (buka/tutup) + DEBOUNCE, lalu dipasang di DUA jalur —
			 * delegasi document (andal di Android) DAN onclick langsung pada tombol (andal di
			 * iOS). Debounce 250ms mencegah dobel-picu (buka lalu langsung tutup) bila kedua
			 * jalur sama-sama jalan pada satu ketukan. */
			js.append("var _ddLast=0;function toggleDd(tgl){try{var now=(new Date()).getTime();if(now-_ddLast<250){return;}_ddLast=now;if(document.querySelector('.ais-modul-dd-buka')){tutup();}else{buka(tgl);}}catch(e){}}");
			/* Label tombol: "Menu" di HP (<=768px), "Modul" di tablet sempit. */
			js.append("function labelTgl(){return ((window.innerWidth||document.documentElement.clientWidth||320)<=768)?'&#9776;&nbsp;Menu&nbsp;&#9662;':'&#9776;&nbsp;Modul&nbsp;&#9662;';}");
			js.append("function siapkan(){try{var s=cariStrip();if(!s){return;}var host=s.parentNode;if(!host){return;}var ada=null;for(var i=0;i<host.childNodes.length;i++){var c=host.childNodes[i];if(c&&c.nodeType===1&&(' '+(c.className||'')+' ').indexOf(' ais-modul-dd-toggle ')>=0){ada=c;break;}}");
			js.append("if(!ada){ada=document.createElement('button');ada.type='button';ada.className='ais-modul-dd-toggle';host.insertBefore(ada,s);}");
			js.append("if(ada.innerHTML!==labelTgl()){ada.innerHTML=labelTgl();}");
			/* iOS: pasang onclick LANGSUNG pada tombol (reliabel di Safari). Dipasang ulang
			 * tiap siapkan() sehingga tetap ada walau ZK membangun ulang DOM header. */
			js.append("try{ada.onclick=function(ev){try{ev=ev||window.event;if(ev&&ev.preventDefault){ev.preventDefault();}toggleDd(ada);}catch(e2){}return false;};}catch(e){}");
			js.append("if((' '+(host.className||'')+' ').indexOf(' ais-modul-dd-host ')<0){host.className=host.className+' ais-modul-dd-host';}}catch(e){}}");
			js.append("siapkan();setTimeout(siapkan,400);setTimeout(siapkan,1500);");
			js.append("try{if(!window.ecampusModulDropdown){window.ecampusModulDropdown=true;");
			js.append("setInterval(siapkan,1200);");
			/* Klik toggle: buka/tutup. Klik item modul di dalam panel: biarkan event
			 * onClick ZK terkirim dulu, panel ditutup setelah jeda kecil.
			 * Klik di luar: tutup langsung. */
			js.append("document.addEventListener('click',function(ev){try{var x=ev.target||ev.srcElement;var tgl=null,dlm=false;for(var i=0;x&&i<14;i++,x=x.parentNode){var cn=' '+(x.className||'')+' ';if(cn.indexOf(' ais-modul-dd-toggle ')>=0){tgl=x;break;}if(cn.indexOf(' main-responsive-module-strip ')>=0){dlm=true;break;}}");
			js.append("if(tgl){if(ev.preventDefault){ev.preventDefault();}toggleDd(tgl);return;}");
			js.append("if(dlm){setTimeout(tutup,160);return;}tutup();}catch(e){}},true);");
			/* Scroll halaman menutup panel (posisi fixed tidak ikut bergeser);
			 * scroll di dalam panel sendiri dibiarkan. */
			js.append("document.addEventListener('scroll',function(ev){try{var t=ev.target;if(t&&t.nodeType===1){for(var i=0;t&&i<8;i++,t=t.parentNode){if(t.nodeType===1&&(' '+(t.className||'')+' ').indexOf(' main-responsive-module-strip ')>=0){return;}}}tutup();}catch(e){}},true);");
			js.append("window.addEventListener('resize',function(){try{tutup();siapkan();}catch(e){}});");
			js.append("}}catch(e){}");
			js.append("})();");

			/* "Pencarian Lebih Lanjut" generik untuk SEMUA popup banbox AmbilData*.
			 * Grid pencarian di area North bandpopup diciutkan: hanya baris filter
			 * PERTAMA (paling umum: kode/nama/NIM) yang tampil, baris lain
			 * disembunyikan di balik tombol toggle - meniru pola tombol
			 * "Pencarian Lebih Lanjut" pada halaman *.zul (agama.zul dkk).
			 * Dilakukan di DOM (bukan per class Java) supaya autowire dan
			 * listener ZK tetap hidup; CSS: blok "PENCARIAN LANJUT BANBOX". */
			js.append("(function(){");
			js.append("function siapkanBp(){try{var bps=document.querySelectorAll('.z-bandpopup');for(var i=0;i<bps.length;i++){var bp=bps[i];if(bp.getAttribute('data-ais-cari')==='1'){continue;}var north=bp.querySelector('.z-north');if(!north){bp.setAttribute('data-ais-cari','1');continue;}var grid=north.querySelector('.z-grid');if(!grid){bp.setAttribute('data-ais-cari','1');continue;}var trs=grid.querySelectorAll('tr.z-row');if(trs.length<2){bp.setAttribute('data-ais-cari','1');continue;}");
			js.append("for(var r=1;r<trs.length;r++){trs[r].className+=' ais-bp-baris-lanjut';}");
			js.append("grid.className+=' ais-bp-cari-grid ais-bp-cari-sembunyi';");
			js.append("var btn=document.createElement('button');btn.type='button';btn.className='ais-bp-cari-toggle';btn.innerHTML='&#9662;&nbsp;Pencarian Lebih Lanjut';");
			js.append("if(grid.parentNode){grid.parentNode.insertBefore(btn,grid.nextSibling);}bp.setAttribute('data-ais-cari','1');}}catch(e){}}");
			js.append("siapkanBp();setTimeout(siapkanBp,400);");
			js.append("try{if(!window.ecampusBanboxCariLanjut){window.ecampusBanboxCariLanjut=true;");
			/* Toggle ditangani delegasi (tahan re-render); klik lain memicu scan
			 * ulang karena bandpopup dibuat dinamis saat banbox dibuka. */
			js.append("document.addEventListener('click',function(ev){try{var x=ev.target||ev.srcElement;var tg=null;for(var i=0;x&&i<8;i++,x=x.parentNode){if(x.nodeType===1&&(' '+(x.className||'')+' ').indexOf(' ais-bp-cari-toggle ')>=0){tg=x;break;}}");
			js.append("if(tg){if(ev.preventDefault){ev.preventDefault();}var g=tg.previousSibling;while(g&&(g.nodeType!==1||(' '+(g.className||'')+' ').indexOf(' ais-bp-cari-grid ')<0)){g=g.previousSibling;}");
			js.append("if(g){var cn=String(g.className||'');var buka=cn.indexOf('ais-bp-cari-sembunyi')>=0;g.className=buka?cn.replace(/\\s*ais-bp-cari-sembunyi\\s*/g,' '):(cn+' ais-bp-cari-sembunyi');tg.innerHTML=buka?'&#9652;&nbsp;Sembunyikan Pencarian':'&#9662;&nbsp;Pencarian Lebih Lanjut';try{if(window.zUtl){zUtl.fireSized();}}catch(e){}}return;}");
			js.append("setTimeout(siapkanBp,250);setTimeout(siapkanBp,800);}catch(e){}},true);");
			js.append("var n=0,t=setInterval(function(){siapkanBp();n++;if(n>30){clearInterval(t);}},900);");
			js.append("}}catch(e){}");
			js.append("})();");

			/* PENUTUP POPUP FILTER "Pencarian Lebih Lanjut" (addWindowPencarian).
			 *
			 * Penyebab popup tidak bisa ditutup:
			 * (1) ZK 5 hanya menutup popup saat click pada widget ZK lain;
			 *     klik area kosong/non-widget (body, table, dsb) tidak memicu
			 *     close bawaan ZK.
			 * (2) Versi lama memakai zk.Widget.$(p) — API ZK 9; di ZK 5 harus
			 *     zk.$(p).
			 * (3) Deteksi popup terbuka via style.display tidak selalu akurat
			 *     karena ZK 5 kadang memakai visibility/left:-9999.
			 *
			 * SOLUSI: backdrop div transparan fullscreen yang duduk di bawah
			 * popup (z-index popup - 1). Klik backdrop = tutup popup + hapus
			 * backdrop. Backdrop dibuat setiap kali popup terdeteksi terbuka
			 * via polling 120ms (cepat, ringan). Juga handle:
			 * - tombol Cari/Search/Tampilkan dalam popup → tutup berjeda 300ms
			 * - Escape key → tutup popup
			 * - tombol open diklik lagi saat popup terbuka → tutup dulu. */
			js.append("(function(){");
			/* tutup() memanggil API ZK 5 yang benar: zk.$(domElement) */
			js.append("function tutupP(p){if(!p){return;}try{var w=window.zk?zk.$(p):null;if(w&&typeof w.close==='function'){w.close();return;}}catch(e){}try{p.style.display='none';}catch(e2){}}");
			js.append("function popupTerbuka(){var ps=document.querySelectorAll('.z-popup.ecampus-zul-filter-window');for(var i=0;i<ps.length;i++){var p=ps[i];if(p.offsetWidth>0&&p.offsetHeight>0){return p;}}return null;}");
			/* backdrop: div transparan fullscreen, z-index di bawah popup */
			js.append("var _bd=null,_lastP=null;");
			js.append("function hapusBd(){if(_bd&&_bd.parentNode){_bd.parentNode.removeChild(_bd);}_bd=null;_lastP=null;}");
			js.append("function buatBd(p){hapusBd();var zi=parseInt(p.style.zIndex||'',10)||1000;var d=document.createElement('div');d.id='ais-filter-backdrop';d.style.cssText='position:fixed;top:0;left:0;width:100%;height:100%;z-index:'+(zi-1)+';background:transparent;cursor:default;';");
			/* Klik backdrop: tutup popup */
			js.append("d.onclick=function(ev){if(ev&&ev.stopPropagation){ev.stopPropagation();}tutupP(p);hapusBd();};");
			js.append("document.body.appendChild(d);_bd=d;_lastP=p;}");
			/* Poll setiap 120ms: deteksi popup buka/tutup */
			js.append("if(!window._aisFilterPollRunning){window._aisFilterPollRunning=true;");
			js.append("setInterval(function(){var p=popupTerbuka();if(p&&p!==_lastP){buatBd(p);}else if(!p&&_lastP){hapusBd();}},120);}");
			/* Semua toolbarbutton/button dalam popup → tutup setelah event ZK terkirim */
			js.append("document.addEventListener('click',function(ev){try{var t=ev.target||ev.srcElement;var p2=_lastP;");
			js.append("for(var n=t,i=0;n&&n!==document&&i<20;n=n.parentNode,i++){var c=' '+(n.className||'')+' ';");
			js.append("if(c.indexOf(' ecampus-zul-filter-window ')>=0){break;}");
			js.append("if(c.indexOf(' z-toolbarbutton ')>=0||c.indexOf(' z-button ')>=0||String(n.tagName||'').toLowerCase()==='button'){");
			js.append("if(p2&&p2.offsetWidth>0&&dalamPopup(n,p2)){var pp2=p2;setTimeout(function(){tutupP(pp2);hapusBd();},350);}break;}}");
			js.append("if(p2&&p2.offsetWidth>0&&!dalamPopup(t,p2)){tutupP(p2);hapusBd();}}catch(e){}},true);");
			/* Helper: cek apakah t di dalam popup atau float anak-nya */
			js.append("function dalamPopup(t,p){for(var n=t;n&&n!==document;n=n.parentNode){if(n===p){return true;}var c=' '+(n.className||'')+' ';if(c.indexOf('-pp ')>=0||c.indexOf(' z-calendar ')>=0||c.indexOf(' z-menu-popup ')>=0||c.indexOf(' z-errbox ')>=0){return true;}}return false;}");
			/* Tombol open diklik saat popup sudah terbuka → toggle tutup */
			js.append("document.addEventListener('click',function(ev){try{var t=ev.target||ev.srcElement;for(var n=t;n&&n!==document;n=n.parentNode){if((' '+(n.className||'')+' ').indexOf(' ecampus-zul-filter-open-button ')>=0){if(_lastP&&_lastP.offsetWidth>0){tutupP(_lastP);hapusBd();}break;}}}catch(e){}},true);");
			/* Escape key → tutup popup */
			js.append("document.addEventListener('keydown',function(ev){try{if((ev.keyCode||ev.which)===27){var p=_lastP;if(p){tutupP(p);hapusBd();}}}catch(e){}},true);");
			/* Enter pada field dalam popup → submit + tutup berjeda */
			js.append("document.addEventListener('keydown',function(ev){try{if((ev.keyCode||ev.which)!==13){return;}var t=ev.target||ev.srcElement;var tag=String(t.tagName||'').toLowerCase();if(tag!=='input'){return;}var p=_lastP;if(p&&p.offsetWidth>0&&dalamPopup(t,p)){setTimeout(function(){tutupP(p);hapusBd();},450);}}catch(e){}},true);");
			js.append("})();");

				Clients.evalJavaScript(js.toString());
		} catch (Exception e) {
			showError(e);
		}
	}

	private static String buildSmartFilterPopupScript() {
		StringBuilder js = new StringBuilder();
		js.append("(function(){");
		js.append("function cls(e,c){return e&&(' '+(e.className||'')+' ').indexOf(' '+c+' ')>=0;}");
		js.append("function text(e){return String((e&& (e.innerText||e.textContent) || '')).toLowerCase();}");
		js.append("function controls(e){return e?e.querySelectorAll('input,select,textarea,.z-textbox,.z-combobox,.z-bandbox,.z-datebox,.z-checkbox,.z-radio').length:0;}");
		js.append("function buttons(e){return e?e.querySelectorAll('.z-toolbarbutton,.z-button,button,a.z-menu-item-cnt').length:0;}");
		js.append("function excluded(e){for(var n=e;n&&n!==document;n=n.parentNode){var c=' '+(n.className||'')+' ';if(c.indexOf(' main-responsive-header ')>=0||c.indexOf(' main-announcement-panel ')>=0||c.indexOf(' main-responsive-menu-popup ')>=0||c.indexOf(' z-menu-popup ')>=0||c.indexOf(' ecampus-filter-popup-window ')>=0){return true;}}return false;}");
		js.append("function getNorthBody(n){if(!n){return null;}var b=n.querySelector('.z-north-body');if(b){return b;}for(var i=0;i<n.childNodes.length;i++){var x=n.childNodes[i];if(x&&x.nodeType===1&&String(x.id||'').indexOf('-cave')>0){return x;}}return n;}");
		js.append("function isFilterNorth(n){if(!n||n.getAttribute('data-ecampus-filter-popup')==='1'||excluded(n)){return false;}var ncx=' '+(n.className||'')+' ';if(ncx.indexOf('ecampus-zul-filter-launcher')>=0||ncx.indexOf('ecampus-inline-filter')>=0){return false;}if(n.querySelector&&n.querySelector('.ecampus-zul-filter-launcher,.ecampus-zul-quick-search,.ecampus-inline-filter')){return false;}var b=getNorthBody(n);var r=n.getBoundingClientRect?n.getBoundingClientRect():{width:n.offsetWidth,height:n.offsetHeight};if(r.width<360||r.height<38||r.height>520){return false;}var c=controls(b),bt=buttons(b),t=text(b);var keys=['cari','search','filter','download','upload','tambah','tanggal','aktif','status','fakultas','prodi','sekolah','satuan kerja','nama','nomor','keterangan'];var k=0;for(var i=0;i<keys.length;i++){if(t.indexOf(keys[i])>=0){k++;}}return (c>=1&&k>=1)||(c>=2)||(bt>=2&&k>=1);}");
		js.append("function fire(){try{if(window.zUtl){zUtl.fireSized();}}catch(e){}try{if(document.createEvent){var ev=document.createEvent('HTMLEvents');ev.initEvent('resize',true,false);window.dispatchEvent(ev);}else if(document.createEventObject){window.fireEvent('onresize');}}catch(e){}}");
		js.append("function mkButton(html,klass){var b=document.createElement('button');b.type='button';b.className=klass;b.innerHTML=html;return b;}");
		js.append("function createPopup(title){var mask=document.createElement('div');mask.className='ecampus-filter-popup-mask';mask.style.display='none';var win=document.createElement('div');win.className='ecampus-filter-popup-window';var head=document.createElement('div');head.className='ecampus-filter-popup-head';var ttl=document.createElement('div');ttl.className='ecampus-filter-popup-title';ttl.innerHTML=title||'Filter Pencarian';var x=mkButton('&times;','ecampus-filter-popup-x');head.appendChild(ttl);head.appendChild(x);var info=document.createElement('div');info.className='ecampus-filter-popup-info';info.innerHTML='Gunakan filter ini untuk mempersempit data. Setelah selesai, klik Cari atau Tutup.';var body=document.createElement('div');body.className='ecampus-filter-popup-body';var foot=document.createElement('div');foot.className='ecampus-filter-popup-foot';var close=mkButton('Tutup','ecampus-filter-popup-close');foot.appendChild(close);win.appendChild(head);win.appendChild(info);win.appendChild(body);win.appendChild(foot);mask.appendChild(win);document.body.appendChild(mask);function hide(){mask.style.display='none';document.body.className=String(document.body.className||'').replace(' ecampus-filter-popup-open','');fire();}function show(){mask.style.display='flex';if((' '+document.body.className+' ').indexOf(' ecampus-filter-popup-open ')<0){document.body.className=(document.body.className||'')+' ecampus-filter-popup-open';}setTimeout(fire,80);setTimeout(fire,300);}x.onclick=hide;close.onclick=hide;mask.onclick=function(ev){ev=ev||window.event;var t=ev.target||ev.srcElement;if(t===mask){hide();}};body.onclick=function(ev){var t=ev.target||ev.srcElement;for(var i=0;t&&i<5;i++,t=t.parentNode){var cn=' '+(t.className||'')+' ';var tx=String(t.innerText||t.textContent||'').toLowerCase();if((cn.indexOf(' z-toolbarbutton ')>=0||cn.indexOf(' z-button ')>=0||String(t.tagName).toLowerCase()==='button')&&(tx.indexOf('cari')>=0||tx.indexOf('search')>=0||tx.indexOf('tampilkan')>=0)){setTimeout(hide,450);break;}}};return {mask:mask,body:body,show:show,hide:hide};}");
		js.append("function findCari(body){var btns=body.querySelectorAll('.z-toolbarbutton,.z-button,button');for(var i=0;i<btns.length;i++){var tx=String(btns[i].innerText||btns[i].textContent||'').toLowerCase();if(tx.indexOf('cari')>=0||tx.indexOf('search')>=0||tx.indexOf('tampilkan')>=0){return btns[i];}}return null;}");
		js.append("function pasangEnter(popup){popup.body.onkeydown=function(ev){ev=ev||window.event;if((ev.keyCode||ev.which)!==13){return;}var t=ev.target||ev.srcElement;var tag=String(t.tagName||'').toLowerCase();if(tag!=='input'){return;}var b=findCari(popup.body);if(b){try{b.click();var inner=b.querySelector?b.querySelector('.z-toolbarbutton-cnt'):null;if(inner){inner.click();}}catch(e){}}setTimeout(popup.hide,500);};}");
		js.append("function make(n){try{if(!n||n.getAttribute('data-ecampus-filter-popup')==='1'){return;}var src=getNorthBody(n);if(!src){return;}n.setAttribute('data-ecampus-filter-popup','1');var popup=createPopup('Filter Pencarian');pasangEnter(popup);var moved=[];while(src.firstChild){moved[moved.length]=src.firstChild;popup.body.appendChild(src.firstChild);}var bar=document.createElement('div');bar.className='ecampus-filter-popup-togglebar';var btn=mkButton('&#128269; Buka Filter Pencarian','ecampus-filter-popup-toggle-button');bar.appendChild(btn);src.appendChild(bar);btn.onclick=function(){popup.show();return false;};try{n.style.setProperty('height','46px','important');n.style.setProperty('min-height','46px','important');n.style.setProperty('max-height','46px','important');n.style.setProperty('overflow','visible','important');src.style.setProperty('height','46px','important');src.style.setProperty('min-height','46px','important');src.style.setProperty('overflow','visible','important');}catch(e){n.style.height='46px';src.style.height='46px';}setTimeout(fire,80);setTimeout(fire,300);}catch(e){}}");
		js.append("function scan(){try{var ns=document.querySelectorAll('.z-north,.z-north-noborder');for(var i=0;i<ns.length;i++){if(isFilterNorth(ns[i])){make(ns[i]);}}}catch(e){}}");
		js.append("scan();setTimeout(scan,250);setTimeout(scan,900);setTimeout(scan,1800);try{if(!window.ecampusSmartFilterPopup){window.ecampusSmartFilterPopup=true;var n=0,t=setInterval(function(){scan();n++;if(n>25){clearInterval(t);}},800);document.addEventListener('click',function(){setTimeout(scan,180);},true);window.addEventListener('resize',scan);}}catch(e){}");
		js.append("})();");
		return js.toString();
	}

	private static String js(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "");
	}

	private static void showError(Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/maintenance/MainStyleHelper.java:650");
		}
	}
}
