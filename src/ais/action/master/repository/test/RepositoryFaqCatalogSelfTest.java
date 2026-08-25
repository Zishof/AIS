package ais.action.master.repository.test;

import java.util.Map;
import ais.action.master.repository.RepositoryPublicService;
import ais.action.master.repository.RepositoryPublicService.FaqResult;

public final class RepositoryFaqCatalogSelfTest {
    private RepositoryFaqCatalogSelfTest(){}
    public static void main(String[] args){RepositoryPublicService service=new RepositoryPublicService();FaqResult all=service.faqCatalog("","",1,30);check(all.total==300L,"Total FAQ bukan 300.");check(all.categories.size()==20,"Kategori FAQ bukan 20.");long categoryTotal=0L;for(Map.Entry<String,Long> category:all.categories.entrySet()){check(category.getValue().longValue()==15L,"Kategori tidak berisi 15 topik: "+category.getKey());categoryTotal+=category.getValue().longValue();}check(categoryTotal==300L,"Jumlah facet FAQ tidak konsisten.");FaqResult search=service.faqCatalog("unggah PDF","",1,12);check(search.total>0L,"Pencarian multi-token gagal.");FaqResult page=service.faqCatalog("","",999,12);check(page.page==page.totalPages&&page.items.size()<=12,"Koreksi halaman FAQ gagal.");System.out.println("RepositoryFaqCatalogSelfTest OK total=300 categories=20 search="+search.total);}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
