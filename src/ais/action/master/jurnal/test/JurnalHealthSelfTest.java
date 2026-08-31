package ais.action.master.jurnal.test;
import java.util.HashSet;import org.json.JSONObject;import ais.action.master.jurnal.JurnalHealthService;import ais.common.JurnalAksesKatalog;import ais.database.hibernate.HibernateUtil;import ais.database.model.Menu;import ais.database.model.Tbmrole;import ais.database.model.Tbmuser;
/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan test otomatis) untuk memverifikasi
 * kesiapan modul jurnal ({@link ais.action.master.jurnal.JurnalHealthService}) pada database
 * clone. Membangun {@link Tbmrole} administrator dengan akses jurnal penuh (lewat
 * {@link JurnalAksesKatalog#modelUntukEditor}) dan {@link Tbmuser} sementara
 * ({@code JRN_HEALTH_SELF_TEST}), lalu memanggil {@code JurnalHealthService.check} dan memvalidasi
 * status {@code "UP"} beserta jumlah skema jurnal utama dan streaming yang diharapkan.
 * <p>
 * <b>Pengaman wajib</b>: menolak berjalan (melempar {@link IllegalStateException}) kecuali
 * environment variable {@code AIS_JURNAL_DB_NAME} diset ke nama database selain {@code "ais"}
 * (mis. clone UAT) — mencegah harness ini tidak sengaja dijalankan terhadap database produksi.
 * </p>
 */
public final class JurnalHealthSelfTest{private JurnalHealthSelfTest(){}
	/** Menjalankan skenario pengecekan kesiapan jurnal; keluar dengan kode 0 bila berhasil, atau melempar exception bila status tidak {@code "UP"} atau prasyarat database clone tidak terpenuhi. */
	public static void main(String[]a)throws Exception{String db=System.getenv("AIS_JURNAL_DB_NAME");if(db==null||"ais".equalsIgnoreCase(db))throw new IllegalStateException("Health test wajib clone.");System.setProperty("javax.persistence.validation.mode","none");Tbmrole role=new Tbmrole();role.setRoleId(Tbmrole.ADMINISTRATOR);JSONObject access=JurnalAksesKatalog.modelUntukEditor(null);access.getJSONObject("menu").put("operations",true);access.getJSONObject("crud").getJSONObject("operations").put("read",true);role.setJurnalAksesJson(access.toString());HashSet<Menu>menus=new HashSet<Menu>();Menu menu=new Menu();menu.setId(2000460528L);menus.add(menu);role.setMenus(menus);Tbmuser user=new Tbmuser();user.setUserId("JRN_HEALTH_SELF_TEST");user.setUserRole(role);Tbmuser.getUserRoleYgDipakai.put(user.getUserId(),role);try{JSONObject out=new JurnalHealthService().check(user);if(!"UP".equals(out.getString("status"))||out.getInt("mainJournalSchema")!=3||out.getInt("streamingSchema")!=1)throw new IllegalStateException("Readiness journal tidak UP: "+out);System.out.println("JurnalHealthSelfTest OK main streaming schema-mutation-disabled");}finally{HibernateUtil.closeSession();Tbmuser.getUserRoleYgDipakai.remove(user.getUserId());}System.exit(0);}}
