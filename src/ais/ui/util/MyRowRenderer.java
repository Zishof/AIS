package ais.ui.util;

import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;

public abstract class MyRowRenderer implements RowRenderer {

    public void render(Row arg0, Object arg1, int arg2) throws Exception {
        render(arg0, arg1);
        UIHelper.absorptionKebab(arg0);
    }

    public abstract void render(Row arg0, Object arg1) throws Exception;

}
