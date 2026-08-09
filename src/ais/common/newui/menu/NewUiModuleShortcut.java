package ais.common.newui.menu;

import java.io.Serializable;

/** Kartu modul pada beranda New UI yang sudah lolos flag role dan RBAC menu. */
public class NewUiModuleShortcut implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String key;
    private final String label;
    private final String description;
    private final String icon;
    private final int order;
    private final NewUiHybridMenuNode target;
    public NewUiModuleShortcut(String key, String label, String description, String icon, int order, NewUiHybridMenuNode target) {
        this.key=key; this.label=label; this.description=description; this.icon=icon; this.order=order; this.target=target;
    }
    public String getKey(){return key;}
    public String getLabel(){return label;}
    public String getDescription(){return description;}
    public String getIcon(){return icon;}
    public int getOrder(){return order;}
    public NewUiHybridMenuNode getTarget(){return target;}
    public String getSearchText(){return label+" "+description+" "+(target==null?"":target.getSearchText());}
}
