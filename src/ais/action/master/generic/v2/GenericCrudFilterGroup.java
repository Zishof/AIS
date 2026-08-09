package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class GenericCrudFilterGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean conjunction = true;
    private List filters = new ArrayList();
    public boolean isConjunction() { return conjunction; }
    public void setConjunction(boolean conjunction) { this.conjunction = conjunction; }
    public List getFilters() { return filters; }
    public void setFilters(List filters) { this.filters = filters == null ? new ArrayList() : filters; }
}
