package com.jama.carouselview;

import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

public class CustomLinearSnapHelper extends LinearSnapHelper {

    @Override
    public View findSnapView(RecyclerView.LayoutManager layoutManager) {
        if(layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            if(!needToDoSnap(linearLayoutManager)) {
                return null;
            }
        }
        return super.findSnapView(layoutManager);
    }

    public int getSnapPosition(LinearLayoutManager layoutManager) {
        if(layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
            return 0;
        }
        else if(layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.getItemCount() - 1) {
            return layoutManager.getItemCount() - 1;
        }
        else {
            View snapView = findSnapView(layoutManager);
            if(snapView != null) {
                return layoutManager.getPosition(snapView);
            }
            else {
                return -1;
            }
        }
    }

    public boolean needToDoSnap(LinearLayoutManager linearLayoutManager){
        boolean result = linearLayoutManager.findFirstCompletelyVisibleItemPosition() != 0 && linearLayoutManager.findLastCompletelyVisibleItemPosition() != linearLayoutManager.getItemCount() - 1;
        return result;
    }
}
