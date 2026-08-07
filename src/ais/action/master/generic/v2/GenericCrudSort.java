package ais.action.master.generic.v2;

import java.io.Serializable;

public class GenericCrudSort implements Serializable {
    private static final long serialVersionUID = 1L;
    private String property;
    private boolean ascending = true;

    public GenericCrudSort() { }
    public GenericCrudSort(String property, boolean ascending) {
        this.property = property;
        this.ascending = ascending;
    }
    public String getProperty() { return property; }
    public void setProperty(String property) { this.property = property; }
    public boolean isAscending() { return ascending; }
    public void setAscending(boolean ascending) { this.ascending = ascending; }
}
