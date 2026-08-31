package ais.action.master.generic.v2;

import java.io.Serializable;

/**
 * Objek data (POJO serializable) yang mendeskripsikan satu kolom/properti entitas pada framework
 * CRUD generik {@code ais.action.master.generic.v2}. Satu instance mewakili metadata lengkap sebuah
 * field untuk kebutuhan render UI otomatis dan validasi: {@code property}/{@code label}/
 * {@code javaType} adalah identitas dasar; {@code editorType} menentukan komponen input (default
 * {@code "text"}); flag visibilitas ({@code tableVisible}, {@code quickFilter}, {@code detailVisible})
 * mengatur di layar mana kolom muncul; flag hak akses per-field ({@code readable}, {@code createable},
 * {@code updateable}, {@code required}) mengatur operasi apa yang diizinkan pada kolom tersebut
 * secara independen dari hak akses entitas; {@code sortable}/{@code searchable}/{@code exportable}
 * mengatur kapabilitas query dan ekspor; {@code sensitive} menandai kolom berisi data sensitif (mis.
 * untuk masking); {@code restoreable} menandai kolom dapat dipulihkan setelah soft-delete;
 * {@code relationEntityKey}/{@code relationDisplayProperty}/{@code relationSearchProperties}
 * mendeskripsikan relasi ke entitas lain (untuk field bertipe lookup/dropdown); {@code enumValues}
 * menyediakan daftar nilai tetap untuk field enum; dan {@code position} menentukan urutan tampil
 * (default 9999, ditempatkan paling akhir bila tidak diatur eksplisit). Seluruh metode adalah
 * getter/setter sederhana atas field-field di atas.
 */
public class GenericCrudFieldDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String property;
    private String label;
    private String javaType;
    private String editorType = "text";
    private boolean tableVisible;
    private boolean quickFilter;
    private boolean detailVisible = true;
    private boolean readable = true;
    private boolean createable;
    private boolean updateable;
    private boolean required;
    private boolean sortable;
    private boolean searchable;
    private boolean exportable = true;
    private boolean sensitive;
    private boolean restoreable;
    private String relationEntityKey;
    private String relationDisplayProperty;
    private String relationSearchProperties;
    private String[] enumValues;
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
    public boolean isQuickFilter() { return quickFilter; }
    public void setQuickFilter(boolean value) { quickFilter = value; }
    public boolean isDetailVisible() { return detailVisible; }
    public void setDetailVisible(boolean value) { detailVisible = value; }
    public boolean isReadable() { return readable; }
    public void setReadable(boolean value) { readable = value; }
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
    public String getRelationEntityKey() { return relationEntityKey; }
    public void setRelationEntityKey(String value) { relationEntityKey = value; }
    public String getRelationDisplayProperty() { return relationDisplayProperty; }
    public void setRelationDisplayProperty(String value) { relationDisplayProperty = value; }
    public String getRelationSearchProperties() { return relationSearchProperties; }
    public void setRelationSearchProperties(String value) { relationSearchProperties = value; }
    public String[] getEnumValues() { return enumValues; }
    public void setEnumValues(String[] value) { enumValues = value; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
