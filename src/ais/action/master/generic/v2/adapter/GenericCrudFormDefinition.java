package ais.action.master.generic.v2.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model data (bean) yang mendeskripsikan tata letak dan perilaku satu form pada kerangka kerja CRUD
 * generik ({@code generic/v2}): kunci form, mode (mis. create/update/view), judul, strategi
 * penyimpanan ({@code saveStrategy}), penjaga perubahan belum tersimpan ({@code unsavedChangeGuard},
 * default aktif), serta daftar tab, section, dan action yang membentuk form. Murni kumpulan
 * getter/setter tanpa logika — dikonsumsi oleh renderer form generik untuk membangun tampilan ZK
 * secara dinamis dari definisi ini. Setter {@code tabs}/{@code sections}/{@code actions} selalu
 * menjamin daftar tidak pernah {@code null} (mengganti dengan {@link ArrayList} kosong bila
 * diberi {@code null}).
 */
@SuppressWarnings("rawtypes")
public class GenericCrudFormDefinition implements Serializable {
    private static final long serialVersionUID = 1L;
    private String formKey;
    private String mode;
    private String title;
    private String saveStrategy;
    private boolean unsavedChangeGuard = true;
    private List tabs = new ArrayList();
    private List sections = new ArrayList();
    private List actions = new ArrayList();
    public String getFormKey() { return formKey; }
    public void setFormKey(String value) { formKey = value; }
    public String getMode() { return mode; }
    public void setMode(String value) { mode = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getSaveStrategy() { return saveStrategy; }
    public void setSaveStrategy(String value) { saveStrategy = value; }
    public boolean isUnsavedChangeGuard() { return unsavedChangeGuard; }
    public void setUnsavedChangeGuard(boolean value) { unsavedChangeGuard = value; }
    public List getTabs() { return tabs; }
    public void setTabs(List value) { tabs = value == null ? new ArrayList() : value; }
    public List getSections() { return sections; }
    public void setSections(List value) { sections = value == null ? new ArrayList() : value; }
    public List getActions() { return actions; }
    public void setActions(List value) { actions = value == null ? new ArrayList() : value; }
}
