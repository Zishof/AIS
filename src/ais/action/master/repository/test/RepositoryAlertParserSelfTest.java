package ais.action.master.repository.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryAlertService;

/**
 * Harness uji manual (dijalankan langsung via {@code main}, bukan dari test runner) untuk method
 * privat {@code parse(String)} pada {@link RepositoryAlertService} — pengurai query string URL
 * pencarian repositori menjadi objek filter. Diakses lewat refleksi karena method target bersifat
 * privat. Memverifikasi dua skenario: (1) query string valid dengan parameter URL-encoded terurai
 * benar (keyword, field, id koleksi, tahun, filter kelengkapan berkas); (2) input tidak valid
 * (koleksi negatif, tahun bukan angka, field/filter di luar allow-list) DITOLAK secara fail-closed —
 * dikembalikan sebagai {@code null}/nilai default aman, bukan diteruskan mentah. Kegagalan
 * pemeriksaan melempar {@link IllegalStateException} lewat {@link #check(boolean, String)}.
 */
public final class RepositoryAlertParserSelfTest {
    private RepositoryAlertParserSelfTest(){}
    /** Menjalankan seluruh skenario uji parser secara berurutan; melempar {@link IllegalStateException} pada pemeriksaan pertama yang gagal, atau mencetak "OK" ke stdout bila seluruhnya lolos. */
    public static void main(String[] args)throws Exception{RepositoryAlertService service=new RepositoryAlertService();Method parse=RepositoryAlertService.class.getDeclaredMethod("parse",String.class);parse.setAccessible(true);Object filter=parse.invoke(service,"/ais/repository?view=search&q=pendidikan+Islam&field=title&collection=12&type=Skripsi&access=OPEN_ACCESS&year=2025&fullText=WITH_FILE");check("pendidikan Islam".equals(field(filter,"keyword")),"Keyword URL tidak terurai.");check("title".equals(field(filter,"field")),"Field tidak terurai.");check(Long.valueOf(12L).equals(field(filter,"collectionId")),"Collection tidak terurai.");check(Integer.valueOf(2025).equals(field(filter,"year")),"Tahun tidak terurai.");check("WITH_FILE".equals(field(filter,"fullText")),"Filter berkas tidak terurai.");Object invalid=parse.invoke(service,"?collection=-1&year=abc&field=DROP&fullText=UNKNOWN");check(field(invalid,"collectionId")==null&&field(invalid,"year")==null,"Angka invalid tidak ditolak.");check("all".equals(field(invalid,"field"))&&"".equals(field(invalid,"fullText")),"Allow-list filter gagal.");System.out.println("RepositoryAlertParserSelfTest OK URL decode and fail-closed allow-list");}
    /** Membaca nilai field privat {@code name} milik {@code object} lewat refleksi, untuk memeriksa hasil parse tanpa memerlukan getter publik. */
    private static Object field(Object object,String name)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(object);}
    /** Menegaskan {@code value} bernilai {@code true}; melempar {@link IllegalStateException} berisi {@code message} bila tidak (menghentikan harness pada pemeriksaan pertama yang gagal). */
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
