package ais.action.master.repository.test;

import java.lang.reflect.Method;
import java.util.Date;
import ais.action.servlet.Repository;

public final class RepositoryOaiProtocolSelfTest {
    private RepositoryOaiProtocolSelfTest(){}
    public static void main(String[] args)throws Exception{Repository servlet=new Repository();Method build=Repository.class.getDeclaredMethod("buildOaiToken",int.class,Long.class,Date.class,Date.class,String.class);build.setAccessible(true);Method valid=Repository.class.getDeclaredMethod("validOaiToken",String.class);valid.setAccessible(true);Method page=Repository.class.getDeclaredMethod("parseOaiPage",String.class);page.setAccessible(true);Method verb=Repository.class.getDeclaredMethod("parseOaiTokenVerb",String.class);verb.setAccessible(true);String token=(String)build.invoke(servlet,3,Long.valueOf(9L),new Date(1000L),new Date(2000L),"ListRecords");check(Boolean.TRUE.equals(valid.invoke(servlet,token)),"Token bertanda tangan ditolak.");check(((Integer)page.invoke(servlet,token)).intValue()==3,"Halaman token salah.");check("ListRecords".equals(verb.invoke(servlet,token)),"Verb token salah.");char replacement=token.charAt(token.length()/2)=='A'?'B':'A';String tampered=token.substring(0,token.length()/2)+replacement+token.substring(token.length()/2+1);check(Boolean.FALSE.equals(valid.invoke(servlet,tampered)),"Token yang diubah diterima.");check(Boolean.FALSE.equals(valid.invoke(servlet,"p3;s9;f1000;u2000;vR")),"Token legacy tanpa signature diterima.");System.out.println("RepositoryOaiProtocolSelfTest OK signed opaque token tamper rejection");}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
