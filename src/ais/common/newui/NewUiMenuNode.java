package ais.common.newui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO node menu New UI (sudah difilter server-side & lepas dari entity lazy).
 *
 * <p>Dipakai bersama oleh sidebar, command palette, breadcrumb, dan route guard,
 * sehingga sumber navigasi satu dan konsisten. Sengaja tidak menyimpan entity
 * {@link ais.database.model.Menu} agar aman dirender setelah session ditutup.</p>
 *
 * <p>Kompatibel Java 1.6.</p>
 */
public class NewUiMenuNode implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long menuId;
    private String label;
    private String legacyUrl;      // URL legacy /baru (selalu tersedia bila menu punya url)
    private String newUiModule;    // modul New UI hasil registry, null bila UNMAPPED
    private String newUiPage;      // page New UI hasil registry, null bila UNMAPPED
    private Integer nomorUrut;
    private Long root;
    private Long child;
    private boolean group;         // parent tanpa URL yang menampung child (heading/collapsible)
    private boolean active;        // node yang sedang dibuka
    private NewUiPermission permission = NewUiPermission.none();
    private List<NewUiMenuNode> children = new ArrayList<NewUiMenuNode>();

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLegacyUrl() {
        return legacyUrl;
    }

    public void setLegacyUrl(String legacyUrl) {
        this.legacyUrl = legacyUrl;
    }

    public String getNewUiModule() {
        return newUiModule;
    }

    public void setNewUiModule(String newUiModule) {
        this.newUiModule = newUiModule;
    }

    public String getNewUiPage() {
        return newUiPage;
    }

    public void setNewUiPage(String newUiPage) {
        this.newUiPage = newUiPage;
    }

    public Integer getNomorUrut() {
        return nomorUrut;
    }

    public void setNomorUrut(Integer nomorUrut) {
        this.nomorUrut = nomorUrut;
    }

    public Long getRoot() {
        return root;
    }

    public void setRoot(Long root) {
        this.root = root;
    }

    public Long getChild() {
        return child;
    }

    public void setChild(Long child) {
        this.child = child;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public NewUiPermission getPermission() {
        return permission;
    }

    public void setPermission(NewUiPermission permission) {
        this.permission = permission == null ? NewUiPermission.none() : permission;
    }

    public List<NewUiMenuNode> getChildren() {
        return children;
    }

    public void setChildren(List<NewUiMenuNode> children) {
        this.children = children == null ? new ArrayList<NewUiMenuNode>() : children;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /** true bila node ini punya route New UI yang termapping. */
    public boolean isMappedToNewUi() {
        return newUiModule != null && newUiModule.length() > 0;
    }

    /** true bila node sendiri boleh dibaca. */
    public boolean isReadable() {
        return permission != null && permission.isCanRead();
    }
}
