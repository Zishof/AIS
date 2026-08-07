package ais.action.master.generic.v2;

import java.io.Serializable;

public class GenericCrudFieldDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String property;
    private String label;
    private String javaType;
    private String editorType = "text";
    private boolean tableVisible;
    private boolean detailVisible = true;
    private boolean createable;
    private boolean updateable;
    private boolean required;
    private boolean sortable;
    private boolean searchable;
    private boolean exportable = true;
    private boolean sensitive;
    private boolean restoreable;
    private int position = 9999;

    public GenericCrudFieldDefinition() { }
    public GenericCrudFieldDefinition(String property, String label, String javaType) {
        this.property = property;
        this.label = label;
        this.javaType = javaType;
    }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getJavaType() { return javaType; }
    public void setJavaType(String javaType) { this.javaType = javaType; }
    public String getEditorType() { return editorType; }
    public void setEditorType(String editorType) { this.editorType = editorType; }
    public boolean isTableVisible() { return tableVisible; }
    public void setTableVisible(boolean value) { tableVisible = value; }
    public boolean isDetailVisible() { return detailVisible; }
    public void setDetailVisible(boolean value) { detailVisible = value; }
    public boolean isCreateable() { return createable; }
    public void setCreateable(boolean value) { createable = value; }
    public boolean isUpdateable() { return updateable; }
    public void setUpdateable(boolean value) { updateable = value; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean value) { required = value; }
    public boolean isSortable() { return sortable; }
    public void setSortable(boolean value) { sortable = value; }
    public boolean isSearchable() { return searchable; }
    public void setSearchable(boolean value) { searchable = value; }
    public boolean isExportable() { return exportable; }
    public void setExportable(boolean value) { exportable = value; }
    public boolean isSensitive() { return sensitive; }
    public void setSensitive(boolean value) { sensitive = value; }
    public boolean isRestoreable() { return restoreable; }
    public void setRestoreable(boolean value) { restoreable = value; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
