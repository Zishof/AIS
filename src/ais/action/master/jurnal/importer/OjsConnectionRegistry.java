package ais.action.master.jurnal.importer;

import java.util.Locale;

/** Resolves named OJS source connections from deployment secrets, never HTTP payloads or database rows. */
public final class OjsConnectionRegistry {
    private OjsConnectionRegistry(){}
    public static OjsImportPreflightService.Config resolve(String reference){
        String ref=reference==null?"":reference.trim().toUpperCase(Locale.ENGLISH);
        if(!ref.matches("[A-Z][A-Z0-9_]{1,39}"))throw new IllegalArgumentException("Connection reference OJS tidak valid.");
        String prefix="AIS_JURNAL_OJS_"+ref+"_";
        OjsImportPreflightService.Config c=new OjsImportPreflightService.Config();
        c.jdbcUrl=required(prefix+"JDBC_URL");c.user=required(prefix+"USER");c.password=required(prefix+"PASSWORD");c.schema=optional(prefix+"SCHEMA");
        c.loginTimeoutSeconds=integer(prefix+"LOGIN_TIMEOUT_SECONDS",15,1,60);c.queryTimeoutSeconds=integer(prefix+"QUERY_TIMEOUT_SECONDS",30,1,300);
        return c;
    }
    private static String required(String key){String v=System.getenv(key);if(v==null||v.trim().length()==0)throw new IllegalArgumentException("Secret/config sumber OJS belum tersedia untuk reference tersebut.");return v.trim();}
    private static String optional(String key){String v=System.getenv(key);return v==null?null:v.trim();}
    private static int integer(String key,int fallback,int min,int max){String v=System.getenv(key);if(v==null||v.trim().length()==0)return fallback;try{int n=Integer.parseInt(v.trim());if(n<min||n>max)throw new Exception();return n;}catch(Exception e){throw new IllegalArgumentException("Timeout sumber OJS tidak valid.");}}
}
