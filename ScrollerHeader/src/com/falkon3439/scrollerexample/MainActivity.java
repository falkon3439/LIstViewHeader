package com.falkon3439.scrollerexample;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Interpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.enrique.stackblur.StackBlurManager;

/**
 * A demo of a listview scrolling/blurring thing with a header
 * 
 * To use: Put whatever view you want in the viewPagerHolder in the layout,
 * everything should automatically adhust to it and i don't think you will have
 * any problems
 */
public class MainActivity extends Activity implements OnScrollListener, OnGlobalLayoutListener, OnTouchListener {

	private ListView mListView;
	private ArrayAdapter<String> mListAdapter;

	private FrameLayout mViewPagerHolder;
	private View whiteWash;

	private ImageView mImageViewBlur;
	private Bitmap mBlurredViewPagerHolder;

	private int mTopPaddingAmount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize our views
		mViewPagerHolder = (FrameLayout) findViewById(R.id.viewPagerHolder);

		mImageViewBlur = (ImageView) findViewById(R.id.imageViewBlur);
		whiteWash = findViewById(R.id.viewWhiteWash);

		// Set up the listview
		mListView = (ListView) findViewById(R.id.listView);
		mListView.setOnScrollListener(this);

		// Hack to pass touches through the listview
		mListView.setOnTouchListener(this);

		// Add the layout listener so we can dynamically set the padding
		mListView.getViewTreeObserver().addOnGlobalLayoutListener(this);

		// Intiialize adapter
		mListAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.textViewListItem);
		// Fill the adapter with some test data
		fillAdapter();
		// apply it to the list
		mListView.setAdapter(mListAdapter);

	}

	public void fillAdapter() {
		for (int i = 0; i < 30; i++) {
			mListAdapter.add("Test Item " + i);
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// We use this method for the blurring as you scroll
		// If the listview is at the top, then we find out where the top item is
		// located
		if (firstVisibleItem == 0) {
			View topView = view.getChildAt(firstVisibleItem);

			if (topView != null) {
				// this will give us a value between mTopPaddingAmount and 0
				// mTopPaddingAmount means we are all the way scrolled down, 0
				// means that we have scrolled the top listview all the way to
				// the top of the screen
				int top = topView.getTop();

				// If we no longer need the blur, set it to null and make the
				// imageview have a null bitmap to
				if (top == mTopPaddingAmount) {
					mBlurredViewPagerHolder = null;
					mImageViewBlur.setImageBitmap(null);

					// mViewPagerHolder.setEnabled(true);
				}

				// Once we start scrolling we need to re-make the image and
				// apply a blur again
				else if (top < mTopPaddingAmount && mBlurredViewPagerHolder == null) {

					// This should porbably be done asynchronously,
					// however then there will be a weird pop-in when the image
					// is finished
					//
					// I find there isn't really that much lag if you scale a
					// lot, then blur and the effect is still the same
					//
					// Alternatively you can update this every time your pager
					// is moved instead of every time the user needs to scroll
					mBlurredViewPagerHolder = new StackBlurManager(getViewScaledBitmap(mViewPagerHolder, 8)).processNatively(5);
					mImageViewBlur.setImageBitmap(mBlurredViewPagerHolder);

					// Dont want people messing with the pager while scrolling
					// otherwise we will have an outdated blur
					//
					// Alternatively you can have the list view scroll to top
					// when they click on the pager
					// mViewPagerHolder.setEnabled(false);
				}

				// Set the alpha of the imageview so it appears as if the image
				// is blurring dynamically
				if (mTopPaddingAmount != 0) {

					float interpolation = ((float) (mTopPaddingAmount - top)) / mTopPaddingAmount;

					float alphaInterpolation = mAlphaInterpolator.getInterpolation(interpolation);
					mImageViewBlur.setAlpha(alphaInterpolation);
					whiteWash.setAlpha(alphaInterpolation);
				}
			}
		}
	}

	// An interpolator that makes it so the alpha happens faster, you can make
	// this any value you want

	private Interpolator mAlphaInterpolator = new Interpolator() {

		@Override
		public float getInterpolation(float input) {
			return Math.min(input, .7f) / .7f;
		}
	};

	/**
	 * Create a scaled bitmap from a view
	 * 
	 * @param view
	 *            The view to get a scaled bitmap from
	 * @param scale
	 *            How much to scale the view down by, if scrolling is slow make
	 *            this bigger
	 * @return A scaled bitmap
	 */
	public static Bitmap getViewScaledBitmap(View view, int scale) {
		Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		view.draw(canvas);

		Bitmap ret = Bitmap.createScaledBitmap(bitmap, view.getWidth() / scale, view.getHeight() / scale, true);
		bitmap.recycle();
		return ret;
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// We dont need this method but it is part of the scroll listener
	}

	@Override
	public void onGlobalLayout() {
		// Here we are setting the top padding of the listview,
		// We do this in a global layout listener so everything is already
		// measured and ready since we need the bottom of the view pager holder
		mTopPaddingAmount = mViewPagerHolder.getBottom();

		mListView.setPadding(mListView.getPaddingLeft(), mTopPaddingAmount, mListView.getPaddingRight(), mListView.getPaddingRight());

		// Scroll the listview down to where we just set
		mListView.setSelectionFromTop(0, mTopPaddingAmount);

		// Remove this listener, we only need to do it once, unless you have
		// some dynamic layouts going on
		mListView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
	}

	// Some additional touch hackery, there may be a better way to do this
	// But this should work
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		ListView listView = (ListView) v;
		if (listView.getFirstVisiblePosition() == 0) {
			View topView = listView.getChildAt(0);
			Rect touchRect = new Rect(listView.getLeft(), topView.getTop(), listView.getRight(), listView.getBottom());
			if (!touchRect.contains((int) event.getX(), (int) event.getY())) {
				mViewPagerHolder.onTouchEvent(event);
				return true;
			}
		}
		return false;

	}
}
