package ais.common.newui.menu;

public final class NewUiNativeJspExplicitRouteSelfTest {
    public static void main(String[] args) {
        check("/WEB-INF/new/root/uiux/mahasiswa.jsp", "/pages/master/mahasiswa.zul", false);
        check("/WEB-INF/new/root/services/mahasiswa_service.jsp", "/pages/master/mahasiswa.zul?x=1", true);
        check("/WEB-INF/new/root/uiux/pegawai.jsp", "/pages/master/pegawai.zul", false);
        check("/WEB-INF/new/root/uiux/jenis_pembayaran.jsp", "/pages/master/jenis_pembayaran.zul", false);
        check("/WEB-INF/new/alumni/uiux/mahasiswa.jsp", "/pages/master/alumni/mahasiswa.zul", false);
        if(NewUiNativeJspResolver.explicitFromRoute("/pages/other/mahasiswa.zul",false)!=null)throw new IllegalStateException("Route yang tidak terdaftar tidak boleh ditebak eksplisit.");
        System.out.println("NewUiNativeJspExplicitRouteSelfTest OK");
    }
    private static void check(String expected,String route,boolean service){NewUiNativeJspResolver.Result result=NewUiNativeJspResolver.explicitFromRoute(route,service);if(result==null||!expected.equals(result.getTarget()))throw new IllegalStateException(route+" -> "+(result==null?"null":result.getTarget()));}
}
