package ais.action.master.generic.v2;

import java.util.ArrayList;
import ais.action.master.generic.v2.GenericCrudSort;

public class GenericCrudLookupService {
    private final GenericCrudQueryService query = new GenericCrudQueryService();
    public GenericCrudResult lookup(GenericCrudRequestContext context, String search, int page, int pageSize) throws Exception {
        GenericCrudPage data = query.list(context, page, pageSize, search, new ArrayList(), null);
        java.util.Map result = new java.util.LinkedHashMap();
        result.put("mode", data.getTotal() <= context.getDefinition().getLookupThreshold() ? "COMBO" : "PAGED_LOOKUP");
        result.put("page", data);
        return GenericCrudResult.ok("Lookup berhasil.", result);
    }
}
