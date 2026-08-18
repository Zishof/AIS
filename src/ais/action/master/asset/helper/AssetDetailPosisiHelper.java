package ais.action.master.asset.helper;

import java.util.HashMap;
import java.util.HashSet;

import org.zkoss.gmaps.Gmaps;
import org.zkoss.gmaps.Gmarker;
import org.zkoss.gmaps.Gpolyline;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.North;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Konfigurasi;
import ais.database.model.asset.AssetDetail;
import ais.ui.util.MyButtonConfig;

public class AssetDetailPosisiHelper {

	private AssetDetail assetDetail;
	protected boolean editable = false;
	protected boolean edit = false;
	private Gmaps theMap;
	private Gmarker theMarker = new Gmarker();
	private EventListener eventListener;
	protected boolean[][] conTable;

	protected HashMap<Gmarker, HashSet<Gpolyline>> indexMap;

	public AssetDetailPosisiHelper(AssetDetail assetDetail,
			EventListener eventListener) {
		indexMap = new HashMap<Gmarker, HashSet<Gpolyline>>();
		this.assetDetail = assetDetail;
		this.eventListener = eventListener;
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
	}

	public Borderlayout display() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		North north = new North();
		north.setHeight("45px");
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		final Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("40px");
		toolbar.setVisible(edit);
		toolbar.setParent(north);

		final MyButtonConfig ubah = new MyButtonConfig("Ubah");
		ubah.setVisible(false);
		ubah.setHeight("40px");
		ubah.addEventListener("onClick", new EventListener() {
			public void onEvent(Event event) throws Exception {
				if (!editable) {
					editable = true;
					ubah.setLabel("Kunci");
				} else {
					editable = false;
					ubah.setLabel("Ubah");
				}
				theMarker.setDraggingEnabled(editable);
			}
		});
		toolbar.appendChild(ubah);

		final Timer timer = new Timer(2000);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage()
				.getFirstRoot());

		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ubah.setVisible(true);
				initMap(center);
				timer.detach();
			}
		});

		timer.start();

		return borderlayout;
	}

	public void initMap(Component center) throws Exception {
		String strLat = Common.getKonfigurasi("default_lat", Konfigurasi.AKTIF,
				"" + -6.195168, "", "").getInfo1();
		String strLng = Common.getKonfigurasi("default_lng", Konfigurasi.AKTIF,
				"" + 106.846046, "", "").getInfo1();

		assetDetail.setLat(Double.parseDouble(strLat));
		assetDetail.setLng(Double.parseDouble(strLng));

		theMarker.setLat(Double.parseDouble(strLat));
		theMarker.setLng(Double.parseDouble(strLng));

		theMap = new Gmaps();
		theMap.setWidth("100%");
		theMap.setHeight("100%");
		theMap.setDroppable("true");
		theMap.setMapType("hybrid");
		theMap.setLat(Double.parseDouble(strLat));
		theMap.setLng(Double.parseDouble(strLng));
		theMap.setZoom(18);

		theMap.setShowScaleCtrl(true);
		theMap.setShowSmallCtrl(true);
		theMap.setParent(center);

		if (assetDetail.getId() != null) {
			theMarker.setLat(assetDetail.getLat());
			theMarker.setLng(assetDetail.getLng());
			theMap.setLat(assetDetail.getLat());
			theMap.setLng(assetDetail.getLng());
		} else {
			assetDetail.setLat(theMarker.getLat());
			assetDetail.setLng(theMarker.getLng());
		}

		theMarker
				.setIconImage("http://www.google.com/mapfiles/ms/micons/red.png");
		theMarker.setDraggingEnabled(editable);

		theMarker.setParent(theMap);

		theMap.addEventListener("onMapDrop", new EventListener() {
			public void onEvent(Event event) throws Exception {

				String domain = theMap.getBaseDomain();
				System.out.println("============ onMapDrop =========== "
						+ domain);
				if (editable) {
					assetDetail.setLat(theMarker.getLat());
					assetDetail.setLng(theMarker.getLng());

					final Timer addressLoader = new Timer(200);
					addressLoader.setParent(ExecutionsCtrl.getCurrentCtrl()
							.getCurrentPage().getFirstRoot());
					addressLoader.addEventListener("onTimer",
							new EventListener() {

								@Override
								public void onEvent(Event arg0)
										throws Exception {
//									try {
//										List<String> strings = AssetHelper
//												.getAdsress(theMap.getLat(),
//														theMap.getLng());
//										assetDetail.setDetailAlamat(strings
//												+ "");
//										if (strings.size() != 0) {
//											assetDetail.setAlamat(strings
//													.get(0));
//										}
//										eventListener.onEvent(new Event("",
//												theMap, assetDetail));
//									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/asset/helper/AssetDetailPosisiHelper.java:171");
//										Common.tampilErrorJikaAdmin(e); 
//									}
									addressLoader.detach();
								}
							});
					addressLoader.start();
					eventListener.onEvent(new Event("", theMap, assetDetail));
				}
			}
		});

		// theMap.addEventListener("onMapClick", new EventListener() {
		// public void onEvent(Event e) throws Exception {
		// if (editable) {
		// MapMouseEvent mme = (MapMouseEvent) e;
		// Gmarker marker = new Gmarker();
		// marker.setLat(mme.getLat());
		// marker.setLng(mme.getLng());
		// marker.setDraggingEnabled(true);
		// marker.setParent(theMap);
		// linkBuffer.add(marker);
		// thePolygon.addPoint(mme.getLat(), mme.getLng(), 3);
		//
		// StringBuffer coordinate = new StringBuffer();
		// Iterator<Gmarker> i = linkBuffer.iterator();
		// while (i.hasNext()) {
		// Gmarker mymarker = i.next();
		// coordinate.append(mymarker.getLat() + ","
		// + mymarker.getLng() + ";");
		// }
		// assetDetail.setCoordinate(coordinate.toString());
		// eventListener.onEvent(new Event("", theMap, assetDetail));
		//
		// }
		// }
		// });

		theMap.panTo(theMarker.getLat(), theMarker.getLng());
		eventListener.onEvent(new Event("", theMap, assetDetail));
	}

}
