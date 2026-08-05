package org.zkoss.zk.ui.event;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.zkoss.zk.au.AuRequest;
import org.zkoss.util.TimeZones;

/**
 * Shadow/patch of ZK's ClientInfoEvent.
 * Root cause: ZK 5 getInt() casts List element to Integer, but modern browsers
 * send numeric values as Double via JSON, causing ClassCastException.
 * Fix: cast to Number (supertype of both Integer and Double) before calling intValue().
 */
public class ClientInfoEvent extends Event {

    private final TimeZone _timeZone;
    private final int _scrnwd;
    private final int _scrnhgh;
    private final int _colorDepth;
    private final int _dtwd;
    private final int _dthgh;
    private final int _dtx;
    private final int _dty;

    public static final ClientInfoEvent getClientInfoEvent(AuRequest request) {
        Map data = request.getData();
        List vals = (List) data.get("");
        return new ClientInfoEvent(
                request.getCommand(),
                getInt(vals, 0),
                getInt(vals, 1),
                getInt(vals, 2),
                getInt(vals, 3),
                getInt(vals, 4),
                getInt(vals, 5),
                getInt(vals, 6),
                getInt(vals, 7));
    }

    private static final int getInt(List data, int index) {
        return ((Number) data.get(index)).intValue();
    }

    public ClientInfoEvent(String name, int tzOffset, int scrnwd, int scrnhgh,
            int colorDepth, int dtwd, int dthgh, int dtx, int dty) {
        super(name, null);
        _timeZone = TimeZones.getTimeZone(-tzOffset);
        _scrnwd = scrnwd;
        _scrnhgh = scrnhgh;
        _colorDepth = colorDepth;
        _dtwd = dtwd;
        _dthgh = dthgh;
        _dtx = dtx;
        _dty = dty;
    }

    public TimeZone getTimeZone() { return _timeZone; }
    public int getScreenWidth()   { return _scrnwd; }
    public int getScreenHeight()  { return _scrnhgh; }
    public int getColorDepth()    { return _colorDepth; }
    public int getDesktopWidth()  { return _dtwd; }
    public int getDesktopHeight() { return _dthgh; }
    public int getDesktopXOffset(){ return _dtx; }
    public int getDesktopYOffset(){ return _dty; }
}
