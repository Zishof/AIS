package ais.action.master.jurnal.test;

import java.util.Arrays;
import java.util.Date;

import ais.action.master.jurnal.JurnalMetadataFormatService;
import ais.database.model.repository.RepoItem;
import ais.database.model.repository.RepoItemMetadata;

/** Contract test for the five OAI metadata serializers required by plugin parity. */
public final class JurnalMetadataFormatSelfTest {
    public static void main(String[] args){
        JurnalMetadataFormatService service=new JurnalMetadataFormatService();
        check(service.formats().size()==5,"Harus tersedia lima format OAI.");
        RepoItem item=new RepoItem();item.setTitle("Riset <Aman>");item.setAuthors("Penulis A;Penulis B");item.setSubjects("jurnal;AIS");item.setAbstractText("Abstrak & bukti");item.setPublisher("eCampus");item.setIssuedAt(new Date(0));item.setDocumentType("JOURNAL_SUBMISSION");item.setLanguage("id_ID");item.setAccessPolicy("OPEN");item.setOaiIdentifier("oai:ais:jurnal:1");item.setDspaceHandle("123/1");
        RepoItemMetadata meta=new RepoItemMetadata();meta.setMetadataField("dc.subject");meta.setMetadataValue("metadata tambahan");
        String[] prefixes={"oai_dc","marc","marcxml","oai_jats","rfc1807"};
        for(String prefix:prefixes){String xml=service.serialize(prefix,item,Arrays.asList(meta));check(xml.indexOf("Riset &lt;Aman&gt;")>=0,prefix+" harus XML-safe");check(xml.indexOf("Abstrak & bukti")<0,prefix+" tidak boleh membocorkan ampersand mentah");}
        check(service.serialize("marcxml",item,null).indexOf("MARC21/slim")>=0,"Namespace MARCXML salah.");
        check(service.serialize("oai_jats",item,null).indexOf("article-title")>=0,"JATS title hilang.");
        try{service.serialize("unknown",item,null);throw new IllegalStateException("Unknown format harus ditolak.");}catch(IllegalArgumentException expected){}
        System.out.println("JurnalMetadataFormatSelfTest OK formats=5 xml-safe fail-closed");
    }
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
