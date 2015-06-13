package com.alextam.tlistview;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * 下拉刷新, 点击加载更多
 * @author Alex Tam
 */
public class TListView extends ListView implements OnScrollListener{
	private String TAG = "TListView";
	
	private Context mContext;
	private View headerView = null, footerView = null;
	
	//是否允许下拉功能
	private boolean isCanPullDown = true;
	//顶部View,底部View的高度
	private int headerHeight, footerHeight;
	
	private int dDownY;
	//监听item的滑动位置
	private int firstItemPosition,lastItemPosition,itemTotal;
	
	private final static int PULL_DOWN_REFRESH = 1;
	private final static int CLICK_LOADMORE = 2;
	private final static int RELEASE_TO_REFRESH = 3;
//	private final static int RELEASE_TO_LOADMORE = 4;
	private final static int REFRESHING = 5;
	private final static int LOADING_MORE = 6;
	
	private int pullDownState = PULL_DOWN_REFRESH;
	private int loadmoreState = CLICK_LOADMORE;
	
	private TextView tv_header,tv_footer;
	private ImageView imv_footer;
	
	private final String PULL_DOWN_STR = "下拉获取数据";
	private final String PULL_REL_STR = "松开获取数据";
	private final String PULL_LOADING = "努力加载中...";
	private final String PULL_UP_STR = "点击加载更多";
	
	/** 拖曳模式 **/
	public static enum PULL_MODE{
		BOTH_PULL,PULL_DOWN,CLICK_LOADMORE,NONE_OF_ALL
	}
	/** 当前拖曳模式 **/
	private PULL_MODE pullMode;
	
	private ValueAnimator vAnimatorHeader = null;
	
	private onTListViewListener listener;
	
	
	
	public TListView(Context context) 
	{
		this(context, null);
	}
	
	public TListView(Context context, AttributeSet attrs) 
	{
		this(context, attrs, 0);
	}
	
