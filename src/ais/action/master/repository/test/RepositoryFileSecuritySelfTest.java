package ais.action.master.repository.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryFileService;

public final class RepositoryFileSecuritySelfTest {
    private RepositoryFileSecuritySelfTest(){}
    public static void main(String[] args)throws Exception{Method signature=RepositoryFileService.class.getDeclaredMethod("validSignature",String.class,byte[].class,int.class);signature.setAccessible(true);byte[] pdf=new byte[]{0x25,0x50,0x44,0x46,0x2d,0x31};check(Boolean.TRUE.equals(signature.invoke(null,"pdf",pdf,pdf.length)),"Signature PDF valid ditolak.");check(Boolean.FALSE.equals(signature.invoke(null,"png",pdf,pdf.length)),"Ekstensi palsu diterima.");Method name=RepositoryFileService.class.getDeclaredMethod("safeFileName",String.class);name.setAccessible(true);check(".._rahasia.pdf".equals(name.invoke(null,"../rahasia.pdf")),"Separator path tidak dinetralisasi.");try{name.invoke(null," ");throw new IllegalStateException("Nama kosong diterima.");}catch(InvocationTargetException expected){check(expected.getCause() instanceof IllegalArgumentException,"Exception nama kosong salah.");}Method access=RepositoryFileService.class.getDeclaredMethod("normalizeAccess",String.class);access.setAccessible(true);check("OPEN_ACCESS".equals(access.invoke(null,"OPEN_ACCESS")),"Open access valid ditolak.");check("RESTRICTED".equals(access.invoke(null,"UNKNOWN")),"Akses asing tidak fail-closed.");System.out.println("RepositoryFileSecuritySelfTest OK signatures filename access fail-closed");}
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
