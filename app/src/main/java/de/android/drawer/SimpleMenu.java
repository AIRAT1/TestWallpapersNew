package de.android.drawer;


import android.view.Menu;
import android.view.MenuItem;

import java.util.List;

public class SimpleMenu extends SimpleAbstractMenu {

    public SimpleMenu(Menu menu, MenuItemCallback callback){
        super();
        this.menu = menu;
        this.callback = callback;
    }

    public MenuItem add(String title, int drawable, List<NavItem> action) {
        return add(menu, title, drawable, action);
    }

    public MenuItem add(String title, int drawable, List<NavItem> action, boolean requiresPurchase) {
        return add(menu, title, drawable, action, requiresPurchase);
    }

}
