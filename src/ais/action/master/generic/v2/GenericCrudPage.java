package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("rawtypes")
public class GenericCrudPage implements Serializable {
    private static final long serialVersionUID = 1L;
    private List rows = new ArrayList();
    private long total;
    private int page = 1;
    private int pageSize = 10;

    public List getRows() { return rows; }
    public void setRows(List rows) { this.rows = rows == null ? new ArrayList() : rows; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public long getPageCount() { return pageSize < 1 ? 0 : (total + pageSize - 1) / pageSize; }
}
