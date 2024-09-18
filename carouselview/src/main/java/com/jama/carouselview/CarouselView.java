package com.jama.carouselview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import com.jama.carouselview.enums.OffsetType;

public class CarouselView extends FrameLayout {

  private Context context;
  private RecyclerView carouselRecyclerView;
  private CarouselLinearLayoutManager layoutManager;
  private CarouselViewListener carouselViewListener;
  private CarouselScrollListener carouselScrollListener;
  private CarouselOnManualSelectionListener carouselOnItemSelectedListener;
  private OffsetType offsetType;
  private SnapHelper snapHelper;
  private boolean enableSnapping;
  private boolean enableAutoPlay;
  private int autoPlayDelay;
  private Handler autoPlayHandler;
  private boolean scaleOnScroll;
  private int resource;
  private int size;
  private int spacing;
  private int currentItem;
  private boolean isResourceSet = false;

  private CarouselViewAdapter carouselViewAdapter;

  public CarouselView(@NonNull Context context) {
    super(context);
    this.context = context;
    init(null);
  }

  public CarouselView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    init(attrs);
  }

  private void init(AttributeSet attributeSet) {
    LayoutInflater inflater = LayoutInflater.from(context);
    View carouselView = inflater.inflate(R.layout.view_carousel, this);
    this.carouselRecyclerView = carouselView.findViewById(R.id.carouselRecyclerView);
    this.autoPlayHandler = new Handler();

    carouselRecyclerView.setHasFixedSize(false);
    carouselRecyclerView.getItemAnimator().setChangeDuration(0);
    this.initializeAttributes(attributeSet);
  }

  private void initializeAttributes(AttributeSet attributeSet) {
    if (attributeSet != null) {
      TypedArray attributes = this.context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.CarouselView, 0, 0);
      this.enableSnapping(attributes.getBoolean(R.styleable.CarouselView_enableSnapping, true));
      this.setScaleOnScroll(attributes.getBoolean(R.styleable.CarouselView_scaleOnScroll, false));
      this.setAutoPlay(attributes.getBoolean(R.styleable.CarouselView_setAutoPlay, false));
      this.setAutoPlayDelay(attributes.getInteger(R.styleable.CarouselView_setAutoPlayDelay, 2500));
      this.setCarouselOffset(this.getOffset(attributes.getInteger(R.styleable.CarouselView_carouselOffset, 0)));
      int resourceId = attributes.getResourceId(R.styleable.CarouselView_resource, 0);
      if (resourceId != 0) {
        this.setResource(resourceId);
      }
      this.setSize(attributes.getInteger(R.styleable.CarouselView_item_size, 0));
      this.setSpacing(attributes.getInteger(R.styleable.CarouselView_item_spacing, 0));
      attributes.recycle();
    }
  }

  public void enableSnapping(boolean enable) {
    this.enableSnapping = enable;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    this.setAutoPlay(false);
  }

  private void setAdapter() {
    this.carouselRecyclerView.clearOnScrollListeners();
    this.layoutManager = new CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
    this.layoutManager.isOffsetStart(this.getCarouselOffset() == OffsetType.START);
    if (this.getScaleOnScroll()) this.layoutManager.setScaleOnScroll(true);
    carouselRecyclerView.setLayoutManager(this.layoutManager);
    carouselViewAdapter = new CarouselViewAdapter(getCarouselViewListener(), getResource(), getSize(), carouselRecyclerView, this.getSpacing(), this.getCarouselOffset() == OffsetType.CENTER);
    this.carouselRecyclerView.setAdapter(carouselViewAdapter);
    if (this.enableSnapping) {
      this.carouselRecyclerView.setOnFlingListener(null);
      this.snapHelper.attachToRecyclerView(this.carouselRecyclerView);
    }
    this.setScrollListener();
    this.enableAutoPlay();
  }

  private void setScrollListener() {
    this.carouselRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
      boolean wasScrollingManually = false;
      @Override
      public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);

        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
          wasScrollingManually = true;
        }

        int snapPosition = getSnapPosition();

        if(snapPosition >= 0) {
          if (carouselScrollListener != null) {
            carouselScrollListener.onScrollStateChanged(recyclerView, newState, snapPosition);
          }

          if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            if (carouselOnItemSelectedListener != null && wasScrollingManually) {
              carouselOnItemSelectedListener.onItemManuallySelected(snapPosition);
            }
            setCurrentItem(snapPosition);
          }
        }

        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
          wasScrollingManually = false;
        }
      }

      @Override
      public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (carouselScrollListener != null) {
          carouselScrollListener.onScrolled(recyclerView, dx, dy);
        }
      }
    });
  }

  public void setAutoPlay(boolean enableAutoPlay) {
    this.enableAutoPlay = enableAutoPlay;
  }

  public boolean getAutoPlay() {
    return this.enableAutoPlay;
  }

  public void setAutoPlayDelay(int autoPlayDelay) {
    this.autoPlayDelay = autoPlayDelay;
  }

  public int getAutoPlayDelay() {
    return this.autoPlayDelay;
  }

  private void enableAutoPlay() {
    autoPlayHandler.postDelayed(new Runnable() {
      public void run() {
        if (getAutoPlay()) {
          if (getSize() - 1 == getCurrentItem()) {
            setCurrentItem(0);
          } else {
            setCurrentItem(getCurrentItem() + 1);
          }
          autoPlayHandler.postDelayed(this, getAutoPlayDelay());
        }
      }
    }, getAutoPlayDelay());
  }

  public void setCarouselOffset(OffsetType offsetType) {
    this.offsetType = offsetType;
    switch (offsetType) {
      case CENTER:
        this.snapHelper = new CustomLinearSnapHelper();
        break;
      case START:
        this.snapHelper = new CarouselSnapHelper();
        break;
    }
  }

  public OffsetType getCarouselOffset() {
    return this.offsetType;
  }

  public void setCurrentItem(int item) {
    if (item < 0) {
      this.currentItem = 0;
    } else if (item >= this.getSize()) {
      // stay on same position and doesn't scroll to last one
//      this.currentItem = this.getSize() - 1;
    } else {
      this.currentItem = item;
    }
  }

  public void smoothScrollToItem(int index) {
    int oldSelectedItem = currentItem;
    setCurrentItem(index);

    int indexToScroll = currentItem;
    if(indexToScroll == 0) {
      indexToScroll = 1;
    }
    else if(indexToScroll == size - 1) {
      indexToScroll = size - 2;
    }

    View wantedView = layoutManager.findViewByPosition(currentItem);
    if(wantedView == null) {
      // scroll
      int offset = 300;
      if(oldSelectedItem < indexToScroll) {
        offset = getWidth() - offset;
      }
      layoutManager.scrollToPositionWithOffset(indexToScroll, offset);

      carouselRecyclerView.post(() -> {
        View view = layoutManager.findViewByPosition(currentItem);
        smoothScrollToView(view);
      });
    }
    else {
      smoothScrollToView(wantedView);
    }
  }

  private void smoothScrollToView(View view) {
    if (view == null) {
      return;
    }
    int[] snapDistance = snapHelper.calculateDistanceToFinalSnap(layoutManager, view);

    if (snapDistance != null && (snapDistance[0] != 0 || snapDistance[1] != 0)) {
      carouselRecyclerView.smoothScrollBy(snapDistance[0], snapDistance[1]);
    }
  }

  public int getCurrentItem() {
    return this.currentItem;
  }

  public void setScaleOnScroll(boolean scaleOnScroll) {
    this.scaleOnScroll = scaleOnScroll;
  }

  public boolean getScaleOnScroll() {
    return this.scaleOnScroll;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public int getSize() {
    return this.size;
  }

  public void setSpacing(int spacing) {
    this.spacing = spacing;
  }

  public int getSpacing() {
    return this.spacing;
  }

  public void setResource(int resource) {
    this.resource = resource;
    this.isResourceSet = true;
  }

  public int getResource() {
    return this.resource;
  }

  public void setCarouselViewListener(CarouselViewListener carouselViewListener) {
    this.carouselViewListener = carouselViewListener;
  }

  public CarouselViewListener getCarouselViewListener() {
    return this.carouselViewListener;
  }

  public void setCarouselScrollListener(CarouselScrollListener carouselScrollListener) {
    this.carouselScrollListener = carouselScrollListener;
  }

  public void setCarouselOnItemSelectedListener(CarouselOnManualSelectionListener carouselOnItemSelectedListener) {
    this.carouselOnItemSelectedListener = carouselOnItemSelectedListener;
  }

  public CarouselScrollListener getCarouselScrollListener() {
    return this.carouselScrollListener;
  }

  public void notifyDataSetChanged() {
    carouselViewAdapter.notifyDataSetChanged();
  }

  public void notifyItemChanged(int position) {
    carouselViewAdapter.notifyItemChanged(position);
  }

  private void validate() {
    if (!this.isResourceSet) throw new RuntimeException("Please add a resource layout to populate the carouselview");
  }

  private int getSnapPosition() {
    int snapPosition = -1;
    if(snapHelper instanceof CustomLinearSnapHelper) {
      CustomLinearSnapHelper snap = (CustomLinearSnapHelper) snapHelper;
      snapPosition = snap.getSnapPosition(layoutManager);
    }
    else {
      View centerView = snapHelper.findSnapView(layoutManager);
      if(centerView != null) {
        snapPosition = layoutManager.getPosition(centerView);
      }
    }

    return snapPosition;
  }

  private OffsetType getOffset(int value) {
    OffsetType offset;
    switch (value) {
      case 1:
        offset = OffsetType.CENTER;
        break;
      case 0:
      default:
        offset = OffsetType.START;
    }
    return offset;
  }

  public void show() {
    this.validate();
    this.setAdapter();
  }
}
