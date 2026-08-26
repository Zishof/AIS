package ais.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Kontrak paritas fitur eBisnis untuk Desktop, Android, JSP, dan ZKoss. */
public final class EbisnisPlatformParityRegistry {
	public static final String DESKTOP = "desktop";
	public static final String ANDROID = "android";
	public static final String JSP = "jsp";
	public static final String ZKOSS = "zkoss";
	public static final String ERROR_CONTRACT = "EBISNIS_ERROR_V1";
	public static final int DEFAULT_PAGE_SIZE = 10;
	public static final int MAX_PAGE_SIZE = 100;
	public interface AccessPolicy { boolean apakahAdmin(); boolean diizinkan(String menuKey, String aksi); }
	public static final class PlatformProfile {
		public final String platform; public final Set<String> capabilities;
		private PlatformProfile(String p, String c) { platform=p; capabilities=nilai(c); }
		public boolean mendukung(String c) { return capabilities.contains(normalisasi(c)); }
	}
	public static final class ResolvedAction {
		public final String platform, menuKey, aksi, canonicalRoute, permissionKey, errorContract, denialReason;
		public final boolean visible, enabled, writeOperation, idempotencyRequired, optimisticVersionRequired;
		public final int pageSize;
		private ResolvedAction(String p,String m,String a,String r,boolean ok,boolean w,boolean v,int s,String d){platform=p;menuKey=m;aksi=a;canonicalRoute=r;permissionKey=m+":"+a;visible=ok;enabled=ok;writeOperation=w;idempotencyRequired=w;optimisticVersionRequired=v;pageSize=s;errorContract=ERROR_CONTRACT;denialReason=d;}
	}
	private static final Map<String,PlatformProfile> PROFILES=new LinkedHashMap<String,PlatformProfile>();
	static { String u="responsive_navigation,work_queue,optimistic_lock,error_contract,export_pdf,export_excel,print,contextual_help"; profil(DESKTOP,u+",barcode_scan,qr_scan,offline_queue"); profil(ANDROID,u+",barcode_scan,qr_scan,offline_queue"); profil(JSP,u); profil(ZKOSS,u); }
	private EbisnisPlatformParityRegistry(){}
	private static void profil(String p,String c){PROFILES.put(p,new PlatformProfile(p,c));}
	public static List<PlatformProfile> semuaPlatform(){return Collections.unmodifiableList(new ArrayList<PlatformProfile>(PROFILES.values()));}
	public static PlatformProfile platform(String p){return PROFILES.get(normalisasi(p));}
	public static ResolvedAction resolve(String p,String k,String a,int requested,AccessPolicy policy){PlatformProfile profile=platform(p);if(profile==null)throw new IllegalArgumentException("Platform tidak dikenal: "+p);EbisnisMenuBlueprintRegistry.Entri menu=EbisnisMenuBlueprintRegistry.dapatkan(k);if(menu==null||!menu.platforms.contains(profile.platform))throw new IllegalArgumentException("Menu tidak tersedia pada platform: "+k+" / "+p);String ak=EbisnisMenuActionRegistry.aksiKanonik(a);if(!EbisnisMenuActionRegistry.aksiTerdaftar(ak)||!menu.requiredActions.contains(ak))throw new IllegalArgumentException("Aksi tidak tersedia pada menu: "+menu.menuKey+":"+a);boolean ok=policy!=null&&(policy.apakahAdmin()||policy.diizinkan(menu.menuKey,ak));boolean w=bukanBaca(ak);boolean v=w&&!"create".equals(ak)&&!"export".equals(ak);int s=requested<=0?DEFAULT_PAGE_SIZE:Math.min(requested,MAX_PAGE_SIZE);return new ResolvedAction(profile.platform,menu.menuKey,ak,menu.canonicalRoute,ok,w,v,s,ok?"":"AKSES_DITOLAK");}
	public static void validasi(){EbisnisMenuBlueprintRegistry.validasi();List<EbisnisMenuBlueprintRegistry.Entri> menus=EbisnisMenuBlueprintRegistry.semua();for(int i=0;i<menus.size();i++){EbisnisMenuBlueprintRegistry.Entri m=menus.get(i);for(String p:PROFILES.keySet())if(!m.platforms.contains(p))throw new IllegalStateException("Menu belum parity: "+m.menuKey+" / "+p);}for(PlatformProfile p:PROFILES.values())if(!p.mendukung("error_contract")||!p.mendukung("optimistic_lock"))throw new IllegalStateException("Kontrak inti belum tersedia: "+p.platform);}
	private static boolean bukanBaca(String a){return !("view".equals(a)||"export".equals(a)||"view_cost".equals(a)||"view_all_location".equals(a));}
	private static Set<String> nilai(String csv){Set<String> h=new LinkedHashSet<String>();String[] b=csv.split(",");for(int i=0;i<b.length;i++){String x=normalisasi(b[i]);if(x.length()>0)h.add(x);}return Collections.unmodifiableSet(h);}
	private static String normalisasi(String n){return n==null?"":n.trim().toLowerCase();}
}
