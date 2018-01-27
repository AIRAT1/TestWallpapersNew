package de.android.drawer;


import android.view.MenuItem;
import android.view.SubMenu;

import java.util.List;


public class SimpleSubMenu {

    private SubMenu subMenu;
    private String subMenuTitle;


    private SimpleMenu parent;


    public SimpleSubMenu(SimpleMenu menu, String subMenu){
        super();
        this.parent = menu;
        this.subMenuTitle = subMenu;
        this.subMenu = menu.getMenu().addSubMenu(subMenu);

    }

    public MenuItem add(String title, int drawable, List<NavItem> action) {
        return parent.add(subMenu, title, drawable, action);
    }

    public MenuItem add(String title, int drawable, List<NavItem> action, boolean requiresPurchase) {
        return parent.add(subMenu, title, drawable, action, requiresPurchase);
    }

    public String getSubMenuTitle(){
        return subMenuTitle;
    }

}
