package de.android.drawer;


import android.view.Menu;
import android.view.MenuItem;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.android.R;


public abstract class SimpleAbstractMenu {

    protected Menu menu;
    protected MenuItemCallback callback;


    protected Map<MenuItem, List<NavItem>> menuContent;

    public SimpleAbstractMenu(){
        menuContent = new LinkedHashMap<>();
    }

    protected MenuItem add(Menu menu, String title, int drawable, final List<NavItem> action){
        return add(menu, title, drawable, action, false);
    }

    protected MenuItem add(Menu menu, String title, int drawable, final List<NavItem> action, final boolean requiresPurchase){

        MenuItem item = menu.add(R.id.main_group, Menu.NONE, Menu.NONE, title).setCheckable(true).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                callback.menuItemClicked(action, menuItem, requiresPurchase);
                return true;
            }
        });

        if (drawable != 0)
            item.setIcon(drawable);

        menuContent.put(item, action);

        return item;
    }

    protected Menu getMenu(){
        return menu;
    }


    public Map.Entry<MenuItem, List<NavItem>> getFirstMenuItem(){
        if (menuContent.size() < 1) {
            return null;
        }

        return menuContent.entrySet().iterator().next();
    }

    public Set<MenuItem> getMenuItems(){
        return menuContent.keySet();
    }

}