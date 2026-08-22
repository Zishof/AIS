package ais.action.master.jurnal.test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ais.action.master.jurnal.importer.OjsTransformCatalog;

/** Verifies all 905 generated field rows have an executable catalog outcome. */
public final class OjsFieldMappingCoverageSelfTest {
    private OjsFieldMappingCoverageSelfTest() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Path mapping Markdown wajib diberikan.");
        List<String> lines=Files.readAllLines(Paths.get(args[0]),Charset.forName("UTF-8"));
        String table=null;int tables=0,fields=0;Set<String> identities=new HashSet<String>();
        for(String line:lines){
            if(line.startsWith("## `")&&line.endsWith("`")){table=line.substring(4,line.length()-1);tables++;continue;}
            if(line.startsWith("| `")&&line.indexOf("` |")>3){if(table==null)throw new IllegalStateException("Field tanpa tabel.");String field=line.substring(3,line.indexOf("` |"));String id=table+"."+field;if(!identities.add(id))throw new IllegalStateException("Field mapping duplikat: "+id);OjsTransformCatalog.Outcome outcome=OjsTransformCatalog.outcome(table,field);if(outcome.targetType.length()==0||outcome.targetField.length()==0)throw new IllegalStateException("Outcome kosong: "+id);fields++;}
        }
        if(tables!=134||fields!=905)throw new IllegalStateException("Coverage bukan 134/905: "+tables+"/"+fields);
        System.out.println("OjsFieldMappingCoverageSelfTest OK tables=134 fields=905 explicit-outcomes=905");
    }
}
