package testapp.testwallpapers.util;


import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;

import java.util.List;


public class CustomAppBarLayout extends AppBarLayout
        implements AppBarLayout.OnOffsetChangedListener {

    private State state;
    private List<OnStateChangeListener> onStateChangeListeners;

    public CustomAppBarLayout(Context context) {
        super(context);
    }

    public CustomAppBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isToolbarHidden(){
        return state == State.COLLAPSED;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!(getLayoutParams() instanceof CoordinatorLayout.LayoutParams)
                || !(getParent() instanceof CoordinatorLayout)) {
            throw new IllegalStateException(
                    "MyAppBarLayout must be a direct child of CoordinatorLayout.");
        }
        addOnOffsetChangedListener(this);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (state != State.EXPANDED) {
                notifyListeners(State.EXPANDED);
            }
            state = State.EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (state != State.COLLAPSED) {
                notifyListeners(State.COLLAPSED);
            }
            state = State.COLLAPSED;
        } else {
            if (state != State.IDLE) {
                notifyListeners(State.IDLE);
            }
            state = State.IDLE;
        }
    }

    private void notifyListeners(State state){
        if (onStateChangeListeners == null) return;

        for (OnStateChangeListener listener : onStateChangeListeners){
            listener.onStateChange(state);
        }
    }



    public interface OnStateChangeListener {
        void onStateChange(State toolbarChange);
    }

    public enum State {
        COLLAPSED,
        EXPANDED,
        IDLE
    }
}
