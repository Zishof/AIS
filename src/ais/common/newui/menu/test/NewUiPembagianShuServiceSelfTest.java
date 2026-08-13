package ais.common.newui.menu.test;

import ais.common.newui.menu.NewUiPembagianShuService;
import ais.database.model.koperasi.Koperasi;

public final class NewUiPembagianShuServiceSelfTest {
    public static void main(String[] args) {
        NewUiPembagianShuService service=new NewUiPembagianShuService(); Koperasi koperasi=new Koperasi(Long.valueOf(1));
        rejected(service,2026,0,new double[]{25,25,30,10,5,5,0},koperasi,"total");
        rejected(service,2026,100,new double[]{25,25,30,10,5,4,0},koperasi,"100%");
        rejected(service,2026,100,new double[]{-1,26,30,10,5,5,25},koperasi,"0 sampai 100");
        System.out.println("NewUiPembagianShuServiceSelfTest OK");
    }
    private static void rejected(NewUiPembagianShuService service,int year,double total,double[] values,Koperasi koperasi,String expected){try{service.calculate(year,total,values,koperasi);throw new IllegalStateException("Validasi tidak menolak input.");}catch(IllegalArgumentException ok){if(ok.getMessage()==null||ok.getMessage().toLowerCase().indexOf(expected.toLowerCase())<0)throw new IllegalStateException(ok.getMessage());}}
}
