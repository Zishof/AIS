package ais.action.master.jurnal.test;

import java.lang.reflect.Method;
import java.util.Base64;
import ais.action.servlet.Oai;

/** Stateless OAI resumption-token integrity and bounds gate. */
public final class JurnalOaiProtocolSelfTest {
    private JurnalOaiProtocolSelfTest(){}
    public static void main(String[]args)throws Exception{Oai oai=new Oai();Method encode=Oai.class.getDeclaredMethod("encodeToken",java.util.Date.class,java.util.Date.class,String.class,int.class,long.class,String.class);encode.setAccessible(true);Method decode=Oai.class.getDeclaredMethod("decodeToken",String.class);decode.setAccessible(true);String token=(String)encode.invoke(oai,null,null,"col_1",100,250L,"marcxml");if(token.length()<40||decode.invoke(oai,token)==null)throw new IllegalStateException("Token OAI valid gagal round-trip.");byte[]raw=Base64.getUrlDecoder().decode(token);raw[Math.max(0,raw.length/2)]^=1;String tampered=Base64.getUrlEncoder().withoutPadding().encodeToString(raw);if(decode.invoke(oai,tampered)!=null)throw new IllegalStateException("Token OAI yang diubah harus ditolak.");String malformed=Base64.getUrlEncoder().withoutPadding().encodeToString("||||-1|oai_dc|0|00".getBytes("UTF-8"));if(decode.invoke(oai,malformed)!=null)throw new IllegalStateException("Token OAI malformed harus ditolak.");System.out.println("JurnalOaiProtocolSelfTest OK signed opaque token tamper bounds fail-closed");}
}
