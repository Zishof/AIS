package ais.action.master.jurnal.test;
import ais.action.master.jurnal.JurnalProfileValidator;
public final class JurnalProfileValidatorSelfTest{
 private JurnalProfileValidatorSelfTest(){}
 public static void main(String[]a){String old="{\"schemaVersion\":1,\"reviewForms\":[{\"formKey\":\"standard\",\"version\":1,\"status\":\"PUBLISHED\",\"elements\":[{\"elementKey\":\"quality\",\"type\":\"SCALE\"}]}]}";check(JurnalProfileValidator.workflow(old,null).length()>0,"valid workflow");deny(new Runnable(){public void run(){JurnalProfileValidator.workflow("{\"schemaVersion\":1,\"reviewForms\":[]}",old);}},"published immutable");deny(new Runnable(){public void run(){JurnalProfileValidator.access("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"paid\",\"format\":\"SUBSCRIPTION\",\"price\":-1}]}");}},"negative price");check(JurnalProfileValidator.access("{\"schemaVersion\":1,\"policies\":[{\"policyKey\":\"open\",\"format\":\"OPEN\"}]}").length()>0,"valid access");System.out.println("JurnalProfileValidatorSelfTest OK");}
 private static void deny(Runnable r,String m){boolean denied=false;try{r.run();}catch(IllegalArgumentException e){denied=true;}check(denied,m);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}
}