	public TListView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		mContext = context;
		initHeaderView();
		setOnScrollListener(this);
	}
	
	//初始化头部View
	private void initHeaderView()
	{
		headerView = LayoutInflater.from(mContext).inflate(R.layout.lv_header, null);
		
		tv_header = (TextView)headerView.findViewById(R.id.tv_lv_header);
		tv_header.setText(PULL_DOWN_STR);
		
		vAnimatorHeader = getAnimator(headerView);
		
		measureView(headerView);
		
		headerHeight = headerView.getMeasuredHeight();
		headerView.setPadding(0, -headerHeight, 0, 0);
		addHeaderView(headerView);
	}
	
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount)
	{
		if(visibleItemCount >= 3)
		{
			firstItemPosition = firstVisibleItem;
			lastItemPosition = firstItemPosition + visibleItemCount - 3;
			itemTotal = totalItemCount - 2;
		}
	}
	
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{ }
	
	//初始化底部View
	private void initFooterView()
	{
		footerView = LayoutInflater.from(mContext).inflate(R.layout.lv_footer, null);
		tv_footer = (TextView)footerView.findViewById(R.id.tv_lv_footer);
		imv_footer = (ImageView)footerView.findViewById(R.id.imv_lv_footer);
		tv_footer.setText(PULL_UP_STR);
		
		measureView(footerView);
		
		footerHeight = footerView.getMeasuredHeight();
		
		footerView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(loadmoreState == CLICK_LOADMORE)
				{
					loadmoreState = LOADING_MORE;
					tv_footer.setText(PULL_LOADING);
					imv_footer.setVisibility(View.INVISIBLE);
					new pullTask(2, listener).execute();
				}
				else if(loadmoreState == LOADING_MORE)
				{
					updateFooterView();
				}
			}
		});
		addFooterView(footerView);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if(firstItemPosition == 0 && pullDownState == PULL_DOWN_REFRESH && canPullDown())
			{
				//标记可下拉刷新
				isCanPullDown = true;
				dDownY = (int)ev.getY();
			}
			else{
				isCanPullDown = false;
			}
			
			break;

		case MotionEvent.ACTION_MOVE:
			if(isCanPullDown && firstItemPosition == 0 && canPullDown())
			{	
				int mY = (int)ev.getY();
				int distance = (mY - dDownY)/2;
				
				headerView.setPadding(0, distance - headerHeight, 0, 0);

				if(headerView.getPaddingTop() >= 0)
				{
					pullDownState = RELEASE_TO_REFRESH;
					tv_header.setText(PULL_REL_STR);
					return true;
				}
				else if(headerView.getPaddingTop() < 0)
				{
					pullDownState = PULL_DOWN_REFRESH;
					tv_header.setText(PULL_DOWN_STR);
				}
			}
			
			
			break;
			
		case MotionEvent.ACTION_UP:
			if(pullDownState == RELEASE_TO_REFRESH){	
				updateHeaderView();
			}
			else if(pullDownState == PULL_DOWN_REFRESH){	
				updateHeaderView();
			}
			
			
			break;
			
		default:
			break;
		}
		
		return super.onTouchEvent(ev);
	}
	
	private Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			if(msg.what == PULL_DOWN_REFRESH)
			{
				completeHeader();
			}
			else if(msg.what == RELEASE_TO_REFRESH)
			{
				tv_header.setText(PULL_LOADING);
				headerView.setPadding(0, 0, 0, 0);
				
				if(listener != null)
				{
					pullDownState = REFRESHING;
					startAnimator(vAnimatorHeader);
					new pullTask(1, listener).execute();
				}
				else
				{
					pullDownState = PULL_DOWN_REFRESH;
					updateHeaderView();
					Log.e(TAG, "The onPullListViewListener used is null...");
				}
				postInvalidate();
			}
			else if(msg.what == CLICK_LOADMORE)
			{
				completeFooter();
			}
			else if(msg.what == LOADING_MORE)
			{
				Toast.makeText(mContext, "数据正在获取中", Toast.LENGTH_SHORT).show();
			}
			super.handleMessage(msg);
		}
	};
	
	/**
	 * 刷新headerView
	 */
	private void updateHeaderView()
	{
		switch (pullDownState) {
		case PULL_DOWN_REFRESH:
			//放手刷新
			isCanPullDown = true;
			mHandler.sendEmptyMessage(PULL_DOWN_REFRESH);
			
			break;

		case RELEASE_TO_REFRESH:
			//提示继续下拉
			isCanPullDown = false;
			mHandler.sendEmptyMessage(RELEASE_TO_REFRESH);
			
			break;
			
		default:
			break;
		}
	}
	
	/**
	 * 刷新footerView
	 */
	private void updateFooterView()
	{
		switch (loadmoreState) {
		case CLICK_LOADMORE:
			mHandler.sendEmptyMessage(CLICK_LOADMORE);
			break;

		case LOADING_MORE:
			mHandler.sendEmptyMessage(LOADING_MORE);
			break;
			
		default:
			break;
		}
	}
	
	/**
	 * 恢復headerView
	 */
	public void completeHeader()
	{
		tv_header.setText(PULL_DOWN_STR);
		headerView.setPadding(0, -headerHeight, 0, 0);
		
		endAnimator(vAnimatorHeader);
		postInvalidate();
	}
	
	/**
	 * 恢復footerView
	 */
	public void completeFooter()
	{
		tv_footer.setText(PULL_UP_STR);
		imv_footer.setVisibility(View.VISIBLE);
		loadmoreState = CLICK_LOADMORE;
	}
	
	/**
	 * 开始动画
	 * @param animator
	 */
	private void startAnimator(Animator animator)
	{
		if(animator != null)
		{
			animator.start();
		}
	}
	
	/**
	 * 停止动画
	 * @param animator
	 */
	private void endAnimator(Animator animator)
	{
		if(animator != null)
		{
			animator.cancel();
		}
	}
	
	
	/**
	 * 执行异步任务类
	 */
	private class pullTask extends AsyncTask<Void, Void, Void>
	{
		private int code;
		private onTListViewListener mListener;
		
		public pullTask(int code, onTListViewListener listener)
		{
			this.code = code;
			this.mListener = listener;
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			try 
			{
				if(mListener != null)
				{
					if(code == 1)
					{	//刷新
						try {
							mListener.onRefresh();
							//恢复初始态
							pullDownState = PULL_DOWN_REFRESH;
							updateHeaderView();
						} 
						catch (Exception e) {
							e.printStackTrace();
							pullDownState = PULL_DOWN_REFRESH;
							updateHeaderView();
						}
					}
					else if(code == 2)
					{	//加载更多
						try {
							mListener.onLoadMore();
							//恢复初始态
							loadmoreState = CLICK_LOADMORE;
							updateFooterView();
						} 
						catch (Exception e) {
							e.printStackTrace();
							loadmoreState = CLICK_LOADMORE;
							updateFooterView();
						}
					}
				}
				else
				{
					Log.e(TAG, "onPullListViewListener in pullTask is NULL...");
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result){ }
		
		@Override
		protected void onPreExecute(){ }
	}
	
	
	private void measureView(View viewSholdBeMeasure) 
	{
		ViewGroup.LayoutParams viewLayoutParams=(LayoutParams) viewSholdBeMeasure.getLayoutParams();
		if(viewLayoutParams == null)
		{
			viewLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}
		
		
		int widthSpecificationConstraint = ViewGroup.getChildMeasureSpec(0, 0, viewLayoutParams.width);
		int heightSpecificationConstraint;
		if(viewLayoutParams.height > 0)
		{
			heightSpecificationConstraint=MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY );
		}
		else
		{
			heightSpecificationConstraint=MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED );
		}
		viewSholdBeMeasure.measure(widthSpecificationConstraint, heightSpecificationConstraint);
	}

	/**
	 * 设置透明度动画
	 * @param target
	 * @return
	 */
	private ValueAnimator getAnimator(final View target)
	{
		ValueAnimator vAnimator = ValueAnimator.ofFloat(0.8f,1.0f);
		vAnimator.setTarget(target);
		vAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				target.setAlpha((float)animation.getAnimatedValue());
			}
		});
		vAnimator.setDuration(500);
		vAnimator.setRepeatCount(ValueAnimator.INFINITE);
		vAnimator.setRepeatMode(ValueAnimator.REVERSE);
		return vAnimator;
	}
	
	/**
	 * 上下拉接口
	 */
	public interface onTListViewListener
	{
		void onRefresh();
		void onLoadMore();
	}
	
	public void setOnTListViewListener(onTListViewListener listener)
	{
		this.listener = listener;
	}
	
	@Override
    public void setAdapter(ListAdapter adapter) 
	{
		super.setAdapter(adapter);
	}
	
	/**
	 * 是否可下拉
	 * @return
	 */
	private boolean canPullDown()
	{
		if(pullMode == PULL_MODE.BOTH_PULL 
				|| pullMode == PULL_MODE.PULL_DOWN)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * 设置列表拖曳模式
	 * @param mode
	 */
	public void setTListViewMode(PULL_MODE mode)
	{
		this.pullMode = mode;
		if(pullMode != PULL_MODE.BOTH_PULL && pullMode != PULL_MODE.CLICK_LOADMORE)
		{
			if(footerView != null){
				removeFooterView(footerView);
			}
		}
		else{
			if(footerView != null){
				addFooterView(footerView);
			}
			else
			{
				initFooterView();
			}
		}
	}
	
	/** 获取列表拖曳模式 **/
	public PULL_MODE getTListViewMode()
	{
		return this.pullMode;
	}
}
