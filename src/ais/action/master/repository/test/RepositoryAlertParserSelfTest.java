package ais.action.master.repository.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryAlertService;

public final class RepositoryAlertParserSelfTest {
    private RepositoryAlertParserSelfTest(){}
    public static void main(String[] args)throws Exception{RepositoryAlertService service=new RepositoryAlertService();Method parse=RepositoryAlertService.class.getDeclaredMethod("parse",String.class);parse.setAccessible(true);Object filter=parse.invoke(service,"/ais/repository?view=search&q=pendidikan+Islam&field=title&collection=12&type=Skripsi&access=OPEN_ACCESS&year=2025&fullText=WITH_FILE");check("pendidikan Islam".equals(field(filter,"keyword")),"Keyword URL tidak terurai.");check("title".equals(field(filter,"field")),"Field tidak terurai.");check(Long.valueOf(12L).equals(field(filter,"collectionId")),"Collection tidak terurai.");check(Integer.valueOf(2025).equals(field(filter,"year")),"Tahun tidak terurai.");check("WITH_FILE".equals(field(filter,"fullText")),"Filter berkas tidak terurai.");Object invalid=parse.invoke(service,"?collection=-1&year=abc&field=DROP&fullText=UNKNOWN");check(field(invalid,"collectionId")==null&&field(invalid,"year")==null,"Angka invalid tidak ditolak.");check("all".equals(field(invalid,"field"))&&"".equals(field(invalid,"fullText")),"Allow-list filter gagal.");System.out.println("RepositoryAlertParserSelfTest OK URL decode and fail-closed allow-list");}
    private static Object field(Object object,String name)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
